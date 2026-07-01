# Neo4j Operations Cheatsheet

## Inspect

```cypher
SHOW DATABASES;
SHOW CONSTRAINTS;
SHOW INDEXES;
MATCH (n) RETURN labels(n), count(n);
MATCH ()-[r]->() RETURN type(r), count(r);
```

## Watch

| Area | Signal |
|---|---|
| latency | p95/p99 query and transaction latency |
| query plan | rows, db hits, Cartesian products |
| memory | heap and page cache |
| writes | transaction failures, retries, locks |
| storage | disk and transaction log growth |
| graph shape | high-degree nodes and relationship counts |
| sync | failed events and freshness lag |
| quality | duplicate entities and stale relationships |

## Incident Formula

```text
symptom -> Cypher/params -> anchor/index -> traversal depth/fan-out -> PROFILE -> mitigation -> model/query fix
```