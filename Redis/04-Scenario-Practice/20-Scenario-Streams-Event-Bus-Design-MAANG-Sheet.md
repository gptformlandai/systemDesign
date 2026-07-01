# 20. Scenario: Build An Event Bus With Redis Streams

## Scenario

Your team wants to replace a point-to-point notification system with a shared event bus. Services need durable, at-least-once event delivery with independent consumer group offsets.

---

## Why Redis Streams Over Pub/Sub

| Feature | Pub/Sub | Streams |
|---|---|---|
| persistence | no | yes |
| consumer groups | no | yes |
| at-least-once delivery | no | yes |
| replay from offset | no | yes |
| independent service offsets | no | yes |

Streams fit when events must not be lost and multiple services consume independently.

---

## Design: Producer

```text
producer service -> XADD events:orders * ... -> Redis Stream
```

```bash
# Event format: field-value pairs.
XADD events:orders * \
  event_type order_placed \
  order_id 5001 \
  customer_id 1001 \
  total_cents 4500 \
  timestamp 1720000000000

# Cap stream to prevent unbounded growth.
XADD events:orders MAXLEN ~ 100000 * event_type order_placed order_id 5001
```

---

## Design: Consumer Groups

Each downstream service gets its own consumer group.

```bash
# Create groups (from beginning).
XGROUP CREATE events:orders notifications 0 MKSTREAM
XGROUP CREATE events:orders audit-log 0 MKSTREAM
XGROUP CREATE events:orders analytics 0 MKSTREAM
```

---

## Design: Consumer

```bash
# Read up to 10 undelivered events for this group.
XREADGROUP GROUP notifications worker-1 COUNT 10 BLOCK 2000 STREAMS events:orders >

# Process each event.
# On success: ACK.
XACK events:orders notifications 1720000000000-0

# Check pending events (unacked).
XPENDING events:orders notifications - + 100
```

---

## Design: Dead Letter Handling

```bash
# Claim events idle > 60 seconds (from failed consumer).
XAUTOCLAIM events:orders notifications worker-2 60000 0 COUNT 10

# After N retries, move to dead-letter stream.
XADD events:orders:dlq * original_id 1720000000000-0 error "processing failed 3 times" payload "..."
```

---

## Design: Event Schema

```text
field        type     description
event_type   string   order_placed, order_shipped, order_cancelled
order_id     string   domain identifier
customer_id  string   for routing notifications
total_cents  int      numeric values as strings in Redis
timestamp    long     epoch milliseconds from producer
trace_id     string   for distributed tracing correlation
```

---

## Operational Concerns

| Concern | Action |
|---|---|
| Stream memory growth | XADD with MAXLEN ~ 100000 |
| Consumer lag | monitor via XINFO GROUPS, alert on pending count |
| Slow consumer blocking slot | XAUTOCLAIM to reassign stuck entries |
| Multiple workers per group | each reads from `>`, Redis auto-distributes |
| Restart recovery | consumer reconnects, XREADGROUP with `>` resumes from group offset |

```bash
# Monitor consumer group lag.
XINFO GROUPS events:orders
# Check: lag field (entries behind)
# Check: pel-count (pending entries needing ACK)
```

---

## Failure Recovery Story

```text
1. notifications service crashes mid-processing.
2. XACK is not sent for 3 events.
3. Events stay in PEL (pending entries list).
4. After 60 seconds, XAUTOCLAIM moves them to a healthy worker.
5. Worker reprocesses and ACKs.
6. Result: at-least-once delivery.
```

---

## Interview Sound Bite

Redis Streams are a natural event bus for services that need at-least-once delivery with independent consumer group offsets. Each service group maintains its own read offset and PEL. Unacked events are reclaimed after a timeout. Trim with MAXLEN prevents unbounded memory growth. For services needing Kafka-level throughput or multi-partition parallelism, migrate to Kafka; use Streams for moderate throughput with simpler operations.
