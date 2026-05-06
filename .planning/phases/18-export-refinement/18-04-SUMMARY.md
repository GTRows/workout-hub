---
phase: 18-export-refinement
plan: 04
subsystem: exports, sessions
tags: [full-export, round-trip, is-pr, validator, jpa, integration-test, junit5]

requires:
  - phase: 18-export-refinement
    plan: 01
    reason: 18-01 audit Section 4 Part F Gap 1+2 prescribed the wire-field + validator rule
  - phase: 16-sessions-analytics
    plan: 04
    reason: V27 column + SessionSet.isPr field + recomputePrForExerciseHistory helper landed in 16-04

provides:
  - FullExportDto.SetRow with 9 components (Boolean isPr appended)
  - FullExportService.toSessionSection populates isPr from entity
  - FullImportService.insertSessions reads isPr null-safely + recompute safety net per touched exercise
  - SessionSetsService.recomputePrForExerciseHistory widened from private to public
  - ImportValidator.validateSessionDayIds rule (referential integrity for session.workoutDayId -> plans[].days[].id)
  - 2 new integration tests in FullExportImportIntegrationTest
  - docs/EXPORT_FORMAT.md isPr row + "Acceptable round-trip drift" section

affects:
  - 19-api-contract-docs (claude-summary section in docs/EXPORT_FORMAT.md gains the round-trip drift sub already documented; OpenAPI surface for /api/export/import gets the new SetRow.isPr field)
  - frontend (no change today; FullExportDto SetRow consumers are operator-only via download/upload, not client code)

tech-stack:
  added: []
  patterns:
    - "Per-record additive 9th component on FullExportDto record (mirrors 17-02 UpsertBodyMetricRequest pattern)"
    - "Boxed Boolean for legacy-payload null tolerance vs primitive boolean (default-false silent drift)"
    - "Cross-package service injection for transactional safety net (FullImportService -> SessionSetsService.recomputePrForExerciseHistory in single @Transactional boundary)"

key-files:
  created: []
  modified:
    - backend/src/main/java/com/workouthub/exports/dto/FullExportDto.java
    - backend/src/main/java/com/workouthub/exports/FullExportService.java
    - backend/src/main/java/com/workouthub/exports/FullImportService.java
    - backend/src/main/java/com/workouthub/exports/ImportValidator.java
    - backend/src/main/java/com/workouthub/sessions/SessionSetsService.java
    - backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java
    - docs/EXPORT_FORMAT.md
    - .planning/ISSUES.md

key-decisions:
  - "is_pr round-trip durability via wire-field + recompute safety net (belt-and-braces); audit's 'AND/OR' framing taken as 'AND' because recompute cost is bounded (one call per distinct touched exercise post-loop)"
  - "Boxed Boolean on SetRow.isPr (not primitive boolean); legacy payloads from pre-18-04 dumps deserialize null cleanly"
  - "workoutDayId validator: 422 (declared-day-id set membership) selected over null-out-on-stale; matches existing validator's error pattern"
  - "recomputePrForExerciseHistory widens from private to public; visibility-only edit; no signature, no static, no annotation churn"
  - "SchemaVersion stays at 1; isPr addition is additive on the wire"
  - "FullExportDto stays camelCase per Direction C from 18-03; no @JsonNaming on this record"
  - "No schema migration; V27 already provides is_pr column"
  - "ISSUES.md i-10 entered as Closed directly (no prior Open entry); 18-04 surfaced and resolved the gap atomically"
---

# Phase 18 Plan 04: roundtrip-stability Summary

FullExportDto.SetRow gains 9th component Boolean isPr; FullExportService writes from entity, FullImportService reads null-safely + recompute safety net per touched exercise; ImportValidator gains validateSessionDayIds rule (422 on stale workoutDayId reference); 2 new integration tests; docs/EXPORT_FORMAT.md updated with isPr field + Acceptable round-trip drift section; closes 18-01-AUDIT Section 6 roundtrip-stability bucket and ISSUES.md i-10.

## Accomplishments

- `FullExportDto.SetRow` extended from 8 to 9 components.
- `FullExportService.toSessionSection` populates `set.isPr()` on export.
- `FullImportService.insertSessions` reads `r.isPr()` null-safely on import; collects touched exercise ids; orchestrator invokes `sessionSets.recomputePrForExerciseHistory` once per distinct touched exercise after the insert loop.
- `FullImportService.replaceSessionsSection` mirrors the same recompute pass for the per-slice import path.
- `SessionSetsService.recomputePrForExerciseHistory` widened from private to public (visibility-only edit).
- `ImportValidator.validateSessionDayIds` rule added; collects `Set<UUID>` from `plans[].days[].id`, walks `sessions[]`, errors any non-null `workoutDayId` not in the declared set.
- 2 new integration tests: `prFlagSurvivesFullExportImportRoundTrip` (PR durability across export -> import -> read), `importRejectsSessionWithUnknownWorkoutDayId` (422 on stale referential).
- `docs/EXPORT_FORMAT.md` SetRow gains `isPr` row; new "Acceptable round-trip drift" H2 documents the 3 metadata fields (BodyMetric.id, Supplement.id, exportedAt non-preservation; timing validator-vs-importer asymmetry).
- ISSUES.md i-10 closed.

## Files Created/Modified

- `backend/src/main/java/com/workouthub/exports/dto/FullExportDto.java` - SetRow 8 -> 9 components.
- `backend/src/main/java/com/workouthub/exports/FullExportService.java` - 1 line in `toSessionSection`.
- `backend/src/main/java/com/workouthub/exports/FullImportService.java` - constructor + import + insertSessions signature + insertSessions body + importDump orchestration + replaceSessionsSection orchestration.
- `backend/src/main/java/com/workouthub/exports/ImportValidator.java` - new rule + 1 call line + 1 import.
- `backend/src/main/java/com/workouthub/sessions/SessionSetsService.java` - private -> public on recomputePrForExerciseHistory.
- `backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java` - 2 new @Test methods + seedExercise helper + ExerciseRepository autowire.
- `docs/EXPORT_FORMAT.md` - SetRow row + drift H2.
- `.planning/ISSUES.md` - i-10 Closed entry.

## Decisions Made

- Wire field + recompute safety net (audit's "AND/OR" taken as "AND" for round-trip belt-and-braces).
- Boxed `Boolean` for legacy-payload null tolerance.
- 422 over null-out-on-stale on workoutDayId validation.
- Visibility-only edit on `recomputePrForExerciseHistory`; no signature change, no static, no extra annotation.
- Schema version stays at 1.
- FullExportDto stays camelCase (Direction C from 18-03).
- ISSUES.md i-10 entered as Closed directly (no Open precursor).

## Issues Encountered

None. Plan landed cleanly across 3 atomic commits (Task 1: round-trip wiring; Task 2: validator + 422 test; Task 3: docs + ISSUES.md). Backend mvn unavailable on this Windows host per local-maven-gap memory; test execution delegated to CI.

## Next Phase Readiness

- Phase 18 complete (4/4 plans shipped: 18-01 audit + 18-02 fields + 18-03 naming + 18-04 roundtrip).
- v0.4 milestone progress moves from "Phase 18 in progress (3/4)" to "Phase 18 complete; ready for Phase 19".
- Phase 19 (api-contract-docs) is unblocked - the export/import wire format now has its full final shape and accumulated docs lift cleanly into OpenAPI generation.
- Plan 18-04 leaves no marker open; the deferred items (importedAt asymmetry, BodyMetric/Supplement id stable preservation) are scoped to v1.0 docs per the 18-01 audit.
