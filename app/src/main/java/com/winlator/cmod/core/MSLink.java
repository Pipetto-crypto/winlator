package com.winlator.cmod.core;

import android.content.Context;

import com.winlator.cmod.xenvironment.ImageFs;
import android.util.Log;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public abstract class MSLink {
    public static final byte SW_SHOWNORMAL = 1;
    public static final byte SW_SHOWMAXIMIZED = 3;
    public static final byte SW_SHOWMINNOACTIVE = 7;
    private static final int HasLinkTargetIDList = 1<<0;
    private static final int HasArguments = 1<<5;
    private static final int HasIconLocation = 1<<6;
    private static final int ForceNoLinkInfo = 1<<8;

    public static final class Options {
        public String arguments;
        public int fileSize;
        public int iconIndex;
        public String iconLocation;
        public boolean isDirectory;
        public String targetPath;
        public String cmdArgs;
        public int showCommand = SW_SHOWNORMAL;
    }

    private static int charToHexDigit(char chr) {
        return chr >= 'A' ? chr - 'A' + 10 : chr - '0';
    }

    private static byte twoCharsToByte(char chr1, char chr2) {
        return (byte)(charToHexDigit(Character.toUpperCase(chr1)) * 16 + charToHexDigit(Character.toUpperCase(chr2)));
    }

    private static byte[] convertCLSIDtoDATA(String str) {
        return new byte[]{
            twoCharsToByte(str.charAt(6), str.charAt(7)),
            twoCharsToByte(str.charAt(4), str.charAt(5)),
            twoCharsToByte(str.charAt(2), str.charAt(3)),
            twoCharsToByte(str.charAt(0), str.charAt(1)),
            twoCharsToByte(str.charAt(11), str.charAt(12)),
            twoCharsToByte(str.charAt(9), str.charAt(10)),
            twoCharsToByte(str.charAt(16), str.charAt(17)),
            twoCharsToByte(str.charAt(14), str.charAt(15)),
            twoCharsToByte(str.charAt(19), str.charAt(20)),
            twoCharsToByte(str.charAt(21), str.charAt(22)),
            twoCharsToByte(str.charAt(24), str.charAt(25)),
            twoCharsToByte(str.charAt(26), str.charAt(27)),
            twoCharsToByte(str.charAt(28), str.charAt(29)),
            twoCharsToByte(str.charAt(30), str.charAt(31)),
            twoCharsToByte(str.charAt(32), str.charAt(33)),
            twoCharsToByte(str.charAt(34), str.charAt(35))
        };
    }

    private static byte[] stringToByteArray(String str) {
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte)str.charAt(i);
        return bytes;
    }

    private static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static String readStringData(ByteBuffer data, boolean isUnicode) {
        short CountCharacters = data.getShort();
        if (CountCharacters == 0) {
            return null;
        }
        byte[] bytes = new byte[(isUnicode ? (short) 2 : (short) 1) * CountCharacters];
        data.get(bytes);
        String string = isUnicode ? new String(bytes, StandardCharsets.UTF_16LE) : new String(bytes);
        int indexOfNull = string.indexOf(0);
        return indexOfNull != -1 ? string.substring(0, indexOfNull) : string;
    }

    private static String readNullTerminatedString(ByteBuffer data) {
        byte[] bytes = new byte[256];
        int i = 0;
        while (true) {
            byte value = data.get();
            if (value == 0) {
                return new String(Arrays.copyOf(bytes, i));
            }
            bytes[i] = value;
            i++;
        }
    }

    private static byte[] generateStringData(String str) {
        ByteBuffer buffer = ByteBuffer.allocate(str.length() + 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) str.length());
        for (int i = 0; i < str.length(); i++) {
            buffer.put((byte) str.charAt(i));
        }
        return buffer.array();
    }

    private static byte[] stringSizePaddedToByteArray(String str) {
        ByteBuffer buffer = ByteBuffer.allocate(str.length() + 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short)str.length());
        for (int i = 0; i < str.length(); i++) buffer.put((byte)str.charAt(i));
        return buffer.array();
    }

    private static byte[] generateIDLIST(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)(bytes.length + 2));
        return ArrayUtils.concat(buffer.array(), bytes);
    }

    public static Boolean createFile(String targetPath, File outputFile) {
        Options options = new Options();
        options.targetPath = targetPath;
        return createFile(options, outputFile);
    }

    public static Boolean createFile(Options options, File outputFile) {
        byte[] HeaderSize = new byte[]{0x4c, 0x00, 0x00, 0x00};
        byte[] LinkCLSID = convertCLSIDtoDATA("00021401-0000-0000-c000-000000000046");

        int linkFlags = HasLinkTargetIDList | ForceNoLinkInfo;
        if (options.cmdArgs != null && !options.cmdArgs.isEmpty()) linkFlags |= HasArguments;
        if (options.iconLocation != null && !options.iconLocation.isEmpty()) linkFlags |= HasIconLocation;

        byte[] LinkFlags = intToByteArray(linkFlags);

        byte[] FileAttributes, prefixOfTarget;
        options.targetPath = options.targetPath.replaceAll("/+", "\\\\");
        if (options.targetPath.endsWith("\\")) {
            FileAttributes = new byte[]{0x10, 0x00, 0x00, 0x00};
            prefixOfTarget = new byte[]{0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            options.targetPath = options.targetPath.replaceAll("\\\\+$", "");
        }
        else {
            FileAttributes = new byte[]{0x20, 0x00, 0x00, 0x00};
            prefixOfTarget = new byte[]{0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }

        byte[] CreationTime, AccessTime, WriteTime;
        CreationTime = AccessTime = WriteTime = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] FileSize = intToByteArray(options.fileSize);
        byte[] IconIndex = intToByteArray(options.iconIndex);
        byte[] ShowCommand = intToByteArray(options.showCommand);
        byte[] Hotkey = new byte[]{0x00, 0x00};
        byte[] Reserved1 = new byte[]{0x00, 0x00};
        byte[] Reserved2 = new byte[]{0x00, 0x00, 0x00, 0x00};
        byte[] Reserved3 = new byte[]{0x00, 0x00, 0x00, 0x00};

        byte[] CLSIDComputer = convertCLSIDtoDATA("20d04fe0-3aea-1069-a2d8-08002b30309d");
        byte[] CLSIDNetwork = convertCLSIDtoDATA("208d2c60-3aea-1069-a2d7-08002b30309d");

        byte[] itemData, prefixRoot, targetRoot, targetLeaf;
        if (options.targetPath.startsWith("\\")) {
            prefixRoot = new byte[]{(byte)0xc3, 0x01, (byte)0x81};
            targetRoot = stringToByteArray(options.targetPath);
            targetLeaf = !options.targetPath.endsWith("\\") ? stringToByteArray(options.targetPath.substring(options.targetPath.lastIndexOf("\\") + 1)) : null;
            itemData = ArrayUtils.concat(new byte[]{0x1f, 0x58}, CLSIDNetwork);
        }
        else {
            prefixRoot = new byte[]{0x2f};
            int index = options.targetPath.indexOf("\\");
            targetRoot = stringToByteArray(options.targetPath.substring(0, index+1));
            targetLeaf = stringToByteArray(options.targetPath.substring(index+1));
            itemData = ArrayUtils.concat(new byte[]{0x1f, 0x50}, CLSIDComputer);
        }

        targetRoot = ArrayUtils.concat(targetRoot, new byte[21]);

        byte[] endOfString = new byte[]{0x00};
        byte[] IDListItems = ArrayUtils.concat(generateIDLIST(itemData), generateIDLIST(ArrayUtils.concat(prefixRoot, targetRoot, endOfString)));
        if (targetLeaf != null) IDListItems = ArrayUtils.concat(IDListItems, generateIDLIST(ArrayUtils.concat(prefixOfTarget, targetLeaf, endOfString)));
        byte[] IDList = generateIDLIST(IDListItems);

        byte[] TerminalID = new byte[]{0x00, 0x00};

        byte[] StringData = new byte[0];
        if ((linkFlags & HasArguments) != 0) StringData = ArrayUtils.concat(StringData, stringSizePaddedToByteArray(options.cmdArgs));
        if ((linkFlags & HasIconLocation) != 0) StringData = ArrayUtils.concat(StringData, stringSizePaddedToByteArray(options.iconLocation));

        try (FileOutputStream os = new FileOutputStream(outputFile)) {
            os.write(HeaderSize);
            os.write(LinkCLSID);
            os.write(LinkFlags);
            os.write(FileAttributes);
            os.write(CreationTime);
            os.write(AccessTime);
            os.write(WriteTime);
            os.write(FileSize);
            os.write(IconIndex);
            os.write(ShowCommand);
            os.write(Hotkey);
            os.write(Reserved1);
            os.write(Reserved2);
            os.write(Reserved3);
            os.write(IDList);
            os.write(TerminalID);

            if (StringData.length > 0){
                os.write(StringData);
                os.close();
                return true;
            } 
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static String parseFilePath(File lnkFile) {
        String filePath = "";
        try {
            int linkFlags, linkInfoStart;
            FileInputStream fis = new FileInputStream(lnkFile);
            byte[] bytes = new byte[(int) lnkFile.length()];
            DataInputStream dis = new DataInputStream(fis);
            dis.readFully(bytes);
            ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            linkFlags = data.getInt(0x14);
            if ((linkFlags & (1 << 0)) != 0) {
                short linkInfoTargetIdListSize = data.getShort(0x4C);
                linkInfoStart = 0x4E + linkInfoTargetIdListSize;
            }
            else if ((linkFlags & (1 << 1)) != 0) {
                linkInfoStart = 0x4C;
            }
            else {
                return filePath;
            }
            int localBasePathOffset = data.getInt(linkInfoStart + 16);
            if (localBasePathOffset > 0) {
                filePath = StringUtils.fromANSIString(data, linkInfoStart + localBasePathOffset);
            }
            dis.close();
            fis.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    public static void createDesktopFile(File lnkFile, Context context) {
        String lnkFilePath = lnkFile.getPath();
        String filePath = StringUtils.escapeFileDOSPath(parseFilePath(lnkFile));
        ImageFs imageFs = ImageFs.find(context);

        File desktopFile = new File(lnkFilePath.substring(0, lnkFilePath.lastIndexOf(".")) + ".desktop");
        try {
            FileOutputStream fos = new FileOutputStream(desktopFile);
            PrintWriter pw = new PrintWriter(fos);
            pw.write("[Desktop Entry]\n");
            pw.write("Name=" + lnkFile.getName().substring(0, lnkFile.getName().lastIndexOf(".")) + "\n");
            pw.write("Exec=env WINEPREFIX=" + "\"" + imageFs.wineprefix + "\"" + " wine " + filePath + "\n");
            pw.write("Type=Application\n");
            pw.write("StartupNotify=True\n");
            pw.close();
            fos.close();
        }
        catch (IOException e) {
        }
    }

    public static Options extractLinkInfo(File linkFile) throws IOException {
        boolean z;
        int iAbs;
        byte[] bytes;
        String str;
        byte[] bytes2 = FileUtils.read(linkFile);
        if (bytes2 == null) {
            return null;
        }
        ByteBuffer data = ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN);
        if (data.get() != 76 || data.get() != 0 || data.get() != 0 || data.get() != 0) {
            return null;
        }
        int LinkFlags = data.getInt(20);
        int IconIndex = data.getInt(56);
        data.position(76);
        String driveLetter = "";
        String path = "";
        boolean isDirectory = false;
        Options linkInfo = null;
        if ((LinkFlags & 1) != 0) {
            short IDListSize = data.getShort();
            byte[] prefixOfDirectory = {49, 0, 0, 0, 0, 0};
            byte[] prefixOfArchive = {50, 0, 0, 0, 0, 0};
            while (IDListSize > 2) {
                short ItemIDSize = data.getShort();
                byte[] ItemData = new byte[ItemIDSize - 2];
                data.get(ItemData);
                if (ArrayUtils.startsWith(prefixOfDirectory, ItemData)) {
                    String filename = StringUtils.fromANSIString(Arrays.copyOfRange(ItemData, 12, ItemData.length));
                    StringBuilder sb = new StringBuilder();
                    sb.append(path);
                    if (path.isEmpty()) {
                        bytes = bytes2;
                        str = "";
                    } else {
                        bytes = bytes2;
                        str = "\\";
                    }
                    sb.append(str);
                    sb.append(filename);
                    path = sb.toString();
                    isDirectory = true;
                } else {
                    bytes = bytes2;
                    if (ArrayUtils.startsWith(prefixOfArchive, ItemData)) {
                        String filename2 = StringUtils.fromANSIString(Arrays.copyOfRange(ItemData, 12, ItemData.length));
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append(path);
                        sb2.append(!path.isEmpty() ? "\\" : "");
                        sb2.append(filename2);
                        path = sb2.toString();
                        isDirectory = false;
                    } else if (ItemData[0] == 47 || ItemData[0] == 35) {
                        driveLetter = StringUtils.fromANSIString(Arrays.copyOfRange(ItemData, 1, ItemData.length));
                    }
                }
                IDListSize = (short) (IDListSize - ItemIDSize);
                bytes2 = bytes;
            }
            data.getShort();
        }
        if ((LinkFlags & 2) != 0) {
            int oldPosition = data.position();
            int LinkInfoSize = data.getInt();
            int LinkInfoHeaderSize = data.getInt();
            int LinkInfoFlags = data.getInt();
            if (LinkInfoHeaderSize < 36 && (LinkInfoFlags & 2) == 0) {
                data.getInt();
                data.getInt();
                data.getInt();
                data.getInt();
                if ((LinkInfoFlags & 1) != 0) {
                    data.position(data.position() + 17);
                    String LocalBasePath = readNullTerminatedString(data);
                    linkInfo = 0 == 0 ? new Options() : null;
                    linkInfo.targetPath = LocalBasePath;
                }
                data.get();
            } else {
                data.position(oldPosition + LinkInfoSize);
            }
        }
        if (!driveLetter.matches("[A-Za-z]:\\\\?")) {
            z = true;
        } else {
            if (!driveLetter.endsWith("\\")) {
                driveLetter = driveLetter + "\\";
            }
            if (linkInfo == null) {
                linkInfo = new Options();
            }
            linkInfo.targetPath = driveLetter + path;
            linkInfo.isDirectory = isDirectory;
            if (IconIndex != 0) {
                z = true;
                iAbs = Math.abs(IconIndex) + 1;
            } else {
                z = true;
                iAbs = -1;
            }
            linkInfo.iconIndex = iAbs;
        }
        boolean isUnicode = (LinkFlags & 128) != 0 ? z : false;
        if ((LinkFlags & 4) != 0) {
            readStringData(data, isUnicode);
        }
        if ((LinkFlags & 8) != 0) {
            readStringData(data, isUnicode);
        }
        if ((LinkFlags & 16) != 0) {
            readStringData(data, isUnicode);
        }
        if ((LinkFlags & 32) != 0) {
            String arguments = readStringData(data, isUnicode);
            if (linkInfo != null) {
                linkInfo.arguments = arguments;
            }
        }
        if ((LinkFlags & 64) != 0) {
            String iconLocation = readStringData(data, isUnicode);
            if (linkInfo != null) {
                if (iconLocation != null && iconLocation.equals("shell32.dll")) {
                    iconLocation = "C:/windows/system32/shell32.dll";
                }
                linkInfo.iconLocation = iconLocation;
            }
        }
        return linkInfo;
    }
}
