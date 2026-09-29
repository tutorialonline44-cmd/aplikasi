package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey
    val yearMonth: String, // format "YYYY-MM", e.g. "2026-09"
    val budgetAmount: Double
)
