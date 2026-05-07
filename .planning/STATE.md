# State

## Current Position

Milestone: v0.5 Frontend Completion (active)
Phase: 21 of 30 (frontend-audit) - not started
Plan: Not started
Status: v0.4 milestone shipped 2026-05-07. Ready to plan Phase 21 (frontend-audit).
Last activity: 2026-05-07 - v0.4 milestone archive complete; v0.4.0 tag at c8fab14

Progress: v0.5 ____________________ 0% (0/10 phases)
          v0.6 (planned) - Phases 31-37
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.5 active; v0.6/v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-4, i-5, i-6, i-7, i-8, i-9, i-13 open; i-1, i-2, i-10, i-12 closed during v0.4)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** v0.5 frontend completion - inventory routes/components in Phase 21, then deliver auth pages, dashboard, plan editor, session-execution screen with IndexedDB drain wired to v0.4 sync contract, history, exercise catalog, metrics UI, profile, export-import UI through Phases 22-30.

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md` and `.planning/milestones/v0.4-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v0.5 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).
- ClaudeSummary naming: Direction C per-record `@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only; full-export DTOs stay camelCase for backup-restore stability.
- PR durability: persisted via V27 `is_pr` column with ROW_NUMBER backfill; the wire field round-trips via `FullExportDto.SetRow.isPr` (9th component) with a recompute safety net per touched exercise on import.
- Body-metrics status code: `MetricsService.UpsertResult` wrapper drives `wasCreated ? 201 : 200`; date-keyed identity (V6 UNIQUE-by-`(user, date)`) is the design.
- ApiError.code typed-values catalog: 4 values from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`). Tooling should branch on `status` and `code`, not `message`.
- SpringDoc artifact: 2.6.0 starter-webmvc-ui; `paths-to-match=/api/**` excludes operator paths from the public OpenAPI surface; bundled UI per Direction D1 with `BIND_ADDR=127.0.0.1` as the trust boundary.

### v0.4 Findings (archived)

The v0.4 audit and per-phase findings (workouts cascade-id root cause, sessions offline-first contract, sessions-analytics package boundary, body-metrics column drift, export ProjectBrief gap matrix, api-contract-docs SpringDoc integration shape) are consolidated in `.planning/milestones/v0.4-ROADMAP.md`. Each phase audit's source-of-truth artifact stays under `.planning/phases/13-*` through `.planning/phases/19-*`.

### Issue-to-Phase Mapping (carried forward)

- i-3: documentation note on `.gitignore` `data/` glob-form correction. Stays carried as audit reference.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-5: closed by v1.0 Phase 40 (framework-majors).
- i-6: closed by v0.5 Phase 21+ (frontend completion phase) or v1.0 Phase 40.
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-8: closed by v0.6 / v1.0 (Spring Boot major bump).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13 (NEW in v0.4): bump SpringDoc to >= 2.7 to remove the ControllerAdviceBean workaround. Trigger: plan a backend dependency review phase post-v0.5.

## Session Continuity

Last session: 2026-05-07 - v0.4 milestone archived; v0.4.0 tag at c8fab14
Stopped at: v0.4 close complete; next action: `/gsd:plan-phase 21-01` (frontend-audit) once v0.5 work begins.
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/milestones/v0.4-ROADMAP.md` (full v0.4 archive)
- `.planning/ROADMAP.md` (current; v0.5 active; v0.6 / v1.0 outlined)
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
