---
phase: 18-export-refinement
plan: 01
deliverable: audit
---

# Exports Package Audit

## Section 1 - Exports Package Inventory

Tree of `backend/src/main/java/com/workouthub/exports/`-owned files plus the documentation pair under `docs/`. Line counts measured 2026-05-06.

### Controller (1 file, 184 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `ExportController.java` | 184 | REST surface under `/api/export`. Constructor-injects six collaborators (`ExportService`, `FullExportService`, `FullImportService`, `CsvExportService`, `CsvImportService`, `IcsExportService`); 13 endpoints across claude-summary + full-dump + 5 read slices + 5 write slices + ICS + CSV. Class-level `@Validated`. |

### Read-side services (4 files, 598 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `ExportService.java` | 131 | `buildSummary(userId, days)` produces `ClaudeSummaryDto`. Hand-written `toEntry` and `computeTotals` private statics; private `ExerciseGroup` record; `@Transactional(readOnly = true)`. |
| `FullExportService.java` | 191 | `build(userId)` and per-slice builders for `UserSection`, `PlanSection[]`, `SessionSection[]`, `MetricRow[]`, `SupplementRow[]`. `SCHEMA_VERSION = 1` (`:37`). Hand-written `toPlanSection`, `toDayRow`, `toDayExerciseRow`, `toSessionSection`, `toMetricRow`, `toSupplementRow` private statics. |
| `CsvExportService.java` | 78 | Strong/Hevy-compatible session history CSV via `buildSessionsCsv(userId)` + static `buildCsv(history)`. Header at `:26-27`. RFC-4180 quoting via `csvField`. `@Transactional(readOnly = true)`. |
| `IcsExportService.java` | 98 | RFC 5545 iCalendar feed for the active plan (`buildActivePlanIcs`). One weekly-recurring VEVENT per `WorkoutDay`. Default 18:00 start, 60-min duration. |

### Write-side services (2 files, 509 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `FullImportService.java` | 328 | `importDump(userId, dump)` orchestrator (`:71-109`); per-slice `importProfile`, `replaceMetricsSection`, `replaceSupplementsSection`, `replacePlansSection`, `replaceSessionsSection`. Delete-then-reinsert replace semantics under a single `@Transactional`. `parseFocus` and `parseTiming` private statics with strict `valueOf` (throws 422 on unknown). |
| `CsvImportService.java` | 181 | Strong/Hevy CSV import via `importSessionsCsv(userId, csv)`. Groups rows by `(date, workoutName)`; uses `ExerciseNameMatcher`; surfaces unmatched names + warnings instead of failing. |

### Helpers (3 files, 299 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `ImportValidator.java` | 176 | Static `validate(FullExportDto)` returns `ValidationReport(errors, warnings, suggestions)`. Schema version, multiple-active-plan, blank-plan-name, day-of-week range, exercise rows, session set sanity, supplement timing tolerance. |
| `CsvRowReader.java` | 66 | Minimal RFC 4180 CSV reader (quoted fields, escaped double-quotes, CRLF/LF line endings). Package-private. |
| `ExerciseNameMatcher.java` | 56 | Tiered match (exact, case-insensitive, substring) of free-text exercise names against the catalog. |

### DTOs (4 files, 174 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `dto/ClaudeSummaryDto.java` | 48 | Top-level + 6 nested records: `UserSummary`, `Period`, `Totals`, `WorkoutEntry`, `ExerciseEntry`, `SetEntry`. |
| `dto/FullExportDto.java` | 92 | Top-level + 8 nested records: `UserSection`, `PlanSection`, `DayRow`, `DayExerciseRow`, `SessionSection`, `SetRow`, `MetricRow`, `SupplementRow`. |
| `dto/ImportResultDto.java` | 25 | 8-component result; 6-arg back-compat constructor at `:15-24`. |
| `dto/CsvImportResultDto.java` | 9 | 4-component CSV import result. |

### Tests (3 files, 398 lines)

| File | Lines | Responsibility |
| --- | ---: | --- |
| `ExportIntegrationTest.java` | 141 | 4 `@Test` methods: 401, claude-summary content + Content-Disposition, cross-user isolation, days=0 -> 400. |
| `FullExportImportIntegrationTest.java` | 190 | 6 `@Test` methods: 401, schemaVersion + slices, schemaVersion-99 -> 422, 2-active -> 422, plans round-trip (the post-Plan-14-02 test seeds via POST `/api/workout-plans`), metrics+supplements replace round-trip. |
| `ExportFormatExampleTest.java` | 67 | 1 `@Test` method that imports `docs/examples/full-export-example.json` and asserts `metricsInserted=1`, `supplementsInserted=1`, then GETs `/api/metrics` and `/api/supplements` to verify the example becomes the read state. |

### Documentation pair (under `docs/`)

| File | Status | Responsibility |
| --- | --- | --- |
| `docs/EXPORT_FORMAT.md` | Present (209 lines) | Wire-format source of truth for `/api/export/full` and `/api/export/import`. Documents envelope, every section, enum vocabulary, import semantics. Already declares schemaVersion 1, RFC 3339 UTC timestamps, `YYYY-MM-DD` dates. NO mention of claude-summary shape. NO snake_case stance documented. |
| `docs/examples/full-export-example.json` | Present (40 lines) | Minimal payload (1 metric, 1 supplement, empty plans/sessions, populated user). Used by `ExportFormatExampleTest` as a contract regression. NO `is_pr` field on sets (intentional - no sessions present); confirms `goals` is a free-text string. |

**`exports/`-owned source total:** 14 source files / 1489 lines (10 services + helpers) plus 4 DTOs / 174 lines plus 3 test classes / 398 lines. **Grand total 21 files / 2061 lines under `exports/`** (production + test + DTO).

### Cross-package repository touchpoints (no new SQL)

| Source repository | Method called from exports | Tables touched | Migration source |
| --- | --- | --- | --- |
| `UserRepository.findById` | `ExportService.buildSummary` (`ExportService.java:52`), `FullExportService.buildUserSection` (`FullExportService.java:73`), `FullImportService.importDump` (`FullImportService.java:82`) | `users` | V1 (initial schema) |
| `UserProfileRepository.findById` | `ExportService.buildSummary` (`:54`), `FullExportService.buildUserSection` (`:75`), `FullImportService.doImportProfile` (`:139`) | `user_profile` | V1 |
| `WorkoutPlanRepository.findByUserIdOrderByCreatedAtAsc` | `FullExportService.buildPlans` (`:89`), `FullImportService.replacePlans` (`:213`) | `workout_plans`, `workout_days`, `workout_day_exercises` | V2/V3/V4 (Phase 13-01 audit Section 1) |
| `WorkoutPlanRepository.findByUserIdAndActiveTrue` | `IcsExportService.buildActivePlanIcs` (`:38`) | `workout_plans` | V2 |
| `WorkoutSessionRepository.findFinishedSince` | `ExportService.buildSummary` (`:57`) | `workout_sessions`, `session_sets` | V5/V6 + V20 (Phase 15) + V26/V27 (Phase 16) |
| `WorkoutSessionRepository.findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc` | `FullExportService.buildSessions` (`:96`), `CsvExportService.buildSessionsCsv` (`:37`), `FullImportService.wipeSessions` (`:202`) | `workout_sessions`, `session_sets` | V5+V20+V26+V27 |
| `BodyMetricRepository.findByUserIdOrderByRecordedDateDesc` | `FullExportService.buildMetrics` (`:102`), `FullImportService.replaceMetrics` (`:157`) | `body_metrics` | V6 (Phase 17-01 audit Section 1) |
| `SupplementRepository.findByUserIdOrderByCreatedAtAsc` | `FullExportService.buildSupplements` (`:108`), `FullImportService.replaceSupplements` (`:182`) | `supplements` | V6 |
| `WorkoutSessionRepository.detachSessionsFromDays` | `FullImportService.replacePlansSection` (`:128`) | `workout_sessions` (UPDATE workout_day_id = NULL) | V5/V20 |
| `WorkoutSessionRepository.findByUserIdAndEndedAtIsNull` | `FullImportService.wipeSessions` (`:205`) | `workout_sessions` | V5/V20 |
| `ExerciseRepository.findAll` | `CsvImportService.importSessionsCsv` (`:48`) | `exercises` | V1/V2 |

The exports package is read-heavy and write-coordinating: 11 distinct repository methods consumed across 8 source files. No JPQL / `@Query` defined inside `exports/`; all queries live in their owning packages.

## Section 2 - Endpoint and DTO Catalog

### `ExportController` endpoints (base `/api/export`)

All endpoints require a JWT-authenticated principal (`@AuthenticationPrincipal AppUserPrincipal`). 13 endpoints total (the plan estimated 11; `/api/export/import/sessions` and `/api/export/import/plans` plus `/api/export/import/csv` were missed in the plan estimate).

| HTTP | Path | Method (file:line) | Request DTO | Response DTO | 2xx | Idempotency | Query / headers |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | `/api/export/claude-summary` | `claudeSummary` (`ExportController.java:56-67`) | none | `ClaudeSummaryDto` | 200 | yes (read-only) | `days` query (default 30, `@Min(1) @Max(365)`); `Content-Disposition: attachment; filename="workouthub-claude-<period.to>.json"`; `Content-Type: application/json` |
| GET | `/api/export/full` | `fullDump` (`:69-79`) | none | `FullExportDto` | 200 | yes (read-only) | none; `Content-Disposition: attachment; filename="workouthub-full-<LocalDate.now>.json"` |
| POST | `/api/export/import` | `importDump` (`:81-86`) | `FullExportDto` | `ImportResultDto` | 200 | yes (delete-then-reinsert; same payload re-applied converges) | none; rejects missing schemaVersion 1 with 422 (`ImportValidator` + `FullImportService` `:75-80`) |
| GET | `/api/export/profile` | `profileSlice` (`:88-91`) | none | `UserSection` | 200 | yes | none |
| GET | `/api/export/plans` | `plansSlice` (`:93-96`) | none | `List<PlanSection>` | 200 | yes | none |
| GET | `/api/export/sessions` | `sessionsSlice` (`:98-102`) | none | `List<SessionSection>` | 200 | yes | none |
| GET | `/api/export/metrics` | `metricsSlice` (`:104-107`) | none | `List<MetricRow>` | 200 | yes | none |
| GET | `/api/export/supplements` | `supplementsSlice` (`:109-113`) | none | `List<SupplementRow>` | 200 | yes | none |
| POST | `/api/export/import/profile` | `importProfileSlice` (`:115-121`) | `UserSection` | `ImportResultDto` | 200 | yes (overwrite-or-create) | none |
| POST | `/api/export/import/metrics` | `importMetricsSlice` (`:123-129`) | `List<MetricRow>` | `ImportResultDto` | 200 | yes (delete-then-reinsert) | none |
| POST | `/api/export/import/supplements` | `importSupplementsSlice` (`:131-137`) | `List<SupplementRow>` | `ImportResultDto` | 200 | yes (delete-then-reinsert) | none |
| POST | `/api/export/import/plans` | `importPlansSlice` (`:139-145`) | `List<PlanSection>` | `ImportResultDto` | 200 | yes (delete-then-reinsert; calls `sessions.detachSessionsFromDays(userId)` first per `:128`) | none |
| POST | `/api/export/import/sessions` | `importSessionsSlice` (`:177-183`) | `List<SessionSection>` | `ImportResultDto` | 200 | yes (wipe-then-insert) | none |
| GET | `/api/export/plan.ics` | `planIcs` (`:147-156`) | none | `String` (`text/calendar; charset=utf-8`) | 200 | yes | `Content-Disposition: attachment; filename="workouthub-plan.ics"` |
| POST | `/api/export/import/csv` | `importSessionsCsv` (`:158-163`) | `String` (`text/csv`) | `CsvImportResultDto` | 200 | NOT idempotent (each call inserts new sessions; no replace semantics) | `consumes = "text/csv"` |
| GET | `/api/export/csv/sessions` | `sessionsCsv` (`:165-175`) | none | `String` (`text/csv; charset=utf-8`) | 200 | yes | `Content-Disposition: attachment; filename="workouthub-sessions-<LocalDate.now>.csv"` |

**Endpoint count correction.** The plan stated "Eleven rows expected"; the actual count is 16 endpoints (13 controller methods, with one `import/csv` and one `csv/sessions` accounting for two CSV surfaces; the count of distinct `@*Mapping` annotations on `ExportController.java` is 16). The plan's "11 rows" likely undercounted the per-slice imports + the CSV pair. This audit catalogs all 16.

### `ClaudeSummaryDto` record breakdown (`ClaudeSummaryDto.java:13-48`)

| Layer | Components | Count |
| --- | --- | ---: |
| Top level (`:13-17`) | `user`, `period`, `summary`, `workouts` | 4 |
| `UserSummary` (`:19-25`) | `displayName`, `email`, `heightCm`, `weightKg`, `healthNotes`, `goals` | 6 |
| `Period` (`:27`) | `from`, `to`, `days` | 3 |
| `Totals` (`:29-32`) | `totalWorkouts`, `totalVolumeKg`, `avgSessionDurationMin` | 3 |
| `WorkoutEntry` (`:34-40`) | `date`, `durationMin`, `exercises`, `userNotes`, `mood`, `energyLevel` | 6 |
| `ExerciseEntry` (`:42-45`) | `nameTr`, `nameEn`, `sets` | 3 |
| `SetEntry` (`:47`) | `repsDone` (short), `weightKg` (BigDecimal) | 2 |

Total: 7 record types, 27 components. NO `prs`, NO `bodyMetrics`, NO `consistency`, NO `age`, NO `type`, NO `plannedWorkouts`, NO `adherencePercent`, NO `weightChangeKg`. Class-level Javadoc at `:7-12` already acknowledges "body_metrics and consistency.missed_reasons left empty until PHASE 5's metrics surface lands" - i.e. the gap is documented in the source itself.

### `FullExportDto` record breakdown (`FullExportDto.java:9-92`)

| Layer | Components | Count |
| --- | --- | ---: |
| Top level (`:9-16`) | `schemaVersion`, `exportedAt`, `user`, `plans`, `sessions`, `bodyMetrics`, `supplements` | 7 |
| `UserSection` (`:18-27`) | `id`, `email`, `displayName`, `heightCm`, `weightKg`, `birthDate`, `gender`, `healthNotes`, `goals` | 9 |
| `PlanSection` (`:29-33`) | `id`, `name`, `active`, `days` | 4 |
| `DayRow` (`:35-41`) | `id`, `dayOfWeek`, `name`, `focus`, `estimatedDurationMin`, `exercises` | 6 |
| `DayExerciseRow` (`:43-52`) | `id`, `exerciseId`, `orderIndex`, `targetSets`, `targetRepsMin`, `targetRepsMax`, `targetWeightKg`, `restSeconds`, `notes` | 9 |
| `SessionSection` (`:54-62`) | `id`, `workoutDayId`, `startedAt`, `endedAt`, `notes`, `mood`, `energyLevel`, `sets` | 8 |
| `SetRow` (`:64-72`) | `id`, `exerciseId`, `setNumber`, `repsDone`, `weightKg`, `rpe`, `completed`, `notes` | 8 |
| `MetricRow` (`:74-84`) | `id`, `recordedDate`, `weightKg`, `bodyFatPercent`, `waistCm`, `chestCm`, `armCm`, `thighCm`, `photoUrl`, `notes` | 10 |
| `SupplementRow` (`:86-91`) | `id`, `name`, `dosage`, `timing`, `active` | 5 |

Total: 9 record types, 66 components. **`SetRow` has 8 components, none is `isPr`.** This is the round-trip gap surfaced post-Plan-16-04 (Section 4 Part F).

### `ImportResultDto` shape (`ImportResultDto.java:5-25`)

8-component record (`profileUpdated`, `metricsInserted`, `supplementsInserted`, `plansInserted`, `sessionsInserted`, `userEmail`, `warnings`, `suggestions`) with a 6-arg back-compat constructor at `:15-24` (the per-slice handlers use it: `ExportController.java:120, 128, 136, 144, 182`).

### `CsvImportResultDto` shape (`CsvImportResultDto.java:5-9`)

4-component record (`sessionsInserted`, `setsInserted`, `unmatchedExerciseNames`, `warnings`).

### Test methods catalog

| # | Class | Method | Line | Outcome |
| ---: | --- | --- | ---: | --- |
| 1 | `ExportIntegrationTest` | `unauthenticatedIsRejected` | `:38` | Bare `GET /api/export/claude-summary` -> 401. |
| 2 | `ExportIntegrationTest` | `summaryBundlesRecentFinishedSessionsAndTotalsForTheCaller` | `:43` | Run 2 sets x 10 reps @ 50kg; assert `$.summary.totalWorkouts=1`, `$.summary.totalVolumeKg=1000.00`, `$.workouts[0].exercises[0].sets[0].repsDone=10`, `$.workouts[0].exercises[0].sets[0].weightKg=50.0`. Asserts current camelCase shape. |
| 3 | `ExportIntegrationTest` | `summaryDoesNotIncludeOtherUsersSessions` | `:71` | User-A sees 0 workouts when only User-B has finished sessions. |
| 4 | `ExportIntegrationTest` | `daysQueryParamClampedBelowOneReturns400` | `:90` | `?days=0` -> 400 (Bean Validation `@Min(1)`). |
| 5 | `FullExportImportIntegrationTest` | `unauthenticatedFullExportReturns401` | `:30` | Bare `GET /api/export/full` -> 401. |
| 6 | `FullExportImportIntegrationTest` | `fullExportReturnsSchemaVersionAndCurrentUserSlices` | `:35` | Seeds 1 metric, 1 supplement, 1 plan; asserts `$.schemaVersion=1`, `$.bodyMetrics.length()=1`, `$.supplements.length()=1`, `$.plans.length()=1`. |
| 7 | `FullExportImportIntegrationTest` | `importRejectsUnsupportedSchemaVersion` | `:67` | `schemaVersion=99` -> 422. |
| 8 | `FullExportImportIntegrationTest` | `importRejectsPayloadsWithMultipleActivePlans` | `:89` | Two `active=true` plans -> 422 (validator rule). |
| 9 | `FullExportImportIntegrationTest` | `importRoundTripPreservesPlansFromExport` | `:114` | The post-Plan-14-02 test: seeds via `POST /api/workout-plans` BEFORE export, then export -> import -> assert `plansInserted >= 1`, then `GET /api/workout-plans/active` is 2xx. The seed step (`:120-124`) is the i-2 test-side fix from Phase 14-02. |
| 10 | `FullExportImportIntegrationTest` | `importRoundTripReplacesMetricsAndSupplements` | `:144` | Seeds 1 metric + 1 supplement; exports; rewrites payload to drop supplements; imports modified payload; asserts `supplementsInserted=0`, GET `/api/supplements` length 0, GET `/api/metrics` length 1. |
| 11 | `ExportFormatExampleTest` | `documentedExampleImportsAndBecomesTheCurrentExportState` | `:41` | Reads `docs/examples/full-export-example.json`; POSTs to import; asserts `metricsInserted=1`, `supplementsInserted=1`; reads back via `/api/metrics` and `/api/supplements`. Locks docs/example/code triangle. |

Eleven tests total across the three classes.
