# Reactive URL Shortener (MAANG Standards)

A production-grade, reactive URL Shortener designed for massive scale ($\ge 10\text{B}$ redirects/day) and ultra-low latency ($P_{99} < 100\text{ ms}$). This application is implemented in **Java 21** using **Spring WebFlux (Netty)**, **R2DBC (Reactive PostgreSQL)**, **Reactive Redis**, and **Reactor Kafka**, strictly following **Hexagonal Architecture (Ports & Adapters)**.

---

## 1. Quick Start Guide

### Prerequisites
*   Java 21 installed.
*   Docker and Docker Compose installed and running.

### Step 1: Start the Infrastructure Stack
Run the following command to spin up PostgreSQL, Redis, Kafka (KRaft), Prometheus, and Grafana:
```bash
docker-compose up -d
```
Verify the containers are running:
```bash
docker ps
```

### Step 2: Build and Run the Application
Run the Spring Boot application locally:
```bash
# Windows
.\gradlew.bat bootRun --no-daemon

# macOS/Linux
./gradlew bootRun
```
The application will start on port `8080`.

### Step 3: Run the Test Suite
Execute the unit, integration, and structural boundaries test suite:
```bash
# Windows
.\gradlew.bat test --no-daemon

# macOS/Linux
./gradlew test
```

---

## 2. Swagger / OpenAPI Endpoint

OpenAPI/Swagger-UI auto-generates documentation for our functional routes and WebFlux endpoints.

*   **Swagger HTML UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **OpenAPI JSON Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

*Note: These paths are configured as public in [SecurityConfig.java](file:///c:/Users/aravi/Desktop/SystemDesign/july-2026/url-shortener/src/main/java/com/urlshortener/infrastructure/config/SecurityConfig.java) and do not require JWT authentication.*

---

## 3. API Reference (Sample Requests & Responses)

All URL Management APIs (`/v1/**`) require JWT Authentication via the `Authorization: Bearer <token>` header. 
For local testing, you can use any valid JWT signed with the HMAC-256 secret `change-me-in-production-must-be-32-chars-minimum` (default in `application.yml`).

### A. Create a Short URL (Generated Code)
*   **Method**: `POST`
*   **Path**: `/v1/urls`
*   **Headers**: 
    *   `Authorization: Bearer <JWT_TOKEN>`
    *   `Idempotency-Key: idempotency-key-001`
*   **Request Body**:
    ```json
    {
      "longUrl": "https://example.com/products/electronics/headphones?camp=summer",
      "expiresAt": "2027-12-31T23:59:59Z"
    }
    ```
*   **Response (`201 Created`)**:
    ```json
    {
      "code": "19gR8A9",
      "shortUrl": "http://localhost:8080/19gR8A9",
      "longUrl": "https://example.com/products/electronics/headphones?camp=summer",
      "expiresAt": "2027-12-31T23:59:59Z",
      "status": "ACTIVE",
      "createdAt": "2026-08-06T08:35:00Z"
    }
    ```

### B. Create a Short URL with Custom Alias
*   **Method**: `POST`
*   **Path**: `/v1/urls`
*   **Request Body**:
    ```json
    {
      "longUrl": "https://google.com",
      "customAlias": "custom-link"
    }
    ```
*   **Response (`201 Created`)**:
    ```json
    {
      "code": "custom-link",
      "shortUrl": "http://localhost:8080/custom-link",
      "longUrl": "https://google.com",
      "status": "ACTIVE",
      "createdAt": "2026-08-06T08:36:00Z"
    }
    ```

### C. Resolve Redirect (Data Plane - Public)
*   **Method**: `GET`
*   **Path**: `/{code}` (e.g. `/custom-link`)
*   **Headers**: None (Public)
*   **Response (`302 Found`)**:
    *   **Location Header**: `https://google.com`
    *   **Cache-Control**: `public, max-age=60`

### D. Get Metadata (Control Plane)
*   **Method**: `GET`
*   **Path**: `/v1/urls/{code}`
*   **Response (`200 OK`)**:
    ```json
    {
      "code": "custom-link",
      "shortUrl": "http://localhost:8080/custom-link",
      "longUrl": "https://google.com",
      "status": "ACTIVE",
      "createdAt": "2026-08-06T08:36:00Z"
    }
    ```

### E. Disable Short URL (Control Plane)
*   **Method**: `DELETE`
*   **Path**: `/v1/urls/{code}`
*   **Response (`200 OK`)**:
    ```json
    {
      "code": "custom-link",
      "status": "DISABLED"
    }
    ```

---

## 4. Technical Execution & EventLoop Internals (Reactive Flow)

At MAANG scale, blocking threads is unacceptable. Traditional Servlet containers (like Tomcat) allocate one thread per request. If PostgreSQL or Redis is slow, the thread blocks, thread pools exhaust, and the server crashes.

```
Traditional Thread-per-Request:
Client Request ──► Thread (Blocked on DB) ──► DB Query ──► Thread Resumes ──► Response

Reactive Non-blocking (Netty EventLoop):
Client Request ──► EventLoop Selector ──► Register Callback ──► EventLoop free to serve other requests
                     ▲                                             │
                     └────────── DB Response (Trigger Callback) ───┘
```

### How the Netty Event Loop Works in our URL Shortener:
1.  **Request Arrival**: A request hits Netty's worker EventLoop. Netty reads bytes from the socket without blocking.
2.  **Route Dispatch**: The routing engine invokes [RedirectHandler](file:///c:/Users/aravi/Desktop/SystemDesign/july-2026/url-shortener/src/main/java/com/urlshortener/api/RedirectHandler.java). The Handler delegates to `RedirectUseCase.resolve()`.
3.  **Non-blocking Cache Check**:
    *   `Caffeine` (L1) check runs instantly in memory.
    *   `Redis` (L2) check executes via the non-blocking Lettuce driver. Rather than blocking, Lettuce registers a callback on the channel and returns immediately. The EventLoop thread is freed to handle other requests.
4.  **R2DBC DB Fallback**: On L2 cache miss, R2DBC sends a SQL command to PostgreSQL asynchronously. R2DBC yields the thread back to the runtime.
5.  **Reactivation**: When PostgreSQL returns data, the OS notifies the socket. Netty's EventLoop picks up the event, resumes the JVM stream, validates `isActiveAt()`, and completes the response.
6.  **Async Ingestion**: The EventLoop immediately fires the HTTP response `302 Found` back to the user. *After* dispatching the response, it publishes the analytics click event to Kafka asynchronously without waiting for the Kafka broker acknowledgement to block the request.

### Reactor Threading Model & Schedulers
We enforce explicit thread separation:
*   **EventLoop Thread pool**: Reserved for low-latency network I/O.
*   **`Schedulers.boundedElastic()`**: Used specifically in `RedirectService.publishClickAsync()` when publishing to Kafka:
    ```java
    analyticsPublisher.publish(event)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();
    ```
    This prevents any transient Kafka driver blockages or heavy JSON serialization CPU cycles from starving the primary Netty network threads.

---

## 5. Project Reactor Operators & Concepts

This system utilizes advanced reactive extensions. Understanding these is critical for the machine coding round:

### Eager vs Lazy Evaluation (`Mono.defer` vs `Mono.just`)
In Reactor, `.then(Publisher)` evaluates its argument eagerly during *chain assembly* time, not during *subscription execution* time. We debugged and resolved a NullPointerException here:
*   **Incorrect Eager Pattern**:
    ```java
    return validateRequest(command)
        .then(abuseChecker.check(url)); // Triggers check() even if validation failed!
    ```
*   **Correct Lazy Pattern**:
    ```java
    return validateRequest(command)
        .then(Mono.defer(() -> abuseChecker.check(url))); // Only evaluated if validation succeeds!
    ```

### Core Reactive Operators Used:
1.  **`flatMap` vs `map`**:
    *   `map` transforms synchronous data: `Mono<UrlMappingEntity> ➔ Mono<UrlMapping>`.
    *   `flatMap` flattens an asynchronous publisher: `Mono<String> ➔ flatMap(code -> repository.findByCode(code))` (returns `Mono<UrlMapping>`).
2.  **`switchIfEmpty`**:
    *   Defines fallback publishers. In `RedirectService`, we check: L1 Cache ➔ `switchIfEmpty` ➔ L2 Redis ➔ `switchIfEmpty` ➔ L3 DB.
3.  **`doOnSuccess` / `doOnError`**:
    *   Side-effect operators. We use them for logging, increasing Micrometer counters, and starting the async event publication without modifying the pipeline data.
4.  **`StepVerifier`**:
    *   Testing utility that allows us to assert reactive streams sequentially:
    ```java
    StepVerifier.create(service.resolve("abc1234", ctx))
        .expectNext("https://dest.com/page")
        .verifyComplete();
    ```

---

## 6. System Design (SD) Mapping to Request Flows

Here is how E2E system design concepts map to our code:

```
                  ┌───────────────────────────────┐
                  │          GET /{code}          │
                  └───────────────┬───────────────┘
                                  │
                                  ▼
                   Local Caffeine L1 Cache hit?
                     ├──► [YES] ──► Validate & Redirect
                     │
                     └──► [NO]
                            │
                            ▼
                     Redis L2 Cache hit?
                       ├──► [YES] ──► Write to L1 ──► Validate & Redirect
                       │
                       └──► [NO]
                              │
                              ▼
                       Postgres L3 DB hit?
                         ├──► [YES] ──► Write L2 & L1 ──► Validate & Redirect
                         │
                         └──► [NO] (Random Scan)
                                │
                                ▼
                         Negative Caching (Sentinel)
                         Write '__NOT_FOUND__' to Redis (30s)
                         ➔ Return 404
```

### 1. Multi-Layer Cache Hierarchy
We implement L1 (in-memory Caffeine) and L2 (distributed Redis) caches. 
*   **Write-Through / Write-Around**: When a link is resolved from the DB, we write it to Redis and Caffeine to speed up subsequent reads.
*   **Cache Eviction**: When a link is disabled, we execute an explicit cache purge `cache.evict(code)` to prevent stale redirects.

### 2. Negative Caching (Spike/DDoS Defense)
To prevent a malicious bot from running random-code scans (e.g. hitting `http://localhost:8080/doesNotExist`), we write a negative sentinel `__NOT_FOUND__` to Redis with a short TTL (30s). If a bot queries the same code again, the application rejects the request straight from Redis without touching PostgreSQL.

### 3. Atomic Alias Reservation & Concurrency Control
*   **Create path race condition**: Two threads try to register the custom alias `"custom-link"`.
*   **Solution**: We do not do a `select-before-insert` (which is vulnerable to race conditions). Instead, we use a single atomic query using `ON CONFLICT (code) DO NOTHING` in R2DBC:
    ```sql
    INSERT INTO url_mapping (...) VALUES (...) ON CONFLICT (code) DO NOTHING
    ```
    Exactly one concurrent request gets `rowsUpdated() = 1` (returns `true`), while the other gets `0` (returns `false` ➔ throws `AliasConflictException`).

### 4. Idempotency Keys (Exactly-Once Semantics)
*   **Problem**: Retried network requests create duplicate shortened links.
*   **Solution**: The client provides an `Idempotency-Key` header. We check `RedisIdempotencyStore` for the tuple `(userId, idempotencyKey)`. If found, we return the previously resolved code without performing a DB insert.

### 5. Snowflake ID Generator
*   **Problem**: Auto-incrementing database primary keys do not scale across multiple database partitions and reveal our business volume.
*   **Solution**: We implemented [SnowflakeBase62CodeGenerator](file:///c:/Users/aravi/Desktop/SystemDesign/july-2026/url-shortener/src/main/java/com/urlshortener/adapter/codegen/SnowflakeBase62CodeGenerator.java). It generates a thread-safe 64-bit ID based on:
    *   **41 bits**: Timestamp (ms since custom epoch)
    *   **10 bits**: Node/Machine ID (up to 1024 servers)
    *   **12 bits**: Monotonic sequence counter (allows 4096 creations/ms/node)
*   This ID is encoded into Base62, yielding a compact, URL-safe 7-character code.

---

## 7. OOP and Design Patterns Implemented

For your machine coding round, be ready to defend the design patterns we used:

### A. Hexagonal Architecture (Ports & Adapters)
Our package structure isolates business rules:
*   **Pure Domain**: No Spring, No Redis, No JPA. Classes like `UrlMapping` are immutable records holding state invariants.
*   **Ports**: Interfaces that specify the dependency contracts.
*   **Adapters**: Concrete implementations (R2DBC, Lettuce, Reactor Kafka) that depend on ports, not the other way around. 
*   **Why?**: Highly testable, easily swappable infrastructure, and zero structural leakage.

### B. Strategy Pattern
We defined the `CodeGenerator` interface:
```java
public interface CodeGenerator { String nextCode(); }
```
Our service layer does not care *how* codes are generated. We can switch from `SnowflakeBase62CodeGenerator` to a `RandomCodeGenerator` or `PreGeneratedPoolCodeGenerator` by simply changing the Spring bean declaration, without altering the use-case service code.

### C. Proxy Pattern (Cache Layering)
The `CachePort` acts as a proxy wrapping data retrieval. If the cache misses, it delegates downstream.

### D. Observer Pattern / Event Publisher
The `AnalyticsPublisher` publishes click events to Kafka. The redirect service does not wait for a response or know who is consuming the events (e.g. data warehouse, fraud detection, real-time analytics). It simply emits the event and completes the response.

### E. Builder Pattern
Used to map between domain aggregates and database entities within the `UrlMappingMapper`.
