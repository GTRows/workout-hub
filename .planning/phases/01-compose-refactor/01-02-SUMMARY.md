---
phase: 01-compose-refactor
plan: 02
subsystem: infra
tags: [docker, compose, healthcheck, resource-limits, depends-on]
requires:
  - phase: 01-compose-refactor (Plan 01-01)
    provides: parametric ports, named network, bind mount data root
provides:
  - backend healthcheck via wget against /actuator/health (provisional, Phase 2 will re-point to /healthz)
  - frontend healthcheck via Node 22 built-in fetch against /
  - frontend.depends_on.backend.condition = service_healthy
  - per-service CPU + memory limits and reservations
  - top-of-file ceiling comment block (4 CPU / 1536 MB)
affects:
  - Phase 2 backend health endpoints (will re-point both healthchecks)
  - Phase 6 docker-compose.override.yml.example (lighter-host overrides)
tech-stack:
  added: []
  patterns:
    - healthcheck via in-image binary (busybox wget on backend, node fetch on frontend) - no Dockerfile changes
    - long-form depends_on with service_healthy gate
    - explicit deploy.resources.limits + reservations on every service
key-files:
  modified:
    - compose.yml
key-decisions:
  - "Backend healthcheck uses busybox wget instead of plan-prescribed curl (eclipse-temurin:21-jre-alpine ships wget; zero Dockerfile change wins on smaller surface area)"
  - "Provisional /actuator/health probe will be re-pointed to /healthz in Phase 2 (commented inline)"
  - "Frontend probe uses Node 22 built-in fetch (no extra binary)"
  - "Resource defaults sized for x86_64 home server; ARM/lighter overrides via Phase 6 docker-compose.override.yml.example"
issues-created: []
duration: ~30 min
completed: 2026-05-02
---

# Phase 1 Plan 02: Compose lifecycle hardening

**Three-service healthcheck wiring with frontend.depends_on.backend service_healthy gate, plus 4 CPU / 1536 MB resource ceiling enforced via deploy.resources.limits + reservations.**

## Performance

- **Duration:** ~30 min (plan-only; runtime verification deferred)
- **Started:** 2026-05-02T16:32:44Z
- **Completed:** 2026-05-02T17:00:00Z (estimate; checkpoint resolved as deferred)
- **Tasks:** 2 of 3 executed; Task 3 (human-verify checkpoint) deferred
- **Files modified:** 1 (compose.yml)

## Accomplishments

- Backend service now has a real readiness probe (`wget --spider http://localhost:8080/actuator/health`) inside the container with sensible interval / timeout / retries / start_period values. Eliminates the previous "container is up therefore healthy" assumption.
- Frontend has a process-liveness probe via the Node 22 built-in fetch against `/`. Per the locked-in decision in STATE.md, frontend health = process up only; it does not couple to backend reachability (proxy upstreams them independently).
- `frontend.depends_on.backend` switched from the plain list form to the long form with `condition: service_healthy`. Frontend will no longer start before backend's healthcheck reports healthy, eliminating the early-503 race the previous compose left open.
- `deploy.resources.limits` and `reservations` now constrain every service: db 1.0 CPU / 512 MB, backend 2.0 CPU / 768 MB, frontend 1.0 CPU / 256 MB. Reservations match maintainer's PC primary host with headroom for the Pi (override path documented for Phase 6).
- File-level comment block at the top of `compose.yml` documents the 4 CPU / 1536 MB ceiling and points at the future `docker-compose.override.yml.example` for smaller-host tuning.

## Task Commits

1. **Task 1: backend and frontend healthchecks plus dependency condition** - `5b5b9c1` (feat)
2. **Task 2: resource limits and reservations on all services** - `7dcfb8e` (feat)
3. **Task 3: human-verify checkpoint** - DEFERRED to operator-side verification (see Issues Encountered)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `compose.yml` - healthcheck blocks on backend and frontend, frontend.depends_on long-form with service_healthy gate, deploy.resources.{limits,reservations} on all three services, file-level resource ceiling comment.

## Decisions Made

- **wget over curl for backend probe.** Plan preferred `curl -fsS`. Subagent verified that `eclipse-temurin:21-jre-alpine` already ships busybox `wget`, so the probe became `wget -q --spider http://localhost:8080/actuator/health || exit 1` with zero Dockerfile change. Cleaner than installing curl - smaller image, fewer moving parts.
- **Provisional probe target.** Both healthchecks currently hit endpoints that already exist (`/actuator/health` for backend, `/` for frontend). Phase 2 introduces dedicated `/healthz` and `/livez` endpoints and will re-point these probes. The compose.yml has inline comments flagging the provisional state.
- **Resource defaults sized for primary host.** Limits target the maintainer's x86_64 PC. Operators on ARM single-board hosts will tune via the Phase 6 `docker-compose.override.yml.example`. The top-of-file comment makes this contract explicit.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Switched backend probe binary from curl to wget**

- **Found during:** Task 1 (backend healthcheck wiring)
- **Issue:** Plan preferred `curl` but the JRE base image (`eclipse-temurin:21-jre-alpine`) does not ship curl. Plan permitted a one-line `apk add` in `backend/Dockerfile`, but `Dockerfile` is also a protected file requiring interactive confirmation, which subagents cannot satisfy.
- **Fix:** Used busybox `wget` instead, which is already present in the alpine base. Probe is functionally equivalent for a 200-OK liveness check.
- **Files modified:** `compose.yml`
- **Verification:** Subagent confirmed `wget` presence by inspecting the image filesystem.
- **Committed in:** `5b5b9c1` (Task 1 commit)

### Deferred Enhancements

None.

---

**Total deviations:** 1 auto-fixed (Rule 3 - Blocking). 0 deferred.
**Impact on plan:** Deviation produced a strictly better outcome (no Dockerfile change, no extra package installed, smaller surface area). No scope creep.

## Issues Encountered

- **Task 3 (docker compose human-verify) deferred.** Verification requires `docker compose up -d` against a real `.env` file. The repo's `.env` is gitignored AND blocked from being created via Bash (`pre_guard_secrets.py` hook) AND blocked from being created via Write (Read/Write deny rule on `.env*`). This is the correct security posture - preventing accidental secret commits - but it means Claude in this session cannot run the runtime smoke test.
- The structural correctness was verified by the executing subagent via `python -c "import yaml; yaml.safe_load(open('compose.yml'))"` and visual inspection of the resolved YAML structure (healthchecks present, depends_on long-form, deploy.resources blocks, file-level comment).
- The runtime verification (services reach `healthy` status, MEM LIMIT enforcement via `docker stats`, loopback port binding via `ss/netstat`, host filesystem under `./data/postgres/`) is **deferred to operator-side execution**. The maintainer can run the full 9-step checkpoint locally any time after creating their `.env` from `.env.example`. Planned to be re-validated naturally as part of Phase 12 (Release v0.3.0) which requires a clean `docker compose up -d` against a fresh `.env`.

## Next Phase Readiness

- **Phase 1 closes here.** Compose stack is in full structural compliance with `docs/SELF_HOSTED_CONTRACT.md` sections 3.1-3.7. Provisional healthchecks are in place; they will be tightened in Phase 2.
- **Phase 2 (Backend Health Endpoints) unblocked.** It will add `/healthz` and `/livez` Spring controllers and update the compose healthcheck targets in lockstep.
- **Watch for:** during the first `docker compose up -d` against a real `.env`, if `wget --spider` against `/actuator/health` reports unhealthy because of a non-2xx status while the actuator's downstream sub-checks (e.g. DB sub-check) are still booting, the trivial follow-up is to switch the probe to `wget -qO- ... > /dev/null` (which only fails on connection error, not HTTP status). Note this in the Phase 2 PLAN if observed.

---
*Phase: 01-compose-refactor*
*Completed: 2026-05-02*
