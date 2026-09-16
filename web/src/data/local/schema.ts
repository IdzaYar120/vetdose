/**
 * IndexedDB entity types (camelCase) — the offline-first single source of
 * truth for the UI, mirroring Android's Room entities 1:1 with the backend
 * tables. Decimal fields stay as strings here too (IndexedDB has no Decimal
 * type, and storing floats would reintroduce the precision loss the project
 * forbids); they are parsed into `Decimal` only where the calculator or a
 * screen needs to do math or format them.
 *
 * `isDeleted` rows are never written locally: `runSync()` (`data/sync.ts`)
 * physically deletes them, the same policy as the Android `SyncRepository`.
 */

import type {
  ConcentrationUnit,
  DoseUnit,
  MaxTotalDoseUnit,
  Severity,
} from "../../domain/calculator/doseCalculator"
import type { FoodProduct, ProductForm, Route } from "../../domain/enums"

export interface SpeciesEntity {
  id: string
  code: string
  nameUk: string
  isFoodProducing: boolean
  typicalMinWeightKg: string
  typicalMaxWeightKg: string
  updatedAt: string
}

export interface SubstanceEntity {
  id: string
  name: string
  nameUk: string
  pharmacologicalGroup: string | null
  notes: string | null
  updatedAt: string
}

export interface ProductEntity {
  id: string
  tradeName: string
  manufacturer: string | null
  substanceId: string
  form: ProductForm
  concentrationValue: string
  concentrationUnit: ConcentrationUnit
  tabletDivisibleBy: number | null
  updatedAt: string
}

export interface DoseRuleEntity {
  id: string
  substanceId: string
  speciesId: string
  route: Route
  indication: string | null
  doseMin: string
  doseMax: string
  doseUnit: DoseUnit
  maxTotalDose: string | null
  maxTotalDoseUnit: MaxTotalDoseUnit | null
  frequency: string | null
  duration: string | null
  notes: string | null
  source: string
  isVerified: boolean
  updatedAt: string
}

export interface ContraindicationEntity {
  id: string
  substanceId: string
  speciesId: string | null
  condition: string | null
  severity: Severity
  messageUk: string
  source: string
  updatedAt: string
}

export interface WithdrawalPeriodEntity {
  id: string
  productId: string
  speciesId: string
  route: Route
  foodProduct: FoodProduct
  days: number
  hours: number | null
  source: string
  updatedAt: string
}

/** Small persisted key/value store for app settings (server base URL, last
 * sync cursor) — the web equivalent of Android's DataStore-backed
 * `SettingsRepository`. */
export interface SettingEntity {
  key: string
  value: string
}

/**
 * Local-only tables (never synced, never touched by `runSync()`) — the web
 * equivalent of Android's `calculation_history` / `favorite_product` Room
 * tables added in its Stage 5.
 */

/** Stores a calculation's *inputs*, not its result: the calculator is pure
 * and cheap, so the result is always recomputed on display. `weightKg`
 * stays a string for the same reason every other Decimal field does. */
export interface CalculationHistoryEntity {
  id: string
  timestamp: number
  speciesId: string
  doseRuleId: string
  productId: string
  weightKg: string
}

export interface FavoriteProductEntity {
  productId: string
  addedAt: number
}
