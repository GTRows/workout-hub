---
name: "source-command-gtr-new-rule"
description: "[TEMPLATE] Scaffold a path-scoped AGENTS.md rule under .agents/rules/."
---

# source-command-gtr-new-rule

Use this skill when the user asks to run the migrated source command `gtr-new-rule` or `/gtr:new-rule`.

## Command Template

Create a new path-scoped rule for: `$ARGUMENTS`

**Output language.** Read `## Communication` from `AGENTS.md` and conduct the interview and any explanatory output in that language. The rule file content itself stays in English (it is loaded into context as instructions). File paths and frontmatter keys stay verbatim. If `## Communication` is missing, default to the language the user wrote the request in.

Path-scoped rules let you keep `AGENTS.md` short by storing focused guidance in tracked files. Codex does not automatically load arbitrary rule files in every environment, so link important rules from `AGENTS.md`. They live under `.agents/rules/<slug>.md` and use frontmatter to declare their scope.

Use a rule when you have guidance that applies to a specific subset of the project (e.g. "tests must use pytest fixtures") instead of cluttering top-level `AGENTS.md` with rules that are 90% irrelevant to most files.

## Steps

1. Slugify the title from `$ARGUMENTS` (lowercase, hyphenated, ASCII).
2. Refuse if `.agents/rules/<slug>.md` already exists.
3. Ask the user for the path glob(s) the rule applies to (e.g. `tests/**/*.py`, `src/api/**`).
4. Create `.agents/rules/<slug>.md` using the template below.
5. Tell the user the rule is scaffolded and should be linked from `AGENTS.md` if it must be active in every session.

## Template

```markdown
---
paths:
  - "<glob 1>"
  - "<glob 2>"
---

# <Title>

Short paragraph describing the scope and why this rule exists.

## Rules

- Rule 1: concrete, actionable. Example: "Use pytest fixtures, not setUp/tearDown."
- Rule 2: ...

## Anti-patterns

- <what NOT to do — Codex's likely default that this rule overrides>

## When this rule does NOT apply

- <edge cases or sibling paths that are intentionally excluded>
```

## Conventions

- One rule file = one concern. If the rule starts saying "and also", split.
- Keep rules concrete. "Use pytest fixtures" beats "write idiomatic tests".
- Reference plugin commands or other rules with explicit names, not vague pointers.
- Rules under `.agents/rules/` are tracked in git. They are not personal — they are project conventions.

## When NOT to make a rule

- The guidance is universal across the whole project → put it in `AGENTS.md` instead.
- The guidance is your personal preference → put it in `~/.Codex/AGENTS.md`.
- The guidance is a one-time decision → use `/gtr:new-adr` instead (decisions live in ADRs, ongoing rules live here).

## Report

- Path to the new rule file.
- The path globs you set.
- Suggested next step: open the file and fill the Rules section, then commit.
