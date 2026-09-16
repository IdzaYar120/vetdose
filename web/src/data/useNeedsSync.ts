import { useLiveQuery } from "dexie-react-hooks"
import { db } from "./local/db"
import { LAST_SYNC_TIME_KEY } from "./settingsRepository"

export interface NeedsSyncState {
  /** `undefined` while the initial read is still in flight. */
  loaded: boolean
  needsSync: boolean
  /** Distinguishes "never synced" from "synced before, but the core tables
   * are empty now" (iOS Safari can evict IndexedDB under storage pressure —
   * this is the "cleared storage" recovery case the Stage 7 plan asks for),
   * so the empty state can say the right thing. */
  storageWasCleared: boolean
}

/** Reactive "is there any synced data at all" check, used by every screen
 * that depends on the synced tables (mirrors Android's `needsFirstSync`,
 * generalized to also detect data that disappeared after having synced
 * before, not just a fresh install). */
export function useNeedsSync(): NeedsSyncState {
  const speciesCount = useLiveQuery(() => db.species.count())
  // `db.settings.get()` resolves to `undefined` both while still loading AND
  // once loaded with no matching row (a fresh install), so — unlike
  // `speciesCount` — it can't be used to gate `loaded`; it's only consulted
  // for the "was this cleared" distinction once `loaded` is already true.
  const lastSyncRow = useLiveQuery(() => db.settings.get(LAST_SYNC_TIME_KEY))

  const loaded = speciesCount !== undefined
  const needsSync = loaded && speciesCount === 0
  const storageWasCleared = needsSync && lastSyncRow?.value !== undefined

  return { loaded, needsSync, storageWasCleared }
}
