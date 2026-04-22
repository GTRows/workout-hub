# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## First-time setup check

Before doing any coding work, check for `.claude/.setup-complete`.
- If missing: recommend `/setup` to the user and wait for confirmation before starting implementation. Read-only questions and template maintenance are fine without it.
- If present: proceed normally.

## Available commands

- `/tpl` — list every template command, hook, and file in this repo.
- `/setup` — first-time wizard (only needed once per clone).
- `/task <subcommand>` — manage persistent TODO.md tasks. `/task` with no args prints usage.
- `/doctor` — read-only health check.
- `/release <version>` — prepare a release (bump, rotate CHANGELOG, commit, tag). Never pushes.
- Plugin commands: `/commit`, `/commit-push-pr`, `/review-pr`, `/revise-claude-md`, `/create-skill`.

## Task workflow

Two layers — do not conflate them:

1. **TODO.md (persistent)** — durable tasks across sessions. Managed via `/task`. Each entry has an id (`t-N`), a title, and an Acceptance line. Sections: Active / Blocked / Done.
2. **Built-in TaskCreate (ephemeral)** — this session's subtask breakdown of whatever TODO task is in flight. Use it to plan and track step-by-step work within the conversation. Do not mirror TODO.md into it.

When starting work:
- Run `/task next` (or pick a task manually) — this moves it to the top of Active.
- Break it into `TaskCreate` subtasks.
- Step through subtasks one at a time. Mark each completed as soon as done, not in a batch.
- When the TODO task's acceptance is met AND tests pass AND a commit exists referencing its id, call `/task done <id>`. The verification gate in `/task` enforces this.

Do not leave a task half-implemented to start another. Finish or explicitly block (`/task block <id> <reason>`).

## Project Overview

**Name:** workout-hub
**Display Name:** WorkoutHub
**Description:** Self-hosted multi-user fitness tracker with workout logging, progress charts, and JSON export for AI coach analysis.
**Platform:** Web (self-hosted via Docker Compose). Mobile-first responsive PWA.

**Tech Stack:**
- Backend: Java 21 (LTS) + Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security + JWT, Flyway, Bean Validation)
- Frontend: Next.js 15 (App Router, React Server Components) + React 19 + TypeScript (strict) + Tailwind CSS v4 + shadcn/ui + TanStack Query
- Database: PostgreSQL 16
- Offline sync: IndexedDB via Dexie.js in the workout-execution flow
- Build: Maven (backend), pnpm (frontend)
- Deploy: Docker Compose (app + db, optional nginx reverse proxy)

See `ProjectBrief.md` for the phase-by-phase build plan (phases 0-8) and data-model proposal.

## Architecture

Two deployable services (`backend`, `frontend`) plus one stateful service (`db`), all composed by `compose.yml` at repo root. No service ever talks to the database directly except `backend`; the frontend calls only the public REST API.

### Entry Flow

1. `docker compose up -d` reads `compose.yml` and `.env` at repo root.
2. `db` (`postgres:16-alpine`) starts first; the healthcheck polls `pg_isready` until the cluster accepts connections.
3. `backend` builds from `backend/Dockerfile` (multi-stage: Maven build -> Eclipse Temurin 21 JRE). It only starts once `db` reports healthy (`depends_on: condition: service_healthy`). Boot sequence:
   - `WorkoutHubApplication.main` -> Spring Boot `SpringApplication.run`.
   - `application.yml` (profile `docker` by default from `.env`) is merged with environment variables. `spring.datasource.*`, `app.jwt.*`, and `app.cors.allowed-origins` are all env-driven.
   - Flyway applies every migration under `classpath:db/migration/V*__*.sql` in order. `spring.jpa.hibernate.ddl-auto=validate` ensures schema matches entities; migrations are the only way to change the schema.
   - Spring Security wires the JWT filter (PHASE 1) and CORS config; actuator publishes `/actuator/health` and `/actuator/info`.
   - Tomcat listens on `SERVER_PORT` (default `8080`), published to the host as `${SERVER_PORT}:8080`.
4. `frontend` builds from `frontend/Dockerfile` (multi-stage Node 22 with pnpm + Next.js `output: standalone`). It starts after `backend` (dependency-only, no healthcheck on backend yet). The server listens on port `3000`, published as `3000:3000`.
5. Browser loads `http://localhost:3000`. The Next.js 15 App Router renders Server Components first; client code uses `NEXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`) for API calls via TanStack Query.
6. Offline: inside the workout-execution flow (PHASE 4), unposted sets are persisted to IndexedDB via Dexie.js and flushed to `backend` when the network returns.

### Module Breakdown

Backend (`backend/src/main/java/com/workouthub/`) is feature-sliced. Each feature package owns its Controller, Service, Repository, DTOs, and domain types. Planned packages (materialize phase-by-phase):

| Package | Responsibility | Lands in |
|---------|----------------|----------|
| `common/` | Shared config: `SecurityConfig`, `CorsConfig`, `JacksonConfig`, global `@ControllerAdvice` error handler, `JwtService`, `Clock` provider | PHASE 1 |
| `auth/` | `/api/auth/register`, `/login`, `/refresh`; `UserDetailsService`; password hashing | PHASE 1 |
| `users/` | `/api/users/me` profile read/update; `UserProfile` aggregate | PHASE 1 |
| `exercises/` | Master catalog: `/api/exercises`, `/exercises/:id`, search; admin CRUD | PHASE 2 |
| `workouts/` | `WorkoutPlan`, `WorkoutDay`, `WorkoutDayExercise` -- weekly plan CRUD + activation | PHASE 3 |
| `sessions/` | Live sessions: `/sessions/start`, `/sessions/:id/sets`, `/finish`; per-exercise last-performance and progress queries | PHASE 4 |
| `metrics/` | Body metrics (weight, measurements), progress photos | PHASE 5 |
| `export/` | `/api/export/claude-summary` and import endpoint | PHASE 5 |

Frontend (`frontend/src/app/`) uses App Router with route groups. Planned groups:

| Route group | Purpose | Lands in |
|-------------|---------|----------|
| `(auth)/login`, `(auth)/register` | Unauthenticated entry pages | PHASE 5 |
| `(app)/dashboard` | "Today's workout" card + weekly summary + quick actions | PHASE 5 |
| `(app)/plan` | Weekly plan viewer + editor (drag-and-drop) | PHASE 5 |
| `(app)/session/[id]` | Active workout execution screen; the app's core surface | PHASE 4-5 |
| `(app)/history` | Calendar + per-session detail | PHASE 5 |
| `(app)/exercises` | Catalog with filters, search, detail modal | PHASE 5 |
| `(app)/metrics` | Body metrics + charts | PHASE 5-6 |
| `(app)/profile` | Settings, health notes, supplements | PHASE 5 |
| `(app)/export` | JSON export / import | PHASE 5 |

Cross-cutting frontend modules: `src/lib/` for API client and Zod schemas shared across features; `src/components/ui/` for shadcn-style primitives; `src/i18n/` for `next-intl` messages (default locale `tr`); no generic `utils/` dump file.

### Data Storage

- **Relational data (authoritative):** PostgreSQL 16 inside the `db` service. Named Docker volume `db_data` persists the cluster across recreates.
- **Schema source of truth:** Flyway migrations in `backend/src/main/resources/db/migration/`. Filenames follow `V<n>__<snake_case_description>.sql`. Once merged, a migration is immutable -- changes ship as a new `V<n+1>` file. `ddl-auto: validate` guarantees JPA entities cannot silently drift from the schema.
- **Testing:** Testcontainers spins up a real Postgres per test run. H2 is intentionally excluded.
- **Uploaded media** (exercise GIFs, progress photos): planned to live on a host-bind volume mounted into `backend`, path controlled by an env var added in PHASE 5. MinIO is deferred (see `DEFERRED.md` when the decision lands).
- **Client-side offline store:** IndexedDB via Dexie.js, scoped to the `(app)/session` flow. Unposted sets queue under a single object store keyed by session id; a sync routine drains it to `POST /sessions/:id/sets` when the browser regains connectivity (PHASE 4).
- **Exports:** generated on demand by `export/` endpoints. Not persisted server-side beyond the HTTP response.
- **Secrets:** `.env` at repo root (never committed). `.env.example` enumerates every variable both services read.

## Development Commands

Commands are placeholders until the PHASE 0 scaffolding lands. Update this section as part of PHASE 0 completion.

```bash
# --- Backend (Spring Boot, from ./backend) ---
# ./mvnw spring-boot:run              # run API in dev mode
# ./mvnw test                         # run unit + integration tests
# ./mvnw verify                       # full verify incl. checks
# ./mvnw package                      # build runnable jar

# --- Frontend (Next.js, from ./frontend) ---
# pnpm install                        # install deps
# pnpm dev                            # run dev server on :3000
# pnpm test                           # run unit tests (vitest)
# pnpm test:e2e                       # run Playwright e2e tests
# pnpm lint                           # ESLint
# pnpm typecheck                      # tsc --noEmit
# pnpm build                          # production build

# --- Full stack via Docker ---
# docker compose up -d                # bring up db + api + web
# docker compose logs -f              # tail logs
# docker compose down                 # stop stack
```

## Code Standards

- **Language:** All code, comments, variable names, and identifiers must be in English. User-facing strings are Turkish (the product UI is Turkish-first), but they live in i18n resource files, never hard-coded into components or Java sources.
- **No emojis:** Do not use emojis anywhere in code, comments, or responses.
- **Formatting:** Follow the conventions already established in the project.

### Java (backend)

- Java 21 language level. Use records for DTOs, sealed types where applicable, pattern matching in switches.
- Package layout: feature-sliced (`auth/`, `users/`, `exercises/`, `workouts/`, `sessions/`, `metrics/`, `export/`), not layer-sliced.
- Controller -> Service -> Repository. No business logic in controllers. Transactions declared on service methods.
- DTOs at the HTTP boundary; never return JPA entities directly.
- Validation via `jakarta.validation` on request DTOs.
- Flyway migrations are immutable once merged. New schema changes are new `V<n>__<desc>.sql` files.
- Tests: JUnit 5, Testcontainers for integration tests against real Postgres. No H2.
- Logging: SLF4J + structured JSON in production profile. No `System.out.println`.

### TypeScript (frontend)

- `"strict": true`. No `any` in committed code -- use `unknown` and narrow.
- React Server Components by default; `"use client"` only when needed (state, effects, browser APIs).
- Data fetching: Server Components + TanStack Query on the client; no ad-hoc `fetch` in components.
- Forms: `react-hook-form` + Zod schemas. Validation schemas shared with the API contract.
- Styling: Tailwind utility-first. Design tokens in `tailwind.config.ts`. No inline styles. shadcn/ui components copied into `components/ui/`.
- i18n: all user-facing text goes through the i18n layer (`next-intl` or equivalent). Default locale `tr`.

## File Organization

- Max ~200 lines per file. Split by responsibility if a module grows beyond that.
- One module = one responsibility. Do not put unrelated logic in the same file.
- New files must fit into the existing directory structure.
- Do not create "utils" or "helpers" dump files. Keep feature-specific utilities in that feature's module.

## Protected Files

The following files are protected by a pre-edit hook and require explicit user confirmation to edit:

- `PROJECT.yaml`, `CHANGELOG.md`, `RELEASE.md`
- `pom.xml` (backend Maven), `build.gradle` / `build.gradle.kts` if added later
- `package.json`, `package-lock.json`, `pnpm-lock.yaml`, `yarn.lock` (frontend)
- `Dockerfile`, `docker-compose.yml`
- Anything under `.github/workflows/`
- Anything under `scripts/`

When the PHASE 0 scaffolding lands Maven or Gradle files, extend `PROTECTED_EXACT` in `.claude/hooks/pre_guard_release_files.py` accordingly (`pom.xml`, `build.gradle`, `build.gradle.kts`, `gradlew`, `gradlew.bat`). Do not edit the hook without user confirmation.

## Git and Commits

- Use conventional commit format: `type(scope): description`
  - Types: feat, fix, refactor, style, docs, chore, test, build
  - Scopes for this project:
    - Backend: `api`, `auth`, `db`, `migrations`, `exercises`, `workouts`, `sessions`, `metrics`, `export`
    - Frontend: `ui`, `pages`, `components`, `hooks-fe`, `i18n`, `pwa`
    - Cross-cutting: `docker`, `ci`, `deps`, `config`, `docs`, `test`
  - Example: `feat(api): add rate limiting to /users endpoint`
- Keep commit messages in English, concise, imperative mood.
- One logical change per commit. Do not bundle unrelated changes.
- Reference the TODO.md task id in the commit subject when applicable: `feat(api): add rate limit (t-42)`.
- Commits are authored by the user via local git config. Do NOT add `Co-Authored-By: Claude` trailers to commit messages.

## Branch Strategy

This is the primary repo. Use feature branches for non-trivial work (`feature/<name>` or `fix/<name>`); small, safe fixes can go on `main` if the project convention allows. No direct commits to `main` for feature work. One PR per feature.

## What NOT to Do

- Do not add `console.log` / `print()` for debugging. Use proper logging.
- Do not add TODO comments. Track work in TODO.md, DEFERRED.md, or the issue tracker.
- Do not write defensive code against impossible states.
- Do not add polyfills unless the minimum supported version requires them.
- Do not add external dependencies without discussing first.

## Deferred Work

Work that is intentionally postponed goes in `DEFERRED.md` at the repo root.
Each entry must have: what, why deferred, concrete trigger that unblocks it, owner.
See `.claude/TIPS.md` for the format. Do not leave TODO comments in code instead.

## Release

- **Identity**: `PROJECT.yaml` at repo root is the single source of truth for `name`, `display_name`, `version`, `icon`, license, and release config. Every derived manifest (`package.json`, `pyproject.toml`, etc.) follows it.
- **Changelog**: `CHANGELOG.md` uses the Keep a Changelog format. The release workflow extracts notes from the matching `## [x.y.z]` section.
- **Runbook**: See `RELEASE.md` for the end-to-end release procedure (preflight, cut, post-release, rollback).
- **Automation**: `.github/workflows/release.yml` triggers on tag push `v*.*.*`. Release is test-gated, matrix-built per platform, checksum-signed, and draft-first — a maintainer publishes manually.
- **Version bumps**: Use `/release <version>` to do the mechanical steps (bump `PROJECT.yaml`, rotate `CHANGELOG.md`, sync derived manifests, commit, tag). Push is always manual.
- **Identity drift**: If `PROJECT.yaml` disagrees with a derived manifest, `PROJECT.yaml` wins. `/doctor` reports drift; fix it by updating the derived file, never the other direction.
