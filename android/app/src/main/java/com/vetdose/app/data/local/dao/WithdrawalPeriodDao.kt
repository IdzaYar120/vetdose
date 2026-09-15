package com.vetdose.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vetdose.app.data.local.entity.WithdrawalPeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WithdrawalPeriodDao {
    @Query("SELECT * FROM withdrawal_period WHERE productId = :productId")
    fun observeByProduct(productId: String): Flow<List<WithdrawalPeriodEntity>>

    @Upsert
    suspend fun upsertAll(items: List<WithdrawalPeriodEntity>)

    @Query("DELETE FROM withdrawal_period WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
