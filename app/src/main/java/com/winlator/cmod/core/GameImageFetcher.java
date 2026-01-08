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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameImageFetcher {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "GameImageFetcher";
    private static final int ICON_SIZE = 256;
    
    private static final Map<String, String> ACRONYMS = new HashMap<>();
    
    static {
        ACRONYMS.put("DBXV", "Dragon Ball Xenoverse");
        ACRONYMS.put("DBXV2", "Dragon Ball Xenoverse 2");
        ACRONYMS.put("DMC", "Devil May Cry");
        ACRONYMS.put("DMC3", "Devil May Cry 3");
        ACRONYMS.put("DMC4", "Devil May Cry 4");
        ACRONYMS.put("DMC5", "Devil May Cry 5");
        ACRONYMS.put("RE", "Resident Evil");
        ACRONYMS.put("RE2", "Resident Evil 2");
        ACRONYMS.put("RE3", "Resident Evil 3");
        ACRONYMS.put("RE4", "Resident Evil 4");
        ACRONYMS.put("RE5", "Resident Evil 5");
        ACRONYMS.put("RE6", "Resident Evil 6");
        ACRONYMS.put("GTA", "Grand Theft Auto");
        ACRONYMS.put("GTA3", "Grand Theft Auto III");
        ACRONYMS.put("GTA4", "Grand Theft Auto IV");
        ACRONYMS.put("GTA5", "Grand Theft Auto V");
        ACRONYMS.put("GTAV", "Grand Theft Auto V");
        ACRONYMS.put("SA", "Grand Theft Auto San Andreas");
        ACRONYMS.put("VC", "Grand Theft Auto Vice City");
        ACRONYMS.put("NFS", "Need for Speed");
        ACRONYMS.put("NFSU", "Need for Speed Underground");
        ACRONYMS.put("NFSU2", "Need for Speed Underground 2");
        ACRONYMS.put("MW", "Need for Speed Most Wanted");
        ACRONYMS.put("COD", "Call of Duty");
        ACRONYMS.put("COD4", "Call of Duty 4 Modern Warfare");
        ACRONYMS.put("BO", "Call of Duty Black Ops");
        ACRONYMS.put("BO2", "Call of Duty Black Ops II");
        ACRONYMS.put("AC", "Assassin's Creed");
        ACRONYMS.put("AC2", "Assassin's Creed II");
        ACRONYMS.put("ACB", "Assassin's Creed Brotherhood");
        ACRONYMS.put("ACR", "Assassin's Creed Revelations");
        ACRONYMS.put("AC3", "Assassin's Creed III");
        ACRONYMS.put("AC4", "Assassin's Creed IV Black Flag");
        ACRONYMS.put("BF", "Battlefield");
        ACRONYMS.put("BF3", "Battlefield 3");
        ACRONYMS.put("BF4", "Battlefield 4");
        ACRONYMS.put("CS", "Counter-Strike");
        ACRONYMS.put("CSGO", "Counter-Strike Global Offensive");
        ACRONYMS.put("CSS", "Counter-Strike Source");
        ACRONYMS.put("L4D", "Left 4 Dead");
        ACRONYMS.put("L4D2", "Left 4 Dead 2");
        ACRONYMS.put("TF2", "Team Fortress 2");
        ACRONYMS.put("HL", "Half-Life");
        ACRONYMS.put("HL2", "Half-Life 2");
        ACRONYMS.put("MC", "Minecraft");
        ACRONYMS.put("LOL", "League of Legends");
        ACRONYMS.put("WOW", "World of Warcraft");
        ACRONYMS.put("ESO", "Elder Scrolls Online");
        ACRONYMS.put("SKYRIM", "The Elder Scrolls V Skyrim");
        ACRONYMS.put("OBLIVION", "The Elder Scrolls IV Oblivion");
        ACRONYMS.put("MORROWIND", "The Elder Scrolls III Morrowind");
        ACRONYMS.put("FO3", "Fallout 3");
        ACRONYMS.put("FO4", "Fallout 4");
        ACRONYMS.put("FNV", "Fallout New Vegas");
        ACRONYMS.put("MGS", "Metal Gear Solid");
        ACRONYMS.put("MGS5", "Metal Gear Solid V");
        ACRONYMS.put("MGSV", "Metal Gear Solid V");
        ACRONYMS.put("MGR", "Metal Gear Rising Revengeance");
        ACRONYMS.put("DS", "Dark Souls");
        ACRONYMS.put("DS2", "Dark Souls II");
        ACRONYMS.put("DS3", "Dark Souls III");
        ACRONYMS.put("ER", "Elden Ring");
        ACRONYMS.put("FF7", "Final Fantasy VII");
        ACRONYMS.put("FFX", "Final Fantasy X");
        ACRONYMS.put("FF14", "Final Fantasy XIV");
        ACRONYMS.put("KH", "Kingdom Hearts");
        ACRONYMS.put("PES", "Pro Evolution Soccer");
        ACRONYMS.put("FIFA", "EA SPORTS FC");
    }

    public interface OnImageDownloadedListener {
        void onSuccess(File imageFile);
        void onFailure(Exception e);
    }

    public static void fetchIcon(String gameName, File destinationFile, OnImageDownloadedListener listener) {
        executor.execute(() -> {
            try {
                String searchName = resolveAcronym(gameName);
                String imageUrl = findBestImageUrl(searchName);

                if (imageUrl == null) {
                    String normalized = normalizeName(searchName);
                    if (!normalized.equals(searchName)) {
                        imageUrl = findBestImageUrl(normalized);
                    }
                }

                if (imageUrl == null) {
                    throw new Exception("Game not found: " + gameName + " (searched as " + searchName + ")");
                }

                downloadAndProcessImage(imageUrl, destinationFile);

                if (listener != null) listener.onSuccess(destinationFile);

            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch icon for: " + gameName, e);
                if (listener != null) listener.onFailure(e);
            }
        });
    }
    
    private static String resolveAcronym(String name) {
        String clean = name.replaceAll("(?i)\\bdx\\d+\\b", "").trim(); 
        String upper = clean.toUpperCase().replaceAll("[^A-Z0-9]", "");
        
        if (ACRONYMS.containsKey(upper)) {
            return ACRONYMS.get(upper);
        }
        return name;
    }

    private static String findBestImageUrl(String gameName) {
        String url = trySteam(gameName);
        if (url != null) return url;

        String normalizedName = normalizeName(gameName);
        if (!normalizedName.equals(gameName)) {
            url = trySteam(normalizedName);
            if (url != null) return url;
        }

        url = tryGog(gameName);
        if (url != null) return url;

        if (!normalizedName.equals(gameName)) {
            return tryGog(normalizedName);
        }

        return null;
    }

    private static String normalizeName(String name) {
        String n = name;
        n = n.replaceAll("(?i)sigma", "Σ");
        n = n.replaceAll("(?i)goty", "Game of the Year");
        n = n.replaceAll("(?i)remastered", "");
        n = n.replaceAll("(?i)definitive edition", "");
        n = n.replaceAll("(?i)directx\\s*\\d*", "");
        n = n.replaceAll("(?i)\\bdx\\d+\\b", ""); 
        n = n.replaceAll("(?i)\\bhd\\b", ""); 
        n = n.replaceAll("(?i)\\bsteam\\b", "");
        
        n = n.replaceAll("([a-zA-Z])(\\d)", "$1 $2");
        
        n = n.replaceAll("[^a-zA-Z0-9\\s]", " ");
        n = n.replaceAll("\\s+", " ").trim();
        
        return n;
    }

    private static String trySteam(String gameName) {
        try {
            int appId = getSteamAppId(gameName);
            if (appId > 0) {
                String hdUrl = "https://steamcdn-a.akamaihd.net/steam/apps/" + appId + "/library_600x900.jpg";
                if (checkUrlExists(hdUrl)) return hdUrl;

                String iconUrl = scrapeSteamIcon(appId);
                if (iconUrl != null) return iconUrl;

                return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/header.jpg";
            }
        } catch (Exception e) {
            Log.w(TAG, "Steam search failed for: " + gameName);
        }
        return null;
    }

    private static String tryGog(String gameName) {
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
        } catch (Exception e) { 
            Log.w(TAG, "GOG search failed for: " + gameName); 
        }
        return null;
    }

    private static int getSteamAppId(String gameName) throws Exception {
        String searchUrl = "https://store.steampowered.com/api/storesearch/?term=" 
                + URLEncoder.encode(gameName, "UTF-8") 
                + "&l=english&cc=US";
        String jsonResponse = downloadString(searchUrl);
        JSONObject json = new JSONObject(jsonResponse);
        if (json.getInt("total") > 0) {
            return json.getJSONArray("items").getJSONObject(0).getInt("id");
        }
        return -1;
    }

    private static String scrapeSteamIcon(int appId) {
        try {
            String storeUrl = "https://store.steampowered.com/app/" + appId;
            String html = downloadString(storeUrl);
            Pattern pattern = Pattern.compile("class=\"apphub_AppIcon\">\\s*<img src=\"(.*?)\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) return matcher.group(1); 
        } catch (Exception e) {}
        return null;
    }

    private static boolean checkUrlExists(String urlString) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(urlString).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(3000);
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
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