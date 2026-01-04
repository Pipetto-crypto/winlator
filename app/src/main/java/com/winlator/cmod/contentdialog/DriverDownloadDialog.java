package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
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

    // SEU LINK DO GITHUB AQUI
    private static final String DRIVERS_JSON_URL = "https://raw.githubusercontent.com/SEU_USER/SEU_REPO/main/drivers.json";

    public DriverDownloadDialog(Context context) {
        this.context = context;
        this.adrenotoolsManager = new AdrenotoolsManager(context);
    }

    public void setOnDismissCallback(Runnable callback) {
        this.onDismissCallback = callback;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Baixar Drivers (GitHub)");

        // Configura o RecyclerView
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        
        builder.setView(recyclerView);
        builder.setNegativeButton("Fechar", (d, w) -> {
            if (onDismissCallback != null) onDismissCallback.run();
        });

        dialog = builder.create();
        dialog.show();

        // Inicia o carregamento
        fetchDrivers();
    }

    // --- LÓGICA DE REDE ---

    private void fetchDrivers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            String jsonStr = Downloader.downloadString(DRIVERS_JSON_URL);
            
            if (jsonStr == null) {
                runOnUi(() -> Toast.makeText(context, "Erro ao buscar lista!", Toast.LENGTH_SHORT).show());
                return;
            }

            List<DriverItem> drivers = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    drivers.add(new DriverItem(
                        obj.getString("name"),
                        obj.getString("version"),
                        obj.getString("description"),
                        obj.getString("url")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUi(() -> setupAdapter(drivers));
        });
    }

    private void downloadAndInstall(DriverItem driver) {
        Toast.makeText(context, "Baixando " + driver.name + "...", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, "Falha! O ZIP deve conter meta.json.", Toast.LENGTH_LONG).show();
                        }
                        tmpFile.delete();
                    });
                } else {
                    runOnUi(() -> Toast.makeText(context, "Erro no Download!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUi(() -> Toast.makeText(context, "Erro crítico: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // --- ADAPTER ---

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
            // CORREÇÃO: Usando 'adrenotools_list_item' que sabemos que existe
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false); 
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DriverItem item = list.get(position);
            holder.title.setText(item.name);
            holder.subtitle.setText(item.description); 
            
            // Reutiliza o botão de menu para ser o botão de download
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
                // CORREÇÃO: IDs que existem no adrenotools_list_item.xml
                title = v.findViewById(R.id.TVName);     // Era TVTitle
                subtitle = v.findViewById(R.id.TVVersion); // Era TVDescr
                actionButton = v.findViewById(R.id.BTMenu); // Era BTAction
            }
        }
    }

    private void runOnUi(Runnable action) {
        if (context instanceof Activity) ((Activity) context).runOnUiThread(action);
    }
}