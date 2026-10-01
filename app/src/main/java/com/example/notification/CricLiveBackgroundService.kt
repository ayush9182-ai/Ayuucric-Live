package com.example.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Sticky Foreground Service that keeps ground sync active in the background.
 * Ensures the Android OS does not kill the Firestore listeners when the app
 * is minimized, locked, or closed from the recent apps drawer.
 */
class CricLiveBackgroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CricLiveBackgroundService created")
        createServiceNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CricLiveBackgroundService started")

        // 1. Post low-profile persistent notification to keep service alive
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "startForeground error: ${e.message}")
        }

        // 2. Start / Ensure background real-time sync listeners are active
        try {
            AyuuBackgroundSyncManager.start(applicationContext)
        } catch (e: Throwable) {
            Log.w(TAG, "AyuuBackgroundSyncManager start error: ${e.message}")
        }

        // Return START_STICKY so the OS restarts this service if memory pressure subsides
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "App task removed (swiped from recents). Scheduling instant service restart...")

        // Schedule an immediate alarm to restart the service when the task is swiped away
        try {
            val restartIntent = Intent(applicationContext, CricBootAndAlarmReceiver::class.java).apply {
                action = CricBootAndAlarmReceiver.ACTION_RESTART_SYNC
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                REQUEST_CODE_RESTART,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000L,
                pendingIntent
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Error scheduling restart alarm on task removed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CricLiveBackgroundService destroyed")
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "CricLive Background Ground Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps real-time cricket notifications active when the app is closed"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🏏 CricLive Ground Sync Active")
            .setContentText("Background match alerts & message notifications chalu hain")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "CricLiveBgService"
        const val SERVICE_CHANNEL_ID = "ayuu_cric_service_channel"
        const val NOTIFICATION_ID = 8801
        private const val REQUEST_CODE_RESTART = 8802

        fun start(context: Context) {
            try {
                val intent = Intent(context, CricLiveBackgroundService::class.java)
                ContextCompat.startForegroundService(context, intent)
                Log.d(TAG, "startForegroundService requested")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to start CricLiveBackgroundService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, CricLiveBackgroundService::class.java)
                context.stopService(intent)
            } catch (_: Throwable) {}
        }
    }
}
