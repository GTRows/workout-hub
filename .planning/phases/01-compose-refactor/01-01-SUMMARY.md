---
phase: 01-compose-refactor
plan: 01
subsystem: infra
tags: [docker, compose, postgres, bind-mount, network]
requires: []
provides:
  - parametric ports
  - named network
  - bind-mount data
  - loopback default
affects:
  - Phase 2 healthchecks
  - Phase 6 env vars
  - Phase 7 pg_dump sidecar
  - Phase 9 migration docs
tech-stack:
  added: []
  patterns:
    - bind-mount under ./data/<component>/
key-files:
  modified:
    - compose.yml
    - .gitignore
  created:
    - data/.gitkeep
    - data/postgres/.gitkeep
key-decisions:
  - BACKEND_PORT renamed from SERVER_PORT for compose-level clarity
  - FRONTEND_PORT introduced
  - loopback default per contract
duration: ~25 minutes
completed: 2026-05-02
---

# Phase 1 Plan 01: Compose structural refactor

Loopback-defaulted parametric ports, workouthub-net named network, and bind-mount data/postgres/ replacing named volume.

## Accomplishments

- All three service port bindings rewritten to `${BIND_ADDR:-127.0.0.1}:${SVC_PORT:-DEFAULT}:CONTAINER_PORT` form so a fresh `docker compose up` binds only to loopback per self-hosted contract section 3.2.
- BACKEND_PORT introduced at the compose level (replacing SERVER_PORT), FRONTEND_PORT introduced (replacing the literal `3000:3000`), POSTGRES_PORT preserved.
- Explicit `workouthub-net` named network declared and attached to db, backend, and frontend per contract section 3.3, eliminating reliance on the implicit default bridge.
- Postgres data volume converted from named Docker volume `db_data` to bind mount `./data/postgres/:/var/lib/postgresql/data` per contract section 3.4; top-level `volumes:` block removed.
- `.gitignore` extended so the `data/` operator-state tree is ignored except for the `.gitkeep` placeholders, ensuring the directory layout exists on a fresh clone.

## Files Created/Modified

- `compose.yml` - parametric port bindings on all three services, workouthub-net named network attached, db volumes switched to bind mount, top-level `volumes:` block removed.
- `.gitignore` - new "Self-hosted bind-mount data root" section ignoring `data/` contents while tracking `.gitkeep` files.
- `data/.gitkeep` - placeholder so the bind-mount root exists on fresh clones.
- `data/postgres/.gitkeep` - placeholder so the Postgres data dir exists on fresh clones.

## Decisions Made

- **BACKEND_PORT vs SERVER_PORT.** Renamed at the compose layer for unambiguous per-service env var naming. The backend application still reads `SERVER_PORT` internally; this is a compose-only rename and does not affect Spring Boot config.
- **FRONTEND_PORT introduced.** Replaces the literal `3000:3000` so the frontend host port is parametric like the others.
- **Loopback default.** `BIND_ADDR` defaults to `127.0.0.1` exactly as the contract requires; operators opt into LAN/Tailnet exposure via their `.env`.
- **Gitignore pattern deviation.** Plan-prescribed text `data/` + `!data/.gitkeep` + `!data/postgres/.gitkeep` does not work because Git cannot re-include files under an excluded parent directory. Replaced with `data/*` + `!data/.gitkeep` + `!data/postgres/` + `data/postgres/*` + `!data/postgres/.gitkeep`, which preserves the plan's intent and satisfies both verification checks. Logged as `.planning/ISSUES.md` i-3.

## Issues Encountered

- `docker compose -f compose.yml config` could not be run as a verification step because the compose file's own `env_file: - .env` directive requires a real `.env`, and the repo pre-commit hook blocks creating one (correctly - it would risk committing secrets). Fell back to the python `yaml.safe_load` validation listed in the user's instructions as the explicit fallback. All structural assertions pass: top-level keys are `services` + `networks` only, all three services have `workouthub-net` attached, all three port bindings start with `${BIND_ADDR:-127.0.0.1}:`, and `db.volumes` references the bind mount.
- Gitignore pattern from the plan body required correction (see Decisions above).

## Next Step

Ready for `01-02-PLAN.md` (lifecycle hardening: healthchecks + resource limits).
