# 21. Scenario: Redlock Failures And Distributed Lock Safety

## Scenario

Your team uses Redlock for distributed mutual exclusion. A colleague argues it is unsafe. What are the failure modes and how do you mitigate them?

---

## What Redlock Does

```text
5 independent Redis nodes.
Algorithm per lock acquisition:
1. Get current time T1.
2. Try SET NX PX on all 5 nodes, same key, same UUID.
3. Count successful acquisitions.
4. If >= 3 acquired AND elapsed < TTL: lock is valid.
5. Actual validity = TTL - elapsed - clock-drift-margin.
6. On release: DEL on all 5 nodes (Lua check UUID first).
```

Quorum majority prevents split-brain on single-node failure.

---

## Failure Mode 1: GC Pause Or Network Partition

```text
1. Client A acquires lock on 3/5 nodes. Lock is valid for 10s.
2. Client A pauses (GC, OS scheduling) for 12 seconds.
3. Lock expires on all nodes.
4. Client B acquires lock on 3/5 nodes.
5. Client A resumes, believes it still holds the lock.
6. Both A and B are now inside the critical section.
```

This is the core unsafe scenario: Redis TTL expiry cannot guarantee exclusion against paused processes.

---

## Failure Mode 2: Clock Drift

```text
Redis node clocks drift differently.
One node expires the lock earlier than others.
Another client acquires that node's slot.
Partial quorum overlap allows two clients to hold lock.
```

Mitigation: keep clock drift within NTP bounds and use a margin in validity calculation.

---

## Failure Mode 3: Node Restart With No Persistence

```text
1. Client A acquires lock on nodes 1, 2, 3.
2. Node 3 crashes and restarts with no data.
3. Client B tries to acquire: nodes 3 (fresh), 4, 5 grant.
4. Two clients hold lock simultaneously.
```

Mitigation: enable AOF with `appendfsync always` on Redlock nodes, and configure delayed restart:

```conf
aof-use-rdb-preamble no
appendonly yes
appendfsync always
```

Or set `min-slaves-to-write` and delay node restart enough for lock TTL to expire.

---

## Fencing Token Pattern

The correct defense against GC pauses: include a monotonically increasing fencing token with every lock grant.

```text
1. Lock server returns token N.
2. Client sends token N with every write to protected resource.
3. Protected resource rejects any write with token < current max.
4. If Client A wakes from pause, its old token N is rejected.
5. Client B's token N+1 is accepted.
```

Redis does not natively provide fencing tokens. Implementation requires a separate monotonic counter.

---

## When To Use Redlock

| Use Redlock | Avoid Redlock |
|---|---|
| best-effort distributed coordination | strict single-execution guarantees |
| low-criticality mutual exclusion | financial transactions |
| fault-tolerant job scheduling | idempotency by itself is not enough |
| work deduplication (not perfect) | when fencing token pattern is not implemented |

---

## Simpler Alternative

Single-node Redis lock is sufficient for many real applications:

```bash
SET lock:job:5001 worker-uuid NX PX 30000
```

If your system can tolerate lock loss during a single Redis node failure (or you run Sentinel/Cluster HA), single-node is simpler and has fewer failure modes.

---

## Interview Sound Bite

Redlock provides distributed mutual exclusion across 5 independent Redis nodes with quorum acquisition. Its fundamental limitation is the safety gap created by GC pauses and clock drift: a process can believe it holds a lock that has expired. The fencing token pattern addresses this by having the protected resource reject stale writes. In practice, use Redlock for best-effort coordination with known failure tolerance, and implement fencing tokens when strict single-execution guarantees are required.
