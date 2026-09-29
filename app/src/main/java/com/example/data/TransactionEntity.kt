package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: String, // Stores Product Name or Product ID
    val walletId: String,
    val timestamp: Long,
    val note: String = "",
    val imageUri: String? = null
)
