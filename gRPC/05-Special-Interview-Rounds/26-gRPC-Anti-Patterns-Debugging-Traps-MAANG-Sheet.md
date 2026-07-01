# 26. gRPC Anti-Patterns And Debugging Traps

## Contract Anti-Patterns

| Anti-Pattern | Risk |
|---|---|
| reusing field numbers | old clients decode wrong meaning |
| deleting fields without `reserved` | future accidental reuse |
| using `map<string,string>` as main payload | weak contract and validation drift |
| enum zero is meaningful state | missing value looks valid |
| changing units without new field | silent semantic corruption |
| one giant `GenericRequest` | defeats typed RPC design |

---

## Runtime Anti-Patterns

| Anti-Pattern | Risk |
|---|---|
| no caller deadlines | cascading latency and stuck resources |
| retrying non-idempotent methods | duplicate side effects |
| ignoring cancellation | wasted work and saturation |
| unbounded stream buffers | memory incidents |
| logging all metadata | token or sensitive data exposure |
| no per-method metrics | impossible to isolate incident scope |

---

## Deployment Anti-Patterns

| Anti-Pattern | Risk |
|---|---|
| assuming any HTTP proxy handles gRPC | broken HTTP/2 behavior |
| route timeout shorter than app deadline | unexpected proxy failures |
| no connection draining | rolling deploys break streams/calls |
| relying only on DNS round-robin | poor distribution with long-lived connections |
| enabling reflection everywhere without review | introspection exposure |

---

## Debugging Traps

### Trap 1: `OK` Means Everything Is Fine

`OK` only means the RPC completed successfully. It does not prove the business data is semantically correct. Proto breaking changes can produce wrong data with `OK`.

### Trap 2: `DEADLINE_EXCEEDED` Means Server Is Slow

The budget may be consumed in the client, proxy, queue, TLS handshake, server, dependency, retries, or response transfer.

### Trap 3: `UNAVAILABLE` Means Server Is Down

It can mean DNS failure, no healthy endpoints, TLS failure, LB issue, proxy reset, connection drain, or server shutdown.

### Trap 4: Streaming Is Just A Loop

Streaming needs flow control, bounded buffers, cancellation, reconnect, dedupe, and observability.

---

## Prevention Checklist

- Buf lint and breaking checks.
- Caller deadlines on every RPC.
- Status-code mapping guide.
- Idempotency keys for side-effect retries.
- Per-method metrics and traces.
- Safe metadata redaction.
- Streaming load tests with slow consumers.
- Mesh/proxy timeout alignment tests.
- Cert rotation runbook.

---

## Interview Sound Bite

Most serious gRPC mistakes come from treating it like simple generated code. The real risks are schema evolution, missing deadlines, unsafe retries, streaming backpressure, proxy/HTTP2 behavior, metadata leaks, and poor method-level observability.