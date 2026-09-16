import { expect, test } from "@playwright/test"

const BACKEND_HEALTH_URL = "http://localhost:8000/api/v1/health"

test.beforeAll(async () => {
  try {
    const response = await fetch(BACKEND_HEALTH_URL, { signal: AbortSignal.timeout(2000) })
    test.skip(
      !response.ok,
      `Backend not healthy at ${BACKEND_HEALTH_URL} — run "docker compose up -d" first.`,
    )
  } catch {
    test.skip(true, `Backend not reachable at ${BACKEND_HEALTH_URL} — run "docker compose up -d" first.`)
  }
})

test("first launch shows the sync-needed empty state", async ({ page }) => {
  await page.goto("/")
  await expect(page.getByText("Потрібна перша синхронізація")).toBeVisible()
})

test("sync populates the local database", async ({ page }) => {
  await page.goto("/settings")
  await page.getByRole("button", { name: "Синхронізувати зараз" }).click()
  await expect(page.getByText("Синхронізацію завершено успішно")).toBeVisible({ timeout: 15000 })

  await page.getByText("Розрахунок").click()
  await expect(page.getByText("Оберіть вид тварини", { exact: true })).toBeVisible()
  await expect(page.locator(".calculate-screen__species-row button").first()).toBeVisible()
})

test("full flow: pick species/product, calculate, see a result", async ({ page }) => {
  await page.goto("/settings")
  await page.getByRole("button", { name: "Синхронізувати зараз" }).click()
  await expect(page.getByText("Синхронізацію завершено успішно")).toBeVisible({ timeout: 15000 })

  await page.getByText("Розрахунок").click()
  await page.locator(".calculate-screen__species-row button").first().click()
  await page.fill("#weight-input", "5,2")
  await page.getByRole("button", { name: "Обрати препарат" }).click()

  await expect(page.getByText("Пошук препарату")).toBeVisible()
  await page.locator(".search-screen__row-main").first().click()

  // Chosen product must survive the round trip back to the calculate form.
  await expect(page.getByText("Оберіть вид тварини")).toBeVisible()
  await expect(page.locator(".calculate-screen__product-card")).toBeVisible()

  await page.getByRole("button", { name: "Розрахувати" }).click()
  const confirmDialog = page.getByText("Перевірте вагу")
  if (await confirmDialog.isVisible().catch(() => false)) {
    await page.getByRole("button", { name: "Так, продовжити" }).click()
  }

  await expect(page).toHaveURL(/\/result\//)
  await expect(page.getByText("Програма лише допомагає в розрахунках.")).toBeVisible()
})

test("absolute contraindication hides the dose until the vet confirms", async ({ page }) => {
  await page.goto("/settings")
  await page.getByRole("button", { name: "Синхронізувати зараз" }).click()
  await expect(page.getByText("Синхронізацію завершено успішно")).toBeVisible({ timeout: 15000 })

  await page.getByText("Розрахунок").click()
  await page.locator(".calculate-screen__species-row button", { hasText: "Собака" }).click()
  await page.fill("#weight-input", "20")
  await page.getByRole("button", { name: "Обрати препарат" }).click()
  await page.fill('input[type="search"]', "meloxicam")
  await page.locator(".search-screen__row-main").first().click()

  await page.getByRole("button", { name: "Розрахувати" }).click()
  const confirmDialog = page.getByText("Перевірте вагу")
  if (await confirmDialog.isVisible().catch(() => false)) {
    await page.getByRole("button", { name: "Так, продовжити" }).click()
  }

  await expect(page).toHaveURL(/\/result\//)
  await expect(page.getByText("Абсолютне протипоказання")).toBeVisible()
  await expect(page.locator(".result-screen__amount")).not.toBeVisible()

  await page.getByRole("button", { name: "Я ознайомлений(а), показати дозу" }).click()
  await expect(page.locator(".result-screen__amount")).toBeVisible()
})
