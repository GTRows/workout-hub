# State

## Current Position

Milestone: v0.4 Backend Feature Completion (active)
Phase: 13 of 20 (workouts-audit) - first phase, not yet planned
Plan: Not started
Status: Ready to plan Phase 13
Last activity: 2026-05-04 - Created v0.4 milestone (8 phases) and outlined v0.5/v0.6/v1.0 in ROADMAP.md

Progress: v0.4 ░░░░░░░░░░░░░░░░░░░░ 0% (0/8 phases)
          v0.5 (planned) - Phases 21-30
          v0.6 (planned) - Phases 31-37
          v1.0 (planned) - Phases 38-44

## Project Reference

- See: `.planning/MILESTONES.md` for shipped milestone log
- See: `.planning/milestones/v0.3-ROADMAP.md` for full v0.3 archive
- See: `.planning/ROADMAP.md` for current roadmap (v0.4 detailed; v0.5/v0.6/v1.0 outlined)
- See: `.planning/ISSUES.md` for open deferred issues (i-1, i-2, i-4, i-5, i-6, i-7, i-8, i-9)

**Current focus:** Plan and execute Phase 13 (workouts-audit) to inventory the existing `workouts/` package and scope the hardening work for Phase 14.

## Accumulated Context

### Locked-in Decisions (carried from v0.3)

Full decision log lives in `.planning/milestones/v0.3-ROADMAP.md` "Key Decisions" section. The most operationally relevant ones for v0.4 and beyond:

- Update model: Renovate-pin (operator opens PR for new `vX.Y.Z`), not `:latest`.
- Reverse proxy: out of scope; operator brings their own.
- Public exposure: tailnet-only; never assume public exposure in app code.
- Auth modes: built-in JWT default, `AUTH_MODE=forward-auth` opt-in. No OIDC client code.
- Image registry: `ghcr.io/gtrows/workouthub-{backend,frontend}` (separate version cadences). Multi-arch amd64+arm64.
- BIND_ADDR: parametric env var, default `127.0.0.1`.
- Spring Boot version line: 3.5.x (bumped from 3.4 in v0.3.1 to close CVEs).
- Image tag construction: `${IMAGE_PREFIX,,}` lowercase via bash parameter expansion (mixed-case `github.repository_owner` safe).

### Issue-to-Phase Mapping

- i-1, i-2: closed by v0.4 Phase 14 (workouts-hardening).
- i-4: closed by v0.6 Phase 35 (frontend-http-metrics).
- i-9: closed by v0.6 Phase 37 (structured-logging-test-fix).
- i-7: closed by v1.0 Phase 39 (testcontainers-major).
- i-5, i-6, i-8: closed by v1.0 Phase 40 (framework-majors).

### Roadmap Evolution

- 2026-05-04: v0.4 milestone created with 8 phases (Phase 13-20). Full v1.0 path scoped: v0.5 frontend completion (Phase 21-30), v0.6 operational maturity (Phase 31-37), v1.0 release hardening (Phase 38-44). Total 32 phases to v1.0.

## Session Continuity

Last session: 2026-05-04 - milestone creation
Stopped at: v0.4 milestone created with 8 phases; v0.5/v0.6/v1.0 outlined in ROADMAP.md; phase directories created; STATE.md reset.
Resume file: None (next action: `/gsd:discuss-phase 13` for context, `/gsd:plan-phase 13` to break down, or `/gsd:research-phase 13` if needed).

## Reference Documents

- `.planning/MILESTONES.md` (shipped milestone log)
- `.planning/milestones/v0.3-ROADMAP.md` (full v0.3 archive)
- `.planning/ROADMAP.md` (current, v0.4 detailed; future milestones outlined)
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
