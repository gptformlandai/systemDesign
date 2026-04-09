# Kafka Story Mode: Producers, Partitions, Consumer Groups, and Distributed Flow

> Goal: remove the confusion around `producer -> topic -> partitions -> consumer groups -> consumers` and reach senior-architect clarity.

---

## The One Mental Model

Kafka looks like "many-to-many chaos" from far away, but one message lives a very simple life:

1. A producer writes the message to exactly one topic partition.
2. The message gets an offset inside that partition.
3. Every consumer group can read that same message independently.
4. Inside one consumer group, only one consumer reads that partition at that moment.

That is the whole game.

Short version:
- Fan-out happens across consumer groups.
- Load balancing happens inside a consumer group.
- Ordering exists inside a partition, not across the whole topic.
- "Consumed" does not mean "deleted". Kafka keeps the message until retention expires.

---

# Topic 1: Kafka Producers, Partitions, Consumer Groups, and Distributed Behavior

> Track: Kafka
> Scope: producer routing, partitioning, consumer groups, replicas, ordering, idempotency, hot partitions, and distributed-system behavior

---

## 1. Intuition

Think of Kafka as a huge distributed notebook.

- A topic is one notebook.
- Partitions are separate pages or page ranges in that notebook.
- Producers write new lines into the notebook.
- Consumer groups are different teams reading the same notebook for different reasons.
- Consumers inside one team divide pages among themselves so they do not duplicate work.

Beginner explanation in 3 lines:

Kafka is a distributed event log. Producers append messages to partitions, and consumers read them by offset. Different consumer groups can read the same messages independently, while consumers inside the same group share the partitions.

---

## 2. Definition

- Definition: Kafka is a distributed, replicated, append-only event streaming platform where topics are split into partitions for scale and ordering.
- Category: Distributed log / event streaming system
- Core idea: write once to a partitioned durable log, then let multiple independent consumer groups read that log at their own pace.

---

## 3. Why It Exists

Kafka exists because direct service-to-service communication breaks down when scale, spikes, retries, and independent downstream consumers appear.

Without Kafka:
- producers and consumers become tightly coupled
- slow consumers slow down producers
- replay is difficult
- fan-out to many downstream systems becomes messy
- failures create duplication, missing events, or both

Kafka solves this by giving us:
- durable storage
- horizontal scale through partitions
- replay through offsets
- consumer independence through consumer groups
- fault tolerance through replication

---

## 4. Reality

Kafka is used in systems such as:
- order pipelines
- payment event flows
- clickstream analytics
- log aggregation
- CDC pipelines
- recommendation systems
- fraud detection

Teams that use Kafka often:
- microservices teams
- platform teams
- data engineering teams
- real-time analytics teams
- marketplace and fintech teams

---

## 5. How It Works

### Part A: Story Mode Walkthrough

Let us build one concrete example and follow one message.

System:
- Topic: `orders`
- Partitions: `P0`, `P1`, `P2`
- Producer service: `order-service`
- Consumer groups:
  - `payments`
  - `inventory`
  - `analytics`

Inside each consumer group, partitions are assigned like this:

| Consumer Group | Consumers | Partition Ownership |
|---|---|---|
| `payments` | `pay-c1`, `pay-c2` | `P0 -> pay-c1`, `P1 -> pay-c2`, `P2 -> pay-c1` |
| `inventory` | `inv-c1`, `inv-c2`, `inv-c3` | `P0 -> inv-c1`, `P1 -> inv-c2`, `P2 -> inv-c3` |
| `analytics` | `ana-c1` | `P0,P1,P2 -> ana-c1` |

Now a user places an order:

```text
OrderPlaced(orderId=9182, customerId=42, amount=250)
```

Assume the producer uses `customerId` as the message key.

Kafka computes:

```text
partition = hash(customerId) % 3
```

Assume `hash(42) % 3 = 1`.

So this message goes to:

```text
topic = orders
partition = P1
offset = say 400
```

Important point:
- The message is stored once in `orders-P1` at offset `400`.

Now who reads it?

- In `payments`, `pay-c2` reads it because `pay-c2` owns `P1`.
- In `inventory`, `inv-c2` reads it because `inv-c2` owns `P1`.
- In `analytics`, `ana-c1` reads it because `ana-c1` owns all partitions.

Who does not read it?

- `pay-c1` does not.
- `inv-c1` does not.
- `inv-c3` does not.

This is the confusion killer:

- The producer does not send to every consumer.
- The producer sends to one partition.
- Each consumer group independently decides which consumer owns that partition.
- So one message is processed by one consumer per group, not by every consumer everywhere.

### Part B: Why It Feels Like Many-to-Many

At the system level, yes, Kafka looks many-to-many:

- many producers can write to the same topic
- one topic has many partitions
- many consumer groups can subscribe to the same topic
- each group can have many consumers

But at the message level, it is deterministic:

```text
One message
-> one topic
-> one partition
-> one offset
-> one consumer inside each consumer group
```

That is why Kafka scales without turning into randomness.

### Part C: The Most Important Rule About Consumer Groups

Inside a single consumer group:
- one partition is assigned to only one consumer at a time
- one consumer may own multiple partitions
- max parallelism of a group is the number of partitions

Examples:

If topic has `3` partitions and group has `5` consumers:
- only `3` consumers can be active on that topic
- `2` consumers stay idle

If topic has `10` partitions and group has `3` consumers:
- consumers will split those `10` partitions
- some consumers will own multiple partitions

### Part D: Kafka Is a Log, Not a Traditional Queue

This is another major confusion point.

In a queue mindset:
- one consumer takes the message
- the message disappears

In Kafka:
- consumer reads by offset
- offset moves forward
- message remains in Kafka until retention expires

So "consume" really means:
- "I have processed up to offset X in this partition"

It does not mean:
- "Kafka deleted the message after I read it"

That is why replay is possible.

---

## 6. What Problem It Solves

- Primary problem solved: scalable, durable, ordered event distribution with independent consumers
- Secondary benefits: buffering, replay, decoupling, failure isolation, async processing
- Systems impact: separates write path from processing path and lets different downstream services move at different speeds

---

## 7. When to Rely on It

Kafka is a strong fit when:
- many downstream systems need the same event
- you need replay
- ordering matters for a key like `userId`, `orderId`, or `accountId`
- write throughput is high
- consumers may be slow or temporarily down
- you want durable event history

Interviewer keywords that should trigger Kafka thinking:
- event-driven architecture
- CDC
- activity stream
- order pipeline
- audit log
- asynchronous workflow
- decoupling
- fan-out

---

## 8. When Not to Use It

Avoid Kafka when:
- the system is very small and simple
- a normal database plus background jobs is enough
- you need synchronous request-response only
- you need ad hoc random queries over state rather than sequential event reads
- the team cannot absorb Kafka operational complexity

Alternatives:
- direct API calls
- database polling
- RabbitMQ or SQS for simpler work queues
- cron plus database tables for small internal workflows

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| High throughput and horizontal scale | Operational complexity |
| Replayable event history | Partition-key mistakes can hurt badly |
| Independent consumer groups | Ordering is only per partition |
| Durable replication | Rebalances and duplicates must be handled |
| Strong fit for event-driven systems | Hot partitions can limit throughput |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- More partitions:
  More parallelism and throughput, but more metadata, more file handles, more network chatter, and more rebalance cost.
- Strong ordering:
  If you key by `customerId`, all events for one customer go to one partition, but you may create skew if one customer is extremely hot.
- Higher durability:
  `acks=all` and replication improve safety, but increase latency compared to weaker acknowledgments.
- More consumer replicas:
  Good only until you hit partition count. After that, extra consumers do not increase throughput for that topic in that group.
- Long retention:
  Great for replay and audit, but increases storage cost.

### Common Mistakes

- Mistake: "Every consumer in a group gets every message."
  Why it is wrong: inside a group, partitions are shared, not duplicated.
  Better approach: say "every group gets the message; inside the group, one consumer handles that partition."

- Mistake: "Consumed means deleted."
  Why it is wrong: Kafka is a retained log.
  Better approach: think in terms of offsets and retention.

- Mistake: "Kafka gives exactly-once everywhere automatically."
  Why it is wrong: duplicates can still appear across retries, rebalances, and external side effects.
  Better approach: use idempotent producers, careful offset management, and idempotent consumers or transactional sinks.

- Mistake: "More consumers always means more throughput."
  Why it is wrong: throughput inside a group is capped by partition count.
  Better approach: scale partitions and consumers together, with care.

- Mistake: "Any key is fine."
  Why it is wrong: low-cardinality or skewed keys create hot partitions.
  Better approach: choose a stable, high-cardinality key that matches ordering needs.

---

## 11. Key Numbers

Use these as interview-ready approximations:

- Typical replication factor: `3`
- Common `min.insync.replicas`: `2` when RF is `3`
- Max active consumers in one group for one topic: roughly the number of partitions
- Typical partition count: from a handful to hundreds, depending on throughput and parallelism
- Retention: hours to weeks, sometimes longer
- Produce latency:
  often low single-digit to tens of milliseconds depending on `acks`, batching, hardware, and network
- Failure tolerance example:
  with RF=`3`, `min.insync.replicas=2`, and `acks=all`, the cluster can usually keep safe writes during one replica failure for that partition

Note:
- exact numbers depend heavily on hardware, network, message size, compression, and broker tuning

---

## 12. Failure Modes

### Producer Retry Duplication

What fails:
- producer sends a record
- broker writes it
- ack is lost or timeout happens
- producer retries

What user observes:
- duplicate event may appear

Mitigation:
- enable idempotent producer
- design consumers to be idempotent

### Consumer Crash After Side Effect But Before Offset Commit

What fails:
- consumer updates database
- consumer crashes before committing offset

What user observes:
- same message may be processed again after restart

Mitigation:
- commit offset after successful processing
- use idempotent writes or dedupe keys

### Broker Failure

What fails:
- broker that leads a partition goes down

What user observes:
- short pause, then leadership moves to an in-sync replica

Mitigation:
- replication factor > 1
- `acks=all`
- healthy ISR

### Consumer Group Rebalance

What fails:
- a consumer joins, leaves, or misses heartbeats

What user observes:
- temporary pause in processing
- partition ownership changes

Mitigation:
- cooperative rebalancing when possible
- stable consumer membership
- fast processing and regular heartbeats

### Hot Partition

What fails:
- one key receives disproportionate traffic

What user observes:
- one partition lags badly while others stay underused

Mitigation:
- choose better partition keys
- split the hot entity if business semantics allow
- separate ultra-hot traffic into another topic or key strategy

---

## 13. Scenario

- Product / system: E-commerce order platform
- Why this concept fits: one order event should trigger payment, inventory, analytics, notifications, and audit independently
- What would go wrong without it: services become tightly coupled, retries become messy, replay becomes painful, and spikes can overload downstream systems

---

## 14. Code Sample

This small example shows the architect mindset:
- use a key to control ordering and partitioning
- use idempotent producer settings for safer retries

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class OrderProducerExample {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "broker1:9092,broker2:9092,broker3:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // Safer producer settings
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String customerId = "42";
            String payload = "{\"orderId\":9182,\"customerId\":42,\"event\":\"OrderPlaced\"}";

            // Same key -> same partition -> per-customer ordering
            ProducerRecord<String, String> record =
                    new ProducerRecord<>("orders", customerId, payload);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf(
                            "topic=%s partition=%d offset=%d%n",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset()
                    );
                } else {
                    exception.printStackTrace();
                }
            });
        }
    }
}
```

---

## 15. Mini Program / Simulation

This runnable Python example shows exactly which consumer gets a message inside each group.

```python
import hashlib


def partition_for(key: str, partition_count: int) -> int:
    digest = hashlib.sha256(key.encode()).hexdigest()
    return int(digest, 16) % partition_count


def main():
    partition_count = 3
    key = "42"  # customerId
    partition = partition_for(key, partition_count)

    group_assignments = {
        "payments": {
            0: "pay-c1",
            1: "pay-c2",
            2: "pay-c1",
        },
        "inventory": {
            0: "inv-c1",
            1: "inv-c2",
            2: "inv-c3",
        },
        "analytics": {
            0: "ana-c1",
            1: "ana-c1",
            2: "ana-c1",
        },
    }

    print(f"Message key={key} goes to partition P{partition}")
    print("One physical message is stored once, but each consumer group can read it.")
    print()

    for group, owners in group_assignments.items():
        consumer = owners[partition]
        print(f"Group '{group}' -> consumer '{consumer}' reads the message from P{partition}")


if __name__ == "__main__":
    main()
```

Expected learning:
- one message lands in one partition
- each group sees the message independently
- one consumer per group owns that partition at that moment

---

## 16. Practical Question

> You are designing an order platform where many replicas of `order-service` publish events and many replicas of `payment-service`, `inventory-service`, and `analytics-service` consume them. How would you use Kafka, and what trade-offs would you consider around partitioning, ordering, idempotency, and hot partitions?

---

## 17. Strong Answer

1. I would use Kafka because the same order event must fan out to multiple independent downstream services, and Kafka gives durable storage, replay, and consumer-group isolation.
2. I would create a topic such as `orders` and partition it based on a business key that matches the ordering need. If I need all events of a customer in order, I would key by `customerId`. If I need order-level ordering only, I would key by `orderId`.
3. Producer replicas are just multiple Kafka clients. Any replica can publish, but the partitioner uses the same key logic, so the same key consistently lands on the same partition leader.
4. Each downstream service gets its own consumer group. That means `payments`, `inventory`, and `analytics` all read the same topic independently. Inside each group, a partition is assigned to one consumer at a time, so processing scales with partition count.
5. I would enable producer idempotency and use `acks=all` for safer writes. On the consumer side, I would make processing idempotent because duplicates can still happen during retries or rebalances.
6. I would watch for hot partitions. If one key becomes too popular, one partition becomes the bottleneck. So partition-key choice is an architectural decision, not a small implementation detail.
7. I would remember that increasing partitions later improves future parallelism, but it can remap keys to different partitions and affect ordering assumptions across old and new data.

---

## 18. Distributed Environment: The God-Level Clarity

Now let us answer the harder question:

> "What happens when Kafka itself is distributed, and my producer service also has many replicas?"

### The Setup

Assume:
- Kafka brokers: `B1`, `B2`, `B3`
- Topic: `orders`
- Partitions: `P0`, `P1`, `P2`
- Replication factor: `3`

Partition leadership may look like this:

| Partition | Leader | Followers |
|---|---|---|
| `P0` | `B1` | `B2`, `B3` |
| `P1` | `B2` | `B3`, `B1` |
| `P2` | `B3` | `B1`, `B2` |

Producer service replicas:
- `order-service-pod-1`
- `order-service-pod-2`
- `order-service-pod-3`
- `order-service-pod-4`

Consumer service replicas:
- `payments` group: 2 pods
- `inventory` group: 3 pods
- `analytics` group: 1 pod

### The Write Path

When any producer replica wants to publish:

1. It connects to Kafka using bootstrap servers.
2. It fetches metadata and learns which broker is the leader for each partition.
3. It chooses a partition using the key and partitioner.
4. It sends the message to the leader broker for that partition.
5. The leader appends the record to its local log.
6. Followers replicate that record.
7. Once the required acknowledgment condition is met, the producer gets success.

Example:

```text
Producer pod-3 creates OrderPlaced(customerId=42)
-> key = 42
-> hash(42) % 3 = 1
-> target partition = P1
-> leader for P1 = B2
-> B2 appends the record at offset 400
-> B1 and B3 replicate it
-> producer receives ack
```

Important insight:
- many producer replicas do not create chaos
- they all obey the same partitioning and leader-routing rules

So if two different producer pods emit events for `customerId=42`, both go to the same partition, preserving per-key order as long as the producer behavior is correct.

Senior nuance:
- Kafka preserves the order in which records are appended to that partition.
- If multiple producer replicas emit the same key at nearly the same time, Kafka does not know your business-time intent.
- It only knows arrival and append order.
- If strict causal ordering matters, upstream serialization, version checks, or sequence numbers may still be needed.

### The Read Path

Now consumers read:

1. Each consumer joins a consumer group.
2. Kafka assigns partitions in that group.
3. Consumers fetch records from their assigned partitions.
4. Consumers track progress using offsets.
5. Offsets are committed so the group can resume later, commonly into Kafka's internal `__consumer_offsets` topic.

Important insight:
- consumer group state is independent per group
- `payments` may be at offset 401 on `P1`
- `inventory` may still be at offset 398 on `P1`
- both are correct

Kafka does not force all groups to move together.

### What Replication Really Means

Replication is for durability and availability, not for multiplying consumer reads.

If `P1` has leader `B2` and followers `B1`, `B3`:
- producer writes to leader `B2`
- followers copy the same log
- if `B2` fails, a follower can become the new leader

Replication answers:
- "Can my data survive broker failure?"

Consumer groups answer:
- "How do multiple applications read the same data independently?"

These are different concerns.

### If a Broker Fails

Suppose `B2` fails and `P1` leader was on `B2`.

Then:
1. Kafka elects a new leader from in-sync replicas, say `B1`.
2. Producers refresh metadata.
3. New writes for `P1` go to `B1`.
4. Consumers start fetching `P1` from the new leader.

User-visible impact:
- short disruption
- then recovery

If replication and ISR are healthy, data remains available.

### If a Consumer Dies

Suppose `inv-c2` dies while owning `P1`.

Then:
1. Kafka notices heartbeats stopped.
2. A rebalance happens.
3. Another consumer in `inventory` takes ownership of `P1`.
4. Processing resumes from the last committed offset.

This is why offset management matters so much.

---

## 19. Senior Architect Lens

This is the vocabulary and mental model expected at senior level.

### Partition Key / Sharding Key

Kafka topic partitions are essentially shards.

Your message key is your sharding key.

That key decides:
- which partition receives the event
- what ordering you preserve
- where hotspots form
- how evenly traffic spreads

Good keys:
- stable
- high cardinality
- aligned with business ordering needs

Examples:
- `userId`
- `accountId`
- `orderId`

Bad keys:
- `country`
- `status`
- `eventType`
- `null` for workloads that need order per entity

### Hashing

Kafka typically maps key to partition through hashing:

```text
partition = hash(key) % number_of_partitions
```

Meaning:
- same key usually maps to same partition
- different keys spread across partitions

This is why key choice is so important.

### Hot Partitions

A hot partition happens when one partition gets much more traffic than others.

Common causes:
- low-cardinality keys
- one extremely active customer or account
- skewed event distribution

Impact:
- one partition becomes the throughput bottleneck
- one consumer in a group becomes overloaded
- lag grows only on that partition

Mitigation:
- choose a better key
- split heavy entities if semantics allow
- route ultra-hot tenants separately
- isolate workloads into separate topics

### Idempotency

In distributed systems, duplicates are normal enough that you should expect them.

Why duplicates happen:
- producer retry after timeout
- network ambiguity
- consumer crash after processing but before offset commit
- rebalance during in-flight processing

Producer idempotency:
- protects against duplicate writes caused by producer retries in many normal cases
- should usually be enabled for important streams

Consumer idempotency:
- still needed
- because external side effects like database updates, emails, and payments can be repeated if your app is not careful

Common patterns:
- dedupe by `eventId`
- upsert by business key
- use transactional tables
- store processed event IDs for exactly-once effect at the application boundary

### Ordering

Kafka guarantees order within a partition, not across all partitions of a topic.

So if you need:
- per-customer order: key by `customerId`
- per-order order: key by `orderId`

But also remember:
- Kafka preserves broker append order, not necessarily the order in which two different app servers believed events happened
- with multiple producers for the same key, sequence numbers or versioning may still be needed for strict business correctness

But if you need global total order for the whole topic:
- that is much harder
- often requires one partition
- which hurts throughput and parallelism

Architect trade-off:
- stronger ordering usually means less parallelism

### Rebalancing

Consumer groups rebalance when:
- consumers join
- consumers leave
- heartbeats are missed
- topic partition counts change

Impact:
- short pause
- partition ownership changes
- duplicate processing risk if offsets were not managed well

Senior-level takeaway:
- rebalances are normal operational events, not edge cases

### Partition Count Is an Architectural Decision

Partition count controls:
- max parallelism per consumer group
- write spread
- storage layout
- rebalance complexity
- future scaling options

But partition growth is not free.

Important warning:
- if you increase partition count later, keys may map to different partitions for future events
- that can break assumptions about long-lived per-key ordering across historical and future data

---

## 20. Same Key Across Groups vs Same Group

This is one of the most important clarifications.

Assume:
- topic = `orders`
- key = `abc`
- `abc` hashes to partition `P1`

Kafka stores the record once:

```text
orders-P1, offset=401, key=abc
```

### Case A: Same Key Seen by Different Consumer Groups

Suppose two groups subscribe to `orders`:
- `payments`
- `analytics`

Then:
- `payments` reads `abc` using its own group offset
- `analytics` also reads `abc` using its own group offset

Both groups treat it as a fresh event for their own business purpose.

That is not an error.
That is the design.

Why?
- consumer groups are independent subscribers
- each group has its own offsets
- each group has its own database, logic, retries, and side effects

So the same Kafka record can legitimately become:
- a payment workflow in `payments`
- a dashboard update in `analytics`
- a notification in `notifications`

Same physical message.
Different business meaning in each group.

### Case B: Same Key Inside the Same Consumer Group

Now suppose group `payments` has:
- `pay-c1`
- `pay-c2`

And `P1` is assigned to `pay-c2`.

Then all records for key `abc` that map to `P1` are handled by `pay-c2`, not randomly by both consumers.

Important rule:
- within one group, one partition is owned by one consumer at a time

So under normal operation:
- `abc` is not processed by both `pay-c1` and `pay-c2`
- `pay-c2` keeps reading `P1` in order

When can another consumer in the same group handle it?

Only during ownership change, such as:
- `pay-c2` crashes
- a rebalance happens
- partition ownership moves to `pay-c1`

Then `pay-c1` continues from the last committed offset of the `payments` group.

This is continuation, not independent fan-out.

One more important nuance:
- if `pay-c2` processed the message but crashed before offset commit, `pay-c1` may reprocess the same message after rebalance
- that is at-least-once behavior
- that is why idempotent consumers matter

Short memory trick:
- same message across different groups = expected
- same message inside same group = only on retry/rebalance, not normal fan-out

---

## 21. What a Consumer Actually Does After Reading

Kafka itself does not decide the business action.
Your application code does.

After a consumer reads a message, it usually does one or more of these:
- write to a database
- update a cache
- call another service
- call an external API
- trigger an email or notification
- publish another Kafka event
- start a workflow
- run fraud checks
- update analytics counters

The common flow is:

1. Poll records from Kafka.
2. Deserialize the message.
3. Validate schema and business fields.
4. Run business logic.
5. Perform side effect such as DB write or external call.
6. Publish follow-up events if needed.
7. Commit the offset after successful processing.

### Real-World Example: `OrderPlaced`

Suppose Kafka has:

```text
OrderPlaced(orderId=9182, customerId=42, amount=250)
```

Different consumer groups may do completely different work:

`payments` group:
- insert payment row in `payments_db`
- call payment gateway
- publish `PaymentAuthorized` or `PaymentFailed`

`inventory` group:
- reserve stock in `inventory_db`
- publish `InventoryReserved` or `InventoryFailed`

`notifications` group:
- store notification request
- send email, SMS, or push notification

`analytics` group:
- increment counters
- push event to warehouse or OLAP system

So when we say "consumer consumes the message", we really mean:
- it reads the event
- turns that event into some business action
- then marks its progress with offset commit

---

## 22. Simple Pseudocode: Producer and Consumer

### Producer Side

In real systems, producers are usually triggered by business actions:
- API request comes in
- DB change happens
- another event is handled

Simple producer pseudocode:

```text
onOrderCreated(order):
    key = order.customerId
    event = {
        "eventType": "OrderPlaced",
        "orderId": order.id,
        "customerId": order.customerId,
        "amount": order.amount
    }

    producer.send(topic="orders", key=key, value=event)
```

More realistic mental model:

```text
App thread:
    producer.send(...)

Producer internals:
    partition = hash(key) % partitionCount
    put record into in-memory batch for that partition

Background sender thread:
    send batch to leader broker for that partition
    wait for ack based on acks setting
```

Important point:
- producer code in your app may look simple
- the Kafka producer client does batching, retries, compression, and network I/O behind the scenes

### Consumer Side: Simplest Safe Model

This is the common beginner-friendly model:

```text
consumer.subscribe(["orders"])

while running:
    records = consumer.poll(timeout=100ms)

    for record in records:
        handle(record)

    consumer.commit()
```

And `handle(record)` may look like:

```text
handle(record):
    event = deserialize(record.value)

    if event.type == "OrderPlaced":
        save_to_db(event)
        call_downstream_service_if_needed(event)
        maybe_publish_followup_event(event)
```

This means:
- consumer keeps polling in a loop
- records are usually processed one by one in code
- offset is committed after successful work

So yes, a loop is absolutely normal on consumer side.

Producer side is different:
- producer does not usually run a "read Kafka forever" loop
- it usually reacts to application events and calls `send()`
- but internally the client batches and flushes in the background

---

## 23. Multithreading Mental Model

This is where most confusion comes from, so let us separate producer and consumer.

### Producer Multithreading

In common Kafka clients such as Java:
- `KafkaProducer` is thread-safe
- many application threads can share one producer instance

Mental model:

```text
request-thread-1 -> producer.send(...)
request-thread-2 -> producer.send(...)
request-thread-3 -> producer.send(...)

shared producer client
    -> buffers records
    -> batches by partition
    -> background sender thread pushes to brokers
```

So multi-threading on producer side is usually easier.

### Consumer Multithreading

In common Kafka clients such as Java:
- `KafkaConsumer` is not thread-safe
- one consumer instance is usually driven by one poll thread

Safe default model:
- one consumer instance
- one thread calling `poll()`
- process assigned partitions in that thread

That one consumer may still read multiple partitions.

Example:
- consumer `pay-c2` owns `P1` and `P2`
- same thread polls records from both partitions
- records within `P1` remain ordered
- records within `P2` remain ordered

### Why a Naive Thread Pool Can Break Things

Suppose you do this:

```text
records = consumer.poll()

for record in records:
    threadPool.submit(handle(record))

consumer.commit()
```

This is dangerous because:
- records from the same partition may finish out of order
- you may commit offsets before processing really finishes
- crash after commit can lose work
- crash before commit can duplicate work

### Safer Concurrency Patterns

Pattern 1: Scale with more consumers and more partitions
- simplest approach
- most common approach

Pattern 2: One poll thread, partition-aware workers
- poll thread reads records
- it sends `P1` records to worker queue `P1`
- it sends `P2` records to worker queue `P2`
- ordering is preserved inside each partition queue
- offsets are committed only after that partition's earlier records finish

Pseudo pattern:

```text
while running:
    records = consumer.poll()
    grouped = groupByPartition(records)

    for partition in grouped:
        partitionQueue[partition].submit(grouped[partition])

    completedOffsets = findPartitionsWhoseWorkFinishedInOrder()
    consumer.commit(completedOffsets)
```

Pattern 3: Pause/resume partitions
- if one partition has too much in-flight work, pause fetching from it
- continue reading other partitions
- resume when safe

### The Cleanest Interview Answer

If asked about multithreading, say this:

1. Producer side is usually easier because the producer client is built for async batching and can often be shared across threads.
2. Consumer side is trickier because ordering and offset safety matter.
3. The simplest safe pattern is one consumer instance per thread with a poll loop.
4. If I need more throughput, I first increase partitions and consumer instances.
5. If I use worker threads behind a consumer, I keep work partition-aware and commit offsets only after processing is safely complete.

### Final Confusion Killer

Think in lanes:

- topic = highway
- partitions = lanes
- consumer group = one company of drivers
- consumer = one driver

Rules:
- one car enters one lane
- one lane belongs to one driver in a group at a time
- another company of drivers can watch the same traffic independently
- if a driver changes, the new driver continues from the last known checkpoint

---

## 24. Revision Notes

- One-line summary: Kafka fans out across consumer groups and load-balances within a consumer group.
- Three keywords: partition key, offsets, consumer group
- One interview trap: saying every consumer in a group gets the same message
- One memory trick: one message, one partition, one consumer per group
