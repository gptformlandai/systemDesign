# Thread Pool - End-to-End System Design

> Goal: practice one complete machine-coding + LLD classic from problem understanding to thread-safe implementation, shutdown, failure handling, and scale-up thinking.

---

## How To Use This File

- Use this when the interview asks about thread pool, executor service, worker pool, async task execution, or background job runners.
- Start with a fixed-size thread pool, then discuss bounded queues, rejection policies, futures, graceful shutdown, and tuning.
- Keep one idea sharp: a thread pool reuses a fixed or controlled number of worker threads to process tasks from a shared queue.
- In interviews, explain worker lifecycle, task queue, synchronization, shutdown, rejection, and exception isolation.

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

| Layer | Interview signal | Thread pool focus |
|---|---|---|
| Problem understanding | Can define executor contract | submit tasks, workers execute, pool shuts down |
| HLD | Can design worker architecture | task queue, worker threads, executor API, rejection policy |
| LLD | Can model lifecycle safely | `ThreadPool`, `Worker`, `TaskQueue`, `Task`, `Future`, `PoolState` |
| Machine coding | Can implement critical path | submit, worker loop, take task, execute, shutdown |
| Traffic spikes | Can protect process | bounded queue, backpressure, caller-runs/drop/reject |
| Scale | Can tune workload | CPU-bound vs I/O-bound, dynamic sizing, metrics |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Accept tasks for asynchronous execution.
- Maintain a fixed or configurable number of worker threads.
- Workers fetch tasks from a queue and execute them.
- Support graceful shutdown: finish queued tasks, then stop.
- Support immediate shutdown: stop accepting and return/drop queued tasks.
- Support task failure isolation so one bad task does not kill the pool.
- Optionally return a `Future` for task result.

Optional requirements to clarify:

- Fixed-size or dynamic-size pool?
- Bounded or unbounded task queue?
- What should happen when queue is full?
- Do tasks return results?
- Should shutdown wait for completion?
- Do we need task priorities or scheduling delays?

Out of scope unless asked:

- Distributed job scheduler.
- Cron scheduler.
- Work stealing runtime.
- Fork-join parallelism.

## 1.2 Non-Functional Requirements

Correctness:

- Submitted accepted tasks should execute at most once in this in-memory pool.
- Pool must not accept tasks after shutdown.
- Worker threads should exit cleanly.
- Task exceptions should be captured/logged and not break the worker loop.

Thread safety:

- Task queue operations are synchronized.
- Pool state changes are synchronized/atomic.
- Shutdown wakes idle workers.

Performance:

- Reuse threads instead of creating one per task.
- Bound queue to protect memory.
- Avoid holding locks during task execution.

## 1.3 Constraints

- Tasks can be slow, blocking, or throw exceptions.
- Too many threads cause context switching and memory overhead.
- Too few threads underutilize resources.
- Unbounded queues hide overload and cause memory pressure.
- Shutdown can race with submit.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---:|
| Worker threads | 1-1000 depending workload |
| Queue capacity | 100-1M tasks |
| Task duration | microseconds to minutes |
| Submitters | many concurrent threads |
| Ordering | FIFO start order best-effort |

## 1.5 Capacity Math

Back-of-the-envelope:

- CPU-bound pool size is often near number of CPU cores.
- I/O-bound pool can be larger because threads spend time waiting.
- If 10 workers each process 100 tasks/sec, pool throughput is roughly 1000 tasks/sec.
- If submission is 1500 tasks/sec, backlog grows by 500 tasks/sec.
- Queue capacity controls how long overload can be absorbed.

Useful formulas:

```text
throughput ~= worker_count / avg_task_time
backlog_growth = submit_rate - completion_rate
```

## 1.6 Clarifying Questions To Ask

- Is the pool fixed or dynamically resizable?
- Should submit block, reject, or run in caller when full?
- Do we need task results/futures?
- What should shutdown do to queued tasks?
- Are tasks CPU-bound or I/O-bound?
- Should task ordering be guaranteed?

Strong interview framing:

> I will design a fixed-size thread pool with a bounded blocking queue. `submit` enqueues tasks if the pool is running, workers loop on `take`, execute tasks outside queue locks, catch exceptions, and shutdown wakes workers so they exit predictably.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Caller Threads
  -> ThreadPool.submit(task)
  -> Bounded Blocking Queue
  -> Worker Threads
  -> Task.run()
```

With results:

```text
Callable<T>
  -> FutureTask<T>
  -> Queue
  -> Worker executes
  -> Future completed with result/error
```

Core flow:

1. Caller submits task.
2. ThreadPool checks pool state.
3. Task is enqueued or rejected based on policy.
4. Worker wakes and takes task.
5. Worker executes task outside queue lock.
6. Worker catches task exceptions.
7. Worker loops until shutdown rules say exit.

## 2.2 APIs

```java
interface Executor {
    void execute(Runnable task);
    <T> Future<T> submit(Callable<T> task);
    void shutdown();
    List<Runnable> shutdownNow();
    boolean awaitTermination(long timeoutMs) throws InterruptedException;
}
```

Rejection policy:

```java
interface RejectionPolicy {
    void reject(Runnable task, ThreadPool pool);
}
```

Common policies:

| Policy | Behavior |
|---|---|
| Abort | throw exception |
| CallerRuns | caller executes task |
| Discard | silently drop task |
| DiscardOldest | drop oldest queued task |
| Block | wait for queue capacity |

## 2.3 Core Components

Think of Thread Pool as five planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Submission plane | API, state check, rejection | controlled admission |
| Queue plane | bounded task storage | backpressure |
| Worker plane | reusable threads | execution |
| Lifecycle plane | running/shutdown/terminated | safe stop |
| Result plane | futures and exceptions | caller feedback |

### Component Responsibility Map

| Component | Owns | Does not own |
|---|---|---|
| `ThreadPool` | state, workers, submit/shutdown | task business logic |
| `BlockingQueue` | task buffering | worker lifecycle |
| `Worker` | take/execute loop | submission policy |
| `RejectionPolicy` | full/shutdown behavior | task execution |
| `FutureTask` | result/error completion | scheduling |

### Worker Lifecycle

States:

```text
NEW -> RUNNING -> SHUTDOWN -> TERMINATED
```

Worker loop:

```text
while pool should continue:
    task = queue.take()
    if task is None and shutdown:
        break
    try:
        task.run()
    catch:
        record failure
```

Important:

- Worker should not die permanently because one task throws.
- Task execution must happen outside queue lock.
- Idle workers must wake on shutdown.

### Shutdown Semantics

Graceful shutdown:

- Stop accepting new tasks.
- Continue processing queued tasks.
- Exit when queue empty and workers idle.

Immediate shutdown:

- Stop accepting new tasks.
- Wake workers.
- Return queued tasks.
- Running tasks may be interrupted if supported.

### Thread Pool Sizing

CPU-bound:

- Use around CPU core count.
- Too many threads cause context-switch overhead.

I/O-bound:

- More threads can help because many are blocked.
- Still bound by memory, downstream capacity, and queue pressure.

Interview signal:

> Thread pool size is a workload decision, not a universal magic number.

## 2.4 Data Layer

In-memory state:

```json
{
  "poolState": "RUNNING",
  "workerCount": 8,
  "queueCapacity": 10000,
  "queuedTasks": 231,
  "completedTasks": 9182
}
```

Data structures:

| Need | Data structure |
|---|---|
| pending tasks | bounded blocking queue |
| workers | list/set of worker threads |
| pool state | atomic enum / lock-protected state |
| metrics | atomic counters |
| future result | condition/latch around result state |

## 2.5 Scalability

### Single Process Scaling

- Tune worker count by workload.
- Use bounded queue.
- Use rejection policy for overload.
- Split pools by task type to avoid starvation.
- Avoid mixing slow blocking tasks with latency-sensitive tasks.

### Distributed Evolution

- Replace in-memory queue with durable queue.
- Run multiple worker processes.
- Add task leases/visibility timeout.
- Add idempotency and retries.
- Add DLQ for terminal failures.

## 2.6 Performance

Optimization rules:

- Reuse threads.
- Keep queue operations O(1).
- Avoid task execution under locks.
- Avoid unbounded queues.
- Track queue wait time and task runtime.
- Separate CPU-bound and I/O-bound pools.

Latency components:

```text
total latency = queue wait time + task execution time + scheduling overhead
```

## 2.7 Async Systems

Thread pools power:

- web server request handling
- async background tasks
- logging appenders
- message consumers
- scheduled jobs
- file processing workers

Reliability upgrades:

- task timeouts
- retries
- cancellation
- circuit breakers around downstream calls
- bulkheads: separate pools per dependency

## 2.8 Safety And Failure Handling

Failure modes:

| Failure | Handling |
|---|---|
| task throws exception | catch and record; worker continues |
| queue full | apply rejection policy |
| submit after shutdown | reject |
| worker thread dies | replace if pool still running |
| shutdown while idle | wake all workers |
| long-running task | timeout/cancel if supported |

Thread-safety rules:

- State transitions are atomic.
- Queue operations are synchronized.
- Worker set updates are synchronized.
- Future completion happens once.

## 2.9 Observability

Track:

- active worker count
- idle worker count
- queue depth
- task wait time
- task execution time
- completed task count
- failed task count
- rejected task count
- shutdown duration

Alerts:

- queue near full
- rejection rate increases
- task failures spike
- active workers stuck high
- queue wait time exceeds SLO

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| queue | bounded | unbounded | memory safety vs submit blocking/rejection |
| pool size | fixed | dynamic | simplicity vs adaptability |
| rejection | abort | caller-runs/block/drop | clear failure vs backpressure/latency |
| shutdown | graceful | immediate | complete accepted work vs fast stop |
| task result | fire-and-forget | future | simplicity vs caller feedback |

Interview framing:

> A thread pool is a bounded producer-consumer system with worker lifecycle management. The most important choices are queue capacity, rejection policy, shutdown semantics, and exception isolation.

---

# 3. Low-Level Design

LLD goal:

> Model Thread Pool around worker threads, a blocking queue, pool state, task execution, rejection policy, and shutdown behavior.

Simple rules:

- Accepted tasks go into queue.
- Workers repeatedly take and execute tasks.
- Pool state controls submit and worker exit.
- Task exceptions do not kill the pool.
- Shutdown wakes idle workers.

## 3.1 Object Modelling

| Entity | Owns | Key invariant |
|---|---|---|
| `ThreadPool` | workers, queue, state | no submit after shutdown |
| `Worker` | execution loop | catches task failure |
| `Task` | work to run | executed at most once in local pool |
| `FutureTask` | result/error state | completes once |
| `RejectionPolicy` | overload behavior | deterministic decision |
| `PoolState` | lifecycle | valid state transitions |

## 3.2 OOP Fundamentals

Encapsulation:

- `ThreadPool` hides worker management.
- `BlockingQueue` hides synchronization.
- `FutureTask` hides completion signaling.

Abstraction:

- `Executor` interface hides implementation.
- `RejectionPolicy` hides overload behavior.

Composition:

- `ThreadPool` composes `BlockingQueue`, `Worker`, and `RejectionPolicy`.

## 3.3 SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | worker executes tasks only |
| Open/Closed | add rejection policy without changing pool |
| Liskov Substitution | any queue follows blocking contract |
| Interface Segregation | separate execute/submit/shutdown APIs |
| Dependency Inversion | pool depends on queue/policy abstractions |

## 3.4 Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Producer-Consumer | submitters and workers | decouple submission/execution |
| Command | task | encapsulate work |
| Strategy | rejection policy | configurable overload behavior |
| State | pool lifecycle | safe transitions |
| Future/Promise | task result | async completion |

## 3.5 Sequence Diagram

```text
Caller -> ThreadPool: submit(task)
ThreadPool -> PoolState: check running
ThreadPool -> BlockingQueue: offer task
Worker -> BlockingQueue: take task
Worker -> Task: run
Worker -> Metrics/Future: record success/failure
```

## 3.6 Class Design

```java
interface Executor {
    void execute(Runnable task);
    void shutdown();
}

interface RejectionPolicy {
    void reject(Runnable task);
}

final class ThreadPool {
    private final BlockingQueue<Runnable> queue;
    private final List<Worker> workers;
    private volatile PoolState state;
}
```

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| queue full | apply rejection policy |
| task throws | catch and continue worker loop |
| submit races with shutdown | atomic state check and enqueue |
| shutdown while workers wait | wake all workers |
| worker dies unexpectedly | replace if pool still running |
| long blocking task | timeout/cancellation if supported |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
threadpool/
  executor/
    ThreadPool.java
    Worker.java
    PoolState.java
  queue/
    BlockingQueue.java
    BoundedBlockingQueue.java
  policy/
    RejectionPolicy.java
    AbortPolicy.java
    CallerRunsPolicy.java
  future/
    FutureTask.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from queue import Queue, Full, Empty
from threading import Event, Thread
from typing import Callable


class SimpleThreadPool:
    def __init__(self, workers: int, queue_capacity: int) -> None:
        self.tasks: Queue[Callable[[], None]] = Queue(maxsize=queue_capacity)
        self.shutdown_event = Event()
        self.threads = [
            Thread(target=self._worker_loop, name=f"worker-{i}", daemon=True)
            for i in range(workers)
        ]
        for thread in self.threads:
            thread.start()

    def submit(self, task: Callable[[], None], timeout: float | None = None) -> None:
        if self.shutdown_event.is_set():
            raise RuntimeError("thread pool is shut down")
        try:
            self.tasks.put(task, timeout=timeout)
        except Full as exc:
            raise RuntimeError("task queue is full") from exc

    def _worker_loop(self) -> None:
        while not self.shutdown_event.is_set() or not self.tasks.empty():
            try:
                task = self.tasks.get(timeout=0.1)
            except Empty:
                continue
            try:
                task()
            except Exception as exc:
                print(f"task failed: {exc}")
            finally:
                self.tasks.task_done()

    def shutdown(self, wait: bool = True) -> None:
        self.shutdown_event.set()
        if wait:
            self.tasks.join()
            for thread in self.threads:
                thread.join(timeout=1)
```

## 4.3 Concurrency Checklist

- Submit checks shutdown state.
- Queue is bounded.
- Workers keep running after task exception.
- Shutdown stops accepting tasks.
- Graceful shutdown drains queue.
- Worker does not hold queue internals while executing task.

## 4.4 Testing Thinking

Tests:

- Submitted tasks execute.
- More tasks than workers execute eventually.
- Queue full rejects or blocks.
- Task exception does not stop later tasks.
- Shutdown rejects new tasks.
- Shutdown waits for queued tasks when requested.

Stress tests:

- Many submitter threads.
- Random task failures.
- Shutdown while submitting.
- Slow tasks causing queue pressure.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike | Risk |
|---|---|
| submit burst | queue fills |
| slow downstream | workers block |
| failing tasks | retry storm |
| CPU-heavy tasks | context switching |

## 5.2 Immediate Response

- Use bounded queue.
- Apply rejection policy.
- Split pools by dependency.
- Add circuit breaker around slow downstream.
- Increase workers only if workload supports it.

## 5.3 Degradation Policy

Protect:

1. Process memory.
2. Worker availability.
3. Accepted task completion.
4. Metrics and retries.

Avoid:

- Unbounded queues.
- One shared pool for all task types.
- Infinite retries inside worker.
- Blocking submit forever on request threads.

## 5.4 Spike Interview Answer

> Under spikes, a thread pool protects the system with bounded queues and rejection policies. For request-path work, I avoid blocking forever. For slow dependencies, I use separate pools and circuit breakers so one dependency cannot starve all workers.

---

# 6. Scaling Beyond One Process

## 6.1 Distributed Evolution

```text
API -> durable queue -> worker pool fleet -> ack/retry/DLQ
```

## 6.2 Additions

- persistent queue
- task lease
- ack on success
- retry with backoff
- DLQ
- autoscaling workers
- idempotent task handlers

## 6.3 Interview Answer

> A local thread pool is for in-process concurrency. At distributed scale, the queue becomes durable and workers become a fleet. The same concepts remain: bounded admission, worker lifecycle, retry, and failure isolation.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

```text
I will clarify fixed vs dynamic pool, queue capacity, rejection policy, task results, and shutdown.
I will design workers pulling from a bounded blocking queue.
Submit checks pool state and enqueues or rejects.
Workers execute tasks outside queue locks and catch exceptions.
Shutdown stops new tasks and either drains or stops immediately.
For production, I add metrics, separate pools, timeouts, and backpressure.
```

---

# 8. Fast Recall Rules

- Thread pool = reusable workers + task queue.
- It is producer-consumer with lifecycle management.
- Use bounded queue.
- Always define rejection policy.
- Do not let task exception kill worker.
- Do not execute task under queue lock.
- Graceful shutdown drains; immediate shutdown stops quickly.
- CPU-bound pool near core count.
- I/O-bound pool can be larger.
- Separate pools prevent starvation across task types.
