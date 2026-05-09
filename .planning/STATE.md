# State

## Current Position

Milestone: v1.0 Release Hardening (planned)
Phase: 38 of 44 (e2e-tests) - not started
Plan: 38-01 e2e-tests - not started
Status: v0.6 closed at v0.6.0 (tag 08cc968; 11/11 plans across Phases 31-37). No post-release patch window opened. Next: `/gsd:plan-phase 38-01` (e2e-tests) when v1.0 work begins.
Last activity: 2026-05-09 - v0.6 milestone bookkeeping closed; v0.6.0 tag at 08cc968

Progress: v1.0 ____________________  0% (0/7 phases planned)
          v1.0 - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/milestones/v0.5-ROADMAP.md` for full v0.5 archive
- See: `.planning/milestones/v0.6-ROADMAP.md` for full v0.6 archive
- See: `.planning/ROADMAP.md` for current roadmap (v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-5, i-6, i-7b, i-8b, i-13, i-14, i-15 open; i-1, i-2, i-4, i-7, i-8, i-9, i-10, i-12 closed)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** v1.0 release hardening - Playwright e2e tests (Phase 38), testcontainers 1.x -> 2.x major bump (Phase 39, i-7), framework majors (Phase 40, i-5 next-intl 4 + i-6 Next 16 + i-8 Spring Boot 4), security hardening (Phase 41, refresh-token hash collisions + brute-force lockout finalization + Netty 4.2 bump per i-14), docs completion (Phase 42), backup/restore drill (Phase 43), v1.0.0 release (Phase 44).

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4 + v0.5 + v0.6)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md`, `.planning/milestones/v0.4-ROADMAP.md`, `.planning/milestones/v0.5-ROADMAP.md`, and `.planning/milestones/v0.6-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v1.0 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs); Netty pinned to 4.1.133.Final via override (v0.5.0 patch window) to close CVE-2026-42583/42584/42587. Spring Boot 4 bump (i-8) and Netty 4.2 bump (i-14) deferred to v1.0 Phase 40 / 41.
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).
- ClaudeSummary naming: Direction C per-record `@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only; full-export DTOs stay camelCase for backup-restore stability.
- PR durability: persisted via V27 `is_pr` column with ROW_NUMBER backfill; the wire field round-trips via `FullExportDto.SetRow.isPr` (9th component) with a recompute safety net per touched exercise on import.
- Body-metrics status code: `MetricsService.UpsertResult` wrapper drives `wasCreated ? 201 : 200`; date-keyed identity (V6 UNIQUE-by-`(user, date)`) is the design.
- ApiError.code typed-values catalog: 4 values from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`). Frontend branches on typed `code` (Phase 22.5 helper); tooling should branch on `status` and `code`, not `message`.
- SpringDoc artifact: 2.6.0 starter-webmvc-ui with the `ControllerAdviceBean` workaround in `OpenApiConfig`; bump to 2.7+ deferred per i-13.
- Frontend offline-first: Dexie IndexedDB queue scoped to session-execution; drains on `online` event via `clientSetId` UUID idempotency key (Phase 25 contract).
- Frontend route protection: `frontend/src/middleware.ts` enumerates 12 (app)-group segments in both `PROTECTED_PREFIXES` (runtime) and `config.matcher` (compile-time) arrays; the middleware test scaffold (Phase 29.5) gates future drift.
- Zod schema parity: backend DTO surfaces (ImportResult, FullExport, vapid, push subscribe, rest-timer schedule request/response) round-trip via shared Zod schemas in the frontend (Phase 25.5 + Phase 34); future endpoint additions should mirror this pattern.
- Insights surface composition (v0.6 lock-in): `/insights` is the canonical "Detayli insight sayfasi" deliverable from ProjectBrief Phase 6, NOT a separate `/stats` route. Splitting `insights-client.tsx` into per-card files is deferred to v1.0 polish.
- Service-worker cache versioning (v0.6 lock-in): bump the `CACHE` constant on every behaviour-affecting SW change. v0.6 cycle moved v1 -> v2 (Phase 32) -> v3 (Phase 33). The activate handler already deletes any cache name that does not match the current `CACHE`.
- Web Push schedule contract (v0.6 lock-in): server-scheduled rest-timer push is the primary path because service workers cannot persist a `setTimeout` across termination and `TimestampTrigger` is Chromium-only behind a flag. Cancel + re-schedule is the lifecycle contract; no edit / reschedule / snooze.
- Frontend metrics dependency posture (v0.6 lock-in): `prom-client` and `@opentelemetry/sdk-metrics` rejected because `frontend/package.json` is protected. Hand-rolled in-memory registry under single-instance Self-Hosted Contract is the load-bearing decision.
- Web Vitals reporter dependency posture (v0.6 lock-in): framework-native `next/web-vitals` re-export from the `next` package is used (no `web-vitals` direct dep). Mounted once in the root layout.
- Perf-budget gating posture (v0.6 lock-in): `docs/PERF_BUDGETS.md` is doc-only in v0.6. Lighthouse CI workflow + `scripts/check-bundle-size.mjs` + `@next/bundle-analyzer` dep all rejected because they would each touch a protected path; deferred as i-15.

### v0.6 Findings (archived)

The v0.6 cycle delivered seven phases (31-37) across 11 plans in a single working session on 2026-05-09 (~2.5 hours real time). Per-phase findings (charts-stats `/insights` re-composition, PWA install prompt + offline shell + SW scope hardening, web push end-to-end with self-test endpoint, server-scheduled rest-timer push with V28 migration, hand-rolled frontend HTTP metrics closing i-4, RouteSkeleton + RouteError + Web Vitals reporter + perf-budget doc, structured-logging test redesign closing i-9) are consolidated in `.planning/milestones/v0.6-ROADMAP.md`. Each phase plan's source-of-truth artifact stays under `.planning/phases/31-*` through `.planning/phases/37-*`. SUMMARY.md files were not produced by the plan executors; the accomplishment record lives in `CHANGELOG.md` `## [0.6.0]`, the per-plan PLAN.md output blocks, and the git commit messages (the v0.6-ROADMAP.md archive consolidates all three).

### Issue-to-Phase Mapping (carried forward)

- i-3: documentation note on `.gitignore` `data/` glob-form correction. Stays carried as audit reference.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-5: closed by v1.0 Phase 40 (framework-majors).
- i-6: closed by v1.0 Phase 40 (framework-majors); v0.5 + v0.6 stayed on Next 15.
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-7b (NEW in v1.0 Phase 39): residual 2.x major bump, deferred until
  upstream `org.testcontainers:testcontainers:2.0.0` GA on Maven Central.
- i-8: closed by v1.0 Phase 40 Plan 01 (framework-majors) re-defer
  playbook. Original 3.5.14 -> 4.0.6 attempt rolled back at commits
  75c3ea1..e49e1dc on 2026-05-09 after Jackson 2 vs Jackson 3
  default-classpath shift surfaced; runtime stays pinned at Spring Boot
  3.5.14 (tail of the 3.5.x line on Maven Central). Successor work tracked
  as i-8b. v0.6 stayed on Spring Boot 3.5.x.
- i-8b (NEW in v1.0 Phase 40 Plan 01): residual Spring Boot 4.0.x major
  bump deferred until either (Path A) `spring-boot-jackson2` compat-module
  runtime probe completes successfully OR (Path B) Jackson 2 -> Jackson 3
  codebase migration sub-plan absorbs the 36-file refactor; whichever
  path reopens, also applies trivial Jackson2-module + Prometheus-actuator
  package-relocation import fixes.
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13 (carried from v0.4): bump SpringDoc to >= 2.7 to remove the ControllerAdviceBean workaround. Trigger: backend dependency review phase post-v0.6 (likely v1.0 Phase 41 or 42).
- i-14 (carried from v0.5): bump Netty to 4.2.13.Final to close CVE-2026-42577. Suppressed in trivy for v0.5.0 + v0.6.0 (no runtime path; Spring MVC + Tomcat). Trigger: v1.0 Phase 41 (security-hardening).
- i-15 (NEW in v0.6): Lighthouse CI workflow + `scripts/check-bundle-size.mjs` + `@next/bundle-analyzer` dev dep for perf-budget gating. Trigger: operator approves at least one protected-file edit, OR Phase 38+ (v1.0 hardening) explicitly takes ownership of CI perf gating.

## Session Continuity

Last session: 2026-05-09 - v0.6 milestone archived; v0.6.0 tag at 08cc968.
Stopped at: v0.6 close complete; next action: `/gsd:plan-phase 38-01` (e2e-tests) once v1.0 work begins.
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/milestones/v0.4-ROADMAP.md` (full v0.4 archive)
- `.planning/milestones/v0.5-ROADMAP.md` (full v0.5 archive)
- `.planning/milestones/v0.6-ROADMAP.md` (full v0.6 archive)
- `.planning/ROADMAP.md` (current; v1.0 outlined)
- `.planning/ISSUES.md` (open deferred issues)
- `.planning/HANDOFF.md` (pre-GSD v0.2 snapshot)
- `.planning/codebase/STACK.md`
- `.planning/codebase/ARCHITECTURE.md`
- `.planning/codebase/STRUCTURE.md`
- `.planning/codebase/CONVENTIONS.md`
- `.planning/codebase/TESTING.md`
- `.planning/codebase/INTEGRATIONS.md`
- `.planning/codebase/CONCERNS.md`
- `docs/SELF_HOSTED_CONTRACT.md` (binding operational contract)
- `ProjectBrief.md` (original phase plan)
