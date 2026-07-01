# Elasticsearch Pro Gap Fill: Capacity, Relevance, SLOs, and Design Review

> Track File #27 of 27 - Group 06: Practice Upgrade
> For: backend/search/system design interviews | Level: pro / MAANG | Mode: final gap fill, production design review, staff-level checklist

This sheet fills the advanced gaps that separate a good Elasticsearch learner from a production-ready search designer:

- shard/capacity worksheet
- relevance evaluation and rollout safety
- schema/mapping evolution
- sync freshness and stale-result SLOs
- tenant/ACL and RAG retrieval safety
- final design-review checklist

---

## 1. Pro Mental Model

```text
pro Elasticsearch design = search intent + document model + mapping/analyzer + query/ranking + sync freshness + shard capacity + SLO + incident plan
```

If your design cannot explain relevance, freshness, security, migration, and failure behavior, it is not finished.

---

## 2. Capacity Worksheet

| Input | Example |
|---|---|
| documents/day | 50 million |
| average source document | 2 KB |
| index expansion | 1.5x to 3x depending on fields/analyzers |
| retention | 30 days |
| replica count | 1 |
| target shard size | platform/team dependent |
| query concurrency | 500 QPS |
| aggregation workload | heavy facets or dashboards |
| refresh SLO | 1 to 5 seconds |

Rough storage:

```text
storage = docs * source_size * expansion * (1 + replicas) * retention * headroom
```

Design check:

```text
Shard count should satisfy target shard size, search fan-out, recovery time, and cluster-state overhead together.
```

---

## 3. Relevance Evaluation

Use a golden query set:

- common head queries
- long-tail queries
- typo queries
- synonym queries
- exact SKU/name queries
- zero-result-prone queries
- sensitive/ACL queries

Track:

- precision/recall on judged queries
- CTR/conversion
- zero-result rate
- reformulation rate
- abandonment
- latency
- freshness

Rule:

```text
Analyzer, synonym, boost, or vector changes need relevance tests before rollout.
```

---

## 4. Mapping Evolution

You cannot freely change many field mappings in place. Use versioned indices and aliases.

Safe pattern:

1. Create new index version with mapping.
2. Backfill from source of truth.
3. Dual-write if needed.
4. Run relevance and count validation.
5. Switch alias atomically.
6. Keep rollback window.
7. Delete old index after retention/approval.

---

## 5. SLO And Alert Design

Define SLOs per search path:

| Search Path | SLO | Alerts |
|---|---|---|
| product search | p99 < 200 ms | latency, zero results, stale price, search rejects |
| log search | p99 < 2 s | query latency, disk watermarks, ingest lag |
| autocomplete | p99 < 50 ms | latency, timeout, low-result rate |
| RAG retrieval | p99 < 500 ms | latency, ACL leak tests, stale embeddings |

Minimum dashboard:

- search/indexing latency
- slow logs
- search/indexing rejections
- heap/GC
- merge time
- disk watermarks
- shard allocation and hot shards
- bulk failures and sync lag
- zero-result rate and relevance metrics

---

## 6. Staff-Level Design Review

Ask these before approving a design:

### Search Model

- What exact user intents are supported?
- What queries are intentionally not supported?
- What is the source of truth?

### Mapping And Ranking

- What fields are `text`, `keyword`, `nested`, `flattened`, vector, geo, date, numeric?
- What analyzers and synonyms are used?
- What ranking signals are measured?

### Sync And Freshness

- How do documents reach Elasticsearch?
- What is the freshness SLO?
- What happens when sync lags or fails?
- How do we reindex safely?

### Scale And Operations

- What shard and replica strategy is used?
- What ILM or rollover policy exists?
- What SLOs and alerts exist?
- What snapshot and restore plan exists?

### Security

- How are tenant/ACL filters enforced?
- How is PII protected?
- How are API keys and roles scoped?

---

## 7. Staff-Level Interview Answer Template

```text
I would use Elasticsearch for <search use case>, while keeping <source system> as source of truth. The index document is <shape>, with mappings/analyzers chosen for <field behavior>. The query uses <DSL> with filters for <tenant/security>, ranking via <signals>, and facets via <aggregations>. Data syncs through <pipeline> with freshness SLO <target>. Operationally, I would use <shards/ILM/aliases>, monitor <latency/rejects/heap/disk/relevance>, and reject Elasticsearch for <unsupported workload> by using <alternative>.
```

---

## 8. Revision Notes

- One-line summary: Pro Elasticsearch design proves search quality, freshness, security, capacity, and operations.
- Three keywords: capacity, relevance, SLO.
- One interview trap: designing the query but not the quality or sync model.
- Memory trick: search is not done until the result is relevant, fresh, authorized, and observable.