package com.winlator.cmod;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.core.GameImageFetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileManagerFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvCurrentPath;
    private MaterialButton btDriveSelect;
    private File currentDir;
    private FileAdapter adapter;
    private ContainerManager containerManager;
    private FloatingActionButton fabPaste; 

    private File clipboardFile = null;
    private boolean isCutOperation = false;

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView progressPercent;
    private boolean isOperationCancelled = false;

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
        btDriveSelect = view.findViewById(R.id.BTDriveSelect);

        view.findViewById(R.id.BTUpDir).setOnClickListener(v -> navigateUp());

        if (btDriveSelect != null) {
            btDriveSelect.setOnClickListener(v -> showDriveMenu());
        }

        fabPaste = view.findViewById(R.id.fabPaste);
        if (fabPaste != null) {
            fabPaste.setVisibility(View.GONE);
            fabPaste.setOnClickListener(v -> startPasteOperation());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        currentDir = Environment.getExternalStorageDirectory();
        loadDirectory(currentDir);

        return view;
    }

    private void showDriveMenu() {
        PopupMenu popup = new PopupMenu(getContext(), btDriveSelect);
        
        popup.getMenu().add("Drive D: (Android Storage)").setOnMenuItemClickListener(item -> {
            loadDirectory(Environment.getExternalStorageDirectory());
            return true;
        });

        File storageRoot = new File("/storage");
        File[] externalDrives = storageRoot.listFiles();
        if (externalDrives != null) {
            for (File drive : externalDrives) {
                if (!drive.getName().equals("emulated") && !drive.getName().equals("self")) {
                    popup.getMenu().add("External (" + drive.getName() + ")").setOnMenuItemClickListener(item -> {
                        loadDirectory(drive);
                        return true;
                    });
                }
            }
        }

        popup.getMenu().add("Drive C: (Wine System)").setOnMenuItemClickListener(item -> {
            handleDriveCSelection();
            return true;
        });

        popup.getMenu().add("Drive Z: (RootFS)").setOnMenuItemClickListener(item -> {
            File rootFs = new File(getContext().getFilesDir(), "imagefs");
            if (rootFs.exists()) loadDirectory(rootFs);
            else Toast.makeText(getContext(), "RootFS not found", Toast.LENGTH_SHORT).show();
            return true;
        });

        popup.show();
    }

    private void handleDriveCSelection() {
        ArrayList<Container> containers = containerManager.getContainers();

        if (containers == null || containers.isEmpty()) {
            Toast.makeText(getContext(), "No containers created yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (containers.size() == 1) {
            navigateToContainerDriveC(containers.get(0));
        } else {
            String[] names = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) names[i] = containers.get(i).getName();

            new AlertDialog.Builder(getContext())
                .setTitle("Select Container Drive C:")
                .setItems(names, (dialog, which) -> navigateToContainerDriveC(containers.get(which)))
                .show();
        }
    }

    private void navigateToContainerDriveC(Container container) {
        File driveC = new File(getContext().getFilesDir(), "imagefs/home/xuser/.wine/drive_c");
        
        if (driveC.exists()) {
            loadDirectory(driveC);
            Toast.makeText(getContext(), "Opened C: (" + container.getName() + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Drive C structure not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateUp() {
        if (currentDir == null) return;
        File parent = currentDir.getParentFile();
        if (parent != null && parent.canRead()) {
            loadDirectory(parent);
        } else {
            Toast.makeText(getContext(), "Root reached", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        tvCurrentPath.setText(dir.getAbsolutePath());
        
        updateDriveButtonLabel(dir);

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
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        adapter = new FileAdapter(fileList);
        recyclerView.setAdapter(adapter);
        
        if (fabPaste != null) {
            fabPaste.setVisibility(clipboardFile != null ? View.VISIBLE : View.GONE);
        }
    }

    private void updateDriveButtonLabel(File dir) {
        if (btDriveSelect == null) return;
        
        String path = dir.getAbsolutePath();
        if (path.contains("/drive_c")) {
            btDriveSelect.setText("Drive C:");
            btDriveSelect.setIconResource(R.drawable.icon_wine); 
        } else if (path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            btDriveSelect.setText("Drive D:");
            btDriveSelect.setIconResource(android.R.drawable.stat_sys_phone_call);
        } else if (path.startsWith("/storage") && !path.contains("emulated")) {
             btDriveSelect.setText("External");
             btDriveSelect.setIconResource(android.R.drawable.stat_sys_data_bluetooth);
        } else {
            btDriveSelect.setText("System (Z:)");
            btDriveSelect.setIconResource(android.R.drawable.stat_notify_sdcard);
        }
    }

    private void copyToClipboard(File file, boolean isCut) {
        this.clipboardFile = file;
        this.isCutOperation = isCut;
        if (fabPaste != null) fabPaste.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), (isCut ? "Cut: " : "Copied: ") + file.getName(), Toast.LENGTH_SHORT).show();
    }

    private void startPasteOperation() {
        if (clipboardFile == null || !clipboardFile.exists()) {
            Toast.makeText(getContext(), "Nothing to paste", Toast.LENGTH_SHORT).show();
            return;
        }

        final File source = clipboardFile;
        File tempDest = new File(currentDir, source.getName());
        if (tempDest.exists()) tempDest = new File(currentDir, "Copy_" + source.getName());
        final File dest = tempDest;

        if (isCutOperation) {
            if (source.renameTo(dest)) {
                Toast.makeText(getContext(), "Moved instantly", Toast.LENGTH_SHORT).show();
                finishPaste(true);
                return;
            }
        }

        showProgressDialog(isCutOperation ? "Moving..." : "Copying...");
        isOperationCancelled = false;

        new Thread(() -> {
            try {
                long totalBytes = getFolderSize(source);
                AtomicLong copiedBytes = new AtomicLong(0);

                copyRecursiveWithProgress(source, dest, totalBytes, copiedBytes);

                if (isCutOperation && !isOperationCancelled) {
                    deleteRecursive(source);
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    dismissProgressDialog();
                    if (!isOperationCancelled) {
                        Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
                        finishPaste(isCutOperation);
                    } else {
                        Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                        deleteRecursive(dest);
                        loadDirectory(currentDir);
                    }
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    dismissProgressDialog();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void finishPaste(boolean clearClipboard) {
        if (clearClipboard) {
            clipboardFile = null;
            if (fabPaste != null) fabPaste.setVisibility(View.GONE);
        }
        loadDirectory(currentDir);
    }

    private void showProgressDialog(String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);
        builder.setCancelable(false);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        progressPercent = new TextView(getContext());
        progressPercent.setText("0%");
        progressPercent.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        progressPercent.setTextSize(18);
        layout.addView(progressPercent);

        progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        layout.addView(progressBar);

        progressText = new TextView(getContext());
        progressText.setText("Calculating size...");
        progressText.setPadding(0, 20, 0, 0);
        layout.addView(progressText);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", (d, w) -> isOperationCancelled = true);

        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void updateProgress(long current, long total) {
        int percent = (total > 0) ? (int) ((current * 100) / total) : 0;
        final String status = formatSize(current) + " / " + formatSize(total);
        
        new Handler(Looper.getMainLooper()).post(() -> {
            if (progressBar != null) progressBar.setProgress(percent);
            if (progressPercent != null) progressPercent.setText(percent + "%");
            if (progressText != null) progressText.setText(status);
        });
    }

    private long getFolderSize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) size += getFolderSize(child);
            }
        } else {
            size = file.length();
        }
        return size;
    }

    private void copyRecursiveWithProgress(File src, File dst, long totalBytes, AtomicLong copiedBytes) throws IOException {
        if (isOperationCancelled) return;

        if (src.isDirectory()) {
            if (!dst.exists()) dst.mkdirs();
            String[] children = src.list();
            if (children != null) {
                for (String child : children) {
                    copyRecursiveWithProgress(new File(src, child), new File(dst, child), totalBytes, copiedBytes);
                }
            }
        } else {
            copyFileChunks(src, dst, totalBytes, copiedBytes);
        }
    }

    private void copyFileChunks(File source, File dest, long totalBytes, AtomicLong totalCopied) throws IOException {
        try (FileInputStream inStr = new FileInputStream(source);
             FileOutputStream outStr = new FileOutputStream(dest);
             FileChannel inChannel = inStr.getChannel();
             FileChannel outChannel = outStr.getChannel()) {

            long size = inChannel.size();
            long position = 0;
            long chunkSize = 16 * 1024 * 1024; 

            while (position < size) {
                if (isOperationCancelled) break;
                
                long count = Math.min(chunkSize, size - position);
                long transferred = outChannel.transferFrom(inChannel, position, count);
                
                position += transferred;
                long currentTotal = totalCopied.addAndGet(transferred);
                
                updateProgress(currentTotal, totalBytes);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
    
    private void renameFile(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename");
        final EditText input = new EditText(getContext());
        input.setText(file.getName());
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString();
            File newFile = new File(file.getParent(), newName);
            if (file.renameTo(newFile)) {
                loadDirectory(currentDir);
            } else {
                Toast.makeText(getContext(), "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private boolean isExecutable(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".msi");
    }

    private void showFileOptions(File file, View anchor) {
        PopupMenu popup = new PopupMenu(getContext(), anchor);
        if (isExecutable(file)) popup.getMenu().add("Create Shortcut (Winlator)");
        popup.getMenu().add("Copy");
        popup.getMenu().add("Cut (Move)");
        popup.getMenu().add("Rename");
        popup.getMenu().add("Delete");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Create Shortcut (Winlator)")) checkContainersAndCreate(file);
            else if (title.equals("Copy")) copyToClipboard(file, false);
            else if (title.equals("Cut (Move)")) copyToClipboard(file, true);
            else if (title.equals("Rename")) renameFile(file);
            else if (title.equals("Delete")) {
                new AlertDialog.Builder(getContext())
                    .setTitle("Delete")
                    .setMessage("Are you sure you want to delete " + file.getName() + "?")
                    .setPositiveButton("Yes", (d, w) -> {
                        deleteRecursive(file);
                        loadDirectory(currentDir);
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
            return true;
        });
        popup.show();
    }

    private void checkContainersAndCreate(File file) {
        ArrayList<Container> containers = containerManager.getContainers();
        if (containers == null || containers.isEmpty()) {
            Toast.makeText(getContext(), "Create a container first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (containers.size() == 1) {
            createShortcutDirectly(file, containers.get(0));
        } else {
            String[] names = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) names[i] = containers.get(i).getName();
            new AlertDialog.Builder(getContext())
                .setTitle("Select Container")
                .setItems(names, (dialog, which) -> createShortcutDirectly(file, containers.get(which)))
                .show();
        }
    }

    private void createShortcutDirectly(File file, Container container) {
        try {
            String displayName = getSmartDisplayName(file);
            String unixPath = file.getAbsolutePath();
            File shortcutsDir = container.getDesktopDir();
            if (!shortcutsDir.exists()) shortcutsDir.mkdirs();
            File desktopFile = new File(shortcutsDir, displayName + ".desktop");
            
             try (PrintWriter writer = new PrintWriter(new FileWriter(desktopFile))) {
                writer.println("[Desktop Entry]");
                writer.println("Name=" + displayName);
                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"" + unixPath + "\"");
                writer.println("Type=Application");
                writer.println("container_id:" + container.id);
            }
            Toast.makeText(getContext(), "Shortcut created!", Toast.LENGTH_SHORT).show();
            
             File iconsDir = new File(Environment.getExternalStorageDirectory(), "Winlator/icons");
             if (!iconsDir.exists()) iconsDir.mkdirs();
             GameImageFetcher.fetchIcon(displayName, new File(iconsDir, displayName + ".png"), null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSmartDisplayName(File file) {
        String filename = cleanGameName(file.getName());
        String lowerName = filename.toLowerCase();
        List<String> genericNames = Arrays.asList(
            "game", "launcher", "setup", "installer", "start", "run", 
            "speed", "update", "patch", "loader", "client", "app", "main", "boot", "play",
            "application", "shipping", "x64", "x86", "win64", "win32", "binaries"
        );
        
        boolean isModOrGeneric = false;
        if (lowerName.contains("mod") || lowerName.contains("fix") || 
            lowerName.contains("crack") || lowerName.contains("patch")) {
            isModOrGeneric = true;
        }
        if (!isModOrGeneric && filename.length() < 4) isModOrGeneric = true;
        
        if (!isModOrGeneric) {
            for (String gen : genericNames) {
                if (lowerName.equals(gen) || lowerName.startsWith(gen + " ")) {
                    isModOrGeneric = true;
                    break;
                }
            }
        }
        
        if (isModOrGeneric) {
            File parent = file.getParentFile();
            if (parent != null) {
                String parentName = cleanGameName(parent.getName());
                List<String> genericFolders = Arrays.asList("bin", "bin32", "bin64", "system", "release", "retail", "win64");
                if (genericFolders.contains(parentName.toLowerCase())) {
                    File grandParent = parent.getParentFile();
                    if (grandParent != null) return cleanGameName(grandParent.getName());
                }
                return parentName;
            }
        }
        return filename;
    }private String cleanGameName(String filename) {
        String name = filename;
        int pos = name.lastIndexOf(".");
        if (pos > 0) name = name.substring(0, pos);
        name = name.replace("_", " ").replace(".", " ").replace("-", " ");
        name = name.replaceAll("(?i)\\b(v\\d+|repack|setup|installer|portable|goty|edition)\\b", "");
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
                holder.tvDetails.setText("Folder");
                holder.btMenu.setVisibility(View.VISIBLE);
                holder.btMenu.setImageResource(android.R.drawable.ic_menu_more);
                holder.btMenu.setOnClickListener(v -> showFileOptions(file, holder.btMenu));
                holder.itemView.setOnClickListener(v -> loadDirectory(file));
            } else {
                holder.tvDetails.setText(formatSize(file.length()));
                boolean isExe = isExecutable(file);
                if (isExe) holder.ivIcon.setImageResource(R.drawable.icon_wine); 
                else holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                holder.btMenu.setVisibility(View.VISIBLE);
                holder.btMenu.setImageResource(android.R.drawable.ic_menu_more);
                holder.btMenu.setOnClickListener(v -> showFileOptions(file, holder.btMenu));
                holder.itemView.setOnClickListener(v -> Toast.makeText(getContext(), file.getName(), Toast.LENGTH_SHORT).show());
            }
        }@Override
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