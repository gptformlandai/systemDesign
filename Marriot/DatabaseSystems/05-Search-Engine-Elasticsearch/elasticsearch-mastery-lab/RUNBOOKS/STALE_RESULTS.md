# Runbook: Stale Results

## Symptoms

- search shows old price, inventory, status, or permissions
- database has newer data
- users report inconsistent search/detail views

## Confirm

1. Check source-of-truth value.
2. Check document in Elasticsearch by ID.
3. Check sync event/outbox/CDC status.
4. Check bulk indexing failures.
5. Check refresh behavior and freshness SLO.
6. Check alias points to expected index.

## Mitigate

- serve critical fields from source of truth on detail page
- replay failed events
- force controlled refresh only if justified
- temporarily hide stale documents if unsafe

## Durable Fix

- deterministic IDs
- idempotent sync
- freshness lag alert
- failed bulk item retry queue
- alias and reindex validation

## Interview Summary

```text
Stale search is usually a sync, refresh, or alias issue. Define freshness SLOs and monitor lag instead of pretending search is instantly consistent.
```