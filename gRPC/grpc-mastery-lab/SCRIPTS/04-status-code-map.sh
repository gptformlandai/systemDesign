#!/usr/bin/env bash
set -euo pipefail

cat <<'MAP'
gRPC Status Code Map

INVALID_ARGUMENT    malformed or invalid request independent of system state
NOT_FOUND           requested resource does not exist
ALREADY_EXISTS      create conflicts with existing resource
FAILED_PRECONDITION system state blocks operation
ABORTED             concurrency or transaction conflict
RESOURCE_EXHAUSTED  quota/capacity/rate limit reached
UNAUTHENTICATED     missing or invalid identity
PERMISSION_DENIED   identity is valid but not allowed
UNAVAILABLE         transient service/connectivity/dependency unavailability
DEADLINE_EXCEEDED   caller time budget expired
INTERNAL            unexpected server bug or invariant failure
UNIMPLEMENTED       method not implemented or wrong service/method
MAP