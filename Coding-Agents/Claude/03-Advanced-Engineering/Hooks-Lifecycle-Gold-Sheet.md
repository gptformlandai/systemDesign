# Hooks and Lifecycle Events — Gold Sheet

> **Track**: Claude Mastery Track — Group 3: Advanced Engineering
> **File**: 3 of 7 (Track File #16)
> **Read after**: Skills-System-Gold-Sheet.md

---

## 1. What Hooks Are

### Must Know

```
Hooks are shell scripts that Claude Code runs at specific lifecycle points.

Location: .claude/hooks/

Three hook points:
  pre_tool_use.sh    → Runs BEFORE Claude executes any tool call
  post_tool_use.sh   → Runs AFTER Claude executes a tool call
  on_error.sh        → Runs when Claude encounters an error

Hook return codes:
  exit 0  → allow the operation to proceed
  exit 1  → BLOCK the operation (Claude cannot proceed until fixed)

Why hooks matter:
  Without hooks: Claude can run any command, modify any file, execute any script.
  With hooks: every tool use goes through your validation gates.
  
  Hooks are your safety layer for autonomous Claude sessions.
```

---

## 2. pre_tool_use.sh — Validate Before Execution

### What It Validates

```bash
#!/bin/bash
# .claude/hooks/pre_tool_use.sh

# $1 = tool type (bash, edit, read, etc.)
# $2 = tool arguments/content

TOOL_TYPE="$1"
TOOL_ARGS="$2"

# ─── BLOCK DANGEROUS COMMANDS ─────────────────────────────────────────────────

DANGEROUS_PATTERNS=(
  "rm -rf /"
  "rm -rf ~"
  "DROP DATABASE"
  "DROP TABLE"
  "git push --force"
  "git reset --hard"
  "kubectl delete"
  "terraform destroy"
  "> /dev/null 2>&1 ; "  # command injection attempt
  "curl.*|.*bash"         # piping curl to bash
  "wget.*|.*bash"         # piping wget to bash
)

for pattern in "${DANGEROUS_PATTERNS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qiE "$pattern"; then
    echo "HOOK BLOCKED: Dangerous pattern detected: $pattern"
    echo "Run manually if this is intentional."
    exit 1
  fi
done

# ─── REQUIRE CHECKPOINT COMMIT FOR FILE EDITS ──────────────────────────────────

if [[ "$TOOL_TYPE" == "edit" ]]; then
  # Check if there's a recent checkpoint commit
  LAST_COMMIT=$(git log -1 --format="%s" 2>/dev/null)
  if ! echo "$LAST_COMMIT" | grep -q "checkpoint:"; then
    UNCOMMITTED=$(git status --porcelain | wc -l | tr -d ' ')
    if [[ "$UNCOMMITTED" -gt "5" ]]; then
      echo "WARNING: Many uncommitted changes. Consider: git add . && git commit -m 'checkpoint:'"
      # Not blocking — just warning
    fi
  fi
fi

# ─── BLOCK MIGRATION COMMANDS ──────────────────────────────────────────────────

MIGRATION_PATTERNS=(
  "alembic upgrade"
  "alembic downgrade"
  "rails db:migrate"
  "npx prisma migrate"
  "flyway migrate"
)

for pattern in "${MIGRATION_PATTERNS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qi "$pattern"; then
    echo "HOOK BLOCKED: Database migration command detected."
    echo "Run migrations manually: $TOOL_ARGS"
    echo "Review the migration file first, then run manually."
    exit 1
  fi
done

# ─── BLOCK PRODUCTION ENVIRONMENT ACCESS ───────────────────────────────────────

PROD_INDICATORS=(
  "--env production"
  "ENV=production"
  "ENVIRONMENT=production"
  "prod.company.com"
  "production.db"
)

for indicator in "${PROD_INDICATORS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qi "$indicator"; then
    echo "HOOK BLOCKED: Production environment indicator detected."
    echo "Claude should not access production systems directly."
    exit 1
  fi
done

# All checks passed
exit 0
```

---

## 3. post_tool_use.sh — Validate After Execution

```bash
#!/bin/bash
# .claude/hooks/post_tool_use.sh

TOOL_TYPE="$1"
TOOL_OUTPUT="$2"

# ─── VERIFY TESTS STILL PASS AFTER FILE EDITS ─────────────────────────────────

if [[ "$TOOL_TYPE" == "edit" ]]; then
  # Run a quick test to catch regressions immediately
  if command -v pytest &>/dev/null; then
    echo "Running quick regression check..."
    pytest tests/unit/ -x --tb=short -q 2>&1 | tail -5
    if [ ${PIPESTATUS[0]} -ne 0 ]; then
      echo "WARNING: Tests failing after edit. Claude should fix this."
    fi
  fi
fi

# ─── CHECK FOR SECRETS IN MODIFIED FILES ──────────────────────────────────────

if [[ "$TOOL_TYPE" == "edit" ]]; then
  # Scan recently modified files for potential secrets
  if command -v gitleaks &>/dev/null; then
    CHANGED_FILES=$(git diff --name-only 2>/dev/null)
    if [[ -n "$CHANGED_FILES" ]]; then
      gitleaks detect --no-git --path . --source "$CHANGED_FILES" 2>/dev/null
      if [ $? -ne 0 ]; then
        echo "WARNING: Potential secret detected in modified files."
        echo "Review before committing."
      fi
    fi
  fi
fi

exit 0
```

---

## 4. on_error.sh — Handle Errors Gracefully

```bash
#!/bin/bash
# .claude/hooks/on_error.sh

ERROR_TYPE="$1"
ERROR_MESSAGE="$2"

echo "=== Claude Error Hook ==="
echo "Error type: $ERROR_TYPE"
echo "Error: $ERROR_MESSAGE"
echo "========================="

# Log the error for debugging
mkdir -p .claude/logs
echo "$(date): $ERROR_TYPE — $ERROR_MESSAGE" >> .claude/logs/errors.log

# Specific error handling
case "$ERROR_TYPE" in
  "command_failed")
    echo "A command failed. Check if required tools are installed."
    echo "Recent git status:"
    git status --short 2>/dev/null
    ;;
  "file_not_found")
    echo "File not found. Check if the path is correct."
    ;;
  "permission_denied")
    echo "Permission denied. Do not run Claude as root."
    ;;
  *)
    echo "Unknown error. Check .claude/logs/errors.log for history."
    ;;
esac

exit 0
```

---

## 5. Hook Best Practices

```
Design principle: hooks should be fast.
  A hook that takes 10 seconds runs on EVERY tool call.
  Keep pre_tool_use.sh under 1 second.

Design principle: hooks should be explicit about what they block.
  When blocking: echo a clear message explaining WHY.
  Without a clear message, Claude and the developer don't know what happened.

Design principle: hooks should not replace CLAUDE.md.
  CLAUDE.md: Claude understands and follows (behavioral)
  Hooks: system-level enforcement (mechanical)
  Use both: CLAUDE.md says "don't run migrations", hook blocks migrations anyway.

Design principle: test hooks before relying on them.
  bash .claude/hooks/pre_tool_use.sh "bash" "rm -rf /tmp/test"
  Expected: exit 1 with BLOCKED message
```

---

## 6. Revision Checklist

- [ ] Has all 3 hook files in `.claude/hooks/`
- [ ] pre_tool_use.sh blocks: rm -rf, DROP TABLE, git push --force, migrations, production access
- [ ] post_tool_use.sh checks for secrets in modified files
- [ ] on_error.sh logs errors and provides helpful context
- [ ] Has tested hooks manually to verify they fire correctly
- [ ] Hooks are fast (under 1 second for pre_tool_use)
- [ ] Understands exit 0 (allow) vs exit 1 (block)
