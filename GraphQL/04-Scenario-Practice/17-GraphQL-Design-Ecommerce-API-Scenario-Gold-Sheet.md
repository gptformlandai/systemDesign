# GraphQL Design E-Commerce API Scenario - Gold Sheet

> Track File #17 of 30 - Group 04: Scenario Practice
> For: API design interviews | Level: intermediate | Mode: product API design

## 1. Scenario

```text
Design a GraphQL API for product browsing, product detail, cart, and orders.
```

Goal: expose a client-friendly schema while protecting performance, auth, and evolution.

---

## 2. Schema Shape

Key types:

- `Product`
- `ProductConnection`
- `Cart`
- `CartItem`
- `Order`
- `User`

Key operations:

- `products(filter, sort, first, after)`
- `product(id)`
- `cart`
- `addToCart(input)`
- `checkout(input)`

---

## 3. Design Decisions

- cursor pagination for product lists
- stable `ID!` on entity types
- `Money` or cents integer for price
- auth required for cart/order fields
- DataLoader for product/user/order lookup
- mutation payloads return updated domain state
- deprecate fields rather than removing immediately

---

## 4. Interview Summary

```text
For an e-commerce GraphQL API, I design around product, cart, order, and user concepts, use cursor pagination, stable IDs, typed mutation inputs/payloads, request-scoped loaders, field-level auth, and schema evolution rules.
```

---

## 5. Revision Notes

- One-line summary: Good GraphQL API design models product workflows, not database tables.
- Three keywords: product, connection, mutation payload.
- One trap: exposing unbounded product reviews/orders as nested lists.