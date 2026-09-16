package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.DoseRuleDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.DoseRule
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DoseRuleRepository @Inject constructor(private val doseRuleDao: DoseRuleDao) {
    fun observeBySubstance(substanceId: String): Flow<List<DoseRule>> =
        doseRuleDao.observeBySubstance(substanceId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): DoseRule? = doseRuleDao.getById(id)?.toDomain()

    /** The Calculate screen's product picker already narrows products to
     * ones with a rule for the chosen species, so there should be exactly
     * one match; null means the data changed underneath the user (e.g. a
     * sync just removed the rule) and they need to re-pick. */
    suspend fun findForSubstanceAndSpecies(substanceId: String, speciesId: String): DoseRule? =
        observeBySubstance(substanceId).first().firstOrNull { it.speciesId == speciesId }
}
