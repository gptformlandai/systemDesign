# Project 01: Chat Message History

Goal: build a Cassandra-backed chat history read model.

---

## Requirements

- Fetch latest messages for a room and day.
- Fetch messages sent by a user and day.
- Support high write throughput.
- Avoid unbounded room partitions.

---

## Tables

```text
messages_by_room_day ((room_id, bucket_day), message_ts, message_id)
messages_by_sender_day ((sender_id, bucket_day), message_ts, message_id)
```

---

## APIs

- `GET /rooms/{roomId}/messages?day=2026-07-01&limit=50`
- `GET /users/{userId}/messages?day=2026-07-01`
- `POST /rooms/{roomId}/messages`

---

## Scaling Discussion

- Hot rooms may need hour buckets or shard suffixes.
- Full-text search belongs in a search engine.
- Multi-table writes need idempotent `message_id`.

---

## Interview Talking Points

- why room/day is the partition key
- why sender/day needs a second table
- why `ALLOW FILTERING` is not acceptable
- how to handle celebrity rooms
- how to monitor p99 and partition skew