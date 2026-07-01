# 17. PyCharm: Python Threading, GIL, and Concurrency Debug

## Goal

Debug multithreaded Python code — understand the GIL, inspect thread state in PyCharm, suspend individual threads, debug ThreadPoolExecutor, and inspect synchronization primitives.

---

## The Global Interpreter Lock (GIL)

```text
Python (CPython) has a GIL: a mutex that allows only ONE thread to execute
Python bytecode at a time.

Implication for threading debug:
  - Only one thread is "active" (running Python) at any moment.
  - I/O operations (network, disk) release the GIL.
  - CPU-bound threads compete for the GIL — true parallelism not achieved.
  - Race conditions are still possible (GIL is released between bytecodes).
  - threading module: uses OS threads; useful for I/O-bound work.
  - multiprocessing: separate processes with separate GILs; true parallelism.
```

---

## PyCharm Threads Panel

When paused at a breakpoint in multithreaded code:

```text
Debug window -> Frames panel header -> thread dropdown
  Shows all active threads:
    MainThread     RUNNING  (or PAUSED AT BREAKPOINT)
    Thread-1       WAITING
    Thread-2       BLOCKED
    ThreadPoolExecutor-0_0  RUNNING
    ...

Click any thread -> Frames panel updates to show that thread's call stack.
```

---

## Inspecting Thread State

```python
import threading

t = threading.current_thread()

# In PyCharm Evaluate Expression (Alt+F8):
threading.current_thread().name      # 'MainThread' or 'Thread-1'
threading.current_thread().ident     # OS thread ID
threading.active_count()             # total live threads
[t.name for t in threading.enumerate()]  # all thread names

# Inspect a specific thread.
threading.enumerate()  # returns list of all Thread objects
```

---

## Basic Thread Debug

```python
import threading

counter = 0

def increment():
    global counter
    for _ in range(100000):
        counter += 1   # <- breakpoint: which thread runs here?

t1 = threading.Thread(target=increment, name='incrementer-1')
t2 = threading.Thread(target=increment, name='incrementer-2')

t1.start()
t2.start()

t1.join()
t2.join()

print(counter)  # Expected: 200000. Actual: often less (race condition)
```

### Debug Steps

```text
1. Set breakpoint on: counter += 1
2. Configure breakpoint: Suspend = Thread (not All)
   -> Only the thread that hits the breakpoint pauses
   -> Other threads continue running
3. When thread 1 pauses:
   -> Check the thread dropdown: see which thread is paused
   -> Resume only thread 1
4. Observe counter value on each step
5. Switch to thread 2 in the dropdown to see its independent frame
```

---

## ThreadPoolExecutor Debug

```python
from concurrent.futures import ThreadPoolExecutor

def process_order(order_id):
    # <- set breakpoint here
    # PyCharm shows: thread is ThreadPoolExecutor-0_N (pool worker thread name)
    result = fetch_order(order_id)
    return result

with ThreadPoolExecutor(max_workers=4) as executor:
    futures = [executor.submit(process_order, f'ORD-{i}') for i in range(10)]
    results = [f.result() for f in futures]
```

When a breakpoint fires in a pool worker:
- The Frames dropdown shows the worker thread name (e.g., ThreadPoolExecutor-0_3)
- Other workers continue running (if Suspend = Thread)
- You can switch between worker thread frames

---

## threading.Lock and threading.RLock Debug

```python
import threading

lock = threading.Lock()

def transfer(from_acc, to_acc, amount):
    with lock:  # <- breakpoint inside lock: only one thread here at a time
        from_acc.balance -= amount
        to_acc.balance += amount

# In Evaluate Expression when paused:
lock.locked()   # True if lock is currently held
```

### Inspecting Nested Locks (RLock)

```python
import threading

rlock = threading.RLock()

def recursive_work(n):
    with rlock:
        if n > 0:
            recursive_work(n - 1)  # RLock allows same thread to re-acquire

# In Evaluate:
rlock._count    # how many times current thread has acquired the lock
rlock._owner    # thread ID that owns the lock
```

---

## threading.Event Debug

```python
import threading

event = threading.Event()

def producer():
    # produce data
    event.set()  # <- signal the consumer

def consumer():
    event.wait()  # <- breakpoint: thread is blocked here until event is set
    # consume data
```

In PyCharm when paused at `event.wait()`:
```python
# In Evaluate Expression:
event.is_set()   # False = thread is blocked waiting
```

---

## Suspending Individual Threads (PyCharm)

```text
Frames panel thread dropdown:
  Right-click a thread -> "Suspend" (suspends that specific thread)
  Right-click a suspended thread -> "Resume" (resumes that specific thread)

Use case:
  Freeze thread A while letting thread B run and modify shared state.
  Resume thread A to see it react to the changed state.
  Deterministic reproduction of race conditions.
```

---

## Common Thread Safety Bugs In Python

### Race Condition: Non-Atomic Increment

```python
# counter += 1 is NOT atomic.
# Python compiles it to:
#   LOAD_FAST counter
#   LOAD_CONST 1
#   INPLACE_ADD
#   STORE_FAST counter
# GIL can switch threads between these bytecodes.

# Fix: threading.Lock or threading.atomic alternatives.
import threading
lock = threading.Lock()

def safe_increment():
    with lock:
        counter += 1
```

### Thread-Safe Queues

```python
from queue import Queue

q = Queue()

def producer():
    q.put('item')  # thread-safe

def consumer():
    item = q.get()  # thread-safe, blocks when empty
```

---

## Interview Sound Bite

Python's GIL means only one thread executes Python bytecode at a time, but the GIL is released between bytecodes so race conditions are still possible — `counter += 1` compiles to multiple bytecodes and is not atomic. PyCharm's thread dropdown in the Frames panel shows all live threads; click any to switch to its call stack. Set breakpoint suspension to Thread (not All) to pause only the triggering thread and let others continue — this enables deterministic race condition reproduction. Use `threading.enumerate()` in Evaluate Expression to list all live threads at any point.
