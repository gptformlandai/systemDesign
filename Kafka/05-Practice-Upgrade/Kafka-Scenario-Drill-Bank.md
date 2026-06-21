# Kafka Scenario Drill Bank

> Track: Kafka Interview Track - Practice Upgrade  
> Goal: practice Kafka design and incident scenarios under interview pressure.

Use this after reading the concept sheets.

---

## 1. Answer Format

For design scenarios:

```text
requirements -> event model -> topic/key/partition strategy -> delivery semantics -> failure handling -> observability -> trade-offs
```

For debugging scenarios:

```text
symptom -> blast radius -> topic/partition/group -> client/broker metrics -> hypothesis -> mitigation -> prevention
```

For governance scenarios:

```text
owner -> data classification -> schema -> ACLs -> retention -> replay/audit -> operational guardrails
```

---

## 2. Foundation Scenarios

### Scenario 1: Order Event Pipeline

Prompt:

```text
Design Kafka events for an order service consumed by inventory, payment, email, and analytics.
```

Must include:

- event type and envelope
- topic boundary
- partition key choice
- consumer groups for fan-out
- schema registry compatibility
- idempotent consumers
- DLQ/retry strategy
- monitoring

---

### Scenario 2: Ordering Per Customer

Prompt:

```text
All events for a customer must be processed in order.
```

Answer should include:

- key by customer id
- ordering only within partition
- hot customer risk
- consumer group partition ownership
- replay and idempotency

---

### Scenario 3: More Consumers Than Partitions

Prompt:

```text
A team scales consumers from 6 to 20 but throughput does not improve.
```

Answer should include:

- one partition assigned to one consumer in a group
- idle consumers
- check partition count and bottleneck
- scaling consumers helps only up to partition count unless processing model changes

---

## 3. Delivery And Correctness Scenarios

### Scenario 4: Consumer Crashes After DB Write

Prompt:

```text
Consumer writes to DB, crashes before committing offset, then reprocesses message.
```

Answer should include:

- at-least-once duplicate window
- idempotent consumer
- processed_event table or unique business key
- commit after successful side effect
- monitor duplicates

---

### Scenario 5: Duplicate Payment Event

Prompt:

```text
Payment is charged twice after Kafka replay.
```

Answer should include:

- external side effect not protected by Kafka exactly-once
- idempotency key/payment id unique constraint
- replay governance
- transactional outbox/inbox where appropriate

---

### Scenario 6: Exactly-Once Claim

Prompt:

```text
Candidate says Kafka guarantees exactly once. Challenge them.
```

Strong answer:

```text
Kafka can provide idempotent production and transactional consume-transform-produce inside Kafka.
External side effects still need idempotency, dedupe, or transactional integration.
```

---

## 4. Topic Design Scenarios

### Scenario 7: Hot Partition

Prompt:

```text
One partition has much higher traffic and consumer lag than others.
```

Answer should include:

- inspect key distribution
- identify hot key
- change key strategy carefully
- split hot entity if business allows
- increase partitions only if key distribution benefits
- ordering trade-off

---

### Scenario 8: Increase Partitions In Production

Prompt:

```text
Topic needs more throughput. Should we increase partitions?
```

Answer should include:

- affects key-to-partition mapping
- may break per-key ordering if historical and future events move
- helps if consumers are partition-limited
- update capacity, placement, and monitoring

---

### Scenario 9: Latest User Profile State

Prompt:

```text
Consumers need latest user profile state, not every historical change.
```

Answer should include:

- compacted topic keyed by user id
- tombstones for deletes
- consumers must handle snapshot/changelog semantics
- compaction is not immediate deletion

---

## 5. Schema Event Design Scenarios

### Scenario 10: Breaking Schema Change

Prompt:

```text
Producer removes a field and old consumers fail.
```

Answer should include:

- compatibility mode
- schema registry CI gate
- additive optional changes first
- deprecation window
- semantic compatibility review

---

### Scenario 11: Semantic Break

Prompt:

```text
A field keeps same type but changes meaning.
```

Answer should include:

- schema compatibility cannot catch all semantic changes
- version event or add new field
- communicate contract change
- consumer tests

---

### Scenario 12: Schema Registry Unavailable

Prompt:

```text
Schema Registry is down and producers fail.
```

Answer should include:

- client schema cache behavior
- registry HA
- impact on new schema registration vs cached schemas
- rollback/restore registry
- monitor compatibility failures

---

## 6. Operations Scenarios

### Scenario 13: Consumer Lag Rising

Prompt:

```text
Lag rises after a deployment.
```

Answer should include:

- check group/topic/partition lag
- processing latency/errors
- rebalance rate
- `max.poll.interval.ms`
- downstream DB/API slowness
- poison message
- rollback or pause/retry/DLQ

---

### Scenario 14: Under-Replicated Partitions

Prompt:

```text
Under-replicated partitions spike after broker issue.
```

Answer should include:

- broker health
- disk/network saturation
- follower catch-up
- ISR status
- avoid unsafe changes
- restore replicas and monitor recovery

---

### Scenario 15: Offline Partitions

Prompt:

```text
Some partitions have no leader.
```

Answer should include:

- unavailable partition
- check broker/ISR/controller health
- restore in-sync replica
- unsafe leader election only if business accepts data loss
- incident communication

---

### Scenario 16: Rebalance Storm

Prompt:

```text
Consumers keep rebalancing and throughput collapses.
```

Answer should include:

- heartbeat/session timeout
- processing exceeds poll interval
- rolling deploy churn
- cooperative rebalancing/static membership
- pause/resume or controlled worker model

---

### Scenario 17: Poison Message

Prompt:

```text
One bad record blocks a partition.
```

Answer should include:

- identify topic/partition/offset
- retry with limit
- DLQ with metadata
- alert owner
- replay after fix
- avoid infinite retry blocking progress

---

## 7. Streams Connect CDC Scenarios

### Scenario 18: CDC Outbox

Prompt:

```text
Publish order events reliably from an OLTP database.
```

Answer should include:

- write business row and outbox row in same DB transaction
- Debezium captures outbox
- Kafka event produced from outbox
- idempotent consumers
- schema contract

---

### Scenario 19: Kafka Streams State Recovery

Prompt:

```text
Streams app restarts and takes long time to recover state.
```

Answer should include:

- state store restore from changelog
- standby replicas if configured
- state size and disk
- changelog topic health
- partition assignment

---

### Scenario 20: Connect Sink Failing

Prompt:

```text
Kafka Connect sink task keeps failing.
```

Answer should include:

- connector/task status
- task logs
- bad record vs sink unavailable
- DLQ settings
- schema mismatch
- offset progress

---

## 8. Platform Governance Scenarios

### Scenario 21: Shared Kafka Platform

Prompt:

```text
40 teams share one Kafka cluster. Make it safe.
```

Answer should include:

- topic ownership
- ACLs
- quotas
- retention defaults
- schema compatibility
- PII classification
- monitoring and onboarding workflow

---

### Scenario 22: Sensitive Payment Topic

Prompt:

```text
Payment events include PII and are consumed by many teams.
```

Answer should include:

- minimize event payload
- publish IDs over raw PII
- strict ACLs
- DLQ/replay governance
- retention policy
- audit consumers

---

### Scenario 23: Cross-Region DR

Prompt:

```text
Design Kafka DR for critical order events.
```

Answer should include:

- RPO/RTO
- replication technology
- schemas/ACLs/topic configs
- consumer offsets/failover
- idempotent downstreams
- active-active caution

---

### Scenario 24: Replay Old Events

Prompt:

```text
A bug requires replaying 3 days of events.
```

Answer should include:

- exact topic/partition/time window
- separate replay group/job
- idempotency check
- downstream capacity
- rate limit
- audit approval
- monitor side effects

---

## 9. Capstone Scenarios

### Scenario 25: Marketplace Event Platform

Prompt:

```text
Design Kafka for marketplace orders, payments, inventory, fraud, notifications, analytics, and audit.
```

Strong answer includes:

- domain topics and keys
- schema registry
- outbox for DB-to-Kafka reliability
- idempotent consumers
- retry/DLQ strategy
- Streams/Connect choices
- monitoring dashboard
- PII governance
- DR plan

---

### Scenario 26: Real-Time Fraud Pipeline

Prompt:

```text
Build low-latency payment fraud scoring using Kafka.
```

Strong answer includes:

- payment events keyed by account/customer
- fraud feature enrichment
- latency SLA
- Kafka Streams or consumer app trade-off
- state store/changelog
- idempotency and replay
- schema evolution
- monitoring

---

### Scenario 27: Multi-Tenant Event Platform Review

Prompt:

```text
Review Kafka design for a multi-tenant SaaS platform.
```

Strong answer includes:

- tenant topic strategy
- tenant field limitations
- ACLs and quotas
- sensitive data controls
- schema ownership
- retention and deletion policy
- audit and replay controls

---

## 10. Completion Gate

You are ready when you can solve:

1. 5 delivery/correctness scenarios.
2. 5 operational incident scenarios.
3. 3 schema/topic design scenarios.
4. 3 Streams/Connect/CDC scenarios.
5. 3 governance/DR scenarios.
6. 1 full marketplace or booking event-platform capstone.
