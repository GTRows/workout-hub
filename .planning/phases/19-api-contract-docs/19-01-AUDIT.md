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

## Section 4 - Behavioral Analysis

### 4A. Two security schemes (contract section 7.1 + 7.2)

**Built-in JWT (default mode)**:
```yaml
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
security:
  - bearerAuth: []
```
Applies globally to every `/api/**` endpoint EXCEPT the permitAll matchers
(`POST /api/auth/login`, `POST /api/auth/refresh`,
`POST /api/auth/reset-password`, `POST /api/webhooks/scale/**`). Plan 19-02
declares per-method `security: []` overrides on those four operations.

**Forward-auth (opt-in)**:
```yaml
components:
  securitySchemes:
    forwardAuth:
      type: apiKey
      in: header
      name: X-Forwarded-Email
```
Declared as an alternative scheme. Client tooling shows it as "Pick one"
rather than "Both required".

**Asymmetry note (un-modelable in OpenAPI)**: the source-IP trust gate at
`ForwardAuthFilter.java:62-65` is enforced server-side; OpenAPI 3.x has no
mechanism to model "header trusted only when source IP is in
`TRUSTED_PROXIES`". The OpenAPI document declares the header presence; the
trust boundary is documented in `docs/API.md` prose plus a link to
`docs/SELF_HOSTED_CONTRACT.md` section 7.2. Plan 19-05 bears this prose.

Verdict: declare both schemes. `bearerAuth` is the default (applied
globally). Operators flipping `APP_AUTH_MODE=forward-auth` rely on the
contract section 7.2 setup; OpenAPI documents it as a header for tooling
convenience but the security boundary is the reverse proxy.

### 4B. Actuator + management exclusion

Walk `backend/src/main/resources/application.yml:60-74`:
- `management.endpoints.web.exposure.include = health,info,prometheus`.
- `management.endpoint.health.show-details = never`.
- `management.metrics.tags.application = workouthub`.
- `management.prometheus.metrics.export.enabled = true`.

Three management endpoints exposed publicly:
- `/actuator/health` (Spring Boot Actuator default).
- `/actuator/info` (build info).
- `/actuator/prometheus` (Prometheus scrape).

Plus the contract-required app-owned endpoints:
- `/livez`, `/healthz` via `HealthController`.
- `/metrics` via `PrometheusMetricsController`.

None belong in the publicly-documented OpenAPI surface (the surface for
user-facing clients = `/api/**`). Plan 19-02 must add
`springdoc.paths-to-match: "/api/**"` to suppress the actuator + operator
paths.

Verdict: confirm `/api/**` include scope. Note for plan 19-05 (docs/API.md
rewrite): operator-targeted documentation already lives in
`docs/OBSERVABILITY.md`; cross-link from `docs/API.md` to that file rather
than duplicate.

### 4C. docs/API.md rewrite shape (single most important Phase 19 verdict)

Three directions walked:
- **C1 (hand-write per-endpoint reference)**: O(N) prose for N=134 mappings;
  high maintenance burden (every plan in v0.5+ that adds an endpoint
  requires a doc edit). **Rejected**.
- **C2 (navigational README, link to runtime + per-section docs)**:
  - Link to deployed `/v3/api-docs` JSON + `/swagger-ui.html`.
  - (Optional, deferred) static export under `docs/openapi.json` per release
    tag; refreshed by a CI job. v0.4 ships only the runtime endpoint.
  - Link to `docs/EXPORT_FORMAT.md` for export wire shape.
  - Link to `docs/SELF_HOSTED_CONTRACT.md` section 7 for auth.
  - Link to `docs/OBSERVABILITY.md` for operator paths.
  - In-file: security-scheme overview, typed `ApiError.code` table, "How to
    consume the OpenAPI document for client codegen" pointer.
- **C3 (hybrid: prose for auth+errors, embed Swagger UI iframe)**: most
  maintenance; doc duplication. **Rejected**.

**Verdict: Direction C2.** SpringDoc-generated OpenAPI is the single source of
truth; `docs/API.md` is navigational. Static `docs/openapi.json` deferred to
Phase 42 (v1.0).

### 4D. Swagger UI exposure stance (operator concern)

Three directions walked:
- **D1 (bundle UI; BIND_ADDR is the trust boundary)**: contract section 3.x
  default `BIND_ADDR=127.0.0.1` means UI is reachable only from operator's
  host. Operators who set `0.0.0.0` (against the contract) accept the
  discoverability risk. Zero operator work. **Recommended**.
- **D2 (env-flag opt-in `APP_OPENAPI_UI_ENABLED=false` default)**: extra env
  var + extra config wiring. Safer default at the cost of operator-side
  friction.
- **D3 (no UI; JSON only)**: maps to Option A2 from Section 3A.

**Verdict: Direction D1.** Plan 19-05 documents the BIND_ADDR-as-trust-boundary
stance in a "Discoverability" sub-section of `docs/API.md`.

### 4E. ApiError.code catalog

Walk `Grep "new ConflictException(" --type java`:

| Code constant                  | Value                       | Origin                                                          | HTTP status |
|--------------------------------|-----------------------------|------------------------------------------------------------------|-------------|
| `CODE_SESSION_ALREADY_ACTIVE`  | `SESSION_ALREADY_ACTIVE`    | `sessions/SessionsService.java:25` -- thrown at line 44          | 409         |
| `CODE_SESSION_ALREADY_FINISHED`| `SESSION_ALREADY_FINISHED`  | `sessions/SessionsService.java:26` -- thrown at line 81          | 409         |
| `CODE_SESSION_FINISHED`        | `SESSION_FINISHED`          | `sessions/SessionsService.java:27` -- thrown at line 98          | 409         |
| `CODE_SET_NUMBER_DUPLICATE`    | `SET_NUMBER_DUPLICATE`      | `sessions/SessionSetsService.java:29` -- thrown at line 103-105  | 409         |

Other `ConflictException` throw sites (no typed code; `code` field is null on
the wire):
- `admin/AdminExercisesService.java:30` -- "Exercise with this English name already exists".
- `admin/AdminExercisesService.java:57` -- duplicate-name on update.
- `admin/AdminExercisesService.java:83` -- "Exercise is referenced by a workout plan and cannot be deleted".
- `admin/AdminUsersService.java:42` -- "Email already registered".
- `admin/AdminUsersService.java:85` -- "Cannot delete your own account".
- `workouts/WorkoutDaysService.java:52` -- "Day {n} already exists on this plan".
- `workouts/WorkoutDaysService.java:70` -- duplicate day-of-week on update.
- `workouts/WorkoutDaysService.java:143` -- (un-typed conflict).

Phase 15-04 introduced typed codes only on the live-session conflict surface
(4 codes). The remaining 8 throw sites surface 409 with `code: null`. Plan
19-04 hand-curates the OpenAPI schema annotation on `ApiError.code` listing
the 4 typed values + an explicit "may be null on legacy paths" note.

**Verdict: hand-curate 4 typed values in `@Schema(allowableValues = ...)`;
defer extracting `ApiErrorCode` Java enum to Phase 42.**

---

## Section 5 - Cross-Package Coupling Map and Verdicts

### 5A. SpringDoc consumers (zero today)

`Grep "io.swagger.v3.oas.annotations|org.springdoc" --type java` against
`backend/` returns no results. Phase 19 introduces the package boundary
cleanly. Plan 19-02 will add:
- `OpenApiConfig` (under `common/config/`) declaring `@Bean OpenAPI` with the
  two security schemes + project info.
- Per-controller `@Tag` annotations (Section 5B Option B2).
- Per-method `@Operation(operationId = "...")` for the two SessionsController
  collisions (Section 1B). The full project does NOT need per-method
  annotations -- SpringDoc auto-derives reasonable defaults.

### 5B. Tag strategy

| Option | Description | Cost | Verdict |
|--------|-------------|------|---------|
| B1 | rely on SpringDoc class-name auto-tagging | 0 edits | Rejected: ugly tags, dual-class collisions. |
| B2 | `@Tag(name = "...")` per controller | 31 single-line edits | **Recommended.** |
| B3 | `springdoc.group-configs` per package in YAML | 0 Java; ~22 yaml lines | Rejected: less granular than per-controller. |

**Verdict: Option B2.** 31 edits in plan 19-03; one tag per controller.

Tag-name proposals (1 per controller, mirrors feature-package mental model):

| Controller class | Proposed tag |
|------------------|--------------|
| AchievementsController | Achievements |
| AdminChallengesController (challenges/) | Admin: Challenges |
| AdminExercisesController (admin/) | Admin: Exercises |
| AdminUsersController (admin/) | Admin: Users |
| AnalyticsController | Analytics |
| AuditController | Admin: Audit |
| AuthController | Auth |
| ChallengesController | Challenges |
| ExerciseAnalyticsController | Analytics: Exercise |
| ExerciseController | Exercises |
| ExportController | Export |
| HealthImportController | Health Import |
| MetricsController | Body Metrics |
| NutritionController | Nutrition |
| PasswordChangeController | Users (password) |
| PasswordResetController | Auth (password reset) |
| PushController | Push Notifications |
| sessions.SessionsController | Workout Sessions |
| SessionSetsController | Workout Sessions: Sets |
| SupplementsController | Supplements |
| TwoFactorController | 2FA |
| users.SessionsController | User Sessions (refresh tokens) |
| UsersController | Users |
| WaterController | Water |
| WebhookTokensController | Webhook Tokens |
| WorkoutDayByIdController | Workout Days |
| WorkoutDaysController | Workout Plans: Days |
| WorkoutPlansController | Workout Plans |
| HealthController + PrometheusMetricsController + ScaleWebhookController | (excluded by `paths-to-match`) |

### 5C. SecurityConfig changes for plan 19-02

`SecurityConfig.java:43-45` already permitAlls operator paths. SpringDoc paths
to add to the permitAll list:
- `/v3/api-docs/**` (the OpenAPI JSON; must be reachable for tooling).
- `/swagger-ui/**` (UI assets; required if Direction D1).
- `/swagger-ui.html` (UI entry).

Plan 19-02 extends the existing operator permitAll matcher in
`SecurityConfig.java:43-45` with these three patterns. Without this, the JWT
filter returns 401 for any unauthenticated GET on the OpenAPI document --
defeating the purpose.

**Verdict: 1 SecurityConfig edit, 3 path patterns added.**

### 5D. Build wiring options

| Option | Description | v0.4 verdict |
|--------|-------------|--------------|
| E1 | pom.xml dep only; OpenAPI served at runtime | **Recommended for v0.4.** |
| E2 | + `springdoc-openapi-maven-plugin` to generate `target/openapi.json` at build time AND copy to `docs/openapi.json`. | Defer to Phase 42. |
| E3 | + CI tag-push step that runs the maven plugin and commits `docs/openapi.json` | Defer to Phase 42. Touches protected `pom.xml` AND protected workflow file. |

**Verdict: Option E1 for v0.4.** Plan 19-02 makes a SINGLE protected
`pom.xml` edit (the dependency block + optional `<springdoc.version>`
property). No plugin block.

### 5E. Test strategy (OpenApiSurfaceIntegrationTest)

New file `backend/src/test/java/com/workouthub/common/web/OpenApiSurfaceIntegrationTest.java`
(~80 lines) extending `AbstractIntegrationTest`. Five tests:

1. `openApiJsonIsReachableUnauthenticated` -- 200 on GET `/v3/api-docs`.
2. `openApiJsonContainsApiPathsAndExcludesActuator` -- assert
   `paths.["/api/sessions"]` exists; `paths.["/actuator/health"]` does NOT;
   `paths.["/livez"]` does NOT; `paths.["/metrics"]` does NOT.
3. `openApiJsonDeclaresBothSecuritySchemes` -- assert
   `components.securitySchemes.bearerAuth` (http/bearer/JWT) and
   `components.securitySchemes.forwardAuth` (apiKey/header).
4. `swaggerUiIsReachableUnauthenticated` -- 200 on GET `/swagger-ui.html`
   (Direction D1).
5. `apiErrorCodeEnumMatchesGlobalExceptionHandler` -- assert the `ApiError`
   schema's `code` field enum lists the 4 typed codes from Section 4E.

---

## Section 6 - Recommended Phase 19 plan-02+ Scope

Bucket-by-bucket fix list. Each bucket cites the section that produced the
verdict.

- **`springdoc-dependency`** -- add
  `org.springdoc:springdoc-openapi-starter-webmvc-ui` (Section 3A Option A1,
  Section 3B conservative pin `2.6.0` subject to plan-19-02-time WebFetch
  verification) to `backend/pom.xml`. **`pom.xml` is PROTECTED per
  `CLAUDE.md` "Protected Files"; the executor MUST halt and ask the user
  for confirmation before editing.** Single `<dependency>` block in
  `<dependencies>`, plus a `<springdoc.version>` `<properties>` entry
  mirroring `<jjwt.version>` precedent at `backend/pom.xml:22`. Targets:
  `backend/pom.xml` (1 dependency block, 1 property entry). Depends on
  Section 3 verdicts.

- **`openapi-config-class`** -- new file
  `backend/src/main/java/com/workouthub/common/config/OpenApiConfig.java`
  declaring `@Bean OpenAPI customOpenAPI()`:
  - `info.title = "WorkoutHub API"`, `info.version` (read from
    `IDENTITY.yaml` via `@Value("${app.version:0.0.0}")` or static),
    `info.description = "Self-hosted multi-user fitness tracker. See docs/API.md."`.
  - `components.securitySchemes.bearerAuth` (`type: http`, `scheme: bearer`,
    `bearerFormat: JWT`).
  - `components.securitySchemes.forwardAuth` (`type: apiKey`, `in: header`,
    `name: X-Forwarded-Email`).
  - Global `security: [{bearerAuth: []}]`.
  - `servers: []` left empty (operator's PUBLIC_URL is per-deployment;
    SpringDoc default is request scheme/host).
  Targets: `backend/src/main/java/com/workouthub/common/config/OpenApiConfig.java`
  (~50 lines new file). Depends on Section 4 Part 4A verdict.

- **`application-yml-springdoc-keys`** -- add 6 `springdoc.*` keys to
  `backend/src/main/resources/application.yml` per Section 3D. NOT added to
  `application-prod.yml` (no override needed). Depends on Sections 3D + 4B
  verdicts.

- **`security-config-permitall`** -- extend
  `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java:43-45`
  permitAll matcher with `/v3/api-docs/**`, `/swagger-ui/**`,
  `/swagger-ui.html`. Single edit; 3 patterns added. Depends on Section 5C
  verdict.

- **`controller-tag-annotations`** -- add `@Tag(name = "...")` to all 28
  user/admin-facing `@RestController` classes (the 3 operator controllers
  -- `HealthController`, `PrometheusMetricsController`, `ScaleWebhookController`
  -- are excluded from SpringDoc scan via `paths-to-match`). Tag names per
  Section 5B table. Targets: 28 `*Controller.java` files (1 line each).
  Depends on Section 5B Option B2 verdict.

- **`operationid-disambiguation`** -- add
  `@Operation(operationId = "...")` to method-level entries on
  `users.SessionsController` AND/OR `sessions.SessionsController` so any
  same-named methods (`list`, `get`, `delete`) emit unique operationIds in
  the OpenAPI document. Plan 19-03 inspects both classes at execution time
  and lands the disambiguating annotations on the smaller class (likely
  `users.SessionsController` -- 3 mappings). Targets: 1-2
  `*Controller.java` files (3-6 method-level annotations expected).
  Depends on Section 1B verdict.

- **`apierror-code-enum-annotation`** -- add
  `@Schema(description = "Typed error code", allowableValues = {"SESSION_ALREADY_ACTIVE", "SESSION_ALREADY_FINISHED", "SESSION_FINISHED", "SET_NUMBER_DUPLICATE"})`
  to `ApiError.code` per Section 4E catalog. Targets:
  `backend/src/main/java/com/workouthub/common/web/ApiError.java` (1
  annotation update; ~3 lines). Depends on Section 4 Part 4E verdict.

- **`docs-api-md-rewrite`** -- replace the 3-line placeholder with the
  navigational README per Section 4 Part 4C Direction C2. Sections:
  - One-paragraph intro (what this doc is, what it links to).
  - "Runtime endpoints" (`/v3/api-docs`, `/swagger-ui.html`, link to live).
  - "Authentication" (built-in JWT + forward-auth; link to
    `docs/SELF_HOSTED_CONTRACT.md` section 7).
  - "Error envelope" (`ApiError` shape + typed `code` table).
  - "Export format" (link to `docs/EXPORT_FORMAT.md`).
  - "Operator endpoints" (link to `docs/OBSERVABILITY.md`).
  - "Discoverability" (BIND_ADDR-as-trust-boundary stance per Section 4D).
  Targets: `docs/API.md` (~80-120 lines, full rewrite). Depends on Sections
  4C + 4D + 4E verdicts.

- **`openapi-surface-test`** -- new integration test per Section 5E.
  Targets:
  `backend/src/test/java/com/workouthub/common/web/OpenApiSurfaceIntegrationTest.java`
  (~80 lines new file). Depends on Section 5E verdict.

- **`defer`** -- explicit non-goals for v0.4: static `docs/openapi.json`
  export under git (Phase 42); `springdoc-openapi-maven-plugin` build wiring
  (Phase 42); `ApiErrorCode` Java enum extraction (Phase 42); per-method
  `@Operation` annotations beyond the two collision spots (low ROI on the
  v0.4 budget); `@Schema` per-DTO annotations beyond `ApiError.code` (DTO
  records auto-derive correctly); OpenAPI 3.1 spec (SpringDoc 2.x emits 3.1
  by default; no extra config); per-environment server overrides via
  `springdoc.servers`.

### Plan-count recommendation

Four plans total for Phase 19 implementation:

- **Plan 19-02: springdoc-integration** (medium-large). Bundles
  `springdoc-dependency` + `openapi-config-class` +
  `application-yml-springdoc-keys` + `security-config-permitall`. Touches:
  `backend/pom.xml` (PROTECTED -- halt-and-ask gate),
  `backend/src/main/java/com/workouthub/common/config/OpenApiConfig.java`
  (new), `backend/src/main/resources/application.yml`,
  `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java`.
  ~4 file touches.
- **Plan 19-03: tag-and-collision-fix** (medium). Bundles
  `controller-tag-annotations` + `operationid-disambiguation`. Touches: 28
  `*Controller.java` files (1 line each) + 1-2 method-level annotations on
  the SessionsController collision. ~28-30 file touches. Mechanical scope;
  keep as a single plan.
- **Plan 19-04: error-schema-and-test** (small-medium). Bundles
  `apierror-code-enum-annotation` + `openapi-surface-test`. Touches:
  `ApiError.java` (1 annotation) + new `OpenApiSurfaceIntegrationTest.java`
  (~80 lines). ~2 file touches.
- **Plan 19-05: docs-api-md-rewrite** (small-medium). Touches: `docs/API.md`
  only. ~1 file rewrite. Depends on 19-02 + 19-04 because the content
  references runtime endpoints + the typed error-code list.

### Protected-file gate

Plan 19-02 MUST halt and ask the user before editing `backend/pom.xml`. The
plan's preamble must state the protection explicitly so the executor cannot
silently bypass the gate. The other targeted files in the bucket
(`OpenApiConfig.java`, `application.yml`, `SecurityConfig.java`) are not
protected.

### NEW issue candidates surfaced by this audit

(Recording here; opening `ISSUES.md` is out-of-scope for plan 19-01.)

- **i-NEW-A**: `ApiError.code` typed-value drift -- 8 `ConflictException`
  throw sites today have NO typed code (`code: null` on the wire). Phase
  19-04 hand-curates the 4 typed codes in `@Schema.allowableValues`. If
  v0.5+ plans add new typed codes without updating the annotation, the
  OpenAPI document silently drifts. Mitigation: extract `ApiErrorCode` Java
  enum (Phase 42 candidate) so the annotation reads from the enum.
- **i-NEW-B**: SpringDoc + Spring Boot 3.5.x first-integration risk -- local
  Maven gap (`local_maven_gap` memory) means the dependency add is
  CI-tested only. Mitigation: the surface integration test
  (`OpenApiSurfaceIntegrationTest`) hits `/v3/api-docs` and asserts a
  non-empty `paths` map + at least one `/api/**` entry. Failure surfaces
  the incompatibility immediately on push.
- **i-NEW-C**: Dual `SessionsController` operationId collision -- captured
  by plan 19-03; record as resolved-by-plan rather than deferred.

---

End of `19-01-AUDIT.md`. No Java, migration, test, or docs source files
modified by plan 19-01. Plan 19-02 begins with the `pom.xml` halt-and-ask
gate.
