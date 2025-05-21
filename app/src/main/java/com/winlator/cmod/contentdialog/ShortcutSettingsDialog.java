package com.winlator.cmod.contentdialog;



import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.ContainerDetailFragment;
import com.winlator.cmod.R;
import com.winlator.cmod.ShortcutsFragment;
import com.winlator.cmod.box86_64.Box86_64PresetManager;
import com.winlator.cmod.box86_64.rc.RCManager;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.fexcore.FEXCoreManager;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.midi.MidiManager;
import com.winlator.cmod.widget.EnvVarsView;
import com.winlator.cmod.winhandler.WinHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShortcutSettingsDialog extends ContentDialog {
    private final ShortcutsFragment fragment;
    private final Shortcut shortcut;
    private InputControlsManager inputControlsManager;
//    private ContentsManager contentsManager;
//
//    private boolean overrideGraphicsDriver = false;

    private TextView tvGraphicsDriverVersion; 

    private String box64Version;

    private String selectedCPUList;
    private String selectedCPUListWoW64;


    public ShortcutSettingsDialog(ShortcutsFragment fragment, Shortcut shortcut) {
        super(fragment.getContext(), R.layout.shortcut_settings_dialog);
        this.fragment = fragment;
        this.shortcut = shortcut;
        setTitle(shortcut.name);
        setIcon(R.drawable.icon_settings);

        // Initialize the ContentsManager
        ContainerManager containerManager = shortcut.container.getManager();

//        if (containerManager != null) {
//            this.contentsManager = new ContentsManager(containerManager.getContext());
//            this.contentsManager.syncTurnipContents();
//        } else {
//            Toast.makeText(fragment.getContext(), "Failed to initialize container manager. Please try again.", Toast.LENGTH_SHORT).show();
//            return;
//        }

        createContentView();
    }

    private void createContentView() {
        final Context context = fragment.getContext();
        inputControlsManager = new InputControlsManager(context);
        LinearLayout llContent = findViewById(R.id.LLContent);
        llContent.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        applyDynamicStyles(findViewById(R.id.LLContent), isDarkMode);

        // Initialize the turnip version TextView
        tvGraphicsDriverVersion = findViewById(R.id.TVGraphicsDriverVersion);

        // Get the shared preferences and check the legacy mode status
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isLegacyModeEnabled = preferences.getBoolean("legacy_mode_enabled", false);

        final EditText etName = findViewById(R.id.ETName);
        etName.setText(shortcut.name);

        final EditText etExecArgs = findViewById(R.id.ETExecArgs);
        etExecArgs.setText(shortcut.getExtra("execArgs"));

        ContainerDetailFragment containerDetailFragment = new ContainerDetailFragment(shortcut.container.id);
//        containerDetailFragment.loadScreenSizeSpinner(getContentView(), shortcut.getExtra("screenSize", shortcut.container.getScreenSize()));

        loadScreenSizeSpinner(getContentView(), shortcut.getExtra("screenSize", shortcut.container.getScreenSize()), isDarkMode);


        final Spinner sGraphicsDriver = findViewById(R.id.SGraphicsDriver);
        
        final Spinner sDXWrapper = findViewById(R.id.SDXWrapper);

        final Spinner sBox64Version = findViewById(R.id.SBox64Version);
        
        ContentsManager contentsManager = new ContentsManager(context);
        
        contentsManager.syncContents();
        
        loadBox64VersionSpinner(context, contentsManager, sBox64Version);

        // Add this part to set the initial spinner selection based on the shortcut
        String currentBox64Version = shortcut.getExtra("box64Version", null);
        if (currentBox64Version != null) {
            AppUtils.setSpinnerSelectionFromValue(sBox64Version, currentBox64Version);
        } else {
            // Default selection or use a preferred default version
            sBox64Version.setSelection(0);  // Assuming the first item is the default
        }

        // Set OnItemSelectedListener for the Box64 version spinner
        sBox64Version.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVersion = parent.getItemAtPosition(position).toString();
                box64Version = selectedVersion;  // Update the class-level variable
                // Update the shortcut extra immediately, or wait until saveData() is called
                shortcut.putExtra("box64Version", selectedVersion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // This method must be implemented, even if it's empty.
                // Optional: You can handle the case where no item is selected, if needed.
            }
        });

        final View vGraphicsDriverConfig = findViewById(R.id.BTGraphicsDriverConfig);
        
        final View vDXWrapperConfig = findViewById(R.id.BTDXWrapperConfig);
        vDXWrapperConfig.setTag(shortcut.getExtra("dxwrapperConfig", shortcut.container.getDXWrapperConfig()));

        ContainerDetailFragment.setupDXWrapperSpinner(sDXWrapper, vDXWrapperConfig);
        loadGraphicsDriverSpinner(sGraphicsDriver, sDXWrapper, vGraphicsDriverConfig, shortcut.getExtra("graphicsDriver", shortcut.container.getGraphicsDriver()),
            shortcut.getExtra("dxwrapper", shortcut.container.getDXWrapper()));

        findViewById(R.id.BTHelpDXWrapper).setOnClickListener((v) -> AppUtils.showHelpBox(context, v, R.string.dxwrapper_help_content));

        final Spinner sAudioDriver = findViewById(R.id.SAudioDriver);
        AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, shortcut.getExtra("audioDriver", shortcut.container.getAudioDriver()));
        final Spinner sEmulator = findViewById(R.id.SEmulator);
        AppUtils.setSpinnerSelectionFromIdentifier(sEmulator, shortcut.getExtra("emulator", shortcut.container.getEmulator()));
        final Spinner sMIDISoundFont = findViewById(R.id.SMIDISoundFont);
        MidiManager.loadSFSpinner(sMIDISoundFont);
        AppUtils.setSpinnerSelectionFromValue(sMIDISoundFont, shortcut.getExtra("midiSoundFont", shortcut.container.getMIDISoundFont()));

        final CheckBox cbUseSecondaryExec = findViewById(R.id.CBUseSecondaryExec);
        final LinearLayout llSecondaryExecOptions = findViewById(R.id.LLSecondaryExecOptions);
        final EditText etSecondaryExec = findViewById(R.id.ETSecondaryExec);
        final EditText etExecDelay = findViewById(R.id.ETExecDelay);

        boolean useSecondaryExec = !shortcut.getExtra("secondaryExec", "").isEmpty();
        cbUseSecondaryExec.setChecked(useSecondaryExec);
        llSecondaryExecOptions.setVisibility(useSecondaryExec ? View.VISIBLE : View.GONE);
        etSecondaryExec.setText(shortcut.getExtra("secondaryExec"));
        etExecDelay.setText(shortcut.getExtra("execDelay", "0"));

        cbUseSecondaryExec.setOnCheckedChangeListener((buttonView, isChecked) -> {
            llSecondaryExecOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Initialize the TextView for the legacy mode message
        TextView tvLegacyInputMessage = findViewById(R.id.TVLegacyInputMessage);

        final CheckBox cbFullscreenStretched =  findViewById(R.id.CBFullscreenStretched);
        boolean fullscreenStretched = shortcut.getExtra("fullscreenStretched", "0").equals("1");
        cbFullscreenStretched.setChecked(fullscreenStretched);


        final Runnable showInputWarning = () -> ContentDialog.alert(context, R.string.enable_xinput_and_dinput_same_time, null);
        final CheckBox cbEnableXInput = findViewById(R.id.CBEnableXInput);
        final CheckBox cbEnableDInput = findViewById(R.id.CBEnableDInput);
        final View llDInputType = findViewById(R.id.LLDinputMapperType);
        final View btHelpXInput = findViewById(R.id.BTXInputHelp);
        final View btHelpDInput = findViewById(R.id.BTDInputHelp);
        Spinner SDInputType = findViewById(R.id.SDInputType);
        int inputType = Integer.parseInt(shortcut.getExtra("inputType", String.valueOf(shortcut.container.getInputType())));

        if (isLegacyModeEnabled) {
            // Display legacy mode message and hide input controls
            tvLegacyInputMessage.setText("You are in 7.1.2 legacy input mode. Advanced input settings are not available.");
            tvLegacyInputMessage.setVisibility(View.VISIBLE);
            // In legacy mode, hide all input-related UI elements
            cbEnableXInput.setVisibility(View.GONE);
            cbEnableDInput.setVisibility(View.GONE);
            llDInputType.setVisibility(View.GONE);
            btHelpXInput.setVisibility(View.GONE);
            btHelpDInput.setVisibility(View.GONE);
            SDInputType.setVisibility(View.GONE);
        } else {
            cbEnableXInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_XINPUT) == WinHandler.FLAG_INPUT_TYPE_XINPUT);
            cbEnableDInput.setChecked((inputType & WinHandler.FLAG_INPUT_TYPE_DINPUT) == WinHandler.FLAG_INPUT_TYPE_DINPUT);
            cbEnableDInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
                llDInputType.setVisibility(isChecked?View.VISIBLE:View.GONE);
                if (isChecked && cbEnableXInput.isChecked())
                    showInputWarning.run();
            });
            cbEnableXInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && cbEnableDInput.isChecked())
                    showInputWarning.run();
            });
            btHelpXInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_xinput));
            btHelpDInput.setOnClickListener(v -> AppUtils.showHelpBox(context, v, R.string.help_dinput));
            SDInputType.setSelection(((inputType & WinHandler.FLAG_DINPUT_MAPPER_STANDARD) == WinHandler.FLAG_DINPUT_MAPPER_STANDARD) ? 0 : 1);
            llDInputType.setVisibility(cbEnableDInput.isChecked()?View.VISIBLE:View.GONE);

            // Always show input-related UI elements when not in legacy mode
            cbEnableXInput.setVisibility(View.VISIBLE);
            cbEnableDInput.setVisibility(View.VISIBLE);
            llDInputType.setVisibility(View.VISIBLE);
            btHelpXInput.setVisibility(View.VISIBLE);
            btHelpDInput.setVisibility(View.VISIBLE);
            SDInputType.setVisibility(View.VISIBLE);


        }

        final CheckBox cbForceFullscreen = findViewById(R.id.CBForceFullscreen);
        cbForceFullscreen.setChecked(shortcut.getExtra("forceFullscreen", "0").equals("1"));

        final Spinner sBox86Preset = findViewById(R.id.SBox86Preset);
        Box86_64PresetManager.loadSpinner("box86", sBox86Preset, shortcut.getExtra("box86Preset", shortcut.container.getBox86Preset()));

        final Spinner sBox64Preset = findViewById(R.id.SBox64Preset);
        Box86_64PresetManager.loadSpinner("box64", sBox64Preset, shortcut.getExtra("box64Preset", shortcut.container.getBox64Preset()));
        
        final Spinner sFEXCoreTSOPreset = findViewById(R.id.SFEXCoreTSOPreset);
        final Spinner sFEXCoreMultiBlock = findViewById(R.id.SFEXCoreMultiblock);
        final Spinner sFEXCoreX87ReducedPrecision = findViewById(R.id.SFEXCoreX87ReducedPrecision);
        
        FEXCoreManager.loadFEXCoreSpinners(context, shortcut, sFEXCoreTSOPreset, sFEXCoreMultiBlock, sFEXCoreX87ReducedPrecision);

        final Spinner sRCFile = findViewById(R.id.SRCFile);
        final int[] rcfileIds = {0};
        RCManager manager = new RCManager(context);
        String rcfileId = shortcut.getExtra("rcfileId", String.valueOf(shortcut.container.getRCFileId()));
        RCManager.loadRCFileSpinner(manager, Integer.parseInt(rcfileId), sRCFile, id -> {
            rcfileIds[0] = id;
        });

//        // Get CPU lists from the container
//        selectedCPUList = shortcut.container.getCPUList(true);
//        selectedCPUListWoW64 = shortcut.container.getCPUListWoW64(true);

        final Spinner sControlsProfile = findViewById(R.id.SControlsProfile);
        loadControlsProfileSpinner(sControlsProfile, shortcut.getExtra("controlsProfile", "0"));

        final CheckBox cbDisabledXInput = findViewById(R.id.CBDisabledXInput);
        // Set the initial value based on the shortcut extras
        boolean isXInputDisabled = shortcut.getExtra("disableXinput", "0").equals("1");
        cbDisabledXInput.setChecked(isXInputDisabled);


        //        ContainerDetailFragment.createWinComponentsTab(getContentView(), shortcut.getExtra("wincomponents", shortcut.container.getWinComponents()));
        ContainerDetailFragment.createWinComponentsTabFromShortcut(this, getContentView(),
                shortcut.getExtra("wincomponents", shortcut.container.getWinComponents()), isDarkMode);

        final EnvVarsView envVarsView = createEnvVarsTab();

        AppUtils.setupTabLayout(getContentView(), R.id.TabLayout, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabAdvanced);

        TabLayout tabLayout = findViewById(R.id.TabLayout);

        if (isDarkMode) {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background_dark);
        } else {
            tabLayout.setBackgroundResource(R.drawable.tab_layout_background);
        }

        findViewById(R.id.BTExtraArgsMenu).setOnClickListener((v) -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.inflate(R.menu.extra_args_popup_menu);
            popupMenu.setOnMenuItemClickListener((menuItem) -> {
                String value = String.valueOf(menuItem.getTitle());
                String execArgs = etExecArgs.getText().toString();
                if (!execArgs.contains(value)) etExecArgs.setText(!execArgs.isEmpty() ? execArgs + " " + value : value);
                return true;
            });
            popupMenu.show();
        });

        String selectedDriver = sGraphicsDriver.getSelectedItem().toString();
        List<String> sGraphicsItemsList = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.graphics_driver_entries)));
        sGraphicsDriver.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, sGraphicsItemsList));
        AppUtils.setSpinnerSelectionFromValue(sGraphicsDriver, selectedDriver);

        setOnConfirmCallback(() -> {
            String name = etName.getText().toString().trim();
            boolean nameChanged = !shortcut.name.equals(name) && !name.isEmpty();

            // First, handle renaming if the name has changed
            if (nameChanged) {
                renameShortcut(name);
            }


            // Determine if renaming is needed
            boolean renamingSuccess = !nameChanged || new File(shortcut.file.getParent(), name + ".desktop").exists();

            if (renamingSuccess) {
                String graphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());
                String graphicsDriverConfig = vGraphicsDriverConfig.getTag().toString();
                String dxwrapper = StringUtils.parseIdentifier(sDXWrapper.getSelectedItem());
//                dxwrapper = dxwrapper == null || dxwrapper.isEmpty() ? "default_dxwrapper" : dxwrapper;
                String dxwrapperConfig = vDXWrapperConfig.getTag().toString();
                String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
                String emulator = StringUtils.parseIdentifier(sEmulator.getSelectedItem());
                String midiSoundFont = sMIDISoundFont.getSelectedItemPosition() == 0 ? "" : sMIDISoundFont.getSelectedItem().toString();
                String screenSize = containerDetailFragment.getScreenSize(getContentView());

                int finalInputType = 0;
                finalInputType |= cbEnableXInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_XINPUT : 0;
                finalInputType |= cbEnableDInput.isChecked() ? WinHandler.FLAG_INPUT_TYPE_DINPUT : 0;
                finalInputType |= SDInputType.getSelectedItemPosition() == 0 ?  WinHandler.FLAG_DINPUT_MAPPER_STANDARD : WinHandler.FLAG_DINPUT_MAPPER_XINPUT;


                shortcut.putExtra("inputType", String.valueOf(finalInputType));

                boolean disabledXInput = cbDisabledXInput.isChecked();
                shortcut.putExtra("disableXinput", disabledXInput ? "1" : null);

                String execArgs = etExecArgs.getText().toString();
                shortcut.putExtra("execArgs", !execArgs.isEmpty() ? execArgs : null);
                shortcut.putExtra("screenSize", !screenSize.equals(shortcut.container.getScreenSize()) ? screenSize : null);
                shortcut.putExtra("graphicsDriver", !graphicsDriver.equals(shortcut.container.getGraphicsDriver()) ? graphicsDriver : null);
                shortcut.putExtra("wrapperGraphicsDriverVersion", graphicsDriverConfig);
                shortcut.putExtra("dxwrapper", !dxwrapper.equals(shortcut.container.getDXWrapper()) ? dxwrapper : null);
                shortcut.putExtra("dxwrapperConfig", !dxwrapperConfig.equals(shortcut.container.getDXWrapperConfig()) ? dxwrapperConfig : null);
                shortcut.putExtra("audioDriver", !audioDriver.equals(shortcut.container.getAudioDriver()) ? audioDriver : null);
                shortcut.putExtra("emulator", !emulator.equals(shortcut.container.getEmulator()) ? emulator : null);
                shortcut.putExtra("midiSoundFont", !midiSoundFont.equals(shortcut.container.getMIDISoundFont()) ? midiSoundFont : null);
                shortcut.putExtra("forceFullscreen", cbForceFullscreen.isChecked() ? "1" : null);

                if (cbUseSecondaryExec.isChecked()) {
                    String secondaryExec = etSecondaryExec.getText().toString().trim();
                    String execDelay = etExecDelay.getText().toString().trim();
                    shortcut.putExtra("secondaryExec", !secondaryExec.isEmpty() ? secondaryExec : null);
                    shortcut.putExtra("execDelay", !execDelay.isEmpty() ? execDelay : null);
                } else {
                    shortcut.putExtra("secondaryExec", null);
                    shortcut.putExtra("execDelay", null);
                }

                shortcut.putExtra("fullscreenStretched", cbFullscreenStretched.isChecked() ? "1" : null);

                String wincomponents = containerDetailFragment.getWinComponents(getContentView());
                shortcut.putExtra("wincomponents", !wincomponents.equals(shortcut.container.getWinComponents()) ? wincomponents : null);

                String envVars = envVarsView.getEnvVars();
                shortcut.putExtra("envVars", !envVars.isEmpty() ? envVars : null);

//                if (box64Version != null) {
//                    shortcut.putExtra("box64Version", box64Version);
//                }

                String box86Preset = Box86_64PresetManager.getSpinnerSelectedId(sBox86Preset);
                String box64Preset = Box86_64PresetManager.getSpinnerSelectedId(sBox64Preset);
                shortcut.putExtra("box86Preset", !box86Preset.equals(shortcut.container.getBox86Preset()) ? box86Preset : null);
                shortcut.putExtra("box64Preset", !box64Preset.equals(shortcut.container.getBox64Preset()) ? box64Preset : null);

                shortcut.putExtra("rcfileId", rcfileIds[0] != shortcut.container.getRCFileId() ? Integer.toString(rcfileIds[0]) : null);


                ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
                int controlsProfile = sControlsProfile.getSelectedItemPosition() > 0 ? profiles.get(sControlsProfile.getSelectedItemPosition() - 1).id : 0;
                shortcut.putExtra("controlsProfile", controlsProfile > 0 ? String.valueOf(controlsProfile) : null);

                // Save all changes to the shortcut
                shortcut.saveData();
//                shortcut.putExtra("overrideGraphicsDriver", overrideGraphicsDriver ? "1" : null);
                FEXCoreManager.saveFEXCoreSpinners(shortcut.container, sFEXCoreTSOPreset, sFEXCoreMultiBlock, sFEXCoreX87ReducedPrecision); 
            }
        });
    }
    
    private void updateGraphicsDriverVersionText(String version) {
        tvGraphicsDriverVersion.setText(version);
    }

    // Utility method to apply styles to dynamically added TextViews based on their content
    private void applyFieldSetLabelStylesDynamically(ViewGroup rootView, boolean isDarkMode) {
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            if (child instanceof ViewGroup) {
                applyFieldSetLabelStylesDynamically((ViewGroup) child, isDarkMode); // Recursive call for nested ViewGroups
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                // Apply the style based on the content of the TextView
                if (isFieldSetLabel(textView.getText().toString())) {
                    applyFieldSetLabelStyle(textView, isDarkMode);
                }
            }
        }
    }

    // Method to check if the text content matches any fieldset label
    private boolean isFieldSetLabel(String text) {
        return text.equalsIgnoreCase("DirectX") ||
                text.equalsIgnoreCase("General") ||
                text.equalsIgnoreCase("Box86/Box64") ||
                text.equalsIgnoreCase("Input Controls") ||
                text.equalsIgnoreCase("Game Controller") ||
                text.equalsIgnoreCase("System");
    }

    public void onWinComponentsViewsAdded(boolean isDarkMode) {
        // Apply styles to all dynamically added TextViews
        ViewGroup llContent = findViewById(R.id.LLContent);
        applyFieldSetLabelStylesDynamically(llContent, isDarkMode);
    }


    public static void loadScreenSizeSpinner(View view, String selectedValue, boolean isDarkMode) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);

        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);

        applyDarkThemeToEditText(view.findViewById(R.id.ETScreenWidth), isDarkMode);
        applyDarkThemeToEditText(view.findViewById(R.id.ETScreenHeight), isDarkMode);


        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String value = sScreenSize.getItemAtPosition(position).toString();
                llCustomScreenSize.setVisibility(value.equalsIgnoreCase("custom") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            AppUtils.setSpinnerSelectionFromValue(sScreenSize, "custom");
            String[] screenSize = selectedValue.split("x");
            ((EditText)view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText)view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    private void applyDynamicStyles(View view, boolean isDarkMode) {

        // Update edit text
        EditText etName = view.findViewById(R.id.ETName);
        applyDarkThemeToEditText(etName, isDarkMode);

        // Update Spinners
        Spinner sGraphicsDriver = view.findViewById(R.id.SGraphicsDriver);
        Spinner sDXWrapper = view.findViewById(R.id.SDXWrapper);
        Spinner sAudioDriver = view.findViewById(R.id.SAudioDriver);
        Spinner sBox86Preset = view.findViewById(R.id.SBox86Preset);
        Spinner sBox64Preset = view.findViewById(R.id.SBox64Preset);
        Spinner sControlsProfile = view.findViewById(R.id.SControlsProfile);
//        Spinner sDInputMapperType = view.findViewById(R.id.SDInputMapperType);
        Spinner sRCFile = view.findViewById(R.id.SRCFile);
        Spinner sDInputType = view.findViewById(R.id.SDInputType);
        Spinner sMIDISoundFont = view.findViewById(R.id.SMIDISoundFont);
        Spinner sBox64Version = view.findViewById(R.id.SBox64Version);
        Spinner sFEXCoreTSOPreset = findViewById(R.id.SFEXCoreTSOPreset);
        Spinner sFEXCoreMultiBlock = findViewById(R.id.SFEXCoreMultiblock);
        Spinner sFEXCoreX87ReducedPrecision = findViewById(R.id.SFEXCoreX87ReducedPrecision);
        

        // Set dark or light mode background for spinners
        sGraphicsDriver.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sDXWrapper.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sAudioDriver.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sBox86Preset.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sBox64Preset.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sControlsProfile.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
//        sDInputMapperType.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sRCFile.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sDInputType.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sMIDISoundFont.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sBox64Version.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sFEXCoreTSOPreset.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sFEXCoreMultiBlock.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);
        sFEXCoreX87ReducedPrecision.setPopupBackgroundResource(isDarkMode ? R.drawable.content_dialog_background_dark : R.drawable.content_dialog_background);

//        EditText etLC_ALL = view.findViewById(R.id.ETlcall);
        EditText etExecArgs = view.findViewById(R.id.ETExecArgs);

//        applyDarkThemeToEditText(etLC_ALL, isDarkMode);
        applyDarkThemeToEditText(etExecArgs, isDarkMode);

    }

    private void applyFieldSetLabelStyle(TextView textView, boolean isDarkMode) {
        if (isDarkMode) {
            // Apply dark mode-specific attributes
            textView.setTextColor(Color.parseColor("#cccccc")); // Set text color to #cccccc
            textView.setBackgroundColor(Color.parseColor("#424242")); // Set dark background color
        } else {
            // Apply light mode-specific attributes
            textView.setTextColor(Color.parseColor("#bdbdbd")); // Set text color to #bdbdbd
            textView.setBackgroundResource(R.color.window_background_color); // Set light background color
        }
    }

    private static void applyDarkThemeToEditText(EditText editText, boolean isDarkMode) {
        if (isDarkMode) {
            editText.setTextColor(Color.WHITE);
            editText.setHintTextColor(Color.GRAY);
            editText.setBackgroundResource(R.drawable.edit_text_dark);
        } else {
            editText.setTextColor(Color.BLACK);
            editText.setHintTextColor(Color.GRAY);
            editText.setBackgroundResource(R.drawable.edit_text);
        }
    }

    private void showGraphicsDriverConfigDialog(View anchor, String graphicsDriver) {
        // Use the shortcut's graphics driver version to initialize the dialog
        String graphicsDriverVersion;
        graphicsDriverVersion = shortcut.getExtra("wrapperGraphicsDriverVersion", shortcut.container.getWrapperGraphicsDriverVersion());

        String blExtensions = shortcut.getExtra("blacklistedextensions", shortcut.container.getBlacklistedExtensions());

        new GraphicsDriverConfigDialog(anchor, graphicsDriverVersion, blExtensions, graphicsDriver, (version, blackListedExtensions) -> {
            // Update the shortcut's graphics driver version with the selected version from the dialog.
            shortcut.putExtra("wrapperGraphicsDriverVersion", version);
            shortcut.putExtra("blacklistedextensions", blackListedExtensions);
            updateGraphicsDriverVersionText(version);
        }).show();
    }


    private void updateExtra(String extraName, String containerValue, String newValue) {
        String extraValue = shortcut.getExtra(extraName);
        if (extraValue.isEmpty() && containerValue.equals(newValue))
            return;
        shortcut.putExtra(extraName, newValue);
    }

    private void renameShortcut(String newName) {
        File parent = shortcut.file.getParentFile();
        File oldDesktopFile = shortcut.file; // Reference to the old file
        File newDesktopFile = new File(parent, newName + ".desktop");

        // Rename the desktop file if the new one doesn't exist
        if (!newDesktopFile.isFile() && oldDesktopFile.renameTo(newDesktopFile)) {
            // Successfully renamed, update the shortcut's file reference
            updateShortcutFileReference(newDesktopFile); // New helper method

            // As a precaution, delete any remaining old file
            deleteOldFileIfExists(oldDesktopFile);
        }

        // Rename link file if applicable
        File linkFile = new File(parent, shortcut.name + ".lnk");
        if (linkFile.isFile()) {
            File newLinkFile = new File(parent, newName + ".lnk");
            if (!newLinkFile.isFile()) linkFile.renameTo(newLinkFile);
        }

        fragment.loadShortcutsList();
        fragment.updateShortcutOnScreen(newName, newName, shortcut.container.id, newDesktopFile.getAbsolutePath(),
                Icon.createWithBitmap(shortcut.icon), shortcut.getExtra("uuid"));
    }

    // Method to ensure no old file remains
    private void deleteOldFileIfExists(File oldFile) {
        if (oldFile.exists()) {
            if (!oldFile.delete()) {
                Log.e("ShortcutSettingsDialog", "Failed to delete old file: " + oldFile.getPath());
            }
        }
    }

    // Update the shortcut's file reference to ensure saveData() writes to the correct file
    private void updateShortcutFileReference(File newFile) {
        try {
            Field fileField = Shortcut.class.getDeclaredField("file");
            fileField.setAccessible(true);
            fileField.set(shortcut, newFile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("ShortcutSettingsDialog", "Error updating shortcut file reference", e);
        }
    }


    private EnvVarsView createEnvVarsTab() {
        final View view = getContentView();
        final Context context = view.getContext();

        // Retrieve the existing EnvVarsView
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);

        // Update the dark mode setting of the existing instance
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        envVarsView.setDarkMode(isDarkMode);

        // Set the environment variables in the existing EnvVarsView
        envVarsView.setEnvVars(new EnvVars(shortcut.getExtra("envVars")));

        // Set the click listener for adding new environment variables
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) ->
                new AddEnvVarDialog(context, envVarsView).show()
        );

        return envVarsView;
    }

    private void loadControlsProfileSpinner(Spinner spinner, String selectedValue) {
        final Context context = fragment.getContext();
        final ArrayList<ControlsProfile> profiles = inputControlsManager.getProfiles(true);
        ArrayList<String> values = new ArrayList<>();
        values.add(context.getString(R.string.none));

        int selectedPosition = 0;
        int selectedId = Integer.parseInt(selectedValue);
        for (int i = 0; i < profiles.size(); i++) {
            ControlsProfile profile = profiles.get(i);
            if (profile.id == selectedId) selectedPosition = i + 1;
            values.add(profile.getName());
        }

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(selectedPosition, false);
    }

    private void showInputWarning() {
        final Context context = fragment.getContext();
        ContentDialog.alert(context, R.string.enable_xinput_and_dinput_same_time, null);
    }

    public static void loadBox64VersionSpinner(Context context, ContentsManager manager, Spinner spinner) {
        String[] originalItems = context.getResources().getStringArray(R.array.box64_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));
        for (ContentProfile profile : manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_BOX64)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
    }
    
    public void loadGraphicsDriverSpinner(final Spinner sGraphicsDriver, final Spinner sDXWrapper, final View vGraphicsDriverConfig, String selectedGraphicsDriver, String selectedDXWrapper) {
        final Context context = sGraphicsDriver.getContext();
        
        ContainerDetailFragment.updateGraphicsDriverSpinner(context, sGraphicsDriver);
        
        final String[] dxwrapperEntries = context.getResources().getStringArray(R.array.dxwrapper_entries);
        
        Runnable update = () -> {
            String graphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());

            ArrayList<String> items = new ArrayList<>();
            for (String value : dxwrapperEntries) {
                    items.add(value);
            }
            sDXWrapper.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items.toArray(new String[0])));
            AppUtils.setSpinnerSelectionFromIdentifier(sDXWrapper, selectedDXWrapper);
        };

        sGraphicsDriver.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               String driver = StringUtils.parseIdentifier(parent.getItemAtPosition(position));
                Log.d("Selected driver in shortcut", driver);     
                switch(driver) {
                    case "wrapper":
                        vGraphicsDriverConfig.setVisibility(View.VISIBLE);
                        vGraphicsDriverConfig.setTag(shortcut.getExtra("wrapperGraphicsDriverVersion", shortcut.container.getWrapperGraphicsDriverVersion()));
                        updateGraphicsDriverVersionText(shortcut.getExtra("wrapperGraphicsDriverVersion", shortcut.container.getWrapperGraphicsDriverVersion()));
                        break;
                }
                vGraphicsDriverConfig.setOnClickListener((v) -> {
                        showGraphicsDriverConfigDialog(vGraphicsDriverConfig, StringUtils.parseIdentifier(driver));
                });
                update.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                String driver = StringUtils.parseIdentifier(parent.getSelectedItem());
                Log.d("Selected driver in shortcut", driver);
            }
        });

        AppUtils.setSpinnerSelectionFromIdentifier(sGraphicsDriver, selectedGraphicsDriver);
        update.run();
    }
}
