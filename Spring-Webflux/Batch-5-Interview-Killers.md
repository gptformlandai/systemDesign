# Batch 5 — Interview Killers: Tricky Concepts and War Stories

> Goal: understand the production mistakes and conceptual traps that expose whether someone has truly used WebFlux in real systems or only knows the surface syntax.

---

## 0. War-Story Mental Model First

Batch 5 is where interviews stop rewarding theory alone.

At this stage, interviewers usually test whether you can answer:

- what breaks if someone sneaks blocking code into the reactive chain?
- how do long-lived subscriptions leak memory or connections?
- why does R2DBC exist, and why does JPA/Hibernate not fit the same model well?
- what are the real WebClient footguns?
- when do you use SSE vs WebSocket?
- when should you reject WebFlux entirely?

### The Six Questions To Ask In Production

1. Is any part of this flow secretly blocking?
2. Who owns this subscription, and who cleans it up?
3. Is the database access truly reactive, or only wrapped in reactive syntax?
4. Is the HTTP client call truly non-blocking and properly consumed?
5. Does this endpoint need request-response, server push, or full duplex communication?
6. Is WebFlux the right tool here, or am I forcing it where MVC would be simpler and safer?

### The Core Rules Of Batch 5

- Returning `Mono` or `Flux` at the controller is not enough if the underlying code is still blocking.
- Long-lived subscriptions and replay/buffer patterns need explicit lifecycle thinking.
- Reactive transactions are not just JDBC transactions with different syntax.
- WebClient is powerful but easy to misuse if you ignore status handling, body consumption, or timeouts.
- SSE and WebSocket solve different communication patterns.
- WebFlux loses when the workload is blocking, CPU-heavy, tiny in scale, or too complex for the team's needs.

### Simple Analogy

Think of Batch 5 as the fire inspection for a building.

- Batch 1-4 taught you how the building is designed.
- Batch 5 checks whether the doors jam, the wires overheat, the pipes leak, and whether the building should even have been constructed that way.

---

## 19. Blocking In Reactive Chains (The Cardinal Sin)

### What It Changes In The Request Flow

Blocking inside a reactive chain destroys the main reason WebFlux exists.

If a request reaches WebFlux and then calls:

- `.block()`
- `.toFuture().get()`
- `Thread.sleep(...)`
- blocking JDBC
- blocking SDK or RestTemplate without isolation

then the request is no longer truly non-blocking at that point.

### Simple Analogy

Think of an airport moving walkway.

- WebFlux assumes passengers keep moving without stopping the walkway.
- Blocking code is someone sitting down in the middle of the moving walkway and refusing to move.

Now people behind them are delayed too.

### The Two Main Failure Modes

#### 1. Event Loop Starvation

If the blocking call happens on a Netty event loop thread:

- that thread is stuck waiting
- other channels assigned to that loop can get delayed
- throughput and latency degrade for unrelated requests too

#### 2. Deadlock / Self-Waiting Patterns

Sometimes blocking is used while waiting for a reactive result that itself needs the same runtime resources to continue.

That can create deadlocks or "hung" flows.

### Typical Wrong Examples

```java
@GetMapping("/orders/{id}")
Mono<OrderView> getOrder(@PathVariable String id) {
    Customer customer = customerClient.getCustomer(id).block();
    return Mono.just(new OrderView(customer));
}
```

or:

```java
Mono<String> wrong = Mono.just(blockingSdk.fetchValue());
```

Both break the model, just at different points.

### Correct Direction

If code is already non-blocking, stay fully reactive.

If code is blocking and you cannot replace it yet:

- wrap it lazily with `fromCallable(...)`
- isolate it on `boundedElastic()`

```java
Mono<RiskScore> riskMono = Mono.fromCallable(() -> legacyRiskSdk.fetch(customerId))
    .subscribeOn(Schedulers.boundedElastic());
```

### Why BlockHound Matters

BlockHound helps detect blocking calls on non-blocking threads during development and testing.

It is not a silver bullet, but it is a great safety net.

```java
BlockHound.install();
```

If code accidentally blocks on a non-blocking thread, BlockHound can fail fast instead of letting the issue hide until production.

### Backfires That Might Occur

- calling `.block()` in controller or service code
- wrapping blocking code in `Mono.just(...)` and thinking it is safe
- using Feign/RestTemplate/JPA inside WebFlux request flow without isolation
- hiding a blocking SDK behind a method name that looks reactive

### Anti-Patterns

- `.block()` anywhere in request handling except truly explicit application boundary code
- "reactive outside, blocking inside" architecture
- mixing JDBC repositories into WebFlux and assuming the controller return type makes it reactive
- using blocking logging appenders or libraries on critical reactive threads

### Code Sample (Java)

```java
Mono<CustomerRiskView> safeFlow = customerClient.getCustomer(customerId)
    .zipWith(Mono.fromCallable(() -> legacyRiskSdk.fetch(customerId))
        .subscribeOn(Schedulers.boundedElastic()))
    .map(tuple -> new CustomerRiskView(tuple.getT1(), tuple.getT2()));
```

### Interview Trap

"Using `.block()` is okay inside a controller because eventually the client needs a real value anyway."

That is wrong. Spring subscribes and writes the response for you. Blocking inside the request path wastes the event-driven model and can stall critical threads.

### Quick Revision Notes

- blocking inside WebFlux is the main cardinal sin
- `Mono.just(blockingCall())` computes too early
- `.block()` turns reactive flow back into waiting thread flow
- use `fromCallable(...).subscribeOn(boundedElastic())` only as a bridge
- Trap to remember = reactive return type does not make blocking internals safe

---

## 20. Memory Leaks and Subscription Cleanup

### What It Changes In The Request Flow

Reactive systems can leak resources in ways imperative systems often hide differently.

Common leak sources include:

- forgotten long-lived subscriptions
- replay/caching of too much data
- unbounded buffering
- open streaming responses with no cleanup thinking
- WebClient responses not fully consumed
- manual subscriptions stored forever

### Simple Analogy

Think of a hotel with rooms and keys.

- each subscription is a room in use
- if checkout never happens, the room stays reserved forever
- eventually the hotel looks full even if nobody is actively using the rooms well

### Where Leaks Commonly Happen

#### Manual `subscribe()` Ownership

If your application manually subscribes to a long-lived `Flux`, someone must own the `Disposable` and dispose it when appropriate.

```java
Disposable disposable = eventFlux.subscribe(this::handleEvent);
```

If this is never disposed:

- subscriber remains active
- references remain alive
- upstream work may continue forever

#### Unbounded Replay / Cache Patterns

Operators like `cache()` or replay-style patterns can hold large histories.

If used on high-volume or infinite streams, memory can keep growing.

#### Slow Consumers + Buffering

If the system buffers faster than downstream drains, memory can balloon.

#### Streaming Endpoints Without Cleanup

SSE and WebSocket handlers need proper cancellation and cleanup behavior when clients disconnect.

### Why `timeout`, `cancel`, and `doFinally` Matter

- `timeout(...)` can terminate zombie flows that should not hang forever
- cancellation frees resources when client goes away
- `doFinally(...)` gives one cleanup hook for success, error, or cancellation

### Connection Pool Exhaustion Pattern

One common production symptom is not a Java heap OOM first. It is connection pool exhaustion.

Why?

- subscriptions stay alive too long
- HTTP responses or DB resources are not released properly
- slow leaks accumulate until no resources are available for new work

### Backfires That Might Occur

- manual background subscriptions with no disposal path
- infinite `Flux` combined with `collectList()` or unbounded replay
- buffering large amounts of data for slow consumers
- forgetting cleanup on streaming disconnects

### Anti-Patterns

- `flux.subscribe()` in service layer with no ownership model
- `cache()` on endless or massive streams without retention control
- `collectList()` on effectively unbounded data
- ignoring cancel and terminate hooks in long-lived flows

### Code Sample (Java)

```java
Disposable subscription = notificationFlux
    .timeout(Duration.ofMinutes(5))
    .doFinally(signal -> log.info("notification stream ended with {}", signal))
    .subscribe(this::handleNotification);

// later
subscription.dispose();
```

### Interview Trap

"Spring manages subscription cleanup for every reactive flow automatically, so leaks are not really a concern."

That is wrong. Spring manages request lifecycle for request-bound publishers, but manual subscriptions, replay caches, and shared streams still require explicit ownership and cleanup thinking.

### Quick Revision Notes

- leaks often come from long-lived or manually owned flows
- `Disposable` ownership matters
- unbounded replay/buffer/cache patterns are dangerous
- `doFinally` is a strong cleanup hook
- Trap to remember = not all subscriptions are request-scoped and auto-cleaned

---

## 21. Reactive Transactions and R2DBC

### What It Changes In The Request Flow

Reactive transactions are not the same mental model as JDBC transactions.

Classic JDBC transaction assumptions often depend on:

- blocking calls
- thread-bound transaction context
- JPA/Hibernate session behavior

Reactive transactions work differently because request execution may cross threads and the driver itself must be non-blocking.

### Simple Analogy

JDBC transaction model is like one banker handling your paperwork at one desk the whole time.

Reactive transaction model is like your transaction packet moving through a coordinated workflow system where the transaction state must travel with the flow, not just with one clerk.

### Why R2DBC Exists

R2DBC exists because JDBC is blocking.

In WebFlux, if the database access is still blocking JDBC:

- request threads wait
- event loops can be harmed if isolation is not correct
- the stack is no longer end-to-end reactive

R2DBC gives:

- reactive database access
- non-blocking drivers
- transaction integration for reactive chains

### Transaction Control Options

#### `@Transactional`

Works only when the application is configured with a reactive transaction manager and the method returns reactive types.

#### `TransactionalOperator`

Often clearer in interviews because it makes the reactive transaction boundary explicit.

```java
Mono<OrderResult> result = orderRepository.save(order)
    .then(auditRepository.save(audit))
    .as(transactionalOperator::transactional);
```

### Why Hibernate/JPA Is A Bad Fit Here

Main reasons:

- JPA/Hibernate uses blocking JDBC underneath
- session and lazy-loading assumptions fit imperative thread-bound flows better
- returning `Mono` from controller does not make Hibernate reactive

The senior answer is not "Hibernate never works anywhere." The senior answer is:

- Hibernate/JPA is fine for imperative MVC stacks
- it is not the right foundation for true end-to-end WebFlux data access

### Connection Pooling Still Matters

Reactive does not mean "no pool needed."

With R2DBC:

- use connection pooling such as `r2dbc-pool`
- size it thoughtfully
- watch transaction duration and backpressure interactions

### Why This Matters In Request Flow

Suppose request does:

1. call payment service
2. insert order row
3. insert audit row
4. update loyalty row

If these DB steps must be atomic, you need a reactive transaction boundary across them.

That boundary must fit reactive execution, not rely on JDBC thread-bound behavior.

### Backfires That Might Occur

- using JPA repository in WebFlux and thinking the app is now reactive
- mixing imperative transaction assumptions into asynchronous flows
- holding transaction open across long remote waits when it is not necessary
- assuming `@Transactional` behaves identically regardless of blocking or reactive driver stack

### Anti-Patterns

- WebFlux controller + JPA repository + blocking JDBC
- opening DB transaction too early before remote calls that may take long
- trying to force Hibernate lazy-loading semantics into reactive flow
- ignoring R2DBC connection pool sizing under concurrency

### Code Sample (Java)

```java
Mono<OrderConfirmation> confirmation = paymentClient.authorize(order.id())
    .flatMap(payment -> orderRepository.save(order.withPayment(payment.id()))
        .then(auditRepository.save(new AuditEntry(order.id(), "ORDER_SAVED")))
        .then(loyaltyRepository.addPoints(order.customerId(), 10))
        .thenReturn(new OrderConfirmation(order.id(), payment.id())))
    .as(transactionalOperator::transactional);
```

### Interview Trap

"If my controller returns `Mono`, using JPA underneath is still basically reactive enough."

That is wrong. That is reactive syntax wrapped around a blocking persistence stack.

### Quick Revision Notes

- R2DBC exists for non-blocking DB access
- reactive transaction context is not just JDBC with new wrappers
- `TransactionalOperator` is interview-friendly and explicit
- JPA/Hibernate is usually the wrong persistence model for end-to-end WebFlux
- Trap to remember = controller return type does not define the real blocking behavior underneath

---

## 22. WebClient vs RestTemplate

### What It Changes In The Request Flow

HTTP client choice determines whether outbound service-to-service calls preserve the non-blocking model or drag the request back into a blocking one.

### Simple Analogy

- RestTemplate is like making a phone call and waiting on the line until the other side answers.
- WebClient is like registering an async callback with a dispatcher who notifies you when the result arrives.

### Why WebClient Matters In WebFlux

`RestTemplate` is blocking.

`WebClient` is the reactive HTTP client designed to work naturally with WebFlux.

If a WebFlux service calls many downstream services:

- RestTemplate ties up threads during network waits
- WebClient lets the system continue handling other work during those waits

### `retrieve()` vs lower-level exchange handling

In interview language, older material often mentions `exchange()`.

In current Spring practice, the better answer is usually:

- `retrieve()` for common success/error mapping
- `exchangeToMono(...)` or `exchangeToFlux(...)` when you need full control over status and response handling

Why this matters:

- low-level response handling gives power
- but mishandling it can leak resources if the response body is not properly consumed or released

### Typical Safe Usage

```java
Mono<Customer> customerMono = webClient.get()
    .uri("/customers/{id}", id)
    .retrieve()
    .bodyToMono(Customer.class);
```

### When More Control Is Needed

```java
Mono<Customer> customerMono = webClient.get()
    .uri("/customers/{id}", id)
    .exchangeToMono(response -> {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(Customer.class);
        }
        if (response.statusCode().value() == 404) {
            return Mono.empty();
        }
        return response.createException().flatMap(Mono::error);
    });
```

### Timeouts And Connection Pooling

A production WebClient setup should think about:

- connect timeout
- response timeout
- connection pool size
- pending acquire behavior

Because otherwise the service may appear fine in low traffic but fail under concurrency.

### Common Footguns

- creating a new WebClient instance per request unnecessarily
- forgetting timeouts and allowing slow downstreams to hang too long
- using low-level response handling and not consuming the body correctly
- calling `.block()` on the WebClient result inside reactive request handling

### Why This Matters In Request Flow

Suppose API gateway fans out to 5 services.

If each call is blocking:

- waiting threads multiply
- concurrency density collapses

If each call uses WebClient correctly:

- waiting does not pin a worker thread per call
- fan-out works much better at scale

### Backfires That Might Occur

- connection pool exhaustion from poor client tuning or unconsumed responses
- hidden blocking by calling `.block()` after WebClient
- error handling gaps when `retrieve()` is not enough but custom logic is missing
- creating too many clients or misconfiguring connection pools

### Anti-Patterns

- WebFlux app using RestTemplate for all downstream calls
- `webClient.get(...).retrieve().bodyToMono(...).block()` inside request flow
- no timeout configuration in latency-sensitive services
- low-level response handling without body consumption discipline

### Code Sample (Java)

```java
HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofSeconds(2));

WebClient client = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();

Mono<PaymentStatus> paymentStatus = client.get()
    .uri("http://payment-service/payments/{id}", orderId)
    .retrieve()
    .bodyToMono(PaymentStatus.class);
```

### Interview Trap

"If I use WebClient, I am safe by default no matter how I consume the response or configure timeouts."

That is wrong. WebClient fits WebFlux well, but misuse still causes leaks, hangs, and poor pool behavior.

### Quick Revision Notes

- WebClient is the non-blocking HTTP client choice for WebFlux
- `retrieve()` is common; `exchangeToMono/Flux` is for full control
- body consumption and timeouts matter
- RestTemplate is blocking and usually the wrong fit for WebFlux request paths
- Trap to remember = WebClient can still be misused badly

---

## 23. SSE, WebSocket and Streaming Responses

### What They Change In The Request Flow

Now the question is not just how to return one response.

It is how to keep a client connected and continue sending updates over time.

### SSE vs WebSocket In One Line

- SSE = server-to-client one-way streaming over normal HTTP
- WebSocket = full duplex two-way connection

### Simple Analogy

- SSE is like a radio station broadcasting updates to listeners
- WebSocket is like a live phone call where both sides can speak anytime

### When SSE Is The Better Choice

Use SSE when:

- server pushes updates to browser/client
- client does not need full duplex messaging
- simple HTTP-friendly streaming is enough
- reconnection behavior is desired with browser compatibility

In Spring WebFlux, a common SSE shape is:

```java
@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<OrderEvent>> events() {
    return orderEventFlux.map(event -> ServerSentEvent.builder(event).build());
}
```

### When WebSocket Is The Better Choice

Use WebSocket when:

- both client and server send messages frequently
- low-latency bidirectional messaging matters
- chat, multiplayer, collaborative editing, or command streaming is needed

In Spring WebFlux, this is often modeled with a `WebSocketHandler`.

```java
public class OrderSocketHandler implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<WebSocketMessage> outbound = orderEventFlux
            .map(event -> session.textMessage(event.toString()));

        return session.send(outbound);
    }
}
```

### Why This Matters In Request Flow

Streaming endpoints are long-lived.

That means you must think about:

- cancellation on disconnect
- backpressure or slow-client strategy
- heartbeat/keepalive behavior
- resource cleanup
- scaling across instances if updates come from distributed sources

### SSE Strengths

- simpler than WebSocket
- works naturally with HTTP infrastructure
- great for dashboards, progress updates, notifications

### WebSocket Strengths

- bidirectional
- flexible real-time interaction
- fits client-server conversations rather than just push

### Backfires That Might Occur

- choosing WebSocket when SSE is simpler and enough
- forgetting heartbeats or disconnect handling for long-lived connections
- assuming one-node in-memory stream model scales automatically across many instances
- pushing every use case into streaming when normal request-response is better

### Anti-Patterns

- WebSocket for one-way status feed that SSE could handle more simply
- SSE without slow-client/backpressure thinking on hot high-volume streams
- in-memory hot stream only, with no distributed event source strategy for multi-instance deployment
- forgetting cleanup hooks on disconnect

### Code Sample (Java)

```java
@GetMapping(value = "/orders/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ServerSentEvent<OrderProgress>> progress(@PathVariable String id) {
    return progressSink.asFlux()
        .filter(event -> event.orderId().equals(id))
        .map(event -> ServerSentEvent.builder(event).build())
        .doOnCancel(() -> log.info("SSE client disconnected for {}", id));
}
```

### Interview Trap

"WebSocket is always the better real-time choice because it is more powerful than SSE."

That is wrong. More powerful does not mean better fit. If the need is one-way server push, SSE is often simpler and more operationally friendly.

### Quick Revision Notes

- SSE = one-way server push over HTTP
- WebSocket = full duplex connection
- choose by communication pattern, not hype
- long-lived streams need cleanup and scaling design
- Trap to remember = WebSocket is not automatically superior for all streaming cases

---

## 24. Performance: When WebFlux Actually Loses

### What It Changes In The Architecture Decision

This topic is about engineering honesty.

WebFlux is not a universal upgrade over MVC.

It wins in the right problem shape, not in every problem shape.

### Simple Analogy

Using WebFlux for the wrong system is like buying a race bike for a small office hallway.

It is technically impressive, but the environment does not reward it.

### When WebFlux Usually Wins

- high concurrency
- heavy I/O wait
- service fan-out to many downstreams
- streaming APIs
- end-to-end reactive stack

### When WebFlux Often Loses

#### 1. CPU-Bound Workloads

If the service mostly burns CPU instead of waiting on I/O, WebFlux gives less benefit.

Why?

- there is not much waiting to optimize away
- scheduler and reactive complexity may add overhead without enough payoff

#### 2. Blocking JDBC/JPA-Centric Systems

If persistence is dominated by blocking JPA/JDBC and you are not moving to R2DBC, MVC is often cleaner.

#### 3. Small Or Low-Concurrency CRUD Services

If traffic is modest and architecture is simple:

- MVC may be easier to debug
- onboarding cost is lower
- reactive complexity may not pay for itself

#### 4. Teams Without Reactive Maturity

If the team cannot reason about:

- lazy execution
- thread hops
- cleanup
- debugging signal flows

then production incidents can take longer to diagnose than the scale benefit is worth.

### The Senior Interview Answer

Do not say:

- "WebFlux is better because it is newer"

Say:

- "WebFlux is valuable when the system spends significant time waiting on I/O and the stack can stay non-blocking end to end. Otherwise MVC may be the better trade-off."

### Why This Matters In Request Flow

Suppose you have an internal admin CRUD app with:

- low traffic
- JPA repositories
- no streaming
- no large fan-out

If you convert it to WebFlux:

- thread savings may be marginal
- debugging becomes harder
- JPA still blocks
- developers may misuse the model and create hybrid complexity

That is a loss, not a win.

### Backfires That Might Occur

- rewriting a stable MVC service to WebFlux without performance evidence
- keeping a blocking persistence layer and expecting reactive magic
- using WebFlux for small apps where operational simplicity matters more
- creating a harder-to-debug system without meaningful load benefit

### Anti-Patterns

- "reactive because modern"
- `Mono.just(jpaRepository.findById(id))`
- adopting WebFlux where no streaming or I/O concurrency pressure exists
- measuring only average latency and ignoring complexity cost

### Code Sample (Java)

```java
// Looks reactive, but is not really a good WebFlux fit.
@GetMapping("/admin/users/{id}")
Mono<User> getAdminUser(@PathVariable Long id) {
    return Mono.just(userJpaRepository.findById(id).orElseThrow());
}
```

This is not a strong WebFlux design:

- JPA is blocking
- work is computed during assembly
- the service shape is probably better served by MVC if the whole stack is imperative

### Interview Trap

"WebFlux always outperforms Spring MVC under load."

That is wrong. WebFlux is better for the right I/O-bound concurrency profile. It is not a blanket performance win.

### Quick Revision Notes

- WebFlux wins on non-blocking I/O concurrency, not everywhere
- CPU-bound or JDBC-heavy apps often gain less or even lose in net value
- complexity cost is part of the performance discussion
- Trap to remember = newer and more scalable in theory does not mean better for every service

---

## Batch 5 — Putting The Traps Into One Production Story

Now connect all six topics into one realistic production war story.

### Use Case

Endpoint: `POST /orders/{id}/confirm`

Business need:

- call inventory service
- call payment service
- persist order + audit atomically
- push live order status updates to the UI
- keep service scalable under concurrency

### The Wrong Design That Fails In Production

Team builds WebFlux controller, but inside it they do this:

- use `RestTemplate` for inventory and payment calls
- use JPA repository for order save
- call `.block()` to simplify control flow
- publish UI updates over WebSocket without cleanup thinking
- create a shared replay stream with too much retained history

At first, traffic is light and everything seems fine.

Then under load:

- event loops get stalled by blocking calls
- response times spike
- connection pools fill up
- memory rises because of replay/buffer misuse
- clients disconnect but subscriptions linger
- production debugging becomes painful

This is exactly the hybrid architecture trap.

### The Better Design

1. Use WebClient for inventory and payment calls
2. Use R2DBC repositories for DB writes
3. Use `TransactionalOperator` for atomic DB work
4. If a legacy blocking fraud SDK still exists, isolate it on `boundedElastic()`
5. Use SSE for one-way status push if client only needs updates
6. Use sink/shared stream carefully and clean up on cancel

### Correct Flow Sketch

```java
Mono<OrderStatusResponse> confirmOrder(String id) {
    Mono<InventoryResult> inventoryMono = inventoryClient.reserve(id);
    Mono<PaymentResult> paymentMono = paymentClient.charge(id);
    Mono<FraudResult> fraudMono = Mono.fromCallable(() -> legacyFraudSdk.check(id))
        .subscribeOn(Schedulers.boundedElastic());

    return Mono.zip(inventoryMono, paymentMono, fraudMono)
        .flatMap(tuple -> orderRepository.save(new Order(id, tuple.getT1(), tuple.getT2(), tuple.getT3()))
            .then(auditRepository.save(new AuditEntry(id, "ORDER_CONFIRMED")))
            .thenReturn(new OrderStatusResponse(id, "CONFIRMED")))
        .as(transactionalOperator::transactional);
}
```

### Full Request Life Journey

#### Phase 1: Request Arrives

1. client calls `POST /orders/42/confirm`
2. WebFlux receives request on Netty event loop
3. controller assembles reactive pipeline

#### Phase 2: Subscription Starts Execution

Spring subscribes after controller returns `Mono<OrderStatusResponse>`.

Now:

- WebClient starts non-blocking inventory and payment calls
- legacy fraud SDK runs on `boundedElastic()`

If someone had used `.block()` here, the whole benefit would start collapsing.

#### Phase 3: Remote Results Arrive

- inventory and payment results come back through non-blocking I/O
- fraud result comes back from `boundedElastic()` after blocking call finishes

If WebClient had been replaced with RestTemplate, waiting threads would now be tied up.

#### Phase 4: Atomic Persistence Happens

Order save and audit save execute inside a reactive transaction boundary using R2DBC.

If JPA/JDBC had been used instead, the architecture would no longer be end-to-end reactive.

#### Phase 5: Status Streaming To UI

After confirmation, order progress updates can be pushed over SSE or WebSocket.

If the UI only needs server-to-client updates, SSE is usually simpler.

If the team instead uses long-lived streams with poor cleanup or unbounded replay, memory and connection issues begin accumulating.

#### Phase 6: Production Decision Point

Now ask the real senior question:

Was WebFlux the correct choice for this service?

If the system has:

- multiple downstream service calls
- streaming updates
- high concurrency
- R2DBC/non-blocking access

then yes, WebFlux may be a strong fit.

If instead the system were:

- simple CRUD
- JPA-heavy
- low traffic
- no streaming

then MVC might be the wiser choice.

### Final Batch 5 Memory Model

Use this sentence in interviews:

"Batch 5 is where WebFlux stops being syntax and becomes production engineering: avoid blocking, own subscription lifecycle, keep persistence truly reactive, use WebClient correctly, pick the right streaming model, and be honest about when WebFlux is not the best tool." 

---

## Batch 5 — Interview Hot Questions

### 1. Why is `.block()` called the cardinal sin in WebFlux?

Because it turns non-blocking execution back into waiting-thread execution and can stall critical reactive threads.

### 2. What is a common source of reactive memory leaks?

Manual long-lived subscriptions, unbounded replay/cache/buffer patterns, and poor cleanup of streaming flows.

### 3. Why does R2DBC matter if my controller already returns `Mono`?

Because the persistence layer itself must be non-blocking for the stack to remain end-to-end reactive.

### 4. Why is RestTemplate a poor fit for WebFlux request paths?

Because it is blocking and wastes the thread-efficiency benefits of the reactive model.

### 5. When is SSE better than WebSocket?

When the need is one-way server push and full duplex messaging is unnecessary.

### 6. What is the most honest answer to "Should we always use WebFlux?"

No. Use it where non-blocking I/O concurrency or streaming gives real value; otherwise MVC may be simpler and better.

### 7. Why does `TransactionalOperator` sound stronger in interviews than just mentioning `@Transactional`?

Because it makes the reactive transaction boundary explicit and shows you understand the flow, not just the annotation.

### 8. What is the hidden WebClient leak trap?

Low-level response handling without properly consuming or releasing the body, plus poor timeout and connection-pool discipline.

---

## Batch 5 — Revision Notes

- One-line summary: Batch 5 is about the production failure modes that reveal whether a WebFlux design is truly reactive, safely managed, and worth using at all.
- Three keywords: blocking, lifecycle, honesty.
- One trap: confusing reactive controller signatures with genuinely reactive internals.
- One memory trick: do not stop the walkway, do not forget checkout, keep the DB truly reactive, use the right client, pick the right live channel, and know when to walk away from WebFlux.
