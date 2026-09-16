import { useEffect, useState } from "react"
import { useParams } from "react-router-dom"
import {
  CalculatorError,
  Decimal,
  calculateDose,
  formatNumber,
  type CalculationResult,
} from "../../domain/calculator/doseCalculator"
import type { DoseRule, Product, Species, WithdrawalPeriod } from "../../domain/model"
import {
  getContraindicationsBySubstance,
  getDoseRuleById,
  getProductById,
  getSpeciesById,
  getWithdrawalPeriodsByProduct,
  recordHistory,
} from "../../data/repositories"
import { WarningCard } from "../components/WarningCard"
import { displayScaled } from "../format"
import { FOOD_PRODUCT_LABELS, ROUTE_LABELS, UNIT_LABELS } from "../labels"
import "./ResultScreen.css"

interface ResultState {
  loading: boolean
  errorMessage: string | null
  species: Species | null
  product: Product | null
  doseRule: DoseRule | null
  result: CalculationResult | null
  withdrawalPeriods: WithdrawalPeriod[]
  /** Captured once when the calculation completes, not read fresh on every
   * render — `Date.now()` is impure and React may re-render this component
   * for reasons unrelated to time passing. */
  computedAt: number
}

const INITIAL_STATE: ResultState = {
  loading: true,
  errorMessage: null,
  species: null,
  product: null,
  doseRule: null,
  result: null,
  withdrawalPeriods: [],
  computedAt: 0,
}

const DAY_MS = 24 * 60 * 60 * 1000

/**
 * Route wrapper: resolves the ids from the URL against Dexie, runs the
 * calculator, records history, and delegates rendering to the pure
 * presentational components below — split out (mirroring Android's
 * `ResultScreen`/`ResultContent`/`AbsoluteContraindicationGate` split) so
 * those can be unit-tested with synthetic props, without needing a Dexie/
 * IndexedDB polyfill in the test environment.
 */
export function ResultScreen() {
  const params = useParams<{ speciesId: string; doseRuleId: string; productId: string; weightKg: string }>()
  const [state, setState] = useState<ResultState>(INITIAL_STATE)
  const [absoluteContraindicationConfirmed, setAbsoluteContraindicationConfirmed] = useState(false)
  const [explanationExpanded, setExplanationExpanded] = useState(false)

  useEffect(() => {
    const { speciesId, doseRuleId, productId, weightKg } = params
    if (
      speciesId === undefined ||
      doseRuleId === undefined ||
      productId === undefined ||
      weightKg === undefined
    ) {
      return
    }

    let cancelled = false

    void (async () => {
      const [species, doseRule, product] = await Promise.all([
        getSpeciesById(speciesId),
        getDoseRuleById(doseRuleId),
        getProductById(productId),
      ])

      if (species === null || doseRule === null || product === null) {
        if (!cancelled) {
          setState({
            ...INITIAL_STATE,
            loading: false,
            errorMessage: "Дані не знайдено. Спробуйте синхронізувати базу ще раз.",
          })
        }
        return
      }

      const [contraindications, withdrawalPeriods] = await Promise.all([
        getContraindicationsBySubstance(doseRule.substanceId),
        getWithdrawalPeriodsByProduct(product.id),
      ])
      const relevantContraindications = contraindications.filter(
        (c) => c.speciesId === null || c.speciesId === species.id,
      )
      const relevantWithdrawalPeriods = withdrawalPeriods.filter((p) => p.speciesId === species.id)

      try {
        const result = calculateDose({
          weightKg: new Decimal(weightKg),
          species: {
            isFoodProducing: species.isFoodProducing,
            typicalMinWeightKg: species.typicalMinWeightKg,
            typicalMaxWeightKg: species.typicalMaxWeightKg,
          },
          doseRule: {
            doseMin: doseRule.doseMin,
            doseMax: doseRule.doseMax,
            doseUnit: doseRule.doseUnit,
            isVerified: doseRule.isVerified,
            maxTotalDose: doseRule.maxTotalDose,
            maxTotalDoseUnit: doseRule.maxTotalDoseUnit,
          },
          product: {
            concentrationValue: product.concentrationValue,
            concentrationUnit: product.concentrationUnit,
            tabletDivisibleBy: product.tabletDivisibleBy,
          },
          contraindications: relevantContraindications.map((c) => ({
            severity: c.severity,
            messageUk: c.messageUk,
          })),
        })
        if (!cancelled) {
          setState({
            loading: false,
            errorMessage: null,
            species,
            product,
            doseRule,
            result,
            withdrawalPeriods: relevantWithdrawalPeriods,
            computedAt: Date.now(),
          })
        }
        await recordHistory(species.id, doseRule.id, product.id, new Decimal(weightKg))
      } catch (error) {
        if (!cancelled) {
          setState({
            loading: false,
            errorMessage: error instanceof CalculatorError ? error.message : String(error),
            species,
            product,
            doseRule,
            result: null,
            withdrawalPeriods: [],
            computedAt: 0,
          })
        }
      }
    })()

    return () => {
      cancelled = true
    }
    // Deliberately depends on the individual param strings, not `params`
    // itself: `useParams()` returns a new object every render, so depending
    // on it directly would re-run this effect (and re-record history) on
    // every render instead of only when the route actually changes.
  }, [params.speciesId, params.doseRuleId, params.productId, params.weightKg])

  if (state.loading) {
    return null
  }
  if (state.errorMessage !== null || state.result === null) {
    return (
      <div className="result-screen">
        <h2>Неможливо розрахувати дозу</h2>
        <p>{state.errorMessage}</p>
      </div>
    )
  }

  const { result, doseRule } = state
  const hasAbsoluteContraindication = result.warnings.some((w) => w.code === "ABSOLUTE_CONTRAINDICATION")
  const doseIsRevealed = !hasAbsoluteContraindication || absoluteContraindicationConfirmed

  if (hasAbsoluteContraindication && !doseIsRevealed) {
    return (
      <AbsoluteContraindicationGate
        result={result}
        onConfirm={() => setAbsoluteContraindicationConfirmed(true)}
      />
    )
  }

  return (
    <ResultContent
      result={result}
      doseRule={doseRule}
      withdrawalPeriods={state.withdrawalPeriods}
      computedAt={state.computedAt}
      explanationExpanded={explanationExpanded}
      onExplanationToggle={() => setExplanationExpanded((v) => !v)}
    />
  )
}

export function AbsoluteContraindicationGate({
  result,
  onConfirm,
}: {
  result: CalculationResult
  onConfirm: () => void
}) {
  return (
    <div className="result-screen">
      <div className="card result-screen__absolute-gate">
        <h2>Абсолютне протипоказання</h2>
        {result.warnings
          .filter((w) => w.code === "ABSOLUTE_CONTRAINDICATION")
          .map((w, i) => (
            <p key={i}>{w.messageUk}</p>
          ))}
      </div>
      <button type="button" className="btn btn--primary btn--block" onClick={onConfirm}>
        Я ознайомлений(а), показати дозу
      </button>
    </div>
  )
}

export function ResultContent({
  result,
  doseRule,
  withdrawalPeriods,
  computedAt,
  explanationExpanded,
  onExplanationToggle,
}: {
  result: CalculationResult
  doseRule: DoseRule | null
  withdrawalPeriods: WithdrawalPeriod[]
  computedAt: number
  explanationExpanded: boolean
  onExplanationToggle: () => void
}) {
  const range = result.administrationMin.value.eq(result.administrationMax.value)
    ? displayScaled(result.administrationMin)
    : `${displayScaled(result.administrationMin)}–${displayScaled(result.administrationMax)}`

  return (
    <div className="result-screen">
      <p className="result-screen__amount">
        {range} {UNIT_LABELS[result.administrationUnit]}
      </p>

      {result.doseMin !== null && result.doseMax !== null && result.doseAmountUnit !== null && (
        <LabeledValue
          label="Доза діючої речовини"
          value={
            result.doseMin.value.eq(result.doseMax.value)
              ? `${formatNumber(result.doseMin.value)} ${UNIT_LABELS[result.doseAmountUnit as keyof typeof UNIT_LABELS]}`
              : `${formatNumber(result.doseMin.value)}–${formatNumber(result.doseMax.value)} ${UNIT_LABELS[result.doseAmountUnit as keyof typeof UNIT_LABELS]}`
          }
        />
      )}

      {doseRule !== null && (
        <>
          <LabeledValue label="Шлях введення" value={ROUTE_LABELS[doseRule.route]} />
          {doseRule.frequency !== null && <LabeledValue label="Кратність" value={doseRule.frequency} />}
          {doseRule.duration !== null && <LabeledValue label="Тривалість" value={doseRule.duration} />}
        </>
      )}

      {result.warnings.map((warning, i) => (
        <WarningCard key={i} warning={warning} />
      ))}

      {withdrawalPeriods.length > 0 && (
        <div className="card">
          <p className="section-title">Терміни виведення</p>
          {withdrawalPeriods.map((period) => {
            const safeFrom = new Date(computedAt + period.days * DAY_MS)
            const dateStr = safeFrom.toLocaleDateString("uk-UA", {
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
            })
            return (
              <p key={period.id}>
                {FOOD_PRODUCT_LABELS[period.foodProduct]} — до {dateStr} ({period.days} дн.)
              </p>
            )
          })}
        </div>
      )}

      <div className="card">
        <button type="button" className="result-screen__explanation-toggle" onClick={onExplanationToggle}>
          <span>Покроковий розрахунок</span>
          <span aria-hidden="true">{explanationExpanded ? "▲" : "▼"}</span>
        </button>
        {explanationExpanded && (
          <ul className="result-screen__explanation-list">
            {result.explanation.map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ul>
        )}
      </div>

      {doseRule !== null && <LabeledValue label="Джерело" value={doseRule.source} />}

      <p className="disclaimer">Програма лише допомагає в розрахунках. Остаточне рішення приймає лікар.</p>
    </div>
  )
}

function LabeledValue({ label, value }: { label: string; value: string }) {
  return (
    <div className="result-screen__labeled-value">
      <span className="result-screen__label">{label}</span>
      <span className="result-screen__value">{value}</span>
    </div>
  )
}
