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
