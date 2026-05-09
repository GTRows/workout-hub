# State

## Current Position

Milestone: v1.1 Deferred Debt Closure (ACTIVE)
Phase: 46 of 50 (jackson-2-to-3-migration) - DEFERRED 2026-05-09
Phase: 47 of 50 (spring-boot-4-with-jackson-3) - SHIPPED 2026-05-09 (consolidates Phase 46 + 47 scope per Phase 46-01 deferral)
Phase: 48 of 50 (next-16) - SHIPPED 2026-05-09
Phase: 49 of 50 (lighthouse-ci) - SHIPPED 2026-05-09
Plan: 46-01 jackson-2-to-3-migration - shipped 2026-05-09 (deferral; no source / pom edits; consolidates 36-file Jackson rewrite into Phase 47)
Plan: 47-01 spring-boot-4-with-jackson-3 - shipped 2026-05-09 (Spring Boot 3.5.14 -> 4.0.6, SpringDoc 2.8.17 -> 3.0.3, 36 Jackson 2 files migrated to tools.jackson.*, JacksonConfig flipped to JsonMapperBuilderCustomizer, PrometheusMetricsController actuator imports relocated, Netty 4.2.13.Final pin preserved; closes i-8b)
Plan: 48-01 next-16 - shipped 2026-05-09 (Next 15.1 -> 16.2.6, eslint-config-next 15.5.15 -> 16.2.6, engines.node >=20.9.0, frontend/src/middleware.ts -> proxy.ts with function rename, login page Suspense-wrapped via Path B, eslint flat-config migration, 6 react-hooks/set-state-in-effect targeted disables, exercise-progress-chart renderTooltip refactor; closes i-6b)
Plan: 49-01 lighthouse-ci - shipped 2026-05-09 (.github/workflows/lighthouse.yml + scripts/check-bundle-size.mjs + .lighthouserc.cjs created; @next/bundle-analyzer ^16.2.6 + @lhci/cli ^0.15.1 added to frontend devDeps; build:analyze script + next.config.ts withBundleAnalyzer wrap; closes i-15)
Status: v1.1 Phase 49 shipped: perf-budget gating posture flipped from doc-only (v0.6 Phase 36-03) to CI-gated. New `.github/workflows/lighthouse.yml` runs two jobs (bundle-size + lighthouse) on PR + main pushes; `scripts/check-bundle-size.mjs` enforces 250KB compressed per-route ceiling against webpack-emitted manifests; `.lighthouserc.cjs` declares the unauthenticated audited routes (/login, /, /offline, /_not-found) and assertions mirroring docs/PERF_BUDGETS.md verbatim. `@lhci/cli@^0.15.1` is the Lighthouse runner, `@next/bundle-analyzer@^16.2.6` wraps next.config.ts with ANALYZE=true opt-in. Local gates green: typecheck, lint (0 errors, 3 pre-existing warnings), test 351/351 across 64 files, build --webpack compiled successfully with 18 static pages, node scripts/check-bundle-size.mjs exit 0 with all 20 routes under the 250KB ceiling (largest `/` at 243.7KB; root-main shared 122.6KB). i-15 closed; the v1.1 milestone advances to Phase 50 (release-v1-1) which is the next phase to plan via `/gsd:plan-phase 50`.
Last activity: 2026-05-09 - Phase 49 Plan 01 shipped (lighthouse.yml + check-bundle-size.mjs + lighthouserc + bundle-analyzer/lhci deps + next.config.ts wrap; closes i-15).

Progress: v1.1 #################____   83% (6/7 plans complete; 5/6 phases shipped)
          v1.1 - Phase 50 (release-v1-1) NEXT

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/milestones/v0.5-ROADMAP.md` for full v0.5 archive
- See: `.planning/milestones/v0.6-ROADMAP.md` for full v0.6 archive
- See: `.planning/milestones/v1.0-ROADMAP.md` for full v1.0 archive
- See: `.planning/ROADMAP.md` for the active roadmap (v0.3 through v1.0 shipped and archived; v1.1 active with Phases 45-50 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-7b open; i-1, i-2, i-4, i-5, i-6, i-6b, i-7, i-8, i-8b, i-9, i-10, i-12, i-13, i-14, i-15 closed)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** v1.1 Deferred Debt Closure active. The milestone has closed all four open carry-forward issues from v1.0: i-13 (Phase 45 springdoc-2.7; SHIPPED), i-8b (Phase 46 jackson-2-to-3-migration preparatory + Phase 47 spring-boot-4; SHIPPED), i-6b (Phase 48 next-16; SHIPPED), i-15 (Phase 49 lighthouse-ci; SHIPPED). Phase 50 cuts v1.1.0. The two remaining open issues NOT in v1.1 scope are i-3 (`.gitignore` `data/` glob audit reference; documentation note only) and i-7b (Testcontainers 2.0.0 jump; trigger blocked on upstream Maven Central GA). Phase 50 is the next-up phase to plan via `/gsd:plan-phase 50`.

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4 + v0.5 + v0.6)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md`, `.planning/milestones/v0.4-ROADMAP.md`, `.planning/milestones/v0.5-ROADMAP.md`, and `.planning/milestones/v0.6-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v1.0 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 4.0.x (bumped from 3.5.14 in v1.1 Phase 47-01 to close i-8b; the v0.3.1 -> v0.5.0 -> v1.0 history was 3.4 -> 3.5.x; v0.5.0 pinned Netty 4.1.133.Final to close CVE-2026-42583/42584/42587; v1.0 Phase 41-01 bumped Netty 4.1.133.Final -> 4.2.13.Final to close i-14 CVE-2026-42577; the 4.2.13.Final pin is preserved via `<netty.version>` property override across the SB4 jump because the SB4 BOM ships 4.2.12.Final).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).
- ClaudeSummary naming: Direction C per-record `@JsonNaming(SnakeCaseStrategy)` on `ClaudeSummaryDto` only; full-export DTOs stay camelCase for backup-restore stability.
- PR durability: persisted via V27 `is_pr` column with ROW_NUMBER backfill; the wire field round-trips via `FullExportDto.SetRow.isPr` (9th component) with a recompute safety net per touched exercise on import.
- Body-metrics status code: `MetricsService.UpsertResult` wrapper drives `wasCreated ? 201 : 200`; date-keyed identity (V6 UNIQUE-by-`(user, date)`) is the design.
- ApiError.code typed-values catalog: 4 values from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`). Frontend branches on typed `code` (Phase 22.5 helper); tooling should branch on `status` and `code`, not `message`.
- SpringDoc artifact (v1.1 Phase 45 lock-in): `2.8.17` starter-webmvc-ui. The `ControllerAdviceBean(Object)` workaround was removed when 2.7+ landed (`apiErrorSchemaCustomizer` `@Bean` and `springdoc.override-with-generic-response: false` both deleted). Schemas surface via the framework default path; `OpenApiSurfaceIntegrationTest` (5 tests) is the regression gate. SpringDoc 3.x intentionally deferred (targets Spring Boot 4 / Jakarta EE 11; sequenced behind Phase 47 spring-boot-4 in v1.1).
- Frontend offline-first: Dexie IndexedDB queue scoped to session-execution; drains on `online` event via `clientSetId` UUID idempotency key (Phase 25 contract).
- Frontend route protection: `frontend/src/middleware.ts` enumerates 12 (app)-group segments in both `PROTECTED_PREFIXES` (runtime) and `config.matcher` (compile-time) arrays; the middleware test scaffold (Phase 29.5) gates future drift.
- Zod schema parity: backend DTO surfaces (ImportResult, FullExport, vapid, push subscribe, rest-timer schedule request/response) round-trip via shared Zod schemas in the frontend (Phase 25.5 + Phase 34); future endpoint additions should mirror this pattern.
- Insights surface composition (v0.6 lock-in): `/insights` is the canonical "Detayli insight sayfasi" deliverable from ProjectBrief Phase 6, NOT a separate `/stats` route. Splitting `insights-client.tsx` into per-card files is deferred to v1.0 polish.
- Service-worker cache versioning (v0.6 lock-in): bump the `CACHE` constant on every behaviour-affecting SW change. v0.6 cycle moved v1 -> v2 (Phase 32) -> v3 (Phase 33). The activate handler already deletes any cache name that does not match the current `CACHE`.
- Web Push schedule contract (v0.6 lock-in): server-scheduled rest-timer push is the primary path because service workers cannot persist a `setTimeout` across termination and `TimestampTrigger` is Chromium-only behind a flag. Cancel + re-schedule is the lifecycle contract; no edit / reschedule / snooze.
- Frontend metrics dependency posture (v0.6 lock-in): `prom-client` and `@opentelemetry/sdk-metrics` rejected because `frontend/package.json` is protected. Hand-rolled in-memory registry under single-instance Self-Hosted Contract is the load-bearing decision.
- Web Vitals reporter dependency posture (v0.6 lock-in): framework-native `next/web-vitals` re-export from the `next` package is used (no `web-vitals` direct dep). Mounted once in the root layout.
- Perf-budget gating posture (v0.6 lock-in): `docs/PERF_BUDGETS.md` is doc-only in v0.6. Lighthouse CI workflow + `scripts/check-bundle-size.mjs` + `@next/bundle-analyzer` dep all rejected because they would each touch a protected path; deferred as i-15.
- Perf-budget gating posture (v1.1 Phase 49 lock-in): flipped from "doc-only" (v0.6 Phase 36-03 ship of `docs/PERF_BUDGETS.md`) to "CI-gated". `.github/workflows/lighthouse.yml` runs `pnpm exec lhci autorun` against the just-built frontend on `pull_request` + `push: branches: [main]`; the `.lighthouserc.cjs` config at repo root mirrors `docs/PERF_BUDGETS.md` verbatim — LCP <= 2.5s, FCP <= 1.8s, CLS <= 0.1, TTFB <= 0.8s gated as `error`; INP <= 200ms gated as `warn` (Lighthouse's headless audit cannot reliably measure interaction-to-next-paint without real user interaction); `categories:performance >= 0.9` gated as `error`. `scripts/check-bundle-size.mjs` computes per-route gzip-compressed first-load JS size from the webpack-emitted `.next/build-manifest.json` + `.next/app-path-routes-manifest.json` + walked `.next/static/chunks/app/<route>/`, fails if any route exceeds 250KB. Audited Lighthouse routes: `/login`, `/`, `/offline`, `/_not-found` (unauthenticated set only; authenticated-route coverage deferred to v1.2+ because it requires a CI-side backend boot). `@next/bundle-analyzer` (`^16.2.6` lockstep with `next`) wraps `frontend/next.config.ts` with `enabled: process.env.ANALYZE === "true"` (no-op unless `ANALYZE=true pnpm build:analyze` for operator-side investigation; not gated). `@lhci/cli` (`^0.15.1` — npm `latest` dist-tag at execute time) is the Lighthouse runner. Bundler stays on webpack via Phase 48's `--webpack` flag (`scripts.build:analyze` also passes `--webpack`); a future Turbopack-as-default migration would update the bundle-size script to read `.next/app-build-manifest.json` instead.
- Refresh-token hashing posture (v1.0 Phase 41 lock-in): SHA-256 hex over the full JWT bytes via `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(...)`, persisted in `refresh_tokens.token_hash VARCHAR(128) UNIQUE` (V7 migration). The 2026-05-04 `pending_ci_fixes.md` "refresh-token hash collisions" entry was superseded — those tests pass on `96d81b0`. No source change needed; tightening to bcrypt/Argon2 was rejected because refresh tokens carry full JWT entropy at issuance and the hash is a fingerprint for DB lookup, not a password derivation.
- Brute-force lockout posture (v1.0 Phase 41 lock-in): `BruteForceGuard` enforces 10 failures / 15-minute window / 60-minute lockout, throws HTTP 423 via `ResponseStatusException(HttpStatus.LOCKED, ...)`, records every login outcome in `login_attempts` (V15 migration) via `@Transactional(propagation = REQUIRES_NEW)`. `BruteForceLockoutIntegrationTest` covers under-threshold pass, at-threshold lock, post-cooldown unlock — all green on origin/main. Threshold-tightening + (email, IP)-keyed lockout were rejected as v1.0 scope creep.
- Rate-limiting posture (v1.0 Phase 41 lock-in): zero generic in-process rate limiter; the Self-Hosted Contract delegates rate limiting to the operator's reverse proxy. The only in-process throttle is `BruteForceGuard` (per-email login throttle). Adding a generic rate limiter was rejected because it would duplicate operator-layer enforcement and contradict the contract's "never assume public exposure" posture (tailnet-only deployment is the v0.3 lock-in).
- Jackson 3 migration posture (v1.1 Phase 47 lock-in): shipped in Phase 47-01. The 36-file Jackson 2 -> Jackson 3 (`com.fasterxml.jackson.*` -> `tools.jackson.*`) rewrite landed in a single CI-validated commit alongside the `<spring-boot-starter-parent>` 3.5.14 -> 4.0.6 bump and the `<springdoc.version>` 2.8.17 -> 3.0.3 bump. Path B (full Jackson 3 migration) selected; Path A (`spring-boot-jackson2` compat module) foreclosed. JacksonConfig declares a Jackson 3 `JsonMapperBuilderCustomizer @Bean` from `org.springframework.boot.jackson.autoconfigure` that registers `tools.jackson.datatype.jsr310.JavaTimeModule`, disables `tools.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`, and sets `tools.jackson.annotation.JsonInclude.Include.NON_NULL`. `AuditLogService` + `WebPushNotificationDispatcher` catch `tools.jackson.core.JacksonException` (Jackson 3 unified exception hierarchy; replaces `JsonProcessingException`). PrometheusMetricsController actuator imports flipped from `org.springframework.boot.actuate.metrics.export.prometheus.*` to `org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.*` (Spring Boot 4 per-feature autoconfigure module split).
- Spring Boot 4 BOM transitive lock-ins (v1.1 Phase 47-01): Spring Framework 7.0.7 / Spring Security 7.0.5 / Hibernate 7.2.12.Final / Jakarta EE 11 (Servlet 6.1.0, Persistence 3.2.0, Validation 3.1.1, Annotation 3.0) / Tomcat 11 / Micrometer 1.15.x / Netty 4.2.13.Final (override preserved across the bump; the SB4 BOM default of 4.2.12.Final is overridden to retain CVE-2026-42577 patch posture per i-14) / Jackson 3.0.x / SpringDoc 3.0.3 (bumped from 2.8.17 alongside the SB4 jump per Phase 45's i-13 closure note that explicitly sequenced SpringDoc 3.x for Phase 47).
- Next.js runtime line (v1.1 Phase 48 lock-in): `^16.2.6` pinned in `frontend/package.json` (latest stable on the npm `latest` dist-tag at execute time). The Middleware -> Proxy rename landed (`frontend/src/middleware.ts` -> `frontend/src/proxy.ts`; function `middleware` -> `proxy`; companion test renamed in lockstep). The `<Suspense>` boundary around the `useSearchParams`-reading body of `frontend/src/app/(auth)/login/page.tsx` (Path B: single-file inner `LoginForm` function with inline skeleton fallback) closes the Next 16 hard-error gate. `engines.node` bumped to `>=20.9.0`. Bundler stayed on webpack via `--webpack` opt-out flag for the v1.1 ship; Turbopack-as-default migration deferred as a separate concern. Codebase has zero `next/image` callers (`frontend/src/components/exercise-media.tsx` uses raw `<img>` intentionally); framework default `images.localPatterns` enforcement is moot. `next.config.ts` untouched (no `images:`, `experimental:`, `webpack:`, or `turbo:` block needed). next-intl 4.11.1 (Phase 40-03 lock-in) peers cleanly against `next@^16`; install is clean with zero `pnpm.overrides` or `--strict-peer-dependencies=false` hacks. eslint-config-next@16 ships flat config natively; `frontend/eslint.config.mjs` migrated from `FlatCompat` shim to direct imports of `eslint-config-next/core-web-vitals` and `/typescript`. eslint-plugin-react-hooks@7 (transitive via eslint-config-next@16) introduced two new rules: `set-state-in-effect` (6 targeted disables with platform-API rationale) and `static-components` (refactored exercise-progress-chart's `TooltipContent` to a `renderTooltip` render-prop function). The Phase 35 metric-recording contract and the Phase 29.5 matcher-coverage contract both survive the rename intact (the renamed `proxy.test.ts` 11 cases assert this).

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
- i-6b: closed by v1.1 Phase 48 Plan 01 (next-16). Bumped `next` from
  `^15.1.0` to `^16.2.6` and `eslint-config-next` from `^15.5.15` to
  `^16.2.6` in `frontend/package.json`; bumped `engines.node` from
  `>=20.0.0` to `>=20.9.0`; regenerated `frontend/pnpm-lock.yaml`
  cleanly (zero peer warnings, zero `pnpm.overrides`, zero
  `--strict-peer-dependencies=false` flags). Renamed
  `frontend/src/middleware.ts` -> `frontend/src/proxy.ts` (function
  `middleware` -> `proxy`; comment block updated to reference the v1.1
  Phase 48 closure context) and the companion test
  `frontend/src/middleware.test.ts` -> `frontend/src/proxy.test.ts`
  (import + 8 call-site updates + 3 cosmetic describe-label updates;
  preserved the matcher-coverage + redirect-behavior + metric-recording
  assertions verbatim). Wrapped the `useSearchParams`-reading body of
  `frontend/src/app/(auth)/login/page.tsx` in a `<Suspense>` boundary
  via Path B (single-file inner `LoginForm` function with inline
  skeleton fallback). Bundler choice: webpack via `--webpack` flag
  retained on `dev` and `build` scripts; Turbopack-as-default migration
  deferred. Codebase has zero `next/image` callers; `images.localPatterns`
  enforcement is moot. `next.config.ts` untouched.
  `frontend/eslint.config.mjs` migrated from `FlatCompat` shim to
  native flat-config import paths (eslint-config-next@16 ships flat
  config natively; the shim triggered a circular-structure JSON
  serialization error). Six files received targeted
  `react-hooks/set-state-in-effect` disable comments with rationale
  (`install-prompt-card`, `push-permission-card`, `theme-toggle`,
  `exercise-media`, `profile/notifications-section`,
  `profile/profile-client`) where state is initialized from platform
  APIs (localStorage, Notification.permission, IntersectionObserver,
  TanStack Query cache) that the new eslint-plugin-react-hooks@7 rule
  does not model. `exercise-progress-chart.tsx` refactored its inner
  `TooltipContent` to a `renderTooltip` render-prop function (recharts
  `Tooltip` `content` prop accepts both) to satisfy the new
  `react-hooks/static-components` rule without disabling.
  `frontend/tsconfig.json` + `frontend/next-env.d.ts` auto-updated by
  Next 16's build (`jsx` -> `react-jsx`; routes.d.ts triple-slash
  reference -> import; `.next/dev/types/**/*.ts` include added). Local
  gates green: `pnpm typecheck` clean, `pnpm lint` zero errors (3
  pre-existing warnings unrelated to the bump), `pnpm test` 351/351
  across 64 files (proxy.test.ts 11/11; login/page.test.tsx 5/5),
  `pnpm build --webpack` "Compiled successfully in 9.3s" with 18
  static pages generated and the "Proxy (Middleware)" route surfaced.
  The Phase 35 metric-recording contract and the Phase 29.5
  matcher-coverage contract both survive the rename. next-intl 4.11.1
  (Phase 40-03 lock-in) peers cleanly against `next@^16` (peer range
  `^12.0.0 || ^13.0.0 || ^14.0.0 || ^15.0.0 || ^16.0.0`).
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-7b (NEW in v1.0 Phase 39): residual 2.x major bump, deferred until
  upstream `org.testcontainers:testcontainers:2.0.0` GA on Maven Central.
- i-8: closed by v1.0 Phase 40 Plan 01 (framework-majors) re-defer
  playbook. Original 3.5.14 -> 4.0.6 attempt rolled back at commits
  75c3ea1..e49e1dc on 2026-05-09 after Jackson 2 vs Jackson 3
  default-classpath shift surfaced; runtime stays pinned at Spring Boot
  3.5.14 (tail of the 3.5.x line on Maven Central). Successor work tracked
  as i-8b. v0.6 stayed on Spring Boot 3.5.x.
- i-8b: closed by v1.1 Phase 47 Plan 01 (spring-boot-4-with-jackson-3).
  Bumped `<spring-boot-starter-parent>` from `3.5.14` to `4.0.6` and
  `<springdoc.version>` from `2.8.17` to `3.0.3` in `backend/pom.xml`;
  preserved `<netty.version>4.2.13.Final</netty.version>` property
  override across the bump (SB4 BOM defaults to 4.2.12.Final; override
  retains the i-14 CVE-2026-42577 patch posture). Migrated 36 Jackson
  2 source files (`com.fasterxml.jackson.*` -> `tools.jackson.*`): 7
  main-source files (`JacksonConfig.java` rewritten to declare a Jackson
  3 `JsonMapperBuilderCustomizer @Bean` from `org.springframework.boot.jackson.autoconfigure`;
  `ClaudeSummaryDto`, `AuditLogService`, `WebPushNotificationDispatcher`,
  `GoogleFitParser`, `ScalePayload`, `SupplementTiming`) plus 29
  test-source files (28 `@Autowired ObjectMapper` integration tests +
  the standalone `WebPushNotificationDispatcherTest`). Renamed
  `JsonProcessingException` catch sites to `JacksonException` in
  `AuditLogService.toJson` and `WebPushNotificationDispatcher.buildPayload`
  (Jackson 3 unified exception hierarchy). Flipped
  `PrometheusMetricsController` actuator imports to
  `org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.*`.
  Path B (full Jackson 3 migration) shipped; Path A
  (`spring-boot-jackson2` compat module) foreclosed per Phase 46-01
  lock-in. BOM transitives carried forward: Spring Framework 7.0.7,
  Spring Security 7.0.5, Hibernate 7.2.12.Final, Jakarta EE 11 (Servlet
  6.1.0, Persistence 3.2.0, Validation 3.1.1, Annotation 3.0), Tomcat
  11, Micrometer 1.15.x, Jackson 3.0.x. CI is the authoritative gate
  (local Maven gap on the Windows dev host); the GitHub Actions backend
  job validates `mvn verify` (Java 21), the Testcontainers integration
  test suite, `OpenApiSurfaceIntegrationTest` (5 tests), `MetricsIntegrationTest`,
  `ExportIntegrationTest`, `FullExportImportIntegrationTest`,
  `AuditLogIntegrationTest`, `WebPushNotificationDispatcherTest`,
  `StructuredLoggingTest`, `GrafanaDashboardTest`, the trivy image-scan
  job, and the jacoco >= 70% coverage gate. Concludes the largest open
  carry-forward debt from v1.0 and unblocks v1.1 Phase 48 (next-16) and
  Phase 49 (lighthouse-ci).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13: closed by v1.1 Phase 45 Plan 01 (springdoc-2.7-bump). Bumped `<springdoc.version>` from `2.6.0` to `2.8.17` in `backend/pom.xml` (commit 4889375); deleted the `apiErrorSchemaCustomizer` `OpenApiCustomizer` `@Bean` from `OpenApiConfig.java` (33 lines + 6 orphan imports; commit 1140faa); deleted the `springdoc.override-with-generic-response: false` line + comment block from `application.yml` (commit f3be469). SpringDoc 2.7.0 patches the removed Spring Framework 6.2 `ControllerAdviceBean(Object)` constructor; the framework default `GenericResponseService` scan now surfaces ApiError schemas without the manual customizer. CI gate validates `mvn verify` green and `OpenApiSurfaceIntegrationTest` (5 tests) green. Closes the v0.4 fix bundle (commit `c1b95f7`) workaround.
- i-14: closed by v1.0 Phase 41 Plan 01 (security-hardening). Bumped `<netty.version>` from `4.1.133.Final` to `4.2.13.Final` in `backend/pom.xml` (commit 99f0fb4); removed `CVE-2026-42577` suppression block from `.trivyignore` (commit 5c1cd42). Static audit at HEAD confirmed zero `WebFlux` / `reactor.netty` / `spring-boot-starter-webflux` matches under `backend/`; the only Netty consumer is `async-http-client:2.12.4` for outbound web push (not on the request-handling path). CI is the authoritative gate (local Maven unavailable on the dev host); `mvn verify` and the trivy image-scan job validate Spring Boot 3.5.14 + Netty 4.2.x runtime compatibility on push. Concludes the trivy-suppression carry-forward debt from v0.5.0 + v0.6.0.
- i-15: closed by v1.1 Phase 49 Plan 01 (lighthouse-ci). Created `.github/workflows/lighthouse.yml` (two jobs: `bundle-size` invoking `node scripts/check-bundle-size.mjs` post-`pnpm build`, and `lighthouse` invoking `pnpm exec lhci autorun`; triggered on `pull_request` + `push: branches: [main]`; concurrency `lighthouse-${{ github.ref }}` with `cancel-in-progress: true`); created `scripts/check-bundle-size.mjs` (Node ES-module script; gzip-compressed per-route first-load JS; 250KB ceiling per `docs/PERF_BUDGETS.md`); created `.lighthouserc.cjs` at repo root (audited routes: `/login`, `/`, `/offline`, `/_not-found`; `numberOfRuns: 3`; assertions mirror PERF_BUDGETS.md verbatim — LCP <= 2.5s, FCP <= 1.8s, CLS <= 0.1, TTFB <= 0.8s as errors; INP <= 200ms as warn; `categories:performance >= 0.9`). Added `@next/bundle-analyzer@^16.2.6` (lockstep with `next@^16.2.6`) + `@lhci/cli@^0.15.1` (npm `latest` dist-tag at execute time) to `frontend/package.json` devDependencies; added `"build:analyze": "ANALYZE=true next build --webpack"` script; regenerated `frontend/pnpm-lock.yaml` cleanly (zero peer warnings); wrapped `frontend/next.config.ts` export with `withBundleAnalyzer({ enabled: process.env.ANALYZE === "true" })` (no-op unless `ANALYZE=true pnpm build:analyze`). Bundler stays on webpack via Phase 48's `--webpack` flag. Local gates green: `pnpm typecheck`, `pnpm lint` (0 errors, 3 pre-existing warnings), `pnpm test` 351/351 across 64 files, `pnpm build --webpack` compiled successfully with 18 static pages, `node scripts/check-bundle-size.mjs` exit 0 with all 20 routes under the 250KB ceiling (largest `/` at 243.7KB; root-main shared 122.6KB). CI re-runs the gates as a backstop. Concludes the last open carry-forward debt scheduled for v1.1; only i-3 (audit reference) and i-7b (Testcontainers 2.x) remain open.

## Session Continuity

Last session: 2026-05-09 - v1.1 Phase 49-01 (lighthouse-ci) shipped via the orchestrator executor.
Stopped at: Phase 49 Plan 01 landed atomically across four commits per task: (1) `.lighthouserc.cjs` create, (2) `scripts/check-bundle-size.mjs` create, (3) `.github/workflows/lighthouse.yml` create, (4) `frontend/package.json` + `frontend/pnpm-lock.yaml` + `frontend/next.config.ts` (deps + script + bundle-analyzer wrap). Then i-15 closure in ISSUES.md + STATE.md bookkeeping. Local frontend gates green (typecheck clean, lint 0 errors, test 351/351 across 64 files, build --webpack compiled successfully with 18 static pages, node scripts/check-bundle-size.mjs exit 0 with all 20 routes under the 250KB ceiling). Next action: push commits + monitor GitHub Actions runs (existing ci.yml jobs + the two new lighthouse.yml jobs); on green, plan Phase 50 (release-v1-1) via `/gsd:plan-phase 50`.
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
