import { useCallback, useState } from "react"
import { runSync } from "./sync"

export type SyncOutcome = "success" | "error" | null

export function useSyncAction() {
  const [isSyncing, setIsSyncing] = useState(false)
  const [outcome, setOutcome] = useState<SyncOutcome>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const sync = useCallback((forceFullSync: boolean = false) => {
    setIsSyncing(true)
    setOutcome(null)
    setErrorMessage(null)
    runSync(forceFullSync)
      .then(() => {
        setOutcome("success")
      })
      .catch((error: unknown) => {
        setOutcome("error")
        setErrorMessage(error instanceof Error ? error.message : String(error))
      })
      .finally(() => {
        setIsSyncing(false)
      })
  }, [])

  return { sync, isSyncing, outcome, errorMessage }
}
