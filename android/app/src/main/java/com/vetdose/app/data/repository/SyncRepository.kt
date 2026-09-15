package com.vetdose.app.data.repository

import androidx.room.withTransaction
import com.vetdose.app.data.local.VetDoseDatabase
import com.vetdose.app.data.mapper.toEntity
import com.vetdose.app.data.remote.VetDoseApi
import com.vetdose.app.data.remote.dto.SyncResponseDto
import com.vetdose.app.data.remote.dto.SyncableDto
import com.vetdose.app.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Pulls `/api/v1/sync` and applies it to Room in one transaction: active rows
 * are upserted, rows the server reports as deleted are removed outright (see
 * [com.vetdose.app.data.local.entity.SpeciesEntity] for why we don't keep
 * local tombstones). Room is the single source of truth the rest of the app
 * reads from — nothing here is read back out again.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val api: VetDoseApi,
    private val database: VetDoseDatabase,
    private val settingsRepository: SettingsRepository,
) {
    /** Incremental sync using the last successful sync's `server_time` as `since`. */
    suspend fun sync() {
        val since = settingsRepository.lastSyncTimeFlow.first()
        runSync(since)
    }

    /** Ignores any stored cursor and re-pulls everything — used for the very
     * first sync, and as a recovery path if local storage was ever cleared. */
    suspend fun fullSync() {
        runSync(since = null)
    }

    private suspend fun runSync(since: String?) {
        val response = api.sync(since)
        applyToDatabase(response)
        settingsRepository.setLastSyncTime(response.serverTime)
    }

    private suspend fun applyToDatabase(response: SyncResponseDto) {
        database.withTransaction {
            applyEntitySync(response.species, { it.toEntity() }, database.speciesDao()::upsertAll, database.speciesDao()::deleteByIds)
            applyEntitySync(response.substances, { it.toEntity() }, database.substanceDao()::upsertAll, database.substanceDao()::deleteByIds)
            applyEntitySync(response.products, { it.toEntity() }, database.productDao()::upsertAll, database.productDao()::deleteByIds)
            applyEntitySync(response.doseRules, { it.toEntity() }, database.doseRuleDao()::upsertAll, database.doseRuleDao()::deleteByIds)
            applyEntitySync(
                response.contraindications,
                { it.toEntity() },
                database.contraindicationDao()::upsertAll,
                database.contraindicationDao()::deleteByIds,
            )
            applyEntitySync(
                response.withdrawalPeriods,
                { it.toEntity() },
                database.withdrawalPeriodDao()::upsertAll,
                database.withdrawalPeriodDao()::deleteByIds,
            )
        }
    }

    private suspend fun <D : SyncableDto, E> applyEntitySync(
        dtos: List<D>,
        toEntity: (D) -> E,
        upsert: suspend (List<E>) -> Unit,
        delete: suspend (List<String>) -> Unit,
    ) {
        val (deleted, active) = dtos.partition { it.isDeleted }
        if (active.isNotEmpty()) upsert(active.map(toEntity))
        if (deleted.isNotEmpty()) delete(deleted.map { it.id })
    }
}
