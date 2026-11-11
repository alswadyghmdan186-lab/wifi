package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_invoices")
data class SaleInvoiceEntity(
    @PrimaryKey
    val id: String,
    val storeId: String,
    val storeName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // Format: YYYY-MM-DD for daily aggregation
    val itemsSummary: String, // Human readable items summary
    val totalAmount: Double,
    val cashPaid: Double,
    val remainingDebt: Double,
    val nextDeliveryDate: Long? = null
)
