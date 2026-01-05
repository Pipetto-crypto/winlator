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
import com.winlator.cmod.core.GameImageFetcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
        containerManager = new ContainerManager(getContext());
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

        Collections.sort(fileList, (f1, f2) -> {
            boolean d1 = f1.isDirectory();
            boolean d2 = f2.isDirectory();
            
            if (d1 && !d2) return -1;
            if (!d1 && d2) return 1;

            if (!d1 && !d2) {
                boolean isExe1 = isExecutable(f1);
                boolean isExe2 = isExecutable(f2);
                if (isExe1 && !isExe2) return -1;
                if (!isExe1 && isExe2) return 1;
            }

            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        adapter = new FileAdapter(fileList);
        recyclerView.setAdapter(adapter);
    }

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
            createShortcutDirectly(file, containers.get(0));
        } else {
            String[] names = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) {
                Container c = containers.get(i);
                names[i] = c.getName() + " (ID: " + c.id + ")";
            }

            new AlertDialog.Builder(getContext())
                .setTitle("Select Container")
                .setItems(names, (dialog, which) -> {
                    createShortcutDirectly(file, containers.get(which));
                })
                .show();
        }
    }

    private void createShortcutDirectly(File file, Container container) {
        try {
            String displayName = getSmartDisplayName(file);
            String unixPath = file.getAbsolutePath();
            String workDir = file.getParent();

            File shortcutsDir = container.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();

            File desktopFile = new File(shortcutsDir, displayName + ".desktop");
            
            int counter = 1;
            while (desktopFile.exists()) {
                desktopFile = new File(shortcutsDir, displayName + " (" + counter + ").desktop");
                counter++;
            }

            File externalStorage = Environment.getExternalStorageDirectory();
            File iconsDir = new File(externalStorage, "Winlator/icons");
            if (!iconsDir.exists()) iconsDir.mkdirs();
            
            File iconFile = new File(iconsDir, displayName + ".png");

            Toast.makeText(getContext(), "Fetching icon for: " + displayName, Toast.LENGTH_SHORT).show();
            
            GameImageFetcher.fetchIcon(displayName, iconFile, new GameImageFetcher.OnImageDownloadedListener() {
                @Override
                public void onSuccess(File imageFile) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Icon found: " + displayName, Toast.LENGTH_SHORT).show()
                        );
                    }
                }

                @Override
                public void onFailure(Exception e) {
                }
            });

            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + unixPath + "\"");
                writer.println("Type=Application");
                writer.println("Terminal=false");
                writer.println("StartupNotify=true");
                writer.println("Icon=" + displayName); 
                writer.println("Path=" + workDir);
                writer.println("container_id:" + container.id);
                writer.println("");
                writer.println("[Extra Data]");
                writer.println("container_id=" + container.id);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating shortcut: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getSmartDisplayName(File file) {
        String filename = cleanGameName(file.getName());
        String lowerName = filename.toLowerCase();
        
        List<String> genericNames = Arrays.asList(
            "game", "launcher", "setup", "installer", "start", "run", 
            "speed", "update", "patch", "loader", "client", "app", "main", "boot", "play",
            "application", "shipping", "x64", "x86", "win64", "win32", "binaries",
            "application-x64", "application-x86", "shipping-x64", "shipping-x86"
        );
        
        boolean isModOrGeneric = false;
        if (lowerName.contains("mod") || lowerName.contains("fix") || 
            lowerName.contains("crack") || lowerName.contains("patch") || 
            lowerName.contains("limit") || lowerName.contains("changed")) {
            isModOrGeneric = true;
        }

        if (!isModOrGeneric && filename.length() < 4) {
            isModOrGeneric = true;
        }
        
        if (!isModOrGeneric) {
            for (String gen : genericNames) {
                if (lowerName.equals(gen) || lowerName.startsWith(gen + " ") || lowerName.equals(gen.replace("-", " "))) {
                    isModOrGeneric = true;
                    break;
                }
            }
        }
        
        if (isModOrGeneric) {
            File parent = file.getParentFile();
            if (parent != null) {
                String parentName = cleanGameName(parent.getName());
                List<String> genericFolders = Arrays.asList("bin", "bin32", "bin64", "system", "release", "retail", "win64", "win32");
                
                if (genericFolders.contains(parentName.toLowerCase())) {
                    File grandParent = parent.getParentFile();
                    if (grandParent != null) {
                        return cleanGameName(grandParent.getName());
                    }
                }
                return parentName;
            }
        }
        return filename;
    }

    private String cleanGameName(String filename) {
        String name = filename;
        int pos = name.lastIndexOf(".");
        if (pos > 0) name = name.substring(0, pos);
        
        name = name.replace("_", " ").replace(".", " ").replace("-", " ");
        
        name = name.replaceAll("(?i)\\b(v\\d+|repack|setup|installer|portable|goty|edition|codex|fitgirl|dodi|elamigos|gold|ultimate|remastered|director|cut|application|shipping|mod|fix|patch|update|limit|changed|file)\\b", "");
        
        name = name.replaceAll("[^a-zA-Z0-9 ]", "");
        name = name.replaceAll("\\s+", " ").trim();
        
        return name;
    }

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
                holder.ivIcon.setImageResource(R.drawable.icon_open); 
                int count = file.list() != null ? file.list().length : 0;
                holder.tvDetails.setText(count + " items");
                holder.btMenu.setVisibility(View.GONE);
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);

                if (isExe) {
                    holder.ivIcon.setImageResource(R.drawable.icon_wine); 
                    holder.btMenu.setVisibility(View.VISIBLE);
                    holder.btMenu.setImageResource(android.R.drawable.ic_menu_more);
                    View.OnClickListener action = v -> showExeOptions(file, holder.btMenu);
                    holder.itemView.setOnClickListener(action);
                    holder.btMenu.setOnClickListener(action);
                } else {
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