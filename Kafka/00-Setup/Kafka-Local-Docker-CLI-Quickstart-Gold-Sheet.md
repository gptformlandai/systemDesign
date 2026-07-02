# Kafka Local Docker And CLI Quickstart Gold Sheet

> Track: Kafka Interview Track - Setup
> Goal: make Kafka hands-on from day zero using Docker, CLI tools, topics, producers, consumers, offsets, and cleanup.

---

## 1. Why This Sheet Exists

Kafka is easier to understand after you see records move.

Most beginners read:

```text
producer -> topic -> partition -> consumer group -> offset
```

But they do not feel it until they:

- start a broker
- create a topic
- produce records
- consume records
- inspect consumer group lag
- reset offsets in a safe test topic
- delete the environment

This sheet is the first practical lab before deep theory.

---

## 2. Mental Model

Local Kafka has four visible things:

```text
broker:
  stores topic partitions and serves clients

topic:
  named log of records

producer:
  writes records to a topic

consumer group:
  reads records and commits offsets
```

Modern local Kafka can run without ZooKeeper because Kafka now supports KRaft mode.

---

## 3. Start Kafka With Docker

Official Apache Kafka Docker images exist for local development.

Simple local broker:

```bash
docker pull apache/kafka:4.3.1
docker run -p 9092:9092 --name kafka-local apache/kafka:4.3.1
```

Experimental native image for local testing:

```bash
docker pull apache/kafka-native:4.3.1
docker run -p 9092:9092 --name kafka-native-local apache/kafka-native:4.3.1
```

Important:
The native image is for local development/testing, not production.

---

## 4. Open A Shell In The Container

```bash
docker exec -it kafka-local bash
```

Most Kafka CLI scripts are available inside the container.

Common command shape:

```bash
/opt/kafka/bin/<tool>.sh --bootstrap-server localhost:9092 ...
```

If your image path differs, locate scripts:

```bash
find / -name 'kafka-topics.sh' 2>/dev/null
```

---

## 5. Create A Topic

```bash
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic orders.created.v1 \
  --partitions 3 \
  --replication-factor 1
```

Local note:
Use replication factor `1` for a single local broker.

Production note:
Critical production topics commonly use replication factor `3` and `min.insync.replicas=2`.

---

## 6. Describe The Topic

```bash
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic orders.created.v1
```

What to look for:

- partition count
- leader broker
- replicas
- ISR

Beginner translation:
Each partition is a lane. The leader receives reads/writes for that partition.

---

## 7. Produce Records

```bash
kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders.created.v1 \
  --property parse.key=true \
  --property key.separator=:
```

Then type:

```text
order-1:{"eventId":"e1","orderId":"order-1","amount":42}
order-2:{"eventId":"e2","orderId":"order-2","amount":99}
order-1:{"eventId":"e3","orderId":"order-1","amount":50}
```

Why keys matter:
Records with the same key go to the same partition, preserving order for that key.

---

## 8. Consume Records

Consume from beginning:

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders.created.v1 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=':'
```

Use a consumer group:

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders.created.v1 \
  --group order-debug-group \
  --from-beginning \
  --property print.key=true \
  --property key.separator=':'
```

Run the same command again with the same group.
You should not see old records unless offsets are reset or new records arrive.

---

## 9. Inspect Consumer Group Offsets

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-debug-group
```

Key columns:

| Column | Meaning |
|---|---|
| CURRENT-OFFSET | last committed position |
| LOG-END-OFFSET | latest position in partition |
| LAG | records not yet consumed |
| CONSUMER-ID | active consumer instance |
| HOST | where consumer is running |

Interview sentence:
Lag is per group, topic, and partition. A single group can lag while another group is fully caught up.

---

## 10. Reset Offsets In Local Only

Dry run first:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group order-debug-group \
  --topic orders.created.v1 \
  --reset-offsets \
  --to-earliest \
  --dry-run
```

Execute:

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group order-debug-group \
  --topic orders.created.v1 \
  --reset-offsets \
  --to-earliest \
  --execute
```

Production warning:
Offset reset can replay or skip work. Use it only with owner approval, idempotent consumers, and a scoped plan.

---

## 11. Topic Config Example

Create compacted local topic:

```bash
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic user.profile.current.v1 \
  --partitions 3 \
  --replication-factor 1 \
  --config cleanup.policy=compact
```

Use compacted topics for latest state by key, not full event history.

---

## 12. Clean Up

Stop container:

```bash
docker stop kafka-local
```

Remove container:

```bash
docker rm kafka-local
```

If you used named volumes, remove only when you intentionally want to delete local data.

---

## 13. Common Beginner Mistakes

| Mistake | Why It Hurts | Better |
|---|---|---|
| No key for ordered entity events | order can spread across partitions | key by aggregate ID |
| Same consumer group for debugging and app | debug consumer steals/commits app offsets | use separate debug group |
| Reset production offsets casually | duplicate/skipped side effects | dry run, approval, idempotency |
| Assume local RF=1 is production-safe | broker loss loses data | RF=3 and ISR policy |
| Treat compaction as audit history | old values can disappear | separate audit topic |

---

## 14. Mini Lab

Complete this in 30 minutes:

1. Start Kafka with Docker.
2. Create `orders.created.v1` with 3 partitions.
3. Produce 5 keyed records.
4. Consume with `order-debug-group`.
5. Describe group lag.
6. Produce 2 more records.
7. Observe lag change.
8. Reset the group to earliest in dry-run, then execute.
9. Re-consume old records.
10. Explain why this would be dangerous in production.

---

## 15. Strong Interview Answer

```text
For a beginner Kafka setup I would start a local KRaft Kafka broker with Docker,
create a topic, produce keyed records, consume them with a named consumer group,
and inspect group offsets. The important learning is that Kafka stores records in
partitioned logs, consumers track progress through committed offsets, and replay is
possible but dangerous when downstream side effects are not idempotent.
```

---

## 16. Revision Notes

- One-line summary: Local Kafka makes partitions, keys, consumer groups, and offsets visible.
- Three keywords: Docker, topic, offset.
- One interview trap: Using the same consumer group for debugging and production.
- Memory trick: Create, produce, consume, inspect, reset, clean.

---

## 17. Official Source Notes

- Apache Kafka Docker docs: https://kafka.apache.org/43/getting-started/docker/
- Apache Kafka Quick Start: https://kafka.apache.org/43/getting-started/quick-start/
