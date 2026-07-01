# Performance Tuning Exercises

## Exercise 1: Fix a Collection Scan

1. Drop non-primary indexes on `orders` in a local throwaway lab.
2. Run:

```javascript
db.orders.find({ tenantId: 't1', status: 'PAID' }).sort({ createdAt: -1 }).explain('executionStats')
```

3. Add the right index.
4. Compare explain output.

## Exercise 2: Replace Deep Skip

Bad query:

```javascript
db.orders.find({ tenantId: 't1' }).sort({ createdAt: -1 }).skip(100000).limit(20)
```

Task: rewrite with cursor pagination using `createdAt` and `_id`.

## Exercise 3: Reduce Returned Document Size

Query products with and without projection. Compare output size and fields.

```javascript
db.products.find({ tenantId: 't1' }, { name: 1, priceCents: 1, _id: 0 })
```

## Exercise 4: Avoid N+1 Queries

Scenario: an API lists 50 orders and fetches each user separately.

Fix options:

- embed customer snapshot in order
- batch query users with `$in`
- `$lookup` after `$limit`
- maintain read model

Explain the tradeoff of each.

## Exercise 5: Precompute Dashboard

Scenario: dashboard computes revenue by status every page load.

Task: design a `dailyRevenue` or `orderDailyStats` collection.

Questions:

- How is it updated?
- What happens if updates lag?
- How do you rebuild it?

## Exercise 6: Write Slowdown After Indexes

Scenario: ingestion slows after adding 12 indexes.

Tasks:

- list indexes
- identify unused indexes with `$indexStats`
- drop duplicates
- batch writes
- split analytics indexes into read model if needed
