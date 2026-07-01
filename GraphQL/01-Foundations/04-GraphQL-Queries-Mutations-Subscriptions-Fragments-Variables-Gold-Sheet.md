# GraphQL Queries, Mutations, Subscriptions, Fragments, Variables - Gold Sheet

> Track File #4 of 30 - Group 01: Foundations
> For: operation fluency | Level: beginner | Mode: operations

## 1. Core Idea

GraphQL operations describe what the client wants from the schema.

```graphql
query ProductPage($id: ID!) {
  product(id: $id) {
    id
    name
    priceCents
  }
}
```

---

## 2. Operation Types

| Operation | Purpose |
|---|---|
| query | read data |
| mutation | change data |
| subscription | receive stream of events |

Mutation example:

```graphql
mutation AddToCart($productId: ID!, $quantity: Int!) {
  addToCart(productId: $productId, quantity: $quantity) {
    cartId
    itemCount
  }
}
```

---

## 3. Fragments And Variables

Fragments reuse field selections:

```graphql
fragment ProductCard on Product {
  id
  name
  priceCents
}
```

Variables keep operations reusable and safer than string interpolation.

---

## 4. Directives

Built-in directives include:

- `@include(if: Boolean!)`
- `@skip(if: Boolean!)`
- `@deprecated(reason: String)` in schema SDL

---

## 5. Interview Summary

```text
Queries read, mutations write, and subscriptions stream. Variables make operations reusable, fragments share selections, and directives conditionally shape execution or document schema behavior.
```

---

## 6. Revision Notes

- One-line summary: Operations are the client-requested field tree.
- Three keywords: query, mutation, fragment.
- One trap: using string concatenation for inputs instead of variables.