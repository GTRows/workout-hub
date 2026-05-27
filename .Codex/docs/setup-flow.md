# First-time setup flow

When you clone this template into a new project, the setup state is:

1. `.Codex/.setup-complete` does not exist.
2. `AGENTS.md` has `PROJECT_NAME` and placeholder blocks.
3. `IDENTITY.yaml` has placeholder values.
4. `.planning/` does not exist (created later if the user opts into GSD).
5. The `SessionStart` and `UserPromptSubmit` hooks inject a soft reminder into Codex's context (no blocking).

The reminder tells Codex to recommend `/gtr:setup` before starting implementation work. It does not stop the user from asking read-only questions or doing template maintenance.

## What `/gtr:setup` does

1. **Asks for conversation language first** — before any other prompt. Whatever the user picks is bound in `AGENTS.md` `## Communication`, and every later question in this wizard plus all subsequent slash-command output (`/gtr:help`, `/gtr:doctor`, `/gsd:*`) follow that language. Code, identifiers, and commit messages stay English regardless.
2. Detects the stack from `package.json`, `pyproject.toml`, `go.mod`, `Cargo.toml`, etc.
3. Fills `IDENTITY.yaml` with detected values (name, version, license, icon path, platforms).
4. Fills `AGENTS.md` placeholders. Sections it cannot fill from current state become lines in `.Codex/setup-followups.md`.
5. Asks fork-vs-primary; appends the right branch strategy block.
6. Asks about project i18n (separate from the conversation language in step 1 — this is about the app's UI strings).
7. Checks Codex session capabilities and writes `.Codex/plugin-pin.json` as a human-readable checklist; no external plugin CLI commands are run.
8. Hands off planning to GSD on opt-in: `/gsd:new-project` next, with the brief auto-filled from `IDENTITY.yaml` + `AGENTS.md` + `README.md`.
9. Optional CI / release scaffolding (opt-in).
10. Optional extra scaffolding (`/gtr:setup --extras` for LICENSE, SECURITY.md, CONTRIBUTING.md, CODEOWNERS, dependabot, pre-commit, `.env.example`).
11. Hooks sanity check.
12. Writes `.Codex/.setup-complete` (gitignored) and `.Codex/.template-manifest.json` (committed).

After `/gtr:setup` finishes, the reminder hook goes silent and normal work proceeds. `/gtr:setup` is idempotent — re-running it refreshes detected values.
