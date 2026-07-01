# GraphQL Hands-On Exercises And Query Drills

> Track File #28 of 30 - Group 06: Practice Upgrade
> For: query/schema fluency | Level: beginner to pro | Mode: drills

## 1. Drill Rules

- write SDL by hand
- write operation documents by hand
- name every operation
- explain resolver path for every query
- identify auth and cache scope before optimizing
- write one production risk for every design

---

## 2. SDL Drills

1. Model `Product`, `Category`, and `Review`.
2. Add `ProductConnection` with `edges` and `pageInfo`.
3. Add `CreateReviewInput` and `CreateReviewPayload`.
4. Add a deprecated field with a reason.
5. Add an interface for `Node { id: ID! }`.

---

## 3. Query Drills

Write operations for:

- product detail
- paginated product list
- add to cart mutation
- user order history
- subscription for order status updates

Each operation must use variables and have a name.

---

## 4. Resolver Drills

For each operation, write:

```text
field path -> resolver -> data source -> auth check -> caching/batching plan
```

---

## 5. Debug Drills

Practice explaining:

- validation error
- execution error
- null bubbling
- N+1 latency
- unauthorized nested field
- stale client cache
- breaking schema change

---

## 6. Senior Drill

Take one real product page and produce:

1. schema excerpt
2. operation document
3. resolver map
4. DataLoader plan
5. auth policy
6. complexity controls
7. observability fields
8. schema evolution plan