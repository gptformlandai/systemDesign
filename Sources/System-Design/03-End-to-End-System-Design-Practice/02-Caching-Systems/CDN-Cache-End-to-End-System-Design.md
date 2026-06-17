# CDN Cache - End-to-End System Design

> Goal: design a CDN caching layer that serves content close to users, reduces origin load, improves latency, and safely handles TTLs, purges, cache keys, private content, and cache-miss storms.

---

## How To Use This File

- Use this when the interview problem says CDN, edge cache, static asset delivery, media delivery, API response caching at edge, or origin offload.
- Focus on request flow, cache keys, TTL, invalidation, origin protection, and private-content safety.
- Explain CDN as a distributed cache in front of origin, not just a faster server.
- In interviews, always discuss cache-hit ratio and what happens on miss storms.

---

## Starter Learning Path

Read this file in four passes:

1. First pass: understand the product goal, core requirements, and the user-facing workflow.
2. Second pass: trace the main read/write path through the high-level design.
3. Third pass: study the data model, scaling choices, failures, and trade-offs.
4. Fourth pass: practice the LLD, machine-coding layer, and final interview playbook without looking.

What a starter should master first:

- The one-line purpose of the system.
- The core entities and APIs.
- The main request flow.
- The storage choice and why it fits.
- The biggest bottleneck.
- The failure that most affects users.
- The trade-off you would defend in an interview.

Gold-level self-check:

- You can draw the architecture from memory in 5 minutes.
- You can explain the happy path and one failure path clearly.
- You can justify consistency, latency, availability, and cost choices.
- You can name what you would simplify for an MVP and what you would add at scale.
- You can answer follow-ups about spikes, retries, idempotency, observability, and data growth.

---

# Master Checklist For This Problem

| Layer | Interview signal | CDN cache focus |
|---|---|---|
| Problem understanding | Can define cacheable content | static assets, media segments, public API responses |
| HLD | Can design edge flow | DNS, edge POP, regional cache, origin shield, origin |
| LLD | Can model cache key and policy | URL, headers, query params, TTL, purge, signed URLs |
| Machine coding | Can simulate cache behavior | key normalization, TTL, stale-while-revalidate |
| Traffic spikes | Can protect origin | origin shield, request coalescing, stale serve, rate limits |
| Billion users | Can scale globally | POPs, replication, regional routing, invalidation propagation |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Cache static assets and cacheable responses at edge locations.
- Route users to nearby edge POPs.
- Serve cache hits without contacting origin.
- Fetch from origin on cache miss.
- Respect TTL and cache-control policies.
- Support purge/invalidation by URL, prefix, tag, or version.
- Support large-object/media delivery.
- Protect private or user-specific content from leaking across users.
- Emit cache hit/miss, latency, bandwidth, and origin-load metrics.

Optional requirements:

- Signed URLs or signed cookies for protected content.
- Stale-while-revalidate and stale-if-error.
- Origin shield or mid-tier cache.
- Image/video transformation at edge.
- API response caching.
- Multi-CDN failover.
- Geo-blocking or data residency rules.

Out of scope unless asked:

- Full video transcoding system.
- Full DNS provider implementation.
- Full WAF/bot protection platform.
- Complete browser cache design.

## 1.2 Non-Functional Requirements

Latency:

- Serve cache hits close to users with low latency.
- Minimize origin round trips.
- Avoid slow global invalidation blocking request serving.

Availability:

- Continue serving cached content when origin is degraded if policy allows.
- Prevent cache-miss storms from taking down origin.
- Support failover between edge POPs or CDN providers.

Correctness and safety:

- Never cache private content under a public cache key.
- Respect auth-sensitive headers and cache-control.
- Purge or version content when it changes.
- Avoid serving stale content beyond acceptable bounds.

## 1.3 Constraints

- Edge POPs are globally distributed and eventually receive config/purge updates.
- Cache capacity at each POP is finite.
- Popular content distribution is skewed.
- Query parameters and headers can explode cache-key cardinality.
- Low TTL improves freshness but reduces hit ratio.
- High TTL improves performance but increases stale-content risk.
- Invalidation at global scale is hard.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Global traffic | 10 Tbps+ peak for media/static assets |
| Edge POPs | 100+ globally |
| Cache hit ratio target | 90%+ for static assets, lower for APIs |
| Origin request reduction | 10x to 100x for cacheable assets |
| Object sizes | KBs for assets, MBs for images, MB/GB for videos |
| Purge propagation | seconds to minutes depending provider |
| Availability target | 99.99%+ edge serving |

Back-of-the-envelope:

- If origin receives 1M requests/sec and CDN hit ratio is 95%, origin sees only 50K requests/sec.
- A drop from 95% to 80% hit ratio quadruples origin traffic.
- Short TTLs can accidentally create periodic miss storms.
- Large media objects need range requests and partial caching.

## 1.5 Clarifying Questions To Ask

- What content is cacheable: static assets, images, videos, API responses?
- Is content public, private, or mixed?
- What freshness guarantees are required?
- Do we need purge by URL, tag, prefix, or version?
- Are signed URLs required?
- What is acceptable stale serving during origin failure?
- Do query parameters affect response content?
- Do headers like `Authorization`, `Cookie`, or `Accept-Language` affect response content?

Strong interview framing:

> I will design CDN caching as a global edge cache in front of origin. Users are routed to a nearby POP, cache hits are served locally, misses go through an origin shield to protect origin, and cache behavior is controlled through cache keys, TTL, stale policies, signed URLs, and purge/versioning.

---

# 2. High-Level Design

## 2.1 Architecture

Basic flow:

```text
Client
  -> DNS / Anycast / CDN routing
  -> Edge POP cache
       hit  -> serve response
       miss -> regional cache / origin shield
            -> origin service/object store
            -> store response with TTL
            -> serve client
```

Recommended architecture:

```text
Client
  |
  v
+------------------+
| CDN Edge POP     |
| L1 cache         |
+--------+---------+
         |
         | miss
         v
+------------------+
| Regional Cache   |
| Origin Shield    |
+--------+---------+
         |
         | miss/coalesced fetch
         v
+------------------+       +------------------+
| Origin Service   |<----->| Object Store      |
+--------+---------+       +------------------+
         |
         v
+------------------+
| Purge/Config API |
+------------------+
```

## 2.2 APIs And Headers

### Public Static Asset Request

```http
GET /assets/app.v123.css HTTP/1.1
Host: cdn.example.com
```

Origin response:

```http
HTTP/1.1 200 OK
Cache-Control: public, max-age=31536000, immutable
ETag: "asset-v123"
Content-Type: text/css
```

### API Response Cache

```http
GET /v1/catalog/top-products?country=US HTTP/1.1
Host: api.example.com
```

Origin response:

```http
HTTP/1.1 200 OK
Cache-Control: public, max-age=60, stale-while-revalidate=30
Vary: Accept-Language
```

### Protected Media With Signed URL

```http
GET /videos/vid123/segment001.ts?expires=1781694000&signature=abc123 HTTP/1.1
Host: media.example.com
```

### Purge API

```http
POST /internal/cdn/purge
Content-Type: application/json

{
  "type": "TAG",
  "value": "product:123",
  "reason": "product image updated"
}
```

## 2.3 Core Components

Think of CDN Cache as a globally distributed cache hierarchy.

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| DNS/Anycast routing | send user to good POP | object freshness | global routing health |
| Edge POP | first-level cache and TLS | source of truth | local cache capacity |
| Regional cache/origin shield | mid-tier cache | final content generation | origin protection |
| Origin service | source response generation | edge eviction | miss load |
| Object store | static/media bytes | request policy | storage/bandwidth |
| Cache policy engine | TTL, cache key, stale rules | content generation | config correctness |
| Purge service | invalidation events | cache serving path | global propagation |
| Metrics pipeline | hit/miss/latency/bandwidth | request routing decisions | high volume |

### Cache Key

Cache key decides whether two requests can share a cached response.

Common cache-key parts:

- scheme,
- host,
- path,
- selected query parameters,
- selected headers,
- content encoding,
- locale or device type if response varies.

Dangerous cache-key mistakes:

| Mistake | Result |
|---|---|
| ignore auth/cookie for private response | data leak |
| include all query params blindly | low hit ratio from key explosion |
| ignore locale header when response varies | wrong language |
| ignore content encoding | wrong compressed response |
| cache personalized API response as public | severe privacy issue |

### TTL And Freshness

| Policy | Meaning | Use case |
|---|---|---|
| `max-age` | cache lifetime | normal cache freshness |
| `s-maxage` | shared cache lifetime | CDN-specific TTL |
| `immutable` | asset never changes at same URL | versioned static files |
| `stale-while-revalidate` | serve stale while refreshing | low-latency public APIs |
| `stale-if-error` | serve stale when origin fails | availability during origin outage |
| `no-store` | do not cache | private/sensitive data |

### Invalidation Strategies

| Strategy | How it works | Trade-off |
|---|---|---|
| versioned URL | `/app.v123.js` | best for static assets |
| TTL expiry | wait for expiration | simple, stale window |
| explicit purge | remove object from edge | operational complexity |
| tag purge | purge all objects with tag | useful for grouped content |
| soft purge | mark stale and revalidate | avoids sudden empty cache |

Recommendation:

- Use versioned URLs for static assets.
- Use TTL plus stale policies for public API responses.
- Use explicit purge for urgent correctness fixes.
- Use signed URLs/cookies for protected media.

### Origin Protection

Tools:

- origin shield,
- request coalescing,
- stale-while-revalidate,
- stale-if-error,
- rate limiting,
- circuit breakers,
- cache warming for major launches,
- soft purge instead of hard purge.

One-stop interview answer:

> A CDN cache serves content from nearby edge POPs using a safe cache key and TTL policy. Cache misses go through a regional shield to protect origin. For freshness, I prefer versioned URLs for static assets, TTL/stale policies for public API responses, and explicit purge for urgent changes. Private content needs signed URLs and must not be cached under a public key.

---

# 3. Low-Level Design

LLD goal:

> Model CDN caching around cache key construction, cache policy, object metadata, and stale behavior.

Starter map:

| LLD question | CDN cache answer |
|---|---|
| Request input | `HttpRequest` |
| Cache identity | `CacheKey` |
| Policy object | `CachePolicy` |
| Cached value | `CachedObject` |
| Freshness check | `expiresAt`, `staleUntil`, `etag` |
| Origin fetch | `OriginClient` |
| Purge | `PurgeIndex` or tag map |
| Output | cached response or fetched response |

Beginner-friendly design order:

1. Normalize request into a cache key.
2. Look up cached object.
3. If fresh, serve it.
4. If stale but allowed, serve stale and refresh async.
5. If missing/expired, fetch from origin.
6. Store response if cacheable.
7. Support purge by key/tag.
8. Emit hit/miss/stale metrics.

Interview sentence:

> The CDN cache is mostly about safe cache-key construction and freshness policy. The same URL can be safely shared only when all response-varying inputs are included in the key or the response is explicitly public.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `CacheKey` | normalized identity of cacheable response | includes all vary dimensions |
| `CachePolicy` | TTL, stale rules, cacheability | private data must not be public cached |
| `CachedObject` | response body, headers, metadata | expiration metadata is explicit |
| `OriginClient` | fetch from source | protected by timeouts/coalescing |
| `PurgeRequest` | invalidate object(s) | auditable and idempotent |
| `PurgeIndex` | URL/tag to cached entries | eventually consistent globally |

## 3.2 Class Sketch

```java
final class CacheKey {
    private final String host;
    private final String path;
    private final Map<String, String> queryParts;
    private final Map<String, String> varyHeaders;
}

final class CachePolicy {
    private final boolean cacheable;
    private final long ttlMs;
    private final long staleWhileRevalidateMs;
    private final boolean privateResponse;
}

final class CachedObject {
    private final byte[] body;
    private final Map<String, String> headers;
    private final long expiresAtMs;
    private final long staleUntilMs;
    private final String etag;
}
```

## 3.3 Sequence Diagram

Cache hit:

```text
Client -> EdgePOP: GET object
EdgePOP -> CacheKeyBuilder: normalize request
EdgePOP -> LocalCache: lookup key
LocalCache --> EdgePOP: fresh object
EdgePOP --> Client: cached response
```

Cache miss:

```text
Client -> EdgePOP: GET object
EdgePOP -> LocalCache: miss
EdgePOP -> OriginShield: fetch key
OriginShield -> Origin: fetch object
Origin --> OriginShield: response with cache headers
OriginShield -> EdgePOP: response
EdgePOP -> LocalCache: store if cacheable
EdgePOP --> Client: response
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Cache-Aside | edge fetches from origin on miss |
| Strategy | TTL, key-building, eviction policies |
| Adapter | different origin/object store clients |
| Decorator | metrics/tracing around cache lookup |
| Circuit Breaker | protect origin during failures |
| Single Flight | coalesce concurrent misses for same key |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| private response | `Cache-Control: private/no-store` or signed per-user key |
| query param does not affect content | exclude from cache key to improve hit ratio |
| query param affects content | include in cache key |
| origin fails | serve stale if allowed |
| purge not propagated yet | versioned URLs or bounded TTL reduce risk |
| large object | support range requests and chunk caching |
| personalized API | avoid shared CDN caching or use user-specific key safely |
| compressed response | include encoding in vary key |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
cdncache/
  CacheKey.java
  CachePolicy.java
  CachedObject.java
  EdgeCache.java
  OriginClient.java
  PurgeService.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from time import time


@dataclass
class CachedObject:
    value: str
    expires_at: float
    stale_until: float


class EdgeCache:
    def __init__(self) -> None:
        self.objects: dict[str, CachedObject] = {}

    def get(self, key: str, fetch_origin, ttl: int, stale_ttl: int = 0) -> tuple[str, str]:
        now = time()
        cached = self.objects.get(key)

        if cached and cached.expires_at > now:
            return cached.value, "HIT"

        if cached and cached.stale_until > now:
            # Real CDNs refresh asynchronously here.
            return cached.value, "STALE"

        value = fetch_origin(key)
        self.objects[key] = CachedObject(
            value=value,
            expires_at=now + ttl,
            stale_until=now + ttl + stale_ttl,
        )
        return value, "MISS"

    def purge(self, key: str) -> None:
        self.objects.pop(key, None)


cache = EdgeCache()
origin = lambda key: f"origin-value-for-{key}"
print(cache.get("/assets/app.v1.js", origin, ttl=60))
print(cache.get("/assets/app.v1.js", origin, ttl=60))
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| object lookup | hash map / local cache index |
| eviction | LRU/LFU/size-aware policy |
| purge by tag | tag to keys inverted index |
| request coalescing | key to in-flight fetch map |
| metadata | object header/TTL record |
| metrics | counters/histograms by POP/key class |

## 4.4 Concurrency

- Concurrent misses for the same key should be coalesced.
- Cache writes should be atomic per key.
- Purge and fetch racing should respect object version or purge timestamp.
- Large object fetches should support streaming instead of full buffering.
- Metrics should be async and non-blocking.

## 4.5 Testing Checklist

- Fresh object returns hit.
- Missing object fetches origin and stores response.
- Expired object fetches again.
- Stale object serves stale if policy allows.
- Purge removes object.
- Private response is not cached.
- Cache key includes required query/header dimensions.
- Concurrent misses coalesce to one origin fetch.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Cache-Miss Storm

Problem:

- Many objects expire or purge at once, causing massive origin traffic.

Handling:

- Origin shield.
- Request coalescing.
- TTL jitter.
- Soft purge.
- Stale-while-revalidate.
- Cache warming.
- Origin rate limiting and circuit breakers.

## 5.2 Viral Content Spike

Handling:

- Replicate hot object across POPs.
- Increase TTL if safe.
- Pre-warm caches for expected launches.
- Use regional shield to avoid origin fan-in.
- Monitor bandwidth and hit ratio.

## 5.3 Origin Outage

Handling:

- Serve stale-if-error when allowed.
- Return friendly errors for uncacheable content.
- Fail over to backup origin.
- Avoid retry storms.
- Keep health-based origin selection.

## 5.4 Bad Purge

Problem:

- Accidentally purging too many objects drops hit ratio and overloads origin.

Handling:

- Require approvals for broad purge.
- Use soft purge.
- Rate-limit purge jobs.
- Canary purge by region.
- Keep origin shield and request coalescing ready.

---

# 6. Scaling To A Billion Users

## 6.1 Cache Hierarchy

| Layer | Purpose |
|---|---|
| browser cache | avoid network request entirely |
| edge POP | serve near user |
| regional cache | reduce long-haul origin fetches |
| origin shield | coalesce misses and protect origin |
| origin/object store | source of truth |

## 6.2 Global Routing

Routing inputs:

- user location,
- POP health,
- network latency,
- capacity,
- content availability,
- compliance/geofence rules.

Approaches:

- DNS-based routing,
- Anycast routing,
- CDN internal request routing,
- multi-CDN traffic steering.

## 6.3 Eviction At Edge

CDN POPs have finite storage.

Eviction signals:

- recency,
- frequency,
- object size,
- content tier,
- cost to refetch,
- regional popularity.

Common policy:

- size-aware LRU/LFU hybrid,
- keep hot media segments,
- evict cold large objects first.

## 6.4 Observability

Track:

- cache hit ratio,
- byte hit ratio,
- miss rate by origin,
- edge latency,
- origin latency,
- purge propagation lag,
- stale served count,
- top cache keys,
- 4xx/5xx by POP,
- origin shield hit ratio,
- bandwidth by region,
- private-cache bypass count.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify cacheable content and privacy requirements.
2. Draw client to edge POP to shield to origin flow.
3. Define cache key and TTL policy.
4. Explain hit, miss, stale, and purge paths.
5. Discuss signed URLs/private content safety.
6. Add origin protection and miss-storm handling.
7. Discuss global POP routing and cache hierarchy.
8. Close with observability and hit-ratio trade-offs.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| freshness | short TTL | long TTL | long for versioned static, short/stale for APIs |
| invalidation | purge | versioned URL | versioned URL when possible |
| private content | CDN cache | bypass/signed URL | signed URL only if key is safe |
| origin fetch | direct edge to origin | origin shield | origin shield at scale |
| purge | hard purge | soft purge | soft purge for large groups |

## 7.3 Common Mistakes

- Caching private data with a public cache key.
- Including every query parameter and destroying hit ratio.
- Ignoring headers that change response content.
- Using short TTLs everywhere and overloading origin.
- Purging huge content sets without origin protection.
- Forgetting stale-if-error for availability.
- Tracking request hit ratio but not byte hit ratio.

## 7.4 Strong Closing

> CDN caching is a global edge-cache design. I would route users to nearby POPs, use safe cache keys and TTLs, serve hits at the edge, send misses through an origin shield, protect origin with coalescing and stale policies, and use versioned URLs or controlled purge for freshness. Private content must be handled with signed URLs or bypassed to avoid leaks.

---

# 8. Fast Recall Rules

- CDN = distributed edge cache.
- Cache key correctness is safety-critical.
- Public static assets should use versioned URLs and long TTL.
- API responses need careful TTL and vary headers.
- Private responses should not be shared cached.
- Origin shield protects origin from miss storms.
- Stale-while-revalidate improves latency.
- Stale-if-error improves availability.
- Purge is eventually propagated and can create miss storms.
- Track hit ratio, byte hit ratio, origin load, and purge lag.