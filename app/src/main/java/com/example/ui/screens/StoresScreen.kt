package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StoreEntity
import com.example.ui.DistributorViewModel
import com.example.ui.ReceiptUtils
import com.example.ui.components.DebtPaymentDialog
import com.example.ui.components.StoreDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintSecondary
import com.example.ui.theme.SoftBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StoresScreen(
    viewModel: DistributorViewModel
) {
    val context = LocalContext.current
    val stores by viewModel.filteredStores.collectAsState()
    val totalMarketDebt by viewModel.totalMarketDebt.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddStoreDialog by remember { mutableStateOf(false) }
    var storeToEdit by remember { mutableStateOf<StoreEntity?>(null) }
    var storeToDelete by remember { mutableStateOf<StoreEntity?>(null) }
    var storeToPayDebt by remember { mutableStateOf<StoreEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- 1. Total Debts in Market Banner ---
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي الديون في السوق:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stores.size} بقالة مسجلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = "${totalMarketDebt.toInt()} ريال",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AmberAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- 2. Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("ابحث باسم البقالة أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_stores_input")
            )

            // --- 3. Stores List ---
            if (stores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث" else "لا توجد أي بقالات بعد",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على زر (+) في الأسفل لإضافة بقالة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MintSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(stores, key = { it.id }) { store ->
                        StoreCardItem(
                            store = store,
                            onEdit = { storeToEdit = store },
                            onDelete = { storeToDelete = store },
                            onPayDebt = { storeToPayDebt = store },
                            onCall = { ReceiptUtils.dialPhoneNumber(context, store.phone) },
                            onWhatsApp = {
                                val greeting = "مرحباً يا ${store.name}، بخصوص كروت الواي فاي والحساب المشترك."
                                ReceiptUtils.sendViaWhatsApp(context, store.phone, greeting)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // --- FAB to Add Store ---
        FloatingActionButton(
            onClick = { showAddStoreDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_store_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة بقالة")
        }
    }

    // --- Add / Edit Dialog ---
    if (showAddStoreDialog || storeToEdit != null) {
        val target = storeToEdit
        StoreDialog(
            existingStore = target,
            onDismiss = {
                showAddStoreDialog = false
                storeToEdit = null
            },
            onConfirm = { id, name, phone, debt, nextDate, address, notes ->
                viewModel.addOrUpdateStore(id, name, phone, debt, nextDate, address, notes)
                showAddStoreDialog = false
                storeToEdit = null
            }
        )
    }

    // --- Delete Confirmation Dialog ---
    if (storeToDelete != null) {
        val store = storeToDelete!!
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            title = { Text("تأكيد حذف البقالة") },
            text = { Text("هل أنت متأكد من حذف ${store.name} من قائمة البقالات؟ لن يتم التراجع عن هذه الخطوة.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStore(store.id)
                        storeToDelete = null
                    }
                ) {
                    Text("حذف", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Debt Payment Dialog ---
    if (storeToPayDebt != null) {
        DebtPaymentDialog(
            store = storeToPayDebt!!,
            onDismiss = { storeToPayDebt = null },
            onConfirm = { amount ->
                viewModel.payStoreDebt(storeToPayDebt!!.id, amount)
                storeToPayDebt = null
            }
        )
    }
}

@Composable
private fun StoreCardItem(
    store: StoreEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPayDebt: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val deliveryInfo = remember(store.nextDeliveryDate) {
        getDeliveryStatus(store.nextDeliveryDate)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("store_card_${store.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Store Name + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = SoftBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = CoralRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Info row: Phone & Debt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (store.phone.isNotBlank()) "📞 ${store.phone}" else "بدون رقم هاتف",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الدين: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${store.debt.toInt()} ريال",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (store.debt > 0) AmberAccent else EmeraldPrimary
                    )
                }
            }

            // Delivery Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = deliveryInfo.color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "موعد التوزيع: ${deliveryInfo.text}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = deliveryInfo.color
                )
            }

            // Quick Actions: WhatsApp, Call, Pay Debt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (store.debt > 0) {
                    TextButton(
                        onClick = onPayDebt,
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد دفعة", fontSize = 12.sp, color = EmeraldPrimary)
                    }
                }

                if (store.phone.isNotBlank()) {
                    IconButton(
                        onClick = onWhatsApp,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "واتساب",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SoftBlue.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "اتصال",
                            tint = SoftBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class DeliveryStatus(val text: String, val color: Color)

private fun getDeliveryStatus(dateMillis: Long?): DeliveryStatus {
    if (dateMillis == null) {
        return DeliveryStatus("غير محدد", Color(0xFF94A3B8))
    }

    val now = Calendar.getInstance()
    val todayMidnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val targetCal = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffDays = ((targetCal.timeInMillis - todayMidnight.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = sdf.format(Date(dateMillis))

    return when {
        diffDays == 0 -> DeliveryStatus("اليوم (يحين التوزيع)", AmberAccent)
        diffDays == 1 -> DeliveryStatus("غداً ($dateStr)", MintSecondary)
        diffDays < 0 -> DeliveryStatus("متأخر منذ ${-diffDays} يوم", CoralRed)
        else -> DeliveryStatus("بعد $diffDays أيام ($dateStr)", MintSecondary)
    }
}
