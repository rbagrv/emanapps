package com.example.myapplication.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.util.BiometricHelper

enum class PinScreenMode {
    SETUP,
    UNLOCK
}

@Composable
fun PinLockScreen(
    mode: PinScreenMode,
    companyName: String,
    cashierName: String,
    savedPin: String?,
    isBiometricEnabled: Boolean,
    onPinSet: (pin: String, enableBiometric: Boolean) -> Unit,
    onPinSuccess: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricChecked by remember { mutableStateOf(true) }

    val hasBiometricSensor = remember { BiometricHelper.isBiometricAvailable(context) }

    // Auto-trigger biometric prompt on unlock mode if enabled
    LaunchedEffect(mode, isBiometricEnabled) {
        if (mode == PinScreenMode.UNLOCK && isBiometricEnabled && activity != null && hasBiometricSensor) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "e-Manager Kassa Girişi",
                subtitle = "$companyName - $cashierName",
                negativeButtonText = "PİN ilə daxil ol",
                onSuccess = { onPinSuccess() },
                onError = { /* Let user use keypad */ }
            )
        }
    }

    val onDigitClick: (String) -> Unit = { digit ->
        errorMessage = null
        if (mode == PinScreenMode.UNLOCK) {
            if (enteredPin.length < 4) {
                val newPin = enteredPin + digit
                enteredPin = newPin
                if (newPin.length == 4) {
                    if (newPin == savedPin) {
                        onPinSuccess()
                    } else {
                        errorMessage = "PİN kod yanlışdır!"
                        enteredPin = ""
                    }
                }
            }
        } else {
            // SETUP MODE
            if (!isConfirming) {
                if (enteredPin.length < 4) {
                    enteredPin += digit
                    if (enteredPin.length == 4) {
                        isConfirming = true
                    }
                }
            } else {
                if (confirmPin.length < 4) {
                    val newConfirm = confirmPin + digit
                    confirmPin = newConfirm
                    if (newConfirm.length == 4) {
                        if (enteredPin == newConfirm) {
                            onPinSet(enteredPin, biometricChecked && hasBiometricSensor)
                        } else {
                            errorMessage = "PİN kodlar uyğun gəlmir, yenidən daxil edin"
                            enteredPin = ""
                            confirmPin = ""
                            isConfirming = false
                        }
                    }
                }
            }
        }
    }

    val onDeleteClick = {
        errorMessage = null
        if (mode == PinScreenMode.UNLOCK) {
            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
        } else {
            if (isConfirming) {
                if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                else isConfirming = false
            } else {
                if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (mode == PinScreenMode.SETUP) Icons.Default.Shield else Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = if (mode == PinScreenMode.SETUP) {
                    if (!isConfirming) "Kassa PİN Kodu Təyin Edin" else "PİN Kodu Təsdiqləyin"
                } else {
                    companyName.ifBlank { "e-Manager POS" }
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (mode == PinScreenMode.SETUP) {
                    "Kassaya sürətli daxil olmaq üçün 4 rəqəmli şifrə seçin"
                } else {
                    "Kassir: $cashierName"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            val currentPinLength = if (mode == PinScreenMode.UNLOCK) enteredPin.length
            else if (!isConfirming) enteredPin.length else confirmPin.length

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < currentPinLength
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Error display
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            // Biometric Option for Setup Mode
            if (mode == PinScreenMode.SETUP && hasBiometricSensor) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { biometricChecked = !biometricChecked }
                ) {
                    Checkbox(
                        checked = biometricChecked,
                        onCheckedChange = { biometricChecked = it }
                    )
                    Text(
                        text = "Barmaq izi ilə girişi aktivləşdir",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3x4 KEYPAD
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        when (key) {
                                            "BIO" -> if (mode == PinScreenMode.UNLOCK && hasBiometricSensor) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            "DEL" -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable(enabled = key != "BIO" || (mode == PinScreenMode.UNLOCK && hasBiometricSensor)) {
                                        when (key) {
                                            "DEL" -> onDeleteClick()
                                            "BIO" -> {
                                                if (mode == PinScreenMode.UNLOCK && activity != null && hasBiometricSensor) {
                                                    BiometricHelper.showBiometricPrompt(
                                                        activity = activity,
                                                        title = "e-Manager Kassa Girişi",
                                                        subtitle = "$companyName - $cashierName",
                                                        onSuccess = { onPinSuccess() },
                                                        onError = { msg -> errorMessage = msg }
                                                    )
                                                }
                                            }
                                            else -> onDigitClick(key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "DEL" -> Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Sil",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    "BIO" -> {
                                        if (mode == PinScreenMode.UNLOCK && hasBiometricSensor) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Barmaq izi",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    else -> Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch account / Log out
            TextButton(
                onClick = onSwitchAccount,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Fərqli hesabla daxil ol",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
