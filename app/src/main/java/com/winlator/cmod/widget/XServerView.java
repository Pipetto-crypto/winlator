package com.winlator.cmod.widget;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import com.winlator.cmod.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.math.XForm;
import com.winlator.cmod.renderer.RenderableWindow;
import com.winlator.cmod.renderer.Texture;
import com.winlator.cmod.renderer.ViewTransformation;
import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.Cursor;
import com.winlator.cmod.xserver.Drawable;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowAttributes;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class XServerView extends SurfaceView implements SurfaceHolder.Callback, WindowManager.OnWindowModificationListener, Pointer.OnPointerMotionListener, Choreographer.FrameCallback {
    private XServer xServer;
    private Context context;
    private final Drawable rootCursorDrawable;
    private final ViewTransformation viewTransformation = new ViewTransformation();
    private int surfaceWidth;
    private int surfaceHeight;
    private boolean fullscreen = false;
    private String[] unviewableWMClasses = null;
    private boolean screenOffsetYRelativeToCursor = false;
    private boolean toggleFullscreen = false;
    private boolean magnifierEnabled = true;
    private boolean viewportNeedsUpdate = true;
    private float magnifierZoom = 1.0f;
    private final ArrayList<RenderableWindow> renderableWindows = new ArrayList<>();
    private final float[] tmpXForm1 = XForm.getInstance();
    private final float[] tmpXForm2 = XForm.getInstance();
    private boolean cursorVisible = true;
    private AtomicBoolean surfaceReady = new AtomicBoolean(false);
    private AtomicBoolean sceneDirty = new AtomicBoolean(false);
    private AtomicBoolean rendererRequested = new AtomicBoolean(false);
    private AtomicBoolean positionDirty = new AtomicBoolean(false);
    private final AtomicReference<Window> pendingWindowPosition = new AtomicReference<>();
    private Executor renderingThread = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    
    public XServerView(Context context, XServer xserver) {
        super(context);
        this.xServer = xserver;
        this.context = context;
        this.rootCursorDrawable = createRootCursorDrawable();
        getHolder().addCallback(this);
        xServer.windowManager.addOnWindowModificationListener(this);
        xServer.pointer.addOnPointerMotionListener(this);
    }
    
    @Override 
    public void doFrame(long nano) {
        if (sceneDirty.get()) {
            queueEvent(() -> updateScene());
            sceneDirty.set(false);
        }
        
        if (positionDirty.get()) {
            queueEvent(() -> updateWindowPosition(pendingWindowPosition.getAndSet(null)));
            positionDirty.set(false);
        }
            
        if (rendererRequested.get() && surfaceReady.get()) {
            queueEvent(() -> drawFrame());
            rendererRequested.set(false);
        }
        
        Choreographer.getInstance().postFrameCallback(this);
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        queueEvent(() -> {
            createSurface(holder.getSurface());
            surfaceReady.set(true);
            mHandler.post(() -> Choreographer.getInstance().postFrameCallback(this));
        });
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        queueEvent(() -> {
            destroySurface();
            surfaceReady.set(false);
            mHandler.post(() -> Choreographer.getInstance().removeFrameCallback(this));
        });
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        queueEvent(() -> {
            surfaceWidth = width;
            surfaceHeight = height;
            viewTransformation.update(width, height, xServer.screenInfo.width, xServer.screenInfo.height);
            viewportNeedsUpdate = true;
            mHandler.post(() -> Choreographer.getInstance().postFrameCallback(this));
        });
    }
    
    public void drawFrame() {
        if (toggleFullscreen) {
            fullscreen = !fullscreen;
            toggleFullscreen = false;
            viewportNeedsUpdate = true;
        }
        
        if (viewportNeedsUpdate && magnifierEnabled) {
            if (fullscreen) {
                updateViewport(0, 0, surfaceWidth, surfaceHeight);
            }
            else {
                updateViewport(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY, viewTransformation.viewWidth, viewTransformation.viewHeight);
            }
            viewportNeedsUpdate = false;
        }
        
        beginRendering(xServer.screenInfo.width, xServer.screenInfo.height);
        
        if (magnifierEnabled) {
            float pointerX = 0;
            float pointerY = 0;
            float magnifierZoom = !screenOffsetYRelativeToCursor ? this.magnifierZoom : 1.0f;

            if (magnifierZoom != 1.0f) {
                pointerX = Mathf.clamp(xServer.pointer.getX() * magnifierZoom - xServer.screenInfo.width * 0.5f, 0, xServer.screenInfo.width * Math.abs(1.0f - magnifierZoom));
            }

            if (screenOffsetYRelativeToCursor || magnifierZoom != 1.0f) {
                float scaleY = magnifierZoom != 1.0f ? Math.abs(1.0f - magnifierZoom) : 0.5f;
                float offsetY = xServer.screenInfo.height * (screenOffsetYRelativeToCursor ? 0.25f : 0.5f);
                pointerY = Mathf.clamp(xServer.pointer.getY() * magnifierZoom - offsetY, 0, xServer.screenInfo.height * scaleY);
            }

            XForm.makeTransform(tmpXForm2, -pointerX, -pointerY, magnifierZoom, magnifierZoom, 0);
        } else {
            if (!fullscreen) {
                int pointerY = 0;
                if (screenOffsetYRelativeToCursor) {
                    short halfScreenHeight = (short)(xServer.screenInfo.height / 2);
                    pointerY = Mathf.clamp(xServer.pointer.getY() - halfScreenHeight / 2, 0, halfScreenHeight);
                }

                XForm.makeTransform(tmpXForm2, viewTransformation.sceneOffsetX, viewTransformation.sceneOffsetY - pointerY, viewTransformation.sceneScaleX, viewTransformation.sceneScaleY, 0);

                updateScissor(1, viewTransformation.viewOffsetX, viewTransformation.viewOffsetY, viewTransformation.viewWidth, viewTransformation.viewHeight);
            } else {
                XForm.identity(tmpXForm2);
            }
        }
        
        renderWindows();
        if (cursorVisible) renderCursor();
        
        finishRendering();
        
        if (!magnifierEnabled && !fullscreen) {
            updateScissor(0, 0, 0, 0, 0);
        }
    }
    
    private void renderWindows() {
        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            for (RenderableWindow window : renderableWindows) {
                renderDrawable(window.content, window.rootX, window.rootY, true);
            }
        }
    }
    
    private void renderCursor() {
        try (XLock lock = xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
            Window pointWindow = xServer.inputDeviceManager.getPointWindow();
            Cursor cursor = pointWindow != null ? pointWindow.attributes.getCursor() : null;
            short x = xServer.pointer.getClampedX();
            short y = xServer.pointer.getClampedY();

            if (cursor != null) {
                if (cursor.isVisible()) renderDrawable(cursor.cursorImage, x - cursor.hotSpotX, y - cursor.hotSpotY, false);
            }
            else renderDrawable(rootCursorDrawable, x, y, false);
        }
    }
    
    private void renderDrawable(Drawable drawable, int x, int y, boolean isWindow) {
        if (drawable == null) return;
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            texture.updateFromDrawable(drawable);
            XForm.set(tmpXForm1, x, y, drawable.width, drawable.height);
            XForm.multiply(tmpXForm1, tmpXForm1, tmpXForm2);
            
            renderDrawable(texture.getTextureId(), tmpXForm1, isWindow);
        }
    }
    
    private Drawable createRootCursorDrawable() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor, options);
        return Drawable.fromBitmap(bitmap);
    }
    
    private void updateScene() {
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
            renderableWindows.clear();
            collectRenderableWindows(xServer.windowManager.rootWindow, xServer.windowManager.rootWindow.getX(), xServer.windowManager.rootWindow.getY());
        }
    }

    private void collectRenderableWindows(Window window, int x, int y) {
        if (!window.attributes.isMapped()) return;
        if (window != xServer.windowManager.rootWindow) {
            boolean viewable = true;

            if (unviewableWMClasses != null) {
                String wmClass = window.getClassName();
                for (String unviewableWMClass : unviewableWMClasses) {
                    if (wmClass.contains(unviewableWMClass)) {
                        if (window.attributes.isEnabled()) window.disableAllDescendants();
                        viewable = false;
                        break;
                    }
                }
            }

            if (viewable)
                renderableWindows.add(new RenderableWindow(window.getContent(), x, y));
        }

        for (Window child : window.getChildren()) {
            collectRenderableWindows(child, child.getX() + x, child.getY() + y);
        }
    }

    private void removeRenderableWindow(Window window) {
        for (int i = 0; i < renderableWindows.size(); i++) {
            if (renderableWindows.get(i).content == window.getContent()) {
                renderableWindows.remove(i);
                break;
            }
        }
    }
    
    private void updateWindowPosition(Window window) {
        for (RenderableWindow renderableWindow : renderableWindows) {
            if (renderableWindow.content == window.getContent()) {
                renderableWindow.rootX = window.getRootX();
                renderableWindow.rootY = window.getRootY();
                break;
            }
        }
    }
    
    public void toggleFullscreen() {
        toggleFullscreen = true;
        requestRenderer();
    }
    
    public void setCursorVisible(boolean cursorVisible) {
        this.cursorVisible = cursorVisible;
        requestRenderer();
    }

    public boolean isCursorVisible() {
        return cursorVisible;
    }

    public boolean isScreenOffsetYRelativeToCursor() {
        return screenOffsetYRelativeToCursor;
    }

    public void setScreenOffsetYRelativeToCursor(boolean screenOffsetYRelativeToCursor) {
        this.screenOffsetYRelativeToCursor = screenOffsetYRelativeToCursor;
        requestRenderer();
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public float getMagnifierZoom() {
        return magnifierZoom;
    }

    public void setMagnifierZoom(float magnifierZoom) {
        this.magnifierZoom = magnifierZoom;
        requestRenderer();
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    public boolean isViewportNeedsUpdate() {
        return viewportNeedsUpdate;
    }

    public void setViewportNeedsUpdate(boolean viewportNeedsUpdate) {
        this.viewportNeedsUpdate = viewportNeedsUpdate;
    }
    
    public void setUnviewableWMClasses(String... unviewableWMNames) {
        this.unviewableWMClasses = unviewableWMNames;
    }

    @Override
    public void onMapWindow(Window window) {
        sceneDirty.set(true);
        requestRenderer();
    }

    @Override
    public void onUnmapWindow(Window window) {
        sceneDirty.set(true);
        requestRenderer();
    }

    @Override
    public void onChangeWindowZOrder(Window window) {
        sceneDirty.set(true);
        requestRenderer();
    }

    @Override
    public void onUpdateWindowContent(Window window) {
        requestRenderer();
    }

    @Override
    public void onUpdateWindowGeometry(final Window window, boolean resized) {
        if (resized) {
            sceneDirty.set(true);
        }
        else {
        	positionDirty.set(true);
        	sceneDirty.set(true);
            pendingWindowPosition.set(window);
        }
        requestRenderer();
    }

    @Override
    public void onUpdateWindowAttributes(Window window, Bitmask mask) {
        if (mask.isSet(WindowAttributes.FLAG_CURSOR)) 
            requestRenderer();
    }
    
    
    @Override
    public void onPointerMove(short x, short y) {
        requestRenderer();
    }
    
    public void queueEvent(Runnable r) {
        renderingThread.execute(r);
    }
    
    public void requestRenderer() {
        rendererRequested.set(true);
    }
    
    static {
        System.loadLibrary("winlator");
    }

    public native void createSurface(Surface surface);
    public native void beginRendering(int width, int height);
    public native void updateViewport(int x, int y, int width, int height);
    public native void updateScissor(int active, int x, int y, int width, int height);
    public native void renderDrawable(int textureId, float[] xform, boolean isFromWindow);
    public native void finishRendering();
    public native void destroySurface();
}
