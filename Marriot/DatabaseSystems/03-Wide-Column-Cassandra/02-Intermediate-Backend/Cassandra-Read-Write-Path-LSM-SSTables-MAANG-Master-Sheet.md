# Cassandra Read Path, Write Path, LSM, and SSTables - MAANG Master Sheet

> Track File #7 of 25 - Group 02: Intermediate Backend
> For: backend/database/system design interviews | Level: intermediate to senior | Mode: internals, performance intuition, storage flow

This sheet builds:
- Write path and read path mental models
- Commit log, memtable, SSTables, bloom filters, compaction
- Why Cassandra is fast for writes and sensitive to reads/tombstones

---

## 1. Write Path

```text
client write
-> coordinator
-> replica nodes
-> commit log append
-> memtable update
-> acknowledge based on consistency level
-> memtable flushes to SSTable later
-> compaction merges SSTables later
```

Why writes are fast:

- append to commit log
- update memory structure
- no in-place B-tree page updates
- replicas can acknowledge based on chosen consistency level

---

## 2. Read Path

```text
client read
-> coordinator
-> replica nodes
-> check memtable
-> use bloom filters to skip SSTables
-> use partition index/summary to locate data
-> merge rows and tombstones across SSTables
-> reconcile by timestamp
-> return based on consistency level
```

Reads become expensive when:

- query scans too much of a partition
- partition is too wide
- many SSTables must be checked
- tombstones are high
- data model does not match query
- disk or cache behavior is poor

---

## 3. LSM Tree Mental Model

Cassandra uses an LSM-style storage engine.

```text
write now cheaply -> clean/merge later through compaction
```

Tradeoff:

| Gain | Cost |
|---|---|
| Very fast writes | Background compaction work |
| Sequential disk writes | Read amplification across SSTables |
| Efficient append workloads | Tombstone and compaction tuning matter |

---

## 4. SSTables

SSTables are immutable on-disk files. New writes do not update old SSTables in place.

Important consequences:

- updates create newer versions
- deletes create tombstones
- compaction eventually merges old versions
- reads may merge data from multiple SSTables

---

## 5. Bloom Filters And Indexes

Bloom filters help Cassandra avoid checking SSTables that definitely do not contain a partition.

But:

```text
Bloom filters reduce work; they do not fix a bad query model.
```

Partition index and summary help locate partitions inside SSTables.

---

## 6. Timestamp Conflict Resolution

Cassandra reconciles versions using timestamps; the latest timestamp generally wins.

Implications:

- clock behavior matters
- concurrent writes need idempotency and careful semantics
- last-write-wins may be unacceptable for some business domains
- LWT may help for compare-and-set, but it costs latency and coordination

---

## 7. Strong Answer

Question:

> Why is Cassandra write-heavy friendly but read-sensitive?

Strong answer:

```text
Cassandra writes are append-oriented: replicas append to the commit log and update memtables, then flush immutable SSTables later. That makes writes fast. Reads may need to consult memtables and multiple SSTables, merge versions and tombstones, and reconcile replica responses. So read performance depends heavily on table design, bounded partitions, low tombstone pressure, compaction health, and cache/disk behavior.
```

---

## 8. Revision Notes

- One-line summary: Cassandra pays for cheap writes with read/compaction complexity later.
- Three keywords: commit log, memtable, SSTable.
- One interview trap: saying writes are fast without mentioning compaction and read amplification.
- Memory trick: Cassandra writes now, cleans later.