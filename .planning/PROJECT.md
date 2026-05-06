# WorkoutHub

## What This Is

Self-hosted multi-user fitness tracker with workout logging, progress charts, body metrics, and JSON export for Claude (AI coach) analysis. Java 21 + Spring Boot 3 backend, Next.js 15 + React 19 + TypeScript frontend, PostgreSQL 16, packaged as Docker images and shipped against the operator-portable contract in `docs/SELF_HOSTED_CONTRACT.md`. Mobile-first PWA, default locale Turkish, code English.

## Core Value

A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.

## Requirements

### Validated

<!-- Shipped before GSD planning (v0.1 / v0.2) and through v0.3 milestone. -->

- [x] JWT auth (register, login, refresh, password reset, brute-force lockout) - pre-GSD v0.1/v0.2
- [x] User profile (height, weight, birth date, gender, health notes, goals, supplements) - pre-GSD
- [x] Exercise master catalog with seed data, search, filters, admin CRUD - pre-GSD
- [x] Body metrics base (weight, body-fat, waist, chest, arm, thigh, photo URL, notes) - pre-GSD
- [x] Smart-scale webhook ingest path - pre-GSD
- [x] Garmin .fit health-import endpoint - pre-GSD
- [x] Push notifications (VAPID, web push) - pre-GSD
- [x] OIDC controller scaffolding - pre-GSD (kept dormant per contract; built-in auth is default)
- [x] JSON full-export and import (round-trip stable per Phase 14 i-2 fix) - pre-GSD + v0.4 Phase 14
- [x] Self-hosted contract baseline: parametric `BIND_ADDR`, pinned tags, multi-arch GHCR publish, `/livez`+`/healthz`, structured JSON logs, optional `pg_dump` sidecar, forward-auth opt-in - v0.3
- [x] CI gates: lint, test, gitleaks, trivy, compose config validation, multi-arch image build - v0.3
- [x] Workouts package: weekly plan + days + day-exercises CRUD; cascade-id fix for child entity create - v0.4 Phases 13-14
- [x] Sessions core: start, active, sets POST/PUT, finish, history, detail; clientSetId idempotency; typed 409 codes (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`); heart-rate field exposure; auto-numbering documented as live-online only - v0.4 Phase 15
- [x] Sessions analytics: `/exercises/:id/last-performance`, `/exercises/:id/progress`; package extraction to `com.workouthub.analytics`; durable `is_pr` column with backfill; Epley 1RM projection on progress points - v0.4 Phase 16
- [x] Body-metrics API surface: `photo_url` exposure on upsert, optional `from`/`to` range filter on GET `/api/metrics` - v0.4 Phase 17 (plans 01-03 of 4)

### Active

<!-- v0.4 remaining work and forward path through v1.0. -->

- [ ] Phase 17 plan 04: status-code split (201 on create / 200 on update via `wasCreated` wrapper); optional `BodyMetricValidator` static-helper absorb
- [ ] Phase 18: `/api/export/claude-summary` aligned with ProjectBrief example (period, summary, prs, consistency); import round-trip stable
- [ ] Phase 19: OpenAPI via SpringDoc; refresh `docs/API.md`; document JWT and forward-auth schemes
- [ ] Phase 20: CVE patch sweep; CHANGELOG, RELEASE, MIGRATION; cut `v0.4.0` to GHCR
- [ ] v0.5 (Phases 21-30): full frontend - auth pages, dashboard, plan editor, session-execution screen with IndexedDB drain wired to v0.4 sync contract, history, exercise catalog, metrics UI, profile, export/import UI
- [ ] v0.6 (Phases 31-37): charts/stats (volume, 1RM Epley, weight, frequency heatmap, PR list, streak), PWA polish, web push, rest-timer notifications, frontend HTTP metrics (i-4), error states + perf budgets, structured-logging test fix (i-9)
- [ ] v1.0 (Phases 38-44): Playwright e2e, testcontainers 2.x (i-7), framework majors (Spring Boot 4 i-8, Next 16 i-6, next-intl 4 i-5), security hardening, docs completion, backup/restore drill, `v1.0.0` release

### Out of Scope

- AI/LLM integration inside the app (e.g. embedded Claude API client) - user explicitly declined; export to JSON and paste into chat is the contract
- OIDC client / OAuth flow / external identity provider session - per `docs/SELF_HOSTED_CONTRACT.md` section 7, that layer lives in the operator's reverse-proxy SSO gateway; this repo only carries built-in JWT and forward-auth header trust
- Reverse proxy, TLS termination, DNS, alerting, host firewalling, backup execution - operator concerns, not application concerns
- Maintainer-specific deployment artifacts (Cloudflare Tunnel client, Tailscale daemon, litestream config tied to one bucket) - belong in the separate `GTRows/homelab` deployment repo
- `:latest` tag, Watchtower, in-app auto-updater - banned by contract section 10
- Named Docker volumes for stateful data - contract requires `./data/<component>/` bind mounts so backup tools can find data
- MinIO / object-storage sidecar in this repo - deferred until photo upload pipeline lands (Phase 28+)
- Nutrition/meal tracking, water tracker, gamification, period tracking, Google Fit / Apple Health export - bonus features outside MVP path

## Context

- **Solo project, Turkish-first product, English code/comments.** UI strings live in `next-intl` resource files (`tr` default, `en` available); Java sources and identifiers are English only.
- **Brownfield as of GSD adoption.** Pre-GSD work shipped v0.1 + v0.2 informally (23 backend feature packages, 25 Flyway migrations, IndexedDB offline queue, OIDC controller, web push, smart-scale webhook, JSON export). GSD planning began at v0.3 (self-hosted contract alignment).
- **Local Maven gap.** This Windows host has no `mvnw` and no system `mvn`; backend tests run only in CI. Frontend tests run locally (vitest, Playwright).
- **CI was red on `main`** at GSD adoption due to three pre-existing bean-name-conflict failures (refresh-token hash collisions, brute-force lockout, export-format example drift). Closed during v0.3.
- **Self-hosted contract is binding.** `docs/SELF_HOSTED_CONTRACT.md` overrides any conflicting application-level guidance. Operator-portable env vars, parametric bind address, structured JSON logs, multi-arch images, no `:latest`.
- **GHCR image publishing:** `ghcr.io/gtrows/workouthub-backend` and `-frontend`, multi-arch (amd64+arm64), separate version cadences allowed but currently aligned.
- **Reference deployment:** maintained separately at `GTRows/homelab` (not part of this repo).
- **Schema source of truth:** Flyway migrations under `backend/src/main/resources/db/migration/V<n>__*.sql`. Once merged, immutable. JPA entities are validated, never auto-generated (`ddl-auto: validate`).
- **Offline-first execution:** Dexie IndexedDB queue scoped to the session-execution flow drains via `clientSetId` idempotency key; 200 = replay, 201 = newly created.

## Constraints

- **Tech stack:** Java 21 + Spring Boot 3.5.x backend; Next.js 15 + React 19 + TypeScript strict frontend; PostgreSQL 16. No H2 in tests - Testcontainers only.
- **Deployment:** Single-host Docker Compose. Three services (`db`, `backend`, `frontend`). Bind-mount volumes under `./data/<component>/`.
- **Auth:** Built-in JWT default; `AUTH_MODE=forward-auth` opt-in via env. No OIDC client code in repo.
- **Bind address:** `${BIND_ADDR:-127.0.0.1}` parametric, never `0.0.0.0`.
- **Database:** PostgreSQL only. SQLite would simplify single-user homelab but multi-user + concurrent writers + JSONB rule it out.
- **Versioning:** Semantic versioning via `IDENTITY.yaml`. Tag push `vX.Y.Z` triggers GHCR publish + GitHub Release (draft-first, manual publish).
- **Security:** No real secrets in source. `gitleaks` blocks the commit; `--no-verify` is not allowed. Trivy fs + image CVE scans in CI.
- **Quality:** JaCoCo backend line coverage threshold 70%. Frontend coverage thresholds: 70/70/60/60 (lines/statements/functions/branches).
- **Mobile-first responsive design;** workout-execution screen target < 2s open on warm cache.
- **No emojis** anywhere in code, comments, or commit messages.
- **No TODO comments;** deferred work tracked in `.planning/ISSUES.md` (currently i-3 through i-9 open).

## Key Decisions

| Decision                                                                | Rationale                                                                                                              | Outcome                          |
| ----------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| Backend: Java 21 + Spring Boot 3 (not Node.js / FastAPI as originally floated) | LTS toolchain, mature JPA + Flyway story, native JSON support, single-binary deploy via Docker                         | Good (v0.1 onward)               |
| Frontend: Next.js 15 App Router + RSC by default                        | Server Components reduce client JS; PWA + offline still feasible via `"use client"` islands                            | Good (v0.1 onward)               |
| Database: PostgreSQL 16 (not SQLite)                                    | Multi-user, concurrent writers, JSONB, full-text search future-proofing                                                 | Good                             |
| Schema authority: Flyway migrations, `ddl-auto=validate`                | JPA entity drift cannot silently mutate schema; migrations are the source of truth and are immutable once merged       | Good                             |
| Tests: Testcontainers (real Postgres), no H2                            | Production-equivalent behavior; H2 dialect drift bit projects before                                                   | Good                             |
| Auth: built-in JWT default; forward-auth opt-in; no OIDC client in repo | Per `docs/SELF_HOSTED_CONTRACT.md` section 7 - identity provider sessions belong in operator's SSO gateway              | Good (locked v0.3)               |
| Volumes: bind-mounts under `./data/`, not named Docker volumes          | Operators target `./data/` with whatever backup tool they choose; named volumes are invisible to backup tooling         | Good (locked v0.3)               |
| Bind address parametric `${BIND_ADDR:-127.0.0.1}`                       | Default safe (loopback); operator picks LAN / VPN interface; never `0.0.0.0`                                            | Good (locked v0.3)               |
| Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` multi-arch amd64+arm64; no `:latest` | Contract section 10; supports homelab ARM SBCs and x86 servers                                              | Good (locked v0.3)               |
| Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`); no in-app auto-update daemon | Contract section 10; operator owns when to upgrade                                                            | Good                             |
| AI integration deferred indefinitely                                    | User explicitly declined embedded Claude API; JSON export to paste into chat is the design                              | Good (locked decision)           |
| Reference deployment lives in separate `GTRows/homelab` repo            | This repo ends at the registry; operator-specific deployment is out of scope                                            | Good                             |
| OIDC controller kept dormant in repo                                    | Built earlier; per contract, real OIDC client should not ship here. Marked deferred / removable; review at v1.0 cleanup | - Pending (revisit at Phase 41)  |
| Local Maven not installed on this host; backend tests run only in CI    | Workflow constraint; plans skip the local mvn verify gate and delegate to CI                                            | - Pending (acceptable while solo)|

---
*Last updated: 2026-05-06 after brownfield synthesis from CLAUDE.md, ProjectBrief.md, ROADMAP.md, IDENTITY.yaml, .planning/codebase/, docs/SELF_HOSTED_CONTRACT.md, .planning/HANDOFF.md, .planning/ISSUES.md*
