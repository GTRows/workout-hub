---
phase: 17-body-metrics
plan: 02
subsystem: metrics
tags: [java, spring-boot, dto, validation, body-metrics, contract-drift]

requires:
  - phase: 17-body-metrics
    plan: 01
    reason: 17-01-AUDIT Section 3 Part A photo_url GAP row + Section 6 photo-url-exposure bucket prescribed the exact 3 file touches

provides:
  - photoUrl on UpsertBodyMetricRequest exposed end-to-end (POST request -> entity setter -> V6 column -> response DTO already wired)
  - round-trip integration test asserting POST + GET carry the URL string

affects:
  - 17-body-metrics/17-03 (time-series-range; independent, no shared lines)
  - 17-body-metrics/17-04 (status-code-split; independent, no shared lines)
  - 18-export-refinement (FullExportService.MetricRow.photoUrl alignment - already in sync, but verify on Phase 18 entry)
  - 28-metrics-ui (frontend can now write photoUrl; upload-pipeline plan still owns multipart/MinIO/signed-URL choice)

tech-stack:
  added: []
  patterns:
    - "Optional trailing record component with @Size mirroring SQL column width (V6 VARCHAR(500) -> @Size(max = 500))"

key-files:
  created: []
  modified:
    - backend/src/main/java/com/workouthub/metrics/dto/UpsertBodyMetricRequest.java
    - backend/src/main/java/com/workouthub/metrics/MetricsService.java
    - backend/src/test/java/com/workouthub/metrics/MetricsIntegrationTest.java

key-decisions:
  - "Append-only DTO change: existing 8 components keep position, new 9th component is String photoUrl"
  - "@Size(max = 500) mirrors V6 VARCHAR(500); no @NotNull, no @URL syntax check (deferred to Phase 28 upload-pipeline plan)"
  - "No new endpoint, no migration: V6 + entity + response DTO already carry photoUrl"
  - "Single round-trip integration test; field-specific size-violation test deferred (existing invalidWeightRangeReturns400 already proves @Valid is wired)"

patterns-established:
  - "Pattern: opening a request DTO field that the response DTO and SQL column already carry is a 3-touch change (DTO append + service setter line + round-trip test)"

issues-created: []

duration: ~2 min
completed: 2026-05-05
---

# Phase 17 Plan 02: Photo-URL Exposure Summary

**Appended optional `photoUrl` to `UpsertBodyMetricRequest` (`@Size(max = 500)`); threaded `req.photoUrl()` into `MetricsService.upsert` between `setThighCm` and `setNotes`; round-trip integration test locks POST + GET behavior. No migration, no entity change, no controller signature change.**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-05-05T14:27:30Z
- **Completed:** 2026-05-05T14:28:51Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- `UpsertBodyMetricRequest` extended to 9 components with optional `photoUrl` (`@Size(max = 500)` mirroring V6 `VARCHAR(500)`).
- `MetricsService.upsert` writes `entity.setPhotoUrl(req.photoUrl())` between `setThighCm` and `setNotes` (V6 column-order alignment).
- `MetricsIntegrationTest.postWithPhotoUrlIsRoundTripped` asserts POST + GET carry the URL string end-to-end; test method count grows 6 -> 7.

## Task Commits

Each task was committed atomically:

1. **Task 1: Append photoUrl to UpsertBodyMetricRequest and thread into MetricsService.upsert** - `48d7a32` (feat)
2. **Task 2: Round-trip integration test for photoUrl** - `fc5eea3` (test)

**Plan metadata:** [filled by metadata commit] (docs: complete plan)

## Files Created/Modified

- `backend/src/main/java/com/workouthub/metrics/dto/UpsertBodyMetricRequest.java` - appended `@Size(max = 500) String photoUrl` as the 9th and final record component; reused existing `Size` import.
- `backend/src/main/java/com/workouthub/metrics/MetricsService.java` - one new setter line `entity.setPhotoUrl(req.photoUrl());` in `upsert` between `setThighCm` and `setNotes`.
- `backend/src/test/java/com/workouthub/metrics/MetricsIntegrationTest.java` - one new `@Test postWithPhotoUrlIsRoundTripped` (~21 lines) appended after `crossUserListIsIsolated`. Self-contained with `nanoTime`-suffixed email (`photo-{nanoTime}@test.local`). Uses `https://example.test/photos/abc.jpg` as RFC-2606-style placeholder URL.

## Decisions Made

- **Append-only DTO change:** existing 8 components keep position, new 9th component is `String photoUrl`. JSON shape is name-keyed, so existing clients unaffected.
- **`@Size(max = 500)` mirrors V6 `VARCHAR(500)`:** no `@NotNull`, no `@URL` syntax check. Storage strategy (multipart/MinIO/signed URLs) deferred to Phase 28; over-validating now would constrain that plan.
- **Setter placement matches column order:** `setPhotoUrl` placed between `setThighCm` and `setNotes` so the service setter block follows V6 column order (`thigh_cm`, `photo_url`, `notes`).
- **Single round-trip test only:** field-specific size-violation 400 test deferred. Existing `invalidWeightRangeReturns400` already proves `@Valid` is wired on the controller; a second size test would inflate scope past the audit's 1-test budget for this bucket.
- **No migration, no entity change, no controller change:** V6 already defines `photo_url VARCHAR(500)`; `BodyMetric` already has the field, getter, and setter; `BodyMetricDto` already carries `photoUrl`; `MetricsService.toDto` already reads `m.getPhotoUrl()`. The only gap was the request DTO + service write path.

## Deviations from Plan

None - plan executed exactly as written. Verification quirk: `rg "photoUrl"` over `backend/src/main/java/com/workouthub/metrics` returns 6 hits (not 5 as the plan's verify line stated), because the plan's count omitted that case-sensitive `photoUrl` does not match `m.getPhotoUrl()` (capital P) in `MetricsService.toDto`. The match is on the new line `entity.setPhotoUrl(req.photoUrl())` where `req.photoUrl()` is the lowercase record accessor. Net plan-prescribed changes (1 DTO append, 1 service setter line, 1 test method) all present and correct.

## Issues Encountered

None. Mechanical change per audit photo-url-exposure bucket. mvn verify gate delegated to CI per local-maven-gap memory.

## Next Phase Readiness

- Plan 17-03 (time-series-range): extend `GET /api/metrics` with optional `from`/`to` `LocalDate` query params; add `findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc` repo method; 2 new integration tests (range hit, empty range). Independent of 17-02 (no shared lines).
- Plan 17-04 (status-code-split): Direction A wrapper (`(BodyMetricDto, boolean wasCreated)`); controller maps 201 on create / 200 on update. Optional absorb of `BodyMetricValidator` static helper if scope budget allows.
- Phase 18 (export-refinement) entry check: `FullExportService.MetricRow.photoUrl` already in sync; verify alignment when phase begins.
- No blockers. Phase 17 is 2/4 plans complete.

---
*Phase: 17-body-metrics*
*Completed: 2026-05-05*
