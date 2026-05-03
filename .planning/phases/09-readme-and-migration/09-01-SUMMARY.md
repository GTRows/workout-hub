---
phase: 09-readme-and-migration
plan: 01
subsystem: docs
tags: [readme, migration, operator, contract]
requires:
  - phase: 06-env-vars-and-override
    provides: canonical .env.example
  - phase: 03-structured-logging
  - phase: 04-prometheus-metrics
  - phase: 05-forward-auth-mode
  - phase: 07-pg-dump-sidecar
provides:
  - operator-facing README per contract section 11
  - docs/MIGRATION.md with v0.3.0 entry per contract section 10.2
  - vNext placeholder template for future releases
affects:
  - Phase 12 release v0.3.0 (release notes derive from CHANGELOG which references this MIGRATION entry)
tech-stack:
  added: []
  patterns:
    - "README order locked to contract section 11 subsections"
    - "MIGRATION format: per-release entry with env-var changes + commands + rollback"
key-files:
  created:
    - docs/MIGRATION.md
  modified:
    - README.md
key-decisions:
  - "README points at .env.example for full env reference rather than duplicating the table - one source of truth"
  - "Reverse-proxy snippets shipped as examples for operators (Caddy / Traefik / nginx) but explicitly NOT defaults"
  - "MIGRATION volume rename uses placeholder `<project>_db_data` and instructs operator to `docker volume ls | grep db_data` rather than asserting a specific Compose-namespaced name"
issues-created: []
duration: ~20 min
completed: 2026-05-03
---

# Phase 9 Plan 01: README and MIGRATION docs

**Operator-facing README rewritten per contract section 11 (six subsections in order, plus reference links) and `docs/MIGRATION.md` seeded with the v0.3.0 entry covering every breaking and additive change Phase 1-8 shipped.**

## Performance

- **Duration:** ~20 min (subagent execution)
- **Started:** 2026-05-03T01:00:00Z (estimate)
- **Completed:** 2026-05-03T01:20:00Z (estimate)
- **Tasks:** 2 of 2
- **Files modified:** 1 modified, 1 created

## Accomplishments

- `README.md` rewritten from scratch (was leftover claude-code-template README). 165 lines covering Description, Quick Start, Configuration, Exposure (Caddy + Traefik + nginx examples), Data and backup, Updating, Reference. Contract section 11 verbatim ordering.
- `docs/MIGRATION.md` created. 188 lines. Header explains the per-release format. v0.3.0 entry covers: env var additions/removals, named-volume to bind-mount data migration with exact `docker run` extract+restore commands, OIDC client removal + forward-auth migration path, healthcheck endpoint rename, ECS JSON log format change, /metrics endpoint addition, opt-in pg_dump sidecar enable instructions, and explicit rollback recipe (revert merge SHA + restore last pg_dump). vNext placeholder + copy-paste template appended for future releases.
- README "Reference" section links every existing `docs/*.md`, validated by Glob.

## Task Commits

1. **Task 1: README rewrite** - `5b2f48e` (docs)
2. **Task 2: MIGRATION seed** - `b28be0b` (docs)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `README.md` - operator-facing rewrite per contract section 11
- `docs/MIGRATION.md` - new; v0.3.0 entry plus vNext template

## Decisions Made

- **Single source of truth for env vars.** README's Configuration section summarizes groups and points at `.env.example` for the full one-comment-per-var reference rather than duplicating the table inline. Avoids drift between the two files.
- **Reverse-proxy examples not defaults.** Each snippet (Caddy / Traefik / nginx) is presented as an operator example, with the contract reminder that the application speaks plain HTTP and the proxy terminates TLS.
- **Volume migration uses dynamic naming.** The MIGRATION recipe says `docker volume ls | grep db_data` to find the Compose-namespaced volume name rather than asserting a specific value, since the prefix depends on the operator's project directory name.

## Deviations from Plan

- **`docs/BACKUP.md` describes a legacy gzip cron pipeline, not a restic setup.** The README "Data and backup" section calls it a "reference offsite-backup setup" without naming restic specifically, since the file's actual content does not match the maintainer's homelab restic stack. Worth a follow-up to update `docs/BACKUP.md` separately.

## Issues Encountered

None.

## Next Phase Readiness

- **Phase 9 closes here.** Operator now has a single entry point (README) + a per-version migration log (MIGRATION.md).
- **Phase 10 (CI gates) unblocked.** It needs no docs prerequisite.
- **Phase 12 release benefit.** The release workflow extracts notes from CHANGELOG; CHANGELOG can now reference MIGRATION sections for operator details.

---
*Phase: 09-readme-and-migration*
*Completed: 2026-05-03*
