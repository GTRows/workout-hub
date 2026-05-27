# Hooks

Hooks are shell commands that run automatically at lifecycle events. They are configured in `.Codex/settings.json`.

## Active hooks in this template

| Hook | Event | What it does |
|------|-------|--------------|
| `pre_guard_release_files.py` | PreToolUse Write/Edit | Blocks editing protected release/build files |
| `pre_guard_security.py` | PreToolUse Write/Edit | Blocks dangerous patterns (innerHTML, eval, SQL injection, etc.) |
| `pre_guard_env_secrets.py` | PreToolUse Write/Edit | Blocks hardcoded secrets and writing to .env files |
| `post_validate_syntax.py` | PostToolUse Write/Edit | Validates syntax (Python, JS, JSON) after writes |
| `session_check_setup.py` | SessionStart | Injects a `/gtr:setup` reminder when the marker is missing |
| `pre_check_setup.py` | UserPromptSubmit | Injects the same reminder on every prompt until setup is done (does not block) |
| `session_log_usage.py` | SessionEnd | Appends a JSONL line per session to `.Codex/usage-log.jsonl` (gitignored) |

## Optional hooks (opt-in)

Located under `.Codex/hooks/optional/`. Not registered in `settings.json` by default. Enable per project by copying the registration snippet into `settings.json`.

| Hook | Use case |
|------|----------|
| `pre_warn_win32_danger.py` | Windows projects using `winreg` / `ctypes` / `SystemParametersInfo`. Warns on HKLM writes, UAC elevation, advapi32 calls. |

## Hook exit codes

- `0` = allow, continue normally
- `2` = **block** + send stderr to Codex as feedback (Codex will retry/fix)
- other = non-fatal warning (shown in verbose mode only)

## Adding a new hook

1. Create a Python script in `.Codex/hooks/`
2. Read JSON from `sys.stdin` — it contains `tool_name` and `tool_input`
3. Print error message to `sys.stderr` and `sys.exit(2)` to block
4. Register it in `.Codex/settings.json` under the right event
5. Add a matching test under `.Codex/hooks/tests/test_<name>.py`

```python
import json, sys

data = json.load(sys.stdin)
tool_input = data.get("tool_input", {})
# ... your logic ...
sys.exit(0)  # allow
```

## Available events

| Event | When |
|-------|------|
| `PreToolUse` | Before a tool runs (can block) |
| `PostToolUse` | After a tool succeeds |
| `UserPromptSubmit` | Before Codex processes your message |
| `Stop` | After Codex finishes responding |
| `Notification` | When Codex needs your attention |
| `SessionStart` | On session start/resume |
| `SessionEnd` | On session shutdown (token usage available in stdin) |

## Environment variables Codex provides to hooks

| Variable | Value |
|----------|-------|
| `CODEX_PROJECT_DIR` | Absolute path to project root |
| `CODEX_SESSION_ID` | Current session ID, when provided by the host |
| `CODEX_TOOL_NAME` | Name of the tool being called, when provided by the host |

Hooks in this repo also fall back to `CLAUDE_PROJECT_DIR` for compatibility with older migrated configs.
