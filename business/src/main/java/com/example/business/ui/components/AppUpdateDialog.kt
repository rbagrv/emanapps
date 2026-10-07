package com.example.business.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.business.ui.theme.EmeraldPrimary
import com.example.business.util.AppUpdateInfo
import com.example.business.util.AppUpdateManager
import com.example.business.util.UpdateStatus
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AppUpdateDialog() {
    val status by AppUpdateManager.updateStatus.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (val currentStatus = status) {
        is UpdateStatus.Available -> {
            val info = currentStatus.info
            AlertDialog(
                onDismissRequest = {
                    if (!info.forceUpdate) {
                        AppUpdateManager.resetStatus()
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !info.forceUpdate,
                    dismissOnClickOutside = !info.forceUpdate
                ),
                icon = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = "🚀 Yeni Versiya: v${info.newVersionName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "e-Manager Sahibkar üçün yeni yenilənmə GitHub-da yayımlandı.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Yeniliklər:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = info.releaseNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Yeniləmək üçün aşağıdakı düyməni sıxın. APK birbaşa yüklənib quraşdırılacaqdır.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                AppUpdateManager.downloadAndInstallApk(context, info)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("İndi Yenilə", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    if (!info.forceUpdate) {
                        TextButton(onClick = { AppUpdateManager.resetStatus() }) {
                            Text("Daha Sonra")
                        }
                    }
                }
            )
        }

        is UpdateStatus.Downloading -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { currentStatus.progress },
                            color = EmeraldPrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "Yenilənmə Yüklənir...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        LinearProgressIndicator(
                            progress = { currentStatus.progress },
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(currentStatus.progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", currentStatus.currentMb)} / ${String.format(Locale.US, "%.1f", currentStatus.totalMb)} MB",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Yükləmə tamamlanan kimi quraşdırma avtomatik başlayacaq.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is UpdateStatus.ReadyToInstall -> {
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                icon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                },
                title = { Text("Yüklənmə Tamamlandı", fontWeight = FontWeight.Bold) },
                text = { Text("Yeni versiya uğurla endirildi. Proqramı yeniləmək üçün quraşdırmanı təsdiq edin.") },
                confirmButton = {
                    Button(
                        onClick = { AppUpdateManager.installApk(context, currentStatus.apkFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Quraşdır")
                    }
                }
            )
        }

        is UpdateStatus.Error -> {
            AlertDialog(
                onDismissRequest = { AppUpdateManager.resetStatus() },
                icon = {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                },
                title = { Text("Yenilənmə Xətası", fontWeight = FontWeight.Bold) },
                text = { Text(currentStatus.message) },
                confirmButton = {
                    TextButton(onClick = { AppUpdateManager.resetStatus() }) {
                        Text("Bağla")
                    }
                }
            )
        }

        else -> {}
    }
}
