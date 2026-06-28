# Kafka Producer Consumer Delivery Guarantees Gold Sheet

> Goal: understand what Kafka actually guarantees, where duplicates come from, and how to design producer and consumer code that survives real failures.

---

## 0. How To Read This

Beginner focus:

- producer
- consumer
- offset
- consumer group
- retry
- duplicate

Intermediate focus:

- `acks`
- idempotent producer
- manual offset commit
- rebalance
- retry topic
- DLQ

Senior focus:

- exact failure windows
- idempotent side effects
- offset commit strategy
- delivery semantics boundary
- poison message handling
- lag and backpressure

---

# Topic 1: Kafka Producer Consumer Delivery Guarantees

---

## 1. Intuition

Kafka delivery is like a courier system with receipts.

- Producer sends a package.
- Broker stores it.
- Consumer picks it up.
- Consumer later marks "processed up to offset X".

Most bugs happen when the package was delivered, but the receipt got lost, or when the consumer did the work but crashed before updating its progress.

Beginner explanation:

Kafka stores messages durably, but your application still decides when a message is considered processed. If you commit offsets too early, you can lose work. If you commit offsets after processing, you can get duplicates.

---

## 2. Definition

- Definition: Kafka delivery guarantees describe whether records may be lost, duplicated, or processed exactly once within a defined boundary.
- Category: Distributed messaging correctness
- Core idea: producer acknowledgments plus consumer offset commits determine the practical guarantee.

---

## 3. Why It Exists

Distributed systems fail in awkward places:

- producer times out after broker wrote the record
- broker leader dies during replication
- consumer crashes after database update
- consumer commits offset before processing
- rebalance moves partition ownership mid-flow
- poison message keeps failing forever

Delivery guarantees exist so we can explain and control those failure windows.

---

## 4. Reality

Delivery guarantees are crucial in:

- payments
- order state changes
- inventory updates
- email notifications
- user activity streams
- fraud detection
- CDC pipelines
- analytics ingestion

In interviews, the strongest answer is not "Kafka is exactly once." The strongest answer is:

> Inside Kafka, we can use idempotent producers and transactions for stronger semantics. For external systems, we still need idempotency, dedupe, or an outbox pattern.

---

## 5. How It Works

### Part A: Producer Acknowledgments

Producer `acks` controls what success means.

| Setting | Meaning | Risk |
|---|---|---|
| `acks=0` | producer does not wait for broker acknowledgment | fastest, can lose data silently |
| `acks=1` | leader acknowledges after local write | follower may not have copied yet |
| `acks=all` | leader waits for enough in-sync replicas | safest common option, higher latency |

For important data:

```text
replication.factor = 3
min.insync.replicas = 2
producer acks = all
enable.idempotence = true
```

This means the write succeeds only when enough in-sync replicas have the record.

### Part B: Producer Retry Duplicate Window

Failure sequence:

```text
producer sends record
broker writes record
ack is delayed or lost
producer times out
producer retries
```

Without idempotence, Kafka may store the record twice.

With idempotence, Kafka can identify producer retry duplicates for a producer session and partition.

Important:

- idempotent producer helps producer retry duplication
- it does not make external side effects exactly once
- it does not save you from using a bad event key or bad consumer logic

### Part C: Consumer Offset Commit

Consumer tracks progress with offsets.

```text
partition orders-1
processed offset 100
committed offset 101 means "next record to read is 101"
```

Offset commit strategies:

| Strategy | Flow | Guarantee |
|---|---|---|
| Commit before processing | commit, then do work | at-most-once, can lose work |
| Commit after processing | do work, then commit | at-least-once, can duplicate work |
| Transactional consume-transform-produce | process and commit offsets transactionally to Kafka | exactly-once inside Kafka boundary |

### Part D: At-Most-Once

Flow:

```text
poll record
commit offset
process record
```

If consumer crashes after commit but before processing:

```text
Kafka thinks record is done
work never happened
```

Use only when loss is acceptable, such as low-value metrics.

### Part E: At-Least-Once

Flow:

```text
poll record
process record
commit offset
```

If consumer crashes after processing but before commit:

```text
work happened
record is replayed
work may happen again
```

This is the common production default.

Requirement:

- consumer side effects must be idempotent
- use unique event IDs or business IDs
- dedupe in target table when needed

### Part F: Exactly-Once Boundary

Kafka can support exactly-once style processing for:

```text
consume from Kafka
transform
produce to Kafka
commit consumed offsets in the same Kafka transaction
```

But if your consumer:

- updates a database
- calls a payment gateway
- sends email
- calls another HTTP service

then Kafka alone cannot guarantee exactly-once for that external action.

You need:

- idempotency keys
- unique constraints
- outbox pattern
- transactional sink support
- dedupe table
- compensating actions

### Part G: Consumer Rebalance Failure Window

Consumer group rebalances happen when:

- consumer joins
- consumer leaves
- heartbeat is missed
- `max.poll.interval.ms` is exceeded
- partition count changes
- assignment strategy changes

During rebalance:

- partitions are revoked
- another consumer may receive them
- uncommitted records can be replayed

Mitigation:

- process records quickly
- avoid blocking the poll thread
- commit at safe checkpoints
- use cooperative rebalancing when appropriate
- pause partitions during slow downstream calls

---

## 6. What Problem It Solves

- Primary problem solved: reasoning about loss, duplication, and processing progress
- Secondary benefits: safer retries, replay, recoverability, predictable operations
- Systems impact: prevents silent data loss and helps teams design idempotent workflows

---

## 7. When To Rely On Each Guarantee

Use at-most-once when:

- data is low value
- loss is acceptable
- latency is more important than completeness

Use at-least-once when:

- business events must not be lost
- duplicate handling is possible
- you can make side effects idempotent

Use Kafka transactions when:

- consuming from Kafka and producing back to Kafka
- correctness is worth extra complexity
- downstream is Kafka-aware

Use outbox or idempotency when:

- external database or API side effects are involved
- payment, order, inventory, or notification behavior must be safe

---

## 8. When Not To Overuse Strong Guarantees

Avoid transactions when:

- simple at-least-once plus idempotency is enough
- output is not Kafka
- latency is extremely sensitive
- the team does not understand transaction failure modes
- the business accepts duplicates but not complexity

Avoid `acks=all` for:

- throwaway telemetry
- noncritical logs
- very low-latency paths where small data loss is accepted

---

## 9. Pros and Cons

| Approach | Pros | Cons |
|---|---|---|
| At-most-once | low latency, simple | can lose messages |
| At-least-once | safe against loss | duplicates possible |
| Idempotent producer | handles producer retry duplicates | does not solve consumer side effects |
| Transactions | strong Kafka boundary semantics | more latency and complexity |
| Idempotent consumer | practical and robust | needs business keys/dedupe design |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Committing early:
  Lower duplicate risk, higher data loss risk.
- Committing late:
  Lower data loss risk, higher duplicate risk.
- `acks=all`:
  Better durability, more latency.
- More retries:
  Better resilience to transient failures, but may increase duplicates without idempotence.
- DLQ:
  Keeps pipeline moving, but creates a separate recovery workflow.

### Common Mistakes

- Mistake: "At-least-once means exactly once if my code is fast."
  Why it is wrong: crash windows still exist.
  Better approach: design idempotent consumers.

- Mistake: "I enabled idempotent producer, so the whole workflow is exactly once."
  Why it is wrong: producer idempotence only handles producer retry duplicates.
  Better approach: define the boundary and protect external side effects.

- Mistake: "Auto commit is always fine."
  Why it is wrong: offsets may be committed before processing is truly done.
  Better approach: use manual commit for business-critical consumers.

- Mistake: "DLQ means the problem is solved."
  Why it is wrong: DLQ is only parking. Someone must inspect, fix, and replay or discard.
  Better approach: add ownership, alerting, and replay tooling.

---

## 11. Key Numbers

Useful interview ranges:

- Default producer `delivery.timeout.ms` in Kafka docs: 2 minutes
- Default `request.timeout.ms` in Kafka docs: 30 seconds
- Default producer `linger.ms` in Kafka 4.x docs: 5 ms
- Common business consumer commit batch: every 100 to 1000 records or every few seconds, depending on duplicate tolerance
- Common max retry attempts for app-level processing: 3 to 10 before retry topic or DLQ
- Common retry topic delays: seconds, minutes, then tens of minutes

Always tune based on:

- message cost
- duplicate tolerance
- downstream latency
- lag SLA
- throughput

---

## 12. Failure Modes

### Producer Times Out After Successful Broker Write

Observed:

- producer thinks send failed
- broker already stored record

Risk:

- duplicate on retry

Mitigation:

- `enable.idempotence=true`
- stable keys
- idempotent downstream processing

### Consumer Crashes After DB Write Before Offset Commit

Observed:

- database changed
- offset not committed
- record reprocessed after restart

Risk:

- duplicate charge/order/email/update

Mitigation:

- unique event ID
- idempotency table
- database unique constraint
- outbox pattern

### Consumer Commits Before Work

Observed:

- offset moves forward
- processing fails

Risk:

- lost work

Mitigation:

- commit after successful processing
- manually control commits
- only use early commit when loss is acceptable

### Poison Message

Observed:

- one message fails every time
- partition stops progressing

Risk:

- lag grows forever

Mitigation:

- bounded retries
- retry topic
- DLQ
- alert with original topic, partition, offset, key, exception

### Rebalance During Long Processing

Observed:

- partition revoked from consumer
- another consumer processes same records

Risk:

- duplicate side effects

Mitigation:

- keep `poll()` healthy
- move slow work off poll thread carefully
- pause/resume partitions
- commit on partition revoke when safe

---

## 13. Scenario

- Product / system: Inventory reservation pipeline
- Why this concept fits: inventory events must not be lost, but duplicate reservation attempts must be safe
- What would go wrong without it: consumer crash could double-reserve inventory or skip a reservation

---

## 14. Code Sample

Manual commit after successful processing:

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtLeastOnceConsumer {
    private final KafkaConsumer<String, String> consumer;
    private final InventoryService inventoryService;

    public AtLeastOnceConsumer(KafkaConsumer<String, String> consumer, InventoryService inventoryService) {
        this.consumer = consumer;
        this.inventoryService = inventoryService;
    }

    public void run() {
        consumer.subscribe(List.of("inventory-events"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();

            for (ConsumerRecord<String, String> record : records) {
                inventoryService.applyIdempotently(record.key(), record.value());

                TopicPartition partition = new TopicPartition(record.topic(), record.partition());
                offsets.put(partition, new OffsetAndMetadata(record.offset() + 1));
            }

            if (!offsets.isEmpty()) {
                consumer.commitSync(offsets);
            }
        }
    }
}

interface InventoryService {
    void applyIdempotently(String key, String payload);
}
```

Interview explanation:

- offset is committed only after processing
- duplicate processing is still possible
- `applyIdempotently` is mandatory for correctness

---

## 15. Mini Program / Simulation

This simulation shows why commit timing matters.

```python
def process(commit_before_work: bool, crash_after_commit: bool, crash_after_work: bool):
    committed = False
    work_done = False

    if commit_before_work:
        committed = True
        if crash_after_commit:
            return committed, work_done, "lost work risk"

    work_done = True

    if crash_after_work:
        return committed, work_done, "duplicate risk on restart"

    committed = True
    return committed, work_done, "safe completion"


def main():
    cases = [
        ("commit before, crash after commit", True, True, False),
        ("commit after, crash after work", False, False, True),
        ("commit after, no crash", False, False, False),
    ]

    for name, before, crash_commit, crash_work in cases:
        print(name, "->", process(before, crash_commit, crash_work))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> A Kafka consumer writes order events into a database. It sometimes crashes. How do you avoid losing messages and avoid duplicate business effects?

---

## 17. Strong Answer

I would use at-least-once consumption with manual offset commits after the database write succeeds. That prevents message loss, but it means the same record can be processed again if the consumer crashes after the DB write and before the offset commit.

To handle that duplicate window, I would make the database write idempotent. Each event would have a stable `eventId` or business key. The target table would use a unique constraint or a processed-events table so reprocessing the same event becomes a no-op.

For transient failures, I would retry with backoff. For poison messages, I would send them to a DLQ with topic, partition, offset, key, payload, and error details. I would alert on DLQ growth and consumer lag.

Kafka gives me durable replay and offset control. The database side still needs idempotency.

---

## 18. Revision Notes

- One-line summary: Delivery guarantees are controlled by producer ack/retry behavior and consumer offset commit timing.
- Three keywords: `acks`, idempotency, offset commit
- One interview trap: exactly-once must always name its boundary.
- One memory trick: commit before work can lose; commit after work can duplicate.

---

## 19. Official Source Notes

- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>

