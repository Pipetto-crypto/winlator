package com.winlator.cmod.core;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import android.util.Log;

public abstract class MSBitmap {
    public static Bitmap open(File targetFile) {
        if (!targetFile.isFile()) return null;
        byte[] bytes = FileUtils.read(targetFile);
        if (bytes == null) return null;

        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getShort() != 0x4d42) return null;

        int fileSize = data.getInt();
        if (fileSize > targetFile.length()) return null;

        data.getInt();
        int dataOffset = data.getInt();
        int infoHeaderSize = data.getInt();
        int width = data.getInt();
        int height = data.getInt();
        short planes = data.getShort();
        short bitCount = data.getShort();
        int compression = data.getInt();
        int imageSize = data.getInt();
        int hr = data.getInt();
        int vr = data.getInt();
        int colorsUsed = data.getInt();
        int colorsImportant = data.getInt();

        if (width == 0 || height == 0) return null;

        boolean invertY = true;
        if (height < 0) {
            height *= -1;
            invertY = false;
        }

        ByteBuffer pixels = ByteBuffer.allocate(width * height * 4);
        byte r1 = 0, g1 = 0, b1 = 0, r2 = 0, g2 = 0, b2 = 0;
        boolean started = false;
        boolean blank = true;
        for (int y = height - 1, i = data.position(), j, line; y >= 0; y--) {
            line = invertY ? y : height - 1 - y;

            for (int x = 0; x < width; x++) {
                j = line * width * 4 + x * 4;
                b1 = data.get(i++);
                g1 = data.get(i++);
                r1 = data.get(i++);
                pixels.put(j+2, b1);
                pixels.put(j+1, g1);
                pixels.put(j+0, r1);
                pixels.put(j+3, (byte)255);

                if (!started) {
                    b2 = b1;
                    g2 = g1;
                    r2 = r1;
                    started = true;
                }
                else if (r1 != r2 || b1 != b2 || g1 != g2) {
                    blank = false;
                }
            }

            i += width % 4;
        }

        if (blank) return null;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(pixels);
        return bitmap;
    }

    public static Bitmap decodeBuffer(int width, int height, int bitCount, ByteBuffer data) {
        int height2;
        ByteBuffer byteBuffer = data;
        if (width == 0 || height == 0) {
            return null;
        }
        boolean invertY = true;
        if (height >= 0) {
            height2 = height;
        } else {
            height2 = height * (-1);
            invertY = false;
        }
        ByteBuffer pixels = ByteBuffer.allocate(width * height2 * 4);
        if (bitCount == 32) {
            int bytesPerPixel = 4;
            int rowStride = (width * bytesPerPixel + 3) & ~3;
            int padding = rowStride - width * bytesPerPixel;
            int i = data.position();
            for (int y = height2 - 1; y >= 0; y--) {
                int line = invertY ? y : (height2 - 1) - y;
                int x = 0;
                while (x < width) {
                    int i2 = i + 1;
                    byte b = byteBuffer.get(i);
                    int i3 = i2 + 1;
                    byte g = byteBuffer.get(i2);
                    int i4 = i3 + 1;
                    byte r = byteBuffer.get(i3);
                    int i5 = i4 + 1;
                    byte a = byteBuffer.get(i4);
                    int j = (line * width * 4) + (x * 4);
                    pixels.put(j + 2, b);
                    pixels.put(j + 1, g);
                    pixels.put(j + 0, r);
                    pixels.put(j + 3, a);
                    x++;
                    i = i5;
                }
                i += padding;
            }
        } else {
            if (bitCount == 24) {
                int bytesPerPixel = 3;
                int rowStride = (width * bytesPerPixel + 3) & ~3;
                int padding = rowStride - width * bytesPerPixel;
                int i6 = data.position();
                for (int y2 = height2 - 1; y2 >= 0; y2--) {
                    int line2 = invertY ? y2 : (height2 - 1) - y2;
                    int x2 = 0;
                    while (x2 < width) {
                        int i7 = i6 + 1;
                        byte b2 = byteBuffer.get(i6);
                        int i8 = i7 + 1;
                        byte g2 = byteBuffer.get(i7);
                        int i9 = i8 + 1;
                        byte r3 = byteBuffer.get(i8);
                        int j2 = (line2 * width * 4) + (x2 * 4);
                        pixels.put(j2 + 2, b2);
                        pixels.put(j2 + 1, g2);
                        pixels.put(j2 + 0, r3);
                        pixels.put(j2 + 3, (byte) 255);
                        x2++;
                        i6 = i9;
                    }
                    i6 += padding;
                }
            } else if (bitCount <= 8) {
                int bytesPerPixel = 1;
                int rowStride = (width * bytesPerPixel + 3) & ~3;
                int padding = rowStride - width * bytesPerPixel;
                int colorTableOffset = data.position();
                int colorTableSize = (int) (Math.pow(2.0d, bitCount) * 4.0d);
                int y3 = height2 - 1;
                int colorIndex = data.position() + colorTableSize;
                while (y3 >= 0) {
                    int line3 = invertY ? y3 : (height2 - 1) - y3;
                    int x3 = 0;
                    while (x3 < width) {
                        int i10 = colorIndex + 1;
                        int colorIndex2 = Byte.toUnsignedInt(byteBuffer.get(colorIndex)) * 4;
                        byte b3 = byteBuffer.get(colorTableOffset + colorIndex2 + 0);
                        byte g3 = byteBuffer.get(colorTableOffset + colorIndex2 + 1);
                        byte r4 = byteBuffer.get(colorTableOffset + colorIndex2 + 2);
                        int j3 = (line3 * width * 4) + (x3 * 4);
                        pixels.put(j3 + 2, b3);
                        pixels.put(j3 + 1, g3);
                        pixels.put(j3 + 0, r4);
                        pixels.put(j3 + 3, (byte) 255);
                        x3++;
                        colorIndex = i10;
                    }
                    colorIndex += padding;
                    y3--;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height2, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(pixels);
        return bitmap;
    }

    public static boolean create(Bitmap bitmap, File outputFile) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        int extraBytes = width % 4;
        int imageSize = height * (3 * width + extraBytes);
        int infoHeaderSize = 40;
        int dataOffset = 54;
        int bitCount = 24;
        int planes = 1;
        int compression = 0;
        int hr = 0;
        int vr = 0;
        int colorsUsed = 0;
        int colorsImportant = 0;
        
        ByteBuffer buffer = ByteBuffer.allocate(dataOffset + imageSize).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short)0x4d42); // "BM"
        buffer.putInt(dataOffset + imageSize);
        buffer.putInt(0);
        buffer.putInt(dataOffset);

        buffer.putInt(infoHeaderSize);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.putShort((short)planes);
        buffer.putShort((short)bitCount);
        buffer.putInt(compression);
        buffer.putInt(imageSize);
        buffer.putInt(hr);
        buffer.putInt(vr);
        buffer.putInt(colorsUsed);
        buffer.putInt(colorsImportant);

        int rowBytes = 3 * width + extraBytes;
        for (int y = height - 1, i = 0, j; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                j = dataOffset + y * rowBytes + x * 3;
                int pixel = pixels[i++];
                buffer.put(j+0, (byte)Color.blue(pixel));
                buffer.put(j+1, (byte)Color.green(pixel));
                buffer.put(j+2, (byte)Color.red(pixel));
            }

            if (extraBytes > 0) {
                int fillOffset = dataOffset + y * rowBytes + width * 3;
                for (j = fillOffset; j < fillOffset + extraBytes; j++) buffer.put(j, (byte)255);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(buffer.array());
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
