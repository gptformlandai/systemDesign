# 14. Observability: Metrics, Tracing, Logs, SLOs

## Goal

Make gRPC behavior diagnosable from production evidence.

```text
method + status + latency + deadline + metadata context + trace span + logs = debuggable RPC
```

---

## Golden Signals For gRPC

| Signal | Examples |
|---|---|
| traffic | RPC count by service/method |
| errors | status code rate by method |
| latency | p50/p90/p95/p99 by method/status |
| saturation | active streams, queue depth, connection/subchannel state, CPU/memory |

---

## Required Labels

Useful low-cardinality labels:

- service
- method
- status code
- client service
- deployment version
- region/zone
- route or cluster when using Envoy/mesh

Avoid high-cardinality labels such as user id, order id, token, or request id in metrics.

---

## Trace Spans

Each RPC should show:

- client span
- server span
- method name
- status code
- deadline or timeout if available
- downstream dependency spans
- retries/attempts when visible
- useful error details without sensitive data

Trace propagation often travels through metadata. Interceptors should handle propagation consistently.

---

## Logs

Log at boundaries:

- request accepted with method, request id, caller, safe identifiers
- auth decision when needed
- validation failure with reason category
- dependency failure with status/code
- final status and latency

Redact metadata and payload fields that can contain secrets or personal data.

---

## SLO Examples

```text
99.9% of GetOrder RPCs return OK within 150 ms over 30 days.
99.5% of CreatePayment RPCs complete with non-INTERNAL status within 500 ms.
Deadline exceeded rate for InventoryService/ListItems remains below 0.1%.
```

SLOs should separate client errors from server/dependency failures where possible.

---

## Interview Sound Bite

For gRPC observability, I instrument method-level metrics by status and latency, propagate traces through metadata, log safe request context and final status, monitor deadlines and active streams, and define SLOs around user-visible or caller-visible RPC behavior.