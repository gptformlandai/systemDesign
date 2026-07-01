# Elasticsearch Indexing, CRUD, and Search Basics - Gold Sheet

> Track File #4 of 27 - Group 01: Starter Path
> For: backend/search/system design interviews | Level: beginner to intermediate | Mode: REST syntax, CRUD, refresh, simple search

This sheet builds:
- Index creation and document APIs
- CRUD basics and near-real-time search
- First query DSL examples

---

## 1. Create An Index

```http
PUT products
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "category": { "type": "keyword" },
      "price": { "type": "double" },
      "in_stock": { "type": "boolean" },
      "created_at": { "type": "date" }
    }
  }
}
```

---

## 2. Index Documents

```http
PUT products/_doc/p1
{
  "name": "Mechanical keyboard",
  "category": "electronics",
  "price": 129.99,
  "in_stock": true,
  "created_at": "2026-07-01T10:00:00Z"
}
```

Use deterministic IDs when Elasticsearch mirrors a source-of-truth entity. This makes replays and retries idempotent.

---

## 3. Get, Update, Delete

```http
GET products/_doc/p1

POST products/_update/p1
{
  "doc": {
    "price": 119.99
  }
}

DELETE products/_doc/p1
```

Professional nuance:

```text
Updates are internally read-modify-write and create new Lucene segment versions. Heavy update workloads can increase merge and delete-marker pressure.
```

---

## 4. Refresh

Elasticsearch is near-real-time. Indexed documents become searchable after refresh.

Manual lab refresh:

```http
POST products/_refresh
```

Production caution:

```text
Do not force refresh after every write on hot paths. It hurts indexing throughput and segment behavior.
```

---

## 5. Simple Search

```http
GET products/_search
{
  "query": {
    "match": {
      "name": "mechanical keyboard"
    }
  }
}
```

Filter plus full-text query:

```http
GET products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "keyboard" } }
      ],
      "filter": [
        { "term": { "category": "electronics" } },
        { "range": { "price": { "lte": 150 } } }
      ]
    }
  }
}
```

---

## 6. Strong Answer

Question:

> What does near-real-time mean in Elasticsearch?

Strong answer:

```text
After a document is indexed, it is not necessarily searchable immediately. Elasticsearch makes new segment data visible on refresh, commonly around a one-second interval by default. For search systems this is usually acceptable, but if product requirements need instant read-after-write search, I would discuss refresh tradeoffs, source-of-truth reads, or a different design for that path.
```

---

## 7. Revision Notes

- One-line summary: Elasticsearch CRUD is easy; production correctness depends on refresh, sync, mapping, and workload shape.
- Three keywords: `_doc`, `_refresh`, `_search`.
- One interview trap: assuming indexed means instantly searchable.
- Memory trick: write to index, refresh to search.