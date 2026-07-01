# Lab 05: Pagination And Filtering

## Goal

Design bounded list fields with stable ordering.

## Exercise

Improve this unsafe field:

```graphql
type Query {
  products: [Product!]!
}
```

Better shape:

```graphql
type Query {
  products(filter: ProductFilterInput, first: Int!, after: String): ProductConnection!
}
```

## Checklist

- max `first` value
- stable default sort
- opaque cursor
- filter validation
- auth constraints included in data access
- behavior when data changes between pages

## Interview Takeaway

```text
Production GraphQL list fields need bounded pagination, stable order, explicit filters, and clear consistency expectations.
```