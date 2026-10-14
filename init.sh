#!/usr/bin/env bash
# Development entrypoint used by cursor-harness sessions.
set -euo pipefail
cd "$(dirname "$0")"
chmod +x gradlew
./gradlew test
