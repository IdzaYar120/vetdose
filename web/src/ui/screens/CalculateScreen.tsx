import { useLiveQuery } from "dexie-react-hooks"
import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { formatNumber } from "../../domain/calculator/doseCalculator"
import type { Product } from "../../domain/model"
import { findDoseRuleForSubstanceAndSpecies, getAllSpecies } from "../../data/repositories"
import { useNeedsSync } from "../../data/useNeedsSync"
import { SyncNeeded } from "../components/SyncNeeded"
import { parseWeightInput } from "../format"
import { speciesEmoji } from "../labels"
import { ROUTES } from "../routes"
import { ProductSearchScreen } from "./ProductSearchScreen"
import "./CalculateScreen.css"

export function CalculateScreen() {
  const navigate = useNavigate()
  const { loaded, needsSync, storageWasCleared } = useNeedsSync()
  const speciesList = useLiveQuery(getAllSpecies, [], [])

  const [selectedSpeciesId, setSelectedSpeciesId] = useState<string | null>(null)
  const [weightInput, setWeightInput] = useState("")
  const [weightError, setWeightError] = useState(false)
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [pendingWeightConfirmation, setPendingWeightConfirmation] = useState<ReturnType<
    typeof parseWeightInput
  > | null>(null)
  const [noDoseRuleError, setNoDoseRuleError] = useState(false)

  const selectedSpecies = speciesList?.find((s) => s.id === selectedSpeciesId) ?? null

  if (!loaded) {
    return null
  }
  if (needsSync) {
    return <SyncNeeded storageWasCleared={storageWasCleared} />
  }

  const handleSpeciesSelected = (speciesId: string) => {
    setSelectedSpeciesId(speciesId)
    setSelectedProduct(null)
    setNoDoseRuleError(false)
  }

  const proceedToResult = async (weightKg: NonNullable<ReturnType<typeof parseWeightInput>>) => {
    if (selectedSpecies === null || selectedProduct === null) return
    const doseRule = await findDoseRuleForSubstanceAndSpecies(selectedProduct.substanceId, selectedSpecies.id)
    if (doseRule === null) {
      setNoDoseRuleError(true)
      return
    }
    navigate(ROUTES.result(selectedSpecies.id, doseRule.id, selectedProduct.id, weightKg.toFixed()))
  }

  const handleCalculateClicked = () => {
    if (selectedSpecies === null || selectedProduct === null) return
    const weight = parseWeightInput(weightInput)
    if (weight === null) {
      setWeightError(true)
      return
    }
    const outOfRange =
      weight.lt(selectedSpecies.typicalMinWeightKg) || weight.gt(selectedSpecies.typicalMaxWeightKg)
    if (outOfRange) {
      setPendingWeightConfirmation(weight)
      return
    }
    void proceedToResult(weight)
  }

  return (
    <div className="calculate-screen">
      <section>
        <p className="section-title">Оберіть вид тварини</p>
        <div className="calculate-screen__species-row">
          {(speciesList ?? []).map((species) => (
            <button
              key={species.id}
              type="button"
              className={`chip${selectedSpeciesId === species.id ? " chip--selected" : ""}`}
              onClick={() => {
                handleSpeciesSelected(species.id)
              }}
            >
              {speciesEmoji(species.code)} {species.nameUk}
            </button>
          ))}
        </div>
      </section>

      <section className={`field${weightError ? " field--error" : ""}`}>
        <label htmlFor="weight-input">Вага, кг</label>
        <input
          id="weight-input"
          type="text"
          inputMode="decimal"
          placeholder="Наприклад: 5,2"
          value={weightInput}
          onChange={(e) => {
            setWeightInput(e.target.value)
            setWeightError(false)
            setNoDoseRuleError(false)
          }}
        />
        {weightError && <span className="field__error">Введіть вагу більшу за 0</span>}
      </section>

      <section>
        <p className="section-title">Препарат</p>
        {selectedProduct === null ? (
          <button
            type="button"
            className="btn btn--outline btn--block"
            disabled={selectedSpecies === null}
            onClick={() => setSearchOpen(true)}
          >
            {selectedSpecies === null ? "Спершу оберіть вид тварини" : "Обрати препарат"}
          </button>
        ) : (
          <div className="card calculate-screen__product-card">
            <span>{selectedProduct.tradeName}</span>
            <button type="button" className="btn btn--outline" onClick={() => setSearchOpen(true)}>
              Змінити
            </button>
          </div>
        )}

        {noDoseRuleError && (
          <div className="card calculate-screen__warning">
            Для цього препарату немає правила дозування для обраного виду тварини
          </div>
        )}
      </section>

      <button
        type="button"
        className="btn btn--primary btn--block"
        disabled={selectedSpecies === null || selectedProduct === null}
        onClick={handleCalculateClicked}
      >
        Розрахувати
      </button>

      {pendingWeightConfirmation !== null && selectedSpecies !== null && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>Перевірте вагу</h2>
            <p>
              Вага {formatNumber(pendingWeightConfirmation)} кг нетипова для виду «{selectedSpecies.nameUk}»
              (типово {formatNumber(selectedSpecies.typicalMinWeightKg)}–
              {formatNumber(selectedSpecies.typicalMaxWeightKg)} кг). Продовжити розрахунок?
            </p>
            <div className="modal__actions">
              <button
                type="button"
                className="btn btn--outline"
                onClick={() => setPendingWeightConfirmation(null)}
              >
                Скасувати
              </button>
              <button
                type="button"
                className="btn btn--primary"
                onClick={() => {
                  const weight = pendingWeightConfirmation
                  setPendingWeightConfirmation(null)
                  void proceedToResult(weight)
                }}
              >
                Так, продовжити
              </button>
            </div>
          </div>
        </div>
      )}

      {searchOpen && selectedSpecies !== null && (
        <ProductSearchScreen
          speciesId={selectedSpecies.id}
          onClose={() => setSearchOpen(false)}
          onProductChosen={(product) => {
            setSelectedProduct(product)
            setNoDoseRuleError(false)
            setSearchOpen(false)
          }}
        />
      )}
    </div>
  )
}
