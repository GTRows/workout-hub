---
phase: 17-body-metrics
plan: 01
deliverable: audit
---

# Body Metrics Package Audit

## Section 1 - Metrics Package Inventory

Tree of `backend/src/main/java/com/workouthub/metrics/`-owned files plus the V6 SQL definition. Line counts measured 2026-05-05.

### Controller (1 file, 50 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `MetricsController.java` | 50 | REST surface under `/api/metrics`: `list` (GET), `upsert` (POST), `delete` (DELETE /{id}). Constructor-injected `MetricsService`; principal via `@AuthenticationPrincipal AppUserPrincipal`. |

### Service (1 file, 70 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `MetricsService.java` | 70 | Owner-scoped `list`, upsert-by-`(userId, recordedDate)`, delete-by-`(id, userId)`. `@Transactional` at class level; `@Transactional(readOnly = true)` on `list`. Hand-written private static `toDto(BodyMetric)` mapper at lines 55-69 (no separate `MetricsMapper` class). |

### Domain entity (1 file, 104 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/BodyMetric.java` | 104 | JPA entity, `@UuidGenerator` id, `@PrePersist` / `@PreUpdate` audit timestamps, primitive getters/setters for the 9 user-facing columns plus audit timestamps. No parent-child cascade relationships (Phase 14 saveAndFlush lesson does not apply). |

### Repository (1 file, 27 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `domain/BodyMetricRepository.java` | 27 | Spring Data JPA: `findByUserIdOrderByRecordedDateDesc`, `findByUserIdAndRecordedDate`, `findByIdAndUserId`, plus a custom JPQL `findUserIdsWithoutMetricsSince(@Param("since") LocalDate)` (lines 19-26) for `ReminderJob.runWeightNudge`. |

### DTOs (2 files, 38 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `dto/BodyMetricDto.java` | 20 | Response record (12 components: `id, recordedDate, weightKg, bodyFatPercent, waistCm, chestCm, armCm, thighCm, photoUrl, notes, createdAt, updatedAt`). |
| `dto/UpsertBodyMetricRequest.java` | 18 | Request record (8 components, validated via `jakarta.validation`). NO `photoUrl` component. |

### Tests (1 file, 130 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `MetricsIntegrationTest.java` | 130 | 6 `@Test` methods covering 401, POST + GET round-trip, upsert-by-date, weight validation, DELETE, cross-user isolation. Uses `AbstractIntegrationTest` (real Postgres via Testcontainers) and `TestAuthHelpers.seed`. |

**`metrics/`-owned source total:** 6 source files / 309 lines plus 1 test class / 130 lines.

### V6 SQL `body_metrics` table definition

`V6__body_metrics_and_supplements.sql` lines 6-24 define the table. Two indexes at lines 26-28.

| Column | SQL type | Nullability | Constraint / default |
| --- | --- | --- | --- |
| `id` | `UUID` | NOT NULL | PRIMARY KEY (`V6:7`) |
| `user_id` | `UUID` | NOT NULL | FK `users(id) ON DELETE CASCADE` (`V6:8-9`) |
| `recorded_date` | `DATE` | NOT NULL | (`V6:10`) |
| `weight_kg` | `NUMERIC(5,2)` | nullable | (`V6:11`) |
| `body_fat_percent` | `NUMERIC(4,1)` | nullable | CHECK `body_fat_percent IS NULL OR body_fat_percent BETWEEN 0 AND 100` (`V6:22-23`) |
| `waist_cm` | `NUMERIC(5,1)` | nullable | (`V6:13`) |
| `chest_cm` | `NUMERIC(5,1)` | nullable | (`V6:14`) |
| `arm_cm` | `NUMERIC(5,1)` | nullable | (`V6:15`) |
| `thigh_cm` | `NUMERIC(5,1)` | nullable | (`V6:16`) |
| `photo_url` | `VARCHAR(500)` | nullable | (`V6:17`) |
| `notes` | `TEXT` | nullable | (`V6:18`) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | DEFAULT `NOW()` (`V6:19`) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | DEFAULT `NOW()` (`V6:20`) |
| - | - | - | UNIQUE `body_metrics_user_date_unique (user_id, recorded_date)` (`V6:21`) |

Indexes (`V6:26-28`):
- `idx_body_metrics_recorded_date ON body_metrics (recorded_date)` (`V6:26`)
- `idx_body_metrics_user_date ON body_metrics (user_id, recorded_date DESC)` (`V6:27-28`)

## Section 2 - Endpoint and DTO Catalog

All endpoints require a JWT-authenticated principal (`@AuthenticationPrincipal AppUserPrincipal`). No admin-only or public surfaces.

### `MetricsController` endpoints (base `/api/metrics`)

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | Auth | 2xx | Idempotency | Query params |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/metrics` | `list` (`MetricsController.java:30-33`) | none | `List<BodyMetricDto>` | JWT user | 200 | yes (read-only) | NONE - returns full history; no date-window filter |
| POST | `/api/metrics` | `upsert` (`MetricsController.java:35-41`) | `UpsertBodyMetricRequest` | `BodyMetricDto` | JWT user | 201 (always) | yes BY DATE - same `(userId, recordedDate)` is updated in place via `MetricsService.upsert` `findByUserIdAndRecordedDate(...).orElseGet(...)` (`MetricsService.java:31-38`) and SQL UNIQUE `body_metrics_user_date_unique` (`V6:21`). Status code is always 201 even on UPDATE branch - flagged in Section 4 Part A. |
| DELETE | `/api/metrics/{id}` | `delete` (`MetricsController.java:43-49`) | none | (empty) | JWT user | 204 | retry-only on first-call-wins; second call returns 404 via `NotFoundException` from `findByIdAndUserId` (`MetricsService.java:49-52`) |

### `BodyMetricDto` response components (12 total)

`BodyMetricDto.java:8-20` — record components in declared order.

| # | Component | Type | Source on read | Line |
| ---: | --- | --- | --- | ---: |
| 1 | `id` | `UUID` | `m.getId()` | `BodyMetricDto.java:9` |
| 2 | `recordedDate` | `LocalDate` | `m.getRecordedDate()` | `:10` |
| 3 | `weightKg` | `BigDecimal` | `m.getWeightKg()` | `:11` |
| 4 | `bodyFatPercent` | `BigDecimal` | `m.getBodyFatPercent()` | `:12` |
| 5 | `waistCm` | `BigDecimal` | `m.getWaistCm()` | `:13` |
| 6 | `chestCm` | `BigDecimal` | `m.getChestCm()` | `:14` |
| 7 | `armCm` | `BigDecimal` | `m.getArmCm()` | `:15` |
| 8 | `thighCm` | `BigDecimal` | `m.getThighCm()` | `:16` |
| 9 | `photoUrl` | `String` | `m.getPhotoUrl()` | `:17` |
| 10 | `notes` | `String` | `m.getNotes()` | `:18` |
| 11 | `createdAt` | `Instant` | `m.getCreatedAt()` | `:19` |
| 12 | `updatedAt` | `Instant` | `m.getUpdatedAt()` | `:20` |

### `UpsertBodyMetricRequest` request components (8 total)

`UpsertBodyMetricRequest.java:10-18` — record components in declared order with validation annotations.

| # | Component | Type | Validation | Range | Line |
| ---: | --- | --- | --- | --- | ---: |
| 1 | `recordedDate` | `LocalDate` | `@NotNull` | required | `UpsertBodyMetricRequest.java:11` |
| 2 | `weightKg` | `BigDecimal` | `@DecimalMin("20.0") @DecimalMax("500.0")` | 20.0-500.0 kg | `:12` |
| 3 | `bodyFatPercent` | `BigDecimal` | `@DecimalMin("0.0") @DecimalMax("100.0")` | 0.0-100.0 % | `:13` |
| 4 | `waistCm` | `BigDecimal` | `@DecimalMin("20.0") @DecimalMax("300.0")` | 20.0-300.0 cm | `:14` |
| 5 | `chestCm` | `BigDecimal` | `@DecimalMin("20.0") @DecimalMax("300.0")` | 20.0-300.0 cm | `:15` |
| 6 | `armCm` | `BigDecimal` | `@DecimalMin("10.0") @DecimalMax("100.0")` | 10.0-100.0 cm | `:16` |
| 7 | `thighCm` | `BigDecimal` | `@DecimalMin("20.0") @DecimalMax("200.0")` | 20.0-200.0 cm | `:17` |
| 8 | `notes` | `String` | `@Size(max = 2000)` | 0-2000 chars | `:18` |

### Asymmetry between request and response DTOs

`BodyMetricDto` carries 12 components vs `UpsertBodyMetricRequest`'s 8. Four components missing from the request:

- `id` (server-assigned via `@UuidGenerator`) - expected.
- `createdAt`, `updatedAt` (server-assigned via `@PrePersist` / `@PreUpdate` on `BodyMetric.java:59-69`) - expected.
- **`photoUrl` (`BodyMetricDto.java:17`) - NOT expected.** Reachable on read but UNREACHABLE on write via this endpoint. The only path that writes `BodyMetric.photoUrl` today is `FullImportService.replaceMetrics` at `FullImportService.java:172`. The user-facing `POST /api/metrics` cannot set it. Section 3 Part A flags this as the lone GAP; Section 6 Plan 17-02 closes it.

### Test methods in `MetricsIntegrationTest`

6 `@Test` methods in `MetricsIntegrationTest.java`:

| # | Method | Line | Outcome |
| ---: | --- | ---: | --- |
| 1 | `unauthenticatedCallReturns401` | `:32` | Bare `GET /api/metrics` without `Authorization` header returns 401. |
| 2 | `postStoresAndGetReturnsTheEntry` | `:37` | POST a metric, then GET; assert length 1 plus `recordedDate`, `notes` round-trip. |
| 3 | `repeatedPostForSameDateUpdatesInsteadOfInserting` | `:59` | Two POSTs with same `recordedDate` produce ONE row; second response asserts updated `weightKg`. Currently asserts `isCreated()` (`:74`) on the UPDATE-branch second POST - flagged in Section 4 Part A and locked by Section 6 Plan 17-04. |
| 4 | `invalidWeightRangeReturns400` | `:82` | `weightKg=5.0` violates `@DecimalMin("20.0")`; expects 400. |
| 5 | `deleteRemovesTheEntry` | `:94` | POST then DELETE the returned id; expects 204. |
| 6 | `crossUserListIsIsolated` | `:113` | User A's POST is invisible to user B's GET. |

## Section 3 - ProjectBrief + ROADMAP Gap Matrix

### Part A - Schema column drift

`ProjectBrief.md:177-186` lists 9 user-facing columns for `body_metrics`. Column-by-column matrix across 5 sources: ProjectBrief schema -> V6 SQL -> entity -> response DTO -> upsert request.

| Column | ProjectBrief (line) | V6 SQL (line) | Entity (line) | BodyMetricDto (line) | UpsertBodyMetricRequest (line) | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `recorded_date` | listed `:180` | `DATE NOT NULL` `V6:10` | `recordedDate` `BodyMetric.java:27` | `recordedDate` `:10` | `recordedDate` `:11` (`@NotNull`) | OK |
| `weight_kg` | listed `:181` | `NUMERIC(5,2)` `V6:11` | `weightKg` `BodyMetric.java:30` | `weightKg` `:11` | `weightKg` `:12` (20.0-500.0) | OK |
| `body_fat_percent` | listed `:182` | `NUMERIC(4,1)` + CHECK `V6:12,22-23` | `bodyFatPercent` `BodyMetric.java:33` | `bodyFatPercent` `:12` | `bodyFatPercent` `:13` (0.0-100.0) | OK |
| `waist_cm` | listed `:183` | `NUMERIC(5,1)` `V6:13` | `waistCm` `BodyMetric.java:36` | `waistCm` `:13` | `waistCm` `:14` (20.0-300.0) | OK |
| `chest_cm` | listed `:184` | `NUMERIC(5,1)` `V6:14` | `chestCm` `BodyMetric.java:39` | `chestCm` `:14` | `chestCm` `:15` (20.0-300.0) | OK |
| `arm_cm` | listed `:184` | `NUMERIC(5,1)` `V6:15` | `armCm` `BodyMetric.java:42` | `armCm` `:15` | `armCm` `:16` (10.0-100.0) | OK |
| `thigh_cm` | listed `:184` | `NUMERIC(5,1)` `V6:16` | `thighCm` `BodyMetric.java:45` | `thighCm` `:16` | `thighCm` `:17` (20.0-200.0) | OK |
| `photo_url` | listed `:185` (opsiyonel) | `VARCHAR(500)` `V6:17` | `photoUrl` `BodyMetric.java:48` | `photoUrl` `:17` | **MISSING** | **GAP** |
| `notes` | listed `:186` | `TEXT` `V6:18` | `notes` `BodyMetric.java:51` | `notes` `:18` | `notes` `:18` (`@Size(max=2000)`) | OK |

`photo_url` is the only GAP across the five sources. Schema, entity, and response DTO carry it; the upsert request does not. This blocks the user-facing `POST /api/metrics` from setting the field that ROADMAP Phase 17 explicitly lists ("optional progress photo URL"). Closed by Section 6 Plan 17-02.

### Part B - ProjectBrief Phase 5 page #7 deliverables

`ProjectBrief.md:360-363`:

| Deliverable | Status | Evidence |
| --- | --- | --- |
| "Kilo grafigi (haftalik/aylik/tum zamanlar)" | Partial | `GET /api/metrics` (`MetricsController.java:30-33`) returns the full history with NO `from`/`to`/`window` query parameter. The frontend can client-side filter, but the chart-window phrasing implies a server-side range read. ROADMAP `:78` says "time-series read endpoints" explicitly. Phase 17 must add a date-range read; Section 5 Part B settles the shape. |
| "Olcum ekleme formu" | Partial | `POST /api/metrics` accepts the form; validation ranges present (Section 2 DTO sub-section). Missing `photoUrl` per Part A. |
| "Progress fotografi yukleme (opsiyonel)" | Gap on the API surface | `BodyMetric.photoUrl` (`BodyMetric.java:48`) and `body_metrics.photo_url` (`V6:17`) exist; `BodyMetricDto.photoUrl` (`:17`) reads it; only `FullImportService.replaceMetrics` at `FullImportService.java:172` writes it today. The user-facing `POST /api/metrics` cannot set it. Phase 17 minimum is to expose `photoUrl` as an OPTIONAL string on `UpsertBodyMetricRequest` so the import path's value can also reach the user-facing path. Storage strategy (multipart vs URL string vs MinIO) is a Phase 5 frontend concern (deferred). |

### Part C - ROADMAP Phase 17 deliverables

`ROADMAP.md:76-83`:

| Deliverable | Status | Evidence |
| --- | --- | --- |
| "weight" | Implemented | `weightKg` in entity, request, response. Validation `@DecimalMin("20.0") @DecimalMax("500.0")` (`UpsertBodyMetricRequest.java:12`). Test `invalidWeightRangeReturns400` (`MetricsIntegrationTest.java:82`) covers the 400 path. |
| "body measurements" | Implemented | 4 measurement fields (`waistCm`, `chestCm`, `armCm`, `thighCm`) in entity, request, response with bounded ranges (`UpsertBodyMetricRequest.java:14-17`). |
| "optional progress photo URL" | Schema/entity/response yes; request DTO no | Cross-link to Part A `photo_url` row. |
| "time-series read endpoints for the metrics UI" | Missing | Only `GET /api/metrics` exists, returns full history with no date-range filter and no per-field projection. Cross-link to Part B "Kilo grafigi" row. Section 5 Part B picks the shape; Section 6 Plan 17-03 lands it. |

## Section 4 - Behavioral Analysis

### Part A - POST status-code semantics

Walk `MetricsController.upsert` (`MetricsController.java:35-41`) and `MetricsService.upsert` (`MetricsService.java:30-47`):

1. Service queries `repo.findByUserIdAndRecordedDate(userId, req.recordedDate())` (`MetricsService.java:31-32`).
2. **Hit** (existing row for `(userId, recordedDate)`): the existing entity is mutated and saved (UPDATE) via `repo.save(entity)` at `:46`. `@PreUpdate` fires, refreshing `updatedAt` (`BodyMetric.java:66-69`).
3. **Miss** (no existing row): a new entity is created via `orElseGet(() -> { ... })` at `:33-38`, mutated, saved (INSERT). `@PrePersist` fires, populating `createdAt` and `updatedAt` (`BodyMetric.java:59-64`).
4. Controller returns `ResponseEntity.status(HttpStatus.CREATED)` UNCONDITIONALLY (`MetricsController.java:39`).

**Drift verdict:** 201 is wrong for the UPDATE branch. Per HTTP semantics 200 OK fits UPDATE; 201 Created fits INSERT. Phase 15-02's `clientSetId` precedent (200 = replay/update, 201 = newly created) applied here closes the same shape.

The existing test `repeatedPostForSameDateUpdatesInsteadOfInserting` at `MetricsIntegrationTest.java:74` asserts `isCreated()` on the UPDATE-branch second POST, which encodes the drift; Plan 17-04 must flip that assertion.

#### Direction A: split the status, keep the upsert-by-date contract

- `MetricsService.upsert` returns a wrapper `(BodyMetricDto dto, boolean wasCreated)` (record or pair).
- Controller maps `wasCreated ? 201 : 200`.
- Touches: 1 service signature change, 1 controller method, 1 existing test assertion flip (`:74` becomes `isOk()`), 1 new test (create-then-update status pair on the same date).
- Pros: smallest change; matches Phase 15-02's idempotency pattern; respects V6's "one row per `(user, date)`" design.
- Cons: introduces a new tuple/wrapper shape that no other controller currently uses.

#### Direction B: split into two endpoints

- `POST /api/metrics` always creates and returns 409 on date collision (UNIQUE `body_metrics_user_date_unique`).
- `PUT /api/metrics/{id}` updates by id.
- Touches: 1 controller method split into 2; service split into create + update; date-collision uniqueness moves from upsert-by-find to a 409 mapping path; 6 tests rewritten plus new `PUT /{id}` tests.
- Pros: more REST-pure; per-id semantics for updates.
- Cons: inconsistent with V6's UNIQUE-by-`(user, date)` design (the SQL constraint already encodes "one row per date"); larger change footprint; no client driver for per-id updates documented in ProjectBrief.

**Verdict: Direction A.** The smaller change matches the V6 schema's date-keyed identity, mirrors the Phase 15-02 `clientSetId` 200/201 split precedent, and avoids inventing a per-id update surface that ProjectBrief Phase 5 page #7 does not request.

### Part B - upsert-by-date semantics and the missing PUT-by-id

Current contract: NO `PUT /api/metrics/{id}` and NO `GET /api/metrics/{id}`. To edit an existing row by id, the client must know its `recordedDate` and POST that. Implications:

- Frontend MUST cache `recordedDate` per row to update an existing entry. This is cheap; `recordedDate` is already on `BodyMetricDto` (`:10`).
- DELETE-then-POST is needed if the user changes the `recordedDate` of an existing row, because the UNIQUE constraint `body_metrics_user_date_unique` (`V6:21`) ties identity to date. Without a separate PUT-by-id, the client must recognize this case explicitly.
- Per-field PATCH is not a separate concern: `UpsertBodyMetricRequest` requires the full mutable shape on every call (no nullable-ignored semantics). A POST with a smaller payload silently nulls unspecified columns because `MetricsService.upsert` (`:39-45`) writes every setter unconditionally.

**Verdict: keep the date-keyed contract; do not add PUT-by-id.** The brief's "Olcum ekleme formu" wording does not imply per-field PATCH; the V6 UNIQUE constraint encodes "one row per date" intentionally. Plan 17-04 (status-code split) is enough; Section 6 lists PUT-by-id under defer.

### Part C - Validation bypass on cross-package writers (Section 5 preview)

`ScaleWebhookController.ingest` at `ScaleWebhookController.java:53-57` writes a `BodyMetric` directly with NO Bean Validation. The endpoint validates only `payload.weightKg() == null` (`:43-45`); the SQL precision check on `NUMERIC(5,2)` permits 0.00-999.99, but the user-facing rule "weight in human range 20-500 kg" is enforced ONLY by `UpsertBodyMetricRequest.java:12`. A scale that posts `weightKg=5.0` would pass the webhook and store a 5.0kg row that `MetricsController.upsert` would reject as 400.

`HealthImportService.apply` at `HealthImportService.java:80-91` has the same shape: per body-mass record, no field-bounds validation, only a date-collision skip-check. An Apple Health export with a malformed entry would be persisted unchecked.

`FullImportService.replaceMetrics` at `FullImportService.java:155-178` is wholesale-replace; `ImportValidator.validate` (called at `FullImportService.java:75-80`) handles payload shape but not field bounds.

Section 5 Part A catalogues each writer; Section 6 settles the validator-extraction direction.

## Section 5 - Cross-Package Coupling Map and Time-Series Endpoint Shape

### Part A - Cross-package writers/readers of `BodyMetric` (bypass `MetricsService`)

One row per call site outside `metrics/` that touches `BodyMetricRepository` or `BodyMetric` directly.

| Caller (file:line) | Operation | Validation surface | Idempotency strategy | Adopt `MetricsService.upsert`? |
| --- | --- | --- | --- | --- |
| `webhooks/ScaleWebhookController.ingest` (`ScaleWebhookController.java:35-60`) | write | payload-level `payload.weightKg() == null` only (`:43-45`); NO `@Valid` on `ScalePayload`; NO weight-range check | first-write-wins by `(userId, recordedDate)` via `findByUserIdAndRecordedDate.isPresent()` short-circuit at `:49-51` (returns 200 without overwrite) | Maybe - gains weight-range validation, but loses passive-sensor "first-write-wins" semantic (upsert overwrites the existing row's weight). Recommend keep direct; share the bounds check via a thin static helper. |
| `health/HealthImportService.apply` (`HealthImportService.java:73-117`, body-mass loop at `:80-91`) | write (per record) | NONE - parser-level only | skip-on-conflict by `(userId, recordedDate)` via `findByUserIdAndRecordedDate.isPresent()` (`:81`); skipped rows count toward `bmSkipped` | No - skip semantics is intentional (avoid clobbering manual edits with imported data). Same bounds-check helper recommendation. |
| `exports/FullExportService.buildMetrics` (`FullExportService.java:101-105,169-181`) | read | N/A | N/A | No, intentional - read-side projection via `findByUserIdOrderByRecordedDateDesc` (`:102`); `MetricRow` builder at `:169-181` carries `photoUrl` (`:179`). |
| `exports/FullImportService.replaceMetrics` (`FullImportService.java:155-178`) | delete-all + insert-all | NONE for field bounds; `ImportValidator` (`FullImportService.java:75-80`) is payload-shape only | wholesale replace | No - import is "delete existing then insert from dump"; per-row upsert would be O(n) round-trips and break the `ImportResultDto.metricsInserted` count semantic. |
| `notifications/ReminderJob.runWeightNudgeAt` (`ReminderJob.java:77-89`) | read | N/A | N/A | No, intentional - pure read for cron logic via `findUserIdsWithoutMetricsSince(since)` (`:79`, query at `BodyMetricRepository.java:19-26`). |

**Verdict.** Five direct call sites; only `ScaleWebhookController` and `HealthImportService` are write paths that bypass `UpsertBodyMetricRequest`'s weight + measurement bounds. Each writer has different conflict semantics (idempotent-by-date for the scale, skip-on-conflict for health import, wholesale-replace for full import), so adopting `MetricsService.upsert` would conflate semantics. The right shape is a thin `BodyMetricValidator` static helper (no Spring bean) covering weight + measurement bounds, called from `MetricsService.upsert`, `ScaleWebhookController.ingest`, and `HealthImportService.apply`. Recommend **defer the validator extraction unless Section 6 plan-count opens a slot**; otherwise mark it as a Phase 31 (charts-stats) deferred concern. Each writer's conflict semantic stays intact; only the shared field-bounds check is consolidated.

### Part B - Time-series endpoint shape

ROADMAP `:78` requires "time-series read endpoints for the metrics UI." ProjectBrief `:361` requires "Kilo grafigi (haftalik/aylik/tum zamanlar)." Three options:

#### Option 1: extend `GET /api/metrics` with `from` and `to` query parameters

- Both optional `LocalDate`. When omitted, returns full history (back-compat with the existing test `postStoresAndGetReturnsTheEntry`).
- When set, returns only rows with `recordedDate BETWEEN from AND to` via a new repo method `findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc(UUID, LocalDate, LocalDate)`.
- Pros: zero new endpoints, single read path for both list and chart UIs, idiomatic, reuses the existing `idx_body_metrics_user_date` (`V6:27-28`) which is `(user_id, recorded_date DESC)` and index-friendly for both `from` and `to` filters.
- Cons: response payload still carries all 12 fields per row even when the chart only needs `(date, weightKg)`.

#### Option 2: add `GET /api/metrics/series?from=&to=&field=weightKg`

- Returns `List<{recordedDate: LocalDate, value: BigDecimal}>` projection via a new DTO.
- Pros: lighter wire payload for chart UIs; matches ROADMAP wording literally.
- Cons: new DTO, new controller method, another endpoint to document, field-projection overkill for v0.4 single-user load (estimated <1000 rows per user).

#### Option 3: hybrid - both Option 1 query params and the lightweight chart endpoint

- Pros: covers list + chart UIs perfectly.
- Cons: 2x change cost, not justified by v0.4 budget.

**Verdict: Option 1.** The `idx_body_metrics_user_date` index makes the range filter free; payload-size concern is hypothetical at v0.4 single-user scale; one endpoint matches the broader codebase convention (`GET /api/sessions/history` is paginated but does not split into a chart endpoint). Defer Option 2 to Phase 28 (frontend metrics-ui) or Phase 31 (charts-stats) if a payload-size finding surfaces during chart wiring.

## Section 6 - Recommended Phase 17 plan-02+ Scope

### photo-url-exposure bucket

- **Add `photoUrl` to `UpsertBodyMetricRequest`.**
  - Targets: append optional `String photoUrl` as the 9th record component on `UpsertBodyMetricRequest.java` with `@Size(max = 500)` mirroring V6's `VARCHAR(500)`. Thread `req.photoUrl()` into `MetricsService.upsert` (one `entity.setPhotoUrl(req.photoUrl())` line between current setters at `:39-45`). Extend `MetricsIntegrationTest` with a `postWithPhotoUrlIsRoundTripped` test asserting POST -> GET carries the value.
  - File touches: `dto/UpsertBodyMetricRequest.java` (1 component append), `MetricsService.java` (1 setter line), `MetricsIntegrationTest.java` (1 new test).
  - Rationale: Section 3 Part A row `photo_url` shows the only column-level GAP across the five sources (ProjectBrief / V6 / entity / response DTO / upsert request). Closes ROADMAP Phase 17 deliverable "optional progress photo URL" on the API surface. No migration needed (V6 already defines the column).

### time-series-range bucket

- **Add `from` and `to` `LocalDate` query parameters to `GET /api/metrics`.**
  - Targets: append `@RequestParam(required = false) LocalDate from` and `to` to `MetricsController.list` (`:30-33`); add `findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc(UUID, LocalDate, LocalDate)` to `BodyMetricRepository.java`; branch in `MetricsService.list` on parameter presence (full history when both null; range when both set; pick a 400-on-mixed-or-inverted policy or accept partial range). Extend `MetricsIntegrationTest` with 2 tests: `rangeFilterReturnsOnlyMatchingRows`, `rangeWithEmptyMatchReturnsEmptyArray`.
  - File touches: `MetricsController.java` (signature change), `MetricsService.java` (branch in `list`), `domain/BodyMetricRepository.java` (1 query method), `MetricsIntegrationTest.java` (2 new tests).
  - Rationale: Section 5 Part B Option 1 verdict. Closes ROADMAP "time-series read endpoints" and the partial verdict on ProjectBrief "Kilo grafigi (haftalik/aylik/tum zamanlar)". Reuses `idx_body_metrics_user_date` (`V6:27-28`); zero schema impact.

### status-code-split bucket

- **Return 200 on update vs 201 on create from `POST /api/metrics`.**
  - Targets: `MetricsService.upsert` returns a wrapper (`(BodyMetricDto dto, boolean wasCreated)` as a record or `Map.Entry`-shaped pair). Controller (`MetricsController.java:35-41`) maps `wasCreated ? 201 : 200`. Existing test `repeatedPostForSameDateUpdatesInsteadOfInserting` at `MetricsIntegrationTest.java:74` flips the second POST assertion from `isCreated()` to `isOk()`. Add 1 test `firstPostReturns201SecondPostReturns200` asserting the create-then-update status pair on the same date.
  - File touches: `MetricsService.java` (signature change), `MetricsController.java` (map), `MetricsIntegrationTest.java` (1 flip + 1 new test).
  - Rationale: Section 4 Part A Direction A verdict. Phase 15-02's `clientSetId` 200/201 precedent applies; the V6 UNIQUE-by-`(user, date)` constraint validates the upsert-by-date contract.

### validation-extraction bucket (deferred unless slot opens)

- **Extract `BodyMetricValidator` static helper covering weight + measurement bounds.**
  - Targets: new file `metrics/BodyMetricValidator.java` (~30 lines, no Spring bean, static methods matching `UpsertBodyMetricRequest`'s ranges); call sites at `MetricsService.upsert` (no behavior change because `@Valid` already runs), `ScaleWebhookController.ingest` (`:53-57`, replaces the `payload.weightKg() == null` early-return with bounds check), `HealthImportService.apply` (`:80-91`, replaces the unconditional save with a bounds-check + skip-or-error path).
  - File touches: 1 new file (~30 lines), `webhooks/ScaleWebhookController.java` (~3 lines), `health/HealthImportService.java` (~3 lines), `MetricsService.java` (optional cleanup - keep `@Valid` for the request path).
  - Rationale: Section 5 Part A verdict. Cross-package coverage; defer unless plan-count budget allows. Each writer's conflict semantic stays intact; only the shared field-bounds rule consolidates. If deferred, mark as a Phase 17-04 absorb-if-slot or Phase 31 deferred concern.

### defer bucket (explicit non-goals for v0.4)

- **PUT-by-id endpoint.** Section 4 Part B verdict; not requested by ProjectBrief; V6 UNIQUE-by-date encodes the date-keyed contract. Defer indefinitely.
- **Photo upload mechanics.** Multipart vs MinIO vs URL string. Phase 5 frontend concern (ProjectBrief `:363` "opsiyonel"); this audit only opens the URL field on the upsert request. Storage strategy belongs to Phase 28 (metrics-ui) or a dedicated upload-pipeline plan.
- **Chart-projection endpoint (`/api/metrics/series`).** Section 5 Part B Option 2; revisit at Phase 28 or Phase 31 if a chart-payload-size finding surfaces.
- **`MetricsMapper` extraction.** `MetricsService.toDto` is a private static at `:55-69`; extract only if test count grows past 12 or a second feature consumes the mapping. Phase 31 candidate.
- **`GET /api/metrics/{id}`.** No client driver in ProjectBrief; the date-keyed list + range read covers the chart UI.

### Plan-count recommendation

**Four plans for Phase 17** (this audit + three hardening plans). The validation-extraction bucket is deferred; it does not earn its own plan slot at v0.4 budget.

- **Plan 17-01 (this audit) - shipped via this deliverable.**
- **Plan 17-02: photo-url-exposure.** Smallest change; closes the lone column-level GAP. ~3 file touches (1 DTO append, 1 service line, 1 test). Independent of 17-03 and 17-04. Land first to remove the spec-vs-code mismatch.
- **Plan 17-03: time-series-range.** Adds the chart-friendly read; new repo method + new query params + 2 new tests. ~4 file touches. Depends on 17-02 only for sequencing (no shared lines).
- **Plan 17-04: status-code-split.** Semantic fix for upsert; tightens the contract; ~3 file touches + 1 test flip + 1 new test. May absorb the validation-extraction bucket if scope budget allows; otherwise the validator stays deferred.

Bucket totals: 1 photo-url entry, 1 time-series-range entry, 1 status-code-split entry, 1 deferred validation-extraction entry, 5 defer markers. No migrations (V6 already carries every column; V27 is the last applied per Phase 16-04).
