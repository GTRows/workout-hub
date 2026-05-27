# Custom command skills and Codex capabilities

## Custom slash commands

Files under `.Codex/commands/gtr/*.md` are the durable source prompts for the template commands. Codex loads the runnable forms through matching `.agents/skills/source-command-gtr-*` skills. Keep both in sync when adding or changing a command.

## Included in this template

| Command | Purpose |
|---------|---------|
| `/gtr:menu` | Interactive entry point — pick what to do, Codex routes to the right command. |
| `/gtr:setup` | First-time project wizard. Detects stack, fills AGENTS.md, checks Codex capabilities, hands off planning to GSD, writes setup marker. |
| `/gtr:onboard` | Interactive runbook to merge the template into an existing project. |
| `/gtr:update` | Pull template updates from upstream and merge non-destructively. |
| `/gtr:doctor` | Read-only health check. |
| `/gtr:release <ver>` | Prepare a release: bump IDENTITY.yaml, rotate CHANGELOG, commit, tag. Never pushes. |
| `/gtr:help` | Detailed reference for `/gtr:*` and `/gsd:*` commands. |
| `/gtr:set-language <lang>` | Updates the conversation language binding in `AGENTS.md`. |
| `/gtr:next` | Reads project state and recommends the next command. |
| `/gtr:orchestrate [scope]` | Runs planner/executor/verifier subagents across phase plans. |
| `/gtr:new-adr` | Creates an Architecture Decision Record under `docs/adr/`. |
| `/gtr:new-migration` | Creates a new DB migration file following the project's conventions. |
| `/gtr:new-rule` | Scaffolds a project rule under `.agents/rules/`. |
| `/gtr:new-skill` | Scaffolds a project skill under `.agents/skills/`. |

All template commands have `[TEMPLATE]` as the first word in their frontmatter description so they are easy to distinguish from app/plugin commands.

Name project commands to avoid collisions with Codex app/plugin commands. Use template-prefixed names such as `/gtr:*`.

## Recommended Codex capabilities

Codex capabilities are session-provided by the Codex app and plugin system. This template does not shell-install external plugin CLIs. `/gtr:setup` records the expected capability checklist in `.Codex/plugin-pin.json`; `/gtr:doctor` prints it for comparison with the active Plugins/Skills list.

| Capability | What it does |
|------------|--------------|
| `browser@openai-bundled` | Browser automation for localhost and file-based UI verification. |
| `github@openai-curated` | Repository, issue, PR, and CI workflows. |
| `figma@openai-curated` | Figma diagrams, design files, libraries, and Code Connect work. |
| `documents@openai-primary-runtime` | Word/document artifact workflows. |
| `presentations@openai-primary-runtime` | PPTX / slide deck workflows. |
| `spreadsheets@openai-primary-runtime` | Spreadsheet analysis and workbook generation. |

Repo-local skills live under `.agents/skills/`. The migrated `/gtr:*` commands are exposed as `source-command-gtr-*` skills, and the project-specific UI guidance lives in `frontend-design`.

If a user explicitly asks to use a missing installable Codex connector, request that connector through the Codex install flow. Otherwise continue with the available tools.

## Task trackers (compatible, not bundled)

Vibe Kanban and similar external task boards are compatible with this template's `.planning/` flow — they are not mutually exclusive. Treat external boards as the source of truth for cross-team work; keep `.planning/` for the handful of items the AI assistant is actively driving. Do not mirror one into the other.

## Useful Codex built-ins

| Command | What it does |
|---------|-------------|
| `/memory` | View and manage Codex's memory and loaded rules |
| `/mcp` | Check MCP server status |
| `/config` | Edit settings |
| `/clear` | Clear context (new session) |
| `/compact` | Compress context when it gets long |
| `Shift+Tab` | Toggle auto-accept edits mode |
| `Ctrl+R` | Interrupt Codex mid-response |
