package com.example.business.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Status Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { viewModel.debtorFilterStatus.value = null },
                    label = { Text("Hamısı") }
                )
                FilterChip(
                    selected = filterStatus == DebtStatus.ACTIVE,
                    onClick = { viewModel.debtorFilterStatus.value = DebtStatus.ACTIVE },
                    label = { Text("Aktiv") }
                )
                FilterChip(
                    selected = filterStatus == DebtStatus.OVERDUE,
                    onClick = { viewModel.debtorFilterStatus.value = DebtStatus.OVERDUE },
                    label = { Text("Gecikmiş") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Debtor List
            if (debtors.isEmpty()) {
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
                    items(debtors, key = { it.id }) { debtor ->
                        DebtorCard(
                            debtor = debtor,
                            onCardClick = { selectedDebtorForDetails = debtor },
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
    onPayClick: () -> Unit,
    onCallClick: () -> Unit
) {
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

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₼ ${String.format(Locale.US, "%,.2f", debtor.totalDebt)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (debtor.status == DebtStatus.OVERDUE) LossRed else EmeraldPrimary
                    )
                    Text(
                        text = debtor.status.label,
                        fontSize = 11.sp,
                        color = if (debtor.status == DebtStatus.OVERDUE) LossRed else InfoBlue
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onCallClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
