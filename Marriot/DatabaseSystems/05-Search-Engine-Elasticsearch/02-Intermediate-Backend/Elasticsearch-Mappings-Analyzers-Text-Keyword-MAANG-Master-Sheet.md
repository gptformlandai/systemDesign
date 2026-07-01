# Elasticsearch Mappings, Analyzers, Text, and Keyword - MAANG Master Sheet

> Track File #5 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate to senior | Mode: mappings, analyzers, field design

This sheet builds:
- Field type selection
- Analyzer and tokenization mental model
- Dynamic mapping, multi-fields, nested/flattened choices

---

## 1. Core Principle

Mappings are the schema of search behavior.

```text
mapping decides how a field is indexed, searched, sorted, aggregated, and stored
```

A wrong mapping can force reindexing, break relevance, or make aggregations expensive/impossible.

---

## 2. `text` vs `keyword`

| Type | Use For | Example |
|---|---|---|
| `text` | full-text search with analysis | product title, article body |
| `keyword` | exact match, filters, sorting, aggregations | status, SKU, tenant ID, category |

Common pattern:

```json
"name": {
  "type": "text",
  "fields": {
    "keyword": { "type": "keyword" }
  }
}
```

This lets `name` support full-text search and `name.keyword` support exact sorting/aggregation.

---

## 3. Analyzer Mental Model

An analyzer transforms text into tokens.

```text
character filters -> tokenizer -> token filters -> indexed terms
```

Example API:

```http
POST _analyze
{
  "analyzer": "standard",
  "text": "Water-resistant running shoes"
}
```

Analyzer decisions affect:

- stemming
- lowercase behavior
- stop words
- synonyms
- autocomplete
- language support
- exact phrase behavior

---

## 4. Dynamic Mapping Risk

Dynamic mapping is convenient but risky in production.

Risks:

- wrong field types from first document
- mapping explosion from arbitrary JSON keys
- inconsistent search behavior
- high cluster-state pressure

Safer approach:

```text
Use explicit mappings for important indexes and dynamic templates for controlled patterns.
```

---

## 5. Object, Nested, Flattened

| Type | Use When | Risk |
|---|---|---|
| object | simple nested JSON where cross-field matching does not matter | false matches across array objects |
| nested | arrays of objects that must preserve per-object matching | heavier query/index cost |
| flattened | arbitrary key-value metadata | less precise typed querying |

Example nested use:

```json
"variants": {
  "type": "nested",
  "properties": {
    "color": { "type": "keyword" },
    "size": { "type": "keyword" },
    "in_stock": { "type": "boolean" }
  }
}
```

---

## 6. Strong Answer

Question:

> Why do mappings matter so much in Elasticsearch?

Strong answer:

```text
Mappings define how fields are indexed and queried. A product name should usually be `text` for full-text search and have a `keyword` subfield for exact sort or aggregation. IDs, status, categories, and tenant IDs should be `keyword`. If dynamic mapping guesses wrong or arbitrary fields explode, we may need to reindex and can hurt cluster state, relevance, and performance.
```

---

## 7. Revision Notes

- One-line summary: Mapping is search schema; analyzer is text-to-token behavior.
- Three keywords: text, keyword, analyzer.
- One interview trap: using `text` fields for exact filters and aggregations.
- Memory trick: text searches meaning; keyword matches identity.