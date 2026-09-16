import { useCallback, useState } from "react"
import { runSync } from "./sync"

export type SyncOutcome = "success" | "error" | null

/** Drives one `runSync()` call and exposes its in-flight/outcome state to a
 * component — the web equivalent of Android watching `SyncWorker`'s
 * `WorkInfo` through `WorkManager`, simplified since there's no persistent
 * background-work system here: the sync either finishes while the component
 * watching it is still mounted, or (having already written to Dexie) its
 * result is simply picked up by `useLiveQuery` elsewhere on next render. */
export function useSyncAction() {
  const [isSyncing, setIsSyncing] = useState(false)
  const [outcome, setOutcome] = useState<SyncOutcome>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const sync = useCallback(() => {
    setIsSyncing(true)
    setOutcome(null)
    setErrorMessage(null)
    runSync()
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
