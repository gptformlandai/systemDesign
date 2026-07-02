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
| 1 | [Kafka Local Docker CLI Quickstart](00-Setup/Kafka-Local-Docker-CLI-Quickstart-Gold-Sheet.md) | Hands-on start: local Kafka, topics, producers, consumers, groups, offsets, and safe cleanup |
| 2 | [Kafka Producer, Partition, Consumer Group Flow](01-Foundations/Kafka-Producer-Partition-Consumer-Group-Distributed-Flow.md) | Foundation: producer routing, partition ownership, consumer groups, offsets, and fan-out |
| 3 | [Kafka Broker Internals and Architecture](02-Architecture-Internals/Kafka-Broker-Internals-Architecture-Gold-Sheet.md) | Broker, KRaft controller, replicas, ISR, page cache, log segments, and why Kafka is fast |
| 4 | [Kafka Producer Consumer Delivery Guarantees](01-Foundations/Kafka-Producer-Consumer-Delivery-Guarantees-Gold-Sheet.md) | `acks`, retries, idempotent producer, offset commits, at-most-once, at-least-once, duplicates |
| 5 | [Kafka Producer Consumer Config Tuning](02-Architecture-Internals/Kafka-Producer-Consumer-Config-Tuning-Gold-Sheet.md) | Practical tuning of latency, throughput, batching, compression, fetch, poll loop, and rebalance stability |
| 6 | [Kafka Transactions, Idempotency, and Exactly-Once](03-Advanced-Streaming/Kafka-Transactions-Exactly-Once-Idempotency-Gold-Sheet.md) | Transactional producer, consume-transform-produce, `read_committed`, and external side effects |
| 7 | [Kafka Schema Registry and Event Design](02-Architecture-Internals/Kafka-Schema-Registry-Event-Design-Gold-Sheet.md) | Avro/Protobuf/JSON Schema, compatibility, event contracts, versioning, and schema mistakes |
| 8 | [Kafka Topic Design, Retention, and Compaction](01-Foundations/Kafka-Topic-Design-Retention-Compaction-Gold-Sheet.md) | Topic naming, partition count, key design, retention, tombstones, compaction, and storage planning |
| 9 | [Kafka CLI Admin Command Playbook](01-Foundations/Kafka-CLI-Admin-Command-Playbook-Gold-Sheet.md) | Production command literacy: topics, configs, groups, offsets, ACLs, logs, and safe incident commands |
| 10 | [Kafka Streams, Connect, and CDC](03-Advanced-Streaming/Kafka-Streams-Connect-CDC-Gold-Sheet.md) | Streams vs consumer app vs Connect, state stores, joins, windows, connectors, Debezium, outbox |
| 11 | [Kafka Streams Advanced Processor API and Testing](03-Advanced-Streaming/Kafka-Streams-Advanced-ProcessorAPI-Testing-Gold-Sheet.md) | Processor API, state stores, punctuators, internal topics, topology tests, reset, and production testing |
| 12 | [Kafka Connect Operations and Connector Development](03-Advanced-Streaming/Kafka-Connect-Operations-Connector-Development-Gold-Sheet.md) | Workers, connectors, tasks, converters, SMTs, DLQ, REST operations, custom connectors, and upgrades |
| 13 | [Kafka Operations, Monitoring, and Security](04-Senior-Operations-Platform/Kafka-Operations-Monitoring-Security-Gold-Sheet.md) | Lag, under-replicated partitions, broker failure, KRaft health, TLS/SASL/ACLs, runbooks |
| 14 | [Kafka Observability, SLOs, and Tracing](04-Senior-Operations-Platform/Kafka-Observability-SLO-Tracing-Gold-Sheet.md) | Event freshness, producer/broker/consumer metrics, Connect/Streams dashboards, alerts, and trace headers |
| 15 | [Kafka Advanced API and Platform Concepts](03-Advanced-Streaming/Kafka-Advanced-API-Platform-Concepts-Gold-Sheet.md) | AdminClient, `seek`, `pause/resume`, static membership, cooperative rebalancing, rack awareness, cross-cluster caveats |
| 16 | [Kafka Modern Platform Operations, KRaft, Tiered Storage, and Multi-Tenant Platform](04-Senior-Operations-Platform/Kafka-Modern-Platform-Operations-KRaft-Tiered-Storage-Multi-Tenant-Gold-Sheet.md) | Senior platform layer: KRaft operations, tiered storage, managed Kafka caveats, quotas, reassignment, capacity, and DR |
| 17 | [Kafka Reprocessing, Backfill, Replay, and Governance](04-Senior-Operations-Platform/Kafka-Reprocessing-Backfill-Replay-Governance-Gold-Sheet.md) | Safe offset reset, DLQ replay, backfill, idempotency, audit, rate limiting, and side-effect risk |
| 18 | [Kafka Kubernetes and Strimzi Operations](04-Senior-Operations-Platform/Kafka-Kubernetes-Strimzi-Operations-Gold-Sheet.md) | Operator-based Kafka, CRDs, storage, upgrades, rolling operations, scheduling, and Kubernetes failure modes |
| 19 | [Kafka Share Consumer and Modern Feature Boundaries](04-Senior-Operations-Platform/Kafka-Share-Consumer-Modern-Features-Gold-Sheet.md) | Version-aware discussion of share-consumer style patterns, queue-like workloads, group protocols, and modern feature boundaries |
| 20 | [Kafka Security Governance, PII, and Tenant Isolation](04-Senior-Operations-Platform/Kafka-Security-Governance-PII-Tenant-Isolation-Gold-Sheet.md) | Governance layer: data classification, PII minimization, ACL policy, DLQ/replay controls, retention, audit, and tenant isolation |
| 21 | [Kafka FAANG Scenario Drill Bank](05-Practice-Upgrade/Kafka-FAANG-Scenario-Drill-Bank-Gold-Sheet.md) | Fast revision: scenario questions, senior answers, traps, and debugging playbooks |
| 22 | [Kafka Production Scenarios and Interview Stress Checklist](05-Practice-Upgrade/Kafka-Production-Scenarios-Interview-Stress-Checklist.md) | Production fire-drill checklist: lag, duplicates, ISR, schema breaks, DLQ, offset reset, Streams, Connect |
| 23 | [Kafka Active Recall Question Bank](05-Practice-Upgrade/Kafka-Active-Recall-Question-Bank.md) | Retrieval practice across fundamentals, internals, delivery, schemas, ops, platform, and governance |
| 24 | [Kafka Scenario Drill Bank](05-Practice-Upgrade/Kafka-Scenario-Drill-Bank.md) | Interview-style design, debugging, Streams/Connect, platform, DR, and governance scenarios |
| 25 | [Kafka Design Coding Mini Labs](05-Practice-Upgrade/Kafka-Design-Coding-Mini-Labs.md) | Hands-on design/coding exercises for producers, consumers, retry/DLQ, outbox, lag triage, Streams, KRaft, DR, and security |
| 26 | [Kafka Mock Interview Scripts](05-Practice-Upgrade/Kafka-Mock-Interview-Scripts.md) | Timed mock rounds from fundamentals to MAANG event-platform capstone |
| 27 | [Kafka Interview Scoring Rubrics](05-Practice-Upgrade/Kafka-Interview-Scoring-Rubrics.md) | Measurable scoring for senior/MAANG readiness across correctness, operations, platform, and governance |
| 28 | [Kafka 2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/Kafka-2-Week-4-Week-Mastery-Roadmaps.md) | Day-by-day acceleration and deeper mastery plans for interview preparation |
| 29 | [Kafka Production Event Platform Capstone](06-Capstone/Kafka-Production-Event-Platform-Capstone.md) | End-to-end proof of mastery: design, operate, secure, observe, replay, and communicate a production Kafka platform |

---

## Practice Upgrade Layer

Use the `05-Practice-Upgrade` folder after the concept sheets. It turns reading into interview performance.

| Practice File | Use It For |
|---|---|
| [Kafka Active Recall Question Bank](05-Practice-Upgrade/Kafka-Active-Recall-Question-Bank.md) | Daily retrieval practice and weak-spot detection |
| [Kafka Scenario Drill Bank](05-Practice-Upgrade/Kafka-Scenario-Drill-Bank.md) | Production debugging, design, governance, and DR prompts |
| [Kafka Design Coding Mini Labs](05-Practice-Upgrade/Kafka-Design-Coding-Mini-Labs.md) | Small implementation/design labs that force practical reasoning |
| [Kafka Mock Interview Scripts](05-Practice-Upgrade/Kafka-Mock-Interview-Scripts.md) | Timed interview rounds and capstone rehearsals |
| [Kafka Interview Scoring Rubrics](05-Practice-Upgrade/Kafka-Interview-Scoring-Rubrics.md) | Objective self-scoring after every mock or scenario |
| [Kafka 2 Week 4 Week Mastery Roadmaps](05-Practice-Upgrade/Kafka-2-Week-4-Week-Mastery-Roadmaps.md) | Structured 2-week and 4-week study paths |
| [Kafka Production Event Platform Capstone](06-Capstone/Kafka-Production-Event-Platform-Capstone.md) | Final end-to-end system design, operations, security, observability, and replay exercise |

Recommended loop:

```text
read one concept sheet -> answer recall questions -> solve one scenario/lab -> speak a mock answer -> score with rubric
```

Final loop:

```text
complete concept sheets -> run practice drills -> score weak areas -> complete capstone -> repeat mock answer under time pressure
```

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
- tiered storage and managed Kafka caveats
- durability vs availability during ISR shrink
- exactly-once boundaries
- event design and schema evolution
- CDC/outbox patterns
- stateful stream processing
- cross-region mirroring and disaster recovery
- operational metrics, quotas, tenant isolation, security, and incident recovery
- topic ownership, PII minimization, DLQ/replay governance, retention, and audit
- production stress scenarios such as lag, duplicate processing, offset reset, schema breaks, and hot partitions
- advanced APIs such as AdminClient, `seek`, `pause/resume`, static membership, and rack-aware placement
- CLI/admin command literacy for topics, groups, configs, offsets, ACLs, and safe dry runs
- producer/consumer tuning across latency, throughput, batching, compression, fetch, poll loop, and rebalance behavior
- Streams testing, topology resets, Connect task operations, connector development boundaries, and CDC reliability
- event freshness SLOs, distributed tracing headers, dashboards, alerting, and business-impact triage
- Kubernetes/Strimzi or managed-platform boundaries, including storage, rolling upgrades, and operator behavior
- modern share-consumer style features with clear version, client, provider, ordering, and acknowledgement caveats

---

## Modern Kafka Watchlist

Kafka keeps evolving. For interviews, know the stable fundamentals first, then mention version-specific features carefully:

- KRaft is the modern metadata/controller architecture.
- Kafka 4.x transaction protocol improvements matter for teams using transactional producers.
- Tiered storage can change retention and storage economics, but operational behavior depends on the platform.
- Share-consumer style features are version/client/provider-specific; use them only after checking exact ordering, acknowledgement, retry, replay, and monitoring semantics.
- Managed Kafka platforms may expose only part of upstream Kafka behavior, so always check provider docs in real projects.

Rule:

> Say "in modern Kafka versions..." only when you can name the feature and boundary. Otherwise, keep the answer on stable Kafka fundamentals.

---

## Kafka Interview Master Map

```text
Setup / CLI side:
  local Docker cluster
  topic creation
  console producer / consumer
  consumer group describe
  offset dry-run reset
  topic configs
  ACL and quota inspection

Producer side:
  key choice
  partitioner
  batching
  compression
  retries
  idempotence
  transactions
  delivery timeout
  request timeout
  buffer pressure
  serialization

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
  freshness
  fetch tuning
  max poll interval
  idempotent processing
  retry topic / DLQ

Streaming / integration side:
  Kafka Streams DSL
  Processor API
  state stores
  changelog topics
  repartition topics
  topology testing
  application reset
  Kafka Connect workers
  connectors / tasks
  converters
  SMTs
  CDC / outbox

Platform side:
  schema registry
  monitoring
  SLOs
  tracing headers
  security
  governance
  quotas
  tiered storage
  disaster recovery
  Kubernetes / Strimzi
  managed Kafka caveats

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
  modern group protocols
  share-consumer feature evaluation

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
  DLQ replay
  backfill
  replay governance
  tenant isolation
  KRaft quorum issue
  Kubernetes broker restart
  connector task failure
  trace/freshness SLO breach
```

---

## MAANG Completion Definition

This track is complete only when you can do all of the following without notes:

1. Explain Kafka as a durable partitioned log from producer to downstream side effect.
2. Define ordering, durability, at-most-once, at-least-once, and exactly-once boundaries precisely.
3. Design topic names, partition keys, partition counts, retention, compaction, schemas, and DLQs from requirements.
4. Build an idempotent producer/consumer design and explain the crash windows.
5. Explain Kafka transactions, outbox, CDC, and why external side effects still need idempotency.
6. Choose between plain consumers, Kafka Streams, Kafka Connect, and CDC.
7. Use Kafka CLI/admin commands safely for local learning, production diagnosis, offset dry runs, and configuration inspection.
8. Tune producers and consumers across latency, throughput, batching, compression, fetch behavior, poll loop stability, and duplicate windows.
9. Test and operate Kafka Streams, Kafka Connect, CDC, connector tasks, DLQs, state stores, and topology resets.
10. Debug lag, poison records, rebalance storms, hot partitions, ISR shrink, offline partitions, schema breaks, connector failures, and SLO breaches.
11. Operate a KRaft-era Kafka platform with quotas, rack awareness, partition reassignment, tiered storage, capacity planning, Kubernetes/Strimzi or managed-provider caveats, and DR.
12. Govern sensitive multi-tenant topics with ownership, classification, ACLs, PII minimization, retention, DLQ/replay controls, approval, and audit.
13. Explain modern feature boundaries, including share-consumer style features, without overclaiming ordering, acknowledgement, replay, or provider support.
14. Deliver a full event-platform capstone using the mock scripts and score at least 4/5 on the rubric.

---

## Official Source Notes

Use the local notes for interview learning, and use these sources when checking exact version behavior:

- Apache Kafka 4.3 documentation: <https://kafka.apache.org/43/>
- Apache Kafka getting started with Docker: <https://kafka.apache.org/43/getting-started/docker/>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>
- Apache Kafka basic operations: <https://kafka.apache.org/43/operations/basic-operations/>
- Apache Kafka KRaft docs: <https://kafka.apache.org/43/operations/kraft/>
- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka share consumer configs: <https://kafka.apache.org/43/generated/shareconsumer_config.html>
- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>
- Apache Kafka Streams docs: <https://kafka.apache.org/43/streams/introduction/>
- Apache Kafka Streams testing docs: <https://kafka.apache.org/43/streams/developer-guide/testing/>
- Apache Kafka Streams application reset docs: <https://kafka.apache.org/43/streams/developer-guide/app-reset-tool/>
- Apache Kafka Connect docs: <https://kafka.apache.org/43/kafka-connect/overview/>
- Apache Kafka Connect user guide: <https://kafka.apache.org/43/kafka-connect/userguide/>
- Apache Kafka Connect developer guide: <https://kafka.apache.org/43/kafka-connect/devguide/>
- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Strimzi operator overview: <https://strimzi.io/docs/operators/latest/overview.html>
- Confluent Schema Registry docs: <https://docs.confluent.io/platform/current/schema-registry/index.html>

---

## Final Interview Rule

Never say "Kafka guarantees exactly once" without defining the boundary.

Better answer:

> Kafka can provide idempotent production and transactional consume-transform-produce semantics inside Kafka. Once you update an external database, call an API, send email, or charge money, you must design idempotency, dedupe, outbox, or transactional integration yourself.
