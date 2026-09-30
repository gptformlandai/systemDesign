# Module 2: Locks & AQS — The Fun, Deep-Dive Explanation

> This document explains Java's locking mechanisms like you're a junior dev who's never seen this before. We'll use analogies, ASCII diagrams, and step-by-step traces to make everything crystal clear.

---

## 📑 Table of Contents

- [Part 1: synchronized — The JVM Intrinsic Lock](#part-1-synchronized--the-jvm-intrinsic-lock)
  - [1.1 Object Layout — Where the Lock Actually Lives](#11-object-layout--where-the-lock-actually-lives)
  - [1.2 How the Mark Word Changes Based on Lock State](#12-how-the-mark-word-changes-based-on-lock-state)
  - [1.3 Lock Escalation](#13-lock-escalation)
  - [1.4 ObjectMonitor Internals](#14-objectmonitor-internals)
- [Part 2: wait(), notify(), notifyAll()](#part-2-wait-notify-notifyall)
  - [2.1 The Three Rules](#21-the-three-rules)
  - [2.2 The Canonical BoundedBuffer](#22-the-canonical-boundedbuffer)
  - [2.3 Reentrancy](#23-reentrancy)
- [Part 3: Common synchronized Bugs](#part-3-common-synchronized-bugs)
  - [3.1 Interned Strings](#31-interned-strings)
  - [3.2 Boolean.TRUE](#32-booleantrue)
  - [3.3 Mutable Lock Fields](#33-mutable-lock-fields)
- [Part 4: AbstractQueuedSynchronizer (AQS)](#part-4-abstractqueuedsynchronizer-aqs)
  - [4.1 The Purpose](#41-the-purpose)
  - [4.2 The State Variable](#42-the-state-variable)
  - [4.3 The CLH Queue](#43-the-clh-queue)
  - [4.4 Acquire/Release Flow](#44-acquirerelease-flow)
  - [4.5 Exclusive vs Shared Mode](#45-exclusive-vs-shared-mode)
- [Part 5: Explicit Locks](#part-5-explicit-locks)
  - [5.1 ReentrantLock](#51-reentrantlock)
  - [5.2 ReentrantReadWriteLock](#52-reentrantreadwritelock)
  - [5.3 Condition](#53-condition)
- [Part 6: L5-Grade Code Examples](#part-6-l5-grade-code-examples)
- [Part 7: Production War Stories](#part-7-production-war-stories)
- [Part 8: Interview Traps](#part-8-interview-traps)

---

# Part 1: `synchronized` — The JVM Intrinsic Lock

## The Big Picture: A Bathroom Analogy

Before we dive into code, let's understand what `synchronized` actually does with a simple analogy:

> Picture a **single-stall public bathroom** with a lock on the door.
> - The **door lock** = the object's monitor
> - **You entering + latching** = acquiring the monitor
> - Someone knocking outside = a thread in the **_EntryList** waiting to get in
> - You standing at the sink saying *"I'll wait until the soap is refilled"* = you called `wait()` and joined the **_WaitSet**. You *release the door* while waiting.
> - The janitor shouts *"soap refilled!"* = someone called `notifyAll()`. All sink-waiters go back to the door queue (they still have to re-acquire the lock).

Now let's see how this actually works in memory!

---

## 1.1 Object Layout — Where the Lock Actually Lives

When you write `synchronized(someObject)`, you're locking on **that object**. But have you ever wondered:

> "Where does Java actually *store* the lock information? Is there a hidden `Lock` field inside every object?"

The answer is: **Yes, kind of!** Every single Java object has a hidden "header" at the beginning of its memory layout, and part of that header is used to track lock state.

### What Every Java Object Looks Like in Memory

Imagine every Java object as a box. Before your actual data (fields), there's always a **header section**:

```
+--------------------------------------------------+
|  HEADER (hidden, managed by JVM)                 |
|  +-- Mark Word (8 bytes)  <-- LOCK INFO HERE!    |
|  +-- Klass Word (4 bytes) <-- "What class am I?" |
+--------------------------------------------------+
|  YOUR ACTUAL FIELDS                              |
|  (e.g., int age, String name, etc.)              |
+--------------------------------------------------+
```

Let me explain each part:

### Mark Word (8 bytes) — The Lock Lives Here!

This is the magic part. The Mark Word is like a **multi-purpose sticky note** attached to every object. It stores:
- Hash code (when you call `hashCode()`)
- GC age (how many garbage collections the object survived)
- **Lock state** (who owns the lock, if anyone)

The clever part: **the same 8 bytes are reused for different purposes depending on the object's state**.

### Klass Word (4 bytes) — Class Pointer

This is just a pointer that says "I'm an instance of `String`" or "I'm an instance of `MyClass`". Not related to locking.

### Padding — Alignment

Java aligns objects to 8-byte boundaries for performance. Sometimes empty bytes are added.

---

## 1.2 How the Mark Word Changes Based on Lock State

Here's where it gets interesting. The Mark Word is like a **chameleon** — it changes what it stores based on whether the object is locked or not.

Think of it like a whiteboard that gets erased and rewritten:

| Object State | What's Written on the Mark Word |
|--------------|--------------------------------|
| **Unlocked** | Hash code + GC age + a tag saying "I'm unlocked" (`01`) |
| **Lightweight Locked** | A pointer to the lock owner's stack (fast, no OS involvement) |
| **Heavyweight Locked** | A pointer to an `ObjectMonitor` (slow, involves OS) |
| **Being Garbage Collected** | GC-specific bits |

### Let's Make This Concrete with Code

```java
public class Person {
    private String name;  // your field
    private int age;      // your field
}

Person p = new Person();
synchronized (p) {
    // do something
}
```

**What happens in memory when you create `new Person()`:**

```
Memory address 0x1000:
+----------------------------------------+
| Mark Word: [hashcode | age | 01]       |  <-- "01" means UNLOCKED
| Klass Word: -> Person.class            |
| name: null                             |
| age: 0                                 |
+----------------------------------------+
```

**What happens when Thread-1 does `synchronized(p)`:**

```
Memory address 0x1000:
+----------------------------------------+
| Mark Word: [pointer to Thread-1 stack] |  <-- Now LIGHTWEIGHT LOCKED!
| Klass Word: -> Person.class            |
| name: null                             |
| age: 0                                 |
+----------------------------------------+
```

The JVM **overwrote** the hash code with a lock pointer! (Don't worry, the original data is saved on the thread's stack and restored when unlocked.)

---

## 1.3 Lock Escalation — The Three States

Remember our three states? Here's how the JVM moves between them:

```
UNLOCKED  --(1st thread, CAS)-->  LIGHTWEIGHT  --(contention)-->  HEAVYWEIGHT
    |                                  |                              |
    |                                  |                              |
 "Nobody                          "One thread                   "Multiple threads
  owns it"                         owns it,                      fighting for it,
                                   no fighting"                  need OS help"
```

### The Story of Three Threads and One Bathroom

Imagine an office with **one bathroom** (the object) and **three employees** (threads).

#### Scene 1: The Bathroom is Empty (UNLOCKED)

```
Bathroom door sign: "VACANT"

Mark Word = [hashcode | age | 01]
                              ^
                        Tag says "unlocked"
```

Nobody's using it. The Mark Word just stores the object's hash code and GC age.

#### Scene 2: Alice Needs the Bathroom (LIGHTWEIGHT LOCK)

Alice (Thread-1) walks up and tries to enter.

**What the JVM does:**

```java
// Pseudocode of what happens inside synchronized(bathroom)
// 1. Alice's thread creates a "Lock Record" on her stack
// 2. Lock Record saves the current Mark Word (backup!)
// 3. JVM attempts CAS: 
//    "If Mark Word == [hash|age|01], replace with [pointer to Alice's Lock Record]"
// 4. CAS succeeds! Alice owns the lock.
```

**Memory now looks like:**

```
Alice's Stack:                    Bathroom Object:
+-------------------+             +---------------------------+
| Lock Record       |<------------|  Mark Word: 0x7F3A...     |
| +-- saved Mark    |             |  (points to Alice's       |
| |   Word backup   |             |   Lock Record)            |
| +-- owner: Alice  |             +---------------------------+
+-------------------+
```

**Why is this fast?**
- Just ONE atomic CPU instruction (CAS)
- No operating system involvement
- No context switches
- No kernel calls

Think of it like Alice just **wrote her name on the door** with a dry-erase marker. Super quick!

#### Scene 3: Bob Arrives While Alice is Inside (CONTENTION -> INFLATION)

Bob (Thread-2) walks up. The bathroom is occupied!

**What the JVM does:**

```java
// Bob tries to acquire
// 1. Bob creates his own Lock Record
// 2. Bob attempts CAS:
//    "If Mark Word == [hash|age|01], replace with [pointer to Bob's Lock Record]"
// 3. CAS FAILS! Mark Word is not [hash|age|01], it's [pointer to Alice's stack]
// 4. JVM says: "Uh oh, we have CONTENTION!"
// 5. JVM INFLATES the lock:
//    - Allocates an ObjectMonitor in native memory
//    - Copies lock state into ObjectMonitor
//    - Updates Mark Word to point to ObjectMonitor
// 6. Bob gets PARKED (OS puts him to sleep)
```

**Memory now looks like:**

```
                                    Native Memory:
Bathroom Object:                   +-----------------------------+
+---------------------------+      | ObjectMonitor               |
| Mark Word: 0x9B2C...      |----->| +-- _owner: Alice           |
| (points to                |      | +-- _recursions: 1          |
|  ObjectMonitor)           |      | +-- _EntryList: [Bob]       |
+---------------------------+      | +-- _WaitSet: []            |
                                   +-----------------------------+
                                              |
                                              v
                                   Bob is PARKED (sleeping)
                                   waiting in _EntryList
```

**Why is this slow?**
- Memory allocation for ObjectMonitor
- OS system calls to park/unpark threads
- Context switches when threads wake up
- Cache invalidation across CPUs

Think of it like: "We now need a **proper queue management system** with a ticket machine, a waiting area, and a PA system to call the next person."

### The Performance Difference (Why This Matters)

| Operation | Lightweight Lock | Heavyweight Lock |
|-----------|-----------------|------------------|
| Acquire (no contention) | ~20 nanoseconds | ~20 nanoseconds |
| Acquire (with contention) | N/A (inflates) | ~10,000+ nanoseconds |
| Release | ~20 nanoseconds | ~1,000+ nanoseconds |
| Involves OS? | No | Yes |
| Context switch? | No | Yes |

**The takeaway:** Lightweight locks are **500x faster** than heavyweight locks under contention!

---

## 1.4 ObjectMonitor Internals — The Full Picture

When a lock gets "inflated" to heavyweight, the JVM creates an `ObjectMonitor` structure in native memory. Think of it as a **complete thread management office** with different waiting rooms.

```
        ObjectMonitor (in native memory)
        +----------------------------------------+
        |                                        |
        |  _owner       = Thread A               |  <-- Who holds the lock RIGHT NOW
        |                                        |
        |  _recursions  = 3                      |  <-- How many times owner re-entered
        |                                        |
        |  _EntryList   -> [ T2 ] -> [ T5 ]      |  <-- Threads waiting to GET IN
        |                                        |
        |  _WaitSet     -> [ T3 ] -> [ T4 ]      |  <-- Threads that called wait()
        |                                        |
        |  _cxq         -> [ ... ]               |  <-- Contention queue (impl detail)
        |                                        |
        +----------------------------------------+
```

Let me explain each field with our bathroom analogy, but upgraded to a **fancy hotel bathroom**.

### `_owner` — Who's Inside Right Now?

```
_owner = Thread A
```

This is simple: **who currently holds the lock?**

- If `_owner == null` -> bathroom is free
- If `_owner == Thread A` -> Thread A is inside

### `_recursions` — The Re-entry Counter

```
_recursions = 3
```

Remember, `synchronized` is **reentrant** — the same thread can enter the same lock multiple times!

```java
public class ReentrancyDemo {
    private final Object lock = new Object();
    
    public void methodA() {
        synchronized (lock) {       // _recursions = 1
            System.out.println("In A");
            methodB();              // Calls another synchronized method
        }                           // _recursions back to 0, lock released
    }
    
    public void methodB() {
        synchronized (lock) {       // _recursions = 2 (same thread, no blocking!)
            System.out.println("In B");
            methodC();
        }                           // _recursions back to 1
    }
    
    public void methodC() {
        synchronized (lock) {       // _recursions = 3
            System.out.println("In C");
        }                           // _recursions back to 2
    }
}
```

**Why this matters:** Without reentrancy, calling `methodB()` from `methodA()` would **deadlock** — Thread A would wait for itself forever!

### `_EntryList` — The Line Outside the Door

```
_EntryList -> [ T2 ] -> [ T5 ]
```

These are threads that want to **acquire the lock** but can't because someone else owns it.

**Analogy:** People standing in line outside the bathroom door, waiting for whoever's inside to come out.

**When do threads join `_EntryList`?**

```java
// Thread A holds the lock
synchronized (lock) {
    // doing work...
}

// Meanwhile, Thread T2 tries:
synchronized (lock) {    // BLOCKED! Goes to _EntryList
    // can't enter yet
}
```

### `_WaitSet` — The Waiting Room (for `wait()` callers)

```
_WaitSet -> [ T3 ] -> [ T4 ]
```

This is **completely different** from `_EntryList`! These are threads that:
1. **Had** the lock
2. Called `wait()`
3. **Released** the lock voluntarily
4. Are now waiting for someone to `notify()` them

**Analogy:** People who were IN the bathroom, but realized they need soap. They step out to a **waiting room** and say "Call me when the soap arrives!" They give up the bathroom while waiting.

### `_EntryList` vs `_WaitSet` — The Key Difference

| Aspect | `_EntryList` | `_WaitSet` |
|--------|-------------|-----------|
| **Who's here?** | Threads that want to acquire | Threads that HAD the lock, called `wait()` |
| **Did they ever have the lock?** | No (or released it normally) | Yes, then gave it up |
| **What are they waiting for?** | Lock to be free | Someone to call `notify()` |
| **How do they leave?** | Lock becomes available | `notify()` or `notifyAll()` |
| **After leaving?** | Become `_owner` | Move to `_EntryList`, then compete |

---

## 1.5 The `wait()` / `notify()` Dance — Step by Step

Let's trace through a real scenario:

### Setup: Producer-Consumer with One Lock

```java
public class WaitNotifyDemo {
    private final Object lock = new Object();
    private String data = null;
    
    // CONSUMER: waits for data
    public void consume() throws InterruptedException {
        synchronized (lock) {
            while (data == null) {      // Step 2: Check condition
                lock.wait();            // Step 3: Wait for data
            }
            System.out.println("Got: " + data);  // Step 6: Use data
            data = null;
        }
    }
    
    // PRODUCER: provides data
    public void produce(String value) {
        synchronized (lock) {
            data = value;               // Step 4: Set data
            lock.notify();              // Step 5: Wake up consumer
        }
    }
}
```

### The Timeline

```
TIME    CONSUMER (T1)                    PRODUCER (T2)              ObjectMonitor
----    -------------                    ------------               -------------
 |
 |      synchronized(lock)                                          _owner: T1
 |      |                                                           _EntryList: []
 |      while(data == null) YES                                     _WaitSet: []
 |      |
 |      lock.wait()  <-------------------------------------------------+
 |      |                                                              |
 |      |  1. T1 RELEASES the lock (_owner = null)                     |
 |      |  2. T1 moves to _WaitSet                                     |
 |      |  3. T1 is PARKED (sleeping)                                  |
 |      v                                                              |
 |      (sleeping in _WaitSet)                                      _owner: null
 |                                                                  _WaitSet: [T1]
 |                                       synchronized(lock)         
 |                                       |                          _owner: T2
 |                                       data = "Hello"
 |                                       |
 |                                       lock.notify() ----------------+
 |                                       |                             |
 |                                       |  1. Pick T1 from            |
 |                                       |     _WaitSet                |
 |                                       |  2. Move T1 to              |
 |                                       |     _EntryList              |
 |                                       v                             |
 |                                       } // exit synchronized     _owner: null
 |                                                                  _EntryList: [T1]
 |                                                                  _WaitSet: []
 |      
 |      +-- T1 wakes up!
 |      |   But wait... T1 must RE-ACQUIRE the lock first!
 |      |   (T1 is in _EntryList, not _owner yet)
 |      v
 |      T1 acquires lock                                            _owner: T1
 |      |                                                           _EntryList: []
 |      while(data == null) NO  <-- FALSE now!
 |      |
 |      println("Got: Hello")
 |      |
 |      } // exit synchronized                                      _owner: null
 v
```

### Critical Insight: `notify()` Does NOT Give the Lock!

This is where juniors get confused:

```java
lock.notify();  // Does NOT transfer lock ownership!
```

**What `notify()` actually does:**
1. Picks ONE thread from `_WaitSet`
2. Moves it to `_EntryList`
3. That's it! The notified thread still has to **compete** for the lock!

---

## 1.6 The Three Rules of `wait()/notify()` — Now They Make Sense!

### Rule 1: You MUST hold the lock to call `wait()` or `notify()`

```java
// WRONG - throws IllegalMonitorStateException!
lock.wait();  // Who are you? You don't own this lock!

// CORRECT
synchronized (lock) {
    lock.wait();  // OK, you own it, you can wait
}
```

**Why?** Because `wait()` needs to:
1. Release the lock (can't release what you don't have!)
2. Add you to `_WaitSet` (only the monitor can do this)

### Rule 2: Always `wait()` in a `while` loop, NEVER `if`

```java
// WRONG
synchronized (lock) {
    if (data == null) {
        lock.wait();
    }
    process(data);  // DANGER: data might still be null!
}

// CORRECT
synchronized (lock) {
    while (data == null) {
        lock.wait();
    }
    process(data);  // SAFE: we re-checked after waking
}
```

**Why?** Two reasons:

**Reason 1: Spurious Wakeups**
The OS can wake your thread for NO REASON. It's in the JLS (Java Language Specification). It really happens!

**Reason 2: Stolen Condition**
Between `notify()` and your thread re-acquiring the lock, ANOTHER thread might grab the lock and change the condition!

```
TIME    T1 (consumer)           T2 (producer)           T3 (another consumer)
----    -------------           -------------           ---------------------
 |      wait() sleeping
 |                              data = "X"
 |                              notify() -> wakes T1
 |                              releases lock
 |                                                      acquires lock!
 |                                                      data != null YES
 |                                                      takes data
 |                                                      data = null
 |                                                      releases lock
 |      wakes up
 |      acquires lock
 |      if(data==null)? 
 |      YES! It's null again!
 |      NPE if we used "if"
 |      Safe with "while" - we wait() again
```

### Rule 3: Prefer `notifyAll()` over `notify()`

```java
// RISKY
lock.notify();      // Wakes ONE arbitrary thread

// SAFER
lock.notifyAll();   // Wakes ALL threads in _WaitSet
```

**Why?** With one lock guarding multiple conditions, `notify()` might wake the WRONG thread.

---

## 1.7 The Canonical `wait/notify` Template — BoundedBuffer

This is the **classic interview question**: "Implement a thread-safe bounded buffer using `wait/notify`."

### What's a Bounded Buffer?

It's a queue with a **maximum capacity**:
- **Producers** add items (but must wait if full)
- **Consumers** take items (but must wait if empty)

```
+-----------------------------------------------------+
|                  BOUNDED BUFFER                      |
|                  (capacity = 5)                      |
|                                                      |
|   PRODUCER -->  [X][X][X][  ][  ]  --> CONSUMER     |
|                                                      |
|   "I'll wait     <-- 3 items -->      "I'll wait    |
|    if full"                            if empty"    |
+-----------------------------------------------------+
```

### The Code — Memorize This Shape!

```java
public class BoundedBuffer<T> {
    
    private final Object lock = new Object();      // The monitor
    private final Queue<T> queue = new ArrayDeque<>();
    private final int capacity;
    
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }
    
    // ================================================
    // PRODUCER: Add item to buffer
    // ================================================
    public void put(T item) throws InterruptedException {
        synchronized (lock) {
            
            // WHILE, not IF! (spurious wakeups + stolen conditions)
            while (queue.size() == capacity) {
                lock.wait();    // Buffer full -> release lock, sleep
            }
            
            queue.add(item);
            
            lock.notifyAll();   // Wake up consumers (and other producers)
        }
    }
    
    // ================================================
    // CONSUMER: Take item from buffer
    // ================================================
    public T take() throws InterruptedException {
        synchronized (lock) {
            
            // WHILE, not IF!
            while (queue.isEmpty()) {
                lock.wait();    // Buffer empty -> release lock, sleep
            }
            
            T item = queue.remove();
            
            lock.notifyAll();   // Wake up producers (and other consumers)
            
            return item;
        }
    }
}
```

### Why `notifyAll()` and not `notify()`?

With **one** lock guarding **two** conditions (full and empty), `notify()` might wake a waiting *producer* when it was a *consumer* you needed to wake, and the signal is lost.

The clean fix is using `Condition` with **two** separate wait queues (covered in Part 3).

---

## 1.8 Common Bugs — Where Devs Get Burned

### Bug 1: `wait()` Outside `synchronized`

```java
// WRONG
public void broken() throws InterruptedException {
    lock.wait();  // IllegalMonitorStateException!
}

// CORRECT
public void correct() throws InterruptedException {
    synchronized (lock) {
        lock.wait();
    }
}
```

### Bug 2: `wait()` Inside `if` Instead of `while`

```java
// WRONG
synchronized (lock) {
    if (queue.isEmpty()) {
        lock.wait();
    }
    process(queue.remove());  // Queue might still be empty!
}

// CORRECT
synchronized (lock) {
    while (queue.isEmpty()) {
        lock.wait();
    }
    process(queue.remove());  // Guaranteed not empty
}
```

### Bug 3: `notify()` with Multiple Conditions

```java
// WRONG - One lock, two conditions, using notify()
synchronized (lock) {
    while (full) lock.wait();
    // ... add item ...
    lock.notify();  // Might wake wrong type of thread!
}

// CORRECT - Use notifyAll() or separate Conditions
synchronized (lock) {
    while (full) lock.wait();
    // ... add item ...
    lock.notifyAll();  // Wake everyone, let them re-check
}
```

### Bug 4: `synchronized` on Interned Strings

This is a **sneaky production bug**:

```java
// WRONG
public void process(String userId) {
    synchronized (userId.intern()) {  // DANGER!
        // do work for this user
    }
}
```

**Why is this bad?**

`String.intern()` returns a **globally shared** string from the JVM's string pool. If two completely unrelated parts of your app both use `synchronized("admin".intern())`, they're locking on **the same object**!

```java
// In UserService.java
synchronized ("admin".intern()) { /* user logic */ }

// In CacheService.java (completely unrelated!)
synchronized ("admin".intern()) { /* cache logic */ }

// These TWO blocks are synchronized with EACH OTHER!
// Unrelated code paths now block each other!
```

**Fix:**

```java
// Use a dedicated lock object per user
private final Map<String, Object> userLocks = new ConcurrentHashMap<>();

public void process(String userId) {
    Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
    synchronized (lock) {
        // do work
    }
}
```

### Bug 5: `synchronized` on `Boolean.TRUE`

Same problem as interned strings:

```java
// WRONG
Boolean flag = Boolean.TRUE;
synchronized (flag) {
    // ...
}
```

`Boolean.TRUE` is a **singleton** shared across the entire JVM! You're locking against every other piece of code that does the same thing.

**Fix:** Never synchronize on boxed primitives or cached objects.

```java
// CORRECT
private final Object lock = new Object();
synchronized (lock) {
    // ...
}
```

### Bug 6: Locking on a Mutable Field

This is **extremely subtle**:

```java
// WRONG
public class Broken {
    private Object lock = new Object();
    
    public void method1() {
        synchronized (lock) {
            // Thread A is here
        }
    }
    
    public void setLock(Object newLock) {
        this.lock = newLock;  // Changed the lock object!
    }
    
    public void method2() {
        synchronized (lock) {
            // Thread B uses the NEW lock object
            // Thread A and B are NOT synchronized!
        }
    }
}
```

**Fix:** Make the lock `final`:

```java
// CORRECT
private final Object lock = new Object();  // Can never be reassigned
```

---

# Part 2: AQS — The Engine of `java.util.concurrent`

## 2.1 Why Does AQS Exist?

Doug Lea (the genius who wrote `java.util.concurrent`) noticed something:

> "Wait a minute... `ReentrantLock`, `Semaphore`, `CountDownLatch`, `ReadWriteLock`... they ALL do the same basic things!"

What do they all need?

| Component | What It Does |
|-----------|--------------|
| **Some state** | "Is the lock held? How many permits left? Has the latch counted down?" |
| **A queue** | "Who's waiting in line?" |
| **Atomic updates** | "Change the state without race conditions" |
| **Thread parking** | "Put waiting threads to sleep efficiently" |

So instead of writing the same code 10 times, Doug Lea created **ONE base class** that handles all the hard stuff. Subclasses just define what the "state" means.

```
+---------------------------------------------------------------------+
|                    AQS (the engine)                                  |
|                                                                      |
|   Handles: queueing, parking, waking, cancellation, fairness        |
+---------------------------------------------------------------------+
                              |
        +---------------------+---------------------+
        |                     |                     |
        v                     v                     v
+---------------+    +---------------+    +---------------+
| ReentrantLock |    |   Semaphore   |    |CountDownLatch |
|               |    |               |    |               |
| state = 0/N   |    | state = permits|   | state = count |
| (hold count)  |    | (available)   |    | (remaining)   |
+---------------+    +---------------+    +---------------+
```

---

## 2.2 The Two Things AQS Owns

AQS has just **two main components**. That's it!

```
+---------------------------------------------------------------+
|                           AQS                                  |
|                                                                |
|   +----------------------------------------------------------+ |
|   |  volatile int state    <-- THE magic number              | |
|   |                          (meaning defined by subclass)   | |
|   +----------------------------------------------------------+ |
|                                                                |
|   +----------------------------------------------------------+ |
|   |  CLH-style FIFO wait queue:                              | |
|   |                                                          | |
|   |  head --> Node(T1) --> Node(T2) --> Node(T3) <-- tail   | |
|   |           (parked)     (parked)     (parked)            | |
|   +----------------------------------------------------------+ |
|                                                                |
+---------------------------------------------------------------+
```

Let me explain each one:

### Component 1: `volatile int state` — The Magic Number

This single integer holds **all the synchronization state**. But what it MEANS depends on who's using it:

| Synchronizer | What `state` Means |
|--------------|-------------------|
| **ReentrantLock** | `0` = unlocked, `N` = locked N times by owner |
| **Semaphore** | Number of available permits |
| **CountDownLatch** | Count remaining (starts at N, goes to 0) |
| **ReentrantReadWriteLock** | High 16 bits = reader count, Low 16 bits = writer count |

**Example for ReentrantLock:**

```
state = 0  -->  "Lock is FREE, anyone can take it"
state = 1  -->  "Lock is HELD once"
state = 2  -->  "Lock is HELD twice (same thread re-entered)"
state = 3  -->  "Lock is HELD three times"
```

**Example for Semaphore(3):**

```
state = 3  -->  "3 permits available"
state = 2  -->  "2 permits available (1 acquired)"
state = 1  -->  "1 permit available (2 acquired)"
state = 0  -->  "No permits! Next thread must wait"
```

**Example for CountDownLatch(3):**

```
state = 3  -->  "3 more countDowns needed"
state = 2  -->  "2 more countDowns needed"
state = 1  -->  "1 more countDown needed"
state = 0  -->  "RELEASED! All awaiters can proceed!"
```

### Component 2: The CLH Wait Queue — The Line of Waiting Threads

When a thread can't acquire (state doesn't allow it), it joins a **queue** and goes to sleep.

```
        The CLH Queue (named after Craig, Landin, and Hagersten)
        
        head                                              tail
          |                                                 |
          v                                                 v
        +------+      +------+      +------+      +------+
        | Node | ---> | Node | ---> | Node | ---> | Node |
        | (T1) |      | (T2) |      | (T3) |      | (T4) |
        | zzz  |      | zzz  |      | zzz  |      | zzz  |
        +------+      +------+      +------+      +------+
        
        All these threads are PARKED (sleeping), waiting for their turn
```

**Key insight:** The queue is **lock-free**! Adding a new node uses CAS, not a lock. This means even under heavy contention, the queue itself never becomes a bottleneck.

---

## 2.3 What's Inside Each Node?

Each waiting thread is wrapped in a `Node` object:

```java
class Node {
    volatile int waitStatus;    // Am I cancelled? Should next node be woken?
    volatile Node prev;         // Previous node in queue
    volatile Node next;         // Next node in queue
    volatile Thread thread;     // The actual thread waiting
    Node nextWaiter;            // For Condition queues (we'll see later)
}
```

### The `waitStatus` Values

| Status | Value | Meaning |
|--------|-------|---------|
| `0` | 0 | Fresh node, just added |
| `SIGNAL` | -1 | "When I release, wake up the NEXT node" |
| `CANCELLED` | 1 | "I gave up (timeout/interrupt), skip me" |
| `CONDITION` | -2 | "I'm waiting on a Condition, not the main queue" |
| `PROPAGATE` | -3 | "Shared mode: release should propagate" |

**The most important one is `SIGNAL`:**

```
+----------+      +----------+      +----------+
|  Node A  | ---> |  Node B  | ---> |  Node C  |
| status=  |      | status=  |      | status=0 |
| SIGNAL   |      | SIGNAL   |      | (fresh)  |
+----------+      +----------+      +----------+
     |                 |
     |                 +--> "When B releases, wake C"
     +--> "When A releases, wake B"
```

**Why not just wake everyone?** Efficiency! We only wake ONE thread at a time. No thundering herd.

---

## 2.4 The Acquire Flow — This Is The Interview Answer!

When a thread calls `lock.lock()`, here's what happens inside AQS:

```
Thread T calls lock.lock()
         |
         v
    +-------------------------------------------------------------+
    |  tryAcquire(1)                                              |
    |  <-- Subclass hook: "Can I grab the state via CAS?"         |
    +-------------------------------------------------------------+
         |                              |
         | SUCCESS                      | FAIL
         | (got the lock!)              | (someone else has it)
         v                              v
      return                    +-------------------------+
      (done!)                   | Create Node for T       |
                                | Enqueue at tail via CAS |
                                +-------------------------+
                                        |
                                        v
                                +-------------------------+
                                | Is my predecessor's     |
                                | status == SIGNAL?       |
                                +-------------------------+
                                   |              |
                                   | NO           | YES
                                   v              v
                            +-----------+   +-----------+
                            | Set pred  |   | PARK(T)   |
                            | to SIGNAL |   | zzz sleep |
                            +-----------+   +-----------+
                                   |              |
                                   +------+-------+
                                          |
                                          v
                                   +-------------+
                                   | Wake up!    |
                                   | Try again   |
                                   | tryAcquire()|
                                   +-------------+
                                          |
                                   (loop until success)
```

---

## 2.5 Example: Three Threads Fighting for a ReentrantLock

Let's trace through this step by step:

**Setup:**
```java
ReentrantLock lock = new ReentrantLock();

// Thread A, B, C all try to acquire
```

### Step 1: Thread A calls `lock.lock()`

```java
// Inside AQS:
tryAcquire(1):
    if (state == 0) {                    // TRUE! state is 0
        if (CAS(state, 0, 1)) {          // SUCCESS!
            setExclusiveOwnerThread(A);  // "A owns this"
            return true;
        }
    }
    return false;
```

```
AQS State:
+-------------------------------------+
| state = 1                           |
| exclusiveOwnerThread = Thread A     |
| queue: (empty)                      |
+-------------------------------------+

Thread A: "Got it! Doing my work..."
```

### Step 2: Thread B calls `lock.lock()` while A holds it

```java
// Inside AQS:
tryAcquire(1):
    if (state == 0) {                    // FALSE! state is 1
        // ...
    }
    // Is current thread the owner? 
    if (currentThread == exclusiveOwnerThread) {  // FALSE! B != A
        // ...
    }
    return false;  // FAILED to acquire

// Since tryAcquire failed, enqueue B:
addWaiter(Node.EXCLUSIVE):
    Node node = new Node(Thread.B);
    // CAS to add at tail
```

```
AQS State:
+-------------------------------------+
| state = 1                           |
| exclusiveOwnerThread = Thread A     |
|                                     |
| queue:                              |
|   head --> [dummy] --> [Node B] <-- tail
|                         thread=B    |
|                         status=0    |
+-------------------------------------+
```

**Note:** There's always a **dummy head node**. The first real waiter is `head.next`.

Now B checks: "Should I park?"

```java
shouldParkAfterFailedAcquire(pred, node):
    // pred is the dummy head, its status is 0
    // Set pred's status to SIGNAL (-1)
    CAS(pred.status, 0, SIGNAL);
    return false;  // Don't park yet, try once more

// Loop again, tryAcquire fails again

shouldParkAfterFailedAcquire(pred, node):
    // pred.status is now SIGNAL
    return true;  // OK to park

parkAndCheckInterrupt():
    LockSupport.park(this);  // zzz B goes to sleep
```

```
AQS State:
+-------------------------------------+
| state = 1                           |
| exclusiveOwnerThread = Thread A     |
|                                     |
| queue:                              |
|   head --> [dummy] --> [Node B] <-- tail
|            status=     thread=B     |
|            SIGNAL      status=0     |
|                        zzz PARKED   |
+-------------------------------------+
```

### Step 3: Thread C also calls `lock.lock()`

Same process: tryAcquire fails, C gets enqueued after B.

```
AQS State:
+-----------------------------------------------------+
| state = 1                                           |
| exclusiveOwnerThread = Thread A                     |
|                                                     |
| queue:                                              |
|   head --> [dummy] --> [Node B] --> [Node C] <-- tail
|            status=     status=      status=0        |
|            SIGNAL      SIGNAL       zzz PARKED      |
|                        zzz PARKED                   |
+-----------------------------------------------------+
```

B's status is now SIGNAL because C set it (C needs B to wake it when B is done).

### Step 4: Thread A calls `lock.unlock()`

```java
// Inside AQS:
tryRelease(1):
    int newState = state - 1;  // 1 - 1 = 0
    if (newState == 0) {
        setExclusiveOwnerThread(null);
    }
    setState(0);
    return true;

// Release succeeded, now wake someone:
unparkSuccessor(head):
    Node s = head.next;  // That's Node B!
    LockSupport.unpark(s.thread);  // Wake up B!
```

```
AQS State:
+-----------------------------------------------------+
| state = 0  <-- UNLOCKED!                            |
| exclusiveOwnerThread = null                         |
|                                                     |
| queue:                                              |
|   head --> [dummy] --> [Node B] --> [Node C] <-- tail
|                        WAKING UP!   zzz PARKED      |
+-----------------------------------------------------+
```

### Step 5: Thread B wakes up and acquires

```java
// B wakes up from park(), continues the loop:
tryAcquire(1):
    if (state == 0) {                    // TRUE! A released
        if (CAS(state, 0, 1)) {          // SUCCESS!
            setExclusiveOwnerThread(B);
            return true;
        }
    }

// B acquired! Now B removes itself from queue:
setHead(node):
    head = node;  // B's node becomes the new dummy head
    node.thread = null;
    node.prev = null;
```

```
AQS State:
+-----------------------------------------------------+
| state = 1                                           |
| exclusiveOwnerThread = Thread B                     |
|                                                     |
| queue:                                              |
|   head --> [was B, now dummy] --> [Node C] <-- tail |
|            status=SIGNAL           zzz PARKED       |
+-----------------------------------------------------+

Thread B: "My turn! Doing my work..."
Thread C: zzz (still sleeping, waiting for B)
```

---

## 2.6 The Two Clever Bits Doug Lea Baked In

### Clever Bit 1: Lock-Free Enqueue

Adding a new waiter to the queue uses **CAS**, not a lock:

```java
// Simplified enqueue logic
Node pred = tail;
node.prev = pred;
if (CAS(tail, pred, node)) {  // Atomic swap!
    pred.next = node;
    return node;
}
```

Even if 1000 threads try to enqueue simultaneously, they don't block each other. They just retry the CAS until they succeed.

### Clever Bit 2: One Wake-Up at a Time

When releasing, AQS only wakes **ONE** thread (the head's successor). No thundering herd!

```java
// On release:
Node s = head.next;
if (s != null) {
    LockSupport.unpark(s.thread);  // Wake just ONE
}
```

Compare to `notifyAll()` which wakes EVERYONE. AQS is much more efficient.

---

## 2.7 Exclusive vs Shared Mode

AQS supports two modes:

### Exclusive Mode — Only ONE Thread

Used by: `ReentrantLock`, write lock of `ReadWriteLock`

```java
// Only one thread can hold state
acquire(1);    // Block until you're the ONE owner
release(1);    // Give up ownership
```

### Shared Mode — MULTIPLE Threads

Used by: `Semaphore`, `CountDownLatch`, read lock of `ReadWriteLock`

```java
// Multiple threads can hold state simultaneously
acquireShared(1);    // Block until there's a permit/count allows
releaseShared(1);    // Release, possibly wake multiple waiters
```

**Example: Semaphore(3)**

```
state = 3 (3 permits)

Thread A: acquireShared() --> state = 2 OK
Thread B: acquireShared() --> state = 1 OK
Thread C: acquireShared() --> state = 0 OK
Thread D: acquireShared() --> state = 0, BLOCKED! zzz

Thread A: releaseShared() --> state = 1
--> Wake Thread D!
Thread D: acquireShared() --> state = 0 OK
```

---

## 2.8 The Subclass Contract — What YOU Implement

If you want to build your own synchronizer, you extend AQS and override these methods:

| Method | Mode | What You Implement |
|--------|------|-------------------|
| `tryAcquire(int)` | Exclusive | "Can I take the lock? Return true/false" |
| `tryRelease(int)` | Exclusive | "Release the lock. Return true if fully released" |
| `tryAcquireShared(int)` | Shared | "Can I acquire? Return negative=fail, 0=success but no more, positive=success and more available" |
| `tryReleaseShared(int)` | Shared | "Release. Return true if waiters should be woken" |
| `isHeldExclusively()` | Both | "Does current thread hold exclusively? (needed for Condition)" |

**Everything else — queueing, parking, cancellation, fairness — AQS handles for you!**

---

## 2.9 Micro-Example: Build a One-Shot Latch in 15 Lines

Let's build a simple latch (like `CountDownLatch(1)`) to see AQS in action:

```java
public class OneShotLatch {
    
    private final Sync sync = new Sync();
    
    // Block until someone signals
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }
    
    // Release all waiters
    public void signal() {
        sync.releaseShared(1);
    }
    
    // The AQS subclass — just 10 lines!
    private static class Sync extends AbstractQueuedSynchronizer {
        
        @Override
        protected int tryAcquireShared(int ignored) {
            // state == 0 means "not signaled yet" --> block (return negative)
            // state == 1 means "signaled!" --> proceed (return positive)
            return getState() == 1 ? 1 : -1;
        }
        
        @Override
        protected boolean tryReleaseShared(int ignored) {
            setState(1);  // Set to "signaled"
            return true;  // Yes, wake up waiters
        }
    }
}
```

**How it works:**

```
Initial: state = 0

Thread A: await()
  --> tryAcquireShared() returns -1 (state != 1)
  --> A gets queued and parked zzz

Thread B: await()
  --> tryAcquireShared() returns -1
  --> B gets queued and parked zzz

Thread C: signal()
  --> tryReleaseShared() sets state = 1, returns true
  --> AQS wakes A and B!

Thread A: wakes, tryAcquireShared() returns 1 (state == 1) OK
Thread B: wakes, tryAcquireShared() returns 1 (state == 1) OK

Both proceed!
```

**That's it!** You just built a synchronization primitive with 10 lines of actual logic. AQS did all the hard work.

---

## 2.10 AQS Mental Model Summary

```
+---------------------------------------------------------------------+
|                           AQS                                        |
|                                                                      |
|  +------------------+    +----------------------------------------+ |
|  | volatile int     |    | CLH Queue                              | |
|  | state            |    |                                        | |
|  |                  |    | head -> [N1] -> [N2] -> [N3] <- tail  | |
|  | Meaning defined  |    |         zzz    zzz    zzz             | |
|  | by subclass!     |    |                                        | |
|  +------------------+    +----------------------------------------+ |
|                                                                      |
|  Subclass implements:     AQS handles:                              |
|  * tryAcquire()           * Queueing (lock-free CAS)               |
|  * tryRelease()           * Parking/Unparking                      |
|  * tryAcquireShared()     * Cancellation                           |
|  * tryReleaseShared()     * Fairness                               |
|  * isHeldExclusively()    * Condition queues                       |
|                                                                      |
+---------------------------------------------------------------------+
```

---

# Part 3: Explicit Locks Built on AQS

Now that you understand the engine (AQS), let's see the cars built on it!

---

## 3.1 ReentrantLock — `synchronized` with Superpowers

`ReentrantLock` has the same *semantics* as `synchronized` (reentrant, mutual exclusion), **but** with superpowers:

| Feature | `synchronized` | `ReentrantLock` |
|---------|---------------|-----------------|
| Reentrancy | Yes | Yes |
| Fairness option | No (always unfair) | Yes (`new ReentrantLock(true)`) |
| `tryLock()` | No | Yes (non-blocking + timeout variant) |
| Interruptible acquire | No | Yes (`lockInterruptibly()`) |
| Multiple wait conditions | No (one implicit) | Yes (`newCondition()` — many) |
| Explicit release control | No (scope-based) | Yes (must `unlock()` in `finally`) |

### The Golden Template — Memorize This Shape!

```java
private final ReentrantLock lock = new ReentrantLock();

lock.lock();
try {
    // critical section
} finally {
    lock.unlock();     // ALWAYS in finally; else an exception leaks the lock
}
```

**Why `finally`?** If an exception is thrown in the critical section and you don't have `finally`, the lock is NEVER released. Every other thread waits forever. Deadlock!

```java
// WRONG - Exception leaks the lock!
lock.lock();
doSomethingThatMightThrow();  // If this throws...
lock.unlock();                 // ...this never runs!

// CORRECT - Always releases
lock.lock();
try {
    doSomethingThatMightThrow();
} finally {
    lock.unlock();  // Runs even if exception thrown
}
```

---

## 3.2 Fair vs Unfair — The Barging Concept

### The Coffee Shop Analogy

Imagine a coffee shop with one barista (the lock).

**Unfair Mode (Default):**
> A new customer walks in. Instead of going to the back of the line, they shout "Hey, are you free RIGHT NOW?" If the barista happens to be between orders, the new customer gets served immediately — even though others have been waiting longer!

This is called **barging**. The new arrival tries CAS on state *before* joining the queue.

**Fair Mode:**
> A new customer walks in and goes to the back of the line. Period. No cutting, no barging.

### The Code Difference

```java
// Unfair (default) - allows barging
ReentrantLock unfairLock = new ReentrantLock();        // or new ReentrantLock(false)

// Fair - strict FIFO ordering
ReentrantLock fairLock = new ReentrantLock(true);
```

### The Performance Tradeoff

| Mode | Throughput | Fairness | Starvation Risk |
|------|------------|----------|-----------------|
| **Unfair** | HIGH (5-10x faster) | Low | Possible |
| **Fair** | LOW | High | None |

**Why is unfair faster?**

When Thread A releases the lock:
1. AQS starts waking Thread B (who's been waiting)
2. Thread B is being unparked by the OS (takes time!)
3. Meanwhile, Thread C arrives and tries `lock()`
4. **Unfair:** Thread C does CAS, succeeds, runs immediately (C is already "warm" in CPU cache)
5. **Fair:** Thread C goes to queue, waits for B to wake up

The "warm" thread (C) can do useful work while the "cold" thread (B) is being woken. This is why unfair mode is faster.

### When to Pick Fair Mode?

Only when:
1. Starvation is a **real, measured** risk
2. Fairness matters more than throughput

**Example:** A multi-tenant system where one greedy tenant could starve others.

**Default to unfair** unless you have a specific reason.

---

## 3.3 `tryLock` Patterns — Why We Use ReentrantLock Over synchronized

This is the killer feature that `synchronized` doesn't have!

### Pattern 1: Non-Blocking Attempt (Skip if Busy)

```java
if (lock.tryLock()) {
    try {
        // Do the optional work
        updateCache();
    } finally {
        lock.unlock();
    }
} else {
    // Couldn't get it; skip, log, or degrade gracefully
    log.info("Cache update skipped - lock busy");
    return staleData;
}
```

**Use case:** Optional work that's nice to do but not critical. If someone else is doing it, skip.

### Pattern 2: Timed Acquire (Bounded Backpressure)

```java
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try {
        // Critical section
        processRequest();
    } finally {
        lock.unlock();
    }
} else {
    // Waited 500ms, still couldn't get lock
    throw new TimeoutException("Request processing took too long");
}
```

**Use case:** Don't wait forever. If the lock is held too long, fail fast and let the caller handle it.

### Pattern 3: Deadlock-Safe Multi-Lock Acquire (The L5 Answer)

This is the **interview favorite**! How do you acquire two locks without risking deadlock?

**The Problem:**

```java
// Thread 1:
lockA.lock();
lockB.lock();  // Waits for B

// Thread 2:
lockB.lock();
lockA.lock();  // Waits for A

// DEADLOCK! Each thread holds one lock and waits for the other.
```

**The Solution with `tryLock`:**

```java
while (true) {
    lockA.lock();
    if (lockB.tryLock()) {
        try {
            // Got both locks! Do the work.
            transferMoney(accountA, accountB);
        } finally {
            lockB.unlock();
            lockA.unlock();
        }
        return;  // Success, exit the loop
    }
    // Couldn't get lockB, release lockA and retry
    lockA.unlock();
    Thread.yield();  // Let other threads make progress
}
```

**Why this works:**
- If we can't get both locks, we **back off** (release what we have)
- `Thread.yield()` gives other threads a chance
- Eventually, we'll get both locks

**Even better with timeout:**

```java
while (true) {
    if (lockA.tryLock(100, TimeUnit.MILLISECONDS)) {
        try {
            if (lockB.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    // Got both!
                    transferMoney(accountA, accountB);
                    return;
                } finally {
                    lockB.unlock();
                }
            }
        } finally {
            lockA.unlock();
        }
    }
    // Back off and retry
    Thread.sleep(50 + random.nextInt(50));  // Random backoff
}
```

---

## 3.4 ReentrantReadWriteLock — Readers vs Writers

### The Library Analogy

Imagine a library with one special book that everyone wants to read.

**Reading (shared access):**
> Multiple people can read the book at the same time. They just need to see it, not change it.

**Writing (exclusive access):**
> Only ONE person can write in the book at a time. And while they're writing, NO ONE can read (they might see half-written garbage).

```
+------------------------------------------------------------------+
|                    THE SPECIAL BOOK                               |
|                                                                   |
|  READERS (can be many):          WRITER (only one):              |
|  +-------+  +-------+  +-------+     +-------+                   |
|  |  R1   |  |  R2   |  |  R3   |     |   W   |                   |
|  | read  |  | read  |  | read  |     | write |                   |
|  +-------+  +-------+  +-------+     +-------+                   |
|                                                                   |
|  All can read simultaneously        Must have exclusive access   |
+------------------------------------------------------------------+
```

### How State is Packed

Remember AQS has one `int state`? ReadWriteLock cleverly packs TWO counts into it:

```
        32-bit state integer
+----------------+----------------+
|  Reader Count  |  Writer Count  |
|  (high 16 bits)|  (low 16 bits) |
+----------------+----------------+

Examples:
state = 0x00020001  -->  2 readers, 1 writer (impossible in practice!)
state = 0x00050000  -->  5 readers, 0 writers
state = 0x00000001  -->  0 readers, 1 writer
state = 0x00000000  -->  0 readers, 0 writers (unlocked)
```

### The Four Rules to Memorize

**Rule 1: Write Starvation**
With unfair mode + a busy reader stream, a writer can wait forever.

```
Time -->
Reader1: [====read====]
Reader2:    [====read====]
Reader3:       [====read====]
Reader4:          [====read====]
Writer:  waiting... waiting... waiting... (starved!)
```

**Fix:** Use `new ReentrantReadWriteLock(true)` if writers must not starve.

**Rule 2: Downgrade is Allowed**
Hold write -> acquire read -> release write -> still holding read.

```java
writeLock.lock();
try {
    // Modify data
    updateData();
    
    readLock.lock();      // Acquire read WHILE holding write (OK!)
} finally {
    writeLock.unlock();   // Release write, still holding read
}

try {
    // Now just reading, others can read too
    return data;
} finally {
    readLock.unlock();
}
```

**Rule 3: Upgrade is NOT Allowed**
Hold read -> try to acquire write -> DEADLOCK!

```java
readLock.lock();
try {
    if (needsUpdate()) {
        writeLock.lock();  // DEADLOCK! You're waiting for yourself!
        // You hold read lock
        // Write lock waits for all readers to release
        // You're a reader who will never release
        // BOOM!
    }
} finally {
    readLock.unlock();
}
```

**Why?** The write lock waits for ALL readers to release. But you're a reader who's waiting for the write lock. Circular wait = deadlock.

**Rule 4: Reentrant Reads/Writes**
Same thread can re-acquire either without blocking.

```java
readLock.lock();
readLock.lock();   // OK! Same thread, count = 2
readLock.unlock();
readLock.unlock(); // Now released
```

### The Correct Downgrade Pattern

This is a common interview question: "How do you safely update a cache?"

```java
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
private final Lock readLock = rwLock.readLock();
private final Lock writeLock = rwLock.writeLock();
private volatile Data cache;

public Data get() {
    // First, try to read
    readLock.lock();
    try {
        if (cache != null) {
            return cache;  // Cache hit! Fast path.
        }
    } finally {
        readLock.unlock();
    }
    
    // Cache miss - need to load
    writeLock.lock();
    try {
        // Double-check! Another thread might have loaded while we waited
        if (cache == null) {
            cache = loadFromDatabase();  // Expensive operation
        }
        
        // DOWNGRADE: acquire read before releasing write
        readLock.lock();
    } finally {
        writeLock.unlock();  // Release write, still holding read
    }
    
    try {
        return cache;
    } finally {
        readLock.unlock();
    }
}
```

**Why downgrade?**
- After loading, we just need to read
- By downgrading, we let other readers in immediately
- If we kept the write lock until return, we'd block readers unnecessarily

### When to Actually Use ReadWriteLock

Use it when:
1. **Read-heavy** workloads: reads:writes >= 10:1
2. **Non-trivial critical sections**: for tiny critical sections, RWLock overhead exceeds the win

**Don't use it when:**
- Reads are truly cheap -> prefer a `volatile` reference to an **immutable** snapshot
- Write ratio is high -> just use `ReentrantLock`

---

## 3.5 Condition — The Modern `wait/notify`

### Why Condition Exists

Remember the problem with `wait/notify`?

```java
// One lock, TWO conditions (full and empty)
synchronized (lock) {
    while (queue.isFull()) lock.wait();   // Producer waits
}

synchronized (lock) {
    while (queue.isEmpty()) lock.wait();  // Consumer waits
}

// Problem: notify() might wake the WRONG type of thread!
```

`Condition` solves this by giving you **separate wait queues**:

```
+------------------------------------------------------------------+
|                     ReentrantLock                                 |
|                                                                   |
|  +------------------+     +------------------+                    |
|  | Condition:       |     | Condition:       |                    |
|  | notFull          |     | notEmpty         |                    |
|  |                  |     |                  |                    |
|  | waiters:         |     | waiters:         |                    |
|  | [Producer1]      |     | [Consumer1]      |                    |
|  | [Producer2]      |     | [Consumer2]      |                    |
|  +------------------+     +------------------+                    |
|                                                                   |
|  signal() on notFull  --> wakes a PRODUCER                       |
|  signal() on notEmpty --> wakes a CONSUMER                       |
+------------------------------------------------------------------+
```

### BoundedBuffer Rewritten with Two Conditions

Compare this to the `synchronized` version:

```java
public class BoundedBuffer<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();   // Producers wait here
    private final Condition notEmpty = lock.newCondition();   // Consumers wait here
    private final Object[] items;
    private int count, putIdx, takeIdx;

    public BoundedBuffer(int capacity) {
        this.items = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        lock.lock();
        try {
            // Wait on notFull (only producers here!)
            while (count == items.length) {
                notFull.await();
            }
            
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            
            // Signal notEmpty (wake a consumer!)
            notEmpty.signal();   // signal() is safe now!
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lock();
        try {
            // Wait on notEmpty (only consumers here!)
            while (count == 0) {
                notEmpty.await();
            }
            
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            
            // Signal notFull (wake a producer!)
            notFull.signal();   // signal() is safe now!
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

**Why `signal()` is safe now:**
- `notFull.signal()` only wakes threads waiting on `notFull` (producers)
- `notEmpty.signal()` only wakes threads waiting on `notEmpty` (consumers)
- No more "wrong thread" problem!

### API Comparison: Object Monitor vs Condition

| `Object` monitor | `Condition` | Notes |
|-----------------|-------------|-------|
| `wait()` | `await()` | Same semantics |
| `wait(timeout)` | `await(time, unit)` | Better time API |
| `wait(timeout)` | `awaitNanos(nanos)` | Nanosecond precision |
| `wait(timeout)` | `awaitUntil(deadline)` | Absolute deadline |
| `notify()` | `signal()` | Wake one waiter |
| `notifyAll()` | `signalAll()` | Wake all waiters |
| — | `awaitUninterruptibly()` | Ignore interrupts |

### The Rules Still Apply!

Even with `Condition`, you must:
1. **Hold the lock** before calling `await()` or `signal()`
2. **Use `while` loops** around `await()` (spurious wakeups still happen!)

```java
// Still WRONG!
while (condition) {
    cond.await();  // Must hold lock!
}

// CORRECT
lock.lock();
try {
    while (condition) {
        cond.await();
    }
} finally {
    lock.unlock();
}
```

---

# Part 4: L5-Grade Code Examples

These are the examples that interviewers love to ask. Let's break each one down with detailed explanations.

---

## 4.1 Custom CountDownLatch from Raw `wait/notify`

This shows you understand the primitive without the AQS layer.

**The Challenge:** Build `CountDownLatch` using only `synchronized`, `wait()`, and `notify()`.

```java
public class ManualCountDownLatch {
    private final Object lock = new Object();
    private int count;

    public ManualCountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException();
        this.count = count;
    }

    /**
     * Decrements the count. When count reaches 0, wake all waiters.
     */
    public void countDown() {
        synchronized (lock) {
            if (count == 0) return;           // Already at 0, nothing to do
            if (--count == 0) {
                lock.notifyAll();             // Wake EVERYONE waiting on 0
            }
        }
    }

    /**
     * Blocks until count reaches 0.
     */
    public void await() throws InterruptedException {
        synchronized (lock) {
            while (count > 0) {               // WHILE, not IF!
                lock.wait();
            }
        }
    }

    /**
     * Blocks until count reaches 0 OR timeout expires.
     * Returns true if count reached 0, false if timed out.
     */
    public boolean await(long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        synchronized (lock) {
            while (count > 0) {
                long remainingMs = (deadline - System.nanoTime()) / 1_000_000L;
                if (remainingMs <= 0) {
                    return false;             // Timed out!
                }
                lock.wait(remainingMs);       // Wait with remaining time
            }
            return true;                      // Count reached 0
        }
    }
}
```

### What an Interviewer Will Grade

| Aspect | What They're Looking For |
|--------|-------------------------|
| `while` loop guard | Yes! Not `if`. Handles spurious wakeups. |
| `notifyAll` | Yes! Multiple threads might be waiting. |
| Remaining-time recomputation | Yes! After spurious wakeup, recalculate how much time is left. |
| Idempotent `countDown()` at 0 | Yes! Calling `countDown()` when already 0 does nothing. |
| Thread-safe | Yes! All access to `count` is inside `synchronized`. |

### How It Works — Step by Step

```
Initial: count = 3

Thread A: await()
  --> count > 0? YES (3 > 0)
  --> wait() zzz

Thread B: await()
  --> count > 0? YES (3 > 0)
  --> wait() zzz

Thread X: countDown()
  --> count = 2

Thread Y: countDown()
  --> count = 1

Thread Z: countDown()
  --> count = 0
  --> notifyAll()!

Thread A: wakes up
  --> count > 0? NO (0 > 0 is false)
  --> exits while loop, returns!

Thread B: wakes up
  --> count > 0? NO
  --> exits while loop, returns!
```

---

## 4.2 Fair Token Dispenser via ReentrantLock + Condition

**The Challenge:** Allow N concurrent workers, block the rest fairly.

This is like a `Semaphore`, but built from scratch to show you understand the pieces.

```java
public class FairTokenDispenser {
    private final ReentrantLock lock = new ReentrantLock(true);   // FAIR!
    private final Condition available = lock.newCondition();
    private int freeTokens;

    public FairTokenDispenser(int permits) {
        this.freeTokens = permits;
    }

    /**
     * Acquire a token. Blocks if none available.
     * Fair: threads are served in FIFO order.
     */
    public void acquire() throws InterruptedException {
        lock.lock();
        try {
            while (freeTokens == 0) {
                available.await();            // Wait for a token
            }
            freeTokens--;                     // Got one!
        } finally {
            lock.unlock();
        }
    }

    /**
     * Release a token. Wakes one waiting thread.
     */
    public void release() {
        lock.lock();
        try {
            freeTokens++;
            available.signal();               // Wake exactly ONE waiter
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Try to acquire without blocking.
     * Returns true if acquired, false otherwise.
     */
    public boolean tryAcquire() {
        lock.lock();
        try {
            if (freeTokens > 0) {
                freeTokens--;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
```

### Why `signal()` Instead of `signalAll()`?

With `Condition`, we have a **single-purpose wait queue**. Everyone waiting on `available` wants the same thing: a token.

- `signal()` wakes ONE waiter
- That waiter gets the token
- No wasted wakeups!

Compare to `synchronized` where we had to use `notifyAll()` because producers and consumers shared one queue.

### The Fair Lock Matters

```java
new ReentrantLock(true)   // FAIR
```

Without fairness, a "barging" thread could repeatedly steal tokens from threads that have been waiting longer. With fairness, threads are served in order.

---

## 4.3 Custom AQS-Based Non-Reentrant Mutex (The Interviewer's Favorite)

**The Challenge:** Build a simple mutex using AQS directly.

This is the **ultimate test** of whether you understand AQS.

```java
public final class Mutex {
    private final Sync sync = new Sync();

    public void lock()                { sync.acquire(1); }
    public boolean tryLock()          { return sync.tryAcquire(1); }
    public void unlock()              { sync.release(1); }
    public boolean isLocked()         { return sync.isHeldExclusively(); }
    public Condition newCondition()   { return sync.newCondition(); }

    /**
     * The AQS subclass that does the real work.
     * 
     * State meaning:
     *   0 = unlocked
     *   1 = locked
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        /**
         * Try to acquire the lock.
         * Returns true if successful, false otherwise.
         */
        @Override
        protected boolean tryAcquire(int ignored) {
            // Try to change state from 0 to 1
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * Release the lock.
         * Returns true to indicate waiters should be woken.
         */
        @Override
        protected boolean tryRelease(int ignored) {
            if (!isHeldExclusively()) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        /**
         * Create a Condition for this lock.
         */
        Condition newCondition() {
            return new ConditionObject();
        }
    }
}
```

### Talking Points to Say Aloud in an Interview

1. **`compareAndSetState(0, 1)`** handles the fast path — atomic check-and-set.

2. **AQS enqueues losers automatically** — if `tryAcquire` returns false, AQS puts the thread in the CLH queue and parks it.

3. **`isHeldExclusively()`** unlocks `Condition` support — AQS needs this to know if the current thread owns the lock.

4. **No reentrancy on purpose** — a second `lock()` from the same thread will deadlock! This is intentional for this example.

### How It Differs from ReentrantLock

| Aspect | This Mutex | ReentrantLock |
|--------|-----------|---------------|
| Reentrancy | NO (deadlocks) | YES (increments state) |
| State meaning | 0 or 1 | 0 to N (hold count) |
| Same thread re-lock | Deadlock | Allowed |

**ReentrantLock's tryAcquire would look like:**

```java
@Override
protected boolean tryAcquire(int acquires) {
    Thread current = Thread.currentThread();
    int c = getState();
    
    if (c == 0) {
        // Unlocked, try to acquire
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    } else if (current == getExclusiveOwnerThread()) {
        // Already own it, increment count (REENTRANCY!)
        int nextc = c + acquires;
        setState(nextc);
        return true;
    }
    
    return false;
}
```

---

# Part 5: Production War Stories

These are real-world bugs that have bitten production systems. Learn from others' pain!

---

## War Story 1: The `synchronized(Boolean.TRUE)` Incident

### What Happened

A caching library used this pattern:

```java
public class CacheManager {
    public Object get(Object cacheKey) {
        synchronized (cacheKey) {
            // Check cache, load if missing
            return loadOrGetFromCache(cacheKey);
        }
    }
}
```

Seems reasonable, right? Lock on the key to prevent duplicate loads.

**The Problem:** Sometimes `cacheKey` was `Boolean.TRUE`.

```java
// In UserService
cacheManager.get(Boolean.TRUE);  // "is user premium?"

// In PaymentService (completely unrelated!)
cacheManager.get(Boolean.TRUE);  // "is payment enabled?"
```

### Why It Broke

`Boolean.TRUE` is a **JVM-global singleton**. There's only ONE instance in the entire JVM.

```java
Boolean a = Boolean.TRUE;
Boolean b = Boolean.TRUE;
System.out.println(a == b);  // true! Same object!
```

So when UserService locked on `Boolean.TRUE`, it blocked PaymentService — even though they had nothing to do with each other!

### The Symptoms

- Sporadic latency spikes
- Unrelated services blocking each other
- Thread dumps showing threads waiting on the same `Boolean` object
- Extremely hard to reproduce

### The Fix

```java
// WRONG
synchronized (cacheKey) { ... }

// CORRECT - Use a dedicated lock per logical key
private final ConcurrentHashMap<Object, Object> locks = new ConcurrentHashMap<>();

public Object get(Object cacheKey) {
    Object lock = locks.computeIfAbsent(cacheKey, k -> new Object());
    synchronized (lock) {
        return loadOrGetFromCache(cacheKey);
    }
}
```

### The Lesson

**Never synchronize on:**
- `Boolean.TRUE` / `Boolean.FALSE`
- Interned strings (`"literal".intern()`)
- Boxed primitives (`Integer.valueOf(1)` for small values)
- Any object you don't control

---

## War Story 2: The Write-Starved Cache

### What Happened

A config service used `ReentrantReadWriteLock` to protect a cache:

```java
public class ConfigCache {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Map<String, String> config;
    
    public String get(String key) {
        rwLock.readLock().lock();
        try {
            return config.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void refresh() {
        rwLock.writeLock().lock();
        try {
            config = loadFromDatabase();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

### The Problem

Under sustained read traffic, the `refresh()` method **never got the write lock**.

```
Time -->
Reader1: [====read====]
Reader2:    [====read====]
Reader3:       [====read====]
Reader4:          [====read====]
Writer:  waiting... waiting... waiting... (STARVED!)
```

The default `ReentrantReadWriteLock` is **unfair**. New readers can "barge" in front of a waiting writer.

### The Symptoms

- Config TTL expired
- `refresh()` called but never completed
- Stale config served for HOURS
- Alerts firing about outdated configuration

### The Fix (Option 1: Fair Mode)

```java
// Use fair mode - writers won't starve
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
```

### The Fix (Option 2: Copy-on-Write Pattern)

```java
public class ConfigCache {
    private volatile Map<String, String> config;  // Immutable snapshot
    
    public String get(String key) {
        return config.get(key);  // No lock needed! Just read volatile reference.
    }
    
    public void refresh() {
        Map<String, String> newConfig = loadFromDatabase();
        config = Collections.unmodifiableMap(newConfig);  // Atomic swap
    }
}
```

### The Lesson

- **Unfair ReadWriteLock can starve writers** under heavy read load
- Consider **copy-on-write** for read-heavy, write-rare scenarios
- Fair mode has throughput cost — measure before using

---

## War Story 3: The Lost `notify()`

### What Happened

A team implemented producer/consumer using one monitor and `notify()`:

```java
public class MessageQueue {
    private final Object lock = new Object();
    private final Queue<Message> queue = new LinkedList<>();
    private final int capacity = 100;
    
    public void produce(Message msg) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() >= capacity) {
                lock.wait();  // Wait if full
            }
            queue.add(msg);
            lock.notify();    // Wake ONE waiter  <-- THE BUG!
        }
    }
    
    public Message consume() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();  // Wait if empty
            }
            Message msg = queue.remove();
            lock.notify();    // Wake ONE waiter  <-- THE BUG!
            return msg;
        }
    }
}
```

### The Problem

Every so often, the queue would **stall completely**.

```
Scenario:
- Queue is FULL
- Producer P1 waiting (queue full)
- Producer P2 waiting (queue full)
- Consumer C1 takes an item
- C1 calls notify()
- notify() wakes... P2! (arbitrary choice)
- P2: "Is queue full? YES! Back to wait()"
- P1 is still waiting
- C1 is done, exits
- No one is running!
- Queue has space but P1 never woke up!
```

### The Symptoms

- Queue randomly stops processing
- Producers and consumers both stuck
- Restarting the service "fixes" it temporarily
- Impossible to reproduce reliably

### The Fix (Option 1: Use notifyAll)

```java
lock.notifyAll();  // Wake everyone, let them re-check
```

### The Fix (Option 2: Two Conditions)

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notFull = lock.newCondition();
private final Condition notEmpty = lock.newCondition();

public void produce(Message msg) {
    lock.lock();
    try {
        while (queue.size() >= capacity) notFull.await();
        queue.add(msg);
        notEmpty.signal();  // Wake a CONSUMER (safe!)
    } finally {
        lock.unlock();
    }
}
```

### The Lesson

- **One lock + multiple conditions + `notify()` = lost signals**
- Use `notifyAll()` or separate `Condition` objects
- This bug is **intermittent** and hard to reproduce

---

## War Story 4: The AQS Parked-Thread Mystery

### What Happened

A service showed hundreds of threads in this state:

```
"http-worker-42" WAITING (parking)
    at sun.misc.Unsafe.park(Native Method)
    at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(...)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(...)
    at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(...)
    at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(...)
    at com.example.PaymentService.processPayment(PaymentService.java:42)
```

The team blamed AQS: "AQS is broken! Look at all these parked threads!"

### The Real Problem

```java
public class PaymentService {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void processPayment(Payment p) {
        lock.lock();
        try {
            // Validate payment
            validate(p);
            
            // THE REAL PROBLEM: HTTP call inside critical section!
            HttpResponse response = httpClient.post(
                "https://slow-payment-gateway.com/charge",
                p
            );
            
            // Process response
            handleResponse(response);
        } finally {
            lock.unlock();
        }
    }
}
```

A single external HTTP call was taking **5-10 seconds** under load. During that time, the lock was held, and ALL other threads queued up in AQS.

### The Symptoms

- Thread dump showed hundreds of threads waiting on one lock
- The lock holder was doing an HTTP call
- AQS was doing its job perfectly — the problem was the critical section

### The Fix

```java
public void processPayment(Payment p) {
    // Validate OUTSIDE the lock
    validate(p);
    
    // HTTP call OUTSIDE the lock
    HttpResponse response = httpClient.post(...);
    
    // Only lock for the actual critical section
    lock.lock();
    try {
        // Quick in-memory operations only
        updateLocalState(p, response);
    } finally {
        lock.unlock();
    }
}
```

Or use `tryLock` with timeout:

```java
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
    try {
        // ...
    } finally {
        lock.unlock();
    }
} else {
    // Fail fast, don't wait forever
    throw new ServiceUnavailableException("Payment processing busy");
}
```

### The Lesson

- **AQS is rarely the problem** — look at what's INSIDE the critical section
- **Never do I/O inside a critical section** if you can avoid it
- **Use `tryLock(timeout)`** to fail fast instead of waiting forever
- **Minimize critical section duration** — get in, do the work, get out

---

# Part 6: Interview Traps — Where L5 Candidates Fumble

These are the questions that separate "I read the docs" from "I actually understand this."

---

## The Trap Table

| Interview Question | The Bad Answer | The L5 Answer |
|-------------------|----------------|---------------|
| **"How does `synchronized` work?"** | "It's a lock." | "CAS on Mark Word for lightweight locking. If contended, JVM inflates to an `ObjectMonitor` with `_EntryList` and `_WaitSet`, using OS-level parking." |
| **"Why `while` around `wait()`?"** | "Because... best practice." | "Two reasons: (1) Spurious wakeups — the OS can wake you for no reason, it's in the JLS. (2) Lost signals — another thread might grab the lock and change the condition between your wakeup and re-check." |
| **"Why `notifyAll` over `notify`?"** | "It's safer." | "With one monitor guarding multiple conditions, `notify()` can wake the wrong waiter type, dropping the signal entirely. Better: use `Condition` with separate queues so `signal()` is safe." |
| **"Difference between `synchronized` and `ReentrantLock`?"** | "One's a keyword, one's a class." | "Same reentrancy semantics, but `ReentrantLock` adds: `tryLock()` for non-blocking acquire, timed acquire, interruptibility, fairness knob, and multiple `Condition` objects." |
| **"How does AQS work?"** | "It's a base class for locks." | "State int + FIFO CLH queue + CAS enqueue + `park`/`unpark`. Subclass defines what `state` means via `tryAcquire`/`tryRelease`. AQS handles all the queueing and parking." |
| **"Can you upgrade read lock to write lock?"** | "Sure, just acquire it." | "No! Deadlocks with yourself. You hold read, write waits for all readers, you're a reader who won't release. Only *downgrade* is safe: hold write, take read, release write." |
| **"Is fair mode better?"** | "Fairness is always better." | "Fair mode kills throughput 5-10x due to no barging. Only use when actual starvation is measured and fairness matters more than performance." |
| **"What's in the Mark Word?"** | "Lock information." | "Depends on state: unlocked = hashcode + GC age + tag bits. Lightweight locked = pointer to lock record on owner's stack. Heavyweight = pointer to ObjectMonitor." |
| **"What's the difference between `_EntryList` and `_WaitSet`?"** | "They're both queues." | "`_EntryList` = threads blocked trying to acquire. `_WaitSet` = threads that HAD the lock, called `wait()`, released it, waiting for `notify()`. After notify, they move to `_EntryList` to re-compete." |
| **"Why is `tryLock` useful?"** | "To try to get the lock." | "Three patterns: (1) Skip optional work if busy. (2) Timed acquire for bounded backpressure. (3) Deadlock-safe multi-lock acquire with back-off." |

---

## Deep Dive: How to Answer Each One

### "How does `synchronized` work?"

**Structure your answer:**

1. **Start with the Mark Word** — "Every object has a header with a Mark Word that stores lock state."

2. **Explain the states** — "Unlocked stores hashcode. Lightweight uses CAS to point to a lock record. Heavyweight inflates to ObjectMonitor."

3. **Mention the transition** — "First acquisition is lightweight (fast, no OS). Contention triggers inflation to heavyweight (slow, OS mutex)."

4. **Bonus points** — "ObjectMonitor has `_owner`, `_recursions` for reentrancy, `_EntryList` for blocked threads, `_WaitSet` for `wait()` callers."

### "Why `while` around `wait()`?"

**Give BOTH reasons:**

1. **Spurious wakeups** — "The JLS explicitly allows the OS to wake threads without `notify()`. It happens in practice."

2. **Stolen condition** — "Between `notify()` and re-acquiring the lock, another thread might change the condition. The `while` re-checks."

**Bonus:** "This is why the pattern is `while (condition) wait()` not `if (condition) wait()`."

### "How does AQS work?"

**Hit these points:**

1. **Two components** — "A `volatile int state` and a CLH FIFO queue."

2. **State meaning** — "Subclass defines it. ReentrantLock: hold count. Semaphore: permits. CountDownLatch: remaining count."

3. **The flow** — "Thread calls `tryAcquire()`. Success = done. Failure = enqueue via CAS, set predecessor to SIGNAL, park."

4. **Release** — "Update state, unpark head's successor."

5. **Why it's clever** — "Lock-free enqueue, one wakeup at a time, no thundering herd."

---

## Self-Check Questions

Before your interview, make sure you can answer these aloud:

1. What's inside an object's Mark Word, and how does it change across lock states?

2. Draw the `ObjectMonitor` structure and label `_EntryList` vs `_WaitSet`.

3. Why is `while (cond) wait()` mandatory? Name both reasons.

4. When would `notify()` beat `notifyAll()`? When is `notify()` unsafe?

5. What does the AQS `state` int mean for: `ReentrantLock`, `Semaphore`, `CountDownLatch`, `ReadWriteLock`?

6. Trace the AQS acquire flow when 3 threads contend for a `ReentrantLock`.

7. Give one production reason to pick fair mode. What does it cost?

8. Why is read-lock upgrade forbidden but downgrade allowed?

9. Write the golden `lock/try/finally/unlock` block from memory.

10. Sketch a custom AQS non-reentrant mutex in 20 lines or less.

**Any hesitation? Re-read that section!**

---

# Summary: The Complete Mental Model

```
+=========================================================================+
|                        JAVA LOCKING HIERARCHY                            |
+=========================================================================+
|                                                                          |
|  LEVEL 1: JVM Intrinsic (synchronized)                                  |
|  +--------------------------------------------------------------------+ |
|  |  Object Header                                                      | |
|  |  +-- Mark Word (lock state)                                        | |
|  |      +-- Unlocked: hashcode + age + 01                             | |
|  |      +-- Lightweight: ptr to stack lock record                     | |
|  |      +-- Heavyweight: ptr to ObjectMonitor                         | |
|  |                                                                      | |
|  |  ObjectMonitor                                                      | |
|  |  +-- _owner (current holder)                                       | |
|  |  +-- _recursions (reentry count)                                   | |
|  |  +-- _EntryList (blocked on acquire)                               | |
|  |  +-- _WaitSet (called wait())                                      | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  LEVEL 2: AQS (AbstractQueuedSynchronizer)                              |
|  +--------------------------------------------------------------------+ |
|  |  volatile int state (meaning defined by subclass)                   | |
|  |  CLH queue: head -> [Node] -> [Node] -> [Node] <- tail             | |
|  |                                                                      | |
|  |  Subclass implements:        AQS handles:                           | |
|  |  - tryAcquire/Release        - Queueing (lock-free)                | |
|  |  - tryAcquireShared/Release  - Parking/Unparking                   | |
|  |  - isHeldExclusively         - Cancellation, Fairness              | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
|  LEVEL 3: Explicit Locks (built on AQS)                                 |
|  +--------------------------------------------------------------------+ |
|  |  ReentrantLock                                                      | |
|  |  +-- Fair/Unfair modes                                             | |
|  |  +-- tryLock(), lockInterruptibly()                                | |
|  |  +-- Multiple Conditions                                           | |
|  |                                                                      | |
|  |  ReentrantReadWriteLock                                            | |
|  |  +-- Read lock (shared)                                            | |
|  |  +-- Write lock (exclusive)                                        | |
|  |  +-- Downgrade OK, Upgrade = deadlock                              | |
|  |                                                                      | |
|  |  Condition                                                          | |
|  |  +-- await/signal/signalAll                                        | |
|  |  +-- Separate wait queues per condition                            | |
|  +--------------------------------------------------------------------+ |
|                                                                          |
+=========================================================================+
```

---

# Key Takeaways

1. **Every Java object can be a lock** — the Mark Word in the object header stores lock state.

2. **Lightweight locks are fast** (~20ns), **heavyweight locks are slow** (~10,000ns+) — avoid contention!

3. **`while` around `wait()` is mandatory** — spurious wakeups and stolen conditions are real.

4. **`notifyAll()` is safer than `notify()`** — unless you have separate `Condition` queues.

5. **AQS is the engine** — one `int state` + one queue + CAS + park/unpark = all of `java.util.concurrent`.

6. **`ReentrantLock` > `synchronized`** when you need tryLock, timeout, fairness, or multiple conditions.

7. **ReadWriteLock** — great for read-heavy workloads, but watch for write starvation.

8. **Never do I/O inside a critical section** — minimize lock hold time.

9. **Fair mode costs throughput** — only use when starvation is measured.

10. **Read-lock upgrade = deadlock** — only downgrade is safe.

---

**You've made it!** You now understand Java locking from the Mark Word all the way up to production war stories. Go ace that interview! 🎯
