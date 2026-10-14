#!/usr/bin/env python3
"""Copy vendored v3.2.1 prompt .md files into the installed cursor_harness package.

pip install of nirmalarya/cursor-harness@v3.2.1 omits prompts/*.md from the wheel.
"""
from __future__ import annotations

import shutil
import sys
from pathlib import Path


def main() -> int:
    repo = Path(__file__).resolve().parents[1]
    src = repo / "vendor" / "cursor-harness-3.2.1" / "prompts"
    if not src.is_dir():
        print(f"Missing vendored prompts: {src}", file=sys.stderr)
        return 1
    try:
        import cursor_harness
    except ImportError:
        print("cursor_harness is not installed. pip install -r requirements-harness.txt", file=sys.stderr)
        return 1
    dest = Path(cursor_harness.__file__).resolve().parent / "prompts"
    dest.mkdir(parents=True, exist_ok=True)
    copied = 0
    for path in src.glob("*.md"):
        shutil.copy2(path, dest / path.name)
        copied += 1
    needed = dest / "enhancement_coding.md"
    if not needed.is_file():
        print(f"Copy failed, still missing {needed}", file=sys.stderr)
        return 1
    print(f"Copied {copied} prompt files to {dest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
