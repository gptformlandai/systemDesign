# Microservices API Management, Partner APIs, And Webhooks Gold Sheet

> Track: Microservices Interview Track - Intermediate Path  
> Goal: go beyond API Gateway basics into external API lifecycle, partner onboarding, quotas, webhooks, analytics, and safe deprecation.

---

## 1. Intuition

An API Gateway is the front door. API management is the whole building operation:

```text
publish -> onboard -> authenticate -> authorize -> quota -> observe -> support -> evolve -> deprecate
```

Internal microservice APIs optimize team-to-team integration. Partner APIs optimize trust,
stability, documentation, supportability, and long-term compatibility.

---

## 2. Definition

- Definition: API management is the discipline of exposing APIs safely to internal or
  external consumers through lifecycle controls, documentation, access, policy, analytics,
  support, and deprecation.
- Category: platform, integration architecture, security, product operations.
- Core idea: an API is a contract and a product surface, not just a route.

---

## 3. API Gateway vs API Management

| Concern | API Gateway | API Management |
|---|---|---|
| Routing | yes | yes, through gateway/policies |
| Auth enforcement | yes | yes, plus onboarding and keys |
| Rate limit | yes | yes, plan-based |
| Documentation | usually no | yes |
| Developer portal | usually no | yes |
| Consumer lifecycle | limited | yes |
| Analytics | basic metrics | product and consumer metrics |
| Deprecation | limited | lifecycle process |
| Support | limited | owner, tickets, SLAs |

Interview line:

```text
Gateway handles runtime edge traffic. API management handles the API lifecycle and consumer
relationship.
```

---

## 4. External API Design Checklist

For partner APIs, define:

- owner team
- business capability
- consumer type
- authentication method
- authorization model
- rate limit and quota
- request/response schema
- error shape
- idempotency behavior
- pagination/filtering/sorting
- versioning policy
- deprecation policy
- webhook behavior
- observability and support path
- SLA or support expectation

Hotel example:

```text
Partner API lets travel partners create bookings and receive booking status webhooks.
```

---

## 5. Authentication And Authorization

Common options:

| Option | Use When | Trade-Off |
|---|---|---|
| API key | simple partner identification | weak alone, rotate carefully |
| OAuth2 client credentials | machine-to-machine access | more setup, stronger lifecycle |
| JWT bearer token | delegated identity or signed claims | claim validation and rotation needed |
| mTLS | high-trust B2B integration | certificate lifecycle complexity |

Strong design:

```text
API key identifies the partner or app. OAuth2/mTLS proves identity. Service still enforces
authorization for the actual booking action.
```

Do not rely on:

- hidden endpoint URLs
- IP allowlist alone
- one shared API key for all partners
- gateway-only authorization for domain decisions

---

## 6. Rate Limits And Quotas

Rate limiting protects runtime health. Quotas protect business and partner plans.

| Control | Example |
|---|---|
| per-second rate | 100 requests/sec |
| daily quota | 1 million requests/day |
| burst limit | 500 requests for short window |
| concurrent request limit | max 50 in-flight |
| per-endpoint limit | stricter create booking limit |
| tenant/partner limit | partner-specific plan |

Design rules:

- return `429` with retry guidance
- use idempotency for write retries
- separate read and write limits
- protect expensive endpoints more aggressively
- monitor limit hits by partner
- have emergency throttles

---

## 7. Developer Portal

A useful portal contains:

- API overview
- authentication guide
- OpenAPI/protobuf/schema files
- example requests
- error codes
- sandbox credentials
- webhook setup
- changelog
- status page
- deprecation notices
- support contact

Interview line:

```text
For external APIs, documentation and onboarding are part of system design because bad
consumer behavior becomes production load and support cost.
```

---

## 8. API Analytics

Track:

- requests by consumer
- error rate by consumer
- latency by endpoint
- quota usage
- 429s
- top expensive queries
- deprecated version usage
- webhook delivery success
- support tickets by API
- business conversion by endpoint

Use analytics for:

- capacity planning
- partner support
- deprecation readiness
- abuse detection
- product decisions

---

## 9. Webhooks

Webhook means your system calls the partner when an event happens.

Hotel example:

```text
BookingConfirmed -> send webhook to partner callback URL
```

Webhook delivery design:

1. Partner registers endpoint and secret.
2. System validates URL ownership or admin approval.
3. Event occurs and is stored durably.
4. Delivery worker signs payload.
5. Worker sends POST with event ID.
6. Partner returns 2xx.
7. Worker records delivery status.
8. Retries use backoff and max attempts.
9. Failed delivery goes to DLQ/support queue.

---

## 10. Webhook Contract

Payload example:

```json
{
  "eventId": "evt_123",
  "eventType": "booking.confirmed",
  "occurredAt": "2026-07-02T10:15:30Z",
  "data": {
    "bookingId": "B123",
    "status": "CONFIRMED"
  }
}
```

Headers:

```text
X-Event-Id: evt_123
X-Signature: hmac-sha256...
X-Delivery-Attempt: 1
```

Rules:

- event ID must be stable
- receiver must process idempotently
- signature must cover timestamp and body
- do not include unnecessary PII
- keep schema backward-compatible
- document retry behavior

---

## 11. Webhook Failure Modes

| Failure | Mitigation |
|---|---|
| partner endpoint down | retry with backoff |
| partner slow | timeout and retry later |
| duplicate delivery | event ID idempotency |
| partner returns 400 | mark permanent failure |
| partner URL hijacked | registration verification and signing |
| secret leaked | rotation support |
| replay attack | timestamp and signature validation |
| payload breaks consumer | schema compatibility and versioning |

Strong answer:

```text
Webhook delivery is an async integration. I treat it like a message pipeline: durable event,
idempotent delivery, signed payload, retries, DLQ, metrics, and support visibility.
```

---

## 12. Versioning And Deprecation

Prefer compatible evolution:

- add optional response fields
- add optional request fields
- add new endpoint for different semantics
- support unknown enum values where possible
- keep old behavior during migration

Deprecation process:

1. Announce change.
2. Identify consumers using old version.
3. Provide migration guide.
4. Add dashboard for usage.
5. Block new adoption.
6. Set removal date.
7. Remove only after usage is safe.

---

## 13. Partner API Incident

Scenario:

```text
Partner creates duplicate bookings after retrying 504 responses.
```

Diagnosis:

- write endpoint lacked idempotency
- timeout result was unknown
- partner retried with new request body/key
- API docs did not define retry behavior

Fix:

- require idempotency key on writes
- return stored response for duplicate keys
- document retry rules
- add duplicate booking detection
- add partner-specific monitoring

---

## 14. Interview Question

> Design partner APIs and webhooks for a hotel booking platform.

Strong answer structure:

1. Expose partner-facing APIs through API management/gateway.
2. Use OAuth2 client credentials or mTLS plus partner identity.
3. Apply partner-specific quotas and rate limits.
4. Publish OpenAPI docs, sandbox, changelog, and error model.
5. Require idempotency keys for booking creation.
6. Deliver booking status through signed webhooks.
7. Retry webhooks with backoff and DLQ.
8. Track analytics by partner and version.
9. Use compatible evolution and deprecation windows.

---

## 15. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| API key is the only security | weak identity and rotation | OAuth2/mTLS plus scoped access |
| Gateway owns domain logic | gateway becomes monolith | domain services own decisions |
| No idempotency on writes | duplicate bookings/charges | require idempotency key |
| Webhook has no signature | spoof/replay risk | signed payload with timestamp |
| No consumer analytics | blind deprecation | track usage by consumer/version |
| Version bump for every change | too many contracts | compatible evolution first |
| No sandbox | partner tests in prod | sandbox/test environment |

---

## 16. Strong Closing Answer

```text
For partner APIs, I design beyond routes. I need authentication, authorization, quotas,
documentation, sandbox onboarding, idempotency, analytics, deprecation, and support. For
webhooks, I use durable events, signed payloads, idempotent event IDs, retry with backoff,
DLQ, and delivery visibility. That turns APIs into a reliable product surface instead of
just an exposed internal service.
```

