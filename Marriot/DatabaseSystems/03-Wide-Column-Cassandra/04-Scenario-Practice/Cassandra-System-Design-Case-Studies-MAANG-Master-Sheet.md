# Cassandra System Design Case Studies - MAANG Master Sheet

> Track File #18 of 25 - Group 04: Scenario Practice
> For: backend/database/system design interviews | Level: senior / MAANG | Mode: case studies, table models, tradeoffs

This sheet builds:
- 12 Cassandra system design cases
- Table-model thinking under interview pressure
- Failure modes and alternatives for each case

---

## Case Study Template

Use this shape for every design:

```text
requirements -> access patterns -> tables -> partition/clustering keys -> consistency -> retention/compaction -> failure modes -> alternatives
```

---

## 1. Chat Message History

Access pattern: latest messages in a room.

Table:

```sql
messages_by_room_day ((room_id, bucket_day), message_ts, message_id)
```

Notes:

- bucket by day/hour for hot rooms
- cluster by timestamp descending
- use idempotent message IDs
- handle celebrity rooms with extra sharding if needed

---

## 2. IoT Metrics Store

Access pattern: metrics for device over a recent time window.

Table:

```sql
metrics_by_device_hour ((device_id, bucket_hour), metric_ts, metric_name)
```

Notes:

- TTL and TWCS likely
- export aggregates to OLAP for dashboards
- avoid scanning many buckets on every request

---

## 3. Audit Log Platform

Access pattern: list tenant audit events by day.

Table:

```sql
audit_events_by_tenant_day ((tenant_id, event_day), event_ts, event_id)
```

Notes:

- append-only
- retention policy explicit
- strong write durability expectations
- search/export handled elsewhere if needed

---

## 4. Activity Feed

Access pattern: latest feed items for user.

Table:

```sql
feed_items_by_user_week ((user_id, bucket_week), score_ts, item_id)
```

Notes:

- fan-out-on-write may fit moderate follower counts
- celebrities need hybrid/fan-out-on-read strategy
- store ranking metadata carefully

---

## 5. Notification Delivery Log

Access pattern: delivery attempts by user/day and notification ID.

Tables:

```text
notification_attempts_by_user_day
notification_by_id
```

Notes:

- retries require idempotency
- TTL old attempts
- query by notification ID needs separate table

---

## 6. Session Store

Access pattern: lookup session by session ID, list sessions by user.

Tables:

```text
session_by_id
sessions_by_user
```

Notes:

- TTL naturally fits sessions
- avoid massive per-user session partitions
- security and token revocation need careful semantics

---

## 7. Fraud Signal Timeline

Access pattern: latest signals for account/device.

Table:

```text
signals_by_account_day ((account_id, day), signal_ts, signal_id)
```

Notes:

- low-latency writes
- strong enough reads for decisioning
- aggregates may be computed in stream processor

---

## 8. Rate Limit Ledger

Access pattern: counts/events for key and window.

Table:

```text
rate_events_by_key_minute ((limit_key, minute_bucket), event_ts, request_id)
```

Notes:

- Cassandra can store event ledger
- exact counter enforcement may need Redis or specialized limiter
- TTL short windows

---

## 9. User Presence History

Access pattern: status transitions by user/day.

Table:

```text
presence_by_user_day ((user_id, day), changed_at, event_id)
```

Notes:

- latest status can be separate table
- history table can TTL
- conflict semantics need timestamp discipline

---

## 10. Multi-Region Event Ingestion

Access pattern: local writes, local reads, later global reconciliation.

Notes:

- use NetworkTopologyStrategy
- LOCAL_QUORUM per region for local correctness
- define conflict resolution and replay semantics
- monitor cross-DC lag and repair

---

## 11. API Request Log

Access pattern: tenant request history by time.

Table:

```text
api_requests_by_tenant_hour ((tenant_id, hour), request_ts, request_id)
```

Notes:

- high write volume
- TTL and TWCS
- analytics exported downstream

---

## 12. Product View History

Access pattern: recent views by user/product.

Tables:

```text
views_by_user_day
views_by_product_day
```

Notes:

- two tables because two query directions
- idempotency key avoids duplicate events
- heavy analytics belongs elsewhere

---

## Strong Case Study Answer

```text
I would use Cassandra only for the predictable high-volume access paths. For chat, I would create messages_by_room_day with room and time bucket as the partition key and message timestamp as clustering order. I would choose LOCAL_QUORUM for important reads/writes in a local DC, monitor hot rooms, partition size, tombstones, and p99 latency, and export search/analytics to specialized systems. If the product needs arbitrary message search, Cassandra alone is not enough.
```

---

## Revision Notes

- One-line summary: Cassandra design cases are won by naming access patterns and tables precisely.
- Three keywords: access pattern, bucket, failure mode.
- One interview trap: designing one table and promising it serves all queries.
- Memory trick: every new query direction deserves a new table conversation.