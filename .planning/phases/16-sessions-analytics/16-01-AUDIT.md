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

## Section 3 - ProjectBrief Phase 4 + ROADMAP Phase 16 Gap Matrix

### Part A - ProjectBrief Phase 4 special endpoints (`ProjectBrief.md:306-308`)

| Brief endpoint | Status | Evidence | Notes |
| --- | --- | --- | --- |
| `GET /api/exercises/:id/last-performance` | Implemented | `ExerciseAnalyticsController.java:30` | Bundle shape (`sessionId, startedAt, endedAt, sets`) matches brief intent. The 200/204 split is a defensible refinement; brief is silent. |
| `GET /api/exercises/:id/progress` | Implemented | `ExerciseAnalyticsController.java:39` | Per-session aggregate. Brief is silent on per-row shape; ROADMAP Phase 16 dictates the deliverables (Part B). |

Both special endpoints are physically present today as Phase 15-01 audit Section 5 "in-package leaks." Phase 16 inherits a working surface and must decide formalization, not introduction.

### Part B - ROADMAP Phase 16 deliverables (`.planning/ROADMAP.md:62-66`)

| Deliverable | Status | Evidence | Notes |
| --- | --- | --- | --- |
| PR computation | Implemented at create-time | `SessionSetsService.java:80-82` invokes `PrDetector.beatsPriorBest`; result threaded into `SessionsMapper.toSetDto(saved, isPr ? Boolean.TRUE : null)` at `SessionSetsService.java:97` | Phase 15-04 documented create-only `newPr` flag. Test `newPrFlagPresentOnCreateButOmittedOnDetailReread` at `SessionSetsIntegrationTest.java:282-294`. NOT durable across detail re-read. Section 5 decides persist vs stay-create-only. |
| Volume aggregation | Implemented | `ExerciseAnalyticsService.java:73-76` Java-side `sum(weightKg * repsDone)` per session | Java-side aggregation over full `findHistoricalByUserAndExercise` result; perf assessed in Section 4 Part B. |
| 1RM (Epley) projections at the query layer | Partial - logic exists, DTO does not expose | `PrDetector.epleyOneRm` at `PrDetector.java:12-18`; consumed by `SessionSetsService.bestPriorOneRm` (`SessionSetsService.java:105-115`); NOT consumed by `ExerciseAnalyticsService.summarize` (`ExerciseAnalyticsService.java:71-93`); `ProgressPointDto` (`ProgressPointDto.java:7-13`) has no `estimatedOneRmKg` field | Cheapest Phase 16 deliverable: one DTO field plus one mapper line plus one test extension. |

### Field-level drift

| Missing field | Type | Owner | Rationale |
| --- | --- | --- | --- |
| `ProgressPointDto.estimatedOneRmKg` | `BigDecimal` | Phase 16 ROADMAP deliverable | Should equal `PrDetector.epleyOneRm(maxWeightKg, topRepsDone)` per session, OR the max Epley over all completed sets in the group (more expensive but more correct). Either policy is acceptable; recommend per-top-set Epley to match `AnalyticsService.oneRepMax` (`AnalyticsService.java:117-123`) which picks the top-Epley set per session. |
| `ProgressPointDto.prSessionId` / `prSetCount` / `hasPr` | `UUID` / `int` / `boolean` | candidate, not required by brief or roadmap | Defer if PR durability stays create-only (Section 5). If `is_pr` lands on `session_sets`, this becomes a derived field at zero extra cost. |
| `LastPerformanceDto.sets[*].newPr` | `Boolean` | always null on this read path | Intentional. `last-performance` is a "current shape" view, not a PR view; do not flip without explicit user-facing motivation. |

## Section 4 - Package-Boundary Verdict and Perf Baseline

### Part A - Package-Boundary Verdict

Read-only grep evidence (`com.workouthub` namespace, `backend/src/main`):

- `grep PrDetector|LastPerformanceDto|ProgressPointDto`: only `sessions/SessionSetsService` (`PrDetector` consumer at lines 82, 110), `sessions/PrDetector` (definition), `sessions/ExerciseAnalyticsController` and `sessions/ExerciseAnalyticsService` (DTO consumers), `sessions/dto/{LastPerformanceDto,ProgressPointDto}` (definitions). NO cross-package consumer outside `sessions/`.
- `grep findHistoricalByUserAndExercise`: three consumers - `sessions/SessionSetsService.java:106`, `sessions/ExerciseAnalyticsService.java:38,58`, AND `analytics/AnalyticsService.java:105`. The shared repo method is already cross-package via `analytics/`.

**Adjacent finding (load-bearing for the verdict):** the `com.workouthub.analytics` package already exists with `AnalyticsController` serving `/api/analytics/{volume,one-rm,streak,prs,heatmap}` (`AnalyticsController.java:24-62`) and `AnalyticsService` (237 lines) re-implementing Epley locally at `AnalyticsService.java:230-236`. This is Phase 31 (charts-stats per ROADMAP) territory landed pre-GSD. Two consequences:

1. The "extract to `analytics/`" namespace is no longer empty - it is occupied by code that already imports `sessions.domain.SessionSet`, `SessionSetRepository`, and `WorkoutSessionRepository`.
2. There is now Epley duplication: `PrDetector.epleyOneRm` (Phase 16 owner) and `AnalyticsService.epley` (Phase 31 origin) compute identical values. This is the kind of drift extraction would surface; staying in place lets it grow.

#### Direction A: extract to `com.workouthub.analytics`

- **Targets:** move `ExerciseAnalyticsController.java`, `ExerciseAnalyticsService.java`, `dto/LastPerformanceDto.java`, `dto/ProgressPointDto.java` to `analytics/`. Move `PrDetector.java` to `analytics/` and update `SessionSetsService.java:82,110` imports (single-line touch).
- **Pros:** matches CONVENTIONS feature-sliced layout; co-locates with the existing `analytics/` package; opens the door to merging `PrDetector.epleyOneRm` with `AnalyticsService.epley` in a follow-up plan; Phase 31 (charts-stats) extension lands naturally; `findHistoricalByUserAndExercise` already crosses the boundary so no NEW cross-package coupling is introduced.
- **Cons:** 5 file moves plus import churn in `SessionSetsService` (1 import) and 3 test classes (`ExerciseAnalyticsIntegrationTest`, `PrDetectorTest`, `PrDetectionIntegrationTest`); risk of merge conflicts with any in-flight Phase 15 follow-up (none active per STATE.md).
- **Spring impact:** none. Component scan root is `com.workouthub`; analytics is already a peer.

#### Direction B: stay-in-place inside `sessions/`

- **Targets:** keep all five files; document the boundary via Javadoc package-info or a Phase 16-01 SUMMARY note.
- **Pros:** zero churn; matches current shipping shape; consistent with Phase 15-04's Javadoc-as-binding-contract pattern.
- **Cons:** violates feature-slicing convention; LEAVES THE EPLEY DUPLICATION between `sessions/PrDetector` and `analytics/AnalyticsService.epley` un-resolved; Phase 31 charts-stats extensions will continue to ignore `sessions/PrDetector` and grow their own helpers; analytics knowledge fragments across two packages.

**Verdict: extract to `com.workouthub.analytics`.** The grep-confirmed isolation (only `SessionSetsService` imports `PrDetector` from outside `sessions/dto/` consumers) plus the pre-existing `analytics/` package plus the Epley-duplication finding make extraction the highest-value cleanup for Phase 16. The churn is one-import-add in `SessionSetsService` and a directory move; the payoff is a single home for analytics where Phase 31 deliverables can converge. Two-step adoption recommended: (1) move files; (2) consolidate Epley into one helper (defer-able to Phase 31 if scope budget tight).

### Part B - Perf Baseline

Walk `ExerciseAnalyticsService.progress` (`ExerciseAnalyticsService.java:55-69`):

1. `sets.findHistoricalByUserAndExercise(userId, exerciseId)` (`SessionSetRepository.java:21-30`): unbounded JPQL fetching ALL completed-session sets for this user+exercise, ordered `startedAt DESC, setNumber ASC`. No LIMIT in the query.
2. Java-side group-by session id via `LinkedHashMap` (`ExerciseAnalyticsService.java:60-63`).
3. `.limit(limit)` AFTER the group-by (`ExerciseAnalyticsService.java:66`): SQL still scans every row regardless of `limit`.
4. `summarize(group)` per group (`ExerciseAnalyticsService.java:71-93`): reads `setsInSession.get(0).getSession()` -> `@ManyToOne(fetch = LAZY)` on `SessionSet.session` (`SessionSet.java:26-28`) triggers a SELECT on `workout_sessions` per unique session id.

**Row-count estimate (power user, single tenant):**

- 200 finished sessions for an exercise * 4 sets = **800 rows fetched** for a `/progress?limit=10` request that only displays the most recent 10 groups.
- Up to **200 lazy SELECTs** on `workout_sessions` (one per unique `session_id` in the group-by) unless Hibernate batches via the second-level/persistence context cache (it does not by default for `@ManyToOne` lazy proxies fetched per-entity).
- Net SQL: 1 + N round trips where N = unique sessions, even though only 10 are rendered.

**Direction A: keep Java-side aggregation (current state).**

- Pros: zero churn; matches the project's broader pattern (`AnalyticsService.weeklyVolume` and `oneRepMax` use the same Java-side group-and-aggregate at `AnalyticsService.java:78-89` and `AnalyticsService.java:107-131`); `WorkoutPlansService` and `MetricsService` (per Phase 15 audit context) follow the same convention. `@Transactional(readOnly = true)` keeps a session open so the lazy SELECTs do not fail.
- Cons: O(rows-since-account-creation) growth per request; lazy `getSession()` fan-out; not a v1.0 shape but acceptable for v0.4 single-user self-hosted.

**Direction B: push down to SQL aggregate.**

- Shape: `SELECT new ProgressRow(s.session.id, s.session.startedAt, COUNT(*), SUM(s.weightKg * s.repsDone), MAX(s.weightKg), MAX(s.repsDone)) FROM SessionSet s WHERE s.session.userId = :userId AND s.exercise.id = :exerciseId AND s.session.endedAt IS NOT NULL GROUP BY s.session.id, s.session.startedAt ORDER BY s.session.startedAt DESC` plus pageable `LIMIT :limit`.
- Pros: single round-trip; row count = `:limit` not `count(history)`; Postgres window functions trivially extend this for `estimatedOneRmKg = MAX(weight * (1 + reps/30.0)) FILTER (WHERE completed)`.
- Cons: a Spring Data projection record (`ProgressRow`) plus DTO mapper extra step; constructor expression in JPQL requires fully-qualified class name; behavior parity tests required (the existing `progressReturnsPerSessionSummariesNewestFirst` test pins behavior).

**ROADMAP wording check:** "1RM (Epley) projections at the query layer" - "query layer" is ambiguous. The project has consistently used Java-side aggregation in the query path (the path of code that produces the query response), and `AnalyticsService.weeklyVolume` is the precedent. Lean on convention.

**Verdict: Java-side aggregation is acceptable for v0.4. Defer SQL pushdown to Phase 31.** Justification:

1. Single-user self-hosted target. A power user with 200 sessions x 4 sets = 800 rows is well within Postgres + Hibernate batch limits even with the lazy fan-out.
2. Convention: every other analytics query in the codebase (`AnalyticsService.weeklyVolume`, `oneRepMax`, `personalRecords`, `heatmap`) uses Java-side aggregation. Inverting only `ExerciseAnalyticsService.progress` would create a one-off pattern.
3. Trigger for revisiting: when Phase 31 (charts-stats) introduces a multi-exercise dashboard view OR row counts exceed a documented threshold (e.g., 5000 rows scanned per request) under realistic seed data. Mark a deferred-issue stub in plan 16-02+ scope ("perf-pushdown bucket").

