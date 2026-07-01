# Elasticsearch Autocomplete and Geospatial Search - Gap Fill MAANG Sheet

> Gap-Fill Sheet - Group 04: Scenario Practice
> For: backend/search/system design interviews | Level: senior / MAANG | Mode: autocomplete, local search, user-facing latency

This sheet fills two common scenario prompts that deserve standalone practice:

- autocomplete/typeahead search
- geospatial/place search

---

## 1. Autocomplete Requirements

Ask:

- Is this product autocomplete, document title autocomplete, people search, or query suggestions?
- Do we need typo tolerance?
- Do we rank by popularity, recency, personalization, or exact prefix?
- What is the p99 latency target?
- Are there privacy/security filters?

Typical SLO:

```text
autocomplete p99 < 50 ms to 100 ms, with strict limits on payload size and query cost
```

---

## 2. Autocomplete Design Options

| Option | Fit | Tradeoff |
|---|---|---|
| edge n-gram field | flexible prefix matching | larger index, noisy matches if overused |
| search-as-you-type field | common typeahead pattern | still needs relevance tuning |
| completion suggester | fast suggestions | different modeling and feature tradeoffs |
| query log suggestions | popular query suggestions | needs analytics pipeline and moderation |

Production rule:

```text
Autocomplete should have dedicated fields and limits. Do not run broad wildcard queries for every keystroke.
```

---

## 3. Autocomplete Query Shape

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "title.autocomplete": "mech key" } }],
      "filter": [{ "term": { "tenant_id": "t1" } }, { "term": { "in_stock": true } }]
    }
  },
  "size": 5
}
```

Add ranking signals carefully:

- exact title prefix
- popularity
- inventory availability
- brand/category boosts
- recent user behavior if allowed

---

## 4. Geospatial Requirements

Ask:

- Are users searching nearby stores, restaurants, drivers, inventory, or events?
- Is location exact, approximate, or privacy-sensitive?
- Do we sort by distance, relevance, rating, availability, or ETA?
- What maximum radius is allowed?
- How does the system behave when there are too few nearby results?

---

## 5. Geospatial Mapping

```json
{
  "mappings": {
    "properties": {
      "place_id": { "type": "keyword" },
      "tenant_id": { "type": "keyword" },
      "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "category": { "type": "keyword" },
      "location": { "type": "geo_point" },
      "rating": { "type": "double" },
      "open_now": { "type": "boolean" }
    }
  }
}
```

---

## 6. Geospatial Query Shape

```json
{
  "query": {
    "bool": {
      "must": [{ "match": { "name": "coffee" } }],
      "filter": [
        { "term": { "tenant_id": "t1" } },
        { "term": { "open_now": true } },
        { "geo_distance": { "distance": "3km", "location": { "lat": 40.73, "lon": -73.93 } } }
      ]
    }
  },
  "sort": [
    { "_geo_distance": { "location": { "lat": 40.73, "lon": -73.93 }, "order": "asc", "unit": "km" } }
  ]
}
```

---

## 7. Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| autocomplete slow | wildcard/regex on every keystroke | dedicated prefix fields, debounce, cache |
| noisy suggestions | n-gram too broad | tune min/max gram, boosts, popularity filters |
| wrong nearby results | bad geo mapping or stale location | validate `geo_point`, freshness SLO |
| privacy leak | user/location/tenant filter missing | enforce filters before ranking |
| expensive broad radius | radius too large | radius caps, fallback tiers, pagination limits |

---

## 8. Strong Interview Answer

```text
For autocomplete, I would create a dedicated prefix/search-as-you-type field and query it with strict size, tenant/security filters, and popularity or exact-prefix boosts. I would avoid wildcard queries per keystroke. For geospatial search, I would map location as geo_point, filter by bounded radius and business constraints, and sort or boost by distance only when the UX requires it. Both paths need low p99 latency, query limits, stale-data handling, and privacy/tenant filters.
```

---

## 9. Revision Notes

- One-line summary: Autocomplete and geo search are latency-sensitive scenario designs that need dedicated fields and hard guardrails.
- Three keywords: typeahead, geo_point, radius.
- One interview trap: using wildcard for autocomplete or unlimited radius for geo.
- Memory trick: typeahead must be fast; geo must be bounded.