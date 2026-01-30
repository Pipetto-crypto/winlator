package com.winlator.cmod.container;

import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.core.MSLink;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

/* loaded from: classes.dex */
public class FileInfo implements Comparable<FileInfo> {
    private final Container container;
    private MSLink.Options linkInfo;
    public final String name;
    public final String path;
    public final Type type;

    public enum Type {
        FILE,
        DIRECTORY,
        DRIVE
    }

    public FileInfo(Container container, String path, Type type) {
        this(container, FileUtils.getName(path), path, type);
    }

    public FileInfo(Container container, String name, String path, Type type) {
        this.container = container;
        this.name = name;
        this.path = StringUtils.removeEndSlash(path);
        this.type = type;
    }

    public ArrayList<FileInfo> list() {
        File[] files;
        ArrayList<FileInfo> result = new ArrayList<>();
        File linkFile = getLinkFile();
        File parent = linkFile != null ? linkFile : toFile();
        if (parent.isDirectory() && (files = parent.listFiles()) != null) {
            for (File file : files) {
                result.add(new FileInfo(this.container, file.getPath(), file.isDirectory() ? Type.DIRECTORY : Type.FILE));
            }
        }
        Collections.sort(result);
        return result;
    }

    public long getSize() {
        if (this.type == Type.FILE) {
            return toFile().length();
        }
        return 0L;
    }

    public int getItemCount() {
        File linkFile = getLinkFile();
        File file = linkFile != null ? linkFile : toFile();
        String[] items = file.list();
        if (items != null) {
            return items.length;
        }
        return 0;
    }

    public File toFile() {
        return new File(this.path);
    }

    public File getLinkFile() {
        MSLink.Options linkInfo = getLinkinfo();
        if (linkInfo != null) {
            return new File(WineUtils.dosToUnixPath(linkInfo.targetPath, this.container));
        }
        return null;
    }

    public MSLink.Options getLinkinfo() {
        MSLink.Options linkInfo = this.linkInfo;
        if (linkInfo != null) {
            return linkInfo;
        }
        if (this.name.endsWith(".lnk")) {
            try{
                this.linkInfo = MSLink.extractLinkInfo(toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.linkInfo;
    }

    public boolean renameTo(String newName) {
        String newName2 = StringUtils.clearReservedChars(newName);
        File from = toFile();
        File to = new File(from.getParentFile(), newName2);
        return !to.exists() && from.renameTo(to);
    }

    public String getDisplayName() {
        MSLink.Options linkInfo = getLinkinfo();
        String str = this.name;
        return linkInfo != null ? FileUtils.getBasename(str) : str;
    }

    @Override // java.lang.Comparable
    public int compareTo(FileInfo other) {
        Type type = other.type;
        Type type2 = Type.DRIVE;
        int value = Boolean.compare(type == type2, this.type == type2);
        if (value == 0) {
            Type type3 = other.type;
            Type type4 = Type.DIRECTORY;
            value = Boolean.compare(type3 == type4, this.type == type4);
        }
        return value == 0 ? this.name.compareTo(other.name) : value;
    }
}