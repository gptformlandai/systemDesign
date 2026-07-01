# Lab 02: Resolver Execution And Context

## Goal

Map an operation to resolver paths and request context.

## Exercise

For `ProductList`, write this table:

| Field Path | Resolver | Data Source | Context Needed |
|---|---|---|---|
| `Query.products` | products resolver | product service/search index | user, tenant, request ID |
| `Product.seller` | seller resolver | seller service | loader, auth scope |

## Checklist

- identify parent/args/context/info for each resolver
- identify which fields need auth
- identify which fields need batching
- identify which fields should be traced

## Interview Takeaway

```text
Resolver debugging starts with the field path and context. I map each selected field to the resolver, data source, auth policy, and trace evidence.
```