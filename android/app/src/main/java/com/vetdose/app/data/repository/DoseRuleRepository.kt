package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.DoseRuleDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.DoseRule
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DoseRuleRepository @Inject constructor(private val doseRuleDao: DoseRuleDao) {
    fun observeBySubstance(substanceId: String): Flow<List<DoseRule>> =
        doseRuleDao.observeBySubstance(substanceId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): DoseRule? = doseRuleDao.getById(id)?.toDomain()
}
