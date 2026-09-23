# Module 8C — Advanced Distributed Topics (Appendix)

> **Goal:** Lighter coverage of the distributed-systems adjacencies that show up in Staff-level rounds when the interviewer probes deeper. You can *explain each in a minute*, name a real system that uses it, and know the one gotcha per topic.

These are not the primary axis of concurrency, but Staff loops at Google/Meta/Amazon frequently drop one into a system-design or hard-question round. Miss any and the interviewer will note it.

---

## 🗺️ Mind Map

```
                     ┌────────────────────────────────────────┐
                     │        MODULE 8C — APPENDIX            │
                     └────────────────────────────────────────┘
                                    │
       ┌────────────────┬───────────┼──────────────┬─────────────────┐
       │                │           │              │                 │
   8C.1 Spanner    8C.2 Logical  8C.3 Sharding  8C.4 Failure     8C.5 CRDTs
   TrueTime +      Time:         + Placement:   Detection:       (G-Counter,
   External Cons.  Lamport,      Consistent     Heartbeat,       PN-Counter,
                   Vector,       Hashing,       Phi Accrual,     LWW, OR-Set)
                   HLC           Rendezvous     SWIM / Gossip
```

---

## 8C.1 Spanner + TrueTime — External Consistency at Global Scale

### The problem Spanner solved

Serializable isolation is easy on one node. Serializable across **continents** with wall-clock ordering — "the transaction that committed first everywhere in the world was reported first" — is called **external consistency** (a.k.a. strict serializability). Nobody had done it at scale until Spanner (2012).

### The core insight — TrueTime

Google put **atomic clocks + GPS receivers** in every datacenter. The `TT.now()` API returns not a timestamp but an **interval**:

```
TrueTime.now() = { earliest, latest }
```

with a guaranteed bound (`ε ≈ 6ms` at the time of the paper) — the true real-world time is somewhere in that interval.

### Commit wait — the elegant trick

Spanner assigns each transaction a commit timestamp `t = TT.now().latest`. Then **before returning success to the client**, it *waits* until `TT.now().earliest > t`. That guarantees no future transaction can be assigned a timestamp earlier than the one just returned. Ordering is preserved by pausing enough that the uncertainty has passed.

Effect: transactions are ordered by real-world time. Any read at timestamp `T` sees every transaction that committed before `T`, everywhere in the world.

### Why this matters at Staff level

- "Why is `SELECT` at read-only mode instant?" — because Spanner reads at a chosen timestamp and doesn't need to coordinate with the writer (MVCC).
- "How does CockroachDB approximate this without atomic clocks?" — HLC (see 8C.2) plus clock-uncertainty windows. Weaker guarantee ("linearizability under bounded clock skew"), broadly good enough.
- "Why is external consistency ≥ linearizability?" — linearizability preserves per-object order; external consistency preserves order across the whole database.

### The interview one-liner

> "Spanner's contribution is not distributed transactions — those existed. It's using bounded clock uncertainty (TrueTime) plus a commit-wait to give external consistency across the globe at a couple of dozen milliseconds cost per transaction."

### Systems built on similar ideas

- **CockroachDB** — HLC instead of TrueTime.
- **YugabyteDB** — similar.
- **FaunaDB** — Calvin-style deterministic ordering.
- **AWS Aurora / DynamoDB global tables** — do not offer strict serializability across regions.

---

## 8C.2 Logical Time — Lamport, Vector Clocks, HLC

### Why physical time is not enough

Wall clocks skew. NTP jumps. Systems still need to know "did event A cause event B?" — that question can be answered without wall-clock time.

### Lamport timestamps

Each process keeps a counter `L`.
- On any local event: `L = L + 1`.
- On send: attach `L`.
- On receive of `m`: `L = max(L, m.L) + 1`.

Property: **if A causally happened before B, then `L(A) < L(B)`**. But **not** the converse — two unrelated events can have `L(A) < L(B)` without any causal link.

Good for **total ordering** (with ties broken by node id). Insufficient for detecting concurrent events.

### Vector clocks

Each process keeps a vector `V[i]` — one counter per known node.
- Local event: `V[me] += 1`.
- On send: attach `V`.
- On receive: `V[i] = max(V[i], m.V[i])` for all i; then `V[me] += 1`.

Property: **`A → B` (A happened before B) iff `V(A) < V(B)` component-wise**. If neither is `<` the other, they are **concurrent**.

**Downside:** vectors grow with cluster size. Dynamo capped this via pruning.

### Hybrid Logical Clocks (HLC)

Combine wall-clock time with a Lamport counter:
```
timestamp = (physicalMs, logicalCounter)
```

- On local event: pick `max(physicalMs, now)`; if unchanged, `logicalCounter++`.
- On send/receive: standard Lamport merge on both fields.

Property: monotonic, close to wall-clock (within skew bound), and captures causality. CockroachDB, YugabyteDB, MongoDB (5.0+) all use HLC.

### When you meet each

| Timestamp | System |
|---|---|
| Lamport | Simple message ordering in academic algorithms; rare in prod |
| Vector clock | Dynamo, Riak, Voldemort (conflict detection) |
| HLC | CockroachDB, YugabyteDB, MongoDB, Anna |
| TrueTime | Spanner |

### The gotcha

Vector clocks don't tell you *which* event to prefer when you detect concurrency — they just tell you a conflict exists. Resolution is application-level: last-writer-wins, merge, or expose to the user.

---

## 8C.3 Sharding & Placement — Consistent + Rendezvous Hashing

### Why not `key.hashCode() % N`?

Because when `N` changes (add/remove a shard), almost every key remaps. All caches invalidated. Storage rebalancing = full copy. Unusable at scale.

### Consistent hashing

Place both nodes and keys on a **ring** (hash space `[0, 2^32)`). A key belongs to the first node clockwise from its hash.

```
     Node A
       │
    ...........
   :           :
   :   ← keys hash        Node B
   :     go here   ────────►  │
   :                          :
    ..........................:
                              :
                        Node C
```

- Add node D → only keys between D and its predecessor move. Everyone else stays put.
- Remove node → its keys move to the next successor.

**Refinement — virtual nodes (vnodes):** each real node maps to `V` positions on the ring. Balances load (real nodes rarely land equally spaced) and softens the impact of node changes. Cassandra, DynamoDB, Riak all use vnodes.

### Rendezvous hashing (HRW — Highest Random Weight)

For each key, compute `hash(node_i, key)` for every node and pick the node with the highest hash.

- No ring; no vnodes.
- Add a node: only keys where the new node wins the top hash move. Same proportional movement as consistent hashing.
- Better load-balance out of the box (no vnodes needed).
- O(N) per lookup — fine when N ≤ hundreds.

Kafka uses rendezvous hashing internally for partition-consumer assignment when using cooperative sticky assignor.

### Which to pick

| Scenario | Pick |
|---|---|
| Cache cluster (memcached, Redis Cluster hash slots) | Consistent hashing |
| Storage / distributed DB | Consistent hashing with vnodes |
| Small N (< 100), balance matters | Rendezvous |
| CDN / edge routing | Consistent hashing (bounded loads variant) |

### The trap

Neither algorithm gives strong ownership guarantees during a rebalance — two nodes may briefly believe they own the same key. Use **fencing** at the storage layer if writes must be safe (Module 8 fencing tokens).

---

## 8C.4 Failure Detection — Heartbeats, Phi Accrual, Gossip / SWIM

### Why detection is hard

The only signal you have is *lack of a message*. Was the peer down, or was the network slow, or was your own clock off? Distributed systems literature calls this the "eventually perfect" failure detector — you can never be 100% sure, but you can be increasingly confident over time.

### Simple heartbeats

Node P sends `HEARTBEAT` every `Δ` ms. Node Q assumes P is dead if it hasn't heard in `k·Δ` ms.

Downside: static timeouts. If the network briefly hiccups, you flap. If your cluster is normally fast, you're slow to detect real failures.

### Phi Accrual (Cassandra, Akka)

Instead of a boolean "alive/dead," output a **suspicion level `φ`** that increases as time since the last heartbeat grows, calibrated by a running histogram of past interarrival times.

- `φ = 1` → 1-in-10 chance the peer is dead.
- `φ = 8` → 1-in-10⁸ chance you're wrong.

Set a threshold (Cassandra defaults to `φ_convict = 8`) and act. Adaptive to network jitter without you tuning static timeouts.

### Gossip protocols (SWIM specifically)

Instead of a central heartbeat, every node picks a random peer every T seconds and swaps a summary of who they think is alive. In O(log N) rounds, every fact reaches every node.

**SWIM (Scalable Weakly-consistent Infection-style Membership)** is the industry-standard gossip protocol:
- **Ping** a random peer.
- If no reply, ask K other peers to **indirect-ping** — asks "can you reach P?" This distinguishes "P is dead" from "I have a broken link to P."
- Piggyback membership updates on every ping.

Used in Consul, Hashicorp Serf, memberlist, Cassandra Gossiper.

### Where to see them

| Detector | System |
|---|---|
| Static timeouts | Kafka broker liveness (session.timeout.ms) |
| Phi accrual | Cassandra, Akka Cluster |
| Gossip (SWIM) | Consul, Serf, Nomad, Redis Cluster |
| Consensus-based | etcd, ZK — leader-lease-driven |

### The one you'll be asked about

**SWIM.** Interviewers love it because it demonstrates how to detect failures without a central coordinator, scale to thousands of nodes, and distinguish "peer down" from "path broken." Say indirect-ping if the topic comes up — that's the differentiator.

---

## 8C.5 CRDTs — Conflict-Free Replicated Data Types

### The problem

Two replicas make concurrent updates while partitioned. They rejoin. Which value wins? For simple key-value with `LWW` you drop one update. For structured data (a counter, a set, an edit log), you want the two updates to **merge** without human intervention.

### The math (skip if the interviewer isn't a CRDT nerd)

A CRDT defines a merge function `m(a, b)` that is **commutative, associative, and idempotent**. Given those, replicas that have seen the same set of updates in any order converge to the same state.

### The five you should be able to name

#### G-Counter (grow-only)

Vector of counters, one entry per node.
- Node `i` increments: `state[i] += n`.
- Merge: element-wise max.
- Read: sum of all entries.

Cannot decrement. Fine for "page views."

#### PN-Counter

Two G-Counters — one for increments, one for decrements. Read = `P.sum() - N.sum()`. Handles decrements while remaining conflict-free.

#### LWW-Register / LWW-Element-Set

Each write carries a timestamp; on conflict, keep the highest timestamp. Simple; loses concurrent updates. Cassandra uses this at the cell level.

#### OR-Set (Observed-Remove Set)

Each element carries a unique tag when added. Remove only affects the tags currently observed. Adds and removes commute. Solves the "add wins vs remove wins" question predictably.

#### RGA / LSEQ / Yjs (Ordered lists for collaborative text)

Positions encoded so concurrent inserts never conflict. Backbone of Google Docs, Figma, Notion collaborative editing.

### Real systems

| System | CRDT usage |
|---|---|
| Redis Enterprise (Active-Active) | PN-Counters, OR-Sets, LWW-Registers built in |
| Riak | CRDT map, set, counter types |
| Cassandra | LWW at the cell level |
| Automerge / Yjs | RGA-family for collaborative editing |
| CRDT Foundation datastores | Antidote, ElectricSQL |

### The trap

CRDTs converge, but they don't preserve *intent*. Two people concurrently "cancelling an order" and "shipping an order" merge into … a shipped-and-cancelled order. If your semantics don't survive merging, CRDTs are not enough — you need consensus (Module 8B.1).

### The L5 one-liner

> "CRDTs let you accept writes on any replica during a partition and merge them mechanically when the partition heals. They're the AP-side answer to 'how do we still function during a network split,' whereas Spanner is the CP-side answer."

---

## 🔥 Where L5 Candidates Fumble

| Trap | Bad answer | L5 answer |
|---|---|---|
| "Why does Spanner need atomic clocks?" | "For accuracy." | "To bound the clock-skew interval. Commit-wait pauses transactions long enough that timestamp ordering matches real-world ordering — the definition of external consistency." |
| "Lamport vs vector clock?" | "Different names." | "Lamport gives total order but can't detect concurrency. Vector clocks detect concurrency but grow with cluster size. HLC combines wall-clock and Lamport." |
| "Consistent hashing eliminates all rebalance cost?" | "Yes." | "It bounds it. Adding a node moves ~1/N of the keys, not everything. Vnodes improve balance." |
| "Cassandra detects failures how?" | "Heartbeats." | "Phi accrual — a running histogram of interarrival times converts absence into a suspicion level, so timeouts adapt to network conditions." |
| "SWIM — is that a fitness app?" | "…" | "Gossip-based membership with indirect-ping to distinguish node death from network path loss. Used in Consul, Serf, Nomad." |
| "Which CRDT for a shared counter that can decrement?" | "Lamport." | "PN-Counter — two G-Counters, one increment, one decrement. Read is the difference." |
| "External vs sequential vs linearizable?" | "All the same." | "Linearizable: real-time order per object. Sequential: consistent order without real-time. External: linearizable across the entire DB, globally." |

---

## 💻 Diagram-Level Summaries (memorize these)

### Consistent hashing ring with 3 nodes + 3 keys

```
       0 ─────────────────────
     /       [key1 ─► A]      \
    /                          \
   |    A ─────────── B         |
   |    |             |         |
   |    │  [key2 ─► C]│         |
   |    |             |         |
   |    C ─────────── (…)       |
    \    [key3 ─► C]           /
     \                        /
       (2^32) ────────────────
```

### HLC transition

```
localEvent:
  ptn = physicalNow()
  if ptn > l.p:  l = (ptn, 0)
  else:          l = (l.p, l.l + 1)
```

### PN-Counter

```
state = { P: G-Counter, N: G-Counter }
inc(n) → P.inc(n)
dec(n) → N.inc(n)
value  = P.sum() - N.sum()
merge  = element-wise max of both P and N
```

---

## 🏭 Production War Stories

**1. The DynamoDB rebalance stall.**
Team switched a Cassandra cluster to DynamoDB. Old code did `key.hashCode() % shardCount`. On DynamoDB's transparent resharding under load, some keys started landing on new nodes with warm caches. Old cache invalidation logic used the old hash → served stale for hours. Fix: hashed by DynamoDB's partition-key semantics, not raw modulo.

**2. The Cassandra false-positive flap.**
Cluster of 40 Cassandra nodes across two regions; cross-region latency spiked from 30 ms to 200 ms during an ISP incident. Static-heartbeat clients marked half the cluster dead → coordinators started re-routing → cascading load. Fix: phi accrual thresholds calibrated per rack; retries limited when a whole DC was suspected.

**3. The Consul gossip storm.**
Old cluster with 4,000 nodes on a slow LAN. Default SWIM gossip settings caused enough packet loss that indirect-pings quintupled — the gossip bandwidth alone saturated the network. Fix: tuned `gossip_interval` and `probe_interval`; split into WAN-federation of smaller DC-scoped clusters.

**4. The CRDT "we lost the cancellation."**
E-commerce team enabled active-active Redis. Two customers used two data centers within seconds; one added an item, the other removed it. Merged OR-Set kept it added — wrong semantics for their business (they wanted last-write-wins). Fix: switched that specific value to a versioned LWW register with client-supplied timestamps.

**5. The HLC clock jump.**
CockroachDB node's NTP jumped 400 ms backward at 02:00. HLC guardrail kicked in and refused new timestamps until wall clock caught up — service paused for 20 seconds. Fix: locked NTP to slew-only (`-x`) mode; disabled step corrections.

---

## 🎯 Self-Check

1. What is external consistency? What does Spanner do differently from a "normal" 2PC system to achieve it?
2. Lamport vs vector clock — one thing each can and can't do.
3. What is HLC? Which systems use it?
4. Consistent hashing — how many keys move when you add a node?
5. What problem do virtual nodes (vnodes) solve?
6. Rendezvous hashing — when do you pick it over consistent hashing?
7. Phi accrual — what does the number φ represent?
8. Draw the SWIM ping / indirect-ping flow and explain what each detects.
9. Which CRDT for a monotonically increasing counter? For a set with adds and removes?
10. Give a real system that uses each: TrueTime, HLC, phi accrual, SWIM, PN-Counter.

---

## 🏁 Distributed Track Complete

You have now covered the full L5 Staff distributed-concurrency surface:

| # | Module |
|---|---|
| 8 | [Distributed Concurrency (locks + DB)](./Module-08-Distributed-Concurrency.md) |
| 8B | [Coordination, Consensus & Events](./Module-08B-Distributed-Coordination-Consensus-Events.md) |
| 8C | Advanced Distributed Appendix (this file) |

Combined with Modules 1–10, you have material for:
- Any concurrency machine-coding round.
- Any distributed system-design round involving locks, transactions, ordering, or replication.
- Production incident reviews and diagnostics rounds.
- Modern JVM and Loom-era questions.

Optional next expansions if you want them:
- **Streaming / Flink concurrency** (stateful operators, exactly-once via 2PC-like commit protocol).
- **Actor model** (Akka/Erlang mailbox semantics, supervision hierarchies) — Meta likes this.
- **Formal methods**: TLA+ intuition — Amazon uses it for spec review.

Say the word if you want any of those.
