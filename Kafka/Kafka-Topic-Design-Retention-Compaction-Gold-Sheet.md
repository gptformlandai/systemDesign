# Kafka Topic Design, Retention, and Compaction Gold Sheet

> Goal: design Kafka topics that scale, preserve the right ordering, control storage cost, and support replay or state restoration safely.

---

## 0. How To Read This

Beginner focus:

- topic
- partition
- key
- retention
- offset

Intermediate focus:

- partition count
- replication factor
- retention by time/size
- compaction
- tombstone
- hot partition

Senior focus:

- partition sizing
- key migration
- storage growth
- compacted state topics
- replay strategy
- multi-tenant topic governance

---

# Topic 1: Kafka Topic Design, Retention, and Compaction

---

## 1. Intuition

A Kafka topic is like a road system.

- Partitions are lanes.
- Keys decide which lane a car enters.
- Retention decides how long cars stay recorded on camera.
- Compaction keeps the latest known state per key.

If you choose too few lanes, traffic jams. If you choose a bad lane rule, one lane melts while others are empty. If you keep video forever without planning storage, disks fill.

Beginner explanation:

Topic design decides how events are grouped, ordered, retained, and scaled. The partition key is the most important early decision because it controls ordering and load distribution.

---

## 2. Definition

- Definition: Kafka topic design is the process of choosing topic boundaries, partition count, keys, replication, retention, and cleanup policy for an event stream.
- Category: Event platform architecture
- Core idea: shape the log around business ordering, throughput, replay, and storage requirements.

---

## 3. Why It Exists

Bad topic design causes:

- hot partitions
- poor ordering
- consumer lag
- expensive storage
- painful replay
- broken consumers after partition changes
- unclear ownership
- too many tiny topics or one overloaded mega-topic

Good topic design makes Kafka predictable.

---

## 4. Reality

Topic design appears in:

- order event streams
- payment ledgers
- user activity tracking
- database CDC
- inventory state updates
- notification pipelines
- analytics ingestion
- ML feature logs

At senior level, the interviewer expects you to defend:

- why this topic exists
- why this key is chosen
- why this partition count is enough
- how long data is retained
- whether compaction or deletion is correct
- how consumers recover after failure

---

## 5. How It Works

### Part A: Topic Boundary

Choose a topic around an event stream with shared meaning.

Good examples:

```text
order-events
payment-events
inventory-events
user-activity-events
customer-profile-updates
```

Weak examples:

```text
events
data
service-topic
temp-topic
everything-prod
```

Topic boundary questions:

- Who owns this topic?
- What business fact does it represent?
- Who consumes it?
- Is ordering needed between event types?
- Does it need replay?
- Does it need compaction?

### Part B: Partition Key

The key controls:

- partition routing
- per-key ordering
- compaction identity
- load distribution

Example:

```text
topic: order-events
key: orderId
```

Benefits:

- all events for one order go to same partition
- consumers see per-order order
- compacted topic can keep latest state per order if needed

Bad keys:

| Bad Key | Why It Hurts |
|---|---|
| `country` | low cardinality, hot partitions |
| `status` | few values, skew |
| `eventType` | all same events pile together |
| random UUID for related events | destroys business ordering |
| null key for ordered domain events | no per-entity ordering guarantee |

### Part C: Partition Count

Partition count controls maximum parallelism per consumer group.

```text
topic has 12 partitions
consumer group can actively use up to 12 consumers for that topic
```

Sizing inputs:

- target write throughput
- target read throughput
- average message size
- consumer processing time
- ordering requirements
- future growth
- broker count
- operational overhead

Rule:

Do not choose partition count only from today's traffic. Choose for the next growth window because changing partitions later can alter key-to-partition mapping.

### Part D: Replication Factor

Replication factor controls how many copies of each partition exist.

Common critical topic setup:

```text
replication.factor = 3
min.insync.replicas = 2
producer.acks = all
```

This balances durability and availability for many business systems.

### Part E: Retention

Retention decides how long Kafka keeps records.

Common policies:

```text
retention.ms
retention.bytes
```

Use delete retention when:

- topic is an event history
- old events can expire after replay window
- storage cost matters

Examples:

- logs retained for 7 days
- clickstream retained for 3 days
- payment events retained for 90 days or archived longer

### Part F: Compaction

Compaction keeps at least the latest value per key.

Example records:

```text
key=user-7 value=email=a@example.com
key=user-7 value=email=b@example.com
key=user-7 value=email=c@example.com
```

After compaction, Kafka can remove older values and keep the latest state:

```text
key=user-7 value=email=c@example.com
```

Use compaction for:

- latest customer profile
- latest account status
- table changelog
- Kafka Streams state-store changelog
- CDC state topic

Do not use compaction when:

- every historical event matters
- audit requires full event history
- downstream analytics needs all changes

### Part G: Tombstones

A tombstone is:

```text
key = some-id
value = null
```

In a compacted topic, tombstone means:

```text
delete the latest state for this key eventually
```

Important:

- tombstones are not immediately removed
- compaction is background work
- consumers may see tombstones
- consumers must handle null values correctly

### Part H: `delete` vs `compact` vs Both

| Cleanup Policy | Meaning | Use Case |
|---|---|---|
| `delete` | remove old records by time/size | event history with replay window |
| `compact` | keep latest value per key | state snapshot/changelog |
| `compact,delete` | compact by key plus age/size cleanup | bounded latest-state topics |

### Part I: Increasing Partition Count

Increasing partitions can be useful for throughput, but it can break key mapping.

Before:

```text
partition = hash(key) % 6
```

After:

```text
partition = hash(key) % 12
```

Many keys may move to different partitions for future records.

Risk:

- old events for key are in one partition
- new events for key may go to another partition
- ordering across old/new boundary can be confusing for consumers

Mitigation:

- choose enough partitions early
- use custom partitioner if absolutely needed
- migrate with a new topic if strict ordering matters

---

## 6. What Problem It Solves

- Primary problem solved: scalable, ordered, cost-aware event storage
- Secondary benefits: replay control, state restoration, consumer parallelism, ownership clarity
- Systems impact: prevents hot spots, disk surprises, and long-term contract confusion

---

## 7. When To Use Each Topic Pattern

Use event-history topic when:

- every change matters
- audit/replay matters
- consumers need the full event stream

Use compacted state topic when:

- only latest value per key matters
- consumers rebuild current state
- topic acts like a distributed table/changelog

Use separate topics when:

- events have different owners
- retention differs significantly
- schema differs significantly
- consumer groups are unrelated

Use shared topic when:

- events belong to same aggregate
- ordering across event types matters
- consumers usually process the lifecycle together

---

## 8. When Not To Do It

Avoid too many topics when:

- each topic has tiny traffic and no clear ownership
- operations become noisy
- governance is weak

Avoid one giant topic when:

- schemas are unrelated
- retention differs by event type
- security/ACLs differ
- consumers waste work filtering

Avoid compaction when:

- legal/audit requires every event
- the latest state is not enough
- consumers need intermediate transitions

---

## 9. Pros and Cons

| Decision | Pros | Cons |
|---|---|---|
| More partitions | more parallelism | more metadata, files, rebalance cost |
| Fewer partitions | simpler operations | throughput/parallelism ceiling |
| Delete retention | predictable storage | old replay disappears |
| Compaction | latest state restoration | not full history |
| Key by aggregate ID | per-entity ordering | hot entity can create skew |
| Random key | good distribution | no business ordering |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Strong ordering:
  Use stable aggregate key, but accept possible hot keys.
- High throughput:
  Use more partitions, but accept higher operational overhead.
- Long replay:
  Increase retention, but pay storage cost.
- Compacted state:
  Great for latest state, but not enough for full audit.
- Separate topics:
  Clear ownership, but more platform surface area.

### Common Mistakes

- Mistake: "We can always increase partitions later."
  Why it is wrong: key mapping may change and affect ordering.
  Better approach: size with growth in mind or migrate intentionally.

- Mistake: "Compaction is backup."
  Why it is wrong: compaction may remove old values.
  Better approach: use backups/archives for compliance and disaster recovery.

- Mistake: "Retention means consumer processed it."
  Why it is wrong: retention is time/size based, independent of consumers.
  Better approach: monitor lag and ensure consumers stay within retention.

- Mistake: "Use null key for everything."
  Why it is wrong: partitioning becomes distribution-focused, not ordering-focused.
  Better approach: choose a key based on business ordering.

---

## 11. Key Numbers

Interview-ready planning numbers:

- Common replication factor for critical topics: `3`
- Common `min.insync.replicas` with RF=3: `2`
- Partitions per topic: depends heavily; from a few to hundreds
- Retention: hours to weeks for event topics; sometimes longer with archive/tiered storage
- Consumer group parallelism ceiling: number of partitions
- Target partition size: plan by broker storage, segment settings, and recovery time
- Tombstone retention: long enough for all consumers/restorers to observe deletes

Sizing formula:

```text
daily_storage = messages_per_day * average_message_size * replication_factor
```

Add compression:

```text
effective_storage = daily_storage / compression_ratio
```

Then multiply by retention days.

---

## 12. Failure Modes

### Hot Partition

Cause:

- low-cardinality key
- one celebrity customer
- skewed event source

Symptoms:

- one partition has high lag
- one broker is hot
- consumer group underutilized

Fix:

- change key strategy for future records
- split hot entity if business ordering allows
- use separate topic for hot traffic

### Consumer Falls Behind Retention

Cause:

- consumer down longer than retention
- lag grows beyond replay window

Symptoms:

- consumer cannot read required offsets
- data loss from consumer perspective

Fix:

- restore from archive if available
- reset offset with business decision
- increase retention or improve consumer capacity

### Compacted Topic Loses Needed History

Cause:

- topic used for audit even though compaction removed older values

Symptoms:

- replay reconstructs latest state but not state transitions

Fix:

- use separate event-history topic
- archive raw events
- use compacted topic only for current state

### Tombstone Not Handled

Cause:

- consumer assumes value is never null

Symptoms:

- null pointer errors
- state never deletes properly

Fix:

- consumers handle null values
- test tombstone processing
- document delete semantics

---

## 13. Scenario

- Product / system: Customer profile platform
- Why this concept fits: consumers need latest profile state, while audit may also need history
- What would go wrong without it: using one compacted topic for everything would lose historical changes needed for audit

Design:

```text
customer-profile-events       cleanup.policy=delete
customer-profile-current      cleanup.policy=compact
```

---

## 14. Code Sample

Topic creation command examples:

```bash
kafka-topics.sh \
  --bootstrap-server broker1:9092 \
  --create \
  --topic order-events \
  --partitions 24 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=604800000
```

Compacted topic:

```bash
kafka-topics.sh \
  --bootstrap-server broker1:9092 \
  --create \
  --topic customer-profile-current \
  --partitions 24 \
  --replication-factor 3 \
  --config cleanup.policy=compact \
  --config min.insync.replicas=2
```

---

## 15. Mini Program / Simulation

This simulation shows how increasing partitions can move keys.

```python
import hashlib


def partition(key: str, partition_count: int) -> int:
    digest = hashlib.sha256(key.encode()).hexdigest()
    return int(digest, 16) % partition_count


def main():
    keys = ["order-1", "order-2", "order-3", "order-4", "order-5"]

    for key in keys:
        before = partition(key, 6)
        after = partition(key, 12)
        moved = before != after
        print(f"{key}: p6={before}, p12={after}, moved={moved}")


if __name__ == "__main__":
    main()
```

Interview takeaway:

Increasing partitions changes the modulo calculation for many keys. Plan migrations carefully if per-key ordering is strict.

---

## 16. Practical Question

> You need to design Kafka topics for order events. How do you choose topic name, key, partitions, replication, and retention?

---

## 17. Strong Answer

I would start from the business access pattern. For order lifecycle events, I would create an `order-events` topic owned by the order domain. I would key events by `orderId` so all events for a given order stay in one partition and preserve per-order ordering.

I would choose partition count based on target throughput, consumer parallelism, average message size, and growth. Since changing partition count later can affect key mapping, I would size for a realistic growth window rather than just today's traffic.

For critical events, I would use replication factor 3, `min.insync.replicas=2`, and producers with `acks=all`. Retention would depend on replay and audit needs. If consumers need full order history, I would use delete retention with an appropriate time window or archive. If another use case needs latest state by order, I would create a separate compacted topic such as `order-current-state`.

I would document owner, schema, key, cleanup policy, retention, expected volume, and DLQ strategy.

---

## 18. Revision Notes

- One-line summary: Topic design is key choice, partition count, retention, compaction, and ownership.
- Three keywords: key, retention, compaction
- One interview trap: increasing partitions can affect per-key ordering expectations.
- One memory trick: key decides lane, retention decides memory, compaction decides latest state.

---

## 19. Official Source Notes

- Apache Kafka topic configs: <https://kafka.apache.org/43/generated/topic_config.html>
- Apache Kafka log compaction design: <https://kafka.apache.org/43/design/design/>

