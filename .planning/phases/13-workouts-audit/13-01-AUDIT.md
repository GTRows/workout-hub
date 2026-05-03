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

## Section 4 - i-1 / i-2 Root-Cause Analysis

### 4a) i-1 trace: `WorkoutDaysIntegrationTest.createDay` helper NPE

Symptom: `objectMapper.readTree(body).get("id")` returns null after a 201 from `POST /api/workout-plans/{planId}/days`. The status check passes (controller succeeded); the response body lacks an `id` field.

Call chain (`file:line` for each hop):

1. Test helper issues `POST /api/workout-plans/{planId}/days` (`WorkoutDaysIntegrationTest.java:194-204`).
2. Controller `WorkoutDaysController.createDay` calls service then wraps in `ResponseEntity.status(201)` (`WorkoutDaysController.java:35-41`).
3. Service `WorkoutDaysService.createDay` builds the `WorkoutDay`, calls `plan.addDay(day)`, then `plans.saveAndFlush(plan)`, then `WorkoutPlanMapper.toDayDto(day)` (`WorkoutDaysService.java:41-54`).
4. Domain `WorkoutPlan.addDay` sets the back-reference `day.setPlan(this)` and adds to the list (`WorkoutPlan.java:62-65`).
5. Mapper reads `day.getId()` and other fields (`WorkoutPlanMapper.java:35-43`).

Evidence on each suspect:

- Back-reference: `WorkoutPlan.addDay` does set `day.plan = this` (`WorkoutPlan.java:63`). Hypothesis "missing back-reference" from ISSUES.md is FALSIFIED by code reading. The same is true for `WorkoutDay.addExercise` (`WorkoutDay.java:71-74`).
- Mapper read order: `plans.saveAndFlush(plan)` runs at `WorkoutDaysService.java:52`, BEFORE `WorkoutPlanMapper.toDayDto(day)` at line 53. With `@UuidGenerator`, Hibernate assigns the id at `persist()` time (which is forced by `saveAndFlush`), so by line 53 `day.getId()` should be non-null. Code is structurally fine.
- DTO field declaration: `WorkoutDayDto` is a record with `UUID id` as its first component (`WorkoutDayDto.java:6-12`). No `@JsonIgnore`, no class-level `@JsonInclude` overriding the global `NON_NULL`.
- Global Jackson config: `JacksonConfig.serializationInclusion(JsonInclude.Include.NON_NULL)` (`JacksonConfig.java:18`). This drops the field from the JSON if and only if `getId()` returns null at serialization time.

Comparison with the passing test: `createDayAddsItToPlan` (`WorkoutDaysIntegrationTest.java:59-73`) hits the same endpoint with `dayOfWeek=1, focus=push` and asserts `$.dayOfWeek` and `$.focus` only. It NEVER reads `$.id`, so it cannot disprove that `id` is missing from the body. It only proves the controller returns 201 and the dayOfWeek/focus survive the round-trip.

Ranked hypotheses (most likely first), each with the minimum 1-3 line falsification step:

1. **Hibernate's `saveAndFlush(plan)` does NOT propagate id assignment to a transient cascade-child added in the same call.** Although cascade-all should `persist()` the new `WorkoutDay` and `@UuidGenerator` should assign its id at that moment, the dirty-checking flush ordering may cascade-merge the child without re-fetching the post-flush state into the in-memory entity. Falsification (in Phase 14, not here): add a single `assertNotNull(day.getId())` line at `WorkoutDaysService.java:52.5` (between `saveAndFlush` and the mapper call). If it passes, hypothesis is falsified; if it fails, hypothesis is confirmed and the fix is to either explicitly `em.refresh(plan)` or call `daysRepo.saveAndFlush(day)` directly instead of going through the parent.
2. **Test reads the response body before `MockMvc` finishes serializing.** Falsification: change the helper to call `r.getResponse().getContentAsString()` exactly once into a local `String body`, log it (`System.err.println(body)`) and re-read with `objectMapper.readTree(body)`. If the printed body shows `id` is present but the parser sees null, the bug is in the helper, not the server. The helper code at `WorkoutDaysIntegrationTest.java:202-203` already does this in one expression, but logging the raw string would prove which side drops the field.
3. **`@UuidGenerator` on a transient entity in a cascade chain produces id only on actual SQL insert, and `saveAndFlush(plan)` skips the cascaded child because the parent itself has no dirty fields.** The plan was just created with `active=false` then immediately fetched again via `findOwnedOrThrow` (a managed entity). Adding `day` to its collection marks the collection dirty, but Hibernate's flush may insert the day row and set its id correctly only if the collection cascade tracks the child as new. This is the same family as hypothesis 1 but framed at the cascade level. Falsification: replace `plans.saveAndFlush(plan)` at `WorkoutDaysService.java:52` with an explicit `daysRepo.saveAndFlush(day)` first, then `plans.flush()`. If the response body now includes `id`, hypothesis is confirmed; the fix is to inject and use `WorkoutDayRepository` for direct persistence.

The three hypotheses are not mutually exclusive; they all point at the same surface (mapper reads `getId()` before Hibernate has finished assigning it through the cascade). The fix is in `WorkoutDaysService` and likely 1-3 lines.

### 4b) i-2 trace: `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport`

Symptom: After exporting a plan and re-importing the dump, `plansInserted` is `0` instead of `>= 1`.

Call chain:

1. Test seeds a user via `helpers.seed(...)` (`FullExportImportIntegrationTest.java:111-113`).
2. Test calls `GET /api/export/full` and captures the JSON (`FullExportImportIntegrationTest.java:117-120`).
3. Test posts the dump back to `POST /api/export/import` and asserts `$.plansInserted >= 1` (`FullExportImportIntegrationTest.java:122-128`).
4. Server `FullExportService.build` walks `plans.findByUserIdOrderByCreatedAtAsc(userId)` (`FullExportService.java:88-92`). Field name in JSON: `plans` (record component at `FullExportDto.java:13`).
5. Server `FullImportService.importDump` reads `dump.plans()` and calls `replacePlans(userId, dump.plans())` (`FullImportService.java:93`).
6. `replacePlans` deletes existing, then iterates rows; if `rows == null || rows.isEmpty()` it returns 0 immediately (`FullImportService.java:217`).

Evidence on each suspect:

- Field-name parity: export emits `"plans": [...]` (record component name `plans` at `FullExportDto.java:13`); importer reads the same `dump.plans()` (`FullImportService.java:93`). Names match. Hypothesis "field-name drift" is FALSIFIED.
- Transaction rollback: `FullImportService` is `@Transactional` at the class level (`FullImportService.java:41`). The counter is incremented inside the same transaction (`FullImportService.java:266`) and returned to the controller before commit; a silent rollback would propagate as an exception (5xx response), not a `200 OK` with `plansInserted=0`. Hypothesis "silent rollback" is FALSIFIED.
- Shared root cause with i-1: i-1 affects the `id` field only. i-2 would require `dump.plans()` to be empty/null at the import boundary. Even if i-1 dropped `id` from each plan in the export, the array itself would still be non-empty and `replacePlans` would loop and increment the count (`FullImportService.java:266`). Hypothesis "i-1 root cause also explains i-2" is FALSIFIED.

Smoking-gun finding for i-2:

- `TestAuthHelpers.seed` (`TestAuthHelpers.java:32-46`) creates the `User` and `UserProfile` rows directly via repository `save()` and skips the admin-create flow entirely.
- `DefaultPlanSeeder.seedFor` (`DefaultPlanSeeder.java:54-91`) is invoked by `AdminUsersService` after a successful create, NOT by `TestAuthHelpers.seed`. The seeder is never called for `helpers.seed(...)` users.
- The test's own comment at `FullExportImportIntegrationTest.java:115-116` claims "the default plan seeder runs on user creation, so the initial export should already contain at least one plan." This claim is wrong: the seeder does not run on `TestAuthHelpers.seed`, only on `AdminUsersService.create`.
- Therefore, the user has zero plans at the moment of export. The export's `plans` array is `[]`. The import's `replacePlans` hits the `rows.isEmpty()` branch at `FullImportService.java:217` and returns 0. The test's expectation `>= 1` fails. The 0 is correct given the missing precondition.

Ranked hypotheses for i-2 (most likely first):

1. **Test setup is wrong: it does not seed a plan before exporting.** Falsification: insert one explicit plan create before the export call (e.g. `mvc.perform(post("/api/workout-plans")...)` with `{"name":"Seed"}`) and re-run. If the import then reports `plansInserted == 1`, this hypothesis is confirmed and the fix is on the test side. Minimum change: 5-7 lines in `FullExportImportIntegrationTest.java:114` adding a plan-create call before line 117.
2. **The default plan seeder should run for `TestAuthHelpers`-seeded users.** Alternative fix: have `TestAuthHelpers.seed` invoke `defaultPlanSeeder.seedFor(user.getId())` after creating the user, matching production semantics. Falsification: same reproduction as above; if the seeder runs, the export contains the seeded plan.
3. **The import should accept an empty plans array as a valid round-trip.** Conceptually weakest: the test asserts `>= 1`, which only makes sense if the precondition seeds at least one plan. Changing the assertion to `>= 0` would silence the test without exercising the round-trip semantics. Falsification: not useful; this is a contract change, not a bug fix.

Verdict: **i-2's root cause is independent of i-1's**. Fixing i-1 will not unblock i-2. The fix for i-2 is a one-paragraph test setup change (hypothesis 1 or 2). i-2 is a coverage / test-correctness bug, not a production code bug.

## Section 5 - Recommended Phase 14 Scope

Each entry: target files, expected change shape, which `@Disabled` test(s) it re-enables, bucket.

### bug-fix bucket (Phase 14)

- **F1: Force id-population for cascade-persisted `WorkoutDay`.** `WorkoutDaysService.java:41-54` (createDay), `WorkoutDaysService.java:78-97` (addItem). Inject `WorkoutDayRepository` (already exists) and a new `WorkoutDayExerciseRepository` (does not exist yet, must be added under `domain/`). Replace `plans.saveAndFlush(plan)` with explicit `daysRepo.saveAndFlush(day)` (or `itemsRepo.saveAndFlush(item)`) so the new entity's `@UuidGenerator` id is assigned and visible before the mapper runs. Expected change: ~5 lines in service, ~10 lines for the new repository file. Bucket: `bug-fix`. Closes i-1. Re-enables: `addItemsAndReorderCloseNoGaps`, `deleteDayCascadesItems`, `deleteItemRenumbersRemaining`, `duplicateDayOfWeekReturns409`, `reorderWithIncompleteListReturns409`, `updateItemPatchesFields` (6 disabled tests in `WorkoutDaysIntegrationTest`).

- **F2: Test-side precondition for the round-trip plan export.** `FullExportImportIntegrationTest.java:111-128`. Insert a plan-create call (POST `/api/workout-plans`) and a day-create call before line 117 so the export has something to round-trip. Alternative: change `TestAuthHelpers.seed` (`TestAuthHelpers.java:32`) to invoke `DefaultPlanSeeder.seedFor` after creating the user. Pick the test-local fix; it is narrower and avoids changing the helper's semantics for tests that intentionally start from an empty user. Expected change: 5-7 lines in the test method. Bucket: `bug-fix` (because the test currently mis-asserts; it is a wrong-test bug). Closes i-2. Re-enables: `importRoundTripPreservesPlansFromExport`.

### coverage bucket (Phase 14)

- **C1: Remove `@Disabled` annotations once F1 lands.** Six methods in `WorkoutDaysIntegrationTest.java` (lines 76, 100, 123, 139, 161, 177). One-line edits each.

- **C2: Remove the i-2 `@Disabled` once F2 lands.** `FullExportImportIntegrationTest.java:109`. One-line edit.

- **C3: Add an `id`-presence assertion to `createDayAddsItToPlan`.** `WorkoutDaysIntegrationTest.java:59-73`. Add `.andExpect(jsonPath("$.id").exists())` so future regressions of the i-1 root cause break the green test, not just the disabled ones.

- **C4: Add a non-empty plans-export assertion to `fullExportReturnsSchemaVersionAndCurrentUserSlices`.** `FullExportImportIntegrationTest.java:36-59`. Currently asserts non-empty `bodyMetrics` and `supplements`; mirror the same for `plans` after seeding one. Same shape as F2.

### feature-gap bucket (deferred to Phase 15+)

These fall out of Section 3 and the Section 2 surface review. Phase 14 should NOT plan these; listing them so Phase 14's boundary is explicit.

- **G1: Day-level reorder endpoint.** `ProjectBrief.md:283` calls for drag-and-drop on days as well as exercises. Currently only items can be reordered; days are reordered implicitly by `PUT`-ing each day's `dayOfWeek`. Adding a bulk day-reorder endpoint (and possibly an `order_index` column on `workout_days`) is feature work. Bucket: `feature-gap`. Owner: Phase 24 (frontend plan editor) or earlier if the API needs to lead.
- **G2: Decide and document `PUT /api/workout-plans/{id}` body shape beyond `name`.** Today only `name` is patchable; `active` is owned by `/activate`. If the brief intended more, document it. Bucket: `feature-gap`. Owner: Phase 19 (api-contract-docs).
- **G3: Reconcile path id naming `:itemId` vs `:exId`.** Brief says `:exId`; implementation uses `:itemId` (correctly meaning the join row). Either rename the path variable to `:exId` (cosmetic) or add an explicit OpenAPI/docs note. Bucket: `feature-gap` (docs-only). Owner: Phase 19.
- **G4: ProjectBrief Phase 3 said "Fatih's mevcut planini default olarak seed et" (`ProjectBrief.md:269`). The implementation has `DefaultPlanSeeder` but it is invoked only by `AdminUsersService`, not by self-registration. v0.3 self-hosted contract section 7 disables public registration anyway, so the seeder runs on the only user-create path (admin). Document this in Phase 19, no code change needed.

