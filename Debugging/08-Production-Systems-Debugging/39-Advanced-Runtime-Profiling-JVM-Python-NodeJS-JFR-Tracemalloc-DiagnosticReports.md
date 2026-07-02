# 39. Advanced Runtime Profiling: JVM, Python, Node.js

## Goal

Use runtime-specific profilers and diagnostic artifacts to debug production performance and memory issues beyond IDE stepping.

---

## Profiling Decision Map

| Symptom | Java | Python | Node.js |
|---|---|---|---|
| CPU high | JFR, async-profiler | py-spy, cProfile | inspector CPU profile, clinic flame |
| memory grows | heap dump, JFR alloc | tracemalloc, objgraph | heap snapshot, diagnostic report |
| GC pauses | GC logs, JFR | GC less central | V8 GC trace/heap |
| event loop blocked | thread dump/JFR | asyncio debug/py-spy | event loop delay, clinic doctor |
| native hotspot | async-profiler/perf | py-spy native/perf | perf/clinic |

---

## Java: JFR And JMC

Java Flight Recorder (JFR) records low-overhead runtime events.

Start recording:

```bash
jcmd <pid> JFR.start name=prod-debug settings=profile duration=5m filename=/tmp/app.jfr
```

Stop manually:

```bash
jcmd <pid> JFR.stop name=prod-debug filename=/tmp/app.jfr
```

Analyze with Java Mission Control.

Look for:

- method profiling
- allocation hotspots
- lock contention
- thread parking
- socket reads/writes
- GC pauses
- exceptions
- class loading

---

## Java: async-profiler

Use when you need accurate CPU/allocation/lock profiling.

```bash
./profiler.sh -d 30 -e cpu -f /tmp/cpu.html <pid>
./profiler.sh -d 30 -e alloc -f /tmp/alloc.html <pid>
./profiler.sh -d 30 -e lock -f /tmp/lock.html <pid>
```

Interpret flame graph:

```text
width = cost
height = stack depth
wide frame = hotspot
```

Do not chase tall stacks; chase wide stacks.

---

## Java: GC Log Interpretation

Useful questions:

- Are pauses too long?
- Is old generation growing after full GC?
- Is allocation rate too high?
- Is heap too small or leak present?
- Is container memory limit lower than JVM expectation?

Pattern:

```text
frequent full GC + little memory reclaimed = leak or retained objects
young GC very frequent = high allocation churn
long mixed GC = old gen pressure
```

---

## Python: tracemalloc

Enable memory tracing:

```python
import tracemalloc

tracemalloc.start()

# later
snapshot = tracemalloc.take_snapshot()
top = snapshot.statistics("lineno")
for stat in top[:10]:
    print(stat)
```

Compare snapshots:

```python
before = tracemalloc.take_snapshot()
# run suspected code
after = tracemalloc.take_snapshot()
for stat in after.compare_to(before, "lineno")[:10]:
    print(stat)
```

Use for Python allocation growth, not native memory leaks.

---

## Python: cProfile vs py-spy

| Tool | Use |
|---|---|
| cProfile | controlled local/profile run |
| py-spy | live process sampling without code changes |
| memory-profiler | line-by-line memory in local/dev |
| objgraph | object reference growth |
| scalene | CPU/memory profiling by line |

Production preference: sampling tools first, code-instrumenting tools carefully.

---

## Node.js Diagnostic Reports

Generate a report on demand:

```bash
node --report-on-signal server.js
kill -USR2 <pid>
```

Report includes:

- JS stack
- native stack
- heap stats
- resource usage
- libuv handles
- environment
- loaded libraries

Useful for hangs, crashes, and resource leaks.

---

## Node.js Heap Snapshot Workflow

```text
1. Take baseline heap snapshot.
2. Reproduce traffic pattern.
3. Force GC if safe.
4. Take second snapshot.
5. Compare object counts and retained size.
6. Inspect retaining paths.
7. Look for timers, closures, EventEmitter listeners, caches, request objects.
```

Tools:

- Chrome DevTools Memory panel
- `--inspect`
- `heapdump` package for controlled environments

---

## Node.js Event Loop Utilization

Track event loop delay:

```javascript
const { monitorEventLoopDelay } = require('perf_hooks');
const h = monitorEventLoopDelay({ resolution: 20 });
h.enable();

setInterval(() => {
  console.log({
    p99_ms: h.percentile(99) / 1e6,
    mean_ms: h.mean / 1e6
  });
  h.reset();
}, 10000);
```

If event loop delay rises while CPU is high, suspect synchronous CPU work.

---

## Practical Question

> Java service CPU doubled after a deploy. APM says time is inside application code. What do you do?

---

## Strong Answer

I would capture a low-overhead production profile using JFR or async-profiler, scoped to a short window and the affected version. Then I would inspect the CPU flame graph and compare it with a baseline or previous version. The widest new frame identifies the code path consuming CPU.

If CPU is not the only symptom, I would also check allocation rate, GC logs, lock contention, and thread dumps. If a deployment is correlated, rollback may be the first mitigation while profiling confirms the root cause.

---

## Interview Sound Bite

Advanced profiling is runtime evidence, not guessing. For Java use JFR/async-profiler/GC logs, for Python use py-spy/tracemalloc/cProfile, and for Node use inspector profiles, heap snapshots, diagnostic reports, and event-loop delay.
