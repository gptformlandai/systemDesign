# Module 8 — Distributed Concurrency (The Deep Dive)

> **Welcome to the Boss Level!** 🎮 Everything we learned about concurrency on a single machine? Now multiply the chaos by network partitions, clock skew, and machines that can't even agree on what time it is!

---

## 📑 Table of Contents

- [🎯 What You'll Master](#-what-youll-master)
- [🗺️ The Journey Ahead](#️-the-journey-ahead)
- [🚨 The Fundamental Truth](#-the-fundamental-truth-read-this-first)
- [Part 1: Distributed Locks — Redis, ZooKeeper, etcd](#part-1-distributed-locks--redis-zookeeper-etcd)
  - [1.1 The Analogy: From Bathroom Keys to Post-It Notes](#-the-analogy-from-bathroom-keys-to-post-it-notes)
  - [1.2 Redis Distributed Locks](#-redis-distributed-locks)
  - [1.3 ZooKeeper Distributed Locks](#-zookeeper-distributed-locks)
  - [1.4 etcd Distributed Locks](#-etcd-distributed-locks)
  - [1.5 Fencing Tokens — The Safety Net](#-fencing-tokens--the-safety-net)
  - [1.6 Comparison Table](#-comparison-table)
- [Part 2: Database Concurrency Control](#part-2-database-concurrency-control)
  - [2.1 Optimistic vs Pessimistic Locking](#-optimistic-vs-pessimistic-locking)
  - [2.2 SQL Isolation Levels](#-sql-isolation-levels)
  - [2.3 MVCC — Multi-Version Concurrency Control](#-mvcc--multi-version-concurrency-control)
  - [2.4 Write Skew](#-write-skew)
- [Part 3: Distributed Scheduling](#part-3-distributed-scheduling)
  - [3.1 ShedLock](#-shedlock)
  - [3.2 Quartz Clustered](#-quartz-clustered)
  - [3.3 Clock Skew](#-clock-skew)
- [Part 4: Idempotency & Exactly-Once Semantics](#part-4-idempotency--exactly-once-semantics)
  - [4.1 The Idempotency Key Pattern](#-the-idempotency-key-pattern)
  - [4.2 Exactly-Once Semantics](#-exactly-once-semantics)
- [Part 5: Production War Stories](#part-5-production-war-stories)
- [Part 6: Interview Traps & Self-Check](#part-6-interview-traps--self-check)

---

## 🎯 What You'll Master

By the end of this module, you'll be able to:
- Pick a distributed-lock backend and defend your choice like a pro
- Name the four transaction anomalies and which isolation level prevents each
- Explain MVCC in Postgres in under 60 seconds
- Tell the interviewer *why* Redlock alone is unsafe and how a fencing token fixes it
- Design idempotent systems that handle "exactly-once" semantics

---

## 🗺️ The Journey Ahead

```
                    ┌─────────────────────────────────────┐
                    │    DISTRIBUTED CONCURRENCY          │
                    │    "When Networks Attack!"          │
                    └─────────────────────────────────────┘
                                    │
       ┌────────────────┬───────────┴───────────┬────────────────┐
       │                │                       │                │
       ▼                ▼                       ▼                ▼
  ┌─────────┐    ┌───────────┐          ┌───────────┐    ┌───────────┐
  │ 8.1     │    │ 8.2       │          │ 8.3       │    │ 8.4       │
  │Distrib. │    │ Database  │          │ Distrib.  │    │Idempotency│
  │ Locks   │    │Concurrency│          │Scheduling │    │& Exactly- │
  │         │    │           │          │           │    │   Once    │
  └─────────┘    └───────────┘          └───────────┘    └───────────┘
       │                │                       │                │
       ▼                ▼                       ▼                ▼
   Redis, ZK,      Optimistic vs          ShedLock,        Fencing +
   etcd, fencing   Pessimistic,           Quartz           Idempotency
                   MVCC, Isolation                         Keys
```

---

## 🚨 The Fundamental Truth (Read This First!)

Before we dive in, let me share the most important insight about distributed systems:

> **"A distributed lock is not a lock. It's a *promise* that might be broken."**

On a single machine, when you hold a lock, you HOLD it. Period. The OS guarantees it.

In a distributed system? You're holding a *lease* that says "I probably have exclusive access... unless:
- The lock service crashed
- My network cable got unplugged
- I got GC-paused for 30 seconds
- The clocks disagreed about when my lease expired
- A cosmic ray flipped a bit somewhere"

**This is not pessimism. This is engineering reality.** And understanding this is what separates L5 engineers from everyone else.

---

# Part 1: Distributed Locks — Redis, ZooKeeper, etcd

## 🏠 The Analogy: From Bathroom Keys to Post-It Notes

Remember our bathroom analogy from single-machine locks?

**Single-node lock = A bathroom door with one physical key**
```
┌─────────────────────────────────────────────────────────┐
│                    YOUR HOUSE                           │
│                                                         │
│   ┌─────────┐                      ┌─────────┐         │
│   │ Thread  │ ──── wants ────────► │ 🚽      │         │
│   │   A     │                      │ BATHROOM│         │
│   └─────────┘                      │         │         │
│                                    │  🔑     │         │
│   ┌─────────┐                      │ (key is │         │
│   │ Thread  │ ──── wants ────────► │  HERE)  │         │
│   │   B     │                      └─────────┘         │
│   └─────────┘                                          │
│                                                         │
│   Everyone sees the SAME lock. Simple!                 │
└─────────────────────────────────────────────────────────┘
```

**Distributed lock = A Post-It note on a shared fridge**
```
┌─────────────────────────────────────────────────────────────────────┐
│                    THE NEIGHBORHOOD                                  │
│                                                                      │
│  ┌──────────┐        ┌──────────┐        ┌──────────┐              │
│  │ House A  │        │ House B  │        │ House C  │              │
│  │ (Server) │        │ (Server) │        │ (Server) │              │
│  └────┬─────┘        └────┬─────┘        └────┬─────┘              │
│       │                   │                   │                     │
│       │    ┌──────────────┴──────────────┐   │                     │
│       └───►│     SHARED FRIDGE           │◄──┘                     │
│            │  ┌─────────────────────┐    │                         │
│            │  │ POST-IT NOTE:       │    │                         │
│            │  │ "Server A owns the  │    │                         │
│            │  │  printer until 3pm" │    │                         │
│            │  └─────────────────────┘    │                         │
│            └─────────────────────────────┘                         │
│                                                                      │
│   NEW PROBLEMS:                                                      │
│   • What if the fridge falls over? (lock store crashes)             │
│   • What if Server A went on vacation? (GC pause for 30s)           │
│   • What if two people wrote at once? (network partition)           │
│   • What if clocks disagree about "3pm"? (clock skew)               │
└─────────────────────────────────────────────────────────────────────┘
```

**The L5 mindset:** Distributed locks are *approximations*, not guarantees. Every answer starts with acknowledging this!

---

## 🏆 The Three Lock Backends: A Tale of Trade-offs

### Quick Decision Table

```
┌─────────────────┬────────────────────┬─────────────────┬──────────────┬─────────────────────────┐
│    Backend      │    Mechanism       │   Consistency   │    Speed     │      When to Use        │
├─────────────────┼────────────────────┼─────────────────┼──────────────┼─────────────────────────┤
│ Redis           │ SET NX PX + Lua    │ Weak (single)   │ ⚡ Sub-ms    │ Short-lived, best-      │
│ (single node)   │                    │                 │              │ effort locks            │
├─────────────────┼────────────────────┼─────────────────┼──────────────┼─────────────────────────┤
│ Redis Redlock   │ Quorum across N    │ Better, but     │ ⚡ Fast      │ When you need more      │
│ (multi-node)    │ independent nodes  │ still contested │              │ availability            │
├─────────────────┼────────────────────┼─────────────────┼──────────────┼─────────────────────────┤
│ ZooKeeper       │ Ephemeral znodes   │ Linearizable    │ 🐢 ~ms      │ Long-lived leases,      │
│                 │ + ZAB consensus    │ (strong!)       │              │ leader election         │
├─────────────────┼────────────────────┼─────────────────┼──────────────┼─────────────────────────┤
│ etcd            │ Raft consensus     │ Linearizable    │ 🐢 ~ms      │ Kubernetes-native       │
│                 │ + lease TTL        │ (strong!)       │              │ systems                 │
└─────────────────┴────────────────────┴─────────────────┴──────────────┴─────────────────────────┘
```

Let's explore each one in detail!

---

## 🔴 Redis Single-Node Lock: The Fast and Furious

### The Basic Pattern

```
SET lock:orders <owner-uuid> NX PX 30000
```

Let's decode this:
- `lock:orders` — the key name (what we're locking)
- `<owner-uuid>` — who owns it (e.g., "server-a-uuid-12345")
- `NX` — **N**ot e**X**ists: only set if key doesn't exist
- `PX 30000` — auto-expire in 30,000 milliseconds (30 seconds)

### Visual: How It Works

```
Timeline: Acquiring a Redis Lock
═══════════════════════════════════════════════════════════════════

Server A tries to acquire:
┌─────────────────────────────────────────────────────────────────┐
│  SET lock:orders "server-a-uuid" NX PX 30000                    │
│                                                                  │
│  Redis checks: Does "lock:orders" exist?                        │
│                                                                  │
│  ┌─────────────┐                                                │
│  │   NO! ✓    │ ──► SET the key, return OK                     │
│  └─────────────┘     Server A now "holds" the lock              │
└─────────────────────────────────────────────────────────────────┘

Server B tries to acquire (while A holds it):
┌─────────────────────────────────────────────────────────────────┐
│  SET lock:orders "server-b-uuid" NX PX 30000                    │
│                                                                  │
│  Redis checks: Does "lock:orders" exist?                        │
│                                                                  │
│  ┌─────────────┐                                                │
│  │   YES! ✗   │ ──► Return nil (failed to acquire)             │
│  └─────────────┘     Server B must wait or retry                │
└─────────────────────────────────────────────────────────────────┘

After 30 seconds (TTL expires):
┌─────────────────────────────────────────────────────────────────┐
│  Redis automatically deletes "lock:orders"                      │
│                                                                  │
│  Now anyone can acquire it again!                               │
│  (This is the safety net if Server A crashes)                   │
└─────────────────────────────────────────────────────────────────┘
```

### ⚠️ The Classic Mistake: Unsafe Release

**WRONG way to release:**
```
DEL lock:orders    // DANGER! What if someone else owns it now?
```

**Why this is catastrophic:**
```
Timeline of Disaster:
═════════════════════════════════════════════════════════════════

t=0s    Server A acquires lock (TTL=30s)
t=25s   Server A starts processing (slow!)
t=30s   TTL expires! Lock auto-deleted by Redis
t=31s   Server B acquires lock (new TTL=30s)
t=32s   Server A finishes, runs "DEL lock:orders"
        ─────────────────────────────────────────
        💥 Server A just deleted Server B's lock!
        💥 Server C can now acquire!
        💥 Both B and C think they have exclusive access!
```

### ✅ The Safe Release: Lua Script

```lua
-- Check-and-delete atomically
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

**Why Lua?** Redis executes Lua scripts atomically. No other command can sneak in between the GET and DEL.

```
Safe Release Flow:
═════════════════════════════════════════════════════════════════

Server A tries to release:
┌─────────────────────────────────────────────────────────────────┐
│  EVAL script KEYS[1]="lock:orders" ARGV[1]="server-a-uuid"     │
│                                                                  │
│  Lua script runs ATOMICALLY:                                    │
│  1. GET lock:orders → "server-b-uuid"                          │
│  2. Compare: "server-b-uuid" == "server-a-uuid"? NO!           │
│  3. Return 0 (don't delete - it's not ours!)                   │
│                                                                  │
│  Server B's lock is SAFE! ✓                                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔴🔴🔴 Redlock: The Multi-Node Redis Lock

### The Idea

"What if we use MULTIPLE independent Redis nodes and require a QUORUM?"

```
Redlock Architecture:
═════════════════════════════════════════════════════════════════

        ┌─────────────┐
        │   Client    │
        └──────┬──────┘
               │
    ┌──────────┼──────────┬──────────┬──────────┐
    │          │          │          │          │
    ▼          ▼          ▼          ▼          ▼
┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐
│Redis 1│  │Redis 2│  │Redis 3│  │Redis 4│  │Redis 5│
│  ✓    │  │  ✓    │  │  ✓    │  │  ✗    │  │  ✗    │
└───────┘  └───────┘  └───────┘  └───────┘  └───────┘

To acquire: Must succeed on N/2 + 1 nodes (3 out of 5)
            within a small time window

Got 3 successes? You have the lock!
Got only 2? Release everywhere and retry.
```

### 🔥 Martin Kleppmann's Famous Critique (MUST KNOW for interviews!)

Martin Kleppmann wrote a famous blog post explaining why Redlock is fundamentally unsafe. Here's the scenario:

```
The GC Pause Attack on Redlock:
═════════════════════════════════════════════════════════════════

t=0s     Client A acquires Redlock (TTL=30s on all nodes)
         Client A: "I have the lock! Let me do some work..."

t=5s     Client A: *starts a long GC pause* 😴
         (Stop-the-world GC, the JVM freezes everything)

t=35s    TTL expires on all Redis nodes
         Lock is released automatically

t=36s    Client B acquires Redlock successfully
         Client B: "I have the lock! Let me write to storage..."
         Client B: *writes data*

t=40s    Client A: *wakes up from GC pause*
         Client A: "I still have the lock!" (it doesn't know!)
         Client A: *writes data*

         ╔═══════════════════════════════════════════════════╗
         ║  💥 DATA CORRUPTION! Both clients wrote!          ║
         ║                                                    ║
         ║  Client A's local state said "I have the lock"    ║
         ║  but the ACTUAL lock was gone 5 seconds ago!      ║
         ╚═══════════════════════════════════════════════════╝
```

### The Core Problem

Redlock assumes:
1. **Bounded clock drift** — all clocks tick at roughly the same rate
2. **Bounded process pauses** — no process pauses longer than TTL

**Neither assumption holds in reality!**
- GC pauses can last minutes
- VMs can be suspended and resumed
- Clocks can jump (NTP corrections, leap seconds)

### The Interview One-Liner

> *"Redlock buys you availability but not strict correctness under adversarial timing. If two writers touching the same resource is unacceptable, add a fencing token and check it at the resource."*

---

## 🟢 ZooKeeper: The Correct (But Slower) Approach

### The Ephemeral Sequential Znode Pattern

ZooKeeper uses a fundamentally different approach that's actually correct!

```
ZooKeeper Lock Structure:
═════════════════════════════════════════════════════════════════

/locks/my-resource/
    ├── lock-0000000001  (Client A - EPHEMERAL SEQUENTIAL)
    ├── lock-0000000002  (Client B - EPHEMERAL SEQUENTIAL)
    └── lock-0000000003  (Client C - EPHEMERAL SEQUENTIAL)

Rule: Whoever has the LOWEST sequence number holds the lock!
```

### The Algorithm (Step by Step)

```
ZooKeeper Lock Acquisition:
═════════════════════════════════════════════════════════════════

Step 1: Create an ephemeral sequential znode
┌─────────────────────────────────────────────────────────────────┐
│  Client A: CREATE /locks/resource/lock- (EPHEMERAL_SEQUENTIAL) │
│  ZK returns: /locks/resource/lock-0000000001                   │
└─────────────────────────────────────────────────────────────────┘

Step 2: Get all children and sort them
┌─────────────────────────────────────────────────────────────────┐
│  Client A: GETCHILDREN /locks/resource/                        │
│  Returns: [lock-0000000001]                                    │
│                                                                  │
│  Am I the lowest? YES! → I HOLD THE LOCK! ✓                    │
└─────────────────────────────────────────────────────────────────┘

Meanwhile, Client B arrives:
┌─────────────────────────────────────────────────────────────────┐
│  Client B: CREATE /locks/resource/lock- (EPHEMERAL_SEQUENTIAL) │
│  ZK returns: /locks/resource/lock-0000000002                   │
│                                                                  │
│  Client B: GETCHILDREN /locks/resource/                        │
│  Returns: [lock-0000000001, lock-0000000002]                   │
│                                                                  │
│  Am I the lowest? NO! (0000000001 < 0000000002)                │
│  → Watch the node JUST BEFORE me (lock-0000000001)             │
└─────────────────────────────────────────────────────────────────┘

Step 3: Wait for watch notification
┌─────────────────────────────────────────────────────────────────┐
│  Client A finishes, deletes lock-0000000001                    │
│  (Or Client A dies → ZK auto-deletes ephemeral node!)          │
│                                                                  │
│  ZK notifies Client B: "lock-0000000001 is gone!"              │
│                                                                  │
│  Client B re-checks: Am I lowest now? YES! → I HOLD THE LOCK!  │
└─────────────────────────────────────────────────────────────────┘
```

### Why This Is Stronger Than Redis

```
ZooKeeper vs Redis Comparison:
═════════════════════════════════════════════════════════════════

┌─────────────────────┬─────────────────────┬─────────────────────┐
│      Feature        │       Redis         │     ZooKeeper       │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ Lock expiry         │ TTL-based (time)    │ Session-based       │
│                     │ "Delete after 30s"  │ (heartbeats)        │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ Client crash        │ Wait for TTL        │ Immediate! Session  │
│ detection           │ (could be 30s!)     │ dies → node deleted │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ Fairness            │ None (race to SET)  │ FIFO (sequence #)   │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ Thundering herd     │ All clients retry   │ Each watches only   │
│                     │ simultaneously      │ its predecessor     │
├─────────────────────┼─────────────────────┼─────────────────────┤
│ Consistency         │ Weak                │ Linearizable (ZAB)  │
└─────────────────────┴─────────────────────┴─────────────────────┘
```

### The "No Thundering Herd" Trick

```
Why watch only your predecessor?
═════════════════════════════════════════════════════════════════

BAD: Everyone watches the lock holder
┌─────────────────────────────────────────────────────────────────┐
│  lock-001 (holder) ◄─── watched by ─── lock-002               │
│                     ◄─── watched by ─── lock-003               │
│                     ◄─── watched by ─── lock-004               │
│                     ◄─── watched by ─── lock-005               │
│                                                                  │
│  When lock-001 releases:                                        │
│  💥 ALL 4 clients wake up simultaneously!                       │
│  💥 All 4 check "am I lowest?" at once!                        │
│  💥 Thundering herd! Wasted work!                              │
└─────────────────────────────────────────────────────────────────┘

GOOD: Each watches only predecessor
┌─────────────────────────────────────────────────────────────────┐
│  lock-001 (holder) ◄─── watched by ─── lock-002 only          │
│  lock-002          ◄─── watched by ─── lock-003 only          │
│  lock-003          ◄─── watched by ─── lock-004 only          │
│  lock-004          ◄─── watched by ─── lock-005 only          │
│                                                                  │
│  When lock-001 releases:                                        │
│  ✓ Only lock-002 wakes up                                      │
│  ✓ lock-002 becomes holder                                     │
│  ✓ Orderly succession!                                         │
└─────────────────────────────────────────────────────────────────┘
```

### In Practice: Use Curator!

```java
// Don't roll your own! Use Apache Curator
InterProcessMutex lock = new InterProcessMutex(client, "/locks/my-resource");

try {
    if (lock.acquire(10, TimeUnit.SECONDS)) {
        try {
            // Do your critical section work
        } finally {
            lock.release();
        }
    }
} catch (Exception e) {
    // Handle acquisition failure
}
```

---

## 🔵 etcd: The Kubernetes-Native Choice

### The Pattern

```
etcd Lock Flow:
═════════════════════════════════════════════════════════════════

1. LeaseGrant → Get a lease with TTL
┌─────────────────────────────────────────────────────────────────┐
│  Request: LeaseGrant(TTL=15s)                                  │
│  Response: leaseId=12345                                       │
└─────────────────────────────────────────────────────────────────┘

2. Put the lock key with the lease attached
┌─────────────────────────────────────────────────────────────────┐
│  Request: Put("/locks/resource", "holder-id", WithLease(12345))│
│  Response: revision=98765  ← THIS IS YOUR FENCING TOKEN!       │
└─────────────────────────────────────────────────────────────────┘

3. Keep the lease alive with heartbeats
┌─────────────────────────────────────────────────────────────────┐
│  Background goroutine: KeepAlive(leaseId=12345)                │
│  Sends heartbeats every ~TTL/3 seconds                         │
│  If heartbeats stop → lease expires → key deleted              │
└─────────────────────────────────────────────────────────────────┘

4. Use revision as fencing token!
┌─────────────────────────────────────────────────────────────────┐
│  etcd's revision is:                                           │
│  • Monotonically increasing                                    │
│  • Cluster-wide unique                                         │
│  • Perfect fencing token!                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Why etcd Revision Is Special

```
etcd Revision as Natural Fencing Token:
═════════════════════════════════════════════════════════════════

Client A acquires lock → revision 100
Client A gets partitioned, lease expires
Client B acquires lock → revision 101 (always higher!)

Client A reconnects, tries to use old lock:
┌─────────────────────────────────────────────────────────────────┐
│  Storage receives: write(data, fencingToken=100)               │
│  Storage checks: 100 >= lastSeenToken(101)? NO!                │
│  Storage: REJECTED! ✓                                          │
└─────────────────────────────────────────────────────────────────┘

The revision number GUARANTEES ordering across the cluster!
```

---

## 🛡️ Fencing Tokens: The ACTUAL Fix

This is the most important concept in this entire module!

### The Problem No TTL-Based Lock Can Solve

```
The Fundamental Problem:
═════════════════════════════════════════════════════════════════

LOCAL STATE ≠ GLOBAL STATE

Your process THINKS it has the lock (local variable says so)
But the ACTUAL lock (in Redis/ZK/etcd) might have expired!

There's no way to atomically check "do I still have the lock?"
and "perform my write" across a network boundary.
```

### The Fencing Token Solution

```
Fencing Token Flow:
═════════════════════════════════════════════════════════════════

Step 1: Lock service issues monotonically increasing tokens
┌─────────────────────────────────────────────────────────────────┐
│  Client A acquires lock → receives token 33                    │
│  Client A pauses (GC, network, whatever)...                    │
│  Client B acquires lock → receives token 34                    │
│  Client B writes to storage with token 34                      │
└─────────────────────────────────────────────────────────────────┘

Step 2: Storage tracks the highest token it's seen
┌─────────────────────────────────────────────────────────────────┐
│  Storage state: lastSeenToken = 34                             │
└─────────────────────────────────────────────────────────────────┘

Step 3: Client A wakes up, tries to write
┌─────────────────────────────────────────────────────────────────┐
│  Client A: write(data, token=33)                               │
│                                                                  │
│  Storage checks: 33 >= 34? NO!                                 │
│  Storage: REJECTED! "Your token is stale"                      │
│                                                                  │
│  DATA INTEGRITY PRESERVED! ✓                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Visual Timeline

```
Fencing Token Timeline:
═════════════════════════════════════════════════════════════════

Time ──────────────────────────────────────────────────────────►

Client A:  [acquire]────[token=33]────[GC PAUSE 😴]────[wake]────[write token=33]
                                                                        │
                                                                        ▼
                                                                   REJECTED!
                                                                   (33 < 34)

Client B:            [acquire]────[token=34]────[write token=34]────►
                                                        │
                                                        ▼
                                                   ACCEPTED!
                                                   lastSeen=34

Storage:   [empty]──────────────────────────────[lastSeen=34]────────────────►
```

### The Full Production Ritual

```java
// The complete safe pattern:

// 1. Acquire lock, get fencing token
long fencingToken = lockService.acquire("my-resource");
if (fencingToken < 0) {
    throw new LockNotAcquiredException();
}

try {
    // 2. Include fencing token in EVERY write
    storage.write(data, fencingToken);  // Storage validates!
    
    // 3. If storage rejects (token too old), abort
    // The storage.write() should throw if token < lastSeen
    
} finally {
    // 4. Release with owner check
    lockService.release("my-resource", ownerId);
}

// 5. If timeout/error: assume lost, don't touch resource
//    without fresh acquisition
```

### What the Storage Must Do

```java
// Storage side - MUST validate fencing tokens!
public class FencedStorage {
    private final AtomicLong lastSeenToken = new AtomicLong(0);
    
    public void write(Data data, long fencingToken) {
        // Atomically check-and-update
        long current = lastSeenToken.get();
        if (fencingToken < current) {
            throw new StaleFencingTokenException(
                "Token " + fencingToken + " < current " + current);
        }
        
        // Update last seen (CAS to handle concurrent writes)
        while (!lastSeenToken.compareAndSet(current, fencingToken)) {
            current = lastSeenToken.get();
            if (fencingToken < current) {
                throw new StaleFencingTokenException(...);
            }
        }
        
        // Now safe to write
        actualStorage.write(data);
    }
}
```

---

## 🚫 When NOT to Use Distributed Locks

Sometimes the best lock is no lock at all!

### Alternative 1: Partition by Key

```
Instead of locking, partition the work:
═════════════════════════════════════════════════════════════════

BAD: All workers fight for one lock
┌─────────────────────────────────────────────────────────────────┐
│  Worker A ──┐                                                   │
│  Worker B ──┼──► [LOCK] ──► Process all orders                 │
│  Worker C ──┘                                                   │
│                                                                  │
│  Contention! Bottleneck! Sadness!                              │
└─────────────────────────────────────────────────────────────────┘

GOOD: Hash to partition, coordinate locally
┌─────────────────────────────────────────────────────────────────┐
│  Order 1 (hash=0) ──► Worker A (owns partition 0)              │
│  Order 2 (hash=1) ──► Worker B (owns partition 1)              │
│  Order 3 (hash=0) ──► Worker A (owns partition 0)              │
│  Order 4 (hash=2) ──► Worker C (owns partition 2)              │
│                                                                  │
│  No distributed lock needed! Kafka consumer groups do this!    │
└─────────────────────────────────────────────────────────────────┘
```

### Alternative 2: Idempotent Operations

```
Instead of "lock then write", use "write with version":
═════════════════════════════════════════════════════════════════

-- Optimistic locking with version
UPDATE accounts 
SET balance = balance - 100, version = version + 1
WHERE id = 42 AND version = 5;

-- If version changed, 0 rows affected → retry with fresh read
-- No distributed lock needed!
```

### Alternative 3: Leader Election (Not Mutex)

```
If you need "at most one leader", use a leader election primitive:
═════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────┐
│  ZooKeeper: Ephemeral node for leader                          │
│  etcd: Lease-based leader election                             │
│  Consul: Session-based leader election                         │
│                                                                  │
│  These are DESIGNED for "one active leader" semantics          │
│  Don't abuse a mutex for this!                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

# Part 2: Database Concurrency Control

## 🍽️ The Analogy: Two Waiters, One Reservation Book

Imagine a busy restaurant with two waiters sharing one paper reservation book.

**Pessimistic Locking = Hide the book in a drawer**
```
Waiter A: *grabs book, puts in drawer*
Waiter A: "Nobody touch this until I'm done!"

Waiter B: "I need to check a reservation..."
Waiter B: *waits*

SAFE but SLOW - everyone waits
```

**Optimistic Locking = Carbon copy pages**
```
Waiter A: *takes carbon copy of page 5*
Waiter B: *takes carbon copy of page 5*

Both work independently...

Waiter A: "Done! Let me update the master..."
Waiter A: *checks* "Page 5 unchanged? Yes!" *writes*

Waiter B: "Done! Let me update the master..."
Waiter B: *checks* "Page 5 unchanged? NO! Someone wrote!"
Waiter B: "Ugh, I need to redo my work..."

FAST but may need RETRIES
```

**Databases use BOTH!** The right choice depends on contention level.

---

## 🔓 Optimistic Locking: Trust But Verify

### The JPA @Version Pattern

```java
@Entity
public class Account {
    @Id 
    private Long id;
    
    private BigDecimal balance;
    
    @Version 
    private long version;  // JPA manages this automatically!
}
```

### What Happens Under the Hood

```sql
-- When you call accountRepository.save(account):

UPDATE account 
SET balance = 500.00, 
    version = 6           -- Increment version
WHERE id = 42 
  AND version = 5;        -- Only if version matches!

-- If 0 rows updated -> someone else changed it -> OptimisticLockException!
```

### When to Use Optimistic Locking

- Fast: no locks held for transaction duration
- On conflict: JPA throws `OptimisticLockException`, caller retries
- Best for **low-contention** or **read-heavy** workloads
- **NEVER** use on hot rows (leaderboard counters) - you'll retry-storm!

**Rule of thumb:** If retry rate > 5%, switch to pessimistic!

---

## 🔒 Pessimistic Locking: SELECT ... FOR UPDATE

### The Pattern

```sql
BEGIN;

-- Acquire exclusive row lock
SELECT * FROM account WHERE id = 42 FOR UPDATE;

-- Now we have exclusive access - do our work
UPDATE account SET balance = balance - 100 WHERE id = 42;

COMMIT;  -- Lock released
```

- Acquires a **row-level exclusive lock** for the transaction's duration
- Other `FOR UPDATE` and writes on the row block
- Great for **high-contention** hot rows and correctness-critical paths
- **Downside:** longer critical section = higher latency, deadlock risk

---

## 🎯 SKIP LOCKED: The Queue-in-a-Table Trick

This is a **MUST KNOW** pattern for L5 interviews!

```sql
BEGIN;

SELECT id, payload FROM jobs
WHERE status = 'PENDING'
ORDER BY created_at
FOR UPDATE SKIP LOCKED  -- Magic sauce!
LIMIT 1;

UPDATE jobs SET status = 'IN_PROGRESS', locked_by = 'worker-a' 
WHERE id = ?;

COMMIT;
```

**What SKIP LOCKED does:**
- Multiple workers can safely poll
- Each grabs a row nobody else has locked
- Skips over locked rows instead of waiting

**The "Poor Man's Job Queue"** - replaces Kafka/RabbitMQ for many workloads!

Available in: Postgres 9.5+, Oracle, SQL Server, MySQL 8+

---

## 🎭 The Four SQL Anomalies (Interview Gold!)

Understanding these anomalies is CRITICAL for L5 interviews.

### The Anomalies Table

```
┌──────────────────────┬────────────┬────────────┬────────────┬──────────────┐
│      Anomaly         │    READ    │    READ    │ REPEATABLE │ SERIALIZABLE │
│                      │ UNCOMMITTED│  COMMITTED │    READ    │              │
├──────────────────────┼────────────┼────────────┼────────────┼──────────────┤
│ Dirty Read           │  Possible  │  Prevented │  Prevented │   Prevented  │
│ (see uncommitted)    │     ✗      │     ✓      │     ✓      │      ✓       │
├──────────────────────┼────────────┼────────────┼────────────┼──────────────┤
│ Non-repeatable Read  │  Possible  │  Possible  │  Prevented │   Prevented  │
│ (row changes)        │     ✗      │     ✗      │     ✓      │      ✓       │
├──────────────────────┼────────────┼────────────┼────────────┼──────────────┤
│ Phantom Read         │  Possible  │  Possible  │  Possible* │   Prevented  │
│ (new rows appear)    │     ✗      │     ✗      │     ✗      │      ✓       │
├──────────────────────┼────────────┼────────────┼────────────┼──────────────┤
│ Write Skew           │  Possible  │  Possible  │  Possible  │   Prevented  │
│ (invariant broken)   │     ✗      │     ✗      │     ✗      │      ✓       │
└──────────────────────┴────────────┴────────────┴────────────┴──────────────┘

* Postgres RR actually prevents phantom reads but NOT write skew!
```

### Anomaly 1: Dirty Read

```
Dirty Read: Seeing uncommitted data
═══════════════════════════════════════════════════════════════

Transaction A                    Transaction B
─────────────                    ─────────────
BEGIN                            BEGIN

UPDATE account 
SET balance = 0 
WHERE id = 42;
(balance was 1000)
                                 SELECT balance FROM account
                                 WHERE id = 42;
                                 --> Returns 0 (UNCOMMITTED!)

ROLLBACK;
(balance back to 1000)
                                 -- Transaction B made decisions
                                 -- based on data that NEVER EXISTED!
```

**Fix:** Use READ COMMITTED or higher (default in most DBs)

### Anomaly 2: Non-Repeatable Read

```
Non-Repeatable Read: Same query, different results
═══════════════════════════════════════════════════════════════

Transaction A                    Transaction B
─────────────                    ─────────────
BEGIN                            BEGIN

SELECT balance FROM account
WHERE id = 42;
--> Returns 1000
                                 UPDATE account 
                                 SET balance = 500
                                 WHERE id = 42;
                                 COMMIT;

SELECT balance FROM account
WHERE id = 42;
--> Returns 500 (DIFFERENT!)

-- Same query, same transaction, different result!
```

**Fix:** Use REPEATABLE READ or higher

### Anomaly 3: Phantom Read

```
Phantom Read: New rows appear mid-transaction
═══════════════════════════════════════════════════════════════

Transaction A                    Transaction B
─────────────                    ─────────────
BEGIN                            BEGIN

SELECT COUNT(*) FROM orders
WHERE status = 'PENDING';
--> Returns 5
                                 INSERT INTO orders (status)
                                 VALUES ('PENDING');
                                 COMMIT;

SELECT COUNT(*) FROM orders
WHERE status = 'PENDING';
--> Returns 6 (PHANTOM ROW!)

-- A new row "appeared" that wasn't there before!
```

**Fix:** Use SERIALIZABLE (or Postgres REPEATABLE READ)

### Anomaly 4: Write Skew (THE INTERVIEW CLASSIC!)

This is the trickiest one and interviewers LOVE it!

```
Write Skew: Two transactions break an invariant together
═══════════════════════════════════════════════════════════════

SCENARIO: Hospital on-call system
RULE: At least one doctor must always be on call

Initial state:
┌─────────┬─────────┐
│ Doctor  │ On Call │
├─────────┼─────────┤
│ Alice   │  true   │
│ Bob     │  true   │
└─────────┴─────────┘

Transaction A (Alice)            Transaction B (Bob)
─────────────────────            ───────────────────
BEGIN                            BEGIN

SELECT COUNT(*) FROM oncall      SELECT COUNT(*) FROM oncall
WHERE on_call = true;            WHERE on_call = true;
--> 2 doctors on call            --> 2 doctors on call
--> "Safe to remove myself!"     --> "Safe to remove myself!"

UPDATE oncall                    UPDATE oncall
SET on_call = false              SET on_call = false
WHERE doctor = 'Alice';          WHERE doctor = 'Bob';

COMMIT;                          COMMIT;

RESULT:
┌─────────┬─────────┐
│ Doctor  │ On Call │
├─────────┼─────────┤
│ Alice   │  false  │
│ Bob     │  false  │
└─────────┴─────────┘

INVARIANT BROKEN! Zero doctors on call!
No row conflict - no DB error - silent corruption!
```

**Why this is tricky:**
- Neither transaction modified the same row
- No conflict detected by the database
- Both transactions saw valid state and made valid decisions
- Combined effect breaks the invariant

**Fix:** Use SERIALIZABLE (Postgres SSI aborts one transaction)

```sql
-- Or use SELECT FOR UPDATE to lock the whole set:
SELECT * FROM oncall WHERE on_call = true FOR UPDATE;
-- Now the second transaction blocks until the first commits
```

---

## 🔮 MVCC: How Postgres Actually Works

**MVCC = Multi-Version Concurrency Control**

Instead of locking rows for readers, Postgres keeps multiple versions!

### The Tuple Structure

```
Every Postgres row (tuple) carries hidden columns:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                     POSTGRES TUPLE                          │
├──────────┬──────────┬───────────────────────────────────────┤
│   xmin   │   xmax   │          Actual Data                  │
│  (who    │  (who    │   (id, name, balance, etc.)          │
│ created) │ deleted) │                                       │
└──────────┴──────────┴───────────────────────────────────────┘

xmin = Transaction ID that CREATED this version
xmax = Transaction ID that DELETED/REPLACED this version (0 if live)
```

### How Updates Work

```
UPDATE creates a NEW tuple, marks OLD tuple as deleted:
═══════════════════════════════════════════════════════════════

Before UPDATE (balance = 1000):
┌──────────┬──────────┬─────────────────┐
│ xmin=100 │ xmax=0   │ balance = 1000  │  <-- Live tuple
└──────────┴──────────┴─────────────────┘

Transaction 200 runs: UPDATE account SET balance = 500

After UPDATE:
┌──────────┬──────────┬─────────────────┐
│ xmin=100 │ xmax=200 │ balance = 1000  │  <-- Dead (replaced by 200)
└──────────┴──────────┴─────────────────┘
┌──────────┬──────────┬─────────────────┐
│ xmin=200 │ xmax=0   │ balance = 500   │  <-- Live tuple
└──────────┴──────────┴─────────────────┘

Both versions exist! Different transactions see different versions!
```

### Visibility Rules (Simplified)

```
Is tuple visible to transaction T?
═══════════════════════════════════════════════════════════════

A tuple is visible to transaction T if:

1. xmin committed BEFORE T's snapshot started
   AND
2. Either:
   - xmax = 0 (never deleted), OR
   - xmax committed AFTER T's snapshot started

In plain English:
"I can see rows that existed when I started,
 and haven't been deleted by transactions I can see"
```

### The Killer Feature

```
MVCC's Superpower:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   READERS NEVER BLOCK WRITERS                              │
│   WRITERS NEVER BLOCK READERS                              │
│                                                             │
│   This is why Postgres can handle massive read loads       │
│   while writes are happening!                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Compare to 2PL (Two-Phase Locking):
- Readers take shared locks
- Writers take exclusive locks
- Readers and writers BLOCK each other
- Much lower throughput!
```

### The VACUUM Problem

```
The Dark Side of MVCC:
═══════════════════════════════════════════════════════════════

Old tuple versions pile up!

Transaction 100: Creates tuple
Transaction 200: Updates tuple (old version now dead)
Transaction 300: Updates tuple (another dead version)
Transaction 400: Updates tuple (another dead version)
...

Dead tuples accumulate --> TABLE BLOAT!

VACUUM's job: Clean up dead tuples that NO transaction can see anymore

BUT: Long-running transactions BLOCK VACUUM!
     (They might still need to see old versions)

PRODUCTION LESSON:
- Keep transactions SHORT
- Set idle_in_transaction_session_timeout
- Monitor table bloat
- A 3-hour transaction can bloat your DB by gigabytes!
```

### 2PL vs MVCC One-Liner

> **2PL:** Reads take shared locks, writes take exclusive locks. Simple but readers/writers block each other.
>
> **MVCC:** Writers create new versions, readers see snapshots. Higher throughput but needs VACUUM cleanup.

---

## 💀 Deadlocks

Two transactions lock resources in inconsistent order:

```
Classic Deadlock:
═══════════════════════════════════════════════════════════════

Transaction 1                    Transaction 2
─────────────                    ─────────────
LOCK row A                       LOCK row B
   ✓ acquired                       ✓ acquired

LOCK row B                       LOCK row A
   ⏳ waiting for T2...            ⏳ waiting for T1...

   ╔═══════════════════════════════════════════════════════╗
   ║  DEADLOCK! Both waiting for each other forever!       ║
   ║                                                        ║
   ║  Database detects this and KILLS one transaction      ║
   ║  Application must RETRY the killed transaction        ║
   ╚═══════════════════════════════════════════════════════╝
```

### Prevention Rule (Say This in Interviews!)

> *"Always acquire locks in a fixed, application-wide order - e.g., lowest account_id first - and keep transactions short."*

```java
// GOOD: Consistent lock ordering
public void transfer(Long fromId, Long toId, BigDecimal amount) {
    // Always lock lower ID first!
    Long firstId = Math.min(fromId, toId);
    Long secondId = Math.max(fromId, toId);
    
    Account first = accountRepo.findByIdForUpdate(firstId);
    Account second = accountRepo.findByIdForUpdate(secondId);
    
    // Now do the transfer...
}
```

---

# Part 3: Distributed Scheduling

## 🎯 The Single Problem

> "I have 5 replicas of my service. This cron job must run **exactly once**, no matter which replica wins."

```
The Problem Visualized:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                    KUBERNETES CLUSTER                       │
│                                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ Pod 1   │  │ Pod 2   │  │ Pod 3   │  │ Pod 4   │       │
│  │ @Sched  │  │ @Sched  │  │ @Sched  │  │ @Sched  │       │
│  │ 9:00 AM │  │ 9:00 AM │  │ 9:00 AM │  │ 9:00 AM │       │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘       │
│       │            │            │            │             │
│       └────────────┴─────┬──────┴────────────┘             │
│                          │                                  │
│                          ▼                                  │
│                   ┌─────────────┐                          │
│                   │ DAILY JOB   │                          │
│                   │ (send bills)│                          │
│                   └─────────────┘                          │
│                                                             │
│  WITHOUT coordination: 4 pods = 4x billing emails! 💥      │
│  WITH coordination: exactly 1 pod runs the job ✓           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔧 Solution 1: ShedLock (Recommended for Spring Boot)

### How It Works

```
ShedLock uses a database table as a distributed lock:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                    shedlock table                           │
├──────────────┬─────────────────────┬────────────┬──────────┤
│     name     │     lock_until      │ locked_at  │locked_by │
├──────────────┼─────────────────────┼────────────┼──────────┤
│ daily-report │ 2024-01-15 09:10:00 │ 09:00:00   │ pod-1    │
│ hourly-sync  │ 2024-01-15 09:05:00 │ 09:00:00   │ pod-3    │
└──────────────┴─────────────────────┴────────────┴──────────┘

Pod 1 at 9:00: "Can I run daily-report?"
  --> Check: lock_until < now()? YES (expired or never run)
  --> Atomic upsert: set lock_until = now() + 10 minutes
  --> SUCCESS! Run the job.

Pod 2 at 9:00: "Can I run daily-report?"
  --> Check: lock_until < now()? NO (pod-1 just set it!)
  --> SKIP! Don't run.
```

### The SQL Behind ShedLock

```sql
CREATE TABLE shedlock (
  name       VARCHAR(64) PRIMARY KEY,
  lock_until TIMESTAMP NOT NULL,
  locked_at  TIMESTAMP NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);

-- Atomic acquire (only succeeds if lock expired):
INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
VALUES ('daily-report', NOW() + INTERVAL '10 minutes', NOW(), 'pod-1')
ON CONFLICT (name) DO UPDATE
   SET lock_until = EXCLUDED.lock_until,
       locked_at  = EXCLUDED.locked_at,
       locked_by  = EXCLUDED.locked_by
   WHERE shedlock.lock_until < NOW();  -- Only if expired!
```

### Spring Boot Integration

```java
// 1. Add dependency
// implementation 'net.javacrumbs.shedlock:shedlock-spring:5.x.x'
// implementation 'net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.x.x'

// 2. Enable ShedLock
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
@EnableScheduling
@Configuration
public class SchedulerConfig {
    
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}

// 3. Use on your scheduled methods
@Component
public class BillingJob {
    
    @Scheduled(cron = "0 0 9 * * *")  // 9 AM daily
    @SchedulerLock(
        name = "daily-billing",
        lockAtMostFor = "PT1H",    // Hold lock max 1 hour
        lockAtLeastFor = "PT5M"   // Hold lock min 5 minutes
    )
    public void runDailyBilling() {
        // Only ONE pod runs this!
        billingService.processAllPendingBills();
    }
}
```

### ⚠️ The Clock Skew Gotcha

```
Clock Skew Problem:
═══════════════════════════════════════════════════════════════

Pod 1 clock: 9:00:00 (correct)
Pod 2 clock: 9:01:30 (90 seconds ahead - bad NTP!)

Scenario with lockAtMostFor = 1 minute:

t=9:00:00 (real time)
  Pod 1: Acquires lock, sets lock_until = 9:01:00
  Pod 1: Starts running job...

t=9:00:30 (real time)
  Pod 2 thinks it's 9:02:00!
  Pod 2: Checks lock_until (9:01:00) < now (9:02:00)? YES!
  Pod 2: Acquires lock! Starts running job!

  💥 BOTH PODS RUNNING THE SAME JOB!
```

**Mitigations:**
1. Set `lockAtMostFor` **generously** (much longer than job duration)
2. Sync clocks with NTP; alert on drift > 500ms
3. For real correctness: make jobs **idempotent**!

---

## 🔧 Solution 2: Quartz Clustered Mode

For more complex scheduling needs (triggers, misfires, calendars).

```
Quartz Cluster Architecture:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                    QUARTZ TABLES (JDBC)                     │
│                                                             │
│  QRTZ_TRIGGERS    - What jobs to run and when              │
│  QRTZ_JOB_DETAILS - Job definitions                        │
│  QRTZ_LOCKS       - Cluster coordination                   │
│  QRTZ_FIRED_TRIGGERS - Currently executing                 │
└─────────────────────────────────────────────────────────────┘
           │
           │ All nodes share these tables
           │
    ┌──────┴──────┬──────────────┐
    │             │              │
┌───▼───┐    ┌───▼───┐     ┌───▼───┐
│ Node 1│    │ Node 2│     │ Node 3│
│Quartz │    │Quartz │     │Quartz │
└───────┘    └───────┘     └───────┘

Nodes compete for triggers using row-level locks.
Only one node fires each trigger.
```

**Use Quartz when:**
- You need complex cron expressions
- You need misfire handling (what if the job was supposed to run but didn't?)
- You need job chaining and dependencies
- You already have Quartz in your stack

**Use ShedLock when:**
- Simple "run this once" semantics
- You want minimal infrastructure
- Spring Boot is your stack

---

## 🎯 The Real Answer: Idempotency

Here's the L5 insight that most candidates miss:

> *"Distributed 'exactly-once' is impossible in the general case. Distributed 'effectively-once' is a solved problem - a lock for coordination, idempotency at the destination."*

```
The Idempotent Job Pattern:
═══════════════════════════════════════════════════════════════

Instead of: "Make sure only one pod runs this"
Think:      "Make sure running it twice has the same effect as once"

@Scheduled(cron = "0 0 9 * * *")
@SchedulerLock(name = "daily-billing")
public void runDailyBilling() {
    LocalDate today = LocalDate.now();
    
    for (Customer customer : customerRepo.findPendingBilling()) {
        // Idempotency key = (customer_id, billing_date)
        String idempotencyKey = customer.getId() + "-" + today;
        
        // Only process if not already done
        if (!billingRepo.existsByIdempotencyKey(idempotencyKey)) {
            Bill bill = createBill(customer, today);
            bill.setIdempotencyKey(idempotencyKey);
            billingRepo.save(bill);  // Unique constraint on idempotency_key
        }
    }
}
```

**Why this is better:**
- Lock gives you coordination (usually only one runs)
- Idempotency gives you safety (if two run, no harm done)
- Belt AND suspenders!

---

# Part 4: Idempotency & Exactly-Once

## 🎯 The Pattern

Every write-side operation carries:
1. A **client-generated idempotency key** (`X-Idempotency-Key: <uuid>`), OR
2. A **fencing token** issued by a coordinator, OR
3. **Both** (belt and suspenders!)

```
Idempotency Flow:
═══════════════════════════════════════════════════════════════

Request 1: POST /payments {amount: 100, idempotency_key: "abc-123"}
┌─────────────────────────────────────────────────────────────┐
│  Server checks: Have I seen "abc-123" before?               │
│  --> NO                                                     │
│  Server: Process payment, store (abc-123 -> result)        │
│  Response: 200 OK {payment_id: 456}                        │
└─────────────────────────────────────────────────────────────┘

Request 2 (retry): POST /payments {amount: 100, idempotency_key: "abc-123"}
┌─────────────────────────────────────────────────────────────┐
│  Server checks: Have I seen "abc-123" before?               │
│  --> YES! Found stored result                              │
│  Server: Return stored result (don't process again!)       │
│  Response: 200 OK {payment_id: 456}  (same as before)      │
└─────────────────────────────────────────────────────────────┘

Customer charged exactly once, even with retries!
```

### The Stripe Model

Stripe's API is the gold standard for idempotency:

```bash
curl https://api.stripe.com/v1/charges \
  -H "Idempotency-Key: unique-key-per-request" \
  -d amount=2000 \
  -d currency=usd
```

- Same idempotency key = same response (cached for 24 hours)
- Different key = new charge
- No key = no idempotency protection

### Implementation Checklist

```
Idempotency Checklist:
═══════════════════════════════════════════════════════════════

✓ Retry logic must REUSE the same idempotency key
  (Don't generate a new UUID on each retry!)

✓ The (key -> result) store must have TTL
  (Otherwise infinite growth)

✓ Downstream writes must carry their own keys
  (Idempotency doesn't automatically propagate)

✓ Consider what "same result" means
  (Return cached response? Re-fetch current state?)
```

### Code Example: Idempotent Payment Service

```java
@Service
public class PaymentService {
    
    private final IdempotencyStore idempotencyStore;
    private final PaymentGateway gateway;
    
    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        String key = request.getIdempotencyKey();
        
        // 1. Check if we've seen this request before
        Optional<PaymentResult> cached = idempotencyStore.get(key);
        if (cached.isPresent()) {
            log.info("Returning cached result for key: {}", key);
            return cached.get();
        }
        
        // 2. Process the payment
        PaymentResult result = gateway.charge(
            request.getAmount(),
            request.getCustomerId()
        );
        
        // 3. Store the result (with TTL)
        idempotencyStore.put(key, result, Duration.ofHours(24));
        
        return result;
    }
}

// The idempotency store (Redis implementation)
@Component
public class RedisIdempotencyStore implements IdempotencyStore {
    
    private final RedisTemplate<String, PaymentResult> redis;
    
    public void put(String key, PaymentResult result, Duration ttl) {
        redis.opsForValue().set(
            "idempotency:" + key, 
            result, 
            ttl
        );
    }
    
    public Optional<PaymentResult> get(String key) {
        return Optional.ofNullable(
            redis.opsForValue().get("idempotency:" + key)
        );
    }
}
```

---

# Part 5: Production War Stories

## 💥 War Story 1: The Double-Charged Payment (Redlock Without Fencing)

**The Setup:**
- Payment service used single-node Redis lock (`SET NX PX 30s`)
- Lock protected the "charge customer" operation

**What Happened:**
```
Timeline of Disaster:
═══════════════════════════════════════════════════════════════

t=0s     Process A acquires lock (TTL=30s)
t=5s     Process A starts charging customer...
t=10s    Process A hits a MASSIVE GC pause (heap issue)
         *Process A is frozen*

t=35s    TTL expires! Lock auto-deleted
t=36s    Process B acquires lock
t=37s    Process B charges customer $500 ✓

t=45s    Process A wakes up from GC pause
         Process A thinks it still has the lock!
         Process A charges customer $500 AGAIN! 💥

Customer charged $1000 instead of $500!
Refund-storm the next day.
```

**The Fix:**
- Added monotonic fencing token (Redis `INCR`)
- Payment aggregate stores `lastSeenToken`
- Rejects any charge with token < lastSeenToken

---

## 💥 War Story 2: The Write-Skew That Leaked Inventory

**The Setup:**
- Warehouse system with "min 1 unit reserved" rule
- Using Postgres `REPEATABLE READ` isolation

**What Happened:**
```
The Invisible Bug:
═══════════════════════════════════════════════════════════════

Initial: 3 units in stock, rule says keep at least 1

Order Flow A                     Order Flow B
────────────                     ────────────
BEGIN (RR)                       BEGIN (RR)

SELECT stock FROM inventory;     SELECT stock FROM inventory;
--> 3 units                      --> 3 units

"3 > 1, safe to sell 2"          "3 > 1, safe to sell 2"

UPDATE inventory                 UPDATE inventory
SET stock = stock - 2;           SET stock = stock - 2;

COMMIT;                          COMMIT;

Final stock: 3 - 2 - 2 = -1 units! 💥

No error, no exception, no conflict detected.
Discovered days later during audit.
```

**The Fix:**
- Switched critical path to `SERIALIZABLE`
- Added retry-on-40001 (serialization failure)
- One transaction now aborts, retries, sees correct stock

---

## 💥 War Story 3: The FOR UPDATE That Deadlocked Nightly

**The Setup:**
- Batch job locked accounts for reconciliation
- Multiple code paths acquired locks

**What Happened:**
```
The Inconsistent Lock Order:
═══════════════════════════════════════════════════════════════

Code Path A (by name):           Code Path B (by ID):
SELECT ... FOR UPDATE            SELECT ... FOR UPDATE
ORDER BY name;                   ORDER BY id;

Path A locks: Alice, Bob, Charlie
Path B locks: ID 3, ID 1, ID 2 (Charlie, Alice, Bob)

Nightly overlap:
Path A: LOCK Alice --> wants Bob
Path B: LOCK Bob   --> wants Alice

DEADLOCK! Postgres kills one transaction.
Every night. For weeks.
```

**The Fix:**
- Single canonical lock order: `ORDER BY id ASC` everywhere
- Code review checklist item: "Lock order consistent?"
- Deadlocks dropped to near-zero

---

## 💥 War Story 4: The ShedLock Double-Fire

**The Setup:**
- 5 replicas running nightly billing via ShedLock
- `lockAtMostFor = PT1M` (1 minute)

**What Happened:**
```
Clock Skew Strikes:
═══════════════════════════════════════════════════════════════

Replica 1 clock: 00:00:00 (correct)
Replica 3 clock: 00:01:30 (90 seconds ahead - bad NTP config!)

t=00:00:00 (real)
  Replica 1: Acquires lock, lock_until = 00:01:00
  Replica 1: Starts billing job...

t=00:00:30 (real)
  Replica 3 thinks it's 00:02:00!
  Replica 3: lock_until (00:01:00) < now (00:02:00)? YES!
  Replica 3: Acquires lock! Starts billing!

Both replicas sent billing emails!
Duplicate invoices to customers!
```

**The Fix:**
- Bumped `lockAtMostFor` to 30 minutes
- Fixed NTP configuration, added drift alerting
- Added job-level idempotency: `(customer_id, billing_cycle)` unique key

---

## 💥 War Story 5: The Long-Running Transaction That Bloated Postgres

**The Setup:**
- Background reconciliation job
- Opened transaction, processed millions of records

**What Happened:**
```
VACUUM Starvation:
═══════════════════════════════════════════════════════════════

t=0:00    Reconciliation job starts
          BEGIN TRANSACTION;
          
t=0:30    Job processing... (transaction still open)
          VACUUM tries to clean old tuples
          "Can't clean - transaction 12345 might need them!"
          
t=1:00    More updates happening, dead tuples accumulating
          Table bloat: +5 GB
          
t=2:00    Still processing...
          Table bloat: +15 GB
          Query planner: "This table is huge now!"
          Latency spikes across the system
          
t=3:00    Job finally commits
          VACUUM can now clean up
          But damage done - 30 GB of bloat
          Queries slow for hours until VACUUM catches up
```

**The Fix:**
- Broke job into short transactions (max 5 minutes each)
- Added `idle_in_transaction_session_timeout = 15min` at DB level
- Monitoring for long-running transactions

---

# Part 6: Interview Traps & L5 Answers

## 🎯 Quick Reference Table

| Trap | Bad Answer | L5 Answer |
|------|------------|-----------|
| "Redlock - safe?" | "Yes, quorum." | "Safer than single-node but not strictly correct under adversarial timing. Combine with fencing token at the resource." |
| "Which lock backend?" | "Redis, it's fast." | "Depends on tolerance: Redis for short-lived best-effort, ZK/etcd for correctness-critical. Always add fencing." |
| "FOR UPDATE hurts throughput?" | "Use READ COMMITTED." | "Optimistic locking (@Version) if low contention, or SKIP LOCKED for queue-in-a-table pattern." |
| "RR vs SERIALIZABLE in Postgres?" | "Just phantom prevention." | "Postgres RR is Snapshot Isolation - prevents phantoms but allows write skew. SERIALIZABLE adds SSI conflict detection." |
| "What is write skew?" | "A phantom." | "Two txns read same set, each modify one row, combined effect breaks invariant. No row conflict, so RR misses it." |
| "How does Postgres avoid readers blocking writers?" | "Row locks." | "MVCC - every tuple has xmin/xmax, readers see snapshot, writers create new versions. VACUUM cleans up." |
| "Exactly-once with distributed lock?" | "Yes if TTL." | "No. Exactly-once needs idempotency at destination. Lock gives coordination; fencing token + idempotency key give safety." |

---

## 🏆 L5-Grade Code Examples

### Example 1: Redis Lock with Fencing Token

```java
public class FencedRedisLock {
    
    // Acquire - returns fencing token or -1 if not acquired
    private static final String ACQUIRE_SCRIPT = """
        if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
            return redis.call('INCR', KEYS[2])
        else
            return -1
        end
        """;
    
    // Release - only if we still own it
    private static final String RELEASE_SCRIPT = """
        if redis.call('GET', KEYS[1]) == ARGV[1] then
            return redis.call('DEL', KEYS[1])
        else
            return 0
        end
        """;
    
    public long acquire(String resource, String ownerId, Duration ttl) {
        Object result = redis.eval(
            ACQUIRE_SCRIPT,
            List.of("lock:" + resource, "fencing:" + resource),
            List.of(ownerId, String.valueOf(ttl.toMillis()))
        );
        return (long) result;
    }
    
    public boolean release(String resource, String ownerId) {
        long result = (long) redis.eval(
            RELEASE_SCRIPT,
            List.of("lock:" + resource),
            List.of(ownerId)
        );
        return result == 1;
    }
}

// Usage with fencing token validation
public void processOrder(Order order) {
    String ownerId = UUID.randomUUID().toString();
    long fencingToken = lock.acquire("orders", ownerId, Duration.ofSeconds(30));
    
    if (fencingToken < 0) {
        throw new LockNotAcquiredException();
    }
    
    try {
        // Pass fencing token to storage!
        orderStorage.save(order, fencingToken);
    } finally {
        lock.release("orders", ownerId);
    }
}
```

### Example 2: Optimistic Locking with Bounded Retry

```java
@Service
public class TransferService {
    
    private static final int MAX_RETRIES = 3;
    
    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        int attempts = 0;
        
        while (true) {
            try {
                Account from = accountRepo.findById(fromId).orElseThrow();
                Account to = accountRepo.findById(toId).orElseThrow();
                
                from.setBalance(from.getBalance().subtract(amount));
                to.setBalance(to.getBalance().add(amount));
                
                accountRepo.save(from);
                accountRepo.save(to);  // @Version drives UPDATE...WHERE version=?
                return;
                
            } catch (OptimisticLockException e) {
                if (++attempts >= MAX_RETRIES) {
                    log.error("Transfer failed after {} retries", MAX_RETRIES);
                    throw e;
                }
                // Exponential backoff
                Thread.sleep(50L << attempts);  // 100ms, 200ms, 400ms
            }
        }
    }
}
```

### Example 3: SKIP LOCKED Job Queue

```java
@Repository
public class JobQueueRepository {
    
    @Transactional
    public Optional<Job> takeNextJob(String workerId) {
        // Atomic: find + lock + update status
        return jdbc.query("""
            SELECT id, payload, attempt_count 
            FROM jobs
            WHERE status = 'PENDING'
              AND (visibility_timeout IS NULL OR visibility_timeout < NOW())
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, this::mapJob)
            .stream()
            .findFirst()
            .map(job -> {
                jdbc.update("""
                    UPDATE jobs 
                    SET status = 'IN_PROGRESS',
                        locked_by = ?,
                        visibility_timeout = NOW() + INTERVAL '5 minutes',
                        attempt_count = attempt_count + 1
                    WHERE id = ?
                    """, workerId, job.getId());
                return job;
            });
    }
    
    @Transactional
    public void completeJob(Long jobId) {
        jdbc.update("UPDATE jobs SET status = 'COMPLETED' WHERE id = ?", jobId);
    }
    
    @Transactional
    public void failJob(Long jobId, int maxAttempts) {
        jdbc.update("""
            UPDATE jobs 
            SET status = CASE 
                    WHEN attempt_count >= ? THEN 'DEAD_LETTER'
                    ELSE 'PENDING'
                END,
                visibility_timeout = NULL,
                locked_by = NULL
            WHERE id = ?
            """, maxAttempts, jobId);
    }
}
```

### Example 4: Handling Write Skew with SERIALIZABLE

```java
@Service
public class OnCallService {
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeFromOnCall(Long doctorId) {
        int retries = 0;
        
        while (true) {
            try {
                // Count doctors currently on call
                int onCallCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM oncall WHERE on_call = true",
                    Integer.class
                );
                
                if (onCallCount <= 1) {
                    throw new BusinessRuleException(
                        "Cannot remove - at least one doctor must be on call"
                    );
                }
                
                // Safe to remove
                jdbc.update(
                    "UPDATE oncall SET on_call = false WHERE doctor_id = ?",
                    doctorId
                );
                return;
                
            } catch (SerializationFailureException e) {
                // Error code 40001 - another transaction conflicted
                if (++retries >= 3) throw e;
                // Retry with fresh snapshot
            }
        }
    }
}
```

---

## 🎯 Self-Check Questions

1. **Name three distributed-lock backends and one strength of each.**

2. **Why is Redlock alone unsafe? What does a fencing token add?**

3. **Draw the ZooKeeper ephemeral-sequential lock algorithm.**

4. **Optimistic vs pessimistic - pick criteria in 2 sentences or less.**

5. **Give an SQL example of SELECT...FOR UPDATE SKIP LOCKED and one production use.**

6. **Four SQL anomalies and which isolation level prevents each.**

7. **Explain write skew with a real invariant. Which isolation level fixes it?**

8. **Postgres MVCC - xmin, xmax, and why VACUUM matters.**

9. **How does ShedLock fail under clock skew, and what saves you?**

10. **State the L5 mantra on exactly-once semantics.**

---

## 🎓 The L5 Mantra (Memorize This!)

> *"Distributed 'exactly-once' is impossible in the general case. Distributed 'effectively-once' is a solved problem - a lock for coordination, idempotency at the destination."*

---

## ➡️ Next Module

Move to **Module 9 - Production Diagnostics** (thread dumps, JFR, async-profiler, deadlock/livelock/starvation, thread-pool starvation deadlock, ThreadLocal leaks).

---

*"In distributed systems, the network is not reliable, clocks are not synchronized, and processes can pause at any time. Design for failure, not for success."* 🌐
