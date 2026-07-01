# Cassandra Data Modeling, Partition Keys, and Clustering - MAANG Master Sheet

> Track File #5 of 25 - Group 02: Intermediate Backend
> For: backend/database/system design interviews | Level: intermediate to senior | Mode: query modeling, primary key design, partition sizing

This sheet builds:
- Access-pattern-first table design
- Partition key and clustering key tradeoffs
- Bucketing, denormalization, and table-per-query thinking

---

## 1. Core Principle

Cassandra data modeling starts with queries.

```text
Query -> table -> partition key -> clustering keys -> consistency -> operational cost
```

Do not start with normalized entities. Start with what the application must read and write at p50, p95, and p99.

---

## 2. Modeling Questions

Ask these before creating a table:

| Question | Example |
|---|---|
| What is the exact query? | Latest 50 messages in room R for day D |
| What is the partition key? | `(room_id, bucket_day)` |
| How large can one partition get? | Max messages per room per day |
| What order is needed? | newest first by `message_ts` |
| What is the read/write ratio? | heavy writes, frequent latest reads |
| What consistency is required? | local quorum for important reads/writes |
| What expires? | maybe TTL for temporary events |
| What failure matters? | hot celebrity room, tombstone-heavy deletes, cross-region latency |

---

## 3. Primary Key Design

```sql
PRIMARY KEY ((partition_key_part1, partition_key_part2), clustering_col1, clustering_col2)
```

| Part | Controls |
|---|---|
| Partition key | replica placement, distribution, hot-key risk, partition width |
| Clustering columns | sort order, range scans, uniqueness inside partition |

Example:

```sql
CREATE TABLE events_by_tenant_day (
  tenant_id text,
  event_day date,
  event_ts timestamp,
  event_id timeuuid,
  event_type text,
  actor_id text,
  payload text,
  PRIMARY KEY ((tenant_id, event_day), event_ts, event_id)
) WITH CLUSTERING ORDER BY (event_ts DESC, event_id DESC);
```

This table serves:

```text
List latest events for tenant T on day D.
```

It does not serve:

```text
Find all events of type PAYMENT_FAILED across all tenants this month.
```

That second query needs another table or analytics/search system.

---

## 4. Partition Key Quality

Good partition keys are:

- high enough cardinality
- evenly distributed
- aligned with common queries
- bounded in maximum partition size
- stable and deterministic
- not dominated by one hot tenant/user/device/room

Bad partition keys:

| Key | Problem |
|---|---|
| `status` | Too few values, hot partitions |
| `country` | Low cardinality and skew |
| `tenant_id` alone | One large tenant can dominate |
| `event_day` alone | All tenants for one day hit same partitions |
| random UUID only | distributes writes but cannot serve meaningful reads |

---

## 5. Bucketing Patterns

Use buckets to cap partition size.

| Workload | Bucket |
|---|---|
| chat messages | room + day/hour |
| metrics | device + day/hour |
| audit logs | tenant + day |
| feed items | user + week/day |
| rate-limit counters | key + minute |

Example:

```text
Without bucket: partition = room_id -> unbounded messages forever.
With bucket: partition = (room_id, bucket_day) -> bounded daily partition.
```

---

## 6. Denormalization

In Cassandra, duplicate data by query.

Example tables for orders:

- `order_by_id`
- `orders_by_customer_day`
- `orders_by_status_day`
- `orders_by_tenant_day`

Tradeoff:

| Gain | Cost |
|---|---|
| Fast reads for each query | Multiple writes per business event |
| No joins at read time | Consistency handled by app/idempotency |
| Predictable p99 | More storage and write amplification |

---

## 7. Strong Answer

Question:

> How do you model Cassandra tables?

Strong answer:

```text
I start from access patterns, not entities. For each important query, I design a table whose partition key targets the data directly and whose clustering keys provide the required sort or range scan. I check partition size, cardinality, skew, hot-key risk, TTL behavior, and consistency needs. If another query needs a different lookup shape, I create another denormalized table and make writes idempotent.
```

---

## 8. Revision Notes

- One-line summary: Cassandra tables are read models designed from access patterns.
- Three keywords: partition, clustering, bucketing.
- One interview trap: one normalized schema for all queries.
- Memory trick: one query shape usually deserves one table shape.