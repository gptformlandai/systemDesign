# Runbook: Java Deadlock Investigation

## When To Use This Runbook

- Java application is stuck and not responding
- No OOM error, no crash, just a hang
- CPU is at 0% but threads are active
- jstack shows "Found one Java-level deadlock"

---

## Step 1: Confirm It Is A Deadlock (Not Starvation Or Infinite Loop)

```bash
# Get the Java process PID.
jps -l

# Quick check: is CPU near 0%?
top -pid <PID>
# Deadlock: ~0% CPU (threads are blocked, not spinning)
# Infinite loop: ~100% CPU per thread
```

---

## Step 2: Capture Thread Dump

```bash
# Option A: Using the lab script (3 dumps 10s apart).
./SCRIPTS/java-thread-dump.sh <PID>

# Option B: Single jstack.
jstack <PID> > /tmp/thread-dump.txt

# Option C: Via actuator (if Spring Boot).
curl http://localhost:8080/actuator/threaddump > /tmp/actuator-threads.json

# Option D: IntelliJ.
# Run → Dump Threads (while debugging)
```

---

## Step 3: Find The Deadlock Section

```bash
grep -A 50 "deadlock" /tmp/thread-dump.txt
```

Expected output:

```text
Found one Java-level deadlock:
=============================
"Thread-A":
  waiting to lock monitor 0xAAAA (object 0x1111, a java.lang.Object),
  which is held by "Thread-B"
"Thread-B":
  waiting to lock monitor 0xBBBB (object 0x2222, a java.lang.Object),
  which is held by "Thread-A"
```

---

## Step 4: Identify The Code

```bash
# Get the full stack trace for each deadlocked thread.
grep -A 20 '"Thread-A"' /tmp/thread-dump.txt
grep -A 20 '"Thread-B"' /tmp/thread-dump.txt
```

Look for:
- `- waiting to lock <address>` — what it wants
- `- locked <address>` — what it holds
- The Java source file and line number in the stack

---

## Step 5: Determine Lock Acquisition Order

```text
From the stack trace, identify:
  Thread A acquires: LockX first, then LockY
  Thread B acquires: LockY first, then LockX
  -> Opposite order = deadlock possible
```

---

## Step 6: Apply Fix

```java
// Fix A: Lock ordering (always acquire in the same order).
// Use System.identityHashCode() to establish consistent order.
Object first = System.identityHashCode(lockA) < System.identityHashCode(lockB) ? lockA : lockB;
Object second = (first == lockA) ? lockB : lockA;
synchronized (first) {
    synchronized (second) {
        // ...
    }
}

// Fix B: ReentrantLock with timeout.
boolean gotLock = lock.tryLock(100, TimeUnit.MILLISECONDS);
if (!gotLock) {
    // could not acquire; retry or fail fast
}

// Fix C: Reduce lock scope (avoid holding one lock while acquiring another).
```

---

## Step 7: Verify Fix

```bash
# Run the fixed application under load for several minutes.
# Take a thread dump during load to confirm no BLOCKED threads.
jstack <PID> | grep "State: BLOCKED" | wc -l
# Expected: 0
```

---

## Immediate Mitigation (If Fix Is Not Ready)

```bash
# If the application is deadlocked in production and must recover now:
# Restart the JVM process.
kill <PID>       # SIGTERM (graceful)
kill -9 <PID>    # SIGKILL (force, if graceful does not respond)
# Kubernetes: kubectl rollout restart deployment/<name>
```

---

## Prevention Checklist

- [ ] All multi-lock code acquires locks in a consistent, documented order
- [ ] Lock acquisition order is code-reviewed and commented
- [ ] `ReentrantLock.tryLock()` used with timeout for resilience
- [ ] Deadlock detection enabled in CI load tests
- [ ] Actuator thread dump endpoint enabled and protected for production
