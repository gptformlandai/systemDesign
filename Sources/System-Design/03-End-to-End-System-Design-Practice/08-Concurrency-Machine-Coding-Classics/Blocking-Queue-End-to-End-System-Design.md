# Blocking Queue - End-to-End System Design

> Goal: practice one complete thread-safe data-structure classic from problem understanding to LLD, machine coding, correctness proofs, and scale-up thinking.

---

## How To Use This File

- Use this when the interview asks for a blocking queue, bounded queue, concurrent queue, or producer-consumer primitive.
- Start with the contract, then explain locks, condition variables, FIFO ordering, capacity, timeout operations, and shutdown.
- Keep one idea sharp: a blocking queue coordinates threads by making producers wait when full and consumers wait when empty.
- In interviews, highlight spurious wakeups, lost notifications, and why user code must not run while holding internal locks.

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

| Layer | Interview signal | Blocking queue focus |
|---|---|---|
| Problem understanding | Can define queue contract | `put`, `take`, `offer`, `poll`, capacity, shutdown |
| HLD | Can design synchronization | lock, `notEmpty`, `notFull`, FIFO deque |
| LLD | Can model state safely | `BoundedBlockingQueue`, `QueueState`, `WaitPolicy` |
| Machine coding | Can implement critical path | wait while full/empty, signal after state change |
| Traffic spikes | Can protect memory | bounded capacity, producer backpressure |
| Scale | Can reason alternatives | lock-free queues, segmented queues, distributed queues |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Queue supports FIFO insertion and removal.
- `put(item)` blocks if queue is full.
- `take()` blocks if queue is empty.
- `offer(item, timeout)` waits up to timeout and returns success/failure.
- `poll(timeout)` waits up to timeout and returns item/empty.
- Queue exposes `size()` safely.
- Optional `shutdown()` wakes waiting threads and stops future operations.

Optional requirements to clarify:

- Is the queue bounded or unbounded?
- Should ordering be FIFO or priority?
- Is fairness between waiting threads required?
- Should `null` values be allowed?
- Should interrupts/cancellation be supported?
- Do we need shutdown/drain semantics?

Out of scope unless asked:

- Persistent queue.
- Distributed queue.
- Lock-free MPMC queue.
- Priority scheduling.

## 1.2 Non-Functional Requirements

Correctness:

- Size never goes below 0 or above capacity.
- No item disappears after successful `put`.
- No item appears twice from `take`.
- FIFO order is preserved.

Thread safety:

- Multiple producers and consumers can call concurrently.
- No busy waiting.
- No lost wakeups.
- Spurious wakeups are safe.

Performance:

- O(1) enqueue/dequeue.
- Critical section is small.
- Waiting releases lock.

## 1.3 Constraints

- Queue state is shared mutable data.
- Threads can be interrupted or time out.
- `wait()` can wake spuriously.
- `notify()` before a thread waits can cause a lost-signal bug if state checks are wrong.
- Fair locking can reduce throughput.
- `size()` may be immediately stale after return, but internally consistent.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---:|
| Producers | many threads |
| Consumers | many threads |
| Capacity | fixed positive integer |
| Operation cost | O(1) |
| Ordering | FIFO |
| Memory | bounded by capacity |

## 1.5 Capacity Math

Back-of-the-envelope:

- Queue memory is roughly `capacity * average_item_size + overhead`.
- With one lock, throughput is limited by lock contention and scheduling.
- If operations are short, a single lock is usually fine for interviews.
- If contention is very high, segmented queues or lock-free structures may help.

## 1.6 Clarifying Questions To Ask

- Do we need bounded capacity?
- Should operations block forever or support timeout?
- How should shutdown behave?
- Is fairness required?
- Can items be `null`?
- Should we handle interrupts?

Strong interview framing:

> I will implement a bounded FIFO blocking queue using a deque, one lock, and two condition variables. Producers wait on `notFull`, consumers wait on `notEmpty`, waits are guarded by `while`, and shutdown wakes all waiting threads.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Producer threads -> put/offer
                 -> BoundedBlockingQueue
Consumer threads -> take/poll
```

Internal structure:

```text
BoundedBlockingQueue
  - deque items
  - capacity
  - lock
  - condition notEmpty
  - condition notFull
  - shutdown flag
```

Core `put` flow:

1. Acquire lock.
2. While queue full and not shutdown, wait on `notFull`.
3. If shutdown, reject.
4. Add item to tail.
5. Signal `notEmpty`.
6. Release lock.

Core `take` flow:

1. Acquire lock.
2. While queue empty and not shutdown, wait on `notEmpty`.
3. If queue empty and shutdown, return empty/throw.
4. Remove item from head.
5. Signal `notFull`.
6. Release lock.

## 2.2 APIs

```java
interface BlockingQueue<T> {
    void put(T item) throws InterruptedException;
    boolean offer(T item, long timeoutMs) throws InterruptedException;
    T take() throws InterruptedException;
    Optional<T> poll(long timeoutMs) throws InterruptedException;
    int size();
    int capacity();
    void shutdown(boolean drain);
}
```

Important API points:

- `put` and `take` block.
- `offer` and `poll` bound waiting time.
- `size` is thread-safe but not a coordination substitute.
- Shutdown behavior must be documented.

## 2.3 Core Components

Think of Blocking Queue as four planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Storage plane | FIFO deque | item order |
| Capacity plane | max size | memory safety |
| Synchronization plane | lock/conditions | thread safety |
| Lifecycle plane | shutdown/drain | clean exit |

### Component Responsibility Map

| Component | Owns | Does not own |
|---|---|---|
| `Deque` | item order | thread safety by itself |
| `Lock` | mutual exclusion | condition meaning |
| `notFull` | producer waiting | consumer business logic |
| `notEmpty` | consumer waiting | producer business logic |
| `QueueState` | shutdown/drain flags | task processing |

### Why Two Conditions?

Using separate conditions is cleaner:

- Producers wait for space: `notFull`.
- Consumers wait for items: `notEmpty`.
- After enqueue, signal consumers.
- After dequeue, signal producers.

With one condition, all waiters may wake more often than necessary.

### Spurious Wakeups

Bad:

```java
if (queue.isEmpty()) {
    notEmpty.await();
}
```

Good:

```java
while (queue.isEmpty()) {
    notEmpty.await();
}
```

Why:

- Thread can wake without a signal.
- Another consumer may take the item first.
- Shutdown may change expected behavior.

### Fairness

Fairness options:

| Option | Behavior | Trade-off |
|---|---|---|
| non-fair lock | higher throughput | possible thread starvation |
| fair lock | first-waiter preference | lower throughput |
| separate wait queues | more control | more complexity |

Default interview answer:

- Use normal lock unless fairness is required.
- Mention fair lock if starvation matters.

## 2.4 Data Layer

Queue state:

```json
{
  "capacity": 100,
  "size": 0,
  "shutdown": false,
  "drain": true
}
```

Data structures:

| Need | Data structure |
|---|---|
| FIFO items | `ArrayDeque` / circular buffer |
| lock | `ReentrantLock` / monitor |
| producer wait | `Condition notFull` |
| consumer wait | `Condition notEmpty` |
| shutdown state | boolean/enum |

Circular buffer alternative:

- Fixed array.
- `head`, `tail`, `count`.
- Avoids linked-node allocation.
- Slightly more error-prone than deque.

## 2.5 Scalability

### Single Queue Limits

- One lock becomes contention point.
- FIFO ordering requires central coordination.
- For most machine-coding interviews, this is acceptable.

### Higher-Scale Alternatives

- Multiple queues by partition key.
- Lock-free MPMC queues.
- Disruptor/ring buffer style queues.
- Work-stealing deques.
- Distributed queues for multi-process systems.

## 2.6 Performance

Optimization rules:

- Use O(1) enqueue/dequeue.
- Keep critical section tiny.
- Use `signal` for normal operations and `signalAll` on shutdown.
- Prefer circular buffer for low allocation.
- Do not call user code from queue methods.

Latency behavior:

- Under no contention: lock + deque operation.
- Under full queue: producer wait time.
- Under empty queue: consumer wait time.

## 2.7 Async Systems

Blocking queues are used in:

- thread pools
- producer-consumer systems
- async loggers
- connection pools
- bounded work queues
- background task schedulers

The same concepts reappear at distributed scale as:

- broker partitions
- consumer lag
- backpressure
- retention
- visibility timeout

## 2.8 Safety And Failure Handling

Failure modes:

| Failure | Handling |
|---|---|
| producer interrupted | exit without enqueuing partial item |
| consumer interrupted | exit or retry by caller policy |
| shutdown while full | wake producers and reject |
| shutdown while empty | wake consumers and return empty |
| timeout expires | return false/empty |
| item is null | reject to avoid ambiguity |

Invariants:

- `0 <= size <= capacity`.
- `notEmpty` signaled after size increases from 0.
- `notFull` signaled after size decreases from capacity.
- Shutdown wakes all waiters.

## 2.9 Observability

Track:

- queue depth
- enqueue rate
- dequeue rate
- producer wait count/time
- consumer wait count/time
- timeout count
- shutdown count
- max observed depth

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| queue | array ring buffer | linked deque | memory locality vs easier resize |
| lock | one lock | two locks | simplicity vs more concurrency |
| fairness | fair | non-fair | starvation prevention vs throughput |
| blocking | forever | timeout | simple API vs caller control |
| shutdown | supported | not supported | lifecycle safety vs simpler code |

Interview framing:

> A blocking queue is a thread-safe bounded FIFO with explicit wait conditions. The implementation is simple if all queue state is under one lock and every wait is guarded by a while loop.

---

# 3. Low-Level Design

LLD goal:

> Model Blocking Queue around a FIFO buffer, capacity, mutual exclusion, producer condition, consumer condition, and lifecycle state.

Simple rules:

- All queue state changes happen under lock.
- `put` waits while full.
- `take` waits while empty.
- `while` guards wait.
- Signal after state changes.

## 3.1 Object Modelling

| Entity | Owns | Key invariant |
|---|---|---|
| `BoundedBlockingQueue<T>` | buffer and sync | size within capacity |
| `QueueState` | running/shutdown/drain | shutdown wakes waiters |
| `WaitPolicy` | block/timeout behavior | deterministic return |
| `QueueMetrics` | counters and timings | no effect on correctness |

## 3.2 OOP Fundamentals

Encapsulation:

- Queue hides lock and conditions.
- Callers only see blocking methods.

Abstraction:

- Interface allows different implementations.

Composition:

- Thread pool composes blocking queue instead of implementing waiting itself.

## 3.3 SOLID Principles

| Principle | Application |
|---|---|
| Single Responsibility | queue coordinates storage and waiting |
| Open/Closed | add array/deque implementation behind interface |
| Liskov Substitution | implementations preserve blocking contract |
| Interface Segregation | blocking methods separate from metrics/lifecycle if needed |
| Dependency Inversion | thread pool depends on queue interface |

## 3.4 Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Monitor Object | lock + condition guarded state | safe concurrent access |
| Producer-Consumer | put/take coordination | handoff |
| State | shutdown lifecycle | clear behavior |

## 3.5 Sequence Diagram

```text
Producer -> Queue: put(item)
Queue: lock
Queue: wait while full
Queue: enqueue
Queue: signal notEmpty
Queue: unlock

Consumer -> Queue: take()
Queue: lock
Queue: wait while empty
Queue: dequeue
Queue: signal notFull
Queue: unlock
```

## 3.6 Class Design

```java
final class BoundedBlockingQueue<T> {
    private final Deque<T> items;
    private final int capacity;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    private boolean shutdown;
}
```

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| capacity <= 0 | reject constructor |
| null item | reject |
| wait wakes spuriously | re-check condition in while |
| shutdown with waiting producers | signalAll and reject |
| shutdown with waiting consumers | signalAll and return empty/throw |
| timeout while waiting | return false/empty |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
blockingqueue/
  BlockingQueue.java
  BoundedBlockingQueue.java
  QueueClosedException.java
  QueueMetrics.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from collections import deque
from threading import Condition
from time import monotonic
from typing import Deque, Generic, Optional, TypeVar


T = TypeVar("T")


class BoundedBlockingQueue(Generic[T]):
    def __init__(self, capacity: int) -> None:
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        self.capacity = capacity
        self.items: Deque[T] = deque()
        self.condition = Condition()
        self.closed = False

    def put(self, item: T) -> None:
        if item is None:
            raise ValueError("None items are not allowed")
        with self.condition:
            while len(self.items) == self.capacity and not self.closed:
                self.condition.wait()
            if self.closed:
                raise RuntimeError("queue closed")
            self.items.append(item)
            self.condition.notify_all()

    def take(self) -> Optional[T]:
        with self.condition:
            while not self.items and not self.closed:
                self.condition.wait()
            if not self.items:
                return None
            item = self.items.popleft()
            self.condition.notify_all()
            return item

    def offer(self, item: T, timeout_seconds: float) -> bool:
        deadline = monotonic() + timeout_seconds
        with self.condition:
            while len(self.items) == self.capacity and not self.closed:
                remaining = deadline - monotonic()
                if remaining <= 0:
                    return False
                self.condition.wait(remaining)
            if self.closed:
                return False
            self.items.append(item)
            self.condition.notify_all()
            return True

    def poll(self, timeout_seconds: float) -> Optional[T]:
        deadline = monotonic() + timeout_seconds
        with self.condition:
            while not self.items and not self.closed:
                remaining = deadline - monotonic()
                if remaining <= 0:
                    return None
                self.condition.wait(remaining)
            if not self.items:
                return None
            item = self.items.popleft()
            self.condition.notify_all()
            return item

    def shutdown(self) -> None:
        with self.condition:
            self.closed = True
            self.condition.notify_all()
```

## 4.3 Concurrency Checklist

- Use one condition/lock or two conditions with one lock.
- Guard waits with `while`.
- Signal after enqueue/dequeue.
- Handle timeout with monotonic clock.
- Wake all on shutdown.
- Reject nulls if `None` means timeout/closed.

## 4.4 Testing Thinking

Tests:

- FIFO order.
- `put` blocks when full.
- `take` blocks when empty.
- `offer` times out.
- `poll` times out.
- Multiple producers/consumers preserve exactly-once dequeue.
- Shutdown wakes blocked threads.

Stress tests:

- Random producer/consumer sleeps.
- Queue capacity 1.
- Many threads.
- Shutdown during wait.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike | Risk |
|---|---|
| producer burst | queue fills |
| no consumers | producers block forever |
| slow consumers | high wait time |
| shutdown storm | stuck waiters if not notified |

## 5.2 Immediate Response

- Use bounded capacity.
- Use timed `offer` for request-path callers.
- Track queue depth.
- Add consumers or shed load.
- Wake all waiters on shutdown.

## 5.3 Degradation Policy

Protect:

1. Memory safety.
2. FIFO correctness.
3. Thread liveness.
4. Metrics.

Do not:

- Use busy loops.
- Ignore spurious wakeups.
- Allow size above capacity.
- Forget to notify on shutdown.

## 5.4 Spike Interview Answer

> During producer spikes, the bounded blocking queue applies backpressure. If callers cannot block indefinitely, I expose timed `offer`. I measure queue depth and wait time so overload is visible.

---

# 6. Scaling Beyond One Queue

## 6.1 Alternatives

| Alternative | Use |
|---|---|
| multiple queues | partition by key |
| priority blocking queue | urgent tasks |
| lock-free queue | high-throughput low-latency systems |
| distributed queue | multiple processes/machines |
| ring buffer | fixed-size low-GC systems |

## 6.2 Interview Answer

> The simple blocking queue is correct and interview-friendly. If contention is the bottleneck, I can partition queues or use more advanced lock-free/ring-buffer designs, but I would start with the monitor-based version for clarity.

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
I will clarify bounded capacity, FIFO, blocking vs timeout, null handling, fairness, and shutdown.
I will use a deque, a lock, and conditions.
put waits while full and signals notEmpty after enqueue.
take waits while empty and signals notFull after dequeue.
I use while for spurious wakeups.
I wake all waiters on shutdown.
I test capacity 1 and many producers/consumers.
```

---

# 8. Fast Recall Rules

- Blocking Queue = FIFO + lock + conditions.
- Bounded queue protects memory.
- Producers wait on full.
- Consumers wait on empty.
- Always wait in `while`.
- Signal after state change.
- Use monotonic time for timeout.
- Reject null if null means no item.
- Shutdown must wake all.
- Size is informational, not a synchronization primitive.
