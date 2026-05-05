---
phase: 17-body-metrics
plan: 03
subsystem: api
tags: [spring-data, jpa, derived-query, request-param, localdate, response-status-exception, integration-test]

requires:
  - phase: 17-body-metrics
    provides: existing GET /api/metrics single-route full-history shape preserved by 17-02 photo-url-exposure
provides:
  - GET /api/metrics now accepts optional from/to LocalDate query params (inclusive Between, reuses idx_body_metrics_user_date)
  - Derived-query method findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc on BodyMetricRepository
  - MetricsService.list policy for full-history vs range vs 400 (mixed-pair, inverted)
  - 4 new integration tests asserting hit, inclusive bounds, empty range, and 400 paths
affects:
  - Phase 28 (metrics-ui) - chart UI consumes the from/to range read directly
  - Phase 31 (charts-stats) - if Epley-style projections need a metrics-side equivalent, the optional /series?field= projection deferral lives here

tech-stack:
  added: []
  patterns:
    - "Optional-pair query params with mixed-pair 400 contract via ResponseStatusException(BAD_REQUEST, ...)"
    - "Spring Data Between derived query reused on existing composite index"

key-files:
  created: []
  modified:
    - backend/src/main/java/com/workouthub/metrics/domain/BodyMetricRepository.java
    - backend/src/main/java/com/workouthub/metrics/MetricsService.java
    - backend/src/main/java/com/workouthub/metrics/MetricsController.java
    - backend/src/test/java/com/workouthub/metrics/MetricsIntegrationTest.java

key-decisions:
  - "Mixed-pair (only one of from/to) returns 400 instead of partial-range - single explicit contract for the chart UI"
  - "Inverted range (from > to) returns 400 - empty-result silently masks a client bug"
  - "Replaced single-arg list(UUID) instead of overloading - controller is the only caller, overload would dilute the policy"
  - "Repo uses Spring Data Between (inclusive on both bounds), reusing idx_body_metrics_user_date"
  - "No DTO projection - chart UI gets full BodyMetricDto per row; /series?field= projection deferred to Phase 28/31"

patterns-established:
  - "Optional-pair query-param 400s: validate fromSet xor toSet before isAfter check to avoid NPE on the inverted check"

issues-created: []

duration: 1 min
completed: 2026-05-05
---

# Phase 17 Plan 03: Time-Series Range Summary

**from/to LocalDate query params on GET /api/metrics with mixed-pair and inverted-range 400s; new derived repo method; 4 integration tests; no migration, no DTO change.**

## Performance

- **Duration:** 1 min
- **Started:** 2026-05-05T15:38:20Z
- **Completed:** 2026-05-05T15:39:39Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- `BodyMetricRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc(UUID, LocalDate, LocalDate)` derived-query method appended below `findByIdAndUserId` (`:17`); reuses `idx_body_metrics_user_date` per V6.
- `MetricsService.list` signature changed from `(UUID)` to `(UUID, LocalDate, LocalDate)`; branches full-history (both null), range (both set), or 400 with `ResponseStatusException(BAD_REQUEST, ...)` on mixed-pair (`fromSet ^ toSet`) and inverted (`from.isAfter(to)`); mixed-pair check ordered before inverted check to avoid NPE.
- `MetricsController.list` exposes `@RequestParam(required = false) LocalDate from` and `to`; ISO-8601 `yyyy-MM-dd` binding via Spring Boot defaults; invalid date strings flow to existing `GlobalExceptionHandler.handleTypeMismatch` 400 (no controller-side validation).
- 4 new integration tests appended to `MetricsIntegrationTest`: `rangeFilterReturnsOnlyMatchingRows`, `rangeFilterIsInclusiveOnBothBounds`, `rangeWithNoMatchingRowsReturnsEmptyArray`, `rangeMixedOrInvertedReturns400` (3 `mvc.perform` blocks: partial-from, partial-to, inverted). `seedMetric(auth, recordedDate, weightKg)` private helper consolidates the POST setup.
- Test method count grew 7 to 11.

## Task Commits

1. **Task 1: Add repo method, branch service.list on from/to, expose query params on controller** - `d3bba6e` (feat)
2. **Task 2: Add 4 integration tests for range filter, empty range, mixed-pair 400, inverted 400** - `660d75d` (test)

**Plan metadata:** _to be filled at metadata commit_

## Files Created/Modified

- `backend/src/main/java/com/workouthub/metrics/domain/BodyMetricRepository.java` - 1 new derived-query method (3 lines)
- `backend/src/main/java/com/workouthub/metrics/MetricsService.java` - signature change + branching logic + 2 ResponseStatusException throws + 4 imports
- `backend/src/main/java/com/workouthub/metrics/MetricsController.java` - 2 new `@RequestParam` params + 2 imports
- `backend/src/test/java/com/workouthub/metrics/MetricsIntegrationTest.java` - 4 new `@Test` methods + 1 private helper

## Decisions Made

- Mixed-pair (only one of `from`/`to`) returns 400 instead of partial-range. Single explicit contract for the chart UI.
- Inverted range (`from > to`) returns 400. Empty-result silently masks a client bug.
- Replaced single-arg `list(UUID)` rather than overloading. Controller is the only caller; overload would dilute the policy.
- Repo uses Spring Data `Between` (inclusive on both bounds), reusing `idx_body_metrics_user_date`. No JPQL needed.
- No DTO projection; chart UI gets full `BodyMetricDto` per row. `/series?field=` projection deferred to Phase 28/31.

## Deviations from Plan

None - plan executed exactly as written. The Task 2 spec already collapsed the 5-test draft to 4 tests by folding partial-from + partial-to + inverted into a single `rangeMixedOrInvertedReturns400` method; followed that final shape verbatim. Added a private `seedMetric` helper inside the test class to keep the 3 seeding tests terse - this is a local-only refactor inside the test file already touched, not a deviation.

## Issues Encountered

None. Local mvn verification gate skipped per `local-maven-gap` memory; full compile + test gate delegated to CI.

## Next Phase Readiness

- Plan 17-04 (status-code-split) is the last Phase 17 plan: `MetricsService.upsert` returns `(BodyMetricDto, boolean wasCreated)` wrapper; controller maps 201 on create / 200 on update; `repeatedPostForSameDateUpdatesInsteadOfInserting` flips the second-POST assertion from `isCreated()` to `isOk()`. Optional `BodyMetricValidator` static-helper absorb if scope budget allows.
- Phase 17 progress 3/4. After 17-04, Phase 17 closes; Phase 18 (export-refinement) follows.
- ROADMAP Phase 17 deliverable "time-series read endpoints for the metrics UI" closed on the API surface.
- ProjectBrief "Kilo grafigi (haftalik/aylik/tum zamanlar)" lifts from Partial: server-side range read present; client-side bucketing (weekly/monthly aggregation) lives in Phase 28 metrics-ui.

---
*Phase: 17-body-metrics*
*Completed: 2026-05-05*
