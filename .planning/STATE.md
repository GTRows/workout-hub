# State

## Current Position

Milestone: v1.1 Deferred Debt Closure (ACTIVE)
Phase: 45 of 50 (springdoc-2.7-bump) - not started
Plan: 45-01 springdoc-2.7-bump - not started
Status: v1.1 milestone opened to close the four remaining v1.0 carry-forward issues (i-13 SpringDoc 2.7+, i-8b Spring Boot 4 via Jackson 2 -> 3 migration first, i-6b Next 16, i-15 Lighthouse CI + bundle-size + bundle-analyzer). Six phases scoped: 45 springdoc-2.7 (closes i-13), 46 jackson-2-to-3-migration (preparatory for SB4), 47 spring-boot-4 (closes i-8b), 48 next-16 (closes i-6b), 49 lighthouse-ci (closes i-15; user pre-approved the protected `.github/workflows/**` + `scripts/**` + `frontend/package.json` edits), 50 release-v1-1 (cut v1.1.0). Phase 45 is the next phase to plan via `/gsd:plan-phase 45`.
Last activity: 2026-05-09 - v1.1 milestone-opening bookkeeping commit. Re-opened ROADMAP.md with v1.1 as the Active section (kept v0.3..v1.0 archived `<details>` blocks); added the 6 phase outlines (45 springdoc-2.7, 46 jackson-2-to-3-migration, 47 spring-boot-4, 48 next-16, 49 lighthouse-ci, 50 release-v1-1); appended the v1.1 entry to PROJECT.md Active list (mirroring the v0.5/v0.6/v1.0 pattern); advanced this STATE.md cursor from project-complete to v1.1 Phase 45 not-started. No source code edits, no protected-file edits in this bookkeeping commit; the Phase 49 protected-path edits and the Phase 50 IDENTITY/CHANGELOG/package.json/pom.xml lockstep edits land at execution time.

Progress: v1.1 ____________________    0% (0/? plans complete; 0/6 phases shipped)
          v1.1 - Phase 45 (springdoc-2.7-bump) NOT STARTED

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/milestones/v0.5-ROADMAP.md` for full v0.5 archive
- See: `.planning/milestones/v0.6-ROADMAP.md` for full v0.6 archive
- See: `.planning/milestones/v1.0-ROADMAP.md` for full v1.0 archive
- See: `.planning/ROADMAP.md` for the active roadmap (v0.3 through v1.0 shipped and archived; v1.1 active with Phases 45-50 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-6b, i-7b, i-8b, i-13, i-15 open; i-6b/i-8b/i-13/i-15 scheduled for v1.1; i-1, i-2, i-4, i-5, i-6, i-7, i-8, i-9, i-10, i-12, i-14 closed)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** v1.1 Deferred Debt Closure active. The milestone closes the four open carry-forward issues from v1.0: i-13 (Phase 45 springdoc-2.7), i-8b (Phase 46 jackson-2-to-3-migration preparatory + Phase 47 spring-boot-4), i-6b (Phase 48 next-16), i-15 (Phase 49 lighthouse-ci; protected `.github/workflows/**` + `scripts/**` + `frontend/package.json` edits user-pre-approved). Phase 50 cuts v1.1.0. The two remaining open issues NOT in v1.1 scope are i-3 (`.gitignore` `data/` glob audit reference; documentation note only) and i-7b (Testcontainers 2.0.0 jump; trigger blocked on upstream Maven Central GA). Phase 45 is the next-up phase to plan via `/gsd:plan-phase 45`.

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4 + v0.5 + v0.6)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md`, `.planning/milestones/v0.4-ROADMAP.md`, `.planning/milestones/v0.5-ROADMAP.md`, and `.planning/milestones/v0.6-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v1.0 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs); Netty pinned to 4.1.133.Final via override (v0.5.0 patch window) to close CVE-2026-42583/42584/42587. Spring Boot 4 bump (i-8) and Netty 4.2 bump (i-14) deferred to v1.0 Phase 40 / 41.
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).
- ClaudeSummary naming: Direction C per-record `@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only; full-export DTOs stay camelCase for backup-restore stability.
- PR durability: persisted via V27 `is_pr` column with ROW_NUMBER backfill; the wire field round-trips via `FullExportDto.SetRow.isPr` (9th component) with a recompute safety net per touched exercise on import.
- Body-metrics status code: `MetricsService.UpsertResult` wrapper drives `wasCreated ? 201 : 200`; date-keyed identity (V6 UNIQUE-by-`(user, date)`) is the design.
- ApiError.code typed-values catalog: 4 values from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`). Frontend branches on typed `code` (Phase 22.5 helper); tooling should branch on `status` and `code`, not `message`.
- SpringDoc artifact: 2.6.0 starter-webmvc-ui with the `ControllerAdviceBean` workaround in `OpenApiConfig`; bump to 2.7+ deferred per i-13.
- Frontend offline-first: Dexie IndexedDB queue scoped to session-execution; drains on `online` event via `clientSetId` UUID idempotency key (Phase 25 contract).
- Frontend route protection: `frontend/src/middleware.ts` enumerates 12 (app)-group segments in both `PROTECTED_PREFIXES` (runtime) and `config.matcher` (compile-time) arrays; the middleware test scaffold (Phase 29.5) gates future drift.
- Zod schema parity: backend DTO surfaces (ImportResult, FullExport, vapid, push subscribe, rest-timer schedule request/response) round-trip via shared Zod schemas in the frontend (Phase 25.5 + Phase 34); future endpoint additions should mirror this pattern.
- Insights surface composition (v0.6 lock-in): `/insights` is the canonical "Detayli insight sayfasi" deliverable from ProjectBrief Phase 6, NOT a separate `/stats` route. Splitting `insights-client.tsx` into per-card files is deferred to v1.0 polish.
- Service-worker cache versioning (v0.6 lock-in): bump the `CACHE` constant on every behaviour-affecting SW change. v0.6 cycle moved v1 -> v2 (Phase 32) -> v3 (Phase 33). The activate handler already deletes any cache name that does not match the current `CACHE`.
- Web Push schedule contract (v0.6 lock-in): server-scheduled rest-timer push is the primary path because service workers cannot persist a `setTimeout` across termination and `TimestampTrigger` is Chromium-only behind a flag. Cancel + re-schedule is the lifecycle contract; no edit / reschedule / snooze.
- Frontend metrics dependency posture (v0.6 lock-in): `prom-client` and `@opentelemetry/sdk-metrics` rejected because `frontend/package.json` is protected. Hand-rolled in-memory registry under single-instance Self-Hosted Contract is the load-bearing decision.
- Web Vitals reporter dependency posture (v0.6 lock-in): framework-native `next/web-vitals` re-export from the `next` package is used (no `web-vitals` direct dep). Mounted once in the root layout.
- Perf-budget gating posture (v0.6 lock-in): `docs/PERF_BUDGETS.md` is doc-only in v0.6. Lighthouse CI workflow + `scripts/check-bundle-size.mjs` + `@next/bundle-analyzer` dep all rejected because they would each touch a protected path; deferred as i-15.
- Refresh-token hashing posture (v1.0 Phase 41 lock-in): SHA-256 hex over the full JWT bytes via `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(...)`, persisted in `refresh_tokens.token_hash VARCHAR(128) UNIQUE` (V7 migration). The 2026-05-04 `pending_ci_fixes.md` "refresh-token hash collisions" entry was superseded — those tests pass on `96d81b0`. No source change needed; tightening to bcrypt/Argon2 was rejected because refresh tokens carry full JWT entropy at issuance and the hash is a fingerprint for DB lookup, not a password derivation.
- Brute-force lockout posture (v1.0 Phase 41 lock-in): `BruteForceGuard` enforces 10 failures / 15-minute window / 60-minute lockout, throws HTTP 423 via `ResponseStatusException(HttpStatus.LOCKED, ...)`, records every login outcome in `login_attempts` (V15 migration) via `@Transactional(propagation = REQUIRES_NEW)`. `BruteForceLockoutIntegrationTest` covers under-threshold pass, at-threshold lock, post-cooldown unlock — all green on origin/main. Threshold-tightening + (email, IP)-keyed lockout were rejected as v1.0 scope creep.
- Rate-limiting posture (v1.0 Phase 41 lock-in): zero generic in-process rate limiter; the Self-Hosted Contract delegates rate limiting to the operator's reverse proxy. The only in-process throttle is `BruteForceGuard` (per-email login throttle). Adding a generic rate limiter was rejected because it would duplicate operator-layer enforcement and contradict the contract's "never assume public exposure" posture (tailnet-only deployment is the v0.3 lock-in).

### v0.6 Findings (archived)

The v0.6 cycle delivered seven phases (31-37) across 11 plans in a single working session on 2026-05-09 (~2.5 hours real time). Per-phase findings (charts-stats `/insights` re-composition, PWA install prompt + offline shell + SW scope hardening, web push end-to-end with self-test endpoint, server-scheduled rest-timer push with V28 migration, hand-rolled frontend HTTP metrics closing i-4, RouteSkeleton + RouteError + Web Vitals reporter + perf-budget doc, structured-logging test redesign closing i-9) are consolidated in `.planning/milestones/v0.6-ROADMAP.md`. Each phase plan's source-of-truth artifact stays under `.planning/phases/31-*` through `.planning/phases/37-*`. SUMMARY.md files were not produced by the plan executors; the accomplishment record lives in `CHANGELOG.md` `## [0.6.0]`, the per-plan PLAN.md output blocks, and the git commit messages (the v0.6-ROADMAP.md archive consolidates all three).

### Issue-to-Phase Mapping (carried forward)

- i-3: documentation note on `.gitignore` `data/` glob-form correction. Stays carried as audit reference.
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-5: closed by v1.0 Phase 40 Plan 03 (framework-majors). Bumped
  `next-intl` from `^3.26.0` to `^4.11.1` in `frontend/package.json`
  and regenerated `frontend/pnpm-lock.yaml`. Audit of the 4.0 release
  notes (next-intl.dev/blog/next-intl-4-0, March 2025) found 13
  breaking-change surfaces; ZERO required source-file edits in this
  codebase because (a) `frontend/src/i18n/request.ts` already returns
  `{ locale, messages }` from `getRequestConfig` and `await
  requestLocale` (post-3.22 shape), (b) `NextIntlClientProvider` 4.x
  auto-inheritance is opt-in cleanup (the 27 explicit `messages={...}`
  call sites continue to work), (c) the codebase doesn't use
  locale-based routing, `defineRouting`, `next-intl/middleware`,
  `next-intl/navigation`, `format.relativeTime`, or any 3.x deprecated
  API removed in 4.0. Closes open-redirect vuln alerts #8 and #11
  referenced in the original i-5 entry. Runtime stays on Next 15.x;
  next-intl 4.11.1 peers `next ^12.0.0 || ^13.0.0 || ^14.0.0 ||
  ^15.0.0 || ^16.0.0`, install is clean. Forward-compatible with the
  eventual i-6b (Next 16) closure: a future plan that absorbs
  Middleware->Proxy + Suspense gating + lockfile-resolution probe with
  next@^16 + next-intl@^4 co-installed is unblocked.
- i-6: closed by v1.0 Phase 40 Plan 02 (framework-majors) re-defer
  playbook. Plan-author audited `https://registry.npmjs.org/next-intl` on
  2026-05-09 and confirmed zero `next-intl@3.x` versions list
  `next ^16.0.0` in their peer range; the 3.26.x line tail (3.26.5) tops
  out at `next ^15.0.0`. Only `next-intl@4.x` peers `^16.0.0`. Bumping
  Next past 15.x while keeping next-intl 3.x in the same lockfile would
  break `pnpm install` peer-check or require a `pnpm.overrides` hack;
  the roadmap's "in sequence (each its own commit and test pass)" rule
  forbids bundling Next 16 with next-intl 4 in one commit. Frontend
  runtime stays pinned within the existing `^15.1.0` caret (latest 15.x
  backport `15.5.18` per the npm `backport` dist-tag). Successor work
  tracked as i-6b. v0.5 + v0.6 stayed on Next 15.
- i-6b (NEW in v1.0 Phase 40 Plan 02): residual Next.js 16.x major bump
  deferred until Phase 40-03 (i-5 next-intl 3 -> 4 migration) ships
  green. Closure plan must (a) probe the lockfile resolution with
  co-installed next@^16 and next-intl@^4, (b) rename
  `frontend/src/middleware.ts` -> `proxy.ts` (or accept the deprecation
  warning), (c) wrap `frontend/src/app/(auth)/login/page.tsx` in a
  `<Suspense>` boundary so `useSearchParams` does not error at build
  time, (d) bump `engines.node: ">=20.0.0"` -> `">=20.9.0"`, (e) audit
  `next/image` callers for query-string `src` values that the new
  `images.localPatterns` enforcement gates, and (f) decide on Turbopack
  vs `--webpack`. Closure plan must also exercise the protected-file
  pre-authorisation flow because `frontend/package.json` and
  `frontend/pnpm-lock.yaml` are both protected.
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-7b (NEW in v1.0 Phase 39): residual 2.x major bump, deferred until
  upstream `org.testcontainers:testcontainers:2.0.0` GA on Maven Central.
- i-8: closed by v1.0 Phase 40 Plan 01 (framework-majors) re-defer
  playbook. Original 3.5.14 -> 4.0.6 attempt rolled back at commits
  75c3ea1..e49e1dc on 2026-05-09 after Jackson 2 vs Jackson 3
  default-classpath shift surfaced; runtime stays pinned at Spring Boot
  3.5.14 (tail of the 3.5.x line on Maven Central). Successor work tracked
  as i-8b. v0.6 stayed on Spring Boot 3.5.x.
- i-8b (NEW in v1.0 Phase 40 Plan 01): residual Spring Boot 4.0.x major
  bump deferred until either (Path A) `spring-boot-jackson2` compat-module
  runtime probe completes successfully OR (Path B) Jackson 2 -> Jackson 3
  codebase migration sub-plan absorbs the 36-file refactor; whichever
  path reopens, also applies trivial Jackson2-module + Prometheus-actuator
  package-relocation import fixes.
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13 (carried from v0.4): bump SpringDoc to >= 2.7 to remove the ControllerAdviceBean workaround. Trigger: backend dependency review phase post-v0.6 (likely v1.0 Phase 41 or 42).
- i-14: closed by v1.0 Phase 41 Plan 01 (security-hardening). Bumped `<netty.version>` from `4.1.133.Final` to `4.2.13.Final` in `backend/pom.xml` (commit 99f0fb4); removed `CVE-2026-42577` suppression block from `.trivyignore` (commit 5c1cd42). Static audit at HEAD confirmed zero `WebFlux` / `reactor.netty` / `spring-boot-starter-webflux` matches under `backend/`; the only Netty consumer is `async-http-client:2.12.4` for outbound web push (not on the request-handling path). CI is the authoritative gate (local Maven unavailable on the dev host); `mvn verify` and the trivy image-scan job validate Spring Boot 3.5.14 + Netty 4.2.x runtime compatibility on push. Concludes the trivy-suppression carry-forward debt from v0.5.0 + v0.6.0.
- i-15 (NEW in v0.6): Lighthouse CI workflow + `scripts/check-bundle-size.mjs` + `@next/bundle-analyzer` dev dep for perf-budget gating. Trigger: operator approves at least one protected-file edit, OR Phase 38+ (v1.0 hardening) explicitly takes ownership of CI perf gating.

## Session Continuity

Last session: 2026-05-09 - v1.1 milestone opened (deferred debt closure).
Stopped at: v1.1 milestone-opening bookkeeping commit landed (ROADMAP.md re-opened with v1.1 active section + 6 phase outlines; PROJECT.md Active list extended with the v1.1 entry; this STATE.md cursor advanced from project-complete to v1.1 Phase 45 not-started). Next action: `/gsd:plan-phase 45` to author `.planning/phases/45-springdoc-2.7-bump/45-01-PLAN.md`.
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/milestones/v0.4-ROADMAP.md` (full v0.4 archive)
- `.planning/milestones/v0.5-ROADMAP.md` (full v0.5 archive)
- `.planning/milestones/v0.6-ROADMAP.md` (full v0.6 archive)
- `.planning/milestones/v1.0-ROADMAP.md` (full v1.0 archive)
- `.planning/ROADMAP.md` (archived; all milestones v0.3 through v1.0 shipped)
- `.planning/ISSUES.md` (open deferred issues)
- `.planning/HANDOFF.md` (pre-GSD v0.2 snapshot)
- `.planning/codebase/STACK.md`
- `.planning/codebase/ARCHITECTURE.md`
- `.planning/codebase/STRUCTURE.md`
- `.planning/codebase/CONVENTIONS.md`
- `.planning/codebase/TESTING.md`
- `.planning/codebase/INTEGRATIONS.md`
- `.planning/codebase/CONCERNS.md`
- `docs/SELF_HOSTED_CONTRACT.md` (binding operational contract)
- `ProjectBrief.md` (original phase plan)
