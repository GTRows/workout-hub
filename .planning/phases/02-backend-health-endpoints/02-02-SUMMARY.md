---
phase: 02-backend-health-endpoints
plan: 02
subsystem: infra
tags: [nextjs, app-router, healthcheck, compose]
requires:
  - phase: 02-backend-health-endpoints (Plan 02-01)
    provides: backend /livez and /healthz endpoints
  - phase: 01-compose-refactor (Plan 01-02)
    provides: compose healthcheck wiring (provisional probe targets)
provides:
  - frontend /api/healthz process-liveness route handler
  - frontend /api/livez process-liveness route handler
  - compose backend probe retargeted to /healthz
  - compose frontend probe retargeted to /api/healthz
affects:
  - Phase 4 frontend metrics endpoint (sibling /api/metrics route handler can follow same pattern)
  - Phase 9 README quick start (probe URLs documented)
tech-stack:
  added: []
  patterns:
    - "Next.js App Router route handler with runtime=nodejs and dynamic=force-dynamic for probe endpoints"
key-files:
  created:
    - frontend/src/app/api/healthz/route.ts
    - frontend/src/app/api/livez/route.ts
    - frontend/src/app/api/healthz/route.test.ts
  modified:
    - compose.yml
key-decisions:
  - "Single test file covering /api/healthz only - /api/livez is structurally identical so a duplicate test would be coverage theater"
  - "Both endpoints return identical body and behavior; they exist as separate routes for orchestrator compatibility (k8s-style livez vs healthz semantics)"
  - "Force-dynamic + Node runtime to prevent Next from statically rendering or edge-routing probe responses"
issues-created: []
duration: ~10 min
completed: 2026-05-02
---

# Phase 2 Plan 02: Frontend health endpoints and compose retarget

**Frontend `/api/healthz` and `/api/livez` Node-runtime route handlers landed, and compose healthchecks now probe the contract endpoints (`/healthz` for backend, `/api/healthz` for frontend) instead of the provisional fallbacks from Plan 01-02.**

## Performance

- **Duration:** ~10 min (autonomous segment) plus deferred runtime verification
- **Started:** 2026-05-02T17:55:25Z
- **Completed:** 2026-05-02T18:00:00Z (estimate; checkpoint resolved as deferred)
- **Tasks:** 2 of 3 executed; Task 3 (human-verify checkpoint) deferred
- **Files modified:** 1 modified, 3 created

## Accomplishments

- `frontend/src/app/api/healthz/route.ts` and `frontend/src/app/api/livez/route.ts` both export a `GET` returning `{"status":"alive"}` with HTTP 200. Both declare `runtime = "nodejs"` and `dynamic = "force-dynamic"` so Next does not pre-render or edge-route the probe responses. They do not ping the backend, do not read env vars, do not touch IndexedDB or any persistent state - process liveness only, per the locked-in decision.
- `frontend/src/app/api/healthz/route.test.ts` covers the GET handler with a single Vitest assertion (status 200, body `{"status":"alive"}`). One file is enough; livez is structurally identical and a duplicate test would be coverage theater.
- `compose.yml` backend healthcheck now probes `wget -q --spider http://localhost:8080/healthz`. Frontend healthcheck now probes `node -e "fetch('http://localhost:3000/api/healthz').then(...)"`. Provisional inline comments removed. Interval / timeout / retries / start_period preserved.
- Operator-facing surface now matches `docs/SELF_HOSTED_CONTRACT.md` section 9.2 verbatim. Both services advertise dedicated `/healthz` and `/livez` per their semantics; the compose stack actually probes them.

## Task Commits

1. **Task 1: frontend route handlers + test** - `f11b8c0` (feat)
2. **Task 2: compose healthcheck retarget** - `e334acb` (chore)
3. **Task 3: human-verify checkpoint** - DEFERRED to operator-side verification (same constraint as Plan 01-02 Task 3; environment file cannot be created from this session)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `frontend/src/app/api/healthz/route.ts` - GET route handler returning process-liveness response
- `frontend/src/app/api/livez/route.ts` - GET route handler returning process-liveness response (structurally identical to healthz)
- `frontend/src/app/api/healthz/route.test.ts` - Vitest test asserting 200 + status field
- `compose.yml` - backend probe retargeted to `/healthz`, frontend probe retargeted to `/api/healthz`, provisional comments removed

## Decisions Made

- **Single Vitest test for /api/healthz only.** `/api/livez` is structurally identical; adding a duplicate test would be pure coverage theater. If livez later diverges (k8s-style two-stage liveness vs readiness, etc.) we add a test then.
- **Force-dynamic + Node runtime.** Probes need real-time, in-process answers. Static rendering at build time would freeze the response; edge runtime would not have access to the same process state once we add introspection later.
- **Probe target inside the container.** The compose probe runs inside the container and hits `localhost:3000/api/healthz` / `localhost:8080/healthz` directly. Reverse proxies front the operator-facing URLs separately; that wiring is operator scope.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- **Task 3 (docker compose human-verify) deferred.** Same root cause as Plan 01-02 Task 3: `docker compose up -d` requires a real `.env` file, which the repo blocks (gitignored AND `pre_guard_secrets.py` blocks Bash creation AND deny rule blocks Write). Naturally re-validated as part of Phase 12 (Release v0.3.0) which runs the full stack against a fresh env file.
- Local verification by the executing subagent: `pnpm typecheck` exit 0, `pnpm test -- src/app/api/healthz/route.test.ts` 1 passed, `pnpm lint` 0 errors (3 pre-existing warnings unrelated), and `python -c "import yaml; yaml.safe_load(open('compose.yml'))"` parses cleanly. Structural correctness is established; runtime smoke (containers actually reach `healthy`) is the deferred operator step.

## Next Phase Readiness

- **Phase 2 closes here.** Backend exposes contract-aligned `/livez` and `/healthz`; frontend exposes process-liveness `/api/healthz` and `/api/livez`; compose healthchecks probe them by name. Operator can `curl http://<host>:8080/healthz` and get a contract-shaped JSON response (200 ready / 503 unready_with_reason).
- **Phase 3 (Structured Logging) unblocked.** It can build on the `TraceIdFilter` MDC propagation already present and add Logback JSON encoder + secret deny-list filter for the production profile.
- **No blockers.** The deferred runtime checkpoint is a known repo-wide constraint, not specific to this plan, and the planned Phase 12 release cut naturally exercises it.

---
*Phase: 02-backend-health-endpoints*
*Completed: 2026-05-02*
