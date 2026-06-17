# LFU Cache - End-to-End System Design

> Goal: design and implement a Least Frequently Used cache that evicts low-frequency entries in O(1), handles recency tie-breaks, and explains when frequency-based caching beats simple LRU.

---

## How To Use This File

- Use this when the interview problem says LFU cache, frequency-based eviction, O(1) cache, or cache eviction with usage counts.
- Start with the key insight: LFU needs fast key lookup plus fast access to the minimum frequency bucket.
- For machine coding, implement hashmap + frequency buckets + min-frequency tracker.
- For system design, explain frequency aging, memory pressure, hot keys, and scan resistance.

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

| Layer | Interview signal | LFU cache focus |
|---|---|---|
| Problem understanding | Can define eviction rule | least frequency first, recency tie-break inside same frequency |
| HLD | Can place LFU correctly | frequency-driven workloads, admission policy, local cache |
| LLD | Can maintain O(1) invariants | key map, freq map, ordered sets, min frequency |
| Machine coding | Can implement updates | get increments frequency, put evicts min-frequency victim |
| Traffic spikes | Can reason about pollution | one-hit scans, hot keys, stale frequency counts |
| Billion users | Can discuss production limits | aging, approximate LFU, TinyLFU, distributed caches |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Support `get(key)` in O(1).
- Support `put(key, value)` in O(1).
- Store at most `capacity` entries.
- Track access frequency for every key.
- On capacity overflow, evict the least frequently used key.
- If multiple keys have the same lowest frequency, evict the least recently used among them.
- A successful `get` increments frequency.
- Updating an existing key updates value and increments frequency.

Optional requirements:

- Support TTL expiration.
- Support frequency aging/decay.
- Support byte-size capacity instead of entry count.
- Support thread safety.
- Emit hit/miss/eviction/frequency metrics.
- Support weighted values where some entries cost more memory.

Out of scope unless asked:

- Full distributed cache cluster.
- Persistent database.
- ML ranking of cache admission.
- Complete TinyLFU/Caffeine internals.

## 1.2 Non-Functional Requirements

Performance:

- O(1) average-time `get`.
- O(1) average-time `put`.
- O(1) frequency update.
- O(1) access to eviction victim.

Correctness:

- Every key has exactly one value and one frequency.
- Every key appears in exactly one frequency bucket.
- `minFrequency` always points to the lowest non-empty frequency bucket.
- Within the same frequency, eviction uses least-recent order.

Operations:

- Track hit ratio and eviction reasons.
- Track frequency distribution.
- Prevent unbounded frequency counters.
- Avoid stale old hot keys living forever.

## 1.3 Constraints

- Frequency counters can grow without bound unless aged or capped.
- LFU can keep old historically hot keys even after they stop being useful.
- O(1) LFU is more complex than O(1) LRU.
- Frequency buckets need recency ordering for tie-breaks.
- Thread-safe LFU is harder because `get` changes frequency and bucket membership.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Local capacity | 10K to 10M entries |
| Operation target | O(1) average |
| Frequency update | every successful read/write |
| Tie-break rule | LRU within same frequency |
| Hit latency | microseconds to low milliseconds locally |
| Production policy | often approximate LFU/TinyLFU instead of exact LFU |

Back-of-the-envelope:

- Each entry needs key, value, frequency, and pointers/order metadata.
- Frequency buckets add overhead but avoid scanning all keys.
- Exact LFU can cost more memory than LRU.
- For massive caches, approximate frequency sketches are often cheaper.

## 1.5 Clarifying Questions To Ask

- Should `get` increment frequency?
- Should updating an existing key increment frequency?
- How should ties be broken?
- What should happen at capacity zero?
- Is capacity by entries or bytes?
- Do frequencies decay over time?
- Is thread safety required?
- Should expired entries be removed lazily or eagerly?

Strong interview framing:

> I will implement LFU with a key map for O(1) lookup, a frequency map from frequency to ordered keys for O(1) victim selection, and a `minFrequency` variable that always points to the lowest active bucket. On every hit, I move the key from frequency f to f+1 and update minFrequency if needed.

---

# 2. High-Level Design

## 2.1 Architecture

Internal structure:

```text
keyToEntry:
  key -> {value, frequency}

freqToKeys:
  1 -> ordered keys: least recent ... most recent
  2 -> ordered keys: least recent ... most recent
  3 -> ordered keys: least recent ... most recent

minFrequency -> 1
```

Read flow:

```text
get(key)
  -> lookup entry in keyToEntry
  -> miss if absent
  -> remove key from old frequency bucket
  -> increment frequency
  -> add key to new frequency bucket as most recent
  -> update minFrequency if old bucket became empty
  -> return value
```

Write flow:

```text
put(key, value)
  -> if key exists: update value and increment frequency
  -> else if capacity full: evict oldest key from minFrequency bucket
  -> insert new key with frequency 1
  -> set minFrequency = 1
```

## 2.2 APIs

Machine-coding API:

```text
LFUCache(capacity)
get(key) -> value or -1/null/Optional.empty
put(key, value) -> void
```

Production-style interface:

```java
interface Cache<K, V> {
    Optional<V> get(K key);
    void put(K key, V value);
    void invalidate(K key);
    CacheStats stats();
}
```

Stats:

```json
{
  "hits": 1200000,
  "misses": 300000,
  "evictions": 8000,
  "minFrequency": 2,
  "entryCount": 100000
}
```

## 2.3 Core Components

Think of LFU Cache as LRU inside frequency buckets.

| Component | Owns | Why it exists |
|---|---|---|
| `keyToEntry` map | key to value/frequency | O(1) lookup |
| `freqToKeys` map | frequency to ordered keys | O(1) lowest-frequency bucket access |
| Ordered set/list per frequency | recency tie-break | evict LRU among same frequency |
| `minFrequency` | current lowest frequency | O(1) eviction bucket selection |
| Entry object | key, value, frequency | source of per-key state |
| Capacity policy | max entries/bytes | bounded memory |

### Invariant

```text
Every key in keyToEntry appears in exactly one freqToKeys bucket.
entry.frequency equals the bucket number containing the key.
minFrequency is the smallest frequency with a non-empty bucket.
Within a bucket, oldest key is the tie-break eviction victim.
```

### LFU vs LRU

| Feature | LRU | LFU |
|---|---|---|
| Eviction signal | recency | frequency |
| Main data structure | hashmap + doubly linked list | key map + frequency buckets |
| Good for | recent reuse | stable popularity |
| Weak against | scans | stale old popularity |
| Complexity | simpler | more complex |

### Why Recency Tie-Break Still Matters

If keys `A`, `B`, and `C` all have frequency 2, LFU alone cannot choose. The usual rule is:

```text
Evict the least recently used key among the least frequently used keys.
```

This makes behavior deterministic and practical.

### Frequency Aging

Problem:

- A key that was hot yesterday can keep a huge frequency and never be evicted.

Solutions:

- Periodically halve frequencies.
- Use time-windowed counts.
- Cap max frequency.
- Use approximate TinyLFU with a resettable frequency sketch.
- Combine admission policy with LRU eviction.

One-stop interview answer:

> LFU needs key lookup, frequency tracking, and fast victim selection. I would store key to entry in one map, frequency to ordered keys in another map, and maintain `minFrequency`. On every hit, the key moves from frequency f to f+1. On eviction, remove the oldest key from the `minFrequency` bucket.

---

# 3. Low-Level Design

LLD goal:

> Keep frequency movement and min-frequency updates correct after every get and put.

Starter map:

| LLD question | LFU answer |
|---|---|
| Main lookup | `Map<K, Entry>` |
| Frequency buckets | `Map<Integer, LinkedHashSet<K>>` |
| Tie-break | insertion order inside frequency bucket |
| Minimum frequency | `minFrequency` integer |
| Get hit | increment frequency |
| Put existing | update value and increment frequency |
| Put new | insert with frequency 1 |
| Eviction | remove oldest key from `freqToKeys[minFrequency]` |

Beginner-friendly design order:

1. Define `Entry` with value and frequency.
2. Create `keyToEntry` map.
3. Create `freqToKeys` map from frequency to ordered keys.
4. Maintain `minFrequency`.
5. Write `increaseFrequency(key)` helper.
6. Implement `get` using lookup plus `increaseFrequency`.
7. Implement `put existing` using value update plus `increaseFrequency`.
8. Implement `put new` with eviction from `minFrequency` bucket.

Interview sentence:

> LFU looks tricky until you isolate `increaseFrequency`. That helper removes the key from the old bucket, increments its entry frequency, inserts it into the new bucket, and advances `minFrequency` only if the old minimum bucket became empty.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `Entry` | value and frequency | frequency matches bucket |
| `LFUCache` | owns maps and minFrequency | capacity respected |
| `FrequencyBucket` | ordered keys for same frequency | oldest key is eviction victim |
| `EvictionPolicy` | chooses victim | lowest frequency, then oldest |
| `CacheStats` | hit/miss/eviction counters | monotonic counters |

## 3.2 Class Sketch

```java
final class Entry<V> {
    V value;
    int frequency;
}

final class LFUCache<K, V> {
    private final int capacity;
    private int minFrequency = 0;
    private final Map<K, Entry<V>> keyToEntry = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> freqToKeys = new HashMap<>();

    Optional<V> get(K key) { }
    void put(K key, V value) { }
    private void increaseFrequency(K key) { }
}
```

## 3.3 Sequence Diagram

Get hit:

```text
Client -> LFUCache: get(k)
LFUCache -> keyToEntry: lookup k
keyToEntry --> LFUCache: entry(freq=2)
LFUCache -> freqToKeys[2]: remove k
LFUCache -> Entry: frequency = 3
LFUCache -> freqToKeys[3]: add k as most recent
LFUCache -> LFUCache: update minFrequency if needed
LFUCache --> Client: value
```

Put new with eviction:

```text
Client -> LFUCache: put(k, v)
LFUCache -> LFUCache: capacity full?
LFUCache -> freqToKeys[minFrequency]: remove oldest victim
LFUCache -> keyToEntry: delete victim
LFUCache -> keyToEntry: add k with freq=1
LFUCache -> freqToKeys[1]: add k
LFUCache -> LFUCache: minFrequency = 1
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | swap LFU/LRU/FIFO/TinyLFU policies |
| Adapter | expose generic cache interface |
| Decorator | add metrics, tracing, TTL |
| Factory | choose cache policy by config |
| Snapshot | expose stats without mutating cache |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| capacity zero | store nothing |
| get missing key | return miss, do not change minFrequency |
| put existing key | update value and increment frequency |
| min bucket becomes empty | increment/update minFrequency |
| multiple keys same frequency | evict oldest in bucket |
| frequency grows huge | cap or age counters |
| expired key | remove from key map and bucket |
| concurrent get | lock because get mutates frequency |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
lfu/
  LFUCache.java
  Entry.java
  CacheStats.java
  LFUCacheTest.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import OrderedDict, defaultdict


class LFUCache:
    def __init__(self, capacity: int) -> None:
        self.capacity = capacity
        self.min_frequency = 0
        self.values: dict[int, int] = {}
        self.frequencies: dict[int, int] = {}
        self.buckets: dict[int, OrderedDict[int, None]] = defaultdict(OrderedDict)

    def get(self, key: int) -> int:
        if key not in self.values:
            return -1
        self._increase_frequency(key)
        return self.values[key]

    def put(self, key: int, value: int) -> None:
        if self.capacity <= 0:
            return

        if key in self.values:
            self.values[key] = value
            self._increase_frequency(key)
            return

        if len(self.values) >= self.capacity:
            victim, _ = self.buckets[self.min_frequency].popitem(last=False)
            del self.values[victim]
            del self.frequencies[victim]

        self.values[key] = value
        self.frequencies[key] = 1
        self.buckets[1][key] = None
        self.min_frequency = 1

    def _increase_frequency(self, key: int) -> None:
        old_frequency = self.frequencies[key]
        del self.buckets[old_frequency][key]

        if not self.buckets[old_frequency] and self.min_frequency == old_frequency:
            self.min_frequency += 1

        new_frequency = old_frequency + 1
        self.frequencies[key] = new_frequency
        self.buckets[new_frequency][key] = None


cache = LFUCache(2)
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

- `get` mutates frequency and bucket membership, so it needs synchronization.
- A single lock is simplest for correctness.
- Segmented LFU can reduce lock contention by sharding keys.
- Approximate frequency sketches can reduce metadata writes.
- Production systems often prefer proven libraries rather than hand-written exact LFU.

## 4.5 Testing Checklist

- `get` missing returns miss.
- `put` then `get` returns value.
- `get` increments frequency.
- `put` existing updates value and increments frequency.
- Evicts lowest-frequency key.
- For same frequency, evicts least recently used key.
- Capacity zero stores nothing.
- `minFrequency` updates when old bucket empties.
- Repeated gets do not break bucket membership.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 One-Time Scan Traffic

Problem:

- Many cold keys are read once.

Why LFU helps:

- One-hit keys stay at frequency 1.
- Frequently used keys have higher frequency and survive.

Remaining risk:

- If scan is huge and cache fills with frequency-1 keys, recent frequency-1 keys may still churn.

## 5.2 Old Hot Key Problem

Problem:

- A key was hot earlier, built a high frequency, and now is no longer useful.

Handling:

- Frequency aging.
- Time-windowed frequency.
- Counter cap.
- TinyLFU-style resettable sketch.
- TTL for entries.

## 5.3 Hot Key Spike

Handling:

- Hot keys naturally gain high frequency.
- Avoid single lock bottleneck under high read concurrency.
- Use segmented design or read-optimized production cache.
- Track top keys and frequency distribution.

## 5.4 Memory Pressure

Handling:

- Bound by bytes if values vary.
- Avoid caching huge values.
- Track metadata overhead.
- Use approximate LFU if exact frequency buckets are too costly.

---

# 6. Scaling To A Billion Users

## 6.1 Exact LFU vs Approximate LFU

| Dimension | Exact LFU | Approximate LFU/TinyLFU |
|---|---|---|
| accuracy | high | approximate |
| memory overhead | higher | lower |
| implementation | more complex than LRU | more complex conceptually |
| production fit | small/medium local cache | large high-throughput cache |
| scan resistance | good | very good with admission policy |

## 6.2 Multi-Level Caching

LFU can be used in:

```text
L1 local cache: fast, process-local, exact or approximate LFU
L2 distributed cache: Redis/Memcached with approximate eviction policy
L3 source of truth: database/service
```

Important:

- Different layers can use different eviction policies.
- Local cache can use LFU while distributed cache uses LRU/volatile-LFU depending platform.
- Policies should match workload and memory cost.

## 6.3 Invalidation And Freshness

LFU only decides what to evict. It does not solve stale data.

Freshness tools:

- TTL,
- explicit invalidate,
- versioned keys,
- write-through/update-on-write,
- pub/sub invalidation,
- tombstone markers for deleted data.

## 6.4 Observability

Track:

- hit ratio,
- miss ratio,
- eviction count,
- eviction frequency distribution,
- average frequency,
- max frequency,
- frequency aging events,
- lock contention,
- memory usage,
- stale read count,
- cache load time.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify API, capacity, and tie-break rule.
2. State LFU eviction: lowest frequency first.
3. Explain why recency tie-break is needed.
4. Introduce key map, frequency buckets, and minFrequency.
5. Walk through `get` frequency increment.
6. Walk through `put` existing and new.
7. Walk through eviction from min-frequency bucket.
8. Discuss production issues: aging, concurrency, TTL, metrics, approximate LFU.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| eviction | LRU | LFU | LFU when frequency predicts reuse |
| implementation | exact LFU | approximate LFU | exact for coding, approximate for huge scale |
| counters | unbounded | aged/capped | aged/capped in production |
| tie-break | arbitrary | LRU within frequency | LRU tie-break |
| concurrency | global lock | segmented | global for coding, segmented/proven library for production |

## 7.3 Common Mistakes

- Forgetting to update frequency on `get`.
- Forgetting LRU tie-break inside same frequency.
- Not updating `minFrequency` when a bucket becomes empty.
- Leaving a key in old frequency bucket after increment.
- Letting frequency counters grow forever.
- Claiming LFU solves stale-data problems.
- Ignoring that `get` is a metadata write.

## 7.4 Strong Closing

> LFU cache is best when repeated frequency predicts future reuse. I would implement exact O(1) LFU using a key map, frequency buckets with recency ordering, and a `minFrequency` tracker. In production, I would add TTL, frequency aging, metrics, and likely use approximate LFU/TinyLFU for high-throughput workloads.

---

# 8. Fast Recall Rules

- LFU means least frequently used.
- Evict lowest frequency first.
- Tie-break with least recently used inside that frequency.
- Use `key -> entry` map for O(1) lookup.
- Use `frequency -> ordered keys` map for O(1) victim selection.
- Maintain `minFrequency`.
- `get` increments frequency.
- `put existing` updates value and increments frequency.
- New keys start at frequency 1.
- Production LFU needs aging or counters become stale.