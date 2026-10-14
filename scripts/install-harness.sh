#!/usr/bin/env bash
# Install the pinned cursor-harness (v3.2.1) for this repo.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 -m pip install --user -r "$ROOT/requirements-harness.txt"
export PATH="${HOME}/.local/bin:${PATH}"
cursor-harness --version
echo "Ready. Run enhancements with:"
echo "  cursor-harness enhance \"$ROOT\" --spec \"$ROOT/docs/development/enhancement_spec.txt\""
