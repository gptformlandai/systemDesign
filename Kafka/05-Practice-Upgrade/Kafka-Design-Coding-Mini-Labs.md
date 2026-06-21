# Kafka Design Coding Mini Labs

> Track: Kafka Interview Track - Practice Upgrade  
> Goal: turn Kafka concepts into small buildable design/coding exercises.

Each lab should take 45-120 minutes.

---

## 1. Lab Output Rules

For every lab, produce:

1. Short design note.
2. Topic/key/config choices.
3. Pseudocode or Java/Spring sketch if useful.
4. Failure mode notes.
5. Metrics to monitor.
6. 60-second interview explanation.

---

## 2. Lab 1: Producer Config For Order Events

Build/sketch:

- producer config for durable order events
- `acks=all`
- idempotence enabled
- bounded `delivery.timeout.ms`
- compression
- key by `orderId` or `customerId` with ordering trade-off

Deliverable:

```text
Explain how durability, ordering, batching, and retry behavior interact.
```

---

## 3. Lab 2: Idempotent Consumer

Build/sketch:

- consumer reads `OrderCreated`
- writes downstream DB row
- `processed_events` table keyed by event id
- commit offset after successful transaction

Test mentally:

- crash after DB write before offset commit
- same event is re-read
- unique key prevents duplicate side effect

---

## 4. Lab 3: Retry Topic And DLQ Flow

Design:

```text
main topic -> retry topic with delay/backoff -> DLQ after max attempts
```

Must include:

- error metadata
- original topic/partition/offset
- retry count
- owner alert
- replay procedure
- no infinite poison-message loop

---

## 5. Lab 4: Topic Design For Booking Platform

Design topics:

- `booking.created.v1`
- `booking.cancelled.v1`
- `payment.authorized.v1`
- `inventory.adjusted.v1`

For each:

- owner
- key
- partition count assumption
- retention
- compaction or delete
- schema subject
- PII classification

---

## 6. Lab 5: Schema Evolution Test

Build/sketch:

- v1 event schema
- v2 safe additive optional field
- v3 breaking field removal
- CI compatibility gate

Deliverable:

```text
Explain backward compatibility and semantic compatibility.
```

---

## 7. Lab 6: Outbox With CDC

Design:

- `orders` table
- `outbox_events` table
- one DB transaction writes order + outbox row
- Debezium captures outbox row
- Kafka topic receives event

Must include:

- event id
- aggregate id
- event type
- payload
- created timestamp
- idempotent downstream consumer

---

## 8. Lab 7: Consumer Lag Triage Runbook

Write runbook for:

```text
Lag rises on payment-risk-consumer after deploy.
```

Include:

- topic/group/partition lag
- processing latency
- consumer errors
- rebalance rate
- downstream DB/API health
- poison message check
- rollback/pause/retry/DLQ mitigation

---

## 9. Lab 8: Hot Partition Investigation

Given:

```text
One partition has 90 percent of traffic.
```

Deliverable:

- identify key distribution
- find hot key
- explain ordering trade-off
- redesign key or split workload
- explain why adding partitions may not fix one hot key

---

## 10. Lab 9: Safe Replay With `seek`

Design replay job:

- separate consumer group
- identify topic/partition/time window
- lookup offsets by timestamp
- `seek` to start offset
- rate limit processing
- require idempotent downstream
- audit replay owner and reason

Trap to explain:

```text
Seeking production consumer group offsets can skip or duplicate work if done carelessly.
```

---

## 11. Lab 10: Pause/Resume Backpressure

Build/sketch:

- consumer sees downstream DB saturation
- pauses selected partitions
- continues polling to maintain group membership
- resumes after recovery
- alerts if pause too long

Deliverable:

```text
Explain why pause/resume is backpressure, not a permanent error-handling strategy.
```

---

## 12. Lab 11: Kafka Streams Aggregation

Design:

- input topic: `payment.authorized.v1`
- aggregate total amount by merchant per 5-minute window
- state store
- changelog topic
- output topic

Must include:

- windowing
- late events behavior
- state recovery
- partitioning by merchant
- monitoring

---

## 13. Lab 12: Kafka Connect Sink Failure

Write runbook:

- connector status
- task status
- task logs
- DLQ topic
- schema mismatch
- sink availability
- offset progress
- restart vs fix config decision

---

## 14. Lab 13: KRaft Health Dashboard

Design dashboard panels:

- active controller changes
- controller quorum health
- metadata request latency
- broker registration errors
- topic/admin operation failures
- offline partitions
- under-replicated partitions

Deliverable:

```text
Explain why metadata health matters even if producers/consumers are still partly working.
```

---

## 15. Lab 14: Multi-Tenant Kafka Guardrails

Design a platform onboarding template:

- topic owner
- schema subject
- PII classification
- allowed producers
- allowed consumers
- ACLs
- quotas
- retention
- DLQ policy
- replay approval
- dashboard ownership

---

## 16. Lab 15: Sensitive Payment Topic Governance

Design controls for payment events:

- no raw card data
- minimize PII
- strict ACLs
- short DLQ retention
- schema review
- replay approval
- audit unexpected consumers
- field tokenization if needed

---

## 17. Lab 16: Cross-Region DR Plan

Design:

- RPO/RTO
- replicated topics
- schema registry replication/availability
- ACL and secret readiness
- consumer offset strategy
- failover runbook
- downstream idempotency
- failback considerations

---

## 18. Lab 17: Partition Reassignment Runbook

Write runbook for adding brokers:

- identify imbalance
- plan reassignment batches
- throttle movement
- monitor under-replicated partitions
- monitor broker network/disk
- verify leader balance
- pause if latency rises

---

## 19. Lab 18: Storage And Retention Estimate

Calculate:

```text
messages_per_second * avg_message_bytes * 86400 * retention_days * replication_factor / compression_ratio
```

Deliverable:

- storage estimate
- effect of compaction
- tiered storage decision
- replay latency trade-off

---

## 20. Lab 19: End-To-End Marketplace Event Platform

Design:

- domain events
- topic names
- keys
- schemas
- outbox
- idempotent consumers
- retry/DLQ
- Streams/Connect choices
- security/PII governance
- monitoring
- DR

---

## 21. Lab 20: Incident Postmortem

Given:

```text
A replay job caused duplicate refunds and consumer lag across the cluster.
```

Write:

- timeline
- root causes
- impact
- immediate mitigation
- contributing factors
- prevention actions
- new guardrails

---

## 22. Completion Gate

You completed the labs when you can:

1. Configure producer/consumer semantics confidently.
2. Design idempotent processing and retry/DLQ flow.
3. Debug lag, hot partitions, Connect failures, and Streams recovery.
4. Explain KRaft, reassignment, DR, tiered storage, and quotas.
5. Secure sensitive topics with ACLs, retention, audit, and replay controls.
6. Deliver one full event-platform capstone.
