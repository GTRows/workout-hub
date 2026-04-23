# Backup and Restore

Database backups for WorkoutHub.

## Backup

`scripts/backup.sh` does a `pg_dump` of the `db` service, pipes it through `gzip -9`, and writes `./backups/YYYY-MM-DD.sql.gz`. Anything older than 30 days is purged. Override either with environment variables:

```sh
BACKUP_DIR=/mnt/backups BACKUP_RETENTION_DAYS=90 scripts/backup.sh
```

Schedule it as a host cron:

```
30 2 * * * cd /opt/workouthub && scripts/backup.sh >> /var/log/workouthub-backup.log 2>&1
```

The script uses `docker compose exec`, so the db service must be up. It also validates the dump is non-empty and decompresses cleanly; a zero-byte or truncated backup aborts with a non-zero exit.

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

## Retention

Host disk only holds the last 30 days. Push the `.sql.gz` files offsite (S3, Backblaze, borg-to-rsync target) with an external tool. `scripts/backup.sh` deliberately does not know about remote storage so ops can swap backends freely.
