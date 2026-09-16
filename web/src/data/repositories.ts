/**
 * Repository layer — mirrors Android's `data/repository/*.kt`, adapted to
 * plain async functions instead of Kotlin `Flow`s: screens make them
 * reactive by calling them from `dexie-react-hooks`'s `useLiveQuery`, which
 * already tracks which tables a query touched and re-runs it on change, so
 * there's no need for a hand-rolled observable layer here.
 */

import type { Decimal } from "../domain/calculator/doseCalculator"
import type {
  CalculationHistoryEntry,
  Contraindication,
  DoseRule,
  FavoriteProduct,
  Product,
  Species,
  WithdrawalPeriod,
} from "../domain/model"
import {
  calculationHistoryEntityToDomain,
  contraindicationEntityToDomain,
  doseRuleEntityToDomain,
  favoriteProductEntityToDomain,
  productEntityToDomain,
  speciesEntityToDomain,
  withdrawalPeriodEntityToDomain,
} from "./entityMappers"
import { db } from "./local/db"

export async function getAllSpecies(): Promise<Species[]> {
  const entities = await db.species.toArray()
  return entities.map(speciesEntityToDomain).sort((a, b) => a.nameUk.localeCompare(b.nameUk, "uk"))
}

export async function getSpeciesById(id: string): Promise<Species | null> {
  const entity = await db.species.get(id)
  return entity ? speciesEntityToDomain(entity) : null
}

/**
 * Mirrors the backend's `GET /products?search=&species_id=`, but runs
 * entirely against Dexie: `query` matches trade name or substance name via
 * JS's Unicode-aware `String#toLowerCase`/`includes` (not a SQL `LIKE`,
 * which is ASCII-only and would miss Cyrillic case-folding — same reasoning
 * as the Android port), `speciesId` narrows to products whose substance has
 * a dose rule for that species.
 */
export async function searchProducts(query: string | null, speciesId: string | null): Promise<Product[]> {
  let productEntities
  if (speciesId !== null) {
    const doseRules = await db.doseRules.where("speciesId").equals(speciesId).toArray()
    const substanceIds = [...new Set(doseRules.map((rule) => rule.substanceId))]
    productEntities =
      substanceIds.length > 0 ? await db.products.where("substanceId").anyOf(substanceIds).toArray() : []
  } else {
    productEntities = await db.products.toArray()
  }

  const trimmedQuery = query?.trim().toLowerCase() ?? ""
  let filtered = productEntities
  if (trimmedQuery !== "") {
    const substances = await db.substances.toArray()
    const substanceById = new Map(substances.map((s) => [s.id, s]))
    filtered = productEntities.filter((product) => {
      const substance = substanceById.get(product.substanceId)
      return (
        product.tradeName.toLowerCase().includes(trimmedQuery) ||
        (substance?.nameUk.toLowerCase().includes(trimmedQuery) ?? false) ||
        (substance?.name.toLowerCase().includes(trimmedQuery) ?? false)
      )
    })
  }

  return filtered.map(productEntityToDomain).sort((a, b) => a.tradeName.localeCompare(b.tradeName, "uk"))
}

export async function getProductById(id: string): Promise<Product | null> {
  const entity = await db.products.get(id)
  return entity ? productEntityToDomain(entity) : null
}

/** The Calculate screen's product picker already narrows products to ones
 * with a rule for the chosen species, so there should be exactly one match;
 * `null` means the data changed underneath the user (e.g. a sync just
 * removed the rule) and they need to re-pick. */
export async function findDoseRuleForSubstanceAndSpecies(
  substanceId: string,
  speciesId: string,
): Promise<DoseRule | null> {
  const entity = await db.doseRules.where({ substanceId, speciesId }).first()
  return entity ? doseRuleEntityToDomain(entity) : null
}

export async function getDoseRuleById(id: string): Promise<DoseRule | null> {
  const entity = await db.doseRules.get(id)
  return entity ? doseRuleEntityToDomain(entity) : null
}

export async function getContraindicationsBySubstance(substanceId: string): Promise<Contraindication[]> {
  const entities = await db.contraindications.where("substanceId").equals(substanceId).toArray()
  return entities.map(contraindicationEntityToDomain)
}

export async function getWithdrawalPeriodsByProduct(productId: string): Promise<WithdrawalPeriod[]> {
  const entities = await db.withdrawalPeriods.where("productId").equals(productId).toArray()
  return entities.map(withdrawalPeriodEntityToDomain)
}

export async function getAllFavoriteProductIds(): Promise<Set<string>> {
  const entities = await db.favoriteProducts.toArray()
  return new Set(entities.map((e) => e.productId))
}

export async function getAllFavorites(): Promise<FavoriteProduct[]> {
  const entities = await db.favoriteProducts.toArray()
  return entities.map(favoriteProductEntityToDomain)
}

export async function setFavorite(productId: string, isFavorite: boolean): Promise<void> {
  if (isFavorite) {
    await db.favoriteProducts.put({ productId, addedAt: Date.now() })
  } else {
    await db.favoriteProducts.delete(productId)
  }
}

export async function getRecentHistory(limit: number): Promise<CalculationHistoryEntry[]> {
  const entities = await db.calculationHistory.orderBy("timestamp").reverse().limit(limit).toArray()
  return entities.map(calculationHistoryEntityToDomain)
}

export async function recordHistory(
  speciesId: string,
  doseRuleId: string,
  productId: string,
  weightKg: Decimal,
): Promise<void> {
  await db.calculationHistory.put({
    id: crypto.randomUUID(),
    timestamp: Date.now(),
    speciesId,
    doseRuleId,
    productId,
    weightKg: weightKg.toFixed(),
  })
}
