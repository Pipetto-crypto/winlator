package com.winlator.cmod.container;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.MSLink;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.xenvironment.ImageFs;

import java.util.Arrays;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;

public class ContainerManager {
    private final ArrayList<Container> containers = new ArrayList<>();
    private int maxContainerId = 0;
    private final File homeDir;
    private final Context context;

    private boolean isInitialized = false;

    public ContainerManager(Context context) {
        this.context = context;
        File rootDir = ImageFs.find(context).getRootDir();
        homeDir = new File(rootDir, "home");
        loadContainers();
        isInitialized = true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public ArrayList<Container> getContainers() {
        return containers;
    }

    private void loadContainers() {
        containers.clear();
        maxContainerId = 0;

        try {
            File[] files = homeDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (file.getName().startsWith(ImageFs.USER + "-")) {
                            Container container = new Container(
                                    Integer.parseInt(file.getName().replace(ImageFs.USER + "-", "")), this
                            );

                            container.setRootDir(new File(homeDir, ImageFs.USER + "-" + container.id));
                            JSONObject data = new JSONObject(FileUtils.readString(container.getConfigFile()));
                            container.loadData(data);
                            containers.add(container);
                            maxContainerId = Math.max(maxContainerId, container.id);
                        }
                    }
                }
            }
        } catch (JSONException | NullPointerException e) {
            Log.e("ContainerManager", "Error loading containers", e);
        }
    }


    public Context getContext() {
        return context;
    }


    public void activateContainer(Container container) {
        container.setRootDir(new File(homeDir, ImageFs.USER+"-"+container.id));
        File file = new File(homeDir, ImageFs.USER);
        file.delete();
        FileUtils.symlink("./"+ImageFs.USER+"-"+container.id, file.getPath());
    }

    public void createContainerAsync(final JSONObject data, ContentsManager contentsManager, Callback<Container> callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            final Container container = createContainer(data, contentsManager);
            handler.post(() -> callback.call(container));
        });
    }

    public void duplicateContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            duplicateContainer(container);
            handler.post(callback);
        });
    }

    public void removeContainerAsync(Container container, Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(() -> {
            removeContainer(container);
            handler.post(callback);
        });
    }

    private Container createContainer(JSONObject data, ContentsManager contentsManager) {
        try {
            int id = maxContainerId + 1;
            data.put("id", id);

            File containerDir = new File(homeDir, ImageFs.USER+"-"+id);
            if (!containerDir.mkdirs()) return null;

            Container container = new Container(id, this);
            container.setRootDir(containerDir);
            container.loadData(data);

            container.setWineVersion(data.getString("wineVersion"));

            if (!extractContainerPatternFile(container, container.getWineVersion(), contentsManager, containerDir, null)) {
                FileUtils.delete(containerDir);
                return null;
            }

            container.saveData();
            maxContainerId++;
            containers.add(container);
            return container;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void duplicateContainer(Container srcContainer) {
        int id = maxContainerId + 1;

        File dstDir = new File(homeDir, ImageFs.USER + "-" + id);
        if (!dstDir.mkdirs()) return;

        if (!FileUtils.copy(srcContainer.getRootDir(), dstDir, file -> FileUtils.chmod(file, 0771))) {
            FileUtils.delete(dstDir);
            return;
        }

        Container dstContainer = new Container(id, this);
        dstContainer.setRootDir(dstDir);
        dstContainer.setName(srcContainer.getName() + " (" + context.getString(R.string._copy) + ")");
        dstContainer.setScreenSize(srcContainer.getScreenSize());
        dstContainer.setEnvVars(srcContainer.getEnvVars());
        dstContainer.setCPUList(srcContainer.getCPUList());
        dstContainer.setCPUListWoW64(srcContainer.getCPUListWoW64());
        dstContainer.setGraphicsDriver(srcContainer.getGraphicsDriver());
        dstContainer.setDXWrapper(srcContainer.getDXWrapper());
        dstContainer.setDXWrapperConfig(srcContainer.getDXWrapperConfig());
        dstContainer.setAudioDriver(srcContainer.getAudioDriver());
        dstContainer.setWinComponents(srcContainer.getWinComponents());
        dstContainer.setDrives(srcContainer.getDrives());
        dstContainer.setShowFPS(srcContainer.isShowFPS());
        dstContainer.setStartupSelection(srcContainer.getStartupSelection());
        dstContainer.setBox64Preset(srcContainer.getBox64Preset());
        dstContainer.setDesktopTheme(srcContainer.getDesktopTheme());
        dstContainer.setWineVersion(srcContainer.getWineVersion());
        dstContainer.saveData();

        maxContainerId++;
        containers.add(dstContainer);
    }


    private void removeContainer(Container container) {
        if (FileUtils.delete(container.getRootDir())) containers.remove(container);
    }

    public ArrayList<Shortcut> loadShortcuts() {
        ArrayList<Shortcut> shortcuts = new ArrayList<>();
        for (Container container : containers) {
            File desktopDir = container.getDesktopDir();
            ArrayList<File> files = new ArrayList<>();
            if (desktopDir.exists())
                files.addAll(Arrays.asList(desktopDir.listFiles()));
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".lnk")) {
                        String filePath = file.getPath();
                        File desktopFile = new File(filePath.substring(0, filePath.lastIndexOf(".")) + ".desktop");
                        if (!desktopFile.exists()) {
                            MSLink.createDesktopFile(file, context);
                            shortcuts.add(new Shortcut(container, desktopFile));
                        }
                    }
                    else if (fileName.endsWith(".desktop")) shortcuts.add(new Shortcut(container, file));
                }
            }
        }

        shortcuts.sort(Comparator.comparing(a -> a.name));
        return shortcuts;
    }

    public int getNextContainerId() {
        return maxContainerId + 1;
    }

    public Container getContainerById(int id) {
        for (Container container : containers) if (container.id == id) return container;
        return null;
    }

    // Refactored to accept File wineDir instead of WineInfo object, to support Proton manually
    private void extractCommonDlls(File wineDir, boolean isArm64EC, String srcName, String dstName, File containerDir, OnExtractFileListener onExtractFileListener) throws JSONException {
        File srcDir = new File(wineDir, "lib/wine/" + srcName);

        File[] srcfiles = srcDir.listFiles(file -> file.isFile());
        if (srcfiles == null) return;

        for (File file : srcfiles) {
            String dllName = file.getName();
            if (dllName.equals("iexplore.exe") && isArm64EC && srcName.equals("aarch64-windows"))
                file = new File(wineDir, "lib/wine/" + "i386-windows/iexplore.exe");
            if (dllName.equals("tabtip.exe") || dllName.equals("icu.dll"))
                continue;
            File dstFile = new File(containerDir, ".wine/drive_c/windows/" + dstName + "/" + dllName);
            if (dstFile.exists()) continue;
            if (onExtractFileListener != null ) {
                dstFile = onExtractFileListener.onExtractFile(dstFile, 0);
                if (dstFile == null) continue;
            }
            FileUtils.copy(file, dstFile);
        }
    }

    public boolean extractContainerPatternFile(Container container, String wineVersion, ContentsManager contentsManager, File containerDir, OnExtractFileListener onExtractFileListener) {
        WineInfo wineInfo = WineInfo.fromIdentifier(context, contentsManager, wineVersion);
        
        File sourceDir;
        boolean isArm64EC;

        // Logic to determine source directory and architecture (Supports Proton fallback)
        if (wineInfo != null) {
            sourceDir = new File(wineInfo.path);
            isArm64EC = wineInfo.isArm64EC();
        } else {
            // Fallback for Proton or unrecognized versions
            ContentProfile profile = contentsManager.getProfileByEntryName(wineVersion);
            if (profile != null) {
                sourceDir = ContentsManager.getInstallDir(context, profile);
                isArm64EC = wineVersion.toLowerCase(Locale.ROOT).contains("arm64ec");
            } else {
                return false; // Cannot find profile
            }
        }

        if (sourceDir == null || !sourceDir.exists()) return false;

        String containerPattern = wineVersion + "_container_pattern.tzst";
        boolean result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, containerPattern, containerDir, onExtractFileListener);

        if (!result) {
            File containerPatternFile = new File(sourceDir, "prefixPack.txz");
            result = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, containerPatternFile, containerDir);
        }

        if (result) {
            try {
                if (isArm64EC)
                    extractCommonDlls(sourceDir, true, "aarch64-windows", "system32", containerDir, onExtractFileListener); // arm64ec only
                else
                    extractCommonDlls(sourceDir, false, "x86_64-windows", "system32", containerDir, onExtractFileListener);

                extractCommonDlls(sourceDir, isArm64EC, "i386-windows", "syswow64", containerDir, onExtractFileListener);
            }
            catch (JSONException e) {
                return false;
            }
        }
   
        return result;
    }

    public Container getContainerForShortcut(Shortcut shortcut) {
        for (Container container : containers) {
            if (container.id == shortcut.getContainerId()) {
                return container;
            }
        }
        return null; 
    }

    private void runOnUiThread(Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}