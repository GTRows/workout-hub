#!/usr/bin/env python3
"""
Codex capability pin tracker.

Codex plugins and connectors are provided by the Codex app/session rather than
installed through a project-local shell command. This script keeps a committed
record of the capabilities the template expects so `/gtr:doctor` can surface a
human-readable checklist without pretending there is a project-local plugin CLI.

Usage:

    python3 .Codex/scripts/plugins.py --pin
    python3 .Codex/scripts/plugins.py --check
    python3 .Codex/scripts/plugins.py
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


PIN_FILE = ".Codex/plugin-pin.json"

RECOMMENDED_CAPABILITIES = [
    {
        "id": "browser@openai-bundled",
        "purpose": "Open, inspect, click, and screenshot local web targets.",
    },
    {
        "id": "github@openai-curated",
        "purpose": "Inspect repositories, PRs, issues, CI, and publish branches.",
    },
    {
        "id": "figma@openai-curated",
        "purpose": "Create diagrams, design files, and design-system artifacts.",
    },
    {
        "id": "documents@openai-primary-runtime",
        "purpose": "Create, edit, render, and verify document artifacts.",
    },
    {
        "id": "presentations@openai-primary-runtime",
        "purpose": "Create, edit, render, and verify presentation decks.",
    },
    {
        "id": "spreadsheets@openai-primary-runtime",
        "purpose": "Create, edit, analyze, and verify spreadsheet artifacts.",
    },
]


def project_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_pin() -> dict:
    return {
        "note": (
            "Codex capabilities are session-provided. Verify availability in the "
            "active Plugins/Skills list; there is no project-local install step."
        ),
        "plugins": [item["id"] for item in RECOMMENDED_CAPABILITIES],
        "capabilities": RECOMMENDED_CAPABILITIES,
    }


def _render(item) -> str | None:
    if isinstance(item, str):
        return item if "@" in item else None
    if isinstance(item, dict):
        name = item.get("name") or item.get("id")
        market = item.get("marketplace") or item.get("source")
        if name and market:
            return f"{name}@{market}"
    return None


def load_pin(root: Path) -> dict:
    f = root / PIN_FILE
    return json.loads(f.read_text(encoding="utf-8")) if f.is_file() else {}


def save_pin(root: Path, plugins: list[str] | None = None) -> Path:
    f = root / PIN_FILE
    f.parent.mkdir(parents=True, exist_ok=True)
    payload = default_pin()
    if plugins is not None:
        payload["plugins"] = plugins
        payload["capabilities"] = [
            {"id": plugin, "purpose": "Pinned manually."} for plugin in plugins
        ]
    f.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return f


def diff(installed: list[str], pinned: list[str]) -> tuple[list[str], list[str]]:
    """Return (missing_locally, extra_locally). Kept for tests and compatibility."""
    pinned_set = set(pinned)
    installed_set = set(installed)
    return sorted(pinned_set - installed_set), sorted(installed_set - pinned_set)


def print_check(pin: dict) -> None:
    plugins = pin.get("plugins", [])
    note = pin.get("note")
    if note:
        print(note)
    if not plugins:
        print("no plugin-pin.json found — run --pin to seed the Codex capability checklist")
        return
    print("expected Codex capabilities:")
    for plugin in plugins:
        print(f"  {plugin}")
    print("verify these against the active Plugins/Skills list in the current Codex session")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pin", action="store_true", help="write the default Codex capability checklist")
    parser.add_argument("--check", action="store_true", help="print the pinned Codex capability checklist")
    args = parser.parse_args()

    root = project_root()

    if args.pin:
        path = save_pin(root)
        print(f"wrote {path.relative_to(root)} ({len(RECOMMENDED_CAPABILITIES)} capabilities pinned)")
        return 0

    pin = load_pin(root)
    if args.check:
        print_check(pin)
        return 0

    if not pin:
        print("no plugin-pin.json — run --pin to seed")
        return 0
    print("\n".join(pin.get("plugins", [])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
