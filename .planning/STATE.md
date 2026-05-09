# State

## Current Position

Milestone: v1.1 Deferred Debt Closure (CLOSED 2026-05-09)
Phase: 50 of 50 (release-v1-1) - SHIPPED 2026-05-09
Plan: 50-01 release-v1-1 - shipped 2026-05-09 (v1.1.0 cut at sha `53082ab`; annotated tag `v1.1.0` created locally; multi-arch GHCR publish queued for maintainer-side tag push per `IDENTITY.yaml#release.draft_first: true`)
Status: v1.1 milestone CLOSED. All planned phases (45-50) shipped; all four open carry-forward issues from v1.0 closed in this milestone (i-13 Phase 45 springdoc-2.7-bump, i-8b Phase 47 spring-boot-4-with-jackson-3, i-6b Phase 48 next-16, i-15 Phase 49 lighthouse-ci). Phase 46 deferred-into-47 per the HEAD audit that found four blockers preventing a Spring Boot 3.5-compatible Jackson 3 source-only intermediate state. v1.1.0 cut at sha `53082ab` on 2026-05-09 with annotated tag `v1.1.0` created locally; multi-arch GHCR publish via `.github/workflows/release.yml` is queued for maintainer-side tag push. Project-complete state restored: no active milestone. Two issues remain open as carry-forward (i-3 audit reference; i-7b Testcontainers 2.x trigger blocked on upstream Maven Central GA).
Last activity: 2026-05-09 - v1.1 milestone bookkeeping commit (collapse Phases 45-50 into ROADMAP archive block; archive v1.1 to `.planning/milestones/v1.1-ROADMAP.md`; record v1.1 shipped entry in MILESTONES.md; update STATE.md cursor to project-complete).

Progress: v1.1 ####################   100% (6/6 plans complete; 6/6 phases shipped; v1.1.0 cut at 53082ab)

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/milestones/v0.4-ROADMAP.md` for full v0.4 archive
- See: `.planning/milestones/v0.5-ROADMAP.md` for full v0.5 archive
- See: `.planning/milestones/v0.6-ROADMAP.md` for full v0.6 archive
- See: `.planning/milestones/v1.0-ROADMAP.md` for full v1.0 archive
- See: `.planning/milestones/v1.1-ROADMAP.md` for full v1.1 archive
- See: `.planning/ROADMAP.md` for the archived roadmap (all milestones v0.3 through v1.1 shipped and archived)
- See: `.planning/ISSUES.md` for open deferred issues (i-3, i-7b open; i-1, i-2, i-4, i-5, i-6, i-6b, i-7, i-8, i-8b, i-9, i-10, i-12, i-13, i-14, i-15 closed)

**Core value:** A user can log a workout end-to-end on a phone (mid-set), see prior performance for each exercise, and export the full history as a JSON snapshot Claude can ingest as context. Offline-first execution and self-hosted data ownership are non-negotiable.
**Current focus:** No active milestone. v1.1 closed all four open carry-forward issues from v1.0 (i-13 SpringDoc 2.7+, i-8b Spring Boot 4 + Jackson 3, i-6b Next 16, i-15 Lighthouse CI + bundle-size + bundle-analyzer) and cut v1.1.0 at sha `53082ab`. Two issues remain carried-forward as audit reference / upstream-gated: i-3 (`.gitignore` `data/` glob audit reference; documentation note only) and i-7b (Testcontainers 2.0.0 jump; trigger blocked on upstream Maven Central GA). v1.2+ scope (if opened) would address authenticated-route Lighthouse coverage (currently deferred because it requires CI-side backend boot) and INP CI-gating (currently `warn` because Lighthouse's headless audit cannot reliably measure interaction-to-next-paint).

## Accumulated Context

### Locked-in Decisions (carried from v0.3 + v0.4 + v0.5 + v0.6 + v1.0 + v1.1)

Full decision logs live in `.planning/milestones/v0.3-ROADMAP.md`, `.planning/milestones/v0.4-ROADMAP.md`, `.planning/milestones/v0.5-ROADMAP.md`, `.planning/milestones/v0.6-ROADMAP.md`, `.planning/milestones/v1.0-ROADMAP.md`, and `.planning/milestones/v1.1-ROADMAP.md` "Key Decisions" sections. The most operationally relevant ones for v1.2+ and beyond:

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
- SpringDoc artifact (v1.1 Phase 45 lock-in then Phase 47 lock-in): `2.8.17` starter-webmvc-ui under Spring Boot 3.5.14, then `3.0.3` after the Phase 47 SB4 jump. The `ControllerAdviceBean(Object)` workaround was removed when 2.7+ landed (`apiErrorSchemaCustomizer` `@Bean` and `springdoc.override-with-generic-response: false` both deleted). Schemas surface via the framework default path; `OpenApiSurfaceIntegrationTest` (5 tests) is the regression gate.
- Frontend offline-first: Dexie IndexedDB queue scoped to session-execution; drains on `online` event via `clientSetId` UUID idempotency key (Phase 25 contract).
- Frontend route protection: `frontend/src/proxy.ts` (renamed from `middleware.ts` in v1.1 Phase 48) enumerates 12 (app)-group segments in both `PROTECTED_PREFIXES` (runtime) and `config.matcher` (compile-time) arrays; the proxy test scaffold (originally `middleware.test.ts` from Phase 29.5; renamed to `proxy.test.ts` in Phase 48) gates future drift.
- Zod schema parity: backend DTO surfaces (ImportResult, FullExport, vapid, push subscribe, rest-timer schedule request/response) round-trip via shared Zod schemas in the frontend (Phase 25.5 + Phase 34); future endpoint additions should mirror this pattern.
- Insights surface composition (v0.6 lock-in): `/insights` is the canonical "Detayli insight sayfasi" deliverable from ProjectBrief Phase 6, NOT a separate `/stats` route. Splitting `insights-client.tsx` into per-card files is deferred to v1.2+ polish.
- Service-worker cache versioning (v0.6 lock-in): bump the `CACHE` constant on every behaviour-affecting SW change. v0.6 cycle moved v1 -> v2 (Phase 32) -> v3 (Phase 33). The activate handler already deletes any cache name that does not match the current `CACHE`.
- Web Push schedule contract (v0.6 lock-in): server-scheduled rest-timer push is the primary path because service workers cannot persist a `setTimeout` across termination and `TimestampTrigger` is Chromium-only behind a flag. Cancel + re-schedule is the lifecycle contract; no edit / reschedule / snooze.
- Frontend metrics dependency posture (v0.6 lock-in): `prom-client` and `@opentelemetry/sdk-metrics` rejected because `frontend/package.json` is protected. Hand-rolled in-memory registry under single-instance Self-Hosted Contract is the load-bearing decision.
- Web Vitals reporter dependency posture (v0.6 lock-in): framework-native `next/web-vitals` re-export from the `next` package is used (no `web-vitals` direct dep). Mounted once in the root layout.
- Perf-budget gating posture (v0.6 lock-in then v1.1 Phase 49 lock-in): flipped from "doc-only" (v0.6 Phase 36-03 ship of `docs/PERF_BUDGETS.md`) to "CI-gated" via `.github/workflows/lighthouse.yml` running `pnpm exec lhci autorun` against the just-built frontend on `pull_request` + `push: branches: [main]`. The `.lighthouserc.cjs` config at repo root mirrors `docs/PERF_BUDGETS.md` verbatim — LCP <= 2.5s, FCP <= 1.8s, CLS <= 0.1, TTFB <= 0.8s gated as `error`; INP <= 200ms gated as `warn` (Lighthouse's headless audit cannot reliably measure interaction-to-next-paint without real user interaction); `categories:performance >= 0.9` gated as `error`. `scripts/check-bundle-size.mjs` computes per-route gzip-compressed first-load JS size from the webpack-emitted `.next/build-manifest.json` + `.next/app-path-routes-manifest.json` + walked `.next/static/chunks/app/<route>/`, fails if any route exceeds 250KB. Audited Lighthouse routes: `/login`, `/`, `/offline`, `/_not-found` (unauthenticated set only; authenticated-route coverage deferred to v1.2+ because it requires a CI-side backend boot). `@next/bundle-analyzer` (`^16.2.6` lockstep with `next`) wraps `frontend/next.config.ts` with `enabled: process.env.ANALYZE === "true"` (no-op unless `ANALYZE=true pnpm build:analyze` for operator-side investigation; not gated). `@lhci/cli` (`^0.15.1`) is the Lighthouse runner. Bundler stays on webpack via Phase 48's `--webpack` flag (`scripts.build:analyze` also passes `--webpack`); a future Turbopack-as-default migration would update the bundle-size script to read `.next/app-build-manifest.json` instead.
- Refresh-token hashing posture (v1.0 Phase 41 lock-in): SHA-256 hex over the full JWT bytes via `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(...)`, persisted in `refresh_tokens.token_hash VARCHAR(128) UNIQUE` (V7 migration). The 2026-05-04 `pending_ci_fixes.md` "refresh-token hash collisions" entry was superseded — those tests pass on `96d81b0`. No source change needed; tightening to bcrypt/Argon2 was rejected because refresh tokens carry full JWT entropy at issuance and the hash is a fingerprint for DB lookup, not a password derivation.
- Brute-force lockout posture (v1.0 Phase 41 lock-in): `BruteForceGuard` enforces 10 failures / 15-minute window / 60-minute lockout, throws HTTP 423 via `ResponseStatusException(HttpStatus.LOCKED, ...)`, records every login outcome in `login_attempts` (V15 migration) via `@Transactional(propagation = REQUIRES_NEW)`. `BruteForceLockoutIntegrationTest` covers under-threshold pass, at-threshold lock, post-cooldown unlock — all green on origin/main. Threshold-tightening + (email, IP)-keyed lockout were rejected as v1.0 scope creep.
- Rate-limiting posture (v1.0 Phase 41 lock-in): zero generic in-process rate limiter; the Self-Hosted Contract delegates rate limiting to the operator's reverse proxy. The only in-process throttle is `BruteForceGuard` (per-email login throttle). Adding a generic rate limiter was rejected because it would duplicate operator-layer enforcement and contradict the contract's "never assume public exposure" posture (tailnet-only deployment is the v0.3 lock-in).
- Jackson 3 migration posture (v1.1 Phase 47 lock-in): shipped in Phase 47-01. The 36-file Jackson 2 -> Jackson 3 (`com.fasterxml.jackson.*` -> `tools.jackson.*`) rewrite landed in a single CI-validated commit alongside the `<spring-boot-starter-parent>` 3.5.14 -> 4.0.6 bump and the `<springdoc.version>` 2.8.17 -> 3.0.3 bump. Path B (full Jackson 3 migration) selected; Path A (`spring-boot-jackson2` compat module) foreclosed. JacksonConfig declares a Jackson 3 `JsonMapperBuilderCustomizer @Bean` from `org.springframework.boot.jackson.autoconfigure` that registers `tools.jackson.datatype.jsr310.JavaTimeModule`, disables `tools.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`, and sets `tools.jackson.annotation.JsonInclude.Include.NON_NULL`. `AuditLogService` + `WebPushNotificationDispatcher` catch `tools.jackson.core.JacksonException` (Jackson 3 unified exception hierarchy; replaces `JsonProcessingException`). PrometheusMetricsController actuator imports flipped from `org.springframework.boot.actuate.metrics.export.prometheus.*` to `org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.*` (Spring Boot 4 per-feature autoconfigure module split).
- Spring Boot 4 BOM transitive lock-ins (v1.1 Phase 47-01): Spring Framework 7.0.7 / Spring Security 7.0.5 / Hibernate 7.2.12.Final / Jakarta EE 11 (Servlet 6.1.0, Persistence 3.2.0, Validation 3.1.1, Annotation 3.0) / Tomcat 11 / Micrometer 1.15.x / Netty 4.2.13.Final (override preserved across the bump; the SB4 BOM default of 4.2.12.Final is overridden to retain CVE-2026-42577 patch posture per i-14) / Jackson 3.0.x / SpringDoc 3.0.3 (bumped from 2.8.17 alongside the SB4 jump per Phase 45's i-13 closure note that explicitly sequenced SpringDoc 3.x for Phase 47).
- Next.js runtime line (v1.1 Phase 48 lock-in): `^16.2.6` pinned in `frontend/package.json` (latest stable on the npm `latest` dist-tag at execute time). The Middleware -> Proxy rename landed (`frontend/src/middleware.ts` -> `frontend/src/proxy.ts`; function `middleware` -> `proxy`; companion test renamed in lockstep). The `<Suspense>` boundary around the `useSearchParams`-reading body of `frontend/src/app/(auth)/login/page.tsx` (Path B: single-file inner `LoginForm` function with inline skeleton fallback) closes the Next 16 hard-error gate. `engines.node` bumped to `>=20.9.0`. Bundler stayed on webpack via `--webpack` opt-out flag for the v1.1 ship; Turbopack-as-default migration deferred as a separate concern. Codebase has zero `next/image` callers (`frontend/src/components/exercise-media.tsx` uses raw `<img>` intentionally); framework default `images.localPatterns` enforcement is moot. `next.config.ts` untouched (no `images:`, `experimental:`, `webpack:`, or `turbo:` block needed except the Phase 49 `withBundleAnalyzer` outer wrap). next-intl 4.11.1 (Phase 40-03 lock-in) peers cleanly against `next@^16`; install is clean with zero `pnpm.overrides` or `--strict-peer-dependencies=false` hacks. eslint-config-next@16 ships flat config natively; `frontend/eslint.config.mjs` migrated from `FlatCompat` shim to direct imports of `eslint-config-next/core-web-vitals` and `/typescript`. eslint-plugin-react-hooks@7 (transitive via eslint-config-next@16) introduced two new rules: `set-state-in-effect` (6 targeted disables with platform-API rationale) and `static-components` (refactored exercise-progress-chart's `TooltipContent` to a `renderTooltip` render-prop function). The Phase 35 metric-recording contract and the Phase 29.5 matcher-coverage contract both survive the rename intact (the renamed `proxy.test.ts` 11 cases assert this).

### v1.1 Findings (archived)

The v1.1 cycle delivered six phases (45-50) across 6 plans (Phase 46 deferred-into-47) on 2026-05-09 inside a single working session. Per-phase findings (SpringDoc 2.6 -> 2.8.17 with the `ControllerAdviceBean` workaround removed; Phase 46 deferred-into-47 after the HEAD audit found four blockers preventing a Spring Boot 3.5-compatible Jackson 3 source-only intermediate state; Spring Boot 3.5.14 -> 4.0.6 with the consolidated 36-file Jackson 2 -> Jackson 3 rewrite + SpringDoc 2.8.17 -> 3.0.3 + Prometheus-actuator package relocation + Netty 4.2.13.Final pin preservation; Next 15.1 -> 16.2.6 with `middleware.ts` -> `proxy.ts` rename + login Suspense wrap + eslint flat-config migration + react-hooks@7 rule accommodations; Lighthouse CI workflow + bundle-size gate script + lighthouserc + bundle-analyzer/lhci devDeps; v1.1.0 release cut at sha `53082ab` with annotated tag `v1.1.0`) are consolidated in `.planning/milestones/v1.1-ROADMAP.md`. Each phase plan's source-of-truth artifact stays under `.planning/phases/45-*` through `.planning/phases/49-*`. SUMMARY.md files were not produced by the plan executors; the accomplishment record lives in `CHANGELOG.md` `## [1.1.0]`, the per-plan PLAN.md output blocks, and the git commit messages (the v1.1-ROADMAP.md archive consolidates all three).

### Issue-to-Phase Mapping (carried forward)

- i-3: documentation note on `.gitignore` `data/` glob-form correction. Stays carried as audit reference. **OPEN at v1.1 close.**
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-5: closed by v1.0 Phase 40 Plan 03 (framework-majors / next-intl 4).
- i-6: closed by v1.0 Phase 40 Plan 02 (framework-majors) re-defer playbook (superseded by i-6b).
- i-6b: closed by v1.1 Phase 48 Plan 01 (next-16). Bumped `next` from `^15.1.0` to `^16.2.6`; renamed `middleware.ts` -> `proxy.ts`; wrapped login page in `<Suspense>`; bumped `engines.node` to `>=20.9.0`; eslint flat-config migration; react-hooks@7 rule accommodations.
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-7b: residual 2.x major bump, deferred until upstream `org.testcontainers:testcontainers:2.0.0` GA on Maven Central. **OPEN at v1.1 close.**
- i-8: closed by v1.0 Phase 40 Plan 01 (framework-majors) re-defer playbook (superseded by i-8b).
- i-8b: closed by v1.1 Phase 47 Plan 01 (spring-boot-4-with-jackson-3). Bumped `<spring-boot-starter-parent>` from `3.5.14` to `4.0.6` and `<springdoc.version>` from `2.8.17` to `3.0.3`; preserved `<netty.version>4.2.13.Final</netty.version>` property override; migrated 36 Jackson 2 source files (`com.fasterxml.jackson.*` -> `tools.jackson.*`); renamed `JsonProcessingException` catch sites to `JacksonException`; flipped `PrometheusMetricsController` actuator imports per the Spring Boot 4 per-feature autoconfigure module split. Path B (full Jackson 3 migration) shipped; Path A foreclosed per Phase 46-01 lock-in.
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-13: closed by v1.1 Phase 45 Plan 01 (springdoc-2.7-bump). Bumped `<springdoc.version>` from `2.6.0` to `2.8.17`; deleted the `apiErrorSchemaCustomizer` workaround from `OpenApiConfig.java`; deleted the `springdoc.override-with-generic-response: false` opt-out from `application.yml`.
- i-14: closed by v1.0 Phase 41 Plan 01 (security-hardening). Bumped `<netty.version>` from `4.1.133.Final` to `4.2.13.Final`; removed `CVE-2026-42577` suppression from `.trivyignore`.
- i-15: closed by v1.1 Phase 49 Plan 01 (lighthouse-ci). Created `.github/workflows/lighthouse.yml` + `scripts/check-bundle-size.mjs` + `.lighthouserc.cjs`; added `@next/bundle-analyzer@^16.2.6` + `@lhci/cli@^0.15.1` to `frontend/package.json` devDependencies; added `build:analyze` script + `withBundleAnalyzer` wrap around `next.config.ts`. Concludes the last open carry-forward debt scheduled for v1.1.

## Session Continuity

Last session: 2026-05-09 - v1.1 milestone close bookkeeping (this commit).
Stopped at: v1.1.0 cut at sha `53082ab` on 2026-05-09 with annotated tag `v1.1.0` created locally. The milestone close bookkeeping commit lands on top of the release commit: collapses Phases 45-50 into a `<details>` archive block in `.planning/ROADMAP.md` (mirrors v0.4 / v0.5 / v0.6 / v1.0 pattern); archives v1.1 to `.planning/milestones/v1.1-ROADMAP.md` (mirrors the v1.0 archive structure); records the v1.1 shipped entry in `.planning/MILESTONES.md`; updates this STATE.md cursor to project-complete (no active milestone). Two issues remain open as carry-forward (i-3 audit reference; i-7b Testcontainers 2.x trigger blocked on upstream Maven Central GA). Next action: maintainer-side tag push (`git push origin v1.1.0`) to fire `.github/workflows/release.yml` for the multi-arch GHCR publish at `ghcr.io/gtrows/workouthub-{backend,frontend}:1.1.0` per `IDENTITY.yaml#release.draft_first: true`. v1.2+ scope, if opened, would address authenticated-route Lighthouse coverage (currently deferred because it requires CI-side backend boot) and INP CI-gating (currently `warn` because Lighthouse's headless audit cannot reliably measure interaction-to-next-paint).
Resume file: None.

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/milestones/v0.4-ROADMAP.md` (full v0.4 archive)
- `.planning/milestones/v0.5-ROADMAP.md` (full v0.5 archive)
- `.planning/milestones/v0.6-ROADMAP.md` (full v0.6 archive)
- `.planning/milestones/v1.0-ROADMAP.md` (full v1.0 archive)
- `.planning/milestones/v1.1-ROADMAP.md` (full v1.1 archive)
- `.planning/ROADMAP.md` (archived; all milestones v0.3 through v1.1 shipped and archived)
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
