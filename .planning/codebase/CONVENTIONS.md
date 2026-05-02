# Coding Conventions

**Analysis Date:** 2026-05-02

## Java Conventions

**Package layout:**
- Feature-sliced under `backend/src/main/java/com/workouthub/<feature>/` (auth, users, exercises, workouts, sessions, etc.)
- Inside each feature: `Controller`, `Service`, `Mapper`, `dto/`, `domain/`
- Cross-cutting under `common/config/`, `common/security/`, `common/web/`

**Java 21 idioms in use:**
- All DTOs are records (e.g., `LoginRequest`, `WorkoutDayDto`, `SupplementDto`)
- No Lombok dependency in `backend/pom.xml`
- Pattern matching and sealed types not yet used (acceptable; introduce when fitting)

**Naming:**
- Classes / records: `PascalCase` (`AuthService`, `LoginRequest`)
- Methods, fields, locals: `camelCase`
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_FAILURES`, `WINDOW_MINUTES` in `BruteForceGuard`)
- Enums: `PascalCase` type, `UPPER_SNAKE_CASE` values (`SupplementTiming.MORNING`, `Role.ADMIN`)
- Test classes: `*Test` (unit), `*IntegrationTest` (Spring + Testcontainers)
- Migrations: `V<n>__<snake_case_description>.sql`

**DTOs at HTTP boundary:**
- Records under `<feature>/dto/`
- Validation via `jakarta.validation` annotations on request DTOs:
  ```java
  public record LoginRequest(
      @NotBlank @Email String email,
      @NotBlank String password,
      String totpCode) {}
  ```
- Never return JPA entities directly

**Dependency injection:**
- Constructor injection only; no `@Autowired` on fields
- Final fields, no setters

**Transactions:**
- `@Transactional` at service class or method level
- `propagation = REQUIRES_NEW` only when needed (e.g., `BruteForceGuard.recordFailure` to survive outer rollback)
- Read-only operations marked `@Transactional(readOnly = true)`

**Error handling:**
- Custom domain exceptions: `NotFoundException`, `ConflictException` (`backend/src/main/java/com/workouthub/common/web/`)
- `ResponseStatusException` for HTTP-status-tagged failures from services
- All mapped centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`)
- No raw `RuntimeException` in service code

**Logging:**
- SLF4J `LoggerFactory.getLogger(<Class>.class)`
- No `System.out.println` anywhere in `backend/src/main/`
- Trace ID propagated via MDC by `TraceIdFilter`
- Field deny-list filter (production logs): planned per `docs/SELF_HOSTED_CONTRACT.md` §8 (currently not enforced - tracked in CONCERNS)

**Comments / docs:**
- Class-level JavaDoc on services and controllers when behavior is non-obvious
- Inline comments only for "why", not "what"

## TypeScript / React Conventions

**TypeScript settings:**
- `"strict": true` in `frontend/tsconfig.json`
- No `any` in committed code; use `unknown` and narrow
- Path aliases: `@/` -> `src/`

**React Server Components default:**
- Server-first; `"use client"` directive only when needed (state, effects, browser APIs)
- Confirmed selective use: `dashboard/page.tsx`, `exercises-client.tsx`, `achievements-client.tsx`

**Data fetching:**
- Server Components fetch on the server
- Client Components use TanStack Query hooks
- No ad-hoc `fetch` in components - everything goes through `frontend/src/lib/api/client.ts` -> `endpoints.ts`

**Forms:**
- `react-hook-form` + `zod` schemas
- Schemas live in `frontend/src/lib/api/schemas.ts` and feature-local files when narrow

**Validation:**
- Zod at the API boundary (`apiClient.parseResponse` style)
- Shared schemas mirror backend DTO shape

**Styling:**
- Tailwind utility-first; design tokens in `frontend/tailwind.config.ts`
- `class-variance-authority` for component variants (e.g., `Button`)
- shadcn/ui components copied into `frontend/src/components/ui/` (not imported from a package)
- No inline styles

**i18n:**
- All user-facing strings via `next-intl`
- Default locale `tr`; English available
- Messages: `frontend/messages/tr.json`, `frontend/messages/en.json`
- Keys are dotted (`nav.dashboard`, `auth.loginTitle`)

**File organization:**
- Max ~200 lines per file (CLAUDE.md File Organization rule); CONCERNS lists exceptions
- One module = one responsibility
- No `utils.ts` dump file at feature level

**Naming (frontend):**
- File names: `kebab-case.ts` / `kebab-case.tsx`
- React Server Components: `page.tsx`, `layout.tsx`, `loading.tsx`, `error.tsx` (App Router conventions)
- Client component files often suffixed `-client.tsx` when paired with a server `page.tsx`
- Test files: `*.test.ts` / `*.test.tsx` co-located with source

## Tooling

**Backend:**
- Build: `./mvnw verify` (lint + test + jacoco coverage 70% line threshold)
- No checkstyle / spotless config detected
- Coverage gate: JaCoCo 70% line minimum (per `pom.xml`)

**Frontend:**
- Lint: `pnpm lint` (ESLint with Next config)
- Type-check: `pnpm typecheck` (`tsc --noEmit`)
- Format: Prettier (config in `frontend/package.json` or `.prettierrc` if present)
- Test: `pnpm test` (Vitest), `pnpm test:e2e` (Playwright)

**Pre-commit hooks:**
- None at repo level
- Claude Code hook layer in `.claude/hooks/` (template-managed; pre_guard_*, post_validate_syntax)
- Pre-commit secret scanning planned per `docs/SELF_HOSTED_CONTRACT.md` §4 / §12 (CONCERNS)

## Standing Rules (project-wide)

From `CLAUDE.md`:
- All code, identifiers, comments must be in English
- User-facing strings live in i18n files (Turkish primary, English secondary)
- No emojis anywhere in code, comments, or responses
- Commits use conventional commit format with scope (`feat(api):`, `fix(auth):`, `chore(docker):`, etc.)
- One logical change per commit
- Reference `.planning/ISSUES.md` IDs in commit subject when applicable
- No `Co-Authored-By: Claude` trailers (user authors via local git config)

## What NOT to Do (project-specific)

From `CLAUDE.md`:
- No `console.log` / `print()` for debugging - use proper logging
- No TODO comments in code - track in `.planning/ISSUES.md` or DEFERRED.md
- No defensive code against impossible states
- No polyfills unless minimum supported version requires them
- No new external dependencies without discussing first
- No backwards-compatibility shims for code being removed
- No edits to protected files (`IDENTITY.yaml`, `pom.xml`, `package.json`, lockfiles, `compose.yml`, Dockerfiles, GH workflows, `scripts/`) without explicit confirmation

---

*Convention analysis: 2026-05-02*
*Update when patterns change*
