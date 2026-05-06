---
phase: 19-api-contract-docs
plan: 01
subsystem: docs, common/config, common/web
tags: [audit, openapi, springdoc, jwt-bearer, forward-auth, actuator-exclusion, contract-alignment, planning]

requires:
  - phase: 18-export-refinement
    plan: 04
    reason: Phase 18 close fixed the post-FullExportDto wire shape (SetRow.isPr 9th component, round-trip drift section); Phase 19 inherits exactly and references docs/EXPORT_FORMAT.md from the new docs/API.md
  - phase: 17-body-metrics
    plan: 04
    reason: MetricsService.UpsertResult 200/201 split must be reflected accurately on the OpenAPI document for the POST /api/metrics endpoint
  - phase: 15-sessions-core
    plan: 04
    reason: ApiError.code typed envelope is the project-wide error response shape; the OpenAPI ApiError schema must list the catalogued codes
  - phase: v0.3 (Phase 5)
    reason: ForwardAuthFilter + AUTH_MODE=forward-auth was landed in v0.3; the OpenAPI document declares the forward-auth security scheme based on this filter

provides:
  - REST surface inventory (31 controllers, 134 mappings, auth tier breakdown 5 admin / 3 operator / 1 anonymous-mixed / 22 user-JWT, two namespace collisions flagged)
  - current docs/API.md placeholder state (3 lines, stale by 2 milestones) and docs/EXPORT_FORMAT.md reference role
  - SpringDoc 2.x artifact verdict (Option A1 starter-webmvc-ui) and conservative version pin (2.6.0; plan 19-02 must verify via WebFetch)
  - integration shape (zero-touch defaults + 6 application.yml keys recommended, paths-to-match=/api/**)
  - two security schemes (bearerAuth http/bearer/JWT global + forwardAuth apiKey/header alternative)
  - actuator-and-management exclusion verdict (springdoc.paths-to-match = /api/**)
  - docs/API.md rewrite shape (Direction C2 navigational README; static export deferred to Phase 42)
  - Swagger UI exposure stance (Direction D1 bundled; BIND_ADDR=127.0.0.1 default is the trust boundary)
  - ApiError.code typed-values catalog (4 codes from Phase 15-04; 8 ConflictException sites without typed code documented as drift risk)
  - tag strategy (Option B2 per-controller @Tag, 28 mechanical edits)
  - SecurityConfig permitAll path additions for OpenAPI endpoints (/v3/api-docs/**, /swagger-ui/**, /swagger-ui.html)
  - integration test plan (5 tests in new OpenApiSurfaceIntegrationTest)
  - protected-file warning on backend/pom.xml for plan 19-02 dependency add (halt-and-ask gate)
  - explicit plan boundaries (19-02 through 19-05) with one-line objectives

affects:
  - 19-api-contract-docs/19-02 (springdoc-integration: pom.xml PROTECTED + OpenApiConfig + application.yml + SecurityConfig)
  - 19-api-contract-docs/19-03 (tag-and-collision-fix: 28 @Tag annotations + 1-2 @Operation operationId)
  - 19-api-contract-docs/19-04 (error-schema-and-test: ApiError @Schema + OpenApiSurfaceIntegrationTest)
  - 19-api-contract-docs/19-05 (docs-api-md-rewrite: full docs/API.md replacement per Direction C2)
  - 20-release-v0-4 (CHANGELOG.md user-visible entry: "OpenAPI document at /v3/api-docs; Swagger UI at /swagger-ui.html"; docs/MIGRATION.md "no migration steps" entry)
  - ISSUES.md (new candidates surfaced: i-NEW-A ApiError.code drift, i-NEW-B SpringDoc + Boot 3.5 first-integration risk, i-NEW-C dual SessionsController collision; opening ISSUES.md edits owned by plan 19-02+)

tech-stack:
  added: ["org.springdoc:springdoc-openapi-starter-webmvc-ui (Option A1; conservative pin 2.6.0 pending plan 19-02 WebFetch verification)"]
  patterns: ["@Tag per-controller (Option B2)", "@Operation operationId disambiguation for dual-class collisions"]

key-files:
  created:
    - .planning/phases/19-api-contract-docs/19-01-AUDIT.md
  modified: []

key-decisions:
  - "SpringDoc artifact: Option A1 starter-webmvc-ui (UI bundled; A2 api-only kept as fallback if Direction D1 is overturned)"
  - "SpringDoc version pin: conservative 2.6.0 fallback; plan 19-02 WebFetch-verifies against current 3.5.x compatibility"
  - "docs/API.md rewrite shape: Direction C2 (navigational README linking to runtime + per-section docs); C1 hand-written and C3 hybrid rejected"
  - "Swagger UI exposure stance: Direction D1 (bundled, BIND_ADDR=127.0.0.1 is the trust boundary); D2 env-flag and D3 JSON-only rejected"
  - "Tag strategy: Option B2 per-controller @Tag (28 mechanical single-line edits); B1 auto-tag and B3 group-configs rejected"
  - "Build wiring: Option E1 (runtime-only via /v3/api-docs); E2 maven-plugin static export and E3 CI tag-driven export deferred to Phase 42"
  - "ApiError.code curation: hand-write @Schema(allowableValues=...) on ApiError.code in plan 19-04; ApiErrorCode Java enum extraction deferred to Phase 42"
  - "Plan-count for Phase 19 implementation: 4 plans (19-02 springdoc-integration, 19-03 tag-and-collision-fix, 19-04 error-schema-and-test, 19-05 docs-api-md-rewrite)"
  - "Protected-file gate on backend/pom.xml: plan 19-02 executor must halt and ask the user before adding the SpringDoc dependency"
---

# Phase 19 Plan 01: api-contract-docs audit Summary

The audit produces the single source of truth for Phase 19 plan-02+ scope: 31 controllers + 134 mappings catalogued with auth tier and namespace collisions flagged, SpringDoc Option A1 starter-webmvc-ui verdict at conservative pin 2.6.0, two OpenAPI 3.x security schemes (bearerAuth global + forwardAuth alternative), Direction C2 navigational `docs/API.md` rewrite, Direction D1 Swagger UI bundled with BIND_ADDR-as-trust-boundary, Option B2 per-controller `@Tag` (28 edits), and explicit 4-plan implementation boundary (19-02 through 19-05) with the protected-file halt-and-ask gate on `backend/pom.xml` surfaced for plan 19-02.

## Accomplishments

- REST surface inventory complete: 31 controllers across 22 feature packages, 134 mapping annotations, auth tier breakdown (5 admin / 3 operator / 1 mixed-permitAll / 22 user-JWT). Two collisions flagged: dual `SessionsController` (`com.workouthub.sessions` vs `com.workouthub.users`) requires `@Operation(operationId=...)` disambiguation, and `MetricsController` / `PrometheusMetricsController` / `/actuator/prometheus` namespace overlap is solved by `springdoc.paths-to-match=/api/**`.
- Auth filter wiring catalogued: `SecurityConfig.java` line-by-line (CSRF disabled, stateless session, permitAll matchers, JWT filter chain with optional `ForwardAuthFilter` mounted before JWT iff `app.auth.mode=forward-auth`). `JwtAuthenticationFilter` and `ForwardAuthFilter` source-verified.
- Error envelope confirmed: `ApiError(int status, String error, String message, String path, String code)` is the project-wide response shape via `@RestControllerAdvice` on `GlobalExceptionHandler`. Only `ConflictException` populates `code` today; the other handlers leave it null (Jackson `NON_NULL` strips it from the wire).
- Documentation state: `docs/API.md` is a 3-line Phase-1-era placeholder (stale by 2 milestones); `docs/EXPORT_FORMAT.md` is post-18-04 and is THE export wire-shape reference (Phase 19 links rather than re-documents); contract section 7 sub-sections (7.1 built-in JWT, 7.2 forward-auth, 7.3 sessions) cited with line ranges; `application.yml` env-var prefix convention (`APP_AUTH_MODE` vs contract bare `AUTH_MODE`) confirmed as project-specific and consistent with `APP_JWT_SECRET` precedent.
- SpringDoc artifact verdict: Option A1 (`org.springdoc:springdoc-openapi-starter-webmvc-ui`); A2 (api-only) kept as fallback if Direction D1 is overturned; A3 (manual core) rejected as non-idiomatic.
- SpringDoc version pin: conservative 2.6.0 fallback (WebFetch unavailable in this audit's read-only environment); plan 19-02 must verify against `springdoc.org` and the GitHub releases page before merging the `pom.xml` edit. Pin via `<springdoc.version>` `<properties>` entry mirroring `<jjwt.version>` precedent.
- Integration shape: 4 SpringDoc auto-configured paths (`/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui.html`, `/swagger-ui/**`); 6 `application.yml` keys recommended (`paths-to-match=/api/**`, default api-docs/swagger-ui paths, `disable-swagger-default-url=true`, `tags-sorter=alpha`, `operations-sorter=method`).
- Two security schemes declared with concrete OpenAPI 3.x shape: `bearerAuth` (http/bearer/JWT) applies globally; `forwardAuth` (apiKey/header `X-Forwarded-Email`) is an alternative scheme. Source-IP trust gate (`TRUSTED_PROXIES`) is explicitly un-modelable in OpenAPI; documented prose-side in `docs/API.md` cross-link to contract 7.2.
- Actuator-and-management exclusion verdict: `springdoc.paths-to-match=/api/**` suppresses `/actuator/**`, `/livez`, `/healthz`, `/metrics`, `/error` from the public surface. Operator paths remain documented in `docs/OBSERVABILITY.md` (cross-link from `docs/API.md`).
- `docs/API.md` rewrite shape: Direction C2 (navigational README linking to runtime endpoints + per-section docs); C1 hand-written reference rejected (134-endpoint maintenance burden); C3 hybrid rejected (doc duplication). Static `docs/openapi.json` export deferred to Phase 42.
- Swagger UI exposure stance: Direction D1 (bundled in production); D2 env-flag opt-in and D3 JSON-only rejected. Operator-discoverability risk is mitigated by the contract's `BIND_ADDR=127.0.0.1` default; trust boundary documented in `docs/API.md` "Discoverability" sub-section.
- `ApiError.code` typed-values catalog: 4 codes confirmed from Phase 15-04 (`SESSION_ALREADY_ACTIVE`, `SESSION_ALREADY_FINISHED`, `SESSION_FINISHED`, `SET_NUMBER_DUPLICATE`); 8 `ConflictException` throw sites without typed `code` flagged as drift risk (i-NEW-A) and mitigated by future `ApiErrorCode` enum extraction (Phase 42).
- Tag strategy verdict: Option B2 (per-controller `@Tag`); B1 auto-tag rejected for ugly `*Controller` suffix; B3 group-configs rejected for granularity loss. 28 single-line edits + 3 operator-controller exclusions (excluded from `paths-to-match=/api/**`).
- SecurityConfig permitAll additions: 3 path patterns (`/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`) added to the existing permitAll matcher at `SecurityConfig.java:43-45`.
- Build wiring: Option E1 (runtime-only via `/v3/api-docs`) for v0.4; Option E2 (maven plugin static export) and E3 (CI tag-driven export) deferred to Phase 42. No `pom.xml` plugin block edit beyond the single dependency add.
- Test strategy: 5 integration tests in new `OpenApiSurfaceIntegrationTest` (~80 lines): JSON reachable unauthenticated, paths contain `/api/**` and exclude actuator, both security schemes declared, Swagger UI reachable, `ApiError.code` enum mirrors GlobalExceptionHandler.
- Protected-file gate surfaced: `backend/pom.xml` is protected per `CLAUDE.md`; plan 19-02 executor MUST halt and ask the user before adding the SpringDoc dependency. The plan preamble must state the protection explicitly.
- Plan-count recommendation: 4 plans for Phase 19 implementation (19-02 springdoc-integration, 19-03 tag-and-collision-fix, 19-04 error-schema-and-test, 19-05 docs-api-md-rewrite) with explicit file-touch boundaries and dependency arrows between plans.

## Files Created/Modified

- `.planning/phases/19-api-contract-docs/19-01-AUDIT.md` (728 lines, 6 sections + frontmatter) - audit deliverable.
- `.planning/phases/19-api-contract-docs/19-01-PLAN.md` - committed alongside Task 1 (was untracked from a prior planner spawn that hit a usage limit before commit).

## Decisions Made

- SpringDoc artifact: Option A1 starter-webmvc-ui.
- SpringDoc version pin: conservative 2.6.0 fallback; plan 19-02 verifies via WebFetch.
- `docs/API.md` rewrite shape: Direction C2 (navigational README).
- Swagger UI exposure stance: Direction D1 (bundled, BIND_ADDR is trust boundary).
- Tag strategy: Option B2 (per-controller @Tag).
- Build wiring: Option E1 (runtime-only); static export deferred to Phase 42.
- `ApiError.code` curation: hand-write `@Schema.allowableValues` in plan 19-04; enum extraction deferred to Phase 42.
- Phase 19 plan boundaries: 4 plans (19-02 through 19-05).
- Protected-file gate on `backend/pom.xml`: plan 19-02 must halt and ask before edit.

## Issues Encountered

- NEW issue candidate i-NEW-A: `ApiError.code` typed-value drift. 8 `ConflictException` throw sites today emit `code: null`. Plan 19-04 hand-curates 4 typed codes in `@Schema.allowableValues`; if v0.5+ adds new typed codes without updating the annotation, the OpenAPI document silently drifts. Mitigation: extract `ApiErrorCode` enum (Phase 42 candidate).
- NEW issue candidate i-NEW-B: SpringDoc + Spring Boot 3.5.x first-integration risk. Local Maven gap (`local_maven_gap` memory) means the dependency add is CI-tested only. Mitigation: `OpenApiSurfaceIntegrationTest` hits `/v3/api-docs` and asserts non-empty `paths` map + at least one `/api/**` entry; failure surfaces incompatibility immediately on push.
- NEW issue candidate i-NEW-C: dual `SessionsController` operationId collision. Captured by plan 19-03 (resolved-by-plan, not deferred).
- WebFetch unavailable in this audit's read-only environment; conservative pin 2.6.0 used as fallback. Plan 19-02 executor must verify against `springdoc.org` + GitHub releases before merging the `pom.xml` edit.
- Resume note: prior executor hit Anthropic usage limit mid-run after writing AUDIT.md to disk but before committing. This resume executor committed the AUDIT.md across three task-aligned commits (19ba373 task 1, 529fa58 task 2, cdc215b task 3) followed by the SUMMARY.md commit, mirroring the Phase 18-01 commit cadence.

## Next Phase Readiness

- Ready for Phase 19 plan-02+ planning: yes.
- Open questions for plan 19-02+: confirm SpringDoc version pin via WebFetch at execution time (current 2.6.0 is the conservative fallback); confirm `pom.xml` edit authorization at the protected-file halt gate before adding the `<dependency>` block; confirm whether `<springdoc.version>` `<properties>` entry mirrors `<jjwt.version>` precedent or is hard-coded inline.
- Open ISSUES.md edits: i-NEW-A (ApiError.code drift), i-NEW-B (first-integration CI risk), i-NEW-C (dual SessionsController collision). This audit only flags them as candidates per the plan's "out of scope: opening ISSUES.md edits".
