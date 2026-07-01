# VectorDB Freshness, CDC, Sync, and Reindexing - Gold Sheet

> Track File #13 of 30 - Group 03: Senior / MAANG
> For: backend/data platform interviews | Level: senior | Mode: ingestion, freshness, reindexing

## 1. Freshness Problem

Vector indexes are usually derived stores.

```text
source system -> ingestion pipeline -> chunking -> embedding -> vector DB
```

If the source changes but the vector index does not, retrieval becomes stale.

---

## 2. Sync Events

Handle:

- create document
- update document
- delete document
- permission change
- model version change
- metadata correction
- source outage/replay

---

## 3. Reindex Strategy

For embedding model upgrades:

1. create new index/namespace or versioned collection
2. backfill embeddings
3. run offline evaluation
4. shadow query or canary traffic
5. switch reads gradually
6. keep rollback path
7. remove old version after confidence

---

## 4. Freshness Metrics

- ingestion lag
- embedding queue depth
- failed chunk count
- stale document rate
- delete propagation latency
- ACL propagation latency
- reindex completion percentage

---

## 5. Interview Summary

```text
Vector DBs are derived indexes, so I would design ingestion with idempotent upserts, explicit deletes, CDC or event streams, content hashes, model-version metadata, freshness metrics, and reindex rollouts. Embedding upgrades require side-by-side evaluation and a rollback path because old and new vectors may not be comparable.
```

---

## 6. Revision Notes

- One-line summary: Stale vectors are stale product behavior.
- Three keywords: CDC, delete, reindex.
- One trap: updating source permissions without updating vector metadata.