# Architecture Comparison Ecommerce Marketplace Case Study - MAANG Sheet

> Track File #17 of 30 - Group 04: Scenario Practice
> For: marketplace system design interviews | Level: MAANG | Mode: datastore selection

## 1. Workloads

- product catalog browsing
- full-text product search
- cart and checkout
- orders and payments
- inventory reservation
- recommendations
- analytics/reporting

---

## 2. Store Choices

| Workflow | Strong Choice | Why |
|---|---|---|
| orders/payments | PostgreSQL/MySQL | transactions, constraints, audit |
| product catalog source | MongoDB or SQL | document aggregate or relational catalog needs |
| product search | Elasticsearch/OpenSearch | relevance, filters, facets |
| recommendations | Vector DB plus feature store | similarity and ranking |
| cart/session | Redis plus source DB | low latency, short-lived state |
| images | object storage/CDN | cheap durable blobs |
| analytics | warehouse/lakehouse | OLAP, dashboards |

---

## 3. Production Risks

- overselling inventory
- stale search index
- payment/order inconsistency
- recommendation privacy leaks
- cache serving old price
- catalog image/blob lifecycle issues

---

## 4. Strong Interview Answer

```text
For ecommerce, I would keep orders, payments, and inventory reservations in a transactional relational store. Product catalog can be SQL or document depending on schema and access patterns. Elasticsearch serves derived product search, Redis caches hot catalog/cart reads, object storage serves media, vector DB supports recommendations or semantic search, and a warehouse handles analytics. CDC keeps derived stores fresh, and money/inventory workflows get strong consistency and audit.
```

---

## 5. Revision Notes

- One-line summary: Ecommerce is polyglot, but money and inventory need transactional correctness.
- Three keywords: orders, search, cache.
- One trap: making Elasticsearch the source of truth for prices or inventory.