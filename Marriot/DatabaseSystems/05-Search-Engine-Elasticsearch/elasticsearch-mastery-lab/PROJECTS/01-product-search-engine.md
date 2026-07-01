# Project 01: Product Search Engine

Goal: build an Elasticsearch-backed e-commerce product search read model.

---

## Requirements

- Full-text product search.
- Filters for tenant, category, brand, price, and stock.
- Facets for brand/category/price.
- Relevance boosts for title, brand, rating, and popularity.
- Freshness SLO for price/inventory.

---

## Index

```text
products-v1 behind products-read/products-write aliases
```

---

## Interview Talking Points

- source of truth remains product database
- aliases support reindexing
- title is `text`; brand/category are `keyword`
- facets are aggregations
- freshness and relevance must be measured