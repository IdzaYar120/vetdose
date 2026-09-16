import type { CalculationWarning } from "../../domain/calculator/doseCalculator"
import "./WarningCard.css"

type WarningStyle = "absolute" | "caution"

function styleFor(code: string): WarningStyle {
  return code === "ABSOLUTE_CONTRAINDICATION" ? "absolute" : "caution"
}

/**
 * Renders one calculator warning as a colored card — red for an absolute
 * contraindication, amber for everything else (caution, unverified rule,
 * capped dose, food-producing-animal reminder, out-of-range weight). The
 * text itself (`warning.messageUk`) already comes from doseCalculator in
 * Ukrainian; this component only supplies color/icon.
 */
export function WarningCard({ warning }: { warning: CalculationWarning }) {
  const style = styleFor(warning.code)
  return (
    <div className={`warning-card warning-card--${style}`} role="alert">
      <span className="warning-card__icon" aria-hidden="true">
        {style === "absolute" ? "⛔" : "⚠️"}
      </span>
      <p>{warning.messageUk}</p>
    </div>
  )
}
