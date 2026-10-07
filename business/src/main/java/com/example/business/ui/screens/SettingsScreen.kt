package com.example.business.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.ThemeMode
import com.example.business.ui.theme.EmeraldPrimary
import com.example.business.ui.theme.LossRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BusinessViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val companyName by viewModel.companyName.collectAsState()
    val ownerName by viewModel.ownerName.collectAsState()
    val ownerProfile by viewModel.ownerProfile.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil və Tənzimləmələr",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Profile Card with Edit action
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldPrimary,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (ownerProfile.displayName.isNotBlank()) ownerProfile.displayName else ownerName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ownerProfile.role} • $companyName",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (ownerProfile.email.isNotBlank()) {
                                    Text(
                                        text = ownerProfile.email,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (ownerProfile.phone.isNotBlank()) {
                                    Text(
                                        text = "Əlaqə: ${ownerProfile.phone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { showEditProfileDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Profili Redaktə Et",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tenant / Müəssisə Dəyişdirici
            item {
                Text(
                    text = "Müəssisə və Filial İdarəetməsi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Business,
                            title = "Müəssisə / Tenant Dəyişdir",
                            subtitle = "Cari aktiv: $companyName. Digər biznes obyektlərinə keçid.",
                            onClick = { viewModel.openTenantSelector() }
                        )
                        HorizontalDivider()
                        SettingsRow(
                            icon = Icons.Default.AccountBalance,
                            title = "VÖEN və Bank Rekvizitləri",
                            subtitle = "e-manager.az ERP sistemində qeydiyyatdan keçmiş rekvizitlər",
                            onClick = {
                                Toast.makeText(context, "Müəssisə rekvizitləri e-manager ERP ilə sinxrondur.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 3. Security Settings
            item {
                Text(
                    text = "Təhlükəsizlik və Giriş",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Pin,
                            title = "PIN Kodu Dəyişdir",
                            subtitle = "Tətbiqə sürətli giriş kodu",
                            onClick = { showPinChangeDialog = true }
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                                Column {
                                    Text("Biometrik Giriş", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Barmaq izi və ya Face ID ilə ani giriş", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.isBiometricEnabled.value = it }
                            )
                        }
                    }
                }
            }

            // 3.1 POS Terminal Switch
            item {
                Text(
                    text = "Sistem Tətbiqləri",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    SettingsRow(
                        icon = Icons.Default.PointOfSale,
                        title = "e-Manager POS Kassa Terminalı",
                        subtitle = "Kassa və barkod satış terminalına keçid",
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.emanager.app")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Kassa Terminalı quraşdırılıb. Ana ekrandan 'e-manager POS' seçin.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }

            // 4. Theme & Appearance
            item {
                Text(
                    text = "Görünüş Rejimi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Sistem",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Gündüz",
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Gecə",
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Actions: Lock & Logout
            item {
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { viewModel.lockApp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tətbiqi Kilidlə (PIN Ekrana Qayıt)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = LossRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hesabdan Çıxış", color = LossRed, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Yenilənmələri Yoxla",
                            subtitle = "GitHub-dan ən son versiyanı yoxla və yenilə",
                            onClick = {
                                scope.launch {
                                    val res = com.example.business.util.AppUpdateManager.checkForUpdates(forceSilent = false)
                                    if (res is com.example.business.util.UpdateStatus.UpToDate) {
                                        Toast.makeText(context, "Tətbiq ən son versiyadadır (v${com.example.business.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "e-Manager Sahibkar Edition • v${com.example.business.BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // PROFILE EDIT DIALOG
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(ownerProfile.displayName.ifBlank { ownerName }) }
        var editPhone by remember { mutableStateOf(ownerProfile.phone) }
        var editCompany by remember { mutableStateOf(ownerProfile.companyName.ifBlank { companyName }) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonOutline, contentDescription = null, tint = EmeraldPrimary)
                    Text("Profil Məlumatlarını Yenilə", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Ad və Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Əlaqə Nömrəsi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCompany,
                        onValueChange = { editCompany = it },
                        label = { Text("Şirkət / Müəssisə Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOwnerProfile(editName.trim(), editPhone.trim(), editCompany.trim()) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Profil məlumatları yeniləndi!", Toast.LENGTH_SHORT).show()
                                showEditProfileDialog = false
                            } else {
                                Toast.makeText(context, err ?: "Xəta baş verdi", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Yadda saxla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Ləğv et")
                }
            }
        )
    }

    // PIN CHANGE DIALOG
    if (showPinChangeDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text("Yeni PIN Kod Təyin Edin", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("4 rəqəmli yeni PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            viewModel.savedPin.value = newPin
                            Toast.makeText(context, "PIN kod uğurla dəyişdirildi!", Toast.LENGTH_SHORT).show()
                            showPinChangeDialog = false
                        } else {
                            Toast.makeText(context, "PIN 4 rəqəmli olmalıdır", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Yadda saxla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) { Text("Ləğv et") }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EmeraldPrimary,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White
        ),
        modifier = modifier
    )
}
