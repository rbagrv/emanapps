package com.example.business.data.model

enum class ReportPeriod(val title: String) {
    TODAY("Bu gün"),
    THIS_WEEK("Bu həftə"),
    THIS_MONTH("Bu ay"),
    THIS_YEAR("Bu il")
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
