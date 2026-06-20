# Microservices Event Driven Kafka Messaging FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- event-driven architecture
- Kafka fundamentals
- topics, partitions, offsets, consumer groups
- ordering
- consumer lag
- rebalancing
- delivery guarantees
- retry and DLQ
- schema registry and schema evolution
- event replay
- poison messages
- event design

Goal:

```text
After reading this sheet, you should be able to design event-driven microservices using
Kafka or similar brokers, explain ordering and delivery guarantees, and debug consumer lag,
retries, schema evolution, and replay safely.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | producer, consumer, topic, queue, event |
| Intermediate | partitions, offsets, consumer groups, DLQ |
| Senior | ordering, lag, rebalancing, schema compatibility, idempotency |
| FAANG-ready | replay safety, throughput math, operational runbooks, exactly-once caveats |

Must-say line:

```text
Event-driven systems improve decoupling and scalability, but they require explicit design
for ordering, duplicates, retries, schema evolution, and observability.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Event-driven architecture | Very high | Async system design |
| Kafka topic | Very high | Core abstraction |
| Partition | Very high | Scale and ordering |
| Consumer group | Very high | Parallel consumption |
| Offset | Very high | Progress tracking |
| Ordering | Very high | Correctness |
| Consumer lag | Very high | Production debugging |
| Rebalancing | High | Operational behavior |
| At-least-once | Very high | Duplicate handling |
| DLQ/DLT | Very high | Poison messages |
| Schema evolution | High | Safe independent consumers |
| Event replay | High | Recovery and backfill |
| Schema registry | Medium-high | Typed event governance |

---

# 2. Event-Driven Architecture

Event-driven architecture means services publish events when important state changes happen.

Example:

```text
BookingConfirmed
PaymentAuthorized
InventoryReserved
LoyaltyPointsAwarded
```

Consumers react independently.

Benefits:
- loose coupling
- fan-out
- async workflows
- traffic buffering
- audit trail
- independent consumers

Costs:
- eventual consistency
- harder debugging
- duplicate events
- ordering problems
- schema compatibility

Strong answer:

```text
Events are best for facts that already happened. Commands ask a service to do something;
events announce that something happened.
```

---

# 3. Event vs Command

| Command | Event |
|---|---|
| asks for action | announces fact |
| imperative | past tense |
| one intended handler often | many subscribers possible |
| `ReserveInventory` | `InventoryReserved` |
| can be rejected | already happened |

Strong answer:

```text
I name events in past tense because they represent facts. Commands are requests; events are
observations.
```

---

# 4. Kafka Mental Model

```text
Producer -> Topic -> Partitions -> Consumer Group -> Consumers
```

Topic:

```text
booking-events
```

Partitions:

```text
booking-events-0
booking-events-1
booking-events-2
```

Offset:

```text
position of a record inside a partition
```

Strong answer:

```text
Kafka is a distributed append-only log. Producers write records to topic partitions, and
consumers track offsets to know what they have processed.
```

---

# 5. Topics And Partitions

Partitions provide:
- parallelism
- scale
- ordered log per partition
- distribution across brokers

Ordering rule:

```text
Kafka guarantees order within a partition, not globally across all partitions.
```

If booking events must stay ordered:

```text
use bookingId as message key
```

Strong answer:

```text
Key choice is a correctness decision. If all events for one booking use bookingId as key,
they go to the same partition and preserve per-booking order.
```

---

# 6. Consumer Groups

Consumer group lets many consumers share work.

Example:

```text
Topic has 6 partitions.
Consumer group has 3 consumers.
Each consumer gets about 2 partitions.
```

Rule:

```text
Within one consumer group, a partition is consumed by only one consumer at a time.
```

Different services use different groups:

```text
email-service group receives event
analytics-service group also receives event
```

---

# 7. Offset Commit

Offset commit records progress.

Failure cases:

| Case | Result |
|---|---|
| process success, commit fails | duplicate possible |
| process fails, no commit | retry/reprocess |
| commit before processing | message loss risk |

Strong answer:

```text
At-least-once delivery usually means commit offset after successful processing, and make
the consumer idempotent because duplicates can still happen.
```

---

# 8. Delivery Guarantees

| Guarantee | Meaning |
|---|---|
| At-most-once | may lose, no duplicate |
| At-least-once | no loss under normal failure handling, duplicates possible |
| Exactly-once | limited scope and requires careful transactional design |

Interview truth:

```text
Most systems should design for at-least-once delivery plus idempotent processing.
```

Exactly-once caution:

```text
Exactly-once in a broker does not automatically make external side effects exactly-once.
```

---

# 9. Idempotent Consumer

Consumer must handle duplicate events.

Patterns:
- processed event table
- unique constraint on business key
- idempotency key
- status transition guard
- dedupe cache for short window

Example:

```sql
create table processed_events (
    event_id varchar(100) primary key,
    processed_at timestamp not null
);
```

Strong answer:

```text
If an event causes a database write or external side effect, I make the consumer idempotent
using event IDs or business constraints.
```

---

# 10. Consumer Lag

Consumer lag means consumers are behind producers.

Lag grows when:
- producer rate is higher than consumer rate
- consumer processing is slow
- downstream dependency is slow
- poison message blocks partition
- rebalance pauses consumption
- insufficient partitions/consumers

Debug:
1. Check lag by topic/partition/group.
2. Check consumer error rate.
3. Check processing latency.
4. Check downstream dependency latency.
5. Check rebalances.
6. Check DLQ growth.
7. Add consumers only if partitions allow it.

Strong answer:

```text
Consumer lag is a symptom. I check whether the bottleneck is consumer CPU, downstream I/O,
poison messages, insufficient partitions, or frequent rebalancing.
```

---

# 11. Rebalancing

Rebalance redistributes partitions among consumers.

Triggers:
- consumer joins
- consumer leaves
- heartbeat lost
- partition count changes
- deployment rolling restart

Impact:
- temporary pause
- duplicate processing around ownership changes
- latency spikes

Controls:
- graceful shutdown
- stable group membership where supported
- tune poll/heartbeat settings carefully
- avoid slow processing in poll loop
- idempotent consumers

Strong answer:

```text
Rebalancing is normal, but frequent rebalances cause lag and duplicates. I monitor rebalance
rate and make consumers idempotent.
```

---

# 12. Retry Strategies

Bad:

```text
retry same poison message forever in main topic
```

Better options:
- limited in-memory retry
- retry topic with delay
- exponential backoff
- DLQ after max attempts
- classify retryable vs non-retryable

Retryable:
- temporary downstream timeout
- 503
- deadlock

Not retryable:
- invalid schema
- missing required field
- unauthorized
- business rule impossible

---

# 13. Dead Letter Queue / Topic

DLQ stores failed messages after retries.

DLQ record should include:
- original topic
- partition and offset
- event key
- event ID
- failure reason
- exception class
- timestamp
- retry count
- original payload

Strong answer:

```text
DLQ prevents poison messages from blocking progress. But DLQ is not a trash can; it needs
alerts, ownership, replay tooling, and runbooks.
```

---

# 14. Event Replay

Replay means reprocessing old events.

Use cases:
- rebuild read model
- recover bad consumer bug
- backfill new projection
- analytics recomputation

Risks:
- duplicate side effects
- sending old emails again
- external API calls repeated
- order assumptions break
- old schema incompatibility

Safe replay rules:
- consumers are idempotent
- side effects can be disabled or guarded
- replay into separate consumer group/read model
- schema compatibility maintained
- monitor throughput and lag

Strong answer:

```text
Replay is powerful, but only safe when consumers are idempotent and side effects are
controlled. I distinguish state-building consumers from side-effect consumers like email.
```

---

# 15. Schema Evolution

Event schemas evolve over time.

Compatibility modes:
- backward compatible
- forward compatible
- full compatible

Rules:
- add optional fields
- avoid removing required fields
- avoid changing field meaning
- keep enum changes safe
- version event types when necessary
- consumers ignore unknown fields

Strong answer:

```text
Events outlive code deployments. I design schemas additively and use compatibility checks
so old consumers keep working during rolling deploys.
```

---

# 16. Schema Registry

Schema registry stores and validates schemas.

Common with:
- Avro
- Protobuf
- JSON Schema

Benefits:
- compatibility checks
- centralized schema discovery
- typed producers/consumers
- safer evolution

Trade-off:
- extra infrastructure
- governance needed
- not necessary for every small system

---

# 17. Event Design Checklist

Good event includes:
- event ID
- event type
- event version
- occurredAt
- producer
- aggregate ID
- correlation ID
- causation ID
- payload

Avoid:
- full internal entity dump
- sensitive data
- ambiguous field meaning
- huge payloads
- unstable nested models

Example:

```json
{
  "eventId": "evt-123",
  "eventType": "BookingConfirmed",
  "eventVersion": 1,
  "occurredAt": "2026-06-17T10:00:00Z",
  "bookingId": "B123",
  "customerId": "C99",
  "correlationId": "corr-456"
}
```

---

# 18. Topic Design

Options:

| Style | Example | Trade-off |
|---|---|---|
| Domain topic | `booking-events` | simple and cohesive |
| Event-type topic | `booking-confirmed` | easier per-type consumption |
| Command topic | `reserve-inventory-command` | command processing |
| Retry topic | `booking-events-retry-5m` | delayed retry |
| DLT | `booking-events-dlt` | failed messages |

Rule:

```text
Topic design should reflect ownership, ordering, retention, access control, and consumer needs.
```

---

# 19. Ordering Strategies

If ordering is required:
- key by aggregate ID
- keep all related events in same partition
- avoid parallel handling for same key
- design idempotent state transitions
- tolerate duplicates

If global ordering is requested:

```text
Global ordering limits parallelism and is rarely needed. Ask what entity actually needs
ordering, such as one booking or one customer.
```

Strong answer:

```text
I avoid global ordering unless absolutely required. Most business workflows need per-entity
ordering, which is achieved through partition keys.
```

---

# 20. Throughput And Capacity Reasoning

Basic thinking:

```text
required consumers = incoming events per second / events one consumer can process per second
```

Example:

```text
10,000 events/sec incoming
one consumer handles 1,000 events/sec
need about 10 consumers, plus headroom
topic needs at least 10 partitions for one group to use 10 consumers
```

Add headroom:
- spikes
- rebalances
- downstream slowness
- deployment restarts
- replay jobs

---

# 21. Production Scenario: Notification Consumer Lag

Symptom:

```text
Booking confirmations are delayed by 30 minutes.
```

Debug:
1. Check consumer lag by partition.
2. Check email provider latency.
3. Check retry/DLQ counts.
4. Check consumer CPU and thread pool.
5. Check rebalances during deployment.
6. Check whether one partition is hot due to skewed key.
7. Scale consumers only if partitions allow it.
8. Add rate limit/backoff for email provider.

Strong answer:

```text
I would not blindly add pods. First I inspect lag per partition, processing latency, errors,
DLQ, rebalances, and downstream email latency. If partitions are hot or insufficient, scaling
consumers alone may not help.
```

---

# 22. Common Mistakes

| Mistake | Why Wrong | Better |
|---|---|---|
| No event ID | cannot dedupe | include stable event ID |
| Random key for ordered workflow | order lost | key by aggregate ID |
| Infinite retry in main topic | stuck partition | retry topic then DLQ |
| DLQ without owner | silent data loss | alert and runbook |
| Consumer not idempotent | duplicate side effects | processed event table |
| Assume exactly-once everywhere | false safety | design at-least-once |
| Replay email events directly | duplicate emails | guard side effects |
| Breaking schema changes | consumers fail | compatibility strategy |
| Add consumers beyond partitions | no throughput gain | partition planning |

---

# 23. Hot Interview Questions

### Q1. What is Kafka partitioning?

```text
A topic is split into partitions for scale. Ordering is guaranteed within a partition.
```

### Q2. What is consumer lag?

```text
Lag is how far a consumer group is behind the latest produced messages.
```

### Q3. How do you preserve ordering?

```text
Use a stable key like bookingId so all events for that entity go to the same partition.
```

### Q4. What is DLQ?

```text
A place for messages that fail after retries so they do not block normal processing.
```

### Q5. Is exactly-once real?

```text
It exists in limited broker/stream processing scopes, but external side effects still need
idempotency.
```

---

# 24. Final Rapid Revision

| Need | Concept |
|---|---|
| Scale Kafka topic | partitions |
| Track consumer progress | offsets |
| Parallel consumption | consumer group |
| Per-booking order | key by bookingId |
| Duplicate safety | idempotent consumer |
| Failed poison message | DLQ/DLT |
| Old events reprocess | replay |
| Slow consumers | lag |
| Consumer ownership change | rebalance |
| Schema safety | registry/compatibility |
| Reliable DB event | outbox/CDC |

---

# 25. Strong Closing Answer

If interviewer asks:

```text
How do you design event-driven microservices with Kafka?
```

Say:

```text
I model events as facts, use topics around domain ownership, and choose keys based on the
ordering requirement, usually aggregate ID. Consumers are in groups for scale, commit offsets
after successful processing, and remain idempotent because duplicates can happen. I monitor
lag, retries, DLQ, and rebalances. For schema evolution, I use additive changes and schema
compatibility checks. For DB-to-event reliability, I use Outbox, often with CDC.
```

---

# 26. Official Source Notes

Useful references:

- Apache Kafka Documentation: https://kafka.apache.org/documentation/
- Kafka Design: https://kafka.apache.org/documentation/#design
- Kafka Operations: https://kafka.apache.org/documentation/#operations
- AsyncAPI Specification: https://www.asyncapi.com/docs/reference/specification/latest
- CloudEvents Specification: https://cloudevents.io/

