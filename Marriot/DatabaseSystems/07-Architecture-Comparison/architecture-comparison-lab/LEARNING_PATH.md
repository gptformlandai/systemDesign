# Architecture Comparison Lab Learning Path

This path turns datastore selection into a repeatable interview and production design skill.

---

## Stage 1: Foundations

Read:

- `../01-Starter-Path/Architecture-Comparison-Master-Map-Data-Store-Families-Hot-Interview-Sheet.md`
- `../01-Starter-Path/Architecture-Comparison-Decision-Dimensions-CAP-SLO-Cost-Gold-Sheet.md`
- `../01-Starter-Path/Architecture-Comparison-Workload-Access-Patterns-Gold-Sheet.md`
- `../01-Starter-Path/Architecture-Comparison-Source-Of-Truth-Derived-Indexes-Gold-Sheet.md`

Run:

```bash
bash SCRIPTS/03-source-derived-map.sh
```

Lab:

- [LABS/01-source-of-truth-vs-derived.md](LABS/01-source-of-truth-vs-derived.md)
- [CHEATSHEETS/DECISION_MATRIX.md](CHEATSHEETS/DECISION_MATRIX.md)

---

## Stage 2: Core Comparisons

Read:

- `../02-Intermediate-Backend/Architecture-Comparison-SQL-vs-NoSQL-Foundations-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-PostgreSQL-vs-MongoDB-Document-Tradeoffs-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-Cassandra-vs-SQL-vs-MongoDB-Scale-Tradeoffs-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-Elasticsearch-vs-DBs-Search-Tradeoffs-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-Neo4j-vs-Joins-vs-Search-vs-Vector-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-VectorDB-vs-Search-vs-Graph-vs-SQL-Gold-Sheet.md`
- `../02-Intermediate-Backend/Architecture-Comparison-Cache-ObjectStorage-TimeSeries-Warehouse-Gap-Fill-Gold-Sheet.md`

Lab:

- [LABS/02-ecommerce-decision-matrix.md](LABS/02-ecommerce-decision-matrix.md)
- [LABS/03-rag-search-vector-graph.md](LABS/03-rag-search-vector-graph.md)
- [CHEATSHEETS/PROS_CONS.md](CHEATSHEETS/PROS_CONS.md)

---

## Stage 3: Senior Production Review

Read:

- `../03-Senior-MAANG/Architecture-Comparison-Consistency-Transactions-CAP-PACELC-MAANG-Sheet.md`
- `../03-Senior-MAANG/Architecture-Comparison-Scaling-Sharding-Replication-Partitioning-MAANG-Sheet.md`
- `../03-Senior-MAANG/Architecture-Comparison-Polyglot-Persistence-System-Architecture-MAANG-Sheet.md`
- `../03-Senior-MAANG/Architecture-Comparison-SLO-Operations-Backup-DR-Security-Gold-Sheet.md`
- `../03-Senior-MAANG/Architecture-Comparison-Cost-Capacity-Latency-Storage-MAANG-Sheet.md`
- `../03-Senior-MAANG/Architecture-Comparison-CDC-Sync-Materialized-Views-Derived-Stores-Gold-Sheet.md`

Lab:

- [LABS/04-payments-ledger-consistency.md](LABS/04-payments-ledger-consistency.md)
- [LABS/06-polyglot-sync-failure.md](LABS/06-polyglot-sync-failure.md)
- [RUNBOOKS/DUAL_WRITE_INCONSISTENCY.md](RUNBOOKS/DUAL_WRITE_INCONSISTENCY.md)
- [RUNBOOKS/STALE_DERIVED_STORE.md](RUNBOOKS/STALE_DERIVED_STORE.md)

---

## Stage 4: Scenario Design

Read:

- `../04-Scenario-Practice/Architecture-Comparison-Ecommerce-Marketplace-Case-Study-MAANG-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-Chat-Feed-Notification-Case-Study-Gold-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-RAG-Search-Knowledge-Graph-Case-Study-MAANG-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-Observability-Logs-Metrics-TimeSeries-Case-Study-Gold-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-Fraud-Risk-Identity-Case-Study-MAANG-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-Payments-Orders-Ledger-Case-Study-MAANG-Sheet.md`
- `../04-Scenario-Practice/Architecture-Comparison-Analytics-Reporting-Warehouse-Lakehouse-Case-Study-Gold-Sheet.md`

Projects:

- [PROJECTS/01-marketplace-data-architecture.md](PROJECTS/01-marketplace-data-architecture.md)
- [PROJECTS/02-enterprise-rag-data-architecture.md](PROJECTS/02-enterprise-rag-data-architecture.md)
- [PROJECTS/03-chat-feed-storage-architecture.md](PROJECTS/03-chat-feed-storage-architecture.md)
- [PROJECTS/04-fraud-risk-data-platform.md](PROJECTS/04-fraud-risk-data-platform.md)
- [PROJECTS/05-observability-data-platform.md](PROJECTS/05-observability-data-platform.md)

---

## Stage 5: Interview Readiness

Read:

- `../05-Special-Interview-Rounds/Architecture-Comparison-Anti-Patterns-Debugging-Decision-Traps-MAANG-Sheet.md`
- `../05-Special-Interview-Rounds/Architecture-Comparison-Interview-Prep-QA-MAANG-Sheet.md`
- `../06-Practice-Upgrade/Architecture-Comparison-Active-Recall-Question-Bank.md`
- `../06-Practice-Upgrade/Architecture-Comparison-Hands-On-Exercises-And-Decision-Drills.md`
- `../06-Practice-Upgrade/Architecture-Comparison-Mini-Projects-Portfolio.md`
- `../06-Practice-Upgrade/Architecture-Comparison-Cheat-Sheets-Roadmap-Golden-Rules.md`
- `../06-Practice-Upgrade/Architecture-Comparison-Pro-Gap-Fill-Decision-Review-Checklist.md`

MAANG deep-dive gate:

- Name access pattern before database.
- Name source of truth and derived stores.
- Explain sync, freshness, deletes, permissions, and rebuilds.
- Cover SLO, cost, security, backup, DR, and operations.
- Reject at least one alternative clearly.