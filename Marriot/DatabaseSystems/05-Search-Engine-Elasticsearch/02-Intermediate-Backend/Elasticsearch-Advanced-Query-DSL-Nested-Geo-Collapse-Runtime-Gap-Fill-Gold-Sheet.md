# Elasticsearch Advanced Query DSL, Nested, Geo, Collapse, and Runtime Fields - Gap Fill Gold Sheet

> Gap-Fill Sheet - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate to senior | Mode: advanced query features, modeling traps, debugging hooks

This sheet fills query and data-modeling features that often appear in senior follow-ups:

- phrase and proximity search
- prefix, wildcard, and regex guardrails
- nested queries and parent-child tradeoffs
- field collapsing and result grouping
- geo queries and sorts
- runtime fields and script risks
- `explain` and profile-driven debugging

---

## 1. Phrase And Proximity Search

Use phrase search when order matters.

```json
{
  "query": {
    "match_phrase": {
      "title": {
        "query": "wireless running headphones",
        "slop": 1
      }
    }
  }
}
```

Use cases:

- exact product names
- document titles
- legal/policy text
- support ticket phrases

Trap:

```text
Phrase search is more precise but can reduce recall. Use it deliberately, often as a boosted clause rather than the only clause.
```

---

## 2. Prefix, Wildcard, And Regex Guardrails

| Query Type | Use | Risk |
|---|---|---|
| prefix | controlled prefix search on keyword-like values | broad prefixes can still be costly |
| wildcard | occasional admin/search tools | expensive on hot user path |
| regex | rare diagnostics or controlled fields | can be very expensive |
| n-gram field | autocomplete/search-as-you-type | larger index size |

Production rule:

```text
Do not make wildcard or regex the default product-search path. Model autocomplete with dedicated fields.
```

---

## 3. Nested Query

Use `nested` when arrays of objects must preserve per-object matching.

Example problem:

```json
{
  "variants": [
    { "color": "red", "size": "M" },
    { "color": "blue", "size": "XL" }
  ]
}
```

Question:

```text
Find products where the same variant is red and size M.
```

Plain object mapping can false-match across objects. `nested` prevents that at extra query/index cost.

Strong answer:

```text
I use nested only when per-object correctness matters. If I only need simple filtering or display, plain object or denormalization may be cheaper.
```

---

## 4. Parent-Child Join

Parent-child can model related entities without duplicating everything, but it is usually a last resort in Elasticsearch.

Use carefully for:

- very large child collections
- update-heavy children where full denormalization is too expensive

Prefer first:

- denormalized documents
- separate search index
- app-side composition after search
- database query for transactional joins

Interview phrase:

```text
Elasticsearch can support limited join-like behavior, but search systems usually prefer denormalized documents and query-time filters over relational joins.
```

---

## 5. Field Collapse

Field collapse groups search hits by a keyword field.

Use cases:

- one result per product family
- one result per document
- one result per user/entity

Example:

```json
{
  "query": { "match": { "description": "running" } },
  "collapse": { "field": "product_id" }
}
```

Trap:

```text
Collapse is not a replacement for aggregations, and pagination/sorting rules need careful testing.
```

---

## 6. Geo Query And Sort

Use `geo_point` for location search.

Common patterns:

- distance filter
- bounding box
- distance sort
- category + location filter

Example shape:

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "category": "restaurant" } },
        { "geo_distance": { "distance": "5km", "location": { "lat": 40.73, "lon": -73.93 } } }
      ]
    }
  },
  "sort": [
    { "_geo_distance": { "location": { "lat": 40.73, "lon": -73.93 }, "order": "asc", "unit": "km" } }
  ]
}
```

Trap:

```text
Geo search still needs product constraints, radius guardrails, and privacy thinking.
```

---

## 7. Runtime Fields And Scripts

Runtime fields compute values at query time.

Use for:

- migration bridge
- low-volume exploratory queries
- temporary derived fields

Avoid for:

- hot-path ranking
- high-QPS facets
- expensive per-document calculations

Strong answer:

```text
If a derived field is needed frequently, I would usually index it at ingest time instead of computing it at search time.
```

---

## 8. Explain And Profile

Use:

- `_explain` to understand why one document scored a certain way
- profile API to understand query execution cost
- slow logs to find production outliers

Debugging flow:

```text
bad ranking or slow query -> exact DSL -> explain/profile sample -> mapping/analyzer check -> query or index redesign
```

---

## 9. Strong Interview Answer

Question:

> How do you handle advanced search requirements without making Elasticsearch slow or incorrect?

Strong answer:

```text
I first map the requirement to the right field model: phrase search for ordered text, dedicated n-gram or search-as-you-type fields for autocomplete, nested for arrays where same-object matching matters, geo_point for location, and collapse only for grouping hits. I avoid wildcard/regex/scripts on hot paths and validate expensive queries with profile, slow logs, and load tests. If the model starts looking relational, I either denormalize, split the search index, or keep joins in the source database.
```

---

## 10. Revision Notes

- One-line summary: Advanced DSL features are powerful, but each one has a modeling and cost boundary.
- Three keywords: nested, profile, guardrails.
- One interview trap: using wildcard, scripts, or parent-child as the first design choice.
- Memory trick: advanced query features are scalpels, not the whole toolbox.