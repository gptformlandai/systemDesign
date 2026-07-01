# Elasticsearch Operations Cheatsheet

## Inspect

```bash
curl http://localhost:9200/_cluster/health?pretty
curl http://localhost:9200/_cat/indices?v
curl http://localhost:9200/_cat/shards?v
curl http://localhost:9200/_nodes/stats/jvm,fs,indices,thread_pool?pretty
```

## Watch

| Area | Signal |
|---|---|
| latency | p95/p99 search and indexing latency |
| rejects | search/indexing thread-pool rejections |
| heap | heap usage and GC pauses |
| shards | hot shards, unassigned shards |
| disk | watermarks and growth |
| mappings | field count and dynamic field growth |
| sync | bulk failures and freshness lag |
| relevance | zero-result rate and golden-query regressions |

## Incident Formula

```text
symptom -> query/index alias -> mappings -> shard fan-out -> slow logs/profile -> node metrics -> mitigation -> design fix
```