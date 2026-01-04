package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager; // Necessário para ler as configs
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DividerItemDecoration;

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

    // URL PADRÃO (Caso o usuário não mude nada nas configurações)
    // Você pode colocar aqui a sua repo principal
    private static final String DEFAULT_REPO_URL = "https://raw.githubusercontent.com/SEU_USER/SEU_REPO/main/drivers.json";

    public DriverDownloadDialog(Context context) {
        this.context = context;
        this.adrenotoolsManager = new AdrenotoolsManager(context);
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    // Método auxiliar para pegar a URL salva nas configurações
    private String getDriversRepoUrl() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // "drivers_repo_url" será a chave que vamos criar no SettingsFragment depois
        return prefs.getString("drivers_repo_url", DEFAULT_REPO_URL);
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Baixar Drivers");

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        
        builder.setView(recyclerView);
        builder.setNegativeButton("Fechar", (d, w) -> {
            if (onDismissCallback != null) onDismissCallback.run();
        });

        dialog = builder.create();
        dialog.show();

        fetchDrivers();
    }

    private void fetchDrivers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. Pega a URL dinâmica das configurações
            String repoUrl = getDriversRepoUrl();
            
            // 2. Baixa o JSON
            String jsonStr = Downloader.downloadString(repoUrl);
            
            if (jsonStr == null) {
                runOnUi(() -> Toast.makeText(context, "Erro ao conectar na Repo:\n" + repoUrl, Toast.LENGTH_LONG).show());
                return;
            }

            // 3. Processa o JSON
            List<DriverItem> drivers = new ArrayList<>();
            try {
                // Suporte para o formato de Lista
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    
                    // Lógica para suportar Releases do GitHub
                    // Se o JSON tiver um campo "browser_download_url" (API do GitHub) ou "url" (Seu JSON manual)
                    String downloadUrl = obj.has("url") ? obj.getString("url") : "";
                    
                    // Se você usar a API de Releases do GitHub direto, a estrutura muda um pouco,
                    // mas assumindo que você vai fazer um JSON manual apontando para as releases:
                    drivers.add(new DriverItem(
                        obj.optString("name", "Driver Sem Nome"),
                        obj.optString("version", ""),
                        obj.optString("description", ""),
                        downloadUrl
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUi(() -> Toast.makeText(context, "Erro ao ler JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            runOnUi(() -> setupAdapter(drivers));
        });
    }

    private void downloadAndInstall(DriverItem driver) {
        if (driver.downloadUrl.isEmpty()) {
            Toast.makeText(context, "URL de download inválida!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Baixando...", Toast.LENGTH_SHORT).show();
        dialog.dismiss();

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
                            Toast.makeText(context, "Instalado: " + installedName, Toast.LENGTH_LONG).show();
                            if (onDismissCallback != null) onDismissCallback.run();
                        } else {
                            Toast.makeText(context, "Falha! ZIP inválido (precisa do meta.json).", Toast.LENGTH_LONG).show();
                        }
                        tmpFile.delete();
                    });
                } else {
                    runOnUi(() -> Toast.makeText(context, "Erro no Download!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUi(() -> Toast.makeText(context, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupAdapter(List<DriverItem> drivers) {
        if (drivers.isEmpty()) {
            Toast.makeText(context, "Nenhum driver encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }
        recyclerView.setAdapter(new DriverAdapter(drivers));
    }

    private class DriverItem {
        String name, version, description, downloadUrl;
        DriverItem(String n, String v, String d, String u) { 
            this.name = n; this.version = v; this.description = d; this.downloadUrl = u; 
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
            holder.subtitle.setText(item.description.isEmpty() ? item.version : item.description);
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