---
name: "source-command-gtr-setup"
description: "[TEMPLATE] First-time project setup wizard. Detects stack, fills AGENTS.md, checks Codex capabilities, hands off planning to GSD, writes setup marker."
---

# source-command-gtr-setup

Use this skill when the user asks to run the migrated source command `gtr-setup` or `/gtr:setup`.

## Command Template

You are the **template setup wizard**. Run this once per project. It is idempotent — safe to re-run to refresh detected values.

**Output budget.** Follow `.Codex/docs/output-style.md`. Each step asks one question or makes one announcement — no recap, no narration. The final summary stays under ~10 lines.

`$ARGUMENTS` may include `--extras`. When present, **skip steps 1–12 entirely** and run only step 13 (Optional scaffolding) plus the closing manifest refresh and summary. Step 1 (conversation language) still applies — read the existing `## Communication` section in `AGENTS.md` and continue in that language. Only re-ask the language question if no `## Communication` section exists yet.

Do not narrate each step while running. Execute them, then print the summary in the final step.

---

## 1. Conversation language (ask before anything else)

This is the very first action in the wizard, **before the preflight check, before stack detection, before any other question**. Every later prompt, summary, confirmation, and slash-command output the user sees must follow the chosen language.

Ask exactly:

> What language should I speak with you in this project? (English / Türkçe / Deutsch / Français / Español / ... — pick one)

Default suggestion: detect from the language the user wrote `/gtr:setup` in and propose it as the default. If the user typed in Turkish, default to Türkçe; if in English, default to English; etc.

Whatever the user picks, append a binding rule to `AGENTS.md` under a new `## Communication` section (create the section if it does not exist):

```
- Speak with the user in <Language>. All conversational responses,
  prompts, summaries, questions, and slash-command output (including
  /gtr:help, /gtr:doctor, /gsd:* commands) must be in this language.
- Code, identifiers, comments, commit messages, and file contents
  must always be in English regardless of conversation language.
- Status lines, table headers, and command names stay verbatim
  (e.g. `/gtr:setup`, `IDENTITY.yaml`) — do not translate them.
```

This rule overrides any default. Do not switch languages mid-project unless the user re-runs `/gtr:setup`.

**From this point on, every prompt and summary in the rest of this wizard is in the chosen language.** Section headers in this file stay English (they are instructions to Codex, not to the user).

If the wizard is being re-run and `## Communication` already exists, skip this step silently and continue in the already-configured language — but still confirm with one short sentence: "Continuing in <Language>." Then proceed.

---

## 2. Preflight

Check if `.Codex/.setup-complete` exists. Use the **Read** tool, not shell commands. (Bash on Windows is Git Bash; PowerShell-style `Test-Path` will fail.) If the Read returns "file does not exist", treat that as Missing — do not retry with another shell.

- **Exists** → tell the user setup was already done (show `date` from the marker). Ask if they want to re-run. If no, stop.
- **Missing** → continue.

---

## 3. Detect the stack

Do NOT ask the user what the stack is. Detect it by reading files. Run these in parallel:

- `package.json` (Node/JS/TS — inspect `dependencies`, `scripts`, `engines`)
- `pyproject.toml` / `requirements.txt` / `Pipfile` (Python)
- `go.mod` (Go)
- `Cargo.toml` (Rust)
- `pom.xml` / `build.gradle` / `build.gradle.kts` (Java/Kotlin)
- `composer.json` (PHP)
- `Gemfile` (Ruby)
- Frontend hints: `index.html`, `next.config.*`, `vite.config.*`, `tsconfig.json`, `tailwind.config.*`
- Containerization: `Dockerfile`, `docker-compose.*`
- CI: `.github/workflows/`
- Directory layout: `src/`, `app/`, `backend/`, `frontend/`, `api/`, `web/`
- `README.md` first paragraph for a one-line description

Summarize detection in one short paragraph. **Confirm with the user** before proceeding. Absorb corrections.

---

## 4. Fill `IDENTITY.yaml` (single source of truth)

`IDENTITY.yaml` at repo root is the canonical identity and release config. Every other manifest derives from it.

If missing, copy the template skeleton. Then fill:

| Field | Source |
|-------|--------|
| `identity.name` | kebab-case from directory name, `package.json#name`, or `pyproject.toml#project.name`. Must match repo slug. |
| `identity.display_name` | Ask. Propose Title-Cased `name` as default. |
| `identity.description` | README first paragraph, or ask. |
| `identity.homepage` | `package.json#homepage`, else empty. |
| `identity.license` | `LICENSE` header or `package.json#license`. Ask if ambiguous. |
| `identity.icon` | `assets/icon.*`, `static/icon.*`, `src-tauri/icons/`. If none, leave as `assets/icon.png` and add a line to `.Codex/setup-followups.md`. |
| `version` | Highest of `package.json#version` / `pyproject.toml` / `Cargo.toml`. Default `0.1.0`. Flag drift. |
| `release.platforms` | Infer from stack (Electron → windows+macos; CLI binary → windows-x64+linux-x64+macos-arm64; web-only → empty). |

If any derived manifest disagrees with `IDENTITY.yaml#version` after filling, sync it to `IDENTITY.yaml`'s value and add a line to `.Codex/setup-followups.md` to verify downstream.

---

## 5. Fill `AGENTS.md`

Replace placeholder blocks using values from `IDENTITY.yaml`:

- `PROJECT_NAME` → `identity.name`
- `Description` → `identity.description`
- `Tech Stack` → from detection
- `Platform` → from detection

**Development Commands**: pull from `package.json#scripts`, Python entry points, `Makefile`, etc. Placeholders you cannot fill confidently stay as-is and become follow-ups captured in `.Codex/setup-followups.md`.

**Sections to leave placeholder** when the code doesn't tell you — add a line to `.Codex/setup-followups.md` instead of fabricating:
- `Architecture / Entry Flow`
- `Module Breakdown`
- `Data Storage`
- Git commit scopes

If `PROJECT_NAME` appears as literal text elsewhere (README, docs), replace it too.

---

## 6. Branch strategy (fork or primary)

Ask exactly one question:

> Is this repo a **fork** of an upstream project, or your **primary/own** repo?

Append to `AGENTS.md` under a new `## Branch Strategy` section:

- **Fork**:
  ```
  This is a fork. `main` tracks upstream.
  - Never commit directly to `main`.
  - Use `feature/<name>` or `fix/<name>` branches.
  - One PR per feature. Rebase onto upstream before opening.
  ```
- **Primary**:
  ```
  This is the primary repo. Use feature branches for non-trivial work;
  small fixes can go on `main` if the project convention allows.
  ```

---

## 7. Commit authorship

Append under `## Git and Commits` in `AGENTS.md`:

```
- Commits are authored by the user via local git config.
  Do NOT add `Co-Authored-By: Codex` trailers.
```

---

## 8. Project localization (i18n)

Ask:

> Will this project ship to end users in multiple languages? (yes / no / later)

- **no** → no further action.
- **later** → add a `setup-i18n` follow-up line to `.Codex/setup-followups.md`: "i18n strategy decided".
- **yes** → ask one follow-up: `Which UI languages will you support? (comma-separated, e.g. en, tr, de)`. Then append to `AGENTS.md` under `## Localization`:
  ```
  - Supported UI languages: <list>.
  - Default / fallback: <first in list>.
  - Translation source format: TBD (gettext, JSON, ICU, ...) — set when picking the i18n library.
  - User-facing strings must NOT be hard-coded. Wrap them in the
    project's translation function.
  ```
  Add `pick-i18n-lib` follow-up to `.Codex/setup-followups.md`: "i18n library installed, default + at least one non-default locale rendering".

Note: this is *product* localization (the app's UI), independent of step 1 (which controls the language Codex speaks to the developer).

---

## 9. Check Codex capabilities

Codex plugins and connectors are provided by the active Codex app/session, not installed through a project-local shell command. Do **not** run external plugin CLI install commands.

Tell the user which Codex capabilities are useful for this project and whether they are visible in the current session's plugin/skill list:

| Capability | Purpose |
|------------|---------|
| `browser@openai-bundled` | Open, inspect, click, and screenshot local web targets. |
| `github@openai-curated` | Inspect repositories, PRs, issues, CI, and publish branches. |
| `figma@openai-curated` | Create diagrams, design files, and design-system artifacts. |
| `documents@openai-primary-runtime` | Create, edit, render, and verify document artifacts. |
| `presentations@openai-primary-runtime` | Create, edit, render, and verify presentation decks. |
| `spreadsheets@openai-primary-runtime` | Create, edit, analyze, and verify spreadsheet artifacts. |

If the user explicitly asks to use a connector that is not active but is installable in this Codex session, use `request_plugin_install`. Do not install broad or adjacent tools silently.

For UI-heavy projects, prefer the repo-local `frontend-design` skill under `.agents/skills/frontend-design/` plus the Browser plugin for screenshot verification.

Refresh the documented capability pin after setup:

```bash
python3 .Codex/scripts/plugins.py --pin
```

This writes `.Codex/plugin-pin.json` as a committed checklist. `/gtr:doctor` prints it for humans to compare against the active Codex Plugins/Skills list.

## 10. Planning hand-off (GSD)

Ask exactly:

> Do you want to use **GSD** for planning and execution? (recommended)
> - **yes**   → I will run `/gsd:new-project` next (and `/gsd:map-codebase` first if this is an existing codebase). The brief is auto-filled from `IDENTITY.yaml` / `AGENTS.md` / `README.md`; you only confirm or tweak.
> - **no**    → skip planning. Template still works for hooks, releases, onboarding, doctor.
> - **later** → captured in `.Codex/setup-followups.md` (gitignored). `/gtr:doctor` surfaces it.

For every `AGENTS.md` section you could **not** fill in step 5, capture it in `.Codex/setup-followups.md` (one line per gap with the section name). These become natural seeds for the first GSD plan when the user later runs `/gsd:plan-phase 1`.

Do **NOT** create `TODO.md` or `DEFERRED.md` — both are removed in v0.2.0. Planning lives under `.planning/` (managed by GSD) or not at all.

---

## 11. CI scaffolding (opt-in)

Ask:

> Do you want CI (lint + test on push/PR) wired up? (yes / no / later)

- **no** → skip.
- **later** → add `setup-ci` follow-up to `.Codex/setup-followups.md`: "CI scaffolding decision revisited".
- **yes** →
  1. If `.github/workflows/ci.yml` is missing and `ci.yml.template` exists, copy template → `ci.yml`. Do NOT delete the `.template`.
  2. Tell the user which `REPLACE` markers to customise (runtime setup, lint command, test command).
  3. Add `ci-verify` follow-up to `.Codex/setup-followups.md`: "push a dummy commit to a branch and confirm the CI job runs green".

---

## 12. Release scaffolding (opt-in)

Ask:

> Do you want release automation for this project? (yes / no / later)

- **no** → skip entirely. Do not create `CHANGELOG.md`, `RELEASE.md`, or the workflow.
- **later** → add `setup-release` follow-up to `.Codex/setup-followups.md`: "release scaffolding decision revisited".
- **yes** →
  1. If `CHANGELOG.md` is missing at repo root, copy the template skeleton.
  2. If `RELEASE.md` is missing at repo root, copy the template skeleton.
  3. If `.github/workflows/release.yml` is missing and `release.yml.template` exists, copy template → `release.yml`. Do NOT delete the `.template`.
  4. Tell the user which `REPLACE` markers to customise (test command, per-platform build command).
  5. Reconcile `IDENTITY.yaml#release.platforms` with the workflow matrix. Ask which platforms to enable; prune the rest from both files.
  6. Add `release-verify` follow-up to `.Codex/setup-followups.md`: "dry-run a pre-release tag (e.g. `v0.0.1-rc.1`) and confirm the draft release appears on GitHub".

---

## 13. Optional scaffolding (opt-in, batched)

Ask the user once, listing every option:

> Want any of these scaffolding files? Reply with comma-separated numbers, `all`, or `none`.
>
>   1. `LICENSE` (MIT default — ask the year and copyright holder)
>   2. `SECURITY.md` (vulnerability disclosure policy)
>   3. `CONTRIBUTING.md` (PR / commit / branching guidance from this AGENTS.md)
>   4. `.github/CODEOWNERS` (auto-request reviewers)
>   5. `.github/dependabot.yml` (weekly dependency updates)
>   6. `.pre-commit-config.yaml` (pre-commit framework — runs lint/format on staged files)
>   7. `.env.example` (env-var template; only meaningful if the project uses dotenv)

For each picked option:

- `LICENSE` → write the MIT text with `<year>` / `<owner>` filled in. Update `IDENTITY.yaml#identity.license` to `MIT`.
- `SECURITY.md` → minimal disclosure policy (email contact, expected response time, supported versions table — fill from `IDENTITY.yaml`).
- `CONTRIBUTING.md` → derive from this AGENTS.md's `Git and Commits`, `Code Standards`, `File Organization` sections.
- `.github/CODEOWNERS` → ask for the user's GitHub handle, default `* @<handle>`.
- `.github/dependabot.yml` → infer ecosystem from detection in step 3 (npm, pip, cargo, gomod, ...).
- `.pre-commit-config.yaml` → infer hooks from stack (ruff for Python, prettier for JS, etc.); leave a comment block of suggested hooks the user can opt into.
- `.env.example` → only if the project already imports `dotenv` / uses `os.environ.get` patterns. Scan top-level source for variable names and stub them with empty values.

For files the user did not pick, do nothing — do not add follow-ups for them.

---

## 14. Hooks sanity check

- Read `PROTECTED_EXACT` in `.Codex/hooks/pre_guard_release_files.py`. If the project has lock or manifest files not yet listed (`Cargo.lock`, `yarn.lock`, `pnpm-lock.yaml`, `Pipfile.lock`, `poetry.lock`, `go.sum`, ...), suggest additions and wait for user confirmation. Do NOT edit the hook without approval.
- If the project uses Windows-specific native APIs (detected `winreg` / `ctypes` imports, `SystemParametersInfo`), offer to register `.Codex/hooks/optional/pre_warn_win32_danger.py` in `.Codex/settings.json`.

---

## 15. Write the setup marker

Create `.Codex/.setup-complete` with exactly this content (no trailing whitespace):

```
version: 1
date: <today's ISO date, YYYY-MM-DD>
stack: <one-line stack summary from step 3>
type: <fork|primary>
language: <conversation language picked in step 1>
```

Ensure `.Codex/.setup-complete` is listed in `.gitignore` — the marker is per-clone, not per-repo.

Then write the template manifest so `/gtr:update` can later detect which files the user has modified:

```bash
python3 .Codex/scripts/manifest.py --write
```

Add `.Codex/.template-manifest.json` to git so it ships with the project.

---

## 16. Summary

Print a short summary (in the chosen conversation language):

- Conversation language locked in (step 1)
- What you filled in `AGENTS.md`
- Follow-ups captured in `.Codex/setup-followups.md`
- Plugins installed
- Fork or primary
- Whether CI and release scaffolding were activated
- Next step — usually `/gsd:new-project` (if planning was opted in) or `/gtr:menu` for the option list
