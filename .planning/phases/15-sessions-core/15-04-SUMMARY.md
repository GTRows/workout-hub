---
phase: 15-sessions-core
plan: 04
subsystem: sessions
tags: [api-error, conflict, error-codes, javadoc, integration-test, contract-finalization]

requires:
  - phase: 15-sessions-core
    plan: 01
    reason: audit Section 4b PARTIAL (typed 409 codes) and Section 4c FAIL (auto-numbering verdict) and Section 6 C4 (newPr re-read coverage) all closed here
  - phase: 15-sessions-core
    plan: 02
    reason: outOfOrderDrainPreservesSetNumberOrdering proves the explicit-number drain path that this plan's Javadoc points to as the offline contract

provides:
  - optional String code component on ApiError envelope (Jackson NON_NULL omits when null; legacy 4xx/5xx responses unchanged)
  - two-arg ConflictException(code, message) constructor with single-arg backward-compat constructor preserved
  - GlobalExceptionHandler propagates ConflictException.code() into 409 response body
  - four typed sessions/ conflict codes: SESSION_ALREADY_ACTIVE, SESSION_ALREADY_FINISHED, SESSION_FINISHED, SET_NUMBER_DUPLICATE
  - SessionSetsService.add Javadoc documenting auto-numbering and idempotency contracts (binding written-down rule for offline drainers)
  - newPrFlagPresentOnCreateButOmittedOnDetailReread integration test documenting the create-vs-reread newPr flag behavior

affects:
  - 16-sessions-analytics (PR/analytics surface should not regress the create-only newPr flag pattern)
  - 19-api-contract-docs (OpenAPI surface for ApiError.code; per-code documentation for the four sessions codes; per-feature code adoption guidance)
  - any future feature adopting typed conflict codes (auth/, users/, workouts/, etc.) - infrastructure already in place

tech-stack:
  added: []
  patterns:
    - optional trailing record component on a shared envelope, append-only, Jackson NON_NULL for back-compat
    - per-throw-site String constants for error codes (no central enum; SCREAMING_SNAKE_CASE ASCII)
    - Javadoc as the binding contract artifact for documented-only verdicts

key-files:
  created:
    - .planning/phases/15-sessions-core/15-04-SUMMARY.md
  modified:
    - backend/src/main/java/com/workouthub/common/web/ApiError.java
    - backend/src/main/java/com/workouthub/common/web/ConflictException.java
    - backend/src/main/java/com/workouthub/common/web/GlobalExceptionHandler.java
    - backend/src/main/java/com/workouthub/sessions/SessionsService.java
    - backend/src/main/java/com/workouthub/sessions/SessionSetsService.java
    - backend/src/test/java/com/workouthub/common/GlobalExceptionHandlerTest.java
    - backend/src/test/java/com/workouthub/sessions/SessionLifecycleIntegrationTest.java
    - backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java

key-decisions:
  - "Optional vs required code on ApiError: Optional. Required would cascade an edit through every existing 4xx/5xx producer; this plan is conflict-only by design. Jackson NON_NULL keeps legacy responses byte-for-byte unchanged."
  - "String constants vs enum for codes: Per-throw-site private static final String constants. An enum would force a central registry every feature must import; literal strings keep adoption local and decoupled."
  - "NotFoundException codes: Out of scope. Audit's typed-code requirement is conflict-only (drainer disambiguation). Adding NotFoundException codes would extend scope without a concrete consumer."
  - "Auto-numbering fallback (CF2 verdict): Keep + document. Removing the fallback would break the live-online next-set UX. Offline path is already proven safe with explicit numbers (15-02's outOfOrderDrainPreservesSetNumberOrdering)."
  - "C4 test seeding: Fresh user, no historical session. PrDetector.beatsPriorBest(null, w, r) returns true on the first completed set; extra seeding would couple the test to history-query internals."

duration: 3 min
completed: 2026-05-04
---

# Phase 15 Plan 04: typed 409 codes + auto-numbering verdict Summary

Optional `code` field on `ApiError`, four sessions/ 409 throw sites carry typed codes, auto-numbering contract documented in Javadoc, `newPr` re-read documentation test landed.

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-04T13:40:19Z
- **Completed:** 2026-05-04T13:42:54Z
- **Tasks:** 3
- **Files modified:** 8 (3 common/web sources, 2 sessions sources, 1 common test, 2 sessions tests)

## Accomplishments

- `ApiError` gains optional trailing `String code` component; Jackson `@JsonInclude(NON_NULL)` (global config) omits it from JSON when null, preserving every existing 4xx/5xx response shape.
- `ConflictException` accepts an optional code via a new two-arg constructor; the single-arg constructor is preserved by delegating, so auth/, workouts/, and every other thrower keep working unchanged.
- `GlobalExceptionHandler.handleConflict` reads `ex.code()` and threads it into the 409 body via the extended `build()` helper signature; every other handler passes `null`.
- Four sessions/ 409 paths now emit machine-readable codes: `SESSION_ALREADY_ACTIVE` (start while active), `SESSION_ALREADY_FINISHED` (finish a finished session), `SESSION_FINISHED` (POST/PUT/DELETE on a finished session), `SET_NUMBER_DUPLICATE` (V5 unique violation on `(session_id, exercise_id, set_number)`).
- Five existing 409 integration-test assertions across `SessionLifecycleIntegrationTest` and `SessionSetsIntegrationTest` extended with `$.code` value checks, and `GlobalExceptionHandlerTest` covers both null-code (back-compat) and explicit-code (new contract) paths.
- `SessionSetsService.add` carries a Javadoc block stating the auto-numbering contract (live-online safe; offline drainers MUST send explicit `setNumber`) and the idempotency contract (clientSetId replay does not re-fire PR detector or achievements). The fallback at lines 55-57 is unchanged - the Javadoc is the binding written-down rule.
- `newPrFlagPresentOnCreateButOmittedOnDetailReread` documents that `$.newPr=true` lands on the create response and is absent on the detail re-read (Jackson NON_NULL on the `newPr` arg passed by `SessionsMapper.toSetDto(set, isPr ? TRUE : null)` on create vs `toSetDto(set)` on detail).
- IndexedDB drainer's branch table is now unambiguous: 200 = persisted, 201 = newly created, 409 + `SET_NUMBER_DUPLICATE` = renumbering bug (bubble up), 409 + `SESSION_FINISHED` = drop the queued row, 409 + `SESSION_ALREADY_ACTIVE`/`SESSION_ALREADY_FINISHED` = drop the queued lifecycle command and refresh state.
- Audit Section 4b PARTIAL closed; Section 4c FAIL verdict documented (no behavior change; Javadoc is the contract); Section 6 C4 closed.
- Phase 15 (sessions-core) complete: 4/4 plans shipped (15-01 audit, 15-02 idempotency key, 15-03 heart-rate exposure, 15-04 typed codes + auto-numbering verdict).

## Task Commits

1. **Task 1: ApiError + ConflictException + GlobalExceptionHandler infra + GlobalExceptionHandlerTest** - `f3cdc8c` (feat)
2. **Task 2: Sessions/ throw sites carry codes + integration tests assert codes** - `ffe16e6` (feat)
3. **Task 3: Auto-numbering Javadoc contract + newPr re-read documentation test** - `e3000ec` (docs)

**Plan metadata:** (this commit) (docs: complete plan)

## Files Created/Modified

- `backend/src/main/java/com/workouthub/common/web/ApiError.java` - Added optional trailing `String code` component on the record.
- `backend/src/main/java/com/workouthub/common/web/ConflictException.java` - Added private final code field, two-arg constructor, and `code()` accessor; preserved single-arg constructor via delegation.
- `backend/src/main/java/com/workouthub/common/web/GlobalExceptionHandler.java` - Extended `build()` helper with trailing `String code` parameter; `handleConflict` reads `ex.code()`; every other handler passes `null`.
- `backend/src/main/java/com/workouthub/sessions/SessionsService.java` - Added three `CODE_*` constants and applied them at the three `ConflictException` throw sites (start, finish, findActiveOwnedOrThrow).
- `backend/src/main/java/com/workouthub/sessions/SessionSetsService.java` - Added `CODE_SET_NUMBER_DUPLICATE` constant and applied at the V5 unique-violation throw site; added Javadoc on `add` documenting auto-numbering and idempotency contracts.
- `backend/src/test/java/com/workouthub/common/GlobalExceptionHandlerTest.java` - Extended `conflictMapsTo409` to assert `code()` is null; added `conflictWithCodePropagatesCodeToApiError` for the new constructor.
- `backend/src/test/java/com/workouthub/sessions/SessionLifecycleIntegrationTest.java` - Asserted `$.code` value on the two 409 expectations in `startActiveFinishFlow`.
- `backend/src/test/java/com/workouthub/sessions/SessionSetsIntegrationTest.java` - Asserted `$.code` on three existing 409 tests (`duplicateExplicitSetNumberReturns409`, `updateAfterFinishReturns409`, `differentClientSetIdSameSetNumberReturns409`); added `newPrFlagPresentOnCreateButOmittedOnDetailReread` test.

## Decisions Made

- **Optional vs required `code` on `ApiError`:** Optional. Required would cascade an edit through every existing 4xx/5xx producer; this plan is conflict-only by design. Jackson NON_NULL keeps legacy responses byte-for-byte unchanged.
- **String constants vs enum for codes:** Per-throw-site `private static final String` constants. An enum would force a central registry every feature must import; literal strings keep adoption local and decoupled.
- **`NotFoundException` codes:** Out of scope. Audit's typed-code requirement is conflict-only (drainer disambiguation).
- **Auto-numbering fallback (CF2 verdict):** Keep + document. Removing it would break the live-online next-set UX; offline path is already proven safe with explicit numbers.
- **C4 test seeding strategy:** Fresh user, no historical session. `PrDetector.beatsPriorBest(null, w, r)` returns true on the first completed set; extra seeding would couple the test to history-query internals.

## Deviations from Plan

None - plan executed exactly as written. All eight files modified match the plan's verification checklist; zero files created beyond `15-04-SUMMARY.md`. No `PrDetector`, `ExerciseAnalyticsController/Service`, `LastPerformanceDto`, `ProgressPointDto`, or `SessionSetRepository.findHistoricalByUserAndExercise` modifications (Phase 16 leak guard upheld). No Flyway migration changes (immutability rule upheld).

## Issues Encountered

None. Local Maven is not available on this Windows host (per memory `local_maven_gap.md`); test execution deferred to CI as has been the project convention since Phase 14.

## Next Phase Readiness

- Phase 15 complete (4/4 plans shipped). v0.4 advances to Phase 16 (sessions-analytics) per ROADMAP.md.
- Phase 19 (api-contract-docs) inherits the OpenAPI surface for the new `ApiError.code` field and the four sessions code values.
- Other features (auth/, users/, workouts/, etc.) can adopt typed conflict codes incrementally without further infrastructure work; the two-arg constructor is the single adoption point.
- CI (GitHub Actions backend job) will run `./mvnw -pl backend verify` on the next push to validate compile and the eight modified tests; line coverage gate (>= 70%) expected to hold since the changes are surface-level (constants + assertions + Javadoc + one new test).

---
*Phase: 15-sessions-core*
*Completed: 2026-05-04*
