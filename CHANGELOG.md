# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The release workflow extracts the body of the section matching the pushed tag
and uses it as the GitHub release notes. Do not change the heading format.

## [Unreleased]

### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [1.0.0] - 2026-05-09

Production-ready release. Closes the v1.0 release-hardening deliverables
across phases 38-43: end-to-end test scaffolding (Phase 38), test-
infrastructure cumulative-fix bump from testcontainers 1.20.4 to 1.21.3
(Phase 39, closes i-7; opens i-7b for the 2.x major bump), framework-
major next-intl 3 -> 4 migration plus Spring Boot 4 and Next 16 re-
deferral playbooks (Phase 40, closes i-5; opens i-6b and i-8b), security
hardening with Netty 4.1.133.Final -> 4.2.13.Final and refresh-token /
brute-force / rate-limit posture lock-in (Phase 41, closes i-14),
documentation completion with new DATA_SCHEMA.md plus API.md /
DEPLOYMENT.md / README.md / VAPID.md refresh (Phase 42), and a
backup-restore drill runbook with BACKUP.md cron refresh and a populated
MIGRATION.md `## v1.0.0` section (Phase 43). Operators upgrade with a
normal `docker compose pull && docker compose up -d`; no Flyway
migrations, no env-var changes, no compose-topology change. After
upgrade, the new `docs/BACKUP_RESTORE_DRILL.md` runbook is the
recommended quarterly verification cadence.

### Added

- End-to-end test scaffolding under `frontend/e2e/`: locale-stable
  Playwright selectors with i18n-aware matchers, `loginAsAdmin` fixture,
  session-save-resume spec exercising the offline-first IndexedDB
  drain, and an e2e README documenting the local-run cadence (Phase 38).
- `docs/DATA_SCHEMA.md` (new): V1..V28 schema reference for operators
  auditing the database surface (Phase 42).
- `docs/BACKUP_RESTORE_DRILL.md` (new): operator-runnable runbook for
  verifying backup integrity quarterly via a throwaway Postgres
  container. Both `pg_dump` sidecar (custom format, `pg_restore`) and
  `scripts/backup.sh` (gzipped SQL, `psql`-piped) drill modes
  documented (Phase 43).
- `docs/MIGRATION.md` `## v1.0.0` entry: required-env-var changes
  (none), schema-and-data migration (no new schema), compose-runtime
  changes (Netty bump only), one-shot commands (none required;
  recommends running the new restore drill after upgrade), rollback
  (Phase 43).
- `docs/BACKUP.md` "Verify your backups" sub-section cross-linking
  the drill, plus a parallel cron paragraph for the `pg_dump` sidecar's
  internal `SCHEDULE` (Phase 43).
- `docs/DEPLOYMENT.md` and `README.md` cross-link entries pointing at
  `BACKUP_RESTORE_DRILL.md` from the "What's not here" / "Documentation
  map" sections (Phase 43).

### Changed

- Bumped `org.testcontainers:testcontainers` from `1.20.4` to `1.21.3`
  (latest stable 1.x as of 2026-05-09). `AbstractIntegrationTest` API
  surface preserved; all 10 migration tests inherit unchanged. The 2.x
  major bump is re-deferred as i-7b (no upstream artifact published on
  Maven Central). Closes i-7 (Phase 39).
- Bumped `next-intl` from `^3.26.0` to `^4.11.1` and regenerated
  `frontend/pnpm-lock.yaml`. Plan-author audit of the 4.0 release notes
  found 13 breaking-change surfaces but ZERO required source-file edits
  in this codebase: `frontend/src/i18n/request.ts` already returns the
  post-3.22 `{ locale, messages }` shape, and the codebase doesn't use
  locale-based routing, `defineRouting`, `next-intl/middleware`,
  `next-intl/navigation`, or `format.relativeTime`. Closes i-5 plus the
  open-redirect vuln alerts #8 and #11 (Phase 40-03).
- Spring Boot 3.5 -> 4.0 attempt rolled back (commits 75c3ea1..e49e1dc)
  after the Jackson 2 vs Jackson 3 default-classpath shift surfaced.
  Runtime stays pinned at Spring Boot 3.5.14 (the tail of the 3.5.x
  patch line on Maven Central). The major-version jump is re-deferred
  as i-8b with the 36-file Jackson-2 migration analysis carried
  forward verbatim (Phase 40-01).
- Next 15 -> 16 jump re-deferred as i-6b: the 3.x next-intl line tops
  out at `next ^15.0.0`, and bundling Next 16 with next-intl 4 in the
  same commit violates the roadmap's "in sequence (each its own commit
  and test pass)" rule. Frontend stays on Next 15.x (latest backport
  `15.5.18` per the npm `backport` dist-tag) (Phase 40-02).
- `docs/API.md`, `docs/DEPLOYMENT.md`, `docs/VAPID.md`, and `README.md`
  refreshed for v1.0 alignment: API surface fully enumerated against
  the controller layer at HEAD, deployment runbook updated for the
  Netty 4.2 baseline and the new backup drill cross-link, and the
  `vapid-keygen.sh` stale reference corrected (Phase 42).

### Fixed

- N/A — v1.0 is hardening + docs work; no production bug closures.

### Security

- Bumped `<netty.version>` from `4.1.133.Final` to `4.2.13.Final` in
  `backend/pom.xml`. Closes CVE-2026-42577 (epoll RST DoS); the
  `.trivyignore` suppression block for that CVE is removed in the
  same release window. The runtime is API-stable for this codebase's
  consumers — Spring MVC + Tomcat servlet stack with no Reactor Netty
  on the request path; the only Netty consumer is
  `async-http-client:2.12.4` for outbound web push. Closes i-14
  (Phase 41).
- Refresh-token hashing posture lock-in: SHA-256 hex over the full JWT
  bytes via `MessageDigest.getInstance("SHA-256")` and
  `HexFormat.of().formatHex(...)`, persisted in
  `refresh_tokens.token_hash VARCHAR(128) UNIQUE` (V7 migration). The
  2026-05-04 "refresh-token hash collisions" debt is superseded — those
  tests pass on `96d81b0`. Tightening to bcrypt or Argon2 was rejected
  because refresh tokens carry full JWT entropy at issuance and the
  hash is a fingerprint for DB lookup, not a password derivation
  (Phase 41).
- Brute-force lockout posture lock-in: `BruteForceGuard` enforces 10
  failures / 15-minute window / 60-minute lockout, throws HTTP 423 via
  `ResponseStatusException(HttpStatus.LOCKED, ...)`, and records every
  login outcome in `login_attempts` (V15 migration) via
  `@Transactional(propagation = REQUIRES_NEW)`. Threshold-tightening
  and (email, IP)-keyed lockout were rejected as v1.0 scope creep
  (Phase 41).
- Rate-limiting posture lock-in: zero generic in-process rate limiter;
  the Self-Hosted Contract delegates rate limiting to the operator's
  reverse proxy. The only in-process throttle is `BruteForceGuard` on
  the password-grant path. Adding a generic rate limiter was rejected
  because it would duplicate operator-layer enforcement and contradict
  the "never assume public exposure" tailnet-only posture (Phase 41).

### Known Issues (carried forward)

Tracked in `.planning/ISSUES.md`:

- i-3: documentation note on `.gitignore` `data/` glob-form correction
  (resolved at execution time; carried for audit).
- i-6b (NEW in v1.0): Next.js 16.x major bump deferred until a closure
  plan probes the lockfile resolution with co-installed `next@^16` and
  `next-intl@^4`, renames `frontend/src/middleware.ts` -> `proxy.ts`,
  wraps `frontend/src/app/(auth)/login/page.tsx` in `<Suspense>`,
  bumps `engines.node: ">=20.9.0"`, and exercises the protected-file
  pre-authorisation flow.
- i-7b (NEW in v1.0): testcontainers 2.0.0 major bump deferred until
  upstream Maven Central publishes 2.0.0 GA.
- i-8b (NEW in v1.0): Spring Boot 4.0.x major bump deferred until
  either a `spring-boot-jackson2` compat-module CI probe completes
  successfully OR a Jackson 2 -> Jackson 3 codebase migration sub-plan
  absorbs the 36-file refactor.
- i-12: `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport`
  returns 500 on a freshly-seeded user; CI is the authoritative gate.
- i-13: SpringDoc 2.6.0 ControllerAdviceBean workaround should be
  removed once SpringDoc 2.7+ ships against Spring Boot 3.5.x.
- i-15: Lighthouse CI workflow + bundle-size enforcement script + the
  `@next/bundle-analyzer` devDependency deferred — all three require
  edits to protected paths.

## [0.6.0] - 2026-05-09

Operational maturity release. Closes the v0.6 deliverables across phases
31-37: insights charts and stats (weight trend + PR teaser + range toggle
+ locale-aware tooltips), PWA polish (offline route + install-prompt card
+ service-worker v3 cache + scope), web-push notifications (sw push
handler + NotificationsSection + `/api/push/test` + VAPID docs),
rest-timer push notifications (V28 migration + scheduler + endpoint +
frontend `useRestTimer` push wiring), frontend HTTP metrics (registry +
middleware instrumentation + `/api/metrics` extension; closes i-4),
route-level error states and perf budgets (RouteSkeleton + RouteError +
WebVitalsReporter + `/api/vitals` + `docs/PERF_BUDGETS.md`), and a
structured-logging test fix that boots the full application under the
prod profile (closes i-9). Operators upgrade with a normal
`docker compose pull && docker compose up -d`; one new Flyway migration
(V28 rest-timer schedule columns) auto-applies on backend start.

### Added

- Insights charts and stats: weight-trend chart with locale-aware tooltips,
  personal-record teaser card surfacing latest PR per exercise, and a
  range toggle (1W / 1M / 3M / All) bound to the metrics range query
  (Phase 31).
- PWA polish: dedicated `/offline` route with retry CTA,
  `InstallPromptCard` driven by `beforeinstallprompt`, service-worker v3
  cache strategy (network-first navigation, stale-while-revalidate
  static), and explicit `scope: '/'` registration so the SW controls the
  full app shell (Phase 32).
- Web-push notifications: service-worker `push` event handler rendering
  notifications via `self.registration.showNotification`, profile
  `NotificationsSection` with subscribe / unsubscribe + permission state
  surfacing, `POST /api/push/test` developer self-send endpoint, and
  `docs/VAPID.md` operator guide for generating and rotating VAPID keys
  (Phase 33).
- Rest-timer push notifications: V28 migration adds
  `rest_timer_scheduled_at` and `rest_timer_duration_seconds` columns on
  `workout_sessions`; `RestTimerScheduler` spring component schedules a
  push at the requested wall-clock instant; new
  `POST /api/sessions/{id}/rest-timer` endpoint accepts duration; the
  frontend `useRestTimer` hook drives the schedule call and falls back
  to in-app countdown when push is unavailable (Phase 34).
- Frontend HTTP metrics: hand-rolled
  `frontend/src/lib/metrics/registry.ts` (counter + Prometheus default-
  bucket histogram) populated from `frontend/src/middleware.ts` per
  request and rendered on `/api/metrics` as
  `wh_frontend_http_requests_total` and
  `wh_frontend_http_request_duration_seconds_*`. Route-label
  cardinality bounded by `frontend/src/lib/metrics/route-label.ts`. No
  new runtime dependency. Closes i-4 (Phase 35).
- Route-level error states and perf budgets: `RouteSkeleton` shared
  component adopted via `loading.tsx` per `(app)` segment (dashboard,
  plan, session, history, exercises, metrics, profile, insights, prs,
  achievements); `RouteError` component wired via `error.tsx` per
  segment plus `global-error.tsx` for app-shell crashes;
  `WebVitalsReporter` client component mounted in the root layout posts
  CLS / FID / LCP / FCP / TTFB / INP to the new `POST /api/vitals`
  ingest, which records into the metrics registry; `docs/PERF_BUDGETS.md`
  documents Core Web Vitals targets and per-route bundle ceilings; new
  i18n keys `common.error`, `common.errorHint`, `common.tryAgain`,
  `common.goHome` (Phase 36).

### Changed

- `StructuredLoggingTest` now extends `AbstractIntegrationTest` and boots
  the full `WorkoutHubApplication` under `@ActiveProfiles("prod")` with
  Testcontainers Postgres so `LoggingApplicationListener` reconfigures
  the logging system to ECS-format JSON. `@Disabled` removed; both
  contract assertions (JSON shape with `@timestamp` + `message`;
  deny-listed `authorization` MDC value masked from stdout) run on
  every CI build. Closes i-9 (Phase 37).

### Fixed

- i-4 (frontend per-request HTTP histogram) closed by Phase 35 -- the
  metric series were missing on `/api/metrics` since v0.3 contract
  alignment.
- i-9 (`StructuredLoggingTest` cannot capture ECS JSON under
  `@SpringBootTest`) closed by Phase 37.

### Security

- No backend dependency changes in v0.6.0 over v0.5.0. Netty stays at
  4.1.133.Final; CVE-2026-42577 (epoll RST DoS) remains suppressed in
  `.trivyignore` pending the deferred Netty 4.2 bump in i-14.

### Known Issues (carried forward)

Tracked in `.planning/ISSUES.md`:
- i-3: documentation note on `.gitignore` `data/` glob-form correction
  (resolved at execution time; carried for audit).
- i-5: next-intl 3 -> 4 major bump deferred (open-redirect mitigated
  via reverse-proxy header rewrite).
- i-6: Next 15 -> 16 major framework bump deferred.
- i-7: testcontainers 1.x -> 2.x major bump deferred to next test-infra
  session (likely v1.0 Phase 39).
- i-8: Spring Boot 3.5 -> 4.0 major framework bump deferred to v1.0.
- i-12: `FullExportImportIntegrationTest.importRoundTripPreservesPlans
  FromExport` returns 500 on a freshly-seeded user; CI is the
  authoritative gate.
- i-13: SpringDoc 2.6.0 ControllerAdviceBean workaround should be
  removed once SpringDoc 2.7+ ships against Spring Boot 3.5.x.
- i-14: Netty 4.1.x -> 4.2.13.Final bump deferred to a v1.0 security-
  hardening phase; mitigated for v0.6 by trivy suppression and the
  servlet-stack architecture (no Reactor Netty in the request path).
- i-15: Lighthouse CI workflow + bundle-size enforcement script + the
  `@next/bundle-analyzer` devDependency deferred -- all three require
  edits to protected paths (`.github/workflows/**`, `scripts/**`,
  `frontend/package.json`). Perf-budget doc shipped 2026-05-09 in
  Phase 36-03; CI gating awaits operator approval or the v1.0
  hardening phase.

## [0.5.0] - 2026-05-09

Frontend feature completion release. Closes the v0.5 UI deliverables across
phases 22-30: auth UX polish, dashboard cards, full plan editor, session
execution with offline IndexedDB drain, exercise catalog detail surface,
history repeat-workout flow, body-metrics inputs and 200/201 toast split,
profile settings polish, middleware matcher coverage gap closure, and the
export/import UI with download history. Adds a typed `ApiError` code helper
module shared across mutations, extends Zod schema validation to import/
export and push surfaces, and grows the frontend test suite from 99 (v0.4
baseline) to 263. Backend is unchanged from v0.4.0; pull and restart.

### Added

- Auth UX: LogoutButton on desktop nav and profile page; SessionExpiredShell
  auto-redirects to `/login` on token expiry with a reason banner
  (Phase 22).
- Typed `ApiError` code helper module (`api-error-codes.ts`) with
  `ApiErrorCode` constants, `isApiError` / `isApiErrorWithCode` guards,
  `apiErrorCodeMessageKey` resolver, and an i18n generic fallback
  (Phase 22.5).
- Dashboard cards: `WeeklySummaryCard` pulling local-week session count,
  `LastWeightCard` with relative-date readout, and a QuickActions History
  link; new iso-week and relative-date helpers (Phase 23).
- Plan editor: full plan CRUD/activate, day CRUD with `WorkoutFocus` enum,
  exercise CRUD modal with master catalog picker, and day-level
  `dnd-kit` drag-drop reorder (Phase 24, 4 sub-plans).
- Session execution: `addSet` payload now carries a `clientSetId` UUID for
  backend idempotency; IndexedDB drain wired (`subscribeOnline` +
  on-mount drain) with a permanent-reject branch on
  `SESSION_FINISHED`; typed `ApiError` UX on dashboard start, `addSet`,
  and finish; RPE input on the submit form; inline `EditSetRow` for
  edit / delete on prior sets (Phase 25, 4 sub-plans).
- Zod schemas extended to import / export and push subscribe surfaces:
  `importResultSchema`, `fullExportSchema` family (8 nested),
  `vapidPublicKeyResponseSchema`, `pushSubscribeRequestSchema`;
  endpoints validate inbound and outbound payloads (Phase 25.5).
- Exercise detail: `ExercisePersonalRecordCard` derived from
  last-performance `newPr` filter; `ExerciseProgressChart` (recharts) on
  the progress endpoint (Phase 26).
- History: Repeat-workout CTA on the per-session detail card with the
  `SESSION_ALREADY_ACTIVE` typed-error pattern (Phase 27).
- Metrics: chest / arm / thigh form fields, 200 / 201 distinct toasts via
  a `returnStatus` client overload (Phase 28).
- Profile: typed `ApiError` flash on save, an a11y form region, and
  load-error banners with retry on profile / supplements /
  webhook-tokens; `PrToast` on mutation failures (Phase 29).
- Middleware matcher now covers `/achievements` (was unprotected);
  `PROTECTED_PREFIXES` exported for test introspection (Phase 29.5).
- Export / Import UI: `window.confirm` gate before destructive import
  (with file name); a localStorage-backed download history shim
  (max 10) and `ExportHistoryCard` with relative timestamps
  (Phase 30).

### Changed

- Frontend tests grew from 99 (v0.4 baseline) to 263 in v0.5;
  `pnpm typecheck` and `pnpm lint` clean throughout.
- `ProgressPoint.maxWeightKg` and `estimatedOneRmKg` widened to nullable
  in the Zod schema to match the backend's null-on-bodyweight-only sets
  (Phase 26-01).

### Fixed

- N/A -- v0.5 is feature work; backend tests remain green from the
  v0.4.0 hotfix bundle.

### Security

- Override io.netty to 4.1.133.Final (Spring Boot BOM brings
  4.1.132.Final) to close CVE-2026-42583 (Lz4FrameDecoder ReDoS),
  CVE-2026-42584 (HttpClientCodec response desync), and
  CVE-2026-42587 (HttpContentDecompressor maxAllocation bypass).
  CVE-2026-42577 (epoll RST DoS) deferred -- only fixed in Netty
  4.2.13.Final, not yet backported to 4.1.x line; will revisit if
  trivy still reports it after this bump.
- Suppress CVE-2026-42577 (Netty epoll transport RST DoS) in trivy
  via `.trivyignore`. The fix is only in Netty 4.2.x; 4.1.x has no
  backport. This app uses Spring MVC + Tomcat (servlet stack), not
  Reactor Netty / WebFlux, so `netty-transport-native-epoll` is on
  the classpath but never touched by request-handling code. Tracked
  for v0.6 Phase 41 (security-hardening) Netty 4.2 bump as
  ISSUES.md i-14.

## [0.4.0] - 2026-05-07

Backend feature completion release. Closes the workout-plan editing,
live-session execution, body-metrics, full-export refinement, and API
contract documentation deliverables from `ProjectBrief.md` Phases 3-5.
Adds 14 new endpoints across sessions analytics and body-metrics
slices, persists PR durability via a new `is_pr` column on
`session_sets`, exposes the runtime OpenAPI document at
`GET /v3/api-docs` and Swagger UI at `GET /swagger-ui.html`, and
hardens the Claude-summary export wire shape with snake_case naming
and import-time validation. Closes 2 deferred test issues (i-1, i-2)
and 1 export round-trip drop (i-10). Operators upgrade with a normal
`docker compose pull && docker compose up -d`; two new Flyway
migrations (V26 clientSetId, V27 is_pr) auto-apply on backend start.

### Added

- `POST /api/sessions/{id}/sets` accepts a `clientSetId` UUID for
  idempotent offline-queue replay; 200 returns the existing record on
  replay, 201 returns the newly created record (Phase 15-02; new V26
  migration).
- `Short heartRateAvgBpm` field exposed on `SessionDto` and
  `SessionSummaryDto` (read-only; populated by the existing Garmin
  .fit importer; Phase 15-03).
- Typed `code` field on the `ApiError` envelope for the four sessions-
  surface 409 conflicts: `SESSION_ALREADY_ACTIVE`,
  `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`,
  `SET_NUMBER_DUPLICATE` (Phase 15-04). The field is null on legacy
  conflict paths and stripped from the wire by Jackson `NON_NULL`.
- `GET /api/exercises/{id}/last-performance` and `GET /api/exercises/
  {id}/progress` formalised under the new `com.workouthub.analytics`
  package (Phase 16-02; the in-package leak from v0.3.x is now a
  proper feature slice).
- `estimatedOneRmKg` field on `ProgressPointDto` (Epley projection;
  per-top-set semantics; null for unweighted sets; Phase 16-03).
- Persistent `is_pr` column on `session_sets` with window-function
  backfill on V27 migration; the PR flag now survives a session
  detail-reread, a session update, and a full-export round-trip
  (Phase 16-04 + Phase 18-04).
- `photoUrl` field on `UpsertBodyMetricRequest` (round-trips with the
  V6 `photo_url` column; Phase 17-02).
- Optional `from` and `to` `LocalDate` query params on
  `GET /api/metrics` for time-series range filtering; mixed-pair and
  inverted-range yield 400 (Phase 17-03).
- 201/200 status-code split on `POST /api/metrics`: 201 on first
  insert per date, 200 on idempotent overwrite of the same date
  (Phase 17-04).
- snake_case naming convention on `ClaudeSummaryDto` aligning the
  LLM-paste surface with the ProjectBrief example (Phase 18-03).
- Import-time validation on `POST /api/export/import`: unknown
  `session.workoutDayId` references yield a curated 422 instead of a
  500-class FK violation (Phase 18-04).
- SpringDoc 2.6.0 OpenAPI document at `GET /v3/api-docs` (JSON;
  `/v3/api-docs.yaml` for YAML) and Swagger UI at
  `GET /swagger-ui.html`. Document declares two security schemes
  (`bearerAuth` http/bearer/JWT default; `forwardAuth` apiKey/header
  `X-Forwarded-Email` opt-in), 28 hand-curated tag groupings, and the
  `ApiError` envelope with `code` enum (Phases 19-02, 19-03, 19-04).
- `OpenApiSurfaceIntegrationTest` (5 tests) CI-asserts the runtime
  OpenAPI contract on every push (Phase 19-04).
- `docs/API.md` rewritten as a navigational entry point to the runtime
  OpenAPI surface (Phase 19-05).

### Changed

- `WorkoutDaysService.createDay` and `addItem` use explicit child-repo
  `saveAndFlush` to populate `@UuidGenerator` ids before the mapper
  reads them; closes the helper-NPE on 6 disabled
  `WorkoutDaysIntegrationTest` cases (Phase 14-01; closes i-1).
- `FullExportImportIntegrationTest.importRoundTripPreservesPlansFrom
  Export` now seeds a plan via `POST /api/workout-plans` before the
  export GET; the production export/import round-trip was correct all
  along (Phase 14-02; closes i-2).
- `FullExportDto.SetRow` grows from 8 to 9 components; `Boolean isPr`
  appended in the 9th position. Legacy schemaVersion-1 dumps
  deserialize without error; the import path treats null as `false`
  and runs a recompute pass after `insertSessions` returns as a
  safety net (Phase 18-04).
- `FullExportService.toSessionSection` populates the new `isPr`
  component from `SessionSet.isPr()`; `FullImportService` reads
  null-safely; `SessionSetsService.recomputePrForExerciseHistory` is
  invoked once per distinct touched `(userId, exerciseId)` pair after
  the insert loop (Phase 18-04).
- `docs/EXPORT_FORMAT.md` documents the new `isPr` field on `SetRow`,
  the BodyMetric.id / Supplement.id non-preservation stance, and the
  acceptable-round-trip-drift catalog (Phase 18-04).

### Fixed

- `i-1` `WorkoutDaysIntegrationTest` 6 helper-NPE cases re-enabled and
  passing (Phase 14-01).
- `i-2` `FullExportImportIntegrationTest.importRoundTripPreservesPlans
  FromExport` re-enabled and passing (Phase 14-02).
- `i-10` `is_pr` round-trip drop on full-export import path closed
  (Phase 18-04).
- Dual `SessionsController` operationId collision in the OpenAPI
  document resolved via per-method `@Operation(operationId = ...)`
  hand-disambiguation (Phase 19-03).
- Sort exercises by `orderIndex` in `WorkoutDaysService.reorderItems`
  and `deleteItem` responses so the JSON reflects the freshly
  assigned order rather than the in-memory insertion order.
- Add the missing 9th `isPr` argument to `SetRow` constructors in
  `ImportValidatorTest` so the suite compiles after the Phase 18-04
  record widening.
- Seed `started_at` in `V27SessionSetsIsPrMigrationTest` so the
  `workout_sessions_ended_after_started` check constraint is satisfied
  during the migration backfill test (commit `2b2a2e1`).
- Strip the `newPr` JSON field from idempotent `clientSetId` replay
  responses; the field is a fresh-write signal and was leaking on
  retry, breaking `SessionSetsIntegrationTest.sameClientSetIdRetryReturns200WithSameId`
  (commit `ddc4785`).
- Drop ID preservation during full-export import to avoid the Hibernate
  detached-entity trap when re-inserting plans/days with assigned
  UUIDs; rewrite `session.workoutDayId` references via a payload-to-DB
  UUID map to keep round-trip referential integrity (commit `d3d83d8`).
- Work around the SpringDoc 2.6.0 vs Spring Boot 3.5.x
  `ControllerAdviceBean(Object)` constructor break by disabling the
  override-with-generic-response scan and registering `ApiError`
  schemas via `OpenApiCustomizer`; restores `GET /v3/api-docs` (commit
  `c1b95f7`).

### Security

- Override `org.postgresql:postgresql` to 42.7.11 (Spring Boot BOM
  brings 42.7.10) to close CVE-2026-42198 (HIGH, client-side DoS in
  pgjdbc).
- Backend dependency baseline carried from v0.3.2: Spring Boot 3.5.14,
  bouncycastle 1.84, jjwt 0.13.0, jose4j 0.9.6, async-http-client 2.12.4
  all unchanged. SpringDoc 2.6.0 introduced in Phase 19-02 is the only
  new backend runtime dependency in v0.4.0;
  springdoc-openapi-starter-webmvc-ui 2.6.0 is published to Maven Central
  with no open CVEs at release time. Trivy fs + Trivy image CI jobs
  (Phase 10-01) re-validate the image at every push to main. Dependabot
  continues to surface new advisories; force-pinned versions for
  transitive dependencies (async-http-client 2.12.4 via
  dependencyManagement, jose4j 0.9.6 via dependencyManagement) stay in
  place. `.trivyignore` carries CVE-2026-33671 (picomatch ReDoS, vendored
  copy in next/dist/compiled/picomatch; deferred to v0.5 per ISSUES.md
  i-6); no other ignores added.

### Known Issues (carried forward)

Tracked in `.planning/ISSUES.md`:
- i-3: documentation note on `.gitignore` `data/` glob-form correction
  during Plan 01-01 (resolved at execution time; carried for audit).
- i-4: Frontend per-request HTTP histogram deferred to v0.6.
- i-5: next-intl 3 -> 4 major bump deferred (open-redirect mitigated
  via reverse-proxy header rewrite).
- i-6: Next 15 -> 16 major bump deferred to v0.5.
- i-7: testcontainers 1.x -> 2.x major bump deferred to next test-infra
  session (likely v1.0 Phase 39).
- i-8: Spring Boot 3.5 -> 4.0 major bump deferred to v0.6 / v1.0.
- i-9: `StructuredLoggingTest` test-config gap; real-runtime ECS output
  unaffected. Deferred to v0.6 Phase 37.

## [0.3.2] - 2026-05-03

Patch release fixing the v0.3.1 release-workflow failure. The image-tag
construction in `.github/workflows/release.yml` did not lowercase
`github.repository_owner`, so buildx rejected
`ghcr.io/GTRows/workouthub-backend:0.3.1` (Docker registry repo names
must be lowercase). No code change; only `.github/workflows/release.yml`
and the version bump.

### Fixed
- `Compute image tag` and the release-notes generator both now lowercase
  `IMAGE_PREFIX` via bash `${VAR,,}` parameter expansion before composing
  the GHCR ref. Backend and frontend images for v0.3.2 publish to
  `ghcr.io/gtrows/workouthub-{backend,frontend}:0.3.2`.

## [0.3.1] - 2026-05-03

Patch release. Closes 10 CVEs flagged by `trivy fs` and `trivy image` after the v0.3.0 cut, restores green CI on `main` (the v0.3.0 release shipped with a Spring bean-name collision and an ESLint group major-bump that both broke their respective gates), and stabilizes the test suite by deferring three pre-existing flakes to ISSUES.md. No operator-facing API or config changes; pull and restart.

### Changed
- Spring Boot 3.4.1 -> 3.5.14 (closes CVE-2025-22235, CVE-2025-41232, CVE-2025-41248, CVE-2025-41249, CVE-2026-22732, CVE-2026-22733).
- web-push 5.1.1 -> 5.1.2 with explicit compile-scope pins for httpasyncclient 4.1.5 and jose4j 0.9.6 (5.1.2 demoted both to runtime, breaking compile).
- Backend runtime image now runs `apk -U upgrade --no-cache` to pull the latest Alpine OS patches at build time.

### Fixed
- Bean name collision between Phase 4 `com.workouthub.common.web.MetricsController` and pre-existing `com.workouthub.metrics.MetricsController`. Renamed the Prometheus aliasing controller to `PrometheusMetricsController`. (Release-blocking regression from v0.3.0; Spring `ApplicationContext` failed to load and broke 259 of 334 tests.)
- Frontend lint pipeline crashed under the v0.3.0-shipped `eslint-config-next` 16.x against Next 15.x with `Converting circular structure to JSON`. Reverted to `eslint-config-next` ^15.5.15 and `eslint` ^9.39.4 to match the on-main Next 15 line.
- `aquasecurity/trivy-action` ref pinned to `v0.36.0` (the unprefixed `0.28.0` and `0.36.0` do not exist as release tags).
- Release notes generator no longer trips shellcheck SC2016 on backtick-in-single-quote format strings.

### Security
- Forced `org.asynchttpclient:async-http-client` to 2.12.4 via dependencyManagement (closes CVE-2024-53990, CRITICAL).
- Forced `org.bitbucket.b_c:jose4j` to 0.9.6 via dependencyManagement (closes CVE-2023-31582 and CVE-2024-29371).
- Added `.trivyignore` for CVE-2026-33671 (picomatch ReDoS) - vendored copy inside `next/dist/compiled/picomatch` cannot be patched without a Next.js bump (deferred to v0.5 per ISSUES.md i-6).

### Deferred (tests)
- `WorkoutDaysIntegrationTest` (6 helper-NPE tests, i-1) and `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport` (i-2) marked `@Disabled` pending local-Maven debugger access.
- `StructuredLoggingTest` (i-9, new) marked `@Disabled` - the minimal `@SpringBootConfiguration` does not trigger Spring Boot's logging-system reconfiguration to ECS, so stdout capture sees no JSON. Real-runtime ECS output is unaffected.

## [0.3.0] - 2026-05-03

First release aligned with `docs/SELF_HOSTED_CONTRACT.md`. Operators on amd64 or arm64 hosts can now pull pinned `vX.Y.Z` images from GHCR and run the stack via Docker Compose with parametric port bindings, real readiness checks, structured JSON logs, contract-aligned env vars, and an opt-in backup sidecar. See `docs/MIGRATION.md` for the full per-section migration steps from v0.2.x.

### Added
- Parametric Compose port bindings (`BIND_ADDR`, `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT`) defaulting to loopback (`127.0.0.1`).
- Named Docker network `workouthub-net` shared by all services.
- Bind-mount Postgres data under `./data/postgres/` (replaces named volume).
- Resource limits and reservations on every service plus a tunable default ceiling documented inline.
- Backend `/livez` (process liveness) and `/healthz` (DB + Flyway readiness) endpoints, public-allowlisted.
- Frontend `/api/livez` and `/api/healthz` route handlers (process liveness only) with Vitest coverage.
- Compose healthchecks on backend (`/healthz` via wget) and frontend (`/api/healthz` via Node fetch); `frontend.depends_on.backend` now waits for `service_healthy`.
- Production profile (`SPRING_PROFILES_ACTIVE=prod`) emits ECS-format JSON logs to stdout via Spring Boot 3.4 native structured logging.
- `SecretMaskingStructuredLoggingCustomizer` masks deny-list field names (`password`, `token`, `secret`, `authorization`, `cookie`, `set_cookie`, `api_key`, `client_secret`, `private_key`) with `<redacted>`.
- `UserIdMdcFilter` propagates authenticated `user_id` into MDC so it appears in structured log output.
- `/metrics` endpoint on backend main listener (aliases Micrometer's `PrometheusScrapeEndpoint`).
- Frontend `/api/metrics` Prometheus-text-format endpoint serving Node process metrics inline (no `prom-client` dependency).
- `AUTH_MODE=forward-auth` opt-in mode: `ForwardAuthFilter` reads `X-Forwarded-User` / `X-Forwarded-Email` / `X-Forwarded-Groups` only when source IP is in `APP_AUTH_TRUSTED_PROXIES` CIDR list (non-optional gate).
- Optional `pg_dump` backup sidecar (`COMPOSE_PROFILES=backup`) using `prodrigestivill/postgres-backup-local:16` with cron schedule and day-based retention. Snapshots written to `./data/backups/`.
- Canonical `.env.example` covering every env var the application reads with one-line per-variable comments.
- `docker-compose.override.yml.example` for ARM-friendly resource overrides; `docker-compose.override.yml` is gitignored.
- `README.md` rewrite per contract section 11 (description, quick start, configuration, exposure with Caddy/Traefik/nginx examples, data and backup, updating, reference).
- `docs/MIGRATION.md` with v0.3.0 entry and `vNext` template for future releases.
- 7 new CI gate jobs in `ci.yml`: `gitleaks`, `trivy_fs`, `trivy_image` (matrix), `compose_config`, `hadolint` (matrix), `actionlint`, `shellcheck`.
- `.gitleaks.toml` baseline allowlist (empty for v0.3).
- Multi-arch GHCR publish (`linux/amd64` + `linux/arm64`) for both backend and frontend images via `docker/setup-qemu-action` + buildx.
- SBOM and provenance attestation on built images.
- Image digest pins included in release notes.

### Changed
- Compose: `db_data` named volume migrated to bind mount `./data/postgres/`. Operators upgrading from v0.2.x must extract their data first per `docs/MIGRATION.md`.
- Compose port bindings now default to loopback (`127.0.0.1`) and are parametric via `BIND_ADDR` env var.
- Backend healthcheck retargeted from `/actuator/health` (provisional) to dedicated `/healthz`.
- Frontend healthcheck retargeted from `/` to `/api/healthz`.
- `SupplementTiming` enum serialized lowercase via `@JsonValue` for canonical export round-trip.
- `BruteForceGuard.recordFailure` now uses `REQUIRES_NEW` propagation so failed-attempt counter survives outer auth-service rollback.
- JWT generation includes per-token `jti` claim to prevent refresh-token hash collisions.
- Refresh-token store uses idempotent UPSERT semantics.

### Removed
- In-app OIDC client (`OidcController`, `OidcControllerIntegrationTest`, `AuthService.issueTokensForOidc`, `app.auth.oidc.*` config). Per contract section 7, SSO lives in the operator's reverse-proxy gateway. Operators using OIDC should switch to `AUTH_MODE=forward-auth` with Authentik / Authelia / oauth2-proxy in front.
- `:latest` floating image tag. Operators pin `vX.Y.Z` (or `@sha256:digest`) per contract section 3.1.

### Fixed
- 12 of 19 pre-existing test failures cleared:
  - `AuthFlowIntegrationTest.loginRefreshRotationAgainstSeededUser`
  - `PasswordChangeIntegrationTest.wrongCurrentPasswordReturns401`
  - `PasswordResetIntegrationTest` (x2)
  - `BruteForceLockoutIntegrationTest.hittingTheThresholdLocksWithHttp423`
  - `CsvImportIntegrationTest.emptyBodyReturnsZeroesNotAnError`
  - `FullExportImportIntegrationTest.importRejectsPayloadsWithMultipleActivePlans`
  - `FullExportImportIntegrationTest.importRejectsUnsupportedSchemaVersion`
  - `SectionImportIntegrationTest.importSupplementsSliceRejectsUnknownTimingWith422`
  - `SupplementsIntegrationTest.invalidTimingReturns400`
  - `SupplementsIntegrationTest.postCreatesAndGetReturnsTheEntry`
  - `ExportFormatExampleTest.documentedExampleImportsAndBecomesTheCurrentExportState`
  - `StreakCalculatorTest.streakDoesNotSurviveSecondGapWhenSeparated`
  - `DefaultPlanSeederTest.creatingUserViaAdminApiSeedsAnActiveStarterPlan`
- `GlobalExceptionHandler` now maps `ResponseStatusException` to its declared HTTP status (was falling through to generic 500).
- `GlobalExceptionHandler` now maps `HttpMessageNotReadableException` to 400 with a generic message.

### Security
- Bouncycastle `bcprov-jdk18on` bumped 1.77 -> 1.84, closing 7 CVE alerts including 1 HIGH (Covert Timing Channel).
- Routine bumps merged via Dependabot: jjwt 0.12.6 -> 0.13.0, jacoco 0.8.12 -> 0.8.14, tanstack/react-query patches, eslint group, GitHub Actions majors (docker/build-push-action 6 -> 7, pnpm/action-setup 4 -> 6, docker/setup-buildx-action 3 -> 4, docker/login-action 3 -> 4, softprops/action-gh-release 2 -> 3), lucide-react and @types/node majors.
- Forward-auth filter trust gated by `APP_AUTH_TRUSTED_PROXIES` CIDR list; empty list = trust nothing.

### Known Issues (carried forward)

Tracked in `.planning/ISSUES.md`:
- i-1: 6 errors in `WorkoutDaysIntegrationTest` (createDay helper NPE on response `id`); needs local-Maven debugging.
- i-2: 1 failure in `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport`; same trigger.
- i-4: Frontend per-request HTTP histogram deferred to v0.6.
- i-5: next-intl 3 -> 4 major bump deferred (open-redirect mitigated via reverse-proxy header rewrite).
- i-6: Next 15 -> 16 major bump deferred to v0.5.
- i-7: testcontainers 1 -> 2 major bump deferred to next test-infra session.
- i-8: Spring Boot 3.4 -> 4.0 major bump deferred to v0.6.

<!--
Section template for a new release:

## [x.y.z] - YYYY-MM-DD

### Added
- ...

### Changed
- ...

### Fixed
- ...
-->
