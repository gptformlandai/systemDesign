# Kafka Transactions, Idempotency, and Exactly-Once Gold Sheet

> Goal: explain Kafka exactly-once semantics without overclaiming, and design correct workflows across Kafka and external systems.

---

## 0. How To Read This

Beginner focus:

- duplicate
- idempotency
- transaction
- commit
- abort

Intermediate focus:

- idempotent producer
- transactional producer
- `transactional.id`
- `read_committed`
- `sendOffsetsToTransaction`

Senior focus:

- consume-transform-produce boundary
- external side effects
- outbox pattern
- transactional sink limitations
- producer fencing
- failure recovery

---

# Topic 1: Kafka Transactions, Idempotency, and Exactly-Once

---

## 1. Intuition

A Kafka transaction is like submitting a group of changes as one sealed envelope.

Inside the envelope:

- output records
- consumed offsets
- commit or abort decision

If the transaction commits, consumers with `read_committed` can see the output. If it aborts, the output is ignored.

Beginner explanation:

Kafka transactions help when an application reads from Kafka, writes back to Kafka, and commits the consumed offsets as one atomic unit. They do not automatically make database writes, emails, or payment calls exactly once.

---

## 2. Definition

- Definition: Kafka transactions allow a producer to atomically write records to one or more partitions and commit consumed offsets as part of the same transaction.
- Category: Stream processing correctness
- Core idea: either all Kafka writes and offset commits in the transaction become visible, or none of them do.

---

## 3. Why It Exists

Without transactions, this failure is possible:

```text
consume order event
produce payment event
crash before committing consumed offset
restart
consume same order event again
produce duplicate payment event
```

Kafka transactions solve this for Kafka-to-Kafka processing:

```text
consume input
produce output
commit input offset
all in one Kafka transaction
```

If the app crashes mid-transaction, Kafka can abort or recover the transaction state, and downstream `read_committed` consumers do not see partial output.

---

## 4. Reality

Kafka transactions are commonly discussed in:

- Kafka Streams
- stream enrichment
- fraud detection pipelines
- aggregation pipelines
- CDC transformations
- real-time analytics joins
- event routing and normalization

They are less commonly the right answer for:

- calling payment gateways
- sending user emails
- updating non-transactional external APIs
- workflows where simple idempotent consumers are enough

---

## 5. How It Works

### Part A: Idempotent Producer vs Transactional Producer

Idempotent producer:

- prevents duplicate writes caused by producer retries
- works per producer session and partition
- does not group multiple writes atomically
- does not commit consumer offsets

Transactional producer:

- uses idempotence internally
- has a stable `transactional.id`
- can group records across partitions
- can commit offsets with produced records
- supports commit/abort

Think of it this way:

```text
idempotence = avoid retry duplicates
transactions = atomic Kafka workflow
```

### Part B: Transaction Flow

Typical consume-transform-produce flow:

```text
producer.initTransactions()

loop:
  records = consumer.poll()
  producer.beginTransaction()
  for record in records:
      output = transform(record)
      producer.send(outputTopic, output)
  producer.sendOffsetsToTransaction(offsets, consumerGroupMetadata)
  producer.commitTransaction()
```

If something fails:

```text
producer.abortTransaction()
```

### Part C: `read_committed`

Consumers can choose isolation level:

| Consumer isolation | Behavior |
|---|---|
| `read_uncommitted` | may read records from open or aborted transactions |
| `read_committed` | reads only committed transactional records |

For downstream correctness, use `read_committed` when reading transactional outputs.

### Part D: Producer Fencing

`transactional.id` identifies the logical transactional producer.

If two producer instances accidentally use the same `transactional.id`, Kafka fences the older producer epoch so the system does not allow two active writers for the same transactional identity.

Interview wording:

> The `transactional.id` must be stable for recovery but unique per active producer instance or task. Reusing it incorrectly can fence a live producer.

### Part E: Exactly-Once Boundary

Kafka transactions can protect this:

```text
Kafka input topic
-> app transform
-> Kafka output topic
-> offset commit
```

Kafka transactions do not automatically protect this:

```text
Kafka input topic
-> app
-> external DB
-> external payment API
-> email provider
```

For external systems, use:

- idempotency key
- unique constraint
- transactional outbox
- CDC outbox relay
- dedupe table
- saga compensation
- sink connector with transactional semantics if supported

### Part F: Outbox Pattern

Outbox solves this common problem:

```text
write business data to DB
publish event to Kafka
```

Naive failure:

```text
DB commit succeeds
Kafka publish fails
event missing
```

Outbox flow:

```text
one DB transaction:
  update business table
  insert row into outbox table

CDC or relay:
  reads outbox row
  publishes to Kafka
  marks/publishes idempotently
```

This makes DB state and event publication reliable without needing a distributed two-phase commit across DB and Kafka.

### Part G: Kafka Streams EOS (EXACTLY_ONCE_V2)

Kafka Streams has built-in exactly-once support when the input and output are both Kafka topics.

Enable it with:

```java
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
```

What EXACTLY_ONCE_V2 does internally:

```text
For each task:
  input consumer uses read_committed isolation
  output producer uses a transactional producer per task
  offset commits happen within the Kafka transaction
  output records and input offset commit are atomic
```

`EXACTLY_ONCE_V2` improvements over V1:

| Aspect | EXACTLY_ONCE_V1 | EXACTLY_ONCE_V2 |
|---|---|---|
| Transactional producers | one per Streams thread | one per task |
| Rebalance handling | task-level isolation | task-level, fewer coordinators needed |
| Kafka requirement | Kafka 2.5+ | Kafka 2.5+ (same) |
| Performance | more overhead | slightly better with task-level granularity |

Interview line:

```text
Kafka Streams EXACTLY_ONCE_V2 wraps each task's produce-and-commit in a Kafka transaction.
It does not protect side effects outside Kafka, like database writes or API calls.
```

Caveats:

- throughput is lower than `at_least_once` due to transaction overhead
- state store operations are still eventually consistent outside the Kafka boundary
- changelog topics and repartition topics must have sufficient replication

### Part H: transactional.id In Kubernetes

In Kubernetes, pods restart and may get new names. The `transactional.id` must be stable across restarts.

Strategy 1: Stateful naming

```text
Deploy the Kafka Streams app as a StatefulSet with pod names like:
  payment-processor-0
  payment-processor-1

transactional.id = app-name + task-id = "payment-processor-0-task-0"
```

Strategy 2: External ID registry

```text
On startup, each instance claims a stable ID from a coordination store (e.g., config map, Redis, ZooKeeper)
transactional.id = claimed stable ID
On crash and restart, the same ID is reclaimed
```

Why it matters:

```text
If a Kubernetes pod restarts and a new pod picks up the same partition, it must use the same
transactional.id to correctly fence the old producer and continue processing without duplication.

If the transactional.id is pod-name-based but pod names are ephemeral, a new pod starts a new
transaction producer with no connection to in-flight transactions from the old pod.
```

### Part I: Transaction Coordinator

The transaction coordinator is not a separate service. It is a broker that owns the `__transaction_state` partition.

Assignment:

```text
coordinator_partition = hash(transactional.id) % num_partitions(__transaction_state)
coordinator_broker = leader of that partition
```

Why it matters:

- if the coordinator broker is down, new `initTransactions()` and `commitTransaction()` calls block
- coordinator failover (leader election) is automatic but adds latency
- avoid co-locating many transactional producers that hash to the same coordinator partition

---

## 6. What Problem It Solves

- Primary problem solved: duplicate or partial Kafka-to-Kafka processing during crashes
- Secondary benefits: atomic offset commits with output records, cleaner stream processing recovery
- Systems impact: lets stateful stream processors recover without publishing partial results

---

## 7. When To Rely On It

Use Kafka transactions when:

- reading from Kafka and writing back to Kafka
- output records and offset commits must be atomic
- duplicate output is expensive
- you are building Kafka Streams or similar processing
- downstream consumers can use `read_committed`

Use idempotency instead when:

- output is an external DB or API
- duplicate work is easy to detect
- latency must be lower
- operational simplicity matters more

Use outbox when:

- DB state change and Kafka event must stay consistent
- a service owns a database and publishes domain events
- you want reliable event publication without distributed transactions

---

## 8. When Not To Use It

Avoid Kafka transactions when:

- you are only producing independent events
- duplicates are acceptable and cheap
- output is not Kafka
- team lacks operational maturity
- high throughput/low latency matters more than atomicity
- a simple idempotent consumer solves the business problem

Senior point:

Complex correctness tools can create new failure modes. Use transactions when the atomicity boundary truly matches Kafka.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Atomic Kafka writes and offset commits | More configuration and operational complexity |
| Prevents partial output visibility | Higher latency than simple produce |
| Useful for stream processing | Requires careful `transactional.id` management |
| Works across partitions/topics | External systems still need idempotency |
| Pairs with `read_committed` consumers | Misuse can cause producer fencing issues |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Stronger correctness:
  Fewer duplicate Kafka outputs, but more latency and complexity.
- `read_committed`:
  Safer output visibility, but consumers may wait behind open transactions.
- Stable `transactional.id`:
  Needed for recovery, but wrong reuse fences producers.
- Outbox:
  Strong DB/event consistency, but adds table, relay, CDC, and cleanup complexity.

### Common Mistakes

- Mistake: "Kafka transactions make my database update exactly once."
  Why it is wrong: Kafka cannot control your external database unless the integration is designed for it.
  Better approach: use idempotency, outbox, or transactional sink semantics.

- Mistake: "Idempotent producer and transaction are the same thing."
  Why it is wrong: idempotence handles retry duplicates; transactions group records and offsets atomically.
  Better approach: use the lighter tool when it is enough.

- Mistake: "All consumers automatically ignore aborted records."
  Why it is wrong: consumers must use `isolation.level=read_committed`.
  Better approach: configure downstream consumers intentionally.

- Mistake: "One `transactional.id` for every instance is fine."
  Why it is wrong: active producers can fence each other.
  Better approach: assign stable unique IDs per task/instance.

---

## 11. Key Numbers

Useful interview approximations:

- Transaction timeout: commonly seconds to minutes, tune to processing time
- Transaction batch size: keep bounded; huge transactions increase latency and recovery cost
- Common outbox relay retry: exponential backoff with idempotent publish
- Common dedupe retention: at least as long as Kafka replay window or business duplicate risk window

Rules of thumb:

- Keep transactions short.
- Do not include slow external calls inside a Kafka transaction.
- Use unique event IDs for dedupe even when using transactions.

---

## 12. Failure Modes

### App Crashes Before Commit

What fails:

- transaction started
- records produced
- app crashes before commit

What downstream observes:

- `read_committed` consumers do not see uncommitted output

Recovery:

- transaction is aborted or recovered
- input records can be processed again

### App Commits Output But Not Offset Without Transaction

What fails:

- output event produced
- crash before offset commit

What downstream observes:

- duplicate output after restart

Mitigation:

- use transactions for Kafka-to-Kafka
- or make output idempotent

### External DB Write Inside Kafka Transaction

What fails:

- DB write succeeds
- Kafka transaction aborts

What downstream observes:

- DB state changed but Kafka output missing

Mitigation:

- do not rely on Kafka transaction for external DB atomicity
- use outbox or idempotent DB writes

### Producer Fenced

What fails:

- two instances use same `transactional.id`
- Kafka fences older producer

What user observes:

- producer gets fencing exception

Mitigation:

- unique transactional ID per active task
- stable assignment strategy
- restart logic that does not create duplicate active owners

---

## 13. Scenario

- Product / system: Fraud scoring pipeline
- Why this concept fits: consume `payment-events`, enrich with fraud score, publish `payment-risk-events`, and commit offsets atomically
- What would go wrong without it: crash after output but before offset commit could publish duplicate fraud events

---

## 14. Code Sample

Transactional consume-transform-produce skeleton:

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.List;

public class TransactionalProcessor {
    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;

    public TransactionalProcessor(KafkaConsumer<String, String> consumer,
                                  KafkaProducer<String, String> producer) {
        this.consumer = consumer;
        this.producer = producer;
    }

    public void run() {
        producer.initTransactions();
        consumer.subscribe(List.of("payment-events"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (records.isEmpty()) {
                continue;
            }

            try {
                producer.beginTransaction();

                for (ConsumerRecord<String, String> record : records) {
                    String riskEvent = enrich(record.value());
                    producer.send(new ProducerRecord<>("payment-risk-events", record.key(), riskEvent));
                }

                producer.sendOffsetsToTransaction(
                        OffsetUtil.offsetsFor(records),
                        consumer.groupMetadata()
                );

                producer.commitTransaction();
            } catch (Exception e) {
                producer.abortTransaction();
            }
        }
    }

    private String enrich(String paymentEvent) {
        return paymentEvent.replace("PaymentCreated", "PaymentRiskScored");
    }
}
```

Note:

`OffsetUtil.offsetsFor(records)` is intentionally left as a helper name. In real code, it builds a map of `TopicPartition` to `OffsetAndMetadata` using the next offset for each partition.

---

## 15. Mini Program / Simulation

This simulation shows the boundary difference.

```python
def kafka_transaction(output_written: bool, offset_committed: bool, commit: bool):
    if commit:
        return {
            "visible_output": output_written,
            "offset_advanced": offset_committed,
            "result": "atomic success",
        }
    return {
        "visible_output": False,
        "offset_advanced": False,
        "result": "atomic abort",
    }


def external_side_effect(db_updated: bool, kafka_committed: bool):
    return {
        "db_updated": db_updated,
        "kafka_committed": kafka_committed,
        "warning": "Kafka cannot automatically roll back the external DB",
    }


def main():
    print(kafka_transaction(True, True, commit=False))
    print(external_side_effect(db_updated=True, kafka_committed=False))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> A service consumes `orders`, produces `invoices`, and commits offsets. How would you prevent duplicate invoices if the service crashes?

---

## 17. Strong Answer

If both input and output are Kafka topics, I would use a transactional producer. The service would consume records, begin a transaction, produce invoice events, send consumed offsets to the transaction, and then commit. Downstream consumers should use `read_committed`.

This prevents the classic crash window where output is produced but the input offset is not committed. On restart, Kafka can avoid exposing partial transactional output.

If invoice creation also updates an external database, Kafka transactions alone are not enough. I would add an idempotency key such as `orderId` or `invoiceId`, use unique constraints in the database, or use a transactional outbox so DB state and event publication stay consistent.

---

## 18. Revision Notes

- One-line summary: Kafka transactions make Kafka writes and consumed offsets atomic, but external side effects still need idempotency or outbox.
- Three keywords: `transactional.id`, `read_committed`, outbox
- One interview trap: never claim end-to-end exactly-once without naming the boundary.
- One memory trick: Kafka can atomically protect Kafka, not the whole world.

---

## 19. Official Source Notes

- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>
- Apache Kafka producer configs: <https://kafka.apache.org/43/generated/producer_config.html>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>

