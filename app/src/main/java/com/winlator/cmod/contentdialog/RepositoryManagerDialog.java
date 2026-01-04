package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import org.json.JSONArray;
import org.json.JSONException;
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

    public RepositoryManagerDialog(Context context) {
        this.context = context;
        loadRepos();
    }
    
    public void setOnDismissCallback(Runnable callback) {
        this.onGlobalDismissCallback = callback;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Driver Sources"); // English

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        // Ajuste de padding para não ficar colado na borda
        recyclerView.setPadding(0, 10, 0, 10);

        adapter = new RepoAdapter();
        recyclerView.setAdapter(adapter);

        builder.setView(recyclerView);
        
        builder.setPositiveButton("Add Source", (d, w) -> showRepoDialog(null, -1));
        builder.setNegativeButton("Close", null);

        dialog = builder.create();
        dialog.show();
    }

    // Função unificada para Adicionar ou Editar
    private void showRepoDialog(DriverRepo repoToEdit, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(repoToEdit == null ? "Add Repository" : "Edit Repository");

        final EditText inputName = new EditText(context);
        inputName.setHint("Name (e.g. Kimchi Turnip)");
        if (repoToEdit != null) inputName.setText(repoToEdit.name);
        
        final EditText inputUrl = new EditText(context);
        inputUrl.setHint("GitHub API URL");
        if (repoToEdit != null) inputUrl.setText(repoToEdit.apiUrl);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30); // Mais espaçamento lateral
        layout.addView(inputName);
        layout.addView(inputUrl);
        builder.setView(layout);

        builder.setPositiveButton("Save", (d, w) -> {
            String name = inputName.getText().toString().trim();
            String url = inputUrl.getText().toString().trim();
            
            // Auto-convert GitHub browser links to API links
            if (url.startsWith("https://github.com/") && !url.contains("api.github.com")) {
                url = url.replace("https://github.com/", "https://api.github.com/repos/");
                if (!url.endsWith("/releases")) url += "/releases";
            }

            if (!name.isEmpty() && !url.isEmpty()) {
                if (repoToEdit == null) {
                    repos.add(new DriverRepo(name, url));
                } else {
                    repoToEdit.name = name;
                    repoToEdit.apiUrl = url;
                    repos.set(position, repoToEdit);
                }
                saveRepos();
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadRepos() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonStr = prefs.getString("custom_driver_repos", "");
        
        repos.clear();
        if (jsonStr.isEmpty()) {
            // Default Repos
            repos.add(new DriverRepo("KIMCHI Turnip (Releases)", "https://api.github.com/repos/K11MCH1/AdrenoToolsDrivers/releases"));
            repos.add(new DriverRepo("Qualcomm Official", "https://api.github.com/repos/JingMatrix/AdrenoGPU/releases"));
        } else {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    repos.add(DriverRepo.fromJson(array.getJSONObject(i)));
                }
            } catch (Exception e) {
                // If error, load empty or defaults
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adrenotools_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DriverRepo repo = repos.get(position);
            holder.title.setText(repo.name);
            holder.subtitle.setText(repo.apiUrl);
            
            // Ícone de "Mais Opções" (3 pontinhos) ou Settings
            holder.actionButton.setImageResource(R.drawable.icon_settings); 
            
            // Clique no item: ABRE A LISTA DE DRIVERS
            holder.itemView.setOnClickListener(v -> {
                DriverDownloadDialog driverDialog = new DriverDownloadDialog(context, repo.apiUrl);
                driverDialog.setOnDismissCallback(onGlobalDismissCallback);
                driverDialog.show();
            });

            // Clique no botão lateral: MENU DE OPÇÕES (Edit/Delete)
            holder.actionButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, holder.actionButton);
                popup.getMenu().add("Edit");
                popup.getMenu().add("Delete");
                
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Edit")) {
                        showRepoDialog(repo, position);
                    } else if (item.getTitle().equals("Delete")) {
                        repos.remove(position);
                        saveRepos();
                        notifyDataSetChanged();
                    }
                    return true;
                });
                popup.show();
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