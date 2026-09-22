package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
    private const val PREFS_NAME = "ayuu_cric_notifications"
    private const val KEY_LAST_LIVE_MATCH = "last_notified_live_match_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyMatchLive(context: Context, match: MatchEntity, forceNotify: Boolean = false) {
        if (match.status != "LIVE") return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifiedId = prefs.getString(KEY_LAST_LIVE_MATCH, null)

        // Only notify once when this match becomes live, unless forced
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
            "Match shuru hone ke liye ready hai!"
        }

        val contentText = "${match.teamA} vs ${match.teamB} Live ho gaya hai! $scoreText. Tap karke scorecard aur stream dekhein."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🏏 Match Live Ho Gaya: ${match.teamA} vs ${match.teamB}!")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(match.id.hashCode(), notification)
            prefs.edit().putString(KEY_LAST_LIVE_MATCH, match.id).apply()
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Throwable) {
            // Fallback safe catch
        }
    }
}
