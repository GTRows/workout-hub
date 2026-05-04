---
phase: 15-sessions-core
plan: 02
subsystem: api
tags: [idempotency, flyway, postgres, jpa, mockmvc, testcontainers]

requires:
  - phase: 15-sessions-core
    plan: 01
    reason: audit Section 4a identified 409-conflation gap that this plan closes
  - phase: 14-workouts-hardening
    plan: 01
    reason: established saveAndFlush-after-addChild pattern already applied at SessionSetsService:64

provides:
  - idempotency-key contract on POST /sessions/{sessionId}/sets (clientSetId UUID)
  - partial unique index pattern for opt-in keys on session_sets
  - AddSetResult outcome record for HTTP status disambiguation (200 vs 201)
  - 200/201 controller status-mapping pattern for idempotent vs fresh insert
affects:
  - 15-sessions-core/15-04 (typed 409 codes will reuse the status-mapping pattern)
  - v0.5 Phase 25 session-execution frontend (drainer wires to 200=idempotent success / 201=created / 409=true collision)

tech-stack:
  added: []
  patterns:
    - idempotency-key fast-path (check before insert, return persisted row without re-firing side effects)
    - partial unique index for opt-in keys (WHERE client_set_id IS NOT NULL)
    - service result record (AddSetResult) for HTTP status disambiguation

key-files:
  created:
    - backend/src/main/resources/db/migration/V26__session_sets_client_id.sql
    - backend/src/test/java/com/workouthub/migrations/V26SessionSetsClientIdMigrationTest.java
    - .planning/phases/15-sessions-core/15-02-SUMMARY.md
  modified:
    - backend/src/main/java/com/workouthub/sessions/domain/SessionSet.java
    - backend/src/main/java/com/workouthub/sessions/dto/AddSetRequest.java
    - backend/src/main/java/com/workouthub/sessions/domain/SessionSetRepository.java
    - backend/src/main/java/com/workouthub/sessions/SessionSetsService.java
    - backend/src/main/java/com/workouthub/sessions/SessionSetsController.java
    - backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java

key-decisions:
  - "Idempotency-key contract: same key wins over body diff. Replay returns the original persisted row, not a 409 and not a re-insert. Body diffing is out of scope (Phase 19 at earliest)."
  - "newPr on idempotent hit: must be null. The PR moment fired on the first call; a replay is not a new event and must not re-fire PrDetector."
  - "achievements.onSetSaved on idempotent hit: must NOT fire. Achievements are event-sourced from first insert; a replay would duplicate streak counts."
  - "Fast-path placement: AFTER findActiveOwnedOrThrow and exercises.findById, so 404 and 409-on-finished still fire correctly even on an idempotent replay attempt."
  - "clientSetId opt-in: no server-side UUID assigned when field is absent. Auto-filling would break the replay contract (client could not construct the lookup key)."

duration: 18 min
completed: 2026-05-04
---

# Phase 15 Plan 02: contract-finalization (clientSetId idempotency key) Summary

**Idempotency-key contract on POST /sets: V26 partial UNIQUE on (session_id, client_set_id), service fast-path returns 200 on replay without re-firing PR or achievements, four new integration tests cover replay/body-diff-replay/collision/ordering.**

## Performance

- **Duration:** 18 min
- **Started:** 2026-05-04
- **Completed:** 2026-05-04
- **Tasks:** 3
- **Files modified:** 8 (2 new migrations/tests, 1 entity, 1 DTO, 1 repository, 1 service, 1 controller, 1 integration test)

## Accomplishments

- V26 migration adds nullable `client_set_id UUID` column to `session_sets` with partial UNIQUE index on `(session_id, client_set_id) WHERE client_set_id IS NOT NULL`.
- `SessionSet` entity gains `clientSetId` field with non-Lombok getter/setter matching codebase style.
- `V26SessionSetsClientIdMigrationTest` asserts column shape (uuid, nullable), partial unique enforcement (duplicate non-null key rejected), and NULL coexistence (multiple NULLs allowed).
- `AddSetRequest` gains optional `UUID clientSetId` last component (no validation annotation; opt-in contract).
- `SessionSetRepository` gains derived query `findBySessionIdAndClientSetId`.
- `SessionSetsService.add` returns `AddSetResult(dto, idempotentHit)`; fast-path before insert returns persisted row without re-firing `PrDetector` or `achievements.onSetSaved`.
- `SessionSetsController` maps `idempotentHit=true` to 200, fresh insert to 201.
- Four new integration tests: replay returns 200 + same id, body-diff replay returns original row, different key same set_number still 409, out-of-order drain GET returns sets sorted by set_number.
- Audit Section 4a PARTIAL verdict closed.

## Task Commits

1. **Task 1: V26 migration + entity field + migration test** - `7711a23`
2. **Task 2: AddSetRequest + repository + service fast-path + controller mapping** - `de552e1`
3. **Task 3: SessionSetsIntegrationTest four new tests** - `9fa32a1`

## Files Created/Modified

- `backend/src/main/resources/db/migration/V26__session_sets_client_id.sql` - new migration: ALTER TABLE + partial UNIQUE index
- `backend/src/test/java/com/workouthub/migrations/V26SessionSetsClientIdMigrationTest.java` - new migration test: three methods asserting column shape, unique enforcement, NULL coexistence
- `backend/src/main/java/com/workouthub/sessions/domain/SessionSet.java` - added `clientSetId` field with getter/setter
- `backend/src/main/java/com/workouthub/sessions/dto/AddSetRequest.java` - added optional `clientSetId UUID` last component
- `backend/src/main/java/com/workouthub/sessions/domain/SessionSetRepository.java` - added `findBySessionIdAndClientSetId` derived query
- `backend/src/main/java/com/workouthub/sessions/SessionSetsService.java` - added `AddSetResult` record, changed `add` return type, wired fast-path and `set.setClientSetId`
- `backend/src/main/java/com/workouthub/sessions/SessionSetsController.java` - updated `add` to consume `AddSetResult` and map 200/201
- `backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java` - added `get` import, `setBody` overload, four new test methods

## Decisions Made

- **Idempotency-key contract: same key wins over body diff.** Replay returns the original persisted row, not a 409 and not a re-insert. Body diffing is out of scope (Phase 19 at earliest); standard idempotency semantics.
- **`newPr` on idempotent hit: must be null.** The PR moment fired on the first call only. `SessionsMapper.toSetDto(existing)` (no `newPr` arg) correctly returns `null` for `newPr`.
- **`achievements.onSetSaved` on idempotent hit: must NOT fire.** Achievements are event-sourced from first insert; a replay would duplicate streak counts.
- **Fast-path placement: after guard checks.** `findActiveOwnedOrThrow` and `exercises.findById` run before the clientSetId lookup so 404 and 409-on-finished still fire correctly even during an idempotent replay.

## Deviations from Plan

None. Plan executed exactly as written. All three tasks landed in one atomic commit each with exactly the expected file counts (3 / 4 / 1).

## Issues Encountered

None. Local Maven is unavailable on the Windows host; CI verification is the gate for test execution. The `./mvnw test` lines in the plan's `<verify>` blocks were skipped per standing instructions; file-inspection grep checks confirm all changes are structurally correct.

## Next Phase Readiness

- Plan 15-03 (heart-rate field exposure on session DTOs) can proceed independently; no dependency on 15-02 changes.
- Plan 15-04 (auto-numbering verdict and typed 409 codes) can also proceed; the controller status-mapping pattern introduced here (`AddSetResult.idempotentHit`) is the direct precedent for the typed-error-code work.
- Audit Section 4b PARTIAL and 4c FAIL remain open for plan 15-04.

---
*Phase: 15-sessions-core*
*Completed: 2026-05-04*
