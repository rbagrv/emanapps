package com.example.business.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.business.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val appType: String,
    val newVersionCode: Int,
    val newVersionName: String,
    val apkName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val info: AppUpdateInfo) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progress: Float, val currentMb: Float, val totalMb: Float) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val UPDATES_MANIFEST_URL = "https://raw.githubusercontent.com/rbagrv/emanapps/main/app_updates.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    suspend fun checkForUpdates(forceSilent: Boolean = false): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceSilent) {
                    _updateStatus.value = UpdateStatus.Checking
                }

                val request = Request.Builder()
                    .url("$UPDATES_MANIFEST_URL?nocache=${System.currentTimeMillis()}")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val status = UpdateStatus.Error("Yenilənmə serverinə qoşulmaq mümkün olmadı (HTTP ${response.code})")
                    if (!forceSilent) _updateStatus.value = status
                    return@withContext status
                }

                val bodyStr = response.body?.string() ?: ""
                val rootJson = gson.fromJson(bodyStr, JsonObject::class.java)
                val businessJson = rootJson.getAsJsonObject("business")

                if (businessJson != null) {
                    val remoteVersionCode = businessJson.get("versionCode")?.asInt ?: 0
                    val remoteVersionName = businessJson.get("versionName")?.asString ?: ""
                    val apkName = businessJson.get("apkName")?.asString ?: "e-Manager_Sahibkar_latest.apk"
                    val downloadUrl = businessJson.get("downloadUrl")?.asString ?: ""
                    val releaseNotes = businessJson.get("releaseNotes")?.asString ?: "Tətbiq üçün yeni versiya mövcuddur."
                    val forceUpdate = businessJson.get("forceUpdate")?.asBoolean ?: false

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    val currentVersionName = BuildConfig.VERSION_NAME

                    Log.d(TAG, "Update check - Current: $currentVersionCode (v$currentVersionName), Remote: $remoteVersionCode (v$remoteVersionName)")

                    if (remoteVersionCode > currentVersionCode || (remoteVersionName != currentVersionName && remoteVersionCode >= currentVersionCode)) {
                        val info = AppUpdateInfo(
                            appType = "business",
                            newVersionCode = remoteVersionCode,
                            newVersionName = remoteVersionName,
                            apkName = apkName,
                            downloadUrl = downloadUrl,
                            releaseNotes = releaseNotes,
                            forceUpdate = forceUpdate
                        )
                        val status = UpdateStatus.Available(info)
                        _updateStatus.value = status
                        return@withContext status
                    }
                }

                val upToDate = UpdateStatus.UpToDate
                if (!forceSilent) _updateStatus.value = upToDate
                return@withContext upToDate
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates: ${e.message}", e)
                val status = UpdateStatus.Error("Yenilənmə yoxlanılarkən xəta: ${e.localizedMessage ?: "Şəbəkə xətası"}")
                if (!forceSilent) _updateStatus.value = status
                return@withContext status
            }
        }
    }

    suspend fun downloadAndInstallApk(context: Context, info: AppUpdateInfo) {
        withContext(Dispatchers.IO) {
            try {
                _updateStatus.value = UpdateStatus.Downloading(0.01f, 0f, 0f)

                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                downloadDir.mkdirs()
                val targetFile = File(downloadDir, info.apkName)

                // If file already exists and is not empty, check if we need to re-download or install
                if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024) {
                    Log.d(TAG, "APK already downloaded (${targetFile.length()} bytes), launching installer directly")
                    _updateStatus.value = UpdateStatus.ReadyToInstall(targetFile)
                    withContext(Dispatchers.Main) {
                        installApk(context, targetFile)
                    }
                    return@withContext
                }

                val request = Request.Builder()
                    .url(info.downloadUrl)
                    .header("User-Agent", "e-Manager-Sahibkar-Android")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _updateStatus.value = UpdateStatus.Error("Fayl yüklənə bilmədi (HTTP ${response.code})")
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    _updateStatus.value = UpdateStatus.Error("Boş fayl cavabı alındı")
                    return@withContext
                }

                val contentLength = body.contentLength()
                val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else 26f

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesRead: Long = 0
                    var lastReportedTime = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastReportedTime > 200 || totalBytesRead == contentLength) {
                            val currentMb = totalBytesRead / (1024f * 1024f)
                            val progress = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else 0.5f

                            _updateStatus.value = UpdateStatus.Downloading(progress, currentMb, totalMb)
                            lastReportedTime = currentTime
                        }
                    }
                    outputStream.flush()
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }

                Log.d(TAG, "Download finished successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                _updateStatus.value = UpdateStatus.ReadyToInstall(targetFile)

                withContext(Dispatchers.Main) {
                    installApk(context, targetFile)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}", e)
                _updateStatus.value = UpdateStatus.Error("Yükləmə zamanı xəta: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
                return
            }

            // Android 8.0 (API 26) and above requires REQUEST_INSTALL_PACKAGES check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            Log.d(TAG, "Launched APK package installer for: ${apkFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}", e)
            _updateStatus.value = UpdateStatus.Error("Quraşdırma xətası: ${e.localizedMessage}")
        }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }
}
