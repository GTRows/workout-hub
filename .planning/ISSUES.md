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

### i-6b — Defer Next.js 16.x major bump (successor to i-6)

**Context:** i-6 was named "Next 15 -> 16" but the Phase 40-02 standalone
single-commit budget could not absorb the actual major migration because the
in-tree i18n stack (`next-intl@^3.26.0`) has no peer-dependency entry for
`next ^16.0.0`. Plan-author audited every published 3.x version on
`https://registry.npmjs.org/next-intl` (queried 2026-05-09) and confirmed
zero 3.x versions list `^16.0.0` in the `next` peer range; the latest 3.x
release `3.26.5` tops out at `next ^15.0.0`. Only the 4.x line peers
`^16.0.0` (`next-intl@4.11.1` lists `next ^12.0.0 || ^13.0.0 || ^14.0.0 ||
^15.0.0 || ^16.0.0`). Bumping `next` past `15.x` while keeping
`next-intl@^3.26.0` would either break `pnpm install` peer-check (default
behaviour) or require a `pnpm.overrides` / `--strict-peer-dependencies=false`
hack that hides a real runtime-compatibility risk (next-intl 3.x's
`createNextIntlPlugin` patches Next's middleware and `unstable_rootParams`
plumbing in ways 4.x removed and 16.x deprecated).

Independent of the next-intl block, Next 16 also introduces six surface
changes that affect this codebase: (1) the `Middleware` API is renamed to
`Proxy` (deprecation warning in 16.x; codemod available; affects
`frontend/src/middleware.ts` plus `frontend/src/middleware.test.ts`'s
matcher-coverage assertions and the bound metric-recording call
`getRegistry().record(...)` from Phase 35); (2) `useSearchParams` now hard
errors at build time when called outside a `<Suspense>` boundary instead of
warning (affects `frontend/src/app/(auth)/login/page.tsx` which reads `next`
and `reason` query params at the top of its client-component body without
any wrapping Suspense); (3) `useReportWebVitals` from `next/web-vitals`
unchanged (NON-blocker; v0.6 lock-in survives); (4) App Router `params:
Promise<{ id: string }>` already adopted in
`session/[id]/page.tsx` and `exercises/[id]/page.tsx` (NON-blocker); (5)
default `images.minimumCacheTTL` bumped from 1 minute to 4 hours, default
`images.imageSizes` loses the 16px entry, `images.domains` deprecated, and
`next/legacy/image` deprecated (NON-blocker because `frontend/next.config.ts`
does not set an `images` block — codebase relies on framework defaults); (6)
TypeScript 5.1.0 minimum (already satisfied by `^5.7.2`), sass-loader v16
(NON-blocker; codebase has no sass dep), eslint-plugin-react-hooks v7
(transitive via `eslint-config-next` and bumps in lockstep), browserslist
update (NON-blocker; uses framework default), and the
`@next/eslint-plugin-next` flat-config default (NON-blocker;
`frontend/eslint.config.mjs` is already flat). Surfaces 1 and 2 are real
source-file mutations that exceed Phase 40-02's bookkeeping scope; the
remaining four are NON-blockers.

Other Next 16 breaking changes that the eventual closure plan must account
for include (per the Vercel `v16.0.0` release notes): removed deprecated
`unstable_rootParams`, removed deprecated sync access to Dynamic APIs,
mandatory `images.localPatterns` for query strings on `<Image>` `src`,
removed deprecated AMP support, the `experimental.cacheComponents` flag,
Turbopack as default bundler (still opt-out via `--webpack` flag through
the canary line), the new MCP server in dev mode, and the `proxy` codemod
shipped via `@next/codemod` for the `middleware -> proxy` rename. Next 16
also bumps the engines.node floor to `>=20.9.0`; the codebase's
`engines.node: ">=20.0.0"` declaration in `frontend/package.json` will need
a one-line bump in the closure plan (the runtime container already runs
Node 22 per the frontend Dockerfile multi-stage builder, so this is a
declaration alignment, not a runtime risk).

**Why deferred:** Phase 40-02 cannot absorb the next-intl 4 migration plus
the Middleware->Proxy rename plus the Suspense-boundary additions plus the
Node-engine declaration bump plus the lockfile-resolution probe with
co-installed `next@^16.x` and `next-intl@^4.11.x` within the small,
ship-able, single-commit-and-CI-pass cadence the v1.0 milestone requires.
The roadmap's "in sequence (each its own commit and test pass)" rule
explicitly forbids combining the Next 16 jump with the next-intl 4 jump.
The phased v1.0 scope is e2e tests (Phase 38), framework patch-level
hardening (Phase 39 testcontainers; Phase 40-01 Spring Boot 4 deferral;
Phase 40-02 Next 16 deferral; Phase 40-03 next-intl 4 migration), security
hardening (Phase 41 i-13 SpringDoc + i-14 Netty 4.2), docs (Phase 42),
backup/restore drill (Phase 43), and v1.0.0 release (Phase 44). Adding the
Next 16 closure work to that timeline would either compress one of the
existing phases or push v1.0.0 by multiple working sessions; both are
worse than honest deferral.

**Trigger to reopen:** Phase 40-03 (i-5 next-intl 3 -> 4 migration) ships
green AND the maintainer chooses to absorb Next 16 inside the v1.0
remaining phase budget OR the v1.1 milestone-opening sub-plan picks it up.
Either path requires a fresh sub-plan (likely 40-04 inside v1.0 OR a
v1.1.0 first sub-plan) that explicitly:

1. **Probes the lockfile resolution** by running `pnpm install` with
   `next@^16.2.6` (or whatever is current latest stable at probe time)
   AND `next-intl@^4.x` (already on the lockfile after 40-03) AND
   `eslint-config-next@^16.x` AND `@types/node@^25.x` (or current). Reads
   the resulting `pnpm-lock.yaml` and confirms no peer-dependency
   warnings, no `--strict-peer-dependencies=false` requirement, no
   `pnpm.overrides` block needed.

2. **Renames `frontend/src/middleware.ts` -> `frontend/src/proxy.ts`** OR
   keeps `middleware.ts` and accepts the deprecation warning in CI logs.
   The codemod from Vercel (`@next/codemod middleware-to-proxy`) is the
   preferred mechanism. Updates `frontend/src/middleware.test.ts`
   accordingly (or splits it into `frontend/src/proxy.test.ts`).
   Preserves the Phase 29.5 matcher-coverage contract (12
   `(app)`-group route segments + three operator paths) AND the Phase 35
   bound `getRegistry().record(...)` per-request metric call.

3. **Wraps `frontend/src/app/(auth)/login/page.tsx` in a `<Suspense>`
   boundary** so the `useSearchParams()` calls do not break the now-fatal
   build error. The simplest fix is to lift the `useSearchParams`-reading
   body into a child client component and wrap the parent in
   `<Suspense fallback={<RouteSkeleton variant="auth" />}>` (the
   `RouteSkeleton` primitive already exists from Phase 36). Alternative:
   `export const dynamic = "force-dynamic"` on the page module to opt out
   of static prerender entirely. Either path requires source mutation;
   re-validates `frontend/src/app/(auth)/login/page.test.tsx` and the
   middleware redirect test that constructs a `?next=...` query param.

4. **Bumps `engines.node: ">=20.0.0"` -> `">=20.9.0"`** in
   `frontend/package.json` to match Next 16's published floor.

5. **Re-runs `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build`,
   AND `pnpm test:e2e`** locally (the Windows host CAN run all five for
   the frontend; only the Maven backend has the local-mvn-gap). The
   `pnpm build` standalone-trace-copy EPERM noted in the spawn prompt is
   a known host-only issue; the compile-success criterion is the gate,
   not the trace-copy step.

6. **Validates the `next/image` default-cache-TTL bump** from 1 minute to
   4 hours does not regress any operator-facing CDN posture. Self-hosted
   contract does not constrain image cache TTL, so this is expected to
   be a NON-issue, but the closure plan should explicitly assert it.

7. **Validates that `images.localPatterns` enforcement does not break
   `next/image` callers with query-string `src` values.** Plan-author did
   NOT audit every `next/image` caller for query-string `src` patterns
   (out of bookkeeping scope); the closure plan must enumerate them. As
   of plan-author time, `frontend/src/components/exercise-media.test.tsx`
   is the most likely surface to hit this constraint and should be
   audited first.

8. **Drops the deprecated AMP, `images.domains`, and `next/legacy/image`
   surfaces** if any are in use (none audited at plan time; closure plan
   must confirm).

9. **Decides whether to enable Turbopack as the default bundler** (Next
   16's new default) OR pass `--webpack` to keep the existing webpack
   path. Both work; Turbopack is the recommended path going forward but
   may surface unrelated regressions on first build.

The plan author for the closure must also rebuild the lockfile from
`frontend/pnpm-lock.yaml` (`pnpm install --frozen-lockfile=false` then
`pnpm install`), commit the regenerated lockfile alongside the
`package.json` edits, and ensure CI runs `pnpm install
--frozen-lockfile=true` cleanly. Both `frontend/package.json` and
`frontend/pnpm-lock.yaml` are protected files per `CLAUDE.md`; the
closure plan MUST exercise the protected-file pre-authorisation flow
(unlike this deferral plan, which deliberately does NOT touch any
protected file).

**Owner:** aciro
**Status:** Open

### i-7b — Defer Testcontainers 2.0.0 major bump (successor to i-7)

**Context:** i-7 was named "1.x -> 2.x" but no 2.x artifact was published by
the v1.0 release. Phase 39-01 captured all available cumulative fixes by
bumping 1.20.4 -> 1.21.3 (latest 1.x). The actual major-version jump remains.

**Why deferred:** Testcontainers 2.0.0 has not been released as of
2026-05-09. There is no public RC, no published migration guide, no
breaking-change list to plan against. A speculative plan would be wasted.

**Trigger to reopen:** Maven Central publishes
`org.testcontainers:testcontainers:2.0.0` (GA, not RC). At that point: read
the upstream migration guide, audit `AbstractIntegrationTest` for the
@Container-field pattern shift if required, run the full migration-test
suite in CI.

**Owner:** aciro
**Status:** Open

### i-8b — Defer Spring Boot 4.0.x major bump (successor to i-8)

**Context:** i-8 was named "3.4 -> 4.0" but the v1.0 release-hardening
milestone could not absorb the actual major migration within the original
single-line-bump budget. The rolled-back 40-01 attempt (commits
75c3ea1..e49e1dc on 2026-05-09) confirmed three breaking-change surfaces
that were not foreseen in the original plan: (1) `Jackson2ObjectMapperBuilderCustomizer`
package relocation from `org.springframework.boot.autoconfigure.jackson` to
`org.springframework.boot.jackson2.autoconfigure` (trivial import edit;
affects `JacksonConfig.java`); (2) `PrometheusScrapeEndpoint` +
`PrometheusOutputFormat` package relocation from
`org.springframework.boot.actuate.metrics.export.prometheus` to
`org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus`
(trivial import edits; affects `PrometheusMetricsController.java`); and
(3) BLOCKING — Jackson 2 (`com.fasterxml.jackson.*`) is no longer the
default transitive of `spring-boot-starter-web` under Spring Boot 4 (the
default is Jackson 3 / `tools.jackson.*` via `spring-boot-jackson`). The
codebase has 36 files referencing Jackson 2 (`JacksonConfig`,
`ClaudeSummaryDto.@JsonNaming(SnakeCaseStrategy)`, `AuditLogService`,
all DTO `@JsonInclude(NON_NULL)` annotations, `WebPushNotificationDispatcher`,
`GoogleFitParser`, `AppleHealthParser`, etc.). Restoring Jackson 2 compat
under Spring Boot 4 requires either (a) adding `spring-boot-jackson2` as
an explicit dependency AND verifying Spring MVC's `HttpMessageConverter`
selection picks the Jackson 2 `ObjectMapper` (with both Jackson 2 and
Jackson 3 simultaneously on the classpath), OR (b) migrating all 36
Jackson 2 usages to Jackson 3 (`tools.jackson.*`). Both paths exceed the
v1.0 release-hardening single-commit cadence and depend on runtime
behaviour the local-Maven-gap cannot validate without per-attempt CI
round-trips. Spring Boot 4.0.6 BOM also ships Spring Framework 7.0.7 /
Spring Security 7.0.5 / Hibernate 7.2.12.Final / Jakarta EE 11 (Servlet
6.1.0, Persistence 3.2.0, Validation 3.1.1) and Netty 4.2.12.Final (the
Netty pin drop side-effect would narrow i-14 to a 4.2.12 -> 4.2.13 micro
bump, but only when i-8b closes for real).

**Why deferred:** v1.0 cannot absorb the 36-file Jackson 2 -> 3 migration
or the spring-boot-jackson2 compat-module runtime probe within the small,
ship-able, single-commit-and-CI-pass cadence the milestone requires. The
v1.0 milestone scope is e2e tests (Phase 38), framework patch-level
hardening (Phase 39 testcontainers; Phase 40 frontend Next 16 +
next-intl 4), security hardening (Phase 41 i-13 SpringDoc + i-14 Netty
4.2), docs (Phase 42), backup/restore drill (Phase 43), and v1.0.0
release (Phase 44). Adding a 36-file Jackson migration to that timeline
would either compress one of the existing phases or push v1.0.0 by
multiple working sessions; both are worse than honest deferral.

**Trigger to reopen:** Author a fresh plan (likely a dedicated phase
post-v1.0, or a v1.1 milestone-opening sub-plan) that explicitly chooses
between the two Jackson-compat paths:

1. **Path A (Jackson 2 compat module):** Add `org.springframework.boot:spring-boot-jackson2`
   to `backend/pom.xml` `<dependencies>`. Bump `<spring-boot-starter-parent>`
   to the latest stable 4.0.x (or 4.1.x at the time of plan). In a CI
   probe, confirm Spring MVC's `HttpMessageConverter` chain
   resolves to the Jackson 2 `ObjectMapper` for application/json
   (the `MappingJackson2HttpMessageConverter` should still be
   registered when both Jackson 2 and Jackson 3 are on the classpath).
   Verify all 36 Jackson 2 callers continue to compile and behave
   identically. Apply the trivial import-line fixes for the Jackson2-module
   and Prometheus-actuator relocations.

2. **Path B (full Jackson 3 migration):** Replace all 36 files'
   `com.fasterxml.jackson.*` imports with `tools.jackson.*` equivalents.
   Re-validate `@JsonNaming(SnakeCaseStrategy)`,
   `@JsonInclude(NON_NULL)`, `@JsonProperty`, `@JsonIgnore`,
   `@JsonCreator`, `@JsonValue`, custom serializers/deserializers,
   `ObjectMapper` config under the Jackson 3 API. Re-validate the
   ClaudeSummaryDto snake_case round-trip (Direction C lock-in) and
   the Full-Export camelCase posture. Bump
   `<spring-boot-starter-parent>` to the latest stable 4.0.x or 4.1.x.
   Apply the trivial import-line fixes for the Jackson2-module
   relocation (which is moot under Path B because Path B doesn't use
   Jackson 2) and the Prometheus-actuator relocation.

The plan author for either path must also account for the
`spring-boot-autoconfigure` -> per-feature module split documented in
i-8's enriched body (Jackson2 + micrometer-metrics surfaces verified
above; the rest of the codebase should be greppable for old-package
imports). The Spring Framework 7 / Spring Security 7 / Hibernate 7 /
Jakarta EE 11 BOM transitives also flow in via the bump and need a
fresh DSL audit; the original 40-01 plan (now superseded) captured
that audit and can be reused as a starting reference.

**Owner:** aciro
**Status:** Open

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

### i-15: Lighthouse CI workflow + bundle-size enforcement script (perf-budget gating)

- **What**: Phase 36-03 ships `docs/PERF_BUDGETS.md` with written Core Web Vitals targets and per-route bundle ceilings, plus a runtime collector that emits the five Core Web Vitals to the existing `/api/metrics` Prometheus scrape. CI gating is NOT yet wired: there is no Lighthouse CI workflow, no `scripts/check-bundle-size.mjs`, and no `@next/bundle-analyzer` devDependency. Operators must measure manually per the doc's "How to measure" section.
- **Why deferred**: All three add-ons require edits to protected paths. `.github/workflows/lighthouse.yml` would touch `.github/workflows/**` (protected). `scripts/check-bundle-size.mjs` would touch `scripts/**` (protected). `@next/bundle-analyzer` would mutate `frontend/package.json` (protected). Phase 36-03 stayed protected-file-clean by scope.
- **Trigger**: Operator approves at least one protected-file edit, OR Phase 38+ (v1.0 hardening) explicitly takes ownership of CI perf gating. Perf-budget doc shipped 2026-05-09 in 36-03.
- **Owner**: aciro
- **Related files**: `docs/PERF_BUDGETS.md`, `frontend/src/components/web-vitals-reporter.tsx`, `frontend/src/app/api/vitals/route.ts`, `frontend/src/lib/metrics/registry.ts`.
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

### i-9 — StructuredLoggingTest cannot capture ECS JSON under @SpringBootTest

**Disabled in:** `backend/src/test/java/com/workouthub/common/logging/StructuredLoggingTest.java`
**Reason:** The test boots a minimal `@SpringBootConfiguration` with `WebEnvironment.NONE` and `@ActiveProfiles("prod")` to exercise the Spring Boot 3.4 native ECS structured logging customizer. Even with `OutputCaptureExtension` (which intercepts stdout before Logback starts), the test still finds no JSON line in captured output. Likely cause: the minimal configuration does not trigger Spring Boot's `LoggingApplicationListener` reconfiguration to ECS, so the test runs with default human-readable logback layout. ECS output works correctly at real runtime (`docker compose up` boots the full `WorkoutHubApplication` and emits ECS JSON).

**Trigger to reopen:** Redesign the test to boot the full `WorkoutHubApplication` (probably with a Testcontainers postgres) so the prod logging system actually activates. Likely v0.4 testing-infra session.

*Closed by Phase 37 Plan 01: StructuredLoggingTest now extends
AbstractIntegrationTest and boots WorkoutHubApplication.class under
@ActiveProfiles("prod") + Testcontainers Postgres, so
LoggingApplicationListener reconfigures the logging system to ECS-format JSON.
@Disabled removed; both contract assertions (JSON shape with @timestamp +
message; deny-listed authorization MDC value masked from stdout) run on every
CI build.*

### i-6 — Defer next 15 -> 16 major framework bump (PR #13)

**PR:** https://github.com/GTRows/workout-hub/pull/13
**Reason for defer:** Next.js 16 has its own migration story (App Router conventions, breaking changes in middleware, async dynamic APIs). Cannot ship in v0.3 without absorbing the migration cost. Stays on 15.x line.

**Trigger to reopen:** v0.5 milestone (frontend completion phase) OR Next 15 LTS end-of-life signal.

*Closed by Phase 40 Plan 02: the Next 15 -> 16 single-line bump within the original i-6 budget proved infeasible because next-intl 3.x's published peer range tops out at `next ^15.0.0` (verified at plan-author time against `https://registry.npmjs.org/next-intl` on 2026-05-09; every 3.x version including the 3.26.x line tail at 3.26.5 lists `next ^10.0.0 || ^11.0.0 || ^12.0.0 || ^13.0.0 || ^14.0.0 || ^15.0.0` and ZERO 3.x versions list `^16.0.0`). Only next-intl 4.x peers `^16.0.0` (4.11.1 lists `next ^12.0.0 || ^13.0.0 || ^14.0.0 || ^15.0.0 || ^16.0.0`), so the Next 16 install requires next-intl 4 in the same lockfile — which the roadmap's "in sequence (each its own commit and test pass)" rule forbids combining into a single bump. The frontend runtime stays pinned within the existing `^15.1.0` caret (latest 15.x backport `15.5.18` per the npm `backport` dist-tag at plan-author time). The major-version jump is re-deferred as follow-up issue i-6b with the dependency-graph evidence and the App Router / middleware / Suspense / image / sass-loader / eslint surface analysis carried forward verbatim. v1.0 ships on Next 15.x; the 15.5.x backport line still receives security patches via the `^15.1.0` caret. Mirror precedent: Phase 39-01 / i-7 -> i-7b deferral playbook AND Phase 40-01 / i-8 -> i-8b deferral playbook.*

### i-7 — Defer testcontainers 1.x -> 2.x major bump (PR #11)

**PR:** https://github.com/GTRows/workout-hub/pull/11
**Reason for defer:** Major bump on test infrastructure. Could break the `AbstractIntegrationTest` base class and migration tests. Currently on 1.20.4 which is well-supported. Risk vs benefit not justified for v0.3.

**Trigger to reopen:** Next time test infrastructure receives attention (likely after a CI flake batch or v0.4 backend feature work).

*Closed by Phase 39 Plan 01 for the 1.x scope: bumped
`<testcontainers.version>` from 1.20.4 to 1.21.3 (latest stable 1.x as of
2026-05-09; Maven Central confirmed no 2.x artifact published yet).
AbstractIntegrationTest API surface (PostgreSQLContainer constructor +
withDatabaseName/withUsername/withPassword/withReuse, manual static-init
start pattern) is compatibility-preserved across the 1.20 -> 1.21 hop. All
10 migration tests under backend/src/test/java/com/workouthub/migrations/
inherit from AbstractIntegrationTest with zero direct Testcontainers imports
and required no edits. The eventual 2.x major bump is re-deferred as
follow-up issue i-7b, triggered by the upstream Testcontainers 2.0.0 GA.*

### i-8 — Defer Spring Boot 3.4 -> 4.0 major framework bump (PR #8)

**PR:** https://github.com/GTRows/workout-hub/pull/8
**Reason for defer:** Spring Boot 4 brings Servlet 6 / Hibernate 7 / Spring Framework 7 changes. Cannot land in v0.3 self-hosted-contract scope. The Spring Boot 3.4.x line is still actively patched.

**Trigger to reopen:** Spring Boot 3.4 EOL OR v0.6 (Operational Maturity) milestone OR a security-driven need.

**v1.0 Phase 40-01 attempt rolled back (2026-05-09):** A Spring Boot
3.5.14 -> 4.0.6 bump was attempted at commits 75c3ea1..e49e1dc and
reverted at the next commit. Two Spring Boot 4 surface changes broke
compilation that the original 40-01 plan did not catch:

1. **`Jackson2ObjectMapperBuilderCustomizer` package relocation.** Moved
   from `org.springframework.boot.autoconfigure.jackson` (Spring Boot 3
   `spring-boot-autoconfigure`) to
   `org.springframework.boot.jackson2.autoconfigure` (Spring Boot 4
   `spring-boot-jackson2`). Affects `JacksonConfig.java`. Trivial import
   line edit.
2. **`PrometheusScrapeEndpoint` + `PrometheusOutputFormat` package
   relocation.** Moved from
   `org.springframework.boot.actuate.metrics.export.prometheus` to
   `org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus`
   (new `spring-boot-micrometer-metrics` module). Affects
   `PrometheusMetricsController.java`. Trivial import line edits.
3. **Jackson 2 vs Jackson 3 default-classpath shift (BLOCKING).** Spring
   Boot 4 makes Jackson 3 (`tools.jackson.*` package family) the default
   transitive of `spring-boot-starter-web` via `spring-boot-starter-jackson
   -> spring-boot-jackson`. The Jackson 2 family (`com.fasterxml.jackson.*`,
   used by 36 files in this codebase including `JacksonConfig`,
   `ClaudeSummaryDto.@JsonNaming(SnakeCaseStrategy)`, `AuditLogService`,
   all `@JsonInclude` annotations on DTOs, `WebPushNotificationDispatcher`,
   `GoogleFitParser`, `AppleHealthParser`, etc.) is now opt-in via the
   separate `spring-boot-jackson2` module. The follow-up CI showed
   `package org.springframework.boot.jackson2.autoconfigure does not exist`
   even after the import-path fix, because that module is not pulled in
   transitively by any of the starters this project uses (web, data-jpa,
   security, validation, actuator).

   Restoring Jackson 2 compatibility under Spring Boot 4 requires either
   (a) adding `spring-boot-jackson2` as an explicit dependency AND
   verifying that Spring MVC's `HttpMessageConverter` selection resolves
   to the Jackson 2 `ObjectMapper` (not Jackson 3 — both will be on the
   classpath), OR (b) migrating all 36 Jackson 2 usages to Jackson 3
   (`tools.jackson.*`). Both paths exceed the 10-line follow-up budget
   the 40-01 plan defined and depend on runtime behaviour that the
   local-Maven-gap cannot validate without a CI round-trip per attempt.

**Updated trigger to reopen:** Author a fresh 40-01-PLAN that explicitly
chooses between Jackson-2-compat (add `spring-boot-jackson2` dep + verify
HttpMessageConverter ObjectMapper selection) and Jackson-3-migration
(replace 36 files' `com.fasterxml.jackson.*` imports with
`tools.jackson.*`). The plan must also account for the
`spring-boot-autoconfigure` -> per-feature module split (Jackson2 +
micrometer-metrics surfaces verified above; the rest of the codebase
should be greppable for old-package imports). Spring Boot 4.0.6 BOM
ships Spring Framework 7.0.7 / Spring Security 7.0.5 / Hibernate
7.2.12.Final / Jakarta EE 11 (Servlet 6.1.0, Persistence 3.2.0,
Validation 3.1.1) and Netty 4.2.12.Final (the netty pin drop side-effect
narrows i-14 to a 4.2.12 -> 4.2.13 micro bump, but only when i-8 closes
for real).

*Closed by Phase 40 Plan 01 (replan iteration 2/2): the Spring Boot 4 single-line bump within the original i-8 budget proved infeasible after the rolled-back 3.5.14 -> 4.0.6 attempt surfaced the Jackson 2 vs Jackson 3 default-classpath shift, the Jackson2-module package relocation, and the Prometheus-actuator-endpoint package relocation. The runtime stays pinned at Spring Boot 3.5.14 (the tail of the 3.5.x patch line on Maven Central as of 2026-05-09; metadata `<lastUpdated>` `20260423155822`). The major-version jump is re-deferred as follow-up issue i-8b with the executor's enriched analysis carried forward verbatim. v1.0 ships on Spring Boot 3.5.x; no in-line CVE bump on the 3.5 line is available within this plan because 3.5.14 is the published patch tail. Mirror precedent: Phase 39-01 / i-7 -> i-7b deferral playbook.*
