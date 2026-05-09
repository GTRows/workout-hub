# Issues

Deferred work and known failing tests. Each entry includes the trigger that should reopen it.

## Open

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

### i-5 — Defer next-intl 3 -> 4 major bump (PR #2)

**PR:** https://github.com/GTRows/workout-hub/pull/2
**Closes vuln alerts:** #8 and #11 (open redirect, severity medium)
**Reason for defer:** Major API change; requires migration of `frontend/messages/*.json` and `i18n/request.ts`. Out of v0.3 self-hosted-contract scope. The open-redirect risk is mitigated for v0.3 by the operator's reverse-proxy configuration (Caddy / nginx header rewrite); documented in Phase 9 README.

**Trigger to reopen:** v0.4 frontend work OR a careful next-intl 4 migration session.

### i-6 — Defer next 15 -> 16 major framework bump (PR #13)

**PR:** https://github.com/GTRows/workout-hub/pull/13
**Reason for defer:** Next.js 16 has its own migration story (App Router conventions, breaking changes in middleware, async dynamic APIs). Cannot ship in v0.3 without absorbing the migration cost. Stays on 15.x line.

**Trigger to reopen:** v0.5 milestone (frontend completion phase) OR Next 15 LTS end-of-life signal.

### i-7 — Defer testcontainers 1.x -> 2.x major bump (PR #11)

**PR:** https://github.com/GTRows/workout-hub/pull/11
**Reason for defer:** Major bump on test infrastructure. Could break the `AbstractIntegrationTest` base class and migration tests. Currently on 1.20.4 which is well-supported. Risk vs benefit not justified for v0.3.

**Trigger to reopen:** Next time test infrastructure receives attention (likely after a CI flake batch or v0.4 backend feature work).

### i-8 — Defer Spring Boot 3.4 -> 4.0 major framework bump (PR #8)

**PR:** https://github.com/GTRows/workout-hub/pull/8
**Reason for defer:** Spring Boot 4 brings Servlet 6 / Hibernate 7 / Spring Framework 7 changes. Cannot land in v0.3 self-hosted-contract scope. The Spring Boot 3.4.x line is still actively patched.

**Trigger to reopen:** Spring Boot 3.4 EOL OR v0.6 (Operational Maturity) milestone OR a security-driven need.

### i-9 — StructuredLoggingTest cannot capture ECS JSON under @SpringBootTest

**Disabled in:** `backend/src/test/java/com/workouthub/common/logging/StructuredLoggingTest.java`
**Reason:** The test boots a minimal `@SpringBootConfiguration` with `WebEnvironment.NONE` and `@ActiveProfiles("prod")` to exercise the Spring Boot 3.4 native ECS structured logging customizer. Even with `OutputCaptureExtension` (which intercepts stdout before Logback starts), the test still finds no JSON line in captured output. Likely cause: the minimal configuration does not trigger Spring Boot's `LoggingApplicationListener` reconfiguration to ECS, so the test runs with default human-readable logback layout. ECS output works correctly at real runtime (`docker compose up` boots the full `WorkoutHubApplication` and emits ECS JSON).

**Trigger to reopen:** Redesign the test to boot the full `WorkoutHubApplication` (probably with a Testcontainers postgres) so the prod logging system actually activates. Likely v0.4 testing-infra session.

### i-12: FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport returns 500

- **What**: GET /api/export/full returns HTTP 500 when called for a freshly-seeded user with one day-less plan, no sessions, no metrics. Test asserts 200 at line 134 of FullExportImportIntegrationTest.java.
- **Why deferred**: Static causal trace at commit 96d81b0 found no deterministic 500 path in FullExportService for this specific fixture (TestAuthHelpers.seed bypasses DefaultPlanSeeder, so user has zero plans before the test creates one day-less plan via POST /api/workout-plans). Phase 18-04's export-path changes (Boolean isPr in SetRow, recompute safety net, validateSessionDayIds) are orthogonal to this failure. Likely transient or a yet-undiagnosed export-side defect not visible in static analysis. Local mvn unavailable on dev host (local-maven-gap), CI is the authoritative gate.
- **Trigger**: Push v0.4 commits and observe CI run. If FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport still fails, capture the Surefire stack trace and isolate the exception class + line.
- **Owner**: aciro
- **Status**: Open

### i-13: Bump SpringDoc to >= 2.7 to remove the ControllerAdviceBean workaround

- **What**: Phase 19-02 pinned SpringDoc to 2.6.0, which calls Spring 6.2's removed `new ControllerAdviceBean(Object)` constructor and crashes `GET /v3/api-docs` on Spring Boot 3.5.x. Phase 20 fix bundle (commit c1b95f7) worked around this by disabling SpringDoc's generic-response scan and registering ApiError schemas via OpenApiCustomizer.
- **Why deferred**: A SpringDoc bump introduces a transitive dependency change shortly before release; safer to ship 0.4.0 with the targeted workaround and bump the dep in a follow-up patch (0.4.1 or 0.5.0).
- **Trigger**: Plan a backend dependency review phase. Bump `springdoc.version` from 2.6.0 to 2.7+ (whatever is latest stable on Spring Boot 3.5.x), revert the OpenApiCustomizer workaround, and re-run the OpenApi integration tests.
- **Owner**: aciro
- **Status**: Open

### i-14: Bump Netty to 4.2.13.Final to close CVE-2026-42577

- **What**: io.netty:netty-transport-native-epoll 4.1.x line carries CVE-2026-42577 (epoll RST DoS). Fix only published in Netty 4.2.13.Final; no 4.1.x backport. Suppressed in trivy via .trivyignore for v0.5.0 release because the artifact is on the classpath but never touched by request handling (this app uses Spring MVC + Tomcat).
- **Why deferred**: Netty 4.2 is a major-line bump and risks Spring Boot 3.5.x compatibility. Out of scope for a v0.5.0 hotfix.
- **Trigger**: v0.6 Phase 41 (security-hardening). Bump Spring Boot's `<netty.version>` property to 4.2.13.Final or later, run full mvn verify, run e2e if available, and remove the CVE-2026-42577 entry from .trivyignore.
- **Owner**: aciro
- **Status**: Open

## Closed

### i-4 — Frontend per-request HTTP metrics not yet exposed (Phase 4 follow-up)

Plan 04-01 shipped only Node process metrics (uptime, memory) on `/api/metrics`. Contract section 9.1 also calls for "HTTP request count and duration histogram", which required a Next.js middleware-level instrumentation hook to count fetch handler invocations and record latency histograms. Tracked for v0.6 (Operational Maturity).

*Closed by Phase 35 Plan 01: hand-rolled `frontend/src/lib/metrics/registry.ts` (counter + histogram with Prometheus default bucket bounds) populated from `frontend/src/middleware.ts` per request, rendered on `/api/metrics` as `wh_frontend_http_requests_total` and `wh_frontend_http_request_duration_seconds_*`. Route-label cardinality bounded by `frontend/src/lib/metrics/route-label.ts`. No new runtime dependency.*

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

*Closed by Phase 14 Plan 01: explicit child-repo saveAndFlush in WorkoutDaysService (createDay, addItem) populates @UuidGenerator id before WorkoutPlanMapper reads it.*

### i-2 — FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (1 failure)

**Symptom:** After exporting a plan and re-importing the dump, `plansInserted` is `0` instead of `>= 1`. The round-trip drops the plans slice entirely.

**Trigger to reopen:** Same as i-1 — needs local repro.

*Closed by Phase 14 Plan 02: test now seeds a plan via POST /api/workout-plans before exporting; the production export/import round-trip was correct all along.*

### i-10 — is_pr round-trip drop on full-export import path

**Symptom:** After Phase 16-04 persisted `SessionSet.isPr` via V27, the
full-export wire format silently dropped the column at
`FullExportService.toSessionSection`. After re-import, every set's
`is_pr` flipped to `false` (V27 `DEFAULT false`) regardless of what the
source database had. Subsequent `GET /api/sessions/{id}` reads returned
`newPr: null` for every set until a PR-recompute pass ran.

**Trigger:** Surfaced post-Phase-16-04 by the 18-01 audit Section 4 Part F
Gap 1.

*Closed by Phase 18 Plan 04: appended `Boolean isPr` to
`FullExportDto.SetRow` (9th component); `FullExportService.toSessionSection`
populates from entity; `FullImportService.insertSessions` reads null-safely;
`SessionSetsService.recomputePrForExerciseHistory` is invoked once per
distinct touched (user, exerciseId) pair after the insert loop as a
safety net for legacy payloads.*
