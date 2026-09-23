# Module 8B — Distributed Coordination, Consensus & Event Concurrency

> **Goal:** You can explain Raft in three diagrams, defend a choice between 2PC / Saga / TCC in a design round, describe Kafka's exactly-once semantics precisely, and pattern-match production incidents (thundering herd, split brain, cascading overload) to the standard fixes.

This is the Staff-level extension of Module 8. If Module 8 was "how do I make one operation safe across nodes?", this is "how do I make a **system** safe across nodes over time?"

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────────────┐
                     │       MODULE 8B CORE                        │
                     └────────────────────────────────────────────┘
                                    │
   ┌────────────────┬───────────────┼────────────────┬──────────────────┐
   │                │               │                │                  │
 8B.1 Consensus  8B.2 Consistency 8B.3 Distributed 8B.4 Kafka         8B.5 Outbox
 (Raft, quorum,  Models + CAP    Transactions:    Concurrency:       + CDC
 leader-based)   PACELC          2PC → Saga → TCC EOS-v2, groups     (Debezium)
   │                │               │                │                  │
 8B.6 Distributed 8B.7 Cache      8B.8 Distributed 8B.9 Split Brain  8B.10 Leader
 Rate Limiting   Concurrency:    Backpressure:    + STONITH +       Election
 (Redis Lua)     Cache-aside,    Circuit breaker, Cluster Fencing   Patterns
                 single-flight   bulkhead, adaptive
```

---

## 8B.1 Consensus — Raft in Three Diagrams

### Why we care

Every distributed lock, config store, k8s controller, database primary, and Kafka controller ultimately sits on a **consensus algorithm**. Understand consensus and you understand *why* your infrastructure has odd numbers of replicas, why writes need quorum, and why "the leader disappeared" is the same failure mode everywhere.

You do **not** need to hand-code Paxos. You need to explain **Raft** clearly enough that an interviewer sees you've built systems on it.

### Real-world analogy

> Consensus is a **committee vote among unreliable members over a bad phone line**. Any member can drop the call. Any message can be delayed. Yet the committee must agree on a single ordered sequence of decisions — and any surviving majority must remember every decision the group ever ratified.

### The three pieces of Raft

Raft splits consensus into three sub-problems, each with its own diagram.

#### (1) Leader election

```
Term 5 leader (F1) is heard from
via periodic heartbeats.
                                              [F1 leader, term 5]
                                                     │
          heartbeats  ┌────────────────┬─────────────┴─────────────┐
                      ▼                ▼                           ▼
                    F2 follower      F3 follower                F4 follower
                    (timer resets)   (timer resets)             (timer resets)


F1 crashes. F3's election timer fires first.
                                       ┌── F3 candidate ──►  "vote for me, term 6"
                                       │
                                       ▼
                       F2, F4 vote YES  → F3 becomes leader, term 6
```

- Each node has a randomized election timeout (~150–300 ms).
- Follower not hearing from a leader → becomes **candidate**, increments term, votes for itself, asks peers.
- **Quorum of votes = leadership.** Old-leader messages carrying a lower term are rejected.
- Randomization is the trick that avoids simultaneous elections.

#### (2) Log replication

```
Leader appends entry     Followers append & ack     Leader commits when
locally (term, index)    (respect term/index gaps)  quorum has replicated

    Leader   [ e1  e2  e3* ]                        * = uncommitted
    Follower [ e1  e2  e3  ] ← ack                    Leader waits for
    Follower [ e1  e2  e3  ] ← ack                    ⌈N/2⌉ acks
    Follower [ e1        _ ] (stale)                  then commits e3
```

- **`AppendEntries`** RPC ships the next log entry to every follower.
- Each entry carries `(term, index, prevTerm, prevIndex)`. A follower that doesn't match refuses; leader backs up until it finds the point of divergence, then rewrites forward.
- Entry is **committed** only when a majority has stored it. Committed entries are then applied to the state machine (the DB, KV store, config map…).

#### (3) Safety — "committed means committed forever"

Two Raft invariants prevent lost data across leadership changes:
- A candidate needs its log to be at least as up-to-date as any other majority-node's log to win. Old candidates cannot become leaders.
- A leader never overwrites a committed entry; it only appends.

Together, these give **linearizable writes** to the replicated state machine.

### Quorum math (memorize)

- `N` replicas → majority = `⌊N/2⌋ + 1`.
- Tolerates `f = ⌊(N−1)/2⌋` failures.
- 3 nodes → tolerate 1. 5 nodes → tolerate 2. 7 nodes → tolerate 3.
- Adding an even-numbered node **doesn't help availability**; you still need the same majority.

### "Why Paxos too?"

- **Multi-Paxos** predates Raft (Lamport, 1998). Correct, but the paper is famously hard to implement. Google Chubby, Spanner internals use variants.
- **Raft** (Ongaro & Ousterhout, 2014) is a re-explanation designed for **understandability**. Same guarantees; easier to implement correctly.
- L5 answer: *"They're equivalent in guarantees; Raft won because it decomposes consensus into three orthogonal sub-problems and is easier to build without subtle bugs."*

### Where you meet Raft in production

| System | Consensus role |
|---|---|
| etcd | Raft is the entire protocol; backs Kubernetes' state |
| Kafka (KRaft mode) | Metadata log via Raft; replaces ZooKeeper |
| CockroachDB | Per-range Raft groups for replication |
| Consul | Raft for KV / service registry |
| Redis (Sentinel/Cluster) | Not Raft; a lighter gossip + failover protocol (weaker) |

### Trap-avoid answers

- *"Consensus is slow — why do we use it?"* — Only writes to state that must survive a crash go through consensus. Reads (often) don't. Latency is the price of durability.
- *"Why odd numbers of replicas?"* — Availability + fault tolerance are dictated by majority, so 3 and 4 tolerate the same number of failures (1). 4 just costs more.
- *"Can we do consensus with 2 nodes?"* — No — no majority possible when they disagree. You'd need a witness/tiebreaker (Azure SQL, HA Postgres use this pattern).

---

## 8B.2 Consistency Models — The Spectrum + CAP + PACELC

### Real-world analogy

> Imagine a **shared Google Doc** you and 20 colleagues are editing.
> - **Linearizable** — everyone sees every edit in the same order the instant it's typed. Feels like one shared blackboard.
> - **Sequential** — same order for everyone, but everyone might be seeing a slightly delayed view.
> - **Causal** — if A replies to B, everyone sees B before A. Order between unrelated edits may differ.
> - **Read-your-writes** — you see your own edits immediately, but others may lag.
> - **Eventual** — if editing stops, everyone converges. No promise about *when*.
>
> Real systems live somewhere on this spectrum; you rarely need the top.

### The spectrum (strongest ⇢ weakest)

| Model | Guarantee | Cost | Example systems |
|---|---|---|---|
| **Linearizable / Strict Serializable** | Global real-time order; one true sequence | Highest (majority quorum + fencing) | Spanner, etcd, CockroachDB SERIALIZABLE |
| **Sequential** | Global order but not tied to wall-clock time | High | Rare in the wild |
| **Snapshot Isolation** | Each txn sees a consistent snapshot; **allows write skew** | Medium | Postgres RR, Oracle, MySQL InnoDB |
| **Causal** | Preserves cause→effect; unrelated events may reorder | Medium | COPS, Bayou |
| **Read-your-writes** | You see your own updates immediately | Low–Medium | Sticky sessions, DynamoDB with consistent-read flag |
| **Monotonic reads** | Once you see a value, later reads see that or newer | Low | Cassandra reads with token routing |
| **Eventual** | Given enough quiet time, replicas agree | Lowest | Cassandra default, S3, DNS |

### CAP theorem — memorize the honest version

> When a **partition** happens, you can pick **Consistency** or **Availability**, not both. Everything else is marketing.

The mistake juniors make: treating CAP as a design-time knob. It isn't. Partitions are *rare*; you spend most of your life in the **non-partitioned** case, and CAP says nothing about that. Enter PACELC.

### PACELC — the model that actually matches production

**If a Partition, choose Availability or Consistency; Else (normal ops), choose Latency or Consistency.**

| System | P → | E → |
|---|---|---|
| DynamoDB (default) | A | L |
| MongoDB (default) | A | L |
| Cassandra | A | L |
| CockroachDB | C | C |
| Spanner | C | C |
| Postgres primary | C | C |

**Interview one-liner:** *"CAP tells you what happens during a partition. PACELC is the design knob you actually turn — how much latency will I pay for consistency during normal operation?"*

### Consistency ≠ isolation

Very common confusion:
- **Isolation** (SQL) — a property of *transactions* against each other on **one** DB (Module 8).
- **Consistency** (distributed systems) — a property of *replicas* against each other across a cluster (this module).

A Postgres primary can be `SERIALIZABLE` (top isolation) yet be **inconsistent** with an asynchronous replica (stale reads).

---

## 8B.3 Distributed Transactions — 2PC → Saga → TCC

### The problem

You need to update **two systems** atomically: an order in Postgres and an inventory reservation in Redis. Either both succeed or both roll back. Local `@Transactional` cannot help.

### Option A — Two-Phase Commit (2PC / XA)

```
Coordinator                  Participant A             Participant B
    │       PREPARE?                │                        │
    ├──────────────────────────────►│                        │
    ├───────────────────────────────┼───────────────────────►│
    │  ◄─── VOTE-YES ────           │                        │
    │  ◄─── VOTE-YES ─────────────────                        │
    │       COMMIT                  │                        │
    ├──────────────────────────────►│  (persist)             │
    ├───────────────────────────────┼───────────────────────►│  (persist)
```

- **Phase 1 (prepare):** every participant locks resources and votes YES/NO.
- **Phase 2 (commit/abort):** coordinator broadcasts the decision.

**Why 2PC is largely abandoned:**
- Blocks resources between prepare and commit — can be **seconds**.
- If the coordinator crashes mid-phase-2, participants are stuck **in-doubt** until it recovers. No timeout is safe.
- Not fault-tolerant against network partitions.
- Needs XA-aware drivers everywhere.

**Where 2PC still lives:** classic bank cores on IBM/Oracle stacks. Mostly not new development.

### Option B — Saga

Break the distributed transaction into **local transactions**, each with a compensating action if a later step fails.

```
Order Saga:
  T1  Reserve inventory      ──►  Compensating: Release inventory
  T2  Charge card            ──►  Compensating: Refund
  T3  Create shipping label  ──►  Compensating: Cancel label
  T4  Send confirmation email       (no compensation needed)
```

Two flavors:

- **Choreography** — each service listens to events and reacts. No central brain.
  - Pros: no single-point-of-failure orchestrator; loose coupling.
  - Cons: hard to visualize; debugging = piecing together events across systems.
- **Orchestration** — one **saga orchestrator** service issues commands in sequence.
  - Pros: readable state machine; easy to add steps; central retry/timeout logic.
  - Cons: orchestrator is a critical component.

**Modern L5 default:** orchestration via **Temporal.io**, **AWS Step Functions**, or **Camunda**. These give you durable state machines with automatic retries, timeouts, and versioning. Say this in the interview.

### Option C — TCC (Try-Confirm-Cancel)

A middle ground for systems where compensation is unnatural (e.g., charging a card and then refunding a fee).

```
Try     — reserve capacity, don't yet make it visible/final
Confirm — flip the reservation to final (usually cheap and idempotent)
Cancel  — release the reservation (idempotent, must succeed)
```

Example: `Try` = "reserve 500 seats"; `Confirm` = "convert reservation to booking"; `Cancel` = "release the seats." Popular in fintech / seat-inventory / trading systems.

### Which to pick

| If… | Choose |
|---|---|
| Only 2 systems, both XA-capable, short critical section, no partition tolerance needed | 2PC |
| Many services, business-level compensations make sense | **Saga (orchestration)** — this is the modern default |
| High-value reservations where "undo" is hard/impossible | TCC |

### The killer L5 point

> "Distributed transactions are a business-level design problem, not a database feature. The right question is *'what does compensation look like'* — not *'how do I get atomicity across two databases.'* Sagas make compensation explicit, which is why they scale."

---

## 8B.4 Kafka Concurrency — Consumer Groups, EOS-v2

Kafka is the L5 canonical exam of "distributed concurrency in an event system." Know it cold.

### Partitions = the unit of parallelism

- A topic has **N partitions**. Each partition is an ordered log.
- Ordering guarantees are **per partition**, not per topic.
- The producer picks partition via `key.hashCode() % N` (default) or a custom partitioner.

### Consumer groups & rebalancing

- Consumers in a **group** split partitions among themselves — each partition is assigned to **exactly one** consumer at a time.
- Adding a consumer, removing one, or a partition change triggers a **rebalance**.

**The rebalance model (2019+):**
- **Eager rebalance** (legacy) — everyone stops consuming, all partitions are reassigned, everyone resumes. "Stop-the-world" — bad for latency.
- **Cooperative Sticky rebalance** (Kafka 2.4+, now the default) — only *moved* partitions are paused; the rest keep consuming.

**L5 tip:** always confirm the client uses cooperative sticky. A team that runs 2.3-era eager rebalances silently doubles their p99 during deploys.

### Delivery semantics

| Semantic | How | When |
|---|---|---|
| **At-most-once** | Auto-commit offsets before processing | Log metrics; loss is acceptable |
| **At-least-once** | Process, then commit offsets manually | 99% of pipelines |
| **Exactly-once (EOS-v2)** | Idempotent producer + transactional producer + `read_committed` consumer | Money, dedup-sensitive pipelines |

### Idempotent producer (the "no dupes on retry" fix)

```properties
enable.idempotence=true            # producer assigns sequence numbers per (producer, partition)
acks=all
max.in.flight.requests.per.connection=5   # ok — sequence-based dedup
retries=Integer.MAX_VALUE
```

Without this, a network retry can cause a partition to see the same message twice.

### Transactional producer (EOS-v2)

```java
producer.initTransactions();
try {
    producer.beginTransaction();
    producer.send(recordA);
    producer.send(recordB);
    producer.sendOffsetsToTransaction(offsets, "consumer-group-id");
    producer.commitTransaction();
} catch (Exception e) {
    producer.abortTransaction();
    throw e;
}
```

Consumers must set `isolation.level=read_committed` so aborted transactions are skipped. This is the correct "read-process-write" atomicity primitive Kafka gives you.

### The concurrency traps you must name

1. **Rebalance storms during deploy** — one restart triggers full reassignment; second restart triggers another. Use **static group membership** (`group.instance.id`) so short restarts don't trigger a rebalance.
2. **Slow processing → session timeout → rebalance** — if a consumer takes longer than `max.poll.interval.ms` to process a batch, Kafka assumes it's dead and rebalances. Tune batch size or split work into a bounded queue.
3. **Ordering assumption across partitions** — there is none. If ordering matters (event sourcing, per-user timelines), key by the entity ID.
4. **`enable.auto.commit=true` in prod** — offsets get committed even for messages you haven't finished processing. Bugs disappear silently. Turn it off.
5. **Zombie writers with old epochs** — an old producer with a stale epoch tries to commit after a new one has taken over. Transactional API rejects this via **producer fencing** (epoch numbers act as fencing tokens — same idea as Module 8).

---

## 8B.5 Outbox Pattern + CDC — Atomic DB-and-Event

### The problem

You want to **update a DB row and publish an event** atomically. Naive approach:

```java
@Transactional
void placeOrder(Order o) {
    orderRepo.save(o);
    kafkaTemplate.send("orders", o);   // BUG
}
```

Failure modes:
- If Kafka is down but the DB commit succeeds → order exists, no event ever fired.
- If the DB commit fails after Kafka has published → phantom order downstream.
- If the process crashes between the two → non-deterministic partial state.

There is **no way** to make `save + send` truly atomic — they're two systems.

### The Outbox pattern

Write the event into the **same DB transaction** as the business row, in a dedicated `outbox` table. A separate **relay process** reads from `outbox` and publishes to Kafka; on success, marks the row as sent.

```
┌─────────────────────────────┐              ┌──────────────────┐
│  Postgres (single txn)      │              │                  │
│                             │              │                  │
│   INSERT orders (…);        │              │                  │
│   INSERT outbox (payload,   │              │                  │
│                  status)    │              │                  │
│   COMMIT;                   │              │                  │
└──────────┬──────────────────┘              └──────────────────┘
           │
           │ CDC / poll                              publish
           ▼
       Debezium ────────────────────────────────────►   Kafka
       (reads Postgres WAL)
```

Two implementations:

1. **Polling relay** — background job SELECTs unsent rows, publishes them, marks them sent. Simple but poll interval = event latency.
2. **CDC via Debezium** — reads Postgres logical replication log; publishes changes to Kafka. No polling, near-real-time.

### Delivery guarantees

- **At-least-once** — the relay may publish an event, crash before marking it sent, and re-publish on restart.
- Downstream consumers must be **idempotent** — the event carries a unique ID (`event_id`); consumers dedupe by that ID.

This is the **canonical L5 answer** for "how do you keep a DB and Kafka in sync." Say "Outbox pattern with Debezium and idempotent consumers." Interviewers relax.

### Related patterns worth naming

- **Transactional Outbox** — the pattern above.
- **Listen/Notify** — Postgres `LISTEN`/`NOTIFY` for low-latency delivery within one DB cluster; not a substitute for durable messaging.
- **Change Data Capture (CDC)** — Debezium, Maxwell — read the DB's replication log as a stream.
- **Log-Trailing** (Netflix DBLog) — same idea; scaling infra.

---

## 8B.6 Distributed Rate Limiting

Single-node in Module 4 was easy. Now the token bucket must be shared across a cluster.

### Option A — Redis token bucket (Lua script)

```lua
-- KEYS[1] = bucket key
-- ARGV[1] = capacity
-- ARGV[2] = refill per second
-- ARGV[3] = requested tokens
-- ARGV[4] = now (ms)

local capacity  = tonumber(ARGV[1])
local refill    = tonumber(ARGV[2])
local requested = tonumber(ARGV[3])
local now       = tonumber(ARGV[4])

local bucket = redis.call("HMGET", KEYS[1], "tokens", "ts")
local tokens = tonumber(bucket[1]) or capacity
local ts     = tonumber(bucket[2]) or now

local delta = math.max(0, now - ts) * refill / 1000
tokens = math.min(capacity, tokens + delta)

if tokens < requested then
    redis.call("HMSET", KEYS[1], "tokens", tokens, "ts", now)
    redis.call("PEXPIRE", KEYS[1], 60000)
    return 0
end

tokens = tokens - requested
redis.call("HMSET", KEYS[1], "tokens", tokens, "ts", now)
redis.call("PEXPIRE", KEYS[1], 60000)
return 1
```

- Whole script executes **atomically** on the Redis single-threaded event loop.
- Refill computed on-demand — no background timer.
- `PEXPIRE` keeps the keyspace bounded — idle buckets self-clean.

### Option B — Sliding-window log (more accurate)

```lua
-- Remove events older than window; count remaining; reject if >= limit.
redis.call("ZREMRANGEBYSCORE", KEYS[1], 0, now - window)
local count = redis.call("ZCARD", KEYS[1])
if count >= limit then return 0 end
redis.call("ZADD", KEYS[1], now, now .. ":" .. math.random())
redis.call("PEXPIRE", KEYS[1], window * 2)
return 1
```

Accurate to the millisecond; costs more memory (one sorted-set entry per request).

### Option C — Local pre-rejection + centralized reconciliation

Cluster-wide limit is 10k rps. Each of 20 pods gets 500 rps locally. Periodically pull remaining budget from Redis. Trades small over/undershoot for **near-zero request latency overhead**. This is what most cost-conscious teams (Stripe, Cloudflare) actually use.

### The distributed-rate-limit interview trap

*"Redis is the bottleneck now."* Answer:
- Use Redis Cluster with the bucket key sharded by user/tenant.
- Or a Redis proxy that supports script sharding.
- Or accept Option C's soft-cap tradeoff.

---

## 8B.7 Cache Concurrency — Cache-Aside, Thundering Herd, Single-Flight

### The four cache-write strategies

| Strategy | Read path | Write path | Consistency |
|---|---|---|---|
| **Cache-aside (lazy loading)** | Miss → load from DB → populate cache | Write DB → invalidate cache | Weak; race between load and invalidate |
| **Write-through** | Read cache; miss loads and populates | Write goes to cache and DB synchronously | Stronger; slower writes |
| **Write-behind (write-back)** | Read cache | Write to cache; async flush to DB | Weakest durability; fastest writes |
| **Refresh-ahead** | Read cache | Cache proactively refreshes near TTL | Good for read-heavy hot keys |

Cache-aside is the default. Every trap below is a cache-aside trap.

### Trap 1 — Cache invalidation race

```
T1 reads: cache miss → loads OLD from DB
T2 writes DB → cache.delete()
T1 populates cache with OLD value → cache is now STALE indefinitely
```

Root cause: the load and the invalidate are not ordered. Fixes:
- **Write-through** for the field.
- **Versioned entries** — cache stores `(value, version)`; DB writes bump version; on load, only populate if version ≥ current.
- **Short TTL** — accept staleness bounded by TTL. Pragmatic default.

### Trap 2 — Thundering herd on cache expiry

The hot key expires at exactly T. At T + 1 ms, 5,000 concurrent requests all miss → all hit the DB → DB dies.

**Fixes:**

1. **Single-flight / request coalescing** — the first miss loads; others wait for the same in-flight load. Reuse the same `CompletableFuture`.

    ```java
    ConcurrentHashMap<String, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    public V load(String key) {
        V cached = cache.get(key);
        if (cached != null) return cached;

        CompletableFuture<V> f = inFlight.computeIfAbsent(key, k ->
            CompletableFuture.supplyAsync(() -> {
                try { return loadFromDb(k); }
                finally { inFlight.remove(k); }        // release the slot
            }));
        V value = f.join();
        cache.put(key, value);
        return value;
    }
    ```

    Guava's `LoadingCache` and Caffeine's `AsyncLoadingCache` do this internally.

2. **Probabilistic early refresh** — refresh with probability that grows as TTL nears. XFetch algorithm.

3. **Stale-while-revalidate** — serve the expired value once; async refresh; subsequent readers get the new value. Trades staleness for stability.

### Trap 3 — Hot key on cluster

One product goes viral. All requests hash to the same Redis node. Node saturates.

- **Replicated cache** — cache the hot key on every node (client-side L1) with a very short TTL.
- **Randomized keys** — `hotkey#shard0..3` picked randomly; DB fanout on write.
- **Aggressive local cache** with `Caffeine` in the app tier absorbing the hot reads before Redis ever sees them.

### Trap 4 — Cache stampede on deploy

Rolling deploy = fresh JVM = empty local cache. Every pod starts hammering Redis / DB. Solutions:
- Warm-up phase on startup.
- Gradual rollout with health checks after cache-warming.
- Preloaded cache snapshot from S3.

---

## 8B.8 Distributed Backpressure — Circuit Breakers, Bulkheads, Adaptive Concurrency

### Circuit breaker — the L5 canonical pattern

Three states:
- **CLOSED** — calls pass through; failures counted.
- **OPEN** — fail fast; don't even try downstream.
- **HALF-OPEN** — after cooldown, allow a **probe** call. Success → CLOSED, failure → back to OPEN.

Netflix Hystrix popularized it; Resilience4j is the modern default.

```java
CircuitBreaker cb = CircuitBreaker.of("payments",
    CircuitBreakerConfig.custom()
        .failureRateThreshold(50)                  // ≥50% failure → OPEN
        .slowCallRateThreshold(80)
        .slowCallDurationThreshold(Duration.ofMillis(500))
        .slidingWindowSize(50)
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .permittedNumberOfCallsInHalfOpenState(5)
        .build());

Supplier<Payment> call = CircuitBreaker.decorateSupplier(cb, () -> gateway.charge(req));
Payment result = Try.ofSupplier(call)
                    .recover(t -> Payment.fallback())
                    .get();
```

### Bulkhead — isolate failures

Split your downstream capacity so a slow dependency can't consume all your threads.

- **Thread-pool bulkhead** — dedicated pool per downstream; if it saturates, other downstreams are unaffected.
- **Semaphore bulkhead** — cheaper; caps concurrent in-flight calls without a separate pool.

The "one slow downstream took down the whole app" incident is *always* fixable with bulkheads.

### Adaptive concurrency (the modern trick)

Instead of a static limit, dynamically shrink or grow your outgoing-call concurrency based on measured latency. If p99 rises → shed load; if it falls → increase concurrency. Netflix's **concurrency-limits** library implements Vegas/Gradient algorithms.

**L5 point:** *"Static thread pools guess at capacity. Adaptive concurrency measures it and reacts. On modern service meshes (Istio, Envoy) this is done at the sidecar via `outlier-detection` + `local-rate-limit` — but knowing the library-side implementation matters at Staff level."*

### Load shedding — the last resort

When you can't slow down, drop. Sophisticated version:
- Priority tiers: shed low-priority traffic first.
- Token bucket at ingress; reject > budget with a `429`.
- **Retry-After** headers so clients don't hammer.

### Combining them

Standard defense-in-depth:
```
Request → [Rate Limiter] → [Bulkhead] → [Circuit Breaker] → [Timeout] → Downstream
```

Miss any layer and one incident cascades.

---

## 8B.9 Split Brain, STONITH & Cluster Fencing

### Split brain

A network partition splits your cluster into two halves that can't see each other. Both halves have a majority *of what they see*, so both elect a leader. **Both accept writes.** Bad things happen when the partition heals.

**Prevention: quorum.** Only the side with a strict majority may accept writes. The minority side goes read-only or shuts down.

- 3-node cluster, 2-1 split → 2-node side wins, 1-node side stops.
- 4-node cluster, 2-2 split → nobody wins → total unavailability. Why odd numbers matter.

### STONITH — "Shoot The Other Node In The Head"

The classic HA (Pacemaker, Corosync) pattern. When a node is *suspected* dead:
1. **Fence** it — power-cycle via IPMI, cut its SAN access, or fence its network port.
2. **Then** promote the standby.

Without fencing, the "dead" node might come back and write to shared storage while the standby is also writing → corruption. STONITH ensures **at most one writer at a time**.

Modern cloud equivalents:
- AWS: revoke IAM permissions, detach EBS volume, remove from ALB target group.
- k8s: pod eviction + PDBs + node cordon.

### Cluster-level fencing tokens

Module 8's fencing tokens were per-lock. STONITH-style fencing is per-node. Both are needed. Diagram:

```
Client A holds lock (token=42) but GC-paused
Cluster fencing: kicks A out of the network (STONITH-cloud)
Client B acquires lock (token=43)
A wakes, tries to write → rejected by BOTH
   (a) cluster network (node fenced)
   (b) storage layer (token 42 < 43)
```

Two independent layers of defense — a common Staff-level system design pattern.

### The "leader lease" trick

Rather than "am I the leader?", a leader holds a **time-bounded lease**. If a partition happens, the leader's lease expires — it voluntarily steps down before the other side elects a new leader. Requires roughly-synchronized clocks. Spanner's TrueTime, etcd's leader lease, and K8s Lease objects all use this.

---

## 8B.10 Leader Election Patterns

### Real-world analogy

> A **shift manager** must be picked when the previous manager's shift ends or they call in sick. The rules must ensure exactly one manager at a time, and everyone else must be able to recognize the current manager reliably.

### Pattern 1 — ZooKeeper ephemeral sequential znode (Module 8)

- Create `/leader/candidate-` with ephemeral+sequential mode.
- Lowest sequence number wins; others watch the one just below theirs.
- Session death → automatic dropout.

Fair, correct, battle-tested. Curator's `LeaderLatch` / `LeaderSelector` implement this.

### Pattern 2 — etcd election API

```
1. Create lease (TTL 15s), keep-alive
2. Put(/election/leader, myId, lease)
3. Watch the key with lower revision than mine
4. If it disappears, I might be the new leader — re-check I am the lowest revision holder
```

Same idea, native lease-based TTL. Cleaner API; used by k8s controllers.

### Pattern 3 — K8s `Lease` objects (`coordination.k8s.io/v1`)

The k8s-native primitive. Client goes:
```
GET /apis/coordination.k8s.io/v1/namespaces/x/leases/my-lease
- if holder empty or renewTime < now - leaseDurationSeconds → try to acquire (compare-and-set on resourceVersion)
- else → I'm not leader, wait
```

If your service already runs in k8s, this is the cheapest coordination — no ZK/etcd needed. Backed by etcd anyway, transparently.

### Pattern 4 — Postgres advisory locks

Session-scoped:
```sql
SELECT pg_try_advisory_lock(hashtext('billing-job'));
-- do work
SELECT pg_advisory_unlock(hashtext('billing-job'));
```

Free if you already run Postgres. **Session-tied** — if the connection dies, the lock releases. Good for cron singletons. Not for high-frequency contention.

### Pattern 5 — Redis single-node "leader lock"

`SET leader:myrole holder-id NX PX 15000` + periodic renew. Same pattern as Module 8's Redis lock; add fencing tokens if writes must be safe.

### Which to pick

| Need | Pick |
|---|---|
| Deep infra, already on ZK | ZK ephemeral sequential |
| K8s-native service | K8s Lease |
| Runs everywhere, cheap | etcd Election |
| Only need "one job at a time," Postgres present | Postgres advisory lock |
| Fastest / most permissive, best-effort | Redis lock + renew |

---

## 🔥 Where L5 Candidates Fumble (Interview Traps)

| Trap | Bad answer | L5 answer |
|---|---|---|
| "How does Raft work?" | "It's a consensus algorithm." | "Leader-based Multi-Paxos variant with three sub-problems: election with randomized timeouts, log replication via AppendEntries, safety via up-to-date-log rule. Commits after majority replication." |
| "CAP: pick 2." | "AP or CP." | "CAP is about partition-time behavior only. PACELC is the honest model — during normal operation you're trading latency for consistency, not availability." |
| "How do I get atomic write to DB + Kafka?" | "2PC." | "Outbox pattern. Same DB txn writes business row and outbox row; Debezium ships outbox to Kafka; consumers idempotent by event id." |
| "Which transaction pattern for microservices?" | "2PC." | "Saga, orchestration-flavor, on Temporal or Step Functions. 2PC blocks resources and can't tolerate partitions." |
| "Kafka exactly-once — real?" | "No." | "Yes for the read-process-write pipeline: idempotent producer + transactional producer + `read_committed` consumer. Not for external side effects — those need consumer-side idempotency." |
| "How do I prevent split brain?" | "Careful monitoring." | "Quorum-based writes only + node fencing on suspected failures + leader leases. Never rely on 'careful monitoring.'" |
| "Distributed rate limiter — how?" | "Redis INCR." | "Atomic Lua script — token bucket or sliding window. Then either cluster-sharded or a local pre-rejection layer that reconciles with Redis." |
| "Thundering herd on cache expiry — fix?" | "Longer TTL." | "Single-flight (request coalescing) so only one loader hits DB. Optionally probabilistic early refresh or stale-while-revalidate." |

---

## 💻 L5-Grade Snippets

### Snippet 1 — Outbox row + Debezium-ready schema

```sql
CREATE TABLE outbox (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate     TEXT        NOT NULL,
    aggregate_id  TEXT        NOT NULL,
    event_type    TEXT        NOT NULL,
    payload       JSONB       NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX outbox_aggregate_idx ON outbox (aggregate, created_at);
```

App code:
```java
@Transactional
public void placeOrder(Order o) {
    orderRepo.save(o);
    outboxRepo.save(new OutboxEvent("order", o.id(), "OrderPlaced", toJson(o)));
}
```

Debezium picks it up from the Postgres WAL; ships to Kafka. Consumer dedupes by `id`.

### Snippet 2 — Kafka EOS-v2 read-process-write

```java
producer.initTransactions();
while (running) {
    ConsumerRecords<K, V> records = consumer.poll(Duration.ofSeconds(1));
    if (records.isEmpty()) continue;

    producer.beginTransaction();
    try {
        for (ConsumerRecord<K, V> r : records) {
            producer.send(new ProducerRecord<>("out-topic", transform(r)));
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (TopicPartition p : records.partitions()) {
            long last = records.records(p).get(records.records(p).size() - 1).offset();
            offsets.put(p, new OffsetAndMetadata(last + 1));
        }
        producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
        producer.commitTransaction();
    } catch (Exception e) {
        producer.abortTransaction();
    }
}
```

Consumers downstream must set `isolation.level=read_committed`.

### Snippet 3 — Resilience4j full defense-in-depth

```java
RateLimiter    rl = RateLimiter.of("gateway", RateLimiterConfig.custom()
                        .limitForPeriod(500).limitRefreshPeriod(Duration.ofSeconds(1)).build());
Bulkhead       bh = Bulkhead.of("gateway", BulkheadConfig.custom().maxConcurrentCalls(50).build());
CircuitBreaker cb = CircuitBreaker.of("gateway", CircuitBreakerConfig.custom()
                        .failureRateThreshold(50).build());
TimeLimiter    tl = TimeLimiter.of(Duration.ofMillis(500));

Supplier<CompletionStage<Response>> supplier = () ->
    CompletableFuture.supplyAsync(() -> gateway.call(req), ioPool);

Callable<Response> decorated = Decorators.ofSupplier(supplier)
    .withRateLimiter(rl)
    .withBulkhead(bh)
    .withCircuitBreaker(cb)
    .withTimeLimiter(tl, scheduler)
    .decorate();

Response resp = Try.ofCallable(decorated).recover(t -> Response.fallback()).get();
```

### Snippet 4 — Single-flight cache load

```java
private final ConcurrentHashMap<String, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

public V get(String key) {
    V cached = cache.getIfPresent(key);
    if (cached != null) return cached;

    CompletableFuture<V> f = inFlight.computeIfAbsent(key, k ->
        CompletableFuture.supplyAsync(() -> loadFromSource(k), loaderPool)
            .whenComplete((v, e) -> {
                inFlight.remove(k);
                if (v != null) cache.put(k, v);
            }));
    return f.join();
}
```

---

## 🏭 Production War Stories

**1. The Raft cluster that lost its majority (etcd).**
Team ran a 4-node etcd cluster "for more capacity." One node crashed. Now they had 3 available, needed majority = 3 → any single further failure meant total unavailability. Later a network hiccup cost them a second node → cluster halted. Fix: rebuilt as 5 nodes; documented that **etcd sizes must be odd**.

**2. The 2PC coordinator crash.**
Legacy service used XA across DB2 and MQ. Coordinator process crashed after PREPARE, before COMMIT. Both participants held locks for 4 hours until an ops engineer manually forced the commit. During that window, all downstream writes stalled. Fix: migrated to Saga with orchestration on Temporal; XA retired.

**3. The Kafka rebalance storm.**
20-pod consumer group. Rolling deploy triggered a rebalance per pod restart → 20 full "stop-the-world" rebalances → 4-minute consumer downtime. Fix: enabled cooperative sticky assignor; added static group membership; deploy-time downtime dropped to seconds.

**4. The outbox that lost events.**
Team put events in an in-memory queue and flushed to Kafka in a `@TransactionalEventListener(AFTER_COMMIT)`. Kafka was down → events dropped. Fix: proper outbox table + Debezium. Retro found ~1,200 lost events during a prior outage.

**5. The thundering-herd blackout.**
Homepage feature-flag cache had 30-second TTL. Every 30 seconds, all pods refetched from LaunchDarkly. Vendor rate-limited them → all requests failed → *all pods entered fallback* → homepage 500-ed globally. Fix: single-flight + jittered TTL (30s ± 5s) + stale-while-revalidate.

**6. The split-brain outage.**
HA Postgres with a fencing agent misconfigured. Network partition → both nodes promoted themselves → both accepted writes for 6 minutes → converged with two divergent WALs → manual data reconciliation over a weekend. Fix: added real IPMI STONITH, tested regularly with chaos drills.

**7. The circuit breaker that never opened.**
Team set `failureRateThreshold=50`, `slidingWindowSize=100`. But downstream degraded slowly — 30% failures, 70% slow-but-successful. Breaker stayed CLOSED. Cascading timeouts consumed the pool. Fix: added `slowCallRateThreshold=60` with `slowCallDurationThreshold=500ms` — breaker now opens on latency, not just errors.

---

## 🎯 Self-Check

1. Three sub-problems Raft decomposes consensus into.
2. Quorum for `N=5`. How many failures tolerated?
3. Explain PACELC in one sentence and give an example system for each quadrant.
4. When would you choose TCC over a Saga?
5. What guarantees does Kafka EOS-v2 give and what does it *not* cover?
6. Draw the Outbox pattern. Why is at-least-once acceptable if consumers dedupe?
7. Two Redis rate-limit approaches — how do they differ in accuracy and cost?
8. What is a thundering herd on cache expiry? Name two independent fixes.
9. Draw the standard defense-in-depth resilience chain (five layers).
10. Why must clusters have odd sizes? What is STONITH?

---

## ➡️ Next

Move to **Module 8C — Advanced Distributed Appendix** (Spanner + TrueTime, vector clocks / Lamport / HLC, consistent hashing, gossip / failure detection, CRDTs).
