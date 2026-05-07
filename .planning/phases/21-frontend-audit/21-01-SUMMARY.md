# Phase 21 Plan 01: frontend-audit Summary

**v0.5 entry audit complete: 18 user-facing routes + 3 API route handlers inventoried; v0.4 backend coverage matrix produced; ProjectBrief Phase 4-6 gap matrix tags 38 Implemented / 4 Partial / 1 Stub / 14 Missing; Phases 22-30 each get a concrete keep/narrow/expand/split/merge/defer recommendation tied to Section 5 evidence.**

## Accomplishments

- Inventory complete: 18 user-facing route pages + 3 API route handlers + middleware catalogued; 9 components + 11 lib modules tabled with line counts and consumers; PWA manifest, service worker strategy, i18n locale resolver, and Playwright config quoted verbatim.
- API coverage matrix: 36 wrapped+typed, 6 wrapped+untyped (`fetchClaudeSummary`, `fetchFullExport`, `fetchSectionExport`, `fetchSessionsCsv`, `importFullDump`, push subscribe pair), 14 missing wrappers (plan/day/exercise CRUD, edit-existing-set, exercise progress, admin exercise CRUD, logout). Three v0.4 contract decisions explicitly graded: snake_case ClaudeSummary PASS (pass-through); `isPr` durability PASS (display + offline + export round-trip safe); `clientSetId` idempotency NOT honored (`AddSetPayload` lacks the field).
- ProjectBrief Phase 4-6 gap matrix: 38 Implemented, 4 Partial (set-logging-rpe, IndexedDB queue wiring, dashboard quick actions, weekly-summary), 1 Stub (online-drain not wired into session-client), 14 Missing (logout, dashboard summary cards, plan/day/exercise CRUD, RPE input, edit-existing-set, body-metric sub-fields, progress-photo upload, public registration removed by design).
- v0.5 phase re-scope: 22 narrow, 23 narrow, 24 expand+split (4 sub-plans), 25 expand+split (4 sub-plans), 26 narrow-or-defer-v0.6, 27 narrow-or-defer-v0.6, 28 narrow, 29 merge-into-22 OR narrow, 30 narrow-or-defer-v0.6. Plus 3 net-new phases recommended: 22.5 typed-ApiError-helper, 25.5 zod-schemas-for-AddSet/ImportResult, 29.5 middleware-matcher-includes-achievements.
- Frontend test gate posture documented: 3 CI scripts active (`pnpm lint`, `pnpm typecheck`, `pnpm test:coverage`); Playwright e2e present locally only (not in CI). Per-phase test additions enumerated with fixture blockers.

## Files Created/Modified

- `.planning/phases/21-frontend-audit/21-01-AUDIT.md` - audit deliverable, 590 lines, Sections 1-7.
- `.planning/phases/21-frontend-audit/21-01-SUMMARY.md` - this file.

No frontend source, config, lockfile, or workflow file changed. `git diff --stat` for the phase range shows only `.planning/phases/21-frontend-audit/` activity.

## Decisions Made

- Phase 22 auth-pages: narrow scope to logout + session-expired UX (login page is done; register is intentionally removed).
- Phase 24 plan-editor: split into 4 sub-plans because 9 plan/day/exercise wrappers are missing in `endpoints.ts`.
- Phase 25 session-execution: split into 4 sub-plans (`clientSetId`, IndexedDB drain wiring, typed ApiError branching, RPE+edit-set). The current Dexie code is well-tested in isolation but never invoked by the session client.
- Phase 29 profile-settings: recommend merging into 22 since profile/supplements/webhook-tokens are already shipped.
- Three net-new phases (22.5, 25.5, 29.5) recommended for insertion before their respective downstream phases.
- ROADMAP.md will need an edit before Phase 22 plan-phase runs - either accept the narrow/expand/split recommendations as a roadmap rewrite, or insert the three net-new phases. This audit does not rewrite ROADMAP.md (out of scope per plan).

## Issues Encountered

- Convention `max ~200 lines per file` flagged: `endpoints.ts` (591), `schemas.ts` (332), `session-client.tsx` (352), `export-client.tsx` (329), `nutrition-client.tsx` (316), `plan-client.tsx` (277), `profile-client.tsx` (263), `metrics-client.tsx` (246). Audit notes only - no splits proposed.
- Middleware matcher (`middleware.ts:42-54`) does not include `/achievements/:path*` even though `/achievements` is reachable via top nav and is implicitly authenticated. Surfaced as candidate net-new phase 29.5.
- v0.4 contract decision `ApiError.code` typed catalog (4 values) is not consumed anywhere on the frontend except an `error.status`-only check in `(auth)/login/page.tsx`. Recommended new helper `mapApiError(error)` in phase 22.5.
- v0.4 body-metrics 200/201 split is not surfaced in `/metrics` UI; the wrapper returns the same shape regardless. Phase 28 candidate.
- No backend gap surfaced that requires a new v0.6 backend issue. The frontend-only deferred items (i-4 frontend HTTP metrics, i-5 next-intl 4, i-6 Next 16) stay open by design.

## Next Phase Readiness

- Ready for Phase 22 planning: yes. Audit gives the planner concrete scope + dependencies + test additions.
- Open questions for Phase 22 onwards: (1) accept the audit's `narrow` recommendations as ROADMAP edits, OR keep ROADMAP outline as-is and let each plan-phase narrow scope inside its plan? (2) Do we insert phases 22.5 / 25.5 / 29.5 explicitly, or fold them into their parents? (3) Should phases 26 / 27 / 30 advance to v0.6 to free v0.5 budget for the expand/split phases (24, 25)?
- Whether ROADMAP.md needs an edit before Phase 22 plan-phase runs: recommended yes - at minimum, mark Phase 22 / 24 / 25 with their split-counts so the planner knows to emit multiple plans.
