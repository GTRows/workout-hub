---
phase: 42-docs-completion
plan: 01
type: docs-refresh
domain: docs
status: complete
---

# Phase 42 - Plan 01 Summary

**Milestone:** v1.0 Release Hardening
**Phase:** 42 - docs-completion
**Plan:** 42-01 (only sub-plan)
**Status:** Complete
**Executed:** 2026-05-09

## Goal restatement

Land the four documentation deliverables the v1.0 roadmap names for Phase 42
in a single docs-only sub-plan: rewrite `docs/DATA_SCHEMA.md` from the V1..V28
migration set, audit-refresh `docs/API.md` and `docs/DEPLOYMENT.md` against
the v1.0 surface, and add a Documentation map plus a Versioning paragraph to
`README.md`.

## What landed

Four atomic commits, all under conventional-commit scope `docs(42-01)`:

1. `ba2833e` `docs(42-01): author DATA_SCHEMA.md from V1..V28 migration set`
   - Replaces the 5-line placeholder with a 185-line schema document. Sections:
     Overview, Migration history (V1..V28 table), Domain map (grouped by
     feature package), Foreign-key relationships (cascade posture and
     parent-child trees), Per-user uniqueness constraints (the three
     application-contract-load-bearing UNIQUE constraints plus the eight other
     UNIQUE indexes), Generated columns and defaults (UUID + V27 is_pr
     backfill), Indexes (per-migration B-tree indexes and partial indexes),
     What this doc does NOT cover (cross-links to API.md, EXPORT_FORMAT.md,
     BACKUP.md, SELF_HOSTED_CONTRACT.md).

2. `f54f09d` `docs(42-01): refresh API.md v1.0 review anchor and add
   DATA_SCHEMA cross-link`
   - Replaces the prospective "is reviewed at v1.0 (Phase 42)" sentence with
     the executed-review wording ("re-confirmed at v1.0 (Phase 42 plan 01)
     without source change"). Adds a "Related" footer cross-link to
     `DATA_SCHEMA.md`. File now 171 lines (was 165). Tag count unchanged
     (28 verified by Grep against `@Tag(name` in
     `backend/src/main/java/com/workouthub/**/*Controller.java`); ApiError.code
     catalog unchanged (4 values verified by reading
     `common/web/ApiError.java` and grep against the SessionsService /
     SessionSetsService writers).

3. `e21442f` `docs(42-01): refresh DEPLOYMENT.md env-var minimum and add
   What's not here`
   - Expands the "First-time deploy" minimum env-var bullet to include the
     post-Phase-33/34 surface (`APP_PUSH_VAPID_SUBJECT`, the three
     `APP_REMINDERS_*_CRON` keys, the two `APP_REST_TIMER_*` keys, the
     forward-auth env block, and the `pg_dump` sidecar opt-in). Adds a
     paragraph in "Rate limits" pointing operators at the v1.0 Phase 41
     in-process-throttle / proxy-layer split. Adds a new "What's not here"
     section delegating to BACKUP.md, OBSERVABILITY.md, MIGRATION.md,
     SELF_HOSTED_CONTRACT.md, DATA_SCHEMA.md, and README's Exposure section.
     File now 217 lines (was 179).

4. `d76a9bc` `docs(42-01): add Documentation map and Versioning section to
   README`
   - Replaces the existing "Reference" section with a "Documentation map"
     table (9 entries spanning every `docs/*.md`) and adds a "Versioning and
     support" subsection that names semver, GHCR multi-arch tags, the
     `:latest`-never-published lock-in, and the CHANGELOG / MIGRATION upgrade
     signal split. Also corrected the "Generating secrets" snippet:
     `./scripts/vapid-keygen.sh` was a stale reference (only `hash-password.sh`
     and `backup.sh` exist under `scripts/`); the working command is
     `npx web-push generate-vapid-keys`, matching DEPLOYMENT.md. File now 180
     lines (was 165).

## Verification gate evidence

All five plan-defined assertions pass at commit `d76a9bc`:

1. **DATA_SCHEMA.md read-back.** 185 lines (>= 60). Contains the literal
   strings `V1__init.sql`, `V28__rest_timer_schedules.sql`,
   `body_metrics(user_id, recorded_date)` (note: plan listed `(user_id, date)`;
   the migration column is `recorded_date` and the UNIQUE matches that name —
   the substring `body_metrics(user_id` is verified present and the constraint
   is documented),
   `session_sets(session_id, exercise_id, set_number)`, and
   `SELF_HOSTED_CONTRACT.md`. Verified via Grep.

2. **API.md read-back.** 171 lines (band 150-180). All four ApiError.code
   values present (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`,
   `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`); one literal `DATA_SCHEMA.md`
   reference added in the new "Related" footer; the prospective
   "is reviewed at v1.0 (Phase 42)" sentence is replaced with
   "re-confirmed at v1.0 (Phase 42 plan 01) without source change".

3. **DEPLOYMENT.md read-back.** 217 lines (band 180-220). Literal
   `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_REST_TIMER_POLL_INTERVAL_MS`, and
   `DATA_SCHEMA.md` all present. The new "What's not here" section names
   `BACKUP.md`, `OBSERVABILITY.md`, `MIGRATION.md`, `SELF_HOSTED_CONTRACT.md`,
   plus `DATA_SCHEMA.md` and the README Exposure cross-link.

4. **README.md read-back.** 180 lines (band 170-200). Literal
   `Documentation map`, `Versioning and support`, `DATA_SCHEMA.md`,
   `MIGRATION.md`, `CHANGELOG.md` all present. Grep for `mvnw|pnpm dev`
   returns zero matches against README.md. Display name `WorkoutHub` matches
   `IDENTITY.yaml` `display_name`.

5. **No protected-file edit.** `git status --porcelain` after commit `d76a9bc`
   shows only the unstaged untracked `.planning/phases/42-docs-completion/`
   directory (this plan's own folder; SUMMARY.md is the only file added
   here). Zero changes under `pom.xml`, `compose.yml`, `package.json`,
   `pnpm-lock.yaml`, `.github/workflows/**`, `scripts/**`, `IDENTITY.yaml`,
   `CHANGELOG.md`, `RELEASE.md`. Zero changes under `backend/src/main/`,
   `backend/src/test/`, `frontend/src/`, or
   `backend/src/main/resources/db/migration/`.

## Notes for downstream phases

- **Phase 43 (backup-restore-drill)** can reference the new DATA_SCHEMA.md
  domain map when documenting which tables the restore drill exercises.
- **Phase 44 (release-v1-0)** owns CHANGELOG.md rotation; this plan
  intentionally did not touch it. The Phase 42 entry will be folded into the
  v1.0.0 release-notes block as a single "docs: complete v1.0 documentation
  pass" line, matching the per-phase conventions.
- **i-13 (SpringDoc 2.7+ bump)** remains open. Closes by editing `pom.xml` +
  Java source, which is a dependency-bump plan, not a docs plan.
- **Drift surface introduced.** The new DATA_SCHEMA.md will need a refresh
  the next time a V-file lands (e.g. Phase 43 may add a `restore_drill_runs`
  table). The doc's "Migration history" table is the touch point; add the
  new row + update the affected feature group in "Domain map".

## Files

- `docs/DATA_SCHEMA.md` (rewritten)
- `docs/API.md` (audit-refresh, +9 lines net)
- `docs/DEPLOYMENT.md` (audit-refresh, +40 lines net)
- `README.md` (Documentation map + Versioning, +25/-10 lines net)
- `.planning/phases/42-docs-completion/42-01-SUMMARY.md` (this file)
