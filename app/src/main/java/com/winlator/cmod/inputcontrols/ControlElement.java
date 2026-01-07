package com.winlator.cmod.inputcontrols;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;

import androidx.core.graphics.ColorUtils;

import com.winlator.cmod.math.Mathf;
import com.winlator.cmod.widget.InputControlsView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class ControlElement {
    public static final float STICK_DEAD_ZONE = 0.15f;
    public static final float DPAD_DEAD_ZONE = 0.3f;
    public static final float STICK_SENSITIVITY = 3.0f;
    public static final float TRACKPAD_MIN_SPEED = 0.8f;
    public static final float TRACKPAD_MAX_SPEED = 20.0f;
    public static final byte TRACKPAD_ACCELERATION_THRESHOLD = 4;
    public static final short BUTTON_MIN_TIME_TO_KEEP_PRESSED = 50;

    // --- OTIMIZAÇÃO: Limitador de FPS Visual (Evita Crash) ---
    private long lastInvalidateTime = 0;
    private static final long REFRESH_INTERVAL_MS = 32; 
    // --------------------------------------------------------

    // OTIMIZAÇÃO: Objetos reutilizáveis
    private final Rect iconRect = new Rect();
    private final RectF roundRectF = new RectF(); 

    public enum Type {
        BUTTON, STICK, TRACKPAD, D_PAD, RANGE_BUTTON;

        public static String[] names() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }
    }

    public enum Shape {
        CIRCLE, RECT, ROUND_RECT, SQUARE; // ADICIONADO: SQUARE para corrigir o novo crash

        public static String[] names() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }
    }

    public enum Range {
        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10);

        public final int max;

        Range(int max) {
            this.max = max;
        }

        public static String[] names() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }
    }

    private final InputControlsView inputControlsView;
    private int id;
    private Type type = Type.BUTTON;
    private String text = "";
    private Binding[] bindings;
    private int x;
    private int y;
    private float scale = 1.0f;
    private boolean selected = false;
    private int currentPointerId = -1;
    private final boolean[] states = new boolean[4];
    private final Rect boundingBox = new Rect();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private PointF currentPosition; 
    private Scroller scroller;

    private Shape shape = Shape.CIRCLE;
    private boolean toggleSwitch = false;
    private int iconId = 0;
    private Range range = Range.FROM_A_TO_Z; 
    private byte orientation = 0; 

    public ControlElement(InputControlsView inputControlsView) {
        this.inputControlsView = inputControlsView;
        safeInvalidate(true);
    }

    private void safeInvalidate(boolean force) {
        long now = SystemClock.uptimeMillis();
        if (force || (now - lastInvalidateTime >= REFRESH_INTERVAL_MS)) {
            inputControlsView.invalidate();
            lastInvalidateTime = now;
        }
    }

    public void setType(Type type) {
        this.type = type;
        if (type == Type.RANGE_BUTTON) scroller = new Scroller();
        createBindings();
    }

    public Type getType() { return type; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public void setShape(Shape shape) { this.shape = shape; }
    public Shape getShape() { return shape; }

    public void setToggleSwitch(boolean toggleSwitch) { this.toggleSwitch = toggleSwitch; }
    public boolean isToggleSwitch() { return toggleSwitch; }

    public void setIconId(int iconId) { this.iconId = iconId; }
    public byte getIconId() { return (byte)iconId; }

    public void setRange(Range range) { this.range = range; }
    public Range getRange() { return range; }

    public void setOrientation(byte orientation) { this.orientation = orientation; }
    public byte getOrientation() { return orientation; }

    public void setBindingCount(int count) {
        if (bindings == null || bindings.length != count) {
            bindings = new Binding[count];
            Arrays.fill(bindings, Binding.NONE);
        }
    }

    public int getBindingCount() {
        return bindings != null ? bindings.length : 0;
    }

    public void setBinding(Binding binding) {
        if (bindings != null && bindings.length > 0) {
            bindings[0] = binding;
        }
    }

    public void setBindingAt(int index, Binding binding) {
        if (index < bindings.length) bindings[index] = binding;
    }

    public Binding getBindingAt(int index) {
        return index < bindings.length ? bindings[index] : Binding.NONE;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) {
        this.selected = selected;
        safeInvalidate(true); 
    }

    private void createBindings() {
        switch (type) {
            case BUTTON:
            case RANGE_BUTTON:
                bindings = new Binding[1];
                bindings[0] = Binding.NONE;
                break;
            case STICK:
            case D_PAD:
            case TRACKPAD:
                bindings = new Binding[4];
                Arrays.fill(bindings, Binding.NONE);
                break;
        }
    }

    public Rect getBoundingBox() { return boundingBox; }

    public boolean containsPoint(float x, float y) {
        return x >= boundingBox.left && x < boundingBox.right && y >= boundingBox.top && y < boundingBox.bottom;
    }

    public void draw(Canvas canvas) {
        int snappingSize = inputControlsView.getSnappingSize();
        int size = (int)(snappingSize * (type == Type.STICK || type == Type.TRACKPAD || type == Type.D_PAD ? 4.5f : 2.0f) * scale);
        
        if (type == Type.RANGE_BUTTON) {
            int width = (int)(snappingSize * 2.5f * scale);
            int height = (int)(snappingSize * 5.0f * scale);
            boundingBox.set(x - width / 2, y - height / 2, x + width / 2, y + height / 2);
        } else {
            boundingBox.set(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtils.setAlphaComponent(selected ? inputControlsView.getSecondaryColor() : inputControlsView.getPrimaryColor(), isEngaged() ? 160 : 64));

        float radius = Math.min(boundingBox.width(), boundingBox.height()) * 0.5f;

        if (type == Type.STICK || type == Type.TRACKPAD) {
            canvas.drawCircle(boundingBox.centerX(), boundingBox.centerY(), radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(selected ? inputControlsView.getSecondaryColor() : inputControlsView.getPrimaryColor());
            canvas.drawCircle(boundingBox.centerX(), boundingBox.centerY(), radius, paint);

            float knobX = boundingBox.centerX();
            float knobY = boundingBox.centerY();

            if (currentPosition != null) {
                knobX = currentPosition.x;
                knobY = currentPosition.y;
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(inputControlsView.getPrimaryColor());
            canvas.drawCircle(knobX, knobY, radius * 0.4f, paint);

        } else if (type == Type.D_PAD) {
            float dpadSize = boundingBox.width();
            float third = dpadSize / 3;
            
            canvas.drawRect(boundingBox.left + third, boundingBox.top, boundingBox.right - third, boundingBox.bottom, paint);
            canvas.drawRect(boundingBox.left, boundingBox.top + third, boundingBox.right, boundingBox.bottom - third, paint);
            
            paint.setColor(ColorUtils.setAlphaComponent(inputControlsView.getSecondaryColor(), 200));
            if (states[0]) canvas.drawRect(boundingBox.left + third, boundingBox.top, boundingBox.right - third, boundingBox.top + third, paint); 
            if (states[1]) canvas.drawRect(boundingBox.right - third, boundingBox.top + third, boundingBox.right, boundingBox.bottom - third, paint); 
            if (states[2]) canvas.drawRect(boundingBox.left + third, boundingBox.bottom - third, boundingBox.right - third, boundingBox.bottom, paint); 
            if (states[3]) canvas.drawRect(boundingBox.left, boundingBox.top + third, boundingBox.left + third, boundingBox.bottom - third, paint); 

        } else if (type == Type.RANGE_BUTTON) {
            if (scroller != null) {
                scroller.draw(canvas, boundingBox.centerX(), boundingBox.centerY(), boundingBox.width(), boundingBox.height());
            }
        } else { // BUTTON
            // CORREÇÃO: Tratamento para SQUARE (usa lógica do RECT)
            if (shape == Shape.RECT || shape == Shape.SQUARE) {
                canvas.drawRect(boundingBox, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(selected ? inputControlsView.getSecondaryColor() : inputControlsView.getPrimaryColor());
                canvas.drawRect(boundingBox, paint);
            } else if (shape == Shape.ROUND_RECT) {
                roundRectF.set(boundingBox);
                float cornerRadius = Math.min(boundingBox.width(), boundingBox.height()) * 0.25f;
                canvas.drawRoundRect(roundRectF, cornerRadius, cornerRadius, paint);
                
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(selected ? inputControlsView.getSecondaryColor() : inputControlsView.getPrimaryColor());
                canvas.drawRoundRect(roundRectF, cornerRadius, cornerRadius, paint);
            } else { // CIRCLE (padrão)
                canvas.drawCircle(boundingBox.centerX(), boundingBox.centerY(), radius, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                paint.setColor(selected ? inputControlsView.getSecondaryColor() : inputControlsView.getPrimaryColor());
                canvas.drawCircle(boundingBox.centerX(), boundingBox.centerY(), radius, paint);
            }

            if (bindings[0] != Binding.NONE) {
                if (text.isEmpty()) {
                     int idToDraw = (iconId > 0) ? iconId : (int)bindings[0].ordinal();
                     Bitmap icon = inputControlsView.getIcon((byte)idToDraw);
                     
                     if (icon != null) {
                         iconRect.set(
                             (int)(boundingBox.centerX() - radius * 0.5f), 
                             (int)(boundingBox.centerY() - radius * 0.5f), 
                             (int)(boundingBox.centerX() + radius * 0.5f), 
                             (int)(boundingBox.centerY() + radius * 0.5f)
                         );
                         canvas.drawBitmap(icon, null, iconRect, paint);
                     }
                } else {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(radius * 0.8f);
                    float textHeight = paint.descent() - paint.ascent();
                    float textOffset = (textHeight / 2) - paint.descent();
                    canvas.drawText(text, boundingBox.centerX(), boundingBox.centerY() + textOffset, paint);
                }
            }
        }
    }

    public boolean handleTouchDown(int pointerId, float x, float y) {
        if (currentPointerId == -1 && containsPoint(x, y)) {
            currentPointerId = pointerId;
            
            if (type == Type.BUTTON && toggleSwitch) {
                boolean newState = !states[0];
                states[0] = newState;
                inputControlsView.handleInputEvent(getBindingAt(0), newState);
                safeInvalidate(true);
                return true;
            }

            updateState(x, y);
            safeInvalidate(true); 
            return true;
        }
        return false;
    }

    public boolean handleTouchMove(int pointerId, float x, float y) {
        if (currentPointerId == pointerId) {
            if (type == Type.BUTTON && toggleSwitch) return true;

            updateState(x, y);
            if (type == Type.STICK || type == Type.RANGE_BUTTON || type == Type.TRACKPAD) {
                 safeInvalidate(false); 
            }
            return true;
        }
        return false;
    }

    public boolean handleTouchUp(int pointerId) {
        if (currentPointerId == pointerId) {
            if (type == Type.BUTTON && toggleSwitch) {
                currentPointerId = -1;
                return true;
            }

            if (type == Type.BUTTON) {
                inputControlsView.handleInputEvent(getBindingAt(0), false);
                safeInvalidate(true);
            }
            else if (type == Type.RANGE_BUTTON || type == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD) {
                for (byte i = 0; i < states.length; i++) {
                    if (states[i]) inputControlsView.handleInputEvent(getBindingAt(i), false);
                    states[i] = false;
                }
                if (type == Type.RANGE_BUTTON && scroller != null) scroller.handleTouchUp();
                else if (type == Type.STICK) safeInvalidate(true);
                if (currentPosition != null) currentPosition = null;
            }
            currentPointerId = -1;
            safeInvalidate(true);
            return true;
        }
        return false;
    }

    private void updateState(float x, float y) {
        if (type == Type.BUTTON) {
            if (!toggleSwitch) {
                inputControlsView.handleInputEvent(getBindingAt(0), true);
            }
        }
        else if (type == Type.RANGE_BUTTON) {
            if (scroller != null) {
                scroller.handleTouchMove(x, y, boundingBox);
                boolean pressed = scroller.getValue() > 0.5f;
                if (pressed != states[0]) {
                    states[0] = pressed;
                    inputControlsView.handleInputEvent(getBindingAt(0), pressed);
                }
            }
        }
        else if (type == Type.D_PAD) {
            float dx = x - boundingBox.centerX();
            float dy = y - boundingBox.centerY();
            float deadZone = boundingBox.width() * 0.5f * DPAD_DEAD_ZONE;

            boolean[] newStates = new boolean[4];
            if (Math.abs(dy) > deadZone && Math.abs(dy) > Math.abs(dx)) {
                if (dy < 0) newStates[0] = true; 
                else newStates[2] = true; 
            }
            else if (Math.abs(dx) > deadZone && Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) newStates[1] = true; 
                else newStates[3] = true; 
            }

            for (byte i = 0; i < 4; i++) {
                if (newStates[i] != states[i]) {
                    states[i] = newStates[i];
                    inputControlsView.handleInputEvent(getBindingAt(i), states[i]);
                }
            }
        }
        else if (type == Type.STICK) {
            updateStick(x, y);
        }
    }

    private void updateStick(float x, float y) {
        float maxRadius = boundingBox.width() * 0.5f;
        float dx = x - boundingBox.centerX();
        float dy = y - boundingBox.centerY();
        float distance = (float)Math.sqrt(dx * dx + dy * dy);

        if (distance > maxRadius) {
            float scale = maxRadius / distance;
            dx *= scale;
            dy *= scale;
        }
        
        setCurrentPosition(boundingBox.centerX() + dx, boundingBox.centerY() + dy);

        float axisX = dx / maxRadius;
        float axisY = dy / maxRadius;

        if (Math.abs(axisX) < STICK_DEAD_ZONE) axisX = 0;
        if (Math.abs(axisY) < STICK_DEAD_ZONE) axisY = 0;

        inputControlsView.handleInputEvent(getBindingAt(1), axisX > 0, Math.abs(axisX)); 
        inputControlsView.handleInputEvent(getBindingAt(3), axisX < 0, Math.abs(axisX)); 
        inputControlsView.handleInputEvent(getBindingAt(2), axisY > 0, Math.abs(axisY)); 
        inputControlsView.handleInputEvent(getBindingAt(0), axisY < 0, Math.abs(axisY)); 
    }

    public PointF getCurrentPosition() {
        if (currentPosition == null) currentPosition = new PointF(x, y); 
        return currentPosition;
    }

    public void setCurrentPosition(float x, float y) {
        if (currentPosition == null) currentPosition = new PointF();
        currentPosition.set(x, y);
    }

    private boolean isEngaged() {
        if (type == Type.BUTTON || type == Type.RANGE_BUTTON) {
            if (toggleSwitch && type == Type.BUTTON) return states[0]; 
            return currentPointerId != -1 || selected;
        }
        for (boolean b : states) if (b) return true;
        return selected;
    }
    
    private class Scroller {
        float value = 0;
        void handleTouchMove(float x, float y, Rect bounds) {
            float relativeY = y - bounds.top;
            value = Mathf.clamp(relativeY / bounds.height(), 0, 1);
        }
        void handleTouchUp() { value = 0; }
        float getValue() { return value; }
        void draw(Canvas canvas, float cx, float cy, float w, float h) {
            canvas.drawRect(cx - w/2, cy - h/2, cx + w/2, cy + h/2, paint);
            paint.setColor(ColorUtils.setAlphaComponent(inputControlsView.getSecondaryColor(), 200));
            float fillH = h * value;
            canvas.drawRect(cx - w/2, cy - h/2, cx + w/2, (cy - h/2) + fillH, paint);
        }
    }
    
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("type", type.name());
        jsonObject.put("text", text);
        jsonObject.put("x", x);
        jsonObject.put("y", y);
        jsonObject.put("scale", (double)scale);
        
        if (shape != Shape.CIRCLE) jsonObject.put("shape", shape.name());
        if (toggleSwitch) jsonObject.put("toggleSwitch", true);
        if (iconId > 0) jsonObject.put("iconId", iconId);
        if (range != null) jsonObject.put("range", range.name());
        if (orientation != 0) jsonObject.put("orientation", (int)orientation);

        JSONArray bindingsJson = new JSONArray();
        for (Binding binding : bindings) bindingsJson.put(binding.name());
        jsonObject.put("bindings", bindingsJson);
        return jsonObject;
    }

    public static ControlElement fromJSONObject(JSONObject jsonObject, InputControlsView view) throws JSONException {
        ControlElement element = new ControlElement(view);
        element.id = jsonObject.getInt("id");
        element.type = Type.valueOf(jsonObject.getString("type"));
        element.text = jsonObject.optString("text", "");
        element.x = jsonObject.getInt("x");
        element.y = jsonObject.getInt("y");
        element.scale = (float)jsonObject.getDouble("scale");

 if (jsonObject.has("shape")) element.shape = Shape.valueOf(jsonObject.getString("shape"));
        if (jsonObject.has("toggleSwitch")) element.toggleSwitch = jsonObject.getBoolean("toggleSwitch");
        if (jsonObject.has("iconId")) element.iconId = jsonObject.getInt("iconId");
        if (jsonObject.has("range")) element.range = Range.valueOf(jsonObject.getString("range"));
        if (jsonObject.has("orientation")) element.orientation = (byte)jsonObject.getInt("orientation");

        element.createBindings();
        JSONArray bindingsJson = jsonObject.getJSONArray("bindings");
        for (int i = 0; i < bindingsJson.length(); i++) {
            element.setBindingAt(i, Binding.valueOf(bindingsJson.getString(i)));
        }
        if (element.type == Type.RANGE_BUTTON) element.scroller = element.new Scroller();
        return element;
    }
}