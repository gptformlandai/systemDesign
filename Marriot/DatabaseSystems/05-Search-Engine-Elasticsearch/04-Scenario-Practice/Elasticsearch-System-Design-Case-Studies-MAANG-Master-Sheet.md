# Elasticsearch System Design Case Studies - MAANG Master Sheet

> Track File #19 of 27 - Group 04: Scenario Practice
> For: backend/search/system design interviews | Level: senior / MAANG | Mode: case studies, index design, tradeoffs

This sheet builds:
- 12 Elasticsearch system design cases
- Index, mapping, relevance, sync, and operations thinking under interview pressure
- Failure modes and alternatives for each case

---

## Case Study Template

Use this shape for every design:

```text
requirements -> document model -> mapping/analyzer -> query/relevance -> sync/freshness -> shard/ILM -> failure modes -> alternatives
```

---

## 1. Product Search

Index: `products-vN` behind `products-read` alias.

Notes:

- title/description text fields
- category/brand/status keyword filters
- price/rating numeric fields
- facets and synonyms
- freshness SLO for price/inventory

---

## 2. Autocomplete

Use:

- edge n-grams or search-as-you-type fields
- popularity boosting
- typo tolerance with guardrails
- prefix limits

Risk: autocomplete can be expensive and noisy if every field uses n-grams.

---

## 3. Log Search

Use data streams with `@timestamp`, ILM, controlled mappings, and dashboard aggregations.

Risk: mapping explosion and retention cost.

---

## 4. Security Event Search

Use structured keyword fields, IP/geo fields, date histograms, and role-based access.

Risk: PII and sensitive event leakage.

---

## 5. Geospatial Search

Use `geo_point` fields and distance queries/sorts.

Risk: expensive sorting/filtering at broad radius and poor caching.

---

## 6. Document Search

Use body chunks, title boosts, highlighting, ACL filters, and source metadata.

Risk: returning unauthorized documents if ACL filters are applied after retrieval.

---

## 7. RAG Hybrid Retrieval

Use lexical fields, dense vectors, metadata filters, tenant/ACL fields, and reranking.

Risk: stale embeddings and ACL leaks.

---

## 8. Multi-Tenant SaaS Search

Options:

- shared index with `tenant_id` filter
- custom routing by tenant
- separate index for large tenants

Risk: noisy tenant, hot shards, filter leaks.

---

## 9. Support Ticket Search

Use title/body text, tags/status keywords, customer/account filters, highlighting.

Risk: exact IDs and natural language need different query paths.

---

## 10. Catalog Admin Search

Use exact filters and audit-friendly sort fields.

Risk: admin queries become arbitrary DSL without guardrails.

---

## 11. Metrics/Event Analytics

Use date histograms and terms aggregations over time-based indices.

Risk: Elasticsearch is not always the best OLAP store for heavy analytics.

---

## 12. People/Profile Search

Use exact fields, synonyms, autocomplete, privacy filters, and ranking.

Risk: privacy and sensitive attribute leakage.

---

## Strong Case Study Answer

```text
I would use Elasticsearch only for the search/read side, not as the source of truth. For product search, I would index denormalized product documents with explicit mappings and analyzers, use bool queries with filters and relevance scoring, generate facets through aggregations, sync from the product database with deterministic IDs and aliases, and monitor relevance, freshness, p99 latency, failed bulk indexing, shard pressure, and zero-result rate.
```

---

## Revision Notes

- One-line summary: Elasticsearch design cases are won by naming index design, relevance, sync, and operations precisely.
- Three keywords: mapping, relevance, freshness.
- One interview trap: designing search without source-of-truth sync.
- Memory trick: every search design has a quality story and a failure story.