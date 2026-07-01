# Lab 05: Performance Incident Drills

Goal: rehearse production debugging language for Cassandra incidents.

---

## Drill 1: Hot Chat Room

Symptom:

```text
The chat API p99 jumps from 80 ms to 2 seconds for one live event room.
```

Likely cause:

- hot partition for `(room_id, bucket_day)`
- one room dominates traffic

Immediate mitigations:

- cache latest messages
- throttle or shed noncritical reads
- split room into smaller time buckets or shard suffix in next table version

Long-term fix:

```text
messages_by_room_hour or messages_by_room_day_shard, plus fan-in query logic if needed.
```

---

## Drill 2: Tombstone Storm

Symptom:

```text
Read timeouts increase after a TTL-heavy data set expires.
```

Check:

- tombstone warnings
- partition scans
- compaction backlog
- TTL pattern
- table compaction strategy

Fix:

- avoid reading expired ranges
- use time-windowed tables and TWCS for matching workloads
- reduce range deletes
- redesign retention if needed

---

## Drill 3: Stale Read

Symptom:

```text
User updates a value but immediately reads the old value from another region.
```

Check:

- read/write consistency levels
- client local DC routing
- cross-DC lag
- timeout/retry behavior
- replica health

Fix:

- use LOCAL_QUORUM in same DC for stronger local behavior
- route reads to write region when needed
- define acceptable stale-read semantics

---

## Completion Gate

For each incident, explain:

1. Most likely table/query.
2. Evidence to gather.
3. Immediate mitigation.
4. Durable data model or operations fix.
5. Alert that should catch it next time.