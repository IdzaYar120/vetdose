package com.vetdose.app.domain.calculator

import java.io.File
import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Runs every case in shared/calculation_test_cases.json against the Kotlin
 * calculator. The same file is also read by the backend (pytest) and web
 * (Vitest) test suites so all three implementations are checked for
 * agreement. See app/build.gradle.kts for how the file path is supplied.
 */
@Serializable
data class RawSpecies(
    @SerialName("is_food_producing") val isFoodProducing: Boolean,
    @SerialName("typical_min_weight_kg") val typicalMinWeightKg: String,
    @SerialName("typical_max_weight_kg") val typicalMaxWeightKg: String,
)

@Serializable
data class RawDoseRule(
    @SerialName("dose_min") val doseMin: String,
    @SerialName("dose_max") val doseMax: String,
    @SerialName("dose_unit") val doseUnit: String,
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("max_total_dose") val maxTotalDose: String? = null,
    @SerialName("max_total_dose_unit") val maxTotalDoseUnit: String? = null,
)

@Serializable
data class RawProduct(
    @SerialName("concentration_value") val concentrationValue: String? = null,
    @SerialName("concentration_unit") val concentrationUnit: String? = null,
    @SerialName("tablet_divisible_by") val tabletDivisibleBy: Int? = null,
)

@Serializable
data class RawContraindication(
    val severity: String,
    @SerialName("message_uk") val messageUk: String,
)

@Serializable
data class RawInput(
    @SerialName("weight_kg") val weightKg: String,
    val species: RawSpecies,
    @SerialName("dose_rule") val doseRule: RawDoseRule,
    val product: RawProduct,
    val contraindications: List<RawContraindication> = emptyList(),
)

@Serializable
data class RawExpected(
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("dose_min") val doseMin: String? = null,
    @SerialName("dose_max") val doseMax: String? = null,
    @SerialName("dose_amount_unit") val doseAmountUnit: String? = null,
    @SerialName("administration_min") val administrationMin: String? = null,
    @SerialName("administration_max") val administrationMax: String? = null,
    @SerialName("administration_unit") val administrationUnit: String? = null,
    @SerialName("max_dose_capped") val maxDoseCapped: Boolean = false,
    @SerialName("warning_codes") val warningCodes: List<String> = emptyList(),
    val explanation: List<String> = emptyList(),
)

@Serializable
data class RawCase(
    val id: String,
    val description: String,
    val input: RawInput,
    val expected: RawExpected,
) {
    override fun toString(): String = id
}

private fun buildInput(raw: RawInput): CalculationInput {
    val species = SpeciesInput(
        isFoodProducing = raw.species.isFoodProducing,
        typicalMinWeightKg = BigDecimal(raw.species.typicalMinWeightKg),
        typicalMaxWeightKg = BigDecimal(raw.species.typicalMaxWeightKg),
    )
    val doseRule = DoseRuleInput(
        doseMin = BigDecimal(raw.doseRule.doseMin),
        doseMax = BigDecimal(raw.doseRule.doseMax),
        doseUnit = DoseUnit.fromValue(raw.doseRule.doseUnit),
        isVerified = raw.doseRule.isVerified,
        maxTotalDose = raw.doseRule.maxTotalDose?.let { BigDecimal(it) },
        maxTotalDoseUnit = raw.doseRule.maxTotalDoseUnit?.let { MaxTotalDoseUnit.fromValue(it) },
    )
    val product = ProductInput(
        concentrationValue = raw.product.concentrationValue?.let { BigDecimal(it) },
        concentrationUnit = raw.product.concentrationUnit?.let { ConcentrationUnit.fromValue(it) },
        tabletDivisibleBy = raw.product.tabletDivisibleBy,
    )
    val contraindications = raw.contraindications.map {
        ContraindicationInput(severity = Severity.fromValue(it.severity), messageUk = it.messageUk)
    }
    return CalculationInput(
        weightKg = BigDecimal(raw.weightKg),
        species = species,
        doseRule = doseRule,
        product = product,
        contraindications = contraindications,
    )
}

@RunWith(Parameterized::class)
class DoseCalculatorSharedCasesTest(private val case: RawCase) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val path = System.getProperty("vetdose.sharedTestCasesPath")
                ?: error(
                    "System property vetdose.sharedTestCasesPath is not set " +
                        "(configured in app/build.gradle.kts's tasks.withType<Test>)",
                )
            val json = Json { ignoreUnknownKeys = true }
            val text = File(path).readText(Charsets.UTF_8)
            val decoded = json.decodeFromString(ListSerializer(RawCase.serializer()), text)
            check(decoded.size >= 25) { "Expected at least 25 shared cases, found ${decoded.size}" }
            return decoded.map { arrayOf<Any>(it) }
        }
    }

    @Test
    fun runsSharedCase() {
        val input = buildInput(case.input)
        val expected = case.expected

        if (expected.errorCode != null) {
            val error = assertThrows(CalculatorError::class.java) { calculateDose(input) }
            assertEquals(expected.errorCode, error.code)
            return
        }

        val result = calculateDose(input)

        assertEquals(expected.doseMin, result.doseMin?.toString())
        assertEquals(expected.doseMax, result.doseMax?.toString())
        assertEquals(expected.doseAmountUnit, result.doseAmountUnit)
        assertEquals(expected.administrationMin, result.administrationMin.toString())
        assertEquals(expected.administrationMax, result.administrationMax.toString())
        assertEquals(expected.administrationUnit, result.administrationUnit)
        assertEquals(expected.maxDoseCapped, result.maxDoseCapped)
        assertEquals(expected.warningCodes, result.warnings.map { it.code })
        assertEquals(expected.explanation, result.explanation)
    }
}
