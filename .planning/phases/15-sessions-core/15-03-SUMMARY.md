---
phase: 15-sessions-core
plan: 03
subsystem: sessions
tags: [dto, mapper, integration-test, heart-rate, contract-completeness]

requires:
  - phase: 15-sessions-core
    plan: 01
    reason: audit Section 3 field-level drift and Section 6 FG1 identified the exposure gap closed by this plan

provides:
  - heartRateAvgBpm read-only exposure on SessionDto (detail/active/start/finish responses)
  - heartRateAvgBpm read-only exposure on SessionSummaryDto (history response rows)
  - integration test pattern for write-via-repository mimicking the Garmin importer

affects:
  - 19-api-contract-docs (OpenAPI surface for the new field; document import-fed semantics)

tech-stack:
  added: []
  patterns:
    - direct repository mutation in integration tests as a stand-in for upstream-package writers (here Garmin .fit importer)

key-files:
  created:
    - .planning/phases/15-sessions-core/15-03-SUMMARY.md
  modified:
    - backend/src/main/java/com/workouthub/sessions/dto/SessionDto.java
    - backend/src/main/java/com/workouthub/sessions/dto/SessionSummaryDto.java
    - backend/src/main/java/com/workouthub/sessions/SessionsMapper.java
    - backend/src/test/java/com/workouthub/sessions/SessionLifecycleIntegrationTest.java
    - backend/src/test/java/com/workouthub/sessions/SessionHistoryIntegrationTest.java

key-decisions:
  - "Read-only exposure: no FinishSessionRequest field, no PATCH endpoint. Garmin .fit importer at HealthImportService.java:68 remains the sole writer; Phase 17 owns the data source."
  - "Field placement: appended as last record component to preserve positional ordering for mapper call sites."
  - "Test write path: direct WorkoutSessionRepository.saveAndFlush after setHeartRateAvgBpm mimics the importer without booting health/ package internals (out of Phase 15 scope)."
  - "Jackson NON_NULL omission: legacy sessions with null heartRateAvgBpm have the field absent from the JSON response; the test asserts both omitted and present cases."

duration: 3 min
completed: 2026-05-04
---

# Phase 15 Plan 03: heart-rate exposure on session DTOs Summary

Read-only heartRateAvgBpm exposure on SessionDto and SessionSummaryDto with repository-mutation integration tests for Garmin .fit importer parity.

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-04
- **Completed:** 2026-05-04
- **Tasks:** 2
- **Files modified:** 5 (2 DTOs, 1 mapper, 2 integration tests)

## Accomplishments

- `SessionDto` gains `Short heartRateAvgBpm` as last record component; `SessionSummaryDto` gains the same as last record component.
- `SessionsMapper.toDto` and `SessionsMapper.toSummary` pass `s.getHeartRateAvgBpm()` as the new last constructor argument.
- `SessionLifecycleIntegrationTest` injects `WorkoutSessionRepository` and adds `heartRateAvgBpmIsNullByDefaultAndPopulatedAfterImporterWrite` covering detail and active endpoints (null-omitted via NON_NULL, value-present after repository mutation).
- `SessionHistoryIntegrationTest` injects `WorkoutSessionRepository` and adds `historyRowsExposeHeartRateAvgBpm` covering history page row exposure with and without value.
- Audit Section 3 field-level drift "heartRateAvgBpm exposure gap" closed.
- Audit Section 6 FG1 (heart-rate exposure on `SessionDto` + `SessionSummaryDto`) shipped.
- Audit Section 6 C3 coverage gap (heart-rate field assertion) closed via the two new integration tests.

## Task Commits

1. **Task 1: Add heartRateAvgBpm to SessionDto, SessionSummaryDto, and SessionsMapper** - `8865b27`
2. **Task 2: Integration tests asserting heart-rate exposure on detail and history endpoints** - `fb77aa8`

## Files Created/Modified

- `backend/src/main/java/com/workouthub/sessions/dto/SessionDto.java` - added `Short heartRateAvgBpm` as last record component
- `backend/src/main/java/com/workouthub/sessions/dto/SessionSummaryDto.java` - added `Short heartRateAvgBpm` as last record component
- `backend/src/main/java/com/workouthub/sessions/SessionsMapper.java` - both `toDto` and `toSummary` populate the new field via `s.getHeartRateAvgBpm()`
- `backend/src/test/java/com/workouthub/sessions/SessionLifecycleIntegrationTest.java` - injected repository, new test asserting null-omitted then 142-present on detail and active endpoints
- `backend/src/test/java/com/workouthub/sessions/SessionHistoryIntegrationTest.java` - injected repository, new test asserting 138-present on most-recent history row then doesNotExist after a no-heart-rate session
- `.planning/phases/15-sessions-core/15-03-SUMMARY.md` - this deliverable

## Decisions Made

- **Read-only exposure: no FinishSessionRequest field, no PATCH endpoint.** Garmin .fit importer at `HealthImportService.java:68` remains the sole writer; Phase 17 owns the data source.
- **Field placement: appended as last record component** to preserve positional ordering for mapper call sites.
- **Test write path: direct `WorkoutSessionRepository.saveAndFlush`** after `setHeartRateAvgBpm` mimics the importer without booting `health/` package internals (out of Phase 15 scope).
- **Jackson NON_NULL omission:** legacy sessions with null heartRateAvgBpm have the field absent from the JSON response; the test asserts both omitted and present cases.

## Deviations from Plan

None. Plan executed exactly as written. Both tasks landed in one atomic commit each with exactly the expected file counts (3 / 2).

## Issues Encountered

None. Local Maven is unavailable on the Windows host; CI verification is the gate for test execution. The grep checks in each task's `<verify>` block confirmed structural correctness.

## Next Phase Readiness

- Plan 15-04 (auto-numbering verdict + typed 409 codes) is the last remaining Phase 15 hardening plan.
- Phase 19 (api-contract-docs) inherits the OpenAPI doc note for heartRateAvgBpm import-fed semantics.

---
*Phase: 15-sessions-core*
*Completed: 2026-05-04*
