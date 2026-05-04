---
phase: 14-workouts-hardening
plan: 02
subsystem: exports
tags: [bug-fix, test-precondition, integration-tests, planning-bookkeeping]

requires: [14-workouts-hardening/14-01]
provides:
  - seeded-plan precondition in FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport
  - non-empty $.plans assertion in fullExportReturnsSchemaVersionAndCurrentUserSlices
  - ISSUES.md i-1 and i-2 moved to Closed
  - STATE.md and ROADMAP.md updated to reflect Phase 14 complete
affects: [15-sessions-core]

tech-stack:
  added: []
  patterns:
    - "Round-trip integration tests must seed their own preconditions when TestAuthHelpers does not invoke production seeders."

key-files:
  created: []
  modified:
    - backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java
    - .planning/ISSUES.md
    - .planning/STATE.md
    - .planning/ROADMAP.md

key-decisions:
  - "i-2 fixed test-side (added explicit POST /api/workout-plans before export GET) per audit Section 4b verdict; TestAuthHelpers.seed semantics preserved for tests that intentionally start from an empty user."
  - "Audit's Phase 13 hypothesis ranking validated end-to-end: independent root causes for i-1 and i-2; both fixes shipped in their own plans within the same phase."

patterns-established:
  - "When the production seeder is bypassed by a test helper, integration tests that depend on a seeded slice must POST through the production controller to materialize it."

issues-created: []
issues-closed: [i-2]

duration: 14 min
completed: 2026-05-04
---

# Phase 14 Plan 02: workouts-hardening i-2 export round-trip test fix Summary

**FullExportImportIntegrationTest now seeds a workout plan via the production controller before exporting, closing i-2 without changing any production code.**

## Performance

- **Duration:** 14 min
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Re-enabled `importRoundTripPreservesPlansFromExport` and added explicit plan-create precondition; removed unused `@Disabled` import.
- Added `$.plans.length()` assertion (with matching plan-create) to `fullExportReturnsSchemaVersionAndCurrentUserSlices` so a regression that drops the plans slice is caught at the smaller-blast-radius shape test first.
- Closed i-1 and i-2 in ISSUES.md with resolution notes; updated STATE.md and ROADMAP.md so Phase 14 reads as complete (2/2 plans).

## Task Commits

1. **Task 1: Seed plan in importRoundTripPreservesPlansFromExport and re-enable** - `7e38286` (test)
2. **Task 2: Assert non-empty plans slice in fullExportReturnsSchemaVersionAndCurrentUserSlices** - `a7a9371` (test)
3. **Task 3: Close i-1/i-2 in ISSUES, mark Phase 14 complete in STATE/ROADMAP** - `d01238d` (docs)

## Files Created/Modified

- `backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java` - Re-enabled disabled round-trip test, added plan-create precondition in two tests, asserted non-empty plans slice, removed unused Disabled import.
- `.planning/ISSUES.md` - Moved i-1 and i-2 from Open to Closed with resolution notes.
- `.planning/STATE.md` - Bumped Phase 14 to complete (2/2), updated progress gauge to 25%, renamed findings section to "Phase 13-14 Findings", updated session continuity to point at Phase 15.
- `.planning/ROADMAP.md` - Marked 14-02 bullet as `[x]`, set Phase 14 row to `2/2 Complete 2026-05-04`.

## Decisions Made

- Picked the test-local fix (POST /api/workout-plans before export) over modifying `TestAuthHelpers.seed`, honoring audit Section 4b verdict and preserving the helper's "start from an empty user" semantics for other tests.
- Switched the trailing `/active` assertion in the round-trip test from `status().isOk()` to `status().is2xxSuccessful()` because the seeded plan is created with `active=false` and `WorkoutPlansController.getActive` returns 204 No Content when there is no active plan; the round-trip semantics do not include activation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Removed unused `import org.junit.jupiter.api.Disabled`**

- **Found during:** Task 1 (re-enabling `importRoundTripPreservesPlansFromExport`)
- **Issue:** Removing the only `@Disabled` annotation in the file leaves an unused import. Compilation succeeds but the file becomes inconsistent and would trigger linter warnings; the plan's verify line "`import org.junit.jupiter.api.Disabled;` is removed (Task 1 of Plan 14-01 already removed it from a different file; this is a separate file)" implies the removal is expected.
- **Fix:** Deleted the import.
- **Files modified:** `backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java`
- **Verification:** `grep -c "@Disabled" ...` returns 0; `grep -c "import org.junit.jupiter.api.Disabled" ...` returns 0.
- **Committed in:** `7e38286` (Task 1 commit).

### Deferred Enhancements

None.

---

**Total deviations:** 1 auto-fixed (Rule 3 blocking — unused import cleanup), 0 deferred.
**Impact on plan:** No scope creep; the removal was implicit in the plan's verify check.

## Issues Encountered

None.

## Next Phase Readiness

- Phase 14 is closed end-to-end across plans 14-01 (production cascade-id fix) and 14-02 (test precondition + bookkeeping).
- All seven previously disabled WorkoutDaysIntegrationTest / FullExportImportIntegrationTest cases are now active.
- ISSUES.md cleanly separates closed v0.4 work from still-open deferred items (i-3 through i-9 remain open).
- STATE.md, ROADMAP.md, and ISSUES.md are mutually consistent and ready for `/gsd:plan-phase 15` to break down sessions-core (`/sessions/start`, `/sessions/active`, `/sessions/:id/sets`, `/sessions/:id/finish`, `/sessions/history`, `/sessions/:id` plus the offline-first sync contract).
- CI verification is the standard "runs only on push" path noted in `pending_ci_fixes.md` / `local_maven_gap.md`; backend `mvn verify` exercises `FullExportImportIntegrationTest` with 6 tests run, 0 disabled.

---
*Phase: 14-workouts-hardening*
*Completed: 2026-05-04*
