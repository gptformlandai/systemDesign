# 33. Distributed Systems Debugging: Microservices, Traces, Retries, Timeouts

## Goal

Debug failures that cross service boundaries: partial outages, retries, timeouts, circuit breakers, async calls, and version/config drift.

---

## Mental Model

Local bugs are often line-level.

Distributed bugs are often boundary-level.

```text
client -> gateway -> service A -> service B -> database/cache/queue
```

The bug may not be inside the service reporting the error. It may be upstream, downstream, or between them.

---

## Boundary Checklist

At every service boundary, ask:

```text
request:
  method, path, payload size, headers, auth, trace context

timeout:
  client timeout, server timeout, load balancer timeout, DB timeout

retry:
  retry count, backoff, jitter, idempotency

failure:
  status code, exception, reset, timeout, rejected connection

version:
  caller version, callee version, API/schema version
```

---

## Trace Reading Pattern

```text
root span:
  user-facing operation

wide span:
  where most latency is spent

red/error span:
  where exception was recorded

missing span:
  propagation break, uninstrumented service, async boundary

many repeated spans:
  retry storm or N+1 behavior

long gap:
  queueing, event-loop block, thread pool starvation, uninstrumented work
```

Do not assume the red span is the root cause. It may only be where failure surfaced.

---

## Timeout Budget

Timeouts must fit inside the caller's deadline.

```text
User request budget: 2000ms
  gateway: 100ms
  orders-api: 300ms
  payments-api: 500ms
  inventory-api: 300ms
  buffer: 800ms
```

Bad pattern:

```text
caller timeout = 2s
downstream timeout = 5s
retry count = 3
```

The caller gives up while downstream work continues, creating wasted load.

---

## Retry Storm Pattern

Symptoms:

```text
downstream latency increases
caller retries increase
traffic to downstream multiplies
error rate increases
queue/thread pools saturate
```

Fixes:

- exponential backoff
- jitter
- retry only safe/idempotent operations
- small retry budget
- circuit breaker
- load shedding
- idempotency key

---

## Circuit Breaker Debugging

| State | Meaning |
|---|---|
| Closed | calls pass normally |
| Open | calls fail fast to protect dependency |
| Half-open | test calls allowed to see if recovery happened |

Debug questions:

- Did the breaker open because downstream failed or because timeout too low?
- Is fallback returning stale, partial, or incorrect data?
- Are all instances opening at once?
- Is the monitor alerting on fallback rate?

---

## Missing Trace Context

Common causes:

```text
HTTP client not instrumented
custom headers stripped by proxy
message queue did not copy trace headers
W3C traceparent vs vendor header mismatch
thread/coroutine context lost
manual async task starts new root span
```

Fix:

```text
standardize propagation format
instrument HTTP/DB/queue clients
copy trace headers into message metadata
propagate context across executor/thread boundaries
```

---

## Version Drift

Distributed failures often happen when versions disagree.

```text
orders-api v3 sends field: discountPolicy
payments-api v2 does not understand it
gateway routes 20% traffic to v3
only canary users fail
```

Debug:

- split traces/logs by service version
- compare canary vs stable
- check schema compatibility
- check feature flag targeting
- check API contract tests

---

## Async Boundary Debugging

For queues/events:

```text
producer success != consumer success
```

Track:

- event ID
- correlation ID
- producer timestamp
- queue/topic
- consumer group
- delivery attempts
- DLQ status
- idempotency key

Async systems need event lineage, not only request traces.

---

## Practical Question

> Users see checkout timeouts. `orders-api` shows many errors, but payments owns the slow span. How do you debug?

---

## Strong Answer

I would start with an end-to-end trace from a timed-out checkout request. I would identify whether `orders-api` is failing locally or waiting on `payments-api`. If the payments span is wide and repeated, I would check timeout and retry behavior to see whether `orders-api` is multiplying load. Then I would inspect payments metrics: latency, error rate, CPU, DB pool, downstream calls, and recent deployments.

I would also verify timeout budgets: the downstream timeout and retry plan must fit inside the user request deadline. If payments is overloaded, I would mitigate with rollback, circuit breaker, reduced retry budget, or load shedding. Root cause could be payments itself, its database, a schema/version mismatch, or a retry storm from callers.

---

## Interview Sound Bite

Distributed debugging is boundary debugging. I use traces to find the slow or failing edge, logs to explain local state, metrics to detect saturation, and deployment/version tags to find what changed. Retries, timeouts, and circuit breakers can be either the fix or the outage amplifier.
