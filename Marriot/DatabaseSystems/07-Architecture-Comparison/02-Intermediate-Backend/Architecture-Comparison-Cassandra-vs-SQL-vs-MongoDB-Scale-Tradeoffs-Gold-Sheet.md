# Architecture Comparison Cassandra vs SQL vs MongoDB Scale Tradeoffs - Gold Sheet

> Track File #7 of 30 - Group 02: Intermediate Backend
> For: backend/system design interviews | Level: intermediate | Mode: wide-column scale tradeoffs

## 1. Use Cassandra When

- write volume is huge
- access patterns are predictable
- queries are mostly by partition key
- high availability matters more than rich querying
- data can be denormalized per query
- eventual consistency is acceptable or tunable

---

## 2. Do Not Use Cassandra When

- arbitrary joins are needed
- ad hoc filters are needed
- multi-row transactions are core
- data model is still unknown
- strong global consistency is mandatory

---

## 3. Comparison

| Dimension | SQL | MongoDB | Cassandra |
|---|---|---|---|
| query flexibility | high | medium | low by design |
| transactions | strong | moderate | limited |
| write scale | good with tuning | good | excellent for right model |
| model | normalized relations | document aggregates | query tables by partition key |
| operational risk | familiar | document growth/indexes | tombstones, partitions, compaction |

---

## 4. Strong Interview Answer

```text
I would choose Cassandra only when the query pattern is known and high-scale partition-key reads/writes dominate, such as time-series events, user activity, messages by conversation, or IoT data. If I need joins, ad hoc querying, or strong transactional workflows, SQL or another system is a better fit.
```

---

## 5. Revision Notes

- One-line summary: Cassandra is excellent when the query is known and the partition key is king.
- Three keywords: partition key, denormalize, availability.
- One trap: using Cassandra like a relational database.