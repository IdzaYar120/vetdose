import Dexie, { type EntityTable } from "dexie"
import type {
  CalculationHistoryEntity,
  ContraindicationEntity,
  DoseRuleEntity,
  FavoriteProductEntity,
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
  calculationHistory!: EntityTable<CalculationHistoryEntity, "id">
  favoriteProducts!: EntityTable<FavoriteProductEntity, "productId">

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
    // Stage 7: local-only history/favorites tables, the same as Android's
    // MIGRATION_1_2. Dexie requires every version's stores() call to repeat
    // unchanged table schemas — omitting one deletes that table.
    this.version(2).stores({
      species: "id, code",
      substances: "id, nameUk",
      products: "id, substanceId, tradeName",
      // The compound index serves findDoseRuleForSubstanceAndSpecies's
      // exact-match lookup (data/repositories.ts); without it Dexie can only
      // use one of the two single-column indexes and filters the rest in
      // memory.
      doseRules: "id, substanceId, speciesId, [substanceId+speciesId]",
      contraindications: "id, substanceId, speciesId",
      withdrawalPeriods: "id, productId, speciesId",
      settings: "key",
      calculationHistory: "id, timestamp",
      favoriteProducts: "productId",
    })
  }
}

export const db = new VetDoseDatabase()
