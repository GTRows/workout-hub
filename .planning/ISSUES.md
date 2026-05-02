# Issues

Deferred work and known failing tests. Each entry includes the trigger that should reopen it.

## Open

### i-1 — WorkoutDaysIntegrationTest helper NPE on response `id` (6 errors)

**Affected tests** (all via `createDay` helper at line 196):
- `addItemsAndReorderCloseNoGaps`
- `deleteDayCascadesItems`
- `deleteItemRenumbersRemaining`
- `duplicateDayOfWeekReturns409`
- `reorderWithIncompleteListReturns409`
- `updateItemPatchesFields`

**Symptom:** `objectMapper.readTree(body).get("id")` returns null after a 201 from `POST /api/workout-plans/{id}/days`. The status check passes, so the controller succeeded; the response body simply lacks an `id` field at the top level.

**Confounder:** `createDayAddsItToPlan` (the only WorkoutDays test that does NOT use the helper) hits the same endpoint with the same payload shape and passes — but it asserts `$.dayOfWeek` and `$.focus`, never `$.id`, so we cannot tell from that test whether `id` is actually in the body or not.

**Hypotheses to investigate:**
- `WorkoutPlanMapper.toDayDto` returns the entity's `getId()` immediately after `plans.saveAndFlush(plan)`. Hibernate's `@UuidGenerator` should populate the id before the flush. Verify with a debugger that `day.getId()` is non-null at mapper time.
- Possible cascade ordering issue: `plan.addDay(day)` may not set `day.plan = this` (need to check `WorkoutPlan.addDay`). If the back-reference is missing, the FK insert may fail silently and the entity may not be persisted, leaving id null.
- Jackson with `serializationInclusion = NON_NULL` would omit a null id. If the entity is detached or not yet generator-assigned, the field is dropped silently.

**Trigger to reopen:** Local Maven environment available, OR move investigation onto a worktree where the test can be repeatedly run with breakpoints.

### i-2 — FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (1 failure)

**Symptom:** After exporting a plan and re-importing the dump, `plansInserted` is `0` instead of `>= 1`. The round-trip drops the plans slice entirely.

**Trigger to reopen:** Same as i-1 — needs local repro.

### i-3 — Deviation: .gitignore data/ pattern adjusted to satisfy verification (Plan 01-01)

**Context:** Plan 01-01 Task 2 prescribed:

```
data/
!data/.gitkeep
!data/postgres/.gitkeep
```

**Symptom:** Per Git's documented rule "It is not possible to re-include a file if a parent directory of that file is excluded", the `data/` directory exclusion prevents `!data/.gitkeep` and `!data/postgres/.gitkeep` from re-including their files. Verification step `git check-ignore data/postgres/.gitkeep` returned exit 0 (ignored) when it must return exit 1 (not ignored).

**Resolution applied:** Replaced with the working glob form that does not exclude the parent directory:

```
data/*
!data/.gitkeep
!data/postgres/
data/postgres/*
!data/postgres/.gitkeep
```

This preserves the plan's intent (only `.gitkeep` files tracked under `data/`) while satisfying both verification checks.

**Trigger to reopen:** None — resolved at execution time.

### i-4 — Frontend per-request HTTP metrics not yet exposed (Phase 4 follow-up)

Plan 04-01 ships only Node process metrics (uptime, memory) on `/api/metrics`. Contract section 9.1 also mentions "HTTP request count and duration histogram" - that needs a Next.js App Router middleware-level instrumentation hook (or a small server-side counter store) to count fetch handler invocations and record latency histograms. Out of scope for the v0.3 contract baseline; track for v0.6 (Operational Maturity).

**Trigger to reopen:** v0.6 milestone planning OR operator request for richer frontend telemetry.

## Closed

(none)
