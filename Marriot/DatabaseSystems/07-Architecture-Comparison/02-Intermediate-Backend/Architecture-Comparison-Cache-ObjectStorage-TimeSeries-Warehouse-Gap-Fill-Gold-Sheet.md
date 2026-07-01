# Architecture Comparison Cache, Object Storage, Time-Series, Warehouse, and Lakehouse Gap Fill - Gold Sheet

> Gap Fill Appendix - Group 02: Intermediate Backend
> For: system design interviews | Level: intermediate to senior | Mode: non-primary datastore tradeoffs

## 1. Why This Sheet Exists

SQL, MongoDB, Cassandra, Elasticsearch, Neo4j, and VectorDB often get the most interview attention, but production systems also depend heavily on caches, object storage, time-series stores, warehouses, and lakehouses.

These systems are usually not generic OLTP sources of truth. They are specialized infrastructure choices with very sharp boundaries.

```text
cache/object/time-series/warehouse choice = latency + retention + query shape + freshness + cost + recovery
```

---

## 2. Redis / Cache vs Primary Database

| Use Cache When | Avoid Cache As Primary When |
|---|---|
| hot reads need sub-millisecond or low-millisecond latency | money, ledger, inventory, or permission correctness is canonical |
| repeated reads overload the source database | stale reads are unacceptable |
| sessions, rate limits, counters, locks, or ephemeral state fit | data must survive cache eviction without durable backing |
| cache miss can rebuild from source | no invalidation, TTL, or versioning plan exists |

Production checks:

- cache-aside, write-through, or write-behind pattern is explicit
- TTL and invalidation strategy are clear
- hot keys are monitored
- stale-read budget is known
- source database can absorb cache-miss storms

Strong answer:

```text
I would use Redis to reduce latency and protect the source database for hot reads, counters, sessions, and ephemeral workflows. I would not make Redis the canonical source for money, permissions, or inventory unless persistence, replication, recovery, and correctness semantics are explicitly designed.
```

---

## 3. Object Storage vs Database

| Use Object Storage When | Avoid Object Storage When |
|---|---|
| storing images, videos, PDFs, backups, exports, logs, or data lake files | low-latency record updates are required |
| durability and cheap retention matter | ad hoc row-level queries are needed directly over objects |
| files are accessed by key or through CDN | transactional updates across many small records are required |
| lifecycle policies can move data across hot/cold tiers | frequent small overwrites dominate |

Production checks:

- object key naming and partitioning avoid hot prefixes
- metadata lives in a queryable database
- lifecycle/retention policy exists
- encryption and access control are configured
- egress and request cost are modeled

Strong answer:

```text
I would use object storage for durable blobs, backups, exports, and data lake files, while keeping metadata and transactional state in a database. Object storage is excellent for durability and cost, but it is not a replacement for indexed OLTP records.
```

---

## 4. Time-Series Store vs Cassandra vs Warehouse

| Workload | Good Fit | Why |
|---|---|---|
| recent metrics queries and alerts | time-series store | time-window queries, rollups, retention, alerting |
| huge append-only events by partition key | Cassandra/wide-column | high write throughput and predictable key access |
| long historical analytics | warehouse/lakehouse | columnar scans and aggregations |
| text search over logs | Elasticsearch/OpenSearch | full-text and structured field search |

Production checks:

- retention tiers are explicit
- label/cardinality limits are enforced
- rollups and downsampling are defined
- current time bucket or hot partition risk is controlled
- alert queries have SLOs

Strong answer:

```text
For metrics, I would prefer a time-series store because it is optimized for time-window queries, retention, rollups, and alerting. Cassandra can fit massive append-only events when query patterns are key-based. Warehouses fit historical analytics, and search engines fit log search.
```

---

## 5. Warehouse / Lakehouse vs OLTP Database

| Use Warehouse/Lakehouse When | Avoid It When |
|---|---|
| dashboards, analytics, BI, ML features, audits, historical reporting | low-latency transactional writes are required |
| large scans and joins over historical data dominate | user-facing request path needs single-digit millisecond responses |
| batch/stream ingestion from many sources exists | source-of-truth constraints are required in the serving path |
| raw files need lake storage plus governed table formats | data freshness needs are hard real-time |

Production checks:

- freshness SLA is labeled on dashboards
- schema evolution and backfill plan exist
- PII/PHI masking and governance are enforced
- cost controls are in place for scanned bytes and compute
- lineage links reports back to source systems

Strong answer:

```text
I would use a warehouse or lakehouse for analytical scans, reports, ML features, and historical analysis. I would not run heavy analytics on the OLTP database because it competes with application transactions and usually has the wrong storage/query model.
```

---

## 6. Quick Decision Table

| Need | Choose First | Key Risk |
|---|---|---|
| hot low-latency read | Redis/cache | stale data, hot keys, miss storm |
| durable blob/file | object storage | metadata/query limitations, egress cost |
| metrics and alerting | time-series store | cardinality and retention cost |
| log text search | Elasticsearch/OpenSearch | shard cost and index freshness |
| historical analytics | warehouse/lakehouse | cost, freshness, governance |
| canonical transaction | SQL/OLTP database | scale and partitioning complexity |

---

## 7. Interview Summary

```text
I would treat cache, object storage, time-series stores, and warehouses as specialized systems rather than generic database replacements. Cache optimizes hot reads but needs stale-read controls. Object storage gives durable cheap blobs but metadata belongs in a database. Time-series systems fit metrics and alert windows. Warehouses and lakehouses fit analytics and history, not OLTP transactions.
```

---

## 8. Revision Notes

- One-line summary: These systems are powerful when their boundary is explicit.
- Three keywords: latency, retention, freshness.
- One trap: using cache, object storage, or warehouse as if it were a transactional OLTP database.