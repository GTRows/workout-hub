# Migration

Operator-visible changes per release: env vars added or removed, schema migrations introduced, one-shot commands the operator must run, and the rollback recipe. Pair this file with [`../CHANGELOG.md`](../CHANGELOG.md) — the changelog explains *what* changed for users; this file explains *what the operator must do* to land the change safely.

Each release gets its own `## vX.Y.Z` section with these subsections (in order):

- **Required env var changes** — vars added, removed, renamed, or with changed defaults.
- **Schema and data migration** — auto-applied Flyway migrations and any one-shot operator commands.
- **Compose / runtime changes** — service topology, healthchecks, ports, profiles.
- **One-shot commands** — exact commands the operator runs once during the upgrade.
- **Rollback** — how to revert if the upgrade fails.

If a release needs no migration steps, write `No migration steps.` explicitly under the version heading. An empty section is not allowed.

---

## v0.3.0

First release after the self-hosted contract was adopted (`docs/SELF_HOSTED_CONTRACT.md`). Phases 1-8 reshaped the operator-facing surface: parametric port bindings, bind-mount data, contract healthchecks, structured JSON logs, Prometheus `/metrics`, forward-auth mode (with the in-app OIDC client removed), opt-in `pg_dump` sidecar, and a fully documented `.env.example` / `docker-compose.override.yml.example` pair.

### Required env var changes

Copy the new `.env.example` over your existing `.env` (or merge keys manually) and review the additions. Existing keys keep their previous semantics unless noted.

**Added (compose binding):**

- `BIND_ADDR` — interface to publish all service ports on. Defaults to `127.0.0.1`. Set to a LAN/VPN IP for remote access; never `0.0.0.0`.
- `BACKEND_PORT` — host-side port for the backend (default `8080`). Replaces the implicit `SERVER_PORT`-based binding.
- `FRONTEND_PORT` — host-side port for the frontend (default `3000`). Replaces the previous literal `3000:3000` mapping.
- `POSTGRES_PORT` — host-side port for Postgres (default `5432`). Only useful if you run `psql` against the host port.

**Added (auth mode):**

- `APP_AUTH_MODE` — `builtin` (default) or `forward-auth`. The opt-in `forward-auth` mode trusts identity headers injected by a reverse-proxy SSO gateway.
- `APP_AUTH_TRUSTED_PROXIES` — comma-separated CIDR list of reverse-proxy IPs allowed to inject identity headers. Empty = trust nothing (the safe default; required when `APP_AUTH_MODE=builtin`).
- `APP_AUTH_HEADER_USER` / `APP_AUTH_HEADER_EMAIL` / `APP_AUTH_HEADER_GROUPS` — identity header names. Override only if the proxy uses non-default names.

**Added (Web Push):**

- `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_PUSH_VAPID_SUBJECT` — VAPID keypair for browser push notifications. Generate with `./scripts/vapid-keygen.sh`.

**Added (reminder cron schedules):**

- `APP_REMINDERS_WORKOUT_CRON`, `APP_REMINDERS_WEIGHT_CRON`, `APP_REMINDERS_SUPPLEMENT_CRON` — Spring `CronExpression` syntax (6 fields). Empty = disabled. Example: `0 0 18 * * *` runs daily at 18:00.

**Added (pg_dump backup sidecar):**

- `ENABLE_PG_DUMP` — set to `true` (and `COMPOSE_PROFILES=backup`) to start the optional sidecar. Default `false`.
- `PG_DUMP_SCHEDULE` — cron expression. Default `0 3 * * *` (daily at 03:00).
- `PG_DUMP_RETENTION_DAYS` — day-based retention. Default `7`.

**Removed:**

- `APP_AUTH_OIDC_*` (entire block) — the in-app OIDC client was deleted per contract section 7. See "Compose / runtime changes" below for the migration path.

### Schema and data migration

No new schema changes ship in v0.3.0 beyond what was already merged on `main` for the v0.2.x line. Flyway runs automatically on backend start; if `ddl-auto: validate` rejects the schema after upgrade, the container fails fast with the offending migration in the logs.

The big operator-side data move is **named volume `db_data` -> bind mount `./data/postgres/`**. Previous releases used a Docker named volume; v0.3.0 uses a bind mount under `./data/postgres/` so backups, snapshots, and disk-usage tooling can see the cluster files directly. Run the migration commands in the next section before bringing the v0.3.0 stack up.

### Compose / runtime changes

- **Port bindings are parametric and loopback-default.** Every published port is now `${BIND_ADDR:-127.0.0.1}:${PORT:-DEFAULT}:CONTAINER`. A fresh `docker compose up` is reachable only from the host.
- **Postgres data path moved.** From a named Docker volume (`db_data`) to bind mount `./data/postgres/`. See the one-shot migration recipe below.
- **In-app OIDC client removed.** `OidcController`, the OIDC integration test, `AuthService.issueTokensForOidc`, and the `app.auth.oidc.*` config block are gone. Operators previously using the in-app OIDC flow must move SSO to a reverse-proxy forward-auth setup: deploy Authentik / Authelia / oauth2-proxy in front, set `APP_AUTH_MODE=forward-auth`, and add the proxy IP/CIDR to `APP_AUTH_TRUSTED_PROXIES`. See README "Exposure" and contract section 7.2.
- **Healthcheck endpoints renamed.** Backend probes now hit `/healthz` (was `/actuator/health`) and `/livez`; the frontend probes `/api/healthz` (was `/`). Compose healthchecks are already updated in `compose.yml`. **Reverse-proxy upstream selection should follow** — if your proxy health-checks the upstreams, repoint backend checks to `/healthz` and frontend checks to `/api/healthz`. Do not health-check the frontend through the backend.
- **Production logs are structured JSON to stdout.** Activated automatically when `SPRING_PROFILES_ACTIVE=prod` (or `docker` if your profile composes prod). Format is ECS-compatible JSON with secret deny-list masking. If you ship logs to Loki / Elastic / Vector / Fluent Bit, update the parser stage to expect JSON instead of the previous Spring text format.
- **Prometheus `/metrics` is on the main listener.** Backend exposes `/metrics` on `:8080` (an alias to `/actuator/prometheus`, which is preserved for compat); frontend exposes `/api/metrics` on `:3000`. Prometheus scrape configs can switch from `/actuator/prometheus` to `/metrics` and add a frontend target. Restrict access by interface binding (loopback or proxy-network), not by application auth.
- **Optional pg_dump sidecar.** New service `pg_dump` is profile-gated by `backup`. Plain `docker compose up` does not start it. Enable with `COMPOSE_PROFILES=backup` in `.env` (or `docker compose --profile backup up -d`). Snapshots land in `./data/backups/<date>.dump`.

### One-shot commands

**1. Migrate the Postgres data from named volume to bind mount.** Run this on the host BEFORE `docker compose up` against v0.3.0. Adjust `db_data` to match your previous compose project's actual volume name (`docker volume ls | grep db_data` to confirm — Compose namespaces volumes as `<project>_db_data`).

```bash
# 0. Stop the v0.2.x stack so nothing is writing to the cluster.
docker compose down

# 1. Create the new bind-mount target.
mkdir -p ./data/postgres ./data/backups

# 2. Take a final pg_dump from the old named volume as a safety net.
docker run --rm \
    -v <project>_db_data:/var/lib/postgresql/data \
    -v "$(pwd)/data/backups":/backups \
    postgres:16-alpine \
    sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom > /backups/pre-v0.3.0.dump'

# 3. Copy the cluster files from the named volume into the bind mount.
docker run --rm \
    -v <project>_db_data:/from \
    -v "$(pwd)/data/postgres":/to \
    alpine sh -c 'cp -a /from/. /to/'

# 4. Verify ownership inside the bind mount (Postgres expects uid 70 in alpine images,
#    or 999 in debian-based; `postgres:16-alpine` uses 70).
ls -la ./data/postgres | head

# 5. Bring up the v0.3.0 stack. Postgres will start against the bind mount.
docker compose up -d

# 6. Once the stack is healthy and you have verified data, drop the old named volume.
docker volume rm <project>_db_data
```

If step 5 fails with permission errors on the data directory, `chown -R 70:70 ./data/postgres` (alpine image uid) or run a one-shot `postgres:16-alpine` container with the bind mount and `--user 0:0` to fix ownership in place.

**2. Switch SSO to forward-auth (only if you used the deleted in-app OIDC client).**

```bash
# Edit .env:
#   APP_AUTH_MODE=forward-auth
#   APP_AUTH_TRUSTED_PROXIES=<your reverse-proxy IP or CIDR>
#   (optionally) APP_AUTH_HEADER_USER / APP_AUTH_HEADER_EMAIL / APP_AUTH_HEADER_GROUPS

# Then deploy your forward-auth proxy (Authentik / Authelia / oauth2-proxy) in front
# of the stack. The application will trust X-Forwarded-User / -Email / -Groups
# headers ONLY when the request source IP matches APP_AUTH_TRUSTED_PROXIES.

docker compose up -d backend
```

**3. Enable the optional pg_dump sidecar (recommended).**

```bash
# Edit .env:
#   COMPOSE_PROFILES=backup
#   PG_DUMP_SCHEDULE=0 3 * * *
#   PG_DUMP_RETENTION_DAYS=7

docker compose --profile backup up -d pg_dump
```

### Rollback

If the upgrade misbehaves:

```bash
# 1. In your operator deployment repo, revert the merge that bumped to v0.3.0.
git revert <merge-sha>

# 2. The orchestrator (Komodo / Watchtower-replacement / manual `compose pull`)
#    redeploys the previously pinned tag. Confirm the stack is back on v0.2.x.

# 3. If schema or data changed during the v0.3.0 run, restore the pre-deploy dump:
docker compose stop backend
gunzip -c ./data/backups/pre-v0.3.0.dump | \
    docker compose exec -T db pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists
docker compose start backend
```

The bind-mount cluster directory survives a rollback because v0.2.x can mount the same path with a one-line `compose.override.yml` (`volumes: - ./data/postgres/:/var/lib/postgresql/data`). Once back on a known-good release, investigate the v0.3.0 failure before retrying.

---

## v0.4.0

Backend feature completion release. Closes the workout-plan editing,
live-session execution, body-metrics, full-export refinement, and API
contract documentation deliverables from `ProjectBrief.md` Phases 3-5.
Operator-visible surface changes are limited to two new auto-applied
Flyway migrations (V26 clientSetId on `session_sets`, V27 `is_pr` on
`session_sets`), one new operator-reachable runtime surface (the
SpringDoc OpenAPI document and Swagger UI), and a refined
`/api/export/import` failure mode (curated 422 instead of 500-class
FK violation on unknown `workoutDayId`). No env var changes; no
compose-topology changes; no one-shot operator commands; rollback is
the standard `git revert` of the merge that bumped to v0.4.0.

### Required env var changes

No new env vars. No removed env vars. No env-default changes. The
v0.3.0 contract-aligned env surface (`BIND_ADDR`, `BACKEND_PORT`,
`FRONTEND_PORT`, `POSTGRES_PORT`, `APP_AUTH_*`, `APP_PUSH_VAPID_*`,
`APP_REMINDERS_*_CRON`, `ENABLE_PG_DUMP`, `PG_DUMP_*`) carries forward
unchanged.

### Schema and data migration

Two new Flyway migrations apply automatically on backend start:

- **V26** -- adds `client_set_id UUID` (nullable, unique-per-session)
  to `session_sets` for offline-queue idempotent replay (Phase 15-02).
- **V27** -- adds `is_pr BOOLEAN NOT NULL DEFAULT false` to
  `session_sets` plus a `ROW_NUMBER() OVER (PARTITION BY user_id,
  exercise_id ORDER BY weight*(1 + reps/30) DESC, created_at ASC)`
  backfill that marks the highest-Epley completed set per
  `(user_id, exercise_id)` (Phase 16-04).

Both migrations are forward-only and idempotent on first apply. The
backfill in V27 is bounded by existing `session_sets` rows; on a
fresh install it is a no-op.

`spring.jpa.hibernate.ddl-auto: validate` continues to gate schema
drift. If V26 or V27 fails to apply against an existing database (for
example, an operator with a manually-edited schema), the backend
container fails fast at boot with the offending migration in the
logs.

### Compose / runtime changes

- **OpenAPI document and Swagger UI now reachable on the backend
  listener.** SpringDoc 2.6.0 (introduced in Phase 19-02) serves
  `GET /v3/api-docs` (JSON), `GET /v3/api-docs.yaml` (YAML), and
  `GET /swagger-ui.html` (interactive UI). Both surfaces are reachable
  without authentication by design; the application's public-network
  exposure is bounded by `BIND_ADDR=127.0.0.1` (compose default) and
  the operator's reverse proxy. See `docs/API.md` "Discoverability"
  for the trust-boundary stance.
- **Operator-facing endpoints excluded from the OpenAPI document.**
  `/livez`, `/healthz`, `/metrics`, `/actuator/health`,
  `/actuator/info`, `/actuator/prometheus` are NOT in `/v3/api-docs`
  per `springdoc.paths-to-match=/api/**`. They stay documented in
  `docs/OBSERVABILITY.md`.
- **No compose-topology change.** No new service. The `pg_dump`
  sidecar profile from v0.3.0 stays opt-in.
- **No image registry change.** Multi-arch publish to
  `ghcr.io/gtrows/workouthub-{backend,frontend}:0.4.0` (amd64 +
  arm64) per the v0.3.2 release-workflow flow.

### One-shot commands

No one-shot commands required. Operators upgrade with the standard
two-step:

```bash
# 1. Pull the new image tags.
docker compose pull

# 2. Restart the stack. V26 + V27 apply at backend boot.
docker compose up -d
```

If the optional pg_dump sidecar is enabled (`COMPOSE_PROFILES=backup`),
the second step also restarts the sidecar against the same schema. No
data conversion is required for V26 (new nullable column) or V27 (new
column with default-and-backfill).

### Rollback

```bash
# 1. In your operator deployment repo, revert the merge that bumped to v0.4.0.
git revert <merge-sha>

# 2. The orchestrator (Komodo / Watchtower-replacement / manual `compose pull`)
#    redeploys the previously pinned tag (v0.3.2). Confirm the stack is back.

# 3. V26 + V27 columns survive the downgrade. v0.3.2 does not read
#    `client_set_id` or `is_pr`; the columns sit unused but cause no
#    harm. They re-activate automatically on the next forward upgrade
#    to v0.4.x or later.

# 4. Optional: take a fresh pg_dump after the rollback completes:
docker compose exec db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    --format=custom > ./data/backups/post-rollback-v0.3.2.dump
```

The bind-mount cluster directory at `./data/postgres/` is unchanged by
v0.4.0. No data conversion landed in v0.4.0; the rollback to v0.3.2 is
schema-additive-only and reversible without data loss.

---

## vNext

`No migration steps.` (or replace with the structure above when the next release lands.)

Template to copy when adding a release entry:

```markdown
## vX.Y.Z

One-paragraph summary of the release as it lands on operators.

### Required env var changes
- ...

### Schema and data migration
- ...

### Compose / runtime changes
- ...

### One-shot commands
\`\`\`bash
...
\`\`\`

### Rollback
\`\`\`bash
git revert <merge-sha>
# orchestrator redeploys previous pinned tag
# restore pre-deploy pg_dump if state changed
\`\`\`
```
