# Aggregation Exercises

Use `SCRIPTS/aggregation-labs.js` for runnable examples, then solve these manually.

## Exercise 1: Orders by Status

Goal: count orders and revenue by status.

Stages to use:

- `$match`
- `$group`
- `$sort`

Stretch: add average order value.

## Exercise 2: Top Products by Units Sold

Goal: unwind order items and rank SKUs.

Stages:

- `$match`
- `$unwind`
- `$group`
- `$sort`
- `$limit`

Question: why does `$unwind` increase pipeline cardinality?

## Exercise 3: Customer Order Summary

Goal: join users and orders after limiting recent orders.

Stages:

- `$match`
- `$sort`
- `$limit`
- `$lookup`
- `$unwind`
- `$project`

Question: why is `$lookup` after `$limit` safer?

## Exercise 4: Faceted Product Search

Goal: return product results, brand counts, category counts, and price buckets.

Stages:

- `$match`
- `$facet`
- `$bucket`
- `$group`

Question: when should Atlas Search replace this pattern?

## Exercise 5: Daily Revenue Materialized View

Goal: aggregate orders into `dailyRevenue`.

Stages:

- `$match`
- `$group`
- `$merge`

Question: what consistency tradeoff does a materialized view introduce?

## Exercise 6: Moving Average

Goal: compute moving average from `dailyRevenue`.

Stage:

- `$setWindowFields`

Question: what index supports sorting by tenant and day?
