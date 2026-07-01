# Architecture Comparison Pro Gap Fill: Decision Review Checklist

> Track File #30 of 30 - Group 06: Practice Upgrade
> For: staff-level architecture reviews | Level: pro / MAANG | Mode: review checklist and scoring rubric

## 1. Staff-Level Review Questions

1. What product workflow is being optimized?
2. What is the exact access pattern?
3. What is the source of truth?
4. What are derived stores and why are they needed?
5. What consistency is required?
6. What is the p99 latency SLO?
7. What is the write/read scale?
8. What is the partitioning strategy?
9. What are hot-key/hot-shard risks?
10. How do data changes propagate?
11. How do deletes and permissions propagate?
12. How is the system rebuilt after corruption?
13. What are backup, RPO, and RTO plans?
14. What is encrypted and audited?
15. What is the cost model?
16. What alternative was rejected and why?

---

## 2. Scoring Rubric

| Score | Meaning |
|---:|---|
| 0 | database chosen by popularity |
| 1 | access pattern named, weak production reasoning |
| 2 | pros/cons covered, limited failure handling |
| 3 | source/derived stores, sync, SLOs, and failure modes covered |
| 4 | staff-level answer with alternatives, cost, security, DR, and migration plan |

---

## 3. Red Flags

- no source of truth named
- no consistency model named
- no failure mode named
- no backup/restore plan
- no derived-store freshness plan
- no delete/permission propagation
- no cost model
- no operational owner
- no rejected alternatives

---

## 4. Strong Final Answer

```text
I would approve this datastore choice only if the access pattern, source of truth, derived stores, consistency, partitioning, SLOs, CDC, freshness, backup/DR, security, cost, and rejected alternatives are explicit. A system design answer is not complete when the database is named; it is complete when the operational tradeoffs are defensible.
```

---

## 5. Revision Notes

- One-line summary: Staff-level comparison means defending the whole data architecture lifecycle.
- Three keywords: source, SLO, recovery.
- One trap: no plan to rebuild derived stores.