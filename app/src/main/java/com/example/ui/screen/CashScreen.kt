package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import com.example.util.CashExportHelper
import com.example.util.Formatters
import com.example.util.ReportExportHelper
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"

    // Dialog States
    var showAddCashDialog by remember { mutableStateOf(false) }
    var dialogCashType by remember { mutableStateOf("INCOME") } // "INCOME" or "EXPENSE"
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var generatedExportFile by remember { mutableStateOf<Pair<File, String>?>(null) }

    // Aggregate summary numbers
    val totalIn = remember(allTransactions) {
        allTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalOut = remember(allTransactions) {
        allTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    // Filtered transaction list
    val filteredTransactions = remember(allTransactions, selectedTypeFilter) {
        allTransactions.filter { tx ->
            (selectedTypeFilter == "ALL") || (tx.type == selectedTypeFilter)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. Hero Saldo Kas Toko
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_hero_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(SageTeal800, SageTeal500, SageTeal700)
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_saletreng_logo),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Buku Uang Kas Toko",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = userProfile.businessName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Total Saldo Kas Toko",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = Formatters.formatRupiah(totalBalance),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mini stats: Total Kas Masuk vs Kas Keluar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "📥 Total Kas Masuk",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = Formatters.formatRupiah(totalIn),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "📤 Total Kas Keluar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = Formatters.formatRupiah(totalOut),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Tombol Aksi Cepat Kas (Kas Masuk & Kas Keluar)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kas Masuk
                    Button(
                        onClick = {
                            dialogCashType = "INCOME"
                            showAddCashDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_cash_in"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kas Masuk (+)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    // Kas Keluar
                    Button(
                        onClick = {
                            dialogCashType = "EXPENSE"
                            showAddCashDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_cash_out"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kas Keluar (-)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 3. Ekspor Pembukuan Uang Kas (PDF & Excel)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_cash_book_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ekspor Buku Uang Kas",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Unduh pembukuan mutasi kas toko (${filteredTransactions.size} transaksi)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isExporting) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = "Menyiapkan dokumen pembukuan kas...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Ekspor PDF Button
                            Button(
                                onClick = {
                                    if (!isExporting) {
                                        isExporting = true
                                        scope.launch(Dispatchers.IO) {
                                            val file = CashExportHelper.exportCashBookToPdf(
                                                context = context,
                                                transactions = filteredTransactions,
                                                totalBalance = totalBalance,
                                                userProfile = userProfile,
                                                filterType = selectedTypeFilter
                                            )
                                            withContext(Dispatchers.Main) {
                                                isExporting = false
                                                generatedExportFile = Pair(file, "application/pdf")
                                            }
                                        }
                                    }
                                },
                                enabled = !isExporting && filteredTransactions.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_cash_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626) // PDF Red
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor PDF", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            // Ekspor Excel Button
                            Button(
                                onClick = {
                                    if (!isExporting) {
                                        isExporting = true
                                        scope.launch(Dispatchers.IO) {
                                            val file = CashExportHelper.exportCashBookToExcel(
                                                context = context,
                                                transactions = filteredTransactions,
                                                totalBalance = totalBalance,
                                                userProfile = userProfile,
                                                filterType = selectedTypeFilter
                                            )
                                            withContext(Dispatchers.Main) {
                                                isExporting = false
                                                generatedExportFile = Pair(file, "text/csv")
                                            }
                                        }
                                    }
                                },
                                enabled = !isExporting && filteredTransactions.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_cash_excel_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669) // Excel Green
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor Excel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // 4. Header Mutasi Buku Kas & Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mutasi Buku Kas (${filteredTransactions.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedTypeFilter != "ALL") {
                            TextButton(onClick = { selectedTypeFilter = "ALL" }) {
                                Text("Reset Filter", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Tipe Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedTypeFilter == "ALL",
                            onClick = { selectedTypeFilter = "ALL" },
                            label = { Text("Semua") }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "INCOME",
                            onClick = { selectedTypeFilter = "INCOME" },
                            label = { Text("Kas Masuk (+)") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "EXPENSE",
                            onClick = { selectedTypeFilter = "EXPENSE" },
                            label = { Text("Kas Keluar (-)") },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // 5. Daftar Mutasi Buku Kas
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Mutasi Kas",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Catat pemasukan atau pengeluaran kas pertama Anda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    CashTransactionCard(
                        transaction = tx,
                        onDeleteClick = { transactionToDelete = tx }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                dialogCashType = "INCOME"
                showAddCashDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Catat Kas") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_cash")
        )
    }

    // DIALOG: Tambah Kas Masuk / Kas Keluar
    if (showAddCashDialog) {
        AddCashTransactionDialog(
            initialType = dialogCashType,
            onDismiss = { showAddCashDialog = false },
            onConfirm = { title, amount, type, category, note ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = category,
                    walletId = "CASH",
                    note = note
                )
                showAddCashDialog = false
            }
        )
    }

    // DIALOG: Konfirmasi Hapus Transaksi Kas
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus Mutasi Kas?") },
            text = { Text("Transaksi '${tx.title}' senilai ${Formatters.formatRupiah(tx.amount)} akan dihapus dan saldo kas diperbarui kembali.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG: Aksi Berkas Hasil Ekspor Kas (PDF / Excel)
    if (generatedExportFile != null) {
        val (file, mimeType) = generatedExportFile!!
        AlertDialog(
            onDismissRequest = { generatedExportFile = null },
            icon = {
                Icon(
                    imageVector = if (mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                    contentDescription = null,
                    tint = if (mimeType == "application/pdf") Color(0xFFDC2626) else Color(0xFF059669),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (mimeType == "application/pdf") "Buku Kas PDF Siap!" else "Buku Kas Excel Siap!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Berkas mutasi kas toko (${filteredTransactions.size} transaksi) berhasil digenerate. Silakan pilih untuk membuka langsung atau membagikannya.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (mimeType == "application/pdf") "Bagikan Buku Kas PDF" else "Bagikan Buku Kas Excel"
                        ReportExportHelper.shareFile(context, file, mimeType, title)
                        generatedExportFile = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mimeType == "application/pdf") Color(0xFFDC2626) else Color(0xFF059669)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim / Bagikan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val title = if (mimeType == "application/pdf") "Buka Buku Kas PDF" else "Buka Buku Kas Excel"
                        ReportExportHelper.openFile(context, file, mimeType, title)
                        generatedExportFile = null
                    }
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka File")
                }
            }
        )
    }
}

@Composable
private fun CashTransactionCard(
    transaction: TransactionEntity,
    onDeleteClick: () -> Unit
) {
    val isIncome = transaction.type == "INCOME"
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID")) }
    val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isIncome) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + Formatters.formatRupiah(transaction.amount),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG: Catat Kas Masuk / Keluar
// -------------------------------------------------------------

@Composable
private fun AddCashTransactionDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, type: String, category: String, note: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) } // "INCOME" or "EXPENSE"
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val incomeQuickCategories = listOf("Modal Kasir", "Penjualan Tunai", "Setoran Pemilik", "Penerimaan Piutang", "Pendapatan Lain")
    val expenseQuickCategories = listOf("Operasional Toko", "Beli Stok Tunai", "Gaji / Uang Makan", "Prive Pemilik", "Listrik & Air", "Transportasi")

    val quickCategories = if (type == "INCOME") incomeQuickCategories else expenseQuickCategories

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (type == "INCOME") "Catat Kas Masuk 💵" else "Catat Kas Keluar 💸",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                // Toggle Tipe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Kas Masuk (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Kas Keluar (-)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Nominal
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Kas (Rp)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Judul / Keterangan
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Keterangan Kas") },
                    placeholder = { Text("Contoh: Modal Awal Kasir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Tag Chips
                Text("Pilihan Cepat Kategori:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.take(3).forEach { cat ->
                        SuggestionChip(
                            onClick = { title = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Catatan Tambahan
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    placeholder = { Text("Keterangan nota atau transaksi...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                val t = title.ifBlank { if (type == "INCOME") "Kas Masuk" else "Kas Keluar" }
                                onConfirm(t, amt, type, t, note)
                            }
                        },
                        enabled = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "INCOME") IncomeGreen else ExpenseRed
                        )
                    ) {
                        Text("Simpan Kas")
                    }
                }
            }
        }
    }
}
