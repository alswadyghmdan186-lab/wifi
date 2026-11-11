package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.DistributorViewModel
import com.example.ui.ReceiptUtils
import com.example.ui.components.BagSetupDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.StoreDialog
import com.example.ui.screens.DailyRunScreen
import com.example.ui.screens.SaleScreen
import com.example.ui.screens.StoresScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DistributorViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                // Ensure RTL layout for Arabic
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: DistributorViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var showMorningBagDialog by remember { mutableStateOf(false) }

    val lastReceiptText by viewModel.lastSaleReceipt.collectAsState()
    val bagItems by viewModel.bagItems.collectAsState()
    var receiptPhone by remember { mutableStateOf("") }

    val titles = listOf(
        "تسليم كروت لمحل",
        "سجل البقالات والديون",
        "تقرير جولة اليوم والمطابقة"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titles[selectedTab],
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    when (selectedTab) {
                        1 -> {
                            IconButton(
                                onClick = { showAddStoreDialog = true },
                                modifier = Modifier.testTag("appbar_add_store_button")
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "إضافة بقالة",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                        2 -> {
                            IconButton(
                                onClick = { showMorningBagDialog = true },
                                modifier = Modifier.testTag("appbar_edit_bag_button")
                            ) {
                                Icon(
                                    Icons.Default.EditNote,
                                    contentDescription = "تعبئة رصيد الصباح",
                                    tint = MintSecondary
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    },
                    label = { Text("تسليم كروت") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_sale")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(Icons.Default.Storefront, contentDescription = null)
                    },
                    label = { Text("البقالات والديون") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_stores")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null)
                    },
                    label = { Text("جولة اليوم") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_daily_run")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> SaleScreen(
                    viewModel = viewModel,
                    onNavigateToStores = { selectedTab = 1 },
                    onSaleCompleted = { receipt, phone ->
                        receiptPhone = phone
                        // Trigger immediate WhatsApp send as requested by original behavior,
                        // and the ReceiptDialog remains accessible
                        ReceiptUtils.sendViaWhatsApp(context, phone, receipt)
                    }
                )
                1 -> StoresScreen(viewModel = viewModel)
                2 -> DailyRunScreen(
                    viewModel = viewModel,
                    onDeliverToStore = { store ->
                        viewModel.selectStoreForSale(store)
                        selectedTab = 0
                    }
                )
            }
        }
    }

    // TopBar triggered Store Dialog
    if (showAddStoreDialog) {
        StoreDialog(
            existingStore = null,
            onDismiss = { showAddStoreDialog = false },
            onConfirm = { id, name, phone, debt, nextDate, address, notes ->
                viewModel.addOrUpdateStore(id, name, phone, debt, nextDate, address, notes)
                showAddStoreDialog = false
            }
        )
    }

    // TopBar triggered Bag Setup Dialog
    if (showMorningBagDialog) {
        BagSetupDialog(
            cardTypes = viewModel.cardTypes,
            currentBag = bagItems,
            onDismiss = { showMorningBagDialog = false },
            onSave = { updatedBag ->
                viewModel.updateMorningBag(updatedBag)
                showMorningBagDialog = false
            }
        )
    }

    // Completed Sale Receipt Dialog
    if (lastReceiptText != null) {
        ReceiptDialog(
            receiptText = lastReceiptText!!,
            storePhone = receiptPhone,
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }
}
