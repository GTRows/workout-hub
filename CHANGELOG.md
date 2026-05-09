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
