# 07. Redis Streams: XADD, XREAD, Consumer Groups, ACK

## Goal

Use Redis Streams as a durable, append-only event log with consumer group semantics.

---

## What Redis Streams Are

Redis Streams are an append-only data structure where entries have auto-generated or provided IDs. They support consumer groups for at-least-once delivery and acknowledgement.

```text
producer -> XADD stream events -> stream stores entries
consumer reads via XREAD or via consumer group
consumer ACKs -> entry removed from pending list
```

---

## Producer: Adding Events

```bash
# Auto-generated ID (timestamp-sequence).
XADD orders:stream * customer_id 1001 total_cents 4500 status pending

# Explicit ID.
XADD orders:stream 1720000000000-0 customer_id 1001 total_cents 4500 status pending

# Capped stream (MAXLEN trims old entries).
XADD orders:stream MAXLEN ~ 10000 * customer_id 1001 total_cents 4500 status pending
```

---

## Simple Consumer: XREAD

```bash
# Read from beginning.
XREAD COUNT 100 STREAMS orders:stream 0

# Read from specific ID (exclusive).
XREAD COUNT 100 STREAMS orders:stream 1720000000000-0

# Blocking read (wait for new entries).
XREAD COUNT 10 BLOCK 5000 STREAMS orders:stream $
```

XREAD without consumer groups: all readers see all entries. Good for broadcast-style reads.

---

## Consumer Groups

Consumer groups give each group its own offset, enabling multiple services to process independently and enabling load balancing within a group.

```bash
# Create group starting from beginning.
XGROUP CREATE orders:stream order-processor $ MKSTREAM

# Create group from a specific offset.
XGROUP CREATE orders:stream order-processor 0 MKSTREAM

# Read as a specific consumer in a group.
XREADGROUP GROUP order-processor worker-1 COUNT 10 STREAMS orders:stream >

# Acknowledge processed entry.
XACK orders:stream order-processor 1720000000000-0

# Inspect pending entries.
XPENDING orders:stream order-processor - + 100
```

The `>` means: give me entries not yet delivered to any consumer in this group.

---

## Stream Entry Inspection

```bash
# Length of stream.
XLEN orders:stream

# Range read.
XRANGE orders:stream - +
XRANGE orders:stream - + COUNT 10

# Reverse range.
XREVRANGE orders:stream + - COUNT 10

# Stream info.
XINFO STREAM orders:stream
XINFO GROUPS orders:stream
XINFO CONSUMERS orders:stream order-processor
```

---

## Dead Letter And Redelivery

Entries that fail processing stay in the pending-entries list (PEL). Re-claim them for retry.

```bash
# Claim entries idle more than 60 seconds.
XAUTOCLAIM orders:stream order-processor worker-2 60000 0 COUNT 10
```

After too many retries, move to a dead-letter stream or alert.

---

## Trimming Old Entries

```bash
# Exact trim.
XTRIM orders:stream MAXLEN 50000

# Approximate trim (more efficient).
XTRIM orders:stream MAXLEN ~ 50000
```

Trim to prevent unlimited memory growth. Match retention to consumer lag and replay needs.

---

## Streams vs Kafka vs Pub/Sub

| Feature | Redis Streams | Kafka | Redis Pub/Sub |
|---|---|---|---|
| persistence | yes | yes | no |
| consumer groups | yes | yes | no |
| at-least-once | yes | yes | no |
| replay | yes | yes | no |
| partitioning | manual | automatic | no |
| throughput | high but limited | very high | high |
| operational complexity | low | high | low |

Use Streams for moderate-throughput event pipelines. Use Kafka for high-throughput multi-partition workloads.

---

## Interview Sound Bite

Redis Streams provide an append-only log with consumer groups, acknowledgement, and pending-entry management. Unlike Pub/Sub, messages are persisted. Consumer groups allow multiple independent services and load-balanced workers. Trim with MAXLEN to bound memory. XPENDING and XAUTOCLAIM handle failed-delivery recovery.
