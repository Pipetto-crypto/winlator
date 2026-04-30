package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import android.util.SparseArray;

import com.winlator.cmod.renderer.GPUImage;
import com.winlator.cmod.renderer.Texture;
import com.winlator.cmod.renderer.VulkanRenderer;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.Drawable;
import com.winlator.cmod.xserver.Pixmap;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xserver.errors.BadImplementation;
import com.winlator.cmod.xserver.errors.BadMatch;
import com.winlator.cmod.xserver.errors.BadPixmap;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.events.PresentCompleteNotify;
import com.winlator.cmod.xserver.events.PresentIdleNotify;

import java.io.IOException;

public class PresentExtension implements Extension {
    public static final byte MAJOR_OPCODE = -103;
    public enum Kind { PIXMAP, MSC_NOTIFY }
    public enum Mode { COPY, FLIP, SKIP }

    private final SparseArray<Event> events = new SparseArray<>();
    private SyncExtension syncExtension;


    
    private static class PendingIdle {
        Window window; Pixmap pixmap; int serial; int idleFence;
        long targetNs;     
        int  vsyncSkips;    // vsyncs left to skip before firing (for fps < refresh)
        PendingIdle(Window w, Pixmap p, int s, int f, long t, int sk) {
            window = w; pixmap = p; serial = s; idleFence = f; targetNs = t; vsyncSkips = sk;
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<Integer, PendingIdle> pendingIdles =
        new java.util.concurrent.ConcurrentHashMap<>();

    private volatile android.view.Choreographer choreographer = null;
    private volatile boolean choreographerChecked = false;
    private final Object choreographerLock = new Object();

    private Thread cpuPacerThread = null;
    private final java.util.concurrent.PriorityBlockingQueue<PendingIdle> cpuQueue =
        new java.util.concurrent.PriorityBlockingQueue<>(11,
            java.util.Comparator.comparingLong(p -> p.targetNs));

    private static final long FIRE_EARLY_NS = 700_000L; // 0.7 ms

    private android.view.Choreographer tryGetChoreographer(VulkanRenderer renderer) {
        if (choreographerChecked) return choreographer;
        synchronized (choreographerLock) {
            if (choreographerChecked) return choreographer;
            choreographerChecked = true;
            try {
                if (renderer != null && renderer.xServerView != null) {
                    choreographer = android.view.Choreographer.getInstance();
                }
            } catch (Exception ignored) {
                android.util.Log.w("PresentExtension", "Choreographer unavailable, using CPU pacer");
            }
            if (choreographer == null) {
                startCpuPacer();
            }
            return choreographer;
        }
    }

    private void startCpuPacer() {
        if (cpuPacerThread != null) return;
        cpuPacerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                PendingIdle p = cpuQueue.peek();
                if (p == null) {
                    java.util.concurrent.locks.LockSupport.parkNanos(500_000L);
                    continue;
                }
                long now = System.nanoTime();
                if (now >= p.targetNs) {
                    cpuQueue.poll();
                    pendingIdles.remove(p.window.id, p);
                    sendIdleNotify(p.window, p.pixmap, p.serial, p.idleFence);
                } else {
                    long diff = p.targetNs - now;
                    if (diff > 2_000_000L)
                        java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L);
                    else
                        Thread.yield();
                }
            }
        }, "PresentPacer-CPU");
        cpuPacerThread.setDaemon(true);
        cpuPacerThread.setPriority(Thread.MAX_PRIORITY);
        cpuPacerThread.start();
    }

    private volatile boolean choreographerPosted = false;
    private final android.view.Choreographer.FrameCallback vsyncCallback = frameTimeNs -> {
        choreographerPosted = false;
        boolean anyRemaining = false;
        for (java.util.Iterator<java.util.Map.Entry<Integer, PendingIdle>> it =
                pendingIdles.entrySet().iterator(); it.hasNext(); ) {
            PendingIdle p = it.next().getValue();
            if (frameTimeNs >= p.targetNs) {
                if (p.vsyncSkips > 0) {
                    p.vsyncSkips--;
                    anyRemaining = true;
                } else {
                    it.remove();
                    sendIdleNotify(p.window, p.pixmap, p.serial, p.idleFence);
                }
            } else {
                anyRemaining = true;
            }
        }
        if (anyRemaining) postChoreographerCallback();
    };

    private void postChoreographerCallback() {
        if (choreographer == null || choreographerPosted) return;
        choreographerPosted = true;
        choreographer.postFrameCallback(vsyncCallback);
    }

    private static class WindowTiming { long nextIdleNs = 0; }
    private final java.util.concurrent.ConcurrentHashMap<Integer, WindowTiming> windowTimings =
        new java.util.concurrent.ConcurrentHashMap<>();

    private void scheduleIdleNotify(Window window, Pixmap pixmap, int serial,
                                     int idleFence, int targetFps, VulkanRenderer renderer) {
        if (targetFps <= 0) {
            sendIdleNotify(window, pixmap, serial, idleFence);
            return;
        }

        final long frameNs = 1_000_000_000L / targetFps;
        long now = System.nanoTime();

        WindowTiming wt = windowTimings.computeIfAbsent(window.id, k -> new WindowTiming());
        if (wt.nextIdleNs <= now - frameNs) {
            wt.nextIdleNs = now + frameNs;
        } else {
            wt.nextIdleNs += frameNs;
        }
        long fireTime = wt.nextIdleNs - FIRE_EARLY_NS;

        android.view.Choreographer ch = tryGetChoreographer(renderer);
        if (ch != null) {
            pendingIdles.put(window.id,
                new PendingIdle(window, pixmap, serial, idleFence, fireTime, 0));
            postChoreographerCallback();
        } else {
            cpuQueue.offer(new PendingIdle(window, pixmap, serial, idleFence, fireTime, 0));
        }
    }


    private static abstract class ClientOpcodes {
        static final byte QUERY_VERSION = 0;
        static final byte PRESENT_PIXMAP = 1;
        static final byte SELECT_INPUT = 3;
    }

    private static class Event {
        Window window;
        XClient client;
        int id;
        Bitmask mask;
    }

    @Override public String getName() { return "Present"; }
    @Override public byte getMajorOpcode() { return MAJOR_OPCODE; }
    @Override public byte getFirstErrorId() { return 0; }
    @Override public byte getFirstEventId() { return 0; }

    private void sendIdleNotify(Window window, Pixmap pixmap, int serial, int idleFence) {
        if (idleFence != 0 && syncExtension != null)
            syncExtension.setTriggered(idleFence);
        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                Event e = events.valueAt(i);
                if (e.window == window && e.mask.isSet(PresentIdleNotify.getEventMask()))
                    e.client.sendEvent(new PresentIdleNotify(e.id, window, pixmap, serial, idleFence));
            }
        }
    }

    private void sendCompleteNotify(Window window, int serial, Kind kind, Mode mode, long ust, long msc) {
        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                Event e = events.valueAt(i);
                if (e.window == window && e.mask.isSet(PresentCompleteNotify.getEventMask()))
                    e.client.sendEvent(new PresentCompleteNotify(e.id, window, serial, kind, mode, ust, msc));
            }
        }
    }

    private void flushClientOutput(XClient client) {
        try {
            try (XStreamLock ignored = client.getOutputStream().lock()) {
            }
        } catch (IOException ignored) {}
    }

    private static void queryVersion(XClient client, XInputStream in, XOutputStream out) throws IOException {
        in.skip(8);
        try (XStreamLock lock = out.lock()) {
            out.writeByte(RESPONSE_CODE_SUCCESS);
            out.writeByte((byte)0);
            out.writeShort(client.getSequenceNumber());
            out.writeInt(0);
            out.writeInt(1);
            out.writeInt(0);
            out.writePad(16);
        }
    }

    private void presentPixmap(XClient client, XInputStream in, XOutputStream out)
            throws IOException, XRequestError {
        int windowId = in.readInt();
        int pixmapId = in.readInt();
        int serial   = in.readInt();
        in.skip(8);
        short xOff = in.readShort();
        short yOff = in.readShort();
        in.skip(8);
        int idleFence = in.readInt();
        in.skip(client.getRemainingRequestLength());

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        Pixmap pixmap = client.xServer.pixmapManager.getPixmap(pixmapId);
        if (pixmap == null) throw new BadPixmap(pixmapId);

        Drawable content = window.getContent();
        int contentDepth = content.visual.depth;
        int pixmapDepth = pixmap.drawable.visual.depth;
        boolean depthCompat = (contentDepth == pixmapDepth) ||
            ((contentDepth == 24 || contentDepth == 32) && (pixmapDepth == 24 || pixmapDepth == 32));
        if (!depthCompat) throw new BadMatch();

        VulkanRenderer renderer = client.xServer.getRenderer();
        int targetFps = renderer != null ? renderer.getFpsLimit() : 0;

        long ust = System.nanoTime() / 1000;
        long msc = ust / (targetFps > 0 ? (1_000_000L / targetFps) : (1_000_000L / 60));

        synchronized (content.renderLock) {
            boolean isNative = renderer != null && renderer.isNativeMode();

            if (isNative && pixmap.drawable.isDirectScanout()) {
                content.setTexture(pixmap.drawable.getTexture());
                content.setDirectScanout(true);
                sendCompleteNotify(window, serial, Kind.PIXMAP, Mode.FLIP, ust, msc);
                flushClientOutput(client);
                if (window.attributes.isMapped() && renderer != null)
                    renderer.onUpdateWindowContent(window);
                if (targetFps > 0) scheduleIdleNotify(window, pixmap, serial, idleFence, targetFps, renderer);
                else sendIdleNotify(window, pixmap, serial, idleFence);
            } else if (renderer != null && window.attributes.isMapped()) {
                sendCompleteNotify(window, serial, Kind.PIXMAP, Mode.COPY, ust, msc);
                flushClientOutput(client);
                renderer.onUpdateWindowContentDirect(window, pixmap.drawable, xOff, yOff);
                if (targetFps > 0) scheduleIdleNotify(window, pixmap, serial, idleFence, targetFps, renderer);
                else sendIdleNotify(window, pixmap, serial, idleFence);
            } else {
                content.copyArea((short)0, (short)0, xOff, yOff,
                    pixmap.drawable.width, pixmap.drawable.height, pixmap.drawable);
                sendCompleteNotify(window, serial, Kind.PIXMAP, Mode.COPY, ust, msc);
                flushClientOutput(client);
                scheduleIdleNotify(window, pixmap, serial, idleFence, targetFps, renderer);
            }
        }
    }

    private void selectInput(XClient client, XInputStream in, XOutputStream out)
            throws IOException, XRequestError {
        int eventId  = in.readInt();
        int windowId = in.readInt();
        Bitmask mask = new Bitmask(in.readInt());

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        if (GPUImage.isSupported() && !mask.isEmpty()) {
            Drawable content = window.getContent();
            final Texture oldTexture = content.getTexture();
            if (oldTexture != null && !(oldTexture instanceof GPUImage)) {
                VulkanRenderer r = client.xServer.getRenderer();
                if (r != null)
                    r.xServerView.queueEvent(oldTexture::destroy);
            }
            if (!(content.getTexture() instanceof GPUImage))
                content.setTexture(new GPUImage(content.width, content.height));
        }

        synchronized (events) {
            Event event = events.get(eventId);
            if (event != null) {
                if (event.window != window || event.client != client) throw new BadMatch();
                if (!mask.isEmpty()) event.mask = mask;
                else events.remove(eventId);
            } else {
                event = new Event();
                event.id     = eventId;
                event.window = window;
                event.client = client;
                event.mask   = mask;
                events.put(eventId, event);
            }
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream in, XOutputStream out)
            throws IOException, XRequestError {
        int opcode = client.getRequestData();
        if (syncExtension == null)
            syncExtension = client.xServer.getExtension(SyncExtension.MAJOR_OPCODE);

        switch (opcode) {
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, in, out);
                break;
            case ClientOpcodes.PRESENT_PIXMAP:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    presentPixmap(client, in, out);
                }
                break;
            case ClientOpcodes.SELECT_INPUT:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    selectInput(client, in, out);
                }
                break;
            default:
                throw new BadImplementation();
        }
    }
}
