# 06. Redis Pub/Sub: Channels, Patterns, Fan-out, Limitations

## Goal

Understand Redis Pub/Sub for fire-and-forget messaging, and know when to use Streams instead.

---

## How Pub/Sub Works

```text
publisher sends message to channel
Redis routes it to all current subscribers
subscribers receive or miss it
message is not stored
```

Unlike Kafka or Redis Streams, there is no persistence or replay.

---

## Basic Commands

```bash
# Subscribe to a channel.
SUBSCRIBE notifications:user:1001

# Subscribe to multiple channels.
SUBSCRIBE orders alerts maintenance

# Publish a message.
PUBLISH notifications:user:1001 '{"type":"email","body":"Your order shipped"}'

# Pattern subscribe (glob-style).
PSUBSCRIBE notifications:user:*

# Unsubscribe.
UNSUBSCRIBE notifications:user:1001
PUNSUBSCRIBE notifications:user:*
```

---

## Fan-out Pattern

```text
service publishes to: events:orders

subscribers:
  - audit-service -> events:orders
  - notification-service -> events:orders
  - analytics-service -> events:orders
```

All subscribers receive each message simultaneously. Redis does not track whether each subscriber received it.

---

## Limitations

| Limitation | Impact |
|---|---|
| no message persistence | subscriber must be connected when message is published |
| no delivery guarantee | messages are lost if subscriber disconnects or is slow |
| no acknowledgement | publisher cannot confirm delivery |
| no consumer groups | all subscribers in same channel receive all messages |
| no replay | new subscriber cannot read historical messages |
| memory unbounded | slow subscribers can back up network buffers |

---

## Pub/Sub vs Streams

| Feature | Pub/Sub | Streams |
|---|---|---|
| persistence | no | yes |
| replay | no | yes |
| consumer groups | no | yes |
| acknowledgement | no | yes |
| fan-out to all subscribers | yes | yes with multiple groups |
| message ordering | per channel order | global stream order |
| delivery guarantee | at-most-once | at-least-once |

Use Pub/Sub when:

- fire-and-forget is acceptable
- subscribers are always connected
- you need simple broadcast with no history

Use Streams when:

- messages must not be lost
- replay or consumer groups are needed
- delivery must be at-least-once

---

## Keyspace Notifications

Redis can publish keyspace events via Pub/Sub for operations like SET, DEL, EXPIRE, LPUSH.

```bash
# Enable in config.
CONFIG SET notify-keyspace-events KEA

# Subscribe to all key events on db 0.
PSUBSCRIBE __keyevent@0__:*

# Subscribe to expired events.
PSUBSCRIBE __keyevent@0__:expired
```

Use keyspace notifications for: cache invalidation signals, TTL monitoring, debug/audit. Do not use in high-throughput systems without careful testing.

---

## Interview Sound Bite

Redis Pub/Sub is fire-and-forget fan-out. It has no persistence, no delivery guarantee, and no consumer groups. Use it for low-criticality broadcasts where subscriber availability is guaranteed. Use Redis Streams when durability, replay, consumer groups, or acknowledgement are required.
