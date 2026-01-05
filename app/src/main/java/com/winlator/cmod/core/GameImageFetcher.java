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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameImageFetcher {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "GameImageFetcher";

    public interface OnImageDownloadedListener {
        void onSuccess(File imageFile);
        void onFailure(Exception e);
    }

    public static void fetchIcon(String gameName, File destinationFile, OnImageDownloadedListener listener) {
        executor.execute(() -> {
            try {
                String imageUrl = null;
                
                // 1. Tenta achar o AppID na Steam
                int appId = getSteamAppId(gameName);

                if (appId > 0) {
                    // 2. Tenta pegar o ícone quadrado real da página da loja (HTML Scraping)
                    imageUrl = scrapeSteamIcon(appId);
                    
                    // 3. Se não achar o ícone, usa o Header (Banner) como fallback
                    if (imageUrl == null) {
                        imageUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/header.jpg";
                    }
                } else {
                    // Fallback para GOG se não achar na Steam
                    Log.d(TAG, "Not found on Steam, trying GOG...");
                    imageUrl = tryGogSearch(gameName);
                }

                if (imageUrl == null) {
                    throw new Exception("Game not found on Steam or GOG: " + gameName);
                }

                // 4. Baixa, recorta (se necessário) e redimensiona para 64x64
                downloadAndProcessImage(imageUrl, destinationFile);

                if (listener != null) listener.onSuccess(destinationFile);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch icon for: " + gameName, e);
                if (listener != null) listener.onFailure(e);
            }
        });
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

    // Pega o ícone quadrado real sem API Key
    private static String scrapeSteamIcon(int appId) {
        try {
            String storeUrl = "https://store.steampowered.com/app/" + appId;
            String html = downloadString(storeUrl);
            
            // Procura por: <div class="apphub_AppIcon"><img src="...">
            Pattern pattern = Pattern.compile("class=\"apphub_AppIcon\">\\s*<img src=\"(.*?)\"");
            Matcher matcher = pattern.matcher(html);
            
            if (matcher.find()) {
                return matcher.group(1); 
            }
        } catch (Exception e) {
            Log.w(TAG, "Steam scraping error: " + e.getMessage());
        }
        return null;
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
                return "https:" + imageBase + "_196.jpg"; // Imagem quadrada da GOG
            }
        } catch (Exception e) { Log.w(TAG, "GOG error: " + e.getMessage()); }
        return null;
    }

    private static String downloadString(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // Finge ser navegador
        conn.setRequestProperty("Cookie", "birthtime=568022401"); // Passa pelo filtro de idade (+18)
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

            Bitmap finalBitmap;
            
            // Se a imagem for muito larga (Banner), fazemos um CENTER CROP para virar quadrado
            if (original.getWidth() > original.getHeight() * 1.5) {
                int size = original.getHeight();
                int x = (original.getWidth() - size) / 2;
                finalBitmap = Bitmap.createBitmap(original, x, 0, size, size);
            } else {
                finalBitmap = original;
            }

            // Redimensiona para 64x64 para ficar leve na lista e compatível
            Bitmap resized = Bitmap.createScaledBitmap(finalBitmap, 64, 64, true);

            try (FileOutputStream out = new FileOutputStream(dst)) {
                resized.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            
            if (original != finalBitmap) finalBitmap.recycle();
            original.recycle();
            resized.recycle();
        }
    }
}