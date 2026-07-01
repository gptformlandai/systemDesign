# 06. Status Codes, Errors, Metadata, Deadlines, Cancellation

## Goal

Handle failure as a first-class part of the gRPC contract.

```text
deadline + metadata + handler result -> status code + message + trailers
```

---

## Canonical Status Codes

| Code | Meaning | Retry? |
|---|---|---|
| `OK` | success | no |
| `INVALID_ARGUMENT` | request is invalid regardless of state | no |
| `NOT_FOUND` | requested resource does not exist | no, usually |
| `ALREADY_EXISTS` | create conflict | no, usually |
| `FAILED_PRECONDITION` | system state blocks operation | no until fixed |
| `ABORTED` | concurrency conflict | maybe with retry logic |
| `RESOURCE_EXHAUSTED` | quota or capacity exhausted | maybe after backoff |
| `UNAUTHENTICATED` | missing/invalid identity | no until credentials fixed |
| `PERMISSION_DENIED` | identity lacks permission | no |
| `UNAVAILABLE` | transient service unavailable | yes if idempotent |
| `DEADLINE_EXCEEDED` | time budget expired | maybe if safe and useful |
| `INTERNAL` | server invariant or bug | no blind retries |

---

## Deadlines

A deadline is the maximum time budget for an RPC.

Deadlines should be:

- set by callers
- propagated across downstream RPCs
- shorter than user-facing or queue-processing budgets
- visible in traces/logs
- enforced before expensive work continues

Bad pattern:

```text
service A has no deadline -> service B waits -> service C queues -> cascading latency
```

Better pattern:

```text
caller budget 300 ms -> service A keeps 50 ms -> service B gets 250 ms -> dependency gets bounded sub-budget
```

---

## Cancellation

When the client cancels or deadline expires, the server should stop unnecessary work.

Check cancellation before:

- expensive database calls
- long loops
- streaming writes
- background fan-out
- large response assembly

---

## Metadata

Metadata carries request or response context.

Common metadata:

- authorization token
- request id/correlation id
- trace context
- tenant id
- locale/region
- feature flags
- idempotency key

Do not put large, sensitive, or business-critical payloads in metadata without strong controls.

---

## Rich Errors

Some ecosystems support richer error details using protobuf messages such as validation violations or retry info.

Use rich errors when clients need structured recovery behavior. Avoid leaking internal stack traces or sensitive system details.

---

## Debugging Checklist

1. What status code did the client receive?
2. Did the server handler run?
3. Was there a caller deadline?
4. Did the server observe cancellation?
5. Was auth metadata present and valid?
6. Are trailers/status visible in logs or traces?
7. Is the error retryable and is the method idempotent?

---

## Interview Sound Bite

gRPC failure handling is explicit: callers set deadlines, servers observe cancellation, metadata carries context, and canonical status codes tell clients whether the failure is validation, auth, concurrency, capacity, dependency, timeout, or a server bug.