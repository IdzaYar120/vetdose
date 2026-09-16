import { useLiveQuery } from "dexie-react-hooks"
import { useNavigate } from "react-router-dom"
import type { CalculationHistoryEntry } from "../../domain/model"
import { formatNumber } from "../../domain/calculator/doseCalculator"
import { getProductById, getRecentHistory, getSpeciesById } from "../../data/repositories"
import { ROUTES } from "../routes"
import "./HistoryScreen.css"

interface HistoryItem {
  entry: CalculationHistoryEntry
  speciesName: string
  productName: string
}

const HISTORY_LIMIT = 50

async function loadHistoryItems(): Promise<HistoryItem[]> {
  const entries = await getRecentHistory(HISTORY_LIMIT)
  const items: HistoryItem[] = []
  for (const entry of entries) {
    const [species, product] = await Promise.all([
      getSpeciesById(entry.speciesId),
      getProductById(entry.productId),
    ])
    if (species === null || product === null) continue
    items.push({ entry, speciesName: species.nameUk, productName: product.tradeName })
  }
  return items
}

export function HistoryScreen() {
  const navigate = useNavigate()
  const items = useLiveQuery(loadHistoryItems, [], [] as HistoryItem[])

  if (items === undefined || items.length === 0) {
    return <div className="empty-state">Історія розрахунків поки порожня</div>
  }

  return (
    <div className="history-screen">
      {items.map((item) => (
        <button
          key={item.entry.id}
          type="button"
          className="card history-screen__item"
          onClick={() =>
            navigate(
              ROUTES.result(
                item.entry.speciesId,
                item.entry.doseRuleId,
                item.entry.productId,
                item.entry.weightKg.toFixed(),
              ),
            )
          }
        >
          <span className="history-screen__product">{item.productName}</span>
          <span className="history-screen__meta">
            {item.speciesName} • {formatNumber(item.entry.weightKg)} кг
          </span>
          <span className="history-screen__timestamp">
            {new Date(item.entry.timestamp).toLocaleString("uk-UA", {
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </span>
        </button>
      ))}
    </div>
  )
}
