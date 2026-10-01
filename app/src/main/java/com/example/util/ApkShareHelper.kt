package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ApkShareHelper {

    /**
     * Extracts and shares the currently running app's fresh APK directly to WhatsApp,
     * Quick Share, Nearby Share, Bluetooth, Telegram, Drive or any sharing app.
     * Uses dynamic version code and build timestamp in filename to prevent cached old APK overwrites.
     */
    fun shareInstalledApk(context: Context) {
        try {
            val appInfo = context.applicationInfo
            val sourceApkFile = File(appInfo.sourceDir)

            if (!sourceApkFile.exists() || sourceApkFile.length() == 0L) {
                Toast.makeText(context, "APK file not found on device!", Toast.LENGTH_SHORT).show()
                return
            }

            // Create or clean export directory so old APK versions are never re-sent
            val exportDir = File(context.cacheDir, "apk_share")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { it.delete() }
            } else {
                exportDir.mkdirs()
            }

            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            } catch (_: Throwable) {
                null
            }

            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong()
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toLong() ?: BuildConfig.VERSION_CODE.toLong()
            }
            val vName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME

            val timeTag = SimpleDateFormat("ddMMM_HHmmss", Locale.US).format(Date())

            // Unique filename with version and second-level timestamp prevents Android or WhatsApp from opening older cached downloads
            val appName = "AyuuCric_v${vName}_b${vCode}_${timeTag}.apk"
            val targetApk = File(exportDir, appName)

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
                putExtra(Intent.EXTRA_SUBJECT, "AyuuCric Live Cricket Update (v$vName Build #$vCode)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🏏 AyuuCric Live Cricket Scorer LATEST UPDATE!\n\n" +
                    "Version: v$vName (Build #$vCode)\n" +
                    "Generated: $timeTag\n\n" +
                    "Apne phone me sabhi naye features aur live commentary pane ke liye is latest APK par tap karke Install / Update karein!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Latest AyuuCric APK (v$vName)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "APK Share Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
