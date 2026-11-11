package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val debt: Double = 0.0,
    val nextDeliveryDate: Long? = null,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
