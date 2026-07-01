# 23. Scenario: Production gRPC Incident Debugging

## Universal Incident Frame

Use this when the symptom is unclear.

```text
who is calling what method -> what status/latency changed -> did handler run -> where is evidence -> what changed -> safe mitigation
```

---

## Step 1: Classify The Symptom

| Symptom | Likely Area |
|---|---|
| `UNAVAILABLE` | discovery, LB, TLS, proxy, server down |
| `DEADLINE_EXCEEDED` | budget consumed by proxy/server/dependency/retry |
| `UNAUTHENTICATED` | missing/invalid identity or credential |
| `PERMISSION_DENIED` | authz policy/resource denial |
| `INVALID_ARGUMENT` spike | client validation or contract change |
| `INTERNAL` spike | server bug or unhandled dependency failure |
| stream stalls | flow control, slow consumer, proxy timeout |
| successful but wrong data | schema evolution or business logic regression |

---

## Step 2: Find The Call Path

Record:

- client service/version/region
- target service/method
- request rate and status distribution
- deadline/timeout config
- auth identity
- resolver/LB/proxy path
- server version/pod/zone
- dependencies called by handler

---

## Step 3: Evidence Checklist

- client metrics and logs
- server metrics and logs
- distributed trace
- proxy/mesh access logs
- deployment/config diff
- proto diff
- health check status
- DNS/resolver output
- cert/trust-bundle state
- dependency dashboards

---

## Step 4: Mitigation Options

Choose the least risky action that targets the proven cause:

- roll back deployment
- disable bad route/retry policy
- drain bad endpoints
- restore trust bundle or cert config
- increase capacity for saturated dependency
- reduce payload/traffic temporarily
- switch clients to known-good endpoint/config
- pause rollout and protect error budget

---

## Step 5: Prevention

- method-level SLO and alerting
- schema breaking-change gates
- grpcurl smoke tests in deploy pipeline
- canary plus rollback automation
- deadline/retry policy review
- cert rotation runbook
- stream backpressure tests
- service mesh config tests

---

## Interview Sound Bite

I debug gRPC incidents by classifying status and latency, proving whether the server handler ran, tracing client-proxy-server-dependency timing, checking recent code/config/proto changes, and then applying a mitigation tied to evidence rather than guessing.