#!/bin/bash
# .claude/hooks/pre_tool_use.sh
# Runs before every tool execution in Claude Code
# exit 0 = allow, exit 1 = block

TOOL_TYPE="${1}"
TOOL_ARGS="${2:-}"

# ─── BLOCK IRRECOVERABLE DESTRUCTIVE COMMANDS ─────────────────────────────────
DESTRUCTIVE_PATTERNS=(
  "rm -rf /"
  "rm -rf ~"
  "rm -rf \$HOME"
  ": > /dev/sda"
  "mkfs"
  "dd if=/dev/zero"
)
for pat in "${DESTRUCTIVE_PATTERNS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qF "$pat"; then
    echo "PRE_HOOK BLOCKED: Irrecoverable destructive command: $pat"
    exit 1
  fi
done

# ─── BLOCK DATABASE MUTATIONS ─────────────────────────────────────────────────
DB_DANGEROUS=(
  "DROP TABLE"
  "DROP DATABASE"
  "DROP SCHEMA"
  "TRUNCATE TABLE"
  "DELETE FROM"      # without WHERE is dangerous
)
for pat in "${DB_DANGEROUS[@]}"; do
  if echo "${TOOL_ARGS^^}" | grep -qF "${pat^^}"; then
    # Allow if it has a WHERE clause (targeted delete)
    if ! echo "${TOOL_ARGS^^}" | grep -qF "WHERE"; then
      echo "PRE_HOOK BLOCKED: Dangerous DB command without WHERE: $pat"
      exit 1
    fi
  fi
done

# ─── BLOCK DATABASE MIGRATIONS ────────────────────────────────────────────────
MIGRATION_CMDS=(
  "alembic upgrade"
  "alembic downgrade"
  "rails db:migrate"
  "npx prisma migrate"
  "flyway migrate"
  "liquibase update"
)
for pat in "${MIGRATION_CMDS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qi "$pat"; then
    echo "PRE_HOOK BLOCKED: Database migration command."
    echo "Run manually after reviewing: $TOOL_ARGS"
    exit 1
  fi
done

# ─── BLOCK GIT HISTORY REWRITES ───────────────────────────────────────────────
GIT_DANGEROUS=(
  "git push --force"
  "git push -f "
  "git reset --hard"
  "git rebase --force"
  "git filter-branch"
  "git filter-repo"
)
for pat in "${GIT_DANGEROUS[@]}"; do
  if echo "$TOOL_ARGS" | grep -qi "$pat"; then
    echo "PRE_HOOK BLOCKED: Potentially destructive git operation: $pat"
    echo "Run manually if intentional."
    exit 1
  fi
done

# ─── BLOCK CURL/WGET PIPE TO SHELL ────────────────────────────────────────────
if echo "$TOOL_ARGS" | grep -qE "(curl|wget).*(bash|sh|zsh|python)"; then
  echo "PRE_HOOK BLOCKED: Piping remote content to shell is a security risk."
  exit 1
fi

# ─── WARN ON MANY UNCOMMITTED CHANGES + LARGE EDIT ───────────────────────────
if [[ "$TOOL_TYPE" == "edit" ]]; then
  UNCOMMITTED=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
  if [[ "$UNCOMMITTED" -gt "20" ]]; then
    echo "PRE_HOOK WARNING: $UNCOMMITTED uncommitted changes. Consider:"
    echo "  git add . && git commit -m 'checkpoint: before claude session'"
    # Not blocking — just informational
  fi
fi

exit 0
