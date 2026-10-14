#!/bin/bash
# Block git commits if secrets detected
# Based on Cursor hooks spec: https://cursor.com/docs/agent/hooks

# Read JSON input
input=$(cat)
command=$(echo "$input" | jq -r '.command // empty')

# Check if this is a git commit
if [[ "$command" =~ "git commit" ]] || [[ "$command" =~ "git add" ]]; then
    # Scan for secrets
    if grep -r -i "api[_-]key.*=.*['\"]" . 2>/dev/null | grep -v ".git" | grep -v "hooks" | grep -v ".env.example" | head -1; then
        cat << 'EOF'
{
  "permission": "deny",
  "user_message": "⚠️  Potential API key detected in code!",
  "agent_message": "Git operation blocked: Found potential API key in code. Never commit secrets! Use environment variables instead. Remove the secret and try again."
}
EOF
        exit 0
    fi
    
    if grep -r -i "password.*=.*['\"]" . 2>/dev/null | grep -v ".git" | grep -v "hooks" | grep -v ".env.example" | head -1; then
        cat << 'EOF'
{
  "permission": "deny",
  "user_message": "⚠️  Potential password detected in code!",
  "agent_message": "Git operation blocked: Found potential password in code. Never commit secrets! Use environment variables instead. Remove the password and try again."
}
EOF
        exit 0
    fi
fi

# No secrets found - allow
cat << 'EOF'
{
  "permission": "allow"
}
EOF
exit 0
