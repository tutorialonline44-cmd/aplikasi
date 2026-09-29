package com.example.ui.screen

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ProductEntity
import com.example.data.TransactionEntity
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.Formatters
import com.example.util.ImageStorageHelper
import java.util.Calendar

@Composable
fun AddEditTransactionDialog(
    initialTransaction: TransactionEntity? = null,
    initialProduct: ProductEntity? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    customProducts: List<ProductEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        walletId: String,
        timestamp: Long,
        note: String,
        imageUri: String?
    ) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null,
    onAddCustomProduct: (name: String, type: String, defaultPrice: Double, imageUri: String?, colorHex: Long) -> Unit
) {
    var type by remember {
        mutableStateOf(
            if (initialTransaction != null) {
                if (initialTransaction.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
            } else if (initialProduct != null) {
                if (initialProduct.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
            } else {
                initialType
            }
        )
    }

    var title by remember {
        mutableStateOf(
            initialTransaction?.title ?: initialProduct?.name ?: ""
        )
    }
    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null) initialTransaction.amount.toLong().toString()
            else if (initialProduct != null && initialProduct.defaultPrice > 0) initialProduct.defaultPrice.toLong().toString()
            else ""
        )
    }
    var note by remember { mutableStateOf(initialTransaction?.note ?: "") }
    var selectedWallet by remember {
        mutableStateOf(
            if (initialTransaction != null) WalletType.fromId(initialTransaction.walletId) else WalletType.BANK
        )
    }
    var selectedProductName by remember {
        mutableStateOf(
            initialTransaction?.categoryId ?: initialProduct?.name ?: customProducts.firstOrNull()?.name ?: ""
        )
    }
    var timestamp by remember {
        mutableStateOf(initialTransaction?.timestamp ?: System.currentTimeMillis())
    }
    var transactionImageUri by remember {
        mutableStateOf(initialTransaction?.imageUri ?: initialProduct?.imageUri)
    }

    var showCreateProductDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Transaction photo receipt picker
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveUriToInternalStorage(context, uri)
            if (savedPath != null) {
                transactionImageUri = savedPath
            }
        }
    }

    // Filter custom products by active type (or ALL)
    val filteredProducts = remember(customProducts, type) {
        customProducts.filter { it.type == type.name || it.type == "ALL" }
    }

    val quickAmounts = listOf(10_000L, 20_000L, 50_000L, 100_000L, 500_000L, 1_000_000L)

    if (showCreateProductDialog) {
        AddEditProductDialog(
            initialType = type,
            onDismiss = { showCreateProductDialog = false },
            onProductSaved = { _, pName, pType, pPrice, pImg, pColor ->
                onAddCustomProduct(pName, pType, pPrice, pImg, pColor)
                selectedProductName = pName
                if (title.isBlank()) {
                    title = pName
                }
                if (pPrice > 0 && amountText.isBlank()) {
                    amountText = pPrice.toLong().toString()
                }
                if (pImg != null && transactionImageUri == null) {
                    transactionImageUri = pImg
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction == null) "Catat Transaksi" else "Ubah Transaksi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        if (initialTransaction != null && onDelete != null) {
                            IconButton(
                                onClick = {
                                    onDelete(initialTransaction)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("delete_transaction_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = ExpenseRed
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_transaction_dialog_button")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Type Switcher: Pengeluaran vs Pemasukan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                                .clickable { type = TransactionType.EXPENSE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pengeluaran",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (type == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                                .clickable { type = TransactionType.INCOME }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pemasukan",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (type == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Nominal Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                amountText = input
                            }
                        },
                        label = { Text("Nominal (Rp)") },
                        placeholder = { Text("Contoh: 50000") },
                        prefix = {
                            Text(
                                "Rp ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Quick Nominal Shortcut Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickAmounts.forEach { quickVal ->
                            AssistChip(
                                onClick = {
                                    val current = amountText.toLongOrNull() ?: 0L
                                    amountText = (current + quickVal).toString()
                                },
                                label = { Text("+${Formatters.formatCompactRupiah(quickVal.toDouble())}") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    // Judul / Keterangan Transaksi
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Produk / Transaksi") },
                        placeholder = { Text("cth. Kopi Susu, Baju Kaos, Restock Bahan") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_title_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Section Pilih Produk Custom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pilih Produk Custom",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pilih produk Anda atau buat baru dengan foto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { showCreateProductDialog = true },
                            modifier = Modifier.testTag("add_custom_product_shortcut_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Produk", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Custom Products Horizontal / Grid selector
                    if (filteredProducts.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCreateProductDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Belum ada produk custom untuk ${type.label}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ketuk untuk tambah produk custom Anda dengan foto!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 190.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { prod ->
                                val isSelected = selectedProductName == prod.name
                                val prodColor = Color(prod.colorHex)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) prodColor.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) prodColor else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedProductName = prod.name
                                            if (title.isBlank()) {
                                                title = prod.name
                                            }
                                            if (prod.defaultPrice > 0 && amountText.isBlank()) {
                                                amountText = prod.defaultPrice.toLong().toString()
                                            }
                                            if (prod.imageUri != null && transactionImageUri == null) {
                                                transactionImageUri = prod.imageUri
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(prodColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (prod.imageUri != null) {
                                            AsyncImage(
                                                model = prod.imageUri,
                                                contentDescription = prod.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Inventory,
                                                contentDescription = prod.name,
                                                tint = prodColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Foto Transaksi / Nota (Opsional)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lampiran Foto Nota / Produk",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        receiptPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(if (transactionImageUri == null) "+ Pilih Foto" else "Ubah Foto")
                                }
                            }

                            if (transactionImageUri != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = transactionImageUri,
                                        contentDescription = "Lampiran Foto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { transactionImageUri = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus Foto",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pilihan Dompet / Akun
                    Text(
                        text = "Sumber Dana / Dompet",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WalletType.entries.forEach { wallet ->
                            val isSelected = selectedWallet == wallet
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) wallet.color.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) wallet.color else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedWallet = wallet }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = wallet.icon,
                                    contentDescription = null,
                                    tint = wallet.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = wallet.title.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Pilihan Tanggal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val selectedCal = Calendar.getInstance().apply {
                                            set(y, m, d, 12, 0, 0)
                                        }
                                        timestamp = selectedCal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tanggal Transaksi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatDate(timestamp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Catatan Tambahan (Opsional)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan Tambahan (Opsional)") },
                        placeholder = { Text("cth. Pembayaran transfer, pelanggan baru") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Simpan
                val amountValue = amountText.toDoubleOrNull() ?: 0.0
                val effectiveTitle = title.ifBlank { selectedProductName }
                val isValid = amountValue > 0.0 && effectiveTitle.isNotBlank()

                Button(
                    onClick = {
                        if (isValid) {
                            onSave(
                                initialTransaction?.id ?: 0L,
                                effectiveTitle,
                                amountValue,
                                type,
                                selectedProductName.ifBlank { effectiveTitle },
                                selectedWallet.id,
                                timestamp,
                                note,
                                transactionImageUri
                            )
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                    )
                ) {
                    Text(
                        text = if (initialTransaction == null) "Simpan Transaksi" else "Perbarui Transaksi",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
