# 13. Resilience: Retries, Hedging, Deadlines, Idempotency

## Goal

Make gRPC calls resilient without amplifying outages.

```text
deadline -> retry policy -> idempotency -> backoff -> circuit breaking -> observability
```

---

## First Rule

Retries are not a replacement for deadlines. A retry policy without a total time budget can multiply load during an incident.

---

## Retry Decision

| Condition | Guidance |
|---|---|
| method is read-only or idempotent | retry may be safe |
| method causes side effects | require idempotency key or no automatic retry |
| status is `UNAVAILABLE` | retry with backoff if safe |
| status is `RESOURCE_EXHAUSTED` | retry only with server guidance/backoff |
| status is `INVALID_ARGUMENT` | do not retry |
| deadline nearly exhausted | do not start another attempt |

---

## Idempotency

Idempotency means repeating the same request has the same intended effect.

Patterns:

- client-generated idempotency key
- request id stored with operation result
- dedupe table with TTL
- operation state machine
- safe retry only after persistence boundary is clear

---

## Hedging

Hedging sends a backup request after a delay to reduce tail latency.

Use hedging carefully:

- only for safe/idempotent methods
- cap total attempts
- use delay to avoid immediate duplicate load
- exclude overloaded backends if possible
- monitor duplicate work and downstream load

---

## Circuit Breaking

Circuit breaking stops repeated calls to unhealthy dependencies.

Signals:

- high `UNAVAILABLE` or `DEADLINE_EXCEEDED`
- connection failures
- dependency saturation
- high queue wait
- health check failure

Circuit breaking should protect the caller and the dependency.

---

## Interview Sound Bite

For gRPC resilience, I set caller deadlines first, retry only safe/idempotent operations with bounded attempts and backoff, use idempotency keys for side effects, apply hedging only for carefully selected reads, and monitor retry amplification, status codes, and downstream saturation.