package com.vetdose.app.data.mapper

import com.vetdose.app.data.remote.dto.SpeciesDto
import com.vetdose.app.data.remote.dto.DoseRuleDto
import com.vetdose.app.data.remote.dto.ProductDto
import com.vetdose.app.domain.calculator.ConcentrationUnit
import com.vetdose.app.domain.calculator.DoseUnit
import com.vetdose.app.domain.model.ProductForm
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `species dto maps to entity and then to domain with correct types`() {
        val dto = SpeciesDto(
            id = "sp-1",
            code = "cat",
            nameUk = "Кіт/кішка",
            isFoodProducing = false,
            typicalMinWeightKg = "2.5",
            typicalMaxWeightKg = "8",
            isDeleted = false,
        )

        val entity = dto.toEntity()
        val domain = entity.toDomain()

        assertEquals("sp-1", domain.id)
        assertEquals("cat", domain.code)
        assertEquals(BigDecimal("2.5"), domain.typicalMinWeightKg)
        assertEquals(BigDecimal("8"), domain.typicalMaxWeightKg)
    }

    @Test
    fun `product dto maps form and concentration unit enums correctly`() {
        val dto = ProductDto(
            id = "prod-1",
            tradeName = "TEST_Antibiotic",
            manufacturer = "TEST Labs",
            substanceId = "sub-1",
            form = "tablet",
            concentrationValue = "250",
            concentrationUnit = "mg_per_tablet",
            tabletDivisibleBy = 2,
            isDeleted = false,
        )

        val domain = dto.toEntity().toDomain()

        assertEquals(ProductForm.TABLET, domain.form)
        assertEquals(ConcentrationUnit.MG_PER_TABLET, domain.concentrationUnit)
        assertEquals(BigDecimal("250"), domain.concentrationValue)
        assertEquals(2, domain.tabletDivisibleBy)
    }

    @Test
    fun `dose rule dto with null optional fields maps cleanly`() {
        val dto = DoseRuleDto(
            id = "rule-1",
            substanceId = "sub-1",
            speciesId = "sp-1",
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
            isDeleted = false,
        )

        val domain = dto.toEntity().toDomain()

        assertEquals(DoseUnit.MG_PER_KG, domain.doseUnit)
        assertEquals(null, domain.maxTotalDose)
        assertEquals(null, domain.maxTotalDoseUnit)
        assertEquals(BigDecimal("5"), domain.doseMin)
    }

    @Test
    fun `dose rule dto with max total dose maps the decimal and unit`() {
        val dto = DoseRuleDto(
            id = "rule-2",
            substanceId = "sub-1",
            speciesId = "sp-1",
            route = "po",
            doseMin = "5",
            doseMax = "10",
            doseUnit = "mg_per_kg",
            maxTotalDose = "50",
            maxTotalDoseUnit = "mg",
            source = "TEST DATA — NOT FOR CLINICAL USE",
            isVerified = true,
            isDeleted = false,
        )

        val domain = dto.toEntity().toDomain()

        assertEquals(BigDecimal("50"), domain.maxTotalDose)
        assertEquals(com.vetdose.app.domain.calculator.MaxTotalDoseUnit.MG, domain.maxTotalDoseUnit)
    }
}
