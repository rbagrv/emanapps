package com.example.business.data.model

data class WarehouseItem(
    val id: String,
    val name: String,
    val sku: String = "",
    val barcode: String? = null,
    val unit: String = "ədəd",
    val stock: Double = 0.0,
    val costPrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "Ümumi",
    val warehouseName: String = "Əsas Anbar"
) {
    val totalStockCost: Double
        get() = stock * costPrice

    val totalStockValue: Double
        get() = stock * salePrice

    val isLowStock: Boolean
        get() = stock <= 5.0
}

data class WarehouseSummary(
    val totalProductsCount: Int = 0,
    val totalStockQuantity: Double = 0.0,
    val totalStockCostValue: Double = 0.0,
    val totalStockSaleValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0
)
