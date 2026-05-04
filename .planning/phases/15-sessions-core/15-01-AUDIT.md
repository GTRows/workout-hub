---
phase: 15-sessions-core
plan: 01
deliverable: audit
---

# Sessions Package Audit

## Section 1 - Package Layout

Tree of `backend/src/main/java/com/workouthub/sessions/` grouped by role. Line counts measured 2026-05-04.

### Controllers (3 files, 172 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `SessionsController.java` | 70 | Session lifecycle REST surface: `start`, `active`, `finish`, `history`, `detail`. |
| `SessionSetsController.java` | 56 | Nested set REST surface under `/api/sessions/{sessionId}/sets`: `add`, `update`, `delete`. |
| `ExerciseAnalyticsController.java` | 46 | Per-exercise analytics under `/api/exercises/{exerciseId}`: `last-performance`, `progress`. (Phase 16 territory; physically in this package today.) |

### Services (3 files, 295 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `SessionsService.java` | 96 | Session lifecycle, ownership checks (`findActiveOwnedOrThrow`), single-active-session enforcement, finish patch. |
| `SessionSetsService.java` | 105 | Set CRUD; auto-numbering, PR detection invocation, 409 mapping on `(session_id, exercise_id, set_number)` collision. |
| `ExerciseAnalyticsService.java` | 94 | Last-performance bundle and `ProgressPointDto` aggregation (volume, max weight, top reps). (Phase 16.) |

### Mapper (1 file, 64 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `SessionsMapper.java` | 64 | Static `toDto` / `toSummary` / `toSetDto` (with optional `newPr` overload). Sorts sets by exercise id then set number. |

### Domain logic helper (1 file, 27 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `PrDetector.java` | 27 | Static Epley 1RM calc and `beatsPriorBest` predicate. (Phase 16 logic invoked from Phase 15 set-add path.) |

### Domain entities (2 files, 220 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/WorkoutSession.java` | 121 | JPA entity, `@UuidGenerator` id, `@OneToMany` cascade-all + orphan-removal to `SessionSet`, holds `heartRateAvgBpm` (V20). |
| `domain/SessionSet.java` | 99 | JPA entity, `@ManyToOne` to `WorkoutSession` and `Exercise`, `set_number`, `reps_done`, `weight_kg`, `rpe`, `completed`. |

### Repositories (2 files, 67 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/WorkoutSessionRepository.java` | 38 | Active-session lookup, paginated history, finished-since query, `detachSessionsFromDays` for plan delete. |
| `domain/SessionSetRepository.java` | 29 | Set lookup by id+session, by-session-and-exercise list, count, historical-by-user-and-exercise (Phase 16 query). |

### DTOs (9 files, 120 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `dto/StartSessionRequest.java` | 5 | Optional `workoutDayId`. |
| `dto/FinishSessionRequest.java` | 10 | Optional `notes`, `mood` (1-5), `energyLevel` (1-5). No `heartRateAvgBpm`. |
| `dto/AddSetRequest.java` | 19 | `exerciseId`, optional `setNumber`, `repsDone`, `weightKg`, `rpe`, `completed`, `notes`. No client idempotency key. |
| `dto/UpdateSetRequest.java` | 15 | All fields optional patch (`repsDone`, `weightKg`, `rpe`, `completed`, `notes`). |
| `dto/SessionDto.java` | 16 | Full session response with nested `sets`. No `heartRateAvgBpm`. |
| `dto/SessionSummaryDto.java` | 14 | Lightweight history row (`setCount`, mood, energyLevel). No `heartRateAvgBpm`. |
| `dto/SessionSetDto.java` | 17 | Set row including denormalized exercise names and optional `newPr`. |
| `dto/LastPerformanceDto.java` | 11 | Most-recent-session bundle for an exercise. (Phase 16.) |
| `dto/ProgressPointDto.java` | 13 | Per-session aggregate (totalVolumeKg, maxWeightKg, topRepsDone). (Phase 16.) |

Total: 21 files, 965 lines under `sessions/`.

## Section 2 - Endpoint Catalog

All endpoints require a JWT-authenticated principal (`@AuthenticationPrincipal AppUserPrincipal`). The forward-auth opt-in path passes the same principal type. None are public or admin-only.

### `SessionsController` (base `/api/sessions`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/sessions/start` | `start` (`SessionsController.java:33`) | `StartSessionRequest` (optional body) | `SessionDto` | JWT user | 201 | 409 if user already has an unfinished session (`SessionsService.java:39-41`). Not idempotent: a retried POST after a successful 201 returns 409 because the active session now exists. |
| GET | `/api/sessions/active` | `active` (`SessionsController.java:41`) | - | `SessionDto` | JWT user | 200 / 204 | Idempotent. 204 when no active session. |
| POST | `/api/sessions/{id}/finish` | `finish` (`SessionsController.java:49`) | `FinishSessionRequest` (optional body) | `SessionDto` | JWT user | 200 | 409 if session already finished (`SessionsService.java:75-77`). Not idempotent on retry: second call returns 409. |
| GET | `/api/sessions/history` | `history` (`SessionsController.java:57`) | - | `Page<SessionSummaryDto>` | JWT user | 200 | Idempotent. `@PageableDefault(size = 20)`. Sort fixed in repo: `findByUserIdOrderByStartedAtDesc`. |
| GET | `/api/sessions/{id}` | `detail` (`SessionsController.java:64`) | - | `SessionDto` | JWT user | 200 | Idempotent. 404 via `NotFoundException`. |

### `SessionSetsController` (base `/api/sessions/{sessionId}/sets`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/sessions/{sessionId}/sets` | `add` (`SessionSetsController.java:30`) | `AddSetRequest` | `SessionSetDto` | JWT user | 201 | Server-side `setNumber` derivation when omitted (`SessionSetsService.java:46-48`). 409 on `(session_id, exercise_id, set_number)` collision via `DataIntegrityViolationException` mapping (`SessionSetsService.java:67-70`). 409 if session is finished (`SessionsService.java:91-93`). NOT retry-safe without explicit `setNumber`. |
| PUT | `/api/sessions/{sessionId}/sets/{setId}` | `update` (`SessionSetsController.java:39`) | `UpdateSetRequest` | `SessionSetDto` | JWT user | 200 | Patch-style. 409 if session finished. Idempotent for same body. `newPr` is NOT recomputed on update (`SessionSetsService.java:96` calls `toSetDto(set)` without the `newPr` overload). |
| DELETE | `/api/sessions/{sessionId}/sets/{setId}` | `delete` (`SessionSetsController.java:48`) | - | - | JWT user | 204 | Bonus surface (not in ProjectBrief). 409 if session finished. Idempotent on retry only if first call wins; second call returns 404. |

### `ExerciseAnalyticsController` (base `/api/exercises/{exerciseId}`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/exercises/{exerciseId}/last-performance` | `lastPerformance` (`ExerciseAnalyticsController.java:30`) | - | `LastPerformanceDto` | JWT user | 200 / 204 | [Phase 16] Idempotent. 204 when no finished history. |
| GET | `/api/exercises/{exerciseId}/progress` | `progress` (`ExerciseAnalyticsController.java:39`) | - | `List<ProgressPointDto>` | JWT user | 200 | [Phase 16] Idempotent. Query param `limit` (default 10, range 1..100). |

Total: 10 endpoints across 3 controllers (8 Phase 15 + 2 Phase 16).

## Section 3 - ProjectBrief Phase 4 Gap Matrix

ProjectBrief Phase 4 endpoint list at `ProjectBrief.md:297-308`. Offline-first strategy at `ProjectBrief.md:310-312`. Phase 4 outputs at `ProjectBrief.md:314-317`.

| ProjectBrief endpoint | Current implementation | Status | Evidence | Notes |
| --- | --- | --- | --- | --- |
| `POST /api/sessions/start` | `POST /api/sessions/start` | Implemented | `SessionsController.java:33` | 409 if active session exists. Optional `workoutDayId` validated against `WorkoutDayRepository.findByIdAndPlanUserId` (`SessionsService.java:45`). |
| `GET /api/sessions/active` | `GET /api/sessions/active` | Implemented | `SessionsController.java:41` | 204 when none active. |
| `POST /api/sessions/:id/sets` | `POST /api/sessions/{sessionId}/sets` | Implemented | `SessionSetsController.java:30` | 409 on duplicate `(session, exercise, set_number)`. NOT retry-safe under offline-queue drain (see Section 4). |
| `PUT /api/sessions/:id/sets/:setId` | `PUT /api/sessions/{sessionId}/sets/{setId}` | Implemented | `SessionSetsController.java:39` | Patch-style; `newPr` not recomputed (Section 3 field-level drift below). |
| `POST /api/sessions/:id/finish` | `POST /api/sessions/{id}/finish` | Implemented | `SessionsController.java:49` | Sets `endedAt = now()`. 409 on retry. |
| `GET /api/sessions/history` | `GET /api/sessions/history` | Implemented | `SessionsController.java:57` | `Pageable` default size 20, sort fixed `startedAt DESC`. |
| `GET /api/sessions/:id` | `GET /api/sessions/{id}` | Implemented | `SessionsController.java:64` | Owner-scoped via `findByIdAndUserId`. |
| `GET /api/exercises/:id/last-performance` | `GET /api/exercises/{exerciseId}/last-performance` | Implemented | `ExerciseAnalyticsController.java:30` | [Phase 16] In-package leak; Phase 15 hardening must not touch. |
| `GET /api/exercises/:id/progress` | `GET /api/exercises/{exerciseId}/progress` | Implemented | `ExerciseAnalyticsController.java:39` | [Phase 16] Same as above. |
| - | `DELETE /api/sessions/{sessionId}/sets/{setId}` | Bonus | `SessionSetsController.java:48` | Not in brief. Useful for the session-execution UI when the user mistypes a set. The offline queue does not need it (queue only emits POSTs); a manual UI delete is the only realistic caller. Keep as Phase 15 surface. |

### Field-level drift

**`heartRateAvgBpm` exposure gap.** Column exists from V20 (`V20__sessions_heart_rate.sql:7`), entity holds it (`WorkoutSession.java:47-48`, getter `WorkoutSession.java:114`), but neither `SessionDto` (`SessionDto.java:7-16`) nor `SessionSummaryDto` (`SessionSummaryDto.java:6-14`) exposes it. The setter is invoked ONLY by `HealthImportService.applyHeartRate` at `HealthImportService.java:68` (Garmin .fit importer). The session-execution flow has NO API path to set or read this field today. `FinishSessionRequest` (`FinishSessionRequest.java:7-10`) accepts `notes`, `mood`, `energyLevel` but not `heartRateAvgBpm`. Net effect: a Garmin-imported value is invisible to clients reading `/api/sessions/{id}`.

**`SessionDto` vs entity:** record components are `id, workoutDayId, startedAt, endedAt, notes, mood, energyLevel, finished, sets` (`SessionDto.java:7-16`). Missing relative to entity getters: `heartRateAvgBpm`, `createdAt`, `updatedAt`. Audit timestamps are intentionally hidden (consistent with `WorkoutPlanDto`); `heartRateAvgBpm` is a true gap.

**`SessionSummaryDto` vs entity:** components are `id, workoutDayId, startedAt, endedAt, finished, setCount, mood, energyLevel` (`SessionSummaryDto.java:6-14`). `heartRateAvgBpm` missing here too.

**`SessionSetDto.newPr` set on create only.** `SessionSetsService.add` calls `toSetDto(saved, isPr ? Boolean.TRUE : null)` (`SessionSetsService.java:66`); `update` calls `toSetDto(set)` without the overload (`SessionSetsService.java:96`); `SessionsMapper.toDto` for detail re-read calls `toSetDto(x)` for every set (`SessionsMapper.java:19`). Result: `newPr` is `null` (omitted by `NON_NULL` Jackson) on every read except the create response. This matches the brief's expectation that PR detection happens at log time, but a client reloading the session detail (e.g. after offline-queue resync) loses the PR badge. State: **contract gap, candidate for plan 15-02+** to either (a) persist `is_pr` on `session_sets` and recompute on update, or (b) document `newPr` as create-response-only and rely on `/exercises/:id/progress` for the durable PR view (Phase 16).

## Section 4 - Offline-First Sync Contract Analysis

ProjectBrief Phase 4 line 310-312 declares the offline-first strategy. Three failure modes evaluated against current backend behavior.

### 4a) Network drop after server commit, before client sees response

Scenario: client `POST /api/sessions/{id}/sets` body `{exerciseId, setNumber=1, ...}`. Server commits row. Response is lost. Client retries the same body.

**Today:** the second insert collides on `UNIQUE (session_id, exercise_id, set_number)` (`V5__sessions_and_sets.sql:45`). Hibernate raises `DataIntegrityViolationException`; `SessionSetsService.add` catches it and throws `ConflictException` (`SessionSetsService.java:67-70`) -> 409.

**IndexedDB drainer treatment:** per `.planning/codebase/ARCHITECTURE.md:53` ("409 from backend is treated as already-synced"), the existing frontend drainer maps 409 -> "stop retrying, mark queued row synced". This is a fragile contract: 409 also fires when the client mistakenly reuses a set_number for a DIFFERENT physical set (genuine operator error), and the drainer cannot distinguish "I already saved this" from "you tried to overwrite a different set". The 409 path also returns no row body, so the client never learns the server-assigned `id`.

**Verdict: PARTIAL.** The 409 path prevents duplicates but conflates two semantically distinct outcomes ("idempotent retry" vs "set_number collision"). A client-supplied `clientSetId` (UUID stored in IndexedDB, accepted as request field, persisted with a UNIQUE index) would be a strictly better contract: retried POST with the same `clientSetId` returns 200 + the persisted row; a different `clientSetId` with the same `set_number` correctly returns 409 as "you have a server-side numbering conflict, refresh and renumber".

### 4b) Concurrent finish + late set POST

Scenario: user finishes the session on tab A. Tab B's offline queue drains a queued set after `endedAt` is set.

**Today:** `SessionSetsService.add` at `SessionSetsService.java:41` calls `sessionsService.findActiveOwnedOrThrow`; that method at `SessionsService.java:88-95` reads `session.isFinished()` and throws `ConflictException` (-> 409) if true.

**Drainer treatment:** the frontend would receive 409 with message "Session is finished and cannot be modified". This is indistinguishable from the 4a 409 ("Set number X already recorded"). The drainer cannot decide whether to drop the queued set (correct here, the session is closed) or treat as already-synced (wrong here, the set was never persisted).

**Verdict: PARTIAL.** 409 is defensible (the resource is in a state that rejects the write), but it masks the late-set-on-finished-session case behind the same status code as the duplicate-set case. RFC 9110 distinguishes 409 (current state conflict, retry plausible after state change) from 410 Gone (resource permanently unavailable). For a finished session, neither is a perfect fit; the cleanest fix is a typed `ApiError.code` field on the 409 body (e.g. `SESSION_FINISHED` vs `SET_NUMBER_DUPLICATE`) so the drainer branches on code, not status alone.

### 4c) Set number gap when offline queue drains out-of-order

Scenario: client logs sets 1, 2, 3 offline. Queue drains 1 first (success, server stores `set_number=1`), then 3 (request body omits `setNumber`), then 2 (also omits `setNumber`).

**Today:** `SessionSetsService.add` at `SessionSetsService.java:46-48` derives `setNumber = countBySessionIdAndExerciseId(...) + 1` when the request omits it. Walk:

- Drain of set "3" with no explicit `setNumber`: count is 1 (set 1 already in DB), server assigns `set_number = 2`. Row is server-set 2 but client-truth set 3.
- Drain of set "2" with no explicit `setNumber`: count is 2, server assigns `set_number = 3`. Row is server-set 3 but client-truth set 2.

The data lands but the displayed order on `GET /api/sessions/{id}` (sorted by `setNumber` in `SessionsMapper.toDto`) is wrong: the user sees set 1 then the heavier set 3 then the lighter set 2, which corrupts both the PR computation (recomputed best of all) and the user's mental model of order.

If the client DOES send explicit `setNumber`, drain of set 3 works (server stores set_number=3), but drain of set 2 collides on UNIQUE because... actually it does NOT collide; (session, exercise, 2) is unused, so the insert succeeds. The 409 path only fires when the same `setNumber` is reused, not when a gap is filled. Out-of-order drain with explicit numbers is therefore safe.

**Verdict: FAIL** (without explicit `setNumber` from the client). The auto-numbering at `SessionSetsService.java:46-48` is unsafe for offline-queue drain because it is order-sensitive. The corrective contract is: the client MUST pass an explicit `setNumber` for any set drained from IndexedDB. Server can keep the auto-numbering as a fallback for the live-online ad-hoc-add path, but documentation must call out that the offline path requires explicit numbering. Alternatively, accept a `clientSetId` and renumber server-side in a deterministic order based on a client-supplied `recordedAt` timestamp.

### Top three sync-contract gaps (ranked)

1. **No client-supplied idempotency key on `POST /sets`.** Adds a `clientSetId UUID` to `AddSetRequest` and a `client_set_id UUID` column on `session_sets` with a UNIQUE index per `(session_id, client_set_id)`. On retry of the same key, return 200 with the persisted row. Closes 4a's PARTIAL verdict; eliminates the 409-conflation bug. Touches: `AddSetRequest.java`, `SessionSet.java`, new `V26__session_sets_client_id.sql`, `SessionSetsService.add`. Test coverage gap: `SessionSetsIntegrationTest` (new test asserting same-key retry returns 200 + same id).
2. **Auto-numbering is unsafe for offline drain.** Either require explicit `setNumber` in the OpenAPI/docs and remove the `?:` fallback at `SessionSetsService.java:46-48`, or document the live-online vs offline split and add an integration test that drains 1, 3, 2 with explicit numbers and asserts row order on detail re-read. Closes 4c FAIL. Touches: `SessionSetsService.java` and a new test `OfflineDrainOrderingIntegrationTest`.
3. **409 body lacks a typed error code.** Frontend cannot distinguish "set already saved" from "session finished" from "set number collision". Add an `ApiError.code` enum or extend the existing error body with a `code` string. Closes 4b PARTIAL verdict. Touches: `ApiError.java`, `GlobalExceptionHandler` (or new `ConflictException` subtypes), `SessionSetsService` throw sites. Test coverage gap: assert `$.code` value in 409 paths.

**Verdict for v0.4: must-harden in plan 15-02+.** The current 409-only path "works" for a happy-path online flow but fails the offline-first contract that ProjectBrief Phase 4 explicitly mandates. Hardening is a 1-2 plan effort (one for the idempotency key + DB column, one optionally for typed error codes if scope budget allows).
