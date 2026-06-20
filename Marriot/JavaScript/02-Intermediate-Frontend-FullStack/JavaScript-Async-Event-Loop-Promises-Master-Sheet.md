# JavaScript Async, Event Loop, And Promises Master Sheet

Target: JavaScript interviews where event loop behavior, async output order, promises, timers, browser tasks, Node.js phases, async/await, and production async patterns are tested.

This sheet covers:
- Runtime mental model
- Call stack, heap, Web APIs, libuv, task queues, microtask queues
- Browser event loop
- Node.js event loop awareness
- Macrotasks vs microtasks
- Timers and scheduling
- `queueMicrotask`, `process.nextTick`, `setImmediate`
- Promise states and chaining
- Promise executor behavior
- `.then`, `.catch`, `.finally`
- `async` / `await`
- Error handling
- Promise combinators
- Sequential vs parallel async work
- Concurrency control
- Cancellation and `AbortController`
- Timeouts and retries
- Race conditions
- UI responsiveness and long tasks
- Output-order interview traps
- Production patterns
- FAANG-level scenario answers

How to use this:
- First master the mental model.
- Then practice output-order questions until they feel mechanical.
- Then learn production patterns: cancellation, timeout, retry, concurrency limit, and stale-result protection.
- In interviews, always separate JavaScript language behavior from runtime APIs.

---

## 1. Mental Model

JavaScript executes synchronous code on one main call stack.

Async work is not magic. It is coordination between:

```text
JavaScript engine        -> runs JS code, stack, heap, promises
Runtime environment      -> browser APIs or Node.js/libuv
Task queues              -> timers, IO callbacks, UI events, message events
Microtask queue          -> promise reactions, queueMicrotask, mutation observers
Event loop               -> decides what runs next
```

Simple model:

```text
1. Run current synchronous script until the call stack is empty.
2. Drain microtasks.
3. Run one task/macrotask.
4. Drain microtasks again.
5. Browser may render between tasks.
6. Repeat.
```

Strong interview line:

```text
JavaScript is single-threaded at the execution stack level, but the runtime can handle async
operations outside the stack and schedule callbacks back through queues. Promises use the
microtask queue, while timers and many IO/UI callbacks use task queues.
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Call stack | Very high | Foundation for sync execution |
| Event loop | Very high | Explains async scheduling |
| Microtasks vs macrotasks | Very high | Output-order questions |
| Promise executor behavior | Very high | Common trap |
| Promise chaining | Very high | Real async control flow |
| `async` / `await` | Very high | Modern async baseline |
| Error handling | Very high | Production correctness |
| `Promise.all` | Very high | Parallel async work |
| `allSettled`, `race`, `any` | High | Senior async patterns |
| Timers | High | Scheduling and output traps |
| `queueMicrotask` | Medium-high | Explicit microtask scheduling |
| Browser rendering timing | Medium-high | Frontend performance |
| Node.js event loop phases | Medium-high | Backend/full-stack interviews |
| `process.nextTick` | Medium | Node-specific priority trap |
| `setImmediate` | Medium | Node scheduling awareness |
| Cancellation | Very high | Real production async |
| Timeouts | Very high | Production reliability |
| Retries with backoff | High | Distributed systems readiness |
| Concurrency limits | Very high | Prevent API storms |
| Race conditions | Very high | UI and backend correctness |
| Long tasks/event-loop blocking | Very high | Performance debugging |

---

## 3. Runtime Components

JavaScript code runs inside a runtime.

Browser runtime includes:

```text
JavaScript engine
Web APIs
DOM
networking
storage
timers
rendering pipeline
task queues
microtask queue
```

Node.js runtime includes:

```text
JavaScript engine
libuv
file system APIs
networking APIs
timers
worker thread support
process APIs
task queues
microtask queue
```

Important distinction:

```text
setTimeout, fetch, DOM events, file system APIs, and process.nextTick are not all pure language
features. Many are runtime APIs.
```

Strong answer:

```text
The ECMAScript language defines promises and job queues at the language level, but practical
async behavior also depends on the host environment, such as the browser or Node.js.
```

---

## 4. Synchronous Execution

JavaScript runs synchronous code first.

```javascript
console.log("A");
console.log("B");
console.log("C");
```

Output:

```text
A
B
C
```

The call stack tracks active function calls.

```javascript
function c() {
    console.log("C");
}

function b() {
    c();
}

function a() {
    b();
}

a();
```

Stack flow:

```text
main -> a -> b -> c
c returns
b returns
a returns
main finishes
```

Rule:

```text
Nothing async can run while synchronous code is still occupying the call stack.
```

---

## 5. Blocking The Event Loop

Long synchronous work blocks async callbacks, UI events, and rendering.

```javascript
console.log("start");

setTimeout(() => {
    console.log("timer");
}, 0);

const end = Date.now() + 2000;
while (Date.now() < end) {
    // blocking loop
}

console.log("end");
```

Output:

```text
start
end
timer
```

Why:

```text
The timer callback cannot run until the call stack is empty.
```

Production impact:

- Frozen UI.
- Delayed click handling.
- Delayed timers.
- Delayed promise continuation execution.
- Increased event-loop lag in Node.js.
- Timeouts firing late.

Strong interview line:

```text
Timers are not exact execution guarantees. They schedule callbacks after at least the delay,
but the callback still waits for the call stack and scheduling queues.
```

---

## 6. Event Loop Core Rule

High-level browser rule:

```text
Run one task.
Drain all microtasks.
Possibly render.
Run next task.
Drain all microtasks.
Possibly render.
Repeat.
```

A task can be:

- Initial script execution.
- Timer callback.
- DOM event callback.
- Message event callback.
- Network callback.

A microtask can be:

- Promise `.then` / `.catch` / `.finally` reaction.
- `queueMicrotask` callback.
- MutationObserver callback in browsers.

Important:

```text
After each task, the runtime drains the microtask queue before moving to the next task.
```

Strong answer:

```text
Microtasks have priority after the current synchronous task completes. That is why promise
callbacks usually run before setTimeout callbacks scheduled with zero delay.
```

---

## 7. Macrotasks / Tasks

Many people say macrotask. The more spec-friendly term is task.

Common task sources:

```text
script execution
timers
user events
network callbacks
message channel callbacks
postMessage callbacks
some IO callbacks
```

Example:

```javascript
console.log("script start");

setTimeout(() => {
    console.log("timer");
}, 0);

console.log("script end");
```

Output:

```text
script start
script end
timer
```

Why:

```text
The timer callback is scheduled as a later task.
```

---

## 8. Microtasks

Microtasks run after the current synchronous code and before the next task.

```javascript
console.log("A");

Promise.resolve().then(() => {
    console.log("B");
});

console.log("C");
```

Output:

```text
A
C
B
```

Why:

```text
The promise reaction is a microtask. It runs after the current script finishes.
```

Microtask priority over timers:

```javascript
setTimeout(() => console.log("timer"), 0);
Promise.resolve().then(() => console.log("promise"));
console.log("sync");
```

Output:

```text
sync
promise
timer
```

Strong answer:

```text
Promise callbacks do not run immediately. They are scheduled as microtasks and run after the
current synchronous execution completes, before the next task such as a timer.
```

---

## 9. Microtask Drain Rule

The runtime drains the microtask queue completely before moving to the next task.

```javascript
setTimeout(() => console.log("timer"), 0);

Promise.resolve().then(() => {
    console.log("microtask 1");
    Promise.resolve().then(() => console.log("microtask 2"));
});

console.log("sync");
```

Output:

```text
sync
microtask 1
microtask 2
timer
```

Why:

```text
The second microtask is added while draining microtasks, and it is also drained before the timer task.
```

Production caution:

```text
Too many recursively scheduled microtasks can starve tasks and rendering.
```

Microtask starvation example:

```javascript
function loop() {
    queueMicrotask(loop);
}

loop();
```

This can prevent timers and rendering from getting a chance to run.

---

## 10. `queueMicrotask`

`queueMicrotask` explicitly schedules a microtask.

```javascript
console.log("A");

queueMicrotask(() => {
    console.log("B");
});

console.log("C");
```

Output:

```text
A
C
B
```

Why use it:

- Normalize sync/async callback behavior.
- Run code after current call stack.
- Avoid creating a Promise just for scheduling.

Example:

```javascript
function notifyLater(listener, value) {
    queueMicrotask(() => listener(value));
}
```

Interview line:

```text
queueMicrotask schedules a callback in the microtask queue. It runs after current synchronous
code but before the next task.
```

---

## 11. Promise States

A promise has three states:

```text
pending
fulfilled
rejected
```

Once settled, a promise does not change state.

```javascript
const promise = new Promise((resolve, reject) => {
    resolve("done");
    reject(new Error("ignored"));
    resolve("ignored too");
});

promise.then(value => console.log(value));
```

Output:

```text
done
```

Rule:

```text
First settlement wins.
```

Strong answer:

```text
A promise represents a future result. It starts pending and eventually becomes fulfilled or
rejected. After it settles, its state and result are fixed.
```

---

## 12. Promise Executor Is Synchronous

The function passed to `new Promise` runs immediately and synchronously.

```javascript
console.log("A");

const promise = new Promise(resolve => {
    console.log("B");
    resolve("C");
});

promise.then(value => console.log(value));

console.log("D");
```

Output:

```text
A
B
D
C
```

Why:

```text
The executor logs B synchronously. The then callback is a microtask.
```

Common trap:

```javascript
new Promise(() => {
    console.log("executor");
});
```

This logs immediately even if no `.then` is attached.

Interview line:

```text
Promise construction is synchronous, but promise reactions attached with then/catch/finally run
as microtasks after the current stack completes.
```

---

## 13. `.then` Chaining

`.then` returns a new promise.

```javascript
Promise.resolve(1)
    .then(value => value + 1)
    .then(value => value * 2)
    .then(value => console.log(value));
```

Output:

```text
4
```

If a `.then` returns a promise, the chain waits for it.

```javascript
Promise.resolve("A")
    .then(value => {
        return new Promise(resolve => {
            setTimeout(() => resolve(value + "B"), 10);
        });
    })
    .then(value => console.log(value));
```

Output:

```text
AB
```

If a `.then` throws, the returned promise rejects.

```javascript
Promise.resolve()
    .then(() => {
        throw new Error("failed");
    })
    .catch(error => console.log(error.message));
```

Output:

```text
failed
```

Strong answer:

```text
Promise chains work because each then returns a new promise. Returning a value fulfills the next
promise with that value, returning a promise unwraps it, and throwing rejects the next promise.
```

---

## 14. `.catch`

`.catch` handles rejection.

```javascript
fetchUser()
    .then(user => fetchBookings(user.id))
    .catch(error => {
        console.error("failed", error);
    });
```

`.catch(fn)` is similar to `.then(undefined, fn)`.

Recovery example:

```javascript
Promise.reject(new Error("network"))
    .catch(() => [])
    .then(bookings => console.log(bookings.length));
```

Output:

```text
0
```

Important:

```text
If catch returns a normal value, the chain becomes fulfilled again.
```

Rethrow when caller must know:

```javascript
fetchBookings()
    .catch(error => {
        logError(error);
        throw error;
    });
```

Interview line:

```text
catch can either recover by returning a fallback value or propagate failure by throwing or
returning a rejected promise.
```

---

## 15. `.finally`

`.finally` runs cleanup regardless of fulfillment or rejection.

```javascript
setLoading(true);

fetchBookings()
    .then(renderBookings)
    .catch(showError)
    .finally(() => setLoading(false));
```

Important behavior:

```javascript
Promise.resolve("value")
    .finally(() => "ignored")
    .then(value => console.log(value));
```

Output:

```text
value
```

`finally` does not replace the value unless it throws or returns a rejected promise.

```javascript
Promise.resolve("value")
    .finally(() => {
        throw new Error("cleanup failed");
    })
    .catch(error => console.log(error.message));
```

Output:

```text
cleanup failed
```

Strong answer:

```text
finally is for cleanup. It normally passes through the original result or error, but if finally
throws, that new error replaces the chain outcome.
```

---

## 16. `async` Functions

An `async` function always returns a promise.

```javascript
async function getValue() {
    return 10;
}

console.log(getValue());
```

Output idea:

```text
Promise fulfilled with 10
```

Use:

```javascript
getValue().then(value => console.log(value)); // 10
```

Throwing inside async function rejects the returned promise.

```javascript
async function fail() {
    throw new Error("boom");
}

fail().catch(error => console.log(error.message));
```

Output:

```text
boom
```

Strong answer:

```text
async marks a function as promise-returning. Returning a value fulfills the promise, and throwing
an error rejects it.
```

---

## 17. `await`

`await` pauses the async function until the awaited value settles.

```javascript
async function run() {
    console.log("A");
    await Promise.resolve();
    console.log("B");
}

run();
console.log("C");
```

Output:

```text
A
C
B
```

Why:

```text
After await, the continuation is scheduled through promise/microtask behavior.
```

Awaiting a non-promise wraps it like a resolved promise.

```javascript
async function run() {
    const value = await 10;
    console.log(value);
}
```

Output:

```text
10
```

Strong answer:

```text
await pauses only the async function, not the whole JavaScript runtime. Other synchronous code
continues, and the function resumes later through the promise job queue.
```

---

## 18. `async` / `await` Error Handling

Use `try/catch` inside async functions.

```javascript
async function loadBookings() {
    try {
        const response = await fetch("/api/bookings");
        return await response.json();
    } catch (error) {
        throw new Error("failed to load bookings", { cause: error });
    }
}
```

Caller must handle rejection:

```javascript
loadBookings().catch(error => {
    console.error(error.message);
});
```

Common mistake:

```javascript
try {
    loadBookings();
} catch (error) {
    console.log("will not catch async rejection");
}
```

Why wrong:

```text
The promise rejection happens asynchronously. Without await or catch, the surrounding try/catch
will not catch it.
```

Correct:

```javascript
try {
    await loadBookings();
} catch (error) {
    console.log("caught");
}
```

---

## 19. Output Order Method

For output questions, use this process:

1. Mark all synchronous logs.
2. Mark promise reactions and `queueMicrotask` as microtasks.
3. Mark timers and events as tasks.
4. Remember promise executor runs synchronously.
5. Finish synchronous code first.
6. Drain microtasks in order.
7. Run next task.
8. Drain microtasks after that task.
9. Repeat.

Checklist:

```text
sync first
promise executor sync
then/catch/finally microtasks
timer callbacks later tasks
microtasks drain fully before next task
async function before first await is sync
after await is continuation microtask-like behavior
```

---

## 20. Output Trap 1: Basic Timer vs Promise

```javascript
console.log("A");

setTimeout(() => console.log("B"), 0);

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

Output:

```text
A
D
C
B
```

Reason:

```text
A and D are sync. C is microtask. B is timer task.
```

---

## 21. Output Trap 2: Promise Executor

```javascript
console.log("A");

new Promise(resolve => {
    console.log("B");
    resolve();
}).then(() => console.log("C"));

console.log("D");
```

Output:

```text
A
B
D
C
```

Reason:

```text
The executor runs immediately. The then callback is a microtask.
```

---

## 22. Output Trap 3: Nested Microtasks

```javascript
Promise.resolve().then(() => {
    console.log("A");
    Promise.resolve().then(() => console.log("B"));
});

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

Output:

```text
D
A
C
B
```

Why:

```text
Initial microtask queue: A callback, C callback. While A runs, it enqueues B at the end.
```

---

## 23. Output Trap 4: Async Function

```javascript
async function run() {
    console.log("A");
    await Promise.resolve();
    console.log("B");
}

console.log("C");
run();
console.log("D");
```

Output:

```text
C
A
D
B
```

Why:

```text
run starts synchronously until await. Continuation after await runs later.
```

---

## 24. Output Trap 5: Await Non-Promise

```javascript
async function run() {
    console.log("A");
    await 1;
    console.log("B");
}

run();
console.log("C");
```

Output:

```text
A
C
B
```

Why:

```text
await converts non-promises through promise resolution behavior, so continuation still happens later.
```

---

## 25. Output Trap 6: Timer Creates Microtask

```javascript
setTimeout(() => {
    console.log("timer 1");
    Promise.resolve().then(() => console.log("promise in timer"));
}, 0);

setTimeout(() => {
    console.log("timer 2");
}, 0);
```

Typical output:

```text
timer 1
promise in timer
timer 2
```

Why:

```text
After timer 1 task finishes, microtasks are drained before timer 2 task runs.
```

---

## 26. Output Trap 7: `finally`

```javascript
Promise.resolve("A")
    .finally(() => console.log("B"))
    .then(value => console.log(value));

console.log("C");
```

Output:

```text
C
B
A
```

Reason:

```text
C is sync. finally runs as a promise reaction. It passes through the original value.
```

---

## 27. Output Trap 8: Rejection Recovery

```javascript
Promise.reject("A")
    .catch(value => {
        console.log(value);
        return "B";
    })
    .then(value => console.log(value));

console.log("C");
```

Output:

```text
C
A
B
```

Reason:

```text
catch handles rejection and returns B, so the next then receives B.
```

---

## 28. Sequential vs Parallel Async Work

Sequential:

```javascript
const user = await fetchUser(userId);
const bookings = await fetchBookings(user.id);
```

Use when second call depends on first.

Parallel:

```javascript
const [user, settings] = await Promise.all([
    fetchUser(userId),
    fetchSettings(userId)
]);
```

Use when calls are independent.

Common mistake:

```javascript
const user = await fetchUser(userId);
const settings = await fetchSettings(userId);
```

If independent, this adds unnecessary latency.

Strong answer:

```text
I run async operations sequentially only when there is a dependency. If operations are independent,
I start them together and await them with Promise.all or another combinator.
```

---

## 29. Promise.all

`Promise.all` runs promises concurrently and fails fast.

```javascript
const [user, bookings, payments] = await Promise.all([
    fetchUser(userId),
    fetchBookings(userId),
    fetchPayments(userId)
]);
```

If one rejects, `Promise.all` rejects.

```javascript
try {
    await Promise.all([taskA(), taskB(), taskC()]);
} catch (error) {
    console.log("at least one failed");
}
```

Important:

```text
Promise.all does not cancel the other operations automatically when one rejects.
```

Strong answer:

```text
Promise.all is best for independent async operations where all results are required. It rejects
as soon as one input rejects, but it does not automatically cancel work already started.
```

---

## 30. Promise.allSettled

`Promise.allSettled` waits for every promise to settle.

```javascript
const results = await Promise.allSettled([
    fetchProfile(userId),
    fetchBookings(userId),
    fetchRecommendations(userId)
]);

const successful = results
    .filter(result => result.status === "fulfilled")
    .map(result => result.value);
```

Result shape:

```javascript
{ status: "fulfilled", value: data }
{ status: "rejected", reason: error }
```

Use when partial success is acceptable.

Strong answer:

```text
I use allSettled when I care about every outcome and do not want one failure to hide the rest,
such as dashboard widgets or batch processing reports.
```

---

## 31. Promise.race

`Promise.race` settles with the first settled promise.

```javascript
const result = await Promise.race([
    fetchData(),
    timeoutAfter(3000)
]);
```

Timeout helper:

```javascript
function timeoutAfter(ms) {
    return new Promise((_, reject) => {
        setTimeout(() => reject(new Error("timeout")), ms);
    });
}
```

Caution:

```text
Promise.race does not cancel the losing operation. Use AbortController or explicit cancellation
when the underlying API supports it.
```

---

## 32. Promise.any

`Promise.any` fulfills with the first fulfilled promise.

```javascript
const result = await Promise.any([
    fetchFromReplica("a"),
    fetchFromReplica("b"),
    fetchFromReplica("c")
]);
```

If all reject, it rejects with `AggregateError`.

```javascript
try {
    await Promise.any([taskA(), taskB()]);
} catch (error) {
    if (error instanceof AggregateError) {
        console.log(error.errors);
    }
}
```

Strong answer:

```text
Promise.any is useful when any successful result is enough. It ignores rejections until all
inputs reject, then fails with AggregateError.
```

---

## 33. Async In Loops

Sequential loop:

```javascript
for (const id of ids) {
    const user = await fetchUser(id);
    console.log(user.name);
}
```

This is intentionally sequential.

Parallel:

```javascript
const users = await Promise.all(ids.map(id => fetchUser(id)));
```

Mistake:

```javascript
ids.forEach(async id => {
    await fetchUser(id);
});
```

Why wrong:

```text
forEach does not await async callbacks. The outer function continues immediately.
```

Better sequential:

```javascript
for (const id of ids) {
    await fetchUser(id);
}
```

Better parallel:

```javascript
await Promise.all(ids.map(id => fetchUser(id)));
```

Interview line:

```text
I avoid async forEach when I need control flow. I use for...of for sequential awaits and
Promise.all with map for parallel work.
```

---

## 34. Concurrency vs Parallelism

Concurrency:

```text
Multiple tasks are in progress during overlapping time periods.
```

Parallelism:

```text
Multiple tasks execute at the exact same time on different cores or workers.
```

JavaScript main thread:

```text
One call stack executes JS at a time, but many async operations can be in flight concurrently.
```

Node.js:

```text
IO can be concurrent through the runtime and OS. CPU-heavy JS still blocks the main event loop
unless moved to workers or native/libuv threadpool operations.
```

Strong answer:

```text
JavaScript can handle concurrent async IO efficiently, but CPU-heavy JavaScript on the main
thread blocks the event loop. Parallel CPU work requires workers or external services.
```

---

## 35. Concurrency Limit Pattern

Unbounded concurrency can overload services.

Bad:

```javascript
await Promise.all(userIds.map(id => fetchUser(id)));
```

If `userIds` has 50,000 items, this may create an API storm.

Simple concurrency limiter:

```javascript
async function mapWithConcurrency(items, limit, mapper) {
    const results = new Array(items.length);
    let nextIndex = 0;

    async function worker() {
        while (nextIndex < items.length) {
            const currentIndex = nextIndex;
            nextIndex++;
            results[currentIndex] = await mapper(items[currentIndex], currentIndex);
        }
    }

    const workers = Array.from(
        { length: Math.min(limit, items.length) },
        () => worker()
    );

    await Promise.all(workers);
    return results;
}
```

Usage:

```javascript
const users = await mapWithConcurrency(userIds, 5, id => fetchUser(id));
```

Strong answer:

```text
For large async batches, I avoid unbounded Promise.all. I use a concurrency limit so the system
can make progress without overwhelming downstream services, memory, or network resources.
```

---

## 36. Cancellation With AbortController

JavaScript promises do not cancel by themselves.

For APIs that support cancellation, use `AbortController`.

```javascript
const controller = new AbortController();

const request = fetch("/api/bookings", {
    signal: controller.signal
});

controller.abort();

try {
    await request;
} catch (error) {
    if (error.name === "AbortError") {
        console.log("request aborted");
    }
}
```

Use cases:

- User navigates away.
- Search query changes.
- Request exceeds timeout.
- Component unmounts.
- Backend no longer needs work.

Strong answer:

```text
Promises represent outcomes but do not inherently cancel work. Cancellation requires cooperation
from the underlying API, commonly through AbortController for fetch and compatible APIs.
```

---

## 37. Timeout With AbortController

Timeout should cancel the underlying work when possible.

```javascript
async function fetchWithTimeout(url, ms) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), ms);

    try {
        const response = await fetch(url, { signal: controller.signal });
        return response;
    } finally {
        clearTimeout(timeoutId);
    }
}
```

Usage:

```javascript
try {
    const response = await fetchWithTimeout("/api/bookings", 3000);
    console.log(response.status);
} catch (error) {
    if (error.name === "AbortError") {
        console.log("timed out or aborted");
    }
}
```

Why `finally` matters:

```text
It clears the timer if the request finishes before timeout.
```

Production line:

```text
A timeout that only rejects a wrapper promise but does not cancel underlying IO can leave work
running in the background.
```

---

## 38. Retry With Backoff

Retry transient failures, not every failure.

```javascript
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function retry(operation, options = {}) {
    const {
        attempts = 3,
        baseDelayMs = 100,
        shouldRetry = () => true
    } = options;

    let lastError;

    for (let attempt = 1; attempt <= attempts; attempt++) {
        try {
            return await operation(attempt);
        } catch (error) {
            lastError = error;

            if (attempt === attempts || !shouldRetry(error)) {
                throw error;
            }

            const backoff = baseDelayMs * 2 ** (attempt - 1);
            await delay(backoff);
        }
    }

    throw lastError;
}
```

Usage:

```javascript
const result = await retry(
    () => fetchWithTimeout("/api/bookings", 3000),
    {
        attempts: 4,
        baseDelayMs: 200,
        shouldRetry: error => error.name === "AbortError"
    }
);
```

Production cautions:

- Do not retry validation errors.
- Do not retry non-idempotent writes blindly.
- Add jitter in distributed systems.
- Respect rate limits.
- Use circuit breakers or bulkheads for persistent failures.

Strong answer:

```text
Retries should be limited, delayed, and used only for transient failures. For production systems,
I add backoff, jitter, idempotency protection, observability, and clear stop conditions.
```

---

## 39. Race Conditions In UI

Search example:

```javascript
let latestRequestId = 0;

async function search(query) {
    const requestId = ++latestRequestId;
    const results = await fetchSearchResults(query);

    if (requestId !== latestRequestId) {
        return;
    }

    renderResults(results);
}
```

Problem:

```text
User types "ja", then "java". The "java" request may finish first. Later, the older "ja"
request may finish and overwrite the UI with stale data.
```

Protection:

```text
Track request identity or cancel older requests.
```

Abort version:

```javascript
let currentController;

async function search(query) {
    currentController?.abort();
    currentController = new AbortController();

    try {
        const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`, {
            signal: currentController.signal
        });
        renderResults(await response.json());
    } catch (error) {
        if (error.name !== "AbortError") {
            showError(error);
        }
    }
}
```

Strong answer:

```text
Async race conditions happen when older work completes after newer work and overwrites current
state. I prevent it with cancellation, request IDs, or state-machine checks.
```

---

## 40. Debounce

Debounce waits until activity stops.

```javascript
function debounce(fn, delayMs) {
    let timeoutId;

    return function debounced(...args) {
        clearTimeout(timeoutId);

        timeoutId = setTimeout(() => {
            fn.apply(this, args);
        }, delayMs);
    };
}
```

Use case:

```javascript
const onSearchInput = debounce(event => {
    search(event.target.value);
}, 300);
```

Good for:

- Search input.
- Window resize end.
- Autosave after typing pauses.

Strong answer:

```text
Debounce delays execution until calls stop for a period. It reduces unnecessary work for noisy
events like typing.
```

---

## 41. Throttle

Throttle runs at most once per interval.

```javascript
function throttle(fn, intervalMs) {
    let lastRun = 0;
    let trailingTimeoutId;

    return function throttled(...args) {
        const now = Date.now();
        const remaining = intervalMs - (now - lastRun);

        if (remaining <= 0) {
            clearTimeout(trailingTimeoutId);
            trailingTimeoutId = undefined;
            lastRun = now;
            fn.apply(this, args);
            return;
        }

        if (!trailingTimeoutId) {
            trailingTimeoutId = setTimeout(() => {
                lastRun = Date.now();
                trailingTimeoutId = undefined;
                fn.apply(this, args);
            }, remaining);
        }
    };
}
```

Use cases:

- Scroll handler.
- Mousemove handler.
- Resize while dragging.
- Rate-limited progress updates.

Strong answer:

```text
Throttle limits how often a function runs during continuous activity. Debounce waits until the
activity pauses.
```

---

## 42. Browser Rendering And Async

Browser simplified frame loop:

```text
handle a task
run microtasks
style/layout/paint opportunity
next task
```

Important:

```text
Long tasks delay rendering.
```

Example:

```javascript
button.addEventListener("click", () => {
    button.textContent = "Saving...";

    const end = Date.now() + 2000;
    while (Date.now() < end) {
        // blocks rendering
    }
});
```

The UI may not visibly update until after the blocking work finishes.

Better:

```javascript
button.addEventListener("click", async () => {
    button.textContent = "Saving...";
    await new Promise(resolve => setTimeout(resolve, 0));
    await save();
});
```

Even better for CPU-heavy work:

```text
Move CPU-heavy work to Web Workers or chunk it into smaller tasks.
```

Strong answer:

```text
In the browser, async scheduling affects responsiveness. If JavaScript blocks the main thread,
user input and rendering are delayed even if timers or promises are scheduled.
```

---

## 43. Splitting Long Work

Chunk long work to avoid blocking.

```javascript
async function processInChunks(items, processItem, chunkSize = 100) {
    for (let index = 0; index < items.length; index += chunkSize) {
        const chunk = items.slice(index, index + chunkSize);

        for (const item of chunk) {
            processItem(item);
        }

        await new Promise(resolve => setTimeout(resolve, 0));
    }
}
```

Why:

```text
The zero-delay timer yields back to the event loop between chunks, giving other tasks and
rendering chances to run.
```

Caution:

```text
For serious CPU-heavy work, chunking helps responsiveness but does not create true parallel CPU
execution. Use Web Workers or worker threads when needed.
```

---

## 44. Node.js Event Loop Awareness

Node.js has event loop phases managed by libuv.

Simplified phases:

```text
timers
pending callbacks
idle/prepare
poll
check
close callbacks
```

Common scheduling APIs:

| API | General Behavior |
|---|---|
| `setTimeout(fn, 0)` | Timer phase after minimum delay |
| `setImmediate(fn)` | Check phase |
| `process.nextTick(fn)` | Runs before promise microtasks in Node-specific nextTick queue |
| `Promise.then(fn)` | Microtask queue |
| `queueMicrotask(fn)` | Microtask queue |

Important:

```text
Node scheduling details can vary depending on context, such as top-level code vs inside IO callbacks.
```

Strong answer:

```text
Node.js uses libuv event-loop phases. For most interviews, I explain timers, poll, check,
promises, process.nextTick, and the risk of blocking the event loop with CPU-heavy JavaScript.
```

---

## 45. `process.nextTick`

`process.nextTick` is Node-specific.

```javascript
console.log("A");

process.nextTick(() => console.log("B"));
Promise.resolve().then(() => console.log("C"));

console.log("D");
```

Typical Node output:

```text
A
D
B
C
```

Why:

```text
Node processes the nextTick queue before the promise microtask queue.
```

Caution:

```javascript
function loop() {
    process.nextTick(loop);
}

loop();
```

This can starve the event loop.

Interview line:

```text
process.nextTick is Node-specific and has very high priority. Overusing it can starve IO, timers,
and normal microtasks.
```

---

## 46. `setImmediate`

`setImmediate` is Node-specific and runs in the check phase.

```javascript
setImmediate(() => console.log("immediate"));
setTimeout(() => console.log("timeout"), 0);
```

At top level, exact ordering can be environment-dependent.

Inside IO callback, `setImmediate` usually runs before zero-delay timer scheduled there.

```javascript
const fs = require("node:fs");

fs.readFile(__filename, () => {
    setTimeout(() => console.log("timeout"), 0);
    setImmediate(() => console.log("immediate"));
});
```

Typical Node output inside IO:

```text
immediate
timeout
```

Strong answer:

```text
setImmediate is a Node scheduling API for the check phase. Its ordering with setTimeout(0) can
depend on context, so I avoid relying on it unless I specifically need Node event-loop behavior.
```

---

## 47. Unhandled Rejections

Unhandled promise rejection:

```javascript
async function fail() {
    throw new Error("boom");
}

fail();
```

Problem:

```text
The returned promise rejects, but no one handles it.
```

Correct:

```javascript
fail().catch(error => {
    console.error(error);
});
```

Or:

```javascript
try {
    await fail();
} catch (error) {
    console.error(error);
}
```

Production caution:

```text
Unhandled rejection behavior depends on runtime and configuration. Treat unhandled rejections
as serious bugs.
```

Node process-level awareness:

```javascript
process.on("unhandledRejection", error => {
    console.error("Unhandled rejection", error);
});
```

Strong answer:

```text
Every promise chain should be returned or awaited, and every async boundary should have a clear
error-handling strategy. Unhandled rejections are production defects.
```

---

## 48. Fire-And-Forget Work

Sometimes work is intentionally not awaited.

Bad silent fire-and-forget:

```javascript
sendAuditEvent(event);
```

If it rejects, the error may be unhandled.

Better:

```javascript
void sendAuditEvent(event).catch(error => {
    logger.warn("audit event failed", { error });
});
```

Production caution:

```text
Fire-and-forget should still have error handling, observability, and lifecycle ownership.
```

Strong answer:

```text
If I intentionally do not await async work, I still attach rejection handling and make the
ownership explicit. Silent floating promises are risky.
```

---

## 49. Async API Design

Good async API design:

```javascript
async function reserveRoom({ roomId, guestId, signal }) {
    validateReservationInput({ roomId, guestId });

    const response = await fetch("/api/reservations", {
        method: "POST",
        body: JSON.stringify({ roomId, guestId }),
        signal
    });

    if (!response.ok) {
        throw new Error(`reservation failed: ${response.status}`);
    }

    return response.json();
}
```

Good traits:

- Accepts an options object.
- Supports cancellation with `signal`.
- Throws useful errors.
- Does not swallow failures.
- Returns parsed result.
- Keeps validation at boundary.

Interview line:

```text
A production async API should make cancellation, timeout, errors, and ownership explicit. It
should not hide failures or create unbounded background work.
```

---

## 50. Mini Program: Async Output Order Simulator

This small helper records output order while mixing sync, microtasks, and tasks.

```javascript
function runOutputOrderDemo() {
    const logs = [];
    const log = value => logs.push(value);

    log("sync 1");

    setTimeout(() => {
        log("timer 1");
        Promise.resolve().then(() => log("promise inside timer"));
    }, 0);

    queueMicrotask(() => log("queueMicrotask 1"));

    Promise.resolve()
        .then(() => {
            log("promise 1");
            queueMicrotask(() => log("nested microtask"));
        })
        .then(() => log("promise 2"));

    log("sync 2");

    setTimeout(() => {
        log("timer 2");
        console.log(logs);
    }, 0);
}

runOutputOrderDemo();
```

Expected order idea:

```text
sync 1
sync 2
queueMicrotask 1
promise 1
nested microtask
promise 2
timer 1
promise inside timer
timer 2
```

Note:

```text
The exact final console array appears when timer 2 runs. Timer order follows scheduling order
for these two timers in typical environments, and microtasks drain after timer 1 before timer 2.
```

---

## 51. Mini Program: Reliable Fetch Helper

This helper combines timeout, cancellation, response validation, and JSON parsing.

```javascript
async function fetchJson(url, options = {}) {
    const {
        timeoutMs = 5000,
        signal,
        ...fetchOptions
    } = options;

    const timeoutController = new AbortController();
    const timeoutId = setTimeout(() => timeoutController.abort(), timeoutMs);

    const combinedSignal = signal
        ? AbortSignal.any([signal, timeoutController.signal])
        : timeoutController.signal;

    try {
        const response = await fetch(url, {
            ...fetchOptions,
            signal: combinedSignal
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
    } finally {
        clearTimeout(timeoutId);
    }
}
```

Compatibility caution:

```text
AbortSignal.any is not available in every older runtime. If unavailable, compose signals manually
or use a project-approved helper.
```

Why this is strong:

- Has timeout.
- Supports caller cancellation.
- Clears timer.
- Checks HTTP status.
- Returns parsed JSON.
- Keeps failure visible.

---

## 52. Mini Program: Async Pool With Error Collection

Use this when partial results matter.

```javascript
async function mapSettledWithConcurrency(items, limit, mapper) {
    const results = new Array(items.length);
    let nextIndex = 0;

    async function worker() {
        while (nextIndex < items.length) {
            const currentIndex = nextIndex;
            nextIndex++;

            try {
                results[currentIndex] = {
                    status: "fulfilled",
                    value: await mapper(items[currentIndex], currentIndex)
                };
            } catch (error) {
                results[currentIndex] = {
                    status: "rejected",
                    reason: error
                };
            }
        }
    }

    const workers = Array.from(
        { length: Math.min(limit, items.length) },
        () => worker()
    );

    await Promise.all(workers);
    return results;
}
```

Usage:

```javascript
const results = await mapSettledWithConcurrency(userIds, 10, fetchUser);
```

Production value:

```text
This avoids unbounded concurrency and still preserves per-item success/failure results.
```

---

## 53. Common Mistakes

| Mistake | Why It Fails | Better Approach |
|---|---|---|
| Thinking `setTimeout(fn, 0)` runs immediately | It runs in a later task | Remember sync and microtasks first |
| Forgetting promise executor is sync | Output order wrong | Executor runs immediately |
| Using `forEach` with async when awaiting matters | Outer flow does not wait | Use `for...of` or `Promise.all` |
| Using unbounded `Promise.all` on huge arrays | API/memory storm | Add concurrency limit |
| Catching async errors with sync try/catch but no await | Rejection not caught | `await` inside try/catch or attach `.catch` |
| Swallowing errors in catch | Hides production failures | Recover intentionally or rethrow |
| Assuming `Promise.race` cancels losers | It does not | Use AbortController if supported |
| Retrying all failures | Can worsen incidents | Retry only transient/idempotent operations |
| Blocking event loop with CPU loop | Delays everything | Worker/chunk/optimize |
| Overusing `process.nextTick` | Starves IO/microtasks | Use carefully in Node |
| Optional fire-and-forget without catch | Unhandled rejection | Attach catch and log |
| Ignoring stale async responses | UI shows old data | Use request IDs or cancellation |

---

## 54. Strong Interview Answers

### Event Loop

```text
The event loop coordinates when asynchronous callbacks run. JavaScript runs synchronous code on
the call stack first. When the stack is empty, promise microtasks are drained before the runtime
moves to the next task such as a timer, UI event, or IO callback.
```

### Microtask vs Macrotask

```text
Promise reactions and queueMicrotask callbacks are microtasks. Timers and many UI or IO callbacks
are tasks/macrotasks. After each task, the runtime drains microtasks before running the next task.
```

### Promise Executor

```text
The promise executor runs synchronously when the promise is created. The callbacks attached with
then, catch, or finally run later as promise reactions.
```

### Async/Await

```text
async/await is syntax over promises. An async function always returns a promise. Code before the
first await runs synchronously, and the continuation after await resumes later through promise
job scheduling.
```

### Promise.all

```text
Promise.all is useful for independent operations where every result is required. It starts work
concurrently if the promises are created immediately, rejects on first failure, and does not
cancel the other operations automatically.
```

### Production Async

```text
Production async code needs timeouts, cancellation, bounded concurrency, error handling,
observability, and stale-result protection. Writing await is not enough; we need to control
how async work behaves under latency, failure, and load.
```

---

## 55. FAANG-Level Question 1

> A page has a search box. Every keystroke calls an API. Users report stale results, UI freezes, and occasional rate-limit errors. How would you fix it?

Strong answer:

```text
I would separate the problems. For too many API calls, I would debounce the search input so the
API is called only after typing pauses. For stale results, I would cancel previous requests with
AbortController or track request IDs so older responses cannot overwrite newer state.

For UI freezing, I would check for long synchronous work in rendering, result processing, or
highlighting. If processing is heavy, I would chunk it, move it to a Web Worker, or reduce the
amount of data rendered at once with pagination or virtualization.

For rate limits, I would add client-side throttling, backend-supported pagination, server-side
rate-limit handling, and proper retry behavior only for transient failures. I would also add
loading states, error states, metrics, and logs so we can see request volume, latency, aborts,
and failures.
```

This answer shows:

- Debounce knowledge.
- Cancellation/stale-result protection.
- Event-loop blocking awareness.
- Rate-limit maturity.
- Production observability.

---

## 56. FAANG-Level Question 2

> A Node.js API has high latency under load even though the database is healthy. CPU spikes and timers fire late. What do you suspect?

Strong answer:

```text
I would suspect event-loop blocking in the Node.js process. If the database is healthy but timers
fire late and latency rises with CPU spikes, the main JavaScript thread may be doing expensive
synchronous work such as JSON serialization, crypto, compression, regex processing, large loops,
or heavy logging.

I would measure event-loop lag, CPU profiles, request traces, and heap usage. Then I would look
for synchronous hotspots and either optimize them, stream the work, move CPU-heavy work to worker
threads or another service, add backpressure, or reduce concurrency.

I would also check unbounded Promise.all patterns, retry storms, memory pressure causing GC
pauses, and large payload processing. Node is strong for IO-heavy workloads, but CPU-heavy JS can
block the event loop and delay every request.
```

This answer shows:

- Node event-loop understanding.
- Debugging maturity.
- Production metrics awareness.
- Practical mitigation options.

---

## 57. Rapid Revision

- JavaScript executes synchronous code on the call stack first.
- Async callbacks cannot run while the stack is busy.
- The event loop coordinates tasks and microtasks.
- Promise reactions are microtasks.
- `queueMicrotask` schedules a microtask.
- Timers schedule later tasks.
- Microtasks drain before the next task.
- Promise executor runs synchronously.
- `.then` returns a new promise.
- Returning a value from `.then` fulfills the next promise.
- Throwing inside `.then` rejects the next promise.
- `.catch` can recover or rethrow.
- `.finally` is for cleanup and usually passes through the original result.
- `async` functions always return promises.
- Code before the first `await` runs synchronously.
- `await` pauses the async function, not the whole runtime.
- Awaiting a non-promise still resumes later.
- `try/catch` catches async failures only when using `await` inside the try.
- `Promise.all` fails fast and does not cancel other operations.
- `Promise.allSettled` waits for every outcome.
- `Promise.race` settles with the first settled promise.
- `Promise.any` fulfills with the first fulfilled promise.
- Avoid async `forEach` when control flow matters.
- Use `for...of` for sequential awaits.
- Use `Promise.all` for independent parallel work.
- Use concurrency limits for large batches.
- Promises do not inherently cancel work.
- Use AbortController when the underlying API supports cancellation.
- Timeouts should clear timers and cancel underlying work when possible.
- Retries need limits, backoff, jitter, and idempotency awareness.
- Stale async results cause UI race conditions.
- Debounce waits until activity stops.
- Throttle limits execution frequency.
- Long synchronous work blocks rendering and async callbacks.
- Browser rendering can be delayed by long tasks.
- Node.js uses libuv event-loop phases.
- `process.nextTick` is Node-specific and very high priority.
- `setImmediate` is Node-specific and context-sensitive.
- Unhandled rejections are production bugs.
- Fire-and-forget work still needs error handling.

---

## 58. Official Source Notes

Use these sources when refreshing async/event-loop details:

- ECMAScript specification jobs and execution: `https://tc39.es/ecma262/`
- MDN Event loop: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Event_loop`
- MDN Promise: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise`
- MDN async function: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/async_function`
- MDN await: `https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/await`
- MDN queueMicrotask: `https://developer.mozilla.org/en-US/docs/Web/API/queueMicrotask`
- MDN setTimeout: `https://developer.mozilla.org/en-US/docs/Web/API/setTimeout`
- MDN AbortController: `https://developer.mozilla.org/en-US/docs/Web/API/AbortController`
- MDN Fetch API: `https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API`
- Node.js event loop guide: `https://nodejs.org/en/learn/asynchronous-work/event-loop-timers-and-nexttick`
- Node.js process.nextTick: `https://nodejs.org/api/process.html#processnexttickcallback-args`
- Node.js timers: `https://nodejs.org/api/timers.html`
- web.dev long tasks: `https://web.dev/articles/long-tasks-devtools`

Interview safety line:

```text
I explain async JavaScript by separating language-level promises and jobs from host runtime APIs.
Then I reason through sync code, microtasks, tasks, rendering or Node phases, and production
concerns like cancellation, timeout, concurrency, retries, stale responses, and event-loop blocking.
```
