---
phase: 16-sessions-analytics
plan: 01
subsystem: analytics
tags: [audit, gap-analysis, package-boundary, pr-durability, planning]

requires:
  - phase: 15-sessions-core
    plan: 01
    reason: ownership map (Section 5) and Phase 16 scope-leak inventory inherited from this plan
  - phase: 15-sessions-core
    plan: 04
    reason: create-only newPr verdict and SessionSetsIntegrationTest.newPrFlagPresentOnCreateButOmittedOnDetailReread regression baseline that Phase 16 PR durability work must respect or replace

provides:
  - analytics scaffold inventory (5 Phase 16-owned files plus 4 shared call sites)
  - endpoint and DTO catalog (2 endpoints, 2 response DTOs, 10 record components)
  - ProjectBrief Phase 4 plus ROADMAP Phase 16 gap matrix (2-of-2 brief endpoints implemented; 2-of-3 ROADMAP deliverables fully landed plus 1 partial)
  - package-boundary verdict (extract to com.workouthub.analytics; package already exists with charts-stats scaffold)
  - perf baseline analysis with row-count estimate (~800 rows for power user, Java-side acceptable for v0.4)
  - PR durability verdict (persist is_pr via V27 with window-function backfill)
  - recommended Phase 16 plan-02+ scope (1 package-extraction entry, 1 epley-projection entry, 3 pr-durability entries, 0 perf-pushdown entries, 5 defer markers)
  - explicit plan boundaries for 4 plans (16-01 audit, 16-02 package-extraction, 16-03 epley-projection, 16-04 pr-durability)

affects:
  - 16-sessions-analytics/16-02 (package-extraction)
  - 16-sessions-analytics/16-03 (epley-projection)
  - 16-sessions-analytics/16-04 (pr-durability)
  - 31-charts-stats (perf-pushdown deferral marker; Epley consolidation marker)

tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - .planning/phases/16-sessions-analytics/16-01-AUDIT.md
  modified: []

key-decisions:
  - "Package-boundary verdict: extract to com.workouthub.analytics. Grep confirms PrDetector cross-package consumer is only SessionSetsService; the analytics package already exists (charts-stats scaffold landed pre-GSD with Epley duplication waiting to be consolidated). Move is mechanical: 5 files, 1 import line in SessionSetsService, 3 test package declarations."
  - "PR durability verdict: persist is_pr via V27 with window-function backfill. Postgres 16 supports ROW_NUMBER OVER natively; Epley expression is inline arithmetic (no UDF). Frontend Phase 25 and analytics Phase 31 both gain a stable badge; PUT update path re-evaluation cost is bounded."
  - "Perf verdict: Java-side aggregation acceptable for v0.4. Power-user estimate of 800 rows fetched per /progress request is within Postgres+Hibernate budget for single-tenant self-hosted; convention matches AnalyticsService.weeklyVolume and oneRepMax. SQL pushdown deferred to Phase 31."
  - "Plan-count for Phase 16 hardening: 4 plans (16-01 audit shipped, 16-02 package-extraction, 16-03 epley-projection, 16-04 pr-durability)."
---

# Phase 16 Plan 01: sessions-analytics audit Summary

Audit confirms Phase 16 inherits a fully-implemented endpoint surface from Phase 15 in-package leaks; remaining work is formalization (extract to existing analytics/ package), one missing DTO field (estimatedOneRmKg), and one durability gap (V27 is_pr persist).

## Accomplishments

- Inventory complete: 5 Phase 16-owned files (191 lines) plus 4 shared call sites under `sessions/`. 3 dedicated test files (409 lines) cover analytics and PR detection paths.
- Endpoint and DTO catalog: 2 endpoints (`GET /api/exercises/{id}/last-performance` and `GET /api/exercises/{id}/progress`), 2 response DTOs, 10 record components total.
- ProjectBrief Phase 4 special endpoints: 2-of-2 implemented; ROADMAP Phase 16 deliverables: 2-of-3 fully landed (PR computation create-time; volume aggregation), 1 partial (1RM Epley logic exists but is not exposed on `ProgressPointDto`).
- Package-boundary verdict: extract to `com.workouthub.analytics`. The package already exists with charts-stats scaffold (`AnalyticsController` plus `AnalyticsService`, 237 lines of pre-GSD analytics endpoints) and re-implements Epley locally; extraction lets future plans consolidate the duplication.
- Perf baseline: Java-side aggregation acceptable for v0.4. Estimated ~800 rows fetched per `/progress?limit=10` request for a power user with 200 sessions x 4 sets per exercise; convention matches existing `AnalyticsService.weeklyVolume` and `oneRepMax` aggregation patterns.
- PR durability verdict: persist `is_pr` via V27 migration with window-function backfill (`ROW_NUMBER() OVER (PARTITION BY user_id, exercise_id ORDER BY epley DESC)`). SQL drafted inline in audit Section 5; feasibility verified against Postgres 16.
- Phase 16 plan-02+ scope: 1 package-extraction entry (PE1), 1 epley-projection entry (EP1), 3 pr-durability entries (PD1 V27 migration plus backfill, PD2 entity plus mapper plus service, PD3 update path re-evaluation), 0 perf-pushdown entries, 5 defer markers.
- Plan-count recommendation: 4 plans for Phase 16. 16-01 (this audit) shipped; 16-02 package-extraction; 16-03 epley-projection; 16-04 pr-durability.

## Files Created/Modified

- `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` - audit deliverable (297 lines, 6 sections).

## Decisions Made

- Package-boundary: extract to `com.workouthub.analytics` (Section 4 Part A).
- PR durability: persist `is_pr` via V27 (Section 5).
- Perf direction: Java-side aggregation for v0.4; SQL pushdown deferred to Phase 31 (Section 4 Part B).
- Phase 16 plan boundaries: 4 plans total, one bug-shape per plan per Phase 14/15 precedent (Section 6).

## Issues Encountered

- **Pre-existing `com.workouthub.analytics` package not anticipated by the plan context.** The plan's Section 4 Part A walked the extract direction assuming the namespace was empty. The audit found `analytics/AnalyticsController` plus `AnalyticsService` (5 endpoints under `/api/analytics/{volume,one-rm,streak,prs,heatmap}`, 237-line service) already in place from pre-GSD work. This LOAD-BEARING discovery strengthens the extract verdict because the destination is already populated; it also surfaces an Epley-implementation duplication (`sessions/PrDetector.epleyOneRm` vs `analytics/AnalyticsService.epley`) that Phase 31 (charts-stats) should consolidate. Documented in Section 4 Part A "Adjacent finding" and Section 6 defer bucket D2.
- No file:line citations in the plan turned out to be stale; all referenced controllers, services, DTOs, and repository methods are at the cited locations or within +/- 2 lines.

## Next Phase Readiness

- Ready for Phase 16 plan-02+ planning: yes. Plan boundaries are explicit; file-level scope per plan is concrete.
- Open questions for plan 16-02+: 
  - PE1 (package-extraction): does the move retain the `com.workouthub.sessions.dto.LastPerformanceDto` type FQDN as a deprecated alias, or is the type a strict-rename? Recommend strict rename; no other backend feature imports the DTO.
  - PD2 (pr-durability): keep `Boolean isPr` (nullable, NON_NULL omits when false) or `boolean isPr` (always serialized)? Recommend nullable to mirror current `newPr` shape and minimize diff in the rewritten test.

---
*Phase: 16-sessions-analytics*
*Completed: 2026-05-04*
