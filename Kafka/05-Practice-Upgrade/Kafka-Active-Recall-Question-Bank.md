# Kafka Active Recall Question Bank

> Track: Kafka Interview Track - Practice Upgrade  
> Mode: answer from memory before checking notes.

Goal: turn Kafka concepts into fast interview recall.

---

## 1. How To Use

Rules:

1. Answer aloud without notes.
2. Draw the data flow when confused.
3. Always name the boundary of ordering, durability, and exactly-once claims.
4. Mark each answer Green, Yellow, or Red.
5. Repeat Red questions after 24 hours and 7 days.

Strong answer shape:

```text
concept -> data flow -> failure mode -> config/operational control -> trade-off -> test/monitoring
```

---

## 2. Core Kafka Flow

1. What is a Kafka topic?
2. What is a partition?
3. What is an offset?
4. Why is Kafka a log rather than a traditional queue?
5. How does a producer choose a partition?
6. What happens when a message has no key?
7. What ordering does Kafka guarantee?
8. Why does adding partitions affect key distribution?
9. What is a consumer group?
10. Why can one partition be consumed by only one consumer in a group at a time?
11. How does fan-out work across consumer groups?
12. What happens when there are more consumers than partitions?
13. What is consumer lag?
14. What is an offset commit?
15. What happens if a consumer crashes after processing but before commit?

---

## 3. Broker Internals

1. What is a broker?
2. What is a partition leader?
3. What is a follower replica?
4. What is ISR?
5. What is replication factor?
6. What is `min.insync.replicas`?
7. How does Kafka use the page cache?
8. What are log segments?
9. What is KRaft?
10. What did KRaft replace?
11. What does controller quorum manage?
12. What happens when a broker hosting a leader fails?
13. What is an offline partition?
14. What is an under-replicated partition?
15. Why is disk full dangerous in Kafka?

---

## 4. Producer Delivery

1. What does `acks=0` mean?
2. What does `acks=1` mean?
3. What does `acks=all` mean?
4. Why pair `acks=all` with `min.insync.replicas`?
5. What does idempotent producer solve?
6. What does idempotent producer not solve?
7. Why can retries create duplicates without idempotence?
8. What is `max.in.flight.requests.per.connection`?
9. What do `batch.size` and `linger.ms` affect?
10. What does compression improve?
11. What can compression cost?
12. What does `delivery.timeout.ms` bound?
13. When can a producer timeout after a successful broker write?
14. How do producer retries interact with ordering?
15. What producer metrics matter?

---

## 5. Consumer Delivery

1. At-most-once vs at-least-once?
2. Why is exactly-once often misunderstood?
3. Auto commit vs manual commit?
4. `commitSync` vs `commitAsync`?
5. What is the poll loop?
6. What happens if processing exceeds `max.poll.interval.ms`?
7. What is a rebalance?
8. What causes rebalance storms?
9. What is cooperative rebalancing?
10. What is static membership?
11. When do you use `pause` and `resume`?
12. When do you use `seek`?
13. How do you replay safely?
14. What is an idempotent consumer?
15. Why should downstream side effects be idempotent?

---

## 6. Transactions Exactly-Once Idempotency

1. Idempotent producer vs transactional producer?
2. What is `transactional.id`?
3. What is producer fencing?
4. What does `read_committed` do?
5. What is consume-transform-produce exactly-once?
6. Why does Kafka transaction not make external DB updates exactly-once?
7. What is outbox pattern?
8. What is inbox/processed-event table?
9. How do unique constraints help idempotency?
10. What happens if app commits output but not offset?
11. What happens if app writes DB then crashes before offset commit?
12. When are Kafka transactions worth it?
13. When are they overkill?
14. What metrics or logs help transaction debugging?
15. What is the safest answer to "Kafka guarantees exactly once"?

---

## 7. Schema Registry Event Design

1. Why use Schema Registry?
2. Avro vs Protobuf vs JSON Schema?
3. Backward vs forward vs full compatibility?
4. What is a safe additive schema change?
5. Why is renaming a field dangerous?
6. What is semantic compatibility?
7. What belongs in an event envelope?
8. Event notification vs event-carried state?
9. What is event versioning?
10. How do key design and schema design interact?
11. What happens if Schema Registry is unavailable?
12. What is replay compatibility?
13. How do you design `OrderCreated` event safely?
14. How do you handle PII in events?
15. What schema change should fail CI?

---

## 8. Topic Design Retention Compaction

1. How do you choose topic boundaries?
2. How do you choose partition key?
3. How do you choose partition count?
4. Why is partition count hard to shrink?
5. What happens when partition count increases?
6. What is delete retention?
7. What is compaction?
8. What is a tombstone?
9. `delete` vs `compact` vs `compact,delete`?
10. When is compaction appropriate?
11. When is compaction dangerous?
12. How does retention affect lagging consumers?
13. What is a hot partition?
14. What metrics reveal hot partitions?
15. How do you estimate Kafka storage?

---

## 9. Streams Connect CDC

1. Kafka Streams vs plain consumer app?
2. Kafka Connect vs consumer app?
3. What is CDC?
4. What is Debezium used for?
5. What is outbox with CDC?
6. KStream vs KTable?
7. What is a state store?
8. What is a changelog topic?
9. What is repartitioning?
10. Why can repartition topics explode?
11. How do Streams apps recover state?
12. What is a connector task?
13. How do you debug a failing sink connector?
14. How do you handle poison records in Connect?
15. When should you avoid raw CDC as public event API?

---

## 10. Operations Monitoring Security

1. What are Kafka golden signals?
2. How do you debug consumer lag?
3. How do you debug under-replicated partitions?
4. How do you debug offline partitions?
5. What is ISR shrink?
6. What is a rebalance storm?
7. How do you handle poison messages?
8. What metrics should a Kafka dashboard include?
9. TLS vs SASL vs ACLs?
10. What are quotas?
11. How do quotas help multi-tenant Kafka?
12. What is rack awareness?
13. What is partition reassignment?
14. What should you monitor during reassignment?
15. How do you avoid unsafe leader election?

---

## 11. Modern Platform Governance

1. What should a topic ownership record include?
2. What does KRaft controller quorum health affect?
3. What is tiered storage?
4. What tiered storage trade-offs matter?
5. What are managed Kafka caveats?
6. What belongs in a Kafka DR plan besides messages?
7. Why is active-active Kafka hard?
8. What is RPO/RTO?
9. How do you govern replay jobs?
10. Why are DLQs sensitive?
11. How do you classify event data?
12. Why avoid raw PII in Kafka?
13. How do you design least-privilege ACLs?
14. Tenant ID field vs tenant isolation?
15. What should be audited in Kafka platform operations?

---

## 12. Final Readiness Gate

You are ready when you can answer without notes:

1. Explain producer -> partition -> broker log -> consumer group -> offset -> side effect.
2. Explain ordering, durability, and exactly-once boundaries precisely.
3. Debug lag, duplicates, hot partitions, poison records, schema breaks, and ISR shrink.
4. Design event schema, topic key, partition count, retention, compaction, and DLQ strategy.
5. Implement idempotent producer and idempotent consumer reasoning.
6. Explain Kafka transactions and outbox boundaries.
7. Compare Streams, Connect, CDC, and plain consumer apps.
8. Design multi-tenant Kafka platform guardrails.
9. Explain KRaft, tiered storage, DR, quotas, and rack awareness.
10. Secure PII-sensitive topics with ACLs, retention, audit, and replay governance.
