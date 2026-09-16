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

  const [addressOverride, setAddressOverride] = useState<string | null>(null)
  const { sync, isSyncing, outcome, errorMessage } = useSyncAction()

  const handleSave = () => {
    void setServerBaseUrl(addressOverride ?? storedAddress)
  }

  return (
    <div className="settings-screen">
      <div className="card settings-screen__card">
        <p className="section-title">🌐 З'єднання з сервером</p>
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
          Зберегти адресу
        </button>
      </div>

      <div className="card settings-screen__card">
        <p className="section-title">🔄 Оновлення бази даних</p>
        <section className="settings-screen__sync">
          <p className="disclaimer">
            {lastSyncRow?.value !== undefined
              ? `🕒 Остання синхронізація: ${formatSyncTime(lastSyncRow.value)}`
              : "⚠️ Ще не синхронізовано з сервером"}
          </p>
          <button
            type="button"
            className="btn btn--primary btn--block"
            disabled={isSyncing}
            onClick={() => sync(true)}
          >
            {isSyncing ? "Синхронізація…" : "🔄 Повна пересинхронізація (Очистити й завантажити дійсні препарати)"}
          </button>
          {outcome === "success" && (
            <p className="settings-screen__outcome settings-screen__outcome--success">
              ✅ Базу даних успішно оновлено реальними препаратами!
            </p>
          )}
          {outcome === "error" && (
            <p className="settings-screen__outcome settings-screen__outcome--error">
              ❌ Помилка синхронізації. Перевірте адресу сервера та з'єднання.
              {errorMessage !== null && ` (${errorMessage})`}
            </p>
          )}
        </section>
      </div>

      <p className="disclaimer">
        ⚠️ Програма надана для медично-технічної допомоги ветеринарному лікарю. Остаточне рішення про дозування та схему лікування завжди приймає ветеринарний лікар.
      </p>
    </div>
  )
}
