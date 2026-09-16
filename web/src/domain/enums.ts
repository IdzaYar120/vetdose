/** String enums shared with the backend's `app/models/enums.py` (and the
 * Android `enum class`es in `domain/calculator/DoseCalculator.kt` /
 * `domain/model/`). Kept separate from `doseCalculator.ts`'s own unit enums
 * because these describe API/storage shapes, not the calculator's inputs. */

export const ProductForm = {
  INJECTION_SOLUTION: "injection_solution",
  ORAL_SOLUTION: "oral_solution",
  TABLET: "tablet",
  POWDER: "powder",
  SUSPENSION: "suspension",
  OTHER: "other",
} as const
export type ProductForm = (typeof ProductForm)[keyof typeof ProductForm]

export const Route = {
  IV: "iv",
  IM: "im",
  SC: "sc",
  PO: "po",
  TOPICAL: "topical",
  OTHER: "other",
} as const
export type Route = (typeof Route)[keyof typeof Route]

export const FoodProduct = {
  MEAT: "meat",
  MILK: "milk",
  EGGS: "eggs",
  HONEY: "honey",
} as const
export type FoodProduct = (typeof FoodProduct)[keyof typeof FoodProduct]
