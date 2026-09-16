package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vetdose.app.data.local.entity.FavoriteProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteProductDao {
    @Query("SELECT * FROM favorite_product ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteProductEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_product WHERE productId = :productId)")
    fun observeIsFavorite(productId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entry: FavoriteProductEntity)

    @Query("DELETE FROM favorite_product WHERE productId = :productId")
    suspend fun remove(productId: String)
}
