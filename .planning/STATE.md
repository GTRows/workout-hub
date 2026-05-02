# State

## Current Position

Milestone: v0.3 Self-Hosted Contract Alignment
Phase: 1 of 12 (Compose Refactor)
Plan: Not started
Status: Ready to plan
Last activity: 2026-05-02 - Milestone v0.3 created (12 phases)

Progress: ░░░░░░░░░░ 0%

## Accumulated Context

### Decisions Locked-in (2026-05-02)

- **Update model**: Renovate-pin (operator opens PR for new `vX.Y.Z`), not Watchtower / `:latest`. Reason: audit trail, rollback via `git revert`.
- **Reverse proxy**: Out of scope for this repo. Operator brings their own (Caddy in maintainer's homelab). The application speaks plain HTTP behind whatever the operator chooses.
- **Public exposure**: Tailnet-only for the maintainer's deployment, hardened later. Forbidden for this app to assume public exposure.
- **Auth modes**: Built-in JWT default (already implemented), `AUTH_MODE=forward-auth` opt-in (Phase 5). No OIDC client code in this repo per contract section 7 - the existing `OidcController` predates the contract and will be reconsidered in Phase 5.
- **Persistence migration**: Local DB starts fresh on contract migration (named volume `db_data` -> bind mount `./data/postgres/`). Operator instructions in `docs/MIGRATION.md` per Phase 9.
- **Host topology**: Application is single-primary; replica is the operator's concern (litestream / pg_dump sidecar / restic). The app exposes the data layout, the operator backs it up.
- **Image registry**: `ghcr.io/gtrows/workouthub-backend` and `-frontend` (two separate images, separate version cadences). Multi-arch amd64+arm64.
- **BIND_ADDR**: parametric env var, default `127.0.0.1`. Tailnet IP injected at deploy time, never committed to repo.
- **Frontend health**: `/healthz` and `/livez` both = process liveness only. Frontend is stateless and should not depend on backend reachability for its own healthcheck (proxy upstreams them independently).

### Pre-GSD Work Carried Forward

- v0.2 was shipped informally, snapshot in `.planning/HANDOFF.md`. Treat as historical record, not gospel.
- 25 Flyway migrations applied (V1..V25)
- 23 backend feature packages already implemented
- Frontend has API client, providers, i18n, offline queue, Service Worker - but most pages are stubs

### Open Issues

- **i-1**: WorkoutDaysIntegrationTest helper NPE on response `id` (6 errors). Needs local Maven for breakpoint debugging. See `.planning/ISSUES.md`.
- **i-2**: FullExportImportIntegrationTest.importRoundTripPreservesPlansFromExport (1 failure). Same trigger as i-1.
- Both blocked on local-Maven environment setup (memory: local_maven_gap).

### Roadmap Evolution

- 2026-05-02: Milestone v0.3 created. Theme: self-hosted contract alignment (`docs/SELF_HOSTED_CONTRACT.md`). 12 phases (Phase 1-12).

## Session Continuity

Last session: 2026-05-02 ~14:00 local
Stopped at: Milestone v0.3 initialization
Resume file: None (next action: `/gsd:plan-phase 1`)

## Reference Documents

- `.planning/codebase/STACK.md`
- `.planning/codebase/ARCHITECTURE.md`
- `.planning/codebase/STRUCTURE.md`
- `.planning/codebase/CONVENTIONS.md`
- `.planning/codebase/TESTING.md`
- `.planning/codebase/INTEGRATIONS.md`
- `.planning/codebase/CONCERNS.md`
- `.planning/HANDOFF.md` (pre-GSD snapshot)
- `.planning/ISSUES.md` (deferred test failures)
- `docs/SELF_HOSTED_CONTRACT.md` (binding operational contract)
- `ProjectBrief.md` (original phase plan)
