# External Integrations

**Analysis Date:** 2026-05-02

## APIs and External Services

The application is **self-contained**: it does not depend on any third-party hosted service to run. All optional integrations are opt-in via env vars and can be left disabled.

### Web Push Notifications (VAPID, optional)

- Implementation: `backend/src/main/java/com/workouthub/notifications/WebPushJavaSender.java`
- Library: `nl.martijndwars:web-push 5.1.1` plus BouncyCastle 1.77 for VAPID signing
- Env vars: `APP_PUSH_VAPID_PUBLIC_KEY`, `APP_PUSH_VAPID_PRIVATE_KEY`, `APP_PUSH_VAPID_SUBJECT` (default `mailto:admin@localhost`)
- Frontend subscription flow: `frontend/src/lib/push/subscribe.ts` (Service Worker + PushManager)
- Schema: `backend/src/main/resources/db/migration/V10__push_subscriptions.sql`
- Reminder cron jobs: `APP_REMINDERS_WORKOUT_CRON`, `APP_REMINDERS_WEIGHT_CRON`, `APP_REMINDERS_SUPPLEMENT_CRON`

### OIDC / OpenID Connect (optional)

- Implementation: `backend/src/main/java/com/workouthub/auth/OidcController.java`
- Client: built-in `java.net.http.HttpClient` (no SDK dependency)
- Env vars: `APP_AUTH_OIDC_ENABLED` (default false), `APP_AUTH_OIDC_ISSUER_URL`, `APP_AUTH_OIDC_CLIENT_ID`, `APP_AUTH_OIDC_CLIENT_SECRET`, `APP_AUTH_OIDC_REDIRECT_URI`, `APP_AUTH_OIDC_AUTO_PROVISION` (default true)
- Flow: Authorization Code -> token exchange -> userinfo fetch -> auto-provision or lookup
- Scopes: `openid profile email`
- Note: `docs/SELF_HOSTED_CONTRACT.md` §7 forbids OIDC client code in this repo for v0.3+; the existing controller predates the contract and should be reconsidered

## Data Storage

**Authoritative Database:**
- PostgreSQL 16-alpine - service `db` in `compose.yml`
- Connection via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Schema authority: Flyway migrations under `backend/src/main/resources/db/migration/V1__*.sql` ... `V25__*.sql`
- Currently uses named Docker volume `db_data` (CONCERNS: contract requires bind mount under `./data/postgres/`)

**Client Offline Store:**
- IndexedDB via Dexie 4.0 in the workout-execution flow
- Implementation: `frontend/src/lib/offline/session-set-queue.ts`
- Idempotency key: `(sessionId, exerciseId, setNumber)`; 409 from backend treated as success on resync

**File Storage:**
- Currently no media volume - exercise GIFs and progress photos use `image_url` placeholder
- Planned: host-bind `./data/uploads/` via env-controlled path (PHASE 5)

## Authentication and Identity

**Built-in (default):**
- BCrypt-hashed passwords in `users` table
- JWT access + refresh tokens (JJWT 0.12.6)
- Refresh token rotation, hash-stored in `refresh_tokens`
- Brute-force lockout: `backend/src/main/java/com/workouthub/auth/BruteForceGuard.java` (10 failures / 15 min window / 60 min lockout)
- Optional TOTP 2FA: `backend/src/main/java/com/workouthub/twofa/`, schema `V12__user_totp.sql`
- Password reset: `backend/src/main/java/com/workouthub/auth/PasswordResetService.java`, schema `V14__password_reset_tokens.sql`
- Audit log: `V16__audit_log.sql`, queryable via `backend/src/main/java/com/workouthub/audit/`

**Reverse-proxy forward-auth (planned, opt-in):**
- Per `docs/SELF_HOSTED_CONTRACT.md` §7.2: read `X-Forwarded-User`, `X-Forwarded-Email`, `X-Forwarded-Groups`
- Activated by `AUTH_MODE=forward-auth`, gated by `TRUSTED_PROXIES` CIDR list
- **Not yet implemented** - tracked in CONCERNS for v0.3

## Webhooks and External Ingestion

**Smart Scale Webhook (Incoming):**
- Implementation: `backend/src/main/java/com/workouthub/webhooks/ScaleWebhookController.java`
- Endpoint: `POST /api/webhooks/scale/{token}`
- Payload: `ScalePayload` (weight in kg, optional timestamp)
- Authentication: per-user webhook tokens stored in `webhook_tokens` table (`V21__webhook_tokens.sql`)
- Token management: `POST/DELETE /api/users/me/webhook-tokens`

**Health Data Import (file-based, not webhook):**
- Apple Health XML: `POST /api/health/import/apple`
- Google Fit JSON: `POST /api/health/import/google-fit`
- Garmin FIT binary: `POST /api/health/import/fit`
- Frontend client: `frontend/src/lib/api/endpoints.ts` (around lines 495-591)
- Options: `bodyMass`, `workouts` (boolean query parameters)

**Outgoing Webhooks:**
- None.

## Observability and Health

**Built-in Endpoints:**
- `/actuator/health` - Spring Boot Actuator health
- `/actuator/info`
- `/actuator/prometheus` - Micrometer Prometheus metrics scrape endpoint
- Configured in `backend/src/main/resources/application.yml` lines 62-75

**Missing per Contract (`docs/SELF_HOSTED_CONTRACT.md` §9):**
- `/healthz` (real readiness: DB reachable, migrations applied)
- `/livez` (process liveness)
- `/metrics` on main listener (currently only `/actuator/prometheus`)
- Tracked in CONCERNS

**Logs:**
- Containers write to stdout/stderr
- Default pattern includes `[trace_id]` correlation
- Production JSON format planned in `application-prod.yml` (existence not verified) - tracked in CONCERNS

## CI/CD and Deployment

**Hosting:**
- Self-hosted via Docker Compose
- Maintainer's reference deployment is in `GTRows/homelab` (separate repo); this repo only publishes the application image

**Image Registry (planned for v0.3):**
- GHCR: `ghcr.io/gtrows/workouthub-backend:vX.Y.Z` and `ghcr.io/gtrows/workouthub-frontend:vX.Y.Z`
- Multi-arch: `linux/amd64` + `linux/arm64`
- Currently `.github/workflows/release.yml` builds both components but does not push images yet

**CI Pipeline:**
- `.github/workflows/ci.yml` - backend `mvn verify` + frontend lint/typecheck/test
- `.github/workflows/codeql.yml` - CodeQL scan on push + weekly cron
- Dependabot weekly grouped updates (npm, maven, gh-actions)

## Environment Configuration

**Required env vars (per `application.yml`):**
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET` (BCrypt secret >=32 bytes for HS256)

**Optional env vars (defaults shown):**
- `SPRING_PROFILES_ACTIVE` - default `local`
- `APP_JWT_ACCESS_TTL_SECONDS` - 3600
- `APP_JWT_REFRESH_TTL_SECONDS` - 1209600
- `APP_CORS_ALLOWED_ORIGINS` - `http://localhost:3000`
- `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD_HASH`, `APP_ADMIN_DISPLAY_NAME` - first-boot admin seeding
- `APP_PUSH_VAPID_*`
- `APP_AUTH_OIDC_*`
- `APP_REMINDERS_*` - cron expressions

**Missing per contract (tracked in CONCERNS):**
- `BIND_ADDR` (default `127.0.0.1`)
- `HTTP_PORT` (default 8080)
- `TRUSTED_PROXIES` (CIDR list)
- `AUTH_MODE` (`builtin` | `forward-auth`)
- `ENABLE_PG_DUMP`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS`

**Secrets:**
- `.env` is gitignored
- `.env.example` documents (most) variables - audit in CONCERNS

---

*Integration audit: 2026-05-02*
*Update when adding/removing external services*
