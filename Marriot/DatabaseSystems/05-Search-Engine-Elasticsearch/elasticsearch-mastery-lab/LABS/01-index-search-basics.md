# Lab 01: Index And Search Basics

Goal: create indices, seed documents, and run basic search.

---

## Run

```bash
docker compose up -d
bash SCRIPTS/wait-for-elasticsearch.sh
bash SCRIPTS/run-request.sh SCRIPTS/01-create-indices.sh
bash SCRIPTS/run-request.sh SCRIPTS/02-seed-data.sh
bash SCRIPTS/run-request.sh SCRIPTS/03-search-queries.sh
```

---

## What To Observe

- `products-v1` has explicit mappings.
- `products-read` and `products-write` aliases point to `products-v1`.
- Product search uses full-text query plus exact filters.
- `rag-chunks-v1` includes tenant and ACL metadata.

---

## Explain Out Loud

```text
Why is Elasticsearch not the source of truth for products?
```

Strong answer:

```text
The product database owns transactional truth. Elasticsearch stores a denormalized search projection optimized for relevance, filters, and facets. It must be synced and can be rebuilt.
```