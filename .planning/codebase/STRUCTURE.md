# Codebase Structure

**Analysis Date:** 2026-05-02

## Directory Layout

```
WorkoutHub/
├── backend/                              Spring Boot 3 + Java 21 application
│   ├── src/main/java/com/workouthub/    Feature-sliced packages
│   ├── src/main/resources/
│   │   ├── db/migration/                Flyway V1..V25 SQL files
│   │   ├── application.yml              Default Spring config (env-driven)
│   │   └── application-*.yml            Profile overrides (e.g. docker, prod)
│   ├── src/test/java/com/workouthub/    JUnit 5 + Testcontainers tests
│   ├── pom.xml                          Maven manifest (protected)
│   └── Dockerfile                       Multi-stage build to Temurin 21 JRE
├── frontend/                             Next.js 15 + React 19 + TypeScript
│   ├── src/
│   │   ├── app/                         App Router (routes + layouts)
│   │   ├── components/                  Reusable React components
│   │   │   └── ui/                      shadcn/ui primitives
│   │   ├── lib/                         API client, auth, offline, push, time, utils
│   │   ├── i18n/                        next-intl config
│   │   ├── messages/{tr,en}.json        i18n translations
│   │   └── test/                        Vitest setup
│   ├── package.json                     pnpm manifest (protected)
│   ├── next.config.ts                   Next config (standalone output)
│   ├── tsconfig.json                    TypeScript strict
│   └── Dockerfile                       Multi-stage build to Node standalone
├── docs/                                 Project docs
│   ├── SELF_HOSTED_CONTRACT.md          App-portable operational contract (binding)
│   ├── API.md                           REST endpoint reference
│   ├── DATA_SCHEMA.md                   Entity model
│   ├── EXPORT_FORMAT.md                 JSON dump format for AI coach
│   ├── DEPLOYMENT.md                    Deployment notes
│   ├── BACKUP.md                        Backup procedure
│   ├── OBSERVABILITY.md                 Logging / metrics
│   └── examples/full-export-example.json
├── .planning/                            GSD planning artifacts
│   ├── HANDOFF.md                       Pre-GSD session handoff
│   ├── ISSUES.md                        Known deferred test failures
│   └── codebase/                        This directory (codebase map)
├── .claude/                              Claude Code template state (template-managed)
├── .github/                              CI workflows + issue templates
├── assets/icon.svg                      Project icon (referenced by IDENTITY.yaml)
├── compose.yml                           Production compose stack
├── CLAUDE.md                             Claude Code instructions
├── IDENTITY.yaml                         Single source of truth for name/version/icon
├── CHANGELOG.md                          Keep-a-changelog
├── ProjectBrief.md                       Phase-by-phase build plan (phases 0-8)
└── README.md
```

## Backend Feature Packages

Each package owns Controller, Service, Repository, DTO records, and domain entities.

| Package (`backend/src/main/java/com/workouthub/`) | Responsibility |
|---|---|
| `WorkoutHubApplication.java` | Spring Boot entry point with `@EnableScheduling` |
| `common/config/` | `SecurityConfig`, `CorsConfig`, `JacksonConfig`, `WebConversionConfig`, Clock provider |
| `common/security/` | `JwtService`, `JwtAuthenticationFilter`, `TraceIdFilter`, `AppUserPrincipal` |
| `common/web/` | `GlobalExceptionHandler`, `ApiError`, `NotFoundException`, `ConflictException` |
| `auth/` | Login, refresh, password reset, brute-force guard, optional OIDC controller |
| `users/` | Profile read/update, password change, webhook tokens |
| `admin/` | Admin user CRUD, admin exercise CRUD |
| `exercises/` | Master catalog (search, filters, detail) |
| `workouts/` | Weekly plan, days, day-exercise items, default plan seeder |
| `sessions/` | Live workout sessions, set logging, last-performance, PR detection |
| `metrics/` | Body weight, measurements, photos |
| `nutrition/` | Meal logging, macro tracking, nutrition goals |
| `supplements/` | Supplement tracking + reminders |
| `water/` | Daily water intake |
| `achievements/` | Streaks, badges, streak freeze |
| `challenges/` | Monthly challenges + admin CRUD |
| `analytics/` | Volume, 1RM trend, streak calculator |
| `notifications/` | In-app and Web Push delivery |
| `push/` | Push subscription endpoints |
| `webhooks/` | Smart-scale ingestion |
| `health/` | Apple Health / Google Fit / FIT file import |
| `exports/` | Full export, claude-summary, CSV / ICS / JSON, full import |
| `audit/` | Audit log queries |
| `twofa/` | TOTP setup and verification |

## Frontend Route Groups

`frontend/src/app/`:

| Group / Route | Purpose |
|---|---|
| `layout.tsx` | Root layout, providers, PWA registration |
| `page.tsx` | Auth-aware landing redirect |
| `(auth)/login`, `(auth)/register` | Unauthenticated entry |
| `(auth)/[callback]` | OIDC callback (when `APP_AUTH_OIDC_ENABLED=true`) |
| `(app)/dashboard` | Today's workout card + weekly summary |
| `(app)/plan` | Weekly plan viewer + editor |
| `(app)/session/[id]` | Live workout execution screen (core surface) |
| `(app)/history` | Calendar + per-session detail |
| `(app)/exercises`, `(app)/exercises/[id]` | Catalog and detail |
| `(app)/metrics` | Body metrics + charts |
| `(app)/nutrition` | Meal logging |
| `(app)/profile` | Settings, password, 2FA |
| `(app)/insights` | Volume, frequency, trends |
| `(app)/achievements`, `(app)/prs` | Streaks and PRs |
| `(app)/export` | JSON export / import / claude summary |
| `offline` | Offline fallback shell |

## Cross-Cutting Frontend Modules

`frontend/src/`:
- `lib/api/client.ts` - singleton API client; JWT refresh interceptor; Zod validation at boundary
- `lib/api/schemas.ts` - shared Zod schemas mirroring backend DTOs
- `lib/api/endpoints.ts` - typed endpoint helpers (currently 591 lines, candidate for domain split per CONCERNS)
- `lib/auth/token-store.ts` - access + refresh token persistence
- `lib/offline/session-set-queue.ts` - Dexie store + drain helpers
- `lib/push/` - Web Push subscription helpers
- `lib/time/` - date and time utilities
- `lib/utils.ts` - misc helpers (kept small, not a dump file)
- `components/ui/` - shadcn/ui primitives (button, input, card, modal, ...)
- `components/providers.tsx` - TanStack Query, i18n, theme providers
- `components/service-worker-registrar.tsx` - PWA registration

## Database Migrations

`backend/src/main/resources/db/migration/` (V1..V25, immutable once merged):

| Version | Subject |
|---|---|
| V1 | Init (users, roles) |
| V2 | Users + profile |
| V3 | Exercises catalog |
| V4 | Workout plans / days / day exercises |
| V5 | Sessions and sets |
| V6 | Body metrics + supplements |
| V7 | Refresh tokens |
| V8 | Exercise bilingual (tr/en) |
| V9 | Seed exercises |
| V10 | Push subscriptions |
| V11 | Supplement reminder time |
| V12 | User TOTP |
| V13 | Refresh token session metadata |
| V14 | Password reset tokens |
| V15 | Login attempts |
| V16 | Audit log |
| V17 | Nutrition |
| V18 | User profile nutrition goals |
| V19 | Water entries |
| V20 | Sessions heart rate |
| V21 | Webhook tokens |
| V22 | Achievements |
| V23 | Monthly challenges |
| V24 | Streak freeze |
| V25 | Theme preference |

## Compose Topology

`compose.yml` - 3 services:
- `db` - postgres:16-alpine, healthcheck `pg_isready`, named volume `db_data` (CONCERNS: should be bind mount)
- `backend` - built from `backend/Dockerfile`, depends on `db: service_healthy`, port `${SERVER_PORT:-8080}:8080`
- `frontend` - built from `frontend/Dockerfile`, depends on `backend` (no health condition), port `3000:3000` (CONCERNS: hard-coded)

## Naming Conventions

**Java:**
- Files: `PascalCase.java` matching the public type
- Test files: `*Test.java` (unit) or `*IntegrationTest.java`
- Migrations: `V<n>__<snake_case_description>.sql`
- DTO records under `<feature>/dto/`
- Domain entities and repositories under `<feature>/domain/`

**TypeScript:**
- Files: kebab-case for utilities (`token-store.ts`), kebab-case for components (`bottom-nav.tsx`)
- Routes: Next App Router convention (`page.tsx`, `layout.tsx`, `loading.tsx`, `error.tsx`)
- Test files: `*.test.ts` / `*.test.tsx` co-located with source

## Where to Add New Code

**New backend feature:**
- Create `backend/src/main/java/com/workouthub/<feature>/` with `dto/`, `domain/`, `<Feature>Controller.java`, `<Feature>Service.java`
- Add Flyway migration as next `V<n+1>__<desc>.sql`
- Add `<Feature>IntegrationTest` extending `AbstractIntegrationTest`

**New frontend page:**
- Create `frontend/src/app/(app)/<feature>/page.tsx` (Server Component default)
- Add Zod schemas to `frontend/src/lib/api/schemas.ts`
- Add endpoint helpers to `frontend/src/lib/api/endpoints.ts`
- Add i18n keys to `frontend/messages/tr.json` and `en.json`
- Add Vitest test next to component

**New shared util:**
- Backend: extract into `common/<area>/` only when truly cross-cutting; otherwise keep within feature
- Frontend: `lib/<area>/` (not `lib/utils.ts` dump)

## Special Directories

- `.planning/` - GSD artifacts; tracked in git
- `.claude/` - Claude Code template state; partially tracked (manifest, scripts)
- `backend/target/` - Maven build output; gitignored at sub-directory level
- `frontend/.next/`, `frontend/node_modules/` - generated; gitignored
- `data/` (planned) - bind-mount target for production, will be gitignored

---

*Structure analysis: 2026-05-02*
*Update when directory structure changes*
