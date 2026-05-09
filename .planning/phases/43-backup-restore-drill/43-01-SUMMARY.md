---
phase: 43-backup-restore-drill
plan: 01
type: docs-runbook
domain: docs
status: complete
---

# Phase 43 - Plan 01 Summary

**Milestone:** v1.0 Release Hardening
**Phase:** 43 - backup-restore-drill
**Plan:** 43-01 (only sub-plan)
**Status:** Complete
**Executed:** 2026-05-09

## Goal restatement

Land the three documentation deliverables the v1.0 roadmap names for Phase 43
in a single docs-only sub-plan: author the new operator-runnable
`docs/BACKUP_RESTORE_DRILL.md`, refresh `docs/BACKUP.md` to document both
backup-cron primitives plus a "Verify your backups" cross-link, and replace
the `docs/MIGRATION.md` `## vNext` placeholder with a populated `## v1.0.0`
section. Two cross-link adds (`docs/DEPLOYMENT.md` "What's not here" and
`README.md` Documentation map) keep the doc cluster internally consistent.

## What landed

Five atomic commits, all under conventional-commit scope `docs(43-01)`:

1. `60b1a56` `docs(43-01): add BACKUP_RESTORE_DRILL.md operator runbook`
   - New 146-line file. Sections: lead paragraph (cadence, contract
     reference), Prerequisites, Drill mode A (pg_dump sidecar
     `./data/backups/<date>.dump` via `pg_restore`), Drill mode B
     (`scripts/backup.sh` `./backups/<date>.sql.gz` via `gunzip | psql`),
     What the drill validates, What the drill does NOT validate, Failure
     modes and triage (six entries), Recommended cadence, Cross-references.
     Throwaway container is `wh-restore-drill` on host port `127.0.0.1:55432`
     to avoid clashing with the live `db` service. Tear-down at the end
     leaves zero residue.

2. `bc32625` `docs(43-01): document pg_dump sidecar cron and verify-your-backups in BACKUP.md`
   - File grew from 62 to 91 lines. Lead paragraph added documenting the
     two backup primitives ("pick ONE per host"). New `### Cron via the
     pg_dump sidecar (in-container schedule)` sub-section explains that the
     sidecar schedules itself via `PG_DUMP_SCHEDULE` and does NOT need a
     host cron. New `## Verify your backups` sub-section before
     `## Retention` cross-links the drill doc and recommends quarterly
     cadence. Existing host-cron line (`30 2 * * * ...`) and Restore
     section preserved verbatim.

3. `c747a0d` `docs(43-01): add v1.0.0 entry to MIGRATION.md and preserve vNext template`
   - File grew from 297 to 365 lines. New `## v1.0.0` section captures
     Phase 38-44 scope (e2e, testcontainers 2.x, framework-majors
     re-deferral, Netty 4.2.13.Final / i-14 closure, docs completion,
     drill runbook, release cut). Subsections: Required env var changes
     (zero), Schema and data migration (zero new, V28 is head), Compose /
     runtime changes (Netty bump + CVE-2026-42577 closure +
     `.trivyignore` cleanup), One-shot commands (standard two-step pull +
     up), Rollback (revert + redeploy v0.6.0 + optional fresh pg_dump).
     `## vNext` placeholder re-added below the v1.0.0 section with the
     template-to-copy block preserved verbatim. v0.3.0 + v0.4.0 sections
     unmodified.

4. `cf7ba13` `docs(43-01): cross-link BACKUP_RESTORE_DRILL.md from DEPLOYMENT What's not here`
   - File grew from 217 to 219 lines. Added one bullet for
     `BACKUP_RESTORE_DRILL.md` directly after the existing `BACKUP.md`
     bullet in the "What's not here" section. Description: "Backup-restore
     drill (operator-runnable verification)".

5. `0dd179e` `docs(43-01): add BACKUP_RESTORE_DRILL.md row to README Documentation map`
   - File grew from 180 to 181 lines. The Documentation map is a
     two-column table; the new row goes directly after the BACKUP.md row
     and before the MIGRATION.md row, matching the table's existing
     topic-grouped order. Description: "Operator-runnable runbook for
     verifying backup integrity (quarterly drill cadence)."

## Verification gate evidence

All six plan-defined assertions pass at HEAD `0dd179e`:

1. **BACKUP_RESTORE_DRILL.md read-back.** 146 lines (band 100-200).
   Required literals all present: `wh-restore-drill`, `pg_restore`, `\dt`,
   `--clean --if-exists`, `Drill mode A`, `Drill mode B`, `quarterly`,
   `BACKUP.md`, `DATA_SCHEMA.md`, `SELF_HOSTED_CONTRACT.md`.

2. **BACKUP.md read-back.** 91 lines (band 80-105). Required literals all
   present: `BACKUP_RESTORE_DRILL.md`, `pg_dump sidecar`, `quarterly`,
   `30 2 * * *` host-cron line preserved verbatim.

3. **MIGRATION.md read-back.** 365 lines (band 320-365). Exactly one
   `## v1.0.0` heading; exactly one `## vNext` heading. Required literals
   all present in v1.0.0 section: `BACKUP_RESTORE_DRILL.md`, `Netty`,
   `4.2.13.Final`, `CVE-2026-42577`. v0.3.0 + v0.4.0 sections unmodified
   (verified by Grep against `## v0.3.0`, `## v0.4.0`, `db_data`, `V26`,
   `V27`).

4. **DEPLOYMENT.md read-back.** 219 lines (band 217-222).
   `BACKUP_RESTORE_DRILL.md` present at line 210 in the "What's not here"
   section. Existing `BACKUP.md` cross-link bullet at line 208 preserved.

5. **README.md read-back.** 181 lines (band 180-185).
   `BACKUP_RESTORE_DRILL.md` present in the Documentation map table.
   Existing 9 entries preserved; the new entry is the 10th, inserted
   directly after the BACKUP.md row.

6. **Protected-files audit via `git status --porcelain`.** Diff covers
   exactly the five expected paths (plus the untracked phase folder for
   this plan's PLAN.md and SUMMARY.md). Zero changes under `pom.xml`,
   `compose.yml`, `package.json`, `pnpm-lock.yaml`, `.github/workflows/**`,
   `scripts/**`, `IDENTITY.yaml`, `CHANGELOG.md`, `RELEASE.md`. Zero
   changes under `backend/src/`, `frontend/src/`, or
   `backend/src/main/resources/db/migration/`.

## Notes for downstream phases

- **Phase 44 (release-v1-0)** owns CHANGELOG.md rotation; this plan
  intentionally did not touch it. The Phase 43 entry will be folded into
  the v1.0.0 release-notes block as a single
  "docs: backup-restore drill runbook + MIGRATION v1.0.0 entry" line.
- **MIGRATION.md `## v1.0.0` summary paragraph** is forward-looking on
  Phases 38-40 + 44 (which had not shipped at execute time). Per the
  plan's Risk #5, the executor adapted the prose to STATE.md's
  observed reality (Phase 41 + 42 + 43 locked-in; Phase 38 + 39 + 40 + 44
  outstanding). If those upstream phases' deliverables diverge from the
  current paragraph (e.g. i-6 closure path changes during Phase 40), the
  v1.0.0 summary paragraph is iterable in Phase 44.
- **Drift surface introduced.** `docs/BACKUP_RESTORE_DRILL.md` is a new
  load-bearing operator-runbook artifact. The next time the backup
  primitives change (sidecar image bump, `scripts/backup.sh` semantics,
  `pg_restore` flag set), this doc plus `docs/BACKUP.md` are the touch
  points.
- **No new issue opened.** The plan-author considered i-16 ("automate the
  backup-restore drill via CI") and rejected it (would require
  `.github/workflows/**` + `scripts/**` + a stored test artifact, all
  protected). Status quo: drill is operator-driven, manual, quarterly.

## Files

- `docs/BACKUP_RESTORE_DRILL.md` (new, 146 lines)
- `docs/BACKUP.md` (modified, +30 lines net)
- `docs/MIGRATION.md` (modified, +69 lines net; `## vNext` template preserved)
- `docs/DEPLOYMENT.md` (modified, +2 lines net)
- `README.md` (modified, +1 line net)
- `.planning/phases/43-backup-restore-drill/43-01-SUMMARY.md` (this file)
