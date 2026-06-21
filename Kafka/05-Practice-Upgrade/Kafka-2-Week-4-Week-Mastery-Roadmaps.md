# Kafka 2 Week 4 Week Mastery Roadmaps

> Track: Kafka Interview Track - Practice Upgrade  
> Goal: convert the Kafka notes into a focused MAANG-level study plan.

Use the 2-week roadmap for interview acceleration. Use the 4-week roadmap for deeper mastery.

---

## 1. Daily Study Loop

Use this loop every day:

1. Read one focused topic sheet.
2. Answer 15 active recall questions.
3. Solve 1 scenario or mini-lab.
4. Speak a 3-minute summary aloud.
5. Score yourself with the rubric.
6. Write down 3 weak spots for the next day.

---

## 2. 2-Week Interview Acceleration Plan

Target: backend engineer who already knows distributed systems basics.

### Day 1: Kafka Core Flow

Read:

- `Kafka-Producer-Partition-Consumer-Group-Distributed-Flow.md`
- root index overview

Practice:

- 25 foundation recall questions
- Mock 1 fundamentals

Outcome:

```text
Explain producer -> topic -> partition -> broker log -> consumer group -> offset.
```

---

### Day 2: Broker Internals

Read:

- `Kafka-Broker-Internals-Architecture-Gold-Sheet.md`

Practice:

- broker internals recall
- broker failure scenario

Outcome:

```text
Explain leader, follower, ISR, replication factor, KRaft, and offline partitions.
```

---

### Day 3: Producer And Consumer Delivery

Read:

- `Kafka-Producer-Consumer-Delivery-Guarantees-Gold-Sheet.md`

Practice:

- Lab 1 producer config
- Lab 2 idempotent consumer

Outcome:

```text
Explain at-most-once, at-least-once, retries, offset commits, and duplicate windows.
```

---

### Day 4: Transactions And Idempotency

Read:

- `Kafka-Transactions-Exactly-Once-Idempotency-Gold-Sheet.md`

Practice:

- Mock 4 exactly-once
- duplicate payment scenario

Outcome:

```text
Explain Kafka EOS boundaries and external side-effect idempotency.
```

---

### Day 5: Schema And Event Design

Read:

- `Kafka-Schema-Registry-Event-Design-Gold-Sheet.md`

Practice:

- Lab 5 schema evolution test
- schema break scenario

Outcome:

```text
Design evolvable events with compatibility and semantic review.
```

---

### Day 6: Topic Design Retention Compaction

Read:

- `Kafka-Topic-Design-Retention-Compaction-Gold-Sheet.md`

Practice:

- Lab 4 topic design
- hot partition scenario

Outcome:

```text
Choose topic boundaries, keys, partitions, retention, and compaction intentionally.
```

---

### Day 7: Weekly Review And Capstone 1

Practice:

- 50 mixed active recall questions
- Scenario 25 marketplace platform
- score with capstone rubric

Outcome:

```text
Find weak spots in correctness, topic design, and operations before week 2.
```

---

### Day 8: Streams Connect CDC

Read:

- `Kafka-Streams-Connect-CDC-Gold-Sheet.md`

Practice:

- Lab 6 outbox with CDC
- Lab 11 Streams aggregation

Outcome:

```text
Choose between Kafka Streams, Connect, CDC, and plain consumers.
```

---

### Day 9: Operations Monitoring Security

Read:

- `Kafka-Operations-Monitoring-Security-Gold-Sheet.md`

Practice:

- Lab 7 lag triage
- Mock 7 incident debugging

Outcome:

```text
Debug lag, rebalances, poison records, ISR shrink, and broker incidents.
```

---

### Day 10: Advanced APIs

Read:

- `Kafka-Advanced-API-Platform-Concepts-Gold-Sheet.md`

Practice:

- Lab 9 safe replay with seek
- Lab 10 pause/resume backpressure

Outcome:

```text
Use seek, pause/resume, commits, AdminClient, static membership, and cooperative rebalancing responsibly.
```

---

### Day 11: Modern Platform Operations

Read:

- `Kafka-Modern-Platform-Operations-KRaft-Tiered-Storage-Multi-Tenant-Gold-Sheet.md`

Practice:

- Lab 13 KRaft dashboard
- Lab 17 partition reassignment runbook

Outcome:

```text
Explain KRaft, tiered storage, quotas, rack awareness, reassignment, capacity, and DR.
```

---

### Day 12: Security Governance

Read:

- `Kafka-Security-Governance-PII-Tenant-Isolation-Gold-Sheet.md`

Practice:

- Lab 14 multi-tenant guardrails
- Lab 15 payment topic governance

Outcome:

```text
Govern sensitive Kafka topics with classification, ACLs, retention, DLQ/replay, and audit.
```

---

### Day 13: FAANG Scenario Bank

Read:

- `Kafka-FAANG-Scenario-Drill-Bank-Gold-Sheet.md`
- `Kafka-Production-Scenarios-Interview-Stress-Checklist.md`

Practice:

- 5 scenario drills
- 1 full mock

Outcome:

```text
Build fluency under pressure.
```

---

### Day 14: Final Mock Day

Practice:

- Mock 10 MAANG capstone
- 50 active recall questions
- score every section

Pass bar:

- Fundamentals: 5
- Delivery/correctness: 4+
- Ops/debugging: 4+
- Security/governance: 4+
- Capstone: 4+

---

## 3. 4-Week Mastery Plan

Target: strong MAANG readiness with deeper retention and more scenario reps.

---

## Week 1: Foundations And Correctness

### Focus

- Kafka core flow
- broker internals
- producer delivery
- consumer delivery
- offset commits
- idempotency

### Files

- `Kafka-Producer-Partition-Consumer-Group-Distributed-Flow.md`
- `Kafka-Broker-Internals-Architecture-Gold-Sheet.md`
- `Kafka-Producer-Consumer-Delivery-Guarantees-Gold-Sheet.md`
- `Kafka-Transactions-Exactly-Once-Idempotency-Gold-Sheet.md`

### Practice

- Labs 1, 2, 3
- Mocks 1, 2, 4
- 100 active recall questions

### Week 1 Gate

```text
You can explain ordering, durability, offset commits, duplicate windows, and exactly-once boundaries.
```

---

## Week 2: Event Design And Processing

### Focus

- schema registry
- event contracts
- topic design
- retention and compaction
- Kafka Streams
- Kafka Connect
- CDC/outbox

### Files

- `Kafka-Schema-Registry-Event-Design-Gold-Sheet.md`
- `Kafka-Topic-Design-Retention-Compaction-Gold-Sheet.md`
- `Kafka-Streams-Connect-CDC-Gold-Sheet.md`

### Practice

- Labs 4, 5, 6, 11, 12
- Mocks 5, 6
- Scenarios 10-12 and 18-20

### Week 2 Gate

```text
You can design event contracts and choose Streams, Connect, CDC, or consumer apps based on requirements.
```

---

## Week 3: Operations Platform Security

### Focus

- lag and incident triage
- broker/platform health
- KRaft
- tiered storage
- reassignment
- quotas
- DR
- PII and tenant governance

### Files

- `Kafka-Operations-Monitoring-Security-Gold-Sheet.md`
- `Kafka-Advanced-API-Platform-Concepts-Gold-Sheet.md`
- `Kafka-Modern-Platform-Operations-KRaft-Tiered-Storage-Multi-Tenant-Gold-Sheet.md`
- `Kafka-Security-Governance-PII-Tenant-Isolation-Gold-Sheet.md`

### Practice

- Labs 7, 8, 9, 10, 13, 14, 15, 16, 17, 18
- Mocks 7, 8, 9
- platform and governance scenario drills

### Week 3 Gate

```text
You can operate and govern a shared Kafka platform safely.
```

---

## Week 4: Capstone And Interview Simulation

### Focus

- MAANG system design style answers
- production incident fluency
- full event-platform architecture
- trade-off articulation
- speed and precision

### Files

- `Kafka-FAANG-Scenario-Drill-Bank-Gold-Sheet.md`
- `Kafka-Production-Scenarios-Interview-Stress-Checklist.md`
- all `05-Practice-Upgrade` files

### Practice

- Mock 10 twice
- 10 scenario drills
- 3 postmortems
- 200 active recall questions
- final rubric review

### Week 4 Gate

```text
You can deliver a full Kafka architecture with correctness, operations, governance, and DR in 45-60 minutes.
```

---

## 4. Topic Weighting For MAANG Interviews

| Area | Weight |
|---|---:|
| Core flow and partitioning | 15% |
| Delivery semantics and idempotency | 20% |
| Topic/schema/event design | 15% |
| Operations/debugging | 20% |
| Streams/Connect/CDC | 10% |
| Platform operations and DR | 10% |
| Security/governance | 10% |

---

## 5. Final Readiness Checklist

You are ready when you can:

1. Explain Kafka accurately in 5 minutes.
2. Debug consumer lag with a structured runbook.
3. Design idempotent producer and consumer flows.
4. Explain Kafka exactly-once boundaries without overclaiming.
5. Design schemas and event contracts safely.
6. Choose topic keys, partitions, retention, and compaction.
7. Use Streams, Connect, CDC, or plain consumers intentionally.
8. Operate KRaft-era Kafka with dashboards and runbooks.
9. Govern sensitive multi-tenant topics.
10. Deliver one full marketplace/event-platform capstone at senior level.

---

## 6. Final Message To Remember

```text
Kafka interview strength is not memorizing configs. It is knowing the data flow, the failure
windows, the guarantees, the operational signals, and the governance controls well enough to
make safe trade-offs under pressure.
```
