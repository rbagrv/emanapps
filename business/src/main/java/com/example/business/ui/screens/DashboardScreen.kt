package com.example.business.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val companyName by viewModel.companyName.collectAsState()

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
                    IconButton(onClick = { viewModel.openTenantSelector() }) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Obyekt seçimi",
                            tint = EmeraldPrimary
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
                        subtitle = "Xalis rentabellik: ~38%",
                        icon = Icons.Default.MonetizationOn,
                        iconBg = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )

                    KpiStatCard(
                        title = "Kassa & Bank",
                        amount = "₼ ${String.format(Locale.US, "%,.0f", summary.cashInHand + summary.bankBalance)}",
                        subtitle = "Nəğd: ₼ ${String.format(Locale.US, "%,.0f", summary.cashInHand)}",
                        icon = Icons.Default.AccountBalance,
                        iconBg = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
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
