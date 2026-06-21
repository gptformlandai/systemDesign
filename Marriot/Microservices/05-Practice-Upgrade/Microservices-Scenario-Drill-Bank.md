# Microservices Scenario Drill Bank

> Track: Microservices Interview Track - Group 5 Practice Upgrade  
> Goal: practice realistic interview prompts under pressure.

Use this after reading the core sheets. Each scenario should be answered aloud, preferably
with a quick diagram.

---

## 1. How To Use These Drills

For every scenario, answer in this format:

```text
1. Clarify requirement.
2. Identify owning service and data boundary.
3. Choose sync/async communication.
4. State consistency model.
5. Add failure handling.
6. Add observability and security.
7. Mention testing/deployment/migration if relevant.
8. Close with trade-off.
```

Time boxes:

| Drill Type | Time |
|---|---:|
| quick concept answer | 90 seconds |
| focused design scenario | 5 minutes |
| production debugging scenario | 7 minutes |
| full capstone design | 20-30 minutes |

---

## 2. Starter Scenarios

### Scenario 1: Microservices vs Monolith

Prompt:

```text
A startup with 5 engineers wants microservices for a new hotel booking MVP. What do you
recommend?
```

Strong direction:

```text
Prefer modular monolith first unless independent scale/team ownership is already clear.
```

Must mention:

- small team coordination cost
- unclear boundaries early
- modular monolith with clean modules
- split later when boundaries stabilize

---

### Scenario 2: Shared Database

Prompt:

```text
Booking Service and Payment Service share the same database because joins are easy. Is this
okay?
```

Strong direction:

```text
Accept only as temporary migration state. Long term, each service owns its data and exposes
APIs/events/read models.
```

Must mention:

- data ownership
- deployment coupling
- schema coupling
- migration plan

---

### Scenario 3: Chatty APIs

Prompt:

```text
Booking details page makes 18 synchronous service calls and p99 is poor.
```

Strong direction:

```text
Reduce fan-out, add BFF/API composition carefully, cache/read model where appropriate, and
move non-critical work async.
```

Must mention:

- tail latency
- timeout budgets
- parallel calls vs aggregation
- read model for repeated queries

---

## 3. Communication Scenarios

### Scenario 4: REST vs gRPC

Prompt:

```text
Internal Pricing Service is called thousands of times per second by Booking Service. Should
it be REST or gRPC?
```

Answer shape:

- ask about latency, schema, clients, debugging
- gRPC may fit internal typed low-latency calls
- REST may be simpler for public/external APIs
- both still need timeouts, auth, observability

---

### Scenario 5: Gateway Becoming Heavy

Prompt:

```text
Gateway now calculates prices, checks loyalty, and decides booking status.
```

Diagnosis:

```text
Gateway became a domain monolith.
```

Fix:

- move domain logic to owning services
- keep gateway for edge concerns
- use BFF for client shaping only
- use orchestrator if workflow needs coordination

---

### Scenario 6: Breaking API Change

Prompt:

```text
Booking Service wants to rename `bookingStatus` to `status` in response JSON.
```

Safe rollout:

- add `status` while keeping `bookingStatus`
- update consumers
- monitor usage
- deprecate with deadline
- remove later
- contract tests

---

## 4. Data Consistency Scenarios

### Scenario 7: Prevent Double Booking

Prompt:

```text
Two users try to reserve the last room at the same time.
```

Strong direction:

- Availability Service owns inventory
- use DB locking or optimistic versioning
- transaction protects invariant
- Booking Service does not directly write inventory DB

---

### Scenario 8: Payment Timeout

Prompt:

```text
Payment authorization times out. Do you mark booking failed?
```

Strong direction:

```text
Timeout means unknown, not automatically failed.
```

Must mention:

- idempotency key
- reconciliation
- pending/unknown state
- safe retry
- release inventory only after confirmed failure or timeout policy

---

### Scenario 9: Lost Event Risk

Prompt:

```text
Booking DB update commits, but Kafka publish fails.
```

Answer:

- transactional outbox
- relay publishes later
- monitor outbox age
- consumers idempotent

---

### Scenario 10: Duplicate Event

Prompt:

```text
BookingConfirmed event is processed twice by Loyalty Service.
```

Answer:

- processed_event table
- unique key consumer + event id
- ledger idempotency
- commit offset after processing

---

### Scenario 11: Compensation Fails

Prompt:

```text
Payment failed, but inventory release compensation also fails.
```

Answer:

- persist compensation state
- retry with backoff
- alert/manual repair
- do not hide inconsistency
- dashboard compensation failures

---

## 5. Kafka Scenarios

### Scenario 12: Lag Rising

Prompt:

```text
Notification consumer lag keeps increasing.
```

Debug path:

- producer rate vs consumer rate
- handler error rate
- downstream provider latency
- partition count vs consumer count
- hot partition
- rebalance frequency
- DLQ/retry volume

---

### Scenario 13: Hot Partition

Prompt:

```text
One Kafka partition has massive lag but others are normal.
```

Likely causes:

- skewed key
- one large tenant/hotel
- poison message blocking partition
- slow processing for one aggregate

Fix:

- inspect key distribution
- DLQ poison message
- redesign key if needed
- split topic/partition strategy carefully

---

### Scenario 14: Replay Old Events

Prompt:

```text
Reporting Service needs to rebuild its read model from old booking events.
```

Answer:

- replay from earliest offset or event store
- idempotent projection
- version-aware event handling
- isolate rebuild from live processing if needed
- validate counts/checksums

---

## 6. Resilience Scenarios

### Scenario 15: Retry Storm

Prompt:

```text
Payment Service slows down. Upstream services retry aggressively and traffic triples.
```

Mitigation:

- reduce/disable retries
- circuit breaker
- timeout budgets
- load shedding
- retry budget
- queue async work if safe
- protect Payment Service

---

### Scenario 16: DB Pool Exhaustion

Prompt:

```text
Booking Service threads wait for DB connections and p99 spikes.
```

Debug:

- DB pool wait time
- active connections
- slow queries
- transaction duration
- connection leaks
- recent traffic/deploy

Fix:

- optimize queries
- tune pool with DB capacity
- add timeouts
- reduce fan-out/long transactions

---

### Scenario 17: Downstream Quota

Prompt:

```text
External email provider allows 100 requests/sec. Notification Service receives 500/sec.
```

Answer:

- rate limit outbound calls
- queue and smooth traffic
- backpressure
- retry with delay
- provider-specific circuit breaker
- alert on backlog age

---

## 7. Observability Scenarios

### Scenario 18: p99 Latency Spike

Prompt:

```text
Checkout p99 increases from 400 ms to 5 seconds. Error rate is low.
```

Debug:

- confirm scope/region/version
- trace slow requests
- inspect downstream spans
- DB and pool metrics
- retry counts
- CPU throttling
- recent deploy/config

---

### Scenario 19: 5xx After Deploy

Prompt:

```text
New Booking Service version causes 5xx after canary starts.
```

Answer:

- compare version metrics
- rollback or shift traffic
- inspect logs by version
- check config, secret, schema, contract
- verify readiness and dependency compatibility

---

### Scenario 20: Outbox Lag

Prompt:

```text
Booking confirmations succeed but downstream services do not receive events for 20 minutes.
```

Answer:

- check outbox oldest age
- relay errors
- broker connectivity
- schema errors
- retry count
- alert and replay after fix

---

## 8. Security Scenarios

### Scenario 21: Gateway Bypass

Prompt:

```text
Internal caller hits Booking Service directly and cancels another user's booking.
```

Answer:

- service-level authorization missing
- verify service/user identity
- enforce ownership check in Booking Service
- network policy
- audit log

---

### Scenario 22: Secret Rotation Outage

Prompt:

```text
Payment Service fails after rotating provider credentials.
```

Answer:

- compare rotation timeline
- validate secret mount/env reload
- dual credentials/canary secret
- rollback credential if needed
- improve runbook

---

### Scenario 23: Tenant Data Leak

Prompt:

```text
Tenant A sees booking data from Tenant B in search results.
```

Answer:

- tenant boundary missing in query/index/cache
- audit affected data
- fix authorization/data filtering
- invalidate bad cache/index
- add tests and monitoring

---

## 9. Kubernetes Scenarios

### Scenario 24: Bad Liveness Probe

Prompt:

```text
Database blip causes all Booking pods to restart.
```

Diagnosis:

```text
Liveness incorrectly checks DB.
```

Fix:

- liveness checks process health
- readiness checks traffic serving ability
- use dependency status carefully

---

### Scenario 25: CPU Throttling

Prompt:

```text
Latency spikes but CPU average looks normal.
```

Answer:

- check CPU throttling metrics
- review CPU limits
- p99 and queue wait
- tune limits/requests
- load test

---

### Scenario 26: HPA Does Not Reduce Kafka Lag

Prompt:

```text
Consumer replicas scale from 5 to 20, but lag stays high.
```

Possible reasons:

- topic has only 5 partitions
- downstream bottleneck
- hot partition
- slow handler
- poison messages
- rebalance churn

---

## 10. Platinum Capstone Scenarios

### Scenario 27: Full Hotel Booking System

Prompt:

```text
Design hotel search, booking, payment, notification, loyalty, reporting, and operations.
```

Must include:

- service boundaries
- data ownership
- saga/outbox/idempotency
- sync/async decisions
- Kafka topics/events
- observability dashboard
- security model
- deployment and rollback

---

### Scenario 28: Monolith Migration

Prompt:

```text
A monolith handles booking, inventory, payment, and notification. Migrate safely.
```

Must include:

- modularize first
- choose first extraction by boundary/risk
- anti-corruption layer
- data migration/reconciliation
- canary traffic
- rollback
- deprecate old path

---

### Scenario 29: Multi-Region

Prompt:

```text
Design multi-region booking with RTO 15 minutes and RPO 5 minutes.
```

Must include:

- active-passive vs active-active
- data replication
- inventory correctness
- failover steps
- DR testing
- user experience during failover

---

### Scenario 30: Architecture Critique

Prompt:

```text
A candidate proposes 25 services, shared database, synchronous calls everywhere, and one
large E2E test suite. Review it.
```

Expected critique:

- distributed monolith
- no data ownership
- high latency/failure coupling
- poor test strategy
- missing observability/security
- recommend modular monolith or boundary redesign

---

## 11. Self-Scoring

For each scenario, score 1-5:

| Score | Meaning |
|---:|---|
| 1 | vague, pattern names only |
| 2 | basic concept but misses failure/trade-off |
| 3 | correct design, limited production depth |
| 4 | strong design with failure, observability, security |
| 5 | owner-level answer with migration, testing, cost, and trade-offs |

Target:

```text
Most senior interviews require consistent 4s. FAANG-level system design needs several 5s
under follow-up pressure.
```
