package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.DoseRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseRuleDao {
    @Query("SELECT * FROM dose_rule ORDER BY id")
    fun observeAll(): Flow<List<DoseRuleEntity>>

    @Query("SELECT * FROM dose_rule WHERE substanceId = :substanceId")
    fun observeBySubstance(substanceId: String): Flow<List<DoseRuleEntity>>

    @Query("SELECT * FROM dose_rule WHERE id = :id")
    suspend fun getById(id: String): DoseRuleEntity?

    @Upsert
    suspend fun upsertAll(items: List<DoseRuleEntity>)

    @Query("DELETE FROM dose_rule WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
