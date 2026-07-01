#!/usr/bin/env bash
set -euo pipefail

cat <<'TEMPLATE'
# gRPC Streaming Risk Review

Method:
Stream type: server/client/bidi

## Contract
- Ordering guarantee:
- Resume token:
- Duplicate behavior:
- Heartbeat behavior:
- Max stream duration:
- Message size limit:

## Operations
- Cancellation handling:
- Slow-consumer behavior:
- Buffer limits:
- Reconnect behavior:
- Metrics:
- Alerts:

## Failure Practice
- client disconnects
- server deploy drains stream
- proxy idle timeout
- receiver stops reading
- duplicate event after reconnect
TEMPLATE