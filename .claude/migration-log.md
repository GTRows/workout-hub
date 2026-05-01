# Template migration log

## v0.1.x -> v0.5.0 (wholesale drop-in)

The project was on the pre-`v0.2.0` template layout (root-level commands,
no `gtr/` namespace, `PROJECT.yaml` for identity). `.claude/` template-owned
content was replaced wholesale with the upstream `v0.5.0` tree; project
content was preserved.

### Pre-replacement snapshot

`/tmp/wh-pre-v050/` (outside the repo) holds a copy of the previous
`.claude/`, `TODO.md`, `DEFERRED.md`, `PROJECT.yaml` for reference. Git
history also preserves everything before the drop-in commit.

### Renames applied

- `PROJECT.yaml` -> `IDENTITY.yaml` (single source of truth for identity).
- `TODO.md` -> `TODO.md.legacy` (pre-GSD task list; migrate to GSD via
  `/gsd:new-project`).
- `DEFERRED.md` -> `DEFERRED.md.legacy` (kept for reference).

### Slash-command renames in prose

`/setup`, `/menu`, `/doctor`, `/release`, `/update`, `/onboard`,
`/new-migration` rewritten to their `/gtr:*` form in:

- `CLAUDE.md`
- `README.md`
- `IMPLEMENT.md`
- `RELEASE.md`
- `.planning/HANDOFF.md`

`/tpl` mentions rewritten to `/gtr:help`.

### Manual review needed

1. `CLAUDE.md` "Task workflow" section (lines around 15-33) still refers to
   `/task` and `TODO.md`. Both are gone in `v0.5.0`. The replacement is GSD
   (`/gsd:new-project`, `/gsd:plan-phase`, `/gsd:execute-plan`). Rewrite or
   delete the section to match.
2. `README.md` lines 46, 84, 87 list `/task` in the command table and the
   workflow description. Same deal: replace with GSD.
3. `TODO.md.legacy` content (128 lines of active/blocked/done tasks) needs
   to be ported into GSD plans. Run `/gsd:new-project` to bootstrap.
4. `DEFERRED.md.legacy` content stays valid (deferred-work format is
   unchanged); decide whether to keep `.legacy` suffix or rename back. The
   v0.5.0 template no longer ships a default `DEFERRED.md`, so either name
   is fine.

### What was NOT touched

- `backend/`, `frontend/`, `compose*.yml`, `deploy/`, `docs/`, `nginx/`,
  `observability/`, `scripts/`, `assets/`, `.github/workflows/*`, `.env*`,
  `LICENSE`, `CHANGELOG.md`, `ProjectBrief.md`, `Claudecodekickoff.MD`.
- `.claude/.setup-complete`, `.claude/settings.local.json`.
