# Elasticsearch Query DSL, Filters, Bool, and Pagination - Gold Sheet

> Track File #6 of 27 - Group 02: Intermediate Backend
> For: backend/search/system design interviews | Level: intermediate | Mode: query DSL, filters, pagination, correctness

This sheet builds:
- Query vs filter context
- Bool query patterns
- Pagination strategy and deep pagination risks

---

## 1. Query vs Filter Context

| Context | Purpose | Scoring? |
|---|---|---|
| query | relevance search | yes |
| filter | exact constraints | no |

Example:

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "wireless headphones" } }
      ],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "term": { "status": "active" } },
        { "range": { "price": { "lte": 200 } } }
      ]
    }
  }
}
```

Rule:

```text
Use query context for relevance and filter context for mandatory exact constraints.
```

---

## 2. Match vs Term

| Query | Use For |
|---|---|
| `match` | analyzed text search |
| `match_phrase` | ordered phrase search |
| `term` | exact keyword/numeric/boolean match |
| `terms` | exact match against a set |
| `range` | dates and numbers |

Common mistake:

```text
Using `term` against analyzed `text` fields and expecting natural-language search.
```

---

## 3. Bool Query

| Clause | Meaning |
|---|---|
| `must` | required and contributes to score |
| `should` | optional boost or minimum-should-match requirement |
| `filter` | required, no score |
| `must_not` | excluded, no score |

Use `minimum_should_match` carefully for multi-intent queries.

---

## 4. Pagination

Basic pagination:

```json
{
  "from": 0,
  "size": 20
}
```

Deep pagination problem:

```text
from + size forces Elasticsearch to collect and sort many skipped results across shards.
```

Better for deep/scroll-like user flows:

- `search_after` with stable sort
- point-in-time for consistent pagination
- avoid arbitrary page 10,000 UX

Example:

```json
{
  "size": 20,
  "sort": [
    { "created_at": "desc" },
    { "_id": "asc" }
  ],
  "search_after": ["2026-07-01T10:00:00Z", "doc-123"]
}
```

---

## 5. Strong Answer

Question:

> Why is deep pagination risky in Elasticsearch?

Strong answer:

```text
With `from` and `size`, Elasticsearch still has to collect and sort skipped hits across shards before returning the requested page. Very deep pages create high memory and coordination cost. For deep pagination I would use `search_after` with a stable sort, often with point-in-time for consistency, and I would avoid product experiences that require arbitrary jumps into huge result sets.
```

---

## 6. Revision Notes

- One-line summary: Query clauses score; filters constrain; deep pagination needs `search_after`.
- Three keywords: bool, filter, search_after.
- One interview trap: using `from` for unbounded deep pagination.
- Memory trick: users see page 2; shards still worked through everything skipped.