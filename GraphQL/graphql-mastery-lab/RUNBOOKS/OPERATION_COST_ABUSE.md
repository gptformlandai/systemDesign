# Runbook: GraphQL Operation Cost Abuse

## Symptoms

- CPU or latency spikes from few operations
- deeply nested queries
- large page sizes
- anonymous/public traffic overloads GraphQL

## Evidence

- operation hash/name
- depth and complexity score
- variables and page size
- caller identity/IP/client
- resolver fanout and data-source calls

## Mitigate

- reject operation with cost limit
- lower max page size
- require persisted queries
- rate-limit by identity and operation
- disable abusive anonymous access

## Prevent

- depth/complexity limits
- persisted operation allowlist
- operation-aware rate limiting
- dashboard of top expensive operations