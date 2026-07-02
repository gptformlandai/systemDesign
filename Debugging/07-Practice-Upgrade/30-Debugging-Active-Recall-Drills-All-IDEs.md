# 30. Debugging Active Recall Drills — All IDEs

## How To Use This Sheet

Cover the answer column. Read the question. Answer out loud or write it. Then uncover and check. Do this 3 times per question until instant recall.

---

## IntelliJ Java Drills

**Q1**: What shortcut resumes execution in IntelliJ (macOS)?
**A**: F9

**Q2**: What shortcut opens the breakpoint conditions dialog?
**A**: Cmd+Shift+F8

**Q3**: What does a diamond-shaped breakpoint icon in IntelliJ mean?
**A**: Method entry/exit breakpoint

**Q4**: What JDWP flag makes the JVM wait for a debugger before running?
**A**: `suspend=y` in `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005`

**Q5**: What thread state in IntelliJ Threads panel indicates a deadlock?
**A**: BLOCKED — thread is waiting to acquire a synchronized monitor held by another thread

**Q6**: How do you find which thread holds a lock that another thread is BLOCKED on?
**A**: Look at the BLOCKED thread's frame: it says "waiting to lock 0xABCD". Search other thread frames for "locked 0xABCD" — that thread is the holder.

**Q7**: What does "Drop Frame" do in IntelliJ?
**A**: Rewinds execution to the start of the current method (without reversing side effects)

**Q8**: How do you add a field watchpoint in IntelliJ?
**A**: Right-click the field declaration in source → "Add Field Watchpoint"

**Q9**: What actuator endpoint shows the Spring app thread dump?
**A**: `GET /actuator/threaddump`

**Q10**: What OOM error means Metaspace is full?
**A**: `java.lang.OutOfMemoryError: Metaspace` — too many class definitions loaded

---

## VS Code Shortcuts Drills

**Q11**: What key starts or continues debugging in VS Code?
**A**: F5

**Q12**: What key toggles a breakpoint in VS Code?
**A**: F9

**Q13**: What key steps over the current line in VS Code?
**A**: F10

**Q14**: What key steps into a function call in VS Code?
**A**: F11

**Q15**: How do you open the Debug Console in VS Code?
**A**: Cmd+Shift+Y (macOS) / Ctrl+Shift+Y (Windows/Linux)

**Q16**: What does `"restart": true` do in a VS Code attach config?
**A**: Automatically reconnects when the process restarts (useful with nodemon)

**Q17**: What does `skipFiles: ["<node_internals>/**"]` do?
**A**: Prevents VS Code from stepping into Node.js internal modules; Step Into only enters user code

**Q18**: What are the two VS Code launch config `request` modes?
**A**: `"launch"` (VS Code starts the process) and `"attach"` (VS Code connects to a running process)

**Q19**: How do you debug both a Node backend and React frontend simultaneously in VS Code?
**A**: Use a compound configuration in launch.json with two configurations listed under `"compounds"`

**Q20**: What node flag pauses execution at the very first line (before any code runs)?
**A**: `--inspect-brk=9229`

---

## PyCharm Python Drills

**Q21**: What shortcut starts debug in PyCharm (macOS)?
**A**: Ctrl+D

**Q22**: What is the PyCharm shortcut for "Step Into My Code" (skip libraries)?
**A**: Alt+Shift+F7

**Q23**: What does `--noreload` do for Django debug?
**A**: Prevents Django from launching a subprocess file watcher; ensures the debugger controls the single main process

**Q24**: Why must FastAPI avoid `--reload` when debugging?
**A**: `--reload` forks a subprocess not connected to the debugger; breakpoints won't hit

**Q25**: What Python 3.7+ built-in sets a breakpoint compatible with PyCharm?
**A**: `breakpoint()`

**Q26**: What PyCharm command opens Evaluate Expression?
**A**: Alt+F8

**Q27**: What pytest argument lets `print()` and `breakpoint()` work during test?
**A**: `-s` (disables output capture)

**Q28**: How do you debug only one parametrize iteration in pytest?
**A**: Use a conditional breakpoint: `order_id == "ORD-FAILING"` OR add `-k "ORD-FAILING"` to pytest args

**Q29**: What does `PYTHONASYNCIODEBUG=1` do?
**A**: Enables asyncio debug mode: warns on slow callbacks, detects unawaited coroutines

**Q30**: How do you list all running asyncio tasks in PyCharm Evaluate Expression?
**A**: `asyncio.all_tasks()`

---

## Concurrency Drills

**Q31**: What is the difference between BLOCKED and WAITING in a Java thread dump?
**A**: BLOCKED = waiting to acquire a synchronized monitor (locked by another thread); WAITING = voluntarily waiting via wait(), join(), or park()

**Q32**: What jstack section indicates a deadlock was found?
**A**: "Found one Java-level deadlock:" section near the bottom of the dump

**Q33**: What does py-spy do, and why is it useful in production?
**A**: Sampling profiler for Python — attaches to a running process without code changes or restart; shows thread stacks and generates flame graphs

**Q34**: What does `volatile` guarantee in Java? What does it NOT guarantee?
**A**: Guarantees memory visibility (writes immediately visible to all threads). Does NOT guarantee atomicity (counter++ is still not thread-safe with volatile)

**Q35**: In Node.js, when does `process.nextTick` run vs `Promise.then` vs `setImmediate`?
**A**: nextTick runs first (before microtasks), then Promise.then (microtasks), then setImmediate (check phase of next event loop iteration)

**Q36**: What Python counter operation is NOT thread-safe and why?
**A**: `counter += 1` — it compiles to LOAD_GLOBAL, INPLACE_ADD, STORE_GLOBAL; the GIL can switch between these bytecodes

**Q37**: What is a livelock and how does it differ from deadlock?
**A**: Livelock: threads are ACTIVE (RUNNABLE) but make no progress because they keep reacting to each other. Deadlock: threads are BLOCKED (stuck waiting for locks). Livelock shows high CPU; deadlock shows low CPU.

**Q38**: What Java class provides atomic read-modify-write without synchronized?
**A**: `AtomicInteger`, `AtomicLong`, `AtomicReference` — use compare-and-swap (CAS) operations

**Q39**: What `-XX:` flag captures a heap dump automatically on OOM?
**A**: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/oom.hprof`

**Q40**: What is "retained size" vs "shallow size" in a heap dump?
**A**: Shallow size = memory the object itself uses. Retained size = memory freed if this object AND all objects only reachable through it were collected. High retained size = likely leak root.

---

## Production Systems Drills

**Q41**: In production, why is a debugger attach usually the wrong first move?
**A**: It can pause or mutate live traffic and may expose privileged process control. Start with logs, metrics, traces, profiles, dumps, and deployment metadata.

**Q42**: What four fields must appear in production logs for correlation?
**A**: `service`, `env`, `version`, and `correlation_id` or `trace_id`/`span_id`.

**Q43**: What does a wide span in a distributed trace usually mean?
**A**: Most request latency is spent there. It identifies the slow boundary or operation, not always the root cause.

**Q44**: Name three retry-storm protections.
**A**: Exponential backoff, jitter, retry budget, idempotency key, circuit breaker, load shedding.

**Q45**: What Kubernetes command gives the most useful first look at CrashLoopBackOff?
**A**: `kubectl describe pod <pod> -n <namespace>` plus `kubectl logs <pod> --previous`.

**Q46**: What does `OOMKilled` mean in Kubernetes?
**A**: The container exceeded its cgroup memory limit and was killed, often with exit code 137.

**Q47**: What command checks TLS certificate/SNI behavior from the terminal?
**A**: `openssl s_client -connect host:443 -servername host`

**Q48**: What is the difference between connection refused and connection timeout?
**A**: Refused means the host responded but no process/listener accepted the port. Timeout means the connection attempt did not receive a response, often firewall/routing/security group.

**Q49**: What database evidence separates slow query execution from connection pool exhaustion?
**A**: Slow query execution shows long DB execution/EXPLAIN issues. Pool exhaustion shows app pool active=max, pending waiters, acquisition timeouts, while DB may be normal.

**Q50**: What does an N+1 query look like in a trace?
**A**: One request contains many repeated similar DB spans, often one query per row/item.

**Q51**: In Chrome DevTools, which panel diagnoses main-thread freezes?
**A**: Performance panel. Look for long tasks, scripting, layout, paint, and GC.

**Q52**: What browser symptom suggests service worker or cache problems?
**A**: Hard refresh fixes it, only some users see stale UI, or Application panel shows old service worker/cache entries.

**Q53**: What does exit code 139 usually mean?
**A**: SIGSEGV, usually a segmentation fault/native crash.

**Q54**: Which tool shows Linux syscalls for a running process?
**A**: `strace -p <pid>` on Linux; `dtruss -p <pid>` is a rough macOS equivalent.

**Q55**: What is JFR used for?
**A**: Java Flight Recorder captures low-overhead runtime events such as CPU, allocation, locks, socket I/O, exceptions, GC, and thread activity.

**Q56**: What Python tool compares memory allocation snapshots by line?
**A**: `tracemalloc`.

**Q57**: What Node.js artifact captures heap stats, stacks, libuv handles, and environment for debugging?
**A**: Node.js diagnostic report.

**Q58**: Why should consumers be idempotent in queue systems?
**A**: Most queues provide at-least-once delivery, so duplicate messages are normal and must not duplicate side effects.

**Q59**: Why should you not blindly redrive a DLQ?
**A**: If the consumer is still broken, redrive recreates the failure or amplifies duplicate side effects.

**Q60**: Why are heap dumps sensitive artifacts?
**A**: They can contain tokens, passwords, cookies, PII, payloads, and business data from process memory.

---

## Speed Drill: Shortcuts Summary

```text
30-second quiz: say the shortcut before looking.

IntelliJ macOS:
  Resume          = F9
  Step Over       = F8
  Step Into       = F7
  Step Out        = Shift+F8
  Run To Cursor   = Alt+F9
  Evaluate        = Alt+F8
  Toggle BP       = Cmd+F8
  All BPs dialog  = Cmd+Shift+F8

VS Code:
  Start/Continue  = F5
  Stop            = Shift+F5
  Step Over       = F10
  Step Into       = F11
  Step Out        = Shift+F11
  Toggle BP       = F9
  Debug Console   = Cmd+Shift+Y
  Debug View      = Cmd+Shift+D

PyCharm macOS:
  Resume          = F9
  Step Over       = F8
  Step Into       = F7
  Step Into Mine  = Alt+Shift+F7
  Step Out        = Shift+F8
  Run To Cursor   = Alt+F9
  Evaluate        = Alt+F8
  Toggle BP       = Cmd+F8
```
