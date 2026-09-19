package com.example.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.random.Random

object OtpVerificationService {

    private const val CHANNEL_ID = "AyuuCric_Live_otp_channel"
    private const val CHANNEL_NAME = "AyuuCric Live Verification Codes"

    /**
     * Generates a random 4-digit OTP, creates an Android Heads-Up Notification with the OTP message,
     * and also provides an SMS app intent to send/receive real SMS.
     */
    fun sendRealOtp(context: Context, phoneNumber: String): String {
        // Generate random 4-digit code (1000 - 9999)
        val generatedCode = Random.nextInt(1000, 9999).toString()

        // Trigger Android Heads-Up Notification
        showOtpNotification(context, phoneNumber, generatedCode)

        return generatedCode
    }

    private fun showOtpNotification(context: Context, phoneNumber: String, otpCode: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AyuuCric Live SMS Verification Codes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📩 SMS: AyuuCric Live Security Code")
            .setContentText("Your AyuuCric Live login OTP is $otpCode. Do not share this with anyone. Valid for 5 minutes.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("AYUUCRIC LIVE VERIFICATION\nYour 4-digit OTP for $phoneNumber is: $otpCode.\n\nUse this code to verify your profile identity.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(7788, notification)
    }

    /**
     * Opens native Android SMS app prefilled with recipient or code if user wants real SMS carrier
     */
    fun openSmsApp(context: Context, phoneNumber: String, otpCode: String) {
        try {
            val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber")
                putExtra("sms_body", "AYUUCRIC OTP: $otpCode is your 4-digit verification code.")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
        } catch (_: Exception) {
        }
    }
}
