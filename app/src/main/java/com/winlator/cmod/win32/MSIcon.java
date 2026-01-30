package com.winlator.cmod.win32;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ImageUtils;
import com.winlator.cmod.core.MSBitmap;
import com.winlator.cmod.win32.MSIcon;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/* loaded from: classes.dex */
public abstract class MSIcon {

    /* JADX INFO: Access modifiers changed from: private */
    private static class IconDirEntry {
        private short bitCount;
        private short colorPlanes;
        private int height;
        private int imageOffset;
        private int imageSize;
        private byte numberOfColors;
        private byte reserved;
        private int width;

        private IconDirEntry() {
        }
    }

    public static Bitmap decodeByteArray(byte[] bytes, int offset, int length) {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        data.position(offset);
        if (ImageUtils.isPNGData(data)) {
            return BitmapFactory.decodeByteArray(bytes, offset, length);
        }
        int bitmapOffset = data.getInt();
        int bmpWidth = data.getInt();
        data.getInt();
        data.getShort();
        short bitCount = data.getShort();
        data.position(offset + bitmapOffset);
        return MSBitmap.decodeBuffer(bmpWidth, bmpWidth, bitCount, data);
    }

    public static Bitmap decodeFile(File icoFile) {
        byte[] bytes;
        if (!icoFile.isFile() || (bytes = FileUtils.read(icoFile)) == null) {
            return null;
        }
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        short reserved = data.getShort();
        short imageType = data.getShort();
        short numberOfImages = data.getShort();
        if (reserved != 0 || imageType != 1 || numberOfImages <= 0 || numberOfImages >= 32) {
            return null;
        }
        ArrayList<IconDirEntry> entries = new ArrayList<>();
        for (byte i = 0; i < numberOfImages; i = (byte) (i + 1)) {
            IconDirEntry entry = new IconDirEntry();
            entry.width = Byte.toUnsignedInt(data.get());
            entry.height = Byte.toUnsignedInt(data.get());
            entry.numberOfColors = data.get();
            entry.reserved = data.get();
            entry.colorPlanes = data.getShort();
            entry.bitCount = data.getShort();
            entry.imageSize = data.getInt();
            entry.imageOffset = data.getInt();
            entries.add(entry);
        }
        Collections.sort(entries, new Comparator() {
            @Override
            public int compare(Object obj, Object obj2) {
                IconDirEntry iconDirEntry = (IconDirEntry) obj;
                IconDirEntry iconDirEntry2 = (IconDirEntry) obj;
                int value = Short.compare(iconDirEntry2.bitCount, iconDirEntry.bitCount);
                return value != 0 ? value : Integer.compare(iconDirEntry2.width, iconDirEntry.width);   
            }
        });
        IconDirEntry firstEntry = entries.get(0);
        return decodeByteArray(data.array(), firstEntry.imageOffset, firstEntry.imageSize);
    }
}