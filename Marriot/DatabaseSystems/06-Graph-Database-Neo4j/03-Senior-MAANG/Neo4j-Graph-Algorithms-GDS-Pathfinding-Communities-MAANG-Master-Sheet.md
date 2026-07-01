# Neo4j Graph Algorithms, GDS, Pathfinding, and Communities - MAANG Master Sheet

> Track File #11 of 30 - Group 03: Senior / MAANG
> For: backend/data/science/system design interviews | Level: senior | Mode: graph algorithms and GDS judgment

This sheet builds:
- Graph algorithm vocabulary
- When to use Neo4j Graph Data Science style workflows
- Recommendation, risk, and community detection reasoning

---

## 1. Algorithm Families

| Family | Use Cases |
|---|---|
| pathfinding | shortest path, route, dependency path, fraud path |
| centrality | influential users, critical services, important entities |
| community detection | clusters, fraud rings, groups, topic communities |
| similarity | similar users/products/documents/entities |
| link prediction | recommendations and likely relationships |

---

## 2. Transactional Graph vs Analytical Projection

Operational Neo4j queries answer live traversal questions. Graph algorithm workloads often create a projection optimized for analytics.

```text
transactional graph -> graph projection -> algorithm -> scores/communities -> write back or serve downstream
```

Do not run heavy graph analytics on the same hot path as user-facing transactions without capacity isolation.

---

## 3. Recommendation Pattern

```cypher
MATCH (u:User {userId: $userId})-[:BOUGHT]->(:Product)<-[:BOUGHT]-(similar:User)-[:BOUGHT]->(rec:Product)
WHERE NOT (u)-[:BOUGHT]->(rec)
RETURN rec.productId, count(DISTINCT similar) AS score
ORDER BY score DESC
LIMIT 10;
```

This is a simple collaborative filtering pattern. At scale, consider precomputed scores, projections, or recommendation services.

---

## 4. Pathfinding Maturity

Ask:

- Is the graph weighted?
- Are paths directed?
- Is there a maximum depth?
- Are some relationship types excluded?
- Is this online or batch?

Trap:

```text
Shortest path over a large dense graph can be expensive if the anchors and constraints are weak.
```

---

## 5. Strong Answer

```text
I use graph algorithms when relationship structure itself carries signal: centrality for importance, community detection for clusters/fraud rings, pathfinding for dependency or route questions, and similarity for recommendations. For heavy workloads, I separate operational queries from analytical graph projections, validate algorithm quality, and decide whether results are computed online, precomputed, or written back as scored relationships.
```

---

## 6. Revision Notes

- One-line summary: Graph algorithms turn connection structure into scores, paths, and communities.
- Three keywords: pathfinding, centrality, community.
- One interview trap: running expensive graph algorithms on a user request path without isolation.
- Memory trick: traversal answers a question; algorithms score the graph.