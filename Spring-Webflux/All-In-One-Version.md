# Spring WebFlux — All-In-One Final Version

> Goal: one final interview pack that ties all six batches together into a single story, a fast cheat sheet, a last-minute revision page, and domain-style interview answers.

---

## 1. How To Use This File

Use this file in three passes:

1. Read the story mode section once so all concepts connect in one mental movie.
2. Use the cheat sheet for fast recall of terms, traps, and decision rules.
3. Practice the Marriott-style questions out loud using the spoken-answer versions.

If you are short on time:

- read Section 3 for the end-to-end request travel
- read Section 4 for one-liners and traps
- read Section 5 for final revision
- read Section 6 for interview speaking practice

---

## 2. Coverage Map

This file intentionally pulls ideas from all batches:

- Batch 1: reactive model, Mono/Flux, event loop, subscription, assembly vs execution
- Batch 2: operators and how they change signal flow
- Batch 3: schedulers, publishOn/subscribeOn, Context, defer/fromCallable
- Batch 4: backpressure, hot vs cold, Sinks, StepVerifier
- Batch 5: blocking traps, cleanup, R2DBC, WebClient, SSE/WebSocket, when WebFlux loses
- Batch 6: gateway, Kafka, resilience, observability, architecture trade-offs

---

## 3. Story Mode — One End-To-End Hospitality Request Travel

This is a Marriott-style hospitality story, but generic enough to work for any large booking platform.

### The Business Setting

A guest uses the mobile app to:

1. search hotels and room options
2. confirm a booking
3. see live booking or check-in updates
4. trigger downstream events for loyalty, notifications, and analytics

System components:

- mobile app / web app
- Spring Cloud Gateway
- booking-service built with Spring WebFlux
- guest-profile service
- pricing service
- inventory service
- payment service
- loyalty store via R2DBC
- legacy PMS adapter that is still blocking
- Kafka for downstream events
- SSE endpoint for live booking updates
- Micrometer + tracing + structured logs for observability

### The Cast Of Publishers

- `Mono<GuestProfile>` from guest-profile service
- `Mono<RateQuote>` from pricing service
- `Mono<InventoryHold>` from inventory service
- `Mono<PaymentAuth>` from payment service
- `Mono<LoyaltySnapshot>` from reactive DB
- `Mono<PmsReservationRef>` from a blocking legacy PMS adapter wrapped safely
- `Flux<BookingUpdate>` from a hot sink that pushes live updates

### Scene 0 — Before Booking: Search Typeahead Uses `switchMap`

Before the actual booking request even happens, the guest types hotel or city search text.

This is where `switchMap` fits naturally.

```java
Flux<SearchResult> searchResults = queryFlux
    .filter(query -> query.length() >= 2)
    .distinctUntilChanged()
    .switchMap(searchClient::searchHotels);
```

Why `switchMap`?

- if guest types `n`, then `ne`, then `new`, old searches are stale
- we want only the latest query result
- old in-flight searches should be cancelled

This is a perfect example of Batch 2 operator choice based on business meaning.

### Scene 1 — The Booking Request Hits The Gateway

Guest taps `Confirm Booking`.

Request:

`POST /api/bookings/confirm`

Gateway does:

1. route match by predicate
2. auth token validation
3. trace/correlation header propagation
4. rate limiting
5. forward to `booking-service`

Why WebFlux is a strong fit here:

- gateway is coordination-heavy and I/O-heavy
- every request passes through it
- blocking here would amplify across the whole platform

### Scene 2 — Controller Assembly Happens In booking-service

Booking controller returns `Mono<BookingConfirmationResponse>`.

At controller execution time, the pipeline is assembled.

```java
@PostMapping("/bookings/confirm")
Mono<BookingConfirmationResponse> confirm(@RequestBody Mono<BookingRequest> requestMono) {
    return requestMono.flatMap(request ->
        Mono.deferContextual(ctx -> {
            Mono<GuestProfile> guestMono = guestClient.getGuest(request.guestId())
                .filter(GuestProfile::active)
                .switchIfEmpty(Mono.error(new IllegalStateException("Inactive guest")));

            Mono<RateQuote> rateMono = pricingClient.quote(request.hotelId(), request.roomType(), request.dates());

            Mono<InventoryHold> inventoryMono = inventoryClient.hold(request.hotelId(), request.roomType(), request.dates())
                .timeout(Duration.ofMillis(800))
                .transformDeferred(CircuitBreakerOperator.of(inventoryCircuitBreaker));

            Mono<PaymentAuth> paymentMono = paymentClient.authorize(request.paymentToken())
                .timeout(Duration.ofSeconds(1))
                .transformDeferred(CircuitBreakerOperator.of(paymentCircuitBreaker))
                .onErrorResume(ex -> paymentFallback.cachedAuthorization(request.paymentToken()));

            Mono<LoyaltySnapshot> loyaltyMono = loyaltyRepository.findByMemberId(request.memberId());

            Mono<PmsReservationRef> pmsMono = Mono.fromCallable(() -> legacyPmsAdapter.reserve(request))
                .subscribeOn(Schedulers.boundedElastic());

            Flux<AncillaryOffer> offerFlux = offerClient.getOffers(request.hotelId())
                .filter(AncillaryOffer::eligible)
                .distinct(AncillaryOffer::id)
                .take(3);

            Mono<List<AncillaryOffer>> offersMono = offerFlux.collectList();

            return Mono.zip(guestMono, rateMono, inventoryMono, paymentMono, loyaltyMono, pmsMono, offersMono)
                .publishOn(Schedulers.parallel())
                .map(tuple -> buildDraftBooking(request, tuple, ctx.getOrDefault("traceId", "missing")))
                .flatMap(draft -> saveAndPublish(draft))
                .map(this::toResponse);
        })
    )
    .doOnSubscribe(sub -> log.info("booking confirm started"))
    .doOnSuccess(resp -> log.info("booking confirm succeeded"))
    .doOnError(ex -> log.error("booking confirm failed", ex))
    .doFinally(signal -> metrics.recordBookingCompletion(signal))
    .contextWrite(context -> context.put("traceId", UUID.randomUUID().toString()));
}
```

What has happened so far?

- only assembly
- no final response yet
- if all sources are truly lazy and reactive, most real work has not started yet

This is Batch 1 in action:

- controller returns a publisher, not the actual JSON body
- Spring still has to subscribe before execution starts

### Scene 3 — Spring Subscribes, So Execution Starts

This is the trigger point many candidates miss.

After the controller returns the final `Mono<BookingConfirmationResponse>`, Spring subscribes because it needs actual data to write the HTTP response.

That means:

- `guestMono` starts
- `rateMono` starts
- `inventoryMono` starts
- `paymentMono` starts
- `loyaltyMono` starts
- `pmsMono` starts lazily on `boundedElastic()`
- `offerFlux` starts and emits eligible offers

This is the real start of execution.

### Scene 4 — Event Loop, WebClient, and Schedulers Work Together

Now different kinds of work run in different places.

#### Non-blocking HTTP calls

- guest-profile
- pricing
- inventory
- payment

These run through non-blocking WebClient / Reactor Netty flow.

While waiting on remote responses:

- event-loop threads are not sleeping on one request
- they process other ready sockets and tasks

#### Reactive DB call

- `loyaltyRepository.findByMemberId(...)`
- if using R2DBC, it stays non-blocking and reactive

#### Blocking legacy PMS call

- wrapped with `fromCallable(...)`
- moved off hot threads with `subscribeOn(Schedulers.boundedElastic())`

That means:

- blocking still exists
- but it is isolated away from the Netty event loop

#### CPU-heavy response enrichment

The `publishOn(Schedulers.parallel())` before heavy mapping means:

- downstream `map(...)` runs on a CPU-oriented pool
- we avoid doing heavier compute on I/O-oriented threads

This is Batch 3 execution control.

### Scene 5 — Operators Shape The Flow

Here is how Batch 2 concepts show up naturally.

#### `filter`

`guestMono.filter(GuestProfile::active)`

- if guest is inactive, value is removed
- `switchIfEmpty(...)` turns that emptiness into a business error

#### `map`

Used when building DTOs or final response objects from already available values.

#### `flatMap`

Used when moving from draft booking to async persistence flow:

- save booking
- save audit
- publish event

#### `zip`

Used because final confirmation needs all of these together:

- guest profile
- rate
- inventory hold
- payment authorization
- loyalty snapshot
- PMS reservation reference
- top ancillary offers list

Overall latency now depends on the slowest required source.

#### `take(3)`

Offer stream stops after 3 eligible unique offers.

- no need to process all offers if the UX wants only top 3
- upstream may get cancelled early

#### `doOn...`

Used only for observation:

- log start
- log success/failure
- record metrics on finally

Not used for core business transformation.

### Scene 6 — Error Handling and Resilience Protect The Request

Distributed systems fail in partial ways.

That is why the pipeline uses:

- `timeout(...)`
- `CircuitBreakerOperator`
- `onErrorResume(...)`

Example:

- payment service times out after 1 second
- circuit breaker tracks repeated failure patterns
- fallback returns cached authorization status if business permits

This is Batch 2 + Batch 6 together.

Key interview line:

- non-blocking helps concurrency
- resilience patterns handle failure
- these are related but not the same thing

### Scene 7 — Context Carries The Trace ID Across Thread Hops

This request touches:

- Netty event loop
- boundedElastic thread for PMS adapter
- parallel thread for final enrichment

So plain `ThreadLocal` is not enough.

That is why `contextWrite(...)` and `deferContextual(...)` matter.

The `traceId` travels with the reactive chain, not with one thread.

This is critical for:

- tracing
- MDC bridging
- debugging across async boundaries

### Scene 8 — Reactive Transaction and R2DBC

Once all required booking inputs are available, the system persists booking data.

Correct direction:

- use R2DBC repositories
- use `TransactionalOperator` or reactive transaction manager

```java
Mono<BookingDraft> saveAndPublish(BookingDraft draft) {
    return bookingRepository.save(draft)
        .then(auditRepository.save(new AuditEntry(draft.bookingId(), "BOOKING_CONFIRMED")))
        .then(bookingEventProducer.publishConfirmed(draft))
        .thenReturn(draft)
        .as(transactionalOperator::transactional);
}
```

Why this matters:

- if you used JPA/JDBC here, the stack would not be truly reactive
- a `Mono` controller return type does not make blocking persistence safe

### Scene 9 — Booking Confirmed Event Goes To Kafka

After confirmation succeeds, the request publishes `BookingConfirmed` to Kafka.

Why this is architecturally strong:

- loyalty update does not slow down the user-facing response unnecessarily
- notification service does not need to be in the synchronous path
- analytics service consumes the same event independently

This is Batch 6 messaging decoupling.

### Scene 10 — Reactive Kafka Consumers Process The Afterlife Of The Request

Downstream consumers receive `BookingConfirmed`.

Example:

- notification-service sends email/SMS
- loyalty-service updates points
- analytics-service records business event

Consumer-side processing may use `concatMap` if order per partition matters.

```java
Flux<Void> processed = inboundRecords.concatMap(record ->
    notificationService.sendBookingEmail(record.value())
        .doOnSuccess(ignored -> record.receiverOffset().acknowledge())
);
```

Why `concatMap` here?

- simple ordered processing story
- good when partition ordering matters more than maximum concurrency

### Scene 11 — The Client Opens A Live Booking Status Stream

After booking confirmation, the app may subscribe to:

`GET /bookings/{id}/status-stream`

This endpoint is a strong place to show Batch 4 and Batch 5 ideas.

```java
@GetMapping(value = "/bookings/{id}/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<BookingStatusView>> statusStream(@PathVariable String id) {
    Flux<BookingStatusView> initialSnapshot = bookingRepository.findById(id)
        .map(BookingStatusView::snapshot)
        .flux();

    Flux<BookingStatusView> liveUpdates = bookingUpdateSink.asFlux()
        .filter(update -> update.bookingId().equals(id))
        .onBackpressureLatest()
        .map(BookingStatusView::live);

    return Flux.concat(initialSnapshot, liveUpdates)
        .map(view -> ServerSentEvent.builder(view).build())
        .doOnCancel(() -> log.info("status stream cancelled for {}", id));
}
```

Here is what is happening:

- initial snapshot is cold: each subscriber gets a fresh read
- live updates are hot: they come from a shared sink
- `onBackpressureLatest()` protects slow clients by keeping only fresh state

If a second client joins late:

- it gets its own fresh snapshot
- it does not get the entire history of old hot events unless replay is explicitly configured

### Scene 12 — Sinks Bridge Imperative External Events Into The Stream

Suppose housekeeping or PMS sends imperative callbacks about room readiness or booking state.

Those events are pushed into Reactor using `Sinks`.

```java
Sinks.Many<BookingUpdate> bookingUpdateSink = Sinks.many().multicast().onBackpressureBuffer();

public void onExternalBookingUpdate(BookingUpdate update) {
    Sinks.EmitResult result = bookingUpdateSink.tryEmitNext(update);
    if (result.isFailure()) {
        log.warn("Failed to emit booking update: {}", result);
    }
}
```

Why `Sinks`, not old Processors?

- `Sinks` are the modern Reactor answer
- they are the correct interview-safe answer for imperative-to-reactive bridging

### Scene 13 — Backpressure Matters On Live Status Streams

Suppose booking updates arrive faster than the mobile app can render.

Without strategy:

- stale updates queue up
- memory and latency rise
- the UI sees old states too late

With `onBackpressureLatest()`:

- stale intermediate updates are dropped
- the UI sees near-current state

This is correct for state-feed semantics.

But if every event were audit-critical, `latest` would be the wrong strategy.

That is the senior answer: backpressure strategy depends on business meaning.

### Scene 14 — Cleanup And Leak Prevention

Long-lived streams and shared subscriptions need cleanup thinking.

What can leak?

- manual subscriptions with no `Disposable` ownership
- replay/caching too much history
- unbounded buffers
- streaming connections that do not clean up on cancel

This is why:

- `doFinally(...)`
- `timeout(...)`
- `doOnCancel(...)`
- careful sink and replay choices

matter in production.

### Scene 15 — StepVerifier Proves The Behavior

Reactive correctness should be tested signal-by-signal.

Examples:

```java
StepVerifier.create(offerFlux)
    .expectNextCount(3)
    .expectComplete()
    .verify();

StepVerifier.create(liveUpdates, 0)
    .then(() -> bookingUpdateSink.tryEmitNext(new BookingUpdate("b1", "CHECKED_IN")))
    .thenRequest(1)
    .expectNextCount(1)
    .thenCancel()
    .verify();
```

This tests:

- selection behavior
- demand behavior
- live update flow

StepVerifier is how you prove the reactive semantics instead of assuming them.

### Scene 16 — When WebFlux Wins In This Story

WebFlux is a strong fit here because the system has:

- high concurrency at the gateway
- multiple downstream I/O calls
- live SSE stream updates
- Kafka-based async afterlife of the request
- potential fan-out and waiting-heavy coordination

### Scene 17 — When WebFlux Would Lose In A Similar Hotel System

If the service were instead:

- a low-traffic admin CRUD app
- mostly JPA/JDBC
- no streaming
- minimal downstream fan-out

then Spring MVC might be a better engineering choice.

This is the right final maturity answer:

- use WebFlux where the problem shape rewards it
- do not force it where imperative design is simpler and safer

---

## 4. Cheat Sheet

### 4.1 One-Liners For All 28 Topics

#### Batch 1

1. Reactive Programming Model: reactive programming is signal-driven composition with demand control.
2. Mono and Flux: `Mono` emits 0..1 item, `Flux` emits 0..N items, and nothing meaningful starts until subscription.
3. Event Loop and Netty: Netty uses a small number of event-loop threads to coordinate many connections without thread-per-request waiting.
4. WebFlux vs MVC: WebFlux wins in non-blocking I/O-heavy concurrency; MVC often wins in simple blocking CRUD services.
5. Reactor Lifecycle: assembly builds the pipeline, subscription starts execution, signals then flow until completion/error/cancel.

#### Batch 2

6. map vs flatMap vs concatMap vs switchMap: `map` reshapes data, `flatMap` does async expansion, `concatMap` preserves order, `switchMap` keeps only the latest inner work.
7. filter/take/skip/distinct: these operators allow, limit, ignore, or deduplicate items and can affect upstream lifecycle.
8. doOn callbacks: `doOn...` hooks observe signals and should not hold core business logic.
9. zip/merge/concat/combineLatest: `zip` waits for all, `merge` emits as ready, `concat` sequences, `combineLatest` recomputes with latest values.
10. Error Handling: `onErrorReturn`, `onErrorResume`, `onErrorMap`, and `retryWhen` determine how failure changes the flow.

#### Batch 3

11. Schedulers: schedulers decide where work runs and are mainly about isolation and fit.
12. publishOn vs subscribeOn: `subscribeOn` affects source/upstream start, `publishOn` shifts downstream work from that point forward.
13. Reactor Context: Context carries request metadata with the chain when thread hops make ThreadLocal unreliable.
14. defer and fromCallable: `defer` lazily creates publishers per subscriber; `fromCallable` lazily bridges one blocking or throwing computation into a `Mono`.

#### Batch 4

15. Backpressure: downstream controls demand with `request(n)` and chooses buffer/drop/latest/error trade-offs.
16. Hot vs Cold: cold restarts per subscriber; hot keeps emitting regardless of subscriber timing.
17. Sinks API: `Sinks` are the modern way to bridge imperative events into Reactor streams.
18. StepVerifier: StepVerifier proves signal order, demand, timing, completion, cancellation, and errors.

#### Batch 5

19. Blocking in Reactive Chains: blocking code inside WebFlux destroys thread-efficiency and can stall event loops.
20. Memory Leaks and Cleanup: long-lived subscriptions, replay/buffer misuse, and forgotten cleanup cause hidden reactive leaks.
21. Reactive Transactions and R2DBC: end-to-end reactive persistence needs non-blocking drivers and reactive transaction boundaries.
22. WebClient vs RestTemplate: WebClient preserves non-blocking HTTP flow; RestTemplate does not.
23. SSE and WebSocket: SSE is one-way server push, WebSocket is full duplex.
24. When WebFlux Loses: WebFlux loses when the workload is blocking, CPU-heavy, small-scale, or too complex for the team.

#### Batch 6

25. API Gateway with WebFlux: gateway is a strong WebFlux fit because it is I/O-heavy coordination on the hot path of all traffic.
26. Reactive Kafka and Messaging: messaging decouples side effects and moves some work off the synchronous user request path.
27. Reactive Microservices Patterns: timeout, circuit breaker, fallback, bulkhead, and budget propagation are mandatory distributed-system design tools.
28. Observability in Reactive Stacks: tracing, metrics, structured logs, and debug tooling must be async-aware and Context-friendly.

### 4.2 Top Interview Traps

1. Thinking a `Mono` return type automatically makes the whole stack reactive.
2. Forgetting that Spring subscribes after the controller returns.
3. Confusing lazy emission with lazy computation.
4. Using `Mono.just(blockingCall())`.
5. Calling `.block()` inside request flow.
6. Using `flatMap` and still expecting strict ordering on a `Flux`.
7. Using `doOnNext` for core business logic.
8. Forgetting that `take(n)` can cancel upstream.
9. Treating `publishOn` and `subscribeOn` as interchangeable.
10. Relying on ThreadLocal for request correlation in reactive code.
11. Thinking WebClient misuse is harmless because it is already reactive.
12. Using JPA/Hibernate in WebFlux and calling it good enough.
13. Ignoring cleanup of long-lived subscriptions and replay/buffer patterns.
14. Forgetting hot vs cold when multiple subscribers arrive at different times.
15. Thinking `zip` emits partial results early.
16. Retrying non-idempotent operations blindly.
17. Using WebSocket when SSE is enough.
18. Assuming non-blocking automatically means resilient.
19. Assuming Reactor Kafka removes partition, lag, or commit trade-offs.
20. Using WebFlux where MVC is the simpler and better fit.

### 4.3 When To Use WebFlux — Decision Flow

Use this in interviews as a simple decision tree.

1. Is the service mostly waiting on I/O?
   If `no`, MVC may be better.
   If `yes`, continue.

2. Is the stack end-to-end or mostly non-blocking?
   If `no`, either isolate blocking carefully or prefer MVC.
   If `yes`, continue.

3. Does the service need one or more of these?
   - high concurrency
   - many downstream service calls
   - streaming responses
   - gateway behavior
   - event-driven workflows

   If `no`, MVC may still be the cleaner option.
   If `yes`, continue.

4. Can the team support reactive debugging and operations?
   If `no`, WebFlux may create more complexity than value.
   If `yes`, WebFlux is a strong candidate.

### 4.4 The 10-Second Summary Of WebFlux

WebFlux is not about making one slow call magically fast.

It is about making waiting cheap, keeping the stack non-blocking, coordinating many I/O-bound tasks efficiently, and still staying correct under failure, streaming, and scale.

---

## 5. Ultra-Short Revision Sheet Across Batches 1–6

### Batch 1 — Foundations

- controller returns publisher, not final value
- Spring subscribes, and only then execution really starts
- Netty event loop coordinates many requests with few threads
- `Mono` = 0..1, `Flux` = 0..N

### Batch 2 — Operators

- `map` for sync transform, `flatMap` for async next step
- `zip` for all required together
- `take` can cancel upstream
- `doOn...` observes, does not own business logic

### Batch 3 — Execution Control

- `boundedElastic()` for blocking bridges
- `parallel()` for CPU work
- `subscribeOn` moves source start, `publishOn` moves downstream
- Reactor Context replaces ThreadLocal assumptions

### Batch 4 — Advanced Streams

- backpressure is demand control
- cold restarts, hot keeps going
- `Sinks` bridge imperative events
- StepVerifier proves signal behavior and demand

### Batch 5 — Production Traps

- `.block()` is the cardinal sin in request flow
- long-lived subscriptions need cleanup ownership
- R2DBC fits WebFlux; JPA usually does not
- WebClient is right, but still easy to misuse

### Batch 6 — Architecture

- gateway is a strong WebFlux use case
- Kafka decouples side effects from user latency
- resilience patterns are mandatory in distributed flows
- observability must be async-aware

### The One Paragraph To Remember Everything

In WebFlux, a request enters a non-blocking runtime, the controller assembles a publisher graph, Spring subscribes, signals flow through operators, schedulers move the right work to the right execution pools, Context carries request metadata across thread hops, backpressure protects long-lived streams, reactive persistence and WebClient keep the path non-blocking, Kafka decouples side effects, resilience handles failures, and observability makes the whole async system operable.

---

## 6. Marriott-Style WebFlux Interview Questions With Spoken Answers

These are hospitality-domain flavored, but the answers are reusable for many enterprise WebFlux interviews.

### 1. Why would a hospitality platform use WebFlux at the API gateway?

**Strong spoken answer:**

"The gateway is a strong WebFlux fit because it is mostly doing I/O-heavy coordination work like auth checks, routing, rate limiting, header propagation, and forwarding to downstream services. Every request hits the gateway, so if we make that layer blocking, the cost gets multiplied across the whole platform. With WebFlux and Reactor Netty, the gateway can handle high concurrency without tying one thread to one request during network waits." 

### 2. How would you design a booking confirmation API that calls guest, pricing, inventory, payment, and loyalty services?

**Strong spoken answer:**

"I would keep the request path reactive end to end. The controller would return a `Mono<BookingConfirmationResponse>`, assemble cold publishers for guest, pricing, inventory, payment, and loyalty, and combine the independent ones with `Mono.zip`. I would use `flatMap` only where the next step is another async call, and `map` for synchronous DTO building. If there is a legacy blocking adapter like an old PMS or fraud SDK, I would wrap it with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` so it does not block the event loop." 

### 3. What would you do if payment service becomes slow during booking confirmation?

**Strong spoken answer:**

"Non-blocking code alone is not enough, so I would add timeout, circuit breaker, and fallback where the business allows it. For example, payment authorization call would have a bounded timeout, go through a Resilience4j circuit breaker, and optionally fall back to a cached or degraded status if that is acceptable. The important point is that WebFlux helps concurrency during waiting, but resilience patterns are what protect correctness and latency under downstream failure." 

### 4. Why WebClient over RestTemplate in a booking service?

**Strong spoken answer:**

"Because the booking service is a good candidate for parallel I/O fan-out. RestTemplate would block a thread during each remote call, while WebClient lets those waits happen without pinning request threads. That matters when one booking request calls multiple downstream services like rate, inventory, payment, and profile. I would still configure timeouts, handle statuses carefully, and make sure response bodies are properly consumed, because WebClient is the right fit but still easy to misuse." 

### 5. How do you explain `Mono` and `Flux` to a panel without sounding too abstract?

**Strong spoken answer:**

"I explain them as publishers of future signals, not just containers. `Mono` means zero or one value, `Flux` means zero to many values. In WebFlux, the controller returns one of these publishers, and Spring subscribes to it later when it needs real data to write the HTTP response. The client never sees Mono or Flux; it still receives normal JSON or streaming bytes over HTTP." 

### 6. How would you stream live booking or check-in status to a mobile app?

**Strong spoken answer:**

"If the app only needs one-way server push, I would usually choose SSE before WebSocket because it is simpler and HTTP-friendly. I would return a `Flux<ServerSentEvent<BookingStatus>>`, usually composed of a cold initial snapshot plus a hot live update stream from a sink. If updates are frequent and the client can fall behind, I would choose a backpressure strategy like `onBackpressureLatest()` if only the freshest state matters." 

### 7. What is the difference between cold and hot in a booking-status stream?

**Strong spoken answer:**

"Cold means each subscriber gets a fresh execution, like reading the current booking snapshot from the database. Hot means the stream is already producing live events and late subscribers only see events from when they joined onward, unless replay is configured. In a status-stream endpoint, I often combine both: a cold initial snapshot for every subscriber and then a hot live event feed for ongoing updates." 

### 8. Where would Kafka fit in a booking architecture?

**Strong spoken answer:**

"I would keep the core user-facing confirmation path synchronous only for what the client truly needs immediately, and then publish a `BookingConfirmed` event to Kafka for side effects like loyalty updates, notifications, and analytics. That decouples those consumers from the booking latency budget. On the consumer side, I would think carefully about partitioning, ordering, idempotency, and commit-after-success semantics." 

### 9. How would you propagate trace IDs through gateway, booking service, and Kafka?

**Strong spoken answer:**

"At the gateway I would create or forward a correlation ID and pass it in headers. Inside reactive services I would store that in Reactor Context rather than relying only on ThreadLocal. When producing Kafka messages I would copy that trace or correlation data into message headers as well. That gives me consistent tracing and structured logging across HTTP and messaging boundaries even though the execution crosses threads." 

### 10. When would you tell the team not to use WebFlux for a hotel system?

**Strong spoken answer:**

"I would avoid WebFlux if the service is mostly simple CRUD on top of blocking JPA, has low concurrency, no streaming, and no meaningful fan-out to downstream services. In that shape, MVC is usually easier to debug and maintain, and the reactive complexity does not buy enough. I only push WebFlux when the workload shape actually rewards non-blocking I/O, streaming, or high-concurrency coordination." 

### 11. What is the single biggest mistake teams make in WebFlux projects?

**Strong spoken answer:**

"The biggest mistake is building a reactive surface with blocking internals. That includes things like `.block()` in services, `Mono.just(blockingCall())`, RestTemplate in the hot path, or JPA repositories behind a reactive controller. The framework type alone does not make the system reactive. The benefit only appears when the real I/O path and execution model are aligned." 

### 12. If I say `take(3)` on an offer stream, what subtle lifecycle effect should you mention?

**Strong spoken answer:**

"I would mention that `take(3)` is not just a final list trim. It can cancel upstream after three items have been received. So it affects lifecycle and resource usage, not just the shape of the output. That is important when the upstream is expensive or long-lived." 

---

## 7. Final Two-Minute Read Before Interview

If you only remember one mental movie, remember this:

1. request hits a non-blocking gateway or service
2. controller assembles a `Mono` or `Flux`
3. Spring subscribes and execution starts
4. WebClient and R2DBC do non-blocking I/O
5. blocking legacy code, if unavoidable, is isolated on `boundedElastic()`
6. operators like `map`, `flatMap`, `zip`, `filter`, and `onErrorResume` shape the signal flow
7. Context carries trace data across thread hops
8. backpressure protects long-lived streams
9. sinks bridge external live events
10. Kafka moves side effects off the synchronous request path
11. resilience patterns protect against slow or failing downstreams
12. observability makes the async system debuggable
13. WebFlux is excellent for the right shape of I/O-heavy, concurrent, streaming, fan-out systems
14. WebFlux is the wrong choice when the system is mostly blocking CRUD and the complexity is not justified

That is the senior WebFlux answer.
