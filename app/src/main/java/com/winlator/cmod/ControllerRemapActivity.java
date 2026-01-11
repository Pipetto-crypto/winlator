package com.winlator.cmod;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.cmod.inputcontrols.ExternalController;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual controller remapper - standalone, no profile required.
 * Shows Xbox controller drawn programmatically.
 * Press physical button → highlights mapped visual button.
 * Tap visual button → press physical button to remap.
 */
public class ControllerRemapActivity extends AppCompatActivity {

    private ControllerView controllerView;
    private TextView statusText;
    private int remapTargetButton = -1;
    private int remapTargetAxis = -1;

    // For interactive stick selection: which stick was tapped
    private int awaitingStickSelection = -1;
    private int awaitingStickDirection = -1;

    // Stick positions for visualization
    private float leftStickX = 0, leftStickY = 0;
    private float rightStickX = 0, rightStickY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#1A1A1A"));

        controllerView = new ControllerView(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(20, 80, 20, 20);
        root.addView(controllerView, params);

        statusText = new TextView(this);
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(14);
        statusText.setPadding(32, 20, 32, 20);
        statusText.setBackgroundColor(Color.parseColor("#80000000"));
        statusText.setText("Press physical button to see mapping • Tap button to remap");
        root.addView(statusText);

        // Reset button in top-right corner
        TextView resetBtn = new TextView(this);
        resetBtn.setText("Reset Defaults");
        resetBtn.setTextColor(Color.WHITE);
        resetBtn.setTextSize(12);
        resetBtn.setPadding(16, 8, 16, 8);
        resetBtn.setBackgroundColor(Color.parseColor("#CC333333"));
        resetBtn.setOnClickListener(v -> resetToDefaults());
        FrameLayout.LayoutParams resetParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        resetParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        resetParams.setMargins(0, 16, 16, 0);
        root.addView(resetBtn, resetParams);

        setContentView(root);
        loadMappings();
    }

    private void resetToDefaults() {
        getSharedPreferences("ctrl_map", MODE_PRIVATE).edit().clear().apply();
        ExternalController.buttonMappings.clear();
        for (byte i = 0; i <= 15; i++)
            ExternalController.buttonMappings.put(i, i);
        ExternalController.axisMappings.clear();
        for (byte i = 0; i < 4; i++)
            ExternalController.axisMappings.put(i, i);
        saveMappings();
        Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show();
        statusText.setText("All mappings reset to defaults");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int itemIdx = -1;

        // Map Keys to Internal IDs (Unified)
        itemIdx = ExternalController.getButtonIdxByKeyCode(keyCode);

        if (itemIdx != -1) {
            byte mappedIdx = ExternalController.buttonMappings.getOrDefault((byte) itemIdx, (byte) itemIdx);

            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                // Check if awaiting stick selection (Guard against accidental button mapping to
                // sticks)
                if (awaitingStickSelection != -1) {
                    if (awaitingStickDirection == 4 || awaitingStickDirection == -1) {
                        int target = awaitingStickSelection == 0 ? ExternalController.IDX_BUTTON_L3
                                : ExternalController.IDX_BUTTON_R3;
                        awaitingStickSelection = -1;
                        handleRemap(itemIdx, target);
                        return true;
                    }
                }

                if (remapTargetButton != -1) {
                    handleRemap(itemIdx, remapTargetButton);
                } else {
                    // Update Status Text
                    statusText.setText(getButtonName(itemIdx) + " → " + getButtonName(mappedIdx));

                    // Visualization: Highlight Mapped Target
                    if (mappedIdx >= ExternalController.IDX_BUTTON_UP
                            && mappedIdx <= ExternalController.IDX_BUTTON_LEFT) {
                        // Mapped to D-Pad (0=Up, 1=Right, 2=Down, 3=Left)
                        int dir = mappedIdx - ExternalController.IDX_BUTTON_UP;
                        controllerView.highlightDpad(dir);
                    } else {
                        // Mapped to Button
                        controllerView.highlightButton(mappedIdx);
                    }
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                controllerView.clearHighlight();
                // Ensure D-Pad is also cleared
                controllerView.highlightDpad(-1);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Capture raw stick positions
        float rawLX = event.getAxisValue(MotionEvent.AXIS_X);
        float rawLY = event.getAxisValue(MotionEvent.AXIS_Y);
        float rawRX = event.getAxisValue(MotionEvent.AXIS_Z);
        float rawRY = event.getAxisValue(MotionEvent.AXIS_RZ);

        // Apply Axis Remapping & Inversion for Visualization
        float[] physical = { rawLX, rawLY, rawRX, rawRY };
        float[] output = { 0, 0, 0, 0 };

        for (byte phys = 0; phys < 4; phys++) {
            byte mapped = ExternalController.axisMappings.getOrDefault(phys, phys);
            byte target = (byte) (mapped % 4);
            boolean invert = mapped >= 4;

            float val = physical[phys];
            if (invert)
                val = -val;

            output[target] += val;
        }

        // Clamp values just in case
        for (int i = 0; i < 4; i++)
            output[i] = Math.max(-1f, Math.min(1f, output[i]));

        // Update visual with remapped stick positions
        controllerView.setStickPositions(output[0], output[1], output[2], output[3]);

        // Capture raw values for Interactive Selection (Stick "Tap")
        leftStickX = rawLX;
        leftStickY = rawLY;
        rightStickX = rawRX;
        rightStickY = rawRY;

        // Handle interactive stick selection: detect direction of stick movement
        if (awaitingStickSelection != -1 && awaitingStickDirection != -1) {
            // Detect movement on ANY physical axis to map to the target
            int detectedAxis = -1;
            float detectedVal = 0;

            final int[] AXES_TO_CHECK = { MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z,
                    MotionEvent.AXIS_RZ,
                    MotionEvent.AXIS_RX, MotionEvent.AXIS_RY, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y };

            // Find the axis with the largest deflection
            float maxVal = 0;
            for (int ax : AXES_TO_CHECK) {
                float v = event.getAxisValue(ax);
                if (Math.abs(v) > Math.abs(maxVal)) {
                    maxVal = v;
                    detectedAxis = ax;
                }
            }

            // Wait for significant movement
            if (Math.abs(maxVal) > 0.6f && detectedAxis != -1) {
                detectedVal = maxVal;

                // Which physical axis index is this?

                // We need to convert Android Axis to Internal Physical ID (0-3)
                // 0=LX, 1=LY, 2=RX, 3=RY
                byte finalPhysical = 0;
                if (detectedAxis == MotionEvent.AXIS_X)
                    finalPhysical = 0;
                else if (detectedAxis == MotionEvent.AXIS_Y)
                    finalPhysical = 1;
                else if (detectedAxis == MotionEvent.AXIS_Z)
                    finalPhysical = 2;
                else if (detectedAxis == MotionEvent.AXIS_RZ)
                    finalPhysical = 3;
                else if (detectedAxis == MotionEvent.AXIS_RX)
                    finalPhysical = 2; // Treat RX as Z
                else if (detectedAxis == MotionEvent.AXIS_RY)
                    finalPhysical = 3; // Treat RY as RZ
                else if (detectedAxis == MotionEvent.AXIS_HAT_X)
                    finalPhysical = 0; // Fallback
                else if (detectedAxis == MotionEvent.AXIS_HAT_Y)
                    finalPhysical = 1; // Fallback

                // Target Axis Logic:
                byte targetBase = (byte) (awaitingStickSelection == 0 ? 0 : 2); // 0=Left Visual, 2=Right Visual

                // Direction Logic: 0=Up (Y-), 1=Right (X+), 2=Down (Y+), 3=Left (X-)
                boolean mapToX = (awaitingStickDirection == 1 || awaitingStickDirection == 3);
                byte targetAxis = mapToX ? targetBase : (byte) (targetBase + 1);

                // Check Inversion based on Polarity
                // Expected polarity for this TARGET direction:
                // Up (0) -> Negative, Right (1) -> Positive, Down (2) -> Positive, Left (3) ->
                // Negative
                boolean expectedPositive = (awaitingStickDirection == 1 || awaitingStickDirection == 2);
                boolean isPositive = detectedVal > 0;

                boolean invert = (expectedPositive != isPositive);

                // Apply Offset for Inverted Axis IDs (Base + 4)
                if (invert) {
                    targetAxis += 4;
                }

                handleAxisRemap(finalPhysical, targetAxis);

                awaitingStickSelection = -1;
                awaitingStickDirection = -1;
            }
        }

        // D-pad hat Remapping Visualization & Logic
        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        // Map Hat Inputs to Virtual IDs: 12=Up, 13=Right, 14=Down, 15=Left
        int[] virtualIds = { ExternalController.IDX_BUTTON_UP, ExternalController.IDX_BUTTON_RIGHT,
                ExternalController.IDX_BUTTON_DOWN, ExternalController.IDX_BUTTON_LEFT };
        boolean[] active = { hatY < -0.5f, hatX > 0.5f, hatY > 0.5f, hatX < -0.5f };

        int highlightedDpadDir = -1;
        int highlightedBtn = -1;
        int activeHatId = -1;

        for (int i = 0; i < 4; i++) {
            if (active[i]) {
                activeHatId = virtualIds[i]; // The physical virtual ID pressed (12-15)

                // If we are in Remap Mode (waiting for button press), treat Hat as a button!
                if (remapTargetButton != -1) {
                    handleRemap(activeHatId, remapTargetButton);
                    return true;
                }

                // Visualization Logic
                byte mapped = ExternalController.buttonMappings.getOrDefault((byte) activeHatId, (byte) activeHatId);
                if (mapped >= ExternalController.IDX_BUTTON_UP && mapped <= ExternalController.IDX_BUTTON_LEFT) {
                    highlightedDpadDir = mapped - ExternalController.IDX_BUTTON_UP;
                } else {
                    highlightedBtn = mapped;
                }

                // Update Status Text for feedback
                statusText.setText(getButtonName(activeHatId) + " → " + getButtonName(mapped));

                break;
            }
        }

        if (highlightedDpadDir != -1)
            controllerView.highlightDpad(highlightedDpadDir);
        else
            controllerView.highlightDpad(-1);

        if (highlightedBtn != -1)
            controllerView.highlightButton(highlightedBtn);
        else if (highlightedDpadDir == -1) {
            // Fix: Stuck D-Pad/Button bug.
            // If Hat is released, we MUST clear the button highlight.
            // (Even if it risks minor flicker with Keys, stuck buttons are worse).
            controllerView.highlightButton(-1);
        }

        return true;
    }

    private void handleAxisRemap(byte physicalAxis, byte targetAxis) {
        // Normalize targets (ignore inversion for conflict check)
        byte targetBase = (byte) (targetAxis % 4);

        // Check for conflicts: Is any OTHER physical axis already driving this target?
        byte conflictingAxis = -1;
        for (byte otherPhys = 0; otherPhys < 4; otherPhys++) {
            if (otherPhys == physicalAxis)
                continue;

            // Get current mapping for otherPhys (handling defaults)
            Byte currentMap = ExternalController.axisMappings.get(otherPhys);
            byte mappedVal = (currentMap != null) ? currentMap : otherPhys;
            byte currentTargetBase = (byte) (mappedVal % 4);

            if (currentTargetBase == targetBase) {
                conflictingAxis = otherPhys;
                break;
            }
        }

        if (conflictingAxis != -1) {
            final byte conflictPhys = conflictingAxis;
            new AlertDialog.Builder(this)
                    .setTitle("Stick Conflict")
                    .setMessage(getAxisName(physicalAxis) + " wants to map to " + getAxisName(targetAxis) + ".\n" +
                            "But " + getAxisName(conflictPhys) + " is already driving this stick.\n\n" +
                            "Swap them?")
                    .setPositiveButton("Swap", (d, w) -> {
                        // Capture OLD target of 'physicalAxis'
                        // Must be done BEFORE putting the new mapping
                        Byte oldMapObj = ExternalController.axisMappings.get(physicalAxis);
                        byte oldPhysMap = (oldMapObj != null) ? oldMapObj : physicalAxis;

                        // 1. Map Physical (New) -> Target
                        ExternalController.axisMappings.put(physicalAxis, targetAxis);

                        // 2. Map Conflicting (Old) -> Old Target of Physical
                        ExternalController.axisMappings.put(conflictPhys, oldPhysMap);

                        finishAxisRemap(physicalAxis, targetAxis);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            ExternalController.axisMappings.put(physicalAxis, targetAxis);
            finishAxisRemap(physicalAxis, targetAxis);
        }
    }

    private void finishAxisRemap(byte phys, byte targ) {
        saveMappings();
        remapTargetAxis = -1;
        controllerView.clearRemapTarget();
        statusText.setText("Mapped: " + getAxisName(phys) + " → " + getAxisName(targ));
        Toast.makeText(this, "Axis mapped!", Toast.LENGTH_SHORT).show();
    }

    void startAxisRemapMode(int targetAxis) {
        remapTargetAxis = targetAxis;
        remapTargetButton = -1;
        controllerView.setRemapTarget(-1);
        statusText.setText("Move a stick to map to " + getAxisName((byte) targetAxis));
    }

    private void handleInput(int physicalIdx) {
        if (remapTargetButton != -1) {
            handleRemap(physicalIdx, remapTargetButton);
        } else {
            byte mappedIdx = ExternalController.buttonMappings.getOrDefault((byte) physicalIdx, (byte) physicalIdx);
            controllerView.highlightButton(mappedIdx);
            statusText.setText(getButtonName(physicalIdx) + " → " + getButtonName(mappedIdx));
        }
    }

    private void handleRemap(int physicalIdx, int targetIdx) {
        Byte existingTarget = ExternalController.buttonMappings.get((byte) physicalIdx);

        if (existingTarget != null && existingTarget != targetIdx) {
            new AlertDialog.Builder(this)
                    .setTitle("Conflict")
                    .setMessage(getButtonName(physicalIdx) + " is currently mapped to " + getButtonName(existingTarget)
                            + ". Override with " + getButtonName(targetIdx) + "?")
                    .setPositiveButton("Yes", (d, w) -> applyMapping(physicalIdx, targetIdx))
                    .setNegativeButton("No", null)
                    .show();
        } else {
            applyMapping(physicalIdx, targetIdx);
        }
    }

    private void applyMapping(int physicalIdx, int targetIdx) {
        // Remove existing mappings to this target
        ExternalController.buttonMappings.entrySet().removeIf(e -> e.getValue() == (byte) targetIdx);
        ExternalController.buttonMappings.put((byte) physicalIdx, (byte) targetIdx);

        saveMappings();
        controllerView.clearRemapTarget();
        remapTargetButton = -1;
        controllerView.highlightButton(targetIdx);
        statusText.setText("Mapped: " + getButtonName(physicalIdx) + " → " + getButtonName(targetIdx));
        Toast.makeText(this, "Mapped!", Toast.LENGTH_SHORT).show();
    }

    void startRemapMode(int target) {
        remapTargetButton = target;
        controllerView.setRemapTarget(target);

        // Find what is currently mapped to this target
        StringBuilder current = new StringBuilder();
        for (Map.Entry<Byte, Byte> entry : ExternalController.buttonMappings.entrySet()) {
            if (entry.getValue() == (byte) target) {
                if (current.length() > 0)
                    current.append(", ");
                current.append(getButtonName(entry.getKey()));
            }
        }

        String currentStr = current.length() > 0 ? current.toString() : "None";
        statusText.setText(
                "Currently mapped: " + currentStr + ". Press physical button to map to " + getButtonName(target));
    }

    private void saveMappings() {
        SharedPreferences.Editor editor = getSharedPreferences("ctrl_map", MODE_PRIVATE).edit();

        // Save button mappings
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Byte, Byte> e : ExternalController.buttonMappings.entrySet()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        editor.putString("buttons", sb.toString());

        // Save axis mappings
        StringBuilder ax = new StringBuilder();
        for (Map.Entry<Byte, Byte> e : ExternalController.axisMappings.entrySet()) {
            if (ax.length() > 0)
                ax.append(",");
            ax.append(e.getKey()).append(":").append(e.getValue());
        }
        editor.putString("axes", ax.toString());

        editor.apply();
    }

    private void loadMappings() {
        SharedPreferences prefs = getSharedPreferences("ctrl_map", MODE_PRIVATE);

        // Load button mappings
        String buttons = prefs.getString("buttons", prefs.getString("m", "")); // Fallback to old key
        ExternalController.buttonMappings.clear();
        if (!buttons.isEmpty()) {
            for (String p : buttons.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    try {
                        ExternalController.buttonMappings.put(Byte.parseByte(kv[0]), Byte.parseByte(kv[1]));
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            for (byte i = 0; i <= 15; i++) { // All buttons including L2/R2 and D-Pad
                ExternalController.buttonMappings.put(i, i);
            }
        }

        // Load axis mappings
        String axes = prefs.getString("axes", "");
        ExternalController.axisMappings.clear();
        if (!axes.isEmpty()) {
            for (String p : axes.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    try {
                        ExternalController.axisMappings.put(Byte.parseByte(kv[0]), Byte.parseByte(kv[1]));
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            // Initialize default axis mappings (each axis maps to itself)
            for (byte i = 0; i < 4; i++) {
                ExternalController.axisMappings.put(i, i);
            }
        }
    }

    private String getButtonName(int idx) {
        switch (idx) {
            case ExternalController.IDX_BUTTON_A:
                return "A";
            case ExternalController.IDX_BUTTON_B:
                return "B";
            case ExternalController.IDX_BUTTON_X:
                return "X";
            case ExternalController.IDX_BUTTON_Y:
                return "Y";
            case ExternalController.IDX_BUTTON_L1:
                return "LB";
            case ExternalController.IDX_BUTTON_R1:
                return "RB";
            case ExternalController.IDX_BUTTON_L2:
                return "LT";
            case ExternalController.IDX_BUTTON_R2:
                return "RT";
            case ExternalController.IDX_BUTTON_L3:
                return "LS";
            case ExternalController.IDX_BUTTON_R3:
                return "RS";
            case ExternalController.IDX_BUTTON_SELECT:
                return "Back";
            case ExternalController.IDX_BUTTON_START:
                return "Start";
            case ExternalController.IDX_BUTTON_UP:
                return "D-Pad Up";
            case ExternalController.IDX_BUTTON_RIGHT:
                return "D-Pad Right";
            case ExternalController.IDX_BUTTON_DOWN:
                return "D-Pad Down";
            case ExternalController.IDX_BUTTON_LEFT:
                return "D-Pad Left";
            default:
                return "?";
        }
    }

    private String getAxisName(byte axis) {
        switch (axis) {
            case ExternalController.AXIS_LEFT_X:
                return "Left X";
            case ExternalController.AXIS_LEFT_Y:
                return "Left Y";
            case ExternalController.AXIS_RIGHT_X:
                return "Right X";
            case ExternalController.AXIS_RIGHT_Y:
                return "Right Y";
            case ExternalController.AXIS_LEFT_INVERT_X:
                return "Left X (Inv)";
            case ExternalController.AXIS_LEFT_INVERT_Y:
                return "Left Y (Inv)";
            case ExternalController.AXIS_RIGHT_INVERT_X:
                return "Right X (Inv)";
            case ExternalController.AXIS_RIGHT_INVERT_Y:
                return "Right Y (Inv)";
            default:
                return "Axis " + axis;
        }
    }

    // Inner class: Programmatically drawn controller
    class ControllerView extends View {
        private Paint bodyPaint, buttonPaint, highlightPaint, remapPaint, textPaint, stickPaint, arrowPaint;
        private int highlightedButton = -1;
        private int remapTarget = -1;
        private int highlightedDpad = -1;

        // Stick positions
        private float lsX = 0, lsY = 0, rsX = 0, rsY = 0;
        private float triggerL = 0, triggerR = 0;

        private final Map<Integer, RectF> buttonBounds = new HashMap<>();

        public ControllerView(Context context) {
            super(context);

            // CRITICAL: Enable touch events on this view
            setClickable(true);
            setFocusable(true);

            bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(Color.parseColor("#3D3D3D"));
            bodyPaint.setStyle(Paint.Style.FILL);

            buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            buttonPaint.setColor(Color.parseColor("#555555"));
            buttonPaint.setStyle(Paint.Style.FILL);

            highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setColor(Color.parseColor("#80FF00"));
            highlightPaint.setStyle(Paint.Style.FILL);

            remapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            remapPaint.setColor(Color.parseColor("#FF8800"));
            remapPaint.setStyle(Paint.Style.STROKE);
            remapPaint.setStrokeWidth(4);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextAlign(Paint.Align.CENTER);

            stickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            stickPaint.setColor(Color.parseColor("#2A2A2A"));
            stickPaint.setStyle(Paint.Style.FILL);

            arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(Color.parseColor("#80FF00"));
            arrowPaint.setStyle(Paint.Style.FILL);
        }

        public void setStickPositions(float lx, float ly, float rx, float ry) {
            lsX = lx;
            lsY = ly;
            rsX = rx;
            rsY = ry;
            invalidate();
        }

        public void setTriggerValues(float lt, float rt) {
            triggerL = lt;
            triggerR = rt;
            invalidate();
        }

        public void highlightButton(int idx) {
            highlightedButton = idx;
            invalidate();
        }

        public void clearHighlight() {
            highlightedButton = -1;
            highlightedDpad = -1;
            invalidate();
        }

        public void setRemapTarget(int idx) {
            remapTarget = idx;
            invalidate();
        }

        public void clearRemapTarget() {
            remapTarget = -1;
            invalidate();
        }

        public void highlightDpad(int dir) {
            highlightedDpad = dir;
            invalidate();
        }

        private int draggingStickIdx = -1;
        private float touchOffsetX = 0, touchOffsetY = 0;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    draggingStickIdx = -1;
                    touchOffsetX = 0;
                    touchOffsetY = 0;

                    for (Map.Entry<Integer, RectF> e : buttonBounds.entrySet()) {
                        if (e.getValue().contains(x, y)) {
                            int key = e.getKey();
                            if (key == ExternalController.IDX_BUTTON_L3 || key == ExternalController.IDX_BUTTON_R3) {
                                draggingStickIdx = key;
                                awaitingStickSelection = (key == ExternalController.IDX_BUTTON_L3) ? 0 : 1;
                                awaitingStickDirection = 4; // Default to Center (Tap)
                                highlightButton(key);
                                statusText.setText("Drag to select direction, or Release for L3/R3");
                                return true;
                            } else {
                                awaitingStickSelection = -1;
                                awaitingStickDirection = -1;
                                startRemapMode(key);
                                return true;
                            }
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (draggingStickIdx != -1) {
                        RectF bounds = buttonBounds.get(draggingStickIdx);
                        if (bounds != null) {
                            float cx = bounds.centerX();
                            float cy = bounds.centerY();
                            float maxDist = bounds.width() / 2; // Radius

                            float dx = x - cx;
                            float dy = y - cy;
                            float dist = (float) Math.sqrt(dx * dx + dy * dy);

                            // Clamp visual stick to radius
                            if (dist > maxDist) {
                                float ratio = maxDist / dist;
                                dx *= ratio;
                                dy *= ratio;
                            }

                            // Normalize for visual feedback (-1 to 1)
                            touchOffsetX = dx / (maxDist * 0.4f); // Scale to match drawStick offset logic
                            touchOffsetY = dy / (maxDist * 0.4f);

                            // Determine direction if pulled far enough
                            if (dist > maxDist * 0.3f) {
                                double angle = Math.toDegrees(Math.atan2(dy, dx));
                                if (angle < 0)
                                    angle += 360;

                                if (angle >= 45 && angle < 135) {
                                    awaitingStickDirection = 2; // Down
                                    statusText.setText("Selected: DOWN. Release to confirm.");
                                } else if (angle >= 135 && angle < 225) {
                                    awaitingStickDirection = 3; // Left
                                    statusText.setText("Selected: LEFT. Release to confirm.");
                                } else if (angle >= 225 && angle < 315) {
                                    awaitingStickDirection = 0; // Up
                                    statusText.setText("Selected: UP. Release to confirm.");
                                } else {
                                    awaitingStickDirection = 1; // Right
                                    statusText.setText("Selected: RIGHT. Release to confirm.");
                                }
                            } else {
                                awaitingStickDirection = 4; // Center
                                statusText.setText("Selected: CENTER (L3/R3). Release to confirm.");
                            }
                            invalidate(); // Redraw stick at new position
                        }
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (draggingStickIdx != -1) {
                        // Commit selection
                        if (awaitingStickDirection == 4) {
                            statusText.setText("Press any button to map to "
                                    + (draggingStickIdx == ExternalController.IDX_BUTTON_L3 ? "L3" : "R3"));
                        } else if (awaitingStickDirection != -1) {
                            String dirStr = "";
                            switch (awaitingStickDirection) {
                                case 0:
                                    dirStr = "UP";
                                    break;
                                case 1:
                                    dirStr = "RIGHT";
                                    break;
                                case 2:
                                    dirStr = "DOWN";
                                    break;
                                case 3:
                                    dirStr = "LEFT";
                                    break;
                            }
                            statusText.setText("Press physical stick " + dirStr);
                        }

                        // Reset visual drag
                        draggingStickIdx = -1;
                        touchOffsetX = 0;
                        touchOffsetY = 0;
                        invalidate();
                        return true;
                    }
                    break;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            buttonBounds.clear();

            float w = getWidth();
            float h = getHeight();
            float cx = w / 2;
            float cy = h / 2;
            float scale = Math.min(w / 800f, h / 400f);

            // Draw controller body
            RectF body = new RectF(cx - 350 * scale, cy - 150 * scale, cx + 350 * scale, cy + 150 * scale);
            canvas.drawRoundRect(body, 80 * scale, 80 * scale, bodyPaint);

            // Grips
            Path leftGrip = new Path();
            leftGrip.moveTo(cx - 300 * scale, cy + 100 * scale);
            leftGrip.quadTo(cx - 380 * scale, cy + 200 * scale, cx - 280 * scale, cy + 200 * scale);
            leftGrip.lineTo(cx - 200 * scale, cy + 100 * scale);
            canvas.drawPath(leftGrip, bodyPaint);

            Path rightGrip = new Path();
            rightGrip.moveTo(cx + 300 * scale, cy + 100 * scale);
            rightGrip.quadTo(cx + 380 * scale, cy + 200 * scale, cx + 280 * scale, cy + 200 * scale);
            rightGrip.lineTo(cx + 200 * scale, cy + 100 * scale);
            canvas.drawPath(rightGrip, bodyPaint);

            textPaint.setTextSize(16 * scale);

            // Face buttons (A, B, X, Y)
            drawButton(canvas, cx + 200 * scale, cy + 30 * scale, 25 * scale, ExternalController.IDX_BUTTON_A, "A",
                    Color.parseColor("#4CAF50"));
            drawButton(canvas, cx + 250 * scale, cy - 20 * scale, 25 * scale, ExternalController.IDX_BUTTON_B, "B",
                    Color.parseColor("#F44336"));
            drawButton(canvas, cx + 150 * scale, cy - 20 * scale, 25 * scale, ExternalController.IDX_BUTTON_X, "X",
                    Color.parseColor("#2196F3"));
            drawButton(canvas, cx + 200 * scale, cy - 70 * scale, 25 * scale, ExternalController.IDX_BUTTON_Y, "Y",
                    Color.parseColor("#FFEB3B"));

            // Left stick
            drawStick(canvas, cx - 180 * scale, cy - 30 * scale, 45 * scale, ExternalController.IDX_BUTTON_L3, "LS",
                    true);

            // Right stick
            drawStick(canvas, cx + 80 * scale, cy + 80 * scale, 45 * scale, ExternalController.IDX_BUTTON_R3, "RS",
                    false);

            // D-Pad
            drawDpad(canvas, cx - 180 * scale, cy + 80 * scale, 35 * scale);

            // Shoulders
            drawButton(canvas, cx - 250 * scale, cy - 140 * scale, 40 * scale, 20 * scale,
                    ExternalController.IDX_BUTTON_L1, "LB");
            drawButton(canvas, cx + 250 * scale, cy - 140 * scale, 40 * scale, 20 * scale,
                    ExternalController.IDX_BUTTON_R1, "RB");

            // Triggers
            drawTrigger(canvas, cx - 250 * scale, cy - 170 * scale, 35 * scale, 25 * scale,
                    ExternalController.IDX_BUTTON_L2, "LT");
            drawTrigger(canvas, cx + 250 * scale, cy - 170 * scale, 35 * scale, 25 * scale,
                    ExternalController.IDX_BUTTON_R2, "RT");

            // Start/Back
            drawButton(canvas, cx - 50 * scale, cy - 30 * scale, 20 * scale, ExternalController.IDX_BUTTON_SELECT, "⊞");
            drawButton(canvas, cx + 50 * scale, cy - 30 * scale, 20 * scale, ExternalController.IDX_BUTTON_START, "≡");
        }

        private void drawButton(Canvas c, float x, float y, float r, int idx, String label, int color) {
            RectF bounds = new RectF(x - r, y - r, x + r, y + r);
            buttonBounds.put(idx, bounds);

            Paint p = (highlightedButton == idx) ? highlightPaint : buttonPaint;
            if (highlightedButton != idx) {
                buttonPaint.setColor(color);
            }
            c.drawCircle(x, y, r, p);

            if (remapTarget == idx) {
                c.drawCircle(x, y, r + 4, remapPaint);
            }

            textPaint.setColor(Color.WHITE);
            c.drawText(label, x, y + textPaint.getTextSize() / 3, textPaint);
            buttonPaint.setColor(Color.parseColor("#555555"));
        }

        private void drawButton(Canvas c, float x, float y, float r, int idx, String label) {
            drawButton(c, x, y, r, idx, label, Color.parseColor("#555555"));
        }

        private void drawButton(Canvas c, float x, float y, float w, float h, int idx, String label) {
            RectF bounds = new RectF(x - w, y - h, x + w, y + h);
            buttonBounds.put(idx, bounds);

            Paint p = (highlightedButton == idx) ? highlightPaint : buttonPaint;
            c.drawRoundRect(bounds, 10, 10, p);

            if (remapTarget == idx)
                c.drawRoundRect(bounds, 10, 10, remapPaint);

            c.drawText(label, x, y + textPaint.getTextSize() / 3, textPaint);
        }

        private void drawTrigger(Canvas c, float x, float y, float w, float h, int idx, String label) {
            RectF bounds = new RectF(x - w, y - h, x + w, y + h);
            buttonBounds.put(idx, bounds);

            Paint p = (highlightedButton == idx) ? highlightPaint : buttonPaint;
            c.drawRoundRect(bounds, 8, 8, p);

            if (remapTarget == idx)
                c.drawRoundRect(bounds, 8, 8, remapPaint);

            c.drawText(label, x, y + textPaint.getTextSize() / 3, textPaint);
        }

        private void drawDpad(Canvas c, float x, float y, float size) {
            // Dpad cross shape
            Paint dpadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dpadPaint.setColor(Color.parseColor("#444444"));

            // Calculate rects for drawing and hit testing
            RectF upRect = new RectF(x - size / 3, y - size, x + size / 3, y - size / 3);
            RectF downRect = new RectF(x - size / 3, y + size / 3, x + size / 3, y + size);
            RectF leftRect = new RectF(x - size, y - size / 3, x - size / 3, y + size / 3);
            RectF rightRect = new RectF(x + size / 3, y - size / 3, x + size, y + size / 3);

            // Register bounds for hit testing (Virtual IDs 12-15)
            buttonBounds.put(12, upRect);
            buttonBounds.put(14, downRect);
            buttonBounds.put(15, leftRect);
            buttonBounds.put(13, rightRect);

            // Draw Base
            c.drawRect(x - size / 3, y - size, x + size / 3, y + size, dpadPaint);
            c.drawRect(x - size, y - size / 3, x + size, y + size / 3, dpadPaint);

            // Highlight directions
            Paint dirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            // Up
            dirPaint.setColor((highlightedDpad == 0 || highlightedButton == 12) ? highlightPaint.getColor()
                    : Color.parseColor("#444444"));
            if (remapTarget == 12) {
                dirPaint.setStyle(Paint.Style.STROKE);
                dirPaint.setColor(remapPaint.getColor());
                dirPaint.setStrokeWidth(4);
                c.drawRect(upRect, dirPaint);
                dirPaint.setStyle(Paint.Style.FILL);
            } else if (highlightedDpad == 0 || highlightedButton == 12)
                c.drawRect(upRect, dirPaint);

            // Down
            dirPaint.setColor((highlightedDpad == 2 || highlightedButton == 14) ? highlightPaint.getColor()
                    : Color.parseColor("#444444"));
            if (remapTarget == 14) {
                dirPaint.setStyle(Paint.Style.STROKE);
                dirPaint.setColor(remapPaint.getColor());
                dirPaint.setStrokeWidth(4);
                c.drawRect(downRect, dirPaint);
                dirPaint.setStyle(Paint.Style.FILL);
            } else if (highlightedDpad == 2 || highlightedButton == 14)
                c.drawRect(downRect, dirPaint);

            // Left
            dirPaint.setColor((highlightedDpad == 3 || highlightedButton == 15) ? highlightPaint.getColor()
                    : Color.parseColor("#444444"));
            if (remapTarget == 15) {
                dirPaint.setStyle(Paint.Style.STROKE);
                dirPaint.setColor(remapPaint.getColor());
                dirPaint.setStrokeWidth(4);
                c.drawRect(leftRect, dirPaint);
                dirPaint.setStyle(Paint.Style.FILL);
            } else if (highlightedDpad == 3 || highlightedButton == 15)
                c.drawRect(leftRect, dirPaint);

            // Right
            dirPaint.setColor((highlightedDpad == 1 || highlightedButton == 13) ? highlightPaint.getColor()
                    : Color.parseColor("#444444"));
            if (remapTarget == 13) {
                dirPaint.setStyle(Paint.Style.STROKE);
                dirPaint.setColor(remapPaint.getColor());
                dirPaint.setStrokeWidth(4);
                c.drawRect(rightRect, dirPaint);
                dirPaint.setStyle(Paint.Style.FILL);
            } else if (highlightedDpad == 1 || highlightedButton == 13)
                c.drawRect(rightRect, dirPaint);
        }

        private void drawStick(Canvas c, float x, float y, float r, int idx, String label, boolean isLeft) {
            // Make touch area 50% larger than visual for easier tapping
            float touchR = r * 1.5f;
            RectF bounds = new RectF(x - touchR, y - touchR, x + touchR, y + touchR);
            buttonBounds.put(idx, bounds);

            // Draw outer ring
            c.drawCircle(x, y, r, stickPaint);

            // Get stick position
            float stickX = isLeft ? lsX : rsX;
            float stickY = isLeft ? lsY : rsY;

            // Override with touch drag if applicable
            if (draggingStickIdx == idx) {
                // Determine normalized X/Y from drag logic?
                // Actually we stored normalized offset in touchOffsetX/Y relative to
                // maxDist*0.4
                // But simplified: we just want to draw the stick CAP where the finger is.
                // Re-calculating:
                // We stored normalized visually-scaled offsets in touchOffsetX/Y
                // Let's us them if dragging
                stickX = touchOffsetX;
                stickY = touchOffsetY;
            }

            // Calculate inner stick position (offset by stick input)
            float offsetX = stickX * r * 0.4f;
            float offsetY = stickY * r * 0.4f;

            Paint p = (highlightedButton == idx) ? highlightPaint : buttonPaint;

            // If stick is moved, show green
            if (Math.abs(stickX) > 0.2f || Math.abs(stickY) > 0.2f) {
                p = arrowPaint;
            }

            c.drawCircle(x + offsetX, y + offsetY, r * 0.6f, p);

            if (remapTarget == idx)
                c.drawCircle(x, y, r, remapPaint);

            textPaint.setTextSize(12 * Math.min(getWidth() / 800f, getHeight() / 400f));
            c.drawText(label, x + offsetX, y + offsetY + textPaint.getTextSize() / 3, textPaint);
            textPaint.setTextSize(16 * Math.min(getWidth() / 800f, getHeight() / 400f));
        }
    }
}
