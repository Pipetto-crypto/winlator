package com.winlator.cmod.core;
import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public abstract class Sha256Util {
    public static byte[] calculate(Context context, String file) {
        AssetManager am = context.getAssets();
        byte[] hash;
        
        try (InputStream io = am.open(file)) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            hash = sha256.digest(io.readAllBytes());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return hash;
    }

    public static byte[] calculate(InputStream io) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(io.readAllBytes());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }    
    }

    public static String convert(byte[] hash) {
        StringBuilder sb = new StringBuilder();
        
        for (byte b : hash) {
            sb.append(String.format("%02x", b & 0xff));
        }
        
        return sb.toString();
    }

    public static boolean isEqual(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
    
    public static byte[] readFromFile(File sumFile) {
        Path path = sumFile.toPath();
        byte[] hash;
        
        try {
            hash = Files.readAllBytes(path);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return hash;
    }

    public static void writeToFile(File sumFile, byte[] hash) {
        Path path = sumFile.toPath();
        
        try {
            Files.write(path, hash);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
