# Deployment

Self-hosted deployment guide for WorkoutHub.

## Files

- `compose.yml` — base stack (db, backend, frontend).
- `compose.prod.yml` — production overlay adding nginx + certbot, binding app services to localhost.
- `nginx/conf.d/workouthub.conf` — reverse proxy, TLS, HSTS, and `/api/auth/*` rate limit.
- `nginx/certs/` — certificate material (created by certbot, bind-mounted read-only into nginx).

## First-time deploy

1. Copy `.env.example` to `.env` and fill in:
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
   - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
   - `APP_JWT_SECRET` (32+ bytes of random)
   - `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD_HASH` (BCrypt hash of plaintext), `APP_ADMIN_DISPLAY_NAME`
   - `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY` (generated once via `npx web-push generate-vapid-keys`)
   - `APP_CORS_ALLOWED_ORIGINS=https://yourdomain.tld`

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

## Rate limits

`/api/auth/*` is rate-limited to 10 req/min per client IP with a 5-burst tolerance (see `limit_req_zone auth_rl`). Responses beyond that return `429 Too Many Requests`.

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
