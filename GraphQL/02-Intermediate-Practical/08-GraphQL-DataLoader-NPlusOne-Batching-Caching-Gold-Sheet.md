# GraphQL DataLoader, N+1, Batching, Caching - Gold Sheet

> Track File #8 of 30 - Group 02: Intermediate Practical
> For: resolver performance | Level: intermediate | Mode: N+1 prevention

## 1. Core Idea

GraphQL nested field execution can accidentally make one data-source call per object. This is the N+1 problem.

```text
1 query for products + N queries for each product owner = N+1 fanout
```

DataLoader-style batching groups many field loads into one request per key type per tick/request.

---

## 2. DataLoader Rules

- create loaders per request, not global mutable singleton
- batch by key type and auth scope
- preserve result order for requested keys
- cache only within safe scope
- include tenant/user constraints in load behavior
- instrument batch size and call count

---

## 3. Example Flow

```text
products resolver returns 50 products
owner resolver requests 50 owner IDs
DataLoader batches into one user service call
results are mapped back to each product owner field
```

---

## 4. Failure Modes

- global loader leaks cross-user data
- per-field database calls cause latency explosion
- loader cache ignores tenant/auth context
- batching hides slow upstream dependency
- too-large batches overload a downstream service

---

## 5. Interview Summary

```text
GraphQL N+1 happens when nested resolvers fan out to data sources. I use request-scoped DataLoader batching and caching, preserve auth scope, instrument resolver/data-source counts, and set query complexity limits.
```

---

## 6. Revision Notes

- One-line summary: DataLoader turns resolver fanout into scoped batch calls.
- Three keywords: N+1, batching, request cache.
- One trap: using a global DataLoader cache and leaking data across users or tenants.