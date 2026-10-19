package com.example.business.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.business.data.model.Supplier
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.EmeraldPrimary
import com.example.business.ui.theme.WarningOrange
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: BusinessViewModel,
    onNavigateToAddSupplier: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.filteredSuppliers.collectAsState()
    val searchQuery by viewModel.supplierSearchQuery.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val context = LocalContext.current

    var selectedSupplierForPayment by remember { mutableStateOf<Supplier?>(null) }
    var selectedSupplierForDetails by remember { mutableStateOf<Supplier?>(null) }
    var selectedSupplierForDebtOrigin by remember { mutableStateOf<Supplier?>(null) }
    var sortOrder by remember { mutableStateOf("debt_desc") }

    val sortedSuppliers = remember(suppliers, sortOrder) {
        when (sortOrder) {
            "debt_desc" -> suppliers.sortedByDescending { it.totalPayable }
            "name_asc" -> suppliers.sortedBy { it.companyName.lowercase(Locale.ROOT) }
            else -> suppliers
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Təchizatçılar (Kreditorlar)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAddSupplier) {
                        Icon(
                            imageVector = Icons.Default.AddBusiness,
                            contentDescription = "Yeni Təchizatçı Səhifəsi",
                            tint = EmeraldPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddSupplier,
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Təchizatçı")
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
            // 1. Total Payables Card
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
                            text = "Təchizatçılara Ümumi Borc",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₼ ${String.format(Locale.US, "%,.2f", summary.totalPayables)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${suppliers.size} Şirkət",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.supplierSearchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Təchizatçı şirkət və ya nümayəndə...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.supplierSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Təmizlə")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2.1 Sorting Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = sortOrder == "debt_desc",
                    onClick = { sortOrder = "debt_desc" },
                    label = { Text("Borca görə ↓", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                FilterChip(
                    selected = sortOrder == "name_asc",
                    onClick = { sortOrder = "name_asc" },
                    label = { Text("Ada görə A-Z", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Supplier List
            if (sortedSuppliers.isEmpty()) {
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
                            text = "Təchizatçı tapılmadı",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToAddSupplier,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yeni Təchizatçı Səhifəsi Aç")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedSuppliers, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            onCardClick = { selectedSupplierForDetails = supplier },
                            onDebtClick = { selectedSupplierForDebtOrigin = supplier },
                            onPayClick = { selectedSupplierForPayment = supplier },
                            onCallClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${supplier.phone}")
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
    selectedSupplierForPayment?.let { supplier ->
        var payAmount by remember { mutableStateOf("") }
        var payNote by remember { mutableStateOf("Bank köçürməsi") }

        AlertDialog(
            onDismissRequest = { selectedSupplierForPayment = null },
            title = {
                Text(
                    text = "Təchizatçıya Ödəniş Köçür",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = supplier.companyName, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Cari borc: ₼ ${String.format(Locale.US, "%,.2f", supplier.totalPayable)}",
                        color = WarningOrange,
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

                    OutlinedTextField(
                        value = payNote,
                        onValueChange = { payNote = it },
                        label = { Text("Qeyd / Təyinat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.recordSupplierPayment(supplier.id, amount, payNote)
                            selectedSupplierForPayment = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Təsdiq Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSupplierForPayment = null }) {
                    Text("Ləğv et")
                }
            }
        )
    }

    // SUPPLIER DEBT ORIGIN DIALOG
    selectedSupplierForDebtOrigin?.let { supplier ->
        AlertDialog(
            onDismissRequest = { selectedSupplierForDebtOrigin = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = WarningOrange,
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
                    Text(text = supplier.companyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow("Borcun Yaranma Tarixi:", supplier.debtStartDate)
                            DetailRow("Cari Qalıq Borcumuz:", "₼ ${String.format(Locale.US, "%,.2f", supplier.totalPayable)}")
                            DetailRow("Təchizat Kateqoriyası:", supplier.category)
                            DetailRow("Əlaqədar Şəxs:", supplier.contactPerson)
                            DetailRow("Telefon:", supplier.phone)
                            DetailRow("Son Təhvil Tarixi:", supplier.lastDeliveryDate)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareText = "Salam, ${supplier.companyName} təmsilçisi. ${supplier.debtStartDate} tarixində yaranmış ₼ ${String.format(Locale.US, "%,.2f", supplier.totalPayable)} məbləğində hesablaşma barədə əlaqə saxlayırıq."
                        val cleanPhone = supplier.phone.replace("+", "").replace(" ", "").replace("-", "")
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
                    Text("WhatsApp ilə Yaz")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSupplierForDebtOrigin = null }) {
                    Text("Bağla")
                }
            }
        )
    }

    // DETAILS DIALOG
    selectedSupplierForDetails?.let { supplier ->
        AlertDialog(
            onDismissRequest = { selectedSupplierForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = EmeraldPrimary)
                    Text(text = supplier.companyName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Nümayəndə:", supplier.contactPerson)
                    DetailRow("Telefon:", supplier.phone)
                    supplier.email?.let { DetailRow("E-poçt:", it) }
                    DetailRow("Kateqoriya:", supplier.category)
                    DetailRow("Borcun Yaranma Tarixi:", supplier.debtStartDate)
                    DetailRow("Qalıq Borc:", "₼ ${String.format(Locale.US, "%,.2f", supplier.totalPayable)}")
                    DetailRow("Status:", supplier.lastDeliveryDate)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSupplierForDetails = null }) {
                    Text("Bağla")
                }
            }
        )
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
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
                        text = supplier.companyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${supplier.contactPerson} • ${supplier.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = onDebtClick,
                    shape = RoundedCornerShape(8.dp),
                    color = WarningOrange.copy(alpha = 0.12f),
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
                                text = "₼ ${String.format(Locale.US, "%,.2f", supplier.totalPayable)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WarningOrange
                            )
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Tarixə bax",
                                modifier = Modifier.size(13.dp),
                                tint = WarningOrange
                            )
                        }
                        Text(
                            text = "Yaranma: ${supplier.debtStartDate}",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = supplier.phone,
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        onClick = {
                            val cleanPhone = supplier.phone.replace("+", "").replace(" ", "").replace("-", "")
                            val msg = "Salam, ${supplier.companyName} təmsilçisi. Cari qalıq borcumuz: ₼${String.format(Locale.US, "%,.2f", supplier.totalPayable)}. Hesablaşma üzrə əlaqə saxlayırıq."
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val generalIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(generalIntent, "WhatsApp ilə Paylaş"))
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
                                contentDescription = "WhatsApp",
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
                    Text("Ödəniş Et", fontSize = 12.sp)
                }
            }
        }
    }
}
