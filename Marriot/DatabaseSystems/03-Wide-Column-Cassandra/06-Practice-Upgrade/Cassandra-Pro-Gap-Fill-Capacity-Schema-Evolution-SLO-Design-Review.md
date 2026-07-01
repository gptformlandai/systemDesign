# Cassandra Pro Gap Fill: Capacity, Schema Evolution, SLOs, and Design Review

> Track File #26 of 26 - Group 06: Practice Upgrade
> For: backend/database/system design interviews | Level: pro / MAANG | Mode: final gap fill, production design review, staff-level checklist

This sheet fills the advanced gaps that separate a good Cassandra learner from a production-ready Cassandra designer:

- collection, UDT, and static-column judgment
- schema evolution and table migration strategy
- capacity worksheet and partition-size estimation
- paging-state and API contract safety
- SLO, alert, and incident-review thinking
- final design-review checklist for MAANG-level answers

---

## 1. Pro Mental Model

At pro level, Cassandra design is not only table syntax.

```text
pro Cassandra design = query model + key distribution + bounded partitions + consistency math + storage lifecycle + migration path + observable SLO
```

If your design cannot explain growth, migration, repair, and incident behavior, it is not finished.

---

## 2. Collections, UDTs, and Static Columns

### Collections

Cassandra supports collections such as `list`, `set`, and `map`, but they should be used carefully.

Good uses:

- small bounded attributes
- small tag maps
- small preference sets
- metadata that is read with the parent row

Risky uses:

- unbounded comments/messages/events inside one row
- frequently updated large collections
- collection values used as a replacement for query tables
- collection tombstone-heavy delete/update patterns

Rule:

```text
Use collections only when they are bounded, owned by the row, and not a hidden unbounded child table.
```

### User-Defined Types

UDTs can make repeated structured values easier to model.

Good uses:

- stable small value objects
- address-like metadata
- structured payload fragments that are always read together

Risks:

- schema evolution can be awkward
- deeply nested structures hide query needs
- large UDTs can make rows heavy

Rule:

```text
Use UDTs for small stable value objects, not for flexible document modeling.
```

### Static Columns

Static columns store one value per partition rather than per clustering row.

Example:

```sql
CREATE TABLE messages_by_room_day (
  room_id text,
  bucket_day date,
  room_name text static,
  message_ts timestamp,
  message_id timeuuid,
  sender_id text,
  body text,
  PRIMARY KEY ((room_id, bucket_day), message_ts, message_id)
);
```

Good uses:

- small metadata shared by every row in a partition
- partition-level labels used by the same query

Risks:

- static data changes can become surprising when partition ownership changes over time
- static columns do not replace a real parent/entity table
- large static metadata makes every partition heavier to reason about

Rule:

```text
Use static columns for small partition-level metadata, not as a relational parent-table replacement.
```

---

## 3. Schema Evolution And Migration Strategy

Cassandra schema changes should be boring, planned, and reversible.

Safe patterns:

1. Add a new table for a new query pattern.
2. Dual-write old and new table.
3. Backfill in small throttled batches.
4. Shadow-read or compare sampled results.
5. Shift reads gradually.
6. Keep rollback window.
7. Retire old table only after retention, validation, and operational approval.

Avoid:

- changing primary key shape in place
- backfilling with unbounded cluster-wide scans during peak traffic
- adding new query requirements through `ALLOW FILTERING`
- relying on schema changes without deploy/runbook coordination
- changing TTL/compaction strategy without understanding tombstone and SSTable impact

Interview answer:

```text
If the access pattern changes, I usually create a new query table rather than mutate the old one into a shape it was not designed for. I dual-write, backfill carefully, validate reads, then cut over gradually.
```

---

## 4. Capacity Worksheet

Use this worksheet before saying a Cassandra table is production-ready.

### Workload Inputs

| Input | Example |
|---|---|
| peak writes/sec | 250,000 events/sec |
| peak reads/sec | 40,000 reads/sec |
| average row size | 800 bytes |
| retention | 30 days |
| replication factor | 3 |
| bucket size | 1 hour |
| hottest key multiplier | 20x normal key |
| consistency levels | LOCAL_QUORUM write/read |
| compaction strategy | TWCS |

### Storage Estimate

```text
logical_bytes = writes_per_second * row_size_bytes * retention_seconds
replicated_bytes = logical_bytes * replication_factor
planned_disk = replicated_bytes * compaction_overhead * growth_headroom
```

Use conservative multipliers for compaction overhead and growth headroom because compaction, repair, snapshots, and streaming need free disk.

### Partition Estimate

```text
rows_per_partition = writes_for_key_per_second * bucket_seconds
bytes_per_partition = rows_per_partition * average_row_size
```

Design check:

```text
If a hot key makes one partition grow too large, shrink the bucket or add another partition component.
```

Practical rule:

```text
Do not quote one universal partition-size limit as law. State the estimate, keep partitions bounded, validate with load tests, and follow the platform/team's guardrails.
```

---

## 5. Paging State And API Contracts

Cassandra drivers support paging. API design must treat paging state carefully.

Good practices:

- page within a known bounded partition
- include stable request parameters with the cursor contract
- expire externally exposed cursors
- sign/encrypt opaque cursors if exposed outside trusted services
- avoid promising random page jumps across huge datasets
- test behavior when rows are inserted/deleted between pages

Bad practices:

- exposing raw driver paging state directly as a permanent public API contract
- using pagination to hide an unbounded partition design
- letting users page through massive historical partitions interactively

Interview answer:

```text
I would use driver paging for bounded partition reads, but I would wrap paging state as an opaque short-lived cursor and avoid making it a long-term external contract.
```

---

## 6. SLO And Alert Design

Define SLOs per critical access pattern, not just cluster-wide averages.

Example:

| Access Pattern | SLO | Alerts |
|---|---|---|
| latest chat messages by room/day | p99 < 150 ms | p99, read timeouts, hot partition signals |
| ingest IoT metric | p99 write < 50 ms | write latency, dropped mutations, disk/commit log pressure |
| audit events by tenant/day | p99 < 250 ms | read latency, tombstones, compaction backlog |
| session lookup by ID | p99 < 30 ms | read latency, stale read rate, node errors |

Minimum dashboard:

- client-side latency by query/table
- server read/write latency by table
- timeouts and unavailable exceptions
- tombstone warnings
- pending compactions
- repair age/failures
- disk utilization and growth
- dropped messages/mutations
- GC pauses
- node status and uneven load

Rule:

```text
If you cannot alert by table/query shape, you will debug Cassandra too late and too broadly.
```

---

## 7. Production Readiness Review

Before approving a Cassandra design, answer these:

### Query Model

- What exact read/write paths does each table serve?
- Which queries are intentionally not supported?
- What system handles search, analytics, or joins?

### Key Design

- What is the partition key?
- What is the clustering order?
- What is the worst-case partition size?
- What is the hottest-key scenario?
- How does bucketing change over time?

### Correctness

- What consistency levels are used?
- What stale-read behavior is acceptable?
- Are writes idempotent?
- Are retries safe after timeout ambiguity?
- Is LWT needed, and what is the contention risk?

### Storage Lifecycle

- What is retention?
- Is TTL used?
- What tombstone pattern does TTL/delete create?
- Which compaction strategy fits?
- What repair schedule keeps delete safety valid?

### Operations

- What SLOs exist per query?
- What metrics prove health?
- What is the backup and restore plan?
- What is the node replacement plan?
- What is the multi-DC failover/failback plan?
- What is the schema migration plan?

---

## 8. Staff-Level Interview Answer Template

Use this when an interviewer asks for a Cassandra-backed design:

```text
I would use Cassandra only for the predictable high-volume access paths. The main query is <query>, so the table is <table>. The partition key is <key> because it routes reads directly and distributes writes; I bound it with <bucket/shard> so the hottest case is <size estimate>. Clustering is <columns> because the API needs <sort/range>. I would use <CL> because <correctness/latency reason>. Operationally, I would watch <p99/tombstones/compaction/repair/disk>, use <TTL/compaction/backup plan>, and reject Cassandra for <ad hoc query/search/transaction> by using <alternative>.
```

---

## 9. Final Pro Checklist

- I can explain collections, UDTs, and static columns without abusing them.
- I can create a migration plan for a changed access pattern.
- I can estimate partition size and storage growth.
- I can design paging without exposing fragile driver internals.
- I can define SLOs per access pattern.
- I can name alerts and runbooks for hot partitions, tombstones, stale reads, and repair backlog.
- I can reject Cassandra when the workload needs joins, ad hoc filters, global analytics, or strong relational transactions.

---

## 10. Revision Notes

- One-line summary: Pro Cassandra design proves growth, migration, observability, and failure behavior.
- Three keywords: capacity, migration, SLO.
- One interview trap: designing the table but not the operating model.
- Memory trick: Cassandra is not production-ready until the partition has a size, the query has an SLO, and the schema has a migration path.