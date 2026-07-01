# Cassandra Mastery Sheet System - Start Here

This folder is a modular beginner-to-pro Cassandra track.

Start with:

- [Cassandra-Interview-Track-Index.md](Cassandra-Interview-Track-Index.md)
- [cassandra-mastery-lab/README.md](cassandra-mastery-lab/README.md)
- [cassandra-mastery-lab/LEARNING_PATH.md](cassandra-mastery-lab/LEARNING_PATH.md)

The modular track has:

- `01-Starter-Path` for fundamentals, architecture, setup, CQL, and basic query modeling.
- `02-Intermediate-Backend` for access-pattern data modeling, primary keys, indexes, read/write path, application integration, and time-series patterns.
- `03-Senior-MAANG` for consistency, replication, topology, compaction, repair, performance, security, cloud, Kubernetes, and testing.
- `04-Scenario-Practice` for microservices, event-driven systems, system design cases, and database tradeoff analysis.
- `05-Special-Interview-Rounds` for anti-patterns, internals, debugging, and interview Q&A.
- `06-Practice-Upgrade` for active recall, runnable labs, mini projects, cheat sheets, roadmap, and golden rules.
- `cassandra-mastery-lab` for local Docker/cqlsh practice guidance, staged learning, and project/lab workflow.

Core mental model:

```text
Cassandra mastery = access pattern + partition key + clustering order + consistency level + compaction/repair cost + failure-mode explanation
```

Use the root index as the source of truth for study order.