#!/usr/bin/env python3
"""Compatibility wrapper for the renamed AGENTS.md checker."""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from agents_md_check import main

if __name__ == "__main__":
    raise SystemExit(main())
