# Neo4j Performance, Query Plans, EXPLAIN, and PROFILE - Gold Sheet

> Track File #10 of 30 - Group 02: Intermediate Backend
> For: backend/data/system design interviews | Level: intermediate to senior | Mode: query plans, cardinality, performance debugging

This sheet builds:
- How to reason about Neo4j query cost
- EXPLAIN/PROFILE usage
- Common performance failure modes

---

## 1. Performance First Principle

Most Neo4j performance problems come from:

```text
missing anchor index, broad label scan, unbounded traversal, Cartesian product, high fan-out, supernode, too much returned data, lock contention, or bad graph model
```

---

## 2. EXPLAIN vs PROFILE

| Tool | Use |
|---|---|
| EXPLAIN | show planned query without running it |
| PROFILE | run query and show actual operators, rows, db hits |

Use `PROFILE` carefully on expensive production-like queries.

---

## 3. What To Inspect

| Signal | Meaning |
|---|---|
| label scan | may need index or better anchor |
| node index seek | good selective start |
| rows | cardinality growth |
| db hits | work done by operator |
| Cartesian product | disconnected pattern or missing join condition |
| expand all | traversal expansion |
| eager | materialization boundary, sometimes costly |

---

## 4. Tuning Checklist

- add constraints/indexes for anchors
- start from selective nodes
- specify labels and relationship types
- bound variable-length paths
- limit early inside subqueries
- avoid returning whole paths unless needed
- split read-heavy analytics from transactional paths
- review graph model if query is awkward

---

## 5. Strong Answer

Question:

> A Neo4j query is slow. How do you debug it?

Strong answer:

```text
I start with the exact Cypher and parameters, then inspect EXPLAIN/PROFILE for the starting operator, cardinality growth, db hits, Cartesian products, and traversal expansion. I check whether the query starts from an indexed anchor, whether labels and relationship types are explicit, whether variable-length paths are bounded, and whether high-degree nodes create fan-out. The durable fix may be an index, query rewrite, subquery limit, relationship redesign, or graph model change.
```

---

## 6. Revision Notes

- One-line summary: Neo4j performance is anchor selectivity plus bounded traversal plus graph shape.
- Three keywords: PROFILE, db hits, fan-out.
- One interview trap: adding hardware before checking the query plan.
- Memory trick: find the anchor, then watch rows multiply.