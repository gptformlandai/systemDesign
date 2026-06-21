# Kafka Mock Interview Scripts

> Track: Kafka Interview Track - Practice Upgrade  
> Goal: practice Kafka like a real senior/backend/platform interview.

Use these scripts aloud. Record answers when possible.

---

## 1. How To Run A Mock

Rules:

1. Timebox the round.
2. Answer with diagrams or spoken structure.
3. Name assumptions early.
4. Always explain trade-offs.
5. End with monitoring and failure handling.
6. Score immediately using the Kafka rubric.

Default answer format:

```text
requirements -> design -> correctness -> operations -> trade-offs -> risks
```

---

## 2. Mock 1: Kafka Fundamentals

Time: 30 minutes.

### Opening

```text
Explain Kafka from producer to consumer group as if teaching a new backend engineer.
```

Expected areas:

- topic and partition
- broker log
- partition leader
- offset
- consumer group
- ordering boundary
- fan-out across groups

### Follow-ups

1. What happens if there are 10 consumers and 6 partitions?
2. How does a producer choose a partition?
3. What does Kafka guarantee about ordering?
4. What is consumer lag?
5. What happens during rebalance?

### Strong Close

```text
Kafka is a durable partitioned log. Ordering is per partition, scaling is through partitions,
and fan-out is through consumer groups.
```

---

## 3. Mock 2: Producer And Consumer Delivery

Time: 45 minutes.

### Opening

```text
Design reliable producer and consumer settings for payment events.
```

Expected areas:

- `acks=all`
- idempotent producer
- retries and delivery timeout
- partition key
- manual offset commits
- idempotent consumer
- retry and DLQ

### Follow-ups

1. When can duplicates still happen?
2. What does idempotent producer solve?
3. What does it not solve?
4. What if consumer writes DB then crashes before committing offset?
5. How do you replay safely?

### Red Flags

- claims Kafka removes need for idempotency
- commits offsets before side effects without naming at-most-once trade-off
- ignores downstream duplicate side effects

---

## 4. Mock 3: Broker Internals And Operations

Time: 45 minutes.

### Opening

```text
A broker fails in a 6-broker Kafka cluster. Walk me through what happens.
```

Expected areas:

- partition leaders on failed broker unavailable temporarily
- controller elects new leaders from ISR
- producers/consumers refresh metadata
- replicas catch up after broker returns
- under-replicated partitions may spike
- no data loss if in-sync replicas and configs are healthy

### Follow-ups

1. What is ISR?
2. Why does `min.insync.replicas` matter?
3. What is an offline partition?
4. What is unsafe leader election?
5. What metrics do you inspect first?

---

## 5. Mock 4: Exactly-Once And Transactions

Time: 45 minutes.

### Opening

```text
Explain Kafka exactly-once semantics and where the boundary ends.
```

Expected areas:

- idempotent producer
- transactional producer
- consume-transform-produce transaction
- transactional offsets
- `read_committed`
- producer fencing
- external DB/API side effects need idempotency/outbox/inbox

### Follow-ups

1. What is `transactional.id`?
2. What is producer fencing?
3. Why does Kafka EOS not make Stripe charges exactly-once?
4. When would you choose outbox over Kafka transactions?
5. How do you design an idempotent consumer?

---

## 6. Mock 5: Schema And Topic Design

Time: 45 minutes.

### Opening

```text
Design Kafka topics and schemas for an order lifecycle.
```

Expected areas:

- domain topic boundaries
- event envelope
- stable event ids
- partition key choice
- schema registry
- compatibility mode
- retention/compaction
- PII minimization

### Follow-ups

1. Backward vs forward compatibility?
2. Is changing field meaning a breaking change?
3. When do you use compaction?
4. What happens if partition count changes?
5. How do you handle event versioning?

---

## 7. Mock 6: Kafka Streams Connect CDC

Time: 45 minutes.

### Opening

```text
A company wants reliable events from its orders database and real-time fraud aggregates. What Kafka tools do you use?
```

Expected areas:

- transactional outbox
- Debezium CDC
- Kafka Connect source connector
- Kafka Streams for fraud aggregates
- state stores
- changelog topics
- schema registry
- idempotent consumers

### Follow-ups

1. Why not publish directly after DB commit?
2. What is the outbox pattern?
3. KStream vs KTable?
4. How does Streams recover state?
5. How do you debug a failing sink connector?

---

## 8. Mock 7: Incident Debugging

Time: 60 minutes.

### Opening

```text
Consumer lag has been rising for 45 minutes after a deploy. Production impact is increasing.
```

Expected response:

1. Confirm impacted topic, group, partitions, and business flow.
2. Check lag distribution by partition.
3. Check consumer errors, processing latency, and rebalance rate.
4. Check downstream DB/API saturation.
5. Look for poison message at stuck offset.
6. Mitigate with rollback, pause/resume, scaling, DLQ, or controlled replay.
7. Add prevention actions.

### Follow-ups

1. What if only one partition lags?
2. What if all partitions lag equally?
3. What if consumers are rebalancing constantly?
4. What if lagging records are older than retention?
5. What dashboards should have caught this sooner?

---

## 9. Mock 8: Modern Platform Operations

Time: 60 minutes.

### Opening

```text
You own a shared Kafka platform used by 50 teams. Design operating guardrails.
```

Expected areas:

- topic ownership registry
- onboarding workflow
- schema compatibility
- quotas
- ACLs/RBAC
- retention defaults
- KRaft quorum health
- broker capacity
- partition reassignment process
- tiered storage trade-offs
- DR testing
- platform dashboards

### Follow-ups

1. What do you monitor in KRaft?
2. How do quotas prevent noisy neighbors?
3. What does tiered storage change operationally?
4. Why is active-active Kafka difficult?
5. What belongs in a DR test?

---

## 10. Mock 9: Security Governance

Time: 45 minutes.

### Opening

```text
A payments topic contains sensitive customer data and many teams want access. How do you govern it?
```

Expected areas:

- classify data
- remove raw PII where possible
- use IDs/tokenization/field encryption when needed
- least-privilege ACLs
- schema review
- strict retention
- DLQ restrictions
- replay approval
- audit unexpected consumers

### Follow-ups

1. Why is a DLQ sensitive?
2. Why is tenant_id not complete isolation?
3. What should be audited?
4. How do you handle deletion/retention expectations?
5. How do you approve a new consumer?

---

## 11. Mock 10: MAANG Capstone

Time: 75 minutes.

### Opening

```text
Design an event-driven marketplace platform using Kafka for orders, payments, inventory, fraud, notifications, analytics, and audit.
```

Expected structure:

1. Clarify scale, latency, durability, compliance, and replay needs.
2. Define domain events and topic boundaries.
3. Choose keys and partition strategy.
4. Define schemas and compatibility rules.
5. Use outbox/CDC where DB consistency matters.
6. Design idempotent consumers and retry/DLQ flow.
7. Pick Streams/Connect/plain consumers where appropriate.
8. Add observability for lag, throughput, errors, brokers, schemas, DLQs.
9. Add security, PII, ACLs, and replay governance.
10. Add DR and capacity planning.

### Follow-ups

1. How do you avoid duplicate payments?
2. How do you recover from schema breaks?
3. How do you handle a hot seller or hot customer?
4. How do you replay a bad analytics window?
5. How do you migrate this across regions?

---

## 12. Self Review Questions

After each mock, answer:

1. Did I define the ordering boundary?
2. Did I define delivery semantics accurately?
3. Did I name the exact failure window?
4. Did I include idempotency where needed?
5. Did I include metrics and alerts?
6. Did I include security/governance when data is sensitive?
7. Did I avoid overclaiming exactly-once?
8. Did I explain trade-offs clearly?

---

## 13. Completion Gate

You are mock-ready when:

1. Fundamentals answer fits in 5 minutes.
2. Delivery and correctness answer survives challenge questions.
3. Operations scenario has a clean triage order.
4. Capstone includes design, correctness, ops, security, and DR.
5. You can say what Kafka guarantees and what the application must still guarantee.
