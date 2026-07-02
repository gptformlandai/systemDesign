# 34. Datadog Dynamic Instrumentation: Live Debugging With Probes

## Goal

Understand how Dynamic Instrumentation lets you add observability to running code without changing source code, rebuilding, or redeploying.

---

## Mental Model

Traditional debugging:

```text
add log line -> build -> deploy -> reproduce issue -> inspect log
```

Dynamic instrumentation:

```text
add probe in Datadog -> running app emits new debug data -> inspect safely -> remove probe
```

It is production-safe debugging when redeploying is too slow or risky.

---

## Why It Exists

Production bugs often happen when:

- You do not have enough logs.
- You cannot reproduce locally.
- Redeploying with extra logs takes too long.
- Adding broad debug logging would be expensive.
- The issue occurs only for specific users, inputs, or rare states.

Dynamic Instrumentation adds targeted visibility at runtime.

---

## Probe Types

| Probe | Purpose | Example |
|---|---|---|
| Log probe | Emit a dynamic log line | "cartId={cart.id}, total={total}" |
| Metric probe | Emit a metric from code execution | count calls to risky branch |
| Span probe | Add context to traces | tag trace when feature flag enabled |
| Snapshot probe | Capture local variables/stack context | inspect state without full debugger |

Exact probe support depends on language and Datadog library version.

---

## How It Works

```text
1. Datadog tracing library runs inside the application.
2. Engineer creates a probe in Datadog for a class/method/line.
3. Probe configuration is delivered to the tracer.
4. When code reaches that location, the probe captures approved data.
5. Captured data is sent as logs, metrics, spans, or snapshots.
6. Engineer removes the probe after investigation.
```

No source rebuild is required.

---

## Example Use Case

```text
Bug:
  0.1% of checkout requests calculate discount incorrectly.

Existing logs:
  "discount applied"
  but no coupon, rule, cart value, or feature flag state.

Dynamic probe:
  Add log probe to DiscountService.applyCoupon().

Captured fields:
  coupon.code
  cart.total
  customer.segment
  featureFlags.newDiscountEngine
  result.discountAmount
```

Now the rare bad state becomes visible without deploying a new build.

---

## Probe Design Checklist

Before adding a probe:

- Is the probe scoped to the smallest useful code location?
- Is it conditional?
- Does it avoid PII, tokens, secrets, and payment data?
- Does it have a short expiration?
- Is rate limiting or sampling enabled where available?
- Is the owning team aware?
- Is this safe for a hot code path?

---

## Conditional Probe Examples

```text
Only capture when:
  customer.segment == "enterprise"
  coupon.code startsWith "SUMMER"
  result.discountAmount > cart.total * 0.5
  featureFlags.newDiscountEngine == true
  request.region == "ap-south-1"
```

Conditional probes reduce cost and noise.

---

## Dynamic Logs vs Normal Logs

| Aspect | Normal Log | Dynamic Log Probe |
|---|---|---|
| Added in source code | Yes | No |
| Requires deploy | Yes | No |
| Best for permanent observability | Yes | No |
| Best for rare debugging | Sometimes | Yes |
| Risk if forgotten | Lower if reviewed | Higher if ungoverned |

Dynamic probes are investigation tools, not substitutes for well-designed permanent logs.

---

## RBAC And Safety

Only trusted roles should create probes in production.

Recommended controls:

```text
Role: Dynamic Instrumentation Admin
  - create/update/delete probes
  - production access allowed

Role: Dynamic Instrumentation Viewer
  - inspect probes and captured data
  - no write access

Policy:
  - production probes expire automatically
  - sensitive fields blocked
  - probe changes audited
  - emergency probes reviewed after incident
```

---

## Failure Modes

| Failure | User Impact | Mitigation |
|---|---|---|
| Probe on hot path emits too much | Cost/noise spike | Add condition and rate limit |
| Probe captures sensitive data | Compliance risk | Redaction and field denylist |
| Wrong line/method selected | No useful data | Validate with staging or lower environment |
| Probe left active too long | Ongoing overhead | Expiration policy |
| Too many engineers can add probes | Governance risk | RBAC and audit trail |

---

## Practical Question

> A production-only bug happens once every 10,000 requests. Existing logs do not show enough request state, and redeploying will take 3 hours. How can Datadog help?

---

## Strong Answer

I would use Dynamic Instrumentation to add a temporary conditional probe at the suspected method. The probe would capture only safe fields needed to distinguish good and bad requests, such as feature flag state, rule ID, sanitized request attributes, and computed output. I would scope it to the affected service/env/version and add a condition so it only fires for the rare branch.

After collecting evidence, I would remove the probe and convert any generally useful insight into a permanent structured log, metric, or trace tag through code review.

---

## Interview Sound Bite

Dynamic Instrumentation is live, targeted observability for running code. Use it when production lacks the exact log or metric you need, but govern it tightly because it can expose sensitive data or create noisy telemetry if used carelessly.
