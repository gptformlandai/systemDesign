# GraphQL Mental Model: Schema, Operations, Resolvers - Hot Sheet

> Track File #1 of 30 - Group 01: Foundations
> For: first principles | Level: beginner | Mode: mental model

## 1. Core Idea

GraphQL is a typed contract and execution model for graph-shaped API data.

```text
client operation -> schema validation -> resolver execution -> data sources -> data/errors response
```

The schema says what is possible. The operation says what the client wants. Resolvers decide how each field is produced.

---

## 2. Main Parts

| Part | Meaning |
|---|---|
| schema | typed API contract |
| query | read operation |
| mutation | write operation |
| subscription | real-time stream operation |
| resolver | function behind a field |
| context | request-scoped auth/loaders/services |
| data source | database, service, cache, or API behind resolvers |

---

## 3. REST vs GraphQL Framing

REST commonly exposes many resource endpoints. GraphQL exposes a typed graph and lets the client select fields from that graph.

GraphQL helps with:

- over-fetching
- under-fetching
- endpoint sprawl
- typed client/server contracts
- aggregating multiple backend sources

GraphQL adds risk around:

- resolver fanout
- caching
- authorization
- query cost
- schema governance

---

## 4. Interview Summary

```text
GraphQL is a typed API contract plus execution engine. Clients send operations against a schema; the server validates them, walks the selected field tree, calls resolvers, and returns data with structured errors.
```

---

## 5. Revision Notes

- One-line summary: GraphQL is schema plus operation plus resolver execution.
- Three keywords: schema, operation, resolver.
- One trap: calling GraphQL just a replacement for REST without discussing execution and production tradeoffs.