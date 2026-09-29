package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import com.example.util.Formatters
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database.transactionDao(), database.budgetDao(), database.productDao())
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    // User Profile Management (Preferences)
    private val prefs = application.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)

    private val _userProfile = run {
        val savedBiz = prefs.getString("user_business", "Saletreng Store") ?: "Saletreng Store"
        val resolvedBiz = if (savedBiz == "Toko FinansialKu" || savedBiz.isBlank()) "Saletreng Store" else savedBiz
        MutableStateFlow(
            UserProfile(
                name = prefs.getString("user_name", "Pemilik Usaha") ?: "Pemilik Usaha",
                email = prefs.getString("user_email", "tutorialonline44@gmail.com") ?: "tutorialonline44@gmail.com",
                businessName = resolvedBiz,
                phone = prefs.getString("user_phone", "0812-3456-7890") ?: "0812-3456-7890",
                avatarUri = prefs.getString("user_avatar", null)
            )
        )
    }
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateProfile(
        name: String,
        email: String,
        businessName: String,
        phone: String,
        avatarUri: String?
    ) {
        val updated = UserProfile(
            name = name.trim().ifBlank { "Pemilik Usaha" },
            email = email.trim(),
            businessName = businessName.trim().ifBlank { "Saletreng Store" },
            phone = phone.trim(),
            avatarUri = avatarUri
        )
        _userProfile.value = updated
        prefs.edit()
            .putString("user_name", updated.name)
            .putString("user_email", updated.email)
            .putString("user_business", updated.businessName)
            .putString("user_phone", updated.phone)
            .putString("user_avatar", updated.avatarUri)
            .apply()
    }

    // Custom Products Flow
    val customProducts: StateFlow<List<ProductEntity>> = repository.allCustomProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Month & Year for Monthly Report
    private val initialCalendar = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(initialCalendar.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(initialCalendar.get(Calendar.MONTH) + 1) // 1-based (1..12)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    // All transactions flow
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current selected year-month string "YYYY-MM"
    val selectedYearMonth: StateFlow<String> = combine(_selectedYear, _selectedMonth) { year, month ->
        String.format("%04d-%02d", year, month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Current month's budget
    val currentMonthBudget: StateFlow<BudgetEntity?> = selectedYearMonth
        .flatMapLatest { ym -> repository.getBudgetForMonth(ym) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Filtered transactions for the selected month
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedYear,
        _selectedMonth
    ) { list, year, month ->
        val cal = Calendar.getInstance()
        list.filter { tx ->
            cal.timeInMillis = tx.timestamp
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Monthly Report Summary
    val monthlySummary: StateFlow<MonthlySummary> = combine(
        monthlyTransactions,
        currentMonthBudget,
        customProducts,
        _selectedYear,
        _selectedMonth
    ) { transactions, budgetEntity, products, year, month ->
        computeMonthlySummary(transactions, budgetEntity?.budgetAmount ?: 0.0, products, year, month)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        computeMonthlySummary(emptyList(), 0.0, emptyList(), initialCalendar.get(Calendar.YEAR), initialCalendar.get(Calendar.MONTH) + 1)
    )

    // Wallet Balances
    val walletBalances: StateFlow<Map<String, Double>> = allTransactions.map { list ->
        val map = mutableMapOf("CASH" to 0.0, "BANK" to 0.0, "EWALLET" to 0.0)
        list.forEach { tx ->
            val delta = if (tx.type == "INCOME") tx.amount else -tx.amount
            val current = map[tx.walletId] ?: 0.0
            map[tx.walletId] = current + delta
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Total Overall Net Balance
    val totalBalance: StateFlow<Double> = allTransactions.map { list ->
        list.sumOf { tx ->
            if (tx.type == "INCOME") tx.amount else -tx.amount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Transactions Screen Filters
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<TransactionType?>(null) // null for all
    val filterProductId = MutableStateFlow<String?>(null)
    val filterCategoryId: MutableStateFlow<String?> get() = filterProductId

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterProductId
    ) { list, query, type, prodId ->
        list.filter { tx ->
            val matchQuery = query.isBlank() || tx.title.contains(query, ignoreCase = true) || tx.note.contains(query, ignoreCase = true)
            val matchType = type == null || tx.type == type.name
            val matchProduct = prodId == null || tx.categoryId == prodId
            matchQuery && matchType && matchProduct
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value += 1
        } else {
            _selectedMonth.value += 1
        }
    }

    fun prevMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value -= 1
        } else {
            _selectedMonth.value -= 1
        }
    }

    fun setMonthYear(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun saveTransaction(
        id: Long = 0,
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        walletId: String,
        timestamp: Long,
        note: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                title = title.trim(),
                amount = amount,
                type = type.name,
                categoryId = categoryId,
                walletId = walletId,
                timestamp = timestamp,
                note = note.trim(),
                imageUri = imageUri
            )
            if (id == 0L) {
                repository.insertTransaction(entity)
            } else {
                repository.updateTransaction(entity)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun transferCash(
        fromWalletId: String,
        toWalletId: String,
        amount: Double,
        note: String
    ) {
        viewModelScope.launch {
            if (amount <= 0.0 || fromWalletId == toWalletId) return@launch
            val now = System.currentTimeMillis()
            val fromName = com.example.model.WalletType.fromId(fromWalletId).title
            val toName = com.example.model.WalletType.fromId(toWalletId).title
            
            // Outflow from source wallet
            repository.insertTransaction(
                TransactionEntity(
                    title = "Pindah Kas ke $toName",
                    amount = amount,
                    type = "EXPENSE",
                    categoryId = "Pindah Kas",
                    walletId = fromWalletId,
                    timestamp = now,
                    note = note.ifBlank { "Pindah saldo dari $fromName ke $toName" }
                )
            )
            // Inflow to destination wallet
            repository.insertTransaction(
                TransactionEntity(
                    title = "Penerimaan Kas dari $fromName",
                    amount = amount,
                    type = "INCOME",
                    categoryId = "Pindah Kas",
                    walletId = toWalletId,
                    timestamp = now + 1,
                    note = note.ifBlank { "Penerimaan saldo dari $fromName ke $toName" }
                )
            )
        }
    }

    fun adjustCashBalance(
        walletId: String,
        currentCalculated: Double,
        physicalCount: Double,
        note: String
    ) {
        viewModelScope.launch {
            val diff = physicalCount - currentCalculated
            if (kotlin.math.abs(diff) < 0.01) return@launch
            val now = System.currentTimeMillis()
            val walletName = com.example.model.WalletType.fromId(walletId).title
            if (diff > 0) {
                // Surplus fisik lebih besar dari sistem
                repository.insertTransaction(
                    TransactionEntity(
                        title = "Penyesuaian Kas Fisik Lebih (Opname)",
                        amount = diff,
                        type = "INCOME",
                        categoryId = "Kas Opname",
                        walletId = walletId,
                        timestamp = now,
                        note = note.ifBlank { "Penyesuaian saldo fisik $walletName lebih besar dari sistem" }
                    )
                )
            } else {
                // Kurang fisik lebih kecil dari sistem
                repository.insertTransaction(
                    TransactionEntity(
                        title = "Penyesuaian Kas Fisik Kurang (Opname)",
                        amount = kotlin.math.abs(diff),
                        type = "EXPENSE",
                        categoryId = "Kas Opname",
                        walletId = walletId,
                        timestamp = now,
                        note = note.ifBlank { "Penyesuaian saldo fisik $walletName kurang dari sistem" }
                    )
                )
            }
        }
    }

    fun setBudgetForSelectedMonth(amount: Double) {
        viewModelScope.launch {
            repository.setBudget(selectedYearMonth.value, amount)
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        categoryId: String,
        walletId: String,
        note: String
    ) {
        val txType = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        saveTransaction(
            title = title,
            amount = amount,
            type = txType,
            categoryId = categoryId,
            walletId = walletId,
            timestamp = System.currentTimeMillis(),
            note = note
        )
    }

    // Custom Products Management
    fun addCustomProduct(
        name: String,
        type: String = "ALL",
        defaultPrice: Double = 0.0,
        imageUri: String? = null,
        colorHex: Long = 0xFF10B981
    ) {
        viewModelScope.launch {
            repository.insertCustomProduct(
                ProductEntity(
                    name = name.trim(),
                    type = type,
                    defaultPrice = defaultPrice,
                    imageUri = imageUri,
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateCustomProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateCustomProduct(product)
        }
    }

    fun saveCustomProduct(
        id: Long = 0L,
        name: String,
        type: String = "ALL",
        defaultPrice: Double = 0.0,
        imageUri: String? = null,
        colorHex: Long = 0xFF10B981
    ) {
        viewModelScope.launch {
            val entity = ProductEntity(
                id = id,
                name = name.trim(),
                type = type,
                defaultPrice = defaultPrice,
                imageUri = imageUri,
                colorHex = colorHex
            )
            if (id == 0L) {
                repository.insertCustomProduct(entity)
            } else {
                repository.updateCustomProduct(entity)
            }
        }
    }

    fun deleteCustomProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteCustomProduct(product)
        }
    }

    fun deleteAllCustomProducts() {
        viewModelScope.launch {
            repository.deleteAllCustomProducts()
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    private fun computeMonthlySummary(
        transactions: List<TransactionEntity>,
        budget: Double,
        products: List<ProductEntity>,
        year: Int,
        month: Int
    ): MonthlySummary {
        var totalIncome = 0.0
        var totalExpense = 0.0

        val categoryExpenseMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()

        // Prepare days of month
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyMap = (1..maxDays).associateWith { 0.0 }.toMutableMap()

        transactions.forEach { tx ->
            cal.timeInMillis = tx.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)

            if (tx.type == "INCOME") {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
                val currentCatAmt = categoryExpenseMap[tx.categoryId] ?: 0.0
                categoryExpenseMap[tx.categoryId] = currentCatAmt + tx.amount

                val count = categoryCountMap[tx.categoryId] ?: 0
                categoryCountMap[tx.categoryId] = count + 1

                val currentDayAmt = dailyMap[day] ?: 0.0
                dailyMap[day] = currentDayAmt + tx.amount
            }
        }

        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100f).toFloat().coerceIn(-100f, 100f) else 0f
        val budgetRemaining = budget - totalExpense
        val budgetUsedPercentage = if (budget > 0) ((totalExpense / budget) * 100f).toFloat() else 0f

        val productBreakdown = categoryExpenseMap.map { (prodId, amount) ->
            val percentage = if (totalExpense > 0) ((amount / totalExpense) * 100f).toFloat() else 0f
            ProductSpend(
                product = ProductRegistry.getProduct(prodId, products),
                totalAmount = amount,
                percentage = percentage,
                transactionCount = categoryCountMap[prodId] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        val topProduct = productBreakdown.firstOrNull()

        val maxDayAmount = dailyMap.values.maxOrNull() ?: 0.0
        val dailySpendingList = dailyMap.map { (day, amt) ->
            DailySpending(
                dayOfMonth = day,
                amount = amt,
                dateLabel = "$day ${Formatters.getMonthName(month)}",
                isPeakDay = amt > 0 && amt == maxDayAmount
            )
        }.sortedBy { it.dayOfMonth }

        val daysCount = maxDays.coerceAtLeast(1)
        val dailyAverage = totalExpense / daysCount

        return MonthlySummary(
            year = year,
            month = month,
            monthName = Formatters.getMonthName(month),
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRate = savingsRate,
            budget = budget,
            budgetRemaining = budgetRemaining,
            budgetUsedPercentage = budgetUsedPercentage,
            topSpendingProduct = topProduct,
            dailyAverageExpense = dailyAverage,
            productBreakdown = productBreakdown,
            dailySpendingList = dailySpendingList
        )
    }
}
