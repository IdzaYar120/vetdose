package com.vetdose.app.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure dose-calculation engine.
 *
 * No Android/framework dependency on purpose: this is a line-by-line port of
 * the backend's `app/services/calculator.py`, and the web app's
 * `doseCalculator.ts` is a third independent port of the same logic. All
 * three read `shared/calculation_test_cases.json` and must produce identical
 * results, so any behavioural change here must be mirrored (and re-verified)
 * in the other two implementations.
 *
 * All dose math uses [BigDecimal] — never `Float`/`Double`.
 */
enum class DoseUnit(val value: String) {
    MG_PER_KG("mg_per_kg"),
    MCG_PER_KG("mcg_per_kg"),
    IU_PER_KG("iu_per_kg"),
    ML_PER_KG("ml_per_kg"),
    ML_PER_10KG("ml_per_10kg"),
    MG_PER_ANIMAL("mg_per_animal"),
    ML_PER_ANIMAL("ml_per_animal"),
    ;

    companion object {
        fun fromValue(value: String): DoseUnit = entries.first { it.value == value }
    }
}

enum class ConcentrationUnit(val value: String) {
    MG_PER_ML("mg_per_ml"),
    MCG_PER_ML("mcg_per_ml"),
    IU_PER_ML("iu_per_ml"),
    MG_PER_TABLET("mg_per_tablet"),
    MG_PER_G("mg_per_g"),
    ;

    companion object {
        fun fromValue(value: String): ConcentrationUnit = entries.first { it.value == value }
    }
}

enum class MaxTotalDoseUnit(val value: String) {
    MG("mg"),
    MCG("mcg"),
    IU("iu"),
    ML("ml"),
    ;

    companion object {
        fun fromValue(value: String): MaxTotalDoseUnit = entries.first { it.value == value }
    }
}

enum class Severity(val value: String) {
    ABSOLUTE("absolute"),
    CAUTION("caution"),
    ;

    companion object {
        fun fromValue(value: String): Severity = entries.first { it.value == value }
    }
}

private val PRIMARY_BASIS: Map<DoseUnit, String> = mapOf(
    DoseUnit.MG_PER_KG to "mg",
    DoseUnit.MG_PER_ANIMAL to "mg",
    DoseUnit.MCG_PER_KG to "mcg",
    DoseUnit.IU_PER_KG to "iu",
    DoseUnit.ML_PER_KG to "ml",
    DoseUnit.ML_PER_10KG to "ml",
    DoseUnit.ML_PER_ANIMAL to "ml",
)

private val CONCENTRATION_BASIS: Map<ConcentrationUnit, String> = mapOf(
    ConcentrationUnit.MG_PER_ML to "mg",
    ConcentrationUnit.MCG_PER_ML to "mcg",
    ConcentrationUnit.IU_PER_ML to "iu",
    ConcentrationUnit.MG_PER_TABLET to "mg",
    ConcentrationUnit.MG_PER_G to "mg",
)

private val DOSE_RATE_LABELS: Map<DoseUnit, String> = mapOf(
    DoseUnit.MG_PER_KG to "мг/кг",
    DoseUnit.MCG_PER_KG to "мкг/кг",
    DoseUnit.IU_PER_KG to "МО/кг",
    DoseUnit.ML_PER_KG to "мл/кг",
    DoseUnit.ML_PER_10KG to "мл/10кг",
    DoseUnit.MG_PER_ANIMAL to "мг/тварину",
    DoseUnit.ML_PER_ANIMAL to "мл/тварину",
)

private val BASIS_LABELS: Map<String, String> = mapOf("mg" to "мг", "mcg" to "мкг", "iu" to "МО", "ml" to "мл")

private val CONCENTRATION_LABELS: Map<ConcentrationUnit, String> = mapOf(
    ConcentrationUnit.MG_PER_ML to "мг/мл",
    ConcentrationUnit.MCG_PER_ML to "мкг/мл",
    ConcentrationUnit.IU_PER_ML to "МО/мл",
    ConcentrationUnit.MG_PER_TABLET to "мг/таблетку",
    ConcentrationUnit.MG_PER_G to "мг/г",
)

private val ADMINISTRATION_UNIT_LABELS: Map<String, String> =
    mapOf("ml" to "мл", "tablets" to "таблеток", "g" to "г")

private val MIN_WEIGHT_KG: BigDecimal = BigDecimal.ZERO
private val MAX_WEIGHT_KG: BigDecimal = BigDecimal(1500)

/**
 * Raised for invalid input or an unsatisfiable unit combination. [code] is
 * the machine-readable error code from the VetDose protocol (e.g.
 * `INCOMPATIBLE_UNITS`); it is what shared test cases assert on.
 */
class CalculatorError(val code: String, message: String) : Exception(message)

data class SpeciesInput(
    val isFoodProducing: Boolean,
    val typicalMinWeightKg: BigDecimal,
    val typicalMaxWeightKg: BigDecimal,
)

data class DoseRuleInput(
    val doseMin: BigDecimal,
    val doseMax: BigDecimal,
    val doseUnit: DoseUnit,
    val isVerified: Boolean = false,
    val maxTotalDose: BigDecimal? = null,
    val maxTotalDoseUnit: MaxTotalDoseUnit? = null,
)

data class ProductInput(
    val concentrationValue: BigDecimal? = null,
    val concentrationUnit: ConcentrationUnit? = null,
    val tabletDivisibleBy: Int? = null,
)

data class ContraindicationInput(
    val severity: Severity,
    val messageUk: String,
)

data class CalculationInput(
    val weightKg: BigDecimal,
    val species: SpeciesInput,
    val doseRule: DoseRuleInput,
    val product: ProductInput,
    val contraindications: List<ContraindicationInput> = emptyList(),
)

data class CalculationWarning(
    val code: String,
    val messageUk: String,
)

data class CalculationResult(
    val doseMin: BigDecimal?,
    val doseMax: BigDecimal?,
    val doseAmountUnit: String?,
    val administrationMin: BigDecimal,
    val administrationMax: BigDecimal,
    val administrationUnit: String,
    val maxDoseCapped: Boolean,
    val warnings: List<CalculationWarning> = emptyList(),
    val explanation: List<String> = emptyList(),
)

/** Formats a BigDecimal the way it is shown to a vet: trimmed, comma decimal. */
fun formatNumber(value: BigDecimal): String {
    val quantized = value.setScale(4, RoundingMode.HALF_UP)
    var text = quantized.toPlainString()
    if (text.contains('.')) {
        text = text.trimEnd('0').trimEnd('.')
    }
    return text.replace('.', ',')
}

private fun fmtRange(minVal: BigDecimal, maxVal: BigDecimal, unitLabel: String): String {
    return if (minVal.compareTo(maxVal) == 0) {
        "${formatNumber(minVal)} $unitLabel"
    } else {
        "${formatNumber(minVal)}–${formatNumber(maxVal)} $unitLabel"
    }
}

private fun quantize(value: BigDecimal, places: Int): BigDecimal = value.setScale(places, RoundingMode.HALF_UP)

fun roundVolumeForDisplay(value: BigDecimal): BigDecimal {
    return when {
        value < BigDecimal.ONE -> value.setScale(2, RoundingMode.HALF_UP)
        value <= BigDecimal.TEN -> value.setScale(1, RoundingMode.HALF_UP)
        else -> {
            val step = BigDecimal("0.5")
            val units = value.divide(step, 0, RoundingMode.HALF_UP)
            units.multiply(step)
        }
    }
}

private fun floorToStep(value: BigDecimal, step: BigDecimal): BigDecimal {
    val units = value.divide(step, 0, RoundingMode.DOWN)
    return units.multiply(step)
}

private fun ceilToStep(value: BigDecimal, step: BigDecimal): BigDecimal {
    val units = value.divide(step, 0, RoundingMode.UP)
    return units.multiply(step)
}

private fun convertible(fromBasis: String, toBasis: String): Boolean {
    if (fromBasis == toBasis) return true
    val massBasis = setOf("mg", "mcg")
    return fromBasis in massBasis && toBasis in massBasis
}

private fun conversionFactor(fromBasis: String, toBasis: String): BigDecimal {
    if (fromBasis == toBasis) return BigDecimal.ONE
    if (fromBasis == "mg" && toBasis == "mcg") return BigDecimal(1000)
    if (fromBasis == "mcg" && toBasis == "mg") return BigDecimal("0.001")
    throw CalculatorError(
        "INCOMPATIBLE_UNITS",
        "Неможливо конвертувати одиниці «$fromBasis» у «$toBasis».",
    )
}

/** Division with enough working precision (28 significant digits, matching
 * Python's default Decimal context) before any explicit rounding is applied. */
private fun divide(a: BigDecimal, b: BigDecimal): BigDecimal =
    a.divide(b, java.math.MathContext(28))

fun calculateDose(data: CalculationInput): CalculationResult {
    val weightKg = data.weightKg
    val species = data.species
    val doseRule = data.doseRule
    val product = data.product

    if (!(weightKg > MIN_WEIGHT_KG && weightKg <= MAX_WEIGHT_KG)) {
        throw CalculatorError(
            "INVALID_WEIGHT",
            "Вага має бути більшою за 0 і не більшою за $MAX_WEIGHT_KG кг " +
                "(отримано ${formatNumber(weightKg)} кг).",
        )
    }

    if (doseRule.doseMin > doseRule.doseMax) {
        throw CalculatorError(
            "INVALID_DOSE_RANGE",
            "Мінімальна доза правила не може перевищувати максимальну.",
        )
    }

    val doseUnit = doseRule.doseUnit
    val primaryBasis = PRIMARY_BASIS.getValue(doseUnit)

    val explanation = mutableListOf<String>()

    // Step 1: raw primary quantity (substance mass/IU dose, or volume for
    // volume-based dose units) before any capping.
    var doseMinRaw: BigDecimal
    var doseMaxRaw: BigDecimal
    when (doseUnit) {
        DoseUnit.MG_PER_ANIMAL, DoseUnit.ML_PER_ANIMAL -> {
            doseMinRaw = doseRule.doseMin
            doseMaxRaw = doseRule.doseMax
            explanation.add(
                "Фіксована доза на тварину: " +
                    fmtRange(doseMinRaw, doseMaxRaw, DOSE_RATE_LABELS.getValue(doseUnit)),
            )
        }
        DoseUnit.ML_PER_10KG -> {
            doseMinRaw = divide(weightKg * doseRule.doseMin, BigDecimal(10))
            doseMaxRaw = divide(weightKg * doseRule.doseMax, BigDecimal(10))
            explanation.add(
                "${formatNumber(weightKg)} кг ÷ 10 × " +
                    fmtRange(doseRule.doseMin, doseRule.doseMax, DOSE_RATE_LABELS.getValue(doseUnit)) +
                    " = ${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS.getValue(primaryBasis))}",
            )
        }
        else -> {
            doseMinRaw = weightKg * doseRule.doseMin
            doseMaxRaw = weightKg * doseRule.doseMax
            explanation.add(
                "${formatNumber(weightKg)} кг × " +
                    fmtRange(doseRule.doseMin, doseRule.doseMax, DOSE_RATE_LABELS.getValue(doseUnit)) +
                    " = ${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS.getValue(primaryBasis))}",
            )
        }
    }

    // Step 2: cap at max_total_dose, if any. Applies uniformly to the primary
    // quantity, whatever its basis (mass, IU or volume).
    var maxDoseCapped = false
    if (doseRule.maxTotalDose != null) {
        val maxTotalDoseUnit = doseRule.maxTotalDoseUnit
            ?: throw CalculatorError(
                "MISSING_MAX_DOSE_UNIT",
                "Вказано максимальну дозу без одиниці вимірювання.",
            )
        val maxBasis = maxTotalDoseUnit.value
        if (!convertible(primaryBasis, maxBasis)) {
            throw CalculatorError(
                "INCOMPATIBLE_UNITS",
                "Одиниця максимальної дози «$maxBasis» несумісна з одиницею дози «$primaryBasis».",
            )
        }
        val factor = conversionFactor(maxBasis, primaryBasis)
        val maxInPrimaryBasis = doseRule.maxTotalDose * factor
        if (doseMaxRaw > maxInPrimaryBasis) {
            doseMaxRaw = maxInPrimaryBasis
            maxDoseCapped = true
        }
        if (doseMinRaw > maxInPrimaryBasis) {
            doseMinRaw = maxInPrimaryBasis
            maxDoseCapped = true
        }
        if (maxDoseCapped) {
            explanation.add(
                "Доза обмежена максимумом ${formatNumber(maxInPrimaryBasis)} " +
                    BASIS_LABELS.getValue(primaryBasis),
            )
        }
    }

    // Step 3: administration amount (what the vet actually draws up / gives).
    var tabletRangeInvalid = false
    val doseMinOut: BigDecimal?
    val doseMaxOut: BigDecimal?
    val doseAmountUnitOut: String?
    val administrationMin: BigDecimal
    val administrationMax: BigDecimal
    val administrationUnit: String

    if (primaryBasis == "mg" || primaryBasis == "mcg" || primaryBasis == "iu") {
        val concentrationValue = product.concentrationValue
        val concentrationUnit = product.concentrationUnit
        if (concentrationValue == null || concentrationUnit == null) {
            throw CalculatorError(
                "MISSING_CONCENTRATION",
                "Для розрахунку об'єму або кількості таблеток потрібна концентрація препарату.",
            )
        }
        if (concentrationValue <= BigDecimal.ZERO) {
            throw CalculatorError("INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль.")
        }
        val concentrationBasis = CONCENTRATION_BASIS.getValue(concentrationUnit)
        if (!convertible(primaryBasis, concentrationBasis)) {
            throw CalculatorError(
                "INCOMPATIBLE_UNITS",
                "Одиниця дози «$primaryBasis» несумісна з концентрацією препарату «$concentrationBasis».",
            )
        }
        val factor = conversionFactor(primaryBasis, concentrationBasis)
        val doseMinConcBasis = doseMinRaw * factor
        val doseMaxConcBasis = doseMaxRaw * factor

        if (concentrationUnit == ConcentrationUnit.MG_PER_TABLET) {
            val tabletDivisibleBy = product.tabletDivisibleBy
                ?: throw CalculatorError("MISSING_TABLET_DIVISOR", "Не вказано крок ділення таблетки.")
            val step = divide(BigDecimal.ONE, BigDecimal(tabletDivisibleBy))
            val rawMin = divide(doseMinConcBasis, concentrationValue)
            val rawMax = divide(doseMaxConcBasis, concentrationValue)
            administrationMin = ceilToStep(rawMin, step)
            administrationMax = floorToStep(rawMax, step)
            administrationUnit = "tablets"
            explanation.add(
                fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS.getValue(primaryBasis)) + " ÷ " +
                    "${formatNumber(concentrationValue)} " +
                    "${CONCENTRATION_LABELS.getValue(concentrationUnit)} = " +
                    "${fmtRange(administrationMin, administrationMax, "таблеток")} " +
                    "(округлено до кроку ${formatNumber(step)})",
            )
            if (administrationMin > administrationMax) {
                tabletRangeInvalid = true
            }
        } else {
            val rawMin = divide(doseMinConcBasis, concentrationValue)
            val rawMax = divide(doseMaxConcBasis, concentrationValue)
            administrationUnit = if (concentrationUnit == ConcentrationUnit.MG_PER_G) "g" else "ml"
            if (administrationUnit == "ml") {
                administrationMin = roundVolumeForDisplay(rawMin)
                administrationMax = roundVolumeForDisplay(rawMax)
            } else {
                administrationMin = quantize(rawMin, 2)
                administrationMax = quantize(rawMax, 2)
            }
            explanation.add(
                fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS.getValue(primaryBasis)) + " ÷ " +
                    "${formatNumber(concentrationValue)} " +
                    "${CONCENTRATION_LABELS.getValue(concentrationUnit)} = " +
                    fmtRange(rawMin, rawMax, ADMINISTRATION_UNIT_LABELS.getValue(administrationUnit)),
            )
        }

        doseMinOut = quantize(doseMinRaw, 4)
        doseMaxOut = quantize(doseMaxRaw, 4)
        doseAmountUnitOut = primaryBasis
    } else {
        administrationUnit = "ml"
        administrationMin = roundVolumeForDisplay(doseMinRaw)
        administrationMax = roundVolumeForDisplay(doseMaxRaw)
        var auxDoseMinOut: BigDecimal? = null
        var auxDoseMaxOut: BigDecimal? = null
        var auxDoseAmountUnitOut: String? = null

        val concentrationValue = product.concentrationValue
        val concentrationUnit = product.concentrationUnit
        if (concentrationValue != null &&
            (concentrationUnit == ConcentrationUnit.MG_PER_ML || concentrationUnit == ConcentrationUnit.MCG_PER_ML)
        ) {
            if (concentrationValue <= BigDecimal.ZERO) {
                throw CalculatorError("INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль.")
            }
            val auxMin = doseMinRaw * concentrationValue
            val auxMax = doseMaxRaw * concentrationValue
            auxDoseMinOut = quantize(auxMin, 4)
            auxDoseMaxOut = quantize(auxMax, 4)
            auxDoseAmountUnitOut = CONCENTRATION_BASIS.getValue(concentrationUnit)
            explanation.add(
                fmtRange(doseMinRaw, doseMaxRaw, "мл") + " × " +
                    "${formatNumber(concentrationValue)} " +
                    "${CONCENTRATION_LABELS.getValue(concentrationUnit)} = " +
                    fmtRange(auxDoseMinOut, auxDoseMaxOut, BASIS_LABELS.getValue(auxDoseAmountUnitOut)),
            )
        }
        doseMinOut = auxDoseMinOut
        doseMaxOut = auxDoseMaxOut
        doseAmountUnitOut = auxDoseAmountUnitOut
    }

    // Step 4: warnings, in a fixed, deterministic order (mirrored in the other
    // two implementations so shared test cases compare warning_codes as lists).
    val warnings = mutableListOf<CalculationWarning>()
    if (!(weightKg >= species.typicalMinWeightKg && weightKg <= species.typicalMaxWeightKg)) {
        warnings.add(
            CalculationWarning(
                "WEIGHT_OUT_OF_TYPICAL_RANGE",
                "Вага ${formatNumber(weightKg)} кг виходить за типовий діапазон виду " +
                    "(${formatNumber(species.typicalMinWeightKg)}–" +
                    "${formatNumber(species.typicalMaxWeightKg)} кг). Підтвердіть значення.",
            ),
        )
    }
    for (contraindication in data.contraindications) {
        val code = if (contraindication.severity == Severity.ABSOLUTE) "ABSOLUTE_CONTRAINDICATION" else "CAUTION"
        warnings.add(CalculationWarning(code, contraindication.messageUk))
    }
    if (!doseRule.isVerified) {
        warnings.add(
            CalculationWarning(
                "RULE_NOT_VERIFIED",
                "Це правило дозування ще не підтверджене лікарем. Застосовуйте з обережністю.",
            ),
        )
    }
    if (species.isFoodProducing) {
        warnings.add(
            CalculationWarning(
                "FOOD_PRODUCING_ANIMAL",
                "Продуктивна тварина: перед використанням продукції перевірте терміни виведення.",
            ),
        )
    }
    if (maxDoseCapped) {
        warnings.add(
            CalculationWarning(
                "MAX_DOSE_CAPPED",
                "Розрахована доза перевищувала максимально допустиму і була обмежена цим значенням.",
            ),
        )
    }
    if (tabletRangeInvalid) {
        warnings.add(
            CalculationWarning(
                "TABLET_CANNOT_MATCH_RANGE",
                "Після округлення до кроку ділення таблетки мінімальна кількість перевищує " +
                    "максимальну. Перевірте дозу вручну.",
            ),
        )
    }

    return CalculationResult(
        doseMin = doseMinOut,
        doseMax = doseMaxOut,
        doseAmountUnit = doseAmountUnitOut,
        administrationMin = administrationMin,
        administrationMax = administrationMax,
        administrationUnit = administrationUnit,
        maxDoseCapped = maxDoseCapped,
        warnings = warnings,
        explanation = explanation,
    )
}
