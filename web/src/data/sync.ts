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

export async function runSync(forceFullSync: boolean = false): Promise<void> {
  const baseUrl = await getServerBaseUrl()
  const since = forceFullSync ? null : await getLastSyncTime()
  const response = await fetchSync(baseUrl, since)

  await db.transaction(
    "rw",
    [db.species, db.substances, db.products, db.doseRules, db.contraindications, db.withdrawalPeriods],
    async () => {
      if (since === null) {
        await Promise.all([
          db.species.clear(),
          db.substances.clear(),
          db.products.clear(),
          db.doseRules.clear(),
          db.contraindications.clear(),
          db.withdrawalPeriods.clear(),
        ])
      }
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
