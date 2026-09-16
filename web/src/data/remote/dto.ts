/**
 * Wire types for `GET /api/v1/sync`, matching the backend's
 * `app/schemas/*.py` `*Read` models field-for-field (snake_case, as JSON
 * sends them). Decimal fields are strings on the wire (see
 * `backend/app/schemas/common.py::DecimalStr`) — never parse them with
 * `Number()`; use `Decimal` from `domain/calculator/doseCalculator.ts`.
 *
 * Kept separate from the camelCase entity types in `data/local/schema.ts`;
 * `data/mappers.ts` converts between the two, mirroring the Android data
 * layer's DTO -> Room-entity split.
 */

import type {
  ConcentrationUnit,
  DoseUnit,
  MaxTotalDoseUnit,
  Severity,
} from "../../domain/calculator/doseCalculator"
import type { FoodProduct, ProductForm, Route } from "../../domain/enums"

export interface SpeciesDto {
  id: string
  code: string
  name_uk: string
  is_food_producing: boolean
  typical_min_weight_kg: string
  typical_max_weight_kg: string
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface SubstanceDto {
  id: string
  name: string
  name_uk: string
  pharmacological_group: string | null
  notes: string | null
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface ProductDto {
  id: string
  trade_name: string
  manufacturer: string | null
  substance_id: string
  form: ProductForm
  concentration_value: string
  concentration_unit: ConcentrationUnit
  tablet_divisible_by: number | null
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface DoseRuleDto {
  id: string
  substance_id: string
  species_id: string
  route: Route
  indication: string | null
  dose_min: string
  dose_max: string
  dose_unit: DoseUnit
  max_total_dose: string | null
  max_total_dose_unit: MaxTotalDoseUnit | null
  frequency: string | null
  duration: string | null
  notes: string | null
  source: string
  is_verified: boolean
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface ContraindicationDto {
  id: string
  substance_id: string
  species_id: string | null
  condition: string | null
  severity: Severity
  message_uk: string
  source: string
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface WithdrawalPeriodDto {
  id: string
  product_id: string
  species_id: string
  route: Route
  food_product: FoodProduct
  days: number
  hours: number | null
  source: string
  created_at: string
  updated_at: string
  is_deleted: boolean
}

export interface SyncResponseDto {
  server_time: string
  species: SpeciesDto[]
  substances: SubstanceDto[]
  products: ProductDto[]
  dose_rules: DoseRuleDto[]
  contraindications: ContraindicationDto[]
  withdrawal_periods: WithdrawalPeriodDto[]
}
