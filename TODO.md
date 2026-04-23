# TODO

Persistent task tracking for this project. Managed via `/task`.

Format:
```
- [t-N] Short imperative title
  - Acceptance: one-line objective criterion
  - Notes: optional context (only if useful)
```

Ids are monotonic (`t-1`, `t-2`, ...). Never reuse. Never renumber.

## Active

- [t-37] [p6] Backend analytics endpoints
  - Acceptance: /api/analytics/volume (weekly totals), /analytics/one-rm/:exerciseId (Epley), /analytics/streak, /analytics/prs, /analytics/heatmap return Zod-validated payloads; integration tests cover empty-data and populated cases

- [t-38] [p6] (app)/insights page with volume + 1RM charts
  - Acceptance: Recharts-based weekly volume bar and per-exercise 1RM line chart; reads from analytics endpoints; RTL test covers render with mocked payload

- [t-39] [p6] PR detection + PR list component
  - Acceptance: Backend detects PR (max weight x reps volume) on set save and emits a marker; frontend PR list page shows per-exercise best set; backend unit test covers detection; RTL covers list render

- [t-40] [p6] Streak calculator + heatmap component
  - Acceptance: Heatmap shows last 12 weeks with per-day session count; current streak + longest streak chips; unit tests cover streak math (edge cases: today, yesterday, broken)

- [t-41] [p7] Push subscription backend
  - Acceptance: VAPID keys read from env; POST /api/push/subscribe stores endpoint + keys; Flyway migration for push_subscriptions table; integration test covers subscribe and duplicate-idempotent insert

- [t-42] [p7] Notification triggers
  - Acceptance: Scheduled job fires workout-day reminder at configurable hour; weight-missing nudge after 7 days without body_metrics entry; supplement reminder per user-defined time; integration tests use a fake clock

- [t-43] [p7] Frontend push UX + rest-timer OS notification
  - Acceptance: Notification permission flow on dashboard; rest timer posts OS notification via service worker when it elapses; RTL test covers permission prompt state rendering

- [t-44] [p8] Backend coverage + JaCoCo gate
  - Acceptance: JaCoCo added to pom.xml; mvn verify enforces line coverage >= 70%; any gap below threshold is closed or explicitly waived with a documented reason

- [t-45] [p8] Frontend unit test harness + coverage
  - Acceptance: Vitest + React Testing Library configured; coverage reporter emits lcov; overall line coverage >= 70%; pnpm test runs clean

- [t-46] [p8] Playwright E2E critical flow
  - Acceptance: playwright.config.ts runs against docker compose up stack; flow: register -> login -> start session -> log 3 sets -> finish -> export JSON; pnpm test:e2e passes

- [t-47] [p8] Production compose.prod.yml with nginx + TLS
  - Acceptance: compose.prod.yml brings up nginx reverse proxy terminating TLS (via certbot volume), rate limits /api/auth/*, sets HSTS, and proxies to backend/frontend; tested locally against self-signed cert; DEPLOYMENT.md documents the cert-renewal cron

- [t-48] [p8] Backup script + restore playbook
  - Acceptance: scripts/backup.sh pg_dumps to ./backups/YYYY-MM-DD.sql.gz with retention of 30 days; docs/DEPLOYMENT.md has a verified restore recipe; dry-run of restore on a fresh container succeeds

- [t-49] [p8] Unblock and activate CI (closes setup-ci)
  - Acceptance: .github/workflows/ci.yml runs `mvn verify` (backend) and `pnpm lint && pnpm typecheck && pnpm test` (frontend) on push and PR; job is green on main; TODO#setup-ci moves to Done

- [t-50] [p8] Unblock and activate Release (closes setup-release)
  - Acceptance: Decision recorded in RELEASE.md (self-hosted docker images via ghcr on tag push, OR docker-compose bundle as a GitHub Release asset); chosen path implemented in .github/workflows/release.yml and dry-run with a pre-release tag; TODO#setup-release moves to Done

- [t-52] [p5] Supplements CRUD (backend + (app)/profile list)
  - Acceptance: Supplement entity + repository + Flyway migration if not already present; GET/POST/PUT/DELETE /api/supplements scoped per user; profile page lists, adds, edits, and deletes supplements; RTL covers add + delete flow; backend integration test covers CRUD + cross-user isolation

- [t-53] [p5] Full-JSON export + restore import
  - Acceptance: GET /api/export/full returns a single JSON dump (user profile, plans, sessions, sets, body_metrics, supplements); POST /api/export/import accepts that dump and idempotently re-inserts it; (app)/export UI adds "Download full JSON" button next to claude-summary and a file-upload restore control; RTL covers download + import success + import failure; backend integration tests cover round-trip and malformed-input rejection

## Blocked

- [setup-ci] Revisit CI scaffolding decision
  - Acceptance: `.github/workflows/ci.yml` exists and runs lint + test for backend and frontend on push/PR; `ci.yml.template` still kept for reference
  - Blocked: deferred at setup per user direction (2026-04-22). Revisit after backend has a Maven wrapper or a stable docker-based CI recipe so the job does not start red.

- [setup-release] Revisit release scaffolding decision
  - Acceptance: decision recorded in CHANGELOG or RELEASE.md whether this self-hosted app uses tag-triggered GitHub Releases or image-based docker tags; scaffolding either activated or explicitly marked not-applicable
  - Blocked: deferred at setup per user direction (2026-04-22). Revisit around PHASE 8 when the deployment model (self-hosted docker pull vs github release) is concrete.

## Done

- [t-1] 2026-04-22 -- Scaffold PHASE 0 baseline (backend, frontend, docker-compose, env)
- [setup-1] 2026-04-22 -- Document Architecture / Entry Flow in CLAUDE.md
- [setup-2] 2026-04-22 -- Document Module Breakdown in CLAUDE.md
- [setup-3] 2026-04-22 -- Document Data Storage in CLAUDE.md
- [setup-icon] 2026-04-22 -- Moved to DEFERRED.md (needs design input and graphics tooling)
- [setup-protected-maven] 2026-04-22 -- Extend PROTECTED_EXACT now that Maven files exist
- [t-7] 2026-04-23 -- Testcontainers harness + replace trivial smoke test
- [t-2] 2026-04-23 -- Flyway V2: users + user_profile tables
- [t-3] 2026-04-23 -- Flyway V3: exercises master catalog table
- [t-4] 2026-04-23 -- Flyway V4: workout_plans + workout_days + workout_day_exercises
- [t-5] 2026-04-23 -- Flyway V5: workout_sessions + session_sets
- [t-6] 2026-04-23 -- Flyway V6: body_metrics + supplements
- [t-8] 2026-04-23 -- Common config: SecurityConfig, CorsConfig, JacksonConfig, GlobalExceptionHandler
- [t-9] 2026-04-23 -- JwtService + JwtAuthenticationFilter
- [t-10] 2026-04-23 -- auth module: /api/auth/register, /login, /refresh
- [t-11] 2026-04-23 -- users module: GET/PUT /api/users/me
- [t-51] 2026-04-23 -- Lock down auth: env-seeded admin + admin user management (no public register)
- [t-12] 2026-04-23 -- Exercise JPA entity, repository, DTO, mapper
- [t-13] 2026-04-23 -- Public exercise endpoints
- [t-14] 2026-04-23 -- Admin exercise CRUD
- [t-15] 2026-04-23 -- Bilingual schema (V8) + 50-exercise seed (V9) with at-home coverage
- [t-16] 2026-04-23 -- WorkoutPlan / Day / DayExercise entities + repository
- [t-17] 2026-04-23 -- Plan CRUD endpoints with ownership guard
- [t-18] 2026-04-23 -- Plan activation semantics
- [t-19] 2026-04-23 -- Day + exercise nested CRUD with reorder
- [t-20] 2026-04-23 -- Default "Baslangic Plani" seeded on admin-driven user creation
- [t-21] 2026-04-23 -- WorkoutSession + SessionSet entities + repositories
- [t-22] 2026-04-23 -- Session lifecycle endpoints (start / active / finish)
- [t-23] 2026-04-23 -- Set capture endpoints with finished-session gate
- [t-24] 2026-04-23 -- Session history list + detail endpoints
- [t-25] 2026-04-23 -- Per-exercise last-performance + progress analytics
- [t-27] 2026-04-23 -- Frontend foundations (TanStack Query + Zod client + i18n + shadcn + vitest)
- [t-28] 2026-04-23 -- Login page (react-hook-form + zod + mutation) -- /register removed per t-51
- [t-29] 2026-04-23 -- Dashboard (Resume / Start today / Rest day) + GET /api/workout-plans/active
- [t-31] 2026-04-23 -- Session execution screen (planned cards + addSet + finish) + GET /api/workout-days/{id}
  Notes: rest-timer UI and exercise-detail modal deferred -- MVP ships without them; revisit in a follow-up if Fatih asks
- [t-33] 2026-04-23 -- Exercises catalog grid + filters + debounced search + detail page (bilingual)
  Notes: PR chip deferred to t-39 which owns PR detection end-to-end
- [t-32] 2026-04-23 -- History calendar (Monday-first grid) + session-detail drawer
- [t-26] 2026-04-23 -- Dexie offline queue for session sets (enqueue, drain, subscribeOnline)
- [t-30] 2026-04-23 -- Plan viewer + accessible Move up/down reorder (dnd-kit visual deferred)
- [t-34] 2026-04-23 -- Metrics: body_metrics CRUD + weight chart (photo upload deferred)
- [t-35] 2026-04-23 -- (app)/profile page editing /users/me (supplements + full-export/import carved into t-52 and t-53)
- [t-36] 2026-04-23 -- PWA manifest + service worker + offline fallback (placeholder icons pending setup-icon)
