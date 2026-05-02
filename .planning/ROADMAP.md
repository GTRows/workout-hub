# Roadmap: WorkoutHub

## Overview

WorkoutHub is a self-hosted multi-user fitness tracker with a Java 21 + Spring Boot 3 backend and a Next.js 15 + React 19 frontend, packaged as Docker images for operators to run on their own infrastructure. The project ships against a portable contract documented at `docs/SELF_HOSTED_CONTRACT.md`; the maintainer's reference deployment lives separately at `GTRows/homelab` and is not part of this repository.

Pre-GSD work (informally tracked in `.planning/HANDOFF.md`) delivered the application surface area through v0.2: 23 backend feature packages, 25 Flyway migrations, the offline session queue, OIDC controller, push notifications, smart-scale webhook, and JSON export. Formal GSD planning starts here, with v0.3 focused on operator-interface alignment to the contract.

## Domain Expertise

None - project is application code; planning draws from `docs/SELF_HOSTED_CONTRACT.md` and `.planning/codebase/` reference.

## Milestones

- ⚪ **v0.1 / v0.2** - shipped pre-GSD (informal); see `.planning/HANDOFF.md` for state snapshot
- 🚧 **v0.3 Self-Hosted Contract Alignment** - Phases 1-12 (in progress)
- 📋 **v0.4 Backend Feature Completion** - planned (workouts, sessions, metrics, export modules per ProjectBrief phases 3-5)
- 📋 **v0.5 Frontend Completion** - planned (auth pages, dashboard, plan editor, session execution UI, history, metrics, profile, export)
- 📋 **v0.6 Operational Maturity** - planned (richer metrics, error states, performance budgeting)

## Phases

### 🚧 v0.3 Self-Hosted Contract Alignment (In Progress)

**Milestone Goal:** Bring the application image and its compose stack into full compliance with `docs/SELF_HOSTED_CONTRACT.md` so the homelab operator can adopt it cleanly via Renovate-pinned `vX.Y.Z` GHCR images. Closes `.planning/ISSUES.md` i-1 and i-2 along the way.

**Pre-existing items (already done):**
- ✅ PR #18 merged (2026-05-02): cleared 12 of 19 pre-existing test failures - refresh-token jti, brute-force REQUIRES_NEW, ResponseStatusException handler, SupplementTiming JsonValue, HttpMessageNotReadableException handler, Streak fixture, DefaultPlanSeeder LazyInit, CSV empty body. Two test classes still red (i-1 WorkoutDays, i-2 RoundTrip) tracked separately.

#### Phase 1: Compose Refactor [COMPLETE 2026-05-02]

**Goal**: Bring `compose.yml` into compliance with contract sections 3.1-3.7. Parametric `BIND_ADDR`, named network, bind mount under `./data/postgres/`, healthchecks on every service that something else depends on, resource limits, `condition: service_healthy` for the frontend's dependency on backend.
**Depends on**: Nothing
**Research**: Unlikely (contract is prescriptive, no decisions left)
**Plans**: 2 plans (2/2 complete)

Plans:
- [x] 01-01: Compose structural refactor (parametric ports, named network, bind-mount data/postgres/) - `01-01-SUMMARY.md`
- [x] 01-02: Compose lifecycle hardening (healthchecks, depends_on service_healthy, resource limits) - `01-02-SUMMARY.md` (runtime verify deferred to operator)

#### Phase 2: Backend Health Endpoints

**Goal**: Add `/healthz` (real readiness: DB reachable, Flyway applied) and `/livez` (process liveness) on the backend. Frontend exposes both as process-only liveness per the agreed simplification. Contract section 9.2.
**Depends on**: Phase 1
**Research**: Unlikely (Spring Boot Actuator + custom indicators; established pattern)
**Plans**: TBD

Plans:
- [ ] 02-01: TBD

#### Phase 3: Structured Logging

**Goal**: Production profile emits JSON to stdout with `ts`, `level`, `msg`, `service`, `request_id`, `user_id` fields. Deny-list filter for `password`, `token`, `secret`, `authorization`, `cookie`, `set_cookie`, `api_key`, `client_secret`, `private_key`. Request-id MDC propagation already exists (`TraceIdFilter`); harden it. Contract section 8.
**Depends on**: Phase 1
**Research**: Likely (Logback JSON encoder choice - logstash-logback-encoder vs Spring native Structured Logging in 3.4+)
**Research topics**: Best Logback JSON encoder for Spring Boot 3.4, deny-list filter implementation patterns, performance impact

Plans:
- [ ] 03-01: TBD

#### Phase 4: Prometheus Metrics on Main Listener

**Goal**: Expose `/metrics` on the main HTTP listener (currently only `/actuator/prometheus`). Decide between rebinding the Actuator endpoint or adding a thin alias. Verify scrape format matches Prometheus expectations. Frontend gets a minimal `/metrics` via Next route handler with `prom-client`. Contract section 9.1.
**Depends on**: Phase 1
**Research**: Unlikely (Micrometer Prometheus already wired; alias pattern well-known)

Plans:
- [ ] 04-01: TBD

#### Phase 5: Forward-Auth Mode and OIDC Reconsideration

**Goal**: Implement `AUTH_MODE=forward-auth` filter that reads `X-Forwarded-User`, `X-Forwarded-Email`, `X-Forwarded-Groups`. Source-IP gated by `TRUSTED_PROXIES` CIDR list - non-optional. Default mode stays built-in JWT. Decide whether to remove or gate the existing `OidcController` per contract section 7 (which forbids OIDC client code in this repo).
**Depends on**: Phase 1
**Research**: Likely (forward-auth header semantics across Authentik / Authelia / oauth2-proxy; secure decommission path for OIDC)
**Research topics**: Identity header naming conventions, TRUSTED_PROXIES enforcement patterns, OIDC removal vs gate

Plans:
- [ ] 05-01: TBD

#### Phase 6: Env Vars and Override Example

**Goal**: Audit and rewrite `.env.example` to enumerate every variable the app reads with one-line comments per contract section 4.1. Add `BIND_ADDR`, `HTTP_PORT`, `AUTH_MODE`, `TRUSTED_PROXIES`, `ENABLE_PG_DUMP`, `PG_DUMP_SCHEDULE`, `PG_DUMP_RETENTION_DAYS`. Ship `docker-compose.override.yml.example` for local-dev tweaks (loopback-only ports, lighter resource limits).
**Depends on**: Phase 1, Phase 5 (AUTH_MODE), Phase 7 (PG_DUMP_*)
**Research**: Unlikely

Plans:
- [ ] 06-01: TBD

#### Phase 7: Optional pg_dump Sidecar

**Goal**: Opt-in sidecar container, off by default, that runs `pg_dump --format=custom` on `PG_DUMP_SCHEDULE` cron and prunes by `PG_DUMP_RETENTION_DAYS`. Writes to `./data/backups/<date>.dump`. Contract section 5.2.
**Depends on**: Phase 1
**Research**: Likely (sidecar image choice - postgres:16-alpine + cron vs prodrigestivill/postgres-backup-local)
**Research topics**: Maintained backup sidecar images, retention pruning idiom

Plans:
- [ ] 07-01: TBD

#### Phase 8: Dependabot Alert Triage

**Goal**: Resolve the 12 outstanding security alerts (1 high, 11 moderate). Group by transitive dependency chain; bump or pin where possible; document accept-with-mitigation otherwise. No new dependencies without justification per contract section 13.
**Depends on**: Nothing (independent housekeeping)
**Research**: Unlikely (CVE-by-CVE)

Plans:
- [ ] 08-01: TBD

#### Phase 9: README and MIGRATION Docs

**Goal**: Bring `README.md` to contract section 11 spec: one-paragraph description, quick-start, configuration table from `.env.example`, exposure (Caddy / Traefik / nginx examples as references for operators), data and backup hooks, updating. Create `docs/MIGRATION.md` with v0.3.0 entry covering the named-volume to bind-mount migration.
**Depends on**: Phases 1, 6 (env vars), 7 (backup hook docs)
**Research**: Unlikely

Plans:
- [ ] 09-01: TBD

#### Phase 10: CI Gates

**Goal**: Add `gitleaks-action`, `trivy fs .`, `trivy image <built-tag>`, `docker compose -f compose.yml config`, `hadolint`, `actionlint`, `shellcheck` to `.github/workflows/ci.yml`. All gating merge to main. Contract section 12.
**Depends on**: Nothing (parallelizable with feature phases)
**Research**: Likely (current versions and configuration of each action; CodeQL interaction)
**Research topics**: Trivy ignore-policy patterns, gitleaks baseline file, hadolint rule selection

Plans:
- [ ] 10-01: TBD

#### Phase 11: GHCR Multi-Arch Image Publish

**Goal**: Extend `.github/workflows/release.yml` to build and push two multi-arch images per release: `ghcr.io/gtrows/workouthub-backend:vX.Y.Z` and `ghcr.io/gtrows/workouthub-frontend:vX.Y.Z` (linux/amd64 + linux/arm64). Digest pin in release notes. No `:latest`, no floating alias. Contract sections 3.1 and 10.
**Depends on**: Phase 10 (Trivy needs an image to scan)
**Research**: Unlikely (docker buildx + qemu pattern is standard)

Plans:
- [ ] 11-01: TBD

#### Phase 12: Release v0.3.0

**Goal**: Cut the first contract-compliant release. Run `/gtr:release 0.3.0`, push tag, validate workflow draft, attach `MIGRATION.md` excerpt to release notes, publish.
**Depends on**: All previous phases in this milestone, plus i-1 and i-2 resolved (or explicitly carried forward as known issues in the release notes)
**Research**: Unlikely

Plans:
- [ ] 12-01: TBD

### 📋 v0.4 Backend Feature Completion (Planned)

**Milestone Goal:** Land the remaining backend modules from `ProjectBrief.md` phases 3-5 - workout plan editing, live session execution, body metrics, full export refinement. Phase numbering continues from 13.

### 📋 v0.5 Frontend Completion (Planned)

**Milestone Goal:** Ship the user-facing surfaces - auth pages, dashboard, plan editor, the session-execution screen (with offline queue), history, metrics UI, profile, export UI, PWA polish.

### 📋 v0.6 Operational Maturity (Planned)

**Milestone Goal:** Real-world hardening from maintainer's own daily use - richer metrics, better error states, perf budgets, additional structured logs as gaps surface.

## Progress

**Execution Order:**
Phases execute in numeric order. Within v0.3, phases 8 and 10 are parallelizable with the feature work; everything else gates as listed under `Depends on`.

| Phase | Milestone | Plans | Status      | Completed  |
| ----- | --------- | ----- | ----------- | ---------- |
| 1. Compose refactor | v0.3 | 2/2 | Complete | 2026-05-02 |
| 2. Backend health endpoints | v0.3 | 0/? | Not started | - |
| 3. Structured logging | v0.3 | 0/? | Not started | - |
| 4. Prometheus metrics on main listener | v0.3 | 0/? | Not started | - |
| 5. Forward-auth mode | v0.3 | 0/? | Not started | - |
| 6. Env vars and override example | v0.3 | 0/? | Not started | - |
| 7. Optional pg_dump sidecar | v0.3 | 0/? | Not started | - |
| 8. Dependabot alert triage | v0.3 | 0/? | Not started | - |
| 9. README and MIGRATION docs | v0.3 | 0/? | Not started | - |
| 10. CI gates | v0.3 | 0/? | Not started | - |
| 11. GHCR multi-arch image publish | v0.3 | 0/? | Not started | - |
| 12. Release v0.3.0 | v0.3 | 0/? | Not started | - |
