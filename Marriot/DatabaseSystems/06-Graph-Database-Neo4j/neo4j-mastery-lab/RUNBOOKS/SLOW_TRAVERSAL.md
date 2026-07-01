# Runbook: Slow Traversal

## Symptoms

- p99 query latency spike
- slow query logs show graph expansion
- user-facing graph API times out
- CPU or memory increases during traversal-heavy queries

## Confirm

1. Identify endpoint, Cypher, and parameters.
2. Run EXPLAIN/PROFILE in a safe environment.
3. Check starting anchor and index usage.
4. Check relationship types and depth.
5. Check row counts, db hits, and returned payload.
6. Check supernode/high-degree nodes.

## Mitigate

- reduce traversal depth
- add LIMIT inside subqueries
- disable expensive relationship types temporarily
- route analytics away from user-facing paths
- return IDs instead of large paths if possible

## Durable Fix

- add/repair constraints and indexes
- rewrite Cypher
- split relationship types
- add intermediate nodes or precomputed scores
- change graph model if the query is structurally awkward

## Interview Summary

```text
Slow traversal is debugged from Cypher shape, anchor selectivity, fan-out, and graph model evidence.
```