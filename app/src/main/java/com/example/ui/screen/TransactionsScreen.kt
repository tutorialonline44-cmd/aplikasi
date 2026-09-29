package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionEntity
import com.example.model.ProductRegistry
import com.example.model.TransactionType
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.ExpenseRed
import com.example.util.Formatters
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val customProducts by viewModel.customProducts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterProductId by viewModel.filterProductId.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Cari produk atau keterangan...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag("search_transactions_input"),
            shape = RoundedCornerShape(16.dp)
        )

        // Type Filter Chips: Semua, Pengeluaran, Pemasukan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == null,
                onClick = { viewModel.filterType.value = null },
                label = { Text("Semua") }
            )
            FilterChip(
                selected = filterType == TransactionType.EXPENSE,
                onClick = {
                    viewModel.filterType.value = if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                },
                label = { Text("Pengeluaran") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                    selectedLabelColor = ExpenseRed
                )
            )
            FilterChip(
                selected = filterType == TransactionType.INCOME,
                onClick = {
                    viewModel.filterType.value = if (filterType == TransactionType.INCOME) null else TransactionType.INCOME
                },
                label = { Text("Pemasukan") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                    selectedLabelColor = IncomeGreen
                )
            )
        }

        // Product Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterProductId == null,
                onClick = { viewModel.filterProductId.value = null },
                label = { Text("Semua Produk", fontSize = 12.sp) }
            )
            val filteredProductList = when (filterType) {
                TransactionType.EXPENSE -> customProducts.filter { it.type == "EXPENSE" || it.type == "ALL" }
                TransactionType.INCOME -> customProducts.filter { it.type == "INCOME" || it.type == "ALL" }
                else -> customProducts
            }
            filteredProductList.forEach { prod ->
                val pColor = Color(prod.colorHex)
                FilterChip(
                    selected = filterProductId == prod.name,
                    onClick = {
                        viewModel.filterProductId.value = if (filterProductId == prod.name) null else prod.name
                    },
                    label = { Text(prod.name, fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pColor)
                        )
                    }
                )
            }
        }

        // Transactions List Grouped by Date
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tidak ada transaksi yang cocok",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coba ubah kata kunci atau reset filter pencarian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val grouped = remember(transactions) {
                transactions.groupBy { tx ->
                    getDateGroupHeader(tx.timestamp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
            ) {
                grouped.forEach { (dateHeader, list) ->
                    item {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(list, key = { it.id }) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            customProducts = customProducts,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }
}

private fun getDateGroupHeader(timestamp: Long): String {
    val calNow = Calendar.getInstance()
    val calTx = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isSameYear = calNow.get(Calendar.YEAR) == calTx.get(Calendar.YEAR)
    val isToday = isSameYear && calNow.get(Calendar.DAY_OF_YEAR) == calTx.get(Calendar.DAY_OF_YEAR)
    val isYesterday = isSameYear && calNow.get(Calendar.DAY_OF_YEAR) - calTx.get(Calendar.DAY_OF_YEAR) == 1

    return when {
        isToday -> "Hari Ini (${Formatters.formatDayDate(timestamp)})"
        isYesterday -> "Kemarin (${Formatters.formatDayDate(timestamp)})"
        else -> Formatters.formatDayDate(timestamp)
    }
}
