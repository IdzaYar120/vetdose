package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.CalculationHistoryDao
import com.vetdose.app.data.local.entity.CalculationHistoryEntity
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.CalculationHistoryEntry
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class HistoryRepository @Inject constructor(private val historyDao: CalculationHistoryDao) {
    fun observeRecent(limit: Int = 50): Flow<List<CalculationHistoryEntry>> =
        historyDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun record(speciesId: String, doseRuleId: String, productId: String, weightKg: BigDecimal) {
        historyDao.insert(
            CalculationHistoryEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                speciesId = speciesId,
                doseRuleId = doseRuleId,
                productId = productId,
                weightKg = weightKg.toString(),
            ),
        )
    }
}
