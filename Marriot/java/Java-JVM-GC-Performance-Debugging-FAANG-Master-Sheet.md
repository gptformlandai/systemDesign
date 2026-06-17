# Java JVM, GC, Performance, And Debugging FAANG Master Sheet

Target: senior Java backend interviews, production debugging rounds, and FAANG-style depth checks.

This sheet covers:
- JVM execution pipeline
- Class loading
- JIT and tiered compilation
- Heap, stack, metaspace
- GC roots and collectors
- G1, ZGC, Shenandoah awareness
- Memory leaks
- Thread dumps and heap dumps
- JFR, JMC, jcmd, jstack, jmap, jstat
- Latency and CPU debugging

---

## 1. Mental Model

Java is not just a language. It is a managed runtime.

Flow:

```text
.java source
    -> javac
.class bytecode
    -> class loading + verification
JVM interpreter
    -> hot methods detected
JIT compiler
    -> optimized machine code
Garbage collector
    -> reclaims unreachable heap objects
```

Strong interview line:

```text
The JVM gives portability, runtime optimization, memory management, and observability tools.
Senior Java debugging means understanding both code behavior and runtime behavior.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| JVM/JRE/JDK | Very high | Entry check |
| Heap vs stack vs metaspace | Very high | Memory clarity |
| Class loading | High | Framework and classloader leak questions |
| JIT | High | Performance maturity |
| GC roots | Very high | Memory leak reasoning |
| Young/old generation | High | GC basics |
| G1 GC | High | Common production default |
| ZGC/Shenandoah | Medium-high | Low-latency awareness |
| Thread dump | Very high | Production debugging |
| Heap dump | Very high | Memory debugging |
| JFR/JMC | High | Modern profiling |
| High CPU debugging | Very high | Real incident skill |
| Latency debugging | Very high | Backend interview depth |

---

## 3. Runtime Memory Areas

| Area | Scope | Stores |
|---|---|---|
| Heap | Shared JVM | Objects and arrays |
| Java stack | Per thread | Stack frames, local variables, references |
| Metaspace | Shared JVM/native memory | Class metadata |
| PC register | Per thread | Current bytecode instruction |
| Native method stack | Per thread | Native method frames |

Example:

```java
public void process() {
    int count = 10;
    User user = new User("u1");
}

record User(String id) {}
```

Memory:

```text
count -> stack
user reference -> stack
User object -> heap
User class metadata -> metaspace
```

Trap:

```text
Objects live on heap conceptually, but the JIT can optimize some allocations through escape
analysis. Do not use that as the beginner mental model unless discussing performance.
```

---

## 4. Class Loading

Class loading steps:

1. Loading
2. Linking
3. Initialization

Linking includes:

- Verification
- Preparation
- Resolution

ClassLoader hierarchy:

```text
Bootstrap ClassLoader
    -> Platform ClassLoader
        -> Application ClassLoader
            -> Custom ClassLoaders
```

Parent delegation:

```text
A classloader first delegates to its parent before trying to load the class itself.
```

Why it matters:

- Frameworks
- App servers
- Plugins
- Hot deployment
- Classloader leaks
- Duplicate class conflicts

Strong answer:

```text
Class loading turns bytecode into JVM class metadata. Parent delegation helps avoid duplicate
core classes and protects Java platform classes from being overridden by application classes.
```

---

## 5. JIT Compiler

The interpreter starts executing bytecode quickly.

The JIT compiler optimizes hot methods into native machine code.

Optimizations:

- Method inlining
- Dead code elimination
- Escape analysis
- Lock elimination
- Loop optimizations
- Devirtualization

Strong answer:

```text
JIT uses runtime profiling to optimize code paths that are actually hot. This is why Java
can start by interpreting bytecode and later run optimized native code for frequently
executed methods.
```

Interview trap:

```text
Java is not simply interpreted. Modern JVMs use both interpretation and JIT compilation.
```

---

## 6. Tiered Compilation

Tiered compilation combines fast startup and optimized peak performance.

Concept:

```text
Start with interpretation or quick compilation, collect profiling data, then compile hot
methods more aggressively.
```

Why it matters:

- Warm-up effects
- Benchmarking traps
- Latency during startup
- JIT compilation overhead

Interview line:

```text
Java performance can change after warm-up because the JIT optimizes hot paths based on
runtime profile data.
```

---

## 7. Escape Analysis

Escape analysis checks whether an object escapes a method/thread.

If it does not escape, JVM may optimize:

- Allocation
- Synchronization
- Scalar replacement

Example:

```java
int sum(int a, int b) {
    Point point = new Point(a, b);
    return point.x() + point.y();
}

record Point(int x, int y) {}
```

The object may be optimized away.

Interview caution:

```text
Write clear code first. Let the JIT optimize. Do not manually micro-optimize without profiling.
```

---

## 8. Garbage Collection Basics

GC reclaims heap objects that are no longer reachable.

An object is alive if reachable from GC roots.

Common GC roots:

- Local variables in active stack frames
- Static fields
- Active threads
- JNI references
- Classloader-related references

Strong answer:

```text
Garbage collection does not collect objects just because we are done with them mentally.
It collects objects that are unreachable from GC roots.
```

---

## 9. Generational GC

Most objects die young.

Memory model:

```text
Young generation
    -> Eden
    -> Survivor spaces
Old generation
    -> Long-lived objects
```

Basic flow:

1. New objects allocated in young generation.
2. Minor GC collects dead young objects.
3. Surviving objects age.
4. Long-lived survivors move to old generation.
5. Major/mixed/full collections handle older regions depending on collector.

Why it works:

```text
Backend applications create many short-lived request objects, DTOs, strings, collections,
and temporary buffers.
```

---

## 10. G1 GC

G1 means Garbage First.

It divides heap into regions and aims for predictable pause times.

Strengths:

- Good general-purpose server collector.
- Handles large heaps better than older collectors.
- Does mixed collections.
- Has pause-time goals.

Interview answer:

```text
G1 is a region-based collector designed for server applications. It tries to collect regions
with the most garbage first and balance throughput with predictable pause goals.
```

Common tuning starting point:

```text
Set realistic heap size, observe GC logs/JFR, then tune. Do not tune GC blindly.
```

---

## 11. ZGC And Shenandoah

Low-latency collectors aim to keep pause times very small, even with large heaps.

| Collector | Awareness |
|---|---|
| ZGC | Low-latency collector with concurrent work |
| Shenandoah | Low-pause collector with concurrent compaction |

Use when:

- Latency is more important than maximum throughput.
- Heap is large.
- Pause times hurt user experience or SLO.

Trade-off:

```text
Low-latency collectors can use more CPU overhead. Choose based on measured latency goals,
heap size, and application behavior.
```

---

## 12. Common Memory Leak Patterns

Java can leak memory when unused objects remain reachable.

Common leaks:

| Leak | Example |
|---|---|
| Static collection | `static Map` grows forever |
| Unbounded cache | No TTL/eviction |
| Listener not removed | Object retained by observer list |
| ThreadLocal not removed | Request data retained in pool thread |
| Classloader leak | Old app classes retained after redeploy |
| Large response buffering | Holding huge byte arrays |
| Bad equals/hashCode | Map grows with duplicate logical keys |

Example:

```java
class BadCache {
    private static final Map<String, byte[]> CACHE = new HashMap<>();

    static void put(String key, byte[] value) {
        CACHE.put(key, value); // no limit, no TTL
    }
}
```

Better:

```text
Use bounded caches with eviction, TTL, size metrics, and ownership rules.
```

---

## 13. OutOfMemoryError Types

Common variants:

| Error | Meaning |
|---|---|
| Java heap space | Heap exhausted |
| GC overhead limit exceeded | Too much time in GC, little memory reclaimed |
| Metaspace | Class metadata/native memory pressure |
| Direct buffer memory | Off-heap direct buffers exhausted |
| Unable to create native thread | OS/JVM cannot create more threads |

Strong answer:

```text
The OOM message matters. Heap OOM, metaspace OOM, direct buffer OOM, and native thread OOM
have different root causes and debugging paths.
```

---

## 14. Thread Dump

A thread dump shows what every thread is doing.

Use it for:

- Deadlocks
- Blocked threads
- High CPU thread identification
- Thread pool starvation
- Hung requests
- Lock contention

Tools:

```text
jstack <pid>
jcmd <pid> Thread.print
kill -3 <pid>   # Unix signal, writes to stdout/log
```

Thread states:

| State | Meaning |
|---|---|
| RUNNABLE | Running or ready, may include native IO |
| BLOCKED | Waiting for monitor lock |
| WAITING | Waiting indefinitely |
| TIMED_WAITING | Sleeping/timed wait |

How to read:

1. Identify many threads in same stack.
2. Look for BLOCKED threads.
3. Look for deadlock section.
4. Match high CPU thread ID to stack.
5. Check executor pools and request threads.

---

## 15. High CPU Debugging

Steps:

1. Find Java process PID.
2. Find hot thread native ID.
3. Convert native thread ID to hex.
4. Take thread dump.
5. Match `nid=0x...`.
6. Inspect stack.
7. Confirm with repeated dumps or profiler.

Commands:

```text
top -H -p <pid>
printf "%x\n" <thread-id>
jstack <pid>
```

Common causes:

- Infinite loop
- Busy spin
- Bad regex
- Excessive JSON serialization
- Hot lock contention
- GC pressure
- CPU-heavy stream/collection processing
- Encryption/compression

Strong answer:

```text
For high CPU, I identify the hot thread, map it to a Java stack, take repeated samples,
and confirm whether CPU is in application code, GC, serialization, locking, or native calls.
```

---

## 16. Heap Dump

A heap dump captures objects in heap.

Use it for:

- Memory leak
- OOM diagnosis
- Large object retention
- Cache growth
- Duplicate strings/objects

Commands:

```text
jcmd <pid> GC.heap_dump /tmp/app.hprof
jmap -dump:format=b,file=/tmp/app.hprof <pid>
```

Analyze with:

- Eclipse MAT
- VisualVM
- JProfiler
- YourKit

What to inspect:

- Dominator tree
- Retained size
- GC roots path
- Large collections
- Classloader retention
- Duplicate strings

Strong answer:

```text
In heap dump analysis, retained size and GC root paths matter more than shallow object size.
I want to know who is keeping the memory alive.
```

---

## 17. GC Logs

GC logs answer:

- How often GC runs.
- Pause duration.
- Heap before/after.
- Promotion behavior.
- Full GC occurrence.
- Allocation pressure.

Modern JVM logging shape:

```text
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

What to watch:

- Frequent young GC
- Full GC
- Long pauses
- Old generation growth
- Humongous allocations
- Promotion failures

Interview line:

```text
I do not tune GC from guesses. I collect GC logs or JFR data, identify the actual pressure,
and then tune heap, allocation behavior, or collector choice.
```

---

## 18. JFR And JMC

JFR means Java Flight Recorder.

JMC means Java Mission Control.

JFR records low-overhead runtime events:

- CPU samples
- Allocation hotspots
- Lock contention
- GC pauses
- Exceptions
- File/socket IO
- Method profiling
- Thread states

Command:

```text
jcmd <pid> JFR.start name=profile duration=60s filename=/tmp/profile.jfr
```

Why it is powerful:

```text
JFR gives production-friendly profiling data without attaching heavy profilers in many cases.
```

Strong answer:

```text
For production Java performance issues, JFR is often my first profiling tool because it
captures CPU, allocation, GC, lock, and IO signals with relatively low overhead.
```

---

## 19. Latency Debugging

Latency can come from many places.

Checklist:

- Application CPU
- GC pauses
- DB query latency
- Connection pool wait
- Remote API latency
- Lock contention
- Thread pool queueing
- Slow serialization
- Large payloads
- DNS/TLS/network issues
- Disk IO

Method:

1. Look at percentiles, not just average.
2. Break down request path.
3. Check pool wait times.
4. Check downstream latency.
5. Check GC pauses.
6. Check CPU and lock contention.
7. Compare normal vs bad traces.

Strong answer:

```text
For latency, I decompose the request path and look at p95/p99. I check whether time is spent
in app CPU, GC, thread pool queue, DB pool wait, downstream calls, or lock contention.
```

---

## 20. JDK Tools Cheat Sheet

| Tool | Use |
|---|---|
| `jps` | List Java processes |
| `jcmd` | General JVM diagnostic command |
| `jstack` | Thread dump |
| `jmap` | Heap dump and memory info |
| `jstat` | GC/class/compiler stats |
| `jfr` | Flight Recorder files |
| `jconsole` | Basic monitoring |
| `jvisualvm` | Visual monitoring/profiling |
| JMC | Java Mission Control for JFR analysis |

Favorite production command:

```text
jcmd <pid> help
```

Why:

```text
It shows available diagnostic commands for that JVM.
```

---

## 21. Production Debugging Playbooks

### High CPU

```text
top -H -> map thread id -> jstack -> repeated samples -> JFR/profile -> fix hot path
```

### Memory Leak

```text
heap usage trend -> GC logs/JFR -> heap dump -> dominator tree -> GC root path -> fix retention
```

### Deadlock

```text
thread dump -> deadlock section/BLOCKED threads -> lock order -> code path -> consistent ordering
```

### Thread Pool Starvation

```text
metrics -> active threads maxed -> queue grows -> thread dump -> blocked downstream -> bulkhead/timeouts/sizing
```

### GC Pause Spike

```text
GC logs/JFR -> pause type -> allocation rate/old gen/humongous -> heap/object/collector tuning
```

---

## 22. Mini Program: Memory Leak Shape

```java
import java.util.*;

public class StaticMapLeakDemo {
    private static final Map<Integer, byte[]> cache = new HashMap<>();

    public static void main(String[] args) {
        for (int i = 0; i < 1_000_000; i++) {
            cache.put(i, new byte[1024]);
        }
    }
}
```

What happens:

```text
The map is static and remains reachable. GC cannot collect the byte arrays because the
static map still references them.
```

Fix direction:

```text
Use bounded cache, eviction, TTL, weak references only if appropriate, and metrics.
```

---

## 23. Common Mistakes

| Mistake | Why Wrong | Better Approach |
|---|---|---|
| Calling `System.gc()` as a fix | Only a request, can hurt latency | Find allocation/retention cause |
| Looking only at average latency | Hides p95/p99 pain | Use percentiles |
| Taking one thread dump only | May be misleading | Take multiple samples |
| Heap dump without retained-size analysis | Shallow size misleads | Use dominator tree and GC roots |
| Tuning GC before profiling | Guesswork | Measure allocation, pause, heap |
| Huge heap as first fix | Can increase pause/cost | Find leak/pressure first |
| Ignoring direct memory | Heap may look fine | Check direct buffers/native memory |
| Ignoring classloader leaks | Redeploy OOM mystery | Inspect classloader retention |

---

## 24. FAANG-Level Question

Question:

> A Java service has p99 latency spikes every few minutes. CPU is normal. What do you check?

Strong answer:

```text
I would first verify whether the spikes align with GC pauses using GC logs or JFR. Then I
would check request traces to see whether time is in DB, connection pool wait, remote APIs,
thread pool queueing, lock contention, or serialization. Since CPU is normal, I would suspect
waiting, GC, IO, locks, or downstream saturation before CPU-bound code. I would compare
p50/p95/p99, take thread dumps during spikes if possible, and inspect JFR events for GC,
monitor blocking, socket IO, and allocation pressure.
```

---

## 25. Rapid Revision

Must-say lines:

```text
The JVM interprets bytecode first and JIT-compiles hot code paths.
```

```text
GC collects unreachable objects, not objects we no longer intend to use.
```

```text
Memory leaks in Java happen when unused objects are still reachable from GC roots.
```

```text
Thread dumps are for blocked/hung/high-CPU/deadlock debugging.
```

```text
Heap dumps are for retained memory debugging.
```

```text
JFR is a strong low-overhead tool for CPU, allocation, lock, GC, and IO profiling.
```

---

## 26. Official Source Notes

Use official sources when refreshing:

- Java tools and API docs: `https://docs.oracle.com/en/java/javase/`
- Java specifications: `https://docs.oracle.com/javase/specs/`
- OpenJDK project: `https://openjdk.org/`
- JDK builds and GA/EA status: `https://jdk.java.net/`

Interview safety line:

```text
I do not tune JVM performance from memory. I collect evidence using metrics, logs, dumps,
and profilers, then choose the smallest change that addresses the measured bottleneck.
```
