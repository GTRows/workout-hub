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

- [t-90] [p14] Monthly challenge feature (admin-defined, global)
  - Acceptance: monthly_challenges table + admin CRUD + /api/challenges/current returns live progress; integration tests cover progress math across month boundary.

- [t-91] [p14] Streak freeze (1 skip/month) - extends StreakCalculator
  - Acceptance: user_streak_state.freeze_used_in_month + enabled-by-default; StreakCalculatorTest gains 3 cases (no-freeze, freeze-used, freeze-mid-broken).

- [t-92] [p15] Real app icon asset set (closes setup-icon DEFERRED)
  - Acceptance: Designed 512/192/maskable/apple-touch PNGs committed; placeholder PNGs replaced; Lighthouse PWA >= 90; setup-icon removed from DEFERRED.md.

- [t-93] [p15] Exercise-detail modal on session screen
  - Acceptance: Click on exercise title opens modal with bilingual form tips + common mistakes + last performance; RTL covers open/close + content + keyboard escape.

- [t-94] [p15] dnd-kit visual drag reorder in plan viewer
  - Acceptance: @dnd-kit installed; mouse drag reorders; existing Move up/down buttons stay as keyboard fallback; RTL covers keyboard reorder AND @dnd-kit/test-utils drag simulation.

- [t-95] [p15] Responsive audit at 360px + bottom tab-bar below sm
  - Acceptance: BottomNav component visible under sm; top Nav switches to compact; Playwright tests take viewport screenshots at 360x800 for 5 key pages and assert no horizontal overflow.

- [t-96] [p15] Dark mode toggle (persisted in localStorage + profile column)
  - Acceptance: Tailwind class-based dark mode wired; toggle in nav; V21 user_profile.theme_preference; RTL covers toggle flipping the html class + persistence across reload.

- [t-97] [p15] PR celebration on set save (when newPr=true)
  - Acceptance: Toast + subtle scale animation on the set card; RTL covers toast appearing for newPr=true fetch response and not for regular sets.

- [t-98] [p15] ExerciseMedia component (video/GIF on detail page, lazy loaded)
  - Acceptance: Uses existing exercises.video_url; lazy-load via loading="lazy" + IntersectionObserver; RTL covers render with and without URL; a11y alt/title.

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
- [t-55] 2026-04-23 -- ESLint 9 flat config + pnpm lint wired into CI
- [t-56] 2026-04-23 -- Full-import plans + sessions path (replace semantics preserving UUIDs; multi-active-plan guard)
- [t-54] 2026-04-23 -- Supplement reminder trigger + V11 reminder_time (webpush sender carved into t-57)
- [t-57] 2026-04-23 -- Real Web Push sender via nl.martijndwars:web-push 5.1.1 (VAPID sign + 404/410 pruning; activated when private-key is set)
- [t-58] 2026-04-24 -- docs/EXPORT_FORMAT.md + guarded example JSON round-tripped by ExportFormatExampleTest
- [t-59] 2026-04-24 -- Per-section export endpoints + single-slice download card on /export
- [t-60] 2026-04-24 -- Per-section import endpoints with replace semantics + detach-sessions-from-days safety query
- [t-61] 2026-04-24 -- ImportValidator with errors/warnings/suggestions + UI renders both under the import success
- [t-62] 2026-04-24 -- /export/ai cheatsheet + section picker + /export/ai/apply diff-preview-then-commit
- [t-63] 2026-04-24 -- Strong/Hevy-compatible sessions.csv export endpoint + UI button
- [t-64] 2026-04-24 -- Strong CSV import with tiered ExerciseNameMatcher + unmatched-names surfaced
- [t-65] 2026-04-24 -- /api/export/plan.ics RFC 5545 feed (one VEVENT per plan day, BYDAY RRULEs)
- [t-66] 2026-04-24 -- TOTP 2FA (samstevens.totp) with setup/verify/status/disable + login gate
- [t-67] 2026-04-24 -- Active session list + revoke (V13 UA/last_used + /api/users/me/sessions)
- [t-68] 2026-04-24 -- Admin password reset link + /api/auth/reset-password single-use flow
- [t-69] 2026-04-24 -- Brute-force lockout (V15 login_attempts + BruteForceGuard, HTTP 423)
- [t-70] 2026-04-24 -- Audit log (V16 audit_log + /api/admin/audit + create/update/delete/reset_link coverage)
- [t-71] 2026-04-24 -- Self-serve password change + revoke-all-sessions (PUT /api/users/me/password)
- [t-72] 2026-04-24 -- Prometheus endpoint + docs/OBSERVABILITY.md scrape snippet (tag application=workouthub)
- [t-73] 2026-04-24 -- ECS-JSON structured logs on prod profile + TraceIdFilter MDC propagation
- [t-74] 2026-04-24 -- Grafana dashboard JSON (6 panels) + shape-guard test
- [t-75] 2026-04-24 -- Alertmanager rules (5xx / pool / GC / disk) + YAML-parse guard test
- [t-76] 2026-04-24 -- Uptime-Kuma monitor JSON + health unauth-reachable guard test
- [t-77] 2026-04-24 -- Homelab Caddy snippet + DEPLOYMENT.md runbook for workouthub.<domain>
- [t-78] 2026-04-24 -- Optional Authentik OIDC login (hand-rolled, flag-gated, auto-provision) - Phase 11 complete
- [t-79] 2026-04-24 -- V17 nutrition schema + 50-item TR food seed + repository tests
- [t-80] 2026-04-24 -- /api/foods search + /api/nutrition per-user CRUD with macro scaling
- [t-81] 2026-04-24 -- /nutrition page (autocomplete + day nav + per-row macro display)
- [t-82] 2026-04-24 -- V18 nutrition goals on user_profile + profile form fields + goals line on /nutrition
- [t-83] 2026-04-25 -- V19 water_entries + /api/water + /nutrition water card; Phase 12 complete
- [t-84] 2026-04-25 -- Apple Health parser (StAX, ZIP unwrap) + /api/health/import/apple + /export card
- [t-85] 2026-04-25 -- Google Fit parser (Jackson, dataPoint+session shapes) + /api/health/import/google-fit
- [t-86] 2026-04-25 -- Hand-rolled Garmin .fit parser + V20 heart_rate_avg_bpm + /api/health/import/fit
- [t-87] 2026-04-25 -- V21 webhook_tokens + /api/webhooks/scale/{token} + profile token UI; Phase 13 complete
- [t-88] 2026-04-25 -- V22 achievements + 10 seed defs + AchievementEvaluator hooks (session/set) + /api/achievements/me
- [t-89] 2026-04-25 -- /achievements grid (unlocked + next-up locked) + nav link + RTL coverage
