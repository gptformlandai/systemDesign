# Neo4j Anti-Patterns, Internals, and Debugging - MAANG Sheet

> Track File #24 of 30 - Group 05: Special Interview Rounds
> For: backend/data/system design interviews | Level: senior / MAANG | Mode: traps, fixes, production debugging

This sheet builds:
- Neo4j anti-pattern recognition
- Production debugging playbooks
- Follow-up confidence for graph interviews

---

## 1. Top Anti-Patterns

| Anti-Pattern | Why It Fails | Better Approach |
|---|---|---|
| table-to-node migration | misses traversal requirements | model from domain questions |
| generic `RELATED_TO` | vague traversal semantics | specific relationship types |
| unbounded variable paths | explosive fan-out | depth limits and constraints |
| missing constraints | duplicate identities | uniqueness constraints |
| no anchor index | label scans | indexed lookup anchors |
| Cartesian products | row explosion | connect patterns or split queries |
| supernodes ignored | hot traversal/write points | split model or cap traversal |
| raw user Cypher API | security and cost risk | controlled query builders |
| graph as warehouse | expensive analytical scans | analytics platform or projections |

---

## 2. Debug: Cartesian Product

Symptoms:

- sudden row explosion
- query plan shows Cartesian product
- latency and memory spike

Fixes:

- connect patterns through shared variables
- split query into subqueries
- add missing filters
- use indexed anchors

---

## 3. Debug: Hot Node

Symptoms:

- one node has extremely high degree
- traversals from/to that node are slow
- writes contend around the same node

Fixes:

- split relationship types
- add intermediate bucket nodes
- cap traversal depth
- precompute summary relationships

---

## 4. Debug: Slow Traversal

Checklist:

- exact Cypher and parameters
- EXPLAIN/PROFILE
- starting anchor and index
- relationship types and direction
- variable path bounds
- rows and db hits
- returned payload size
- supernode or dense subgraph

---

## 5. Strong Debugging Answer

```text
I start with the exact Cypher and parameters, inspect EXPLAIN/PROFILE, verify the query starts from an indexed anchor, and look for label scans, Cartesian products, row explosion, unbounded paths, high db hits, and supernodes. If the query is structurally awkward, the durable fix may be a graph model change, not just an index.
```

---

## 6. Revision Notes

- One-line summary: Neo4j failures are usually graph model, anchor, fan-out, or query-shape failures.
- Three keywords: Cartesian product, supernode, PROFILE.
- One interview trap: treating graph performance as only an indexing problem.
- Memory trick: bad graph shape beats good hardware.