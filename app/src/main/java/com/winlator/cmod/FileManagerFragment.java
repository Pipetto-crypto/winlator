package com.winlator.cmod;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import com.winlator.cmod.core.FileUtils;

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

    /**
     * Attempts to extract the icon from the EXE using 'wrestool'.
     * This relies on the internal Linux environment tools present in Winlator.
     */
    private void extractIconFromExe(File exeFile, String iconName, Container container) {
        try {
            // Path to the executable file
            String exePath = exeFile.getAbsolutePath();

            // Target directory for icons in the container's virtual file system
            File iconsDir = new File(container.getRootDir(), "home/xuser/.local/share/icons");
            if (!iconsDir.exists()) iconsDir.mkdirs();

            // Output file (using .ico as it is the native extraction format of wrestool)
            File outputIcon = new File(iconsDir, iconName + ".ico");

            // We attempt to call 'wrestool' which is usually available in the container's /usr/bin.
            // Note: This command is a "best effort". It assumes the app environment 
            // has 'wrestool' in the path or accessible via shell.
            String[] cmd = {
                "/system/bin/sh", 
                "-c", 
                "wrestool -x -t 14 \"" + exePath + "\" -o \"" + outputIcon.getAbsolutePath() + "\""
            };

            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && outputIcon.exists()) {
                Log.d("FileManagerFragment", "Icon extracted successfully: " + outputIcon.getAbsolutePath());
            } else {
                Log.e("FileManagerFragment", "Failed to extract icon. Exit code: " + exitCode);
            }

        } catch (Exception e) {
            Log.e("FileManagerFragment", "Error extracting icon", e);
            e.printStackTrace();
        }
    }

    private void createShortcutDirectly(File file, Container container) {
        try {
            String filename = file.getName();
            String name = filename;
            int pos = filename.lastIndexOf(".");
            if (pos > 0) name = filename.substring(0, pos);
            
            // Fix: Use Absolute Unix Path to prevent XServerDisplayActivity crash
            String unixPath = file.getAbsolutePath();
            String workDir = file.getParent();

            // --- EXTRACT ICON ---
            // This attempts to extract the EXE icon to /home/xuser/.local/share/icons
            extractIconFromExe(file, name, container);

            File shortcutsDir = container.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();

            File desktopFile = new File(shortcutsDir, name + ".desktop");
            
            int counter = 1;
            while (desktopFile.exists()) {
                desktopFile = new File(shortcutsDir, name + " (" + counter + ").desktop");
                counter++;
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + name);
                
                // Using Unix path (handles spaces correctly if wrapped in quotes)
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + unixPath + "\"");
                
                writer.println("Type=Application");
                writer.println("Terminal=false");
                writer.println("StartupNotify=true");
                
                // Set the Icon name. Since we extracted it to ~/.local/share/icons, 
                // we just need the name (without extension usually works, or match the file we saved).
                // If extraction failed, it falls back to the system default (white/generic).
                writer.println("Icon=" + name); 
                
                writer.println("Path=" + workDir);
                
                writer.println("container_id:" + container.id);
                writer.println("");
                writer.println("[Extra Data]");
                writer.println("container_id=" + container.id);
            }

            Toast.makeText(getContext(), "Shortcut created: " + name, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving shortcut: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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