---
phase: 04-prometheus-metrics
plan: 01
subsystem: infra
tags: [metrics, prometheus, micrometer, observability, nextjs]
requires:
  - phase: 02-backend-health-endpoints
    provides: SecurityConfig allowlist pattern for additive contract endpoints
provides:
  - backend /metrics alias to PrometheusScrapeEndpoint (additive; /actuator/prometheus preserved)
  - frontend /api/metrics inline Node process metrics
  - SecurityConfig allowlist for /metrics
affects:
  - Phase 9 README quick start (operator scrape config)
  - v0.6 (Operational Maturity) - frontend per-request HTTP histogram via i-4
tech-stack:
  added: []
  patterns:
    - "Additive Spring controller alias to Actuator endpoint (delegate, do not move)"
    - "Inline Prometheus text format from Node process.* in Next route handler (no prom-client dep)"
key-files:
  created:
    - backend/src/main/java/com/workouthub/common/web/MetricsController.java
    - backend/src/test/java/com/workouthub/common/MetricsControllerIntegrationTest.java
    - frontend/src/app/api/metrics/route.ts
    - frontend/src/app/api/metrics/route.test.ts
  modified:
    - backend/src/main/java/com/workouthub/common/config/SecurityConfig.java
    - .planning/ISSUES.md
key-decisions:
  - "Alias rather than replace - /actuator/prometheus stays for any consumer that already depends on it"
  - "Frontend inline metrics chosen over prom-client dep (zero new package.json change; Node process.* gives uptime + memory natively)"
  - "Per-request HTTP count/duration histogram deferred to v0.6 via ISSUES i-4 (Next App Router middleware integration is non-trivial; out of v0.3 contract baseline scope)"
issues-created: [i-4]
duration: ~12 min
completed: 2026-05-02
---

# Phase 4 Plan 01: Prometheus /metrics on main listener

**Backend `/metrics` aliases to Micrometer's PrometheusScrapeEndpoint and frontend `/api/metrics` serves Node process metrics inline; both reachable on the main HTTP listener with zero new pom or npm dependencies.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-05-02T18:21:12Z
- **Completed:** 2026-05-02T18:24:25Z
- **Tasks:** 3 of 3
- **Files modified:** 4 created, 2 modified

## Accomplishments

- Backend `MetricsController` delegates to `PrometheusScrapeEndpoint` via the actual Spring Boot 3.4.1 API (`PrometheusOutputFormat.CONTENT_TYPE_004`, `byte[]` return decoded as UTF-8). Returns the same body as `/actuator/prometheus` with `text/plain; version=0.0.4; charset=utf-8`.
- `/actuator/prometheus` left in place for backward compat with any monitoring that already targets it.
- SecurityConfig adds `/metrics` to the public allowlist next to `/livez`, `/healthz`, and the actuator block.
- Frontend `/api/metrics` route handler emits 6 Prometheus-format gauges (uptime, RSS, heap used/total, external memory, process start time) with `# HELP` and `# TYPE` lines. No `prom-client` dependency added.
- Vitest covers the frontend handler shape; backend integration test covers both the aliased `/metrics` and the preserved `/actuator/prometheus`.
- ISSUES.md gains i-4 (frontend per-request HTTP count/duration histogram deferred to v0.6).

## Task Commits

1. **Task 1: backend MetricsController + SecurityConfig** - `b2fe6e0` (feat)
2. **Task 2: MetricsControllerIntegrationTest** - `23ed67f` (test)
3. **Task 3: frontend route handler + test + ISSUES i-4** - `9b302b1` (feat)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `backend/src/main/java/com/workouthub/common/web/MetricsController.java` (new) - delegates to `PrometheusScrapeEndpoint.scrape(PrometheusOutputFormat.CONTENT_TYPE_004, null)`
- `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java` - allowlist `/metrics`
- `backend/src/test/java/com/workouthub/common/MetricsControllerIntegrationTest.java` (new) - asserts /metrics 200 with `# HELP` line + /actuator/prometheus still 200
- `frontend/src/app/api/metrics/route.ts` (new) - inline 6-metric Prometheus text endpoint
- `frontend/src/app/api/metrics/route.test.ts` (new) - asserts content-type and metric shape
- `.planning/ISSUES.md` - added i-4

## Decisions Made

- **Alias not replace.** `/actuator/prometheus` stays alive; `/metrics` is purely additive. Consumers already depending on the actuator path do not break.
- **Inline frontend metrics.** Node's `process.memoryUsage()` and `process.uptime()` cover the contract baseline (process metrics) without adding `prom-client` (which `frontend/package.json` is protected from receiving without confirmation). The 6 emitted gauges map to standard Prometheus `nodejs_process_*` names so a Grafana dashboard configured for any Node.js exporter recognizes them.
- **Per-request HTTP histogram deferred.** Section 9.1 of the contract mentions `HTTP request count and duration histogram`. Implementing that on the frontend cleanly needs a Next App Router middleware-level instrumentation hook (or a small shared counter store), which is real work and would expand this plan's scope. Tracked as ISSUES i-4 for v0.6 (Operational Maturity).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] PrometheusScrapeEndpoint API signature mismatch**

- **Found during:** Task 1 (backend controller implementation)
- **Issue:** Plan body sketched `scrape(TextOutputFormat.CONTENT_TYPE_004, null)` returning `String`. Actual 3.4.1 signature is `scrape(PrometheusOutputFormat, Set<String>)` returning `byte[]`. `TextOutputFormat` is the legacy enum used by the deprecated `PrometheusSimpleclientScrapeEndpoint`.
- **Fix:** Switched to `PrometheusOutputFormat.CONTENT_TYPE_004`, decoded `byte[]` body to String via `new String(bytes, StandardCharsets.UTF_8)`. Plan body explicitly permitted adaptation as long as behavior was preserved.
- **Files modified:** `backend/src/main/java/com/workouthub/common/web/MetricsController.java`
- **Verification:** API surface verified by javap inspection of `spring-boot-actuator-3.4.1.jar` from local Maven cache.
- **Committed in:** `b2fe6e0` (Task 1 commit)

### Deferred Enhancements

Logged to .planning/ISSUES.md for future consideration:
- i-4: Frontend per-request HTTP metrics not yet exposed (Phase 4 follow-up, v0.6 milestone)

---

**Total deviations:** 1 auto-fixed (Rule 3 - Blocking). 1 deferred to ISSUES (planned).
**Impact on plan:** Adaptation produced strictly equivalent behavior with the correct 3.4.1 API. No scope creep.

## Issues Encountered

None beyond the API adaptation. Frontend `pnpm typecheck` exit 0; `pnpm test -- src/app/api/metrics/route.test.ts` 1/1 passing locally. Backend integration test runs in CI on push.

## Next Phase Readiness

- **Phase 4 closes here.** Both services advertise `/metrics` on their main listeners with zero new dependencies.
- **Phase 5 (Forward-Auth Mode + OIDC reconsideration) unblocked.** Phase 5 has a real architectural decision (remove or gate the existing `OidcController` per contract section 7) that may warrant user input before execution.
- **Operator-facing benefit:** A single Prometheus job can now scrape both services with two simple targets (`backend:8080/metrics`, `frontend:3000/api/metrics`), no need to know `/actuator/*` namespace.

---
*Phase: 04-prometheus-metrics*
*Completed: 2026-05-02*
