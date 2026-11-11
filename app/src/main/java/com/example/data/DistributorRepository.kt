package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class DistributorRepository(private val database: AppDatabase) {

    private val storeDao = database.storeDao()
    private val bagDao = database.bagDao()
    private val saleDao = database.saleDao()

    val allStoresFlow: Flow<List<StoreEntity>> = storeDao.getAllStoresFlow()

    val allBagItemsFlow: Flow<Map<Int, BagItemEntity>> = bagDao.getAllBagItemsFlow().map { list ->
        val map = list.associateBy { it.denomination }.toMutableMap()
        // Ensure all default denominations exist
        for (cat in AppDatabase.DEFAULT_DENOMINATIONS) {
            if (!map.containsKey(cat)) {
                map[cat] = BagItemEntity(denomination = cat, morningCount = 0, currentCount = 0)
            }
        }
        map
    }

    fun getTodaySalesFlow(): Flow<List<SaleInvoiceEntity>> {
        val todayStr = getTodayDateString()
        return saleDao.getSalesForDateFlow(todayStr)
    }

    suspend fun ensureDefaultBagItems() = withContext(Dispatchers.IO) {
        val existing = bagDao.getAllBagItems()
        val missing = AppDatabase.DEFAULT_DENOMINATIONS.filter { cat ->
            existing.none { it.denomination == cat }
        }.map { BagItemEntity(denomination = it, morningCount = 0, currentCount = 0) }

        if (missing.isNotEmpty()) {
            bagDao.insertOrUpdateBagItems(missing)
        }
    }

    suspend fun addOrUpdateStore(store: StoreEntity) = withContext(Dispatchers.IO) {
        storeDao.insertOrUpdateStore(store)
    }

    suspend fun deleteStore(storeId: String) = withContext(Dispatchers.IO) {
        storeDao.deleteStoreById(storeId)
    }

    suspend fun payStoreDebt(storeId: String, paymentAmount: Double) = withContext(Dispatchers.IO) {
        storeDao.payDebt(storeId, paymentAmount)
    }

    suspend fun updateMorningBag(newMorningCounts: Map<Int, Int>) = withContext(Dispatchers.IO) {
        val entities = AppDatabase.DEFAULT_DENOMINATIONS.map { cat ->
            val count = newMorningCounts[cat] ?: 0
            BagItemEntity(denomination = cat, morningCount = count, currentCount = count)
        }
        bagDao.insertOrUpdateBagItems(entities)
    }

    suspend fun completeSale(
        store: StoreEntity,
        itemsSold: Map<Int, Int>, // cat -> qty
        totalInvoice: Double,
        cashPaid: Double,
        nextDeliveryDate: Long?
    ): SaleInvoiceEntity = withContext(Dispatchers.IO) {
        // 1. Deduct from bag
        for ((cat, qty) in itemsSold) {
            if (qty > 0) {
                bagDao.deductFromBag(cat, qty)
            }
        }

        // 2. Update store debt and next delivery date
        val remainingDebt = totalInvoice - cashPaid
        storeDao.applySaleToStore(
            storeId = store.id,
            deltaDebt = remainingDebt,
            nextDate = nextDeliveryDate
        )

        // 3. Create items summary
        val summaryItems = itemsSold.filter { it.value > 0 }
            .map { "فئة ${it.key} (${it.value} كرت)" }
            .joinToString("، ")

        val saleInvoice = SaleInvoiceEntity(
            id = UUID.randomUUID().toString(),
            storeId = store.id,
            storeName = store.name,
            timestamp = System.currentTimeMillis(),
            dateString = getTodayDateString(),
            itemsSummary = summaryItems,
            totalAmount = totalInvoice,
            cashPaid = cashPaid,
            remainingDebt = remainingDebt,
            nextDeliveryDate = nextDeliveryDate
        )

        saleDao.insertSale(saleInvoice)
        saleInvoice
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }
    }
}
