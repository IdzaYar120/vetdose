package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.SpeciesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {
    @Query("SELECT * FROM species ORDER BY nameUk")
    fun observeAll(): Flow<List<SpeciesEntity>>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getById(id: String): SpeciesEntity?

    @Upsert
    suspend fun upsertAll(items: List<SpeciesEntity>)

    @Query("DELETE FROM species WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM species")
    suspend fun count(): Int
}
