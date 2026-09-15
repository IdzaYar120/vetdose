package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.ContraindicationDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.Contraindication
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContraindicationRepository @Inject constructor(private val contraindicationDao: ContraindicationDao) {
    fun observeBySubstance(substanceId: String): Flow<List<Contraindication>> =
        contraindicationDao.observeBySubstance(substanceId).map { list -> list.map { it.toDomain() } }
}
