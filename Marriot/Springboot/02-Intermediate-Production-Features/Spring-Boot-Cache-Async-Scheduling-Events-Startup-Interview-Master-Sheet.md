# Spring Boot Cache Async Scheduling Events Startup Interview Master Sheet

Target: Java Backend / Spring Boot / production interviews.

This sheet groups Spring Boot runtime features that often appear after core Spring:
- caching
- async execution
- scheduling
- application events
- startup lifecycle
- runner hooks
- production traps around concurrency and duplicate execution

Goal:

```text
After reading this sheet, you should be able to explain how Spring Boot handles cached
data, background execution, scheduled jobs, application events, and startup hooks in a
production-safe way.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | `@Cacheable`, `@Async`, `@Scheduled`, `ApplicationRunner` |
| Intermediate | cache keys, eviction, thread pools, cron, event listeners |
| Senior | cache invalidation, stampede, context propagation, distributed scheduling |
| MAANG-ready | consistency trade-offs, multi-node behavior, observability, failure handling |

Strong line:

```text
These features are easy to add with annotations, but production correctness depends on
keys, thread pools, invalidation, idempotency, and multi-instance behavior.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Spring cache abstraction | Very high | Common performance tool |
| `@Cacheable` | Very high | Everyday annotation |
| Cache eviction | Very high | Invalidation correctness |
| Cache key design | Very high | Avoid wrong results |
| Redis cache | High | Distributed cache usage |
| Cache stampede | High | Senior production issue |
| `@Async` | High | Background execution |
| ThreadPoolTaskExecutor | High | Avoid unbounded execution |
| `@Scheduled` | High | Periodic jobs |
| Cron vs fixedRate vs fixedDelay | High | Scheduling semantics |
| Distributed scheduling | Very high | Multi-node duplicate jobs |
| Application events | Medium-high | Decoupling |
| `@TransactionalEventListener` | High | Transaction-aware events |
| Startup lifecycle | Medium-high | Boot internals |
| Runners | Medium-high | Startup tasks |

---

# 2. Spring Cache Big Picture

## What Caching Solves

Caching stores frequently used results so the system avoids repeated expensive work.

Examples:
- hotel details by ID
- room rate rules
- feature flags
- user permissions
- static reference data

Strong answer:

```text
Spring Cache provides an annotation-based abstraction over cache providers. It does not
solve invalidation automatically; we still need correct keys, TTLs, eviction, and consistency
strategy.
```

---

# 3. Cache Abstraction

Spring cache abstraction gives common annotations:

| Annotation | Purpose |
|---|---|
| `@EnableCaching` | Enables cache support |
| `@Cacheable` | Read-through cache |
| `@CachePut` | Update cache without skipping method |
| `@CacheEvict` | Remove cache entry |
| `@Caching` | Combine multiple cache operations |

Provider options:
- simple in-memory cache
- Caffeine
- Redis
- Hazelcast
- Ehcache

Interview line:

```text
Spring Cache is an abstraction. The actual storage depends on the configured CacheManager.
```

---

# 4. `@Cacheable`

Example:

```java
@Service
class HotelService {
    @Cacheable(cacheNames = "hotels", key = "#hotelId")
    public HotelDetails getHotel(Long hotelId) {
        return hotelClient.fetchHotel(hotelId);
    }
}
```

Flow:

```text
1. Method is called through Spring proxy.
2. Spring computes cache key.
3. If key exists, return cached value.
4. If key misses, execute method.
5. Store result in cache.
6. Return result.
```

Strong answer:

```text
@Cacheable is read-through caching. The method is skipped on cache hit and executed on
cache miss.
```

---

# 5. Cache Key Design

Bad key:

```java
@Cacheable(cacheNames = "rooms")
public List<Room> searchRooms(String city, LocalDate checkIn, LocalDate checkOut) {
}
```

Why risky:
- default key may be okay for simple cases, but can become unclear
- missing user/currency/language/ratePlan can return wrong data

Better:

```java
@Cacheable(
    cacheNames = "roomSearch",
    key = "#city + ':' + #checkIn + ':' + #checkOut + ':' + #currency"
)
public List<Room> searchRooms(String city,
                              LocalDate checkIn,
                              LocalDate checkOut,
                              String currency) {
}
```

Rule:

```text
Cache key must include every input that affects the result.
```

---

# 6. Conditional Caching

Use `condition` to decide before method execution.

```java
@Cacheable(cacheNames = "hotels", key = "#id", condition = "#id != null")
public HotelDetails getHotel(Long id) {
}
```

Use `unless` to decide after method execution.

```java
@Cacheable(cacheNames = "hotels", key = "#id", unless = "#result == null")
public HotelDetails getHotel(Long id) {
}
```

Interview line:

```text
condition is evaluated before the call. unless is evaluated after the result is available.
```

---

# 7. Cache Eviction

Eviction removes stale data.

Example:

```java
@CacheEvict(cacheNames = "hotels", key = "#hotelId")
public void updateHotel(Long hotelId, UpdateHotelRequest request) {
    hotelRepository.update(hotelId, request);
}
```

Evict all:

```java
@CacheEvict(cacheNames = "hotelSearch", allEntries = true)
public void refreshRates() {
}
```

Strong answer:

```text
The hard part of caching is invalidation. Whenever the source of truth changes, we must
evict or update affected cache entries, or choose short TTLs when exact invalidation is hard.
```

---

# 8. `@CachePut`

`@CachePut` always runs the method and updates cache.

Example:

```java
@CachePut(cacheNames = "hotels", key = "#hotel.id")
public Hotel updateHotel(Hotel hotel) {
    return hotelRepository.save(hotel);
}
```

Difference:

| `@Cacheable` | `@CachePut` |
|---|---|
| skips method on hit | always executes method |
| read optimization | write/update cache |

---

# 9. Cache Provider Choice

| Provider | Best For |
|---|---|
| In-memory simple | local dev, tiny app |
| Caffeine | fast local cache |
| Redis | distributed cache |
| Hazelcast | clustered in-memory data |

Rule:

```text
In multi-instance production, local cache means each instance has its own copy.
```

Use Redis when:
- multiple app instances need shared cache
- cache must survive app restart
- central TTL and eviction are needed

Use local cache when:
- data is small
- minor staleness per node is acceptable
- ultra-low latency matters

---

# 10. Cache Stampede

Cache stampede happens when many requests miss the same key at once.

Example:

```text
popular hotel key expires
10,000 requests miss cache
all hit database/API together
```

Controls:
- stagger TTLs with jitter
- cache lock/single-flight
- warm important keys
- serve stale while refresh
- rate limit expensive recompute
- avoid same expiration for all keys

Strong answer:

```text
Cache stampede is a production failure mode where cache expiry causes a traffic burst
against the source of truth. I handle it with TTL jitter, request coalescing, warmups,
or stale-while-refresh patterns.
```

---

# 11. Cache Common Traps

| Trap | Why It Is Wrong | Better Approach |
|---|---|---|
| Missing key fields | wrong data returned | include all result inputs |
| Caching user-specific data globally | data leak | include user/tenant in key |
| No eviction strategy | stale data | TTL and explicit eviction |
| Infinite TTL for mutable data | correctness issue | bounded TTL |
| Caching null blindly | hides new data | use `unless` carefully |
| Local cache in multi-node app | inconsistent nodes | Redis or short TTL |
| Caching transactional method incorrectly | cache updates before rollback | evict after successful update |

---

# 12. `@Async`

`@Async` runs a method on another thread.

Enable:

```java
@EnableAsync
@Configuration
class AsyncConfig {
}
```

Example:

```java
@Async
public CompletableFuture<Void> sendBookingEmail(Long bookingId) {
    emailService.sendConfirmation(bookingId);
    return CompletableFuture.completedFuture(null);
}
```

Strong answer:

```text
@Async moves work to a TaskExecutor thread. It is proxy-based, so self-invocation does
not work, and production code should configure a bounded thread pool.
```

---

# 13. Async Thread Pool

Bad:

```text
Use default executor without knowing limits.
```

Better:

```java
@Bean
ThreadPoolTaskExecutor applicationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("app-async-");
    executor.initialize();
    return executor;
}
```

Production concerns:
- pool size
- queue capacity
- rejection policy
- timeout
- metrics
- graceful shutdown

Interview line:

```text
Async without a bounded executor can become a hidden overload amplifier.
```

---

# 14. Async Return Types

Common return types:
- `void`
- `Future<T>`
- `CompletableFuture<T>`

Prefer `CompletableFuture` when caller needs result composition.

Example:

```java
@Async
public CompletableFuture<RateQuote> fetchRateQuote(Long hotelId) {
    return CompletableFuture.completedFuture(rateClient.getQuote(hotelId));
}
```

Exception handling:
- exceptions in `void` async methods need `AsyncUncaughtExceptionHandler`
- exceptions in `CompletableFuture` are captured in the future

---

# 15. Async Context Propagation

ThreadLocal context may not automatically move to async threads.

Examples:
- security context
- MDC correlation ID
- tenant context
- request context

Problem:

```text
Controller thread has traceId, async thread logs without traceId.
```

Solutions:
- task decorator
- explicit context passing
- Micrometer context propagation
- avoid relying blindly on ThreadLocal

Strong answer:

```text
Because @Async switches threads, I think about context propagation for MDC, tracing,
tenant, and security context.
```

---

# 16. `@Scheduled`

`@Scheduled` runs method periodically.

Enable:

```java
@EnableScheduling
@Configuration
class SchedulingConfig {
}
```

Examples:

```java
@Scheduled(fixedRate = 60_000)
void refreshCache() {
}

@Scheduled(fixedDelay = 60_000)
void cleanup() {
}

@Scheduled(cron = "0 0 1 * * *")
void nightlyJob() {
}
```

---

# 17. Fixed Rate vs Fixed Delay vs Cron

| Mode | Meaning |
|---|---|
| fixedRate | start every N ms measured from previous start |
| fixedDelay | wait N ms after previous completion |
| cron | run by calendar expression |

Example:

```text
fixedRate=1 minute, task takes 90 seconds -> next run may be late/overlap depending executor
fixedDelay=1 minute, task takes 90 seconds -> next run starts 1 minute after finish
```

Interview line:

```text
fixedDelay is safer when I do not want overlapping runs.
```

---

# 18. Distributed Scheduling Problem

In a multi-instance deployment:

```text
pod-1 runs scheduled method
pod-2 runs scheduled method
pod-3 runs scheduled method
```

This can create duplicate work.

Solutions:
- external scheduler, such as Kubernetes CronJob
- single scheduler deployment
- distributed lock
- database lock row
- ShedLock-like pattern
- idempotent job logic

Strong answer:

```text
@Scheduled is local to each application instance. In a cluster, I either use an external
scheduler, a single scheduler instance, or a distributed lock, and I still make the job
idempotent.
```

---

# 19. Application Events

Spring events decouple publisher from listeners.

Example:

```java
record BookingCreatedEvent(Long bookingId) {
}

@Service
class BookingService {
    private final ApplicationEventPublisher publisher;

    BookingService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void createBooking() {
        Long bookingId = 100L;
        publisher.publishEvent(new BookingCreatedEvent(bookingId));
    }
}
```

Listener:

```java
@Component
class BookingEventListener {
    @EventListener
    void onBookingCreated(BookingCreatedEvent event) {
        // send email, update read model, audit, etc.
    }
}
```

Strong answer:

```text
Spring events help decouple internal application components. By default they are in-process
events, not durable messaging.
```

---

# 20. `@TransactionalEventListener`

Sometimes event should run only after transaction commits.

Example:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void sendConfirmation(BookingCreatedEvent event) {
    emailService.sendConfirmation(event.bookingId());
}
```

Why it matters:

```text
Do not send confirmation email if booking transaction rolls back.
```

Strong answer:

```text
For side effects that depend on committed database state, I prefer AFTER_COMMIT transactional
events or an outbox pattern.
```

---

# 21. Events vs Message Broker

| Spring Event | Kafka/RabbitMQ |
|---|---|
| in-process | cross-service |
| not durable by default | durable depending broker/config |
| same application | distributed systems |
| simple decoupling | integration/event-driven architecture |

Interview line:

```text
Spring events are not a replacement for Kafka when messages must survive process failure
or cross service boundaries.
```

---

# 22. Spring Boot Startup Lifecycle

High-level startup:

```text
main()
  -> SpringApplication.run()
  -> create Environment
  -> create ApplicationContext
  -> load bean definitions
  -> instantiate beans
  -> run post-processors
  -> start embedded server
  -> publish ready events
  -> run runners
```

Common hooks:
- `ApplicationRunner`
- `CommandLineRunner`
- `ApplicationListener`
- `SmartLifecycle`
- `@PostConstruct`

---

# 23. `CommandLineRunner` vs `ApplicationRunner`

Example:

```java
@Component
class WarmupRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        // warm caches, validate config, etc.
    }
}
```

Difference:

| CommandLineRunner | ApplicationRunner |
|---|---|
| raw `String[] args` | parsed `ApplicationArguments` |
| simple | better option parsing |

Use runners for:
- cache warmup
- startup validation
- local dev seed data
- one-time boot tasks

Avoid runners for:
- long blocking jobs
- unsafe DB migrations
- work that should run on only one node unless controlled

---

# 24. Startup Events

Important events:

| Event | Meaning |
|---|---|
| `ApplicationStartingEvent` | startup begins |
| `ApplicationEnvironmentPreparedEvent` | environment ready |
| `ApplicationPreparedEvent` | context prepared |
| `ApplicationStartedEvent` | context started |
| `ApplicationReadyEvent` | app ready to serve traffic |
| `ApplicationFailedEvent` | startup failed |

Common use:

```java
@EventListener(ApplicationReadyEvent.class)
void onReady() {
    // app is ready
}
```

Trap:

```text
ApplicationReadyEvent does not mean dependencies are healthy forever. It only means startup
completed.
```

---

# 25. `SmartLifecycle`

`SmartLifecycle` gives controlled start/stop behavior.

Use cases:
- start background consumers after app context is ready
- stop workers gracefully
- order lifecycle phases

Interview line:

```text
SmartLifecycle is useful when a bean needs coordinated startup and shutdown, especially
for background workers.
```

---

# 26. Production Scenario: Booking Cache Refresh

Requirement:

```text
Hotel rate rules are expensive to load but change a few times per day. APIs need fast reads.
```

Design:
1. Cache rate rules by hotel ID and date.
2. Use Redis for shared cache across nodes.
3. Use TTL with jitter.
4. Evict cache after rate update.
5. Use scheduled warmup for top hotels.
6. Protect warmup with distributed lock.
7. Expose cache hit/miss metrics.
8. Keep source of truth in database.

Strong answer:

```text
I would use Redis-backed Spring Cache with keys that include hotel, date, currency, and
rate plan. Updates evict affected keys, TTL limits stale data, and scheduled warmups handle
popular hotels. In multiple instances, scheduled warmup needs a distributed lock or external
scheduler.
```

---

# 27. Hot Interview Questions

### Q1. What does `@Cacheable` do?

```text
It checks cache before method execution. On hit, method is skipped. On miss, method runs
and result is stored.
```

### Q2. What is the hardest part of caching?

```text
Invalidation and key correctness. Wrong keys or stale entries can produce incorrect business
results.
```

### Q3. Why can `@Async` fail on self-invocation?

```text
Because it is proxy-based. A method call inside the same class does not go through the
Spring proxy.
```

### Q4. Why configure async executor?

```text
To control threads, queue size, rejection, metrics, and shutdown behavior. Defaults may not
match production load.
```

### Q5. What is the problem with `@Scheduled` in Kubernetes?

```text
Every pod can run the scheduled method. Use external scheduler, leader election, distributed
lock, or idempotent logic.
```

### Q6. When use `@TransactionalEventListener`?

```text
When listener side effects should happen only after transaction commit, such as sending
confirmation email after booking is saved.
```

### Q7. Spring events vs Kafka?

```text
Spring events are in-process and not durable by default. Kafka is for durable cross-service
messaging.
```

---

# 28. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Cache key misses tenant/user | data leak | include all security dimensions |
| No cache eviction | stale results | evict, update, or TTL |
| Local cache assumed shared | each instance has own cache | Redis for shared cache |
| Async without pool limits | thread/queue overload | bounded executor |
| Async method called internally | proxy bypass | call through Spring bean |
| Losing trace ID in async logs | ThreadLocal not propagated | task decorator/context propagation |
| Scheduled job on every pod | duplicate work | external scheduler/lock |
| Sending event before commit | side effect for rolled-back transaction | AFTER_COMMIT event/outbox |
| Long startup runner | slow readiness | move to async job or controlled startup |

---

# 29. One-Hour Revision Plan

### First 15 Minutes: Cache

Revise:
- `@Cacheable`
- key design
- eviction
- Redis vs local
- stampede

Must say:

```text
Caching improves latency but introduces consistency and invalidation trade-offs.
```

### Next 15 Minutes: Async

Revise:
- `@Async`
- executor config
- return types
- self-invocation
- context propagation

Must say:

```text
Async is proxy-based and must run on a controlled executor.
```

### Next 15 Minutes: Scheduling

Revise:
- fixedRate
- fixedDelay
- cron
- multi-node duplication
- distributed lock

Must say:

```text
@Scheduled is local to each instance; cluster-safe scheduling needs extra design.
```

### Final 15 Minutes: Events And Startup

Revise:
- `ApplicationEventPublisher`
- `@EventListener`
- `@TransactionalEventListener`
- runners
- startup events

Must say:

```text
Spring events are good for in-process decoupling, but durable integration needs a broker
or outbox pattern.
```

---

# 30. Final Rapid Revision Sheet

| Need | Spring Feature |
|---|---|
| Enable cache | `@EnableCaching` |
| Read-through cache | `@Cacheable` |
| Update cache | `@CachePut` |
| Remove cache | `@CacheEvict` |
| Shared distributed cache | Redis |
| Background method | `@Async` |
| Async executor | `ThreadPoolTaskExecutor` |
| Periodic method | `@Scheduled` |
| Calendar schedule | cron |
| Avoid duplicate scheduled jobs | distributed lock/external scheduler |
| Publish in-process event | `ApplicationEventPublisher` |
| Listen to event | `@EventListener` |
| Run after commit | `@TransactionalEventListener` |
| Startup code | `ApplicationRunner` / `CommandLineRunner` |
| Startup completed event | `ApplicationReadyEvent` |

---

# 31. Strong Closing Answer

If interviewer asks:

```text
How do you use cache, async, scheduling, and events in Spring Boot?
```

Say:

```text
I use Spring Cache for expensive reads, but design keys and eviction carefully and choose
Redis when cache must be shared across instances. For @Async, I configure bounded executors
and account for proxy behavior and context propagation. For @Scheduled, I remember that
each app instance runs the schedule, so production jobs need external scheduling or locking.
For events, I use Spring events for in-process decoupling and TransactionalEventListener
when side effects should happen only after commit.
```

---

# 32. Official Source Notes

Useful official references:

- Spring Framework Cache: https://docs.spring.io/spring-framework/reference/integration/cache.html
- Spring Boot Caching: https://docs.spring.io/spring-boot/reference/io/caching.html
- Spring Framework Scheduling and Async: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- Spring Boot Task Execution and Scheduling: https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html
- Spring Boot Application Events and Listeners: https://docs.spring.io/spring-boot/reference/features/spring-application.html

