/**
 * Route paths and arg-builders — mirrors Android's `VetDoseDestinations.kt`.
 * Product search is an in-component overlay, not a route — see
 * `ProductSearchScreen.tsx`'s doc comment for why.
 */
export const ROUTES = {
  calculate: "/",
  resultPattern: "/result/:speciesId/:doseRuleId/:productId/:weightKg",
  result: (speciesId: string, doseRuleId: string, productId: string, weightKg: string) =>
    `/result/${encodeURIComponent(speciesId)}/${encodeURIComponent(doseRuleId)}/` +
    `${encodeURIComponent(productId)}/${encodeURIComponent(weightKg)}`,
  history: "/history",
  settings: "/settings",
}
