# Architecture

**Analysis Date:** 2026-05-02

## Pattern Overview

**Overall:** Feature-sliced monolithic backend + Next.js client, talking over a REST + JWT boundary, both packaged as Docker containers and orchestrated by a single Compose file.

**Key Characteristics:**
- Backend is the only service that touches the database
- Frontend calls only the public REST API (`/api/*`)
- Stateless backend (JWT, no server-side session) - DB is the only durable store
- Offline-first execution flow (workout sessions queued in IndexedDB, drained when online)
- Schema is owned by Flyway migrations; entities never auto-generate tables (`ddl-auto=validate`)

## Layers

**Backend - Controller / Service / Repository (per feature):**

- **Controller** (`*Controller.java`) - `@RestController`, request validation via `@Valid` + Bean Validation, returns response DTOs only. Example: `backend/src/main/java/com/workouthub/auth/AuthController.java`.
- **Service** (`*Service.java`) - business logic and transactions (`@Transactional`). Example: `backend/src/main/java/com/workouthub/sessions/SessionsService.java`.
- **Repository** (Spring Data JPA interfaces under `*/domain/`) - persistence. Example: `backend/src/main/java/com/workouthub/users/domain/UserRepository.java`.
- **Mapper** (`*Mapper.java`) - hand-written entity <-> DTO conversion. Example: `backend/src/main/java/com/workouthub/workouts/WorkoutPlanMapper.java`.
- **Domain** (`*/domain/*.java`) - JPA entities and enums.
- **DTO** (`*/dto/*.java`) - Java records for HTTP boundary; never expose entities directly.

**Frontend - Server-first with selective client islands:**

- **Server Components (default)** - data fetching, layouts, static parts.
- **Client Components** (`"use client"`) - state, effects, browser APIs. Files seen: `dashboard/page.tsx`, `exercises-client.tsx`, `achievements-client.tsx`.
- **API client layer** (`frontend/src/lib/api/client.ts`) - singleton, JWT refresh interceptor, Zod schema validation at boundary.
- **Schemas** (`frontend/src/lib/api/schemas.ts`) - Zod schemas mirroring backend DTO shape.
- **Offline layer** (`frontend/src/lib/offline/`) - Dexie store + drain logic for unsynced sets.
- **i18n layer** (`frontend/src/i18n/request.ts`, `frontend/messages/*.json`) - next-intl, default locale `tr`.

## Data Flow

**Authenticated HTTP Request:**

1. Browser issues `fetch` against `NEXT_PUBLIC_API_BASE_URL` from a TanStack Query hook in a client component, OR a Next Server Component fetches server-side.
2. Request hits backend Tomcat on `SERVER_PORT` (default 8080).
3. `TraceIdFilter` (`backend/src/main/java/com/workouthub/common/security/TraceIdFilter.java`) attaches a request-id MDC entry.
4. `JwtAuthenticationFilter` (`backend/src/main/java/com/workouthub/common/security/JwtAuthenticationFilter.java`) reads `Authorization: Bearer <jwt>`, parses via `JwtService`, populates `AppUserPrincipal` in the SecurityContext (or 401).
5. Spring routes to the matching Controller method; Bean Validation runs on `@Valid` request DTOs (validation failures -> `MethodArgumentNotValidException`).
6. Controller delegates to Service (`@Transactional` boundary), which uses Repositories and Mappers.
7. Service returns DTO records; Controller wraps in `ResponseEntity` and Spring serializes via Jackson (lowercase enum names via `JacksonConfig` and `@JsonValue` per-enum).
8. Errors bubble up to `GlobalExceptionHandler` (`backend/src/main/java/com/workouthub/common/web/GlobalExceptionHandler.java`) which maps to `ApiError` JSON.

**Offline Workout Session:**

1. User starts a session - `POST /api/sessions/start`.
2. Each completed set is recorded client-side first via `frontend/src/lib/offline/session-set-queue.ts` (Dexie).
3. Background drain loop attempts `POST /api/sessions/:id/sets`. Idempotency key `(sessionId, exerciseId, setNumber)`; 409 from backend is treated as "already synced".
4. On reconnect, queue drains; backend canonicalises and PR detection runs (`backend/src/main/java/com/workouthub/analytics/PrDetector.java`).

**State Management:**
- Backend: stateless. JWT carries identity. No server-side sessions (rejected by `application.yml: open-in-view: false`).
- Frontend: TanStack Query owns server cache; React local state for ephemeral UI; Dexie for offline queue.

## Key Abstractions

**Service:**
- Purpose: domain logic per feature, transactional boundary
- Examples: `backend/src/main/java/com/workouthub/auth/AuthService.java`, `backend/src/main/java/com/workouthub/workouts/WorkoutPlansService.java`, `backend/src/main/java/com/workouthub/sessions/SessionsService.java`
- Pattern: constructor-injected dependencies, no field injection, `@Transactional` at class or method level

**Repository:**
- Purpose: typed persistence over JPA entities
- Examples: `backend/src/main/java/com/workouthub/sessions/domain/WorkoutSessionRepository.java`
- Pattern: Spring Data JPA interface + custom `@Query` when joins or aggregates are needed

**DTO (record):**
- Purpose: serialize / deserialize HTTP payload, separate from domain
- Examples: `backend/src/main/java/com/workouthub/auth/dto/LoginRequest.java`, `backend/src/main/java/com/workouthub/workouts/dto/WorkoutDayDto.java`
- Pattern: Java 21 record, validation annotations, no Lombok

**Mapper:**
- Purpose: hand-written entity <-> DTO conversion
- Examples: `backend/src/main/java/com/workouthub/workouts/WorkoutPlanMapper.java`, `backend/src/main/java/com/workouthub/exercises/ExerciseMapper.java`
- Pattern: static methods, no MapStruct

**Filter (servlet):**
- Purpose: cross-cutting on every request
- Examples: `JwtAuthenticationFilter`, `TraceIdFilter` (both under `backend/src/main/java/com/workouthub/common/security/`)
- Pattern: `extends OncePerRequestFilter`, ordered via `SecurityConfig`

**ControllerAdvice:**
- Purpose: centralized exception-to-response mapping
- Example: `backend/src/main/java/com/workouthub/common/web/GlobalExceptionHandler.java`
- Pattern: `@RestControllerAdvice` with per-exception `@ExceptionHandler`; covers validation, security, custom domain exceptions, `ResponseStatusException`, `HttpMessageNotReadableException`, generic fallback

**Offline queue (frontend):**
- Purpose: durable client-side store for unsynced workout sets
- Implementation: Dexie database (`frontend/src/lib/offline/session-set-queue.ts`)
- Pattern: write-through during workout, drain on reconnect

## Entry Points

**Backend:**
- `backend/src/main/java/com/workouthub/WorkoutHubApplication.java` - `@SpringBootApplication` with `@EnableScheduling` (for reminder cron jobs)
- Boot sequence: load `application.yml` -> apply env -> Flyway migrate -> Hibernate validate -> Spring Security wiring -> Tomcat bind

**Frontend:**
- `frontend/src/app/layout.tsx` - root layout, registers `Providers` (TanStack Query, i18n, theme), Service Worker registration
- `frontend/src/app/page.tsx` - landing redirect (auth gate)
- Route group `(auth)` - login, register, OIDC callback (publicly reachable)
- Route group `(app)` - all authenticated surfaces (dashboard, plan, session, history, metrics, profile, etc.)

**Compose:**
- `compose.yml` - `db` (postgres:16-alpine, healthcheck `pg_isready`), `backend` (depends on db `service_healthy`), `frontend` (depends on backend, no health condition - tracked in CONCERNS)

## Error Handling

**Strategy:** Throw typed exceptions in services; map at the edge in `GlobalExceptionHandler`.

**Custom exceptions:**
- `NotFoundException` -> 404 (`backend/src/main/java/com/workouthub/common/web/NotFoundException.java`)
- `ConflictException` -> 409 (`backend/src/main/java/com/workouthub/common/web/ConflictException.java`)
- `ResponseStatusException` (Spring) -> mapped to its declared status by handler added in PR #18

**Auth and validation:**
- `BadCredentialsException` -> 401 generic message
- `AccessDeniedException` -> 403
- `MethodArgumentNotValidException` / `ConstraintViolationException` -> 400 with field-level errors
- `HttpMessageNotReadableException` -> 400 generic "Malformed request body"
- Generic `Exception` -> 500 + structured log

## Cross-Cutting Concerns

**Security:**
- `SecurityConfig` (`backend/src/main/java/com/workouthub/common/config/SecurityConfig.java`) - stateless JWT, role-based `@EnableMethodSecurity`, CORS allowlist
- BCrypt password hashing via `PasswordEncoder` bean
- TOTP 2FA optional (`twofa/`)
- Brute-force lockout via `BruteForceGuard`

**CORS:**
- `CorsConfig` (`backend/src/main/java/com/workouthub/common/config/CorsConfig.java`) - origins from `app.cors.allowed-origins` env var

**Serialization:**
- `JacksonConfig` (`backend/src/main/java/com/workouthub/common/config/JacksonConfig.java`) - JavaTimeModule, ISO-8601 instants, `NON_NULL` inclusion, case-insensitive enums
- Per-enum `@JsonValue` lowercase for `SupplementTiming` (added in PR #18)

**Validation:**
- `WebConversionConfig` (`backend/src/main/java/com/workouthub/common/config/WebConversionConfig.java`) - case-insensitive enum binding for query parameters (Category, Equipment, Difficulty)
- Bean Validation on request DTOs

**Logging:**
- SLF4J on every class
- Trace ID MDC via `TraceIdFilter`
- No `System.out.println` anywhere in `backend/src/main/`

**Scheduling:**
- `@EnableScheduling` on the application class
- Cron-driven reminders for workouts, weight logs, supplements (env-controlled)

**Time:**
- `Clock` bean injected where current time matters (e.g., `BruteForceGuard`) - keeps tests deterministic

---

*Architecture analysis: 2026-05-02*
*Update when major patterns change*
