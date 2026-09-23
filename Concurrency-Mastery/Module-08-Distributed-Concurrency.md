# Module 8 — Distributed Concurrency

> **Goal:** You can pick a distributed-lock backend and defend it, name the four transaction anomalies and which isolation level prevents each, explain MVCC in Postgres in one minute, and tell the interviewer *why* Redlock alone is unsafe and how a fencing token fixes it.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────┐
                     │        MODULE 8 CORE               │
                     └────────────────────────────────────┘
                                    │
       ┌────────────────────┬───────┴─────────┬────────────────────┐
       │                    │                 │                    │
   8.1 Distributed      8.2 DB Concurrency  8.3 Distributed    8.4 Idempotency
   Locks                (Optimistic vs      Scheduling         & Exactly-Once
   (Redlock, ZK,        Pessimistic,        (ShedLock,         (fencing token +
   etcd, fencing)       MVCC, isolation)    Quartz clustered)  idempotency key)
```

---

## 8.1 Distributed Locks — Redis, ZooKeeper, etcd

### Real-world analogy

> The **single-node lock** was a bathroom door with one physical key. Everyone in the house sees the same lock.
>
> The **distributed lock** is a *promise* stored somewhere else — "You may use the printer if the Post-It on the fridge says your name and today's date." Now you have new problems the single-node lock never had:
> - What if the fridge falls over (the lock store crashes)?
> - What if you go on vacation with the key (you got GC-paused for 30 seconds)?
> - What if two people wrote their names at once (clock skew, network partition)?

Distributed locks are approximations, not the real thing. Every L5 answer starts with acknowledging this.

### The three backends

| Backend | Mechanism | Consistency guarantee | Perf | When |
|---|---|---|---|---|
| **Redis (SET NX PX + Lua)** | Optimistic key with TTL | Weak (single node); Redlock adds quorum but is still contested | Very fast (~sub-ms) | Short-lived, best-effort locks |
| **ZooKeeper (ephemeral sequential znodes)** | ZAB consensus + session ephemeral nodes | Linearizable | Slower (~ms) | Long-lived leases, election, coordination |
| **etcd (leases + revision)** | Raft consensus + lease TTL + revision # | Linearizable | Slower (~ms) | Kubernetes-native systems |

### Redis single-node lock (the naive version)

```
SET lock:orders <owner-uuid> NX PX 30000
```

- `NX` — only set if not exists.
- `PX 30000` — auto-expire in 30s (guards against holder crash).
- Release must **check owner** before deleting (Lua):

```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

Without the Lua check-and-delete, one holder's stale delete can release a lock that a **different** holder just acquired. Classic mistake.

### Redlock (multi-node Redis) — and why it's controversial

**Idea:** Acquire the lock on **N/2 + 1** independent Redis nodes within a small time window; release everywhere on any failure.

**Martin Kleppmann's critique (canonical, must know):**
- Assumes bounded clock drift and bounded process pauses. Neither holds in reality.
- A GC pause on the holder can outlast the TTL → the holder thinks it still holds; some other client already acquired; two writers proceed simultaneously.
- Fix: don't rely on time alone. Use a **fencing token** (see below).

**Interview one-liner:** *"Redlock buys you availability but not strict correctness under adversarial timing. If two writers touching the same resource is unacceptable, add a fencing token and check it at the resource."*

### ZooKeeper's ephemeral sequential znodes (the classic correct approach)

```
/lock/queue-lock-0000001   (my client)
/lock/queue-lock-0000002   (other client)
```

Algorithm:
1. Create an **ephemeral sequential** znode under `/lock/`.
2. Get children of `/lock/`, sort. If you're the lowest → you hold the lock.
3. Otherwise **watch the znode just before yours** (not the whole directory — avoids herd effect).
4. On watch fire, re-run step 2.
5. On release, delete your znode. If your session dies, the znode disappears automatically (ephemeral).

Why this is stronger:
- **Session-based**, not TTL-based: ZK's ZAB handles session expiry via heartbeats.
- **Fair**: sequence number orders waiters.
- **No thundering herd**: each waiter watches only its predecessor.

Curator library (`InterProcessMutex`) implements this correctly. Roll your own only if you must.

### etcd leases (K8s-native)

```
1. LeaseGrant   → leaseId, TTL=15s
2. Put(key, holderId, WithLease(leaseId))
3. KeepAlive(leaseId)  — heartbeats
4. On acquisition: use returned revision # as your fencing token
```

etcd's revision number is monotonic and cluster-wide unique — a natural fencing token. If a holder gets partitioned, its `KeepAlive` fails, the lease expires, the key vanishes, someone else acquires — and gets a *higher* revision. Downstream resources reject anything with a lower revision.

### Fencing tokens — the actual fix

The problem no TTL-based lock can solve alone:

```
Client A: acquire lock (TTL 30s) → GC-pauses 60s
Client B: sees TTL expired, acquires new lock
Client A: wakes up, thinks it still holds the lock, writes to storage
Client B: also writes to storage.  DATA CORRUPTION.
```

**Fencing token = a monotonically increasing number issued by the lock service on every acquisition.**

```
Client A: acquires → gets token 33
Client A pauses...
Client B: acquires → gets token 34
Client B writes: storage.write(payload, token=34) → OK; storage now knows about token 34
Client A wakes, tries: storage.write(payload, token=33) → REJECTED (33 < 34)
```

Every downstream resource (DB, file storage) must **know the current max token** and reject anything lower. This is the **only reliable defense against pause-based safety violations** in distributed locking.

### The full production ritual

```
1. Acquire the lock, receive fencingToken.
2. Include fencingToken in every write to the guarded resource.
3. Resource compares against its stored last-seen token; rejects lower.
4. On release: idempotent delete (check-owner-then-delete).
5. On timeout: assume lost; do not touch the resource unless with a fresh acquisition.
```

### When you should not reach for a distributed lock

- **You can partition the work by key** — hash to a single shard, coordinate locally. This is what Kafka consumer groups do.
- **You can use an idempotent operation** — designs like "put with version" (see MVCC below) never need the lock in the first place.
- **You only need "at most one leader"** — use a leader-election primitive (ZK, etcd, Consul) which is designed for this, not a mutex.

---

## 8.2 Database Concurrency Control

### Real-world analogy

> Two waiters share one paper reservation book.
> - **Pessimistic locking** = whoever picks it up hides it in a drawer; nobody else can even read. Safe, slow.
> - **Optimistic locking** = both write their reservation on carbon copy pages, then check at the end — "did anyone else write in row 5 since I read it?" If yes, one of them redoes their write.

Databases use *both*. Which one is right depends on contention.

### Optimistic locking (`@Version` in JPA)

```java
@Entity
class Account {
    @Id Long id;
    BigDecimal balance;
    @Version long version;   // JPA increments on every update
}

// UPDATE account SET balance=?, version=version+1 WHERE id=? AND version=?
```

- Fast: no locks held for the duration of a transaction.
- On conflict: JPA throws `OptimisticLockException` → caller retries.
- Best for **low-contention** or **read-heavy** workloads.
- **Never** use optimistic locking on hot rows (a leaderboard's top-1 counter) — you'll retry-storm.

### Pessimistic locking (`SELECT ... FOR UPDATE`)

```sql
BEGIN;
SELECT * FROM account WHERE id = 42 FOR UPDATE;   -- exclusive row lock
UPDATE account SET balance = balance - 100 WHERE id = 42;
COMMIT;
```

- Acquires a **row-level exclusive lock** for the transaction's duration.
- Other `FOR UPDATE` and writes on the row block.
- Great for **high-contention** hot rows and correctness-critical financial paths.
- **Downside:** longer critical section under load → higher latency, deadlock risk if lock ordering isn't consistent.

### `FOR UPDATE SKIP LOCKED` — the queue-in-a-table trick

```sql
BEGIN;
SELECT id, payload FROM jobs
  WHERE status = 'PENDING'
  ORDER BY created_at
  FOR UPDATE SKIP LOCKED
  LIMIT 1;
UPDATE jobs SET status = 'IN_PROGRESS' WHERE id = ?;
COMMIT;
```

- Multiple workers can safely poll — each grabs a row nobody else has locked.
- Postgres 9.5+, Oracle, SQL Server, MySQL 8. **Not** in older MySQL.
- Replaces a distributed queue for many small workloads. **This is the "poor man's job queue"** that L5 candidates should know.

### The four SQL isolation levels + anomalies

Reads left → right; higher levels prevent more anomalies at higher cost.

| Anomaly | READ UNCOMMITTED | READ COMMITTED | REPEATABLE READ | SERIALIZABLE |
|---|:---:|:---:|:---:|:---:|
| **Dirty read** — see another txn's uncommitted write | ✅ possible | ❌ prevented | ❌ | ❌ |
| **Non-repeatable read** — re-read same row, see new value | ✅ | ✅ possible | ❌ prevented | ❌ |
| **Phantom read** — re-run same query, new rows appear | ✅ | ✅ | ✅ (SQL std) / ❌ (Postgres) | ❌ prevented |
| **Write skew** — no direct conflict, but two txns break an invariant together | ✅ | ✅ | ✅ | ❌ prevented |

**Postgres note:** its `REPEATABLE READ` actually gives Snapshot Isolation → phantom reads are prevented for individual reads, but **write skew still happens**. Only `SERIALIZABLE` (SSI) prevents it. This trips a lot of candidates.

### Write skew — the interview classic

> Doctors on-call table. Two doctors, Alice and Bob. Rule: "at least one doctor must remain on call."
>
> - Both read the table under Snapshot Isolation: each sees "2 doctors on call — safe to remove me."
> - Both write: `UPDATE oncall SET on_call = false WHERE name = 'me'`.
> - Both commit. Invariant broken: zero doctors on call. No row conflict — no DB error.

The fix: `SERIALIZABLE` (Postgres SSI aborts one of them), or a `SELECT ... FOR UPDATE` that locks the whole set the invariant depends on.

### MVCC — how Postgres actually pulls this off

**MVCC = Multi-Version Concurrency Control.** Instead of locking rows for readers, each row has multiple versions tagged with transaction IDs.

Every Postgres tuple carries:
- `xmin` — the transaction ID that **created** this version.
- `xmax` — the transaction ID that **deleted** or replaced this version (0 if still live).

Rules for visibility (roughly):
- A tuple is visible to txn T if `xmin` committed **before** T's snapshot **and** either `xmax` is 0 or `xmax` committed **after** T's snapshot.
- Update = insert new tuple + set `xmax` on old.
- Delete = set `xmax`.
- Old tuples are cleaned up by **VACUUM** later.

**Consequences you should be able to explain:**
- Readers **never block writers**, writers **never block readers**. This is the killer feature vs pure 2PL.
- Long-running txns delay `VACUUM` → **table bloat**. A big Postgres perf story.
- Snapshot Isolation is what you actually get at `REPEATABLE READ`. `SERIALIZABLE` layers Serializable Snapshot Isolation (SSI) on top with conflict detection.

### 2PL vs MVCC (one-liner for the interview)

- **2PL (Two-Phase Locking)** — reads take shared locks, writes take exclusive locks. Simple; readers and writers block each other. Older systems (MSSQL default before 2005, DB2).
- **MVCC** — writers create new versions; readers see a snapshot. Postgres, Oracle, InnoDB. Higher throughput; needs vacuum/undo cleanup.

### Deadlocks

Two txns lock rows in inconsistent order:

```
T1: LOCK A → wants B
T2: LOCK B → wants A       → deadlock
```

The DB detects (Postgres/Oracle: cycle detection) and **kills one txn**. Application must retry.

**Prevention rule to state aloud:** *"Always acquire locks in a fixed, application-wide order — e.g., lowest `account_id` first — and keep transactions short."*

---

## 8.3 Distributed Scheduling

### The single problem

> "I have 5 replicas of my service. This cron job must run **exactly once**, no matter which replica wins."

### The two solutions

| Tool | Storage | How | Notes |
|---|---|---|---|
| **ShedLock** (Spring Boot) | JDBC / Redis / Mongo / DynamoDB | Wraps `@Scheduled` with an atomic `INSERT` (or `UPDATE ... WHERE lock_until < now()`); only one holder | Simplest; recommended for most Spring apps |
| **Quartz clustered mode** | JDBC | Rich cron features (triggers, misfires, calendars); scheduler nodes take turns | Heavier; use when you already have Quartz |

### The ShedLock intuition

```sql
CREATE TABLE shedlock (
  name       VARCHAR(64) PRIMARY KEY,
  lock_until TIMESTAMP,
  locked_at  TIMESTAMP,
  locked_by  VARCHAR(255)
);

-- Acquire (atomic upsert):
INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
VALUES ('daily-report', now() + interval '10 minutes', now(), 'host-A')
ON CONFLICT (name) DO UPDATE
   SET lock_until = EXCLUDED.lock_until,
       locked_at  = EXCLUDED.locked_at,
       locked_by  = EXCLUDED.locked_by
   WHERE shedlock.lock_until < now();     -- only take if expired
```

Same TTL-based limitations as Redis locks. Same fix if strict once-only matters: **fencing tokens on the resource**.

### Clock skew — the ShedLock/Quartz gotcha

- Both use wall-clock timestamps (`now()`).
- Replicas may have skewed clocks. If replica A's clock is 30s behind, it may "still hold" while B considers the lock expired → both fire.
- Mitigations:
  - Set `lock_at_most_for` **generously** vs your actual job duration.
  - Sync clocks with NTP; alert on drift > 500 ms.
  - For real correctness: use idempotent jobs + fencing tokens.

### Idempotency is the real answer

> "The job wrote 100 emails, then crashed. Did it commit before crashing? Should the next run redo the last 20?"

If your job is **idempotent** — running it twice has the same effect as once — you don't actually need "exactly once." You just need:
- **Idempotency keys** on every side effect (`INSERT ... ON CONFLICT DO NOTHING` keyed on `(job_id, item_id)`).
- **Progress markers** persisted transactionally with side effects.

This is worth saying at L5:
> *"Distributed 'exactly-once' is impossible in the general case. Distributed 'effectively-once' is a solved problem — a lock for coordination, idempotency at the destination."*

---

## 8.4 Idempotency & Exactly-Once (the L5 headline)

### The pattern

Every write-side operation carries:
1. A **client-generated idempotency key** (`X-Idempotency-Key: <uuid>`), OR
2. A **fencing token** issued by a coordinator, OR
3. Both.

The destination service stores `(key → result)` and returns the stored result on retry. Stripe's API and AWS's request-signing model both work this way.

### Cross-cutting checklist

- Retry logic must **reuse** the same idempotency key.
- The destination's `(key, result)` store must have TTL — otherwise infinite growth.
- Downstream writes triggered by an idempotent op must also carry their own key. Otherwise inner-most retries fan out.

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "Redlock — safe?" | "Yes, quorum." | "Safer than single-node Redis but not strictly correct under adversarial timing. Combine with a fencing token at the resource." |
| "Which lock backend?" | "Redis, it's fast." | "Depends on tolerance: Redis for short-lived best-effort, ZooKeeper/etcd for correctness-critical leadership. Always add fencing." |
| "SELECT FOR UPDATE hurts throughput. What else?" | "Read committed." | "Optimistic locking (`@Version`) if low contention, or `SKIP LOCKED` if it's a queue-in-a-table pattern." |
| "Difference between REPEATABLE READ and SERIALIZABLE in Postgres?" | "Just phantom prevention." | "In Postgres, RR is Snapshot Isolation — prevents phantoms on individual reads but allows write skew. SERIALIZABLE adds SSI conflict detection that aborts one of the writers." |
| "What is write skew?" | "A phantom." | "Two txns each read the same set and each modify one row so their combined effect breaks an invariant. Neither directly conflicts on any row, so RR misses it." |
| "How does Postgres avoid readers blocking writers?" | "Row locks." | "MVCC — every tuple carries xmin/xmax; readers see a snapshot. Writers create new versions. Vacuum cleans up old ones later." |
| "Exactly-once with a distributed lock?" | "Yes if TTL." | "No. Exactly-once needs idempotency at the destination. The lock gives coordination; the fencing token + idempotency key give safety." |

---

## 💻 L5-Grade Code Examples

### Example 1 — Redis lock with fencing token (Lua)

```java
// Acquire — returns fencing token or -1 if not acquired
private static final String ACQUIRE = """
    if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
        return redis.call('INCR', KEYS[2])
    else
        return -1
    end
""";

// Release — only if we still own it
private static final String RELEASE = """
    if redis.call('GET', KEYS[1]) == ARGV[1] then
        return redis.call('DEL', KEYS[1])
    else
        return 0
    end
""";

public long acquire(String key, String ownerId, Duration ttl) {
    Object result = redis.eval(ACQUIRE,
        List.of("lock:" + key, "fencing:" + key),
        List.of(ownerId, String.valueOf(ttl.toMillis())));
    return (long) result;
}

public boolean release(String key, String ownerId) {
    return ((long) redis.eval(RELEASE, List.of("lock:" + key), List.of(ownerId))) == 1;
}
```

Points to say:
- `SET NX PX` acquires atomically.
- `INCR` on a separate key issues a monotonic fencing token.
- Release checks owner before deleting — no stale unlock.
- Downstream writes must present the token; storage checks `token >= last_seen`.

### Example 2 — Optimistic locking with retry

```java
@Transactional
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    int attempts = 0;
    while (true) {
        try {
            Account from = accounts.findById(fromId).orElseThrow();
            Account to   = accounts.findById(toId).orElseThrow();
            from.balance = from.balance.subtract(amount);
            to.balance   = to.balance.add(amount);
            accounts.save(from);
            accounts.save(to);          // @Version fields drive the UPDATE ... WHERE version=?
            return;
        } catch (OptimisticLockException e) {
            if (++attempts >= 3) throw e;
            // small backoff
            Thread.sleep(50L << attempts);
        }
    }
}
```

Talk about:
- Bounded retry loop — no infinite storm.
- Exponential backoff on collision.
- If retry rate is >5%, switch to pessimistic (`SELECT FOR UPDATE`) — you're on a hot row.

### Example 3 — `SKIP LOCKED` job queue

```java
@Transactional
public Optional<Job> takeNextJob() {
    return jdbc.query("""
        SELECT id, payload FROM jobs
         WHERE status = 'PENDING'
         ORDER BY created_at
         FOR UPDATE SKIP LOCKED
         LIMIT 1
        """, this::mapJob).stream().findFirst().map(job -> {
            jdbc.update("UPDATE jobs SET status='IN_PROGRESS', locked_by=? WHERE id=?",
                        workerId, job.id());
            return job;
        });
}
```

L5 add-ons:
- Add `visibility_timeout` so a crashed worker's job is re-taken (`WHERE locked_at < now() - interval '5 minutes'`).
- Track `attempt_count`; move to `DEAD_LETTER` after N.

### Example 4 — Handling write skew with SERIALIZABLE

```java
// Postgres: session-level isolation
jdbc.execute("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE");

// The doctor-oncall check-and-write
Integer oncall = jdbc.queryForObject(
    "SELECT count(*) FROM oncall WHERE on_call = true", Integer.class);
if (oncall > 1) {
    jdbc.update("UPDATE oncall SET on_call = false WHERE doctor_id = ?", doctorId);
}
// Postgres will throw 40001 serialization_failure on the loser txn → application retries.
```

Application must **retry on 40001**. Not retrying is the classic mistake.

---

## 🏭 Production War Stories

**1. The double-charged payment (Redlock without fencing).**
Payment service used single-node Redis lock (`SET NX PX 30s`). One process GC-paused for 45s during a heap issue. TTL expired; another node acquired; both processed the charge. Refund-storm the next day. Fix: added a monotonic fencing token (Redis `INCR`), stored last-seen token in the payment aggregate, rejected lower tokens.

**2. The write-skew that leaked inventory.**
Warehouse system had a "min 1 unit reserved" rule under `REPEATABLE READ` (Postgres). Two concurrent order flows each saw 3 units, each decremented; total became −1. No error, no exception. Discovered days later during audit. Fix: switched the flow to `SERIALIZABLE`, added retry-on-40001.

**3. The `FOR UPDATE` that deadlocked nightly.**
Batch job locked `accounts` by name order in one path, by ID in another. Nightly overlap caused Postgres deadlocks. Fix: single canonical lock order (`id ASC`) across all code paths. Deadlocks dropped to near-zero.

**4. The ShedLock double-fire.**
Cluster of 5 replicas ran a nightly billing job via ShedLock (`lockAtMostFor=PT1M`). One replica's clock was skewed by 90s (misconfigured NTP). Both fired the billing job simultaneously → duplicate invoices. Fix: bumped `lockAtMostFor` to 30m, added job-level idempotency check keyed by `(customer, billing_cycle)`.

**5. The long-running txn that bloated Postgres.**
A background reconciliation job opened a Postgres transaction and ran for 3 hours. VACUUM stopped working; table bloat grew 30 GB. Query planner picked worse plans; latency spiked. Fix: broke the job into short transactions (max 5 min each), added `idle_in_transaction_session_timeout=15min` at the DB level.

---

## 🎯 Self-Check

1. Name three distributed-lock backends and one strength of each.
2. Why is Redlock alone unsafe? What does a fencing token add?
3. Draw the ZooKeeper ephemeral-sequential lock algorithm.
4. Optimistic vs pessimistic — pick criteria in ≤ 2 sentences.
5. Give an SQL example of `SELECT ... FOR UPDATE SKIP LOCKED` and one production use.
6. Four SQL anomalies and which isolation level prevents each.
7. Explain write skew with a real invariant. Which isolation level fixes it?
8. Postgres MVCC — `xmin`, `xmax`, and why VACUUM matters.
9. How does ShedLock fail under clock skew, and what saves you?
10. State the L5 mantra on exactly-once semantics.

---

## ➡️ Next

Move to **Module 9 — Production Diagnostics** (thread dumps, JFR, async-profiler, deadlock/livelock/starvation, thread-pool starvation deadlock, `ThreadLocal` leaks).
