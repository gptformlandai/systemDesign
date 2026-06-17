# Key-Value Store (Redis-like) - End-to-End System Design

> Goal: practice one complete E2E storage engine problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for in-memory database and distributed cache systems.
- Start broad with requirements and scale, then zoom into command routing, sharding, replication, persistence, TTL, eviction, consistency, failover, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Redis-like systems, optimize low-latency reads/writes, simple data structures, predictable memory behavior, replication safety, and clear consistency trade-offs.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | Redis-like focus |
|---|---|---|
| Problem understanding | Can clarify DB/cache semantics | get/set/delete, TTL, data types, persistence, clustering |
| HLD | Can design storage infrastructure | client router, shard manager, primary-replica nodes, persistence, failover |
| LLD | Can model maintainable components | `Key`, `Value`, `Shard`, `Command`, `ExpiryIndex`, `ReplicationLog` |
| Machine coding | Can implement critical path | set/get/delete, TTL expiry, eviction, hash-slot routing |
| Traffic spikes | Can protect production | hot keys, cache stampede, memory pressure, failover storms |
| Global scale | Can reason across regions | partitioning, replication, consistency, resharding, disaster recovery |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Support `GET`, `SET`, `DELETE`.
- Support TTL/expiration.
- Support common data types: string, hash/map, list, set, sorted set if in scope.
- Support high-throughput low-latency operations.
- Support sharding across nodes.
- Support primary-replica replication.
- Support persistence through snapshots and/or append-only log.
- Support failover when primary fails.

Optional requirements to clarify:

- Is this a cache only or durable database?
- Do we need transactions or Lua/script-style atomic operations?
- Do we need pub/sub or streams?
- Is strong consistency required, or is eventual replica consistency acceptable?
- Is multi-region active-active required?
- What eviction policy is needed?

Out of scope unless interviewer asks:

- Full Redis protocol compatibility.
- Full SQL/query engine.
- Full CRDT active-active implementation.
- Full managed cloud control plane.

## 1.2 Non-Functional Requirements

Performance:

- Sub-millisecond to low-millisecond in-memory operations.
- High throughput per node.
- Predictable latency under TTL expiry and eviction.

Availability:

- Cluster should survive node failures.
- Clients should discover new primary after failover.
- Replicas should catch up after downtime.

Durability:

- Configurable persistence.
- AOF/snapshot recovery after restart.
- Users should understand data loss window.

## 1.3 Constraints

- Memory is expensive and finite.
- Single-threaded command execution simplifies atomicity but limits one-node throughput.
- Cross-shard transactions are hard.
- Replication is asynchronous by default in many Redis-like systems.
- Hot keys can overload one shard.
- Persistence can add write amplification and recovery cost.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Keys | billions |
| Value size | bytes to MB, usually small |
| QPS | millions/sec globally |
| Single-node memory | tens to hundreds of GB |
| P99 latency target | under 5-10 ms |
| Replication factor | 2-3 |
| Persistence mode | snapshot + append-only log |
| Availability target | 99.99% for cache/control-plane |

## 1.5 Capacity Math

Back-of-the-envelope:

- `1B keys * 100 bytes value` is already `100 GB` before metadata overhead.
- Metadata overhead can be comparable to or larger than small values.
- With replication factor 3, memory cost triples.
- A hot key with 100K QPS can overload one shard even if the cluster has many nodes.
- AOF persistence increases disk write throughput roughly proportional to write QPS.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| In-memory get/set | microseconds to low milliseconds |
| TTL precision | milliseconds to seconds depending design |
| Snapshot interval | minutes |
| AOF fsync policy | every write / every second / OS-managed |
| Replication lag | milliseconds to seconds under load |
| Hash slots | fixed large number, e.g. 16K-style slots |

## 1.6 Clarifying Questions To Ask

- Is durability required, or is data disposable cache?
- What operations/data types must be supported?
- Are replicas read-only and eventually consistent?
- What happens on primary failure?
- Should the cluster support online resharding?
- What eviction and memory policies are required?

Strong interview framing:

> I will design a Redis-like key-value store with hash-slot sharding, primary-replica replication, in-memory command execution, TTL/eviction, and configurable persistence through snapshots and append-only logs. I will be explicit that replicas are usually eventually consistent unless we pay extra latency for quorum writes.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Write flow:
Client -> Client Library / Proxy
       -> hash key to slot
       -> route to primary shard
       -> command executed in memory
       -> append to replication/AOF log
       -> async replicate to replicas
       -> response

Read flow:
Client -> route to primary or replica
       -> lookup in in-memory dictionary
       -> check TTL/expiry
       -> return value or miss

Failover flow:
Primary failure detected
  -> choose replica
  -> promote replica
  -> update cluster metadata
  -> clients reroute
```

Recommended architecture:

```text
Clients
  |
  v
+-----------------------+
| Client Lib / Proxy    |
| slot routing          |
+-----------+-----------+
            |
            v
+-----------------------+       +----------------------+
| Cluster Metadata      |<----->| Coordinator/Sentinel |
| slots -> primaries    |       | health/failover      |
+-----------+-----------+       +----------------------+
            |
            v
+-----------------------+       +----------------------+
| Primary Shard Node    |------>| Replica Shard Node   |
| memory + command loop |       | async replication    |
+-----------+-----------+       +----------------------+
            |
            v
+-----------------------+
| Persistence           |
| Snapshot + AOF        |
+-----------------------+
```

Request flow for `SET key value EX 60`:

1. Client computes hash slot for `key`.
2. Client routes command to primary owning slot.
3. Node checks memory limit and parses command.
4. Command loop updates key dictionary and expiry index.
5. Command is appended to AOF/replication log.
6. Replicas receive and apply command asynchronously.
7. Primary returns success.

## 2.2 APIs / Commands

Basic commands:

```text
SET key value [EX seconds]
GET key
DEL key
EXPIRE key seconds
TTL key
MGET key1 key2
INCR key
HSET key field value
HGET key field
```

Cluster commands:

```text
CLUSTER SLOTS
MIGRATE SLOT source target
REPLICAOF primary
```

Example client response:

```json
{
  "status": "OK",
  "slot": 8123,
  "node": "kv-node-7",
  "replicationOffset": 982233
}
```

Important API points:

- Single-key operations are easy to route.
- Multi-key operations need keys in same slot or special handling.
- TTL expiration may be lazy and active.
- Reads from replicas may be stale.

## 2.3 Core Components

Think of a Redis-like store as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Command plane | protocol parsing, command execution | low latency and atomic local ops |
| Storage plane | in-memory structures, TTL, eviction | fast access and memory control |
| Sharding plane | key -> slot -> node | horizontal scale |
| Replication plane | primary to replicas, offsets | availability and recovery |
| Persistence plane | snapshots/AOF | restart durability |
| Control plane | membership, failover, resharding | cluster operations |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Client Router | slot lookup and retry | key storage | client QPS |
| Command Processor | parse/execute commands | failover decisions | per-node QPS |
| In-Memory Store | key/value and data types | cluster metadata | memory/key count |
| Expiry Manager | TTL indexes and deletion | replication topology | expiring keys |
| Eviction Manager | memory pressure policy | app semantics | memory pressure |
| Replication Log | ordered mutations | client routing | write QPS |
| Persistence Manager | snapshots/AOF | command semantics | write/disk throughput |
| Coordinator | health, failover, slot ownership | data values | nodes/slots |

### In-Memory Storage Engine

Core structures:

- Hash table: `key -> value object`.
- Expiry map: `key -> expireAt`.
- Optional LRU/LFU metadata for eviction.
- Type-specific value objects: string, hash, list, set, sorted set.

Command execution:

- Single command loop can make per-key operations atomic without locks.
- Long-running commands must be avoided or broken up.
- Expired keys can be deleted lazily on access and actively in background.

Interview signal:

> Single-node Redis-like atomicity is simple because commands run serially on that node; cross-shard atomicity is the hard part.

### Sharding

Strategy:

- Compute `slot = hash(key) % N`.
- Assign many logical slots to physical nodes.
- Moving slots supports resharding without changing hash function.
- Client library/proxy caches slot map.

Hot-key challenge:

- Sharding distributes keys, not load for one key.
- Hot key can overload one primary.

Hot-key mitigations:

- Client-side caching.
- Read replicas.
- Key splitting for counters.
- Request coalescing.
- Application-level fanout cache.

### Replication And Failover

Replication:

- Primary writes to memory and replication log.
- Replicas pull/receive mutation stream.
- Replicas track replication offset.
- Initial sync uses snapshot + incremental log.

Failover:

- Coordinator detects primary failure.
- Choose replica with latest offset.
- Promote replica to primary.
- Update slot map.
- Clients refresh cluster metadata.

Consistency trade-off:

- Async replication gives low latency but can lose recent writes on failover.
- Synchronous/quorum replication reduces loss but increases latency.

### Persistence

Snapshot:

- Periodic point-in-time copy.
- Fast recovery baseline.
- Can lose writes since last snapshot.

Append-only file:

- Logs write commands.
- Can fsync every write, every second, or OS-managed.
- More durable but adds write amplification.

Recovery:

1. Load latest snapshot.
2. Replay AOF after snapshot offset.
3. Rejoin cluster and catch up replication.

### TTL And Eviction

Expiration:

- Lazy: check on access and delete if expired.
- Active: sample expiry index and delete expired keys periodically.

Eviction:

| Policy | Use |
|---|---|
| noeviction | fail writes when memory full |
| allkeys-lru | cache workloads |
| volatile-lru | evict only keys with TTL |
| allkeys-lfu | protect frequently used keys |
| random | simple fallback |

Interview signal:

> TTL expiration and eviction are not the same: expiration is time-based correctness; eviction is memory-pressure policy.

## 2.4 Data Layer

### Core Data Models

Key entry:

```json
{
  "key": "user:123",
  "type": "STRING",
  "valueRef": "memory-pointer",
  "expireAt": "2026-06-17T12:01:00Z",
  "version": 42
}
```

Slot metadata:

```json
{
  "slot": 8123,
  "primaryNode": "kv-7",
  "replicaNodes": ["kv-8", "kv-9"],
  "state": "STABLE"
}
```

Replication record:

```json
{
  "offset": 982233,
  "command": "SET",
  "key": "user:123",
  "args": ["value", "EX", "60"]
}
```

### Store Choices

| Data | Store | Why |
|---|---|---|
| Key/value data | process memory | low latency |
| TTL index | in-memory heap/map | expiration |
| AOF | local disk/SSD | persistence |
| Snapshot | local/object storage | restart/rebuild |
| Cluster metadata | consensus store/coordinator | failover/slot ownership |
| Metrics/logs | observability backend | operations |

### Partitioning

- Partition by hash slot.
- Assign slots to primary nodes.
- Replicate each slot group to replica nodes.
- Reshard by moving slots.
- Keep multi-key operations within same slot when possible.

### Replication And Consistency

- Primary is source of truth for a slot.
- Replicas are eventually consistent unless using sync replication.
- Read-your-writes is guaranteed only when reading from primary or sticky/session-aware replicas.
- Failover can lose acknowledged writes if replication was async and primary died before replica received them.

## 2.5 Scalability

### Horizontal Scaling

- Add nodes and redistribute slots.
- Split hot shards by moving slots.
- Use replicas to scale read-heavy workloads.
- Use client-side routing to avoid proxy bottlenecks.

### Resharding Strategy

1. Mark slot as migrating.
2. Copy keys to target node.
3. Forward or redirect commands during migration.
4. Switch slot owner in metadata.
5. Delete old copy after safe window.

### Hot Key Strategy

- Detect high QPS keys.
- Enable local/client caching for read-mostly keys.
- Split counters into multiple keys and aggregate.
- Use replicas for stale-tolerant reads.
- Apply per-key rate limiting if needed.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Client route lookup | microseconds |
| Network RTT | 0.1-2 ms in same region |
| Command parse/execute | microseconds to low ms |
| AOF append | depends on fsync policy |
| Replica ack | async or added latency if required |

### Optimization Rules

- Keep values reasonably small.
- Avoid blocking commands on hot path.
- Use pipelining/batching.
- Keep data structures compact.
- Use lazy + active expiry mix.
- Use zero-copy/network-efficient protocol where possible.

## 2.7 Async Systems

Use background tasks for:

- active expiry scanning
- eviction sampling
- snapshots
- AOF rewrite/compaction
- replica catch-up
- slot migration
- metrics export
- memory defragmentation if needed

Queue notes:

- Background tasks must not create large latency pauses.
- Snapshot/AOF rewrite should be incremental or copy-on-write aware.
- Slot migration needs safe redirects/retries.

## 2.8 Security And Multi-Tenancy

Security:

- TLS for client connections if needed.
- Auth tokens/passwords.
- ACLs by command/key prefix for multi-tenant usage.
- Encryption at rest for snapshots/AOF.

Multi-tenancy:

- Separate clusters for hard isolation where possible.
- Per-tenant memory and QPS quotas.
- Avoid noisy neighbor hot keys.

Abuse controls:

- Limit max value size.
- Block dangerous commands in managed environments.
- Rate-limit expensive operations.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Latency | p50/p95/p99 by command |
| Memory | used memory, fragmentation, eviction count |
| Replication | lag, offset gap, replica health |
| Persistence | AOF fsync latency, snapshot duration, recovery time |
| Cluster | slot movement, failover time, split-brain alerts |
| Hot keys | key QPS, CPU per shard, network egress |

Alerts:

- Memory near limit.
- Replication lag exceeds threshold.
- AOF fsync latency spikes.
- Primary unavailable or failover loop.
- Hot key overloads one shard.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Storage | in-memory | disk-backed | latency vs capacity/cost |
| Replication | async | sync/quorum | latency vs data-loss window |
| Persistence | snapshot | AOF | low overhead vs write durability |
| Sharding | client-side | proxy-side | performance vs operational simplicity |
| Execution | single-threaded commands | multi-threaded writes | simplicity/atomicity vs CPU scale |
| Eviction | LRU/LFU | no eviction | availability under pressure vs data retention |

Interview framing:

> I would design a Redis-like store with in-memory per-shard command execution, hash-slot partitioning, primary-replica replication, TTL/eviction, and configurable persistence. I would explicitly state the consistency and durability trade-offs.

---

# 3. Low-Level Design

LLD goal:

> Model the key-value store around commands, key entries, value objects, expiry indexes, slots, replication logs, and node state.

Simple rules:

- A key belongs to one slot.
- A slot has one primary at a time.
- Single-node commands are atomic in the command loop.
- Replicas can be stale unless synchronous replication is enabled.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `KeyEntry` | key, type, value, expiry | expired key is not visible |
| `ValueObject` | type-specific data | command must match type |
| `Shard` | slot range and in-memory map | primary handles writes |
| `ExpiryIndex` | expireAt lookup | deletes eventually |
| `EvictionPolicy` | victim choice | memory under limit |
| `ReplicationLog` | ordered mutations | replicas apply in order |
| `ClusterMetadata` | slot ownership | clients route correctly |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `CommandProcessor` | parse/execute commands | decide failover |
| `StorageEngine` | key/value memory | cluster coordination |
| `ReplicationService` | stream mutations | parse client protocol |
| `PersistenceService` | snapshot/AOF | route client requests |
| `ClusterCoordinator` | health/failover/slots | store key values |

## 3.2 OOP Fundamentals

Encapsulation:

- `KeyEntry` owns expiry visibility.
- `StorageEngine` owns type-safe command mutation.
- `Shard` owns slot membership.

Abstraction:

- `PersistenceLog` hides AOF implementation.
- `ClusterMetadataStore` hides consensus backend.
- `EvictionPolicy` hides replacement algorithm.

Polymorphism:

- Different value types.
- Different eviction policies.
- Different persistence policies.

Composition:

- `ShardNode` composes command processor, storage engine, replication, persistence, and metrics.

## 3.3 SOLID Principles

| Principle | Redis-like application |
|---|---|
| Single Responsibility | `ExpiryManager` only handles expiration |
| Open/Closed | add data type without rewriting cluster routing |
| Liskov Substitution | any eviction policy returns valid victims |
| Interface Segregation | separate command, replication, persistence, metadata APIs |
| Dependency Inversion | node depends on persistence/metadata interfaces |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Command | client operations | parse and execute uniformly |
| Strategy | eviction and persistence policy | configurable behavior |
| State | node role primary/replica/migrating | safe transitions |
| Observer/Event Publisher | mutations to replication/AOF | decouple side effects |
| Adapter | protocol and storage backends | isolate implementations |

## 3.5 UML / Diagrams

### SET Sequence

```text
Client -> Router: SET key value EX 60
Router -> ClusterMetadata: slot owner
Router -> PrimaryNode: command
PrimaryNode -> StorageEngine: set key/value
PrimaryNode -> ExpiryIndex: set expireAt
PrimaryNode -> AOF: append
PrimaryNode -> ReplicationLog: publish mutation
PrimaryNode -> Client: OK
ReplicationLog -> ReplicaNode: apply mutation
```

### Failover Sequence

```text
Coordinator -> Nodes: heartbeat check
Coordinator: primary failed
Coordinator -> ReplicaSet: choose latest replica
Coordinator -> MetadataStore: promote replica and update slots
Clients -> MetadataStore: refresh slot map
Clients -> NewPrimary: retry commands
```

## 3.6 Class Design

Interfaces:

```java
interface StorageEngine {
    Optional<ValueObject> get(String key);
    void set(String key, ValueObject value, Optional<Instant> expireAt);
    boolean delete(String key);
}

interface EvictionPolicy {
    Optional<String> chooseVictim(StorageStats stats);
    void recordAccess(String key);
}

interface ReplicationLog {
    long append(Mutation mutation);
    List<Mutation> readFrom(long offset, int limit);
}

interface ClusterRouter {
    NodeAddress route(String key);
}
```

Design notes:

- `get()` should check expiry before returning.
- `set()` should update value and expiry atomically in node loop.
- `append()` offset is used for replica catch-up.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| key expires but not actively deleted | lazy delete on access |
| memory full | eviction or reject write based on policy |
| primary dies before replica gets write | possible data loss in async mode |
| client has stale slot map | redirect and refresh |
| multi-key command spans slots | reject or require hash tag/same slot |
| replica promoted with lag | accept data-loss window or use quorum |
| AOF corrupted | recover to last valid command/checksum |
| hot key overload | client cache/read replicas/key splitting |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
kvstore/
  domain/
    KeyEntry.java
    ValueObject.java
    Shard.java
    Mutation.java
    ClusterSlot.java
  service/
    CommandProcessor.java
    ExpiryManager.java
    EvictionManager.java
    ReplicationService.java
  port/
    StorageEngine.java
    PersistenceLog.java
    ClusterMetadataStore.java
  adapter/
    InMemoryStorageEngine.java
  app/
    KvStoreDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from time import time
from typing import Dict, Optional


@dataclass
class Entry:
    value: str
    expires_at: Optional[float] = None


class InMemoryKvStore:
    def __init__(self, max_keys: int = 1000) -> None:
        self.data: Dict[str, Entry] = {}
        self.access_order: list[str] = []
        self.max_keys = max_keys

    def set(self, key: str, value: str, ttl_seconds: Optional[int] = None) -> None:
        self._expire_key(key)
        if key not in self.data and len(self.data) >= self.max_keys:
            self._evict_one()
        expires_at = time() + ttl_seconds if ttl_seconds is not None else None
        self.data[key] = Entry(value, expires_at)
        self._record_access(key)

    def get(self, key: str) -> Optional[str]:
        if self._expire_key(key):
            return None
        entry = self.data.get(key)
        if entry is None:
            return None
        self._record_access(key)
        return entry.value

    def delete(self, key: str) -> bool:
        existed = key in self.data
        self.data.pop(key, None)
        self.access_order = [k for k in self.access_order if k != key]
        return existed

    def _expire_key(self, key: str) -> bool:
        entry = self.data.get(key)
        if entry and entry.expires_at is not None and entry.expires_at <= time():
            self.delete(key)
            return True
        return False

    def _record_access(self, key: str) -> None:
        self.access_order = [k for k in self.access_order if k != key]
        self.access_order.append(key)

    def _evict_one(self) -> None:
        if self.access_order:
            self.delete(self.access_order[0])


store = InMemoryKvStore(max_keys=2)
store.set("a", "1")
store.set("b", "2")
store.set("c", "3")
print(store.get("a"), store.get("c"))
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[key -> Entry]` | primary lookup |
| `dict[key -> expireAt]` | TTL lookup |
| `heap/ordered set(expireAt,key)` | active expiry |
| `list/log[Mutation]` | AOF/replication |
| `dict[slot -> node]` | cluster routing |

## 4.4 Concurrency

High-signal concurrency issues:

- Concurrent commands on same key.
- Slot migration while commands arrive.
- Primary failover with in-flight writes.
- Snapshot/AOF rewrite while writes continue.

Handling strategy:

- Single command loop per shard or fine-grained locks.
- Migration state redirects/forwards commands.
- Idempotent client retries where possible.
- Copy-on-write snapshot or incremental persistence.

## 4.5 Testing Thinking

Unit tests:

- Set/get/delete.
- TTL expiry.
- Eviction when memory full.
- Multi-key slot validation.
- Replica applies mutations in order.

Load tests:

- Hot key QPS.
- Memory pressure with eviction.
- Failover under writes.
- Resharding during traffic.
- AOF recovery time.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Hot key | viral config/session key | one shard overload |
| Cache stampede | expired popular key | backend collapse |
| Memory pressure | many new keys | eviction storm or OOM |
| Failover storm | multiple node failures | stale slot maps and retries |
| Resharding spike | slot migration | latency and redirects |

## 5.2 Immediate Spike Response

1. Protect node memory and event loop latency.
2. Detect hot keys and enable client caching/coalescing.
3. Apply per-key/per-client rate limits.
4. Increase replicas for read-heavy hot keys.
5. Slow or pause resharding if latency spikes.
6. Use backpressure before OOM.
7. Degrade stale-tolerant reads to replicas if acceptable.

## 5.3 Degradation Policy

Protect in this order:

1. Cluster metadata and primary availability.
2. Memory safety.
3. Core get/set/delete.
4. Replication catch-up.
5. Persistence freshness.
6. Expensive commands and scans.

Not allowed:

- Split-brain primaries for same slot.
- Silent data corruption.
- Unbounded memory growth.
- Unsafe cross-slot transaction claims.

## 5.4 Spike Interview Answer

> During spikes I protect memory and primary command latency. Hot keys need detection, client caching, replicas, or key splitting. Failover and resharding must preserve one primary per slot, even if some reads become stale or expensive commands are throttled.

---

# 6. Scaling To Large Clusters

## 6.1 Global Architecture

```text
Clients
  -> local cluster router/proxy
  -> slot-partitioned primary nodes
  -> replicas in same region/AZs
  -> snapshots/AOF to durable storage
  -> optional cross-region async replication
```

## 6.2 Multi-Region Strategy

- Prefer regional clusters for low latency.
- Cross-region active-passive with async replication for DR.
- Active-active requires conflict resolution/CRDTs and is a different design.
- Use application-aware routing to keep reads/writes close.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Routing | client-side/proxy slot maps |
| Storage | add shards and move slots |
| Replication | primary-replica groups |
| Persistence | snapshots + AOF to durable storage |
| Failover | coordinator/sentinel/consensus metadata |
| Hot keys | caching, replicas, key splitting |
| Operations | metrics, resharding, backups |

## 6.4 Global Interview Answer

> I would scale a Redis-like store with hash-slot sharding, one primary per slot, replicas for availability/read scale, async or quorum replication depending durability needs, and persistence through snapshots plus AOF. The main trade-off is low latency versus consistency/durability.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

Use this answer flow:

```text
I will clarify cache vs durable DB, required commands, data types, TTL, eviction, persistence, replication, and consistency.
I will estimate key count, value size, QPS, memory, replication factor, AOF throughput, and hot-key risk.
HLD includes client router, slot metadata, primary shard nodes, replicas, persistence, coordinator, and monitoring.
Single-key operations route by hash slot and execute atomically on one primary.
Replication can be async for low latency or quorum for stronger durability.
Persistence uses snapshot and/or append-only log.
For spikes, I handle hot keys, memory pressure, failover, and cache stampede.
```

---

# 8. Fast Recall Rules

- Redis-like store is memory-first.
- Key maps to hash slot; slot maps to primary.
- Single-node command loop simplifies atomicity.
- Replicas are usually stale under async replication.
- Snapshot is not enough for minimal data loss; AOF narrows the window.
- TTL expiration and eviction are different.
- Hot keys are not fixed by normal sharding.
- Cross-shard transactions are hard.
- Failover must avoid split brain.
- Always state durability/consistency trade-offs clearly.
