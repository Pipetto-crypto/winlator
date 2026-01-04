package com.winlator.cmod.contentdialog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RepositoryManagerDialog {
    private final Context context;
    private AlertDialog dialog;
    private RecyclerView recyclerView;
    private RepoAdapter adapter;
    private final List<DriverRepo> repos = new ArrayList<>();
    private Runnable onGlobalDismissCallback;

    // Repositórios Padrão (Adicione os que você quiser aqui)
    private void loadDefaults() {
        repos.add(new DriverRepo("KIMCHI Turnip (Releases)", "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases"));
        repos.add(new DriverRepo("Qualcomm Oficial (Exemplo)", "https://api.github.com/repos/JingMatrix/AdrenoGPU/releases")); 
    }

    public RepositoryManagerDialog(Context context) {
        this.context = context;
        loadRepos();
    }
    
    public void setOnDismissCallback(Runnable callback) {
        this.onGlobalDismissCallback = callback;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Fontes de Drivers");

        View view = LayoutInflater.from(context).inflate(R.layout.settings_fragment, null); 
        // Nota: Estamos criando o layout programaticamente abaixo para não depender de XML novo
        
        // Configura o RecyclerView Manualmente
        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        recyclerView.setPadding(16, 16, 16, 16);

        adapter = new RepoAdapter();
        recyclerView.setAdapter(adapter);

        builder.setView(recyclerView);
        
        // Botão para Adicionar Nova Repo
        builder.setPositiveButton("Adicionar Fonte", (d, w) -> showAddRepoDialog());
        builder.setNegativeButton("Fechar", null);

        dialog = builder.create();
        dialog.show();
    }

    private void showAddRepoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Adicionar Repositório");

        final EditText inputName = new EditText(context);
        inputName.setHint("Nome (ex: Mr. Purple)");
        
        final EditText inputUrl = new EditText(context);
        inputUrl.setHint("URL da API do GitHub");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.addView(inputName);
        layout.addView(inputUrl);
        builder.setView(layout);

        builder.setPositiveButton("Salvar", (d, w) -> {
            String name = inputName.getText().toString();
            String url = inputUrl.getText().toString();
            
            // Auto-converte link de navegador para API
            if (url.startsWith("https://github.com/") && !url.contains("api.github.com")) {
                url = url.replace("https://github.com/", "https://api.github.com/repos/");
                if (!url.endsWith("/releases")) url += "/releases";
            }

            if (!name.isEmpty() && !url.isEmpty()) {
                repos.add(new DriverRepo(name, url));
                saveRepos();
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void loadRepos() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonStr = prefs.getString("custom_driver_repos", "");
        
        repos.clear();
        if (jsonStr.isEmpty()) {
            loadDefaults(); // Se não tiver nada salvo, carrega os padrões
        } else {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    repos.add(DriverRepo.fromJson(array.getJSONObject(i)));
                }
            } catch (Exception e) {
                loadDefaults();
            }
        }
    }

    private void saveRepos() {
        try {
            JSONArray array = new JSONArray();
            for (DriverRepo repo : repos) {
                array.put(repo.toJson());
            }
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString("custom_driver_repos", array.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ADAPTER ---
    private class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Reutiliza o layout de lista existente
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DriverRepo repo = repos.get(position);
            holder.title.setText(repo.name);
            holder.subtitle.setText(repo.apiUrl);
            
            // Ícone de "Abrir Pasta" ou "Seta"
            holder.actionButton.setImageResource(android.R.drawable.ic_menu_directions); 
            
            // Clique simples: ABRE A LISTA DE DRIVERS
            holder.itemView.setOnClickListener(v -> {
                // Abre o DriverDownloadDialog passando a URL específica
                DriverDownloadDialog driverDialog = new DriverDownloadDialog(context, repo.apiUrl);
                driverDialog.setOnDismissCallback(onGlobalDismissCallback); // Repassa o callback para atualizar a lista no final
                driverDialog.show();
            });

            // Clique no botão lateral: DELETAR
            holder.actionButton.setImageResource(android.R.drawable.ic_menu_delete);
            holder.actionButton.setOnClickListener(v -> {
                repos.remove(position);
                saveRepos();
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() { return repos.size(); }

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
}