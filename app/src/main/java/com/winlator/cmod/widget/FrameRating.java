package com.winlator.cmod.widget;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.winlator.cmod.R;
import com.winlator.cmod.core.GPUInformation;
import com.winlator.cmod.core.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Locale;

public class FrameRating extends FrameLayout implements Runnable {
    private Context context;
    private long lastTime = 0;
    private int frameCount = 0;
    private float lastFPS = 0;
    private String totalRAM = null;
    private final TextView tvFPS;
    private final TextView tvFPSLabel;
    private final TextView tvRenderer;
    private final TextView tvGPU;
    private final TextView tvRAM;
    private final TextView tvCPU;
    private final TextView tvCPUUsage;

    private HashMap graphicsDriverConfig;

    // CPU usage tracking
    private long lastCpuTotal = 0;
    private long lastCpuIdle = 0;

    public FrameRating(Context context, HashMap graphicsDriverConfig) {
        this(context, graphicsDriverConfig, null);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs) {
        this(context, graphicsDriverConfig, attrs, 0);
    }

    public FrameRating(Context context, HashMap graphicsDriverConfig, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.graphicsDriverConfig = graphicsDriverConfig;

        View view = LayoutInflater.from(context).inflate(R.layout.frame_rating, this, false);

        tvFPS = view.findViewById(R.id.TVFPS);
        tvFPSLabel = view.findViewById(R.id.TVFPSLabel);
        tvRenderer = view.findViewById(R.id.TVRenderer);
        tvGPU = view.findViewById(R.id.TVGPU);
        tvRAM = view.findViewById(R.id.TVRAM);
        tvCPU = view.findViewById(R.id.TVCPU);
        tvCPUUsage = view.findViewById(R.id.TVCPUUsage);


        // Set initial values
        tvRenderer.setText("OpenGL");
        tvGPU.setText(GPUInformation.getRenderer(graphicsDriverConfig.get("version").toString(), context));
        tvCPU.setText(getCPUModel());
        totalRAM = getTotalRAM();

        addView(view);

        // تقليل الشفافية لعدم تغطية المحتوى
        view.setAlpha(0.75f);
    }

    private String getCPUModel() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Hardware") || line.startsWith("model name")) {
                    br.close();
                    return line.split(":")[1].trim();
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown CPU";
    }

    private float getCPUUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String line = reader.readLine();
            reader.close();

            String[] parts = line.split("\\s+");
            long idle = Long.parseLong(parts[4]);
            long total = 0;
            for (int i = 1; i < parts.length; i++) {
                total += Long.parseLong(parts[i]);
            }

            long diffTotal = total - lastCpuTotal;
            long diffIdle = idle - lastCpuIdle;
            lastCpuTotal = total;
            lastCpuIdle = idle;

            if (diffTotal == 0) return 0;
            return ((float)(diffTotal - diffIdle) / diffTotal) * 100f;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCPUFreqMHz() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r");
            String line = reader.readLine();
            reader.close();
            return Integer.parseInt(line.trim()) / 1000;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCPUCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    private String getTotalRAM() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return StringUtils.formatBytes(memoryInfo.totalMem);
    }

    private String getUsedRAM() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long usedMem = memoryInfo.totalMem - memoryInfo.availMem;
        return StringUtils.formatBytes(usedMem, false);
    }

    private void updateFPSColor(float fps) {
        if (fps >= 50) {
            tvFPS.setTextColor(Color.parseColor("#00E676"));       // Green
            tvFPSLabel.setTextColor(Color.parseColor("#00E676"));
        } else if (fps >= 30) {
            tvFPS.setTextColor(Color.parseColor("#FFEB3B"));       // Yellow
            tvFPSLabel.setTextColor(Color.parseColor("#FFEB3B"));
        } else {
            tvFPS.setTextColor(Color.parseColor("#FF1744"));       // Red
            tvFPSLabel.setTextColor(Color.parseColor("#FF1744"));
        }
    }

    private void updateCPUColor(float usage) {
        if (usage < 50) {
            tvCPUUsage.setTextColor(Color.parseColor("#00E676"));  // Green
        } else if (usage < 80) {
            tvCPUUsage.setTextColor(Color.parseColor("#FFEB3B"));  // Yellow
        } else {
            tvCPUUsage.setTextColor(Color.parseColor("#FF1744"));  // Red
        }
    }

    public void setRenderer(String renderer) {
        tvRenderer.setText(renderer);
    }

    public void setGpuName(String gpuName) {
        tvGPU.setText(gpuName);
    }

    public void reset() {
        tvRenderer.setText("OpenGL");
        tvGPU.setText(GPUInformation.getRenderer(graphicsDriverConfig.get("version").toString(), context));
    }

    public void update() {
        if (lastTime == 0) lastTime = SystemClock.elapsedRealtime();
        long time = SystemClock.elapsedRealtime();
        if (time >= lastTime + 500) {
            lastFPS = ((float) (frameCount * 1000) / (time - lastTime));
            post(this);
            lastTime = time;
            frameCount = 0;
        }
        frameCount++;
    }

    @Override
    public void run() {
        if (getVisibility() == GONE) setVisibility(View.VISIBLE);

        // FPS
        tvFPS.setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));
        updateFPSColor(lastFPS);

        // CPU
        float cpuUsage = getCPUUsage();
        int cpuFreq = getCPUFreqMHz();
        int cores = getCPUCoreCount();
        tvCPUUsage.setText(String.format(Locale.ENGLISH, "%.0f%% | %d MHz | %d Cores", cpuUsage, cpuFreq, cores));
        updateCPUColor(cpuUsage);



        // RAM
        tvRAM.setText(getUsedRAM() + " / " + totalRAM);
    }
}

