/** Small persisted app settings — the web equivalent of Android's
 * DataStore-backed `SettingsRepository`: the backend base URL and the `since`
 * cursor for the next incremental `/sync`. Backed by `db.settings`
 * (`data/local/db.ts`) rather than `localStorage` so it stays inside the same
 * transactional store as the synced data. */

import { db } from "./local/db"

const SERVER_BASE_URL_KEY = "serverBaseUrl"
const LAST_SYNC_TIME_KEY = "lastSyncTime"

/** Vite's dev server runs on :5173 (see `backend/app/config.py`'s default
 * CORS origin); the backend itself defaults to :8000. */
export const DEFAULT_SERVER_BASE_URL = "http://localhost:8000/"

export async function getServerBaseUrl(): Promise<string> {
  const row = await db.settings.get(SERVER_BASE_URL_KEY)
  return row?.value ?? DEFAULT_SERVER_BASE_URL
}

export async function setServerBaseUrl(url: string): Promise<void> {
  await db.settings.put({ key: SERVER_BASE_URL_KEY, value: url })
}

/** ISO-8601 timestamp of the last successful sync, or `null` before the
 * first one — passed as `?since=` to make the next `/sync` incremental. */
export async function getLastSyncTime(): Promise<string | null> {
  const row = await db.settings.get(LAST_SYNC_TIME_KEY)
  return row?.value ?? null
}

export async function setLastSyncTime(isoTimestamp: string): Promise<void> {
  await db.settings.put({ key: LAST_SYNC_TIME_KEY, value: isoTimestamp })
}
