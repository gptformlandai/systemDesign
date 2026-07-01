# X04. Cross-IDE Threads, Async, And Concurrency Triage Playbook

> Bridge module for Java threads, Python threads/asyncio, and Node.js async/event-loop debugging.

---

## 1. Core Idea

Concurrency bugs are usually not "line bugs." They are ordering bugs.

```text
same code + different scheduling = different outcome
```

The debugger must answer:

- What units of execution exist?
- Which one is running?
- Which one is waiting?
- What lock, promise, queue, task, or resource is blocking progress?
- Does pausing change the behavior?

---

## 2. Cross-Language Execution Units

| Runtime | Execution Units | Common Debug View |
|---|---|---|
| Java | platform threads, virtual threads, executor tasks, locks | IntelliJ Threads, jstack |
| Python | OS threads, asyncio tasks, processes | PyCharm threads, debugpy, py-spy |
| Node.js | event loop tasks, promises, callbacks, worker threads, cluster workers | VS Code call stack, Chrome DevTools, inspector |

---

## 3. Symptom To Evidence Map

| Symptom | Likely Cause | First Evidence |
|---|---|---|
| process hangs | deadlock, blocked I/O, event loop stall | thread dump, task dump, profiler |
| CPU high | spin loop, hot function, busy wait | CPU profile, repeated dumps |
| request times out | lock contention, downstream wait, stuck promise | stack traces by request/thread |
| bug disappears under breakpoint | race condition or timing issue | log breakpoints, snapshots |
| memory grows | unbounded queue, retained promises, cache leak | heap snapshot, object retention |
| tasks never finish | missing await, executor starvation, blocked worker | pending tasks and pool state |

---

## 4. Java Triage

Java tools:

```bash
jps -l
jstack <pid>
jcmd <pid> Thread.print
jcmd <pid> GC.heap_info
jcmd <pid> VM.system_properties
```

Java IDE flow:

```text
IntelliJ Debugger -> Threads tab
  check RUNNABLE, BLOCKED, WAITING, TIMED_WAITING
  inspect locks
  suspend only one thread when stepping
  compare frames across worker threads
```

Java concurrency questions:

- Are two threads locking objects in opposite order?
- Is a synchronized block too broad?
- Is a `CompletableFuture` waiting on the same executor it needs to continue?
- Is a thread pool saturated?
- Is a virtual thread pinned by blocking synchronized/native code?
- Is a request context stored in ThreadLocal and lost across async boundary?

Java fix patterns:

- use consistent lock ordering
- reduce lock scope
- use timeouts
- use concurrent collections
- avoid blocking inside common ForkJoinPool
- propagate context explicitly

---

## 5. Python Triage

Python tools:

```bash
py-spy dump --pid <pid>
py-spy top --pid <pid>
python -X dev app.py
PYTHONASYNCIODEBUG=1 python app.py
```

Python IDE flow:

```text
PyCharm / VS Code debugger
  inspect current thread
  inspect active coroutine
  pause process during hang
  check queue size and locks
  check event loop ownership
```

Python concurrency questions:

- Is the bug CPU-bound but implemented with threads?
- Is shared mutable state updated without a lock?
- Is an asyncio coroutine missing `await`?
- Is blocking I/O running inside the event loop?
- Is a thread waiting on a queue that is never filled?
- Is multiprocessing using stale state copied at fork?

Python fix patterns:

- use `threading.Lock` around shared mutable state
- use `asyncio.to_thread` for blocking calls
- use multiprocessing for CPU-bound work
- add timeouts to queue/get/future operations
- avoid global state in workers

---

## 6. Node.js Triage

Node tools:

```bash
node --inspect server.js
node --trace-warnings server.js
node --unhandled-rejections=strict server.js
kill -USR1 <pid>
```

Node IDE flow:

```text
VS Code debugger
  enable async stack traces
  inspect promises and callbacks
  use skipFiles for node internals
  attach to worker threads or cluster workers when needed
```

Node concurrency questions:

- Is the event loop blocked by CPU work?
- Is a promise created but never awaited?
- Is an error swallowed in a `.catch()` or callback?
- Is a worker thread failing silently?
- Is cluster load uneven?
- Is a timer or interval retaining memory?

Node fix patterns:

- move CPU-heavy work to worker threads
- always return/await promises
- centralize unhandled rejection handling
- add cancellation/timeouts
- close timers, sockets, and handles
- profile event-loop delay

---

## 7. When Not To Step

Avoid normal stepping when:

- pausing changes timing
- there are many threads
- the bug happens only under load
- the issue is event-loop delay
- the process is in production-like environment
- deadlock or starvation is suspected

Use instead:

- log breakpoints
- thread dumps
- CPU profiles
- heap snapshots
- repeated samples
- correlation ids
- metrics and traces

---

## 8. Three-Snapshot Method

For hangs and stalls, take three snapshots over time.

```text
t0: capture thread/task/profile snapshot
t1: wait 10-30 seconds
t2: capture again
t3: capture again
```

Interpretation:

| Pattern | Meaning |
|---|---|
| same stack in all snapshots | stuck, blocked, or spinning |
| different stack each time | making progress |
| many workers waiting | idle or downstream bottleneck |
| one hot stack consuming CPU | likely hot loop or CPU-heavy code |
| all tasks waiting on same resource | lock, DB, network, queue, or rate limit |

---

## 9. Strong Interview Answer

```text
For concurrency issues, I avoid immediately stepping line by line because pausing can change timing. I first identify the runtime units: Java threads or virtual threads, Python threads/coroutines/processes, or Node promises/event-loop tasks/workers. Then I capture evidence with thread dumps, task dumps, async stacks, or profiles. I look for stable blocked stacks across multiple snapshots, lock ownership, executor saturation, event-loop blocking, missing awaits, or shared mutable state. Only after I have a theory do I use targeted breakpoints.
```

---

## 10. Revision Notes

- One-line summary: concurrency debugging is scheduling, blocking, and ownership analysis.
- Three keywords: schedule, wait, ownership.
- One trap: using a normal breakpoint on a timing-sensitive race and believing the bug is gone.
- Memory trick: `R-W-O` = Running, Waiting, Owning.
