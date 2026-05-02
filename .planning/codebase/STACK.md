# Technology Stack

**Analysis Date:** 2026-05-02

## Languages

**Primary:**
- Java 21 (LTS) - All backend application code (`backend/pom.xml`)
- TypeScript 5.7.2 (strict) - All frontend application code (`frontend/package.json`, `frontend/tsconfig.json`)

**Secondary:**
- SQL - Flyway migrations (`backend/src/main/resources/db/migration/V*.sql`)

## Runtime

**Backend:**
- JVM (Eclipse Temurin) 21 - `backend/Dockerfile` (multi-stage Maven build to JRE 21)
- Spring Boot embedded Tomcat - listens on `SERVER_PORT` (default 8080)

**Frontend:**
- Node.js >= 20 - `frontend/package.json` engines
- Next.js 15.1 standalone server - listens on 3000

**Package Manager:**
- Maven (backend) - lockfile-less; pinned via `pom.xml`
- pnpm 9.15 (frontend) - lockfile `frontend/pnpm-lock.yaml`

## Frameworks

**Backend Core:**
- Spring Boot 3.4.1 (`backend/pom.xml`)
- Spring Web, Spring Data JPA, Spring Security
- Hibernate ORM (transitive via JPA)
- Flyway 11.x - schema migrations, source of truth
- Bean Validation (jakarta.validation)
- Spring Boot Actuator - health and Prometheus endpoints

**Frontend Core:**
- Next.js 15.1 (App Router, RSC default) - `frontend/package.json`
- React 19
- Tailwind CSS 4.0 - `frontend/postcss.config.mjs`, `frontend/tailwind.config.ts`
- shadcn/ui - components copied into `frontend/src/components/ui/`
- TanStack Query 5.62 - server state
- Radix UI - accessible primitives
- next-intl 3.26 - i18n; default locale `tr`
- React Hook Form 7.54 + Zod 3.24 - forms and validation
- Recharts 2.15 - progress charts
- Lucide React - icons
- Dexie 4.0 - IndexedDB wrapper for offline workout sets

**Backend Testing:**
- JUnit 5 - unit and integration tests
- Spring Boot Test
- Testcontainers 1.20.4 (`testcontainers-bom`) - real PostgreSQL per test run; H2 intentionally excluded
- AssertJ + Hamcrest matchers
- JaCoCo - coverage with 70% line threshold

**Frontend Testing:**
- Vitest - unit and component tests, jsdom environment - `frontend/vitest.config.ts`
- @testing-library/react + @testing-library/user-event
- Playwright - end-to-end - `frontend/playwright.config.ts`
- Coverage thresholds: lines 70, statements 70, functions 60, branches 60

**Build/Dev:**
- Maven (backend); `./mvnw test`, `./mvnw verify`
- pnpm scripts (frontend); `pnpm dev`, `pnpm build`, `pnpm test`, `pnpm test:e2e`, `pnpm typecheck`, `pnpm lint`

## Key Dependencies

**Backend Critical:**
- `io.jsonwebtoken:jjwt 0.12.6` - JWT signing/parsing (`backend/pom.xml`)
- `nl.martijndwars:web-push 5.1.1` - VAPID Web Push (`backend/src/main/java/com/workouthub/notifications/WebPushJavaSender.java`)
- `dev.samstevens.totp:totp 1.7.1` - TOTP 2FA (`backend/src/main/java/com/workouthub/twofa/`)
- `org.bouncycastle 1.77` - VAPID crypto
- `io.micrometer:micrometer-registry-prometheus` - metrics

**Frontend Critical:**
- `next 15.1.0`, `react 19`, `typescript 5.7.2`
- `@tanstack/react-query 5.62.0`
- `dexie 4.0.10` - offline session set queue
- `next-intl 3.26.0` - i18n
- `react-hook-form 7.54.0`, `zod 3.24.0`
- `tailwindcss 4.0.0`, `class-variance-authority`

## Configuration

**Backend Environment:**
- All runtime config via env vars merged into `backend/src/main/resources/application.yml`
- Profiles: `local` default, `docker` via `SPRING_PROFILES_ACTIVE`
- Required: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`
- Optional: `APP_JWT_ACCESS_TTL_SECONDS`, `APP_JWT_REFRESH_TTL_SECONDS`, `APP_CORS_ALLOWED_ORIGINS`, `APP_ADMIN_*`, `APP_AUTH_OIDC_*`, `APP_PUSH_VAPID_*`, `APP_REMINDERS_*`
- Schema source of truth: Flyway migrations; `spring.jpa.hibernate.ddl-auto=validate`

**Frontend Environment:**
- `NEXT_PUBLIC_API_BASE_URL` - default `http://localhost:8080`
- Service Worker for PWA registration

**Build Configuration:**
- `backend/Dockerfile` (multi-stage Maven -> Temurin 21 JRE)
- `frontend/Dockerfile` (multi-stage Node 22 with pnpm + Next standalone)
- `compose.yml` (3 services: db, backend, frontend)

## Platform Requirements

**Development:**
- Local Maven currently unavailable on this Windows host (memory: local_maven_gap); backend tests run only in CI
- pnpm + Node 20 for frontend dev
- Docker Desktop for full-stack via Compose

**Production / Self-hosted:**
- Single-host Docker Compose deployment expected
- Database PostgreSQL 16-alpine
- Bind interface configurable via `BIND_ADDR` (per `docs/SELF_HOSTED_CONTRACT.md` - currently not parametric in `compose.yml`, see CONCERNS)
- GHCR image publishing planned for v0.3 milestone (multi-arch amd64+arm64)

---

*Stack analysis: 2026-05-02*
*Update after major dependency changes*
