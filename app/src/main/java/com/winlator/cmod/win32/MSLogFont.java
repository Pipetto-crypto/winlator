package com.winlator.cmod.win32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class MSLogFont {
    private int height = -11;
    private int width = 0;
    private int escapement = 0;
    private int orientation = 0;
    private int weight = 400;
    private byte italic = 0;
    private byte underline = 0;
    private byte strikeOut = 0;
    private byte charSet = 0;
    private byte outPrecision = 0;
    private byte clipPrecision = 0;
    private byte quality = 0;
    private byte pitchAndFamily = 34;
    private String faceName = "Tahoma";

    public MSLogFont setWeight(int weight) {
        this.weight = weight;
        return this;
    }

    public String getFaceName() {
        return this.faceName;
    }

    public MSLogFont setFaceName(String faceName) {
        this.faceName = faceName;
        return this;
    }

    public byte[] toByteArray() {
        ByteBuffer data = ByteBuffer.allocate(92).order(ByteOrder.LITTLE_ENDIAN);
        data.putInt(this.height);
        data.putInt(this.width);
        data.putInt(this.escapement);
        data.putInt(this.orientation);
        data.putInt(this.weight);
        data.put(this.italic);
        data.put(this.underline);
        data.put(this.strikeOut);
        data.put(this.charSet);
        data.put(this.outPrecision);
        data.put(this.clipPrecision);
        data.put(this.quality);
        data.put(this.pitchAndFamily);
        for (int i = 0; i < this.faceName.length(); i++) {
            data.putChar(this.faceName.charAt(i));
        }
        return data.array();
    }

    public MSLogFont fromByteArray(byte[] byteArray) {
        char chr;
        if (byteArray == null || byteArray.length < 92) {
            return this;
        }
        ByteBuffer data = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);
        this.height = data.getInt();
        this.width = data.getInt();
        this.escapement = data.getInt();
        this.orientation = data.getInt();
        this.weight = data.getInt();
        this.italic = data.get();
        this.underline = data.get();
        this.strikeOut = data.get();
        this.charSet = data.get();
        this.outPrecision = data.get();
        this.clipPrecision = data.get();
        this.quality = data.get();
        this.pitchAndFamily = data.get();
        StringBuilder sb = new StringBuilder();
        while (data.remaining() > 0 && (chr = data.getChar()) != 0) {
            sb.append(chr);
        }
        this.faceName = sb.toString();
        return this;
    }
}