package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingSaleDao {
    @Insert
    fun insert(sale: PendingSaleEntity)

    @Query("SELECT * FROM pending_sales WHERE isSynced = 0")
    fun getUnsyncedSales(): List<PendingSaleEntity>

    @Query("UPDATE pending_sales SET isSynced = 1 WHERE id = :id")
    fun markAsSynced(id: Int)
}
