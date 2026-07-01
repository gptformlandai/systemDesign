# Neo4j Cheat Sheets, Roadmap, and Golden Rules - Gold Sheet

> Track File #29 of 30 - Group 06: Practice Upgrade
> For: backend/data/system design interviews | Level: revision and final consolidation | Mode: fast recall, commands, roadmap, rules

This sheet builds:
- Neo4j command and design cheat sheets
- Beginner-to-pro roadmap
- Golden rules and final readiness checklist

---

## 1. Cypher Cheat Sheet

```cypher
MATCH (n) RETURN count(n);
CREATE (:User {userId: 'u1'});
MERGE (u:User {userId: 'u1'});
MATCH (u:User {userId: 'u1'})-[:BOUGHT]->(p:Product) RETURN p;
MATCH path = (u:User)-[:FRIEND_OF*1..2]->(v:User) RETURN path;
EXPLAIN MATCH (u:User {userId: 'u1'}) RETURN u;
PROFILE MATCH (u:User {userId: 'u1'}) RETURN u;
```

---

## 2. Modeling Cheat Sheet

| Need | Model Choice |
|---|---|
| entity | node |
| entity category | label |
| meaningful connection | relationship type |
| connection fact | relationship property |
| identity protection | uniqueness constraint |
| lookup start | index/constraint-backed property |
| text entry point | full-text index |
| semantic entry point | vector index |
| expensive score | precomputed relationship/property |

---

## 3. Golden Rules

1. Start from domain questions, not table structure.
2. Use specific relationship types.
3. Put connection facts on relationships.
4. Add constraints for identity.
5. Index query anchors.
6. Bound variable-length paths.
7. Inspect EXPLAIN/PROFILE.
8. Watch fan-out and supernodes.
9. Use parameters, not string-built Cypher.
10. Keep heavy algorithms away from hot request paths.
11. Define freshness SLOs for derived graphs.
12. Protect tenant and relationship access.
13. Test restore, not only backup creation.
14. Know when SQL/search/vector/analytics systems are better.

---

## 4. Beginner To Pro Roadmap

### Stage 1: Beginner

Topics:

- nodes, relationships, labels, properties
- Browser and cypher-shell
- basic Cypher
- simple traversals

Project: tiny social graph.

### Stage 2: Intermediate

Topics:

- modeling from queries
- constraints and indexes
- drivers and transactions
- imports and sync
- EXPLAIN/PROFILE

Project: fraud or recommendation graph.

### Stage 3: Senior

Topics:

- graph algorithms
- clustering and backups
- security and multi-tenancy
- scale and partitioning
- observability

Project: production fraud or permissions graph.

### Stage 4: MAANG / Pro

Topics:

- GraphRAG
- entity resolution
- capacity and traversal budgets
- incident runbooks
- design reviews

Project: knowledge graph retrieval platform.

---

## 5. Final MAANG Checklist

- I can explain when Neo4j is right and wrong.
- I can design labels, relationships, directions, properties, constraints, and indexes.
- I can write Cypher for CRUD, traversals, recommendations, fraud, and GraphRAG.
- I can explain EXPLAIN/PROFILE, db hits, row multiplication, and Cartesian products.
- I can design ingestion, CDC, idempotent writes, and freshness SLOs.
- I can discuss graph algorithms and GDS-style projections.
- I can explain clustering, backups, security, and multi-tenancy.
- I can debug slow traversals, hot nodes, lock contention, and stale graph projections.
- I can compare Neo4j with SQL, MongoDB, Cassandra, Elasticsearch, vector databases, and RDF/triplestores.

---

## 6. Final Summary

```text
Neo4j is a strong choice when connected paths, relationship semantics, graph algorithms, or knowledge graph retrieval are the core product problem. It is a poor default for simple CRUD, arbitrary warehouse analytics, or workloads whose access patterns are better served by SQL, search, vector, or key-value systems.
```