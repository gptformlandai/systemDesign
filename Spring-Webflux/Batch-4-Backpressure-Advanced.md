# Batch 4 — Backpressure, Hot vs Cold and Advanced Patterns

> Goal: understand how reactive systems stay safe when producers and consumers run at different speeds, how publisher behavior changes across subscribers, how to bridge external events into Reactor, and how to test all of this with confidence.

---

## 0. Advanced Mental Model First

Batch 1 taught you when execution starts.

Batch 2 taught you how operators shape signal flow.

Batch 3 taught you where execution runs.

Batch 4 teaches you what happens when the stream is long-lived, fast, shared, replayed, or tested under pressure.

This is the point where interviews stop being "Can you write `map` and `flatMap`?" and become:

- what if the producer is faster than the client?
- what happens if two subscribers arrive at different times?
- how do you push external events into a reactive stream?
- how do you prove your backpressure and timing behavior in tests?

### The Four Questions To Ask For Any Stream

1. Is this publisher cold or hot?
2. Can the consumer keep up with the producer?
3. If not, what strategy do I want: buffer, drop, latest, or fail?
4. How do I test the behavior without guessing?

### The Core Rules Of Batch 4

- Backpressure is demand control between downstream and upstream.
- Cold publishers start fresh per subscriber.
- Hot publishers emit independently of subscriber timing.
- `Sinks` are the safe way to bridge imperative event sources into Reactor.
- `StepVerifier` is how you prove behavior instead of trusting intuition.

### Simple Analogy

Think of a live concert venue:

- cold publisher = each viewer gets a private replay of the whole show from the beginning
- hot publisher = the concert is happening live now; if you join late, you miss the opening songs
- backpressure = the crowd can only pass so many people through a gate at a time
- sink = the stage crew microphone where new announcements are injected into the live system
- StepVerifier = rehearsal script proving the stage cues happen in the right order

### Decision Cheat Sheet

- Need downstream demand control: backpressure-aware design
- Need fresh execution per subscriber: cold publisher
- Need shared live feed: hot publisher
- Need to emit events manually into Reactor: `Sinks`
- Need to assert signal order, completion, error, and demand: `StepVerifier`

---

## 15. Backpressure

### What It Changes In The Request Flow

Backpressure controls what happens when the producer can emit faster than the consumer can safely process.

In a normal blocking system, the mismatch often shows up as:

- queues growing silently
- memory rising
- downstream threads saturating
- timeouts and instability appearing later

In Reactive Streams, backpressure is part of the contract.

The consumer says how much it can handle through `request(n)`.

### Simple Analogy

Think of a warehouse conveyor belt.

- producer = machine dropping boxes onto the belt
- consumer = worker packing boxes into trucks
- backpressure = worker telling the machine: "Send me 5 now, not 5000."

Without that control, boxes pile up and the warehouse collapses.

### Core Idea

Reactive Streams include this lifecycle:

1. `onSubscribe`
2. downstream requests data using `request(n)`
3. upstream emits up to that demand
4. downstream requests more when ready

This is why Reactor is not just "async." It has controlled demand.

### What If Producer Is Too Fast?

Common strategies are:

- buffer the excess
- drop excess items
- keep only the latest
- fail fast with an error

Typical Reactor operators:

- `onBackpressureBuffer(...)`
- `onBackpressureDrop(...)`
- `onBackpressureLatest()`
- `onBackpressureError()`

### When Each Strategy Makes Sense

#### Buffer

Keep extra items in memory until downstream catches up.

Good for:

- short bursts
- workloads where no data may be lost

Danger:

- unbounded memory growth if producer keeps outpacing consumer

#### Drop

Drop extra items when downstream cannot keep up.

Good for:

- telemetry
- metrics streams
- firehose data where some loss is acceptable

#### Latest

Keep only the newest item and discard stale ones.

Good for:

- dashboards
- UI state updates
- location tracking where only latest state matters

#### Error

Fail immediately when overflow happens.

Good for:

- flows where loss or silent buffering would be worse than explicit failure

### Why This Matters In Request Flow

Suppose a live order-tracking SSE endpoint emits location updates 500 times per second but the frontend can render only 20 per second.

If you do nothing:

- buffers may grow
- memory pressure rises
- latency increases because consumer processes stale updates

If you use `onBackpressureLatest()`:

- old location points are dropped
- consumer sees near-current state
- system remains responsive

### Request-Level Example

```java
Flux<LocationUpdate> uiFriendly = shipmentUpdateFlux
    .onBackpressureLatest();
```

This says: "If the client falls behind, do not waste time on stale coordinates. Keep the freshest one."

### Backfires That Might Occur

- buffering infinite or very large streams and consuming memory until failure
- dropping business-critical events where every item matters
- using `latest` in financial or audit flows where intermediate states matter
- assuming all publishers always honor backpressure equally well

### Anti-Patterns

- using unbounded buffering without capacity thinking
- ignoring consumer speed on live streaming endpoints
- choosing drop/latest without aligning to business semantics
- believing backpressure automatically fixes bad upstream design with no trade-off

### Code Sample (Java)

```java
Flux<ShipmentEvent> safeLiveFeed = shipmentSink.asFlux()
    .filter(event -> event.customerId().equals(customerId))
    .onBackpressureLatest();
```

### Interview Trap

"Backpressure just means the system gets slower politely when load rises."

That is wrong. Backpressure is explicit demand control, and the chosen strategy changes data retention, memory behavior, and business correctness.

### Quick Revision Notes

- backpressure = downstream demand control
- `request(n)` is the heart of it
- buffer/drop/latest/error are different business decisions
- Trap to remember = backpressure strategy is correctness, not just performance

---

## 16. Hot vs Cold Publishers

### What They Change In The Request Flow

This topic answers:

"If two subscribers arrive at different times, do they get the same full sequence or only what is live now?"

That is the cold vs hot distinction.

### Cold Publisher

A cold publisher starts fresh for each subscriber.

Examples:

- `WebClient` call
- reactive DB query
- `Mono.just(...)`
- `Flux.range(...)`

If subscriber A and subscriber B both subscribe, each gets its own run of the source.

### Hot Publisher

A hot publisher emits independently of whether a subscriber was present from the beginning.

Examples:

- live WebSocket feed
- market price stream
- Kafka-driven event bridge converted into shared Reactor stream
- `Sinks.Many` used as a shared event bus

If subscriber B joins late, it may miss earlier events.

### Simple Analogy

- cold = each customer gets a private movie screening starting from minute 0
- hot = live TV broadcast already in progress; join late, miss the beginning

### Why This Matters In WebFlux Requests

Suppose endpoint returns:

- customer profile from DB
- current order status from service A
- live shipment updates from a shared event bus

Then:

- profile query is cold
- order status call is cold
- live shipment update stream is hot

Understanding which part is cold and which is hot tells you:

- whether each request restarts the source
- whether late subscribers get full history
- whether you need replay behavior

### Common Ways To Turn Cold Toward Shared/Hot Behavior

- `share()`
- `publish().autoConnect()`
- `replay()`

These change how subscribers interact with the source and whether values are shared or replayed.

### `share()`

Makes a source effectively shared among active subscribers.

Use when:

- you want one upstream subscription
- multiple subscribers should observe the same live flow

### `replay()`

Replays previous items to late subscribers.

Use when:

- new subscribers need history
- recent items must be available after the fact

### Why Replay Matters

Some flows are naturally hot but still need limited history.

Example:

- new dashboard subscriber joins
- should receive last known state immediately, not wait for next event

That is a replay use case.

### Backfires That Might Occur

- assuming a WebClient `Mono` is shared when it is cold and reruns per subscriber
- using hot publishers where each request should be isolated
- replaying too much history and causing memory cost
- forgetting that late subscribers to hot streams may miss critical events

### Anti-Patterns

- treating every stream as if it restarts from scratch
- sharing a stream without understanding lifecycle and subscriber timing
- using replay carelessly with large or unbounded event history
- expecting hot live feeds to behave like deterministic request-response sources

### Code Sample (Java)

```java
Mono<Customer> coldCustomerMono = customerClient.getCustomer(customerId);

Sinks.Many<ShipmentEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
Flux<ShipmentEvent> hotShipmentFeed = sink.asFlux();

Flux<ShipmentEvent> replayingFeed = hotShipmentFeed.replay(10).autoConnect();
```

### Interview Trap

"If I subscribe twice to a `Mono<WebClient call>`, both subscribers naturally share the same result."

That is wrong. Standard WebClient publishers are cold, so each subscriber usually triggers its own execution unless you intentionally share/cache them.

### Quick Revision Notes

- cold = fresh execution per subscriber
- hot = source emits independently of subscriber timing
- `share` = shared live flow
- `replay` = shared flow with history
- Trap to remember = WebClient and DB calls are usually cold by default

---

## 17. Processor / Sinks API

### What They Change In The Request Flow

Sometimes the source of data is not already a Reactor publisher.

Examples:

- callback from a legacy listener
- message arriving from external broker integration code
- imperative event generated inside the app
- manual admin trigger or background event source

You need a way to push those events into a reactive stream safely.

That is what `Sinks` are for.

### Important Current Guidance

Older `Processor` types are deprecated for most practical use.

In modern Reactor, prefer `Sinks`.

That is the interview-safe answer.

### Simple Analogy

Think of `Sinks` as a controlled microphone on a live stage.

- external code speaks into the microphone
- the sound system turns that into the live stream heard by subscribers

The sink is the bridge from imperative world into reactive world.

### Main Sink Families

#### `Sinks.one()`

- emit one value or one terminal signal
- good for one-time completion or callback bridging

#### `Sinks.many().unicast()`

- one subscriber only
- buffers for that single subscriber
- useful when exactly one consumer is intended

#### `Sinks.many().multicast()`

- multiple subscribers
- live fan-out behavior
- good for live feeds where many listeners observe events as they happen

#### `Sinks.many().replay()`

- multiple subscribers
- can replay past items to late subscribers
- good when history or last-known values matter

### Why Emit Result Handling Matters

When you call:

```java
sink.tryEmitNext(event);
```

you should not blindly ignore the result in production-grade code.

Why?

- sink may reject emission
- there may be no subscribers yet
- sink may be terminated
- emission may conflict under certain conditions

The senior answer is: inspect or consciously handle emission outcomes.

### Why This Matters In Request Flow

Suppose Kafka listener receives shipment updates imperatively.

You want SSE clients connected to `/customers/{id}/shipment-stream` to see those updates live.

Flow becomes:

1. Kafka listener receives event
2. listener pushes event into `Sinks.Many`
3. SSE endpoint exposes `sink.asFlux()`
4. connected clients receive the event reactively

This is exactly how imperative event sources get bridged into WebFlux streaming APIs.

### Backfires That Might Occur

- using `unicast` but expecting multiple subscribers
- replaying too much history and growing memory
- ignoring `EmitResult` and losing visibility into rejected emissions
- keeping old deprecated `Processor` code when `Sinks` are the supported model

### Anti-Patterns

- hiding shared global mutable state inside sink usage
- assuming sinks replace normal request-response publishers everywhere
- choosing replay sink for huge high-volume streams without retention thinking
- using `Processor` in new code when `Sinks` are the current model

### Code Sample (Java)

```java
Sinks.Many<ShipmentEvent> shipmentSink = Sinks.many().multicast().onBackpressureBuffer();

public void onShipmentEvent(ShipmentEvent event) {
    Sinks.EmitResult result = shipmentSink.tryEmitNext(event);
    if (result.isFailure()) {
        log.warn("Could not emit shipment event: {}", result);
    }
}

Flux<ShipmentEvent> shipmentFlux() {
    return shipmentSink.asFlux();
}
```

### Interview Trap

"Processors are still the standard answer for pushing events into Reactor."

That is wrong for modern Reactor. Prefer `Sinks` in current code.

### Quick Revision Notes

- `Sinks` bridge imperative events into reactive streams
- `one`, `unicast`, `multicast`, `replay` serve different subscriber patterns
- handle emission result consciously
- Trap to remember = modern answer is `Sinks`, not `Processor`

---

## 18. StepVerifier and Testing

### What They Change In The Request Flow

StepVerifier does not change production request flow.

It changes your confidence in it.

Reactive systems are timing-sensitive and signal-oriented, so traditional assert-after-the-fact testing is often too weak.

StepVerifier lets you assert:

- next values
- completion
- errors
- cancellation
- timing behavior
- demand behavior

### Simple Analogy

Think of StepVerifier as a rehearsal conductor.

It says:

- first cue should happen here
- second cue should happen here
- then completion
- if the wrong cue appears, rehearsal fails immediately

### Common Patterns

#### Assert Values And Completion

```java
StepVerifier.create(Flux.just("A", "B"))
    .expectNext("A")
    .expectNext("B")
    .expectComplete()
    .verify();
```

#### Assert Error

```java
StepVerifier.create(Mono.error(new IllegalStateException("boom")))
    .expectError(IllegalStateException.class)
    .verify();
```

#### Assert Backpressure / Demand

```java
StepVerifier.create(Flux.range(1, 5), 0)
    .thenRequest(2)
    .expectNext(1, 2)
    .thenRequest(3)
    .expectNext(3, 4, 5)
    .expectComplete()
    .verify();
```

This is one of the most important Batch 4 examples because it proves request-driven demand.

#### Use Virtual Time

```java
StepVerifier.withVirtualTime(() -> Flux.interval(Duration.ofSeconds(1)).take(3))
    .thenAwait(Duration.ofSeconds(3))
    .expectNext(0L, 1L, 2L)
    .expectComplete()
    .verify();
```

Virtual time avoids waiting real seconds in tests.

### Why This Matters In Request Flow

Suppose you build:

- a backpressure-sensitive stream
- an SSE endpoint using a hot sink
- retry or timeout behavior

Without StepVerifier, teams often just "run it and see." That is weak.

With StepVerifier, you can prove:

- fallback happened
- cancellation happened
- demand behaved as expected
- replay or hot behavior worked correctly

### Backfires That Might Occur

- writing flaky tests using real waiting instead of virtual time
- asserting only final result and ignoring signal order or demand
- misunderstanding hot streams in tests and expecting deterministic history without replay
- forgetting to control initial request amount when testing backpressure

### Anti-Patterns

- `Thread.sleep(...)` heavy reactive tests
- testing only with `.block()` and losing signal semantics
- not verifying cancellation or error conditions for long-lived streams
- assuming hot publishers behave deterministically in tests without setup

### Code Sample (Java)

```java
Flux<Integer> limited = Flux.range(1, 10).take(3);

StepVerifier.create(limited)
    .expectNext(1, 2, 3)
    .expectComplete()
    .verify();
```

### Interview Trap

"If a reactive service test passes with `.block()`, that is enough proof that the flow is correct."

That is wrong. `.block()` often hides signal order, demand behavior, cancellation, and timing semantics that StepVerifier can assert explicitly.

### Quick Revision Notes

- StepVerifier asserts signal-by-signal behavior
- use `thenRequest(...)` for backpressure tests
- use `withVirtualTime(...)` for time-based flows
- Trap to remember = `.block()` tests value only, not full reactive semantics

---

## Batch 4 — Putting Advanced Patterns Into One Request Story

Now connect all four topics using one realistic WebFlux endpoint.

### Use Case

Endpoint: `GET /customers/{id}/shipment-stream`

Behavior:

- return initial shipment snapshot from service and DB
- continue streaming live shipment updates as SSE
- late subscribers should get the initial snapshot from their own request
- live updates come from a shared hot source fed by external events
- if client is slow, keep the most recent state rather than flooding it with stale positions

This is perfect for Batch 4 because it mixes:

- cold request data
- hot live data
- sink-based bridging
- backpressure strategy
- StepVerifier testing

### The Code

```java
private final Sinks.Many<ShipmentUpdate> shipmentSink =
    Sinks.many().multicast().onBackpressureBuffer();

@GetMapping(value = "/customers/{id}/shipment-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<ShipmentView>> shipmentStream(@PathVariable String id) {
    Mono<Customer> customerMono = customerClient.getCustomer(id);
    Flux<Shipment> shipmentsFlux = shipmentRepository.findActiveShipments(id);

    Flux<ShipmentView> initialSnapshot = Mono.zip(customerMono, shipmentsFlux.collectList())
        .map(tuple -> ShipmentView.snapshot(tuple.getT1(), tuple.getT2()))
        .flux();

    Flux<ShipmentView> liveUpdates = shipmentSink.asFlux()
        .filter(update -> update.customerId().equals(id))
        .onBackpressureLatest()
        .map(ShipmentView::fromUpdate);

    return Flux.concat(initialSnapshot, liveUpdates)
        .map(view -> ServerSentEvent.builder(view).build())
        .doOnSubscribe(sub -> log.info("shipment stream started for {}", id))
        .doOnCancel(() -> log.info("shipment stream cancelled for {}", id))
        .doFinally(signal -> metrics.recordShipmentStreamEnd(id, signal));
}

public void onShipmentMessageFromKafka(ShipmentUpdate update) {
    Sinks.EmitResult result = shipmentSink.tryEmitNext(update);
    if (result.isFailure()) {
        log.warn("shipment update emit failed: {}", result);
    }
}
```

### Why Each Batch 4 Concept Is Here

- `customerMono` = cold WebClient request per subscriber
- `shipmentsFlux` = cold reactive DB query per subscriber
- `shipmentSink.asFlux()` = hot shared live update source
- `onBackpressureLatest()` = if client is slow, keep freshest live state
- `Flux.concat(initialSnapshot, liveUpdates)` = one request first gets snapshot, then continues with live feed
- `Sinks.Many` = bridge Kafka or other imperative event source into Reactor

### Full Request Life Journey

#### Phase 1: Request Arrives

1. client opens SSE request `GET /customers/42/shipment-stream`
2. Netty receives request
3. Spring routes to controller
4. controller assembles the cold snapshot part and the hot live update part

#### Phase 2: Spring Subscribes

After the controller returns `Flux<ServerSentEvent<ShipmentView>>`, Spring subscribes because it must stream response bytes to the client.

Execution starts now.

#### Phase 3: Cold Snapshot Executes Per Subscriber

`initialSnapshot` is built from:

- customer service call
- reactive DB query

These are cold sources, so this request gets its own run of them.

If another browser opens the same endpoint later, it gets a fresh snapshot execution too.

This is exactly what you want for request-scoped current data.

#### Phase 4: Snapshot Emits First

`Mono.zip(...).flux()` emits one snapshot item.

Then `Flux.concat(...)` moves on to the live update stream.

This means:

- client first sees a full starting state
- then client stays connected for ongoing updates

#### Phase 5: Hot Live Stream Takes Over

`shipmentSink.asFlux()` is hot.

That means:

- updates can be emitted by Kafka listener whether or not a given subscriber is ready
- a subscriber joining late does not automatically get old live updates
- it only sees events from the point it is connected onward

This is the correct model for live shipment tracking.

#### Phase 6: External Event Bridge Uses Sink

When Kafka listener or some external callback receives a shipment update:

1. listener gets imperative event
2. listener calls `shipmentSink.tryEmitNext(update)`
3. Reactor turns that into a signal for current subscribers
4. each connected client whose filter matches gets the event

That is the bridge from imperative event source to reactive stream.

#### Phase 7: Backpressure Protects Slow Client

Suppose shipment updates arrive 100 times per second but the browser renders only 10 per second.

Without backpressure strategy:

- stale updates queue up
- client lags further behind
- memory and latency become worse

With `onBackpressureLatest()`:

- if downstream cannot keep up, old pending updates are discarded
- latest state is preserved
- UI sees fresh location rather than ancient location

This is the senior answer because location streaming is state-oriented, not audit-oriented.

#### Phase 8: Another Subscriber Arrives Late

Suppose a second browser connects 30 seconds later.

What happens?

- it gets its own cold snapshot execution from service and DB
- it does not automatically replay 30 seconds of hot live events
- from that point onward, it listens to current live updates

This clearly shows the difference between cold and hot portions of the same endpoint.

#### Phase 9: Client Disconnects

If the browser closes the tab:

- downstream cancels subscription
- `doOnCancel(...)` runs
- `doFinally(...)` runs with cancellation signal

The individual subscriber is gone, but the shared sink can still continue serving other subscribers.

### How You Would Test This With StepVerifier

#### Test Cold Snapshot Behavior

```java
StepVerifier.create(initialSnapshot)
    .expectNextMatches(ShipmentView::isSnapshot)
    .expectComplete()
    .verify();
```

#### Test Backpressure Demand

```java
StepVerifier.create(liveUpdates, 0)
    .then(() -> {
        shipmentSink.tryEmitNext(new ShipmentUpdate("42", "A"));
        shipmentSink.tryEmitNext(new ShipmentUpdate("42", "B"));
    })
    .thenRequest(1)
    .expectNextCount(1)
    .thenCancel()
    .verify();
```

#### Test End-To-End Stream Shape

```java
StepVerifier.create(Flux.concat(initialSnapshot, liveUpdates).take(2))
    .expectNextMatches(ShipmentView::isSnapshot)
    .then(() -> shipmentSink.tryEmitNext(new ShipmentUpdate("42", "IN_TRANSIT")))
    .expectNextMatches(ShipmentView::isLiveUpdate)
    .expectComplete()
    .verify();
```

### What Would Go Wrong Without Batch 4 Concepts?

- without backpressure strategy, slow clients could accumulate stale live events
- without understanding cold vs hot, teams would misread why new subscribers miss old events
- without `Sinks`, imperative external updates would not integrate cleanly into Reactor
- without StepVerifier, timing and demand behavior would remain guesswork

### Final Batch 4 Memory Model

Use this sentence in interviews:

"Batch 4 is about long-lived and shared streams: backpressure controls producer-consumer mismatch, cold vs hot explains subscriber timing, Sinks bridge external events into Reactor, and StepVerifier proves the behavior signal by signal." 

---

## Batch 4 — Interview Hot Questions

### 1. What is backpressure in one line?

It is the downstream's ability to control how much data upstream should send.

### 2. When should I use `onBackpressureLatest()`?

When only the newest state matters more than preserving every intermediate item, such as dashboards or live location feeds.

### 3. Why is unbounded buffering dangerous?

Because memory grows with backlog and can destabilize the system.

### 4. What is the easiest way to explain cold vs hot?

Cold restarts per subscriber. Hot keeps emitting regardless of who joined late.

### 5. Are WebClient and DB calls hot or cold by default?

Usually cold by default.

### 6. What replaces Processors in modern Reactor?

`Sinks`.

### 7. Why does `tryEmitNext(...)` matter?

Because emission can fail or be rejected, and production-grade code should account for that outcome.

### 8. Why use StepVerifier instead of just `.block()` in tests?

Because StepVerifier checks signals, errors, completion, timing, cancellation, and demand, not just one final value.

---

## Batch 4 — Revision Notes

- One-line summary: Batch 4 is about protecting and reasoning about live/shared streams under real pressure and subscriber timing differences.
- Three keywords: backpressure, hot-cold, sinks.
- One trap: treating every publisher like isolated request-response data instead of understanding shared live behavior.
- One memory trick: cold restarts the movie, hot joins the live broadcast, backpressure controls the gate, sink is the microphone, StepVerifier is the rehearsal script.
