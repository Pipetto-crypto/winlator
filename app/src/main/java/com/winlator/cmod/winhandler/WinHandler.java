package com.winlator.cmod.winhandler;

import static com.winlator.cmod.inputcontrols.ExternalController.TRIGGER_IS_AXIS;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.net.LocalServerSocket;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalController;
import com.winlator.cmod.inputcontrols.FakeInputWriter;
import com.winlator.cmod.inputcontrols.GamepadState;
import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.xserver.XServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WinHandler {
    private static final short SERVER_PORT = 7947;
    private static final short CLIENT_PORT = 7946;
    public static final byte FLAG_DINPUT_MAPPER_STANDARD = 0x01;
    public static final byte FLAG_DINPUT_MAPPER_XINPUT = 0x02;
    public static final byte FLAG_INPUT_TYPE_XINPUT = 0x04;
    public static final byte FLAG_INPUT_TYPE_DINPUT = 0x08;
    public static final byte DEFAULT_INPUT_TYPE = FLAG_INPUT_TYPE_XINPUT;
    public static final byte INPUT_TYPE_MIXED = 2;
    private DatagramSocket socket;
    private final ByteBuffer sendData = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
    private final ByteBuffer receiveData = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
    private final DatagramPacket sendPacket = new DatagramPacket(sendData.array(), 64);
    private final DatagramPacket receivePacket = new DatagramPacket(receiveData.array(), 64);
    private final ArrayDeque<Runnable> actions = new ArrayDeque<>();
    private boolean initReceived = false;
    private boolean running = false;
    private OnGetProcessInfoListener onGetProcessInfoListener;
    private ExternalController currentController;
    private final Map<Integer, ExternalController> controllers = new HashMap<>();
    private InetAddress localhost;
    private byte inputType = DEFAULT_INPUT_TYPE;
    private final XServerDisplayActivity activity;
    private final List<Integer> gamepadClients = new CopyOnWriteArrayList<>();
    private SharedPreferences preferences;
    private byte triggerType;

    private static final int MAX_CONTROLLERS = 4;
    private static final int OSC_DEVICE_ID = -1;
    private FakeInputWriter[] writers = new FakeInputWriter[MAX_CONTROLLERS];
    private Map<Integer, Integer> deviceToSlot = new HashMap<>();
    private Set<Integer> usedSlots = new HashSet<>();
    private String fakeInputBasePath;

    private boolean xinputDisabled; // Used for exclusive mouse controllegacy
    private boolean xinputDisabledInitialized = false;


    private int fallbackSlot = -1;

    private final InputManager inputManager;
    private final InputManager.InputDeviceListener inputDeviceListener;

    private boolean useLegacyInputMethod = false; // Default to using the new input method
    // Add this field near the other field declarations at the top of the class
    // Add these constants at the top of the class where other constants are defined
    public static final byte DINPUT_MAPPER_TYPE_STANDARD = 0;
    public static final byte DINPUT_MAPPER_TYPE_XINPUT = 1;
    private byte dinputMapperType = DINPUT_MAPPER_TYPE_XINPUT; // Default value, you can set this as needed



    // Gyro related variables
    private float gyroX = 0;
    private float gyroY = 0;
    // Add fields for sensitivity, smoothing, and inversion
    private float gyroSensitivityX = 0.35f;
    private float gyroSensitivityY = 0.25f;
    private float smoothingFactor = 0.45f; // For exponential smoothing
    private boolean invertGyroX = true;
    private boolean invertGyroY = false;
    private float gyroDeadzone = 0.01f;

    // Implement exponential smoothing
    private float smoothGyroX = 0;
    private float smoothGyroY = 0;

    private boolean processGyroWithLeftTrigger = false;

    private int gyroTriggerButton;
    private boolean isGyroActive = false;
    private boolean isToggleMode;

    public void setGyroSensitivityX(float sensitivity) {
        this.gyroSensitivityX = sensitivity;
    }

    public void setGyroSensitivityY(float sensitivity) {
        this.gyroSensitivityY = sensitivity;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = factor;
    }

    public void setInvertGyroX(boolean invert) {
        this.invertGyroX = invert;
    }

    public void setInvertGyroY(boolean invert) {
        this.invertGyroY = invert;
    }

    public void setGyroDeadzone(float deadzone) {
        this.gyroDeadzone = deadzone;
    }

    private boolean isLeftTriggerPressed() {
        return currentController != null && currentController.state.triggerL > 0.5f; // Assuming 0.5f is the threshold for pressed
    }


    public void updateGyroData(float rawGyroX, float rawGyroY) {
        // Check if gyro is enabled before processing the data
        if (!preferences.getBoolean("gyro_enabled", false)) {
            return; // Exit if the gyro is disabled
        }

        boolean shouldProcessGyro = true;

        // Check if processing gyro data only when the left trigger is held
        if (processGyroWithLeftTrigger) {
            shouldProcessGyro = isLeftTriggerPressed();
        }



        if (isGyroActive) {
            // Apply deadzone
            if (Math.abs(rawGyroX) < gyroDeadzone) rawGyroX = 0;
            if (Math.abs(rawGyroY) < gyroDeadzone) rawGyroY = 0;

            // Apply inversion
            if (invertGyroX) rawGyroX = -rawGyroX;
            if (invertGyroY) rawGyroY = -rawGyroY;

            // Further reduce sensitivity by lowering the multiplier
            float sensitivityMultiplier = 0.25f; // Reduce the sensitivity even more
            rawGyroX *= gyroSensitivityX * sensitivityMultiplier;
            rawGyroY *= gyroSensitivityY * sensitivityMultiplier;

            // Apply smoothing
            smoothGyroX = smoothGyroX * smoothingFactor + rawGyroX * (1 - smoothingFactor);
            smoothGyroY = smoothGyroY * smoothingFactor + rawGyroY * (1 - smoothingFactor);

            // Clamp the result to reduce the overall range of movement
            smoothGyroX = Mathf.clamp(smoothGyroX, -0.25f, 0.25f); // Reduce clamping range for less movement
            smoothGyroY = Mathf.clamp(smoothGyroY, -0.25f, 0.25f);

            // Update the gyro data
            this.gyroX = smoothGyroX;
            this.gyroY = smoothGyroY;

            // Send the updated gamepad state
            sendGamepadState();
        }
    }

    private void writeState(int slot, GamepadState state) {
        FakeInputWriter writer = writers[slot];
        if (writer == null) return;
        writer.writeGamepadState(state);
    }


    public WinHandler(XServerDisplayActivity activity) {
        this.activity = activity;
        this.inputManager = (InputManager) activity.getSystemService(Context.INPUT_SERVICE);
        this.inputDeviceListener = new InputManager.InputDeviceListener() {
            @Override
            public void onInputDeviceAdded(int deviceId) {
            }

            @Override
            public void onInputDeviceRemoved(int deviceId) {
                releaseSlot(deviceId);
            }

            @Override
            public void onInputDeviceChanged(int deviceId) {
            }
        };
        inputManager.registerInputDeviceListener(inputDeviceListener, null);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
    }
    private boolean sendPacket(int port) {
        try {
            int size = sendData.position();
            if (size == 0) return false;
            sendPacket.setAddress(localhost);
            sendPacket.setPort(port);
            socket.send(sendPacket);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public void exec(String command) {
        command = command.trim();
        if (command.isEmpty()) return;

        // The `split` function here should be sensitive to paths with spaces.
        // Instead of splitting, let's assume that command is directly provided in two parts: filename and parameters.
        // Adjust command splitting based on whether it contains quotes.

        String filename;
        String parameters;

        if (command.contains("\"")) {
            // If the command is quoted, extract the quoted part as the filename
            int firstQuote = command.indexOf("\"");
            int lastQuote = command.lastIndexOf("\"");
            filename = command.substring(firstQuote + 1, lastQuote);
            if (lastQuote + 1 < command.length()) {
                parameters = command.substring(lastQuote + 1).trim();
            } else {
                parameters = "";
            }
        } else {
            // Standard split when no quotes
            String[] cmdList = command.split(" ", 2);
            filename = cmdList[0];
            if (cmdList.length > 1) {
                parameters = cmdList[1];
            } else {
                parameters = "";
            }
        }

        addAction(() -> {
            byte[] filenameBytes = filename.getBytes();
            byte[] parametersBytes = parameters.getBytes();

            sendData.rewind();
            sendData.put(RequestCodes.EXEC);
            sendData.putInt(filenameBytes.length + parametersBytes.length + 8);
            sendData.putInt(filenameBytes.length);
            sendData.putInt(parametersBytes.length);
            sendData.put(filenameBytes);
            sendData.put(parametersBytes);
            sendPacket(CLIENT_PORT);
        });
    }


    public void killProcess(final String processName) {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.KILL_PROCESS);
            byte[] bytes = processName.getBytes();
            sendData.putInt(bytes.length);
            sendData.put(bytes);
            sendPacket(CLIENT_PORT);
        });
    }

    public void listProcesses() {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.LIST_PROCESSES);
            sendData.putInt(0);

            if (!sendPacket(CLIENT_PORT) && onGetProcessInfoListener != null) {
                onGetProcessInfoListener.onGetProcessInfo(0, 0, null);
            }
        });
    }

    public void setProcessAffinity(final String processName, final int affinityMask) {
        addAction(() -> {
            byte[] bytes = processName.getBytes();
            sendData.rewind();
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY);
            sendData.putInt(9 + bytes.length);
            sendData.putInt(0);
            sendData.putInt(affinityMask);
            sendData.put((byte)bytes.length);
            sendData.put(bytes);
            sendPacket(CLIENT_PORT);
        });
    }

    public void setProcessAffinity(final int pid, final int affinityMask) {
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.SET_PROCESS_AFFINITY);
            sendData.putInt(9);
            sendData.putInt(pid);
            sendData.putInt(affinityMask);
            sendData.put((byte)0);
            sendPacket(CLIENT_PORT);
        });
    }

    public void mouseEvent(int flags, int dx, int dy, int wheelDelta) {
        if (!initReceived) return;
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.MOUSE_EVENT);
            sendData.putInt(10);
            sendData.putInt(flags);
            sendData.putShort((short)dx);
            sendData.putShort((short)dy);
            sendData.putShort((short)wheelDelta);
            sendData.put((byte)((flags & MouseEventFlags.MOVE) != 0 ? 1 : 0)); // cursor pos feedback
            sendPacket(CLIENT_PORT);
        });
    }

    public void keyboardEvent(byte vkey, int flags) {
        if (!initReceived) return;
        addAction(() -> {
            sendData.rewind();
            sendData.put(RequestCodes.KEYBOARD_EVENT);
            sendData.put(vkey);
            sendData.putInt(flags);
            sendPacket(CLIENT_PORT);
        });
    }

    public void bringToFront(final String processName) {
        bringToFront(processName, 0);
    }

    public void bringToFront(final String processName, final long handle) {
        addAction(() -> {
            sendData.rewind();
            try {
                sendData.put(RequestCodes.BRING_TO_FRONT);
                byte[] bytes = processName.getBytes();
                sendData.putInt(bytes.length);
                // FIXME: Chinese and Japanese got from winhandler.exe are broken, and they cause overflow.
                sendData.put(bytes);
                sendData.putLong(handle);
            } catch (java.nio.BufferOverflowException e) {
                e.printStackTrace();
                sendData.rewind();
            }
            sendPacket(CLIENT_PORT);
        });
    }

    private void addAction(Runnable action) {
        synchronized (actions) {
            actions.add(action);
            actions.notify();
        }
    }

    public OnGetProcessInfoListener getOnGetProcessInfoListener() {
        return onGetProcessInfoListener;
    }

    public void setOnGetProcessInfoListener(OnGetProcessInfoListener onGetProcessInfoListener) {
        synchronized (actions) {
            this.onGetProcessInfoListener = onGetProcessInfoListener;
        }
    }

    private void startSendThread() {
        Executors.newSingleThreadExecutor().execute(() -> {
            while (running) {
                synchronized (actions) {
                    while (initReceived && !actions.isEmpty()) actions.poll().run();
                    try {
                        actions.wait();
                    }
                    catch (InterruptedException e) {}
                }
            }
        });
    }

    public void stop() {
        running = false;
        closeFakeInputWriter();

        if (socket != null) {
            socket.close();
            socket = null;
        }

        synchronized (actions) {
            actions.notify();
        }
    }

    private void handleRequest(byte requestCode, final int port) {
        switch (requestCode) {
            case RequestCodes.INIT: {
                initReceived = true;

                preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());

                gyroTriggerButton = preferences.getInt("gyro_trigger_button", KeyEvent.KEYCODE_BUTTON_L1);
                isToggleMode = preferences.getInt("gyro_mode", 0) == 1; // 1 is toggle mode, 0 is hold mode


                // Load and apply trigger mode and xinput toggle settings
                triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);

                refreshControllerMappings();

                // Only set xinputDisabled if it hasn't been set explicitly by XServerDisplayActivity
                if (!xinputDisabledInitialized) {
                    xinputDisabled = preferences.getBoolean("xinput_toggle", false);
                }

                // Load the flag to use legacy input method
                useLegacyInputMethod = preferences.getBoolean("useLegacyInputMethod", false);

                // Load and apply gyro settings
                setGyroSensitivityX(preferences.getFloat("gyro_x_sensitivity", 1.0f));
                setGyroSensitivityY(preferences.getFloat("gyro_y_sensitivity", 1.0f));
                setSmoothingFactor(preferences.getFloat("gyro_smoothing", 0.9f));
                setInvertGyroX(preferences.getBoolean("invert_gyro_x", false));
                setInvertGyroY(preferences.getBoolean("invert_gyro_y", false));
                setGyroDeadzone(preferences.getFloat("gyro_deadzone", 0.05f));

                processGyroWithLeftTrigger = preferences.getBoolean("process_gyro_with_left_trigger", false);

                synchronized (actions) {
                    actions.notify();
                }
                break;
            }

            case RequestCodes.GET_PROCESS: {
                if (onGetProcessInfoListener == null) return;
                receiveData.position(receiveData.position() + 4);
                int numProcesses = receiveData.getShort();
                int index = receiveData.getShort();
                int pid = receiveData.getInt();
                long memoryUsage = receiveData.getLong();
                int affinityMask = receiveData.getInt();
                boolean wow64Process = receiveData.get() == 1;

                byte[] bytes = new byte[32];
                receiveData.get(bytes);
                String name = StringUtils.fromANSIString(bytes);

                onGetProcessInfoListener.onGetProcessInfo(index, numProcesses,
                        new ProcessInfo(pid, name, memoryUsage, affinityMask, wow64Process));
                break;
            }
            case RequestCodes.GET_GAMEPAD: {
                if (xinputDisabled) return;
                boolean isXInput = receiveData.get() == 1;
                boolean notify = receiveData.get() == 1;
                final ControlsProfile profile = activity.getInputControlsView().getProfile();
                boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad();

                if (!useVirtualGamepad && (currentController == null || !currentController.isConnected())) {
                    currentController = ExternalController.getController(0);
                    if (currentController != null) {
                        currentController.setTriggerType(triggerType);
                    }
                }

                final boolean enabled = currentController != null || useVirtualGamepad;

                if (enabled && notify) {
                    if (!gamepadClients.contains(port)) gamepadClients.add(port);
                } else {
                    gamepadClients.remove(Integer.valueOf(port));
                }

                addAction(() -> {
                    sendData.rewind();
                    sendData.put(RequestCodes.GET_GAMEPAD);

                    if (enabled) {
                        sendData.putInt(!useVirtualGamepad ? currentController.getDeviceId() : profile.id);

                        if (useLegacyInputMethod) {
                            // Use legacy DInput mapper type
                            sendData.put(dinputMapperType);
                        } else {
                            // Use new input type flags
                            sendData.put(inputType);
                        }

                        byte[] bytes = (useVirtualGamepad ? profile.getName() : currentController.getName()).getBytes();
                        sendData.putInt(bytes.length);
                        sendData.put(bytes);
                    } else {
                        sendData.putInt(0);
                    }

                    sendPacket(port);
                });
                break;
            }
            case RequestCodes.GET_GAMEPAD_STATE: {
                if (xinputDisabled) return;
                int gamepadId = receiveData.getInt();
                final ControlsProfile profile = activity.getInputControlsView().getProfile();
                boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad();
                final boolean enabled = currentController != null || useVirtualGamepad;

                if (currentController != null && currentController.getDeviceId() != gamepadId) currentController = null;

                addAction(() -> {
                    sendData.rewind();
                    sendData.put(RequestCodes.GET_GAMEPAD_STATE);
                    sendData.put((byte)(enabled ? 1 : 0));

                    if (enabled) {
                        sendData.putInt(gamepadId);
                        if (useVirtualGamepad) {
                            profile.getGamepadState().writeTo(sendData);
                        } else {
                            currentController.state.writeTo(sendData);
                        }
                    }

                    sendPacket(port);
                });
                break;
            }
            case RequestCodes.RELEASE_GAMEPAD: {
                currentController = null;
                gamepadClients.clear();
                break;
            }
            case RequestCodes.CURSOR_POS_FEEDBACK: {
                short x = receiveData.getShort();
                short y = receiveData.getShort();
                XServer xServer = activity.getXServer();
                xServer.pointer.setX(x);
                xServer.pointer.setY(y);
                activity.getXServerView().onPointerMove(x, y);
                break;
            }
            default: {
                // Handle any other request codes if needed
                break;
            }
        }
    }



    public void start() {
        try {
            localhost = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e) {
            try {
                localhost = InetAddress.getByName("127.0.0.1");
            }
            catch (UnknownHostException ex) {}
        }

        running = true;
        startSendThread();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                socket = new DatagramSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress((InetAddress)null, SERVER_PORT));

                while (running) {
                    socket.receive(receivePacket);

                    synchronized (actions) {
                        receiveData.rewind();
                        byte requestCode = receiveData.get();
                        handleRequest(requestCode, receivePacket.getPort());
                    }
                }
            }
            catch (IOException e) {}
        });
    }

    public void sendGamepadState() {
        if (xinputDisabled) return;

        final ControlsProfile profile = activity.getInputControlsView().getProfile();

        Log.d("WinHandler", "sendGamepadState: profile=" + (profile != null ? profile.getName() : "null")
                + " virtual=" + (profile != null && profile.isVirtualGamepad())
                + " showOsc=" + activity.getInputControlsView().isShowTouchscreenControls()
                + " xinputDisabled=" + xinputDisabled);

        if (profile == null) {
            releaseSlot(OSC_DEVICE_ID);
            return;
        }

        final boolean useVirtualGamepad = profile.isVirtualGamepad()
                && activity.getInputControlsView().isShowTouchscreenControls();

        if (useVirtualGamepad) {
            int slot = assignSlot(OSC_DEVICE_ID);
            if (slot >= 0) writeState(slot, profile.getGamepadState());
        } else {
            releaseSlot(OSC_DEVICE_ID);
        }
    }

    public void sendGamepadState(ExternalController controller) {
        if (xinputDisabled || controller == null) return;

        ControlsProfile profile = activity.getInputControlsView().getProfile();
        if (profile != null) {
            ExternalController profileController = profile.getController(controller.getDeviceId());
            if (profileController != null && profileController.getControllerBindingCount() > 0) {
                int slot = assignSlot(controller.getDeviceId());
                if (slot >= 0) writeState(slot, profileController.remappedState);
                return;
            }
        }

        int slot = assignSlot(controller.getDeviceId());
        if (slot >= 0) writeState(slot, controller.state);
    }

    private int assignSlot(int deviceId) {
        Integer existing = deviceToSlot.get(deviceId);
        if (existing != null)
            return existing;

        for (int slot = 0; slot < MAX_CONTROLLERS; slot++) {
            if (!usedSlots.contains(slot)) {
                usedSlots.add(slot);
                deviceToSlot.put(deviceId, slot);
                if (fakeInputBasePath != null && writers[slot] == null) {
                    writers[slot] = new FakeInputWriter(fakeInputBasePath, slot);
                    writers[slot].open();
                    Log.d("WinHandler", "Assigned device " + deviceId + " to slot " + slot);
                }
                return slot;
            }
        }
        Log.w("WinHandler", "No slots available for device " + deviceId);
        return -1;
    }

    private void releaseSlot(int deviceId) {
        Integer slot = deviceToSlot.remove(deviceId);
        if (slot != null) {
            if (fallbackSlot == slot) fallbackSlot = -1;
            if (writers[slot] != null) {
                writers[slot].destroy();
                writers[slot] = null;
            }
            usedSlots.remove(slot);
            ExternalController removed = controllers.remove(deviceId);
            if (removed != null) removed.unregisterListener();
            Log.d("WinHandler", "Device " + deviceId + " disconnected (or OSC disabled). Slot released: " + slot);
        }
    }

    public void setXInputDisabled(boolean disabled) {
        this.xinputDisabled = disabled;
        this.xinputDisabledInitialized = true; // Mark as initialized
        Log.d("WinHandler", "XInput Disabled set to: " + xinputDisabled);
    }

    public void setFakeInputPath(String fakeInputPath) {
        if (fakeInputPath != null && !fakeInputPath.isEmpty()) {
            this.fakeInputBasePath = fakeInputPath;
            Log.d("WinHandler", "FakeInputWriter base path set: " + fakeInputPath);
        }
    }

    public void closeFakeInputWriter() {
        if (inputManager != null && inputDeviceListener != null) {
            inputManager.unregisterInputDeviceListener(inputDeviceListener);
        }
        for (int i = 0; i < MAX_CONTROLLERS; i++) {
            if (writers[i] != null) {
                writers[i].destroy();
                writers[i] = null;
            }
        }
        deviceToSlot.clear();
        usedSlots.clear();
        for (ExternalController c : controllers.values()) {
            if (c != null) c.unregisterListener();
        }
        controllers.clear();
        fallbackSlot = -1;
    }

    private ExternalController getController(int deviceId) {
        if (controllers.containsKey(deviceId)) {
            return controllers.get(deviceId);
        }
        ExternalController controller = ExternalController.getController(deviceId);
        if (controller != null) {
            controller.setTriggerType((byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS));
            controller.setContext(activity); // Loads deadzone, sensitivity and invert prefs
        }
        controllers.put(deviceId, controller);
        return controller;
    }

//    public boolean onGenericMotionEvent(MotionEvent event) {
//        boolean handled = false;
//        if (currentController != null && currentController.getDeviceId() == event.getDeviceId()) {
//            handled = currentController.updateStateFromMotionEvent(event);
//            if (handled) sendGamepadState();
//        }
//        return handled;
//    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean handled = false;
        ExternalController controller = getController(event.getDeviceId());

        if (controller != null) {
            handled = controller.updateStateFromMotionEvent(event);
            if (handled) {
                sendGamepadState(controller);
            }
        }
        return handled;
    }

    public boolean onKeyEvent(KeyEvent event) {
        boolean handled = false;
        ExternalController controller = getController(event.getDeviceId());

        if (event.getKeyCode() == gyroTriggerButton) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (isToggleMode) {
                    isGyroActive = !isGyroActive;
                } else {
                    isGyroActive = true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && !isToggleMode) {
                isGyroActive = false;

                // Reset the analog stick to center when the gyro activator is released
                if (currentController != null) {
                    currentController.state.thumbRX = 0.0f; // Reset X axis
                    currentController.state.thumbRY = 0.0f; // Reset Y axis
                }

                // Immediately send the updated gamepad state
                sendGamepadState();
            }
        }

        if (controller != null && event.getRepeatCount() == 0) {
            int action = event.getAction();

            if (action == KeyEvent.ACTION_DOWN) {
                handled = controller.updateStateFromKeyEvent(event);
            } else if (action == KeyEvent.ACTION_UP) {
                handled = controller.updateStateFromKeyEvent(event);
            }

            if (handled)
                sendGamepadState(controller);
        }
        return handled;
    }

    public byte getInputType() {
        return inputType;
    }

    public void setInputType(byte inputType) {
        this.inputType = inputType;
    }

    public ExternalController getCurrentController() {
        return currentController;
    }

    public void execWithDelay(String command, int delaySeconds) {
        if (command == null || command.trim().isEmpty() || delaySeconds < 0) return;

        // Use a scheduled executor for delay
        Executors.newSingleThreadScheduledExecutor().schedule(() -> exec(command), delaySeconds, TimeUnit.SECONDS);
    }

    public void initializeController() {
        currentController = ExternalController.getController(0);
        if (currentController != null) {
            currentController.setContext(activity); // Ensure context is set
            // Enforce mappings upon initialization
//            for (byte originalButton : ExternalController.buttonMappings.keySet()) {
//                currentController.setButtonMapping(originalButton, ExternalController.buttonMappings.get(originalButton));
//            }
//
//
//            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
//            triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);
//            currentController.setTriggerType(triggerType); // Ensure triggerType is set

            Log.d("WinHandler", "Force mappings applied on initialization.");
        }
    }



    // Re-applies controller preferences after the gamepad configurator dialog saves them
    public void refreshControllerMappings() {
        byte trigger = (byte) preferences.getInt("trigger_type", TRIGGER_IS_AXIS);
        for (ExternalController controller : controllers.values()) {
            if (controller == null) continue; // Cached "not a controller" entries
            controller.setTriggerType(trigger);
            controller.setContext(activity); // Reloads deadzone, sensitivity and inversion prefs
        }
    }


}
