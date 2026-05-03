# Project Milestones: WorkoutHub

## v0.3 Self-Hosted Contract Alignment (Shipped: 2026-05-03)

**Delivered:** First contract-compliant self-hosted release. Operators on amd64 or arm64 hosts can pull pinned `vX.Y.Z` images from `ghcr.io/gtrows/workouthub-{backend,frontend}` and run the stack via Docker Compose with parametric port bindings, real readiness checks, structured JSON logs, contract-aligned env vars, and an opt-in backup sidecar.

**Phases completed:** 1-12 (14 plans total)

**Releases:** v0.3.0 (initial cut, draft release blocked by post-tag regression discovery), v0.3.1 (CVE wave + bean-collision fix, release workflow failed on GHCR tag casing), v0.3.2 (release-workflow lowercase fix, published cleanly).

**Key accomplishments:**

- Compose stack contract-compliant: parametric `BIND_ADDR` / `BACKEND_PORT` / `FRONTEND_PORT` / `POSTGRES_PORT`, named `workouthub-net` network, bind-mount `./data/postgres/`, healthchecks + `condition: service_healthy`, resource limits.
- Backend `/livez` (process) and `/healthz` (DB + Flyway readiness) endpoints; frontend `/api/healthz` + `/api/livez` route handlers (process liveness only).
- Spring Boot 3.4 native ECS structured logging on the `prod` profile with deny-list masking via `JsonWriter.Members#applyingValueProcessor` (no logstash-logback-encoder dep).
- Prometheus `/metrics` aliased on the main HTTP listener; `/actuator/prometheus` preserved for backward compat.
- `AUTH_MODE=forward-auth` filter gated by a `TRUSTED_PROXIES` CIDR list; OIDC client surface deleted entirely per contract section 7.
- Canonical `.env.example` (124 lines, 11 sections) and a shipped `docker-compose.override.yml.example` for ARM-friendly tuning.
- Opt-in `pg_dump` sidecar gated by the `backup` profile (off by default).
- 12 of 17 Dependabot PRs merged (1 HIGH bouncycastle CVE closed, 5 patches, 5 GH Actions majors, 2 dev-only majors); 4 breaking-change majors deferred via ISSUES.md i-5..i-8.
- README rewrite + `docs/MIGRATION.md` per contract section 11; per-version migration entries in place.
- 9-job CI gate suite (gitleaks, trivy fs, trivy image x2, hadolint x2, actionlint, shellcheck, docker compose config) plus the existing backend mvn verify and frontend lint+test+typecheck.
- Multi-arch (amd64 + arm64) GHCR publish via release workflow with no `:latest`, digest pins in release notes.
- Patch window v0.3.1+v0.3.2 closed 9 additional CVEs (Spring Boot 3.4.1 -> 3.5.14, web-push transitive overrides, Alpine OS upgrade) and fixed two release-blocking regressions (Spring bean-name collision, GHCR tag casing).

**Stats:**

- 14 plans across 12 phases, plus a ~24h post-release patch window with 17 follow-up commits (v0.3.0 -> v0.3.2).
- Files modified across the milestone: 30 (post-release window) + larger pre-tag set across all phases.
- Timeline: 2026-05-02 (Phase 1 start) to 2026-05-03 (v0.3.2 cut) = ~2 days for the core milestone, +1 day for the patch window.

**Git range:** `feat(01-01)` through `chore(release): v0.3.2`. Tags: `v0.3.0`, `v0.3.1`, `v0.3.2`.

**What's next:** v0.4 Backend Feature Completion - workouts, sessions, metrics, export modules per `ProjectBrief.md` phases 3-5. Phase numbering continues from 13.

**Archive:** [milestones/v0.3-ROADMAP.md](milestones/v0.3-ROADMAP.md)

---
