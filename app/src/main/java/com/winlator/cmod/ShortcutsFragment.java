package com.winlator.cmod;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.core.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShortcutsFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    
    private Shortcut shortcutForIconUpdate;
    private ActivityResultLauncher<String> iconPickerLauncher;

    public static final int IMPORT_SHORTCUT = 1005;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        iconPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && shortcutForIconUpdate != null) {
                updateShortcutIcon(uri, shortcutForIconUpdate);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ContainerManager(getContext());
        loadShortcutsList();
        if (getActivity() != null && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.shortcuts);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout)inflater.inflate(R.layout.shortcuts_fragment, container, false);
        recyclerView = frameLayout.findViewById(R.id.RecyclerView);
        emptyTextView = frameLayout.findViewById(R.id.TVEmptyText);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        return frameLayout;
    }

    public void loadShortcutsList() {
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts();
        if (shortcuts != null) {
            shortcuts.removeIf(shortcut -> shortcut == null || shortcut.file == null || shortcut.file.getName().isEmpty());
            
            // Aplica o ícone do Wine se o atalho não tiver ícone (evita crash e ícone invisível)
            Bitmap defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_wine);
            for (Shortcut shortcut : shortcuts) {
                if (shortcut.icon == null) {
                    shortcut.icon = defaultIcon;
                }
            }
            
            recyclerView.setAdapter(new ShortcutsAdapter(shortcuts));
            if (shortcuts.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);
            else emptyTextView.setVisibility(View.GONE);
        }
    }

    private void updateShortcutIcon(Uri sourceUri, Shortcut shortcut) {
        try {
            File externalStorage = Environment.getExternalStorageDirectory();
            File iconsDir = new File(externalStorage, "Winlator/icons");
            
            if (!iconsDir.exists()) {
                iconsDir.mkdirs();
            }

            String baseName = FileUtils.getBasename(shortcut.file.getPath());
            File destFile = new File(iconsDir, baseName + ".png");

            try (InputStream is = getContext().getContentResolver().openInputStream(sourceUri);
                 OutputStream os = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            }

            Toast.makeText(getContext(), "Icon updated!", Toast.LENGTH_SHORT).show();
            loadShortcutsList(); 

        } catch (Exception e) {
            Log.e("ShortcutsFragment", "Error saving icon", e);
            Toast.makeText(getContext(), "Error saving icon", Toast.LENGTH_SHORT).show();
        }
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {
        private final List<Shortcut> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageButton menuButton;
            private final ImageView imageView;
            private final TextView title;
            private final TextView subtitle;
            private final View innerArea;

            private ViewHolder(View view) {
                super(view);
                this.imageView = view.findViewById(R.id.ImageView);
                this.title = view.findViewById(R.id.TVTitle);
                this.subtitle = view.findViewById(R.id.TVSubtitle);
                this.menuButton = view.findViewById(R.id.BTMenu);
                this.innerArea = view.findViewById(R.id.LLInnerArea);
            }
        }

        public ShortcutsAdapter(List<Shortcut> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.shortcut_list_item, parent, false));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            holder.menuButton.setOnClickListener(null);
            holder.innerArea.setOnClickListener(null);
            holder.imageView.setOnClickListener(null);
            super.onViewRecycled(holder);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Shortcut item = data.get(position);
            
            if (item.icon != null) holder.imageView.setImageBitmap(item.icon);
            else holder.imageView.setImageResource(R.drawable.icon_wine); // Fallback extra
            
            holder.title.setText(item.name);
            holder.subtitle.setText(item.container.getName());
            
            holder.menuButton.setOnClickListener((v) -> showListItemMenu(v, item));
            holder.innerArea.setOnClickListener((v) -> runFromShortcut(item));

            holder.imageView.setOnClickListener((v) -> {
                shortcutForIconUpdate = item;
                iconPickerLauncher.launch("image/*");
            });
        }

        @Override
        public final int getItemCount() {
            return data.size();
        }

        private void showListItemMenu(View anchorView, final Shortcut shortcut) {
            final Context context = getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listItemMenu.setForceShowIcon(true);

            listItemMenu.inflate(R.menu.shortcut_popup_menu);
            listItemMenu.setOnMenuItemClickListener((menuItem) -> {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.shortcut_settings) {
                    (new ShortcutSettingsDialog(ShortcutsFragment.this, shortcut)).show();
                }
                else if (itemId == R.id.shortcut_remove) {
                    ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_shortcut, () -> {
                        boolean fileDeleted = shortcut.file.delete();
                        
                        try {
                            File lnkFile = new File(shortcut.file.getPath().substring(0, shortcut.file.getPath().lastIndexOf(".")) + ".lnk");
                            if (lnkFile.exists()) lnkFile.delete();
                        } catch (Exception e) {}

                        if (fileDeleted) {
                            disableShortcutOnScreen(requireContext(), shortcut);
                            loadShortcutsList();
                            Toast.makeText(context, "Shortcut removed successfully.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to remove the shortcut.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if (itemId == R.id.shortcut_clone_to_container) {
                    ContainerManager containerManager = new ContainerManager(context);
                    ArrayList<Container> containers = containerManager.getContainers();
                    showContainerSelectionDialog(containers, selectedContainer -> {
                        if (shortcut.cloneToContainer(selectedContainer)) {
                            Toast.makeText(context, "Shortcut cloned successfully.", Toast.LENGTH_SHORT).show();
                            loadShortcutsList(); 
                        } else {
                            Toast.makeText(context, "Failed to clone shortcut.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if (itemId == R.id.shortcut_add_to_home_screen) {
                    if (shortcut.getExtra("uuid").equals(""))
                        shortcut.genUUID();
                    addShortcutToScreen(shortcut);
                }
                else if (itemId == R.id.shortcut_export) {
                    exportShortcut(shortcut);
                }
                else if (itemId == R.id.shortcut_import) {
                    importShortcut(shortcut);
                }
                else if (itemId == R.id.shortcut_properties) {
                    showShortcutProperties(shortcut);
                }
                return true;
            });
            listItemMenu.show();
        }

        private void showContainerSelectionDialog(ArrayList<Container> containers, OnContainerSelectedListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Select a container");

            String[] containerNames = new String[containers.size()];
            for (int i = 0; i < containers.size(); i++) {
                containerNames[i] = containers.get(i).getName();
            }

            builder.setItems(containerNames, (dialog, which) -> {
                listener.onContainerSelected(containers.get(which));
            });
            builder.show();
        }
        
        private void runFromShortcut(Shortcut shortcut) {
            Activity activity = getActivity();

            if (!XrActivity.isEnabled(getContext())) {
                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", shortcut.container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                intent.putExtra("shortcut_name", shortcut.name); 
                String disableXinputValue = shortcut.getExtra("disableXinput", "0"); 
                intent.putExtra("disableXinput", disableXinputValue); 
                activity.startActivity(intent);
            }
            else XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
        }

        private void exportShortcut(Shortcut shortcut) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String uriString = sharedPreferences.getString("shortcuts_export_path_uri", null);

            File shortcutsDir;

            if (uriString != null) {
                Uri folderUri = Uri.parse(uriString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), folderUri);

                if (pickedDir == null || !pickedDir.canWrite()) {
                    Toast.makeText(getContext(), "Cannot write to the selected folder", Toast.LENGTH_SHORT).show();
                    return;
                }

                shortcutsDir = new File(FileUtils.getFilePathFromUri(getContext(), folderUri));
            } else {
                shortcutsDir = new File(SettingsFragment.DEFAULT_SHORTCUT_EXPORT_PATH);
            }

            if (!shortcutsDir.exists() && !shortcutsDir.mkdirs()) {
                Toast.makeText(getContext(), "Failed to create default directory", Toast.LENGTH_SHORT).show();
                return;
            }

            File exportFile = new File(shortcutsDir, shortcut.file.getName());

            boolean fileExists = exportFile.exists();
            boolean containerIdFound = false;

            try {
                List<String> lines = new ArrayList<>();

                try (BufferedReader reader = new BufferedReader(new FileReader(shortcut.file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("container_id:")) {
                            lines.add("container_id:" + shortcut.container.id);
                            containerIdFound = true;
                        } else {
                            lines.add(line);
                        }
                    }
                }

                if (!containerIdFound) {
                    lines.add("container_id:" + shortcut.container.id);
                }

                try (FileWriter writer = new FileWriter(exportFile, false)) {
                    for (String line : lines) {
                        writer.write(line + "\n");
                    }
                    writer.flush();
                }

                Toast.makeText(getContext(), fileExists ? "Shortcut Updated" : "Shortcut Exported", Toast.LENGTH_LONG).show();

            } catch (IOException e) {
                Log.e("ShortcutsFragment", "Failed to export shortcut", e);
                Toast.makeText(getContext(), "Failed to export shortcut", Toast.LENGTH_LONG).show();
            }
        }

        private void importShortcut(Shortcut shortcut) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String uriString = sharedPreferences.getString("shortcuts_export_path_uri", null);
            File shortcutsDir;
            Container container = shortcut.container;
            File desktopFile = shortcut.file;

            if (uriString != null) {
                Uri folderUri = Uri.parse(uriString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), folderUri);

                if (pickedDir == null || !pickedDir.canWrite()) {
                    Toast.makeText(getContext(), "Cannot write to the selected folder", Toast.LENGTH_SHORT).show();
                    return;
                }

                shortcutsDir = new File(FileUtils.getFilePathFromUri(getContext(), folderUri));
            } else {
                shortcutsDir = new File(SettingsFragment.DEFAULT_SHORTCUT_EXPORT_PATH);
            }

            if (!shortcutsDir.exists()) {
                Toast.makeText(getContext(), "No exported shortcuts found", Toast.LENGTH_LONG).show();
                return;
            }

            boolean found = false;

            for (File f : shortcutsDir.listFiles()) {
                if (f.getName().endsWith(".desktop")) {
                    Shortcut tempShorcut = new Shortcut(container, f);
                    if (tempShorcut.getExecutable().equals(shortcut.getExecutable())) {
                        found = true;
                        try {
                            Files.copy(f.toPath(), desktopFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            Toast.makeText(getContext(), "Shortcut imported successfully", Toast.LENGTH_SHORT).show();
                            loadShortcutsList();
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Failed to import shortcut", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            if (!found) {
                Toast.makeText(getContext(), "No matching shortcut found to import", Toast.LENGTH_SHORT).show();
            }
        }

        private void showShortcutProperties(Shortcut shortcut) {
            SharedPreferences playtimePrefs = getContext().getSharedPreferences("playtime_stats", Context.MODE_PRIVATE);

            String playtimeKey = shortcut.name + "_playtime";
            String playCountKey = shortcut.name + "_play_count";

            long totalPlaytime = playtimePrefs.getLong(playtimeKey, 0);
            int playCount = playtimePrefs.getInt(playCountKey, 0);

            long seconds = (totalPlaytime / 1000) % 60;
            long minutes = (totalPlaytime / (1000 * 60)) % 60;
            long hours = (totalPlaytime / (1000 * 60 * 60)) % 24;
            long days = (totalPlaytime / (1000 * 60 * 60 * 24));

            String playtimeFormatted = String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);

            ContentDialog dialog = new ContentDialog(getContext(), R.layout.shortcut_properties_dialog);
            dialog.setTitle("Properties");

            TextView playCountTextView = dialog.findViewById(R.id.play_count);
            TextView playtimeTextView = dialog.findViewById(R.id.playtime);

            playCountTextView.setText("Number of times played: " + playCount);
            playtimeTextView.setText("Playtime: " + playtimeFormatted);

            Button resetPropertiesButton = dialog.findViewById(R.id.reset_properties);

            resetPropertiesButton.setOnClickListener(v -> {
                playtimePrefs.edit().remove(playtimeKey).remove(playCountKey).apply();
                Toast.makeText(getContext(), "Properties reset successfully.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.show();
        }
    }
    
    public interface OnContainerSelectedListener {
        void onContainerSelected(Container container);
    }

    private ShortcutInfo buildScreenShortCut(String shortLabel, String longLabel, int containerId, String shortcutPath, Icon icon, String uuid) {
        Intent intent = new Intent(getActivity(), XServerDisplayActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("container_id", containerId);
        intent.putExtra("shortcut_path", shortcutPath);

        return new ShortcutInfo.Builder(getActivity(), uuid)
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(intent)
                .build();
    }

    private void addShortcutToScreen(Shortcut shortcut) {
        ShortcutManager shortcutManager = getSystemService(requireContext(), ShortcutManager.class);
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported())
            shortcutManager.requestPinShortcut(buildScreenShortCut(shortcut.name, shortcut.name, shortcut.container.id,
                    shortcut.file.getPath(), Icon.createWithBitmap(shortcut.icon), shortcut.getExtra("uuid")), null);
    }

    public static void disableShortcutOnScreen(Context context, Shortcut shortcut) {
        ShortcutManager shortcutManager = getSystemService(context, ShortcutManager.class);
        try {
            shortcutManager.disableShortcuts(Collections.singletonList(shortcut.getExtra("uuid")),
                    context.getString(R.string.shortcut_not_available));
        } catch (Exception e) {}
    }

    public void updateShortcutOnScreen(String shortLabel, String longLabel, int containerId, String shortcutPath, Icon icon, String uuid) {
        ShortcutManager shortcutManager = getSystemService(requireContext(), ShortcutManager.class);
        try {
            for (ShortcutInfo shortcutInfo : shortcutManager.getPinnedShortcuts()) {
                if (shortcutInfo.getId().equals(uuid)) {
                    shortcutManager.updateShortcuts(Collections.singletonList(
                            buildScreenShortCut(shortLabel, longLabel, containerId, shortcutPath, icon, uuid)));
                    break;
                }
            }
        } catch (Exception e) {}
    }
}