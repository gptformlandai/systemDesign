# Spring Boot REST Clients WebFlux Resilience Interview Master Sheet

Target: Java Backend / Spring Boot / microservice interviews.

This sheet covers:
- outbound HTTP calls
- `RestTemplate`, `RestClient`, and `WebClient`
- client timeouts
- retries and backoff
- circuit breaker
- rate limiter
- bulkhead
- fallback
- WebFlux basics
- Reactor `Mono` and `Flux`
- blocking vs non-blocking trade-offs

Goal:

```text
After reading this sheet, you should be able to choose the right HTTP client, design safe
microservice-to-microservice calls, explain WebFlux basics, and protect services with
timeouts, retries, circuit breakers, rate limits, and bulkheads.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | REST clients, status codes, timeouts |
| Intermediate | `RestClient`, `WebClient`, DTOs, error handling |
| Senior | retry/backoff, circuit breaker, bulkhead, rate limiter |
| MAANG-ready | cascading failure prevention, reactive trade-offs, production observability |

Strong line:

```text
Every outbound network call needs a timeout, error strategy, observability, and a decision
on whether retry is safe.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| RestTemplate vs RestClient vs WebClient | Very high | Modern client choice |
| Timeouts | Very high | Production survival |
| Error handling | Very high | Real API behavior |
| Retry with backoff | Very high | Transient failure handling |
| Circuit breaker | Very high | Cascading failure prevention |
| Bulkhead | High | Resource isolation |
| Rate limiter | High | Protect downstream/upstream |
| Fallback | High | Graceful degradation |
| WebFlux | High | Reactive awareness |
| Mono and Flux | High | Reactor basics |
| Blocking in reactive flow | Very high | Common production trap |
| Idempotency and retries | Very high | Correctness |

---

# 2. Outbound HTTP Client Choices

| Client | Style | Current Interview Position |
|---|---|---|
| `RestTemplate` | blocking | legacy but still common |
| `RestClient` | blocking fluent API | modern synchronous client |
| `WebClient` | non-blocking/reactive | reactive and async HTTP |
| OpenFeign | declarative HTTP | common in Spring Cloud microservices |

Strong answer:

```text
For new blocking code, I prefer RestClient. For reactive/non-blocking code or high-concurrency
I/O flows, I use WebClient. I still understand RestTemplate because many older services use it.
```

---

# 3. `RestTemplate`

`RestTemplate` is the older blocking Spring HTTP client.

Example:

```java
HotelResponse response = restTemplate.getForObject(
        "https://hotel-service/api/hotels/{id}",
        HotelResponse.class,
        hotelId
);
```

Interview position:

```text
RestTemplate is not the preferred modern choice for new code, but it is common in existing
Spring Boot applications.
```

Main drawbacks:
- less fluent API
- easy to forget timeouts
- older style compared to `RestClient`
- blocking thread-per-request model

---

# 4. `RestClient`

`RestClient` is the modern synchronous HTTP client in Spring Framework.

Example:

```java
@Service
class HotelClient {
    private final RestClient restClient;

    HotelClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://hotel-service")
                .build();
    }

    HotelResponse getHotel(Long hotelId) {
        return restClient.get()
                .uri("/api/hotels/{id}", hotelId)
                .retrieve()
                .body(HotelResponse.class);
    }
}
```

Why it is useful:
- fluent API
- synchronous style
- easier migration path from `RestTemplate`
- works well in MVC/blocking applications

Strong answer:

```text
RestClient is a good default for synchronous Spring MVC services that need a modern,
fluent blocking HTTP client.
```

---

# 5. `WebClient`

`WebClient` is a non-blocking reactive HTTP client.

Example:

```java
@Service
class RateClient {
    private final WebClient webClient;

    RateClient(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://rate-service")
                .build();
    }

    Mono<RateResponse> getRate(Long hotelId) {
        return webClient.get()
                .uri("/api/rates/{hotelId}", hotelId)
                .retrieve()
                .bodyToMono(RateResponse.class);
    }
}
```

Strong answer:

```text
WebClient is useful for non-blocking I/O and reactive flows. It returns Mono or Flux and
should not be mixed with blocking operations carelessly.
```

---

# 6. Timeouts

Every outbound call needs timeouts.

Timeout types:
- connection timeout
- read/response timeout
- write timeout
- overall request timeout

Why:

```text
Without timeouts, a slow dependency can exhaust threads and bring down the caller.
```

Strong answer:

```text
Timeouts are mandatory for service-to-service calls. I set them based on the endpoint's
latency budget and monitor timeout rate separately from other failures.
```

---

# 7. Error Handling

HTTP failures are not all equal.

| Status | Meaning | Typical Handling |
|---|---|---|
| 400 | caller sent bad request | do not retry |
| 401/403 | auth problem | do not retry blindly |
| 404 | missing resource | maybe normal |
| 409 | conflict | business-specific |
| 429 | rate limited | retry later with backoff |
| 500 | server error | retry if safe |
| 503 | unavailable | retry/circuit breaker |

Example:

```java
return restClient.get()
        .uri("/api/hotels/{id}", hotelId)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
            throw new DownstreamClientException("hotel-service client error");
        })
        .body(HotelResponse.class);
```

Strong answer:

```text
I do not treat all exceptions the same. Client errors, auth errors, rate limits, timeouts,
and server errors have different retry and fallback strategies.
```

---

# 8. Retry

Retry means trying again after failure.

Use retry for:
- transient network failure
- timeout
- 503
- 429 with delay
- dead connection

Do not retry:
- validation error
- 401/403
- non-idempotent payment charge without idempotency key
- permanent 404 in many cases

Strong answer:

```text
Retry is safe only when the operation is idempotent or protected by an idempotency key.
Otherwise retry can duplicate side effects.
```

---

# 9. Exponential Backoff

Bad retry:

```text
retry immediately 3 times
```

Why bad:
- creates retry storm
- increases downstream pressure
- worsens outage

Better:

```text
retry after 100ms, then 300ms, then 900ms, with jitter
```

Strong answer:

```text
I use bounded retries with exponential backoff and jitter. Retry should reduce transient
failure impact, not amplify an outage.
```

---

# 10. Circuit Breaker

A circuit breaker stops calling a failing dependency for a while.

States:

| State | Meaning |
|---|---|
| Closed | calls allowed |
| Open | calls blocked/fail fast |
| Half-open | limited trial calls |

Flow:

```text
dependency starts failing
failure rate crosses threshold
circuit opens
caller fails fast or uses fallback
after wait, half-open trial
success closes circuit
failure reopens circuit
```

Strong answer:

```text
A circuit breaker prevents cascading failure by failing fast when a downstream service is
unhealthy, instead of letting callers pile up blocked requests.
```

---

# 11. Bulkhead

Bulkhead isolates resources.

Example:

```text
payment-service calls get its own small thread pool
search-service calls get another pool
```

Why:

```text
If payment-service slows down, it should not consume all application threads and break
unrelated features.
```

Types:
- semaphore bulkhead
- thread pool bulkhead

Strong answer:

```text
Bulkheads prevent one dependency from exhausting shared resources. They isolate failures
by limiting concurrent calls per dependency or feature.
```

---

# 12. Rate Limiter

Rate limiter controls request rate.

Use cases:
- protect downstream API
- respect vendor quota
- prevent abusive clients
- smooth traffic

Example:

```text
payment provider allows 100 requests/sec
caller limits outbound payment calls to 90 requests/sec
```

Strong answer:

```text
Rate limiting protects either our service or a downstream dependency by controlling request
volume before overload happens.
```

---

# 13. Fallback

Fallback is alternate behavior when dependency fails.

Examples:
- return cached hotel details
- show stale room recommendations
- degrade optional loyalty points section
- enqueue work for later

Avoid fallback when:
- correctness is critical
- payment authorization fails
- inventory confirmation is required
- security decision cannot be verified

Strong answer:

```text
Fallback is good for optional or degradeable features. For correctness-critical operations,
it is better to fail clearly than return fake success.
```

---

# 14. Resilience Pattern Order

Common outbound call design:

```text
rate limit
bulkhead
timeout
retry with backoff
circuit breaker
fallback
metrics/logging/tracing
```

Interview caution:

```text
Retry and circuit breaker must be tuned together. Too many retries can open circuits faster
and overload dependencies.
```

---

# 15. Resilience4j Concepts

Resilience4j provides:
- CircuitBreaker
- Retry
- RateLimiter
- Bulkhead
- TimeLimiter

Annotation style:

```java
@CircuitBreaker(name = "hotelService", fallbackMethod = "fallbackHotel")
@Retry(name = "hotelService")
public HotelResponse getHotel(Long hotelId) {
    return hotelClient.getHotel(hotelId);
}

HotelResponse fallbackHotel(Long hotelId, Throwable ex) {
    return HotelResponse.unavailable(hotelId);
}
```

Interview line:

```text
The library is less important than knowing the pattern trade-offs and failure behavior.
```

---

# 16. WebFlux Big Picture

Spring MVC:

```text
one request often uses one servlet thread while waiting for I/O
```

Spring WebFlux:

```text
event-loop/non-blocking model handles many concurrent I/O waits with fewer threads
```

WebFlux uses:
- Reactor
- `Mono`
- `Flux`
- non-blocking HTTP runtime
- reactive streams backpressure concepts

Strong answer:

```text
WebFlux is useful for high-concurrency I/O-bound workloads, but it requires non-blocking
code end to end to get the benefit.
```

---

# 17. Mono and Flux

| Type | Meaning |
|---|---|
| `Mono<T>` | zero or one value |
| `Flux<T>` | zero to many values |

Example:

```java
Mono<Hotel> hotel = hotelClient.getHotel(10L);
Flux<Room> rooms = roomClient.streamRooms(10L);
```

Common operations:
- `map`
- `flatMap`
- `filter`
- `zip`
- `timeout`
- `onErrorResume`

---

# 18. Reactive Composition

Example:

```java
Mono<BookingPage> page =
        Mono.zip(hotelClient.getHotel(hotelId), rateClient.getRates(hotelId))
                .map(tuple -> new BookingPage(tuple.getT1(), tuple.getT2()));
```

Why useful:

```text
Independent I/O calls can be composed without blocking the request thread.
```

---

# 19. Blocking Trap In WebFlux

Bad:

```java
Mono<Booking> getBooking(Long id) {
    Booking booking = bookingRepository.findById(id).orElseThrow(); // blocking JPA
    return Mono.just(booking);
}
```

Why bad:
- blocks event-loop thread
- reduces WebFlux scalability
- can create latency spikes

Better options:
- use reactive database driver
- keep app Spring MVC if stack is blocking
- isolate blocking work on bounded elastic scheduler only when necessary

Strong answer:

```text
Using WebFlux with blocking JPA everywhere gives complexity without the main benefit.
Reactive works best when the whole I/O chain is non-blocking.
```

---

# 20. When To Use WebFlux

Use WebFlux when:
- many concurrent I/O-bound requests
- streaming responses
- reactive database/messaging stack
- non-blocking clients end to end
- gateway/proxy workloads

Avoid WebFlux when:
- team is not comfortable with Reactor
- app is mostly CRUD with JPA
- blocking libraries dominate
- debugging simplicity matters more

Interview line:

```text
I do not choose WebFlux just because it is modern. I choose it when non-blocking I/O gives
a real benefit and the team can operate it safely.
```

---

# 21. WebClient In MVC App

You can use `WebClient` in MVC apps.

Two styles:

Reactive return:

```java
Mono<RateResponse> getRate(Long hotelId) {
    return webClient.get()
            .uri("/rates/{id}", hotelId)
            .retrieve()
            .bodyToMono(RateResponse.class);
}
```

Blocking bridge:

```java
RateResponse response = webClient.get()
        .uri("/rates/{id}", hotelId)
        .retrieve()
        .bodyToMono(RateResponse.class)
        .block();
```

Caution:

```text
Blocking on WebClient in MVC can be acceptable, but then it behaves like a blocking call.
Still configure timeouts and resilience.
```

---

# 22. Production Scenario: Hotel Search Aggregator

Requirement:

```text
Search API calls hotel-service, rates-service, review-service, and recommendation-service.
Some data is optional, but rates are required.
```

Design:
1. Set per-client timeouts.
2. Use `RestClient` for blocking MVC or `WebClient` for non-blocking aggregator.
3. Retry only idempotent GET calls with backoff.
4. Use circuit breaker for each downstream.
5. Use fallback cache for optional review/recommendation data.
6. Do not fallback fake room rates if rate service fails.
7. Add bulkhead per downstream.
8. Record metrics: latency, error rate, retry count, circuit state.
9. Propagate correlation ID.

Strong interview answer:

```text
I would define separate clients for each downstream with timeouts and metrics. Idempotent
GET calls can have bounded retry with backoff, and each dependency gets a circuit breaker
and bulkhead. Optional data such as reviews can fall back to cache, but required rate or
inventory confirmation should fail clearly. I would monitor latency, timeout rate, retries,
and circuit state per downstream.
```

---

# 23. Hot Interview Questions

### Q1. RestTemplate vs RestClient vs WebClient?

```text
RestTemplate is older blocking client. RestClient is modern blocking/fluent client.
WebClient is reactive and non-blocking.
```

### Q2. Why are timeouts important?

```text
Without timeouts, slow dependencies can hold caller threads until the application becomes
unresponsive.
```

### Q3. When should you retry?

```text
Only for transient failures and only when the operation is idempotent or protected by an
idempotency key.
```

### Q4. What does a circuit breaker solve?

```text
It prevents cascading failure by failing fast when a downstream is unhealthy.
```

### Q5. What is a bulkhead?

```text
It isolates resources so one slow dependency cannot consume all threads or concurrency.
```

### Q6. What is WebFlux?

```text
Spring's reactive web stack based on non-blocking I/O and Reactor types like Mono and Flux.
```

### Q7. Why not use WebFlux with JPA?

```text
JPA is blocking. If most of the stack blocks, WebFlux adds complexity without much benefit.
```

---

# 24. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| No timeout | caller can hang | set connect/read/overall timeouts |
| Retry all failures | duplicates and storms | retry only safe transient failures |
| Retry POST payment without key | duplicate charge | idempotency key |
| One shared pool for all clients | one dependency can starve others | bulkhead |
| Fallback fake success | data corruption | fallback only safe/optional data |
| WebFlux with blocking JPA | event-loop blocking | MVC or reactive persistence |
| Swallowing downstream errors | hard debugging | structured error mapping |
| No per-client metrics | blind operations | latency/error/retry/circuit metrics |

---

# 25. One-Hour Revision Plan

### First 15 Minutes: Clients

Revise:
- `RestTemplate`
- `RestClient`
- `WebClient`
- error handling
- DTO boundaries

Must say:

```text
RestClient is my modern blocking default; WebClient is for reactive/non-blocking flows.
```

### Next 15 Minutes: Timeouts And Retry

Revise:
- timeout types
- retry safety
- backoff
- jitter
- idempotency

Must say:

```text
Retry without timeout and idempotency can make failures worse.
```

### Next 15 Minutes: Resilience

Revise:
- circuit breaker
- rate limiter
- bulkhead
- fallback
- metrics

Must say:

```text
Circuit breakers and bulkheads protect the caller from downstream failure.
```

### Final 15 Minutes: WebFlux

Revise:
- Mono
- Flux
- non-blocking I/O
- blocking trap
- when to use WebFlux

Must say:

```text
Reactive gives benefits only when the I/O chain is non-blocking and the team can manage
the complexity.
```

---

# 26. Final Rapid Revision Sheet

| Need | Concept |
|---|---|
| Legacy blocking client | `RestTemplate` |
| Modern blocking client | `RestClient` |
| Reactive HTTP client | `WebClient` |
| One async value | `Mono` |
| Stream of values | `Flux` |
| Bound network wait | timeout |
| Transient failure recovery | retry |
| Avoid retry storm | backoff + jitter |
| Stop calling failed service | circuit breaker |
| Isolate dependency resources | bulkhead |
| Control request volume | rate limiter |
| Degrade safely | fallback |
| Avoid duplicate side effect | idempotency key |

---

# 27. Strong Closing Answer

If interviewer asks:

```text
How do you design resilient service-to-service calls in Spring Boot?
```

Say:

```text
I choose RestClient for synchronous MVC services and WebClient for reactive or high-concurrency
I/O flows. Every outbound call gets timeouts, structured error handling, metrics, and tracing.
I retry only transient idempotent operations with bounded backoff and jitter. I use circuit
breakers to fail fast during downstream outages, bulkheads to isolate resources, rate limiters
to protect dependencies, and fallbacks only for safe optional data.
```

---

# 28. Official Source Notes

Useful official references:

- Spring REST Clients: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- Spring WebFlux: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Spring Boot Calling REST Services: https://docs.spring.io/spring-boot/reference/io/rest-client.html
- Spring Cloud Circuit Breaker: https://docs.spring.io/spring-cloud-circuitbreaker/reference/index.html
- Resilience4j: https://resilience4j.readme.io/docs/getting-started

