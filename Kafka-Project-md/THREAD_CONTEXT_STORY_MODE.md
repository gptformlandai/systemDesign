# PRE-NG Story Mode: How Data Travels Across Threads, Reactive Chains, and Kafka

## Checklist
- [x] Explain `ThreadLocal`
- [x] Explain Reactor `Context`
- [x] Explain `contextWrite(...)`
- [x] Explain `Mono.deferContextual(...)`
- [x] Explain `doFinally(...)`
- [x] Explain thread switching with `parallel()` + `runOn(Schedulers.boundedElastic())`
- [x] Explain the dedicated Kafka `ExecutorService`
- [x] Connect all concepts to actual PRE-NG classes

---

## 1. The Big Idea in One Line

A single PRE-NG request starts on one thread, may continue on other threads, and still needs to carry **request-scoped data** like:

- `requestId`
- `clientName`
- pipeline timing state

That is why this codebase uses **two different carriers**:

1. **`ThreadLocal`** → good when execution stays on the same thread
2. **Reactor `Context`** → good when reactive execution may hop across threads

---

## 2. Meet the Characters

### Character 1: `ThreadLocal`
Think of `ThreadLocal` as a **small side pocket attached to one thread**.

If thread `T1` stores:

```java
RequestIdContext.set(121265);
```

then later, on that **same thread**, you can do:

```java
Long requestId = RequestIdContext.get();
```

and you get `121265`.

But if execution jumps to another thread, that other thread does **not automatically** see the same pocket.

### Character 2: Reactor `Context`
Think of Reactor `Context` as a **backpack attached to the reactive pipeline**, not to the thread.

That means when Reactor switches threads, the context can still travel with the reactive chain.

This is exactly why `contextWrite(...)` and `Mono.deferContextual(...)` matter.

---

## 3. Where This Story Starts: The Controller

File: `src/main/java/com/optum/prepcp/apps/preng/controllers/MemberProviderRecommendationsController.java`

The request enters here:

```java
public Mono<ResponseEntity<ResponseDto>> memberProviderRecommendations(...)
```

### Step A — Audit creates the real `requestId`

```java
final Long requestId = auditRequestAndLog(request);
```

This is the **existing DB-generated requestId**. We are not inventing a new one.

---

## 4. First Carrying Mechanism: `ThreadLocal`

Immediately after the request is audited, PRE-NG does:

```java
RequestIdContext.set(requestId);
```

This stores the `requestId` inside `RequestIdContext`, which internally uses:

```java
private static final ThreadLocal<Long> requestIdHolder = new ThreadLocal<>();
```

File: `src/main/java/com/optum/prepcp/apps/preng/services/kafka/RequestIdContext.java`

### Why do this?
Because lots of downstream code is written as regular method calls and it is convenient to fetch the `requestId` without passing it through every method signature.

### Simple mental model
- request enters controller
- current thread gets a thread-local value
- downstream synchronous code can read it easily

### But here is the catch
This only works **reliably** as long as execution stays on the same thread.

---

## 5. Why `ThreadLocal` Alone Is Not Enough in WebFlux

PRE-NG is using **Spring WebFlux + Reactor**.

In Reactor:
- work may continue later
- work may run on another thread
- work may be parallelized
- callbacks like `map`, `flatMap`, `doOnNext` may execute after a thread hop

So if you depend only on `ThreadLocal`, you can suddenly get:

```java
RequestIdContext.get() == null
```

not because the requestId never existed,
but because you are now on a **different thread**.

---

## 6. Second Carrying Mechanism: `contextWrite(...)`

In the controller, PRE-NG also does this:

```java
.contextWrite(reactor.util.context.Context.of(
        RequestIdContext.REQUEST_ID_KEY, requestId))
```

### What `contextWrite(...)` really means
It says:

> “For the downstream reactive chain, attach this data to the Reactor Context.”

This is not a normal Java variable.
This is not a thread-local.
This is metadata attached to the **reactive subscription path**.

### Important mental model
`contextWrite(...)` does **not** mutate the current thread.
It enriches the **reactive flow**.

So now PRE-NG has:
- `requestId` in `ThreadLocal`
- `requestId` also in Reactor `Context`

This is a dual-protection approach.

---

## 7. Why `doFinally(...)` Is There

At the end of the controller chain, PRE-NG does:

```java
.doFinally(signalType -> RequestIdContext.clear())
```

### What `doFinally(...)` means
It is Reactor’s “always run cleanup” hook.
It runs when the reactive sequence ends because of:

- success
- error
- cancellation

### Why this matters
If `ThreadLocal` is not cleared, one request’s data can leak into another request handled later by the same reused thread.

That would be extremely dangerous.

### So the story is:
- set requestId at entry
- use it during the request
- always clear it at exit

This is the same idea as `finally { ... }` in regular Java.

---

## 8. The Other ThreadLocal Character: `PipelineTrackerContext`

File: `src/main/java/com/optum/prepcp/apps/preng/services/kafka/PipelineTrackerContext.java`

This class also uses `ThreadLocal`:

```java
private static final ThreadLocal<PipelineTracker> TRACKER = new ThreadLocal<>();
private static final ThreadLocal<long[]> CONTROLLER_TIMINGS = new ThreadLocal<>();
```

### Why another `ThreadLocal`?
Because the code wants many services to record timings like:

- `PODM_LOOKUP`
- `NDAR_LOOKUP`
- `PES_REQUEST_MAPPING`
- `COSMOS_FILTER_PROVIDERS`
- `DB_AUDIT_RESPONSE`

without adding `PipelineTracker tracker` to a huge number of method signatures.

### What it gives us
This lets any downstream code do something like:

```java
PipelineTrackerContext.recordStage("NDAR_LOOKUP", ndarMs);
```

instead of manually passing tracker everywhere.

### Important truth
Just like `RequestIdContext`, this is **thread-bound**, not magically cross-thread.

---

## 9. Where Pipeline Tracking Starts

File: `src/main/java/com/optum/prepcp/apps/preng/services/memberproviderrecommendations/impl/StandardMemberProviderRecommendationsService.java`

At the start of the service flow:

```java
PipelineTracker tracker = latencyIntelligenceService.startPipelineTracking(request.getClientName());
PipelineTrackerContext.set(tracker);
PipelineTrackerContext.replayControllerTimings();
```

### What is happening here?

#### `startPipelineTracking(...)`
Creates a `PipelineTracker` for this request.

#### `PipelineTrackerContext.set(tracker)`
Stores it in thread-local context so many downstream methods can record stages.

#### `replayControllerTimings()`
The controller had already measured some timings before the tracker existed.
So those values were temporarily stored, then replayed into the tracker once the tracker becomes available.

This is a neat pattern:
- controller measures early timings
- service creates tracker later
- stored timings are replayed into the tracker

---

## 10. `contextWrite(...)` for `clientName`

File: `src/main/java/com/optum/prepcp/apps/preng/services/memberproviderrecommendations/impl/MemberRecommendationServiceHelper.java`

Before calling COSMOS, PRE-NG does this:

```java
.contextWrite(reactor.util.context.Context.of("clientName", request.getClientName()));
```

### Why is this interesting?
This is a second example of Reactor Context usage, but for `clientName` instead of `requestId`.

The idea is:

> “I know COSMOS may do async/parallel work, so let me attach `clientName` to the reactive chain itself.”

This is stronger than relying on thread-local when the flow may switch threads.

---

## 11. `Mono.deferContextual(...)` — Reading from Reactor Context

File: `src/main/java/com/optum/prepcp/apps/preng/services/providervalidation/impl/CosmosInvalidProviderRemoverService.java`

COSMOS starts with:

```java
return Mono.deferContextual(ctx -> {
    final String clientName = ctx.getOrDefault("clientName", "N/A").toString();
    ...
});
```

### What `Mono.deferContextual(...)` means
It says:

> “Don’t run this now. Wait until subscription time, then give me the current Reactor Context.”

### Why not just read a variable directly?
Because Reactor Context lives inside the reactive execution model.
You read it properly through `deferContextual`.

### Super simple interpretation
- `contextWrite(...)` = **put into backpack**
- `Mono.deferContextual(...)` = **open backpack and read from it**

### Why this works better than `ThreadLocal` here
Because COSMOS is doing parallel/reactive work where thread switching is expected.
Reactor Context is designed for that world.

---

## 12. The Big Thread Switch: `parallel()` + `runOn(Schedulers.boundedElastic())`

In `CosmosInvalidProviderRemoverService`:

```java
Flux.fromIterable(listOfProviderLists)
    .parallel()
    .runOn(Schedulers.boundedElastic())
```

### What `parallel()` does
It splits the work into multiple rails so chunks can be processed concurrently.

### What `runOn(...)` does
It says which scheduler should execute those rails.

### What `Schedulers.boundedElastic()` is
A Reactor scheduler designed for work that may block or take time.
It is commonly used for:
- blocking I/O
- legacy clients
- heavier background tasks

### Why this matters for context
This is exactly the kind of place where plain `ThreadLocal` becomes unreliable.
Because once the work runs on other worker threads, the original thread’s local storage is no longer guaranteed to follow.

That is why PRE-NG:
- captures `requestId` early before the switch
- uses Reactor Context for `clientName`

---

## 13. The “Capture Before Async Boundary” Pattern

This is used in multiple places.

### PES example
File: `src/main/java/com/optum/prepcp/apps/preng/services/providers/imp/PesProvidersService.java`

```java
final Long requestId = RequestIdContext.get();
```

Comment in code:

> capture requestId BEFORE reactive chain — ThreadLocal may be lost on reactor thread

### DROOLS example
File: `src/main/java/com/optum/prepcp/apps/preng/services/providerordering/imp/DroolsProviderOrderingService.java`

```java
final Long requestId = RequestIdContext.get();
```

### Why do this?
Because once you cross an async boundary, calling `RequestIdContext.get()` later may return `null`.

So the safer pattern is:

1. read from `ThreadLocal` while still on the original thread
2. store it in a normal local variable
3. use that local variable inside later callbacks

### This is a very important technique
It is one of the most practical lessons in reactive systems.

If you know a thread switch may happen, **capture first**.

---

## 14. So When Do We Use Which?

### Use `ThreadLocal` when:
- data is request-scoped
- code is still on the same thread
- you want convenient access without threading parameters everywhere

Examples here:
- `RequestIdContext`
- `PipelineTrackerContext`

### Use Reactor `Context` when:
- you are inside Reactor chains
- thread hopping may happen
- you need data to move with the reactive flow

Examples here:
- `contextWrite(Context.of("clientName", ...))`
- `Mono.deferContextual(ctx -> ...)`

### Use local variable capture when:
- you already have data in `ThreadLocal`
- you are about to cross async/reactive boundaries
- you want to safely retain it

Examples here:
- `final Long requestId = RequestIdContext.get();` in PES/DROOLS/COSMOS

---

## 15. The Kafka Side: Another Async Boundary

File: `src/main/java/com/optum/prepcp/apps/preng/services/kafka/LatencyIntelligenceService.java`

This class has a dedicated:

```java
private final ExecutorService kafkaExecutor = new ThreadPoolExecutor(...)
```

### Why is this important?
Because Kafka-related work is intentionally moved away from the request thread.

That includes:
- payload serialization
- truncation
- JVM metric capture
- actual Kafka send

### Why not do this on `reactor-http-nio`?
Because that would slow the request lifecycle.

### Why not do this on `boundedElastic`?
Because `boundedElastic` is already being used by actual business work like COSMOS parallel calls.
Kafka work should not steal those threads.

So PRE-NG created a **dedicated pool** for Kafka work.

---

## 16. What `ExecutorService.execute(...)` Is Doing

Inside `LatencyIntelligenceService`:

```java
kafkaExecutor.execute(() -> {
    processAndSendEvent(event);
});
```

### Meaning
This submits a task to the Kafka thread pool.
The caller does **not** wait for serialization + send to finish.

That is why this is effectively **fire-and-forget async offload**.

### Important effect
The request thread can keep moving.
Kafka work happens later on a background pool thread.

---

## 17. What `setDaemon(true)` Means — and What It Does NOT Mean

The Kafka thread factory does:

```java
t.setDaemon(true);
```

### What it means
A daemon thread does **not** keep the JVM alive by itself.
If only daemon threads remain, the JVM can shut down.

### What it does NOT mean
It does **not** mean:
- “start in background magically”
- “be async by itself”
- “detach from execution automatically”

The thread becomes background-like because it is created in a pool and tasks are submitted to it.
`setDaemon(true)` is only about **JVM shutdown behavior**.

### So who actually starts the async work?
Not `setDaemon(true)`.

The real async handoff happens here:

```java
kafkaExecutor.execute(...)
```

That is the actual scheduling/submission step.

---

## 18. Why PRE-NG Tries to Grab `requestId` Early for Kafka

In `LatencyIntelligenceService.reportLatency(...)` and `reportLatencyWithRawPayloads(...)`:

```java
if (event.getRequestId() == null) {
    Long contextRequestId = RequestIdContext.get();
    if (contextRequestId != null) {
        event.setRequestId(contextRequestId);
    }
}
```

### Why?
Because once work moves onto the Kafka executor thread, the original `ThreadLocal` is gone.

So PRE-NG copies the `requestId` into the event object **before** the async handoff becomes a problem.

This is again the same design pattern:

> capture what you need before the thread boundary

---

## 19. One Honest Note About the Current Code

There is a method named:

```java
private Long tryGetRequestIdFromReactorContext()
```

but today it effectively does:

```java
return RequestIdContext.get();
```

So despite the name, it is currently reading from `ThreadLocal`, not from Reactor `Context` directly.

### Why this matters
Conceptually, the system supports both models.
But in the current implementation, some requestId handling still relies more on:
- thread-local capture
- passing/copying values early

than on directly reading requestId from Reactor `Context` deep inside the chain.

That is not necessarily wrong.
It is just important to understand the real behavior.

---

## 20. The Full Story in One End-to-End Walkthrough

### Scene 1 — Request enters controller
`MemberProviderRecommendationsController`

- request is audited
- real DB `requestId` is created
- `RequestIdContext.set(requestId)` stores it in `ThreadLocal`
- controller timings are stored via `PipelineTrackerContext.storeControllerTimings(...)`
- `.contextWrite(Context.of("requestId", requestId))` attaches it to Reactor Context
- `.doFinally(...)` ensures cleanup

### Scene 2 — Pipeline tracker begins
`StandardMemberProviderRecommendationsService`

- tracker is created
- tracker is stored in `PipelineTrackerContext`
- controller timings are replayed into it

### Scene 3 — PES call
`PesProvidersService`

- requestId is captured into a normal local variable **before** reactive callbacks
- stage timings are recorded through `PipelineTrackerContext`
- Kafka latency event is built
- raw payload serialization is deferred to Kafka executor

### Scene 4 — DROOLS call
`DroolsProviderOrderingService`

- same pattern: capture requestId early
- record timings
- offload Kafka work

### Scene 5 — COSMOS call
`MemberRecommendationServiceHelper` + `CosmosInvalidProviderRemoverService`

- `clientName` is attached via `contextWrite(...)`
- `Mono.deferContextual(...)` reads it later from Reactor Context
- parallel work runs on `boundedElastic`
- requestId is captured before thread switching

### Scene 6 — Kafka emission
`LatencyIntelligenceService`

- event is queued to `kafkaExecutor`
- serialization happens off the request thread
- Kafka send happens on Kafka pool threads

### Scene 7 — Request ends
Controller `doFinally(...)`

- `RequestIdContext.clear()` prevents thread-local leakage

---

## 21. The Core Lessons You Should Remember

### Lesson 1
`ThreadLocal` is attached to a **thread**, not a request.

### Lesson 2
Reactor `Context` is attached to the **reactive chain**, not to a thread.

### Lesson 3
`contextWrite(...)` is how you **put data into** Reactor Context.

### Lesson 4
`Mono.deferContextual(...)` is how you **read data from** Reactor Context.

### Lesson 5
`doFinally(...)` is the reactive cleanup hook — the equivalent of `finally`.

### Lesson 6
When a thread switch may happen, a very practical pattern is:

```java
final Long requestId = RequestIdContext.get();
```

before the async boundary.

### Lesson 7
`parallel()` + `runOn(boundedElastic())` means work is likely moving across threads.
So plain thread-local assumptions become weaker.

### Lesson 8
`ExecutorService.execute(...)` is the real async handoff for Kafka work.
`setDaemon(true)` is only about shutdown behavior.

---

## 22. Quick Cheat Sheet

### `ThreadLocal`
**Use for:** thread-bound ambient request data  
**Good at:** simple synchronous access  
**Weak at:** thread hops

### `contextWrite(...)`
**Use for:** attaching metadata to a Reactor pipeline  
**Good at:** downstream propagation in reactive flows

### `Mono.deferContextual(...)`
**Use for:** reading Reactor Context lazily at subscription time  
**Good at:** safe context access inside reactive code

### `doFinally(...)`
**Use for:** cleanup regardless of success/error/cancel  
**Good at:** clearing thread-local state

### `parallel()` + `runOn(...)`
**Use for:** concurrent processing  
**Consequence:** thread switching becomes real

### `ExecutorService.execute(...)`
**Use for:** explicit async offload  
**Good at:** keeping request threads free

---

## 23. PRE-NG-Specific Takeaway

If someone asks:

> “How does PRE-NG keep request data while using WebFlux, parallel COSMOS, and async Kafka?”

A strong answer is:

> PRE-NG uses a hybrid model. It uses `ThreadLocal` (`RequestIdContext`, `PipelineTrackerContext`) for convenient request-scoped access on the current thread, Reactor `Context` (`contextWrite`, `Mono.deferContextual`) for data that must survive reactive thread switching, and explicit early capture into local variables before async boundaries. Then Kafka serialization/sending is offloaded to a dedicated `ExecutorService` so request threads are not blocked.

---

## 24. Files Referenced in This Story

- `src/main/java/com/optum/prepcp/apps/preng/controllers/MemberProviderRecommendationsController.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/kafka/RequestIdContext.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/kafka/PipelineTrackerContext.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/kafka/LatencyIntelligenceService.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/kafka/PipelineTracker.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/memberproviderrecommendations/impl/StandardMemberProviderRecommendationsService.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/memberproviderrecommendations/impl/MemberRecommendationServiceHelper.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/providers/imp/PesProvidersService.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/providerordering/imp/DroolsProviderOrderingService.java`
- `src/main/java/com/optum/prepcp/apps/preng/services/providervalidation/impl/CosmosInvalidProviderRemoverService.java`

---

## 25. Final One-Line Memory Trick

**ThreadLocal = pocket on the thread.**  
**Reactor Context = backpack on the reactive journey.**  
**Capture early before thread hops. Clean up at the end.**

