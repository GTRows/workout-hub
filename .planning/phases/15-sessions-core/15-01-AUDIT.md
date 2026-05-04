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
