---
phase: 16-sessions-analytics
plan: 01
deliverable: audit
---

# Sessions Analytics Audit

## Section 1 - Analytics Scaffold Inventory

Tree of Phase 16-owned files under `backend/src/main/java/com/workouthub/sessions/` plus the shared call sites that Phase 16 logic touches. Line counts measured 2026-05-04.

### Phase 16-owned files (5 files, 191 lines)

| Role | File | Lines | Responsibility |
| --- | --- | ---: | --- |
| Controller | `sessions/ExerciseAnalyticsController.java` | 46 | Per-exercise REST surface under `/api/exercises/{exerciseId}`: `last-performance` (200/204), `progress` (200). |
| Service | `sessions/ExerciseAnalyticsService.java` | 94 | Builds `LastPerformanceDto` from most-recent finished session and `ProgressPointDto` per-session aggregate. `@Transactional(readOnly = true)`. |
| Domain helper | `sessions/PrDetector.java` | 27 | Static `epleyOneRm(weightKg, reps)` and `beatsPriorBest(prior, w, r)` predicate. Pure (no Spring beans). |
| DTO | `sessions/dto/LastPerformanceDto.java` | 11 | Record `(sessionId, startedAt, endedAt, sets)` reusing `SessionSetDto`. |
| DTO | `sessions/dto/ProgressPointDto.java` | 13 | Record `(sessionId, startedAt, setCount, totalVolumeKg, maxWeightKg, topRepsDone)`. |

### Shared call sites (Phase 15 entry-point plus Phase 16 logic)

| Location | Phase split | Notes |
| --- | --- | --- |
| `SessionSetsService.java:80-82` | Phase 15 entry point invokes Phase 16 logic | `bestPriorOneRm(userId, exerciseId)` walks history then `PrDetector.beatsPriorBest(priorBest, weightKg, repsDone)` decides `isPr` for create response. |
| `SessionSetsService.java:105-115` | Phase 16 logic | `bestPriorOneRm` private helper, walks `findHistoricalByUserAndExercise` and Epleys each completed set. Identical Java-side aggregation pattern to `ExerciseAnalyticsService.summarize`. |
| `SessionsMapper.java:47-65` | Phase 15 mapper plus Phase 16 `newPr` overload | `toSetDto(set)` (no flag) used by detail re-read at `SessionsMapper.java:19`; `toSetDto(set, Boolean newPr)` used by create response at `SessionSetsService.java:97`. |
| `sessions/domain/SessionSetRepository.java:21-30` | shared (Phase 15 owns lifecycle queries; Phase 16 owns analytics use) | `findHistoricalByUserAndExercise(userId, exerciseId)` JPQL: returns ALL completed-session sets ordered by `startedAt DESC, setNumber ASC`. No LIMIT, no aggregate. |

### Test surface

| File | Lines | Coverage |
| --- | ---: | --- |
| `sessions/ExerciseAnalyticsIntegrationTest.java` | 215 | 6 tests: `lastPerformanceReturns204WhenNoHistory`, `lastPerformanceReturnsMostRecentFinishedSessionSets`, `progressReturnsPerSessionSummariesNewestFirst`, `progressLimitClampedByLimitParam`, `progressOnAnotherUsersExerciseIsEmptyForCaller`, `unknownExerciseIdReturns404`, `limitCountIsAtLeastOne`. |
| `sessions/PrDetectorTest.java` | 60 | 9 tests: Epley 1-rep identity, 60kg x 10 -> 80, null/zero guards, `beatsPriorBest` truth table. |
| `sessions/PrDetectionIntegrationTest.java` | 134 | 3 tests covering create-response `$.newPr` flag (first-set, beats-prior, does-not-beat). |
| `sessions/SessionSetsIntegrationTest.java:281-294` | (one test) | `newPrFlagPresentOnCreateButOmittedOnDetailReread` (Phase 15-04). Asserts `$.newPr=true` on create, `$.newPr` absent on detail re-read. Phase 16 PR-durability work MUST keep this assertion green or replace it with a stronger durable-read assertion. |

**Phase 16-owned total:** 5 source files / 191 lines plus 3 dedicated test files / 409 lines.

### Adjacent surface discovered during audit

**`com.workouthub.analytics` package already exists** (`backend/src/main/java/com/workouthub/analytics/`). It contains `AnalyticsController.java` (62 lines), `AnalyticsService.java` (237 lines), `StreakCalculator.java`, and 5 DTOs under `analytics/dto/`. It serves `/api/analytics/{volume,one-rm/{exerciseId},streak,prs,heatmap}` and re-implements Epley locally at `AnalyticsService.java:230-236`. It calls `sets.findHistoricalByUserAndExercise` at `AnalyticsService.java:105` (third consumer of the shared Phase 16 query) and uses `WorkoutSessionRepository.findFinishedSince` for week-bucketed aggregation. This package is Phase 31 (charts-stats per ROADMAP) territory that landed pre-GSD; Phase 16's package-boundary decision must account for it (see Section 4 Part A).

## Section 2 - Endpoint and DTO Catalog

All endpoints require a JWT-authenticated principal (`@AuthenticationPrincipal AppUserPrincipal`). Both are idempotent GETs.

### `ExerciseAnalyticsController` endpoints

| HTTP | Path | Method (file:line) | Path/Query params | Request DTO | Response DTO | 2xx | Idempotent | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/exercises/{exerciseId}/last-performance` | `lastPerformance` (`ExerciseAnalyticsController.java:30`) | `@PathVariable UUID exerciseId` | none | `LastPerformanceDto` | 200 / 204 | yes | 204 when `findHistoricalByUserAndExercise` returns empty (`ExerciseAnalyticsService.java:39-41`). 404 if `exerciseId` not in catalog (`ExerciseAnalyticsService.java:36-37`). |
| GET | `/api/exercises/{exerciseId}/progress` | `progress` (`ExerciseAnalyticsController.java:39`) | `@PathVariable UUID exerciseId`; `@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit` (`ExerciseAnalyticsController.java:43`) | none | `List<ProgressPointDto>` | 200 | yes | Empty list when no history. 404 if `exerciseId` not in catalog. `limit=0` -> 400 via `@Validated`. |

### Response DTOs

#### `LastPerformanceDto` (`LastPerformanceDto.java:7-11`)

| Component | Type | Source | Semantics |
| --- | --- | --- | --- |
| `sessionId` | `UUID` | `mostRecent.getId()` (`ExerciseAnalyticsService.java:49`) | Most recent finished session id for this user+exercise. |
| `startedAt` | `Instant` | `mostRecent.getStartedAt()` (`ExerciseAnalyticsService.java:50`) | Session start. |
| `endedAt` | `Instant` | `mostRecent.getEndedAt()` (`ExerciseAnalyticsService.java:51`) | Session finish (always non-null because the JPQL filters `endedAt IS NOT NULL`). |
| `sets` | `List<SessionSetDto>` | `SessionsMapper::toSetDto` (no overload) at `ExerciseAnalyticsService.java:46` | Every set of that session for this exercise, sorted by `setNumber`. `newPr` is always null because the no-flag overload is used. |

`SessionSetDto.newPr=null` on this read path is INTENTIONAL: the bundle is a "current shape" snapshot ("how did the user lift this exercise last time"), not a PR view. ProjectBrief line 307 ("bu hareketi en son ne zaman, kac kg, kac tekrar yapti") matches this; PR badge is not implied.

#### `ProgressPointDto` (`ProgressPointDto.java:7-13`)

| Component | Type | Source | Semantics | ROADMAP Phase 16 deliverable |
| --- | --- | --- | --- | --- |
| `sessionId` | `UUID` | `session.getId()` (`ExerciseAnalyticsService.java:87`) | Session id of this row. | indexing only |
| `startedAt` | `Instant` | `session.getStartedAt()` (`ExerciseAnalyticsService.java:88`) | Session start. | indexing only |
| `setCount` | `int` | `setsInSession.size()` (`ExerciseAnalyticsService.java:89`) | Number of sets logged for this exercise in the session. | volume-aggregation companion |
| `totalVolumeKg` | `BigDecimal` | sum of `weightKg * repsDone` (`ExerciseAnalyticsService.java:73-76`) | Volume aggregation. Coalesces null `weightKg` to ZERO. | **volume aggregation** |
| `maxWeightKg` | `BigDecimal` | max non-null `weightKg` (`ExerciseAnalyticsService.java:77-81`) | Max weight in this session. | adjacent (PR-input) |
| `topRepsDone` | `short` | max `repsDone` (`ExerciseAnalyticsService.java:82-85`) | Max reps in this session. | adjacent (PR-input) |

**Absent fields (flagged):**

- **NO `estimatedOneRmKg`** (BigDecimal, Epley): `PrDetector.epleyOneRm` exists at `PrDetector.java:12-18` and is invoked at `SessionSetsService.java:110` for create-time PR detection but is never materialized into the read-side `/progress` shape. Closes nothing of the ROADMAP "1RM (Epley) projections at the query layer" deliverable.
- **NO `prSetCount`** / **NO `hasPr`** / **NO `prSessionId`** flag at the per-session level. PR durability is create-only today (see Section 5).

