# 39. Datadog Data Streams Monitoring: Kafka, SQS, Kinesis, RabbitMQ

## Goal

Understand Data Streams Monitoring (DSM) for event-driven systems, queue pipelines, and streaming architectures.

---

## Mental Model

APM follows one request through services.

Data Streams Monitoring follows data through producers, queues, topics, consumers, and downstream processors.

```text
producer -> topic/queue -> consumer -> processor -> downstream topic/queue
```

DSM answers: where is the pipeline slow, stuck, or broken?

---

## Why It Exists

Message systems hide latency differently than HTTP:

- Producer is fast, but consumer is behind.
- Queue depth grows silently.
- One partition is hot.
- Retries duplicate messages.
- Payload size increases and slows processing.
- Downstream consumer fails but producer still succeeds.

Traditional APM often misses end-to-end event pipeline health.

---

## Supported Pipeline Types

Common DSM targets include:

```text
Kafka
Amazon SQS
Amazon Kinesis
Amazon SNS
RabbitMQ
Google Pub/Sub
Azure Service Bus
IBM MQ
OpenTelemetry-instrumented messaging
```

Exact support depends on language, client, and instrumentation.

---

## Core Metrics

| Metric Concept | Meaning |
|---|---|
| End-to-end latency | Time from produce to final consume |
| Consumer lag | How far consumer is behind producer |
| Payload size | Size of messages moving through pipeline |
| Throughput | Messages/bytes per second |
| Error rate | Failed produce or consume operations |
| Backlog | Queued messages waiting |
| Retry/DLQ count | Failed messages needing recovery |

---

## Topology Example

```text
orders-api
  -> kafka topic: orders.created
      -> fraud-checker
          -> kafka topic: orders.approved
              -> fulfillment-worker
      -> analytics-consumer
      -> email-worker
```

DSM should show:

- Which producer emitted the event.
- Which topic/queue carried it.
- Which consumer group is slow.
- Which downstream edge has high latency.
- Where lag is accumulating.

---

## Kafka Investigation Workflow

```text
Alert:
  Consumer lag for fulfillment-worker > 10 minutes

Step 1: Open data stream topology.
Step 2: Identify topic and consumer group with lag.
Step 3: Check if lag is all partitions or one hot partition.
Step 4: Compare produce rate vs consume rate.
Step 5: Check consumer errors and processing duration.
Step 6: Inspect payload size and recent deployment version.
Step 7: Scale consumers, fix slow handler, or rebalance partitions.
```

---

## Queue Failure Patterns

| Pattern | Symptom | Likely Cause |
|---|---|---|
| Producer surge | backlog grows, consumers healthy | traffic spike |
| Consumer failure | backlog grows, error rate high | bad deploy, poison message |
| Hot partition | one partition lagging | bad partition key |
| Large payloads | latency and cost rise | schema or payload change |
| Retry storm | duplicate processing, DLQ growth | downstream dependency failure |
| Silent drop | produced count > consumed count | routing/config bug |

---

## SQS Example

```text
Monitor:
  queue age of oldest message > 300 seconds
  OR DLQ visible messages > 0

Investigation:
  - check Lambda/consumer errors
  - check concurrency throttles
  - inspect poison message payload
  - verify visibility timeout
  - verify max receive count and DLQ redrive policy
```

---

## Kinesis Example

```text
Monitor:
  iterator age > 120 seconds

Common causes:
  - consumer cannot keep up
  - shard count too low
  - hot partition key
  - downstream writes slow
```

---

## Tags To Standardize

```text
service:orders-api
env:production
version:2.4.1
team:checkout
messaging.system:kafka
messaging.destination:orders.created
messaging.operation:publish|process
consumer_group:fulfillment-worker
region:us-east-1
```

Good tags make topology and monitors usable.

---

## DSM vs APM

| Need | DSM | APM |
|---|---:|---:|
| End-to-end event pipeline latency | Yes | Partial |
| Consumer lag by queue/topic | Yes | No/partial |
| HTTP request flame graph | No | Yes |
| Producer-consumer topology | Yes | Partial |
| Method-level bottleneck inside consumer | No | Yes |

Use both: DSM finds the slow edge; APM finds slow code inside a service.

---

## Practical Question

> Orders are delayed by 20 minutes, but the orders API is healthy. The architecture uses Kafka. How do you debug?

---

## Strong Answer

I would use Data Streams Monitoring to inspect the event pipeline rather than starting with only HTTP APM. I would check the topology from producer to topic to consumer groups, then look for end-to-end latency and consumer lag. If lag is concentrated in one consumer group, I would compare consume rate to produce rate and inspect consumer errors, processing duration, recent version changes, and payload size.

If only one partition is lagging, I would suspect a hot partition key. If all partitions lag, the consumer may be under-scaled or blocked on a downstream dependency. Once DSM identifies the slow edge, I would pivot to APM for that consumer to find the code or dependency bottleneck.

---

## Interview Sound Bite

Data Streams Monitoring is APM for asynchronous pipelines. It shows producer-to-consumer topology, end-to-end latency, lag, payload size, and faulty edges across Kafka, queues, and streaming systems.
