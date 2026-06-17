# Producer-Consumer - End-to-End System Design

> Goal: practice one complete machine-coding + design classic from problem understanding to LLD, thread-safe implementation, failure handling, and scale-up thinking.

---

## How To Use This File

- Use this when the interview asks about producer-consumer, bounded buffer, worker pipeline, async processing, or backpressure.
- Start with a single in-process queue, then evolve to multiple producers, multiple consumers, graceful shutdown, retry, and distributed queues.
- Keep one idea sharp: producers and consumers share a buffer, so correctness depends on synchronization around empty/full conditions.
- In interviews, explain the buffer, capacity, lock, condition variables, state transitions, shutdown policy, and failure behavior.

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

| Layer | Interview signal | Producer-consumer focus |
|---|---|---|
| Problem understanding | Can define coordination contract | producers publish work, consumers process work, bounded memory |
| HLD | Can design async pipeline | producer threads, blocking queue, consumer workers, retry/dead-letter |
| LLD | Can model thread-safe components | `Task`, `BoundedBuffer`, `Producer`, `Consumer`, `ConsumerGroup` |
| Machine coding | Can implement critical path | `put`, `take`, wait on full/empty, notify, shutdown |
| Traffic spikes | Can protect system | backpressure, dropping, queue limits, slow consumers |
| Scale | Can evolve design | partitioned queues, distributed brokers, idempotent consumers |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Multiple producer threads can submit tasks.
- Multiple consumer threads can take and process tasks.
- Queue can be bounded by capacity.
- Producers block or fail when queue is full, depending policy.
- Consumers block or timeout when queue is empty.
- System supports graceful shutdown.
- Optional retry and dead-letter handling for failed tasks.

Optional requirements to clarify:

- Should `put` block forever, timeout, or return false when full?
- Should `take` block forever, timeout, or return null when empty?
- Should task ordering be FIFO?
- What happens if consumer processing fails?
- Is graceful shutdown required?
- Do we need priorities?

Out of scope unless asked:

- Full distributed message broker.
- Exactly-once processing.
- Persistent disk-backed queue.
- Complex scheduling.

## 1.2 Non-Functional Requirements

Correctness:

- No task should be lost after successful `put`, unless policy allows dropping.
- No task should be processed twice in the in-memory single-queue version.
- Queue size must never exceed capacity.
- Consumers must not take from an empty queue.

Thread safety:

- Shared queue state must be protected.
- Waiting threads must wake up correctly.
- Spurious wakeups must be handled with `while`, not `if`.
- Shutdown should not deadlock waiting threads.

Performance:

- Avoid busy waiting.
- Keep critical sections small.
- Use bounded memory.

## 1.3 Constraints

- Producers and consumers run concurrently.
- Consumer processing can be slower than production.
- Failures can happen during task processing.
- Waiting without condition variables wastes CPU.
- Incorrect notify logic can deadlock.
- Unbounded queues can cause memory pressure.

## 1.4 Scale Assumptions

Example machine-coding assumptions:

| Metric | Assumption |
|---|---:|
| Producers | 1-100 threads |
| Consumers | 1-100 threads |
| Queue capacity | 100-100K tasks |
| Task processing time | milliseconds to seconds |
| Ordering | FIFO within one queue |
| Memory policy | bounded queue |

## 1.5 Capacity Math

Back-of-the-envelope:

- If producers create `1000 tasks/sec` and consumers process `800 tasks/sec`, backlog grows by `200 tasks/sec`.
- A bounded queue of `10,000` tasks fills in `50 seconds` at that imbalance.
- Throughput is limited by consumer count and average processing time.
- Increasing queue capacity hides overload temporarily; it does not fix slow consumers.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Queue capacity | fixed memory budget |
| Consumer throughput | `num_consumers / avg_task_seconds` |
| Producer wait time | grows when queue is full |
| Shutdown drain time | `queue_size / consumer_throughput` |

## 1.6 Clarifying Questions To Ask

- Should producers block, timeout, or drop when full?
- Should consumers block, timeout, or stop on shutdown?
- Should shutdown drain existing tasks or stop immediately?
- Should task failures be retried?
- Is ordering strict FIFO?
- Are producers/consumers in one process or distributed?

Strong interview framing:

> I will design a bounded producer-consumer system using a shared FIFO queue protected by a lock and two conditions: `notFull` for producers and `notEmpty` for consumers. Producers wait when full, consumers wait when empty, and shutdown wakes all waiters to avoid deadlock.

---

# 2. High-Level Design

## 2.1 Architecture

Single-process version:

```text
Producer Threads
  -> Bounded Blocking Queue
  -> Consumer Worker Threads
  -> Task Handler
```

With retry/dead-letter:

```text
Producer
  -> Main Queue
  -> Consumer
  -> success: ack/drop from memory
  -> failure: Retry Queue or DLQ
```

Core flow:

1. Producer calls `put(task)`.
2. Queue acquires lock.
3. If full, producer waits on `notFull`.
4. Queue enqueues task.
5. Queue signals `notEmpty`.
6. Consumer calls `take()`.
7. If empty, consumer waits on `notEmpty`.
8. Queue dequeues task.
9. Queue signals `notFull`.
10. Consumer processes task outside the lock.

## 2.2 APIs

### Queue API

```java
interface BlockingTaskQueue<T> {
    void put(T item) throws InterruptedException;
    boolean offer(T item, long timeoutMs) throws InterruptedException;
    T take() throws InterruptedException;
    Optional<T> poll(long timeoutMs) throws InterruptedException;
    void shutdown(boolean drain);
    int size();
}
```

### Producer API

```java
interface Producer<T> {
    void produce(T item);
}
```

### Consumer API

```java
interface TaskHandler<T> {
    void handle(T item) throws Exception;
}
```

Important API points:

- `put` and `take` are blocking.
- `offer` and `poll` support timeout.
- `shutdown(drain=true)` lets consumers finish queued work.
- `shutdown(drain=false)` wakes waiters and stops quickly.

## 2.3 Core Components

Think of Producer-Consumer as four planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Buffer plane | bounded queue state | safe handoff |
| Synchronization plane | lock and conditions | no races/deadlocks |
| Worker plane | producer/consumer loops | throughput |
| Failure plane | retry, DLQ, shutdown | predictable recovery |

### Component Responsibility Map

| Component | Owns | Does not own |
|---|---|---|
| `BoundedBlockingQueue` | queue state, capacity, wait/notify | task business logic |
| `Producer` | task creation | consumer scheduling |
| `ConsumerWorker` | task polling and handling | queue internals |
| `TaskHandler` | actual business processing | synchronization |
| `RetryPolicy` | retry decision | core queue state |
| `DeadLetterQueue` | failed terminal tasks | normal processing |

### Synchronization Design

Shared state:

- Queue/deque.
- Capacity.
- Shutdown flag.
- Drain mode.

Synchronization:

- One lock protects shared state.
- `notFull` waits for capacity.
- `notEmpty` waits for items.
- `while` loops guard against spurious wakeups.
- Processing happens outside lock.

Race-condition trap:

```text
if queue is empty:
    wait()

This is wrong. A thread can wake up spuriously or another consumer can take the item first.

Correct:
while queue is empty:
    wait()
```

### Backpressure

Why it exists:

- Producers can outpace consumers.
- Without bounds, memory can grow until the process fails.

Policies:

| Policy | Behavior | Use |
|---|---|---|
| block | producer waits | preserve tasks |
| timeout | producer waits briefly then fails | request path safety |
| drop newest | reject incoming task | telemetry/debug |
| drop oldest | make room by removing old task | latest-state systems |

Interview signal:

> A bounded queue is a backpressure mechanism, not just a data structure.

### Shutdown Semantics

Drain shutdown:

- Stop accepting new tasks.
- Consumers process remaining tasks.
- Consumers exit when queue empty.

Immediate shutdown:

- Stop accepting new tasks.
- Wake all waiters.
- Consumers exit quickly.
- Remaining tasks may be returned or dropped depending policy.

Important:

- Shutdown must notify both producer and consumer conditions.
- Producers waiting on full queue must wake and fail/return.
- Consumers waiting on empty queue must wake and exit.

## 2.4 Data Layer

Single-process state:

```json
{
  "capacity": 1000,
  "size": 42,
  "shutdown": false,
  "drain": true
}
```

Task:

```json
{
  "taskId": "task-1",
  "payload": {},
  "attempt": 0,
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Data structures:

| Need | Data structure |
|---|---|
| FIFO buffer | `Deque<T>` |
| bounded capacity | integer capacity + size |
| waiting producers | `Condition notFull` |
| waiting consumers | `Condition notEmpty` |
| retry tracking | attempt count |
| DLQ | separate queue/list |

## 2.5 Scalability

### Single Process Scaling

- Increase consumer count for I/O-bound tasks.
- Keep CPU-bound consumer count near CPU cores.
- Use batching for high overhead handlers.
- Use multiple queues for priority or partitioning.

### Distributed Evolution

Move from in-memory queue to:

- Kafka/Pulsar for ordered partitions and replay.
- RabbitMQ/SQS for managed work queue semantics.
- Redis streams/lists for simpler distributed queues.

Distributed trade-off:

- Need acknowledgments.
- Need retry and DLQ.
- Need idempotent consumers.
- Need visibility timeout or lease.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| enqueue under no contention | microseconds |
| dequeue under no contention | microseconds |
| wait when full/empty | workload-dependent |
| task processing | business-dependent |

### Optimization Rules

- Do not process task while holding queue lock.
- Use `signal` for one waiter, `signalAll` on shutdown.
- Keep queue operations O(1).
- Avoid unbounded queues.
- Measure queue depth and wait time.

## 2.7 Async Systems

Producer-consumer is the basic shape behind:

- logging appenders
- thread pools
- async notification delivery
- video processing workers
- message queue consumers
- background job systems

Core reliability upgrades:

- persistent queue
- ack/visibility timeout
- retry policy
- DLQ
- idempotent task handling

## 2.8 Safety And Failure Handling

Failure modes:

| Failure | Handling |
|---|---|
| producer interrupted while waiting | return/throw without corrupting queue |
| consumer interrupted while waiting | exit or retry based on policy |
| task handler throws | retry or DLQ outside queue lock |
| queue full forever | timeout/backpressure |
| shutdown while threads wait | wake all waiters |

Thread-safety rules:

- Every read/write of queue state happens under lock.
- Wait condition is checked in a `while`.
- Notify after state changes.
- Never call user code while holding lock.

## 2.9 Observability

Track:

- queue depth
- enqueue rate
- dequeue rate
- producer wait time
- consumer idle time
- task processing latency
- failure/retry/DLQ count
- shutdown drain time

Alerts:

- queue stays near capacity
- consumers stop draining
- retry/DLQ spikes
- producer timeouts increase

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| queue | bounded | unbounded | memory safety vs possible producer blocking |
| producer full policy | block | fail/drop | no loss vs request latency |
| shutdown | drain | immediate | task completion vs fast stop |
| ordering | FIFO | priority | fairness vs urgency |
| processing | inside lock | outside lock | simpler state vs throughput/deadlock risk |

Interview framing:

> I would implement Producer-Consumer with a bounded queue, one lock, two conditions, `while`-guarded waits, and explicit shutdown semantics. The queue provides backpressure; consumers handle tasks outside the lock.

---

# 3. Low-Level Design

LLD goal:

> Model producer-consumer around a bounded buffer, wait conditions, producer/consumer worker loops, task handling, retries, and shutdown state.

Simple rules:

- Queue state is protected by one lock.
- Producers wait while full.
- Consumers wait while empty.
- Notify after enqueue/dequeue.
- User task code runs outside the queue lock.

## 3.1 Object Modelling

| Entity | Owns | Key invariant |
|---|---|---|
| `Task` | payload and attempt count | task identity does not change |
| `BoundedBuffer` | queue, lock, conditions | size never exceeds capacity |
| `Producer` | task generation | does not mutate queue internals |
| `ConsumerWorker` | take/process loop | exits cleanly on shutdown |
| `RetryPolicy` | retry count/backoff | terminal failures go to DLQ |

## 3.2 OOP Fundamentals

Encapsulation:

- `BoundedBuffer` hides lock and condition variables.
- `ConsumerWorker` hides worker loop.
- `TaskHandler` hides business logic.

Abstraction:

- `BlockingQueue<T>` interface separates queue contract from implementation.
- `TaskHandler<T>` separates processing from scheduling.

Composition:

- `ConsumerGroup` composes queue, workers, handler, and retry policy.

## 3.3 SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | queue only coordinates handoff |
| Open/Closed | add retry policy without changing queue |
| Liskov Substitution | any queue implementation follows blocking contract |
| Interface Segregation | separate queue, handler, producer interfaces |
| Dependency Inversion | workers depend on queue/handler abstractions |

## 3.4 Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Producer-Consumer | whole design | decouple production from processing |
| Strategy | retry/drop policies | configurable failure behavior |
| Command | task object | encapsulate work |
| State | queue shutdown state | explicit lifecycle |

## 3.5 Sequence Diagram

```text
Producer -> Queue: put(task)
Queue -> Queue: wait while full
Queue -> Queue: enqueue task
Queue -> Consumer: signal notEmpty
Consumer -> Queue: take()
Queue -> Queue: wait while empty
Queue -> Queue: dequeue task
Queue -> Producer: signal notFull
Consumer -> TaskHandler: handle(task)
```

## 3.6 Class Design

```java
interface BlockingQueue<T> {
    void put(T item) throws InterruptedException;
    T take() throws InterruptedException;
    void shutdown(boolean drain);
}

interface TaskHandler<T> {
    void handle(T item) throws Exception;
}

final class ConsumerWorker<T> implements Runnable {
    private final BlockingQueue<T> queue;
    private final TaskHandler<T> handler;
}
```

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| spurious wakeup | use `while` around wait |
| producer waits during shutdown | wake and reject |
| consumer waits during shutdown | wake and exit if no drain |
| task handler fails | retry/DLQ outside queue lock |
| producer faster than consumer | bounded queue applies backpressure |
| consumer faster than producer | consumers block without CPU spin |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
producerconsumer/
  domain/
    Task.java
  queue/
    BlockingQueue.java
    BoundedBlockingQueue.java
  worker/
    Producer.java
    ConsumerWorker.java
    ConsumerGroup.java
  policy/
    RetryPolicy.java
```

## 4.2 Core Logic Implementation

Python sketch using `Condition`:

```python
from collections import deque
from threading import Condition
from typing import Deque, Generic, Optional, TypeVar


T = TypeVar("T")


class BoundedBlockingQueue(Generic[T]):
    def __init__(self, capacity: int) -> None:
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        self.capacity = capacity
        self.items: Deque[T] = deque()
        self.condition = Condition()
        self.shutdown_requested = False
        self.drain = True

    def put(self, item: T) -> None:
        with self.condition:
            while len(self.items) >= self.capacity and not self.shutdown_requested:
                self.condition.wait()
            if self.shutdown_requested:
                raise RuntimeError("queue is shut down")
            self.items.append(item)
            self.condition.notify_all()

    def take(self) -> Optional[T]:
        with self.condition:
            while not self.items and not self.shutdown_requested:
                self.condition.wait()
            if not self.items:
                return None
            item = self.items.popleft()
            self.condition.notify_all()
            return item

    def shutdown(self, drain: bool = True) -> None:
        with self.condition:
            self.shutdown_requested = True
            self.drain = drain
            if not drain:
                self.items.clear()
            self.condition.notify_all()

    def size(self) -> int:
        with self.condition:
            return len(self.items)
```

## 4.3 Concurrency Checklist

- `put` checks full condition inside lock.
- `take` checks empty condition inside lock.
- Both use `while`, not `if`.
- State changes call `notify_all`.
- Shutdown wakes all waiters.
- User processing happens after `take`, outside queue lock.

## 4.4 Testing Thinking

Tests:

- Queue blocks producers when full.
- Queue blocks consumers when empty.
- Multiple producers/consumers process every item once.
- Shutdown wakes waiting producer.
- Shutdown wakes waiting consumer.
- Queue size never exceeds capacity.

Stress tests:

- 100 producers, 100 consumers.
- Random sleeps.
- Interrupt/shutdown while threads wait.
- Handler failure with retry/DLQ.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike | Risk |
|---|---|
| producer burst | queue fills |
| slow consumers | backlog grows |
| failing tasks | retry storm |
| shutdown during load | lost or stuck work |

## 5.2 Immediate Response

- Apply backpressure by blocking producers.
- Add consumers if workload is I/O-bound.
- Use timeouts for request-path producers.
- Send terminal failures to DLQ.
- Shed optional work under overload.

## 5.3 Degradation Policy

Protect:

1. Process health and memory.
2. Already accepted tasks.
3. Fairness between producers.
4. Optional retries and metrics.

Do not:

- Use unbounded memory.
- Busy-wait.
- Swallow handler failures silently.
- Deadlock on shutdown.

## 5.4 Spike Interview Answer

> When producers outpace consumers, the bounded queue applies backpressure. If this is request-path code, I use `offer` with timeout instead of blocking forever. If processing fails, retries are bounded and terminal failures go to a DLQ.

---

# 6. Scaling Beyond One Process

## 6.1 Distributed Architecture

```text
Producers -> Durable Queue/Broker -> Consumer Group -> Handler -> Ack/DLQ
```

## 6.2 Distributed Additions

- Persistent storage.
- Ack after successful processing.
- Visibility timeout or lease.
- Retry topic/queue.
- Dead-letter queue.
- Partitioning for scale.
- Idempotent consumers.

## 6.3 Interview Answer

> The in-process producer-consumer pattern uses locks and conditions. At distributed scale, the queue becomes a broker with persistence, ack, retry, DLQ, and partitioning. The core idea stays the same: bounded buffering decouples producers and consumers while applying backpressure.

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

Use this answer flow:

```text
I will clarify bounded vs unbounded, blocking vs timeout, FIFO vs priority, retry, and shutdown semantics.
I will model a bounded queue protected by a lock and two conditions: notFull and notEmpty.
Producers wait while full; consumers wait while empty.
I use while loops for spurious wakeups.
I process tasks outside the lock.
I support shutdown by waking all waiters.
For scale, I move to a durable broker with ack/retry/DLQ.
```

---

# 8. Fast Recall Rules

- Producer-consumer = shared bounded buffer.
- Use lock + condition variables.
- Wait in `while`, not `if`.
- Producers wait when full.
- Consumers wait when empty.
- Notify after enqueue/dequeue.
- Never run user code under queue lock.
- Bounded queue is backpressure.
- Shutdown must wake all waiters.
- Distributed version needs ack, retry, DLQ, and idempotency.
