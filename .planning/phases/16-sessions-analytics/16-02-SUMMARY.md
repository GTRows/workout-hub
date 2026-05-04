---
phase: 16-sessions-analytics
plan: 02
subsystem: backend-package-layout
tags: [java, spring-boot, refactor, package-extraction, analytics]

# Dependency graph
requires:
  - phase: 16-sessions-analytics
    provides: 16-01 audit Section 4 Part A package-boundary verdict
provides:
  - com.workouthub.analytics package consolidates per-exercise analytics scaffold (controller, service, PR detector, Last/Progress DTOs)
  - clean cross-package import target for SessionSetsService.PrDetector
  - co-location of Epley duplicates (analytics/PrDetector.epleyOneRm vs analytics/AnalyticsService.epley) for Phase 31 consolidation
affects: [16-03 epley-projection, 16-04 pr-durability, 31 charts-stats]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Cross-package public-static mapper call: analytics/ExerciseAnalyticsService -> sessions/SessionsMapper.toSetDto"
    - "DTO co-location with owning feature package (analytics owns LastPerformanceDto and ProgressPointDto, sessions retains SessionSetDto)"

key-files:
  created:
    - backend/src/main/java/com/workouthub/analytics/ExerciseAnalyticsController.java
    - backend/src/main/java/com/workouthub/analytics/ExerciseAnalyticsService.java
    - backend/src/main/java/com/workouthub/analytics/PrDetector.java
    - backend/src/main/java/com/workouthub/analytics/dto/LastPerformanceDto.java
    - backend/src/main/java/com/workouthub/analytics/dto/ProgressPointDto.java
    - backend/src/test/java/com/workouthub/analytics/ExerciseAnalyticsIntegrationTest.java
    - backend/src/test/java/com/workouthub/analytics/PrDetectorTest.java
    - backend/src/test/java/com/workouthub/analytics/PrDetectionIntegrationTest.java
  modified:
    - backend/src/main/java/com/workouthub/sessions/SessionSetsService.java
    - .planning/codebase/ARCHITECTURE.md

key-decisions:
  - "Direction A (extract) realized per audit Section 4 Part A; no late switch to stay-in-place"
  - "SessionsMapper kept in com.workouthub.sessions; cross-package call from analytics/ExerciseAnalyticsService is fine via public static"
  - "No back-compat re-export shim for old fully-qualified names; clean directory move"

patterns-established:
  - "Pattern: feature-sliced packages may consume public-static mappers from a peer feature without moving the mapper to a shared module"

issues-created: []

# Metrics
duration: 4 min
completed: 2026-05-04
---

# Phase 16 Plan 02: Package Extraction Summary

**Relocated 5 analytics source files plus 3 tests from `com.workouthub.sessions` to `com.workouthub.analytics`; one-import touch on `SessionSetsService`; verification gates green pending CI mvn run.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-05-04T16:53:21Z
- **Completed:** 2026-05-04T16:57:18Z
- **Tasks:** 2
- **Files modified:** 10 (8 renamed, 1 single-line import edit, 1 doc path update)

## Accomplishments

- Moved `ExerciseAnalyticsController`, `ExerciseAnalyticsService`, `PrDetector` from `com.workouthub.sessions` to `com.workouthub.analytics`.
- Moved `LastPerformanceDto`, `ProgressPointDto` from `sessions/dto` to `analytics/dto`.
- Moved `ExerciseAnalyticsIntegrationTest`, `PrDetectorTest`, `PrDetectionIntegrationTest` to `com.workouthub.analytics` test package.
- Added `import com.workouthub.analytics.PrDetector;` to `SessionSetsService` (single-line touch; call sites at lines 83 and 111 unchanged).
- Phase 15-01 audit Section 5 in-package leaks closed (5 files / 191 lines no longer dual-owned).
- Epley duplication between `analytics/PrDetector.epleyOneRm` and `analytics/AnalyticsService.epley` now visible in a single package; consolidation deferred to Phase 31 per audit Section 6 D2.
- Updated `.planning/codebase/ARCHITECTURE.md:54` PR-detector path reference to track the move.

## Task Commits

1. **Task 1: Move analytics scaffold to com.workouthub.analytics** - `35f7206` (refactor)
2. **Task 2: Verify package extraction (grep + git diff)** - no commit (verification-only; mvn deferred to CI per local-maven-gap memory)

**Plan metadata:** [hash assigned in metadata commit] (docs: complete package-extraction plan)

## Files Created/Modified

- `backend/src/main/java/com/workouthub/analytics/ExerciseAnalyticsController.java` - moved from `sessions/`; updated DTO imports to `analytics.dto`
- `backend/src/main/java/com/workouthub/analytics/ExerciseAnalyticsService.java` - moved from `sessions/`; added cross-package import for `com.workouthub.sessions.SessionsMapper`; DTO imports point at `analytics.dto`
- `backend/src/main/java/com/workouthub/analytics/PrDetector.java` - moved from `sessions/`; only package decl changed
- `backend/src/main/java/com/workouthub/analytics/dto/LastPerformanceDto.java` - moved from `sessions/dto/`; added cross-package import for `com.workouthub.sessions.dto.SessionSetDto`
- `backend/src/main/java/com/workouthub/analytics/dto/ProgressPointDto.java` - moved from `sessions/dto/`; only package decl changed
- `backend/src/main/java/com/workouthub/sessions/SessionSetsService.java` - one new import (`com.workouthub.analytics.PrDetector`); call sites untouched
- `backend/src/test/java/com/workouthub/analytics/ExerciseAnalyticsIntegrationTest.java` - moved from `sessions/`; only package decl changed
- `backend/src/test/java/com/workouthub/analytics/PrDetectorTest.java` - moved from `sessions/`; only package decl changed (PrDetector reference now same-package, unqualified)
- `backend/src/test/java/com/workouthub/analytics/PrDetectionIntegrationTest.java` - moved from `sessions/`; only package decl changed
- `.planning/codebase/ARCHITECTURE.md` - line 54 PR-detector path updated to `analytics/PrDetector.java`

## Decisions Made

- Picked Direction A (extract) per Phase 16-01 audit Section 4 Part A verdict; no late switch to stay-in-place.
- Kept `SessionsMapper` in `com.workouthub.sessions`. Cross-package call from `analytics/ExerciseAnalyticsService` is fine because both class and methods are public static. Moving `SessionsMapper` to a shared module is out of scope; it owns `SessionDto`/`SessionSummaryDto` mapping that belongs in `sessions/`.
- Did NOT introduce a back-compat shim or deprecated re-export; clean directory move per audit recommendation.
- DTO record component order preserved (6 fields on `ProgressPointDto`); plan 16-03 will append `estimatedOneRmKg` as the 7th component.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Local Maven verification gate skipped (host has no mvn, no mvnw script)**
- **Found during:** Task 2 (`./mvnw -v` and `mvn -v` both unavailable on this Windows host)
- **Issue:** Plan Task 2 verify steps require `./mvnw -q test` and `./mvnw -q -DskipTests compile test-compile`; backend has no `mvnw` script and system PATH has no `mvn` binary
- **Fix:** Verification fell back to grep + git rename detection + git diff stat. Plan compile/test gate is delegated to CI (this matches the project's local-maven-gap memory: backend tests run only in CI on this host)
- **Files modified:** none
- **Verification:** Grep for `com.workouthub.sessions.(PrDetector|ExerciseAnalyticsController|ExerciseAnalyticsService|dto.(LastPerformanceDto|ProgressPointDto))` returns zero matches across `backend/src`; `git status --short` shows 8 R + 1 M; `git diff --stat HEAD` shows 1 insertion in `SessionSetsService.java`; rename similarity scores 71-99% for the 8 moves
- **Commit:** 35f7206 (Task 1 commit; no separate commit for fallback verification)

**2. [Rule 1 - Doc drift] Updated `.planning/codebase/ARCHITECTURE.md` PR-detector path**
- **Found during:** Task 2 grep over the whole repo (excluding `.planning/` patterns the plan deemed out-of-scope)
- **Issue:** ARCHITECTURE.md line 54 referenced `backend/src/main/java/com/workouthub/sessions/PrDetector.java` after the move would render this path stale
- **Fix:** Single-line edit to point at `backend/src/main/java/com/workouthub/analytics/PrDetector.java`
- **Files modified:** `.planning/codebase/ARCHITECTURE.md`
- **Verification:** post-edit grep for the old path in `.planning/codebase/` returns zero matches
- **Committed in:** metadata commit (planning artifact, not a code commit)

### Deferred Enhancements

None - plan executed exactly as written modulo the local-maven gate.

---

**Total deviations:** 2 auto-fixed (1 blocking environmental constraint, 1 doc path drift)
**Impact on plan:** None. Both deviations preserve the plan's behavior-unchanged contract; mvn gate moves from local to CI; doc-map update is required by the workflow's `update_codebase_map` step.

## Issues Encountered

None - mechanical move with audit-listed import-path changes only. Git rename detection captured all 8 moves cleanly (71-99% similarity); diff shape matches plan's expected "5 source moves, 3 test moves, 1 single-line edit" exactly.

## Next Phase Readiness

- Plan 16-03 (epley-projection) and Plan 16-04 (pr-durability) can pick up the relocated files in `com.workouthub.analytics` without further moves.
- CI must run green on this commit before plan 16-03 starts; if CI exposes a missed import, fix-forward in 16-02-fix or fold into 16-03 (decide based on CI signal).
- Phase 31 (charts-stats) Epley consolidation marker remains open per audit Section 6 D2 (now both Epley implementations live in the same package, simplifying that work).

---
*Phase: 16-sessions-analytics*
*Completed: 2026-05-04*
