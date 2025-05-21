package com.winlator.cmod.contentdialog;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.GPUInformation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

public class GraphicsDriverConfigDialog extends ContentDialog {

    private static final String TAG = "GraphicsDriverConfigDialog"; // Tag for logging
    HashMap<String, Boolean> extensionsState = new HashMap<>();
    private Spinner sVersion;
    private Spinner sAvailableExtensions;
    private String selectedVersion;
    private String blacklistedExtensions = "";

    protected class ExtensionAdapter extends ArrayAdapter<String> {
        ArrayList<String> extensions;

        public ExtensionAdapter(Context context, List<String> list) {
            super(context, 0, list);
            this.extensions = new ArrayList<>(list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return initSpinnerElement(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return initDropDownView(position, convertView, parent);
        }

        private View initSpinnerElement(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = (View)new TextView(getContext());
            }
            ((TextView)convertView).setText(extensions.size() + " System Extensions");
            return convertView;
        }

        private View initDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.checkbox_spinner, parent, false);
            }
            CheckBox cb = convertView.findViewById(R.id.checkbox);
            cb.setText(extensions.get(position));
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(extensionsState.getOrDefault(extensions.get(position), true));
            cb.setOnCheckedChangeListener((buttonView, isChecked) ->  {
                extensionsState.put(extensions.get(position), isChecked);
            });
            return convertView;
        }
    }

    public interface OnGraphicsDriverVersionSelectedListener {
        void onGraphicsDriverVersionSelected(String version, String blacklistedExtensions);
    }
  
    public GraphicsDriverConfigDialog(View anchor, String initialVersion, String blExtensions, String graphicsDriver, OnGraphicsDriverVersionSelectedListener listener) {
        super(anchor.getContext(), R.layout.graphics_driver_config_dialog);
        initializeDialog(anchor, initialVersion, blExtensions, graphicsDriver, null, listener);
    }

    private void initializeDialog(View anchor, String initialVersion, String blExtensions, String graphicsDriver, @Nullable File rootDir, OnGraphicsDriverVersionSelectedListener listener) {
        setIcon(R.drawable.icon_settings);
        setTitle(anchor.getContext().getString(R.string.graphics_driver_configuration));

        sVersion = findViewById(R.id.SGraphicsDriverVersion);
        sAvailableExtensions = findViewById(R.id.SGraphicsDriverAvailableExtensions);

        // Update the selectedVersion whenever the user selects a different version
        sVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedVersion = sVersion.getSelectedItem().toString();
                Log.d(TAG, "User selected version: " + selectedVersion);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedVersion = sVersion.getSelectedItem().toString();
                Log.d(TAG, "User selected version: " + selectedVersion);
            }
        });

        // Ensure ContentsManager syncContents is called
        ContentsManager contentsManager = new ContentsManager(anchor.getContext());
        contentsManager.syncContents();
        
        // Populate the spinner with available versions from ContentsManager and pre-select the initial version
        populateGraphicsDriverVersions(anchor.getContext(), contentsManager, initialVersion, blExtensions, graphicsDriver);

        setOnConfirmCallback(() -> {
            anchor.setTag(selectedVersion);
            for (HashMap.Entry<String, Boolean> entry : extensionsState.entrySet()) {
                if(!entry.getKey().isEmpty() && !entry.getValue()) {
                    blacklistedExtensions += entry.getKey() + ",";
                }
            }

            if (!blacklistedExtensions.isEmpty())
                blacklistedExtensions = blacklistedExtensions.substring(0, blacklistedExtensions.length() - 1);

//            if (rootDir != null) {
//                // Apply the selected version to the container
//                Log.d(TAG, "Applying selected version to container: " + selectedVersion);
//                containerManager.extractGraphicsDriverFiles(selectedVersion, rootDir, null);
//            }

            // Pass the selected version back to the listener
            if (listener != null) {
                Log.d(TAG, "Blacklisted extensions " + blacklistedExtensions);
                listener.onGraphicsDriverVersionSelected(selectedVersion, blacklistedExtensions);
            }
        });
    }


    // Updated constructor to accept a container
//    public GraphicsDriverConfigDialog(View anchor, Container container, ContainerManager containerManager, @Nullable String initialVersion, OnGraphicsDriverVersionSelectedListener listener) {
//        super(anchor.getContext(), R.layout.graphics_driver_config_dialog);
//        this.containerManager = containerManager;
//
//        setIcon(R.drawable.icon_settings);
//        setTitle(anchor.getContext().getString(R.string.graphics_driver_configuration));
//
//        sVersion = findViewById(R.id.SGraphicsDriverVersion);
//
//        // Ensure ContentsManager syncContents is called
//        ContentsManager contentsManager = new ContentsManager(anchor.getContext());
//        contentsManager.syncContents();
//
//        // Populate the spinner with available versions from ContentsManager and pre-select the container's version
//        String containerVersion = container != null ? container.getGraphicsDriverVersion() : initialVersion;
//        populateGraphicsDriverVersions(anchor.getContext(), contentsManager, containerVersion);
//
//        // Update the selectedVersion whenever the user selects a different version
//        sVersion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                selectedVersion = sVersion.getSelectedItem().toString();
//                Log.d(TAG, "User selected version: " + selectedVersion); // Log the selected version
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do nothing
//            }
//        });
//
//        setOnConfirmCallback(() -> {
//            anchor.setTag(selectedVersion);
//
//            if (container != null) {
//                // Apply the selected version to the container
//                Log.d(TAG, "Applying selected version to container: " + selectedVersion);
//                container.setGraphicsDriverVersion(selectedVersion);
//
//                // Attempt to extract the graphics driver files
//                boolean extractionSuccess = containerManager.extractGraphicsDriverFiles(selectedVersion, container.getRootDir(), null);
//                Log.d(TAG, "Graphics driver extraction " + (extractionSuccess ? "succeeded" : "failed") + " for version: " + selectedVersion);
//
//                if (!extractionSuccess) {
//                    // Handle extraction failure (optional: you might want to notify the user here)
//                    Log.e(TAG, "Failed to extract graphics driver files for version: " + selectedVersion);
//                }
//            }
//
//            // Pass the selected version back to ContainerDetailFragment or other listeners
//            if (listener != null) {
//                listener.onGraphicsDriverVersionSelected(selectedVersion);
//            }
//        });
//    }

    private void populateGraphicsDriverVersions(Context context, ContentsManager contentsManager, @Nullable String initialVersion, @Nullable String blExtensions, String graphicsDriver) {
        List<String> turnipVersions = new ArrayList<>();
        List<String> wrapperVersions = new ArrayList<>();
        ArrayList<String> availableExtensions = new ArrayList<>();

        // Load the default versions from arrays.xml
        String[] turnipDefaultVersions = context.getResources().getStringArray(R.array.turnip_graphics_driver_version_entries);
        String[] wrapperDefaultVersions = context.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);

        turnipVersions.addAll(Arrays.asList(turnipDefaultVersions));
        wrapperVersions.addAll(Arrays.asList(wrapperDefaultVersions));

        // Add installed versions from ContentsManager
        List<ContentProfile> profiles = contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_TURNIP);
        if (profiles != null) {
            for (ContentProfile profile : profiles) {
                String profileName = ContentsManager.getEntryName(profile);
                if (profileName != null && !turnipVersions.contains(profileName)) {
                    turnipVersions.add(profileName);
                }
            }
        }
        
        // Add installed versions from AdrenotoolsManager
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(context);
        wrapperVersions.addAll(adrenotoolsManager.enumarateInstalledDrivers());

        availableExtensions = new ArrayList<>(Arrays.asList(GPUInformation.enumerateExtensions()));

        // Remove essential and wrapper disabled extensions
        String[] essentialExtensions = {"VK_EXT_hdr_metadata", "VK_GOOGLE_display_timing", "VK_KHR_shader_float_controls", "VK_KHR_shader_presentable_image", "VK_EXT_image_compression_control_swapchain"};
        for (String extension : essentialExtensions) {
            availableExtensions.remove(extension);
        }

        // Set the adapter and select the initial version
        ArrayAdapter<String> turnipAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, turnipVersions);
        ArrayAdapter<String> wrapperAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, wrapperVersions);
        ExtensionAdapter extensionsAdapter = new ExtensionAdapter(context, availableExtensions);

        String[] bl = blExtensions.split("\\,");

        for (String extension : bl) {
            if (!extension.isEmpty()) {
                Log.d("GraphicsDriverConfigDialog", "Getting initial blacklisted extension: " + extension);
                extensionsState.put(extension, false);
            }
        }
        
        if (graphicsDriver.contains("turnip"))
            sVersion.setAdapter(turnipAdapter);
        else
            sVersion.setAdapter(wrapperAdapter);

        sAvailableExtensions.setAdapter(extensionsAdapter);
        
        // We can start logging selected graphics driver and initial version
        Log.d(TAG, "Graphics driver: " + graphicsDriver);
        Log.d(TAG, "Initial version: " + initialVersion);

        // Use the custom selection logic
        setSpinnerSelectionWithFallback(sVersion, initialVersion, graphicsDriver);

        // We can log the spinner values now
        Log.d(TAG, "Spinner selected position: " + sVersion.getSelectedItemPosition());
        Log.d(TAG, "Spinner selected value: " + sVersion.getSelectedItem());
    }



    public String getSelectedVersion() {
        return selectedVersion;
    }

    private void setSpinnerSelectionWithFallback(Spinner spinner, String version, String graphicsDriver) {
        // First, attempt to find an exact match (case-insensitive)
        for (int i = 0; i < spinner.getCount(); i++) {
            String item = spinner.getItemAtPosition(i).toString();
            if (item.equalsIgnoreCase(version)) {
                spinner.setSelection(i);
                return;
            }
        }

        // If no exact match is found, try to match based on base version
        if (version != null && version.startsWith("Turnip")) {
            String baseVersion = extractBaseVersionFromTurnip(version);
            int lastTurnipIndex = -1;

            for (int i = 0; i < spinner.getCount(); i++) {
                String item = spinner.getItemAtPosition(i).toString();
                // Check if the item is a Turnip version and matches the base version
                if (item.startsWith("Turnip") && item.contains(baseVersion)) {
                    spinner.setSelection(i);
                    return;
                }
                // Save the index of the last Turnip version with the matching base version
                if (item.equalsIgnoreCase(baseVersion)) {
                    lastTurnipIndex = i;
                }
            }

            // If no Turnip version matches, fall back to the last index of a base version match
            if (lastTurnipIndex != -1) {
                spinner.setSelection(lastTurnipIndex);
                return;
            }
        }

        if (graphicsDriver.contains("turnip")) {
            AppUtils.setSpinnerSelectionFromValue(spinner, DefaultVersion.TURNIP);
        }
        else
            AppUtils.setSpinnerSelectionFromValue(spinner, DefaultVersion.WRAPPER);
    }

    // Helper method to extract the base version from the Turnip string
    private String extractBaseVersionFromTurnip(String version) {
        // Assumes the format "Turnip-X.Y.Z-anything" and extracts "X.Y.Z"
        String[] parts = version.split("-");
        if (parts.length > 1) {
            return parts[1]; // This should give us "X.Y.Z"
        }
        return version; // Fallback to the original version string
    }


}
