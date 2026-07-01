# Lab 07: Operations Incident Drills

Goal: rehearse production debugging language for Neo4j incidents.

---

## Drill 1: Slow Traversal

Symptom:

```text
Fraud traversal p99 jumps after adding a new signal source.
```

Check:

- exact Cypher and parameters
- EXPLAIN/PROFILE
- new signal cardinality
- supernode risk
- path depth
- constraints/indexes

---

## Drill 2: Cartesian Product

Symptom:

```text
Query latency spikes and PROFILE shows row explosion.
```

Check:

- disconnected MATCH patterns
- missing shared variables
- missing filters
- row count after each operator

---

## Drill 3: Stale Graph Projection

Symptom:

```text
Graph recommendation ignores new purchases from the source database.
```

Check:

- source event emitted
- ingestion lag
- failed writes
- idempotency and constraints
- freshness SLO

---

## Completion Gate

For each incident, explain:

1. Most likely graph path.
2. Evidence to gather.
3. Immediate mitigation.
4. Durable model/query/operations fix.
5. Alert that should catch it next time.