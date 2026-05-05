---
phase: 17-body-metrics
plan: 01
subsystem: metrics
tags: [audit, gap-analysis, contract-drift, cross-package-coupling, planning]

requires:
  - phase: 16-sessions-analytics
    plan: 04
    reason: V27 migration ordering; V28 is the next free Flyway slot, but Phase 17 needs no schema work because every column already exists in V6
  - phase: 15-sessions-core
    plan: 02
    reason: clientSetId 200/201 status-code split precedent applied to MetricsController.upsert in Section 4 Part A

provides:
  - metrics package inventory (6 source files / 309 lines + 1 test class / 130 lines + V6 SQL column table)
  - endpoint + DTO catalog (3 endpoints, BodyMetricDto 12 components, UpsertBodyMetricRequest 8 components, 6 tests)
  - schema column drift matrix (9 rows; photo_url is the lone GAP)
  - ProjectBrief Phase 5 #7 + ROADMAP Phase 17 deliverable status
  - status-code semantics verdict for MetricsController.upsert (Direction A - 200 update / 201 create via service-returns-(dto, wasCreated) wrapper)
  - cross-package coupling map (5 direct BodyMetric call sites outside MetricsService)
  - time-series endpoint shape verdict (Option 1 - from/to query parameters on GET /api/metrics)
  - validator-extraction verdict (deferred - keep each writer with its own bounds; absorb into 17-04 only if scope budget allows)
  - recommended Phase 17 plan-02+ scope (photo-url-exposure / time-series-range / status-code-split / deferred validation-extraction / defer buckets)
  - explicit plan boundaries (3 hardening plans 17-02..17-04 plus this audit)

affects:
  - 17-body-metrics/17-02 (photo-url-exposure)
  - 17-body-metrics/17-03 (time-series-range)
  - 17-body-metrics/17-04 (status-code-split, optionally absorbing validation-extraction)
  - 18-export-refinement (FullExportService.MetricRow already carries photoUrl; verify alignment after 17-02)
  - 28-metrics-ui (time-series endpoint shape will inform chart wiring)

tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - .planning/phases/17-body-metrics/17-01-AUDIT.md
  modified: []

key-decisions:
  - "Status-code verdict: Direction A - service returns (BodyMetricDto, boolean wasCreated); controller maps 201 on create / 200 on update; matches Phase 15-02 clientSetId precedent"
  - "Time-series verdict: Option 1 - extend GET /api/metrics with optional from/to LocalDate query params; reuses idx_body_metrics_user_date; defers chart-projection endpoint to Phase 28/31"
  - "Validator-extraction verdict: defer; each writer has different conflict semantics (idempotent-by-date / skip-on-conflict / wholesale-replace); absorb into 17-04 only if scope budget allows"
  - "Plan-count for Phase 17 hardening: 4 plans total (17-01 audit + 17-02 photo-url + 17-03 time-series + 17-04 status-code-split with optional validator absorb)"
  - "PUT-by-id endpoint deferred indefinitely; V6 UNIQUE (user_id, recorded_date) encodes date-keyed identity by design"
---

# Phase 17 Plan 01: body-metrics audit Summary

**Metrics package audit landed: photo_url is the lone column GAP, time-series range read missing, POST status-code drift on update branch; plan boundaries 17-02 photo / 17-03 series / 17-04 status, with validator extraction deferred unless scope absorbs.**

## Accomplishments

- Inventory complete: 6 metrics package source files / 309 lines + 1 test class / 130 lines + V6 column table.
- Endpoint + DTO catalog: 3 endpoints (GET / POST / DELETE), BodyMetricDto 12 components, UpsertBodyMetricRequest 8 components, 6 tests.
- Schema column drift: 9 rows across ProjectBrief / V6 / entity / response DTO / upsert request; `photo_url` is the only GAP.
- ProjectBrief Phase 5 #7 deliverables: 0-of-3 fully implemented, 2 partial, 1 gap on the API surface.
- ROADMAP Phase 17 deliverables: 2-of-4 implemented, 1 partial (photo URL on schema/entity/response yes; request DTO no), 1 missing (time-series read endpoints).
- Status-code semantics verdict: Direction A - service returns (dto, wasCreated) wrapper; controller maps 201 vs 200; Phase 15-02 precedent.
- Cross-package coupling: 5 direct BodyMetric call sites cataloged; 2 write paths (ScaleWebhookController, HealthImportService) bypass MetricsService validation.
- Time-series endpoint verdict: Option 1 - extend `GET /api/metrics` with from/to query params; index-friendly on `idx_body_metrics_user_date`.
- Validator-extraction verdict: defer; each writer has different conflict semantics; absorb into 17-04 if budget allows.
- Phase 17 plan-02+ scope: 1 photo-url entry, 1 time-series-range entry, 1 status-code-split entry, 1 deferred validation-extraction entry, 5 defer markers.
- Plan-count recommendation: 4 plans (17-01 audit + 17-02 photo-url + 17-03 time-series + 17-04 status-code-split).

## Files Created/Modified

- `.planning/phases/17-body-metrics/17-01-AUDIT.md` - audit deliverable (325 lines).

## Decisions Made

- Status-code semantics direction: A (wrapper-and-map).
- Time-series endpoint shape: Option 1 (extend GET /api/metrics with from/to).
- Cross-package validator extraction direction: defer.
- Phase 17 plan boundaries: 17-02 / 17-03 / 17-04.

## Issues Encountered

- None. ProjectBrief, V6 schema, and entity all align on `photo_url`; only the request DTO drifts. No documentation drift between codebase docs and source warranted ISSUES.md additions.

## Next Phase Readiness

- Ready for Phase 17 plan-02 planning: yes. Plan-count revised from TBD to 4.
- Open questions for plan 17-02+: range filter policy when only one of `from`/`to` is set (accept partial vs 400-on-mixed) - deferred to plan 17-03 author. Validator extraction lands inside 17-04 or stays deferred - deferred to plan 17-04 author.

---
*Phase: 17-body-metrics*
*Completed: 2026-05-05*
