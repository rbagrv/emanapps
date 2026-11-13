package com.example.myapplication.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapplication.BuildConfig
import com.example.myapplication.MainActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    private const val D1_QUERY_URL = "https://api.cloudflare.com/client/v4/accounts/998c5b83aa3cdafa7f4325d5ee1791f2/d1/database/682601ec-f260-421b-bbe3-d9824fd82b95/query"
    private const val D1_AUTH_TOKEN = "Bearer cfat_fRTPrXdOjVACm5ibMRhczAs5tNjKlgxmgomAyuyO58760cf8"
    private const val UPDATE_CHANNEL_ID = "emanager_pos_updates"
    private const val UPDATE_NOTIFICATION_ID = 9001
    private const val GITHUB_AUTH_TOKEN = "token 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"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private val _updateStatus = MutableStateFlow<PosUpdateStatus>(PosUpdateStatus.Idle)
    val updateStatus: StateFlow<PosUpdateStatus> = _updateStatus.asStateFlow()

    private fun normalizeDownloadUrl(rawUrl: String): String {
        return rawUrl.trim().replace(
            "https://github.com/rbagrv/emanapps/raw/",
            "https://raw.githubusercontent.com/rbagrv/emanapps/"
        )
    }

    private fun fetchFromD1(): PosUpdateInfo? {
        return try {
            val payload = """{"sql":"SELECT version_code, version_name, apk_name, download_url, release_notes, force_update FROM app_updates WHERE id = 'pos' LIMIT 1"}"""
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(D1_QUERY_URL)
                .header("Authorization", D1_AUTH_TOKEN)
                .post(payload)
                .build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val bodyStr = resp.body?.string() ?: return null
            val root = gson.fromJson(bodyStr, JsonObject::class.java)
            val rows = root?.getAsJsonArray("result")?.firstOrNull()?.asJsonObject?.getAsJsonArray("results")
            val row = rows?.firstOrNull()?.asJsonObject ?: return null
            PosUpdateInfo(
                appType = "pos",
                newVersionCode = row.get("version_code")?.asInt ?: 0,
                newVersionName = row.get("version_name")?.asString ?: "",
                apkName = row.get("apk_name")?.asString ?: "e-Manager_POS_latest.apk",
                downloadUrl = normalizeDownloadUrl(row.get("download_url")?.asString ?: ""),
                releaseNotes = row.get("release_notes")?.asString ?: "POS Kassa üçün yeni versiya mövcuddur.",
                forceUpdate = (row.get("force_update")?.asInt ?: 0) == 1
            )
        } catch (e: Exception) {
            Log.w(TAG, "D1 update check fallback to GitHub: ${e.message}")
            null
        }
    }

    private fun fetchFromGitHub(): PosUpdateInfo? {
        return try {
            val request = Request.Builder()
                .url("$UPDATES_MANIFEST_URL?nocache=${System.currentTimeMillis()}")
                .header("Authorization", GITHUB_AUTH_TOKEN)
                .header("Cache-Control", "no-cache")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyStr = response.body?.string() ?: return null
            val rootJson = gson.fromJson(bodyStr, JsonObject::class.java)
            val posJson = rootJson?.getAsJsonObject("pos") ?: return null
            PosUpdateInfo(
                appType = "pos",
                newVersionCode = posJson.get("versionCode")?.asInt ?: 0,
                newVersionName = posJson.get("versionName")?.asString ?: "",
                apkName = posJson.get("apkName")?.asString ?: "e-Manager_POS_latest.apk",
                downloadUrl = normalizeDownloadUrl(posJson.get("downloadUrl")?.asString ?: ""),
                releaseNotes = posJson.get("releaseNotes")?.asString ?: "POS Kassa üçün yeni versiya mövcuddur.",
                forceUpdate = posJson.get("forceUpdate")?.asBoolean ?: false
            )
        } catch (e: Exception) {
            Log.w(TAG, "GitHub update check error: ${e.message}")
            null
        }
    }

    private fun showUpdateNotification(context: Context, info: PosUpdateInfo) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    "Proqram Yenilənmələri",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "e-Manager POS yeni versiya bildirişləri"
                }
                nm.createNotificationChannel(channel)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("🚀 Yeni e-Manager POS v${info.newVersionName}")
                .setContentText(info.releaseNotes)
                .setStyle(NotificationCompat.BigTextStyle().bigText(info.releaseNotes))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            nm.notify(UPDATE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post update notification: ${e.message}")
        }
    }

    suspend fun checkForUpdates(forceSilent: Boolean = false, context: Context? = null): PosUpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                if (!forceSilent) {
                    _updateStatus.value = PosUpdateStatus.Checking
                }

                val info = fetchFromD1() ?: fetchFromGitHub()
                if (info == null) {
                    val status = PosUpdateStatus.Error("Yenilənmə serverinə qoşulmaq mümkün olmadı")
                    if (!forceSilent) _updateStatus.value = status
                    return@withContext status
                }

                val currentVersionCode = BuildConfig.VERSION_CODE
                val currentVersionName = BuildConfig.VERSION_NAME

                Log.d(TAG, "POS Update check - Current: $currentVersionCode (v$currentVersionName), Remote: ${info.newVersionCode} (v${info.newVersionName})")

                if (info.newVersionCode > currentVersionCode ||
                    (info.newVersionName != currentVersionName && info.newVersionCode >= currentVersionCode)
                ) {
                    context?.let { showUpdateNotification(it, info) }
                    val status = PosUpdateStatus.Available(info)
                    _updateStatus.value = status
                    return@withContext status
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

                // Delete any old download to avoid corrupt or mismatched versions
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val normalizedUrl = normalizeDownloadUrl(info.downloadUrl)
                val requestBuilder = Request.Builder()
                    .url(normalizedUrl)
                    .header("User-Agent", "e-Manager-POS-Android")

                if (normalizedUrl.contains("github.com") || normalizedUrl.contains("raw.githubusercontent.com")) {
                    requestBuilder.header("Authorization", GITHUB_AUTH_TOKEN)
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
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
                val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else 48f

                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = body.byteStream()
                    outputStream = FileOutputStream(targetFile)

                    val buffer = ByteArray(16 * 1024)
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

                targetFile.setReadable(true, false)
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
                _updateStatus.value = PosUpdateStatus.Error("APK faylı tapılmadı")
                return
            }

            apkFile.setReadable(true, false)

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

            val resolved = context.packageManager.queryIntentActivities(installIntent, 0)
            for (ri in resolved) {
                context.grantUriPermission(ri.activityInfo.packageName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
