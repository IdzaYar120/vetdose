import type { AdministrationUnit } from "../domain/calculator/doseCalculator"
import { FoodProduct, Route } from "../domain/enums"

export const ROUTE_LABELS: Record<Route, string> = {
  [Route.IV]: "внутрішньовенно",
  [Route.IM]: "внутрішньом'язово",
  [Route.SC]: "підшкірно",
  [Route.PO]: "перорально",
  [Route.TOPICAL]: "накожно",
  [Route.OTHER]: "інше",
}

export const FOOD_PRODUCT_LABELS: Record<FoodProduct, string> = {
  [FoodProduct.MEAT]: "м'ясо",
  [FoodProduct.MILK]: "молоко",
  [FoodProduct.EGGS]: "яйця",
  [FoodProduct.HONEY]: "мед",
}

/** Maps DoseCalculator's machine-readable unit codes to their Ukrainian
 * display label ("mg"/"mcg"/"iu" are the dose-amount units; "ml"/"g"/
 * "tablets" are administration units). */
export const UNIT_LABELS: Record<AdministrationUnit | "mg" | "mcg" | "iu", string> = {
  ml: "мл",
  tablets: "таблеток",
  g: "г",
  mg: "мг",
  mcg: "мкг",
  iu: "МО",
}

const SPECIES_EMOJI: Record<string, string> = {
  cat: "🐱",
  dog: "🐶",
  cattle: "🐄",
  pig: "🐷",
  horse: "🐴",
  sheep: "🐑",
  goat: "🐐",
  poultry: "🐔",
  rabbit: "🐰",
}

export function speciesEmoji(code: string): string {
  return SPECIES_EMOJI[code] ?? "🐾"
}
