package com.winlator.cmod.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.BatteryManager;
import android.content.BroadcastReceiver;
import com.winlator.cmod.R;

import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.StringUtils;
import com.winlator.cmod.core.CPUStatus;

import java.util.HashMap;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FrameRating extends FrameLayout implements Runnable {
    private Context context;
    private long lastTime = 0;
    private int frameCount = 0;
    private float lastFPS = 0;
    private long totalRAM = 0;
    private final TextView tvFPS;
    private final TextView tvRenderer;
    private final TextView tvCPU;
    private final TextView tvRAM;
    private final TextView tvTMP;
    private final TextView tvPOWER;
    private final TextView tvTIME;
    private HashMap graphicsDriverConfig;
    private BroadcastReceiver batteryReceiver;
    private int voltage = 0;
    private final BatteryManager batteryManager;
    private final ActivityManager activityManager;
    private final ActivityManager.MemoryInfo memoryInfo;
    private short maxClockSpeed;
    private final SimpleDateFormat timeFormat;
    private final StringBuilder timeStringBuilder = new StringBuilder(5);

    public FrameRating(Context context, HashMap graphicsDriverConfig) {
        this(context, graphicsDriverConfig ,null);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs) {
        this(context, graphicsDriverConfig, attrs, 0);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.frame_rating, this, false);
        tvFPS = view.findViewById(R.id.TVFPS);
        tvRenderer = view.findViewById(R.id.TVRenderer);
        tvRenderer.setText("OpenGL");
        tvCPU = view.findViewById(R.id.TVCPU);
        tvRAM = view.findViewById(R.id.TVRAM);
        tvPOWER = view.findViewById(R.id.TVPOWER);
        tvTMP = view.findViewById(R.id.TVTMP);
        tvTIME = view.findViewById(R.id.TVTIME);
        this.graphicsDriverConfig = graphicsDriverConfig;
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        totalRAM = memoryInfo.totalMem;
        maxClockSpeed = calculateMaxClockSpeed();
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BatteryLevelReceiver();
        context.registerReceiver(batteryReceiver , batteryFilter);
        addView(view);
    }

    private short calculateMaxClockSpeed() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        short maxSpeed = 0;
        for (int i = 0; i < numProcessors; i++) {
            short cpuMaxSpeed = CPUStatus.getMaxClockSpeed(i);
            if (cpuMaxSpeed > maxSpeed) {
                maxSpeed = cpuMaxSpeed;
            }
        }
        return maxSpeed;
    }

    private float getAvailableRAM() {
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem - memoryInfo.availMem;
    }

    private void readCPUAvalByHPM(){
        short[] clockSpeeds = CPUStatus.getCurrentClockSpeeds();
        int totalClockSpeed = 0;
        int length = clockSpeeds.length;

        for (int i = 0; i < length; i++) {
            totalClockSpeed += clockSpeeds[i];
        }
        int avgClockSpeed = totalClockSpeed / length;
        tvCPU.setText(String.format(Locale.ENGLISH, "%.2f%%", ((float)avgClockSpeed/maxClockSpeed)*100.0f ));
        tvTMP.setText(String.format(Locale.ENGLISH, "%.0f°C", CPUStatus.getCpuTemperature()));
    }


    private class BatteryLevelReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000; //伏特
        }
    }
    
    private String getPower(){
        double current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000000.0;
        double power = voltage * current;
        int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return String.format(Locale.ENGLISH, "%.1fW (%d%%)", power, capacity);
    }

    private void setTimeString(){
        timeStringBuilder.setLength(0);
        timeStringBuilder.append(timeFormat.format(new Date()));
        tvTIME.setText(timeStringBuilder);
    }

    public void setRenderer(String renderer) {
        tvRenderer.setText(renderer);
    }


    public void reset() {
        tvRenderer.setText("OpenGL");
    }

    public void update() {
        if (lastTime == 0) lastTime = SystemClock.elapsedRealtime();
        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float)(frameCount * 1000) / (time - lastTime));
            post(this);
            lastTime = time;
            frameCount = 0;
        }
        frameCount++;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);
        tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        tvRAM.setText(String.format(Locale.ENGLISH, "%.2f%%", (getAvailableRAM()/totalRAM)*100.0f ));
        tvPOWER.setText(getPower());
        readCPUAvalByHPM();
        setTimeString();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        context.unregisterReceiver(batteryReceiver);
    }
}