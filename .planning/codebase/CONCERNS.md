# Codebase Concerns

**Analysis Date:** 2026-05-02

## Known Bugs (tracked in `.planning/ISSUES.md`)

**i-1: WorkoutDaysIntegrationTest helper NPE (6 errors)**
- File: `backend/src/test/java/com/workouthub/workouts/WorkoutDaysIntegrationTest.java` (createDay helper at line 196)
- Symptoms: `objectMapper.readTree(body).get("id")` returns null after a 201 from `POST /api/workout-plans/{id}/days`. Status check passes; body lacks `id`.
- Affected tests: `addItemsAndReorderCloseNoGaps`, `deleteDayCascadesItems`, `deleteItemRenumbersRemaining`, `duplicateDayOfWeekReturns409`, `reorderWithIncompleteListReturns409`, `updateItemPatchesFields`
- Confounder: `createDayAddsItToPlan` (the only non-helper test) hits the same endpoint with the same payload and passes - but it asserts `$.dayOfWeek` and `$.focus`, never `$.id`
- Hypotheses: Hibernate `@UuidGenerator` timing, `WorkoutPlan.addDay` back-reference, Jackson `NON_NULL` swallowing null id
- Trigger: needs local Maven for breakpoint debugging

**i-2: FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (1 failure)**
- File: `backend/src/test/java/com/workouthub/exports/FullExportImportIntegrationTest.java` (line 125)
- Symptom: `plansInserted` is 0 instead of `>= 1` after export -> import round-trip
- Likely in `backend/src/main/java/com/workouthub/exports/FullImportService.java` `replacePlans` flow
- Trigger: same as i-1 - needs local repro

## Self-hosted Contract Compliance Gaps

These are violations of `docs/SELF_HOSTED_CONTRACT.md` and form the v0.3 milestone backlog.

**§3.2 Port bindings - not parametric**
- File: `compose.yml` lines 8, 28, 39
- Current: `"${POSTGRES_PORT:-5432}:5432"`, `"${SERVER_PORT:-8080}:8080"`, `"3000:3000"` (frontend literally hard-coded)
- Required: `"${BIND_ADDR:-127.0.0.1}:${HTTP_PORT:-8080}:8080"` pattern across all services
- Risk: `docker compose up` exposes ports on `0.0.0.0` (every interface) by default

**§3.3 Network - no named network**
- File: `compose.yml`
- Current: relies on default bridge network
- Required: explicit `networks: workouthub-net` block

**§3.4 Volumes - named volume forbidden for stateful data**
- File: `compose.yml` lines 10, 41-42
- Current: `db_data:/var/lib/postgresql/data` (named volume)
- Required: `./data/postgres/:/var/lib/postgresql/data` (bind mount)
- Migration: drop named volume, create `./data/postgres/` (operator step in `docs/MIGRATION.md`)

**§3.5 Healthchecks - missing on backend and frontend**
- File: `compose.yml` lines 18-39
- Current: only `db` has healthcheck
- Required: `backend` healthcheck calling `/healthz`, `frontend` healthcheck (process liveness only); `frontend.depends_on.backend` must use `condition: service_healthy`

**§3.7 Resource limits - none defined**
- File: `compose.yml`
- Required: `deploy.resources.limits.{cpus,memory}` per service, documented in README

**§9.2 Health endpoints - missing `/healthz` and `/livez`**
- Currently only `/actuator/health` and `/actuator/prometheus` exposed
- Required:
  - Backend `/livez` - process liveness
  - Backend `/healthz` - real readiness (DB reachable, Flyway applied, dependencies up)
  - Frontend `/healthz` and `/livez` - both = process liveness (per agreed simplification, since frontend is stateless)

**§9.1 Metrics endpoint location**
- Current: `/actuator/prometheus`
- Required: `/metrics` on main listener (or separate metrics port bound to loopback)
- Either rebind or alias

**§8 Structured JSON logging not enforced**
- File: `backend/src/main/resources/application.yml` lines 77-88
- Current: log pattern includes trace-id but is plain text
- Required: JSON-only stdout in production profile (`application-prod.yml` override mentioned in comment but existence not verified)
- Required: deny-list filter for `password`, `token`, `secret`, `authorization`, `cookie`, `set_cookie`, `api_key`, `client_secret`, `private_key`
- Required: never log request bodies

**§7.2 Forward-auth mode not implemented**
- No `AUTH_MODE` env var
- No filter that reads `X-Forwarded-User` / `X-Forwarded-Email` / `X-Forwarded-Groups`
- No `TRUSTED_PROXIES` CIDR check
- Existing `OidcController` (`backend/src/main/java/com/workouthub/auth/OidcController.java`) implements OIDC client - this is **forbidden** by §7.2 last paragraph and should be reconsidered for the application repo (the contract pushes SSO into the operator's reverse proxy, not the app)

**§5.2 Optional pg_dump sidecar missing**
- No `ENABLE_PG_DUMP`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS` plumbing
- Required: opt-in sidecar container that writes `./data/backups/<date>.dump`

**§4.1 / §11 .env.example incomplete**
- Cannot read directly (Read tool denied by permission rule), but compose.yml + application.yml reveal missing variables:
  - `BIND_ADDR`, `HTTP_PORT`
  - `AUTH_MODE`, `TRUSTED_PROXIES`
  - `ENABLE_PG_DUMP`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS`
- Each variable must have a one-line comment per §4.1
- Secret placeholders should be `CHANGE_ME_BASE64_60` etc., with the generation command in the comment

**§12 CI gates incomplete**
- File: `.github/workflows/ci.yml`
- Missing: `gitleaks-action`, `trivy fs .`, `trivy image <built-tag>`, `docker compose config`, `hadolint`, `actionlint`, `shellcheck`

**§3.1 / §10 Image publishing missing**
- File: `.github/workflows/release.yml`
- Current: builds backend + frontend in matrix, does not push images
- Required: multi-arch buildx (`linux/amd64`, `linux/arm64`), push to GHCR as `ghcr.io/gtrows/workouthub-backend:vX.Y.Z` and `ghcr.io/gtrows/workouthub-frontend:vX.Y.Z`
- No `:latest` tag

**§11 README required sections**
- README likely missing: Quick Start, full Configuration table from `.env.example`, Exposure (Caddy / Traefik / nginx examples), Data and Backup Hooks, Updating
- `docs/MIGRATION.md` does not yet exist

## Code Hygiene

**Files exceeding ~200 line guideline (CLAUDE.md File Organization):**
- `backend/src/main/java/com/workouthub/exports/FullImportService.java` (328 lines) - 5 distinct slice replacements (profile, metrics, supplements, plans, sessions); candidate for split
- `frontend/src/lib/api/endpoints.ts` (591 lines) - monolithic API client; split by domain (`endpoints/auth.ts`, `endpoints/workouts.ts`, ...)
- `backend/src/main/java/com/workouthub/analytics/AnalyticsService.java` (~237 lines) - review for single responsibility
- Test files exceeding 200 lines are acceptable but factory extraction can help (e.g., `frontend/src/app/(app)/export/export-client.test.tsx` ~431 lines)

**No TODO / FIXME / HACK markers found**
- Repo is clean of debt-marker comments per grep over `backend/src/main/` and `frontend/src/`
- Project policy is to track in `.planning/ISSUES.md` instead - working as intended

## Security

**Dependabot alerts:** 1 high, 11 moderate (per `.planning/HANDOFF.md`)
- Mostly transitive Next / Vite chain
- Tracked at https://github.com/GTRows/workout-hub/security/dependabot
- Triage planned in v0.3 P2

**Forward-headers trust open**
- File: `backend/src/main/resources/application.yml` line 30 (`forward-headers-strategy: framework`)
- Spring `ForwardedHeaderFilter` will trust ALL proxies because no `TRUSTED_PROXIES` enforcement exists
- Risk: header spoofing in deployments without an adjacent reverse proxy
- Fix: pair with `TRUSTED_PROXIES` CIDR validation in the (planned) forward-auth filter

**Pre-commit secret scanning absent**
- No `husky` / pre-commit framework configured
- gitleaks runs in CI but not on local commits
- Adding a pre-commit gitleaks hook would shorten the leak-feedback loop

## Performance

No measurements yet (no APM, no load tests run). Add when traffic exists. The `analytics/` package contains aggregations worth profiling once historical data accumulates.

## Branch Protection

**Main branch protection NOT enabled** (per HANDOFF):
- Private repo on free tier; rule sets are Pro-only
- Mitigation: maintainer discipline (PRs with CI gates) + admin merge bypass on documented exceptions

## Recommendations (priority order)

1. **Blockers for v0.3 release:**
   - Resolve i-1 and i-2 (needs local Maven)
   - Fix compose.yml: `BIND_ADDR` parametric, named network, bind mount, healthchecks, resource limits
   - Add `/healthz` + `/livez` endpoints
   - Document missing env vars in `.env.example`

2. **High - operator interface:**
   - Multi-arch image publishing to GHCR
   - `pg_dump` opt-in sidecar
   - `AUTH_MODE=forward-auth` filter
   - Remove or gate the existing OIDC client per §7

3. **Medium - observability and CI:**
   - Structured JSON logging in production profile
   - `/metrics` on main listener
   - gitleaks + trivy + compose config + hadolint in CI
   - README §11 sections
   - `docs/MIGRATION.md` first entry

4. **Low - hygiene:**
   - Split `FullImportService` and `endpoints.ts` by domain
   - Triage Dependabot alerts
   - Consider local pre-commit gitleaks hook

---

*Concerns audit: 2026-05-02*
*Update as issues are fixed or new ones discovered*
