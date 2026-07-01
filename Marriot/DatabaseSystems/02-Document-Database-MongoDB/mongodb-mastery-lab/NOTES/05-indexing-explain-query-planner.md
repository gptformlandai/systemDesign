# 05. Indexing, Explain, and Query Planner

## What an Index Does

An index lets MongoDB find matching documents without scanning the whole collection. It is a sorted data structure over selected fields.

Tradeoff:

- Reads get faster.
- Writes get slower because indexes must be maintained.
- Storage and memory usage increase.

## Compound Indexes

For a query like:

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' }).sort({ createdAt: -1 })
```

Use:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

## ESR Rule

Start index design with:

1. Equality
2. Sort
3. Range

It is a guideline, not a law. Always validate with `explain()`.

## Explain Plan

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' })
  .sort({ createdAt: -1 })
  .explain('executionStats')
```

Read:

- `winningPlan`
- `IXSCAN` vs `COLLSCAN`
- `totalKeysExamined`
- `totalDocsExamined`
- `nReturned`

Good sign:

```text
keysExamined ~= docsExamined ~= nReturned
```

Bad sign:

```text
docsExamined is huge but nReturned is tiny
```

## Covered Queries

A covered query can be answered from the index without fetching full documents.

```javascript
db.users.createIndex({ tenantId: 1, email: 1, name: 1 })

db.users.find(
  { tenantId: 't1', email: 'asha@example.com' },
  { _id: 0, email: 1, name: 1 }
)
```

## Index Types To Know

- single field
- compound
- multikey
- unique
- TTL
- partial
- sparse
- text
- geospatial
- hashed
- wildcard
- vector search index in Atlas

## Anti-Patterns

- Index every field.
- Ignore sort order.
- Use low-cardinality index alone for hot query.
- Keep duplicate indexes.
- Use deep skip pagination.
- Use regex contains search for user-facing search.

## Practical Exercise

Run:

```bash
bash SCRIPTS/run-mongosh.sh SCRIPTS/create-indexes.js
bash SCRIPTS/run-mongosh.sh EXAMPLES/mongosh/03-indexing-explain.js
```

Then inspect `PERFORMANCE/EXPLAIN_PLAYBOOK.md`.
