# Spring WebFlux — Interview Mastery Index

> Goal: Cover Spring WebFlux end-to-end at senior engineer depth (6+ YOE). No fluff — core ideology, tricky internals, and interview-ready answers.

---

## Track Layout

Each topic is a standalone section inside a single file per batch. Every section follows the mentorship template (intuition → definition → how it works → code → tricky questions → revision notes).

---

## Batch 1 — Foundations: Reactive Core & Runtime

> Why WebFlux exists, what makes it fundamentally different from Spring MVC, and the runtime that powers it.

| # | Topic | Core Focus |
|---|---|---|
| 1 | **Reactive Programming Model** | Imperative vs Declarative vs Reactive. Publisher-Subscriber contract. Why "pull-based demand" matters. |
| 2 | **Mono and Flux** | Mono = 0..1, Flux = 0..N. Nothing happens until you subscribe. Lazy evaluation. Assembly vs Execution time. |
| 3 | **Event Loop & Netty Runtime** | How Netty's event loop replaces thread-per-request. Channel, EventLoopGroup, non-blocking I/O. Why fewer threads handle more load. |
| 4 | **Spring WebFlux vs Spring MVC** | Servlet stack vs Reactive stack. When WebFlux wins, when MVC is still better. Thread model comparison. Annotation vs Functional endpoints. |
| 5 | **Project Reactor Lifecycle** | Assembly phase → Subscription phase → Data flows. onSubscribe → request(n) → onNext → onComplete/onError. Signal flow internals. |

---

## Batch 2 — Operators: The Workbench

> The operators you must know cold — what they do, when to pick one over the other, and the tricky gotchas interviewers love.

| # | Topic | Core Focus |
|---|---|---|
| 6 | **map vs flatMap vs concatMap vs switchMap** | Synchronous transform vs async expansion. Ordering guarantees. Interleaving behavior of flatMap. Why concatMap preserves order. switchMap cancels previous. |
| 7 | **filter, take, skip, distinct** | Filtering operators. take(n) and cancellation signal. distinct vs distinctUntilChanged. |
| 8 | **doOn Callbacks (Lifecycle Hooks)** | doOnNext, doOnError, doOnComplete, doOnSubscribe, doOnCancel, doOnTerminate, doFinally. Side-effect-only — never modify the signal. |
| 9 | **zip, merge, concat, combineLatest** | Combining publishers. zip = waits for all. merge = interleaved. concat = sequential. combineLatest = latest from each. |
| 10 | **Error Handling Operators** | onErrorReturn, onErrorResume, onErrorMap, retry, retryWhen. Where in the chain they catch. Retry with backoff (Retry.backoff). |

---

## Batch 3 — Schedulers, Context & Execution Control

> Where code actually runs, how to shift threads, and the reactive replacement for ThreadLocal.

| # | Topic | Core Focus |
|---|---|---|
| 11 | **Schedulers** | Schedulers.parallel(), boundedElastic(), single(), immediate(). Which for CPU-bound vs I/O-bound. Why boundedElastic for blocking calls. |
| 12 | **publishOn vs subscribeOn** | publishOn = shifts downstream. subscribeOn = shifts subscription (upstream source). Only one subscribeOn matters. Multiple publishOn allowed. |
| 13 | **Reactor Context (replacing ThreadLocal)** | Why ThreadLocal is broken in reactive. Context = immutable map propagated upstream. contextWrite / contextView. Real use: tracing, auth tokens, MDC. |
| 14 | **defer and fromCallable** | Mono.defer = lazy publisher creation per subscriber. Mono.fromCallable = wraps blocking call. Why defer fixes the "hot source at assembly time" bug. |

---

## Batch 4 — Backpressure, Hot vs Cold, Advanced Patterns

> The concepts that separate senior candidates from mid-level. Interviewers test these to gauge real reactive experience.

| # | Topic | Core Focus |
|---|---|---|
| 15 | **Backpressure** | request(n) protocol. What happens when producer is faster than consumer. Strategies: buffer, drop, latest, error. onBackpressureBuffer/Drop/Latest/Error. |
| 16 | **Hot vs Cold Publishers** | Cold = replay from start per subscriber. Hot = emit regardless of subscribers. Sinks, share(), replay(), publish().autoConnect(). ConnectableFlux. |
| 17 | **Processor / Sinks API** | Sinks.many().multicast(), unicast(), replay(). Sinks.one(). Thread-safe emission. Why Processors are deprecated in favor of Sinks. |
| 18 | **StepVerifier & Testing** | StepVerifier.create(). expectNext, expectComplete, expectError. Virtual time testing with withVirtualTime(). Verifying backpressure with thenRequest(n). |

---

## Batch 5 — Interview Killers: Tricky Concepts & War Stories

> The questions that catch 90% of candidates off guard. Real debugging scenarios and conceptual traps.

| # | Topic | Core Focus |
|---|---|---|
| 19 | **Blocking in Reactive Chains (The Cardinal Sin)** | .block() on a non-blocking thread = deadlock. Detecting blocking calls (BlockHound). Wrapping legacy blocking code safely. |
| 20 | **Memory Leaks & Subscription Cleanup** | Disposable. Why not disposing = leak. Timeout + cancel. doFinally for cleanup. Connection pool exhaustion pattern. |
| 21 | **Reactive Transactions & R2DBC** | R2DBC vs JDBC. @Transactional in reactive. TransactionalOperator. Connection pooling with r2dbc-pool. Why Hibernate doesn't work here. |
| 22 | **WebClient vs RestTemplate** | Non-blocking HTTP. retrieve() vs exchange(). Memory leak if response body not consumed. Connection pool tuning. Timeout configuration. |
| 23 | **SSE, WebSocket & Streaming Responses** | Server-Sent Events with Flux<ServerSentEvent>. WebSocket with WebSocketHandler. MediaType.TEXT_EVENT_STREAM. Real-time use cases. |
| 24 | **Performance: When WebFlux Actually Loses** | CPU-bound work. Small user base. JDBC-only backends. Debugging complexity. When the throughput gain doesn't justify the cognitive cost. |

---

## Batch 6 — System Design Angle: WebFlux in Architecture

> How to talk about WebFlux in a system design interview — not just code, but architectural decisions.

| # | Topic | Core Focus |
|---|---|---|
| 25 | **API Gateway with WebFlux (Spring Cloud Gateway)** | Why gateways must be non-blocking. Route predicates, filters, rate limiting. Netty under the hood. |
| 26 | **Reactive Kafka & Messaging** | reactor-kafka. Consuming and producing reactively. Backpressure from Kafka partitions. Commit strategies. |
| 27 | **Reactive Microservices Patterns** | Circuit breaker with Resilience4j reactive. Reactive service-to-service calls. Bulkhead with Schedulers. Timeout propagation. |
| 28 | **Observability in Reactive Stacks** | Micrometer + Reactor. Context propagation for tracing (Brave/OpenTelemetry). Structured logging challenge in reactive. Hooks.onOperatorDebug() vs ReactorDebugAgent. |

---

## Platinum Add-Ons — Latest Production Surface

> These complete the official WebFlux surface area for senior and FAANG-style interviews.

| # | Document | Core Focus |
|---|---|---|
| P1 | **Platinum-1-Security-Errors-Versioning.md** | Reactive security, 401/403, validation, structured errors, API versioning, CORS, HTTP caching. |
| P2 | **Platinum-2-Codecs-DataBuffer-Memory-Uploads.md** | Codecs, `DataBuffer`, memory limits, file upload/download, NDJSON/SSE body streaming, large-body protection. |
| P3 | **Platinum-3-HTTP-Interfaces-RSocket-HTTP2-WebTestClient.md** | HTTP interface clients, deeper WebClient, filters, timeouts, RSocket, HTTP/2, WebTestClient, streaming tests. |
| P4 | **Platinum-4-Modern-Reactor-Context-VirtualThreads-Debugging.md** | Modern Reactor context propagation, Java 21 virtual-thread boundedElastic, debugging, BlockHound, cancellation, production playbooks. |

---

## Final Drill Bank — Interview Pressure Practice

| Document | Core Focus |
|---|---|
| **WebFlux-Interview-Drill-Bank-Tricky-Scenarios-Gold-Sheet.md** | Tricky scenario questions, output/thread prediction, cancellation, WebClient traps, R2DBC, DataBuffer, security, and short spoken answers. |

Use this after all batches and platinum add-ons. It is the 30-minute pre-interview sheet.

---

## Quick Reference — Interview Cheat Sheet

Available in:

- [All-In-One-Version.md](Spring-Webflux/All-In-One-Version.md)
- the final revision section of each batch
- the final revision section of each platinum add-on
- [WebFlux-Interview-Drill-Bank-Tricky-Scenarios-Gold-Sheet.md](Spring-Webflux/WebFlux-Interview-Drill-Bank-Tricky-Scenarios-Gold-Sheet.md)

---

## Execution Plan

1. Review this index → ✅ You are here
2. Batch 1 → Foundations file
3. Batch 2 → Operators file
4. Batch 3 → Schedulers & Context file
5. Batch 4 → Backpressure & Advanced file
6. Batch 5 → Interview Killers file
7. Batch 6 → Architecture file
8. Platinum 1 → Security, errors, versioning
9. Platinum 2 → Codecs, DataBuffer, memory, uploads/downloads
10. Platinum 3 → HTTP interfaces, RSocket, HTTP/2, WebTestClient
11. Platinum 4 → Modern Reactor, virtual threads, debugging
12. All-In-One → final story-mode revision
13. Drill Bank → final interview pressure practice

Each batch targets **one sitting of focused reading**. Code samples in Java throughout.

---

## Final Review Assets

- [All-In-One-Version.md](Spring-Webflux/All-In-One-Version.md) — story-mode end-to-end request travel using all six batches, cheat sheet, ultra-short revision, and Marriott-style interview questions with spoken answers.
- [Platinum-1-Security-Errors-Versioning.md](Spring-Webflux/Platinum-1-Security-Errors-Versioning.md) — production API safety and evolution.
- [Platinum-2-Codecs-DataBuffer-Memory-Uploads.md](Spring-Webflux/Platinum-2-Codecs-DataBuffer-Memory-Uploads.md) — byte/body/memory handling.
- [Platinum-3-HTTP-Interfaces-RSocket-HTTP2-WebTestClient.md](Spring-Webflux/Platinum-3-HTTP-Interfaces-RSocket-HTTP2-WebTestClient.md) — clients, protocols, and tests.
- [Platinum-4-Modern-Reactor-Context-VirtualThreads-Debugging.md](Spring-Webflux/Platinum-4-Modern-Reactor-Context-VirtualThreads-Debugging.md) — modern Reactor operations and debugging.
- [WebFlux-Interview-Drill-Bank-Tricky-Scenarios-Gold-Sheet.md](Spring-Webflux/WebFlux-Interview-Drill-Bank-Tricky-Scenarios-Gold-Sheet.md) — final scenario drills and spoken answers.

### Quick Decision Snapshot

Use WebFlux when the service is:

- I/O-heavy
- high-concurrency
- fan-out heavy
- streaming-friendly
- mostly non-blocking end to end

Prefer MVC when the service is:

- simple CRUD
- JPA/JDBC-heavy
- low-concurrency
- CPU-heavy
- not worth the reactive complexity

### Last-Minute Memory Line

WebFlux is about making waiting cheap, not making slow dependencies magically fast.

### Final Platinum Memory Line

WebFlux mastery is not only `Mono`, `Flux`, and operators. It is also security, error
contracts, API evolution, codecs, byte streaming, protocol choice, testing, context
propagation, virtual-thread awareness, and production debugging.
