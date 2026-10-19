package com.example.myapplication.ui.screens

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.PosViewModel
import com.example.myapplication.ui.ThemeMode
import com.example.myapplication.ui.theme.EmanagerBlue
import com.example.myapplication.ui.theme.EmanagerGreen
import com.example.myapplication.ui.theme.EmanagerRose
import com.example.myapplication.ui.theme.Slate200
import com.example.myapplication.ui.theme.Slate500
import com.example.myapplication.util.BluetoothPrinterHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cashierName by viewModel.cashierName.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val isErpConnected by viewModel.isErpConnected.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val activeTenantId = viewModel.activeTenantId

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showPrintResultDialog by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // PIN Change inputs
    var oldPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    // 1. CHANGE PIN DIALOG
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                pinErrorMessage = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = EmanagerBlue)
                    Text("PIN Kodu Dəyişdir", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Təhlükəsizlik üçün cari və yeni 4 rəqəmli PIN kodu daxil edin.", fontSize = 12.sp, color = Slate500)

                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = { if (it.length <= 4) oldPinInput = it },
                        label = { Text("Köhnə PIN (4 rəqəm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4) newPinInput = it },
                        label = { Text("Yeni PIN (4 rəqəm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!pinErrorMessage.isNullOrBlank()) {
                        Text(text = pinErrorMessage!!, color = EmanagerRose, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePin(oldPinInput, newPinInput) { success, msg ->
                            if (success) {
                                Toast.makeText(context, msg ?: "PIN dəyişdirildi", Toast.LENGTH_SHORT).show()
                                oldPinInput = ""
                                newPinInput = ""
                                pinErrorMessage = null
                                showChangePinDialog = false
                            } else {
                                pinErrorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) { Text("Təsdiq Et") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    pinErrorMessage = null
                }) { Text("Ləğv") }
            }
        )
    }

    // 2. PRINTER CONNECTION & TEST PRINT DIALOG
    if (showPrinterDialog) {
        val pairedDevices = remember { BluetoothPrinterHelper.getPairedPrinters() }
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = EmanagerBlue)
                    Text("Bluetooth Çek Printeri", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Cütləşdirilmiş Bluetooth Qurğuları:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (pairedDevices.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Aktiv Bluetooth printer aşkar edilmədi.", fontSize = 12.sp, color = Slate500)
                                Text("Simulyasiya rejimində test çeki yaradıla bilər.", fontSize = 11.sp, color = EmanagerBlue)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pairedDevices) { dev ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        coroutineScope.launch {
                                            val result = BluetoothPrinterHelper.printTestReceipt(dev, companyName, cashierName)
                                            showPrinterDialog = false
                                            showPrintResultDialog = result.second
                                        }
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = EmanagerBlue, modifier = Modifier.size(18.dp))
                                        Column {
                                            @Suppress("MissingPermission")
                                            Text(dev.name ?: "Bilinməyən Printer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(dev.address, fontSize = 11.sp, color = Slate500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = BluetoothPrinterHelper.printTestReceipt(null, companyName, cashierName)
                            showPrinterDialog = false
                            showPrintResultDialog = result.second
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue)
                ) {
                    Text("Test Çeki Çap Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrinterDialog = false }) { Text("Bağla") }
            }
        )
    }

    // 3. PRINT RESULT / PREVIEW DIALOG
    if (showPrintResultDialog != null) {
        AlertDialog(
            onDismissRequest = { showPrintResultDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmanagerGreen)
                    Text("Çap Nəticəsi", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = showPrintResultDialog!!,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrintResultDialog = null }) { Text("Tamam") }
            }
        )
    }

    // 4. LOGOUT CONFIRMATION DIALOG
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Sistemdən Çıxış", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("Sistemdən çıxmaq istədiyinizə əminsiniz?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmanagerRose)
                ) { Text("Çıxış Et") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) { Text("Ləğv") }
            }
        )
    }

    // MAIN SETTINGS LAYOUT
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP PROFILE BANNER
        Surface(
            color = EmanagerBlue,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cashierName.firstOrNull()?.uppercase() ?: "K",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(text = cashierName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text(text = companyName, fontSize = 12.sp, color = Color(0xFFDBEAFE))
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isErpConnected) EmanagerGreen.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isErpConnected) "e-Manager.az Aktiv" else "Oflayn Rejim",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SETTINGS ITEMS LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // SECTION 1: TƏHLÜKƏSİZLİK VƏ PIN
            item {
                Text("TƏHLÜKƏSİZLİK VƏ GİRİŞ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Default.LockReset,
                            title = "PIN Kodu Dəyişdir",
                            subtitle = "4 rəqəmli təhlükəsizlik şifrəsi",
                            onClick = { showChangePinDialog = true }
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmanagerBlue)
                                Column {
                                    Text("Barmaq İzi ilə Giriş", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Biometrik autentifikasiya", fontSize = 11.sp, color = Slate500)
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.setBiometricEnabled(it) }
                            )
                        }
                    }
                }
            }

            // SECTION: SAHİBKAR KABİNETİ
            item {
                Text("SAHİBKAR VƏ BİZNES İDARƏETMƏSİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Assessment,
                            title = "e-Manager Sahibkar Tətbiqi",
                            subtitle = "Debitorlar, Kreditorlar, Satış/Alış hesabatları və Daxili Çat",
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.emanager.business")
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    Toast.makeText(context, "e-Manager Sahibkar quraşdırılıb (Android Studio-dan 'business' modulunu seçib işə salın)", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                }
            }

            // SECTION 2: AVADANLIQ VƏ PRINTER
            item {
                Text("AVADANLIQ VƏ ÇAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Print,
                            title = "Bluetooth Çek Printeri",
                            subtitle = "Termal POS printerə qoşulma və test",
                            onClick = { showPrinterDialog = true }
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.6f))

                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "Test Çeki Çap Et",
                            subtitle = "Printer əlaqəsini yoxlayın",
                            onClick = {
                                coroutineScope.launch {
                                    val result = BluetoothPrinterHelper.printTestReceipt(null, companyName, cashierName)
                                    showPrintResultDialog = result.second
                                }
                            }
                        )
                    }
                }
            }

            // SECTION 3: ERP İNTREQASİYASI
            item {
                Text("ERP VƏ SİNXRONİZASİYA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Sync,
                            title = "İndi Sinxron Et",
                            subtitle = "Məhsul və əməliyyatları e-manager.az ilə yenilə",
                            onClick = {
                                viewModel.setupFirestoreSync()
                                Toast.makeText(context, "Məlumatlar yenilənir...", Toast.LENGTH_SHORT).show()
                            }
                        )

                        HorizontalDivider(color = Slate200.copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = EmanagerBlue)
                                Column {
                                    Text("Tenant ID", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(activeTenantId, fontSize = 11.sp, color = Slate500)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: TEMA VƏ GÖRÜNÜŞ
            item {
                Text("GÖRÜNÜŞ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Sistem", ThemeMode.SYSTEM, Icons.Default.BrightnessAuto),
                            Triple("Açıq", ThemeMode.LIGHT, Icons.Default.LightMode),
                            Triple("Qaranlıq", ThemeMode.DARK, Icons.Default.DarkMode)
                        ).forEach { (label, mode, icon) ->
                            val isSel = themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) EmanagerBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f).clickable { viewModel.setThemeMode(mode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION: PROQRAM YENİLƏNMƏSİ
            item {
                Text("SİSTEM YENİLƏNMƏSİ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate500)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        val res = com.example.myapplication.util.PosAppUpdateManager.checkForUpdates(forceSilent = false)
                                        if (res is com.example.myapplication.util.PosUpdateStatus.UpToDate) {
                                            android.widget.Toast.makeText(context, "POS artıq ən son versiyadadır (v${com.example.myapplication.BuildConfig.VERSION_NAME})", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmanagerBlue)
                                Column {
                                    Text("Yenilənmələri Yoxla", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("v${com.example.myapplication.BuildConfig.VERSION_NAME} • GitHub-dan yenilə", fontSize = 11.sp, color = Slate500)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // SECTION 5: ÇIXIŞ
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmanagerRose.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmanagerRose.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().clickable { showLogoutConfirmDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = EmanagerRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sistemdən Çıxış", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmanagerRose)
                    }
                }
            }
        }
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
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = EmanagerBlue, modifier = Modifier.size(22.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = Slate500)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
    }
}
