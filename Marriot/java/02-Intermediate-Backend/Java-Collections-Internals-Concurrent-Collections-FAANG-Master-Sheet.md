# Java Collections Internals And Concurrent Collections FAANG Master Sheet

Target: Java collection internals for backend and FAANG-style interviews.

This sheet covers:
- Collection hierarchy
- ArrayList, LinkedList
- HashMap internals
- LinkedHashMap, TreeMap, EnumMap, IdentityHashMap
- HashSet, TreeSet
- PriorityQueue
- ConcurrentHashMap
- CopyOnWriteArrayList
- Blocking queues
- Fail-fast vs weakly consistent iterators
- Collection selection under real constraints

---

## 1. Mental Model

Collections are not just containers.

Each collection makes a trade-off between:

- Lookup speed
- Insert/delete cost
- Ordering
- Sorting
- Memory
- Thread safety
- Iterator behavior
- Null support

Strong interview line:

```text
I choose a collection based on access pattern, ordering needs, duplicate policy, concurrency,
and mutation frequency.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| ArrayList vs LinkedList | Very high | Classic comparison |
| HashMap internals | Very high | Most asked |
| equals/hashCode contract | Very high | Correctness |
| HashSet internals | High | Map-backed set |
| TreeMap / TreeSet | High | Sorted structures |
| LinkedHashMap | High | LRU cache pattern |
| ConcurrentHashMap | Very high | Backend concurrency |
| CopyOnWriteArrayList | Medium-high | Read-heavy concurrency |
| PriorityQueue | High | Top-K, scheduling |
| Fail-fast iterator | High | Common trap |
| Weakly consistent iterator | High | Concurrent collections |
| Big-O selection | Very high | Practical judgment |

---

## 3. Collection Hierarchy

Simplified:

```text
Iterable
    -> Collection
        -> List
            -> ArrayList
            -> LinkedList
            -> CopyOnWriteArrayList
        -> Set
            -> HashSet
            -> LinkedHashSet
            -> TreeSet
            -> EnumSet
        -> Queue
            -> PriorityQueue
            -> ArrayDeque
            -> BlockingQueue

Map
    -> HashMap
    -> LinkedHashMap
    -> TreeMap
    -> ConcurrentHashMap
    -> EnumMap
    -> IdentityHashMap
```

Map is not a subtype of Collection.

---

## 4. ArrayList

ArrayList is a dynamic array.

Strengths:

- Fast random access.
- Compact memory.
- Good iteration locality.

Weaknesses:

- Insert/delete in middle shifts elements.
- Resizing copies array.
- Not thread-safe.

Big-O:

| Operation | Cost |
|---|---|
| get by index | O(1) |
| append | Amortized O(1) |
| insert middle | O(n) |
| remove middle | O(n) |
| contains | O(n) |

Example:

```java
List<String> names = new ArrayList<>();
names.add("Aravind");
names.add("Rahul");
System.out.println(names.get(0));
```

Interview line:

```text
ArrayList is usually the default List choice because random access and iteration are fast.
```

---

## 5. LinkedList

LinkedList is a doubly linked list.

Strengths:

- Fast insertion/removal if node is already known.
- Can be used as deque.

Weaknesses:

- Slow random access.
- More memory per element.
- Poor CPU cache locality.

Big-O:

| Operation | Cost |
|---|---|
| get by index | O(n) |
| add first/last | O(1) |
| remove first/last | O(1) |
| contains | O(n) |

Trap:

```text
LinkedList is not automatically faster for insertion. If you must first search by index,
the traversal is O(n).
```

Better deque:

```java
Deque<String> deque = new ArrayDeque<>();
deque.addFirst("a");
deque.addLast("b");
```

---

## 6. HashMap Internals

HashMap stores key-value pairs in buckets.

Simplified flow for `put(key, value)`:

1. Compute key hash.
2. Spread hash bits.
3. Find bucket index.
4. If bucket empty, insert node.
5. If same key exists, replace value.
6. If collision, add to list or tree.
7. Resize if threshold exceeded.

Index formula:

```text
index = (capacity - 1) & hash
```

Why capacity is power of two:

```text
It allows fast bitwise index calculation instead of modulo.
```

---

## 7. HashMap Defaults

| Property | Default |
|---|---|
| Initial capacity | 16 |
| Load factor | 0.75 |
| Resize threshold | capacity * load factor |
| Null key | One allowed |
| Null values | Many allowed |
| Thread-safe | No |

Resize example:

```text
capacity 16, load factor 0.75 -> resize after size exceeds 12
```

Interview line:

```text
Load factor balances memory usage and collision probability. A lower load factor uses more
memory but can reduce collisions.
```

---

## 8. Collision Handling And Treeification

Before Java 8, collision chains were linked lists.

Modern HashMap can convert a long collision chain into a tree.

Concept:

```text
Bucket starts as linked list. If collisions exceed a threshold and table is large enough,
the bucket can treeify into a red-black tree.
```

Why:

```text
To avoid worst-case O(n) lookup for heavily collided buckets.
```

Interview-safe answer:

```text
HashMap average lookup is O(1), but worst-case collisions can degrade. Modern Java improves
worst-case bucket behavior by treeifying long collision chains into red-black trees under
specific conditions.
```

---

## 9. equals And hashCode Contract

Rules:

1. If `a.equals(b)` is true, `a.hashCode()` must equal `b.hashCode()`.
2. If hash codes are equal, objects may or may not be equal.
3. Fields used in equals/hashCode should not mutate while object is in a hash collection.

Bad key:

```java
class UserKey {
    String id;

    UserKey(String id) {
        this.id = id;
    }
}
```

Better:

```java
import java.util.Objects;

final class UserKey {
    private final String id;

    UserKey(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UserKey)) {
            return false;
        }
        UserKey other = (UserKey) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

Trap:

```text
If you mutate a key after putting it in HashMap, the entry may become unreachable by lookup.
```

---

## 10. LinkedHashMap

LinkedHashMap preserves insertion order or access order.

Use cases:

- Ordered map
- Simple LRU cache
- Predictable iteration

LRU example:

```java
import java.util.LinkedHashMap;
import java.util.Map;

class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

Interview line:

```text
LinkedHashMap adds a linked list over HashMap entries, giving predictable iteration order
with extra memory overhead.
```

---

## 11. TreeMap

TreeMap is a sorted map backed by a red-black tree.

Big-O:

| Operation | Cost |
|---|---|
| get | O(log n) |
| put | O(log n) |
| remove | O(log n) |
| range queries | Efficient |

Use when:

- Sorted keys are needed.
- Range queries are needed.
- `floorKey`, `ceilingKey`, `subMap` are useful.

Example:

```java
NavigableMap<Integer, String> rooms = new TreeMap<>();
rooms.put(101, "standard");
rooms.put(205, "deluxe");

System.out.println(rooms.ceilingKey(150)); // 205
```

Trap:

```text
TreeMap ordering is based on compareTo/Comparator, not hashCode.
```

---

## 12. EnumMap And EnumSet

Use when keys/elements are enum values.

Benefits:

- Very efficient.
- Compact.
- Type-safe.
- Faster than HashMap/HashSet for enum keys.

Example:

```java
import java.util.EnumMap;

enum Status {
    NEW, PAID, CANCELLED
}

Map<Status, Integer> counts = new EnumMap<>(Status.class);
counts.put(Status.NEW, 10);
```

Interview line:

```text
If the key is an enum, EnumMap is usually better than HashMap.
```

---

## 13. IdentityHashMap

IdentityHashMap compares keys using `==`, not `equals()`.

Use cases:

- Object graph traversal.
- Serialization internals.
- Identity-based caches.

Do not use for normal business maps.

Example:

```java
Map<String, String> map = new IdentityHashMap<>();
map.put(new String("a"), "one");
map.put(new String("a"), "two");

System.out.println(map.size()); // 2
```

Trap:

```text
IdentityHashMap violates normal Map expectations around logical equality. Use rarely.
```

---

## 14. HashSet

HashSet is backed by HashMap.

Concept:

```text
Set element -> HashMap key
dummy value -> internal constant object
```

Use when:

- Unique elements.
- No ordering required.
- Fast average lookup.

Example:

```java
Set<String> ids = new HashSet<>();
ids.add("u1");
ids.add("u1");

System.out.println(ids.size()); // 1
```

---

## 15. TreeSet

TreeSet is backed by TreeMap.

Use when:

- Unique sorted elements.
- Range operations.

Example:

```java
NavigableSet<Integer> scores = new TreeSet<>();
scores.add(90);
scores.add(75);
scores.add(80);

System.out.println(scores.first()); // 75
```

Trap:

```text
Comparator consistency matters. If comparator treats two different objects as equal,
TreeSet keeps only one.
```

---

## 16. PriorityQueue

PriorityQueue gives access to smallest/highest-priority element.

Default is min-heap.

Use cases:

- Top K
- Scheduling
- Dijkstra-like algorithms
- Merge K sorted lists

Example:

```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(1);
minHeap.offer(3);

System.out.println(minHeap.poll()); // 1
```

Max heap:

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
```

Trap:

```text
PriorityQueue does not keep the whole collection sorted for iteration. It only guarantees
the head is the next priority element.
```

---

## 17. ConcurrentHashMap

ConcurrentHashMap is a thread-safe hash table for concurrent access.

Key properties:

- Allows concurrent reads.
- Updates coordinate at bucket/bin level.
- Does not allow null keys or null values.
- Iterators are weakly consistent.
- Compound operations should use atomic methods.

Common methods:

```java
map.putIfAbsent(key, value);
map.computeIfAbsent(key, k -> load(k));
map.compute(key, (k, oldValue) -> newValue);
map.merge(key, 1, Integer::sum);
```

Frequency count:

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

class FrequencyCounter {
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();

    void record(String word) {
        counts.computeIfAbsent(word, key -> new LongAdder()).increment();
    }

    long count(String word) {
        LongAdder adder = counts.get(word);
        return adder == null ? 0 : adder.sum();
    }
}
```

Strong answer:

```text
ConcurrentHashMap is thread-safe for individual operations, but compound business logic
still needs atomic map methods or external coordination.
```

---

## 18. Hashtable vs SynchronizedMap vs ConcurrentHashMap

| Type | Behavior |
|---|---|
| Hashtable | Legacy, synchronized methods |
| Collections.synchronizedMap | Wraps map with single lock |
| ConcurrentHashMap | Better concurrent access |

Interview line:

```text
ConcurrentHashMap is preferred for high-concurrency maps because it avoids locking the
entire map for most operations.
```

---

## 19. CopyOnWriteArrayList

CopyOnWriteArrayList copies the array on each write.

Good for:

- Many reads
- Very few writes
- Listener lists
- Snapshot-style iteration

Bad for:

- Frequent writes
- Large lists with many mutations

Example:

```java
import java.util.concurrent.CopyOnWriteArrayList;

class ListenerRegistry {
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    void add(Runnable listener) {
        listeners.add(listener);
    }

    void notifyAllListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
```

Interview line:

```text
CopyOnWriteArrayList is excellent for read-heavy listener-style workloads, but expensive
for frequent writes.
```

---

## 20. Iterator Behavior

Fail-fast iterator:

```text
Detects structural modification and may throw ConcurrentModificationException.
Best-effort, not a correctness guarantee.
```

Example:

```java
List<String> names = new ArrayList<>(List.of("a", "b"));

for (String name : names) {
    names.add("c"); // ConcurrentModificationException likely
}
```

Correct remove:

```java
Iterator<String> it = names.iterator();
while (it.hasNext()) {
    if (it.next().equals("a")) {
        it.remove();
    }
}
```

Weakly consistent iterator:

```text
Concurrent collections like ConcurrentHashMap can tolerate concurrent modification and may
reflect some, all, or none of the changes during iteration.
```

---

## 21. Null Support

| Collection | Null Support |
|---|---|
| HashMap | One null key, many null values |
| HashSet | One null element |
| TreeMap | Depends on comparator; natural ordering usually rejects null keys |
| ConcurrentHashMap | No null keys or values |
| ArrayList | Allows null |
| PriorityQueue | Does not allow null |

Why ConcurrentHashMap rejects null:

```text
Null would make it ambiguous whether get(key) means no mapping or mapped-to-null under
concurrent access.
```

---

## 22. Collection Selection Guide

| Need | Choose |
|---|---|
| Fast random access | ArrayList |
| Stack/queue/deque | ArrayDeque |
| Unique unsorted values | HashSet |
| Unique sorted values | TreeSet |
| Key-value fast lookup | HashMap |
| Ordered map | LinkedHashMap |
| LRU cache prototype | LinkedHashMap |
| Sorted/range map | TreeMap |
| Enum keys | EnumMap |
| Thread-safe map | ConcurrentHashMap |
| Read-heavy thread-safe list | CopyOnWriteArrayList |
| Producer-consumer | BlockingQueue |
| Top K | PriorityQueue |

---

## 23. Mini Program: Top K Frequent Words

```java
import java.util.*;

public class TopKWords {
    public static void main(String[] args) {
        List<String> words = List.of("java", "spring", "java", "kafka", "java", "spring");
        System.out.println(topK(words, 2));
    }

    static List<String> topK(List<String> words, int k) {
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            frequency.merge(word, 1, Integer::sum);
        }

        PriorityQueue<Map.Entry<String, Integer>> heap =
            new PriorityQueue<>(Map.Entry.comparingByValue());

        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            heap.offer(entry);
            if (heap.size() > k) {
                heap.poll();
            }
        }

        List<String> result = new ArrayList<>();
        while (!heap.isEmpty()) {
            result.add(heap.poll().getKey());
        }
        Collections.reverse(result);
        return result;
    }
}
```

Why it matters:

```text
This combines HashMap for counting and PriorityQueue for top-K selection.
```

---

## 24. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Using LinkedList for random access | O(n) get | Use ArrayList |
| Mutable HashMap key | Lookup breaks | Use immutable keys |
| Forgetting to override hashCode | Hash collections break | Override equals and hashCode together |
| Using HashMap concurrently | Race/corruption risk | Use ConcurrentHashMap or locking |
| Assuming CHM makes compound logic atomic | Multi-step logic can race | Use compute/merge/putIfAbsent |
| Iterating PriorityQueue expecting sorted order | Only head guaranteed | Poll repeatedly or sort |
| Using CopyOnWriteArrayList for heavy writes | Copies array every write | Use synchronized/list/queue/CHM design |
| Ignoring comparator consistency | TreeSet/TreeMap surprises | Comparator must reflect uniqueness rules |

---

## 25. FAANG-Level Question

Question:

> You need an in-memory cache with LRU eviction in Java. What would you use?

Strong answer:

```text
For a simple single-process prototype, LinkedHashMap with accessOrder=true and
removeEldestEntry can implement LRU. For production, I would use a proven cache like
Caffeine because it handles concurrency, eviction quality, metrics, expiry, and memory
behavior better. If the cache must be shared across instances, I would use Redis or a
distributed cache instead of a local map.
```

---

## 26. Rapid Revision

Must-say lines:

```text
ArrayList is usually the default List because random access and iteration are fast.
```

```text
HashMap average lookup is O(1), but collisions and resizing matter.
```

```text
HashMap uses equals and hashCode; TreeMap uses ordering.
```

```text
ConcurrentHashMap is thread-safe for individual operations, but compound logic needs atomic methods.
```

```text
PriorityQueue guarantees priority at the head, not sorted iteration.
```

```text
Fail-fast iterators are best-effort bug detectors, not synchronization mechanisms.
```

---

## 27. Official Source Notes

Use official sources when refreshing:

- Java Collections Framework API: `https://docs.oracle.com/en/java/javase/`
- Java concurrency collections API: `https://docs.oracle.com/en/java/javase/`
- Java Language Specification: `https://docs.oracle.com/javase/specs/`

Interview safety line:

```text
When collection behavior is version-sensitive, I explain the concept rather than relying
on exact internal thresholds unless the interviewer asks for them.
```
