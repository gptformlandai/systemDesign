# Java Profiling, JFR, Async-Profiler, And JVM Runbooks Gold Sheet

Target: senior production debugging. This sheet turns "I know jstack" into an incident-ready profiling workflow.

---

## 1. Intuition

Profiling is evidence collection for the JVM.

```text
Metrics tell you something is wrong.
Logs tell you what the app reported.
Traces tell you where request time went.
Thread dumps tell you what threads are doing now.
Heap dumps tell you what keeps memory alive.
JFR tells you runtime events over time.
async-profiler tells you where CPU/allocation/lock time is spent.
```

Beginner line:

```text
Do not guess. Pick the diagnostic tool based on the symptom.
```

---

## 2. Definition

- Definition: Java profiling is the process of measuring JVM and application behavior using runtime evidence.
- Category: Production diagnostics and performance engineering.
- Core idea: map symptoms to evidence, collect safely, form hypotheses, validate, and then change code or config.

---

## 3. Why It Exists

Performance bugs often hide behind vague symptoms:

- "API is slow."
- "CPU is high."
- "Memory keeps growing."
- "Threads are stuck."
- "GC pauses spike."

Naive fixes are dangerous:

- Increasing heap without understanding retention.
- Increasing thread pool without checking downstream bottlenecks.
- Switching GC without reading GC evidence.
- Rewriting code based on one log line.

Profiling exists because production Java systems are complex enough that intuition alone is not reliable.

---

## 4. Reality

Senior Java engineers use a tool ladder:

1. Metrics and alerts.
2. Logs and recent deploy/config changes.
3. Distributed traces.
4. Thread dumps or `jcmd Thread.print`.
5. JFR recording.
6. Heap dump when memory retention is suspected.
7. async-profiler or equivalent for flamegraphs.
8. Reproduce locally only after production evidence narrows the space.

Real incident line:

```text
I start with low-risk evidence first. I avoid heap dumps on a critical large process unless
I understand the pause and disk impact.
```

---

## 5. How It Works

### Symptom-To-Tool Map

| Symptom | First Evidence | Deeper Evidence |
|---|---|---|
| High CPU | process CPU, per-thread CPU | thread dumps, JFR, async-profiler CPU flamegraph |
| Memory leak | heap trend, GC logs | heap dump, JFR allocation profile |
| GC pauses | GC logs, JFR GC events | allocation rate, object lifetime, heap sizing |
| Deadlock | thread dump | lock ownership and code path |
| Pool starvation | metrics, thread dump | traces, downstream latency, queue depth |
| Startup slow | startup logs, JFR | class loading, bean init, remote calls |
| Native memory growth | RSS vs heap | NMT, direct buffers, JNI/native libs |

### High CPU Flow

1. Confirm process CPU.
2. Find hot OS thread.
3. Convert thread id to hex nid.
4. Take repeated thread dumps.
5. Confirm with JFR or async-profiler.
6. Fix hot loop, lock contention, serialization, regex, crypto, logging, or excessive allocation.

### Memory Leak Flow

1. Confirm heap grows after GC.
2. Check GC logs and old-gen occupancy.
3. Capture heap dump if safe.
4. Analyze retained size and GC root paths.
5. Fix cache, listener, `ThreadLocal`, classloader, static map, or queue retention.

### Recovery Path

1. Mitigate user impact first: rollback, shed load, disable feature, lower concurrency.
2. Collect evidence.
3. Change the smallest thing that addresses the proven cause.
4. Add a test, metric, alert, or runbook entry.

---

## 6. What Problem It Solves

- Primary problem solved: evidence-driven diagnosis of JVM performance and reliability issues.
- Secondary benefits: faster incidents, fewer blind config changes, better interview communication.
- Systems impact: reduces latency, outages, and repeated incidents.

---

## 7. When To Rely On It

Use these runbooks when:

- p95/p99 latency spikes.
- CPU is saturated.
- heap grows continuously.
- service has frequent Full GC or long pauses.
- request threads are blocked.
- virtual threads pin carrier threads.
- container RSS exceeds heap expectations.
- after a deploy, runtime behavior changes.

Interviewer keywords:

- JFR
- thread dump
- heap dump
- flamegraph
- high CPU
- GC spike
- memory leak
- native memory
- async-profiler

---

## 8. When Not To Use Heavy Profiling

Avoid heavy diagnostics when:

- The incident needs immediate rollback and the culprit deploy is obvious.
- A heap dump would fill disk or freeze a critical process.
- You have no approval to attach tooling in a regulated environment.
- A lower-risk metric or JFR recording is enough.

Better approach:

- Start with metrics/logs/traces.
- Prefer JFR for low-overhead runtime events.
- Use heap dumps and external profilers intentionally.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Converts guesses into evidence | Tools require practice |
| Reveals CPU, allocation, locks, GC, IO | Some diagnostics add overhead |
| Strong incident communication | Production access may be restricted |
| Supports before/after validation | Heap dumps can expose sensitive data |
| Improves interview depth | Flamegraphs need interpretation skill |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- JFR has low overhead and rich JVM events, but less precise native stack profiling than specialized profilers.
- async-profiler gives excellent flamegraphs, but attaching it in production requires operational approval.
- Heap dump shows retained memory, but can be large and sensitive.
- Thread dump is safe and quick, but one sample can mislead.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Taking one thread dump only | Thread state is momentary | Take several samples |
| Looking at shallow heap size | Retention matters more | Use retained size and GC roots |
| Tuning GC first | GC may be symptom | Find allocation or retention cause |
| Ignoring container memory | RSS can exceed heap | Check metaspace, direct buffers, stacks, native |
| Profiling without timeline | No incident context | Check deploys, traffic, dependency changes |

---

## 11. Key Numbers

| Item | Practical Guidance |
|---|---|
| Thread dump samples | 3-5 samples, a few seconds apart |
| JFR short profile | 30-120 seconds often enough |
| Heap dump size | Can approach live heap size or more on disk |
| GC pause analysis | Compare p99 latency against pause timestamps |
| CPU diagnosis | Per-thread CPU matters more than process total alone |
| Container memory | heap + metaspace + direct + stacks + native + JVM overhead |

---

## 12. Failure Modes

| Failure | User Observes | Evidence | Mitigation |
|---|---|---|---|
| Hot loop | High CPU, slow API | hot stack repeated | fix loop/algorithm |
| Lock contention | high latency, BLOCKED threads | monitor/lock events | reduce lock scope |
| Memory leak | growing heap, OOM | heap dump retained graph | remove retention |
| Allocation storm | frequent GC | JFR allocation events | reduce object churn |
| Pool starvation | requests wait | thread dump and pool metrics | timeout, bulkhead, tune queries |
| Native leak | RSS grows but heap stable | NMT, direct buffer evidence | release native/direct memory |
| Virtual-thread pinning | carrier saturation | JFR virtual thread events | avoid pinned blocking sections |

---

## 13. Scenario

- Product / system: Java payment service.
- Why this concept fits: p99 latency spikes after deploy, but CPU and heap both changed.
- What would go wrong without it: team blindly increases heap and misses a new JSON serialization loop that burns CPU.

---

## 14. Code Sample

High CPU demo:

```java
public class HighCpuDemo {
    public static void main(String[] args) {
        long count = 0;
        while (true) {
            count += expensiveHash("booking-" + count);
        }
    }

    private static int expensiveHash(String value) {
        int result = 1;
        for (int i = 0; i < 10_000; i++) {
            result = 31 * result + value.hashCode();
        }
        return result;
    }
}
```

Commands:

```bash
javac HighCpuDemo.java
java HighCpuDemo
jcmd <pid> Thread.print
jcmd <pid> JFR.start name=cpu duration=60s filename=/tmp/high-cpu.jfr
```

What to explain:

- The hot stack should repeatedly point to `expensiveHash`.
- One thread dump is a clue; repeated samples or JFR gives confidence.

---

## 15. Mini Program / Simulation

Memory retention demo:

```java
import java.util.ArrayList;
import java.util.List;

public class RetainedMemoryDemo {
    private static final List<byte[]> CACHE = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        while (true) {
            CACHE.add(new byte[1024 * 1024]);
            System.out.println("cached MB=" + CACHE.size());
            Thread.sleep(250);
        }
    }
}
```

Run:

```bash
javac RetainedMemoryDemo.java
java -Xmx128m -XX:+HeapDumpOnOutOfMemoryError RetainedMemoryDemo
```

Debrief:

1. Why can Java leak memory despite GC?
2. Why is `CACHE` a GC root path?
3. What would a heap dump show?
4. How would this differ from an allocation spike with no retention?

---

## 16. Practical Question

> A Java service has high CPU after a deploy. Logs show nothing useful. How do you debug it in production without guessing?

---

## 17. Strong Answer

I would first confirm the affected process and whether CPU is user CPU, system CPU, or container throttling. Then I would inspect per-thread CPU and map the hot OS thread to a Java `nid` in a thread dump. I would take multiple thread dump samples and start a short JFR recording. If approved, I would collect an async-profiler CPU flamegraph. I would compare the hot methods with the recent deploy and traffic pattern. The fix depends on evidence: hot loop, regex, JSON serialization, logging, lock contention, crypto, or allocation pressure. After the fix, I would compare CPU, p95/p99, allocation rate, and error rate before and after rollout.

---

## 18. Revision Notes

- One-line summary: production profiling is symptom-driven evidence collection, not random JVM flag tuning.
- Three keywords: JFR, thread dump, flamegraph.
- One interview trap: changing GC before proving GC is the cause.
- One memory trick: metrics locate the fire, dumps show the room, JFR shows the movie.
