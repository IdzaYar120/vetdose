import { defineConfig, devices } from "@playwright/test"

// Smoke tests against a real browser (Chromium + WebKit — WebKit is what
// iOS Safari actually uses, so this is the closest CI can get to iOS
// coverage without a real device). Requires the backend to be reachable at
// http://localhost:8000 (`docker compose up -d` from the repo root); tests
// skip themselves with a clear message if it isn't, rather than failing
// opaquely on a timeout.
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: "list",
  use: {
    baseURL: "http://localhost:5173",
    trace: "on-first-retry",
  },
  webServer: {
    command: "npm run dev",
    url: "http://localhost:5173",
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "webkit-iphone", use: { ...devices["iPhone 14"], browserName: "webkit" } },
  ],
})
