# VetDose Web

React + TypeScript PWA client. Offline-first, same as the Android app: only
`GET /api/v1/sync` touches the network, everything else (search, dose
calculation) runs against IndexedDB (via [Dexie](https://dexie.org/)).

```bash
cd web
npm install
npm run dev          # http://localhost:5173
npm run test         # Vitest (component tests + shared/calculation_test_cases.json)
npm run test:e2e     # Playwright smoke tests (chromium + webkit) — needs the backend running
npm run typecheck    # tsc -b
npm run lint         # oxlint
npm run format:check # prettier --check
npm run build         # production build (also generates the PWA service worker)
```

The backend must be running for sync to succeed (`docker compose up --build`
from the repo root, or `uv run uvicorn app.main:app --reload` in `backend/`).
Its default CORS allows both `http://localhost:5173` (Vite dev) and
`http://localhost:8080` (the dockerized web build, below).

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
  dose_rules, contraindications, withdrawal_periods), camelCase, plus
  `settings` (server base URL, `since` sync cursor — the web equivalent of
  Android's DataStore-backed `SettingsRepository`) and two local-only tables
  added in Stage 7, `calculationHistory`/`favoriteProducts`, mirroring
  Android's Room migration.
- **`data/mappers.ts`** / **`data/entityMappers.ts`** — DTO -> entity and
  entity -> domain-model conversion, mirroring the Android data layer's
  DTO -> Room-entity -> domain-model split (`domain/model.ts`; decimal
  fields become parsed `Decimal` only at that last step).
- **`data/repositories.ts`** — one function group per entity (mirrors
  Android's `data/repository/*.kt`), consumed reactively from screens via
  `dexie-react-hooks`'s `useLiveQuery` instead of a hand-rolled `Flow`
  equivalent.
- **`data/sync.ts`** — `runSync()`: fetches `/sync`, applies it to Dexie in
  one transaction (upserts active rows, physically deletes rows the server
  marked `is_deleted` — the app never shows soft-deleted rows, so there's no
  reason to keep them locally, same policy as Android's `SyncRepository`),
  then advances the `since` cursor.

## Screens (Stage 7)

- **Розрахунок** (`ui/screens/CalculateScreen.tsx`, start screen) — species
  chips with emoji icons, weight field (accepts comma or period), product
  picker. Product search (`ProductSearchScreen.tsx`) is rendered as an
  in-component full-screen overlay, **not** a router route — routing away to
  `/search/...` and back would unmount/remount `CalculateScreen` and discard
  its in-progress species/weight selection, since React Router (unlike
  Android's NavHost) doesn't retain a screen's state just because another
  screen was pushed on top. See that file's doc comment.
- **Результат** (`ResultScreen.tsx`) — split into a data-loading route
  wrapper plus pure `ResultContent`/`AbsoluteContraindicationGate`
  components (mirroring Android's `ResultScreen`/`ResultContent` split),
  specifically so those can be unit-tested with synthetic props without a
  Dexie/IndexedDB polyfill in the test environment. Absolute
  contraindication hides the dose behind a red confirmation gate, same as
  Android.
- **Історія** / **Налаштування** — `calculationHistory` (inputs only, result
  always recomputed on display) and server address + sync-now, mirroring
  Android's Stage 5 screens.

## iOS PWA quirks (Stage 7)

- **`ui/IosInstallBanner.tsx`** — iOS Safari has no `beforeinstallprompt`
  event, so there's no native "Install app" banner; this shows explicit
  Share -> "На екран «Домівка»" instructions, detected via UA/platform
  sniffing (iPadOS 13+ reports as `MacIntel` with touch support, not
  `iPad`), dismissible and remembered per browser via `localStorage`.
- **`data/useNeedsSync.ts`** — distinguishes "never synced" from "synced
  before, but the tables are empty now" (iOS Safari can evict IndexedDB
  under storage pressure) so the empty state can tell the vet what actually
  happened instead of always saying "first sync needed".

Verified against a live backend in a real browser (Playwright, Chromium +
WebKit — the engine iOS Safari actually uses): full sync, the
species/product/weight round trip through the search overlay, the absolute-
contraindication gate, and (Stage 6) that data survives a reload and a
second sync doesn't duplicate rows.

## PWA / Docker

`vite-plugin-pwa` (`vite.config.ts`) generates the manifest and service
worker on `npm run build`. Icons are in `public/icons/`.

`Dockerfile` is a two-stage build (Node -> static `dist/` served by nginx,
`docker/nginx.conf`); `docker compose up --build` from the repo root also
brings this up at `http://localhost:8080`, alongside the backend and
Postgres — nginx falls back unmatched paths to `index.html` so react-router
routes like `/result/...` work on a direct hit, not just client-side
navigation.
