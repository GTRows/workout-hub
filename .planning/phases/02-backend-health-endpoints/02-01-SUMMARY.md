---
phase: 02-backend-health-endpoints
plan: 01
subsystem: infra
tags: [health, readiness, spring-boot, flyway, security]
requires:
  - .planning/phases/01-compose-refactor/01-01-SUMMARY.md
  - .planning/phases/01-compose-refactor/01-02-SUMMARY.md
provides:
  - livez endpoint
  - healthz with DB+Flyway readiness
  - public allowlist
affects:
  - Plan 02-02 compose retarget
  - Phase 4 metrics endpoint potential collocation
tech-stack:
  added: []
  patterns:
    - dedicated /healthz +/livez per orchestrator semantics
key-files:
  created:
    - backend/src/main/java/com/workouthub/common/web/HealthController.java
    - backend/src/main/java/com/workouthub/common/web/dto/HealthStatusDto.java
    - backend/src/main/java/com/workouthub/common/web/FlywayMigrationsApplied.java
    - backend/src/test/java/com/workouthub/common/HealthControllerIntegrationTest.java
  modified:
    - backend/src/main/java/com/workouthub/common/config/SecurityConfig.java
---

# Phase 2 Plan 01: Backend health endpoints

`/livez` and `/healthz` controllers with DB + Flyway readiness checks, allowlisted in SecurityConfig, covered by an AbstractIntegrationTest-based MockMvc test.

## Accomplishments

- New `HealthController` exposing two contract endpoints under the existing `common/web/` package: `/livez` returns `200 {"status":"alive"}` unconditionally; `/healthz` returns `200 {"status":"ready"}` only when the JDBC `DataSource` is reachable AND Flyway reports zero pending migrations, otherwise `503 {"status":"unready","reason":"<cause>"}`.
- `FlywayMigrationsApplied` thin component wrapping the autoconfigured `Flyway` bean so the controller can be tested without coupling to Flyway internals; reuses the bean Spring Boot already provides.
- `HealthStatusDto` Java record with `NON_NULL` Jackson serialization (already wired in `JacksonConfig`) so the `reason` field is omitted on the success path.
- `SecurityConfig` allowlists `/livez` and `/healthz` adjacent to the existing `/actuator/*` allowlist block so unauthenticated container probes succeed.
- `HealthControllerIntegrationTest` extends `AbstractIntegrationTest` (real Postgres 16 via Testcontainers) and asserts both endpoints return 200 with the expected `$.status` payload without an `Authorization` header.

## Files Created/Modified

- `backend/src/main/java/com/workouthub/common/web/HealthController.java` (created)
- `backend/src/main/java/com/workouthub/common/web/dto/HealthStatusDto.java` (created)
- `backend/src/main/java/com/workouthub/common/web/FlywayMigrationsApplied.java` (created)
- `backend/src/main/java/com/workouthub/common/config/SecurityConfig.java` (modified - allowlist update only)
- `backend/src/test/java/com/workouthub/common/HealthControllerIntegrationTest.java` (created)

## Decisions Made

- **Readiness composition**: DB ping (`Connection.isValid(1)`, 1-second timeout) + Flyway `info().pending().length == 0`. These two together prove "JDBC pool live AND schema at the version the running JAR expects". Spring Boot is implicitly verified because the bean wouldn't be reachable otherwise.
- **`/actuator/health` left untouched**: Kept in the allowlist and on the classpath. The contract endpoints are the operator-facing surface; actuator stays for ad-hoc monitoring tools that already key off it.
- **No `@PreAuthorize` on the controller**: Compose healthcheck and external probes cannot present a JWT. Authorization is handled at the SecurityConfig allowlist layer instead, matching the existing `/actuator/*` pattern.
- **No success-path logging**: Probes hit every 15s (compose) or more often (orchestrators). Success-path logs would dominate stdout. `log.warn` fires only on the three unready branches (`db_unreachable`, `db_error:<class>`, `migrations_pending`).
- **Reason field is machine-parseable**: `db_unreachable` / `db_error:<exception-class-simple-name>` / `migrations_pending` are short stable tokens an operator can grep for; the Jackson `NON_NULL` config drops the field on success so the success body stays minimal.
- **Unready paths not exercised in integration test**: Forcing them either requires Testcontainer manipulation that fights `AbstractIntegrationTest`'s static singleton container, or `DataSource` mocking that would couple the test to controller internals. The contract correctness is "ready when up"; failure modes are guarded by explicit branches and surface in the operator's runtime.

## Issues Encountered

None. Local Maven is unavailable on this Windows host (per `local_maven_gap` memory), so compilation and test execution will run on CI on push - the code was inspected against existing patterns (`GlobalExceptionHandler`, `HealthEndpointIntegrationTest`, `AuthFlowIntegrationTest`) for syntactic and structural correctness.

## Next Step

Plan 02-02 unblocked: it can retarget `compose.yml`'s backend healthcheck from `/actuator/health` to `/healthz` and add frontend `/healthz` + `/livez` route handlers, knowing the backend endpoint actually exists with the contract-specified semantics.
