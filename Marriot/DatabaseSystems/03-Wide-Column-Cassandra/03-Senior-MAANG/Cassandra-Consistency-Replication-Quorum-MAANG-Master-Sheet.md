# Cassandra Consistency, Replication, and Quorum - MAANG Master Sheet

> Track File #10 of 25 - Group 03: Senior / MAANG
> For: backend/database/system design interviews | Level: senior | Mode: tunable consistency, quorum math, failure behavior

This sheet builds:
- Replication and consistency-level reasoning
- Quorum math and read-your-write behavior
- Failure-mode language for senior interviews

---

## 1. Replication Basics

Replication factor controls how many copies of each partition exist.

```sql
CREATE KEYSPACE app
WITH replication = {
  'class': 'NetworkTopologyStrategy',
  'dc1': 3,
  'dc2': 3
};
```

For production, prefer `NetworkTopologyStrategy` because it understands data centers and racks.

---

## 2. Consistency Levels

| Level | Meaning | Common Use |
|---|---|---|
| ONE | one replica responds | lowest latency, weaker consistency |
| LOCAL_ONE | one local-DC replica responds | local low latency |
| QUORUM | majority of all replicas responds | stronger within RF |
| LOCAL_QUORUM | majority in local DC responds | common production default |
| EACH_QUORUM | quorum in each DC | strict multi-DC write cases, higher latency |
| ALL | all replicas respond | strongest, least available |

Common production posture:

```text
LOCAL_QUORUM writes + LOCAL_QUORUM reads in the same DC for stronger local read-your-write behavior.
```

---

## 3. Quorum Math

For replication factor 3:

```text
quorum = floor(3 / 2) + 1 = 2
```

If:

```text
write consistency + read consistency > replication factor
```

then reads and writes overlap on at least one replica, assuming no other timing/repair/clock caveats.

Example:

```text
W=2, R=2, RF=3 -> 2 + 2 > 3 -> overlap
```

---

## 4. Read-Your-Write Nuance

Read-your-write is not just a slogan. It depends on:

- same data center or cross-DC reads
- write and read consistency levels
- retry behavior
- timestamp conflict behavior
- hinted handoff and replica recovery
- client routing and load balancing policy

Strong interview phrasing:

```text
For local read-your-write, I would use LOCAL_QUORUM for both reads and writes in the same DC with RF=3, then monitor failed writes, timeouts, and replica health.
```

---

## 5. Failure Behavior

| Situation | What Happens |
|---|---|
| one replica down, CL=LOCAL_QUORUM, RF=3 | still available if two local replicas respond |
| two replicas down, CL=LOCAL_QUORUM, RF=3 | local quorum fails |
| CL=ONE read after CL=ONE write | stale read possible |
| cross-DC read before replication catches up | stale read possible |
| timeout after write sent | write may have succeeded; retry must be idempotent |

---

## 6. Hinted Handoff And Repair

Hinted handoff helps deliver missed writes to temporarily unavailable replicas.

Repair reconciles replicas over time.

Neither is a substitute for choosing correct consistency levels for correctness-critical reads/writes.

---

## 7. Strong Answer

Question:

> How does Cassandra consistency work?

Strong answer:

```text
Cassandra uses tunable consistency per operation. With RF=3, quorum is 2. If I write at LOCAL_QUORUM and read at LOCAL_QUORUM in the same DC, the read and write replica sets overlap, giving stronger local read-your-write behavior. If I use ONE, I get lower latency and higher availability but accept stale reads. I would choose consistency based on correctness, latency, failure tolerance, and whether the client reads locally or across regions.
```

---

## 8. Revision Notes

- One-line summary: Cassandra consistency is selected per read/write with quorum tradeoffs.
- Three keywords: RF, LOCAL_QUORUM, overlap.
- One interview trap: saying Cassandra is eventually consistent without explaining tunable consistency.
- Memory trick: W + R > RF gives overlap, not magic perfection.