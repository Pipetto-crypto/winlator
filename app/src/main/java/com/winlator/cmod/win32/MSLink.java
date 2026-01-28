package com.winlator.cmod.win32;

import com.winlator.cmod.core.ArrayUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StringUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/* loaded from: classes.dex */
public abstract class MSLink {

    public static final class LinkInfo {
        public String arguments;
        public int fileSize;
        public int iconIndex;
        public String iconLocation;
        public boolean isDirectory;
        public int showCommand = 1;
        public String targetPath;
    }

    private static int charToHexDigit(char chr) {
        return chr >= 'A' ? (chr - 'A') + 10 : chr - '0';
    }

    private static byte twoCharsToByte(char chr1, char chr2) {
        return (byte) ((charToHexDigit(Character.toUpperCase(chr1)) * 16) + charToHexDigit(Character.toUpperCase(chr2)));
    }

    private static byte[] convertCLSIDtoDATA(String str) {
        return new byte[]{twoCharsToByte(str.charAt(6), str.charAt(7)), twoCharsToByte(str.charAt(4), str.charAt(5)), twoCharsToByte(str.charAt(2), str.charAt(3)), twoCharsToByte(str.charAt(0), str.charAt(1)), twoCharsToByte(str.charAt(11), str.charAt(12)), twoCharsToByte(str.charAt(9), str.charAt(10)), twoCharsToByte(str.charAt(16), str.charAt(17)), twoCharsToByte(str.charAt(14), str.charAt(15)), twoCharsToByte(str.charAt(19), str.charAt(20)), twoCharsToByte(str.charAt(21), str.charAt(22)), twoCharsToByte(str.charAt(24), str.charAt(25)), twoCharsToByte(str.charAt(26), str.charAt(27)), twoCharsToByte(str.charAt(28), str.charAt(29)), twoCharsToByte(str.charAt(30), str.charAt(31)), twoCharsToByte(str.charAt(32), str.charAt(33)), twoCharsToByte(str.charAt(34), str.charAt(35))};
    }

    private static byte[] stringToByteArray(String str) {
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) str.charAt(i);
        }
        return bytes;
    }

    private static byte[] intToByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] generateStringData(String str) {
        ByteBuffer buffer = ByteBuffer.allocate(str.length() + 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) str.length());
        for (int i = 0; i < str.length(); i++) {
            buffer.put((byte) str.charAt(i));
        }
        return buffer.array();
    }

    private static byte[] generateIDLIST(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (bytes.length + 2));
        return ArrayUtils.concat(buffer.array(), bytes);
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

    public static Boolean createFile(LinkInfo linkInfo, File outputFile) throws Exception {
        byte[] FileAttributes;
        byte[] prefixOfTarget;
        byte[] ShowCommand;
        byte[] prefixRoot;
        int i;
        byte b;
        int i2;
        byte[] targetRoot;
        byte[] targetLeaf;
        byte[] targetRoot2;
        int i3;
        Throwable th;
        byte[] targetLeaf2;
        byte[] HeaderSize = {76, 0, 0, 0};
        byte[] LinkCLSID = convertCLSIDtoDATA("00021401-0000-0000-c000-000000000046");
        int linkFlags = 257;
        String str = linkInfo.arguments;
        if (str != null && !str.isEmpty()) {
            linkFlags = 257 | 32;
        }
        String str2 = linkInfo.iconLocation;
        if (str2 != null && !str2.isEmpty()) {
            linkFlags |= 64;
        }
        byte[] LinkFlags = intToByteArray(linkFlags);
        linkInfo.targetPath = linkInfo.targetPath.replaceAll("/+", "\\\\");
        if (linkInfo.isDirectory) {
            FileAttributes = new byte[]{16, 0, 0, 0};
            prefixOfTarget = new byte[]{49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        } else {
            FileAttributes = new byte[]{32, 0, 0, 0};
            prefixOfTarget = new byte[]{50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        }
        byte[] CreationTime = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] FileSize = intToByteArray(linkInfo.fileSize);
        byte[] IconIndex = intToByteArray(linkInfo.iconIndex);
        byte[] ShowCommand2 = intToByteArray(linkInfo.showCommand);
        byte[] Hotkey = {0, 0};
        byte[] Reserved1 = {0, 0};
        byte[] Reserved2 = {0, 0, 0, 0};
        byte[] Reserved3 = {0, 0, 0, 0};
        byte[] CLSIDComputer = convertCLSIDtoDATA("20d04fe0-3aea-1069-a2d8-08002b30309d");
        byte[] CLSIDNetwork = convertCLSIDtoDATA("208d2c60-3aea-1069-a2d7-08002b30309d");
        if (linkInfo.targetPath.startsWith("\\")) {
            byte[] targetRoot3 = stringToByteArray(linkInfo.targetPath);
            prefixRoot = new byte[]{-61, 1, -127};
            if (linkInfo.targetPath.endsWith("\\")) {
                targetLeaf2 = null;
            } else {
                String str3 = linkInfo.targetPath;
                targetLeaf2 = stringToByteArray(str3.substring(str3.lastIndexOf("\\") + 1));
            }
            targetLeaf = targetLeaf2;
            targetRoot = targetRoot3;
            byte[] targetRoot4 = {31, 88};
            ShowCommand = ShowCommand2;
            b = 0;
            i2 = 1;
            i = 2;
            targetRoot2 = ArrayUtils.concat(targetRoot4, CLSIDNetwork);
        } else {
            ShowCommand = ShowCommand2;
            byte[] prefixRoot2 = {47};
            int index = linkInfo.targetPath.indexOf("\\");
            prefixRoot = prefixRoot2;
            byte[] targetRoot5 = stringToByteArray(linkInfo.targetPath.substring(0, index + 1));
            byte[] targetLeaf3 = stringToByteArray(linkInfo.targetPath.substring(index + 1));
            i = 2;
            b = 0;
            i2 = 1;
            targetRoot = targetRoot5;
            targetLeaf = targetLeaf3;
            targetRoot2 = ArrayUtils.concat(new byte[]{31, 80}, CLSIDComputer);
        }
        byte[][] bArr = new byte[i][];
        bArr[b] = targetRoot;
        bArr[i2] = new byte[21];
        byte[] targetRoot6 = ArrayUtils.concat(bArr);
        byte[] endOfString = new byte[i2];
        endOfString[b] = b;
        byte[][] bArr2 = new byte[2][];
        bArr2[b] = generateIDLIST(targetRoot2);
        byte[][] bArr3 = new byte[3][];
        bArr3[b] = prefixRoot;
        bArr3[1] = targetRoot6;
        bArr3[2] = endOfString;
        bArr2[1] = generateIDLIST(ArrayUtils.concat(bArr3));
        byte[] IDListItems = ArrayUtils.concat(bArr2);
        if (targetLeaf != null) {
            byte[][] bArr4 = new byte[2][];
            bArr4[b] = IDListItems;
            byte[][] bArr5 = new byte[3][];
            bArr5[b] = prefixOfTarget;
            bArr5[1] = targetLeaf;
            i3 = 2;
            bArr5[2] = endOfString;
            bArr4[1] = generateIDLIST(ArrayUtils.concat(bArr5));
            IDListItems = ArrayUtils.concat(bArr4);
        } else {
            i3 = 2;
        }
        byte[] IDList = generateIDLIST(IDListItems);
        byte[] TerminalID = new byte[i3];
        // fill-array-data instruction
        TerminalID[0] = 0;
        TerminalID[1] = 0;
        byte[] StringData = new byte[0];
        if ((linkFlags & 32) != 0) {
            StringData = ArrayUtils.concat(StringData, generateStringData(linkInfo.arguments));
        }
        if ((linkFlags & 64) != 0) {
            StringData = ArrayUtils.concat(StringData, generateStringData(linkInfo.iconLocation));
        }
        byte[] StringData2 = StringData;
        try {
                FileOutputStream os = new FileOutputStream(outputFile);
                try {
                    os.write(HeaderSize);
                    os.write(LinkCLSID);
                    os.write(LinkFlags);
                    os.write(FileAttributes);
                    os.write(CreationTime);
                    os.write(CreationTime);
                    os.write(CreationTime);
                    os.write(FileSize);
                    os.write(IconIndex);
                    try {
                        os.write(ShowCommand);
                        try {
                            os.write(Hotkey);
                            try {
                                os.write(Reserved1);
                                try {
                                    os.write(Reserved2);
                                    try {
                                        os.write(Reserved3);
                                        os.write(IDList);
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                    try {
                                        os.write(TerminalID);
                                        if (StringData2.length > 0) {
                                            os.write(StringData2);
                                        }
                                        try {
                                            os.close();
                                            return true;
                                        } catch (IOException e) {
                                            return false;
                                        }
                                    } catch (Throwable th3) {
                                        th = th3;
                                        try {
                                            try {
                                                os.close();
                                                throw th;
                                            } catch (IOException e2) {
                                                return false;
                                            }
                                        } catch (Throwable th4) {
                                            th.addSuppressed(th4);
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
            } catch (IOException e3) {
                return false;
            }
        return false;
    }

    public static LinkInfo extractLinkInfo(File linkFile) throws IOException {
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
        LinkInfo linkInfo = null;
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
                    linkInfo = 0 == 0 ? new LinkInfo() : null;
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
                linkInfo = new LinkInfo();
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