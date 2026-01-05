package com.winlator.cmod.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameImageFetcher {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "GameImageFetcher";
    
    
    private static final int ICON_SIZE = 256; 

    public interface OnImageDownloadedListener {
        void onSuccess(File imageFile);
        void onFailure(Exception e);
    }

    public static void fetchIcon(String gameName, File destinationFile, OnImageDownloadedListener listener) {
        executor.execute(() -> {
            try {
                String imageUrl = null;
                
                int appId = getSteamAppId(gameName);

                if (appId > 0) {
                    imageUrl = "https://steamcdn-a.akamaihd.net/steam/apps/" + appId + "/library_600x900.jpg";
                } else {
                    Log.d(TAG, "Not found on Steam, trying GOG...");
                    imageUrl = tryGogSearch(gameName);
                }

                if (imageUrl == null) {
                    throw new Exception("Game not found on Steam or GOG: " + gameName);
                }
                
                downloadAndProcessImage(imageUrl, destinationFile);

                if (listener != null) listener.onSuccess(destinationFile);

            } catch (Exception e) {
                if (e.getMessage() != null && !e.getMessage().contains("fallback")) {
                    retryWithFallback(gameName, destinationFile, listener);
                } else {
                    Log.e(TAG, "Failed to fetch icon: " + gameName, e);
                    if (listener != null) listener.onFailure(e);
                }
            }
        });
    }
    
    private static void retryWithFallback(String gameName, File destinationFile, OnImageDownloadedListener listener) {
        try {
            int appId = getSteamAppId(gameName);
            if (appId > 0) {
                String fallbackUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/header.jpg";
                downloadAndProcessImage(fallbackUrl, destinationFile);
                if (listener != null) listener.onSuccess(destinationFile);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Fallback failed", ex);
            if (listener != null) listener.onFailure(ex);
        }
    }

    private static int getSteamAppId(String gameName) {
        try {
            String searchUrl = "https://store.steampowered.com/api/storesearch/?term=" 
                    + URLEncoder.encode(gameName, "UTF-8") 
                    + "&l=english&cc=US";
            String jsonResponse = downloadString(searchUrl);
            JSONObject json = new JSONObject(jsonResponse);
            if (json.getInt("total") > 0) {
                return json.getJSONArray("items").getJSONObject(0).getInt("id");
            }
        } catch (Exception e) {
            Log.w(TAG, "Steam ID search error: " + e.getMessage());
        }
        return -1;
    }

    private static String tryGogSearch(String gameName) {
        try {
            String searchUrl = "https://embed.gog.com/games/ajax/filtered?mediaType=game&search=" 
                    + URLEncoder.encode(gameName, "UTF-8");
            String jsonResponse = downloadString(searchUrl);
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("products");
            if (products.length() > 0) {
                String imageBase = products.getJSONObject(0).getString("image");
                return "https:" + imageBase + "_600.jpg"; 
            }
        } catch (Exception e) { Log.w(TAG, "GOG error: " + e.getMessage()); }
        return null;
    }

    private static String downloadString(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0"); 
        conn.setConnectTimeout(5000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        }
    }

    private static void downloadAndProcessImage(String urlString, File dst) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);

        try (InputStream in = conn.getInputStream()) {
            Bitmap original = BitmapFactory.decodeStream(in);
            if (original == null) throw new Exception("Failed to decode image");

            Bitmap croppedBitmap;
            int width = original.getWidth();
            int height = original.getHeight();
            if (height > width) {
                int size = width;
                croppedBitmap = Bitmap.createBitmap(original, 0, 0, size, size);
            } else if (width > height) {
                int size = height;
                int x = (width - size) / 2;
                croppedBitmap = Bitmap.createBitmap(original, x, 0, size, size);
            } else {
                croppedBitmap = original;
            }
            
            Bitmap resized = Bitmap.createScaledBitmap(croppedBitmap, ICON_SIZE, ICON_SIZE, true);

            try (FileOutputStream out = new FileOutputStream(dst)) {
                resized.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            
            if (original != croppedBitmap) original.recycle();
            if (croppedBitmap != resized) croppedBitmap.recycle();
            resized.recycle();
        }
    }
}