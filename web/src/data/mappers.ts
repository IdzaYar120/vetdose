import type {
  ContraindicationDto,
  DoseRuleDto,
  ProductDto,
  SpeciesDto,
  SubstanceDto,
  WithdrawalPeriodDto,
} from "./remote/dto"
import type {
  ContraindicationEntity,
  DoseRuleEntity,
  ProductEntity,
  SpeciesEntity,
  SubstanceEntity,
  WithdrawalPeriodEntity,
} from "./local/schema"

export function speciesDtoToEntity(dto: SpeciesDto): SpeciesEntity {
  return {
    id: dto.id,
    code: dto.code,
    nameUk: dto.name_uk,
    isFoodProducing: dto.is_food_producing,
    typicalMinWeightKg: dto.typical_min_weight_kg,
    typicalMaxWeightKg: dto.typical_max_weight_kg,
    updatedAt: dto.updated_at,
  }
}

export function substanceDtoToEntity(dto: SubstanceDto): SubstanceEntity {
  return {
    id: dto.id,
    name: dto.name,
    nameUk: dto.name_uk,
    pharmacologicalGroup: dto.pharmacological_group,
    notes: dto.notes,
    updatedAt: dto.updated_at,
  }
}

export function productDtoToEntity(dto: ProductDto): ProductEntity {
  return {
    id: dto.id,
    tradeName: dto.trade_name,
    manufacturer: dto.manufacturer,
    substanceId: dto.substance_id,
    form: dto.form,
    concentrationValue: dto.concentration_value,
    concentrationUnit: dto.concentration_unit,
    tabletDivisibleBy: dto.tablet_divisible_by,
    updatedAt: dto.updated_at,
  }
}

export function doseRuleDtoToEntity(dto: DoseRuleDto): DoseRuleEntity {
  return {
    id: dto.id,
    substanceId: dto.substance_id,
    speciesId: dto.species_id,
    route: dto.route,
    indication: dto.indication,
    doseMin: dto.dose_min,
    doseMax: dto.dose_max,
    doseUnit: dto.dose_unit,
    maxTotalDose: dto.max_total_dose,
    maxTotalDoseUnit: dto.max_total_dose_unit,
    frequency: dto.frequency,
    duration: dto.duration,
    notes: dto.notes,
    source: dto.source,
    isVerified: dto.is_verified,
    updatedAt: dto.updated_at,
  }
}

export function contraindicationDtoToEntity(dto: ContraindicationDto): ContraindicationEntity {
  return {
    id: dto.id,
    substanceId: dto.substance_id,
    speciesId: dto.species_id,
    condition: dto.condition,
    severity: dto.severity,
    messageUk: dto.message_uk,
    source: dto.source,
    updatedAt: dto.updated_at,
  }
}

export function withdrawalPeriodDtoToEntity(dto: WithdrawalPeriodDto): WithdrawalPeriodEntity {
  return {
    id: dto.id,
    productId: dto.product_id,
    speciesId: dto.species_id,
    route: dto.route,
    foodProduct: dto.food_product,
    days: dto.days,
    hours: dto.hours,
    source: dto.source,
    updatedAt: dto.updated_at,
  }
}
