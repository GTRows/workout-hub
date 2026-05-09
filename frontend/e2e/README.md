# Playwright e2e suite

End-to-end browser tests covering the critical user flows of WorkoutHub.
The suite runs against a live `docker compose up` stack on the default
Turkish locale, so all locators use stable HTML attributes (input `id`,
`data-testid`) plus EN+TR alternation regexes for action buttons.

## Prerequisites

The suite assumes a running stack at `E2E_WEB_BASE_URL`
(default `http://localhost:3000`) backed by a real Spring Boot backend
on `:8080` and a Postgres 16 cluster. Public registration was removed in
t-51, so an admin user must be seeded before the stack starts:

- Set `APP_ADMIN_EMAIL` (the email the admin will log in with).
- Set `APP_ADMIN_PASSWORD_HASH` to a bcrypt hash of the password the
  spec will use. Generate the hash with:

  ```sh
  ../../scripts/hash-password.sh 'ChangeMe-Admin-1!'
  ```

  The seeding source of truth is
  `backend/src/main/java/com/workouthub/admin/AdminSeeder.java`.

## Env vars consumed by the specs

| Variable | Default | Read by |
|---|---|---|
| `E2E_WEB_BASE_URL` | `http://localhost:3000` | `playwright.config.ts` |
| `E2E_ADMIN_EMAIL` | `admin@workouthub.local` | `e2e/fixtures/auth.ts` |
| `E2E_ADMIN_PASSWORD` | `ChangeMe-Admin-1!` | `e2e/fixtures/auth.ts` |

`E2E_ADMIN_EMAIL` must equal the seeded `APP_ADMIN_EMAIL`, and
`E2E_ADMIN_PASSWORD` must be the plaintext that hashes to
`APP_ADMIN_PASSWORD_HASH`.

## Spec inventory

- `critical-flow.spec.ts` — log in, start a session, log three sets
  against the first planned exercise, finish the session, and download
  the Claude summary JSON. Covers the canonical "admin-seeded login ->
  active workout day -> log three sets -> finish session -> download
  Claude summary" path.
- `responsive-360.spec.ts` — at a 360x800 viewport, asserts no
  horizontal overflow and that the bottom tab-bar is present on
  `/dashboard`, `/plan`, `/history`, `/exercises`, `/nutrition`.
  Captures a screenshot per page under `e2e/screenshots/` (gitignored).
- `session-save-resume.spec.ts` — proves a partial session (one set
  logged) survives navigation away from `/session` and is resumable
  from the dashboard's Resume CTA, exercising the offline-first
  contract from phase 25.

## Run commands

From `frontend/`:

```sh
pnpm test:e2e                                 # full suite
pnpm test:e2e -- critical-flow.spec.ts        # single spec
pnpm test:e2e --headed                        # debug with a visible browser
```

`pnpm exec playwright test --list` enumerates the specs without
executing them — useful for confirming TypeScript compiles.

## Convention: `data-testid` policy

The suite adds `data-testid` attributes only on locators that have no
i18n-immune alternative. Today that is two attributes on two elements
in `frontend/src/app/(app)/session/[id]/session-client.tsx`:

- `data-testid="set-reps-input"` on the rep-count input per set row.
- `data-testid="set-done-button"` on the per-set submit button.

Form fields with stable `id` attributes (e.g. `#email`, `#password` on
the login page) and elements queryable by ARIA `role` + accessible name
should be addressed by those instead of test-ids.

## CI wiring

Pending plan 38-02; the suite does not yet run in
`.github/workflows/ci.yml`. Plan 38-02 will add an `e2e` job that
brings up the stack, installs Playwright browsers, runs the suite, and
publishes the HTML report on failure.
