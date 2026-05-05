---
phase: 16-sessions-analytics
plan: 04
subsystem: sessions, analytics, database
tags: [postgres, flyway, hibernate, jpa, jpql, window-functions, integration-test, junit5, mockmvc]

# Dependency graph
requires:
  - phase: 15-sessions-core
    provides: clientSetId idempotency, typed 409 codes, heart-rate exposure on session DTOs, newPr create-only flag (now superseded)
  - phase: 16-01-audit
    provides: Section 5 PR-durability verdict (Direction A), Section 6 PD1+PD2+PD3 markers, V27 SQL draft
  - phase: 16-02-package-extraction
    provides: com.workouthub.analytics.PrDetector at canonical location consumed by SessionSetsService
  - phase: 16-03-epley-projection
    provides: ProgressPointDto.estimatedOneRmKg precedent (entity-derived analytics field on a DTO)
provides:
  - V27 migration adding is_pr BOOLEAN NOT NULL DEFAULT false on session_sets with window-function backfill
  - SessionSet.isPr field with @Column(name="is_pr", nullable=false) primitive boolean
  - SessionsMapper.toSetDto single-arg form reading set.isPr() and emitting Boolean newPr (null when false)
  - SessionSetsService.add mutates entity isPr before save; idempotent replay returns durable flag automatically
  - SessionSetsService.update calls recomputePrForExerciseHistory(userId, exerciseId) after patch, demoting and re-electing the durable PR row
  - SessionSetRepository.findAllByUserAndExercise (no endedAt filter) for active+finished history scan
  - Renamed durability test: newPrFlagPresentOnCreateAndOnDetailReread (assertion inverted from doesNotExist to value(true))
  - 2 new update-path tests: prSurvivesUpdateThatStillBeatsPrior, prClearedWhenUpdatedBelowPriorAndPromotesNextBest
affects: [phase 17 body-metrics (no overlap, sequencing only), phase 25 frontend session-execution (rename newPr->isPr deferred), phase 31 charts-stats personalRecords (single indexed lookup instead of full-history scan)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PR durability via persisted is_pr column with window-function backfill on (user_id, exercise_id) partition; tie-break on created_at ASC"
    - "Demote-and-re-elect helper on update path: O(n) scan over (user, exercise) history with nullsFirst comparator for unweighted sets"
    - "Migration test self-verification: re-run the production backfill SQL inside the test to assert correctness against post-migration seeded fixtures"
    - "Single-arg mapper reads from entity (Boolean wire field stays Boolean, NON_NULL strips false-equivalent absent values)"

key-files:
  created:
    - backend/src/main/resources/db/migration/V27__session_sets_is_pr.sql
    - backend/src/test/java/com/workouthub/migrations/V27SessionSetsIsPrMigrationTest.java
  modified:
    - backend/src/main/java/com/workouthub/sessions/domain/SessionSet.java
    - backend/src/main/java/com/workouthub/sessions/domain/SessionSetRepository.java
    - backend/src/main/java/com/workouthub/sessions/SessionsMapper.java
    - backend/src/main/java/com/workouthub/sessions/SessionSetsService.java
    - backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java
    - backend/src/test/java/com/workouthub/analytics/PrDetectionIntegrationTest.java

key-decisions:
  - "Persist is_pr (Direction A) per audit Section 5 verdict; rejected enrich-progress-DTO (Direction B)"
  - "Demote-and-re-elect on update (full O(n) scan helper) rejected lazy-correction to avoid shipping a known-stale flag"
  - "Wire field name newPr retained on SessionSetDto for back-compat with frontend session-client; rename to isPr deferred to Phase 25"
  - "Backfill tie-break on created_at ASC (earliest set wins ties) matches PrDetector.beatsPriorBest strict-greater"
  - "New repo method findAllByUserAndExercise (includes active sessions) over reusing findHistoricalByUserAndExercise (finished-only) so PRs span in-progress data"
  - "No partial unique index on (user, exercise, is_pr) at schema level; uniqueness enforced by application logic to permit transient demote-then-promote inside a single transaction"
  - "Inline Epley arithmetic in backfill SQL (no Postgres UDF) matches existing migration style and analytics/AnalyticsService.epley semantics"

patterns-established:
  - "Pattern: Migration test re-runs production SQL post-seed when V<n> ran against an empty table at app start (V27 migration test)"
  - "Pattern: nullsFirst comparator with max() to filter unweighted sets without an explicit Optional.filter (recomputePrForExerciseHistory)"
  - "Pattern: Service holds the (user, exercise) PR uniqueness invariant; schema permits transient violation inside a single @Transactional boundary"

issues-created: []

# Metrics
duration: 4 min
completed: 2026-05-05
---

# Phase 16 Plan 04: PR Durability Summary

**Persisted `is_pr` on `session_sets` via V27 with window-function backfill; entity field plus mapper plus service mutation on create and update paths; renamed durability test from create-only to create-and-reread; 2 new update-path tests cover demotion-and-re-election.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-05T07:36:31Z
- **Completed:** 2026-05-05T07:40:27Z
- **Tasks:** 3
- **Files modified:** 8 (2 created, 6 modified)

## Accomplishments

- V27 migration adds `is_pr BOOLEAN NOT NULL DEFAULT false` and backfills the highest-Epley completed set per `(user_id, exercise_id)` via single ROW_NUMBER CTE; tie-break on `created_at ASC` matches `PrDetector.beatsPriorBest` strict-greater semantics.
- `SessionSet` entity gains primitive `boolean isPr` field; `SessionsMapper` collapses to a single `toSetDto(SessionSet)` reading from the entity; wire field stays `Boolean newPr` so Jackson `NON_NULL` strips absent values for non-PR sets.
- `SessionSetsService.add` mutates `set.setPr(isPr)` before save; idempotent-replay path returns the durable flag automatically via the entity-read mapper.
- `SessionSetsService.update` calls `recomputePrForExerciseHistory(userId, exerciseId)` after applying the patch; helper does O(n) demote-and-re-elect on the (user, exercise) set history with `nullsFirst` comparator guarding unweighted sets.
- New repo method `findAllByUserAndExercise` includes active sessions because PRs span both finished and in-progress data; `findHistoricalByUserAndExercise` retained for `bestPriorOneRm` (different invariant).
- Renamed `newPrFlagPresentOnCreateButOmittedOnDetailReread` to `newPrFlagPresentOnCreateAndOnDetailReread`; final assertion inverted from `doesNotExist()` to `value(true)`. Encodes the durability inversion in `git log`.
- `setThatBeatsPriorFinishedBestIsFlaggedPr` extended with detail-reread `value(true)` assertion on session 2.
- 2 new tests: `prSurvivesUpdateThatStillBeatsPrior` (PR retained on patch that still beats prior); `prClearedWhenUpdatedBelowPriorAndPromotesNextBest` (PUT to 1 rep @ 50kg demotes s2 set, re-elects s1 as PR, asserts both demotion AND promotion in one flow).
- Wire field name `newPr` retained on `SessionSetDto` for back-compat; rename to `isPr` deferred to Phase 25 v0.5 frontend session-execution.

## Task Commits

Each task was committed atomically:

1. **Task 1: V27 migration + migration test** - `3705d55` (feat)
2. **Task 2: Entity field, mapper read-from-entity, service.add mutation, durability test rewrite** - `58ffc7e` (feat)
3. **Task 3: Update-path re-evaluation + 2 update-path tests** - `5bb18d1` (feat)

**Plan metadata:** _(this commit)_

## Files Created/Modified

- `backend/src/main/resources/db/migration/V27__session_sets_is_pr.sql` - V27 ALTER TABLE + window-function backfill marking the highest-Epley completed set per (user, exercise) pair
- `backend/src/test/java/com/workouthub/migrations/V27SessionSetsIsPrMigrationTest.java` - 3 tests: column shape (bool, NOT NULL, default false), default-false on raw INSERT, backfill correctness (re-runs SQL post-seed)
- `backend/src/main/java/com/workouthub/sessions/domain/SessionSet.java` - Added primitive `boolean isPr` field with `@Column(name = "is_pr", nullable = false)`, default `false`, getter `isPr()`, setter `setPr(boolean)`
- `backend/src/main/java/com/workouthub/sessions/domain/SessionSetRepository.java` - Added `findAllByUserAndExercise` JPQL (no endedAt filter, includes active sessions)
- `backend/src/main/java/com/workouthub/sessions/SessionsMapper.java` - Collapsed two `toSetDto` overloads into single `toSetDto(SessionSet)` reading `set.isPr()`; emits `Boolean newPr` (null when false) so JSON strips absent values
- `backend/src/main/java/com/workouthub/sessions/SessionSetsService.java` - `add` mutates `set.setPr(isPr)` before save; `update` calls `recomputePrForExerciseHistory` after patch; new private helper does O(n) demote-and-re-elect with nullsFirst comparator
- `backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java` - Renamed durability test; final assertion inverted from `doesNotExist()` to `value(true)`
- `backend/src/test/java/com/workouthub/analytics/PrDetectionIntegrationTest.java` - Extended PR-beats-prior test with detail-reread assertion; added 2 update-path tests; new `addSet` helper returns set id

## Decisions Made

- Persist (Direction A) over enrich-progress-DTO (Direction B) per audit Section 5 verdict.
- Demote-and-re-elect on update (full O(n) scan helper) over lazy-correction (transient stale flag).
- Wire field name preserved (`newPr`); rename deferred to Phase 25.
- Backfill tie-break on `created_at ASC` (earliest set wins ties; matches `beatsPriorBest` strict-greater).
- New repo method `findAllByUserAndExercise` (includes active sessions) over reusing `findHistoricalByUserAndExercise` (finished-only) — active sets ARE eligible PRs because the user has already performed them.
- No schema-level partial unique index on `(user, exercise) WHERE is_pr` — application layer is sole writer and guarantees the invariant; permitting transient violation inside `@Transactional` simplifies the demote-then-promote helper.

## Deviations from Plan

None - plan executed exactly as written. Local mvn verification gate skipped per `local-maven-gap` memory; CI is the binding test gate (matches 16-02 and 16-03 precedent).

## Issues Encountered

None.

## Next Phase Readiness

- Phase 16 complete (4/4 plans shipped). 100% milestone progress on this phase.
- Phase 16-01 audit Section 6 PD1 + PD2 + PD3 markers all closed; D1 (SQL pushdown), D2 (Epley consolidation), D3 (frontend integration), D4 (PR badge per-set policy refinements), D5 (NotFoundException codes) remain open as documented deferrals.
- Phase 17 (body-metrics) is next — different package (`metrics/`), no overlap with sessions/analytics.
- Frontend follow-up at Phase 25: rename wire field `newPr` to `isPr` in `frontend/src/lib/api/schemas.ts`, `frontend/src/app/(app)/session/[id]/session-client.tsx`, and any test consumers; semantic is already aligned (durable not flash) but field name still encodes the legacy "new" semantics.
- CI must run V27SessionSetsIsPrMigrationTest, the renamed durability test, and all 5 PrDetectionIntegrationTest tests green before declaring phase 16 done.

---
*Phase: 16-sessions-analytics*
*Completed: 2026-05-05*
