# Concurrency and Thread Safety - Mentorship Track

> Goal: build practical LLD concurrency intuition with examples that make race conditions, locks, CAS, pools, and producer-consumer flow concrete instead of mysterious.

---

## How We Will Use This Sheet

- We will keep this sheet focused on `2.4 Concurrency & Thread Safety`.
- We will learn each concept through the same lens: what can go wrong, what protects correctness, and what trade-off the protection creates.
- We will include Java examples because Java exposes production-grade concurrency primitives clearly.
- We will include Python examples for compact runnable simulations and mental clarity.
- We will call out Python runtime caveats where CPython's Global Interpreter Lock affects examples.
- Code comments marked `Concurrency concept` show exactly where the thread-safety idea is applied.

---

## Roadmap for This Sheet

1. Thread safety principles
2. Optimistic vs pessimistic locking
3. Atomic operations and CAS intuition
4. Thread pools
5. Producer-consumer pattern

---

## Concurrency Decision Map

| If the problem is... | Think about... | Main question |
|---|---|---|
| Multiple threads read and write shared state | Thread safety principles | What state is shared, and who can mutate it? |
| A record may be updated by competing requests | Optimistic or pessimistic locking | Should we prevent conflict upfront or detect conflict later? |
| A small numeric/reference update must be fast and safe | Atomic operations / CAS | Can one atomic compare-and-set replace a broader lock? |
| Many tasks need controlled execution | Thread pool | How many workers should run, and what happens when the queue fills? |
| Producers generate work and consumers process it asynchronously | Producer-consumer | How do we buffer, apply backpressure, and shut down safely? |

---

## Confusion Map

| Common confusion | Clear distinction |
|---|---|
| Concurrency vs parallelism | Concurrency is managing many tasks in overlapping time. Parallelism is executing multiple tasks at the same instant. |
| Thread-safe vs fast | Thread-safe means correct under concurrent access. It may be slower because coordination has cost. |
| Atomicity vs visibility | Atomicity means an operation is indivisible. Visibility means one thread can see another thread's writes. |
| `volatile` vs atomic in Java | `volatile` helps visibility for a variable. Atomic classes provide safe compound updates like increment and compare-and-set. |
| Lock vs synchronized | `synchronized` is Java's built-in monitor lock. `Lock` gives richer control such as try-lock and interruptible locking. |
| Optimistic vs pessimistic locking | Optimistic detects conflicts later. Pessimistic blocks competing writers earlier. |
| CAS vs lock | CAS retries small updates without blocking. Locks protect larger critical sections. |
| Thread pool vs queue | Pool executes tasks using workers. Queue stores waiting tasks. A thread pool usually has both. |
| Producer-consumer vs pub/sub | Producer-consumer distributes work items to consumers. Pub/sub broadcasts events to subscribers. |
| Python GIL vs thread safety | The GIL does not make business-level shared mutable state safe. Use locks or queues for shared invariants. |

---

## Code Example Convention

- Section 14 uses Java for real concurrency primitives such as `synchronized`, `ReentrantLock`, `AtomicInteger`, `ExecutorService`, and `BlockingQueue`.
- Section 15 uses Python for compact simulations using `threading`, `queue`, locks, and `ThreadPoolExecutor`.
- Python examples are concept demonstrations, not claims that Python threads are always the best CPU-parallel performance tool.
- Examples use hotel booking, inventory, payment, and notification domains to keep the mental model consistent.

---

# Topic 1: Thread Safety Principles

> Track: 2.4 Concurrency & Thread Safety
> Scope: shared mutable state, race conditions, visibility, critical sections, immutability, confinement, synchronization, and safe publication

---

## 1. Intuition

Imagine two front-desk agents trying to book the last available hotel room at the same time.

If both agents read `1 room left` before either writes the update, both may confirm the booking. The system sold one room twice.

Thread safety is the discipline of making code correct when multiple threads touch the same state.

Short memory trick:
- shared mutable state is the danger zone
- protect the read-modify-write sequence
- prefer no sharing, immutability, or synchronization

---

## 2. Definition

- Definition: Thread safety means code behaves correctly when accessed by multiple threads at the same time.
- Category: Concurrency correctness principle
- Core idea: Prevent races by controlling access to shared mutable state and making memory visibility explicit.

Thread-safety tools include:
- immutability
- local variables and thread confinement
- synchronized blocks
- locks
- atomic variables
- concurrent collections
- safe publication

---

## 3. Why It Exists

Modern services handle many requests concurrently.

Without thread-safety principles, bugs appear as:
- lost updates
- duplicate bookings
- negative inventory
- stale reads
- inconsistent cache state
- partially initialized objects becoming visible
- intermittent failures that disappear during debugging

Thread safety exists because the line `rooms = rooms - 1` is not one indivisible action. It is read, compute, and write.

If another thread interleaves during that sequence, correctness breaks.

---

## 4. Reality

Thread safety matters in:

- web service singleton beans
- shared in-memory caches
- counters and rate limiters
- inventory reservation
- payment idempotency stores
- background workers
- batch jobs
- connection pools
- metrics aggregation

Common Java reality:
- Spring services are often singleton scoped.
- Stateless services are usually safe.
- Mutable fields inside singleton services are risky unless protected.

Interview maturity:
- first identify shared mutable state
- then choose the simplest correctness mechanism

---

## 5. How It Works

Thread-safe design flow:

1. Identify state.
2. Ask whether the state is shared across threads.
3. Ask whether the state is mutable.
4. If not shared or not mutable, risk is lower.
5. If shared and mutable, protect access.
6. Keep critical sections small.
7. Make object publication safe.
8. Test with concurrent scenarios when risk is high.

Core danger pattern:

```text
Thread A reads rooms = 1
Thread B reads rooms = 1
Thread A writes rooms = 0 and confirms booking
Thread B writes rooms = 0 and confirms booking
Result: two confirmations, one room
```

Thread-safe version:

```text
Thread A enters lock
Thread A reads rooms = 1, writes rooms = 0, confirms
Thread A exits lock
Thread B enters lock
Thread B reads rooms = 0, rejects
```

---

## 6. What Problem It Solves

- Primary problem solved: incorrect behavior caused by concurrent access to shared mutable state.
- Secondary benefits: predictable invariants, safer singleton services, fewer intermittent production bugs.
- Systems impact: prevents correctness failures in high-concurrency request handling.

Thread safety protects invariants like:
- inventory cannot go negative
- booking id cannot be generated twice
- payment should not be captured twice
- counter should not lose increments

---

## 7. When to Rely on It

Think thread-safety when:

- multiple requests can touch the same object
- object has mutable fields
- in-memory state is shared
- singleton service stores data
- code does read-modify-write
- correctness depends on a value not changing mid-operation

Interviewer keywords:
- concurrent requests
- multiple threads
- race condition
- shared cache
- inventory count
- duplicate update
- thread-safe singleton service

---

## 8. When Not to Use Heavy Synchronization

Avoid locks when:

- data is request-local
- object is immutable
- state is owned by one worker thread
- database transaction already provides the needed isolation
- atomic primitive is enough
- lock would guard slow network I/O unnecessarily

Prefer simpler approaches:
- make objects immutable
- pass data through method parameters
- keep services stateless
- use concurrent collections where appropriate
- use database constraints for cross-process correctness

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects correctness under concurrency | Coordination adds overhead |
| Prevents lost updates and invariant breaks | Bad locks can deadlock |
| Makes shared state predictable | Over-locking reduces throughput |
| Enables safe singleton components | Bugs can still be hard to reproduce |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: correctness and invariant protection.
- Give up: some parallelism when threads wait.
- Latency impact: lock contention can increase tail latency.
- Throughput impact: smaller critical sections improve throughput.

### Common Mistakes

- Mistake: assuming a singleton service is thread-safe because it has one instance.
- Why it is wrong: one shared mutable instance can be accessed by many threads.
- Better approach: keep singleton services stateless or protect mutable fields.

- Mistake: locking around network calls.
- Why it is wrong: one slow dependency blocks all threads needing that lock.
- Better approach: lock only the state change, not the external call.

- Mistake: using non-thread-safe collections from multiple threads.
- Why it is wrong: internal collection state can corrupt or behave unpredictably.
- Better approach: use synchronization or concurrent collections.

- Mistake: checking then acting without a lock.
- Why it is wrong: another thread can change state between check and action.
- Better approach: make check-and-update one critical section.

---

## 11. Key Numbers

Concurrency heuristics:

- Read-modify-write on shared state: unsafe unless protected.
- Critical section target: keep it as short as possible.
- Lock contention: watch p95 and p99 latency, not only average latency.
- Singleton service with mutable fields: treat as a design smell until justified.
- One process lock does not protect multiple service instances.

Memory number:
- If two threads can touch it and one can mutate it, you need a safety story.

---

## 12. Failure Modes

- Lost update: increments or inventory decrements disappear.
- Double booking: two requests reserve the same unit.
- Stale read: one thread sees old value.
- Deadlock: two threads wait forever on each other's locks.
- Race-dependent tests: failures only occur sometimes.
- Cross-process gap: in-memory lock works in one JVM but not across multiple service instances.

Mitigations:
- reduce shared mutable state
- use locks or atomics for in-process shared state
- use database constraints/transactions for cross-process state
- avoid nested locks where possible
- add concurrency tests for critical invariants

---

## 13. Scenario

- Product / system: Hotel inventory service
- Requirement: prevent two concurrent requests from reserving the last available room
- Good design: protect check-and-decrement as one critical section
- Why this concept fits: inventory count is shared mutable state
- What would go wrong without it: two threads could both see one room available and both confirm

---

## 14. Java Code Sample

### Thread-safe inventory reservation with synchronized

```java
class RoomInventory {
	private int availableRooms;

	RoomInventory(int availableRooms) {
		this.availableRooms = availableRooms;
	}

	public synchronized boolean reserveOneRoom() {
		// Concurrency concept: synchronized makes check-and-decrement one critical section.
		if (availableRooms <= 0) {
			return false;
		}
		availableRooms--;
		return true;
	}

	public synchronized int availableRooms() {
		// Concurrency concept: synchronized read gives visibility of updates guarded by the same monitor.
		return availableRooms;
	}
}

class InventoryDemo {
	public static void main(String[] args) throws InterruptedException {
		RoomInventory inventory = new RoomInventory(1);

		Runnable task = () -> {
			boolean reserved = inventory.reserveOneRoom();
			System.out.println(Thread.currentThread().getName() + " reserved=" + reserved);
		};

		Thread first = new Thread(task, "agent-1");
		Thread second = new Thread(task, "agent-2");

		first.start();
		second.start();
		first.join();
		second.join();

		System.out.println("remaining=" + inventory.availableRooms());
	}
}
```

Key idea:
- the dangerous operation is not reading inventory or writing inventory alone; it is the whole check-then-update sequence

---

## 15. Python Mini Program / Simulation

This mini program shows the same idea using a lock. In CPython, the GIL does not make compound business operations automatically safe.

```python
import threading


class RoomInventory:
	def __init__(self, available_rooms: int) -> None:
		self.available_rooms = available_rooms
		self._lock = threading.Lock()

	def reserve_one_room(self) -> bool:
		# Concurrency concept: lock protects the whole check-and-update critical section.
		with self._lock:
			if self.available_rooms <= 0:
				return False
			self.available_rooms -= 1
			return True


def main() -> None:
	inventory = RoomInventory(1)

	def worker(name: str) -> None:
		print(name, inventory.reserve_one_room())

	threads = [threading.Thread(target=worker, args=(f"agent-{index}",)) for index in range(2)]
	for thread in threads:
		thread.start()
	for thread in threads:
		thread.join()

	print("remaining", inventory.available_rooms)


if __name__ == "__main__":
	main()
```

What this demonstrates:
- shared mutable state needs explicit protection
- the GIL is not a replacement for business-level synchronization
- locking makes the invariant easy to reason about

---

## 16. Practical Question

> Two users try to reserve the last room at the same time. How do you prevent double booking in an LLD design?

---

## 17. Strong Answer

I would first identify the shared mutable state: available room count or reservation status. The key operation is check-and-update, so it must be atomic from the business perspective.

Inside one process, I can protect it with a lock, synchronized method, atomic primitive, or concurrent data structure depending on the operation. But in a real distributed service with multiple instances, an in-memory lock is not enough. I would rely on database constraints, transactions, conditional updates, or distributed coordination depending on the architecture.

For LLD, I would keep services mostly stateless and put synchronization only around the critical state transition.

---

## 18. Revision Notes

- One-line summary: Thread safety protects shared mutable state from incorrect concurrent interleavings.
- Three keywords: shared state, critical section, visibility
- One interview trap: assuming singleton means safe.
- One memory trick: if read-check-write must stay together, protect it together.

---

# Topic 2: Optimistic vs Pessimistic Locking

> Track: 2.4 Concurrency & Thread Safety
> Scope: conflict prevention, conflict detection, version columns, row locks, transactions, stale updates, and booking inventory correctness

---

## 1. Intuition

Imagine two hotel agents editing the same booking.

Pessimistic locking says:
- "I expect conflict, so I will lock the booking while I edit it. Others must wait."

Optimistic locking says:
- "I expect conflict to be rare, so I will edit without blocking. Before saving, I will check whether someone else changed it."

Short memory trick:
- pessimistic: block first
- optimistic: check version later

---

## 2. Definition

- Definition: Pessimistic locking prevents conflicts by locking data before update. Optimistic locking detects conflicts at update time using a version, timestamp, or compare condition.
- Category: Concurrency control strategy
- Core idea: Choose between waiting upfront and retrying on conflict.

Pessimistic locking tools:
- database row locks
- `SELECT ... FOR UPDATE`
- mutexes
- `ReentrantLock`

Optimistic locking tools:
- version column
- conditional update
- compare-and-set
- ETag / If-Match headers

---

## 3. Why It Exists

Concurrent updates can overwrite each other.

Example lost update:
1. Agent A reads booking version 7.
2. Agent B reads booking version 7.
3. Agent A changes room and saves version 8.
4. Agent B changes dates and saves version 8 based on stale data.
5. Agent A's change may be lost.

Locking strategies exist to protect updates from this kind of conflict.

The design question is not "which one is always better?"

The design question is:
- are conflicts common and expensive?
- can users retry?
- can we afford blocking?

---

## 4. Reality

Optimistic locking appears in:

- booking updates with version columns
- REST APIs with ETags
- document edits
- inventory conditional updates
- JPA `@Version`
- compare-and-set algorithms

Pessimistic locking appears in:

- payment settlement records
- inventory allocation under high contention
- bank account transfers
- database transactions using row locks
- job leasing where one worker must own a row

Real systems often use both:
- optimistic for normal user edits
- pessimistic or conditional updates for high-value contention points

---

## 5. How It Works

Optimistic flow:

1. Read record with version.
2. User or service computes new value.
3. Update row only if version still matches.
4. If update count is zero, conflict occurred.
5. Reload and retry or return conflict to caller.

Pessimistic flow:

1. Start transaction.
2. Lock row before editing.
3. Other writers wait or fail.
4. Update record.
5. Commit and release lock.

SQL shape:

```sql
-- Optimistic
UPDATE booking
SET status = 'CONFIRMED', version = version + 1
WHERE booking_id = 'b1' AND version = 7;

-- Pessimistic
SELECT * FROM booking
WHERE booking_id = 'b1'
FOR UPDATE;
```

---

## 6. What Problem It Solves

- Primary problem solved: conflicting concurrent updates to the same resource.
- Secondary benefits: lost-update prevention, clearer retry behavior, stronger data correctness.
- Systems impact: protects records in multi-threaded and multi-instance environments.

Optimistic locking is best when:
- conflicts are rare
- reads are common
- retry is acceptable

Pessimistic locking is best when:
- conflicts are common
- correctness is critical
- waiting is better than retry storms

---

## 7. When to Rely on It

Use optimistic locking when:

- users edit records occasionally
- conflicts are rare
- you want high read concurrency
- stale update must be detected
- retry or user conflict message is acceptable

Use pessimistic locking when:

- conflicts are frequent
- resource is scarce
- update must be serialized
- duplicate processing is costly
- long retry loops would overload the system

Interviewer keywords:
- lost update
- version column
- row lock
- concurrent edits
- inventory race
- conflict detection
- `SELECT FOR UPDATE`

---

## 8. When Not to Use It

Avoid optimistic locking when:

- conflicts are so frequent that retries dominate
- failed retries create bad user experience
- operation cannot be safely retried

Avoid pessimistic locking when:

- user think-time is involved
- locks may be held for long periods
- high throughput matters more than strict serialization
- deadlock risk is high

Use database unique constraints when the problem is uniqueness, not update conflict.

Use idempotency keys when the problem is duplicate request execution.

---

## 9. Pros and Cons

| Approach | Pros | Cons |
|---|---|---|
| Optimistic locking | High concurrency, no blocking on read, simple version model | Conflicts fail late, requires retry or user handling |
| Pessimistic locking | Prevents conflicting writers upfront, strong serialization | Blocking, deadlock risk, lower throughput under contention |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Optimistic gains throughput but pays with conflict retries.
- Pessimistic gains certainty but pays with blocking.
- Optimistic is user-friendly for rare conflicts.
- Pessimistic is safer for hot scarce resources.

### Common Mistakes

- Mistake: optimistic update without checking affected row count.
- Why it is wrong: conflict is silently ignored.
- Better approach: if update count is zero, reload and retry or return conflict.

- Mistake: holding pessimistic locks while calling external services.
- Why it is wrong: lock duration explodes and deadlock risk rises.
- Better approach: keep transaction short and avoid network calls inside locks.

- Mistake: using in-memory locks for database rows across many service instances.
- Why it is wrong: each instance has its own memory.
- Better approach: use database locking, conditional updates, or distributed locks when truly needed.

---

## 11. Key Numbers

Concurrency heuristics:

- Optimistic locking works well when conflict rate is low, often under a few percent.
- Pessimistic lock duration should be short, usually milliseconds, not user think-time.
- Always measure retry rate for optimistic locking.
- Always monitor lock wait time and deadlocks for pessimistic locking.
- One hot row can become the bottleneck regardless of locking strategy.

Memory number:
- Optimistic retries work until conflict rate becomes the workload.

---

## 12. Failure Modes

- Lost update: version check missing or ignored.
- Retry storm: many clients repeatedly fail optimistic updates.
- Deadlock: pessimistic locks acquired in different orders.
- Long lock hold: transaction waits on external payment call.
- Starvation: one writer repeatedly loses optimistic conflicts.

Mitigations:
- use version columns and check update count
- cap retries with backoff
- acquire locks in consistent order
- keep transactions short
- expose conflict response to users when retry is not safe

---

## 13. Scenario

- Product / system: Hotel booking update service
- Requirement: prevent stale updates when two agents modify the same booking
- Good design: use optimistic locking with a version column for normal edits
- Why this concept fits: conflicts are possible but usually rare
- What would go wrong without it: the second save could overwrite the first without noticing

---

## 14. Java Code Sample

### Optimistic locking with version check

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record BookingSnapshot(String bookingId, String status, int version) {
}

class OptimisticLockException extends RuntimeException {
	OptimisticLockException(String message) {
		super(message);
	}
}

class BookingRepository {
	private final Map<String, BookingSnapshot> store = new ConcurrentHashMap<>();

	BookingRepository() {
		store.put("booking-1", new BookingSnapshot("booking-1", "DRAFT", 1));
	}

	public BookingSnapshot find(String bookingId) {
		return store.get(bookingId);
	}

	public void updateStatus(String bookingId, String newStatus, int expectedVersion) {
		store.compute(bookingId, (id, current) -> {
			// Concurrency concept: expectedVersion is the optimistic lock guard.
			if (current.version() != expectedVersion) {
				throw new OptimisticLockException("stale booking version");
			}
			return new BookingSnapshot(id, newStatus, current.version() + 1);
		});
	}
}

class BookingUpdateService {
	private final BookingRepository repository;

	BookingUpdateService(BookingRepository repository) {
		this.repository = repository;
	}

	public void confirm(String bookingId) {
		BookingSnapshot snapshot = repository.find(bookingId);
		// Concurrency concept: update succeeds only if no other writer changed the version after this read.
		repository.updateStatus(bookingId, "CONFIRMED", snapshot.version());
	}
}
```

Key idea:
- optimistic locking does not block the first read; it detects stale writes at commit/update time

---

## 15. Python Mini Program / Simulation

This mini program simulates optimistic locking with a version field.

```python
from dataclasses import dataclass, replace
import threading


@dataclass(frozen=True)
class BookingSnapshot:
	booking_id: str
	status: str
	version: int


class BookingRepository:
	def __init__(self) -> None:
		self._booking = BookingSnapshot("booking-1", "draft", 1)
		self._lock = threading.Lock()

	def find(self) -> BookingSnapshot:
		return self._booking

	def update_status(self, new_status: str, expected_version: int) -> None:
		with self._lock:
			# Concurrency concept: version check detects stale update attempts.
			if self._booking.version != expected_version:
				raise ValueError("optimistic lock conflict")
			self._booking = replace(
				self._booking,
				status=new_status,
				version=self._booking.version + 1,
			)


def main() -> None:
	repository = BookingRepository()
	first_reader = repository.find()
	second_reader = repository.find()

	repository.update_status("confirmed", first_reader.version)

	try:
		repository.update_status("cancelled", second_reader.version)
	except ValueError as error:
		print(error)

	print(repository.find())


if __name__ == "__main__":
	main()
```

What this demonstrates:
- both readers can read without blocking
- first writer increments version
- second writer fails because it is based on stale data

---

## 16. Practical Question

> Two support agents edit the same booking. How would you prevent one save from silently overwriting the other?

---

## 17. Strong Answer

I would use optimistic locking if conflicts are expected to be rare. The booking row would have a version column. When an agent reads the booking, the response includes the current version. On update, the database update includes `WHERE booking_id = ? AND version = ?` and increments the version.

If the update affects zero rows, someone else changed the booking first. I would return a conflict response or reload and retry depending on the use case.

I would use pessimistic locking only if conflicts are frequent or the operation must be serialized immediately. I would keep the lock duration short and never hold it across external calls.

---

## 18. Revision Notes

- One-line summary: Optimistic locking detects stale writes later; pessimistic locking blocks competing writers earlier.
- Three keywords: version, conflict, row lock
- One interview trap: forgetting to check optimistic update count.
- One memory trick: optimistic checks later; pessimistic blocks now.

---

# Topic 3: Atomic Operations and CAS Intuition

> Track: 2.4 Concurrency & Thread Safety
> Scope: atomic variables, compare-and-set, lock-free intuition, retry loops, counters, idempotency, and small state transitions

---

## 1. Intuition

Imagine a room reservation board with one slot.

CAS says:
- "I saw the slot was empty. Put my booking there only if it is still empty."

If another thread already filled it, the operation fails and you decide whether to retry.

Short memory trick:
- compare what I expected
- swap to new value only if expectation still holds
- otherwise retry or fail

---

## 2. Definition

- Definition: Compare-and-swap, often called CAS, is an atomic operation that updates a value only if its current value equals an expected value.
- Category: Low-level concurrency primitive
- Core idea: Perform small safe updates without taking a traditional blocking lock.

CAS shape:

```text
compareAndSet(expectedValue, newValue)
```

Meaning:
- if current value equals expected value, write new value and return true
- otherwise leave value unchanged and return false

---

## 3. Why It Exists

Locks are powerful, but sometimes too heavy for tiny state changes.

Examples:
- increment a counter
- set a flag once
- claim a job from queued to processing
- update a reference if unchanged
- implement high-performance concurrent structures

CAS exists because hardware can often perform a tiny compare-and-update atomically.

It avoids blocking, but it may retry under contention.

Important nuance:
- CAS is not magic. It still needs correct retry logic and attention to ABA problems in advanced cases.

---

## 4. Reality

CAS appears in:

- Java `AtomicInteger`
- Java `AtomicReference`
- Java `LongAdder` internals
- lock-free queues
- optimistic concurrency algorithms
- non-blocking counters
- job claim state transitions
- rate limiter counters

In interviews, you do not usually implement CPU-level CAS. You explain the idea and use atomic classes.

Python note:
- CPython does not expose a general built-in CAS primitive like Java atomics.
- Python examples usually use locks or queues unless using special native libraries.

---

## 5. How It Works

CAS increment flow:

1. Read current value.
2. Compute next value.
3. Try `compareAndSet(current, next)`.
4. If it succeeds, update is done.
5. If it fails, another thread changed the value.
6. Read again and retry.

Example:

```text
counter = 10
Thread A reads 10, wants 11
Thread B reads 10, wants 11
Thread A CAS(10, 11) succeeds
Thread B CAS(10, 11) fails because current is now 11
Thread B reads 11, tries CAS(11, 12), succeeds
```

---

## 6. What Problem It Solves

- Primary problem solved: safe small updates to shared values without traditional locks.
- Secondary benefits: non-blocking progress, fast counters, compact state transitions.
- Systems impact: high-throughput concurrency for small shared values.

CAS is useful when:
- update is small
- state fits in one atomic variable/reference
- retry is acceptable
- contention is not extreme

---

## 7. When to Rely on It

Use atomic/CAS operations when:

- incrementing counters
- setting one-time flags
- claiming a job with simple state transition
- updating a reference if unchanged
- lock overhead is unnecessary
- compound invariant is small enough for one atomic variable

Interviewer keywords:
- atomic counter
- compare-and-set
- lock-free
- non-blocking
- lost increment
- thread-safe counter
- claim once

---

## 8. When Not to Use It

Avoid CAS when:

- operation touches multiple variables that must change together
- invariant is complex
- retry under contention becomes expensive
- fairness is required
- code becomes unreadable compared to a lock
- ABA problem matters and is not handled

Use locks for larger critical sections.

Use database transactions for cross-process state.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Avoids blocking locks for small updates | Retry loops can spin under contention |
| Fast for counters and flags | Harder to reason about than locks |
| Prevents lost increments | Not ideal for multi-variable invariants |
| Foundation for non-blocking structures | ABA problem can appear in advanced algorithms |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: fast, non-blocking small updates.
- Give up: simple critical-section readability.
- Latency impact: low under low contention, unpredictable under high contention.
- CPU impact: failed CAS retries can burn CPU.

### Common Mistakes

- Mistake: using atomic variables for related fields separately.
- Why it is wrong: each field is atomic, but the combined invariant is not.
- Better approach: use one immutable holder with `AtomicReference` or use a lock.

- Mistake: ignoring failed CAS.
- Why it is wrong: update may not happen.
- Better approach: retry or return conflict.

- Mistake: assuming CAS works across service instances.
- Why it is wrong: Java atomics protect memory in one process only.
- Better approach: use database conditional updates or distributed coordination for shared external state.

---

## 11. Key Numbers

Concurrency heuristics:

- Atomic increment is strong for simple counters.
- High contention counters may perform better with `LongAdder` than `AtomicLong` in Java.
- CAS loops should be small and bounded where possible.
- If a CAS loop performs I/O, the design is wrong.
- If correctness needs more than one variable, pause before using separate atomics.

Memory number:
- Atomic means one operation is safe, not the whole business workflow.

---

## 12. Failure Modes

- Lost update due to non-atomic increment.
- Infinite or hot retry loop under heavy contention.
- ABA problem: value changes from A to B and back to A, fooling simple CAS.
- Partial invariant: two atomic fields become inconsistent together.
- Local-only correctness: atomics do not coordinate across processes.

Mitigations:
- use built-in atomic classes
- keep CAS loops simple
- use stamped references for ABA-sensitive algorithms
- use locks for complex invariants
- use database conditional updates for cross-instance state

---

## 13. Scenario

- Product / system: Booking request id generator and job claim flag
- Requirement: multiple worker threads must generate unique local sequence numbers and claim jobs only once
- Good design: use atomic increment for counters and compare-and-set for single-claim state
- Why this concept fits: operations are small and local to one process
- What would go wrong without it: increments could be lost and two workers could claim the same job

---

## 14. Java Code Sample

### Atomic counter and compare-and-set job claim

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

enum JobState {
	READY,
	PROCESSING,
	DONE
}

class BookingSequenceGenerator {
	private final AtomicInteger nextValue = new AtomicInteger(0);

	public int nextBookingSequence() {
		// Concurrency concept: atomic increment prevents lost updates without a synchronized block.
		return nextValue.incrementAndGet();
	}
}

class BookingJob {
	private final AtomicReference<JobState> state = new AtomicReference<>(JobState.READY);

	public boolean claim() {
		// Concurrency concept: CAS changes READY to PROCESSING only if nobody changed it first.
		return state.compareAndSet(JobState.READY, JobState.PROCESSING);
	}

	public void complete() {
		state.set(JobState.DONE);
	}

	public JobState state() {
		return state.get();
	}
}

class AtomicDemo {
	public static void main(String[] args) {
		BookingSequenceGenerator generator = new BookingSequenceGenerator();
		System.out.println(generator.nextBookingSequence());

		BookingJob job = new BookingJob();
		System.out.println("first claim=" + job.claim());
		System.out.println("second claim=" + job.claim());
	}
}
```

Key idea:
- CAS is perfect for small state transitions like ready-to-processing, but not for complex workflows involving many fields

---

## 15. Python Mini Program / Simulation

Python does not provide a built-in general CAS primitive like Java's `AtomicReference`, so this example simulates compare-and-set with a lock.

```python
import threading


class AtomicReference:
	def __init__(self, value: str) -> None:
		self._value = value
		self._lock = threading.Lock()

	def compare_and_set(self, expected: str, new_value: str) -> bool:
		with self._lock:
			# Concurrency concept: compare and update happen as one protected operation.
			if self._value != expected:
				return False
			self._value = new_value
			return True

	def get(self) -> str:
		with self._lock:
			return self._value


class BookingJob:
	def __init__(self) -> None:
		self.state = AtomicReference("ready")

	def claim(self) -> bool:
		# Concurrency concept: only one worker can move ready -> processing.
		return self.state.compare_and_set("ready", "processing")


def main() -> None:
	job = BookingJob()

	def worker(name: str) -> None:
		print(name, job.claim())

	threads = [threading.Thread(target=worker, args=(f"worker-{index}",)) for index in range(3)]
	for thread in threads:
		thread.start()
	for thread in threads:
		thread.join()
	print("final", job.state.get())


if __name__ == "__main__":
	main()
```

What this demonstrates:
- CAS intuition is compare expected value, then swap
- only one worker should successfully claim the job
- in Python, a lock is used to simulate the atomicity

---

## 16. Practical Question

> You need a thread-safe counter and a way for only one worker to claim a job. Would you use locks or atomics?

---

## 17. Strong Answer

For a simple counter inside one JVM, I would use `AtomicInteger`, `AtomicLong`, or `LongAdder` depending on contention. This prevents lost increments without writing a synchronized method.

For claiming a job once, I would use compare-and-set if the state transition is local and simple: `READY -> PROCESSING` only if the current state is still `READY`.

If the job state is in a database and multiple service instances can claim it, I would not use Java atomics. I would use a database conditional update such as `UPDATE job SET status='PROCESSING' WHERE id=? AND status='READY'` and check the affected row count.

---

## 18. Revision Notes

- One-line summary: CAS updates a value only if it still matches the expected value.
- Three keywords: atomic, compare, retry
- One interview trap: using atomics for multi-field business invariants.
- One memory trick: CAS says "change it only if nobody changed it first."

---

# Topic 4: Thread Pools

> Track: 2.4 Concurrency & Thread Safety
> Scope: worker threads, task queues, executor services, pool sizing, rejection policies, backpressure, latency, and graceful shutdown

---

## 1. Intuition

Think of a hotel with a fixed number of front-desk agents.

Guests form a queue. Agents pick up the next guest when free. If too many guests arrive, the queue grows. If the queue has no limit, the lobby becomes chaos. If there are too many agents, they trip over each other.

A thread pool is a controlled group of worker threads that execute submitted tasks.

Short memory trick:
- workers execute
- queue buffers
- rejection protects overload

---

## 2. Definition

- Definition: A thread pool is a reusable set of worker threads that execute submitted tasks, usually backed by a task queue and a rejection policy.
- Category: Concurrency execution management
- Core idea: Control concurrency instead of creating unlimited threads.

Thread pool pieces:
- worker count
- task queue
- task submission API
- rejection policy
- shutdown behavior
- monitoring metrics

---

## 3. Why It Exists

Creating one thread per request is dangerous.

Problems:
- thread creation is expensive
- too many threads consume memory
- context switching increases
- downstream systems get overloaded
- queue grows without bound
- shutdown becomes messy

Thread pools exist to bound and reuse execution capacity.

They also make overload visible:
- queue depth grows
- tasks are rejected
- latency increases

---

## 4. Reality

Thread pools appear in:

- web servers
- application servers
- async task executors
- Kafka consumers
- batch processors
- email senders
- image processing jobs
- scheduled background work
- database connection pools conceptually similar in capacity control

Java examples:
- `ExecutorService`
- `ThreadPoolExecutor`
- `ForkJoinPool`
- `CompletableFuture` default executor considerations

Production reality:
- unbounded queues hide overload until memory fails
- wrong pool size can hurt both latency and throughput

---

## 5. How It Works

Thread pool flow:

1. Caller submits a task.
2. If a worker is free, it runs the task.
3. If all workers are busy, task enters queue.
4. If queue is full, rejection policy runs.
5. Worker completes task and picks next queued task.
6. Shutdown stops accepting new tasks and drains or cancels existing work.

Sizing intuition:
- CPU-bound work: pool size near number of CPU cores.
- I/O-bound work: pool can be larger because threads wait on I/O.
- Downstream-limited work: pool should respect downstream capacity.

---

## 6. What Problem It Solves

- Primary problem solved: uncontrolled task execution and thread creation.
- Secondary benefits: reuse, bounded concurrency, overload protection, graceful shutdown.
- Systems impact: prevents a service from overwhelming itself or its dependencies.

Thread pools are especially useful when:
- many independent tasks exist
- tasks can be processed concurrently
- concurrency must be bounded
- queueing and rejection behavior must be explicit

---

## 7. When to Rely on It

Use thread pools when:

- running background tasks
- processing jobs from a queue
- parallelizing independent work
- limiting concurrency to a downstream dependency
- replacing ad hoc thread creation
- adding timeouts and graceful shutdown

Interviewer keywords:
- worker pool
- bounded queue
- executor
- background jobs
- task processing
- concurrency limit
- graceful shutdown
- overload protection

---

## 8. When Not to Use It Blindly

Avoid thread pools when:

- task count is tiny and synchronous call is clearer
- tasks depend heavily on each other
- blocking calls inside the pool can starve important tasks
- pool hides backpressure behind an unbounded queue
- CPU-bound pool is much larger than cores
- you submit tasks to the same pool and wait for subtasks, causing starvation

Use event loops or async I/O when the workload is massive I/O concurrency and the stack supports it.

Use separate pools for workloads with different latency and blocking characteristics.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reuses threads | Wrong sizing causes latency or underuse |
| Bounds concurrency | Unbounded queues can hide overload |
| Supports queues and rejection | Deadlocks possible with nested task waits |
| Enables graceful shutdown | Shared pools can cause noisy-neighbor problems |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: controlled concurrency and reusable workers.
- Give up: immediate execution when pool is saturated.
- Latency impact: queueing increases waiting time.
- Throughput impact: right sizing improves throughput, too many threads can reduce it.

### Common Mistakes

- Mistake: using an unbounded queue for all tasks.
- Why it is wrong: memory absorbs overload until the process fails.
- Better approach: use bounded queues and rejection/backpressure.

- Mistake: one shared pool for CPU-heavy and blocking I/O tasks.
- Why it is wrong: blocking tasks starve CPU tasks or vice versa.
- Better approach: separate pools by workload type.

- Mistake: not shutting down executor.
- Why it is wrong: application may hang or leak resources.
- Better approach: call graceful shutdown and await termination.

- Mistake: submitting subtasks to same small pool and waiting.
- Why it is wrong: all workers can block waiting for queued work that never runs.
- Better approach: avoid nested blocking or use separate executor.

---

## 11. Key Numbers

Concurrency heuristics:

- CPU-bound pool size: near number of cores.
- I/O-bound pool size: larger, based on wait time and downstream capacity.
- Queue size should be bounded and chosen from latency budget and memory limits.
- Monitor active threads, queue depth, task latency, rejection count, and completed task count.
- Pool size should protect downstream dependencies, not only local CPU.

Memory number:
- A thread pool without a bounded queue is often just delayed overload.

---

## 12. Failure Modes

- Queue explosion: memory grows until out-of-memory.
- Thread starvation: all workers blocked on slow I/O.
- Rejection storm: callers retry immediately and worsen overload.
- Deadlock: tasks wait for subtasks in the same saturated pool.
- Slow shutdown: non-daemon worker threads keep process alive.
- Downstream overload: pool sends too much work to database or payment provider.

Mitigations:
- bounded queues
- rejection policies
- timeouts
- circuit breakers
- separate pools by workload
- graceful shutdown
- metrics and alerts

---

## 13. Scenario

- Product / system: Booking confirmation email sender
- Requirement: send emails asynchronously without creating unlimited threads or losing overload visibility
- Good design: use a bounded thread pool with a bounded queue and rejection handling
- Why this concept fits: work is independent, I/O-bound, and should not block checkout forever
- What would go wrong without it: one thread per email could exhaust memory during traffic spikes

---

## 14. Java Code Sample

### Bounded thread pool for booking emails

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class BookingEmailSender {
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
			4,
			4,
			0L,
			TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(100),
			new ThreadPoolExecutor.AbortPolicy()
	);

	public void sendAsync(String userId, String bookingId) {
		// Concurrency concept: bounded pool controls how many email tasks run and wait.
		executor.execute(() -> sendEmail(userId, bookingId));
	}

	private void sendEmail(String userId, String bookingId) {
		System.out.println("sending email to " + userId + " for " + bookingId);
	}

	public void shutdown() {
		// Concurrency concept: graceful shutdown stops accepting work and lets queued work finish.
		executor.shutdown();
	}
}
```

Key idea:
- thread pool design includes worker count, queue size, rejection behavior, and shutdown, not just `execute`

---

## 15. Python Mini Program / Simulation

Python's `ThreadPoolExecutor` is useful for I/O-bound work and simple concurrency demonstrations.

```python
from concurrent.futures import ThreadPoolExecutor, as_completed
import time


def send_email(user_id: str, booking_id: str) -> str:
	# Concurrency concept: task represents independent work executed by a pool worker.
	time.sleep(0.1)
	return f"email sent to {user_id} for {booking_id}"


def main() -> None:
	bookings = [(f"user-{index}", f"booking-{index}") for index in range(8)]

	# Concurrency concept: max_workers bounds concurrent execution.
	with ThreadPoolExecutor(max_workers=3) as executor:
		futures = [executor.submit(send_email, user_id, booking_id) for user_id, booking_id in bookings]
		for future in as_completed(futures):
			print(future.result())


if __name__ == "__main__":
	main()
```

What this demonstrates:
- tasks are submitted faster than workers may complete them
- workers reuse threads
- `max_workers` is a concurrency limit, not just a performance knob

---

## 16. Practical Question

> You need to send booking confirmation emails asynchronously. Why not create a new thread for every email?

---

## 17. Strong Answer

I would avoid creating one thread per email because traffic spikes could create thousands of threads, causing memory pressure and context switching. I would use a bounded thread pool.

The pool should have a fixed or carefully sized worker count, a bounded queue, and a clear rejection policy. Since email sending is I/O-bound, the pool can be larger than a CPU-bound pool, but it should still respect the email provider's capacity.

I would monitor queue depth, active workers, task latency, and rejections. On shutdown, I would stop accepting new tasks and drain existing ones gracefully.

---

## 18. Revision Notes

- One-line summary: Thread pools reuse bounded workers to execute queued tasks safely.
- Three keywords: workers, queue, rejection
- One interview trap: using unbounded queues and calling it scalable.
- One memory trick: pool is workers plus queue plus overload policy.

---

# Topic 5: Producer-Consumer Pattern

> Track: 2.4 Concurrency & Thread Safety
> Scope: producers, consumers, blocking queues, backpressure, buffering, poison pills, graceful shutdown, and work distribution

---

## 1. Intuition

Think of a hotel kitchen.

Waiters place orders on a counter. Chefs pick up orders from the counter. If orders arrive faster than chefs cook, the counter fills. If the counter is full, waiters must slow down or reject new orders.

Producer-consumer is the pattern of separating work creation from work processing using a buffer.

Short memory trick:
- producers create work
- queue buffers work
- consumers process work
- bounded queue creates backpressure

---

## 2. Definition

- Definition: Producer-consumer is a concurrency pattern where producer threads place work into a shared queue and consumer threads take work from that queue for processing.
- Category: Concurrent coordination pattern
- Core idea: Decouple task creation from task execution while coordinating through a thread-safe buffer.

Common pieces:
- producer
- blocking queue
- consumer
- backpressure
- shutdown signal
- retry or dead-letter handling

---

## 3. Why It Exists

Directly processing work in the producer can be inefficient or unsafe.

Problems:
- request thread blocks on slow processing
- bursts overwhelm consumers
- producers and consumers run at different speeds
- work needs retry or batching
- consumers need independent scaling

Producer-consumer exists to smooth bursts and control work handoff.

But a queue is not a magic fix:
- if producers always outrun consumers, the queue eventually fills
- bounded queues provide backpressure
- unbounded queues hide overload

---

## 4. Reality

Producer-consumer appears in:

- logging pipelines
- email notification workers
- image processing
- Kafka consumers internally processing records
- batch job systems
- web crawler fetch queues
- payment reconciliation workers
- inventory event processing
- background task queues

In-process examples:
- Java `BlockingQueue`
- Python `queue.Queue`

Distributed examples:
- Kafka
- RabbitMQ
- SQS
- Redis streams

---

## 5. How It Works

Flow:

1. Producer creates work item.
2. Producer puts item into blocking queue.
3. If queue is full, producer waits, times out, or rejects.
4. Consumer takes item from queue.
5. Consumer processes item.
6. On success, item is complete.
7. On failure, item is retried or moved to dead-letter handling.
8. Shutdown uses a stop flag, poison pill, or queue drain policy.

Backpressure point:
- bounded queue prevents memory growth and tells producers to slow down

---

## 6. What Problem It Solves

- Primary problem solved: decoupling work production from work processing.
- Secondary benefits: buffering, backpressure, worker scaling, smoother burst handling.
- Systems impact: improves resilience when producers and consumers have different speeds.

Producer-consumer is especially useful when:
- work is asynchronous
- processing is slower than request handling
- workers can run independently
- queue size and failure behavior can be controlled

---

## 7. When to Rely on It

Use producer-consumer when:

- request path should not do slow work
- processing can happen later
- tasks are independent
- bursts need buffering
- consumers can scale separately
- you need bounded work handoff

Interviewer keywords:
- queue
- worker
- background processing
- backpressure
- bounded buffer
- async work
- producer and consumer speeds

---

## 8. When Not to Use It

Avoid producer-consumer when:

- caller needs immediate result
- operation must be part of one synchronous transaction
- ordering across all items is strict and hard to preserve
- queue failure behavior is not defined
- async delay harms user experience
- work requires distributed durability but you only use in-memory queue

Use direct call for required synchronous work.

Use durable message broker for cross-process reliability.

---

## 9. Pros and Cons

| Pros | Cons |
|---|---|
| Decouples producers and consumers | Adds queue management complexity |
| Smooths bursts | Queue can become bottleneck |
| Enables backpressure | In-memory queues lose work on process crash |
| Allows worker scaling | Ordering and retries need design |

---

## 10. Trade-offs and Common Mistakes

### Trade-offs

- Gain: smoother processing and decoupled execution.
- Give up: immediate completion and simple direct control flow.
- Latency impact: queue waiting time adds delay.
- Reliability impact: in-memory queue is not durable across crashes.

### Common Mistakes

- Mistake: using an unbounded queue.
- Why it is wrong: memory hides overload until the process fails.
- Better approach: bounded queue with backpressure or rejection.

- Mistake: no shutdown protocol.
- Why it is wrong: consumers may hang forever or lose work.
- Better approach: use poison pills, cancellation, or drain policy.

- Mistake: ignoring processing failures.
- Why it is wrong: failed work disappears.
- Better approach: retry with limits and dead-letter handling.

- Mistake: assuming in-memory queue is reliable like Kafka.
- Why it is wrong: process crash loses queued items.
- Better approach: use durable broker for important cross-process work.

---

## 11. Key Numbers

Concurrency heuristics:

- Queue capacity should be bounded.
- Consumer count should match processing cost and downstream capacity.
- Monitor queue depth, enqueue rate, dequeue rate, processing latency, and failure count.
- If queue depth grows continuously, consumers cannot keep up.
- Poison pill count should usually equal consumer count if each consumer exits after one poison pill.

Memory number:
- A queue absorbs bursts, not permanent overload.

---

## 12. Failure Modes

- Queue full: producers block or fail.
- Consumer crash: work stops processing.
- Poison pill mistake: only one consumer exits while others hang.
- Lost work: in-memory queue disappears on process crash.
- Retry loop: bad item keeps failing and blocks useful work.
- No backpressure: memory grows without bound.

Mitigations:
- bounded queue
- timeouts on enqueue
- poison pill or cancellation protocol
- retry limit
- dead-letter queue
- durable broker for critical work
- metrics and alerts

---

## 13. Scenario

- Product / system: Booking notification pipeline
- Requirement: checkout enqueues notification work while background workers send emails
- Good design: producer puts notification jobs into a bounded blocking queue; consumers process jobs and shut down with poison pills
- Why this concept fits: sending email is slower than checkout and can happen asynchronously
- What would go wrong without it: request threads block on email provider, or unlimited async work exhausts memory

---

## 14. Java Code Sample

### Producer-consumer with BlockingQueue and poison pill

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

record NotificationJob(String userId, String bookingId, boolean poisonPill) {
	static NotificationJob work(String userId, String bookingId) {
		return new NotificationJob(userId, bookingId, false);
	}

	static NotificationJob stop() {
		return new NotificationJob("", "", true);
	}
}

class NotificationPipeline {
	private final BlockingQueue<NotificationJob> queue = new ArrayBlockingQueue<>(100);

	public void enqueue(NotificationJob job) throws InterruptedException {
		// Concurrency concept: bounded queue applies backpressure when consumers fall behind.
		queue.put(job);
	}

	public void consume() throws InterruptedException {
		while (true) {
			NotificationJob job = queue.take();
			if (job.poisonPill()) {
				// Concurrency concept: poison pill gives consumer a clear shutdown signal.
				break;
			}
			sendEmail(job.userId(), job.bookingId());
		}
	}

	private void sendEmail(String userId, String bookingId) {
		System.out.println("email sent to " + userId + " for " + bookingId);
	}
}
```

Key idea:
- producers and consumers coordinate through a blocking queue instead of sharing ad hoc mutable lists

---

## 15. Python Mini Program / Simulation

This mini program uses `queue.Queue`, which is thread-safe and supports blocking producer-consumer coordination.

```python
import queue
import threading
import time
from dataclasses import dataclass


@dataclass(frozen=True)
class NotificationJob:
	user_id: str
	booking_id: str


STOP = object()


def producer(jobs: queue.Queue[NotificationJob | object]) -> None:
	for index in range(5):
		job = NotificationJob(f"user-{index}", f"booking-{index}")
		# Concurrency concept: bounded queue blocks producer if consumers are behind.
		jobs.put(job)
		print("produced", job.booking_id)


def consumer(name: str, jobs: queue.Queue[NotificationJob | object]) -> None:
	while True:
		item = jobs.get()
		try:
			if item is STOP:
				# Concurrency concept: sentinel gives each consumer a shutdown signal.
				print(name, "stopping")
				return
			assert isinstance(item, NotificationJob)
			time.sleep(0.1)
			print(name, "sent", item.booking_id)
		finally:
			jobs.task_done()


def main() -> None:
	jobs: queue.Queue[NotificationJob | object] = queue.Queue(maxsize=2)
	consumers = [threading.Thread(target=consumer, args=(f"worker-{index}", jobs)) for index in range(2)]

	for thread in consumers:
		thread.start()

	producer(jobs)
	jobs.join()

	for _ in consumers:
		jobs.put(STOP)
	for thread in consumers:
		thread.join()


if __name__ == "__main__":
	main()
```

What this demonstrates:
- producer and consumers do not call each other directly
- bounded queue provides backpressure
- shutdown requires one sentinel per consumer

---

## 16. Practical Question

> Checkout must enqueue notifications while workers send them in the background. How would you design this safely?

---

## 17. Strong Answer

I would use a producer-consumer design. Checkout acts as the producer and creates notification jobs. A bounded blocking queue stores the jobs. Worker threads act as consumers and send notifications.

The queue must be bounded so the service does not hide overload in memory. If the queue fills, the producer should block briefly, fail fast, or degrade based on the product requirement. Consumers should handle failures with retries and dead-letter behavior if needed.

For in-process non-critical notifications, a blocking queue is fine. For critical cross-service delivery, I would use a durable broker like Kafka, RabbitMQ, or SQS because an in-memory queue loses work on process crash.

---

## 18. Revision Notes

- One-line summary: Producer-consumer decouples work creation from work processing through a thread-safe queue.
- Three keywords: producer, queue, consumer
- One interview trap: using an unbounded in-memory queue and ignoring overload.
- One memory trick: queue handles bursts, not infinite imbalance.

---

## Final Interview Comparison Sheet

| Concept | Best one-line explanation | Confusion to avoid |
|---|---|---|
| Thread safety principles | Protect shared mutable state from unsafe interleavings | Thread-safe does not mean fast |
| Optimistic locking | Detect stale updates using version checks | It does not prevent conflict upfront |
| Pessimistic locking | Block competing writers before update | Do not hold locks across slow external calls |
| Atomic operations / CAS | Update one value only if it still matches expectation | Atomic variable does not protect whole workflow |
| Thread pools | Execute tasks with bounded reusable workers | Pool needs queue, sizing, rejection, and shutdown |
| Producer-consumer | Hand off work through a thread-safe queue | In-memory queue is not durable messaging |

---

## Fast Recall Rules

- If state is shared and mutable, ask how it is protected.
- If the operation is read-check-write, protect it as one unit.
- If conflicts are rare, optimistic locking is often good.
- If conflicts are frequent and expensive, pessimistic locking may fit.
- If one small value changes, atomic/CAS may beat a lock.
- If multiple values must change together, a lock or transaction is usually clearer.
- If many tasks need controlled execution, use a thread pool.
- If producers and consumers run at different speeds, use a bounded queue.
- If queued work must survive process crash, use a durable broker.
- In distributed systems, local locks and atomics protect only one process.
