# 06. Aggregation Framework

## Mental Model

Aggregation is a pipeline. Documents flow through stages, and each stage transforms the stream.

```text
orders -> $match -> $unwind -> $group -> $sort -> result
```

## Find vs Aggregation

Use `find()` for simple reads. Use aggregation for grouping, reshaping, joins, facets, reports, window functions, and writing derived collections.

## Core Stages

| Stage | Use |
|---|---|
| `$match` | Filter early |
| `$project` | Select/compute fields |
| `$set` | Add computed fields |
| `$group` | Aggregate by key |
| `$sort` | Sort |
| `$unwind` | Expand arrays |
| `$lookup` | Join another collection |
| `$facet` | Multiple reports from same input |
| `$bucket` | Group into ranges |
| `$merge` | Write derived output |
| `$setWindowFields` | Moving averages, ranks, windows |

## Sales Report Example

```javascript
db.orders.aggregate([
  { $match: { tenantId: 't1', status: { $in: ['PAID', 'SHIPPED'] } } },
  { $unwind: '$items' },
  {
    $group: {
      _id: '$items.category',
      units: { $sum: '$items.quantity' },
      revenueCents: { $sum: { $multiply: ['$items.quantity', '$items.priceCents'] } }
    }
  },
  { $sort: { revenueCents: -1 } }
])
```

## `$lookup` Rule

Use `$lookup` after reducing the input set when possible.

Good shape:

```javascript
$match -> $sort -> $limit -> $lookup
```

Dangerous shape:

```javascript
$lookup -> $unwind -> $group over millions
```

## `$facet` Use Case

Use `$facet` for search pages that need results, brands, categories, and price bands from the same filter.

## `$merge` Use Case

Use `$merge` to create materialized dashboard collections.

```javascript
{ $merge: { into: 'dailyRevenue', on: '_id', whenMatched: 'replace', whenNotMatched: 'insert' } }
```

## Performance Rules

- Put `$match` early.
- Use indexes before stages reshape documents.
- Project large fields out early.
- Avoid huge `$lookup` and unbounded `$group`.
- Use `allowDiskUse` only with awareness of slower disk work.
- Precompute repeated dashboards.

Run:

```bash
bash SCRIPTS/run-mongosh.sh SCRIPTS/aggregation-labs.js
```
