---
phase: 13-workouts-audit
plan: 01
deliverable: audit
---

# Workouts Package Audit

## Section 1 - Package Layout

Tree of `backend/src/main/java/com/workouthub/workouts/` grouped by role. Line counts measured 2026-05-04.

### Controllers (3 files, 222 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `WorkoutPlansController.java` | 83 | Plan-level REST surface: list/get/create/update/delete plus `/active` and `/activate`. |
| `WorkoutDaysController.java` | 99 | Day and day-exercise nested REST surface under `/api/workout-plans/{planId}`. |
| `WorkoutDayByIdController.java` | 40 | Standalone day lookup at `/api/workout-days/{id}` for the session-execution path that only knows `workoutDayId`. |

### Services (2 files, 263 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `WorkoutPlansService.java` | 83 | Plan CRUD, ownership check (`findOwnedOrThrow`), and active-plan flip with the partial-unique-index workaround. |
| `WorkoutDaysService.java` | 180 | Day and day-exercise CRUD, conflict checks (`day_of_week` uniqueness, reorder list completeness), and the two-phase reorder around the `(workout_day_id, order_index)` unique constraint. |

### Mapper (1 file, 60 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `WorkoutPlanMapper.java` | 60 | Static entity to DTO mappers (`toDto`, `toSummary`, `toDayDto`, `toItemDto`). |

### Seeder (1 file, 96 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `DefaultPlanSeeder.java` | 96 | Materializes the built-in "Baslangic Plani" template; called by `AdminUsersService` after a successful create. NOT invoked by `TestAuthHelpers.seed`. |

### Domain entities and converters (5 files, 234 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/WorkoutPlan.java` | 88 | JPA entity, `@UuidGenerator` id, `OneToMany` to `WorkoutDay` with cascade-all + orphan-removal, `addDay`/`removeDay` setting back-reference. |
| `domain/WorkoutDay.java` | 103 | JPA entity, ManyToOne to `WorkoutPlan`, `OneToMany` to `WorkoutDayExercise`, `addExercise`/`removeExercise` helpers. |
| `domain/WorkoutDayExercise.java` | 105 | JPA entity, ManyToOne to `WorkoutDay` and `Exercise`, `order_index`, target sets/reps/weight/rest, notes. |
| `domain/WorkoutFocus.java` | 11 | Enum: PUSH, PULL, LEGS, CARDIO, CORE, FULL_BODY, REST. |
| `domain/WorkoutFocusConverter.java` | 12 | Auto-applied JPA converter that lowercases the enum at the column boundary. |

### Repositories (2 files, 38 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/WorkoutPlanRepository.java` | 23 | `findByUserIdOrderByCreatedAtAsc`, `findByUserIdAndActiveTrue`, `findByIdAndUserId`, plus a `findUserIdsWithActiveWorkoutOnDay` aggregate for notifications. |
| `domain/WorkoutDayRepository.java` | 15 | Single `findByIdAndPlanUserId` join through `plan.userId`. |

### DTOs (11 files, 134 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `dto/WorkoutPlanDto.java` | 13 | Full plan response with nested `days`. |
| `dto/WorkoutPlanSummaryDto.java` | 12 | Lightweight list-row response (`dayCount` instead of nested days). |
| `dto/WorkoutDayDto.java` | 12 | Day response with `focus` as lowercase string and nested `exercises`. |
| `dto/WorkoutDayExerciseDto.java` | 17 | Day-exercise row with denormalized `exerciseNameTr`/`exerciseNameEn`. |
| `dto/CreateWorkoutPlanRequest.java` | 7 | `name` (required, max 120). |
| `dto/UpdateWorkoutPlanRequest.java` | 6 | Optional `name` patch. |
| `dto/CreateWorkoutDayRequest.java` | 14 | `dayOfWeek` (1..7), `name`, `focus` (enum), optional `estimatedDurationMin`. |
| `dto/UpdateWorkoutDayRequest.java` | 12 | All fields optional patch. |
| `dto/AddDayExerciseRequest.java` | 18 | `exerciseId`, `targetSets`, optional reps/weight/rest/notes. |
| `dto/UpdateDayExerciseRequest.java` | 15 | All fields optional patch (no `exerciseId` swap). |
| `dto/ReorderDayExercisesRequest.java` | 8 | `itemIdsInOrder` (NotEmpty list of UUIDs). |

Total: 25 files, 1132 lines under `workouts/`.

## Section 2 - Endpoint Catalog

All endpoints require a JWT-authenticated principal (`@AuthenticationPrincipal AppUserPrincipal`). The forward-auth opt-in path passes the same principal type, so auth column reads "JWT user" uniformly. None of the endpoints below are public or admin-only.

### `WorkoutPlansController` (base `/api/workout-plans`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/workout-plans` | `list` (`WorkoutPlansController.java:34`) | - | `List<WorkoutPlanSummaryDto>` | JWT user | 200 | Idempotent. |
| GET | `/api/workout-plans/active` | `getActive` (`WorkoutPlansController.java:39`) | - | `WorkoutPlanDto` or empty | JWT user | 200 / 204 | Idempotent. 204 when no active plan. |
| POST | `/api/workout-plans` | `create` (`WorkoutPlansController.java:47`) | `CreateWorkoutPlanRequest` | `WorkoutPlanDto` | JWT user | 201 | Always creates a plan with `active=false`. |
| GET | `/api/workout-plans/{id}` | `get` (`WorkoutPlansController.java:55`) | - | `WorkoutPlanDto` | JWT user | 200 | Idempotent. 404 via `NotFoundException`. |
| PUT | `/api/workout-plans/{id}` | `update` (`WorkoutPlansController.java:62`) | `UpdateWorkoutPlanRequest` | `WorkoutPlanDto` | JWT user | 200 | Patch-style: only non-null fields applied. Idempotent for same body. |
| DELETE | `/api/workout-plans/{id}` | `delete` (`WorkoutPlansController.java:70`) | - | - | JWT user | 204 | Cascade-removes days and items. |
| POST | `/api/workout-plans/{id}/activate` | `activate` (`WorkoutPlansController.java:78`) | - | `WorkoutPlanDto` | JWT user | 200 | Idempotent: re-activating an already-active plan is a no-op return. |

### `WorkoutDaysController` (base `/api/workout-plans/{planId}`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| POST | `/api/workout-plans/{planId}/days` | `createDay` (`WorkoutDaysController.java:35`) | `CreateWorkoutDayRequest` | `WorkoutDayDto` | JWT user | 201 | 409 on duplicate `dayOfWeek`. |
| PUT | `/api/workout-plans/{planId}/days/{dayId}` | `updateDay` (`WorkoutDaysController.java:44`) | `UpdateWorkoutDayRequest` | `WorkoutDayDto` | JWT user | 200 | Patch-style. 409 if new `dayOfWeek` collides. |
| DELETE | `/api/workout-plans/{planId}/days/{dayId}` | `deleteDay` (`WorkoutDaysController.java:53`) | - | - | JWT user | 204 | Cascade-removes day exercises. |
| POST | `/api/workout-plans/{planId}/days/{dayId}/exercises` | `addItem` (`WorkoutDaysController.java:62`) | `AddDayExerciseRequest` | `WorkoutDayExerciseDto` | JWT user | 201 | Auto-assigns next `orderIndex`. |
| PUT | `/api/workout-plans/{planId}/days/{dayId}/exercises/{itemId}` | `updateItem` (`WorkoutDaysController.java:72`) | `UpdateDayExerciseRequest` | `WorkoutDayExerciseDto` | JWT user | 200 | Patch-style. No `exerciseId` swap. |
| DELETE | `/api/workout-plans/{planId}/days/{dayId}/exercises/{itemId}` | `deleteItem` (`WorkoutDaysController.java:82`) | - | - | JWT user | 204 | Renumbers remaining items after orphan-delete flush. |
| POST | `/api/workout-plans/{planId}/days/{dayId}/exercises/reorder` | `reorder` (`WorkoutDaysController.java:92`) | `ReorderDayExercisesRequest` | `WorkoutDayDto` | JWT user | 200 | 409 if list does not match existing item set exactly. |

### `WorkoutDayByIdController` (base `/api/workout-days`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/workout-days/{id}` | `get` (`WorkoutDayByIdController.java:33`) | - | `WorkoutDayDto` | JWT user | 200 | Idempotent. Owner-scoped via `findByIdAndPlanUserId`. 404 otherwise. |

Total: 15 endpoints across 3 controllers.

## Section 3 - ProjectBrief Phase 3 Gap Matrix

ProjectBrief Phase 3 endpoint list is at `ProjectBrief.md:271-281` (the brief's "Endpoints" subsection of Phase 3) plus the drag-and-drop reorder requirement at `ProjectBrief.md:283`.

| ProjectBrief endpoint | Current implementation | Status | Evidence | Notes |
| --- | --- | --- | --- | --- |
| `GET /api/workout-plans` | `GET /api/workout-plans` | Implemented | `WorkoutPlansController.java:34` | Returns `List<WorkoutPlanSummaryDto>` (lightweight rows). Brief did not specify summary vs full shape; summary is the smaller surface. |
| `POST /api/workout-plans` | `POST /api/workout-plans` | Implemented | `WorkoutPlansController.java:47` | Always creates with `active=false`; user must call `/activate` to flip. Brief does not constrain initial active state. |
| `GET /api/workout-plans/:id` | `GET /api/workout-plans/{id}` | Implemented | `WorkoutPlansController.java:55` | Returns full `WorkoutPlanDto` with nested days and exercises, matching the brief's "plan + gunler + hareketler" requirement. |
| `PUT /api/workout-plans/:id` | `PUT /api/workout-plans/{id}` | Partial | `WorkoutPlansController.java:62` | Patch-only on `name`. Brief's PUT shape is unspecified beyond "update plan", and there is no other plan-level mutable field in the schema, so this is a defensible partial. Flag for confirmation in Phase 14 planning. |
| `DELETE /api/workout-plans/:id` | `DELETE /api/workout-plans/{id}` | Implemented | `WorkoutPlansController.java:70` | Returns 204; cascade-removes days and items via JPA cascade-all + orphan-removal. |
| `POST /api/workout-plans/:id/activate` | `POST /api/workout-plans/{id}/activate` | Implemented | `WorkoutPlansController.java:78` | Idempotent; flips current active off then activates target. Two-write transaction with explicit flush against the partial unique index. |
| `POST /api/workout-plans/:id/days` | `POST /api/workout-plans/{planId}/days` | Implemented | `WorkoutDaysController.java:35` | Path uses `{planId}` instead of `{id}`; semantically identical. 409 on duplicate `dayOfWeek`. |
| `PUT /api/workout-plans/:id/days/:dayId/exercises/:exId` | `PUT /api/workout-plans/{planId}/days/{dayId}/exercises/{itemId}` | Different shape | `WorkoutDaysController.java:72` | Path id named `itemId` (workout_day_exercise.id), not `exId` (the catalog exercise id). Brief's `:exId` reads ambiguously; the implementation interprets it as the join-row id, which is the only addressable resource. The catalog `exercise_id` is set on create and not swappable on update. Drift is naming-only. |
| `DELETE /api/workout-plans/:id/days/:dayId/exercises/:exId` | `DELETE /api/workout-plans/{planId}/days/{dayId}/exercises/{itemId}` | Different shape | `WorkoutDaysController.java:82` | Same naming drift as the PUT row above. Behavior matches: removes the join row, renumbers remaining `orderIndex` values 1..N. |
| Drag-and-drop reorder (gunler ve hareketler) (`ProjectBrief.md:283`) | `POST /api/workout-plans/{planId}/days/{dayId}/exercises/reorder` for items only | Partial | `WorkoutDaysController.java:92` | Item reorder within a day is implemented (two-phase write to dodge the `(workout_day_id, order_index)` unique constraint). Day-of-week reorder across the plan is not exposed; users can only `PUT` an individual day's `dayOfWeek` (with 409 on collision). No bulk day reorder endpoint exists. Note: `WorkoutDay` table has no `order_index` column, only `day_of_week`, so "reorder" of days is implicit through the 1..7 slot. |

### Bonus surface in current implementation, not in ProjectBrief Phase 3

| Path | Method (file:line) | Note |
| --- | --- | --- |
| `GET /api/workout-plans/active` | `WorkoutPlansController.java:39` | Convenience endpoint for the dashboard "today's workout" card. Not in Phase 3 brief but called out as a Phase 5 dashboard requirement. |
| `PUT /api/workout-plans/{planId}/days/{dayId}` | `WorkoutDaysController.java:44` | Day-level patch (name, focus, dayOfWeek, estimatedDurationMin). Brief did not enumerate it but Phase 3 implies "edit a day". |
| `DELETE /api/workout-plans/{planId}/days/{dayId}` | `WorkoutDaysController.java:53` | Day deletion. Same Phase 3 implication argument. |
| `POST /api/workout-plans/{planId}/days/{dayId}/exercises` | `WorkoutDaysController.java:62` | Add-item endpoint. Brief's `POST /api/workout-plans/:id/days` reads as "guen hareket ekle" (add exercise to day) but the implementation splits this into a dedicated nested route, which is the more conventional REST shape. |
| `GET /api/workout-days/{id}` | `WorkoutDayByIdController.java:33` | Standalone day lookup. Brief did not specify it; Phase 4 (sessions) needs it because the session knows only `workoutDayId`, not the parent plan id. |

### Counts

- Implemented: 6 of 9 brief rows.
- Partial: 2 of 9 (`PUT /api/workout-plans/:id`, drag-and-drop reorder).
- Different shape (naming drift only): 2 of 9 (item PUT/DELETE path id `:exId` vs implementation `:itemId`).
- Missing: 0 of 9.
- Out-of-brief but in-implementation: 5 endpoints.
