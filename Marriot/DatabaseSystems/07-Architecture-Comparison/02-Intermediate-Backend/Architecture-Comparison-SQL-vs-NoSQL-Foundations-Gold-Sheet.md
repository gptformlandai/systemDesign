# Architecture Comparison SQL vs NoSQL Foundations - Gold Sheet

> Track File #5 of 30 - Group 02: Intermediate Backend
> For: backend/system design interviews | Level: intermediate | Mode: relational vs NoSQL foundations

## 1. SQL Strengths

- ACID transactions
- joins and constraints
- flexible ad hoc querying
- mature indexing and query planners
- strong correctness for money, inventory, identity, and workflow state

## 2. SQL Weaknesses

- horizontal scale requires careful partitioning/sharding
- schema changes need discipline
- high-write global scale can be complex
- not best for full-text relevance, graph traversal, or semantic similarity by itself

---

## 3. NoSQL Strengths

- specialized data models
- high scale for specific access patterns
- flexible schema in document stores
- high availability and partition tolerance in some systems
- query-driven modeling

## 4. NoSQL Weaknesses

- fewer joins and constraints
- consistency varies by system
- access patterns must often be known up front
- ad hoc querying can be limited
- operational failure modes differ by engine

---

## 5. Strong Interview Answer

```text
I would not choose SQL or NoSQL by label. I would choose SQL when transactions, constraints, joins, and correctness dominate. I would choose a NoSQL family when the workload has a specialized access pattern like document aggregate reads, massive partition-key writes, full-text search, graph traversal, or vector similarity. The right comparison is workload-specific.
```

---

## 6. Revision Notes

- One-line summary: SQL vs NoSQL is too broad; compare actual workload fit.
- Three keywords: ACID, access pattern, specialization.
- One trap: saying NoSQL means no consistency.