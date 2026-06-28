# Kafka FAANG Scenario Drill Bank Gold Sheet

> Goal: revise Kafka through high-signal interview scenarios, traps, and strong answer structures.

---

## 0. How To Use This

Read each scenario in three passes:

1. Beginner pass: identify the Kafka concept.
2. Intermediate pass: name the failure mode and mitigation.
3. Senior pass: state the trade-off, metric, and recovery plan.

Strong Kafka answers usually follow this shape:

```text
1. Clarify the business guarantee.
2. Choose topic/key/partition/consumer design.
3. Define delivery semantics.
4. Handle failure and duplicate windows.
5. Monitor lag, ISR, DLQ, and errors.
6. State trade-offs.
```

---

# Topic 1: Kafka FAANG Scenario Drill Bank

---

## 1. Intuition

Kafka interview scenarios are rarely about remembering one config.

They test whether you can reason through:

- ordering
- scale
- duplication
- failure
- replay
- schema evolution
- operations
- security

Simple rule:

> Every Kafka answer should explain what happens when a producer retries, a broker dies, a consumer crashes, and a bad message appears.

---

## 2. Definition

- Definition: A Kafka scenario drill is an interview-style problem that tests how Kafka behaves under scale, failure, and cross-service evolution.
- Category: Distributed systems interview preparation
- Core idea: turn Kafka theory into production decisions.

---

## 3. Why It Exists

Knowing Kafka terms is not enough.

Interviewers ask scenarios because they want to see if you can:

- protect business correctness
- debug production issues
- choose trade-offs
- avoid overclaiming exactly-once
- design for growth
- communicate clearly

---

## 4. Reality

These scenarios are common in:

- FAANG-style system design rounds
- senior backend interviews
- platform engineering interviews
- fintech interviews
- data infrastructure interviews
- microservices architecture rounds

---

## 5. Scenario Bank

### Scenario 1: Design Order Event Pipeline

Question:

> Users place orders. Payment, inventory, notification, analytics, and fraud systems all need the order event. How would you design this with Kafka?

Strong answer:

- Create `order-events` topic.
- Key by `orderId` for per-order ordering.
- Use replication factor 3, `min.insync.replicas=2`, producer `acks=all`, idempotence enabled.
- Each downstream service uses its own consumer group.
- Use Schema Registry and event envelope with `eventId`, `eventType`, `aggregateId`, `occurredAt`, `traceId`.
- Consumers commit offsets after successful processing.
- Consumers are idempotent using `eventId` or business key.
- Retry transient failures, DLQ poison records.
- Monitor consumer lag, DLQ, under-replicated partitions, producer errors.

Trap:

- Saying all consumers in one group receive every event. They do not. Each consumer group receives independently; inside one group, partitions are shared.

---

### Scenario 2: Consumer Crashes After DB Write Before Offset Commit

Question:

> A consumer updates a database and crashes before committing the Kafka offset. What happens?

Strong answer:

- Kafka will replay the message after restart because the offset was not committed.
- The DB update may run again.
- This is at-least-once processing.
- Make DB update idempotent using `eventId`, unique constraint, or processed-events table.
- Commit offset only after DB transaction succeeds.
- For DB-to-Kafka publication, consider outbox pattern.

Trap:

- Saying Kafka will know the DB update already happened. Kafka does not know external side effects.

---

### Scenario 3: Duplicate Payment Event

Question:

> A payment event is processed twice. How do you prevent double charging?

Strong answer:

- Use idempotency key such as `paymentId` or `eventId`.
- Payment provider call should include idempotency key if supported.
- Database should store payment state with unique constraint.
- Consumer should treat reprocessed event as no-op if already processed.
- Kafka producer idempotence reduces producer retry duplicates, but consumer idempotency protects business side effects.

Trap:

- Relying only on Kafka exactly-once. External payment calls need their own idempotency.

---

### Scenario 4: Hot Partition

Question:

> One partition has huge lag while other partitions are healthy. What do you suspect?

Strong answer:

- Key skew or poison message.
- Check lag per partition, keys in lagging partition, consumer logs, processing latency, DLQ.
- If one key is hot, key strategy is causing imbalance.
- If one record fails repeatedly, move to retry/DLQ with metadata.
- Long-term fix may require key redesign, splitting hot entity, or separate topic.

Trap:

- Adding more consumers blindly. One partition can only be owned by one consumer in a group at a time.

---

### Scenario 5: More Consumers Than Partitions

Question:

> Topic has 6 partitions. Consumer group has 10 consumers. What happens?

Strong answer:

- At most 6 consumers actively consume that topic in that group.
- 4 consumers are idle for that topic.
- To increase parallelism, increase partitions carefully or improve processing speed.
- Increasing partitions may affect future key-to-partition mapping, so review ordering needs.

Trap:

- Saying all 10 consumers split each partition. They do not.

---

### Scenario 6: Broker Leader Fails

Question:

> The broker that leads a partition goes down. What happens?

Strong answer:

- Controller elects a new leader from eligible in-sync replicas.
- Producers/consumers may briefly fail or see latency until metadata refresh.
- With RF=3, `min.insync.replicas=2`, and enough ISR, writes can continue after failover.
- Monitor offline partitions, under-replicated partitions, ISR, and producer errors.

Trap:

- Saying replication factor 3 automatically means no impact. There can still be brief unavailability and ISR risk.

---

### Scenario 7: ISR Shrinks Below `min.insync.replicas`

Question:

> Producer uses `acks=all`, RF=3, `min.insync.replicas=2`. ISR drops to 1. What happens?

Strong answer:

- Writes fail because Kafka cannot satisfy the requested durability threshold.
- This is intentional protection against acknowledging unsafe writes.
- Recover by fixing broker/network/disk issue so replicas catch up.
- Alert on ISR shrink before it reaches this state.

Trap:

- Treating this as purely a Kafka outage. It is a durability vs availability trade-off.

---

### Scenario 8: Schema Change Breaks Consumers

Question:

> Producer renamed `amount` to `totalAmount`, and consumers started failing. What went wrong?

Strong answer:

- Breaking schema change.
- Renaming is removal plus addition from compatibility perspective.
- Use Schema Registry compatibility checks.
- Add new optional field with default, deprecate old field, migrate consumers, then remove only by policy.
- Replay old data must still work.

Trap:

- Thinking JSON avoids schema compatibility problems.

---

### Scenario 9: Need Latest User Profile State

Question:

> Consumers need the latest user profile by user ID. Should you use normal retention or compaction?

Strong answer:

- Use compacted topic if latest value per key is the requirement.
- Key by `userId`.
- Consumers can rebuild current state by reading compacted topic.
- Use tombstone (`key`, null value) for deletes.
- Use separate event-history topic if audit/full history is needed.

Trap:

- Using compaction for audit history. Compaction can remove older values.

---

### Scenario 10: Increase Partition Count In Production

Question:

> The team wants to increase partitions from 12 to 48. What do you check?

Strong answer:

- Why: throughput, consumer parallelism, or future growth?
- Does key ordering matter?
- Future key mapping may change because partition calculation changes.
- Check consumer assumptions, partition assignment, broker capacity, file handles, metadata, and rebalance impact.
- For strict ordering, consider new topic migration.

Trap:

- Treating partition increase as a harmless scaling button.

---

### Scenario 11: Kafka vs RabbitMQ/SQS

Question:

> When would you choose Kafka over a traditional queue?

Strong answer:

- Choose Kafka for high-throughput event streams, replay, fan-out to multiple consumer groups, ordered per-key logs, and event history.
- Choose RabbitMQ/SQS for simpler task queues, per-message work dispatch, delayed jobs, and lower operational complexity.
- Kafka is a durable log, not just a message queue.

Trap:

- Saying Kafka is always better. It is better for event streaming, not every async job.

---

### Scenario 12: Poison Message Blocks Consumer

Question:

> One record always fails and the consumer cannot progress. What do you do?

Strong answer:

- Retry transient failures with bounded attempts.
- For non-transient poison record, write to DLQ with original topic/partition/offset/key/payload/error.
- Commit past it only after safely recording it.
- Alert and provide replay tooling.

Trap:

- Infinite retry on the same record forever.

---

### Scenario 13: Kafka Transactions Needed Or Not?

Question:

> A service reads Kafka, transforms events, writes to another Kafka topic, and commits offsets. Should you use transactions?

Strong answer:

- Yes, if duplicate output is expensive and output is Kafka.
- Use transactional producer, `sendOffsetsToTransaction`, and downstream `read_committed`.
- Keep transactions short.
- If output is an external DB/API, Kafka transaction alone is not enough.

Trap:

- Using transactions for external side effects and assuming full end-to-end exactly-once.

---

### Scenario 14: CDC From Orders Database

Question:

> How do you publish reliable order events when orders are stored in a relational database?

Strong answer:

- Use outbox pattern.
- In same DB transaction, update order table and insert outbox event.
- CDC connector publishes outbox events to Kafka.
- Schema Registry protects event contract.
- Consumers use idempotency.

Trap:

- Write DB first, then publish Kafka directly without handling failure between them.

---

### Scenario 15: Kafka Streams vs Plain Consumer

Question:

> You need to aggregate transactions per merchant in 5-minute windows. Streams or plain consumer?

Strong answer:

- Kafka Streams is a strong fit.
- It supports grouping, windowing, state stores, changelog-backed recovery, and Kafka-to-Kafka output.
- Key by merchant ID for aggregation.
- Monitor state restore time, internal topics, lag, and late/out-of-order events.

Trap:

- Building custom stateful aggregation without understanding recovery.

---

### Scenario 16: Kafka Connect Sink Failing

Question:

> Elasticsearch sink connector is failing on some records. What do you check?

Strong answer:

- Connector task logs.
- Bad schema/data.
- Mapping mismatch in Elasticsearch.
- DLQ records.
- Sink retry settings.
- Whether one poison record is blocking progress.
- Fix schema/mapping, replay DLQ after validation.

Trap:

- Restarting connector repeatedly without looking at poison data.

---

### Scenario 17: Consumer Lag After Deploy

Question:

> Lag grows after a new consumer deploy. How do you debug?

Strong answer:

- Compare before/after version.
- Check processing latency, errors, DLQ, rebalance rate, DB/API latency.
- Check whether `max.poll.interval.ms` is exceeded.
- Roll back if deploy introduced regression.
- Scale only after confirming it is capacity, not a bug.

Trap:

- Scaling consumers when all are failing due to the same code bug.

---

### Scenario 18: Multi-Tenant Kafka Platform

Question:

> Many teams share Kafka. How do you prevent one team from hurting others?

Strong answer:

- Naming and ownership conventions.
- ACLs per topic/group.
- Producer and consumer quotas.
- Retention defaults.
- Schema compatibility policy.
- Topic creation approval or guardrails.
- Monitor noisy tenants and isolate critical workloads.

Trap:

- Giving wildcard permissions and unlimited throughput to every service.

---

### Scenario 19: Cross-Region Kafka DR

Question:

> How would you design disaster recovery for Kafka?

Strong answer:

- Define RPO/RTO first.
- Mirror critical topics to another cluster if regional failover is required.
- Decide active-passive vs active-active.
- Design idempotent consumers because duplicates can happen during failover.
- Plan offset/failover strategy and test it.
- Archive critical topics if compliance requires history.

Trap:

- Assuming cross-region mirroring preserves every local ordering and offset expectation automatically.

---

### Scenario 20: Sensitive Data In Kafka

Question:

> Kafka topics contain PII. What security controls do you add?

Strong answer:

- TLS for encryption in transit.
- SASL or mTLS for authentication.
- ACLs for least privilege.
- Separate sensitive topics.
- Mask or tokenize data when possible.
- Audit access.
- Manage cert/secret rotation.
- Avoid putting secrets in payloads.

Trap:

- Relying only on network isolation.

---

## 6. What Problem It Solves

- Primary problem solved: fast recall under interview pressure
- Secondary benefits: scenario communication, failure reasoning, senior trade-off language
- Systems impact: prepares you for real Kafka production design and debugging conversations

---

## 7. The One-Minute Kafka Answer Template

Use this when stuck:

```text
I would model this as an event stream.
The topic represents <business event>.
The key is <entity id> because we need ordering per <entity>.
Consumers use separate groups for independent fan-out.
For durability, I use RF=3, minISR=2, acks=all, and idempotent producer.
For processing, I commit offsets after successful work and make side effects idempotent.
For schema, I use Schema Registry and compatible evolution.
For failure, I use retries, DLQ, monitoring for lag/ISR/errors, and replay tooling.
The trade-off is <latency/cost/complexity vs durability/replay/ordering>.
```

---

## 8. Common FAANG Traps

| Trap | Better Answer |
|---|---|
| "Kafka is exactly once." | "Kafka can be exactly-once within Kafka transaction boundaries." |
| "More consumers always scale." | "Parallelism is capped by partitions inside a group." |
| "Consumed means deleted." | "Kafka retains records by retention/compaction, not by consumption." |
| "Replication factor alone gives durability." | "Durability depends on RF, ISR, `acks`, and `min.insync.replicas`." |
| "Compaction keeps full history." | "Compaction keeps latest state per key, not every old value." |
| "JSON does not need schema." | "JSON still needs contract and compatibility control." |
| "DLQ solved it." | "DLQ needs ownership, alerting, and replay/discard workflow." |
| "Kafka replaces all queues." | "Kafka is best for durable replayable streams; simple queues may be better for tasks." |

---

## 9. Final Revision Notes

- One-line summary: Kafka interviews are about ordering, durability, duplication, replay, schema, and operations under failure.
- Three keywords: partition, offset, idempotency
- One interview trap: never talk about guarantees without naming the boundary.
- One memory trick: producer can retry, broker can fail, consumer can crash, schema can evolve, ops must catch it.

---

## 10. Official Source Notes

- Apache Kafka 4.3 docs: <https://kafka.apache.org/43/>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>
- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>
- Apache Kafka security overview: <https://kafka.apache.org/43/security/security-overview/>

