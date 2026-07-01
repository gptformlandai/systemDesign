# Lab 08: Transport, Mutations, And Contract Testing

## Goal

Practice the production edges that sit around the GraphQL execution engine.

## Part 1: HTTP And Error Semantics

Explain why each case may need different client handling:

| Case | Client Check |
|---|---|
| HTTP 200 with `errors` | inspect GraphQL error path/code |
| HTTP 401 | refresh/authenticate |
| HTTP 400 validation error | fix operation/schema mismatch |
| HTTP 503 | retry/backoff or failover |

## Part 2: Mutation Idempotency

Design an `addToCart` or `checkout` mutation with:

- `idempotencyKey`
- input type
- payload type
- domain error codes
- transaction boundary
- final state returned to client

Sketch:

```graphql
input CheckoutInput {
  cartId: ID!
  idempotencyKey: String!
}

type CheckoutPayload {
  orderId: ID
  status: CheckoutStatus!
  errorCode: String
}
```

## Part 3: Contract Testing

For every schema change, define:

1. schema diff result
2. active operation validation result
3. generated type update
4. persisted operation registry update
5. client migration plan

## Part 4: Incremental Delivery

Pick one product page query and decide whether `@defer` would help. Document:

- critical fields
- deferred fields
- client fallback
- patch observability
- unsupported-client behavior

## Interview Takeaway

```text
GraphQL production readiness includes transport semantics, idempotent mutation design, contract tests, codegen, operation registry checks, and incremental delivery support boundaries.
```