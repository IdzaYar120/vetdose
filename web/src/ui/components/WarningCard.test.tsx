import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import type { CalculationWarning } from "../../domain/calculator/doseCalculator"
import { WarningCard } from "./WarningCard"

function warning(code: string, messageUk: string): CalculationWarning {
  return { code, messageUk }
}

describe("WarningCard", () => {
  it("renders an absolute contraindication in red", () => {
    render(<WarningCard warning={warning("ABSOLUTE_CONTRAINDICATION", "TEST: протипоказано вагітним")} />)
    expect(screen.getByText("TEST: протипоказано вагітним")).toBeInTheDocument()
    expect(screen.getByRole("alert")).toHaveClass("warning-card--absolute")
  })

  it("renders a caution warning in amber", () => {
    render(<WarningCard warning={warning("CAUTION", "TEST: обережно при нирковій недостатності")} />)
    expect(screen.getByText("TEST: обережно при нирковій недостатності")).toBeInTheDocument()
    expect(screen.getByRole("alert")).toHaveClass("warning-card--caution")
  })

  it("renders an unverified-rule warning in amber", () => {
    render(
      <WarningCard
        warning={warning("RULE_NOT_VERIFIED", "Це правило дозування ще не підтверджене лікарем.")}
      />,
    )
    expect(screen.getByRole("alert")).toHaveClass("warning-card--caution")
  })

  it("renders a max-dose-capped warning in amber", () => {
    render(<WarningCard warning={warning("MAX_DOSE_CAPPED", "Розрахована доза була обмежена.")} />)
    expect(screen.getByRole("alert")).toHaveClass("warning-card--caution")
  })
})
