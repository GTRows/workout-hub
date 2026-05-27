# Tips index

This file is a thin index. The long-form reference lives under `.Codex/docs/`.

| Topic | File |
|-------|------|
| Hooks (active, optional, exit codes, events, env vars) | [.Codex/docs/hooks.md](docs/hooks.md) |
| MCP servers (install, scopes) | [.Codex/docs/mcp.md](docs/mcp.md) |
| Custom command skills and Codex capabilities | [.Codex/docs/commands.md](docs/commands.md) |
| Permissions (`allow` / `ask` / `deny` layers) | [.Codex/docs/permissions.md](docs/permissions.md) |
| Releases (tag pipeline, IDENTITY.yaml, CHANGELOG) | [.Codex/docs/releases.md](docs/releases.md) |
| First-time setup flow | [.Codex/docs/setup-flow.md](docs/setup-flow.md) |
| Deferred work (GSD ISSUES.md) | [.Codex/docs/deferred-work.md](docs/deferred-work.md) |
| AGENTS.md best practices | [.Codex/docs/agents-md-best-practices.md](docs/agents-md-best-practices.md) |
| Workflow tips (Plan Mode, sub-agents, Windows) | [.Codex/docs/workflow.md](docs/workflow.md) |

For per-command help see `/gtr:help <command>`. For per-topic guides see `/gtr:help <topic>` (planning, release, hooks, manifest, migration, onboarding, permissions).

## Configuration files at a glance

```
~/.Codex/settings.json          # Personal settings (all projects)
~/.Codex/AGENTS.md              # Personal instructions (all projects)
.Codex/settings.json            # Project settings (tracked in git)
.Codex/settings.local.json      # Local overrides (gitignored)
.Codex/hooks/                   # Hook scripts (registered by default)
.Codex/hooks/optional/          # Optional hooks (opt-in, not registered)
.Codex/commands/gtr/            # Source prompts for template `/gtr:*` command skills
.agents/skills/                 # Repo-local Codex skills
.Codex/scripts/                 # Helper scripts (manifest, migrations)
.Codex/docs/                    # Long-form reference (this directory)
.agents/rules/                  # Optional topic-split AGENTS.md rules
AGENTS.md                        # Project instructions (tracked in git)
IDENTITY.yaml                    # Identity and release config (tracked)
.planning/                       # GSD planning artifacts (optional, opt-in)
```
