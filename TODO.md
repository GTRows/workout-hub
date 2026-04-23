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

- [t-2] [p1] Flyway V2: users + user_profile tables
  - Acceptance: Migration applies cleanly from empty Postgres; Testcontainers test asserts both tables and expected columns/indexes exist

- [t-3] [p1] Flyway V3: exercises master catalog table
  - Acceptance: Migration applies; test asserts columns, uniqueness of (name_en) and required indexes

- [t-4] [p1] Flyway V4: workout_plans + workout_days + workout_day_exercises
  - Acceptance: Migration applies; test asserts FK cascades on plan delete and unique (plan_id, day_of_week)

- [t-5] [p1] Flyway V5: workout_sessions + session_sets
  - Acceptance: Migration applies; test asserts FK (session_id -> workout_sessions) and set_number uniqueness per (session_id, exercise_id)

- [t-6] [p1] Flyway V6: body_metrics + supplements
  - Acceptance: Migration applies; test asserts both tables; date index on body_metrics.recorded_date

- [t-7] [p1] Testcontainers harness + replace trivial smoke test
  - Acceptance: AbstractIntegrationTest base class with a shared PostgreSQLContainer; WorkoutHubApplicationTests now boots full Spring context against it and passes

- [t-8] [p1] Common config: SecurityConfig, CorsConfig, JacksonConfig, GlobalExceptionHandler
  - Acceptance: Context loads; 401 on protected endpoint; CORS allows configured origin; error responses follow the documented error DTO; tests cover each

- [t-9] [p1] JwtService + JwtAuthenticationFilter
  - Acceptance: Service signs and validates HS512 tokens using APP_JWT_SECRET; filter populates SecurityContext for valid token and rejects tampered/expired tokens; unit tests cover both paths

- [t-10] [p1] auth module: /api/auth/register, /login, /refresh
  - Acceptance: Register creates user + profile with BCrypt hash; login returns access+refresh pair; refresh rotates refresh token and invalidates the old one; integration tests cover happy path and bad-credentials

- [t-11] [p1] users module: GET/PUT /api/users/me
  - Acceptance: Authenticated user reads and updates own profile (height, weight, birth, gender, health_notes, goals); returns 401 without token; integration tests cover both

- [t-12] [p2] Exercise JPA entity, repository, DTO, mapper
  - Acceptance: Entity matches V3 schema; repository has findByCategory/findByEquipment/searchByName; unit tests pass

- [t-13] [p2] Public exercise endpoints
  - Acceptance: GET /api/exercises (filter: category, equipment, difficulty, pagination), GET /api/exercises/:id, GET /api/exercises/search?q= all work anonymously; integration tests cover filters, 404, and search

- [t-14] [p2] Admin exercise CRUD
  - Acceptance: POST/PUT /api/admin/exercises require ADMIN role; validation rejects missing Turkish or English name; 403 for USER role; integration tests cover role gate and validation

- [t-15] [p2] Seed 30+ exercises as Flyway V7 data migration
  - Acceptance: Turkish + English names, how-to steps, form tips, common mistakes, muscle groups, difficulty filled for every exercise listed in ProjectBrief PHASE 2; count test asserts >= 30 rows after migration

- [t-16] [p3] WorkoutPlan / WorkoutDay / WorkoutDayExercise entities + repositories
  - Acceptance: Entities with cascade=ALL, orphanRemoval=true on aggregates; unit tests on repository round-trip

- [t-17] [p3] Plan CRUD endpoints
  - Acceptance: GET /api/workout-plans, POST, GET :id (with nested days+exercises), PUT :id, DELETE :id; users only see their own plans; integration tests cover isolation and not-found

- [t-18] [p3] Plan activation semantics
  - Acceptance: POST /api/workout-plans/:id/activate sets is_active=true on this plan and false on all other plans for the user; atomic in one transaction; integration test asserts exactly one active plan

- [t-19] [p3] Day + exercise nested CRUD with reorder
  - Acceptance: POST /api/workout-plans/:id/days, PUT/DELETE .../exercises/:exId, and reorder endpoint update order_index atomically with no gaps; integration tests cover reorder and cascade

- [t-20] [p3] Default "Baslangic Plani" seeded on registration
  - Acceptance: A new user registered via /auth/register has exactly one active plan matching the template defined in a seed file; integration test asserts the plan shape

- [t-21] [p4] WorkoutSession + SessionSet entities + repositories
  - Acceptance: Entities mirror V5 schema; repositories with findActiveByUser, findBySessionAndExercise; unit tests pass

- [t-22] [p4] Session lifecycle endpoints
  - Acceptance: POST /api/sessions/start creates a session tied to a workout_day_id; GET /api/sessions/active returns at most one; POST /api/sessions/:id/finish stamps ended_at and locks edits; integration tests cover each transition

- [t-23] [p4] Set capture endpoints
  - Acceptance: POST /api/sessions/:id/sets creates a set with reps, weight_kg, rpe, completed; PUT updates; only within a non-finished session; 409 if session is finished; integration tests cover validation and state gate

- [t-24] [p4] Session history endpoints
  - Acceptance: GET /api/sessions/history returns paginated list scoped to the user; GET /api/sessions/:id returns full detail; integration tests cover pagination and ownership

- [t-25] [p4] Per-exercise analytics queries
  - Acceptance: GET /api/exercises/:id/last-performance returns last session's sets; GET /api/exercises/:id/progress returns time series (weight, reps, volume) for last N sessions; integration tests cover empty-history and populated cases

- [t-26] [p4] Frontend Dexie offline queue for session sets
  - Acceptance: src/lib/offline/ stores unposted sets keyed by session id; flush-on-online drains them to POST /api/sessions/:id/sets with idempotency key; vitest unit tests cover enqueue, flush, and retry

- [t-27] [p5] Frontend foundations
  - Acceptance: API client (fetch wrapper + TanStack Query provider + Zod-validated response types), auth token storage with refresh interceptor, next-intl configured with tr messages + en fallback, shadcn/ui initialised, authenticated layout shell with nav and protected-route guard; vitest covers the refresh interceptor

- [t-28] [p5] (auth)/login and (auth)/register pages
  - Acceptance: react-hook-form + zod forms hitting /api/auth/*; correct error surfacing for 400/401; redirect to /dashboard on success; RTL test covers validation errors and successful submit

- [t-29] [p5] (app)/dashboard
  - Acceptance: "Today's workout" card (from active plan + current day), weekly summary (done vs planned), last-weight chip, quick actions; RTL test covers rendering with mocked data

- [t-30] [p5] (app)/plan viewer + editor
  - Acceptance: Weekly Pzt-Pzr view; tap a day to see exercises; editor supports drag-and-drop reorder of days and exercises via dnd-kit; RTL test covers the reorder mutation firing PUT

- [t-31] [p5] (app)/session/[id] execution screen
  - Acceptance: Exercise cards with per-set checkbox + weight/reps inputs, previous-performance chip, rest timer (auto-start on check), exercise-detail modal pulling from catalog; RTL test covers set completion offline (Dexie queue) and online flush

- [t-32] [p5] (app)/history
  - Acceptance: Calendar view with completed days highlighted; tap day to open session-detail drawer; RTL test covers month navigation and empty-day vs populated-day render

- [t-33] [p5] (app)/exercises catalog
  - Acceptance: Grid with filters (category, equipment, difficulty), search input (debounced), detail modal with how-to/form tips/common mistakes and personal-record chip; RTL test covers filter + search

- [t-34] [p5] (app)/metrics
  - Acceptance: Weight chart (weekly/monthly/all), entry form posting body_metrics, progress photo upload accepting JPEG/PNG <= 5MB; RTL test covers chart rendering from mocked data and form submit

- [t-35] [p5] (app)/profile + (app)/export
  - Acceptance: Profile edits /users/me; supplements managed in a list; /export offers "download full JSON" and "download claude-summary JSON" via /api/export/*; restore upload parses and re-inserts; RTL tests cover export download and import success/failure

- [t-36] [p5] PWA manifest + service worker
  - Acceptance: manifest.webmanifest with name/short_name/icons/theme/display=standalone; service worker registered; offline fallback page for the dashboard route; Lighthouse PWA check >= 90

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
