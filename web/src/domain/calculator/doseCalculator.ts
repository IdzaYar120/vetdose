/**
 * Pure dose-calculation engine.
 *
 * No framework dependency on purpose: this is a third independent port of the
 * backend's `app/services/calculator.py` (the Android port is
 * `DoseCalculator.kt`). All three read `shared/calculation_test_cases.json`
 * and must produce identical results, so any behavioural change here must be
 * mirrored (and re-verified) in the other two implementations.
 *
 * All dose math uses `decimal.js` — never native `number`.
 *
 * Unlike Python's `Decimal` or Java's `BigDecimal`, `decimal.js` values do not
 * carry a fixed "scale" (trailing zeros are not preserved once a value is
 * produced by rounding or multiplication). Wherever the original algorithm
 * relies on that scale to decide how many decimal places a result should be
 * *displayed* with (`administration_min`/`administration_max`,
 * `dose_min`/`dose_max`), this port carries the intended decimal-place count
 * alongside the value explicitly as a {@link ScaledDecimal}, and callers must
 * use {@link scaledToFixed} (not `.toString()`/`.toFixed()`) to render it.
 */

import DecimalJs from "decimal.js"

/** Dedicated Decimal constructor: 28 significant digits (matches Python's
 * default context and the `MathContext(28)` used by the Kotlin port),
 * HALF_UP rounding, and plain (never exponential) notation for the magnitudes
 * this domain deals with. */
export const Decimal: DecimalJs.Constructor = DecimalJs.clone({
  precision: 28,
  rounding: DecimalJs.ROUND_HALF_UP,
  toExpPos: 40,
  toExpNeg: -40,
})
export type Decimal = DecimalJs

export const DoseUnit = {
  MG_PER_KG: "mg_per_kg",
  MCG_PER_KG: "mcg_per_kg",
  IU_PER_KG: "iu_per_kg",
  ML_PER_KG: "ml_per_kg",
  ML_PER_10KG: "ml_per_10kg",
  MG_PER_ANIMAL: "mg_per_animal",
  ML_PER_ANIMAL: "ml_per_animal",
} as const
export type DoseUnit = (typeof DoseUnit)[keyof typeof DoseUnit]

export const ConcentrationUnit = {
  MG_PER_ML: "mg_per_ml",
  MCG_PER_ML: "mcg_per_ml",
  IU_PER_ML: "iu_per_ml",
  MG_PER_TABLET: "mg_per_tablet",
  MG_PER_G: "mg_per_g",
} as const
export type ConcentrationUnit = (typeof ConcentrationUnit)[keyof typeof ConcentrationUnit]

export const MaxTotalDoseUnit = {
  MG: "mg",
  MCG: "mcg",
  IU: "iu",
  ML: "ml",
} as const
export type MaxTotalDoseUnit = (typeof MaxTotalDoseUnit)[keyof typeof MaxTotalDoseUnit]

export const Severity = {
  ABSOLUTE: "absolute",
  CAUTION: "caution",
} as const
export type Severity = (typeof Severity)[keyof typeof Severity]

type Basis = "mg" | "mcg" | "iu" | "ml"

const PRIMARY_BASIS: Record<DoseUnit, Basis> = {
  [DoseUnit.MG_PER_KG]: "mg",
  [DoseUnit.MG_PER_ANIMAL]: "mg",
  [DoseUnit.MCG_PER_KG]: "mcg",
  [DoseUnit.IU_PER_KG]: "iu",
  [DoseUnit.ML_PER_KG]: "ml",
  [DoseUnit.ML_PER_10KG]: "ml",
  [DoseUnit.ML_PER_ANIMAL]: "ml",
}

const CONCENTRATION_BASIS: Record<ConcentrationUnit, Basis> = {
  [ConcentrationUnit.MG_PER_ML]: "mg",
  [ConcentrationUnit.MCG_PER_ML]: "mcg",
  [ConcentrationUnit.IU_PER_ML]: "iu",
  [ConcentrationUnit.MG_PER_TABLET]: "mg",
  [ConcentrationUnit.MG_PER_G]: "mg",
}

const DOSE_RATE_LABELS: Record<DoseUnit, string> = {
  [DoseUnit.MG_PER_KG]: "мг/кг",
  [DoseUnit.MCG_PER_KG]: "мкг/кг",
  [DoseUnit.IU_PER_KG]: "МО/кг",
  [DoseUnit.ML_PER_KG]: "мл/кг",
  [DoseUnit.ML_PER_10KG]: "мл/10кг",
  [DoseUnit.MG_PER_ANIMAL]: "мг/тварину",
  [DoseUnit.ML_PER_ANIMAL]: "мл/тварину",
}

const BASIS_LABELS: Record<Basis, string> = { mg: "мг", mcg: "мкг", iu: "МО", ml: "мл" }

const CONCENTRATION_LABELS: Record<ConcentrationUnit, string> = {
  [ConcentrationUnit.MG_PER_ML]: "мг/мл",
  [ConcentrationUnit.MCG_PER_ML]: "мкг/мл",
  [ConcentrationUnit.IU_PER_ML]: "МО/мл",
  [ConcentrationUnit.MG_PER_TABLET]: "мг/таблетку",
  [ConcentrationUnit.MG_PER_G]: "мг/г",
}

export type AdministrationUnit = "ml" | "g" | "tablets"

const ADMINISTRATION_UNIT_LABELS: Record<AdministrationUnit, string> = {
  ml: "мл",
  tablets: "таблеток",
  g: "г",
}

const MIN_WEIGHT_KG = new Decimal(0)
const MAX_WEIGHT_KG = new Decimal(1500)

/** Raised for invalid input or an unsatisfiable unit combination. `code` is
 * the machine-readable error code from the VetDose protocol (e.g.
 * `INCOMPATIBLE_UNITS`); it is what shared test cases assert on. */
export class CalculatorError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = "CalculatorError"
    this.code = code
  }
}

export interface SpeciesInput {
  isFoodProducing: boolean
  typicalMinWeightKg: Decimal
  typicalMaxWeightKg: Decimal
}

export interface DoseRuleInput {
  doseMin: Decimal
  doseMax: Decimal
  doseUnit: DoseUnit
  isVerified: boolean
  maxTotalDose: Decimal | null
  maxTotalDoseUnit: MaxTotalDoseUnit | null
}

export interface ProductInput {
  concentrationValue: Decimal | null
  concentrationUnit: ConcentrationUnit | null
  tabletDivisibleBy: number | null
}

export interface ContraindicationInput {
  severity: Severity
  messageUk: string
}

export interface CalculationInput {
  weightKg: Decimal
  species: SpeciesInput
  doseRule: DoseRuleInput
  product: ProductInput
  contraindications: readonly ContraindicationInput[]
}

export interface CalculationWarning {
  code: string
  messageUk: string
}

/** A Decimal paired with the number of decimal places it must be displayed
 * with — see the module doc comment for why this exists. */
export interface ScaledDecimal {
  readonly value: Decimal
  readonly dp: number
}

/** Renders a {@link ScaledDecimal} preserving its intended scale, e.g.
 * `scaled(new Decimal("1"), 1)` -> `"1.0"`. Use this, never `.toString()`. */
export function scaledToFixed(s: ScaledDecimal): string {
  return s.value.toFixed(s.dp)
}

function scaled(value: Decimal, dp: number): ScaledDecimal {
  return { value, dp }
}

export interface CalculationResult {
  doseMin: ScaledDecimal | null
  doseMax: ScaledDecimal | null
  doseAmountUnit: string | null
  administrationMin: ScaledDecimal
  administrationMax: ScaledDecimal
  administrationUnit: AdministrationUnit
  maxDoseCapped: boolean
  warnings: CalculationWarning[]
  explanation: string[]
}

/** Formats a Decimal the way it is shown to a vet: trimmed, comma decimal. */
export function formatNumber(value: Decimal): string {
  return value.toDecimalPlaces(4, DecimalJs.ROUND_HALF_UP).toFixed().replace(".", ",")
}

function fmtRange(minVal: Decimal, maxVal: Decimal, unitLabel: string): string {
  if (minVal.eq(maxVal)) {
    return `${formatNumber(minVal)} ${unitLabel}`
  }
  return `${formatNumber(minVal)}–${formatNumber(maxVal)} ${unitLabel}`
}

function quantize(value: Decimal, places: number): ScaledDecimal {
  return scaled(value.toDecimalPlaces(places, DecimalJs.ROUND_HALF_UP), places)
}

export function roundVolumeForDisplay(value: Decimal): ScaledDecimal {
  if (value.lt(1)) {
    return quantize(value, 2)
  }
  if (value.lte(10)) {
    return quantize(value, 1)
  }
  const step = new Decimal("0.5")
  const units = value.dividedBy(step).toDecimalPlaces(0, DecimalJs.ROUND_HALF_UP)
  return scaled(units.times(step), 1)
}

function floorToStep(value: Decimal, step: Decimal): Decimal {
  const units = value.dividedBy(step).toDecimalPlaces(0, DecimalJs.ROUND_DOWN)
  return units.times(step)
}

function ceilToStep(value: Decimal, step: Decimal): Decimal {
  const units = value.dividedBy(step).toDecimalPlaces(0, DecimalJs.ROUND_UP)
  return units.times(step)
}

function convertible(fromBasis: string, toBasis: string): boolean {
  if (fromBasis === toBasis) return true
  const massBasis = new Set(["mg", "mcg"])
  return massBasis.has(fromBasis) && massBasis.has(toBasis)
}

function conversionFactor(fromBasis: string, toBasis: string): Decimal {
  if (fromBasis === toBasis) return new Decimal(1)
  if (fromBasis === "mg" && toBasis === "mcg") return new Decimal(1000)
  if (fromBasis === "mcg" && toBasis === "mg") return new Decimal("0.001")
  throw new CalculatorError(
    "INCOMPATIBLE_UNITS",
    `Неможливо конвертувати одиниці «${fromBasis}» у «${toBasis}».`,
  )
}

export function calculateDose(data: CalculationInput): CalculationResult {
  const weightKg = data.weightKg
  const species = data.species
  const doseRule = data.doseRule
  const product = data.product

  if (!(weightKg.gt(MIN_WEIGHT_KG) && weightKg.lte(MAX_WEIGHT_KG))) {
    throw new CalculatorError(
      "INVALID_WEIGHT",
      `Вага має бути більшою за 0 і не більшою за ${MAX_WEIGHT_KG.toFixed()} кг ` +
        `(отримано ${formatNumber(weightKg)} кг).`,
    )
  }

  if (doseRule.doseMin.gt(doseRule.doseMax)) {
    throw new CalculatorError(
      "INVALID_DOSE_RANGE",
      "Мінімальна доза правила не може перевищувати максимальну.",
    )
  }

  const doseUnit = doseRule.doseUnit
  const primaryBasis = PRIMARY_BASIS[doseUnit]

  const explanation: string[] = []

  // Step 1: raw primary quantity (substance mass/IU dose, or volume for
  // volume-based dose units) before any capping.
  let doseMinRaw: Decimal
  let doseMaxRaw: Decimal
  if (doseUnit === DoseUnit.MG_PER_ANIMAL || doseUnit === DoseUnit.ML_PER_ANIMAL) {
    doseMinRaw = doseRule.doseMin
    doseMaxRaw = doseRule.doseMax
    explanation.push(
      `Фіксована доза на тварину: ${fmtRange(doseMinRaw, doseMaxRaw, DOSE_RATE_LABELS[doseUnit])}`,
    )
  } else if (doseUnit === DoseUnit.ML_PER_10KG) {
    doseMinRaw = weightKg.times(doseRule.doseMin).dividedBy(10)
    doseMaxRaw = weightKg.times(doseRule.doseMax).dividedBy(10)
    explanation.push(
      `${formatNumber(weightKg)} кг ÷ 10 × ` +
        `${fmtRange(doseRule.doseMin, doseRule.doseMax, DOSE_RATE_LABELS[doseUnit])}` +
        ` = ${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS[primaryBasis])}`,
    )
  } else {
    doseMinRaw = weightKg.times(doseRule.doseMin)
    doseMaxRaw = weightKg.times(doseRule.doseMax)
    explanation.push(
      `${formatNumber(weightKg)} кг × ` +
        `${fmtRange(doseRule.doseMin, doseRule.doseMax, DOSE_RATE_LABELS[doseUnit])}` +
        ` = ${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS[primaryBasis])}`,
    )
  }

  // Step 2: cap at max_total_dose, if any. Applies uniformly to the primary
  // quantity, whatever its basis (mass, IU or volume).
  let maxDoseCapped = false
  if (doseRule.maxTotalDose !== null) {
    if (doseRule.maxTotalDoseUnit === null) {
      throw new CalculatorError("MISSING_MAX_DOSE_UNIT", "Вказано максимальну дозу без одиниці вимірювання.")
    }
    const maxBasis = doseRule.maxTotalDoseUnit
    if (!convertible(primaryBasis, maxBasis)) {
      throw new CalculatorError(
        "INCOMPATIBLE_UNITS",
        `Одиниця максимальної дози «${maxBasis}» несумісна з одиницею дози «${primaryBasis}».`,
      )
    }
    const factor = conversionFactor(maxBasis, primaryBasis)
    const maxInPrimaryBasis = doseRule.maxTotalDose.times(factor)
    if (doseMaxRaw.gt(maxInPrimaryBasis)) {
      doseMaxRaw = maxInPrimaryBasis
      maxDoseCapped = true
    }
    if (doseMinRaw.gt(maxInPrimaryBasis)) {
      doseMinRaw = maxInPrimaryBasis
      maxDoseCapped = true
    }
    if (maxDoseCapped) {
      explanation.push(
        `Доза обмежена максимумом ${formatNumber(maxInPrimaryBasis)} ${BASIS_LABELS[primaryBasis]}`,
      )
    }
  }

  // Step 3: administration amount (what the vet actually draws up / gives).
  let tabletRangeInvalid = false
  let doseMinOut: ScaledDecimal | null
  let doseMaxOut: ScaledDecimal | null
  let doseAmountUnitOut: string | null
  let administrationMin: ScaledDecimal
  let administrationMax: ScaledDecimal
  let administrationUnit: AdministrationUnit

  if (primaryBasis === "mg" || primaryBasis === "mcg" || primaryBasis === "iu") {
    const concentrationValue = product.concentrationValue
    const concentrationUnit = product.concentrationUnit
    if (concentrationValue === null || concentrationUnit === null) {
      throw new CalculatorError(
        "MISSING_CONCENTRATION",
        "Для розрахунку об'єму або кількості таблеток потрібна концентрація препарату.",
      )
    }
    if (concentrationValue.lte(0)) {
      throw new CalculatorError("INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль.")
    }
    const concentrationBasis = CONCENTRATION_BASIS[concentrationUnit]
    if (!convertible(primaryBasis, concentrationBasis)) {
      throw new CalculatorError(
        "INCOMPATIBLE_UNITS",
        `Одиниця дози «${primaryBasis}» несумісна з концентрацією препарату «${concentrationBasis}».`,
      )
    }
    const factor = conversionFactor(primaryBasis, concentrationBasis)
    const doseMinConcBasis = doseMinRaw.times(factor)
    const doseMaxConcBasis = doseMaxRaw.times(factor)

    if (concentrationUnit === ConcentrationUnit.MG_PER_TABLET) {
      if (product.tabletDivisibleBy === null) {
        throw new CalculatorError("MISSING_TABLET_DIVISOR", "Не вказано крок ділення таблетки.")
      }
      const step = new Decimal(1).dividedBy(product.tabletDivisibleBy)
      const rawMin = doseMinConcBasis.dividedBy(concentrationValue)
      const rawMax = doseMaxConcBasis.dividedBy(concentrationValue)
      const dp = step.decimalPlaces()
      administrationMin = scaled(ceilToStep(rawMin, step), dp)
      administrationMax = scaled(floorToStep(rawMax, step), dp)
      administrationUnit = "tablets"
      explanation.push(
        `${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS[primaryBasis])} ÷ ` +
          `${formatNumber(concentrationValue)} ` +
          `${CONCENTRATION_LABELS[concentrationUnit]} = ` +
          `${fmtRange(administrationMin.value, administrationMax.value, "таблеток")} ` +
          `(округлено до кроку ${formatNumber(step)})`,
      )
      if (administrationMin.value.gt(administrationMax.value)) {
        tabletRangeInvalid = true
      }
    } else {
      const rawMin = doseMinConcBasis.dividedBy(concentrationValue)
      const rawMax = doseMaxConcBasis.dividedBy(concentrationValue)
      administrationUnit = concentrationUnit === ConcentrationUnit.MG_PER_G ? "g" : "ml"
      if (administrationUnit === "ml") {
        administrationMin = roundVolumeForDisplay(rawMin)
        administrationMax = roundVolumeForDisplay(rawMax)
      } else {
        administrationMin = quantize(rawMin, 2)
        administrationMax = quantize(rawMax, 2)
      }
      explanation.push(
        `${fmtRange(doseMinRaw, doseMaxRaw, BASIS_LABELS[primaryBasis])} ÷ ` +
          `${formatNumber(concentrationValue)} ` +
          `${CONCENTRATION_LABELS[concentrationUnit]} = ` +
          fmtRange(rawMin, rawMax, ADMINISTRATION_UNIT_LABELS[administrationUnit]),
      )
    }

    doseMinOut = quantize(doseMinRaw, 4)
    doseMaxOut = quantize(doseMaxRaw, 4)
    doseAmountUnitOut = primaryBasis
  } else {
    administrationUnit = "ml"
    administrationMin = roundVolumeForDisplay(doseMinRaw)
    administrationMax = roundVolumeForDisplay(doseMaxRaw)
    doseMinOut = null
    doseMaxOut = null
    doseAmountUnitOut = null

    const concentrationValue = product.concentrationValue
    const concentrationUnit = product.concentrationUnit
    if (
      concentrationValue !== null &&
      (concentrationUnit === ConcentrationUnit.MG_PER_ML ||
        concentrationUnit === ConcentrationUnit.MCG_PER_ML)
    ) {
      if (concentrationValue.lte(0)) {
        throw new CalculatorError("INVALID_CONCENTRATION", "Концентрація препарату має бути більшою за нуль.")
      }
      const auxMin = doseMinRaw.times(concentrationValue)
      const auxMax = doseMaxRaw.times(concentrationValue)
      doseMinOut = quantize(auxMin, 4)
      doseMaxOut = quantize(auxMax, 4)
      doseAmountUnitOut = CONCENTRATION_BASIS[concentrationUnit]
      explanation.push(
        `${fmtRange(doseMinRaw, doseMaxRaw, "мл")} × ` +
          `${formatNumber(concentrationValue)} ` +
          `${CONCENTRATION_LABELS[concentrationUnit]} = ` +
          fmtRange(doseMinOut.value, doseMaxOut.value, BASIS_LABELS[doseAmountUnitOut as Basis]),
      )
    }
  }

  // Step 4: warnings, in a fixed, deterministic order (mirrored in the other
  // two implementations so shared test cases compare warning_codes as lists).
  const warnings: CalculationWarning[] = []
  if (!(weightKg.gte(species.typicalMinWeightKg) && weightKg.lte(species.typicalMaxWeightKg))) {
    warnings.push({
      code: "WEIGHT_OUT_OF_TYPICAL_RANGE",
      messageUk:
        `Вага ${formatNumber(weightKg)} кг виходить за типовий діапазон виду ` +
        `(${formatNumber(species.typicalMinWeightKg)}–` +
        `${formatNumber(species.typicalMaxWeightKg)} кг). Підтвердіть значення.`,
    })
  }
  for (const contraindication of data.contraindications) {
    const code = contraindication.severity === Severity.ABSOLUTE ? "ABSOLUTE_CONTRAINDICATION" : "CAUTION"
    warnings.push({ code, messageUk: contraindication.messageUk })
  }
  if (!doseRule.isVerified) {
    warnings.push({
      code: "RULE_NOT_VERIFIED",
      messageUk: "Це правило дозування ще не підтверджене лікарем. Застосовуйте з обережністю.",
    })
  }
  if (species.isFoodProducing) {
    warnings.push({
      code: "FOOD_PRODUCING_ANIMAL",
      messageUk: "Продуктивна тварина: перед використанням продукції перевірте терміни виведення.",
    })
  }
  if (maxDoseCapped) {
    warnings.push({
      code: "MAX_DOSE_CAPPED",
      messageUk: "Розрахована доза перевищувала максимально допустиму і була обмежена цим значенням.",
    })
  }
  if (tabletRangeInvalid) {
    warnings.push({
      code: "TABLET_CANNOT_MATCH_RANGE",
      messageUk:
        "Після округлення до кроку ділення таблетки мінімальна кількість перевищує максимальну. " +
        "Перевірте дозу вручну.",
    })
  }

  return {
    doseMin: doseMinOut,
    doseMax: doseMaxOut,
    doseAmountUnit: doseAmountUnitOut,
    administrationMin,
    administrationMax,
    administrationUnit,
    maxDoseCapped,
    warnings,
    explanation,
  }
}
