# Lab 07: Production Incident Debugging

## Goal

Practice evidence-first GraphQL incident response.

## Scenario

```text
GraphQL latency and partial errors spike after a client release.
```

## Evidence Checklist

- operation names/hashes
- client version
- validation vs execution errors
- error paths
- resolver spans
- data-source call counts
- complexity/depth
- recent schema and resolver changes

## Mitigation Options

- rollback persisted operation or client release
- disable expensive optional field
- restore previous schema or supergraph
- add batching
- tighten complexity/depth limits
- rate-limit abusive operation

## RCA Template

```text
Impact:
Trigger:
Affected operation:
Field path:
Evidence:
Mitigation:
Prevention:
```

## Interview Takeaway

```text
GraphQL incidents should be scoped by operation and resolver path, then tied to data-source calls, auth/cache behavior, schema changes, and client impact.
```