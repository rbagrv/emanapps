package com.example.business.data.model

data class BusinessSummary(
    val companyName: String = "Global Ticarət MMC",
    val voen: String = "1802938471",
    val activeCurrency: String = "AZN",
    val todaySales: Double = 3450.80,
    val todayPurchases: Double = 1200.00,
    val todayProfit: Double = 890.50,
    val monthSales: Double = 84920.00,
    val monthPurchases: Double = 52300.00,
    val monthProfit: Double = 32620.00,
    val cashInHand: Double = 12450.00,
    val bankBalance: Double = 45890.00,
    val totalReceivables: Double = 18650.00, // Debitorlar (Müştəri borcları)
    val overdueReceivables: Double = 4320.00, // Gecikmiş borclar
    val totalPayables: Double = 14200.00, // Təchizatçılara borc (Kreditorlar)
    val warehouseStockValue: Double = 67800.00,
    val weeklyTrend: List<DayStat> = listOf(
        DayStat("B.e", 2800.0, 1100.0),
        DayStat("Ç.a", 3100.0, 950.0),
        DayStat("Çər", 3450.0, 1400.0),
        DayStat("C.a", 2900.0, 800.0),
        DayStat("Cüm", 4200.0, 2100.0),
        DayStat("Şən", 5600.0, 1600.0),
        DayStat("Baz", 4800.0, 1200.0)
    ),
    val categoryShares: List<CategoryShare> = listOf(
        CategoryShare("Qida və İçki", 42, 35666.0),
        CategoryShare("Məişət Kimyası", 26, 22079.0),
        CategoryShare("Elektronika", 18, 15285.0),
        CategoryShare("Digər", 14, 11890.0)
    )
)

data class DayStat(
    val dayName: String,
    val salesAmount: Double,
    val purchaseAmount: Double
)

data class CategoryShare(
    val category: String,
    val percentage: Int,
    val amount: Double
)
