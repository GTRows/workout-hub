---
phase: 13-workouts-audit
plan: 01
subsystem: workouts
tags: [audit, gap-analysis, planning]

requires: []
provides:
  - workouts package layout inventory (25 files, 1132 lines)
  - endpoint catalog (15 endpoints across 3 controllers)
  - ProjectBrief Phase 3 gap matrix
  - i-1 / i-2 root-cause analysis with file:line evidence
  - recommended Phase 14 scope (bug-fix / coverage / feature-gap buckets)
affects: [14-workouts-hardening, 15-sessions-core]

tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - .planning/phases/13-workouts-audit/13-01-AUDIT.md
  modified: []

key-decisions:
  - "Top-ranked i-1 hypothesis: Hibernate's saveAndFlush(plan) does not propagate UuidGenerator id assignment to a transient cascade-child in time for the mapper to read it; fix is explicit daysRepo.saveAndFlush(day) instead of going through the parent."
  - "Top-ranked i-2 hypothesis: TestAuthHelpers.seed bypasses DefaultPlanSeeder so the test user has zero plans at export time; plansInserted=0 is correct given the missing precondition. Fix is test-side (seed a plan before exporting)."
  - "i-1 and i-2 do NOT share a root cause. Fixing i-1 will not unblock i-2."

patterns-established:
  - "Audit deliverable shape: package layout + endpoint catalog + brief gap matrix + root-cause analysis + scope buckets, anchored on file:line citations."

issues-created: []

duration: 25min
completed: 2026-05-04
---

# Phase 13 Plan 01: workouts-audit Summary

**Inventoried the workouts/ package, mapped its 15 endpoints against ProjectBrief Phase 3, and identified that i-1 (cascade id population) and i-2 (test-side missing precondition) have independent root causes - Phase 14 now has a concrete two-fix scope.**

## Performance

- **Duration:** 25 min
- **Started:** 2026-05-04
- **Completed:** 2026-05-04
- **Tasks:** 3 of 3
- **Files modified:** 0 (audit is read-only on Java); 1 new audit file under `.planning/`.

## Accomplishments

- Inventory complete: 3 controllers, 15 endpoints catalogued with `file:line` references for every method.
- Gap matrix: 6 implemented, 2 partial, 2 different-shape (naming drift only), 0 missing of 9 ProjectBrief Phase 3 endpoints; 5 out-of-brief endpoints documented.
- i-1 / i-2 root-cause analysis: top hypotheses each have a one-line falsification step. Crucial finding: i-2 is a test-side bug (TestAuthHelpers.seed bypasses DefaultPlanSeeder), not a production code defect.
- Phase 14 scope: 2 bug-fix tasks, 4 coverage tasks, 4 feature-gap deferrals to Phase 15+.

## Task Commits

Each task was committed atomically:

1. **Task 1: Inventory current workouts package surface** - `aac0a80` (docs)
2. **Task 2: Build ProjectBrief Phase 3 gap matrix** - `f696ccd` (docs)
3. **Task 3: Root-cause analysis for i-1 / i-2 plus Phase 14 scope** - `0f43c5c` (docs)

**Plan metadata:** (this commit) (docs: complete plan)

## Files Created/Modified

- `.planning/phases/13-workouts-audit/13-01-AUDIT.md` - 239-line audit deliverable with five sections.

## Decisions Made

- Top-ranked hypothesis for i-1: cascade-persisted child id is read by the mapper before Hibernate has fully assigned it via the parent `saveAndFlush`. Fix in `WorkoutDaysService` (~5 lines plus a new `WorkoutDayExerciseRepository`).
- Top-ranked hypothesis for i-2: missing test precondition; the round-trip test never seeded a plan because `TestAuthHelpers.seed` bypasses `DefaultPlanSeeder`. Fix in the test method (~5-7 lines).
- i-2's fix is independent of i-1's. Phase 14 must plan two separate change sets.

## Issues Encountered

- The test comment at `FullExportImportIntegrationTest.java:115-116` claims "the default plan seeder runs on user creation" - this is wrong; the seeder is invoked only by `AdminUsersService.create`, not by `TestAuthHelpers.seed`. Documenting this drift here so Phase 14 fixes either the test setup or extends the helper.
- ProjectBrief Phase 3 endpoint list path id naming (`:exId`) differs from the implementation (`:itemId`); semantics match (the join row is the addressable resource, not the catalog exercise). Logged as feature-gap G3 for Phase 19 docs.
- ProjectBrief drift on day-level reorder: brief calls for "drag and drop reorder" (`ProjectBrief.md:283`) which the implementation supports for items within a day but not for days across the plan. Logged as feature-gap G1 for Phase 24+ planning.

## Next Phase Readiness

- Ready for Phase 14 planning: yes. The audit identifies two narrow code/test changes that close i-1 and i-2 and re-enable seven `@Disabled` tests in total.
- Open questions for Phase 14: whether to add a `WorkoutDayExerciseRepository` (new file under `domain/`) or to refactor `WorkoutDaysService.addItem` differently; whether to fix i-2 by amending the test or by changing `TestAuthHelpers.seed` semantics. Plan 14-01 should pick one of each.
