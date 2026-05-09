# Project Milestones: WorkoutHub

## v1.0 Release Hardening (Shipped: 2026-05-09)

**Delivered:** Production-ready cut of WorkoutHub. Shipped seven phases (38-44) across 9 plans: Playwright critical-flow + responsive + session-save-resume specs hardened against the default Turkish locale with locale-stable selectors and a shared `loginAsAdmin` fixture (Phase 38), Testcontainers 1.20.4 -> 1.21.3 patch-level bump closing i-7 and opening i-7b for the upstream 2.0.0 GA (Phase 39), framework majors handled in three sub-plans (40-01 Spring Boot 4 attempt rolled back at 38c341e and re-deferred to i-8b after the Jackson 2 vs Jackson 3 default-classpath shift surfaced; 40-02 Next 16 deferred to i-6b after the next-intl 3.x peer-range audit; 40-03 next-intl 3.26 -> 4.11.1 landed clean closing i-5 with ZERO required source-file edits and closing the open-redirect vuln alerts #8 + #11), security hardening with Netty 4.1.133 -> 4.2.13.Final closing i-14 plus the trivy CVE-2026-42577 suppression removal plus a v1.0 audit lock-in for refresh-token SHA-256 hashing + `BruteForceGuard` lockout posture + operator-layer rate-limiting posture in STATE.md (Phase 41), docs-completion adding the V1..V28-derived `docs/DATA_SCHEMA.md` plus `docs/API.md` v1.0 review refresh plus `docs/DEPLOYMENT.md` env-var minimum + What's not here section plus README Documentation map + Versioning section (Phase 42), backup-restore drill landing `docs/BACKUP_RESTORE_DRILL.md` operator runbook plus `docs/BACKUP.md` cron primitive plus the populated `docs/MIGRATION.md ## v1.0.0` section preserving the template (Phase 43), and the v1.0.0 release cut at sha `cac37c1` with annotated tag `v1.0.0` created locally awaiting maintainer push for the multi-arch GHCR publish (Phase 44).

**Phases completed:** 38-44 (9 plans total: 6 single-plan phases + Phase 40 split into 3 sub-plans).

**Releases:** v1.0.0 (cut 2026-05-09 at sha `cac37c1`; annotated tag `v1.0.0` created locally; multi-arch GHCR publish queued for maintainer-side tag push per `IDENTITY.yaml#release.draft_first: true`).

**Key accomplishments:**

- Phase 38 e2e-tests: retrofitted `frontend/e2e/critical-flow.spec.ts` and `frontend/e2e/responsive-360.spec.ts` to locale-stable selectors so they pass against the default `tr` locale; added `frontend/e2e/fixtures/login-as-admin.ts` shared fixture; landed `frontend/e2e/session-save-resume.spec.ts` covering partial-progress IndexedDB-drain recovery; added `data-testid` hooks on session-execution set-row inputs; added `frontend/e2e/screenshots/` placeholder + ignore rule; documented the Playwright run cadence in `frontend/e2e/README.md` (commits c370c98..a2d0b4b).
- Phase 39 testcontainers-major: bumped `<testcontainers.version>` from `1.20.4` to `1.21.3` in `backend/pom.xml`; AbstractIntegrationTest API surface compatibility-preserved across the 1.20 -> 1.21 hop; closed i-7 and opened i-7b for the upstream 2.0.0 GA (commits 6328446..efc4af9 + plan artifact 3a11fd7).
- Phase 40-01 spring-boot-4: attempted Spring Boot 3.5.14 -> 4.0.6 at commits 75c3ea1..e49e1dc; the Jackson 2 vs Jackson 3 default-classpath shift in `spring-boot-starter-web` 4.0.6 (36 files reference `com.fasterxml.jackson.*`) blocked the bump; rolled back at 38c341e and replaced the plan with the i-8 -> i-8b re-defer bookkeeping at 4e3d95c..30b8d97. Closed i-8; opened i-8b with the two-path closure playbook (Path A `spring-boot-jackson2` compat module + HttpMessageConverter selection probe; Path B full Jackson 2 -> Jackson 3 codebase migration across 36 files) plus the trivial Jackson2-module + Prometheus-actuator package-relocation import fixes for the closure plan to apply alongside the chosen path.
- Phase 40-02 next-16: re-deferred to i-6b at 49edb5a..ea7d0d3 after the next-intl 3.x peer-range audit confirmed zero 3.x versions list `next ^16.0.0` in their peer range (the 3.26.x line tail at 3.26.5 tops out at `next ^15.0.0`). i-6b closure plan must probe the lockfile resolution + rename `frontend/src/middleware.ts` -> `proxy.ts` + wrap `frontend/src/app/(auth)/login/page.tsx` in `<Suspense>` + bump `engines.node ">=20.9.0"` + audit `next/image` callers for query-string `src` values + decide on Turbopack vs `--webpack`. Frontend runtime stays pinned at `^15.1.0` (latest 15.x backport `15.5.18` per the npm `backport` dist-tag at plan-author time).
- Phase 40-03 next-intl-4: bumped `next-intl` from `^3.26.0` to `^4.11.1` in `frontend/package.json` and regenerated `frontend/pnpm-lock.yaml` at 7817eb0; ZERO required source-file edits in this codebase because (a) `frontend/src/i18n/request.ts` already returns `{ locale, messages }` from `getRequestConfig` and `await requestLocale` (post-3.22 shape), (b) `<NextIntlClientProvider>` 4.x auto-inheritance is opt-in cleanup (the 27 explicit `messages={...}` call sites continue to work), (c) the codebase doesn't use locale-based routing, `defineRouting`, `next-intl/middleware`, `next-intl/navigation`, `format.relativeTime`, or any 3.x deprecated API removed in 4.0. Closes i-5 and the open-redirect vuln alerts #8 and #11. Forward-compatible with the eventual i-6b closure (next-intl 4.11.1 peers `next ^16.0.0`).
- Phase 41 security-hardening: bumped `<netty.version>` from `4.1.133.Final` to `4.2.13.Final` in `backend/pom.xml` (commit 99f0fb4); removed the `CVE-2026-42577` suppression block from `.trivyignore` (commit 5c1cd42); closed i-14 and locked the v1.0 Phase 41 audit findings (refresh-token SHA-256 hashing posture + `BruteForceGuard` 10-failure / 15-min / 60-min HTTP 423 lockout posture + zero generic in-process rate limiter / operator-layer-only rate-limiting posture) into `.planning/STATE.md` Locked-in Decisions (commits 99f0fb4..67885af + plan artifact 807ec6d). Concludes the trivy-suppression carry-forward debt from v0.5.0 + v0.6.0.
- Phase 42 docs-completion: authored `docs/DATA_SCHEMA.md` from V1..V28 (commit ba2833e); refreshed `docs/API.md` v1.0 review anchor + DATA_SCHEMA cross-link (f54f09d); refreshed `docs/DEPLOYMENT.md` env-var minimum + What's not here section (e21442f); added README Documentation map + Versioning section (d76a9bc); phase plan + summary at 23fd3d0.
- Phase 43 backup-restore-drill: authored `docs/BACKUP_RESTORE_DRILL.md` operator runbook (60b1a56); refreshed `docs/BACKUP.md` to document both backup-cron primitives plus a "Verify your backups" cross-link (bc32625); replaced `docs/MIGRATION.md ## vNext` placeholder with a populated `## v1.0.0` section preserving the template (c747a0d); cross-linked the drill from `docs/DEPLOYMENT.md` What's not here (cf7ba13) and the README Documentation map (0dd179e); phase plan + summary at 3b415a8. Five atomic commits 60b1a56..0dd179e under `docs(43-01)` scope, zero protected-file edits beyond the planning artifacts and the `docs/**` writes.
- Phase 44 release-v1-0: cut `v1.0.0` at sha `cac37c1` on 2026-05-09 21:36 +0300 with a single release commit (CHANGELOG rotation + IDENTITY/pom/package version bumps in lockstep; 139 line additions across 4 files: `IDENTITY.yaml`, `CHANGELOG.md`, `backend/pom.xml`, `frontend/package.json`); annotated tag `v1.0.0` created locally pointing at `cac37c1` with message "Release v1.0.0"; tag push to fire `.github/workflows/release.yml` is queued for maintainer-side execution per `IDENTITY.yaml#release.draft_first: true`. The maintainer-facing Publish click on the resulting draft GitHub Release is OUT of scope per `RELEASE.md` Post-release step.

**Stats:**

- 9 plans across 7 phases (6 single-plan phases + Phase 40 split into 3 sub-plans). No inserted decimal phases.
- Files modified across the milestone: 45 across the v0.6.0 -> v1.0.0 git range (+11230 / -210 line delta).
- Timeline: 2026-05-09 19:06 (v0.6 close commit 481c8a9) to 2026-05-09 21:36 (v1.0.0 cut at cac37c1) = ~2.5 hours real time inside a single working session.

**Git range:** `v0.6.0..v1.0.0` (42 commits). Tag: `v1.0.0` at `cac37c1`.

**What's next:** v1.0 is the final planned milestone in the GSD roadmap. v1.1+ scope (if opened) would address the carried-forward open issues: i-3 (`.gitignore` `data/` glob audit reference), i-6b (Next 16 jump), i-7b (Testcontainers 2.0.0 jump), i-8b (Spring Boot 4.0.x jump), i-13 (SpringDoc >= 2.7), i-15 (Lighthouse CI + bundle-size + bundle-analyzer perf-budget gating).

**Archive:** [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

---

## v0.6 Operational Maturity (Shipped: 2026-05-09)

**Delivered:** Real-world operational hardening per the v0.6 roadmap goal. Shipped seven phases (31-37) across 11 plans: insights surface re-composition with weight trend KPI + PR teaser + 4w/12w/26w volume/heatmap range toggle + locale-aware tooltips for volume/1RM charts on `/insights`, PWA polish with the long-missing `/offline` server-component shell route + `InstallPromptCard` capture of `beforeinstallprompt` on the dashboard + service-worker cache version bump (v1 -> v2) + explicit `scope: "/"` on the registrar, end-to-end web push notifications (sw.js `push` + `notificationclick` handlers + cache v2 -> v3 + `NotificationsSection` profile component + `POST /api/push/test` self-test endpoint + VAPID + reminder cron docs in `docs/DEPLOYMENT.md`), server-scheduled rest-timer push (V28 `rest_timer_schedules` migration + `RestTimerScheduleService` + `RestTimerScheduler` poller + `POST/DELETE /api/sessions/{id}/rest-timer` + Zod schemas + `useRestTimer` push wiring + cancel-on-next-set/finish/unmount + i18n keys), hand-rolled frontend Prometheus HTTP metrics (process-local registry + middleware instrumentation + `/api/metrics` extension exposing `wh_frontend_http_requests_total` + `wh_frontend_http_request_duration_seconds_*`; closed carried-forward i-4), uniform (app)-shell loading + error UX (RouteSkeleton + 15 loading.tsx + RouteError + 15 error.tsx + global-error.tsx) + Web Vitals reporter + `/api/vitals` ingest endpoint + `docs/PERF_BUDGETS.md` + new ISSUES.md i-15 for deferred Lighthouse CI gating, and the structured-logging test redesign so `StructuredLoggingTest` now extends `AbstractIntegrationTest` and boots `WorkoutHubApplication.class` under `@ActiveProfiles("prod")` so `LoggingApplicationListener` reconfigures the logging system to ECS-format JSON exactly as it does in `docker compose up` (closed carried-forward i-9).

**Phases completed:** 31-37 (11 plans total: 6 single-plan phases + Phase 36 split into 36-01 / 36-02 / 36-03).

**Releases:** v0.6.0 (cut 2026-05-09 at sha `08cc968`; multi-arch GHCR images at `ghcr.io/gtrows/workouthub-{backend,frontend}:0.6.0`; annotated tag `v0.6.0`; no post-release patch window opened).

**Key accomplishments:**

- Phase 31 charts-stats: 12 new top-level `insights` i18n keys (TR + EN parallel; `weightTitle/Description/Current/Delta/DeltaEmpty`, `weightRange.{seven,thirty,all}`, `range.{fourWeeks,twelveWeeks,twentySixWeeks}`, `volumeTooltipLabel`, `oneRmTooltipLabel`, `prTeaserTitle`, `prTeaserViewAll`); `ChartTooltip` helper with `Intl.NumberFormat(locale)` wired into volume + 1RM charts; shared 4w/12w/26w range toggle driving both `fetchWeeklyVolume(weeks)` and `fetchHeatmap(weeks)`; inline `WeightTrendCard` at top of `/insights` with current-weight + signed-delta + sparkline + 7/30/all delta-window toggle (reads from `fetchMetrics()`); inline `PrTeaserCard` at bottom rendering top 3 most-recent PRs by `achievedAt` desc with a `Link` to `/prs` (commits 36f743a..b26eef5).
- Phase 32 pwa-polish: 8 new `pwa` i18n keys (TR + EN); shipped the `/offline` server-component route (closes the latent SHELL precache 404 that prevented the v1 SW cache from ever installing cleanly); SW cache constant `wh-shell-v1` -> `wh-shell-v2`; explicit `scope: "/"` on `navigator.serviceWorker.register`; `InstallPromptCard` client component capturing `beforeinstallprompt` with `event.preventDefault()` + persisting dismissal via `wh.installPromptDismissed` localStorage flag + clearing on `appinstalled`; mounted at top of dashboard (renders null when not eligible); 7 new vitest it-blocks across 4 suites (commits b43e52f..54611bf).
- Phase 33 web-push-notifications: `push` + `notificationclick` handlers added to `frontend/public/sw.js` (push handler reads `{title, body, url}` payload + calls `registration.showNotification`; click handler navigates SW client to `event.notification.data.url`); SW cache constant bumped a second time (v2 -> v3); new `profile.notifications` and `push.test` i18n namespaces; `NotificationsSection` client component on the profile page surfacing permission state + enable/disable/test flows; `POST /api/push/test` backend endpoint dispatching a fresh push to every active subscription owned by the authenticated user (counts delivered/gone/failed); `app.push.vapid.{public-key,private-key,subject}` + reminder cron envs documented in `docs/DEPLOYMENT.md` (commits 8fdc1ef..b8aad60).
- Phase 34 rest-timer-notifications: V28 `rest_timer_schedules` table (UUID PK, `(user_id, session_id)` UNIQUE upsert key, `fire_at`, `localized_title/body`, `click_url`, `dispatched_at` sentinel, partial index on `(fire_at) WHERE dispatched_at IS NULL`); `RestTimerScheduleService` validates `seconds` 1..3600 + ownership-checks the session + upserts the row; `RestTimerScheduler` `@Scheduled(fixedDelayString = "${app.rest-timer.poll-interval-ms:1000}")` polling loop batch-claiming due rows + dispatching via the existing `NotificationDispatcher` interface from Phase 33 + a 5-minute cleanup tick deleting rows older than 1 hour past dispatch; `POST /api/sessions/{sessionId}/rest-timer` (201) and `DELETE /api/sessions/{sessionId}/rest-timer` (idempotent 204); `restTimerScheduleRequestSchema` + `restTimerScheduleResponseSchema` Zod parity; `scheduleRestTimer` + `cancelRestTimer` endpoint wrappers; `useRestTimer` extended so `start(seconds)` fires `scheduleRestTimer` and stop/next-set/finish/unmount fires `cancelRestTimer` (all fire-and-forget); `app.rest-timer.poll-interval-ms` + `app.rest-timer.cleanup-interval-ms` documented in `docs/DEPLOYMENT.md` (commits 9c074ce..546de24).
- Phase 35 frontend-http-metrics: hand-rolled `frontend/src/lib/metrics/registry.ts` (Counter Map + Histogram Map with Prometheus default bucket bounds, `record(method, route, status, durationMs)` writer, `renderProm()` reader emitting Prometheus 0.0.4 text); `frontend/src/lib/metrics/route-label.ts` normalizing dynamic segments to bounded label values; `frontend/src/middleware.ts` instrumented to capture start time pre-`NextResponse.next()` and call `record()` post-response; `frontend/src/app/api/metrics/route.ts` extended to render the counter + histogram block alongside process gauges. Metric families: `wh_frontend_http_requests_total{method, route, status_class}` and `wh_frontend_http_request_duration_seconds_{bucket,sum,count}{method, route}`. Closed carried-forward i-4 (commits f7ea729..a37dbcd).
- Phase 36 error-states-perf-budgets: 36-01 added the `Skeleton` shadcn-style primitive + `RouteSkeleton` variant component reusing `common.loading`; 15 `loading.tsx` files (one per (app)-group segment); migrated dashboard / plan / session / history / exercises / metrics / profile / insights / prs / achievements clients from inline loading paragraphs to the shared component (commit cluster ending at af8209e). 36-02 added the `common.error/errorHint/tryAgain/goHome` i18n keys + `RouteError` shared component + 15 `error.tsx` files + a top-level `global-error.tsx` for app-shell-level crashes (commit cluster ending at 018a047). 36-03 extended the metrics registry with `recordWebVital(name, route, value)` + matching histogram render block (`wh_frontend_web_vital_seconds` + `wh_frontend_web_vital_score`); shipped `frontend/src/app/api/vitals/route.ts` Node-runtime POST handler validating a Zod payload + recording into the registry; shipped `WebVitalsReporter` client component using Next 15's framework-native `useReportWebVitals` hook from `next/web-vitals` (no new dep) + `fetch("/api/vitals")` POST per metric event; mounted in root layout; added `docs/PERF_BUDGETS.md` recording numerical targets for LCP / CLS / INP / FCP / TTFB + per-route JS bundle ceiling + the next-action-on-regression procedure; captured deferred Lighthouse CI workflow + bundle-size script + `@next/bundle-analyzer` dep as new ISSUES.md entry i-15 (all three would require protected-file edits) (commit cluster ending at 02f550b).
- Phase 37 structured-logging-test-fix: `StructuredLoggingTest` redesigned to extend `AbstractIntegrationTest` with `@SpringBootTest(classes = WorkoutHubApplication.class, webEnvironment = NONE)` + `@ActiveProfiles("prod")` so `LoggingApplicationListener` reconfigures the logging system to ECS-format JSON exactly as in `docker compose up`; kept Spring Boot's `OutputCaptureExtension` (the only mechanism that wraps stdout before the Logback `ConsoleAppender` opens its `PrintStream` reference); `@Disabled` removed; both contract assertions (JSON shape with `@timestamp` + `message`; deny-listed `authorization` MDC value masked from stdout) now run on every CI build. Closed carried-forward i-9 (commits f0573b3..c884d9b).
- Cut `v0.6.0` release at sha `08cc968` with multi-arch GHCR images and the `## [0.6.0] - 2026-05-09` `CHANGELOG.md` block; annotated tag `v0.6.0` pushed to fire the release workflow; no post-release patch window opened.

**Stats:**

- 11 plans across 7 phases (6 single-plan phases + Phase 36 split into 3 sub-plans). No inserted decimal phases.
- Files modified across the milestone: 118 across the v0.5.0 -> v0.6.0 git range (+6103 / -167 line delta).
- Timeline: 2026-05-09 16:37 (v0.5 close commit a71ce38) to 2026-05-09 19:06 (v0.6.0 cut at 08cc968) = ~2.5 hours real time inside a single working session.

**Git range:** `v0.5.0..v0.6.0` (58 commits). Tag: `v0.6.0` at `08cc968`.

**What's next:** v1.0 Release Hardening - Phases 38-44: e2e-tests (Playwright critical flows), testcontainers-major (i-7), framework-majors (i-5/i-6/i-8 Spring Boot 4 + Next 16 + next-intl 4 in sequence), security-hardening (refresh-token hash collisions + brute-force lockout finalization + Netty 4.2 bump per i-14), docs-completion, backup-restore-drill, release-v1-0.

**Archive:** [milestones/v0.6-ROADMAP.md](milestones/v0.6-ROADMAP.md)

---

## v0.5 Frontend Completion (Shipped: 2026-05-09)

**Delivered:** Frontend feature surface complete per `ProjectBrief.md` Phases 4-6 (frontend slice). Shipped the user-facing app shell across 13 phases (10 main + 3 inserted decimals): frontend audit baseline, auth pages with typed-error UX and logout/session-expired handling, typed `ApiError.code` helper wired across the app, dashboard with today-card + weekly-summary + last-weight, plan editor with workout-plan + day + day-exercise CRUD and accessible day-level drag-drop reorder, session-execution screen with `clientSetId` UUID idempotency, IndexedDB drain on online event, typed 409 UX (SESSION_ALREADY_ACTIVE / SESSION_ALREADY_FINISHED / SESSION_FINISHED), RPE input + inline set edit/delete, Zod schema parity for ImportResult / FullExport / vapid / push-subscribe surfaces, exercise-catalog detail with PR card + progress chart, history-view per-session detail + Repeat-workout CTA, metrics-ui with chest/arm/thigh inputs + 200/201 toast split, profile-settings with load-error banners + a11y polish, the middleware-matcher route-coverage gate for `/achievements`, and export-import-ui with destructive-import confirm gate + bounded export-history shim.

**Phases completed:** 21-30 (19 plans total; 13 phases counting decimal phases 22.5 / 25.5 / 29.5).

**Releases:** v0.5.0 (cut 2026-05-09 at sha `5efaeb5`; multi-arch GHCR images at `ghcr.io/gtrows/workouthub-{backend,frontend}:0.5.0`; draft GitHub Release published; followed by a same-day Netty CVE patch window through `931cb69`).

**Key accomplishments:**

- Phase 21 audit (`.planning/phases/21-frontend-audit/21-01-AUDIT.md`) catalogued the full route tree, component layers, API client coverage matrix vs. the v0.4 backend, the ProjectBrief Phase 4-6 gap matrix, and the v0.5 test gate posture; net-new phases (22.5 typed-apierror-helper, 25.5 zod-schemas, 29.5 middleware-matcher-gap) were inserted from the audit's Section 6.
- Auth shell (Phase 22): `LogoutButton` mounted in desktop nav and profile page, `/login?reason=session-expired` redirect on token clear, session-expired alert banner, `nav.logout` + `sessionExpired` i18n keys.
- Typed ApiError helper (Phase 22.5, INSERTED): exposed `ApiError.code` field from the backend envelope, added typed-code helpers + i18n key map, `errors.api.generic` fallback translation; wired the helper across session-client mutations and dashboard start-mutation in Phase 25-03 (`SESSION_ALREADY_ACTIVE` UX).
- Dashboard (Phase 23): iso-week range helper, `WeeklySummaryCard` with `isInRange` aggregation, `LastWeightCard` with relative-date readout (`relative-date` helper), all mounted on the dashboard with i18n.
- Plan editor (Phase 24, 4 plans): full `PlanList` (create/rename/activate/delete) + `PlanRenameDialog`, `DayCard` + `DayEditDialog` with day-CRUD mutations and aligned focus enum casing, `DayExercisesSection` + `ExerciseEditDialog` with day-exercise CRUD, and `SortableDayGrid` with accessible day-level drag-drop reorder; new endpoint wrappers for workout-plan / workout-day / day-exercise CRUD and the `workoutFocus` schema.
- Session execution (Phase 25, 4 plans): added `clientSetId` UUID generation per submit and `addSetRequestSchema` with the field; wired IndexedDB drain on `online` event with offline-submit fallback and the permanent-reject branch in `drainForSession`; branched on typed ApiError codes (`SESSION_ALREADY_ACTIVE` / `SESSION_ALREADY_FINISHED` / `SESSION_FINISHED`) with i18n; added RPE input + inline edit/delete UX in `ExerciseBlock` plus `updateSet` / `deleteSet` wrappers and the `updateSetRequestSchema`.
- Zod schema parity (Phase 25.5, INSERTED): added `importResultSchema` mirroring backend `ImportResultDto`, `fullExportSchema` family mirroring `FullExportDto`, vapid + push subscribe schemas; wired all four into endpoint wrappers; aligned the export-client full-export fixture with the new schema.
- Exercise catalog detail (Phase 26): wrapped `GET /api/exercises/{id}/progress` with a Zod schema, added `ExerciseProgressChart` with locale-aware tooltip and `ExercisePersonalRecordCard` from last-performance bestSet; mounted both on the exercise detail page; added PR + progress-chart i18n keys.
- History view (Phase 27): per-session card gained the Repeat-workout CTA.
- Metrics UI (Phase 28): added `returnStatus` overload to the api request wrapper, `UpsertMetricResult` with `created` flag, chest/arm/thigh inputs and the 200/201 toast split (`toastCreated` / `toastUpdated` / `toastError`).
- Profile settings (Phase 29): wired the typed ApiError helper and a11y form region in `profile-client`, surfaced load-error banners + toast errors in `supplements-section` and `webhook-tokens-section`, added profile / supplements / webhook a11y polish error keys.
- Middleware matcher gate (Phase 29.5, INSERTED): closed the Next.js middleware route-coverage gap on `/achievements` (added to both `PROTECTED_PREFIXES` and `config.matcher` arrays in `frontend/src/middleware.ts`), shipped the first-ever `frontend/src/middleware.test.ts` vitest suite with 6 it-blocks asserting both arrays cover all 12 (app)-group segments and the redirect/pass-through behavior.
- Export/import UI (Phase 30): export-history `localStorage` shim with bounded entries + `useSyncExternalStore` snapshot caching, `ExportHistoryCard` component with relative timestamps and clear button, destructive-import confirm gate, download-history append on download; added export confirm + history i18n keys.
- Cut `v0.5.0` release at sha `5efaeb5` with multi-arch GHCR images and the `## [0.5.0] - 2026-05-09` `CHANGELOG.md` block; same-day patch window overrode Netty to `4.1.133.Final` to close CVE-2026-42583/42584/42587, and suppressed CVE-2026-42577 (Netty epoll, no 4.1.x backport, no runtime path) in trivy with the suppression noted in the changelog and tracked as i-14.

**Stats:**

- 19 plans across 13 phases (10 main: 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 + 3 inserted decimals: 22.5, 25.5, 29.5).
- Files modified across the milestone: 110 across the v0.4.0 -> v0.5.0 git range (+24390 / -364 line delta).
- Timeline: 2026-05-07 13:56 (v0.4 archive bookkeeping committed) to 2026-05-09 15:33 (v0.5.0 patch window close at 931cb69) = ~2 days, 1.5 hours including the same-day Netty CVE patch window.

**Git range:** `v0.4.0..v0.5.0` (118 commits). Tag: `v0.5.0` at `5efaeb5`.

**What's next:** v0.6 Operational Maturity - Phases 31-37: charts-stats (volume, 1RM Epley, weight change, frequency heatmap, PR list, streak), pwa-polish, web-push-notifications, rest-timer-notifications, frontend-http-metrics (i-4), error-states-perf-budgets, structured-logging-test-fix (i-9).

**Archive:** [milestones/v0.5-ROADMAP.md](milestones/v0.5-ROADMAP.md)

---

## v0.4 Backend Feature Completion (Shipped: 2026-05-07)

**Delivered:** Backend feature surface complete per `ProjectBrief.md` Phases 3-5. Shipped 14 new endpoints across the analytics + body-metrics slices, persisted PR durability via the new `is_pr` column on `session_sets`, hardened the offline-first sessions sync contract with `clientSetId` idempotency + typed 409 codes, refined the LLM-paste claude-summary surface to snake_case + the ProjectBrief field set, exposed the runtime OpenAPI document at `GET /v3/api-docs` with Swagger UI at `GET /swagger-ui.html`, and closed two carried-forward disabled-test debts (i-1, i-2).

**Phases completed:** 13-20 (25 plans total)

**Releases:** v0.4.0 (cut 2026-05-07; multi-arch GHCR images at `ghcr.io/gtrows/workouthub-{backend,frontend}:0.4.0`; draft GitHub Release published).

**Key accomplishments:**

- Closed pre-existing CI debt (i-1 cascade-id propagation in `WorkoutDaysService`, i-2 export round-trip plans seeding) via Phases 13-14; 6 `@Disabled` `WorkoutDaysIntegrationTest` cases re-enabled and the `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport` test back in the green suite.
- Hardened sessions API: `clientSetId` UUID idempotency key on `POST /sets` (V26 migration; 200 = replay, 201 = newly created), `Short heartRateAvgBpm` exposure on `SessionDto` / `SessionSummaryDto`, typed `code` field on `ApiError` for the 4 sessions 409 conflicts (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`), and persistent `is_pr` column on `session_sets` with V27 ROW_NUMBER backfill (Phases 15-16, 8 plans).
- Completed body-metrics module: `photoUrl` exposure on `UpsertBodyMetricRequest` (round-trips with V6 `photo_url` column), optional `from`/`to` `LocalDate` query params on `GET /api/metrics` for time-series filtering (mixed-pair / inverted-range yield 400), and 201/200 status-code split on `POST /api/metrics` via `MetricsService.UpsertResult(BodyMetricDto, boolean wasCreated)` wrapper (Phase 17, 4 plans).
- Refined export contract: `ClaudeSummaryDto` snake_case via per-record `@JsonNaming(SnakeCaseStrategy)` (Direction C; first `@JsonNaming` usage in `backend/src`), grew from 7 records / 27 components to 10 records / ~37 components matching the ProjectBrief example (`prs`, `bodyMetrics`, `consistency`, `user.age`, `goals` array, `summary.plannedWorkouts`/`adherencePercent`/`weightChangeKg`, `workouts.type` + `energy` rename), `FullExportDto.SetRow` 8 -> 9 components with `Boolean isPr` for round-trip durability, and `ImportValidator.validateSessionDayIds` rule (422 on stale `workoutDayId` reference) (Phase 18, 4 plans).
- Shipped SpringDoc 2.6.0 OpenAPI runtime at `GET /v3/api-docs` (JSON) + `/v3/api-docs.yaml` (YAML) + `/swagger-ui.html` (UI) with two security schemes (`bearerAuth` http/bearer/JWT global + `forwardAuth` apiKey/header `X-Forwarded-Email` alternative), 28 hand-curated `@Tag` groupings across user/admin-facing controllers, dual-`SessionsController` operationId disambiguation, `ApiError` envelope rendered with 11 `@Schema` annotations including the 4-value typed-code enum on `ApiError.code`, a 5-test `OpenApiSurfaceIntegrationTest` as the load-bearing CI runtime smoke, and the `docs/API.md` rewrite as a navigational entry point linking the runtime + per-section docs (Phase 19, 5 plans).
- Cut `v0.4.0` release with multi-arch GHCR images, draft GitHub Release, and the `## [0.4.0] - 2026-05-07` `CHANGELOG.md` block; security override of `org.postgresql:postgresql` to 42.7.11 closed CVE-2026-42198 (HIGH, client-side DoS) (Phase 20, 1 plan).

**Stats:**

- 25 plans across 8 phases.
- Files modified across the milestone: 155 across the v0.3.2 -> v0.4.0 git range (+18343 / -440 line delta).
- Timeline: 2026-05-03 23:02 (Phase 13 start) to 2026-05-07 02:11 (v0.4.0 cut) = ~3 days, 3 hours.

**Git range:** `v0.3.2..v0.4.0` (121 commits). Tag: `v0.4.0` at `c8fab14`.

**What's next:** v0.5 Frontend Completion - Phases 21-30: frontend-audit, auth pages, dashboard, plan editor, session-execution screen with IndexedDB drain wired to the v0.4 sync contract, history, exercise catalog, metrics UI, profile, export-import UI.

**Archive:** [milestones/v0.4-ROADMAP.md](milestones/v0.4-ROADMAP.md)

---

## v0.3 Self-Hosted Contract Alignment (Shipped: 2026-05-03)

**Delivered:** First contract-compliant self-hosted release. Operators on amd64 or arm64 hosts can pull pinned `vX.Y.Z` images from `ghcr.io/gtrows/workouthub-{backend,frontend}` and run the stack via Docker Compose with parametric port bindings, real readiness checks, structured JSON logs, contract-aligned env vars, and an opt-in backup sidecar.

**Phases completed:** 1-12 (14 plans total)

**Releases:** v0.3.0 (initial cut, draft release blocked by post-tag regression discovery), v0.3.1 (CVE wave + bean-collision fix, release workflow failed on GHCR tag casing), v0.3.2 (release-workflow lowercase fix, published cleanly).

**Key accomplishments:**

- Compose stack contract-compliant: parametric `BIND_ADDR` / `BACKEND_PORT` / `FRONTEND_PORT` / `POSTGRES_PORT`, named `workouthub-net` network, bind-mount `./data/postgres/`, healthchecks + `condition: service_healthy`, resource limits.
- Backend `/livez` (process) and `/healthz` (DB + Flyway readiness) endpoints; frontend `/api/healthz` + `/api/livez` route handlers (process liveness only).
- Spring Boot 3.4 native ECS structured logging on the `prod` profile with deny-list masking via `JsonWriter.Members#applyingValueProcessor` (no logstash-logback-encoder dep).
- Prometheus `/metrics` aliased on the main HTTP listener; `/actuator/prometheus` preserved for backward compat.
- `AUTH_MODE=forward-auth` filter gated by a `TRUSTED_PROXIES` CIDR list; OIDC client surface deleted entirely per contract section 7.
- Canonical `.env.example` (124 lines, 11 sections) and a shipped `docker-compose.override.yml.example` for ARM-friendly tuning.
- Opt-in `pg_dump` sidecar gated by the `backup` profile (off by default).
- 12 of 17 Dependabot PRs merged (1 HIGH bouncycastle CVE closed, 5 patches, 5 GH Actions majors, 2 dev-only majors); 4 breaking-change majors deferred via ISSUES.md i-5..i-8.
- README rewrite + `docs/MIGRATION.md` per contract section 11; per-version migration entries in place.
- 9-job CI gate suite (gitleaks, trivy fs, trivy image x2, hadolint x2, actionlint, shellcheck, docker compose config) plus the existing backend mvn verify and frontend lint+test+typecheck.
- Multi-arch (amd64 + arm64) GHCR publish via release workflow with no `:latest`, digest pins in release notes.
- Patch window v0.3.1+v0.3.2 closed 9 additional CVEs (Spring Boot 3.4.1 -> 3.5.14, web-push transitive overrides, Alpine OS upgrade) and fixed two release-blocking regressions (Spring bean-name collision, GHCR tag casing).

**Stats:**

- 14 plans across 12 phases, plus a ~24h post-release patch window with 17 follow-up commits (v0.3.0 -> v0.3.2).
- Files modified across the milestone: 30 (post-release window) + larger pre-tag set across all phases.
- Timeline: 2026-05-02 (Phase 1 start) to 2026-05-03 (v0.3.2 cut) = ~2 days for the core milestone, +1 day for the patch window.

**Git range:** `feat(01-01)` through `chore(release): v0.3.2`. Tags: `v0.3.0`, `v0.3.1`, `v0.3.2`.

**What's next:** v0.4 Backend Feature Completion - workouts, sessions, metrics, export modules per `ProjectBrief.md` phases 3-5. Phase numbering continues from 13.

**Archive:** [milestones/v0.3-ROADMAP.md](milestones/v0.3-ROADMAP.md)

---
