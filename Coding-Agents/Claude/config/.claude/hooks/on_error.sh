#!/bin/bash
# .claude/hooks/on_error.sh
# Runs when Claude Code encounters an error

ERROR_TYPE="${1:-unknown}"
ERROR_MSG="${2:-No error message provided}"

LOG_DIR=".claude/logs"
mkdir -p "$LOG_DIR"

echo "$(date '+%Y-%m-%d %H:%M:%S') ERROR_TYPE=$ERROR_TYPE MSG=$ERROR_MSG" >> "$LOG_DIR/errors.log"

echo ""
echo "=== Claude Error Handler ==="
echo "Type: $ERROR_TYPE"
echo "Message: $ERROR_MSG"
echo "==========================="

case "$ERROR_TYPE" in
  "command_failed")
    echo "A command failed. Check if required tools are installed."
    echo "Recent git status: $(git status --short 2>/dev/null | head -5)"
    ;;
  "file_not_found")
    echo "File not found. Verify the path is correct."
    ;;
  "permission_denied")
    echo "Permission denied. Check file permissions with: ls -la [file]"
    ;;
  "test_failure")
    echo "Tests failing. Fix implementation before continuing."
    echo "Run manually: pytest tests/ -v --tb=short"
    ;;
  *)
    echo "Unhandled error. Check .claude/logs/errors.log for history."
    ;;
esac

echo ""
echo "Recovery options:"
echo "  Restore uncommitted changes: git checkout ."
echo "  See what changed: git diff"
echo "  Error log: cat .claude/logs/errors.log"
echo ""

exit 0
