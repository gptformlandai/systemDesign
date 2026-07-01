# GraphQL Pro Gap Fill: Transport, Scalars, Directives, Mutations, Testing - MAANG Sheet

> Gap-fill appendix - Group 03: Senior Production
> For: senior GraphQL/platform interviews | Level: pro | Mode: production edge cases

## 1. Why This Gap Fill Exists

Most GraphQL tracks cover schema, resolvers, N+1, auth, and federation. Senior interviews often probe the smaller edges that cause real production incidents:

- GraphQL-over-HTTP semantics and status codes
- GET vs POST, caching, and persisted operations
- custom scalars and validation boundaries
- schema directives and directive governance
- interfaces/unions for polymorphic domains
- mutation idempotency and transaction boundaries
- contract testing, code generation, and operation registry drift
- incremental delivery with `@defer` and `@stream`

```text
pro GraphQL = schema contract + transport contract + execution contract + client contract
```

---

## 2. GraphQL Over HTTP And Status Codes

GraphQL often runs over HTTP, but GraphQL success/failure is not identical to HTTP success/failure.

| Case | Common HTTP Status | GraphQL Body |
|---|---:|---|
| valid operation with resolver error | 200 | `data` may be partial, `errors` present |
| parse/validation error | 400 or 200 by implementation/policy | `errors` present, often no `data` |
| authentication failure | 401 | `errors` or transport-level failure |
| authorization failure | 200 or 403 by policy | field/object error or null result |
| server unavailable | 500/503 | may not be GraphQL-shaped |

Senior rule:

```text
Clients must inspect GraphQL `errors`, not only HTTP status.
```

---

## 3. GET, POST, Persisted Operations, And CDN Caching

GraphQL can use POST for arbitrary operations and GET for query operations when supported.

Production decisions:

- POST is common for general GraphQL execution.
- GET can improve CDN/browser caching for safe persisted queries.
- persisted operation IDs make CDN cache keys stable.
- cache keys must include operation ID/hash, variables, auth/public scope, and headers that affect response.

Failure modes:

- CDN caches private data
- clients send mutations over cacheable GET
- long query strings exceed proxy limits
- operation registry and client bundle get out of sync

---

## 4. Custom Scalars And Validation Boundaries

Custom scalars such as `DateTime`, `URL`, `Email`, `JSON`, or `Money` need explicit parse/serialize validation.

Good scalar policy:

- validate input during parse
- serialize consistently
- document timezone/format/currency rules
- avoid dumping arbitrary `JSON` everywhere
- keep domain invariants in domain layer, not only scalar parsing

Trap:

```text
Custom scalar does not automatically make downstream database/service input safe.
```

---

## 5. Directives, Interfaces, And Unions

Directives can document or alter schema/execution behavior. Use governance for custom directives such as `@auth`, `@cost`, `@tag`, or `@deprecated`.

Interfaces and unions model polymorphism:

```graphql
interface SearchResult {
  id: ID!
}

type Product implements SearchResult {
  id: ID!
  name: String!
}

type Seller implements SearchResult {
  id: ID!
  displayName: String!
}

union CheckoutResult = CheckoutSuccess | PaymentDeclined | InventoryUnavailable
```

Use polymorphism when clients need typed branching. Avoid unions when a stable payload with status/error fields is clearer.

---

## 6. Mutation Idempotency And Transactions

Mutations are not automatically safe to retry.

Senior mutation design includes:

- input type and payload type
- authorization before side effects
- idempotency key for retried client/network calls
- transaction boundary around related writes
- domain event/outbox when publishing after write
- stable domain error codes
- client-visible final state after mutation

Failure modes:

- duplicate charge/order/cart item on retry
- partial write without rollback or compensation
- event published but database transaction failed
- mutation returns success before downstream work is durable

---

## 7. Contract Testing, Codegen, And Operation Registry

Production GraphQL should test both schema and client operations.

Controls:

- schema diff checks
- client operation validation against proposed schema
- generated server/client types
- persisted operation registry checks
- breaking-change policy
- operation usage telemetry before removal
- contract tests for key operations and auth cases

Failure modes:

- schema is valid but active client operation breaks
- generated types drift from deployed schema
- persisted query registry misses new client operation
- nullable/non-null change breaks client assumptions

---

## 8. Incremental Delivery: `@defer` And `@stream`

Incremental delivery can send initial critical fields first and later patches for deferred fragments or streamed list items.

Use only when platform and clients support it end to end.

Check:

- server support
- router/gateway support
- client cache support
- transport support
- observability for patches
- fallback for unsupported clients

Failure modes:

- proxy buffers patches and removes benefit
- client cache mishandles partial patches
- errors in deferred fragments are not surfaced clearly
- SLOs ignore late patch latency

---

## 9. Interview Summary

```text
At senior level, I cover GraphQL transport behavior, not just schema. I explain HTTP status vs GraphQL errors, GET/POST and persisted operation caching, custom scalar validation, directive governance, polymorphism, mutation idempotency and transactions, contract testing/codegen, and incremental delivery risks.
```

---

## 10. Revision Notes

- One-line summary: Pro GraphQL work includes transport, mutation, type-system, and client-contract edges.
- Three keywords: transport, idempotency, contract.
- One trap: checking only HTTP status and missing GraphQL partial errors.