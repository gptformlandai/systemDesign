# Kafka Streams Advanced Processor API And Testing Gold Sheet

> Track: Kafka Interview Track - Advanced Streaming
> Goal: go beyond Streams basics into Processor API, state stores, punctuators, internal topics, testing, reset, and production operations.

---

## 1. Why This Sheet Exists

The existing Streams sheet explains when to use Kafka Streams and how stateful processing works.

This sheet fills the senior follow-ups:

- When is DSL not enough?
- What is the Processor API?
- What are punctuators?
- How are state stores restored?
- What internal topics does Streams create?
- How do you test a topology?
- How do you reset a broken Streams app safely?

---

## 2. Mental Model

Kafka Streams is a client library, not a separate cluster.

```text
input topics
  -> stream task
  -> processors
  -> state stores
  -> changelog topics
  -> repartition topics
  -> output topics
```

Each application instance owns tasks. Tasks own partitions and local state.

---

## 3. DSL vs Processor API

| Approach | Best For | Avoid When |
|---|---|---|
| Streams DSL | map/filter/join/window/aggregate | custom low-level control needed |
| Processor API | custom processors, state access, scheduling | simple transformations |
| Mixed topology | mostly DSL plus custom processor | team cannot operate/debug it |

Rule:
Use DSL first. Add Processor API only when the DSL cannot express the behavior clearly.

---

## 4. Processor API Shape

Conceptual processor:

```java
class FraudScoreProcessor implements Processor<String, PaymentEvent, String, FraudSignal> {
    private ProcessorContext<String, FraudSignal> context;
    private KeyValueStore<String, MerchantState> merchantStore;

    @Override
    public void init(ProcessorContext<String, FraudSignal> context) {
        this.context = context;
        this.merchantStore = context.getStateStore("merchant-state-store");
    }

    @Override
    public void process(Record<String, PaymentEvent> record) {
        MerchantState state = merchantStore.get(record.value().merchantId());
        FraudSignal signal = score(record.value(), state);
        context.forward(record.withValue(signal));
    }
}
```

Production note:
The exact API varies by Kafka Streams version, but the concept is stable: processor receives records, can access state stores, can forward records, and can schedule periodic work.

---

## 5. Punctuators

Punctuators schedule periodic work.

Use cases:
- emit periodic aggregates
- expire stale state
- flush buffered signals
- run time-based checks

Two time domains:

| Time | Meaning |
|---|---|
| stream time | advances with record timestamps |
| wall-clock time | real elapsed clock time |

Trap:
Stream-time punctuation may not fire if no new records arrive.

---

## 6. State Stores

State store types:
- key-value store
- window store
- session store
- custom store when needed

State store lifecycle:

```text
local RocksDB or in-memory store
  -> changelog topic records updates
  -> restore from changelog after restart
  -> standby replicas can reduce recovery time
```

Rules:
- Key data according to query/update pattern.
- Keep state size measurable.
- Monitor restore time.
- Configure changelog topics intentionally.
- Avoid storing secrets or unbounded payloads.

---

## 7. Repartition Topics

Kafka Streams creates repartition topics when key changes before grouping/joining.

Example:

```text
input key = orderId
group by merchantId
  -> repartition required
```

Risks:
- more internal topics
- more network and storage
- more latency
- more operational complexity

Senior line:
Every key change before aggregation has a cost. I inspect topology and internal topics before calling the design cheap.

---

## 8. Changelog Topics

Changelog topics back up local state stores.

Good:
- fast local reads
- restart recovery
- fault tolerance

Cost:
- extra Kafka writes
- storage cost
- restore time after failure
- sensitive state may be persisted in Kafka

Configure:
- retention/compaction
- replication factor
- min ISR
- ACLs

---

## 9. Interactive Queries

Interactive queries expose local state stores for reads.

Use when:
- Streams app maintains a materialized view
- low-latency query over stream-derived state is needed
- routing can find the instance owning a key

Operational burden:
- service discovery
- instance routing
- standby/read fallback
- rolling deploy compatibility
- cache staleness and state restore states

Alternative:
Write materialized results to an external serving store when query simplicity matters more than pure Kafka-native design.

---

## 10. Testing With TopologyTestDriver

Topology tests should verify:
- input to output transformation
- window boundaries
- late event behavior
- tombstone handling
- state store updates
- schema/deserialization errors

Test shape:

```java
TopologyTestDriver driver = new TopologyTestDriver(topology, props);
TestInputTopic<String, PaymentEvent> input = driver.createInputTopic(
    "payments.authorized.v1",
    stringSerializer,
    paymentSerializer
);
TestOutputTopic<String, FraudSignal> output = driver.createOutputTopic(
    "fraud.signals.v1",
    stringDeserializer,
    signalDeserializer
);

input.pipeInput("payment-1", event);
FraudSignal signal = output.readValue();
```

Why it matters:
You can test stream logic without running a real Kafka cluster.

---

## 11. Integration Testing

Use integration tests when verifying:
- serialization with real Schema Registry
- EOS/transaction behavior
- rebalance behavior
- state restore
- Connect/Streams interaction
- real broker topic configs

Tools:
- Testcontainers Kafka
- local Docker Kafka
- staging Kafka environment for selected end-to-end tests

Test pyramid:

```text
many topology/unit tests
some Testcontainers integration tests
few full environment E2E tests
```

---

## 12. Streams Reset

Reset tool is used when a Streams app needs to reprocess input and clear internal state.

Before reset:
- stop all app instances
- confirm input topics and app ID
- confirm output topics strategy
- back up if needed
- verify downstream idempotency
- announce replay blast radius

Risk:
Reset can rebuild state and re-emit outputs. External side effects can duplicate.

---

## 13. Production Metrics

Track:
- process rate
- process latency
- commit latency
- skipped records
- task count
- rebalance rate
- state restore time
- RocksDB metrics
- changelog lag
- standby restore progress
- output production errors

Alert on:
- state restore stuck
- skipped records spike
- rebalance loop
- output topic errors
- changelog under-replication

---

## 14. Failure Modes

| Failure | Symptom | Mitigation |
|---|---|---|
| bad event timestamp | windows look wrong | validate timestamp extraction |
| unbounded state | disk grows | retention/window design |
| repartition explosion | internal topic sprawl | inspect topology |
| restore too slow | long downtime | standby replicas, smaller state |
| reset misused | duplicate outputs | approval and idempotency |
| interactive query during restore | stale/unavailable state | expose readiness state |

---

## 15. Strong Interview Answer

```text
Kafka Streams DSL is enough for common map/filter/join/window/aggregate flows. I
use the Processor API when I need custom processing, state access, or scheduled
punctuation. For production, I inspect internal repartition and changelog topics,
size state stores, configure standby replicas when restore time matters, test logic
with TopologyTestDriver, and treat reset/reprocessing as a governed operation
because it can duplicate downstream outputs.
```

---

## 16. Revision Notes

- One-line summary: Advanced Streams is topology, state, internal topics, tests, and reset safety.
- Three keywords: Processor API, state store, reset.
- One interview trap: Forgetting that repartition and changelog topics are real Kafka topics.
- Memory trick: DSL for normal flow, Processor API for custom control.

---

## 17. Official Source Notes

- Kafka Streams developer guide: https://kafka.apache.org/43/streams/developer-guide/
- Kafka Streams testing: https://kafka.apache.org/43/streams/developer-guide/testing/
- Kafka Streams application reset: https://kafka.apache.org/43/streams/developer-guide/app-reset-tool/
