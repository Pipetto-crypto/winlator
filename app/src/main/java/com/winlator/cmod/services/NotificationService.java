package com.winlator.cmod.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import androidx.core.content.ContextCompat;
import com.winlator.cmod.R;
import com.winlator.cmod.MainActivity;

public class NotificationService extends Service {
    private static boolean isRunning = false;
    public static PowerManager.WakeLock wakeLock = null;
    
    public static boolean isRunning() {
        return isRunning;
    }
    
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {	
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_ab_gear_0011)
			.setContentTitle("Winlator")
			.setContentText("Winlator is running, do not kill or swipe this notification")
			.setPriority(NotificationCompat.PRIORITY_LOW)
		 	.setContentIntent(pendingIntent)
		 	.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
		 	.setOngoing(true);
		 
		Notification notification = builder.build();
		startForeground(MainActivity.NOTIFICATION_ID, notification);
        
        isRunning = true;
        
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService::KeepAlive");

		return START_NOT_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		stopForeground(STOP_FOREGROUND_REMOVE);
		stopSelf();
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
