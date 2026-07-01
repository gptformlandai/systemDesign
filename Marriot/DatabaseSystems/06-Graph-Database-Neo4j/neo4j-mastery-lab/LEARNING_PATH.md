# Neo4j Mastery Lab Learning Path

This path turns Neo4j from a graph query tool into a graph system you can design, operate, debug, and defend in interviews.

---

## Stage 1: Starter Foundations

Read:

- `../01-Starter-Path/Neo4j-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md`
- `../01-Starter-Path/Neo4j-Graph-Model-Nodes-Relationships-Properties-Gold-Sheet.md`
- `../01-Starter-Path/Neo4j-Installation-Browser-Cypher-Basics-Gold-Sheet.md`
- `../01-Starter-Path/Neo4j-CRUD-Traversal-Pattern-Matching-Gold-Sheet.md`

Run:

```bash
docker compose up -d
bash SCRIPTS/wait-for-neo4j.sh
bash SCRIPTS/run-cypher.sh SCRIPTS/01-schema.cypher
bash SCRIPTS/run-cypher.sh SCRIPTS/02-seed-data.cypher
```

Lab:

- [LABS/01-graph-basics.md](LABS/01-graph-basics.md)
- [CHEATSHEETS/CYPHER.md](CHEATSHEETS/CYPHER.md)

---

## Stage 2: Modeling, Constraints, And Cypher

Read:

- `../02-Intermediate-Backend/Neo4j-Cypher-Querying-MATCH-WHERE-RETURN-MAANG-Master-Sheet.md`
- `../02-Intermediate-Backend/Neo4j-Graph-Data-Modeling-Labels-Relationships-Cardinality-Gold-Sheet.md`
- `../02-Intermediate-Backend/Neo4j-Constraints-Indexes-Fulltext-Vector-Gold-Sheet.md`

Lab:

- [LABS/02-modeling-constraints.md](LABS/02-modeling-constraints.md)
- [LABS/03-traversals-recommendations.md](LABS/03-traversals-recommendations.md)
- [CHEATSHEETS/MODELING.md](CHEATSHEETS/MODELING.md)

---

## Stage 3: App Integration, Import, And Performance

Read:

- `../02-Intermediate-Backend/Neo4j-Transactions-Drivers-Java-Python-Node-Spring-Gold-Sheet.md`
- `../02-Intermediate-Backend/Neo4j-Import-ETL-CDC-Kafka-APOC-Gold-Sheet.md`
- `../02-Intermediate-Backend/Neo4j-Performance-Query-Plans-EXPLAIN-PROFILE-Gold-Sheet.md`

Lab:

- [LABS/06-query-plan-debugging.md](LABS/06-query-plan-debugging.md)
- [INTERVIEW_PREP/ANSWER_PATTERNS.md](INTERVIEW_PREP/ANSWER_PATTERNS.md)

---

## Stage 4: Senior Production Neo4j

Read:

- `../03-Senior-MAANG/Neo4j-Graph-Algorithms-GDS-Pathfinding-Communities-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Neo4j-Clustering-Replication-High-Availability-Operations-Gold-Sheet.md`
- `../03-Senior-MAANG/Neo4j-Security-Multi-Tenancy-Backup-Disaster-Recovery-Gold-Sheet.md`
- `../03-Senior-MAANG/Neo4j-Scaling-Sharding-Fabric-Graph-Partitioning-MAANG-Master-Sheet.md`

Lab:

- [LABS/07-operations-incident-drills.md](LABS/07-operations-incident-drills.md)
- [RUNBOOKS/SLOW_TRAVERSAL.md](RUNBOOKS/SLOW_TRAVERSAL.md)
- [RUNBOOKS/CARTESIAN_PRODUCT.md](RUNBOOKS/CARTESIAN_PRODUCT.md)
- [RUNBOOKS/HOT_NODE.md](RUNBOOKS/HOT_NODE.md)
- [RUNBOOKS/LOCK_CONTENTION.md](RUNBOOKS/LOCK_CONTENTION.md)
- [RUNBOOKS/STALE_GRAPH_PROJECTION.md](RUNBOOKS/STALE_GRAPH_PROJECTION.md)
- [CHEATSHEETS/OPERATIONS.md](CHEATSHEETS/OPERATIONS.md)

---

## Stage 5: Advanced And Scenario Design

Read:

- `../03-Senior-MAANG/Neo4j-Knowledge-Graph-GraphRAG-Vector-Hybrid-Search-MAANG-Master-Sheet.md`
- `../03-Senior-MAANG/Neo4j-Cloud-Kubernetes-Testing-Observability-Gold-Sheet.md`
- `../04-Scenario-Practice/Neo4j-Social-Graph-Recommendation-Engine-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/Neo4j-Fraud-Risk-Identity-Graph-Gold-Sheet.md`
- `../04-Scenario-Practice/Neo4j-Knowledge-Graph-Entity-Resolution-MAANG-Master-Sheet.md`
- `../04-Scenario-Practice/Neo4j-Permission-Graph-Access-Control-Gap-Fill-MAANG-Sheet.md`
- `../04-Scenario-Practice/Neo4j-Supply-Chain-Lineage-Service-Dependency-Gap-Fill-MAANG-Sheet.md`
- `../04-Scenario-Practice/Neo4j-System-Design-Case-Studies-MAANG-Master-Sheet.md`

Lab:

- [LABS/08-permission-graph-access-control.md](LABS/08-permission-graph-access-control.md)
- [LABS/09-dependency-lineage-blast-radius.md](LABS/09-dependency-lineage-blast-radius.md)

Projects:

- [PROJECTS/01-social-recommendation-engine.md](PROJECTS/01-social-recommendation-engine.md)
- [PROJECTS/02-fraud-ring-detection.md](PROJECTS/02-fraud-ring-detection.md)
- [PROJECTS/03-knowledge-graph-graphrag.md](PROJECTS/03-knowledge-graph-graphrag.md)
- [PROJECTS/04-service-dependency-graph.md](PROJECTS/04-service-dependency-graph.md)

---

## Stage 6: Interview Readiness

Read:

- `../05-Special-Interview-Rounds/Neo4j-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md`
- `../05-Special-Interview-Rounds/Neo4j-Interview-Prep-QA-MAANG-Sheet.md`
- `../06-Practice-Upgrade/Neo4j-Active-Recall-Question-Bank.md`
- `../06-Practice-Upgrade/Neo4j-Hands-On-Exercises-And-Runnable-Mini-Labs.md`
- `../06-Practice-Upgrade/Neo4j-Mini-Projects-Portfolio.md`
- `../06-Practice-Upgrade/Neo4j-Cheat-Sheets-Roadmap-Golden-Rules.md`
- `../06-Practice-Upgrade/Neo4j-Pro-Gap-Fill-Capacity-Traversal-SLO-Design-Review.md`

MAANG deep-dive gate:

- Defend labels, relationship types, direction, properties, constraints, and indexes.
- Explain query anchors, traversal depth, fan-out, supernodes, and PROFILE output.
- Debug slow traversal, Cartesian products, hot nodes, stale graph projections, and bad entity resolution.
- Compare Neo4j with SQL, MongoDB, Cassandra, Elasticsearch, vector databases, and RDF/triplestores.
- Design at least 4 portfolio projects end to end.