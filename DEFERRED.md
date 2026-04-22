# Deferred Work

Work that is intentionally postponed. Not abandoned — just not now.

Each entry must answer four questions. If you cannot, it is not "deferred", it is "undecided". Keep it out of this file.

## Format

```
## <short title>
- **Why deferred:** concrete reason it is not being done now (cost, risk, missing info, blocked on other work)
- **Trigger:** specific, observable event that turns this back on (version bump, library reaches stable, user count threshold, bug report, etc.)
- **Owner:** person or role responsible for reassessing
- **Added:** YYYY-MM-DD
```

Keep entries terse. Move to an issue tracker once the trigger fires.

Do not use TODO comments in code instead of this file. Code-level TODOs rot silently; entries here have an owner and a trigger.

## Entries

## Real project icon at `assets/icon.png`

- **Why deferred:** needs design input (logo, palette); no graphics tooling available in the current environment to generate a 512x512+ PNG from a source SVG
- **Trigger:** before the first public release OR when the user supplies an icon asset to place
- **Owner:** project owner (product/branding)
- **Added:** 2026-04-22
