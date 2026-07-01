# Neo4j Interview Prep Q&A - MAANG Sheet

> Track File #25 of 30 - Group 05: Special Interview Rounds
> For: backend/data/system design interviews | Level: beginner to MAANG | Mode: direct Q&A and follow-ups

This sheet builds:
- Fast interview answers
- Follow-up hooks
- Beginner, intermediate, senior, and MAANG readiness

---

## Beginner Questions

### 1. What is Neo4j?

Neo4j is a native graph database that stores data as nodes and relationships, optimized for querying connected data and paths.

### 2. What is a node?

A node is an entity such as a user, account, product, device, document, or service.

### 3. What is a relationship?

A relationship is a typed, directed connection between nodes, optionally with properties.

### 4. What is Cypher?

Cypher is Neo4j's declarative graph query language for matching and manipulating graph patterns.

---

## Intermediate Questions

### 5. When do you use Neo4j?

Use it when relationship traversal, path explanation, graph algorithms, or connected-domain modeling is central to the product.

### 6. What are labels and relationship types?

Labels classify nodes. Relationship types define the meaning of connections and guide traversal patterns.

### 7. Why are constraints important?

They protect identity, prevent duplicates, and support reliable `MERGE` operations and indexed anchors.

### 8. How do you debug slow Cypher?

Use EXPLAIN/PROFILE, inspect anchors, indexes, cardinality, db hits, Cartesian products, path bounds, and supernodes.

---

## Senior Questions

### 9. What is a supernode?

A node with extremely high relationship degree. It can create slow traversals and write contention.

### 10. How do you scale Neo4j?

Use bounded traversals, read scaling, caching, precomputation, tenant/domain partitioning, and workload isolation. Challenge arbitrary global deep traversal.

### 11. How does Neo4j fit with GraphRAG?

It can model entities, facts, chunks, sources, and relationships, then combine vector/text retrieval with graph expansion and provenance filtering.

### 12. Replicas vs backups?

Replicas improve availability. Backups support recovery from bad writes, deletes, corrupt imports, or disaster.

---

## MAANG Deep-Dive Questions

### 13. Design fraud detection with Neo4j.

Model accounts, devices, cards, emails, IPs, and risk events. Traverse bounded shared-signal paths, weight evidence, track provenance, stream updates, and monitor false positives.

### 14. Design social recommendations.

Model users and relationships like FOLLOWS, LIKED, BOUGHT, and BLOCKED. Use bounded two-hop traversals for simple recommendations and precompute for high fan-out workloads.

### 15. Design a knowledge graph.

Model entities, facts, documents, chunks, sources, aliases, provenance, and confidence. Protect entity resolution quality and support GraphRAG retrieval.

### 16. Neo4j or PostgreSQL?

PostgreSQL is better for transactional relational truth and reporting. Neo4j is better when connected paths and graph traversals are central. They can be used together.

---

## Interview Closing Formula

```text
For Neo4j I would first name the relationship-heavy domain question. Then I would design nodes, labels, relationship types, directions, properties, constraints, indexes, traversal paths, fan-out limits, sync/freshness, security, operations, and alternatives.
```

---

## Revision Notes

- One-line summary: Neo4j interview strength is graph model plus bounded traversal plus production tradeoffs.
- Three keywords: model, traversal, PROFILE.
- One interview trap: saying graph DB without explaining query anchors.
- Memory trick: answer with the path, not the buzzword.