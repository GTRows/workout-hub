# State

## Current Position

Milestone: v0.4 Backend Feature Completion (active)
Phase: 17 of 20 (body-metrics) - in progress (3 of 4 plans shipped)
Plan: 17-03 complete (time-series-range)
Status: Phase 17 plans 01-03 shipped; Phase 16 complete (4/4). Plan 17-03 added optional from/to LocalDate query params on GET /api/metrics with mixed-pair and inverted-range 400s, a derived-query repo method reusing idx_body_metrics_user_date, and 4 new integration tests. Plan 17-04 (status-code-split with optional validator absorb) is the last Phase 17 plan.
Last activity: 2026-05-05 - Completed 17-03-PLAN.md (time-series-range: 1 new derived-query method on BodyMetricRepository, MetricsService.list signature (UUID, LocalDate, LocalDate) with full-history vs range vs 400 (mixed-pair, inverted) branches via ResponseStatusException, MetricsController.list with 2 @RequestParam(required = false) LocalDate params, 4 new integration tests; test method count 7 to 11; 4 file touches; no migration/DTO/entity change; mvn verify delegated to CI per local-maven-gap)

Progress: v0.4 ############________ 59% (4/8 phases complete plus 3/4 of Phase 17; Phase 15 and 16 done)
          v0.5 (planned) - Phases 21-30
          v0.6 (planned) - Phases 31-37
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.4 detailed; v0.5/v0.6/v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-4, i-5, i-6, i-7, i-8, i-9 remain open; i-1 and i-2 closed by Phase 14)
- See: `.planning/phases/13-workouts-audit/13-01-AUDIT.md` for the workouts audit deliverable.
- See: `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` for the sessions-analytics audit deliverable (297 lines, 6 sections).
- See: `.planning/phases/14-workouts-hardening/14-01-SUMMARY.md` for the i-1 cascade-id fix.
- See: `.planning/phases/15-sessions-core/15-01-AUDIT.md` for the sessions audit deliverable (251 lines, 6 sections).
- See: `.planning/phases/15-sessions-core/15-02-SUMMARY.md` for the clientSetId idempotency key plan deliverable.
- See: `.planning/phases/15-sessions-core/15-03-SUMMARY.md` for the heart-rate exposure plan deliverable.
- See: `.planning/phases/15-sessions-core/15-04-SUMMARY.md` for the typed 409 codes + auto-numbering verdict plan deliverable.
- See: `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` for the sessions-analytics audit deliverable (297 lines, 6 sections; package-boundary, PR durability, perf, plan-count verdicts).
- See: `.planning/phases/16-sessions-analytics/16-02-SUMMARY.md` for the package-extraction plan deliverable (5 source plus 3 test rename to com.workouthub.analytics; one-import touch on SessionSetsService).
- See: `.planning/phases/16-sessions-analytics/16-03-SUMMARY.md` for the epley-projection plan deliverable (ProgressPointDto 7th component estimatedOneRmKg; per-top-set Epley aggregation in ExerciseAnalyticsService.summarize; 2 numeric assertions + null-weight test).
- See: `.planning/phases/16-sessions-analytics/16-04-SUMMARY.md` for the pr-durability plan deliverable (V27 migration is_pr column + window-function backfill; SessionSet.isPr primitive field; mapper collapse to single arg form; service.add and service.update mutations; new findAllByUserAndExercise repo method; renamed durability test + 2 new update-path tests).
- See: `.planning/phases/17-body-metrics/17-02-SUMMARY.md` for the photo-url-exposure plan deliverable (UpsertBodyMetricRequest 9th component @Size(max = 500) + MetricsService.upsert setter line + round-trip integration test; closes lone column GAP on the API surface).
- See: `.planning/phases/17-body-metrics/17-03-SUMMARY.md` for the time-series-range plan deliverable (BodyMetricRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc derived query, MetricsService.list (UUID, LocalDate, LocalDate) signature with mixed-pair + inverted 400s, MetricsController.list optional from/to @RequestParam, 4 new integration tests; closes ROADMAP Phase 17 "time-series read endpoints" deliverable on the API surface).

**Current focus:** Phase 17 plans 01-03 complete (audit + photo-url-exposure + time-series-range). Next action: `/gsd:plan-phase 17-04` (status-code-split: `MetricsService.upsert` returns `(BodyMetricDto, boolean wasCreated)` wrapper; controller maps 201 on create / 200 on update; `repeatedPostForSameDateUpdatesInsteadOfInserting` flips second-POST assertion from `isCreated()` to `isOk()`; optional `BodyMetricValidator` static-helper absorb if scope budget allows).

## Accumulated Context

### Locked-in Decisions (carried from v0.3)

Full decision log lives in `.planning/milestones/v0.3-ROADMAP.md` "Key Decisions" section. The most operationally relevant ones for v0.4 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).

### Phase 13-14 Findings

- i-1 root cause (top hypothesis): Hibernate's `saveAndFlush(plan)` does not propagate `@UuidGenerator` id assignment to a transient cascade-child in time for `WorkoutPlanMapper.toDayDto` to read it. Fix in `WorkoutDaysService.createDay` and `addItem` is to use explicit `daysRepo.saveAndFlush(day)` (or add `WorkoutDayExerciseRepository` for items).
- i-2 root cause (top hypothesis): `TestAuthHelpers.seed` bypasses `DefaultPlanSeeder`, so the round-trip test exports an empty `plans` array. `plansInserted == 0` is correct given the missing precondition. Fix is test-side, not production.
- i-1 and i-2 do NOT share a root cause. Phase 14 plans two independent change sets.
- Phase 14 confirmed: i-1 fix is service-layer only (`WorkoutDaysService.createDay`/`addItem` use `daysRepo`/`itemsRepo` `saveAndFlush` instead of `plans.saveAndFlush(plan)`); i-2 fix is test-only (added a seed-plan POST before the export GET in `FullExportImportIntegrationTest`). The audit's two-independent-root-causes verdict held.

### Phase 17 Findings (body-metrics audit)

- Metrics package is substantially landed: 6 source files / 309 lines under `metrics/` (`MetricsController`, `MetricsService`, `domain/{BodyMetric, BodyMetricRepository}`, `dto/{BodyMetricDto, UpsertBodyMetricRequest}`) plus 1 test class / 130 lines (6 tests). V6 migration carries every column ProjectBrief lists (no V28 needed). 5 cross-package call sites touch `BodyMetric` directly: 2 writes (`ScaleWebhookController.ingest`, `HealthImportService.apply`) bypass MetricsService validation; 3 reads (`FullExportService.buildMetrics`, `ReminderJob.runWeightNudgeAt`, `FullImportService.replaceMetrics` is wholesale-replace).
- Schema column-drift verdict: `photo_url` is the LONE GAP across the 5 sources (ProjectBrief / V6 / entity / response DTO / upsert request). Schema, entity, and response DTO carry it; the upsert request does not. Closed by Plan 17-02.
- Status-code direction: Direction A (service returns `(BodyMetricDto, boolean wasCreated)` wrapper; controller maps 201 on create / 200 on update). Phase 15-02 `clientSetId` precedent applies; rejects Direction B (POST + PUT-by-id split) because V6 UNIQUE-by-`(user, date)` encodes date-keyed identity by design.
- Time-series direction: Option 1 (extend `GET /api/metrics` with optional `from`/`to` `LocalDate` query params); reuses `idx_body_metrics_user_date`; defers chart-projection endpoint (Option 2) to Phase 28 / Phase 31.
- Validator direction: defer; each writer has different conflict semantics (idempotent-by-date for the scale webhook, skip-on-conflict for health import, wholesale-replace for full import). Static `BodyMetricValidator` helper is a candidate to absorb into Plan 17-04 if scope budget allows; otherwise stays deferred.
- Plan-count: 4 plans (17-01 audit + 17-02 photo-url-exposure + 17-03 time-series-range + 17-04 status-code-split with optional validator absorb). Revised from TBD.

### Phase 16 Findings (sessions-analytics audit)

- Phase 16 endpoint surface is fully implemented today as Phase 15-01 audit Section 5 in-package leaks. 5 Phase 16-owned files / 191 lines under `sessions/` (`ExerciseAnalyticsController`, `ExerciseAnalyticsService`, `PrDetector`, `LastPerformanceDto`, `ProgressPointDto`) plus 4 shared call sites (`SessionSetsService.add` PR-detection at lines 80-82, `SessionSetsService.bestPriorOneRm` at 105-115, `SessionsMapper.toSetDto` overload at 47-65, `SessionSetRepository.findHistoricalByUserAndExercise` at 21-30). Closed across 16-02 (extraction) and 16-04 (mapper-overload collapse + entity-field migration to `SessionSet.isPr`).
- Package-boundary verdict: **extract to `com.workouthub.analytics`**. The package already exists with charts-stats scaffold (`AnalyticsController` 62 lines plus `AnalyticsService` 237 lines, serving `/api/analytics/{volume,one-rm,streak,prs,heatmap}` from pre-GSD work). Grep confirms no cross-package consumer of `PrDetector` outside `SessionSetsService`. Extraction is one-import-add plus a directory move; payoff is consolidating Epley duplication (`sessions/PrDetector.epleyOneRm` vs `analytics/AnalyticsService.epley`) in a Phase 31 follow-up. Closed by 16-02.
- PR durability verdict: **persist `is_pr` via V27 with window-function backfill**. Postgres 16 supports `ROW_NUMBER() OVER (PARTITION BY user_id, exercise_id ORDER BY weight*(1 + reps/30) DESC)` natively; SQL drafted in audit Section 5. Closes the Phase 15-04 create-vs-reread gap; replaces `newPrFlagPresentOnCreateButOmittedOnDetailReread` with a stronger durable-read assertion. Closed by 16-04.
- Perf verdict: **Java-side aggregation acceptable for v0.4**. Power-user estimate ~800 rows fetched per `/progress?limit=10` for 200 sessions x 4 sets; convention matches existing analytics package (`AnalyticsService.weeklyVolume` and `oneRepMax`); SQL pushdown deferred to Phase 31.
- Field-level drift: `ProgressPointDto.estimatedOneRmKg` (Epley projection) was missing despite ROADMAP listing it as a Phase 16 deliverable. Closed by 16-03.
- Plan-count: **4 plans** for Phase 16 (all shipped: 16-01 audit, 16-02 package-extraction, 16-03 epley-projection, 16-04 pr-durability).

### Phase 15 Findings (sessions-core audit)

- All 7 ProjectBrief Phase 4 endpoints are implemented today (no `Missing` rows). 21 files / 965 lines under `sessions/`. 10 endpoints exposed (8 Phase 15 + 2 Phase 16 leaks: `last-performance`, `progress`).
- Phase 14 child-repo `saveAndFlush` lesson is already applied at `SessionSetsService.java:64` (`sets.saveAndFlush(set)`); cascade-id risk that motivated i-1 is mitigated for sessions.
- Offline-first sync contract: fully hardened by Plans 15-02/15-04. (1) clientSetId idempotency key on `POST /sets` (15-02 closed 4a PARTIAL: 200 = replay, 201 = newly created). (2) Auto-numbering at `SessionSetsService.java:55-57` retained for live-online next-set UX; offline drainers MUST send explicit setNumber (15-04 documented 4c FAIL verdict in Javadoc). (3) Typed 409 codes propagated through `ApiError.code` for the four sessions throw sites (15-04 closed 4b PARTIAL: drainer can branch on SESSION_FINISHED vs SET_NUMBER_DUPLICATE without message parsing).
- `heartRateAvgBpm` field (V20 column, entity getter at `WorkoutSession.java:114`) is fed only by Garmin .fit importer at `HealthImportService.java:68`. Plan 15-03 closed the exposure gap: `Short heartRateAvgBpm` is now read-only on `SessionDto` and `SessionSummaryDto`; Section 3 field-level drift and Section 6 FG1 closed.
- Phase 16 in-package leaks (`ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, `PrDetector`, `SessionSetRepository.findHistoricalByUserAndExercise`) stayed in package across all four 15-* plans; Phase 16 owns formalization. `newPr` flag on create response is a Phase 16 leak with UX value; the create-only behavior is now documentation-asserted by 15-04's `newPrFlagPresentOnCreateButOmittedOnDetailReread` test.
- Plan-count: 4 plans (15-01 audit, 15-02 idempotency key, 15-03 heart-rate exposure, 15-04 typed codes + auto-numbering verdict). All shipped.

### Issue-to-Phase Mapping

- i-1, i-2: closed by v0.4 Phase 14 (workouts-hardening) Plans 01 and 02 respectively.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-5, i-6, i-8: closed by v1.0 Phase 40 (framework-majors).

### Roadmap Evolution

- 2026-05-04: v0.4 milestone created with 8 phases (Phase 13-20). Full v1.0 path scoped: v0.5 frontend completion (Phase 21-30), v0.6 operational maturity (Phase 31-37), v1.0 release hardening (Phase 38-44). Total 32 phases to v1.0.
- 2026-05-04: Phase 13 (workouts-audit) shipped (1 plan: 13-01). Audit deliverable at `.planning/phases/13-workouts-audit/13-01-AUDIT.md`.
- 2026-05-04: Phase 14 plan 01 shipped. WorkoutDaysService cascade-id fix lands the explicit child-repo saveAndFlush from audit Section 5; 6 disabled WorkoutDaysIntegrationTest cases re-enabled and a regression assertion added to the green test. CI verification pending.
- 2026-05-04: Phase 14 (workouts-hardening) shipped (2 plans: 14-01 cascade-id fix, 14-02 export round-trip test fix). Closed i-1 and i-2.
- 2026-05-04: Phase 15 plan 01 (sessions-core audit) shipped. Audit deliverable at `.planning/phases/15-sessions-core/15-01-AUDIT.md`. Phase 15 plan-count revised from TBD to 4 plans (1 audit + 3 hardening). Plan 15-02+ scope finalized: 4 coverage tests, 3 contract-finalization items, 2 feature-gap, 5 defer. No new ISSUES.md entries (contract gaps belong to 15-02+ scope, not deferred-issues queue).
- 2026-05-04: Phase 15 plan 02 (clientSetId idempotency key) shipped. V26 migration + entity field + migration test, service fast-path, 200/201 controller mapping, 4 integration tests. Audit Section 4a PARTIAL verdict closed; Sections 4b PARTIAL (typed 409 codes) and 4c FAIL (auto-numbering) remain open for plan 15-04.
- 2026-05-04: Phase 15 plan 03 (heart-rate exposure) shipped. `Short heartRateAvgBpm` appended to `SessionDto` and `SessionSummaryDto` records; `SessionsMapper.toDto`/`toSummary` populate the new field; two integration tests use direct `WorkoutSessionRepository.saveAndFlush` to mimic the Garmin .fit importer write path on detail/active/history endpoints. Audit Section 3 field-level drift and Section 6 FG1 closed; Section 6 C3 coverage gap closed.
- 2026-05-04: Phase 15 plan 04 (typed 409 codes + auto-numbering verdict) shipped. `ApiError` gains optional trailing `String code` (Jackson NON_NULL omits when null - zero regression on existing 4xx/5xx); `ConflictException` two-arg constructor preserves single-arg back-compat. Four sessions/ throw sites carry codes: `SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`. Five existing 409 integration tests assert `$.code`. `SessionSetsService.add` Javadoc documents auto-numbering contract (live-online safe; offline drainers MUST send explicit setNumber) and idempotency contract (clientSetId replay does not re-fire side effects). `newPrFlagPresentOnCreateButOmittedOnDetailReread` documents create-vs-reread newPr flag behavior. Audit Section 4b PARTIAL closed; Section 4c FAIL verdict documented; Section 6 C4 closed. Phase 15 complete (4/4 plans shipped).
- 2026-05-04: Phase 16 plan 01 (sessions-analytics audit) shipped. Audit deliverable at `.planning/phases/16-sessions-analytics/16-01-AUDIT.md` (297 lines). Phase 16 plan-count revised from TBD to 4 plans (1 audit + 3 hardening). Verdicts: package-boundary extract to existing `com.workouthub.analytics` package; PR durability persist `is_pr` via V27 window-function backfill; perf Java-side acceptable for v0.4 with SQL pushdown deferred to Phase 31. Plan 16-02 (package-extraction), 16-03 (epley-projection), 16-04 (pr-durability) scope finalized with file-level entries. Adjacent finding: `com.workouthub.analytics` package already exists with pre-GSD charts-stats scaffold (Epley re-implemented); deferred Epley consolidation marker for Phase 31.
- 2026-05-04: Phase 16 plan 02 (package-extraction) shipped. 8 files renamed from `com.workouthub.sessions` (3 source root, 2 dto, 3 test) to `com.workouthub.analytics` (rename similarity 71-99 percent under git rename detection). `SessionSetsService` touched with a single new `import com.workouthub.analytics.PrDetector;` line (call sites at lines 83 and 111 unchanged; resolution via the new import). Cross-package patterns established: `analytics/ExerciseAnalyticsService` consumes `sessions/SessionsMapper.toSetDto` via public-static call; `analytics/dto/LastPerformanceDto` consumes `sessions/dto/SessionSetDto`. Public API paths (`/api/exercises/{id}/last-performance`, `/api/exercises/{id}/progress`) byte-for-byte identical (controller `@RequestMapping` unchanged). Local mvn verification gate skipped (this host has no `mvnw` script and no system `mvn` per local-maven-gap memory; full mvn test gate delegated to CI). `.planning/codebase/ARCHITECTURE.md:54` PR-detector path updated to track the move. Phase 15-01 audit Section 5 in-package leaks (5 files / 191 lines) closed. Audit Section 6 PE1 deliverable realized exactly: 5+3 moves, 1 single-line edit. Epley consolidation marker for Phase 31 stays open (both Epley implementations now live in the same package, simplifying the future work).
- 2026-05-05: Phase 16 plan 03 (epley-projection) shipped. `ProgressPointDto` extended from 6 to 7 record components (`estimatedOneRmKg` appended as `BigDecimal`; existing components keep position so JSON name-keyed clients are unaffected). `ExerciseAnalyticsService.summarize` adds an independent reduction `setsInSession.stream().map(s -> PrDetector.epleyOneRm(s.getWeightKg(), s.getRepsDone())).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null)`, passed as the 7th positional argument to the `new ProgressPointDto(...)` call. Per-top-set semantic chosen and documented inline (vs `epleyOneRm(maxWeight, topReps)` shortcut, which mixes sets). Two hard-coded numeric assertions added to `progressReturnsPerSessionSummariesNewestFirst` (77.000 for s2 = Epley(55, 12); 66.667 for s1 = Epley(50, 10)) lock the `PrDetector.setScale(3, HALF_UP)` contract. New integration test `progressEstimatedOneRmIsNullForUnweightedSets` covers the null-weight path end-to-end via new `addUnweightedSet` helper that omits `weightKg` from the POST body; null assertions use `doesNotExist` because `JacksonConfig.java:18` enables `Include.NON_NULL` globally. Test method count grew 7 -> 8. Audit Section 6 EP1 marker closed; ROADMAP "1RM (Epley) projections at the query layer" deliverable closed. Local mvn verification gate skipped per host gap, delegated to CI. Total duration ~3 min. No new ISSUES.md entries.
- 2026-05-05: Phase 16 plan 04 (pr-durability) shipped. V27 migration adds `is_pr BOOLEAN NOT NULL DEFAULT false` to `session_sets` plus a single ROW_NUMBER CTE backfill that marks the highest-Epley completed set per `(user_id, exercise_id)` with `created_at ASC` tie-break (matches `PrDetector.beatsPriorBest` strict-greater). `SessionSet` gains primitive `boolean isPr` with `@Column(name = "is_pr", nullable = false)`. `SessionsMapper` collapses two `toSetDto` overloads into a single `toSetDto(SessionSet)` reading `set.isPr()` and emitting `Boolean newPr` (null when false; Jackson `NON_NULL` strips for non-PR sets). `SessionSetsService.add` mutates `set.setPr(isPr)` before save; idempotent-replay path returns the durable flag automatically via the entity-read mapper. `SessionSetsService.update` calls a new private `recomputePrForExerciseHistory(userId, exerciseId)` helper that scans every `(user, exercise)` set, picks the highest-Epley completed row via `Comparator.nullsFirst`-guarded `max`, and writes `is_pr=true` on that single row while clearing all others. New repo method `findAllByUserAndExercise` (no `endedAt` filter) used because PRs span finished AND active sessions. `SessionSetsIntegrationTest.newPrFlagPresentOnCreateButOmittedOnDetailReread` renamed to `...AndOnDetailReread`; final assertion inverted from `doesNotExist()` to `value(true)`. `PrDetectionIntegrationTest.setThatBeatsPriorFinishedBestIsFlaggedPr` extended with detail-reread assertion; 2 new tests `prSurvivesUpdateThatStillBeatsPrior` (Epley(110,6)=132 still beats prior 116.667) and `prClearedWhenUpdatedBelowPriorAndPromotesNextBest` (PUT to 1 rep @ 50kg demotes s2 set, re-elects s1; asserts both demotion and promotion in one flow). Wire field name `newPr` retained on `SessionSetDto` for back-compat; rename to `isPr` deferred to Phase 25. V27 migration test self-verifies by re-running the production backfill SQL post-seed. Audit Section 6 PD1 + PD2 + PD3 markers all closed. Phase 16 100% complete (4/4 plans shipped). Local mvn verification gate skipped per host gap, delegated to CI. Total duration ~4 min. No new ISSUES.md entries.
- 2026-05-05: Phase 17 plan 01 (body-metrics audit) shipped. Audit deliverable at `.planning/phases/17-body-metrics/17-01-AUDIT.md` (325 lines). Phase 17 plan-count revised from TBD to 4 plans (1 audit + 3 hardening). Verdicts: status-code Direction A (service returns `(dto, wasCreated)` wrapper; controller maps 201/200); time-series Option 1 (extend `GET /api/metrics` with optional `from`/`to` query params); validator-extraction deferred (5 cross-package call sites have heterogeneous conflict semantics); plan-count 4. `photo_url` is the lone column GAP across ProjectBrief / V6 / entity / response DTO / upsert request. Plan 17-02 (photo-url-exposure), 17-03 (time-series-range), 17-04 (status-code-split with optional validator absorb) scope finalized with file-level entries. No Java touched.
- 2026-05-05: Phase 17 plan 02 (photo-url-exposure) shipped. `UpsertBodyMetricRequest` extended from 8 to 9 components with `@Size(max = 500) String photoUrl` appended (mirrors V6 `photo_url VARCHAR(500)`); existing 8 components keep position so JSON name-keyed clients are unaffected. `MetricsService.upsert` writes `entity.setPhotoUrl(req.photoUrl())` between `setThighCm` and `setNotes`, matching V6 column order. New integration test `MetricsIntegrationTest.postWithPhotoUrlIsRoundTripped` POSTs `{"recordedDate":"2026-04-25","weightKg":78.0,"photoUrl":"https://example.test/photos/abc.jpg"}`, asserts `$.photoUrl` echo on create + `$[0].photoUrl` on subsequent GET. Test method count 6 to 7. No migration; no entity change; no controller signature change; `BodyMetricDto` and `MetricsService.toDto` were already wired for `photoUrl` pre-plan. 17-01-AUDIT Section 3 Part A `photo_url` GAP row resolves to OK across all 5 sources. ROADMAP Phase 17 deliverable "optional progress photo URL" closed on the API surface. URL-syntax validation (multipart/MinIO/signed URLs) deferred to Phase 28 upload-pipeline plan. mvn verify gate delegated to CI per local-maven-gap. Total duration ~2 min. No new ISSUES.md entries.
- 2026-05-05: Phase 17 plan 03 (time-series-range) shipped. `BodyMetricRepository.findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc(UUID, LocalDate, LocalDate)` appended below `findByIdAndUserId`; reuses `idx_body_metrics_user_date` per V6 (Spring Data `Between` is inclusive on both bounds). `MetricsService.list` signature replaced from `(UUID)` to `(UUID, LocalDate, LocalDate)`; branches full-history (both null), range (both set), `400 "from and to must be provided together"` on `fromSet ^ toSet`, and `400 "from must not be after to"` on `from.isAfter(to)`; mixed-pair check ordered before inverted check to avoid NPE on the inverted check; both 400s thrown via `ResponseStatusException(HttpStatus.BAD_REQUEST, ...)` matching `PasswordResetService` and `FullImportService` precedent. `MetricsController.list` exposes `@RequestParam(required = false) LocalDate from` and `to`; ISO-8601 `yyyy-MM-dd` binding via Spring Boot defaults; invalid date strings flow to existing `GlobalExceptionHandler.handleTypeMismatch` 400. 4 new integration tests appended after `postWithPhotoUrlIsRoundTripped`: `rangeFilterReturnsOnlyMatchingRows` (3 metrics on 2026-03-01/15/31, window 2026-03-10..20 returns the middle row), `rangeFilterIsInclusiveOnBothBounds` (locks `Between` inclusive contract on 2026-04-01 and 2026-04-30), `rangeWithNoMatchingRowsReturnsEmptyArray` (empty-result path), `rangeMixedOrInvertedReturns400` (3 `mvc.perform` blocks: partial-from, partial-to, inverted). Private `seedMetric(auth, recordedDate, weightKg)` helper consolidates POST setup. Test method count 7 to 11. No migration; no entity change; no DTO change; no new endpoint - Option 1 verdict from 17-01-AUDIT Section 5 Part B realized exactly. ROADMAP Phase 17 deliverable "time-series read endpoints for the metrics UI" closed on the API surface. ProjectBrief "Kilo grafigi (haftalik/aylik/tum zamanlar)" lifts from Partial on the API surface (server-side range read present; weekly/monthly bucketing lives in Phase 28 metrics-ui). mvn verify gate delegated to CI per local-maven-gap. Total duration ~1 min. No new ISSUES.md entries.

## Session Continuity

Last session: 2026-05-05 - Phase 17 plan 03 (time-series-range) complete; Phase 17 in progress (3/4 plans shipped)
Stopped at: Phase 17 plan 03 complete; next action: `/gsd:plan-phase 17-04` (status-code-split: 201 on create / 200 on update via wasCreated wrapper; flip repeatedPostForSameDateUpdatesInsteadOfInserting second-POST assertion).
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/ROADMAP.md` (current, v0.4 detailed; future milestones outlined)
- `.planning/ISSUES.md` (open deferred issues)
- `.planning/phases/13-workouts-audit/13-01-AUDIT.md` (workouts audit deliverable)
- `.planning/phases/17-body-metrics/17-01-AUDIT.md` (body-metrics audit deliverable; 325 lines, 6 sections; status-code, time-series, validator, plan-count verdicts)
- `.planning/HANDOFF.md` (pre-GSD v0.2 snapshot)
- `.planning/codebase/STACK.md`
- `.planning/codebase/ARCHITECTURE.md`
- `.planning/codebase/STRUCTURE.md`
- `.planning/codebase/CONVENTIONS.md`
- `.planning/codebase/TESTING.md`
- `.planning/codebase/INTEGRATIONS.md`
- `.planning/codebase/CONCERNS.md`
- `docs/SELF_HOSTED_CONTRACT.md` (binding operational contract)
- `ProjectBrief.md` (original phase plan)
