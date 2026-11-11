package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bag_inventory")
data class BagItemEntity(
    @PrimaryKey
    val denomination: Int, // 100, 150, 200, 250, 300, 500, 1000
    val morningCount: Int = 0,
    val currentCount: Int = 0
)
