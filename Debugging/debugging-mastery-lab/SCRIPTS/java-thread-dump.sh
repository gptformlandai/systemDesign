#!/bin/bash
# java-thread-dump.sh
# Captures a jstack thread dump from a running Java process.
# Usage: ./java-thread-dump.sh <PID>
#   or:  ./java-thread-dump.sh  (auto-detect if one JVM is running)

set -euo pipefail

TIMESTAMP=$(/bin/date +%Y%m%d-%H%M%S)
OUTPUT_DIR="/tmp/thread-dumps"
/bin/mkdir -p "$OUTPUT_DIR"

if [ $# -eq 0 ]; then
  echo "No PID provided. Detecting running JVM processes..."
  /usr/bin/find /proc -maxdepth 2 -name "cmdline" 2>/dev/null | \
    /usr/bin/xargs -I{} /bin/grep -l "java" {} 2>/dev/null || true
  
  # Use jps to list JVM processes.
  echo "JVM processes (jps -l):"
  /usr/bin/jps -l 2>/dev/null || echo "jps not found. Install JDK."
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

OUTPUT_FILE="$OUTPUT_DIR/thread-dump-${PID}-${TIMESTAMP}.txt"

echo "Capturing thread dump for PID $PID..."
echo "Output: $OUTPUT_FILE"

# Take 3 thread dumps 10 seconds apart for trend analysis.
for i in 1 2 3; do
  {
    echo ""
    echo "=== Thread Dump $i of 3 at $(/bin/date) ==="
    echo ""
    /usr/bin/jstack "$PID"
    echo ""
  } >> "$OUTPUT_FILE"
  
  if [ "$i" -lt 3 ]; then
    echo "  Dump $i complete. Waiting 10 seconds for dump $((i+1))..."
    /bin/sleep 10
  fi
done

echo "Done. Thread dumps saved to: $OUTPUT_FILE"
echo ""
echo "Quick analysis:"
echo "  Deadlocks:     grep -A 30 'deadlock' $OUTPUT_FILE"
echo "  BLOCKED threads: grep 'State: BLOCKED' $OUTPUT_FILE | wc -l"
echo "  WAITING threads: grep 'State: WAITING' $OUTPUT_FILE | wc -l"
echo "  Thread count:    grep '\"' $OUTPUT_FILE | grep -c 'prio=' || true"
