# CQL Cheatsheet

## Keyspace

```sql
CREATE KEYSPACE app
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE app;
```

Production-style replication:

```sql
CREATE KEYSPACE app
WITH replication = {'class': 'NetworkTopologyStrategy', 'dc1': 3};
```

## Table

```sql
CREATE TABLE messages_by_room_day (
  room_id text,
  bucket_day date,
  message_ts timestamp,
  message_id uuid,
  sender_id text,
  body text,
  PRIMARY KEY ((room_id, bucket_day), message_ts, message_id)
) WITH CLUSTERING ORDER BY (message_ts DESC, message_id ASC);
```

## Query

```sql
SELECT *
FROM messages_by_room_day
WHERE room_id = 'room-1'
  AND bucket_day = '2026-07-01'
LIMIT 50;
```

## TTL

```sql
INSERT INTO session_by_id (session_id, user_id)
VALUES ('s1', 'u1') USING TTL 86400;
```

## Tracing And Consistency

```sql
CONSISTENCY ONE;
TRACING ON;
SELECT * FROM messages_by_room_day WHERE room_id = 'room-1' AND bucket_day = '2026-07-01';
TRACING OFF;
```