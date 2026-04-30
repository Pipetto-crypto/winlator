package com.winlator.cmod.xserver.events;

import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.Window;

import java.io.IOException;

public class XIRawButtonReleaseNotify extends Event {
    public static final int GENERIC_EVENT_CODE = 35;
    private static final short XI_RAWBUTTONRELEASE_EVTYPE = 16;

    private final byte extensionOpcode;
    private final int deviceId;
    private final int buttonNumber;

    public XIRawButtonReleaseNotify(int deviceId, byte extensionOpcode, int buttonNumber) {
        super(GENERIC_EVENT_CODE);
        this.deviceId = deviceId;
        this.extensionOpcode = extensionOpcode;
        this.buttonNumber = buttonNumber;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(this.code);                      
            outputStream.writeByte(extensionOpcode);                
            outputStream.writeShort(sequenceNumber);               
            outputStream.writeInt(0);                               

            outputStream.writeShort(XI_RAWBUTTONRELEASE_EVTYPE);    
            outputStream.writeShort((short) deviceId);              
            outputStream.writeInt((int) System.currentTimeMillis());
            outputStream.writeInt(buttonNumber);                    
            outputStream.writeShort((short) deviceId);              
            outputStream.writeShort((short) 0);                     
            outputStream.writeInt(0);                               
            outputStream.writePad(4);                               
        }
    }
}
