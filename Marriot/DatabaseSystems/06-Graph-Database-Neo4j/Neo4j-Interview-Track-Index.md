# Neo4j Graph Database Interview Track Index

This folder is the Neo4j graph database track for backend engineers, data engineers, knowledge graph builders, GenAI/RAG engineers, production owners, and MAANG-style system design interviews.

Audience:
- You are a software engineer who wants beginner-to-pro graph database depth.
- You want practical Neo4j readiness, not only Cypher syntax.
- You want MAANG-level mastery of graph modeling, traversals, Cypher, constraints, indexes, performance, clustering, security, graph algorithms, knowledge graphs, GraphRAG, and production debugging.

Goal:
- Build Neo4j from first principles to production ownership.
- Keep each topic modular so revision is fast.
- Make the answer pattern repeatable: domain question, graph model, nodes/relationships/properties, Cypher traversal, constraints/indexes, performance, failure mode, tradeoff, strong interview answer.
- Connect Neo4j decisions to real systems: recommendations, fraud rings, identity graphs, permissions, supply chain dependencies, network topology, knowledge graphs, entity resolution, and GraphRAG.

Use this index as the reading order.

---

## How To Read These Notes As A Backend Engineer

Before diving in, accept these five reframes:

### 1. Neo4j is a relationship-first database, not a generic replacement for SQL

Neo4j is excellent when relationship traversal is the core workload. It is not automatically better than relational/document stores for simple CRUD, large table scans, or transaction-heavy aggregate reporting.

### 2. Graph modeling is query design

Labels, relationship types, direction, properties, constraints, and indexes decide whether a traversal is elegant or painful.

### 3. Traversal cost grows from starting points and fan-out

Graph queries are fast when they start from selective anchors and traverse bounded patterns. Unbounded variable-length paths are a common production trap.

### 4. Cypher is declarative, but the graph shape still matters

Cypher makes patterns readable. It does not rescue a model with hot nodes, missing constraints, Cartesian products, or vague relationship types.

### 5. Graph correctness includes meaning

For graph systems, correctness is not only storing records. It includes relationship semantics, path meaning, identity resolution, freshness, authorization, explainability, and safe traversal limits.

---

## Relational Developer Bridge Pattern

Every important Neo4j topic should be translated through this pattern:

```text
Relational Developer Bridge

Similar to SQL:
  What concept maps cleanly.

Different in Neo4j:
  What works differently and why.

Does not exist or is weaker:
  SQL feature that Neo4j does not provide in the same way.

Neo4j replacement:
  labels, relationship types, indexed anchors, pattern matching, traversals, constraints, projections, or app-side source-of-truth sync.

Interview trap:
  The table-shaped assumption that leads to a bad graph model.
```

---

## Learning Style: Beginner To MAANG Loop

Do not learn Neo4j as isolated Cypher snippets. Learn every topic through this repeatable loop:

```text
domain question -> graph model -> nodes/relationships/properties -> constraints/indexes -> Cypher traversal -> performance/failure mode -> interview answer
```

Use this style at each level:

| Level | How To Learn | Output You Must Produce |
|---|---|---|
| Beginner | Read the concept sheet, draw the graph, run basic Cypher, explain SQL-to-graph bridge | A correct node/relationship model, basic query, and 2-minute explanation |
| Intermediate | Start from access patterns, design labels/relationship types/properties, write Cypher traversals | A graph model, sample data, constraints, indexes, traversal queries, and performance notes |
| Senior | Add transactions, drivers, import/sync, query plans, clustering, security, backup, observability, and incident cases | A production-ready graph design with SLOs, failure/recovery, and quality metrics visible |
| MAANG / Pro | Answer as a system owner: requirements, graph boundary, traversal strategy, scale path, alternatives, and incident response | A whiteboard-ready architecture answer plus debugging and follow-up responses |

Daily study rhythm:

1. Read one concept sheet for 30-45 minutes.
2. Run one modeling, Cypher, constraint, traversal, or lab script.
3. Explain one tradeoff out loud: node property vs relationship property, direction, label granularity, relationship type, variable-length paths, hot nodes, graph DB vs SQL.
4. Answer five active-recall questions without notes.
5. Finish with one graph system design or production-debugging prompt.

MAANG answer rule:

```text
Never stop at "Neo4j stores relationships".
Say what the domain question is, how the graph is modeled, where the traversal starts, how fan-out is bounded, what constraints/indexes exist, what fails, how you observe it, and what alternative you rejected.
```

---

## Track Structure

| Group | Purpose |
|---|---|
| 1. Starter Path | Fundamentals, graph mental model, setup, Cypher basics, CRUD, simple traversals |
| 2. Intermediate Backend Path | Cypher, graph modeling, constraints/indexes, drivers, transactions, import/sync, query plans |
| 3. Senior / MAANG Path | Graph algorithms, GDS, clustering, security, backups, scale patterns, GraphRAG, cloud/testing |
| 4. Scenario Practice Path | Recommendations, fraud, identity, knowledge graphs, permissions, supply chain, tradeoffs |
| 5. Special Interview Rounds | Anti-patterns, internals, debugging, direct interview Q&A |
| 6. Practice Upgrade Path | Active recall, hands-on labs, mini projects, cheat sheets, pro design review |
| 7. Runnable Lab | Docker setup, Cypher scripts, sample graph, guided labs, projects, runbooks, and interview prep |

---

## 1. Starter Path

Read these first. They build Neo4j intuition from zero to useful backend fluency.

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Starter-Path/Neo4j-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md](01-Starter-Path/Neo4j-Master-Map-And-Use-Cases-Hot-Interview-Master-Sheet.md) | Neo4j roadmap, where graph databases fit, graph vs relational thinking, common use cases |
| 2 | [01-Starter-Path/Neo4j-Graph-Model-Nodes-Relationships-Properties-Gold-Sheet.md](01-Starter-Path/Neo4j-Graph-Model-Nodes-Relationships-Properties-Gold-Sheet.md) | nodes, labels, relationships, direction, properties, paths, graph patterns |
| 3 | [01-Starter-Path/Neo4j-Installation-Browser-Cypher-Basics-Gold-Sheet.md](01-Starter-Path/Neo4j-Installation-Browser-Cypher-Basics-Gold-Sheet.md) | Docker/local setup, Browser, cypher-shell, basic Cypher workflow |
| 4 | [01-Starter-Path/Neo4j-CRUD-Traversal-Pattern-Matching-Gold-Sheet.md](01-Starter-Path/Neo4j-CRUD-Traversal-Pattern-Matching-Gold-Sheet.md) | create, merge, match, set, delete, paths, simple traversals |

Starter target:
- You can explain what Neo4j is and when to choose it.
- You can model nodes and relationships for a simple domain.
- You can run basic Cypher and explain relationship traversal.

---

## 2. Intermediate Backend Path

After the starter path, read these to learn how Neo4j becomes a real backend graph system.

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Backend/Neo4j-Cypher-Querying-MATCH-WHERE-RETURN-MAANG-Master-Sheet.md](02-Intermediate-Backend/Neo4j-Cypher-Querying-MATCH-WHERE-RETURN-MAANG-Master-Sheet.md) | MATCH, WHERE, RETURN, aggregation, OPTIONAL MATCH, UNWIND, subqueries, path patterns |
| 6 | [02-Intermediate-Backend/Neo4j-Graph-Data-Modeling-Labels-Relationships-Cardinality-Gold-Sheet.md](02-Intermediate-Backend/Neo4j-Graph-Data-Modeling-Labels-Relationships-Cardinality-Gold-Sheet.md) | labels, relationship types, direction, cardinality, supernodes, access patterns |
| 7 | [02-Intermediate-Backend/Neo4j-Constraints-Indexes-Fulltext-Vector-Gold-Sheet.md](02-Intermediate-Backend/Neo4j-Constraints-Indexes-Fulltext-Vector-Gold-Sheet.md) | uniqueness, existence, indexes, full-text, vector indexes, lookup strategy |
| 8 | [02-Intermediate-Backend/Neo4j-Transactions-Drivers-Java-Python-Node-Spring-Gold-Sheet.md](02-Intermediate-Backend/Neo4j-Transactions-Drivers-Java-Python-Node-Spring-Gold-Sheet.md) | drivers, sessions, transactions, retries, bookmarks, Spring Data Neo4j, API boundaries |
| 9 | [02-Intermediate-Backend/Neo4j-Import-ETL-CDC-Kafka-APOC-Gold-Sheet.md](02-Intermediate-Backend/Neo4j-Import-ETL-CDC-Kafka-APOC-Gold-Sheet.md) | CSV import, ETL, batch writes, CDC, Kafka, APOC patterns, source-of-truth sync |
| 10 | [02-Intermediate-Backend/Neo4j-Performance-Query-Plans-EXPLAIN-PROFILE-Gold-Sheet.md](02-Intermediate-Backend/Neo4j-Performance-Query-Plans-EXPLAIN-PROFILE-Gold-Sheet.md) | EXPLAIN, PROFILE, cardinality, db hits, indexes, Cartesian products, traversal tuning |

Intermediate target:
- You can design graph models from traversal requirements.
- You can write Cypher for real backend APIs.
- You can explain constraints, indexes, transactions, import, and query-plan tradeoffs.

---

## 3. Senior / MAANG Path

These are the production and graph-specialist sheets.

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-MAANG/Neo4j-Graph-Algorithms-GDS-Pathfinding-Communities-MAANG-Master-Sheet.md](03-Senior-MAANG/Neo4j-Graph-Algorithms-GDS-Pathfinding-Communities-MAANG-Master-Sheet.md) | shortest path, centrality, community detection, similarity, recommendations, GDS mental model |
| 12 | [03-Senior-MAANG/Neo4j-Clustering-Replication-High-Availability-Operations-Gold-Sheet.md](03-Senior-MAANG/Neo4j-Clustering-Replication-High-Availability-Operations-Gold-Sheet.md) | cluster roles, routing, read replicas, HA, failover, operational posture |
| 13 | [03-Senior-MAANG/Neo4j-Security-Multi-Tenancy-Backup-Disaster-Recovery-Gold-Sheet.md](03-Senior-MAANG/Neo4j-Security-Multi-Tenancy-Backup-Disaster-Recovery-Gold-Sheet.md) | auth, RBAC, tenant isolation, backups, restore, RPO/RTO, sensitive relationship data |
| 14 | [03-Senior-MAANG/Neo4j-Scaling-Sharding-Fabric-Graph-Partitioning-MAANG-Master-Sheet.md](03-Senior-MAANG/Neo4j-Scaling-Sharding-Fabric-Graph-Partitioning-MAANG-Master-Sheet.md) | scaling graph workloads, partitioning, Fabric, supernodes, graph boundaries, alternatives |
| 15 | [03-Senior-MAANG/Neo4j-Knowledge-Graph-GraphRAG-Vector-Hybrid-Search-MAANG-Master-Sheet.md](03-Senior-MAANG/Neo4j-Knowledge-Graph-GraphRAG-Vector-Hybrid-Search-MAANG-Master-Sheet.md) | knowledge graphs, entity resolution, GraphRAG, vector + graph retrieval, lineage, grounded answers |
| 16 | [03-Senior-MAANG/Neo4j-Cloud-Kubernetes-Testing-Observability-Gold-Sheet.md](03-Senior-MAANG/Neo4j-Cloud-Kubernetes-Testing-Observability-Gold-Sheet.md) | Aura/cloud, Kubernetes, Testcontainers, monitoring, upgrades, production testing |

Senior target:
- You can reason about graph algorithms, traversal complexity, HA, backup, and operations.
- You can decide when Neo4j scales well and when to partition or choose another system.
- You can design GraphRAG and knowledge graph systems with quality, lineage, and safety metrics.

---

## 4. Scenario Practice Path

Use these after the concept sheets to train interview and architecture answers.

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/Neo4j-Social-Graph-Recommendation-Engine-MAANG-Master-Sheet.md](04-Scenario-Practice/Neo4j-Social-Graph-Recommendation-Engine-MAANG-Master-Sheet.md) | social graph, friends-of-friends, recommendations, mutuals, feed signals |
| 18 | [04-Scenario-Practice/Neo4j-Fraud-Risk-Identity-Graph-Gold-Sheet.md](04-Scenario-Practice/Neo4j-Fraud-Risk-Identity-Graph-Gold-Sheet.md) | fraud rings, shared devices/cards/emails, risk scoring, investigation traversals |
| 19 | [04-Scenario-Practice/Neo4j-Knowledge-Graph-Entity-Resolution-MAANG-Master-Sheet.md](04-Scenario-Practice/Neo4j-Knowledge-Graph-Entity-Resolution-MAANG-Master-Sheet.md) | entities, ontologies, aliases, provenance, disambiguation, knowledge graph quality |
| 20 | [04-Scenario-Practice/Neo4j-Permission-Graph-Access-Control-Gap-Fill-MAANG-Sheet.md](04-Scenario-Practice/Neo4j-Permission-Graph-Access-Control-Gap-Fill-MAANG-Sheet.md) | permission graphs, RBAC, inherited access, tenant filters, deny precedence, explainable access paths |
| 21 | [04-Scenario-Practice/Neo4j-Supply-Chain-Lineage-Service-Dependency-Gap-Fill-MAANG-Sheet.md](04-Scenario-Practice/Neo4j-Supply-Chain-Lineage-Service-Dependency-Gap-Fill-MAANG-Sheet.md) | supply chain, service dependencies, data lineage, blast radius, ownership, freshness |
| 22 | [04-Scenario-Practice/Neo4j-System-Design-Case-Studies-MAANG-Master-Sheet.md](04-Scenario-Practice/Neo4j-System-Design-Case-Studies-MAANG-Master-Sheet.md) | 12 case studies: recommendations, fraud, permissions, supply chain, network topology, GraphRAG |
| 23 | [04-Scenario-Practice/Neo4j-vs-SQL-MongoDB-Cassandra-Elasticsearch-Vector-Tradeoff-Gold-Sheet.md](04-Scenario-Practice/Neo4j-vs-SQL-MongoDB-Cassandra-Elasticsearch-Vector-Tradeoff-Gold-Sheet.md) | Neo4j vs SQL, MongoDB, Cassandra, Elasticsearch, vector DBs, RDF/triplestores |

Scenario target:
- You can answer system design prompts with graph model, traversal, constraints, indexes, scaling, freshness, failure modes, and alternatives.
- You can compare Neo4j with other databases/search systems without shallow slogans.

---

## 5. Special Interview Rounds

Use these for debugging, internals, anti-patterns, and direct interview prep.

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/Neo4j-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md](05-Special-Interview-Rounds/Neo4j-Anti-Patterns-Internals-Debugging-MAANG-Sheet.md) | anti-patterns and fixes: supernodes, unbounded paths, Cartesian products, missing indexes, bad relationship semantics |
| 25 | [05-Special-Interview-Rounds/Neo4j-Interview-Prep-QA-MAANG-Sheet.md](05-Special-Interview-Rounds/Neo4j-Interview-Prep-QA-MAANG-Sheet.md) | beginner, intermediate, senior, and MAANG Q&A with crisp answers and follow-ups |

Special-round target:
- You can identify bad graph designs and production failure patterns.
- You can answer Neo4j interview questions from beginner to MAANG level.

---

## 6. Practice Upgrade Path

Use these to convert reading into active recall, labs, projects, and revision.

| Order | File | What It Builds |
|---:|---|---|
| 26 | [06-Practice-Upgrade/Neo4j-Active-Recall-Question-Bank.md](06-Practice-Upgrade/Neo4j-Active-Recall-Question-Bank.md) | foundation, intermediate, senior, and MAANG recall prompts by topic |
| 27 | [06-Practice-Upgrade/Neo4j-Hands-On-Exercises-And-Runnable-Mini-Labs.md](06-Practice-Upgrade/Neo4j-Hands-On-Exercises-And-Runnable-Mini-Labs.md) | beginner-to-pro labs for modeling, Cypher, traversals, constraints, recommendations, fraud, permissions, lineage, GraphRAG, operations |
| 28 | [06-Practice-Upgrade/Neo4j-Mini-Projects-Portfolio.md](06-Practice-Upgrade/Neo4j-Mini-Projects-Portfolio.md) | practical Neo4j projects with graph models, queries, scaling concerns, and interview discussion points |
| 29 | [06-Practice-Upgrade/Neo4j-Cheat-Sheets-Roadmap-Golden-Rules.md](06-Practice-Upgrade/Neo4j-Cheat-Sheets-Roadmap-Golden-Rules.md) | cheat sheets, beginner-to-pro roadmap, golden rules, mistakes, and final readiness checklist |
| 30 | [06-Practice-Upgrade/Neo4j-Pro-Gap-Fill-Capacity-Traversal-SLO-Design-Review.md](06-Practice-Upgrade/Neo4j-Pro-Gap-Fill-Capacity-Traversal-SLO-Design-Review.md) | pro gaps: traversal budget, capacity worksheet, hot-node review, SLOs, entity quality, design review |

Practice target:
- You can answer from memory, run labs, build mini projects, and revise with cheat sheets.
- You can measure readiness instead of passively rereading notes.
- You can run a staff-level graph design review that covers traversal correctness, fan-out, performance, freshness, security, and operations.

---

## 7. Runnable Lab

Use the consolidated lab when you want runnable practice instead of reading-only notes:

- [neo4j-mastery-lab/README.md](neo4j-mastery-lab/README.md)
- [neo4j-mastery-lab/LEARNING_PATH.md](neo4j-mastery-lab/LEARNING_PATH.md)

Lab target:
- You can run Neo4j locally with Docker.
- You can practice graph modeling, constraints, Cypher, traversals, recommendations, fraud detection, knowledge graph retrieval, query plans, and incident drills.
- You can build and discuss senior-level graph projects from social recommendations to fraud graphs to GraphRAG.

---

## 8. Interview Answer Pattern

For most Neo4j interview answers, use this shape:

```text
1. Domain question:
   What relationship-heavy problem are we solving?

2. Graph model:
   What nodes, labels, relationships, directions, properties, constraints, and indexes support it?

3. Traversal/query:
   Where does the query start, what pattern does it traverse, and how is fan-out bounded?

4. Data freshness and sync:
   How does data reach Neo4j and how stale can it be?

5. Scale and operations:
   What indexes, query plans, transaction patterns, cluster/read scaling, backup, and SLOs matter?

6. Failure mode:
   What breaks under unbounded traversal, supernodes, bad cardinality, missing indexes, or stale identity resolution?

7. Tradeoff and alternative:
   What gets faster/slower, simpler/harder, safer/riskier, and what would we use instead?
```

---

## 9. Recommended Study Orders

### 2-Week Practical Path

1. Starter Path files 1-4.
2. Cypher, modeling, constraints/indexes, and drivers files 5-8.
3. Import/sync, performance, algorithms, security, and GraphRAG files 9-15.
4. Active recall and hands-on labs.

### 4-Week MAANG Path

1. Week 1: Starter + Cypher basics + graph modeling.
2. Week 2: constraints, indexes, drivers, transactions, import, query plans.
3. Week 3: graph algorithms, clustering, security, scale, GraphRAG, cloud/testing.
4. Week 4: system design cases, anti-pattern debugging, interview Q&A, projects.
5. Final pass: pro gap-fill appendix, traversal budget, hot-node analysis, SLOs, and design-review checklist.

### Production Debugging Path

1. Read graph model, Cypher, constraints/indexes, and query plans.
2. Read clustering, security/backup, scaling, and observability.
3. Practice incidents: slow traversal, Cartesian product, hot node, missing index, lock contention, stale graph projection, bad identity resolution.
4. Score yourself with the Q&A and active recall sheets.

---

## 10. Readiness Gate

You are Neo4j interview-ready when you can do all of this without notes:

- Explain Neo4j as a native graph database optimized for relationship traversal.
- Design graph models for social recommendations, fraud, permissions, identity, supply chain, network topology, and knowledge graphs.
- Choose labels, relationship types, directions, properties, constraints, and indexes correctly.
- Write Cypher with MATCH, WHERE, MERGE, OPTIONAL MATCH, aggregation, UNWIND, subqueries, path patterns, and bounded traversals.
- Explain query plans with EXPLAIN/PROFILE, db hits, cardinality, indexes, joins, and Cartesian products.
- Explain transactions, drivers, retries, bookmarks, import, batch writes, and sync pipelines.
- Explain graph algorithms, pathfinding, centrality, community detection, similarity, and recommendation patterns.
- Explain clustering, backups, multi-tenancy, security, and operational monitoring.
- Debug slow traversal, hot nodes, lock contention, missing indexes, stale projections, and bad graph models.
- Compare Neo4j with PostgreSQL, MongoDB, Cassandra, Elasticsearch, vector databases, RDF/triplestores, and graph analytics engines.
- Design GraphRAG with entity resolution, provenance, graph traversal, vector retrieval, permissions, and grounded evaluation.
- Give a system design answer that includes graph model, traversal, constraints/indexes, performance, SLOs, failure modes, and alternatives.