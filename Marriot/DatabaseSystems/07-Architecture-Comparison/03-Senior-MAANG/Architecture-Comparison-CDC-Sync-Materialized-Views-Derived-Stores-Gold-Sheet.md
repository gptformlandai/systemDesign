# Architecture Comparison CDC, Sync, Materialized Views, and Derived Stores - Gold Sheet

> Track File #16 of 30 - Group 03: Senior / MAANG
> For: backend/data architecture interviews | Level: senior | Mode: CDC, sync, derived indexes

## 1. The Sync Problem

Many architectures use specialized read stores:

```text
source DB -> CDC/events -> search index/cache/vector index/graph projection/warehouse
```

The hard part is correctness under retry, deletion, ordering, and failure.

---

## 2. Patterns

| Pattern | Use | Risk |
|---|---|---|
| transactional outbox | reliable event publication from source DB | relay lag |
| CDC | database log-based replication to downstream | schema evolution, ordering |
| event sourcing | event log is source of truth | query projection complexity |
| materialized view | optimized query shape | refresh lag |
| batch rebuild | recover/reindex derived store | stale during rebuild |

---

## 3. Failure Modes

- dual-write partial failure
- out-of-order events
- duplicate events
- missed deletes
- stale permissions
- schema mismatch
- poison messages
- reindex inconsistencies

---

## 4. Production Checklist

- idempotent consumers
- deterministic record IDs
- versioned schemas
- dead-letter handling
- replay from checkpoint
- lag monitoring
- delete propagation tests
- rebuild path from source of truth

---

## 5. Interview Summary

```text
I would avoid direct dual writes when possible. The source of truth should commit state and emit changes through an outbox, CDC, or event stream. Derived stores like search, vector, graph, cache, and analytics systems must be idempotent, monitored for lag, able to replay changes, and rebuildable from source.
```

---

## 6. Revision Notes

- One-line summary: Derived-store correctness depends on replayable, idempotent sync.
- Three keywords: outbox, CDC, replay.
- One trap: forgetting delete and permission propagation.