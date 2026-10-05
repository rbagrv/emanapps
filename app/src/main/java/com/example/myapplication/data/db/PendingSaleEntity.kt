package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sales")
data class PendingSaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val saleRequestJson: String,
    val isSynced: Boolean = false
)
