# Roadmap: WorkoutHub

## Overview

WorkoutHub is a self-hosted multi-user fitness tracker with a Java 21 + Spring Boot 3 backend and a Next.js 15 + React 19 frontend, packaged as Docker images for operators to run on their own infrastructure. The project ships against a portable contract documented at `docs/SELF_HOSTED_CONTRACT.md`; the maintainer's reference deployment lives separately at `GTRows/homelab` and is not part of this repository.

Pre-GSD work (informally tracked in `.planning/HANDOFF.md`) delivered the application surface area through v0.2: 23 backend feature packages, 25 Flyway migrations, the offline session queue, OIDC controller, push notifications, smart-scale webhook, and JSON export. Formal GSD planning starts at v0.3.

## Domain Expertise

None - project is application code; planning draws from `docs/SELF_HOSTED_CONTRACT.md` and `.planning/codebase/` reference.

## Milestones

- ⚪ **v0.1 / v0.2** - shipped pre-GSD (informal); see `.planning/HANDOFF.md` for state snapshot
- ✅ [**v0.3 Self-Hosted Contract Alignment**](milestones/v0.3-ROADMAP.md) - Phases 1-12 (shipped 2026-05-03; v0.3.0/v0.3.1/v0.3.2)
- 📋 **v0.4 Backend Feature Completion** - planned (workouts, sessions, metrics, export modules per ProjectBrief phases 3-5); phase numbering continues from 13
- 📋 **v0.5 Frontend Completion** - planned (auth pages, dashboard, plan editor, session execution UI, history, metrics, profile, export)
- 📋 **v0.6 Operational Maturity** - planned (richer metrics, error states, performance budgeting)

## Phases

### 📋 v0.4 Backend Feature Completion (Planned)

**Milestone Goal:** Land the remaining backend modules from `ProjectBrief.md` phases 3-5 - workout plan editing, live session execution, body metrics, full export refinement. Phase numbering continues from 13.

### 📋 v0.5 Frontend Completion (Planned)

**Milestone Goal:** Ship the user-facing surfaces - auth pages, dashboard, plan editor, the session-execution screen (with offline queue), history, metrics UI, profile, export UI, PWA polish.

### 📋 v0.6 Operational Maturity (Planned)

**Milestone Goal:** Real-world hardening from maintainer's own daily use - richer metrics, better error states, perf budgets, additional structured logs as gaps surface.

## Progress

**Execution Order:**
Phases execute in numeric order. v0.4 starts at Phase 13.

| Phase             | Milestone | Plans | Status      | Completed  |
| ----------------- | --------- | ----- | ----------- | ---------- |
| 1-12 (v0.3 scope) | v0.3      | 14/14 | Complete    | 2026-05-03 |
| 13. (v0.4 first)  | v0.4      | 0/?   | Not started | -          |
