# Microservices Mock Interview Scripts

> Track: Microservices Interview Track - Group 5 Practice Upgrade  
> Goal: rehearse microservices interviews under realistic follow-up pressure.

Use these scripts with a timer. Speak answers aloud.

---

## 1. Scoring Rules

Score every answer from 1 to 5.

| Score | Meaning |
|---:|---|
| 1 | vague definitions, pattern names only |
| 2 | basic concept, missing trade-offs and failures |
| 3 | correct mechanics, limited production depth |
| 4 | strong senior answer with failure/observability/security |
| 5 | owner-level answer with migration/testing/cost/follow-up depth |

Target:

```text
For senior backend interviews, aim for consistent 4s. For FAANG-style rounds, aim for 4s
with several 5s in design, debugging, and trade-off questions.
```

---

## 2. Mock 1: Starter Concept Round - 30 Minutes

### Question 1

```text
What are microservices, and when would you avoid them?
```

Expected points:

- independently deployable services
- business capability ownership
- own data
- network and operational complexity
- avoid for small/simple/tightly coupled systems
- modular monolith alternative

Follow-ups:

1. What is a distributed monolith?
2. Why is shared database risky?
3. What is independent deployability really?

---

### Question 2

```text
Explain monolith vs modular monolith vs microservices.
```

Expected points:

- deploy unit
- module boundaries
- team ownership
- data ownership
- migration path

Follow-ups:

1. When would you start modular monolith first?
2. How do you prevent modules from leaking boundaries?

---

### Question 3

```text
How would you split a hotel booking platform into services?
```

Expected points:

- Search, Availability, Booking, Payment, Notification, Loyalty
- service owns data/invariants
- avoid service per table
- sync/async choices

Follow-ups:

1. Who owns room inventory?
2. Who owns payment audit?
3. Why not have a generic RoomService?

---

## 3. Mock 2: Communication And Contracts - 30 Minutes

### Question 1

```text
REST vs gRPC vs events. When do you use each?
```

Expected points:

- REST public/simple resource APIs
- gRPC internal typed low-latency calls
- events for async fan-out and decoupling
- all need contracts, auth, observability

Follow-ups:

1. What is fan-out and why is it dangerous?
2. What belongs in an API Gateway?
3. When would you add a BFF?

---

### Question 2

```text
How do you prevent independent deployments from breaking consumers?
```

Expected points:

- backward compatibility
- additive changes
- consumer-driven contract testing
- schema compatibility
- deprecation windows
- canary rollout

Follow-ups:

1. Is adding a required field safe?
2. How do you evolve an event schema?
3. What does Pact protect against?

---

### Question 3

```text
A provider changes `bookingStatus` to `status`. What is your rollout plan?
```

Expected points:

- add new field while keeping old
- update consumers
- measure old field usage
- remove after deprecation
- contract tests

---

## 4. Mock 3: Data Consistency Round - 45 Minutes

### Question 1

```text
How do you prevent double booking of the last room?
```

Expected points:

- Availability Service owns invariant
- row lock or optimistic versioning
- local transaction
- Booking Service asks, does not write DB
- idempotency for duplicate reserve requests

Follow-ups:

1. What if two requests arrive at the exact same time?
2. Can search page show stale availability?
3. How do you handle reservation holds?

---

### Question 2

```text
Explain saga, outbox, and idempotency using hotel booking.
```

Expected points:

- saga coordinates booking/inventory/payment
- outbox atomically records events with local state
- idempotency protects retries and duplicates
- compensations and DLQ
- observability

Follow-ups:

1. Outbox relay crashes after publish. What happens?
2. Payment timeout happens. Is it failure?
3. Compensation fails. What do you do?

---

### Question 3

```text
Choreography vs orchestration saga?
```

Expected points:

- choreography event-driven, decentralized
- orchestration central workflow state
- orchestration useful for complex booking/payment
- choreography useful for simple side effects
- trade-off: visibility vs central dependency

---

## 5. Mock 4: Kafka And Event-Driven Round - 45 Minutes

### Question 1

```text
Explain Kafka topics, partitions, offsets, and consumer groups.
```

Expected points:

- append-only log
- partition ordering
- offset progress
- group shares work
- different groups get independent copy

Follow-ups:

1. How do you preserve order for one booking?
2. What happens if consumers exceed partition count?
3. What is consumer lag?

---

### Question 2

```text
Notification consumer lag is rising. Debug it.
```

Expected points:

- producer vs consumer rate
- handler error rate
- downstream email provider
- partitions/consumer count
- hot partition
- rebalance count
- retry/DLQ volume

Follow-ups:

1. What if one partition has all lag?
2. How do you replay safely?
3. What metrics belong on the dashboard?

---

### Question 3

```text
What is exactly-once delivery? Is it real?
```

Expected points:

- nuanced answer
- broker-level guarantees have boundaries
- end-to-end side effects still need idempotency
- external DB/email/payment can duplicate

---

## 6. Mock 5: Resilience And Scale Round - 45 Minutes

### Question 1

```text
Timeout, retry, circuit breaker, bulkhead: explain how they work together.
```

Expected points:

- timeout bounds waiting
- retry handles transient failures
- backoff/jitter reduces synchronization
- circuit breaker stops repeated calls to failing dependency
- bulkhead isolates resource pools

Follow-ups:

1. What is retry storm?
2. When should you not retry?
3. What is a retry budget?

---

### Question 2

```text
Payment Service becomes slow. Checkout p99 spikes. What do you do?
```

Expected points:

- mitigate user impact
- check traces and dependency metrics
- timeouts/circuit breaker
- reduce retries
- degrade or queue if business allows
- rollback recent changes
- protect payment and DB resources

---

### Question 3

```text
How do you capacity plan a microservice?
```

Expected points:

- QPS, latency, CPU/memory, DB pool, downstream quotas
- peak vs average
- p95/p99
- load testing
- autoscaling metric
- bottleneck identification

---

## 7. Mock 6: Observability And Incident Round - 45 Minutes

### Question 1

```text
Logs vs metrics vs traces. Give a debugging example.
```

Expected points:

- metrics detect
- traces locate
- logs explain
- correlation/trace context
- example checkout latency

Follow-ups:

1. What is OpenTelemetry?
2. What is SLO and error budget?
3. What is burn-rate alerting?

---

### Question 2

```text
Booking API p99 jumps to 6 seconds with low error rate. Walk through incident response.
```

Expected points:

- confirm scope and user impact
- check deploy/config
- trace slow requests
- inspect downstream spans
- DB/pool/saturation
- mitigate first
- communicate roles/status

---

### Question 3

```text
Region outage affects checkout. What do you do?
```

Expected points:

- incident roles
- failover plan
- RTO/RPO
- data consistency risks
- communicate user impact
- validate after failover
- postmortem

---

## 8. Mock 7: Security Round - 45 Minutes

### Question 1

```text
How do you secure service-to-service communication?
```

Expected points:

- mTLS or workload identity
- service authorization
- network policy
- least privilege
- secrets management
- audit logs

Follow-ups:

1. Does mTLS solve authorization?
2. What is SPIFFE/SPIRE style identity?
3. How does service mesh help?

---

### Question 2

```text
Why is gateway authentication not enough?
```

Expected points:

- direct internal calls
- jobs/services bypass gateway
- service-level domain authorization
- tenant isolation
- audit actions

---

### Question 3

```text
Payment secret rotation causes outage. How do you debug and prevent it?
```

Expected points:

- timeline
- secret mount/reload
- provider credential validity
- dual credentials
- canary validation
- rollback/runbook

---

## 9. Mock 8: Kubernetes And Platform Round - 45 Minutes

### Question 1

```text
Explain liveness, readiness, startup probes and a failure mode.
```

Expected points:

- startup for boot
- readiness for traffic
- liveness for stuck process
- DB check in liveness creates restart storm

---

### Question 2

```text
p99 latency rises after Kubernetes rollout. What do you check?
```

Expected points:

- version metrics
- readiness timing
- CPU throttling
- memory/OOM
- DB pool
- canary/rollback
- logs/traces by version

---

### Question 3

```text
HPA scales consumers but lag remains. Why?
```

Expected points:

- partition count cap
- downstream bottleneck
- hot partition
- handler slow
- rebalances
- poison messages

---

## 10. Mock 9: Full FAANG System Design - 60 Minutes

Prompt:

```text
Design a hotel booking platform that supports search, booking, payment, notification,
loyalty, reporting, high availability, and safe deployments.
```

Expected structure:

1. Clarify requirements and scale.
2. Define service boundaries.
3. Define data ownership.
4. Draw main flows.
5. Explain booking saga.
6. Explain search read model.
7. Explain event design and Kafka topics.
8. Explain failure handling and idempotency.
9. Explain observability and SLOs.
10. Explain security.
11. Explain deployment/migration.
12. Discuss trade-offs.

Follow-up pressure:

- Prevent double booking.
- Payment timeout.
- Outbox failure.
- Consumer lag.
- Regional outage.
- Breaking API change.
- Gateway bypass security.
- Canary rollback.

Passing answer:

```text
A passing answer protects inventory/payment correctness, avoids sync side effects on the
critical path, uses reliable event publishing, exposes observability, and explains failure
recovery without hand-waving.
```

---

## 11. Mock 10: Gap-Fill Senior Ownership Round - 60 Minutes

Use this after reading the local setup, API management, workflow engine, cloud, FinOps,
privacy, and platform engineering sheets.

### Question 1

Prompt:

```text
How would you make the hotel booking system runnable and debuggable for new engineers locally?
```

Expected points:

- local golden path
- config/env documentation
- seed data
- service mocks or containers
- request ID propagation
- smoke test
- idempotency and outbox failure drills

### Question 2

Prompt:

```text
Design partner booking APIs and BookingConfirmed webhooks.
```

Expected points:

- API management vs gateway
- OAuth2/mTLS/API key decision
- quotas and rate limits
- developer portal and sandbox
- idempotency keys
- signed webhook payloads
- retry/DLQ
- partner analytics and deprecation

### Question 3

Prompt:

```text
The group booking workflow has 12 steps and waits up to 48 hours. Would you use a workflow engine?
```

Expected points:

- hand-rolled Saga vs workflow engine
- durable timers
- activity idempotency
- worker crash recovery
- workflow versioning
- stuck workflow alerts
- domain ownership remains in services

### Question 4

Prompt:

```text
Traffic grows 3x, cost grows 10x, and a privacy deletion request arrives during the same week.
What do you investigate and design?
```

Expected points:

- cost per request/booking
- retry/fan-out/log/trace/cross-region cost
- cost tags and dashboard
- PII inventory
- delete/anonymize workflow
- derived stores and partner exports
- audit evidence

### Question 5

Prompt:

```text
Your company now has 200 microservices with inconsistent deployment, observability, secrets,
and API standards. What platform strategy do you propose?
```

Expected points:

- golden path
- service templates
- service catalog
- automated guardrails
- maturity model
- platform vs app ownership
- exception process
- adoption plan

Passing answer:

```text
A passing answer treats microservices as a socio-technical system: local developer flow,
external API lifecycle, durable workflows, cloud runtime choices, cost, privacy, and platform
guardrails all become part of production ownership.
```

---

## 12. Final Mock Schedule

Recommended sequence:

| Day | Mock |
|---:|---|
| 1 | Starter concept round |
| 2 | Communication/contracts |
| 3 | Data consistency |
| 4 | Kafka/events |
| 5 | Resilience/scale |
| 6 | Observability/incident |
| 7 | Security |
| 8 | Kubernetes/platform |
| 9 | Full FAANG system design |
| 10 | Gap-fill senior ownership round |
| 11 | Repeat weakest two rounds |
