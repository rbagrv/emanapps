package com.example.business.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.data.model.WarehouseItem
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(
    viewModel: BusinessViewModel,
    onNavigateToAddProduct: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items by viewModel.warehouseItems.collectAsState()
    val summary by viewModel.warehouseSummary.collectAsState()
    val allMovements by viewModel.stockMovements.collectAsState()
    val searchQuery by viewModel.warehouseSearchQuery.collectAsState()
    val hideNegativeStock by viewModel.hideNegativeStock.collectAsState()
    val hideZeroStock by viewModel.hideZeroStock.collectAsState()
    val onlyLowStock by viewModel.onlyLowStock.collectAsState()
    val context = LocalContext.current
    var selectedItemForDetails by remember { mutableStateOf<WarehouseItem?>(null) }
    var sortOrder by remember { mutableStateOf("name_asc") }

    val sortedItems = remember(items, sortOrder) {
        when (sortOrder) {
            "stock_asc" -> items.sortedBy { it.stock }
            "val_desc" -> items.sortedByDescending { it.totalStockValue }
            else -> items.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Anbar və Ehtiyatlar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "e-manager.az ERP Canlı Qalıqlar (Müşahidə)",
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = StringBuilder().apply {
                            append("📦 *e-Manager Anbar Qalıq İcmalı*\n\n")
                            append("• Cəmi çeşid: ${summary.totalProductsCount} ədəd\n")
                            append("• Ümumi miqdar: ${String.format(Locale.US, "%,.0f", summary.totalStockQuantity)}\n")
                            append("• Anbar dəyəri (Satış): ₼${String.format(Locale.US, "%,.2f", summary.totalStockSaleValue)}\n")
                            append("• Anbar dəyəri (Maya): ₼${String.format(Locale.US, "%,.2f", summary.totalStockCostValue)}\n\n")
                            if (items.any { it.isLowStock }) {
                                append("⚠️ *Kritik Qalıqlı Məhsullar:*\n")
                                items.filter { it.isLowStock }.forEach {
                                    append("- ${it.name}: ${it.stock} ${it.unit}\n")
                                }
                            }
                        }.toString()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Anbar İcmalını Paylaş"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Anbarı Paylaş",
                            tint = EmeraldPrimary
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // 1. KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Cəmi Məhsul", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${summary.totalProductsCount} çeşid",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Miqdar: ${String.format(Locale.US, "%,.0f", summary.totalStockQuantity)}",
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Anbar Dəyəri (Satış)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "₼ ${String.format(Locale.US, "%,.2f", summary.totalStockSaleValue)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "Maya: ₼ ${String.format(Locale.US, "%,.2f", summary.totalStockCostValue)}",
                                fontSize = 11.sp,
                                color = WarningOrange
                            )
                        }
                    }
                }
            }

            // 2. Low stock alert banner if any
            if (summary.lowStockCount > 0 || summary.outOfStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange)
                            Column {
                                Text(
                                    text = "Ehtiyat Xəbərdarlığı",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "${summary.lowStockCount} məhsulun qalığı kritik səviyyədədir, ${summary.outOfStockCount} məhsul isə bitib.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Search Box
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.warehouseSearchQuery.value = it },
                    placeholder = { Text("Məhsul adı və ya barkod ilə axtar...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.warehouseSearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Təmizlə")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // 3.1 Filters: Hide Negative, Hide Zero, Low Stock
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = hideNegativeStock,
                        onClick = { viewModel.hideNegativeStock.value = !hideNegativeStock },
                        label = { Text("Mənfiləri Gizlət", fontSize = 11.sp) },
                        leadingIcon = if (hideNegativeStock) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )

                    FilterChip(
                        selected = hideZeroStock,
                        onClick = { viewModel.hideZeroStock.value = !hideZeroStock },
                        label = { Text("0 Qalıqları Gizlət", fontSize = 11.sp) },
                        leadingIcon = if (hideZeroStock) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )

                    FilterChip(
                        selected = onlyLowStock,
                        onClick = { viewModel.onlyLowStock.value = !onlyLowStock },
                        label = { Text("Kritik Qalıq", fontSize = 11.sp) },
                        leadingIcon = if (onlyLowStock) {
                            { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = WarningOrange) }
                        } else null
                    )
                }
            }

            // 3.2 Sort Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == "name_asc",
                        onClick = { sortOrder = "name_asc" },
                        label = { Text("Ada görə A-Z", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = sortOrder == "stock_asc",
                        onClick = { sortOrder = "stock_asc" },
                        label = { Text("Qalıq ↑", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    )
                    FilterChip(
                        selected = sortOrder == "val_desc",
                        onClick = { sortOrder = "val_desc" },
                        label = { Text("Dəyər ↓", fontSize = 11.sp) }
                    )
                }
            }

            // 4. Products List
            if (sortedItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Anbarda məhsul tapılmadı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "ERP sistemində qeydiyyatdan keçmiş məhsullar burada canlı göstərilir.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateToAddProduct,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Yeni Məhsul Səhifəsi Aç")
                            }
                        }
                    }
                }
            } else {
                items(sortedItems, key = { it.id }) { item ->
                    WarehouseItemCard(
                        item = item,
                        onClickItem = { selectedItemForDetails = item }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // PRODUCT DETAILS & STOCK MOVEMENTS DIALOG (OBSERVATION ONLY)
    selectedItemForDetails?.let { targetItem ->
        val itemMovements = remember(targetItem, allMovements) {
            allMovements.filter { m ->
                m.productName.contains(targetItem.name, ignoreCase = true) ||
                        (targetItem.sku.isNotBlank() && m.productName.contains(targetItem.sku, ignoreCase = true)) ||
                        (targetItem.barcode != null && m.documentNumber.contains(targetItem.barcode))
            }
        }

        AlertDialog(
            onDismissRequest = { selectedItemForDetails = null },
            title = {
                Column {
                    Text(text = targetItem.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        text = "Kateqoriya: ${targetItem.categoryName} • ${targetItem.unit}",
                        fontSize = 12.sp,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPI Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Faktiki Qalıq", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", targetItem.stock)} ${targetItem.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (targetItem.stock <= 0) LossRed else EmeraldPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Satış Qiyməti", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "₼ ${String.format(Locale.US, "%.2f", targetItem.salePrice)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Stock movements history
                    Text(
                        text = "📦 Bu Məhsul Üzrə Hərəkətlər (${itemMovements.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (itemMovements.isEmpty()) {
                        Text(
                            text = "Son əməliyyatlarda bu məhsul üzrə hərəkət qeydə alınmayıb.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(itemMovements) { m ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${m.movementType.title} • ${m.date}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (m.movementType == com.example.business.data.model.StockMovementType.PURCHASE) EmeraldPrimary else InfoBlue
                                            )
                                            Text(
                                                text = m.partnerOrChannel,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val sign = if (m.movementType == com.example.business.data.model.StockMovementType.PURCHASE) "+" else "-"
                                            Text(
                                                text = "$sign${m.quantity} ${m.unit}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (m.movementType == com.example.business.data.model.StockMovementType.PURCHASE) EmeraldPrimary else InfoBlue
                                            )
                                            Text(
                                                text = "₼ ${String.format(Locale.US, "%.2f", m.totalPrice)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
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
                    onClick = { selectedItemForDetails = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Bağla")
                }
            }
        )
    }
}

@Composable
fun WarehouseItemCard(
    item: WarehouseItem,
    onClickItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickItem() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (item.stock <= 0) LossRed.copy(alpha = 0.15f)
                else if (item.isLowStock) WarningOrange.copy(alpha = 0.15f)
                else EmeraldPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.stock <= 0) Icons.Default.HighlightOff
                        else if (item.isLowStock) Icons.Default.ReportProblem
                        else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (item.stock <= 0) LossRed else if (item.isLowStock) WarningOrange else EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Satış: ₼ ${String.format(Locale.US, "%,.2f", item.salePrice)} • Maya: ₼ ${String.format(Locale.US, "%,.2f", item.costPrice)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!item.barcode.isNullOrBlank()) {
                    Text(
                        text = "Barkod: ${item.barcode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (item.salePrice > item.costPrice && item.costPrice > 0.0) {
                    val marginPct = (((item.salePrice - item.costPrice) / item.salePrice) * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ProfitGreen.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "+$marginPct% mənfəət marjası",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.stock} ${item.unit}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.stock <= 0) LossRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Dəyər: ₼ ${String.format(Locale.US, "%,.2f", item.totalStockValue)}",
                    fontSize = 11.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

