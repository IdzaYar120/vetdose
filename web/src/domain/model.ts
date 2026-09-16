/**
 * Domain models — mirrors Android's `domain/model/*.kt`. Decimal fields are
 * parsed `Decimal` here (unlike the string-typed `data/local/schema.ts`
 * entities), since these are what the calculator and screens actually do
 * math and formatting with. `data/repositories.ts` converts entities to
 * these.
 */

import type {
  ConcentrationUnit,
  Decimal,
  DoseUnit,
  MaxTotalDoseUnit,
  Severity,
} from "./calculator/doseCalculator"
import type { FoodProduct, ProductForm, Route } from "./enums"

export interface Species {
  id: string
  code: string
  nameUk: string
  isFoodProducing: boolean
  typicalMinWeightKg: Decimal
  typicalMaxWeightKg: Decimal
}

export interface Substance {
  id: string
  name: string
  nameUk: string
  pharmacologicalGroup: string | null
  notes: string | null
}

// Reuses domain.calculator's ConcentrationUnit rather than duplicating it:
// the value read from Dexie is the exact same thing later fed straight into
// DoseCalculator's ProductInput.
export interface Product {
  id: string
  tradeName: string
  manufacturer: string | null
  substanceId: string
  form: ProductForm
  concentrationValue: Decimal
  concentrationUnit: ConcentrationUnit
  tabletDivisibleBy: number | null
}

export interface DoseRule {
  id: string
  substanceId: string
  speciesId: string
  route: Route
  indication: string | null
  doseMin: Decimal
  doseMax: Decimal
  doseUnit: DoseUnit
  maxTotalDose: Decimal | null
  maxTotalDoseUnit: MaxTotalDoseUnit | null
  frequency: string | null
  duration: string | null
  notes: string | null
  source: string
  isVerified: boolean
}

export interface Contraindication {
  id: string
  substanceId: string
  speciesId: string | null
  condition: string | null
  severity: Severity
  messageUk: string
  source: string
}

export interface WithdrawalPeriod {
  id: string
  productId: string
  speciesId: string
  route: Route
  foodProduct: FoodProduct
  days: number
  hours: number | null
  source: string
}

/** Stores a calculation's inputs, not its result — see
 * `CalculationHistoryEntity`'s doc comment for why. */
export interface CalculationHistoryEntry {
  id: string
  timestamp: number
  speciesId: string
  doseRuleId: string
  productId: string
  weightKg: Decimal
}

export interface FavoriteProduct {
  productId: string
  addedAt: number
}
