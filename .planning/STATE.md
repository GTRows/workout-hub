# State

## Current Position

Milestone: v0.3 Self-Hosted Contract Alignment **ARCHIVED** (2026-05-04)
Phase: 13 of ? (v0.4 first phase, not yet planned)
Plan: Not started
Status: v0.3 archived; ready to discuss/plan v0.4
Last activity: 2026-05-04 - Archived v0.3 via /gsd:complete-milestone (3 releases shipped: v0.3.0, v0.3.1, v0.3.2)

Progress: v0.3 ████████████████████ 100% (14/14 plans)
          v0.4 ░░░░░░░░░░░░░░░░░░░░ 0% (not yet planned)

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/ROADMAP.md` for current roadmap (collapsed)
- See: `.planning/ISSUES.md` for open deferred issues (i-1, i-2, i-4, i-5, i-6, i-7, i-8, i-9)

**Current focus:** Planning v0.4 Backend Feature Completion (workouts, sessions, metrics, export refinement per `ProjectBrief.md` phases 3-5).

## Accumulated Context

### Locked-in Decisions (carried from v0.3)

Full decision log lives in `.planning/milestones/v0.3-ROADMAP.md` "Key Decisions" section. The most operationally relevant ones for v0.4:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).

### Open Issues at v0.3 Close

- i-1: WorkoutDaysIntegrationTest createDay helper NPE (6 tests, `@Disabled`). Needs local Maven debugger.
- i-2: FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (`@Disabled`). Same root cause as i-1.
- i-4: Frontend per-request HTTP metrics. Deferred to v0.6.
- i-5: next-intl 3 -> 4 major bump. Mitigation in place via reverse-proxy header rewrite.
- i-6: Next 15 -> 16 major bump. Deferred to v0.5.
- i-7: testcontainers 1.x -> 2.x major bump. Deferred to next test-infra session.
- i-8: Spring Boot 3.4 -> 4.0 major framework bump. Partially obsoleted by v0.3.1's 3.5.14 bump.
- i-9: StructuredLoggingTest cannot capture ECS JSON under `@SpringBootTest` (`@Disabled`). Real-runtime ECS output is unaffected.

## Session Continuity

Last session: 2026-05-04 - milestone archive
Stopped at: v0.3 archived, MILESTONES.md created, ROADMAP.md collapsed to one-line link
Resume file: None (next action: `/gsd:discuss-milestone` to scope v0.4, or `/gsd:consider-issues` to triage open issues first)

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/ROADMAP.md` (current, collapsed)
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
