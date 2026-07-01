# GraphQL Transport, Mutation, And Testing Cheat Sheet

## HTTP And GraphQL Errors

```text
HTTP status tells transport outcome.
GraphQL `errors` tells operation/execution outcome.
Both matter.
```

Client rule:

- inspect HTTP status
- inspect `errors`
- inspect `data` partiality
- inspect error `path` and `extensions.code`

## GET vs POST

| Method | Best Fit |
|---|---|
| POST | general GraphQL operations |
| GET | safe query operations, often persisted and cacheable |

Cache key should include:

- operation hash/ID
- variables
- auth/public scope
- relevant headers

## Mutation Checklist

- input type
- payload type
- auth before side effects
- idempotency key for retryable actions
- transaction boundary
- outbox/domain event plan
- stable error codes
- final state returned

## Custom Scalars

- parse and validate input
- serialize consistently
- document format/timezone/currency
- do not use generic `JSON` to avoid schema design

## Contract Testing

- schema diff
- operation validation
- generated types
- persisted operation registry
- auth regression tests
- nullability/error behavior tests

## Incremental Delivery

Use `@defer`/`@stream` only when server, gateway, transport, client cache, and observability support patches end to end.