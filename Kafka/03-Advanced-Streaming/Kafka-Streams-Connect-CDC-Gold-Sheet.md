# Kafka Streams, Connect, and CDC Gold Sheet

> Goal: know when to write a consumer app, when to use Kafka Streams, when to use Kafka Connect, and how CDC/outbox fits into event-driven systems.

---

## 0. How To Read This

Beginner focus:

- stream processing
- connector
- source
- sink
- CDC

Intermediate focus:

- KStream
- KTable
- state store
- window
- join
- Connect worker
- source/sink connector

Senior focus:

- Streams vs Connect vs custom service
- stateful processing recovery
- changelog topics
- repartition topics
- Debezium CDC
- outbox pattern
- exactly-once boundaries

---

# Topic 1: Kafka Streams, Connect, and CDC

---

## 1. Intuition

Kafka has three common ways to process data:

```text
plain consumer app = custom worker
Kafka Streams = stream processing library
Kafka Connect = data integration framework
```

Analogy:

- Plain consumer is a custom car you build yourself.
- Kafka Streams is a race-ready engine for transformations, joins, windows, and state.
- Kafka Connect is a moving truck for data between Kafka and external systems.

Beginner explanation:

Use a normal consumer for custom business actions, Kafka Streams for Kafka-to-Kafka transformations and stateful stream processing, and Kafka Connect for moving data between Kafka and databases/search systems/files/cloud services.

---

## 2. Definition

- Definition: Kafka Streams is a client library for building stream processing applications on Kafka, while Kafka Connect is a framework for scalable data import/export between Kafka and external systems.
- Category: Kafka ecosystem processing and integration
- Core idea: use the right tool for transformation, state, and integration instead of hand-building everything.

---

## 3. Why It Exists

Without these tools, teams repeatedly build:

- custom consumers
- offset tracking
- retries
- state management
- joins
- windows
- database import/export logic
- schema conversion
- CDC relays
- sink retry handling

Kafka Streams and Connect exist to standardize these patterns.

---

## 4. Reality

Used in:

- real-time fraud scoring
- clickstream aggregation
- notification fan-out
- user profile materialization
- order lifecycle joins
- CDC from databases
- Elasticsearch indexing
- S3/data lake export
- analytics pipelines

Senior interviewers care less about API memorization and more about choosing correctly:

```text
Do I need transformation?
Do I need state?
Do I need external system integration?
Do I need custom business side effects?
```

---

## 5. How It Works

### Part A: Plain Consumer App

Use a plain consumer when you need custom side effects:

- call payment API
- send email/SMS
- update business DB
- call internal service
- apply complex domain rules

Flow:

```text
poll records
process business logic
write side effect
commit offsets
handle retries/DLQ
```

Strength:

- maximum control

Weakness:

- you own retries, state, idempotency, deployment, lag, and recovery

### Part B: Kafka Streams

Kafka Streams is a Java library. Your application runs as normal app instances, but Kafka handles partitioned input and group coordination.

Use it for:

- filtering
- mapping
- grouping
- aggregating
- joining streams
- time windows
- stateful processing
- Kafka-to-Kafka pipelines

Important abstractions:

| Concept | Meaning |
|---|---|
| `KStream` | unbounded stream of events |
| `KTable` | changelog stream representing latest state by key |
| `GlobalKTable` | full replicated table on each instance |
| state store | local state, often backed by changelog topic |
| window | time-bounded grouping |
| repartition topic | internal topic used when key changes before grouping/joining |
| changelog topic | internal topic used to rebuild state stores |

### Part C: KStream vs KTable

KStream:

```text
every event matters
clicks, orders, payments, logs
```

KTable:

```text
latest value per key matters
customer profile, inventory count, account status
```

Memory trick:

```text
KStream = facts over time
KTable = current state by key
```

### Part D: Stateful Processing

Example:

```text
payment-events
-> group by merchantId
-> aggregate count and amount in 5-minute window
-> fraud-alert-events
```

Kafka Streams stores local state:

- fast local reads
- changelog topic for recovery
- standby replicas optional
- rebalances move task ownership

Failure recovery:

```text
instance dies
another instance takes partitions/tasks
state store is restored from changelog topic
processing resumes
```

### Part E: Kafka Connect

Kafka Connect moves data between Kafka and external systems.

Source connector:

```text
database / file / external system -> Kafka
```

Sink connector:

```text
Kafka -> Elasticsearch / S3 / database / data warehouse
```

Connect components:

| Component | Meaning |
|---|---|
| connector | defines source/sink integration |
| task | unit of parallel work |
| worker | JVM process running connectors/tasks |
| converter | converts data format such as Avro/JSON/Protobuf |
| SMT | single message transform for lightweight changes |
| offset storage | tracks source progress |
| DLQ | captures bad sink/source records when configured |

### Part F: CDC

CDC means change data capture.

Instead of polling:

```sql
select * from orders where updated_at > last_seen;
```

CDC reads database change logs:

```text
DB transaction log
-> CDC connector
-> Kafka topic
```

Common CDC use cases:

- replicate DB changes
- feed search indexes
- materialize read models
- publish domain changes
- migrate data
- sync analytics systems

### Part G: Outbox With CDC

Outbox pattern:

```text
one DB transaction:
  update order table
  insert event row into outbox table

CDC connector:
  reads outbox table changes
  publishes event to Kafka
```

Why it works:

- business state and event record commit together in DB
- CDC reliably exports committed rows
- Kafka publish can be retried independently

### Part H: Streams vs Connect vs Consumer

| Need | Best Fit |
|---|---|
| Call business API | plain consumer |
| Transform Kafka topic to another Kafka topic | Kafka Streams |
| Aggregate/window/join streams | Kafka Streams |
| Move DB changes to Kafka | Kafka Connect CDC |
| Write Kafka data to S3/Elasticsearch | Kafka Connect sink |
| Complex domain workflow with side effects | plain consumer/service |
| Lightweight field rename/drop | Connect SMT if simple |
| Heavy business transformation | Streams or service, not SMT |

---

## 6. What Problem It Solves

- Primary problem solved: reusable stream processing and data integration patterns
- Secondary benefits: state management, connector ecosystem, CDC, replay, scale-out processing
- Systems impact: reduces custom plumbing and makes Kafka useful across operational and analytical systems

---

## 7. When To Rely On It

Use Kafka Streams when:

- input and output are Kafka
- you need joins, windows, or aggregations
- state must be recoverable
- Java/Scala service deployment is acceptable
- exactly-once Kafka processing matters

Use Kafka Connect when:

- source/sink is a known external system
- integration is generic
- you want config-driven scaling
- you need CDC or sink export

Use plain consumer when:

- business logic is custom
- side effects are outside Kafka
- you need full control over retries and idempotency

---

## 8. When Not To Use It

Avoid Kafka Streams when:

- you only need to call an external API
- state is too large without careful design
- language/runtime constraints do not fit
- a SQL/data platform is more appropriate

Avoid Kafka Connect when:

- transformation is complex business logic
- connector quality is poor
- sink requires custom transaction behavior
- error handling needs domain-specific decisions

Avoid CDC when:

- database logs are inaccessible
- schema changes are uncontrolled
- downstream cannot handle update/delete semantics
- event semantics need domain events, not raw row changes

---

## 9. Pros and Cons

| Tool | Pros | Cons |
|---|---|---|
| Plain consumer | full control | more custom code and failure handling |
| Kafka Streams | stateful processing, joins, windows | Java library, internal topics, state tuning |
| Kafka Connect | reusable source/sink integration | connector behavior varies, less custom logic |
| CDC | reliable DB change capture | exposes database shape unless outbox is used |
| Outbox | DB/event consistency | extra table, relay/CDC, cleanup |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Kafka Streams state:
  Fast local state, but restoration and changelog topics must be planned.
- Connect simplicity:
  Less code, but connector-specific behavior and ops matter.
- CDC raw table events:
  Easy integration, but leaks database model to consumers.
- Outbox:
  Better domain events, but more implementation work.
- Repartitioning:
  Enables correct grouping by new key, but adds internal topics and network cost.

### Common Mistakes

- Mistake: "Kafka Streams is a Kafka server feature."
  Why it is wrong: it is a client library running in your application instances.
  Better approach: deploy and monitor it like an application.

- Mistake: "KTable is just another stream."
  Why it is wrong: it represents latest state by key.
  Better approach: use KTable for changelog/table-like semantics.

- Mistake: "Use Connect SMTs for complex business logic."
  Why it is wrong: SMTs are intended for lightweight message transformations.
  Better approach: use Streams or a service for domain logic.

- Mistake: "CDC equals domain events."
  Why it is wrong: raw row changes may not express business meaning.
  Better approach: use outbox for domain events when semantics matter.

---

## 11. Key Numbers

Interview guidance:

- Kafka Streams parallelism is bounded by input partitions/tasks.
- State store recovery time depends on changelog size and restore throughput.
- Connect parallelism depends on connector support and task count.
- CDC lag should be monitored from DB log position to Kafka offset.
- DLQ volume should be near zero for healthy pipelines.
- Internal topics need replication and retention/compaction planning.

No universal number is safe here. State size, message volume, and connector behavior dominate.

---

## 12. Failure Modes

### Streams App Dies

What fails:

- instance owning tasks goes down

What user observes:

- temporary processing pause
- rebalance

Recovery:

- other instances take tasks
- state restored from changelog topics

Mitigation:

- enough instances
- state store changelog replication
- standby replicas for critical low-recovery-time apps

### Repartition Topic Explosion

What fails:

- operations like `groupBy` after changing key create internal topics

What user observes:

- unexpected topics
- extra storage/network cost

Mitigation:

- understand topology
- name processors
- monitor internal topics
- avoid unnecessary key changes

### Connect Sink Poison Record

What fails:

- sink cannot write one record due to schema/data issue

What user observes:

- connector task fails or stalls

Mitigation:

- DLQ configuration
- error tolerance policy
- schema validation
- replay/fix workflow

### CDC Connector Falls Behind

What fails:

- connector cannot keep up with DB change log

What user observes:

- CDC lag grows
- downstream views become stale

Mitigation:

- monitor source lag
- scale tasks if connector supports it
- tune DB log retention
- reduce connector transformations

### Raw CDC Breaks Consumers

What fails:

- DB column renamed or table split

What user observes:

- downstream consumers break even though business event semantics did not change

Mitigation:

- outbox domain events
- schema registry
- contract ownership
- migration plan

---

## 13. Scenario

- Product / system: E-commerce search indexing
- Why this concept fits: order/product DB changes must update Elasticsearch quickly
- What would go wrong without it: custom polling can miss changes, overload DB, or produce stale search results

Design:

```text
product DB
-> CDC source connector
-> product-change-events
-> Kafka Streams enrichment/filtering if needed
-> Elasticsearch sink connector
```

---

## 14. Code Sample

Kafka Streams example: count orders per customer.

```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Produced;

public class OrderCountTopology {
    public static Topology build() {
        StreamsBuilder builder = new StreamsBuilder();

        builder.stream("order-events", Consumed.with(Serdes.String(), Serdes.String()))
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .count()
                .toStream()
                .to("customer-order-counts", Produced.with(Serdes.String(), Serdes.Long()));

        return builder.build();
    }
}
```

Interview explanation:

- input key should be `customerId` if counting per customer
- aggregation creates state
- state is backed by changelog topic
- output topic contains updated counts

---

## 15. Mini Program / Simulation

This simulation chooses the tool based on requirements.

```python
def choose_tool(input_kafka, output_kafka, external_source_or_sink, stateful, custom_side_effect):
    if external_source_or_sink and not custom_side_effect:
        return "Kafka Connect"
    if input_kafka and output_kafka and stateful:
        return "Kafka Streams"
    if custom_side_effect:
        return "Plain Consumer Service"
    if input_kafka and output_kafka:
        return "Kafka Streams or simple consumer"
    return "Re-check requirements"


def main():
    print(choose_tool(True, True, False, True, False))
    print(choose_tool(False, True, True, False, False))
    print(choose_tool(True, False, False, False, True))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> You need to publish order events from a relational database to Kafka and then build a real-time dashboard. Would you use custom consumers, Kafka Streams, Kafka Connect, or CDC?

---

## 17. Strong Answer

I would avoid polling the database. If the service already owns the order write path, I would use the outbox pattern: write the order update and an outbox event in the same database transaction. Then a CDC connector can publish those outbox events to Kafka.

For the dashboard, if I need aggregations like orders per minute or revenue per merchant, I would use Kafka Streams. It gives me windows, grouping, local state stores, and changelog-backed recovery. The output can go to a dashboard topic, Redis, Elasticsearch, or another serving store through a sink connector or dedicated service.

I would use Schema Registry for the outbox event contract, monitor CDC lag and Streams lag, and add DLQs for connector or processing errors. If the dashboard update involves custom side effects, I would use a plain consumer service with idempotency.

---

## 18. Revision Notes

- One-line summary: Streams transforms Kafka data; Connect moves data in/out; CDC captures database changes; outbox makes DB-to-Kafka reliable.
- Three keywords: KStream, connector, outbox
- One interview trap: CDC row changes are not always good domain events.
- One memory trick: Streams computes, Connect moves, consumers act.

---

## 19. Official Source Notes

- Apache Kafka Streams docs: <https://kafka.apache.org/43/streams/introduction/>
- Apache Kafka Connect docs: <https://kafka.apache.org/43/kafka-connect/overview/>
- Apache Kafka transaction protocol: <https://kafka.apache.org/43/operations/transaction-protocol/>

