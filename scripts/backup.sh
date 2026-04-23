#!/usr/bin/env bash
# WorkoutHub database backup.
# Creates ./backups/YYYY-MM-DD.sql.gz from the running "db" service and
# prunes anything older than BACKUP_RETENTION_DAYS (default 30).
#
# Usage:
#   scripts/backup.sh                 # writes into ./backups
#   BACKUP_DIR=/mnt/vol scripts/backup.sh
#
# Authenticates via the db container's own POSTGRES_USER / POSTGRES_DB
# environment variables (already set by the image).

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

BACKUP_DIR="${BACKUP_DIR:-${repo_root}/backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-workouthub}"
DB_SERVICE="${DB_SERVICE:-db}"

mkdir -p "$BACKUP_DIR"

stamp="$(date +%F)"
out="${BACKUP_DIR}/${stamp}.sql.gz"

echo "Dumping -> ${out}"
docker compose -p "$COMPOSE_PROJECT" exec -T "$DB_SERVICE" \
    sh -c 'pg_dump --clean --if-exists --no-owner --no-privileges \
                   -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
    | gzip -9 > "$out"

if [[ ! -s "$out" ]]; then
    echo "ERROR: backup is empty, refusing to proceed" >&2
    rm -f "$out"
    exit 1
fi
gzip -t "$out"

echo "Pruning backups older than ${BACKUP_RETENTION_DAYS} days"
find "$BACKUP_DIR" -maxdepth 1 -type f -name '*.sql.gz' \
    -mtime +"$BACKUP_RETENTION_DAYS" -print -delete

echo "Backup OK: $out ($(du -h "$out" | cut -f1))"
