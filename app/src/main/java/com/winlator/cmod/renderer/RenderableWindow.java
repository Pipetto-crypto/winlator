package com.winlator.cmod.renderer;

import com.winlator.cmod.xserver.Drawable;

public class RenderableWindow {
    public final Drawable content;
    public short rootX;
    public short rootY;

    public RenderableWindow(Drawable content, int rootX, int rootY) {
        this.content = content;
        this.rootX = (short) rootX;
        this.rootY = (short) rootY;
    }
}
