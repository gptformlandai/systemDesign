# WebFlux Interview Drill Bank - Tricky Scenarios Gold Sheet

> Goal: fast revision for WebFlux interview pressure. This is not a theory chapter. It is
> a scenario bank for tricky questions, output prediction, thread reasoning, production
> traps, and crisp spoken answers.

---

## 0. How To Use This Drill Bank

Use this file in three passes:

1. Read the question only and answer out loud.
2. Check the trap and strong answer.
3. Re-answer in 30 seconds like an interview.

Golden interview rule:

```text
In WebFlux questions, always identify subscription, signal flow, thread boundary, cancellation,
and whether the work is truly non-blocking.
```

---

# 1. `map` vs `flatMap` Drill

## Question

What is wrong here?

```java
Mono<Mono<UserDto>> result = userRepository.findById(id)
    .map(user -> profileClient.getProfile(user.profileId()));
```

## Trap

`profileClient.getProfile(...)` returns `Mono<UserDto>`, so `map` creates nested publishers.

## Strong Answer

Use `flatMap` when the lambda returns another publisher.

```java
Mono<UserDto> result = userRepository.findById(id)
    .flatMap(user -> profileClient.getProfile(user.profileId()));
```

Interview line:

```text
map is value-to-value. flatMap is value-to-publisher.
```

---

# 2. `flatMap` Ordering Drill

## Question

Will this preserve order?

```java
Flux.just("o1", "o2", "o3")
    .flatMap(orderService::getDetails);
```

## Trap

No. `flatMap` can interleave results because inner publishers may complete at different
times.

## Strong Answer

Use `concatMap` when ordering matters.

```java
Flux.just("o1", "o2", "o3")
    .concatMap(orderService::getDetails);
```

Trade-off:

```text
concatMap preserves order but reduces concurrency.
```

---

# 3. `switchMap` Business Trap

## Question

Should we use `switchMap` for payment authorization if the user clicks pay twice?

## Trap

`switchMap` cancels older inner work when a newer signal arrives. That is dangerous for
business writes and payments.

## Strong Answer

Use `switchMap` for latest-only read/search flows, not critical write/payment flows.

For payment:

- idempotency key
- state machine
- server-side duplicate protection
- no cancellation-based correctness

Interview line:

```text
switchMap is excellent for typeahead search and dangerous for write flows where old work
must not be silently cancelled.
```

---

# 4. `Mono.just(blockingCall())` Drill

## Question

Why is this bad?

```java
Mono<String> result = Mono.just(legacyClient.call());
```

## Trap

`legacyClient.call()` runs immediately at assembly time, before subscription and before any
scheduler can help.

## Strong Answer

```java
Mono<String> result = Mono.fromCallable(() -> legacyClient.call())
    .subscribeOn(Schedulers.boundedElastic());
```

Interview line:

```text
Mono.just stores an already computed value. fromCallable defers the blocking call until
subscription and lets us isolate it on boundedElastic.
```

---

# 5. `.block()` On Event Loop Drill

## Question

Why can this hurt production?

```java
@GetMapping("/booking/{id}")
Mono<BookingView> booking(@PathVariable String id) {
    BookingView view = bookingClient.get(id).block();
    return Mono.just(view);
}
```

## Trap

The request thread may be a Netty event loop. Blocking it stalls many connections.

## Strong Answer

```java
@GetMapping("/booking/{id}")
Mono<BookingView> booking(@PathVariable String id) {
    return bookingClient.get(id);
}
```

Interview line:

```text
Blocking inside WebFlux defeats the runtime model. Return the publisher and let Spring
subscribe/write the response.
```

---

# 6. `subscribeOn` Prediction Drill

## Question

Which scheduler affects the source?

```java
Mono.fromCallable(this::load)
    .map(this::convert)
    .subscribeOn(Schedulers.boundedElastic())
    .subscribeOn(Schedulers.parallel());
```

## Trap

Only the first effective `subscribeOn` closest to the source affects subscription/source
work in this chain.

## Strong Answer

`load()` runs on `boundedElastic`.

Interview line:

```text
subscribeOn is upstream-oriented. Multiple subscribeOn calls are usually a smell; the one
closest to the source wins for that source.
```

---

# 7. `publishOn` Prediction Drill

## Question

Where do these operators run?

```java
source()
    .map(this::a)
    .publishOn(Schedulers.parallel())
    .map(this::b)
    .publishOn(Schedulers.boundedElastic())
    .map(this::c);
```

## Strong Answer

- `a` runs on the thread delivering source signals.
- after first `publishOn`, `b` runs on `parallel`.
- after second `publishOn`, `c` runs on `boundedElastic`.

Interview line:

```text
publishOn shifts downstream execution from that point onward.
```

---

# 8. `zip` Empty Source Drill

## Question

What happens if one source in `Mono.zip` is empty?

```java
Mono.zip(customerMono, paymentMono, loyaltyMono)
    .map(tuple -> build(tuple));
```

## Trap

If one source completes empty, the zipped result completes empty. The `map` may never run.

## Strong Answer

Use `defaultIfEmpty`, `switchIfEmpty`, or model the absence explicitly.

```java
Mono<Loyalty> loyaltyMono = loyaltyService.find(memberId)
    .defaultIfEmpty(Loyalty.none());
```

Interview line:

```text
zip needs one value from each source. Empty is not null; it means no combined tuple.
```

---

# 9. `zip` Error Drill

## Question

What happens if one source in `zip` errors?

## Strong Answer

The combined publisher errors unless that source handles the error before zip.

```java
Mono<Payment> paymentMono = paymentClient.authorize(req)
    .onErrorResume(PaymentTimeoutException.class, ex -> paymentFallback.pending(req));

return Mono.zip(customerMono, paymentMono, inventoryMono);
```

Interview line:

```text
Handle local fallback before zip if one dependency failure should not fail the whole
combination.
```

---

# 10. `take(n)` Cancellation Drill

## Question

What is the hidden effect of `take(3)`?

```java
offerService.streamOffers(hotelId)
    .take(3)
    .collectList();
```

## Trap

`take(3)` can cancel upstream after three items.

## Strong Answer

This is good if only three offers are needed, but dangerous if upstream cancellation has
side effects or cleanup is missing.

Interview line:

```text
take is not just trimming output; it controls lifecycle by cancelling after enough demand is
satisfied.
```

---

# 11. `doOnNext` Trap Drill

## Question

Should business writes happen inside `doOnNext`?

```java
bookingMono.doOnNext(booking -> auditRepository.save(booking));
```

## Trap

`doOnNext` is for side-effect observation. If `save` returns a publisher, it is not composed
and may not execute correctly.

## Strong Answer

```java
bookingMono.flatMap(booking ->
    auditRepository.save(booking).thenReturn(booking)
);
```

Interview line:

```text
doOnNext observes signals. flatMap composes async work into the chain.
```

---

# 12. Error Swallowing Drill

## Question

What is wrong here?

```java
return paymentClient.authorize(req)
    .onErrorResume(ex -> Mono.just(Payment.approved()));
```

## Trap

It approves payment on any error, including real provider failure.

## Strong Answer

Fallback must match business semantics.

```java
return paymentClient.authorize(req)
    .onErrorResume(TimeoutException.class, ex -> Mono.just(Payment.pending()));
```

Interview line:

```text
onErrorResume is not an error eraser. It is a business fallback decision.
```

---

# 13. Retry Drill

## Question

When is retry dangerous?

## Strong Answer

Retry is dangerous when:

- operation is not idempotent
- no timeout
- no backoff/jitter
- no max attempts
- failure is permanent
- downstream is already overloaded

Good shape:

```java
return inventoryClient.hold(req)
    .timeout(Duration.ofMillis(800))
    .retryWhen(Retry.backoff(2, Duration.ofMillis(100))
        .filter(this::isTransientError));
```

Interview line:

```text
Retry without idempotency and backoff is how you create a retry storm.
```

---

# 14. Hot vs Cold Drill

## Question

What happens when two subscribers subscribe to this?

```java
Flux<String> ids = Flux.defer(() -> orderRepository.findRecentIds());
```

## Strong Answer

This is cold. Each subscriber gets a fresh execution.

Now compare:

```java
Flux<String> shared = ids.share();
```

`share()` makes it hot-ish/shared while subscribers are connected; late subscribers may miss
earlier values.

Interview line:

```text
Cold starts per subscriber. Hot emits independent of individual subscribers.
```

---

# 15. Sink Emission Drill

## Question

Why can this fail?

```java
sink.tryEmitNext(update);
```

## Strong Answer

Emission can fail if:

- no subscribers for certain sink types
- backpressure cannot be honored
- sink is terminated
- concurrent emission violates sink contract
- buffer is full

Check `Sinks.EmitResult`.

```java
Sinks.EmitResult result = sink.tryEmitNext(update);
if (result.isFailure()) {
    log.warn("failed to emit booking update result={}", result);
}
```

Interview line:

```text
Sinks bridge imperative events into reactive streams, but emission success is not guaranteed.
```

---

# 16. Backpressure Strategy Drill

## Question

For live booking status, which backpressure strategy fits?

## Strong Answer

If only latest status matters:

```java
statusFlux.onBackpressureLatest();
```

If every event matters:

- bounded buffer
- slow producer
- durable broker
- client-specific queue
- explicit overflow behavior

Interview line:

```text
Backpressure strategy is a business decision: can we drop, keep latest, buffer, or fail?
```

---

# 17. WebClient Status Drill

## Question

How do you map 404 to empty but fail on 500?

```java
return webClient.get()
    .uri("/hotels/{id}", id)
    .exchangeToMono(response -> {
        if (response.statusCode().value() == 404) {
            return Mono.empty();
        }
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(Hotel.class);
        }
        return response.createException().flatMap(Mono::error);
    });
```

## Interview Line

```text
Use exchangeToMono when response handling depends on status, headers, or body shape.
```

---

# 18. WebClient Body Leak Drill

## Question

What is the hidden risk of low-level response handling?

## Strong Answer

If you access a `ClientResponse` but do not consume or release the body, resources can leak.
Prefer `retrieve()` for simple cases or ensure all branches consume/create exception.

Interview line:

```text
Lower-level WebClient control means lower-level response-body responsibility.
```

---

# 19. R2DBC Transaction Drill

## Question

Why is this suspicious?

```java
@Transactional
public Mono<Booking> confirm(BookingRequest request) {
    return bookingRepository.save(toBooking(request))
        .doOnNext(saved -> paymentClient.charge(request.paymentToken()));
}
```

## Traps

- payment call inside `doOnNext` is not composed
- remote payment does not belong inside DB transaction
- payment side effect may happen inconsistently

## Better Shape

```java
public Mono<Booking> confirm(BookingRequest request) {
    return transactionalOperator.execute(status ->
            bookingRepository.save(Booking.pending(request))
                .flatMap(saved -> outboxRepository.save(BookingCreated.of(saved)).thenReturn(saved))
        )
        .single();
}
```

Payment happens through saga/outbox after local transaction.

---

# 20. Reactive Transaction Boundary Drill

## Question

When does a reactive transaction actually cover work?

## Strong Answer

It covers work inside the subscribed reactive chain. Work outside the chain, manual
subscribe calls, or uncomposed publishers may not participate.

Interview line:

```text
In reactive transactions, composition is the boundary. If it is not in the chain, it is not
reliably in the transaction.
```

---

# 21. DataBuffer Memory Drill

## Question

What is wrong here for a 500 MB file?

```java
filePart.content()
    .collectList()
    .flatMap(buffers -> upload(buffers));
```

## Trap

It aggregates the file in memory.

## Strong Answer

Stream it:

```java
return filePart.transferTo(targetPath);
```

or:

```java
return DataBufferUtils.write(filePart.content(), targetPath).then();
```

Interview line:

```text
Large bodies should stream through DataBuffers, not collect into heap.
```

---

# 22. Context Drill

## Question

Why does this sometimes lose trace ID?

```java
MDC.put("traceId", traceId);
return service.call()
    .publishOn(Schedulers.parallel())
    .map(this::toResponse);
```

## Trap

MDC is ThreadLocal-based. `publishOn` can move work to another thread.

## Strong Answer

Use Reactor Context and framework-supported context propagation.

```java
return service.call()
    .contextWrite(ctx -> ctx.put("traceId", traceId));
```

Interview line:

```text
ThreadLocal follows a thread. Reactor Context follows the subscriber chain.
```

---

# 23. Virtual Threads Drill

## Question

Do virtual threads replace WebFlux?

## Strong Answer

No. Virtual threads make blocking waits cheaper. WebFlux provides non-blocking I/O,
Reactive Streams composition, backpressure, and streaming support.

Decision:

- mostly blocking CRUD with JPA: MVC plus virtual threads may be simpler
- non-blocking fan-out/streaming/backpressure: WebFlux still fits
- legacy blocking adapter in WebFlux: isolate via `boundedElastic`, potentially virtual-thread backed

---

# 24. WebTestClient Stream Drill

## Question

How do you test an infinite SSE stream?

## Strong Answer

Use WebTestClient to get response body, StepVerifier to assert some signals, then cancel.

```java
Flux<BookingStatus> body = webTestClient.get()
    .uri("/api/bookings/b123/status")
    .exchange()
    .expectStatus().isOk()
    .returnResult(BookingStatus.class)
    .getResponseBody();

StepVerifier.create(body)
    .expectNextMatches(status -> status.bookingId().equals("b123"))
    .thenCancel()
    .verify();
```

Interview line:

```text
Infinite streams should be tested by asserting enough behavior and then cancelling.
```

---

# 25. Security Drill

## Question

Gateway validates JWT. Should downstream WebFlux service skip authorization?

## Strong Answer

No. Gateway auth is useful but not sufficient. Downstream service should still enforce
service-level and business-level authorization.

Interview line:

```text
Gateway authentication does not replace domain authorization.
```

---

# 26. Error Response Drill

## Question

What is wrong with returning 200 and an error body?

## Strong Answer

It breaks HTTP semantics, monitoring, client handling, retries, and dashboards.

Better:

```text
Use correct status code plus structured error body with stable code and traceId.
```

---

# 27. API Versioning Drill

## Question

Is adding a new JSON field a breaking change?

## Strong Answer

Usually no, if clients ignore unknown fields. Removing, renaming, changing type, or changing
meaning is breaking.

Interview line:

```text
Version for breaking semantic or contract changes, not every additive field.
```

---

# 28. HTTP Interface Client Drill

## Question

When use Spring HTTP interface clients?

## Strong Answer

Use them for stable typed outbound HTTP APIs where a declarative interface makes service
code cleaner while still using WebClient underneath.

Do not use if the request/response handling is highly dynamic or requires deep custom
exchange control.

---

# 29. RSocket Drill

## Question

When is RSocket better than REST?

## Strong Answer

When both sides are internal/reactive and need backpressure-aware request-stream, fire-and-
forget, or bidirectional channel communication.

Not for simple public CRUD.

---

# 30. "Should We Use WebFlux?" Drill

## Scenario A

```text
Simple CRUD service, Spring Data JPA, low concurrency, no streaming.
```

Answer:

```text
Prefer Spring MVC. WebFlux complexity is not justified.
```

## Scenario B

```text
Gateway calling many downstream services under high concurrency.
```

Answer:

```text
WebFlux is a strong fit because the workload is I/O-heavy coordination.
```

## Scenario C

```text
Streaming live order status to mobile clients.
```

Answer:

```text
WebFlux is a strong fit with SSE/WebSocket, cancellation handling, and backpressure strategy.
```

## Scenario D

```text
CPU-heavy image processing.
```

Answer:

```text
WebFlux does not help much. Use dedicated compute workers, queues, or carefully offload CPU
work.
```

---

# 31. Production Incident Drill

## Question

WebFlux API p99 latency spikes, CPU is low. What do you check?

## Strong Answer

Check:

- downstream service latency
- WebClient connection pool wait
- event loop blocking
- scheduler saturation
- retries/timeouts
- DB/R2DBC pool wait
- large body aggregation
- logs/traces by correlation ID

Interview line:

```text
Low CPU with high latency often means waiting, queueing, or blocked event loops, not compute.
```

---

# 32. Thirty-Second Spoken Answers

## What Is WebFlux?

WebFlux is Spring's reactive web stack. It lets handlers return `Mono` or `Flux`, and the
runtime uses non-blocking I/O and Reactive Streams so the system can handle many concurrent
I/O-heavy requests without tying one thread to each waiting request.

## What Is The Biggest Mistake?

The biggest mistake is building a reactive controller over blocking internals: `.block()`,
JPA, RestTemplate, `Mono.just(blockingCall())`, or manual subscribe in the service layer.

## When Does WebFlux Win?

It wins for high-concurrency, I/O-heavy, fan-out, streaming, and mostly non-blocking
systems.

## When Does WebFlux Lose?

It loses when the app is simple blocking CRUD, CPU-heavy, low concurrency, or the team cannot
operate reactive complexity.

## What Is Backpressure?

Backpressure is demand control. The consumer can signal how much data it can handle, and the
producer should respect that instead of overwhelming memory or downstream systems.

## What Is The Senior WebFlux Answer?

WebFlux is not about making one slow call faster. It is about making waiting cheap, composing
non-blocking I/O safely, handling backpressure, and keeping the system observable and
resilient.

---

# 33. Final Drill Checklist

Before interview, make sure you can answer:

- `map` vs `flatMap`
- `flatMap` vs `concatMap`
- `switchMap` cancellation
- `publishOn` vs `subscribeOn`
- `Mono.just` vs `defer` vs `fromCallable`
- `.block()` danger
- `zip` empty/error behavior
- `take(n)` cancellation
- hot vs cold
- backpressure strategy choice
- `doOnNext` vs `flatMap`
- WebClient `retrieve` vs `exchangeToMono`
- response body leak risk
- R2DBC transaction boundaries
- DataBuffer streaming
- Reactor Context vs ThreadLocal
- virtual threads vs WebFlux
- WebTestClient streaming test
- security/error/versioning basics
- when to reject WebFlux

---

# 34. Final Memory Trick

```text
S T O P before answering WebFlux:

S - Subscription: when does execution start?
T - Thread: where does this part run?
O - Operator: what does this operator do to signal flow?
P - Production: what happens on error, cancel, slow downstream, or high load?
```

That four-letter check prevents most WebFlux interview mistakes.
