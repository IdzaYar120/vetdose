import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { IosInstallBanner } from "./IosInstallBanner"

function mockNavigator(overrides: { userAgent?: string; platform?: string; maxTouchPoints?: number }) {
  Object.defineProperty(window.navigator, "userAgent", {
    value: overrides.userAgent ?? "",
    configurable: true,
  })
  Object.defineProperty(window.navigator, "platform", { value: overrides.platform ?? "", configurable: true })
  Object.defineProperty(window.navigator, "maxTouchPoints", {
    value: overrides.maxTouchPoints ?? 0,
    configurable: true,
  })
}

function mockMatchMedia(matches: boolean) {
  window.matchMedia = ((query: string) => ({
    matches,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  })) as typeof window.matchMedia
}

const IPHONE_UA =
  "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
const ANDROID_UA =
  "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

describe("IosInstallBanner", () => {
  beforeEach(() => {
    window.localStorage.clear()
    mockMatchMedia(false)
  })

  afterEach(() => {
    const nav = window.navigator as Navigator & { standalone?: boolean }
    delete nav.standalone
  })

  it("shows install instructions on iOS Safari, not already installed", () => {
    mockNavigator({ userAgent: IPHONE_UA, platform: "iPhone" })
    render(<IosInstallBanner />)
    expect(screen.getByRole("note")).toHaveTextContent("На екран «Домівка»")
  })

  it("shows on iPadOS, which reports as MacIntel with touch support", () => {
    mockNavigator({
      userAgent: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6)",
      platform: "MacIntel",
      maxTouchPoints: 5,
    })
    render(<IosInstallBanner />)
    expect(screen.getByRole("note")).toBeInTheDocument()
  })

  it("does not show on Android", () => {
    mockNavigator({ userAgent: ANDROID_UA, platform: "Linux armv8l" })
    render(<IosInstallBanner />)
    expect(screen.queryByRole("note")).not.toBeInTheDocument()
  })

  it("does not show once already running standalone (installed)", () => {
    mockNavigator({ userAgent: IPHONE_UA, platform: "iPhone" })
    const nav = window.navigator as Navigator & { standalone?: boolean }
    nav.standalone = true
    render(<IosInstallBanner />)
    expect(screen.queryByRole("note")).not.toBeInTheDocument()
  })

  it("stays dismissed after the user closes it", async () => {
    const user = userEvent.setup()
    mockNavigator({ userAgent: IPHONE_UA, platform: "iPhone" })
    const { unmount } = render(<IosInstallBanner />)

    await user.click(screen.getByRole("button", { name: "Закрити" }))
    expect(screen.queryByRole("note")).not.toBeInTheDocument()

    unmount()
    render(<IosInstallBanner />)
    expect(screen.queryByRole("note")).not.toBeInTheDocument()
  })
})
