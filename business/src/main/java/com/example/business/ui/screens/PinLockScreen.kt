package com.example.business.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.EmeraldPrimary
import com.example.business.util.BiometricHelper

@Composable
fun PinLockScreen(
    viewModel: BusinessViewModel
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val companyName by viewModel.companyName.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun triggerBiometric() {
        if (activity != null && isBiometricEnabled) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                title = "e-Manager Sahibkar Girişi",
                subtitle = "Barmaq iziniz və ya Face ID ilə kiliddən çıxarın",
                negativeButtonText = "PIN ilə daxil ol",
                onSuccess = {
                    viewModel.unlockBiometric()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (isBiometricEnabled && activity != null && BiometricHelper.isBiometricAvailable(activity)) {
            triggerBiometric()
        }
    }

    fun onDigitPress(digit: String) {
        if (pin.length < 4) {
            val newPin = pin + digit
            pin = newPin
            errorMessage = null
            if (newPin.length == 4) {
                val success = viewModel.unlockPin(newPin)
                if (!success) {
                    errorMessage = "Daxil edilmiş PIN kod yanlışdır!"
                    pin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(52.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = companyName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Təhlükəsizlik PIN kodunu daxil edin (Defolt: 1234)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < pin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) EmeraldPrimary else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (isFilled) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Keypad
            val digits = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                digits.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (isBiometricEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .clickable { triggerBiometric() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometrik",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable { onBackspace() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .clickable { onDigitPress(key) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
