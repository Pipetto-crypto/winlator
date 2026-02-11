package com.winlator.cmod.xserver;

import android.view.KeyEvent;
import com.winlator.cmod.xserver.XKeycode;
import com.winlator.cmod.xserver.XServer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class KeyInput {
    public static final XKeycode[] stubKeyCode = {XKeycode.KEY_CUSTOM_1, XKeycode.KEY_CUSTOM_2, XKeycode.KEY_CUSTOM_3, XKeycode.KEY_CUSTOM_4, XKeycode.KEY_CUSTOM_5, XKeycode.KEY_CUSTOM_6, XKeycode.KEY_CUSTOM_7, XKeycode.KEY_CUSTOM_8, XKeycode.KEY_CUSTOM_9, XKeycode.KEY_CUSTOM_10, XKeycode.KEY_CUSTOM_11, XKeycode.KEY_CUSTOM_12, XKeycode.KEY_CUSTOM_13, XKeycode.KEY_CUSTOM_14, XKeycode.KEY_CUSTOM_15, XKeycode.KEY_CUSTOM_16, XKeycode.KEY_CUSTOM_17};
    private static final AtomicInteger currIndex = new AtomicInteger(0);

    public static boolean handleAndroidKeyEvent(XServer xServer, KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == 2) {
            String characters = event.getCharacters();
            if (characters == null) {
                return false;
            }
            for (int i = 0; i < characters.codePointCount(0, characters.length()); i++) {
                int index = currIndex.getAndUpdate(new IntUnaryOperator() { 
                    @Override // java.util.function.IntUnaryOperator
                    public final int applyAsInt(int i2) {
                        return (i2 + 1) % stubKeyCode.length;
                    }
                });
                byte b = stubKeyCode[index].id;
                int keySym = characters.codePointAt(characters.offsetByCodePoints(0, i));
                if (keySym > 255) {
                    keySym |= 16777216;
                }
                xServer.injectKeyPress(stubKeyCode[index], keySym);
                sleep(10);
                xServer.injectKeyRelease(stubKeyCode[index]);
                sleep(10);
                handled = true;
            }
        }
        return handled;
    }

     private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}