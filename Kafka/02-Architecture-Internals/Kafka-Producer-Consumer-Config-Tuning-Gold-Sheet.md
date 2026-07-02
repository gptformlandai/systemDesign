# Kafka Producer And Consumer Config Tuning Gold Sheet

> Track: Kafka Interview Track - Architecture / Client Tuning
> Goal: tune Kafka clients deliberately for durability, latency, throughput, and failure behavior.

---

## 1. Why This Sheet Exists

Kafka correctness is not only architecture. It is also client configuration.

Two producers can write to the same topic with very different behavior:

```text
low-latency producer:
  smaller batches, lower linger, lower throughput

high-throughput producer:
  bigger batches, compression, more buffering, slightly more latency

durable producer:
  acks=all, idempotence, bounded retries, enough ISR
```

Two consumers can also behave differently:

```text
fast poll loop:
  small processing per poll, regular commits, stable membership

slow business consumer:
  downstream calls, max.poll.interval risk, retry/DLQ handling
```

Senior signal:
You can explain why a config exists and what trade-off it changes.

---

## 2. Producer Config Mental Model

Producer path:

```text
send()
  -> serialize
  -> choose partition
  -> append to local buffer
  -> batch records
  -> compress batch
  -> send request
  -> wait for ack
  -> retry if needed
  -> callback success/failure
```

Every producer config controls one of those steps.

---

## 3. Producer Correctness Configs

| Config | Production Meaning |
|---|---|
| `acks` | what broker durability means before success |
| `enable.idempotence` | dedupe producer retries per partition/session |
| `retries` | retry transient send failures |
| `delivery.timeout.ms` | total upper bound for send success/failure |
| `request.timeout.ms` | request-level timeout |
| `max.in.flight.requests.per.connection` | concurrent unacknowledged requests |
| `transactional.id` | enables transactional producer identity |

Common durable profile:

```properties
acks=all
enable.idempotence=true
retries=2147483647
delivery.timeout.ms=120000
request.timeout.ms=30000
```

Topic side must match:

```properties
replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
```

Trap:
`acks=all` is only meaningful when topic replication and ISR policy are sane.

---

## 4. Producer Throughput Configs

| Config | Effect |
|---|---|
| `batch.size` | max batch memory per partition |
| `linger.ms` | wait briefly to build fuller batches |
| `compression.type` | reduce network/disk bytes |
| `buffer.memory` | total memory for unsent records |
| `max.request.size` | largest request producer can send |

High-throughput profile:

```properties
batch.size=65536
linger.ms=10
compression.type=zstd
buffer.memory=67108864
```

Trade-off:
More batching improves throughput and compression ratio, but may add producer-side latency.

---

## 5. Producer Latency Configs

Lower-latency profile:

```properties
linger.ms=0
batch.size=16384
compression.type=lz4
```

But do not tune blindly.

Watch:
- producer request latency
- record queue time
- batch size average
- compression ratio
- error/retry rate
- broker produce request latency

Latency issue may be:
- broker disk/network saturation
- insufficient partitions
- DNS/TLS/auth slowness
- producer buffer pressure
- downstream callback blocking

---

## 6. Partitioning Configs

Partition choice controls ordering and load.

Rules:
- key by aggregate ID when ordering matters
- use no key only when ordering by entity does not matter
- avoid low-cardinality keys
- watch for hot keys

Producer partitioning is not a magic load balancer. If one key gets 60 percent of traffic, that key still maps to one partition.

Custom partitioners:
Use only when business routing requires it and the team can maintain it.

---

## 7. Consumer Config Mental Model

Consumer path:

```text
join group
  -> receive partition assignment
  -> poll records
  -> deserialize
  -> process side effects
  -> commit offsets
  -> heartbeat / rebalance
```

Every consumer config controls group stability, fetch behavior, or processing safety.

---

## 8. Consumer Group Stability Configs

| Config | Why It Matters |
|---|---|
| `group.id` | consumer group identity |
| `group.instance.id` | static membership identity |
| `session.timeout.ms` | how long coordinator waits before member considered dead |
| `heartbeat.interval.ms` | heartbeat frequency |
| `max.poll.interval.ms` | max time between polls before rebalance |
| `partition.assignment.strategy` | assignment and rebalance behavior |
| `group.protocol` | modern group protocol boundary in newer Kafka clients |

Rule:
If processing is slow, do not only increase `max.poll.interval.ms`. Also reduce batch size, move slow work out, use retry topics, or pause partitions.

---

## 9. Consumer Fetch Configs

| Config | Effect |
|---|---|
| `max.poll.records` | upper bound records returned per poll |
| `fetch.min.bytes` | broker waits for enough bytes before responding |
| `fetch.max.wait.ms` | max wait for fetch response |
| `max.partition.fetch.bytes` | max bytes per partition per fetch |
| `fetch.max.bytes` | max bytes per fetch request |

Low-latency consumer:

```properties
fetch.min.bytes=1
fetch.max.wait.ms=50
max.poll.records=100
```

High-throughput consumer:

```properties
fetch.min.bytes=65536
fetch.max.wait.ms=500
max.poll.records=1000
```

Trade-off:
Bigger fetches improve throughput but increase batch processing time and duplicate window.

---

## 10. Offset Commit Strategy

Options:

| Strategy | Trade-off |
|---|---|
| auto commit | simple, risky if processing fails after commit |
| commit before work | at-most-once, can lose work |
| commit after work | at-least-once, can duplicate |
| transactional offset commit | Kafka-to-Kafka exactly-once boundary |

Business default:
Commit after successful processing and make downstream side effects idempotent.

---

## 11. Tuning Profiles

Critical financial event:

```properties
producer:
  acks=all
  enable.idempotence=true
  compression.type=zstd
  delivery.timeout.ms=120000

consumer:
  enable.auto.commit=false
  max.poll.records=100
  isolation.level=read_committed if transactions are used
```

Clickstream analytics:

```properties
producer:
  linger.ms=20
  batch.size=131072
  compression.type=zstd

consumer:
  fetch.min.bytes=1048576
  fetch.max.wait.ms=500
  max.poll.records=2000
```

Low-latency notification:

```properties
producer:
  linger.ms=0
  compression.type=lz4

consumer:
  fetch.min.bytes=1
  fetch.max.wait.ms=50
```

---

## 12. Config Failure Modes

| Symptom | Likely Config Area |
|---|---|
| frequent send timeout | broker latency, `delivery.timeout.ms`, buffer pressure |
| duplicates | retries, consumer crash window, missing idempotency |
| out-of-order per key | partition key change, custom partitioner, producer assumptions |
| rebalance storm | `max.poll.interval.ms`, slow processing, group instability |
| high lag after deploy | `max.poll.records`, downstream latency, bad code path |
| memory pressure | producer `buffer.memory`, huge messages, consumer fetch sizes |
| slow historical replay | fetch settings, tiered storage, downstream throttling |

---

## 13. Tuning Workflow

1. Define goal: durability, latency, throughput, cost, or stability.
2. Measure current producer, broker, and consumer metrics.
3. Change one config family at a time.
4. Load test with realistic key distribution and payload size.
5. Validate failure behavior, not only happy-path throughput.
6. Document final profile and why each non-default exists.

Do not cargo-cult Kafka configs from another workload.

---

## 14. Strong Interview Answer

```text
I tune Kafka clients from the desired guarantee backward. For critical events I
start with acks=all, idempotent producer, RF=3, min.insync.replicas=2, and commit
offsets only after successful processing. For throughput I tune batch.size,
linger.ms, compression, and fetch sizes. For consumer stability I watch
max.poll.interval.ms, max.poll.records, heartbeat/session behavior, and rebalance
rate. I validate changes with metrics and failure tests instead of copying a
generic config profile.
```

---

## 15. Revision Notes

- One-line summary: Kafka configs turn architecture promises into real client behavior.
- Three keywords: durability, batching, polling.
- One interview trap: Thinking `acks=all` alone guarantees no data loss.
- Memory trick: Producer batches and waits; consumer polls and commits.

---

## 16. Official Source Notes

- Apache Kafka producer configs: https://kafka.apache.org/43/generated/producer_config.html
- Apache Kafka consumer configs: https://kafka.apache.org/43/generated/consumer_config.html
