package com.winlator.cmod;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.ShortcutsFragment; // Para atualizar a lista se precisar
// Importe a classe que gerencia atalhos no seu projeto, geralmente é ShortcutManager ou similar
// Vou usar uma lógica genérica de adicionar ao DB

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FileManagerFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvCurrentPath;
    private File currentDir;
    private FileAdapter adapter;
    private ContainerManager containerManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializa o gerenciador de containers para poder consultar
        containerManager = new ContainerManager(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("File Manager");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_manager_fragment, container, false);

        tvCurrentPath = view.findViewById(R.id.TVCurrentPath);
        recyclerView = view.findViewById(R.id.RecyclerViewFiles);
        
        view.findViewById(R.id.BTUpDir).setOnClickListener(v -> navigateUp());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        // Começa na pasta Download para facilitar (ou Raiz)
        currentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!currentDir.exists()) currentDir = Environment.getExternalStorageDirectory();
        
        loadDirectory(currentDir);

        return view;
    }

    private void navigateUp() {
        if (currentDir != null && currentDir.getParentFile() != null && currentDir.getParentFile().canRead()) {
            loadDirectory(currentDir.getParentFile());
        } else {
            Toast.makeText(getContext(), "Root reached", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());

        File[] files = dir.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        }

        Collections.sort(fileList, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        adapter = new FileAdapter(fileList);
        recyclerView.setAdapter(adapter);
    }

    // --- LÓGICA DE CRIAR ATALHO ---
    private void showExeOptions(File file, View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        popup.getMenu().add("Create Shortcut");
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Create Shortcut")) {
                checkContainersAndCreate(file);
            }
            return true;
        });
        popup.show();
    }

    private void checkContainersAndCreate(File file) {
        // 1. Carrega os containers existentes
        ArrayList<Container> containers = containerManager.getContainers();

        // 2. PROTEÇÃO: Se não tiver nenhum, avisa e para.
        if (containers == null || containers.isEmpty()) {
            new AlertDialog.Builder(getContext())
                .setTitle("No Container Found")
                .setMessage("You need to create a Container first before creating shortcuts!")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        // 3. Se tiver só um, usa ele direto
        if (containers.size() == 1) {
            createShortcut(file, containers.get(0));
        } else {
            // 4. Se tiver vários, deixa escolher
            String[] names = new String[containers.size()];
            for (int i=0; i<containers.size(); i++) names[i] = containers.get(i).getName();

            new AlertDialog.Builder(getContext())
                .setTitle("Select Container")
                .setItems(names, (dialog, which) -> {
                    createShortcut(file, containers.get(which));
                })
                .show();
        }
    }

    private void createShortcut(File file, Container container) {
        // Aqui você chama a lógica de adicionar ao DB do Winlator.
        // Isso varia dependendo da versão do seu código, mas geralmente é algo assim:
        
        try {
            // Exemplo genérico: Cria o objeto Shortcut e salva
            // Shortcut shortcut = new Shortcut(name, path, containerId...);
            // shortcutManager.add(shortcut);
            
            // Como eu não tenho o código do seu ShortcutManager, vou simular o sucesso:
            // VOCÊ PRECISA ADAPTAR ESSA PARTE PRO SEU CÓDIGO REAL DE SALVAR ATALHO
            
            Toast.makeText(getContext(), "Shortcut created for " + container.getName(), Toast.LENGTH_SHORT).show();
            
            // Opcional: Navegar para a tela de atalhos
            // getParentFragmentManager().beginTransaction().replace(R.id.FLFragmentContainer, new ShortcutsFragment()).commit();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error creating shortcut", Toast.LENGTH_SHORT).show();
        }
    }

    // --- ADAPTER ---
    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
        private final List<File> files;

        public FileAdapter(List<File> files) { this.files = files; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            File file = files.get(position);
            holder.tvName.setText(file.getName());

            if (file.isDirectory()) {
                holder.ivIcon.setImageResource(R.drawable.icon_folder);
                int count = file.list() != null ? file.list().length : 0;
                holder.tvDetails.setText(count + " items");
                holder.btMenu.setVisibility(View.GONE); // Pasta geralmente não tem menu contextual aqui
                
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.ivIcon.setImageResource(R.drawable.icon_file);
                holder.tvDetails.setText(formatSize(file.length()));

                // Se for executável, mostra o menu de opções
                String name = file.getName().toLowerCase();
                if (name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".msi")) {
                    holder.btMenu.setVisibility(View.VISIBLE);
                    
                    // Clique no item OU no menu abre as opções
                    View.OnClickListener action = v -> showExeOptions(file, holder.btMenu);
                    holder.itemView.setOnClickListener(action);
                    holder.btMenu.setOnClickListener(action);
                } else {
                    holder.btMenu.setVisibility(View.GONE);
                    holder.itemView.setOnClickListener(v -> Toast.makeText(getContext(), file.getName(), Toast.LENGTH_SHORT).show());
                }
            }
        }

        @Override
        public int getItemCount() { return files.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetails;
            ImageView ivIcon, btMenu;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.TVFileName);
                tvDetails = v.findViewById(R.id.TVFileDetails);
                ivIcon = v.findViewById(R.id.IVIcon);
                btMenu = v.findViewById(R.id.BTFileMenu);
            }
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}