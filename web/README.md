# VetDose Web

React + TypeScript PWA client. Offline-first, same as the Android app: only
`GET /api/v1/sync` touches the network, everything else (search, dose
calculation) runs against IndexedDB (via [Dexie](https://dexie.org/)).

```bash
cd web
npm install
npm run dev          # http://localhost:5173
npm run test         # Vitest — includes shared/calculation_test_cases.json
npm run typecheck    # tsc -b
npm run lint         # oxlint
npm run format:check # prettier --check
npm run build         # production build (also generates the PWA service worker)
```

The backend must be running for sync to succeed (`docker compose up --build`
from the repo root, or `uv run uvicorn app.main:app --reload` in `backend/`).
Its default CORS origin is `http://localhost:5173`, matching Vite's dev port.

## Calculator (Stage 6)

`src/domain/calculator/doseCalculator.ts` is a third independent port of
`backend/app/services/calculator.py` (the second is Android's
`DoseCalculator.kt`). All three are checked against
[`shared/calculation_test_cases.json`](../shared/calculation_test_cases.json)
so the math stays identical across server, Android and web.

Uses [`decimal.js`](https://mikemcl.github.io/decimal.js/) — never native
`number` — for all dose math. Unlike Python's `Decimal` or Java's
`BigDecimal`, `decimal.js` values don't carry a fixed "scale": rounding a
value to N decimal places doesn't keep trailing zeros the way `BigDecimal`'s
type system does. Wherever the algorithm relies on that scale to decide how a
result should be *displayed* (`administration_min`/`administration_max`,
`dose_min`/`dose_max`), this port carries the decimal-place count alongside
the value explicitly as a `ScaledDecimal { value, dp }`, rendered with
`scaledToFixed()` — see the doc comment at the top of `doseCalculator.ts` for
the full reasoning and worked examples.

## Data layer (Stage 6)

- **`data/remote/dto.ts`** — wire types for `GET /api/v1/sync`, snake_case,
  matching the backend's Pydantic `*Read` schemas field-for-field. Decimal
  fields are strings on the wire (`DecimalStr` in
  `backend/app/schemas/common.py`) — parsed into `Decimal`, never `Number()`.
- **`data/local/schema.ts` + `data/local/db.ts`** — the Dexie (IndexedDB)
  schema: one table per synced entity (species, substances, products,
  dose_rules, contraindications, withdrawal_periods), camelCase, plus a
  small `settings` key/value table for the server base URL and the `since`
  sync cursor (the web equivalent of Android's DataStore-backed
  `SettingsRepository`).
- **`data/mappers.ts`** — DTO -> entity conversion, mirroring the Android
  data layer's DTO -> Room-entity split.
- **`data/remote/client.ts`** — the one API call the app makes (`fetchSync`).
- **`data/sync.ts`** — `runSync()`: fetches `/sync`, applies it to Dexie in
  one transaction (upserts active rows, physically deletes rows the server
  marked `is_deleted` — the app never shows soft-deleted rows, so there's no
  reason to keep them locally, same policy as Android's `SyncRepository`),
  then advances the `since` cursor.

Verified against a live backend in a real browser (Playwright), not just
Vitest: a full sync populates all six tables, the data survives a page
reload, and a second (incremental) sync doesn't duplicate rows.

Stage 6 has no screens yet (`App.tsx` is a minimal diagnostic page that
drives `runSync()` and shows table counts, to exercise the real data layer
end to end) — those, plus `calculation_history`/`favorite_product` (local
only, same as Android), are Stage 7.

## PWA

`vite-plugin-pwa` (`vite.config.ts`) generates the manifest and service
worker on `npm run build`. Icons are in `public/icons/`. iOS-specific PWA
quirks (Safari's limited install UX, "Add to Home Screen" instructions,
recovering from cleared storage) are Stage 7 work.
