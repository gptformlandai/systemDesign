# URL Shortener - End-to-End System Design

> Goal: practice one complete E2E problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and billion-user scale.

---

## How To Use This File

- Treat this as the repeatable pattern for every E2E problem.
- Start broad with requirements and scale, then zoom into architecture, APIs, data, reliability, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For URL shortener specifically, optimize the redirect path first because reads dominate writes.

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

| Layer | Interview signal | URL shortener focus |
|---|---|---|
| Problem understanding | Can clarify scope and scale | create short link, redirect, expiry, analytics, abuse controls |
| HLD | Can design scalable systems | API gateway, redirect service, cache, metadata store, analytics stream |
| LLD | Can model maintainable components | `UrlMapping`, `CodeGenerator`, `UrlRepository`, `RedirectService` |
| Machine coding | Can implement critical path | generate code, reserve alias, redirect lookup, expiry handling |
| Traffic spikes | Can protect production | CDN/edge cache, rate limits, hot-link handling, analytics shedding |
| Billion users | Can reason at global scale | multi-region reads, sharded metadata, distributed ID generation, async analytics |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create a short URL for a long URL.
- Redirect a short URL code to the original long URL.
- Support optional custom alias, such as `sho.rt/aravind-trip`.
- Support optional expiration time.
- Support authenticated users viewing and managing their own links.
- Track basic analytics such as click count, referrer, country, device, and timestamp.

Optional requirements to clarify:

- Should links be editable after creation?
- Should a deleted link return `404`, `410 Gone`, or a branded error page?
- Should custom aliases be case-sensitive?
- Should malicious URLs be blocked before creation or asynchronously after creation?
- Should we support bulk URL creation for enterprise customers?

Out of scope unless interviewer asks:

- Full marketing campaign management.
- Full fraud detection platform.
- Full analytics dashboard implementation.
- Browser extensions or QR code generation.

## 1.2 Non-Functional Requirements

Redirect path:

- Very low latency because every redirect is user-facing.
- High availability because broken redirects damage trust immediately.
- Heavy read optimization because redirects are far more frequent than creates.
- Safe degradation: redirect should continue even if analytics is down.

Create path:

- Strong uniqueness for generated short codes and custom aliases.
- Idempotent create API for retries.
- Moderate latency is acceptable compared with redirect path.
- Abuse protection for spam, malware, phishing, and bulk creation.

Data and operations:

- Durable mapping storage.
- Clear expiration behavior.
- Auditability for enterprise accounts.
- Privacy controls for analytics data.

## 1.3 Constraints

- Short codes must be compact and URL-safe.
- Generated codes must not collide.
- Custom aliases must be globally unique or scoped by domain.
- Redirects should not synchronously depend on analytics processing.
- The system must handle hot links where one code receives massive traffic.
- The system must handle abusive traffic without hurting normal redirects.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Users | 1 billion registered or reachable users |
| New short links | 100 million/day at large scale |
| Redirects | 10 billion/day initially; can spike much higher |
| Read/write ratio | 100:1 or higher |
| Average long URL | 500 bytes |
| Metadata per URL | 500-1000 bytes before indexes/replication |
| Redirect latency target | p99 under 100 ms from nearest region for cached links |
| Availability target | 99.99%+ for redirect path |

Back-of-the-envelope:

- `100M new links/day * 1 KB/link = 100 GB/day raw mapping data`.
- `100 GB/day * 365 = 36.5 TB/year raw mapping data`.
- With replication, indexes, and overhead, plan for `100-250 TB/year` depending on retention.
- `10B redirects/day = about 116K average QPS`.
- Peak can be 10x-50x average, so design redirect path for millions of QPS globally.
- Click analytics can be much larger than mapping storage. `10B events/day * 300 bytes = 3 TB/day raw click events` before compression and aggregation.

## 1.5 Clarifying Questions To Ask

- Are short links permanent by default or do they expire?
- Is analytics exact or approximate?
- Do we need custom domains for enterprise customers?
- Can popular redirects be cached at CDN/edge?
- Do users expect immediate consistency after creating a link?
- What should happen if malware scanning later marks a destination unsafe?
- Is the redirect response `301`, `302`, or configurable?

Strong interview framing:

> I will optimize the redirect path for low latency and availability, make create operations idempotent and collision-safe, and move analytics/abuse workflows off the critical redirect path.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Create flow:
Client
  -> API Gateway
  -> URL Management Service
  -> Code Generator / Alias Reservation
  -> URL Metadata Store
  -> Cache Warmup
  -> Analytics / Audit Event Stream

Redirect flow:
Browser
  -> CDN / Edge / Anycast Load Balancer
  -> Redirect Service
  -> Cache Lookup
  -> URL Metadata Store on miss
  -> 301/302 response
  -> Async Click Event Stream
```

Recommended architecture:

```text
Client / Browser
    |
    v
+------------+              +---------------------+
| CDN / Edge |              | Analytics Query API |
+-----+------+              +----------+----------+
    |                                |
    v                                v
+------------+              +---------------------+
| API Gateway|              | OLAP / Data Lake    |
+-----+------+              +---------------------+
    |
    v
+------------+           +-------------------+
| Redirect   |           | URL Management    |
| Service    |           | Service           |
+-----+------+           +---------+---------+
    |                            |
    v                            v
+------------+           +-------------------+
| Redis /    |           | Code Generator    |
| Local Cache|           | Alias Reservation |
+-----+------+           +---------+---------+
    |                            |
    v                            v
+---------------------------------------------+
| URL Metadata Store, sharded by short code    |
+---------------------------------------------+
    |
    v
+---------------------------------------------+
| Kafka / Stream for clicks, abuse, audit      |
+---------------------------------------------+
```

Request flow for redirect:

1. Browser requests `https://sho.rt/abc123`.
2. CDN/edge checks cached redirect if allowed.
3. Redirect service extracts `abc123`.
4. Service checks local in-process cache.
5. On miss, service checks Redis or regional cache.
6. On miss, service reads metadata store by primary key `code`.
7. Service validates status and expiry.
8. Service returns `302 Found` or `301 Moved Permanently` depending on product policy.
9. Service emits click event asynchronously.

## 2.2 APIs

### Create Short URL

```http
POST /v1/urls
Idempotency-Key: 9b7f8c2e-1234
Authorization: Bearer <token>
Content-Type: application/json

{
  "longUrl": "https://example.com/products/123?campaign=spring",
  "customAlias": "spring-sale",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Response:

```json
{
  "code": "spring-sale",
  "shortUrl": "https://sho.rt/spring-sale",
  "longUrl": "https://example.com/products/123?campaign=spring",
  "expiresAt": "2027-01-01T00:00:00Z",
  "status": "ACTIVE"
}
```

Important points:

- Use an idempotency key so client retries do not create duplicate links.
- Validate URL syntax and scheme.
- Optionally perform synchronous deny-list checks and asynchronous deep scanning.
- Custom alias reservation must be atomic.

### Redirect

```http
GET /{code}
```

Response:

```http
HTTP/1.1 302 Found
Location: https://example.com/products/123?campaign=spring
Cache-Control: public, max-age=60
```

Redirect status choice:

| Status | When to use | Trade-off |
|---|---|---|
| `301` | permanent links where destination rarely changes | clients/CDNs cache aggressively; harder to change/delete quickly |
| `302` | default product choice for analytics, expiry, or editable links | less permanent caching; more requests reach service |
| `307` | preserve method for non-GET redirects | rarely needed for URL shorteners |

### Delete Or Disable URL

```http
DELETE /v1/urls/{code}
Authorization: Bearer <token>
```

Response:

```json
{
  "code": "abc123",
  "status": "DISABLED"
}
```

### Analytics Query

```http
GET /v1/urls/{code}/analytics?from=2026-06-01&to=2026-06-17
Authorization: Bearer <token>
```

Response:

```json
{
  "code": "abc123",
  "totalClicks": 1423000,
  "uniqueVisitorsApprox": 872000,
  "topCountries": ["US", "IN", "GB"],
  "topReferrers": ["twitter.com", "linkedin.com"]
}
```

## 2.3 Core Components

Think of a URL shortener as two very different systems living together:

| Plane | What it handles | Main goal |
|---|---|---|
| Create/control plane | create link, reserve alias, validate URL, manage expiry/status | correctness and uniqueness |
| Redirect/data plane | resolve `code -> longUrl` and return redirect | very low latency and high availability |
| Background plane | analytics, abuse scanning, audit, expiry cleanup | throughput without slowing redirects |

The redirect path is the product. If analytics, dashboards, or deep abuse scoring are slow, redirects should still work for safe active links.

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Edge/CDN/WAF | global routing, DDoS absorption, hot redirect caching | canonical mapping truth | redirect QPS and hot-link skew |
| API Gateway | auth, request validation, rate limits | URL business logic | API QPS |
| Redirect Service | fast code lookup, expiry/status validation, redirect response | link creation and analytics aggregation | read QPS |
| URL Management Service | create/update/delete/custom alias workflows | serving every redirect | write/admin QPS |
| Code Generator | collision-resistant short-code candidates | persistence and ownership | create QPS |
| Metadata Store | durable `code -> longUrl` mapping | click analytics events | mapping volume/read QPS |
| Cache Hierarchy | hot mapping lookup acceleration | source-of-truth correctness | hit rate and memory budget |
| Analytics Event Pipeline | click event ingestion and aggregation | synchronous redirect success | event volume |
| Abuse/Safety Service | phishing/malware policy decisions | redirect storage mechanics | scan volume and risk load |
| Expiry/Cleanup Jobs | tombstones, expired-link cleanup, retention | real-time redirect validation | object/link count |

### Edge / CDN / WAF Layer

Why it exists:

- Redirect traffic can be globally massive and extremely spiky.
- Viral links can overload origin services if every click reaches the backend.
- Abusive clients and bots should be filtered before hitting application services.

Responsibilities:

- Route users to the nearest healthy region.
- Terminate TLS and apply WAF/DDoS protections.
- Cache safe redirect responses for short TTLs.
- Support health checks and regional failover.
- Apply basic request shaping for suspicious traffic.

Important caching rule:

- Cache only when deletion/expiry/abuse controls can still be honored.
- Prefer short TTLs for editable or user-managed links.
- Use purge/invalidation for high-risk disables.

Failure behavior:

- If one region is unhealthy, route to another region with replicated mappings.
- If CDN cache is stale, TTL/purge bounds incorrect redirects.
- If traffic is abusive, WAF/rate limits protect origin capacity.

Interview signal:

> Edge/CDN protects the redirect path from global latency and hot-link amplification, but the metadata store remains the canonical truth.

### API Gateway

Why it exists:

- Create/manage/analytics APIs need authentication and tenant-level controls.
- Redirect requests are mostly public, but still need abuse controls and safe routing.

Responsibilities:

- Authenticate create/manage/dashboard APIs.
- Validate request size and schema.
- Rate-limit link creation, analytics queries, and admin actions.
- Attach request IDs and trace context.
- Forward business operations to the correct backend service.

What it should avoid:

- Do not generate short codes.
- Do not own URL metadata.
- Do not synchronously perform heavy abuse scans on every redirect.

Interview signal:

> The gateway is the policy boundary. It protects APIs and routes requests, while redirect and URL-management services own business behavior.

### Redirect Service

Why it exists:

- It is the hottest path in the system.
- It must convert a short code into a redirect response with minimal latency.

Core responsibilities:

- Extract and normalize the short code.
- Check local cache, regional cache, then metadata store.
- Validate mapping status: active, expired, disabled, deleted, unsafe.
- Return `301`, `302`, or `307` based on product policy.
- Emit click event asynchronously.
- Fail fast when mapping is missing or invalid.

Critical rule:

```text
Redirect must not wait for analytics aggregation, dashboard writes, or slow abuse scoring.
```

Scaling notes:

- Stateless service; scale horizontally.
- Keep response payload tiny.
- Use aggressive timeouts for cache/store dependencies.
- Use local in-process cache for hottest mappings.

Failure behavior:

- Analytics pipeline down: redirect still succeeds and buffers/samples/drops events by policy.
- Cache down: fallback to metadata store with circuit breaker.
- Metadata store slow: use stale cache only if policy allows; otherwise fail fast.

Interview signal:

> Redirect Service is optimized for p99 latency and availability. Everything non-essential moves off the synchronous path.

### URL Management Service

Why it exists:

- Link creation and management need stronger correctness than redirects.
- Custom aliases must be reserved atomically.
- Client retries should not create duplicate links.

Core responsibilities:

- Validate long URL, expiration, custom alias, and account limits.
- Enforce idempotency for create requests.
- Call code generator for generated codes.
- Reserve custom aliases atomically.
- Save mapping metadata.
- Disable/delete links and publish cache invalidation events.

State transitions:

```text
ACTIVE -> DISABLED
ACTIVE -> EXPIRED
ACTIVE -> DELETED/TOMBSTONED
ACTIVE -> BLOCKED_BY_ABUSE
```

Failure behavior:

- Duplicate create retry returns same code.
- Alias conflict returns a clear conflict error.
- Mapping DB write failure means no link is returned.
- Cache invalidation failure is retried through event pipeline.

Interview signal:

> URL Management Service protects uniqueness and lifecycle correctness, while Redirect Service protects read latency.

### Code Generator / Alias Reservation

Why it exists:

- Every generated short code must be compact, URL-safe, and collision-resistant.
- Custom aliases need atomic ownership.

Generation options:

| Option | How it works | Pros | Cons |
|---|---|---|---|
| Auto-increment ID + Base62 | encode unique numeric ID | compact, no collision if ID unique | central bottleneck unless ID allocation is distributed |
| Snowflake-style ID + Base62 | timestamp + node + sequence | distributed and high throughput | longer codes, clock concerns |
| Random Base62 | generate random candidate and retry collision | simple and hard to guess | collision probability grows with volume |
| Pre-generated pool | workers reserve unused codes ahead of time | fast create path | pool management and wastage |

Recommended default:

- Use distributed ID blocks or Snowflake-style IDs with Base62 for generated links.
- Use `saveIfAbsent(code)` for both generated and custom aliases.
- Use at least 7 Base62 characters for very large scale.

Base62 capacity:

| Code length | Capacity |
|---|---:|
| 6 | about 56.8 billion |
| 7 | about 3.5 trillion |
| 8 | about 218 trillion |

Interview signal:

> Code generation is only half the problem. The real correctness point is atomic reservation in storage.

### Metadata Store

Why it exists:

- It is the durable source of truth for every short-code mapping.
- Redirect traffic needs fast primary-key reads by `code`.

Core responsibilities:

- Store `code`, `longUrl`, owner, status, expiration, creation time, and version.
- Support atomic insert-if-absent for alias reservation.
- Support status changes for disable/delete/abuse block.
- Support user dashboard indexes separately from redirect lookup.

Storage choice:

- Use KV/wide-column storage for massive redirect lookups.
- Use SQL/document store for account/admin/billing metadata if needed.
- Keep analytics out of the OLTP mapping row.

Failure behavior:

- Mapping write failure: create request fails safely.
- Mapping read failure: redirect falls back to cache if allowed.
- Replication lag: recent links may route to write region or use read-your-write cache warming.

Interview signal:

> The metadata store is optimized around one primary access pattern: lookup by short code.

### Cache Hierarchy

Why it exists:

- Redirects are read-heavy and hot-link skew is extreme.
- Caches reduce p99 latency and origin load.

Recommended layers:

| Layer | Stores | Notes |
|---|---|---|
| CDN/edge | cached redirect response | best for viral links, short TTL |
| local LRU | hottest mappings per instance | fastest fallback before Redis |
| regional Redis/Memcached | active mapping records | shared regional cache |
| negative cache | missing/expired codes | short TTL to protect DB from random-code scans |

Invalidation rules:

- DB update first, invalidation event second.
- Purge CDN for critical abuse/deletion cases.
- Keep TTL short enough to bound stale redirects.
- Check expiry/status at service layer even when mapping is cached.

Interview signal:

> Cache makes redirect fast, but correctness still comes from status/expiry validation and bounded stale windows.

### Analytics Event Pipeline

Why it exists:

- Click analytics volume can exceed mapping writes by orders of magnitude.
- Dashboard aggregation should not slow down user redirects.

Core responsibilities:

- Accept click events from Redirect Service.
- Buffer events in Kafka/Pulsar or equivalent stream.
- Aggregate counts by code, time bucket, country, referrer, and device.
- Store queryable analytics in OLAP/data warehouse.
- Support sampling or approximate unique visitor counts at massive scale.

Failure behavior:

- Stream down: bounded buffer, sampling, or drop low-value events.
- Hot code: shard analytics key by time bucket and random bucket.
- Duplicate event: aggregation should be idempotent or approximate.

Interview signal:

> Analytics is asynchronous because redirect availability is more important than exact real-time click counts.

### Abuse / Safety Service

Why it exists:

- URL shorteners are often abused for phishing, malware, and spam.
- Unsafe links damage users and platform reputation.

Responsibilities:

- Validate URL scheme and deny obvious unsafe destinations at create time.
- Run deeper async scans after creation.
- Block or disable unsafe mappings.
- Publish invalidation events for blocked codes.
- Rate-limit suspicious creators and traffic patterns.

Failure behavior:

- Synchronous deny-list unavailable: fail closed for high-risk create flows, or accept with restricted state by policy.
- Async scan later finds abuse: mark mapping blocked and purge/invalidate caches.
- False positive: support admin review/unblock flow.

Interview signal:

> Abuse checks belong in both create-time validation and async safety workflows, but they should not make every redirect slow.

### Expiry, Cleanup, and Observability

Why it exists:

- Expired/deleted links need predictable behavior.
- Operators need to detect hot links, stale caches, abuse, and redirect failures quickly.

Responsibilities:

- Mark expired links and keep redirect service checking `expiresAt` on every lookup.
- Keep tombstones briefly after delete to avoid cache resurrection.
- Run cleanup jobs for old mappings and analytics retention.
- Track redirect p50/p95/p99, cache hit ratio, DB latency, hot-code QPS, and analytics lag.

Interview signal:

> Cleanup jobs are for storage hygiene; redirect correctness must be enforced during lookup through status and expiry checks.

### How The Components Work Together

Create path:

```text
Gateway -> URL Management Service -> Abuse validation -> Code Generator/Alias reservation -> Metadata Store -> Cache warming/invalidation event -> response
```

Redirect path:

```text
Edge/CDN -> Redirect Service -> local/regional cache -> Metadata Store on miss -> validate status/expiry -> 302 response -> async click event
```

One-stop interview answer:

> I split URL shortener into a correctness-focused create plane and a latency-focused redirect plane. Creation handles validation, idempotency, code generation, and atomic alias reservation. Redirect is stateless, cache-heavy, and does only lookup, status/expiry validation, redirect response, and async click event publishing. Analytics, abuse scanning, cleanup, and dashboards are isolated from the hot path.

## 2.4 Data Layer

### Main Mapping Table

Logical schema:

```sql
CREATE TABLE url_mapping (
    code VARCHAR(16) PRIMARY KEY,
    long_url TEXT NOT NULL,
    user_id VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    custom_alias BOOLEAN NOT NULL,
    url_hash VARCHAR(64),
    version BIGINT NOT NULL
);
```

NoSQL shape:

```json
{
  "pk": "code#abc123",
  "longUrl": "https://example.com/products/123",
  "userId": "user-7",
  "status": "ACTIVE",
  "createdAt": "2026-06-17T10:15:00Z",
  "expiresAt": "2027-01-01T00:00:00Z",
  "customAlias": false,
  "version": 1
}
```

### User Links Index

If users need to list their links:

```text
Partition key: user_id
Sort key: created_at desc + code
Fields: code, long_url_preview, status, expires_at
```

Keep this separate from redirect lookup so the redirect path remains simple.

### Click Event Stream

Do not write every click into the OLTP URL mapping table.

Use an append-only event stream:

```json
{
  "code": "abc123",
  "timestamp": "2026-06-17T10:20:00Z",
  "ipHash": "hash",
  "country": "IN",
  "referrer": "linkedin.com",
  "userAgentFamily": "Chrome",
  "requestId": "req-123"
}
```

Analytics storage:

- Kafka/Pulsar for ingestion.
- Flink/Spark streaming for aggregation.
- ClickHouse/Druid/BigQuery/Snowflake for queryable analytics.
- HyperLogLog or similar approximate structure for unique visitors at massive scale.

### Indexing

Required:

- Primary key on `code`.
- Unique reservation for custom alias.

Useful secondary indexes:

- `user_id + created_at` for dashboard list.
- `expires_at` for cleanup jobs.
- `url_hash` only if product wants deduplication.

Avoid:

- Heavy secondary indexes on redirect hot path.
- Updating click count inside the mapping row for every redirect.

### Sharding

Recommended:

- Shard mapping data by hash of `code`.
- Keep generated codes evenly distributed, or hash them before choosing partition.
- Avoid range-sharding sequential IDs directly because recent writes can concentrate on one partition.

### Replication

- Multi-AZ synchronous or quorum replication inside a region.
- Cross-region async replication for disaster recovery and regional reads.
- For newly created links, route immediate redirects to the write region or use read-your-write cache warming.

## 2.5 Scalability

### Horizontal Scaling

- Redirect service is stateless, so scale by adding instances.
- URL management service is stateless except for idempotency and DB writes.
- Cache cluster scales by sharding keys.
- Metadata store scales by partitioning `code`.

### Stateless Services

Keep request state out of application servers:

- Store mappings in metadata store.
- Store idempotency records in durable store/cache with TTL.
- Store analytics in event pipeline.
- Store sessions/tokens outside app instance.

### Partitioning

Partition by code hash:

```text
partition = hash(code) % partition_count
```

At very large scale, avoid direct `mod node_count` routing. Use fixed logical partitions or consistent hashing so adding nodes does not move most data.

### Hot Key Handling

A viral short link is a hot key. Do not solve it by only adding database shards.

Better approach:

- Cache at CDN/edge.
- Cache in local process memory.
- Cache in regional Redis.
- Use request coalescing on cache miss.
- Sample or aggregate analytics for the hot code.
- Split analytics stream key for hot codes, such as `code + minute_bucket + random_bucket`.

## 2.6 Performance

### Redirect Path Optimizations

- Prefer memory/cache lookup before database.
- Keep mapping payload small.
- Use short DB timeout and fallback where safe.
- Return redirect without waiting for analytics.
- Cache negative results briefly to prevent repeated DB misses for random codes.

Example latency budget:

| Step | Target |
|---|---:|
| Edge routing | 5-20 ms |
| Local/Redis cache lookup | 1-5 ms |
| Metadata store lookup on miss | 5-30 ms inside region |
| Redirect service processing | under 5 ms |
| Async event enqueue | non-blocking or under 2 ms budget |

### Caching Strategy

| Cache layer | What it stores | TTL |
|---|---|---:|
| CDN/edge | popular redirect response | 30s-10m depending on mutability |
| Local LRU | hottest code mappings | seconds-minutes |
| Redis/Memcached | code to mapping | minutes-hours |
| Negative cache | missing/expired codes | 10s-60s |

Cache invalidation:

- On disable/delete, update DB first.
- Publish invalidation event.
- Purge CDN for custom domain/short URL if possible.
- Keep redirect cache TTL short enough to bound stale redirects.

### Read-Heavy vs Write-Heavy

URL shortener is usually read-heavy:

- Redirects dominate.
- Analytics writes are high volume but async.
- Link creation is much lower QPS than redirect.

Optimize separately:

- Redirect path: cache, availability, p99 latency.
- Create path: uniqueness, validation, idempotency.
- Analytics path: throughput, backpressure, approximate counts.

## 2.7 Async Systems

Use message queues/streams for:

- Click events.
- Analytics aggregation.
- Abuse/malware scanning.
- Cache invalidation.
- Expiration cleanup.
- Audit logs.

Click event flow:

```text
Redirect Service
  -> non-blocking event publisher
  -> Kafka topic: click-events
  -> stream processor aggregates counts
  -> OLAP store / dashboard
```

Important choices:

- At-least-once delivery is acceptable for raw click events if aggregation is idempotent or approximate.
- Analytics should not block redirects.
- Use DLQ for malformed events.
- Use sampling or aggregation for extremely hot links.

## 2.8 Reliability

### Retry Mechanisms

- Create API: safe retries with idempotency key.
- DB writes: retry only when operation is idempotent or conditional.
- Analytics publish: retry with bounded queue; drop/sample if redirect path is at risk.
- Redirect DB reads: short retry budget, then fail fast or fallback to stale cache.

### Circuit Breakers

Use circuit breakers around:

- Analytics publisher.
- Abuse-scoring service.
- Remote metadata region.
- Cache cluster if it becomes slow.

Critical rule:

> Non-critical dependencies must not take down the redirect path.

### Failover Strategy

- Active-active reads across regions for cached/popular links.
- Writes can be single-region per tenant or active-active with global ID generation.
- Metadata replicated cross-region.
- If one region fails, global load balancer routes redirects to healthy regions.
- For recent writes not replicated yet, return graceful error or route to original write region if available.

### Expiry And Deletion Reliability

Expiry behavior:

- Redirect service checks `expires_at` on every cache/store result.
- Cleanup job deletes or marks expired links later.
- Do not rely only on cleanup job for correctness.

Deletion behavior:

- Mark as `DISABLED` first.
- Invalidate caches.
- Keep tombstone briefly to avoid stale re-creation or cache resurrection.

## 2.9 Tradeoffs

| Decision | Option A | Option B | Staff-level trade-off |
|---|---|---|---|
| Redirect status | `301` | `302` | `301` improves caching but makes edits/deletes harder; `302` preserves control and analytics |
| Storage | SQL | NoSQL KV/wide-column | SQL is simpler; NoSQL scales key lookups better |
| Code generation | random code | distributed ID + Base62 | random is simple but collision retries exist; ID avoids collision but needs ID system |
| Analytics | exact per-click sync write | async approximate stream | exact sync hurts redirects; async scales and protects latency |
| Cache TTL | long | short | long reduces load but increases stale redirects; short improves control but increases origin traffic |
| Consistency | strong global | regional eventual | strong global simplifies correctness but hurts latency/availability |

Interview answer:

> I would make redirect highly available and cache-heavy, and accept eventual consistency for analytics. I would require strong uniqueness for code creation and custom alias reservation, but I would not make click analytics part of the synchronous redirect transaction.

---

# 3. Low-Level Design

LLD goal:

> Turn the HLD into small objects with clear ownership. For URL shortener, keep code creation, mapping persistence, redirect resolution, abuse checks, cache behavior, and analytics publishing separate.

Simple rule:

- Entities own state and invariants.
- Services coordinate use cases.
- Repositories hide storage details.
- Publishers/adapters hide external systems.

Starter map:

| LLD question | URL shortener answer |
|---|---|
| What is the core object? | `UrlMapping`, because it represents one short code and its destination |
| What must be unique? | `ShortCode.value` or custom alias |
| What is the hot method? | `RedirectService.resolve(code)` |
| What is the correctness-heavy method? | `UrlShorteningService.create(request)` |
| What should be async? | click analytics, audit, deep abuse scanning, cleanup |
| What must be atomic? | alias/code reservation with `saveIfAbsent` |

Beginner-friendly design order:

1. Model `UrlMapping` and `ShortCode` first.
2. Design `CodeGenerator` as an interface so the strategy can change.
3. Design `UrlRepository` with atomic save and lookup by code.
4. Build `UrlShorteningService` for create flow.
5. Build `RedirectService` for read flow and keep analytics async.
6. Add edge cases: collisions, stale cache, expired links, duplicate retries.

Interview sentence:

> In LLD, I will keep create and redirect flows separate: create needs validation, idempotency, and atomic code reservation; redirect needs fast lookup, expiry/status validation, and async analytics.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `User` | creator identity, plan, status | disabled users cannot create new links |
| `UrlMapping` | `code -> longUrl` metadata | inactive/expired mappings must not redirect |
| `ShortCode` | short-code value and validation | allowed charset/length only |
| `RedirectRequest` | request context such as IP, user agent, referrer | request data is never trusted blindly |
| `ClickEvent` | analytics payload | analytics must not block redirect success |
| `IdempotencyRecord` | retry key to canonical code | same retry key returns same mapping |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `UrlShorteningService` | validate request, call abuse checker, reserve code, save mapping | handle HTTP/cache/Kafka details |
| `RedirectService` | resolve code, verify active mapping, return destination | run heavy analytics synchronously |
| `CodeGenerator` | produce candidate short codes | write mappings directly |
| `UrlRepository` | atomic save/find/disable operations | decide business validation rules |
| `AnalyticsPublisher` | emit click events asynchronously | affect redirect correctness |
| `AbuseChecker` | reject unsafe destinations | generate short codes |

Core flow:

```text
Create: validate URL -> check abuse -> generate/reserve code -> save mapping -> return short URL
Redirect: lookup code -> verify active/not expired -> publish click event async -> return 302
```

## 3.2 OOP Fundamentals

Encapsulation:

- `ShortCode` validates allowed characters and length.
- `UrlMapping` owns expiry/status checks.
- Repository hides storage details.

Abstraction:

- `CodeGenerator` interface hides generation strategy.
- `UrlRepository` interface hides SQL/NoSQL/in-memory storage.
- `AnalyticsPublisher` interface hides Kafka or mock implementation.

Polymorphism:

- Different code generators: random, counter, Snowflake, pre-generated pool.
- Different abuse checkers: no-op, deny-list, external scanner.

Composition over inheritance:

- `UrlShorteningService` composes `CodeGenerator`, `UrlRepository`, and `AbuseChecker`.
- `RedirectService` composes `UrlRepository`, cache, clock, and analytics publisher.

## 3.3 SOLID Principles

| Principle | URL shortener application |
|---|---|
| Single Responsibility | `CodeGenerator` only generates codes; repository only stores mappings |
| Open/Closed | add Snowflake generator without changing service logic |
| Liskov Substitution | any `UrlRepository` implementation should behave consistently |
| Interface Segregation | redirect path depends on read methods, not admin methods |
| Dependency Inversion | services depend on interfaces, not concrete Redis/Kafka clients |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | `CodeGenerator` | switch between random, counter, Snowflake, pre-generated pool |
| Factory | create generator/repository based on config | keeps wiring separate from business logic |
| Builder | create `UrlMapping` or request object with optional expiry/custom alias | avoids constructor overloads |
| Observer/Event Publisher | click analytics and audit events | keeps redirect path decoupled from analytics consumers |
| Proxy | cache wrapper around repository | adds caching without changing repository contract |

Singleton note:

- Avoid global business singletons.
- Connection pools may be singleton-like in infrastructure wiring, but do not hide them inside domain code.

## 3.5 UML / Diagrams

### Class Diagram

```text
+----------------------+        +--------------------+
| UrlShorteningService |-------> | CodeGenerator      |
| - repository         |        | +nextCode()        |
| - codeGenerator      |        +--------------------+
| - abuseChecker       |
| +create(request)     |        +--------------------+
+----------+-----------+------> | UrlRepository      |
           |                    | +saveIfAbsent()    |
           |                    | +findByCode()      |
           v                    +--------------------+
+----------------------+
| UrlMapping           |
| code                 |        +--------------------+
| longUrl              |        | RedirectService    |
| expiresAt            |<-------| +resolve(code)     |
| status               |        +---------+----------+
| +isActive(now)       |                  |
+----------------------+                  v
                              +----------------------+
                              | AnalyticsPublisher   |
                              | +publish(event)      |
                              +----------------------+
```

### Create Sequence

```text
Client
  -> UrlShorteningService.create(request)
  -> AbuseChecker.validate(longUrl)
  -> CodeGenerator.nextCode()
  -> UrlRepository.saveIfAbsent(mapping)
  -> Cache.put(code, mapping)
  <- shortUrl
```

### Redirect Sequence

```text
Browser
  -> RedirectService.resolve(code)
  -> Cache.get(code)
  -> UrlRepository.findByCode(code) on miss
  -> UrlMapping.isActive(now)
  -> AnalyticsPublisher.publish(clickEvent) async
  <- 302 Location: longUrl
```

## 3.6 Class Design

Important interfaces:

```java
interface CodeGenerator {
    String nextCode();
}

interface UrlRepository {
    boolean saveIfAbsent(UrlMapping mapping);
    Optional<UrlMapping> findByCode(String code);
    boolean markDisabled(String code);
}

interface AnalyticsPublisher {
    void publish(ClickEvent event);
}

interface AbuseChecker {
    void validate(String longUrl);
}
```

Design choices:

- Keep `UrlShorteningService` independent of HTTP framework.
- Keep `RedirectService` independent of Kafka/Redis implementation.
- Use immutable `UrlMapping` records where possible.
- Make alias reservation atomic with `saveIfAbsent`.

## 3.7 Data Handling

In-memory machine-coding version:

- `ConcurrentHashMap<String, UrlMapping>` for code lookup.
- `AtomicLong` for unique numeric IDs.
- Optional `ConcurrentHashMap<String, String>` for idempotency key to code.
- Optional priority queue for expiry cleanup, but redirect must still check expiry.

Production version:

- Distributed metadata store keyed by code.
- Redis/local cache for read performance.
- Kafka for click events.
- OLAP store for analytics.

## 3.8 Edge Cases

| Case | Handling |
|---|---|
| blank/invalid URL | reject before code generation |
| unsafe scheme like `javascript:` | reject in validation/abuse checker |
| custom alias collision | atomic `saveIfAbsent`, return conflict |
| duplicate create retry | idempotency key returns same short code |
| generated-code collision | retry bounded times, alert if collision rate rises |
| expired/disabled code | return `404`/`410`, do not redirect |
| stale cache after disable | use short TTL + delete/invalidate on admin changes |
| analytics publisher down | drop to buffer/DLQ; redirect path still succeeds |

Interview rule:

> URL shortener LLD is mostly about atomic alias reservation, safe redirect resolution, and keeping analytics/abuse concerns outside the hot redirect path.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package structure:

```text
urlshortener/
  domain/
    UrlMapping.java
    UrlStatus.java
    ClickEvent.java
  service/
    UrlShorteningService.java
    RedirectService.java
  port/
    UrlRepository.java
    CodeGenerator.java
    AnalyticsPublisher.java
    AbuseChecker.java
  adapter/
    InMemoryUrlRepository.java
    Base62CounterCodeGenerator.java
    NoopAnalyticsPublisher.java
  app/
    UrlShortenerDemo.java
```

Machine-coding goal:

- Implement create and redirect correctly.
- Use thread-safe storage.
- Handle custom alias conflict.
- Handle expiry.
- Keep analytics decoupled.

## 4.2 Core Logic Implementation

Focused Java implementation:

```java
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

enum UrlStatus {
    ACTIVE,
    DISABLED
}

record UrlMapping(
        String code,
        String longUrl,
        String userId,
        Instant createdAt,
        Instant expiresAt,
        UrlStatus status
) {
    boolean isActiveAt(Instant now) {
        boolean notExpired = expiresAt == null || expiresAt.isAfter(now);
        return status == UrlStatus.ACTIVE && notExpired;
    }
}

record CreateUrlRequest(String longUrl, String userId, String customAlias, Instant expiresAt) {
}

record RedirectResult(String longUrl, int httpStatus) {
}

record ClickEvent(String code, Instant clickedAt) {
}

interface CodeGenerator {
    String nextCode();
}

interface UrlRepository {
    boolean saveIfAbsent(UrlMapping mapping);
    Optional<UrlMapping> findByCode(String code);
    boolean markDisabled(String code);
}

interface AnalyticsPublisher {
    void publish(ClickEvent event);
}

final class Base62CounterCodeGenerator implements CodeGenerator {
    private static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final AtomicLong counter;

    Base62CounterCodeGenerator(long initialValue) {
        this.counter = new AtomicLong(initialValue);
    }

    @Override
    public String nextCode() {
        long number = counter.incrementAndGet();
        StringBuilder encoded = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % ALPHABET.length);
            encoded.append(ALPHABET[remainder]);
            number = number / ALPHABET.length;
        }
        return encoded.reverse().toString();
    }
}

final class InMemoryUrlRepository implements UrlRepository {
    private final ConcurrentHashMap<String, UrlMapping> mappings = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(UrlMapping mapping) {
        return mappings.putIfAbsent(mapping.code(), mapping) == null;
    }

    @Override
    public Optional<UrlMapping> findByCode(String code) {
        return Optional.ofNullable(mappings.get(code));
    }

    @Override
    public boolean markDisabled(String code) {
        UrlMapping current = mappings.get(code);
        if (current == null) {
            return false;
        }
        UrlMapping disabled = new UrlMapping(
                current.code(),
                current.longUrl(),
                current.userId(),
                current.createdAt(),
                current.expiresAt(),
                UrlStatus.DISABLED
        );
        return mappings.replace(code, current, disabled);
    }
}

final class UrlShorteningService {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final UrlRepository repository;
    private final CodeGenerator codeGenerator;
    private final Clock clock;

    UrlShorteningService(UrlRepository repository, CodeGenerator codeGenerator, Clock clock) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    public UrlMapping create(CreateUrlRequest request) {
        validateLongUrl(request.longUrl());
        if (request.expiresAt() != null && !request.expiresAt().isAfter(clock.instant())) {
            throw new IllegalArgumentException("Expiry must be in the future");
        }

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            return reserve(request.customAlias(), request);
        }

        for (int attempt = 0; attempt < 5; attempt++) {
            UrlMapping mapping = buildMapping(codeGenerator.nextCode(), request);
            if (repository.saveIfAbsent(mapping)) {
                return mapping;
            }
        }
        throw new IllegalStateException("Unable to reserve unique short code");
    }

    private UrlMapping reserve(String code, CreateUrlRequest request) {
        validateCode(code);
        UrlMapping mapping = buildMapping(code, request);
        if (!repository.saveIfAbsent(mapping)) {
            throw new IllegalArgumentException("Short code already exists");
        }
        return mapping;
    }

    private UrlMapping buildMapping(String code, CreateUrlRequest request) {
        return new UrlMapping(code, request.longUrl(), request.userId(), clock.instant(), request.expiresAt(), UrlStatus.ACTIVE);
    }

    private void validateLongUrl(String longUrl) {
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
        URI uri = URI.create(longUrl);
        if (!ALLOWED_SCHEMES.contains(uri.getScheme())) {
            throw new IllegalArgumentException("Only http and https URLs are supported");
        }
    }

    private void validateCode(String code) {
        if (!code.matches("[A-Za-z0-9_-]{3,32}")) {
            throw new IllegalArgumentException("Invalid custom alias");
        }
    }
}

final class RedirectService {
    private final UrlRepository repository;
    private final AnalyticsPublisher analyticsPublisher;
    private final Clock clock;

    RedirectService(UrlRepository repository, AnalyticsPublisher analyticsPublisher, Clock clock) {
        this.repository = repository;
        this.analyticsPublisher = analyticsPublisher;
        this.clock = clock;
    }

    public RedirectResult resolve(String code) {
        UrlMapping mapping = repository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found"));
        if (!mapping.isActiveAt(clock.instant())) {
            throw new IllegalStateException("Short URL is expired or disabled");
        }
        analyticsPublisher.publish(new ClickEvent(code, clock.instant()));
        return new RedirectResult(mapping.longUrl(), 302);
    }
}

final class NoopAnalyticsPublisher implements AnalyticsPublisher {
    @Override
    public void publish(ClickEvent event) {
        // Intentionally non-blocking in the demo. Production would enqueue to Kafka or another stream.
    }
}
```

## 4.3 Data Structures

Machine-coding structures:

| Structure | Use |
|---|---|
| `ConcurrentHashMap<String, UrlMapping>` | thread-safe code lookup |
| `AtomicLong` | simple unique ID source |
| `Set<String>` | allowed schemes or blocked domains |
| `Queue<ClickEvent>` | async analytics buffer in coding round |
| `PriorityQueue<ExpiryItem>` | optional cleanup by expiration time |

Production structures:

- Distributed KV store for mapping.
- Redis cluster for cache.
- Kafka topic for click events.
- OLAP tables for analytics.
- Bloom filter for quick known-missing or known-blocked checks if needed.

## 4.4 Concurrency

High-signal concurrency issues:

- Custom alias race: two requests try to reserve the same alias.
- Generated code collision: retry safely with conditional insert.
- Idempotency race: same client retries create request.
- Disable vs redirect race: cache may serve stale mapping briefly.

Machine-coding handling:

- Use `ConcurrentHashMap.putIfAbsent` for atomic alias reservation.
- Use `AtomicLong` for generator state.
- Use immutable `UrlMapping` to avoid shared mutable bugs.
- Use `replace(current, updated)` for safe status update.

Production handling:

- Use conditional writes such as `INSERT IF NOT EXISTS` or DynamoDB conditional put.
- Use database-generated unique constraints or transactional reservation table for aliases.
- Use idempotency table keyed by client key and user.
- Use cache invalidation plus short TTL for delete/disable.

## 4.5 Performance Optimization

Time complexity:

- Create generated URL: average `O(1)` for code generation and conditional insert.
- Create custom alias: average `O(1)` conditional insert.
- Redirect lookup: average `O(1)` cache/store lookup.
- Expiry cleanup with priority queue: `O(log n)` per expired item.

Space complexity:

- Mapping storage: `O(number_of_urls)`.
- Cache: bounded by configured memory.
- Analytics events: bounded by stream retention and aggregation policy.

Optimizations:

- Avoid DB update per click.
- Cache hot mappings aggressively.
- Use async analytics.
- Store compact metadata in redirect cache.
- Preallocate ID ranges per generator node if using central allocator.

## 4.6 Error Handling

Common errors:

| Error | API response |
|---|---|
| invalid long URL | `400 Bad Request` |
| custom alias already exists | `409 Conflict` |
| short code not found | `404 Not Found` |
| expired link | `410 Gone` |
| disabled/malicious link | `403 Forbidden` or branded warning page |
| rate limited create request | `429 Too Many Requests` |
| metadata store unavailable | `503 Service Unavailable` unless stale cache is safe |

Retry logic:

- Retry generated code collision a bounded number of times.
- Retry DB transient failures with exponential backoff.
- Do not retry indefinitely inside user request.
- Use idempotency for create requests.

## 4.7 Testing Thinking

Unit tests:

- Creates valid short URL.
- Rejects invalid scheme.
- Rejects duplicate custom alias.
- Redirect returns original long URL.
- Expired URL cannot redirect.
- Disabled URL cannot redirect.
- Generated code collision retries.

Concurrency tests:

- Many threads reserve same custom alias; exactly one succeeds.
- Many threads create generated links; all codes are unique.
- Disable and redirect race does not corrupt state.

Integration tests:

- Create then redirect.
- Create then analytics event published.
- Delete then cache invalidated.
- Malware block changes redirect behavior.

Load tests:

- Redirect cache hit path.
- Redirect cache miss path.
- Hot link viral traffic.
- Create API with custom alias conflicts.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Types Of Spikes

| Spike type | Example | Main risk |
|---|---|---|
| Viral hot link | celebrity posts one short URL | one code becomes hot key |
| DDoS/random-code scan | bots hit random codes | cache misses and DB pressure |
| Bulk creation abuse | spammer creates millions of links | code/storage abuse and malware risk |
| Analytics spike | campaign produces huge click volume | stream partitions and OLAP ingestion overload |
| Regional failover spike | one region fails and traffic moves | healthy regions overload |

## 5.2 Immediate Spike Response

For redirect spikes:

1. Serve hot redirects from CDN/edge.
2. Raise cache TTL temporarily for safe active links.
3. Enable local LRU cache in redirect service.
4. Use request coalescing so one cache miss does not trigger many DB reads.
5. Shed or sample analytics before hurting redirects.
6. Autoscale redirect service and cache nodes.
7. Rate limit suspicious IPs/user agents, not the hot legitimate link itself.

For create spikes:

1. Apply token bucket per user/API key/IP.
2. Require authentication for high-volume creation.
3. Queue or reject bulk creation above tier quota.
4. Use preallocated ID ranges so code generation does not bottleneck.
5. Run malware scanning and trust scoring.

For random-code scans:

1. Negative-cache missing codes briefly.
2. Rate limit by source and ASN.
3. Use WAF/bot detection.
4. Avoid expensive DB lookups for obviously invalid code format.

## 5.3 Hot Link Strategy

Hot link problem:

```text
One code receives millions of QPS.
DB sharding by code does not help because all reads target one key.
```

Correct strategy:

- Push mapping to CDN/edge.
- Store mapping in every regional cache.
- Store mapping in local process cache.
- Use stale-while-revalidate where safe.
- Keep origin DB out of the hot path.
- Split analytics processing for that code.

Analytics hot partition fix:

```text
Bad partition key:
  key = code

Better for hot links:
  key = code + minute_bucket + random_bucket
```

This spreads analytics writes while preserving aggregate ability.

## 5.4 Degradation Policy

Protect in this order:

1. Redirect availability.
2. Create API for trusted users.
3. Link management API.
4. Analytics freshness.
5. Exact analytics accuracy.

Allowed degradation:

- Drop or sample analytics events during extreme overload.
- Delay analytics dashboards.
- Temporarily disable anonymous bulk creation.
- Use cached/stale mapping briefly for known-safe active links.

Not allowed:

- Redirect malicious or disabled links for long periods.
- Create duplicate custom aliases.
- Let analytics failure block redirects.

## 5.5 Spike Interview Answer

> I would first identify whether the spike is a hot-link spike, random-code scan, create abuse, or regional failover. For hot redirects, the fix is edge caching and local/regional cache replication, not just adding DB shards. I would protect the redirect path by shedding analytics, rate limiting abusive sources, using request coalescing on cache misses, and autoscaling stateless redirect services. For billion-user scale, I would make analytics async and approximate where needed, because redirect latency is the product's core promise.

---

# 6. Scaling To A Billion Users

## 6.1 Code Space

Use Base62:

- 6 chars gives about 56.8B possibilities, but collision margin and reserved/custom codes make it tight for very long-lived global scale.
- 7 chars gives about 3.5T possibilities and is a strong default.
- 8 chars gives about 218T possibilities for extreme longevity.

Recommendation:

- Start at 7 chars for generated codes at large scale.
- Allow custom aliases with separate validation and reservation.
- Reserve some prefixes for internal/product use if needed.

## 6.2 Global Architecture

For billion users:

```text
Global DNS / Anycast / CDN
  -> nearest edge
  -> regional redirect service
  -> regional cache
  -> local regional replica of metadata store
  -> async global replication
```

Create path options:

| Option | Design | Trade-off |
|---|---|---|
| single write region | all creates go to one region | simple uniqueness, worse global latency and failover |
| user-home region | creates go to user's assigned home region | balanced, needs routing metadata |
| active-active writes | every region can create | best latency, hardest uniqueness/conflict handling |

Recommended:

- Generated codes can be active-active if ID generation is globally unique.
- Custom aliases may need strong global reservation, often through a home/leader region or strongly consistent alias service.

## 6.3 Storage Scaling

Mapping store:

- Key by `code`.
- Use large-scale KV/wide-column store such as DynamoDB, Cassandra, Bigtable, or similar.
- Use fixed logical partitions and rebalance gradually.
- Replicate across AZs and regions.

Analytics store:

- Do not keep click analytics in the mapping row.
- Stream raw clicks into Kafka/Pulsar.
- Aggregate by code, time bucket, country, referrer.
- Keep raw events for limited retention.
- Keep aggregates longer.

## 6.4 Cache Scaling

Cache hierarchy:

```text
CDN/edge cache
  -> redirect service local LRU
  -> regional Redis/Memcached
  -> metadata store
```

At billion-user scale:

- Cache hit ratio is the biggest cost/latency lever.
- Hot links should be served from edge.
- Expired/disabled links need short TTL or active purge.
- Use regional caches to avoid cross-region reads.

## 6.5 Multi-Region Consistency

Generated link creation:

- Return short URL after durable write in write region.
- Warm local cache immediately.
- Replicate asynchronously to other regions.
- If user clicks immediately from another region, route to write region or use global cache propagation.

Custom alias creation:

- Requires stronger uniqueness.
- Use conditional write in global strongly consistent store or route alias ownership to one region.
- Return `409 Conflict` on duplicate.

Redirect consistency:

- Most redirects can tolerate eventual replication once link is created.
- Deletes/abuse disables need faster propagation and cache purge.
- Tombstones prevent disabled code from being accidentally reused.

## 6.6 Billion-User Capacity Plan

| Layer | Scaling plan |
|---|---|
| Edge | Anycast/CDN, WAF, bot filtering, redirect caching |
| Redirect service | stateless autoscaling across regions |
| Cache | local LRU + regional Redis + CDN hot key caching |
| Metadata | sharded NoSQL KV store, multi-AZ, cross-region replication |
| ID generation | distributed ID blocks or Snowflake-style generator |
| Analytics | Kafka/Pulsar, stream aggregation, OLAP, sampling for extreme hot links |
| Abuse | rate limits, deny lists, malware scanning, domain reputation |
| Operations | SLOs, dashboards, failover drills, load tests, cache purge tooling |

## 6.7 Billion-User Interview Answer

> For a billion users, I would separate the read path, write path, and analytics path. Redirects would be served globally through CDN/edge, local caches, and regional redirect services backed by a sharded key-value metadata store. Creates would use distributed ID generation with Base62 codes, while custom aliases would use strong conditional reservation. Click analytics would be async through a stream because exact synchronous analytics would destroy redirect latency. Hot links are handled through edge caching and analytics partition splitting, not by only adding database shards.

---

## Gold-Level Interview Traps

Watch for these mistakes when presenting this design:

- Designing only the happy path and ignoring retries, timeouts, and partial failure.
- Skipping the data model or not naming the source of truth.
- Using caches, queues, or async workers without explaining consistency impact.
- Scaling every component equally instead of finding the real bottleneck.
- Forgetting idempotency, deduplication, ordering, or backpressure where the workflow needs it.
- Giving a complex final design without first stating the simple MVP.

# 7. Final Interview Playbook

Use this answer flow:

```text
I will clarify requirements first: create short URL, redirect, custom alias, expiry, analytics, abuse controls.
The system is read-heavy, so I will optimize redirect latency and availability.
For HLD, I use CDN/edge, API gateway, redirect service, URL management service, cache, metadata store, and async analytics stream.
For APIs, create is idempotent and redirect returns 301/302 based on product policy.
For data, code is the primary key; analytics is append-only and not updated synchronously in the mapping row.
For scale, services are stateless, mappings are sharded by code hash, and hot links are cached at edge.
For reliability, analytics and abuse systems cannot block redirect; cache invalidation handles delete/disable.
For LLD, I model UrlMapping, CodeGenerator, UrlRepository, UrlShorteningService, and RedirectService.
For spikes, I distinguish viral hot links from random scans and creation abuse, then protect redirect first.
```

---

# 8. Fast Recall Rules

- URL shortener is read-heavy; redirect path is the product.
- Do not synchronously update click count in the mapping row.
- Use `code -> longUrl` as the primary lookup.
- Use Base62 for compact URL-safe codes.
- Generated codes need global uniqueness; custom aliases need atomic reservation.
- Cache at CDN, local process, and regional cache.
- Hot link is a hot key; solve with edge/cache, not just more DB shards.
- Use async streams for analytics.
- `301` caches better but is harder to change; `302` gives more control.
- Expiry must be checked during redirect, not only by cleanup jobs.
- Delete/disable needs cache invalidation and often tombstones.
- Random-code scans need format validation, negative cache, WAF, and rate limits.
- Billion-user scale needs multi-region reads, sharded metadata, distributed IDs, and async analytics.