# 40. Messaging And Queue Debugging: Kafka, SQS, RabbitMQ, Celery, DLQs

## Goal

Debug asynchronous systems where producer success does not guarantee consumer success.

---

## Mental Model

Synchronous request:

```text
caller waits for callee
```

Asynchronous pipeline:

```text
producer -> broker/queue/topic -> consumer -> downstream side effect
```

Failure can hide in the gap between produce and consume.

---

## Core Evidence

For every message, capture:

```text
message_id
correlation_id / trace_id
producer service/version
topic/queue
partition/shard if applicable
consumer group
attempt count
first produced timestamp
last consumed timestamp
payload schema version
idempotency key
DLQ reason
```

---

## Symptom Map

| Symptom | Likely Cause |
|---|---|
| queue depth rising | consumers slower than producers |
| consumer lag rising | slow consumer, hot partition, downstream slow |
| DLQ growing | poison messages, schema mismatch, permanent failure |
| duplicate side effects | missing idempotency |
| messages disappear | ack/commit bug, wrong routing, retention expiry |
| retries spike | downstream dependency failure |
| only one partition lagging | bad partition key / hot key |

---

## Kafka Debugging

Check:

```bash
kafka-consumer-groups --bootstrap-server <broker> --describe --group <group>
kafka-topics --bootstrap-server <broker> --describe --topic <topic>
```

Look at:

- consumer lag by partition
- partition count
- consumer count
- rebalance frequency
- commit offset behavior
- producer error rate
- message size

Hot partition pattern:

```text
partition 0 lag = 900000
partition 1 lag = 20
partition 2 lag = 15
```

Likely cause: partition key sends too much traffic to one partition.

---

## SQS Debugging

Metrics:

```text
ApproximateNumberOfMessagesVisible
ApproximateNumberOfMessagesNotVisible
ApproximateAgeOfOldestMessage
NumberOfMessagesSent
NumberOfMessagesDeleted
DLQ visible messages
```

Check:

- visibility timeout
- max receive count
- DLQ redrive policy
- Lambda concurrency/throttles
- poison message payload
- consumer timeout vs visibility timeout

Trap:

```text
consumer takes 60s
visibility timeout = 30s
message becomes visible again while still processing
duplicate processing occurs
```

---

## RabbitMQ Debugging

Check:

- queue depth
- unacked messages
- consumer count
- prefetch count
- exchange/routing key binding
- dead-letter exchange
- memory/disk alarms

Common issue:

```text
unacked messages high + consumers alive = consumers received messages but are stuck before ack
```

---

## Celery Debugging

Useful commands:

```bash
celery -A app inspect active
celery -A app inspect reserved
celery -A app inspect scheduled
celery -A app inspect stats
```

Check:

- broker connectivity
- worker concurrency
- task time limits
- retry policy
- result backend slowness
- task serialization errors
- idempotency

Common bug:

```text
task retries forever because exception is permanent, not transient
```

---

## Poison Message Workflow

```text
1. Identify DLQ message.
2. Capture payload safely.
3. Check schema version.
4. Reproduce in staging/local consumer.
5. Determine transient vs permanent failure.
6. Patch consumer or data.
7. Redrive messages only after fix.
8. Add validation/contract test.
```

Never blindly redrive a DLQ into the same broken consumer.

---

## Idempotency

At-least-once delivery means duplicates are normal.

Use:

- idempotency key
- unique constraint
- processed-message table
- deduplication window
- compare-and-set update
- exactly-once semantics only where the platform truly supports it

Rule:

```text
Consumers must survive duplicate delivery.
```

---

## Practical Question

> Orders are accepted by the API, but confirmations arrive 30 minutes late. How do you debug?

---

## Strong Answer

I would trace the async path from producer to broker to consumer. First I would check queue depth, age of oldest message, consumer lag, DLQ depth, and consumer error rate. If backlog is rising, I would compare produce rate and consume rate. If only one partition is lagging, I would suspect a hot partition key. If DLQ is growing, I would inspect a poison message and schema version.

Then I would check consumer health: deployment version, concurrency, downstream dependency latency, retries, timeouts, and idempotency. I would mitigate by scaling consumers only if the bottleneck is consumer capacity, not if every consumer is stuck on a bad dependency.

---

## Interview Sound Bite

Queue debugging is pipeline debugging. Producer success only proves the message entered the system; you still need lag, depth, attempts, DLQ reason, consumer health, idempotency, and downstream side-effect evidence.
