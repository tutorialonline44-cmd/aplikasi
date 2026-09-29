package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth LIMIT 1")
    fun getBudgetForMonth(yearMonth: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM monthly_budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetEntity)
}
