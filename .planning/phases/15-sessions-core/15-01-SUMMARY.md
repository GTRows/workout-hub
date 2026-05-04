---
phase: 15-sessions-core
plan: 01
subsystem: sessions
tags: [audit, gap-analysis, offline-sync, planning]

requires: []
provides:
  - sessions package layout inventory (21 files, 965 lines)
  - endpoint catalog (10 endpoints across 3 controllers; 8 Phase 15 + 2 Phase 16)
  - ProjectBrief Phase 4 gap matrix (7-of-7 brief endpoints implemented + DELETE bonus + 2 Phase 16 leaks)
  - offline-first sync contract analysis (4a PARTIAL, 4b PARTIAL, 4c FAIL)
  - Phase 16/17 scope-leak inventory across 23 file rows
  - recommended Phase 15 plan-02+ scope (0 bug-fix, 4 coverage, 3 contract-finalization, 2 feature-gap, 5 defer)
affects: [15-sessions-core/15-02, 16-sessions-analytics]

tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - .planning/phases/15-sessions-core/15-01-AUDIT.md
  modified: []

key-decisions:
  - "Offline-sync contract verdict: must-harden in plan 15-02+; 4c is FAIL under auto-numbering, 4a and 4b PARTIAL because 409 conflates idempotent retry, set-number collision, and finished-session writes."
  - "heartRateAvgBpm exposure decision: expose read-only on SessionDto and SessionSummaryDto in plan 15-03; Garmin .fit importer remains the sole writer (no client write path), Phase 17 owns the data source."
  - "Phase 16 scope-leak verdict: document only - ExerciseAnalyticsController/Service, LastPerformanceDto, ProgressPointDto, PrDetector, and SessionSetRepository.findHistoricalByUserAndExercise stay in package today; plan 15-02+ MUST NOT modify them."

duration: 6 min
completed: 2026-05-04
---

# Phase 15 Plan 01: sessions-core audit Summary

**Sessions package audit: 7 brief endpoints implemented + DELETE bonus, offline-sync 4a/4b PARTIAL and 4c FAIL verdicts, plan-02+ scope bucketed into 4 coverage / 3 contract-finalization / 2 feature-gap / 5 defer.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-05-04T01:45:39Z
- **Completed:** 2026-05-04T01:51:55Z
- **Tasks:** 3
- **Files modified:** 1 (created)

## Accomplishments

- Inventory complete: 21 files, 965 lines under `sessions/`.
- Endpoint catalog: 10 endpoints (8 Phase 15 + 2 Phase 16); every controller method cited by `file:line`.
- ProjectBrief Phase 4 gap matrix: 7-of-7 brief endpoints implemented; DELETE on sets is in-package bonus; 2 Phase 16 endpoints (`last-performance`, `progress`) ship today and are tagged.
- Field-level drift documented: `heartRateAvgBpm` not exposed on any DTO; `newPr` set on create only and lost on detail re-read.
- Offline-first sync contract: 4a PARTIAL (409 conflates idempotent-retry with set-number collision), 4b PARTIAL (409 conflates session-finished with set-collision), 4c FAIL (auto-numbering at `SessionSetsService.java:46-48` is order-sensitive under offline drain).
- Phase 16/17 scope-leak inventory across all 23 file rows; ownership boundary is unambiguous.
- Plan-02+ scope: 0 bug-fix, 4 coverage tests, 3 contract-finalization items (`clientSetId` idempotency key, explicit `setNumber` enforcement, typed 409 error code), 2 feature-gap (heart-rate exposure, PR durability), 5 defer-to-later-phase entries.
- Plan-count recommendation: three plans (15-02 idempotency key, 15-03 heart-rate exposure, 15-04 auto-numbering verdict + typed error codes).

## Task Commits

1. **Task 1: Inventory sessions package and catalog endpoints** - `869666a` (docs)
2. **Task 2: Add Phase 4 gap matrix and offline-sync analysis** - `60780f3` (docs)
3. **Task 3: Add scope-leak inventory and plan-02+ recommendation** - `20f022f` (docs)

## Files Created/Modified

- `.planning/phases/15-sessions-core/15-01-AUDIT.md` - audit deliverable (251 lines, six sections).

## Decisions Made

- **Offline-sync contract verdict: must-harden in plan 15-02+.** The current 409-only path satisfies happy-path online flow but fails the brief's offline-first contract. Top-ranked fix is a client-supplied `clientSetId` UUID + DB column + UNIQUE index so retries return 200 with the persisted row. Fallback fixes (CF2 explicit `setNumber` enforcement, CF3 typed error codes) sit behind 15-02.
- **heartRateAvgBpm exposure: expose read-only on session DTOs in plan 15-03.** Garmin `.fit` importer at `HealthImportService.java:68` is the sole writer; no client write path needed. Phase 19 (api-contract-docs) follow-up documents the import-fed semantics.
- **Phase 16 scope-leak: document, do not move.** `ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, `PrDetector`, and `SessionSetRepository.findHistoricalByUserAndExercise` are physically in `sessions/` today. Plan 15-02+ MUST NOT modify them; Phase 16 inherits the in-package layout. `newPr` flag on create response stays (existing UX cue) but Phase 15 will not extend it.

## Deviations from Plan

None. The plan was a docs-only audit with three sequential auto tasks; all three executed exactly as written, three atomic commits landed, no ISSUES.md edits required.

## Issues Encountered

None. The audit confirmed:
- All seven ProjectBrief Phase 4 endpoints are implemented; the package has no `Missing` rows.
- The Phase 14 lesson (explicit child-repo `saveAndFlush` after `parent.addChild()`) is already applied at `SessionSetsService.java:64` (`sets.saveAndFlush(set)`); the cascade-id risk that motivated i-1 is mitigated for sessions.
- No new issues warranting `ISSUES.md` entries; the contract gaps documented in Sections 3-4 belong to plan 15-02+ scope, not the deferred-issues queue.

## Next Phase Readiness

- Ready for Phase 15 plan-02+ planning: yes.
- Open questions for plan 15-02+:
  - Plan 15-02 scope confirmation: should the `clientSetId` idempotency-key plan also bundle CF2 (explicit `setNumber` enforcement) or stay narrow on CF1?
  - Plan 15-03 fold-in: heart-rate DTO exposure is small (3 files); could fold into 15-02 if 15-02 budget allows. Default is to keep it as a separate plan per Phase 14 precedent (one feature per plan).
  - Typed error codes (CF3): confirm whether to land in plan 15-04 or push to Phase 19 (api-contract-docs) where `ApiError` is already in scope.

---
*Phase: 15-sessions-core*
*Completed: 2026-05-04*
