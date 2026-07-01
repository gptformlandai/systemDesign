# Architecture Comparison Polyglot Persistence System Architecture - MAANG Sheet

> Track File #13 of 30 - Group 03: Senior / MAANG
> For: senior architecture interviews | Level: senior | Mode: multiple stores, boundaries, consistency

## 1. Core Idea

Polyglot persistence means using different datastores for different access patterns in one system.

```text
PostgreSQL for source of truth + Redis for cache + Elasticsearch for search + VectorDB for RAG + warehouse for analytics
```

This is powerful, but every extra store adds synchronization and operational risk.

---

## 2. Good Reasons To Add A Store

- current source database cannot meet query latency
- full-text relevance is needed
- semantic similarity is needed
- graph traversal is needed
- analytics scans should not hit OLTP
- hot reads need cache
- object/blob storage is cheaper and more durable

---

## 3. Bad Reasons To Add A Store

- trend-driven architecture
- unclear access pattern
- avoiding data modeling
- premature scale assumptions
- adding search/vector/graph without freshness plan
- no team capacity to operate it

---

## 4. Boundary Pattern

```text
source service owns writes -> emits events/CDC -> derived stores update -> read APIs expose specialized queries
```

Rules:

- one source of truth per entity
- derived stores are rebuildable
- writes are idempotent
- deletes and permissions propagate
- freshness is measured

---

## 5. Interview Summary

```text
I would use polyglot persistence when one datastore cannot satisfy all access patterns safely. The source of truth should own correctness, while cache, search, vector, graph, and analytics stores are derived and rebuildable. The hard part is not adding databases; it is designing ownership, CDC, freshness, deletes, security, observability, and recovery.
```

---

## 6. Revision Notes

- One-line summary: Polyglot persistence solves query mismatch but creates sync complexity.
- Three keywords: source, derived, rebuildable.
- One trap: writing to multiple databases in-line without outbox/CDC/retry semantics.