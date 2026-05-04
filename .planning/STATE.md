# State

## Current Position

Milestone: v0.4 Backend Feature Completion (active)
Phase: 14 of 20 (workouts-hardening) - complete (2 of 2 plans shipped)
Plan: 14-02 complete
Status: Phase 14 complete; ready to plan Phase 15 (sessions-core)
Last activity: 2026-05-04 - Completed 14-02-PLAN.md (workouts-hardening i-2 test-precondition fix)

Progress: v0.4 ####________________ 25% (2/8 phases)
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

**Current focus:** Plan and execute Phase 15 (sessions-core). Phase 14 closed i-1 (cascade-id) and i-2 (test precondition); WorkoutDaysIntegrationTest is fully green; export round-trip test now seeds a plan.

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

## Session Continuity

Last session: 2026-05-04 - Phase 14 plan 02 complete
Stopped at: Phase 14 closed (2 of 2 plans). Next action: `/gsd:plan-phase 15` to break down sessions-core.
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
