---
phase: 18-export-refinement
plan: 03
subsystem: exports
tags: [claude-summary, json-naming, snake-case, jackson-annotation, direction-c]

requires:
  - phase: 18-export-refinement
    plan: 01
    reason: 18-01 audit Direction C verdict (Section 4 Part A) prescribes snake_case on claude-summary only
  - phase: 18-export-refinement
    plan: 02
    reason: 18-02 fixed the DTO field set to 10 records; 18-03 annotates them with @JsonNaming

provides:
  - ClaudeSummaryDto and 9 nested records carry @JsonNaming(SnakeCaseStrategy)
  - /api/export/claude-summary wire shape now matches ProjectBrief example literally (snake_case keys)
  - First @JsonNaming usage in backend/src; FullExportDto, ImportResultDto, CsvImportResultDto unaffected

affects:
  - 18-export-refinement/18-04 (roundtrip-stability operates on FullExportDto, untouched here)
  - 19-api-contract-docs (claude-summary section in docs/EXPORT_FORMAT.md will document the snake_case wire shape)

tech-stack:
  added:
    - com.fasterxml.jackson.databind.annotation.JsonNaming (per-record annotation)
    - com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
  patterns:
    - per-record @JsonNaming(SnakeCaseStrategy) on records (does NOT cascade to nested types; each record annotated explicitly)

key-files:
  created: []
  modified:
    - backend/src/main/java/com/workouthub/exports/dto/ClaudeSummaryDto.java
    - backend/src/test/java/com/workouthub/exports/ExportIntegrationTest.java

key-decisions:
  - "Direction C: snake_case on ClaudeSummaryDto ONLY; FullExportDto, ImportResultDto, CsvImportResultDto stay camelCase to preserve backup-restore round-trip on prior payloads."
  - "Per-record @JsonNaming required because Jackson's strategy does not cascade into nested record types. 10 annotations total."
  - "No global JacksonConfig change; localized annotation keeps the rest of the API surface untouched."
  - "Java component names stay camelCase; Jackson translates to snake_case at serialization time. No field rename, no @JsonProperty per-field overrides."
  - "ExportIntegrationTest jsonPath strings flip to snake_case on the response side; request payloads in private helpers (addSet, updateProfile, upsertMetric, createDay) stay camelCase since the receiving endpoints are not annotated."
---

# Phase 18 Plan 03: naming-convention Summary

ClaudeSummaryDto and 9 nested records annotated with @JsonNaming(SnakeCaseStrategy); ExportIntegrationTest jsonPath strings flipped to snake_case on response paths only; first @JsonNaming usage in backend/src; closes 18-01-AUDIT Section 6 naming-convention bucket via Direction C.

## Accomplishments

- 2 Jackson imports + 10 `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` annotations added to `ClaudeSummaryDto.java`.
- 16 jsonPath response-side assertion strings flipped to snake_case across 4 content-emitting tests in `ExportIntegrationTest.java`.
- `FullExportDto`, `ImportResultDto`, `CsvImportResultDto`, `JacksonConfig` untouched.
- Zero record-component, method, helper, seed, or test-count changes.
- First `@JsonNaming` usage in `backend/src`; precedent declared per 18-01-AUDIT Section 5 Part E.

## Files Created/Modified

- `backend/src/main/java/com/workouthub/exports/dto/ClaudeSummaryDto.java` - 2 imports, 10 annotations.
- `backend/src/test/java/com/workouthub/exports/ExportIntegrationTest.java` - 16 jsonPath response strings flipped.

## Decisions Made

- Direction C scope honored: claude-summary only.
- Per-record annotation, not global JacksonConfig.
- `@JsonNaming(SnakeCaseStrategy.class)` form used (not `.SNAKE_CASE` static instance).
- No `@JsonProperty` per-field overrides.
- `$.workouts[0].energy_level` is the new `doesNotExist()` target (replaces 18-02's camelCase form).

## Issues Encountered

None. Mechanical edits as planned; all verification grep checks passed without iteration. Local maven gap means compile/test gates run in CI per project convention (memory: local_maven_gap.md).

## Next Phase Readiness

- Plan 18-04 (`roundtrip-stability`) is unblocked - operates on a different DTO (`FullExportDto`) and a different surface (full-export round-trip + ImportValidator rule).
- Phase 19 (`api-contract-docs`) gains content to document: the snake_case wire shape on claude-summary, the goals delimiter (from 18-02), the adherence-no-clamp note (from 18-02), and the camelCase-on-full-export stance (Direction C non-target).
