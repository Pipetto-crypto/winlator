package com.winlator.cmod.xenvironment.components;

import android.content.Context;
import android.os.Process;

import android.util.Log;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.EnvVars;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ProcessHelper;
import com.winlator.cmod.core.Sha256Util;
import com.winlator.cmod.core.TarCompressorUtils;
import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xenvironment.EnvironmentComponent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public class PulseAudioComponent extends EnvironmentComponent {
    private final UnixSocketConfig socketConfig;
    private final Context context;
    private final EnvVars envVars;
    private static int pid = -1;
    private static final Object lock = new Object();

    public PulseAudioComponent(UnixSocketConfig socketConfig, Context context, EnvVars envVars) {
        this.socketConfig = socketConfig;
        this.context = context;
        this.envVars = envVars;
    }

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            pid = execPulseAudio();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
                pid = -1;
            }
        }
    }
    
    private void extractFromAssets(File workingDir) {
        Log.d("PulseAudioComponent", "Extracting PulseAudio assets");
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "pulseaudio.tzst", workingDir);
    }

    private int execPulseAudio() {
        Context context = environment.getContext();
        File workingDir = new File(context.getFilesDir(), "/pulseaudio");
        if (!workingDir.isDirectory()) {
            workingDir.mkdirs();
            FileUtils.chmod(workingDir, 0771);
        }

        File configFile = new File(workingDir, "default.pa");
        FileUtils.writeString(configFile, String.join("\n",
            "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\""+socketConfig.path+"\"",
            "load-module module-oboe-sink api=" + this.envVars.get("PULSEAUDIO_API"),
            "set-default-sink OboeSink"
        ));

        File modulesDir = new File(workingDir, "modules");
        
        EnvVars envVars = new EnvVars();
        envVars.put("LD_LIBRARY_PATH", modulesDir + ":" + workingDir.getAbsolutePath());
        envVars.put("HOME", workingDir);
        envVars.put("TMPDIR", environment.getImageFs().getTmpDir());
        envVars.put("PULSE_LATENCY_MSEC", this.envVars.get("PULSE_LATENCY_MSEC"));
        
        File sumFile = new File(workingDir, ".pulseaudio.sha256sum");
        byte[] sha256Sum = Sha256Util.calculate(context, "pulseaudio.tzst");
        
        if (!sumFile.exists()) {
            extractFromAssets(workingDir);
            Sha256Util.writeToFile(sumFile, sha256Sum);
        }
        else {
            byte[] oldSum = Sha256Util.readFromFile(sumFile);
            if (!Sha256Util.isEqual(oldSum, sha256Sum)) {
                extractFromAssets(workingDir);
                Sha256Util.writeToFile(sumFile, sha256Sum);
            }
        }

        String command = workingDir.getAbsolutePath() + "/pulseaudio";
        command += " --system=false";
        command += " --disable-shm=true";
        command += " --fail=false";
        command += " -n --file=default.pa";
        command += " --daemonize=false";
        command += " --use-pid-file=false";
        command += " --exit-idle-time=-1";

        return ProcessHelper.exec(command, envVars.toStringArray(), workingDir);
    }
}
