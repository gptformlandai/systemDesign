# Kafka Production Event Platform Capstone

> Goal: prove Kafka mastery by designing, operating, debugging, securing, observing, and safely replaying a real production event platform.

---

## 1. Capstone Scenario

You are designing the event platform for a marketplace.

Core business flows:

- users place orders
- payments are authorized and captured
- inventory is reserved and released
- sellers receive fulfillment events
- buyers receive notifications
- analytics teams consume business events
- fraud systems need near-real-time signals
- search and reporting systems need projections

The company wants Kafka to become the backbone for reliable asynchronous workflows and event-driven data products.

---

## 2. Requirements

### Functional Requirements

- publish order, payment, inventory, fulfillment, notification, and fraud events
- preserve per-order ordering where required
- support multiple independent consumer groups
- support schema evolution
- support CDC from selected databases
- support stream processing for fraud and analytics
- support sink connectors for search/reporting stores
- support DLQ and replay for bad records
- support audit for sensitive events

### Non-Functional Requirements

- high availability for production topics
- durable event retention for replay windows
- low-latency fraud processing
- high-throughput analytics ingestion
- secure tenant/team isolation
- observable freshness and lag
- controlled backfill and replay
- safe upgrades and platform operations

---

## 3. Architecture Sketch

```text
API services
  -> transactional outbox tables
  -> CDC connectors
  -> Kafka topics
  -> consumer services
  -> Kafka Streams apps
  -> Connect sink connectors
  -> read models / search / analytics / notification / fraud systems
```

Key principle:

> Kafka is the durable event backbone, but every external side effect still needs idempotency and monitoring.

---

## 4. Topic Design

Suggested topics:

| Topic | Key | Retention | Compaction | Notes |
|---|---|---|---|---|
| `orders.events.v1` | `order_id` | 14-30 days | no | order lifecycle events |
| `payments.events.v1` | `payment_id` or `order_id` | 30-90 days | no | sensitive, strict governance |
| `inventory.events.v1` | `sku_id` | 14-30 days | optional | preserve SKU ordering |
| `fulfillment.events.v1` | `shipment_id` | 14-30 days | no | seller workflow |
| `notifications.commands.v1` | `notification_id` | 7-14 days | no | task-like; avoid duplicate sends |
| `customer.profile.v1` | `customer_id` | long | yes | latest profile state |
| `fraud.features.v1` | `order_id` | 7-30 days | no | low latency |
| `dlq.events.v1` | original key | 30-90 days | no | preserve source metadata |

Partitioning:

- key by aggregate when ordering matters
- avoid low-cardinality keys such as country/status
- estimate partition count from throughput, consumer parallelism, and future growth
- do not casually increase partitions for keyed ordered topics without understanding ordering impact

---

## 5. Schema and Event Design

Event envelope:

```json
{
  "event_id": "evt_123",
  "event_type": "OrderPlaced",
  "event_version": 1,
  "aggregate_id": "order_456",
  "correlation_id": "req_789",
  "causation_id": "cmd_001",
  "occurred_at": "2026-07-02T10:00:00Z",
  "producer": "order-service",
  "payload": {
    "order_id": "order_456",
    "buyer_id": "buyer_10",
    "total_amount": "42.00",
    "currency": "USD"
  }
}
```

Rules:

- use a schema registry for contract enforcement
- prefer backward-compatible changes
- avoid removing fields without a migration window
- avoid placing large blobs in Kafka events
- classify PII fields
- preserve event IDs for dedupe and replay
- include correlation and causation IDs for tracing

---

## 6. Producer Design

For critical events:

- use idempotent producer
- use `acks=all`
- configure retries and delivery timeout intentionally
- use stable keys
- monitor producer errors and retry rate
- use outbox pattern when publishing after database writes

Outbox flow:

```text
business transaction writes order row + outbox row
CDC connector reads outbox
CDC publishes event to Kafka
consumer processes event idempotently
```

Why:

- avoids database commit succeeding while Kafka publish fails
- preserves recoverable event publishing
- supports audit and replay from outbox if needed

---

## 7. Consumer Design

Consumer rules:

- process records idempotently
- commit offsets only after successful processing
- use retry topics for transient failures
- use DLQ for poison records with full metadata
- avoid long blocking operations inside the poll loop
- use static membership/cooperative rebalancing where appropriate
- monitor lag, freshness, rebalance count, and DLQ rate

Idempotency table:

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL,
    consumer_name VARCHAR(100) NOT NULL
);
```

Crash window to explain:

```text
process side effect succeeds
-> app crashes before offset commit
-> Kafka redelivers
-> idempotency prevents duplicate side effect
```

---

## 8. Kafka Streams Design

Use Streams for:

- fraud feature aggregation
- windowed order/payment matching
- inventory availability projections
- seller performance metrics
- enrichment where state stores help

Streams concerns:

- event time vs processing time
- window size and grace period
- state store changelog topics
- standby replicas
- repartition topics
- topology testing
- reset strategy
- output topic idempotency

Testing requirement:

- use topology-level tests for transformations
- use integration tests with embedded or containerized Kafka for serialization and broker behavior
- test late events and schema evolution

---

## 9. Kafka Connect and CDC Design

Use Connect for:

- CDC from outbox tables
- sink to Elasticsearch/OpenSearch/search service
- sink to analytics lake
- source from legacy systems where appropriate

Operational rules:

- monitor worker health and task health separately
- configure converters and schema behavior explicitly
- preserve DLQ metadata
- test connector upgrades
- plan offset backup and connector restart behavior
- do not let a failed task hide behind a healthy worker process

---

## 10. Security and Governance

Security controls:

- TLS for in-transit encryption
- SASL or mTLS for authentication
- ACLs by service principal
- least privilege per topic and group
- quotas per tenant/team
- separate prod/non-prod clusters or strong namespace controls

Governance controls:

- topic owner
- data classification
- schema owner
- retention policy
- replay approver
- DLQ owner
- PII minimization
- audit trail

Sensitive topics:

- payments
- refunds
- customer profile
- fraud
- notifications with contact data

Rule:

> A team should not be able to replay sensitive production events without approval and audit.

---

## 11. Observability and SLOs

Platform dashboards:

- offline partitions
- under-replicated partitions
- broker request latency
- disk usage
- controller health
- topic throughput
- partition skew

Pipeline dashboards:

- producer error rate
- consumer group lag
- event freshness
- processing latency
- DLQ rate
- connector task status
- stream task state
- downstream error rate

Example SLOs:

- 99.9 percent of order events are available to consumers within 1 second of publish.
- 99.9 percent of fraud signals are processed within 5 seconds of event time.
- No offline partitions for production topics.
- DLQ spikes on payment topics are investigated within 15 minutes.

---

## 12. Replay and Backfill Plan

Replay policy:

1. classify replay risk
2. identify offset/timestamp window
3. snapshot current offsets
4. prove idempotency
5. run dry run
6. replay with rate limit
7. monitor output
8. audit result

Payment/refund replay:

- highest risk
- require idempotency key
- require approval
- consider shadow validation first
- never blindly reset to earliest

Analytics backfill:

- lower risk
- use separate consumer group
- write to shadow table/topic first
- validate counts before promotion

---

## 13. Kubernetes and Platform Operations

If running Kafka on Kubernetes with an operator such as Strimzi:

- manage brokers through custom resources
- use persistent volumes with known performance
- plan broker rack/zone placement
- use pod disruption budgets
- perform rolling upgrades through operator-supported flow
- monitor PVC capacity and broker restart behavior
- use Cruise Control or equivalent only with clear capacity constraints

If using managed Kafka:

- verify feature support
- verify quotas
- verify tiered storage behavior
- verify ACL and networking model
- verify upgrade policy
- verify metrics exposed to your observability stack

---

## 14. Failure Drills

You must be able to explain and run these drills:

| Drill | Expected Response |
|---|---|
| broker failure | leaders move, ISR monitored, no data loss if replication healthy |
| hot partition | identify key skew, split workload, redesign key/topic if needed |
| consumer lag spike | separate broker health from app/downstream bottleneck |
| poison message | isolate offset, send to DLQ, fix schema/code, replay safely |
| schema break | block incompatible producer, roll back schema/code, replay failed records |
| connector task failure | inspect task status, fix root cause, restart task, watch DLQ |
| Streams state restore stuck | inspect changelog lag, local disk, app reset strategy |
| replay mistake | stop consumers, restore saved offsets if possible, audit side effects |

---

## 15. Local Implementation Milestones

Milestone 1: local cluster and CLI

- start Kafka locally
- create topics
- produce and consume records
- inspect consumer groups
- reset offsets in a dry-run lab

Milestone 2: producer and consumer

- build an idempotent producer
- build a consumer with manual offset commit
- add retry and DLQ handling
- add event IDs and correlation IDs

Milestone 3: schema

- define an event envelope
- test compatible and incompatible schema changes
- document field evolution rules

Milestone 4: stream processing

- build a windowed aggregation
- test late events
- test topology logic
- inspect internal topics

Milestone 5: Connect/CDC

- configure a source or sink connector
- inspect connector/task status
- test DLQ behavior
- restart failed tasks

Milestone 6: operations

- create dashboards or metric checklist
- simulate consumer lag
- simulate poison messages
- run a replay with a controlled group

---

## 16. Capstone Interview Prompt

> Design a Kafka-based event platform for a marketplace where order, payment, inventory, fraud, notification, and analytics systems communicate asynchronously. Explain topic design, partitioning, schemas, producer guarantees, consumer failure handling, stream processing, Connect/CDC, security, observability, replay, and disaster recovery.

---

## 17. Strong Answer Structure

1. Start with requirements and critical workflows.
2. Define topics, keys, retention, compaction, and partition strategy.
3. Define schemas and event envelope.
4. Explain producer guarantees and outbox/CDC.
5. Explain consumer groups, idempotency, retries, DLQ, and commits.
6. Use Streams for stateful transformations and Connect for integrations.
7. Cover broker/platform operations: replication, ISR, KRaft, quotas, capacity, DR.
8. Cover security: TLS, auth, ACLs, tenant isolation, PII.
9. Cover observability: freshness, lag, DLQ, broker health, connector/task health.
10. Cover replay: dry-run, offset snapshots, approval, idempotency, rate limits.
11. State trade-offs and alternatives.

---

## 18. Scoring Rubric

| Area | 1 | 3 | 5 |
|---|---|---|---|
| Fundamentals | vague queue answer | explains topics/partitions/groups | explains log, ordering, offsets, fan-out clearly |
| Delivery | says "exactly once" broadly | knows at-least-once/idempotency | defines all crash windows and EOS boundaries |
| Design | topics are generic | reasonable keys/retention | strong topic/schema/partition governance |
| Operations | says "monitor lag" | checks broker and consumer metrics | gives full incident triage and SLO plan |
| Security | mentions ACLs | covers auth/TLS/ACLs | covers PII, tenant isolation, audit, replay governance |
| Replay | resets offsets casually | dry-run and idempotency | offset snapshots, approvals, rate limits, side-effect safety |
| Communication | scattered | mostly structured | crisp, trade-off driven, senior-level |

Target:

- 4/5 average for senior readiness
- 5/5 in fundamentals, delivery, and operations for MAANG-style Kafka rounds

---

## 19. Final Checklist

You are capstone-ready when you can answer these without notes:

- Why Kafka instead of a queue?
- What ordering does Kafka guarantee?
- What does Kafka not guarantee?
- How do you choose a partition key?
- How do you estimate partition count?
- What happens when a broker dies?
- What happens when a consumer crashes after side effect but before commit?
- How do you avoid duplicate charges?
- How do you evolve schemas?
- How do you debug lag?
- How do you safely replay a 4-hour incident window?
- How do you secure PII topics?
- How do you monitor Connect tasks?
- How do you reset a Streams app safely?
- What changes in KRaft-era operations?
- What would you do differently on managed Kafka?

---

## 20. Revision Notes

- One-line summary: The capstone proves Kafka as a production event platform, not just an API.
- Three keywords: design, operate, recover.
- One interview trap: focusing only on topic/consumer basics and ignoring replay, observability, and governance.
- One memory trick: topic design gets you running; operations and replay keep you alive.

---

## 21. Official Source Notes

- Apache Kafka 4.3 documentation: <https://kafka.apache.org/43/>
- Apache Kafka getting started with Docker: <https://kafka.apache.org/43/getting-started/docker/>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>
- Apache Kafka operations docs: <https://kafka.apache.org/43/operations/basic-operations/>
- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Apache Kafka Streams docs: <https://kafka.apache.org/43/streams/introduction/>
- Apache Kafka Streams testing docs: <https://kafka.apache.org/43/streams/developer-guide/testing/>
- Apache Kafka Connect docs: <https://kafka.apache.org/43/kafka-connect/overview/>
- Strimzi operator overview: <https://strimzi.io/docs/operators/latest/overview.html>
