# Backup and Restore

Database backups for WorkoutHub.

WorkoutHub supports two backup primitives; pick ONE per host. Mode A is the
opt-in `pg_dump` sidecar shipped in `compose.yml`, scheduled internally by
the container itself (no host cron). Mode B is the existing
`scripts/backup.sh`, driven by host cron. Per
[`SELF_HOSTED_CONTRACT.md`](SELF_HOSTED_CONTRACT.md) Section 5.2 the
operator may also opt for (c) external host-snapshot tooling against
`./data/postgres/` and skip both. Whichever you pick, verify it (see
"Verify your backups" below).

## Backup

### Cron via scripts/backup.sh (host cron)

`scripts/backup.sh` does a `pg_dump` of the `db` service, pipes it through `gzip -9`, and writes `./backups/YYYY-MM-DD.sql.gz`. Anything older than 30 days is purged. Override either with environment variables:

```sh
BACKUP_DIR=/mnt/backups BACKUP_RETENTION_DAYS=90 scripts/backup.sh
```

Schedule it as a host cron:

```
30 2 * * * cd /opt/workouthub && scripts/backup.sh >> /var/log/workouthub-backup.log 2>&1
```

The script uses `docker compose exec`, so the db service must be up. It also validates the dump is non-empty and decompresses cleanly; a zero-byte or truncated backup aborts with a non-zero exit.

### Cron via the pg_dump sidecar (in-container schedule)

The opt-in `pg_dump` sidecar service in `compose.yml` runs the
`prodrigestivill/postgres-backup-local:16` image and schedules itself from
inside the container. No host cron is needed. Enable with
`COMPOSE_PROFILES=backup` plus `ENABLE_PG_DUMP=true` in `.env`; tune the
schedule with `PG_DUMP_SCHEDULE` (cron 5-field syntax, default
`0 3 * * *` daily at 03:00) and retention with `PG_DUMP_RETENTION_DAYS`
(default `7`). Snapshots land under `./data/backups/<date>.dump` in
`pg_dump --format=custom`, and are restored with `pg_restore`, not `psql`.

## Restore

A restore wipes the target database. Use carefully.

### Into the live stack

Stop anything that writes, then feed the dump back through psql:

```sh
docker compose stop backend
gunzip -c backups/2026-04-23.sql.gz | \
    docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
docker compose start backend
```

The dump is produced with `--clean --if-exists`, so it drops existing tables before re-creating them.

### Dry-run on a fresh container

To verify a backup before trusting it, restore into a throwaway container:

```sh
dump=backups/2026-04-23.sql.gz
docker run -d --name wh-restore-check \
    -e POSTGRES_USER=check \
    -e POSTGRES_PASSWORD=check \
    -e POSTGRES_DB=check \
    postgres:16-alpine
# Wait a few seconds for postgres to accept connections.
until docker exec wh-restore-check pg_isready -U check; do sleep 1; done
gunzip -c "$dump" | \
    docker exec -i wh-restore-check psql -U check -d check
docker exec wh-restore-check psql -U check -d check -c "\\dt"
docker rm -f wh-restore-check
```

If the `\dt` listing shows `users`, `workout_plans`, `workout_sessions`, `session_sets`, `body_metrics`, etc., the backup is good.

## Verify your backups

A backup that has never been restored is not a backup.

Run the full restore drill at [`BACKUP_RESTORE_DRILL.md`](BACKUP_RESTORE_DRILL.md) on a quarterly cadence (every 90 days), and after any change to the backup posture (sidecar enable/disable, host-cron move, retention adjustment, host migration). The drill covers both backup primitives — the `pg_dump` sidecar's `./data/backups/<date>.dump` and the script's `./backups/<date>.sql.gz` — and ends with explicit schema and row-count assertions plus a tear-down step.

The "Dry-run on a fresh container" recipe above is the seed of that drill doc; the drill adds the assertion, tear-down, and sidecar-mode steps the seed does not cover. You may still use the seed recipe for a quick sanity check, but run the full drill at the recommended cadence.

## Retention

Host disk only holds the last 30 days. Push the `.sql.gz` files offsite (S3, Backblaze, borg-to-rsync target) with an external tool. `scripts/backup.sh` deliberately does not know about remote storage so ops can swap backends freely.
