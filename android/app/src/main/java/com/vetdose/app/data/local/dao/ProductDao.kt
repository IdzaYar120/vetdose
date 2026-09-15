package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM product ORDER BY tradeName")
    fun observeAll(): Flow<List<ProductEntity>>

    // Free-text search is done in ProductRepository (Kotlin's contains(ignoreCase
    // = true) is Unicode-aware; SQLite's LIKE case-folding is ASCII-only and
    // would silently miss Cyrillic matches like "амокс" vs "Амокс").
    @Query(
        """
        SELECT DISTINCT product.* FROM product
        INNER JOIN dose_rule ON dose_rule.substanceId = product.substanceId
        WHERE dose_rule.speciesId = :speciesId
        ORDER BY product.tradeName
        """,
    )
    fun observeBySpecies(speciesId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Upsert
    suspend fun upsertAll(items: List<ProductEntity>)

    @Query("DELETE FROM product WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
