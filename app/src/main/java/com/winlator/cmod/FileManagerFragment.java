package com.winlator.cmod;

import android.app.AlertDialog;
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

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.Shortcut;
import com.winlator.cmod.ShortcutsManager;
import com.winlator.cmod.R; 

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
    private ShortcutsManager shortcutsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        containerManager = new ContainerManager(getContext());
        shortcutsManager = new ShortcutsManager(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("File Manager");
        }
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

        currentDir = Environment.getExternalStorageDirectory();
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

        // --- ORDENAÇÃO MELHORADA ---
        // 1. Pastas primeiro
        // 2. Executáveis (.exe, .bat, .msi)
        // 3. Outros arquivos
        Collections.sort(fileList, (f1, f2) -> {
            boolean d1 = f1.isDirectory();
            boolean d2 = f2.isDirectory();
            
            // Se um for pasta e o outro não
            if (d1 && !d2) return -1;
            if (!d1 && d2) return 1;

            // Se ambos forem arquivos, prioriza .EXE
            if (!d1 && !d2) {
                boolean isExe1 = isExecutable(f1);
                boolean isExe2 = isExecutable(f2);
                if (isExe1 && !isExe2) return -1; // f1 vem primeiro
                if (!isExe1 && isExe2) return 1;  // f2 vem primeiro
            }

            // Ordem alfabética padrão para empates
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        adapter = new FileAdapter(fileList);
        recyclerView.setAdapter(adapter);
    }

    // Helper para identificar executáveis
    private boolean isExecutable(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".msi");
    }

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
        ArrayList<Container> containers = containerManager.getContainers();

        if (containers == null || containers.isEmpty()) {
            new AlertDialog.Builder(getContext())
                .setTitle("No Container Found")
                .setMessage("You need to create a Container first before creating shortcuts!")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        if (containers.size() == 1) {
            // Só tem um container, usa ele direto
            createShortcut(file, containers.get(0));
        } else {
            // Tem vários, mostra a lista com NOME CUSTOMIZADO
            String[] names = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) {
                Container c = containers.get(i);
                // Exibe "Nome Customizado (ID: 1)" para ficar bem claro
                names[i] = c.getName() + " (ID: " + c.id + ")";
            }

            new AlertDialog.Builder(getContext())
                .setTitle("Select Container")
                .setItems(names, (dialog, which) -> {
                    createShortcut(file, containers.get(which));
                })
                .show();
        }
    }

    private void createShortcut(File file, Container container) {
        try {
            String name = file.getName();
            int pos = name.lastIndexOf(".");
            if (pos > 0) name = name.substring(0, pos);

            // Cria o atalho vinculado ao ID do container selecionado
            Shortcut shortcut = new Shortcut(name, file.getAbsolutePath(), container.id);
            
            shortcutsManager.add(shortcut);
            shortcutsManager.save();

            Toast.makeText(getContext(), "Shortcut created in " + container.getName() + "!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating shortcut: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                // Ícone de PASTA
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_view); 
                int count = file.list() != null ? file.list().length : 0;
                holder.tvDetails.setText(count + " items");
                holder.btMenu.setVisibility(View.GONE);
                
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                // Configuração para ARQUIVOS
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);

                if (isExe) {
                    // --- MOSTRA ÍCONE DE EXECUTÁVEL (Tacinha do Wine) ---
                    // Se você não tiver o icon_wine, pode usar R.drawable.icon_run ou outro
                    holder.ivIcon.setImageResource(R.drawable.icon_wine); 
                    
                    // Mostra botão de menu
                    holder.btMenu.setVisibility(View.VISIBLE);
                    holder.btMenu.setImageResource(android.R.drawable.ic_menu_more);
                    
                    // Clique no item ou no menu abre as opções
                    View.OnClickListener action = v -> showExeOptions(file, holder.btMenu);
                    holder.itemView.setOnClickListener(action);
                    holder.btMenu.setOnClickListener(action);
                } else {
                    // Arquivo comum
                    holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
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