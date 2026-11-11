package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BagDao {
    @Query("SELECT * FROM bag_inventory ORDER BY denomination ASC")
    fun getAllBagItemsFlow(): Flow<List<BagItemEntity>>

    @Query("SELECT * FROM bag_inventory ORDER BY denomination ASC")
    suspend fun getAllBagItems(): List<BagItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBagItems(items: List<BagItemEntity>)

    @Query("UPDATE bag_inventory SET currentCount = currentCount - :qty WHERE denomination = :denomination")
    suspend fun deductFromBag(denomination: Int, qty: Int)

    @Query("UPDATE bag_inventory SET morningCount = :morning, currentCount = :current WHERE denomination = :denomination")
    suspend fun setDenominationCount(denomination: Int, morning: Int, current: Int)
}
