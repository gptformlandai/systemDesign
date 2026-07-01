# Runbook: Cartesian Product

## Symptoms

- query rows explode unexpectedly
- PROFILE shows Cartesian product
- latency and memory spike
- query returns combinations instead of connected facts

## Confirm

1. Inspect MATCH clauses for disconnected patterns.
2. Check missing shared variables.
3. Check WHERE conditions that should connect patterns.
4. Check row counts after each operator.

## Mitigate

- connect patterns through shared variables
- split query into subqueries
- add explicit filters
- add early LIMIT where semantically correct

## Durable Fix

- redesign query shape
- add model relationships that express the domain path
- add constraints/indexes for anchors
- add query-review tests for important APIs

## Interview Summary

```text
A Cartesian product usually means the query matched disconnected graph patterns and multiplied rows.
```