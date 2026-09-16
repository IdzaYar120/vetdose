package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vetdose.app.data.local.entity.CalculationHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationHistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<CalculationHistoryEntity>>

    @Insert
    suspend fun insert(entry: CalculationHistoryEntity)

    @Query("DELETE FROM calculation_history")
    suspend fun clear()
}
