package com.winlator.cmod.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.winlator.cmod.R;
import com.winlator.cmod.MainActivity;

public class NotificationService extends Service {
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_ab_gear_0011)
			.setContentTitle("Winlator")
			.setContentText("Winlator is running, do not kill or swipe this notification")
			.setPriority(NotificationCompat.PRIORITY_LOW)
		 	.setContentIntent(pendingIntent)
		 	.setOngoing(true);
		 
		Notification notification = builder.build();
		startForeground(MainActivity.NOTIFICATION_ID, notification);

		return START_NOT_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		stopForeground(STOP_FOREGROUND_REMOVE);
		stopSelf();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
