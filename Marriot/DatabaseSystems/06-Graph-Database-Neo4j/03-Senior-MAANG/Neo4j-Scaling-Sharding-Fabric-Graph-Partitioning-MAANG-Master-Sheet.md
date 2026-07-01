# Neo4j Scaling, Sharding, Fabric, and Graph Partitioning - MAANG Master Sheet

> Track File #14 of 30 - Group 03: Senior / MAANG
> For: backend/data/system design interviews | Level: senior / MAANG | Mode: scale boundaries and partitioning judgment

This sheet builds:
- Honest Neo4j scale language
- Graph partitioning and cross-graph tradeoffs
- When to use Neo4j, Fabric, projections, or another system

---

## 1. Scaling First Principle

Graph workloads scale differently from key-value workloads because traversals may cross many connected entities.

Easy to scale:

- many independent tenant graphs
- bounded local traversals
- read replicas for read-heavy query paths
- precomputed algorithm scores

Harder to scale:

- one dense global graph with deep arbitrary traversals
- supernodes with huge degree
- cross-partition path queries
- online graph algorithms over massive graphs

---

## 2. Partitioning Questions

Ask:

- Is there a natural tenant/community boundary?
- Do queries mostly stay within one partition?
- What percentage of traversals cross partitions?
- Can cross-partition answers be approximate or async?
- Can we precompute edges/scores?

---

## 3. Fabric / Multi-Graph Thinking

Fabric-style patterns and multi-database designs can route queries across graphs, but they do not magically make arbitrary global graph traversal cheap.

Maturity phrase:

```text
Graph partitioning is only effective when the product queries respect the boundary most of the time.
```

---

## 4. Scale Pattern Toolbox

| Problem | Option |
|---|---|
| read-heavy graph | read replicas, caching, precomputed views |
| large tenant | isolate tenant graph/database |
| supernode | split relationship type, add intermediate nodes, limit traversal |
| expensive recommendations | precompute scores or use GDS batch pipeline |
| global analytics | graph data science pipeline or separate analytics platform |
| simple lookup | keep in SQL/KV/document store |

---

## 5. Strong Answer

```text
I would scale Neo4j by first bounding the graph problem: choose indexed anchors, keep traversals local, avoid supernodes, and isolate read-heavy or analytical workloads. If tenants or domains are naturally separable, I can partition by tenant/database or use multi-graph routing. But if the product requires arbitrary deep traversals across a dense global graph, I would challenge Neo4j as the sole serving store and consider precomputation, graph analytics pipelines, or a different architecture.
```

---

## 6. Revision Notes

- One-line summary: Neo4j scales best when traversal boundaries are clear.
- Three keywords: partition, supernode, precompute.
- One interview trap: saying "shard it" without explaining cross-partition paths.
- Memory trick: graph scale follows graph boundaries.