package com.example.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MatchEntity

object MatchNotificationHelper {

    private const val CHANNEL_ID = "ayuu_cric_live_matches_channel"
    private const val CHANNEL_NAME = "AyuuCric Live Matches"
    private const val CHANNEL_DESC = "Notifications for when cricket matches go live"

    private const val DM_CHANNEL_ID = "ayuu_cric_dm_channel"
    private const val DM_CHANNEL_NAME = "Direct Messages & Snaps"
    private const val DM_CHANNEL_DESC = "Instant notifications for 1-on-1 player direct messages and snaps"

    private const val PREFS_NAME = "ayuu_cric_notifications"
    private const val KEY_LAST_LIVE_MATCH = "last_notified_live_match_id"
    private const val KEY_DM_MUTED = "is_dm_notifications_muted"
    private const val KEY_MATCH_MUTED = "is_match_start_notifications_muted"

    fun isDmMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DM_MUTED, false)
    }

    fun setDmMuted(context: Context, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DM_MUTED, muted).apply()
    }

    fun isMatchStartMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MATCH_MUTED, false)
    }

    fun setMatchStartMuted(context: Context, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MATCH_MUTED, muted).apply()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val liveMatchChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                try {
                    setBypassDnd(true)
                } catch (_: Throwable) {}
            }
            manager.createNotificationChannel(liveMatchChannel)

            val dmChannel = NotificationChannel(
                DM_CHANNEL_ID,
                DM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = DM_CHANNEL_DESC
                enableLights(true)
                lightColor = 0xFF00E676.toInt()
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 150, 450)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                try {
                    setBypassDnd(true)
                } catch (_: Throwable) {}
                try {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                } catch (_: Throwable) {}
            }
            manager.createNotificationChannel(dmChannel)
        }
    }

    fun notifyMatchLive(context: Context, match: MatchEntity, forceNotify: Boolean = false) {
        if (match.status != "LIVE") return
        if (isMatchStartMuted(context)) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifiedId = prefs.getString(KEY_LAST_LIVE_MATCH, null)

        // Only notify once when this match starts, unless forced
        if (!forceNotify && lastNotifiedId == match.id) {
            return
        }

        // Check POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_MATCH_ID", match.id)
            putExtra("EXTRA_START_TAB", "LIVE_CENTER")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            match.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val scoreText = if (match.score > 0 || match.wickets > 0 || match.legalBalls > 0) {
            val overs = "${match.legalBalls / 6}.${match.legalBalls % 6}"
            "Score: ${match.score}/${match.wickets} in $overs ov"
        } else {
            "Match shuru ho gaya hai!"
        }

        val contentText = "${match.teamA} vs ${match.teamB} Live! $scoreText. Tap karke scorecard dekhein."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🏏 Match Start: ${match.teamA} vs ${match.teamB}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(match.id.hashCode(), notification)
            prefs.edit().putString(KEY_LAST_LIVE_MATCH, match.id).apply()
        } catch (_: Throwable) {}
    }

    fun notifyDirectMessage(
        context: Context,
        senderName: String,
        senderUsername: String,
        messageText: String,
        isSnap: Boolean
    ) {
        if (isDmMuted(context)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_DM", true)
            putExtra("EXTRA_RECIPIENT_USER", senderUsername)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderUsername.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSnap) "⚡ Snap received from $senderName" else "💬 Message from $senderName (@$senderUsername)"
        val body = if (isSnap) "Tap karke Snap dekhein (View once) 🔥" else messageText

        val notification = NotificationCompat.Builder(context, DM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 450, 150, 450))
            .setLights(0xFF00E676.toInt(), 1000, 500)
            .setFullScreenIntent(pendingIntent, false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(senderUsername.hashCode(), notification)
        } catch (_: Throwable) {}
    }

    fun notifyDrsTaken(
        context: Context,
        appealType: String,
        batsman: String,
        bowler: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_START_TAB", "DRS_SYSTEM")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9901,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 DRS REVIEW: $appealType Appeal!"
        val message = "ओए गुरु! $bowler ne $batsman ke khilaf DRS le liya hai! Chak de phatte, 3rd Umpire verdict aane wala hai!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9901, notification)
        } catch (_: Throwable) {}
    }

    fun sendTestLiveNotification(context: Context) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9902,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🏏 AyuuCric Live Notification Test"
        val message = "Notification bilkul sahi chal raha hai! Match live hone par aur DRS appeal lene par status bar me aisi hi alert aayegi."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(9902, notification)
        } catch (_: Throwable) {}
    }

    fun notifyGenericPushAlert(
        context: Context,
        title: String,
        message: String,
        matchId: String = ""
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (matchId.isNotBlank()) {
                putExtra("EXTRA_MATCH_ID", matchId)
                putExtra("EXTRA_START_TAB", "LIVE_CENTER")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (title + message).hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((title + message).hashCode(), notification)
        } catch (_: Throwable) {}
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                try {
                    val appSettingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(appSettingsIntent)
                } catch (_: Throwable) {}
            }
        }
    }

    fun isChannelBypassingDnd(context: Context, channelId: String = DM_CHANNEL_ID): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = manager?.getNotificationChannel(channelId)
            return channel?.canBypassDnd() ?: false
        }
        return true
    }

    fun isDndPolicyAccessGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.isNotificationPolicyAccessGranted ?: false
        } else {
            true
        }
    }

    fun openChannelNotificationSettings(context: Context, channelId: String = DM_CHANNEL_ID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                try {
                    val appIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(appIntent)
                } catch (_: Throwable) {}
            }
        }
    }

    fun requestDndPolicyAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                openChannelNotificationSettings(context)
            }
        }
    }
}
