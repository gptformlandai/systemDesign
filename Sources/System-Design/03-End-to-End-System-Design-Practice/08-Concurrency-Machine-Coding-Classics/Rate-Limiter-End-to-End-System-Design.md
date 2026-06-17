# Rate Limiter - End-to-End System Design

> Goal: design a production-grade rate limiter that protects public APIs, internal services, tenants, users, IPs, and expensive operations while balancing latency, accuracy, availability, and fairness.

---

## How To Use This File

- Use this when the interview problem says rate limiter, throttling, quota system, API protection, abuse prevention, or tenant fairness.
- Start simple with a single-node limiter, then evolve to distributed gateways, sharded counters, local caches, and multi-region trade-offs.
- Keep one idea sharp: a rate limiter is a fast decision system on the request path, so every design choice must respect latency and failure behavior.
- In interviews, explain the key, the policy, the algorithm, the storage, and the fallback.

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

| Layer | Interview signal | Rate limiter focus |
|---|---|---|
| Problem understanding | Can define limit scope | user, IP, API key, tenant, route, region, endpoint |
| HLD | Can design low-latency enforcement | gateway filter, limiter service, policy store, counter store, metrics |
| LLD | Can model algorithms cleanly | `RateLimitKey`, `Policy`, `BucketState`, `RateLimitDecision`, `RateLimitStrategy` |
| Machine coding | Can implement critical algorithm | token bucket, fixed window, sliding window, retry-after |
| Traffic spikes | Can protect dependencies | local fallback, fail-open/fail-closed, hot key handling, overload mode |
| Billion users | Can reason globally | sharding, regional budgets, approximate limits, active-active trade-offs |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Limit how many requests a caller can make in a time window.
- Support multiple limiter keys: IP, user ID, API key, tenant ID, route, endpoint, device, and region.
- Support different policies per endpoint or tenant tier.
- Return allow/deny decision quickly before request reaches backend services.
- Return useful headers such as remaining quota, reset time, and retry-after.
- Support common algorithms such as fixed window, sliding window, token bucket, and leaky bucket.
- Support admin policy updates without redeploying gateway code.
- Emit metrics and logs for allowed, blocked, and degraded decisions.

Optional requirements to clarify:

- Is this for public API gateway, internal service-to-service traffic, or both?
- Do limits need to be exact or approximate?
- Is multi-region active-active required?
- Should the limiter fail open or fail closed when counter storage is unavailable?
- Do we need monthly quotas in addition to per-second/per-minute limits?
- Do we need hierarchical limits, such as tenant plus user plus endpoint?

Out of scope unless asked:

- Full bot detection and fraud scoring.
- Full WAF design.
- Billing system for usage-based charging.
- Complete DDoS protection system.

## 1.2 Non-Functional Requirements

Decision path:

- Very low latency, often single-digit milliseconds at the gateway.
- High availability because every protected request depends on the limiter.
- Predictable behavior during counter-store failures.
- Safe degradation when exact global state is unavailable.

Correctness:

- Prevent obvious quota abuse.
- Keep per-key decisions reasonably fair across gateway nodes.
- Avoid double counting on retries where possible.
- Accept approximate global enforcement if exact enforcement would harm availability.

Operations:

- Support fast policy rollout and rollback.
- Provide observability by key, route, tenant, algorithm, region, and decision.
- Avoid the rate limiter becoming the bottleneck it is meant to prevent.

## 1.3 Constraints

- Rate limiting runs on the request hot path.
- Global exact counters are expensive at high QPS.
- Popular tenants or attackers can create hot keys.
- Shared counter stores can become dependencies with their own failure modes.
- Multi-region active-active introduces quota drift.
- Different endpoints have different cost profiles.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Total request rate | 5M requests/sec globally |
| Gateway nodes | 10,000 globally |
| Active rate-limit keys/day | 500M |
| Policies | 100K active policies |
| Decision latency target | p99 under 5 ms inside gateway |
| Counter TTL | seconds to days depending on policy |
| Hot tenant QPS | 100K+ requests/sec |

Back-of-the-envelope:

- If every request performs a remote counter update, the counter store must handle millions of writes/sec.
- A policy with 60-second fixed windows needs counter TTL slightly above 60 seconds.
- Token bucket state is small, usually key, token count, and last refill timestamp.
- Multi-layer limits reduce pressure: edge coarse limits, gateway tenant limits, service-specific business limits.

## 1.5 Clarifying Questions To Ask

- What key should be limited: IP, user, API key, tenant, route, or composite key?
- What algorithm is expected: fixed window, sliding window, token bucket, or leaky bucket?
- How strict must limits be across multiple gateway nodes?
- Should the limiter allow bursts?
- What should happen if the limiter backend is down?
- Do premium tenants get higher quotas?
- Are quotas per region or global?
- Are limits only request-count based or also cost/weight based?

Strong interview framing:

> I will design the limiter as a fast request-path decision system. Gateway filters build a composite key, fetch a cached policy, update compact counter or token-bucket state, return allow/deny with retry headers, and emit async metrics. For global scale, I use sharding, local caches, approximate regional budgets, and clear fail-open/fail-closed rules.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Client
  -> Edge / CDN / WAF coarse limits
  -> API Gateway Rate Limit Filter
  -> Rate Limiter Service or embedded library
  -> Policy Cache
  -> Counter Store / Token Store
  -> Decision: allow or reject with 429
  -> Backend Service if allowed
```

Recommended architecture:

```text
                         +----------------------+
                         | Admin / Policy API   |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Policy Store         |
                         +----------+-----------+
                                    |
                                    v
Client                   +----------------------+
  |                      | Gateway Policy Cache |
  v                      +----------+-----------+
+-------------+                     |
| Edge / WAF  |                     v
+------+------+          +----------------------+
       |                 | Rate Limit Filter    |
       v                 +----------+-----------+
+-------------+                     |
| API Gateway |---------------------+
+------+------+                     v
       |                 +----------------------+
       |                 | Limiter Service      |
       |                 +----------+-----------+
       |                            |
       v                            v
+-------------+          +----------------------+
| Backend API |          | Counter Store        |
+-------------+          | Redis / KV / Shards  |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Metrics / Logs       |
                         +----------------------+
```

Deployment choices:

| Option | How it works | Pros | Cons |
|---|---|---|---|
| embedded gateway filter | limiter logic runs inside gateway | fastest, fewer hops | harder to update logic independently |
| central limiter service | gateway calls limiter service | reusable, centralized logic | extra network hop |
| local library plus shared store | service/gateway embeds algorithm | low latency | consistency varies by implementation |
| edge plus gateway plus app limits | layered limiters | defense in depth | more tuning complexity |

Recommended interview answer:

- Use gateway filter for hot-path decision.
- Use policy cache locally.
- Use sharded counter store for shared state.
- Use local emergency limits if shared store is unhealthy.
- Use service-level limiters for expensive domain actions.

## 2.2 APIs

### Check Limit

This may be an internal API if limiter is a service.

```http
POST /v1/rate-limit/check
Content-Type: application/json

{
  "key": "tenant:t1:route:/v1/search",
  "cost": 1,
  "requestId": "req_123",
  "timestampMs": 1781690400000
}
```

Response:

```json
{
  "allowed": true,
  "limit": 1000,
  "remaining": 742,
  "retryAfterMs": 0,
  "resetAtMs": 1781690460000,
  "policyId": "policy_gold_search"
}
```

### Gateway Reject Response

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1781690460
Retry-After: 17

{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Try again later."
}
```

### Create Or Update Policy

```http
PUT /v1/rate-limit/policies/{policyId}
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Gold tenant search limit",
  "keyTemplate": "tenant:{tenantId}:route:{route}",
  "algorithm": "TOKEN_BUCKET",
  "capacity": 2000,
  "refillRatePerSecond": 100,
  "scope": "REGIONAL",
  "failMode": "FAIL_OPEN_FOR_LOW_RISK"
}
```

## 2.3 Core Components

Think of Rate Limiter as a policy decision system plus a tiny state machine per key.

| Plane | Owns | Main goal |
|---|---|---|
| Enforcement plane | gateway filter, limiter client, decision response | block excess traffic quickly |
| Policy plane | limits, key templates, algorithm config | define who gets what quota |
| State plane | counters, buckets, windows | track usage safely |
| Observability plane | metrics, logs, audit, dashboards | tune and debug limits |
| Degraded-mode plane | local fallback, fail-open/closed rules | survive dependency failure |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| Edge/WAF | coarse IP/geo/bot limits | tenant quotas | attack volume |
| API Gateway Filter | request key extraction and enforcement | policy authoring | gateway QPS |
| Policy Service | policy CRUD, validation, rollout | request counters | policy count and rollout safety |
| Policy Cache | local copy of policies | source of truth | staleness and invalidation |
| Rate Limiter Service | algorithm execution and decisions | backend business logic | decision QPS |
| Counter Store | usage state per key/window/bucket | policy semantics | write QPS and hot keys |
| Metrics Pipeline | allowed/denied/degraded events | blocking decisions | event volume |
| Admin Console | policy management and audit | hot-path decisions | operator safety |

### Limit Keys

The key is the most important design choice.

| Key | Protects against | Risk |
|---|---|---|
| IP | anonymous abuse | NAT can group many real users |
| user ID | logged-in user abuse | attackers can create many accounts |
| API key | partner quota | stolen keys can still abuse |
| tenant ID | multi-tenant fairness | large tenant may need per-user sublimits |
| route/endpoint | expensive endpoint overload | route names must be stable |
| device ID | mobile/client abuse | spoofable if weak |
| composite key | precise protection | more counters and complexity |

Recommended key pattern:

```text
tenant:{tenantId}:user:{userId}:route:{routeGroup}:region:{region}
```

Use shorter/coarser keys at edge and more precise keys at gateway/service level.

### Algorithms

| Algorithm | How it works | Pros | Cons | Best for |
|---|---|---|---|---|
| fixed window | count requests in time bucket | simple, cheap | boundary burst problem | basic quotas |
| sliding window log | store timestamps | precise | high memory | low-QPS sensitive actions |
| sliding window counter | weighted current/previous buckets | approximate and cheap | not exact | public APIs |
| token bucket | refill tokens over time, spend per request | allows controlled bursts | needs timestamp state | APIs and tenants |
| leaky bucket | drain queue at fixed rate | smooth output | may add delay/drop | traffic shaping |

Default recommendation:

- Use token bucket for most API gateway limits.
- Use fixed window for simple daily/monthly quotas.
- Use sliding log only for sensitive low-volume actions like login attempts.
- Use leaky bucket when you need smoothing instead of burst allowance.

### Counter Store

Counter store requirements:

- atomic increment or compare-and-set,
- TTL support,
- high write throughput,
- low latency,
- sharding by rate-limit key,
- safe behavior when unavailable.

Common choices:

| Store | Fit | Notes |
|---|---|---|
| Redis cluster | common choice | atomic Lua scripts, TTL, fast |
| DynamoDB/Cassandra | high durability | higher latency than Redis |
| in-memory gateway local | fastest | approximate, per-node only |
| custom distributed counter service | high scale | more operational complexity |

### Distributed Enforcement

Exact global rate limiting is hard because many gateways see traffic simultaneously.

Approaches:

| Approach | How it works | Trade-off |
|---|---|---|
| shared central counter | all nodes update same logical state | accurate but store can bottleneck |
| sharded counter by key | key maps to shard owner | scalable but hot keys remain hard |
| local budgets | global quota split across nodes/regions | fast but approximate |
| regional quotas | each region has a quota slice | available but can drift globally |
| async reconciliation | allow local decisions and repair later | good availability, weaker strictness |

One-stop interview answer:

> I would enforce rate limits at the gateway using cached policies and a token-bucket or sliding-window strategy. For shared limits, gateway nodes update sharded counter state, usually Redis or a dedicated counter service. At global scale, I avoid exact cross-region coordination on every request and use regional/local budgets with async reconciliation, because availability and latency matter more than perfect precision for most API limits.

---

# 3. Low-Level Design

LLD goal:

> Model rate limiting as a policy-driven decision engine where algorithms are swappable and state storage is hidden behind an interface.

Simple rule:

- `RateLimitKey` says who/what is limited.
- `RateLimitPolicy` says the rule.
- `RateLimitState` stores current usage.
- `RateLimitStrategy` applies the algorithm.
- `RateLimitDecision` says allow or deny.

Starter map:

| LLD question | Rate limiter answer |
|---|---|
| Main input | `RateLimitRequest` |
| Caller identity | `RateLimitKey` |
| Rule object | `RateLimitPolicy` |
| Algorithm abstraction | `RateLimitStrategy` |
| Stored state | `BucketState`, `WindowCounter`, or timestamp log |
| Output | `RateLimitDecision` |
| State dependency | `RateLimitStateStore` |
| Fallback behavior | `FailurePolicy` |

Beginner-friendly design order:

1. Model the request and key.
2. Model policy: limit, window, capacity, refill rate, scope, fail mode.
3. Add strategy interface for fixed window, sliding window, token bucket.
4. Add state store interface so Redis/in-memory implementations are swappable.
5. Return decision with retry-after and remaining quota.
6. Add local fallback for store failures.
7. Add metrics for allow, block, error, and degraded decisions.

Interview sentence:

> In LLD, I will separate policy lookup, key construction, algorithm execution, state storage, and fallback behavior so the limiter can support multiple algorithms without coupling gateway code to Redis or a specific counter model.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `RateLimitRequest` | key, route, cost, timestamp | cost must be positive |
| `RateLimitKey` | normalized composite limiter key | stable for same caller/scope |
| `RateLimitPolicy` | algorithm and limits | policy version must be explicit |
| `BucketState` | token count and last refill time | token count stays within `0..capacity` |
| `WindowCounter` | count for a time window | expires after window TTL |
| `RateLimitDecision` | allow/deny plus headers | retry-after should be meaningful on deny |
| `FailurePolicy` | fail-open or fail-closed behavior | chosen by endpoint risk |

Core services:

| Service | Responsibility |
|---|---|
| `PolicyService` | fetch and validate policies |
| `KeyBuilder` | build composite keys from request context |
| `RateLimiter` | orchestrate policy, strategy, and state store |
| `RateLimitStrategy` | algorithm-specific decision logic |
| `StateStore` | read/update counter or bucket atomically |
| `MetricsPublisher` | emit allow/deny/error/degraded metrics |

## 3.2 Class Sketch

```java
interface RateLimitStrategy {
    RateLimitDecision evaluate(RateLimitRequest request, RateLimitPolicy policy, RateLimitStateStore store);
}

interface RateLimitStateStore {
    BucketState getBucket(String key);
    void saveBucket(String key, BucketState state, long ttlMs);
    long incrementWindow(String key, long windowStartMs, long ttlMs, long cost);
}

final class RateLimitDecision {
    private final boolean allowed;
    private final long remaining;
    private final long retryAfterMs;
    private final String policyId;
}
```

## 3.3 Sequence Diagram

```text
Client -> Gateway: request
Gateway -> KeyBuilder: build key from user/API key/route
Gateway -> PolicyCache: get policy
Gateway -> RateLimiter: check(request, policy)
RateLimiter -> RateLimitStrategy: evaluate
RateLimitStrategy -> StateStore: atomic update state
StateStore --> RateLimitStrategy: updated state
RateLimitStrategy --> RateLimiter: decision
RateLimiter -> MetricsPublisher: emit decision event
Gateway -> Backend: forward if allowed
Gateway --> Client: 429 if denied
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Strategy | fixed window, sliding window, token bucket algorithms |
| Factory | select strategy by policy algorithm |
| Adapter | Redis/Dynamo/in-memory state store adapters |
| Decorator | metrics and tracing around limiter calls |
| Circuit Breaker | protect gateway from limiter backend failure |
| Chain of Responsibility | multi-layer limits: IP, tenant, user, route |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| request arrives exactly at window boundary | prefer token bucket or sliding counter to reduce boundary bursts |
| Redis unavailable | apply failure policy: fail open for low risk, fail closed for login/payment |
| hot tenant key | shard by route/subkey or allocate local budgets |
| clock skew between gateways | use server-side store time or tolerate approximate refill |
| policy updated mid-window | version policy and choose reset or continue behavior |
| retry duplicates | optionally use request ID dedup for expensive operations |
| multi-region traffic | regional quotas or local budgets with async reconciliation |
| burst traffic from valid tenant | token bucket capacity allows configured burst only |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
ratelimiter/
  domain/
    RateLimitRequest.java
    RateLimitPolicy.java
    RateLimitDecision.java
    BucketState.java
  strategy/
    RateLimitStrategy.java
    TokenBucketStrategy.java
    FixedWindowStrategy.java
  service/
    RateLimiter.java
    PolicyService.java
    KeyBuilder.java
  store/
    RateLimitStateStore.java
    InMemoryStateStore.java
    RedisStateStore.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from time import time


@dataclass
class Bucket:
    tokens: float
    last_refill: float


@dataclass(frozen=True)
class Policy:
    capacity: int
    refill_per_second: float


class TokenBucketLimiter:
    def __init__(self) -> None:
        self.buckets: dict[str, Bucket] = {}

    def allow(self, key: str, policy: Policy, cost: int = 1) -> tuple[bool, float]:
        now = time()
        bucket = self.buckets.get(key)
        if bucket is None:
            bucket = Bucket(tokens=policy.capacity, last_refill=now)

        elapsed = max(0.0, now - bucket.last_refill)
        refilled = min(policy.capacity, bucket.tokens + elapsed * policy.refill_per_second)

        if refilled >= cost:
            self.buckets[key] = Bucket(tokens=refilled - cost, last_refill=now)
            return True, self.buckets[key].tokens

        self.buckets[key] = Bucket(tokens=refilled, last_refill=now)
        return False, refilled


limiter = TokenBucketLimiter()
policy = Policy(capacity=5, refill_per_second=1)
for index in range(7):
    allowed, remaining = limiter.allow("user:u1:route:search", policy)
    print(index, allowed, round(remaining, 2))
```

Thread-safe machine-coding version:

```python
from dataclasses import dataclass
from threading import Lock
from time import time


@dataclass
class Bucket:
    tokens: float
    last_refill: float


@dataclass(frozen=True)
class Policy:
    capacity: int
    refill_per_second: float


class ThreadSafeTokenBucketLimiter:
    def __init__(self) -> None:
        self.buckets: dict[str, Bucket] = {}
        self.locks: dict[str, Lock] = {}
        self.global_lock = Lock()

    def allow(self, key: str, policy: Policy, cost: int = 1) -> tuple[bool, float]:
        lock = self._lock_for(key)
        with lock:
            now = time()
            bucket = self.buckets.get(key)
            if bucket is None:
                bucket = Bucket(tokens=policy.capacity, last_refill=now)

            elapsed = max(0.0, now - bucket.last_refill)
            tokens = min(policy.capacity, bucket.tokens + elapsed * policy.refill_per_second)

            if tokens >= cost:
                tokens -= cost
                self.buckets[key] = Bucket(tokens=tokens, last_refill=now)
                return True, tokens

            self.buckets[key] = Bucket(tokens=tokens, last_refill=now)
            return False, tokens

    def _lock_for(self, key: str) -> Lock:
        with self.global_lock:
            if key not in self.locks:
                self.locks[key] = Lock()
            return self.locks[key]
```

Why the lock matters:

- Without a lock, two threads can read the same token count and both allow.
- The critical section must include refill, check, decrement, and state write.
- A single global lock is simple but reduces throughput.
- Per-key locks preserve correctness while allowing unrelated keys to proceed concurrently.
- In production, the same atomicity is usually implemented with Redis Lua, CAS, or single-threaded shard ownership.

Race-condition trap:

```text
Thread A reads tokens = 1
Thread B reads tokens = 1
Thread A allows and writes tokens = 0
Thread B allows and writes tokens = 0

Result: 2 requests passed even though only 1 token existed.
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| policy lookup | `Map<PolicyId, RateLimitPolicy>` |
| token bucket state | `Map<Key, BucketState>` |
| fixed-window counters | `Map<Key+WindowStart, Count>` |
| sliding log | `Map<Key, Deque<Timestamp>>` |
| policy cache | local LRU/TTL cache |
| metrics | append-only event stream |

## 4.4 Concurrency

- Bucket updates must be atomic per key.
- Redis Lua scripts or compare-and-set can prevent race conditions.
- Local in-memory limiter needs locks or single-threaded event-loop ownership.
- Multi-gateway deployments require shared state or approximate local budgets.
- Policy updates should be versioned to avoid inconsistent decisions.

## 4.5 Performance Optimization

- Cache policies in gateway memory.
- Batch metrics asynchronously.
- Use local pre-checks for obvious blocks.
- Use local token borrowing to reduce remote store calls for high-QPS keys.
- Shard counter store by hashed key.
- Use TTLs to clean up inactive keys.
- Keep limiter state compact.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Attack Or Bot Spike

Problem:

- Many IPs or accounts send high request volume and overload gateway/counter store.

Handling:

- Apply coarse limits at CDN/WAF first.
- Use IP reputation and bot rules before precise tenant limits.
- Use local gateway deny cache for repeated offenders.
- Shed metrics detail if metrics pipeline is overloaded.
- Protect counter store with circuit breakers and local fallback.

## 5.2 Hot Tenant Or Partner

Handling:

- Use tenant plus endpoint composite keys.
- Give local gateway budget slices to reduce shared-store writes.
- Enforce both tenant total limit and per-user sublimit.
- Move hot tenant to dedicated counter shards if needed.
- Alert on sustained near-limit behavior.

## 5.3 Counter Store Failure

Handling choices:

| Endpoint risk | Failure behavior |
|---|---|
| public read/search | fail open with local emergency limits |
| login/password reset | fail closed or strict local fallback |
| payment/refund | fail closed or require service-level approval |
| internal low-risk API | fail open with monitoring |

Rule:

> Fail behavior is a product/security decision, not only an engineering default.

## 5.4 Policy Misconfiguration

Handling:

- Validate policy before publish.
- Canary policy rollout to small traffic slice.
- Keep previous policy version for rollback.
- Alert on sudden spike in `429` responses.
- Support emergency bypass for trusted internal clients.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning

| Data | Partition key | Notes |
|---|---|---|
| policy | `policyId` or tenant | small, cache heavily |
| bucket/counter state | hash of rate-limit key | main write path |
| metrics | region + route + tenant | analytics and alerting |
| audit logs | policy ID and admin user | compliance |
| monthly quotas | tenant/account ID | longer retention |

## 6.2 Multi-Region Strategy

Options:

| Strategy | Pros | Cons |
|---|---|---|
| global central counter | strongest global accuracy | high latency and poor availability |
| regional quota slices | fast and available | global drift possible |
| local gateway budgets | very fast | approximate and needs replenishment |
| async reconciliation | scalable | enforcement is not exact in real time |

Recommended:

- Use regional quotas for most public APIs.
- Use strict centralized checks only for sensitive low-QPS actions.
- Use async reconciliation and abuse alerts for quota drift.
- Keep policy replication fast and versioned.

## 6.3 Observability

Track:

- allow/deny rate by policy,
- top limited keys,
- decision latency,
- counter-store latency/errors,
- fallback decision count,
- fail-open/fail-closed events,
- policy version distribution,
- 429 response rate by route,
- hot key and shard pressure,
- quota drift across regions.

## 6.4 Reliability Rules

- The limiter must not add large tail latency.
- The limiter must have explicit degraded behavior.
- Policy cache should survive policy service outage.
- Counter store overload should not take down the gateway fleet.
- Metrics should be async and lossy if needed.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify key, scope, and algorithm.
2. Start with single-node fixed window or token bucket.
3. Move limiter into API gateway hot path.
4. Add policy store and policy cache.
5. Add shared counter/token store for distributed gateways.
6. Discuss token bucket vs fixed/sliding window.
7. Add failure modes: fail open, fail closed, local fallback.
8. Scale with sharding, regional quotas, local budgets, and observability.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| algorithm | fixed window | token bucket | token bucket for APIs, fixed for simple quotas |
| state | local only | shared store | shared for fairness, local fallback for resilience |
| global quota | exact | approximate regional | approximate for high-QPS, exact for sensitive low-QPS |
| failure | fail open | fail closed | depends on endpoint risk |
| location | edge/gateway | service | both for layered defense |

## 7.3 Common Mistakes

- Not defining the limiter key.
- Using IP-only limits for logged-in APIs.
- Making every request depend on a fragile central counter.
- Ignoring what happens when Redis is down.
- Using fixed window without mentioning boundary bursts.
- Applying one limit for every tenant and endpoint.
- Forgetting observability and policy rollback.

## 7.4 Strong Closing

> A rate limiter is a low-latency policy decision system. I would enforce at the gateway with cached policies, use token bucket or sliding counters over sharded state, layer limits by IP/user/tenant/route, and design explicit degraded behavior because perfect global accuracy is usually less important than protecting the platform without taking it down.

---

# 8. Fast Recall Rules

- Always define the rate-limit key first.
- Rate limiter = key + policy + algorithm + state + decision.
- Token bucket is the default API interview answer.
- Fixed window is simple but has boundary bursts.
- Sliding log is precise but memory-heavy.
- Shared counters improve fairness but add dependency risk.
- Local budgets improve latency but are approximate.
- Fail-open vs fail-closed depends on endpoint risk.
- Return `429` with `Retry-After`.
- Observe allow, deny, latency, fallback, and hot keys.
