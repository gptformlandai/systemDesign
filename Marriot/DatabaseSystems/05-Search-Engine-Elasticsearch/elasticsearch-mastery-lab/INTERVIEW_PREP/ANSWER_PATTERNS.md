# Elasticsearch Answer Patterns

## Design Answer

```text
I start from the search use case. For <intent>, I would index <document shape> into <index/alias>. The mapping uses <field types> and <analyzers> because <search behavior>. The query uses <DSL> with filters for <tenant/security> and ranking via <signals>. Data syncs from <source of truth> with freshness SLO <target>. Operationally I watch <latency/rejects/heap/disk/relevance> and use <alternative> for unsupported workloads.
```

## Debugging Answer

```text
I start with the endpoint, exact query DSL, and index alias. Then I inspect mappings, shard fan-out, slow logs, profile output, heap/GC, search or indexing rejections, merge pressure, disk watermarks, and sync lag. If structural, I reindex with corrected mappings or index strategy.
```

## Tradeoff Answer

```text
Elasticsearch gives full-text search, filters, facets, aggregations, and retrieval at scale. The cost is sync complexity, near-real-time freshness, mapping discipline, relevance evaluation, shard operations, and source-of-truth separation.
```