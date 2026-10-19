package com.example.business.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.data.model.DebtStatus
import com.example.business.data.model.Debtor
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.EmeraldPrimary
import com.example.business.ui.theme.InfoBlue
import com.example.business.ui.theme.LossRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorsScreen(
    viewModel: BusinessViewModel,
    onNavigateToAddDebtor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtors by viewModel.filteredDebtors.collectAsState()
    val searchQuery by viewModel.debtorSearchQuery.collectAsState()
    val filterStatus by viewModel.debtorFilterStatus.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val context = LocalContext.current

    var selectedDebtorForPayment by remember { mutableStateOf<Debtor?>(null) }
    var selectedDebtorForDetails by remember { mutableStateOf<Debtor?>(null) }
    var selectedDebtorForDebtOrigin by remember { mutableStateOf<Debtor?>(null) }
    var sortOrder by remember { mutableStateOf("debt_desc") }

    val sortedDebtors = remember(debtors, sortOrder) {
        when (sortOrder) {
            "debt_desc" -> debtors.sortedByDescending { it.totalDebt }
            "name_asc" -> debtors.sortedBy { it.fullName.lowercase(Locale.ROOT) }
            "overdue_first" -> debtors.sortedWith(compareByDescending<Debtor> { it.status == DebtStatus.OVERDUE }.thenByDescending { it.totalDebt })
            else -> debtors
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Debitorlar (Müştəri Borcları)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAddDebtor) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Yeni Debitor Səhifəsi",
                            tint = EmeraldPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddDebtor,
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Debitor")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Total Debt Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ümumi Alacaq Borclar",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₼ ${String.format(Locale.US, "%,.2f", summary.totalReceivables)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${debtors.size} debitor • ${debtors.count { it.status == DebtStatus.OVERDUE }} gecikmiş",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Gecikmiş Məbləğ",
                            fontSize = 12.sp,
                            color = LossRed
                        )
                        Text(
                            text = "₼ ${String.format(Locale.US, "%,.2f", summary.overdueReceivables)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.debtorSearchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Müştəri adı, şirkət və ya telefon...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.debtorSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Təmizlə")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Status & Sorting Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { viewModel.debtorFilterStatus.value = null },
                    label = { Text("Hamısı", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterStatus == DebtStatus.ACTIVE,
                    onClick = { viewModel.debtorFilterStatus.value = DebtStatus.ACTIVE },
                    label = { Text("Aktiv", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = filterStatus == DebtStatus.OVERDUE,
                    onClick = { viewModel.debtorFilterStatus.value = DebtStatus.OVERDUE },
                    label = { Text("Gecikmiş", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = sortOrder == "debt_desc",
                    onClick = {
                        sortOrder = if (sortOrder == "debt_desc") "name_asc" else "debt_desc"
                    },
                    label = {
                        Text(if (sortOrder == "debt_desc") "Borca görə ↓" else "Ada görə A-Z", fontSize = 11.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Debtor List
            if (sortedDebtors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Debitor tapılmadı",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToAddDebtor,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yeni Debitor Səhifəsi Aç")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedDebtors, key = { it.id }) { debtor ->
                        DebtorCard(
                            debtor = debtor,
                            onCardClick = { selectedDebtorForDetails = debtor },
                            onDebtClick = { selectedDebtorForDebtOrigin = debtor },
                            onPayClick = { selectedDebtorForPayment = debtor },
                            onCallClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${debtor.phone}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(70.dp)) }
                }
            }
        }
    }

    // PAYMENT DIALOG
    selectedDebtorForPayment?.let { debtor ->
        var payAmount by remember { mutableStateOf("") }
        var payType by remember { mutableStateOf("Nağd") }

        AlertDialog(
            onDismissRequest = { selectedDebtorForPayment = null },
            title = {
                Text(
                    text = "Borc Ödənişini Qəbul Et",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = debtor.fullName, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Qalıq borc: ₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Ödənilən Məbləğ (₼)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Nağd", "Kart", "Köçürmə").forEach { type ->
                            FilterChip(
                                selected = payType == type,
                                onClick = { payType = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.recordDebtorPayment(debtor.id, amount, payType)
                            selectedDebtorForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Təsdiq Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtorForPayment = null }) {
                    Text("Ləğv et")
                }
            }
        )
    }

    // DEBT ORIGIN DATE DIALOG
    selectedDebtorForDebtOrigin?.let { debtor ->
        AlertDialog(
            onDismissRequest = { selectedDebtorForDebtOrigin = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Borcun Yaranma Tarixi & Faktura",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = debtor.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow("Borcun Yaranma Tarixi:", debtor.debtStartDate)
                            DetailRow("Faktura / Qaimə:", debtor.invoiceNumber)
                            DetailRow("Cari Qalıq Borc:", "₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)}")
                            DetailRow("Son Ödəmə Müddəti:", debtor.dueDate)
                            DetailRow("Status:", debtor.status.label)
                            debtor.lastPaymentDate?.let {
                                DetailRow("Son Ödəniş Tarixi:", it)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "Hörmətli ${debtor.fullName}, e-Manager ERP məlumatı: Sizin ${debtor.debtStartDate} tarixində yaranmış ₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)} məbləğində borcunuz mövcuddur. Zəhmət olmasa ödənişi təmin edəsiniz."
                        val cleanPhone = debtor.phone.replace("+", "").replace(" ", "").replace("-", "")
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(shareText)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(generalIntent, "WhatsApp ilə Paylaş"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp ilə Xatırlat")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDebtorForDebtOrigin = null }) {
                    Text("Bağla")
                }
            }
        )
    }

    // DETAILS DIALOG
    selectedDebtorForDetails?.let { debtor ->
        AlertDialog(
            onDismissRequest = { selectedDebtorForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                    Text(text = debtor.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    debtor.companyName?.let { DetailRow("Şirkət/Müəssisə:", it) }
                    DetailRow("Telefon:", debtor.phone)
                    DetailRow("Borcun Yaranma Tarixi:", debtor.debtStartDate)
                    DetailRow("Faktura №:", debtor.invoiceNumber)
                    DetailRow("Qalıq Borc:", "₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)}")
                    DetailRow("Kredit Limiti:", "₼ ${String.format(Locale.US, "%,.2f", debtor.creditLimit)}")
                    DetailRow("Son Tarix:", debtor.dueDate)
                    DetailRow("Status:", debtor.status.label)
                    if (debtor.notes.isNotBlank()) {
                        DetailRow("Qeydlər:", debtor.notes)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDebtorForDetails = null }) {
                    Text("Bağla")
                }
            }
        )
    }
}

@Composable
fun DebtorCard(
    debtor: Debtor,
    onCardClick: () -> Unit,
    onDebtClick: () -> Unit,
    onPayClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debtor.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    debtor.companyName?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onDebtClick,
                    shape = RoundedCornerShape(8.dp),
                    color = (if (debtor.status == DebtStatus.OVERDUE) LossRed else EmeraldPrimary).copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (debtor.status == DebtStatus.OVERDUE) LossRed else EmeraldPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarixə bax",
                                modifier = Modifier.size(13.dp),
                                tint = if (debtor.status == DebtStatus.OVERDUE) LossRed else EmeraldPrimary
                            )
                        }
                        Text(
                            text = "Yaranma: ${debtor.debtStartDate}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable { onCallClick() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Zəng et",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = debtor.phone,
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        onClick = {
                            val cleanPhone = debtor.phone.replace("+", "").replace(" ", "").replace("-", "")
                            val msg = "Hörmətli ${debtor.fullName}, son qalıq borcunuz: ₼${String.format(Locale.US, "%,.2f", debtor.totalDebt)}. Son ödəmə tarixi: ${debtor.dueDate}. Nəzərə almağınızı xahiş edirik."
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp açıla bilmədi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp Borc Xatırlatması",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(14.dp)
                            )
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E8E44))
                        }
                    }
                }

                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ödəniş Al", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
