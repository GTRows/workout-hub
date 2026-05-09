# Backup Restore Drill

A backup that has never been restored is not a backup. This runbook walks an operator through a full restore drill against either of the two backup primitives WorkoutHub supports: the opt-in `pg_dump` sidecar (custom-format artifact) or the `scripts/backup.sh` host-cron script (gzip-compressed plain SQL artifact). Per [`SELF_HOSTED_CONTRACT.md`](SELF_HOSTED_CONTRACT.md) Section 5.2, the application puts data where backups can find it but does not perform the backup; backup execution AND backup verification are the operator's responsibility. Recommended cadence: quarterly (every 90 days), or after any change to the backup posture.

## Prerequisites

- Docker and `docker compose` available on the host running this stack.
- At least one backup artifact on disk: either `./data/backups/<date>.dump` (from the opt-in `pg_dump` sidecar) OR `./backups/<date>.sql.gz` (from `scripts/backup.sh`). The drill supports both formats; pick whichever you actually use.
- Free disk space at least 1.5x the size of the backup artifact.
- No production write traffic is required — the drill does NOT touch the live database. It spins up an isolated `postgres:16-alpine` container on host port `55432` (non-default to avoid the live `db` service on `5432`) and tears it down at the end.

## Drill mode A: pg_dump sidecar artifact (`./data/backups/<date>.dump`, custom format)

Step 1. Pick the artifact:

```sh
ls -la ./data/backups/
```

Step 2. Spin up the throwaway container. Bind to `127.0.0.1` per the Self-Hosted Contract loopback default:

```sh
docker run -d --name wh-restore-drill \
    -e POSTGRES_USER=drill \
    -e POSTGRES_PASSWORD=drill \
    -e POSTGRES_DB=drill \
    -p 127.0.0.1:55432:5432 \
    postgres:16-alpine
```

Step 3. Wait for the container to accept connections:

```sh
until docker exec wh-restore-drill pg_isready -U drill -d drill; do sleep 1; done
```

Step 4. Restore the dump via `pg_restore`. The sidecar writes custom-format dumps, which load with `pg_restore`, not `psql`:

```sh
dump=./data/backups/<date>.dump
docker exec -i wh-restore-drill pg_restore \
    -U drill -d drill --clean --if-exists --no-owner --no-privileges \
    < "$dump"
```

`--clean --if-exists` matches the dump's restore semantics. `--no-owner --no-privileges` strips role/privilege metadata so the throwaway's own roles are used. Stderr will contain `role "<original-user>" does not exist` warnings — those are EXPECTED and do NOT indicate a corrupt dump.

Step 5. Assert the schema is intact:

```sh
docker exec wh-restore-drill psql -U drill -d drill -c "\\dt"
```

Expected output includes (at minimum): `users`, `user_profiles`, `refresh_tokens`, `exercises`, `workout_plans`, `workout_days`, `workout_day_exercises`, `workout_sessions`, `session_sets`, `body_metrics`. See [`DATA_SCHEMA.md`](DATA_SCHEMA.md) "Domain map" for the full canonical table list.

Step 6. Assert representative row counts (sanity, not exact):

```sh
docker exec wh-restore-drill psql -U drill -d drill -c \
    "SELECT 'users' AS tbl, COUNT(*) FROM users
     UNION ALL SELECT 'sessions', COUNT(*) FROM workout_sessions
     UNION ALL SELECT 'sets', COUNT(*) FROM session_sets
     UNION ALL SELECT 'metrics', COUNT(*) FROM body_metrics;"
```

The drill PASSES if (a) the query returns four rows, (b) the counts are within an order of magnitude of what the live system reports, and (c) every count is `>= 0` (and `> 0` for `users` if the system has any registered users).

Step 7. Tear down. The drill leaves zero residue:

```sh
docker rm -f wh-restore-drill
```

## Drill mode B: scripts/backup.sh artifact (`./backups/<date>.sql.gz`, plain SQL)

Step 1. Pick the artifact:

```sh
ls -la ./backups/
```

Step 2. Spin up the throwaway container (identical to mode A's step 2):

```sh
docker run -d --name wh-restore-drill \
    -e POSTGRES_USER=drill \
    -e POSTGRES_PASSWORD=drill \
    -e POSTGRES_DB=drill \
    -p 127.0.0.1:55432:5432 \
    postgres:16-alpine
```

Step 3. Wait for readiness (identical to mode A's step 3):

```sh
until docker exec wh-restore-drill pg_isready -U drill -d drill; do sleep 1; done
```

Step 4. Restore via `gunzip | psql`. The script writes plain SQL gzipped, fed to `psql` over stdin:

```sh
dump=./backups/<date>.sql.gz
gunzip -c "$dump" | \
    docker exec -i wh-restore-drill psql -U drill -d drill
```

Role-not-found warnings on stderr are expected, identical to mode A.

Steps 5-7. Assert schema, assert row counts, tear down — identical to mode A's steps 5, 6, and 7. The same `--clean --if-exists` semantics apply (the dump emits `DROP TABLE IF EXISTS` before each `CREATE TABLE`).

## What the drill validates

- The dump file is non-empty and structurally valid (gzip-clean for mode B, pg_dump-custom-clean for mode A).
- Every `CREATE TABLE` / `CREATE INDEX` / `CREATE CONSTRAINT` statement applies cleanly — no schema drift between the dump's snapshot and the Postgres 16 parser.
- At least one user-data row survives the round-trip (the step-6 row-count assertion).

## What the drill does NOT validate

- Application-layer invariants (e.g. `is_pr` recompute correctness on restored sets; the `MetricsService.UpsertResult` 200/201 split). Those are exercised by the backend integration test suite, not by a backup drill.
- Foreign-key cascade behaviour under live write load.
- Backup throughput or recovery-point-objective (RPO). The drill measures restore-ability, not RPO.
- Offsite backup integrity. `scripts/backup.sh` deliberately does not know about remote storage (per [`BACKUP.md`](BACKUP.md)); the operator pushes `.sql.gz` / `.dump` files offsite via restic / rclone / borg / S3-CLI. Offsite verification is separate from this drill.

## Failure modes and triage

- **`pg_restore: error: could not read from input file: end of file`** (mode A): truncated dump. Re-take from the live system; investigate disk-full or container-OOM during the original `pg_dump` run.
- **`gzip: stdin: not in gzip format`** (mode B): file is not a valid gzip. Either a `.sql` plain file misnamed as `.sql.gz`, or a `pg_dump` custom-format file misnamed. Use `file <path>` to confirm.
- **`psql: error: connection to server ... refused`** (both modes): the throwaway container has not finished starting. Re-run step 3 until `pg_isready` returns 0.
- **`CREATE TABLE` fails with `column "X" does not exist`**: schema drift between dump-vintage and the Postgres 16 parser. The dump was taken against a different Postgres major version, or it is corrupted.
- **`\dt` output is empty**: `pg_restore` succeeded but no schema landed. The dump is empty or was taken against an empty database.
- **Row-count assertion returns counts of 0 across the board**: the artifact was taken before any user activity. Retake after some data lands, OR accept zero counts as expected for a fresh-install drill.

## Recommended cadence

- Run the drill **quarterly** (every 90 days) at minimum. Pin to a calendar reminder.
- Run the drill **after any change to the backup posture**: enabling/disabling the `pg_dump` sidecar, moving `BACKUP_DIR`, changing `PG_DUMP_RETENTION_DAYS`, switching from sidecar to host-cron or vice versa.
- Run the drill **before a major upgrade**: Postgres past 16, host migration, restore-from-offsite. This drill does NOT cover Postgres-major-version upgrade compatibility — that is a separate `pg_upgrade` drill, out of scope here.
- Record drill outcomes in your operator runbook (timestamp, artifact filename, row counts, pass/fail). The drill itself is stateless; recording is the operator's job.

## Cross-references

- For backup capture (the half this doc does not cover): [`BACKUP.md`](BACKUP.md).
- For schema reference (which tables to expect in `\dt`): [`DATA_SCHEMA.md`](DATA_SCHEMA.md).
- For deployment context (where artifacts live, what `./data/backups/` is): [`DEPLOYMENT.md`](DEPLOYMENT.md).
- For the binding contract (who owns what): [`SELF_HOSTED_CONTRACT.md`](SELF_HOSTED_CONTRACT.md) Section 5.2.
- For per-version operator upgrade steps: [`MIGRATION.md`](MIGRATION.md).
