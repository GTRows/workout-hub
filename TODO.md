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

- [t-1] Scaffold PHASE 0 baseline (backend, frontend, docker-compose, env)
  - Acceptance: repo is a git repo with an initial commit; `backend/` has a runnable Spring Boot 3 + Java 21 Maven project (compiles via `./mvnw package -DskipTests`); `frontend/` has a runnable Next.js 15 + TS + Tailwind project (`pnpm install` succeeds); `docker-compose.yml` at root declares `db`, `backend`, `frontend` services and `docker compose config` is valid; `.env.example` at root enumerates every env var both services read
  - Notes: prerequisite for `setup-1`, `setup-2`, `setup-3`. Scaffolding only -- no feature code.

- [setup-1] Document Architecture / Entry Flow in CLAUDE.md
  - Acceptance: `### Entry Flow` section has concrete content (how the Spring Boot app boots, how Next.js wires to it, how docker-compose links them); no placeholder or `TODO:` line remains
  - Notes: fill as part of PHASE 0 completion

- [setup-2] Document Module Breakdown in CLAUDE.md
  - Acceptance: `### Module Breakdown` lists each backend package (`auth`, `users`, `exercises`, `workouts`, `sessions`, `metrics`, `export`) and each frontend top-level route group with one-line responsibilities; no placeholder remains
  - Notes: fill as part of PHASE 1-2 once scaffolding exists

- [setup-3] Document Data Storage in CLAUDE.md
  - Acceptance: `### Data Storage` describes the Postgres schema source-of-truth (Flyway migrations path), where uploaded media lives (local volume or MinIO), and where IndexedDB is used client-side; no placeholder remains
  - Notes: tie to PHASE 1 schema work

- [setup-ci] Revisit CI scaffolding decision
  - Acceptance: `.github/workflows/ci.yml` exists and runs lint + test for backend and frontend on push/PR; `ci.yml.template` still kept for reference
  - Notes: deferred at setup. Re-evaluate once PHASE 0 has any runnable `mvn test` / `pnpm test`.

- [setup-release] Revisit release scaffolding decision
  - Acceptance: decision recorded in CHANGELOG or RELEASE.md whether this self-hosted app uses tag-triggered GitHub Releases or image-based docker tags; scaffolding either activated or explicitly marked not-applicable
  - Notes: deferred at setup. Re-evaluate around PHASE 8.

- [setup-icon] Add project icon at assets/icon.png
  - Acceptance: `assets/icon.png` exists, at least 512x512 PNG; matches `PROJECT.yaml#identity.icon`
  - Notes: placeholder is fine for now; replace before first public release.

- [setup-protected-maven] Extend PROTECTED_EXACT when Maven/Gradle lands
  - Acceptance: once the backend scaffold is created, `pre_guard_release_files.py#PROTECTED_EXACT` includes `pom.xml` (or `build.gradle` / `build.gradle.kts` / `gradlew` as applicable) and the CLAUDE.md Protected Files list stays in sync
  - Notes: do not edit the hook until the files actually exist in the repo.

## Blocked

## Done
