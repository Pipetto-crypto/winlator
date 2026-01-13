package com.winlator.cmod.inputcontrols;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.XServerDisplayActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExternalController {
    public static final byte IDX_BUTTON_A = 0;
    public static final byte IDX_BUTTON_B = 1;
    public static final byte IDX_BUTTON_X = 2;
    public static final byte IDX_BUTTON_Y = 3;
    public static final byte IDX_BUTTON_L1 = 4;
    public static final byte IDX_BUTTON_R1 = 5;
    public static final byte IDX_BUTTON_SELECT = 6;
    public static final byte IDX_BUTTON_START = 7;
    public static final byte IDX_BUTTON_L3 = 8;
    public static final byte IDX_BUTTON_R3 = 9;
    public static final byte IDX_BUTTON_L2 = 10;
    public static final byte IDX_BUTTON_R2 = 11;
    public static final byte IDX_BUTTON_UP = 12;
    public static final byte IDX_BUTTON_RIGHT = 13;
    public static final byte IDX_BUTTON_DOWN = 14;
    public static final byte IDX_BUTTON_LEFT = 15;
    private String name;
    private String id;
    private int deviceId = -1;
    private final ArrayList<ExternalControllerBinding> controllerBindings = new ArrayList<>();
    public final GamepadState state = new GamepadState();
    private XServerDisplayActivity activity;



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void loadMappings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ctrl_map", Context.MODE_PRIVATE);

        // Load button mappings
        String buttons = prefs.getString("buttons", prefs.getString("m", ""));
        buttonMappings.clear();
        if (!buttons.isEmpty()) {
            for (String p : buttons.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    try {
                        buttonMappings.put(Byte.parseByte(kv[0]), Byte.parseByte(kv[1]));
                    } catch (Exception e) {}
                }
            }
        } else {
            // Defaults
            for (byte i = 0; i <= 15; i++) buttonMappings.put(i, i);
        }

        // Load axis mappings
        String axes = prefs.getString("axes", "");
        axisMappings.clear();
        if (!axes.isEmpty()) {
            for (String p : axes.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    try {
                        axisMappings.put(Byte.parseByte(kv[0]), Byte.parseByte(kv[1]));
                    } catch (Exception e) {}
                }
            }
        } else {
            // Defaults
            for (byte i = 0; i < 4; i++) axisMappings.put(i, i);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }








    // Remove static keyword
    public static final HashMap<Byte, Byte> buttonMappings = new HashMap<>();
    
    // Axis mapping constants
    public static final byte AXIS_LEFT_X = 0;
    public static final byte AXIS_LEFT_Y = 1;
    public static final byte AXIS_RIGHT_X = 2;
    public static final byte AXIS_RIGHT_Y = 3;
    public static final byte AXIS_LEFT_INVERT_X = 4;  // Invert left X
    public static final byte AXIS_LEFT_INVERT_Y = 5;  // Invert left Y
    public static final byte AXIS_RIGHT_INVERT_X = 6; // Invert right X
    public static final byte AXIS_RIGHT_INVERT_Y = 7; // Invert right Y
    
    // Axis mappings: physical axis → target axis
    public static final HashMap<Byte, Byte> axisMappings = new HashMap<>();
    
    // Get mapped axis with optional inversion
    public static byte getMappedAxis(byte physicalAxis) {
        return axisMappings.getOrDefault(physicalAxis, physicalAxis);
    }
    
    // Check if an axis should be inverted
    public static boolean isAxisInverted(byte axis) {
        byte mapped = getMappedAxis(axis);
        return mapped >= 4 && mapped <= 7;
    }
    
    // Get the actual target axis (removing invert flag)
    public static byte getTargetAxis(byte axis) {
        byte mapped = getMappedAxis(axis);
        if (mapped >= 4) return (byte)(mapped - 4);
        return mapped;
    }

    private boolean triggerLPressedViaButton = false;
    private boolean triggerRPressedViaButton = false;





    public int getDeviceId() {
        if (this.deviceId == -1) {
            for (int deviceId : InputDevice.getDeviceIds()) {
                InputDevice device = InputDevice.getDevice(deviceId);
                if (device != null && device.getDescriptor().equals(id)) {
                    this.deviceId = deviceId;
                    break;
                }
            }
        }
        return this.deviceId;
    }

    public boolean isConnected() {
        for (int deviceId : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && device.getDescriptor().equals(id)) return true;
        }
        return false;
    }

    public ExternalControllerBinding getControllerBinding(int keyCode) {
        for (ExternalControllerBinding controllerBinding : controllerBindings) {
            if (controllerBinding.getKeyCode() == keyCode) return controllerBinding;
        }
        return null;
    }

    public ExternalControllerBinding getControllerBindingAt(int index) {
        return controllerBindings.get(index);
    }

    public void addControllerBinding(ExternalControllerBinding controllerBinding) {
        if (getControllerBinding(controllerBinding.getKeyCode()) == null) controllerBindings.add(controllerBinding);
    }

    public int getPosition(ExternalControllerBinding controllerBinding) {
        return controllerBindings.indexOf(controllerBinding);
    }

    public void removeControllerBinding(ExternalControllerBinding controllerBinding) {
        controllerBindings.remove(controllerBinding);
    }

    public void setButtonMapping(byte originalButton, byte mappedButton) {
        buttonMappings.put(originalButton, mappedButton);
        // Remove triggerType handling from here
    }

    public void removeButtonMapping(byte originalButton) {
        buttonMappings.remove(originalButton);
    }

    public void removeButtonMappingForTarget(byte targetButton) {
        Iterator<Map.Entry<Byte, Byte>> iterator = buttonMappings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Byte, Byte> entry = iterator.next();
            if (entry.getValue() == targetButton) {
                iterator.remove();
            }
        }
    }


    public byte getMappedButton(byte originalButton) {
        byte mappedButton = buttonMappings.getOrDefault(originalButton, originalButton);
//        Log.d("ExternalController", "getMappedButton: Original button = " + originalButton + ", Mapped button = " + mappedButton);
        return mappedButton;
    }


    public int getControllerBindingCount() {
        return controllerBindings.size();
    }

    public JSONObject toJSONObject() {
        try {
            if (controllerBindings.isEmpty()) return null;
            JSONObject controllerJSONObject = new JSONObject();
            controllerJSONObject.put("id", id);
            controllerJSONObject.put("name", name);

            JSONArray controllerBindingsJSONArray = new JSONArray();
            for (ExternalControllerBinding controllerBinding : controllerBindings) controllerBindingsJSONArray.put(controllerBinding.toJSONObject());
            controllerJSONObject.put("controllerBindings", controllerBindingsJSONArray);

            return controllerJSONObject;
        }
        catch (JSONException e) {
            return null;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ExternalController ? ((ExternalController)obj).id.equals(this.id) : super.equals(obj);
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        // Get raw axis values
        float rawLX = getCenteredAxis(event, MotionEvent.AXIS_X, historyPos);
        float rawLY = getCenteredAxis(event, MotionEvent.AXIS_Y, historyPos);
        float rawRX = getCenteredAxis(event, MotionEvent.AXIS_Z, historyPos);
        float rawRY = getCenteredAxis(event, MotionEvent.AXIS_RZ, historyPos);
        
        // Physical axis values array: [leftX, leftY, rightX, rightY]
        float[] physical = {rawLX, rawLY, rawRX, rawRY};
        
        // Initialize output with zeros
        float[] output = {0, 0, 0, 0};
        
        // Apply axis mappings: for each physical axis, route to its target
        for (byte phys = 0; phys < 4; phys++) {
            byte mapped = axisMappings.getOrDefault(phys, phys);  // Default: same axis
            byte target = (byte)(mapped % 4);  // Target axis (0-3)
            boolean invert = mapped >= 4;  // Inversion flag
            
            float value = physical[phys];
            if (invert) value = -value;
            
            // Add value to output (allows multiple sources to same target)
            output[target] += value;
        }
        
        // Clamp and set output values
        state.thumbLX = Math.max(-1f, Math.min(1f, output[0]));
        state.thumbLY = Math.max(-1f, Math.min(1f, output[1]));
        state.thumbRX = Math.max(-1f, Math.min(1f, output[2]));
        state.thumbRY = Math.max(-1f, Math.min(1f, output[3]));

        if (historyPos == -1) {
            float axisX = getCenteredAxis(event, MotionEvent.AXIS_HAT_X, historyPos);
            float axisY = getCenteredAxis(event, MotionEvent.AXIS_HAT_Y, historyPos);

            // Remap Logic for HATs (D-Pad Virtual IDs 12-15)
            // 12=Up, 13=Right, 14=Down, 15=Left
            
            int[] dpadIds = {12, 13, 14, 15};
            boolean[] rawStates = {
                axisY == -1.0f, // Up
                axisX == 1.0f,  // Right
                axisY == 1.0f,  // Down
                axisX == -1.0f  // Left
            };

            for (int i=0; i<4; i++) {
                byte mapped = getMappedButton((byte)dpadIds[i]);
                boolean pressed = rawStates[i] && (i%2==0 ? Math.abs(state.thumbLY) : Math.abs(state.thumbLX)) < ControlElement.STICK_DEAD_ZONE;

                // Handle Target: D-Pad
                if (mapped >= 12 && mapped <= 15) {
                     int targetDir = mapped - 12; // 0-3
                     state.dpad[targetDir] = pressed; 
                } 
                // Handle Target: Triggers
                else if (mapped == IDX_BUTTON_L2) {
                     state.triggerL = Math.max(state.triggerL, pressed ? 1.0f : 0.0f);
                } else if (mapped == IDX_BUTTON_R2) {
                     state.triggerR = Math.max(state.triggerR, pressed ? 1.0f : 0.0f);
                }
                // Handle Target: Buttons
                else {
                     state.setPressed(mapped, pressed);
                }
            }
        }
    }



    private void processTriggerButton(MotionEvent event) {
        float l = event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 0f ? event.getAxisValue(MotionEvent.AXIS_BRAKE) : event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
        float r = event.getAxisValue(MotionEvent.AXIS_RTRIGGER) == 0f ? event.getAxisValue(MotionEvent.AXIS_GAS) : event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
        
        // Apply remapping for triggers
        byte mappedL = getMappedButton(IDX_BUTTON_L2);
        byte mappedR = getMappedButton(IDX_BUTTON_R2);
        
        // Handle left trigger remapping
        if (mappedL == IDX_BUTTON_L2) {
            // L2 not remapped - set trigger value normally
            state.triggerL = Math.max(state.triggerL, l);
        } else if (mappedL == IDX_BUTTON_R2) {
            // L2 remapped to R2 - swap trigger value
            state.triggerR = Math.max(state.triggerR, l);
        } else {
            // L2 remapped to a button - set button pressed, don't set triggerL
            state.setPressed(mappedL, l > 0.5f);
            // Suppress original trigger axis
        }
        
        // Handle right trigger remapping
        if (mappedR == IDX_BUTTON_R2) {
            // R2 not remapped - set trigger value normally
            state.triggerR = Math.max(state.triggerR, r);
        } else if (mappedR == IDX_BUTTON_L2) {
            // R2 remapped to L2 - swap trigger value
            state.triggerL = Math.max(state.triggerL, r);
        } else {
            // R2 remapped to a button - set button pressed, don't set triggerR
            state.setPressed(mappedR, r > 0.5f);
            // Suppress original trigger axis
        }
    }


//    private void processTriggerButton(MotionEvent event) {
//        // Get the raw analog values of L2 and R2 triggers
//        float l = event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 0f ? event.getAxisValue(MotionEvent.AXIS_BRAKE) : event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
//        float r = event.getAxisValue(MotionEvent.AXIS_RTRIGGER) == 0f ? event.getAxisValue(MotionEvent.AXIS_GAS) : event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
//
//        // Get the mapped buttons for L2 and R2
//        byte leftTriggerMapped = getMappedButton(IDX_BUTTON_L2);
//        byte rightTriggerMapped = getMappedButton(IDX_BUTTON_R2);
//
//
//
//        // --- Handle button remapping ONLY ---
//        // (Do NOT store original trigger values yet)
//
//        if (leftTriggerMapped == IDX_BUTTON_R2 && rightTriggerMapped == IDX_BUTTON_L2) {
//            // L2 and R2 are swapped
//            state.triggerL = r;
//            state.triggerR = l;
//            state.setPressed(IDX_BUTTON_L2, r == 1.0f);
//            state.setPressed(IDX_BUTTON_R2, l == 1.0f);
////            Log.d("ExternalController", "trigger was swapped");
//        } else {
//        if (leftTriggerMapped != IDX_BUTTON_L2 && leftTriggerMapped != IDX_BUTTON_R2) {
//            // L2 is remapped to a button OTHER than R2
//            state.setPressed(leftTriggerMapped, l > 0.5f);
//            state.triggerL = 0; // Ensure analog value is reset
////            Log.d("ExternalController", "trigger was reset");
//        }
//        if (rightTriggerMapped != IDX_BUTTON_R2 && rightTriggerMapped != IDX_BUTTON_L2) {
//            // R2 is remapped to a button OTHER than L2
//            state.setPressed(rightTriggerMapped, r > 0.5f);
//            state.triggerR = 0; // Ensure analog value is reset
//        }
//
//        // --- Handle trigger cross-mapping ---
//
//        // Reset trigger values to 0 before cross-mapping < Maybe remove this
////        state.triggerL = 0;
////        state.triggerR = 0;
//
//        if (leftTriggerMapped == IDX_BUTTON_R2 && rightTriggerMapped == IDX_BUTTON_L2) {
//            // L2 and R2 are swapped
//            state.triggerL = r;
//            state.triggerR = l;
//        } else if (leftTriggerMapped == IDX_BUTTON_R2 && rightTriggerMapped == IDX_BUTTON_R2) {
//            // BOTH L2 and R2 are mapped to R2
//            state.triggerR = Math.max(l, r);
//        } else if (leftTriggerMapped == IDX_BUTTON_L2 && rightTriggerMapped == IDX_BUTTON_L2) {
//            // BOTH L2 and R2 are mapped to L2
//            state.triggerL = Math.max(l, r);
//        } else {
//            // Not mapping to the same trigger, handle individually
//            if (rightTriggerMapped == IDX_BUTTON_L2) {
//                // R2 is mapped to L2
//                state.triggerL = r;
//            } else if (leftTriggerMapped == IDX_BUTTON_R2) {
//                // L2 is mapped to R2
//                state.triggerR = l;
//            }
//
//            // Set original values if not cross-mapped
//            if (leftTriggerMapped != IDX_BUTTON_R2) {
//                state.triggerL = l;
//            }
//            if (rightTriggerMapped != IDX_BUTTON_L2) {
//                state.triggerR = r;
//            }
//        }
//
//        if (leftTriggerMapped != IDX_BUTTON_L2 && leftTriggerMapped != IDX_BUTTON_R2) {
//            state.triggerL = 0; // Reset L2 analog value if it's mapped to anything else
////            Log.d("ExternalController", "trigger was reset");
//        }
//        if (rightTriggerMapped != IDX_BUTTON_R2 && rightTriggerMapped != IDX_BUTTON_L2) {
//            state.triggerR = 0; // Reset R2 analog value if it's mapped to anything else
//        }
//
//        }
//        // Log for debugging
////        Log.d("ExternalController", "processTriggerButton: L trigger = " + state.triggerL + ", R trigger = " + state.triggerR +
////                ", Mapped L trigger = " + leftTriggerMapped + ", Mapped R trigger = " + rightTriggerMapped);
//    }







//    public boolean updateStateFromMotionEvent(MotionEvent event) {
//        if (isJoystickDevice(event)) {
//            // Check if the event contains trigger axis data
//            boolean hasTriggerData = event.getAxisValue(MotionEvent.AXIS_LTRIGGER) != 0f ||
//                    event.getAxisValue(MotionEvent.AXIS_RTRIGGER) != 0f ||
//                    event.getAxisValue(MotionEvent.AXIS_BRAKE) != 0f ||
//                    event.getAxisValue(MotionEvent.AXIS_GAS) != 0f;
//
//            if (hasTriggerData) {
////                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
////                triggerType = (byte) preferences.getInt("trigger_type", TRIGGER_IS_BUTTON);
//
//                if (triggerType == TRIGGER_IS_AXIS) {
////                    Log.d("ExternalController", "triggerType is " + triggerType);
//                    processTriggerButton(event);
//                }
//            }
//
//            int historySize = event.getHistorySize();
//            for (int i = 0; i < historySize; i++) {
//                processJoystickInput(event, i);
//            }
//            processJoystickInput(event, -1);
//            return true;
//        }
//        return false;
//    }

    public boolean updateStateFromMotionEvent(MotionEvent event) {
        if (isJoystickDevice(event)) {
            // Reset triggers to base button state (prevents Key presses being cleared by Motion updates)
            state.triggerL = triggerLPressedViaButton ? 1.0f : 0.0f;
            state.triggerR = triggerRPressedViaButton ? 1.0f : 0.0f;
            
            processTriggerButton(event);
            int historySize = event.getHistorySize();
            for (int i = 0; i < historySize; i++) processJoystickInput(event, i);
            processJoystickInput(event, -1);
            return true;
        }
        return false;
    }


    public boolean updateStateFromKeyEvent(KeyEvent event) {
        boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
        int keyCode = event.getKeyCode();
        int buttonIdx = getButtonIdxByKeyCode(keyCode);

        if (buttonIdx != -1) {
            // Apply button mapping: physical source → target ID
            byte mappedIdx = getMappedButton((byte) buttonIdx);

            // Handle Target: Triggers (Analog or Digital Button mapped to Trigger)
            if (mappedIdx == IDX_BUTTON_L2) {
                triggerLPressedViaButton = pressed;
                state.triggerL = pressed ? 1.0f : 0f;
                return true;
            } else if (mappedIdx == IDX_BUTTON_R2) {
                triggerRPressedViaButton = pressed;
                state.triggerR = pressed ? 1.0f : 0f;
                return true;
            } 
            
            // Handle Target: D-Pad (Mapped from Buttons/Keys)
            else if (mappedIdx >= 12 && mappedIdx <= 15) {
                int dpadIndex = mappedIdx - 12;
                state.dpad[dpadIndex] = pressed;
                return true;
            }
            
            // Handle Target: Regular Buttons
            else {
                state.setPressed(mappedIdx, pressed);
                return true;
            }
        }
        return false;
    }





    public static ArrayList<ExternalController> getControllers() {
        int[] deviceIds = InputDevice.getDeviceIds();
        ArrayList<ExternalController> controllers = new ArrayList<>();
        for (int i = deviceIds.length-1; i >= 0; i--) {
            InputDevice device = InputDevice.getDevice(deviceIds[i]);
            if (isGameController(device)) {
                ExternalController controller = new ExternalController();
                controller.setId(device.getDescriptor());
                controller.setName(device.getName());
                controllers.add(controller);
            }
        }
        return controllers;
    }

    public static ExternalController getController(String id) {
        for (ExternalController controller : getControllers()) if (controller.getId().equals(id)) return controller;
        return null;
    }

    public static ExternalController getController(int deviceId) {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int i = deviceIds.length-1; i >= 0; i--) {
            if (deviceIds[i] == deviceId || deviceId == 0) {
                InputDevice device = InputDevice.getDevice(deviceIds[i]);
                if (isGameController(device)) {
                    ExternalController controller = new ExternalController();
                    controller.setId(device.getDescriptor());
                    controller.setName(device.getName());
                    controller.deviceId = deviceIds[i];
                    return controller;
                }
            }
        }
        return null;
    }

    public static boolean isGameController(InputDevice device) {
        if (device == null) return false;
        int sources = device.getSources();
        // Exclude devices with SOURCE_MOUSE from being considered controllers
        return !device.isVirtual() && ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && (sources & InputDevice.SOURCE_MOUSE) == 0));
    }


    public float getCenteredAxis(MotionEvent event, int axis, int historyPos) {
        if (axis == MotionEvent.AXIS_HAT_X || axis == MotionEvent.AXIS_HAT_Y) {
            float value = event.getAxisValue(axis);
            return Math.abs(value) == 1.0f ? value : 0.0f;
        }

        InputDevice device = event.getDevice();
        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range == null) return 0.0f;

        float flat = range.getFlat();
        float value = historyPos < 0 ? event.getAxisValue(axis) : event.getHistoricalAxisValue(axis, historyPos);

        if (Math.abs(value) <= flat) return 0.0f;

        if (axis == MotionEvent.AXIS_X || axis == MotionEvent.AXIS_Y || axis == MotionEvent.AXIS_Z || axis == MotionEvent.AXIS_RZ) {
             return Math.abs(value) >= ControlElement.STICK_DEAD_ZONE ? value : 0.0f;
        }

        return 0.0f;
    }





    public static boolean isJoystickDevice(MotionEvent event) {
        return (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE;
    }

    public static int getButtonIdxByKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
                return IDX_BUTTON_A;
            case KeyEvent.KEYCODE_BUTTON_B:
                return IDX_BUTTON_B;
            case KeyEvent.KEYCODE_BUTTON_X:
                return IDX_BUTTON_X;
            case KeyEvent.KEYCODE_BUTTON_Y:
                return IDX_BUTTON_Y;
            case KeyEvent.KEYCODE_BUTTON_L1:
                return IDX_BUTTON_L1;
            case KeyEvent.KEYCODE_BUTTON_R1:
                return IDX_BUTTON_R1;
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                return IDX_BUTTON_SELECT;
            case KeyEvent.KEYCODE_BUTTON_START:
                return IDX_BUTTON_START;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                return IDX_BUTTON_L3;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                return IDX_BUTTON_R3;
            case KeyEvent.KEYCODE_BUTTON_L2:
                return IDX_BUTTON_L2;
            case KeyEvent.KEYCODE_BUTTON_R2:
                return IDX_BUTTON_R2;
            case KeyEvent.KEYCODE_DPAD_UP:
                return IDX_BUTTON_UP;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return IDX_BUTTON_RIGHT;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return IDX_BUTTON_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return IDX_BUTTON_LEFT;
            default:
                return -1;
        }
    }

    public static int getButtonIdxByName(String name) {
        switch (name) {
            case "A":
                return IDX_BUTTON_A;
            case "B":
                return IDX_BUTTON_B;
            case "X":
                return IDX_BUTTON_X;
            case "Y":
                return IDX_BUTTON_Y;
            case "L1":
                return IDX_BUTTON_L1;
            case "R1":
                return IDX_BUTTON_R1;
            case "SELECT":
                return IDX_BUTTON_SELECT;
            case "START":
                return IDX_BUTTON_START;
            case "L3":
                return IDX_BUTTON_L3;
            case "R3":
                return IDX_BUTTON_R3;
            case "L2":
                return IDX_BUTTON_L2;
            case "R2":
                return IDX_BUTTON_R2;
            case "UP":
                return IDX_BUTTON_UP;
            case "RIGHT":
                return IDX_BUTTON_RIGHT;
            case "DOWN":
                return IDX_BUTTON_DOWN;
            case "LEFT":
                return IDX_BUTTON_LEFT;
            default:
                return -1;
        }
    }

}
