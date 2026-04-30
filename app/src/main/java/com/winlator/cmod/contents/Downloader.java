package com.winlator.cmod.contents;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Downloader {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int BUFFER_SIZE = 262144; // 256 KiB
    private static final long PROGRESS_MIN_INTERVAL_MS = 120L;

    public interface ProgressListener {
        void onProgress(int progress, long downloadedBytes, long totalBytes);
    }

    public static boolean downloadFile(String address, File file) {
        return downloadFile(address, file, null);
    }

    public static boolean downloadFile(String address, File file, ProgressListener progressListener) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(address);
            URLConnection baseConnection = url.openConnection();
            connection = baseConnection instanceof HttpURLConnection ? (HttpURLConnection) baseConnection : null;
            URLConnection activeConnection = connection != null ? connection : baseConnection;
            activeConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            activeConnection.setReadTimeout(READ_TIMEOUT_MS);
            activeConnection.setUseCaches(false);
            activeConnection.setRequestProperty("Connection", "Keep-Alive");
            activeConnection.setRequestProperty("Accept-Encoding", "identity");
            if (connection != null) {
                connection.setInstanceFollowRedirects(true);
            }
            activeConnection.connect();
            long fileLength = activeConnection.getContentLengthLong();

            byte[] data = new byte[BUFFER_SIZE];
            long total = 0;
            int lastProgress = -1;
            long lastProgressUpdate = 0L;

            try (InputStream input = new BufferedInputStream(activeConnection.getInputStream(), BUFFER_SIZE);
                 OutputStream output = new BufferedOutputStream(new FileOutputStream(file.getAbsolutePath()), BUFFER_SIZE)) {
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    total += count;
                    if (progressListener != null && fileLength > 0) {
                        int progress = (int) ((total * 100L) / fileLength);
                        long now = System.currentTimeMillis();
                        boolean shouldNotify = progress != lastProgress &&
                                (progress >= 100 || now - lastProgressUpdate >= PROGRESS_MIN_INTERVAL_MS);
                        if (shouldNotify) {
                            lastProgress = progress;
                            lastProgressUpdate = now;
                            progressListener.onProgress(progress, total, fileLength);
                        }
                    }
                }
                output.flush();
            }
            if (progressListener != null && fileLength > 0 && lastProgress < 100) {
                progressListener.onProgress(100, total, fileLength);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static String downloadString(String address) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(address);
            URLConnection baseConnection = url.openConnection();
            connection = baseConnection instanceof HttpURLConnection ? (HttpURLConnection) baseConnection : null;
            URLConnection activeConnection = connection != null ? connection : baseConnection;
            activeConnection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            activeConnection.setReadTimeout(READ_TIMEOUT_MS);
            activeConnection.setUseCaches(false);
            activeConnection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(activeConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
