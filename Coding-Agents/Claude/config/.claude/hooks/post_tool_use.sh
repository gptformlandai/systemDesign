#!/bin/bash
# .claude/hooks/post_tool_use.sh
# Runs after every tool execution in Claude Code

TOOL_TYPE="${1}"
TOOL_OUTPUT="${2:-}"
EXIT_CODE="${3:-0}"

LOG_DIR=".claude/logs"
mkdir -p "$LOG_DIR"

# ─── LOG TOOL EXECUTIONS ──────────────────────────────────────────────────────
echo "$(date '+%Y-%m-%d %H:%M:%S') TOOL=$TOOL_TYPE EXIT=$EXIT_CODE" >> "$LOG_DIR/tool_history.log"

# ─── CHECK FOR SECRETS IN MODIFIED FILES ─────────────────────────────────────
if [[ "$TOOL_TYPE" == "edit" ]]; then
  CHANGED=$(git diff --name-only 2>/dev/null)
  if [[ -n "$CHANGED" ]]; then
    # Simple pattern-based secret scan (gitleaks is better if installed)
    if echo "$CHANGED" | xargs grep -l \
      -E "(api[_-]?key|secret[_-]?key|password|private[_-]?key|token)" \
      2>/dev/null | grep -v "test\|spec\|\.md$\|example" > /dev/null; then
      echo "POST_HOOK WARNING: Potential secret pattern in modified files."
      echo "Review before committing: git diff"
    fi
  fi
fi

# ─── QUICK REGRESSION CHECK AFTER EDITS ──────────────────────────────────────
if [[ "$TOOL_TYPE" == "edit" ]] && command -v pytest &>/dev/null; then
  # Run only the fastest tests (unit tests) for quick feedback
  RESULT=$(pytest tests/unit/ -x --tb=line -q 2>&1 | tail -3)
  if echo "$RESULT" | grep -q "failed\|error"; then
    echo "POST_HOOK ALERT: Tests failing after edit:"
    echo "$RESULT"
    echo "Claude should fix test failures before continuing."
  fi
fi

exit 0
