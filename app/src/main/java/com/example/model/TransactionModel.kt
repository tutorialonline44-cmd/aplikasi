package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.ProductEntity
import com.example.ui.theme.*

enum class TransactionType(val label: String) {
    EXPENSE("Pengeluaran"),
    INCOME("Pemasukan")
}

enum class WalletType(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    CASH("CASH", "Tunai / Kas", Icons.Default.LocalAtm, Color(0xFF10B981)),
    BANK("BANK", "Rekening Bank", Icons.Default.AccountBalance, Color(0xFF3B82F6)),
    EWALLET("EWALLET", "E-Wallet / QRIS", Icons.Default.QrCodeScanner, Color(0xFF8B5CF6));

    companion object {
        fun fromId(id: String): WalletType = entries.find { it.id == id } ?: CASH
    }
}

data class ProductItem(
    val id: String,
    val title: String,
    val type: TransactionType,
    val icon: ImageVector = Icons.Default.Inventory,
    val color: Color = Emerald600,
    val imageUri: String? = null
)

typealias CategoryItem = ProductItem

object ProductRegistry {
    private val productColors = listOf(
        Emerald600,
        CatFood,
        CatShopping,
        CatTransport,
        CatBills,
        CatEntertainment,
        CatHealth,
        CatEducation,
        CatInvestment
    )

    fun getColorForIndex(index: Int): Color {
        return productColors[index % productColors.size]
    }

    fun getColorHexForIndex(index: Int): Long {
        val c = getColorForIndex(index)
        return (c.value shr 32).toLong()
    }

    fun fromEntity(entity: ProductEntity): ProductItem {
        val t = if (entity.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        return ProductItem(
            id = entity.name,
            title = entity.name,
            type = t,
            icon = Icons.Default.Inventory,
            color = Color(entity.colorHex),
            imageUri = entity.imageUri
        )
    }

    fun getProduct(idOrName: String, customProducts: List<ProductEntity> = emptyList()): ProductItem {
        val found = customProducts.find { it.name.equals(idOrName, ignoreCase = true) || it.id.toString() == idOrName }
        if (found != null) {
            return fromEntity(found)
        }
        val hash = kotlin.math.abs(idOrName.hashCode())
        val assignedColor = productColors[hash % productColors.size]
        return ProductItem(
            id = idOrName,
            title = if (idOrName.isNotBlank()) idOrName else "Produk Umum",
            type = TransactionType.EXPENSE,
            icon = Icons.Default.Inventory,
            color = assignedColor,
            imageUri = null
        )
    }

    fun getCategory(id: String): ProductItem = getProduct(id)
}

typealias CategoryRegistry = ProductRegistry

data class ProductSpend(
    val product: ProductItem,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
) {
    val category: ProductItem get() = product
}

typealias CategorySpend = ProductSpend

data class DailySpending(
    val dayOfMonth: Int,
    val amount: Double,
    val dateLabel: String,
    val isPeakDay: Boolean = false
)

data class MonthlySummary(
    val year: Int,
    val month: Int, // 1 to 12
    val monthName: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Float, // 0..100
    val budget: Double,
    val budgetRemaining: Double,
    val budgetUsedPercentage: Float,
    val topSpendingProduct: ProductSpend?,
    val dailyAverageExpense: Double,
    val productBreakdown: List<ProductSpend>,
    val dailySpendingList: List<DailySpending>
) {
    val topSpendingCategory: ProductSpend? get() = topSpendingProduct
    val categoryBreakdown: List<ProductSpend> get() = productBreakdown
}
