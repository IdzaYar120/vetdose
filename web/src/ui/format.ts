import { Decimal, type ScaledDecimal, scaledToFixed } from "../domain/calculator/doseCalculator"

/** Accepts weight typed with either a decimal comma or a decimal point (doc
 * UX requirement) and returns `null` for anything not a valid positive
 * number — the caller decides how to surface that (disabled button, etc.). */
export function parseWeightInput(text: string): Decimal | null {
  const normalized = text.trim().replace(",", ".")
  if (normalized === "") return null
  let value: Decimal
  try {
    value = new Decimal(normalized)
  } catch {
    return null
  }
  return value.gt(0) ? value : null
}

/**
 * Renders a {@link ScaledDecimal} (`administrationMin`/`administrationMax`)
 * preserving its intended scale, with a comma decimal: `"1.0"` -> `"1,0"`,
 * never trimmed to `"1"` — the trailing zero is exactly what tells the vet
 * this was rounded to one decimal place. Contrast with `formatNumber` (in
 * `domain/calculator/doseCalculator.ts`), which trims trailing zeros and is
 * for values that don't carry a meaningful fixed scale (`doseMin`/`doseMax`,
 * always at 4 decimal places internally).
 */
export function displayScaled(s: ScaledDecimal): string {
  return scaledToFixed(s).replace(".", ",")
}
