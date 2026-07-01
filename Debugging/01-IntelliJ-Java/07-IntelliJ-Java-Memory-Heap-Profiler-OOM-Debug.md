# 07. IntelliJ Java: Memory, Heap Profiler, OOM Debug

## Goal

Capture and analyze heap dumps, use IntelliJ's built-in profiler to find memory leaks, understand OOM error types, and tune JVM memory settings.

---

## OOM Error Types

| Error Message | Cause |
|---|---|
| `java.lang.OutOfMemoryError: Java heap space` | Objects filling heap; GC can't reclaim enough |
| `java.lang.OutOfMemoryError: GC overhead limit exceeded` | JVM spending >98% of time in GC with <2% memory freed |
| `java.lang.OutOfMemoryError: Metaspace` | Class metadata fills Metaspace (post Java 8) |
| `java.lang.OutOfMemoryError: Unable to create new native thread` | Too many threads, OS limit reached |
| `java.lang.OutOfMemoryError: Direct buffer memory` | Off-heap (NIO/Netty) direct buffers exhausted |

---

## JVM Memory Flags

```bash
# Heap size.
-Xmx2g          # maximum heap size = 2 GB
-Xms512m        # initial heap size = 512 MB (allocate upfront to avoid resizing)

# Metaspace (class metadata).
-XX:MaxMetaspaceSize=256m

# Dump heap on OOM (must have before OOM happens).
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof

# GC logging (analyze GC frequency and duration).
-Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags

# See memory usage live (for Flight Recorder).
-XX:+UseG1GC
```

---

## Capturing Heap Dumps

### Method 1: IntelliJ Built-in (Running Process In Debug Mode)

```text
Run -> "Capture Memory Snapshot"
OR
Profiler tool window -> Memory -> "Take Snapshot"
-> Saves .hprof file
-> IntelliJ opens Heap Dump viewer automatically
```

### Method 2: jmap (Any Running JVM)

```bash
# Get PID.
jps -l
# Output: 12345  com.example.orders.OrderServiceApplication

# Capture live heap dump.
jmap -dump:format=b,file=/tmp/heap.hprof 12345

# Capture only live objects (smaller file).
jmap -dump:live,format=b,file=/tmp/heap-live.hprof 12345
```

### Method 3: Automatic On OOM

```bash
# Add to JVM startup flags:
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=/tmp/oom-dump.hprof
```

The JVM writes the heap dump at the moment of OOM before crashing.

### Method 4: HTTP Actuator Endpoint

```bash
curl http://localhost:8080/actuator/heapdump -o /tmp/app-heap.hprof
```

---

## IntelliJ Heap Dump Viewer

Open a .hprof file in IntelliJ: File -> Open -> select .hprof

```text
Heap dump view panels:
  ├── Summary     - total heap size, object count, class count
  ├── Classes     - all loaded classes sorted by retained size or instance count
  ├── Instances   - all instances of a selected class
  ├── Dominators  - objects retaining the most memory (leak suspects)
  └── OQL Console - query heap with SQL-like syntax
```

### Classes View

```text
Class                                  Count  Retained Size
byte[]                                 18432  847 MB    <- most heap by far: likely string data or cached blobs
java.lang.String                       61024  12 MB
com.example.orders.Order               90000  8 MB      <- 90k order objects? leak candidate
java.util.HashMap$Node                 45000  3 MB
...
```

### Retained Size vs Shallow Size

```text
Shallow size:   memory the object itself occupies (fields only)
Retained size:  memory freed if this object (and all objects only reachable through it) were collected

A small object with retained size = 500 MB is retaining a large object graph.
That's your leak.
```

### Instances View

```text
Click a class in Classes view.
Instances panel shows each instance.
Click an instance:
  -> Reference field: what is holding a reference to this object?
  -> Outgoing references: what does this object hold?

Walk the reference chain upward to find what is preventing GC.
```

---

## Finding Memory Leaks: Step By Step

```text
1. Take heap dump at low memory usage (baseline).
2. Exercise the feature suspected of leaking.
3. Force GC: in IntelliJ profiler click "Force GC" button.
4. Take another heap dump.
5. Compare: Sort Classes view by "Retained Size" descending.
6. Look for unexpected growth in object count or retained size.
7. Click the leaking class -> Instances -> inspect references.
8. Find what is holding a reference and preventing GC.
```

### Common Leak Patterns

```text
Pattern 1: Static collections growing unbounded.
  private static List<OrderEvent> auditLog = new ArrayList<>();
  Every order adds to this list. List lives forever.

Pattern 2: ThreadLocal not removed.
  ThreadLocal<SomeObject> local = new ThreadLocal<>();
  Thread pools reuse threads. ThreadLocal values from previous requests persist.
  Fix: always call threadLocal.remove() in a finally block.

Pattern 3: Listener not unregistered.
  eventBus.register(this);
  If this listener is never unregistered, it stays in memory.

Pattern 4: Cache without eviction.
  HashMap used as a cache with no size limit or eviction policy.
  Fix: use Caffeine or Guava Cache with maximumSize or expireAfterWrite.

Pattern 5: Inner class holding outer class reference.
  Anonymous listeners or Runnable instances inside a class
  implicitly hold a reference to the enclosing class.
```

---

## IntelliJ Profiler (Memory Tab)

```text
View -> Tool Windows -> Profiler
OR
Run -> Profile (clock icon)

Memory tab:
  - Live heap graph over time
  - Object allocation by class
  - GC events and pauses
  
CPU tab:
  - Flame graph of method call time
  - Hot methods sorted by time spent
```

### OQL Query Examples

```sql
-- Find all Order objects with status null.
SELECT o FROM com.example.orders.Order o WHERE o.status == null

-- Find all strings containing "ERROR".
SELECT s FROM java.lang.String s WHERE s.toString().contains("ERROR")

-- Find all collections with more than 1000 elements.
SELECT c FROM java.util.ArrayList c WHERE c.size() > 1000
```

---

## VisualVM Integration

IntelliJ works alongside VisualVM for richer profiling:

```bash
# Start VisualVM (bundled with JDK).
jvisualvm
# Or download from: https://visualvm.github.io/

# VisualVM connects to any local JVM process.
# Features: CPU/memory profiler, thread dump, heap dump, GC activity, JMX.
```

---

## Interview Sound Bite

Java OOM errors come in distinct types: heap space means objects aren't being GC'd (usually a leak), GC overhead limit means GC is working but getting nowhere (often a leak too), Metaspace means too many classes are loaded (class loader leak pattern). To find a heap leak: add `-XX:+HeapDumpOnOutOfMemoryError`, capture the .hprof file, and open it in IntelliJ. In the heap dump viewer, sort by retained size — the dominant object that shouldn't exist is the leak root. ThreadLocal not cleaned up in thread pools, unbounded static collections, and listener registrations without deregistration are the three most common Java memory leak patterns.
