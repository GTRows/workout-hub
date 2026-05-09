# Data Schema

WorkoutHub stores all relational state in PostgreSQL 16. The authoritative
schema is the Flyway migration set under
`backend/src/main/resources/db/migration/`; this document is a navigational
summary, not a copy of the SQL. The application boots with
`spring.jpa.hibernate.ddl-auto=validate` (see `application.yml`), so the
migrations are the contract and the JPA entities are validated consumers.
Tests run against a real Postgres via Testcontainers; H2 is intentionally
excluded.

Migrations are immutable once merged. A new schema change ships as a new
`V<n+1>__<snake_case_description>.sql` file; existing V-files are never
edited in place.

## Migration history

| Version | Subject |
|---------|---------|
| `V1__init.sql` | Empty baseline so Flyway has a row to anchor on. |
| `V2__users_and_profile.sql` | `users`, `user_profile`. |
| `V3__exercises.sql` | Master `exercises` catalog (admin-curated). |
| `V4__workout_plans_days_exercises.sql` | `workout_plans`, `workout_days`, `workout_day_exercises`. |
| `V5__sessions_and_sets.sql` | `workout_sessions`, `session_sets`. |
| `V6__body_metrics_and_supplements.sql` | `body_metrics`, `supplements`. |
| `V7__refresh_tokens.sql` | `refresh_tokens` (SHA-256 hashed). |
| `V8__exercise_bilingual.sql` | Adds `_en` columns + renames existing copy to `_tr` on `exercises`. |
| `V9__seed_exercises.sql` | Seeds the public exercise catalog. |
| `V10__push_subscriptions.sql` | `push_subscriptions` (Web Push endpoints). |
| `V11__supplement_reminder_time.sql` | `supplements.reminder_time`. |
| `V12__user_totp.sql` | `user_totp` (per-user TOTP secret + recovery codes). |
| `V13__refresh_token_session_metadata.sql` | `refresh_tokens.user_agent`, `last_used_at`. |
| `V14__password_reset_tokens.sql` | `password_reset_tokens`. |
| `V15__login_attempts.sql` | `login_attempts` (brute-force window). |
| `V16__audit_log.sql` | `audit_log` (admin-surface mutations). |
| `V17__nutrition.sql` | `food_items` (catalog), `nutrition_entries`. |
| `V18__user_profile_nutrition_goals.sql` | Daily kcal/macro goals on `user_profile`. |
| `V19__water_entries.sql` | `water_entries`. |
| `V20__sessions_heart_rate.sql` | `workout_sessions.heart_rate_avg_bpm`. |
| `V21__webhook_tokens.sql` | `webhook_tokens` (per-user inbound HMAC tokens). |
| `V22__achievements.sql` | `achievements` catalog + `user_achievements`. |
| `V23__monthly_challenges.sql` | `monthly_challenges` (one per `year_month`). |
| `V24__streak_freeze.sql` | `user_profile.streak_freeze_used_month`. |
| `V25__theme_preference.sql` | `user_profile.theme_preference`. |
| `V26__session_sets_client_id.sql` | `session_sets.client_set_id` (offline idempotency key). |
| `V27__session_sets_is_pr.sql` | `session_sets.is_pr` + Epley-1RM backfill. |
| `V28__rest_timer_schedules.sql` | `rest_timer_schedules` (server-side rest-over push). |

## Domain map

Tables grouped by feature package (per `.planning/codebase/STRUCTURE.md`):

- **auth / users:** `users`, `user_profile`, `refresh_tokens`,
  `password_reset_tokens`, `login_attempts`, `user_totp`.
- **exercises:** `exercises` (bilingual `_tr`/`_en` columns from V8, seeded
  by V9).
- **workouts:** `workout_plans`, `workout_days`, `workout_day_exercises`.
- **sessions:** `workout_sessions` (heart-rate column from V20),
  `session_sets` (`client_set_id` from V26, `is_pr` from V27),
  `rest_timer_schedules` (V28).
- **metrics:** `body_metrics`, `supplements` (reminder time from V11).
- **nutrition:** `food_items`, `nutrition_entries` (V17), daily goals on
  `user_profile` (V18), `water_entries` (V19).
- **achievements:** `achievements`, `user_achievements` (V22),
  `monthly_challenges` (V23), streak-freeze month on `user_profile` (V24).
- **notifications / push:** `push_subscriptions` (V10),
  `rest_timer_schedules` (V28).
- **webhooks:** `webhook_tokens` (V21).
- **audit:** `audit_log` (V16).

The `user_profile` row carries cross-feature scalars (nutrition goals,
streak freeze, theme preference) so the `/api/users/me` projection stays a
single read.

## Foreign-key relationships

Almost every per-user table anchors on `user_id UUID REFERENCES users(id) ON
DELETE CASCADE`. Deleting a user therefore deletes every owned row across
sessions, sets, metrics, supplements, refresh tokens, push subscriptions,
nutrition entries, water entries, achievements, webhook tokens, audit
entries, and password-reset tokens.

Two non-trivial parent-child trees:

- `workout_plans` -> `workout_days` -> `workout_day_exercises` (V4). Cascade
  deletes flow downward: dropping a plan drops every day and exercise slot
  under it. `workout_day_exercises.exercise_id` references
  `exercises(id) ON DELETE RESTRICT` so the catalog cannot be torn out from
  under live plans.
- `workout_sessions` -> `session_sets` (V5). Same cascade posture; finishing
  a session does not delete it (`ended_at IS NOT NULL` is a state, not a
  lifecycle end). `session_sets.exercise_id` is `ON DELETE RESTRICT` for the
  same reason.

`workout_sessions.workout_day_id` is `ON DELETE SET NULL` so deleting a plan
day does not destroy the recorded session that ran against it; the session
loses its plan-slot pointer but keeps every set.

`audit_log.actor_id` cascades on user delete (audit rows belong to the
actor; if the actor is gone, the audit trail goes with them).

## Per-user uniqueness constraints

The application contract leans on three load-bearing UNIQUE constraints:

- `body_metrics(user_id, recorded_date) UNIQUE` (V6) — one body-metric row
  per user per calendar date. Drives the `MetricsService.UpsertResult`
  200/201 split (existing date -> 200 OK rewrite; new date -> 201 Created).
- `session_sets(session_id, exercise_id, set_number) UNIQUE` (V5) — drives
  the `SET_NUMBER_DUPLICATE` 409 response surfaced via `ApiError.code` (see
  `docs/API.md`).
- `workout_sessions(user_id) WHERE ended_at IS NULL` partial UNIQUE (V5) —
  one active session per user. Backs the `SESSION_ALREADY_ACTIVE` 409.

Other UNIQUE constraints worth knowing about:

- `users.email` UNIQUE plus a unique functional index on `LOWER(email)` (V2).
- `workout_plans(user_id) WHERE is_active = TRUE` partial UNIQUE (V4) — at
  most one active plan per user.
- `workout_days(plan_id, day_of_week) UNIQUE` (V4).
- `workout_day_exercises(workout_day_id, order_index) UNIQUE` (V4).
- `session_sets(session_id, client_set_id) WHERE client_set_id IS NOT NULL`
  partial UNIQUE (V26) — offline-drainer idempotency key. NULL keys are
  allowed to coexist freely so pre-V26 rows and clients that omit the key
  remain valid.
- `refresh_tokens.token_hash` UNIQUE (V7).
- `password_reset_tokens.token_hash` UNIQUE (V14).
- `webhook_tokens.token` UNIQUE (V21).
- `push_subscriptions.endpoint` UNIQUE (V10) — re-subscribing on the same
  device updates the row instead of inserting a duplicate.
- `user_achievements(user_id, achievement_id) UNIQUE` (V22) — idempotent
  unlocks.
- `monthly_challenges.year_month` UNIQUE (V23).
- `rest_timer_schedules(user_id, session_id) UNIQUE` (V28) — one in-flight
  rest-timer schedule per (user, session); rescheduling replaces the row.

## Generated columns and defaults

- All `id` columns are `UUID PRIMARY KEY` with no DB default; the value is
  generated at the JPA layer (`@UuidGenerator`) before insert. Seed-data
  migrations (V17, V22) use `gen_random_uuid()` directly.
- `session_sets.is_pr` defaults to `false` (V27). The same migration
  backfills existing rows: per `(user_id, exercise_id)`, the completed set
  with the highest Epley one-rep-max gets `is_pr = TRUE`, ties broken on
  `created_at ASC`. The schema does NOT enforce a UNIQUE-true-per-pair
  constraint; the application layer (`SessionSetsService`) is the sole
  writer and tolerates transient violations during demote-then-promote
  inside a single `@Transactional` call.
- Snake-case JSON output is an application-layer concern
  (`@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only) and does not
  propagate to database column names — every column on disk is already
  snake_case Postgres style.

## Indexes

Beyond the UNIQUE indexes listed above, the migrations add explicit B-tree
indexes for the hot read paths:

- `idx_login_attempts_email_time ON login_attempts(email, attempted_at DESC)`
  (V15) — backs the brute-force window query in
  `BruteForceGuard.recentFailedAttempts`.
- `idx_workout_sessions_user_started ON workout_sessions(user_id,
  started_at DESC)` (V5) — history list.
- `idx_body_metrics_user_date ON body_metrics(user_id, recorded_date DESC)`
  (V6) — metrics chart.
- `idx_nutrition_entries_user_date`, `idx_water_entries_user_date` (V17,
  V19) — daily-totals queries.
- `idx_user_achievements_user(user_id, unlocked_at DESC)` (V22).
- `idx_audit_log_at`, `idx_audit_log_actor` (V16).
- `idx_rest_timer_schedules_due` partial index on rows with
  `dispatched_at IS NULL` (V28) — drains the pending queue cheaply.
- `idx_food_items_name_tr_lower`, `idx_food_items_name_en_lower` with
  `varchar_pattern_ops` (V17) — prefix search.
- `idx_exercises_name_tr_lower` (V3) — case-insensitive search.

## What this doc does NOT cover

- **Wire shape (HTTP DTOs, route paths, response codes).** See
  `docs/API.md` and the runtime SpringDoc OpenAPI document at
  `/v3/api-docs`.
- **Export wire shape.** See `docs/EXPORT_FORMAT.md`.
- **Backup posture, restore drill, retention.** See `docs/BACKUP.md` and
  `docs/DEPLOYMENT.md`.
- **Operational binding contract (ports, volumes, healthchecks, logging).**
  See `docs/SELF_HOSTED_CONTRACT.md`.
