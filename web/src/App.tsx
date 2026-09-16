import { useCallback, useEffect, useState } from "react"
import { db } from "./data/local/db"
import { DEFAULT_SERVER_BASE_URL, getServerBaseUrl } from "./data/settingsRepository"
import { runSync } from "./data/sync"
import "./App.css"

interface Counts {
  species: number
  substances: number
  products: number
  doseRules: number
  contraindications: number
  withdrawalPeriods: number
}

const EMPTY_COUNTS: Counts = {
  species: 0,
  substances: 0,
  products: 0,
  doseRules: 0,
  contraindications: 0,
  withdrawalPeriods: 0,
}

async function loadCounts(): Promise<Counts> {
  return {
    species: await db.species.count(),
    substances: await db.substances.count(),
    products: await db.products.count(),
    doseRules: await db.doseRules.count(),
    contraindications: await db.contraindications.count(),
    withdrawalPeriods: await db.withdrawalPeriods.count(),
  }
}

/**
 * Stage 6 has no screens yet (those are Stage 7) — this is a minimal
 * diagnostic page exercising the real data layer end to end: it drives
 * `runSync()` against a live backend and shows what landed in Dexie, so the
 * sync/Dexie/API-client code is verified in an actual browser, not just
 * under Vitest.
 */
function App() {
  const [baseUrl, setBaseUrl] = useState<string>(DEFAULT_SERVER_BASE_URL)
  const [counts, setCounts] = useState<Counts>(EMPTY_COUNTS)
  const [status, setStatus] = useState<"idle" | "syncing" | "success" | "error">("idle")
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const refresh = useCallback(() => {
    void getServerBaseUrl().then(setBaseUrl)
    void loadCounts().then(setCounts)
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const handleSync = () => {
    setStatus("syncing")
    setErrorMessage(null)
    runSync()
      .then(() => {
        setStatus("success")
        refresh()
      })
      .catch((error: unknown) => {
        setStatus("error")
        setErrorMessage(error instanceof Error ? error.message : String(error))
      })
  }

  return (
    <main className="diagnostics">
      <h1>VetDose</h1>
      <p className="disclaimer">Програма лише допомагає в розрахунках. Остаточне рішення приймає лікар.</p>

      <section>
        <h2>Синхронізація (діагностика Етапу 6)</h2>
        <p>Сервер: {baseUrl}</p>
        <button type="button" onClick={handleSync} disabled={status === "syncing"}>
          {status === "syncing" ? "Синхронізація…" : "Синхронізувати зараз"}
        </button>
        {status === "success" && <p>Синхронізацію завершено успішно.</p>}
        {status === "error" && <p>Помилка синхронізації: {errorMessage}</p>}
      </section>

      <section>
        <h2>Дані в IndexedDB</h2>
        <ul>
          <li>Види тварин: {counts.species}</li>
          <li>Діючі речовини: {counts.substances}</li>
          <li>Препарати: {counts.products}</li>
          <li>Правила дозування: {counts.doseRules}</li>
          <li>Протипоказання: {counts.contraindications}</li>
          <li>Терміни виведення: {counts.withdrawalPeriods}</li>
        </ul>
      </section>
    </main>
  )
}

export default App
