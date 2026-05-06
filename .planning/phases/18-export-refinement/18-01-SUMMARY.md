---
phase: 18-export-refinement
plan: 01
subsystem: exports
tags: [audit, gap-analysis, contract-drift, cross-package-coupling, round-trip, naming-convention, planning]

requires:
  - phase: 17-body-metrics
    plan: 03
    reason: MetricsService.list(userId, from, to) is the body_metrics source-of-truth in Section 5 Part B
  - phase: 16-sessions-analytics
    plan: 04
    reason: SessionSet.isPr persistence and PrDetector.epleyOneRm are referenced for Section 4 Part F (is_pr round-trip drop) and Section 5 Part A (prs source-of-truth)
  - phase: 16-sessions-analytics
    plan: 02
    reason: PrDetector lives in com.workouthub.analytics post-extraction; cross-package import path
  - phase: 14-workouts-hardening
    plan: 02
    reason: i-2 closure was test-side; Section 4 Part F re-checks production-side round-trip stability

provides:
  - exports package inventory (14 source files + 4 DTOs + 3 test classes + repository touchpoint table)
  - endpoint + DTO catalog (16 endpoints, ClaudeSummaryDto + FullExportDto + ImportResultDto record-component breakdowns)
  - ProjectBrief example field-by-field gap matrix (top-level + user + summary + workouts + sets + missing-keys)
  - JSON property naming convention verdict (Direction C per-consumer @JsonNaming on ClaudeSummaryDto only)
  - PRs source-of-truth verdict (Option 1 PrDetector per-set scan in window)
  - body_metrics source-of-truth (Option 1 MetricsService.list)
  - consistency source-of-truth (Option C-1 missed_reasons:[] with NEW ISSUE i-11 deferral)
  - round-trip stability re-check post-Plan-14-02 (is_pr drop surfaces as NEW ISSUE i-10 candidate; workoutDayId validator gap surfaces as second Plan 18-04 fix)
  - recommended Phase 18 plan-02+ scope (claude-summary-fields / naming-convention / roundtrip-stability / defer buckets)
  - explicit plan boundaries with one-line objective per plan

affects:
  - 18-export-refinement/18-02
  - 18-export-refinement/18-03
  - 18-export-refinement/18-04
  - 19-api-contract-docs (export wire-format documentation lands in docs/EXPORT_FORMAT.md before Phase 19)
  - ISSUES.md (i-10 round-trip is_pr drop, i-11 missed_workouts table - NEW issue candidates; opening ISSUES.md edits is owned by Plan 18-04)

tech-stack:
  added: []
  patterns:
    - first per-record @JsonNaming usage in backend/src (this audit declares the precedent for Phase 18-03)

key-files:
  created:
    - .planning/phases/18-export-refinement/18-01-AUDIT.md
  modified: []

key-decisions:
  - "Naming-convention verdict: Direction C (per-consumer @JsonNaming on ClaudeSummaryDto and its 6 nested records only; FullExportDto/ImportResultDto/CsvImportResultDto stay camelCase)"
  - "PRs source verdict: Option 1 (PrDetector per-set Epley scan in window via ExportService.computePrsInWindow private static helper)"
  - "Body metrics source verdict: Option 1 (MetricsService.list(userId, from, to) projection to (date, weight_kg))"
  - "Consistency source verdict: Option C-1 (missed_reasons:[] always; current_streak_days re-derived locally; missed_days derived from active plan dayOfWeek - session dates)"
  - "goals array shape verdict: option (1) split-at-claude-summary-export-time on newline-or-semicolon delimiter; full-export shape stays single String matching SQL TEXT column"
  - "Round-trip is_pr drop verdict: raise as NEW ISSUE i-10 candidate; bucket in Plan 18-04 (add SetRow.isPr 9th component + recompute hook safety net)"
  - "session.workoutDayId validator gap: bucket in Plan 18-04 (add ImportValidator.validateSessionDayIds rule)"
  - "Plan-count for Phase 18 hardening: 4 plans total (18-01 audit + 18-02 claude-summary-fields + 18-03 naming-convention + 18-04 roundtrip-stability); goals-array-shape absorbed into 18-02; validator-extraction not needed (full-export side already has ImportValidator scaffolding)"
---

# Phase 18 Plan 01: export-refinement audit Summary

The audit produces the single source of truth for Phase 18 plan-02+ scope: 3 missing top-level keys on `ClaudeSummaryDto` (`prs`, `body_metrics`, `consistency`), 6 subkey drifts (user.age + goals array, summary.planned_workouts/adherence_percent/weight_change_kg, workouts.type + energy + set key rename), Direction C @JsonNaming verdict for the LLM-paste claude-summary contract only, and 2 NEW ISSUE candidates (i-10 is_pr round-trip drop, i-11 missed_workouts table) bucketed across 3 hardening plans.

## Accomplishments

- Inventory complete: 14 exports source files (1489 lines) + 4 DTOs (174 lines) + 3 test classes (398 lines) + cross-package repository touchpoint table mapping 11 distinct repository methods to V1/V2/V5/V6/V20/V26/V27 migrations.
- Endpoint + DTO catalog: 16 endpoints (the plan estimated 11; per-slice import POSTs and CSV pair were undercounted), 3 export DTOs with all 27 + 66 + 8 + 4 components enumerated, 11 tests cataloged with file:line.
- ProjectBrief example gap matrix: top-level (3 missing: prs, body_metrics, consistency), user (2 missing: age + goals-array-shape; 1 extra: email), summary (3 missing: planned_workouts, adherence_percent, weight_change_kg), workouts (1 missing: type; 2 key drift: energy/energyLevel, set weight/reps vs weightKg/repsDone) cataloged.
- Naming-convention verdict: Direction C - per-record @JsonNaming on ClaudeSummaryDto + 6 nested records only; full-export/import/csv DTOs stay camelCase. Cross-search confirmed zero pre-existing @JsonNaming in backend/src.
- PRs source-of-truth verdict: Option 1 (per-set Epley scan via PrDetector.epleyOneRm in window). Options 2 (SessionSet.isPr) and 3 (AnalyticsService.personalRecords) rejected for window-scope mismatch.
- Body metrics source-of-truth: Option 1 (MetricsService.list projection to date + weight_kg).
- Consistency source-of-truth: Option C-1 (missed_reasons:[] always); current_streak_days re-derived locally (StreakCalculator all-time semantic mismatch with window); missed_days from active plan minus session dates; missed_workouts table deferred to NEW ISSUE i-11.
- Round-trip stability: post-Plan-14-02 re-check surfaces is_pr drop as NEW ISSUE i-10 candidate (FullExportDto.SetRow has 8 components, no isPr; FullImportService.insertSessions does not preserve - all sets flip to false post-roundtrip). Second gap: ImportValidator does not pre-validate session.workoutDayId against payload's plan day id set; FK violation surfaces as 500 instead of curated 422. Both fold into Plan 18-04. Plan-context-paragraph claim that parseTiming silently downgrades is corrected: parseTiming throws 422 (strict valueOf); only validator-side warning is tolerant.
- Phase 18 plan-02+ scope: 3 buckets populated (claude-summary-fields, naming-convention, roundtrip-stability) + 7 defer markers including NEW ISSUE i-11 candidate.
- Plan-count recommendation: 4 plans (18-01 + 18-02 + 18-03 + 18-04). No migrations needed - V27 already provides is_pr; V28 stays free for Phase 31 / v0.6.

## Files Created/Modified

- `.planning/phases/18-export-refinement/18-01-AUDIT.md` (483 lines, 6 sections) - audit deliverable.
- `.planning/phases/18-export-refinement/18-01-PLAN.md` - committed alongside Task 1 (was untracked from a prior planner spawn that hit a usage limit before commit).

## Decisions Made

- Naming-convention direction: Direction C (per-consumer @JsonNaming).
- PRs source-of-truth: Option 1 (PrDetector per-set scan in window).
- body_metrics source-of-truth: Option 1 (MetricsService.list projection).
- Consistency source-of-truth: Option C-1 (missed_reasons:[] with i-11 deferral).
- goals array shape direction: option (1) split-at-claude-summary-export-time.
- Round-trip is_pr drop bucket: Plan 18-04 (NEW ISSUE i-10 candidate).
- session.workoutDayId validator gap bucket: Plan 18-04 (NEW validator rule).
- Phase 18 plan boundaries: 4 plans (1 audit + 3 hardening).

## Issues Encountered

- NEW ISSUE i-10 candidate: is_pr round-trip drop on full-export import path. SetRow.isPr is absent from FullExportDto, so Phase 16-04's persisted is_pr column silently flips to false on every round-trip. Plan 18-04 lands the fix.
- NEW ISSUE i-11 candidate: missed_workouts table absent. consistency.missed_reasons has no data source today; Option C-1 emits [] always. Trigger Phase 31 (charts-stats) or v0.6 milestone planning.
- Plan estimate vs actual: the plan stated "Eleven rows expected" for endpoint catalog; the actual count is 16. The plan undercounted the per-slice import POSTs (`/api/export/import/{profile,metrics,supplements,plans,sessions}`) and the CSV pair (`/api/export/import/csv`, `/api/export/csv/sessions`). Audit catalogs all 16 faithfully.
- Plan context paragraph misreading: claimed parseTiming "may silently downgrade unknown values"; source inspection shows parseTiming at FullImportService.java:318-327 throws 422 via strict `valueOf`. Only the validator-side warning is tolerant. Corrected in Section 4 Part F.

## Next Phase Readiness

- Ready for Phase 18 plan-02+ planning: yes.
- Open questions for plan 18-02+: none material; all source-of-truth verdicts are landed. Plan 18-02 may revisit whether `email` should be dropped from ClaudeSummaryDto.UserSummary (brief example does not have it; dropping is the cleaner cut but requires updating ExportIntegrationTest's `$.user.email` assertion at ExportIntegrationTest.java:60).
- Open ISSUES.md edits owned by Plan 18-04: i-10 (round-trip is_pr drop) and i-11 (missed_workouts table). This audit only flags them as candidates per the plan's "out of scope: opening ISSUES.md edits".
