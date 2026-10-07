package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
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

data class PosUpdateInfo(
    val appType: String,
    val newVersionCode: Int,
    val newVersionName: String,
    val apkName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean
)

sealed class PosUpdateStatus {
    object Idle : PosUpdateStatus()
    object Checking : PosUpdateStatus()
    data class Available(val info: PosUpdateInfo) : PosUpdateStatus()
    object UpToDate : PosUpdateStatus()
    data class Downloading(val progress: Float, val currentMb: Float, val totalMb: Float) : PosUpdateStatus()
    data class ReadyToInstall(val apkFile: File) : PosUpdateStatus()
    data class Error(val message: String) : PosUpdateStatus()
}

object PosAppUpdateManager {
    private const val TAG = "PosAppUpdateManager"
    private const val UPDATES_MANIFEST_URL = "https://raw.githubusercontent.com/rbagrv/emanapps/main/app_updates.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private val _updateStatus = MutableStateFlow<PosUpdateStatus>(PosUpdateStatus.Idle)
    val updateStatus: StateFlow<PosUpdateStatus> = _updateStatus.asStateFlow()

    suspend fun checkForUpdates(forceSilent: Boolean = false): PosUpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceSilent) {
                    _updateStatus.value = PosUpdateStatus.Checking
                }

                val request = Request.Builder()
                    .url("$UPDATES_MANIFEST_URL?nocache=${System.currentTimeMillis()}")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val status = PosUpdateStatus.Error("Yenilənmə serverinə qoşulmaq mümkün olmadı (HTTP ${response.code})")
                    if (!forceSilent) _updateStatus.value = status
                    return@withContext status
                }

                val bodyStr = response.body?.string() ?: ""
                val rootJson = gson.fromJson(bodyStr, JsonObject::class.java)
                val posJson = rootJson?.getAsJsonObject("pos")

                if (posJson != null) {
                    val remoteVersionCode = posJson.get("versionCode")?.asInt ?: 0
                    val remoteVersionName = posJson.get("versionName")?.asString ?: ""
                    val apkName = posJson.get("apkName")?.asString ?: "e-Manager_POS_latest.apk"
                    val downloadUrl = posJson.get("downloadUrl")?.asString ?: ""
                    val releaseNotes = posJson.get("releaseNotes")?.asString ?: "POS Kassa üçün yeni versiya mövcuddur."
                    val forceUpdate = posJson.get("forceUpdate")?.asBoolean ?: false

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    val currentVersionName = BuildConfig.VERSION_NAME

                    Log.d(TAG, "POS Update check - Current: $currentVersionCode (v$currentVersionName), Remote: $remoteVersionCode (v$remoteVersionName)")

                    if (remoteVersionCode > currentVersionCode || (remoteVersionName != currentVersionName && remoteVersionCode >= currentVersionCode)) {
                        val info = PosUpdateInfo(
                            appType = "pos",
                            newVersionCode = remoteVersionCode,
                            newVersionName = remoteVersionName,
                            apkName = apkName,
                            downloadUrl = downloadUrl,
                            releaseNotes = releaseNotes,
                            forceUpdate = forceUpdate
                        )
                        val status = PosUpdateStatus.Available(info)
                        _updateStatus.value = status
                        return@withContext status
                    }
                }

                val upToDate = PosUpdateStatus.UpToDate
                if (!forceSilent) _updateStatus.value = upToDate
                return@withContext upToDate
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for POS updates: ${e.message}", e)
                val status = PosUpdateStatus.Error("Yenilənmə yoxlanılarkən xəta: ${e.localizedMessage ?: "Şəbəkə xətası"}")
                if (!forceSilent) _updateStatus.value = status
                return@withContext status
            }
        }
    }

    suspend fun downloadAndInstallApk(context: Context, info: PosUpdateInfo) {
        withContext(Dispatchers.IO) {
            try {
                _updateStatus.value = PosUpdateStatus.Downloading(0.01f, 0f, 0f)

                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                downloadDir.mkdirs()
                val targetFile = File(downloadDir, info.apkName)

                // If file already exists and is fully downloaded, directly install
                if (targetFile.exists() && targetFile.length() > 5 * 1024 * 1024) {
                    Log.d(TAG, "POS APK already downloaded (${targetFile.length()} bytes), launching installer directly")
                    _updateStatus.value = PosUpdateStatus.ReadyToInstall(targetFile)
                    withContext(Dispatchers.Main) {
                        installApk(context, targetFile)
                    }
                    return@withContext
                }

                val request = Request.Builder()
                    .url(info.downloadUrl)
                    .header("User-Agent", "e-Manager-POS-Android")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _updateStatus.value = PosUpdateStatus.Error("Fayl yüklənə bilmədi (HTTP ${response.code})")
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    _updateStatus.value = PosUpdateStatus.Error("Boş fayl cavabı alındı")
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

                            _updateStatus.value = PosUpdateStatus.Downloading(progress, currentMb, totalMb)
                            lastReportedTime = currentTime
                        }
                    }
                    outputStream.flush()
                } finally {
                    inputStream?.close()
                    outputStream?.close()
                }

                Log.d(TAG, "POS download finished successfully: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                _updateStatus.value = PosUpdateStatus.ReadyToInstall(targetFile)

                withContext(Dispatchers.Main) {
                    installApk(context, targetFile)
                }

            } catch (e: Exception) {
                Log.e(TAG, "POS Download error: ${e.message}", e)
                _updateStatus.value = PosUpdateStatus.Error("Yükləmə zamanı xəta: ${e.localizedMessage}")
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Log.e(TAG, "POS APK file does not exist: ${apkFile.absolutePath}")
                return
            }

            // Android 8.0 (API 26) and above requires REQUEST_INSTALL_PACKAGES permission
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
            Log.d(TAG, "Launched POS APK package installer for: ${apkFile.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error installing POS APK: ${e.message}", e)
            _updateStatus.value = PosUpdateStatus.Error("Quraşdırma xətası: ${e.localizedMessage}")
        }
    }

    fun resetStatus() {
        _updateStatus.value = PosUpdateStatus.Idle
    }
}
