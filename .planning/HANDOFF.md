# Session Handoff — 2026-04-28

Use this file to resume work after closing the chat. All state below was true at handoff time.

## Where we are

- v0.2 roadmap is **fully shipped** locally and pushed to `https://github.com/GTRows/workout-hub` (private).
- Repo is on branch `main`, working tree clean (last commit: `a600a58`).
- Frontend tests: **89/89 passing**, typecheck clean, lint clean (3 pre-existing warnings, no errors).
- Backend builds and starts cleanly. Most integration tests pass. CI on `main` is **red** for unrelated pre-existing reasons (see "Open issues" below).

## Repo + CI state

| Item | Value |
|------|-------|
| GitHub | `GTRows/workout-hub` (private) |
| Default branch | `main` |
| Last pushed commit | `a600a58` (template pull) |
| Latest CI run | failure on backend `mvn verify` — unrelated to v0.2 features |
| Frontend job | green |
| CodeQL | runs on push + weekly cron |
| Dependabot | weekly grouped updates (npm/maven/gh-actions) — currently 12 vulnerability alerts (1 high, 11 moderate) needing review at https://github.com/GTRows/workout-hub/security/dependabot |
| Branch protection | NOT enabled (requires Pro for private repos) |

## Open issues — pre-existing test failures surfaced after v0.2

These tests had been broken before this session but were masked by the bean-name conflict that v0.2 exposed (now fixed in commit `1adc1ab`). They are **not regressions** caused by v0.2 — they are pre-existing bugs that finally got to run in CI. Each one is a small, isolated fix.

1. **Refresh-token unique-hash collisions**
   - Affected: `PasswordChangeIntegrationTest`, `AuthFlowIntegrationTest.loginRefreshRotationAgainstSeededUser`, `PasswordResetIntegrationTest` (×2)
   - Symptom: `duplicate key value violates unique constraint "refresh_tokens_token_hash_unique"` -> 500 instead of expected 200/401
   - Root cause hypothesis: tests issue two refresh tokens in the same millisecond with identical `(userId, claims)` and the JWT clock is second-precision, so the SHA-256 of the token text collides.
   - Fix candidate: add a per-issuance nonce to the JWT `jti` claim in `JwtService.generateRefreshToken`, OR raise the JWT precision to milliseconds, OR have tests sleep 1ms between issuances.

2. **BruteForceLockoutIntegrationTest.hittingTheThresholdLocksWithHttp423**
   - Symptom: 200 instead of 423 — lockout never triggers
   - Inspect: `BruteForceGuard` and `LoginAttemptRepository`; check if the failed-attempts counter actually persists (could be `@Transactional` rollback eating the increment).

3. **ExportFormatExampleTest.documentedExampleImportsAndBecomesTheCurrentExportState**
   - Symptom: failure — likely the example JSON in `docs/EXPORT_FORMAT.md` drifted after v0.2 schema additions (water_entries, achievements, monthly_challenges, theme_preference, streak_freeze_used_month, heart_rate_avg_bpm).
   - Fix: regenerate the example JSON with the current schema.

The next session's first job should be a single PR that fixes all three. Each is small.

## Recently closed work (commits since the original v0.2 push)

- `42754ab` PWA icons (t-92)
- `52d2502` Dependabot + CodeQL CI
- `1adc1ab` rename users `SessionsController` bean (unblocked CI)
- `ec70f6f` V23 `year_month` VARCHAR not CHAR
- `cabc477` `TwoFactorService` `@Autowired` on canonical ctor
- `e2c0cc9` `WebPushJavaSender` stays loadable with placeholder VAPID keys
- `4193a53` V2/V8 brittle assertions, Epley scale, session ts ordering, 401-on-anon entry point
- `26b6636` `/api/health/import/fit` returns 400 (not 500) for bogus input
- `a600a58` template pull from `claude-code-template` HEAD: new `/menu`, `/onboard`, `/update`, manifest system, hook tests, broader secret-file deny

## Key files / pointers

- Roadmap and per-task acceptance: `TODO.md`
- Migrations applied so far: `backend/src/main/resources/db/migration/V1__*.sql` … `V25__theme_preference.sql`
- Frontend translations: `frontend/messages/{en,tr}.json`
- Backend module map: `CLAUDE.md` (architecture section)
- Template manifest: `.claude/.template-manifest.json` (run `python .claude/scripts/manifest.py --check` to verify)
- Icon source: `frontend/public/icons/source.svg` (regenerate with `node` script that lives in commit message of `42754ab`)
- Vulnerability triage: https://github.com/GTRows/workout-hub/security/dependabot

## Suggested next moves on resume

1. **Fix the three pre-existing CI failures** (refresh-token collisions, brute-force lockout, export-format example). Single PR, three commits. Backend turns green.
2. **Triage Dependabot alerts** — 1 high, 11 moderate. Mostly transitive `next` / `vite` chain.
3. **Cut v0.2.0 release**: run `/release 0.2.0`, push the tag, let the release workflow draft the GitHub Release.
4. **Plan v0.3** with `/gsd:new-milestone`. Likely candidates: real-time sync (Phase 16), social/sharing layer, AI coach loopback (the deferred "AI olmaz" item the user explicitly declined — keep deferred unless they revisit), real branch protection once repo is public or upgraded.

## How to resume cleanly

```bash
cd D:\Workspace\Self\OpenSource\WorkoutHub
git status                  # should be clean
git pull
python .claude/scripts/manifest.py --check    # verifies template state
gh run list --repo GTRows/workout-hub --branch main --limit 5
```

If anything below this line drifts, treat this file as a snapshot, not gospel. Re-derive from `git log` and `gh run list`.
