package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import android.util.Log;

import com.winlator.cmod.xserver.Bitmask;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.XClient;
import com.winlator.cmod.xserver.XLock;
import com.winlator.cmod.xserver.XServer;
import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.errors.BadImplementation;
import com.winlator.cmod.xserver.errors.BadValue;
import com.winlator.cmod.xserver.errors.BadWindow;
import com.winlator.cmod.xserver.errors.XRequestError;
import com.winlator.cmod.xserver.events.XIRawButtonPressNotify;
import com.winlator.cmod.xserver.events.XIRawButtonReleaseNotify;
import com.winlator.cmod.xserver.events.XIRawMotionNotify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class XInput2Extension implements Extension {
    public static final byte MAJOR_OPCODE = -105;
    private byte firstEventId = 0;
    private byte firstErrorId = 0;

    private static final int XI_MAJOR = 2;
    private static final int XI_MINOR = 2;
    private static final int XI_ALL_DEVICES = 0;
    private static final int XI_ALL_MASTER_DEVICES = 1;
    private static final int MASTER_POINTER_ID = 2;
    private static final int MASTER_KEYBOARD_ID = 3;
    private static final int XI_BUTTON_CLASS = 1;
    private static final int XI_VALUATOR_CLASS = 2;
    private static final int XI_RawButtonPress_MASK = 1 << 15;
    private static final int XI_RawButtonRelease_MASK = 1 << 16;
    private static final int XI_RawMotion_MASK  = 1 << 17;
    private static final int RawMotion_XY_MASK = (1 << 0) | (1 << 1);
    private static final int POINTER_BUTTON_COUNT = 7;

    private final List<Selection> selections = new CopyOnWriteArrayList<>();

    private static abstract class ClientOpcodes {
        private static final byte GET_EXTENSION_VERSION = 1;  
        private static final byte GET_CLIENT_POINTER    = 45; 
        private static final byte SELECT_EVENTS         = 46; 
        private static final byte QUERY_VERSION         = 47; 
        private static final byte QUERY_DEVICE          = 48; 
    }

    private static class Selection {
        Window window;
        XClient client;
        int id;
        Bitmask mask;
        int deviceId;
    }

    @Override
    public String getName() {
        return "XInputExtension";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public int getNumEvents() { return 24; }

    @Override
    public int getNumErrors() { return 5; }

    @Override
    public void setFirstEventId(byte id) { this.firstEventId = id; }

    @Override
    public void setFirstErrorId(byte id) { this.firstErrorId = id; }

    @Override
    public byte getFirstEventId() { return firstEventId; }

    @Override
    public byte getFirstErrorId() { return firstErrorId; }

    private boolean isMasterDevice(int deviceId) {
        return deviceId == MASTER_POINTER_ID || deviceId == MASTER_KEYBOARD_ID;
    }

    private boolean matchesSelection(Selection sel, int deviceId) {
        return sel.deviceId == XI_ALL_DEVICES
                || (sel.deviceId == XI_ALL_MASTER_DEVICES && isMasterDevice(deviceId))
                || sel.deviceId == deviceId;
    }

    
     
    
     

    private static void getExtensionVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);

            outputStream.writeShort((short) 2);
            outputStream.writeShort((short) 0);
            outputStream.writeByte((byte) 1);
            outputStream.writePad(19);
        }
    }

    private static void getClientPointer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);

            outputStream.writeByte((byte) 1);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort((short) 2);

            outputStream.writePad(20);
        }
    }

    private static void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        short clientMajor = (short)(inputStream.readShort() & 0xFFFF);
        short clientMinor = (short)(inputStream.readShort() & 0xFFFF);

        inputStream.skip(client.getRemainingRequestLength());

        short negotiatedMajor;
        short negotiatedMinor;

        if (clientMajor < XI_MAJOR || (clientMajor == XI_MAJOR && clientMinor < XI_MINOR)) {
            negotiatedMajor = clientMajor;
            negotiatedMinor = clientMinor;
        } else {
            negotiatedMajor = XI_MAJOR;
            negotiatedMinor = XI_MINOR;
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);

            outputStream.writeShort(negotiatedMajor);
            outputStream.writeShort(negotiatedMinor);

            outputStream.writePad(20);
        }
    }

    private void writeButtonClass(XOutputStream outputStream, int sourceId, int numButtons) throws IOException {
        int stateBytes = Math.max(4, ((numButtons + 31) / 32) * 4);
        int labelsBytes = numButtons * 4;
        int totalBytes = 8 + stateBytes + labelsBytes;
        int length = totalBytes / 4;

        outputStream.writeShort((short) XI_BUTTON_CLASS);  
        outputStream.writeShort((short) length);         
        outputStream.writeShort((short) sourceId);         
        outputStream.writeShort((short) numButtons);        


        outputStream.writeInt(0);
        if (stateBytes > 4) {
            outputStream.writePad(stateBytes - 4);
        }

        for (int i = 0; i < numButtons; i++) {
            outputStream.writeInt(0);
        }
    }

    private void writeValuatorClass(XOutputStream outputStream, int axisNumber) throws IOException {
        outputStream.writeShort((short) XI_VALUATOR_CLASS); 
        outputStream.writeShort((short) 11);               
        outputStream.writeShort((short) MASTER_POINTER_ID); 
        outputStream.writeShort((short) axisNumber);        

        outputStream.writeInt(0); 

        outputStream.writeFP3232(0); 
        outputStream.writeFP3232(0); 

        outputStream.writeFP3232(0);

        outputStream.writeInt(0);

        outputStream.writeByte((byte) 0);
        outputStream.writePad(3);  
    }

    private void queryDevice(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(client.getRemainingRequestLength());

        final int masterPointerId = 2; 
        final String name = "Virtual Core Pointer";
        final byte[] nameBytes = name.getBytes();
        final int nameLen = nameBytes.length;
        final int namePad = (nameLen + 3) & ~3; 

        final int numButtons = POINTER_BUTTON_COUNT;
        final int buttonStateBytes = Math.max(4, ((numButtons + 31) / 32) * 4);
        final int buttonClassBytes = 8 + buttonStateBytes + (numButtons * 4);

        final int numValuators = 2;
        final int numClasses = 1 + numValuators; 

        int deviceInfoSize =
                12 + 
                namePad +
                buttonClassBytes +
                (44 * numValuators);

        int length = deviceInfoSize / 4; 

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);     
            outputStream.writeByte((byte)0);                   
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(length);                    
            outputStream.writeShort((short)1);                 
            outputStream.writePad(22);                  

            outputStream.writeShort((short)masterPointerId);
            outputStream.writeShort((short)1);              
            outputStream.writeShort((short)0);              
            outputStream.writeShort((short)numClasses);    
            outputStream.writeShort((short)nameLen);        
            outputStream.writeByte((byte)1);                 
            outputStream.writeByte((byte)0);                

            outputStream.write(nameBytes);
            outputStream.writePad(namePad - nameLen);

            writeButtonClass(outputStream, masterPointerId, numButtons);
            writeValuatorClass(outputStream, 0);
            writeValuatorClass(outputStream, 1);
        }
    }

    private void selectEvents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        int numMasks = inputStream.readShort() & 0xFFFF;

        if (numMasks == 0) {
            inputStream.skip(client.getRemainingRequestLength());
            throw new BadValue(numMasks);
        }

        inputStream.readShort();

        Window window = client.xServer.windowManager.getWindow(windowId);

        if (window == null) {
            inputStream.skip(client.getRemainingRequestLength());
            throw new BadWindow(windowId);
        }

        for (int i = 0; i < numMasks; i++) {
            int deviceId = inputStream.readShort() & 0xFFFF;
            int maskLen = inputStream.readShort() & 0xFFFF;

            Bitmask mask = new Bitmask(0);

            for (int word = 0; word < maskLen; word++) {
                long value = inputStream.readUnsignedInt();
                mask.set(value << (word * 32));
            }

            Selection sel = new Selection();
            sel.client = client;
            sel.window = window;
            sel.deviceId = deviceId;
            sel.mask = mask;
            sel.id = windowId;

            selections.removeIf(old ->
                    old.client == client &&
                            old.id == windowId &&
                            old.deviceId == deviceId);

            selections.add(sel);
        }

        inputStream.skip(client.getRemainingRequestLength());
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream)
            throws IOException, XRequestError {
        int opcode = client.getRequestData();

        switch (opcode) {
            case ClientOpcodes.GET_EXTENSION_VERSION:
                getExtensionVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_CLIENT_POINTER:
                getClientPointer(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SELECT_EVENTS:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    selectEvents(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_DEVICE:
                queryDevice(client, inputStream, outputStream);
                break;
            default:
                Log.w("XServer", String.format("XInput2Extension: unhandled minor opcode=%d, requestData=%d, requestLength=%d",
                        opcode, client.getRequestData(), client.getRemainingRequestLength()));
                inputStream.skip(client.getRemainingRequestLength());
                break;
        }
    }

    public void onClientDisconnected(XClient client) {
        selections.removeIf(sel -> sel.client == client);
    }

    public void emitRawMotion(int deviceId, double deltaX, double deltaY) {
        for (Selection sel : selections) {
            if (!matchesSelection(sel, deviceId)) continue;
            if (!sel.mask.isSet(XI_RawMotion_MASK)) continue;

            try {
                sendXIRawMotionToClient(sel.client, deviceId, deltaX, deltaY);
            } catch (IOException ignored) {
            }
        }
    }

    public void emitRawButton(int deviceId, int buttonNumber, boolean pressed) {
        int maskBit = pressed ? XI_RawButtonPress_MASK : XI_RawButtonRelease_MASK;

        for (Selection sel : selections) {
            if (!matchesSelection(sel, deviceId)) continue;
            if (!sel.mask.isSet(maskBit)) continue;

            try {
                sendXIRawButtonToClient(sel.client, deviceId, buttonNumber, pressed);
            } catch (IOException ignored) {
            }
        }
    }

    private void sendXIRawMotionToClient(XClient client, int deviceId, double deltaX, double deltaY) throws IOException {
        client.sendEvent(new XIRawMotionNotify(deviceId, MAJOR_OPCODE,
                new double[] {deltaX, deltaY},
                RawMotion_XY_MASK
        ));
    }

    private void sendXIRawButtonToClient(XClient client, int deviceId, int buttonNumber, boolean pressed) throws IOException {
        if (pressed) {
            client.sendEvent(new XIRawButtonPressNotify(deviceId, MAJOR_OPCODE, buttonNumber));
        } else {
            client.sendEvent(new XIRawButtonReleaseNotify(deviceId, MAJOR_OPCODE, buttonNumber));
        }
    }
}
