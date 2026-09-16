import { useLiveQuery } from "dexie-react-hooks"
import { useState } from "react"
import { db } from "../../data/local/db"
import {
  DEFAULT_SERVER_BASE_URL,
  LAST_SYNC_TIME_KEY,
  SERVER_BASE_URL_KEY,
  setServerBaseUrl,
} from "../../data/settingsRepository"
import { useSyncAction } from "../../data/useSyncAction"
import "./SettingsScreen.css"

function formatSyncTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString("uk-UA", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  } catch {
    return iso
  }
}

export function SettingsScreen() {
  const storedAddressRow = useLiveQuery(() => db.settings.get(SERVER_BASE_URL_KEY))
  const lastSyncRow = useLiveQuery(() => db.settings.get(LAST_SYNC_TIME_KEY))
  const storedAddress = storedAddressRow?.value ?? DEFAULT_SERVER_BASE_URL

  // `null` means "user hasn't typed anything yet" — the field displays
  // `storedAddress` via the fallback below until they do, so there's no need
  // to copy it into this state up front.
  const [addressOverride, setAddressOverride] = useState<string | null>(null)
  const { sync, isSyncing, outcome, errorMessage } = useSyncAction()

  const handleSave = () => {
    void setServerBaseUrl(addressOverride ?? storedAddress)
  }

  return (
    <div className="settings-screen">
      <section className="field">
        <label htmlFor="server-address">Адреса сервера</label>
        <input
          id="server-address"
          type="text"
          value={addressOverride ?? storedAddress}
          onChange={(e) => setAddressOverride(e.target.value)}
        />
      </section>
      <button type="button" className="btn btn--outline" onClick={handleSave}>
        Зберегти
      </button>

      <section className="settings-screen__sync">
        <p>
          {lastSyncRow?.value !== undefined
            ? `Остання синхронізація: ${formatSyncTime(lastSyncRow.value)}`
            : "Ще не синхронізовано"}
        </p>
        <button type="button" className="btn btn--primary btn--block" disabled={isSyncing} onClick={sync}>
          {isSyncing ? "Синхронізація…" : "Синхронізувати зараз"}
        </button>
        {outcome === "success" && <p className="settings-screen__outcome">Синхронізацію завершено успішно</p>}
        {outcome === "error" && (
          <p className="settings-screen__outcome settings-screen__outcome--error">
            Помилка синхронізації. Перевірте адресу сервера та з'єднання з мережею.
            {errorMessage !== null && ` (${errorMessage})`}
          </p>
        )}
      </section>

      <p className="disclaimer">
        Програма допомагає лікарю розраховувати дози. Рішення про лікування завжди приймає лікар.
      </p>
    </div>
  )
}
