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
