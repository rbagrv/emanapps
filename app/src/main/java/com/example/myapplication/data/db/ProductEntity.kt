package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.R
import com.example.myapplication.data.Product

@Entity(
    tableName = "products",
    indices = [Index(value = ["barcode"])]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val productNumber: String?,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val price: Double,
    val salePrice: Double,
    val costPrice: Double?,
    val unit: String,
    val stock: Double,
    val stockQuantity: Double,
    val taxRate: Double,
    val isVatApplicable: Boolean,
    val category: String? = "Ümumi",
    val damagedQuantity: Double = 0.0
) {
    fun toProduct(): Product {
        return Product(
            id = id,
            name = name,
            price = if (salePrice > 0.0) salePrice else price,
            costPrice = costPrice ?: 0.0,
            category = category?.ifBlank { "Ümumi" } ?: "Ümumi",
            stockQuantity = if (stockQuantity > 0.0) stockQuantity else stock,
            damagedQuantity = damagedQuantity,
            barcode = barcode ?: sku,
            unit = unit.ifBlank { "əd" },
            iconRes = R.drawable.ic_home
        )
    }
}
