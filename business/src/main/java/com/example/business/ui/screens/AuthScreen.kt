package com.example.business.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.EmeraldDark
import com.example.business.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: BusinessViewModel
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Daxil Ol, 1 = Qeydiyyat

    // Login inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register inputs
    var regNameInput by remember { mutableStateOf("") }
    var regCompanyInput by remember { mutableStateOf("") }
    var regVoenInput by remember { mutableStateOf("") }
    var regPhoneInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }
    var regPasswordInput by remember { mutableStateOf("") }
    var regConfirmPasswordInput by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    var authError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // BRAND ICON
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldDark,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = "e-Manager Sahibkar",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "e-Manager Sahibkar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Biznes İdarəetmə və Maliyyə İcmalı",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // TAB SWITCHER
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == 0) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    activeTab = 0
                                    authError = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Daxil Ol",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeTab == 1) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    activeTab = 1
                                    authError = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Yeni Sahibkar Qeydiyyatı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ERROR DISPLAY WITH ACTION BUTTONS
                authError?.let { errText ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (activeTab == 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            activeTab = 1
                                            regEmailInput = emailInput.trim()
                                            authError = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) {
                                        Text("Qeydiyyata Keç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.loginAsDemoOwner()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text("Demo Sahibkar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (activeTab == 0) {
                    // ==========================================
                    // TAB 0: DAXİL OL
                    // ==========================================
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            authError = null
                        },
                        label = { Text("ERP E-poçt / İstifadəçi adı") },
                        placeholder = { Text("sahibkar@emanager.az") },
                        leadingIcon = {
                            Icon(Icons.Default.Mail, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            authError = null
                        },
                        label = { Text("Şifrə") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            authError = null
                            viewModel.login(emailInput, passwordInput) { success, error ->
                                isLoading = false
                                if (!success) {
                                    authError = error
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sahibkar Kabinetinə Daxil Ol",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // PROMINENT DEMO CARD
                    FilledTonalButton(
                        onClick = {
                            viewModel.loginAsDemoOwner()
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚡ Sınaq / Demo Sahibkar Rejimi (1 Toxunuşla)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SWITCH TO POS APP
                    TextButton(
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.emanager.app")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "e-Manager POS Kassa Terminalı quraşdırılıb. Ana ekrandan 'e-manager POS' seçin.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🛒 Kassa Terminalına Keçid (e-Manager POS)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                } else {
                    // ==========================================
                    // TAB 1: QEYDİYYAT
                    // ==========================================
                    OutlinedTextField(
                        value = regNameInput,
                        onValueChange = { regNameInput = it; authError = null },
                        label = { Text("Sahibkarın Adı və Soyadı") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regCompanyInput,
                        onValueChange = { regCompanyInput = it; authError = null },
                        label = { Text("Müəssisə / Mağaza Adı") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = regVoenInput,
                            onValueChange = { regVoenInput = it; authError = null },
                            label = { Text("VÖEN") },
                            placeholder = { Text("1802938471") },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = regPhoneInput,
                            onValueChange = { regPhoneInput = it; authError = null },
                            label = { Text("Telefon") },
                            placeholder = { Text("+99450...") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regEmailInput,
                        onValueChange = { regEmailInput = it; authError = null },
                        label = { Text("E-poçt ünvanı") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regPasswordInput,
                        onValueChange = { regPasswordInput = it; authError = null },
                        label = { Text("Şifrə (minimum 6 simvol)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                Icon(
                                    imageVector = if (regPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regConfirmPasswordInput,
                        onValueChange = { regConfirmPasswordInput = it; authError = null },
                        label = { Text("Şifrə Təkrarı") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (regNameInput.isBlank() || regCompanyInput.isBlank() || regEmailInput.isBlank() || regPasswordInput.isBlank()) {
                                authError = "Zəhmət olmasa bütün vacib xanaları doldurun!"
                                return@Button
                            }
                            if (regPasswordInput != regConfirmPasswordInput) {
                                authError = "Daxil edilən şifrələr eyni deyil!"
                                return@Button
                            }
                            if (regPasswordInput.length < 6) {
                                authError = "Şifrə ən azı 6 simvoldan ibarət olmalıdır!"
                                return@Button
                            }

                            isLoading = true
                            authError = null
                            viewModel.register(
                                displayName = regNameInput.trim(),
                                companyName = regCompanyInput.trim(),
                                voen = regVoenInput.trim().ifBlank { "1800000000" },
                                phone = regPhoneInput.trim(),
                                email = regEmailInput.trim(),
                                password = regPasswordInput.trim()
                            ) { success, error ->
                                isLoading = false
                                if (!success) {
                                    authError = error
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sahibkar Kimi Qeydiyyatdan Keç və Başla",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
