# Architecture Comparison Mastery Sheet System - Start Here

This folder is a modular beginner-to-pro track for datastore and data architecture comparison.

Start with:

- [Architecture-Comparison-Interview-Track-Index.md](Architecture-Comparison-Interview-Track-Index.md)
- [architecture-comparison-lab/README.md](architecture-comparison-lab/README.md)
- [architecture-comparison-lab/LEARNING_PATH.md](architecture-comparison-lab/LEARNING_PATH.md)

The goal:

```text
Architecture comparison mastery = choose the right data store for the workload, explain tradeoffs, and design production operations around it
```

The modular track has:

- `01-Starter-Path` for datastore families, decision dimensions, access patterns, and source-of-truth vs derived stores.
- `02-Intermediate-Backend` for SQL/NoSQL, MongoDB, Cassandra, Elasticsearch, Neo4j, VectorDB, cache, object storage, time-series, warehouse, and lakehouse comparisons.
- `03-Senior-MAANG` for consistency, CAP/PACELC, scaling, sharding, replication, polyglot persistence, SLOs, cost, security, backup, DR, CDC, and derived stores.
- `04-Scenario-Practice` for ecommerce, chat/feed, RAG/search, observability, fraud/risk, payments/ledger, and analytics/reporting scenarios.
- `05-Special-Interview-Rounds` for anti-patterns, debugging, decision traps, and interview Q&A.
- `06-Practice-Upgrade` for active recall, decision drills, mini projects, cheat sheets, roadmap, golden rules, and pro architecture review.
- `architecture-comparison-lab` for practical scenario scoring, decision matrices, projects, runbooks, and interview prep.

Core mental model:

```text
requirement + access pattern + correctness + scale + operations = datastore decision
```

Use the root index as the source of truth for study order.