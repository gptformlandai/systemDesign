# Elasticsearch E-Commerce Product Search - MAANG Master Sheet

> Track File #17 of 27 - Group 04: Scenario Practice
> For: backend/search/system design interviews | Level: senior / MAANG | Mode: product search, facets, ranking, freshness

This sheet builds:
- Product search architecture
- Facets, synonyms, ranking, autocomplete, and inventory freshness
- Interview-ready e-commerce search answer

---

## 1. Requirements

- search products by user text
- filter by tenant, category, brand, price, inventory, rating
- facet results
- sort by relevance, price, newest, rating
- handle synonyms and typos
- keep price/inventory fresh
- support autocomplete

---

## 2. Document Shape

```json
{
  "product_id": "p1",
  "tenant_id": "t1",
  "title": "Wireless running headphones",
  "brand": "soundmax",
  "category": "electronics",
  "description": "Sweat-resistant Bluetooth headphones",
  "price": 79.99,
  "in_stock": true,
  "rating": 4.5,
  "popularity": 1200,
  "updated_at": "2026-07-01T10:00:00Z"
}
```

---

## 3. Query Pattern

```text
must: title/description/brand match
filter: tenant, status, category, price, inventory
boost: title, brand, exact SKU, popularity, rating
aggs: brand/category/price/rating facets
```

---

## 4. Freshness

Price and inventory can be tricky.

Options:

- index frequently with freshness SLO
- serve final price/inventory from source of truth on product detail page
- split fast-changing fields into separate update path
- use fallback when index is stale

---

## 5. Strong Answer

```text
I would index denormalized product documents with explicit mappings: text fields for title/description, keyword fields for brand/category/tenant, numeric fields for price/rating, and autocomplete fields where needed. Queries use bool with text matching and filter context for facets. Ranking combines lexical relevance, field boosts, popularity, rating, and availability, measured against a golden query set. Product data syncs from the source of truth through bulk indexing/CDC with freshness SLOs for price and inventory.
```

---

## 6. Revision Notes

- One-line summary: E-commerce search is mapping + facets + ranking + freshness.
- Three keywords: facets, synonyms, inventory.
- One interview trap: indexing stale price/inventory without fallback.
- Memory trick: product search is a relevance system with business constraints.