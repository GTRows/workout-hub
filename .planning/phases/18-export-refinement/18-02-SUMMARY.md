---
phase: 18-export-refinement
plan: 02
subsystem: exports
tags: [claude-summary, dto-extension, cross-package-coupling, prs, body-metrics, consistency, goals-array]

requires:
  - phase: 18-export-refinement
    plan: 01
    reason: 18-01 audit produced the source-of-truth verdicts and gap matrix that this plan implements
  - phase: 17-body-metrics
    plan: 03
    reason: MetricsService.list(userId, from, to) is the body_metrics projection source per Section 5 Part B
  - phase: 16-sessions-analytics
    plan: 02
    reason: PrDetector.epleyOneRm lives in com.workouthub.analytics post-extraction (Section 5 Part A)

provides:
  - ClaudeSummaryDto extended with prs, bodyMetrics, consistency top-level keys
  - UserSummary with age + goals as List<String>; email removed
  - Totals with plannedWorkouts, adherencePercent, weightChangeKg
  - WorkoutEntry with type and energy (rename from energyLevel)
  - SetEntry with weight/reps key rename and order swap
  - ExportService with 2 new collaborators (WorkoutPlanRepository, MetricsService) and 8 helpers
  - 2 new integration tests covering profile-driven (age + goals + type) and metrics-driven (bodyMetrics + weightChangeKg) flows

affects:
  - 18-export-refinement/18-03 (naming-convention plan flips assertions on the new keys; no DTO field changes needed)
  - 18-export-refinement/18-04 (roundtrip-stability plan operates on FullExportDto, untouched here)

tech-stack:
  added: []
  patterns:
    - cross-package read seam through MetricsService.list rather than direct repository injection
    - in-window per-set Epley scan as the prs source-of-truth (vs persisted is_pr column or all-time AnalyticsService)

key-files:
  created: []
  modified:
    - backend/src/main/java/com/workouthub/exports/dto/ClaudeSummaryDto.java
    - backend/src/main/java/com/workouthub/exports/ExportService.java
    - backend/src/test/java/com/workouthub/exports/ExportIntegrationTest.java

key-decisions:
  - "Reach WorkoutDay through WorkoutPlan.getDays() rather than injecting a separate WorkoutDayRepository - the active plan's day collection already carries everything needed for type derivation and missed_days calculation."
  - "Re-derive currentStreakDays locally instead of calling analytics/StreakCalculator.compute (semantic mismatch: all-time vs in-window)."
  - "Goals split delimiter is regex \\r?\\n|; with trim + drop-empty; documented in 19's docs/EXPORT_FORMAT.md."
  - "adherencePercent is uncapped (>100 possible for power users); no clamp."
  - "Empty list defaults for prs, bodyMetrics, missedDays, missedReasons, goals; Jackson NON_NULL emits [] not null."
  - "Used java.time.Period via fully-qualified name in computeUserAge to avoid clash with ClaudeSummaryDto.Period nested record."
---

# Phase 18 Plan 02: claude-summary-fields Summary

ClaudeSummaryDto extended to match ProjectBrief example (prs, bodyMetrics, consistency, summary subkeys, user.age, goals array, workouts.type, energy/reps/weight renames); ExportService wires PrDetector + MetricsService + WorkoutPlanRepository; 6 test methods (2 new) covering the new fields; closes 18-01-AUDIT Section 6 claude-summary-fields bucket.

## Accomplishments

- `ClaudeSummaryDto` grew from 7 records / 27 components to 10 records / ~37 components.
- `UserSummary`: dropped `email`, added `Integer age`, switched `goals` to `List<String>`.
- `Totals`: added `plannedWorkouts`, `adherencePercent`, `weightChangeKg` (3 boxed/null-safe Integer/BigDecimal fields).
- `WorkoutEntry`: added `type` and renamed `energyLevel` -> `energy`.
- `SetEntry`: renamed `(repsDone, weightKg)` -> `(weight, reps)` with order swap.
- New nested records: `PrEntry`, `BodyMetricSummary`, `Consistency`.
- `ExportService` constructor: 3 -> 5 collaborators (`WorkoutPlanRepository`, `MetricsService` added).
- 8 new helpers: `computePrsInWindow`, `computeBodyMetrics`, `computeConsistency`, `computePlannedWorkouts`, `computeWeightChangeKg`, `computeUserAge`, `splitGoals`, `collectDayNames`.
- `ExportIntegrationTest` `@Test` count: 4 -> 6. Existing test refreshed; 2 new tests cover profile-driven and metrics-driven flows.

## Files Created/Modified

- `backend/src/main/java/com/workouthub/exports/dto/ClaudeSummaryDto.java` - record-type extension.
- `backend/src/main/java/com/workouthub/exports/ExportService.java` - constructor + 8 helpers + buildSummary rewrite.
- `backend/src/test/java/com/workouthub/exports/ExportIntegrationTest.java` - assertion flips + 2 new test methods.

## Decisions Made

- WorkoutDay reachable via `WorkoutPlan.getDays()`; no `WorkoutDayRepository` injection.
- `currentStreakDays` re-derived locally; `analytics/StreakCalculator.compute` not called.
- Goals delimiter: `\r?\n|;`; trim + drop-empty.
- `adherencePercent` uncapped.
- Empty list defaults for all collection components.
- `java.time.Period` used via fully-qualified reference inside `computeUserAge` to avoid clash with the existing `ClaudeSummaryDto.Period` import; only the latter is imported at file scope.

## Issues Encountered

- Naming clash between `java.time.Period` and `ClaudeSummaryDto.Period` blocked importing both. Resolved by importing only the DTO record and calling `java.time.Period.between(...)` fully qualified inside the single helper that needed it.
- Local `mvn`/`mvnw.cmd` unavailable on this Windows host (per local-maven-gap memory). Compile and test gates delegated to CI.

## Next Phase Readiness

- Plan 18-03 (`naming-convention`) is unblocked - the DTO surface is now fixed for the snake_case flip.
- Plan 18-04 (`roundtrip-stability`) is unblocked and operates on a different DTO (`FullExportDto`).
- Phase 19 (`api-contract-docs`) gains content to document: the new claude-summary shape, the goals delimiter, the adherence-no-clamp note.
