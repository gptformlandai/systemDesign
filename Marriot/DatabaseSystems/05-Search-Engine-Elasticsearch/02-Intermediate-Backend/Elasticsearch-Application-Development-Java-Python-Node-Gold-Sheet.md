# Elasticsearch Application Development with Java, Python, and Node - Gold Sheet

> Track File #10 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate | Mode: clients, API boundaries, retries, timeouts, aliases

This sheet builds:
- Practical client usage principles
- Search repository/API design
- Production-safe retries, timeouts, pagination, and alias use

---

## 1. Application Rules

- Keep Elasticsearch behind a search service/API boundary.
- Reuse clients and connection pools.
- Set timeouts intentionally.
- Use index aliases instead of hardcoded physical index names.
- Use deterministic document IDs for source-of-truth entities.
- Use bulk APIs for ingestion.
- Treat retries carefully; retry failed bulk items with backoff.
- Do not expose raw query DSL to untrusted users without guardrails.

---

## 2. Java Client Shape

```java
SearchResponse<ProductDocument> response = client.search(s -> s
    .index("products-read")
    .query(q -> q.bool(b -> b
        .must(m -> m.match(mm -> mm.field("title").query("keyboard")))
        .filter(f -> f.term(t -> t.field("tenant_id").value("t1")))
    ))
    .size(20),
    ProductDocument.class
);
```

Interview point:

```text
The app should call a stable search API or alias, not know every physical index version.
```

---

## 3. Python Client Shape

```python
from elasticsearch import Elasticsearch

client = Elasticsearch("http://localhost:9200")

response = client.search(
    index="products-read",
    query={
        "bool": {
            "must": [{"match": {"title": "keyboard"}}],
            "filter": [{"term": {"tenant_id": "t1"}}],
        }
    },
    size=20,
)
```

---

## 4. Node Client Shape

```javascript
const response = await client.search({
  index: "products-read",
  query: {
    bool: {
      must: [{ match: { title: "keyboard" } }],
      filter: [{ term: { tenant_id: "t1" } }]
    }
  },
  size: 20
});
```

---

## 5. API Guardrails

Do not let user input become arbitrary Elasticsearch DSL.

Guardrails:

- allowlist searchable fields
- cap `size`
- restrict sort fields
- block expensive regex/wildcard patterns
- enforce tenant/security filters server-side
- use search templates for complex query families
- log query latency and result counts

---

## 6. Strong Answer

Question:

> What are important application integration concerns for Elasticsearch?

Strong answer:

```text
I would hide Elasticsearch behind a search service that owns query templates, aliases, tenant/security filters, timeouts, pagination, and result shaping. I would use deterministic IDs and bulk indexing for ingestion, avoid exposing raw DSL to users, and monitor failed bulk items, search latency, stale index lag, and zero-result rates. Application code should not hardcode physical index versions.
```

---

## 7. Revision Notes

- One-line summary: Application code should own search contracts, not leak raw cluster internals.
- Three keywords: alias, timeout, guardrail.
- One interview trap: exposing arbitrary DSL to end users.
- Memory trick: search API is a product contract; Elasticsearch is the engine behind it.