# Platinum 3 - HTTP Interfaces, RSocket, HTTP/2, WebTestClient

> Goal: go beyond basic WebClient and learn the protocol/client/testing surface that senior
> WebFlux interviews can touch: declarative HTTP interfaces, RSocket, HTTP/2, WebTestClient,
> and production client design.

---

## 0. Mental Model First

WebFlux applications usually play multiple roles:

```text
server API + HTTP client + streaming endpoint + test target + sometimes message protocol peer
```

The first six batches cover the core reactive model. This sheet covers adjacent tools that
make WebFlux production-grade:

- `WebClient`
- HTTP interface clients
- RSocket
- HTTP/2
- `WebTestClient`
- test slices and integration testing

---

## 1. WebClient Recap At Senior Level

WebClient is the reactive HTTP client in Spring.

Use it for:

- non-blocking service-to-service calls
- gateway/filter clients
- parallel fan-out
- streaming responses
- API composition

### Safe Client Shape

```java
@Bean
WebClient bookingWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl("https://booking-api.example.com")
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
}
```

### Strong Answer

```text
WebClient is not enough by itself. I still configure timeouts, connection pool, status
handling, body size expectations, retries only for idempotent calls, and observability.
```

---

## 2. `retrieve()` vs `exchangeToMono()`

### `retrieve()`

Good for common request/response cases:

```java
Mono<HotelView> hotel = webClient.get()
    .uri("/hotels/{id}", id)
    .retrieve()
    .bodyToMono(HotelView.class);
```

### `exchangeToMono()`

Useful when response handling depends on status, headers, or body shape:

```java
Mono<HotelView> hotel = webClient.get()
    .uri("/hotels/{id}", id)
    .exchangeToMono(response -> {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(HotelView.class);
        }
        if (response.statusCode().value() == 404) {
            return Mono.empty();
        }
        return response.createException().flatMap(Mono::error);
    });
```

### Interview Trap

Using low-level exchange APIs and not consuming/releasing the response body can cause leaks.
Prefer `retrieve()` unless you need lower-level control.

---

## 3. Declarative HTTP Interface Clients

Spring supports HTTP service interfaces: define Java interfaces annotated with HTTP mapping
metadata and create a proxy backed by WebClient.

### Interface

```java
interface InventoryHttpClient {

    @GetExchange("/inventory/{hotelId}/{roomType}")
    Mono<InventoryView> getInventory(
        @PathVariable String hotelId,
        @PathVariable String roomType
    );

    @PostExchange("/inventory/holds")
    Mono<InventoryHold> hold(@RequestBody HoldRequest request);
}
```

### Proxy Setup

```java
@Bean
InventoryHttpClient inventoryHttpClient(WebClient.Builder builder) {
    WebClient webClient = builder
        .baseUrl("https://inventory.example.com")
        .build();

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(WebClientAdapter.create(webClient))
        .build();

    return factory.createClient(InventoryHttpClient.class);
}
```

### When To Use

- many typed outbound APIs
- team wants Feign-like style but reactive
- contract is stable
- easier testing and mocking

### When Not To Use

- dynamic request logic is heavy
- client needs very custom exchange handling
- team does not understand generated proxy behavior

---

## 4. WebClient Filters

Filters are client-side interceptors.

Use for:

- correlation ID
- auth header propagation
- logging
- metrics
- tenant header
- common error mapping

Example:

```java
ExchangeFilterFunction correlationFilter = (request, next) ->
    Mono.deferContextual(ctx -> {
        String traceId = ctx.getOrDefault("traceId", "missing");
        ClientRequest newRequest = ClientRequest.from(request)
            .header("X-Correlation-Id", traceId)
            .build();
        return next.exchange(newRequest);
    });
```

### Trap

Do not log full sensitive request/response bodies in filters.

---

## 5. Timeouts In Reactive HTTP

Timeouts can exist at multiple layers:

| Timeout | Meaning |
|---|---|
| connect timeout | time to establish connection |
| response timeout | time waiting for response |
| read/write timeout | socket read/write duration |
| operator timeout | Reactor-level max time for publisher |

Example operator timeout:

```java
return inventoryClient.hold(request)
    .timeout(Duration.ofMillis(800));
```

Senior answer:

```text
I set network timeouts close to the HTTP client and use operator-level timeouts for business
latency budgets. I also make retry behavior respect the same budget.
```

---

## 6. HTTP/2 With WebFlux

HTTP/2 can improve connection efficiency through multiplexing and header compression.

Useful when:

- many concurrent requests to same origin
- TLS is already standard
- gateway/service mesh supports it
- clients and servers are configured correctly

But:

- it does not make blocking code non-blocking
- it does not remove need for timeouts/backpressure
- debugging can be more complex
- proxies/load balancers must be compatible

### Interview Answer

```text
HTTP/2 can improve transport efficiency, especially with many concurrent streams over fewer
connections, but it does not replace WebFlux's non-blocking runtime or resilience patterns.
```

---

## 7. RSocket Mental Model

RSocket is a reactive application protocol supporting multiple interaction models.

Interaction models:

| Model | Shape |
|---|---|
| request-response | one request, one response |
| request-stream | one request, many responses |
| fire-and-forget | send without response |
| channel | bidirectional streams |

### Where It Fits

- low-latency service-to-service messaging
- bidirectional communication
- streaming data
- backpressure-aware interactions
- internal platform services

### Where HTTP Is Better

- public APIs
- browser compatibility
- simple CRUD
- standard REST tooling
- easier debugging

Strong answer:

```text
RSocket is powerful when both sides are reactive and streaming/bidirectional communication
matters. I would not use it just because the service uses WebFlux.
```

---

## 8. RSocket Code Shape

### Controller

```java
@Controller
class BookingRSocketController {

    @MessageMapping("booking.status")
    Flux<BookingStatus> status(Mono<BookingStatusRequest> requestMono) {
        return requestMono.flatMapMany(request ->
            bookingStatusService.stream(request.bookingId())
        );
    }
}
```

### Client

```java
Flux<BookingStatus> statuses = requester.route("booking.status")
    .data(new BookingStatusRequest("b123"))
    .retrieveFlux(BookingStatus.class);
```

### Interview Trap

Using RSocket for public simple CRUD makes the system harder to operate unless the protocol
benefits are real.

---

## 9. WebTestClient

WebTestClient is a reactive test client for WebFlux apps.

It can test:

- controller endpoints
- router functions
- full server over HTTP
- response status/headers/body
- streaming responses

### Controller Test

```java
@WebFluxTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void returnsBooking() {
        webTestClient.get()
            .uri("/api/bookings/b123")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.bookingId").isEqualTo("b123");
    }
}
```

### Full Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrationTest {

    @Autowired
    WebTestClient webTestClient;
}
```

---

## 10. Testing Streaming Responses

Use `returnResult` and StepVerifier.

```java
Flux<BookingStatus> body = webTestClient.get()
    .uri("/api/bookings/b123/status-stream")
    .exchange()
    .expectStatus().isOk()
    .returnResult(BookingStatus.class)
    .getResponseBody();

StepVerifier.create(body)
    .expectNextMatches(status -> status.bookingId().equals("b123"))
    .thenCancel()
    .verify();
```

### Why `thenCancel()` Matters

Many streams do not complete naturally. Tests must cancel intentionally.

---

## 11. Testing WebClient Code

Options:

| Approach | Use |
|---|---|
| mock service interface | unit tests for service logic |
| mock `ExchangeFunction` | focused WebClient behavior |
| mock HTTP server | real HTTP contract behavior |
| Testcontainers/wiremock-style setup | integration confidence |

Senior rule:

```text
Do not over-mock reactive flow. At least one integration test should prove real
serialization, status handling, timeout behavior, and error mapping.
```

---

## 12. Testing Security

Test:

- unauthenticated request -> 401
- authenticated but forbidden -> 403
- authorized -> 200/expected
- ownership violation -> 403 or 404 depending policy
- invalid token -> 401

Example shape:

```java
webTestClient.get()
    .uri("/api/bookings/b123")
    .exchange()
    .expectStatus().isUnauthorized();
```

In real projects, use Spring Security test support for mock users/JWTs.

---

## 13. Testing Error Responses

```java
webTestClient.get()
    .uri("/api/bookings/missing")
    .exchange()
    .expectStatus().isNotFound()
    .expectBody()
    .jsonPath("$.code").isEqualTo("BOOKING_NOT_FOUND")
    .jsonPath("$.traceId").exists();
```

Why:

```text
Error response shape is part of the API contract.
```

---

## 14. Protocol Decision Table

| Need | Pick |
|---|---|
| simple public HTTP API | REST/WebFlux |
| non-blocking service-to-service calls | WebClient or HTTP interface |
| declarative typed HTTP client | HTTP service interface |
| one-way server push to browser | SSE |
| full-duplex browser communication | WebSocket |
| internal bidirectional reactive streams | RSocket |
| async side effects and durability | Kafka/RabbitMQ |
| many streams on same connection | HTTP/2 or RSocket depending model |

---

## 15. End-To-End Client Story

Use case:

```text
Booking service calls inventory, pricing, and payment.
```

Strong design:

1. Use HTTP interface clients for stable downstream APIs.
2. Configure WebClient base URLs, filters, timeouts, and metrics.
3. Use `Mono.zip` for independent calls.
4. Use `exchangeToMono` where status-specific logic matters.
5. Use retries only for idempotent calls.
6. Test happy path, 4xx, 5xx, timeout, and malformed response.
7. Use WebTestClient for incoming API contract.

---

## 16. Interview Hot Questions

### 1. Why use HTTP interface clients?

They give a typed declarative client over WebClient, useful for stable APIs and cleaner
service code.

### 2. When do you prefer `exchangeToMono` over `retrieve`?

When response handling depends on status, headers, or different body shapes.

### 3. When would you use RSocket?

When both sides benefit from reactive, backpressure-aware streaming or bidirectional
communication.

### 4. Is HTTP/2 a replacement for WebFlux?

No. HTTP/2 is transport efficiency. WebFlux is application runtime and programming model.

### 5. Why WebTestClient?

It tests WebFlux endpoints reactively and can verify status, headers, body, and streams.

### 6. How do you test infinite streams?

Use `StepVerifier`, assert expected items, then cancel intentionally.

---

## 17. Final Revision Notes

```text
WebClient = reactive HTTP client.
retrieve = common body extraction.
exchangeToMono = status/header/body-specific control.
HTTP interface = declarative typed WebClient proxy.
Filters propagate auth, trace, tenant, metrics.
HTTP/2 improves transport efficiency, not application correctness.
RSocket fits bidirectional/backpressure-aware internal streaming.
WebTestClient verifies reactive endpoints.
Infinite streams need explicit test cancellation.
Error responses and security behavior are API contracts.
```

---

## 18. Official Source Notes

- Spring WebClient reference: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
- Spring HTTP Interface Clients: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- Spring RSocket reference: https://docs.spring.io/spring-framework/reference/rsocket.html
- Spring WebTestClient reference: https://docs.spring.io/spring-framework/reference/testing/webtestclient.html
- Spring Boot reactive web applications: https://docs.spring.io/spring-boot/reference/web/reactive.html
