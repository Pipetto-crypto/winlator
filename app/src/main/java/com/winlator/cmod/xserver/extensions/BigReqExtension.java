package com.winlator.cmod.xserver.extensions;

import static com.winlator.cmod.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.cmod.xconnector.XInputStream;
import com.winlator.cmod.xconnector.XOutputStream;
import com.winlator.cmod.xconnector.XStreamLock;
import com.winlator.cmod.xserver.XClient;

import java.io.IOException;

public class BigReqExtension implements Extension {
    public static final byte MAJOR_OPCODE = -100;
    private static final int MAX_REQUEST_LENGTH = 4194303;
    private byte firstEventId = 0;
    private byte firstErrorId = 0;

    @Override
    public String getName() {
        return "BIG-REQUESTS";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public int getNumEvents() { return 0; }

    @Override
    public int getNumErrors() { return 0; }

    @Override
    public void setFirstEventId(byte id) { this.firstEventId = id; }

    @Override
    public void setFirstErrorId(byte id) { this.firstErrorId = id; }

    @Override
    public byte getFirstEventId() { return firstEventId; }

    @Override
    public byte getFirstErrorId() { return firstErrorId; }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(MAX_REQUEST_LENGTH);
            outputStream.writePad(20);
        }
    }
}
