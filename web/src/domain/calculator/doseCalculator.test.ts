/**
 * Runs every case in shared/calculation_test_cases.json against the
 * TypeScript calculator. The same file is also read by the backend (pytest)
 * and Android (JUnit) test suites so all three implementations are checked
 * for agreement.
 */
import { readFileSync } from "node:fs"
import { join } from "node:path"
import { describe, expect, it } from "vitest"
import {
  CalculatorError,
  Decimal,
  type CalculationInput,
  type ContraindicationInput,
  calculateDose,
  scaledToFixed,
} from "./doseCalculator"

interface RawSpecies {
  is_food_producing: boolean
  typical_min_weight_kg: string
  typical_max_weight_kg: string
}

interface RawDoseRule {
  dose_min: string
  dose_max: string
  dose_unit: string
  is_verified: boolean
  max_total_dose: string | null
  max_total_dose_unit: string | null
}

interface RawProduct {
  concentration_value: string | null
  concentration_unit: string | null
  tablet_divisible_by: number | null
}

interface RawContraindication {
  severity: string
  message_uk: string
}

interface RawCase {
  id: string
  description: string
  input: {
    weight_kg: string
    species: RawSpecies
    dose_rule: RawDoseRule
    product: RawProduct
    contraindications: RawContraindication[]
  }
  expected: {
    error_code: string | null
    dose_min?: string | null
    dose_max?: string | null
    dose_amount_unit?: string | null
    administration_min?: string
    administration_max?: string
    administration_unit?: string
    max_dose_capped?: boolean
    warning_codes?: string[]
    explanation?: string[]
  }
}

const SHARED_CASES_PATH = join(
  import.meta.dirname,
  "..",
  "..",
  "..",
  "..",
  "shared",
  "calculation_test_cases.json",
)

function loadCases(): RawCase[] {
  return JSON.parse(readFileSync(SHARED_CASES_PATH, "utf-8")) as RawCase[]
}

function decimalOrNull(value: string | null | undefined): Decimal | null {
  return value !== null && value !== undefined ? new Decimal(value) : null
}

function buildInput(raw: RawCase["input"]): CalculationInput {
  const contraindications: ContraindicationInput[] = raw.contraindications.map((c) => ({
    severity: c.severity as ContraindicationInput["severity"],
    messageUk: c.message_uk,
  }))

  return {
    weightKg: new Decimal(raw.weight_kg),
    species: {
      isFoodProducing: raw.species.is_food_producing,
      typicalMinWeightKg: new Decimal(raw.species.typical_min_weight_kg),
      typicalMaxWeightKg: new Decimal(raw.species.typical_max_weight_kg),
    },
    doseRule: {
      doseMin: new Decimal(raw.dose_rule.dose_min),
      doseMax: new Decimal(raw.dose_rule.dose_max),
      doseUnit: raw.dose_rule.dose_unit as CalculationInput["doseRule"]["doseUnit"],
      isVerified: raw.dose_rule.is_verified,
      maxTotalDose: decimalOrNull(raw.dose_rule.max_total_dose),
      maxTotalDoseUnit: raw.dose_rule.max_total_dose_unit as CalculationInput["doseRule"]["maxTotalDoseUnit"],
    },
    product: {
      concentrationValue: decimalOrNull(raw.product.concentration_value),
      concentrationUnit: raw.product.concentration_unit as CalculationInput["product"]["concentrationUnit"],
      tabletDivisibleBy: raw.product.tablet_divisible_by,
    },
    contraindications,
  }
}

const CASES = loadCases()

describe("calculateDose shared cases", () => {
  it("has enough coverage", () => {
    expect(CASES.length).toBeGreaterThanOrEqual(25)
  })

  for (const testCase of CASES) {
    it(testCase.id, () => {
      const input = buildInput(testCase.input)
      const expected = testCase.expected

      if (expected.error_code !== null) {
        try {
          calculateDose(input)
          expect.fail(`expected ${expected.error_code} but calculation succeeded`)
        } catch (error) {
          expect(error).toBeInstanceOf(CalculatorError)
          expect((error as CalculatorError).code).toBe(expected.error_code)
        }
        return
      }

      const result = calculateDose(input)

      expect(result.doseMin ? scaledToFixed(result.doseMin) : null).toBe(expected.dose_min ?? null)
      expect(result.doseMax ? scaledToFixed(result.doseMax) : null).toBe(expected.dose_max ?? null)
      expect(result.doseAmountUnit).toBe(expected.dose_amount_unit ?? null)
      expect(scaledToFixed(result.administrationMin)).toBe(expected.administration_min)
      expect(scaledToFixed(result.administrationMax)).toBe(expected.administration_max)
      expect(result.administrationUnit).toBe(expected.administration_unit)
      expect(result.maxDoseCapped).toBe(expected.max_dose_capped)
      expect(result.warnings.map((w) => w.code)).toEqual(expected.warning_codes)
      expect(result.explanation).toEqual(expected.explanation)
    })
  }
})
