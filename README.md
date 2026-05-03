# WorkoutHub

WorkoutHub is a self-hosted multi-user fitness tracker: weekly workout plans, live session execution, body metrics, progress charts, and JSON export for AI coach analysis. The backend is Java 21 + Spring Boot 3 + PostgreSQL 16; the frontend is Next.js 15 + React 19 + TypeScript, mobile-first PWA. Operators run the stack via Docker Compose against the contract documented in [`docs/SELF_HOSTED_CONTRACT.md`](docs/SELF_HOSTED_CONTRACT.md). The application speaks plain HTTP, binds to loopback by default, and assumes a reverse proxy in front for TLS, DNS, and (optional) SSO.

## Quick start

Smallest copy-pasteable recipe for a fresh local instance:

```bash
git clone https://github.com/GTRows/workout-hub.git
cd workout-hub
cp .env.example .env

# Required edits in .env (at minimum):
#   POSTGRES_PASSWORD       openssl rand -base64 32
#   APP_JWT_SECRET          openssl rand -base64 48
#   APP_ADMIN_PASSWORD_HASH ./scripts/hash-password.sh 'your-strong-password'

docker compose up -d
# Visit http://127.0.0.1:3000  (admin email defaults to admin@workouthub.local)
```

`BIND_ADDR` defaults to `127.0.0.1` so a fresh `docker compose up` is reachable only from the host. To expose the stack on a LAN or VPN interface, set `BIND_ADDR` to the chosen interface IP in `.env` (never `0.0.0.0`).

For host-local tweaks that should not be committed (resource limits on a Raspberry Pi, extra dev mounts), copy `docker-compose.override.yml.example` to `docker-compose.override.yml` and edit. Compose v2 auto-merges the override on top of `compose.yml`; the override file is gitignored.

## Configuration

All runtime configuration is read from environment variables. [`.env.example`](.env.example) is the canonical reference: every variable the application reads, grouped by section, with a one-line comment per variable describing purpose, format, and whether it is a secret. Copy it to `.env` and edit.

The variables are organized into the following groups:

| Group | Purpose |
|-------|---------|
| Compose host binding and port mapping | `BIND_ADDR`, `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT` |
| PostgreSQL service | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| Backend (Spring Boot) | `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`, `SPRING_DATASOURCE_*`, `APP_JWT_*`, `APP_CORS_ALLOWED_ORIGINS` |
| Admin bootstrap | `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD_HASH`, `APP_ADMIN_DISPLAY_NAME` |
| Authentication mode | `APP_AUTH_MODE`, `APP_AUTH_TRUSTED_PROXIES`, `APP_AUTH_HEADER_*` |
| Web Push (VAPID) | `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_PUSH_VAPID_SUBJECT` |
| Reminder cron schedules | `APP_REMINDERS_WORKOUT_CRON`, `APP_REMINDERS_WEIGHT_CRON`, `APP_REMINDERS_SUPPLEMENT_CRON` |
| Optional pg_dump backup sidecar | `ENABLE_PG_DUMP`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS` |
| Frontend (Next.js) | `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_DEFAULT_LOCALE` |

### Generating secrets

The application never ships real secrets. Generate them on the host before bringing the stack up:

```bash
# Postgres password (any length you like; 32 random bytes is plenty)
openssl rand -base64 32

# JWT signing key for built-in auth (>= 32 bytes for HS256; 48 bytes recommended)
openssl rand -base64 48

# Admin password hash (BCrypt; pass the plain password as the only argument)
./scripts/hash-password.sh 'your-strong-password'

# VAPID keypair for Web Push (writes both keys to stdout)
./scripts/vapid-keygen.sh
```

Copy the outputs into the matching `.env` keys. `.env` is gitignored.

### Resource limits

`compose.yml` ships defaults sized for a single-host x86_64 home server (total ceiling ~4 CPU + 1.5 GB across the three services). For smaller hosts (Raspberry Pi 4, 1 GB VPS) override the limits in `docker-compose.override.yml` — see `docker-compose.override.yml.example` for an ARM-friendly starting point.

## Exposure

The application terminates plain HTTP. **The reverse proxy terminates TLS, sets forwarded headers, and decides on hostname-based routing.** The snippets below are operator-facing examples assuming `BIND_ADDR=127.0.0.1` so the proxy and the stack share a host; they are not "the official wiring" — pick whichever proxy fits your environment.

Replace `${PUBLIC_HOSTNAME}` with the public DNS name and adjust the upstream ports if you set `BACKEND_PORT` / `FRONTEND_PORT` to non-defaults.

### Caddy

```caddy
${PUBLIC_HOSTNAME} {
    encode zstd gzip

    handle /api/* {
        reverse_proxy 127.0.0.1:8080
    }

    handle {
        reverse_proxy 127.0.0.1:3000
    }
}
```

### Traefik (compose labels on a separate proxy stack)

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.workouthub-fe.rule=Host(`${PUBLIC_HOSTNAME}`)"
  - "traefik.http.routers.workouthub-fe.entrypoints=websecure"
  - "traefik.http.routers.workouthub-fe.tls.certresolver=letsencrypt"
  - "traefik.http.services.workouthub-fe.loadbalancer.server.port=3000"

  - "traefik.http.routers.workouthub-api.rule=Host(`${PUBLIC_HOSTNAME}`) && PathPrefix(`/api`)"
  - "traefik.http.routers.workouthub-api.entrypoints=websecure"
  - "traefik.http.routers.workouthub-api.tls.certresolver=letsencrypt"
  - "traefik.http.services.workouthub-api.loadbalancer.server.port=8080"
```

### nginx

```nginx
server {
    listen 443 ssl http2;
    server_name ${PUBLIC_HOSTNAME};

    ssl_certificate     /etc/letsencrypt/live/${PUBLIC_HOSTNAME}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${PUBLIC_HOSTNAME}/privkey.pem;

    proxy_set_header Host              $host;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host  $host;
    proxy_set_header X-Request-Id      $request_id;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
    }

    location / {
        proxy_pass http://127.0.0.1:3000;
    }
}
```

When using forward-auth SSO (Authentik, Authelia, oauth2-proxy), set `APP_AUTH_MODE=forward-auth` and add the proxy CIDR to `APP_AUTH_TRUSTED_PROXIES`. The application trusts identity headers ONLY from those CIDRs — see contract section 7.2.

## Data and backup

All persistent state lives under `./data/` as bind mounts. The directory is gitignored except for `.gitkeep` markers.

| Path | Contents | Backed up by |
|------|----------|--------------|
| `./data/postgres/` | PostgreSQL 16 cluster (database state). | Operator. Either host-level snapshots of the directory or pg_dump (see below). |
| `./data/backups/` | `pg_dump --format=custom` snapshots when the opt-in sidecar is enabled. | Operator's offsite tool (restic, rclone, etc.). |

The `pg_dump` sidecar is **opt-in** and disabled by default. Enable it by setting `COMPOSE_PROFILES=backup` in `.env` (or `docker compose --profile backup up -d`). It runs the `prodrigestivill/postgres-backup-local:16` image on `PG_DUMP_SCHEDULE` (default `0 3 * * *`, daily at 03:00) and prunes snapshots older than `PG_DUMP_RETENTION_DAYS` (default 7).

The application does **not** push backups offsite — that is the operator's responsibility. For a reference setup using restic, see [`docs/BACKUP.md`](docs/BACKUP.md). Sessions and other purely transient state are not in `./data/`; losing them just logs users out.

Recommended RPO: daily snapshots at minimum. Live workout sessions are written transactionally to Postgres; offline sets queued in the browser's IndexedDB are flushed on reconnect, so losing the latest dump costs at most one day of body-metrics entries and any sessions completed since the last dump.

## Updating

- [`CHANGELOG.md`](CHANGELOG.md) — user-visible changes per release (Keep a Changelog format).
- [`docs/MIGRATION.md`](docs/MIGRATION.md) — per-version operator steps: env vars added or removed, schema migrations, one-shot commands, and the rollback recipe. Read this before bumping.

Updates flow through the operator's deployment repository (Renovate PR -> human merge -> orchestrator redeploys the new pinned tag), not through auto-update daemons inside this image. Image tags are immutable `vX.Y.Z` references; `:latest` is never published. Rollback in short: revert the bump in the deployment repo, redeploy the previous tag, and restore the most recent `pg_dump` if the schema or data changed in the new release. The full recipe lives in `docs/MIGRATION.md` per release.

## Reference

- [`docs/SELF_HOSTED_CONTRACT.md`](docs/SELF_HOSTED_CONTRACT.md) — binding contract this repo follows (ports, volumes, healthchecks, logging, metrics, auth modes, release flow).
- [`docs/API.md`](docs/API.md) — REST API reference.
- [`docs/DATA_SCHEMA.md`](docs/DATA_SCHEMA.md) — database schema and entity relationships.
- [`docs/EXPORT_FORMAT.md`](docs/EXPORT_FORMAT.md) — JSON export format for the AI coach summary endpoint.
- [`docs/OBSERVABILITY.md`](docs/OBSERVABILITY.md) — health endpoints, structured logs, Prometheus metrics.
- [`docs/BACKUP.md`](docs/BACKUP.md) — reference offsite-backup setup (restic).
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — operator deployment notes.
