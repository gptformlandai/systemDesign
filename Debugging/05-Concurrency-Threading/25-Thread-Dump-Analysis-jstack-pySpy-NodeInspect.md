# 25. Thread Dump Analysis: jstack, py-spy, Node.js Inspect

## Goal

Read and interpret thread dumps for Java, Python, and Node.js — identify deadlocks, stuck threads, and performance bottlenecks from a text dump without a running debugger.

---

## Java: jstack Thread Dump

### Capturing

```bash
# Get PID.
jps -l
# 12345  com.example.OrderServiceApplication

# Dump to stdout.
jstack 12345

# Dump to file.
jstack 12345 > /tmp/thread-dump-$(date +%Y%m%d-%H%M%S).txt

# Multiple dumps 10 seconds apart (compare for deadlocks and trends).
for i in 1 2 3; do
  jstack 12345 >> /tmp/dumps.txt
  echo "=== Dump $i ===" >> /tmp/dumps.txt
  sleep 10
done

# Via kill signal (sends SIGQUIT, dumps to stdout of the JVM process).
kill -3 12345
```

---

### jstack Thread Entry Anatomy

```text
"http-nio-8080-exec-1" #23 daemon prio=5 os_prio=31 cpu=12.34ms elapsed=456.78s tid=0x00007f8b3c001b20 nid=0x3d03 waiting on condition [0x00007000036dc000]
   java.lang.Thread.State: WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x00000007c0003a40> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
        at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
        at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1074)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1134)
```

### Reading Each Field

```text
"http-nio-8080-exec-1"  -> thread name (set by thread factory or thread pool)
#23                     -> thread number
daemon                  -> daemon thread (JVM exits without waiting for daemon threads)
prio=5                  -> Java priority (1-10)
cpu=12.34ms             -> CPU time this thread has consumed
tid=0x...               -> JVM thread ID (not OS)
nid=0x3d03              -> OS thread ID (hex; convert to decimal for top/ps)
WAITING (parking)       -> thread state: waiting on a condition variable

Call stack frames:
  - Read from top (current) to bottom (entry point).
  - "locked <address>" = this thread HOLDS this monitor.
  - "waiting to lock <address>" = this thread WANTS but cannot acquire.
  - "parking to wait for <address>" = waiting on a Lock/Condition.
```

---

### Thread State Cheat Sheet

| State | Meaning | Common Cause |
|---|---|---|
| RUNNABLE | Executing OR runnable on CPU | Active processing |
| WAITING | Indefinitely waiting | Object.wait(), LockSupport.park, Thread.join |
| TIMED_WAITING | Waiting with timeout | Thread.sleep, wait(N), join(N) |
| BLOCKED | Waiting for synchronized lock | Another thread holds the monitor |
| NEW | Created but not started | Thread object created, start() not called |
| TERMINATED | Finished | Thread completed or threw unhandled exception |

---

### Deadlock Section In jstack

```text
Found one Java-level deadlock:
=============================
"order-processor-1":
  waiting to lock monitor 0x00007f8b3c001b20 (object 0x00000007c0003a40, a java.lang.Object),
  which is held by "order-processor-2"

"order-processor-2":
  waiting to lock monitor 0x00007f8b3c001c30 (object 0x00000007c0005b50, a java.lang.Object),
  which is held by "order-processor-1"

Java stack information for the threads listed above:
===================================================
"order-processor-1":
        at com.example.transfer(BankService.java:45)
        - waiting to lock <0x00000007c0003a40>
        - locked <0x00000007c0005b50>
        
"order-processor-2":
        at com.example.transfer(BankService.java:45)
        - waiting to lock <0x00000007c0005b50>
        - locked <0x00000007c0003a40>
```

---

## Python: py-spy Thread Dump

```bash
# Install.
pip install py-spy

# Dump current stacks of all threads.
py-spy dump --pid 12345

# Dump with native frames (C extension calls).
py-spy dump --native --pid 12345

# Top-like view (live update).
py-spy top --pid 12345

# Flame graph (30 second recording).
py-spy record -o /tmp/profile.svg --pid 12345 --duration 30
```

### py-spy dump Output

```text
Process 12345: python app.py
Python v3.11.4 (/usr/bin/python3.11)

Thread 140234123456 (idle)
  File "/usr/lib/python3.11/threading.py", line 320, in wait
    waiter.acquire()
  File "/usr/lib/python3.11/queue.py", line 179, in get
    self.not_empty.wait()
  File "orders/worker.py", line 45, in run_worker
    task = task_queue.get()
  -> Worker is idle, waiting for a task.

Thread 140234123457 (active)
  File "orders/service.py", line 88, in process_order
    result = db.query(sql, params)
  File "psycopg2/extensions.py", line 1
    cursor.execute(query, params)
  -> Active: inside a PostgreSQL query.
```

---

## Node.js: CPU Profile And Heap Via Chrome DevTools

```bash
# Attach Node inspector to running process.
node --inspect=9229 src/server.js

# Or send SIGUSR1 to enable inspector on running process (no restart).
kill -USR1 <node-pid>
```

```text
Open Chrome -> chrome://inspect -> click "inspect" for the Node process.

CPU Profile tab:
  "Start" -> let it run 30 seconds -> "Stop"
  Shows: flame graph of CPU time by function.
  Hot functions at top.

Memory tab:
  "Take heap snapshot" -> shows object counts by constructor.
  Compare two snapshots to find leaks.
```

---

## Clinic.js For Node.js Flame Graphs

```bash
# Install.
npm install -g clinic

# Profile with autocannon (load test).
clinic flame -- node server.js

# Or with your own workload.
clinic doctor -- node server.js
# Opens browser with analysis: event loop delay, CPU, memory.
```

---

## Three-Dump Methodology (Java)

```bash
# Take three jstack dumps 10 seconds apart.
# Compare:
#   Thread in RUNNABLE in all three: doing actual work OR spinning.
#   Thread in WAITING/BLOCKED in all three: stuck on I/O or lock.
#   Thread alternating: normal blocking I/O behavior.
```

---

## Interview Sound Bite

`jstack <PID>` is the primary Java thread dump tool — it shows all threads, their states, and "Found Java-level deadlock" sections. Read a thread entry: `BLOCKED` = waiting for a synchronized lock (potential deadlock), `WAITING (parking)` = waiting on a Condition or lock support (normal for thread pool workers waiting for tasks). Take three dumps 10 seconds apart: threads stuck in the same state across all three are truly blocked. For Python, `py-spy dump --pid` gives equivalent output with zero impact on the running process. Node.js uses Chrome DevTools' CPU profiler for flame graphs — attach via `--inspect` or `kill -USR1`.
