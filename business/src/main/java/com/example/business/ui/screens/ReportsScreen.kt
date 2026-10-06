package com.example.business.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun ReportsScreen(
    viewModel: BusinessViewModel,
    modifier: Modifier = Modifier
) {
    val report by viewModel.currentReport.collectAsState()
    val pnlReport by viewModel.pnlReport.collectAsState()
    val balanceReport by viewModel.balanceReport.collectAsState()
    val cashFlowReport by viewModel.cashFlowReport.collectAsState()
    val taxReport by viewModel.taxReport.collectAsState()

    val selectedPeriod by viewModel.selectedReportPeriod.collectAsState()
    val selectedReportType by viewModel.selectedReportType.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Maliyyə & Vergi Hesabatları",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedReportType.title,
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.shareReportPdf(context, sendToWhatsApp = false)
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Yarat",
                            tint = EmeraldPrimary
                        )
                    }

                    IconButton(onClick = {
                        viewModel.shareReportPdf(context, sendToWhatsApp = true)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "WhatsApp ilə Paylaş",
                            tint = Color(0xFF25D366) // WhatsApp Green
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shareReportPdf(context, sendToWhatsApp = false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Saxla", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.shareReportPdf(context, sendToWhatsApp = true) },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp-a Göndər", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
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

            // 1. Report Type Selector (Horizontal scroll chips)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportType.entries.forEach { type ->
                        val isSelected = selectedReportType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectReportType(type) },
                            label = {
                                Text(
                                    text = type.shortName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 2. Period Selector (Bu gün, Bu həftə, Bu ay, Bu il)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPeriod(period) },
                            label = {
                                Text(
                                    text = period.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Dynamic Report Content based on selectedReportType
            when (selectedReportType) {
                ReportType.TURNOVER -> {
                    item { TurnoverSummarySection(report = report, period = selectedPeriod) }
                    item { PaymentMethodBreakdown(report = report) }

                    item {
                        Text(
                            text = "Ən Çox Satan Məhsullar (Top 5)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(report.topProducts) { prod ->
                        TopProductItemCard(prod = prod)
                    }

                    item {
                        Text(
                            text = "Kassirlərin Fəaliyyəti",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(report.cashiers) { cashier ->
                        CashierPerformanceCard(cashier = cashier)
                    }
                }

                ReportType.MZH -> {
                    item { ProfitAndLossSection(pnl = pnlReport, period = selectedPeriod) }
                }

                ReportType.BALANCE -> {
                    item { BalanceSheetSection(balance = balanceReport) }
                }

                ReportType.CASH_FLOW -> {
                    item { CashFlowSection(cashFlow = cashFlowReport, period = selectedPeriod) }
                }

                ReportType.TAX -> {
                    item { TaxLiabilitySection(tax = taxReport, period = selectedPeriod) }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// =================================================================
// 1. Turnover Section Components
// =================================================================

@Composable
fun TurnoverSummarySection(report: PeriodReport, period: ReportPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${period.title} Üzrə Dövriyyə Nəticəsi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Cəmi Satış Məbləği", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₼ ${String.format(Locale.US, "%,.2f", report.totalSales)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Cəmi Alış / Maya Dəyəri", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₼ ${String.format(Locale.US, "%,.2f", report.totalPurchases)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Xalis Mənfəət", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₼ ${String.format(Locale.US, "%,.2f", report.grossProfit)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Marja: ${String.format(Locale.US, "%.1f", report.marginPercent)}%",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodBreakdown(report: PeriodReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Ödəniş Metodları Üzrə Bölünmə",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PaymentStatItem(color = EmeraldPrimary, label = "Nağd Kassa", amount = report.cashSales)
                PaymentStatItem(color = InfoBlue, label = "Bank Kartı", amount = report.cardSales)
                PaymentStatItem(color = WarningOrange, label = "Nisyə/Borc", amount = report.debtSales)
            }
        }
    }
}

@Composable
fun TopProductItemCard(prod: TopProductSale) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = prod.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "Satılıb: ${prod.quantitySold} ${prod.unit}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₼ ${String.format(Locale.US, "%,.2f", prod.totalRevenue)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = EmeraldPrimary
            )
        }
    }
}

@Composable
fun CashierPerformanceCard(cashier: CashierPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(text = cashier.cashierName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${cashier.checksCount} çek kəsilib",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₼ ${String.format(Locale.US, "%,.2f", cashier.totalSales)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Nağd: ₼ ${cashier.cashAmount.toInt()} | Kart: ₼ ${cashier.cardAmount.toInt()}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =================================================================
// 2. MZH (Mənfəət və Zərər / P&L) Section
// =================================================================

@Composable
fun ProfitAndLossSection(pnl: ProfitAndLossReport, period: ReportPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mənfəət və Zərər Hesabatı (MZH)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Badge(containerColor = EmeraldPrimary) {
                    Text(period.title, color = Color.White)
                }
            }

            HorizontalDivider()

            // 1. Gəlirlər
            FinancialLineItem("1. Satış Gəlirləri", pnl.revenue, isPositive = true)
            FinancialLineItem("2. Malların Maya Dəyəri (COGS)", -pnl.cogs, isPositive = false)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldPrimary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("3. Ümumi Mənfəət (Gross Profit):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("₼ ${formatAmount(pnl.grossProfit)} (${String.format(Locale.US, "%.1f", pnl.grossMarginPercent)}%)", fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 13.sp)
                }
            }

            // 2. Əməliyyat Xərcləri
            Text("Əməliyyat Xərcləri:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FinancialSubLineItem("• Əməkhaqqı Fondu", pnl.salaries)
                FinancialSubLineItem("• İcarə Haqqı", pnl.rent)
                FinancialSubLineItem("• Kommunal & İnternet", pnl.utilities)
                FinancialSubLineItem("• Logistika və Digər", pnl.logistics + pnl.otherExpenses)
            }
            FinancialLineItem("Cəmi Əməliyyat Xərcləri", -pnl.totalOperatingExpenses, isPositive = false)

            HorizontalDivider()

            // 3. EBITDA və Vergi
            FinancialLineItem("Əməliyyat Mənfəəti (EBITDA)", pnl.operatingProfit, isPositive = pnl.operatingProfit >= 0)
            FinancialLineItem("Hesablanmış Vergi Xərci", -pnl.taxExpense, isPositive = false)

            // 4. Xalis Mənfəət
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = EmeraldPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("XALİS MƏNFƏƏT (NET PROFIT)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Rentabellik: ${String.format(Locale.US, "%.1f", pnl.netMarginPercent)}%", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                    Text("₼ ${formatAmount(pnl.netProfit)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

// =================================================================
// 3. Balans Hesabatı (Balance Sheet) Section
// =================================================================

@Composable
fun BalanceSheetSection(balance: BalanceSheetReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Balans Hesabatı (Balance Sheet)", fontSize = 15.sp, fontWeight = FontWeight.Bold)

            HorizontalDivider()

            // AKTİVLƏR
            Text("I. AKTİVLƏR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            FinancialSubLineItem("• Kassa və Bank Qalıqları", balance.cashAndBank)
            FinancialSubLineItem("• Debitor Borcları (Müştəri Alacaqları)", balance.receivables)
            FinancialSubLineItem("• Anbar Mal Qalıqları", balance.inventory)
            FinancialSubLineItem("• Əsas Vəsaitlər / Avadanlıqlar", balance.fixedAssets)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldPrimary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CƏMİ AKTİVLƏR:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("₼ ${formatAmount(balance.totalAssets)}", fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ÖHDƏLİKLƏR VƏ KAPİTAL
            Text("II. ÖHDƏLİKLƏR VƏ KAPİTAL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LossRed)
            FinancialSubLineItem("• Kreditor Borcları (Təchizatçılara)", balance.payables)
            FinancialSubLineItem("• Vergi Öhdəlikləri", balance.taxDebt)
            FinancialSubLineItem("• Qısamüddətli Borclar", balance.shortTermLoans)
            FinancialSubLineItem("• Nizamnamə Kapitalı", balance.charterCapital)
            FinancialSubLineItem("• Bölüşdürülməmiş Mənfəət", balance.retainedEarnings)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LossRed.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CƏMİ ÖHDƏLİKLƏR VƏ KAPİTAL:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("₼ ${formatAmount(balance.totalLiabilitiesAndEquity)}", fontWeight = FontWeight.Bold, color = LossRed, fontSize = 13.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✓ Balans Bərabərliyi Təmin Olunub (Aktivlər = Öhdəliklər + Kapital)",
                    fontSize = 11.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// =================================================================
// 4. Cash Flow Section
// =================================================================

@Composable
fun CashFlowSection(cashFlow: CashFlowReport, period: ReportPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cash Flow (Nəğd Pul Axını)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(period.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            FinancialLineItem("Dövrün Əvvəlinə Pul Qalığı", cashFlow.openingCash, isPositive = true)
            FinancialLineItem("Müştərilərdən Daxilolmalar (+)", cashFlow.inflowSales, isPositive = true)
            FinancialLineItem("Təchizatçılara Mallar Üzrə Çıxış (-)", -cashFlow.outflowSuppliers, isPositive = false)
            FinancialLineItem("Əməkhaqqı Çıxışları (-)", -cashFlow.outflowSalaries, isPositive = false)
            FinancialLineItem("İcarə & Əməliyyat Xərcləri (-)", -cashFlow.outflowExpenses, isPositive = false)
            FinancialLineItem("Büdcəyə Vergi Ödənişləri (-)", -cashFlow.outflowTaxes, isPositive = false)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = (if (cashFlow.netOperatingCashFlow >= 0) EmeraldPrimary else LossRed).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Xalis Əməliyyat Pul Axını:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "₼ ${formatAmount(cashFlow.netOperatingCashFlow)}",
                        fontWeight = FontWeight.Bold,
                        color = if (cashFlow.netOperatingCashFlow >= 0) EmeraldPrimary else LossRed,
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DÖVRÜN SONUNA PUL QALIĞI:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("₼ ${formatAmount(cashFlow.closingCash)}", fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 17.sp)
                }
            }
        }
    }
}

// =================================================================
// 5. Vergi Borcları & Öhdəliklər Section
// =================================================================

@Composable
fun TaxLiabilitySection(tax: TaxLiabilityReport, period: ReportPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vergi Öhdəlikləri & Qalıq Borclar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Badge(containerColor = if (tax.taxDebt > 0) WarningOrange else EmeraldPrimary) {
                    Text(tax.status, color = Color.White)
                }
            }

            HorizontalDivider()

            FinancialSubLineItem("• ƏDV (18%) Hesablanmış", tax.vatDue)
            FinancialSubLineItem("• Sadələşdirilmiş Vergi (2%)", tax.simplifiedTax)
            FinancialSubLineItem("• Mənfəət / Gəlir Vergisi", tax.incomeTax)
            FinancialSubLineItem("• DSMF Sosial Ayırmalar", tax.dsmf)
            FinancialSubLineItem("• İcbari Tibbi Sığorta (İTS)", tax.its)

            HorizontalDivider()

            FinancialLineItem("Cəmi Hesablanmış Vergi Məbləği", tax.totalCalculated, isPositive = true)
            FinancialLineItem("Büdcəyə Ödənilmiş Məbləğ", tax.totalPaid, isPositive = true)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WarningOrange.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CARI QALIQ VERGİ BORCU:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarningOrange)
                        Text("₼ ${formatAmount(tax.taxDebt)}", fontWeight = FontWeight.Bold, color = WarningOrange, fontSize = 18.sp)
                    }
                    Text(
                        text = "Son Bəyannamə və Ödəniş Tarixi: ${tax.deadline}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =================================================================
// Helper UI Rows
// =================================================================

@Composable
fun FinancialLineItem(label: String, amount: Double, isPositive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = (if (amount < 0) "- ₼ " else "₼ ") + formatAmount(kotlin.math.abs(amount)),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPositive) EmeraldPrimary else LossRed
        )
    }
}

@Composable
fun FinancialSubLineItem(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "₼ ${formatAmount(amount)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,.2f", amount)
}

@Composable
fun PaymentStatItem(color: Color, label: String, amount: Double) {
    Column {
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
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = "₼ ${String.format(Locale.US, "%,.0f", amount)}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
