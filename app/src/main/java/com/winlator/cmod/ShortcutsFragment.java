package com.winlator.cmod;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.bigpicture.steamgrid.SteamGridDBApi;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridGridsResponse;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridGridsResponseDeserializer;
import com.winlator.cmod.bigpicture.steamgrid.SteamGridSearchResponse;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog;
import com.winlator.cmod.core.ExeIconExtractor;
import com.winlator.cmod.core.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ShortcutsFragment extends Fragment {
    private static final String TAG = "ShortcutsFragment";
    private static final String STEAMGRID_BASE_URL = "https://www.steamgriddb.com/api/v2/";
    private static String STEAMGRID_API_KEY = "0324c52513634547a7b32d6d323635d0";

    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private ContainerManager manager;
    private SharedPreferences preferences;
    
    private boolean isGridView = false;
    private DividerItemDecoration dividerItemDecoration;

    private Shortcut shortcutForIconUpdate;
    private ActivityResultLauncher<String> iconPickerLauncher;
    private ActivityResultLauncher<String> contentPickerLauncher;
    private com.winlator.cmod.core.Callback<Uri> pendingContentPickerCallback;

    public static final int IMPORT_SHORTCUT = 1005;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        iconPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && shortcutForIconUpdate != null) {
                updateShortcutIcon(uri, shortcutForIconUpdate);
            }
        });
        contentPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            com.winlator.cmod.core.Callback<Uri> cb = pendingContentPickerCallback;
            pendingContentPickerCallback = null;
            if (cb != null) cb.call(uri);
        });
    }

    public void pickContentArchive(com.winlator.cmod.core.Callback<Uri> callback) {
        pendingContentPickerCallback = callback;
        contentPickerLauncher.launch("*/*");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (recyclerView != null) updateLayoutManager();
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

        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isGridView = preferences.getBoolean("shortcuts_grid_view", true);

        updateLayoutManager();

        return frameLayout;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        
        MenuItem item = menu.add(0, 1, 0, isGridView ? "List View" : "Grid View");
        item.setIcon(isGridView ? android.R.drawable.ic_menu_agenda : android.R.drawable.ic_menu_gallery);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            isGridView = !isGridView;
            preferences.edit().putBoolean("shortcuts_grid_view", isGridView).apply();
            updateLayoutManager();
            getActivity().invalidateOptionsMenu(); 
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void fetchCoverFromSteamGrid(Shortcut shortcut, File destFile,
                                          Runnable onSuccess, Runnable onFail) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (prefs.getBoolean("enable_custom_api_key", false)) {
            String custom = prefs.getString("custom_api_key", "");
            if (custom != null && !custom.isEmpty()) STEAMGRID_API_KEY = custom;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(STEAMGRID_BASE_URL)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SteamGridDBApi api = retrofit.create(SteamGridDBApi.class);
        Call<SteamGridSearchResponse> call = api.searchGame("Bearer " + STEAMGRID_API_KEY, shortcut.name);

        call.enqueue(new Callback<SteamGridSearchResponse>() {
            @Override
            public void onResponse(Call<SteamGridSearchResponse> call, Response<SteamGridSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().data != null && !response.body().data.isEmpty()) {
                    int gameId = response.body().data.get(0).id;
                    fetchSteamGridCovers(gameId, destFile, onSuccess, onFail);
                } else {
                    Log.d(TAG, "SteamGridDB: sem resultado para '" + shortcut.name + "', usando fallback");
                    if (onFail != null) onFail.run();
                }
            }

            @Override
            public void onFailure(Call<SteamGridSearchResponse> call, Throwable t) {
                Log.e(TAG, "SteamGridDB search falhou: " + t.getMessage());
                if (onFail != null) onFail.run();
            }
        });
    }

    private void fetchSteamGridCovers(int gameId, File destFile,
                                       Runnable onSuccess, Runnable onFail) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(SteamGridGridsResponse.class, new SteamGridGridsResponseDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(STEAMGRID_BASE_URL)
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        SteamGridDBApi api = retrofit.create(SteamGridDBApi.class);
        Call<SteamGridGridsResponse> gridsCall = api.getGridsByGameId(
                "Bearer " + STEAMGRID_API_KEY, gameId, "alternate", "600x900", "static");

        gridsCall.enqueue(new Callback<SteamGridGridsResponse>() {
            @Override
            public void onResponse(Call<SteamGridGridsResponse> call, Response<SteamGridGridsResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().data.isEmpty()) {
                    String url = response.body().data.get(0).url;
                    downloadAndSaveCover(url, destFile, onSuccess, onFail);
                } else {
                    Log.d(TAG, "SteamGridDB: nenhum grid encontrado para gameId=" + gameId);
                    if (onFail != null) onFail.run();
                }
            }

            @Override
            public void onFailure(Call<SteamGridGridsResponse> call, Throwable t) {
                Log.e(TAG, "SteamGridDB grids falhou: " + t.getMessage());
                if (onFail != null) onFail.run();
            }
        });
    }

    private void downloadAndSaveCover(String url, File destFile,
                                       Runnable onSuccess, Runnable onFail) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();
                Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
                if (bmp == null) { if (onFail != null) onFail.run(); return; }

                if (destFile.getParentFile() != null) destFile.getParentFile().mkdirs();

                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                }
                bmp.recycle();
                Log.d(TAG, "SteamGridDB cover salvo: " + destFile.getAbsolutePath());
                if (onSuccess != null) onSuccess.run();
            } catch (Exception e) {
                Log.e(TAG, "Falha ao baixar cover do SteamGridDB: " + e.getMessage());
                if (onFail != null) onFail.run();
            }
        });
    }

    private File getImagesDir(boolean isCover) {
        File targetDir = new File(Environment.getExternalStorageDirectory(), isCover ? "Winlator/covers" : "Winlator/icons");
        if (!targetDir.exists()) targetDir.mkdirs();
        
        File nomedia = new File(targetDir, ".nomedia");
        if (!nomedia.exists()) {
            try { nomedia.createNewFile(); } catch (IOException e) {}
        }
        return targetDir;
    }

    private void updateLayoutManager() {
        if (isGridView) {
            int orientation = getResources().getConfiguration().orientation;
            boolean isLandscape = (orientation == Configuration.ORIENTATION_LANDSCAPE);
            int spanCount = isLandscape ? 3 : 2;

            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount)); 
            if (dividerItemDecoration != null) {
                recyclerView.removeItemDecoration(dividerItemDecoration);
                dividerItemDecoration = null;
            }
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            if (dividerItemDecoration == null) {
                dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
                recyclerView.addItemDecoration(dividerItemDecoration);
            }
        }
        
        if (recyclerView.getAdapter() != null) recyclerView.setAdapter(recyclerView.getAdapter());
    }

    public void loadShortcutsList() {
        ArrayList<Shortcut> shortcuts = manager.loadShortcuts();
        if (shortcuts != null) {
            shortcuts.removeIf(shortcut -> shortcut == null || shortcut.file == null || shortcut.file.getName().isEmpty());
            
            Bitmap defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon_wine);
            for (Shortcut shortcut : shortcuts) {
                if (shortcut.icon == null) shortcut.icon = defaultIcon;
            }
            
            recyclerView.setAdapter(new ShortcutsAdapter(shortcuts));
            if (shortcuts.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);
            else emptyTextView.setVisibility(View.GONE);
        }
    }

    private void updateShortcutIcon(Uri sourceUri, Shortcut shortcut) {
        try {
            File targetDir = getImagesDir(false);
            String baseName = FileUtils.getBasename(shortcut.file.getPath());
            File destFile = new File(targetDir, baseName + ".user.png");

            try (InputStream is = getContext().getContentResolver().openInputStream(sourceUri);
                 OutputStream os = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) os.write(buffer, 0, length);
            }

            Toast.makeText(getContext(), "Icon updated!", Toast.LENGTH_SHORT).show();
            if (recyclerView.getAdapter() != null) recyclerView.getAdapter().notifyDataSetChanged();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving icon", Toast.LENGTH_SHORT).show();
        }
    }

    
    private File resolveExeFile(Shortcut item) {
        if (item.path == null || item.path.isEmpty()) return null;

        String path = item.path.replace("\\", "/").trim();
        
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }

        
        if (path.startsWith("/")) {
            File f = new File(path);
            if (f.exists()) return f;
        }

        
        String normalized = path.replaceAll("^[A-Za-z]:/", "");
        File containerRoot = item.container.getRootDir();
        if (containerRoot != null) {
            File candidate = new File(containerRoot, ".wine/drive_c/" + normalized);
            if (candidate.exists()) return candidate;
        }

        return null;
    }

    private class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {
        private final List<Shortcut> data;

        private class ViewHolder extends RecyclerView.ViewHolder {
            private final View menuButton; 
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

        public ShortcutsAdapter(List<Shortcut> data) { this.data = data; }

        @Override
        public int getItemViewType(int position) { return isGridView ? 1 : 0; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = (viewType == 1) ? R.layout.shortcut_grid_item : R.layout.shortcut_list_item;
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            if (holder.menuButton != null) holder.menuButton.setOnClickListener(null);
            holder.innerArea.setOnClickListener(null);
            holder.innerArea.setOnLongClickListener(null);
            super.onViewRecycled(holder);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final Shortcut item = data.get(position);
            
            holder.title.setText(item.name);
            holder.subtitle.setText(item.container.getName());
            holder.innerArea.setOnClickListener((v) -> runFromShortcut(item));

            if (isGridView) {
                
                holder.innerArea.setOnLongClickListener(v -> {
                    showListItemMenu(holder.title, item);
                    return true;
                });
                if (holder.menuButton != null) holder.menuButton.setVisibility(View.GONE);
                boolean isLandscape = getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
                boolean useCover = true;

                try {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams imgParams =
                            (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                                    holder.imageView.getLayoutParams();
                    imgParams.dimensionRatio = "3:4"; // Cover ratio in both orientations
                    holder.imageView.setLayoutParams(imgParams);
                } catch (ClassCastException ignored) {}

                String baseName  = FileUtils.getBasename(item.file.getPath());
                File userIcon  = new File(getImagesDir(false), baseName + ".user.png");
                File coverFile = new File(getImagesDir(true),  baseName + ".png");

                if (useCover) {

                    if (userIcon.exists()) {
                        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(userIcon.getPath()));
                    } else if (coverFile.exists()) {
                        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(coverFile.getPath()));
                    } else {
                        holder.imageView.setImageResource(R.drawable.icon_wine);
                        fetchCoverFromSteamGrid(item, coverFile,
                            () -> { // SteamGridDB succeeded
                                if (getActivity() != null)
                                    getActivity().runOnUiThread(() -> {
                                        if (coverFile.exists())
                                            holder.imageView.setImageBitmap(BitmapFactory.decodeFile(coverFile.getPath()));
                                    });
                            },
                            () -> { // SteamGridDB failed — fall back to EXE icon
                                File exeFile = resolveExeFile(item);
                                if (exeFile != null) {
                                    ExeIconExtractor.extractAsync(exeFile, coverFile, true, () -> {
                                        if (getActivity() != null)
                                            getActivity().runOnUiThread(() -> {
                                                if (coverFile.exists())
                                                    holder.imageView.setImageBitmap(BitmapFactory.decodeFile(coverFile.getPath()));
                                            });
                                    });
                                }
                            });
                    }
                }
            } else {
                holder.innerArea.setOnLongClickListener(null);
                if (holder.menuButton != null) {
                    holder.menuButton.setVisibility(View.VISIBLE);
                    holder.menuButton.setOnClickListener((v) -> showListItemMenu(v, item));
                }

                String listBaseName = FileUtils.getBasename(item.file.getPath());
                File targetDir = getImagesDir(false);
                File userIcon  = new File(targetDir, listBaseName + ".user.png"); // user-set
                File autoIcon  = new File(targetDir, listBaseName + ".png");       // auto-extracted

                if (userIcon.exists()) {
                    holder.imageView.setImageBitmap(BitmapFactory.decodeFile(userIcon.getPath()));
                } else if (autoIcon.exists()) {
                    holder.imageView.setImageBitmap(BitmapFactory.decodeFile(autoIcon.getPath()));
                } else if (item.icon != null) {
                    holder.imageView.setImageBitmap(item.icon);
                } else {
                    holder.imageView.setImageResource(R.drawable.icon_wine);
                    File exeFile = resolveExeFile(item);
                    if (exeFile != null) {
                        ExeIconExtractor.extractAsync(exeFile, autoIcon, false, () -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (autoIcon.exists()) {
                                        holder.imageView.setImageBitmap(BitmapFactory.decodeFile(autoIcon.getPath()));
                                    }
                                });
                            }
                        });
                    }
                }
            }
        }

        @Override
        public final int getItemCount() { return data.size(); }

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
                else if (itemId == R.id.shortcut_change_icon) {
                    shortcutForIconUpdate = shortcut;
                    iconPickerLauncher.launch("image/*");
                }
                else if (itemId == R.id.shortcut_remove) {
                    ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_shortcut, () -> {
                        boolean fileDeleted = shortcut.file.delete();
                        try {
                            String basePath = shortcut.file.getPath().substring(0, shortcut.file.getPath().lastIndexOf("."));
                            new File(basePath + ".lnk").delete();
                            new File(basePath + ".bat").delete();
                        } catch (Exception e) {}

                        if (fileDeleted) {
                            disableShortcutOnScreen(requireContext(), shortcut);
                            loadShortcutsList();
                            Toast.makeText(context, "Shortcut removed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else if (itemId == R.id.shortcut_clone_to_container) {
                    ContainerManager containerManager = new ContainerManager(context);
                    ArrayList<Container> containers = containerManager.getContainers();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Select a container");
                    String[] containerNames = new String[containers.size()];
                    for (int i = 0; i < containers.size(); i++) containerNames[i] = containers.get(i).getName();
                    builder.setItems(containerNames, (dialog, which) -> {
                        if (shortcut.cloneToContainer(containers.get(which))) {
                            Toast.makeText(context, "Cloned successfully.", Toast.LENGTH_SHORT).show();
                            loadShortcutsList(); 
                        }
                    });
                    builder.show();
                }
                else if (itemId == R.id.shortcut_add_to_home_screen) {
                    if (shortcut.getExtra("uuid").equals("")) shortcut.genUUID();
                    addShortcutToScreen(shortcut);
                }
                else if (itemId == R.id.shortcut_export) {
                    exportShortcut(shortcut);
                }
                return true;
            });
            listItemMenu.show();
        }

        private void runFromShortcut(Shortcut shortcut) {
            Activity activity = getActivity();
            if (!XrActivity.isEnabled(getContext())) {
                Intent intent = new Intent(activity, XServerDisplayActivity.class);
                intent.putExtra("container_id", shortcut.container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                intent.putExtra("shortcut_name", shortcut.name); 
                intent.putExtra("disableXinput", shortcut.getExtra("disableXinput", "0")); 
                intent.putExtra("native_rendering", shortcut.getNativeRendering());
                activity.startActivity(intent);
            } else XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.getPath());
        }

        private void exportShortcut(Shortcut shortcut) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String uriString = sharedPreferences.getString("shortcuts_export_path_uri", null);
            File shortcutsDir;

            if (uriString != null) {
                Uri folderUri = Uri.parse(uriString);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(getContext(), folderUri);
                if (pickedDir == null || !pickedDir.canWrite()) return;
                shortcutsDir = new File(FileUtils.getFilePathFromUri(getContext(), folderUri));
            } else {
                shortcutsDir = new File(SettingsFragment.DEFAULT_SHORTCUT_EXPORT_PATH);
            }

            if (!shortcutsDir.exists() && !shortcutsDir.mkdirs()) return;
            File exportFile = new File(shortcutsDir, shortcut.file.getName());
            boolean containerIdFound = false;

            try {
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(shortcut.file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("container_id:")) {
                            lines.add("container_id:" + shortcut.container.id);
                            containerIdFound = true;
                        } else lines.add(line);
                    }
                }
                if (!containerIdFound) lines.add("container_id:" + shortcut.container.id);
                try (FileWriter writer = new FileWriter(exportFile, false)) {
                    for (String line : lines) writer.write(line + "\n");
                    writer.flush();
                }
                Toast.makeText(getContext(), exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {}
        }
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
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            File iconDir = getImagesDir(false);
            File imgFile = new File(iconDir, FileUtils.getBasename(shortcut.file.getPath()) + ".png");
            Bitmap bmp = imgFile.exists() ? BitmapFactory.decodeFile(imgFile.getPath()) : shortcut.icon;
            if (bmp == null) bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon_wine);
            
            shortcutManager.requestPinShortcut(buildScreenShortCut(shortcut.name, shortcut.name, shortcut.container.id,
                    shortcut.file.getPath(), Icon.createWithBitmap(bmp), shortcut.getExtra("uuid")), null);
        }
    }

    public static void disableShortcutOnScreen(Context context, Shortcut shortcut) {
        ShortcutManager shortcutManager = getSystemService(context, ShortcutManager.class);
        try {
            shortcutManager.disableShortcuts(Collections.singletonList(shortcut.getExtra("uuid")), context.getString(R.string.shortcut_not_available));
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
