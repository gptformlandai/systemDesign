# Neo4j Hands-On Exercises and Runnable Mini Labs

> Track File #27 of 30 - Group 06: Practice Upgrade
> For: backend/data/system design interviews | Level: beginner to pro | Mode: local labs, Cypher, modeling, debugging

Use these exercises with the `neo4j-mastery-lab` folder.

---

## Lab 1: First Graph And Traversal

Goal: create nodes, relationships, constraints, and simple traversals.

Tasks:

1. Start Neo4j with Docker Compose.
2. Create uniqueness constraints.
3. Seed users, products, accounts, devices, documents, and entities.
4. Run basic `MATCH` queries.
5. Explain labels and relationship types.

---

## Lab 2: Modeling And Constraints

Goal: understand graph identity and idempotent writes.

Tasks:

- inspect uniqueness constraints
- use `MERGE` with stable IDs
- explain why duplicate entities are dangerous
- add a new relationship property

---

## Lab 3: Traversals And Recommendations

Goal: build friends-of-friends and product recommendation queries.

Tasks:

- run bounded two-hop traversals
- count mutual relationships
- exclude already connected nodes
- explain fan-out risk

---

## Lab 4: Fraud And Identity Graph

Goal: identify shared devices, cards, emails, and IPs.

Tasks:

- find accounts sharing a device
- find accounts connected to flagged accounts within two hops
- explain false positives and signal weighting

---

## Lab 5: Knowledge Graph And GraphRAG

Goal: retrieve document chunks and expand entity context.

Tasks:

- find chunks mentioning an entity
- traverse from chunk to entity to related entity
- return source/provenance metadata
- explain permission and citation checks

---

## Lab 6: Query Plan Debugging

Goal: inspect `EXPLAIN` and `PROFILE` output.

Tasks:

- compare indexed anchor query vs broad scan
- inspect row counts and db hits
- detect Cartesian product shape
- rewrite one query to reduce fan-out

---

## Lab 7: Operations Incident Drill

Scenario:

```text
Fraud investigation traversal p99 jumps from 250 ms to 4 seconds after adding a new signal source.
```

Answer:

- inspect exact Cypher and parameters
- check EXPLAIN/PROFILE
- check new signal cardinality
- check supernode risk
- check missing constraint/index
- cap traversal or isolate noisy signal

---

## Lab 8: Permission Graph And Access Control

Goal: answer whether a user can access a resource and explain why.

Tasks:

- run [../neo4j-mastery-lab/SCRIPTS/08-permission-graph.cypher](../neo4j-mastery-lab/SCRIPTS/08-permission-graph.cypher)
- trace inherited group access
- explain deny precedence
- explain tenant filtering and permission-cache risk

---

## Lab 9: Dependency, Lineage, And Blast Radius

Goal: answer what services, datasets, or dashboards are impacted by a dependency change.

Tasks:

- run [../neo4j-mastery-lab/SCRIPTS/09-dependency-lineage.cypher](../neo4j-mastery-lab/SCRIPTS/09-dependency-lineage.cypher)
- trace downstream service impact
- trace dataset-to-dashboard lineage impact
- explain dependency direction, ownership, and freshness

---

## Completion Gate

You finish these labs only when you can explain:

- why each label exists
- why each relationship type exists
- what each traversal starts from
- what can go wrong at scale
- what metric or runbook catches that failure