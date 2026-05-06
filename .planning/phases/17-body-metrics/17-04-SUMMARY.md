# Phase 17 Plan 04: Status-Code Split Summary

POST /api/metrics now returns 201 on create / 200 on update via UpsertResult(dto, wasCreated) wrapper; existing test assertion flipped + id-stability lock added; closes Phase 17 4/4.

## Accomplishments

- New `MetricsService.UpsertResult(BodyMetricDto dto, boolean wasCreated)` inner record (mirrors `SessionSetsService.AddSetResult` precedent from Phase 15-02).
- `MetricsService.upsert` signature changed from `BodyMetricDto` to `UpsertResult`; `wasCreated` derived from `Optional.isEmpty()` on the lookup before the entity is mutated.
- `MetricsController.upsert` maps `wasCreated ? 201 : 200` via the same shape as `SessionSetsController.add`.
- `repeatedPostForSameDateUpdatesInsteadOfInserting` second-POST assertion flipped from `isCreated()` to `isOk()`.
- New test `firstPostReturns201SecondPostReturns200` locks the create/update status pair and `$.id` stability across the upsert. Test method count 11 to 12.

## Files Created/Modified

- `backend/src/main/java/com/workouthub/metrics/MetricsService.java` — `UpsertResult` inner record + `upsert` signature/branch refactor
- `backend/src/main/java/com/workouthub/metrics/MetricsController.java` — `UpsertResult` import + status-code mapping
- `backend/src/test/java/com/workouthub/metrics/MetricsIntegrationTest.java` — 1 assertion flip + 1 new `@Test` method

## Decisions Made

- **Inner record over top-level file.** Mirrored `SessionSetsService.AddSetResult` (Phase 15-02). Avoids a new file for a 2-component wrapper.
- **`Optional.isEmpty()` over `entity.getId() == null`.** Explicit about intent; decoupled from JPA `@UuidGenerator` internals.
- **Boolean named `wasCreated` (positive form).** Direct mapping to `wasCreated ? CREATED : OK` reads naturally for HTTP-status semantics (vs Phase 15-02's `idempotentHit` which framed retry semantics).
- **Validator absorb deferred to Phase 31.** Cross-package edits to `webhooks/ScaleWebhookController` and `health/HealthImportService` involve heterogeneous conflict semantics and would dilute this plan's focus.
- **Did not split into `create` + `update` service methods.** Direction A verdict from the audit; date-keyed contract stays a single entry point.
- **Did not introduce `PUT /api/metrics/{id}`.** Audit Section 4 Part B verdict; V6 UNIQUE-by-`(user, date)` encodes date-keyed identity by design.

## Issues Encountered

None. Both tasks landed cleanly with verification greps passing on first run. Backend compile/test gating delegated to CI per local-maven-gap memory.

## Next Phase Readiness

- **Phase 17 complete (4/4 plans shipped).** Body metrics package now closes ROADMAP Phase 17 deliverables: weight + measurements (pre-existing), photo URL (17-02), time-series range (17-03), HTTP status correctness (17-04). Validator absorb stays open as a Phase 31 marker.
- **Next phase:** Phase 18 (`export-refinement`). `/gsd:plan-phase 18-01`. v0.4 milestone progresses to 5/8 phases complete.
- **Phase 31 marker:** `BodyMetricValidator` static helper for cross-package writers (`ScaleWebhookController.ingest`, `HealthImportService.apply`). Each writer's conflict semantic stays intact; only field-bounds rule consolidates.
