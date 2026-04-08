# Asynchronous & Event-Driven Systems - Mentorship Track

> Goal: build strong intuition and interview-ready depth for asynchronous architectures, queues, streams, delivery semantics, and failure handling.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `1.7 Asynchronous & Event-Driven Systems`.
- We will follow the same learning style used in the communication-model notes.
- We will add topics one by one in a repeatable architect-level structure.
- We will include code samples, mini programs, and interview-style answers.

---

## Roadmap for This Sheet

1. Message queues vs event streams
2. At-most-once vs at-least-once vs exactly-once
3. Idempotent consumers
4. Dead-letter queues
5. Backpressure handling
6. Event-driven architecture

---

# Topic 1: Message Queues vs Event Streams

> Track: 1.7 Asynchronous & Event-Driven Systems
> Scope: task distribution, fan-out, retention, replay, ordering, consumer groups

---

## 1. Intuition

Think of a queue like a work inbox.

One producer drops work into the inbox, and one worker takes each task and finishes it. Once the task is done, it is acknowledged and disappears from the inbox.

Think of an event stream like a durable activity log.

Events are appended in order, and many different readers can look at the same history independently. One consumer may read for analytics, another for notifications, another for audit, and none of them block each other.

Short memory trick:
- Queue = "someone should do this work"
- Stream = "this event happened; interested systems may react"

---

## 2. Definition

- Definition: A message queue is an asynchronous work distribution system where producers send messages for consumers to process, usually with the expectation that each message is handled by one consumer worker.
- Definition: An event stream is an append-only ordered log of events that multiple independent consumers can read, replay, and process at their own pace.
- Category: Asynchronous communication and decoupling patterns
- Core idea: Queues optimize for work dispatch; streams optimize for durable event distribution and replayable history.

---

## 3. Why It Exists

Synchronous systems couple producer speed to consumer speed.

Queues and streams exist because:
- producers and consumers should not need to be online at the same time
- burst traffic should be absorbed safely
- downstream work should be decoupled from user-facing requests
- many systems need retries, buffering, replay, and independent consumers

The distinction exists because not all asynchronous workloads are the same:
- some workloads are tasks that should be processed once by one worker
- some workloads are events that many services need to observe independently

---

## 4. Reality

### Message queues are common in:

- email sending
- image or video processing jobs
- background invoice generation
- payment retry workflows
- order fulfillment task dispatch
- worker pools and job scheduling

Typical examples:
- Amazon SQS
- RabbitMQ
- ActiveMQ

### Event streams are common in:

- order-created and payment-completed event fan-out
- analytics pipelines
- clickstream ingestion
- CDC and data replication
- audit logs
- event sourcing architectures

Typical examples:
- Apache Kafka
- Apache Pulsar
- Amazon Kinesis

### Real-world architecture truth

Strong systems often use both:
- a stream for domain events and fan-out
- a queue for worker-specific background jobs and retries

---

## 5. How It Works

### Message queue flow

1. Producer sends a task message to a queue.
2. The queue stores the message until a consumer is ready.
3. One worker receives the message.
4. The worker processes the task.
5. If successful, the worker acknowledges the message.
6. If the worker fails or times out, the message may be retried or sent to a dead-letter queue.

Key property:
- the message is usually meant to be completed by one consumer path, not replayed forever by many independent readers.

### Event stream flow

1. Producer appends an event to a stream or topic.
2. The broker persists the event in order within a partition.
3. Consumers read by offset from the stream.
4. Each consumer group tracks its own progress independently.
5. Events remain retained for a configured period or size.
6. Consumers can replay older events by resetting offsets.

Key property:
- multiple consumers can independently read the same event history.

### One-line contrast

- Queue = deliver work to a worker
- Stream = publish durable events to many readers

---

## 6. What Problem It Solves

- Primary problem solved by queues: decoupled background work distribution with buffering and retry
- Primary problem solved by streams: scalable event fan-out with retention, ordering, and replay
- Secondary benefits: burst absorption, failure isolation, async scalability, reduced request latency on the hot path
- Systems impact: affects throughput smoothing, consumer independence, recovery strategy, and architecture style

---

## 7. When to Rely on It

Use a message queue when:
- one worker or one consumer path should process the task
- the message represents work to be completed
- retries and DLQ behavior are important
- you want simple async offloading from request-response flows

Use an event stream when:
- multiple independent consumers need the same event
- replay matters
- ordering matters within a key or partition
- you want event history retained for some time
- analytics, CDC, audit, or fan-out are first-class needs

Strong interviewer keywords for queues:
- background jobs
- task dispatch
- retryable work
- worker pool
- asynchronous processing after API return

Strong interviewer keywords for streams:
- fan-out
- replay
- consumer groups
- retention
- analytics
- event-driven microservices

---

## 8. When Not to Use It

Avoid a queue when:
- many systems need to read the same message independently
- you need durable replayable history
- you need long-lived ordered event logs

Avoid a stream when:
- all you need is simple job dispatch to one worker pool
- the team does not need replay, multiple consumers, or log-style retention
- operational simplicity matters more than stream semantics

Better alternatives:
- request-response for short synchronous work
- database state change with polling only for very small/simple systems
- webhooks for server-to-server notification when you do not want to host shared async infrastructure

---

## 9. Pros and Cons

| Pattern | Pros | Cons |
|---|---|---|
| Message queue | Simple async offloading, retries, worker-pool friendly | Weak fit for multi-consumer replayable event history |
| Event stream | Great fan-out, retention, replay, consumer independence | More operational complexity and partition/offset thinking |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Simplicity vs capability:
  Queues are simpler for task execution; streams are more powerful for event distribution.
- Single-consumer work vs multi-consumer fan-out:
  Queues shine when one worker path should act; streams shine when many systems must react.
- Short-lived message handling vs durable history:
  Queues focus on delivery and acknowledgment; streams preserve log-style history.
- Operational ease vs replay/debug power:
  Streams are harder to run well, but they give stronger replay and auditing capabilities.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Using a queue for many downstream consumers | Creates awkward duplication or extra fan-out layers | Use an event stream or pub/sub style backbone |
| Using a stream for simple email-job dispatch | Adds unnecessary partition and offset complexity | Use a queue |
| Assuming "exactly once" end to end without idempotency | Distributed retries and failures still create duplicates | Design consumers to be idempotent |
| Ignoring partition keys in streams | Leads to hot partitions and broken ordering assumptions | Choose keys intentionally |
| Treating the broker like a database of record | Brokers are not a substitute for proper state ownership | Persist business state separately when needed |

---

## 11. Key Numbers

These are practical heuristics and not universal laws.

- Queue backlog:
  queue depth and age of oldest message are top operational signals
- Visibility timeout:
  usually longer than expected processing time, often 2-10x the median task duration
- DLQ threshold:
  often after 3-5 failed processing attempts, depending on task criticality
- Stream retention:
  often days to weeks, depending on replay and audit needs
- Consumer lag:
  lag is the core stream health signal
- Partition count:
  stream throughput and parallelism usually scale with partitions
- Ordering rule:
  strong ordering is generally per partition, not across the entire stream

Interview shorthand:
- queues care about backlog and retries
- streams care about lag, partitions, retention, and replay

---

## 12. Failure Modes

### Poison message in a queue

Problem:
- A message always fails processing because of malformed payload or logic error.

User impact:
- repeated retries, worker waste, growing backlog

Mitigation:
- DLQ
- retry limits
- payload validation

### Consumer lag in a stream

Problem:
- Consumers fall behind while producers keep appending.

User impact:
- stale analytics, delayed notifications, downstream SLAs missed

Mitigation:
- scale consumers
- increase partitions where appropriate
- optimize consumer processing

### Duplicate delivery

Problem:
- A consumer processes a message or event, crashes before committing ack/offset, and receives it again.

User impact:
- duplicate emails, duplicate state changes, duplicate side effects

Mitigation:
- idempotent consumers
- dedupe keys
- transactional patterns where appropriate

### Hot partition in a stream

Problem:
- One partition key receives most traffic.

User impact:
- uneven throughput, lag concentrated on one partition, reduced parallelism

Mitigation:
- better partition-key choice
- sharding hot keys
- workload-aware partition design

---

## 13. Scenario

- Product / system: E-commerce platform
- Queue use case:
  After an order is placed, generate invoice PDF, send email, and trigger thumbnail generation for order assets through worker queues.
- Stream use case:
  Publish `OrderCreated` events so inventory, analytics, fraud detection, notifications, and audit consumers can all react independently.
- Why this split is good:
  The queue handles concrete tasks; the stream distributes business events to many domains.
- What would go wrong without this distinction:
  A queue would make multi-consumer fan-out awkward, while a stream would overcomplicate simple job execution.

---

## 14. Code Sample

### Queue-style worker pseudocode

```java
public void handleQueueMessage(OrderEmailJob job) {
    try {
        emailService.sendOrderConfirmation(job.orderId(), job.email());
        queueClient.ack(job.messageId());
    } catch (TransientException ex) {
        queueClient.retryLater(job.messageId());
    } catch (Exception ex) {
        queueClient.moveToDeadLetterQueue(job.messageId());
    }
}
```

### Stream-style consumer pseudocode

```java
public void onOrderCreated(EventRecord record) {
    OrderCreatedEvent event = deserialize(record.value(), OrderCreatedEvent.class);

    analyticsService.recordOrder(event);
    notificationService.prepareOrderNotification(event);

    streamClient.commitOffset(record.partition(), record.offset());
}
```

### Stream producer example

```java
public void publishOrderCreated(Order order) {
    OrderCreatedEvent event = new OrderCreatedEvent(order.id(), order.userId(), order.total());
    streamClient.publish("order-events", order.id(), serialize(event));
}
```

Key idea:
- Queue code is centered on ack, retry, and DLQ.
- Stream code is centered on partitions, offsets, replay, and multiple consumer groups.

---

## 15. Mini Program / Simulation

This mini program shows the behavioral difference:
- queue messages are consumed once from the shared work list
- stream events stay in the log and each consumer keeps its own offset

```python
from collections import deque


def queue_demo() -> None:
    queue = deque(["job-1", "job-2", "job-3"])
    worker_a = queue.popleft()
    worker_b = queue.popleft()

    print("Queue demo")
    print("Worker A got:", worker_a)
    print("Worker B got:", worker_b)
    print("Remaining queue:", list(queue))


def stream_demo() -> None:
    stream = ["event-1", "event-2", "event-3"]
    offsets = {
        "analytics-consumer": 0,
        "notification-consumer": 0,
    }

    print("\nStream demo")
    for consumer, offset in offsets.items():
        print(consumer, "reads:", stream[offset:])


def main() -> None:
    queue_demo()
    stream_demo()


if __name__ == "__main__":
    main()
```

What this demonstrates:
- queue work is shared among workers
- stream history is independently readable by multiple consumers
- queue semantics and stream semantics solve different problems

---

## 16. Practical Question

> You are designing an order platform. After checkout, the system must send email, update inventory, feed analytics, trigger fraud checks, and persist an audit trail. Would you use a message queue, an event stream, or both? Explain where each fits and why.

---

## 17. Strong Answer

I would use both, but for different responsibilities.

I would publish domain events such as `OrderCreated` to an event stream because multiple independent consumers need to react: analytics, audit, fraud detection, notifications, and potentially more services in the future. A stream gives me fan-out, retention, consumer independence, and replay if I need to rebuild downstream state.

For concrete background jobs such as sending an email or generating a document, I would use a message queue because those are task-oriented workloads where one worker path should process the item, with clear retry and DLQ handling.

The key trade-off is that queues are simpler and excellent for work dispatch, while streams are stronger for durable event propagation across many services. I would avoid forcing everything into one model. In practice, strong architectures often use a stream for domain events and queues for worker execution.

---

## 18. Revision Notes

- One-line summary: Use queues for work that should be done, and streams for events that should be known and replayed by many consumers.
- Three keywords: dispatch, fan-out, replay
- One interview trap: treating queues and streams as interchangeable
- One memory trick: queue = inbox, stream = history

---

# Topic 2: At-most-once vs At-least-once vs Exactly-once

> Track: 1.7 Asynchronous & Event-Driven Systems
> Scope: delivery guarantees, retries, acknowledgments, duplicates, transactional processing

---

## 1. Intuition

Think of package delivery guarantees.

- At-most-once:
  the package is sent once and never retried. It may arrive, or it may be lost, but it should not arrive twice.
- At-least-once:
  the sender keeps retrying until it is sure the package arrived. This reduces loss, but the receiver may get duplicates.
- Exactly-once:
  the business outcome should happen once, even across retries and failures.

That last one sounds perfect, but it is also the most expensive and the most misunderstood.

Short memory trick:
- at-most-once = maybe lost, not duplicated
- at-least-once = not lost easily, maybe duplicated
- exactly-once = one effective outcome, but only with strong coordination

---

## 2. Definition

- Definition: Delivery semantics describe what a broker and consumer system can guarantee about message delivery under retries and failures.
- Category: Reliability and correctness semantics in asynchronous systems
- Core idea: Failures force a trade-off between message loss, duplication, and coordination complexity.

More specifically:
- At-most-once: a message is delivered zero or one time.
- At-least-once: a message is delivered one or more times.
- Exactly-once: the message's effect is observed exactly once within a defined system boundary.

---

## 3. Why It Exists

Distributed systems fail in the middle of work.

Messages can be:
- produced and never persisted
- read and processed but not acknowledged
- acknowledged too early
- written to a downstream system before a crash

So systems need explicit semantics for what they optimize:
- lower latency and simplicity
- lower loss risk
- stronger correctness

This topic exists because you cannot eliminate all failure windows for free. You must choose the failure mode you can tolerate.

---

## 4. Reality

### At-most-once shows up in:

- fire-and-forget telemetry
- low-value monitoring events
- best-effort notifications
- systems where dropping some events is acceptable

Why teams choose it:
- simple
- low overhead
- no retry storms

### At-least-once shows up in:

- most production queues
- stream consumers by default
- order/event processing
- email jobs
- asynchronous workflows where loss is unacceptable

Why teams choose it:
- practical default
- easier than exactly-once
- acceptable when consumers are idempotent

### Exactly-once shows up in:

- carefully controlled stream-processing pipelines
- financial or ledger-like internal workflows
- systems using transactional processing with offset/state coordination

Why teams choose it:
- they need stronger correctness inside a narrow boundary

Real-world truth:
- most real systems are at-least-once plus idempotent consumers
- "exactly-once" is usually limited to a specific boundary, not the entire business world

---

## 5. How It Works

### At-most-once flow

1. Producer sends a message.
2. Consumer receives it.
3. The system marks it as delivered or moves on without retry guarantees.
4. If processing fails after that point, the message may be lost.

Typical way this happens:
- auto-commit offsets before processing
- acknowledge too early
- do not retry failed delivery

### At-least-once flow

1. Producer sends a message.
2. Broker persists it.
3. Consumer reads and processes the message.
4. Consumer acknowledges or commits only after successful processing.
5. If the consumer crashes before the ack/commit, the message is delivered again.

Typical result:
- duplicates are possible
- loss is less likely

### Exactly-once flow

1. Producer writes messages with idempotent or transactional guarantees.
2. Consumer processes the message.
3. Consumer updates its output/state and its consumed position atomically or transactionally.
4. If the transaction succeeds, both the result and progress marker succeed together.
5. If not, both roll back together.

Typical result:
- within that processing boundary, duplicates do not create duplicate effects

Important nuance:
- exactly-once across external side effects like "send email" or "charge card" is much harder
- in practice, that often still requires idempotency keys, dedupe, and compensating logic

### One-line contrast

- At-most-once protects against duplicates by accepting loss
- At-least-once protects against loss by accepting duplicates
- Exactly-once tries to avoid both, but only with more coordination and narrower guarantees

---

## 6. What Problem It Solves

- Primary problem solved: defining how the system behaves when delivery, processing, and acknowledgment do not happen cleanly
- Secondary benefits: clearer correctness model, better retry policy design, safer system behavior under crashes
- Systems impact: changes consumer design, state management, transaction boundaries, and operational cost

---

## 7. When to Rely on It

Use at-most-once when:
- message loss is acceptable
- duplicates are more harmful than occasional loss
- you want minimal delivery overhead

Use at-least-once when:
- message loss is not acceptable
- the consumer can be made idempotent
- retries are a normal and expected part of the system

Use exactly-once when:
- correctness requirements are strict
- you can define a tight system boundary
- the platform supports transactional or coordinated semantics
- the added complexity is justified

Strong interviewer keywords:
- "loss tolerant" -> consider at-most-once
- "retries are okay, but duplicates must be handled" -> at-least-once plus idempotency
- "financial correctness inside the stream pipeline" -> carefully discuss exactly-once

---

## 8. When Not to Use It

Avoid at-most-once when:
- missing a message is unacceptable
- the message represents critical business state

Avoid at-least-once when:
- the consumer causes irreversible non-idempotent side effects without protection

Avoid exactly-once when:
- the team is using the phrase loosely without defining the boundary
- the problem can be solved more simply with idempotent consumers
- the downstream effects include third-party systems that cannot participate transactionally

Better alternative thinking:
- prefer at-least-once plus idempotency for most business systems
- use exactly-once only where the platform and business need truly justify it

---

## 9. Pros and Cons

| Guarantee | Pros | Cons |
|---|---|---|
| At-most-once | Simple, low overhead, no duplicate retries | Can lose messages |
| At-least-once | Safer against loss, practical default | Duplicates are normal and must be handled |
| Exactly-once | Stronger correctness within a defined boundary | Highest complexity, coordination cost, and misunderstanding risk |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Loss vs duplication:
  at-most-once risks loss, at-least-once risks duplicates.
- Simplicity vs correctness:
  exactly-once is stronger but harder and more expensive.
- Broker guarantees vs business guarantees:
  even strong broker semantics do not automatically make external side effects exactly once.
- Retry safety vs operational complexity:
  at-least-once is very practical when consumers are safe to re-run.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Saying "exactly-once" without defining system boundaries | The phrase becomes meaningless without scope | State where the guarantee starts and ends |
| Trusting the broker alone to prevent duplicate side effects | Crashes can happen after side effects but before ack/commit | Make consumers idempotent |
| Using at-most-once for important business events | A single transient failure can permanently lose work | Use at-least-once or stronger safeguards |
| Auto-committing before processing in a critical consumer | Messages can be lost if the consumer crashes mid-processing | Commit after successful processing |
| Assuming duplicates are rare enough to ignore | They eventually happen in production | Design for them intentionally |

---

## 11. Key Numbers

These are system design heuristics.

- Retry attempts:
  often 3-5 before DLQ, depending on business criticality
- Visibility timeout:
  typically longer than expected processing duration
- Consumer processing timeout:
  should fit comfortably within visibility timeout or lease duration
- Duplicate window:
  can happen any time between side effect completion and ack/offset commit
- Exactly-once cost:
  usually increases latency and coordination overhead compared with at-least-once

Operational focus:
- at-most-once -> watch message loss
- at-least-once -> watch duplicates and retry volume
- exactly-once -> watch transaction failures, throughput impact, and boundary clarity

---

## 12. Failure Modes

### Consumer crashes after processing but before ack

Problem:
- The work succeeded, but the broker does not know that.

User impact:
- duplicate delivery on restart

Mitigation:
- idempotent consumers
- dedupe records

### Consumer acks before processing

Problem:
- The broker believes the work is done, then the consumer crashes.

User impact:
- silent message loss

Mitigation:
- ack only after successful processing

### Partial transaction

Problem:
- Consumer updates database but fails before committing offset or ack.

User impact:
- duplicate reprocessing or inconsistent side effects

Mitigation:
- transactional processing where supported
- idempotent writes

### Exactly-once illusion across external systems

Problem:
- Broker has strong semantics, but email provider or payment gateway does not participate.

User impact:
- duplicate emails or charges still possible

Mitigation:
- idempotency keys
- outbox/inbox patterns
- compensating logic

---

## 13. Scenario

- Product / system: Order processing platform
- At-most-once use case:
  low-value debug telemetry from services where some loss is acceptable
- At-least-once use case:
  `OrderCreated` events consumed by notification and inventory systems
- Exactly-once use case:
  a stream-processing stage that updates an internal ledger table and commits offsets transactionally
- Why this distinction matters:
  not every event needs the same correctness-cost trade-off

---

## 14. Code Sample

### At-most-once style consumer mistake

```java
public void consume(Record record) {
    consumer.commit(record.offset());
    process(record); // Crash here means the message is lost.
}
```

### At-least-once style consumer

```java
public void consume(Record record) {
    process(record);
    consumer.commit(record.offset()); // Crash before commit means duplicate delivery.
}
```

### Exactly-once style sketch

```java
public void consumeTransactionally(Record record) {
    transaction.begin();
    process(record);
    stateStore.save(record.key(), "PROCESSED");
    consumer.commitOffsetInTransaction(record.partition(), record.offset());
    transaction.commit();
}
```

Key idea:
- at-most-once commits early and risks loss
- at-least-once commits late and risks duplicates
- exactly-once coordinates business state and progress together

---

## 15. Mini Program / Simulation

This mini program shows the difference in failure behavior.

```python
def at_most_once(process_ok: bool) -> str:
    committed = True
    if not process_ok:
        return "message lost after early commit"
    return "processed once"


def at_least_once(process_ok: bool) -> str:
    committed = False
    if not process_ok:
        return "will be retried because commit never happened"
    committed = True
    return f"processed once, commit={committed}"


def main() -> None:
    print("At-most-once failure:", at_most_once(process_ok=False))
    print("At-least-once failure:", at_least_once(process_ok=False))
    print("At-least-once success:", at_least_once(process_ok=True))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- early commit creates loss risk
- late commit creates duplicate risk
- semantics are mostly about where failure is allowed to hurt you

---

## 16. Practical Question

> You are designing an event consumer for order processing. Duplicate events are acceptable if the business effect remains correct, but losing events is not acceptable. Which delivery guarantee would you choose and why?

---

## 17. Strong Answer

I would choose at-least-once delivery and make the consumer idempotent.

That is the most practical production choice because it protects against message loss, which is usually the bigger business risk, while accepting that duplicates can occur during crashes and retries. I would only acknowledge or commit after successful processing, and I would make downstream writes safe to repeat by using idempotency keys, dedupe tables, or upsert semantics.

I would be careful not to promise exactly-once unless I can define a narrow boundary where both state updates and progress tracking are committed transactionally. For most business systems, at-least-once plus idempotent consumers is the right balance between correctness and complexity.

---

## 18. Revision Notes

- One-line summary: At-most-once risks loss, at-least-once risks duplicates, and exactly-once requires stronger coordination within a defined boundary.
- Three keywords: ack, retry, boundary
- One interview trap: promising exactly-once everywhere
- One memory trick: early commit loses, late commit duplicates

---

# Topic 3: Idempotent Consumers

> Track: 1.7 Asynchronous & Event-Driven Systems
> Scope: duplicate-safe processing, dedupe keys, upserts, inbox tables, external side effects

---

## 1. Intuition

An idempotent consumer behaves like a light switch.

If the light is already ON, pressing ON again does not change the final state. You can repeat the action, but the outcome remains the same.

That is what we want from asynchronous consumers:
- if the same message is processed again, the business result should still be correct

Short memory trick:
- same input repeated -> same final effect

---

## 2. Definition

- Definition: An idempotent consumer is a message or event consumer that can process the same input multiple times without causing an incorrect duplicate business effect.
- Category: Reliability and correctness pattern
- Core idea: retries and duplicate deliveries are normal, so the consumer must make repeated processing safe.

---

## 3. Why It Exists

At-least-once delivery means duplicates are expected.

Even without broker duplicates, repeats can happen because of:
- producer retries
- consumer crashes before ack
- network failures
- replays from streams
- manual reprocessing

Without idempotent consumers:
- emails may be sent twice
- inventory may be reduced twice
- balances may be updated twice
- orders may be duplicated

---

## 4. Reality

Idempotent consumers are common in:

- payment processing
- order and inventory workflows
- event-driven microservices
- CDC pipelines
- replayable event streams
- webhook receivers

Common implementation styles:
- dedupe table keyed by message ID or event ID
- upsert instead of insert
- state transition guard
- unique constraint on business key
- inbox table pattern
- external idempotency key for third-party APIs

Real-world truth:
- if you use at-least-once delivery, you almost always need idempotent consumers

---

## 5. How It Works

### Dedupe-record approach

1. Message arrives with a unique ID.
2. Consumer checks whether that ID was already processed.
3. If yes, it exits safely.
4. If no, it performs business logic.
5. It records the message ID as processed.
6. Then it acknowledges the message.

### Upsert approach

1. Message carries a business key such as `order_id`.
2. Consumer writes using insert-or-update semantics.
3. Reprocessing the same message does not create another business record.

### State-transition guard approach

1. Consumer checks current state.
2. If the state already reflects the event, it skips.
3. If not, it applies the transition once.

Important rule:
- the dedupe decision and business write should be in the same transaction when possible

---

## 6. What Problem It Solves

- Primary problem solved: making duplicate deliveries safe
- Secondary benefits: safer retries, easier replay, more robust recovery after crashes
- Systems impact: changes data model design, database constraints, consumer transaction flow, and side-effect handling

---

## 7. When to Rely on It

Use idempotent consumers when:
- the system is at-least-once
- retries are enabled
- stream replay is possible
- manual reprocessing may happen
- the business effect is costly or dangerous to duplicate

Strong interviewer keywords:
- duplicate deliveries
- retries
- replay
- exactly-once is too expensive
- webhook receiver

---

## 8. When Not to Use It

Do not skip idempotency just because:
- duplicates seem rare
- the broker claims strong guarantees
- the workflow "usually works"

The real question is not whether to use idempotency at all, but how heavy the mechanism should be.

Lighter-weight cases:
- low-value metrics might accept duplication

Heavier-weight cases:
- payments, orders, credits, inventory reservations, and external side effects need strong protection

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Makes retries safe | Adds storage and logic overhead |
| Enables replay and reprocessing | Requires unique identifiers or stable business keys |
| Reduces business corruption from duplicates | Can be tricky around external side effects |
| Works well with at-least-once systems | Needs transactional thinking to avoid race conditions |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Simplicity vs correctness:
  dedupe logic adds complexity, but prevents business corruption.
- Storage vs safety:
  processed-message tables cost storage, but buy strong duplicate protection.
- Generic dedupe key vs business key:
  message ID is simple, but business-key idempotency can be more meaningful.
- Internal safety vs external side effects:
  your database may be safe, but third-party actions still need idempotency handling too.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| Checking processed IDs outside the business transaction | Two concurrent consumers can both pass the check | Keep dedupe and business write atomic |
| Relying only on in-memory dedupe | Restarts lose memory | Persist dedupe state |
| Using non-stable identifiers | Retries may get a different key and bypass dedupe | Use stable event/message/business IDs |
| Making the DB write idempotent but not the external API call | Duplicate emails or charges can still happen | Use idempotency keys with external systems too |
| Assuming replay should not happen in production | Streams and recovery often require replay | Design for replay from day one |

---

## 11. Key Numbers

These are design heuristics.

- Dedupe retention:
  often aligned with maximum replay or retry window
- Processed-message table size:
  grows with throughput x retention period
- Unique key design:
  should be stable across retries and replays
- External idempotency window:
  usually depends on provider support and business retry behavior

Operational focus:
- duplicate processing rate
- dedupe hit rate
- growth of processed-message state

---

## 12. Failure Modes

### Duplicate delivery after crash

Problem:
- Consumer completes work, crashes before ack, and sees the same message again.

User impact:
- duplicate business effect if the consumer is not idempotent

Mitigation:
- processed-message table
- unique constraints
- transactional write plus dedupe

### Race condition between duplicate consumers

Problem:
- Two consumers process the same logical event concurrently.

User impact:
- both may apply the state change if dedupe is not atomic

Mitigation:
- transactional dedupe
- unique constraints
- compare-and-set or state checks

### External side-effect duplication

Problem:
- Internal state is protected, but email or payment provider receives the request twice.

User impact:
- duplicate email or charge

Mitigation:
- provider idempotency key
- outbox pattern
- reconciliation

---

## 13. Scenario

- Product / system: Payment and order confirmation service
- Problem:
  A payment-authorized event may be delivered twice after a consumer restart.
- Good design:
  Store the event ID in a processed-events table inside the same transaction that marks the order as paid.
- Why it works:
  Reprocessing the same event will hit the dedupe record and skip the duplicate update.
- What would go wrong otherwise:
  the order may be marked paid twice, the customer may get two emails, or downstream records may duplicate

---

## 14. Code Sample

### Idempotent consumer with processed-events table

```java
@Transactional
public void consume(PaymentAuthorizedEvent event) {
    if (processedEventRepository.existsById(event.eventId())) {
        return;
    }

    orderRepository.markPaid(event.orderId(), event.paymentId());
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

### Upsert-style idempotent write

```sql
INSERT INTO inventory_reservations(order_id, sku, quantity)
VALUES (:order_id, :sku, :quantity)
ON CONFLICT (order_id, sku) DO NOTHING;
```

### External side-effect with idempotency key

```java
public void sendEmailOnce(Order order, String eventId) {
    emailClient.send(
            order.customerEmail(),
            "order-confirmation",
            eventId
    );
}
```

Key idea:
- dedupe key + atomic write is the heart of idempotency

---

## 15. Mini Program / Simulation

This mini program shows duplicate-safe processing with a processed-event set.

```python
processed_events = set()
paid_orders = set()


def consume_payment_event(event_id: str, order_id: str) -> None:
    if event_id in processed_events:
        print("Duplicate ignored:", event_id)
        return

    paid_orders.add(order_id)
    processed_events.add(event_id)
    print("Processed:", event_id, "for", order_id)


def main() -> None:
    consume_payment_event("evt-101", "ord-9")
    consume_payment_event("evt-101", "ord-9")
    print("Paid orders:", paid_orders)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- duplicate deliveries are harmless when the consumer tracks processed IDs
- idempotency is about final business correctness, not just avoiding code re-execution

---

## 16. Practical Question

> Your queue provides at-least-once delivery, and your `PaymentAuthorized` consumer sometimes sees duplicate messages after retries. How would you make the consumer safe?

---

## 17. Strong Answer

I would make the consumer idempotent because duplicate delivery is expected in an at-least-once system.

The cleanest design is to use a stable event ID and store it in a processed-events table inside the same transaction as the business update. When the event arrives, the consumer checks whether that event ID has already been applied. If yes, it skips safely. If not, it performs the state change and records the event as processed atomically.

If the workflow also triggers external side effects like email or payment operations, I would use idempotency keys with those external systems as well. The goal is not to prevent duplicate delivery at the broker layer, but to make duplicate processing harmless at the business layer.

---

## 18. Revision Notes

- One-line summary: Idempotent consumers make retries and duplicate deliveries safe by ensuring the same message cannot corrupt business state twice.
- Three keywords: dedupe, upsert, transaction
- One interview trap: checking dedupe outside the business transaction
- One memory trick: same event, same final state

---

# Topic 4: Dead-letter Queues

> Track: 1.7 Asynchronous & Event-Driven Systems
> Scope: poison messages, retry exhaustion, isolation, debugging, operational recovery

---

## 1. Intuition

A dead-letter queue is the quarantine room for bad messages.

If a message keeps failing and normal retries are not helping, we stop letting it block healthy traffic. Instead, we move it aside for inspection, debugging, and controlled recovery.

Short memory trick:
- normal queue = active work
- DLQ = failed work that needs investigation

---

## 2. Definition

- Definition: A dead-letter queue is a holding queue for messages that could not be processed successfully after a configured number of retries or failure conditions.
- Category: Failure isolation and operational recovery pattern
- Core idea: isolate poison messages so they do not loop forever or block healthy processing.

---

## 3. Why It Exists

Some failures are transient, and retries help.

Some failures are persistent, such as:
- malformed payloads
- bad schema version
- missing required state
- code bug
- permanently invalid business input

Without a DLQ:
- consumers retry the same poison message endlessly
- backlog grows
- good messages get delayed
- operators have no clean recovery path

---

## 4. Reality

DLQs are common in:

- queue-based worker systems
- order and payment pipelines
- stream-processing failure paths
- webhook delivery systems
- ETL ingestion pipelines

DLQs are especially useful when:
- retry exhaustion should not block the main flow
- operators need visibility into failed payloads
- bad messages must be replayed manually after a fix

Real-world truth:
- a DLQ is not a garbage bin
- it is an operational workflow

---

## 5. How It Works

1. A consumer receives a message.
2. Processing fails.
3. The system retries according to policy.
4. After the retry threshold or failure condition is reached, the message is moved to the DLQ.
5. Healthy traffic continues through the main queue or stream.
6. Operators inspect the DLQ, fix the cause, and either replay or permanently discard the message.

Typical reasons to send to DLQ:
- max retries exceeded
- validation failure
- deserialization error
- unsupported schema
- repeated timeout or downstream failure beyond policy

Important rule:
- not every failure should go to DLQ immediately
- transient failures should usually retry first

---

## 6. What Problem It Solves

- Primary problem solved: preventing poison messages from endlessly failing in the main processing path
- Secondary benefits: easier debugging, safer recovery, reduced backlog contamination
- Systems impact: changes retry policy, observability, on-call workflows, and replay tooling

---

## 7. When to Rely on It

Use a DLQ when:
- retries can exhaust
- some messages may be permanently invalid
- poison messages must not block healthy throughput
- operators need to inspect and recover failed work

Strong interviewer keywords:
- poison message
- retry exhaustion
- malformed payload
- manual replay
- operational recovery

---

## 8. When Not to Use It

Avoid treating DLQ as the first response when:
- the failure is clearly transient and should retry normally
- the failure rate is systemic and a DLQ will only hide a major outage

Avoid bad DLQ usage patterns:
- sending messages to DLQ without alerting
- never draining or reviewing the DLQ
- using DLQ as permanent storage

Better approach:
- combine retries, backoff, observability, and DLQ together

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Isolates poison messages from healthy traffic | Adds operational workflow and tooling needs |
| Prevents infinite retry loops | Can hide systemic failure if nobody monitors it |
| Makes debugging easier | Replay must be handled carefully to avoid repeat failure |
| Preserves failed payloads for investigation | Unbounded DLQ growth becomes its own problem |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Retry more vs fail fast:
  too many retries delay recovery; too few send transient failures to DLQ too early.
- Isolation vs hidden problems:
  DLQ protects throughput, but can hide large-scale breakage if alerts are weak.
- Preserve data vs replay risk:
  keeping failed messages is useful, but replaying blindly can repeat the same outage.

### Common Mistakes

| Mistake | Why it is wrong | Better approach |
|---|---|---|
| No DLQ for a critical queue | Poison messages can block the system indefinitely | Add retry policy and DLQ |
| Infinite retries with no cap | Wastes compute and starves healthy traffic | Use bounded retries then DLQ |
| DLQ without alerts and dashboards | Failures disappear silently | Monitor DLQ depth and age |
| Replaying DLQ messages blindly | The same poison message may fail again | Fix root cause first, then replay carefully |
| Using one shared DLQ for unrelated systems without metadata | Recovery becomes confusing | Include source queue, reason, and timestamps |

---

## 11. Key Numbers

These are practical heuristics.

- Max retry count:
  often 3-5 attempts before DLQ
- Backoff:
  exponential backoff with jitter is common
- DLQ monitoring:
  depth, rate of arrival, and age of oldest failed message are key signals
- Replay batch size:
  should be controlled to avoid another failure storm

Operational focus:
- how fast DLQ fills
- why messages are landing there
- whether replay succeeds after remediation

---

## 12. Failure Modes

### Poison message loops forever

Problem:
- A bad payload keeps failing and reappearing in the main queue.

User impact:
- backlog grows, healthy work slows

Mitigation:
- retry cap
- DLQ

### DLQ silently fills

Problem:
- Messages are being quarantined, but no alert fires.

User impact:
- lost business processing and unnoticed degradation

Mitigation:
- DLQ alarms
- dashboards
- on-call runbooks

### Bad replay after fix

Problem:
- Messages are replayed too aggressively or before the root cause is fixed.

User impact:
- repeated failures, another spike, duplicate side effects

Mitigation:
- replay in controlled batches
- validate fix first
- keep idempotency protections

### DLQ becomes permanent graveyard

Problem:
- nobody owns cleanup or replay

User impact:
- unresolved business failures accumulate

Mitigation:
- ownership, runbooks, retention policy, and periodic review

---

## 13. Scenario

- Product / system: Order email worker
- Problem:
  A malformed email payload causes one message to fail every time.
- Good design:
  Retry a few times for transient issues, then move the message to a DLQ with error reason and metadata.
- Operational recovery:
  Fix the payload bug or consumer code, then replay the quarantined message safely.
- What would go wrong otherwise:
  the worker may keep retrying forever and delay healthy confirmations

---

## 14. Code Sample

### Queue consumer with retry and DLQ

```java
public void consume(OrderEmailJob job) {
    try {
        emailService.send(job.orderId(), job.email());
        queueClient.ack(job.messageId());
    } catch (TransientException ex) {
        queueClient.retry(job.messageId());
    } catch (Exception ex) {
        deadLetterQueueClient.publish(job.messageId(), job, ex.getMessage());
        queueClient.ack(job.messageId());
    }
}
```

### DLQ record shape

```json
{
  "sourceQueue": "order-email-jobs",
  "messageId": "msg-101",
  "failedAt": "2026-04-07T09:30:00Z",
  "reason": "invalid_email_format",
  "payload": {
    "orderId": "ord-9",
    "email": "broken-address"
  }
}
```

### Replay sketch

```java
public void replay(DeadLetterRecord record) {
    mainQueueClient.publish(record.payload());
    deadLetterQueueClient.markReplayed(record.messageId());
}
```

Key idea:
- DLQ isolates failure, but replay still needs discipline and idempotency

---

## 15. Mini Program / Simulation

This mini program shows a simple retry-to-DLQ flow.

```python
main_queue = ["good-1", "bad-1", "good-2"]
dlq = []
attempts = {}


def process(message: str) -> bool:
    return not message.startswith("bad")


def consume_all(max_retries: int) -> None:
    for message in main_queue:
        attempts[message] = attempts.get(message, 0) + 1
        if process(message):
            print("Processed:", message)
        elif attempts[message] >= max_retries:
            dlq.append(message)
            print("Moved to DLQ:", message)
        else:
            print("Will retry:", message)


def main() -> None:
    consume_all(max_retries=2)
    consume_all(max_retries=2)
    print("DLQ contents:", dlq)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- transient retry happens first
- permanently failing work is isolated after the threshold
- DLQ protects the main flow from poison messages

---

## 16. Practical Question

> You run a queue for invoice-generation jobs. One malformed message keeps failing and causing worker churn. How would you design retries and dead-letter queue handling for this system?

---

## 17. Strong Answer

I would use bounded retries with exponential backoff and then move exhausted failures to a dead-letter queue.

The reason is that not every failure is transient. If one malformed or poison message keeps retrying forever, it wastes compute and delays healthy work. So I would retry a few times for transient issues like network failures, but after a configured threshold I would move the message to the DLQ along with source metadata, timestamps, and error reason.

I would also monitor DLQ depth and age, alert on abnormal growth, and provide a controlled replay workflow. Replay should only happen after the root cause is fixed, and consumers should still be idempotent so reprocessing remains safe.

---

## 18. Revision Notes

- One-line summary: A DLQ isolates messages that repeatedly fail so they stop harming the main processing flow and can be investigated safely.
- Three keywords: poison, quarantine, replay
- One interview trap: treating DLQ as a dump instead of an operational recovery mechanism
- One memory trick: retries are treatment, DLQ is quarantine
