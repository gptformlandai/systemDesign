# Cassandra Mini Projects Portfolio

> Track File #24 of 25 - Group 06: Practice Upgrade
> For: backend/database/system design interviews | Level: beginner to pro | Mode: portfolio projects, schema design, interview discussion

Each project should include requirements, table schemas, sample CQL, read/write APIs, consistency choices, scaling concerns, security concerns, and interview talking points.

---

## 1. User Session Store

Build:

- `session_by_id`
- `sessions_by_user`
- TTL-based expiry

Discuss:

- revocation semantics
- stale reads
- token security

---

## 2. Chat Message History

Build:

- `messages_by_room_day`
- optional `message_by_id`
- latest message reads

Discuss:

- hot rooms
- bucketing
- search offload

---

## 3. IoT Telemetry Store

Build:

- `metrics_by_device_hour`
- `latest_metric_by_device`
- TTL and TWCS design

Discuss:

- write volume
- retention
- dashboard aggregation export

---

## 4. Audit Log Platform

Build:

- `audit_events_by_tenant_day`
- `audit_event_by_id`
- append-only write path

Discuss:

- retention and compliance
- immutability expectations
- backup/restore

---

## 5. Notification Delivery Tracker

Build:

- `notification_attempts_by_user_day`
- `notification_by_id`
- `failed_notifications_by_day`

Discuss:

- retry idempotency
- DLQ/replay
- TTL old attempts

---

## 6. Activity Feed Timeline

Build:

- `feed_items_by_user_week`
- `feed_item_by_id`

Discuss:

- fan-out-on-write vs fan-out-on-read
- celebrity accounts
- ranking changes

---

## 7. API Request Log

Build:

- `api_requests_by_tenant_hour`
- `api_errors_by_tenant_hour`

Discuss:

- high write volume
- TTL
- analytics export

---

## 8. Fraud Signal Store

Build:

- `signals_by_account_day`
- `signals_by_device_day`

Discuss:

- low-latency reads
- consistency choice
- stream aggregation

---

## 9. Rate Limit Event Ledger

Build:

- `rate_events_by_key_minute`
- optional materialized count table

Discuss:

- exact vs approximate counters
- Redis comparison
- TTL and bucket expiry

---

## 10. Multi-Region Event Inbox

Build:

- `events_by_region_tenant_day`
- `processed_events_by_consumer`

Discuss:

- local quorum
- replay and dedupe
- cross-region lag

---

## 11. Product View History

Build:

- `views_by_user_day`
- `views_by_product_day`

Discuss:

- duplicate event IDs
- two query directions
- analytics offload

---

## 12. Observability Sample Store

Build:

- `samples_by_service_minute`
- `latest_samples_by_service`

Discuss:

- time bucket sizing
- rollups
- p99 read path

---

## Portfolio Scoring

For each project, score 0-4:

| Area | What To Prove |
|---|---|
| Access patterns | exact reads/writes are named |
| Table design | primary keys are defensible |
| Scale | partition size and skew considered |
| Consistency | CL choice explained |
| Operations | TTL, compaction, repair, backup, metrics covered |
| Alternatives | knows when Cassandra is wrong |

MAANG-ready portfolio:

```text
At least 5 projects can be explained end-to-end in 10 minutes each with follow-up answers.
```