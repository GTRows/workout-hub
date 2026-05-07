# Phase 21 Plan 01 - Frontend Audit

Source plan: `.planning/phases/21-frontend-audit/21-01-PLAN.md`
Audit date: 2026-05-07
Scope: read-only inventory of `frontend/src/` against ProjectBrief Phases 4-6 and v0.5 ROADMAP outline (Phases 22-30). No frontend source files modified.

Conventions:
- Path citations use forward slashes relative to repo root.
- "static: N" = number of `it(` / `test(` calls counted statically.
- Source-of-truth tags: `pre-gsd` (shipped before GSD), `roadmap` (in v0.5 outline 22-30), `v0.4` (added during v0.4 backend work).

---

## Section 1 - App Router Tree

```
frontend/src/middleware.ts                               middleware            55 lines    (no i18n)
frontend/src/app/layout.tsx                              root layout (RSC)     46 lines    next-intl getMessages
frontend/src/app/page.tsx                                page (RSC)             8 lines    (none, static text)
frontend/src/app/globals.css                             style                  -          n/a
frontend/src/app/manifest.test.ts                        test                   -          n/a (manifest unit test)
frontend/src/app/(auth)/
  layout.tsx                                             layout (RSC)           9 lines    (none)
  login/page.tsx                                         page "use client"    103 lines    auth
  login/page.test.tsx                                    test                   -          n/a
frontend/src/app/(app)/
  layout.tsx                                             layout (RSC)          15 lines    (none, mounts nav)
  dashboard/page.tsx                                     page "use client"    133 lines    dashboard
  dashboard/page.test.tsx                                test                   -          n/a
  plan/page.tsx                                          page (RSC stub)        5 lines    (delegates)
  plan/plan-client.tsx                                   client "use client"  277 lines    plan
  plan/plan-client.test.tsx                              test                   -          n/a
  session/[id]/page.tsx                                  page (RSC, async)     10 lines    (delegates)
  session/[id]/session-client.tsx                        client "use client"  352 lines    session, exercises
  session/[id]/session-client.test.tsx                   test                   -          n/a
  session/[id]/exercise-detail-modal.tsx                 client "use client"  129 lines    exercises
  session/[id]/exercise-detail-modal.test.tsx            test                   -          n/a
  exercises/page.tsx                                     page (RSC stub)        5 lines    (delegates)
  exercises/exercises-client.tsx                         client "use client"  163 lines    exercises
  exercises/exercises-client.test.tsx                    test                   -          n/a
  exercises/[id]/page.tsx                                page (RSC, async)     10 lines    (delegates)
  exercises/[id]/exercise-detail-client.tsx              client "use client"   94 lines    exercises
  history/page.tsx                                       page (RSC stub)        5 lines    (delegates)
  history/history-client.tsx                             client "use client"  198 lines    history
  history/history-client.test.tsx                        test                   -          n/a
  metrics/page.tsx                                       page (RSC stub)        5 lines    (delegates)
  metrics/metrics-client.tsx                             client "use client"  246 lines    metrics
  metrics/metrics-client.test.tsx                        test                   -          n/a
  profile/page.tsx                                       page (RSC stub)        5 lines    (delegates)
  profile/profile-client.tsx                             client "use client"  263 lines    profile
  profile/profile-client.test.tsx                        test                   -          n/a
  profile/supplements-section.tsx                        client "use client"  162 lines    supplements
  profile/supplements-section.test.tsx                   test                   -          n/a
  profile/webhook-tokens-section.tsx                     client "use client"   87 lines    webhookTokens
  profile/webhook-tokens-section.test.tsx                test                   -          n/a
  export/page.tsx                                        page (RSC)            11 lines    (composes 2 clients)
  export/export-client.tsx                               client "use client"  329 lines    export
  export/export-client.test.tsx                          test                   -          n/a
  export/health-import-client.tsx                        client "use client"  155 lines    health
  export/health-import-client.test.tsx                   test                   -          n/a
  export/ai/page.tsx                                     page (RSC stub)        5 lines    (delegates)
  export/ai/ai-prepare-client.tsx                        client "use client"  110 lines    ai
  export/ai/ai-prepare-client.test.tsx                   test                   -          n/a
  export/ai/apply/page.tsx                               page (RSC stub)        5 lines    (delegates)
  export/ai/apply/ai-apply-client.tsx                    client "use client"  160 lines    ai
  export/ai/apply/ai-apply-client.test.tsx               test                   -          n/a
  insights/page.tsx                                      page (RSC stub)        5 lines    (delegates)
  insights/insights-client.tsx                           client "use client"  171 lines    insights
  insights/insights-client.test.tsx                      test                   -          n/a
  prs/page.tsx                                           page (RSC stub)        5 lines    (delegates)
  prs/prs-client.tsx                                     client "use client"   54 lines    prs
  prs/prs-client.test.tsx                                test                   -          n/a
  nutrition/page.tsx                                     page (RSC stub)        5 lines    (delegates)
  nutrition/nutrition-client.tsx                         client "use client"  316 lines    nutrition
  nutrition/nutrition-client.test.tsx                    test                   -          n/a
  achievements/page.tsx                                  page (RSC stub)        5 lines    (delegates)
  achievements/achievements-client.tsx                   client "use client"  123 lines    achievements
  achievements/achievements-client.test.tsx              test                   -          n/a
frontend/src/app/offline/page.tsx                        page (RSC, force-static) 15 lines  (none, static text)
frontend/src/app/api/healthz/route.ts                    route handler         10 lines    n/a
frontend/src/app/api/healthz/route.test.ts               test                   -          n/a
frontend/src/app/api/livez/route.ts                      route handler         10 lines    n/a
frontend/src/app/api/metrics/route.ts                    route handler         34 lines    n/a
frontend/src/app/api/metrics/route.test.ts               test                   -          n/a
```

Notes:
- Every authenticated `(app)/` page uses the page-RSC + client-component split, with the page file delegating to a `*-client.tsx` sibling.
- Two pages do not delegate: `dashboard/page.tsx` (entire page is `"use client"` directly) and `(auth)/login/page.tsx` (single client file).
- `export/page.tsx` is the only RSC page that composes two distinct client components (`ExportClient` + `HealthImportClient`).
- `offline/page.tsx` declares `export const dynamic = "force-static"` so the route is pre-rendered for the service worker shell cache.

---

## Section 2 - Route Catalog

| # | Route | Page file | Client file | Render | Auth | Backend endpoints invoked | Tests | SoT |
|---|---|---|---|---|---|---|---|---|
| 1 | `/` | `frontend/src/app/page.tsx` | - | RSC | public | none | none | pre-gsd |
| 2 | `/login` | `frontend/src/app/(auth)/login/page.tsx` | self | CSR | public | `POST /api/auth/login` | `(auth)/login/page.test.tsx` static: 3 | pre-gsd |
| 3 | `/dashboard` | `frontend/src/app/(app)/dashboard/page.tsx` | self | CSR | required | `GET /api/workout-plans/active`; `GET /api/sessions/active`; `POST /api/sessions/start` | `dashboard/page.test.tsx` static: 5 | pre-gsd |
| 4 | `/plan` | `frontend/src/app/(app)/plan/page.tsx` | `plan-client.tsx` | CSR | required | `GET /api/workout-plans/active`; `POST /api/workout-plans/{id}/days/{dayId}/exercises/reorder` | `plan-client.test.tsx` static: 4 | pre-gsd |
| 5 | `/session/[id]` | `frontend/src/app/(app)/session/[id]/page.tsx` | `session-client.tsx` | CSR | required | `GET /api/sessions/{id}`; `GET /api/workout-days/{dayId}`; `POST /api/sessions/{id}/sets`; `POST /api/sessions/{id}/finish`; `GET /api/exercises/{id}/last-performance`; `GET /api/exercises/{id}` (via modal) | `session-client.test.tsx` static: 5; `exercise-detail-modal.test.tsx` static: 3 | pre-gsd |
| 6 | `/exercises` | `frontend/src/app/(app)/exercises/page.tsx` | `exercises-client.tsx` | CSR | required | `GET /api/exercises` (list+filter); `GET /api/exercises/search` | `exercises-client.test.tsx` static: 3 | pre-gsd |
| 7 | `/exercises/[id]` | `frontend/src/app/(app)/exercises/[id]/page.tsx` | `exercise-detail-client.tsx` | CSR | required | `GET /api/exercises/{id}` | none | pre-gsd |
| 8 | `/history` | `frontend/src/app/(app)/history/page.tsx` | `history-client.tsx` | CSR | required | `GET /api/sessions/history`; `GET /api/sessions/{id}` | `history-client.test.tsx` static: 4 | pre-gsd |
| 9 | `/metrics` | `frontend/src/app/(app)/metrics/page.tsx` | `metrics-client.tsx` | CSR | required | `GET /api/metrics`; `POST /api/metrics`; `DELETE /api/metrics/{id}` | `metrics-client.test.tsx` static: 3 | v0.4 |
| 10 | `/profile` | `frontend/src/app/(app)/profile/page.tsx` | `profile-client.tsx` + `supplements-section.tsx` + `webhook-tokens-section.tsx` | CSR | required | `GET /api/users/me`; `PUT /api/users/me`; `GET /api/supplements`; `POST /api/supplements`; `DELETE /api/supplements/{id}`; `GET /api/users/me/webhook-tokens`; `POST /api/users/me/webhook-tokens`; `DELETE /api/users/me/webhook-tokens/{id}` | `profile-client.test.tsx` static: 3; `supplements-section.test.tsx` static: 3; `webhook-tokens-section.test.tsx` static: 2 | pre-gsd |
| 11 | `/export` | `frontend/src/app/(app)/export/page.tsx` | `export-client.tsx` + `health-import-client.tsx` | CSR | required | `GET /api/export/claude-summary`; `GET /api/export/full`; `GET /api/export/{section}` (5x); `GET /api/export/csv/sessions`; `POST /api/export/import`; `POST /api/health/import/apple`; `POST /api/health/import/google-fit`; `POST /api/health/import/fit` | `export-client.test.tsx` static: 9; `health-import-client.test.tsx` static: 4 | pre-gsd |
| 12 | `/export/ai` | `frontend/src/app/(app)/export/ai/page.tsx` | `ai-prepare-client.tsx` | CSR | required | `GET /api/export/{section}` (5x) | `ai-prepare-client.test.tsx` static: 2 | pre-gsd |
| 13 | `/export/ai/apply` | `frontend/src/app/(app)/export/ai/apply/page.tsx` | `ai-apply-client.tsx` | CSR | required | `POST /api/export/import` | `ai-apply-client.test.tsx` static: 3 | pre-gsd |
| 14 | `/insights` | `frontend/src/app/(app)/insights/page.tsx` | `insights-client.tsx` | CSR | required | `GET /api/analytics/volume`; `GET /api/analytics/prs`; `GET /api/analytics/streak`; `GET /api/analytics/heatmap`; `GET /api/analytics/one-rm/{exerciseId}` | `insights-client.test.tsx` static: 2 | v0.4 |
| 15 | `/prs` | `frontend/src/app/(app)/prs/page.tsx` | `prs-client.tsx` | CSR | required | `GET /api/analytics/prs` | `prs-client.test.tsx` static: 2 | v0.4 |
| 16 | `/nutrition` | `frontend/src/app/(app)/nutrition/page.tsx` | `nutrition-client.tsx` | CSR | required | `GET /api/users/me`; `GET /api/nutrition?date=`; `POST /api/nutrition`; `DELETE /api/nutrition/{id}`; `GET /api/foods?q=`; `GET /api/water?date=`; `POST /api/water`; `DELETE /api/water/{id}` | `nutrition-client.test.tsx` static: 5 | pre-gsd |
| 17 | `/achievements` | `frontend/src/app/(app)/achievements/page.tsx` | `achievements-client.tsx` | CSR | required | `GET /api/achievements/me` | `achievements-client.test.tsx` static: 2 | pre-gsd |
| 18 | `/offline` | `frontend/src/app/offline/page.tsx` | - | RSC (force-static) | public (cached by sw) | none | none (covered by `manifest.test.ts` static: 1 only for shell) | pre-gsd |
| 19 | `/api/healthz` | `frontend/src/app/api/healthz/route.ts` | - | route handler | public | n/a (returns `{status:"alive"}`) | `route.test.ts` static: 1 | pre-gsd |
| 20 | `/api/livez` | `frontend/src/app/api/livez/route.ts` | - | route handler | public | n/a (returns `{status:"alive"}`) | none | pre-gsd |
| 21 | `/api/metrics` | `frontend/src/app/api/metrics/route.ts` | - | route handler | public | n/a (Node process metrics, no per-request HTTP histograms - i-4 deferred) | `route.test.ts` static: 1 | pre-gsd |

Middleware coverage (`frontend/src/middleware.ts:14-26`):
`PROTECTED_PREFIXES = [/dashboard, /plan, /history, /exercises, /insights, /prs, /nutrition, /metrics, /profile, /export, /session]`. The `wh.hasSession` cookie (set via `frontend/src/lib/auth/token-store.ts:18`) gates the (app) shell; missing cookie -> redirect to `/login?next=<pathname>`.

Routes existing today but absent from v0.5 outline (Phases 22-30): `/insights`, `/prs`, `/achievements`, `/nutrition`, `/export/ai`, `/export/ai/apply`, `/offline`. The middleware also gates `/insights`, `/prs`, `/nutrition` and `/exercises/[id]` though the outline does not name these routes.

---

## Section 3 - Component and Library Inventory

### 3a) Components (`frontend/src/components/**`)

| File | Exported | Role | "use client" | Consumers (routes) | Lines | Has-test |
|---|---|---|---|---|---|---|
| `frontend/src/components/providers.tsx` | `Providers` | layout chrome (TanStack Query + next-intl) | yes | root layout | 33 | no |
| `frontend/src/components/nav.tsx` | `Nav` | layout chrome (top nav, sm+) | yes | (app) layout | 53 | no |
| `frontend/src/components/bottom-nav.tsx` | `BottomNav` | layout chrome (mobile bottom tabs) | yes | (app) layout | 55 | no |
| `frontend/src/components/theme-toggle.tsx` | `ThemeToggle` | one-shot helper (theme cycler) | yes | top nav | 58 | yes (`theme-toggle.test.tsx` static: 2) |
| `frontend/src/components/heatmap.tsx` | `Heatmap` | cross-route widget (analytics grid) | yes | insights | 44 | no |
| `frontend/src/components/pr-toast.tsx` | `PrToast` | one-shot helper (PR celebration toast) | yes | session-client | 28 | yes (`pr-toast.test.tsx` static: 2) |
| `frontend/src/components/exercise-media.tsx` | `ExerciseMedia` | cross-route widget (lazy video/image) | yes | exercise detail | 78 | yes (`exercise-media.test.tsx` static: 3) |
| `frontend/src/components/push-permission-card.tsx` | `PushPermissionCard` | cross-route widget (push opt-in) | yes | dashboard | 53 | yes (`push-permission-card.test.tsx` static: 4) |
| `frontend/src/components/service-worker-registrar.tsx` | `ServiceWorkerRegistrar` | one-shot helper (registers /sw.js) | yes | root layout | 22 | yes (`service-worker-registrar.test.tsx` static: 2) |
| `frontend/src/components/ui/button.tsx` | `Button`, `buttonVariants` | UI primitive (shadcn) | no (server-safe) | many | 48 | no |
| `frontend/src/components/ui/card.tsx` | `Card`, `CardTitle`, `CardDescription` | UI primitive (shadcn) | no | many | 41 | no |
| `frontend/src/components/ui/input.tsx` | `Input` | UI primitive (shadcn) | no | many | 25 | no |
| `frontend/src/components/ui/label.tsx` | `Label` | UI primitive (shadcn) | no | many | 17 | no |

No file in `frontend/src/components/` exceeds 200 lines. Cross-route widgets `Heatmap`, `ExerciseMedia`, and `PushPermissionCard` are the only candidates that might grow; `PrToast` is a single-purpose helper that should stay tight.

### 3b) Library modules (`frontend/src/lib/**`)

| File | Exported symbols | Responsibility | Lines | Has-test |
|---|---|---|---|---|
| `frontend/src/lib/api/client.ts` | `ApiError`, `createApiClient`, `api` | HTTP client + auth refresh interceptor + Zod parsing | 139 | yes (`client.test.ts` static: 3) |
| `frontend/src/lib/api/endpoints.ts` | 40+ wrappers: `login`, `fetchMe`, `updateMe`, `fetchActiveWorkoutPlan`, `fetchWorkoutDay`, `fetchActiveSession`, `fetchSession`, `startSession`, `addSet`, `finishSession`, `fetchLastPerformance`, `fetchExercises`, `searchExercises`, `fetchExerciseDetail`, `fetchSessionHistory`, `fetchClaudeSummary`, `fetchFullExport`, `fetchSectionExport`, `searchFoods`, `fetchNutritionForDate`, `createNutritionEntry`, `deleteNutritionEntry`, `fetchWaterDay`, `addWater`, `deleteWater`, `fetchSessionsCsv`, `importFullDump`, `fetchMetrics`, `upsertMetric`, `deleteMetric`, `fetchWeeklyVolume`, `fetchOneRepMax`, `fetchStreak`, `fetchPersonalRecords`, `fetchHeatmap`, `fetchSupplements`, `createSupplement`, `updateSupplement`, `deleteSupplement`, `reorderDayExercises`, `importAppleHealth`, `importGoogleFit`, `fetchAchievements`, `fetchWebhookTokens`, `mintWebhookToken`, `revokeWebhookToken`, `importGarminFit`; types `UpdateProfilePayload`, `AddSetPayload`, `FinishSessionPayload`, `ExerciseListFilters`, `ExportSection`, `ImportResult`, `UpsertBodyMetricPayload`, `CreateSupplementPayload`, `UpdateSupplementPayload`, `CreateNutritionEntryPayload` | API wrapper layer (Zod-validated calls into `client.ts`) | 591 | no (only `client.test.ts` exercises core fetch) |
| `frontend/src/lib/api/schemas.ts` | Zod schemas + types: `apiErrorSchema`, `authResponseSchema`, `userMeSchema`, `workoutDayExerciseSchema`, `workoutDaySchema`, `workoutPlanSchema`, `sessionSetSchema`, `sessionDetailSchema`, `lastPerformanceSchema`, `exerciseSchema`, `pageSchema`, `exercisePageSchema`, `sessionSummarySchema`, `sessionSummaryPageSchema`, `bodyMetricSchema`, `weeklyVolumeSchema`, `oneRmPointSchema`, `streakSchema`, `personalRecordSchema`, `heatmapDaySchema`, `supplementTimingSchema`, `supplementSchema`, `foodItemSchema`, `nutritionEntrySchema`, `waterEntrySchema`, `waterDaySchema`, `achievementSchema`, `webhookTokenSchema`, `healthImportResultSchema` | Wire-format Zod definitions (mirror v0.4 backend DTOs) | 332 | no (consumed by `client.test.ts` indirectly) |
| `frontend/src/lib/auth/token-store.ts` | `getAccessToken`, `setAccessToken`, `subscribeAccessToken`, `getRefreshToken`, `setRefreshToken`, `clearTokens` | Auth token store (in-memory access, localStorage refresh, `wh.hasSession` cookie marker) | 60 | no |
| `frontend/src/lib/auth/login-schema.ts` | `loginFormSchema`, `LoginFormValues` | Zod login form schema | 8 | no |
| `frontend/src/lib/offline/session-set-queue.ts` | `QueuedSet`, `getOfflineDb`, `__resetOfflineDb`, `enqueueSet`, `queuedCount`, `queuedForSession`, `drainForSession`, `subscribeOnline`, types `PostOutcome`, `DrainResult` | Dexie offline queue. **IndexedDB store name:** `workouthub-offline` (Dexie database) with object store `queuedSets`. **Primary key:** auto-increment `++id` (numeric); secondary index on `sessionId`. **Drain trigger:** `subscribeOnline()` (line 107) attaches `window.addEventListener("online", ...)` so callers can wire `drainForSession(sessionId, postFn)` to fire when the browser reports network return. | 111 | yes (`session-set-queue.test.ts` static: 7) |
| `frontend/src/lib/push/subscribe.ts` | `subscribePush` | Push subscription flow: fetch VAPID public key, call `pushManager.subscribe`, POST endpoint to `/api/push/subscribe` | 60 | no |
| `frontend/src/lib/push/rest-timer.ts` | `useRestTimer` (hook), `notifyRestElapsed` | Client-side rest timer + Notification API helper for service-worker / fallback notification | 81 | no |
| `frontend/src/lib/locale.ts` | `pickLocaleField`, `pickLocaleArray` | Bilingual field picker (TR/EN with cross-fallback) | 21 | no |
| `frontend/src/lib/time/today.ts` | `getTodayIsoDayOfWeek` | ISO-8601 day-of-week (Mon=1..Sun=7) for plan lookup | 9 | no |
| `frontend/src/lib/utils.ts` | `cn` | Tailwind class merge helper | 6 | no |

Files >200 lines (Convention "max ~200 lines per file" candidates):
- `frontend/src/lib/api/endpoints.ts` (591 lines) - thin wrapper layer; could be split by feature (auth/users/workouts/sessions/exercises/metrics/analytics/export/nutrition/water/supplements/health/webhooks/achievements/push).
- `frontend/src/lib/api/schemas.ts` (332 lines) - Zod schema collection; could be split per feature in lockstep with `endpoints.ts`.

### 3c) Cross-cutting assets

**`frontend/src/middleware.ts:42-54`** - `config.matcher`:
```
matcher: [
  "/dashboard/:path*",
  "/plan/:path*",
  "/history/:path*",
  "/exercises/:path*",
  "/insights/:path*",
  "/prs/:path*",
  "/nutrition/:path*",
  "/metrics/:path*",
  "/profile/:path*",
  "/export/:path*",
  "/session/:path*",
],
```
Missing from matcher (compared to `(app)/` route group): `/achievements/:path*`. The `achievements` route exists at `frontend/src/app/(app)/achievements/page.tsx` and is reachable via top-nav (`frontend/src/components/nav.tsx:16`), but the middleware does not gate it. Server-side fetch from this route still requires Bearer token, so unauthenticated render shows a loading state and then API errors.

**`frontend/src/i18n/request.ts:1-17`** - locale resolution:
- Supported locales: `["tr", "en"]`.
- Default: `process.env.NEXT_PUBLIC_DEFAULT_LOCALE ?? "tr"`.
- Fallback: any unsupported `requestLocale` falls back to default. Messages are imported via `await import("../../messages/${locale}.json")`.

**`frontend/messages/{en,tr}.json`** - top-level namespaces (identical between en and tr):
`nav`, `auth`, `dashboard`, `session`, `profile`, `nutrition`, `metrics`, `insights`, `prs`, `push`, `supplements`, `plan`, `export`, `ai`, `history`, `exercises`, `common`, `achievements`, `webhookTokens`, `health`. (20 namespaces.)

**`frontend/public/manifest.webmanifest`** (verbatim):
- `name: "WorkoutHub"`, `short_name: "WorkoutHub"`.
- `description: "Self-hosted workout tracker"`.
- `start_url: "/dashboard"`, `scope: "/"`, `display: "standalone"`, `orientation: "portrait"`.
- `lang: "tr"`, `theme_color: "#0ea5e9"`, `background_color: "#ffffff"`.
- `icons`: 192/any, 512/any, 512/maskable from `/icons/`.

**`frontend/public/sw.js`** (cache strategy, lines 31-40):
- `CACHE = "wh-shell-v1"`, `SHELL = ["/offline", "/manifest.webmanifest", "/icons/icon-192.png"]`.
- Install (line 7): pre-cache shell + `skipWaiting()`.
- Activate (line 13): purge caches != `wh-shell-v1`, `clients.claim()`.
- Fetch (lines 24-40): `req.method !== "GET"` -> bypass; `url.pathname.startsWith("/api/")` -> bypass (never cache API). Navigation requests are **network-first** with `/offline` fallback (lines 31-37). Non-navigation GETs are **cache-first** with network fallback (line 40).

**`frontend/playwright.config.ts:1-29`**:
- Test dir: `./e2e`. baseURL: `process.env.E2E_WEB_BASE_URL ?? "http://localhost:3000"`.
- Projects: `chromium` only (`...devices["Desktop Chrome"]`); responsive 360 spec uses `test.use({ viewport: { width: 360, height: 800 } })` to override per-test.
- `fullyParallel: false`, `workers: 1`, retries 2 in CI.
- Trace, screenshot, video all "retain-on-failure".

**`frontend/e2e/critical-flow.spec.ts`** - admin login -> start session -> log 3 sets -> finish -> /export download Claude summary JSON. Requires `E2E_ADMIN_EMAIL` + `E2E_ADMIN_PASSWORD`.

**`frontend/e2e/responsive-360.spec.ts`** - login + iterate `[/dashboard, /plan, /history, /exercises, /nutrition]` at 360x800; assert no horizontal overflow and `data-testid="bottom-nav"` visible. Captures screenshots to `e2e/screenshots/`.

Files >200 lines elsewhere in the audit scope (Convention candidates beyond Section 3a/3b):
- `frontend/src/app/(app)/session/[id]/session-client.tsx` (352 lines)
- `frontend/src/app/(app)/export/export-client.tsx` (329 lines)
- `frontend/src/app/(app)/nutrition/nutrition-client.tsx` (316 lines)
- `frontend/src/app/(app)/plan/plan-client.tsx` (277 lines)
- `frontend/src/app/(app)/profile/profile-client.tsx` (263 lines)
- `frontend/src/app/(app)/metrics/metrics-client.tsx` (246 lines)

---

## Section 4 - API Client Coverage vs v0.4 Backend

### 4a) Frontend wrappers declared in `frontend/src/lib/api/endpoints.ts`

| Wrapper (file:line) | Method | Path | Zod schema (file:line) | Consumed by (route) |
|---|---|---|---|---|
| `login` (`endpoints.ts:59`) | POST | `/api/auth/login` | `authResponseSchema` (`schemas.ts:15`) | `/login` |
| `fetchMe` (`endpoints.ts:69`) | GET | `/api/users/me` | `userMeSchema` (`schemas.ts:22`) | `/profile`, `/nutrition` |
| `updateMe` (`endpoints.ts:91`) | PUT | `/api/users/me` | `userMeSchema` (`schemas.ts:22`) | `/profile` |
| `fetchActiveWorkoutPlan` (`endpoints.ts:100`) | GET | `/api/workout-plans/active` | `workoutPlanSchema` (`schemas.ts:64`) | `/dashboard`, `/plan` |
| `fetchWorkoutDay` (`endpoints.ts:108`) | GET | `/api/workout-days/{dayId}` | `workoutDaySchema` (`schemas.ts:55`) | `/session/[id]` |
| `fetchActiveSession` (`endpoints.ts:115`) | GET | `/api/sessions/active` | `sessionDetailSchema` (`schemas.ts:87`) | `/dashboard` |
| `fetchSession` (`endpoints.ts:123`) | GET | `/api/sessions/{id}` | `sessionDetailSchema` (`schemas.ts:87`) | `/session/[id]`, `/history` |
| `startSession` (`endpoints.ts:130`) | POST | `/api/sessions/start` | `sessionDetailSchema` (`schemas.ts:87`) | `/dashboard` |
| `addSet` (`endpoints.ts:149`) | POST | `/api/sessions/{id}/sets` | `sessionSetSchema` (`schemas.ts:73`) | `/session/[id]` |
| `finishSession` (`endpoints.ts:167`) | POST | `/api/sessions/{id}/finish` | `sessionDetailSchema` (`schemas.ts:87`) | `/session/[id]` |
| `fetchLastPerformance` (`endpoints.ts:179`) | GET | `/api/exercises/{id}/last-performance` | `lastPerformanceSchema` (`schemas.ts:99`) | `/session/[id]` |
| `fetchExercises` (`endpoints.ts:197`) | GET | `/api/exercises` | `exercisePageSchema` (`schemas.ts:143`) | `/exercises` |
| `searchExercises` (`endpoints.ts:207`) | GET | `/api/exercises/search` | `exercisePageSchema` (`schemas.ts:143`) | `/exercises` |
| `fetchExerciseDetail` (`endpoints.ts:219`) | GET | `/api/exercises/{id}` | `exerciseSchema` (`schemas.ts:114`) | `/exercises/[id]`, `/session/[id]` (modal) |
| `fetchSessionHistory` (`endpoints.ts:226`) | GET | `/api/sessions/history` | `sessionSummaryPageSchema` (`schemas.ts:156`) | `/history` |
| `fetchClaudeSummary` (`endpoints.ts:237`) | GET | `/api/export/claude-summary?days=N` | - (intentionally untyped pass-through) | `/export` |
| `fetchFullExport` (`endpoints.ts:246`) | GET | `/api/export/full` | - (untyped) | `/export` |
| `fetchSectionExport` (`endpoints.ts:259`) | GET | `/api/export/{section}` | - (untyped) | `/export`, `/export/ai` |
| `searchFoods` (`endpoints.ts:265`) | GET | `/api/foods?q=&size=` | `foodItemListSchema` (`schemas.ts:248`) | `/nutrition` |
| `fetchNutritionForDate` (`endpoints.ts:273`) | GET | `/api/nutrition?date=` | `nutritionEntryListSchema` (`schemas.ts:263`) | `/nutrition` |
| `createNutritionEntry` (`endpoints.ts:288`) | POST | `/api/nutrition` | `nutritionEntrySchema` (`schemas.ts:250`) | `/nutrition` |
| `deleteNutritionEntry` (`endpoints.ts:299`) | DELETE | `/api/nutrition/{id}` | - (204) | `/nutrition` |
| `fetchWaterDay` (`endpoints.ts:306`) | GET | `/api/water?date=` | `waterDaySchema` (`schemas.ts:274`) | `/nutrition` |
| `addWater` (`endpoints.ts:314`) | POST | `/api/water` | `waterEntrySchema` (`schemas.ts:268`) | `/nutrition` |
| `deleteWater` (`endpoints.ts:323`) | DELETE | `/api/water/{id}` | - (204) | `/nutrition` |
| `fetchSessionsCsv` (`endpoints.ts:330`) | GET | `/api/export/csv/sessions` | - (text/csv via `api.raw`) | `/export` |
| `importFullDump` (`endpoints.ts:353`) | POST | `/api/export/import` | - (custom `ImportResult` type, no Zod) | `/export`, `/export/ai/apply` |
| `fetchMetrics` (`endpoints.ts:372`) | GET | `/api/metrics` | `bodyMetricListSchema` (`schemas.ts:173`) | `/metrics` |
| `upsertMetric` (`endpoints.ts:379`) | POST | `/api/metrics` | `bodyMetricSchema` (`schemas.ts:158`) | `/metrics` |
| `deleteMetric` (`endpoints.ts:390`) | DELETE | `/api/metrics/{id}` | - (204) | `/metrics` |
| `fetchWeeklyVolume` (`endpoints.ts:397`) | GET | `/api/analytics/volume?weeks=N` | `weeklyVolumeListSchema` (`schemas.ts:180`) | `/insights` |
| `fetchOneRepMax` (`endpoints.ts:405`) | GET | `/api/analytics/one-rm/{exerciseId}` | `oneRmPointListSchema` (`schemas.ts:188`) | `/insights` |
| `fetchStreak` (`endpoints.ts:412`) | GET | `/api/analytics/streak` | `streakSchema` (`schemas.ts:190`) | `/insights` |
| `fetchPersonalRecords` (`endpoints.ts:419`) | GET | `/api/analytics/prs` | `personalRecordListSchema` (`schemas.ts:205`) | `/insights`, `/prs` |
| `fetchHeatmap` (`endpoints.ts:426`) | GET | `/api/analytics/heatmap?weeks=N` | `heatmapListSchema` (`schemas.ts:211`) | `/insights` |
| `fetchSupplements` (`endpoints.ts:445`) | GET | `/api/supplements` | `supplementListSchema` (`schemas.ts:233`) | `/profile` |
| `createSupplement` (`endpoints.ts:452`) | POST | `/api/supplements` | `supplementSchema` (`schemas.ts:223`) | `/profile` |
| `updateSupplement` (`endpoints.ts:463`) | PUT | `/api/supplements/{id}` | `supplementSchema` (`schemas.ts:223`) | (declared, no current consumer) |
| `deleteSupplement` (`endpoints.ts:475`) | DELETE | `/api/supplements/{id}` | - (204) | `/profile` |
| `reorderDayExercises` (`endpoints.ts:482`) | POST | `/api/workout-plans/{planId}/days/{dayId}/exercises/reorder` | `workoutDaySchema` (`schemas.ts:55`) | `/plan` |
| `importAppleHealth` (`endpoints.ts:525`) | POST (multipart) | `/api/health/import/apple` | `healthImportResultSchema` (`schemas.ts:310`) | `/export` |
| `importGoogleFit` (`endpoints.ts:532`) | POST (multipart) | `/api/health/import/google-fit` | `healthImportResultSchema` (`schemas.ts:310`) | `/export` |
| `importGarminFit` (`endpoints.ts:574`) | POST (multipart) | `/api/health/import/fit` | `healthImportResultSchema` (`schemas.ts:310`) | `/export` |
| `fetchAchievements` (`endpoints.ts:539`) | GET | `/api/achievements/me` | `achievementListSchema` (`schemas.ts:297`) | `/achievements` |
| `fetchWebhookTokens` (`endpoints.ts:546`) | GET | `/api/users/me/webhook-tokens?purpose=` | `webhookTokenListSchema` (`schemas.ts:307`) | `/profile` |
| `mintWebhookToken` (`endpoints.ts:556`) | POST | `/api/users/me/webhook-tokens?purpose=` | `webhookTokenSchema` (`schemas.ts:300`) | `/profile` |
| `revokeWebhookToken` (`endpoints.ts:567`) | DELETE | `/api/users/me/webhook-tokens/{id}` | - (204) | `/profile` |
| `subscribePush` (`subscribe.ts:29`) | POST | `/api/push/subscribe` | - (untyped) | `/dashboard` (via PushPermissionCard) |
| `subscribePush` (`subscribe.ts:35`) | GET | `/api/push/vapid-public-key` | - (inline `VapidResponse` type, no Zod) | `/dashboard` (via PushPermissionCard) |

### 4b) v0.4 backend surface (cross-reference)

Backend surface from v0.4 archives (non-exhaustive but covers Phases 13-19 audited contracts):
- `auth/`: `POST /api/auth/login`, `POST /api/auth/refresh` (transparent retry path); `POST /api/auth/register` removed in t-51 (admin-only seeding); `POST /api/auth/logout` if exposed.
- `users/`: `GET /api/users/me`, `PUT /api/users/me`, `GET/POST/DELETE /api/users/me/webhook-tokens(/{id})`.
- `exercises/`: `GET /api/exercises`, `GET /api/exercises/search`, `GET /api/exercises/{id}`, `GET /api/exercises/{id}/last-performance`, `GET /api/exercises/{id}/progress`, admin CRUD `POST/PUT /api/admin/exercises(/{id})`.
- `workouts/`: `GET /api/workout-plans`, `POST /api/workout-plans`, `GET /api/workout-plans/active`, `GET /api/workout-plans/{id}`, `PUT /api/workout-plans/{id}`, `DELETE /api/workout-plans/{id}`, `POST /api/workout-plans/{id}/activate`, days/items CRUD (`POST /api/workout-plans/{id}/days`, `PUT/DELETE /api/workout-plans/{id}/days/{dayId}/exercises/{exId}`, `POST /api/workout-plans/{id}/days/{dayId}/exercises/reorder`), `GET /api/workout-days/{dayId}`.
- `sessions/`: `POST /api/sessions/start`, `GET /api/sessions/active`, `POST /api/sessions/{id}/sets` (with `clientSetId` idempotency from Phase 15-04), `PUT /api/sessions/{id}/sets/{setId}`, `POST /api/sessions/{id}/finish`, `GET /api/sessions/history`, `GET /api/sessions/{id}`.
- `sessions-analytics/` (Phase 16): `GET /api/analytics/volume`, `GET /api/analytics/one-rm/{exerciseId}`, `GET /api/analytics/streak`, `GET /api/analytics/prs`, `GET /api/analytics/heatmap`.
- `metrics/`: `GET /api/metrics`, `POST /api/metrics` (UPSERT 200/201 from Phase 17), `DELETE /api/metrics/{id}`.
- `export/` (Phase 18): `GET /api/export/full`, `GET /api/export/{section}` (5 sections), `GET /api/export/csv/sessions`, `GET /api/export/claude-summary?days=N` (snake_case per-record `@JsonNaming`), `POST /api/export/import` (idempotent re-import with isPr safety net).
- Operator surfaces: `/livez`, `/healthz`, `/v3/api-docs` (paths-to-match `/api/**` only). Frontend should NOT call these from the browser.

### 4c) Coverage matrix

| Backend endpoint | Frontend wrapper | Zod schema | Consumed by | Status | ApiError handling |
|---|---|---|---|---|---|
| `POST /api/auth/login` | `login` | `authResponseSchema` | /login | Wrapped+typed | message-only (`(auth)/login/page.tsx:38-44` - branches on `error.status`, no `error.code` use) |
| `POST /api/auth/refresh` | implicit in `client.ts:54` | `authResponseSchema` | (interceptor) | Wrapped+typed | none (silent token refresh) |
| `POST /api/auth/register` | - | - | - | Missing (intentional - removed in t-51) | n/a |
| `POST /api/auth/logout` | - | - | - | Missing | none |
| `GET /api/users/me` | `fetchMe` | `userMeSchema` | /profile, /nutrition | Wrapped+typed | none (no error UI on profile load failure) |
| `PUT /api/users/me` | `updateMe` | `userMeSchema` | /profile | Wrapped+typed | message-only (`profile-client.tsx:62` `flash="error"`) |
| `GET /api/users/me/webhook-tokens` | `fetchWebhookTokens` | `webhookTokenListSchema` | /profile | Wrapped+typed | none |
| `POST /api/users/me/webhook-tokens` | `mintWebhookToken` | `webhookTokenSchema` | /profile | Wrapped+typed | none |
| `DELETE /api/users/me/webhook-tokens/{id}` | `revokeWebhookToken` | - (204) | /profile | Wrapped+typed | none |
| `GET /api/exercises` | `fetchExercises` | `exercisePageSchema` | /exercises | Wrapped+typed | none |
| `GET /api/exercises/search` | `searchExercises` | `exercisePageSchema` | /exercises | Wrapped+typed | none |
| `GET /api/exercises/{id}` | `fetchExerciseDetail` | `exerciseSchema` | /exercises/[id], /session/[id] modal | Wrapped+typed | message-only |
| `GET /api/exercises/{id}/last-performance` | `fetchLastPerformance` | `lastPerformanceSchema` | /session/[id] | Wrapped+typed | none |
| `GET /api/exercises/{id}/progress` | - | - | - | Missing | n/a |
| `POST /api/admin/exercises` | - | - | - | Missing | n/a |
| `PUT /api/admin/exercises/{id}` | - | - | - | Missing | n/a |
| `GET /api/workout-plans` | - | - | - | Missing (only "active" wrapped) | n/a |
| `POST /api/workout-plans` | - | - | - | Missing | n/a |
| `GET /api/workout-plans/active` | `fetchActiveWorkoutPlan` | `workoutPlanSchema` | /dashboard, /plan | Wrapped+typed | none |
| `GET /api/workout-plans/{id}` | - | - | - | Missing | n/a |
| `PUT /api/workout-plans/{id}` | - | - | - | Missing | n/a |
| `DELETE /api/workout-plans/{id}` | - | - | - | Missing | n/a |
| `POST /api/workout-plans/{id}/activate` | - | - | - | Missing | n/a |
| `POST /api/workout-plans/{id}/days` | - | - | - | Missing | n/a |
| `PUT /api/workout-plans/{id}/days/{dayId}` | - | - | - | Missing | n/a |
| `DELETE /api/workout-plans/{id}/days/{dayId}` | - | - | - | Missing | n/a |
| `POST /api/workout-plans/{id}/days/{dayId}/exercises` | - | - | - | Missing | n/a |
| `PUT /api/workout-plans/{id}/days/{dayId}/exercises/{exId}` | - | - | - | Missing | n/a |
| `DELETE /api/workout-plans/{id}/days/{dayId}/exercises/{exId}` | - | - | - | Missing | n/a |
| `POST /api/workout-plans/{id}/days/{dayId}/exercises/reorder` | `reorderDayExercises` | `workoutDaySchema` | /plan | Wrapped+typed | none (silent revert on failure - `plan-client.tsx:133`) |
| `GET /api/workout-days/{dayId}` | `fetchWorkoutDay` | `workoutDaySchema` | /session/[id] | Wrapped+typed | none |
| `POST /api/sessions/start` | `startSession` | `sessionDetailSchema` | /dashboard | Wrapped+typed | none (no UI for `SESSION_ALREADY_ACTIVE` typed code) |
| `GET /api/sessions/active` | `fetchActiveSession` | `sessionDetailSchema` | /dashboard | Wrapped+typed | none |
| `POST /api/sessions/{id}/sets` | `addSet` | `sessionSetSchema` | /session/[id] | Wrapped+typed | none (no UI for `SESSION_FINISHED`, `SESSION_ALREADY_FINISHED`, `SET_NUMBER_DUPLICATE`) |
| `PUT /api/sessions/{id}/sets/{setId}` | - | - | - | Missing (no edit-set wrapper) | n/a |
| `POST /api/sessions/{id}/finish` | `finishSession` | `sessionDetailSchema` | /session/[id] | Wrapped+typed | none |
| `GET /api/sessions/history` | `fetchSessionHistory` | `sessionSummaryPageSchema` | /history | Wrapped+typed | none |
| `GET /api/sessions/{id}` | `fetchSession` | `sessionDetailSchema` | /session/[id], /history | Wrapped+typed | message-only (`session-client.tsx:60` shows `notFound`) |
| `GET /api/analytics/volume` | `fetchWeeklyVolume` | `weeklyVolumeListSchema` | /insights | Wrapped+typed | none |
| `GET /api/analytics/one-rm/{exerciseId}` | `fetchOneRepMax` | `oneRmPointListSchema` | /insights | Wrapped+typed | none |
| `GET /api/analytics/streak` | `fetchStreak` | `streakSchema` | /insights | Wrapped+typed | none |
| `GET /api/analytics/prs` | `fetchPersonalRecords` | `personalRecordListSchema` | /insights, /prs | Wrapped+typed | none |
| `GET /api/analytics/heatmap` | `fetchHeatmap` | `heatmapListSchema` | /insights | Wrapped+typed | none |
| `GET /api/metrics` | `fetchMetrics` | `bodyMetricListSchema` | /metrics | Wrapped+typed | none |
| `POST /api/metrics` (200/201 split) | `upsertMetric` | `bodyMetricSchema` | /metrics | Wrapped+typed | none (does NOT branch on 200 vs 201 - both treated identically; toast not differentiated by insert vs replace) |
| `DELETE /api/metrics/{id}` | `deleteMetric` | - (204) | /metrics | Wrapped+typed | none |
| `GET /api/supplements` | `fetchSupplements` | `supplementListSchema` | /profile | Wrapped+typed | none |
| `POST /api/supplements` | `createSupplement` | `supplementSchema` | /profile | Wrapped+typed | none |
| `PUT /api/supplements/{id}` | `updateSupplement` | `supplementSchema` | (no consumer) | Wrapped+typed | none |
| `DELETE /api/supplements/{id}` | `deleteSupplement` | - (204) | /profile | Wrapped+typed | none |
| `GET /api/foods?q=` | `searchFoods` | `foodItemListSchema` | /nutrition | Wrapped+typed | none |
| `GET /api/nutrition?date=` | `fetchNutritionForDate` | `nutritionEntryListSchema` | /nutrition | Wrapped+typed | none |
| `POST /api/nutrition` | `createNutritionEntry` | `nutritionEntrySchema` | /nutrition | Wrapped+typed | none |
| `DELETE /api/nutrition/{id}` | `deleteNutritionEntry` | - (204) | /nutrition | Wrapped+typed | none |
| `GET /api/water?date=` | `fetchWaterDay` | `waterDaySchema` | /nutrition | Wrapped+typed | none |
| `POST /api/water` | `addWater` | `waterEntrySchema` | /nutrition | Wrapped+typed | none |
| `DELETE /api/water/{id}` | `deleteWater` | - (204) | /nutrition | Wrapped+typed | none |
| `GET /api/export/full` | `fetchFullExport` | - | /export | Wrapped+untyped | message-only (`export-client.tsx:72`) |
| `GET /api/export/{section}` | `fetchSectionExport` | - | /export, /export/ai | Wrapped+untyped | message-only |
| `GET /api/export/csv/sessions` | `fetchSessionsCsv` | - (text) | /export | Wrapped+untyped | message-only |
| `GET /api/export/claude-summary?days=N` (snake_case) | `fetchClaudeSummary` | - (intentional pass-through) | /export | Wrapped+untyped | message-only |
| `POST /api/export/import` | `importFullDump` | - (typed `ImportResult`, no Zod) | /export, /export/ai/apply | Wrapped+untyped | message-only (`export-client.tsx:127`) |
| `POST /api/health/import/apple` | `importAppleHealth` | `healthImportResultSchema` | /export | Wrapped+typed | message-only |
| `POST /api/health/import/google-fit` | `importGoogleFit` | `healthImportResultSchema` | /export | Wrapped+typed | message-only |
| `POST /api/health/import/fit` | `importGarminFit` | `healthImportResultSchema` | /export | Wrapped+typed | message-only |
| `GET /api/achievements/me` | `fetchAchievements` | `achievementListSchema` | /achievements | Wrapped+typed | message-only (`achievements-client.tsx:33`) |
| `GET /api/push/vapid-public-key` | `subscribePush` (inline) | - (no Zod) | (push card) | Wrapped+untyped | none (caught + status reset) |
| `POST /api/push/subscribe` | `subscribePush` (inline) | - (no Zod) | (push card) | Wrapped+untyped | none |
| Operator: `/livez`, `/healthz`, `/v3/api-docs` | - | - | - | Missing (correctly NOT consumed from browser per contract) | n/a |

### 4d) Catalog gaps

**Backend endpoints with no frontend wrapper:**
- `POST /api/auth/logout` - no client-side logout call. Token clearing happens via `clearTokens()` in `frontend/src/lib/auth/token-store.ts:57`, which only zeroes local state and does not invalidate the refresh token server-side. (Source: no consumer.)
- Plan CRUD beyond active: `GET /api/workout-plans`, `POST /api/workout-plans`, `GET /api/workout-plans/{id}`, `PUT /api/workout-plans/{id}`, `DELETE /api/workout-plans/{id}`, `POST /api/workout-plans/{id}/activate`, plus day CRUD (`POST /api/workout-plans/{id}/days`, `PUT/DELETE .../days/{dayId}`) and exercise CRUD (`POST/PUT/DELETE .../days/{dayId}/exercises(/{exId})`). Only `reorder` is wrapped (`endpoints.ts:482`). Plan editor (`plan-client.tsx`) can reorder but cannot create plans, switch plans, add/remove days, or add/remove exercises. (Source: no consumer.)
- `PUT /api/sessions/{id}/sets/{setId}` - no edit-existing-set wrapper. Once a set lands, the user cannot correct reps/weight/rpe from the UI. (Source: no consumer.)
- `GET /api/exercises/{id}/progress` - per-exercise progress chart not wrapped. ProjectBrief Phase 4 names this. (Source: no consumer.)
- Admin exercise CRUD (`POST/PUT /api/admin/exercises(/{id})`) - no admin UI exists. Acceptable for v0.5 (admin operations stay backend-only via tooling).
- `GET /api/sessions/active` is wrapped, but no client-side polling or stale-while-revalidate strategy exists (only the dashboard fetches it on mount).

**Frontend wrappers calling endpoints that diverge from v0.4 contract:**
- `addSet` (`endpoints.ts:139-147`, `endpoints.ts:149`) - the `AddSetPayload` type does NOT include a `clientSetId` field. The v0.4 contract (Phase 15-04) specifies `clientSetId` as the idempotency key that suppresses `newPr` on replay. Today, every retry from the IndexedDB queue would re-issue without `clientSetId` and rely solely on the DB UNIQUE constraint on `(session_id, exercise_id, set_number)` to dedupe via 409 (`session-set-queue.ts:95`). This works for crash-recovery dedup but loses the clean `newPr` suppression semantics on idempotent replay; PR toast can fire spuriously after offline drain.
- `upsertMetric` (`endpoints.ts:379`) - does not surface the 200 vs 201 distinction. The wrapper returns `bodyMetricSchema` either way, so `/metrics` cannot show "Yeni kayit" vs "Gunceleme" in the UI based on `wasCreated`. v0.4 Phase 17 designed the split intentionally; the UI ignores it.
- `subscribePush` (`subscribe.ts:35-38`) - calls `GET /api/push/vapid-public-key` and `POST /api/push/subscribe` with inline `VapidResponse` and `SubscribeBody` types instead of Zod schemas in `frontend/src/lib/api/schemas.ts`. Drift risk if the backend wire format changes.
- `importFullDump` (`endpoints.ts:353-359`) - uses a hand-written `ImportResult` type (`endpoints.ts:342-351`) rather than a Zod schema. `warnings` and `suggestions` are optional `string[]`, so a backend regression that drops these fields silently passes.
- `fetchClaudeSummary` (`endpoints.ts:237-244`) - intentionally untyped. The doc comment states this is pass-through to Claude. v0.4 made `ClaudeSummaryDto` snake_case via `@JsonNaming`; the frontend never decodes it, only round-trips it to `downloadJson` (`export-client.tsx:51-56`). Acceptable as long as no consumer reads fields from the response. (Confirmed: no consumer accesses fields.)

**Missing `ApiError.code`-based branching on the four typed values:**
- `SESSION_ALREADY_ACTIVE` - should appear on `POST /api/sessions/start` from `/dashboard`. No branch in `dashboard/page.tsx:32-38`. (Source: `dashboard/page.tsx:32`.)
- `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED` - should appear on `POST /api/sessions/{id}/sets` and `POST /api/sessions/{id}/finish` from `/session/[id]`. No branch in `session-client.tsx:154-171` (`addSet` mutation onError absent) or `session-client.tsx:47-54` (`finishSession` mutation onError absent). (Source: `session-client.tsx:153, 47`.)
- `SET_NUMBER_DUPLICATE` - should appear on `POST /api/sessions/{id}/sets`. No branch in `session-client.tsx:153-171`. The offline queue treats 409 as "already saved" (`session-set-queue.ts:95`) but online path does nothing, so a duplicate set submission silently fails the mutation. (Source: `session-client.tsx:153`.)
- The login page (`(auth)/login/page.tsx:38-44`) is the only call site that branches on `ApiError.status`. No call site reads `error.body.code` to disambiguate; all other consumers either show a generic toast or no UI at all.

**`ClaudeSummaryDto` snake_case decoding:**
- Not decoded - intentionally pass-through. `fetchClaudeSummary` returns `unknown` and `export-client.tsx:51-56` writes the body straight to a file via `downloadJson`. PASS for v0.4 contract: snake_case stays snake_case on disk, which is what Claude consumes. No drift risk.

**`isPr` field handling:**
- Wire schema: `sessionSetSchema.newPr` (`schemas.ts:84`) is the on-display PR flag. Backend `SessionSet.isPr` is exposed as `newPr` in the API response. The frontend does not import a separate `isPr` field; only `newPr` is read.
- Display path: `session-client.tsx:162` reads `newSet.newPr` and triggers `PrToast`. PASS.
- Offline replay path: `session-set-queue.ts:65-105` POSTs queued sets one at a time via `postFn`. The response body is not parsed for `newPr`, so toast is suppressed on offline drain - which matches v0.4's idempotent-replay semantics for a `clientSetId` replay (newPr suppressed). However, since `addSet` does NOT send `clientSetId`, a queued set posted after a real first attempt that succeeded would 409-dedupe (PASS), but a queued set posted as the first attempt does not get a `newPr` celebration on the UI because the queue layer drops the response. ACCEPTABLE: the toast is best-effort during offline mode.
- History display: `history-client.tsx:147-156` lists `s.repsDone` and `s.weightKg` but does not surface `newPr` per set (PR list lives at `/prs`). Acceptable.
- Export round-trip: full-export and import paths go through `fetchFullExport` / `importFullDump`, both untyped on the frontend. Backend Phase 18-04 added isPr to the 9th tuple field; the frontend just round-trips the JSON without inspecting the field, so no drift risk on the wire.

---

## Section 5 - ProjectBrief Phase 4-6 Gap Matrix

ProjectBrief sections cited: Phase 4 (workout execution + offline) `ProjectBrief.md:291-318`; Phase 5 (frontend surfaces) `ProjectBrief.md:321-386`; Phase 6 (charts and stats) `ProjectBrief.md:389-406`.

Status legend: `Implemented` (route + backend wiring + at least one test); `Partial` (renders but a major behavior is missing); `Stub` (placeholder only); `Missing` (no route or component).

| Requirement (PB lines) | Current implementation | Status | Evidence | Phase target |
|---|---|---|---|---|
| **Auth** |  |  |  |  |
| Login (PB:327, PB:202) | `(auth)/login/page.tsx` | Implemented | `(auth)/login/page.tsx:17-103`; test `(auth)/login/page.test.tsx` static: 3 | done |
| Register (PB:327, PB:202) | - | Missing | n/a (admin-only seeding per t-51) | defer-v0.6 (or accept-as-omitted) |
| Logout (PB:327) | - | Missing | `clearTokens` exists in `token-store.ts:57` but no UI invokes it; no nav button | 22 (auth-pages) |
| Refresh-token transparent retry (PB:204) | `client.ts:49-78` interceptor | Implemented | `client.ts:80-96` retry-once on 401; covered by `client.test.ts` static: 3 | done |
| **Dashboard** (PB:328-333) |  |  |  |  |
| Today's workout card | `dashboard/page.tsx` | Implemented | `dashboard/page.tsx:73-97`; test `dashboard/page.test.tsx` static: 5 | done |
| Weekly summary (kac antrenman, hedef vs gercek) | - | Missing | No weekly summary card on dashboard | 23 (dashboard) |
| Last weight readout | - | Missing | Dashboard does not call `fetchMetrics`; no weight chip | 23 (dashboard) |
| Quick actions (Antrenmani basla, Kilo ekle) | `dashboard/page.tsx:121-132` `QuickActions` | Partial | "View plan" + "Quick add" present; "Antrenmani basla" handled separately by main card | 23 (dashboard) - polish |
| **Plan editor** (PB:334-336, PB:265-288) |  |  |  |  |
| Weekly view (Mon-Sun) | `plan-client.tsx` | Implemented | `plan-client.tsx:56-66`; test `plan-client.test.tsx` static: 4 | done |
| Day reorder (drag-and-drop) | - | Missing | Day cards not draggable; only exercises within a day reorder | 24 (plan-editor) |
| Exercise reorder (within a day) | `plan-client.tsx:111-195` `DayExercises` | Implemented | `plan-client.tsx:127-137` (`reorderDayExercises`); covered by `plan-client.test.tsx` | done |
| Exercise CRUD modal | - | Missing | No add/remove/edit exercise UI; only reorder | 24 (plan-editor) |
| Plan create | - | Missing | No `POST /api/workout-plans` consumer | 24 (plan-editor) |
| Plan activate/deactivate / switch | - | Missing | No `POST /api/workout-plans/{id}/activate` consumer | 24 (plan-editor) |
| Day add/remove/edit | - | Missing | No `POST /api/workout-plans/{id}/days` consumer | 24 (plan-editor) |
| **Session execution** (PB:291-318, PB:339-345) |  |  |  |  |
| Set logging UI (reps/weight/rpe) | `session-client.tsx:127-279` `ExerciseBlock` | Partial | Reps + weightKg captured; **rpe missing** (`session-client.tsx:181-189` does not send `rpe`); covered by `session-client.test.tsx` static: 5 | 25 (session-execution) |
| Rest timer (PB:344) | `useRestTimer` (`rest-timer.ts:11`) | Implemented | `session-client.tsx:144` consumes hook; auto-start on `addSet` success | done |
| Last-performance display (PB:341) | `LastPerformanceChip` (`session-client.tsx:300-325`) | Implemented | `session-client.tsx:218`; data via `fetchLastPerformance` | done |
| isPr toast | `PrToast` (`pr-toast.tsx`) | Implemented | `session-client.tsx:226-231` triggers on `newSet.newPr`; test `pr-toast.test.tsx` static: 2 | done |
| IndexedDB queue (Dexie) (PB:310-313) | `session-set-queue.ts` | Partial | Dexie table + drain function exist (`session-set-queue.ts:43-105`); test `session-set-queue.test.ts` static: 7. **NOT WIRED into `session-client.tsx`** - online `addSet` calls `addSet` directly, no queue path; `subscribeOnline` (`session-set-queue.ts:107`) has no caller in app. | 25 (session-execution) |
| Drain on online event | `subscribeOnline` (`session-set-queue.ts:107`) | Stub | Helper exists but has zero call sites in `frontend/src/app/**`; offline writes never enqueue, drain never runs in production code path | 25 (session-execution) |
| Exercise-detail modal during a session (PB:345) | `exercise-detail-modal.tsx` | Implemented | `session-client.tsx:220-225`; test `exercise-detail-modal.test.tsx` static: 3 | done |
| `clientSetId` idempotency | - | Missing | `AddSetPayload` (`endpoints.ts:139-147`) lacks `clientSetId`; relies on 409 dedupe | 25 (session-execution) |
| Edit existing set (PB:300, "PUT /api/sessions/:id/sets/:setId") | - | Missing | No wrapper exists | defer-v0.6 (or 25 if scope allows) |
| **Exercise catalog** (PB:365-369) |  |  |  |  |
| List | `exercises-client.tsx` | Implemented | `exercises-client.tsx:41-54`; test `exercises-client.test.tsx` static: 3 | done |
| Filters (muscle/equipment/difficulty) | `exercises-client.tsx:14-24` | Implemented | `FilterSelect` rows for category, equipment, difficulty | done |
| Search | `searchExercises` (`endpoints.ts:207`) | Implemented | `exercises-client.tsx:35-54` (300ms debounce) | done |
| Detail page | `exercises/[id]/exercise-detail-client.tsx` | Implemented | `exercise-detail-client.tsx:11-94` | done |
| Detail modal (in-session) | `exercise-detail-modal.tsx` | Implemented | covered above | done |
| **History** (PB:357-359) |  |  |  |  |
| Calendar with completed days marked | `history-client.tsx:73-105` | Implemented | `history-client.tsx`; test `history-client.test.tsx` static: 4 | done |
| Per-session detail | `history-client.tsx:126-163` `SelectedSessionCard` | Implemented | shows sets list + link to `/session/{id}` | done |
| **Metrics** (PB:360-364) |  |  |  |  |
| Weight chart (weekly/monthly/all-time) | `metrics-client.tsx:82-121` | Implemented | recharts LineChart with range toggles; test `metrics-client.test.tsx` static: 3 | done |
| Measurement form | `metrics-client.tsx:123-188` | Implemented | weightKg / bodyFatPercent / waistCm / notes | done |
| Other measurements (chest/arm/thigh) (PB:184) | - | Missing | `bodyMetricSchema` defines them (`schemas.ts:163-166`) but form omits them | 28 (metrics-ui) |
| Progress photo upload (optional) (PB:184, PB:362) | - | Missing | `bodyMetricSchema.photoUrl` exists but form has no upload widget; no `/uploads` endpoint wired | defer-v0.6 |
| **Profile** (PB:370-374) |  |  |  |  |
| User info edit | `profile-client.tsx` | Implemented | `profile-client.tsx:86-225`; test `profile-client.test.tsx` static: 3 | done |
| Health notes | `profile-client.tsx:150-158` | Implemented | single-line input | done |
| Goals | `profile-client.tsx:160-166` | Implemented | single-line input | done |
| Daily macro goals (kcal/protein/carbs/fat) | `profile-client.tsx:168-205` | Implemented | 4 numeric fields | done |
| Supplements list edit | `supplements-section.tsx` | Implemented | CRUD list with timing + reminderTime; test static: 3 | done |
| Webhook tokens (scale) | `webhook-tokens-section.tsx` | Implemented | mint/revoke; test static: 2 | done |
| **Export / Import** (PB:376-380) |  |  |  |  |
| Full JSON download | `export-client.tsx:197-218` | Implemented | `fullMutation`; covered by `export-client.test.tsx` static: 9 | done |
| JSON restore | `export-client.tsx:262-326` | Implemented | file picker + `importFullDump`; result + warnings rendered | done |
| Claude summary action | `export-client.tsx:153-195` | Implemented | days input + download | done |
| Per-section export (5 sections) | `export-client.tsx:220-260` | Implemented | profile/plans/sessions/metrics/supplements buttons | done |
| CSV sessions export | `export-client.tsx:246-258` | Implemented | `fetchSessionsCsv` | done |
| AI prepare flow | `ai-prepare-client.tsx` | Implemented | `/export/ai`; test static: 2 | done |
| AI apply flow (commit JSON dump) | `ai-apply-client.tsx` | Implemented | `/export/ai/apply` upload + diff + commit; test static: 3 | done |
| Apple Health import | `health-import-client.tsx` | Implemented | XML/zip picker; test `health-import-client.test.tsx` static: 4 | done |
| Google Fit import | `health-import-client.tsx:94-108` | Implemented | JSON picker | done |
| Garmin .fit import | `health-import-client.tsx:111-125` | Implemented | binary upload | done |
| **Phase 6 charts and stats** (PB:389-406) |  |  |  |  |
| Volume (weekly total kg x reps) | `insights-client.tsx:92-119` | Implemented | recharts BarChart of `weeklyVolume`; test `insights-client.test.tsx` static: 2 | done |
| 1RM (Epley) per exercise | `insights-client.tsx:121-168` | Implemented | recharts LineChart of `oneRepMax`; exercise picker | done |
| Weight change (PB:393) | `metrics-client.tsx` | Implemented | weight chart on /metrics page | done |
| Frequency heatmap (GitHub-style) | `Heatmap` (`heatmap.tsx`) + `insights-client.tsx:85-89` | Implemented | renders heatmapDay grid | done |
| PR list | `prs-client.tsx` | Implemented | `/prs` page; test `prs-client.test.tsx` static: 2 | done |
| Streak | `insights-client.tsx:65-90` | Implemented | streak chips + heatmap | done |

Summary: Implemented 38, Partial 4, Stub 1, Missing 14.

Stub call-out:
- "Drain on online event" - `frontend/src/lib/offline/session-set-queue.ts:107-111` exposes `subscribeOnline(handler)`; grep across `frontend/src/app/**` and `frontend/src/components/**` shows zero call sites that wire this to a real drain. The Dexie table is built and tested in isolation but the production session client (`session-client.tsx:153-171`) calls `addSet` directly without an enqueue-or-flush wrapper.

Routes that exist but are NOT in any ProjectBrief Phase 4-6 surface (extras shipped beyond brief):
- `/nutrition` and `/api/water` flow - nutrition tracking lives in ProjectBrief Phase 7+ bonus list (PB:456-459); already shipped in v0.4 backend + pre-gsd frontend.
- `/achievements` - gamification (PB:475-477 bonus); shipped early.
- Webhook tokens UI in profile - scale-webhook (pre-gsd backend), exposed via UI.
- AI prepare/apply flow - separate from raw JSON download; not explicitly in PB but supports PB:504-573 "Claude data management".

---

## Section 6 - v0.5 Phase Re-scope Recommendation

| Phase | Recommendation | Rationale (Section 5 evidence) | Sub-plans | Dependencies |
|---|---|---|---|---|
| 22 auth-pages | narrow | Login + refresh interceptor already done; only `Logout` action missing (S5 Auth row) and a non-existent `/register` was removed in t-51. Remaining work: add a logout button somewhere in the (app) shell (top nav + bottom nav profile menu) that calls `clearTokens` and clears query cache; surface session-expired toast when refresh fails (`client.ts:60` already clears tokens silently). | 1 | none |
| 23 dashboard | narrow | Today's-workout card exists. Missing: weekly summary card (`fetchSessionHistory` aggregation); last-weight chip (`fetchMetrics` first row). Quick actions exist but minimal. (S5 Dashboard rows.) | 1 | 22 (logout link in nav drawer) |
| 24 plan-editor | expand + split | Current plan editor only reorders exercises. Missing: plan create / activate / day add-remove-edit / exercise CRUD modal / drag-drop day reorder (S5 Plan editor rows; 9 missing wrappers in S4 4d). | 4 sub-plans recommended: 24-01 plan CRUD + activate; 24-02 day CRUD + day-reorder; 24-03 exercise CRUD modal; 24-04 day-level drag-and-drop polish. | 23 (dashboard surfaces a "Plan duzenle" CTA) |
| 25 session-execution | expand + split | Set logging UI exists but `rpe` not captured; IndexedDB queue + drain unwired in production code (S5 "IndexedDB queue" Partial; "Drain on online event" Stub). `clientSetId` idempotency missing (S4 4d). Edit-existing-set wrapper missing. ApiError typed-codes (`SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`) not branched (S4 4d). | 4 sub-plans recommended: 25-01 add `clientSetId` + Zod schema for `addSet`; 25-02 wire IndexedDB enqueue + online-drain inside `session-client.tsx`; 25-03 typed-error branching for the four ApiError codes; 25-04 RPE input + edit-existing-set. | 22 (auth refresh stable), 24 (plan day data shape stable) |
| 26 exercise-catalog | keep-as-outlined OR narrow | Catalog list, filter, search, detail modal, detail page all `Implemented` (S5 Exercise catalog rows). Outline already overshipped. Recommend `narrow` to: empty-state polish, search-no-result UX, infinite scroll if pagination becomes painful. If nothing operationally needed, downgrade to `defer-v0.6`. | 0-1 | none |
| 27 history-view | keep-as-outlined OR narrow | Calendar + per-session detail done (S5 History rows). Recommend `narrow` to: link from per-session detail back into a "duplicate this workout" flow, OR `defer-v0.6` if the calendar is sufficient. | 0-1 | 25 (session detail unchanged) |
| 28 metrics-ui | narrow | Weight chart, weekly/monthly/all-time, measurement form, deletion all `Implemented` (S5 Metrics rows). Missing: chest/arm/thigh fields (`bodyMetricSchema` already supports them); 200/201 distinction in toast (S4 4d). Progress photo upload is `defer-v0.6`. | 1 | none |
| 29 profile-settings | merge with 22 OR narrow | Profile, supplements, webhook tokens all `Implemented` (S5 Profile rows). Recommend `merge` into 22 auth-pages: a single "auth+profile" phase covering logout + session-expired UX. If merge unwanted, downgrade to `narrow` for polish only (a11y, error states). | 0 | 22 |
| 30 export-import-ui | keep-as-outlined OR narrow | Full JSON download, restore, Claude summary, per-section, CSV, AI prepare, AI apply, Apple/Google/Garmin import all `Implemented` (S5 Export rows). Recommend `narrow` to: confirmation step before destructive import (current import overwrites without warning beyond text label), and a download-history list (last N exports cached locally). | 0-1 | none |

**Open issues that touch v0.5 phases:**
- i-5 (next-intl 3 -> 4) - stays deferred. v0.5 keeps next-intl 3.x. No phase blocked by it.
- i-6 (Next 15 -> 16) - stays deferred. Phase 22-30 can ship on Next 15.x. The plan note `i-6` should be re-considered post-v0.5 once major-bump bandwidth opens (v1.0 Phase 40).

**Net-new phases the outline missed (recommend insert):**
- `phase-22.5-frontend-error-state-baseline` - typed `ApiError.code` branching utility shared by `/dashboard`, `/session/[id]`, `/metrics`. Today only `(auth)/login/page.tsx:38-44` reads `error.status`; no consumer reads `error.body.code`. A small shared `mapApiError(error)` helper unblocks 25-03 and prevents drift across phases. Section 4 4d evidence.
- `phase-25.5-session-set-zod-schema` - replace inline `AddSetPayload` (`endpoints.ts:139-147`) and `ImportResult` (`endpoints.ts:342-351`) with Zod schemas in `schemas.ts` so the wire format is single-sourced. Section 4 4d evidence.
- `phase-29.5-middleware-route-coverage` - middleware matcher (`middleware.ts:42-54`) lacks `/achievements/:path*`. Server fetches still require Bearer, but unauthenticated visitors hit a render before redirect. Section 3c evidence.

**Phases to advance (potentially v0.6 / v1.0):**
- 26 (exercise-catalog) and 27 (history-view) and 30 (export-import-ui) over-shipped. If `narrow` recommendations land in their sibling phases, these three could be `defer-v0.6` (polish-only milestone) instead.
- 32 pwa-polish (v0.6 outline) - already partially met by `frontend/public/manifest.webmanifest` + `frontend/public/sw.js` (Section 3c). Recommend a thin v0.6 phase that adds install prompt + visual polish only.

---

## Section 7 - Frontend Test Gate Posture

**`frontend/package.json` scripts (verbatim, lines 7-17):**
```
"dev": "next dev",
"build": "next build",
"start": "next start",
"lint": "eslint .",
"typecheck": "tsc --noEmit",
"test": "vitest run",
"test:coverage": "vitest run --coverage",
"test:watch": "vitest",
"test:e2e": "playwright test"
```

**`frontend/playwright.config.ts` projects (verbatim, lines 22-27):**
```
projects: [
  {
    name: "chromium",
    use: { ...devices["Desktop Chrome"] },
  },
],
```
baseURL: `process.env.E2E_WEB_BASE_URL ?? "http://localhost:3000"`. testDir: `./e2e`. Workers: 1, retries: 2 in CI. Single Chromium project; responsive 360 spec overrides viewport per-test.

**CI wiring (`.github/workflows/ci.yml`):**
| Script | Wired into CI? | Job (line) |
|---|---|---|
| `pnpm lint` | yes | `frontend.Lint` (line 58) |
| `pnpm typecheck` | yes | `frontend.Typecheck` (line 62) |
| `pnpm test:coverage` | yes | `frontend.Unit tests with coverage` (line 66) |
| `pnpm build` | no (CI does not run a frontend build; trivy_image builds via Docker) | n/a |
| `pnpm test:e2e` (Playwright) | no | not in `ci.yml` - runs locally only |
| `pnpm test:watch` | no | n/a (dev-only) |

**Routes with `Implemented` status (S5) but no neighbor `*.test.tsx`:**
- `/exercises/[id]` - `exercise-detail-client.tsx` has no test (Section 2 row 7).
- `/profile` - top-level `profile-client.tsx` has a test, but `webhook-tokens-section.tsx` test count is 2 and the underlying token URL builder (`buildScaleUrl`) lacks a unit test for SSR path.
- Most lib modules under `frontend/src/lib/` have no tests: `endpoints.ts` (591 lines, 40+ wrappers), `schemas.ts` (332 lines), `token-store.ts`, `subscribe.ts`, `rest-timer.ts`, `locale.ts`, `today.ts`. Only `client.ts` and `offline/session-set-queue.ts` are unit-tested.
- `Nav` and `BottomNav` and `Heatmap` components have no test (Section 3a).

**Recommended test additions per future v0.5 phase:**

| Phase | Test type | Target | Blocker / fixture need |
|---|---|---|---|
| 22 auth-pages | vitest component | logout button in (app) layout drawer | needs `Providers` test wrapper with mocked QueryClient |
| 22 auth-pages | Playwright e2e | session-expired -> auto-redirect-to-login flow | needs Playwright fixture that pre-seeds an expired refresh token |
| 23 dashboard | vitest component | weekly-summary aggregator | needs MSW mock for `/api/sessions/history` |
| 24 plan-editor (24-01..24-04) | vitest component | plan create / activate flows; day add/remove; exercise CRUD modal | needs MSW handlers per new wrapper |
| 25 session-execution (25-01) | vitest unit | `addSet` Zod schema with `clientSetId` | none |
| 25 session-execution (25-02) | vitest integration | `session-client` enqueue path during offline; drain on online event | needs `fake-indexeddb` (already a devDep) + jsdom `dispatchEvent("online")` |
| 25 session-execution (25-03) | vitest component | typed ApiError code branching for 4 codes (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`) | needs `mapApiError` helper from net-new phase 22.5 |
| 25 session-execution (25-04) | vitest component | RPE input renders + sends; edit-existing-set | needs MSW handler for `PUT /api/sessions/{id}/sets/{setId}` |
| 26 exercise-catalog | vitest component | empty-state / no-result rendering | low blocker |
| 27 history-view | Playwright e2e | calendar -> session detail -> back to history navigation | needs admin-seeded fixtures |
| 28 metrics-ui | vitest component | chest/arm/thigh form fields persist + display | none |
| 28 metrics-ui | vitest unit | `upsertMetric` 200 vs 201 toast differentiation | needs MSW two-status mock; wrapper change required first |
| 29 profile-settings (or merge into 22) | vitest a11y | label/input pairing on `Field` helper | needs `@testing-library/jest-dom` toHaveAccessibleName |
| 30 export-import-ui | vitest component | confirm dialog before destructive import | none |
| 30 export-import-ui | Playwright e2e | full export round-trip: download JSON, re-import | needs admin fixture + file-system tmpdir for the download |
| 22.5 (net-new) | vitest unit | `mapApiError` helper | none |
| 25.5 (net-new) | vitest unit | Zod schemas for `AddSetPayload`, `ImportResult` | none |
| 29.5 (net-new) | vitest unit | middleware matcher includes `/achievements/:path*` | needs Next middleware test scaffold (currently absent) |
