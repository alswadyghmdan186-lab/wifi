package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DistributorRepository
import com.example.data.StoreEntity
import com.example.ui.ReceiptUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DistributorRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DistributorRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("موزع كروت الواي فاي", appName)
    }

    @Test
    fun testStoreAndSaleWorkflow() = runBlocking {
        // 1. Initialize bag with morning inventory
        val morningCounts = mapOf(100 to 50, 200 to 30, 500 to 20)
        repository.updateMorningBag(morningCounts)

        val initialBag = repository.allBagItemsFlow.first()
        assertEquals(50, initialBag[100]?.morningCount)
        assertEquals(50, initialBag[100]?.currentCount)

        // 2. Add store
        val store = StoreEntity(
            id = "store_1",
            name = "بقالة النور",
            phone = "967771234567",
            debt = 500.0
        )
        repository.addOrUpdateStore(store)

        val stores = repository.allStoresFlow.first()
        assertEquals(1, stores.size)
        assertEquals("بقالة النور", stores[0].name)

        // 3. Complete a sale: 5 cards of 100, 2 cards of 200 -> total = 5*100 + 2*200 = 900
        // Cash paid = 500, Remaining debt = 400
        val itemsSold = mapOf(100 to 5, 200 to 2)
        val invoice = repository.completeSale(
            store = store,
            itemsSold = itemsSold,
            totalInvoice = 900.0,
            cashPaid = 500.0,
            nextDeliveryDate = System.currentTimeMillis() + 86400000L
        )

        assertNotNull(invoice)
        assertEquals(900.0, invoice.totalAmount, 0.01)
        assertEquals(500.0, invoice.cashPaid, 0.01)
        assertEquals(400.0, invoice.remainingDebt, 0.01)

        // 4. Verify Bag stock was deducted
        val updatedBag = repository.allBagItemsFlow.first()
        assertEquals(45, updatedBag[100]?.currentCount) // 50 - 5
        assertEquals(28, updatedBag[200]?.currentCount) // 30 - 2

        // 5. Verify Store debt was updated (500 initial + 400 new = 900)
        val updatedStores = repository.allStoresFlow.first()
        assertEquals(900.0, updatedStores[0].debt, 0.01)

        // 6. Test Receipt generation
        val receipt = ReceiptUtils.generateReceiptText(
            store = store,
            itemsSold = itemsSold,
            total = 900.0,
            cash = 500.0,
            remaining = 400.0,
            newTotalDebt = 900.0,
            nextDate = null
        )
        assertTrue(receipt.contains("فاتورة تسليم كروت شبكة"))
        assertTrue(receipt.contains("بقالة النور"))
        assertTrue(receipt.contains("900 ريال"))
    }
}
