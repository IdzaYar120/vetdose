package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.SubstanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubstanceDao {
    @Query("SELECT * FROM substance ORDER BY nameUk")
    fun observeAll(): Flow<List<SubstanceEntity>>

    @Query("SELECT * FROM substance WHERE id = :id")
    suspend fun getById(id: String): SubstanceEntity?

    @Upsert
    suspend fun upsertAll(items: List<SubstanceEntity>)

    @Query("DELETE FROM substance WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
