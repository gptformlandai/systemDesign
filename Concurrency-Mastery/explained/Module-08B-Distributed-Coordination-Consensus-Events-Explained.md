# Module 8B — Distributed Coordination, Consensus & Event Concurrency (The Deep Dive)

> **Welcome to Staff-Level Territory!** 🎓 If Module 8 was "how do I make one operation safe across nodes?", this is "how do I make an entire **SYSTEM** safe across nodes over time?"

---

## 🎯 What You'll Master

By the end of this module, you'll be able to:
- Explain Raft consensus in three diagrams
- Defend a choice between 2PC / Saga / TCC in a design round
- Describe Kafka's exactly-once semantics precisely
- Pattern-match production incidents (thundering herd, split brain, cascading overload) to standard fixes
- Design resilient distributed systems with proper backpressure

---

## 🗺️ The Journey Ahead

```
                 ┌──────────────────────────────────────────────┐
                 │         MODULE 8B - STAFF LEVEL              │
                 │    "Making Systems Safe Over Time"           │
                 └──────────────────────────────────────────────┘
                                    │
    ┌───────────┬──────────┬───────┴────────┬──────────┬───────────┐
    │           │          │                │          │           │
    ▼           ▼          ▼                ▼          ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐     ┌─────────┐ ┌────────┐ ┌─────────┐
│ 8B.1  │  │ 8B.2   │  │ 8B.3   │     │  8B.4   │ │ 8B.5   │ │ 8B.6+   │
│Consen-│  │Consist-│  │Distrib.│     │  Kafka  │ │ Outbox │ │Rate Lim │
│sus    │  │ency    │  │ Txns   │     │Concurr. │ │ + CDC  │ │Cache,   │
│(Raft) │  │Models  │  │2PC/Saga│     │  EOS    │ │Debezium│ │Backpres.│
└───────┘  └────────┘  └────────┘     └─────────┘ └────────┘ └─────────┘
```

---

# Part 1: Consensus — Raft in Three Diagrams

## 🎯 Why We Care

Every distributed lock, config store, Kubernetes controller, database primary, and Kafka controller ultimately sits on a **consensus algorithm**.

Understand consensus and you understand:
- Why your infrastructure has **odd numbers** of replicas
- Why writes need **quorum**
- Why "the leader disappeared" is the same failure mode everywhere

**You don't need to hand-code Paxos.** You need to explain **Raft** clearly enough that an interviewer sees you've built systems on it.

---

## 🗳️ The Analogy: Committee Vote Over a Bad Phone Line

Imagine a **committee vote among unreliable members over a bad phone line**:

```
The Consensus Challenge:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                    THE COMMITTEE                            │
│                                                             │
│   ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐     │
│   │ F1  │    │ F2  │    │ F3  │    │ F4  │    │ F5  │     │
│   │     │    │     │    │     │    │     │    │     │     │
│   └──┬──┘    └──┬──┘    └──┬──┘    └──┬──┘    └──┬──┘     │
│      │          │          │          │          │         │
│      └──────────┴──────┬───┴──────────┴──────────┘         │
│                        │                                    │
│              ┌─────────▼─────────┐                         │
│              │   BAD PHONE LINE  │                         │
│              │   (the network)   │                         │
│              └───────────────────┘                         │
│                                                             │
│   CHALLENGES:                                              │
│   • Any member can drop the call                           │
│   • Any message can be delayed or lost                     │
│   • Yet they must agree on a SINGLE ordered sequence       │
│   • Any surviving majority must remember EVERY decision    │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 The Three Pieces of Raft

Raft's genius is splitting consensus into **three orthogonal sub-problems**:

```
Raft's Three Sub-Problems:
═══════════════════════════════════════════════════════════════

┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  (1) LEADER     │    │  (2) LOG        │    │  (3) SAFETY     │
│     ELECTION    │    │    REPLICATION  │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│                 │    │                 │    │                 │
│ "Who's in      │    │ "How do we      │    │ "How do we      │
│  charge?"      │    │  spread data?"  │    │  prevent loss?" │
│                 │    │                 │    │                 │
│ Randomized     │    │ AppendEntries   │    │ Up-to-date log  │
│ timeouts +     │    │ RPC + majority  │    │ rule + never    │
│ term numbers   │    │ acknowledgment  │    │ overwrite       │
│                 │    │                 │    │ committed       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

Let's explore each one!

---

## 📊 Diagram 1: Leader Election

```
Leader Election - The Happy Path:
═══════════════════════════════════════════════════════════════

Term 5: F1 is the leader, sending heartbeats

                    ┌─────────────────┐
                    │   F1 (Leader)   │
                    │    Term = 5     │
                    └────────┬────────┘
                             │
            heartbeat ♥      │      heartbeat ♥
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
    ┌─────────┐        ┌─────────┐        ┌─────────┐
    │   F2    │        │   F3    │        │   F4    │
    │Follower │        │Follower │        │Follower │
    │(timer   │        │(timer   │        │(timer   │
    │ resets) │        │ resets) │        │ resets) │
    └─────────┘        └─────────┘        └─────────┘

Each follower has an "election timer" (150-300ms, randomized)
Heartbeat resets the timer → "Leader is alive, stay follower"
```

```
Leader Election - When Leader Dies:
═══════════════════════════════════════════════════════════════

F1 crashes! No more heartbeats...

                    ┌─────────────────┐
                    │   F1 (DEAD) 💀  │
                    └─────────────────┘

         F2's timer: 200ms          F3's timer: 180ms (FIRES FIRST!)
         F4's timer: 250ms

Step 1: F3's timer fires first
┌─────────────────────────────────────────────────────────────┐
│  F3: "No heartbeat! I'm becoming a CANDIDATE!"             │
│  F3: Increments term to 6                                  │
│  F3: Votes for itself                                      │
│  F3: Sends "RequestVote" to F2, F4                        │
└─────────────────────────────────────────────────────────────┘

Step 2: Voting
┌─────────────────────────────────────────────────────────────┐
│  F2: "Term 6 > my term 5, and I haven't voted yet"        │
│  F2: Votes YES for F3                                      │
│                                                             │
│  F4: "Term 6 > my term 5, and I haven't voted yet"        │
│  F4: Votes YES for F3                                      │
└─────────────────────────────────────────────────────────────┘

Step 3: F3 wins!
┌─────────────────────────────────────────────────────────────┐
│  F3 has 3 votes (itself + F2 + F4) = MAJORITY of 5        │
│  F3 becomes LEADER for term 6                              │
│  F3 starts sending heartbeats                              │
│                                                             │
│  Any messages from F1 with term 5 are REJECTED            │
│  (old term = old news)                                     │
└─────────────────────────────────────────────────────────────┘
```

**The Randomization Trick:**
- Each node has a DIFFERENT random timeout (150-300ms)
- This prevents simultaneous elections (split vote)
- Simple but effective!

---

## 📊 Diagram 2: Log Replication

```
Log Replication - How Data Spreads:
═══════════════════════════════════════════════════════════════

Client sends: "SET x = 5"

Step 1: Leader appends to its log (uncommitted)
┌─────────────────────────────────────────────────────────────┐
│  Leader's Log:                                              │
│  ┌─────┬─────┬─────────┐                                   │
│  │ e1  │ e2  │ e3*     │  (* = uncommitted)                │
│  │t=4  │t=5  │t=6      │                                   │
│  │SET  │SET  │SET x=5  │                                   │
│  │a=1  │b=2  │         │                                   │
│  └─────┴─────┴─────────┘                                   │
└─────────────────────────────────────────────────────────────┘

Step 2: Leader sends AppendEntries to all followers
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  AppendEntries RPC contains:                               │
│  • term = 6                                                │
│  • prevLogIndex = 2                                        │
│  • prevLogTerm = 5                                         │
│  • entries = [e3]                                          │
│                                                             │
│  Follower checks: "Do I have entry at index 2 with term 5?"│
│  • YES → Append e3, send ACK                               │
│  • NO  → Reject, leader will back up and retry            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Step 3: Followers acknowledge
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Leader Log:    [ e1  e2  e3* ]                            │
│                                                             │
│  Follower 1:    [ e1  e2  e3  ] ← ACK ✓                    │
│  Follower 2:    [ e1  e2  e3  ] ← ACK ✓                    │
│  Follower 3:    [ e1  e2  _   ] (slow/stale)               │
│                                                             │
│  Leader counts: 3 ACKs (including itself) = MAJORITY!      │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Step 4: Leader commits!
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Leader: "Majority has e3, marking it COMMITTED"           │
│  Leader: Applies e3 to state machine (x = 5)              │
│  Leader: Tells followers "commit index = 3"               │
│  Leader: Responds to client "SUCCESS"                      │
│                                                             │
│  Follower 3 will eventually catch up via more              │
│  AppendEntries RPCs                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 📊 Diagram 3: Safety (Committed = Forever)

```
Safety Invariants - Why Data Is Never Lost:
═══════════════════════════════════════════════════════════════

INVARIANT 1: Election Restriction
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  To become leader, your log must be AT LEAST as            │
│  up-to-date as any other majority node's log.              │
│                                                             │
│  "Up-to-date" = higher term on last entry, OR              │
│                 same term but longer log                    │
│                                                             │
│  WHY: Prevents a stale node from becoming leader           │
│       and "forgetting" committed entries                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Example:
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Node A log: [ e1  e2  e3  e4 ]  (term 5 on e4)           │
│  Node B log: [ e1  e2  e3 ]      (term 5 on e3)           │
│  Node C log: [ e1  e2 ]          (term 4 on e2)           │
│                                                             │
│  If election happens:                                      │
│  • A can become leader (most up-to-date)                  │
│  • B can become leader (if A is down)                     │
│  • C CANNOT become leader (too stale)                     │
│                                                             │
│  This ensures committed entries are never lost!            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

INVARIANT 2: Leader Append-Only
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  A leader NEVER overwrites or deletes entries in its log.  │
│  It only APPENDS.                                          │
│                                                             │
│  Combined with Invariant 1:                                │
│  • Committed entries exist on a majority                   │
│  • New leader must have those entries                      │
│  • New leader won't overwrite them                        │
│  • Therefore: COMMITTED = COMMITTED FOREVER               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔢 Quorum Math (MEMORIZE THIS!)

```
Quorum Formula:
═══════════════════════════════════════════════════════════════

N replicas → Majority = ⌊N/2⌋ + 1
           → Tolerates f = ⌊(N-1)/2⌋ failures

┌─────────┬──────────┬────────────┬─────────────────────────┐
│ Nodes   │ Majority │ Tolerates  │ Notes                   │
├─────────┼──────────┼────────────┼─────────────────────────┤
│    3    │    2     │     1      │ Minimum for HA          │
│    4    │    3     │     1      │ Same as 3! Waste of $   │
│    5    │    3     │     2      │ Good for prod           │
│    6    │    4     │     2      │ Same as 5! Waste of $   │
│    7    │    4     │     3      │ High availability       │
└─────────┴──────────┴────────────┴─────────────────────────┘

KEY INSIGHT: Adding an EVEN-numbered node doesn't help!
             3 and 4 both tolerate 1 failure.
             5 and 6 both tolerate 2 failures.
             
             THIS IS WHY CLUSTERS HAVE ODD NUMBERS!
```

---

## 🏭 Where You Meet Raft in Production

```
┌─────────────────┬────────────────────────────────────────────┐
│     System      │              Consensus Role                │
├─────────────────┼────────────────────────────────────────────┤
│ etcd            │ Raft IS the entire protocol                │
│                 │ Backs Kubernetes' state                    │
├─────────────────┼────────────────────────────────────────────┤
│ Kafka (KRaft)   │ Metadata log via Raft                      │
│                 │ Replaces ZooKeeper (Kafka 3.0+)           │
├─────────────────┼────────────────────────────────────────────┤
│ CockroachDB     │ Per-range Raft groups for replication     │
├─────────────────┼────────────────────────────────────────────┤
│ Consul          │ Raft for KV / service registry            │
├─────────────────┼────────────────────────────────────────────┤
│ Redis Sentinel  │ NOT Raft! Lighter gossip + failover       │
│                 │ (weaker guarantees)                        │
└─────────────────┴────────────────────────────────────────────┘
```

---

## 🆚 Raft vs Paxos (The Interview Answer)

```
The History:
═══════════════════════════════════════════════════════════════

Multi-Paxos (Lamport, 1998)
├── Correct and proven
├── Paper is famously hard to understand
├── Even harder to implement correctly
└── Used by: Google Chubby, Spanner internals

Raft (Ongaro & Ousterhout, 2014)
├── Same guarantees as Paxos
├── Designed for UNDERSTANDABILITY
├── Decomposes into 3 clear sub-problems
└── Much easier to implement without subtle bugs

L5 ONE-LINER:
"They're equivalent in guarantees; Raft won because it 
decomposes consensus into three orthogonal sub-problems 
and is easier to build without subtle bugs."
```

---

# Part 2: Consistency Models — The Spectrum + CAP + PACELC

## 📝 The Analogy: The Shared Google Doc

Imagine a **shared Google Doc** you and 20 colleagues are editing simultaneously:

```
Consistency Models as Google Doc Behaviors:
═══════════════════════════════════════════════════════════════

LINEARIZABLE (Strongest)
┌─────────────────────────────────────────────────────────────┐
│  Everyone sees every edit in the SAME ORDER                │
│  the INSTANT it's typed.                                   │
│  Feels like one shared blackboard.                         │
│  "Alice typed 'hello' at 10:00:00.000 - everyone saw it    │
│   at 10:00:00.001"                                         │
└─────────────────────────────────────────────────────────────┘

SEQUENTIAL
┌─────────────────────────────────────────────────────────────┐
│  Same order for everyone, but everyone might be            │
│  seeing a slightly DELAYED view.                           │
│  "We all agree Alice typed before Bob, but I might         │
│   see it 100ms after you"                                  │
└─────────────────────────────────────────────────────────────┘

CAUSAL
┌─────────────────────────────────────────────────────────────┐
│  If A replies to B, everyone sees B before A.              │
│  Order between UNRELATED edits may differ.                 │
│  "If Bob replied to Alice, everyone sees Alice first.      │
│   But Charlie's unrelated edit? Who knows."                │
└─────────────────────────────────────────────────────────────┘

READ-YOUR-WRITES
┌─────────────────────────────────────────────────────────────┐
│  YOU see YOUR own edits immediately.                       │
│  Others may lag.                                           │
│  "I typed 'hello' and I see it. You might not yet."       │
└─────────────────────────────────────────────────────────────┘

EVENTUAL (Weakest)
┌─────────────────────────────────────────────────────────────┐
│  If editing stops, everyone EVENTUALLY converges.          │
│  No promise about WHEN.                                    │
│  "Stop typing for a minute and we'll all sync up...        │
│   eventually."                                             │
└─────────────────────────────────────────────────────────────┘

Real systems live somewhere on this spectrum.
You RARELY need the top!
```

---

## 📊 The Consistency Spectrum

```
┌────────────────────────┬─────────────────────────┬──────────┬─────────────────────┐
│        Model           │       Guarantee         │   Cost   │   Example Systems   │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Linearizable /         │ Global real-time order  │ Highest  │ Spanner, etcd,      │
│ Strict Serializable    │ One true sequence       │          │ CockroachDB SERIAL  │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Sequential             │ Global order, not tied  │ High     │ Rare in the wild    │
│                        │ to wall-clock time      │          │                     │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Snapshot Isolation     │ Each txn sees consistent│ Medium   │ Postgres RR,        │
│                        │ snapshot; ALLOWS write  │          │ Oracle, MySQL       │
│                        │ skew!                   │          │ InnoDB              │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Causal                 │ Preserves cause→effect  │ Medium   │ COPS, Bayou         │
│                        │ Unrelated may reorder   │          │                     │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Read-your-writes       │ You see your updates    │ Low-Med  │ Sticky sessions,    │
│                        │ immediately             │          │ DynamoDB consistent │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Monotonic reads        │ Once you see a value,   │ Low      │ Cassandra with      │
│                        │ later reads see that    │          │ token routing       │
│                        │ or newer                │          │                     │
├────────────────────────┼─────────────────────────┼──────────┼─────────────────────┤
│ Eventual               │ Given enough quiet time │ Lowest   │ Cassandra default,  │
│                        │ replicas agree          │          │ S3, DNS             │
└────────────────────────┴─────────────────────────┴──────────┴─────────────────────┘
```

---

## 🎭 CAP Theorem — The Honest Version

```
CAP Theorem:
═══════════════════════════════════════════════════════════════

When a PARTITION happens, you can pick:
• Consistency, OR
• Availability

NOT BOTH. Everything else is marketing.

        ┌─────────────────────────────────────────┐
        │              PARTITION                  │
        │         (network split)                 │
        └────────────────┬────────────────────────┘
                         │
           ┌─────────────┴─────────────┐
           │                           │
           ▼                           ▼
    ┌─────────────┐             ┌─────────────┐
    │ CONSISTENCY │             │AVAILABILITY │
    │             │             │             │
    │ Reject some │             │ Accept all  │
    │ requests to │             │ requests,   │
    │ stay correct│             │ risk stale  │
    └─────────────┘             └─────────────┘
```

**The Mistake Juniors Make:**
Treating CAP as a design-time knob. It isn't!

Partitions are **RARE**. You spend most of your life in the **non-partitioned** case. CAP says nothing about that!

Enter PACELC...

---

## 🎯 PACELC — The Model That Actually Matches Production

```
PACELC:
═══════════════════════════════════════════════════════════════

IF a Partition → choose Availability or Consistency
ELSE (normal ops) → choose Latency or Consistency

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   P A C E L C                                               │
│   │ │ │ │ │ │                                               │
│   │ │ │ │ │ └── Consistency (normal ops)                   │
│   │ │ │ │ └──── Latency (normal ops)                       │
│   │ │ │ └────── Else (when no partition)                   │
│   │ │ └──────── Consistency (during partition)             │
│   │ └────────── Availability (during partition)            │
│   └──────────── Partition                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Real Systems Mapped to PACELC

```
┌─────────────────┬───────────────┬───────────────┬───────────────────────┐
│     System      │ If Partition  │ Else (Normal) │        Notes          │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ DynamoDB        │      A        │       L       │ Fast, eventually      │
│ (default)       │               │               │ consistent            │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ MongoDB         │      A        │       L       │ Default behavior      │
│ (default)       │               │               │                       │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ Cassandra       │      A        │       L       │ AP system             │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ CockroachDB     │      C        │       C       │ Strong consistency    │
│                 │               │               │ always                │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ Spanner         │      C        │       C       │ Google's CP system    │
├─────────────────┼───────────────┼───────────────┼───────────────────────┤
│ Postgres        │      C        │       C       │ Single primary        │
│ (primary)       │               │               │                       │
└─────────────────┴───────────────┴───────────────┴───────────────────────┘
```

**Interview One-Liner:**
> *"CAP tells you what happens during a partition. PACELC is the design knob you actually turn — how much latency will I pay for consistency during normal operation?"*

---

## ⚠️ Consistency ≠ Isolation (Common Confusion!)

```
Two Different Concepts:
═══════════════════════════════════════════════════════════════

ISOLATION (SQL)
┌─────────────────────────────────────────────────────────────┐
│  A property of TRANSACTIONS against each other              │
│  on ONE database                                            │
│                                                             │
│  "How do concurrent transactions see each other's work?"   │
│                                                             │
│  Levels: READ UNCOMMITTED → READ COMMITTED →               │
│          REPEATABLE READ → SERIALIZABLE                    │
│                                                             │
│  This is Module 8 stuff!                                   │
└─────────────────────────────────────────────────────────────┘

CONSISTENCY (Distributed Systems)
┌─────────────────────────────────────────────────────────────┐
│  A property of REPLICAS against each other                 │
│  across a CLUSTER                                          │
│                                                             │
│  "How quickly do all replicas agree on the same value?"   │
│                                                             │
│  Levels: Linearizable → Sequential → Causal →             │
│          Read-your-writes → Eventual                       │
│                                                             │
│  This is Module 8B stuff!                                  │
└─────────────────────────────────────────────────────────────┘

THEY'RE ORTHOGONAL!

A Postgres primary can be SERIALIZABLE (top isolation)
yet be INCONSISTENT with an async replica (stale reads)!
```

---

# Part 3: Distributed Transactions — 2PC → Saga → TCC

## 🎯 The Problem

You need to update **TWO systems** atomically:
- An order in Postgres
- An inventory reservation in Redis

Either BOTH succeed or BOTH roll back.

Local `@Transactional` cannot help — it only works within ONE database!

```
The Distributed Transaction Problem:
═══════════════════════════════════════════════════════════════

┌─────────────────┐                    ┌─────────────────┐
│    Postgres     │                    │     Redis       │
│                 │                    │                 │
│  INSERT order   │                    │  RESERVE item   │
│                 │                    │                 │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         │         MUST BE ATOMIC!              │
         │    (both succeed or both fail)       │
         │                                      │
         └──────────────────┬───────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  HOW?! 🤔     │
                    └───────────────┘
```

---

## 🔧 Option A: Two-Phase Commit (2PC / XA)

```
Two-Phase Commit Flow:
═══════════════════════════════════════════════════════════════

PHASE 1: PREPARE (Voting)
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Coordinator                                                │
│      │                                                      │
│      │──── "PREPARE?" ────►  Participant A (Postgres)      │
│      │                           │                          │
│      │                           └── Locks resources        │
│      │                           └── Writes to WAL          │
│      │                           └── Votes YES ────────────►│
│      │                                                      │
│      │──── "PREPARE?" ────►  Participant B (Redis)         │
│      │                           │                          │
│      │                           └── Locks resources        │
│      │                           └── Votes YES ────────────►│
│      │                                                      │
│      │◄─── All voted YES                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

PHASE 2: COMMIT (Decision)
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Coordinator                                                │
│      │                                                      │
│      │──── "COMMIT!" ────►  Participant A                  │
│      │                           └── Makes changes durable  │
│      │                           └── Releases locks         │
│      │                                                      │
│      │──── "COMMIT!" ────►  Participant B                  │
│      │                           └── Makes changes durable  │
│      │                           └── Releases locks         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Why 2PC Is Largely Abandoned

```
2PC Problems:
═══════════════════════════════════════════════════════════════

PROBLEM 1: Blocking
┌─────────────────────────────────────────────────────────────┐
│  Resources are LOCKED between PREPARE and COMMIT           │
│  This can be SECONDS!                                      │
│                                                             │
│  Other transactions wait... and wait... and wait...        │
└─────────────────────────────────────────────────────────────┘

PROBLEM 2: Coordinator Crash
┌─────────────────────────────────────────────────────────────┐
│  If coordinator crashes AFTER prepare, BEFORE commit:      │
│                                                             │
│  Participants are stuck "IN-DOUBT"                         │
│  • Can't commit (didn't get the order)                    │
│  • Can't abort (might have been a commit)                 │
│  • Resources locked until coordinator recovers!            │
│                                                             │
│  No timeout is safe!                                       │
└─────────────────────────────────────────────────────────────┘

PROBLEM 3: Not Partition Tolerant
┌─────────────────────────────────────────────────────────────┐
│  Network partition = stuck transactions                    │
│  2PC assumes reliable network (it's not!)                  │
└─────────────────────────────────────────────────────────────┘

PROBLEM 4: XA Drivers Required
┌─────────────────────────────────────────────────────────────┐
│  Every participant needs XA-aware drivers                  │
│  Not all systems support XA                                │
│  Complexity explosion!                                     │
└─────────────────────────────────────────────────────────────┘

WHERE 2PC STILL LIVES:
Classic bank cores on IBM/Oracle stacks.
Mostly NOT new development.
```

---

## 🔧 Option B: Saga (The Modern Default!)

Break the distributed transaction into **local transactions**, each with a **compensating action** if a later step fails.

```
Saga Pattern:
═══════════════════════════════════════════════════════════════

Order Saga:

Step 1: Reserve Inventory
┌─────────────────────────────────────────────────────────────┐
│  Transaction T1: Reserve 5 units                           │
│  Compensation C1: Release 5 units                          │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼ (success)
Step 2: Charge Card
┌─────────────────────────────────────────────────────────────┐
│  Transaction T2: Charge $100                               │
│  Compensation C2: Refund $100                              │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼ (success)
Step 3: Create Shipping Label
┌─────────────────────────────────────────────────────────────┐
│  Transaction T3: Create label                              │
│  Compensation C3: Cancel label                             │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼ (success)
Step 4: Send Confirmation Email
┌─────────────────────────────────────────────────────────────┐
│  Transaction T4: Send email                                │
│  Compensation: None needed (email already sent, oh well)   │
└─────────────────────────────────────────────────────────────┘


IF STEP 3 FAILS:
═══════════════════════════════════════════════════════════════

T1 ✓ → T2 ✓ → T3 ✗ (FAILED!)
                │
                ▼
        Run compensations in REVERSE:
        C2: Refund $100
        C1: Release 5 units
        
        Result: System back to original state!
```

### Two Saga Flavors

```
CHOREOGRAPHY (Event-Driven):
═══════════════════════════════════════════════════════════════

┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Order    │───►│Inventory │───►│ Payment  │───►│ Shipping │
│ Service  │    │ Service  │    │ Service  │    │ Service  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
     │               │               │               │
     │   Events      │   Events      │   Events      │
     └───────────────┴───────────────┴───────────────┘
                          │
                    Message Broker

PROS:
• No single point of failure
• Loose coupling

CONS:
• Hard to visualize the flow
• Debugging = piecing together events across systems
• "Where did my order go?!"


ORCHESTRATION (Central Coordinator):
═══════════════════════════════════════════════════════════════

                    ┌──────────────────┐
                    │  SAGA            │
                    │  ORCHESTRATOR    │
                    │                  │
                    │  State Machine:  │
                    │  1. Reserve      │
                    │  2. Charge       │
                    │  3. Ship         │
                    │  4. Notify       │
                    └────────┬─────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
   ┌──────────┐        ┌──────────┐        ┌──────────┐
   │Inventory │        │ Payment  │        │ Shipping │
   │ Service  │        │ Service  │        │ Service  │
   └──────────┘        └──────────┘        └──────────┘

PROS:
• Readable state machine
• Easy to add steps
• Central retry/timeout logic
• Easy debugging

CONS:
• Orchestrator is a critical component
```

### Modern L5 Default: Orchestration!

> **Use Temporal.io, AWS Step Functions, or Camunda.**
> These give you durable state machines with automatic retries, timeouts, and versioning.
> **Say this in the interview!**

---

## 🔧 Option C: TCC (Try-Confirm-Cancel)

A middle ground for systems where compensation is unnatural.

```
TCC Pattern:
═══════════════════════════════════════════════════════════════

TRY Phase:
┌─────────────────────────────────────────────────────────────┐
│  Reserve capacity, but DON'T make it visible/final         │
│                                                             │
│  Example: "Reserve 500 seats" (not yet booked)             │
│  The seats are held but not sold                           │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼ (all TRYs succeeded)
CONFIRM Phase:
┌─────────────────────────────────────────────────────────────┐
│  Flip the reservation to FINAL                             │
│  Usually cheap and idempotent                              │
│                                                             │
│  Example: "Convert reservation to booking"                 │
│  Now the seats are officially sold                         │
└─────────────────────────────────────────────────────────────┘

OR (if any TRY failed):

CANCEL Phase:
┌─────────────────────────────────────────────────────────────┐
│  Release the reservation                                   │
│  Must be idempotent, must succeed                         │
│                                                             │
│  Example: "Release the 500 seats"                         │
│  Seats available for others again                          │
└─────────────────────────────────────────────────────────────┘
```

**Use TCC when:**
- High-value reservations
- "Undo" is hard or impossible (charging a card then refunding has fees!)
- Popular in fintech, seat inventory, trading systems

---

## 🎯 Which Pattern to Pick?

```
Decision Guide:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│  IF:                                                        │
│  • Only 2 systems                                          │
│  • Both XA-capable                                         │
│  • Short critical section                                  │
│  • No partition tolerance needed                           │
│                                                             │
│  THEN: 2PC (but really, reconsider...)                    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  IF:                                                        │
│  • Many services                                           │
│  • Business-level compensations make sense                 │
│  • Need partition tolerance                                │
│                                                             │
│  THEN: SAGA (orchestration) ← THE MODERN DEFAULT!         │
│        Use Temporal, Step Functions, or Camunda            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  IF:                                                        │
│  • High-value reservations                                 │
│  • "Undo" is hard/impossible                              │
│  • Need explicit reservation semantics                     │
│                                                             │
│  THEN: TCC                                                 │
└─────────────────────────────────────────────────────────────┘
```

### The Killer L5 Point

> *"Distributed transactions are a business-level design problem, not a database feature. The right question is 'what does compensation look like' — not 'how do I get atomicity across two databases.' Sagas make compensation explicit, which is why they scale."*

---

# Part 4: Kafka Concurrency — Consumer Groups, EOS-v2

Kafka is the **L5 canonical exam** of "distributed concurrency in an event system." Know it cold!

## 🎯 Partitions = The Unit of Parallelism

```
Kafka Topic Structure:
═══════════════════════════════════════════════════════════════

Topic: "orders"
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Partition 0: [msg1] [msg4] [msg7] [msg10] ...             │
│  Partition 1: [msg2] [msg5] [msg8] [msg11] ...             │
│  Partition 2: [msg3] [msg6] [msg9] [msg12] ...             │
│                                                             │
└─────────────────────────────────────────────────────────────┘

KEY POINTS:
• A topic has N partitions (you choose N)
• Each partition is an ORDERED log
• Ordering guarantees are PER PARTITION, not per topic!
• Producer picks partition via: key.hashCode() % N
```

```
Partition Selection:
═══════════════════════════════════════════════════════════════

Producer sends: {key: "user-123", value: "order data"}

hash("user-123") % 3 = 1

                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│  Partition 0: [...]                                        │
│  Partition 1: [...] [NEW MESSAGE HERE!]  ← user-123 always │
│  Partition 2: [...]                         goes here      │
└─────────────────────────────────────────────────────────────┘

Same key = Same partition = ORDERING GUARANTEED for that key!
```

---

## 👥 Consumer Groups & Rebalancing

```
Consumer Group Basics:
═══════════════════════════════════════════════════════════════

Topic "orders" with 4 partitions
Consumer Group "order-processors" with 2 consumers

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Partition 0 ──┐                                           │
│  Partition 1 ──┼──► Consumer A (processes P0, P1)          │
│                                                             │
│  Partition 2 ──┐                                           │
│  Partition 3 ──┼──► Consumer B (processes P2, P3)          │
│                                                             │
└─────────────────────────────────────────────────────────────┘

RULES:
• Each partition assigned to EXACTLY ONE consumer in the group
• One consumer can handle MULTIPLE partitions
• Adding consumers = partitions get redistributed
• More consumers than partitions = some consumers idle!
```

### Rebalancing: The Scary Part

```
What Triggers a Rebalance:
═══════════════════════════════════════════════════════════════

• Consumer joins the group (new pod deployed)
• Consumer leaves the group (pod dies)
• Consumer times out (slow processing)
• Partition count changes
• Consumer crashes

During rebalance: CONSUMPTION PAUSES!
```

### Rebalance Strategies

```
EAGER REBALANCE (Legacy - BAD!):
═══════════════════════════════════════════════════════════════

Step 1: ALL consumers stop consuming
Step 2: ALL partitions revoked
Step 3: Coordinator reassigns ALL partitions
Step 4: ALL consumers resume

┌─────────────────────────────────────────────────────────────┐
│  Consumer A: STOP ──────────────────────────────── RESUME  │
│  Consumer B: STOP ──────────────────────────────── RESUME  │
│  Consumer C: STOP ──────────────────────────────── RESUME  │
│                                                             │
│              ◄──── STOP THE WORLD! ────►                   │
│              Everyone pauses. Bad for latency!             │
└─────────────────────────────────────────────────────────────┘


COOPERATIVE STICKY REBALANCE (Kafka 2.4+, DEFAULT NOW):
═══════════════════════════════════════════════════════════════

Only MOVED partitions are paused!

┌─────────────────────────────────────────────────────────────┐
│  Consumer A: [P0, P1] ─────────────────────► [P0, P1]      │
│              (keeps consuming!)                             │
│                                                             │
│  Consumer B: [P2, P3] ── P3 moves ──► [P2]                 │
│              (P2 keeps going, P3 pauses briefly)           │
│                                                             │
│  Consumer C: [new!] ◄── gets P3 ──► [P3]                   │
│                                                             │
│  Much less disruption!                                     │
└─────────────────────────────────────────────────────────────┘
```

**L5 Tip:** Always confirm the client uses cooperative sticky. A team running 2.3-era eager rebalances silently doubles their p99 during deploys!

---

## 📬 Delivery Semantics

```
┌─────────────────┬─────────────────────────────┬─────────────────────────┐
│    Semantic     │           How               │         When            │
├─────────────────┼─────────────────────────────┼─────────────────────────┤
│ At-most-once    │ Auto-commit offsets BEFORE  │ Log metrics             │
│                 │ processing                  │ Loss is acceptable      │
├─────────────────┼─────────────────────────────┼─────────────────────────┤
│ At-least-once   │ Process, THEN commit        │ 99% of pipelines        │
│                 │ offsets manually            │ (with idempotent        │
│                 │                             │ consumers)              │
├─────────────────┼─────────────────────────────┼─────────────────────────┤
│ Exactly-once    │ Idempotent producer +       │ Money, dedup-sensitive  │
│ (EOS-v2)        │ Transactional producer +    │ pipelines               │
│                 │ read_committed consumer     │                         │
└─────────────────┴─────────────────────────────┴─────────────────────────┘
```

---

## 🔒 Idempotent Producer (No Dupes on Retry)

```
The Problem Without Idempotence:
═══════════════════════════════════════════════════════════════

Producer sends message → Network timeout → Producer retries

┌─────────────────────────────────────────────────────────────┐
│  Producer: send(msg) ──────────────────► Broker            │
│                                          (received!)        │
│            ◄── ACK lost in network! ──                     │
│                                                             │
│  Producer: "No ACK? Let me retry..."                       │
│            send(msg) ──────────────────► Broker            │
│                                          (received AGAIN!) │
│                                                             │
│  Partition now has: [msg] [msg]  ← DUPLICATE!              │
└─────────────────────────────────────────────────────────────┘
```

```
The Fix - Idempotent Producer:
═══════════════════════════════════════════════════════════════

enable.idempotence=true

Producer assigns sequence numbers per (producer, partition):

┌─────────────────────────────────────────────────────────────┐
│  Producer: send(msg, seq=42) ──────────► Broker            │
│                                          "seq 42, got it!" │
│            ◄── ACK lost ──                                 │
│                                                             │
│  Producer: send(msg, seq=42) ──────────► Broker            │
│                                          "seq 42 again?    │
│                                           Already have it! │
│                                           Ignoring."       │
│                                                             │
│  Partition has: [msg]  ← NO DUPLICATE!                     │
└─────────────────────────────────────────────────────────────┘
```

**Config:**
```properties
enable.idempotence=true
acks=all
max.in.flight.requests.per.connection=5  # OK with idempotence
retries=Integer.MAX_VALUE
```

---

## 🔐 Transactional Producer (EOS-v2)

For the full "read-process-write" atomicity:

```java
producer.initTransactions();

while (running) {
    ConsumerRecords<K, V> records = consumer.poll(Duration.ofSeconds(1));
    
    producer.beginTransaction();
    try {
        // Process and produce
        for (ConsumerRecord<K, V> record : records) {
            ProducerRecord<K, V> output = transform(record);
            producer.send(output);
        }
        
        // Commit offsets AS PART OF the transaction!
        producer.sendOffsetsToTransaction(
            offsets, 
            consumer.groupMetadata()
        );
        
        producer.commitTransaction();
        
    } catch (Exception e) {
        producer.abortTransaction();
        // Offsets NOT committed, will re-read and reprocess
    }
}
```

**Consumer side must set:**
```properties
isolation.level=read_committed
```

This skips messages from aborted transactions!

---

## 🚨 The Kafka Concurrency Traps (MUST KNOW!)

### Trap 1: Rebalance Storms During Deploy

```
The Problem:
═══════════════════════════════════════════════════════════════

20-pod consumer group, rolling deploy:

Pod 1 restarts → REBALANCE (all 20 pods affected)
Pod 2 restarts → REBALANCE (all 20 pods affected)
Pod 3 restarts → REBALANCE (all 20 pods affected)
...
Pod 20 restarts → REBALANCE

20 full rebalances = 4+ minutes of consumer downtime!
```

**Fix:** Static group membership!
```properties
group.instance.id=pod-unique-id
```
Short restarts don't trigger rebalance — Kafka waits for session timeout.

### Trap 2: Slow Processing → Rebalance

```
The Problem:
═══════════════════════════════════════════════════════════════

max.poll.interval.ms = 300000 (5 minutes, default)

Consumer polls batch of 500 messages
Processing takes 6 minutes (oops!)

Kafka: "Consumer hasn't polled in 5 minutes... DEAD!"
       *triggers rebalance*
       
Consumer: "I'm not dead! I was just busy!"
          *gets kicked out anyway*
```

**Fix:** 
- Tune batch size smaller
- Increase `max.poll.interval.ms`
- Or: poll into a bounded queue, process async

### Trap 3: Ordering Assumption Across Partitions

```
The Problem:
═══════════════════════════════════════════════════════════════

Producer sends:
  Event A (user-123) → Partition 0
  Event B (user-456) → Partition 1
  Event C (user-123) → Partition 0

Consumer might see: B, A, C  or  A, B, C  or  A, C, B

There is NO ordering guarantee across partitions!
```

**Fix:** If ordering matters, key by entity ID!
```java
// All events for user-123 go to same partition
producer.send(new ProducerRecord<>("topic", "user-123", event));
```

### Trap 4: Auto-Commit in Production

```
The Problem:
═══════════════════════════════════════════════════════════════

enable.auto.commit=true (default!)

Consumer polls messages
Auto-commit happens (every 5 seconds)
Consumer crashes BEFORE processing finishes

Messages are "committed" but NEVER PROCESSED!
Data loss! Bugs disappear silently!
```

**Fix:** Turn it off!
```properties
enable.auto.commit=false
```
Commit manually after successful processing.

### Trap 5: Zombie Writers

```
The Problem:
═══════════════════════════════════════════════════════════════

Old producer (epoch 1) gets network-partitioned
New producer (epoch 2) takes over
Old producer reconnects, tries to commit

Without fencing: both write! Corruption!
```

**Fix:** Transactional API uses **producer fencing** via epoch numbers.
Same concept as fencing tokens from Module 8!

---

# Part 5: Outbox Pattern + CDC

## 🎯 The Problem

You want to **update a DB row AND publish an event** atomically.

```java
@Transactional
void placeOrder(Order o) {
    orderRepo.save(o);           // DB write
    kafkaTemplate.send("orders", o);  // Kafka write - BUG!
}
```

**Failure modes:**
- Kafka down, DB commits → order exists, no event ever fired
- DB fails after Kafka published → phantom order downstream
- Process crashes between the two → non-deterministic state

**There is NO WAY to make `save + send` truly atomic — they're two systems!**

---

## 🔧 The Outbox Pattern

Write the event into the **SAME DB transaction** as the business row!

```
Outbox Pattern Flow:
═══════════════════════════════════════════════════════════════

Step 1: Single Transaction
┌─────────────────────────────────────────────────────────────┐
│  BEGIN TRANSACTION                                          │
│                                                             │
│  INSERT INTO orders (id, customer, total)                  │
│  VALUES (123, 'Alice', 99.99);                             │
│                                                             │
│  INSERT INTO outbox (aggregate, aggregate_id, event_type,  │
│                      payload)                               │
│  VALUES ('order', '123', 'OrderPlaced', '{"id":123,...}'); │
│                                                             │
│  COMMIT;  ← Both succeed or both fail!                     │
└─────────────────────────────────────────────────────────────┘

Step 2: Relay Process (Debezium or Polling)
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Debezium reads Postgres WAL (replication log)             │
│  Sees: "New row in outbox table!"                          │
│  Publishes to Kafka: orders.events topic                   │
│                                                             │
│  OR                                                         │
│                                                             │
│  Polling job: SELECT * FROM outbox WHERE sent = false      │
│  Publishes to Kafka                                        │
│  UPDATE outbox SET sent = true WHERE id = ?                │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Step 3: Consumer (Idempotent!)
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Consumer receives event                                   │
│  Checks: "Have I seen event ID abc-123 before?"           │
│  • YES → Skip (idempotent)                                │
│  • NO  → Process, record event ID                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Visual Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      APPLICATION                            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │              SINGLE TRANSACTION                      │  │
│   │                                                      │  │
│   │   orders table    │    outbox table                 │  │
│   │   ┌───────────┐   │    ┌───────────────────┐       │  │
│   │   │ id: 123   │   │    │ event_id: abc-123 │       │  │
│   │   │ total: 99 │   │    │ type: OrderPlaced │       │  │
│   │   └───────────┘   │    │ payload: {...}    │       │  │
│   │                   │    └───────────────────┘       │  │
│   └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Postgres WAL
                              ▼
                    ┌─────────────────┐
                    │    DEBEZIUM     │
                    │  (CDC Connector)│
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     KAFKA       │
                    │  orders.events  │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    CONSUMER     │
                    │  (Idempotent!)  │
                    └─────────────────┘
```

### Delivery Guarantee

- **At-least-once:** The relay may publish, crash before marking sent, and re-publish on restart
- **Consumers MUST be idempotent:** Dedupe by event ID

**This is THE canonical L5 answer for "how do you keep a DB and Kafka in sync."**

> Say: *"Outbox pattern with Debezium and idempotent consumers."*
> Interviewers relax.

---

# Part 6: Distributed Rate Limiting

Single-node rate limiting (Module 4) was easy. Now the token bucket must be **shared across a cluster**!

## 🔧 Option A: Redis Token Bucket (Lua Script)

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

-- Get current bucket state
local bucket = redis.call("HMGET", KEYS[1], "tokens", "ts")
local tokens = tonumber(bucket[1]) or capacity
local ts     = tonumber(bucket[2]) or now

-- Calculate refill since last request
local delta = math.max(0, now - ts) * refill / 1000
tokens = math.min(capacity, tokens + delta)

-- Check if we have enough tokens
if tokens < requested then
    -- Not enough - update state and reject
    redis.call("HMSET", KEYS[1], "tokens", tokens, "ts", now)
    redis.call("PEXPIRE", KEYS[1], 60000)
    return 0  -- REJECTED
end

-- Enough tokens - consume and allow
tokens = tokens - requested
redis.call("HMSET", KEYS[1], "tokens", tokens, "ts", now)
redis.call("PEXPIRE", KEYS[1], 60000)
return 1  -- ALLOWED
```

**Why this works:**
- Whole script executes **ATOMICALLY** on Redis single-threaded event loop
- Refill computed on-demand — no background timer needed
- `PEXPIRE` keeps keyspace bounded — idle buckets self-clean

## 🔧 Option B: Sliding Window Log (More Accurate)

```lua
-- Remove events older than window
redis.call("ZREMRANGEBYSCORE", KEYS[1], 0, now - window)

-- Count remaining
local count = redis.call("ZCARD", KEYS[1])

-- Check limit
if count >= limit then 
    return 0  -- REJECTED
end

-- Add this request
redis.call("ZADD", KEYS[1], now, now .. ":" .. math.random())
redis.call("PEXPIRE", KEYS[1], window * 2)
return 1  -- ALLOWED
```

- Accurate to the millisecond
- Costs more memory (one sorted-set entry per request)

## 🔧 Option C: Local Pre-Rejection + Centralized Reconciliation

```
The Hybrid Approach:
═══════════════════════════════════════════════════════════════

Cluster-wide limit: 10,000 rps
20 pods in the cluster

Each pod gets: 10,000 / 20 = 500 rps locally

┌─────────────────────────────────────────────────────────────┐
│  Pod 1: Local limit 500 rps                                │
│  Pod 2: Local limit 500 rps                                │
│  ...                                                        │
│  Pod 20: Local limit 500 rps                               │
│                                                             │
│  Periodically: Pull remaining budget from Redis            │
│  Adjust local limits based on actual usage                 │
└─────────────────────────────────────────────────────────────┘

TRADEOFF:
• Small over/undershoot possible
• Near-ZERO request latency overhead
• This is what Stripe, Cloudflare actually use!
```

### The Interview Trap

*"Redis is the bottleneck now."*

**Answer:**
- Use Redis Cluster with bucket key sharded by user/tenant
- Or a Redis proxy that supports script sharding
- Or accept Option C's soft-cap tradeoff

---

# Part 7: Cache Concurrency — Thundering Herd & Single-Flight

## 📊 The Four Cache-Write Strategies

```
┌─────────────────┬─────────────────────┬─────────────────────┬─────────────┐
│    Strategy     │     Read Path       │     Write Path      │ Consistency │
├─────────────────┼─────────────────────┼─────────────────────┼─────────────┤
│ Cache-aside     │ Miss → load from DB │ Write DB →          │ Weak        │
│ (lazy loading)  │ → populate cache    │ invalidate cache    │             │
├─────────────────┼─────────────────────┼─────────────────────┼─────────────┤
│ Write-through   │ Read cache; miss    │ Write goes to cache │ Stronger    │
│                 │ loads and populates │ AND DB synchronously│             │
├─────────────────┼─────────────────────┼─────────────────────┼─────────────┤
│ Write-behind    │ Read cache          │ Write to cache;     │ Weakest     │
│ (write-back)    │                     │ async flush to DB   │ durability  │
├─────────────────┼─────────────────────┼─────────────────────┼─────────────┤
│ Refresh-ahead   │ Read cache          │ Cache proactively   │ Good for    │
│                 │                     │ refreshes near TTL  │ hot keys    │
└─────────────────┴─────────────────────┴─────────────────────┴─────────────┘

Cache-aside is the DEFAULT. Every trap below is a cache-aside trap!
```

## 🚨 Trap 1: Cache Invalidation Race

```
The Race Condition:
═══════════════════════════════════════════════════════════════

T1: Cache miss → starts loading OLD value from DB
T2: Writes NEW value to DB → cache.delete()
T1: Finishes loading → populates cache with OLD value

Result: Cache has STALE data indefinitely!

Timeline:
─────────────────────────────────────────────────────────────
T1: read cache (miss)
T1: read DB ──────────────────────────────────────┐
                                                   │ (slow!)
T2: write DB (new value)                          │
T2: delete cache                                  │
                                                   │
T1: ◄─────────────────────────────────────────────┘
T1: write cache (OLD value!)

Cache is now STALE!
```

**Fixes:**
- **Write-through** for the field
- **Versioned entries** — cache stores `(value, version)`; only populate if version >= current
- **Short TTL** — accept staleness bounded by TTL (pragmatic default!)

## 🚨 Trap 2: Thundering Herd on Cache Expiry

```
The Thundering Herd:
═══════════════════════════════════════════════════════════════

Hot key expires at exactly time T

At T + 1ms:
┌─────────────────────────────────────────────────────────────┐
│  Request 1: Cache miss! → Query DB                         │
│  Request 2: Cache miss! → Query DB                         │
│  Request 3: Cache miss! → Query DB                         │
│  ...                                                        │
│  Request 5000: Cache miss! → Query DB                      │
│                                                             │
│  5000 concurrent DB queries for the SAME data!             │
│  💥 DATABASE DIES 💥                                       │
└─────────────────────────────────────────────────────────────┘
```

### Fix 1: Single-Flight / Request Coalescing

```java
ConcurrentHashMap<String, CompletableFuture<V>> inFlight = 
    new ConcurrentHashMap<>();

public V get(String key) {
    // Check cache first
    V cached = cache.getIfPresent(key);
    if (cached != null) return cached;
    
    // Single-flight: only ONE loader per key
    CompletableFuture<V> future = inFlight.computeIfAbsent(key, k ->
        CompletableFuture.supplyAsync(() -> {
            try {
                return loadFromDb(k);
            } finally {
                inFlight.remove(k);  // Release the slot
            }
        })
    );
    
    V value = future.join();  // All waiters get same result
    cache.put(key, value);
    return value;
}
```

```
Single-Flight in Action:
═══════════════════════════════════════════════════════════════

Request 1: Cache miss! → Creates CompletableFuture → Queries DB
Request 2: Cache miss! → Finds existing Future → WAITS
Request 3: Cache miss! → Finds existing Future → WAITS
Request 4: Cache miss! → Finds existing Future → WAITS
Request 5: Cache miss! → Finds existing Future → WAITS

DB query completes → ALL 5 requests get the result!
Only 1 DB query instead of 5!
```

**Guava's `LoadingCache` and Caffeine's `AsyncLoadingCache` do this internally!**

### Fix 2: Probabilistic Early Refresh (XFetch)

```
As TTL approaches, PROBABILITY of refresh increases:

TTL remaining: 100% → 0% chance of early refresh
TTL remaining: 50%  → 10% chance of early refresh  
TTL remaining: 10%  → 50% chance of early refresh
TTL remaining: 1%   → 90% chance of early refresh

Some request refreshes BEFORE expiry → no thundering herd!
```

### Fix 3: Stale-While-Revalidate

```
On cache expiry:
═══════════════════════════════════════════════════════════════

Request 1: Cache expired!
           → Return STALE value immediately
           → Trigger ASYNC refresh in background

Request 2: Gets stale value (fast!)
Request 3: Gets stale value (fast!)

Background refresh completes → cache updated

Request 4: Gets FRESH value

Trades staleness for stability!
```

## 🚨 Trap 3: Hot Key on Cluster

```
The Problem:
═══════════════════════════════════════════════════════════════

Product goes viral. All requests hash to SAME Redis node.

┌─────────────────────────────────────────────────────────────┐
│  Redis Cluster:                                            │
│                                                             │
│  Node 1: [normal load]                                     │
│  Node 2: [normal load]                                     │
│  Node 3: [💥 SATURATED! 💥] ← All "viral-product" requests│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Fixes:**
- **Replicated cache:** Cache hot key on EVERY node (client-side L1) with very short TTL
- **Randomized keys:** `hotkey#shard0..3` picked randomly; DB fanout on write
- **Aggressive local cache:** Caffeine in app tier absorbs hot reads before Redis sees them

## 🚨 Trap 4: Cache Stampede on Deploy

```
Rolling deploy = fresh JVM = empty local cache

Every pod starts hammering Redis/DB simultaneously!
```

**Fixes:**
- Warm-up phase on startup
- Gradual rollout with health checks after cache-warming
- Preloaded cache snapshot from S3

---

# Part 8: Distributed Backpressure — Circuit Breakers, Bulkheads

## 🔌 Circuit Breaker — The L5 Canonical Pattern

```
Circuit Breaker States:
═══════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────┐
│                                                             │
│    ┌──────────┐                         ┌──────────┐       │
│    │  CLOSED  │ ──── failures ────────► │   OPEN   │       │
│    │          │      exceed             │          │       │
│    │ (normal) │      threshold          │(fail fast│       │
│    └────▲─────┘                         └────┬─────┘       │
│         │                                    │             │
│         │                              timeout│             │
│         │                                    │             │
│         │         ┌──────────┐               │             │
│         │         │HALF-OPEN │◄──────────────┘             │
│         │         │          │                             │
│         │         │(probe)   │                             │
│         │         └────┬─────┘                             │
│         │              │                                   │
│         │   success    │   failure                         │
│         └──────────────┘──────────────► back to OPEN      │
│                                                             │
└─────────────────────────────────────────────────────────────┘

CLOSED: Calls pass through; failures counted
OPEN: Fail fast; don't even try downstream
HALF-OPEN: After cooldown, allow a PROBE call
           Success → CLOSED, Failure → back to OPEN
```

### Resilience4j Example

```java
CircuitBreaker cb = CircuitBreaker.of("payments",
    CircuitBreakerConfig.custom()
        .failureRateThreshold(50)           // >=50% failure → OPEN
        .slowCallRateThreshold(80)          // >=80% slow → OPEN
        .slowCallDurationThreshold(Duration.ofMillis(500))
        .slidingWindowSize(50)              // Last 50 calls
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .permittedNumberOfCallsInHalfOpenState(5)
        .build());

Supplier<Payment> call = CircuitBreaker.decorateSupplier(cb, 
    () -> gateway.charge(req));

Payment result = Try.ofSupplier(call)
    .recover(t -> Payment.fallback())
    .get();
```

## 🚧 Bulkhead — Isolate Failures

```
The Problem Without Bulkheads:
═══════════════════════════════════════════════════════════════

Your service calls 3 downstreams: A, B, C
You have 100 threads total

Downstream C becomes SLOW (10 second responses)

┌─────────────────────────────────────────────────────────────┐
│  Thread pool: 100 threads                                  │
│                                                             │
│  Calls to A: 5 threads (fast, done quickly)               │
│  Calls to B: 5 threads (fast, done quickly)               │
│  Calls to C: 90 threads (all STUCK waiting!)              │
│                                                             │
│  New requests to A and B? NO THREADS AVAILABLE!           │
│  One slow downstream took down EVERYTHING!                 │
└─────────────────────────────────────────────────────────────┘
```

```
The Fix - Bulkheads:
═══════════════════════════════════════════════════════════════

Dedicated pool per downstream:

┌─────────────────────────────────────────────────────────────┐
│  Pool for A: 30 threads max                                │
│  Pool for B: 30 threads max                                │
│  Pool for C: 30 threads max                                │
│                                                             │
│  C becomes slow:                                           │
│  • Pool C saturates (30 threads stuck)                    │
│  • Pools A and B UNAFFECTED!                              │
│  • Requests to A and B still work!                        │
│                                                             │
│  Failure is ISOLATED!                                      │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 The Standard Defense-in-Depth Chain

```
Request → [Rate Limiter] → [Bulkhead] → [Circuit Breaker] → [Timeout] → Downstream

Miss any layer and one incident cascades!
```

```java
// Resilience4j full defense-in-depth
RateLimiter rl = RateLimiter.of("gateway", 
    RateLimiterConfig.custom()
        .limitForPeriod(500)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .build());

Bulkhead bh = Bulkhead.of("gateway", 
    BulkheadConfig.custom()
        .maxConcurrentCalls(50)
        .build());

CircuitBreaker cb = CircuitBreaker.of("gateway", 
    CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .build());

TimeLimiter tl = TimeLimiter.of(Duration.ofMillis(500));

// Chain them all!
Callable<Response> decorated = Decorators
    .ofSupplier(() -> CompletableFuture.supplyAsync(
        () -> gateway.call(req), ioPool))
    .withRateLimiter(rl)
    .withBulkhead(bh)
    .withCircuitBreaker(cb)
    .withTimeLimiter(tl, scheduler)
    .decorate();

Response resp = Try.ofCallable(decorated)
    .recover(t -> Response.fallback())
    .get();
```

---

# Part 9: Split Brain, STONITH & Cluster Fencing

## 🧠 Split Brain

```
Split Brain Scenario:
═══════════════════════════════════════════════════════════════

Network partition splits your cluster:

        ┌─────────────────────────────────────────────────┐
        │              NETWORK PARTITION                  │
        │                    ║                            │
        │    Side A          ║          Side B            │
        │                    ║                            │
        │  ┌─────┐ ┌─────┐  ║  ┌─────┐ ┌─────┐          │
        │  │Node1│ │Node2│  ║  │Node3│ │Node4│          │
        │  └─────┘ └─────┘  ║  └─────┘ └─────┘          │
        │                    ║                            │
        │  "We're majority!" ║  "We're majority!"        │
        │  "Elect leader!"   ║  "Elect leader!"          │
        │                    ║                            │
        └─────────────────────────────────────────────────┘

Both sides think they're the majority!
Both elect a leader!
Both accept writes!

When partition heals: TWO DIVERGENT DATASETS! 💥
```

### Prevention: Quorum

```
Quorum Prevents Split Brain:
═══════════════════════════════════════════════════════════════

5-node cluster, partition: 3 nodes vs 2 nodes

Side A (3 nodes): 3 >= majority(3)? YES! → Can elect leader
Side B (2 nodes): 2 >= majority(3)? NO!  → Goes read-only

Only ONE side can accept writes!

4-node cluster, partition: 2 nodes vs 2 nodes

Side A (2 nodes): 2 >= majority(3)? NO!
Side B (2 nodes): 2 >= majority(3)? NO!

NEITHER side can elect! Total unavailability.
THIS IS WHY ODD NUMBERS MATTER!
```

## 💀 STONITH — "Shoot The Other Node In The Head"

```
STONITH Pattern:
═══════════════════════════════════════════════════════════════

When a node is SUSPECTED dead:

Step 1: FENCE it (make SURE it can't write)
┌─────────────────────────────────────────────────────────────┐
│  • Power-cycle via IPMI                                    │
│  • Cut its SAN access                                      │
│  • Fence its network port                                  │
│  • Revoke its credentials                                  │
└─────────────────────────────────────────────────────────────┘

Step 2: THEN promote the standby
┌─────────────────────────────────────────────────────────────┐
│  Only after fencing confirmed!                             │
│  Now safe to have a new leader.                           │
└─────────────────────────────────────────────────────────────┘

WITHOUT fencing:
┌─────────────────────────────────────────────────────────────┐
│  "Dead" node might come back                               │
│  Writes to shared storage                                  │
│  While standby ALSO writes                                 │
│  💥 DATA CORRUPTION 💥                                     │
└─────────────────────────────────────────────────────────────┘
```

### Modern Cloud Equivalents

```
Cloud STONITH:
═══════════════════════════════════════════════════════════════

AWS:
• Revoke IAM permissions
• Detach EBS volume
• Remove from ALB target group

Kubernetes:
• Pod eviction
• PodDisruptionBudgets (PDBs)
• Node cordon/drain
```

## 🔐 Two Layers of Defense

```
Defense in Depth:
═══════════════════════════════════════════════════════════════

Layer 1: Cluster-level fencing (STONITH)
┌─────────────────────────────────────────────────────────────┐
│  Kicks the node out of the network entirely                │
│  Node can't even REACH the storage                         │
└─────────────────────────────────────────────────────────────┘

Layer 2: Storage-level fencing (Fencing Tokens)
┌─────────────────────────────────────────────────────────────┐
│  Even if node somehow reaches storage                      │
│  Storage rejects writes with old tokens                    │
└─────────────────────────────────────────────────────────────┘

Client A holds lock (token=42) but GC-paused
Cluster fencing: kicks A out of network (STONITH)
Client B acquires lock (token=43)
A wakes, tries to write:
  → Rejected by network (fenced)
  → Even if it got through: rejected by storage (token 42 < 43)

TWO independent layers = much safer!
```

---

# Part 10: Production War Stories

## 💥 War Story 1: The Raft Cluster That Lost Its Majority

**Setup:** Team ran a 4-node etcd cluster "for more capacity"

**What Happened:**
```
4-node cluster: majority = 3

Node 1 crashes → 3 remaining, majority = 3 → BARELY OK
Network hiccup → Node 2 unreachable → 2 remaining

2 < 3 = NO MAJORITY = CLUSTER HALTED!

Kubernetes control plane down for 2 hours.
```

**Fix:** Rebuilt as 5 nodes. Documented that **etcd sizes must be odd**.

## 💥 War Story 2: The 2PC Coordinator Crash

**Setup:** Legacy service used XA across DB2 and MQ

**What Happened:**
```
Coordinator: PREPARE → DB2 (YES) → MQ (YES)
Coordinator: *CRASHES*

DB2: "I voted YES... waiting for COMMIT or ABORT..."
MQ:  "I voted YES... waiting for COMMIT or ABORT..."

Both held locks for 4 HOURS until ops manually forced commit.
All downstream writes stalled.
```

**Fix:** Migrated to Saga with orchestration on Temporal. XA retired.

## 💥 War Story 3: The Kafka Rebalance Storm

**Setup:** 20-pod consumer group, rolling deploy

**What Happened:**
```
Pod 1 restarts → Full rebalance (all 20 pods stop)
Pod 2 restarts → Full rebalance (all 20 pods stop)
...
Pod 20 restarts → Full rebalance

20 full "stop-the-world" rebalances
4-minute consumer downtime during deploy!
```

**Fix:** 
- Enabled cooperative sticky assignor
- Added static group membership
- Deploy-time downtime dropped to seconds

## 💥 War Story 4: The Outbox That Lost Events

**Setup:** Team put events in an in-memory queue, flushed to Kafka in `@TransactionalEventListener(AFTER_COMMIT)`

**What Happened:**
```
Kafka was down → Events dropped from memory
No persistence → Events LOST FOREVER

Retro found ~1,200 lost events during a prior outage.
```

**Fix:** Proper outbox table + Debezium. Events now durable.

## 💥 War Story 5: The Thundering-Herd Blackout

**Setup:** Homepage feature-flag cache had 30-second TTL

**What Happened:**
```
Every 30 seconds: ALL pods refetch from LaunchDarkly
Vendor rate-limited them → All requests failed
All pods entered fallback → Homepage 500-ed globally
```

**Fix:**
- Single-flight (request coalescing)
- Jittered TTL (30s ± 5s)
- Stale-while-revalidate

## 💥 War Story 6: The Split-Brain Outage

**Setup:** HA Postgres with fencing agent misconfigured

**What Happened:**
```
Network partition → Both nodes promoted themselves
Both accepted writes for 6 minutes
Partition healed → Two divergent WALs
Manual data reconciliation over a weekend
```

**Fix:** Added real IPMI STONITH, tested regularly with chaos drills.

## 💥 War Story 7: The Circuit Breaker That Never Opened

**Setup:** `failureRateThreshold=50`, `slidingWindowSize=100`

**What Happened:**
```
Downstream degraded slowly:
• 30% failures (below 50% threshold)
• 70% slow-but-successful

Breaker stayed CLOSED!
Cascading timeouts consumed the thread pool.
```

**Fix:** Added `slowCallRateThreshold=60` with `slowCallDurationThreshold=500ms`. Breaker now opens on latency, not just errors.

---

# Part 11: Interview Traps & L5 Answers

## 🎯 Quick Reference Table

| Trap | Bad Answer | L5 Answer |
|------|------------|-----------|
| "How does Raft work?" | "It's a consensus algorithm." | "Leader-based Multi-Paxos variant with three sub-problems: election with randomized timeouts, log replication via AppendEntries, safety via up-to-date-log rule. Commits after majority replication." |
| "CAP: pick 2." | "AP or CP." | "CAP is about partition-time behavior only. PACELC is the honest model — during normal operation you're trading latency for consistency, not availability." |
| "How do I get atomic write to DB + Kafka?" | "2PC." | "Outbox pattern. Same DB txn writes business row and outbox row; Debezium ships outbox to Kafka; consumers idempotent by event id." |
| "Which transaction pattern for microservices?" | "2PC." | "Saga, orchestration-flavor, on Temporal or Step Functions. 2PC blocks resources and can't tolerate partitions." |
| "Kafka exactly-once — real?" | "No." | "Yes for read-process-write pipeline: idempotent producer + transactional producer + read_committed consumer. Not for external side effects — those need consumer-side idempotency." |
| "How do I prevent split brain?" | "Careful monitoring." | "Quorum-based writes only + node fencing on suspected failures + leader leases. Never rely on 'careful monitoring.'" |
| "Distributed rate limiter — how?" | "Redis INCR." | "Atomic Lua script — token bucket or sliding window. Then either cluster-sharded or a local pre-rejection layer that reconciles with Redis." |
| "Thundering herd on cache expiry — fix?" | "Longer TTL." | "Single-flight (request coalescing) so only one loader hits DB. Optionally probabilistic early refresh or stale-while-revalidate." |

---

## 🎯 Self-Check Questions

1. Three sub-problems Raft decomposes consensus into.
2. Quorum for N=5. How many failures tolerated?
3. Explain PACELC in one sentence and give an example system for each quadrant.
4. When would you choose TCC over a Saga?
5. What guarantees does Kafka EOS-v2 give and what does it NOT cover?
6. Draw the Outbox pattern. Why is at-least-once acceptable if consumers dedupe?
7. Two Redis rate-limit approaches — how do they differ in accuracy and cost?
8. What is a thundering herd on cache expiry? Name two independent fixes.
9. Draw the standard defense-in-depth resilience chain (five layers).
10. Why must clusters have odd sizes? What is STONITH?

---

## 🎓 The Staff-Level Mantras

> *"Consensus is the price of durability. Only writes that must survive a crash go through it."*

> *"CAP tells you what happens during a partition. PACELC is the design knob you actually turn."*

> *"Distributed transactions are a business-level design problem, not a database feature."*

> *"Outbox pattern with Debezium and idempotent consumers."*

> *"Single-flight for thundering herd. Bulkheads for isolation. Circuit breakers for cascading failures."*

---

## ➡️ Next Module

Move to **Module 8C — Advanced Distributed Appendix** (Spanner + TrueTime, vector clocks / Lamport / HLC, consistent hashing, gossip / failure detection, CRDTs).

---

*"In distributed systems, everything that can go wrong will go wrong — just not all at once, and not in the order you expected."* 🌐
