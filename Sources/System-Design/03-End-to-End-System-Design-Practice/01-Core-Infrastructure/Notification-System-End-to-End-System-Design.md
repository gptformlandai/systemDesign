# Notification System - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Notification specifically, optimize fanout reliability, user preference controls, and channel fallback behavior.

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

| Layer | Interview signal | Notification system focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | push/email/SMS/in-app, preferences, dedup, urgency, retries |
| HLD | Can design scalable systems | event ingest, rules engine, template service, channel dispatchers, provider adapters |
| LLD | Can model maintainable components | `Notification`, `ChannelPolicy`, `Preference`, `DispatchAttempt`, `RetryPlan` |
| Machine coding | Can implement critical path | ingest event, resolve recipients, apply preferences, send, retry, DLQ |
| Traffic spikes | Can protect production | campaign bursts, provider throttling, queue backpressure |
| Billion users | Can reason at global scale | partitioned queues, multi-provider routing, region failover, cost-aware delivery |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- System accepts notification events from upstream services.
- Supports channels: push, email, SMS, and in-app.
- Applies user preferences and quiet hours.
- Supports templates and localized content.
- Supports immediate and scheduled delivery.
- Tracks delivery state: queued, sent, delivered, failed.
- Supports retries with channel/provider fallback.

Optional requirements to clarify:

- Do we require exactly-once user-visible notifications?
- Is dedup by event ID mandatory across channels?
- Should we support campaign notifications and transactional notifications in same system?
- Are per-tenant quotas required?
- Are attachments/media links in scope for email/push?

Out of scope unless interviewer asks:

- Full campaign builder UI.
- Full anti-spam abuse platform internals.
- Full analytics warehouse implementation.

## 1.2 Non-Functional Requirements

Delivery path:

- High throughput for burst fanout.
- High availability for critical transactional notifications.
- Bounded latency for urgent notifications.
- Graceful degradation when one provider is down.

Reliability and correctness:

- Durable event ingestion.
- Idempotent dispatch behavior.
- Accurate status tracking and auditability.

Operations:

- Channel-level and tenant-level rate limiting.
- Cost-aware channel selection.
- Provider health-aware routing.

## 1.3 Constraints

- Third-party provider rate limits and outages are common.
- User preferences must always be respected.
- Notification bursts can be extremely spiky.
- Duplicate sends damage user trust.
- Latency and cost goals can conflict.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Registered users | 1 billion |
| DAU | 300 million |
| Notification events/day | 50 billion |
| Peak events/sec | 5-10 million/sec during bursts |
| Channel split | 70% push, 20% in-app, 8% email, 2% SMS |
| Urgent notification SLA | p95 under 2 sec for push dispatch start |
| Availability target | 99.99% for transactional pipeline |

Back-of-the-envelope:

- `50B events/day` is about `578K events/sec` average.
- Burst windows may exceed `5M events/sec`.
- Provider quotas force queueing, retry, and multi-provider failover.
- Delivery logs at this scale require tiered retention.

## 1.5 Clarifying Questions To Ask

- Which notification categories are in scope: transactional, promotional, security?
- What are priority classes and SLA per class?
- Is quiet-hour deferral mandatory?
- Are users allowed channel-level preferences and frequency caps?
- What is acceptable duplicate probability?
- How long should delivery logs be retained?

Strong interview framing:

> I will design a durable, event-driven notification pipeline with strict preference enforcement, idempotent dispatch, provider-aware routing, and fallback/retry strategies that preserve reliability during burst traffic.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Event ingest flow:
Producer Service
  -> Notification API / Event Gateway
  -> Validation + Idempotency
  -> Event Queue / Stream
  -> Rule + Preference Engine
  -> Channel Router
  -> Channel-specific Dispatch Queues
  -> Provider Adapters (APNS/FCM/SES/Twilio)

Status flow:
Provider callback/polling
  -> Delivery Status Processor
  -> Status Store + Metrics
```

Recommended architecture:

```text
Upstream Services
      |
      v
+-----------------------+
| Notification Ingest   |
| API + Auth + Limits   |
+-----------+-----------+
            |
            v
+-----------------------+        +----------------------+
| Idempotency Service   |<------>| Preference Store     |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Event Stream/Queue    |------->| Rule + Template Svc  |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
   +-------------------+          +----------------------+
   | Channel Router    |--------->| Dispatch Queues      |
   +-------------------+          +----------+-----------+
                                                 |
                                                 v
                             +-----------------------------------+
                             | Provider Adapters                 |
                             | Push / Email / SMS / In-app       |
                             +----------------+------------------+
                                              |
                                              v
                             +-----------------------------------+
                             | Delivery Status + Analytics        |
                             +-----------------------------------+
```

Request flow for a transactional notification:

1. Service posts event with `eventId`, `userId`, `templateKey`, `priority`.
2. Ingest validates payload and checks idempotency.
3. Event is persisted to stream.
4. Rule engine resolves channels based on category and priority.
5. Preference engine applies opt-outs, quiet hours, locale.
6. Dispatcher sends via primary provider.
7. On failure or timeout, retry/fallback policy executes.
8. Final status emitted and stored.

## 2.2 APIs

### Publish Notification Event

```http
POST /v1/notifications/events
Authorization: Bearer <token>
Idempotency-Key: evt-9a31
Content-Type: application/json

{
  "eventId": "evt-9a31",
  "userId": "u-123",
  "category": "TRANSACTIONAL",
  "priority": "HIGH",
  "templateKey": "payment_success",
  "channelHints": ["PUSH", "EMAIL"],
  "data": {"amount": "120.00", "currency": "USD"}
}
```

### Update User Preferences

```http
PUT /v1/users/{userId}/notification-preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "push": {"enabled": true, "quietHours": {"start": "22:00", "end": "07:00"}},
  "email": {"enabled": true, "digest": "DAILY"},
  "sms": {"enabled": false}
}
```

### Query Delivery Status

```http
GET /v1/notifications/{eventId}/status
Authorization: Bearer <token>
```

Important points:

- Ingest API must be idempotent.
- Status API should distinguish provider accepted vs user delivered if available.
- Preference updates should be strongly consistent for correctness.

## 2.3 Core Components

Think of Notification System as a reliable delivery pipeline:

| Stage | What happens | Main risk |
|---|---|---|
| Ingest | accept event, validate, dedup, persist | duplicate or malformed events |
| Decision | apply rules, preferences, quiet hours, channel policy | sending when user opted out |
| Rendering | choose template and fill variables | bad/missing content |
| Dispatch | route to channel/provider and send | provider timeout/rate limit |
| Status/retry | record outcome, retry/fallback/DLQ | retry storms or unknown status |

The core idea is that notification delivery is asynchronous and stateful. A good design does not simply call APNS/FCM/email/SMS directly from the producer service.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Ingest API | validation, producer auth, idempotency, durable accept | provider calls | event QPS |
| Idempotency Service | duplicate event protection | delivery policy | event volume and TTL |
| Rule Engine | category, priority, channel plan, fallback policy | user contact storage | rule complexity and QPS |
| Preference Service | opt-in/out, quiet hours, locale, frequency caps | provider retry behavior | preference reads/writes |
| Template Service | versioned channel templates and rendering | channel selection | render QPS |
| Channel Router | queue selection by channel/priority/tenant | provider protocol details | routed event volume |
| Dispatch Workers | send attempts and retry handoff | original event ownership | queue lag and provider rate |
| Provider Adapters | APNS/FCM/email/SMS API normalization | business preference decisions | provider calls/sec |
| Status Processor | callback/polling/status state machine | template rendering | callback volume |
| Retry/DLQ Service | backoff, poison messages, fallback retries | first-time channel decision | retry volume |
| Observability/Cost Controls | lag, success rate, spend, provider health | business event mutation | telemetry volume |

### Ingest API

Why it exists:

- Upstream services need one reliable place to publish notification intent.
- Producers should not know provider APIs, user preferences, templates, or retry rules.

Core responsibilities:

- Authenticate producer service or tenant.
- Validate event schema and required fields.
- Enforce tenant quotas and priority limits.
- Reserve idempotency key such as `eventId`.
- Persist event into durable stream/queue.
- Return accepted status quickly.

What it should avoid:

- Do not call APNS/FCM/email/SMS directly.
- Do not render templates synchronously unless product scale is tiny.
- Do not silently accept malformed events.

Failure behavior:

- Duplicate event: return existing canonical status.
- Malformed event: reject with validation error.
- Queue unavailable: fail fast or use durable local/outbox buffer by policy.

Interview signal:

> Ingest API turns producer requests into durable notification events. It is not the dispatcher.

### Idempotency Service

Why it exists:

- Upstream services retry on timeouts.
- Duplicate notifications are user-visible and damage trust.

Core responsibilities:

- Deduplicate by event ID or idempotency key.
- Store request fingerprint to detect same key with different payload.
- Return existing event/status for safe retries.
- Keep TTL based on product retry and audit requirements.

Failure behavior:

- Same key + same payload: return original result.
- Same key + different payload: reject as idempotency conflict.
- Idempotency store down: fail closed for high-value transactional messages or fall back to durable unique constraint.

Interview signal:

> Idempotency prevents duplicate user-visible notifications when producers retry.

### Rule Engine

Why it exists:

- Not every event should go to every channel.
- Transactional, security, promotional, and campaign notifications have different rules.

Core responsibilities:

- Decide notification category and priority.
- Choose candidate channels such as push, in-app, email, SMS.
- Apply fallback order.
- Enforce legal/compliance constraints.
- Apply tenant/product rules such as frequency caps or high-priority bypass.

Example decisions:

| Event type | Typical channel plan |
|---|---|
| password reset | email + optional push, high priority |
| payment success | push/in-app, maybe email receipt |
| marketing campaign | push/email only if opted in, low priority |
| fraud alert | push + SMS fallback if allowed |

Failure behavior:

- Rule config missing: use safe default or fail event to DLQ.
- Rule evaluation slow: cache compiled rules and use bounded timeout.

Interview signal:

> Rule Engine decides what should be attempted; Dispatch Workers decide how to attempt it.

### Preference Service

Why it exists:

- User preferences are correctness requirements, not nice-to-have filters.
- Sending promotional or quiet-hour notifications incorrectly can violate trust or compliance.

Core responsibilities:

- Store channel opt-in/out.
- Apply quiet hours with timezone awareness.
- Apply locale and contact preference.
- Enforce category-specific controls.
- Support strong consistency for recent preference updates where required.

Failure behavior:

- Preference unavailable: fail closed for promotional messages; use stricter policy for transactional/security messages.
- Quiet hours crossing midnight/timezone: evaluate using user's configured timezone.
- Preference changes mid-flight: policy decides whether event uses snapshot at decision time or latest at dispatch time.

Interview signal:

> Preference Service protects user trust. Promotional sends should fail closed when preference is uncertain.

### Template Service

Why it exists:

- Different channels need different content shape and length.
- Templates need versioning, localization, and rollback.

Core responsibilities:

- Render channel-specific templates.
- Validate required placeholders.
- Choose locale and template version.
- Support rollback to previous safe template.
- Prevent unsafe content injection.

Failure behavior:

- Missing variable: fail before provider dispatch and record template error.
- Template version broken: rollback or route to DLQ.
- Locale missing: fallback to default locale.

Interview signal:

> Template Service makes content versioned and channel-aware, so dispatchers do not hardcode message text.

### Channel Router

Why it exists:

- Push, email, SMS, and in-app have different costs, limits, and SLAs.
- Priority classes must be isolated.

Core responsibilities:

- Route delivery plans to channel-specific queues.
- Separate transactional and promotional traffic.
- Apply tenant quotas and fair-share scheduling.
- Consider provider health and cost.
- Delay quiet-hour messages to scheduled queues.

Queue strategy:

| Queue type | Purpose |
|---|---|
| high-priority push | security/transactional low-latency delivery |
| normal push | ordinary app notifications |
| email | slower, batchable, cheaper than SMS |
| SMS | expensive and rate-limited |
| retry | delayed retry attempts |
| DLQ | poison/permanently failed messages |

Failure behavior:

- Campaign burst: promotional queues back up without starving transactional queues.
- Provider rate limit: route to delayed retry or alternate provider.

Interview signal:

> Channel Router is where priority, cost, and provider health become delivery queues.

### Dispatch Workers

Why they exist:

- Provider calls are slow, rate-limited, and failure-prone.
- Workers let each channel scale independently.

Core responsibilities:

- Pull work from channel queues.
- Call Provider Adapter.
- Record `DispatchAttempt`.
- Apply retry/fallback decisions.
- Emit status events.
- Respect provider and tenant rate limits.

Failure behavior:

- Provider timeout: mark attempt `UNKNOWN/PENDING`, retry safely.
- Permanent provider error: mark failed and fallback if allowed.
- Worker crash: message becomes visible again from queue/stream.

Interview signal:

> Dispatch Workers are intentionally async so provider problems do not collapse ingest.

### Provider Adapters

Why they exist:

- APNS, FCM, SES, SendGrid, Twilio, and other providers all have different APIs and error semantics.
- Core dispatch logic should not depend on provider SDK details.

Core responsibilities:

- Normalize provider requests/responses.
- Translate provider-specific errors into common statuses.
- Apply provider-specific idempotency or dedup keys if available.
- Support provider health metrics.

Failure behavior:

- Provider returns rate limit: signal retry-after/backoff.
- Provider accepts but callback delayed: status remains pending until callback or timeout workflow resolves.
- Provider outage: circuit breaker opens and Channel Router shifts traffic if allowed.

Interview signal:

> Provider Adapters isolate messy third-party APIs from the delivery pipeline.

### Status Processor

Why it exists:

- Accepted by provider is not always delivered to user.
- Provider callbacks may arrive late, duplicated, or out of order.

Core responsibilities:

- Process provider callbacks/webhooks.
- Poll providers when callbacks are unavailable.
- Update delivery state machine.
- Deduplicate callbacks by provider event ID.
- Emit final status for dashboards and audit.

Common states:

```text
QUEUED -> RENDERED -> SENT -> DELIVERED
QUEUED -> FAILED_RETRYABLE -> RETRY_SCHEDULED -> SENT
QUEUED -> FAILED_PERMANENT -> DLQ/FINAL_FAILED
SENT -> UNKNOWN -> RECONCILED
```

Failure behavior:

- Duplicate callback: ignore after idempotent status update.
- Callback missing: timeout job moves status to retry/unknown.
- Out-of-order callback: state machine rejects invalid backward transition.

Interview signal:

> Status Processor turns unreliable provider signals into a clear internal delivery lifecycle.

### Retry, Fallback, and DLQ Service

Why it exists:

- Provider failures are normal.
- Blind retries can create storms, high cost, and duplicate sends.

Core responsibilities:

- Schedule exponential backoff with jitter.
- Bound max attempts.
- Choose fallback channel/provider when policy allows.
- Send poison messages to DLQ with reason.
- Alert on DLQ growth or provider-wide failures.

Failure behavior:

- Retry storm risk: global provider circuit breaker slows retries.
- Poison event: isolate in DLQ rather than blocking queue.
- SMS cost spike: cap fallback to SMS by priority and budget.

Interview signal:

> Retry logic must be controlled. More retries are not always better in notification systems.

### Observability and Cost Controls

Why they exist:

- Notification systems are expensive and operationally noisy.
- Operators need visibility by tenant, channel, provider, priority, and template.

Core metrics:

- Ingest QPS and rejection rate.
- Queue lag by channel/priority.
- Provider success/error/rate-limit rate.
- Delivery latency p50/p95/p99.
- Duplicate suppression count.
- DLQ volume.
- SMS/email cost by tenant/campaign.

Interview signal:

> Observability is essential because notification failure often shows up as lag, retries, provider throttling, or unexpected cost.

### How The Components Work Together

Transactional notification path:

```text
Producer -> Ingest API -> Idempotency -> Event Stream -> Rule/Preference -> Template -> Channel Queue -> Dispatch Worker -> Provider Adapter -> Status Processor
```

Retry path:

```text
Provider failure -> Status Processor -> Retry Policy -> delayed retry queue -> Dispatch Worker -> fallback/DLQ if attempts exhausted
```

One-stop interview answer:

> I design notifications as a durable async pipeline: ingest deduplicates and persists events, rules/preferences decide what is allowed, templates render channel content, channel queues isolate priority/cost/provider limits, dispatch workers call adapters, and status/retry/DLQ workflows make delivery observable and recoverable.

## 2.4 Data Layer

### Core Data Models

Notification event:

```json
{
  "eventId": "evt-9a31",
  "userId": "u-123",
  "category": "TRANSACTIONAL",
  "priority": "HIGH",
  "templateKey": "payment_success",
  "payload": {"amount": "120.00", "currency": "USD"},
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Dispatch attempt:

```json
{
  "attemptId": "att-221",
  "eventId": "evt-9a31",
  "channel": "PUSH",
  "provider": "FCM",
  "status": "FAILED_TIMEOUT",
  "retryCount": 1,
  "updatedAt": "2026-06-17T12:00:02Z"
}
```

Preference document:

```json
{
  "userId": "u-123",
  "push": {"enabled": true},
  "email": {"enabled": true, "digest": "DAILY"},
  "sms": {"enabled": false},
  "quietHours": {"start": "22:00", "end": "07:00"}
}
```

### Store Choices

| Data type | Candidate store | Why |
|---|---|---|
| Event log | stream + durable queue | burst buffering and replay |
| Preferences | KV/document store | frequent reads by user |
| Template metadata | relational/doc store | versioned config |
| Delivery status | wide-column/KV | high write throughput |
| Analytics | stream + OLAP | aggregation at scale |

### Partitioning

- Partition events by `tenantId` and/or `userId` hash.
- Partition dispatch queues by channel + priority.
- Isolate hot tenants/campaigns to reduce blast radius.

### Replication

- Multi-AZ for core stores.
- Cross-region DR replication.
- Eventual consistency acceptable for analytics, not preference correctness.

## 2.5 Scalability

### Horizontal Scaling

- Ingest, router, and workers are stateless.
- Scale workers by queue lag per channel.
- Independently scale per channel (SMS often lower throughput, higher cost).

### Queueing Strategy

- Use separate queues by priority and channel.
- High priority transactional notifications preempt promotional load.
- Rate limit promotional notifications under pressure.

### Tenant Isolation

- Per-tenant quotas and rate limits.
- Optional dedicated partitions for very large tenants.

## 2.6 Performance

### Caching Strategy

| Cache layer | What it stores | TTL |
|---|---|---:|
| Preference cache | hot user preference docs | short |
| Template cache | compiled templates | medium |
| Provider config cache | credentials/endpoints/limits | short |

### Latency Budget Example (high priority push)

| Stage | Target |
|---|---:|
| Ingest validation + idempotency | 10-30 ms |
| Rule/preference resolution | 10-25 ms |
| Queueing + dispatch scheduling | 20-80 ms |
| Provider API call | 50-300 ms |

### Optimization Rules

- Keep template render light and precompiled.
- Batch provider calls where supported.
- Apply adaptive retry timing based on provider health.

## 2.7 Async Systems

Use streams/queues for:

- notification event ingest
- dispatch attempt events
- provider callback events
- retry scheduling
- analytics sinks

Queue design notes:

- At-least-once is common.
- Dedup keys prevent duplicate user-visible sends.
- DLQ for repeated failures.
- Backpressure controls and queue lag alarms are mandatory.

## 2.8 Reliability

### Retry and Idempotency

- Ingest idempotency by `eventId`.
- Dispatch idempotency by `(eventId, channel, recipient)`.
- Exponential backoff with jitter.

### Circuit Breakers and Fallbacks

- If primary push provider fails, switch to secondary provider.
- If SMS provider is down, fallback to push/email where policy allows.
- Promotional traffic shedding protects transactional path.

### Failover

- Multi-region active-active ingest.
- Region-local dispatch with cross-region replay if needed.
- Recovery via queue replay and idempotent processors.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Delivery model | synchronous send | async queued send | immediacy vs resilience/scale |
| Provider usage | single provider | multi-provider | simplicity vs reliability/cost optimization |
| Preference consistency | eventual | stronger consistency | speed vs correctness guarantees |
| Retries | aggressive | bounded with fallback | success rate vs cost/noise |
| Logging | full retention | sampled/tiered | observability vs storage cost |

Interview framing:

> I would keep notification delivery asynchronous and idempotent, isolate transactional from promotional traffic, and use provider-aware fallback to maintain reliability under real-world provider failures.

---

# 3. Low-Level Design

LLD goal:

> Model notification delivery as a pipeline: ingest event, evaluate rules/preferences, render template, dispatch through a provider adapter, track status, and retry safely.

Simple rule:

- Events are input facts.
- Preferences and rules decide whether/how to send.
- Adapters isolate provider APIs.
- Status and retry state make delivery observable and recoverable.

Starter map:

| LLD question | Notification answer |
|---|---|
| What starts the workflow? | `NotificationEvent`, produced by another service |
| What decides whether to send? | `RuleEngine` + `PreferenceService` |
| What creates the content? | `TemplateService` using `TemplateVersion` |
| What represents one send try? | `DispatchAttempt` |
| What tracks the lifecycle? | `DeliveryStatus` |
| What isolates APNS/FCM/email/SMS? | `ProviderAdapter` |

Beginner-friendly design order:

1. Model `NotificationEvent` as the durable input.
2. Model `UserPreference` and `ChannelPolicy` before dispatching anything.
3. Model `TemplateVersion` so rendered content is traceable.
4. Model `DispatchAttempt` because each provider call can fail or retry.
5. Model `DeliveryStatus` as a state machine.
6. Design `ProviderAdapter` so push, email, and SMS use the same contract.

Interview sentence:

> In LLD, I will treat notification delivery as a stateful pipeline: dedup the event, apply preferences, render a template, create dispatch attempts, call provider adapters, update status, and retry or DLQ safely.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `NotificationEvent` | source event, tenant, user, template key, idempotency ID | duplicate events must not create duplicate sends |
| `Recipient` | user/contact destinations | missing/invalid contact blocks that channel only |
| `ChannelPolicy` | allowed channels, priority, fallback order | transactional rules should not be overridden by promo policy |
| `UserPreference` | opt-in/out, quiet hours, channel preference | compliance preferences must be respected |
| `TemplateVersion` | channel-specific rendered content definition | sent content should be traceable to a version |
| `DispatchAttempt` | one provider send attempt | retries need attempt count and provider response |
| `DeliveryStatus` | lifecycle such as queued/sent/failed/delivered | state transitions must be legal and monotonic where possible |
| `RetrySchedule` | next retry time/backoff | retry loops must be bounded |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `IngestService` | validate event, enforce idempotency, enqueue work | call external providers directly |
| `PreferenceService` | apply user/tenant/contact preferences | render templates |
| `RuleEngine` | choose channels, priority, fallback behavior | store provider-specific responses |
| `TemplateService` | render versioned channel content | decide whether user opted out |
| `DispatchService` | call provider adapters and create attempts | own retry scheduling policy |
| `RetryService` | schedule bounded retries and DLQ poison jobs | mutate original event payload |
| `StatusService` | record provider responses/callbacks | send notifications itself |

Core flow:

```text
Ingest -> idempotency -> rules/preferences -> render -> dispatch adapter -> status -> retry/DLQ if needed
```

## 3.2 OOP Fundamentals

Encapsulation:

- `ChannelPolicy` owns channel eligibility and fallback order.
- `DeliveryStatus` owns legal state transitions.
- `RetrySchedule` owns next-attempt timing.

Abstraction:

- `ProviderAdapter` hides provider-specific APIs.
- `PreferenceRepository` hides storage internals.

Polymorphism:

- Different adapter implementations for push/email/SMS providers.
- Different retry policies for transactional vs promotional traffic.

Composition over inheritance:

- `DispatchService` composes rule evaluation, template rendering, adapter calls, and status update.

## 3.3 SOLID Principles

| Principle | Notification application |
|---|---|
| Single Responsibility | `TemplateService` only renders content |
| Open/Closed | add new channel/provider adapter without changing core ingest |
| Liskov Substitution | all `ProviderAdapter` implementations honor dispatch contract |
| Interface Segregation | separate preference, dispatch, and status interfaces |
| Dependency Inversion | dispatch depends on `ProviderAdapter` interface |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | channel selection and retry policy | configurable behavior by category |
| Factory | provider adapter creation | clean provider wiring |
| Observer/Event Publisher | emit status/analytics events | decouple analytics from core path |
| Decorator | metrics/tracing/rate limit wrappers | cross-cutting concerns |
| State | delivery lifecycle transitions | correctness in status updates |

## 3.5 UML / Diagrams

### Class Diagram

```text
+----------------------+       +----------------------+
| IngestService        |------>| IdempotencyStore     |
| +publish(event)      |       +----------------------+
+----------+-----------+
           |
           v
+----------------------+       +----------------------+
| DispatchService      |------>| ProviderAdapter      |
| +dispatch(plan)      |       | +send(message)       |
+----------+-----------+       +----------+-----------+
           |                               |
           v                               v
+----------------------+       +----------------------+
| StatusService        |<------| Provider callbacks   |
+----------------------+       +----------------------+
```

### Dispatch Sequence

```text
Producer -> IngestService: publish(eventId, userId, templateKey)
IngestService -> IdempotencyStore: check/reserve(eventId)
IngestService -> RuleEngine: resolve channels + priority
RuleEngine -> PreferenceService: apply user preferences
DispatchService -> TemplateService: render(channel template)
DispatchService -> ProviderAdapter: send(request)
ProviderAdapter -> StatusService: callback/response
StatusService -> RetryService: schedule retry if needed
```

## 3.6 Class Design

Interfaces:

```java
interface ProviderAdapter {
    DispatchResult send(DispatchRequest request);
}

interface PreferenceService {
    ChannelDecision evaluate(String userId, NotificationEvent event);
}

interface IdempotencyStore {
    boolean reserveIfAbsent(String key, long ttlSeconds);
}

interface RetryPolicy {
    RetryDecision nextAttempt(DispatchAttempt attempt);
}
```

## 3.7 Data Handling

Machine-coding version:

- `ConcurrentHashMap<String, NotificationEvent>` for events.
- `ConcurrentHashMap<String, UserPreference>` for preferences.
- `ConcurrentHashMap<String, DeliveryStatus>` for status map.
- `PriorityQueue<DispatchAttempt>` for retry schedule.

Production version:

- Stream-backed ingestion.
- Partitioned channel dispatch queues.
- Durable status store and callback processor.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| duplicate event replay | reserve idempotency key and return existing status |
| malformed template variables | fail validation before dispatch and record error |
| unknown user/contact missing | skip invalid channel, fallback if allowed |
| user opted out/quiet hours | suppress or delay based on notification category |
| provider timeout after acceptance | mark `UNKNOWN/PENDING`, retry idempotently, reconcile by callback |
| delayed/missing callback | status timeout job moves to retry or terminal unknown state |
| provider rate limit | backoff per provider/channel, avoid retry storm |
| campaign burst | isolate promotional queue from transactional queue |
| tenant overload | tenant-level rate limits and fair-share scheduling |
| poison message | move to DLQ with reason and alert if volume rises |

Interview rule:

> Notification LLD is about safe pipeline state: dedup input events, respect preferences, isolate provider adapters, and make retries/status transitions explicit.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
notification/
  domain/
    NotificationEvent.java
    UserPreference.java
    DispatchAttempt.java
    DeliveryStatus.java
  service/
    IngestService.java
    DispatchService.java
    RetryService.java
    StatusService.java
  port/
    ProviderAdapter.java
    PreferenceRepository.java
    IdempotencyStore.java
  adapter/
    PushProviderAdapter.java
    EmailProviderAdapter.java
    SmsProviderAdapter.java
    InMemoryIdempotencyStore.java
  app/
    NotificationDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from collections import defaultdict, deque
from datetime import datetime, timezone


@dataclass(frozen=True)
class NotificationEvent:
    event_id: str
    user_id: str
    category: str
    template_key: str
    payload: dict


class InMemoryNotificationSystem:
    def __init__(self) -> None:
        self.idempotency: set[str] = set()
        self.preferences: dict[str, dict] = defaultdict(lambda: {
            "push": True,
            "email": True,
            "sms": False,
        })
        self.dispatch_log: deque[tuple[str, str, str, str]] = deque()

    def publish(self, event: NotificationEvent) -> str:
        if event.event_id in self.idempotency:
            return "DUPLICATE_IGNORED"

        self.idempotency.add(event.event_id)
        prefs = self.preferences[event.user_id]

        channels = []
        if prefs.get("push"):
            channels.append("PUSH")
        if prefs.get("email"):
            channels.append("EMAIL")
        if not channels and prefs.get("sms"):
            channels.append("SMS")

        now = datetime.now(timezone.utc).isoformat()
        for channel in channels:
            self.dispatch_log.append((event.event_id, event.user_id, channel, now))

        return "QUEUED"


system = InMemoryNotificationSystem()
evt = NotificationEvent("evt-1", "u-1", "TRANSACTIONAL", "payment_success", {"amount": "120"})
print(system.publish(evt))
print(system.publish(evt))
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `set[eventId]` | idempotent ingest dedup |
| `dict[userId -> preference]` | preference lookups |
| queue/deque | dispatch and retry pipelines |
| `dict[eventId -> status]` | status query support |
| priority queue | scheduled retry execution |

## 4.4 Concurrency

High-signal concurrency issues:

- Duplicate publishes from upstream retry.
- Concurrent preference update while dispatching.
- Provider callback race with retry scheduler.
- Same event routed to multiple workers.

Handling strategy:

- Atomic idempotency reservation.
- Snapshot preferences at dispatch decision time.
- Status transition guard via state machine.
- Dedup at dispatch attempt key level.

## 4.5 Performance Optimization

Time complexity (conceptual):

- Ingest dedup is near `O(1)`.
- Preference lookup is near `O(1)` for cached users.
- Dispatch cost scales with recipient/channel fanout.

Optimization rules:

- Precompile templates.
- Batch provider calls where supported.
- Separate high and low priority queues.
- Cap retry fanout under provider outage.

## 4.6 Error Handling

| Error | Response |
|---|---|
| malformed event | `400 Bad Request` |
| unauthorized producer | `401/403` |
| duplicate event ID | idempotent success/no-op |
| provider timeout | retry or fallback channel |
| permanent provider error | mark failed and send to DLQ/manual review |

## 4.7 Testing Thinking

Unit tests:

- Event idempotency.
- Preference enforcement.
- Channel fallback logic.
- Retry policy timing and max attempts.

Concurrency tests:

- Duplicate events under parallel publish.
- Callback + retry race handling.
- Preference changes during dispatch.

Load tests:

- Campaign burst with provider throttling.
- Transactional + promotional mixed load fairness.
- Multi-provider failover scenario.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| campaign blast | millions of promotional sends | queue saturation |
| transactional storm | outage recovery sends | priority starvation |
| provider outage | APNS/FCM/SES down | retry amplification |
| tenant abuse | one tenant flood | noisy-neighbor impact |
| callback delay | provider webhook lag | stale status and duplicate retries |

## 5.2 Immediate Spike Response

1. Isolate queues by priority and channel.
2. Protect transactional channel budgets first.
3. Apply tenant-level throttles and quotas.
4. Enable provider failover and dynamic routing.
5. Increase retry backoff to avoid retry storms.
6. Shed low-priority promotional traffic if needed.
7. Alert on queue lag, drop rate, and provider error spikes.

## 5.3 Provider Outage Strategy

During provider outage:

- Trip circuit breaker for failing provider.
- Route to secondary provider if available.
- If no fallback, queue with bounded retries and TTL.
- Surface degraded mode to dependent services.

## 5.4 Degradation Policy

Protect in this order:

1. Transactional notifications.
2. Preference correctness and compliance rules.
3. Delivery status correctness.
4. Promotional and low-priority sends.
5. Non-critical analytics richness.

Allowed degradation:

- Delay promotional campaigns.
- Lower analytics granularity.
- Extend non-critical notification latency.

Not allowed:

- Ignore user opt-out/legal preferences.
- Lose accepted transactional events.
- Unbounded retry storms.

## 5.5 Spike Interview Answer

> I would isolate traffic by priority, protect transactional notifications, and use provider-aware circuit breakers with fallback. Under spikes, I would throttle promotional traffic, increase retry backoff, and preserve preference correctness and delivery durability first.

---

# 6. Scaling To A Billion Users

## 6.1 Global Architecture

For billion users:

```text
Global ingest endpoints
  -> regional event streams
  -> rules/preferences/template services
  -> channel-specific dispatch clusters
  -> multi-provider adapters
  -> global status and analytics pipelines
```

## 6.2 Partitioning Strategy

- Partition by tenant and user hash to spread load.
- Maintain separate priority partitions.
- Isolate hot tenants/campaign partitions.

## 6.3 Multi-Region Strategy

- Active-active ingest across regions.
- Region-local dispatch to reduce provider latency.
- Cross-region replay for disaster recovery.
- Keep preference writes strongly controlled and replicated.

## 6.4 Provider and Cost Strategy

- Multi-provider contracts per channel.
- Dynamic provider selection by health, latency, and cost.
- Rate cap expensive channels (like SMS) with policy controls.

## 6.5 Data Lifecycle

- Hot retention for operational status.
- Warm/cold archival for long-term audit/analytics.
- Privacy-compliant retention and deletion workflows.

## 6.6 Billion-User Capacity Plan

| Layer | Scaling plan |
|---|---|
| Ingest API | horizontal autoscale by QPS and tenant quotas |
| Streams/queues | high partition counts by priority/channel |
| Dispatch workers | lag-based autoscaling and circuit-breaker routing |
| Provider adapters | pool and parallelize with per-provider rate limits |
| Preference store | read-heavy cached KV with strongly controlled writes |
| Status store | high write throughput store with TTL/tiering |
| Observability | queue lag, send latency, failover rate, duplicate rate |

## 6.7 Billion-User Interview Answer

> At billion-user scale, I would run an asynchronous, partitioned notification platform with strict priority isolation, provider-aware routing, and idempotent processing. Transactional paths stay protected while promotional traffic is throttled under pressure, ensuring reliable and compliant delivery.

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
I will clarify notification categories, SLA tiers, channels, preference rules, and dedup expectations.
Then I estimate event throughput, burst patterns, and provider quota constraints.
I design an async pipeline: ingest + idempotency, rule/preference resolution, channel routing, dispatch queues, provider adapters, and status tracking.
I isolate transactional vs promotional traffic and apply retries/fallback with circuit breakers.
For data, I keep durable event logs, preference store, and delivery status state machine.
For spikes, I protect high-priority queues, throttle noisy tenants, and fail over providers.
At billion scale, I use partitioned queues, multi-region dispatch, and cost-aware routing.
```

---

# 8. Fast Recall Rules

- Notifications should be asynchronous and queue-first.
- Idempotency is mandatory at ingest and dispatch.
- Respect user preferences and legal constraints always.
- Separate transactional and promotional traffic paths.
- Multi-provider routing improves resilience.
- Retries need jitter, caps, and DLQ.
- Provider outages must not collapse the whole pipeline.
- Queue lag and duplicate rate are key operational metrics.
- Cost-aware channel selection matters at scale.
- Degrade low-priority sends before core transactional flow.