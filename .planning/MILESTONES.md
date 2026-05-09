# Project Milestones: WorkoutHub

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
