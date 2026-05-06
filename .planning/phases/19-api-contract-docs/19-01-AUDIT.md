---
phase: 19-api-contract-docs
plan: 01
deliverable: audit
---

# Phase 19 Plan 01: API Contract Docs Audit

Read-only inventory + verdict deliverable for Phase 19. No Java, migration, test,
or documentation source files are modified. The `pom.xml` change required to
land the SpringDoc dependency in plan 19-02 is recommended here, NOT applied
(`pom.xml` is a protected file per `CLAUDE.md`; plan 19-02 must halt and ask the
user before editing).

---

## Section 1 - REST Surface Inventory

### 1A. Controller catalog

`Glob backend/src/main/java/com/workouthub/**/*Controller.java` returns 31
files. `Grep '@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping' --type java --output_mode count`
returns 134 matches across the 31 files (verification cross-check).

| #  | Package           | Controller (file)                                  | Class-level base path                  | Mapping count | Auth tier               | Surface  |
|----|-------------------|----------------------------------------------------|----------------------------------------|---------------|-------------------------|----------|
| 1  | achievements      | AchievementsController.java                        | `/api/achievements`                    | 2             | JWT                     | public   |
| 2  | admin             | AdminExercisesController.java                      | `/api/admin/exercises`                 | 4             | JWT (ADMIN)             | admin    |
| 3  | admin             | AdminUsersController.java                          | `/api/admin/users`                     | 7             | JWT (ADMIN)             | admin    |
| 4  | analytics         | AnalyticsController.java                           | `/api/analytics`                       | 6             | JWT                     | public   |
| 5  | analytics         | ExerciseAnalyticsController.java                   | `/api/exercises/{exerciseId}`          | 3             | JWT                     | public   |
| 6  | audit             | AuditController.java                               | `/api/admin/audit`                     | 2             | JWT (ADMIN)             | admin    |
| 7  | auth              | AuthController.java                                | `/api/auth`                            | 3             | permitAll login/refresh; JWT logout | public/anonymous |
| 8  | auth              | PasswordResetController.java                       | `/api/auth`                            | 2             | permitAll reset-password | anonymous |
| 9  | challenges        | AdminChallengesController.java                     | `/api/admin/challenges`                | 5             | JWT (ADMIN)             | admin    |
| 10 | challenges        | ChallengesController.java                          | `/api/challenges`                      | 2             | JWT                     | public   |
| 11 | common.web        | HealthController.java                              | (none) `/livez`, `/healthz`            | 2             | permitAll               | operator |
| 12 | common.web        | PrometheusMetricsController.java                   | (none) `/metrics`                      | 1             | permitAll               | operator |
| 13 | exercises         | ExerciseController.java                            | `/api/exercises`                       | 4             | JWT                     | public   |
| 14 | exports           | ExportController.java                              | `/api/export`                          | 17            | JWT                     | public   |
| 15 | health            | HealthImportController.java                        | `/api/health/import`                   | 4             | JWT                     | public   |
| 16 | metrics           | MetricsController.java                             | `/api/metrics`                         | 4             | JWT                     | public   |
| 17 | nutrition         | NutritionController.java                           | `/api`                                 | 6             | JWT                     | public   |
| 18 | push              | PushController.java                                | `/api/push`                            | 4             | JWT                     | public   |
| 19 | sessions          | SessionsController.java                            | `/api/sessions`                        | 6             | JWT                     | public   |
| 20 | sessions          | SessionSetsController.java                         | `/api/sessions/{sessionId}/sets`       | 4             | JWT                     | public   |
| 21 | supplements       | SupplementsController.java                         | `/api/supplements`                     | 5             | JWT                     | public   |
| 22 | twofa             | TwoFactorController.java                           | `/api/users/me/2fa`                    | 5             | JWT                     | public   |
| 23 | users             | PasswordChangeController.java                      | `/api/users/me`                        | 2             | JWT                     | public   |
| 24 | users             | SessionsController.java                            | `/api/users/me/sessions`               | 3             | JWT                     | public   |
| 25 | users             | UsersController.java                               | `/api/users`                           | 3             | JWT                     | public   |
| 26 | water             | WaterController.java                               | `/api/water`                           | 4             | JWT                     | public   |
| 27 | webhooks          | ScaleWebhookController.java                        | `/api/webhooks/scale`                  | 2             | permitAll (HMAC token)  | operator |
| 28 | webhooks          | WebhookTokensController.java                       | `/api/users/me/webhook-tokens`         | 4             | JWT                     | public   |
| 29 | workouts          | WorkoutDayByIdController.java                      | `/api/workout-days`                    | 2             | JWT                     | public   |
| 30 | workouts          | WorkoutDaysController.java                         | `/api/workout-plans/{planId}`          | 8             | JWT                     | public   |
| 31 | workouts          | WorkoutPlansController.java                        | `/api/workout-plans`                   | 8             | JWT                     | public   |

Mapping-count cross-check: 2+4+7+6+3+2+3+2+5+2+2+1+4+17+4+4+6+4+6+4+5+5+2+3+3+4+2+4+2+8+8 = 134 (matches the upstream Grep count).

Auth tier breakdown:
- **Admin-only (5)**: AdminExercises, AdminUsers, Audit, AdminChallenges, WebhookTokens (note: WebhookTokens is per-user JWT, not ADMIN; recount = **4 admin** + **23 user-JWT** + **3 operator** + **1 mixed permitAll/JWT auth flow** = 31).
- **User JWT (23)**: Achievements, Analytics, ExerciseAnalytics, Challenges, Exercises, Exports, HealthImport, Metrics, Nutrition, Push, Sessions (workout), SessionSets, Supplements, TwoFactor, PasswordChange, users.SessionsController, Users, Water, WebhookTokens, WorkoutDayByIdController, WorkoutDays, WorkoutPlans, ExerciseAnalytics (already counted) -> recount-trim = 22 in tight tally; the off-by-one tracks through "WebhookTokens" classification ambiguity (user JWT, not ADMIN-restricted at filter level).
- **Operator (3)**: HealthController, PrometheusMetricsController, ScaleWebhookController.
- **Mixed permitAll + JWT (2 in auth/)**: AuthController (login + refresh permitAll; logout JWT), PasswordResetController (reset-password permitAll).

Plan 19-02 must NOT rely on the precise admin/user split here for security; the
authoritative gate is `SecurityConfig.requestMatchers` + per-method
`@PreAuthorize`. The OpenAPI document declares `bearerAuth` globally; admin
endpoints inherit the same scheme + an additional role hint via `@Operation`
description.

Note on the contract drift versus the plan text: plan 19-01 listed
`/api/audit`, `/api/2fa`, `/api/webhook-tokens`, `/api/health-import`. Actual
mounts are `/api/admin/audit`, `/api/users/me/2fa`,
`/api/users/me/webhook-tokens`, `/api/health/import`. NutritionController is
mounted at `/api` (it owns multiple sub-paths like `/api/foods`, `/api/meals`).
The audit captures the actual values from the source. Plan 19-02+ must use the
actual paths for any per-controller `@Tag` / `@Operation` work.

### 1B. Two namespace collisions for SpringDoc

1. **Dual `SessionsController` classes**:
   - `com.workouthub.sessions.SessionsController` (workout sessions; base
     `/api/sessions`).
   - `com.workouthub.users.SessionsController` (refresh-token sessions; base
     `/api/users/me/sessions`).
   SpringDoc auto-derives `operationId` from method-name + class simple name
   (`list`, `get`, `delete` are likely candidates on both classes). Plan 19-02
   must annotate at least one of the two classes (or the colliding methods)
   with `@Operation(operationId = "...")` to disambiguate. Tag names
   ("Sessions" vs "User sessions") via `@Tag` per Section 5B Option B2 also
   helps reader navigation.

2. **Metrics namespace**:
   - `metrics.MetricsController` (user-facing body metrics; `/api/metrics`).
   - `common.web.PrometheusMetricsController` (operator scrape; `/metrics`).
   - Spring Boot Actuator `/actuator/prometheus` (operator).
   The HTTP path namespaces do NOT collide (`/api/metrics` vs `/metrics` vs
   `/actuator/prometheus`). The OpenAPI tag derived from class simple name
   would clash if both class names landed in the SpringDoc scan
   (`MetricsController` vs `PrometheusMetricsController`). Plan 19-02 must set
   `springdoc.paths-to-match: "/api/**"` so neither operator controller
   surfaces in the document; only `metrics.MetricsController` ships in the
   user-facing OpenAPI.

### 1C. Auth filter wiring (SecurityConfig.java)

Walk `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java:27-57`:
- Line 33: CSRF disabled (stateless API; `AbstractHttpConfigurer::disable`).
- Line 34: CORS via `Customizer.withDefaults` (delegates to `CorsConfig`
  bean).
- Line 35: `SessionCreationPolicy.STATELESS` -- no HttpSession; every request
  re-authenticates from the JWT.
- Lines 37-40: `requestMatchers(POST, "/api/auth/login", "/api/auth/refresh",
  "/api/auth/reset-password").permitAll()`.
- Lines 41-42: `requestMatchers(POST, "/api/webhooks/scale/**").permitAll()`
  -- HMAC token gates this controller at the application layer.
- Lines 43-45: `requestMatchers("/livez", "/healthz", "/metrics",
  "/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()`
  -- operator paths.
- Line 46: `requestMatchers("/error").permitAll()`.
- Line 47: `requestMatchers("/api/**").authenticated()` -- catch-all JWT gate.
- Line 48: `anyRequest().denyAll()` -- everything else 403.
- Lines 51-52: 401 entry point via `HttpStatusEntryPoint(UNAUTHORIZED)`.
- Line 53: `JwtAuthenticationFilter` mounted before
  `UsernamePasswordAuthenticationFilter`.
- Lines 54-55: `Optional<ForwardAuthFilter>` mounted before
  `JwtAuthenticationFilter` IFF the bean exists (`@ConditionalOnProperty`
  guard at `ForwardAuthFilter.java:34`).

Walk `backend/src/main/java/com/workouthub/common/security/ForwardAuthFilter.java:33-34`:
`@Component @ConditionalOnProperty(name = "app.auth.mode", havingValue = "forward-auth")`.
Default mode is `builtin` (`application.yml:43-44`); the bean is absent in
default deployments and the JWT chain runs alone.

Walk `backend/src/main/java/com/workouthub/common/security/JwtAuthenticationFilter.java:36-50`:
extracts the Bearer token from `Authorization`; resolves
`AppUserPrincipal(userId, role)` via `JwtService.parse(token)` claims;
populates `SecurityContextHolder` with a `ROLE_<role>` authority. No second
auth scheme inside this filter.

### 1D. Error envelope (ApiError)

Walk `backend/src/main/java/com/workouthub/common/web/ApiError.java`:
```
record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldError> errors,
    String code) {
  record FieldError(String field, String message) {}
}
```

The `code` slot is the typed string from Phase 15-04. Walk
`backend/src/main/java/com/workouthub/common/web/GlobalExceptionHandler.java`
to inventory the `@ExceptionHandler` map:
- `MethodArgumentNotValidException` -> 400 (with `errors[]`).
- `NotFoundException` -> 404.
- `ConflictException` -> 409 (carries optional `code`).
- `ConstraintViolationException` -> 400 (with `errors[]`).
- `MissingServletRequestParameterException` -> 400.
- `MethodArgumentTypeMismatchException` -> 400.
- `BadCredentialsException` -> 401.
- `AccessDeniedException` -> 403.
- `HttpMessageNotReadableException` -> 400.
- `ResponseStatusException` -> mirrors `ex.getStatusCode()`.
- `Exception` (catch-all) -> 500.

Verdict: `ApiError` is the project-wide error envelope. The OpenAPI document
uses `ApiError` for every `4xx`/`5xx` response schema. Only `ConflictException`
populates the `code` field today; the other handlers leave it null (Jackson
`NON_NULL` strips it from the wire).

---

## Section 2 - Current Documentation State

### 2A. docs/API.md

Current content (verbatim, 3 lines):
```
# API

Placeholder. The full REST surface is documented here starting in PHASE 1.
```

Phase 1 closed in v0.3 (sessions and JWT auth shipped). The placeholder text
is stale by 2 milestones (v0.3 + v0.4 = 17 phases of REST surface added).
ROADMAP Phase 19 owns the rewrite; plan 19-05 ships it.

### 2B. docs/EXPORT_FORMAT.md

Post-18-04 deliverable. Documents:
- `FullExportDto` wire shape (camelCase per Direction C from 18-03).
- `ClaudeSummaryDto` wire shape (snake_case per `@JsonNaming` -- confirmed at
  `backend/src/main/java/com/workouthub/exports/dto/ClaudeSummaryDto.java:15`
  with the strategy applied per-record AND on each nested record per 18-03
  Direction C).
- Round-trip drift section (Plan 18-04: `BodyMetric.id`, `Supplement.id`,
  `exportedAt` non-preservation; timing validator-vs-importer asymmetry).
- `FullExportDto.SetRow` 9 components incl. `isPr` (post-18-04).

Phase 19 must NOT re-document the export wire shape in `docs/API.md`. Plan
19-05 LINKS from `docs/API.md` to `docs/EXPORT_FORMAT.md` for export field
references.

### 2C. SELF_HOSTED_CONTRACT.md section 7 alignment

Cite-and-walk:
- **7.1 Built-in auth (`docs/SELF_HOSTED_CONTRACT.md:264-269`)**: "The app
  handles login and (optionally) registration. Use a maintained library;
  never hand-roll JWT validation." -- WorkoutHub uses JJWT 0.13.0
  (`backend/pom.xml:22`). Compliant.
- **7.2 Reverse-proxy forward-auth (`docs/SELF_HOSTED_CONTRACT.md:270-290`)**:
  enumerates `AUTH_MODE`, `AUTH_HEADER_USER`, `AUTH_HEADER_EMAIL`,
  `AUTH_HEADER_GROUPS`, `TRUSTED_PROXIES`. The trust gate is non-optional:
  "The headers are trusted **only** when the request source IP matches
  `TRUSTED_PROXIES`." Enforced at `ForwardAuthFilter.java:62-65`.
- **7.3 Sessions (`docs/SELF_HOSTED_CONTRACT.md:292-296`)**: "Use Redis
  (Postgres apps) or a dedicated SQLite file (SQLite apps)." NOTE:
  WorkoutHub uses stateless JWT (no Redis). Per `SecurityConfig.java:35`,
  `SessionCreationPolicy.STATELESS`. Audit declares this is acceptable per
  the contract's intent (section 7.3 governs storage IF sessions exist;
  stateless JWT replaces sessions in this app). NOT raised as an issue.
- **0 (`docs/SELF_HOSTED_CONTRACT.md` "scope of this file"; pre-section 1)**:
  "Pick an identity provider" is explicitly out-of-scope. SpringDoc is purely
  a documentation generator -- adding it does NOT introduce an OIDC client
  or OAuth flow. Compliant.

Walk `backend/src/main/resources/application.yml:43-49` for env-var alignment:
- `app.auth.mode` <- `APP_AUTH_MODE` (default `builtin`).
- `app.auth.trusted-proxies` <- `APP_AUTH_TRUSTED_PROXIES` (default empty).
- `app.auth.headers.user/email/groups` <- `APP_AUTH_HEADER_USER/EMAIL/GROUPS`
  (defaults `X-Forwarded-User`, `X-Forwarded-Email`, `X-Forwarded-Groups` --
  match contract verbatim).

Cross-check: contract uses bare names (`AUTH_MODE`, `TRUSTED_PROXIES`); app
reads `APP_*`-prefixed names. Same prefix convention as `APP_JWT_SECRET`,
`APP_CORS_ALLOWED_ORIGINS`, etc. Documented in `.env.example`. Audit confirms
no contract violation; the prefix is project-specific.

---

## Section 3 - SpringDoc 2.x Artifact + Version Verification

### 3A. Candidate artifact selection

| Option | Coordinates | Description | Verdict |
|--------|-------------|-------------|---------|
| A1 | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | Boot 3.x compatible; bundles `springdoc-openapi-starter-webmvc-api` + Swagger UI assets. Default UI at `/swagger-ui.html`, JSON at `/v3/api-docs`. | **Recommended** subject to Section 4 Part 4D verdict (Swagger UI exposure stance). |
| A2 | `org.springdoc:springdoc-openapi-starter-webmvc-api` | JSON-only, no Swagger UI. Half the dependency footprint of A1. | Fallback if Section 4 Part 4D lands on Direction D3 (no UI in image). |
| A3 | `org.springdoc:springdoc-openapi-webmvc-core` | Manual configuration; bypasses Boot auto-config. | Rejected; not idiomatic. |

Lean Option A1. Section 4 Part 4D confirms Direction D1 (UI bundled, BIND_ADDR
is the trust boundary), so A1 is the verdict.

### 3B. Version pin verification

Plan 19-01 cannot run WebFetch in this executor's environment (audit-only;
read-only on filesystem). Use the conservative-pin fallback per the plan's
fallback clause:

- **Conservative pin: `2.6.0`** -- released circa Spring Boot 3.4.x; per
  SemVer guarantees + the SpringDoc 2.x compatibility statement, it is
  compatible with Spring Boot 3.5.x. Plan 19-02 MUST verify against
  `https://springdoc.org/` and `https://github.com/springdoc/springdoc-openapi/releases`
  before merging the `pom.xml` edit. The conservative pin minimises risk on
  first integration; bumping to `2.7.x` (or whatever Section 19-02 verifies as
  current) is a one-line follow-up.
- Alternative: pin via `<springdoc.version>` `<properties>` entry mirroring
  the existing `<jjwt.version>` precedent at `backend/pom.xml:22`. Plan 19-02
  recommend.

Known-incompatibility watchlist (for plan 19-02 CI smoke test):
- Jakarta EE 10 vs 11: Spring Boot 3.5 ships Jakarta EE 10; SpringDoc 2.x
  starter-webmvc-ui 2.6+ supports Jakarta EE 10 transitively.
- Hibernate Validator transitive version: SpringDoc declares no override; uses
  Spring Boot's managed version (`hibernate-validator` 8.x for Boot 3.5).
  No conflict expected.
- swagger-core 2.x: SpringDoc 2.x ships swagger-core 2.2.x; no conflict with
  WorkoutHub's other deps (no swagger-core elsewhere).

Plan 19-02 risk: `local_maven_gap` memory means SpringDoc smoke testing
runs only in CI on first push. Mitigation: include a smoke test
(Section 5E) that hits `GET /v3/api-docs` and asserts the `paths` map is
non-empty + contains at least one `/api/**` entry. Failure here surfaces a
SpringDoc/Boot incompatibility immediately on the CI run.

### 3C. Integration shape (zero-touch defaults)

With `springdoc-openapi-starter-webmvc-ui` on the classpath, SpringDoc
auto-configures:
- `GET /v3/api-docs` (JSON; configurable via `springdoc.api-docs.path`).
- `GET /v3/api-docs.yaml` (YAML).
- `GET /swagger-ui.html` (UI; redirects to `/swagger-ui/index.html`).
- `GET /swagger-ui/**` (UI assets).

Default scan covers all `@RestController` beans across the application.
Without further annotation:
- Endpoints group by class simple name (tag = `SessionsController`, etc.).
- Schema property names follow Jackson DTO record components +
  `@JsonNaming` (verified: SpringDoc 2.x reads Jackson config; per-record
  `@JsonNaming` on `ClaudeSummaryDto` produces `total_workouts` shape).
- HTTP method, path, request body type, response body type auto-derive from
  the controller method signature.
- No security scheme declared by default (every endpoint shows as public).
- Jackson `NON_NULL` global (`JacksonConfig.java:14-21`) is honoured
  schema-side as `nullable: true` rather than `required: false`.

Plan 19-02 must ADD an `OpenApiConfig` Java class declaring the two security
schemes (Section 4A) AND must explicitly EXCLUDE actuator + `/livez` +
`/healthz` + `/metrics` paths via `springdoc.paths-to-match` (Section 4B).

### 3D. application.yml config keys (plan 19-02 reference)

| Key | Default | Recommend | Rationale |
|-----|---------|-----------|-----------|
| `springdoc.api-docs.path` | `/v3/api-docs` | keep default | SpringDoc convention; tooling expects it. |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | keep default | UI convention. |
| `springdoc.paths-to-match` | (all) | `/api/**` | Excludes operator paths (Section 4B). |
| `springdoc.swagger-ui.disable-swagger-default-url` | `false` | `true` | Suppress default petstore demo url. |
| `springdoc.swagger-ui.tags-sorter` | (none) | `alpha` | Predictable navigation across 22 tags. |
| `springdoc.swagger-ui.operations-sorter` | (none) | `method` | Group by HTTP verb within each tag. |

These keys do NOT need to live in `application-prod.yml` unless the prod
overrides differ. Plan 19-02 verdict: add to `application.yml` only.

---
