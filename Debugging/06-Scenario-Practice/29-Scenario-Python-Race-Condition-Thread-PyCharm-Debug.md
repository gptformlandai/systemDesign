# 29. Scenario: Python Race Condition — Thread Debug With PyCharm

## Scenario Description

A Python web service uses threading to process orders concurrently. It tracks the total number of successfully processed orders in a shared counter. After running for a while, the count is always lower than expected — some increments are being lost. The bug is non-deterministic: it doesn't happen every run, and adding print statements changes the timing enough to make it appear less often.

---

## Setup: Reproduce The Bug

```python
# order_processor.py
import threading
import time
import random

# Shared state: total processed orders.
processed_count = 0

def process_order(order_id: str) -> bool:
    """Process a single order. Returns True if successful."""
    # Simulate variable processing time.
    time.sleep(random.uniform(0.001, 0.01))
    
    # Simulate ~10% failure rate.
    if random.random() < 0.1:
        return False
    
    return True

def worker(thread_id: int, orders: list[str]) -> None:
    """Worker function: processes a batch of orders."""
    global processed_count
    
    for order_id in orders:
        success = process_order(order_id)
        if success:
            # BUG: read-modify-write is NOT atomic.
            processed_count += 1  # <- race condition here
    
    print(f"Thread {thread_id}: finished batch of {len(orders)} orders")

def run_processing(total_orders: int = 1000, num_threads: int = 5) -> int:
    """Run order processing with multiple threads. Returns processed count."""
    global processed_count
    processed_count = 0  # reset
    
    # Split orders evenly across threads.
    batch_size = total_orders // num_threads
    orders_per_thread = [
        [f"ORD-{i:04d}" for i in range(t * batch_size, (t + 1) * batch_size)]
        for t in range(num_threads)
    ]
    
    threads = [
        threading.Thread(
            target=worker,
            args=(t, orders_per_thread[t]),
            name=f"order-worker-{t}"
        )
        for t in range(num_threads)
    ]
    
    for t in threads:
        t.start()
    
    for t in threads:
        t.join()
    
    return processed_count

if __name__ == '__main__':
    # Run multiple times to see inconsistency.
    for run in range(5):
        count = run_processing(total_orders=1000, num_threads=5)
        # Expected: ~900 (10% failure rate).
        # Actual: varies, often significantly less.
        print(f"Run {run + 1}: processed_count = {count}")
```

---

## Step 1: Observe Non-Determinism

```text
Run the script multiple times:
  Run 1: processed_count = 901
  Run 2: processed_count = 889
  Run 3: processed_count = 894
  Run 4: processed_count = 876
  Run 5: processed_count = 897

Expected (with ~10% failure): ~900 consistently.
Actual: varies, always lower than expected.
Race condition confirmed: increments are being lost.
```

---

## Step 2: PyCharm Debug — Set Breakpoint On The Race Line

```text
Set breakpoint on: processed_count += 1

In PyCharm: right-click the breakpoint -> Edit Breakpoint
  Suspend: Thread   <- CRITICAL: only pause the triggering thread, not all threads
```

---

## Step 3: Reproduce With Breakpoint (Thread Suspension)

```text
1. Start debug session (Ctrl+D in PyCharm).
2. When breakpoint fires:
   -> Frames panel shows: thread "order-worker-2" is paused on processed_count += 1
   -> Other threads continue running.
3. In PyCharm Evaluate Expression (Alt+F8):
   processed_count  -> 347 (current value at this moment)
   threading.current_thread().name  -> 'order-worker-2'
4. Switch to another thread in the Frames dropdown.
   -> "order-worker-0" is also on line: processed_count += 1
   -> Also about to increment from the same value 347.
5. Resume thread "order-worker-2" (right-click -> Resume).
   -> processed_count becomes 348.
6. Now resume "order-worker-0".
   -> processed_count ALSO reads 347, then writes 348.
   -> One increment was lost: both threads read 347, both wrote 348, count should be 349.
```

---

## Step 4: Why Python's GIL Doesn't Protect This

```python
# processed_count += 1 compiles to:
# 1. LOAD_GLOBAL   processed_count   -> read: 347
# 2. LOAD_CONST    1
# 3. INPLACE_ADD                     -> compute: 348
# 4. STORE_GLOBAL  processed_count   -> write: 348

# GIL can switch threads between step 1 and step 4.
# Thread A reads 347. GIL switches. Thread B reads 347. Thread B writes 348.
# GIL switches back. Thread A writes 348 (should be 349). One increment lost.
```

---

## Step 5: Log Breakpoint To Catch All Races

Instead of suspending, use a log breakpoint to observe without changing timing:

```text
Same breakpoint -> Edit -> Suspend: disabled
Evaluate and log: threading.current_thread().name + " reading " + str(processed_count)
```

Console output:

```text
order-worker-0 reading 347
order-worker-2 reading 347   <- both read same value at once
order-worker-0 reading 348
order-worker-1 reading 349
```

Two threads reading the same value = race condition confirmed.

---

## Step 6: Fix — threading.Lock

```python
# order_processor_fixed.py
import threading

processed_count = 0
count_lock = threading.Lock()  # protect the counter

def worker_fixed(thread_id: int, orders: list[str]) -> None:
    global processed_count
    
    for order_id in orders:
        success = process_order(order_id)
        if success:
            with count_lock:          # acquire lock before read-modify-write
                processed_count += 1  # now atomic relative to other threads
    
    print(f"Thread {thread_id}: finished")
```

---

## Step 7: Verify Fix

```text
Run the fixed version multiple times:
  Run 1: processed_count = 901
  Run 2: processed_count = 899
  Run 3: processed_count = 903
  Run 4: processed_count = 898
  Run 5: processed_count = 900

Consistent results around 900. Race condition eliminated.
```

---

## Alternative Fix: Thread-Local Counters (No Lock)

```python
def run_processing_threadlocal(total_orders: int, num_threads: int) -> int:
    """Avoid shared state: each thread counts locally, merge at end."""
    
    local_counts = [0] * num_threads  # each thread writes to its own index
    
    def worker_local(thread_id: int, orders: list[str]) -> None:
        count = 0
        for order_id in orders:
            if process_order(order_id):
                count += 1
        local_counts[thread_id] = count  # single write per thread, no contention
    
    threads = [
        threading.Thread(target=worker_local, args=(t, orders_per_thread[t]))
        for t in range(num_threads)
    ]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    
    return sum(local_counts)  # merge after all threads complete (serial operation)
```

No lock needed: each thread writes only to its own array slot.

---

## Key Takeaways

```text
Race condition debug checklist:
  1. Non-deterministic output (count varies across runs) -> suspect race condition.
  2. Set breakpoint on shared state modification with Suspend: Thread.
  3. Switch thread frames to see multiple threads at the same line.
  4. Check if multiple threads read the same value before writing.
  5. Use log breakpoints for timing-sensitive code (suspending breakpoints alter timing).
  6. Fix: threading.Lock, or thread-local state + merge after join.

The hardest part: adding print statements or suspending breakpoints changes
thread scheduling, which can mask the race condition. Log breakpoints are more
reliable than suspending breakpoints for race condition investigation.
```

---

## Interview Sound Bite

Python's `counter += 1` is not atomic — it compiles to LOAD_GLOBAL, INPLACE_ADD, STORE_GLOBAL, and the GIL can switch threads between any of these. Debug by setting a log breakpoint (non-suspending) on the increment line to capture which thread reads which value — concurrent reads of the same value confirm the race. Fix with `threading.Lock()` around the read-modify-write, or eliminate shared state by using thread-local counters and merging results after all threads join. Log breakpoints are better than suspending breakpoints for race conditions because they don't change thread scheduling.
