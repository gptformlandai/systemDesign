# GraphQL Resolvers, Execution, Context, Data Sources - Gold Sheet

> Track File #5 of 30 - Group 02: Intermediate Practical
> For: backend implementation | Level: intermediate | Mode: resolver execution

## 1. Core Idea

Resolvers are functions that produce field values during GraphQL execution.

```text
operation field tree -> resolver per field -> data source calls -> nested field resolution
```

Typical resolver signature:

```text
resolver(parent, args, context, info)
```

---

## 2. Resolver Inputs

| Input | Meaning |
|---|---|
| parent | result from parent field resolver |
| args | field arguments from operation |
| context | request-scoped auth, loaders, services, trace IDs |
| info | execution metadata and selection details |

---

## 3. Data Source Pattern

Resolvers should call a service/data-source abstraction rather than embedding raw database logic everywhere.

```text
resolver -> domain service/data source -> database/service/cache
```

This improves testing, batching, auth, observability, and reuse.

---

## 4. Failure Modes

- resolver does too much business logic
- resolver calls database once per child field
- context is treated as global mutable state
- auth is skipped in nested resolvers
- resolver returns shape inconsistent with schema

---

## 5. Interview Summary

```text
Resolvers execute the schema field tree. I keep them thin, pass user/loaders/services through context, route data access through data sources, and instrument resolver paths for latency and errors.
```

---

## 6. Revision Notes

- One-line summary: Resolvers are field-level execution functions.
- Three keywords: parent, args, context.
- One trap: assuming only top-level resolvers need auth or performance checks.