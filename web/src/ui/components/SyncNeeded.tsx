import { useNavigate } from "react-router-dom"
import { ROUTES } from "../routes"
import "./SyncNeeded.css"

/** Empty state shown when the synced tables are empty — either a fresh
 * install, or (on iOS Safari especially) the browser evicted IndexedDB under
 * storage pressure after a previous successful sync. `storageWasCleared`
 * picks the right message for which case this is. */
export function SyncNeeded({ storageWasCleared }: { storageWasCleared: boolean }) {
  const navigate = useNavigate()

  return (
    <div className="sync-needed">
      <h2>{storageWasCleared ? "Дані було очищено" : "Потрібна перша синхронізація"}</h2>
      <p>
        {storageWasCleared
          ? "Схоже, браузер очистив локальні дані застосунку. Підключіться до мережі й синхронізуйте базу ще раз."
          : "База препаратів ще порожня. Підключіться до мережі й натисніть «Синхронізувати зараз» на екрані Налаштувань."}
      </p>
      <button type="button" onClick={() => navigate(ROUTES.settings)}>
        Перейти до налаштувань
      </button>
    </div>
  )
}
