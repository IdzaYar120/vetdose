import { useLiveQuery } from "dexie-react-hooks"
import { db } from "./local/db"
import { LAST_SYNC_TIME_KEY } from "./settingsRepository"

export interface NeedsSyncState {
  loaded: boolean
  needsSync: boolean
  storageWasCleared: boolean
}

export function useNeedsSync(): NeedsSyncState {
  const speciesCount = useLiveQuery(() => db.species.count())
  const hasLegacyTestData = useLiveQuery(async () => {
    const firstProduct = await db.products.first()
    return firstProduct?.tradeName.startsWith("TEST_") ?? false
  })
  const lastSyncRow = useLiveQuery(() => db.settings.get(LAST_SYNC_TIME_KEY))

  const loaded = speciesCount !== undefined && hasLegacyTestData !== undefined
  const needsSync = loaded && (speciesCount === 0 || hasLegacyTestData)
  const storageWasCleared = needsSync && lastSyncRow?.value !== undefined && !hasLegacyTestData

  return { loaded, needsSync, storageWasCleared }
}
