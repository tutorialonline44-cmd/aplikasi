package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "ALL", // "INCOME", "EXPENSE", or "ALL"
    val defaultPrice: Double = 0.0,
    val imageUri: String? = null,
    val colorHex: Long = 0xFF10B981
)
