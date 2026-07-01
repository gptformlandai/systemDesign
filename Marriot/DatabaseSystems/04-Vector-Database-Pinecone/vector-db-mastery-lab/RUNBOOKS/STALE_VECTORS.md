# Runbook: Stale Vectors

## Symptoms

- deleted document appears in results
- updated policy returns old text
- permission change not reflected
- model version is mixed unexpectedly

## Confirm

1. Check source update timestamp.
2. Check ingestion lag.
3. Check content hash in vector metadata.
4. Check delete event propagation.
5. Check failed embedding jobs.
6. Check reindex completion status.

## Mitigate

- delete known stale records
- replay source events
- pause affected answer flow
- route to old stable index if needed

## Durable Fix

- CDC/event pipeline
- content hashes
- idempotent upserts
- explicit delete handling
- freshness SLO dashboard

## Interview Summary

```text
Vector indexes are derived stores, so freshness and delete propagation need first-class SLOs.
```