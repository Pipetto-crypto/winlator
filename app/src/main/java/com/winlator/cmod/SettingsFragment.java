package com.winlator.cmod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArrayMap;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.winlator.cmod.box64.Box64EditPresetDialog;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.ArrayUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.fexcore.FEXCoreEditPresetDialog;
import com.winlator.cmod.fexcore.FEXCorePreset;
import com.winlator.cmod.fexcore.FEXCorePresetManager;
import com.winlator.cmod.midi.MidiManager;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class SettingsFragment extends Fragment {
    public static final String DEFAULT_WINE_DEBUG_CHANNELS = "warn,err,fixme";
    public static final String DEFAULT_WINLATOR_PATH = Environment.getExternalStorageDirectory().getPath() + "/Winlator";
    public static final String DEFAULT_SHORTCUT_EXPORT_PATH = DEFAULT_WINLATOR_PATH + "/Shortcuts";
    
    // DEFINA SUA URL PADRÃO AQUI (O usuário pode mudar depois nas configurações)
    public static final String DEFAULT_DRIVERS_REPO = "https://raw.githubusercontent.com/SEU_USER/SEU_REPO/main/drivers.json";

    private Callback<Uri> installSoundFontCallback;
    private PreloaderDialog preloaderDialog;
    private SharedPreferences preferences;

    private CheckBox cbCursorLock;
    private CheckBox cbXinputToggle;
    private CheckBox cbEnableBigPictureMode;
    private CheckBox cbEnableCustomApiKey;
    private EditText etCustomApiKey;
    private CheckBox cbDarkMode;
    boolean isDarkMode;

    private static final int REQUEST_CODE_WINLATOR_PATH = 1002;
    private static final int REQUEST_CODE_SHORTCUT_EXPORT_PATH = 1003;
    private static final int REQUEST_CODE_INSTALL_SOUNDFONT = 1001;
    private static final int REQUEST_CODE_IMPORT_BOX64_PRESET = 1004;
    private static final int REQUEST_CODE_IMPORT_FEXCORE_PRESET = 1005;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyDynamicStylesRecursively(view);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        final Context context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        isDarkMode = preferences.getBoolean("dark_mode", false);
        applyDynamicStyles(view, isDarkMode);

        cbDarkMode = view.findViewById(R.id.CBDarkMode);
        cbDarkMode.setChecked(preferences.getBoolean("dark_mode", false));
        cbDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();
            updateTheme(isChecked);
        });

        cbEnableBigPictureMode = view.findViewById(R.id.CBEnableBigPictureMode);
        cbEnableBigPictureMode.setChecked(preferences.getBoolean("enable_big_picture_mode", false));

        initCustomApiKeySettings(view);

        cbCursorLock = view.findViewById(R.id.CBCursorLock);
        cbCursorLock.setChecked(preferences.getBoolean("cursor_lock", false));

        cbXinputToggle = view.findViewById(R.id.CBXinputToggle);
        cbXinputToggle.setChecked(preferences.getBoolean("xinput_toggle", false));

        setupPathSelectors(view);
        setupPresets(view);
        setupSoundFont(view);

        final CheckBox cbUseDRI3 = view.findViewById(R.id.CBUseDRI3);
        cbUseDRI3.setChecked(preferences.getBoolean("use_dri3", true));

        final CheckBox cbUseXR = view.findViewById(R.id.CBUseXR);
        cbUseXR.setChecked(preferences.getBoolean("use_xr", true));
        if (!XrActivity.isSupported()) {
            cbUseXR.setVisibility(View.GONE);
        }

        final CheckBox cbEnableWineDebug = view.findViewById(R.id.CBEnableWineDebug);
        cbEnableWineDebug.setChecked(preferences.getBoolean("enable_wine_debug", false));

        final ArrayList<String> wineDebugChannels = new ArrayList<>(Arrays.asList(preferences.getString("wine_debug_channels", DEFAULT_WINE_DEBUG_CHANNELS).split(",")));
        loadWineDebugChannels(view, wineDebugChannels);

        final CheckBox cbEnableBox64Logs = view.findViewById(R.id.CBEnableBox64Logs);
        cbEnableBox64Logs.setChecked(preferences.getBoolean("enable_box64_logs", false));

        setupCursorSpeed(view);

        final CheckBox cbEnableFileProvider = view.findViewById(R.id.CBEnableFileProvider);
        cbEnableFileProvider.setChecked(preferences.getBoolean("enable_file_provider", true));
        cbEnableFileProvider.setOnClickListener(v -> AppUtils.showToast(context, R.string.take_effect_next_startup));
        view.findViewById(R.id.BTHelpFileProvider).setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_file_provider));

        final CheckBox cbOpenInBrowser = view.findViewById(R.id.CBOpenWithAndroidBrowser);
        cbOpenInBrowser.setChecked(preferences.getBoolean("open_with_android_browser", false));

        final CheckBox cbShareClipboard = view.findViewById(R.id.CBShareAndroidClipboard);
        cbShareClipboard.setChecked(preferences.getBoolean("share_android_clipboard", false));

        // --- CONFIGURAÇÃO DE URLs ---
        final EditText etDownloadableContentsURL = view.findViewById(R.id.ETDownloadableContentsURL);
        etDownloadableContentsURL.setText(preferences.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES));

        // NOVO: Campo para URL dos Drivers
        final EditText etDriversRepoURL = view.findViewById(R.id.ETDriversRepoURL);
        etDriversRepoURL.setText(preferences.getString("drivers_repo_url", DEFAULT_DRIVERS_REPO));
        // -----------------------------

        view.findViewById(R.id.BTReInstallImagefs).setOnClickListener(v -> {
            ContentDialog.confirm(context, R.string.do_you_want_to_reinstall_imagefs, () -> ImageFsInstaller.installFromAssets((MainActivity) getActivity()));
        });

        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("dark_mode", cbDarkMode.isChecked());
            editor.putString("box64_preset", Box64PresetManager.getSpinnerSelectedId(view.findViewById(R.id.SBox64Preset)));
            editor.putString("fexcore_preset", FEXCorePresetManager.getSpinnerSelectedId(view.findViewById(R.id.SFEXCorePreset)));
            editor.putBoolean("use_dri3", cbUseDRI3.isChecked());
            editor.putBoolean("use_xr", cbUseXR.isChecked());
            editor.putBoolean("enable_wine_debug", cbEnableWineDebug.isChecked());
            editor.putBoolean("enable_box64_logs", cbEnableBox64Logs.isChecked());
            editor.putBoolean("cursor_lock", cbCursorLock.isChecked());
            editor.putBoolean("xinput_toggle", cbXinputToggle.isChecked());
            editor.putBoolean("enable_file_provider", cbEnableFileProvider.isChecked());
            editor.putBoolean("open_with_android_browser", cbOpenInBrowser.isChecked());
            editor.putBoolean("share_android_clipboard", cbShareClipboard.isChecked());
            editor.putFloat("cursor_speed", ((SeekBar)view.findViewById(R.id.SBCursorSpeed)).getProgress() / 100.0f);

            // SALVA AS URLS
            editor.putString("downloadable_contents_url", etDownloadableContentsURL.getText().toString());
            editor.putString("drivers_repo_url", etDriversRepoURL.getText().toString());

            if (!wineDebugChannels.isEmpty()) {
                editor.putString("wine_debug_channels", String.join(",", wineDebugChannels));
            } else if (preferences.contains("wine_debug_channels")) {
                editor.remove("wine_debug_channels");
            }

            editor.putBoolean("enable_big_picture_mode", cbEnableBigPictureMode.isChecked());
            saveCustomApiKeySettings(editor);

            if (editor.commit()) {
                NavigationView navigationView = getActivity().findViewById(R.id.NavigationView);
                navigationView.setCheckedItem(R.id.main_menu_containers);
                FragmentManager fragmentManager = getParentFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.FLFragmentContainer, new ContainersFragment()).commit();
            }
        });

        return view;
    }

    private void setupPathSelectors(View view) {
        TextView tvWinlatorPath = view.findViewById(R.id.TVWinlatorPath);
        String savedUriString = preferences.getString("winlator_path_uri", null);
        if (savedUriString == null) tvWinlatorPath.setText(DEFAULT_WINLATOR_PATH);
        else {
            String displayPath = FileUtils.getFilePathFromUri(getContext(), Uri.parse(savedUriString));
            tvWinlatorPath.setText(displayPath != null ? displayPath : savedUriString);
        }
        view.findViewById(R.id.BTChooseWinlatorPath).setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE_WINLATOR_PATH));

        TextView tvShortcutExportPath = view.findViewById(R.id.TVShortcutExportPath);
        savedUriString = preferences.getString("shortcuts_export_path_uri", null);
        if (savedUriString == null) tvShortcutExportPath.setText(DEFAULT_SHORTCUT_EXPORT_PATH);
        else {
            String displayPath = FileUtils.getFilePathFromUri(getContext(), Uri.parse(savedUriString));
            tvShortcutExportPath.setText(displayPath != null ? displayPath : savedUriString);
        }
        view.findViewById(R.id.BTChooseShortcutExportPath).setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE_SHORTCUT_EXPORT_PATH));
    }

    private void setupPresets(View view) {
        loadBox64PresetSpinners(view, view.findViewById(R.id.SBox64Preset));
        loadFEXCorePresetSpinners(view, view.findViewById(R.id.SFEXCorePreset));
    }

    private void setupSoundFont(View view) {
        Spinner sMIDISoundFont = view.findViewById(R.id.SMIDISoundFont);
        sMIDISoundFont.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        MidiManager.loadSFSpinnerWithoutDisabled(sMIDISoundFont);
        
        view.findViewById(R.id.BTInstallSF).setOnClickListener(v -> openFile(REQUEST_CODE_INSTALL_SOUNDFONT));
        view.findViewById(R.id.BTRemoveSF).setOnClickListener(v -> {
            if (sMIDISoundFont.getSelectedItemPosition() != 0) {
                ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_sound_font, () -> {
                    if (MidiManager.removeSF2File(getContext(), sMIDISoundFont.getSelectedItem().toString())) {
                        AppUtils.showToast(getContext(), R.string.sound_font_removed_success);
                        MidiManager.loadSFSpinnerWithoutDisabled(sMIDISoundFont);
                    } else AppUtils.showToast(getContext(), R.string.sound_font_removed_failed);
                });
            } else AppUtils.showToast(getContext(), R.string.cannot_remove_default_sound_font);
        });
    }

    private void setupCursorSpeed(View view) {
        final TextView tvCursorSpeed = view.findViewById(R.id.TVCursorSpeed);
        final SeekBar sbCursorSpeed = view.findViewById(R.id.SBCursorSpeed);
        sbCursorSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { tvCursorSpeed.setText(progress+"%"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sbCursorSpeed.setProgress((int)(preferences.getFloat("cursor_speed", 1.0f) * 100));
    }

    private void openFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        getActivity().startActivityFromFragment(this, intent, requestCode);
    }
    
    private void loadWineDebugChannels(final View view, final ArrayList<String> debugChannels) {
        final Context context = getContext();
        android.widget.LinearLayout container = view.findViewById(R.id.LLWineDebugChannels);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        
        View itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
        itemView.findViewById(R.id.TextView).setVisibility(View.GONE);
        itemView.findViewById(R.id.BTRemove).setVisibility(View.GONE);
        View addButton = itemView.findViewById(R.id.BTAdd);
        addButton.setVisibility(View.VISIBLE);
        addButton.setOnClickListener((v) -> {
            JSONArray jsonArray = null;
            try { jsonArray = new JSONArray(FileUtils.readString(context, "wine_debug_channels.json")); } catch (JSONException e) {}
            final String[] items = ArrayUtils.toStringArray(jsonArray);
            ContentDialog.showMultipleChoiceList(context, R.string.wine_debug_channel, items, (selectedPositions) -> {
                for (int selectedPosition : selectedPositions) if (!debugChannels.contains(items[selectedPosition])) debugChannels.add(items[selectedPosition]);
                loadWineDebugChannels(view, debugChannels);
            });
        });
        View resetButton = itemView.findViewById(R.id.BTReset);
        resetButton.setVisibility(View.VISIBLE);
        resetButton.setOnClickListener((v) -> {
            debugChannels.clear();
            debugChannels.addAll(Arrays.asList(DEFAULT_WINE_DEBUG_CHANNELS.split(",")));
            loadWineDebugChannels(view, debugChannels);
        });
        container.addView(itemView);

        for (int i = 0; i < debugChannels.size(); i++) {
            itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, container, false);
            TextView textView = itemView.findViewById(R.id.TextView);
            textView.setText(debugChannels.get(i));
            final int index = i;
            itemView.findViewById(R.id.BTRemove).setOnClickListener((v) -> {
                debugChannels.remove(index);
                loadWineDebugChannels(view, debugChannels);
            });
            container.addView(itemView);
        }
    }
    
    private void updateTheme(boolean isDarkMode) {
        if (isDarkMode) getActivity().setTheme(R.style.AppTheme_Dark);
        else getActivity().setTheme(R.style.AppTheme);
        getActivity().recreate();
    }
    
    private void applyDynamicStyles(View view, boolean isDarkMode) {
        Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
        sBox64Preset.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        Spinner sFEXCorePreset = view.findViewById(R.id.SFEXCorePreset);
        sFEXCorePreset.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
    }
    
    private void applyDynamicStylesRecursively(View view) {
        // Implemente conforme o original (pintando os textos de acordo com o tema)
        int color = isDarkMode ? android.graphics.Color.parseColor("#cccccc") : android.graphics.Color.parseColor("#bdbdbd");
        int bg = isDarkMode ? R.color.window_background_color_dark : R.color.window_background_color;
        
        int[] labelIds = {R.id.TVBox64, R.id.TVFEXCore, R.id.TVSound, R.id.TVTheme, R.id.TVShortcutSettings, 
                          R.id.TVBigPictureMode, R.id.TVCustomApiKey, R.id.TVXServer, R.id.TVLogs, 
                          R.id.TVExperimental, R.id.TVImageFs};
                          
        for (int id : labelIds) {
            TextView tv = view.findViewById(id);
            if (tv != null) {
                tv.setTextColor(color);
                tv.setBackgroundResource(bg);
            }
        }
    }
    
    private void initCustomApiKeySettings(View view) {
        cbEnableCustomApiKey = view.findViewById(R.id.CBEnableCustomApiKey);
        etCustomApiKey = view.findViewById(R.id.ETCustomApiKey);
        cbEnableCustomApiKey.setChecked(preferences.getBoolean("enable_custom_api_key", false));
        etCustomApiKey.setText(preferences.getString("custom_api_key", ""));
        etCustomApiKey.setVisibility(cbEnableCustomApiKey.isChecked() ? View.VISIBLE : View.GONE);
        cbEnableCustomApiKey.setOnCheckedChangeListener((b, c) -> etCustomApiKey.setVisibility(c ? View.VISIBLE : View.GONE));
        view.findViewById(R.id.BTHelpApiKey).setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.steamgriddb.com/profile/preferences/api"))));
    }
    
    private void saveCustomApiKeySettings(SharedPreferences.Editor editor) {
        editor.putBoolean("enable_custom_api_key", cbEnableCustomApiKey.isChecked());
        if (cbEnableCustomApiKey.isChecked()) editor.putString("custom_api_key", etCustomApiKey.getText().toString().trim());
        else editor.remove("custom_api_key");
    }
    
    private void loadBox64PresetSpinners(View view, Spinner s) { Box64PresetManager.loadSpinner("box64", s, preferences.getString("box64_preset", Box64Preset.COMPATIBILITY)); }
    private void loadFEXCorePresetSpinners(View view, Spinner s) { FEXCorePresetManager.loadSpinner(s, preferences.getString("fexcore_preset", FEXCorePreset.INTERMEDIATE)); }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            SharedPreferences.Editor editor = preferences.edit();
            try {
                 if (requestCode == REQUEST_CODE_WINLATOR_PATH || requestCode == REQUEST_CODE_SHORTCUT_EXPORT_PATH) {
                     requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                 }
                 if (requestCode == REQUEST_CODE_WINLATOR_PATH) {
                     editor.putString("winlator_path_uri", uri.toString());
                     editor.apply();
                     TextView tv = getView().findViewById(R.id.TVWinlatorPath);
                     String path = FileUtils.getFilePathFromUri(getContext(), uri);
                     tv.setText(path != null ? path : uri.toString());
                 } else if (requestCode == REQUEST_CODE_SHORTCUT_EXPORT_PATH) {
                     editor.putString("shortcuts_export_path_uri", uri.toString());
                     editor.apply();
                     TextView tv = getView().findViewById(R.id.TVShortcutExportPath);
                     String path = FileUtils.getFilePathFromUri(getContext(), uri);
                     tv.setText(path != null ? path : uri.toString());
                 } else if (requestCode == REQUEST_CODE_INSTALL_SOUNDFONT) {
                     if (installSoundFontCallback != null) installSoundFontCallback.call(uri);
                 } else if (requestCode == REQUEST_CODE_IMPORT_BOX64_PRESET) {
                     Box64PresetManager.importPreset("box64", getContext(), getActivity().getContentResolver().openInputStream(uri));
                     Box64PresetManager.loadSpinner("box64", getView().findViewById(R.id.SBox64Preset), preferences.getString("box64_preset", Box64Preset.COMPATIBILITY));
                 } else if (requestCode == REQUEST_CODE_IMPORT_FEXCORE_PRESET) {
                     FEXCorePresetManager.importPreset(getContext(), getActivity().getContentResolver().openInputStream(uri));
                     FEXCorePresetManager.loadSpinner(getView().findViewById(R.id.SFEXCorePreset), preferences.getString("fexcore_preset", FEXCorePreset.INTERMEDIATE));
                 }
            } catch (Exception e) {}
        }
    }
}