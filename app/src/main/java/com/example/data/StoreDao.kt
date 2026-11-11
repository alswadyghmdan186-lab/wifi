package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStoresFlow(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getStoreById(id: String): StoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStore(store: StoreEntity)

    @Update
    suspend fun updateStore(store: StoreEntity)

    @Query("UPDATE stores SET debt = debt + :deltaDebt, nextDeliveryDate = :nextDate WHERE id = :storeId")
    suspend fun applySaleToStore(storeId: String, deltaDebt: Double, nextDate: Long?)

    @Query("UPDATE stores SET debt = debt - :paymentAmount WHERE id = :storeId")
    suspend fun payDebt(storeId: String, paymentAmount: Double)

    @Delete
    suspend fun deleteStore(store: StoreEntity)

    @Query("DELETE FROM stores WHERE id = :storeId")
    suspend fun deleteStoreById(storeId: String)
}
