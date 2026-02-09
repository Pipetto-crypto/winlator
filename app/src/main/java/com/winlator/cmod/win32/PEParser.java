package com.winlator.cmod.win32;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.winlator.cmod.core.ImageUtils;
import com.winlator.cmod.core.StreamUtils;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.MSBitmap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import android.util.Log;
import com.winlator.cmod.core.FileUtils;

/* loaded from: classes.dex */
public class PEParser {
    private final File peFile;
    private int resourcesRVA = 0;
    private int resourcesOffset = 0;
    
    private interface ImageResourceEntry {
    }

    private static class ImageResourceDirectoryEntry implements ImageResourceEntry {
        private final boolean dataIsDirectory;
        private ImageResourceDirectory directory;
        private final int name;
        private final boolean nameIsString;
        private final int offsetToData;

        private ImageResourceDirectoryEntry(ByteBuffer data) {
            int field1 = data.getInt();
            int field2 = data.getInt();
            this.name = field1 & Integer.MAX_VALUE;
            this.nameIsString = ((field1 >> 31) & 1) != 0;
            this.offsetToData = Integer.MAX_VALUE & field2;
            this.dataIsDirectory = ((field2 >> 31) & 1) != 0;
        }
    }

    private static class ImageResourceDataEntry implements ImageResourceEntry {
        private final int codePage;
        private final int offsetToData;
        private final int reserved;
        private final int size;

        private ImageResourceDataEntry(ByteBuffer data) {
            this.offsetToData = data.getInt();
            this.size = data.getInt();
            this.codePage = data.getInt();
            this.reserved = data.getInt();
        }
    }

    private static class ImageResourceDirectory {
        private final int characteristics;
        private final ArrayList<ImageResourceEntry> entries;
        private final short majorVersion;
        private final short minorVersion;
        private final short numberOfIdEntries;
        private final short numberOfNamedEntries;
        private final int timeDateStamp;

        private ImageResourceDirectory(ByteBuffer data, int level) {
            this.entries = new ArrayList<>();
            this.characteristics = data.getInt();
            this.timeDateStamp = data.getInt();
            this.majorVersion = data.getShort();
            this.minorVersion = data.getShort();
            short s = data.getShort();
            this.numberOfNamedEntries = s;
            short s2 = data.getShort();
            this.numberOfIdEntries = s2;
            int numberOfEntries = s + s2;
            for (int i = 0; i < numberOfEntries; i++) {
                ImageResourceDirectoryEntry directoryEntry = new ImageResourceDirectoryEntry(data);
                if ((directoryEntry.name == 3 && directoryEntry.dataIsDirectory) || (level > 0 && directoryEntry.dataIsDirectory)) {
                    int oldPosition = data.position();
                    data.position(directoryEntry.offsetToData);
                    directoryEntry.directory = new ImageResourceDirectory(data, level + 1);
                    data.position(oldPosition);
                    this.entries.add(0, directoryEntry);
                } else if (level > 0) {
                    int oldPosition2 = data.position();
                    data.position(directoryEntry.offsetToData);
                    ImageResourceDataEntry dataEntry = new ImageResourceDataEntry(data);
                    data.position(oldPosition2);
                    this.entries.add(0, dataEntry);
                }
            }
        }
    }

    private PEParser(File peFile) {
        this.peFile = peFile;
    }

    private ByteBuffer readIconData(int iconOffset, int iconSize) throws IOException {
        InputStream inStream = null;
        try {
            inStream = new BufferedInputStream(new FileInputStream(this.peFile), 65536);
            byte[] iconBytes = new byte[iconSize];
            StreamUtils.skip(inStream, iconOffset);
            int bytesRead = inStream.read(iconBytes);
            return bytesRead != -1 ? ByteBuffer.wrap(iconBytes).order(ByteOrder.LITTLE_ENDIAN) : null;
        } catch (IOException e) {
            return null;
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    // Ignore close exception
                }
            }
        }
    }

    private ImageResourceDirectory readImageResourceDirectory() throws IOException {
        InputStream inStream = null;
        try {
            inStream = new BufferedInputStream(new FileInputStream(this.peFile), 65536);
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(64);
            ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
            ByteBuffer dosHeader = byteBufferAllocate.order(byteOrder);
            int filePosition = 0 + inStream.read(dosHeader.array());
            short magicNumber = dosHeader.getShort();
            if (magicNumber == 23117) {
                dosHeader.position(60);
                int fileHeaderOffset = dosHeader.getInt() + 4;
                int filePosition2 = filePosition + StreamUtils.skip(inStream, fileHeaderOffset - filePosition);
                ByteBuffer fileHeader = ByteBuffer.allocate(20).order(byteOrder);
                int filePosition3 = filePosition2 + inStream.read(fileHeader.array());
                Short.toUnsignedInt(fileHeader.getShort());
                short numberOfSections = fileHeader.getShort();
                fileHeader.position(fileHeader.position() + 12);
                short sizeofOptionalHeader = fileHeader.getShort();
                int filePosition4 = filePosition3 + StreamUtils.skip(inStream, sizeofOptionalHeader);
                int i = 0;
                this.resourcesRVA = 0;
                this.resourcesOffset = 0;
                int resourcesSize = 0;
                ByteBuffer sectionHeader = ByteBuffer.allocate(40).order(byteOrder);
                byte[] nameBytes = new byte[8];
                byte i2 = 0;
                while (true) {
                    if (i2 >= numberOfSections) {
                        break;
                    }
                    sectionHeader.position(i);
                    filePosition4 += inStream.read(sectionHeader.array());
                    sectionHeader.get(nameBytes);
                    String name = StringUtils.fromANSIString(nameBytes);
                    if (!name.equals(".rsrc")&&!name.equals(".RSRC")) {
                        i2 = (byte) (i2 + 1);
                        i = 0;
                    } else {
                        sectionHeader.getInt();
                        this.resourcesRVA = sectionHeader.getInt();
                        resourcesSize = sectionHeader.getInt();
                        this.resourcesOffset = sectionHeader.getInt();
                        break;
                    }
                }
                int i3 = this.resourcesOffset;
                if (i3 > 0) {
                    int iSkip = filePosition4 + StreamUtils.skip(inStream, i3 - filePosition4);
                    ByteBuffer resourcesBuffer = ByteBuffer.allocate(resourcesSize).order(ByteOrder.LITTLE_ENDIAN);
                    inStream.read(resourcesBuffer.array(), 0, resourcesBuffer.limit());
                    ImageResourceDirectory imageResourceDirectory = new ImageResourceDirectory(resourcesBuffer, 0);
                    return imageResourceDirectory;
                }
                return null;
            }
            return null;
        } catch (IOException e) {
            Log.e("PEParser", "Error reading image resource directory", e);
            throw e;
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    Log.e("PEParser", "Error closing input stream", e);
                }
            }
        }
    }
    private Bitmap decodeIcon(int iconIndex, boolean largeIcon, ArrayList<ImageResourceDataEntry> dataEntries) throws IOException {
        for (int i = 0; i < dataEntries.size(); i++) {
            ImageResourceDataEntry dataEntry = dataEntries.get(i);
            int fileOffset = (dataEntry.offsetToData - this.resourcesRVA) + this.resourcesOffset;
            ByteBuffer iconData = readIconData(fileOffset, dataEntry.size);
            if (iconData != null) {
                if (ImageUtils.isPNGData(iconData)) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(iconData.array(), 0, iconData.limit(), options);
                    
                    // If looking for specific icon index, match it
                    if (iconIndex >= 0) {
                        if (i == iconIndex) {
                            return BitmapFactory.decodeByteArray(iconData.array(), 0, iconData.limit());
                        }
                    } else {
                        // Otherwise select based on size preference
                        if (largeIcon == (options.outWidth >= 32)) {
                            return BitmapFactory.decodeByteArray(iconData.array(), 0, iconData.limit());
                        }
                    }
                } else {
                    // BMP icon format - check buffer size before reading
                    if (iconData.remaining() >= 40) {
                        int bitmapOffset = iconData.getInt();
                        int bmpWidth = iconData.getInt();
                        iconData.getInt();
                        iconData.getShort();
                        short bitCount = iconData.getShort();
                        int compression = iconData.getInt();
                        iconData.getInt();
                        iconData.getInt();
                        iconData.getInt();
                        int clrUsed = iconData.getInt();
                        
                        // Skip unsupported formats: 8-bit or 32-bit uncompressed images
                        // boolean isUnsupported = (bitCount != 8&& bitCount != 24 && bitCount != 32) || (compression != 0 || clrUsed != 0);
                        boolean isUnsupported = (bitCount != 8&& bitCount != 24 && bitCount != 32) || (compression != 0);
                        if (!isUnsupported) {
                            if (iconIndex < 0) {
                                if (largeIcon == (bmpWidth >= 32)) {
                                    iconData.position(bitmapOffset);
                                    Bitmap bitmap = MSBitmap.decodeBuffer(bmpWidth, bmpWidth, bitCount, iconData);
                                    if (bitmap != null) {
                                        return bitmap;
                                    }
                                }
                            } else if (i == iconIndex) {
                                // Decode if it has at least 8-bit color depth
                                if (bitCount >= 8) {
                                    iconData.position(bitmapOffset);
                                    Bitmap bitmap = MSBitmap.decodeBuffer(bmpWidth, bmpWidth, bitCount, iconData);
                                    if (bitmap != null) {
                                        return bitmap;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Bitmap extractIcon(int iconIndex) throws IOException {
        ImageResourceDirectory rootDirectory;
        if (!this.peFile.isFile() || (rootDirectory = readImageResourceDirectory()) == null) {
            return null;
        }
        ArrayList<ImageResourceDataEntry> dataEntries = new ArrayList<>();
        Stack<ImageResourceDirectory> stack = new Stack<>();
        stack.push(rootDirectory);
        while (!stack.isEmpty()) {
            ImageResourceDirectory directory = stack.pop();
            Iterator it = directory.entries.iterator();
            while (it.hasNext()) {
                ImageResourceEntry entry = (ImageResourceEntry) it.next();
                if (entry instanceof ImageResourceDirectoryEntry) {
                    stack.push(((ImageResourceDirectoryEntry) entry).directory);
                } else if (entry instanceof ImageResourceDataEntry) {
                    dataEntries.add((ImageResourceDataEntry) entry);
                }
            }
        }
        if (iconIndex < 0) {
            Bitmap bitmap = decodeIcon(-1, true, dataEntries);
            if (bitmap != null) {
                return bitmap;
            }
            Bitmap bitmap2 = decodeIcon(-1, false, dataEntries);
            if (bitmap2 != null) {
                return bitmap2;
            }
            return null;
        }
        return decodeIcon(iconIndex, true, dataEntries);
    }

    public static Bitmap extractIcon(File peFile) {
        return extractIcon(peFile, -1);
    }

    public static Bitmap extractIcon(File peFile, int iconIndex) {
        try {
            return new PEParser(peFile).extractIcon(iconIndex);
        } catch (IOException e) {
            return null;
        }
    }
}