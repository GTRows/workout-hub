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
