# Elasticsearch Mastery Sheet System - Start Here

This folder is a modular beginner-to-pro Elasticsearch track.

Start with:

- [Elasticsearch-Interview-Track-Index.md](Elasticsearch-Interview-Track-Index.md)
- [elasticsearch-mastery-lab/README.md](elasticsearch-mastery-lab/README.md)
- [elasticsearch-mastery-lab/LEARNING_PATH.md](elasticsearch-mastery-lab/LEARNING_PATH.md)

The modular track has:

- `01-Starter-Path` for fundamentals, architecture, setup, REST APIs, indexing, and search basics.
- `02-Intermediate-Backend` for mappings, analyzers, query DSL, aggregations, ingest/sync, relevance, app integration, and advanced DSL/modeling gap fills.
- `03-Senior-MAANG` for shards, replicas, scaling, ILM, performance, observability, security, vector/hybrid search, cloud, Kubernetes, and testing.
- `04-Scenario-Practice` for e-commerce search, observability/logging, security analytics, autocomplete, geospatial, multi-tenant ACL/RAG, system design cases, and search/database tradeoff analysis.
- `05-Special-Interview-Rounds` for anti-patterns, internals, debugging, and interview Q&A.
- `06-Practice-Upgrade` for active recall, runnable labs, mini projects, cheat sheets, roadmap, golden rules, and pro design review.
- `elasticsearch-mastery-lab` for local Docker/curl practice, sample data, scripts, alias/reindex drills, ACL/RAG drills, geo search drills, projects, runbooks, and interview prep.

Core mental model:

```text
Elasticsearch mastery = use case + document model + mapping/analyzer + query DSL + relevance + sync freshness + shard/ILM operations
```

Use the root index as the source of truth for study order.