import Dexie, { type EntityTable } from "dexie"
import type {
  ContraindicationEntity,
  DoseRuleEntity,
  ProductEntity,
  SettingEntity,
  SpeciesEntity,
  SubstanceEntity,
  WithdrawalPeriodEntity,
} from "./schema"

export class VetDoseDatabase extends Dexie {
  species!: EntityTable<SpeciesEntity, "id">
  substances!: EntityTable<SubstanceEntity, "id">
  products!: EntityTable<ProductEntity, "id">
  doseRules!: EntityTable<DoseRuleEntity, "id">
  contraindications!: EntityTable<ContraindicationEntity, "id">
  withdrawalPeriods!: EntityTable<WithdrawalPeriodEntity, "id">
  settings!: EntityTable<SettingEntity, "key">

  constructor() {
    super("vetdose")
    this.version(1).stores({
      species: "id, code",
      substances: "id, nameUk",
      products: "id, substanceId, tradeName",
      doseRules: "id, substanceId, speciesId",
      contraindications: "id, substanceId, speciesId",
      withdrawalPeriods: "id, productId, speciesId",
      settings: "key",
    })
  }
}

export const db = new VetDoseDatabase()
