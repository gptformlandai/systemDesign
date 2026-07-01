# Architecture Comparison Interview Track Index

This folder is the architecture comparison track for choosing databases, data stores, and retrieval systems in system design interviews and production architecture reviews.

It connects the individual database tracks into one decision-making skill:

```text
requirements -> workload shape -> consistency needs -> query patterns -> scale profile -> operational risk -> datastore choice -> tradeoffs -> production plan
```

Use this track if:

- You want to decide when to use SQL/PostgreSQL, MongoDB, Cassandra, Elasticsearch/OpenSearch, Vector DB/Pinecone, Neo4j, Redis/cache, object storage, time-series stores, warehouses, or lakehouses.
- You want crisp pros/cons and interview-ready comparison answers.
- You want to explain production relevance: SLOs, scale, consistency, failure modes, cost, security, backups, data sync, and operational ownership.
- You want MAANG-level system design answers where the database choice is justified instead of guessed.

---

## 1. Learning Style: Beginner To MAANG Loop

Every topic should be learned with this loop:

```text
product requirement -> access pattern -> data model -> consistency/SLO -> candidate systems -> tradeoffs -> failure mode -> final architecture answer
```

Architecture comparison mastery is not memorizing pros and cons. It is mapping a workload to the storage engine and operational model that best fits it.

---

## 2. Track Structure

| Group | Folder | Purpose |
|---:|---|---|
| 1 | `01-Starter-Path` | datastore families, decision dimensions, access patterns, source-of-truth vs derived stores |
| 2 | `02-Intermediate-Backend` | SQL vs NoSQL, document, wide-column, search, graph, vector, cache, object storage, time-series, warehouse comparisons |
| 3 | `03-Senior-MAANG` | consistency, CAP/PACELC, scaling, polyglot persistence, SLOs, cost, CDC, operations |
| 4 | `04-Scenario-Practice` | ecommerce, chat/feed, RAG/search, observability, fraud, payments, analytics case studies |
| 5 | `05-Special-Interview-Rounds` | anti-patterns, debugging, interview Q&A, decision traps |
| 6 | `06-Practice-Upgrade` | active recall, exercises, mini projects, cheat sheets, pro design review |
| Lab | `architecture-comparison-lab` | scenario drills, decision matrices, projects, runbooks, and interview prep |

---

## 3. Starter Path

| Order | File | What It Builds |
|---:|---|---|
| 1 | [01-Starter-Path/Architecture-Comparison-Master-Map-Data-Store-Families-Hot-Interview-Sheet.md](01-Starter-Path/Architecture-Comparison-Master-Map-Data-Store-Families-Hot-Interview-Sheet.md) | datastore families, mental model, high-signal use cases |
| 2 | [01-Starter-Path/Architecture-Comparison-Decision-Dimensions-CAP-SLO-Cost-Gold-Sheet.md](01-Starter-Path/Architecture-Comparison-Decision-Dimensions-CAP-SLO-Cost-Gold-Sheet.md) | consistency, latency, scale, cost, operations, security decision axes |
| 3 | [01-Starter-Path/Architecture-Comparison-Workload-Access-Patterns-Gold-Sheet.md](01-Starter-Path/Architecture-Comparison-Workload-Access-Patterns-Gold-Sheet.md) | reads/writes, point lookup, range scan, full-text, graph traversal, vector similarity, analytics |
| 4 | [01-Starter-Path/Architecture-Comparison-Source-Of-Truth-Derived-Indexes-Gold-Sheet.md](01-Starter-Path/Architecture-Comparison-Source-Of-Truth-Derived-Indexes-Gold-Sheet.md) | primary database vs cache/search/vector/graph/materialized derived stores |

Starter target:

- You can name the major datastore families and their significance.
- You can map a product requirement to an access pattern.
- You can separate source of truth from derived indexes.

---

## 4. Intermediate Backend Path

| Order | File | What It Builds |
|---:|---|---|
| 5 | [02-Intermediate-Backend/Architecture-Comparison-SQL-vs-NoSQL-Foundations-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-SQL-vs-NoSQL-Foundations-Gold-Sheet.md) | relational vs NoSQL mental models, transactions, schemas, scale patterns |
| 6 | [02-Intermediate-Backend/Architecture-Comparison-PostgreSQL-vs-MongoDB-Document-Tradeoffs-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-PostgreSQL-vs-MongoDB-Document-Tradeoffs-Gold-Sheet.md) | relational vs document modeling, joins, flexible schema, transactional boundaries |
| 7 | [02-Intermediate-Backend/Architecture-Comparison-Cassandra-vs-SQL-vs-MongoDB-Scale-Tradeoffs-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-Cassandra-vs-SQL-vs-MongoDB-Scale-Tradeoffs-Gold-Sheet.md) | wide-column design, partition keys, massive writes, query-driven modeling |
| 8 | [02-Intermediate-Backend/Architecture-Comparison-Elasticsearch-vs-DBs-Search-Tradeoffs-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-Elasticsearch-vs-DBs-Search-Tradeoffs-Gold-Sheet.md) | search engine vs database, text search, aggregations, derived index risk |
| 9 | [02-Intermediate-Backend/Architecture-Comparison-Neo4j-vs-Joins-vs-Search-vs-Vector-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-Neo4j-vs-Joins-vs-Search-vs-Vector-Gold-Sheet.md) | graph traversal vs joins/search/vector similarity |
| 10 | [02-Intermediate-Backend/Architecture-Comparison-VectorDB-vs-Search-vs-Graph-vs-SQL-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-VectorDB-vs-Search-vs-Graph-vs-SQL-Gold-Sheet.md) | vector similarity, RAG, hybrid search, when vector DB is wrong |
| Gap Fill | [02-Intermediate-Backend/Architecture-Comparison-Cache-ObjectStorage-TimeSeries-Warehouse-Gap-Fill-Gold-Sheet.md](02-Intermediate-Backend/Architecture-Comparison-Cache-ObjectStorage-TimeSeries-Warehouse-Gap-Fill-Gold-Sheet.md) | Redis/cache, object storage, time-series, warehouse/lakehouse boundaries and tradeoffs |

Intermediate target:

- You can compare datastore families with concrete tradeoffs.
- You can explain why one system is strong for one access pattern and weak for another.
- You can separate primary OLTP stores from cache, blob, metrics, and analytics systems.

---

## 5. Senior / MAANG Path

| Order | File | What It Builds |
|---:|---|---|
| 11 | [03-Senior-MAANG/Architecture-Comparison-Consistency-Transactions-CAP-PACELC-MAANG-Sheet.md](03-Senior-MAANG/Architecture-Comparison-Consistency-Transactions-CAP-PACELC-MAANG-Sheet.md) | consistency, isolation, CAP, PACELC, read/write correctness |
| 12 | [03-Senior-MAANG/Architecture-Comparison-Scaling-Sharding-Replication-Partitioning-MAANG-Sheet.md](03-Senior-MAANG/Architecture-Comparison-Scaling-Sharding-Replication-Partitioning-MAANG-Sheet.md) | partitioning, sharding, replication, hot keys, global scale |
| 13 | [03-Senior-MAANG/Architecture-Comparison-Polyglot-Persistence-System-Architecture-MAANG-Sheet.md](03-Senior-MAANG/Architecture-Comparison-Polyglot-Persistence-System-Architecture-MAANG-Sheet.md) | using multiple data stores safely in one architecture |
| 14 | [03-Senior-MAANG/Architecture-Comparison-SLO-Operations-Backup-DR-Security-Gold-Sheet.md](03-Senior-MAANG/Architecture-Comparison-SLO-Operations-Backup-DR-Security-Gold-Sheet.md) | operations, backup, DR, security, compliance, observability |
| 15 | [03-Senior-MAANG/Architecture-Comparison-Cost-Capacity-Latency-Storage-MAANG-Sheet.md](03-Senior-MAANG/Architecture-Comparison-Cost-Capacity-Latency-Storage-MAANG-Sheet.md) | capacity math, cost drivers, p99, storage growth, retention |
| 16 | [03-Senior-MAANG/Architecture-Comparison-CDC-Sync-Materialized-Views-Derived-Stores-Gold-Sheet.md](03-Senior-MAANG/Architecture-Comparison-CDC-Sync-Materialized-Views-Derived-Stores-Gold-Sheet.md) | CDC, dual writes, eventing, materialized views, index freshness |

Senior target:

- You can justify data architecture under consistency, latency, scale, failure, and operations constraints.
- You can explain how multiple stores stay synchronized and where they fail.

---

## 6. Scenario Practice Path

| Order | File | What It Builds |
|---:|---|---|
| 17 | [04-Scenario-Practice/Architecture-Comparison-Ecommerce-Marketplace-Case-Study-MAANG-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Ecommerce-Marketplace-Case-Study-MAANG-Sheet.md) | product catalog, orders, search, recommendations, inventory, payments |
| 18 | [04-Scenario-Practice/Architecture-Comparison-Chat-Feed-Notification-Case-Study-Gold-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Chat-Feed-Notification-Case-Study-Gold-Sheet.md) | chat messages, feeds, timelines, fanout, notifications |
| 19 | [04-Scenario-Practice/Architecture-Comparison-RAG-Search-Knowledge-Graph-Case-Study-MAANG-Sheet.md](04-Scenario-Practice/Architecture-Comparison-RAG-Search-Knowledge-Graph-Case-Study-MAANG-Sheet.md) | RAG, search, vector DB, graph, document stores, ACLs |
| 20 | [04-Scenario-Practice/Architecture-Comparison-Observability-Logs-Metrics-TimeSeries-Case-Study-Gold-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Observability-Logs-Metrics-TimeSeries-Case-Study-Gold-Sheet.md) | logs, metrics, traces, time-series, search, retention, cost |
| 21 | [04-Scenario-Practice/Architecture-Comparison-Fraud-Risk-Identity-Case-Study-MAANG-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Fraud-Risk-Identity-Case-Study-MAANG-Sheet.md) | fraud graph, risk signals, streaming, feature stores, graph/vector tradeoffs |
| 22 | [04-Scenario-Practice/Architecture-Comparison-Payments-Orders-Ledger-Case-Study-MAANG-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Payments-Orders-Ledger-Case-Study-MAANG-Sheet.md) | strong consistency, ledger, idempotency, audit, reconciliation |
| 23 | [04-Scenario-Practice/Architecture-Comparison-Analytics-Reporting-Warehouse-Lakehouse-Case-Study-Gold-Sheet.md](04-Scenario-Practice/Architecture-Comparison-Analytics-Reporting-Warehouse-Lakehouse-Case-Study-Gold-Sheet.md) | OLTP vs OLAP, warehouse, lakehouse, ETL/ELT, dashboards |

Scenario target:

- You can answer real system design prompts with a multi-store production architecture and clear tradeoffs.

---

## 7. Special Interview Rounds

| Order | File | What It Builds |
|---:|---|---|
| 24 | [05-Special-Interview-Rounds/Architecture-Comparison-Anti-Patterns-Debugging-Decision-Traps-MAANG-Sheet.md](05-Special-Interview-Rounds/Architecture-Comparison-Anti-Patterns-Debugging-Decision-Traps-MAANG-Sheet.md) | bad database choices, dual-write bugs, overuse of one system, scaling traps |
| 25 | [05-Special-Interview-Rounds/Architecture-Comparison-Interview-Prep-QA-MAANG-Sheet.md](05-Special-Interview-Rounds/Architecture-Comparison-Interview-Prep-QA-MAANG-Sheet.md) | direct comparison Q&A and system design answer patterns |

Special-round target:

- You can defend and challenge database choices in interviews.

---

## 8. Practice Upgrade Path

| Order | File | What It Builds |
|---:|---|---|
| 26 | [06-Practice-Upgrade/Architecture-Comparison-Active-Recall-Question-Bank.md](06-Practice-Upgrade/Architecture-Comparison-Active-Recall-Question-Bank.md) | recall prompts across beginner to MAANG comparisons |
| 27 | [06-Practice-Upgrade/Architecture-Comparison-Hands-On-Exercises-And-Decision-Drills.md](06-Practice-Upgrade/Architecture-Comparison-Hands-On-Exercises-And-Decision-Drills.md) | practical workload-to-datastore drills |
| 28 | [06-Practice-Upgrade/Architecture-Comparison-Mini-Projects-Portfolio.md](06-Practice-Upgrade/Architecture-Comparison-Mini-Projects-Portfolio.md) | portfolio projects with datastore decisions and tradeoffs |
| 29 | [06-Practice-Upgrade/Architecture-Comparison-Cheat-Sheets-Roadmap-Golden-Rules.md](06-Practice-Upgrade/Architecture-Comparison-Cheat-Sheets-Roadmap-Golden-Rules.md) | cheat sheets, roadmap, golden rules, readiness checklist |
| 30 | [06-Practice-Upgrade/Architecture-Comparison-Pro-Gap-Fill-Decision-Review-Checklist.md](06-Practice-Upgrade/Architecture-Comparison-Pro-Gap-Fill-Decision-Review-Checklist.md) | staff-level architecture review checklist and scoring rubric |

Practice target:

- You can evaluate a workload, choose storage systems, explain tradeoffs, and defend production operations.

---

## 9. Comparison Lab

Use the consolidated lab when you want scenario drills instead of reading-only notes:

- [architecture-comparison-lab/README.md](architecture-comparison-lab/README.md)
- [architecture-comparison-lab/LEARNING_PATH.md](architecture-comparison-lab/LEARNING_PATH.md)

Lab target:

- You can score datastore candidates against workload requirements.
- You can rehearse interview answers for common production scenarios.
- You can identify source-of-truth, derived indexes, CDC, cache, search, vector, graph, and analytics boundaries.

---

## 10. Interview Answer Pattern

For most architecture comparison answers, use this shape:

```text
1. Requirement:
   What product workflow and correctness requirement matter most?

2. Access pattern:
   Is this point lookup, transaction, range query, search, graph traversal, vector similarity, stream, or analytics?

3. Source of truth:
   Which store owns the canonical state?

4. Candidate systems:
   Which systems fit and which are weak?

5. Tradeoff:
   Compare consistency, latency, scale, cost, operations, schema, query flexibility, and failure modes.

6. Production plan:
   Include indexes, partitioning, replication, backups, DR, security, monitoring, CDC, and reprocessing.

7. Final decision:
   Pick one primary choice, name derived stores if needed, and state what would make you change the decision.
```

---

## 11. Recommended Study Orders

### 2-Week Practical Path

1. Starter files 1-4.
2. Intermediate comparison files 5-10.
3. Scenario files 17-23.
4. Active recall and decision drills.

### 4-Week MAANG Path

1. Week 1: datastore families, decision dimensions, access patterns, source of truth vs derived stores.
2. Week 2: SQL, MongoDB, Cassandra, Elasticsearch, Neo4j, VectorDB, cache, object storage, time-series, warehouse, and lakehouse comparisons.
3. Week 3: consistency, CAP/PACELC, scaling, polyglot persistence, SLOs, cost, CDC.
4. Week 4: scenario case studies, anti-patterns, interview Q&A, projects, pro review checklist.

### Production Architecture Review Path

1. Identify workflow and data ownership.
2. Map access patterns and SLOs.
3. Choose source of truth and derived stores.
4. Design sync/CDC/freshness and failure recovery.
5. Score cost, security, DR, observability, and operational ownership.

---

## 12. Readiness Gate

You are architecture-comparison interview-ready when you can do all of this without notes:

- Explain the significance of SQL, document, wide-column, search, graph, vector, cache, object storage, time-series, warehouse, and lakehouse systems.
- Choose a datastore from access patterns and correctness requirements.
- Separate source-of-truth systems from derived stores like cache, search index, vector index, graph projection, and analytics warehouse.
- Compare PostgreSQL, MongoDB, Cassandra, Elasticsearch/OpenSearch, Pinecone/Qdrant, Neo4j, Redis, S3/object storage, and warehouses with pros/cons.
- Explain CAP/PACELC, transactions, consistency, replication, sharding, partitioning, hot keys, and global scale tradeoffs.
- Design CDC and materialized views without hand-waving dual-write failures.
- Cover production SLOs, backups, DR, security, observability, cost, and operational ownership.
- Answer ecommerce, chat/feed, RAG/search, observability, fraud, payments, and analytics scenarios with clear storage choices.