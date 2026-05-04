# Roadmap: WorkoutHub

## Overview

WorkoutHub is a self-hosted multi-user fitness tracker with a Java 21 + Spring Boot 3 backend and a Next.js 15 + React 19 frontend, packaged as Docker images for operators to run on their own infrastructure. The project ships against a portable contract documented at `docs/SELF_HOSTED_CONTRACT.md`; the maintainer's reference deployment lives separately at `GTRows/homelab` and is not part of this repository.

Pre-GSD work (informally tracked in `.planning/HANDOFF.md`) delivered the application surface area through v0.2: 23 backend feature packages, 25 Flyway migrations, the offline session queue, OIDC controller, push notifications, smart-scale webhook, and JSON export. Formal GSD planning starts at v0.3. The full path from v0.4 through v1.0 is scoped below (32 phases, Phase 13-44) with v0.4 phases fully detailed and later milestones outlined.

## Domain Expertise

None - project is application code; planning draws from `docs/SELF_HOSTED_CONTRACT.md` and `.planning/codebase/` reference.

## Milestones

- (Pre-GSD) **v0.1 / v0.2** - shipped pre-GSD (informal); see `.planning/HANDOFF.md` for state snapshot
- (Shipped) [**v0.3 Self-Hosted Contract Alignment**](milestones/v0.3-ROADMAP.md) - Phases 1-12 (shipped 2026-05-03; v0.3.0/v0.3.1/v0.3.2)
- (Active) **v0.4 Backend Feature Completion** - Phases 13-20 (in progress)
- (Planned) **v0.5 Frontend Completion** - Phases 21-30
- (Planned) **v0.6 Operational Maturity** - Phases 31-37
- (Planned) **v1.0 Release Hardening** - Phases 38-44

## Phases

### v0.4 Backend Feature Completion (In Progress)

**Milestone Goal:** Complete the backend modules from `ProjectBrief.md` phases 3-5 - workout plan editing, live session execution, body metrics, full export refinement - and close the disabled-test debt carried from v0.3 (i-1, i-2).

#### Phase 13: workouts-audit

**Goal:** Inventory the existing `workouts/` package state, list missing endpoints from ProjectBrief Phase 3, and review mapper/cascade behavior to scope hardening work.
**Depends on:** v0.3 archive complete
**Research:** Unlikely (internal patterns)
**Plans:** 1

Plans:
- [x] 13-01: workouts-audit

#### Phase 14: workouts-hardening

**Goal:** Fix i-1 (WorkoutDaysIntegrationTest helper NPE, 6 disabled tests) and i-2 (FullExportImportIntegrationTest plan round-trip drop). Address mapper/cascade root causes uncovered in Phase 13.
**Depends on:** Phase 13
**Research:** Unlikely (internal debugging)
**Plans:** 2

Plans:
- [x] 14-01: workouts-hardening i-1 (cascade-id fix in WorkoutDaysService)
- [x] 14-02: workouts-hardening i-2 (export round-trip plans drop)

#### Phase 15: sessions-core

**Goal:** Land `/sessions/start`, `/sessions/active`, `/sessions/:id/sets` (POST/PUT), `/sessions/:id/finish`, `/sessions/history`, `/sessions/:id` endpoints; finalize the offline-first sync contract that the IndexedDB queue drains against.
**Depends on:** Phase 14
**Research:** Unlikely (internal patterns)
**Plans:** TBD

Plans:
- [ ] 15-01: TBD

#### Phase 16: sessions-analytics

**Goal:** Add `/exercises/:id/last-performance` and `/exercises/:id/progress` endpoints; introduce PR computation, volume aggregation, and 1RM (Epley) projections at the query layer.
**Depends on:** Phase 15
**Research:** Unlikely (internal patterns)
**Plans:** TBD

Plans:
- [ ] 16-01: TBD

#### Phase 17: body-metrics

**Goal:** Complete the `metrics/` package: weight, body measurements, optional progress photo URL, and time-series read endpoints for the metrics UI.
**Depends on:** Phase 16
**Research:** Unlikely (internal patterns)
**Plans:** TBD

Plans:
- [ ] 17-01: TBD

#### Phase 18: export-refinement

**Goal:** Align `/api/export/claude-summary` output with the ProjectBrief example (period, summary, prs, consistency sections); make the import endpoint round-trip-stable per the i-2 fix.
**Depends on:** Phase 17
**Research:** Unlikely (internal patterns)
**Plans:** TBD

Plans:
- [ ] 18-01: TBD

#### Phase 19: api-contract-docs

**Goal:** Generate OpenAPI via SpringDoc, refresh `docs/API.md`, document JWT and forward-auth schemes, and verify the surface matches `docs/SELF_HOSTED_CONTRACT.md` section 7.
**Depends on:** Phase 18
**Research:** Likely (SpringDoc 2.x with Spring Boot 3.5 + Java 21, JWT bearer auth scheme, actuator exclusion)
**Research topics:** SpringDoc 2.x configuration on Spring Boot 3.5.x, JWT bearer token security scheme definition, excluding actuator and management endpoints from public docs.
**Plans:** TBD

Plans:
- [ ] 19-01: TBD

#### Phase 20: release-v0-4

**Goal:** Run a CVE patch sweep on backend dependencies, update `CHANGELOG.md`, `RELEASE.md`, and `docs/MIGRATION.md`, and cut `v0.4.0` to GHCR.
**Depends on:** Phase 19
**Research:** Unlikely (release runbook established in v0.3)
**Plans:** TBD

Plans:
- [ ] 20-01: TBD

### v0.5 Frontend Completion (Planned)

**Milestone Goal:** Ship the user-facing surfaces - auth pages, dashboard, plan editor, session-execution screen with the offline queue wired to the v0.4 sync contract, history, metrics UI, profile, export UI.

Phase outline (full breakdown deferred to /gsd:new-milestone when v0.4 ships):
- Phase 21: frontend-audit - inventory routes/components, classify stubs vs real surfaces.
- Phase 22: auth-pages - login/register, client session, refresh token flow, error states.
- Phase 23: dashboard - today's workout card, weekly summary, last weight, quick actions.
- Phase 24: plan-editor - weekly view, drag-drop reorder (days + exercises), exercise CRUD modal.
- Phase 25: session-execution - set logging UI, rest timer, last-performance display, IndexedDB offline queue and sync.
- Phase 26: exercise-catalog - filters (muscle/equipment/difficulty), search, detail modal (how-to, tips, mistakes, PR, progress chart).
- Phase 27: history-view - calendar with completed days marked, per-session detail page.
- Phase 28: metrics-ui - weight chart (weekly/monthly/all-time), measurement form, optional progress photo upload.
- Phase 29: profile-settings - user info, health notes, goals, supplements list editing.
- Phase 30: export-import-ui - JSON download, JSON restore, "Claude summary" action button.

### v0.6 Operational Maturity (Planned)

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
Phases execute in numeric order. v0.4 starts at Phase 13.

| Phase                       | Milestone | Plans | Status      | Completed  |
| --------------------------- | --------- | ----- | ----------- | ---------- |
| 1-12 (v0.3 scope)           | v0.3      | 14/14 | Complete    | 2026-05-03 |
| 13. workouts-audit          | v0.4      | 1/1   | Complete    | 2026-05-04 |
| 14. workouts-hardening      | v0.4      | 2/2   | Complete    | 2026-05-04 |
| 15. sessions-core           | v0.4      | 0/?   | Not started | -          |
| 16. sessions-analytics      | v0.4      | 0/?   | Not started | -          |
| 17. body-metrics            | v0.4      | 0/?   | Not started | -          |
| 18. export-refinement       | v0.4      | 0/?   | Not started | -          |
| 19. api-contract-docs       | v0.4      | 0/?   | Not started | -          |
| 20. release-v0-4            | v0.4      | 0/?   | Not started | -          |
| 21-30 (v0.5 scope)          | v0.5      | 0/?   | Planned     | -          |
| 31-37 (v0.6 scope)          | v0.6      | 0/?   | Planned     | -          |
| 38-44 (v1.0 scope)          | v1.0      | 0/?   | Planned     | -          |
