# 33. Redis JSON, Search, Vector, Time Series, And Probabilistic Structures

## Goal

Teach Redis as a modern multi-model data platform, not just a key-value cache. These features are powerful, but they must be chosen carefully because they add indexing, memory, query planning, and operational complexity.

```text
document/query/vector/metric problem -> right Redis data model -> index/retention/error bound -> memory budget -> production fit
```

---

## 1. When This Sheet Matters

Use this sheet when the system design asks for:

- product catalog search
- autocomplete or filtering
- session/user/profile documents
- semantic cache or vector similarity
- recommendations
- fraud or dedupe filters
- metrics and time-series rollups
- approximate analytics at huge scale

Do not use these features automatically. Redis memory is expensive; every index and derived structure must justify itself.

---

## 2. JSON Documents

JSON lets Redis store nested documents and update paths without rewriting a giant string blob.

Classic anti-pattern:

```bash
# One large opaque blob.
SET user:1001 '{"id":1001,"name":"Alice","plan":"pro","prefs":{"theme":"dark"}}'
```

Problems:

- client must fetch entire value to update one field
- no server-side path update
- no direct indexing of fields
- large values increase network and replication cost

JSON pattern:

```bash
JSON.SET user:1001 $ '{"id":1001,"name":"Alice","plan":"pro","prefs":{"theme":"dark"}}'
JSON.GET user:1001 $.prefs.theme
JSON.SET user:1001 $.prefs.theme '"light"'
JSON.NUMINCRBY user:1001 $.login_count 1
```

Use JSON for:

| Use Case | Why JSON Fits |
|---|---|
| user profile document | partial updates and nested fields |
| product catalog item | searchable fields, tags, price, inventory |
| feature config | hierarchical data |
| session with nested metadata | readable document shape |

Avoid JSON when:

- a simple string/hash is enough
- values are huge and frequently rewritten
- you need relational joins
- you cannot afford index memory

---

## 3. Search And Query

Search/query adds secondary indexes over HASH or JSON documents.

Example product index:

```bash
FT.CREATE idx:products ON JSON PREFIX 1 product: SCHEMA \
  $.name AS name TEXT \
  $.brand AS brand TAG \
  $.category AS category TAG \
  $.price AS price NUMERIC SORTABLE \
  $.description AS description TEXT
```

Add products:

```bash
JSON.SET product:1001 $ '{"name":"Trail Shoe","brand":"Acme","category":"shoes","price":89.99,"description":"Lightweight running shoe"}'
JSON.SET product:1002 $ '{"name":"City Boot","brand":"Acme","category":"boots","price":129.99,"description":"Water resistant leather boot"}'
```

Search:

```bash
FT.SEARCH idx:products '@category:{shoes} @price:[50 100] running' RETURN 3 $.name $.price $.brand
```

Design decisions:

| Decision | Question |
|---|---|
| HASH vs JSON | Is the object flat or nested? |
| TEXT vs TAG | Do you need tokenized search or exact filtering? |
| NUMERIC | Do you filter/range/sort numbers? |
| SORTABLE | Do you need server-side sort? It costs memory. |
| PREFIX | Which keys belong to this index? |
| Index lifecycle | Can you rebuild without downtime? |

Common mistake:

```text
Indexing every field "just in case" wastes RAM and slows writes.
```

Better:

```text
Index only fields used by known access patterns.
```

---

## 4. Vector Search And Vector Sets

Vector search stores embeddings and finds nearby vectors by similarity.

Use cases:

- semantic cache for LLM responses
- RAG retrieval for short-lived or hot documents
- product recommendations
- image/audio/text similarity
- near-duplicate detection

Search index pattern:

```bash
FT.CREATE idx:docs ON HASH PREFIX 1 doc: SCHEMA \
  title TEXT \
  tenant TAG \
  embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE
```

Query pattern:

```bash
FT.SEARCH idx:docs '(@tenant:{acme})=>[KNN 5 @embedding $query_vector AS score]' \
  PARAMS 2 query_vector "<binary-vector>" \
  SORTBY score \
  RETURN 3 title tenant score \
  DIALECT 2
```

Vector Sets pattern:

```text
Vector Sets are useful when you want a Redis-native similarity collection rather than a full document search index.
```

Architecture decision:

| Need | Prefer |
|---|---|
| vector similarity only | Vector Sets or minimal vector index |
| vector + metadata filters | Search/query vector index |
| billion-scale vector corpus | dedicated vector database or search platform |
| hot semantic cache | Redis vector search with TTL and memory budget |

Production questions:

- What is embedding dimension?
- What is vector dtype and memory per vector?
- Do we need exact or approximate search?
- What recall/latency tradeoff is acceptable?
- How do we delete stale embeddings?
- How do we rebuild indexes after model changes?
- How do we isolate tenants?

Memory estimate:

```text
raw vector memory ~= number_of_vectors * dimensions * bytes_per_dimension

Example:
1,000,000 vectors * 1536 dims * 4 bytes ~= 6.1 GB raw vector data

Index overhead can add significant memory beyond raw vectors.
```

Interview sound bite:

```text
I use Redis vector search for hot, low-latency similarity workloads where the corpus fits in memory and metadata filters are simple. For massive corpus, long retention, complex ranking, or cheaper storage, I prefer a dedicated vector/search system and keep Redis as a cache.
```

---

## 5. Time Series

Time Series is for timestamped samples with retention and rollups.

Example:

```bash
TS.CREATE metrics:api:latency:p95 RETENTION 604800000 LABELS service api metric p95
TS.ADD metrics:api:latency:p95 * 83.4
TS.RANGE metrics:api:latency:p95 - + AGGREGATION avg 60000
```

Rollup pattern:

```bash
TS.CREATE metrics:api:latency:p95:1m RETENTION 2592000000
TS.CREATERULE metrics:api:latency:p95 metrics:api:latency:p95:1m AGGREGATION avg 60000
```

Use Time Series for:

| Use Case | Pattern |
|---|---|
| service latency | sample per time bucket, roll up by minute |
| IoT sensor samples | retention by device or tenant |
| business counters | order count, payment failure rate |
| real-time dashboards | latest value + range query |

Avoid Time Series when:

- data volume is huge and RAM cost is too high
- long-term historical analytics belongs in object storage or a warehouse
- query patterns need joins or complex SQL

---

## 6. Probabilistic Structures

Probabilistic structures trade exactness for huge memory savings.

### Bloom Filter

```bash
BF.RESERVE seen:emails 0.001 1000000
BF.ADD seen:emails alice@example.com
BF.EXISTS seen:emails alice@example.com
```

Guarantee:

```text
No false negatives. Possible false positives.
```

Use for:

- avoid DB lookup for definitely-missing items
- duplicate check before expensive processing
- bot/fraud prefilter

### Cuckoo Filter

```bash
CF.RESERVE seen:tokens 1000000
CF.ADD seen:tokens token-abc
CF.EXISTS seen:tokens token-abc
CF.DEL seen:tokens token-abc
```

Cuckoo filters can support deletion, which Bloom filters traditionally do not.

### Count-Min Sketch

```bash
CMS.INITBYPROB cms:views 0.001 0.99
CMS.INCRBY cms:views product:1001 1 product:1002 1
CMS.QUERY cms:views product:1001
```

Use for approximate frequency counting at massive scale.

### Top-K

```bash
TOPK.RESERVE top:searches 100
TOPK.ADD top:searches "redis" "kafka" "redis"
TOPK.LIST top:searches
```

Use for approximate most frequent items.

### t-digest

```bash
TDIGEST.CREATE td:latency
TDIGEST.ADD td:latency 12.4 18.9 250.0
TDIGEST.QUANTILE td:latency 0.95 0.99
```

Use for percentile estimation without storing every sample.

---

## 7. Decision Table

| Problem | Redis Feature | Watch Out For |
|---|---|---|
| nested user profile | JSON | document size, partial update semantics |
| product search | Search/query | index memory, write amplification |
| semantic cache | vector search | embedding memory, model versioning |
| service metrics | Time Series | RAM retention cost |
| unique user count | HyperLogLog | approximate cardinality only |
| pre-check membership | Bloom/Cuckoo | false positives |
| estimate hot items | Count-Min/Top-K | approximate results |
| latency percentile | t-digest | approximation and compression tuning |

---

## 8. Failure Modes

| Failure | User Impact | Mitigation |
|---|---|---|
| index memory explosion | OOM, evictions, write latency | index only needed fields; capacity plan |
| stale search index | missing or wrong query results | monitor indexing errors; rebuild plan |
| vector model change | poor search relevance | version embeddings and rebuild gradually |
| probabilistic false positive | unnecessary DB miss or denied item | use as prefilter, not final authority |
| time-series retention too long | memory growth | set retention and rollups |
| query too broad | latency spike | require filters, limits, pagination |

---

## 9. Strong Architecture Answer

> You are designing product search with personalization and semantic recommendations. Would you use Redis?

Strong answer:

```text
I would use Redis if the hot searchable corpus fits in memory and the application needs very low latency. Product documents can be stored as JSON, indexed with Search for text, tag, and numeric filters, and optionally include vectors for semantic similarity. I would index only known query fields, separate tenants with tags or key prefixes, and cap memory with clear retention or rebuild strategy. If the catalog is massive or search ranking is complex, I would use OpenSearch/Elasticsearch or a dedicated vector database as the source search system and use Redis for hot query caching or semantic cache.
```

---

## 10. Revision Notes

- One-line summary: Redis modern data types are powerful when the data fits in memory and the query/index cost is explicit.
- Three keywords: index, memory, approximation.
- One interview trap: treating Redis Search/vector as free because Redis is fast.
- One memory trick: every query feature becomes an index, and every index becomes RAM.

---

## 11. Official Source Notes

- Redis JSON docs: <https://redis.io/docs/latest/develop/data-types/json/>
- Redis search and query docs: <https://redis.io/docs/latest/develop/ai/search-and-query/>
- Redis vector search docs: <https://redis.io/docs/latest/develop/ai/search-and-query/vectors/>
- Redis Vector Sets docs: <https://redis.io/docs/latest/develop/data-types/vector-sets/>
- Redis Time Series docs: <https://redis.io/docs/latest/develop/data-types/timeseries/>
- Redis probabilistic docs: <https://redis.io/docs/latest/develop/data-types/probabilistic/>
