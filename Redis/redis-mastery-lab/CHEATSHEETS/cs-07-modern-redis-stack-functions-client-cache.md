# Cheatsheet 07: Modern Redis, Functions, Search, And Client-Side Caching

## Compatibility

```bash
INFO server
COMMAND INFO JSON.SET
COMMAND INFO FT.CREATE
COMMAND INFO FUNCTION
COMMAND INFO CLIENT
```

Use this before assuming a command is available in local Redis or a managed provider.

---

## JSON

```bash
JSON.SET user:1001 $ '{"id":1001,"name":"Alice","prefs":{"theme":"dark"}}'
JSON.GET user:1001 $
JSON.GET user:1001 $.prefs.theme
JSON.SET user:1001 $.prefs.theme '"light"'
JSON.NUMINCRBY user:1001 $.login_count 1
JSON.DEL user:1001 $.prefs.theme
```

Use for nested documents and partial updates.

---

## Search And Query

```bash
FT.CREATE idx:products ON JSON PREFIX 1 product: SCHEMA \
  $.name AS name TEXT \
  $.brand AS brand TAG \
  $.price AS price NUMERIC SORTABLE

FT.SEARCH idx:products '@brand:{Acme} @price:[50 100] running' RETURN 2 $.name $.price
FT.INFO idx:products
FT.DROPINDEX idx:products DD
```

Field choice:

| Field Type | Use |
|---|---|
| TEXT | tokenized full-text search |
| TAG | exact filters |
| NUMERIC | range filters |
| GEO | location filters |
| VECTOR | similarity search |

---

## Vector Search

```bash
FT.CREATE idx:docs ON HASH PREFIX 1 doc: SCHEMA \
  title TEXT \
  tenant TAG \
  embedding VECTOR HNSW 6 TYPE FLOAT32 DIM 1536 DISTANCE_METRIC COSINE

FT.SEARCH idx:docs '(@tenant:{acme})=>[KNN 5 @embedding $query_vector AS score]' \
  PARAMS 2 query_vector "<binary-vector>" \
  SORTBY score \
  RETURN 2 title score \
  DIALECT 2
```

Use for semantic cache, recommendations, and similarity search when corpus fits in memory.

---

## Time Series

```bash
TS.CREATE metrics:api:p95 RETENTION 604800000 LABELS service api metric p95
TS.ADD metrics:api:p95 * 83.4
TS.RANGE metrics:api:p95 - + AGGREGATION avg 60000
TS.GET metrics:api:p95
```

Use retention and rollups to prevent unbounded RAM growth.

---

## Probabilistic Structures

```bash
# Bloom filter.
BF.RESERVE seen:email 0.001 1000000
BF.ADD seen:email alice@example.com
BF.EXISTS seen:email bob@example.com

# Cuckoo filter.
CF.RESERVE seen:tokens 1000000
CF.ADD seen:tokens token-abc
CF.DEL seen:tokens token-abc

# Count-Min Sketch.
CMS.INITBYPROB cms:views 0.001 0.99
CMS.INCRBY cms:views product:1001 1
CMS.QUERY cms:views product:1001

# Top-K.
TOPK.RESERVE top:queries 100
TOPK.ADD top:queries redis kafka redis
TOPK.LIST top:queries

# t-digest.
TDIGEST.CREATE td:latency
TDIGEST.ADD td:latency 12 80 250
TDIGEST.QUANTILE td:latency 0.95 0.99
```

---

## Functions

```bash
FUNCTION LOAD "$(cat library.lua)"
FUNCTION LIST
FCALL function_name 1 key arg1 arg2
FCALL_RO read_only_function 1 key arg1
FUNCTION DELETE library_name
```

Rules:

- pass keys explicitly
- keep work bounded
- version function names
- monitor `SLOWLOG` and `INFO commandstats`

---

## Client-Side Caching

```bash
HELLO 3
CLIENT TRACKING ON
CLIENT TRACKING ON BCAST PREFIX user: PREFIX product:
CLIENT TRACKING OFF
```

Near-cache rules:

- local cache must have TTL
- local cache must have max size
- flush local cache on reconnect/failover
- avoid write-hot keys
- verify client library invalidation support

---

## Managed Redis Questions

Ask every provider:

- Which Redis-compatible version?
- Are JSON/Search/Vector/TimeSeries/Functions supported?
- Is Cluster supported?
- How does failover work?
- What metrics are exported?
- How do backups and restores work?
- What are connection and item-size limits?
- How are TLS/auth/ACLs configured?
