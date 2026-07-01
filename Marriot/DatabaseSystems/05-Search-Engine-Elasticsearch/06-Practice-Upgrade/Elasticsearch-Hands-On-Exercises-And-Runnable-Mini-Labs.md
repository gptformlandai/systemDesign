# Elasticsearch Hands-On Exercises and Runnable Mini Labs

> Track File #24 of 27 - Group 06: Practice Upgrade
> For: backend/search/system design interviews | Level: beginner to pro | Mode: local labs, REST, mappings, search, debugging

Use these exercises with the `elasticsearch-mastery-lab` folder.

---

## Lab 1: First Index And Search

Goal: create an index, index documents, and run basic search.

Tasks:

1. Start Elasticsearch with Docker Compose.
2. Create `products-v1` with explicit mappings.
3. Bulk index sample products.
4. Run match and filter queries.
5. Explain `text` vs `keyword` fields.

---

## Lab 2: Analyzer Inspection

Goal: understand tokenization.

Tasks:

- run `_analyze` with standard analyzer
- test custom autocomplete analyzer
- compare tokens for `Water-resistant running shoes`

Explain:

```text
How do analyzer choices affect search recall and precision?
```

---

## Lab 3: Facets And Aggregations

Goal: build e-commerce facets.

Tasks:

- terms aggregation by brand/category
- range aggregation by price
- date histogram for logs
- explain high-cardinality risk

---

## Lab 4: Relevance Tuning

Goal: improve product search ranking.

Tasks:

- compare `match`, `multi_match`, and boosts
- add filters without changing score
- test fuzziness and synonyms conceptually
- record expected top result for five queries

---

## Lab 5: Operations Incident Drill

Scenario:

```text
Product search p99 jumps from 120 ms to 1.8 seconds after a release.
```

Answer:

- inspect query DSL and index alias
- check deep pagination, wildcard, aggregation, or sort changes
- check slow logs/profile
- check heap, search rejections, hot shards, and disk watermarks
- roll back query or route to safer template

---

## Lab 6: Zero-Downtime Reindexing

Goal: migrate from `products-v1` to `products-v2` with aliases.

Tasks:

- run `SCRIPTS/07-alias-reindex-migration.sh`
- inspect `products-read` and `products-write` aliases
- explain rollback strategy
- explain validation before deleting the old index

---

## Lab 7: Authorized RAG Retrieval

Goal: enforce tenant and ACL filters before retrieval.

Tasks:

- run `SCRIPTS/08-rag-acl-search.sh`
- compare authorized and unauthorized result sets
- explain why post-retrieval filtering can leak data
- define tenant/ACL leak tests

---

## Lab 8: Autocomplete And Geospatial Search

Goal: practice typeahead and place-search modeling.

Tasks:

- run product autocomplete queries
- run `SCRIPTS/09-geo-place-search.sh`
- explain `geo_point`, distance filters, and radius caps
- compare edge n-grams, search-as-you-type, and completion-style suggestions

---

## Completion Gate

You finish these labs only when you can explain:

- why each index exists
- what each mapping does
- what each query is trying to rank/filter
- what can go wrong at scale
- what metric or runbook catches that failure