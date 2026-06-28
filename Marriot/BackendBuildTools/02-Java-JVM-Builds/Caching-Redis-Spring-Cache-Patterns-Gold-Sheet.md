# Caching — Redis, Spring Cache, Patterns — Gold Sheet

> Topic: How to make systems fast by storing computed results — and how to avoid cache stampedes, stale data, and thundering herds

---

## 1. Intuition

A Marriott hotel search takes 200ms — it queries availability, pricing, and inventory. With 10,000 requests per second, that's 10,000 × 200ms of database load. If most of those searches are for "New York, 3 nights" and the availability data changes once per minute, you're recalculating the same result 600,000 times between updates. Cache it once, serve it 599,999 times from memory. That's caching.

Beginner version:

> Caching stores the result of an expensive operation so the next caller gets it instantly, without redoing the work.

---

## 2. Definition

- **Cache:** A fast, temporary storage layer that holds precomputed or fetched values to avoid repeated expensive operations.
- **Redis:** An in-memory data structure store used as a distributed cache, message broker, and database.
- **Spring Cache abstraction:** A set of annotations (`@Cacheable`, `@CacheEvict`, `@CachePut`) that wrap method calls with caching logic via AOP — without changing business code.
- **Cache-aside pattern:** The most common caching pattern — the application code checks cache first, falls back to DB on miss, and populates the cache.

---

## 3. Spring Cache Abstraction — Core Annotations

```java
@Service
@CacheConfig(cacheNames = "hotels")   // Default cache name for all methods in this class
public class HotelService {

    // @Cacheable — cache the result; skip the method on subsequent calls if key is in cache
    @Cacheable(key = "#hotelCode")
    public Hotel findByCode(String hotelCode) {
        // Only called on cache MISS
        return hotelRepository.findByHotelCode(hotelCode)
            .orElseThrow(() -> new HotelNotFoundException(hotelCode));
    }

    // @CachePut — ALWAYS execute the method AND update the cache
    // Use for writes — keeps cache consistent with DB
    @CachePut(key = "#hotel.hotelCode")
    public Hotel updateHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    // @CacheEvict — remove entry from cache
    // Use when data is deleted or you need to force a refresh
    @CacheEvict(key = "#hotelCode")
    public void deleteHotel(String hotelCode) {
        hotelRepository.deleteByHotelCode(hotelCode);
    }

    // Evict all entries in the "hotels" cache
    @CacheEvict(allEntries = true)
    @Scheduled(cron = "0 0 * * * *")   // Every hour
    public void clearHotelCache() { }

    // @Caching — combine multiple cache operations on one method
    @Caching(
        evict = {
            @CacheEvict(cacheNames = "hotels",        key = "#booking.hotelCode"),
            @CacheEvict(cacheNames = "availability",  key = "#booking.checkIn + '-' + #booking.hotelCode")
        }
    )
    public Booking confirmBooking(BookingRequest booking) {
        return bookingRepository.save(new Booking(booking));
    }
}
```

**Condition and unless:**

```java
// Only cache if the result has loyalty tier "platinum" or "titanium"
@Cacheable(key = "#userId", condition = "#result != null && #result.loyaltyTier == 'PLATINUM'")
public User findUser(Long userId) { ... }

// Cache the result, but not if it's empty
@Cacheable(key = "#query", unless = "#result.isEmpty()")
public List<Hotel> searchHotels(String query) { ... }
```

---

## 4. Spring Cache Configuration with Redis

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default config — applies to all caches unless overridden
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))                    // 30 min TTL
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));   // JSON serialization

        // Per-cache overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "hotels",       defaults.entryTtl(Duration.ofHours(1)),     // Hotels change rarely
            "availability", defaults.entryTtl(Duration.ofMinutes(5)),   // Availability changes fast
            "pricing",      defaults.entryTtl(Duration.ofMinutes(15)),  // Pricing moderate
            "userSession",  defaults.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaults)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
```

**`application.yml` (Redis connection):**

```yaml
spring:
  data:
    redis:
      host: redis-cluster.marriott.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 1000ms        # Wait up to 1s to borrow a connection
        cluster:
          refresh:
            adaptive: true        # Refresh cluster topology on redirect
```

---

## 5. Redis Data Structures

| Structure | Commands | Use case |
|---|---|---|
| **String** | GET, SET, INCR, EXPIRE | Simple key-value cache, rate counters, distributed locks |
| **Hash** | HGET, HSET, HMGET, HGETALL | User session (field per attribute — partial update without reserializing whole object) |
| **List** | LPUSH, RPOP, LRANGE | Job queues, recent activity feed |
| **Set** | SADD, SISMEMBER, SMEMBERS | Unique visitors, tag collections |
| **Sorted Set** | ZADD, ZRANGE, ZRANGEBYSCORE | Leaderboards, priority queues, rate-limit sliding window |
| **Stream** | XADD, XREAD, XACK | Kafka-like event streams with consumer groups |

```java
// Spring Data Redis — RedisTemplate (raw operations)
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    // Sliding window rate limit: max 100 requests per minute per userId
    public boolean isAllowed(String userId) {
        String key = "rate:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            // First request in window — set expiry
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count <= 100;
    }

    // Sorted Set for sliding window (more precise)
    public boolean isAllowedSlidingWindow(String userId) {
        ZSetOperations<String, String> zops = redisTemplate.opsForZSet();
        String key = "sw:" + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000;  // 1 minute ago

        // Remove entries older than the window
        zops.removeRangeByScore(key, 0, windowStart);

        Long count = zops.zCard(key);
        if (count != null && count >= 100) return false;

        // Add this request
        zops.add(key, UUID.randomUUID().toString(), now);
        redisTemplate.expire(key, Duration.ofMinutes(2));
        return true;
    }
}
```

---

## 6. Caching Patterns

### Cache-Aside (Lazy Loading)

Most common. Application manages the cache explicitly.

```
Read:
  1. Check cache for key
  2. HIT → return cached value
  3. MISS → query database
  4. Store result in cache with TTL
  5. Return result

Write:
  Update database → invalidate/evict cache entry
```

**Pro:** Only caches what's accessed — no wasted memory. Cache can fail gracefully (just slower).
**Con:** Cache miss on first request (cold start); risk of stale data between DB write and eviction.

### Write-Through

Write to cache AND database simultaneously on every write.

```
Write:
  1. Write to cache (with TTL)
  2. Write to database
  Both succeed or both fail (use transactions if needed)

Read:
  Always cache HIT (as long as data was written recently)
```

**Pro:** No stale reads — cache always reflects latest write.
**Con:** Write latency increases; unused data cached (written but never read).

### Write-Behind (Write-Back)

Write to cache immediately; flush to database asynchronously.

```
Write:
  1. Write to cache immediately (fast)
  2. Queue the write for async DB flush

Read:
  Cache HIT always
```

**Pro:** Ultra-low write latency.
**Con:** Data loss risk if cache fails before flush; complex implementation.

---

## 7. Cache Stampede / Thundering Herd

**Problem:** A popular cache key expires. 10,000 concurrent requests all miss the cache simultaneously → all fire a DB query simultaneously → database overwhelmed.

```
T=0: key "availability:NYCMQ:2026-07-04" expires
T=0.001: 10,000 threads all read → all miss → all query DB simultaneously
DB: 10,000 concurrent queries → timeout / crash
```

**Solutions:**

### Lock-based (single writer)

```java
public Hotel getWithLock(String hotelCode) {
    String cached = redisTemplate.opsForValue().get("hotel:" + hotelCode);
    if (cached != null) return deserialize(cached);

    // Only one thread gets the lock
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent("lock:hotel:" + hotelCode, "1", Duration.ofSeconds(5));

    if (Boolean.TRUE.equals(acquired)) {
        try {
            Hotel hotel = hotelRepository.findByHotelCode(hotelCode).orElseThrow();
            redisTemplate.opsForValue().set("hotel:" + hotelCode,
                serialize(hotel), Duration.ofHours(1));
            return hotel;
        } finally {
            redisTemplate.delete("lock:hotel:" + hotelCode);
        }
    } else {
        // Wait briefly and retry — lock holder will populate cache
        Thread.sleep(100);
        return getWithLock(hotelCode);  // Recursive retry (add max retries in prod)
    }
}
```

### Probabilistic Early Expiration (XFetch)

Randomly refresh the cache slightly before it expires — prevents all threads from missing simultaneously.

```java
public Hotel getWithEarlyExpiry(String hotelCode) {
    CacheEntry<Hotel> entry = getEntryWithTtl("hotel:" + hotelCode);

    if (entry == null) {
        return refreshCache(hotelCode);
    }

    // Probabilistic: if TTL is low, randomly decide to refresh early
    // Higher beta = more aggressive early refresh
    double beta = 1.0;
    double delta = computeFetchTime(hotelCode);  // How long last fetch took (ms)
    long ttl = entry.getRemainingTtlMs();

    if (-delta * beta * Math.log(Math.random()) >= ttl) {
        // Probabilistically refresh early — not all threads will do this
        return refreshCache(hotelCode);
    }

    return entry.getValue();
}
```

### Eviction Policies (Redis server-side)

```
# redis.conf
maxmemory 4gb
maxmemory-policy allkeys-lru   # Evict least recently used across ALL keys when full

Options:
  noeviction      — reject writes when full (default)
  allkeys-lru     — evict LRU key across all keys
  volatile-lru    — evict LRU key among keys with TTL set
  allkeys-lfu     — evict least frequently used (Redis 4.0+)
  volatile-ttl    — evict key with shortest remaining TTL
  allkeys-random  — evict random key
```

---

## 8. Local Cache vs Distributed Cache

| | Local (Caffeine) | Distributed (Redis) |
|---|---|---|
| **Latency** | Nanoseconds (in-process) | ~0.5–2ms (network round-trip) |
| **Consistency** | Per-instance — stale if another instance writes | All instances share one view |
| **Eviction** | LRU/W-TinyLFU in heap | LRU/LFU in Redis RAM |
| **Failure mode** | Lost on pod restart | Survives pod restarts |
| **Use case** | Reference data (config, enum lookups) | Session, shared state, rate limiting |

**Caffeine (local cache):**

```java
@Bean
public CacheManager localCacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager("countryConfig");
    manager.setCaffeine(Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofHours(6))
        .recordStats());  // Exposes Micrometer metrics for hit rate
    return manager;
}
```

---

## 9. Common Mistakes

| Mistake | Consequence | Fix |
|---|---|---|
| Caching mutable objects — changing the returned object mutates the cache | Cache corruption — all callers see the mutation | Return immutable objects or deep-copy from cache |
| Not setting TTL | Memory grows unbounded; stale data lives forever | Always set TTL appropriate to data freshness requirement |
| Using object serialization (Java native) for Redis values | Classname embedded in bytes — cache breaks on class rename/refactor | Use JSON serialization (Jackson) |
| Caching null results | `@Cacheable` doesn't cache null by default — cache miss on every call for missing IDs | Use `unless = "#result == null"` or store a sentinel value |
| Cache key collision | Two different methods cache different objects at the same Redis key | Use `cacheNames` + fully qualified `key` expressions; include class context |

---

## 10. Interview Insight

Strong answer:

> Spring Cache provides `@Cacheable`, `@CachePut`, and `@CacheEvict` annotations backed by AOP — the business logic doesn't change, only the cache behavior changes in config. We back it with Redis via `RedisCacheManager` with per-cache TTLs: short TTLs (5min) for availability, longer (1h) for hotel metadata. The cache-aside pattern is the default — on miss, we load from DB and store. The thundering herd problem occurs when a popular key expires and thousands of threads simultaneously query the DB — we solve this with Redis `SET NX` distributed locks or probabilistic early expiry. Eviction policy in Redis is `allkeys-lru` so the most relevant data stays warm.

Follow-up trap:

> What's wrong with setting a very high TTL on your cache?

Good answer:

> Stale data. If hotel pricing is cached for 24 hours but the revenue team updates pricing, users see yesterday's prices for up to 24 hours. The fix is event-driven eviction — when pricing is updated, the system publishes an event and the cache evicts that specific entry immediately (`@CacheEvict`). TTL becomes the safety net, not the primary eviction mechanism.

---

## 11. Revision Notes

- One-line summary: Spring Cache annotations wrap methods with Redis-backed caching; cache-aside is the standard read pattern; thundering herd is solved with distributed locks or probabilistic early expiry; eviction policy and TTL must match data freshness requirements.
- Three keywords: cache-aside, thundering herd, TTL.
- One interview trap: Java object serialization breaks on class changes — always use JSON (Jackson) for Redis values.
- Memory trick: Cache stampede = all threads expire at the same time → DB dies. Fix: probabilistic early refresh = some threads refresh before expiry, distributing load.
