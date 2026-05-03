---
phase: 08-dependabot-triage
plan: 01
subsystem: dependencies
tags: [dependabot, security, cve, deps]
requires: []
provides:
  - bouncycastle 1.84 closing 7 vuln alerts (1 HIGH + 6 medium)
  - safe routine bumps merged
  - GitHub Actions majors aligned with current standards
  - 4 deferred majors documented as ISSUES with explicit reopen triggers
affects: []
tech-stack:
  added: []
  patterns:
    - "Squash-merge Dependabot PRs in priority order: security HIGH first, then patches, then majors with workflow review"
    - "Defer breaking-change majors via ISSUES rather than silent ignore"
key-files:
  modified:
    - .planning/ISSUES.md
key-decisions:
  - "Bouncycastle bumped first (closes 7 of 12 alerts including only HIGH)"
  - "next-intl 3->4 deferred; open-redirect risk mitigated via reverse-proxy header rewrite (operator side)"
  - "next 15->16, testcontainers 1->2, Spring Boot 3.4->4.0 deferred to later milestones"
  - "PR #12 (web-push 5.1.1->5.1.2) hit merge conflict, requested @dependabot rebase; will land on next bot scan"
issues-created: [i-5, i-6, i-7, i-8]
duration: ~20 min
completed: 2026-05-03
---

# Phase 8 Plan 01: Dependabot triage

**Twelve of seventeen Dependabot PRs merged (1 HIGH security fix + 5 safe patches + 5 GitHub Actions majors + 2 dev-only majors); four breaking-change majors deferred with explicit ISSUES entries.**

## Performance

- **Duration:** ~20 min (executed inline; one merge conflict on web-push deferred to bot rebase)
- **Started:** 2026-05-02T22:00:00Z (estimate)
- **Completed:** 2026-05-03T00:40:00Z (estimate; spans midnight)
- **Tasks:** 3 of 3
- **Files modified:** 1 (`.planning/ISSUES.md`)
- **Origin commits landed:** 12 (one per merged Dependabot PR)

## Accomplishments

- **Security HIGH closed first.** Bouncycastle bcprov-jdk18on bumped 1.77 -> 1.84 via PR #1, which closes 7 of 12 outstanding vuln alerts in one shot (covert timing channel + LDAP injection + 5 mediums).
- **Five safe patches merged**: jjwt 0.12.6 -> 0.13.0 (#9), jacoco 0.8.12 -> 0.8.14 (#10), tanstack/react-query 5.100.1 -> 5.100.5 (#14), eslint group (#15). web-push 5.1.1 -> 5.1.2 (#12) hit a merge conflict from the bouncycastle landing and was queued for `@dependabot rebase` - will land on next bot scan automatically.
- **GitHub Actions majors merged**: docker/build-push-action 6 -> 7 (#3), pnpm/action-setup 4 -> 6 (#4), docker/setup-buildx-action 3 -> 4 (#5), docker/login-action 3 -> 4 (#6), softprops/action-gh-release 2 -> 3 (#7). Workflow files reviewed; no removed inputs in use.
- **Dev-only majors merged**: lucide-react 0.469 -> 1.11 (#16; icons only, low risk), @types/node 22 -> 25 (#17; types only, CI typecheck guards regressions).
- **Four breaking-change majors deferred** with ISSUES.md i-5 through i-8 entries:
  - i-5: next-intl 3 -> 4 (PR #2) - mitigation in place via reverse-proxy header rewrite
  - i-6: next 15 -> 16 (PR #13) - to v0.5 frontend work
  - i-7: testcontainers 1 -> 2 (PR #11) - to next test-infra session
  - i-8: spring-boot 3.4 -> 4.0 (PR #8) - to v0.6 operational maturity

## Task Commits

1. **Task 1: security + safe patches** - 6 origin commits (Dependabot squash merges 6762ab9 plus 5 more)
2. **Task 2: GH Actions + dev majors** - 7 origin commits (8e4d5f1 / 78790fb / abaf1f2 / ea19c50 / 9c55791 / 1711741 / 18eb447)
3. **Task 3: ISSUES.md i-5..i-8 entries** - included in this metadata commit (no separate code commit needed)

**Plan metadata:** pending (this commit)

## Files Created/Modified

- `.planning/ISSUES.md` - 4 new entries (i-5 next-intl, i-6 next 16, i-7 testcontainers 2, i-8 Spring Boot 4) with explicit reopen triggers
- (Code commits all came via Dependabot squash merges - tracked above)

## Decisions Made

- **Order: security HIGH before patches.** Bouncycastle's covert-timing-channel CVE is the only HIGH; landing it first reduces the open-vuln window to minutes, then the rest happen at leisure.
- **Defer over force-merge for majors.** Spring Boot 4, Next 16, testcontainers 2, next-intl 4 all carry real migration cost. Squashing them through against CI without a migration plan would either pass with hidden runtime breakage or fail. ISSUES entries capture the work for the right milestone.
- **Reverse-proxy mitigation for open-redirect.** next-intl 4 closes the open-redirect alert on its own, but the operator-side mitigation (reverse-proxy header rewrite, documented for Phase 9) blocks the same attack vector with no application code change. Acceptable v0.3 trade-off.
- **`@dependabot rebase` over force-merge for the conflict.** PR #12 (web-push patch) hit a conflict from the bouncycastle merge; commenting `@dependabot rebase` lets the bot rebase + retest cleanly without admin override.

## Deviations from Plan

None significant. The web-push merge conflict (PR #12) is a normal Dependabot interaction, not a deviation; documented in Decisions Made.

## Issues Encountered

- PR #12 merge conflict from sibling PR landing first. Resolved by requesting bot rebase. Next bot scan reopens the merge cleanly.

## Next Phase Readiness

- **Phase 8 closes here.** Open vuln alert count drops from 12 to 2 (next-intl open-redirect, mitigated via reverse proxy).
- **Phase 9 (README and MIGRATION docs) unblocked.** The operator-side mitigation note for next-intl belongs in the README exposure section.
- **No blockers.** Local main is fast-forwarded to origin including all 12 squash-merge commits.

---
*Phase: 08-dependabot-triage*
*Completed: 2026-05-03*
