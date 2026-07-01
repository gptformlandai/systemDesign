# Architecture Comparison Answer Patterns

## Design Answer

```text
I start from the workflow and access pattern. I choose a source of truth for correctness, add derived stores only for specialized reads, synchronize them with CDC/events, and cover consistency, p99 latency, freshness, cost, security, backup/DR, and rejected alternatives.
```

## Debugging Answer

```text
I identify the source of truth first, then inspect derived stores, CDC lag, cache invalidation, delete/permission propagation, schema/index changes, and recent deploys. Stale derived stores are fixed with replay, rebuild, and freshness alerts.
```

## Tradeoff Answer

```text
No datastore is universally best. SQL is strong for transactions, MongoDB for document aggregates, Cassandra for high-scale key-based access, Elasticsearch for search, VectorDB for semantic similarity, Neo4j for paths, Redis for cache, object storage for blobs, and warehouses for analytics.
```