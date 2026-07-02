# 35. OpenTelemetry Metrics, Channelz, Admin Debugging, And Production RCA

## Goal

Make gRPC production behavior provable with metrics, traces, logs, Channelz/admin evidence, and repeatable RCA workflows.

```text
method metrics + attempt metrics + channel state + trace + safe logs + admin debug = explainable incident
```

---

## 1. Observability Layers

| Layer | Evidence |
|---|---|
| Application | business errors, validation, dependency timing |
| gRPC client | call duration, attempt duration, retries, hedges, target |
| Channel | state, subchannels, resolver/LB behavior |
| Server | method duration, status, active streams, message size |
| Proxy/mesh | route, cluster, upstream reset, timeout, mTLS |
| Kubernetes | EndpointSlice, readiness, pod restarts, node pressure |

Senior rule:

```text
Do not stop at "client saw DEADLINE_EXCEEDED".
Prove whether time was spent in queueing, connection pick, proxy, server,
handler, downstream dependency, retry delay, or slow stream consumer.
```

---

## 2. Official gRPC OTel Metric Families

Common modern gRPC OTel metric ideas:

| Metric Family | Why It Matters |
|---|---|
| `grpc.client.call.duration` | end-to-end client call latency |
| `grpc.client.attempt.started` | retry/hedge attempt count |
| `grpc.client.attempt.duration` | per-attempt latency |
| `grpc.client.attempt.sent_total_compressed_message_size` | request payload size |
| `grpc.client.attempt.rcvd_total_compressed_message_size` | response payload size |
| `grpc.client.call.retries` | retry count |
| `grpc.client.call.hedges` | hedge count |
| `grpc.server.call.started` | server request volume |
| `grpc.server.call.duration` | server-side latency |
| `grpc.server.call.sent_total_compressed_message_size` | response size |
| `grpc.server.call.rcvd_total_compressed_message_size` | request size |

Important labels:

- `grpc.method`
- `grpc.status`
- `grpc.target`
- locality/backend labels when available
- client service/version as custom low-cardinality attributes

Avoid:

- user id
- request id
- token
- full resource id
- raw peer IP if cardinality is huge

---

## 3. PromQL Examples

Traffic:

```promql
sum by (grpc_method) (
  rate(grpc_server_call_started_total[5m])
)
```

Error rate:

```promql
sum by (grpc_method, grpc_status) (
  rate(grpc_server_call_duration_count{grpc_status!="OK"}[5m])
)
/
sum by (grpc_method) (
  rate(grpc_server_call_duration_count[5m])
)
```

p99 latency:

```promql
histogram_quantile(
  0.99,
  sum by (le, grpc_method) (
    rate(grpc_server_call_duration_bucket[5m])
  )
)
```

Retry amplification:

```promql
sum(rate(grpc_client_attempt_started_total[5m]))
/
sum(rate(grpc_client_call_duration_count[5m]))
```

Metric names differ by language/exporter, so treat these as dashboard shape, not copy-paste law.

---

## 4. Channelz / Admin Debugging

Channelz is a gRPC debugging facility that exposes channel, subchannel, socket, and server information in supported languages.

Use it to answer:

- Is the channel `READY`, `IDLE`, or in `TRANSIENT_FAILURE`?
- Which target is the client using?
- How many subchannels exist?
- Are sockets connected?
- Are calls failing before reaching the server?
- Is one backend carrying all traffic?

Security:

```text
Never expose Channelz/admin endpoints publicly.
Protect them with localhost, mTLS, authz, or break-glass access.
```

When Channelz is unavailable, use:

- client logs at gRPC debug level for short controlled windows
- Envoy admin endpoints
- mesh dashboards
- grpcurl/grpcui against non-prod or restricted admin paths
- packet/TLS/ALPN checks only when necessary

---

## 5. Trace Design

Each trace should include:

- client span
- proxy span if mesh/gateway participates
- server span
- downstream dependency spans
- retry/attempt annotations if available
- final status code
- deadline/timeout budget if available

Metadata propagation:

```text
Trace context often travels via metadata.
Interceptors should inject/extract consistently and redact sensitive metadata.
```

---

## 6. RCA Workflow

```text
1. Scope:
   Which service/method/client/version/region/status changed?

2. Deadline:
   What was the caller's deadline and how much budget was consumed?

3. Attempts:
   Were retries/hedges triggered? Did attempts go to different backends?

4. Channel:
   Are channels READY? Any resolver/xDS/LB changes?

5. Server:
   Did the server receive the RPC? What status and latency did it record?

6. Proxy:
   Any route timeout, upstream reset, mTLS, or circuit-breaker evidence?

7. Dependency:
   Which downstream span or queue consumed the time?

8. Mitigation:
   Roll back config, drain bad backend, disable unsafe retry, scale bottleneck,
   or reduce payload/stream pressure.
```

---

## 7. Interview Scenario

> Clients report intermittent `UNAVAILABLE`, but server logs show no errors.

Good answer:

```text
If server logs show no request, I would look before the handler: channel state,
resolver output, subchannel readiness, TLS/ALPN, proxy resets, EndpointSlices,
and LB policy. I would compare client attempt metrics with server call metrics.
If client attempts increase but server call count does not, the failure is likely
connection, proxy, resolver, or mTLS before application code. Channelz or mesh
admin evidence should show whether channels are READY and which backends are
selected.
```

---

## Senior Sound Bite

gRPC observability is not only method latency. It includes per-attempt metrics, retry/hedge amplification, message sizes, channel state, resolver/LB/xDS evidence, proxy resets, and safe trace/log context. That is how you prove where an RPC failed.

## Official Source Notes

- gRPC OpenTelemetry metrics: <https://grpc.io/docs/guides/opentelemetry-metrics/>
- Debugging: <https://grpc.io/docs/guides/debugging/>
- Reflection: <https://grpc.io/docs/guides/reflection/>
- Metadata: <https://grpc.io/docs/guides/metadata/>

