# Lab 04: TTL And Tombstones

Goal: understand retention, TTL, delete markers, and tombstone risk.

---

## Run

```bash
bash SCRIPTS/run-cqlsh.sh SCRIPTS/05-ttl-tombstone-demo.cql
```

---

## What To Observe

- `session_by_id` has `default_time_to_live = 86400`.
- The demo inserts `short-session` with `USING TTL 60`.
- `TTL(user_id)` shows remaining TTL for that column.
- The delete against `sessions_by_user` creates a tombstone.

---

## Explain Out Loud

```text
Expired or deleted data is not instantly free. Cassandra stores tombstones so deletes can propagate to replicas safely. Reads over tombstone-heavy partitions can become slow until compaction can safely remove the markers.
```

---

## Design Drill

For a session store:

1. What TTL should sessions use?
2. How would mass expiry affect reads?
3. Is the table read by exact session ID or by user/day?
4. What alerts would detect TTL/tombstone trouble?

---

## Completion Gate

- You can explain TTL and tombstones.
- You can name why delete-heavy workloads are risky.
- You can connect TTL to compaction strategy.