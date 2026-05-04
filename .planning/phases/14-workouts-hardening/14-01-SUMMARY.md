---
phase: 14-workouts-hardening
plan: 01
subsystem: workouts
tags: [bug-fix, cascade, hibernate, integration-tests]

requires: [13-workouts-audit/13-01]
provides:
  - WorkoutDayExerciseRepository
  - cascade-id fix in WorkoutDaysService.createDay and addItem
  - 6 re-enabled WorkoutDaysIntegrationTest cases
  - id-presence regression assertion on createDayAddsItToPlan
affects: [14-workouts-hardening/14-02, 15-sessions-core]

tech-stack:
  added: []
  patterns:
    - "explicit child-repo saveAndFlush after parent.addChild() to materialize @UuidGenerator id before mapper read"

key-files:
  created:
    - backend/src/main/java/com/workouthub/workouts/domain/WorkoutDayExerciseRepository.java
  modified:
    - backend/src/main/java/com/workouthub/workouts/WorkoutDaysService.java
    - backend/src/test/java/com/workouthub/workouts/WorkoutDaysIntegrationTest.java

key-decisions:
  - "i-1 closed by injecting child repos and calling saveAndFlush on the new entity directly, not via plans.saveAndFlush(plan); cascade-through-parent does not propagate id to mapper in time."
  - "Strengthened createDayAddsItToPlan with explicit $.id assertion so future regressions of the same root cause cannot pass unnoticed."

patterns-established:
  - "When the controller response is built from an entity created via JPA cascade, flush via the child repository (not the parent) to ensure @UuidGenerator id is visible to the mapper."

issues-created: []
issues-closed: [i-1]

duration: 2 min
completed: 2026-05-04
---

# Phase 14 Plan 01: workouts-hardening (i-1) Summary

**WorkoutDaysService cascade-id bug closed by switching `createDay` and `addItem` to direct child-repo `saveAndFlush`, unblocking 6 disabled WorkoutDaysIntegrationTest cases and adding a regression assertion on the green-passing day-create test.**

## Performance

- **Duration:** 2 min
- **Started:** 2026-05-04T01:02:20Z
- **Completed:** 2026-05-04T01:04:10Z
- **Tasks:** 3
- **Files modified:** 3 (1 created, 2 modified)

## Accomplishments

- Added `WorkoutDayExerciseRepository` so `WorkoutDaysService` can flush newly cascaded `WorkoutDayExercise` rows directly.
- Replaced `plans.saveAndFlush(plan)` with `daysRepo.saveAndFlush(day)` in `createDay` and with `itemsRepo.saveAndFlush(item)` in `addItem`. `WorkoutPlanMapper.toDayDto` and `toItemDto` now see a non-null `@UuidGenerator` id.
- Re-enabled six previously skipped tests in `WorkoutDaysIntegrationTest` (`duplicateDayOfWeekReturns409`, `addItemsAndReorderCloseNoGaps`, `reorderWithIncompleteListReturns409`, `deleteItemRenumbersRemaining`, `deleteDayCascadesItems`, `updateItemPatchesFields`) and removed the now-unused `@Disabled` import.
- Strengthened `createDayAddsItToPlan` with an explicit `$.id` JsonPath assertion so a future return of the cascade-id regression breaks a green test, not just the disabled ones.
- `deleteItem` and `reorderItems` flushes were intentionally not touched (orphan-removal and unique-constraint two-phase semantics rely on the parent flush).

## Task Commits

1. **Task 1: Add WorkoutDayExerciseRepository** - `614d24f` (feat)
2. **Task 2: Force id population via direct child saveAndFlush** - `2119c0f` (fix)
3. **Task 3: Re-enable 6 disabled tests + add id regression assertion** - `b0ad1bc` (test)

**Plan metadata:** _(this commit)_ `docs(14-01): complete workouts-hardening i-1 plan`

## Files Created/Modified

- `backend/src/main/java/com/workouthub/workouts/domain/WorkoutDayExerciseRepository.java` - new `JpaRepository<WorkoutDayExercise, UUID>`, empty body.
- `backend/src/main/java/com/workouthub/workouts/WorkoutDaysService.java` - injected `WorkoutDayRepository` and `WorkoutDayExerciseRepository`; `createDay` now flushes via `daysRepo.saveAndFlush(day)`, `addItem` via `itemsRepo.saveAndFlush(item)`. Other methods unchanged.
- `backend/src/test/java/com/workouthub/workouts/WorkoutDaysIntegrationTest.java` - removed 6 `@Disabled` annotations + the import; added `jsonPath("$.id").exists()` to `createDayAddsItToPlan`.

## Decisions Made

- Chose explicit child-repo `saveAndFlush` over alternative fixes (e.g., reading id off the cascade then flushing parent) because audit Section 5 (F1) and the plan explicitly identified this approach. It is also the smallest diff that materializes the id before the mapper executes.
- Did not refactor `deleteItem` (orphan-removal flush) or `reorderItems` (two-phase unique-constraint flush). Those code paths depend on the parent-driven flush ordering and are not implicated in i-1.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Local Maven is not available on this host (memory: `local_maven_gap.md`), so backend test verification will run on CI after push. The change set is constrained to 3 files (1 new repo, 1 service edit, 1 test edit) per the plan, which the plan verification gate explicitly allows.

## Next Phase Readiness

- i-1 fix is shipped. Plan 14-02 will close i-2 (the round-trip export test) using a fully independent root cause per the Phase 13 audit verdict, and should also move i-1 from "Open" to "Closed" in `.planning/ISSUES.md` after CI confirms the eight WorkoutDaysIntegrationTest cases pass.
- Confidence: high. The fix is the smallest possible diff for the root cause, and the strengthened green test guards against silent regression.
- CI gate: `./mvnw verify` on the pushed commit must report `WorkoutDaysIntegrationTest` 8/8 passing, 0 skipped. Until that signal is observed, treat i-1 as "fix shipped, awaiting CI confirmation".

---
*Phase: 14-workouts-hardening*
*Completed: 2026-05-04*
