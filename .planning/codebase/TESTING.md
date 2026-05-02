# Testing Patterns

**Analysis Date:** 2026-05-02

## Backend

### Framework

- JUnit 5 + Spring Boot Test
- Testcontainers 1.20.4 (`org.testcontainers:testcontainers-bom`) via `@Testcontainers`
- AssertJ + Hamcrest for assertions; JSONPath for response body checks
- MockMvc for HTTP-layer integration tests

### Run commands

```bash
cd backend
./mvnw test                              # unit + integration
./mvnw verify                            # full verify with JaCoCo
./mvnw test -Dtest=AuthFlowIntegrationTest    # single class
```

Local Maven currently unavailable on this Windows host (memory: local_maven_gap). Backend tests run in CI only; iterate via PR + Actions.

### Real PostgreSQL via Testcontainers

`backend/src/test/java/com/workouthub/support/AbstractIntegrationTest.java`:

```java
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {
    protected static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("workouthub_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void registerDatasourceAndAppProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-jwt-secret-at-least-32-bytes-long-for-hs512-0123456789");
        registry.add("app.jwt.access-ttl-seconds", () -> "3600");
        registry.add("app.jwt.refresh-ttl-seconds", () -> "1209600");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
    }
}
```

H2 is intentionally NOT used; integration tests must hit real Postgres.

### Test categories

- **Unit tests:** pure logic (e.g., `StreakCalculatorTest`, `PrDetectorTest`, `ImportValidatorTest`, `JwtServiceTest`, `GlobalExceptionHandlerTest`) - no Spring context, fast
- **Integration tests:** `*IntegrationTest` extending `AbstractIntegrationTest` (`AuthFlowIntegrationTest`, `WorkoutPlansIntegrationTest`, `SessionLifecycleIntegrationTest`, etc.) - 42 files
- **Migration tests:** under `backend/src/test/java/com/workouthub/migrations/V*MigrationTest.java` - one per migration that needs validation

### Test helpers

- `backend/src/test/java/com/workouthub/support/TestAuthHelpers.java` - seeds users with role and returns access token:
  ```java
  SeededUser u = helpers.seed("user@test.local", "StrongSecret1!", Role.USER);
  String auth = "Bearer " + u.accessToken();
  ```
- `TestSecuredController` (under `common/security/test/`) - test-only endpoint to verify SecurityConfig wiring

### Test patterns

- `@AutoConfigureMockMvc` + `@Autowired MockMvc mvc`
- Each test seeds its own user via nanoTime suffix to avoid collisions
- Status assertions via `MockMvcResultMatchers.status()`
- Body assertions via `jsonPath("$.field").value(...)`
- For data persistence assertions, repositories injected via `@Autowired`

### Coverage

- JaCoCo configured in `backend/pom.xml`
- Threshold: 70% line coverage minimum (build fails below)
- Report: `backend/target/site/jacoco/index.html`

### Currently failing (tracked in `.planning/ISSUES.md`)

- i-1: 6 errors in `WorkoutDaysIntegrationTest` (createDay helper NPE on response `id`)
- i-2: 1 failure in `FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport`

## Frontend

### Framework

- Vitest (config: `frontend/vitest.config.ts`)
- Environment: jsdom
- Setup file: `frontend/src/test/setup.ts`
- React Testing Library (`@testing-library/react`, `@testing-library/user-event`)

### Run commands

```bash
cd frontend
pnpm test                       # vitest run (one-shot)
pnpm test -- --watch            # watch mode
pnpm test:coverage              # vitest run --coverage (v8 provider)
pnpm test:e2e                   # playwright
pnpm typecheck                  # tsc --noEmit
pnpm lint                       # eslint
```

### Coverage thresholds

```
lines: 70
statements: 70
functions: 60
branches: 60
```

Excludes: layouts, middleware, i18n config, `components/ui/` primitives.

### Test patterns

- `describe / it / expect` with Vitest API
- Mocks via `vi.fn()`, `vi.mock()`
- Co-located test files (e.g., `frontend/src/lib/api/client.test.ts` next to `client.ts`)
- API client tests mock `fetch` and assert request/response shape, retry logic, JWT refresh
- Component tests render with React Testing Library, simulate user events with `@testing-library/user-event`

### E2E

- Playwright configuration: `frontend/playwright.config.ts`
- Critical flows: register -> login -> session -> finish -> history
- Run in CI on every PR (frontend job)

### Last known status

- 89/89 frontend tests passing (per `.planning/HANDOFF.md`)
- 3 lint warnings, 0 errors

## CI Test Execution

`.github/workflows/ci.yml`:
- Backend job: `mvn verify` (Testcontainers uses runner Docker)
- Frontend job: `pnpm install`, `pnpm typecheck`, `pnpm lint`, `pnpm test`
- CodeQL: separate workflow on push + weekly cron

Missing per `docs/SELF_HOSTED_CONTRACT.md` §12 (tracked in CONCERNS):
- gitleaks-action (secret scan)
- trivy fs and trivy image (CVE scan)
- docker compose config (compose validation)
- hadolint (Dockerfile lint)

## What to add when writing tests

**Backend:**
- New service: corresponding `<Service>Test` (unit) or `<Feature>IntegrationTest` (HTTP-level)
- New endpoint: integration test asserting status + response body shape + persistence side effects
- New migration: dedicated `V<n><Name>MigrationTest` if migration is non-trivial
- Use `TestAuthHelpers.seed()` for any test needing an authenticated principal

**Frontend:**
- New component: co-located `*.test.tsx` rendering it with required providers
- New API endpoint helper: `*.test.ts` mocking fetch and asserting request shape
- New form: tests for happy path + Zod validation failure path
- New page in `(app)/`: at least one e2e flow in Playwright

---

*Testing analysis: 2026-05-02*
*Update when test patterns change*
