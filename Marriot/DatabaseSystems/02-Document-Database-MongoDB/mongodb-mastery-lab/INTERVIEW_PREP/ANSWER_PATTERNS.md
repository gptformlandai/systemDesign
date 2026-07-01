# MongoDB Interview Answer Patterns

## Universal Answer Shape

Use this for most questions:

```text
1. Definition
2. Why it exists
3. How MongoDB implements it
4. Concrete example
5. Tradeoff
6. Failure mode
7. Production best practice
```

## Example: Indexes

Definition: an index is a data structure that lets MongoDB find documents without scanning the collection.

Why: hot queries need low latency at large data volume.

Implementation: `createIndex`, compound indexes, multikey indexes, TTL indexes, query planner.

Example:

```javascript
db.orders.createIndex({ tenantId: 1, status: 1, createdAt: -1 })
```

Tradeoff: faster reads and sorts, slower writes and more storage.

Failure mode: wrong index order causes scans or blocking sorts.

Production: validate with `explain('executionStats')` and monitor `$indexStats`.

## Example: Embed vs Reference

Embed bounded data read together. Reference unbounded, shared, or independently updated data.

Example: embed order line items; reference product reviews.

Tradeoff: embedding improves locality and atomicity; referencing prevents document growth but needs additional query or join.

## Example: Shard Key

A shard key determines data distribution and query routing. Good keys have high cardinality, even distribution, query isolation, write distribution, and stability.

Tradeoff: hashed keys distribute writes but hurt range locality. Range keys support range queries but can hotspot.

## Example: MongoDB vs PostgreSQL

MongoDB is better for aggregate-shaped, flexible, JSON-like operational data. PostgreSQL is better for relational integrity, complex joins, and ad hoc SQL reporting.

Strong answer: choose based on workload, not popularity.
