package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    fun searchByBarcode(barcode: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR productNumber LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchByNameOrCode(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("UPDATE products SET stockQuantity = :newStock, stock = :newStock WHERE id = :productId")
    fun updateStock(productId: String, newStock: Double)
}
