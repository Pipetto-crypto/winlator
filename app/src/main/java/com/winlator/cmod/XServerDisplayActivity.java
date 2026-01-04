package com.winlator.cmod;

import static com.winlator.cmod.core.AppUtils.showToast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.winlator.cmod.box64.Box64Preset;
import com.winlator.cmod.box64.Box64PresetManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.contentdialog.DebugDialog;
import com.winlator.cmod.contentdialog.DXVKConfigDialog;
import com.winlator.cmod.contentdialog.ScreenEffectConfigDialog;
import com.winlator.cmod.contentdialog.WineD3DConfigDialog;
import com.winlator.cmod.controllers.ControlsView;
import com.winlator.cmod.controllers.ExternalController;
import com.winlator.cmod.controllers.GameControllerManager;
import com.winlator.cmod.controllers.TouchpadView;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.CursorLocker;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.FrameRating;
import com.winlator.cmod.core.KeyValueSet;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.core.WineUtils;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalControllerBinding;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.math.XForm;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.widget.MagnifierView;
import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.winhandler.WinHandler;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.XEnvironment;
import com.winlator.cmod.xserver.Keyboard;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.Property;
import com.winlator.cmod.xserver.ScreenInfo;
import com.winlator.cmod.xserver.Window;
import com.winlator.cmod.xserver.WindowLifecycleListener;
import com.winlator.cmod.xserver.WindowManager;
import com.winlator.cmod.xserver.XServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class XServerDisplayActivity extends AppCompatActivity implements
        XServerView.RendererCallback,
        WindowLifecycleListener,
        View.OnApplyWindowInsetsListener,
        SensorEventListener {
    private XServerView xServerView;
    private InputControlsManager inputControlsManager;
    private XEnvironment environment;
    private DrawerLayout drawerLayout;
    private ContainerManager containerManager;
    private Container container;
    private XServer xServer;
    private Shortcut shortcut;
    private String graphicsDriver = "turnip";
    private String audioDriver = "alsa";
    private String dXWrapper = "dxvk";
    private WinHandler winHandler;
    private ControlsView controlsView;
    private TouchpadView touchpadView;
    private CursorLocker cursorLocker;
    private final EnvVars overrideEnvVars = new EnvVars();
    private boolean firstTime = true;
    private Runnable onWindowModificationListener;
    private final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
    private FrameRating frameRating;
    private int frameRatingWindowId = -1;
    private String screenEffectProfile = "off";
    private GameControllerManager gameControllerManager;
    private final float[] gyroscopeData = new float[3];
    private MagnifierView magnifierView;
    private boolean taskbarShown = true;
    private boolean canShowTaskbar = false;
    private boolean pointerCaptureRequested = false;
    private boolean isDarkMode;

    @Override
    public void onWindowCreated(Window window) {
        if (onWindowModificationListener != null) onWindowModificationListener.run();
        changeFrameRatingVisibility(window, null);
    }

    @Override
    public void onWindowPropertiesChanged(Window window, Property property) {
        if (onWindowModificationListener != null) onWindowModificationListener.run();
        changeFrameRatingVisibility(window, property);
    }

    @Override
    public void onWindowDestroyed(Window window) {
        if (onWindowModificationListener != null) onWindowModificationListener.run();
        changeFrameRatingVisibility(window, null);
    }

    @Override
    public void onSceneChanged() {
        if (onWindowModificationListener != null) onWindowModificationListener.run();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.hideSystemUI(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        isDarkMode = preferences.getBoolean("dark_mode", false);

        if (isDarkMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }

        setContentView(R.layout.xserver_display_activity);

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int containerId = getIntent().getIntExtra("container_id", 0);
        containerManager = new ContainerManager(this);
        container = containerManager.getContainerById(containerId);
        containerManager.activateContainer(container);

        if (!preferences.getBoolean("show_taskbar", true)) {
            taskbarShown = false;
            findViewById(R.id.FLTaskbar).setVisibility(View.GONE);
        }

        preloaderDialog.show(R.string.starting_up);

        String shortcutPath = getIntent().getStringExtra("shortcut_path");
        if (shortcutPath != null && !shortcutPath.isEmpty()) {
            shortcut = new Shortcut(container, new File(shortcutPath));
        }

        inputControlsManager = new InputControlsManager(this);
        xServerView = findViewById(R.id.XServerView);
        xServerView.setRendererCallback(this);

        controlsView = findViewById(R.id.ControlsView);
        controlsView.setInputControlsManager(inputControlsManager);
        controlsView.setXServerView(xServerView);

        touchpadView = findViewById(R.id.TouchpadView);
        touchpadView.setInputControlsManager(inputControlsManager);
        touchpadView.setXServerView(xServerView);

        setupUI();
        setupEnvironment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (xServer != null) xServerView.onResume();
        ExternalController.updateAvailableControllers(this);
        gameControllerManager = new GameControllerManager(this);
        
        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (xServer != null) xServerView.onPause();
        if (gameControllerManager != null) {
            gameControllerManager.close();
            gameControllerManager = null;
        }

        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (environment != null) environment.stop();
    }

    @Override
    public void onSurfaceCreated(GLRenderer renderer) {
        if (xServer != null && xServer.screenInfo != null) {
            renderer.setScreenInfo(xServer.screenInfo);
        }
    }

    @Override
    public void onSurfaceChanged(GLRenderer renderer, int width, int height) {
        if (xServer != null) xServer.screenInfo.resize(width, height);
    }

    private void setupEnvironment() {
        environment = new XEnvironment(this, container);
        environment.addEnvironmentVariable("WINEPREFIX", ImageFs.USER_HOME_DIR + "/.wine");

        if (shortcut != null) {
            String execArgs = shortcut.getExtra("execArgs");
        }
        
        Executors.newSingleThreadExecutor().execute(() -> {
            environment.start();
            runOnUiThread(this::onXEnvironmentStarted);
        });
    }

    private void onXEnvironmentStarted() {
        xServer = environment.getXServer();
        xServer.setWindowLifecycleListener(this);
        winHandler = environment.getWinHandler();
        
        preloaderDialog.close();
        
        startWine();
    }

    private void startWine() {
        String command = getWineStartCommand();
        winHandler.exec(command);
    }

    private String getWineStartCommand() {
        EnvVars envVars = getOverrideEnvVars();
        String args = "";

        if (shortcut != null) {
            String execArgs = shortcut.getExtra("execArgs");
            execArgs = !execArgs.isEmpty() ? " " + execArgs : "";

            if (shortcut.path.endsWith(".lnk")) {
                args += "\"" + shortcut.path + "\"" + execArgs;
            } else {
                String fullPath = shortcut.path.replace("\"", ""); 
                String exeDir;
                String filename;

                if (fullPath.contains("\\")) {
                    int lastSlash = fullPath.lastIndexOf("\\");
                    if (lastSlash != -1) {
                        exeDir = fullPath.substring(0, lastSlash);
                        filename = fullPath.substring(lastSlash + 1);
                    } else {
                        exeDir = "D:\\";
                        filename = fullPath;
                    }
                } else {
                    exeDir = FileUtils.getDirname(fullPath);
                    filename = FileUtils.getName(fullPath);
                }

                int dotIndex = filename.lastIndexOf(".");
                int spaceIndex = (dotIndex != -1) ? filename.indexOf(" ", dotIndex) : -1;

                if (spaceIndex != -1) {
                    execArgs = filename.substring(spaceIndex + 1) + execArgs;
                    filename = filename.substring(0, spaceIndex);
                }

                args += "/dir " + StringUtils.escapeDOSPath(exeDir) + " \"" + filename + "\"" + execArgs;
            }
        } else {
            if (envVars.has("EXTRA_EXEC_ARGS")) {
                args += " " + envVars.get("EXTRA_EXEC_ARGS");
                envVars.remove("EXTRA_EXEC_ARGS");
            } else {
                args += "\"wfm.exe\"";
            }
        }
        return "winhandler.exe " + args;
    }

    private EnvVars getOverrideEnvVars() {
        return overrideEnvVars;
    }

    private void setupUI() {
        drawerLayout = findViewById(R.id.DrawerLayout);
        
        findViewById(R.id.BTKeyboard).setOnClickListener(v -> {
            AppUtils.showKeyboard(this);
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.BTInputControls).setOnClickListener(v -> {
            showInputControlsDialog();
            if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        });
    }

    private void showInputControlsDialog() {
    }

    @Override
    public boolean onApplyWindowInsets(View v, androidx.core.view.WindowInsetsCompat insets) {
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
             System.arraycopy(event.values, 0, gyroscopeData, 0, 3);
             if (gameControllerManager != null) {
                 gameControllerManager.setGyroscopeState(gyroscopeData);
             }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    private void changeFrameRatingVisibility(Window window, Property property) {
        if (frameRating == null) return;

        if (property != null) {
            if (frameRatingWindowId == -1 && property.nameAsString().contains("_MESA_DRV")) {
                frameRatingWindowId = window.id;
                Log.d("XServerDisplayActivity", "Showing hud for Window " + window.getName());
                frameRating.update();
            }
            if (property.nameAsString().contains("_MESA_DRV_ENGINE_NAME")) {
                runOnUiThread(() -> frameRating.setRenderer(property.toString()));
            }
            if (property.nameAsString().contains("_MESA_DRV_GPU_NAME")) {
                runOnUiThread(() -> frameRating.setGpuName(property.toString()));
            }
        }
        else if (frameRatingWindowId != -1) {
            frameRatingWindowId = -1;
            Log.d("XServerDisplayActivity", "Hiding hud for Window " + window.getName());
            runOnUiThread(() -> frameRating.setVisibility(View.GONE));
            runOnUiThread(() -> frameRating.reset());
        }
    }
}