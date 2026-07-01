# Elasticsearch Performance Tuning and Observability - MAANG Master Sheet

> Track File #13 of 27 - Group 03: Senior / MAANG
> For: backend/search/system design interviews | Level: senior | Mode: p99 debugging, metrics, slow logs, runbooks

This sheet builds:
- Elasticsearch performance debugging workflow
- Metrics and symptoms for slow search, indexing pressure, heap, merges, hot shards, and disk watermarks
- Interview-ready incident response

---

## 1. Performance First Principle

Most Elasticsearch performance problems are one of these:

```text
bad mapping, expensive query, too many shards, hot shard, high-cardinality aggregation, deep pagination, merge pressure, refresh pressure, heap pressure, disk watermark, or sync overload
```

Do not start by randomly increasing heap. Start with query/index/workload evidence.

---

## 2. Debugging Workflow

```text
symptom -> API/query/index -> shard fan-out -> mappings/analyzers -> slow logs/profile -> node metrics -> mitigation -> design fix
```

Ask:

- Which endpoint/query is slow?
- Which index/alias is hit?
- How many shards are queried?
- Is there deep pagination?
- Is an aggregation high-cardinality?
- Are sorts on doc-values-friendly fields?
- Is heap/GC high?
- Are merges or refreshes overloaded?
- Are writes rejected?
- Is disk above watermarks?

---

## 3. Useful Signals

| Signal | What It Suggests |
|---|---|
| slow search logs | expensive DSL, shard fan-out, aggregations |
| high heap/GC | fielddata, aggregations, query load, too many shards |
| search rejections | thread-pool saturation |
| indexing rejections | bulk too aggressive or node pressure |
| high merge time | segment merge backlog |
| disk watermark | allocation blocked or cluster at risk |
| hot shard | routing/skew or uneven index design |
| high refresh cost | too frequent refresh or write-heavy workload |

---

## 4. Common Fixes

| Problem | Fix |
|---|---|
| slow full-text query | analyzer/query tuning, field boosts, filters, profile API |
| deep pagination | `search_after` and point-in-time |
| high-cardinality facet | reduce field cardinality, composite agg, precompute, cache UX |
| hot shard | routing/index redesign, rollover, tenant isolation |
| write pressure | bulk tuning, refresh interval, ingest scaling |
| heap pressure | reduce fielddata, shard count, aggregation cost, mapping explosion |
| disk watermark | add capacity, delete/ILM, snapshot, reduce replicas carefully |

---

## 5. Capacity Thinking

Estimate:

- documents per day
- average document size
- indexed expansion factor
- retention
- replica count
- shard target size
- query concurrency
- aggregation workload
- refresh/freshness SLO
- snapshot and recovery time

Rough storage formula:

```text
source_bytes * index_expansion * (1 + replica_count) * retention * headroom
```

---

## 6. Strong Answer

Question:

> Search latency suddenly gets worse. How do you debug it?

Strong answer:

```text
I start with the exact endpoint, query DSL, and index alias. I check whether the query changed, how many shards it fans out to, whether it uses deep pagination, expensive wildcard/regex, high-cardinality aggregations, or bad sorts. Then I inspect slow logs, profile sampled queries, node heap/GC, search thread-pool rejections, merges, disk watermarks, and hot shards. The durable fix may be query tuning, mapping changes, shard/ILM redesign, or separating user-facing search from analytics-heavy workloads.
```

---

## 7. Revision Notes

- One-line summary: Elasticsearch p99 debugging starts from query/index evidence, then moves to shard and node signals.
- Three keywords: slow log, heap, hot shard.
- One interview trap: blaming Elasticsearch before inspecting query DSL and mappings.
- Memory trick: slow search has a query shape and a shard shape.