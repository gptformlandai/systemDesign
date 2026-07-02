# Java Runnable Mini Labs

Goal: make the Java track hands-on.

How to use:
- Create a temporary local file with the class name shown.
- Run with `javac FileName.java && java FileName`.
- For Java 21 virtual-thread labs, use a JDK 21+ runtime.
- After running, answer the debrief questions aloud.

Rule: do not just read the output. Predict it first, run it, then explain the result.

---

## Lab 1. String Pool And Concatenation

Maps to:
- `01-Starter-Path/Java-String-Deep-Dive.md`
- `05-Special-Interview-Rounds/Java-Tricky-Output-Questions-Gold-Sheet.md`

File: `StringPoolLab.java`

```java
public class StringPoolLab {
    public static void main(String[] args) {
        String literalA = "java";
        String literalB = "java";
        String heap = new String("java");

        String folded = "ja" + "va";
        String part = "ja";
        String runtime = part + "va";
        final String finalPart = "ja";
        String finalFolded = finalPart + "va";

        System.out.println(literalA == literalB);
        System.out.println(literalA == heap);
        System.out.println(literalA.equals(heap));
        System.out.println(literalA == folded);
        System.out.println(literalA == runtime);
        System.out.println(literalA == finalFolded);
        System.out.println(literalA == runtime.intern());
    }
}
```

Expected observations:
- Literals share pooled references.
- `new String()` creates a separate object.
- Compile-time constants are folded.
- Runtime concatenation creates a runtime result.
- `intern()` returns the canonical pooled string.

Debrief:
1. Why does `==` sometimes appear to work for String?
2. Why is `.equals()` the correct content comparison?
3. How would you explain this in a tricky-output round?

---

## Lab 2. Mutable Key Breaks HashMap Lookup

Maps to:
- `01-Starter-Path/Java-Core-Hot-Interview-Master-Sheet.md`
- `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md`

File: `MutableKeyHashMapLab.java`

```java
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MutableKeyHashMapLab {
    static class UserKey {
        private String email;

        UserKey(String email) {
            this.email = email;
        }

        void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof UserKey userKey)) {
                return false;
            }
            return Objects.equals(email, userKey.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(email);
        }
    }

    public static void main(String[] args) {
        Map<UserKey, String> users = new HashMap<>();
        UserKey key = new UserKey("a@example.com");

        users.put(key, "Aravind");
        System.out.println(users.get(key));

        key.setEmail("new@example.com");
        System.out.println(users.get(key));
        System.out.println(users.containsKey(new UserKey("a@example.com")));
        System.out.println(users.containsKey(new UserKey("new@example.com")));
    }
}
```

Expected observations:
- Lookup works before mutation.
- After mutation, the key may be in the wrong bucket.
- Both old and new logical lookups can fail.

Debrief:
1. Why should HashMap keys be immutable?
2. How do `equals` and `hashCode` work together?
3. How would this bug appear in production?

---

## Lab 3. Race Condition Counter

Maps to:
- `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`
- `04-Scenario-Practice/Java-Scenario-Based-Quick-Revision-Gold-Sheet.md`

File: `RaceConditionCounterLab.java`

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionCounterLab {
    static class UnsafeCounter {
        private int count;

        void increment() {
            count++;
        }

        int get() {
            return count;
        }
    }

    static class SynchronizedCounter {
        private int count;

        synchronized void increment() {
            count++;
        }

        synchronized int get() {
            return count;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threads = 20;
        int incrementsPerThread = 100_000;

        UnsafeCounter unsafeCounter = new UnsafeCounter();
        SynchronizedCounter synchronizedCounter = new SynchronizedCounter();
        AtomicInteger atomicCounter = new AtomicInteger();

        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            workers.add(new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    unsafeCounter.increment();
                    synchronizedCounter.increment();
                    atomicCounter.incrementAndGet();
                }
            }));
        }

        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        int expected = threads * incrementsPerThread;
        System.out.println("expected=" + expected);
        System.out.println("unsafe=" + unsafeCounter.get());
        System.out.println("synchronized=" + synchronizedCounter.get());
        System.out.println("atomic=" + atomicCounter.get());
    }
}
```

Expected observations:
- `unsafe` often ends below expected.
- `synchronized` and `atomic` should match expected.
- `count++` is not atomic.

Debrief:
1. Explain read-modify-write.
2. Why is `volatile` alone not enough here?
3. When would `AtomicInteger` be better than `synchronized`?
4. When would `synchronized` be better than atomics?

---

## Lab 4. Executor Queue Starvation

Maps to:
- `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md`
- `04-Scenario-Practice/Java-Scenario-Based-Quick-Revision-Gold-Sheet.md`

File: `ExecutorQueueLab.java`

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorQueueLab {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,
            2,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(3),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 1; i <= 12; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println("start task " + taskId + " on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("end task " + taskId + " on " + Thread.currentThread().getName());
            });

            System.out.println(
                "submitted=" + taskId
                    + " active=" + executor.getActiveCount()
                    + " queue=" + executor.getQueue().size()
            );
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

Expected observations:
- Active threads cap at pool size.
- Queue fills up.
- `CallerRunsPolicy` slows the submitting thread instead of silently dropping work.

Debrief:
1. Why are unbounded queues dangerous?
2. What are reasonable rejection policies?
3. What metrics would you expose for executor health?
4. How would this cause request latency?

---

## Lab 5. ConcurrentHashMap Compound Operation Trap

Maps to:
- `02-Intermediate-Backend/Java-Collections-Internals-Concurrent-Collections-FAANG-Master-Sheet.md`
- `04-Scenario-Practice/Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md`

File: `ConcurrentHashMapCompoundLab.java`

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ConcurrentHashMapCompoundLab {
    public static void main(String[] args) throws InterruptedException {
        int threads = 20;
        int iterations = 20_000;

        ConcurrentHashMap<String, Integer> unsafeCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> safeCounts = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    Integer current = unsafeCounts.get("room:R101");
                    if (current == null) {
                        unsafeCounts.put("room:R101", 1);
                    } else {
                        unsafeCounts.put("room:R101", current + 1);
                    }

                    safeCounts.merge("room:R101", 1, Integer::sum);
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        int expected = threads * iterations;
        System.out.println("expected=" + expected);
        System.out.println("unsafe get/put=" + unsafeCounts.get("room:R101"));
        System.out.println("safe merge=" + safeCounts.get("room:R101"));
    }
}
```

Expected observations:
- `ConcurrentHashMap` is thread-safe, but separate `get` and `put` are not one atomic business operation.
- `merge` makes the per-key update atomic.

Debrief:
1. Why does thread-safe collection not make compound logic safe?
2. When should `compute`, `merge`, or `putIfAbsent` be used?
3. Why is this still not a distributed consistency solution?

---

## Lab 6. Mutable Value Inside ConcurrentHashMap

Maps to:
- `04-Scenario-Practice/Java-ConcurrentHashMap-Request-Scenario-Gold-Sheet.md`
- `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

File: `ConcurrentHashMapMutableValueLab.java`

```java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ConcurrentHashMapMutableValueLab {
    public static void main(String[] args) throws InterruptedException {
        ConcurrentHashMap<String, List<Integer>> bookingsByRoom = new ConcurrentHashMap<>();
        bookingsByRoom.put("R101", new ArrayList<>());

        int threads = 10;
        int itemsPerThread = 5_000;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++) {
                    bookingsByRoom.get("R101").add(j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("expected=" + (threads * itemsPerThread));
        System.out.println("actual=" + bookingsByRoom.get("R101").size());
    }
}
```

Expected observations:
- The map is concurrent, but the stored `ArrayList` is not.
- The result can be wrong, or the program can show inconsistent behavior.

Safer variant to try:

```java
bookingsByRoom.compute("R101", (room, existing) -> {
    List<Integer> copy = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
    copy.add(j);
    return copy;
});
```

Debrief:
1. What is the difference between thread-safe map and thread-safe value?
2. What data structures or locking choices would fix this?
3. How would this bug appear in a booking service?

---

## Lab 7. Streams Laziness And Side Effects

Maps to:
- `01-Starter-Path/Java-Streams-Interview-Prep.md`
- `04-Scenario-Practice/Java-Collectors-Terminal-Operators-Gold-Sheet.md`

File: `StreamsLazinessLab.java`

```java
import java.util.List;

public class StreamsLazinessLab {
    public static void main(String[] args) {
        List<String> names = List.of("Maya", "Aravind", "Noah", "Anika");

        var pipeline = names.stream()
            .filter(name -> {
                System.out.println("filter " + name);
                return name.length() > 4;
            })
            .map(name -> {
                System.out.println("map " + name);
                return name.toUpperCase();
            });

        System.out.println("pipeline created");
        System.out.println(pipeline.toList());
    }
}
```

Expected observations:
- Nothing prints from `filter` or `map` until `toList()` runs.
- Intermediate operations are lazy.

Debrief:
1. Why are streams lazy?
2. Why is `peek` risky for business logic?
3. When would a loop be clearer?

---

## Lab 8. Collectors `toMap` Duplicate Key Trap

Maps to:
- `01-Starter-Path/Java-Streams-Collectors-End-to-End-Examples-Gold-Sheet.md`
- `04-Scenario-Practice/Java-Collectors-Terminal-Operators-Gold-Sheet.md`

File: `CollectorsToMapTrapLab.java`

```java
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectorsToMapTrapLab {
    record User(String id, String city) {}

    public static void main(String[] args) {
        List<User> users = List.of(
            new User("u1", "Dallas"),
            new User("u2", "Chicago"),
            new User("u1", "Boston")
        );

        try {
            Map<String, String> byId = users.stream()
                .collect(Collectors.toMap(User::id, User::city));
            System.out.println(byId);
        } catch (IllegalStateException exception) {
            System.out.println("duplicate key failure: " + exception.getMessage());
        }

        Map<String, String> byIdKeepingLatest = users.stream()
            .collect(Collectors.toMap(User::id, User::city, (oldValue, newValue) -> newValue));

        System.out.println(byIdKeepingLatest);
    }
}
```

Expected observations:
- `toMap` without merge function throws on duplicate keys.
- A merge function forces you to define business meaning.

Debrief:
1. Should the merge keep first, keep latest, combine, or fail?
2. Why is duplicate-key handling a business decision?
3. When is `groupingBy` better than `toMap`?

---

## Lab 9. Virtual Threads For Blocking Work

Maps to:
- `03-Senior-FAANG/Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md`

Requires: JDK 21+

File: `VirtualThreadBlockingLab.java`

```java
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VirtualThreadBlockingLab {
    public static void main(String[] args) throws Exception {
        int tasks = 1_000;
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                int taskId = i;
                futures.add(executor.submit(() -> {
                    Thread.sleep(200);
                    return "done-" + taskId;
                }));
            }

            for (Future<String> future : futures) {
                future.get();
            }
        }

        System.out.println("completed " + tasks + " blocking tasks in "
            + Duration.between(start, Instant.now()).toMillis() + "ms");
    }
}
```

Expected observations:
- Many blocking tasks can be represented with simple thread-per-task code.
- Virtual threads help when tasks spend time waiting.

Debrief:
1. Why does this not prove CPU scaling?
2. What bottleneck would a DB pool introduce?
3. What would you monitor in production?

---

## Lab 10. ThreadLocal Leak Simulation

Maps to:
- `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Virtual-Threads-Modern-Concurrency-FAANG-Master-Sheet.md`
- `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

File: `ThreadLocalLeakLab.java`

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalLeakLab {
    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        executor.submit(() -> {
            CURRENT_USER.set("user-a");
            System.out.println("request 1 user=" + CURRENT_USER.get());
        });

        executor.submit(() -> {
            System.out.println("request 2 user=" + CURRENT_USER.get());
            CURRENT_USER.remove();
        });

        executor.shutdown();
    }
}
```

Expected observations:
- With a reused platform thread, ThreadLocal data can leak into the next task if not removed.

Fix:

```java
try {
    CURRENT_USER.set("user-a");
    // work
} finally {
    CURRENT_USER.remove();
}
```

Debrief:
1. Why do thread pools make ThreadLocal cleanup important?
2. How does the risk change with virtual threads?
3. What is safer context propagation?

---

## Lab 11. Simple Memory Leak Pattern

Maps to:
- `03-Senior-FAANG/Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md`
- `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

File: `MemoryLeakPatternLab.java`

```java
import java.util.ArrayList;
import java.util.List;

public class MemoryLeakPatternLab {
    private static final List<byte[]> CACHE = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        for (int i = 1; i <= 1_000; i++) {
            CACHE.add(new byte[1024 * 1024]);
            System.out.println("cached MB=" + i);
            Thread.sleep(20);
        }
    }
}
```

Run carefully with a small heap:

```bash
javac MemoryLeakPatternLab.java
java -Xmx128m MemoryLeakPatternLab
```

Expected observations:
- Memory grows because static reachable references keep objects alive.
- GC cannot reclaim objects that remain reachable.

Debrief:
1. Why is this a leak even with garbage collection?
2. What would a heap dump show?
3. How would you fix cache growth in production?

---

## Lab 12. Deadlock And Thread Dump Practice

Maps to:
- `02-Intermediate-Backend/Java-Concurrency-Deep-Dive-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-JVM-GC-Performance-Debugging-FAANG-Master-Sheet.md`
- `05-Special-Interview-Rounds/Java-Production-Debugging-Case-Studies-Gold-Sheet.md`

File: `DeadlockThreadDumpLab.java`

```java
public class DeadlockThreadDumpLab {
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        Thread first = new Thread(() -> {
            synchronized (LOCK_A) {
                sleep(100);
                synchronized (LOCK_B) {
                    System.out.println("first acquired both locks");
                }
            }
        }, "booking-worker-1");

        Thread second = new Thread(() -> {
            synchronized (LOCK_B) {
                sleep(100);
                synchronized (LOCK_A) {
                    System.out.println("second acquired both locks");
                }
            }
        }, "booking-worker-2");

        first.start();
        second.start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
```

Thread dump practice:

```bash
javac DeadlockThreadDumpLab.java
java DeadlockThreadDumpLab
jcmd <pid> Thread.print
```

Expected observations:
- Program hangs.
- Thread dump shows threads waiting on each other's locks.

Debrief:
1. What are the four deadlock conditions?
2. How would fixed lock ordering prevent this?
3. How do you explain a deadlock from a thread dump?

---

## Lab 13. Naive Timing vs Warmup Awareness

Maps to:
- `03-Senior-FAANG/Java-Platform-Tooling-Testing-Security-FAANG-Master-Sheet.md`
- `03-Senior-FAANG/Java-Testing-Patterns-Best-Practices-Gold-Sheet.md`

File: `NaiveBenchmarkTrapLab.java`

```java
public class NaiveBenchmarkTrapLab {
    public static void main(String[] args) {
        for (int round = 1; round <= 5; round++) {
            long start = System.nanoTime();
            long result = work();
            long elapsed = System.nanoTime() - start;
            System.out.println("round=" + round + " result=" + result + " nanos=" + elapsed);
        }
    }

    private static long work() {
        long sum = 0;
        for (int i = 0; i < 50_000_000; i++) {
            sum += i;
        }
        return sum;
    }
}
```

Expected observations:
- Timings may change between rounds due to warmup, JIT, CPU state, and measurement noise.
- This is not a substitute for JMH.

Debrief:
1. Why are naive microbenchmarks misleading?
2. What does JMH handle for you?
3. When is high-level load testing more useful than microbenchmarking?

---

## Lab 14. Serialization Trap

Maps to:
- `02-Intermediate-Backend/Java-IO-NIO-Serialization-FAANG-Master-Sheet.md`
- `05-Special-Interview-Rounds/Java-Tricky-Output-Questions-Gold-Sheet.md`

File: `SerializationTrapLab.java`

```java
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SerializationTrapLab {
    static class Session implements Serializable {
        private static final long serialVersionUID = 1L;
        static String serviceName = "booking-service";
        String userId;
        transient String token;

        Session(String userId, String token) {
            System.out.println("constructor called");
            this.userId = userId;
            this.token = token;
        }
    }

    public static void main(String[] args) throws Exception {
        Session original = new Session("u1", "secret-token");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        Session.serviceName = "changed-service";

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Session restored = (Session) in.readObject();
            System.out.println(restored.userId);
            System.out.println(restored.token);
            System.out.println(Session.serviceName);
        }
    }
}
```

Expected observations:
- Constructor is not called during deserialization.
- `transient` field is not restored.
- `static` field belongs to the class, not serialized object state.

Debrief:
1. Why is Java serialization dangerous with untrusted data?
2. What does `serialVersionUID` protect against?
3. What should modern systems use instead?

---

## Lab 15. Small Machine-Coding Drill: In-Memory Booking

Maps to:
- `05-Special-Interview-Rounds/Java-LLD-Machine-Coding-Patterns-Gold-Sheet.md`
- `04-Scenario-Practice/Java-Intervue-Round-2-Concurrency-Streams-Booking-Scenario-Gold-Sheet.md`

Time box: 90 minutes.

Problem:
Build an in-memory room booking service.

Requirements:
- Add rooms.
- Book a room for a date range.
- Reject overlapping bookings for the same room.
- List bookings by room.
- Cancel booking by booking id.
- Keep models valid at construction time.
- Keep service logic separate from storage.
- Add at least five demo cases in `main`.

Suggested package shape:

```text
model: Room, Booking
repository: BookingRepository, InMemoryBookingRepository
service: BookingService
exception: BookingException
main: BookingDemo
```

Thread-safety extension:
- Make booking creation safe when multiple threads try the same room and date range.
- Explain whether your solution is safe in one JVM or across multiple service instances.

Debrief:
1. What entities did you model?
2. Which invariants live in constructors?
3. Which invariants live in the service?
4. What data structure did you choose and why?
5. How would you persist this in a real backend?
6. How would database constraints change the design?

---

## Lab 16. Java Environment Check

Maps to:
- `00-Setup/Java-JDK-CLI-IDE-Maven-Gradle-Gold-Sheet.md`

File: `JavaEnvironmentCheckLab.java`

```java
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JavaEnvironmentCheckLab {
    public static void main(String[] args) {
        List<String> properties = List.of(
            "java.version",
            "java.vendor",
            "java.home",
            "java.class.path",
            "user.dir",
            "os.name",
            "os.arch"
        );

        for (String property : properties) {
            System.out.printf("%-16s = %s%n", property, System.getProperty(property));
        }

        System.out.println("pom.xml exists      = " + Files.exists(Path.of("pom.xml")));
        System.out.println("build.gradle exists = " + Files.exists(Path.of("build.gradle")));
        System.out.println("mvnw exists         = " + Files.exists(Path.of("mvnw")));
        System.out.println("gradlew exists      = " + Files.exists(Path.of("gradlew")));
    }
}
```

Expected observations:
- The runtime JDK version is visible from `java.version`.
- `java.home` shows the actual runtime path.
- Project build files tell you whether Maven or Gradle should be the source of truth.

Debrief:
1. Does terminal Java match your IDE project SDK?
2. What command should CI run for this project?
3. What causes `UnsupportedClassVersionError`?

---

## Lab 17. Connection Pool Pressure Simulation

Maps to:
- `02-Intermediate-Backend/Java-JDBC-Transactions-Connection-Pooling-Gold-Sheet.md`
- `03-Senior-FAANG/Java-Production-Engineering-Best-Practices-FAANG-Master-Sheet.md`

File: `ConnectionPoolPressureLab.java`

```java
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ConnectionPoolPressureLab {
    private static final Semaphore POOL = new Semaphore(3);

    public static void main(String[] args) throws InterruptedException {
        List<Thread> workers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            int requestId = i;
            workers.add(new Thread(() -> handleRequest(requestId), "request-" + requestId));
        }

        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
    }

    private static void handleRequest(int requestId) {
        Instant start = Instant.now();
        try {
            POOL.acquire();
            long waitMillis = Duration.between(start, Instant.now()).toMillis();
            System.out.println("request=" + requestId + " acquired after " + waitMillis + "ms");
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            POOL.release();
        }
    }
}
```

Expected observations:
- Only three requests can hold the simulated connection at once.
- Later requests wait even though many Java threads exist.
- More threads do not remove downstream capacity limits.

Debrief:
1. How is the semaphore like a connection pool?
2. Why can virtual threads still wait on DB connections?
3. What would Hikari pending/active metrics show?
4. Why is blindly increasing the pool risky?

---

## Lab 18. Data Contract Evolution

Maps to:
- `03-Senior-FAANG/Java-Data-Formats-Jackson-Protobuf-Serialization-Gold-Sheet.md`

File: `DataContractEvolutionLab.java`

```java
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class DataContractEvolutionLab {
    record BookingEvent(String bookingId, String roomId, BigDecimal amount, String currency, Instant createdAt) {
    }

    public static void main(String[] args) {
        Map<String, String> v1Payload = Map.of(
            "booking_id", "B1",
            "room_id", "R1",
            "amount", "120.00",
            "created_at", "2026-07-02T10:15:30Z"
        );

        Map<String, String> v2Payload = Map.of(
            "booking_id", "B2",
            "room_id", "R2",
            "amount", "145.50",
            "currency", "USD",
            "created_at", "2026-07-02T10:16:30Z",
            "trace_id", "ignored-by-old-consumer"
        );

        System.out.println(parse(v1Payload));
        System.out.println(parse(v2Payload));
    }

    private static BookingEvent parse(Map<String, String> payload) {
        return new BookingEvent(
            payload.get("booking_id"),
            payload.get("room_id"),
            new BigDecimal(payload.get("amount")),
            payload.getOrDefault("currency", "USD"),
            Instant.parse(payload.get("created_at"))
        );
    }
}
```

Expected observations:
- Additive fields are easier to handle than breaking renames.
- Unknown fields can be ignored.
- Missing fields need explicit defaults or validation.
- Money uses `BigDecimal`, not `double`.
- Timestamp uses `Instant`.

Debrief:
1. What would break if `booking_id` were renamed?
2. Why is `currency` additive and safer?
3. How would this map to JSON/Jackson?
4. How would Protobuf field numbers change the compatibility rules?

---

## Lab 19. Annotation Retention Reflection

Maps to:
- `05-Special-Interview-Rounds/Java-Annotation-Processing-Code-Generation-Gold-Sheet.md`
- `05-Special-Interview-Rounds/Java-Generics-Reflection-Annotations-Deep-Dive-Gold-Sheet.md`

File: `AnnotationRetentionLab.java`

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class AnnotationRetentionLab {
    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface SourceOnly {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface RuntimeVisible {
        String value();
    }

    @SourceOnly
    @RuntimeVisible("booking-service")
    static class BookingService {
    }

    public static void main(String[] args) {
        System.out.println(BookingService.class.getAnnotation(SourceOnly.class));
        RuntimeVisible runtimeVisible = BookingService.class.getAnnotation(RuntimeVisible.class);
        System.out.println(runtimeVisible.value());
    }
}
```

Expected observations:
- `SourceOnly` is not visible through runtime reflection.
- `RuntimeVisible` is visible and its value can be read.

Debrief:
1. Why does retention policy matter?
2. Which retention would an annotation processor often use?
3. Which retention does a runtime framework need?
4. How is annotation processing different from reflection?

---

## Lab Completion Tracker

| Lab | Done | Redo Needed | Notes |
|---:|---|---|---|
| 1 |  |  |  |
| 2 |  |  |  |
| 3 |  |  |  |
| 4 |  |  |  |
| 5 |  |  |  |
| 6 |  |  |  |
| 7 |  |  |  |
| 8 |  |  |  |
| 9 |  |  |  |
| 10 |  |  |  |
| 11 |  |  |  |
| 12 |  |  |  |
| 13 |  |  |  |
| 14 |  |  |  |
| 15 |  |  |  |
| 16 |  |  |  |
| 17 |  |  |  |
| 18 |  |  |  |
| 19 |  |  |  |
