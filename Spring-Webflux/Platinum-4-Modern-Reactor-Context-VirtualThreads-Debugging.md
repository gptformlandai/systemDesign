# Platinum 4 - Modern Reactor Context, Virtual Threads, Debugging

> Goal: understand the modern Reactor layer behind WebFlux: context propagation, virtual
> thread backed boundedElastic, debugging tools, checkpoints, metrics, BlockHound, and
> production troubleshooting.

---

## 0. Mental Model First

Modern WebFlux lives at the intersection of:

```text
Reactive Streams + Reactor operators + Netty event loops + observability + Java 21 runtime
```

The basics are `Mono`, `Flux`, and operators.

The senior layer is:

- context propagation
- scheduler choice
- virtual threads as a blocking bridge
- debugging async stack traces
- finding blocking calls
- production metrics
- cancellation and resource cleanup

---

## 1. Reactor Context Revisited

Reactor Context is an immutable key-value store tied to the subscriber chain.

Use for:

- trace ID
- correlation ID
- auth-derived metadata
- tenant ID
- locale
- request-scoped operational metadata

Do not use for:

- large payloads
- mutable business state
- replacing method arguments everywhere
- hidden control flow

### Code Sample

```java
Mono<String> result = Mono.deferContextual(ctx -> {
        String traceId = ctx.getOrDefault("traceId", "missing");
        return Mono.just("trace=" + traceId);
    })
    .contextWrite(ctx -> ctx.put("traceId", "abc-123"));
```

### Interview Punchline

```text
Context follows the reactive subscriber chain. ThreadLocal follows a thread. In WebFlux,
those are not the same thing.
```

---

## 2. Context Propagation To Logs

Problem:

```text
MDC is ThreadLocal-based, but reactive work can move across threads.
```

Better pattern:

- keep trace ID in Reactor Context
- bridge to logging MDC at signal boundaries if needed
- use Micrometer/observability integration where available
- avoid hand-written global hooks unless team understands the cost

Example local logging:

```java
return Mono.deferContextual(ctx -> {
    String traceId = ctx.getOrDefault("traceId", "missing");
    log.info("loading booking traceId={}", traceId);
    return bookingRepository.findById(id);
});
```

### Trap

Setting MDC once at the controller method and expecting it to survive all scheduler hops.

---

## 3. Automatic Context Propagation

Modern Reactor and Micrometer context-propagation support can help bridge context across
reactive and imperative boundaries.

### What To Know For Interviews

You do not need to memorize every hook, but you should know:

- context propagation is a first-class production concern
- ThreadLocal-based tools need explicit bridging
- tracing/logging frameworks may integrate with Reactor context
- custom hooks can add overhead and should be tested

Strong answer:

```text
I prefer framework-supported context propagation for tracing and logging rather than
inventing custom global hooks. I still verify it with tests and traces because missing
correlation IDs make reactive incidents painful.
```

---

## 4. Scheduler Refresher

| Scheduler | Best For |
|---|---|
| `immediate()` | current thread |
| `single()` | serialized lightweight work |
| `parallel()` | CPU-bound work |
| `boundedElastic()` | blocking bridge |

### Senior Rule

```text
Schedulers are isolation tools, not speed buttons.
```

Use scheduler shifts intentionally:

- isolate unavoidable blocking code
- move CPU-heavy work off event loop
- serialize work when ordering needs it

Do not:

- wrap every chain in random scheduler hops
- use `parallel()` for blocking calls
- hide blocking architecture under `boundedElastic()`

---

## 5. Virtual Threads And `boundedElastic`

Modern Reactor can run shared `Schedulers.boundedElastic()` tasks on Java virtual threads
when the application runs on Java 21+ and the relevant Reactor system property is enabled.

Conceptually:

```text
boundedElastic with platform threads = bounded worker pool for blocking bridges
boundedElastic with virtual threads = thread-per-task style using virtual threads
```

### Why It Matters

Virtual threads make blocking waits cheaper than platform threads, but they do not turn a
blocking API into a non-blocking protocol.

Good use:

```java
Mono<LegacyResult> result = Mono.fromCallable(() -> legacyClient.call(request))
    .subscribeOn(Schedulers.boundedElastic());
```

### Strong Interview Answer

```text
Virtual threads can reduce the cost of unavoidable blocking bridges, but they do not replace
WebClient, R2DBC, backpressure, or resilience. I still keep the event loop non-blocking and
use boundedElastic only at the boundary where blocking cannot be avoided.
```

---

## 6. Virtual Threads vs WebFlux

| Question | Answer |
|---|---|
| Do virtual threads make WebFlux useless? | No |
| Do they help blocking legacy integrations? | Yes |
| Do they provide Reactive Streams backpressure? | No |
| Do they make WebClient unnecessary? | No |
| Do they remove timeout/retry/circuit breaker needs? | No |

### Decision

Use MVC + virtual threads when:

- app is mostly blocking
- programming simplicity matters
- no streaming/backpressure need
- team is not reactive mature

Use WebFlux when:

- end-to-end non-blocking I/O matters
- streaming matters
- fan-out concurrency matters
- reactive composition and backpressure matter

Hybrid:

- WebFlux request path
- boundedElastic/virtual threads only for legacy blocking adapters

---

## 7. Debugging Reactive Stack Traces

Reactive stack traces can be hard because assembly and execution are separated.

Tools:

- `checkpoint()`
- `Hooks.onOperatorDebug()`
- ReactorDebugAgent
- structured logs with trace ID
- StepVerifier
- metrics and traces

### `checkpoint()`

```java
return paymentClient.authorize(request)
    .checkpoint("payment authorization for booking confirmation")
    .flatMap(payment -> bookingService.confirm(request, payment));
```

Use checkpoints at important boundaries.

### `Hooks.onOperatorDebug()`

Useful in development or targeted debugging. It can add overhead, so do not blindly enable
globally in production hot paths.

### ReactorDebugAgent

Can improve assembly tracing with less manual code, but still must be tested in the target
runtime.

---

## 8. BlockHound

BlockHound detects blocking calls on threads that should not block.

Use it in tests/dev to catch:

- `Thread.sleep`
- blocking file/network calls
- accidental JDBC
- `.block()`
- legacy SDK use on event loop

Example test setup shape:

```java
@BeforeAll
static void setup() {
    BlockHound.install();
}
```

### Interview Punchline

```text
BlockHound is a safety net that proves the team is serious about keeping reactive paths
non-blocking.
```

---

## 9. Production Debugging Playbook

### Symptom: High p99 Latency

Check:

- downstream spans
- scheduler saturation
- Netty event loop blockage
- connection pool wait
- retries
- timeout budget
- large body aggregation

### Symptom: CPU Low But Requests Slow

Likely:

- waiting on downstream
- connection pool exhausted
- blocked event loop
- queueing
- slow subscriber

### Symptom: Memory Growth

Likely:

- unbounded buffer
- replay sink/cache
- DataBuffer leak
- long-lived subscription not cancelled
- collecting large body

### Symptom: Missing Trace IDs

Likely:

- ThreadLocal/MDC assumption
- context not written early enough
- context lost across imperative boundary
- Kafka/message headers not propagated

---

## 10. Reactor Metrics And Observation

Monitor:

- HTTP request latency
- downstream WebClient latency
- connection pool metrics
- scheduler queue depth / saturation where available
- error rate by exception class
- timeout count
- retry count
- cancellation count for streams
- sink emission failures
- consumer lag if Kafka involved

Strong answer:

```text
For reactive systems, I watch not just request latency but queueing, scheduler usage,
connection pools, cancellations, retries, timeouts, and missing context propagation.
```

---

## 11. Cancellation Is A First-Class Signal

Cancellation can happen when:

- client disconnects
- `take(n)` has enough data
- timeout fires
- `switchMap` cancels old inner publisher
- test uses `thenCancel`

### Cleanup Pattern

```java
return bookingStatusStream(bookingId)
    .doFinally(signal -> {
        if (signal == SignalType.CANCEL) {
            log.info("client cancelled booking status stream");
        }
        cleanup();
    });
```

### Interview Trap

Ignoring cancellation in streaming endpoints. This causes leaked subscriptions and wasted
work.

---

## 12. Operator Debugging With `log()`

`log()` can show signals:

```java
return bookingService.find(id)
    .log("booking.find")
    .map(this::toView);
```

Use carefully:

- useful for learning/testing
- too noisy for production hot paths
- may expose sensitive data if careless

---

## 13. Assembly vs Execution Bugs

### Bug

```java
Mono<String> result = Mono.just(legacyCall());
```

`legacyCall()` runs immediately at assembly time.

### Fix

```java
Mono<String> result = Mono.fromCallable(this::legacyCall)
    .subscribeOn(Schedulers.boundedElastic());
```

### Senior Line

```text
The timing bug is not just blocking. It is blocking at assembly time before the reactive
chain even has a chance to schedule correctly.
```

---

## 14. Modern Decision: WebFlux, MVC, Virtual Threads

| Workload | Best Default |
|---|---|
| simple CRUD with JPA | MVC, possibly virtual threads |
| high fan-out non-blocking HTTP | WebFlux |
| streaming SSE/WebSocket | WebFlux |
| large blocking legacy integration | MVC virtual threads or WebFlux boundedElastic bridge |
| R2DBC reactive persistence | WebFlux |
| CPU-heavy processing | MVC or offload CPU carefully |
| team new to reactive | MVC unless WebFlux need is clear |

### Honest Senior Answer

```text
I choose based on workload shape, not trend. Virtual threads improve blocking scalability;
WebFlux improves non-blocking composition, streaming, and backpressure.
```

---

## 15. Interview Hot Questions

### 1. Do virtual threads replace WebFlux?

No. They make blocking waits cheaper but do not provide Reactive Streams, non-blocking I/O,
operator composition, or backpressure.

### 2. When should `boundedElastic` be used?

For unavoidable blocking bridges, not for normal reactive WebClient/R2DBC calls.

### 3. Why is ThreadLocal tricky in WebFlux?

Because a request can move across threads; context must follow the reactive chain instead.

### 4. What is `checkpoint()`?

A debugging marker that improves reactive error trace readability at important chain points.

### 5. Should `Hooks.onOperatorDebug()` run in production?

Not blindly. It is useful for debugging but can add overhead.

### 6. What does BlockHound prove?

It helps detect blocking calls on non-blocking threads during tests/dev.

### 7. Why is cancellation important?

Cancellation is how reactive streams stop work. Ignoring it leaks subscriptions and wastes
resources.

---

## 16. Final Revision Notes

```text
Reactor Context follows subscriber chain; ThreadLocal follows thread.
Use context for trace, auth metadata, tenant, locale.
Schedulers isolate work; they are not speed magic.
boundedElastic is for unavoidable blocking bridges.
Virtual-thread boundedElastic can reduce blocking bridge cost on Java 21+.
Virtual threads do not replace WebFlux/backpressure/non-blocking I/O.
checkpoint improves error trace readability.
Hooks.onOperatorDebug is useful but not free.
BlockHound catches blocking calls in reactive paths.
Cancellation needs cleanup.
Monitor retries, timeouts, pools, scheduler pressure, and context propagation.
```

---

## 17. Official Source Notes

- Reactor reference guide: https://docs.spring.io/projectreactor/reactor-core/docs/current/reference/html/
- Reactor debugging: https://docs.spring.io/projectreactor/reactor-core/docs/current/reference/html/#debugging
- Reactor context: https://docs.spring.io/projectreactor/reactor-core/docs/current/reference/html/#context
- Reactor schedulers and virtual threads: https://docs.spring.io/projectreactor/reactor-core/docs/current/reference/html/#schedulers
- Micrometer context propagation: https://docs.micrometer.io/context-propagation/reference/
- BlockHound: https://github.com/reactor/BlockHound
