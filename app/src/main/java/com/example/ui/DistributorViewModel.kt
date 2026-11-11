package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BagItemEntity
import com.example.data.DistributorRepository
import com.example.data.SaleInvoiceEntity
import com.example.data.StoreEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class SaleFormState(
    val selectedStore: StoreEntity? = null,
    val selectedCards: Map<Int, Int> = emptyMap(),
    val cashPaidText: String = "",
    val nextDeliveryDate: Long? = null
) {
    val totalInvoiceAmount: Double
        get() = selectedCards.entries.sumOf { (cat, count) -> (cat * count).toDouble() }

    val cashPaid: Double
        get() = cashPaidText.toDoubleOrNull() ?: 0.0

    val remainingDebt: Double
        get() = (totalInvoiceAmount - cashPaid).coerceAtLeast(0.0)
}

class DistributorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DistributorRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DistributorRepository(db)
        viewModelScope.launch {
            repository.ensureDefaultBagItems()
        }
    }

    val cardTypes: List<Int> = AppDatabase.DEFAULT_DENOMINATIONS

    val allStores: StateFlow<List<StoreEntity>> = repository.allStoresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bagItems: StateFlow<Map<Int, BagItemEntity>> = repository.allBagItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val todaySales: StateFlow<List<SaleInvoiceEntity>> = repository.getTodaySalesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredStores: StateFlow<List<StoreEntity>> = combine(allStores, searchQuery) { stores, query ->
        if (query.isBlank()) {
            stores
        } else {
            val q = query.trim().lowercase()
            stores.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalMarketDebt: StateFlow<Double> = allStores.combine(MutableStateFlow(Unit)) { stores, _ ->
        stores.sumOf { it.debt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCashToday: StateFlow<Double> = todaySales.combine(MutableStateFlow(Unit)) { sales, _ ->
        sales.sumOf { it.cashPaid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalNewDebtToday: StateFlow<Double> = todaySales.combine(MutableStateFlow(Unit)) { sales, _ ->
        sales.filter { it.remainingDebt > 0 }.sumOf { it.remainingDebt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val storesDueToday: StateFlow<List<StoreEntity>> = allStores.combine(MutableStateFlow(Unit)) { stores, _ ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endOfTodayMillis = cal.timeInMillis

        stores.filter { store ->
            store.nextDeliveryDate != null && store.nextDeliveryDate <= endOfTodayMillis
        }.sortedBy { it.nextDeliveryDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Sale Form State ---
    private val _saleForm = MutableStateFlow(SaleFormState())
    val saleForm: StateFlow<SaleFormState> = _saleForm.asStateFlow()

    // Last completed sale receipt popup dialog state
    private val _lastSaleReceipt = MutableStateFlow<String?>(null)
    val lastSaleReceipt: StateFlow<String?> = _lastSaleReceipt.asStateFlow()

    fun dismissReceiptDialog() {
        _lastSaleReceipt.value = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun selectStoreForSale(store: StoreEntity?) {
        _saleForm.value = _saleForm.value.copy(
            selectedStore = store,
            nextDeliveryDate = store?.nextDeliveryDate
        )
    }

    fun setCardCount(cat: Int, count: Int) {
        val currentBagCount = bagItems.value[cat]?.currentCount ?: 0
        val clamped = count.coerceIn(0, currentBagCount)
        val updated = _saleForm.value.selectedCards.toMutableMap()
        if (clamped <= 0) {
            updated.remove(cat)
        } else {
            updated[cat] = clamped
        }
        _saleForm.value = _saleForm.value.copy(selectedCards = updated)
    }

    fun adjustCardCount(cat: Int, delta: Int) {
        val currentCount = _saleForm.value.selectedCards[cat] ?: 0
        setCardCount(cat, currentCount + delta)
    }

    fun setCashPaid(cash: String) {
        _saleForm.value = _saleForm.value.copy(cashPaidText = cash)
    }

    fun setNextDeliveryDate(date: Long?) {
        _saleForm.value = _saleForm.value.copy(nextDeliveryDate = date)
    }

    fun resetSaleForm() {
        val firstStore = allStores.value.firstOrNull()
        _saleForm.value = SaleFormState(
            selectedStore = firstStore,
            nextDeliveryDate = firstStore?.nextDeliveryDate
        )
    }

    fun completeSale(onSuccess: (receiptText: String, phone: String) -> Unit) {
        val form = _saleForm.value
        val store = form.selectedStore ?: return
        val total = form.totalInvoiceAmount
        if (total <= 0) return

        val cash = form.cashPaid
        val remaining = form.remainingDebt
        val nextDate = form.nextDeliveryDate
        val items = form.selectedCards.filter { it.value > 0 }
        val newStoreDebt = store.debt + remaining

        viewModelScope.launch {
            repository.completeSale(
                store = store,
                itemsSold = items,
                totalInvoice = total,
                cashPaid = cash,
                nextDeliveryDate = nextDate
            )

            val receiptText = ReceiptUtils.generateReceiptText(
                store = store,
                itemsSold = items,
                total = total,
                cash = cash,
                remaining = remaining,
                newTotalDebt = newStoreDebt,
                nextDate = nextDate
            )

            _lastSaleReceipt.value = receiptText
            resetSaleForm()
            onSuccess(receiptText, store.phone)
        }
    }

    fun addOrUpdateStore(
        id: String?,
        name: String,
        phone: String,
        debt: Double,
        nextDate: Long?,
        address: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val store = StoreEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                debt = debt,
                nextDeliveryDate = nextDate,
                address = address.trim(),
                notes = notes.trim()
            )
            repository.addOrUpdateStore(store)

            // If this store was currently selected, refresh it
            if (_saleForm.value.selectedStore?.id == store.id) {
                _saleForm.value = _saleForm.value.copy(selectedStore = store)
            }
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            repository.deleteStore(storeId)
            if (_saleForm.value.selectedStore?.id == storeId) {
                val nextStore = allStores.value.firstOrNull { it.id != storeId }
                _saleForm.value = _saleForm.value.copy(
                    selectedStore = nextStore,
                    nextDeliveryDate = nextStore?.nextDeliveryDate
                )
            }
        }
    }

    fun payStoreDebt(storeId: String, amount: Double) {
        viewModelScope.launch {
            repository.payStoreDebt(storeId, amount)
        }
    }

    fun updateMorningBag(counts: Map<Int, Int>) {
        viewModelScope.launch {
            repository.updateMorningBag(counts)
        }
    }
}
