# Roadmap: WorkoutHub

## Overview

WorkoutHub is a self-hosted multi-user fitness tracker with a Java 21 + Spring Boot 3 backend and a Next.js 15 + React 19 frontend, packaged as Docker images for operators to run on their own infrastructure. The project ships against a portable contract documented at `docs/SELF_HOSTED_CONTRACT.md`; the maintainer's reference deployment lives separately at `GTRows/homelab` and is not part of this repository.

Pre-GSD work (informally tracked in `.planning/HANDOFF.md`) delivered the application surface area through v0.2: 23 backend feature packages, 25 Flyway migrations, the offline session queue, OIDC controller, push notifications, smart-scale webhook, and JSON export. Formal GSD planning starts at v0.3. The full path from v0.4 through v1.0 is scoped below (32 phases, Phase 13-44); v0.3, v0.4, and v0.5 are shipped, v0.6 is active, v1.0 is planned.

## Domain Expertise

None - project is application code; planning draws from `docs/SELF_HOSTED_CONTRACT.md` and `.planning/codebase/` reference.

## Milestones

- (Pre-GSD) **v0.1 / v0.2** - shipped pre-GSD (informal); see `.planning/HANDOFF.md` for state snapshot
- (Shipped) [**v0.3 Self-Hosted Contract Alignment**](milestones/v0.3-ROADMAP.md) - Phases 1-12 (shipped 2026-05-03; v0.3.0/v0.3.1/v0.3.2)
- (Shipped) [**v0.4 Backend Feature Completion**](milestones/v0.4-ROADMAP.md) - Phases 13-20 (shipped 2026-05-07; v0.4.0)
- (Shipped) [**v0.5 Frontend Completion**](milestones/v0.5-ROADMAP.md) - Phases 21-30 (shipped 2026-05-09; v0.5.0)
- (Active) **v0.6 Operational Maturity** - Phases 31-37
- (Planned) **v1.0 Release Hardening** - Phases 38-44

## Phases

<details>
<summary>v0.4 Backend Feature Completion (Phases 13-20) - SHIPPED 2026-05-07</summary>

Full archive: [milestones/v0.4-ROADMAP.md](milestones/v0.4-ROADMAP.md).

- [x] Phase 13: workouts-audit (1/1 plan) - completed 2026-05-04
- [x] Phase 14: workouts-hardening (2/2 plans) - completed 2026-05-04
- [x] Phase 15: sessions-core (4/4 plans) - completed 2026-05-04
- [x] Phase 16: sessions-analytics (4/4 plans) - completed 2026-05-05
- [x] Phase 17: body-metrics (4/4 plans) - completed 2026-05-05
- [x] Phase 18: export-refinement (4/4 plans) - completed 2026-05-06
- [x] Phase 19: api-contract-docs (5/5 plans) - completed 2026-05-07
- [x] Phase 20: release-v0-4 (1/1 plan) - completed 2026-05-07

</details>

<details>
<summary>v0.5 Frontend Completion (Phases 21-30) - SHIPPED 2026-05-09</summary>

Full archive: [milestones/v0.5-ROADMAP.md](milestones/v0.5-ROADMAP.md).

- [x] Phase 21: frontend-audit (1/1 plan) - completed 2026-05-07
- [x] Phase 22: auth-pages (1/1 plan) - completed 2026-05-07
- [x] Phase 22.5: typed-apierror-helper (1/1 plan, INSERTED) - completed 2026-05-08
- [x] Phase 23: dashboard (1/1 plan) - completed 2026-05-08
- [x] Phase 24: plan-editor (4/4 plans) - completed 2026-05-08
- [x] Phase 25: session-execution (4/4 plans) - completed 2026-05-08
- [x] Phase 25.5: zod-schemas (1/1 plan, INSERTED) - completed 2026-05-08
- [x] Phase 26: exercise-catalog (1/1 plan) - completed 2026-05-08
- [x] Phase 27: history-view (1/1 plan) - completed 2026-05-08
- [x] Phase 28: metrics-ui (1/1 plan) - completed 2026-05-09
- [x] Phase 29: profile-settings (1/1 plan) - completed 2026-05-09
- [x] Phase 29.5: middleware-matcher-gap (1/1 plan, INSERTED) - completed 2026-05-09
- [x] Phase 30: export-import-ui (1/1 plan) - completed 2026-05-09

</details>

### v0.6 Operational Maturity (Active)

**Milestone Goal:** Real-world hardening for daily use - statistics dashboards, PWA install, web push, frontend metrics, error states, perf budgets, and resolving the v0.3 logging-test debt.

Phase outline:
- Phase 31: charts-stats - volume, 1RM (Epley), weight change, frequency heatmap, PR list, streak (ProjectBrief Phase 6).
- Phase 32: pwa-polish - install prompt, offline shell, service worker scope, app icon and manifest.
- Phase 33: web-push-notifications - workout/weight/supplement reminders wired to existing backend push package.
- Phase 34: rest-timer-notifications - background rest-timer push during sessions.
- Phase 35: frontend-http-metrics - i-4: Next middleware request count + duration histogram on `/api/metrics`.
- Phase 36: error-states-perf-budgets - loading skeletons, error boundaries, Lighthouse and perf budgets.
- Phase 37: structured-logging-test-fix - i-9: redesign StructuredLoggingTest to boot the full app so prod logging activates.

### v1.0 Release Hardening (Planned)

**Milestone Goal:** Production-ready cut - end-to-end tests, framework major bumps, security hardening, full documentation, backup/restore drill, `v1.0.0` release.

Phase outline:
- Phase 38: e2e-tests - Playwright critical flows: register -> login -> workout -> session save.
- Phase 39: testcontainers-major - i-7: testcontainers 1.x -> 2.x with AbstractIntegrationTest and migration test updates.
- Phase 40: framework-majors - i-8 Spring Boot 4, i-6 Next 16, i-5 next-intl 4 in sequence (each its own commit and test pass).
- Phase 41: security-hardening - rate limiting review, brute-force lockout finalization, refresh-token hash collision fix from pending CI memory.
- Phase 42: docs-completion - `docs/API.md`, `docs/DEPLOYMENT.md`, `docs/DATA_SCHEMA.md`, README user-facing rewrite.
- Phase 43: backup-restore-drill - pg_dump sidecar restore drill, backup cron example, MIGRATION updates.
- Phase 44: release-v1-0 - final CHANGELOG, RELEASE notes, GitHub release, multi-arch GHCR `v1.0.0` cut, deploy validation.

## Progress

**Execution Order:**
Phases execute in numeric order. v0.6 starts at Phase 31.

| Phase                       | Milestone | Plans | Status      | Completed  |
| --------------------------- | --------- | ----- | ----------- | ---------- |
| 1-12 (v0.3 scope)           | v0.3      | 14/14 | Complete    | 2026-05-03 |
| 13-20 (v0.4 scope)          | v0.4      | 25/25 | Complete    | 2026-05-07 |
| 21-30 (v0.5 scope)          | v0.5      | 19/19 | Complete    | 2026-05-09 |
| 31-37 (v0.6 scope)          | v0.6      | 0/?   | Active      | -          |
| 38-44 (v1.0 scope)          | v1.0      | 0/?   | Planned     | -          |
