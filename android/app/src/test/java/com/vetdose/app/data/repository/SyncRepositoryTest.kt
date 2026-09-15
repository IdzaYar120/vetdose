package com.vetdose.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vetdose.app.data.local.VetDoseDatabase
import com.vetdose.app.data.remote.VetDoseApi
import com.vetdose.app.data.remote.dto.SpeciesDto
import com.vetdose.app.data.remote.dto.SyncResponseDto
import com.vetdose.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private class FakeVetDoseApi(private val responses: MutableList<SyncResponseDto>) : VetDoseApi {
    val requestedSinceValues = mutableListOf<String?>()

    override suspend fun sync(since: String?): SyncResponseDto {
        requestedSinceValues.add(since)
        return responses.removeAt(0)
    }
}

private fun emptyResponse(serverTime: String) = SyncResponseDto(
    serverTime = serverTime,
    species = emptyList(),
    substances = emptyList(),
    products = emptyList(),
    doseRules = emptyList(),
    contraindications = emptyList(),
    withdrawalPeriods = emptyList(),
)

private fun species(id: String, code: String, deleted: Boolean = false) = SpeciesDto(
    id = id,
    code = code,
    nameUk = "TEST_$code",
    isFoodProducing = false,
    typicalMinWeightKg = "1",
    typicalMaxWeightKg = "10",
    isDeleted = deleted,
)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncRepositoryTest {

    private lateinit var database: VetDoseDatabase
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, VetDoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepository = SettingsRepository(context)
    }

    private fun repositoryWithResponses(vararg responses: SyncResponseDto): Pair<SyncRepository, FakeVetDoseApi> {
        val api = FakeVetDoseApi(responses.toMutableList())
        return SyncRepository(api, database, settingsRepository) to api
    }

    @Test
    fun `sync upserts active rows into Room`() = runBlocking {
        val response = emptyResponse("2026-01-01T00:00:00Z").copy(
            species = listOf(species("sp-1", "cat"), species("sp-2", "dog")),
        )
        val (repository, _) = repositoryWithResponses(response)

        repository.fullSync()

        val stored = database.speciesDao().observeAll().first()
        assertEquals(2, stored.size)
    }

    @Test
    fun `sync deletes rows the server reports as deleted instead of storing them`() = runBlocking {
        val (repository, _) = repositoryWithResponses(
            emptyResponse("2026-01-01T00:00:00Z").copy(species = listOf(species("sp-1", "cat"))),
        )
        repository.fullSync()
        assertEquals(1, database.speciesDao().count())

        val (repository2, _) = repositoryWithResponses(
            emptyResponse("2026-01-02T00:00:00Z").copy(species = listOf(species("sp-1", "cat", deleted = true))),
        )
        repository2.fullSync()

        assertEquals(0, database.speciesDao().count())
        assertNull(database.speciesDao().getById("sp-1"))
    }

    @Test
    fun `sync stores server_time as the cursor for the next incremental sync`() = runBlocking {
        val (repository, _) = repositoryWithResponses(emptyResponse("2026-03-15T12:00:00Z"))

        repository.fullSync()

        assertEquals("2026-03-15T12:00:00Z", settingsRepository.lastSyncTimeFlow.first())
    }

    @Test
    fun `incremental sync sends the stored cursor as since`() = runBlocking {
        settingsRepository.setLastSyncTime("2026-01-01T00:00:00Z")
        val api = FakeVetDoseApi(mutableListOf(emptyResponse("2026-01-02T00:00:00Z")))
        val repository = SyncRepository(api, database, settingsRepository)

        repository.sync()

        assertEquals(listOf("2026-01-01T00:00:00Z"), api.requestedSinceValues)
    }

    @Test
    fun `full sync always requests since=null even if a cursor is stored`() = runBlocking {
        settingsRepository.setLastSyncTime("2026-01-01T00:00:00Z")
        val api = FakeVetDoseApi(mutableListOf(emptyResponse("2026-01-02T00:00:00Z")))
        val repository = SyncRepository(api, database, settingsRepository)

        repository.fullSync()

        assertEquals(listOf(null), api.requestedSinceValues)
    }
}
