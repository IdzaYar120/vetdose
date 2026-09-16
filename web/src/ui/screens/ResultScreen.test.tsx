import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { ConcentrationUnit, Decimal, DoseUnit, calculateDose } from "../../domain/calculator/doseCalculator"
import type { DoseRule, WithdrawalPeriod } from "../../domain/model"
import { FoodProduct, Route } from "../../domain/enums"
import { AbsoluteContraindicationGate, ResultContent } from "./ResultScreen"

// A representative mg/kg + ml result: 5.2 kg x 10 mg/kg / 50 mg/ml = 1.04 ml,
// rounded for display to "1.0 ml" — the same rounding-preserves-scale case
// covered in doseCalculator's own shared test suite.
const BASE_RESULT = calculateDose({
  weightKg: new Decimal("5.2"),
  species: {
    isFoodProducing: false,
    typicalMinWeightKg: new Decimal(1),
    typicalMaxWeightKg: new Decimal(100),
  },
  doseRule: {
    doseMin: new Decimal(10),
    doseMax: new Decimal(10),
    doseUnit: DoseUnit.MG_PER_KG,
    isVerified: true,
    maxTotalDose: null,
    maxTotalDoseUnit: null,
  },
  product: {
    concentrationValue: new Decimal(50),
    concentrationUnit: ConcentrationUnit.MG_PER_ML,
    tabletDivisibleBy: null,
  },
  contraindications: [{ severity: "caution", messageUk: "TEST: обережно при нирковій недостатності" }],
})

const DOSE_RULE: DoseRule = {
  id: "dose-rule-1",
  substanceId: "substance-1",
  speciesId: "species-1",
  route: Route.IM,
  indication: null,
  doseMin: new Decimal(10),
  doseMax: new Decimal(10),
  doseUnit: DoseUnit.MG_PER_KG,
  maxTotalDose: null,
  maxTotalDoseUnit: null,
  frequency: "кожні 24 год",
  duration: null,
  notes: null,
  source: "TEST DATA — NOT FOR CLINICAL USE",
  isVerified: true,
}

const WITHDRAWAL_PERIOD: WithdrawalPeriod = {
  id: "withdrawal-1",
  productId: "product-1",
  speciesId: "species-1",
  route: Route.IM,
  foodProduct: FoodProduct.MEAT,
  days: 10,
  hours: null,
  source: "TEST DATA — NOT FOR CLINICAL USE",
}

describe("ResultContent", () => {
  it("shows the big administration amount with its rounded display scale, not trimmed", () => {
    render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={Date.parse("2026-01-01T00:00:00Z")}
        explanationExpanded={false}
        onExplanationToggle={() => undefined}
      />,
    )
    expect(screen.getByText("1,0 мл")).toBeInTheDocument()
  })

  it("shows the dose-amount range", () => {
    render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={0}
        explanationExpanded={false}
        onExplanationToggle={() => undefined}
      />,
    )
    expect(screen.getByText("52 мг")).toBeInTheDocument()
  })

  it("shows the route label", () => {
    render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={0}
        explanationExpanded={false}
        onExplanationToggle={() => undefined}
      />,
    )
    expect(screen.getByText("внутрішньом'язово")).toBeInTheDocument()
  })

  it("renders a warning card for each calculator warning", () => {
    render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={0}
        explanationExpanded={false}
        onExplanationToggle={() => undefined}
      />,
    )
    expect(screen.getByText("TEST: обережно при нирковій недостатності")).toBeInTheDocument()
  })

  it("shows a withdrawal period with a date computed from computedAt + days", () => {
    render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[WITHDRAWAL_PERIOD]}
        computedAt={Date.parse("2026-01-01T00:00:00Z")}
        explanationExpanded={false}
        onExplanationToggle={() => undefined}
      />,
    )
    // 2026-01-01 + 10 days = 2026-01-11
    expect(screen.getByText(/до 11\.01\.2026 \(10 дн\.\)/)).toBeInTheDocument()
  })

  it("hides the explanation steps until toggled, then shows them", async () => {
    const user = userEvent.setup()
    const onToggle = vi.fn()
    const { rerender } = render(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={0}
        explanationExpanded={false}
        onExplanationToggle={onToggle}
      />,
    )
    expect(screen.queryByText(/5,2 кг × 10 мг\/кг/)).not.toBeInTheDocument()

    await user.click(screen.getByText("Покроковий розрахунок"))
    expect(onToggle).toHaveBeenCalledOnce()

    rerender(
      <ResultContent
        result={BASE_RESULT}
        doseRule={DOSE_RULE}
        withdrawalPeriods={[]}
        computedAt={0}
        explanationExpanded={true}
        onExplanationToggle={onToggle}
      />,
    )
    expect(screen.getByText(/5,2 кг × 10 мг\/кг/)).toBeInTheDocument()
  })
})

describe("AbsoluteContraindicationGate", () => {
  const absoluteResult = calculateDose({
    weightKg: new Decimal(10),
    species: {
      isFoodProducing: false,
      typicalMinWeightKg: new Decimal(1),
      typicalMaxWeightKg: new Decimal(100),
    },
    doseRule: {
      doseMin: new Decimal(10),
      doseMax: new Decimal(10),
      doseUnit: DoseUnit.MG_PER_KG,
      isVerified: true,
      maxTotalDose: null,
      maxTotalDoseUnit: null,
    },
    product: {
      concentrationValue: new Decimal(50),
      concentrationUnit: ConcentrationUnit.MG_PER_ML,
      tabletDivisibleBy: null,
    },
    contraindications: [{ severity: "absolute", messageUk: "TEST: протипоказано вагітним тваринам" }],
  })

  it("hides the dose and shows the contraindication message until confirmed", () => {
    const onConfirm = vi.fn()
    render(<AbsoluteContraindicationGate result={absoluteResult} onConfirm={onConfirm} />)

    expect(screen.getByText("TEST: протипоказано вагітним тваринам")).toBeInTheDocument()
    expect(document.querySelector(".result-screen__amount")).not.toBeInTheDocument()
  })

  it("calls onConfirm when the vet acknowledges the warning", async () => {
    const user = userEvent.setup()
    const onConfirm = vi.fn()
    render(<AbsoluteContraindicationGate result={absoluteResult} onConfirm={onConfirm} />)

    await user.click(screen.getByText("Я ознайомлений(а), показати дозу"))
    expect(onConfirm).toHaveBeenCalledOnce()
  })
})
