package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sale_invoices ORDER BY timestamp DESC")
    fun getAllSalesFlow(): Flow<List<SaleInvoiceEntity>>

    @Query("SELECT * FROM sale_invoices WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getSalesForDateFlow(dateString: String): Flow<List<SaleInvoiceEntity>>

    @Query("SELECT * FROM sale_invoices WHERE dateString = :dateString ORDER BY timestamp DESC")
    suspend fun getSalesForDate(dateString: String): List<SaleInvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleInvoiceEntity)
}
