package com.winlator.cmod.core;

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

public class SteamGameFetcher {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface OnImageDownloadedListener {
        void onSuccess(File imageFile);
        void onFailure(Exception e);
    }

    public static void fetchIcon(String gameName, File destinationFile, OnImageDownloadedListener listener) {
        executor.execute(() -> {
            try {
                // Busca na API pública da Steam
                String searchUrl = "https://store.steampowered.com/api/storesearch/?term=" 
                        + URLEncoder.encode(gameName, "UTF-8") 
                        + "&l=english&cc=US";

                String jsonResponse = downloadString(searchUrl);
                JSONObject json = new JSONObject(jsonResponse);
                
                if (json.getInt("total") == 0) {
                    throw new Exception("Jogo não encontrado: " + gameName);
                }

                JSONArray items = json.getJSONArray("items");
                JSONObject firstItem = items.getJSONObject(0);
                int appId = firstItem.getInt("id");

                // Baixa a "Header Image" (460x215) que tem boa qualidade e proporção
                String imageUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/header.jpg";

                downloadFile(imageUrl, destinationFile);

                if (listener != null) listener.onSuccess(destinationFile);

            } catch (Exception e) {
                Log.e("SteamFetcher", "Erro ao buscar: " + gameName, e);
                if (listener != null) listener.onFailure(e);
            }
        });
    }

    private static String downloadString(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        // Timeout para não travar se a net estiver ruim
        conn.setConnectTimeout(5000); 
        conn.setReadTimeout(5000);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        }
    }

    private static void downloadFile(String urlString, File dst) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
    }
}