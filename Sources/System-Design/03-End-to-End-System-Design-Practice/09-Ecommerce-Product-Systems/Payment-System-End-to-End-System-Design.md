# Payment System - End-to-End System Design

> Goal: practice one complete commerce payment system from checkout payment intent to gateway integration, idempotency, ledger, webhooks, reconciliation, refunds, and failure handling.

---

## How To Use This File

- Use this when the interview asks for payment system, checkout payment, wallet/card/UPI payment, payment gateway orchestration, or financial workflow.
- Start with payment intent, then cover authorization, capture, refunds, webhooks, ledger, reconciliation, provider failover, and compliance boundaries.
- Keep one idea sharp: payments optimize for correctness, idempotency, and auditability before raw latency.
- This complements `Payment-Workflow-End-to-End-System-Design.md`; this file is tuned for e-commerce product checkout.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Payment system focus |
|---|---|---|
| Problem understanding | Can clarify money movement | intent, auth, capture, refund, settlement, dispute |
| HLD | Can isolate financial correctness | payment orchestrator, gateway adapters, ledger, reconciliation |
| LLD | Can model state transitions | `PaymentIntent`, `PaymentAttempt`, `LedgerEntry`, `Refund` |
| Machine coding | Can implement critical path | idempotent charge, state machine, duplicate webhook guard |
| Traffic spikes | Can protect checkout | retry storms, provider outage, flash sale, webhook backlog |
| Scale | Can reason compliance/global | provider routing, regional methods, sharded ledger, audit retention |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create payment intent for an order/checkout.
- Authorize payment.
- Capture payment.
- Support one-step charge where auth and capture happen together.
- Support refunds, full and partial.
- Receive gateway webhooks/callbacks.
- Expose payment status to order service.
- Prevent duplicate charges.
- Maintain ledger/audit records.
- Reconcile internal state with gateway reports.

Optional requirements to clarify:

- Which methods are in scope: cards, UPI, netbanking, wallet, gift card, BNPL?
- Do we support split payments?
- Do we support marketplace seller payouts?
- Are recurring/subscription payments in scope?
- Is fraud/risk scoring in scope?
- Are chargebacks/disputes in scope?
- Is cross-border FX in scope?

Out of scope unless interviewer asks:

- Full PCI token vault implementation.
- Bank settlement network internals.
- Full fraud ML training pipeline.
- Tax reporting.

## 1.2 Non-Functional Requirements

Correctness:

- Never double charge for one checkout.
- Never mark order paid unless payment is confirmed or reconciled.
- Ledger entries must be immutable.
- State transitions must be legal and auditable.

Availability:

- Checkout payment APIs should be highly available.
- Provider outage should degrade via failover or pending state.

Reliability:

- Webhooks can be duplicated, delayed, or arrive out of order.
- Client/server retries must be idempotent.
- Gateway timeouts create unknown outcomes.

Security/compliance:

- Avoid storing raw card data.
- Encrypt sensitive data.
- Use tokenization.
- Keep strict access controls and audit logs.

Performance:

- Payment initiation should be fast.
- External authentication/3DS/UPI flows can take longer and must be async-friendly.

## 1.3 Constraints

- External provider behavior is not fully under our control.
- Network failure after provider charge can leave internal state stale.
- Payment callbacks can arrive before synchronous API response.
- Refunds and settlements can happen days later.
- Financial records need long retention.
- Compliance boundaries may limit data storage and access.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Payment attempts/day | 500M |
| Peak attempts/sec | 100K+ |
| Payment methods | 10+ |
| Providers | 3 to 10 |
| Refunds/day | 10M |
| Webhooks/day | 1B+ |
| Availability target | 99.99% API, correctness above availability |
| Reconciliation | hourly or daily, with near-real-time exceptions |

Back-of-the-envelope:

- `500M attempts/day` is about `5.8K/sec` average.
- Peak can be 10x or more during sales.
- Webhook volume may exceed payment attempts due to retries and status updates.
- Ledger storage grows continuously and should be partitioned.

## 1.5 Clarifying Questions To Ask

- Is this direct capture or auth-then-capture?
- What is the order state dependency on payment state?
- Which payment methods and regions are required?
- How should timeout/unknown outcomes be shown to users?
- Is provider failover allowed after a failed attempt?
- What is the refund SLA?
- What reconciliation frequency is expected?

Strong interview framing:

> I will design the payment system around a payment intent state machine, idempotent APIs, provider adapters, immutable ledger entries, webhook handling, and reconciliation. I will treat gateway timeouts as unknown until confirmed, not as simple failures.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Clients / Checkout Service
  |
  v
Payment API
  |
  +--> Payment Orchestrator
  |      |
  |      +--> Idempotency Store
  |      +--> Payment DB
  |      +--> Gateway Router
  |      +--> Ledger Service
  |      +--> Event Publisher
  |
  +--> Gateway Adapters
          +--> Provider A
          +--> Provider B
          +--> Provider C

Webhook API
  -> Webhook Verifier
  -> Event Deduplicator
  -> Payment State Updater
  -> Ledger Service
  -> Order Events

Reconciliation Jobs
  -> Provider Reports
  -> Internal Payments/Ledger
  -> Exception Queue
```

Payment flow:

```text
Checkout -> Payment API: create intent
Payment API -> Provider: authorize/capture
Provider -> Payment API: sync response or redirect/challenge
Provider -> Webhook API: async final status
Payment API -> Order Service: PaymentSucceeded/Failed/Pending
Reconciliation -> fixes ambiguous/missed updates
```

## 2.2 APIs

Create payment intent:

```http
POST /v1/payment-intents
Idempotency-Key: checkout_123_pay

{
  "orderId": "ord_1",
  "amount": 129900,
  "currency": "INR",
  "paymentMethodId": "pm_1",
  "captureMode": "IMMEDIATE"
}
```

Capture authorized payment:

```http
POST /v1/payment-intents/{paymentIntentId}/capture
Idempotency-Key: capture_123
```

Refund:

```http
POST /v1/payment-intents/{paymentIntentId}/refunds
Idempotency-Key: refund_123

{
  "amount": 50000,
  "reason": "CUSTOMER_RETURN"
}
```

Webhook:

```http
POST /v1/webhooks/providers/{providerName}

{
  "eventId": "evt_1",
  "providerPaymentId": "gw_123",
  "status": "CAPTURED",
  "amount": 129900
}
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| Payment API | validates requests and idempotency keys |
| Payment Orchestrator | owns workflow and state transitions |
| Idempotency Store | maps request key to stable response |
| Gateway Router | chooses provider by method, region, health, cost |
| Gateway Adapter | translates internal model to provider API |
| Payment DB | stores intents, attempts, refunds |
| Ledger Service | immutable debit/credit entries |
| Webhook Handler | verifies, deduplicates, updates state |
| Reconciliation Job | compares internal state against provider records |
| Event Publisher | emits payment status to order/notification systems |

### Payment State Machine

```text
CREATED
  -> REQUIRES_ACTION
  -> AUTHORIZED
  -> CAPTURED
  -> FAILED
  -> CANCELLED
  -> REFUNDED
  -> PARTIALLY_REFUNDED
  -> RECONCILING
```

Legal transitions:

| From | To |
|---|---|
| CREATED | REQUIRES_ACTION, AUTHORIZED, CAPTURED, FAILED |
| REQUIRES_ACTION | AUTHORIZED, CAPTURED, FAILED, CANCELLED |
| AUTHORIZED | CAPTURED, CANCELLED, FAILED |
| CAPTURED | PARTIALLY_REFUNDED, REFUNDED |
| RECONCILING | AUTHORIZED, CAPTURED, FAILED |

Important rule:

- A timeout should usually move to `RECONCILING` or `PENDING`, not immediately `FAILED`.

### Idempotency

Idempotency prevents duplicate charges when:

- user double-clicks pay
- client times out and retries
- checkout service retries
- gateway callback is duplicated

Store:

```text
idempotencyKey -> requestHash, paymentIntentId, response, status, expiresAt
```

Rules:

- Same key + same request returns same response.
- Same key + different request should fail.
- Idempotency key TTL must cover realistic retry windows.

### Ledger

Ledger should be append-only:

```text
LedgerEntry(
  entryId,
  paymentIntentId,
  account,
  direction,
  amount,
  currency,
  reason,
  createdAt
)
```

Example capture:

```text
Debit: customer_receivable or provider_clearing
Credit: merchant_payable or platform_revenue
```

Interview simplification:

- You do not need deep accounting in every interview.
- But say ledger entries are immutable and reconciliation compares ledger/payment/provider state.

## 2.4 Data Layer

Tables/documents:

```text
PaymentIntent(paymentIntentId, orderId, amount, currency, status, captureMode, createdAt)
PaymentAttempt(attemptId, paymentIntentId, provider, providerPaymentId, status, errorCode)
PaymentMethod(paymentMethodId, userId, tokenRef, type, metadata)
Refund(refundId, paymentIntentId, amount, status, providerRefundId)
LedgerEntry(entryId, paymentIntentId, account, direction, amount, createdAt)
IdempotencyRecord(key, requestHash, responseRef, status, expiresAt)
WebhookEvent(eventId, provider, status, processedAt)
```

Storage choices:

| Data | Storage |
|---|---|
| payment intents | relational DB with strong constraints |
| attempts/refunds | relational DB |
| idempotency | strongly consistent KV/DB |
| ledger | append-only relational/event store |
| webhook event IDs | KV/DB with TTL or durable table |
| provider reports | object storage + reconciliation DB |
| events | Kafka/PubSub-like log |

Indexes:

- `orderId -> paymentIntent`
- `paymentIntentId -> attempts/refunds/ledger`
- `providerPaymentId -> attempt`
- `idempotencyKey -> record`
- `webhook eventId -> processed marker`

## 2.5 Scalability

Partitioning:

- Shard payment intents by `paymentIntentId` or `orderId`.
- Keep all attempts/refunds for one payment close to the parent record.
- Partition ledger by time and payment ID/account.

Provider scaling:

- Route by region/payment method.
- Use provider health checks.
- Use circuit breakers.
- Use token buckets to respect provider rate limits.

Webhook scaling:

- Verify request.
- Deduplicate by provider event ID.
- Enqueue for processing.
- Update payment state idempotently.
- Store failed webhook attempts for replay.

## 2.6 Performance

Latency budget:

| Step | Target |
|---|---|
| idempotency check | 5 to 20 ms |
| payment DB write | 5 to 30 ms |
| gateway call | 100 ms to several seconds |
| payment API excluding gateway | under 100 ms typical |
| webhook acknowledgement | fast, often enqueue and return |

Performance rules:

- Avoid holding DB locks while calling external provider.
- Persist attempt before calling provider.
- Use async processing for webhooks and reconciliation.
- Do not retry provider calls aggressively on ambiguous failures.

## 2.7 Async Systems

Events:

| Event | Consumers |
|---|---|
| PaymentIntentCreated | analytics, risk |
| PaymentAuthorized | order service |
| PaymentCaptured | order service, ledger, notification |
| PaymentFailed | order service, notification |
| RefundSucceeded | order service, finance |
| PaymentNeedsReconciliation | operations, retry worker |

Reliability pattern:

- Write payment state and outbox event in one DB transaction.
- Event relay publishes from outbox.
- Consumers are idempotent.

## 2.8 Safety And Failure Handling

| Failure | Handling |
|---|---|
| duplicate client request | idempotency key returns same result |
| provider timeout | mark pending/reconciling, query provider later |
| provider failed response | mark failed if definitive |
| webhook duplicate | ignore via event ID |
| webhook out of order | apply only legal state transitions |
| DB write fails after provider success | reconcile via provider ID |
| refund duplicate | refund idempotency key |
| provider outage | circuit breaker, route alternate provider if safe |
| ledger write fails | block final success or retry with outbox depending design |

## 2.9 Observability

Metrics:

- payment attempt rate
- provider success/failure/timeout rate
- payment method success rate
- idempotency hit rate
- duplicate webhook count
- pending/reconciling payment count
- refund success rate
- ledger mismatch count
- provider latency

Logs:

- `requestId`
- `orderId`
- `paymentIntentId`
- `attemptId`
- `provider`
- `providerPaymentId`
- `idempotencyKey`

Alerts:

- provider timeout spike
- payment success rate drop
- pending payments above threshold
- webhook backlog
- reconciliation mismatch spike
- ledger write failure

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| one provider | simpler integration | lower resilience |
| multi-provider | failover and routing | complex reconciliation |
| sync payment response | simple UX | sensitive to gateway latency |
| async payment finalization | handles slow methods | more states |
| strict state machine | prevents corruption | more code |
| eventual order update | resilient | user may see pending state |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
PaymentIntent
PaymentAttempt
PaymentStatus
PaymentMethod
GatewayProvider
GatewayResponse
Refund
LedgerEntry
IdempotencyRecord
WebhookEvent
ReconciliationJob
```

## 3.2 OOP Fundamentals

Encapsulation:

- `PaymentIntent` owns legal status transitions.
- `PaymentService` owns idempotent payment creation.
- `LedgerService` owns immutable ledger appends.

Polymorphism:

- `PaymentGateway` interface supports multiple providers.
- `PaymentMethodHandler` supports card, UPI, wallet, gift card.

Composition:

- `PaymentOrchestrator` composes idempotency store, gateway router, payment repository, ledger, and event publisher.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| State | payment lifecycle |
| Strategy | provider routing and payment method handling |
| Adapter | gateway-specific APIs |
| Repository | payment persistence |
| Outbox | reliable event publication |
| Command | refund/capture operations |
| Circuit Breaker | provider outage protection |

## 3.4 Sequence Diagram

Successful direct capture:

```text
CheckoutService
  -> PaymentService: createAndCapture(orderId, amount, idempotencyKey)
PaymentService
  -> IdempotencyStore: check key
  -> PaymentRepo: create intent + attempt
  -> GatewayAdapter: charge
  -> PaymentRepo: mark captured
  -> LedgerService: append entries
  -> Outbox: PaymentCaptured
  -> CheckoutService: success
```

Timeout:

```text
Gateway call times out
  -> PaymentRepo: mark RECONCILING
  -> ReconciliationJob: query provider
  -> PaymentRepo: update definitive status
  -> Outbox: publish final event
```

## 3.5 Edge Cases

- User retries after browser timeout.
- Same idempotency key with different amount.
- Provider charges customer but response is lost.
- Webhook says captured before sync response returns.
- Refund requested before capture.
- Partial refund exceeds captured amount.
- Provider sends duplicate webhook.
- Provider status differs from internal status.
- Ledger event published twice.
- Payment succeeds after order expired.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
models/
  payment_intent.py
  payment_attempt.py
  ledger_entry.py
services/
  payment_service.py
  gateway_router.py
  ledger_service.py
repositories/
  payment_repository.py
  idempotency_store.py
```

## 4.2 Core Logic Implementation

Idempotent payment simulation:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock


class PaymentStatus(Enum):
    CREATED = "CREATED"
    CAPTURED = "CAPTURED"
    FAILED = "FAILED"
    RECONCILING = "RECONCILING"


@dataclass
class PaymentIntent:
    payment_id: str
    order_id: str
    amount: int
    status: PaymentStatus
    provider_ref: str | None = None


class Gateway:
    def __init__(self, mode: str) -> None:
        self.mode = mode

    def charge(self, payment_id: str, amount: int) -> tuple[str, str | None]:
        if self.mode == "success":
            return "CAPTURED", f"gw_{payment_id}"
        if self.mode == "timeout":
            return "TIMEOUT", None
        return "FAILED", None


class PaymentService:
    def __init__(self, gateway: Gateway) -> None:
        self.gateway = gateway
        self.payments: dict[str, PaymentIntent] = {}
        self.idempotency: dict[str, str] = {}
        self.request_hashes: dict[str, tuple[str, int]] = {}
        self.lock = Lock()

    def charge(self, idempotency_key: str, order_id: str, amount: int) -> PaymentIntent:
        request_hash = (order_id, amount)

        with self.lock:
            if idempotency_key in self.idempotency:
                if self.request_hashes[idempotency_key] != request_hash:
                    raise ValueError("idempotency key reused with different request")
                return self.payments[self.idempotency[idempotency_key]]

            payment_id = f"pay_{len(self.payments) + 1}"
            payment = PaymentIntent(payment_id, order_id, amount, PaymentStatus.CREATED)
            self.payments[payment_id] = payment
            self.idempotency[idempotency_key] = payment_id
            self.request_hashes[idempotency_key] = request_hash

        status, provider_ref = self.gateway.charge(payment_id, amount)

        with self.lock:
            if status == "CAPTURED":
                payment.status = PaymentStatus.CAPTURED
                payment.provider_ref = provider_ref
            elif status == "FAILED":
                payment.status = PaymentStatus.FAILED
            else:
                payment.status = PaymentStatus.RECONCILING

            return payment
```

What this demonstrates:

- First request creates one payment intent.
- Retried request with same key returns the same payment.
- Reusing key with a different amount is rejected.
- Timeout becomes `RECONCILING`, not blindly failed.

## 4.3 Testing Thinking

Test cases:

- Same idempotency key returns same payment.
- Same idempotency key with different amount fails.
- Successful gateway response marks captured.
- Gateway timeout marks reconciling.
- Duplicate webhook does not create duplicate ledger entry.
- Refund cannot exceed captured amount.
- Illegal state transition is rejected.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Festival sale checkout surge.
- Provider latency spike.
- Provider outage.
- Webhook retry storm.
- Client retry storm after timeouts.
- Refund batch after failed campaign.

## 5.2 Immediate Response

- Enforce idempotency on all write operations.
- Use provider circuit breakers.
- Rate limit retrying clients.
- Queue webhook processing.
- Mark ambiguous payments reconciling.
- Scale read/status endpoints separately.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| provider down | route alternate provider if safe |
| all providers down | show payment unavailable |
| webhook backlog | accept and queue, process async |
| reconciliation backlog | prioritize high-value/old pending payments |
| ledger issue | pause final paid marking if ledger is required |

## 5.4 Spike Interview Answer

> During payment spikes, I would prioritize correctness. I would make all writes idempotent, use provider circuit breakers, avoid aggressive retries, and move ambiguous outcomes to reconciliation. I would scale webhook ingestion separately from payment state updates and make order service consume idempotent payment events.

---

# 6. Global Scale

## 6.1 Regional Concerns

- Payment methods are region-specific.
- Providers have regional availability and costs.
- Compliance and data residency may restrict storage.
- FX and settlement can differ by country.
- Ledger partitioning must preserve auditability.

## 6.2 Architecture

```text
Regional Payment API
  -> regional provider router
  -> regional payment DB
  -> regional ledger partition
  -> global analytics/reconciliation lake
```

Rules:

- Keep payment intent in one home region.
- Route providers by method, region, health, and cost.
- Avoid active-active writes for the same payment.
- Replicate read-only status if needed.

## 6.3 Interview Answer

> I would keep each payment intent owned by one region and use regional provider routing. Global reads can be replicated, but payment state transitions and ledger writes should have one clear owner. Provider reports feed reconciliation jobs, and any mismatch is resolved through audited state transitions.

---

# 7. Final Interview Playbook

Start with:

> Payments are correctness-first. The main risks are duplicate charges, unknown provider outcomes, and state mismatch between order, payment, ledger, and provider.

Then cover:

1. Payment intent lifecycle.
2. Idempotent create/capture/refund APIs.
3. Gateway adapter and routing.
4. State machine and legal transitions.
5. Webhook deduplication.
6. Immutable ledger.
7. Reconciliation.
8. Provider outages and retry storms.

Common traps:

- Treating timeout as failure.
- Missing idempotency.
- Updating order before payment is definitive.
- Ignoring duplicate/out-of-order webhooks.
- Calling external provider while holding DB locks.
- Forgetting reconciliation.

---

# 8. Fast Recall Rules

- Payments are correctness-first.
- Idempotency is mandatory for charge/refund.
- Timeout means unknown.
- Webhooks are duplicated, delayed, and out of order.
- Ledger entries should be immutable.
- Reconciliation is not optional.
- Do not store raw card data.
- Use provider adapters and circuit breakers.
- Order consumes payment events idempotently.
- One payment intent should have one clear state owner.

