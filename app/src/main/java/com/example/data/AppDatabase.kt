package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [StoreEntity::class, BagItemEntity::class, SaleInvoiceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun bagDao(): BagDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val DEFAULT_DENOMINATIONS = listOf(100, 150, 200, 250, 300, 500, 1000)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wifi_distributor.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val bagDao = getDatabase(context).bagDao()
                            val initialItems = DEFAULT_DENOMINATIONS.map { cat ->
                                BagItemEntity(denomination = cat, morningCount = 0, currentCount = 0)
                            }
                            bagDao.insertOrUpdateBagItems(initialItems)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
