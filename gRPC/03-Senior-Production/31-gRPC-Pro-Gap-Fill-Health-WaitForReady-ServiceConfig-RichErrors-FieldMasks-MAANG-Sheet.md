# 31. gRPC Pro Gap-Fill: Health, Wait-For-Ready, Service Config, Rich Errors, Field Masks

## Why This Sheet Exists

Most gRPC tracks cover proto, stubs, status codes, and streaming. Senior interviews and production reviews often go one level deeper:

- How does health checking actually work?
- When should a client wait for a connection instead of failing fast?
- Where do retries, hedging, load balancing, and timeouts live: code, service config, xDS, or mesh?
- How should structured validation or retry guidance be returned?
- How do large response and partial-response contracts stay safe?
- Which limits prevent metadata, message size, and compression incidents?

Use this as the pro gap-fill layer after files 11-16.

---

## 1. gRPC Health Checking Protocol

gRPC has a standard health checking service commonly exposed as:

```text
grpc.health.v1.Health/Check
grpc.health.v1.Health/Watch
```

Typical serving statuses:

- `SERVING`
- `NOT_SERVING`
- `SERVICE_UNKNOWN`

Production rules:

- Liveness answers whether the process should be restarted.
- Readiness answers whether traffic should be sent to this instance.
- Per-service health is better than a single global answer when one server hosts multiple services.
- Health should flip to not-serving before shutdown so clients and load balancers can drain traffic.
- Health should not perform expensive dependency checks on every probe.

Kubernetes/Envoy/mesh checks should align with the same serving truth. A pod can be alive but not ready for a specific gRPC service.

---

## 2. Wait-For-Ready

By default, many gRPC clients fail fast when the channel cannot currently connect. `wait_for_ready` changes that behavior: the RPC waits for the channel to become ready until its deadline expires.

Use it for:

- startup races where server/discovery is briefly unavailable
- batch or background calls that can wait within a deadline
- known transient name-resolution or connection warmup windows

Avoid using it as a blanket fix for:

- user-facing calls that should fail quickly
- missing deadlines
- broken service discovery
- overloaded services
- traffic that should be shed instead of queued

Rule:

```text
wait_for_ready without a clear deadline is incident fuel.
```

---

## 3. Service Config And xDS

gRPC service config can define client behavior such as:

- method timeouts
- retry policy
- hedging policy
- load-balancing policy
- health checking
- wait-for-ready behavior in some ecosystems

xDS lets control planes such as Envoy/service mesh systems deliver dynamic routing, load balancing, circuit breaking, and retry policy.

Senior design question:

```text
Which behavior belongs in application code, client service config, xDS/mesh policy, or gateway policy?
```

Good answer:

- Business idempotency belongs in application contract and storage.
- Deadlines should be explicit at the caller boundary.
- Retry/hedging can be centrally configured only when method safety is proven.
- Mesh policy must not conflict with application deadlines.
- xDS/mesh rollouts need canaries and observability because they change client behavior without code changes.

---

## 4. Structured Rich Errors

Canonical status codes are necessary but sometimes not enough.

Structured error details can carry machine-readable recovery information, commonly with protobuf types such as:

- `google.rpc.BadRequest` for validation field violations
- `google.rpc.RetryInfo` for retry delay guidance
- `google.rpc.ResourceInfo` for resource-specific errors
- `google.rpc.ErrorInfo` for domain/category metadata
- `google.rpc.PreconditionFailure` for failed state requirements

Use structured errors when clients need programmatic recovery.

Do not include:

- stack traces
- secrets
- raw tokens
- internal hostnames
- sensitive customer data
- overly specific exploit-enabling implementation details

Interview phrase:

```text
Status code tells the class of failure; structured details tell the client how to recover safely.
```

---

## 5. Field Masks And Partial Responses

Large response messages create latency, memory, and compatibility pressure. Field masks let clients request or update specific fields.

Common uses:

- partial reads
- patch/update APIs
- response shaping for expensive fields
- avoiding multiple versions of similar methods

Example idea:

```proto
message GetCustomerRequest {
  string customer_id = 1;
  google.protobuf.FieldMask read_mask = 2;
}

message UpdateCustomerRequest {
  Customer customer = 1;
  google.protobuf.FieldMask update_mask = 2;
}
```

Rules:

- Document default fields when mask is absent.
- Validate unknown or unauthorized field paths.
- Keep expensive fields opt-in when needed.
- Do not use masks to hide broken domain boundaries.
- Make auth decisions per requested field when fields have different sensitivity.

---

## 6. Operational Limits

Production gRPC services need explicit limits.

| Limit | Why It Matters |
|---|---|
| max inbound message size | prevents memory and CPU spikes |
| max outbound message size | prevents huge responses and proxy failures |
| metadata size | prevents header abuse and token/logging problems |
| compression policy | avoids CPU spikes and compression-bomb risk |
| max concurrent streams | protects server and proxy resources |
| max stream duration | bounds long-lived stream resource use |
| keepalive policy | prevents dead connections without ping storms |

Limits should be visible in service docs, client SDK defaults, gateway/mesh config, and load tests.

---

## 7. Interview Scenario

Prompt:

```text
After a rollout, clients hang for 30 seconds and then fail with DEADLINE_EXCEEDED. The service was briefly unavailable during startup, and a mesh retry policy was recently changed.
```

Strong answer:

1. Check caller deadlines and whether `wait_for_ready` was enabled.
2. Check service config/xDS retry and hedging policy changes.
3. Confirm health/readiness transitions during startup.
4. Compare app deadline, mesh timeout, and retry budget.
5. Check whether calls queued instead of failing fast.
6. Mitigate by rolling back bad policy or narrowing `wait_for_ready` to safe methods.
7. Prevent with method-level service config review, health-drain tests, and retry/deadline canaries.

---

## 8. Pro Checklist

- [ ] Health service is implemented and distinguishes liveness/readiness/serving.
- [ ] Shutdown flips readiness before draining.
- [ ] `wait_for_ready` is used only with deadlines and method-level intent.
- [ ] Service config/xDS retry and timeout policy is reviewed against method idempotency.
- [ ] Rich errors use structured details without leaking sensitive internals.
- [ ] Field masks are documented, validated, authorized, and tested.
- [ ] Message, metadata, stream, keepalive, and compression limits are explicit.
- [ ] Mesh/gateway policy changes have canary and rollback plans.

---

## Senior Sound Bite

Production gRPC maturity includes health protocol semantics, deadline-bound wait-for-ready behavior, service config or xDS governance, structured rich errors, field masks for partial data, and explicit operational limits. These controls keep generated RPC code from becoming an invisible reliability policy.