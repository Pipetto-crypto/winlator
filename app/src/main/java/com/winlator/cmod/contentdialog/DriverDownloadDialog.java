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
    private final String repoUrl; // <--- A URL AGORA É VARIÁVEL

    // CONSTRUTOR NOVO: Recebe a URL do Gerenciador
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
        builder.setTitle("Drivers Disponíveis");

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        
        builder.setView(recyclerView);
        builder.setNegativeButton("Voltar", null);

        dialog = builder.create();
        dialog.show();

        fetchDrivers();
    }

    private void fetchDrivers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Usa a URL recebida no construtor
            String jsonStr = Downloader.downloadString(repoUrl);
            
            if (jsonStr == null) {
                runOnUi(() -> Toast.makeText(context, "Erro ao conectar na fonte!", Toast.LENGTH_SHORT).show());
                return;
            }

            List<DriverItem> drivers = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(jsonStr);
                
                for (int i = 0; i < array.length(); i++) {
                    JSONObject release = array.getJSONObject(i);
                    // Tenta pegar o nome da release ou a tag
                    String name = release.optString("name", release.optString("tag_name", "Driver"));
                    // Se o nome estiver vazio, tenta usar a tag como fallback
                    if (name.isEmpty()) name = release.optString("tag_name", "Versão " + i);

                    String body = release.optString("body", "");
                    String downloadUrl = "";

                    // Lógica inteligente para achar o ZIP nos assets
                    if (release.has("assets")) {
                        JSONArray assets = release.getJSONArray("assets");
                        for (int j = 0; j < assets.length(); j++) {
                            JSONObject asset = assets.getJSONObject(j);
                            String assetUrl = asset.getString("browser_download_url");
                            if (assetUrl.endsWith(".zip") || assetUrl.endsWith(".tzst")) {
                                downloadUrl = assetUrl;
                                break; 
                            }
                        }
                    } 
                    // Fallback para JSON manual antigo
                    else if (release.has("url")) {
                        downloadUrl = release.getString("url");
                    }
                    
                    if (!downloadUrl.isEmpty()) {
                        drivers.add(new DriverItem(name, body, downloadUrl));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUi(() -> setupAdapter(drivers));
        });
    }

    private void downloadAndInstall(DriverItem driver) {
        Toast.makeText(context, "Baixando...", Toast.LENGTH_SHORT).show();
        // Não fechamos o dialog aqui para permitir baixar outros se quiser

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File tmpFile = new File(context.getCacheDir(), "driver_temp.zip");
                if (tmpFile.exists()) tmpFile.delete();

                boolean success = Downloader.downloadFile(driver.downloadUrl, tmpFile);

                if (success) {
                    Uri fileUri = Uri.fromFile(tmpFile);
                    runOnUi(() -> {
                        String installedName = adrenotoolsManager.installDriver(fileUri);
                        if (!installedName.isEmpty()) {
                            Toast.makeText(context, "Instalado: " + installedName, Toast.LENGTH_SHORT).show();
                            if (onDismissCallback != null) onDismissCallback.run();
                        } else {
                            Toast.makeText(context, "Falha! ZIP inválido (precisa de meta.json).", Toast.LENGTH_LONG).show();
                        }
                        tmpFile.delete();
                    });
                } else {
                    runOnUi(() -> Toast.makeText(context, "Erro no Download!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void setupAdapter(List<DriverItem> drivers) {
        if (drivers.isEmpty()) {
            Toast.makeText(context, "Nenhum driver encontrado nesta fonte.", Toast.LENGTH_LONG).show();
            return;
        }
        recyclerView.setAdapter(new DriverAdapter(drivers));
    }

    private class DriverItem {
        String name, description, downloadUrl;
        DriverItem(String n, String d, String u) { 
            this.name = n; this.description = d; this.downloadUrl = u; 
        }
    }

    private class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.ViewHolder> {
        private final List<DriverItem> list;
        public DriverAdapter(List<DriverItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false); 
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DriverItem item = list.get(position);
            holder.title.setText(item.name);
            String shortDesc = item.description.length() > 60 ? item.description.substring(0, 60) + "..." : item.description;
            holder.subtitle.setText(shortDesc);
            holder.actionButton.setImageResource(android.R.drawable.stat_sys_download);
            holder.actionButton.setOnClickListener(v -> downloadAndInstall(item));
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