package com.example.myapplication.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.CartItem
import com.example.myapplication.data.CashOperation
import com.example.myapplication.data.Product
import com.example.myapplication.data.TransactionRecord
import com.example.myapplication.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    customerName: String,
    subtotalAmount: Double,
    discountPercent: Double,
    discountTotal: Double,
    totalAmount: Double,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onToggleReturn: (String) -> Unit,
    onSetItemDiscount: (String, Double) -> Unit,
    onHoldOrder: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aktiv Səbət (${cartItems.sumOf { it.quantity }} məhsul)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Alıcı: $customerName",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onHoldOrder()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, tint = EmanagerOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Saxla", fontSize = 12.sp, color = EmanagerOrange, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (item.isReturn) EmanagerRose.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            if (item.isReturn) EmanagerRose.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (item.isReturn) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = EmanagerRose.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = "İADƏ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = EmanagerRose,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.2f ₼ × %d %s", item.unitPriceAfterDiscount, item.quantity, item.product.unit),
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f ₼", item.totalPrice),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = if (item.isReturn) EmanagerRose else EmanagerBlue
                                    )
                                    IconButton(
                                        onClick = { onRemove(item.product.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = Slate400, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Controls Row: Return toggle, Item Discount pills, Quantity stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        onClick = { onToggleReturn(item.product.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (item.isReturn) EmanagerRose else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (item.isReturn) EmanagerRose else MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text(
                                            text = if (item.isReturn) "İadə ✓" else "İadə",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isReturn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                                        val isSelected = item.discountPercent == disc
                                        Surface(
                                            onClick = { onSetItemDiscount(item.product.id, disc) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) EmanagerOrange else MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = if (disc == 0.0) "0%" else "-${disc.toInt()}%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Slate500,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        onClick = { onDecrement(item.product.id) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Surface(
                                        onClick = { onIncrement(item.product.id) },
                                        shape = CircleShape,
                                        color = EmanagerBlue,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Totals
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("İlkin Cəmi:", fontSize = 13.sp, color = Slate500)
                Text(String.format(Locale.US, "%.2f ₼", subtotalAmount), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            if (discountTotal > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Səbət Endirimi (${discountPercent.toInt()}%):", fontSize = 13.sp, color = EmanagerOrange)
                    Text(String.format(Locale.US, "-%.2f ₼", discountTotal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmanagerOrange)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Yekun Məbləğ:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = String.format(Locale.US, "%.2f ₼", totalAmount),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = EmanagerBlue
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onDismiss()
                    onProceedToCheckout()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ödənişə Keç", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun ModernProductListRow(
    product: Product,
    quantityInCart: Int,
    onAddToCart: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (quantityInCart > 0) EmanagerBlue.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        shadowElevation = if (quantityInCart > 0) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddToCart() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Sevimli",
                        tint = if (product.isFavorite) EmanagerOrange else Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.category,
                            fontSize = 10.sp,
                            color = EmanagerBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("•", fontSize = 10.sp, color = Slate400)
                        Text(
                            text = "Stok: ${product.stockQuantity.toInt()} ${product.unit}",
                            fontSize = 10.sp,
                            color = if (product.stockQuantity <= 5) EmanagerRose else EmanagerGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = String.format(Locale.US, "%.2f ₼", product.price),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = EmanagerBlue
                )

                if (quantityInCart > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            onClick = onDecrement,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(14.dp))
                            }
                        }
                        Text("$quantityInCart", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Surface(
                            onClick = onIncrement,
                            shape = CircleShape,
                            color = EmanagerBlue,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onAddToCart,
                        shape = CircleShape,
                        color = EmanagerBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "Əlavə et", tint = EmanagerBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ZReportDialog(
    companyName: String,
    cashierName: String,
    transactions: List<TransactionRecord>,
    cashOperations: List<CashOperation>,
    cashBalance: Double,
    context: Context,
    onDismiss: () -> Unit
) {
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todaySales = remember(transactions) {
        transactions.filter { (it.type == "sale" || it.type.isBlank()) && it.timestamp >= startOfToday }
    }
    val todayReturns = remember(transactions) {
        transactions.filter { (it.type == "sale_return" || it.type == "return") && it.timestamp >= startOfToday }
    }
    val todayCashOps = remember(cashOperations) {
        cashOperations.filter { it.timestamp >= startOfToday }
    }

    val grossSales = todaySales.sumOf { it.totalAmount }
    val returnsTotal = todayReturns.sumOf { kotlin.math.abs(it.totalAmount) }
    val netSales = (grossSales - returnsTotal).coerceAtLeast(0.0)

    val cashSales = todaySales.filter { it.paymentType.contains("Nağd", ignoreCase = true) || it.paymentType.contains("Qarışıq", ignoreCase = true) }
        .sumOf { if (it.cashPaid > 0) (it.cashPaid - it.changeGiven).coerceAtLeast(0.0) else it.totalAmount }
    val cardSales = todaySales.filter { it.paymentType.contains("Kart", ignoreCase = true) || it.paymentType.contains("Qarışıq", ignoreCase = true) }
        .sumOf { if (it.cardPaid > 0) it.cardPaid else if (it.paymentType.contains("Kart", ignoreCase = true)) it.totalAmount else 0.0 }
    val creditSales = todaySales.filter { it.paymentType.contains("Nisyə", ignoreCase = true) }
        .sumOf { it.totalAmount }

    val todayIncomes = todayCashOps.filter { it.type == "income" }.sumOf { it.amount }
    val todayExpenses = todayCashOps.filter { it.type == "expense" }.sumOf { it.amount }
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

    val reportText = remember(grossSales, returnsTotal, cashSales, cardSales, creditSales, todayIncomes, todayExpenses, cashBalance) {
        buildString {
            appendLine("================================")
            appendLine("   $companyName - Z-HESABAT")
            appendLine("================================")
            appendLine("Tarix: $dateStr")
            appendLine("Kassir: $cashierName")
            appendLine("--------------------------------")
            appendLine("Satış çeki sayı: ${todaySales.size}")
            appendLine("Ümumi Satış: ${String.format(Locale.US, "%.2f ₼", grossSales)}")
            appendLine("İadələr (${todayReturns.size}): -${String.format(Locale.US, "%.2f ₼", returnsTotal)}")
            appendLine("XALİS SATIŞ: ${String.format(Locale.US, "%.2f ₼", netSales)}")
            appendLine("--------------------------------")
            appendLine("ÖDƏNİŞ BÖLGÜSÜ:")
            appendLine("• Nağd: ${String.format(Locale.US, "%.2f ₼", cashSales)}")
            appendLine("• Kart: ${String.format(Locale.US, "%.2f ₼", cardSales)}")
            appendLine("• Nisyə: ${String.format(Locale.US, "%.2f ₼", creditSales)}")
            appendLine("--------------------------------")
            appendLine("KASSA HƏRƏKƏTİ (Bugün):")
            appendLine("• Mədaxil: +${String.format(Locale.US, "%.2f ₼", todayIncomes)}")
            appendLine("• Məxaric: -${String.format(Locale.US, "%.2f ₼", todayExpenses)}")
            appendLine("• Cari Kassa Qalığı: ${String.format(Locale.US, "%.2f ₼", cashBalance)}")
            appendLine("================================")
            appendLine("e-Manager POS Terminal")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmanagerBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = EmanagerBlue, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text("Gün Sonu Z-Hesabatı", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(dateStr, fontSize = 11.sp, color = Slate500)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Net Sales Highlight Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(EmanagerBlue, EmanagerIndigo)))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bugünkü Xalis Satış", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = String.format(Locale.US, "%.2f ₼", netSales),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "${todaySales.size} qəbz",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Breakdown rows
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ZReportRow("Nağd Satışlar", cashSales, EmanagerGreen)
                        ZReportRow("Kart (POS-Terminal)", cardSales, EmanagerBlue)
                        ZReportRow("Nisyə Satışlar", creditSales, EmanagerOrange)
                        if (returnsTotal > 0) {
                            ZReportRow("İadələr (${todayReturns.size})", -returnsTotal, EmanagerRose)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ZReportRow("Kassa Mədaxil (Bugün)", todayIncomes, EmanagerGreen)
                        ZReportRow("Kassa Məxaric (Bugün)", -todayExpenses, EmanagerRose)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cari Kassa Qalığı", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.2f ₼", cashBalance),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = EmanagerBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, reportText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Z-Hesabatı Paylaş")
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmanagerBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paylaş")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Bağla") }
        }
    )
}

@Composable
private fun ZReportRow(label: String, amount: Double, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = String.format(Locale.US, "%.2f ₼", amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
