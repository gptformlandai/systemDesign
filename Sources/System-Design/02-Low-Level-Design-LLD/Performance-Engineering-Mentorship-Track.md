# Performance Engineering - Mentorship Track

> Goal: build final, interview-ready LLD performance intuition with clear diagnostic thinking, practical examples, and no vague advice like "just optimize it".

---

## How We Will Use This Sheet

- We will keep this sheet focused on `2.5 Performance Engineering`.
- We will learn performance as a measurement-first discipline, not guesswork.
- Every topic connects code behavior to latency, memory, allocation, database calls, or throughput.
- Java examples show enterprise backend performance patterns and JVM concerns.
- Python examples show compact runnable simulations that make the mechanism easy to see.
- Code comments marked `Performance concept` show exactly where the performance idea is applied.

---

## Roadmap for This Sheet

1. Profiling
2. Memory leaks
3. GC basics
4. Object lifecycle
5. Lazy loading
6. Batching

---

## Performance Decision Map

| If the symptom is... | First think about... | Main question |
|---|---|---|
| Endpoint is slow but cause is unknown | Profiling | Where is time actually spent? |
| Memory keeps growing after traffic stabilizes | Memory leaks | What objects are still strongly referenced? |
| Latency spikes happen with allocation pressure | GC basics | Are collections pausing or stealing CPU? |
| Too many objects are created per request | Object lifecycle | Can object creation, scope, or reuse be improved safely? |
| Expensive data is loaded before it is needed | Lazy loading | Can we defer work without creating N+1 calls or hidden latency? |
| Many small network/database calls dominate latency | Batching | Can we combine work into fewer calls while keeping latency acceptable? |

---

## Confusion Map

| Common confusion | Clear distinction |
|---|---|
| Profiling vs logging | Profiling measures where time or memory is spent. Logging records events and context. |
| Benchmark vs production profiling | Benchmark isolates a small operation. Production profiling observes real workload behavior. |
| Memory leak vs high memory usage | High memory may be normal caching. Leak means memory is retained unintentionally and grows without bound. |
| GC problem vs allocation problem | GC pain is often caused by excessive allocation, object retention, or heap sizing, not the collector alone. |
| Lazy loading vs caching | Lazy loading delays work until needed. Caching reuses a previously computed or loaded result. |
| Lazy loading vs N+1 problem | Lazy loading can create N+1 calls if each object loads related data separately. |
| Batching vs bulk transaction | Batching groups operations. A transaction defines atomic commit/rollback semantics. |
| Object pooling vs reuse | Pooling is useful for expensive resources. Pooling normal short-lived objects can hurt modern GC performance. |
| Optimization vs performance engineering | Optimization changes code. Performance engineering measures, explains, changes, and verifies. |

---

## Code Example Convention

- Section 14 uses Java to show production-style backend examples.
- Section 15 uses Python to show compact runnable simulations.
- Examples use hotel booking, search, pricing, and notification domains.
- Comments marked `Performance concept` show the exact performance idea being demonstrated.
- The safest performance answer is always: measure first, change second, verify third.

---

# Topic 1: Profiling

> Track: 2.5 Performance Engineering
> Scope: CPU profiling, wall-clock profiling, allocation profiling, latency tracing, hotspots, flame graphs, p95/p99, and measurement-first debugging

---

## 1. Intuition

Imagine a hotel checkout line is slow.

Guessing is easy:
- maybe payment is slow
- maybe the receptionist is slow
- maybe the printer is slow
- maybe guests ask too many questions

Profiling is timing each step so you know where the delay actually lives.

Short memory trick:
- do not guess
- measure the path
- optimize the hotspot

---

## 2. Definition

- Definition: Profiling is the process of measuring a program's runtime behavior to identify where time, memory, CPU, allocation, or I/O is spent.
- Category: Performance diagnosis and observability
- Core idea: Find the real bottleneck before changing code.

Types of profiling:
- CPU profiling: where CPU time is spent.
- Wall-clock profiling: where elapsed time is spent, including I/O waits.
- Allocation profiling: where objects are created.
- Memory profiling: what objects are retained.
- Tracing: which request path or downstream call is slow.

---

## 3. Why It Exists

Performance bugs are often non-obvious.

Common wrong guesses:
- optimizing string concatenation while database calls dominate
- changing algorithms while network latency dominates
- increasing thread pool size while downstream service is saturated
- blaming GC while object retention is the real issue

Profiling exists because intuition is unreliable under real workloads.

It answers:
- which method is hot?
- which call is slow?
- which allocation dominates?
- which endpoint has p99 spikes?
- which dependency consumes latency budget?

---

## 4. Reality

Profiling is used in:

- slow API debugging
- p95/p99 latency investigations
- high CPU incidents
- memory growth investigations
- GC tuning investigations
- database query optimization
- hot loop optimization
- regression detection after releases

Common tools:
- Java Flight Recorder
- async-profiler
- VisualVM
- YourKit
- JProfiler
- `jcmd`, `jstat`, heap dumps
- Python `cProfile`, `py-spy`, `tracemalloc`
- distributed tracing tools
- database `EXPLAIN`

Interview maturity:
- say what you measure before saying what you change

---

## 5. How It Works

Performance investigation flow:

1. Define the symptom: slow endpoint, high CPU, memory growth, latency spike.
2. Define the metric: average, p95, p99, CPU, allocation, heap, query count.
3. Reproduce with realistic input or observe production safely.
4. Capture profile or trace.
5. Identify top hotspot or wait source.
6. Form a hypothesis.
7. Change one thing.
8. Measure again.
9. Keep or revert based on evidence.

Example request latency budget:

```text
Booking search API p95 = 850 ms
Hotel search DB query = 520 ms
Price enrichment = 180 ms
Serialization = 40 ms
Network and framework overhead = 110 ms
```

The first useful optimization is probably search query or price enrichment, not JSON formatting.

---

## 6. What Problem It Solves

- Primary problem solved: unknown performance bottleneck.
- Secondary benefits: prevents wasted optimization, reveals regressions, guides capacity planning.
- Systems impact: improves latency, throughput, CPU efficiency, and operational confidence.

Profiling solves uncertainty.

It does not automatically solve the bottleneck. It tells you where to look.

---

## 7. When to Rely on It

Use profiling when:

- endpoint latency is high
- CPU usage is high
- memory usage grows
- GC is frequent or expensive
- optimization target is unclear
- a release caused performance regression
- one request path behaves differently from others

Interviewer keywords:
- slow API
- high CPU
- p99 latency
- memory pressure
- bottleneck
- hotspot
- performance regression

---

## 8. When Not to Overdo It

Avoid heavy profiling when:

- issue is already obvious from metrics or logs
- profiler overhead would disturb production too much
- code path is tiny and directly measurable
- you do not have representative input
- optimization would not matter to user-facing latency or cost

Use simple timing or counters first when they answer the question.

Use deeper profiling when simple measurement cannot explain the issue.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Finds real bottlenecks | Some profilers add overhead |
| Prevents guess-based optimization | Profiles can be misread |
| Shows CPU, allocation, and wait hotspots | Synthetic workloads can mislead |
| Helps prove improvement | Production profiling needs care |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: evidence-based optimization.
- Give up: time spent measuring before changing code.
- Latency impact: profiling can add overhead if too invasive.
- Complexity impact: profiles require interpretation.

### Common Mistakes

- Mistake: optimizing without profiling.
- Why it is wrong: you may optimize code that is not on the hot path.
- Better approach: measure first.

- Mistake: using average latency only.
- Why it is wrong: users feel tail latency.
- Better approach: look at p95 and p99.

- Mistake: confusing CPU time with wall-clock time.
- Why it is wrong: slow requests may be waiting on I/O, not burning CPU.
- Better approach: use tracing or wall-clock profiling for I/O-heavy systems.

- Mistake: measuring with unrealistic input size.
- Why it is wrong: small data may hide algorithm and query issues.
- Better approach: use representative production-like data.

---

## 11. Key Numbers

Performance heuristics:

- p50: typical user experience.
- p95: slow but common user experience.
- p99: tail behavior that often reveals contention, GC, or dependency spikes.
- 1 ms inside a loop of 1000 items becomes 1 second.
- 100 small database calls are often worse than one well-designed batch query.
- A 10 percent optimization in a 5 percent code path gives little overall gain.

Memory number:
- Optimize the path that dominates total time, not the code that looks ugly.

---

## 12. Failure Modes

- False hotspot: profiler overhead or test setup distorts result.
- Wrong workload: optimized path is not real production path.
- Ignoring tail latency: average improves but p99 worsens.
- Ignoring allocation: CPU looks fine but GC pauses increase.
- Optimizing locally: one service improves while downstream gets overloaded.

Mitigations:
- profile realistic workloads
- compare before and after
- check p95/p99
- inspect CPU and allocation
- include downstream dependency metrics

---

## 13. Scenario

- Product / system: Hotel search API
- Requirement: reduce p95 latency from 900 ms to under 300 ms
- Good design: profile request path before optimizing code
- Why this concept fits: bottleneck might be DB, enrichment, serialization, or remote calls
- What would go wrong without it: team may optimize local code while database query dominates latency

---

## 14. Java Code Sample

### Lightweight timing around request stages

```java
import java.time.Duration;
import java.time.Instant;
import java.util.List;

record Hotel(String hotelId, int nightlyRate) {
}

interface HotelRepository {
    List<Hotel> search(String city);
}

interface PriceEnricher {
    List<Hotel> enrich(List<Hotel> hotels);
}

class SearchProfiler {
    public <T> T time(String stageName, TimedOperation<T> operation) {
        Instant start = Instant.now();
        try {
            return operation.run();
        } finally {
            Duration elapsed = Duration.between(start, Instant.now());
            // Performance concept: measure each stage so optimization targets are evidence-based.
            System.out.println(stageName + " took " + elapsed.toMillis() + " ms");
        }
    }
}

interface TimedOperation<T> {
    T run();
}

class HotelSearchService {
    private final HotelRepository repository;
    private final PriceEnricher priceEnricher;
    private final SearchProfiler profiler;

    HotelSearchService(HotelRepository repository, PriceEnricher priceEnricher, SearchProfiler profiler) {
        this.repository = repository;
        this.priceEnricher = priceEnricher;
        this.profiler = profiler;
    }

    public List<Hotel> search(String city) {
        List<Hotel> hotels = profiler.time("database search", () -> repository.search(city));
        // Performance concept: stage timing separates DB latency from enrichment latency.
        return profiler.time("price enrichment", () -> priceEnricher.enrich(hotels));
    }
}
```

Key idea:
- profiling starts by measuring the request stages so the team stops guessing

---

## 15. Python Mini Program / Simulation

This mini program uses `cProfile` to show where time is spent.

```python
import cProfile
import pstats
import time


def load_hotels() -> list[int]:
    time.sleep(0.05)
    return list(range(1000))


def enrich_price(hotel_id: int) -> int:
    return hotel_id * 2


def search_hotels() -> list[int]:
    hotels = load_hotels()
    # Performance concept: profiling reveals whether time is in loading, looping, or enrichment.
    return [enrich_price(hotel_id) for hotel_id in hotels]


def main() -> None:
    profiler = cProfile.Profile()
    profiler.enable()
    search_hotels()
    profiler.disable()

    stats = pstats.Stats(profiler).sort_stats("cumtime")
    stats.print_stats(5)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- profiling gives a ranked view of expensive functions
- cumulative time is often more useful than guessing by code size
- measurement comes before optimization

---

## 16. Practical Question

> A hotel search endpoint is slow. What is your first step before changing code?

---

## 17. Strong Answer

I would first define the symptom and metric: for example p95 latency is 900 ms for hotel search. Then I would profile or trace the request path with realistic input. I would break down time across database search, remote calls, enrichment, serialization, and cache access.

If the database query consumes most of the time, I would inspect query plan, indexes, result size, and N+1 behavior. If enrichment dominates, I would inspect loops, batching, and remote call patterns. I would make one change and measure again.

I would avoid starting with random code optimization because it may not affect the hot path.

---

## 18. Revision Notes

- One-line summary: Profiling tells you where time or memory is actually spent.
- Three keywords: measure, hotspot, verify
- One interview trap: optimizing before measuring.
- One memory trick: profile first, optimize second, prove third.

---

# Topic 2: Memory Leaks

> Track: 2.5 Performance Engineering
> Scope: object retention, strong references, caches, listeners, static collections, heap growth, leak diagnosis, and bounded memory design

---

## 1. Intuition

Think of a hotel lost-and-found room.

If staff keep every item forever, the room fills up. Some items are useful to keep temporarily. But without cleanup rules, storage grows without bound.

A memory leak is similar: objects that should be gone are still reachable, so the garbage collector cannot reclaim them.

Short memory trick:
- unused object plus strong reference equals retained memory
- retained forever equals leak
- caches need boundaries

---

## 2. Definition

- Definition: A memory leak happens when memory that is no longer logically needed remains reachable and cannot be reclaimed.
- Category: Runtime memory management problem
- Core idea: The program accidentally keeps references to objects beyond their useful lifetime.

Common leak sources:
- unbounded caches
- static collections
- listener/subscriber lists that never remove entries
- thread locals not cleared
- maps keyed by request/user/session forever
- queues that producers fill faster than consumers drain
- large object graphs retained by one small reference

---

## 3. Why It Exists

Garbage collection reclaims unreachable objects.

It does not know whether an object is logically useful.

If your code still holds a strong reference, the object is considered alive.

Example:
- request finishes
- response object is no longer needed
- debug cache still references it by request id
- GC cannot collect it
- memory grows every request

Memory leaks exist because object lifetime in the business sense and reachability in the runtime sense can diverge.

---

## 4. Reality

Memory leaks appear in:

- service-level caches without eviction
- maps keyed by user/session/request id
- event listeners not unsubscribed
- metrics labels with unbounded cardinality
- background queues with stuck consumers
- static collections used for debugging
- ORM sessions retaining too many entities
- large file buffers stored accidentally

Production symptoms:
- heap grows after traffic stabilizes
- GC becomes more frequent
- full GC fails to reduce heap
- out-of-memory error appears after hours or days
- pods restart due to memory limit

---

## 5. How It Works

Leak investigation flow:

1. Observe memory growth over time.
2. Check whether memory drops after GC.
3. Capture heap dump or memory profile.
4. Identify largest retained object types.
5. Find retention path: who references them?
6. Decide whether retention is valid cache or leak.
7. Add eviction, removal, weak reference, scope fix, or backpressure.
8. Verify heap stabilizes under repeated load.

Important phrase:
- leak diagnosis is about retained objects, not just allocated objects

---

## 6. What Problem It Solves

- Primary problem solved: unbounded memory growth from unintended object retention.
- Secondary benefits: stable heap, fewer GC pauses, fewer restarts, predictable resource use.
- Systems impact: improves availability and latency by preventing memory pressure.

Memory leak work is both correctness and performance work.

The service may be functionally correct but operationally unstable.

---

## 7. When to Rely on Leak Analysis

Think memory leak when:

- heap grows continuously
- memory does not drop after full GC
- pod memory grows with request count
- cache size grows without bound
- listener count grows with sessions
- queue depth never returns to normal
- OOM happens after long uptime

Interviewer keywords:
- memory leak
- heap dump
- retained objects
- unbounded cache
- listener cleanup
- out of memory
- GC pressure

---

## 8. When Not to Call It a Leak Too Early

Avoid calling memory usage a leak when:

- cache is intentionally warming up to a fixed size
- heap expands but stabilizes
- memory is high because traffic is high
- batch job intentionally holds a working set
- GC has not run yet
- native memory, thread stacks, or direct buffers are the real issue

Better wording:
- first say "memory growth" or "retention issue"
- confirm leak after retention analysis

---

## 9. Pros and Cons

| Fix approach | Pros | Cons |
|---|---|---|
| Eviction policy | Keeps cache useful and bounded | Needs size/TTL tuning |
| Explicit unsubscribe | Prevents listener leaks | Requires lifecycle discipline |
| Weak references | Allows GC when only weakly reachable | Can be confusing and not always appropriate |
| Backpressure | Prevents queue memory growth | May reject or delay work |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: stable memory and fewer GC issues.
- Give up: unlimited retention convenience.
- Latency impact: eviction may cause more reloads.
- Complexity impact: lifecycle cleanup must be designed.

### Common Mistakes

- Mistake: using a plain `HashMap` as a cache forever.
- Why it is wrong: it grows without eviction.
- Better approach: use bounded cache with TTL/size limit.

- Mistake: removing object from main collection but not from listener list.
- Why it is wrong: listener reference keeps object alive.
- Better approach: unsubscribe on lifecycle end.

- Mistake: only looking at allocation rate.
- Why it is wrong: high allocation may be fine if objects die young.
- Better approach: inspect retained heap and reference paths.

---

## 11. Key Numbers

Memory heuristics:

- Heap growth that stabilizes is not automatically a leak.
- Heap growth that never stabilizes under stable traffic is suspicious.
- Cache needs a maximum size, TTL, or both.
- High-cardinality metrics labels can create millions of retained series.
- Queue capacity must be bounded if producers can outrun consumers.

Memory number:
- A leak is about what remains reachable, not what was allocated once.

---

## 12. Failure Modes

- OOM kill or `OutOfMemoryError`.
- Increased GC frequency.
- Long full GC pauses.
- Cache evicts nothing and consumes heap.
- Listener list retains inactive users.
- Queue stores millions of pending jobs.

Mitigations:
- bounded caches
- listener cleanup
- queue backpressure
- heap dump analysis
- retention path analysis
- load test repeated cycles

---

## 13. Scenario

- Product / system: Hotel details cache
- Requirement: cache hotel details to reduce database load without memory growing forever
- Good design: bounded cache with maximum entries or TTL
- Why this concept fits: cache retention must match object usefulness
- What would go wrong without it: every searched hotel stays in memory forever

---

## 14. Java Code Sample

### Unbounded cache leak vs bounded cache

```java
import java.util.LinkedHashMap;
import java.util.Map;

record HotelDetails(String hotelId, String description) {
}

class LeakyHotelCache {
    private final Map<String, HotelDetails> cache = new LinkedHashMap<>();

    public void put(HotelDetails hotelDetails) {
        // Performance concept: unbounded strong references can retain every hotel forever.
        cache.put(hotelDetails.hotelId(), hotelDetails);
    }
}

class BoundedHotelCache extends LinkedHashMap<String, HotelDetails> {
    private final int maxEntries;

    BoundedHotelCache(int maxEntries) {
        super(16, 0.75f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, HotelDetails> eldest) {
        // Performance concept: eviction bounds retained memory.
        return size() > maxEntries;
    }
}
```

Key idea:
- a cache without an eviction story is often a slow memory leak

---

## 15. Python Mini Program / Simulation

This mini program shows a bounded LRU-style cache using the standard library.

```python
from collections import OrderedDict
from dataclasses import dataclass


@dataclass(frozen=True)
class HotelDetails:
    hotel_id: str
    description: str


class BoundedHotelCache:
    def __init__(self, max_entries: int) -> None:
        self.max_entries = max_entries
        self._cache: OrderedDict[str, HotelDetails] = OrderedDict()

    def put(self, details: HotelDetails) -> None:
        if details.hotel_id in self._cache:
            self._cache.move_to_end(details.hotel_id)
        self._cache[details.hotel_id] = details

        # Performance concept: evict old entries so memory has a hard boundary.
        while len(self._cache) > self.max_entries:
            self._cache.popitem(last=False)

    def size(self) -> int:
        return len(self._cache)


def main() -> None:
    cache = BoundedHotelCache(max_entries=3)
    for index in range(10):
        cache.put(HotelDetails(f"hotel-{index}", "details"))
    print("cache size", cache.size())


if __name__ == "__main__":
    main()
```

What this demonstrates:
- memory stays bounded even as new hotels are seen
- object retention is a design decision
- cache usefulness and memory safety must be balanced

---

## 16. Practical Question

> A service memory graph grows for hours until it restarts. How would you investigate?

---

## 17. Strong Answer

I would first confirm whether memory growth stabilizes or keeps increasing under stable traffic. Then I would check GC behavior and capture a heap dump or memory profile. The key is to inspect retained objects and reference paths, not only allocation rate.

If a cache, listener list, queue, or static map is retaining objects, I would add a lifecycle boundary: eviction, TTL, unsubscribe, bounded queue, or scoped ownership. Then I would repeat the workload and verify heap stabilizes after GC.

I would be careful not to call every high-memory situation a leak. A warmed cache can be normal if it is bounded.

---

## 18. Revision Notes

- One-line summary: Memory leaks happen when unused objects remain reachable.
- Three keywords: retention, references, eviction
- One interview trap: looking only at allocations instead of retained heap.
- One memory trick: GC collects unreachable objects, not useless objects.

---

# Topic 3: GC Basics

> Track: 2.5 Performance Engineering
> Scope: heap, young generation, old generation, allocation rate, object promotion, stop-the-world pauses, GC logs, and tuning mindset

---

## 1. Intuition

Think of hotel housekeeping.

Most guests leave quickly, so rooms can be cleaned quickly. Some guests stay longer and move into long-stay rooms. If too many rooms become cluttered, housekeeping must pause normal work and clean deeply.

Garbage collection works similarly:
- short-lived objects are cheap when they die young
- long-lived objects occupy heap longer
- too much allocation or retention creates GC pressure

Short memory trick:
- allocate
- survive
- promote
- collect

---

## 2. Definition

- Definition: Garbage collection is automatic memory management that reclaims heap objects no longer reachable by the program.
- Category: Runtime memory management
- Core idea: The runtime tracks reachability and frees memory without explicit `free` calls.

JVM mental model:
- heap stores objects
- young generation stores newly allocated objects
- old generation stores longer-lived objects
- minor GC collects young generation
- major/full GC involves old generation and can be more expensive

---

## 3. Why It Exists

Manual memory management is error-prone:
- free too early and code crashes
- forget to free and memory leaks
- double free causes corruption

GC exists to remove manual deallocation from normal application code.

But GC does not remove memory thinking.

You still need to care about:
- allocation rate
- object lifetime
- retained references
- heap size
- pause time
- promotion rate
- large object allocation

---

## 4. Reality

GC affects:

- Java services
- JVM-based frameworks
- Kotlin/Scala services
- Go services
- managed runtimes like .NET
- Python reference counting plus cyclic GC

Production symptoms of GC pressure:
- p99 latency spikes
- CPU spent in GC
- frequent minor GC
- full GC pauses
- heap after GC remains high
- allocation rate increases after release

JVM tools:
- GC logs
- Java Flight Recorder
- `jstat`
- heap dumps
- allocation profiler
- APM runtime metrics

---

## 5. How It Works

Simple JVM flow:

1. Code allocates objects on heap.
2. Most request-local objects die quickly.
3. Minor GC collects young generation.
4. Objects that survive multiple collections may move to old generation.
5. If old generation fills, major/full GC may occur.
6. If GC cannot reclaim enough memory, OOM may happen.

Performance lens:
- high allocation rate causes frequent young GC
- high retention causes old generation pressure
- long object lifetimes increase promotion
- large temporary objects can hurt memory locality and GC

---

## 6. What Problem It Solves

- Primary problem solved: automatic reclamation of unreachable objects.
- Secondary benefits: simpler application code, fewer manual memory bugs.
- Systems impact: improves developer productivity but introduces runtime pause and CPU trade-offs.

GC tuning is rarely the first fix.

Usually first check:
- are we allocating too much?
- are we retaining too much?
- is heap sized properly?
- did traffic or payload size change?

---

## 7. When to Rely on GC Analysis

Think GC analysis when:

- p99 latency spikes correlate with GC pauses
- CPU is high and GC time is high
- heap after GC keeps growing
- allocation rate suddenly increases
- full GC appears frequently
- service restarts with OOM

Interviewer keywords:
- GC pause
- allocation rate
- heap pressure
- young generation
- old generation
- memory churn
- stop-the-world

---

## 8. When Not to Blame GC Too Early

Do not blame GC when:

- request is slow due to database calls
- thread pool is saturated
- downstream service is slow
- CPU is high due to algorithmic work
- heap is stable and GC time is low
- p99 spike aligns with lock contention, not GC

Better answer:
- correlate GC metrics with latency before concluding

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Removes manual deallocation | Can introduce pause time |
| Handles most short-lived objects efficiently | Allocation-heavy code increases GC work |
| Prevents many memory safety bugs | Retained references still leak memory |
| Runtime can optimize allocation paths | Tuning requires measurement |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: automatic memory management.
- Give up: some control over when cleanup happens.
- Latency impact: GC pauses can affect tail latency.
- CPU impact: high allocation rate consumes collector CPU.

### Common Mistakes

- Mistake: tuning GC before checking allocation and retention.
- Why it is wrong: tuning may hide the real problem temporarily.
- Better approach: inspect allocation rate and retained heap first.

- Mistake: object pooling normal small objects.
- Why it is wrong: modern GC handles short-lived objects well; pooling can retain memory and add complexity.
- Better approach: pool only expensive external resources, not ordinary DTOs.

- Mistake: increasing heap blindly.
- Why it is wrong: larger heap may reduce frequency but increase pause cost or hide leaks.
- Better approach: size heap based on working set and latency goals.

---

## 11. Key Numbers

GC heuristics:

- Most request-local objects should die young.
- High allocation rate can cause frequent minor GC even without leaks.
- Heap after full GC approximates live set.
- Old generation growth under stable traffic suggests retention problem.
- p99 latency is more sensitive to GC pauses than average latency.

Memory number:
- GC pain usually comes from allocation rate, retained live set, or heap sizing.

---

## 12. Failure Modes

- Frequent minor GC due to allocation churn.
- Full GC pauses due to old generation pressure.
- Promotion failure when objects survive too long.
- OOM when live set exceeds heap.
- Latency spikes due to stop-the-world pauses.
- CPU lost to GC instead of useful work.

Mitigations:
- reduce unnecessary allocations
- bound caches and queues
- avoid retaining request objects
- stream large payloads
- inspect GC logs
- choose and tune collector based on workload

---

## 13. Scenario

- Product / system: Hotel price aggregation API
- Requirement: reduce p99 spikes caused by high allocation during large search responses
- Good design: measure allocation rate, reduce temporary objects, avoid building huge intermediate lists
- Why this concept fits: GC pressure affects latency even if business logic is correct
- What would go wrong without it: service keeps passing functional tests but p99 remains unstable

---

## 14. Java Code Sample

### Reducing unnecessary temporary allocations

```java
import java.util.ArrayList;
import java.util.List;

record RawPrice(String hotelId, int cents) {
}

record PriceView(String hotelId, String displayPrice) {
}

class PriceMapper {
    public List<PriceView> mapWithAvoidableChurn(List<RawPrice> rawPrices) {
        List<PriceView> views = new ArrayList<>();
        for (RawPrice rawPrice : rawPrices) {
            // Performance concept: repeated intermediate string creation increases allocation rate.
            String display = "$" + (rawPrice.cents() / 100) + "." + (rawPrice.cents() % 100);
            views.add(new PriceView(rawPrice.hotelId(), display));
        }
        return views;
    }

    public List<PriceView> mapWithPreSizedList(List<RawPrice> rawPrices) {
        // Performance concept: pre-sizing avoids repeated internal array growth for large result sets.
        List<PriceView> views = new ArrayList<>(rawPrices.size());
        for (RawPrice rawPrice : rawPrices) {
            views.add(new PriceView(rawPrice.hotelId(), formatCents(rawPrice.cents())));
        }
        return views;
    }

    private String formatCents(int cents) {
        return "$" + (cents / 100) + "." + String.format("%02d", cents % 100);
    }
}
```

Key idea:
- GC basics matter when code creates many temporary objects on a hot path

---

## 15. Python Mini Program / Simulation

This mini program uses `tracemalloc` to compare allocation behavior.

```python
import tracemalloc


def build_all_price_strings(count: int) -> list[str]:
    # Performance concept: building a large full list retains every string at once.
    return [f"hotel-{index}: ${index % 300}" for index in range(count)]


def stream_price_strings(count: int):
    # Performance concept: generator yields one item at a time, reducing retained working set.
    for index in range(count):
        yield f"hotel-{index}: ${index % 300}"


def main() -> None:
    tracemalloc.start()
    prices = build_all_price_strings(100_000)
    current, peak = tracemalloc.get_traced_memory()
    print("list peak bytes", peak)
    del prices

    tracemalloc.reset_peak()
    for _ in stream_price_strings(100_000):
        pass
    current, peak = tracemalloc.get_traced_memory()
    print("stream peak bytes", peak)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- allocation and retention are different
- streaming can reduce peak memory
- lower peak memory can reduce GC and memory pressure

---

## 16. Practical Question

> A Java service has p99 latency spikes and GC logs show frequent collections. How do you approach it?

---

## 17. Strong Answer

I would first correlate GC events with p99 latency spikes. Then I would inspect allocation rate, heap after GC, promotion rate, and retained heap. If allocation rate is high, I would profile allocations and reduce unnecessary temporary objects on hot paths. If heap after GC keeps growing, I would suspect retention or leak.

I would avoid immediately tuning GC flags. GC tuning can help, but it should come after understanding whether the problem is allocation churn, live-set size, leak, or heap sizing.

---

## 18. Revision Notes

- One-line summary: GC reclaims unreachable objects, but allocation and retention patterns control how painful GC becomes.
- Three keywords: allocation, retention, pause
- One interview trap: blaming GC before measuring allocation and live set.
- One memory trick: GC cleans garbage, not bad design.

---

# Topic 4: Object Lifecycle

> Track: 2.5 Performance Engineering
> Scope: creation cost, scope, reuse, pooling, request-local objects, expensive resources, lifecycle ownership, and cleanup

---

## 1. Intuition

Think of hotel resources.

A paper receipt can be created and thrown away for each guest. A payment terminal should not be created for every transaction. A room key should be active only for the stay and then invalidated.

Object lifecycle is about creating, owning, reusing, and cleaning objects at the right scope.

Short memory trick:
- cheap short-lived objects are fine
- expensive resources need lifecycle management
- wrong scope creates leaks or slowness

---

## 2. Definition

- Definition: Object lifecycle is the span from object creation to last use and cleanup, including who owns it and how long it should live.
- Category: LLD performance and resource design
- Core idea: Match object lifetime to actual usefulness and cost.

Lifecycle scopes:
- method-local
- request-scoped
- session-scoped
- singleton/application-scoped
- pooled resource
- persisted entity

---

## 3. Why It Exists

Wrong object lifecycle causes performance and correctness problems.

Examples:
- creating HTTP client per request causes connection churn
- keeping request objects in singleton fields leaks memory
- pooling small DTOs adds complexity and retention
- not closing file streams leaks descriptors
- reusing mutable object across requests causes data bleed

Object lifecycle thinking exists because not all objects should live the same amount of time.

---

## 4. Reality

Object lifecycle matters for:

- HTTP clients
- database connections
- thread pools
- file handles
- buffers
- DTOs
- entity managers / sessions
- caches
- request contexts
- serializers and parsers

Common backend rule:
- reusable clients and pools are long-lived
- request data is short-lived
- mutable shared objects must be protected or avoided

---

## 5. How It Works

Lifecycle design flow:

1. Identify object creation cost.
2. Identify whether object is thread-safe.
3. Identify whether object holds external resources.
4. Choose scope: local, request, singleton, or pool.
5. Define cleanup if resource is external.
6. Avoid sharing mutable request data across requests.
7. Avoid pooling objects that are cheap and short-lived.

Examples:
- `BookingRequest`: request-scoped value object.
- `HttpClient`: application-scoped reusable client.
- `DatabaseConnection`: pooled resource.
- `ByteArrayOutputStream`: method-local or pooled only if measured.
- `ThreadPoolExecutor`: application-scoped with shutdown.

---

## 6. What Problem It Solves

- Primary problem solved: performance and memory issues caused by wrong object lifetime.
- Secondary benefits: fewer leaks, less allocation churn, safer resource cleanup, clearer ownership.
- Systems impact: improves latency and stability by avoiding unnecessary setup and retained garbage.

Object lifecycle is especially important when:
- creation is expensive
- object owns OS/network resource
- object is mutable
- object is shared across threads

---

## 7. When to Rely on It

Think object lifecycle when:

- expensive client is created per request
- heap retains request objects after request ends
- resources are not closed
- object pooling is proposed
- singleton has mutable request state
- performance issue involves allocation churn

Interviewer keywords:
- object creation overhead
- reuse
- pooling
- cleanup
- request scope
- singleton scope
- resource lifecycle

---

## 8. When Not to Over-Engineer It

Avoid lifecycle complexity when:

- object is tiny and cheap
- object is immutable and request-local
- pooling adds contention or stale state risk
- modern GC handles allocation cheaply
- no profiling evidence shows creation cost matters

Use simple construction for normal DTOs and value objects.

Reserve pooling for expensive external resources such as connections, threads, or large buffers after measurement.

---

## 9. Pros and Cons

| Lifecycle choice | Pros | Cons |
|---|---|---|
| Short-lived local object | Simple, safe, easy for GC | Allocation cost if extremely hot path |
| Singleton reusable client | Avoids repeated setup | Must be thread-safe and configured correctly |
| Pool | Controls expensive resources | Adds contention and cleanup complexity |
| Cache | Avoids recomputation | Can leak or serve stale data |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: better resource efficiency.
- Give up: lifecycle management simplicity.
- Latency impact: reuse avoids setup cost.
- Memory impact: long-lived objects increase retained heap.

### Common Mistakes

- Mistake: creating HTTP client per request.
- Why it is wrong: loses connection reuse and adds setup overhead.
- Better approach: create one configured client and reuse it safely.

- Mistake: pooling small DTOs.
- Why it is wrong: GC is usually faster and simpler than manual reuse.
- Better approach: allocate short-lived immutable DTOs normally.

- Mistake: singleton object stores request-specific fields.
- Why it is wrong: data leaks across users and threads.
- Better approach: keep request state local or scoped.

---

## 11. Key Numbers

Lifecycle heuristics:

- Network clients, database pools, and thread pools should usually be long-lived.
- Request DTOs should usually be short-lived.
- Objects holding OS resources need explicit close/shutdown.
- Pool size should match downstream capacity.
- Object pooling normal allocations is suspicious unless profiling proves need.

Memory number:
- Match lifetime to usefulness: too short wastes setup, too long leaks memory.

---

## 12. Failure Modes

- Connection churn from per-request clients.
- Data bleed from reused mutable request object.
- Memory leak from long-lived references.
- Resource leak from missing close.
- Pool exhaustion from borrowed objects not returned.
- Stale cache data from over-long lifecycle.

Mitigations:
- define ownership
- use try-with-resources / context managers
- keep request state local
- reuse thread-safe expensive clients
- monitor pool usage
- avoid unnecessary pooling

---

## 13. Scenario

- Product / system: Booking payment client
- Requirement: charge payments through an external provider without recreating network client per request
- Good design: reusable thread-safe payment client is application-scoped; request data remains request-scoped
- Why this concept fits: client setup is expensive but request data must not be shared
- What would go wrong without it: latency increases from connection churn or user data leaks across requests

---

## 14. Java Code Sample

### Correct object lifetime for payment client and request data

```java
record PaymentRequest(String userId, int amountCents) {
}

class PaymentHttpClient {
    public String postCharge(PaymentRequest request) {
        return "payment-ref-" + request.userId();
    }
}

class PaymentService {
    private final PaymentHttpClient paymentHttpClient;

    PaymentService(PaymentHttpClient paymentHttpClient) {
        // Performance concept: expensive reusable client is injected once and reused.
        this.paymentHttpClient = paymentHttpClient;
    }

    public String charge(String userId, int amountCents) {
        // Performance concept: request-specific data remains short-lived and local to the call.
        PaymentRequest request = new PaymentRequest(userId, amountCents);
        return paymentHttpClient.postCharge(request);
    }
}
```

Key idea:
- reuse expensive thread-safe infrastructure, but keep user/request data short-lived

---

## 15. Python Mini Program / Simulation

This mini program shows why setup cost should not happen repeatedly when the object is safely reusable.

```python
import time
from dataclasses import dataclass


class PaymentClient:
    def __init__(self) -> None:
        # Performance concept: simulate expensive connection/client setup.
        time.sleep(0.05)

    def charge(self, user_id: str, amount_cents: int) -> str:
        return f"payment-ref-{user_id}-{amount_cents}"


@dataclass(frozen=True)
class PaymentRequest:
    user_id: str
    amount_cents: int


class PaymentService:
    def __init__(self, client: PaymentClient) -> None:
        self.client = client

    def charge(self, request: PaymentRequest) -> str:
        # Performance concept: request object is short-lived, client is reused.
        return self.client.charge(request.user_id, request.amount_cents)


def main() -> None:
    client = PaymentClient()
    service = PaymentService(client)
    for index in range(3):
        print(service.charge(PaymentRequest(f"user-{index}", 1000)))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- expensive setup belongs outside the hot request loop
- request data should stay scoped to the request
- reuse must be safe, not just faster

---

## 16. Practical Question

> A service creates a new HTTP client for every payment request. What is the performance issue and how would you fix it?

---

## 17. Strong Answer

Creating a new HTTP client per request can lose connection pooling, repeat TLS/setup costs, increase latency, and consume resources. I would create a properly configured, thread-safe client at application startup and inject it into the payment service.

I would keep request-specific data local to each call. I would also define shutdown behavior for the client if it owns resources.

I would avoid pooling normal small DTOs unless profiling proves allocation is a real issue. Modern GC handles short-lived objects efficiently.

---

## 18. Revision Notes

- One-line summary: Object lifecycle means choosing the right lifetime and owner for each object.
- Three keywords: scope, reuse, cleanup
- One interview trap: pooling ordinary objects without evidence.
- One memory trick: expensive resource lives longer; request data dies quickly.

---

# Topic 5: Lazy Loading

> Track: 2.5 Performance Engineering
> Scope: deferred work, lazy initialization, expensive data loading, N+1 queries, caching interaction, hidden latency, and thread-safe lazy initialization

---

## 1. Intuition

Think of a hotel brochure.

When a guest views the hotel list, you do not need to print every room photo, review, amenity, and policy immediately. Load the heavy details only if the guest opens a specific hotel.

That is lazy loading.

Short memory trick:
- do not load until needed
- but do not hide many tiny loads

---

## 2. Definition

- Definition: Lazy loading delays creation or fetching of data until it is actually needed.
- Category: Performance optimization and object/data access pattern
- Core idea: Avoid unnecessary work on paths that may not use the data.

Lazy loading can apply to:
- expensive objects
- related database entities
- remote API data
- images/files
- computed values
- configuration snapshots

---

## 3. Why It Exists

Eager loading can waste time and memory.

Example:
- search page returns 50 hotels
- only hotel id, name, and price are shown
- eager loading also fetches all reviews and room photos
- most of that data is not used

Lazy loading exists to defer expensive work until the program knows it is needed.

But lazy loading can create a trap:
- loop over 50 hotels
- each `hotel.getReviews()` triggers one query
- now there are 51 queries instead of 1 or 2

This is the N+1 problem.

---

## 4. Reality

Lazy loading appears in:

- ORM relationships
- image loading
- configuration initialization
- dependency initialization
- expensive computed values
- cache population
- detail pages loaded after list pages
- pagination and infinite scroll

Common backend reality:
- lazy loading is useful inside a transaction/context
- lazy loading can fail if session is closed
- lazy loading can create N+1 queries
- eager loading can load too much data

Performance maturity:
- choose lazy, eager, or batch fetch based on access pattern

---

## 5. How It Works

Lazy flow:

1. Object starts with lightweight state.
2. Expensive field is not loaded yet.
3. Caller requests expensive field.
4. Loader fetches or computes it.
5. Value may be cached for later calls.
6. Later access returns cached value.

Decision point:
- if most callers need the data, eager or batch loading may be better
- if few callers need it, lazy loading may save work

Thread-safety point:
- lazy initialization in shared objects must be protected

---

## 6. What Problem It Solves

- Primary problem solved: unnecessary upfront work.
- Secondary benefits: lower initial latency, lower memory use, faster list views.
- Systems impact: reduces load when expensive data is rarely needed.

Lazy loading can hurt when:
- hidden calls happen inside loops
- each lazy access performs network or DB I/O
- errors happen far from the original request boundary
- transaction/session is already closed

---

## 7. When to Rely on It

Use lazy loading when:

- data is expensive
- not every request needs it
- initial response should be fast
- object can expose lightweight summary first
- data can be loaded safely later

Interviewer keywords:
- defer loading
- expensive details
- list vs detail page
- lazy initialization
- avoid upfront cost
- ORM relationship

---

## 8. When Not to Use It

Avoid lazy loading when:

- almost every caller needs the data
- lazy access occurs in a loop and causes N+1 queries
- transaction/session may be closed
- hidden latency surprises callers
- failure handling needs to happen upfront
- loading must be consistent with original snapshot

Use eager loading when data is always needed.

Use batch loading when many related items are needed together.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces upfront work | Can hide latency later |
| Saves memory when data unused | Can create N+1 queries |
| Improves initial response time | Needs lifecycle/session awareness |
| Pairs well with caching | Thread-safe lazy init needs care |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: faster initial path and less unused work.
- Give up: predictable timing of load.
- Latency impact: initial call faster, later call may be slower.
- Complexity impact: loader lifecycle and caching must be designed.

### Common Mistakes

- Mistake: lazy loading inside a loop.
- Why it is wrong: creates N+1 query pattern.
- Better approach: batch fetch related data.

- Mistake: lazy field without synchronization in shared object.
- Why it is wrong: multiple threads may initialize twice or see partial state.
- Better approach: use safe lazy initialization or immutable snapshots.

- Mistake: hiding remote calls behind simple getters.
- Why it is wrong: callers do not expect I/O from a getter.
- Better approach: make expensive load explicit or document it clearly.

---

## 11. Key Numbers

Lazy loading heuristics:

- If less than a minority of callers need heavy details, lazy can help.
- If most callers need details, eager or batch loading is often better.
- N+1 query count: 1 list query plus N detail queries.
- Hidden getter I/O is a readability and latency smell.
- Batch size should balance fewer calls against memory and payload size.

Memory number:
- Lazy loading saves unused work, but batching saves repeated work.

---

## 12. Failure Modes

- N+1 queries.
- Lazy initialization race.
- Session closed error in ORM.
- Hidden remote latency inside getter.
- Stale lazy cache value.
- Too much eager fallback after lazy problems.

Mitigations:
- query count tests
- batch fetch
- explicit loaders
- safe lazy initialization
- request-scoped data access boundaries
- tracing DB calls per request

---

## 13. Scenario

- Product / system: Hotel search and hotel details
- Requirement: search page needs summary fields, details page needs reviews and amenities
- Good design: load summary eagerly for search; load details lazily or through explicit detail endpoint; batch when rendering many details
- Why this concept fits: heavy details are not needed for every search result
- What would go wrong without it: eager loading wastes work, while naive lazy loading creates N+1 queries

---

## 14. Java Code Sample

### Explicit lazy loader with cached details

```java
import java.util.List;
import java.util.function.Supplier;

record HotelSummary(String hotelId, String name, int nightlyRate) {
}

record HotelDetails(List<String> amenities, List<String> reviews) {
}

class HotelView {
    private final HotelSummary summary;
    private final Supplier<HotelDetails> detailsLoader;
    private HotelDetails cachedDetails;

    HotelView(HotelSummary summary, Supplier<HotelDetails> detailsLoader) {
        this.summary = summary;
        this.detailsLoader = detailsLoader;
    }

    public HotelSummary summary() {
        return summary;
    }

    public synchronized HotelDetails details() {
        if (cachedDetails == null) {
            // Performance concept: expensive details are loaded only if the caller asks for them.
            cachedDetails = detailsLoader.get();
        }
        return cachedDetails;
    }
}
```

Key idea:
- lazy loading should be explicit enough that callers understand when expensive data is fetched

---

## 15. Python Mini Program / Simulation

This mini program shows lazy loading and then demonstrates why repeated lazy loads need batching.

```python
from dataclasses import dataclass
from functools import cached_property


@dataclass(frozen=True)
class HotelSummary:
    hotel_id: str
    name: str


class HotelView:
    def __init__(self, summary: HotelSummary) -> None:
        self.summary = summary

    @cached_property
    def reviews(self) -> list[str]:
        # Performance concept: reviews are loaded only on first access.
        print(f"loading reviews for {self.summary.hotel_id}")
        return ["clean", "great location"]


def main() -> None:
    hotels = [HotelView(HotelSummary(f"hotel-{index}", "Hotel")) for index in range(3)]
    print("search rendered with summaries only")

    for hotel in hotels:
        # Performance concept: this loop can become N+1 if each access performs a database query.
        print(hotel.summary.hotel_id, hotel.reviews)


if __name__ == "__main__":
    main()
```

What this demonstrates:
- lazy loading saves work until data is needed
- cached property avoids repeated loads for one object
- looping over lazy properties can still create N+1 behavior

---

## 16. Practical Question

> Hotel search returns 50 hotels, but reviews are needed only on details page. Would you lazy load reviews?

---

## 17. Strong Answer

For the search page, I would not load reviews eagerly if they are not displayed. I would return summary data only. For the details page, I would load reviews explicitly through a details endpoint or loader.

If a page needs reviews for many hotels at once, I would avoid naive lazy loading because it may create N+1 queries. I would batch fetch reviews by hotel ids.

So the choice is not lazy everywhere or eager everywhere. It depends on access pattern: summary for list, explicit load for detail, batch fetch for many related items.

---

## 18. Revision Notes

- One-line summary: Lazy loading defers expensive work until needed, but can create hidden latency and N+1 queries.
- Three keywords: defer, explicit, N+1
- One interview trap: using lazy loading inside loops without query-count awareness.
- One memory trick: lazy saves unused work; batch saves repeated work.

---

# Topic 6: Batching

> Track: 2.5 Performance Engineering
> Scope: reducing round trips, batch APIs, database batching, bulk writes, latency-throughput trade-offs, batch sizing, flush policy, and N+1 elimination

---

## 1. Intuition

Think of hotel housekeeping delivering towels.

If staff walk to storage separately for every room, they waste time. If they carry towels for 20 rooms in one trip, they reduce round trips. But if they wait all day to create one giant batch, guests wait too long.

Batching groups work to reduce overhead.

Short memory trick:
- fewer calls
- more work per call
- watch waiting time

---

## 2. Definition

- Definition: Batching is the practice of grouping multiple operations into one larger operation to reduce per-operation overhead.
- Category: Performance optimization and throughput pattern
- Core idea: Amortize fixed costs such as network round trips, database calls, serialization, and transaction overhead.

Batching examples:
- batch database inserts
- fetch many records by ids
- send notification batch
- bulk API request
- batch cache get
- flush metrics periodically
- write logs in chunks

---

## 3. Why It Exists

Many systems pay fixed overhead per call.

Example:
- one database query has network overhead
- 100 individual queries pay that overhead 100 times
- one query with 100 ids pays it once

Batching exists because per-call overhead dominates many small operations.

But batching has a trade-off:
- bigger batches improve throughput
- bigger batches may increase waiting time, memory use, and failure blast radius

---

## 4. Reality

Batching appears in:

- database reads and writes
- ORM batch fetching
- Kafka producer batching
- metrics flushes
- log appenders
- notification delivery
- search indexing
- payment reconciliation
- API bulk endpoints
- cache multi-get

Classic performance issue:
- N+1 query problem

Fix:
- collect ids
- fetch related rows in one query
- map results back to owners

---

## 5. How It Works

Batching flow:

1. Collect multiple work items.
2. Stop collecting when size limit or time limit is reached.
3. Send one batch operation.
4. Process response.
5. Map item-level results back to callers if needed.
6. Retry failures carefully.

Batch boundaries:
- max batch size
- max wait time
- max payload size
- transaction size
- downstream rate limits

---

## 6. What Problem It Solves

- Primary problem solved: excessive per-operation overhead.
- Secondary benefits: higher throughput, fewer network round trips, fewer DB queries, better CPU efficiency.
- Systems impact: improves scalability when many small operations dominate latency or load.

Batching is especially powerful when:
- per-call overhead is high
- operations are independent
- ordering requirements are manageable
- downstream supports bulk operations

---

## 7. When to Rely on It

Use batching when:

- many similar operations happen together
- N+1 queries appear
- remote call overhead dominates
- database insert/update volume is high
- downstream has bulk API
- throughput matters more than immediate per-item completion

Interviewer keywords:
- N+1
- bulk insert
- batch API
- round trip
- throughput
- amortize overhead
- flush interval

---

## 8. When Not to Use It

Avoid batching when:

- each item needs immediate response
- batch waits violate latency SLO
- batch failure handling is too complex
- payload becomes too large
- ordering is strict and batching breaks it
- downstream rejects large batches

Use single operation when latency is more important than throughput.

Use micro-batches when both latency and throughput matter.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces round trips | Adds waiting time |
| Improves throughput | Batch failure handling is harder |
| Fixes N+1 query patterns | Large batches use more memory |
| Amortizes fixed overhead | One bad item can affect the batch |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: fewer calls and better throughput.
- Give up: immediate per-item execution.
- Latency impact: batching may add queue/wait time.
- Memory impact: batch holds items until flush.
- Failure impact: partial failure handling becomes important.

### Common Mistakes

- Mistake: making batch size unlimited.
- Why it is wrong: memory and payload size can explode.
- Better approach: cap by item count, bytes, and time.

- Mistake: batching without item-level failure handling.
- Why it is wrong: one failure can lose or duplicate many items.
- Better approach: track per-item status when needed.

- Mistake: replacing N+1 with one huge query without indexes.
- Why it is wrong: big query can still be slow.
- Better approach: batch with proper indexes and query plans.

---

## 11. Key Numbers

Batching heuristics:

- Batch by size and time, not size alone.
- Common micro-batch windows are often milliseconds to seconds depending on workload.
- Max payload size matters for APIs and brokers.
- Database `IN` lists should be bounded and measured.
- Batch size should respect downstream capacity and timeout limits.

Memory number:
- Batching trades per-item latency for throughput.

---

## 12. Failure Modes

- Batch too large times out.
- One bad item fails entire batch.
- Retry duplicates successful items.
- Batch waits too long and violates latency SLO.
- Memory grows while waiting to flush.
- Downstream bulk endpoint has stricter limits than expected.

Mitigations:
- max batch size
- max wait time
- idempotency keys
- per-item result handling
- dead-letter failed items
- monitor batch size and flush latency

---

## 13. Scenario

- Product / system: Hotel search price enrichment
- Requirement: enrich 50 hotels with ratings and availability without making 100 separate database calls
- Good design: collect hotel ids and fetch related data in batches
- Why this concept fits: many similar lookups happen together
- What would go wrong without it: N+1 queries dominate latency

---

## 14. Java Code Sample

### Fixing N+1 with batch fetch

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

record HotelSummary(String hotelId, String name) {
}

record HotelRating(String hotelId, double rating) {
}

interface RatingRepository {
    Map<String, HotelRating> findRatingsByHotelIds(List<String> hotelIds);
}

class HotelSearchEnricher {
    private final RatingRepository ratingRepository;

    HotelSearchEnricher(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public Map<String, HotelRating> enrich(List<HotelSummary> hotels) {
        List<String> hotelIds = hotels.stream().map(HotelSummary::hotelId).toList();

        // Performance concept: one batch call replaces one query per hotel.
        Map<String, HotelRating> ratingsByHotelId = ratingRepository.findRatingsByHotelIds(hotelIds);

        Map<String, HotelRating> result = new HashMap<>();
        for (HotelSummary hotel : hotels) {
            result.put(hotel.hotelId(), ratingsByHotelId.get(hotel.hotelId()));
        }
        return result;
    }
}
```

Key idea:
- batching is often the cleanest fix for N+1 access patterns

---

## 15. Python Mini Program / Simulation

This mini program compares N+1 style calls with one batch call.

```python
import time


def fetch_rating_one(hotel_id: str) -> float:
    time.sleep(0.01)
    return 4.5


def fetch_ratings_batch(hotel_ids: list[str]) -> dict[str, float]:
    # Performance concept: one fixed round trip handles many ids.
    time.sleep(0.01)
    return {hotel_id: 4.5 for hotel_id in hotel_ids}


def n_plus_one(hotel_ids: list[str]) -> dict[str, float]:
    return {hotel_id: fetch_rating_one(hotel_id) for hotel_id in hotel_ids}


def batched(hotel_ids: list[str]) -> dict[str, float]:
    return fetch_ratings_batch(hotel_ids)


def main() -> None:
    hotel_ids = [f"hotel-{index}" for index in range(20)]

    start = time.perf_counter()
    n_plus_one(hotel_ids)
    print("n+1 seconds", round(time.perf_counter() - start, 3))

    start = time.perf_counter()
    batched(hotel_ids)
    print("batch seconds", round(time.perf_counter() - start, 3))


if __name__ == "__main__":
    main()
```

What this demonstrates:
- fixed per-call overhead dominates N+1 patterns
- batch fetch collapses many round trips into one
- batching improves throughput but must still respect size and timeout limits

---

## 16. Practical Question

> A hotel search endpoint fetches ratings one hotel at a time for 50 hotels. How would you improve it?

---

## 17. Strong Answer

I would identify this as an N+1 query or N+1 remote call pattern. Instead of fetching ratings one hotel at a time, I would collect hotel ids and call a batch repository or bulk API such as `findRatingsByHotelIds`.

Then I would map the returned ratings back to the hotel summaries. I would cap batch size and verify the query uses proper indexes. If the list can be very large, I would split into bounded chunks.

I would measure before and after: query count, total latency, p95, p99, and downstream load.

---

## 18. Revision Notes

- One-line summary: Batching groups similar work to reduce per-operation overhead.
- Three keywords: round trips, N+1, throughput
- One interview trap: using one unlimited batch without size, timeout, or failure policy.
- One memory trick: batch when fixed overhead dominates, but cap the batch.

---

## Final Performance Debugging Playbook

Use this sequence when an interviewer says, "This service is slow" or "This service has memory problems."

1. Name the symptom clearly: latency, CPU, memory, GC, throughput, timeout, or cost.
2. Pick the metric: p95, p99, CPU percent, allocation rate, heap after GC, query count, queue depth.
3. Measure before changing code.
4. Separate CPU time from I/O wait time.
5. Check for obvious N+1 calls and missing batching.
6. Check memory retention if heap grows after traffic stabilizes.
7. Check GC only after looking at allocation and retained live set.
8. Match object lifecycle to cost and usefulness.
9. Use lazy loading only when the access pattern supports it.
10. Make one change and verify with the same metric.

---

## Final Interview Comparison Sheet

| Concept | Best one-line explanation | Confusion to avoid |
|---|---|---|
| Profiling | Measures where time or memory is actually spent | Logging or guessing |
| Memory leaks | Unneeded objects remain reachable and cannot be reclaimed | Normal high memory or bounded cache warmup |
| GC basics | Runtime reclaims unreachable objects, but allocation and retention drive pressure | Tuning GC before understanding allocation |
| Object lifecycle | Match object lifetime to creation cost, ownership, and cleanup needs | Pooling every object |
| Lazy loading | Defer expensive work until needed | Hidden N+1 calls |
| Batching | Group many operations to reduce fixed overhead | Unlimited batch size |

---

## Fast Recall Rules

- Profile before optimizing.
- Optimize hotspots, not code that merely looks inefficient.
- Memory leak means retained, not just allocated.
- GC pressure usually comes from allocation rate, retained live set, or heap sizing.
- Reuse expensive clients and pools; do not reuse mutable request data.
- Lazy loading helps when data is rarely needed.
- Lazy loading hurts when it causes N+1 calls.
- Batching reduces round trips but adds waiting and failure-handling complexity.
- Every cache needs an eviction story.
- Every performance fix needs before-and-after measurement.
