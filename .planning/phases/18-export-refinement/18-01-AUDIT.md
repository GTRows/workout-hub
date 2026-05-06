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

## Section 3 - ProjectBrief Claude-Summary Example vs Current `ClaudeSummaryDto` Gap Matrix

The canonical example lives at `ProjectBrief.md:510-573`. This section walks the example top-down against `ClaudeSummaryDto.java:13-48`.

### Part A - Top-level keys

| Brief key | Brief line | Current DTO component | DTO line | Status |
| --- | ---: | --- | ---: | --- |
| `user` | `:514-521` | `user: UserSummary` | `:14` | Subkey-drift (Part B) |
| `period` | `:522-526` | `period: Period` | `:15` | OK at the top level (subkey shape matches: `from`, `to`, `days`) |
| `summary` | `:527-534` | `summary: Totals` | `:16` | Subkey-drift (Part C) |
| `workouts` | `:535-554` | `workouts: List<WorkoutEntry>` | `:17` | Subkey-drift (Part D) |
| `prs` | `:555-562` | - | - | **MISSING** |
| `body_metrics` | `:563-566` | - | - | **MISSING** |
| `consistency` | `:567-571` | - | - | **MISSING** |

Three top-level keys missing entirely; four present with subkey drift.

### Part B - `user` subkey drift (`ProjectBrief.md:514-521` vs `UserSummary` at `ClaudeSummaryDto.java:19-25`)

| Brief key | Brief value (illustrative) | Current component | Drift |
| --- | --- | --- | --- |
| `name` | `"Fatih"` | `displayName` (`:20`) | Key rename only |
| `age` | `26` | - | **MISSING** (computed from `UserProfile.birthDate` and `LocalDate.now(ZoneOffset.UTC)`; full-export already exposes `birthDate` at `FullExportDto.UserSection:24` so the source-of-truth is reachable) |
| `height_cm` | `178` | `heightCm` (`:22`) | Case (snake vs camel) |
| `current_weight_kg` | `78` | `weightKg` (`:23`) | Key rename + case |
| `health_notes` | string | `healthNotes` (`:24`) | Case |
| `goals` | `["Fit vücut", "Karaciğer iyileştirme"]` | `goals: String` (`:25`) | **SHAPE drift**: Brief has array, current is single TEXT string (matches `UserProfile.goals` SQL column TEXT per V1 / `FullExportDto.UserSection:27`). Plus `email` at `:21` of UserSummary is NOT in the brief example - drift in the other direction (an extra field). |

The most surgical concern is `goals` shape (string vs array). Three options walked in Section 4 Part D.

### Part C - `summary` subkey drift (`ProjectBrief.md:527-533` vs `Totals` at `ClaudeSummaryDto.java:29-32`)

| Brief key | Brief value | Current component | Status |
| --- | --- | --- | --- |
| `total_workouts` | `18` | `totalWorkouts` (`:30`) | Case |
| `planned_workouts` | `20` | - | **MISSING** (source: count active plan's `WorkoutDay`s whose `dayOfWeek` falls inside `[period.from, period.to]`, excluding `focus = REST`) |
| `adherence_percent` | `90` | - | **MISSING** (derived: `Math.round(totalWorkouts * 100.0 / plannedWorkouts)` with zero-divide guard) |
| `total_volume_kg` | `45200` | `totalVolumeKg` (`:31`) | Case |
| `avg_session_duration_min` | `47` | `avgSessionDurationMin` (`:32`) | Case |
| `weight_change_kg` | `-1.2` | - | **MISSING** (source: latest `BodyMetric.weightKg` minus earliest in window via `BodyMetricRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc` from Phase 17-03) |

### Part D - `workouts[]` subkey drift (`ProjectBrief.md:535-553` vs `WorkoutEntry`/`ExerciseEntry`/`SetEntry`)

`workouts[i]`:

| Brief key | Brief value | Current component | Status |
| --- | --- | --- | --- |
| `date` | `"2026-04-20"` | `date` (`:35`) | OK |
| `type` | `"Üst Vücut İtme"` | - | **MISSING** (source: `WorkoutSession.workoutDayId` -> `WorkoutDay.name`; null when session is ad-hoc) |
| `duration_min` | `48` | `durationMin` (`:36`) | Case |
| `exercises` | array | `exercises` (`:37`) | OK at the level |
| `user_notes` | text | `userNotes` (`:38`) | Case |
| `mood` | `4` | `mood` (`:39`) | OK |
| `energy` | `4` | `energyLevel` (`:40`) | Key rename (`energy` vs `energyLevel`) |

`workouts[i].exercises[j]`:

| Brief key | Brief value | Current component | Status |
| --- | --- | --- | --- |
| `name` | `"Dumbbell Shoulder Press"` (English single key) | `nameTr` + `nameEn` (`:43-44`) | **Sub-key drift**: brief uses single `name` (English); current splits TR + EN. Direction Part D in Section 4 picks (additive: keep both) vs collapse. |
| `sets` | array | `sets` (`:45`) | OK at the level |

`workouts[i].exercises[j].sets[k]` (Brief: `{"weight": 7, "reps": 10}` at `:544`; current `SetEntry(short repsDone, BigDecimal weightKg)` at `:47`):

| Brief key | Current key | Drift |
| --- | --- | --- |
| `weight` | `weightKg` | Key drift (brief drops the `_kg` suffix; loses unit context but matches the brief example literally) |
| `reps` | `repsDone` | Key drift (`reps` vs `repsDone`) |

Note: record component declaration order differs (brief weight-first vs current reps-first), but Jackson default emits properties in declaration order — Jackson does NOT respect brief order under default settings. The order drift is a JSON aesthetic concern; the key drift is the meaningful one.

### Part E - Missing top-level keys (sources-of-truth deferred to Section 5)

#### `prs[]` (`ProjectBrief.md:555-562`)

Each entry: `{"exercise": "Goblet Squat", "weight": 15, "reps": 12, "date": "2026-04-15"}`.

Source candidates:
- (1) per-set Epley scan via `analytics/PrDetector.epleyOneRm(weightKg, repsDone)` at `PrDetector.java:12-18`, applied to the in-window sessions.
- (2) `SessionSet.isPr=true` rows in window joined to exercise (Phase 16-04 persisted `is_pr` via V27).
- (3) `AnalyticsService.personalRecords(userId)` at `AnalyticsService.java:165-200` (returns all-time top-1-per-exercise as `List<PrDto>`), filtered by date.

Verdict deferred to Section 5 Part A.

#### `body_metrics[]` (`ProjectBrief.md:563-566`)

Each entry: `{"date": "2026-04-01", "weight_kg": 79.2}`. Two fields per row.

Source: `MetricsService.list(userId, period.from, period.to)` from Phase 17-03 returns `List<BodyMetricDto>` (12 components per row). Project to `(date, weightKg)`. Verdict deferred to Section 5 Part B.

#### `consistency` (`ProjectBrief.md:567-571`)

`{"current_streak_days": 5, "missed_days": [...], "missed_reasons": [...]}`.

Source candidates:
- `current_streak_days`: `analytics/StreakCalculator.compute(rawSessionDates, today)` at `StreakCalculator.java:16-19` returns `Result(current, longest)`; the in-window-current is derivable. Or re-derive locally from the in-window sessions list.
- `missed_days`: derive from active plan's `dayOfWeek` set within `[from, to]` minus actual session dates.
- `missed_reasons`: NO data source today. No `missed_workouts` table. V27 is the last applied migration; V28 is the next free Flyway slot.

Verdict deferred to Section 5 Part C.

## Section 4 - Behavioral Analysis

### Part A - JSON property naming convention

`JacksonConfig.java:11-19` configures `JavaTimeModule`, `WRITE_DATES_AS_TIMESTAMPS=false`, `serializationInclusion=NON_NULL`. Does NOT set `PropertyNamingStrategies.SNAKE_CASE`. Grep confirms zero pre-existing `@JsonNaming` usage in `backend/src` (this audit declares the precedent).

#### Direction A - migrate ALL export DTOs to snake_case

Target: class-level `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` on `ClaudeSummaryDto`, `FullExportDto`, `ImportResultDto`, `CsvImportResultDto`.

- Touches: 4 DTO files (1 annotation each), `docs/EXPORT_FORMAT.md` (every camelCase example flips), `docs/examples/full-export-example.json` (every key flips), `ExportIntegrationTest.java` (4 jsonPath assertions flip), `FullExportImportIntegrationTest.java` (~8 assertions flip), `ExportFormatExampleTest.java` (1 assertion flip), frontend export client + tests (all `bodyMetrics`, `supplements`, `schemaVersion` references).
- Pros: matches the brief example literally on the LLM-paste workflow; one consistent wire convention across the package.
- Cons: BREAKING change on `FullExportDto` round-trip - every prior backup file becomes unimportable unless the example also flips and the operator re-runs export. The `ExportFormatExampleTest` would fail until the example JSON is rewritten in lockstep.

#### Direction B - keep camelCase everywhere, document the deviation

Target: extend `docs/EXPORT_FORMAT.md` with a "claude-summary wire format" section that says "WorkoutHub emits camelCase; the ProjectBrief example is illustrative."

- Touches: `docs/EXPORT_FORMAT.md` (add a section, ~20 lines).
- Pros: smallest possible change; no DTO touches; backups remain importable; client unaffected.
- Cons: the wire format diverges from the brief on the canonical LLM-paste workflow; users who paste the spec example into Claude get one shape but the actual export emits a different shape.

#### Direction C - split per consumer

Target: class-level `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` on `ClaudeSummaryDto` ONLY. `FullExportDto`, `ImportResultDto`, `CsvImportResultDto` stay camelCase.

- Touches: `dto/ClaudeSummaryDto.java` (1 class annotation; recursive across nested records via record-level inheritance? Jackson's `SnakeCaseStrategy` applies to all fields of the annotated class but NOT to nested record types unless they are also annotated - **must apply the annotation on every nested record explicitly**: `UserSummary`, `Period`, `Totals`, `WorkoutEntry`, `ExerciseEntry`, `SetEntry` -> 7 annotations total). Or apply via `Jackson2ObjectMapperBuilderCustomizer` scoped at write time - more invasive. Recommend the per-record annotation. `ExportIntegrationTest.java` flips ~4 jsonPath assertions to snake_case.
- Pros: LLM consumer matches the brief; backup/restore is unaffected; no doc/example/frontend churn.
- Cons: two conventions in one package (mild cognitive load); 7 annotations needed (one per claude-summary record).

#### Recommendation

**Direction C.** The full-export contract is a backup-and-restore wire format — LLM ergonomics are not the driver there, and `docs/EXPORT_FORMAT.md` already declares it as the source of truth. The claude-summary contract IS LLM-paste-driven and the brief example is the spec; matching the brief directly improves the paste-workflow without disrupting backup users. Per-record `@JsonNaming` is the surgical landing site.

If during plan 18-03 it surfaces that `frontend/(app)/export/` reads claude-summary fields in code (currently it does NOT — `ExportClient` only triggers downloads), the verdict is unchanged: the JSON is server-rendered for paste, not consumed by frontend code.

### Part B - Cost of adding each missing top-level key

| Missing key | If added to `ClaudeSummaryDto` | If left out |
| --- | --- | --- |
| `prs` | +1 record component on top-level, +1 nested `PrEntry` record (4 fields), +1 mapper helper on `ExportService` (computePrsInWindow scan), +1 cross-package import (`analytics/PrDetector`) | ROADMAP "prs" deliverable not closed; brief example divergent |
| `body_metrics` | +1 record component, +1 nested `BodyMetricSummary` record (2 fields), +1 cross-package call (`metrics/MetricsService.list`) | ROADMAP not literally listing `body_metrics` but brief lists it as authoritative; LLM coach loses weight-trend signal |
| `consistency` | +1 record component, +1 nested `Consistency` record (3 fields), +2 cross-package reads (`analytics/StreakCalculator` or local re-derive; `workouts/WorkoutPlanRepository.findByUserIdAndActiveTrue` for `missed_days`) | ROADMAP "consistency" deliverable not closed |

**Recommendation: ADD all three.** ROADMAP `:90` literally lists `prs` and `consistency` as Phase 18 deliverables. `body_metrics` is brief-mandated. Section 6 buckets them together under `claude-summary-fields` since the touch site is the same DTO + service.

### Part C - `summary` subkey gaps

`planned_workouts`:
- Source: count `WorkoutDay`s on the user's active plan whose `dayOfWeek` (1-7 ISO) maps to a calendar day in `[period.from, period.to]`. `period` covers `windowDays` consecutive days, so `plannedWorkouts = sum over each WorkoutDay d of: count of calendar days in window where DayOfWeek(day) == d.dayOfWeek and d.focus != REST`. One repo call: `WorkoutPlanRepository.findByUserIdAndActiveTrue` (already used by `IcsExportService` at `:38`).
- Cost: cheap; no new SQL.
- Edge case: no active plan -> emit `null` (Jackson NON_NULL strips); `adherence_percent` becomes meaningless and must also emit null.

`adherence_percent`:
- Pure derivation from `totalWorkouts` and `plannedWorkouts`. Zero-divide guard: emit null when `plannedWorkouts == null || plannedWorkouts == 0`. Round to nearest integer per brief (`90` not `90.0`).

`weight_change_kg`:
- Source: `MetricsService.list(userId, period.from, period.to)` returns `List<BodyMetricDto>` ordered by `recordedDate DESC`. Latest minus earliest with non-null `weightKg`. Emit null when fewer than 2 non-null weights in window.
- Cost: 1 cross-package read (already needed for `body_metrics[]`); reuse the same fetch.

All three gaps fold into the same plan as the snake_case migration if Direction C is chosen; one plan covers `summary` subkeys + the snake_case verdict.

### Part D - `user.age` and `user.goals` array shape

`age`:
- Derived from `UserProfile.birthDate` (already on the entity per Phase 17-01 audit Section 1; `FullExportDto.UserSection.birthDate` at `:24` exposes it). Compute `Period.between(birthDate, LocalDate.now(ZoneOffset.UTC)).getYears()` at export time. If `birthDate` null, emit null.
- Cost: trivial.

`goals` array:
- Today: `UserProfile.goals` is SQL TEXT; `FullExportDto.UserSection.goals` is `String` matching the column.
- Three options:
  - (1) Split client-side at claude-summary export time on a delimiter (newline OR `;`). Document the delimiter in `docs/EXPORT_FORMAT.md` claude-summary section. Full-export round-trip unaffected.
  - (2) Add a parallel SQL column `user_profile.goals_array TEXT[]` keeping the original `goals` for back-compat. Two-source-of-truth issue.
  - (3) Migrate `user_profile.goals` to `text[]` in V28; entity `String` -> `List<String>`; profile form rewrites to a chip-input or multiline textarea with split-on-save. BREAKING for the existing profile UI.

**Recommendation: option (1).** v0.4 budget; no migration; the brief example shape lifts to OK without disrupting the existing profile form. Trade-off: the full-export shape stays single-string (operator sees `"goals": "Fit vücut\nKaraciğer iyileştirme"` in the backup file); the claude-summary shape shows the array view.

### Part E - `workouts[i].type` derivation

`WorkoutSession.workoutDayId` is nullable (sessions can be ad-hoc; `FullImportService.replacePlansSection:128` calls `sessions.detachSessionsFromDays(userId)` which UPDATEs `workout_day_id = NULL`). When non-null, fetch `WorkoutDay.name` (e.g. `"Üst Vücut İtme"`); when null, emit null and let Jackson NON_NULL strip the key.

Cost: one repo lookup per distinct non-null `workoutDayId` in the recent-sessions list. Cache via `Map<UUID, String>` populated by a single `findAllById(distinctIds)` call to keep complexity O(1) per session.

Implementation site: `ExportService.toEntry` already iterates `session.getSets()`; thread the prepopulated map through as a second argument. Or batch-load before the `recent.stream().map(...)` chain.

### Part F - Round-trip stability re-check (post-Plan-14-02 i-2 closure)

Pipeline: `GET /api/export/full` -> `POST /api/export/import` -> `GET /api/export/full` -> diff.

#### Acceptable drift

- `exportedAt` is `Instant.now()` per `FullExportService.build:64`. NEVER byte-identical across exports. Acceptable (metadata, not data). Document re-export comparison strategy that ignores `exportedAt`.
- `BodyMetric.id` and `Supplement.id` not preserved on import: `replaceMetrics:155-178` and `replaceSupplements:180-198` instantiate fresh entities WITHOUT `m.setId(r.id())`. After round-trip, these ids change. Acceptable for v0.4: no downstream FK depends on them; round-trip is "data-equal" not "byte-equal". Document.
- `Supplement.timing` enum tolerance: `parseTiming` at `FullImportService.java:318-327` throws 422 on unknown values (NOT silent downgrade as the plan's context paragraph stated; the plan misread the source). The `ImportValidator.validateSupplements:148-157` warns but `parseTiming` itself is strict (`valueOf` -> 422). Behavior is correct. Document the tolerance asymmetry between validator (warns) and importer (rejects).

#### Production gaps

**Gap 1: `is_pr` round-trip drop.** `FullExportDto.SetRow` (`:64-72`) has 8 components, none is `isPr`. After Phase 16-04 persisted `SessionSet.isPr` via V27, the export silently drops the column at `FullExportService.toSessionSection:148-156`. After re-import, every set's `is_pr` flips to `false` (V27 `DEFAULT false`) regardless of what the source database had. **Subsequent `/api/sessions/{id}` reads return `newPr: null` for every set.** Until a PR-recompute pass runs, no PRs are visible.

This is a NEW production gap surfaced by Phase 16-04. Plan 14-02 closed i-2 (test-side seed); it predates the V27 column. Fix: add `Boolean isPr` as the 9th `SetRow` component; preserve on `insertSessions:286-300`; OR call `SessionSetsService.recomputePrForExerciseHistory(userId, exerciseId)` for each touched `(user, exerciseId)` pair at the END of the import transaction.

**Raise as NEW ISSUE i-10 candidate.** Plan 18-04 candidate.

**Gap 2: session `workoutDayId` not validated against the payload's plan.day.id set.** `FullImportService.importDump` (`:71-109`) sequence: wipeSessions -> replacePlans -> insertSessions. If the payload's `sessions[i].workoutDayId` references a `workout_day.id` that does NOT exist in the same payload's `plans[].days[].id` set, the FK violation fires at `sessions.save(s)` (`:301`) and rolls back the entire transaction. The error surfaces as a 500-class status, not a curated 422. `ImportValidator` does NOT pre-validate this referential integrity (`validateSessions:116-146` only checks set-level shape).

Fix: add `ImportValidator.validateSessionDayIds(plans, sessions, errors)` that collects `Set<UUID> declaredDayIds` from `plans[].days[].id` and asserts `every session's workoutDayId is null OR in declaredDayIds`. Adds 1 method, 1 call line in `validate(...)`, 1 new test. Fold into Plan 18-04 alongside Gap 1.

**Gap 3: `SetRow.completed` write back-trip semantics.** `SetRow.completed` (`:71`) is `boolean` primitive (not nullable). `SessionSet.isCompleted()` returns `boolean`. The export carries it. Import preserves it via `set.setCompleted(r.completed())` at `:296`. No drift. Acceptable; mention only to confirm `completed` round-trips correctly (contrast with `is_pr` which silently drops).

**Gap 4: `goals` round-trip stability.** `FullExportDto.UserSection.goals` is `String` matching `UserProfile.goals` SQL TEXT. Round-trip is stable. Direction Part D option (1) "split at claude-summary export time" does NOT affect the full-export shape; it only affects the brief LLM-paste view. Confirmed acceptable.

#### Summary

Five round-trip behaviors cataloged: 3 acceptable (exportedAt drift, BodyMetric/Supplement id non-preservation, timing strict-reject documented), 2 production gaps (is_pr drop -> i-10, session.workoutDayId validator gap). Both gaps fold into Plan 18-04.
