#!/bin/bash
# Validate before git commit
# Runs linting, security checks, tests

# Read JSON input
input=$(cat)
command=$(echo "$input" | jq -r '.command // empty')

# Only validate git commits
if [[ ! "$command" =~ "git commit" ]]; then
    cat << 'EOF'
{
  "permission": "allow"
}
EOF
    exit 0
fi

# Run validations
echo "🔍 Pre-commit validation..." >&2

# 1. Linting (if applicable)
if [ -f "package.json" ]; then
    npm run lint > /dev/null 2>&1 || echo "⚠️  Linting issues (continuing)" >&2
fi

# 2. Quick test (if applicable)
if [ -f "./gradlew" ]; then
    ./gradlew test --quiet || echo "⚠️  Test failures (review before merging)" >&2
elif [ -f "requirements.txt" ]; then
    pytest --tb=short -x > /dev/null 2>&1 || echo "⚠️  Test failures (review before merging)" >&2
elif [ -f "go.mod" ]; then
    go test ./... > /dev/null 2>&1 || echo "⚠️  Test failures (review before merging)" >&2
fi

# 3. Security scan (basic)
if grep -r "TODO.*SECURITY" . 2>/dev/null | grep -v ".git" | head -1 >&2; then
    echo "⚠️  Security TODOs found - review before production" >&2
fi

# Allow commit (warnings shown, but not blocking)
cat << 'EOF'
{
  "permission": "allow"
}
EOF
exit 0
