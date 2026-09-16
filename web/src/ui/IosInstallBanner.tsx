import { useState } from "react"
import "./IosInstallBanner.css"

const DISMISSED_KEY = "iosInstallBannerDismissed"

/** iOS Safari has no `beforeinstallprompt` event (Chrome's native "Install
 * app" banner doesn't exist there) — the only way to add a PWA to the home
 * screen is Share -> "На екран «Домівка»", and users won't discover that on
 * their own, so this shows it explicitly. Detects "running on iOS, not
 * already installed", and is dismissible (remembered per browser via
 * `localStorage`, not per Claude-artifact rules — this is a real app running
 * in the user's own browser). */
function isIos(): boolean {
  const ua = window.navigator.userAgent
  const isIPhoneOrIPod = /iPhone|iPod/.test(ua)
  // iPadOS 13+ reports as "MacIntel" with touch support, not "iPad".
  const isIPad = window.navigator.platform === "MacIntel" && window.navigator.maxTouchPoints > 1
  return isIPhoneOrIPod || isIPad
}

function isStandalone(): boolean {
  const nav = window.navigator as Navigator & { standalone?: boolean }
  return nav.standalone === true || window.matchMedia("(display-mode: standalone)").matches
}

function wasDismissed(): boolean {
  try {
    return window.localStorage.getItem(DISMISSED_KEY) === "true"
  } catch {
    return false
  }
}

function dismiss(): void {
  try {
    window.localStorage.setItem(DISMISSED_KEY, "true")
  } catch {
    // Private browsing / storage disabled — nothing to persist, the banner
    // just reappears next visit, which is a fine fallback.
  }
}

export function IosInstallBanner() {
  const [dismissed, setDismissed] = useState(wasDismissed)

  if (dismissed || !isIos() || isStandalone()) {
    return null
  }

  return (
    <div className="ios-install-banner" role="note">
      <p>
        Щоб встановити VetDose на iPhone/iPad: натисніть «Поділитися» (<span aria-hidden="true">⬆️</span>)
        внизу екрана, потім «На екран «Домівка»».
      </p>
      <button
        type="button"
        className="ios-install-banner__close"
        onClick={() => {
          dismiss()
          setDismissed(true)
        }}
        aria-label="Закрити"
      >
        ×
      </button>
    </div>
  )
}
