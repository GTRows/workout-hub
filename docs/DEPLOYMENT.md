# Deployment

Self-hosted deployment guide for WorkoutHub.

## Files

- `compose.yml` — base stack (db, backend, frontend).
- `compose.prod.yml` — production overlay adding nginx + certbot, binding app services to localhost.
- `nginx/conf.d/workouthub.conf` — reverse proxy, TLS, HSTS, and `/api/auth/*` rate limit.
- `nginx/certs/` — certificate material (created by certbot, bind-mounted read-only into nginx).

## First-time deploy

1. Copy `.env.example` to `.env` and fill in. The README's [Configuration
   table](../README.md#configuration) groups every variable; the deploy-time
   minimum is:
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
   - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
   - `APP_JWT_SECRET` (32+ bytes of random)
   - `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD_HASH` (BCrypt hash of plaintext), `APP_ADMIN_DISPLAY_NAME`
   - `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_PUSH_VAPID_SUBJECT` (generated once via `npx web-push generate-vapid-keys`; subject is `mailto:` or `https:` per the Web Push spec)
   - `APP_CORS_ALLOWED_ORIGINS=https://yourdomain.tld`
   - `APP_REMINDERS_WORKOUT_CRON`, `APP_REMINDERS_WEIGHT_CRON`, `APP_REMINDERS_SUPPLEMENT_CRON` (Spring 6-field cron expressions; a single dash `-` disables a trigger)
   - `APP_REST_TIMER_POLL_INTERVAL_MS`, `APP_REST_TIMER_CLEANUP_INTERVAL_MS` (defaults `1000` / `300000` are sane; rarely overridden)

   When fronting the app with an SSO gateway (Authentik, Authelia,
   oauth2-proxy, Keycloak), also set:
   - `APP_AUTH_MODE=forward-auth`
   - `APP_AUTH_TRUSTED_PROXIES` (comma-separated CIDRs the proxy lives in)
   - `APP_AUTH_HEADER_USER`, `APP_AUTH_HEADER_EMAIL`, `APP_AUTH_HEADER_GROUPS`

   For the opt-in `pg_dump` sidecar set `COMPOSE_PROFILES=backup` plus
   `ENABLE_PG_DUMP=true`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS`. See
   [BACKUP.md](./BACKUP.md) for the full restore drill.

2. Obtain an initial certificate (one-shot, before first nginx boot):

   ```sh
   docker run --rm -p 80:80 \
     -v "$(pwd)/nginx/certs:/etc/letsencrypt" \
     certbot/certbot:latest certonly --standalone \
       -d yourdomain.tld \
       --email you@yourdomain.tld \
       --agree-tos --non-interactive
   ```

   This writes `/etc/letsencrypt/live/yourdomain.tld/{fullchain,privkey}.pem`. Point `workouthub.conf` at the matching directory (the default uses `live/workouthub/`).

3. Bring the stack up:

   ```sh
   docker compose -f compose.yml -f compose.prod.yml up -d
   ```

4. Check health:

   ```sh
   curl -k https://yourdomain.tld/actuator/health
   ```

## TLS renewal

The `certbot` service runs `certbot renew --webroot` every 12 hours and writes into the shared `nginx/certs` volume. Nginx picks up the rotated cert on the next reload:

```sh
docker compose -f compose.yml -f compose.prod.yml exec nginx nginx -s reload
```

If you prefer host cron over the in-container loop, remove the `certbot` service and add this line to the host's crontab:

```
17 3 * * * docker compose -f /opt/workouthub/compose.yml -f /opt/workouthub/compose.prod.yml exec -T certbot certbot renew --webroot -w /var/www/certbot --quiet && docker compose -f /opt/workouthub/compose.yml -f /opt/workouthub/compose.prod.yml exec -T nginx nginx -s reload
```

## Local dry-run with a self-signed cert

For testing `compose.prod.yml` without hitting Let's Encrypt, generate a self-signed pair:

```sh
mkdir -p nginx/certs/live/workouthub
openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout nginx/certs/live/workouthub/privkey.pem \
  -out    nginx/certs/live/workouthub/fullchain.pem \
  -days 90 -subj "/CN=localhost"
docker compose -f compose.yml -f compose.prod.yml up -d
curl -k https://localhost/actuator/health
```

Browsers will flag the cert as untrusted; that is expected.

## Web Push & Reminders

WorkoutHub ships a server-side reminder pipeline (workout, weight, supplement)
that delivers Web Push notifications to subscribed devices. To enable it:

1. Generate a VAPID keypair (Node 22 is already available as a build-time dep):

   ```sh
   npx web-push generate-vapid-keys
   ```

2. Set the resulting keys in your `.env`:

   ```
   APP_PUSH_VAPID_PUBLIC_KEY=<public key>
   APP_PUSH_VAPID_PRIVATE_KEY=<private key>
   APP_PUSH_VAPID_SUBJECT=mailto:you@example.com
   ```

3. Pick the reminder cron expressions (Spring 6-field, second-precision):

   ```
   APP_REMINDERS_WORKOUT_CRON=0 0 8 * * *
   APP_REMINDERS_WEIGHT_CRON=0 0 7 * * *
   APP_REMINDERS_SUPPLEMENT_CRON=0 * * * * *
   ```

   Use a single dash `-` to disable any trigger.

4. Restart the backend. Each authenticated user can verify their device is
   wired by visiting `/profile -> Notifications -> Send test notification`.

Without VAPID keys configured the system falls back to a logging dispatcher:
the cron triggers still run and emit
`notifications.LoggingNotificationDispatcher` log lines so operators can
confirm the schedule is firing before flipping the keys on.

### Rest-timer push notifications

When a user logs a set inside a session, the frontend posts a schedule row
to `/api/sessions/{id}/rest-timer`; an in-process poller dispatches a push
when the rest interval elapses, even if the tab is backgrounded. Two tunables
exist (both have safe defaults; most operators will not need to change them):

```
APP_REST_TIMER_POLL_INTERVAL_MS=1000      # how often to check for due rows
APP_REST_TIMER_CLEANUP_INTERVAL_MS=300000 # how often to prune dispatched rows
```

The corresponding Spring properties are `app.rest-timer.poll-interval-ms`
(default `1000`) and `app.rest-timer.cleanup-interval-ms` (default
`300000`). Schedules that have already fired are pruned after one hour to
keep the `rest_timer_schedules` table bounded.

If `APP_PUSH_VAPID_PRIVATE_KEY` is unset the dispatch path still runs - it
just routes through `LoggingNotificationDispatcher`, so logs confirm the
schedule fired even before VAPID keys are flipped on.

## Rate limits

`/api/auth/*` is rate-limited to 10 req/min per client IP with a 5-burst tolerance (see `limit_req_zone auth_rl`). Responses beyond that return `429 Too Many Requests`.

The application's only in-process throttle is the per-email login lockout
in `BruteForceGuard` (login attempts within a sliding window per email).
General-purpose request rate limiting is the operator's reverse-proxy
responsibility; see
[`SELF_HOSTED_CONTRACT.md`](SELF_HOSTED_CONTRACT.md) for the binding split
between application-layer and proxy-layer concerns.

## Backups and restore

See [BACKUP.md](./BACKUP.md).

## Homelab proxy (Caddy)

If you host the app behind the homelab Caddy at
`homelab/stacks/proxy/`, copy `deploy/caddy/Caddyfile` into the Caddy
sites directory (or wherever the main Caddyfile imports from) and
replace `workouthub.example.com` with the real hostname.

Caddy auto-provisions a Let's Encrypt cert on first request to the
hostname. Validate before reload:

```sh
docker compose -f homelab/stacks/proxy/docker-compose.yml \
    exec caddy caddy validate --config /etc/caddy/Caddyfile
```

Reload:

```sh
docker compose -f homelab/stacks/proxy/docker-compose.yml \
    exec caddy caddy reload --config /etc/caddy/Caddyfile
```

The Caddy config assumes the backend and frontend containers are
reachable under the docker service names `backend:8080` and
`frontend:3000` from the proxy network. If they live on a different
compose project, swap those for the tailscale/LAN hostnames.

For non-homelab installs the `compose.prod.yml` + nginx recipe above
remains valid and ships its own TLS via certbot.

## Shutdown

```sh
docker compose -f compose.yml -f compose.prod.yml down
```

Append `-v` to also drop the database volume. Never do that on a live host.

## What's not here

This file is the operator-deploy quickstart. Topics it deliberately
delegates:

- **Backup setup details (restic, offsite responsibility):**
  [`BACKUP.md`](BACKUP.md).
- **Backup-restore drill (operator-runnable verification):**
  [`BACKUP_RESTORE_DRILL.md`](BACKUP_RESTORE_DRILL.md).
- **Health endpoints, structured logs, Prometheus metrics:**
  [`OBSERVABILITY.md`](OBSERVABILITY.md).
- **Per-version operator upgrade steps:** [`MIGRATION.md`](MIGRATION.md).
- **Binding contract (ports, volumes, healthchecks, logging, metrics, auth
  modes, release flow):** [`SELF_HOSTED_CONTRACT.md`](SELF_HOSTED_CONTRACT.md).
- **Database schema and entity relationships:**
  [`DATA_SCHEMA.md`](DATA_SCHEMA.md).
- **Forward-auth header configuration for SSO:** README's [Exposure
  section](../README.md#exposure).
