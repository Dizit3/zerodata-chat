#!/bin/bash
# Run Android unit tests when a cursor-harness / cursor-agent session stops.
if [ ! -f "./gradlew" ]; then
  exit 0
fi
echo "Running ./gradlew test..." >&2
./gradlew test --quiet || echo "⚠️  Gradle tests failed (review before merging)" >&2
exit 0
