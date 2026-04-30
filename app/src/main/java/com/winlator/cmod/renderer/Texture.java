package com.winlator.cmod.renderer;

import com.winlator.cmod.xserver.Drawable;

import java.nio.ByteBuffer;

public class Texture {
    protected int textureId = 0;
    protected boolean needsUpdate = true;

    public int getTextureId() { return textureId; }

    public boolean isAllocated() { return textureId != 0; }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public boolean needsUpdate() { return needsUpdate; }

    public void allocateTexture(short width, short height, ByteBuffer data) {}

    public void updateFromDrawable(Drawable drawable) {}

    public void destroy() {}
}
