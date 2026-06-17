# Payment Workflow - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For payments, always prioritize financial correctness, idempotency, and reconciliation over raw latency gains.

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

| Layer | Interview signal | Payment workflow focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | checkout, authorization, capture, refunds, settlement, disputes |
| HLD | Can design scalable systems | payment orchestration, gateway adapters, ledger, risk, reconciliation |
| LLD | Can model maintainable components | `PaymentIntent`, `Transaction`, `LedgerEntry`, `ReconciliationJob` |
| Machine coding | Can implement critical path | idempotent payment API, state transitions, retry guards |
| Traffic spikes | Can protect production | flash sales, gateway outages, retry storms, reconciliation backlog |
| Billion users | Can reason at global scale | multi-region routing, sharded ledgers, provider failover, compliance controls |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create payment intent for checkout.
- Authorize and capture funds (one-step or two-step flow).
- Support refunds (full and partial).
- Track payment lifecycle states.
- Trigger settlement and maintain reconciliation records.
- Support merchant webhooks for status updates.
- Prevent duplicate charges via idempotency.

Optional requirements to clarify:

- Which payment methods are in scope (cards, UPI, netbanking, wallets)?
- Is split payment/marketplace escrow needed?
- Should recurring payments/subscriptions be supported?
- Is dispute/chargeback management in scope?
- Are cross-border and FX conversion in scope?

Out of scope unless interviewer asks:

- Full KYC onboarding flow.
- End-to-end fraud model internals.
- Tax filing/legal reporting pipelines.

## 1.2 Non-Functional Requirements

Correctness:

- No double charge.
- Strong consistency for core payment state transitions.
- Immutable financial records (ledger/audit logs).

Reliability:

- High availability on checkout APIs.
- Graceful handling of provider failures/timeouts.
- Idempotent retries across client/server/provider boundaries.

Performance:

- Low latency for checkout initiation.
- Fast status visibility for user and merchant.

Security/compliance:

- PCI-aware boundaries and tokenization.
- Encryption in transit and at rest.
- Robust auditability and access controls.

## 1.3 Constraints

- External gateways can be slow or intermittently unavailable.
- Network failures create ambiguous outcomes.
- Money movement requires stronger correctness than typical CRUD systems.
- Settlement timelines differ by provider and geography.
- Regulatory constraints vary by region.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| Paying users/day | 100 million |
| Payment attempts/day | 2 billion |
| Peak attempts/sec | 300K+ |
| Refunds/day | 20 million |
| Availability target | 99.99% checkout API |
| P95 payment-init latency | under 300 ms (excluding external challenge flows) |

Back-of-the-envelope:

- `2B attempts/day` is about `23K/sec` average globally.
- Peak can be 10x during campaigns.
- Even a small duplicate-rate bug can have major financial impact.
- Ledger and reconciliation storage grows quickly and must be retention-aware.

## 1.5 Clarifying Questions To Ask

- What lifecycle model is expected (authorize+capture vs direct capture)?
- Is eventual consistency acceptable for user-visible status updates?
- What SLAs exist for merchant webhooks?
- How should ambiguous gateway timeout states be resolved?
- How frequently is reconciliation run?
- What compliance boundaries are mandatory (PCI, regional data residency)?

Strong interview framing:

> I will design payment workflow as a state-machine-driven, idempotent system with immutable ledger writes, resilient gateway integration, and reconciliation to resolve eventual inconsistencies.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Checkout request:
Client/App
  -> API Gateway
  -> Payment Orchestrator
  -> Risk + Rule checks
  -> Gateway Adapter
  -> Provider
  -> Payment State Store + Ledger
  -> Webhook/Event to merchant

Back-office flow:
Provider settlement files/events
  -> Reconciliation Service
  -> Mismatch queue and repair workflows
```

Recommended architecture:

```text
Client / Merchant Backend
          |
          v
+-----------------------+
| API Gateway           |
+-----------+-----------+
            |
            v
+-----------------------+      +----------------------+
| Payment Orchestrator  |<---->| Idempotency Store    |
+-----+-----------+-----+      +----------------------+
      |           |
      v           v
+-----------+   +----------------------+
| Risk Svc  |   | Payment Gateway Adpt |
+-----+-----+   +----------+-----------+
      |                    |
      v                    v
+-----------+   +----------------------+
| State DB  |   | External Providers   |
+-----+-----+   +----------+-----------+
      |                    |
      v                    v
+-----------------------+  +----------------------+
| Ledger Service        |  | Webhook Dispatcher   |
+-----------+-----------+  +----------+-----------+
            |                         |
            v                         v
      +----------------------+  +----------------------+
      | Reconciliation Svc   |  | Merchant Consumers   |
      +----------------------+  +----------------------+
```

Request flow for authorize + capture:

1. Merchant creates payment intent with idempotency key.
2. Orchestrator validates request and checks idempotency store.
3. Risk and policy checks run.
4. Adapter calls external provider for authorization.
5. Authorization result persisted with immutable state transition.
6. Capture is triggered based on flow (immediate or delayed).
7. Ledger entries are written.
8. Merchant gets webhook updates.

## 2.2 APIs

### Create Payment Intent

```http
POST /v1/payments/intents
Authorization: Bearer <token>
Idempotency-Key: 9f3c...
Content-Type: application/json

{
  "merchantId": "m_1",
  "customerId": "u_77",
  "amount": 4999,
  "currency": "USD",
  "paymentMethod": "CARD",
  "captureMode": "AUTO"
}
```

### Authorize Payment

```http
POST /v1/payments/{paymentIntentId}/authorize
```

### Capture Payment

```http
POST /v1/payments/{paymentIntentId}/capture
Content-Type: application/json

{ "amount": 4999 }
```

### Refund Payment

```http
POST /v1/payments/{paymentIntentId}/refunds
Content-Type: application/json

{ "amount": 1000, "reason": "CUSTOMER_REQUEST" }
```

### Payment Status

```http
GET /v1/payments/{paymentIntentId}
```

Important points:

- All mutating APIs must support idempotency keys.
- Webhooks must be signed and replay-safe.
- Status may include transient pending states due to provider uncertainty.

## 2.3 Core Components

Think of a payment workflow as a correctness-first state machine wrapped around unreliable external providers:

| Plane | What it handles | Main goal |
|---|---|---|
| Control plane | payment intent state, idempotency, routing, risk | prevent invalid or duplicate money movement |
| Provider plane | gateway API calls, callbacks, timeouts, failover | normalize external uncertainty |
| Financial truth plane | ledger, settlement, reconciliation, audit | prove what money moved and repair mismatches |
| Async plane | webhooks, retries, reconciliation jobs | decouple slow/retryable work from checkout |

The key interview idea: provider APIs can timeout or lie by omission, but your internal state machine and ledger must remain consistent and auditable.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| API Gateway | merchant auth, request validation, rate limits | payment state transitions | checkout API QPS |
| Payment Orchestrator | workflow state machine, auth/capture/refund coordination | provider SDK details | payment operation QPS |
| Idempotency Service | retry-safe canonical responses | business state itself | mutating request QPS |
| Risk/Policy Service | fraud checks, compliance rules, method eligibility | ledger posting | checkout/risk QPS |
| Provider Routing Service | choose gateway/provider by health/cost/region | final payment truth | provider health and routing decisions |
| Gateway Adapter Layer | provider-specific API normalization | business lifecycle decisions | provider calls/sec |
| Payment State Store | current intent/attempt/refund state | immutable accounting truth | state writes/reads |
| Ledger Service | append-only financial records | provider communication | ledger entries/sec |
| Outbox/Event Stream | reliable event handoff | external provider state | event volume |
| Reconciliation Service | compare internal records vs provider settlement | live checkout decisions | settlement batch size |
| Webhook Service | merchant event delivery and retries | core payment commit | webhook volume |
| Audit/Compliance | traceability, access logs, retention | transaction mutation | audit volume |

### API Gateway

Why it exists:

- Payment APIs are sensitive and must enforce merchant identity, schema validation, and rate limits.
- Attackers and buggy clients can cause duplicate charges or retry storms if not controlled.

Core responsibilities:

- Authenticate merchants/clients.
- Validate request shape and idempotency header presence.
- Apply merchant/user/IP rate limits.
- Route operations to Payment Orchestrator.
- Attach request IDs and audit context.

What it should avoid:

- Do not decide payment state transitions.
- Do not call providers directly.
- Do not write ledger entries.

Interview signal:

> Gateway is the security and traffic boundary; Payment Orchestrator owns money movement decisions.

### Payment Orchestrator

Why it exists:

- Payment flows have strict lifecycle rules.
- Authorization, capture, refund, cancellation, and failure states must be coordinated safely.

Core responsibilities:

- Load and validate `PaymentIntent`.
- Enforce legal state transitions.
- Coordinate idempotency, risk checks, routing, provider calls, state updates, ledger writes, and event emission.
- Represent ambiguous provider results as `PENDING`/`UNKNOWN`, not fake success/failure.
- Ensure retries are safe.

Typical state model:

```text
CREATED
  -> AUTHORIZING
  -> AUTHORIZED
  -> CAPTURING
  -> CAPTURED
  -> REFUNDING
  -> REFUNDED / PARTIALLY_REFUNDED

CREATED/AUTHORIZING/CAPTURING
  -> FAILED
  -> PENDING_PROVIDER_CONFIRMATION
```

Failure behavior:

- Provider timeout after request: mark pending/unknown and retry inquiry/idempotent provider operation.
- Capture requested before auth: reject invalid transition.
- Concurrent capture/refund: use optimistic locking or state version checks.

Interview signal:

> Payment Orchestrator is the state-machine brain. It never guesses provider outcomes and never allows invalid transitions.

### Idempotency Service

Why it exists:

- Clients, merchants, and internal workers retry after timeouts.
- Duplicate payment operations can double-charge users.

Core responsibilities:

- Map `(merchantId, idempotencyKey, operation)` to canonical request fingerprint and result.
- Return the original response for duplicate retries.
- Reject same key with different payload.
- Store enough state to survive client retry windows.

Scope examples:

| Operation | Idempotency scope |
|---|---|
| create intent | merchant + key |
| authorize | paymentIntent + operation + key |
| capture | paymentIntent + capture amount + key |
| refund | paymentIntent + refund amount + key |

Failure behavior:

- Same request retried: return existing result.
- Same key, different amount/currency: reject conflict.
- Store unavailable: fail safely for mutating operations.

Interview signal:

> Idempotency turns retry storms into one canonical money movement.

### Risk and Policy Service

Why it exists:

- Some payments should be blocked, challenged, limited, or routed differently.
- Compliance and fraud rules vary by merchant, geography, payment method, and amount.

Core responsibilities:

- Evaluate fraud/risk score.
- Enforce limits, currency support, region rules, and payment-method eligibility.
- Decide whether challenge/3DS/manual review is required.
- Feed provider routing decisions.

Failure behavior:

- Risk service down: fail closed for high-risk flows or allow low-risk cached policy by product decision.
- Policy config missing: reject or route to manual review rather than charge blindly.

Interview signal:

> Risk/policy is separate from orchestration so payment state remains clean while rules evolve independently.

### Provider Routing Service

Why it exists:

- Different providers have different cost, success rate, method support, region coverage, and outages.
- Multi-provider routing improves resilience but adds complexity.

Core responsibilities:

- Choose provider based on method, country, currency, merchant, cost, health, and success rate.
- Avoid providers with open circuit breakers.
- Support fallback chains when safe.
- Record routing decision for audit and reconciliation.

Failure behavior:

- Provider unhealthy: route to alternate provider if method/tokenization allows.
- No safe provider available: fail gracefully or keep intent pending.
- Split-brain routing risk: persist selected provider before external call.

Interview signal:

> Provider routing is a strategy decision; once a provider transaction starts, its references must be tracked carefully.

### Gateway Adapter Layer

Why it exists:

- Providers expose different APIs, error codes, auth, idempotency semantics, and callback formats.
- Business logic should not depend directly on provider SDKs.

Core responsibilities:

- Normalize authorize/capture/refund/inquiry APIs.
- Translate provider errors into common categories: retryable, terminal, pending, rate-limited.
- Attach provider idempotency keys.
- Persist provider transaction IDs.
- Implement provider-specific timeout and circuit breaker behavior.

Failure modes:

| Provider behavior | Internal handling |
|---|---|
| declined | terminal failure state |
| timeout | pending/unknown + inquiry/retry |
| accepted but callback delayed | pending until callback/reconciliation |
| duplicate callback | idempotent callback processing |
| provider rate limit | backoff/circuit breaker/failover |

Interview signal:

> Provider adapters isolate external uncertainty and convert it into internal payment states.

### Payment State Store

Why it exists:

- The system needs a current view of each payment intent, attempt, capture, refund, and provider reference.
- State transitions need transactional protection.

Core responsibilities:

- Store payment intent lifecycle state.
- Store payment attempts and provider transaction references.
- Store refunds and capture records.
- Support optimistic locking/version checks.
- Support status reads for clients and merchants.

Consistency requirement:

- Core payment state should use strong transactional guarantees.
- Do not rely on eventually consistent caches for mutation decisions.

Failure behavior:

- State DB write fails before provider call: do not call provider.
- Provider call succeeds but state update fails: recovery/inquiry/reconciliation workflow must repair.

Interview signal:

> Payment State Store tracks lifecycle; Ledger tracks money truth. Keep both explicit.

### Ledger Service

Why it exists:

- Financial truth must be immutable and auditable.
- You should not rewrite history when corrections happen.

Core responsibilities:

- Write append-only ledger entries for auth, capture, refund, fee, reversal, chargeback, and adjustment events.
- Enforce double-entry or balanced accounting rules where applicable.
- Link entries to payment intent/provider transaction IDs.
- Prevent mutation of historical entries.

Ledger rule:

```text
Never update old money movement rows to "fix" them.
Append compensating entries.
```

Failure behavior:

- Ledger write fails after state update: use transaction/outbox pattern or block final success until ledger durable.
- Duplicate ledger event: dedup by payment operation ID.

Interview signal:

> Ledger is the financial source of truth. Payment status is operational; ledger is accounting truth.

### Outbox / Event Stream

Why it exists:

- Payment state changes must reliably trigger webhooks, analytics, reconciliation, and audit.
- Directly calling downstream systems inside the transaction creates fragile coupling.

Core responsibilities:

- Store state-change events in an outbox transactionally with payment state.
- Publish events to stream after commit.
- Let webhook/reconciliation/reporting consumers process independently.
- Support replay.

Failure behavior:

- Publisher crash: outbox scanner republishes.
- Consumer duplicate: idempotent event handling by event ID.

Interview signal:

> Outbox prevents the classic bug where payment state commits but downstream notifications/events are lost.

### Reconciliation Service

Why it exists:

- Provider callbacks can be late, missing, duplicated, or wrong.
- Settlement files are the external source used to verify actual money movement.

Core responsibilities:

- Ingest provider settlement files/events.
- Match provider transactions to internal intents/ledger entries.
- Detect missing, duplicated, reversed, or amount-mismatched records.
- Create mismatch queues and repair workflows.
- Append correction/adjustment ledger entries.

Mismatch examples:

| Mismatch | Handling |
|---|---|
| provider captured but internal state pending | update state and ledger after verification |
| internal captured but provider missing | investigate/retry inquiry/manual review |
| amount mismatch | create reconciliation case and compensating entry if confirmed |
| duplicate settlement row | dedup by provider transaction ID |

Interview signal:

> Reconciliation is how payment systems recover from external uncertainty. It is not optional.

### Webhook Service

Why it exists:

- Merchants need reliable status updates, but merchant endpoints are often slow or down.
- Webhook delivery must not block payment state commits.

Core responsibilities:

- Send signed webhook events to merchants.
- Retry with exponential backoff.
- Include stable event IDs for merchant dedup.
- Provide dashboard/API for webhook delivery attempts.
- Move permanently failing deliveries to DLQ/manual attention.

Failure behavior:

- Merchant endpoint down: retry later without changing payment state.
- Duplicate webhook: merchant dedups by event ID.
- Secret rotation: sign using active secret version and support transition period.

Interview signal:

> Webhooks are reliable async notifications, not part of the core payment transaction.

### Audit and Compliance Layer

Why it exists:

- Payment systems require strong traceability and access control.
- Debugging payment disputes needs full request/provider/state/ledger history.

Core responsibilities:

- Record who initiated actions and when.
- Store provider request/response references safely.
- Protect PII/PCI boundaries and tokens.
- Support retention and legal/compliance access patterns.

Interview signal:

> Audit is not decorative in payments; it is required to explain and prove financial behavior.

### How The Components Work Together

Authorize/capture path:

```text
Gateway -> Idempotency -> Orchestrator -> Risk/Policy -> Provider Routing -> Adapter -> State Store -> Ledger -> Outbox -> Webhook
```

Uncertainty repair path:

```text
Provider timeout/callback/settlement -> Status/Reconciliation -> State correction -> Ledger compensating entry -> Merchant webhook
```

One-stop interview answer:

> I design payments as a strict state machine with idempotency on every mutating operation, provider adapters for external uncertainty, an immutable ledger for financial truth, outbox events for reliable async side effects, and reconciliation to repair provider mismatches. Latency matters, but correctness, auditability, and no duplicate charges matter more.

## 2.4 Data Layer

### Core Data Models

Payment intent:

```json
{
  "paymentIntentId": "pi_129",
  "merchantId": "m_1",
  "customerId": "u_77",
  "amount": 4999,
  "currency": "USD",
  "state": "AUTHORIZED",
  "createdAt": "2026-06-17T10:00:00Z"
}
```

Ledger entry:

```json
{
  "entryId": "le_88",
  "paymentIntentId": "pi_129",
  "type": "CAPTURE",
  "debitAccount": "customer_funds",
  "creditAccount": "merchant_settlement",
  "amount": 4999,
  "currency": "USD",
  "createdAt": "2026-06-17T10:00:03Z"
}
```

Reconciliation record:

```json
{
  "reconId": "rc_77",
  "paymentIntentId": "pi_129",
  "providerTxnId": "gtw_991",
  "status": "MATCHED",
  "checkedAt": "2026-06-18T01:00:00Z"
}
```

### Store Choices

| Data type | Candidate store | Why |
|---|---|---|
| Payment state | relational DB | strong transactional guarantees |
| Ledger | append-only relational/log store | immutable financial history |
| Idempotency keys | Redis/KV with TTL | fast dedup checks |
| Webhook queue | durable queue/stream | retry and reliability |
| Reconciliation artifacts | object store + operational DB | batch compare and repairs |

### Partitioning

- Partition by merchant ID and payment intent ID hash.
- Keep ledger partitioning aligned with account boundaries.
- Isolate high-volume merchants to avoid hotspots.

### Replication

- Multi-AZ synchronous replication for core payment state.
- Asynchronous cross-region replication with strict DR rules.
- Strong backup and point-in-time recovery.

## 2.5 Scalability

### Horizontal Scaling

- Stateless orchestrator and adapter services scale independently.
- Queue-based decoupling for non-critical downstream tasks.
- Separate autoscaling profiles for checkout and back-office flows.

### Provider Routing

- Route by cost, latency, success rate, and compliance constraints.
- Maintain provider health score and circuit breakers.
- Fallback chains for critical methods.

### Tenant Isolation

- Per-merchant quotas and throttling.
- Priority handling for high-value/critical payment classes.

## 2.6 Performance

### Caching Strategy

| Cache layer | What it stores | TTL |
|---|---|---:|
| Merchant config cache | routing/rules/limits | short |
| Gateway health cache | provider latency/error profile | short |
| Idempotency cache | hot recent keys | policy-based |
| Read status cache | payment status lookups | short |

### Latency Budget Example

| Stage | Target |
|---|---:|
| API auth + validation | 10-30 ms |
| idempotency + state lookup | 10-40 ms |
| risk and policy checks | 20-80 ms |
| provider round-trip | 100-300 ms typical |

### Optimization Rules

- Keep checkout path small and deterministic.
- Push analytics to async pipelines.
- Avoid heavyweight synchronous reconciliation checks.

## 2.7 Async Systems

Use streams/queues for:

- payment status events
- webhook dispatch
- settlement ingestion
- reconciliation jobs
- refunds and dispute workflows

Queue notes:

- At-least-once processing with idempotent consumers.
- DLQ for persistent webhook/provider errors.
- Controlled retries to avoid storm amplification.

## 2.8 Reliability

### Retry and Idempotency

- Idempotency keys on all mutating APIs.
- Retries with same key return canonical result.
- Distinguish retry-safe errors from terminal declines.

### Circuit Breakers

- Per-provider and per-operation breakers.
- Dynamic failover when provider error rates spike.
- Short-circuit non-critical operations under stress.

### Failover

- Region failover playbooks for API and queue layers.
- Outbox/replay for webhook consistency.
- Reconciliation-driven correction for eventual mismatches.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Payment consistency | strong state transitions | eventual status convergence | correctness vs speed |
| Provider strategy | single gateway | multi-gateway routing | simplicity vs resilience |
| Ledger model | inline sync write | async journal write | strict durability vs lower p99 latency |
| Retry strategy | aggressive immediate retries | bounded backoff + inquiry | faster retries vs duplicate risk |
| Reconciliation cadence | real-time micro-batches | daily batches | quick correction vs cost/complexity |

Interview framing:

> Payment workflows should optimize for financial correctness first: strict idempotency, safe state transitions, immutable ledger entries, and reconciliation-based repair for external inconsistencies.

---

# 3. Low-Level Design

LLD goal:

> Model payments as a state machine with explicit attempts, provider transactions, immutable ledger entries, refunds, reconciliation, and idempotent external communication.

Simple rule:

- `PaymentIntent` owns business state.
- `PaymentAttempt` owns one try against a provider.
- `LedgerEntry` owns financial truth and is append-only.
- Provider adapters isolate external payment APIs and idempotency behavior.

Starter map:

| LLD question | Payment answer |
|---|---|
| What is the main aggregate? | `PaymentIntent` |
| What records one provider try? | `PaymentAttempt` |
| What stores provider reference IDs? | `ProviderTransaction` |
| What is immutable financial truth? | `LedgerEntry` |
| What handles money going back? | `Refund` |
| What repairs provider/internal mismatch? | `ReconciliationTask` |

Beginner-friendly design order:

1. Model `PaymentIntent` as a state machine before designing APIs.
2. Model `PaymentAttempt` separately so retries/provider calls are traceable.
3. Model `ProviderTransaction` so every external reference is persisted.
4. Model `LedgerEntry` as append-only; do not update old money movement records.
5. Design `ProviderAdapter` so external gateways are replaceable.
6. Add idempotency and reconciliation because provider uncertainty is normal.

Interview sentence:

> In LLD, I will design payments around explicit state transitions, idempotent mutating operations, provider attempts, immutable ledger entries, and reconciliation for ambiguous external outcomes.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `PaymentIntent` | merchant, amount, currency, lifecycle state | invalid state transitions are rejected |
| `PaymentAttempt` | one provider authorization/capture try | attempt result is never guessed without evidence |
| `ProviderTransaction` | provider reference IDs and raw status | provider IDs must be persisted for reconciliation |
| `LedgerEntry` | append-only money movement record | entries are immutable and balanced |
| `Refund` | refund request, amount, status | total refunded cannot exceed captured amount |
| `ReconciliationTask` | settlement comparison job | unresolved mismatches must remain visible |
| `WebhookEvent` | incoming/outgoing payment events | duplicate callbacks must be idempotent |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `PaymentIntentService` | create/load/update payment intent state | call providers directly for every operation |
| `PaymentOrchestrator` | coordinate auth/capture/refund flow | write ledger entries without state validation |
| `ProviderRoutingService` | choose provider based on method, region, health, cost | mutate payment state |
| `ProviderAdapter` | call provider APIs with provider idempotency keys | expose provider SDK details to domain layer |
| `LedgerService` | post immutable accounting entries | depend on provider callback timing |
| `RefundService` | validate refundable amount and execute refund | bypass original payment state |
| `ReconciliationService` | compare internal records with settlement/provider files | hide mismatches silently |
| `WebhookDispatchService` | notify merchants asynchronously | block core payment commit |

Core flow:

```text
Create intent -> reserve idempotency -> authorize provider -> update state -> post ledger -> emit outbox/webhook
Capture/refund -> validate state/amount -> call provider -> update state -> post ledger -> reconcile later
```

## 3.2 OOP Fundamentals

Encapsulation:

- `PaymentIntent` owns lifecycle transitions.
- `LedgerEntry` is immutable and append-only.
- `Refund` owns remaining refundable amount checks.

Abstraction:

- `ProviderAdapter` hides external API specifics.
- `PaymentRepository` abstracts persistence details.

Polymorphism:

- Different provider adapters implement same contract.
- Different retry strategies by payment method/risk class.

Composition over inheritance:

- `PaymentOrchestrator` composes risk checker, adapter, repository, and ledger writer.

## 3.3 SOLID Principles

| Principle | Payment workflow application |
|---|---|
| Single Responsibility | `LedgerService` only handles ledger posting rules |
| Open/Closed | add new provider integration without rewriting orchestrator core |
| Liskov Substitution | provider adapters interchangeable behind contract |
| Interface Segregation | separate interfaces for auth, capture, refund, reconciliation |
| Dependency Inversion | business layer depends on provider interfaces, not SDKs |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| State | payment lifecycle | prevent invalid transitions |
| Strategy | provider routing + retry policy | flexible runtime behavior |
| Factory | adapter creation by provider/method | clean extensibility |
| Observer/Event Publisher | payment events to webhooks/reports | decoupling |
| Circuit Breaker | provider call wrappers | resilience to external outages |

## 3.5 UML / Diagrams

### Class Diagram

```text
+------------------------+      +----------------------+
| PaymentOrchestrator    |----->| ProviderAdapter      |
| +authorize()           |      | +authorize()         |
| +capture()             |      | +capture()           |
| +refund()              |      | +refund()            |
+-----------+------------+      +----------------------+
            |
            +-------> +----------------------+
            |         | PaymentRepository    |
            |         +----------------------+
            |
            +-------> +----------------------+
                      | LedgerService        |
                      +----------------------+
```

### Sequence Diagram - Auth + Capture

```text
Client -> API: createPaymentIntent(idempotencyKey)
API -> IdempotencyStore: reserve/check
API -> Orchestrator: authorize(paymentIntent)
Orchestrator -> ProviderAdapter: authorize()
ProviderAdapter -> Orchestrator: authResult
Orchestrator -> PaymentRepository: updateState(AUTHORIZED)
Orchestrator -> LedgerService: post(AUTH)
Client -> API: capture(paymentIntent)
API -> Orchestrator: capture()
Orchestrator -> ProviderAdapter: capture()
Orchestrator -> LedgerService: post(CAPTURE)
```

## 3.6 Class Design

Interfaces:

```java
interface PaymentRepository {
    PaymentIntent getById(String paymentIntentId);
    void save(PaymentIntent paymentIntent);
}

interface ProviderAdapter {
    AuthResult authorize(PaymentIntent intent);
    CaptureResult capture(PaymentIntent intent, long amount);
    RefundResult refund(PaymentIntent intent, long amount);
}

interface LedgerService {
    void postEntry(LedgerEntry entry);
}

interface ReconciliationService {
    void reconcile(String settlementBatchId);
}
```

Design notes:

- Validate transition invariants before any external call and before final commit.
- Persist provider reference IDs for traceability.
- Keep idempotency key scoped per merchant/client.

## 3.7 Data Handling

Machine-coding version:

- `ConcurrentHashMap<String, PaymentIntent>` for intents.
- `ConcurrentHashMap<String, String>` for idempotency map.
- `ConcurrentHashMap<String, list>` for payment timeline events.
- append-only list for ledger entries.

Production version:

- ACID-compliant payment state store.
- Immutable ledger store.
- Outbox/event stream for webhooks and async jobs.
- Reconciliation tables and mismatch queues.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| duplicate idempotency key, same payload | return original canonical response |
| duplicate idempotency key, different payload | reject as idempotency conflict |
| unsupported currency/method | reject before provider call |
| capture amount exceeds authorized | reject by state/amount invariant |
| provider timeout after success | mark pending/unknown, retry with provider idempotency key, reconcile by callback |
| duplicate provider callback | dedup by provider event/transaction ID |
| late reversal/chargeback | append compensating ledger entries; do not rewrite old entries |
| provider outage | route to healthy provider if safe, otherwise fail gracefully/pending |
| reconciliation file delayed | keep settlement task open and alert past SLA |
| merchant webhook down | retry from outbox with backoff and DLQ |

Interview rule:

> Payment LLD is about never losing financial truth: state transitions are explicit, provider calls are idempotent, ledger entries are append-only, and reconciliation repairs uncertainty.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
payment/
  domain/
    PaymentIntent.java
    PaymentState.java
    LedgerEntry.java
    RefundRequest.java
  service/
    PaymentOrchestrator.java
    PaymentService.java
    RefundService.java
    ReconciliationService.java
  port/
    PaymentRepository.java
    ProviderAdapter.java
    IdempotencyStore.java
    LedgerRepository.java
  adapter/
    ProviderAAdapter.java
    ProviderBAdapter.java
    InMemoryPaymentRepository.java
  app/
    PaymentWorkflowDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock


class State(str, Enum):
    CREATED = "CREATED"
    AUTHORIZED = "AUTHORIZED"
    CAPTURED = "CAPTURED"
    REFUNDED = "REFUNDED"
    FAILED = "FAILED"


@dataclass
class PaymentIntent:
    payment_id: str
    amount: int
    state: State = State.CREATED


class PaymentEngine:
    def __init__(self) -> None:
        self.lock = Lock()
        self.intents: dict[str, PaymentIntent] = {}
        self.idempotency: dict[str, str] = {}
        self.ledger: list[tuple[str, str, int]] = []

    def create_intent(self, idem_key: str, payment_id: str, amount: int) -> PaymentIntent:
        with self.lock:
            if idem_key in self.idempotency:
                return self.intents[self.idempotency[idem_key]]
            intent = PaymentIntent(payment_id=payment_id, amount=amount)
            self.intents[payment_id] = intent
            self.idempotency[idem_key] = payment_id
            self.ledger.append((payment_id, "INTENT_CREATED", amount))
            return intent

    def authorize(self, payment_id: str) -> PaymentIntent:
        with self.lock:
            intent = self.intents[payment_id]
            if intent.state != State.CREATED:
                return intent
            intent.state = State.AUTHORIZED
            self.ledger.append((payment_id, "AUTHORIZED", intent.amount))
            return intent

    def capture(self, payment_id: str, amount: int) -> PaymentIntent:
        with self.lock:
            intent = self.intents[payment_id]
            if intent.state != State.AUTHORIZED or amount > intent.amount:
                raise ValueError("invalid capture")
            intent.state = State.CAPTURED
            self.ledger.append((payment_id, "CAPTURED", amount))
            return intent


engine = PaymentEngine()
pi = engine.create_intent("k1", "pi_1", 5000)
engine.authorize(pi.payment_id)
engine.capture(pi.payment_id, 5000)
print(engine.intents["pi_1"].state)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[idempotencyKey -> paymentId]` | dedup submit retries |
| `dict[paymentId -> PaymentIntent]` | state transitions |
| append-only list/log | ledger timeline |
| queue | async webhooks and reconciliation |
| map/set | duplicate callback protection |

## 4.4 Concurrency

High-signal concurrency issues:

- Client retries causing duplicate auth/capture.
- Concurrent capture and refund requests.
- Callback and poll updates racing.
- Reconciliation correction races with live updates.

Handling strategy:

- Idempotency keys plus compare-by-request-fingerprint.
- Optimistic locking/versioning on payment records.
- Event ordering by payment timeline sequence.
- Reconciliation writes as compensating ledger entries.

## 4.5 Performance Optimization

Time complexity (conceptual):

- Idempotency and state checks are near `O(1)`.
- Ledger appends are near `O(1)`.
- Reconciliation complexity scales with batch size.

Optimization rules:

- Keep provider call paths lightweight.
- Batch non-critical downstream notifications.
- Preload merchant/provider routing configs in cache.

## 4.6 Error Handling

| Error | Response |
|---|---|
| duplicate idempotent request | return existing canonical result |
| duplicate key with changed payload | `409 Conflict` |
| provider timeout | mark pending + query/reconcile before retrying charge |
| invalid transition | `422 Unprocessable Entity` |
| webhook signature invalid | reject and log security event |

## 4.7 Testing Thinking

Unit tests:

- Idempotent create/authorize/capture/refund flows.
- Payment state machine transition guards.
- Ledger invariants after each transition.
- Webhook signature and dedup behavior.

Concurrency tests:

- Parallel retries preserve single charge outcome.
- Capture/refund race conditions.
- Callback/poll eventual consistency.

Load tests:

- Flash-sale checkout spike.
- Provider partial outage with fallback.
- Reconciliation catch-up after delays.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| flash sale | massive checkout burst | API and provider saturation |
| provider outage | gateway partial failure | increased failures/pending states |
| retry storm | clients aggressively retry | duplicate pressure and queue flood |
| callback delay | provider webhook backlog | stale status and user confusion |
| reconciliation backlog | delayed settlement files | delayed mismatch correction |

## 5.2 Immediate Spike Response

1. Enforce strict idempotency and reject malformed retries.
2. Switch provider routing to healthiest adapters.
3. Gate low-priority traffic with adaptive throttles.
4. Move ambiguous outcomes to pending + async resolution.
5. Increase retry backoff with jitter.
6. Prioritize webhook status for user-facing payment states.
7. Keep ledger write path protected with reserved capacity.

## 5.3 Ambiguous Provider Result Strategy

If provider times out:

- Do not immediately re-charge.
- Save as `PENDING_PROVIDER_CONFIRMATION`.
- Query provider status endpoint and await callback.
- Resolve via reconciliation if still unresolved.

## 5.4 Degradation Policy

Protect in order:

1. Monetary correctness and ledger integrity.
2. Idempotent payment state transitions.
3. Checkout acceptance and status visibility.
4. Non-critical analytics and reports.
5. Dashboard freshness.

Allowed degradation:

- Delay analytics/reports.
- Reduce webhook retry pace for non-critical events.
- Slow lower-priority merchant traffic.

Not allowed:

- Double charge.
- Ledger mutation of committed financial records.
- Silent loss of accepted payment intents.

## 5.5 Spike Interview Answer

> During spikes, I protect idempotent payment acceptance and immutable ledger posting first. I treat provider timeouts as ambiguous pending states, resolve via inquiry/callback/reconciliation, and avoid blind retries that risk double charge.

---

# 6. Scaling To A Billion Users

## 6.1 Global Architecture

For billion users:

```text
Global checkout ingress
  -> regional payment orchestration cells
  -> provider adapter pools by region/method
  -> durable state + ledger stores
  -> async webhooks and reconciliation systems
```

## 6.2 Partitioning Strategy

- Partition payment data by merchant + payment intent hash.
- Route each payment lifecycle to a stable shard for transition consistency.
- Partition ledger by accounting book/account keys.

## 6.3 Multi-Region Strategy

- Regional affinity for latency and compliance.
- Active-active read and controlled write ownership model.
- DR failover with strict data correctness controls.
- Reconciliation cross-checks after failover events.

## 6.4 Provider and Settlement at Scale

- Multi-gateway routing by health/cost/region.
- Separate settlement ingestion pipelines.
- Continuous reconciliation for critical merchants.

## 6.5 Compliance and Security

- Tokenize payment instrument data.
- Minimize PCI scope with gateway token usage.
- Audit logs for every state mutation.
- Region-specific retention and deletion policies.

## 6.6 Billion-User Capacity Plan

| Layer | Scaling plan |
|---|---|
| Gateway/API | horizontal scale + tenant fairness controls |
| Orchestrator | stateless scale with shard-aware routing |
| Provider adapters | parallel pools + breaker/failover |
| Payment DB | partitioned strong-consistency writes |
| Ledger | append-only sharded store |
| Async plane | high-partition webhook/reconciliation queues |
| Observability | pending-rate, duplicate-rate, reconciliation-mismatch SLOs |

## 6.7 Billion-User Interview Answer

> At billion-user scale, payments require region-aware orchestration with strict idempotency, shard-consistent state transitions, immutable ledger posting, and robust reconciliation. Performance optimizations are secondary to financial correctness and auditability.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

Use this answer flow:

```text
I start by clarifying payment methods, state model (auth/capture/refund), and correctness guarantees.
Then I estimate transaction throughput, provider dependency risk, and peak surge behavior.
I design with payment orchestrator, idempotency store, provider adapters, immutable ledger, webhook pipeline, and reconciliation.
I enforce idempotent state transitions and avoid blind retries on ambiguous outcomes.
I isolate critical payment writes from non-critical analytics.
For spikes, I use provider failover, bounded retries, and pending-state resolution.
At billion scale, I use sharded state/ledger stores with regional routing and compliance-aware data boundaries.
```

---

# 8. Fast Recall Rules

- Payment APIs must be idempotent.
- State machine transitions must be guarded.
- Ledger should be immutable and append-only.
- Never retry ambiguous provider outcomes blindly.
- Use pending states and reconciliation to resolve uncertainty.
- Separate synchronous checkout from async side effects.
- Verify and deduplicate webhooks.
- Provider circuit breakers and failover are essential.
- Compliance and audit requirements shape architecture.
- Financial correctness beats micro-latency wins.

