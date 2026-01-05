package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.Downloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DriverDownloadDialog {
    private final Context context;
    private final AdrenotoolsManager adrenotoolsManager;
    private AlertDialog dialog;
    private RecyclerView recyclerView;
    private Runnable onDismissCallback;
    private final String repoUrl;

    public DriverDownloadDialog(Context context, String repoUrl) {
        this.context = context;
        this.adrenotoolsManager = new AdrenotoolsManager(context);
        this.repoUrl = repoUrl;
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Available Drivers"); // English

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        
        builder.setView(recyclerView);
        builder.setNegativeButton("Back", null); // English

        dialog = builder.create();
        dialog.show();

        fetchDrivers();
    }

    private void fetchDrivers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String jsonStr = Downloader.downloadString(repoUrl);
            
            if (jsonStr == null) {
                runOnUi(() -> Toast.makeText(context, "Connection failed!", Toast.LENGTH_SHORT).show());
                return;
            }

            List<ReleaseItem> releases = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(jsonStr);
                
                for (int i = 0; i < array.length(); i++) {
                    JSONObject releaseObj = array.getJSONObject(i);
                    
                    // 1. Clean Name Logic
                    String rawName = releaseObj.optString("name", releaseObj.optString("tag_name", "Unknown Driver"));
                    String cleanName = cleanDriverName(rawName);
                    
                    String description = releaseObj.optString("body", "");
                    
                    // 2. Collect all valid ZIP assets
                    List<DriverAsset> assets = new ArrayList<>();
                    if (releaseObj.has("assets")) {
                        JSONArray assetsArr = releaseObj.getJSONArray("assets");
                        for (int j = 0; j < assetsArr.length(); j++) {
                            JSONObject asset = assetsArr.getJSONObject(j);
                            String url = asset.getString("browser_download_url");
                            String filename = asset.optString("name", "driver.zip");
                            
                            if (url.endsWith(".zip") || url.endsWith(".tzst")) {
                                assets.add(new DriverAsset(filename, url));
                            }
                        }
                    } else if (releaseObj.has("url")) {
                        // Fallback for custom JSON
                        assets.add(new DriverAsset(cleanName + ".zip", releaseObj.getString("url")));
                    }

                    if (!assets.isEmpty()) {
                        releases.add(new ReleaseItem(cleanName, description, assets));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUi(() -> setupAdapter(releases));
        });
    }

    // Função para limpar nomes repetitivos (ex: Kimchi)
    private String cleanDriverName(String raw) {
        // Remove prefixos comuns do Kimchi para destacar a versão
        String clean = raw.replace("Mesa Turnip driver ", "")
                          .replace("Mesa Turnip ", "")
                          .replace("Qualcomm Driver ", "");
        return clean.trim();
    }

    private void onDownloadClick(ReleaseItem item) {
        if (item.assets.isEmpty()) return;

        // Se tiver só 1 arquivo, baixa direto
        if (item.assets.size() == 1) {
            startDownload(item.assets.get(0));
        } else {
            // Se tiver vários (Gmem, Sysmem...), mostra um diálogo para escolher
            String[] assetNames = new String[item.assets.size()];
            for (int i = 0; i < item.assets.size(); i++) {
                assetNames[i] = item.assets.get(i).name;
            }

            new AlertDialog.Builder(context)
                .setTitle("Select Variant")
                .setItems(assetNames, (dialogInterface, which) -> {
                    startDownload(item.assets.get(which));
                })
                .show();
        }
    }

    private void startDownload(DriverAsset asset) {
        Toast.makeText(context, "Downloading " + asset.name + "...", Toast.LENGTH_SHORT).show();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File tmpFile = new File(context.getCacheDir(), "driver_temp.zip");
                if (tmpFile.exists()) tmpFile.delete();

                boolean success = Downloader.downloadFile(asset.url, tmpFile);

                if (success) {
                    Uri fileUri = Uri.fromFile(tmpFile);
                    runOnUi(() -> {
                        String installedName = adrenotoolsManager.installDriver(fileUri);
                        if (!installedName.isEmpty()) {
                            Toast.makeText(context, "Installed: " + installedName, Toast.LENGTH_SHORT).show();
                            if (onDismissCallback != null) onDismissCallback.run();
                        } else {
                            Toast.makeText(context, "Installation failed! Invalid ZIP.", Toast.LENGTH_LONG).show();
                        }
                        tmpFile.delete();
                    });
                } else {
                    runOnUi(() -> Toast.makeText(context, "Download failed!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupAdapter(List<ReleaseItem> releases) {
        if (releases.isEmpty()) {
            Toast.makeText(context, "No drivers found.", Toast.LENGTH_LONG).show();
            return;
        }
        recyclerView.setAdapter(new DriverAdapter(releases));
    }

    // MODEL CLASSES
    private static class ReleaseItem {
        String name, description;
        List<DriverAsset> assets;
        ReleaseItem(String n, String d, List<DriverAsset> a) { name = n; description = d; assets = a; }
    }
    
    private static class DriverAsset {
        String name, url;
        DriverAsset(String n, String u) { name = n; url = u; }
    }

    // ADAPTER
    private class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.ViewHolder> {
        private final List<ReleaseItem> list;
        public DriverAdapter(List<ReleaseItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false); 
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ReleaseItem item = list.get(position);
            
            // Título limpo e destacado (ex: v26.0.0 - R7)
            holder.title.setText(item.name);
            
            // Subtítulo mostra quantos arquivos tem ou uma prévia da descrição
            if (item.assets.size() > 1) {
                holder.subtitle.setText(item.assets.size() + " variants available (Click to choose)");
            } else {
                // Remove quebras de linha para ficar compacto
                String shortDesc = item.description.replace("\n", " ").trim();
                if (shortDesc.length() > 50) shortDesc = shortDesc.substring(0, 50) + "...";
                if (shortDesc.isEmpty()) shortDesc = "No description";
                holder.subtitle.setText(shortDesc);
            }

            holder.actionButton.setImageResource(android.R.drawable.stat_sys_download);
            holder.actionButton.setOnClickListener(v -> onDownloadClick(item));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, subtitle;
            ImageButton actionButton;
            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.TVName);
                subtitle = v.findViewById(R.id.TVVersion);
                actionButton = v.findViewById(R.id.BTMenu);
            }
        }
    }

    private void runOnUi(Runnable action) {
        if (context instanceof Activity) ((Activity) context).runOnUiThread(action);
    }
}