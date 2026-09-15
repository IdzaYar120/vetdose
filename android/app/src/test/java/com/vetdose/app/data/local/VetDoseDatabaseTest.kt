package com.vetdose.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vetdose.app.data.local.entity.DoseRuleEntity
import com.vetdose.app.data.local.entity.ProductEntity
import com.vetdose.app.data.local.entity.SpeciesEntity
import com.vetdose.app.data.local.entity.SubstanceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises Room against a real (in-memory) SQLite database via Robolectric
 * — this catches schema/query mistakes that pure mocking would miss, without
 * needing a device or emulator.
 *
 * Pinned to API 34 (rather than our compileSdk 36): Robolectric's shadow for
 * a very recently added Android 36-era internal (ApplicationSharedMemory)
 * reflects into a JDK-internal package that JDK 21 refuses across the module
 * boundary — a Robolectric/JDK21/API36 rough edge, not anything in our code.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VetDoseDatabaseTest {

    private lateinit var database: VetDoseDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VetDoseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun testSpecies(id: String = "sp-1") = SpeciesEntity(
        id = id,
        code = "cat",
        nameUk = "Кіт/кішка",
        isFoodProducing = false,
        typicalMinWeightKg = "2",
        typicalMaxWeightKg = "8",
    )

    @Test
    fun `upsert then observeAll returns the row`() = runBlocking {
        database.speciesDao().upsertAll(listOf(testSpecies()))

        val all = database.speciesDao().observeAll().first()

        assertEquals(1, all.size)
        assertEquals("cat", all.first().code)
    }

    @Test
    fun `upsert with same id replaces the row rather than duplicating it`() = runBlocking {
        database.speciesDao().upsertAll(listOf(testSpecies()))
        database.speciesDao().upsertAll(listOf(testSpecies().copy(nameUk = "Кіт (оновлено)")))

        val all = database.speciesDao().observeAll().first()

        assertEquals(1, all.size)
        assertEquals("Кіт (оновлено)", all.first().nameUk)
    }

    @Test
    fun `deleteByIds removes only the requested rows`() = runBlocking {
        database.speciesDao().upsertAll(listOf(testSpecies("sp-1"), testSpecies("sp-2")))

        database.speciesDao().deleteByIds(listOf("sp-1"))

        val all = database.speciesDao().observeAll().first()
        assertEquals(1, all.size)
        assertEquals("sp-2", all.first().id)
        assertNull(database.speciesDao().getById("sp-1"))
    }

    @Test
    fun `count reflects the number of stored rows`() = runBlocking {
        assertEquals(0, database.speciesDao().count())

        database.speciesDao().upsertAll(listOf(testSpecies()))

        assertEquals(1, database.speciesDao().count())
    }

    @Test
    fun `observeBySpecies joins product through dose_rule by substance`() = runBlocking {
        database.substanceDao().upsertAll(
            listOf(SubstanceEntity(id = "sub-1", name = "TEST_Sub", nameUk = "TEST_Реч", pharmacologicalGroup = null, notes = null)),
        )
        database.speciesDao().upsertAll(listOf(testSpecies("sp-dog")))
        database.productDao().upsertAll(
            listOf(
                ProductEntity(
                    id = "prod-1",
                    tradeName = "TEST_Product",
                    manufacturer = null,
                    substanceId = "sub-1",
                    form = "injection_solution",
                    concentrationValue = "100",
                    concentrationUnit = "mg_per_ml",
                    tabletDivisibleBy = null,
                ),
            ),
        )
        database.doseRuleDao().upsertAll(
            listOf(
                DoseRuleEntity(
                    id = "rule-1",
                    substanceId = "sub-1",
                    speciesId = "sp-dog",
                    route = "po",
                    indication = null,
                    doseMin = "5",
                    doseMax = "10",
                    doseUnit = "mg_per_kg",
                    maxTotalDose = null,
                    maxTotalDoseUnit = null,
                    frequency = null,
                    duration = null,
                    notes = null,
                    source = "TEST DATA — NOT FOR CLINICAL USE",
                    isVerified = false,
                ),
            ),
        )

        val forDog = database.productDao().observeBySpecies("sp-dog").first()
        val forCat = database.productDao().observeBySpecies("sp-cat").first()

        assertEquals(1, forDog.size)
        assertEquals("prod-1", forDog.first().id)
        assertTrue(forCat.isEmpty())
    }
}
