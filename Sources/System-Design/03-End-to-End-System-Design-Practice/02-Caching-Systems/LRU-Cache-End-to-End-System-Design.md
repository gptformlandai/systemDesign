# LRU Cache - End-to-End System Design

> Goal: design and implement a Least Recently Used cache that provides O(1) get/put operations, evicts stale entries predictably, and scales from machine-coding interviews to production local-cache use cases.

---

## How To Use This File

- Use this when the interview problem says LRU cache, local cache, eviction policy, hashmap plus linked list, or O(1) cache design.
- Start with the data structure invariant, then discuss concurrency, memory limits, TTL, metrics, and production trade-offs.
- For machine coding, the expected solution is a hashmap plus doubly linked list.
- For system design, explain where LRU helps and where it becomes risky.

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

| Layer | Interview signal | LRU cache focus |
|---|---|---|
| Problem understanding | Can define cache behavior | capacity, get, put, update, evict least recently used |
| HLD | Can place cache correctly | in-process cache, service cache, database cache, CDN edge comparison |
| LLD | Can maintain O(1) invariants | hashmap, doubly linked list, head/tail sentinels |
| Machine coding | Can implement cleanly | move-to-front, remove tail, update existing key |
| Traffic spikes | Can avoid cache collapse | hot key handling, cache stampede, bounded memory |
| Billion users | Can discuss limits | local vs distributed cache, invalidation, sharding, observability |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Support `get(key)` in O(1).
- Support `put(key, value)` in O(1).
- Store at most `capacity` entries.
- When capacity is exceeded, evict the least recently used entry.
- A successful `get` makes that key most recently used.
- Updating an existing key should update value and make it most recently used.
- Return a miss result when key does not exist.
- Keep memory bounded.

Common optional requirements:

- Support TTL expiration.
- Support size-based eviction, not just entry-count capacity.
- Support thread-safe operations.
- Emit hit/miss/eviction metrics.
- Support negative caching for missing values.
- Support cache invalidation by key or namespace.

Out of scope unless asked:

- Full distributed cache cluster.
- Persistent storage.
- Complex admission policy.
- Cache warming pipeline.
- Multi-level cache hierarchy.

## 1.2 Non-Functional Requirements

Performance:

- O(1) average-time `get`.
- O(1) average-time `put`.
- Small memory overhead per entry.
- Low lock contention if thread-safe.

Correctness:

- Linked list order must always match recency.
- Hashmap must point to valid nodes.
- Evicted nodes must be removed from both hashmap and list.
- Capacity must never be exceeded after a completed `put`.

Operations:

- Track cache hit ratio.
- Track eviction count.
- Track memory usage.
- Avoid unbounded key/value growth.

## 1.3 Constraints

- Hashmap gives fast lookup but not recency order.
- Linked list gives fast reordering but not lookup by key.
- Combining both provides O(1) lookup and O(1) recency update.
- A single global lock is simple but may reduce throughput under concurrency.
- LRU is not always optimal for scans or bursty one-time reads.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Local cache capacity | 10K to 10M entries depending memory |
| Operation target | O(1) average time |
| Value size | bytes to KBs for local cache |
| Hit latency | microseconds to low milliseconds |
| Eviction policy | least recently used |
| Thread model | single-threaded for coding, locked/segmented for production |

Back-of-the-envelope:

- A million entries can be expensive because each entry stores key, value, node pointers, and hashmap overhead.
- If average value is 1 KB, 1M values alone is about 1 GB before object overhead.
- Local cache hits are much faster than network calls, but every app instance has a different cache view.

## 1.5 Clarifying Questions To Ask

- What should `get` return on miss?
- Is capacity measured by number of entries or bytes?
- Should `get` update recency?
- Does `put` on existing key update recency?
- Is TTL required?
- Is thread safety required?
- Should null values be allowed?
- Are keys/values generic or fixed type?

Strong interview framing:

> I will implement LRU with a hashmap from key to linked-list node and a doubly linked list ordered from most-recent to least-recent. Every successful read or update moves the node to the front. When capacity is exceeded, I remove the tail node and delete it from the hashmap.

---

# 2. High-Level Design

## 2.1 Architecture

Local LRU cache inside a service:

```text
Client Request
  -> Service
  -> LRU Cache lookup
       hit  -> return cached value
       miss -> fetch from database/API
            -> store in LRU cache
            -> return value
```

Internal structure:

```text
HashMap: key -> Node

Most recent                                      Least recent
head <-> Node A <-> Node B <-> Node C <-> tail
```

Operation flow:

| Operation | Hashmap action | Linked-list action |
|---|---|---|
| `get(existing)` | find node by key | move node to head |
| `get(missing)` | no node found | no list change |
| `put(new)` | add key to map | insert node after head |
| `put(existing)` | update node value | move node to head |
| capacity exceeded | remove key from map | remove node before tail |

## 2.2 APIs

Machine-coding API:

```text
LRUCache(capacity)
get(key) -> value or -1/null/Optional.empty
put(key, value) -> void
```

Production-style API:

```http
GET /cache/{key}
PUT /cache/{key}
DELETE /cache/{key}
GET /cache/metrics
```

Typical interface:

```java
interface Cache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
    void invalidate(K key);
    CacheStats stats();
}
```

## 2.3 Core Components

Think of LRU Cache as two structures with one shared invariant.

| Component | Owns | Why it exists |
|---|---|---|
| Hashmap | key to node lookup | O(1) access |
| Doubly linked list | recency order | O(1) move and eviction |
| Head sentinel | most-recent boundary | simpler insertion |
| Tail sentinel | least-recent boundary | simpler eviction |
| Node | key, value, prev, next | bridge between map and list |
| Capacity policy | max entry count or bytes | bounded memory |
| Stats collector | hits, misses, evictions | observability |

### Invariant

The whole LRU cache depends on this invariant:

```text
Every key in map points to exactly one node in the list.
Every real node in the list has exactly one key in the map.
Nodes closer to head are more recently used.
Node before tail is the least recently used.
```

### Why Doubly Linked List

| Need | Singly linked list | Doubly linked list |
|---|---|---|
| remove arbitrary node | needs previous pointer search | O(1) using node.prev |
| move node to front | harder | O(1) remove plus add |
| evict tail | possible with extra tracking | simple with tail.prev |

### LRU In Production

Common use cases:

- in-process database row cache,
- API response cache,
- compiled template cache,
- token/JWKS cache,
- feature flag cache,
- metadata cache,
- expensive calculation cache.

Where LRU struggles:

| Pattern | Why LRU struggles | Better option |
|---|---|---|
| full table scan | one-time reads evict hot items | scan-resistant cache, segmented LRU |
| tiny hot set plus huge cold stream | cold stream pollutes cache | admission policy |
| frequency matters more than recency | recent one-hit keys win | LFU or TinyLFU |
| distributed correctness required | local LRU is inconsistent | distributed cache or database source of truth |

One-stop interview answer:

> LRU is a local, bounded, recency-based cache. I use a hashmap for O(1) lookup and a doubly linked list for O(1) recency updates and tail eviction. It is excellent for repeated recent access, but it needs TTL, invalidation, concurrency control, and metrics in production.

---

# 3. Low-Level Design

LLD goal:

> Maintain the hashmap/list invariant after every operation.

Starter map:

| LLD question | LRU answer |
|---|---|
| Main lookup | `Map<K, Node<K,V>>` |
| Recency structure | doubly linked list |
| Most recent node | `head.next` |
| Least recent node | `tail.prev` |
| Get hit behavior | move node to head |
| Put existing behavior | update value and move to head |
| Put new behavior | insert after head |
| Eviction behavior | remove `tail.prev` |

Beginner-friendly design order:

1. Define node with key, value, prev, and next.
2. Create dummy head and tail.
3. Write `addAfterHead(node)`.
4. Write `removeNode(node)`.
5. Write `moveToHead(node)`.
6. Write `removeTail()`.
7. Implement `get` using map lookup plus move.
8. Implement `put` using update/insert plus eviction.

Interview sentence:

> The helper methods are the real design. Once add, remove, move, and removeTail are correct, get and put become small and the invariant stays easy to reason about.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `Node` | stores key, value, prev, next | if in list, prev and next are non-null |
| `LRUCache` | orchestrates map and list | map size equals real list node count |
| `EvictionPolicy` | chooses victim | for LRU, victim is tail.prev |
| `CacheStats` | tracks hit/miss/eviction | counters are monotonic |
| `Clock` | TTL support if needed | avoid direct time dependency in tests |

## 3.2 Class Sketch

```java
final class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev;
    Node<K, V> next;
}

final class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>();
    private final Node<K, V> tail = new Node<>();

    Optional<V> get(K key) { }
    void put(K key, V value) { }
}
```

## 3.3 Sequence Diagram

Get hit:

```text
Client -> LRUCache: get(k)
LRUCache -> Map: lookup k
Map --> LRUCache: node
LRUCache -> List: remove node
LRUCache -> List: add node after head
LRUCache --> Client: value
```

Put new with eviction:

```text
Client -> LRUCache: put(k, v)
LRUCache -> Map: key missing
LRUCache -> List: add new node after head
LRUCache -> Map: store k -> node
LRUCache -> LRUCache: size > capacity?
LRUCache -> List: remove tail.prev
LRUCache -> Map: delete evicted key
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | swap LRU for LFU/FIFO/TTL policy |
| Decorator | add metrics or tracing around cache |
| Adapter | wrap cache behind generic interface |
| Template Method | common cache flow with custom loader |
| Singleton | sometimes used for local cache, but risky in tests and multi-tenant systems |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| capacity is zero | reject all puts or keep empty cache |
| put existing key | update value and move to head |
| get missing key | return miss without changing list |
| evict last real node | remove it from both list and map |
| null key/value | define policy clearly |
| duplicate node in list | prevent through helper methods and map invariant |
| TTL expired key | treat as miss and remove lazily |
| concurrent get/put | use lock or segmented design |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
lru/
  LRUCache.java
  Node.java
  CacheStats.java
  CacheTest.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
class Node:
    def __init__(self, key: int = 0, value: int = 0) -> None:
        self.key = key
        self.value = value
        self.prev: Node | None = None
        self.next: Node | None = None


class LRUCache:
    def __init__(self, capacity: int) -> None:
        self.capacity = capacity
        self.nodes: dict[int, Node] = {}
        self.head = Node()
        self.tail = Node()
        self.head.next = self.tail
        self.tail.prev = self.head

    def get(self, key: int) -> int:
        if key not in self.nodes:
            return -1
        node = self.nodes[key]
        self._move_to_front(node)
        return node.value

    def put(self, key: int, value: int) -> None:
        if self.capacity <= 0:
            return

        if key in self.nodes:
            node = self.nodes[key]
            node.value = value
            self._move_to_front(node)
            return

        node = Node(key, value)
        self.nodes[key] = node
        self._add_after_head(node)

        if len(self.nodes) > self.capacity:
            victim = self._remove_tail()
            del self.nodes[victim.key]

    def _move_to_front(self, node: Node) -> None:
        self._remove(node)
        self._add_after_head(node)

    def _add_after_head(self, node: Node) -> None:
        first = self.head.next
        node.prev = self.head
        node.next = first
        self.head.next = node
        first.prev = node

    def _remove(self, node: Node) -> None:
        previous = node.prev
        following = node.next
        previous.next = following
        following.prev = previous

    def _remove_tail(self) -> Node:
        victim = self.tail.prev
        self._remove(victim)
        return victim


cache = LRUCache(2)
cache.put(1, 10)
cache.put(2, 20)
print(cache.get(1))
cache.put(3, 30)
print(cache.get(2))
print(cache.get(3))
```

## 4.3 Complexity

| Operation | Time | Space |
|---|---|---|
| get | O(1) average | O(1) extra |
| put existing | O(1) average | O(1) extra |
| put new no eviction | O(1) average | O(1) extra |
| put new with eviction | O(1) average | O(1) extra |
| total cache | O(capacity) | O(capacity) |

## 4.4 Concurrency

Simple thread-safe design:

- Put a single lock around `get`, `put`, and helpers.
- Correct and easy, but throughput is limited.

Higher-throughput designs:

- Segment the cache into multiple LRU shards by key hash.
- Use read/write locks carefully, but note that `get` mutates recency.
- Use approximate recency queues to reduce contention.
- Use proven libraries like Caffeine for production JVM caches.

Important detail:

> In LRU, `get` is a write to cache metadata because it changes recency order.

## 4.5 Testing Checklist

- `get` missing key returns miss.
- `put` then `get` returns value.
- `get` updates recency.
- `put` existing key updates value and recency.
- Capacity overflow evicts least recently used.
- Capacity zero stores nothing.
- Multiple evictions preserve invariant.
- Optional TTL removes expired entries.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Cache Stampede

Problem:

- A hot key expires or is missing, and many requests rebuild it at once.

Handling:

- Single-flight loading per key.
- Request coalescing.
- Soft TTL plus background refresh.
- Serve stale value briefly if safe.
- Add jitter to TTL.

## 5.2 Hot Key

Problem:

- One key receives huge traffic.

Handling:

- Keep hot key in memory.
- Avoid frequent eviction by capacity tuning.
- Use refresh-ahead for expensive values.
- Track top keys and hit ratio.

## 5.3 Memory Pressure

Handling:

- Bound entries or bytes.
- Avoid caching large values.
- Use size-aware eviction if values vary widely.
- Monitor heap/memory pressure.
- Prefer off-heap/proven libraries for very large local caches.

## 5.4 Scan Pollution

Problem:

- A one-time scan fills cache with cold entries and evicts useful hot keys.

Handling:

- Use admission policy.
- Use segmented LRU.
- Use LFU/TinyLFU for frequency-sensitive workloads.
- Avoid caching known scan queries.

---

# 6. Scaling To A Billion Users

## 6.1 Local Cache vs Distributed Cache

| Dimension | Local LRU | Distributed cache |
|---|---|---|
| latency | fastest | network hop |
| consistency | per-process view | shared view |
| capacity | limited by instance memory | cluster memory |
| failure scope | one process | cache cluster/shards |
| invalidation | hard across fleet | centralized but still tricky |
| best for | small hot metadata | shared hot data |

## 6.2 Multi-Level Caching

Common hierarchy:

```text
L1: in-process LRU cache
L2: distributed cache such as Redis/Memcached
L3: database or source service
```

Read flow:

1. Check local LRU.
2. On miss, check distributed cache.
3. On miss, load from source of truth.
4. Populate distributed cache.
5. Populate local LRU.

## 6.3 Invalidation

Invalidation options:

| Option | How it works | Trade-off |
|---|---|---|
| TTL only | entries expire naturally | stale data window |
| explicit invalidate | delete on write | requires reliable notification |
| versioned keys | key includes version | extra storage, clean old versions later |
| pub/sub invalidation | broadcast change | missed messages can cause stale cache |

## 6.4 Observability

Track:

- hit ratio,
- miss ratio,
- eviction count,
- average load time,
- cache size,
- memory usage,
- top keys,
- stale serve count,
- stampede prevention count,
- lock contention.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify API and capacity.
2. State O(1) requirement.
3. Explain why hashmap alone is not enough.
4. Explain hashmap plus doubly linked list.
5. Define head as most-recent and tail as least-recent.
6. Walk through `get`, `put existing`, `put new`, and eviction.
7. Mention concurrency: `get` mutates recency.
8. Discuss production issues: TTL, invalidation, metrics, stampede, memory.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| eviction | LRU | LFU | LRU for recent reuse, LFU for frequency |
| cache scope | local | distributed | local for latency, distributed for shared state |
| capacity | entry count | byte size | byte size in production if values vary |
| concurrency | single lock | segmented cache | single lock for coding, segmented/proven library in production |
| expiration | no TTL | TTL | TTL for stale-data control |

## 7.3 Common Mistakes

- Forgetting that `get` must update recency.
- Removing from list but not hashmap.
- Evicting the newest item instead of tail.
- Using a singly linked list and losing O(1) arbitrary removal.
- Ignoring capacity zero.
- Ignoring thread safety while claiming production readiness.
- Not discussing cache stampede.

## 7.4 Strong Closing

> LRU cache is a clean O(1) data-structure problem: hashmap for lookup and doubly linked list for recency order. In production, I would add TTL, metrics, bounded memory, stampede protection, and concurrency control, and I would switch to LFU/TinyLFU or segmented policies if the workload is scan-heavy or frequency-driven.

---

# 8. Fast Recall Rules

- LRU means least recently used.
- `get` updates recency.
- Hashmap gives O(1) lookup.
- Doubly linked list gives O(1) move and eviction.
- Head is most recent, tail is least recent.
- Evict `tail.prev` when over capacity.
- Store key in node so eviction can remove from hashmap.
- A production cache needs TTL, metrics, memory limits, and stampede protection.
- `get` is a metadata write under concurrency.
- LRU is weak against scan pollution.