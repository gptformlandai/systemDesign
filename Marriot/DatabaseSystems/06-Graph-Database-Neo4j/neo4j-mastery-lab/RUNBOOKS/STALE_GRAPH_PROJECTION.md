# Runbook: Stale Graph Projection

## Symptoms

- graph recommendations miss recent events
- fraud graph ignores a new risky device/card
- GraphRAG returns old entity relationships
- source database has newer data than Neo4j

## Confirm

1. Check source-of-truth value/event.
2. Check ingestion job or Kafka/CDC offset.
3. Check failed writes and retry queue.
4. Check constraints and idempotency.
5. Check freshness SLO and last successful sync.

## Mitigate

- replay failed events
- temporarily fall back to source system for critical checks
- mark stale graph results as degraded
- pause risky automated decisions if graph freshness is unsafe

## Durable Fix

- idempotent MERGE writes
- lag alerts
- reconciliation jobs
- dead-letter handling
- graph quality validation queries

## Interview Summary

```text
Stale graph projections are sync-quality failures. Define freshness SLOs and monitor lag instead of assuming the graph is always current.
```