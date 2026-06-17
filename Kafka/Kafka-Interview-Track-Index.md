# Kafka Interview Track Index

> Goal: make Kafka a one-stop, all-level learning path for SDE interviews, senior design rounds, and real production debugging.

---

## How To Use This Track

Kafka has many moving parts. Do not learn it as random terms. Learn it as one flow:

```text
producer
-> topic
-> partition
-> broker log
-> replicas / ISR
-> consumer group
-> offset commit
-> downstream side effect
-> monitoring / recovery
```

For interviews, always connect each answer to four things:

1. How data moves.
2. How ordering is preserved or lost.
3. How failure is handled.
4. What trade-off you are choosing.

---

## Study Order

| Order | Document | Why It Exists |
|---|---|---|
| 1 | [Kafka Producer, Partition, Consumer Group Flow](Kafka-Producer-Partition-Consumer-Group-Distributed-Flow.md) | Foundation: producer routing, partition ownership, consumer groups, offsets, and fan-out |
| 2 | [Kafka Broker Internals and Architecture](Kafka-Broker-Internals-Architecture-Gold-Sheet.md) | Broker, KRaft controller, replicas, ISR, page cache, log segments, and why Kafka is fast |
| 3 | [Kafka Producer Consumer Delivery Guarantees](Kafka-Producer-Consumer-Delivery-Guarantees-Gold-Sheet.md) | `acks`, retries, idempotent producer, offset commits, at-most-once, at-least-once, duplicates |
| 4 | [Kafka Transactions, Idempotency, and Exactly-Once](Kafka-Transactions-Exactly-Once-Idempotency-Gold-Sheet.md) | Transactional producer, consume-transform-produce, `read_committed`, and external side effects |
| 5 | [Kafka Schema Registry and Event Design](Kafka-Schema-Registry-Event-Design-Gold-Sheet.md) | Avro/Protobuf/JSON Schema, compatibility, event contracts, versioning, and schema mistakes |
| 6 | [Kafka Topic Design, Retention, and Compaction](Kafka-Topic-Design-Retention-Compaction-Gold-Sheet.md) | Topic naming, partition count, key design, retention, tombstones, compaction, and storage planning |
| 7 | [Kafka Streams, Connect, and CDC](Kafka-Streams-Connect-CDC-Gold-Sheet.md) | Streams vs consumer app vs Connect, state stores, joins, windows, connectors, Debezium, outbox |
| 8 | [Kafka Operations, Monitoring, and Security](Kafka-Operations-Monitoring-Security-Gold-Sheet.md) | Lag, under-replicated partitions, broker failure, KRaft health, TLS/SASL/ACLs, runbooks |
| 9 | [Kafka Advanced API and Platform Concepts](Kafka-Advanced-API-Platform-Concepts-Gold-Sheet.md) | AdminClient, `seek`, `pause/resume`, static membership, cooperative rebalancing, rack awareness, cross-cluster caveats |
| 10 | [Kafka FAANG Scenario Drill Bank](Kafka-FAANG-Scenario-Drill-Bank-Gold-Sheet.md) | Fast revision: scenario questions, senior answers, traps, and debugging playbooks |
| 11 | [Kafka Production Scenarios and Interview Stress Checklist](Kafka-Production-Scenarios-Interview-Stress-Checklist.md) | Production fire-drill checklist: lag, duplicates, ISR, schema breaks, DLQ, offset reset, Streams, Connect |

---

## Learning Levels

### Beginner

You should be able to explain:

- topic vs partition
- producer key routing
- offset
- consumer group
- why Kafka is not a normal queue
- why one partition is consumed by one consumer inside a group

### Intermediate

You should be able to reason about:

- `acks=all`, replication factor, and `min.insync.replicas`
- retries and duplicate events
- consumer lag and offset commits
- hot partitions
- retention and compaction
- schema compatibility
- DLQ and poison messages

### Senior / FAANG Level

You should be able to lead a discussion about:

- KRaft controller quorum and broker metadata
- durability vs availability during ISR shrink
- exactly-once boundaries
- event design and schema evolution
- CDC/outbox patterns
- stateful stream processing
- cross-region mirroring and disaster recovery
- operational metrics, quotas, security, and incident recovery
- production stress scenarios such as lag, duplicate processing, offset reset, schema breaks, and hot partitions
- advanced APIs such as AdminClient, `seek`, `pause/resume`, static membership, and rack-aware placement

---

## Modern Kafka Watchlist

Kafka keeps evolving. For interviews, know the stable fundamentals first, then mention version-specific features carefully:

- KRaft is the modern metadata/controller architecture.
- Kafka 4.x transaction protocol improvements matter for teams using transactional producers.
- Tiered storage can change retention and storage economics, but operational behavior depends on the platform.
- Share-consumer style features are version-specific and should be discussed only after checking the target Kafka version.
- Managed Kafka platforms may expose only part of upstream Kafka behavior, so always check provider docs in real projects.

Rule:

> Say "in modern Kafka versions..." only when you can name the feature and boundary. Otherwise, keep the answer on stable Kafka fundamentals.

---

## Kafka Interview Master Map

```text
Producer side:
  key choice
  partitioner
  batching
  compression
  retries
  idempotence
  transactions

Broker side:
  topic
  partition
  segment
  page cache
  leader/follower
  ISR
  controller quorum
  retention/compaction

Consumer side:
  poll loop
  group membership
  assignment
  offset commit
  rebalance
  lag
  idempotent processing
  retry topic / DLQ

Platform side:
  schema registry
  Kafka Streams
  Kafka Connect
  CDC
  monitoring
  security
  disaster recovery

Advanced API side:
  AdminClient
  seek / offset replay
  pause / resume
  subscribe vs assign
  static membership
  cooperative rebalancing
  headers / interceptors
  rack awareness
  partition reassignment

Production stress side:
  lag triage
  duplicate windows
  offset reset
  ISR shrink
  schema break
  poison message
  hot partition
  rebalance storm
  Connect failure
  Streams recovery
```

---

## Official Source Notes

Use the local notes for interview learning, and use these sources when checking exact version behavior:

- Apache Kafka 4.3 documentation: <https://kafka.apache.org/43/>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>
- Apache Kafka KRaft docs: <https://kafka.apache.org/43/operations/kraft/>
- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>
- Apache Kafka Streams docs: <https://kafka.apache.org/43/streams/introduction/>
- Apache Kafka Connect docs: <https://kafka.apache.org/43/kafka-connect/overview/>
- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Confluent Schema Registry docs: <https://docs.confluent.io/platform/current/schema-registry/index.html>

---

## Final Interview Rule

Never say "Kafka guarantees exactly once" without defining the boundary.

Better answer:

> Kafka can provide idempotent production and transactional consume-transform-produce semantics inside Kafka. Once you update an external database, call an API, send email, or charge money, you must design idempotency, dedupe, outbox, or transactional integration yourself.
