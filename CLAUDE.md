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

See open TODO tasks (`setup-1`..`setup-3`) -- filled once PHASE 0 lands actual code.

### Entry Flow

TODO: documented in `setup-1`.

### Module Breakdown

TODO: documented in `setup-2`.

### Data Storage

TODO: documented in `setup-3`.

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
