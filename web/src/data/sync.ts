import { db } from "./local/db"
import {
  contraindicationDtoToEntity,
  doseRuleDtoToEntity,
  productDtoToEntity,
  speciesDtoToEntity,
  substanceDtoToEntity,
  withdrawalPeriodDtoToEntity,
} from "./mappers"
import { fetchSync } from "./remote/client"
import { getLastSyncTime, getServerBaseUrl, setLastSyncTime } from "./settingsRepository"

interface SyncedDto {
  id: string
  is_deleted: boolean
}

/** Upserts active rows and physically deletes rows the server marked
 * `is_deleted` — the app never shows soft-deleted rows, so there is no
 * reason to keep them locally (same policy as Android's `SyncRepository`).
 *
 * Takes `bulkPut`/`bulkDelete` as plain callbacks bound at the call site
 * (rather than a Dexie `Table<Entity, "id">` parameter): Dexie's table types
 * rely on mapped/conditional types keyed off a literal primary-key property
 * name, which do not resolve for a generic `Entity` type parameter. Calling
 * the real, concretely-typed table methods through a closure sidesteps that
 * without weakening type safety at the call site. */
async function applyChanges<Dto extends SyncedDto, Entity>(
  rows: Dto[],
  toEntity: (dto: Dto) => Entity,
  bulkPut: (items: Entity[]) => Promise<unknown>,
  bulkDelete: (ids: string[]) => Promise<unknown>,
): Promise<void> {
  const deletedIds = rows.filter((row) => row.is_deleted).map((row) => row.id)
  const activeEntities = rows.filter((row) => !row.is_deleted).map(toEntity)
  if (deletedIds.length > 0) {
    await bulkDelete(deletedIds)
  }
  if (activeEntities.length > 0) {
    await bulkPut(activeEntities)
  }
}

/** Runs one incremental sync against `/api/v1/sync` and applies the result to
 * Dexie in a single transaction. Safe to call repeatedly (periodic + manual
 * "sync now", mirroring the Android `SyncWorker`); a stage-7 screen is
 * expected to drive this from a button and/or a periodic timer. */
export async function runSync(): Promise<void> {
  const baseUrl = await getServerBaseUrl()
  const since = await getLastSyncTime()
  const response = await fetchSync(baseUrl, since)

  await db.transaction(
    "rw",
    [db.species, db.substances, db.products, db.doseRules, db.contraindications, db.withdrawalPeriods],
    async () => {
      await applyChanges(
        response.species,
        speciesDtoToEntity,
        (items) => db.species.bulkPut(items),
        (ids) => db.species.bulkDelete(ids),
      )
      await applyChanges(
        response.substances,
        substanceDtoToEntity,
        (items) => db.substances.bulkPut(items),
        (ids) => db.substances.bulkDelete(ids),
      )
      await applyChanges(
        response.products,
        productDtoToEntity,
        (items) => db.products.bulkPut(items),
        (ids) => db.products.bulkDelete(ids),
      )
      await applyChanges(
        response.dose_rules,
        doseRuleDtoToEntity,
        (items) => db.doseRules.bulkPut(items),
        (ids) => db.doseRules.bulkDelete(ids),
      )
      await applyChanges(
        response.contraindications,
        contraindicationDtoToEntity,
        (items) => db.contraindications.bulkPut(items),
        (ids) => db.contraindications.bulkDelete(ids),
      )
      await applyChanges(
        response.withdrawal_periods,
        withdrawalPeriodDtoToEntity,
        (items) => db.withdrawalPeriods.bulkPut(items),
        (ids) => db.withdrawalPeriods.bulkDelete(ids),
      )
    },
  )

  await setLastSyncTime(response.server_time)
}
