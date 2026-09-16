import { Decimal } from "../domain/calculator/doseCalculator"
import type {
  CalculationHistoryEntry,
  Contraindication,
  DoseRule,
  FavoriteProduct,
  Product,
  Species,
  Substance,
  WithdrawalPeriod,
} from "../domain/model"
import type {
  CalculationHistoryEntity,
  ContraindicationEntity,
  DoseRuleEntity,
  FavoriteProductEntity,
  ProductEntity,
  SpeciesEntity,
  SubstanceEntity,
  WithdrawalPeriodEntity,
} from "./local/schema"

export function speciesEntityToDomain(entity: SpeciesEntity): Species {
  return {
    id: entity.id,
    code: entity.code,
    nameUk: entity.nameUk,
    isFoodProducing: entity.isFoodProducing,
    typicalMinWeightKg: new Decimal(entity.typicalMinWeightKg),
    typicalMaxWeightKg: new Decimal(entity.typicalMaxWeightKg),
  }
}

export function substanceEntityToDomain(entity: SubstanceEntity): Substance {
  return {
    id: entity.id,
    name: entity.name,
    nameUk: entity.nameUk,
    pharmacologicalGroup: entity.pharmacologicalGroup,
    notes: entity.notes,
  }
}

export function productEntityToDomain(entity: ProductEntity): Product {
  return {
    id: entity.id,
    tradeName: entity.tradeName,
    manufacturer: entity.manufacturer,
    substanceId: entity.substanceId,
    form: entity.form,
    concentrationValue: new Decimal(entity.concentrationValue),
    concentrationUnit: entity.concentrationUnit,
    tabletDivisibleBy: entity.tabletDivisibleBy,
  }
}

export function doseRuleEntityToDomain(entity: DoseRuleEntity): DoseRule {
  return {
    id: entity.id,
    substanceId: entity.substanceId,
    speciesId: entity.speciesId,
    route: entity.route,
    indication: entity.indication,
    doseMin: new Decimal(entity.doseMin),
    doseMax: new Decimal(entity.doseMax),
    doseUnit: entity.doseUnit,
    maxTotalDose: entity.maxTotalDose !== null ? new Decimal(entity.maxTotalDose) : null,
    maxTotalDoseUnit: entity.maxTotalDoseUnit,
    frequency: entity.frequency,
    duration: entity.duration,
    notes: entity.notes,
    source: entity.source,
    isVerified: entity.isVerified,
  }
}

export function contraindicationEntityToDomain(entity: ContraindicationEntity): Contraindication {
  return {
    id: entity.id,
    substanceId: entity.substanceId,
    speciesId: entity.speciesId,
    condition: entity.condition,
    severity: entity.severity,
    messageUk: entity.messageUk,
    source: entity.source,
  }
}

export function withdrawalPeriodEntityToDomain(entity: WithdrawalPeriodEntity): WithdrawalPeriod {
  return {
    id: entity.id,
    productId: entity.productId,
    speciesId: entity.speciesId,
    route: entity.route,
    foodProduct: entity.foodProduct,
    days: entity.days,
    hours: entity.hours,
    source: entity.source,
  }
}

export function calculationHistoryEntityToDomain(entity: CalculationHistoryEntity): CalculationHistoryEntry {
  return {
    id: entity.id,
    timestamp: entity.timestamp,
    speciesId: entity.speciesId,
    doseRuleId: entity.doseRuleId,
    productId: entity.productId,
    weightKg: new Decimal(entity.weightKg),
  }
}

export function favoriteProductEntityToDomain(entity: FavoriteProductEntity): FavoriteProduct {
  return { productId: entity.productId, addedAt: entity.addedAt }
}
