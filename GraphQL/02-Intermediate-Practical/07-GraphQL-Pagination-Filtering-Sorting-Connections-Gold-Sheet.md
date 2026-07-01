# GraphQL Pagination, Filtering, Sorting, Connections - Gold Sheet

> Track File #7 of 30 - Group 02: Intermediate Practical
> For: list API design | Level: intermediate | Mode: pagination

## 1. Core Idea

GraphQL list fields need explicit pagination, filtering, and sorting contracts.

```text
client list request -> stable order -> page boundary -> result edges/nodes -> pageInfo
```

---

## 2. Offset vs Cursor

| Pattern | Good For | Risk |
|---|---|---|
| offset/limit | simple admin lists | duplicates/misses under changes |
| cursor | feeds, search, changing data | more implementation complexity |

Cursor connection shape:

```graphql
type ProductConnection {
  edges: [ProductEdge!]!
  pageInfo: PageInfo!
}

type ProductEdge {
  cursor: String!
  node: Product!
}
```

---

## 3. Design Rules

- define stable sort order
- make filters explicit input types
- limit maximum page size
- avoid unbounded list fields
- use opaque cursors
- document consistency expectations

---

## 4. Failure Modes

- unbounded lists cause memory/latency incidents
- offset pagination skips/duplicates under writes
- cursor leaks internal IDs or query structure
- sorting differs between pages
- filters bypass authorization constraints

---

## 5. Interview Summary

```text
For GraphQL lists, I define stable ordering, explicit filter/sort inputs, max page size, and cursor-based pagination when data changes frequently. I avoid unbounded nested lists because they create cost and latency risk.
```

---

## 6. Revision Notes

- One-line summary: Every production list field needs a bounded pagination contract.
- Three keywords: cursor, pageInfo, max page size.
- One trap: exposing `[Item!]!` with no pagination on high-cardinality data.