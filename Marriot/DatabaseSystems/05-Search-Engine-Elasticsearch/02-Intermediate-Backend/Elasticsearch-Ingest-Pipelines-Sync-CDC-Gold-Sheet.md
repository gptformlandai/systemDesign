# Elasticsearch Ingest Pipelines, Sync, and CDC - Gold Sheet

> Track File #8 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate to senior | Mode: data ingestion, source-of-truth sync, aliases, reindexing

This sheet builds:
- Bulk indexing and ingest pipelines
- Source-of-truth sync patterns
- Reindexing, aliases, and freshness thinking

---

## 1. Elasticsearch Needs A Sync Story

Most production Elasticsearch systems mirror data from a source of truth.

Common sources:

- PostgreSQL/MySQL
- MongoDB
- Kafka events
- CDC pipelines
- application outbox
- log shippers/agents
- object/document processing pipelines

Interview rule:

```text
If you propose Elasticsearch, immediately explain the source of truth and sync pipeline.
```

---

## 2. Bulk Indexing

Use `_bulk` for throughput.

```http
POST _bulk
{ "index": { "_index": "products-v1", "_id": "p1" } }
{ "title": "Mechanical keyboard", "category": "electronics", "price": 129.99 }
{ "index": { "_index": "products-v1", "_id": "p2" } }
{ "title": "Wireless mouse", "category": "electronics", "price": 49.99 }
```

Best practices:

- batch reasonably
- retry failed items, not blindly the whole stream forever
- use deterministic IDs for idempotency
- monitor rejected writes and bulk latency
- tune refresh interval during large backfills

---

## 3. Ingest Pipelines

Ingest pipelines transform documents before indexing.

Example:

```http
PUT _ingest/pipeline/product-normalize
{
  "processors": [
    { "lowercase": { "field": "brand", "ignore_missing": true } },
    { "set": { "field": "indexed_at", "value": "{{_ingest.timestamp}}" } }
  ]
}
```

Use for:

- lightweight normalization
- field extraction
- timestamp enrichment
- simple geo/IP/user-agent enrichment

Avoid heavy business logic in ingest pipelines if it belongs in upstream services.

---

## 4. Aliases And Reindexing

Use aliases to decouple application reads/writes from physical index versions.

```text
products-read -> products-v2
products-write -> products-v2
```

Reindex rollout:

1. Create `products-v2` with new mapping.
2. Backfill from source or `_reindex`.
3. Dual-write if needed.
4. Validate counts/search quality.
5. Atomically switch alias.
6. Keep rollback window.

---

## 5. Freshness SLO

Freshness must be explicit.

Examples:

- product price updates searchable within 5 seconds
- inventory changes searchable within 2 seconds or served from source of truth
- log events searchable within 30 seconds
- RAG documents searchable after ingestion job completes and validates ACLs

---

## 6. Strong Answer

Question:

> How do you keep Elasticsearch in sync with the database?

Strong answer:

```text
I would keep the database as the source of truth and sync changes to Elasticsearch through an outbox, CDC, Kafka, or controlled application events. I would use deterministic document IDs and bulk indexing for idempotency and throughput. For mapping changes I would create a new index version, backfill, validate, and switch an alias atomically. I would also define a freshness SLO and monitor lag, failed bulk items, and stale-search symptoms.
```

---

## 7. Revision Notes

- One-line summary: Elasticsearch search quality depends on a reliable sync and reindexing story.
- Three keywords: bulk, alias, freshness.
- One interview trap: ignoring source-of-truth consistency.
- Memory trick: no sync story, no production search story.