package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ApkShareHelper {

    /**
     * Extracts and shares the currently running app's APK directly to WhatsApp,
     * Nearby Share, Bluetooth, Telegram, Drive or any sharing app.
     */
    fun shareInstalledApk(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val sourceApkFile = File(appInfo.sourceDir)

            if (!sourceApkFile.exists()) {
                Toast.makeText(context, "APK file not found on device!", Toast.LENGTH_SHORT).show()
                return
            }

            // Create an export directory in external cache so other apps can read it
            val exportDir = File(context.cacheDir, "apk_share")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val appName = "AyuuCric_v2_LiveSync.apk"
            val targetApk = File(exportDir, appName)

            // Copy base.apk into export location with clean readable name
            FileInputStream(sourceApkFile).use { input ->
                FileOutputStream(targetApk).use { output ->
                    input.copyTo(output)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                targetApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "AyuuCric Live Cricket Scorer APK")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🏏 AyuuCric Live Cricket Scorer APK!\n" +
                    "Is APK ko install karo aur live match, multi-phone scorer, Sidhu Paaji commentary aur live ball-by-ball updates dekho bina kisi wire ke."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share AyuuCric APK via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "APK Share Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
