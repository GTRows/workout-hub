# State

## Current Position

Milestone: v0.4 Backend Feature Completion (active)
Phase: 15 of 20 (sessions-core) - in progress (2 of 4 plans shipped)
Plan: 15-02 complete (clientSetId idempotency key)
Status: Phase 15 in progress; ready for plan 15-03 (heart-rate exposure) or 15-04 (auto-numbering + typed 409 codes)
Last activity: 2026-05-04 - Completed 15-02-PLAN.md (V26 migration, service fast-path, 200/201 controller mapping, 4 integration tests)

Progress: v0.4 ####________________ 25% (2/8 phases complete; Phase 15 in progress, 2/4 plans done)
          v0.5 (planned) - Phases 21-30
          v0.6 (planned) - Phases 31-37
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.4 detailed; v0.5/v0.6/v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-4, i-5, i-6, i-7, i-8, i-9 remain open; i-1 and i-2 closed by Phase 14)
- See: `.planning/phases/13-workouts-audit/13-01-AUDIT.md` for the workouts audit deliverable.
- See: `.planning/phases/14-workouts-hardening/14-01-SUMMARY.md` for the i-1 cascade-id fix.
- See: `.planning/phases/15-sessions-core/15-01-AUDIT.md` for the sessions audit deliverable (251 lines, 6 sections).
- See: `.planning/phases/15-sessions-core/15-02-SUMMARY.md` for the clientSetId idempotency key plan deliverable.

**Current focus:** Plan 15-03 (heart-rate field exposure on session DTOs) or 15-04 (auto-numbering verdict + typed 409 codes) — both can proceed independently. Plan 15-02 shipped: audit Section 4a PARTIAL verdict closed; Sections 4b PARTIAL and 4c FAIL remain open for plan 15-04.

## Accumulated Context

### Locked-in Decisions (carried from v0.3)

Full decision log lives in `.planning/milestones/v0.3-ROADMAP.md` "Key Decisions" section. The most operationally relevant ones for v0.4 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).

### Phase 13-14 Findings

- i-1 root cause (top hypothesis): Hibernate's `saveAndFlush(plan)` does not propagate `@UuidGenerator` id assignment to a transient cascade-child in time for `WorkoutPlanMapper.toDayDto` to read it. Fix in `WorkoutDaysService.createDay` and `addItem` is to use explicit `daysRepo.saveAndFlush(day)` (or add `WorkoutDayExerciseRepository` for items).
- i-2 root cause (top hypothesis): `TestAuthHelpers.seed` bypasses `DefaultPlanSeeder`, so the round-trip test exports an empty `plans` array. `plansInserted == 0` is correct given the missing precondition. Fix is test-side, not production.
- i-1 and i-2 do NOT share a root cause. Phase 14 plans two independent change sets.
- Phase 14 confirmed: i-1 fix is service-layer only (`WorkoutDaysService.createDay`/`addItem` use `daysRepo`/`itemsRepo` `saveAndFlush` instead of `plans.saveAndFlush(plan)`); i-2 fix is test-only (added a seed-plan POST before the export GET in `FullExportImportIntegrationTest`). The audit's two-independent-root-causes verdict held.

### Phase 15 Findings (sessions-core audit)

- All 7 ProjectBrief Phase 4 endpoints are implemented today (no `Missing` rows). 21 files / 965 lines under `sessions/`. 10 endpoints exposed (8 Phase 15 + 2 Phase 16 leaks: `last-performance`, `progress`).
- Phase 14 child-repo `saveAndFlush` lesson is already applied at `SessionSetsService.java:64` (`sets.saveAndFlush(set)`); cascade-id risk that motivated i-1 is mitigated for sessions.
- Offline-first sync contract verdict: must-harden in plan 15-02+. Top 3 gaps: (1) no client-supplied idempotency key on `POST /sets` (4a PARTIAL: 409 conflates idempotent retry with set_number collision), (2) auto-numbering at `SessionSetsService.java:46-48` is unsafe for offline drain (4c FAIL: out-of-order without explicit `setNumber` corrupts row order), (3) 409 body lacks typed error code (4b PARTIAL: drainer cannot distinguish SESSION_FINISHED from SET_NUMBER_DUPLICATE).
- `heartRateAvgBpm` field (V20 column, entity getter at `WorkoutSession.java:114`) is fed only by Garmin .fit importer at `HealthImportService.java:68`; not exposed on any DTO. Decision: expose read-only on `SessionDto` and `SessionSummaryDto` in plan 15-03; no client write path needed.
- Phase 16 in-package leaks (`ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, `PrDetector`, `SessionSetRepository.findHistoricalByUserAndExercise`) stay in package; plan 15-02+ MUST NOT modify them. `newPr` flag on create response is a Phase 16 leak that landed early but provides UX value; keep as-is, do not extend.
- Plan-count: 3 hardening plans (15-02 idempotency key, 15-03 heart-rate exposure, 15-04 auto-numbering verdict + typed 409 codes). Phase 14 one-feature-per-plan precedent guided the split.

### Issue-to-Phase Mapping

- i-1, i-2: closed by v0.4 Phase 14 (workouts-hardening) Plans 01 and 02 respectively.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-5, i-6, i-8: closed by v1.0 Phase 40 (framework-majors).

### Roadmap Evolution

- 2026-05-04: v0.4 milestone created with 8 phases (Phase 13-20). Full v1.0 path scoped: v0.5 frontend completion (Phase 21-30), v0.6 operational maturity (Phase 31-37), v1.0 release hardening (Phase 38-44). Total 32 phases to v1.0.
- 2026-05-04: Phase 13 (workouts-audit) shipped (1 plan: 13-01). Audit deliverable at `.planning/phases/13-workouts-audit/13-01-AUDIT.md`.
- 2026-05-04: Phase 14 plan 01 shipped. WorkoutDaysService cascade-id fix lands the explicit child-repo saveAndFlush from audit Section 5; 6 disabled WorkoutDaysIntegrationTest cases re-enabled and a regression assertion added to the green test. CI verification pending.
- 2026-05-04: Phase 14 (workouts-hardening) shipped (2 plans: 14-01 cascade-id fix, 14-02 export round-trip test fix). Closed i-1 and i-2.
- 2026-05-04: Phase 15 plan 01 (sessions-core audit) shipped. Audit deliverable at `.planning/phases/15-sessions-core/15-01-AUDIT.md`. Phase 15 plan-count revised from TBD to 4 plans (1 audit + 3 hardening). Plan 15-02+ scope finalized: 4 coverage tests, 3 contract-finalization items, 2 feature-gap, 5 defer. No new ISSUES.md entries (contract gaps belong to 15-02+ scope, not deferred-issues queue).
- 2026-05-04: Phase 15 plan 02 (clientSetId idempotency key) shipped. V26 migration + entity field + migration test, service fast-path, 200/201 controller mapping, 4 integration tests. Audit Section 4a PARTIAL verdict closed; Sections 4b PARTIAL (typed 409 codes) and 4c FAIL (auto-numbering) remain open for plan 15-04.

## Session Continuity

Last session: 2026-05-04 - Phase 15 plan 02 complete
Stopped at: Phase 15 in progress (2 of 4 plans). Next action: `/gsd:execute-plan` with plan 15-03 (heart-rate exposure) or 15-04 (auto-numbering + typed 409 codes).
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/ROADMAP.md` (current, v0.4 detailed; future milestones outlined)
- `.planning/ISSUES.md` (open deferred issues)
- `.planning/phases/13-workouts-audit/13-01-AUDIT.md` (workouts audit deliverable)
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
