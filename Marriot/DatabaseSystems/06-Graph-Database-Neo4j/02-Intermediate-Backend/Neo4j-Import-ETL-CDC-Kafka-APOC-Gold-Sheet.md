# Neo4j Import, ETL, CDC, Kafka, and APOC - Gold Sheet

> Track File #9 of 30 - Group 02: Intermediate Backend
> For: backend/data interviews | Level: intermediate to senior | Mode: ingestion, batch writes, source-of-truth sync

This sheet builds:
- How data enters Neo4j
- Import, ETL, CDC, and Kafka patterns
- Idempotent graph write design

---

## 1. Source-Of-Truth Thinking

Neo4j is often either:

- the primary operational graph database
- a derived graph projection from SQL/MongoDB/Kafka/CDC

Always state which one it is.

```text
If Neo4j is derived, it must be rebuildable and monitored for freshness.
```

---

## 2. CSV And Batch Import

Use CSV/import tooling for initial loads and controlled backfills.

Batch import checklist:

- create constraints first
- load nodes before relationships
- use stable IDs
- batch writes
- validate counts
- sample traversal correctness

---

## 3. Event And CDC Sync

Patterns:

- application events
- outbox table
- Kafka topics
- CDC from source database
- scheduled reconciliation jobs

Graph write rule:

```text
Use MERGE on constrained IDs for idempotent node and relationship creation.
```

---

## 4. APOC Positioning

APOC provides many utility procedures for import, transformation, metadata, and graph operations.

Interview maturity:

```text
APOC is useful, but production designs should still explain source contracts, retry behavior, security posture, and operational support.
```

---

## 5. Strong Answer

```text
For Neo4j ingestion, I create identity constraints first, load nodes before relationships, use stable IDs, and write idempotent Cypher with MERGE. For ongoing sync, I prefer events, outbox, Kafka, or CDC depending on the source system. I monitor lag, failed events, duplicate identities, and traversal correctness because graph quality depends on relationship freshness and entity resolution.
```

---

## 6. Revision Notes

- One-line summary: Neo4j ingestion succeeds when identity, idempotency, and relationship freshness are controlled.
- Three keywords: MERGE, CDC, freshness.
- One interview trap: loading relationships before identity constraints.
- Memory trick: nodes first, relationships second, traversals verified third.