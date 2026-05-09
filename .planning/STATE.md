# State

## Current Position

Milestone: v0.6 Operational Maturity (active)
Phase: 31 of 37 (charts-stats) - not started
Plan: 31-01 charts-stats - not started
Status: v0.5 closed at v0.5.0 (tag 5efaeb5; 19/19 plans across Phases 21-30 + decimal phases 22.5/25.5/29.5). Patch commits up to 931cb69 closed Netty CVE bumps and trivy suppression for CVE-2026-42577. Next: `/gsd:plan-phase 31-01` (charts-stats) when v0.6 work begins.
Last activity: 2026-05-09 - v0.5 milestone bookkeeping closed; v0.5.0 tag at 5efaeb5; patch window through 931cb69

Progress: v0.6 ____________________  0% (0/7 phases planned)
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/milestones/v0.5-ROADMAP.md` for full v0.5 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.6 active; v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-4, i-5, i-6, i-7, i-8, i-9, i-13, i-14 open; i-1, i-2, i-10, i-12 closed during v0.4)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** v0.6 operational maturity - charts/stats (volume, 1RM Epley, weight change, frequency heatmap, PR list, streak) at Phase 31, PWA install/offline shell at Phase 32, web push and rest-timer notifications at Phases 33-34, frontend HTTP metrics (i-4) at Phase 35, error states + perf budgets at Phase 36, and the structured-logging test redesign (i-9) at Phase 37.

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4 + v0.5)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md`, `.planning/milestones/v0.4-ROADMAP.md`, and `.planning/milestones/v0.5-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v0.6 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs); Netty pinned to 4.1.133.Final via override (v0.5.0 patch window) to close CVE-2026-42583/42584/42587.
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).
- ClaudeSummary naming: Direction C per-record `@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only; full-export DTOs stay camelCase for backup-restore stability.
- PR durability: persisted via V27 `is_pr` column with ROW_NUMBER backfill; the wire field round-trips via `FullExportDto.SetRow.isPr` (9th component) with a recompute safety net per touched exercise on import.
- Body-metrics status code: `MetricsService.UpsertResult` wrapper drives `wasCreated ? 201 : 200`; date-keyed identity (V6 UNIQUE-by-`(user, date)`) is the design.
- ApiError.code typed-values catalog: 4 values from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`). Frontend branches on typed `code` (Phase 22.5 helper); tooling should branch on `status` and `code`, not `message`.
- SpringDoc artifact: 2.6.0 starter-webmvc-ui; `paths-to-match=/api/**` excludes operator paths from the public OpenAPI surface; bundled UI per Direction D1 with `BIND_ADDR=127.0.0.1` as the trust boundary.
- Frontend offline-first: Dexie IndexedDB queue scoped to session-execution; drains on `online` event via `clientSetId` UUID idempotency key (Phase 25 contract).
- Frontend route protection: `frontend/src/middleware.ts` enumerates 12 (app)-group segments in both `PROTECTED_PREFIXES` (runtime) and `config.matcher` (compile-time) arrays; the middleware test scaffold (Phase 29.5) gates future drift.
- Zod schema parity: backend DTO surfaces (ImportResult, FullExport, vapid, push subscribe) round-trip via shared Zod schemas in the frontend (Phase 25.5); future endpoint additions should mirror this pattern.

### v0.5 Findings (archived)

The v0.5 frontend audit (`.planning/phases/21-frontend-audit/21-01-AUDIT.md`) catalogued route trees, component layers, API client gaps vs. v0.4 backend, ProjectBrief Phase 4-6 gap matrix, and the test gate posture for v0.5. Per-phase findings (auth-pages typed error UX, dashboard weekly aggregation, plan editor day/exercise CRUD with drag-drop, session-execution offline drain + typed 409 UX + RPE/edit/delete, exercise-catalog PR card + progress chart, history-view repeat-workout CTA, metrics-ui chest/arm/thigh + 200/201 toast, profile-settings load-error banners + a11y, export-import-ui destructive confirm + history shim) are consolidated in `.planning/milestones/v0.5-ROADMAP.md`. Each phase plan's source-of-truth artifact stays under `.planning/phases/21-*` through `.planning/phases/30-*` (and the three inserted decimals 22.5/25.5/29.5).

### Issue-to-Phase Mapping (carried forward)

- i-3: documentation note on `.gitignore` `data/` glob-form correction. Stays carried as audit reference.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-5: closed by v1.0 Phase 40 (framework-majors).
- i-6: closed by v1.0 Phase 40 (framework-majors); v0.5 stayed on Next 15.
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-8: closed by v0.6 / v1.0 (Spring Boot major bump).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13 (carried from v0.4): bump SpringDoc to >= 2.7 to remove the ControllerAdviceBean workaround. Trigger: plan a backend dependency review phase post-v0.5.
- i-14 (NEW in v0.5): bump Netty to 4.2.13.Final to close CVE-2026-42577. Suppressed in trivy for v0.5.0 (no runtime path; Spring MVC + Tomcat). Trigger: v0.6 Phase 41 (security-hardening).

## Session Continuity

Last session: 2026-05-09 - v0.5 milestone archived; v0.5.0 tag at 5efaeb5; HEAD at 931cb69 after Netty CVE patch window.
Stopped at: v0.5 close complete; next action: `/gsd:plan-phase 31-01` (charts-stats) once v0.6 work begins.
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/milestones/v0.4-ROADMAP.md` (full v0.4 archive)
- `.planning/milestones/v0.5-ROADMAP.md` (full v0.5 archive)
- `.planning/ROADMAP.md` (current; v0.6 active; v1.0 outlined)
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
