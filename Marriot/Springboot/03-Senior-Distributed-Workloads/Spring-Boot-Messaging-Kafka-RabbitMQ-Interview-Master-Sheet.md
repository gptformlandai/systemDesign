# Spring Boot Messaging Kafka RabbitMQ Interview Master Sheet

Target: Java Backend / Spring Boot / event-driven microservice interviews.

This sheet covers:
- messaging fundamentals
- Kafka with Spring Boot
- RabbitMQ/AMQP with Spring Boot
- producers and consumers
- consumer groups
- partitions and ordering
- acknowledgments
- retries and dead-letter queues
- idempotent consumers
- outbox pattern
- Kafka vs RabbitMQ trade-offs

Goal:

```text
After reading this sheet, you should be able to design and explain reliable asynchronous
messaging in Spring Boot using Kafka or RabbitMQ, including delivery guarantees, retries,
ordering, idempotency, and production failure handling.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | producer, consumer, topic/queue, async communication |
| Intermediate | consumer groups, partitions, acknowledgments, retries |
| Senior | ordering, idempotent consumers, DLQ, outbox, schema evolution |
| MAANG-ready | delivery guarantees, scaling, rebalancing, exactly-once caveats |

Strong line:

```text
Messaging decouples services in time, but it introduces delivery, ordering, retry, and
idempotency problems that must be designed explicitly.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Producer/consumer | Very high | Basic messaging model |
| Topic vs queue | Very high | Kafka/Rabbit distinction |
| Kafka partitions | Very high | Scaling and ordering |
| Consumer groups | Very high | Kafka consumption model |
| Rabbit exchange/routing key | High | AMQP routing |
| Acknowledgments | Very high | Delivery correctness |
| Retry | Very high | Failure handling |
| Dead-letter queue/topic | Very high | Poison message handling |
| Idempotent consumer | Very high | Duplicate delivery safety |
| Outbox pattern | Very high | DB + message consistency |
| Ordering | High | Common tricky requirement |
| Schema evolution | High | Event compatibility |
| Kafka vs RabbitMQ | Very high | Tool choice maturity |

---

# 2. Why Messaging Exists

Synchronous call:

```text
booking-service -> payment-service -> email-service
```

Messaging:

```text
booking-service publishes BookingCreated
payment-service consumes event
email-service consumes event
analytics-service consumes event
```

Benefits:
- decouples services
- absorbs traffic spikes
- enables async workflows
- improves user-facing latency
- supports multiple subscribers

Costs:
- eventual consistency
- duplicate messages
- retry complexity
- ordering issues
- monitoring complexity

Strong answer:

```text
Messaging is useful when work can happen asynchronously or multiple services need to react
to the same event. It improves decoupling, but requires idempotency, retry, DLQ, and
observability.
```

---

# 3. Core Messaging Vocabulary

| Term | Meaning |
|---|---|
| Producer | sends message |
| Consumer | receives message |
| Message | payload plus headers |
| Topic | named stream/category |
| Queue | buffer consumed by workers |
| Broker | messaging server |
| Ack | consumer confirms processing |
| DLQ/DLT | dead-letter queue/topic for failed messages |
| Offset | Kafka consumer position |
| Partition | Kafka topic shard |
| Exchange | RabbitMQ routing component |
| Routing key | RabbitMQ routing value |

---

# 4. Kafka Mental Model

```text
Producer -> Topic -> Partitions -> Consumer Group -> Consumers
```

Kafka topic:

```text
booking-events
  partition 0: event1, event4, event7
  partition 1: event2, event5, event8
  partition 2: event3, event6, event9
```

Key ideas:
- topic is append-only log
- partitions enable parallelism
- ordering is guaranteed only within a partition
- consumer group divides partitions among consumers
- offsets track progress

Strong answer:

```text
Kafka is a distributed commit log. Producers write records to topic partitions, and consumers
track offsets. Consumer groups allow scalable processing, with each partition consumed by
one consumer in the group at a time.
```

---

# 5. Spring Kafka Producer

Example:

```java
@Service
class BookingEventProducer {
    private final KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate;

    BookingEventProducer(KafkaTemplate<String, BookingCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    void publishBookingCreated(BookingCreatedEvent event) {
        kafkaTemplate.send("booking-events", event.bookingId().toString(), event);
    }
}
```

Why key matters:

```text
Same key goes to same partition, preserving order for that key.
```

For booking events, use:

```text
key = bookingId
```

This preserves order per booking.

---

# 6. Spring Kafka Consumer

Example:

```java
@Component
class BookingEventConsumer {
    @KafkaListener(topics = "booking-events", groupId = "email-service")
    void onBookingCreated(BookingCreatedEvent event) {
        // send email
    }
}
```

Consumer group:

```text
email-service group receives each event once per group
analytics-service group receives its own copy
```

Strong answer:

```text
Kafka consumer groups allow each service to process the same topic independently. Within
one group, partitions are distributed across consumers for scaling.
```

---

# 7. Kafka Partitions And Ordering

Ordering rule:

```text
Kafka preserves order only within a partition.
```

If events must be ordered for a booking:

```text
use bookingId as key
```

Bad:

```text
random key for BookingCreated, BookingPaid, BookingCancelled
```

Why bad:

```text
Events for same booking may land in different partitions and be consumed out of order.
```

Better:

```text
same bookingId key for all booking lifecycle events
```

---

# 8. Kafka Offset And Acknowledgment

Offset is consumer's position in a partition.

If consumer processes message and commits offset:

```text
Kafka knows this group progressed.
```

Failure cases:

| Failure | Result |
|---|---|
| process succeeds, offset commit fails | message may be reprocessed |
| process fails before commit | message is retried/reprocessed |
| commit before processing | message can be lost |

Strong answer:

```text
At-least-once delivery means a message can be delivered more than once, so consumers must
be idempotent.
```

---

# 9. RabbitMQ Mental Model

```text
Producer -> Exchange -> Queue -> Consumer
```

Exchange routes messages to queues.

Common exchange types:

| Exchange | Meaning |
|---|---|
| direct | exact routing key match |
| topic | pattern routing |
| fanout | broadcast |
| headers | route by headers |

Strong answer:

```text
RabbitMQ is queue and routing oriented. Producers publish to exchanges, exchanges route
messages to queues, and consumers process queue messages.
```

---

# 10. Spring RabbitMQ Producer

Example:

```java
@Service
class BookingMessageProducer {
    private final RabbitTemplate rabbitTemplate;

    BookingMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    void publishBookingCreated(BookingCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                "booking.exchange",
                "booking.created",
                event
        );
    }
}
```

---

# 11. Spring RabbitMQ Consumer

Example:

```java
@Component
class BookingMessageConsumer {
    @RabbitListener(queues = "booking.email.queue")
    void onBookingCreated(BookingCreatedEvent event) {
        // send confirmation email
    }
}
```

RabbitMQ model:

```text
multiple consumers on same queue share work
multiple queues bound to exchange each get a copy
```

---

# 12. Kafka vs RabbitMQ

| Kafka | RabbitMQ |
|---|---|
| distributed log | message broker/queue |
| topics and partitions | exchanges and queues |
| replay by offset | messages usually removed after ack |
| high-throughput event streams | flexible routing/work queues |
| good for event sourcing/analytics | good for task queues/routing |
| ordering per partition | ordering per queue with caveats |

Strong answer:

```text
I choose Kafka for durable event streams, replay, high throughput, and multiple independent
consumers. I choose RabbitMQ for work queues, command-style messaging, and flexible routing.
```

---

# 13. Delivery Guarantees

| Guarantee | Meaning |
|---|---|
| at-most-once | may lose message, no duplicate |
| at-least-once | no loss if configured, duplicates possible |
| exactly-once | very limited, requires strict boundaries |

Interview truth:

```text
Most practical systems design for at-least-once delivery with idempotent consumers.
```

Strong answer:

```text
I assume duplicate delivery is possible. The consumer must be idempotent using processed
message IDs, unique constraints, or business keys.
```

---

# 14. Idempotent Consumer

Problem:

```text
Message delivered twice.
Consumer charges card twice or sends duplicate email.
```

Solution patterns:
- processed message table
- unique business key
- idempotency key
- status transition check
- dedupe cache for short window

Example:

```sql
create table processed_messages (
    message_id varchar(100) primary key,
    processed_at timestamp not null
);
```

Consumer idea:

```java
@Transactional
void handle(PaymentCaptured event) {
    if (processedMessageRepository.existsById(event.messageId())) {
        return;
    }

    settlementService.createSettlement(event.paymentId());
    processedMessageRepository.save(new ProcessedMessage(event.messageId()));
}
```

Strong answer:

```text
Idempotent consumers are mandatory because retries, rebalances, and offset commit failures
can cause duplicate processing.
```

---

# 15. Retry And Dead-Letter Queue

Retry handles temporary failures.

DLQ/DLT handles poison messages.

Poison message:

```text
same message fails every time because payload is invalid or business state is impossible
```

Pattern:

```text
consume message
try processing
retry limited times
send to dead-letter queue/topic
alert and inspect
```

Strong answer:

```text
Retries should be bounded. After repeated failure, the message should go to a DLQ/DLT with
enough context for debugging.
```

---

# 16. Backoff

Do not retry immediately in a tight loop.

Better:

```text
retry after 1s, 5s, 30s
then dead-letter
```

Why:
- gives dependency time to recover
- avoids hot loop
- protects broker and database

---

# 17. Outbox Pattern

Problem:

```text
Service saves booking in DB, then publishes Kafka event.
DB commit succeeds but Kafka publish fails.
```

Or:

```text
Kafka publish succeeds but DB commit rolls back.
```

Outbox solution:

1. In same DB transaction, save business row and outbox row.
2. Separate publisher reads outbox rows.
3. Publisher sends to broker.
4. Mark outbox row as published.
5. Consumer remains idempotent.

Table:

```sql
create table outbox_events (
    id uuid primary key,
    aggregate_id varchar(100) not null,
    event_type varchar(100) not null,
    payload jsonb not null,
    status varchar(30) not null,
    created_at timestamp not null
);
```

Strong answer:

```text
The outbox pattern solves the dual-write problem between database and broker by writing
the event to an outbox table in the same transaction as the business change.
```

---

# 18. Consumer Rebalancing

Kafka rebalance happens when:
- consumer joins group
- consumer leaves group
- partitions change
- consumer heartbeat fails

Impact:
- partitions are reassigned
- processing may pause
- duplicates can happen around rebalance

Interview line:

```text
Rebalancing is another reason consumers must be idempotent.
```

---

# 19. Schema Evolution

Events live longer than code.

Rules:
- add optional fields, do not break old consumers
- avoid renaming/removing fields without compatibility plan
- include event version if needed
- use schema registry for Avro/Protobuf setups
- keep event meaning stable

Bad:

```text
remove field hotelId because new code does not need it
```

Why bad:

```text
older consumers may still depend on it
```

---

# 20. Message Design

Good event:

```json
{
  "eventId": "1f7d",
  "eventType": "BookingCreated",
  "occurredAt": "2026-06-17T10:00:00Z",
  "bookingId": "B123",
  "customerId": "C99",
  "hotelId": "H10"
}
```

Include:
- event ID
- event type
- event time
- aggregate ID
- version
- correlation ID
- enough data for consumer

Avoid:
- huge payloads
- sensitive data
- internal entity dump
- fields with unclear meaning

---

# 21. Transactions And Messaging

Common mistake:

```text
Assume @Transactional covers database and Kafka/Rabbit automatically.
```

Better mental model:

```text
Database transaction and broker publishing are separate unless explicitly coordinated.
```

For most systems:
- use outbox
- make consumers idempotent
- accept eventual consistency

---

# 22. Observability For Messaging

Track:
- consumer lag
- processing latency
- error rate
- retry count
- DLQ count
- message age
- partition assignment
- rebalance count
- publish failure count
- payload size

Log:
- event ID
- key
- topic/queue
- partition/offset if Kafka
- correlation ID
- exception summary

---

# 23. Production Scenario: Booking Created Event

Requirement:

```text
When a booking is created, send confirmation email, update analytics, and start loyalty
points workflow. User API should not wait for all of this.
```

Design:
1. Booking service saves booking and outbox event in same DB transaction.
2. Outbox publisher sends `BookingCreated` to Kafka topic `booking-events`.
3. Key is `bookingId` to preserve booking-level order.
4. Email service consumes in group `email-service`.
5. Analytics consumes in group `analytics-service`.
6. Consumers are idempotent using `eventId`.
7. Retry transient failures with backoff.
8. Send poison messages to DLT.
9. Monitor lag, failure count, and DLT size.

Strong interview answer:

```text
I would publish BookingCreated asynchronously through an outbox pattern so database commit
and event creation are atomic. Kafka is a good fit because multiple services can consume
the event independently. I would key by bookingId for per-booking ordering, make consumers
idempotent using eventId, retry transient failures with backoff, send poison messages to
DLT, and monitor lag and failure rates.
```

---

# 24. Hot Interview Questions

### Q1. Kafka vs RabbitMQ?

```text
Kafka is a durable distributed log for event streams and replay. RabbitMQ is a flexible
message broker for queues and routing.
```

### Q2. What is a Kafka consumer group?

```text
A set of consumers sharing topic partitions. Each partition is consumed by one consumer
within the group at a time.
```

### Q3. Does Kafka guarantee ordering?

```text
Only within a partition. Use the same key for events that must stay ordered.
```

### Q4. What is at-least-once delivery?

```text
Message should not be lost, but it may be delivered more than once. Consumers must be
idempotent.
```

### Q5. What is DLQ?

```text
A dead-letter queue/topic stores messages that could not be processed after retries.
```

### Q6. What problem does outbox solve?

```text
It solves the dual-write problem between database commit and message publish.
```

### Q7. Why are idempotent consumers important?

```text
Because retries, rebalances, crashes, and commit failures can cause duplicate message
processing.
```

---

# 25. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Assume exactly-once everywhere | unrealistic boundaries | design at-least-once + idempotency |
| No message key | ordering lost | key by aggregate ID |
| Commit offset before processing | message loss risk | commit after success |
| Infinite retries | stuck partition/queue | bounded retry + DLQ |
| No DLQ alert | failures hidden | monitor and alert |
| Publishing inside DB transaction without outbox | dual-write bug | outbox pattern |
| Dumping entity as event | tight coupling | stable event DTO |
| Ignoring schema compatibility | breaks consumers | version/additive changes |
| No consumer lag monitoring | silent backlog | lag metrics |

---

# 26. One-Hour Revision Plan

### First 15 Minutes: Basics

Revise:
- producer
- consumer
- topic
- queue
- broker
- async benefits

Must say:

```text
Messaging decouples services but introduces eventual consistency.
```

### Next 15 Minutes: Kafka

Revise:
- topic
- partition
- key
- consumer group
- offset
- ordering

Must say:

```text
Kafka ordering is per partition, so key choice matters.
```

### Next 15 Minutes: RabbitMQ

Revise:
- exchange
- queue
- routing key
- direct/topic/fanout exchange
- ack

Must say:

```text
RabbitMQ is excellent for work queues and flexible routing.
```

### Final 15 Minutes: Reliability

Revise:
- at-least-once
- idempotent consumer
- retry
- DLQ
- outbox
- schema evolution

Must say:

```text
At-least-once delivery plus idempotent consumers is the practical default.
```

---

# 27. Final Rapid Revision Sheet

| Need | Concept |
|---|---|
| Durable event stream | Kafka |
| Work queue/routing | RabbitMQ |
| Kafka scale unit | partition |
| Kafka ordering boundary | partition |
| Kafka consumer scaling | consumer group |
| Rabbit routing | exchange + routing key |
| Confirm processing | ack |
| Failed poison messages | DLQ/DLT |
| Duplicate-safe consumer | idempotency |
| DB + publish consistency | outbox |
| Event compatibility | schema evolution |
| Message replay | Kafka offsets |
| Per-aggregate order | key by aggregate ID |

---

# 28. Strong Closing Answer

If interviewer asks:

```text
How do you design messaging in Spring Boot?
```

Say:

```text
I choose Kafka when I need durable event streams, replay, high throughput, and multiple
independent consumers. I choose RabbitMQ when I need work queues or flexible routing.
In Spring Boot, producers and consumers are simple to wire, but production correctness comes
from idempotent consumers, bounded retries, DLQ/DLT, proper message keys for ordering,
schema compatibility, observability, and the outbox pattern when database writes and message
publishing must stay consistent.
```

---

# 29. Official Source Notes

Useful official references:

- Spring Boot Kafka: https://docs.spring.io/spring-boot/reference/messaging/kafka.html
- Spring Kafka Reference: https://docs.spring.io/spring-kafka/reference/index.html
- Spring Boot AMQP: https://docs.spring.io/spring-boot/reference/messaging/amqp.html
- Spring AMQP Reference: https://docs.spring.io/spring-amqp/reference/index.html

