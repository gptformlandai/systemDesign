# Python Concurrency — Threading, Multiprocessing & Executors — MAANG Master Sheet

> **Track**: Python Interview Track — Group 3: Senior MAANG  
> **File**: 1 of 4 (Track File #14)  
> **Audience**: Java developers targeting MAANG-level Python backend interviews  
> **Read after**: Python-Backend-APIs-FastAPI-Flask-Patterns-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Gets Java Devs |
|---|---|---|
| The GIL — what it is and what it blocks | ★★★★★ | No Java equivalent; the most-asked Python concurrency question at MAANG |
| Threading vs multiprocessing — which for CPU, which for I/O | ★★★★★ | Java threads are true parallel; default CPython threads are limited by GIL for CPU work |
| `threading.Lock` — acquire/release, deadlock risks | ★★★★★ | Java `synchronized` / `ReentrantLock`; Python is nearly identical in API |
| `concurrent.futures.ThreadPoolExecutor` | ★★★★★ | Java `ExecutorService` equivalent; most common pattern in production code |
| `concurrent.futures.ProcessPoolExecutor` | ★★★★☆ | Java `ForkJoinPool` equivalent; true parallelism for CPU-bound work |
| `multiprocessing.Queue` and `Pipe` — IPC | ★★★★☆ | `BlockingQueue` equivalent; cross-process communication |
| Race conditions — detection and prevention | ★★★★★ | Java `synchronized`; Python needs explicit locks; easy trap |
| `threading.Event`, `Condition`, `Semaphore` | ★★★★☆ | Java `CountDownLatch`, `Semaphore`, `Condition`; slightly different API |
| `multiprocessing.Value` / `Array` — shared memory | ★★★☆☆ | Java shared heap; Python processes don't share memory by default |
| `futures.Future` — submit and retrieve results | ★★★★☆ | Java `Future<T>` / `CompletableFuture`; Python version is simpler |
| `as_completed` vs `map` — concurrent result collection | ★★★★☆ | Java `invokeAll` vs `invokeAny`; Python's `as_completed` has no Java direct match |
| GIL release in C extensions — why numpy parallelizes | ★★★☆☆ | MAANG-level question; C extensions can release GIL for true parallelism |

---

## 2. The GIL — Global Interpreter Lock

### Must Know

The GIL is the single most important Python concurrency concept for default CPython. Every MAANG interview that touches Python performance or concurrency will bring it up.

```
What it is:
  A mutex (lock) built into default GIL-enabled CPython's interpreter.
  Only ONE thread can execute Python bytecode at any given time in that default build.
  Threads still exist and still provide concurrency — but NOT true parallelism for CPU work.

What it DOES block:
  Multiple threads running Python code simultaneously (e.g., computing prime numbers).
  CPU-bound parallel work across multiple cores — cannot use all cores with threading.

What it does NOT block:
  I/O operations — a thread releases the GIL while waiting on network/disk.
  C extensions that explicitly release the GIL (numpy, scipy, Pillow do this).
  Multiple processes — each process has its OWN GIL.

Java equivalent:
  There is none. Java threads are truly parallel — all cores usable from one JVM.
  This is the fundamental reason Python uses multiprocessing for CPU work.
```

Version caveat:
- Python 3.13+ supports optional **free-threaded CPython builds** where the GIL can be disabled.
- Treat this as an advanced caveat, not the baseline. Dependency support, C-extension compatibility, memory overhead, and single-thread performance must be validated before recommending it in production.

### GIL in Practice

```python
import threading
import time

# Demonstration: GIL prevents CPU-bound speedup with threads

def cpu_bound_task(n: int) -> int:
    """Pure Python computation — fully GIL-bound."""
    result = 0
    for i in range(n):
        result += i * i
    return result

N = 10_000_000

# Single-threaded: 1 thread, full workload
start = time.perf_counter()
cpu_bound_task(N)
single_time = time.perf_counter() - start

# Multi-threaded: 2 threads, each half the workload
start = time.perf_counter()
t1 = threading.Thread(target=cpu_bound_task, args=(N // 2,))
t2 = threading.Thread(target=cpu_bound_task, args=(N // 2,))
t1.start(); t2.start()
t1.join(); t2.join()
thread_time = time.perf_counter() - start

print(f"Single thread: {single_time:.2f}s")
print(f"Two threads:   {thread_time:.2f}s")
# On default GIL-enabled CPython, both times are approximately EQUAL — threads add overhead, GIL prevents parallelism!
# On a CPU-bound task, threading can be SLOWER than single-threaded code.
```

### When Threading DOES Help

```python
import threading
import time
import urllib.request

# I/O-bound: each thread releases the GIL while waiting for network response
# Other threads run while one thread waits for I/O

def fetch_url(url: str) -> None:
    urllib.request.urlopen(url).read()   # GIL released during network wait

urls = ["https://httpbin.org/delay/1"] * 5

# Sequential: ~5 seconds (each waits for previous)
start = time.perf_counter()
for url in urls:
    fetch_url(url)
seq_time = time.perf_counter() - start

# Threaded: ~1 second (all wait concurrently)
start = time.perf_counter()
threads = [threading.Thread(target=fetch_url, args=(url,)) for url in urls]
for t in threads: t.start()
for t in threads: t.join()
thread_time = time.perf_counter() - start

print(f"Sequential: {seq_time:.2f}s")
print(f"Threaded:   {thread_time:.2f}s")   # ~5x speedup for I/O!
```

### GIL and C Extensions

```python
import numpy as np
import threading

# numpy operations release the GIL — TRUE parallelism with threads
a = np.random.rand(10_000_000)

def numpy_sum(arr):
    return np.sum(arr)   # C code, GIL released during computation

# Two threads running numpy operations in PARALLEL (GIL released in C code)
# This is why numpy + threading can actually scale across cores
```

---

## 3. `threading` Module — Core Primitives

### Thread Creation and Lifecycle

```python
import threading
import time
from typing import Any

# Method 1: threading.Thread with target function
def worker(name: str, delay: float) -> None:
    print(f"Thread {name} starting")
    time.sleep(delay)
    print(f"Thread {name} done")

t = threading.Thread(
    target=worker,
    args=("A",),
    kwargs={"delay": 1.0},
    name="WorkerThread-A",
    daemon=True,   # Daemon threads die when main thread exits
)
t.start()          # Start the thread
t.join()           # Block until thread finishes
t.join(timeout=5)  # Block at most 5 seconds
print(t.is_alive())  # False after join completes

# Method 2: Subclass threading.Thread
class PriceCalculator(threading.Thread):
    def __init__(self, items: list[int]):
        super().__init__(daemon=True)
        self.items = items
        self.result: float | None = None    # Shared result field

    def run(self) -> None:
        """Called by t.start() — runs in new thread."""
        self.result = sum(self.items) * 1.10   # 10% markup

calc = PriceCalculator([100, 200, 300])
calc.start()
calc.join()
print(calc.result)   # 660.0
```

### Daemon vs Non-Daemon Threads

```python
import threading
import time

# Non-daemon (default): main program waits for them to finish
t_regular = threading.Thread(target=lambda: time.sleep(10))
t_regular.start()
# Program won't exit until this thread finishes (10 seconds)

# Daemon: killed when main thread exits — used for background workers
t_daemon = threading.Thread(target=lambda: time.sleep(10), daemon=True)
t_daemon.start()
# Program exits immediately — daemon thread is killed

# TRAP: daemon threads don't get cleanup (try/finally) before being killed
# Don't use daemon=True for threads that write to files or update databases

# Current thread info
print(threading.current_thread().name)   # Thread name
print(threading.main_thread().name)       # "MainThread"
print(threading.active_count())           # Total active threads
```

---

## 4. Thread Safety — Race Conditions and Locks

### Race Condition — The Core Problem

```python
import threading

# Shared mutable state WITHOUT synchronization = race condition
counter = 0

def increment_unsafe() -> None:
    global counter
    for _ in range(100_000):
        counter += 1   # NOT atomic! This is: temp = counter; temp += 1; counter = temp
        # Context switch can happen between read and write — another thread sees stale value

threads = [threading.Thread(target=increment_unsafe) for _ in range(10)]
for t in threads: t.start()
for t in threads: t.join()
print(counter)   # Should be 1_000_000 but gets a DIFFERENT value every run!
# Example output: 743_892 — race condition!

# Java equivalent: non-synchronized counter.count++ has the same issue
```

### `threading.Lock` — Mutual Exclusion

```python
import threading

counter = 0
lock = threading.Lock()

def increment_safe() -> None:
    global counter
    for _ in range(100_000):
        with lock:          # Acquires lock; releases on exit (even on exception)
            counter += 1   # Only one thread runs this at a time

threads = [threading.Thread(target=increment_safe) for _ in range(10)]
for t in threads: t.start()
for t in threads: t.join()
print(counter)   # Always 1_000_000 — thread-safe!

# Manual lock management (not recommended — prefer with statement)
lock.acquire()
try:
    counter += 1
finally:
    lock.release()   # MUST release even on exception

# Trylock — non-blocking acquire
acquired = lock.acquire(blocking=False)
if acquired:
    try:
        counter += 1
    finally:
        lock.release()
else:
    print("Lock busy — skip or retry")

# Acquire with timeout
acquired = lock.acquire(timeout=2.0)   # Wait at most 2 seconds
if not acquired:
    raise TimeoutError("Could not acquire lock")
```

### `threading.RLock` — Reentrant Lock

```python
import threading

# Regular Lock: calling acquire() twice from the SAME thread → DEADLOCK
# RLock: the same thread can acquire it multiple times without deadlocking

lock = threading.RLock()

def outer():
    with lock:
        print("Outer acquired lock")
        inner()   # Calls inner which also acquires lock — OK with RLock!

def inner():
    with lock:   # RLock: same thread can re-acquire; Lock: deadlock here!
        print("Inner acquired lock")

outer()   # Works fine with RLock

# Java equivalent: java.util.concurrent.locks.ReentrantLock
# Java's synchronized is also reentrant
```

### Deadlock — Detection and Prevention

```python
import threading
import time

# Classic deadlock: Thread A holds lock1, wants lock2
#                   Thread B holds lock2, wants lock1

lock1 = threading.Lock()
lock2 = threading.Lock()

def thread_a():
    with lock1:
        time.sleep(0.01)   # Give thread B time to grab lock2
        with lock2:        # DEADLOCK — thread B holds lock2 and wants lock1
            print("Thread A done")

def thread_b():
    with lock2:
        time.sleep(0.01)
        with lock1:        # DEADLOCK — thread A holds lock1 and wants lock2
            print("Thread B done")

# Prevention: always acquire locks in the SAME ORDER
# If all threads always acquire lock1 before lock2 → no deadlock possible

def thread_a_safe():
    with lock1:
        with lock2:   # Always lock1 first, then lock2
            print("Thread A done")

def thread_b_safe():
    with lock1:       # Same order: lock1 first, then lock2
        with lock2:
            print("Thread B done")
```

---

## 5. Synchronization Primitives

### `threading.Event` — Thread Signaling

```python
import threading
import time

# Event: one thread signals, others wait
# Java equivalent: CountDownLatch(1) or a manual flag with notify/wait

ready_event = threading.Event()

def producer():
    print("Producer: preparing data...")
    time.sleep(2)
    ready_event.set()   # Signal all waiting threads
    print("Producer: data ready, event set")

def consumer(name: str):
    print(f"Consumer {name}: waiting for data...")
    ready_event.wait()              # Block until event is set
    # ready_event.wait(timeout=5)  # Optional timeout
    print(f"Consumer {name}: got signal, processing data")

threads = [
    threading.Thread(target=producer),
    threading.Thread(target=consumer, args=("A",)),
    threading.Thread(target=consumer, args=("B",)),
]
for t in threads: t.start()
for t in threads: t.join()

# Event methods:
# event.set()      — wake all waiting threads
# event.clear()    — reset to unset state
# event.is_set()   — check without blocking
# event.wait(timeout) — block until set or timeout
```

### `threading.Semaphore` — Limit Concurrency

```python
import threading
import time

# Semaphore: limits number of threads that can run concurrently
# Java equivalent: java.util.concurrent.Semaphore

# Allow at most 3 concurrent connections to a resource
connection_pool = threading.Semaphore(3)

def use_connection(thread_id: int) -> None:
    with connection_pool:   # Acquires one permit; blocks if all 3 are taken
        print(f"Thread {thread_id}: using connection")
        time.sleep(1)
        print(f"Thread {thread_id}: releasing connection")

# 10 threads compete for 3 slots — at most 3 run at any time
threads = [threading.Thread(target=use_connection, args=(i,)) for i in range(10)]
for t in threads: t.start()
for t in threads: t.join()

# BoundedSemaphore — raises ValueError if release() called more times than acquire()
# Regular Semaphore allows more releases than acquires (programming error detection missing)
bounded_sem = threading.BoundedSemaphore(5)
```

### `threading.Condition` — Wait for State Change

```python
import threading
from collections import deque

# Condition: combines Lock + wait/notify for producer-consumer patterns
# Java equivalent: synchronized + wait() + notifyAll()

buffer: deque[int] = deque()
condition = threading.Condition()
MAX_BUFFER = 5

def producer():
    for i in range(20):
        with condition:
            while len(buffer) >= MAX_BUFFER:
                condition.wait()   # Release lock and wait until notified
            buffer.append(i)
            print(f"Produced {i}, buffer size: {len(buffer)}")
            condition.notify_all()   # Wake consumers

def consumer(name: str):
    consumed = 0
    while consumed < 10:
        with condition:
            while not buffer:
                condition.wait()   # Wait until buffer has items
            item = buffer.popleft()
            consumed += 1
            print(f"Consumer {name} consumed {item}")
            condition.notify_all()   # Wake producers (space freed)

p = threading.Thread(target=producer)
c1 = threading.Thread(target=consumer, args=("A",))
c2 = threading.Thread(target=consumer, args=("B",))
for t in [p, c1, c2]: t.start()
for t in [p, c1, c2]: t.join()
```

### `threading.Barrier` — Synchronization Point

```python
import threading
import time

# Barrier: all threads must reach a point before any can proceed
# Java equivalent: CyclicBarrier

NUM_WORKERS = 4
barrier = threading.Barrier(NUM_WORKERS)

def worker(worker_id: int) -> None:
    print(f"Worker {worker_id}: doing phase 1 work")
    time.sleep(worker_id * 0.5)   # Different amounts of work

    barrier.wait()   # All workers must reach here before any continue
    print(f"Worker {worker_id}: all done with phase 1, starting phase 2")

threads = [threading.Thread(target=worker, args=(i,)) for i in range(NUM_WORKERS)]
for t in threads: t.start()
for t in threads: t.join()
```

---

## 6. `concurrent.futures` — High-Level Executor API

### Must Know

`concurrent.futures` is the high-level API for running callables concurrently. It abstracts over both threads and processes with a uniform interface. This is the **standard way to run parallel tasks in modern Python** — prefer it over manual thread/process management.

```python
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor, Future
from concurrent.futures import as_completed, wait, FIRST_COMPLETED, ALL_COMPLETED
from typing import Iterator
import time

# ThreadPoolExecutor — I/O-bound work
# ProcessPoolExecutor — CPU-bound work (bypasses the default CPython GIL)

def fetch_data(url: str) -> str:
    """Simulated I/O-bound task."""
    time.sleep(0.5)   # Simulate network call
    return f"data from {url}"
```

### `submit()` — Individual Tasks with `Future`

```python
from concurrent.futures import ThreadPoolExecutor, Future

def double(n: int) -> int:
    return n * 2

with ThreadPoolExecutor(max_workers=4) as executor:
    # submit() returns a Future immediately (non-blocking)
    future: Future[int] = executor.submit(double, 21)

    # future.result() blocks until the task completes (or raises the exception)
    result = future.result()           # 42
    result = future.result(timeout=5)  # Raises TimeoutError if not done in 5s

    # Check state without blocking
    future.done()       # True if complete (success or error)
    future.running()    # True if currently executing
    future.cancelled()  # True if was cancelled before starting

    # Exception access
    exc = future.exception()   # Returns exception if task raised one; None otherwise
    if exc:
        print(f"Task failed: {exc}")
```

### `map()` — Apply Function to Iterable

```python
from concurrent.futures import ThreadPoolExecutor

urls = [f"https://api.example.com/items/{i}" for i in range(10)]

def fetch(url: str) -> str:
    import time; time.sleep(0.1)
    return f"data:{url}"

with ThreadPoolExecutor(max_workers=5) as executor:
    # map() preserves order; blocks until all are done
    # Returns results as a generator
    results = list(executor.map(fetch, urls))

    # With timeout — raises TimeoutError if any call exceeds limit
    results = list(executor.map(fetch, urls, timeout=10))

# map() vs submit():
# map: blocks until ALL tasks complete; results in submission order
# submit: returns Future immediately; allows per-task control
```

### `as_completed()` — Results As They Finish

```python
from concurrent.futures import ThreadPoolExecutor, as_completed

tasks = {1: 2.0, 2: 0.5, 3: 1.0}   # task_id: sleep_seconds

def slow_task(task_id: int, delay: float) -> tuple[int, str]:
    import time; time.sleep(delay)
    return task_id, f"result_{task_id}"

with ThreadPoolExecutor(max_workers=3) as executor:
    # Submit all tasks, keep future→task_id mapping
    future_to_id = {
        executor.submit(slow_task, tid, delay): tid
        for tid, delay in tasks.items()
    }

    # as_completed yields futures in ORDER OF COMPLETION (fastest first)
    for future in as_completed(future_to_id):
        task_id = future_to_id[future]
        try:
            _, result = future.result()
            print(f"Task {task_id} completed: {result}")
        except Exception as e:
            print(f"Task {task_id} failed: {e}")

# Output order: task 2 (0.5s), task 3 (1.0s), task 1 (2.0s)
# vs executor.map: would return in submission order (1, 2, 3) — REGARDLESS of completion order

# Java: CompletableFuture.allOf().thenApply() or ExecutorCompletionService
```

### `ProcessPoolExecutor` — True CPU Parallelism

```python
from concurrent.futures import ProcessPoolExecutor
import math

def is_prime(n: int) -> bool:
    """CPU-bound: pure computation, no I/O."""
    if n < 2:
        return False
    for i in range(2, int(math.sqrt(n)) + 1):
        if n % i == 0:
            return False
    return True

numbers = list(range(1_000_000, 1_001_000))

# Single-threaded
import time
start = time.perf_counter()
primes_single = [n for n in numbers if is_prime(n)]
single_time = time.perf_counter() - start

# ProcessPoolExecutor — each worker is a separate Python process with its own GIL
# BYPASSES the per-process GIL — true parallelism across CPU cores
start = time.perf_counter()
with ProcessPoolExecutor(max_workers=4) as executor:
    results = list(executor.map(is_prime, numbers, chunksize=50))
primes_process = [n for n, is_p in zip(numbers, results) if is_p]
process_time = time.perf_counter() - start

print(f"Single:  {single_time:.2f}s")
print(f"Process: {process_time:.2f}s")   # ~4x faster with 4 cores

# chunksize: batch multiple items per worker call to reduce IPC overhead
# For large iterables, chunksize=100 or higher reduces serialization overhead
```

### Choosing ThreadPoolExecutor vs ProcessPoolExecutor

```
ThreadPoolExecutor:
  - I/O-bound tasks: network requests, file I/O, database queries
  - Low overhead: threads share memory; no serialization of args/results
  - Default CPython GIL limits CPU parallelism — all threads share one GIL
  - Java: ExecutorService with thread pool

ProcessPoolExecutor:
  - CPU-bound tasks: image processing, data crunching, ML inference, prime checking
  - True parallelism: each process has its own GIL, runs on its own core
  - Higher overhead: args and results are pickled/unpickled for inter-process communication
  - Functions and args MUST be picklable (no lambdas, no local functions in the main scope)
  - Java: ForkJoinPool, parallel streams

Rule of thumb:
  I/O-bound → ThreadPoolExecutor (or asyncio)
  CPU-bound → ProcessPoolExecutor
  Heavy CPU with numpy/scipy → ThreadPoolExecutor (C extensions release GIL)
```

---

## 7. `multiprocessing` Module — Direct Process Control

### `Process` — Creating Child Processes

```python
from multiprocessing import Process, current_process
import os

def worker(name: str) -> None:
    print(f"Process {name}: PID={os.getpid()}, parent={os.getppid()}")
    # This runs in a COMPLETELY separate Python interpreter
    # No shared memory with parent (except explicitly shared via Queue/Pipe/Value)

if __name__ == "__main__":   # REQUIRED for multiprocessing on Windows/macOS
    processes = [Process(target=worker, args=(f"P{i}",)) for i in range(4)]
    for p in processes: p.start()
    for p in processes: p.join()

# CRITICAL: multiprocessing code MUST be inside if __name__ == "__main__"
# On Windows/macOS (spawn start method), the module is re-imported in child processes
# Without the guard, child processes create MORE children → infinite loop → crash
```

### `multiprocessing.Queue` — Inter-Process Communication

```python
from multiprocessing import Process, Queue
import time

def producer(q: Queue, items: list[int]) -> None:
    for item in items:
        q.put(item)
        print(f"Producer: put {item}")
    q.put(None)   # Sentinel — signals consumer to stop

def consumer(q: Queue, results: list) -> None:
    while True:
        item = q.get()   # Blocks until item is available
        if item is None:
            break
        results.append(item * 2)
        print(f"Consumer: processed {item}")

if __name__ == "__main__":
    q = Queue()

    # NOTE: cannot share a regular list across processes — use Queue
    # Results list passed to consumer won't be visible in parent process!
    # Use Queue to return results from workers:

    result_queue: Queue = Queue()

    def compute(n: int, out: Queue) -> None:
        out.put(n * n)

    procs = [Process(target=compute, args=(i, result_queue)) for i in range(5)]
    for p in procs: p.start()
    for p in procs: p.join()

    results = [result_queue.get() for _ in range(5)]
    print(results)   # [0, 1, 4, 9, 16] in some order
```

### `multiprocessing.Pipe` — Bidirectional Channel

```python
from multiprocessing import Process, Pipe

def child_process(conn) -> None:
    msg = conn.recv()       # Receive from parent
    print(f"Child got: {msg}")
    conn.send(f"Echo: {msg}")  # Send back to parent
    conn.close()

if __name__ == "__main__":
    parent_conn, child_conn = Pipe()   # Returns (parent_end, child_end)

    p = Process(target=child_process, args=(child_conn,))
    p.start()

    parent_conn.send("Hello from parent")
    response = parent_conn.recv()   # Blocks until child sends
    print(f"Parent got: {response}")

    p.join()
```

### Shared Memory — `Value` and `Array`

```python
from multiprocessing import Process, Value, Array
import ctypes

def increment(shared_val, lock) -> None:
    for _ in range(1000):
        with lock:
            shared_val.value += 1

if __name__ == "__main__":
    from multiprocessing import Lock

    # Value — single shared scalar
    counter = Value(ctypes.c_int, 0)   # Shared integer, initialized to 0
    lock = Lock()

    procs = [Process(target=increment, args=(counter, lock)) for _ in range(4)]
    for p in procs: p.start()
    for p in procs: p.join()

    print(counter.value)   # 4000 — correct with lock, race without

    # Array — shared array of C types
    shared_arr = Array(ctypes.c_double, [1.0, 2.0, 3.0, 4.0, 5.0])
    print(list(shared_arr))   # [1.0, 2.0, 3.0, 4.0, 5.0]

# TRAP: multiprocessing.Value/Array use ctypes — must use C type codes
# c_int, c_double, c_bool, c_char_p, etc.

# For more complex shared data, use multiprocessing.Manager():
from multiprocessing import Manager

with Manager() as manager:
    shared_list = manager.list([1, 2, 3])   # Shared list via proxy
    shared_dict = manager.dict({"key": "value"})  # Shared dict via proxy
    # These are slower (RPC over pipe) but support complex types
```

---

## 8. Thread-Local Storage

```python
import threading

# threading.local() — data that is unique per thread
# Java equivalent: ThreadLocal<T>

local_data = threading.local()

def worker(name: str) -> None:
    # Each thread has its OWN copy of local_data attributes
    local_data.name = name
    local_data.count = 0

    for _ in range(100):
        local_data.count += 1   # No lock needed — each thread has its own count

    print(f"Thread {name}: count = {local_data.count}")   # Always 100
    print(f"Thread {name}: local name = {local_data.name}")

threads = [threading.Thread(target=worker, args=(f"T{i}",)) for i in range(3)]
for t in threads: t.start()
for t in threads: t.join()
# Each thread prints its own values — no interference

# Common use: per-thread database connections, request context in web servers
# Django uses threading.local() internally for request context
# Flask uses threading.local() for the request context (g, request objects)
```

---

## 9. Common Concurrency Patterns

### Worker Pool with Queue

```python
import threading
import queue
import time

def worker(task_queue: queue.Queue, results: list, lock: threading.Lock) -> None:
    while True:
        try:
            task = task_queue.get(timeout=1)   # Block up to 1 second
        except queue.Empty:
            break
        # Process task
        result = task * 2
        with lock:
            results.append(result)
        task_queue.task_done()   # Signal task completed

# Note: queue.Queue (thread-safe) vs multiprocessing.Queue (process-safe)
task_queue: queue.Queue[int] = queue.Queue()
results: list[int] = []
lock = threading.Lock()

# Fill queue with work
for i in range(20):
    task_queue.put(i)

# Start worker pool
pool = [threading.Thread(target=worker, args=(task_queue, results, lock)) for _ in range(4)]
for t in pool: t.start()

task_queue.join()   # Block until all tasks are processed
print(sorted(results))
```

### Thread-Safe Counter Using `threading.Lock`

```python
import threading

class ThreadSafeCounter:
    """Thread-safe counter. Java equivalent: AtomicInteger."""

    def __init__(self, initial: int = 0) -> None:
        self._value = initial
        self._lock = threading.Lock()

    def increment(self) -> None:
        with self._lock:
            self._value += 1

    def decrement(self) -> None:
        with self._lock:
            self._value -= 1

    def get(self) -> int:
        with self._lock:
            return self._value

    def reset(self) -> None:
        with self._lock:
            self._value = 0

counter = ThreadSafeCounter()
threads = [threading.Thread(target=counter.increment) for _ in range(1000)]
for t in threads: t.start()
for t in threads: t.join()
print(counter.get())   # Always 1000
```

### Read-Write Lock Pattern

```python
import threading

class ReadWriteLock:
    """Multiple readers OR one writer. Java: ReentrantReadWriteLock."""

    def __init__(self) -> None:
        self._read_ready = threading.Condition(threading.RLock())
        self._readers = 0

    def acquire_read(self) -> None:
        with self._read_ready:
            self._readers += 1

    def release_read(self) -> None:
        with self._read_ready:
            self._readers -= 1
            if self._readers == 0:
                self._read_ready.notify_all()

    def acquire_write(self) -> None:
        self._read_ready.acquire()
        while self._readers > 0:
            self._read_ready.wait()

    def release_write(self) -> None:
        self._read_ready.release()
```

---

## 10. Multiprocessing Pitfalls

### Pitfall 1 — Functions Must Be Picklable

```python
from concurrent.futures import ProcessPoolExecutor

# FAILS: lambda is not picklable
with ProcessPoolExecutor() as ex:
    results = list(ex.map(lambda x: x * 2, [1, 2, 3]))   # pickle.PicklingError!

# FAILS: local function defined inside another function
def outer():
    def inner(x):   # Nested function — not picklable
        return x * 2
    with ProcessPoolExecutor() as ex:
        results = list(ex.map(inner, [1, 2, 3]))   # Fails!

# WORKS: module-level functions
def double(x: int) -> int:
    return x * 2

with ProcessPoolExecutor() as ex:
    results = list(ex.map(double, [1, 2, 3]))   # [2, 4, 6]

# WORKS: functools.partial
from functools import partial

def multiply(x: int, factor: int) -> int:
    return x * factor

double_fn = partial(multiply, factor=2)   # Partial is picklable
with ProcessPoolExecutor() as ex:
    results = list(ex.map(double_fn, [1, 2, 3]))
```

### Pitfall 2 — No Shared State Between Processes

```python
from multiprocessing import Process

shared_list = []   # THIS IS NOT SHARED! Each process gets a COPY

def add_to_list(item: int) -> None:
    shared_list.append(item)   # Modifies the child's COPY, not the parent's

if __name__ == "__main__":
    procs = [Process(target=add_to_list, args=(i,)) for i in range(5)]
    for p in procs: p.start()
    for p in procs: p.join()
    print(shared_list)   # [] — EMPTY! Changes happened in child processes

# Fix: use multiprocessing.Queue to return results
```

### Pitfall 3 — Multiprocessing Start Method

```python
import multiprocessing

# Three start methods:
# "spawn" (default on Windows/macOS): fresh Python process, must re-import everything
# "fork" (default on Linux): copies parent process memory (faster but unsafe with threads)
# "forkserver": separate server process handles spawning (safe with threads)

# "fork" can cause deadlocks if parent has threads with locks held
# macOS changed default to "spawn" in Python 3.8 to avoid this

# Set start method explicitly (must be called once, at program start)
if __name__ == "__main__":
    multiprocessing.set_start_method("spawn")   # Safe on all platforms
    # ... rest of multiprocessing code
```

---

## 11. Java Developer Bridge — Complete Comparison

| Concept | Java | Python |
|---|---|---|
| GIL | No equivalent — JVM threads are truly parallel | Default CPython GIL limits CPU parallelism per process; Python 3.13+ free-threaded builds are the caveat |
| Thread creation | `new Thread(runnable)` or `implements Runnable` | `threading.Thread(target=func)` |
| Start thread | `thread.start()` | `thread.start()` |
| Wait for thread | `thread.join()` | `thread.join()` |
| Daemon thread | `thread.setDaemon(true)` | `Thread(daemon=True)` |
| Thread local | `ThreadLocal<T>` | `threading.local()` |
| Mutex | `synchronized` block / `ReentrantLock` | `threading.Lock()` with `with` |
| Reentrant lock | `ReentrantLock` (default) | `threading.RLock()` |
| Semaphore | `java.util.concurrent.Semaphore` | `threading.Semaphore(n)` |
| Condition variable | `lock.wait()` / `lock.notifyAll()` | `threading.Condition()` |
| CountDownLatch | `CountDownLatch(n)` | `threading.Barrier(n)` |
| Signal between threads | `CountDownLatch(1)` | `threading.Event()` |
| Thread pool (I/O) | `Executors.newFixedThreadPool(n)` | `ThreadPoolExecutor(max_workers=n)` |
| Future | `Future<T>` | `concurrent.futures.Future` |
| Submit task | `executor.submit(callable)` | `executor.submit(func, *args)` |
| Get result | `future.get()` | `future.result()` |
| Map over list | `executor.invokeAll(tasks)` | `executor.map(func, items)` |
| First completed | `ExecutorCompletionService` | `as_completed(futures)` |
| CPU-bound pool | `ForkJoinPool` | `ProcessPoolExecutor(max_workers=n)` |
| Parallel stream | `stream.parallel()` | `ProcessPoolExecutor.map()` |
| Shared atomic int | `AtomicInteger` | `threading.Lock()` + regular int |
| BlockingQueue | `LinkedBlockingQueue` | `queue.Queue` (threads) / `multiprocessing.Queue` |
| Inter-process IPC | `Socket`, `Pipe` (external) | `multiprocessing.Pipe` / `Queue` |
| Shared memory | JVM heap (all threads share) | `multiprocessing.Value` / `Array` / `shared_memory` |
| Deadlock | Same risk — lock order discipline required | Same risk — same solution |
| `volatile` | `volatile` keyword | No direct equivalent — use `threading.Event` or locks |

---

## 12. Hot Interview Q&A

**Q: What is the GIL and why does Python have it?**  
A: In default CPython, the GIL (Global Interpreter Lock) is a mutex that ensures only one thread executes Python bytecode at a time. Historically, it simplified CPython's reference counting and interpreter-state safety. The consequence is that default CPython threads do not achieve CPU parallelism for pure Python bytecode — for CPU-bound work you usually need `multiprocessing` or native extensions that release the GIL. For I/O-bound work, threads still provide concurrency because threads release the GIL while waiting on I/O. Python 3.13+ free-threaded builds can disable the GIL, but that is a version- and deployment-specific caveat, not the default answer.

**Q: When do you use `ThreadPoolExecutor` vs `ProcessPoolExecutor`?**  
A: `ThreadPoolExecutor` for I/O-bound work — network calls, database queries, file I/O. Threads share memory (low overhead), and the GIL is released during I/O, so threads run concurrently. `ProcessPoolExecutor` for CPU-bound pure Python work — image processing, numerical computation, data parsing. Each process has its own GIL and runs on its own CPU core, providing true parallelism under default CPython. The tradeoff: processes have higher startup overhead and must serialize (pickle) all arguments and results across process boundaries.

**Q: What is a race condition and how do you prevent it in Python?**  
A: A race condition occurs when two threads read and modify shared state without synchronization, and the final result depends on the order of execution (which is nondeterministic). In Python, `counter += 1` is three bytecode instructions (LOAD, ADD, STORE) — a context switch between any of them causes data loss. Prevention: wrap the critical section with `threading.Lock()` using the `with` statement. For atomic operations on simple values, `collections.Counter` and `queue.Queue` are already thread-safe. For compound operations, always use explicit locks.

**Q: Explain `as_completed()` vs `executor.map()` — when does each matter?**  
A: `executor.map(func, items)` is a blocking call that submits all tasks and returns results in the original input order. You cannot process a result until all faster results before it are also available — slow tasks at the front block all following results. `as_completed(futures)` is a generator that yields each future as soon as it finishes — fastest tasks come first regardless of submission order. Use `map` when you need ordered results and all tasks take roughly equal time. Use `as_completed` when you want to process results progressively (e.g., stream to a database), handle per-task errors individually, or have heterogeneous task durations.

**Q: Why must multiprocessing code go inside `if __name__ == "__main__"`?**  
A: On Windows and macOS, Python uses the `spawn` start method — it starts a fresh Python interpreter for each child process by importing the main module from scratch. If the multiprocessing code is at module level (not inside the `if __name__` guard), the child process also executes it during import, spawning grandchild processes, which spawn great-grandchildren — an infinite recursive explosion. The `if __name__ == "__main__"` guard ensures the spawning code only runs in the original parent process.

**Q: What happens to shared data when you use `multiprocessing.Process`?**  
A: Nothing useful — each child process gets a COPY of the parent's memory at spawn time (on Linux with fork) or a fresh empty namespace (with spawn on Windows/macOS). Modifying a list or dict in a child process does NOT affect the parent's copy. This is a fundamental difference from Java where all threads share the JVM heap. To share data between Python processes, you must use explicit IPC: `multiprocessing.Queue` (item passing), `multiprocessing.Pipe` (channel), `multiprocessing.Value`/`Array` (shared memory via ctypes), or `multiprocessing.Manager()` (proxy objects).

**Q: Can you achieve true parallelism in Python with threads?**  
A: Yes, but only under specific conditions. In default CPython, pure Python code is limited by the GIL — threads compete for it and only one runs Python bytecode at a time. However, C extensions that explicitly release the GIL (numpy, scipy, Pillow, OpenCV, database drivers) can run truly in parallel in multiple threads. Python 3.13+ free-threaded builds are another advanced path if the environment supports them. The practical rule remains: pure Python computation on default CPython → multiprocessing; I/O or C-extension work → threading or asyncio.

---

## 13. Final Revision Checklist

### GIL

- [ ] I can explain what the GIL is and why CPython has it
- [ ] I know the GIL prevents CPU parallelism with threads but NOT I/O concurrency
- [ ] I can add the Python 3.13+ free-threaded build caveat without derailing the baseline answer
- [ ] I know C extensions (numpy) release the GIL — threads can run numpy in parallel
- [ ] I know the GIL is per-process — `multiprocessing` bypasses it

### Threading Primitives

- [ ] I know `threading.Thread(target=func, daemon=True)` and `.start()` / `.join()`
- [ ] I can explain the race condition with `counter += 1` (3 bytecodes, non-atomic)
- [ ] I use `threading.Lock()` with `with lock:` for critical sections
- [ ] I know `RLock` allows same-thread re-entry; `Lock` deadlocks on re-entry
- [ ] I know `Event.set()` / `Event.wait()` for thread signaling
- [ ] I know `Semaphore(n)` limits concurrency to n threads
- [ ] I know `Condition.wait()` / `Condition.notify_all()` for producer-consumer
- [ ] I know `threading.local()` for per-thread state — no lock needed

### `concurrent.futures`

- [ ] I use `ThreadPoolExecutor` for I/O-bound; `ProcessPoolExecutor` for CPU-bound
- [ ] I know `executor.submit(func, *args)` returns a `Future`
- [ ] I know `future.result()` blocks; `future.exception()` checks for errors
- [ ] I know `executor.map()` preserves order; `as_completed()` yields fastest-first
- [ ] I know functions passed to `ProcessPoolExecutor` must be picklable (no lambdas)

### `multiprocessing`

- [ ] I know `multiprocessing.Process` does NOT share memory with parent
- [ ] I use `multiprocessing.Queue` to pass data between processes
- [ ] I know `Value(ctypes.c_int, 0)` for shared scalars; needs its own `Lock`
- [ ] I always put multiprocessing code inside `if __name__ == "__main__":` guard

### Java Developer Reminders

- [ ] Java threads are truly parallel (no GIL); Python threads are limited for CPU work
- [ ] `ExecutorService` → `ThreadPoolExecutor`; `ForkJoinPool` → `ProcessPoolExecutor`
- [ ] `ReentrantLock` → `threading.Lock()` + `with`; `synchronized` block → `with lock:`
- [ ] `AtomicInteger` → `threading.Lock()` + plain `int` (no atomic types in Python stdlib)
- [ ] Java shared heap → Python processes need explicit `Queue`/`Pipe`/`Value` for IPC

---

*File 1 of 4 — Group 3: Senior MAANG*  
*Next: Python-AsyncIO-Modern-Concurrency-MAANG-Master-Sheet.md*
