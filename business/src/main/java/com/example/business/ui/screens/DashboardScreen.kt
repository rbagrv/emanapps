package com.example.business.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.business.data.model.*
import com.example.business.ui.BusinessViewModel
import com.example.business.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BusinessViewModel,
    onNavigateToDebtors: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val smartInsights by viewModel.smartInsights.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val warehouseSummary by viewModel.warehouseSummary.collectAsState()
    val context = LocalContext.current

    // Real dynamic business health calculations
    val realMargin = if (summary.monthSales > 0) (summary.monthProfit / summary.monthSales * 100.0).coerceIn(0.0, 100.0) else 0.0
    val totalCashAndBank = summary.cashInHand + summary.bankBalance
    val liquidityRatio = if (summary.totalPayables > 0) (totalCashAndBank / summary.totalPayables) else if (totalCashAndBank > 0) 3.0 else 1.0
    val overdueRatio = if (summary.totalReceivables > 0) (summary.overdueReceivables / summary.totalReceivables).coerceIn(0.0, 1.0) else 0.0
    val collectionRate = ((1.0 - overdueRatio) * 100).toInt()
    val healthScore = ((if (liquidityRatio >= 1.0) 40 else (liquidityRatio * 40).toInt()) +
                       (realMargin * 0.35).toInt().coerceIn(0, 35) +
                       (collectionRate * 0.25).toInt().coerceIn(0, 25)).coerceIn(10, 100)
    val healthProgress = healthScore / 100f
    val healthGrade = when {
        healthScore >= 85 -> "Əla"
        healthScore >= 70 -> "Yaxşı"
        healthScore >= 50 -> "Orta"
        else -> "Diqqət tələb edir"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.openTenantSelector() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = companyName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Müəssisəni dəyiş",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "VÖEN: ${summary.voen} • e-manager.az ERP Canlı",
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshData()
                        Toast.makeText(context, "Məlumatlar ERP-dən yeniləndi", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenilə",
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.openTenantSelector() }) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Obyekt seçimi",
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Tənzimləmələr",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.lockApp() }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kilidlə",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            // 1. TOP CARDS (Sales & Purchases)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Bu Gün Satış",
                        amount = "₼ ${String.format(Locale.US, "%,.2f", summary.todaySales)}",
                        subtitle = "Aylıq: ₼ ${String.format(Locale.US, "%,.0f", summary.monthSales)}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconBg = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    KpiStatCard(
                        title = "Bu Gün Alış",
                        amount = "₼ ${String.format(Locale.US, "%,.2f", summary.todayPurchases)}",
                        subtitle = "Aylıq: ₼ ${String.format(Locale.US, "%,.0f", summary.monthPurchases)}",
                        icon = Icons.Default.ShoppingCart,
                        iconBg = WarningOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. PROFIT & BALANCES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Aylıq Mənfəət",
                        amount = "₼ ${String.format(Locale.US, "%,.2f", summary.monthProfit)}",
                        subtitle = "Xalis marja: ${String.format(Locale.US, "%.1f", realMargin)}%",
                        icon = Icons.Default.MonetizationOn,
                        iconBg = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )

                    KpiStatCard(
                        title = "Kassa & Bank",
                        amount = "₼ ${String.format(Locale.US, "%,.0f", summary.cashInHand + summary.bankBalance)}",
                        subtitle = "Kassa: ₼ ${String.format(Locale.US, "%,.0f", summary.cashInHand)} | Bank: ₼ ${String.format(Locale.US, "%,.0f", summary.bankBalance)}",
                        icon = Icons.Default.AccountBalance,
                        iconBg = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2.05 DETAILED CASH & BANK ACCOUNTS WITH NAMES & BALANCES
            if (summary.accountBalances.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = InfoBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Kassa və Bank Hesabları (D1 ERP)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = InfoBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${summary.accountBalances.size} hesab",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = InfoBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                summary.accountBalances.forEach { acc ->
                                    val isCash = acc.type == "Kassa"
                                    val badgeColor = if (isCash) EmeraldPrimary else InfoBlue

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
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
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = badgeColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isCash) Icons.Default.Payments else Icons.Default.AccountBalance,
                                                            contentDescription = null,
                                                            tint = badgeColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = acc.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Hesab kodu: ${acc.code} • ${acc.type}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "₼ ${String.format(Locale.US, "%,.2f", acc.balance)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (acc.balance > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2.1 BUSINESS HEALTH INDEX CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                                Text("Müəssisənin Sağlamlıq İndeksi", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (healthScore >= 70) EmeraldPrimary else WarningOrange).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$healthScore / 100 • $healthGrade",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (healthScore >= 70) EmeraldPrimary else WarningOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { healthProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (healthScore >= 70) EmeraldPrimary else WarningOrange,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Likvidlik: ${String.format(Locale.US, "%.1f", liquidityRatio)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rentabellik: ${String.format(Locale.US, "%.1f", realMargin)}%", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Medium)
                            Text("Yığım nisbəti: $collectionRate%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 2.2 CRITICAL LOW STOCK WARNING BANNER
            if (warehouseSummary.lowStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kritik Anbar Xəbərdarlığı",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "${warehouseSummary.lowStockCount} məhsulun anbar qalığı tükənmək üzrədir.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = onNavigateToSuppliers,
                                colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sifariş Et", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. DEBTORS & SUPPLIERS OVERVIEW BANNER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Borc və Hesablaşma Balansı",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.clickable { onNavigateToDebtors() }) {
                                Text(
                                    text = "Debitorlar (Alacaqlar)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₼ ${String.format(Locale.US, "%,.2f", summary.totalReceivables)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InfoBlue
                                )
                                Text(
                                    text = "Gecikmiş: ₼ ${String.format(Locale.US, "%,.2f", summary.overdueReceivables)}",
                                    fontSize = 11.sp,
                                    color = LossRed
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.height(44.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Column(modifier = Modifier.clickable { onNavigateToSuppliers() }) {
                                Text(
                                    text = "Kreditorlar (Verəcəklər)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₼ ${String.format(Locale.US, "%,.2f", summary.totalPayables)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                                Text(
                                    text = "4 təchizatçı üzrə",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. WEEKLY SALES VS PURCHASES CHART
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Həftəlik Dövriyyə Qrafiki",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LegendItem(color = EmeraldPrimary, label = "Satış")
                                LegendItem(color = WarningOrange, label = "Alış")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual chart bars
                        val maxVal = (summary.weeklyTrend.maxOfOrNull { it.salesAmount } ?: 5000.0).coerceAtLeast(100.0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            summary.weeklyTrend.forEach { day ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Sales Bar
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height((100 * (day.salesAmount / maxVal)).dp.coerceAtLeast(6.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(EmeraldPrimary)
                                        )

                                        // Purchase Bar
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height((100 * (day.purchaseAmount / maxVal)).dp.coerceAtLeast(6.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(WarningOrange)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = day.dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4.1 SMART BUSINESS INSIGHTS
            if (smartInsights.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Sahibkarın Ağıllı Analitik Qeydləri",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                smartInsights.forEach { insight ->
                                    val iconColor = when (insight.type) {
                                        InsightType.POSITIVE -> ProfitGreen
                                        InsightType.WARNING -> WarningOrange
                                        InsightType.CRITICAL -> LossRed
                                        InsightType.TIP -> InfoBlue
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(iconColor)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = insight.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = insight.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        insight.actionText?.let { actionLabel ->
                                            TextButton(
                                                onClick = {
                                                    when (actionLabel) {
                                                        "Debitorlara Bax" -> onNavigateToDebtors()
                                                        "Anbara Keç" -> {} // Can switch tab
                                                        "Hesabatlara Bax" -> onNavigateToReports()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) {
                                                Text(actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4.2 TOP SELLING PRODUCTS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ən Çox Satan Məhsullar (Top 4)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dövriyyə payı",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topProducts.forEach { prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = prod.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = "${prod.category} • ${prod.soldCount.toInt()} ${prod.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "₼ ${String.format(Locale.US, "%,.2f", prod.revenue)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4.3 POS TERMINAL CROSS-APP BANNER
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.emanager.app")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Kassa Terminalı quraşdırılıb. Ana ekrandan 'e-manager POS' seçin.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "e-Manager POS Kassa Terminalı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Kassirlərin satış və qəbz çap terminalına sürətli keçid",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Aç",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 5. QUICK ACTIONS
            item {
                Text(
                    text = "Sürətli Əməliyyatlar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.People,
                        label = "Debitorlar",
                        onClick = onNavigateToDebtors,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.LocalShipping,
                        label = "Təchizatçılar",
                        onClick = onNavigateToSuppliers,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.BarChart,
                        label = "Hesabatlar",
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Forum,
                        label = "Daxili Çat",
                        onClick = onNavigateToChat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun KpiStatCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconBg.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBg,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = amount,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
