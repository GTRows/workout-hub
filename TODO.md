# TODO

Persistent task tracking for this project. Managed via `/task`.

Format:
```
- [t-N] Short imperative title
  - Acceptance: one-line objective criterion
  - Notes: optional context (only if useful)
```

Ids are monotonic (`t-1`, `t-2`, ...). Never reuse. Never renumber.

## Active

## Blocked

- [setup-ci] Revisit CI scaffolding decision
  - Acceptance: `.github/workflows/ci.yml` exists and runs lint + test for backend and frontend on push/PR; `ci.yml.template` still kept for reference
  - Blocked: deferred at setup per user direction (2026-04-22). Revisit after backend has a Maven wrapper or a stable docker-based CI recipe so the job does not start red.

- [setup-release] Revisit release scaffolding decision
  - Acceptance: decision recorded in CHANGELOG or RELEASE.md whether this self-hosted app uses tag-triggered GitHub Releases or image-based docker tags; scaffolding either activated or explicitly marked not-applicable
  - Blocked: deferred at setup per user direction (2026-04-22). Revisit around PHASE 8 when the deployment model (self-hosted docker pull vs github release) is concrete.

## Done

- [t-1] 2026-04-22 -- Scaffold PHASE 0 baseline (backend, frontend, docker-compose, env)
- [setup-1] 2026-04-22 -- Document Architecture / Entry Flow in CLAUDE.md
- [setup-2] 2026-04-22 -- Document Module Breakdown in CLAUDE.md
- [setup-3] 2026-04-22 -- Document Data Storage in CLAUDE.md
- [setup-icon] 2026-04-22 -- Moved to DEFERRED.md (needs design input and graphics tooling)
- [setup-protected-maven] 2026-04-22 -- Extend PROTECTED_EXACT now that Maven files exist
