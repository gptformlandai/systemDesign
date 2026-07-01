#!/bin/bash
# python-spy-snapshot.sh
# Captures a py-spy stack trace snapshot from a running Python process.
# Usage: ./python-spy-snapshot.sh <PID>
#
# Requires py-spy: pip install py-spy
# On macOS/Linux: may require sudo for processes owned by other users.

set -euo pipefail

TIMESTAMP=$(/bin/date +%Y%m%d-%H%M%S)
OUTPUT_DIR="/tmp/pyspy-dumps"
/bin/mkdir -p "$OUTPUT_DIR"

if [ $# -eq 0 ]; then
  echo "No PID provided. Python processes running:"
  /bin/ps aux | /usr/bin/grep -E "[Pp]ython" | /usr/bin/grep -v grep || echo "  (none found)"
  echo ""
  echo "Usage: $0 <PID>"
  exit 0
fi

PID="$1"

# Validate PID is numeric.
if ! [[ "$PID" =~ ^[0-9]+$ ]]; then
  echo "Error: PID must be a number. Got: $PID"
  exit 1
fi

# Check py-spy is available.
if ! /usr/bin/which py-spy &>/dev/null && ! /usr/local/bin/py-spy --version &>/dev/null 2>&1; then
  echo "py-spy not found. Install with: pip install py-spy"
  exit 1
fi

PYSPY=$( /usr/bin/which py-spy 2>/dev/null || echo "/usr/local/bin/py-spy" )

DUMP_FILE="$OUTPUT_DIR/pyspy-dump-${PID}-${TIMESTAMP}.txt"
FLAME_FILE="$OUTPUT_DIR/pyspy-flame-${PID}-${TIMESTAMP}.svg"

echo "Capturing py-spy thread dump for PID $PID..."
"$PYSPY" dump --pid "$PID" > "$DUMP_FILE" 2>&1
echo "  Thread dump saved: $DUMP_FILE"

echo ""
echo "Capturing 30-second flame graph for PID $PID..."
echo "  (recording CPU profile for 30 seconds)"
"$PYSPY" record -o "$FLAME_FILE" --pid "$PID" --duration 30 2>&1 || \
  echo "  Flame graph capture failed (process may have ended)"

echo ""
echo "Done."
echo "  Thread dump: $DUMP_FILE"
echo "  Flame graph:  $FLAME_FILE"
echo ""
echo "  Open flame graph: open $FLAME_FILE"
