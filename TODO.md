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

- [t-54] [p7] Notification triggers remainder (webpush-java wiring + supplement reminder)
  - Acceptance: webpush-java (or equivalent) added to pom.xml with user approval; WebPushNotificationDispatcher replaces LoggingNotificationDispatcher and signs payloads with VAPID; supplement reminder trigger fires at each supplement's user-defined reminder_time (after t-52 lands the entity); integration tests cover signed-send success and expired-subscription pruning

- [t-55] [p8] Wire ESLint and add pnpm lint to CI (closes the lint gap from t-49)
  - Acceptance: eslint + eslint-config-next installed under frontend/ with a working flat config; pnpm lint runs non-interactively and passes; .github/workflows/ci.yml runs pnpm lint in the frontend job; setup-ci's remaining "lint" acceptance criterion is covered

- [t-56] [p5] Full-JSON import: plans + sessions path
  - Acceptance: POST /api/export/import accepts plans and sessions slices of a full dump and idempotently replaces them (delete user-owned rows first, then bulk insert preserving UUIDs); FullImportService lifts the current HTTP 422 on plans/sessions; integration test covers round-trip replace for plans+days+day_exercises and sessions+sets; existing schema-version rejection remains

## Blocked

(none)

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
- [t-37] 2026-04-23 -- Backend analytics endpoints (volume / one-rm / streak / prs / heatmap)
- [t-38] 2026-04-23 -- (app)/insights page with weekly volume bar + 1RM line chart
- [t-39] 2026-04-23 -- PR detection on set save + /prs list page
- [t-40] 2026-04-23 -- StreakCalculator + heatmap component + streak chips
- [t-41] 2026-04-23 -- Push subscription module (V10 migration, /api/push/subscribe, VAPID env)
- [t-42] 2026-04-23 -- Reminder scheduler + logging dispatcher (workout + weight triggers; webpush sender + supplement trigger carved into t-54)
- [t-43] 2026-04-23 -- Push permission UX on dashboard + rest-timer OS notification in session flow
- [t-44] 2026-04-23 -- JaCoCo plugin + mvn verify 70% line-coverage gate (DTO records + Application excluded)
- [t-45] 2026-04-23 -- Vitest v8 coverage reporter (lcov) + 70% line gate; current run at 84.5%
- [t-46] 2026-04-23 -- Playwright scaffold + login/start/log-sets/finish/export critical-flow spec
- [t-47] 2026-04-23 -- compose.prod.yml + nginx (TLS, HSTS, auth rate limit) + DEPLOYMENT.md runbook
- [t-48] 2026-04-23 -- scripts/backup.sh (pg_dump + 30d retention) + docs/BACKUP.md restore playbook
- [t-49] 2026-04-23 -- CI workflow live: backend mvn verify + frontend typecheck/test; lint step carved into t-55
- [t-50] 2026-04-23 -- Release workflow live: GHCR images on v*.*.* tag + compose bundle in draft GitHub Release
- [setup-ci] 2026-04-23 -- Superseded by t-49 (CI active on main; pnpm lint follow-up carved into t-55)
- [setup-release] 2026-04-23 -- Superseded by t-50 (GHCR+compose-bundle path recorded in RELEASE.md)
- [t-52] 2026-04-23 -- Supplements CRUD (backend + /profile section) with bilingual timing options
- [t-53] 2026-04-23 -- Full-JSON export (all slices) + import for profile/metrics/supplements (plans+sessions carved into t-56)
