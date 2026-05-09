# Roadmap: WorkoutHub

## Overview

WorkoutHub is a self-hosted multi-user fitness tracker with a Java 21 + Spring Boot 3 backend and a Next.js 15 + React 19 frontend, packaged as Docker images for operators to run on their own infrastructure. The project ships against a portable contract documented at `docs/SELF_HOSTED_CONTRACT.md`; the maintainer's reference deployment lives separately at `GTRows/homelab` and is not part of this repository.

Pre-GSD work (informally tracked in `.planning/HANDOFF.md`) delivered the application surface area through v0.2: 23 backend feature packages, 25 Flyway migrations, the offline session queue, OIDC controller, push notifications, smart-scale webhook, and JSON export. Formal GSD planning starts at v0.3. The full path from v0.4 through v1.0 is scoped below (32 phases, Phase 13-44); v0.3, v0.4, v0.5, v0.6, and v1.0 are shipped. v1.1 (Phases 45-50) is now active to close the four open carry-forward issues from v1.0 (i-13, i-8b, i-6b, i-15).

## Domain Expertise

None - project is application code; planning draws from `docs/SELF_HOSTED_CONTRACT.md` and `.planning/codebase/` reference.

## Milestones

- (Pre-GSD) **v0.1 / v0.2** - shipped pre-GSD (informal); see `.planning/HANDOFF.md` for state snapshot
- (Shipped) [**v0.3 Self-Hosted Contract Alignment**](milestones/v0.3-ROADMAP.md) - Phases 1-12 (shipped 2026-05-03; v0.3.0/v0.3.1/v0.3.2)
- (Shipped) [**v0.4 Backend Feature Completion**](milestones/v0.4-ROADMAP.md) - Phases 13-20 (shipped 2026-05-07; v0.4.0)
- (Shipped) [**v0.5 Frontend Completion**](milestones/v0.5-ROADMAP.md) - Phases 21-30 (shipped 2026-05-09; v0.5.0)
- (Shipped) [**v0.6 Operational Maturity**](milestones/v0.6-ROADMAP.md) - Phases 31-37 (shipped 2026-05-09; v0.6.0)
- (Shipped) [**v1.0 Release Hardening**](milestones/v1.0-ROADMAP.md) - Phases 38-44 (shipped 2026-05-09; v1.0.0)
- (Active) **v1.1 Deferred Debt Closure** - Phases 45-50

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

<details>
<summary>v0.6 Operational Maturity (Phases 31-37) - SHIPPED 2026-05-09</summary>

Full archive: [milestones/v0.6-ROADMAP.md](milestones/v0.6-ROADMAP.md).

- [x] Phase 31: charts-stats (1/1 plan) - completed 2026-05-09
- [x] Phase 32: pwa-polish (1/1 plan) - completed 2026-05-09
- [x] Phase 33: web-push-notifications (1/1 plan) - completed 2026-05-09
- [x] Phase 34: rest-timer-notifications (1/1 plan) - completed 2026-05-09
- [x] Phase 35: frontend-http-metrics (1/1 plan) - completed 2026-05-09
- [x] Phase 36: error-states-perf-budgets (3/3 plans) - completed 2026-05-09
- [x] Phase 37: structured-logging-test-fix (1/1 plan) - completed 2026-05-09

</details>

<details>
<summary>v1.0 Release Hardening (Phases 38-44) - SHIPPED 2026-05-09</summary>

Full archive: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md).

- [x] Phase 38: e2e-tests (1/1 plan) - completed 2026-05-09
- [x] Phase 39: testcontainers-major (1/1 plan) - completed 2026-05-09
- [x] Phase 40: framework-majors (3/3 plans) - completed 2026-05-09
- [x] Phase 41: security-hardening (1/1 plan) - completed 2026-05-09
- [x] Phase 42: docs-completion (1/1 plan) - completed 2026-05-09
- [x] Phase 43: backup-restore-drill (1/1 plan) - completed 2026-05-09
- [x] Phase 44: release-v1-0 (1/1 plan) - completed 2026-05-09

</details>

### v1.1 Deferred Debt Closure (Active)

**Milestone Goal:** Close the four open carry-forward issues from v1.0 (i-13 SpringDoc 2.7+, i-8b Spring Boot 4 via Jackson 2 -> 3 migration, i-6b Next 16, i-15 Lighthouse CI + bundle-size + bundle-analyzer) and cut `v1.1.0`. The protected `.github/workflows/**` and `scripts/**` edits required by Phase 49 are pre-approved by the user as part of opening this milestone.

Phase outline:
- Phase 45: springdoc-2.7 - i-13: bump SpringDoc to 2.7+ in `backend/pom.xml`; remove the `OpenApiConfig` `ControllerAdviceBean` workaround; re-run the OpenApi integration tests.
- Phase 46: jackson-2-to-3-migration - preparatory for Spring Boot 4: migrate the 36 Jackson 2 files (`com.fasterxml.jackson.*` -> `tools.jackson.*`); re-validate `@JsonNaming(SnakeCaseStrategy)`, `@JsonInclude(NON_NULL)`, `@JsonProperty`, `@JsonIgnore`, `@JsonCreator`, `@JsonValue`, custom serializers/deserializers, and `ObjectMapper` config under the Jackson 3 API.
- Phase 47: spring-boot-4 - i-8b: bump `<spring-boot-starter-parent>` from 3.5.14 to the latest stable 4.x; apply the Jackson2-module + Prometheus-actuator package relocations enumerated in i-8b; audit the Spring Framework 7 / Spring Security 7 / Hibernate 7 / Jakarta EE 11 BOM transitives; CI-validate.
- Phase 48: next-16 - i-6b: bump `next` from `^15.x` to `^16.x` in `frontend/package.json`; rename `frontend/src/middleware.ts` -> `proxy.ts` (or accept the deprecation warning); wrap `frontend/src/app/(auth)/login/page.tsx` in `<Suspense>`; bump `engines.node` to `>=20.9.0`; audit `images.localPatterns` for query-string `src` callers; decide Turbopack vs `--webpack`; regenerate `frontend/pnpm-lock.yaml`.
- Phase 49: lighthouse-ci - i-15: add `.github/workflows/lighthouse.yml` (protected `.github/workflows/**` edit, user pre-approved); add `scripts/check-bundle-size.mjs` (protected `scripts/**` edit, user pre-approved); add `@next/bundle-analyzer` devDependency to `frontend/package.json`.
- Phase 50: release-v1-1 - bump `IDENTITY.yaml` to 1.1.0 (lockstep `package.json` + derived manifests); rotate `CHANGELOG.md` `## [Unreleased]` -> `## [1.1.0] - <date>`; commit; tag `v1.1.0` (no push).

## Progress

**Execution Order:**
Phases execute in numeric order. v1.1 starts at Phase 45.

| Phase                       | Milestone | Plans | Status      | Completed  |
| --------------------------- | --------- | ----- | ----------- | ---------- |
| 1-12 (v0.3 scope)           | v0.3      | 14/14 | Complete    | 2026-05-03 |
| 13-20 (v0.4 scope)          | v0.4      | 25/25 | Complete    | 2026-05-07 |
| 21-30 (v0.5 scope)          | v0.5      | 19/19 | Complete    | 2026-05-09 |
| 31-37 (v0.6 scope)          | v0.6      | 11/11 | Complete    | 2026-05-09 |
| 38-44 (v1.0 scope)          | v1.0      | 9/9   | Complete    | 2026-05-09 |
| 45-50 (v1.1 scope)          | v1.1      | 0/?   | Active      | -          |
