package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateState(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String = "2.0.0",
    val latestVersionCode: Int = 2,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val releaseTitle: String = "Gully Cricket Umpire v2.0.0",
    val changelog: List<String> = listOf(
        "⚡ Instant 0-0 Match start (Fair play, all demo records cleared)",
        "🚀 In-App direct Auto-Updater (Update all devices without hassle)",
        "📶 Ground Offline Share (Send APK via Quick Share / WhatsApp to other 3 phones)",
        "🎯 Multi-Angle Camera & DRS UltraEdge calibration"
    ),
    val downloadUrl: String = "",
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val downloadedApkPath: String? = null,
    val errorMessage: String? = null,
    val customUpdateUrl: String = ""
)

class AppUpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow(AppUpdateState())
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(customUrl: String? = null) = withContext(Dispatchers.IO) {
        _updateState.value = _updateState.value.copy(
            isChecking = true,
            errorMessage = null
        )

        try {
            val urlToFetch = customUrl?.takeIf { it.isNotBlank() }
                ?: _updateState.value.customUpdateUrl.takeIf { it.isNotBlank() }

            if (urlToFetch.isNullOrBlank()) {
                // If no remote URL configured yet, simulate checking against current version
                // We show the latest release info ready for install or sharing
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    isUpdateAvailable = false,
                    errorMessage = null
                )
                return@withContext
            }

            val url = URL(urlToFetch)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val newCode = json.optInt("versionCode", BuildConfig.VERSION_CODE)
                val newName = json.optString("versionName", BuildConfig.VERSION_NAME)
                val title = json.optString("title", "New Cricket Umpire Update")
                val dUrl = json.optString("downloadUrl", "")
                val notesArray = json.optJSONArray("changelog")
                val notes = mutableListOf<String>()
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        notes.add(notesArray.getString(i))
                    }
                } else {
                    notes.add("Performance improvements and bug fixes")
                }

                val hasUpdate = newCode > BuildConfig.VERSION_CODE

                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    isUpdateAvailable = hasUpdate,
                    latestVersionCode = newCode,
                    latestVersionName = newName,
                    releaseTitle = title,
                    downloadUrl = dUrl,
                    changelog = if (notes.isNotEmpty()) notes else _updateState.value.changelog,
                    errorMessage = null
                )
            } else {
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    errorMessage = "Server response: ${connection.responseCode}"
                )
            }
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                isChecking = false,
                errorMessage = "Check failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    suspend fun downloadApk(downloadUrl: String) = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) {
            _updateState.value = _updateState.value.copy(
                errorMessage = "Download link is missing"
            )
            return@withContext
        }

        _updateState.value = _updateState.value.copy(
            isDownloading = true,
            downloadProgress = 0.05f,
            errorMessage = null
        )

        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.connect()

            val fileLength = connection.contentLength
            val cacheDir = File(context.cacheDir, "updates")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val apkFile = File(cacheDir, "gully_cricket_update.apk")
            if (apkFile.exists()) apkFile.delete()

            val input: InputStream = connection.inputStream
            val output = FileOutputStream(apkFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total.toFloat() / fileLength.toFloat()).coerceIn(0.05f, 0.98f)
                    _updateState.value = _updateState.value.copy(downloadProgress = progress)
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                downloadProgress = 1.0f,
                downloadedApkPath = apkFile.absolutePath,
                errorMessage = null
            )
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                isDownloading = false,
                errorMessage = "Download failed: ${e.localizedMessage}"
            )
        }
    }

    fun installDownloadedApk(apkFilePath: String? = null) {
        val path = apkFilePath ?: _updateState.value.downloadedApkPath ?: return
        val file = File(path)
        if (!file.exists()) {
            _updateState.value = _updateState.value.copy(errorMessage = "APK file not found on device")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                errorMessage = "Install failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Shares the app's currently installed APK directly with other phones
     * via Quick Share, Nearby Share, WhatsApp, or Bluetooth.
     * ZERO INTERNET REQUIRED AT THE CRICKET GROUND!
     */
    fun shareInstalledApkDirectly() {
        try {
            val appInfo = context.applicationInfo
            val sourceApk = File(appInfo.sourceDir)

            val cacheDir = File(context.cacheDir, "shared_apk")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val shareableApk = File(cacheDir, "AyuuCric_Live_v${BuildConfig.VERSION_NAME}.apk")
            sourceApk.copyTo(shareableApk, overwrite = true)

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                shareableApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "AyuuCric Live Cricket Umpire App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Install AyuuCric Live Cricket App (v${BuildConfig.VERSION_NAME}) to connect as Bowler Cam, Crease Cam, Scorer, or Third Umpire!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share App with Team / Phones").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            _updateState.value = _updateState.value.copy(
                errorMessage = "Share failed: ${e.localizedMessage}"
            )
        }
    }

    fun setCustomUpdateUrl(url: String) {
        _updateState.value = _updateState.value.copy(customUpdateUrl = url)
    }

    fun clearError() {
        _updateState.value = _updateState.value.copy(errorMessage = null)
    }
}
