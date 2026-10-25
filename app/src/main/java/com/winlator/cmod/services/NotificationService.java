package com.winlator.cmod.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.MainActivity;
import com.winlator.cmod.core.ProcessHelper;

public class NotificationService extends Service {
    private static boolean isRunning = false;
	private static boolean isContainerActive = false;
	private BroadcastReceiver screenStateReceiver;
    public static PowerManager.WakeLock wakeLock = null;
	private static volatile SharedPreferences prefs;
	private static final String PREF_USE_WAKELOCK = "enable_background_wakelock";

    public static void acquireLock() {
		if (!isContainerActive || wakeLock == null || prefs == null) return;
		if (!prefs.getBoolean(PREF_USE_WAKELOCK, false)) return;
        if (wakeLock == null || (wakeLock != null && wakeLock.isHeld())) return;

        wakeLock.acquire();
    }

    public static void releaseLock() {
        if (wakeLock == null || (wakeLock != null && !wakeLock.isHeld())) return;

        wakeLock.release();
    }

    public static boolean isRunning() {
        return isRunning;
    }
	public static void setContainerActive(boolean isActive) {
		isContainerActive = isActive;
	}

	private HandlerThread screenReceiverThread;
	private Handler screenReceiverHandler;

	@Override
	public void onCreate() {
		super.onCreate();

		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService::KeepAlive");
		wakeLock.setReferenceCounted(false);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		// Dispatch onReceive() on a dedicated background Looper instead of the main
		// thread, so a synchronous WakeLock Binder call to a contended system_server
		// can never delay the broadcast past the ANR timeout. Also serializes
		// SCREEN_OFF / USER_PRESENT handling in arrival order.
		screenReceiverThread = new HandlerThread("NotificationService-ScreenReceiver");
		screenReceiverThread.start();
		screenReceiverHandler = new Handler(screenReceiverThread.getLooper());

		// Screen-lock detection.
		screenStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    acquireLock();
				} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
					releaseLock();
				}
			}
		};

		IntentFilter screenFilter = new IntentFilter();
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenFilter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(screenStateReceiver, screenFilter, null, screenReceiverHandler);
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
		    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
		    .setOngoing(true);

		Notification notification = builder.build();
		startForeground(MainActivity.NOTIFICATION_ID, notification);
        
        isRunning = true;

		return START_NOT_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		stopForeground(STOP_FOREGROUND_REMOVE);
		stopSelf();
        releaseLock();
        ProcessHelper.killAllWineProcesses();
        isRunning = false;
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseLock();
        isRunning = false;
		if (screenStateReceiver != null) {
			try { unregisterReceiver(screenStateReceiver); } catch (Exception ignored) {}
			screenStateReceiver = null;
		}
		if (screenReceiverThread != null) screenReceiverThread.quitSafely();
    }

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
