# 23. Python: GIL, Threading, Multiprocessing, AsyncIO Debug

## Goal

Understand what the GIL allows and blocks, debug threading vs multiprocessing vs asyncio, and use py-spy for profiling stuck Python processes.

---

## GIL: What It Prevents And What It Doesn't

```text
The Global Interpreter Lock (GIL) is a mutex in CPython.
It allows only ONE thread to execute Python bytecode at any time.

What the GIL PREVENTS:
  - True parallel CPU execution of Python bytecode across threads.
  - Memory corruption from unsynchronized access to Python objects.

What the GIL DOES NOT PREVENT:
  - Race conditions on compound operations (read-modify-write).
    counter += 1  is multiple bytecodes; GIL releases between them.
  - Deadlocks from user-level locks.
  - Concurrent I/O (GIL is released during I/O wait).

GIL release triggers:
  - Every 5ms (sys.getswitchinterval()) by default.
  - During I/O: socket recv/send, file read/write.
  - During C extension calls (e.g., numpy operations release GIL).
```

---

## When To Use threading vs multiprocessing vs asyncio

```text
threading:
  Use for I/O-bound work (network calls, disk, DB queries).
  GIL releases during I/O, so threads run concurrently on I/O.
  Shared memory (heap), needs synchronization for shared state.

multiprocessing:
  Use for CPU-bound work (computation, data processing, ML inference).
  Separate Python processes, each with its own GIL.
  True parallelism on multiple cores.
  No shared memory (unless mp.shared_memory or mp.Queue).

asyncio:
  Use for high-concurrency I/O (many simultaneous connections).
  Single-threaded, cooperative multitasking via event loop.
  No GIL contention, no thread synchronization needed.
  Cannot do CPU-bound work without blocking the event loop.
```

---

## threading Debug

```python
import threading

counter = 0
lock = threading.Lock()

def safe_increment(n: int):
    global counter
    for _ in range(n):
        with lock:       # <- breakpoint: which thread holds the lock?
            counter += 1

t1 = threading.Thread(target=safe_increment, args=(100000,), name='T1')
t2 = threading.Thread(target=safe_increment, args=(100000,), name='T2')

t1.start()
t2.start()

t1.join()  # wait for T1
t2.join()  # wait for T2

print(counter)  # Always 200000 with lock.
```

### Debug In PyCharm

```text
1. Breakpoint on: with lock: line.
2. Suspend = Thread (not All).
3. When T1 hits the breakpoint:
   -> T2 continues running.
4. In PyCharm Evaluate:
   lock.locked()  # True (T1 holds it)
   threading.current_thread().name  # 'T1'
5. In Frames dropdown: switch to T2 to see where T2 is now.
```

---

## multiprocessing Debug

```python
import multiprocessing as mp

def worker(job_id: int, result_queue: mp.Queue):
    # This runs in a SEPARATE PROCESS, not a thread.
    result = process_job(job_id)
    result_queue.put(result)

if __name__ == '__main__':
    result_queue = mp.Queue()
    
    p1 = mp.Process(target=worker, args=(1, result_queue))
    p2 = mp.Process(target=worker, args=(2, result_queue))
    
    p1.start()
    p2.start()
    
    p1.join()
    p2.join()
    
    results = []
    while not result_queue.empty():
        results.append(result_queue.get())
    print(results)
```

### Debug Limitation

Breakpoints in the `worker` function do NOT hit in PyCharm by default — each `mp.Process` is a separate OS process, not a thread.

To debug multiprocessing workers:
```python
# Option 1: Use debugpy to attach to each child process.
import debugpy

def worker(job_id, result_queue):
    if os.getenv('DEBUG_WORKERS') == '1':
        debugpy.listen(5679)  # different port per worker or use dynamic allocation
        debugpy.wait_for_client()
    # now worker is debuggable
```

---

## asyncio + threading: Mixing (Danger Zone)

```python
import asyncio
import threading

# Wrong: calling asyncio coroutine from a thread.
async def fetch_data():
    await asyncio.sleep(1)
    return 'data'

def thread_function():
    # asyncio.run() creates a NEW event loop in this thread.
    result = asyncio.run(fetch_data())  # OK: each thread gets own loop
    print(result)

# More complex: submitting coroutine to an existing event loop from a thread.
loop = asyncio.get_event_loop()
future = asyncio.run_coroutine_threadsafe(fetch_data(), loop)  # thread-safe
result = future.result(timeout=5)
```

---

## py-spy: Profile Stuck Python Processes

py-spy samples Python process stack traces without modifying the process.

```bash
# Install.
pip install py-spy

# Sample a running process for 10 seconds.
py-spy top --pid 12345

# Dump current stack trace of all threads.
py-spy dump --pid 12345

# Generate flame graph.
py-spy record -o profile.svg --pid 12345 --duration 30
open profile.svg
```

### When To Use py-spy

```text
Process is hung (100% CPU or 0% CPU doing nothing).
  -> py-spy dump shows WHERE all threads are stuck.

Process is slow.
  -> py-spy record generates a flame graph showing hot functions.

Cannot attach a debugger (production environment).
  -> py-spy is non-intrusive: no code changes, no restart needed.
  -> Works on running Python processes.
```

### Reading py-spy dump

```text
Thread 0 (idle)
  File "asyncio/base_events.py", line 603, in _run_once
  File "asyncio/events.py", line 80, in _run
  -> event loop is idle (waiting for I/O)

Thread 1 (active)
  File "orders/service.py", line 45, in process
  File "orders/db.py", line 88, in query
  -> thread is in DB query (expected for I/O-bound)

Thread 2 (stuck)
  File "orders/cache.py", line 12, in get_or_set
  File "threading.py", line 298, in wait
  -> thread is waiting on a threading.Event or Lock
```

---

## asyncio.gather Debug

```python
import asyncio

async def fetch_order(oid: str):
    await asyncio.sleep(0.1)  # simulates DB query
    return {'id': oid, 'status': 'PENDING'}

async def main():
    # All three run concurrently.
    results = await asyncio.gather(
        fetch_order('ORD-1'),
        fetch_order('ORD-2'),
        fetch_order('ORD-3'),
    )
    # <- breakpoint: all three are complete, results is a list of 3 dicts
    return results

asyncio.run(main())
```

In PyCharm Evaluate when paused:
```python
asyncio.all_tasks()   # set of all active Task objects
len(asyncio.all_tasks())  # how many tasks are running
```

---

## Interview Sound Bite

The GIL prevents parallel bytecode execution but NOT race conditions on compound operations or deadlocks from user-level locks. Use threading for I/O-bound work (GIL released during I/O), multiprocessing for CPU-bound work (separate processes bypass GIL), and asyncio for high-concurrency I/O with minimal overhead. py-spy profiles live Python processes without code changes or restart — essential for production investigations. `py-spy dump --pid` shows current call stacks of all threads, immediately identifying what's stuck. Multiprocessing workers are separate processes and require debugpy.listen() per worker to make them debuggable.
