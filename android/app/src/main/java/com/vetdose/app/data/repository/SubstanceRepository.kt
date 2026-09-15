package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.SubstanceDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.Substance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SubstanceRepository @Inject constructor(private val substanceDao: SubstanceDao) {
    fun observeAll(): Flow<List<Substance>> =
        substanceDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Substance? = substanceDao.getById(id)?.toDomain()
}
