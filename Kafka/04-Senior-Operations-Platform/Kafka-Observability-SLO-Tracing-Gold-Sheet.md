# Kafka Observability, SLOs, and Tracing Gold Sheet

> Goal: move from "Kafka is slow" debugging to production-grade signal design: what to measure, what to alert on, what the user feels, and how to trace an event across services.

---

## 1. Intuition

Kafka observability is like air-traffic control for events.

You do not only ask "is the airport open?" You ask:

- Are planes taking off?
- Are planes landing?
- Are any routes backed up?
- Are pilots retrying?
- Is baggage arriving late?
- Is one runway overloaded?

In Kafka terms:

- producers must publish successfully
- brokers must replicate and serve data
- consumers must process within freshness targets
- Connect and Streams must keep state and tasks healthy
- operators must know whether the business is late, not only whether a process is alive

---

## 2. Definition

- Definition: Kafka observability is the practice of measuring broker health, producer behavior, consumer freshness, stream processing state, connector reliability, and business-level event latency.
- Category: production operations / platform engineering / SRE.
- Core idea: instrument the event path from producer write to downstream side effect, then define alerts around user-visible impact.

---

## 3. Why It Exists

Kafka failures are often partial:

- one partition is hot while the cluster looks healthy
- a consumer is alive but not committing offsets
- a connector task is failed while the worker is still running
- producer retries hide broker-side latency
- lag grows slowly until business freshness is broken
- schema failures or poison records stop one pipeline while others keep moving

Without observability, teams debug by guessing.

With observability, teams can say:

> The checkout events topic is healthy, but partition 7 has 4 million records of lag because the payment consumer is failing on one poison schema. The broker ISR is normal; this is an application processing issue.

---

## 4. Observability Layers

| Layer | What You Measure | Main Question |
|---|---|---|
| Business | event freshness, dropped events, duplicate side effects | Are users or workflows impacted? |
| Producer | send rate, error rate, retries, batch latency, record size | Are we publishing correctly and efficiently? |
| Broker | controller health, ISR, partitions, request latency, disk, network | Is the platform durable and available? |
| Consumer | lag, processing rate, commit rate, rebalance count, errors | Are consumers keeping up safely? |
| Streams | task state, state store restore, punctuators, changelog lag | Is stateful processing correct and caught up? |
| Connect | connector/task status, DLQ rate, source lag, sink errors | Are integrations moving data reliably? |
| Governance | ACL denies, quota violations, PII topic access, replay approvals | Is the platform being used safely? |

---

## 5. The Golden Signals For Kafka

### 5.1 Latency

Kafka latency is not one number.

Track:

- producer send latency
- broker request latency
- replication latency
- consumer fetch latency
- consumer processing latency
- end-to-end event freshness
- downstream write latency

Interview trap:

> Consumer lag is not the same thing as end-to-end latency.

A consumer can have low lag but still take too long to call downstream systems. A batch consumer can have high record lag but still meet a 5-minute freshness SLO.

### 5.2 Traffic

Track:

- records in per second
- bytes in per second
- records out per second
- bytes out per second
- requests per second
- fetch rate per consumer group
- partition-level skew

### 5.3 Errors

Track:

- producer send failures
- record too large failures
- authorization failures
- serialization/deserialization failures
- schema compatibility failures
- consumer processing failures
- DLQ rate
- connector task failures
- quota throttling

### 5.4 Saturation

Track:

- disk usage
- disk IO wait
- network throughput
- page cache pressure
- CPU
- request handler utilization
- partition leadership skew
- controller event queue
- remote storage fetch pressure if tiered storage is enabled

---

## 6. Broker Metrics To Know

| Signal | Why It Matters | Typical Action |
|---|---|---|
| Under-replicated partitions | durability risk | check broker/network/disk, avoid unsafe leader election |
| Offline partitions | availability outage | restore broker or reassign leadership |
| Active controller count | should be one active controller role per cluster/controller quorum behavior | investigate controller instability |
| Request latency | broker is slow to serve producer/fetch/admin requests | inspect disk, network, CPU, throttling |
| ISR shrink/expand rate | unstable replication | check slow followers, network, disk |
| Leader count per broker | load balance health | rebalance partition leadership |
| Partition count per broker | metadata and file-handle pressure | plan scaling or topic cleanup |
| Log disk usage | retention/storage risk | adjust retention, add storage, tier storage |
| Network bytes in/out | capacity planning | scale brokers or tune clients |
| Produce/fetch request rate | load pattern | tune batching, compression, partitions |

Strong interview phrasing:

> I would not alert only on CPU. I would alert on user-impacting Kafka signals first: offline partitions, under-replicated partitions, request latency, consumer freshness, connector failures, and DLQ growth.

---

## 7. Producer Metrics

Important producer metrics:

- record send rate
- record error rate
- retry rate
- request latency
- record queue time
- batch size
- compression ratio
- buffer exhaustion
- metadata age
- throttle time

What each means:

| Symptom | Likely Cause | Response |
|---|---|---|
| high retries | broker/network throttling or transient errors | inspect broker request latency and client timeout settings |
| high error rate | auth, schema, serialization, record size, unavailable partitions | separate retriable from non-retriable errors |
| low batch size | low traffic or too low `linger.ms` | tune batching for throughput |
| high buffer wait | producer cannot send fast enough | increase broker capacity, tune `batch.size`, compression, partitions |
| high throttle time | quota hit | tune quotas or isolate tenant |

Producer SLO example:

- 99.9 percent of successful producer sends complete within 200 ms.
- Non-retriable producer errors stay below 0.01 percent over 5 minutes.
- No sustained buffer exhaustion for more than 2 minutes.

---

## 8. Consumer Metrics

Important consumer metrics:

- records consumed per second
- bytes consumed per second
- consumer lag by group/topic/partition
- processing time per record or batch
- commit rate
- commit latency
- poll interval
- rebalance count
- assigned partitions
- deserialization failures
- DLQ writes

Lag interpretation:

| Pattern | Meaning |
|---|---|
| lag grows and processing rate is low | consumer is slow or stuck |
| lag grows only on one partition | hot key, poison record, or partition-specific issue |
| lag spikes during deployments | rebalance or rollout behavior |
| lag falls but errors rise | consumer may be dropping or DLQ-ing events |
| lag is zero but business is stale | bug after consumption, downstream side effect, or wrong consumer group |

Freshness SLO is often better than lag:

```text
freshness = now - event_time_of_last_successfully_processed_record
```

Example:

- Payment authorization events must be reflected in the risk store within 30 seconds for 99.9 percent of events.
- Marketing analytics can tolerate 15 minutes.
- Fraud alerting may require under 5 seconds.

---

## 9. Kafka Streams Metrics

Watch:

- task state: running, restoring, rebalancing, failed
- skipped records
- commit latency
- process rate
- state store restore rate
- changelog topic lag
- standby replica health
- RocksDB metrics when applicable
- repartition topic growth
- stream thread failures

Stateful streaming alerts:

- task stuck in restoring
- changelog lag does not decrease
- repeated rebalance storms
- standby replicas unavailable
- state store disk nearly full
- processing latency exceeds window requirements

Interview framing:

> For Kafka Streams, I monitor both Kafka-level lag and application-level state. A Streams app can consume records but still be unavailable if its local state store is restoring or corrupted.

---

## 10. Kafka Connect Metrics

Watch:

- connector status
- task status
- source record poll rate
- source record write rate
- sink record read rate
- sink record send rate
- task error count
- DLQ rate
- offset commit failures
- rebalance time
- worker availability

Connector failure classes:

| Failure | Example | Mitigation |
|---|---|---|
| source system issue | database unavailable | backoff, alert source owner |
| Kafka issue | topic authorization denied | fix ACL/config |
| sink issue | Elasticsearch timeout | retry, DLQ, scale sink |
| data issue | bad schema, bad field | DLQ, schema fix, replay |
| worker issue | task crashed | restart task, inspect logs |

Production rule:

> Alert on failed tasks, not just failed workers. A Connect cluster can look healthy while one connector task is dead.

---

## 11. Tracing Event Flow

Kafka tracing is harder than synchronous HTTP tracing because messages cross async boundaries.

Use message headers:

- `traceparent`
- `tracestate`
- `correlation_id`
- `event_id`
- `causation_id`
- `producer_service`
- `schema_version`

Event identity fields:

| Field | Purpose |
|---|---|
| `event_id` | unique event instance for idempotency and dedupe |
| `aggregate_id` | business entity key such as order ID |
| `correlation_id` | request/workflow chain |
| `causation_id` | event or command that caused this event |
| `occurred_at` | business event time |
| `published_at` | producer publish time |
| `processed_at` | consumer side-effect time |

Trace lifecycle:

1. API receives request and creates trace context.
2. Producer writes event with trace headers.
3. Consumer extracts trace context from headers.
4. Consumer creates a new span for processing.
5. Downstream DB/API calls continue the trace.
6. Logs include event ID and correlation ID.

Do not rely only on offsets for tracing. Offsets are physical log positions, not business identity.

---

## 12. Logging Strategy

Good Kafka logs include:

- topic
- partition
- offset
- key hash or safe key
- event ID
- schema ID/version
- consumer group
- attempt number
- DLQ topic if applicable
- error class

Avoid:

- logging full PII payloads
- logging secrets or auth headers
- logging only "failed to process"
- logging every record at info level in high-throughput paths

Useful log line shape:

```text
level=ERROR service=payment-consumer group=payment-risk-v1 topic=payments.events partition=7 offset=881921
event_id=evt_123 schema=payment_authorized.v3 error=ValidationError action=sent_to_dlq
```

---

## 13. SLO Design

Do not define Kafka SLOs only around broker uptime. Define them around event outcomes.

### Producer SLO

```text
99.9 percent of accepted order events are published to Kafka within 200 ms.
Non-retriable publish failures are below 0.01 percent over 5 minutes.
```

### Consumer Freshness SLO

```text
99.9 percent of payment events are processed into the risk store within 30 seconds of event time.
```

### Platform SLO

```text
No offline partitions for production topics.
Under-replicated partitions recover within 5 minutes unless a broker is intentionally under maintenance.
```

### Connect SLO

```text
99 percent of source connector tasks remain RUNNING.
DLQ growth for production connectors is investigated within 15 minutes.
```

### Governance SLO

```text
All production PII replay requests are approved and auditable before offsets are reset or DLQ records are reprocessed.
```

---

## 14. Alerting Rules

Good alerts are actionable.

| Alert | Page? | Why |
|---|---|---|
| offline production partition | yes | user-visible availability loss |
| sustained under-replicated partitions | yes | durability risk |
| consumer freshness SLO breached | yes for critical products | business impact |
| DLQ spike | usually yes for critical topics | data quality or code break |
| one connector task failed | yes if production | integration stopped |
| disk usage above threshold | yes with runway | data loss risk |
| producer non-retriable errors | yes | records may not be published |
| CPU high for 1 minute | maybe not | symptom, not impact |

Alert anti-patterns:

- alerting on raw lag without context
- same alert for dev/test/prod
- no topic owner in the alert
- no runbook link
- no severity levels
- no suppression during planned reassignment

---

## 15. Dashboard Layout

### Cluster Overview

- offline partitions
- under-replicated partitions
- controller health
- request latency
- broker disk usage
- network in/out
- partition leadership balance

### Topic View

- records in/out
- bytes in/out
- partition skew
- retention bytes
- compaction backlog
- producer error rate

### Consumer Group View

- lag by topic/partition
- freshness
- processing rate
- rebalance count
- commit latency
- DLQ rate

### Pipeline View

```text
API -> producer -> topic -> consumer/streams/connect -> downstream store -> user-visible result
```

Add one row per hop:

- rate
- error rate
- p95/p99 latency
- freshness
- owner

---

## 16. Failure Modes

### Broker Failure

User observes:

- possible producer latency spike
- possible consumer fetch latency spike
- no outage if replicas and ISR are healthy

What to check:

- leader election
- ISR recovery
- under-replicated partitions
- client retries
- disk and network on remaining brokers

### Poison Message

User observes:

- lag grows on one partition
- consumer may repeatedly fail at same offset

What to check:

- error logs with topic/partition/offset
- schema compatibility
- DLQ rate
- recent deployments

### Rebalance Storm

User observes:

- throughput drops
- lag increases during deployments
- repeated consumer revocations

What to check:

- `max.poll.interval.ms`
- long processing inside poll loop
- unstable instances
- static membership/cooperative rebalancing config

### Connector Task Failure

User observes:

- downstream index/table stops updating
- worker process may still be healthy

What to check:

- connector task status
- sink errors
- DLQ
- converter/schema config
- target system health

---

## 17. Incident Triage Script

Use this sequence when someone says "Kafka is down":

1. Is there user impact? Which product and SLA?
2. Which topic, consumer group, connector, or stream app?
3. Are any partitions offline?
4. Are partitions under-replicated?
5. Is producer error rate elevated?
6. Is consumer freshness breached?
7. Is lag growing on all partitions or only some?
8. Are there recent deployments, schema changes, ACL changes, or quota changes?
9. Are DLQs growing?
10. Is the downstream dependency slow or failing?
11. What is the rollback or mitigation?

Interview-quality conclusion:

> I separate platform health from pipeline health. If brokers are healthy but one consumer group is late, I debug processing, poison records, downstream dependencies, or partition skew before scaling the Kafka cluster.

---

## 18. Code Sample: Event Timing Header

```java
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class ObservableEventPublisher {
    public static ProducerRecord<String, String> buildRecord(String orderId, String payloadJson) {
        String eventId = UUID.randomUUID().toString();
        ProducerRecord<String, String> record =
                new ProducerRecord<>("orders.events", orderId, payloadJson);

        Headers headers = record.headers();
        headers.add("event_id", eventId.getBytes(StandardCharsets.UTF_8));
        headers.add("correlation_id", orderId.getBytes(StandardCharsets.UTF_8));
        headers.add("published_at", Instant.now().toString().getBytes(StandardCharsets.UTF_8));

        return record;
    }
}
```

---

## 19. Practical Question

> You are operating a Kafka-based payment pipeline. Customers report that refunds appear late in the UI. Broker CPU is normal. How would you debug it?

---

## 20. Strong Answer

I would not start by scaling Kafka. I would trace the refund event path:

1. Confirm business impact and freshness SLO breach.
2. Check producer publish errors and latency for refund events.
3. Check broker health: offline partitions, under-replicated partitions, request latency.
4. Check the refund consumer group lag by partition and freshness.
5. If one partition is bad, look for hot keys or a poison record at a specific offset.
6. If all partitions are slow, inspect downstream dependencies and consumer processing latency.
7. Check recent schema, ACL, deployment, and quota changes.
8. If records are failing, verify DLQ growth and replay policy.
9. Mitigate by pausing risky traffic, scaling consumers if partitioning allows, bypassing a slow downstream path, or rolling back bad code/schema.

Trade-off:

- scaling consumers helps only if there are enough partitions and processing is CPU-bound
- offset reset/replay can fix missed events but must be governed to avoid duplicate refunds

---

## 21. Revision Notes

- One-line summary: Kafka observability measures the event journey, not just broker uptime.
- Three keywords: freshness, lag, DLQ.
- One interview trap: treating consumer lag as the only latency metric.
- One memory trick: producer, broker, consumer, side effect, business freshness.

---

## 22. Official Source Notes

- Apache Kafka monitoring docs: <https://kafka.apache.org/43/operations/monitoring/>
- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka Connect docs: <https://kafka.apache.org/43/kafka-connect/overview/>
- Apache Kafka Streams docs: <https://kafka.apache.org/43/streams/introduction/>
