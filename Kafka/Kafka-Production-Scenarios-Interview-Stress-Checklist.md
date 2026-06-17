# Kafka Production Scenarios and Interview Stress Checklist

> Goal: revise the highest-value Kafka production scenarios and interview stress points in a fast, practical checklist format.

---

## 0. Why This Sheet Exists

The main Kafka sheets explain the concepts deeply.

This sheet is for the moment when an interviewer says:

```text
Kafka is lagging.
Kafka duplicated a payment.
Kafka consumers stopped.
Kafka broker went down.
Kafka schema broke prod.
What do you do?
```

Your answer should sound calm, structured, and production-aware.

---

# Topic 1: Kafka Production Scenarios and Interview Stress Points

---

## 1. The Golden Debugging Order

When Kafka has an issue, do not jump randomly.

Use this order:

1. Scope the blast radius.
2. Identify whether it is producer, broker, consumer, schema, network, or downstream.
3. Check whether data is unavailable, delayed, duplicated, or corrupted.
4. Protect correctness first.
5. Restore flow second.
6. Prevent recurrence with alerts, limits, and design changes.

Simple interview sentence:

> I first separate availability, durability, latency, and correctness. Kafka can be partially healthy, so I check topic, partition, group, and broker-level signals before acting.

---

## 2. Stress Point: Consumer Lag

### What Interviewer Is Testing

- Do you know lag is per consumer group?
- Do you know lag can be per partition?
- Do you avoid blindly scaling consumers?
- Do you know partition count caps group parallelism?

### Ask First

- Which consumer group is lagging?
- Which topic?
- Which partition?
- Is lag increasing or decreasing?
- Did a deploy happen?
- Is downstream slow?
- Is there a poison message?

### Common Causes

- consumer processing is slow
- downstream DB/API is slow
- one hot partition
- poison message
- consumer rebalance storm
- consumer crashed
- partition count too low
- message size increased

### Strong Answer

I would inspect lag by group, topic, and partition. If one partition is lagging, I suspect hot key or poison data. If all partitions are lagging, I suspect consumer capacity, downstream latency, or a bad deploy. I would check consumer logs, processing latency, rebalance rate, DLQ, and recent releases. I would scale consumers only up to partition count and only after confirming the code path is healthy.

### Trap

> "Add more consumers."

Better:

> Add consumers only if partitions are available and the bottleneck is consumer capacity.

---

## 3. Stress Point: Duplicate Processing

### What Interviewer Is Testing

- Do you understand at-least-once?
- Do you know the crash window?
- Do you design idempotency?

### Classic Failure

```text
consumer polls event
consumer updates DB
consumer crashes before committing offset
consumer restarts
same event is replayed
DB update happens again
```

### Strong Answer

Kafka replay is expected here because the offset was not committed. I would keep commit-after-processing to avoid data loss, but make the side effect idempotent. The event should have an `eventId` or business key, and the database should enforce uniqueness or keep a processed-events table.

### Trap

> "Kafka exactly-once prevents this."

Better:

> Kafka transactions help inside Kafka. External side effects still need idempotency or outbox design.

---

## 4. Stress Point: Message Loss

### What Interviewer Is Testing

- Do you know commit-before-processing risk?
- Do you know producer `acks` risk?
- Do you know retention risk?

### Loss Windows

| Window | Cause | Fix |
|---|---|---|
| Producer loss | `acks=0` or weak retry handling | `acks=all`, idempotence, retries |
| Broker durability risk | ISR too small, weak replication | RF=3, `min.insync.replicas=2` |
| Consumer loss | commit before processing | commit after successful processing |
| Retention loss | consumer lag exceeds retention | monitor lag vs retention |

### Strong Answer

I would first locate the loss boundary. If the producer never got a durable acknowledgment, I check `acks`, retries, idempotence, and broker errors. If Kafka stored the event but the consumer skipped it, I inspect offset commits. If the consumer was down longer than retention, Kafka may have deleted data based on retention policy, so replay may require archive or upstream recovery.

---

## 5. Stress Point: Broker Down

### What Interviewer Is Testing

- Do you understand leader/follower replicas?
- Do you understand ISR?
- Do you understand failover behavior?

### What Happens

```text
leader broker fails
controller elects new leader from eligible replicas
clients refresh metadata
traffic resumes
```

### Check

- offline partitions
- under-replicated partitions
- ISR count
- controller logs
- broker logs
- producer errors
- disk/network health

### Strong Answer

If a broker fails, Kafka can continue if partitions have healthy in-sync replicas. I would check whether any partitions are offline. If not, the system may only have reduced redundancy. If partitions are offline, there is no available leader and the affected topics are unavailable. I would restore broker health, avoid unsafe leader election unless business accepts data loss, and verify ISR recovery.

---

## 6. Stress Point: ISR Shrink

### What Interviewer Is Testing

- Do you know durability vs availability?
- Do you understand `acks=all` plus `min.insync.replicas`?

### Strong Answer

ISR shrink means fewer replicas are safely caught up. If producers use `acks=all` and ISR falls below `min.insync.replicas`, writes fail. That is not random failure; it is Kafka protecting the durability promise. I would restore the slow/down replica, inspect disk/network pressure, and alert before ISR reaches the minimum threshold.

### Trap

> "Lower `min.insync.replicas` to fix it."

Better:

> That may restore writes but weakens durability. It needs business approval.

---

## 7. Stress Point: Hot Partition

### What Interviewer Is Testing

- Do you understand key distribution?
- Do you know ordering trade-offs?

### Symptoms

- one partition lagging
- one broker hotter
- one consumer slower
- total consumer group looks underutilized

### Causes

- low-cardinality key like `country`, `status`, `eventType`
- one very hot customer/account/merchant
- null key with unlucky distribution
- bad custom partitioner

### Strong Answer

I would identify whether lag is concentrated in one partition. If yes, I would inspect keys and traffic distribution. For long-term fix, I may change partition key, split hot entities, isolate hot traffic into a new topic, or redesign ordering requirements. I would not blindly add consumers because one partition has one active consumer in a group.

---

## 8. Stress Point: Rebalance Storm

### What Interviewer Is Testing

- Do you know consumer group behavior?
- Do you know `poll()` health matters?

### Symptoms

- repeated partition revokes/assigns
- lag spikes
- duplicate processing
- consumers appear alive but unstable

### Causes

- processing takes longer than `max.poll.interval.ms`
- slow downstream call blocks consumer
- unstable deployment
- bad health checks
- too many rolling restarts

### Strong Answer

I would check rebalance rate and consumer logs. If processing blocks `poll()`, I would reduce batch size, pause partitions during slow work, tune poll interval carefully, or move slow work to a controlled worker model. I would also use cooperative rebalancing where appropriate and deploy gradually.

---

## 9. Stress Point: Poison Message

### What Interviewer Is Testing

- Do you prevent one bad event from blocking a partition forever?
- Do you know DLQ is an operational workflow?

### Strong Answer

I would retry transient failures with backoff. After bounded attempts, I would publish the bad record to a DLQ with original topic, partition, offset, key, payload, headers, exception, and attempt count. Then I would commit past it only after safely recording it. DLQ must have alerting, ownership, and replay tooling.

### Trap

> "Keep retrying forever."

Better:

> Infinite retry preserves order but can stop the whole partition. Use bounded retries and a business-approved DLQ policy.

---

## 10. Stress Point: Schema Break

### What Interviewer Is Testing

- Do you understand event contracts?
- Do you know compatibility?
- Do you understand replay?

### Symptoms

- deserialization errors
- consumer lag grows
- DLQ fills
- only new producer version causes failure

### Strong Answer

I would rollback the producer if it introduced an incompatible schema. Then I would enforce Schema Registry compatibility in CI/CD. Safe evolution means adding optional/default fields, not renaming or removing required fields. I would replay DLQ only after consumers can handle the schema.

### Trap

> "It is JSON, so schema does not matter."

Better:

> JSON still needs compatibility rules and contract discipline.

---

## 11. Stress Point: Kafka Transactions

### What Interviewer Is Testing

- Do you know exactly-once boundary?
- Do you distinguish Kafka output from external side effects?

### Strong Answer

Kafka transactions are useful when consuming from Kafka, producing to Kafka, and committing offsets atomically. I would use `transactional.id`, `beginTransaction`, produce output, `sendOffsetsToTransaction`, and `commitTransaction`. Downstream consumers should use `read_committed`.

If the workflow updates a database or calls an external API, Kafka transactions alone do not make that exactly once. I would use idempotency keys, unique constraints, outbox, or a transactional sink if available.

---

## 12. Stress Point: Outbox Pattern

### What Interviewer Is Testing

- Do you know how to reliably publish events after DB writes?

### Problem

```text
DB commit succeeds
Kafka publish fails
event is missing
```

### Strong Answer

I would use the outbox pattern. In one database transaction, update the business table and insert an outbox row. Then a CDC connector or relay publishes outbox rows to Kafka. This avoids distributed transactions while preserving reliable event publication.

---

## 13. Stress Point: Consumer Offset Reset

### What Interviewer Is Testing

- Do you understand replay and business impact?

### Cases

| Action | Risk |
|---|---|
| reset to earliest | reprocess many old events |
| reset to latest | skip unprocessed events |
| reset to timestamp | replay from chosen point |
| reset specific partitions | targeted but risky |

### Strong Answer

I would not reset offsets casually. I would identify the exact group/topic/partition and business time window. If reprocessing is safe and idempotent, I can reset to a timestamp or offset. If side effects are not idempotent, I need a replay plan with dedupe or a separate recovery consumer.

---

## 14. Stress Point: Retention Window Missed

### What Interviewer Is Testing

- Do you know Kafka retention is independent of consumers?

### Strong Answer

Kafka does not keep data because a consumer still needs it. It keeps data based on retention policy. If a consumer is down longer than retention, old offsets may be gone. I would recover from archive, upstream source, or accept a business-approved gap. To prevent recurrence, monitor lag against retention window and set alerts before the replay window is lost.

---

## 15. Stress Point: Compaction Misuse

### What Interviewer Is Testing

- Do you know compaction keeps latest state, not full history?

### Strong Answer

I would use compaction for latest state by key, such as user profile or account status. I would not use compacted topics as a full audit log because older values may be removed. If both are needed, I would maintain an event-history topic and a separate compacted current-state topic.

---

## 16. Stress Point: Security Incident

### What Interviewer Is Testing

- Do you know TLS/SASL/ACL basics?
- Do you understand least privilege?

### Strong Answer

For sensitive Kafka topics, I would use TLS for encryption, SASL or mTLS for authentication, and ACLs for authorization. Each service principal gets only the topic and group permissions it needs. I would monitor auth failures, denied ACLs, certificate expiry, and audit access to sensitive topics.

---

## 17. Stress Point: Kafka Connect Failure

### What Interviewer Is Testing

- Do you know source/sink connector operations?

### Strong Answer

I would check connector status, task logs, DLQ, bad records, schema mismatch, sink availability, and offset progress. For sink failures, I would check whether the target system rejected the record due to mapping or constraints. For source CDC, I would check source log position and connector lag.

---

## 18. Stress Point: Kafka Streams State Recovery

### What Interviewer Is Testing

- Do you know Streams is stateful and has internal topics?

### Strong Answer

Kafka Streams state stores are local but backed by changelog topics. If an instance dies, another instance can restore state from the changelog. I would monitor restore time, changelog topic health, state store size, internal topic replication, and standby replicas if low recovery time is needed.

---

## 19. Final Interview Checklist

Before answering any Kafka design question, cover:

- topic name and owner
- partition key and ordering
- partition count and parallelism
- replication factor and `min.insync.replicas`
- producer `acks`, retries, idempotence
- consumer group model
- offset commit strategy
- duplicate handling
- retry and DLQ
- schema compatibility
- retention or compaction
- lag and ISR monitoring
- security and ACLs
- replay plan

---

## 20. Thirty-Second Closing Answer

Use this closing when you want to sound senior:

> My Kafka design treats the topic as a durable event contract. I choose the key based on ordering, partitions based on throughput and consumer parallelism, and replication settings based on durability. Producers use safe acknowledgments and idempotence. Consumers commit after successful processing and make side effects idempotent. I handle poison records with retry and DLQ, protect schemas with compatibility checks, monitor lag and ISR, and define replay and security policies from day one.

---

## 21. Revision Notes

- One-line summary: Kafka production interviews test failure windows, not just APIs.
- Three keywords: lag, idempotency, ISR
- One interview trap: scaling consumers does not fix hot partitions or bad code.
- One memory trick: first find the boundary: producer, broker, consumer, schema, downstream, or ops.

