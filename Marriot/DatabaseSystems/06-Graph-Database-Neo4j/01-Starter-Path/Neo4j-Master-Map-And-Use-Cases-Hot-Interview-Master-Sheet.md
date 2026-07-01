# Neo4j Master Map and Use Cases - Hot Interview Master Sheet

> Track File #1 of 30 - Group 01: Starter Path
> For: backend/data/system design interviews | Level: beginner to pro | Mode: graph database map, use cases, selection judgment

This sheet builds:
- A beginner-friendly mental model for Neo4j
- Where graph databases beat table/document/key-value models
- Interview language for choosing or rejecting Neo4j

---

## 1. Core Mental Model

Neo4j stores data as connected entities.

```text
(node)-[:RELATIONSHIP]->(node)
```

The main question is not "what rows do I filter?" It is:

```text
Starting from this entity, what connected entities and paths matter?
```

---

## 2. Where Neo4j Fits

| Use Case | Why Graph Helps |
|---|---|
| recommendations | friends-of-friends, similar users, shared behavior |
| fraud detection | shared devices, cards, addresses, accounts, identities |
| access control | users, groups, roles, resources, inherited permissions |
| knowledge graphs | concepts, entities, facts, provenance, relationships |
| network/IT topology | services, hosts, dependencies, blast radius |
| supply chain | parts, vendors, products, routes, risk propagation |
| GraphRAG | entity-aware retrieval and explainable context paths |

---

## 3. When Neo4j Is A Strong Fit

Choose Neo4j when:

- relationship traversal is the product feature
- joins are deep, dynamic, or hard to predefine
- path questions matter
- graph explainability matters
- relationships have meaningful properties
- recursive dependency analysis is common

Trigger words:

```text
connected, path, network, dependency, fraud ring, permissions, lineage, recommendation, identity, relationship depth
```

---

## 4. When Not To Use Neo4j

Avoid Neo4j as the default choice when:

- the workload is simple CRUD by primary key
- most queries are analytical table scans
- the domain has few relationships
- graph traversals are unbounded and cannot be constrained
- the team needs a transactional source of truth already served well by SQL
- the graph is only a secondary view that can be computed elsewhere more cheaply

Strong maturity phrase:

```text
Neo4j is powerful when relationships are first-class. It is overkill when the relationship model is incidental.
```

---

## 5. Neo4j vs SQL In One Minute

SQL joins tables at query time. Neo4j stores relationships directly and traverses them as first-class connections.

```text
SQL: Person table + Friendship table + joins
Neo4j: (:Person)-[:FRIEND_OF]->(:Person)
```

SQL can model graphs, but repeated multi-hop joins become harder to read, optimize, and evolve.

---

## 6. Interview Answer Template

```text
I would consider Neo4j if the core problem is relationship traversal, such as fraud rings, recommendations, permissions, dependencies, or knowledge graph retrieval. I would model entities as nodes and meaningful relationships as typed edges, start queries from indexed anchors, bound traversal depth, and use constraints/indexes plus query-plan checks. If the workload is mostly simple CRUD, reporting, or transactional aggregates, I would prefer SQL or another store and maybe keep Neo4j as a derived graph projection.
```

---

## 7. Revision Notes

- One-line summary: Neo4j is strongest when connected paths are the product logic.
- Three keywords: nodes, relationships, traversal.
- One interview trap: choosing graph DB just because data has foreign keys.
- Memory trick: if the question says "how are these connected?", think graph.