package com.vetdose.app.data.repository

import com.vetdose.app.data.local.dao.SpeciesDao
import com.vetdose.app.data.mapper.toDomain
import com.vetdose.app.domain.model.Species
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SpeciesRepository @Inject constructor(private val speciesDao: SpeciesDao) {
    fun observeAll(): Flow<List<Species>> = speciesDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Species? = speciesDao.getById(id)?.toDomain()

    /** True once at least one sync has populated the database. Drives the
     * "Потрібна перша синхронізація" screen (Stage 5). */
    suspend fun hasAnyData(): Boolean = speciesDao.count() > 0
}
