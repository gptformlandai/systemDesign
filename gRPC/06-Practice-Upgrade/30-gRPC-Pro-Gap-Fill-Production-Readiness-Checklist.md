# 30. gRPC Pro Gap-Fill Production Readiness Checklist

## Purpose

Use this file to find gaps after you think you know gRPC.

---

## Contract Readiness

- [ ] Versioned package names are used.
- [ ] Field numbers are never reused.
- [ ] Deleted fields reserve numbers and names.
- [ ] Enum zero values are `UNSPECIFIED`.
- [ ] Large payloads use pagination, streaming, field masks, or references.
- [ ] Field masks are documented, validated, authorized, and tested when partial reads or patch updates exist.
- [ ] Field presence is understood for update/patch semantics.
- [ ] Well-known types are used for timestamps, durations, field masks, and empty messages where appropriate.
- [ ] JSON mapping behavior is tested when gateways or gRPC-Web expose the API.
- [ ] Protobuf Editions vs proto3 choice is documented for new APIs.
- [ ] Side-effecting methods have idempotency design if retryable.
- [ ] Breaking-change checks run in CI.
- [ ] Generated code is reproducible.

---

## Runtime Readiness

- [ ] Every client call has a deadline.
- [ ] `wait_for_ready` is used only for intentional methods and always with a deadline.
- [ ] Servers observe cancellation.
- [ ] Status codes are mapped consistently.
- [ ] Structured rich error details are safe, documented, and machine-readable when clients need recovery guidance.
- [ ] Metadata keys are documented and bounded.
- [ ] Interceptors handle auth, metrics, tracing, and safe logging.
- [ ] Retries are bounded and only used for safe/idempotent calls.
- [ ] Hedging is limited to selected safe methods.
- [ ] Streaming methods have backpressure and cancellation tests.
- [ ] Message size, metadata size, compression, keepalive, and concurrent-stream limits are explicit.
- [ ] Channel lifecycle is documented: reuse, shutdown, state, backoff, and resolver scheme.
- [ ] Service config examples exist for timeouts, retries, hedging, and load balancing.

---

## Security Readiness

- [ ] TLS or mTLS is configured intentionally.
- [ ] Workload identity is documented.
- [ ] JWT/OAuth metadata validation is centralized.
- [ ] Per-method/resource authorization is enforced.
- [ ] Cert rotation is automated and tested.
- [ ] Tokens and sensitive metadata are redacted from logs.
- [ ] Reflection exposure is reviewed.
- [ ] Channelz/admin debugging exposure is protected if enabled.

---

## Deployment Readiness

- [ ] Kubernetes readiness reflects gRPC serving ability.
- [ ] HTTP/2 is preserved through proxies/gateways.
- [ ] App deadlines align with proxy/mesh timeouts.
- [ ] Connection draining works during deploys.
- [ ] Health checks distinguish liveness, readiness, and per-service serving state.
- [ ] Load-balancing policy is explicit.
- [ ] Service config/xDS retry, hedging, timeout, and load-balancing policy is governed and canaried.
- [ ] Gateway mode is documented: native gRPC, gRPC-Web, or transcoding.
- [ ] Graceful shutdown flips health/readiness before terminating.
- [ ] Long-lived streams have heartbeat, max age, drain, and resume behavior.
- [ ] CORS/auth/header rules are documented for gRPC-Web.

---

## Observability Readiness

- [ ] Metrics are tagged by service, method, status, client, and version.
- [ ] Latency percentiles are tracked by method.
- [ ] Traces include client, proxy, server, and dependency spans.
- [ ] Logs include safe request context and final status.
- [ ] SLOs exist for critical methods.
- [ ] Alerts exist for `UNAVAILABLE`, `DEADLINE_EXCEEDED`, `INTERNAL`, and auth spikes.
- [ ] Active stream and slow-consumer signals exist for streaming methods.
- [ ] OTel client/server call, per-attempt, retry/hedge, and message-size metrics are reviewed.
- [ ] Channel/LB/xDS debug evidence is available during incidents.

---

## Implementation Readiness

- [ ] At least one runnable Java or Go service exists.
- [ ] A client sets deadlines and metadata.
- [ ] grpcurl smoke tests are documented.
- [ ] Status-code contract tests run.
- [ ] Docker or compose run path exists.
- [ ] Reflection and health behavior differ correctly between local/dev and production.

---

## Interview Readiness Score

Score each area from 0 to 3.

| Area | 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| proto design | cannot explain | basic messages | safe evolution | governance-level |
| runtime | basic calls | status/deadline aware | retries/streaming | incident-ready |
| security | vague | TLS/auth basics | mTLS/authz | cert/policy ops |
| deployment | local only | basic K8s | proxy/mesh aware | rollout/debug expert |
| observability | logs only | metrics | traces/SLOs | RCA-ready |
| implementation | design only | sample proto | runnable service | tested local platform |

Target score:

- 8+: practical
- 11+: senior
- 17+: MAANG-ready

---

## Final Gap Prompt

Explain this in five minutes:

```text
Design, deploy, secure, observe, evolve, and debug a payment gRPC API running on Kubernetes behind Envoy, with deadlines, retries, mTLS, contract tests, and schema governance.
```

If you cannot do that clearly, revisit the weakest checklist area.
