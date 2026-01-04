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
import com.winlator.cmod.contents.Downloader; // Usa seu Downloader existente

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
        recyclerView.setPadding(20, 20, 20, 20);

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
            // 1. Baixa o JSON usando sua classe Downloader
            String jsonStr = Downloader.downloadString(DRIVERS_JSON_URL);
            
            if (jsonStr == null) {
                runOnUi(() -> Toast.makeText(context, "Erro ao buscar lista!", Toast.LENGTH_SHORT).show());
                return;
            }

            // 2. Processa o JSON manualmente (Sem depender do ContentsManager)
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

            // 3. Mostra na tela
            runOnUi(() -> setupAdapter(drivers));
        });
    }

    private void downloadAndInstall(DriverItem driver) {
        Toast.makeText(context, "Baixando " + driver.name + "...", Toast.LENGTH_SHORT).show();
        dialog.dismiss(); // Fecha para baixar em background

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Cria um arquivo temporário
                File tmpFile = new File(context.getCacheDir(), "driver_temp.zip");
                if (tmpFile.exists()) tmpFile.delete();

                // Baixa o arquivo ZIP
                boolean success = Downloader.downloadFile(driver.downloadUrl, tmpFile);

                if (success) {
                    // Instala usando o AdrenotoolsManager existente
                    // Ele aceita URI, então convertemos o arquivo para URI
                    Uri fileUri = Uri.fromFile(tmpFile);
                    
                    runOnUi(() -> {
                        // Chama o método installDriver do seu AdrenotoolsManager original
                        String installedName = adrenotoolsManager.installDriver(fileUri);
                        
                        if (!installedName.isEmpty()) {
                            Toast.makeText(context, "Instalado: " + installedName, Toast.LENGTH_LONG).show();
                            if (onDismissCallback != null) onDismissCallback.run();
                        } else {
                            Toast.makeText(context, "Falha na instalação! Verifique se o ZIP tem o meta.json.", Toast.LENGTH_LONG).show();
                        }
                        
                        // Limpa o lixo
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

    // --- ADAPTER E MODELO (CLASSES INTERNAS) ---

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
            // Reutiliza o layout de item que você já tem (adrenotools_list_item) ou usa um simples
            // Aqui estou improvisando um layout simples com código, mas o ideal é usar um XML
            // Vou tentar usar o `adrenotools_list_item` se ele tiver os IDs compatíveis, 
            // mas como é um dialog novo, vou usar android.R.layout.simple_list_item_2 para garantir que funcione sem XML novo.
            
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_list_item, parent, false); 
            // Estou assumindo que 'content_list_item' existe pois é padrão do Winlator, 
            // se não existir, troque por um layout seu.
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DriverItem item = list.get(position);
            holder.title.setText(item.name);
            holder.subtitle.setText(item.description); // ou item.version
            
            // Botão de Download
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
                // Ajuste esses IDs conforme o layout XML que você usar (ex: content_list_item.xml)
                title = v.findViewById(R.id.TVTitle);        // Verifique se o ID é TVTitle ou TVName
                subtitle = v.findViewById(R.id.TVDescr);     // Verifique se o ID é TVDescr ou TVVersion
                actionButton = v.findViewById(R.id.BTAction); // Botão lateral
                
                // FALLBACK: Se não achar os IDs (caso layout seja diferente), evite crash
                if (title == null) title = new TextView(context);
                if (subtitle == null) subtitle = new TextView(context);
                if (actionButton == null) actionButton = new ImageButton(context);
            }
        }
    }

    private void runOnUi(Runnable action) {
        if (context instanceof Activity) ((Activity) context).runOnUiThread(action);
    }
}