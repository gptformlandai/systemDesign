# Spring Boot Kafka Batch Advanced Operations Gold Sheet

> Track: Spring Boot Interview Track - Senior Distributed Workloads  
> Goal: deepen Spring Kafka and Spring Batch from API usage into production operations and failure handling.

Read after Messaging Kafka RabbitMQ and Spring Batch.

---

## 1. Why This Sheet Exists

Spring Boot messaging and batch systems fail in ways that simple CRUD services do not:

- consumer lag
- duplicate events
- poison messages
- rebalance storms
- failed chunk restart
- partial writer failure
- long-running job recovery
- schema compatibility issues

Strong answer:

```text
For Spring Kafka and Spring Batch, production readiness means explicit ack/retry/DLQ strategy,
idempotent processing, restartability, partitioning, metrics, and runbooks.
```

---

## 2. `@KafkaListener` Mental Model

A Spring Kafka listener is a message-handling method managed by a listener container.

Container controls:

- polling
- deserialization
- invoking listener
- acknowledgments/offset commits
- concurrency
- error handler
- retry/DLT behavior

Strong answer:

```text
@KafkaListener is only the handler. The listener container determines offset commit, error
handling, concurrency, and retry behavior.
```

---

## 3. Ack Modes

Common ack modes:

| Mode | Meaning |
|---|---|
| RECORD | commit after each record |
| BATCH | commit after batch from poll |
| TIME | commit after time interval |
| COUNT | commit after count |
| MANUAL | app acknowledges manually |
| MANUAL_IMMEDIATE | commit immediately on ack |

Interview line:

```text
Commit offsets only after processing is successful, or you risk message loss. Duplicates are
still possible, so consumers must be idempotent.
```

---

## 4. Error Handler And DLT

Modern Spring Kafka uses error handlers such as `DefaultErrorHandler`.

Typical strategy:

```text
try processing
  -> retry with backoff for transient errors
  -> send to DLT after max attempts
  -> commit/skip according to policy
```

DLT record should include:

- original topic/partition/offset
- exception class/message
- attempt count
- correlation id
- payload or reference
- timestamp

Strong answer:

```text
A DLT is not silent data loss. It needs alerting, dashboard, root-cause process, and safe
replay.
```

---

## 5. Idempotent Spring Kafka Consumer

Processed-event table:

```sql
CREATE TABLE processed_event (
  consumer_name VARCHAR(120) NOT NULL,
  event_id VARCHAR(120) NOT NULL,
  processed_at TIMESTAMP NOT NULL,
  PRIMARY KEY (consumer_name, event_id)
);
```

Listener flow:

```text
1. Start DB transaction.
2. Insert processed_event.
3. If duplicate key, return successfully.
4. Apply business side effect.
5. Commit DB transaction.
6. Acknowledge/commit offset.
```

Strong answer:

```text
Kafka delivery is commonly at-least-once. I make consumers idempotent so duplicate delivery
or replay does not duplicate side effects.
```

---

## 6. Consumer Lag Debugging

Debug path:

1. Producer rate vs consumer rate.
2. Handler processing latency.
3. Error rate and DLT volume.
4. Partition count vs listener concurrency.
5. Hot partition/key skew.
6. Rebalance frequency.
7. Downstream DB/API bottleneck.
8. JVM GC/CPU throttling.

Spring-specific metrics:

- listener processing time
- consumer records lag
- consumer commit latency
- rebalance count
- DLT publish count
- listener container state

---

## 7. Concurrency And Partitions

Spring Kafka concurrency:

```java
factory.setConcurrency(6);
```

Rule:

```text
Within one consumer group, useful parallelism is bounded by partition count.
```

If topic has 3 partitions and listener concurrency 10:

```text
Only 3 consumers actively process partitions; extra concurrency is wasted.
```

Strong answer:

```text
Scaling consumers requires matching partition strategy, listener concurrency, handler speed,
and downstream capacity.
```

---

## 8. Schema Registry And Events

For event contracts, use:

- Avro + Schema Registry
- Protobuf
- JSON Schema
- versioned JSON with compatibility tests

Rules:

- add optional fields safely
- do not remove fields consumers need
- do not change semantic meaning silently
- include event id and occurredAt
- use stable keys for ordering

Strong answer:

```text
Spring Kafka makes producing and consuming easy, but event schemas remain contracts. I use
compatibility checks to avoid breaking consumers.
```

---

## 9. Outbox With Spring

Typical flow:

```text
@Transactional service method:
  update aggregate
  insert outbox row
  commit

Outbox relay:
  read unpublished row
  publish to Kafka
  mark published
```

Spring trap:

```text
Publishing Kafka event inside a transaction does not automatically make DB commit and Kafka
publish atomic.
```

Strong answer:

```text
For reliable DB plus Kafka publishing, I use outbox or CDC. A normal @Transactional method
only controls the database transaction unless specialized transaction coordination is used.
```

---

## 10. Spring Batch Restartability

Spring Batch stores job metadata in JobRepository.

Core concepts:

- JobInstance: job name + parameters
- JobExecution: one attempt to run job
- StepExecution: one attempt for a step
- ExecutionContext: restart state

Strong answer:

```text
Restartability depends on stable JobParameters, persisted execution state, idempotent writes,
and readers/writers that can resume safely.
```

---

## 11. Chunk Processing Failure

Chunk flow:

```text
read N items -> process N items -> write N items -> commit chunk
```

If write fails:

- transaction rolls back for current chunk
- previous committed chunks remain
- restart reprocesses from last checkpoint

Design requirement:

```text
Writer must be idempotent or protected by unique keys/upserts so restart does not duplicate
side effects.
```

---

## 12. Skip, Retry, And No-Rollback

| Feature | Use |
|---|---|
| retry | transient error, same item may succeed later |
| skip | bad record should be recorded and processing continues |
| noRollback | exception should not roll back chunk transaction |
| listener | audit skipped/retried items |

Example:

```text
Bad CSV row can be skipped with audit. DB deadlock can be retried. Payment capture should
not be blindly retried without idempotency.
```

---

## 13. Batch Partitioning

Partitioning splits work across workers.

Use when:

- large dataset
- natural partitions exist
- each partition can process independently
- DB and downstream dependencies can handle concurrency

Examples:

- partition by hotel id range
- partition by date range
- partition by customer shard

Strong answer:

```text
Partitioning improves throughput only if data can be split safely and downstream systems are
not the bottleneck.
```

---

## 14. Batch Monitoring

Track:

- job status/duration
- step duration
- read/process/write counts
- skip count
- retry count
- failure reason
- restart count
- last successful run
- output/reconciliation counts

Runbook:

```text
If nightly settlement fails, check failed step, failed item, whether restart is safe, and
whether downstream side effects already occurred.
```

---

## 15. Kafka vs Batch Boundary

Use Kafka when:

- near-real-time processing
- event-driven reactions
- independent consumers
- continuous stream

Use Batch when:

- large periodic processing
- reconciliation
- reporting exports
- scheduled settlement
- controlled restart/checkpoint behavior

Strong answer:

```text
Kafka is not a replacement for batch, and batch is not a replacement for event streaming.
They solve different timing and recovery problems.
```

---

## 16. Strong Closing Answer

```text
For Spring Kafka, I design listener containers with clear ack mode, bounded retries, DLT,
idempotent consumers, schema compatibility, and lag dashboards. For Spring Batch, I design
job parameters, chunk boundaries, skip/retry policy, idempotent writers, partitioning, and
restart monitoring so failed jobs can recover without corrupting data.
```
