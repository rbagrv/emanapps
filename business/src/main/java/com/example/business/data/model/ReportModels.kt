package com.example.business.data.model

enum class ReportPeriod(val title: String) {
    TODAY("Bu gün"),
    THIS_WEEK("Bu həftə"),
    THIS_MONTH("Bu ay"),
    THIS_YEAR("Bu il")
}

enum class ReportType(val title: String, val shortName: String) {
    TURNOVER("Dövriyyə", "Dövriyyə"),
    MZH("Mənfəət və Zərər (MZH)", "MZH"),
    BALANCE("Balans Hesabatı", "Balans"),
    CASH_FLOW("Cash Flow (Pul Axını)", "Cash Flow"),
    TAX("Vergi Borcları & Öhdəliklər", "Vergilər")
}

data class CashierPerformance(
    val cashierName: String,
    val checksCount: Int,
    val totalSales: Double,
    val cashAmount: Double,
    val cardAmount: Double
)

data class TopProductSale(
    val productName: String,
    val quantitySold: Double,
    val unit: String,
    val totalRevenue: Double
)

data class PeriodReport(
    val period: ReportPeriod,
    val totalSales: Double,
    val totalPurchases: Double,
    val grossProfit: Double,
    val marginPercent: Double,
    val checkCount: Int,
    val averageCheck: Double,
    val cashSales: Double,
    val cardSales: Double,
    val debtSales: Double,
    val topProducts: List<TopProductSale>,
    val cashiers: List<CashierPerformance>
)

data class ProfitAndLossReport(
    val revenue: Double,
    val otherIncome: Double,
    val cogs: Double, // Maya dəyəri
    val grossProfit: Double, // Ümumi Mənfəət
    val grossMarginPercent: Double,
    val salaries: Double, // Əməkhaqqı
    val rent: Double, // İcarə
    val utilities: Double, // Kommunal və xidmət
    val logistics: Double, // Logistika və daşınma
    val otherExpenses: Double, // Digər əməliyyat xərcləri
    val totalOperatingExpenses: Double,
    val operatingProfit: Double, // EBITDA
    val taxExpense: Double, // Vergi xərcləri
    val netProfit: Double, // Xalis Mənfəət
    val netMarginPercent: Double
)

data class BalanceSheetReport(
    // Aktivlər
    val cashAndBank: Double,
    val receivables: Double, // Debitor borcları (Alacaqlar)
    val inventory: Double, // Anbar qalığı
    val totalCurrentAssets: Double,
    val fixedAssets: Double, // Əsas vəsaitlər / avadanlıqlar
    val totalAssets: Double,

    // Öhdəliklər
    val payables: Double, // Təchizatçılara borclar
    val taxDebt: Double, // Vergi borcları
    val shortTermLoans: Double, // Digər qısamüddətli borclar
    val totalCurrentLiabilities: Double,

    // Kapital
    val charterCapital: Double, // Nizamnamə kapitalı
    val retainedEarnings: Double, // Bölüşdürülməmiş mənfəət
    val totalEquity: Double,
    val totalLiabilitiesAndEquity: Double
)

data class CashFlowReport(
    val inflowSales: Double,
    val outflowSuppliers: Double,
    val outflowSalaries: Double,
    val outflowExpenses: Double,
    val outflowTaxes: Double,
    val netOperatingCashFlow: Double,
    val outflowInvestments: Double,
    val netCashFlow: Double,
    val openingCash: Double,
    val closingCash: Double
)

data class TaxLiabilityReport(
    val vatDue: Double, // ƏDV
    val simplifiedTax: Double, // Sadələşdirilmiş vergi
    val incomeTax: Double, // Mənfəət / Gəlir vergisi
    val dsmf: Double, // DSMF ayırmaları
    val its: Double, // İcbari Tibbi Sığorta
    val totalCalculated: Double,
    val totalPaid: Double,
    val taxDebt: Double, // Cari qalıq vergi borcu
    val deadline: String,
    val status: String
)
