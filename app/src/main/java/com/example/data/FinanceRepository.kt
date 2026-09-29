package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val productDao: ProductDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCustomProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getTransactionsForRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startTime, endTime)
    }

    fun getBudgetForMonth(yearMonth: String): Flow<BudgetEntity?> {
        return budgetDao.getBudgetForMonth(yearMonth)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun setBudget(yearMonth: String, amount: Double) {
        budgetDao.setBudget(BudgetEntity(yearMonth = yearMonth, budgetAmount = amount))
    }

    suspend fun insertCustomProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateCustomProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteCustomProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteAllCustomProducts() {
        productDao.deleteAllProducts()
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun checkAndSeedInitialData() {
        // Clean up any previously seeded dummy products if present
        val dummyProductNames = listOf(
            "Kopi Susu Aren",
            "Kaos Polos Cotton",
            "Camilan Keripik Renyah",
            "Bahan Baku Biji Kopi & Susu",
            "Kemasan Cup & Paper Bag",
            "Pulsa & Listrik Usaha"
        )
        val dummyTxTitles = listOf(
            "Penjualan 40 Cup Kopi Susu Aren",
            "Penjualan 25 Pcs Kaos Polos",
            "Restock Bahan Baku Biji Kopi",
            "Beli Kemasan Cup & Plastik",
            "Token Listrik & WiFi Outlet",
            "Penjualan 30 Pack Camilan",
            "Penjualan 50 Cup Kopi Susu",
            "Restock Tambahan Bahan Baku"
        )
        productDao.deleteProductsByNames(dummyProductNames)
        transactionDao.deleteTransactionsByTitles(dummyTxTitles)
    }
}
