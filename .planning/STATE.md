# State

## Current Position

Milestone: v0.3 Self-Hosted Contract Alignment
Phase: 9 of 12 COMPLETE (2026-05-03); Phase 10 next (CI gates)
Plan: 1 of 1 complete in Phase 9 (09-01 done; README + MIGRATION docs)
Status: Phase complete; ready to plan Phase 10
Last activity: 2026-05-03 - Completed 09-01-PLAN.md (README and MIGRATION docs)

Progress: ████████████ 50% (11 of 22 plans across milestone)

## Accumulated Context

### Decisions Locked-in (2026-05-02)

- **Env vars + override (Plan 06-01)**: `.env.example` is canonical contract source for env var surface; rewritten to cover all 11 sections. `docker-compose.override.yml.example` shipped for ARM-friendly resource tuning; operator-edited `docker-compose.override.yml` is gitignored. `.claude/settings.json` deny patterns narrowed from `**/.env.*` to specific filenames so `.env.example` stays editable by Claude Code automation.
- **Forward-auth + OIDC removal (Plan 05-01)**: Option A executed - OIDC client surface deleted entirely (`OidcController.java`, `OidcControllerIntegrationTest.java`, `AuthService.issueTokensForOidc`, `app.auth.oidc.*` YAML, SecurityConfig allowlist for `/api/auth/oidc/*`). Forward-auth filter loads only when `app.auth.mode=forward-auth` (ConditionalOnProperty); ignores headers when source IP not in TRUSTED_PROXIES CIDR; empty TRUSTED_PROXIES = trust nothing. Built-in JWT remains the default mode and is untouched.
- **AppUserPrincipal record signature**: `(UUID userId, String role)` - 2 args. Earlier sketches showing 3-arg variants are wrong.
- **Prometheus /metrics aliasing (Plan 04-01)**: Additive alias from `/metrics` to `PrometheusScrapeEndpoint`; `/actuator/prometheus` preserved for backward compat. Frontend `/api/metrics` is inline (no `prom-client` dep) emitting Node process gauges. Per-request HTTP histogram on the frontend deferred to v0.6 (ISSUES i-4) — Next App Router middleware integration is non-trivial and out of v0.3 contract baseline scope.
- **Spring Boot 3.4.1 PrometheusScrapeEndpoint API**: signature is `scrape(PrometheusOutputFormat, Set<String>)` returning `byte[]` (NOT `TextOutputFormat` and NOT `String` as initial plan sketched - that is the deprecated `PrometheusSimpleclientScrapeEndpoint`). Decode via UTF-8 in the controller.
- **Structured logging API (Plan 03-01)**: Spring Boot 3.4 native ECS structured logging chosen over logstash-logback-encoder (no new pom dep). Deny-list masking via `org.springframework.boot.json.JsonWriter.Members#applyingValueProcessor` (NOT `applyingNameProcessor` as initial plan sketched - the latter only renames keys). Customizer registration via `logging.structured.json.customizer` YAML property in `application-prod.yml` (NOT @Component/@Profile - LoggingApplicationListener runs before the application context exists, so SpringFactoriesLoader-style instantiation is the only available wiring path). Customizer class needs a public no-arg constructor and no Spring annotations.
- **Backend readiness check (Plan 02-01)**: `/healthz` returns 200 only when (a) DataSource.getConnection.isValid(1s) AND (b) Flyway.info().pending() is empty. Spring is implicitly up if the controller responds at all. `/livez` returns 200 unconditionally (process liveness). Both endpoints publicly allowlisted in SecurityConfig; no Authorization header required by container probes.
- **Healthcheck probe binary (Plan 01-02)**: Backend uses `wget --spider` (busybox, ships with `eclipse-temurin:21-jre-alpine`) instead of `curl`. Frontend uses Node 22 built-in `fetch`. Zero Dockerfile changes. Both probes are PROVISIONAL - Phase 2 re-points them to `/healthz`.
- **Resource ceiling (Plan 01-02)**: 4 CPU / 1536 MB total across 3 services (db 1.0/512M, backend 2.0/768M, frontend 1.0/256M). Sized for x86_64 PC primary host; ARM/lighter overrides via Phase 6 `docker-compose.override.yml.example`.
- **Compose env var naming (Plan 01-01)**: `BACKEND_PORT` and `FRONTEND_PORT` introduced at the compose layer (replacing the literal `3000:3000` and `SERVER_PORT`'s double-duty). The backend application internally still reads `SERVER_PORT` - the compose-level rename is for unambiguous per-service env vars only.
- **Gitignore pattern for `data/` (Plan 01-01)**: Cannot use `data/` + `!data/.gitkeep` because Git refuses to re-include files under an excluded parent. Use `data/*` + `!data/.gitkeep` + `!data/postgres/` + `data/postgres/*` + `!data/postgres/.gitkeep`. Pattern documented in ISSUES i-3 for future operators.
- **Update model**: Renovate-pin (operator opens PR for new `vX.Y.Z`), not Watchtower / `:latest`. Reason: audit trail, rollback via `git revert`.
- **Reverse proxy**: Out of scope for this repo. Operator brings their own (Caddy in maintainer's homelab). The application speaks plain HTTP behind whatever the operator chooses.
- **Public exposure**: Tailnet-only for the maintainer's deployment, hardened later. Forbidden for this app to assume public exposure.
- **Auth modes**: Built-in JWT default (already implemented), `AUTH_MODE=forward-auth` opt-in (Phase 5). No OIDC client code in this repo per contract section 7 - the existing `OidcController` predates the contract and will be reconsidered in Phase 5.
- **Persistence migration**: Local DB starts fresh on contract migration (named volume `db_data` -> bind mount `./data/postgres/`). Operator instructions in `docs/MIGRATION.md` per Phase 9.
- **Host topology**: Application is single-primary; replica is the operator's concern (litestream / pg_dump sidecar / restic). The app exposes the data layout, the operator backs it up.
- **Image registry**: `ghcr.io/gtrows/workouthub-backend` and `-frontend` (two separate images, separate version cadences). Multi-arch amd64+arm64.
- **BIND_ADDR**: parametric env var, default `127.0.0.1`. Tailnet IP injected at deploy time, never committed to repo.
- **Frontend health**: `/healthz` and `/livez` both = process liveness only. Frontend is stateless and should not depend on backend reachability for its own healthcheck (proxy upstreams them independently).

### Pre-GSD Work Carried Forward

- v0.2 was shipped informally, snapshot in `.planning/HANDOFF.md`. Treat as historical record, not gospel.
- 25 Flyway migrations applied (V1..V25)
- 23 backend feature packages already implemented
- Frontend has API client, providers, i18n, offline queue, Service Worker - but most pages are stubs

### Open Issues

- **i-1**: WorkoutDaysIntegrationTest helper NPE on response `id` (6 errors). Needs local Maven for breakpoint debugging. See `.planning/ISSUES.md`.
- **i-2**: FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (1 failure). Same trigger as i-1.
- Both i-1/i-2 blocked on local-Maven environment setup (memory: local_maven_gap).
- **i-3**: Resolved at execution time (Plan 01-01 gitignore pattern correction).
- **i-4** (Plan 04-01): Frontend per-request HTTP metrics deferred to v0.6.
- **i-5** (Plan 08-01): next-intl 3->4 major bump deferred (mitigation via reverse-proxy header rewrite).
- **i-6** (Plan 08-01): Next 15->16 major bump deferred to v0.5.
- **i-7** (Plan 08-01): testcontainers 1->2 major bump deferred to next test-infra session.
- **i-8** (Plan 08-01): Spring Boot 3.4->4.0 major bump deferred to v0.6.
- **Deferred runtime verification (Plans 01-02 Task 3 and 02-02 Task 3)**: `docker compose up -d` smoke test deferred. Repo's `.env` is gitignored AND blocked from being created via Bash (`pre_guard_secrets.py`) AND blocked via Write (deny rule on `.env*`). Operator can run the 9-step checkpoint from `01-02-PLAN.md` after creating `.env` from `.env.example`. Naturally re-validated as part of Phase 12 (Release v0.3.0).

### Roadmap Evolution

- 2026-05-02: Milestone v0.3 created. Theme: self-hosted contract alignment (`docs/SELF_HOSTED_CONTRACT.md`). 12 phases (Phase 1-12).

## Session Continuity

Last session: 2026-05-03 ~01:20 local
Stopped at: Completed 09-01-PLAN.md (README + MIGRATION); Phase 9 complete
Resume file: None (next action: `/gsd:plan-phase 10`)

## Reference Documents

- `.planning/codebase/STACK.md`
- `.planning/codebase/ARCHITECTURE.md`
- `.planning/codebase/STRUCTURE.md`
- `.planning/codebase/CONVENTIONS.md`
- `.planning/codebase/TESTING.md`
- `.planning/codebase/INTEGRATIONS.md`
- `.planning/codebase/CONCERNS.md`
- `.planning/HANDOFF.md` (pre-GSD snapshot)
- `.planning/ISSUES.md` (deferred test failures)
- `docs/SELF_HOSTED_CONTRACT.md` (binding operational contract)
- `ProjectBrief.md` (original phase plan)
