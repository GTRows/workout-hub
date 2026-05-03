---
phase: 07-pg-dump-sidecar
plan: 01
subsystem: infra
tags: [postgres, backup, compose, sidecar]
requires:
  - phase: 01-compose-refactor
    provides: bind-mount data root, named network
  - phase: 06-env-vars-and-override
    provides: ENABLE_PG_DUMP / PG_DUMP_SCHEDULE / PG_DUMP_RETENTION_DAYS env doc
provides:
  - opt-in pg_dump sidecar service profile-gated by `backup`
  - bind-mount snapshot output under ./data/backups/
  - cron schedule + day-based retention
affects:
  - Phase 9 README quick start (operator backup how-to)
tech-stack:
  added:
    - prodrigestivill/postgres-backup-local:16
  patterns:
    - "Compose profile gating for opt-in sidecars"
key-files:
  modified:
    - compose.yml
    - .gitignore
  created:
    - data/backups/.gitkeep
key-decisions:
  - "Use maintained third-party image rather than custom alpine+cron build (multi-arch, retention built-in, smaller maintenance surface)"
  - "Profile gate `backup` so plain `docker compose up` never starts the sidecar"
  - "Pin to image major matching db major (Postgres 16) so pg_dump version compatibility is guaranteed"
issues-created: []
duration: ~15 min
completed: 2026-05-02
---

# Phase 7 Plan 01: Opt-in pg_dump backup sidecar

**Profile-gated `pg_dump` sidecar (prodrigestivill/postgres-backup-local:16) writing custom-format snapshots into `./data/backups/` on the operator-defined `PG_DUMP_SCHEDULE`, with day-based retention via `PG_DUMP_RETENTION_DAYS`.**

## Performance

- **Duration:** ~15 min (executed inline)
- **Started:** 2026-05-02T21:30:00Z (estimate)
- **Completed:** 2026-05-02T21:45:00Z (estimate)
- **Tasks:** 1 of 1
- **Files modified:** 2 modified, 1 created

## Accomplishments

- New `pg_dump` service block in `compose.yml` using `prodrigestivill/postgres-backup-local:16` (multi-arch amd64+arm64, maintained, has cron + retention + custom format built-in).
- Profile-gated by `profiles: ["backup"]` so a plain `docker compose up -d` does NOT start the sidecar. Operator opts in via either `COMPOSE_PROFILES=backup` in `.env` or `docker compose --profile backup up -d`.
- Snapshots land in `./data/backups/<date>.dump` via bind mount; the directory tracked on disk via `.gitkeep`. `.gitignore` updated to ignore the contents while keeping the placeholder.
- Healthcheck via the image's built-in HTTP status endpoint (`HEALTHCHECK_PORT=8080` internal-only).
- Resource limits set conservatively (0.5 CPU / 128 MB) so the sidecar does not steal headroom from the application services.
- All knobs flow from `.env` env vars introduced in Phase 6 (`PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS`).

## Task Commits

1. **Task 1: pg_dump sidecar + gitignore + .gitkeep** - `6d37325` (feat)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `compose.yml` - new `pg_dump` service block with profiles + healthcheck + bind mount + resource limits + inline operator opt-in comment
- `.gitignore` - mirror existing `data/postgres/` pattern for `data/backups/` (ignore contents, keep `.gitkeep`)
- `data/backups/.gitkeep` - placeholder so directory exists on fresh clone

## Decisions Made

- **Third-party maintained image vs custom build.** `prodrigestivill/postgres-backup-local` is multi-arch, has cron + retention + custom format built-in, and is widely deployed. Building a custom alpine + cron + pg_dump image would duplicate work for marginal benefit and add maintenance surface. The trade-off is one external dep that Renovate will track.
- **Major version pin.** `:16` matches the db service's Postgres 16 so `pg_dump` version compatibility is guaranteed even when the operator bumps the backup image.
- **Profile gating, not env-flag conditional.** Compose v2 has first-class profiles for opt-in services. Cleaner than parsing `ENABLE_PG_DUMP=true` in a wrapper script. The env var documented in `.env.example` (Phase 6) is now interpreted as "set `COMPOSE_PROFILES=backup` to enable".
- **Day-based retention only.** `BACKUP_KEEP_WEEKS=0` and `BACKUP_KEEP_MONTHS=0` keep retention purely day-based via `BACKUP_KEEP_DAYS=${PG_DUMP_RETENTION_DAYS:-7}`. Operators who want weekly/monthly rolling snapshots can override these env vars in their `.env`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. `python -c "import yaml; ..."` parses the modified compose cleanly; `git check-ignore` confirms `data/backups/anyfile.dump` is ignored while `.gitkeep` is tracked.

## Next Phase Readiness

- **Phase 7 closes here.** Operator-facing backup surface complete: env vars (Phase 6) + service block (Phase 7) + future README/MIGRATION docs (Phase 9).
- **Phase 8 (Dependabot triage) unblocked.** It does not depend on this work; can run in parallel with future phases.

---
*Phase: 07-pg-dump-sidecar*
*Completed: 2026-05-02*
