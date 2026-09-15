package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.ContraindicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContraindicationDao {
    @Query("SELECT * FROM contraindication WHERE substanceId = :substanceId")
    fun observeBySubstance(substanceId: String): Flow<List<ContraindicationEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ContraindicationEntity>)

    @Query("DELETE FROM contraindication WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
