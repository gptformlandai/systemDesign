# Project 06: Product Discovery With Modern Redis

## Goal

Design and prototype a low-latency product discovery backend using modern Redis capabilities.

This is a portfolio-level project because it combines:

- JSON documents
- Search/query indexes
- vector similarity
- hot query caching
- client-side cache invalidation strategy
- production memory and fallback planning

---

## Requirements

Build an API or command-line prototype that supports:

1. Add/update product document.
2. Search products by text query.
3. Filter by brand, category, and price range.
4. Return top-N similar products using embeddings or simulated vectors.
5. Cache hot query results with TTL.
6. Invalidate affected cache keys when a product changes.
7. Explain memory/index tradeoffs.

---

## Data Model

Product key:

```text
product:{productId}
```

Example JSON:

```json
{
  "id": "1001",
  "name": "Trail Shoe",
  "brand": "Acme",
  "category": "shoes",
  "price": 89.99,
  "description": "Lightweight running shoe",
  "embedding_version": "v1"
}
```

Search cache key:

```text
cache:search:{hash-of-query-and-filters}
```

Invalidation set:

```text
product:{productId}:cache_keys
```

---

## Suggested Redis Features

| Feature | Use |
|---|---|
| JSON | store product documents |
| Search/query | text, tag, numeric filters |
| vector search | similar products |
| string with TTL | cache result payload |
| set | track cache keys affected by product |
| Function or Lua | atomic cache invalidation helper |

---

## Implementation Milestones

### Milestone 1: Product Storage

- create sample products
- read product by ID
- update price and description
- verify partial update works

### Milestone 2: Search Index

- create index over product fields
- support text search
- support tag filters
- support numeric price range
- paginate results

### Milestone 3: Similar Products

- generate fake vectors or use real embeddings
- store vector field
- query K nearest products
- filter by category or tenant

### Milestone 4: Hot Query Cache

- cache search response for 30-120 seconds
- add TTL jitter
- store reverse mapping from product ID to cache keys
- invalidate related cache keys on product update

### Milestone 5: Production Memo

Write a design memo that includes:

- memory estimate for 1 million products
- index fields and why each exists
- vector memory estimate
- stale-cache tolerance
- fallback if Redis is unavailable
- when you would move search/vector to a dedicated system

---

## Acceptance Criteria

Your final project should demonstrate:

- exact filters and text search
- at least one vector/similarity query or a documented stub
- cache hit/miss behavior
- invalidation on product update
- measured or estimated memory cost
- clear explanation of why Redis is or is not the right long-term source of truth

---

## Interview Explanation

Strong answer:

```text
I store hot product documents in Redis JSON and index only fields used by known access patterns: category and brand as TAG, price as NUMERIC, name/description as TEXT, and embedding as VECTOR if semantic search is required. Query results are cached with TTL and invalidated on product updates. I capacity-plan the index and vector memory because Redis RAM is expensive. If the catalog or ranking complexity grows beyond memory-friendly limits, I move search/vector to a dedicated search platform and keep Redis for hot query/result caching.
```
