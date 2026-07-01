# Architecture Comparison Neo4j vs Joins vs Search vs Vector - Gold Sheet

> Track File #9 of 30 - Group 02: Intermediate Backend
> For: graph/system design interviews | Level: intermediate | Mode: graph tradeoffs

## 1. Use Neo4j When

- relationship traversal is the core query
- paths explain the result
- depth and fan-out are bounded
- graph algorithms matter
- fraud rings, permissions, lineage, dependencies, or knowledge graphs dominate

---

## 2. Compare To Alternatives

| Need | Best Fit |
|---|---|
| foreign-key joins over moderate relational data | SQL |
| full-text relevance | Elasticsearch/OpenSearch |
| semantic similarity | Vector DB |
| explicit multi-hop relationship path | Neo4j/graph DB |
| offline large-scale graph analytics | graph processing engine or data platform |

---

## 3. Production Risks

- supernodes
- unbounded traversal
- stale graph projection
- missing anchor indexes
- tenant/path permission leaks
- poor relationship semantics

---

## 4. Interview Summary

```text
I would choose Neo4j when the business question is relationship traversal, such as fraud rings, permissions, lineage, recommendations, or service dependencies. If the query is simple joins, SQL may be enough. If it is text relevance, use search. If it is semantic similarity, use vector search. The graph choice must justify path traversal and bounded fan-out.
```

---

## 5. Revision Notes

- One-line summary: Graph DBs are for path questions, not just connected-looking data.
- Three keywords: path, traversal, fan-out.
- One trap: replacing simple relational joins with a graph database unnecessarily.