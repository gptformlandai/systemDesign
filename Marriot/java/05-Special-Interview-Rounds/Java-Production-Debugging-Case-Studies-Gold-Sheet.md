# Java Production Debugging Case Studies Gold Sheet

Target: senior Java interviews and production incident discussions.

This sheet covers practical Java debugging case studies:

- High CPU
- Memory leak
- Deadlock
- Thread pool exhaustion
- GC pause spike
- DB connection pool starvation
- `NoClassDefFoundError`
- Native thread exhaustion
- Direct memory OOM
- Latency spike
- Logging/tracing gaps
- Virtual thread migration surprises

---

## 1. Debugging Mindset

Do not guess.

Use evidence:

```text
metrics -> logs -> traces -> thread dumps -> heap dumps -> GC logs/JFR -> code/config
```

Strong interview line:

```text
In production debugging, I first identify what changed, what resource is saturated, and
where time or memory is being spent. Then I use JVM tools to confirm the root cause.
```

---

## 2. Java Debugging Tools Cheat Sheet

| Tool | Use |
|---|---|
| `jcmd` | General JVM diagnostics |
| `jstack` | Thread dump |
| `jmap` | Heap dump / memory |
| `jstat` | GC and class stats |
| JFR | Low-overhead profiling |
| JMC | Analyze JFR |
| GC logs | GC frequency and pause analysis |
| Heap dump analyzer | Retained memory analysis |
| APM/tracing | Request path latency |
| Logs | Error context and event timeline |

Favorite command:

```text
jcmd <pid> help
```

Why:

```text
It shows available diagnostic commands for that JVM.
```

---

## 3. Case Study: High CPU

Symptom:

```text
Java service CPU stays above 90%.
Latency increases.
No obvious error logs.
```

Likely causes:

- Infinite loop.
- Hot regex.
- Serialization/deserialization.
- Excessive logging.
- Busy spin.
- Lock contention.
- GC pressure.
- Compression/encryption.
- Large stream processing.

Debug flow:

```text
1. Find process PID.
2. Find high-CPU thread.
3. Convert native thread ID to hex.
4. Take thread dump.
5. Match nid.
6. Repeat samples or use JFR.
```

Commands:

```text
top -H -p <pid>
printf "%x\n" <thread-id>
jstack <pid>
jcmd <pid> JFR.start name=cpu duration=60s filename=/tmp/cpu.jfr
```

Strong answer:

```text
For high CPU, I map the hot OS thread to a Java stack using thread dump nid, take repeated
samples, and confirm with JFR. I want to know whether CPU is spent in application logic,
GC, serialization, regex, locking, or native code.
```

---

## 4. Case Study: Memory Leak

Symptom:

```text
Heap usage climbs over time.
GC runs but memory does not return to baseline.
Eventually OutOfMemoryError.
```

Likely causes:

- Static map/list grows forever.
- Unbounded cache.
- ThreadLocal not removed.
- Queue backlog.
- Listener/subscriber not unregistered.
- Classloader leak.
- Large session/request objects retained.

Debug flow:

```text
1. Check heap trend.
2. Check GC logs/JFR.
3. Take heap dump.
4. Analyze dominator tree.
5. Inspect retained size.
6. Follow GC root path.
```

Commands:

```text
jcmd <pid> GC.heap_dump /tmp/app.hprof
jmap -dump:format=b,file=/tmp/app.hprof <pid>
```

Strong answer:

```text
Java memory leaks are unused objects that remain reachable. I use heap dumps to find large
retained objects and GC root paths, then fix the retaining structure such as a static map,
unbounded cache, ThreadLocal, listener list, or queue.
```

---

## 5. Case Study: Deadlock

Symptom:

```text
Some requests hang forever.
CPU may be low.
Thread count is stable.
```

Likely causes:

- Locks acquired in inconsistent order.
- Nested synchronized blocks.
- Waiting while holding lock.
- Cross-service callback while holding lock.

Debug flow:

```text
1. Take thread dump.
2. Search for deadlock section.
3. Inspect BLOCKED threads.
4. Identify lock owners and waiters.
5. Fix lock ordering or reduce lock scope.
```

Command:

```text
jcmd <pid> Thread.print
```

Strong answer:

```text
For deadlock, I use thread dumps to find threads waiting on each other's locks. The usual
fix is consistent lock ordering, smaller critical sections, tryLock with timeout, or
redesigning the shared-state flow.
```

---

## 6. Case Study: Thread Pool Exhaustion

Symptom:

```text
Executor active threads maxed.
Queue keeps growing.
Latency increases.
No CPU saturation.
```

Likely causes:

- Blocking DB/API calls.
- Missing timeouts.
- Downstream slowness.
- Unbounded queue.
- Too few threads.
- Tasks waiting on each other.

Metrics to check:

- Active thread count.
- Queue size.
- Task completion rate.
- Rejection count.
- Downstream latency.
- Timeout count.

Strong answer:

```text
Thread pool exhaustion means tasks arrive faster than they complete or tasks are blocked.
I check queue size, active count, task duration, and downstream waits. I prefer bounded
queues, clear rejection policy, timeouts, and bulkheads over unbounded queue growth.
```

Fixes:

- Add timeouts.
- Bound queue.
- Use CallerRunsPolicy or explicit rejection handling.
- Separate CPU and IO pools.
- Limit downstream concurrency.
- Consider virtual threads for blocking IO if on Java 21+.

---

## 7. Case Study: GC Pause Spike

Symptom:

```text
p99 latency spikes every few minutes.
GC logs show long pauses or Full GC.
```

Likely causes:

- High allocation rate.
- Old generation pressure.
- Memory leak.
- Humongous objects.
- Large heap with wrong collector/tuning.
- Too many temporary objects.

Debug flow:

```text
1. Enable/check GC logs.
2. Check pause type and duration.
3. Check allocation rate.
4. Check old-gen trend.
5. Use JFR allocation profiling.
6. Use heap dump if retention suspected.
```

Strong answer:

```text
For GC pauses, I first determine whether the problem is allocation pressure or object
retention. Then I reduce allocation, fix leaks, tune heap/collector, or choose a low-latency
collector based on evidence.
```

---

## 8. Case Study: DB Connection Pool Starvation

Symptom:

```text
Request latency high.
Thread dumps show many threads waiting for DB connection.
Database CPU may be normal or high.
```

Likely causes:

- Pool too small.
- Queries too slow.
- Connections leaked.
- Transactions too long.
- Too much concurrency.
- Missing query timeout.

Debug:

- Pool active/idle/wait metrics.
- Connection acquisition time.
- Slow query logs.
- Transaction duration.
- Thread dump stacks.

Strong answer:

```text
If threads wait for DB connections, increasing application threads can make it worse.
I check pool wait time, query latency, transaction duration, and connection leaks. The fix
may be query optimization, shorter transactions, leak detection, pool tuning, or concurrency limits.
```

---

## 9. Case Study: `NoClassDefFoundError`

Symptom:

```text
Application starts locally but fails in production with NoClassDefFoundError.
```

Likely causes:

- Missing runtime dependency.
- Dependency scope wrong.
- Version conflict.
- Shaded JAR issue.
- Class initialization failed earlier.
- Container image missing artifact.

Debug flow:

```text
1. Identify missing class.
2. Find which dependency should contain it.
3. Compare compile-time vs runtime classpath.
4. Inspect dependency tree.
5. Check packaging/container contents.
6. Look for earlier ExceptionInInitializerError.
```

Commands:

```text
mvn dependency:tree
./gradlew dependencies
jar tf app.jar
```

Strong answer:

```text
NoClassDefFoundError is a runtime class loading/linkage issue. I compare runtime artifacts
with expected dependencies, inspect dependency tree and packaging, and check whether class
initialization failed before the error.
```

---

## 10. Case Study: Native Thread Exhaustion

Symptom:

```text
OutOfMemoryError: unable to create native thread
Heap is not full.
```

Likely causes:

- Too many platform threads.
- Thread leak.
- Unbounded cached thread pool.
- Large thread stack size.
- OS process/user thread limit.

Debug:

```text
jcmd <pid> Thread.print
ps -eLf | grep <pid>
ulimit -u
```

Strong answer:

```text
This OOM is not necessarily heap. The JVM could not create another native thread. I check
thread count, executor creation, cached pools, blocked threads, and OS limits.
```

---

## 11. Case Study: Direct Buffer Memory OOM

Symptom:

```text
OutOfMemoryError: Direct buffer memory
Heap may look healthy.
```

Likely causes:

- Direct ByteBuffer allocation.
- Netty/NIO buffers.
- File/channel heavy workloads.
- Off-heap leak.
- Direct memory limit too low.

Debug:

- Native memory tracking.
- JFR.
- Buffer pool metrics.
- Framework metrics.

Command:

```text
jcmd <pid> VM.native_memory summary
```

Strong answer:

```text
Direct buffer OOM is off-heap memory pressure. I check direct buffer usage, NIO/network
libraries, native memory tracking, and whether buffers are retained or direct memory limit
is too low.
```

---

## 12. Case Study: Latency Spike Without CPU Spike

Symptom:

```text
p99 latency high.
CPU normal.
Heap normal.
```

Likely causes:

- DB wait.
- Remote API latency.
- Lock contention.
- Thread pool queueing.
- Connection pool wait.
- DNS/TLS/network issue.
- GC pause not visible in CPU.

Debug:

```text
1. Check traces by span.
2. Check thread dump during spike.
3. Check pool wait metrics.
4. Check downstream p95/p99.
5. Check GC pause logs.
```

Strong answer:

```text
If CPU is normal, I look for waiting: DB pool, remote API, locks, thread queues, network,
or GC pauses. Tracing and thread dumps help identify where requests are stuck.
```

---

## 13. Case Study: Virtual Threads Did Not Help

Symptom:

```text
Service moved to virtual threads but throughput did not improve.
```

Likely causes:

- CPU-bound workload.
- DB pool bottleneck.
- Remote API bottleneck.
- Synchronized pinning.
- Blocking inside monitor.
- Downstream rate limit.
- Memory pressure from too many in-flight requests.

Debug:

- DB pool wait.
- Carrier thread pinning.
- Thread dumps.
- JFR virtual thread events.
- Downstream latency.
- In-flight request count.

Strong answer:

```text
Virtual threads remove platform-thread scarcity, not downstream scarcity. If DB pool,
remote API, CPU, locks, or pinning are the bottleneck, virtual threads alone will not help.
I would measure those before and after migration.
```

---

## 14. Case Study: Logs Are Useless

Symptom:

```text
Production issue happened but logs cannot explain which request failed.
```

Likely causes:

- Missing correlation ID.
- Missing entity IDs.
- Error swallowed.
- No stack trace.
- Logs too generic.
- Sensitive data concerns caused over-redaction.

Better log:

```java
log.error(
    "Payment failed orderId={} userId={} gateway={} status={}",
    orderId,
    userId,
    gatewayName,
    status,
    exception
);
```

Strong answer:

```text
Good logs include safe context, correlation ID, operation, entity ID, and exception cause.
They should not include secrets or raw sensitive data.
```

---

## 15. Case Study: Retry Storm

Symptom:

```text
Downstream service slows down.
Our service retries aggressively.
Downstream becomes worse.
```

Likely causes:

- Immediate retries.
- Too many retry attempts.
- No jitter.
- No circuit breaker.
- No timeout budget.
- Retrying non-idempotent writes.

Strong answer:

```text
Retries must be bounded, delayed with backoff and jitter, and only used when the operation
is safe or idempotent. Otherwise retries can amplify an outage.
```

---

## 16. Case Study: Duplicate Booking Or Payment

Symptom:

```text
Client retries request.
System creates duplicate booking or payment.
```

Likely causes:

- Missing idempotency key.
- No unique constraint.
- Check-then-insert race.
- Transaction boundary wrong.
- Retry applied to non-idempotent operation.

Strong answer:

```text
For retryable writes, I use an idempotency key and persist the request result with a unique
constraint. If the same key appears again, return the existing result instead of repeating
the side effect.
```

---

## 17. Case Study: Slow Startup

Symptom:

```text
Java service takes too long to start.
Autoscaling is slow.
```

Likely causes:

- Heavy classpath scanning.
- Slow dependency initialization.
- DB migrations on startup.
- Remote calls during startup.
- JIT warm-up.
- Large framework context.

Options:

- Lazy initialization where appropriate.
- Remove unnecessary startup work.
- Warm up critical paths.
- Optimize classpath scanning.
- Consider CDS/AppCDS.
- Consider GraalVM Native Image for startup-sensitive workloads.

Strong answer:

```text
For slow startup, I profile startup phases and separate classloading, framework scanning,
dependency initialization, migrations, and remote calls. GraalVM Native Image can help
startup, but it has compatibility and build trade-offs.
```

---

## 18. Incident Response Template

Use this in interviews:

```text
1. Scope: how many users/instances?
2. Symptom: latency, errors, CPU, memory, threads, GC?
3. Timeline: what changed?
4. Evidence: metrics, logs, traces, dumps.
5. Hypothesis: most likely bottleneck.
6. Mitigation: reduce impact.
7. Root cause: verified with data.
8. Prevention: tests, alerts, limits, docs.
```

Strong line:

```text
During an incident, mitigation comes before perfect root-cause analysis.
```

---

## 19. Common Mistakes In Debugging Answers

| Mistake | Better Answer |
|---|---|
| Guessing immediately | Collect metrics/logs/dumps first |
| Only checking app logs | Also check JVM/runtime/system metrics |
| Saying "increase heap" for every OOM | Identify OOM type and root cause |
| Taking one thread dump | Take multiple samples |
| Looking at shallow heap size | Use retained size and GC roots |
| Ignoring connection pools | Pool waits often cause latency |
| Increasing threads blindly | Can worsen downstream overload |
| Retrying everything | Retry only safe/idempotent operations |
| Ignoring recent deploy/config change | Always check timeline |

---

## 20. Final Rapid Revision

| Symptom | First Things To Check |
|---|---|
| High CPU | Hot thread, thread dump, JFR |
| Memory leak | Heap dump, retained size, GC roots |
| Deadlock | Thread dump, BLOCKED, lock owners |
| Thread pool exhaustion | Active threads, queue, downstream latency |
| GC spike | GC logs, allocation, old gen, JFR |
| DB pool wait | Pool metrics, slow queries, leaks |
| Native thread OOM | Thread count, cached pools, OS limits |
| Direct memory OOM | NIO/direct buffers, native memory |
| NoClassDefFoundError | Runtime classpath, dependency tree |
| Duplicate payment | Idempotency key, unique constraint |
| Retry storm | Backoff, jitter, circuit breaker |

---

## 21. Final Interview Answer

If interviewer asks:

> How do you debug production Java issues?

Say:

```text
I start with symptoms and resource signals: latency, errors, CPU, memory, threads, GC, and
downstream metrics. Then I use the right JVM evidence: thread dumps for blocked/high-CPU
threads, heap dumps for memory retention, GC logs and JFR for allocation and pauses, and
traces for request path latency. I mitigate first if users are impacted, then verify root
cause and add prevention such as limits, timeouts, alerts, tests, or safer configuration.
```
