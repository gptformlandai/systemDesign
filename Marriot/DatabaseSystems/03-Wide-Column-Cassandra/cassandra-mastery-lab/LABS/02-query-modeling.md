# Lab 02: Query Modeling And Primary Keys

Goal: practice translating API access patterns into Cassandra tables.

---

## Access Patterns

Design tables for these requirements:

| Requirement | Suggested Table |
|---|---|
| latest messages for a room/day | `messages_by_room_day` |
| messages sent by a user/day | `messages_by_sender_day` |
| orders for customer/day | `orders_by_customer_day` |
| orders by status/day | `orders_by_status_day` |
| exact order lookup | `order_by_id` |

---

## Exercise

For each table, write:

1. Access pattern.
2. Partition key.
3. Clustering columns.
4. Worst-case hot key.
5. Whether a time bucket is needed.
6. Why a different database might be better.

---

## Bad Query Clinic

Bad query:

```sql
SELECT * FROM orders_by_customer_day WHERE status = 'PAID' ALLOW FILTERING;
```

Fix:

```sql
SELECT *
FROM orders_by_status_day
WHERE status = 'PAID'
  AND order_day = '2026-07-01';
```

---

## Completion Gate

- You can reject `ALLOW FILTERING` for hot paths.
- You can explain why each query direction gets a table.
- You can spot low-cardinality partition keys.