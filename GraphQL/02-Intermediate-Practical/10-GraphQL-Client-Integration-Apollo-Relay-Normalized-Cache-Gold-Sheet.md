# GraphQL Client Integration, Apollo, Relay, Normalized Cache - Gold Sheet

> Track File #10 of 30 - Group 02: Intermediate Practical
> For: frontend/full-stack integration | Level: intermediate | Mode: clients and cache

## 1. Core Idea

GraphQL clients turn typed operations into UI data flows, caching, refetching, and optimistic updates.

```text
component -> operation -> client cache/network -> normalized entities -> UI state
```

---

## 2. Client Responsibilities

| Concern | Meaning |
|---|---|
| operation documents | query/mutation/subscription text |
| variables | runtime inputs |
| generated types | compile-time safety |
| normalized cache | entity storage by type/id |
| fetch policy | cache-first, network-only, cache-and-network |
| optimistic UI | temporary local result before server confirms |
| error policy | how partial data/errors are exposed |

---

## 3. Normalized Cache Mental Model

```text
Product:123 -> { id, name, priceCents }
User:7 -> { id, name }
query result -> references to normalized entities
```

Stable IDs and `__typename` help clients merge updates correctly.

---

## 4. Failure Modes

- schema changes break generated types
- missing IDs prevent normalization
- optimistic update does not match server result shape
- cache policy hides stale data
- partial errors are ignored by UI
- client queries too many nested fields

---

## 5. Interview Summary

```text
GraphQL clients manage operation documents, variables, generated types, network/cache policy, normalized entities, optimistic updates, and partial errors. Schema design should support stable IDs and predictable cache behavior.
```

---

## 6. Revision Notes

- One-line summary: Client integration turns schema contracts into typed UI data flows.
- Three keywords: generated types, normalized cache, fetch policy.
- One trap: changing schema shape without checking generated client operations and cache identity.