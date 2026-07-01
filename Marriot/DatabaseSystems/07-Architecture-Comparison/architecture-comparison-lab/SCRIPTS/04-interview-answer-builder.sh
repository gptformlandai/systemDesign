#!/usr/bin/env bash
set -euo pipefail

cat <<'TEXT'
Architecture comparison answer builder

1. Requirement:
   What workflow matters most?

2. Access pattern:
   Point lookup, transaction, range scan, search, graph traversal, vector similarity, cache read, blob fetch, or analytics?

3. Source of truth:
   Which system owns correctness?

4. Derived stores:
   Which systems serve specialized reads?

5. Sync:
   Outbox, CDC, event stream, batch rebuild, cache invalidation?

6. Tradeoffs:
   Consistency, latency, scale, cost, security, operations, failure modes?

7. Final decision:
   Pick the primary system, name derived systems, and state when you would change the choice.
TEXT