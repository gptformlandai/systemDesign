# Kafka Reprocessing, Backfill, Replay, and Governance Gold Sheet

> Goal: understand how to replay Kafka data without creating duplicate charges, broken analytics, compliance issues, or a second production incident.

---

## 1. Intuition

Kafka replay is like rewinding a conveyor belt.

That is powerful because you can rebuild state from old events.

It is dangerous because every downstream machine may perform the same action again:

- charge a card
- send an email
- update a search index
- notify a customer
- overwrite a database row
- trigger a fraud alert

Senior Kafka engineers do not say "just reset offsets." They ask:

> What side effects will happen when these records are processed again, and are those side effects idempotent?

---

## 2. Definition

- Definition: Reprocessing is consuming existing Kafka records again to rebuild, repair, or migrate downstream state.
- Category: data recovery / operations / governance.
- Core idea: replay is easy at the log level but risky at the business side-effect level.

---

## 3. Why It Exists

Reprocessing is needed when:

- a consumer bug skipped or corrupted records
- a downstream database needs rebuilding
- a new consumer application needs historical data
- an analytics model needs a backfill
- a schema migration requires historical transformation
- a DLQ must be repaired and reintroduced
- a state store must be restored
- a disaster recovery event requires catch-up

Without replay, teams often write one-off scripts against production databases. That usually loses ordering, auditability, and schema context.

With Kafka replay, teams can use the original event stream, but must control duplicates and side effects.

---

## 4. Replay Types

| Replay Type | How It Works | Common Use |
|---|---|---|
| Same group offset reset | reset existing consumer group offset | repair missed processing after a short bug |
| New group replay | start a new consumer group from old offsets | build a new projection or index |
| Timestamp replay | seek to offsets for a timestamp | backfill a known incident window |
| DLQ replay | fix bad records, produce back to retry/source topic | recover poison messages |
| Shadow topic replay | write transformed old events to a new topic | migration and validation |
| Source backfill | read source system and republish events | when Kafka retention is insufficient |
| Streams app reset | reset internal topics/state for a Kafka Streams app | rebuild stateful topology |
| Compact topic rebuild | consume latest value per key from compacted topic | restore materialized view |

---

## 5. Replay Risk Classification

### Low Risk

Examples:

- rebuild cache
- rebuild search index
- regenerate analytics table
- replay to a shadow topic

Why lower risk:

- side effects are idempotent or replaceable
- duplicates are tolerable
- output can be validated before promotion

### Medium Risk

Examples:

- replay order status projection
- replay user notification preference changes
- replay inventory reservations to a read model

Risks:

- ordering matters
- old events may conflict with current state
- schema versions may differ

### High Risk

Examples:

- payment capture
- refund issuance
- email/SMS notification
- customer account creation
- external API calls

Risks:

- duplicated money movement
- duplicated customer communication
- legal/compliance impact
- irreversible side effects

Rule:

> High-risk replay needs idempotency keys, approval, audit, dry run, rate limit, and rollback strategy.

---

## 6. Preflight Checklist

Before any replay:

1. Identify topic, partitions, consumer group, and offset range.
2. Identify business incident window.
3. Identify downstream side effects.
4. Confirm retention still contains required records.
5. Confirm schemas for historical records are readable.
6. Confirm idempotency and dedupe behavior.
7. Confirm owner approval.
8. Create a dry-run or sample replay plan.
9. Define rate limits.
10. Define success metrics.
11. Define rollback or stop criteria.
12. Create an audit record.

Senior sentence:

> I treat replay as a production change, not a command-line trick.

---

## 7. Offset Reset Patterns

### Reset To Earliest

Use when:

- rebuilding a full projection
- a new consumer group needs all retained data

Risk:

- can cause massive load
- may produce duplicate side effects
- may process obsolete events

### Reset To Latest

Use when:

- abandoning old backlog intentionally
- starting a consumer that only cares about new events

Risk:

- data loss for that consumer group if used accidentally

### Reset To Timestamp

Use when:

- incident has a known start time
- retention contains the window

Risk:

- event time and log append time may not match business time
- producers with clock skew can confuse analysis

### Reset By Shift

Use when:

- moving offsets back or forward a known number of records

Risk:

- record count is rarely a business-safe boundary

---

## 8. CLI Examples

Dry run first:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group payment-risk-v1 \
  --topic payments.events \
  --reset-offsets \
  --to-datetime 2026-07-01T09:30:00.000 \
  --dry-run
```

Execute only after approval:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group payment-risk-v1 \
  --topic payments.events \
  --reset-offsets \
  --to-datetime 2026-07-01T09:30:00.000 \
  --execute
```

Describe after reset:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group payment-risk-v1 \
  --describe
```

Important:

- reset offsets only when the consumer group is stopped or when you fully understand active membership behavior
- always save the previous offsets before changing them
- run a dry run before execute

---

## 9. Idempotency For Replay

Replay-safe side effects require a stable idempotency key.

Common keys:

- event ID
- payment transaction ID
- order ID plus event type plus version
- source database primary key plus operation timestamp
- outbox event ID

Database pattern:

```sql
CREATE TABLE processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL,
    consumer_name VARCHAR(100) NOT NULL
);
```

Consumer logic:

```java
if (processedEvents.exists(eventId)) {
    return; // duplicate replayed event, safe to skip
}

performBusinessSideEffect(event);
processedEvents.insert(eventId, consumerName);
```

Critical nuance:

> The idempotency record must be committed atomically with the business side effect when possible.

If the consumer updates the business table and crashes before inserting `processed_events`, replay can repeat the side effect.

---

## 10. DLQ Replay

DLQ replay is not "copy DLQ back to source."

Safe process:

1. Inspect DLQ record metadata: source topic, partition, offset, error, schema.
2. Classify the error: transient, schema, validation, poison, code bug.
3. Fix the root cause.
4. Validate a small sample.
5. Reprocess to a retry topic or controlled replay topic.
6. Rate limit replay.
7. Watch downstream errors and duplicate side effects.
8. Archive replay result.

DLQ record should preserve:

- original topic
- original partition
- original offset
- original key
- original headers
- exception class
- exception message
- failure timestamp
- consumer version

---

## 11. Backfill Strategies

### Kafka Retention Backfill

Use when:

- required history still exists in Kafka
- schemas are available
- replay side effects are safe

### Source Database Backfill

Use when:

- Kafka retention is too short
- source of truth is a database
- old Kafka records are missing or corrupted

Risk:

- source database may not represent historical changes
- current database state may not reproduce event history

### Snapshot Plus Change Stream

Use when:

- building a new read model from a database
- you need initial state plus ongoing updates

Pattern:

```text
snapshot current rows -> load projection -> consume CDC from snapshot point -> catch up -> switch reads
```

### Shadow Topic Migration

Use when:

- event schema or topic design is changing
- you want validation before cutting consumers over

Pattern:

```text
old topic -> migration job -> new topic -> shadow consumer -> compare output -> cut over
```

---

## 12. Rate Limiting Replay

Replay can overload:

- brokers
- consumers
- databases
- external APIs
- search clusters
- downstream caches

Controls:

- replay only selected partitions
- cap records per second
- cap concurrent consumers
- pause/resume partitions
- use a separate replay consumer group
- write to shadow output first
- temporarily increase downstream capacity
- replay during low-traffic windows

Code sketch:

```java
for (ConsumerRecord<String, String> record : records) {
    process(record);
    limiter.acquire(); // keep replay below downstream capacity
}
```

---

## 13. Streams Reprocessing

Kafka Streams replay has extra state concerns:

- local state stores
- changelog topics
- repartition topics
- application ID
- output topics

Common approaches:

| Approach | Use Case |
|---|---|
| reset app with reset tool | rebuild state for same topology |
| new application ID | run a parallel version |
| new output topics | validate before replacing existing output |
| clear local state | recover corrupted local state |

Be careful:

- topology changes may change internal topic names or semantics
- replay can duplicate output unless output topic is isolated or compacted
- windowed aggregations depend on event time and grace periods

---

## 14. Governance Model

Every production replay request should record:

- requester
- approver
- topic
- consumer group
- offset range or timestamp range
- reason
- affected downstream systems
- data classification
- duplicate risk
- replay command or job version
- start time
- end time
- outcome
- rollback or remediation

For PII or regulated data:

- verify data minimization
- verify retention policy
- verify access permission
- avoid exporting payloads to local machines
- avoid replaying deleted or legally restricted data without review

---

## 15. Failure Modes

### Offset Reset On Wrong Group

Impact:

- wrong application reprocesses data
- duplicates or skipped records

Mitigation:

- require exact group ID
- snapshot offsets before reset
- run dry run
- use change approval

### Replaying To Non-Idempotent Consumer

Impact:

- duplicate charges, emails, or external calls

Mitigation:

- replay to shadow topic first
- add idempotency table
- disable dangerous side-effect path

### Historical Schema Missing

Impact:

- old records cannot deserialize

Mitigation:

- preserve schemas
- test historical sample before full replay
- maintain compatibility rules

### Replay Overloads Downstream

Impact:

- database or API outage

Mitigation:

- rate limit
- run off peak
- monitor SLOs
- stop when error thresholds are crossed

---

## 16. Practical Question

> A bug in the inventory consumer caused 4 hours of order events to be ignored. Kafka still has the records. How would you recover safely?

---

## 17. Strong Answer

I would recover with a governed timestamp replay:

1. Identify the exact incident window and affected topic/partitions/group.
2. Stop or isolate the broken consumer version.
3. Snapshot current offsets for rollback/audit.
4. Confirm inventory updates are idempotent by order ID and event ID.
5. Dry-run offset reset to the incident start timestamp.
6. Replay at a controlled rate.
7. Monitor consumer freshness, DLQ growth, inventory DB load, and duplicate-skip count.
8. If output can be validated separately, replay to a shadow projection first.
9. After catch-up, compare inventory counts with source-of-truth and close the audit record.

I would not blindly reset to earliest because it may replay months of orders and create unnecessary duplicate pressure. I would not replay payment/refund side effects unless idempotency is proven.

---

## 18. Revision Notes

- One-line summary: Kafka replay is operationally easy but business-risky.
- Three keywords: idempotency, dry-run, audit.
- One interview trap: saying "reset offsets" without discussing duplicate side effects.
- One memory trick: before replay, ask "what happens twice?"

---

## 19. Official Source Notes

- Apache Kafka basic operations: <https://kafka.apache.org/43/operations/basic-operations/>
- Apache Kafka consumer configs: <https://kafka.apache.org/43/generated/consumer_config.html>
- Apache Kafka Streams application reset: <https://kafka.apache.org/43/streams/developer-guide/app-reset-tool/>
- Apache Kafka Connect user guide: <https://kafka.apache.org/43/kafka-connect/userguide/>
- Apache Kafka design docs: <https://kafka.apache.org/43/design/design/>
