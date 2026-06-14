package com.winlator.cmod.renderer;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.winlator.cmod.XrActivity;
import com.winlator.cmod.xserver.Drawable;

import java.nio.ByteBuffer;

public class Texture {
    protected int textureId = 0;
    protected boolean needsUpdate = true;

    public void allocateTexture(short width, short height, ByteBuffer data) {
        textureId = nativeAllocateTexture(width, height, data);
    }
    
    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public void updateFromDrawable(Drawable drawable) {
        ByteBuffer data = drawable.getData();
        if (data == null) return;

        if (!isAllocated()) {
            allocateTexture(drawable.width, drawable.height, data);
        }
        else if (needsUpdate) {
            nativeUpdateTextureDrawable(textureId, drawable.width, drawable.height, data);
            needsUpdate = false;
        }
    }

    public boolean isAllocated() {
        return textureId > 0;
    }

    public int getTextureId() {
        return textureId;
    }

    public void destroy() {
        if (textureId > 0) {
            nativeDestroyTexture(textureId);
            textureId = 0;
        }
    }

    static {
        System.loadLibrary("winlator");
    }

    public native int nativeAllocateTexture(short width, short height, ByteBuffer data);
    public native void nativeUpdateTextureDrawable(int textureId, short width, short height, ByteBuffer data);
    public native void nativeDestroyTexture(int textureId);
}