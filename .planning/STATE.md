# State

## Current Position

Milestone: v0.4 Backend Feature Completion (active)
Phase: 16 of 20 (sessions-analytics) - in progress (1 of 4 plans shipped)
Plan: 16-01 complete (sessions-analytics audit)
Status: Phase 16 in progress; plan 16-01 audit shipped, plan 16-02 (package-extraction) is next
Last activity: 2026-05-04 - Completed 16-01-PLAN.md (audit-only deliverable; 297 lines; verdicts: extract analytics scaffold to existing com.workouthub.analytics package, persist is_pr via V27 with window-function backfill, Java-side aggregation acceptable for v0.4 with SQL pushdown deferred to Phase 31; 4-plan recommendation for Phase 16)

Progress: v0.4 ########____________ 44% (3/8 phases complete plus Phase 16-01 of 4 plans; Phase 15 done, Phase 16 in progress)
          v0.5 (planned) - Phases 21-30
          v0.6 (planned) - Phases 31-37
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.4 detailed; v0.5/v0.6/v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-4, i-5, i-6, i-7, i-8, i-9 remain open; i-1 and i-2 closed by Phase 14)
- See: `.planning/phases/13-workouts-audit/13-01-AUDIT.md` for the workouts audit deliverable.
- See: `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` for the sessions-analytics audit deliverable (297 lines, 6 sections).
- See: `.planning/phases/14-workouts-hardening/14-01-SUMMARY.md` for the i-1 cascade-id fix.
- See: `.planning/phases/15-sessions-core/15-01-AUDIT.md` for the sessions audit deliverable (251 lines, 6 sections).
- See: `.planning/phases/15-sessions-core/15-02-SUMMARY.md` for the clientSetId idempotency key plan deliverable.
- See: `.planning/phases/15-sessions-core/15-03-SUMMARY.md` for the heart-rate exposure plan deliverable.
- See: `.planning/phases/15-sessions-core/15-04-SUMMARY.md` for the typed 409 codes + auto-numbering verdict plan deliverable.
- See: `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` for the sessions-analytics audit deliverable (297 lines, 6 sections; package-boundary, PR durability, perf, plan-count verdicts).

**Current focus:** Phase 16 in progress. 16-01 audit shipped; next action: Phase 16 plan 16-02 (package-extraction: move `ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, `PrDetector` from `sessions/` to existing `com.workouthub.analytics` package; one-import touch on `SessionSetsService`).

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

### Phase 16 Findings (sessions-analytics audit)

- Phase 16 endpoint surface is fully implemented today as Phase 15-01 audit Section 5 in-package leaks. 5 Phase 16-owned files / 191 lines under `sessions/` (`ExerciseAnalyticsController`, `ExerciseAnalyticsService`, `PrDetector`, `LastPerformanceDto`, `ProgressPointDto`) plus 4 shared call sites (`SessionSetsService.add` PR-detection at lines 80-82, `SessionSetsService.bestPriorOneRm` at 105-115, `SessionsMapper.toSetDto` overload at 47-65, `SessionSetRepository.findHistoricalByUserAndExercise` at 21-30).
- Package-boundary verdict: **extract to `com.workouthub.analytics`**. The package already exists with charts-stats scaffold (`AnalyticsController` 62 lines plus `AnalyticsService` 237 lines, serving `/api/analytics/{volume,one-rm,streak,prs,heatmap}` from pre-GSD work). Grep confirms no cross-package consumer of `PrDetector` outside `SessionSetsService`. Extraction is one-import-add plus a directory move; payoff is consolidating Epley duplication (`sessions/PrDetector.epleyOneRm` vs `analytics/AnalyticsService.epley`) in a Phase 31 follow-up.
- PR durability verdict: **persist `is_pr` via V27 with window-function backfill**. Postgres 16 supports `ROW_NUMBER() OVER (PARTITION BY user_id, exercise_id ORDER BY weight*(1 + reps/30) DESC)` natively; SQL drafted in audit Section 5. Closes the Phase 15-04 create-vs-reread gap; replaces `newPrFlagPresentOnCreateButOmittedOnDetailReread` with a stronger durable-read assertion.
- Perf verdict: **Java-side aggregation acceptable for v0.4**. Power-user estimate ~800 rows fetched per `/progress?limit=10` for 200 sessions x 4 sets; convention matches existing analytics package (`AnalyticsService.weeklyVolume` and `oneRepMax`); SQL pushdown deferred to Phase 31.
- Field-level drift: `ProgressPointDto.estimatedOneRmKg` (Epley projection) is missing despite ROADMAP listing it as a Phase 16 deliverable. `PrDetector.epleyOneRm` exists but is never materialized into the read DTO. Cheapest Phase 16 deliverable (one DTO field plus one mapper line plus one test extension).
- Plan-count: **4 plans** for Phase 16 (16-01 audit shipped; 16-02 package-extraction; 16-03 epley-projection; 16-04 pr-durability).

### Phase 15 Findings (sessions-core audit)

- All 7 ProjectBrief Phase 4 endpoints are implemented today (no `Missing` rows). 21 files / 965 lines under `sessions/`. 10 endpoints exposed (8 Phase 15 + 2 Phase 16 leaks: `last-performance`, `progress`).
- Phase 14 child-repo `saveAndFlush` lesson is already applied at `SessionSetsService.java:64` (`sets.saveAndFlush(set)`); cascade-id risk that motivated i-1 is mitigated for sessions.
- Offline-first sync contract: fully hardened by Plans 15-02/15-04. (1) clientSetId idempotency key on `POST /sets` (15-02 closed 4a PARTIAL: 200 = replay, 201 = newly created). (2) Auto-numbering at `SessionSetsService.java:55-57` retained for live-online next-set UX; offline drainers MUST send explicit setNumber (15-04 documented 4c FAIL verdict in Javadoc). (3) Typed 409 codes propagated through `ApiError.code` for the four sessions throw sites (15-04 closed 4b PARTIAL: drainer can branch on SESSION_FINISHED vs SET_NUMBER_DUPLICATE without message parsing).
- `heartRateAvgBpm` field (V20 column, entity getter at `WorkoutSession.java:114`) is fed only by Garmin .fit importer at `HealthImportService.java:68`. Plan 15-03 closed the exposure gap: `Short heartRateAvgBpm` is now read-only on `SessionDto` and `SessionSummaryDto`; Section 3 field-level drift and Section 6 FG1 closed.
- Phase 16 in-package leaks (`ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, `PrDetector`, `SessionSetRepository.findHistoricalByUserAndExercise`) stayed in package across all four 15-* plans; Phase 16 owns formalization. `newPr` flag on create response is a Phase 16 leak with UX value; the create-only behavior is now documentation-asserted by 15-04's `newPrFlagPresentOnCreateButOmittedOnDetailReread` test.
- Plan-count: 4 plans (15-01 audit, 15-02 idempotency key, 15-03 heart-rate exposure, 15-04 typed codes + auto-numbering verdict). All shipped.

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
- 2026-05-04: Phase 15 plan 03 (heart-rate exposure) shipped. `Short heartRateAvgBpm` appended to `SessionDto` and `SessionSummaryDto` records; `SessionsMapper.toDto`/`toSummary` populate the new field; two integration tests use direct `WorkoutSessionRepository.saveAndFlush` to mimic the Garmin .fit importer write path on detail/active/history endpoints. Audit Section 3 field-level drift and Section 6 FG1 closed; Section 6 C3 coverage gap closed.
- 2026-05-04: Phase 15 plan 04 (typed 409 codes + auto-numbering verdict) shipped. `ApiError` gains optional trailing `String code` (Jackson NON_NULL omits when null - zero regression on existing 4xx/5xx); `ConflictException` two-arg constructor preserves single-arg back-compat. Four sessions/ throw sites carry codes: `SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`. Five existing 409 integration tests assert `$.code`. `SessionSetsService.add` Javadoc documents auto-numbering contract (live-online safe; offline drainers MUST send explicit setNumber) and idempotency contract (clientSetId replay does not re-fire side effects). `newPrFlagPresentOnCreateButOmittedOnDetailReread` documents create-vs-reread newPr flag behavior. Audit Section 4b PARTIAL closed; Section 4c FAIL verdict documented; Section 6 C4 closed. Phase 15 complete (4/4 plans shipped).
- 2026-05-04: Phase 16 plan 01 (sessions-analytics audit) shipped. Audit deliverable at `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` (297 lines). Phase 16 plan-count revised from TBD to 4 plans (1 audit + 3 hardening). Verdicts: package-boundary extract to existing `com.workouthub.analytics` package; PR durability persist `is_pr` via V27 window-function backfill; perf Java-side acceptable for v0.4 with SQL pushdown deferred to Phase 31. Plan 16-02 (package-extraction), 16-03 (epley-projection), 16-04 (pr-durability) scope finalized with file-level entries. Adjacent finding: `com.workouthub.analytics` package already exists with pre-GSD charts-stats scaffold (Epley re-implemented); deferred Epley consolidation marker for Phase 31.

## Session Continuity

Last session: 2026-05-04 - Phase 16 plan 01 (sessions-analytics audit) complete
Stopped at: Phase 16 in progress (1 of 4 plans shipped). Next action: `/gsd:plan-phase 16-02` for plan 16-02 (package-extraction: move analytics scaffold to com.workouthub.analytics).
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
