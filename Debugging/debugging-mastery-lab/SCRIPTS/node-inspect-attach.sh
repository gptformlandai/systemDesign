#!/bin/bash
# node-inspect-attach.sh
# Enables the V8 inspector on a running Node.js process (no restart needed).
# Then prints the Chrome DevTools URL.
# Usage: ./node-inspect-attach.sh <PID>
#   or:  ./node-inspect-attach.sh  (list running Node processes)

set -euo pipefail

if [ $# -eq 0 ]; then
  echo "No PID provided. Running Node.js processes:"
  /bin/ps aux | /usr/bin/grep -E "[Nn]ode" | /usr/bin/grep -v grep || echo "  (none found)"
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

# Verify process exists.
if ! /bin/kill -0 "$PID" 2>/dev/null; then
  echo "Error: No process with PID $PID"
  exit 1
fi

# Verify it is a Node.js process.
PROC_NAME=$( /bin/ps -p "$PID" -o comm= 2>/dev/null || echo "unknown" )
if [[ "$PROC_NAME" != *"node"* ]]; then
  echo "Warning: Process $PID appears to be '$PROC_NAME', not 'node'."
  echo "Continuing anyway..."
fi

echo "Sending SIGUSR1 to Node.js process $PID (enables inspector)..."
/bin/kill -USR1 "$PID"

/bin/sleep 1  # brief pause for inspector to initialize

echo ""
echo "Inspector enabled. Connection options:"
echo ""
echo "  Chrome DevTools URL:"
echo "    chrome://inspect"
echo "    -> Click 'Open dedicated DevTools for Node'"
echo "    -> Or: Configure target and add localhost:9229"
echo ""
echo "  VS Code: add this to launch.json and press F5:"
echo '    {'
echo '      "name": "Node: Attach (PID '$PID')",'
echo '      "type": "node",'
echo '      "request": "attach",'
echo '      "port": 9229,'
echo '      "skipFiles": ["<node_internals>/**"]'
echo '    }'
echo ""
echo "  Direct WebSocket URL (if needed):"
echo "    ws://127.0.0.1:9229/<uuid>"
echo "    (get UUID from: curl http://localhost:9229/json)"
