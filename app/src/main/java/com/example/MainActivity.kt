package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProductEntity
import com.example.data.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.screen.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceViewModel

enum class MainTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Produk", Icons.Default.Inventory2, "tab_products"),
    CASH("Uang Kas", Icons.Default.AccountBalanceWallet, "tab_cash"),
    PROFILE("Akun / Profil", Icons.Default.Person, "tab_profile")
}

enum class SubScreen {
    ALL_TRANSACTIONS,
    MANAGE_PRODUCTS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    financeViewModel: FinanceViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddProductDialogDirectly by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedProductForAdd by remember { mutableStateOf<ProductEntity?>(null) }
    var presetTypeForAdd by remember { mutableStateOf(TransactionType.EXPENSE) }

    val customProducts by financeViewModel.customProducts.collectAsState()

    // Handle Android system back button when on sub-tabs or sub-screens
    BackHandler(enabled = subScreen != null || currentTab != MainTab.HOME) {
        if (subScreen != null) {
            subScreen = null
        } else {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (subScreen == null && currentTab == MainTab.HOME) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_saletreng_logo),
                                    contentDescription = "Logo Saletreng Store",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = if (subScreen != null) {
                                    when (subScreen) {
                                        SubScreen.ALL_TRANSACTIONS -> "Semua Transaksi"
                                        SubScreen.MANAGE_PRODUCTS -> "Kelola Produk & Foto"
                                        null -> ""
                                    }
                                } else {
                                    when (currentTab) {
                                        MainTab.HOME -> "Saletreng Store"
                                        MainTab.CASH -> "Uang Kas Toko"
                                        MainTab.PROFILE -> "Akun & Profil"
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (subScreen != null) {
                        IconButton(onClick = { subScreen = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Produk"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab && subScreen == null
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            subScreen = null
                            currentTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == MainTab.HOME && subScreen == null) {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        selectedProductForAdd = null
                        presetTypeForAdd = TransactionType.EXPENSE
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catat Transaksi"
                    )
                }
            } else if (subScreen == SubScreen.MANAGE_PRODUCTS) {
                FloatingActionButton(
                    onClick = { showAddProductDialogDirectly = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_product")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Tambah Produk"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (subScreen != null) {
                when (subScreen) {
                    SubScreen.ALL_TRANSACTIONS -> {
                        TransactionsScreen(
                            viewModel = financeViewModel,
                            onTransactionClick = { tx ->
                                editingTransaction = tx
                                selectedProductForAdd = null
                                showAddDialog = true
                            }
                        )
                    }
                    SubScreen.MANAGE_PRODUCTS -> {
                        ProductsScreen(
                            viewModel = financeViewModel,
                            onAddTransactionForProduct = { prod ->
                                editingTransaction = null
                                selectedProductForAdd = prod
                                presetTypeForAdd = if (prod.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                                showAddDialog = true
                            }
                        )
                    }
                    null -> {}
                }
            } else {
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeScreen(
                            viewModel = financeViewModel,
                            onNavigateToTransactions = {
                                subScreen = SubScreen.ALL_TRANSACTIONS
                            },
                            onNavigateToProfile = {
                                subScreen = null
                                currentTab = MainTab.PROFILE
                            },
                            onNavigateToProducts = {
                                subScreen = SubScreen.MANAGE_PRODUCTS
                            },
                            onNavigateToCash = {
                                subScreen = null
                                currentTab = MainTab.CASH
                            },
                            onProductClick = { prod ->
                                editingTransaction = null
                                selectedProductForAdd = prod
                                presetTypeForAdd = if (prod.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                                showAddDialog = true
                            },
                            onAddExpenseClick = {
                                editingTransaction = null
                                selectedProductForAdd = null
                                presetTypeForAdd = TransactionType.EXPENSE
                                showAddDialog = true
                            },
                            onAddIncomeClick = {
                                editingTransaction = null
                                selectedProductForAdd = null
                                presetTypeForAdd = TransactionType.INCOME
                                showAddDialog = true
                            },
                            onTransactionClick = { tx ->
                                editingTransaction = tx
                                selectedProductForAdd = null
                                showAddDialog = true
                            }
                        )
                    }
                    MainTab.CASH -> {
                        CashScreen(viewModel = financeViewModel)
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = financeViewModel,
                            onNavigateToTransactions = {
                                subScreen = SubScreen.ALL_TRANSACTIONS
                            },
                            onNavigateToProducts = {
                                subScreen = SubScreen.MANAGE_PRODUCTS
                            }
                        )
                    }
                }
            }
        }

        // Add or Edit Transaction Dialog
        if (showAddDialog) {
            AddEditTransactionDialog(
                initialTransaction = editingTransaction,
                initialProduct = selectedProductForAdd,
                initialType = presetTypeForAdd,
                customProducts = customProducts,
                onDismiss = {
                    showAddDialog = false
                    editingTransaction = null
                    selectedProductForAdd = null
                },
                onSave = { id, title, amount, type, categoryId, walletId, timestamp, note, imageUri ->
                    financeViewModel.saveTransaction(
                        id = id,
                        title = title,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        walletId = walletId,
                        timestamp = timestamp,
                        note = note,
                        imageUri = imageUri
                    )
                    showAddDialog = false
                    editingTransaction = null
                    selectedProductForAdd = null
                },
                onDelete = { tx ->
                    financeViewModel.deleteTransaction(tx)
                    showAddDialog = false
                    editingTransaction = null
                    selectedProductForAdd = null
                },
                onAddCustomProduct = { pName, pType, pPrice, pImg, pColor ->
                    financeViewModel.addCustomProduct(pName, pType, pPrice, pImg, pColor)
                }
            )
        }

        // Direct Add Product Dialog (from FAB on Products Tab)
        if (showAddProductDialogDirectly) {
            AddEditProductDialog(
                onDismiss = { showAddProductDialogDirectly = false },
                onProductSaved = { id, pName, pType, pPrice, pImg, pColor ->
                    financeViewModel.saveCustomProduct(id, pName, pType, pPrice, pImg, pColor)
                    showAddProductDialogDirectly = false
                }
            )
        }
    }
}
