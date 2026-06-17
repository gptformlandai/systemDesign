# Staff / Principal-Level Additions - Mentorship Track

> Goal: build staff/principal-level design judgment for edge traffic, multi-region resilience, storage economics, and safe production evolution. These topics are where interviews and design reviews shift from "can you design it" to "can you operate, govern, migrate, and explain the trade-offs at company scale."

---

## How We Will Use This Sheet

- Every subtopic follows the same 18-part mentorship template used in the system-design tracks.
- Each section connects intuition, production reality, trade-offs, failure modes, code, simulation, and interview communication.
- Code comments marked `Staff concept` show exactly where the staff-level concept is applied.
- The goal is not memorization. The goal is to speak like someone who has owned reliability, cost, migration risk, and blast radius.

---

## Roadmap

### Part 1

1. Edge, Gateway, and Traffic Management
   - API gateways
   - Global load balancing
   - Request shaping
   - Throttling
   - WAF concepts
   - Edge authentication
   - Multi-layer rate limiting
2. Multi-Region and Disaster Recovery
   - Active-active vs active-passive
   - RPO and RTO
   - Cross-region replication lag
   - Geo-fencing and data residency
   - Region failover strategies
   - Split-brain avoidance
3. Data Lifecycle and Storage Economics
   - Hot, warm, and cold storage tiers
   - Tiered storage policies
   - Archival pipelines
   - Data retention strategies
   - GDPR-style deletions
   - Cost-aware storage design
4. Deployment, Migration, and Evolution
   - Infrastructure as Code mindset
   - Immutable infrastructure
   - Feature flags
   - Dark launches
   - Schema versioning
   - Online migrations
   - Roll-forward vs rollback
 
### Part 2

5. Rate Limiting Algorithms Deep Dive
    - Token bucket
    - Leaky bucket
    - Fixed window
    - Sliding window log
    - Sliding window counter
    - Distributed rate limiting
6. Advanced Security and Compliance
    - PII segregation
    - Encryption key rotation
    - Audit logging
    - RBAC vs ABAC
    - Least privilege enforcement
    - Zero-trust principles
7. Advanced Scaling and Migration Patterns
    - Shadow traffic
    - Dual writes
    - Read/write splitting
    - Strangler Fig pattern
    - Monolith to microservices evolution
    - Contract testing
8. Decision Narration and Staff-Level Communication
    - Assumption declaration
    - Multiple-option framing
    - Explicit trade-off comparison
    - Justified decision making
    - Rejected-alternative explanation
    - Business impact alignment

---

## Template Coverage Matrix

| Area | Subtopics | Coverage |
|---|---:|---|
| 7.1 Edge, gateway, and traffic management | 7 | all 18 sections per subtopic |
| 7.2 Multi-region and disaster recovery | 6 | all 18 sections per subtopic |
| 7.3 Data lifecycle and storage economics | 6 | all 18 sections per subtopic |
| 7.4 Deployment, migration, and evolution | 7 | all 18 sections per subtopic |
| 7.5 Rate limiting algorithms deep dive | 6 | all 18 sections per subtopic |
| 7.6 Advanced security and compliance | 6 | all 18 sections per subtopic |
| 7.7 Advanced scaling and migration patterns | 6 | all 18 sections per subtopic |
| 7.8 Decision narration and staff-level communication | 6 | all 18 sections per subtopic |

---

# Topic 7.1: Edge, Gateway, and Traffic Management

## 7.1.1 API Gateways

### 1. Intuition

An API gateway is the front desk of a large building. It does not do every team's work, but it checks the visitor, routes them to the right room, applies shared rules, and records what happened.

### 2. Definition

- Definition: an API gateway is a managed entry point that routes client requests to backend services while applying cross-cutting policies.
- Category: edge/backend traffic management.
- Core idea: centralize common request handling before traffic reaches many internal services.

### 3. Why It Exists

Without a gateway, every backend service must duplicate auth checks, rate limits, TLS termination, request validation, logging, routing, and version handling. That creates inconsistent behavior and a larger public attack surface.

### 4. Reality

- Where used: public APIs, mobile backends, partner APIs, microservice platforms, BFF layers.
- Systems/products: Kong, Envoy Gateway, AWS API Gateway, Apigee, NGINX, Spring Cloud Gateway.
- Teams: platform, API platform, security, developer experience, SRE.

### 5. How It Works

1. Client sends request to gateway domain.
2. Gateway terminates TLS and identifies route by host, path, method, or headers.
3. Gateway applies policies such as authentication, rate limits, schema checks, and logging.
4. Gateway forwards to the chosen backend service or rejects early.
5. Gateway records metrics, traces, access logs, and failure details.

### 6. What Problem It Solves

- Primary problem solved: consistent public entry-point control across many services.
- Secondary benefits: smaller attack surface, centralized observability, request normalization, version routing.
- Systems impact: improves governance but can become a critical dependency and latency point.

### 7. When to Rely on It

- Use when many clients call many services and shared edge policies matter.
- Strong fit for public APIs, partner integrations, mobile apps, and service versioning.
- Interview keywords: TLS termination, auth, routing, rate limiting, observability, API versioning.

### 8. When Not to Use It

- Avoid putting deep business logic in the gateway.
- Avoid a gateway as a single chokepoint without horizontal scale and fallback planning.
- Use direct internal service calls or a service mesh for east-west traffic when public edge policy is not needed.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Centralizes shared API policy | Can become a bottleneck or single blast radius |
| Reduces duplicated service code | Misconfiguration can break many APIs |
| Improves edge observability | Adds latency and operational ownership |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: policy consistency, traffic control, and public API governance.
- Give up: some simplicity and direct service exposure.
- Latency/cost impact: one more network hop and gateway compute, usually worth it for public APIs.

#### Common Mistakes

- Mistake: turning the gateway into a business orchestration layer. Better approach: keep domain logic in services or BFFs.
- Mistake: one global gateway config changed without canary. Better approach: version and stage gateway rules.
- Mistake: no per-route ownership. Better approach: route owners, review, and rollback policy.

### 11. Key Numbers

- Gateway latency budget: commonly low single-digit milliseconds to tens of milliseconds depending on features.
- Metrics: route p95/p99 latency, 4xx/5xx rate, rejected requests, auth errors, upstream timeout rate.
- Capacity: design for peak QPS plus burst, not daily average.

### 12. Failure Modes

- Bad route sends traffic to the wrong service.
- Auth plugin outage rejects all requests.
- Gateway overload creates global API failure.
- Recovery: config rollback, canary rules, gateway autoscaling, health checks, static fail-closed/fail-open decisions by route.

### 13. Scenario

- Product / system: public hotel booking API used by web, mobile, and partners.
- Why this concept fits: clients need one stable edge while backend services evolve independently.
- What would go wrong without it: every service implements public auth, routing, and telemetry differently.

### 14. Code Sample

```python
def resolve_route(method: str, path: str) -> str:
    routes = {
        ("GET", "/v1/hotels"): "hotel-search-service",
        ("POST", "/v1/bookings"): "booking-service",
    }
    # Staff concept: gateway maps public API surface to internal service ownership.
    return routes.get((method, path), "not-found")
```

### 15. Mini Program / Simulation

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class Request:
    method: str
    path: str
    token_valid: bool


def gateway_handle(request: Request) -> str:
    if not request.token_valid:
        return "401 rejected at gateway"
    service = resolve_route(request.method, request.path)
    if service == "not-found":
        return "404 route missing"
    return f"forward to {service}"


print(gateway_handle(Request("POST", "/v1/bookings", True)))
print(gateway_handle(Request("POST", "/v1/bookings", False)))
```

### 16. Practical Question

> You are designing public APIs for 80 backend services. How would an API gateway help, and what trade-offs would you consider?

### 17. Strong Answer

I would use an API gateway as the public edge for routing, authentication, rate limits, request validation, TLS termination, and observability. It fits because shared policies should be consistent and services should not each expose public concerns. The trade-off is an extra hop, gateway operational ownership, and larger blast radius from bad config. I would avoid domain orchestration in the gateway, canary config changes, scale it horizontally, and monitor route-level p99 and upstream errors.

### 18. Revision Notes

- One-line summary: an API gateway is the controlled public entry point for backend services.
- Three keywords: routing, policy, observability.
- One interview trap: putting business logic in the gateway.
- One memory trick: gateway is front desk, not the whole company.

---

## 7.1.2 Global Load Balancing

### 1. Intuition

Global load balancing is traffic steering at world scale. It decides which region, POP, or endpoint should receive a request based on health, latency, capacity, and policy.

### 2. Definition

- Definition: global load balancing distributes user traffic across geographic regions or edge locations.
- Category: global traffic management and resilience.
- Core idea: route users to healthy and appropriate locations before regional services handle the request.

### 3. Why It Exists

One region cannot serve every user with low latency or survive every outage. Global traffic control reduces latency, spreads load, and supports regional failover.

### 4. Reality

- Where used: CDNs, global APIs, SaaS control planes, gaming, streaming, search.
- Systems/products: Route 53 latency routing, Google Cloud Load Balancing, Azure Front Door, Cloudflare, Akamai.
- Teams: traffic engineering, SRE, edge platform, network engineering.

### 5. How It Works

1. User resolves DNS or connects to a global anycast/edge endpoint.
2. Global balancer evaluates health, latency, geography, weights, and policy.
3. Traffic is routed to a region or POP.
4. Regional load balancer routes to service instances.
5. If a region degrades, traffic shifts according to failover policy.

### 6. What Problem It Solves

- Primary problem solved: low-latency and resilient routing across regions.
- Secondary benefits: capacity balancing, maintenance evacuation, regional isolation.
- Systems impact: improves availability but adds traffic-policy complexity.

### 7. When to Rely on It

- Use for user-facing systems with global users or regional failure requirements.
- Strong fit when latency, data residency, or disaster recovery matters.
- Interview keywords: DNS routing, anycast, health checks, regional failover, weighted traffic.

### 8. When Not to Use It

- Avoid global active routing if the app cannot handle cross-region data consistency.
- Avoid health checks that do not reflect real dependency health.
- Use single-region plus backups for small systems where global HA is not justified.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces global latency | Traffic policy can become complex |
| Supports regional failover | Can amplify data consistency issues |
| Enables maintenance evacuation | DNS caching and route convergence can delay shifts |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: latency, availability, and traffic mobility.
- Give up: simple single-region operation.
- Latency/cost impact: better user p95 globally, higher multi-region infrastructure cost.

#### Common Mistakes

- Mistake: health check only tests gateway ping. Better approach: include critical dependency health.
- Mistake: routing users across data residency boundaries. Better approach: policy-aware routing.
- Mistake: instant failover assumption. Better approach: account for DNS TTL and connection stickiness.

### 11. Key Numbers

- DNS TTL: often 30 seconds to several minutes depending on provider and resolver behavior.
- Health-check interval: seconds to tens of seconds.
- Metrics: regional latency, regional error rate, traffic weight, failover time, capacity headroom.

### 12. Failure Modes

- Partial regional outage passes shallow health checks.
- Failover overloads the backup region.
- DNS caches keep some users on failed endpoints.
- Recovery: synthetic probes, brownout detection, capacity reservations, weighted failover, traffic drain runbooks.

### 13. Scenario

- Product / system: global booking site serving US, EU, and APAC users.
- Why this concept fits: users need low latency and a region may fail.
- What would go wrong without it: all users depend on one far-away or failed region.

### 14. Code Sample

```python
def choose_region(latency_ms: dict[str, int], healthy: dict[str, bool]) -> str:
    candidates = {region: latency for region, latency in latency_ms.items() if healthy.get(region, False)}
    # Staff concept: global balancing selects among healthy regions, not just closest names.
    return min(candidates, key=candidates.get)
```

### 15. Mini Program / Simulation

```python
regions = {
    "us-east": {"latency": 40, "healthy": True, "capacity": 80},
    "eu-west": {"latency": 95, "healthy": True, "capacity": 70},
    "ap-south": {"latency": 130, "healthy": False, "capacity": 90},
}


def route_user(region_state: dict[str, dict[str, int | bool]]) -> str:
    eligible = {
        name: int(state["latency"])
        for name, state in region_state.items()
        if state["healthy"] and int(state["capacity"]) > 20
    }
    return min(eligible, key=eligible.get)


print(route_user(regions))
```

### 16. Practical Question

> You are designing a global API with users on three continents. How would global load balancing help, and what trade-offs would you consider?

### 17. Strong Answer

I would use global load balancing to route users to healthy, low-latency regions while respecting capacity and residency policy. It fits because one region creates latency and failure risk. The trade-off is multi-region cost, data consistency complexity, and failover behavior under DNS/cache delays. I would use deep health checks, reserve failover capacity, test regional evacuation, and monitor per-region p99 and error rate.

### 18. Revision Notes

- One-line summary: global load balancing steers users to healthy appropriate regions.
- Three keywords: health, latency, failover.
- One interview trap: assuming nearest region is always correct.
- One memory trick: global balancer is air traffic control for regions.

---

## 7.1.3 Request Shaping

### 1. Intuition

Request shaping changes how traffic enters the system so downstream services receive a safer, smoother, or more useful version of demand.

### 2. Definition

- Definition: request shaping modifies, delays, prioritizes, rejects, batches, routes, or normalizes requests before they hit backend capacity.
- Category: traffic control and overload prevention.
- Core idea: not all traffic should be treated equally or forwarded immediately.

### 3. Why It Exists

Raw traffic is bursty, uneven, and sometimes wasteful. If every request is forwarded unchanged, backend services see spikes, duplicate work, low-value calls, and expensive query patterns.

### 4. Reality

- Where used: gateways, CDNs, API platforms, queues, search services, recommendation systems.
- Systems/products: priority queues, request coalescing, query normalization, adaptive concurrency, brownout modes.
- Teams: SRE, API platform, performance engineering, search/platform teams.

### 5. How It Works

1. Gateway or edge classifies incoming request.
2. Policy assigns priority, cost, route, or admission decision.
3. System may normalize headers/query, collapse duplicates, batch, queue, or reject.
4. Backend receives shaped traffic within safer limits.
5. Metrics feed back into adaptive shaping rules.

### 6. What Problem It Solves

- Primary problem solved: uncontrolled traffic patterns overwhelming downstream systems.
- Secondary benefits: fairness, cost control, latency protection, better cache hit ratio.
- Systems impact: improves stability but adds policy and product behavior complexity.

### 7. When to Rely on It

- Use when traffic has mixed priority, expensive endpoints, or bursty demand.
- Strong fit for search, checkout, partner APIs, and overload brownouts.
- Interview keywords: priority, admission control, request coalescing, query normalization, brownout.

### 8. When Not to Use It

- Avoid shaping that hides correctness failures or silently drops critical requests.
- Avoid complex shaping before you understand traffic classes and SLOs.
- Use capacity increase or simpler rate limits if all requests are equal and predictable.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects critical paths | Requires clear priority policy |
| Smooths bursts and expensive calls | Can surprise clients if behavior is undocumented |
| Improves backend stability | Adds operational tuning complexity |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: safer downstream load and better SLO protection.
- Give up: simple pass-through semantics.
- Latency/cost impact: may add queueing delay but lowers overload failures and wasted compute.

#### Common Mistakes

- Mistake: shaping only by QPS. Better approach: include request cost and priority.
- Mistake: silently degrading paid or critical flows. Better approach: product-aligned degradation policy.
- Mistake: no feedback loop. Better approach: tune using saturation, latency, and error metrics.

### 11. Key Numbers

- Queue delay budget: should fit within endpoint timeout budget.
- Metrics: admission rate, shaped/rejected count, request cost, queue depth, backend saturation.
- Priority classes: commonly critical, standard, best-effort, background.

### 12. Failure Modes

- Low-priority queue starves forever.
- Expensive queries bypass shaping and overload storage.
- Shaping rule breaks client compatibility.
- Recovery: fair queues, cost scoring, policy rollback, client-visible error contracts.

### 13. Scenario

- Product / system: hotel search API during holiday sale traffic.
- Why this concept fits: broad searches are expensive while checkout availability checks are critical.
- What would go wrong without it: expensive search traffic can starve booking confirmation.

### 14. Code Sample

```python
def request_cost(path: str, query_params: dict[str, str]) -> int:
    if path == "/search" and query_params.get("city") == "any":
        # Staff concept: shape based on cost, not only raw request count.
        return 10
    return 1
```

### 15. Mini Program / Simulation

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class IncomingRequest:
    path: str
    priority: str
    cost: int


def admit(request: IncomingRequest, remaining_budget: int) -> bool:
    if request.priority == "critical":
        return request.cost <= remaining_budget
    return request.cost <= remaining_budget // 2


requests = [IncomingRequest("/book", "critical", 5), IncomingRequest("/search", "best-effort", 8)]
print([admit(request, 10) for request in requests])
```

### 16. Practical Question

> You are designing traffic protection for a search and checkout platform. How would request shaping help, and what trade-offs would you consider?

### 17. Strong Answer

I would shape requests by priority and cost so critical checkout traffic stays protected while expensive or best-effort search traffic is delayed, simplified, or rejected during overload. It fits because not all requests have equal business value or backend cost. The trade-off is policy complexity and possible client-visible degradation. I would use clear error contracts, fair queues, cost scoring, and metrics for saturation, queue delay, and rejected requests.

### 18. Revision Notes

- One-line summary: request shaping makes incoming demand safer before forwarding it.
- Three keywords: priority, cost, admission.
- One interview trap: treating all requests as equal.
- One memory trick: shape the wave before it hits the wall.

---

## 7.1.4 Throttling

### 1. Intuition

Throttling is a speed limit. It allows traffic, but only up to a controlled rate so one client, endpoint, or dependency does not consume all capacity.

### 2. Definition

- Definition: throttling limits the rate or concurrency of requests over time.
- Category: traffic control and capacity protection.
- Core idea: enforce controlled usage before overload happens.

### 3. Why It Exists

Clients can send bursts, bugs can loop, retries can amplify failures, and dependencies have finite capacity. Without throttling, a few callers can harm everyone.

### 4. Reality

- Where used: API gateways, service clients, databases, queues, third-party integrations.
- Systems/products: cloud API quotas, payment provider limits, search QPS limits, Kafka consumer limits.
- Teams: API platform, SRE, backend, partner integrations.

### 5. How It Works

1. Define limit by user, API key, endpoint, tenant, or service.
2. Track request count, tokens, or concurrency.
3. Admit requests within limit.
4. Reject or delay requests beyond limit.
5. Return retry guidance and emit throttle metrics.

### 6. What Problem It Solves

- Primary problem solved: protecting shared capacity from excessive callers.
- Secondary benefits: fairness, cost control, dependency protection, abuse reduction.
- Systems impact: improves reliability but can create client-visible rejections.

### 7. When to Rely on It

- Use for public APIs, partner APIs, expensive endpoints, and finite downstream dependencies.
- Strong fit where fairness and quota enforcement matter.
- Interview keywords: 429, quota, token bucket, concurrency limit, retry-after.

### 8. When Not to Use It

- Avoid blind throttling of critical internal recovery flows.
- Avoid strict limits without burst allowance when traffic is naturally spiky.
- Use backpressure, queues, or autoscaling where slowing rather than rejecting is preferable.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects shared resources | Clients may see 429 or delays |
| Enforces fairness | Requires correct identity/keying |
| Controls cost and abuse | Poor limits can block legitimate bursts |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: predictable capacity protection.
- Give up: accepting every request immediately.
- Latency/cost impact: lower overload cost, possible rejection or queueing delay.

#### Common Mistakes

- Mistake: throttle only globally. Better approach: per-tenant, per-route, and dependency-aware limits.
- Mistake: no Retry-After guidance. Better approach: tell clients when and how to retry.
- Mistake: confusing throttling with auth. Better approach: auth decides who; throttling decides how much.

### 11. Key Numbers

- HTTP status: 429 for too many requests.
- Metrics: allowed rate, throttled rate, token utilization, retry-after compliance, per-tenant usage.
- Limits: often request/minute plus burst capacity and concurrency caps.

### 12. Failure Modes

- Bad keying lets one tenant bypass limits.
- Central limiter outage blocks or allows too much traffic.
- Retry storms after throttling increase load.
- Recovery: local fallback limits, safe default, jittered backoff, per-route policy rollback.

### 13. Scenario

- Product / system: partner booking API with paid tiers.
- Why this concept fits: each partner needs fair capacity and predictable limits.
- What would go wrong without it: one integration can saturate booking or payment services.

### 14. Code Sample

```python
def should_throttle(current_count: int, limit_per_minute: int) -> bool:
    # Staff concept: throttle before downstream capacity collapses.
    return current_count >= limit_per_minute
```

### 15. Mini Program / Simulation

```python
class FixedWindowThrottle:
    def __init__(self, limit: int) -> None:
        self.limit = limit
        self.counts: dict[str, int] = {}

    def allow(self, tenant: str) -> bool:
        count = self.counts.get(tenant, 0)
        if count >= self.limit:
            return False
        self.counts[tenant] = count + 1
        return True


throttle = FixedWindowThrottle(limit=3)
print([throttle.allow("partner-a") for request_number in range(5)])
```

### 16. Practical Question

> You are designing a public partner API. How would throttling help, and what trade-offs would you consider?

### 17. Strong Answer

I would throttle by tenant, route, and possibly cost so one partner cannot consume shared capacity. It fits because public APIs need fairness, predictable quotas, and dependency protection. The trade-off is client-visible 429s and the need for good keying and retry guidance. I would return Retry-After, allow controlled bursts with token bucket, monitor per-tenant usage, and provide safe fallback behavior if the limiter is degraded.

### 18. Revision Notes

- One-line summary: throttling enforces a speed limit on traffic.
- Three keywords: quota, 429, fairness.
- One interview trap: only having a single global limit.
- One memory trick: throttle is a speed limit, not a lock.

---

## 7.1.5 WAF Concepts

### 1. Intuition

A WAF is a security filter at the edge. It looks for suspicious request patterns before they reach the application.

### 2. Definition

- Definition: a Web Application Firewall inspects HTTP traffic and blocks, challenges, or logs requests matching malicious or risky patterns.
- Category: edge security and application-layer protection.
- Core idea: stop common application attacks before backend code handles them.

### 3. Why It Exists

Applications are exposed to SQL injection attempts, cross-site scripting payloads, bot traffic, protocol abuse, and known exploit patterns. App code should still be secure, but WAFs reduce exposure and buy response time.

### 4. Reality

- Where used: public web apps, APIs, admin portals, payment/checkout paths.
- Systems/products: AWS WAF, Cloudflare WAF, Akamai, Fastly, ModSecurity.
- Teams: security engineering, platform, SRE, application teams.

### 5. How It Works

1. Request reaches edge or gateway.
2. WAF evaluates IP reputation, headers, path, body, signatures, and behavior.
3. Rule action is allow, block, challenge, rate limit, or log.
4. Matched requests are recorded for investigation.
5. Rules are tuned to reduce false positives and false negatives.

### 6. What Problem It Solves

- Primary problem solved: reducing common HTTP/application attack traffic before it reaches services.
- Secondary benefits: virtual patching, bot friction, security telemetry, emergency blocks.
- Systems impact: improves defense-in-depth but does not replace secure coding.

### 7. When to Rely on It

- Use for internet-facing apps and APIs, especially high-risk or regulated paths.
- Strong fit for known exploit mitigation and bot/noisy abuse reduction.
- Interview keywords: SQLi, XSS, OWASP, signatures, false positives, virtual patching.

### 8. When Not to Use It

- Do not rely on WAF as the only security layer.
- Avoid aggressive blocking without monitoring if business traffic may match rules.
- Use app validation, parameterized queries, auth, secure headers, and threat modeling as core controls.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Blocks common attacks early | False positives can block valid users |
| Gives emergency mitigation | False negatives still reach apps |
| Adds security telemetry | Rule tuning needs ownership |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: edge protection and fast mitigation.
- Give up: some request flexibility and rule maintenance simplicity.
- Latency/cost impact: inspection adds cost and small latency but reduces attack load.

#### Common Mistakes

- Mistake: WAF equals security. Better approach: WAF is one layer in defense-in-depth.
- Mistake: enabling blocking rules without dry run. Better approach: log mode, tune, then block.
- Mistake: ignoring false positives. Better approach: monitor blocked business transactions.

### 11. Key Numbers

- Metrics: blocked requests, challenged requests, false positives, rule match count, backend attack traffic.
- Deployment mode: log/detect first, then block for high-confidence rules.
- Response time: emergency rule rollout should be minutes, not days.

### 12. Failure Modes

- Bad rule blocks checkout or login.
- WAF bypass path exposes origin directly.
- Attack mutates past simple signatures.
- Recovery: origin lockdown, staged rule rollout, allowlist critical partners, rule rollback, app patching.

### 13. Scenario

- Product / system: public booking checkout page.
- Why this concept fits: checkout is internet-facing and high-value for abuse.
- What would go wrong without it: common exploit and bot traffic reaches app services directly.

### 14. Code Sample

```python
SUSPICIOUS_PATTERNS = ["' OR 1=1", "<script", "../"]


def waf_score(request_body: str) -> int:
    # Staff concept: WAF rules score suspicious application-layer patterns.
    return sum(10 for pattern in SUSPICIOUS_PATTERNS if pattern.lower() in request_body.lower())
```

### 15. Mini Program / Simulation

```python
def waf_decision(request_body: str) -> str:
    score = waf_score(request_body)
    if score >= 20:
        return "block"
    if score >= 10:
        return "challenge"
    return "allow"


samples = ["normal booking", "name=<script>alert(1)</script>", "id=' OR 1=1 and path=../"]
print([waf_decision(sample) for sample in samples])
```

### 16. Practical Question

> You are protecting a public checkout API. How would a WAF help, and what trade-offs would you consider?

### 17. Strong Answer

I would use a WAF as an edge defense layer to block common attack patterns, bots, and emergency exploit traffic before it reaches services. It fits because checkout is public and high value. The trade-off is false positives, rule maintenance, and inspection overhead. I would deploy risky rules in log mode first, monitor blocked revenue paths, keep origin locked down, and still fix application vulnerabilities directly.

### 18. Revision Notes

- One-line summary: WAF filters risky HTTP traffic at the edge.
- Three keywords: signatures, false positives, defense-in-depth.
- One interview trap: treating WAF as a replacement for secure code.
- One memory trick: WAF is a shield, not the whole armor.

---

## 7.1.6 Edge Authentication

### 1. Intuition

Edge authentication checks identity before traffic travels deep into the system. It is like validating a pass at the campus gate instead of inside every room.

### 2. Definition

- Definition: edge authentication verifies client identity and token/session validity at gateway, CDN, or edge layer.
- Category: edge security and access control.
- Core idea: reject unauthenticated traffic early and propagate trusted identity context downstream.

### 3. Why It Exists

Backend services should not spend expensive compute on obviously unauthenticated traffic. Centralized edge auth also reduces duplicated token parsing and standardizes identity propagation.

### 4. Reality

- Where used: API gateways, CDN workers, zero-trust access, mobile APIs, BFFs.
- Systems/products: OAuth2/OIDC gateways, JWT validation at edge, Cloudflare Access, Envoy ext_authz.
- Teams: identity platform, security, API platform, backend services.

### 5. How It Works

1. Request arrives with token, cookie, API key, or client certificate.
2. Edge validates signature, expiration, issuer, audience, and revocation policy where applicable.
3. Edge rejects invalid requests.
4. Edge forwards allowed requests with sanitized identity headers or claims.
5. Backend still authorizes actions based on domain rules.

### 6. What Problem It Solves

- Primary problem solved: consistent early authentication before backend fanout.
- Secondary benefits: reduced backend load, fewer duplicated auth libraries, stronger edge posture.
- Systems impact: improves security and latency for rejected traffic but increases edge dependency.

### 7. When to Rely on It

- Use for public APIs, internal zero-trust gateways, and high-volume unauthenticated traffic.
- Strong fit when token validation can be centralized and cached safely.
- Interview keywords: JWT, OIDC, ext_authz, identity headers, token expiry, backend authorization.

### 8. When Not to Use It

- Do not confuse authentication with authorization.
- Avoid trusting unsanitized client-supplied identity headers.
- Use service-level authz for domain decisions and resource ownership checks.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Rejects invalid traffic early | Edge outage can affect all requests |
| Standardizes identity validation | Revocation and key rotation need care |
| Reduces backend duplicate logic | Backend must still authorize actions |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: centralized identity validation and lower backend waste.
- Give up: some autonomy in per-service auth handling.
- Latency/cost impact: small edge validation cost, lower downstream load for rejected traffic.

#### Common Mistakes

- Mistake: edge auth means backend can skip authorization. Better approach: edge authenticates, service authorizes.
- Mistake: forwarding user-controlled identity headers. Better approach: strip and reissue trusted headers.
- Mistake: no key rotation plan. Better approach: cache JWKS with refresh and overlap.

### 11. Key Numbers

- Token expiry: minutes to hours depending on risk and refresh design.
- Metrics: auth rejection rate, token verification latency, key fetch failures, issuer/audience errors.
- Cache: JWKS and introspection cache TTL must balance performance and revocation needs.

### 12. Failure Modes

- Identity provider outage blocks token introspection.
- Expired key cache rejects valid tokens after rotation.
- Header spoofing bypasses trust boundary.
- Recovery: fail policy per route, JWKS overlap, strip inbound identity headers, local validation fallback.

### 13. Scenario

- Product / system: mobile booking API with JWT-based login.
- Why this concept fits: invalid traffic can be rejected before booking and payment services.
- What would go wrong without it: every service duplicates token validation and may disagree.

### 14. Code Sample

```python
def token_claims_valid(claims: dict[str, str], expected_issuer: str, expected_audience: str) -> bool:
    # Staff concept: edge validates issuer and audience before forwarding identity.
    return claims.get("iss") == expected_issuer and claims.get("aud") == expected_audience
```

### 15. Mini Program / Simulation

```python
def edge_authenticate(headers: dict[str, str]) -> dict[str, str] | None:
    token = headers.get("authorization")
    if token != "Bearer valid-demo-token":
        return None
    # Staff concept: edge emits trusted identity context after validation.
    return {"x-authenticated-user": "user-123", "x-auth-source": "edge"}


print(edge_authenticate({"authorization": "Bearer valid-demo-token"}))
print(edge_authenticate({"authorization": "Bearer expired"}))
```

### 16. Practical Question

> You are designing authentication for a high-traffic public API. How would edge authentication help, and what trade-offs would you consider?

### 17. Strong Answer

I would validate identity at the edge so invalid traffic is rejected before backend fanout and services receive consistent trusted identity context. It fits when token validation is common across APIs. The trade-off is edge dependency, key rotation complexity, and revocation behavior. I would strip inbound identity headers, validate issuer/audience/signature, cache keys safely, and keep service-level authorization for resource decisions.

### 18. Revision Notes

- One-line summary: edge authentication verifies who you are before deeper routing.
- Three keywords: JWT, identity headers, authorization.
- One interview trap: saying edge auth replaces backend authorization.
- One memory trick: gate checks badge; room decides permission.

---

## 7.1.7 Multi-Layer Rate Limiting

### 1. Intuition

Multi-layer rate limiting is multiple safety nets at different depths. CDN, gateway, service, and dependency clients each enforce limits for the risk they can see.

### 2. Definition

- Definition: multi-layer rate limiting applies different limits at edge, gateway, service, and dependency levels.
- Category: defense-in-depth traffic protection.
- Core idea: no single limiter sees every dimension of overload or abuse.

### 3. Why It Exists

Edge limiters see IP and geography, gateways see API keys and routes, services see business entities, and dependency clients see local saturation. One layer alone cannot protect all failure modes.

### 4. Reality

- Where used: large public APIs, payments, login, search, notification systems.
- Systems/products: CDN bot limits, gateway quotas, service-level concurrency caps, database client limits.
- Teams: API platform, SRE, security, service owners.

### 5. How It Works

1. Edge applies coarse IP/bot/geography limits.
2. Gateway applies tenant/API-key/route quotas.
3. Service applies business limits and priority rules.
4. Downstream clients apply concurrency and retry budgets.
5. Observability correlates rejections across layers.

### 6. What Problem It Solves

- Primary problem solved: layered protection against abuse, overload, and dependency collapse.
- Secondary benefits: fairness, blast-radius containment, per-domain control.
- Systems impact: improves resilience but requires clear error semantics and coordination.

### 7. When to Rely on It

- Use for high-traffic public systems, expensive APIs, login/payment, and multi-tenant platforms.
- Strong fit where different layers know different identities and capacities.
- Interview keywords: layered defense, edge IP limit, API key quota, service concurrency, retry budget.

### 8. When Not to Use It

- Avoid many uncoordinated limiters that create confusing client behavior.
- Avoid duplicate limits with different thresholds and no ownership.
- Use a simpler single limiter for small systems with one clear bottleneck.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects multiple bottlenecks | Harder to debug why request was rejected |
| Combines coarse and domain-aware controls | Needs consistent client error contracts |
| Reduces blast radius | Misaligned limits can waste capacity |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: defense-in-depth and better bottleneck-specific protection.
- Give up: one simple place to reason about limits.
- Latency/cost impact: more checks, but less overload and fewer expensive downstream failures.

#### Common Mistakes

- Mistake: all limiters return different errors. Better approach: consistent 429/503 contracts and reason codes.
- Mistake: edge limit only. Better approach: add service-level limits for business entities.
- Mistake: no correlation IDs. Better approach: trace which layer rejected the request.

### 11. Key Numbers

- Metrics: rejects by layer, reason code, tenant, route, dependency saturation, retry rate.
- Limits: coarse edge burst, gateway quota, service concurrency, dependency client pool.
- Error codes: 429 for quota/rate, 503 for overload/unavailable.

### 12. Failure Modes

- Edge blocks shared NAT users unfairly.
- Gateway allows traffic that overwhelms a hot tenant partition.
- Service limiter rejects after expensive upstream work is already done.
- Recovery: tune per-layer order, add reason codes, use tenant-aware keys, push limits earlier where safe.

### 13. Scenario

- Product / system: login API under credential stuffing and normal mobile traffic.
- Why this concept fits: IP, account, device, tenant, and auth provider limits all matter.
- What would go wrong without it: a single global limit either blocks too much or protects too little.

### 14. Code Sample

```python
def rate_limit_key(layer: str, request: dict[str, str]) -> str:
    if layer == "edge":
        return f"ip:{request['ip']}"
    if layer == "gateway":
        return f"api-key:{request['api_key']}:{request['route']}"
    # Staff concept: deeper layers can use business identity, not just network identity.
    return f"account:{request['account_id']}"
```

### 15. Mini Program / Simulation

```python
def evaluate_layers(request: dict[str, str], counters: dict[str, int], limits: dict[str, int]) -> str:
    for layer in ["edge", "gateway", "service"]:
        key = rate_limit_key(layer, request)
        if counters.get(key, 0) >= limits[layer]:
            return f"429 from {layer}"
        counters[key] = counters.get(key, 0) + 1
    return "allowed"


request = {"ip": "1.2.3.4", "api_key": "partner-a", "route": "/login", "account_id": "u1"}
print(evaluate_layers(request, {}, {"edge": 100, "gateway": 50, "service": 5}))
```

### 16. Practical Question

> You are designing protection for login and payment APIs. How would multi-layer rate limiting help, and what trade-offs would you consider?

### 17. Strong Answer

I would use layered limits because each layer sees different risk: edge sees IP and bot patterns, gateway sees API key and route, service sees account or payment identity, and clients see dependency saturation. The trade-off is debugging and policy complexity. I would standardize reason codes, use correlation IDs, monitor rejects by layer, and keep limits aligned with real bottlenecks.

### 18. Revision Notes

- One-line summary: multi-layer limits protect different bottlenecks with different identities.
- Three keywords: edge, tenant, concurrency.
- One interview trap: assuming one limiter can protect everything.
- One memory trick: different doors need different locks.

---

# Topic 7.2: Multi-Region and Disaster Recovery

## 7.2.1 Active-Active vs Active-Passive

### 1. Intuition

Active-active means multiple regions serve live traffic at the same time. Active-passive means one region serves traffic while another waits as a standby.

### 2. Definition

- Definition: active-active runs production workload in multiple regions concurrently; active-passive keeps one or more standby regions ready for failover.
- Category: multi-region availability architecture.
- Core idea: choose between continuous multi-region serving and simpler standby recovery.

### 3. Why It Exists

Regional outages happen. The system needs a way to continue or recover service when one region fails, but different products have different latency, consistency, cost, and operational needs.

### 4. Reality

- Where used: banking, commerce, SaaS, streaming, cloud control planes, marketplaces.
- Systems/products: global databases, regional clusters, replicated queues, DNS/global balancers.
- Teams: SRE, infrastructure, database, platform architecture.

### 5. How It Works

1. Active-active routes users to multiple live regions.
2. Each active region has capacity, data access, observability, and failover plans.
3. Active-passive routes normal traffic to one primary region.
4. Standby region receives replication, backups, or warm services.
5. Failover promotes standby or shifts traffic when primary is unhealthy.

### 6. What Problem It Solves

- Primary problem solved: service continuity during regional failure.
- Secondary benefits: lower global latency for active-active, simpler consistency for active-passive.
- Systems impact: deeply affects data replication, conflict handling, capacity, cost, and operations.

### 7. When to Rely on It

- Use active-active for low-latency global users and high availability where conflict handling is understood.
- Use active-passive for simpler correctness and lower cost when failover delay is acceptable.
- Interview keywords: regional outage, failover, conflict resolution, standby, traffic shift.

### 8. When Not to Use It

- Avoid active-active for strongly consistent writes unless the database/protocol supports it.
- Avoid active-passive if recovery time cannot meet the business promise.
- Use single-region plus backups if regional HA is not worth the cost.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Active-active lowers latency and improves availability | Active-active increases consistency and conflict complexity |
| Active-passive is easier to reason about | Active-passive has failover time and idle capacity cost |
| Both improve regional resilience | Both require regular failover testing |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: resilience to regional failures.
- Give up: single-region simplicity.
- Latency/cost impact: active-active improves latency but costs more and complicates data; active-passive costs less but recovers slower.

#### Common Mistakes

- Mistake: active-active without conflict strategy. Better approach: define write ownership, quorum, or merge rules.
- Mistake: standby never tested. Better approach: scheduled failover drills.
- Mistake: passive region has no real capacity. Better approach: reserve and test capacity.

### 11. Key Numbers

- RTO: active-active can be seconds/minutes; active-passive may be minutes/hours depending on readiness.
- RPO: depends on replication mode and lag.
- Metrics: regional health, replication lag, failover duration, conflict rate, standby readiness.

### 12. Failure Modes

- Active-active writes conflict across regions.
- Passive promotion fails due to stale data or missing dependencies.
- Traffic shifts but backup region lacks capacity.
- Recovery: write fencing, failover runbooks, regular drills, capacity tests, replication monitoring.

### 13. Scenario

- Product / system: global booking platform.
- Why this concept fits: users need continuity when a region fails.
- What would go wrong without it: a single regional outage can stop booking worldwide.

### 14. Code Sample

```python
def choose_dr_mode(rto_minutes: int, conflict_tolerance: bool) -> str:
    if rto_minutes <= 5 and conflict_tolerance:
        # Staff concept: active-active needs very low RTO plus conflict strategy.
        return "active-active"
    return "active-passive"
```

### 15. Mini Program / Simulation

```python
def route_region(user_region: str, mode: str, primary_healthy: bool) -> str:
    if mode == "active-active":
        return user_region if user_region in {"us", "eu"} else "us"
    if primary_healthy:
        return "us-primary"
    # Staff concept: active-passive shifts traffic only after primary failure.
    return "eu-standby"


print(route_region("eu", "active-active", True))
print(route_region("eu", "active-passive", False))
```

### 16. Practical Question

> You are designing disaster recovery for a global booking platform. How would you choose active-active vs active-passive, and what trade-offs would you consider?

### 17. Strong Answer

I would choose based on RTO, RPO, consistency, cost, and operational maturity. Active-active is appropriate when global latency and near-continuous availability matter and the write model can handle conflicts or regional ownership. Active-passive is simpler and often safer for strong consistency, but has failover delay and standby capacity cost. I would test failovers, monitor replication lag, reserve capacity, and define write fencing before an outage.

### 18. Revision Notes

- One-line summary: active-active serves in many regions; active-passive waits and promotes.
- Three keywords: failover, conflict, standby.
- One interview trap: choosing active-active without data conflict design.
- One memory trick: active-active is two drivers; active-passive is a spare driver.

---

## 7.2.2 RPO and RTO

### 1. Intuition

RPO asks, "How much data can we afford to lose?" RTO asks, "How long can we afford to be down?" They turn disaster recovery from vague hope into measurable promises.

### 2. Definition

- Definition: Recovery Point Objective is acceptable data loss window; Recovery Time Objective is acceptable recovery duration.
- Category: disaster recovery planning.
- Core idea: design backup, replication, and failover strategy around explicit business tolerances.

### 3. Why It Exists

Different systems have different recovery needs. Losing one hour of analytics may be acceptable; losing confirmed payments may not be. RPO/RTO prevents overpaying for non-critical systems and underprotecting critical ones.

### 4. Reality

- Where used: DR plans, compliance, SLOs, backup design, database replication, incident planning.
- Systems/products: payment ledgers, customer databases, analytics warehouses, file storage.
- Teams: SRE, database, security/compliance, business continuity, platform.

### 5. How It Works

1. Business defines acceptable data loss and downtime by system.
2. Architects map RPO to replication/backup frequency.
3. Architects map RTO to failover automation and readiness.
4. Teams test recovery against those targets.
5. Results feed back into cost, architecture, and runbook decisions.

### 6. What Problem It Solves

- Primary problem solved: aligning recovery design with business impact.
- Secondary benefits: cost control, prioritization, compliance evidence, incident clarity.
- Systems impact: drives replication mode, backup cadence, failover automation, and staffing.

### 7. When to Rely on It

- Use for any production system that needs a recovery plan.
- Strong fit for tiering systems by criticality.
- Interview keywords: acceptable data loss, downtime, backups, replication, DR drill.

### 8. When Not to Use It

- Do not use one RPO/RTO for all systems.
- Avoid unrealistic zero RPO and zero RTO unless the business funds the complexity.
- Use simpler backup/restore for non-critical internal tools.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Makes recovery goals measurable | Requires business alignment |
| Guides architecture and cost | Targets can be expensive to meet |
| Supports DR testing | False confidence if not tested |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: clear recovery design and prioritization.
- Give up: vague one-size-fits-all resilience claims.
- Latency/cost impact: lower RPO/RTO usually means higher replication, automation, and standby cost.

#### Common Mistakes

- Mistake: saying RPO/RTO after architecture is done. Better approach: use them as inputs.
- Mistake: promising zero data loss without synchronous replication. Better approach: align mechanism and target.
- Mistake: backups exist, so recovery is solved. Better approach: restore tests prove recovery.

### 11. Key Numbers

- RPO examples: zero, seconds, minutes, hours, daily.
- RTO examples: seconds, minutes, hours, days.
- Metrics: actual recovery time, last restorable point, backup success rate, restore test duration.

### 12. Failure Modes

- Backup exists but cannot be restored.
- Replication lag exceeds RPO.
- Manual recovery exceeds RTO.
- Recovery: regular restore drills, automated failover where justified, backup integrity checks, runbook timing.

### 13. Scenario

- Product / system: booking payments ledger and daily analytics dashboard.
- Why this concept fits: ledger and analytics have different data-loss tolerance.
- What would go wrong without it: teams overbuild analytics and underprotect payments.

### 14. Code Sample

```python
def meets_recovery_targets(actual_rpo_minutes: int, actual_rto_minutes: int, target_rpo: int, target_rto: int) -> bool:
    # Staff concept: DR is measured against declared RPO and RTO.
    return actual_rpo_minutes <= target_rpo and actual_rto_minutes <= target_rto
```

### 15. Mini Program / Simulation

```python
systems = {
    "payments-ledger": {"target_rpo": 0, "target_rto": 5, "actual_rpo": 0, "actual_rto": 4},
    "analytics-dashboard": {"target_rpo": 1440, "target_rto": 240, "actual_rpo": 60, "actual_rto": 90},
}


for name, values in systems.items():
    print(name, meets_recovery_targets(values["actual_rpo"], values["actual_rto"], values["target_rpo"], values["target_rto"]))
```

### 16. Practical Question

> You are defining DR strategy for payments and analytics systems. How would RPO and RTO guide the design?

### 17. Strong Answer

I would first classify systems by business impact and define RPO/RTO per tier. Payments may need near-zero RPO and very low RTO, which implies strong replication, tested failover, and strict restore validation. Analytics may tolerate longer RPO/RTO with cheaper backups or batch rebuilds. The trade-off is cost and complexity. I would prove the targets through restore drills and monitor lag, backup health, and recovery time.

### 18. Revision Notes

- One-line summary: RPO is data loss; RTO is downtime.
- Three keywords: loss, time, test.
- One interview trap: claiming backups prove recovery.
- One memory trick: point is data point; time is downtime.

---

## 7.2.3 Cross-Region Replication Lag

### 1. Intuition

Replication lag is the time gap between "written here" and "visible there." In cross-region systems, distance and load make that gap unavoidable unless you pay for synchronous coordination.

### 2. Definition

- Definition: cross-region replication lag is the delay for data changes to propagate from one region to another.
- Category: distributed data replication.
- Core idea: remote replicas are often behind the source, so reads/failover may see stale data.

### 3. Why It Exists

Cross-region replication must send logs over networks, apply them remotely, handle bursts, and respect ordering. Network distance, bandwidth, write rate, and replica apply speed all create lag.

### 4. Reality

- Where used: read replicas, DR replicas, analytics sync, search/index replication, multi-region caches.
- Systems/products: MySQL/Postgres replicas, DynamoDB global tables, Kafka MirrorMaker, object replication.
- Teams: database, SRE, data platform, regional infrastructure.

### 5. How It Works

1. Source region commits write.
2. Change is recorded in log/WAL/binlog/stream.
3. Replication service transfers change to remote region.
4. Remote region applies change.
5. Monitoring tracks source position minus remote applied position.

### 6. What Problem It Solves

- Primary problem solved: understanding freshness risk in remote data copies.
- Secondary benefits: safer failover, read-routing decisions, RPO measurement.
- Systems impact: affects correctness, user experience, and DR confidence.

### 7. When to Rely on It

- Always reason about lag when using async cross-region replication.
- Strong fit for DR planning, remote reads, read-your-writes decisions, and failover.
- Interview keywords: async replication, stale reads, replica lag, WAL position, RPO.

### 8. When Not to Use It

- Do not serve correctness-critical reads from lagging replicas.
- Avoid failover to a stale replica without data-loss decision.
- Use synchronous replication or quorum writes when remote freshness is mandatory.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Async replication preserves local write latency | Remote reads may be stale |
| Enables DR and regional reads | Lag can violate RPO |
| Decouples regions during network issues | Failover may lose recent writes |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: lower write latency and regional decoupling.
- Give up: immediate remote consistency.
- Latency/cost impact: async is cheaper/faster locally; sync is safer but slower and less available.

#### Common Mistakes

- Mistake: treating replica as current. Better approach: check lag before routing reads.
- Mistake: failover without knowing last applied position. Better approach: promote with explicit RPO decision.
- Mistake: monitor bytes only. Better approach: monitor time lag and apply lag.

### 11. Key Numbers

- Normal lag: milliseconds to seconds for healthy async systems, minutes or more during incidents.
- Metrics: replication delay seconds, bytes behind, log position, apply rate, network errors.
- RPO impact: maximum accepted lag should be tied to business target.

### 12. Failure Modes

- Network partition stops replication.
- Replica apply falls behind during write burst.
- Failover promotes stale data.
- Recovery: pause stale reads, catch up replica, choose data-loss cutoff, reconcile after failback.

### 13. Scenario

- Product / system: EU read replica for US booking database.
- Why this concept fits: EU reads are faster but may be stale after US writes.
- What would go wrong without it: user books in US and immediately sees old status in EU.

### 14. Code Sample

```python
def can_use_replica(replication_lag_seconds: int, freshness_slo_seconds: int) -> bool:
    # Staff concept: route reads based on freshness requirement, not only replica health.
    return replication_lag_seconds <= freshness_slo_seconds
```

### 15. Mini Program / Simulation

```python
def choose_read_source(lag_seconds: int, requires_fresh_read: bool) -> str:
    if requires_fresh_read:
        return "primary-region"
    if can_use_replica(lag_seconds, freshness_slo_seconds=5):
        return "nearest-replica"
    return "primary-region"


print(choose_read_source(2, False))
print(choose_read_source(20, False))
print(choose_read_source(2, True))
```

### 16. Practical Question

> You are serving reads from cross-region replicas. How would replication lag affect your design?

### 17. Strong Answer

I would treat replication lag as a correctness and DR metric, not just an operational detail. Async replicas can lower latency and support DR, but they may return stale data and failover may lose recent writes. I would route freshness-critical reads to the primary or use quorum/synchronous mechanisms where needed. I would monitor time lag, apply lag, and last applied log position, and make failover decisions against declared RPO.

### 18. Revision Notes

- One-line summary: replication lag is the freshness gap between regions.
- Three keywords: stale, async, RPO.
- One interview trap: serving all reads from nearest replica without freshness rules.
- One memory trick: remote copy is a shadow that may be behind.

---

## 7.2.4 Geo-Fencing and Data Residency

### 1. Intuition

Geo-fencing says where traffic or data may go. Data residency says where data must live. Staff-level design treats geography as a policy boundary, not just a latency optimization.

### 2. Definition

- Definition: geo-fencing restricts access, routing, or processing by geography; data residency controls where data is stored and processed.
- Category: compliance-aware architecture.
- Core idea: legal, contractual, and business rules can constrain data placement and traffic routing.

### 3. Why It Exists

Some data must remain in specific countries or regions due to regulation, customer contracts, or risk policy. Naive global replication can violate those obligations.

### 4. Reality

- Where used: financial services, healthcare, government, enterprise SaaS, EU customer data.
- Systems/products: regional data stores, tenant routing, policy engines, data catalogs, access controls.
- Teams: platform, security/compliance, legal, data governance, regional infrastructure.

### 5. How It Works

1. Classify data by residency and sensitivity.
2. Assign tenant/user/data to allowed regions.
3. Route requests to permitted processing locations.
4. Store and replicate data only to approved regions.
5. Audit access and data movement continuously.

### 6. What Problem It Solves

- Primary problem solved: preventing illegal or contract-breaking data movement.
- Secondary benefits: customer trust, auditability, regional isolation.
- Systems impact: constrains database replication, backups, logs, analytics, and support tooling.

### 7. When to Rely on It

- Use when customers, laws, or contracts specify data location requirements.
- Strong fit for PII, regulated data, government tenants, and enterprise deployments.
- Interview keywords: residency, sovereignty, regional tenant, PII, audit, policy engine.

### 8. When Not to Use It

- Avoid over-restricting non-sensitive data if it hurts reliability/cost without benefit.
- Avoid IP-only geography as the source of truth for legal data residency.
- Use data classification and tenant policy rather than only request location.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Supports compliance and contracts | Reduces placement and failover flexibility |
| Improves customer trust | Complicates analytics, backups, and support |
| Clarifies data ownership | Can increase regional infrastructure cost |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: compliance and trust.
- Give up: unconstrained global replication.
- Latency/cost impact: local processing may improve latency but increase duplicated regional stacks.

#### Common Mistakes

- Mistake: only fencing application DB. Better approach: include logs, backups, analytics, caches, and support exports.
- Mistake: routing by user IP alone. Better approach: route by tenant/data policy.
- Mistake: failover to disallowed region. Better approach: predefine compliant failover targets.

### 11. Key Numbers

- Metrics: policy violations, cross-region data movement events, residency coverage, audit log completeness.
- Controls: tenant region mapping, data classification, replication allowlist.
- Review cadence: policy and region mappings should be regularly audited.

### 12. Failure Modes

- Backup replication sends restricted data to unauthorized region.
- Debug logs contain PII and are shipped globally.
- Emergency failover violates residency.
- Recovery: block data movement, purge unauthorized copies, audit access, update policy checks and runbooks.

### 13. Scenario

- Product / system: enterprise SaaS with EU-only customer data commitment.
- Why this concept fits: customer data must stay in approved EU regions.
- What would go wrong without it: logs, replicas, or failover could move PII outside EU.

### 14. Code Sample

```python
def allowed_storage_region(tenant_policy: dict[str, list[str]], tenant_id: str, region: str) -> bool:
    # Staff concept: residency is evaluated from tenant policy, not just caller IP.
    return region in tenant_policy.get(tenant_id, [])
```

### 15. Mini Program / Simulation

```python
tenant_policy = {"tenant-eu": ["eu-west", "eu-central"], "tenant-us": ["us-east", "us-west"]}


def route_tenant_write(tenant_id: str, desired_region: str) -> str:
    if allowed_storage_region(tenant_policy, tenant_id, desired_region):
        return f"write to {desired_region}"
    return "reject residency violation"


print(route_tenant_write("tenant-eu", "eu-west"))
print(route_tenant_write("tenant-eu", "us-east"))
```

### 16. Practical Question

> You are designing multi-region SaaS for EU and US enterprise customers. How would geo-fencing and data residency affect the design?

### 17. Strong Answer

I would classify data and assign each tenant a residency policy that controls storage, processing, backups, logs, analytics, and failover. It fits because legal and contractual boundaries can override pure latency or availability choices. The trade-off is reduced failover flexibility and higher regional cost. I would use policy-based routing, regional data stores, audit logs, replication allowlists, and compliant DR targets.

### 18. Revision Notes

- One-line summary: data residency makes geography a correctness constraint.
- Three keywords: tenant policy, PII, audit.
- One interview trap: forgetting logs and backups.
- One memory trick: data has a passport too.

---

## 7.2.5 Region Failover Strategies

### 1. Intuition

Region failover is the planned move from a sick region to a healthier one. The hard part is not the traffic switch; it is knowing when, where, and with what data state.

### 2. Definition

- Definition: a region failover strategy defines how traffic, writes, reads, dependencies, and operations move when a region fails.
- Category: disaster recovery execution.
- Core idea: failover is a practiced workflow, not an improvised DNS edit.

### 3. Why It Exists

Regional failures are messy and partial. Without a strategy, teams argue during incidents, promote stale data, overload backup regions, or split traffic incorrectly.

### 4. Reality

- Where used: multi-region APIs, databases, queues, object storage, SaaS control planes.
- Systems/products: traffic managers, runbooks, database promotion, regional evacuation tooling.
- Teams: SRE, incident response, database, platform, service owners.

### 5. How It Works

1. Detect regional degradation using deep health and user-impact metrics.
2. Decide failover based on severity, RPO/RTO, and data state.
3. Fence writes in the old region if needed.
4. Promote or activate target region.
5. Shift traffic gradually or immediately, then validate and plan failback.

### 6. What Problem It Solves

- Primary problem solved: controlled recovery from regional outage.
- Secondary benefits: lower incident confusion, lower data-loss risk, repeatable operations.
- Systems impact: affects routing, data consistency, dependency recovery, capacity, and customer communication.

### 7. When to Rely on It

- Use for any system claiming regional resilience.
- Strong fit for critical APIs, databases, login, payments, and control planes.
- Interview keywords: failover, failback, traffic drain, write fencing, promotion, runbook.

### 8. When Not to Use It

- Avoid automatic failover when partial failures can cause flapping or split-brain.
- Avoid failover to an untested or under-capacity region.
- Use graceful degradation if backup region cannot safely take writes.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces downtime during regional incidents | Failover can lose data if replication lag exists |
| Makes incident response repeatable | Automation can make bad decisions if signals are weak |
| Supports DR commitments | Requires drills and capacity planning |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: faster recovery and clearer decisions.
- Give up: simple steady-state-only operations.
- Latency/cost impact: standby capacity and failover tooling cost money but lower outage impact.

#### Common Mistakes

- Mistake: no write fencing before promotion. Better approach: prevent old-primary writes.
- Mistake: failover runbook never tested. Better approach: scheduled game days.
- Mistake: failback is ignored. Better approach: plan data reconciliation and traffic return.

### 11. Key Numbers

- Metrics: failover decision time, traffic shift time, replica lag, target capacity, error rate after shift.
- RTO: measured from incident start or declared failover start, depending on agreement.
- Capacity: backup region must handle expected post-failover load.

### 12. Failure Modes

- Failover target lacks capacity.
- Old region accepts writes after new region is promoted.
- Traffic flaps between regions.
- Recovery: write fencing, traffic freeze, manual override, gradual shift, reconciliation jobs.

### 13. Scenario

- Product / system: booking API primary region loses database connectivity.
- Why this concept fits: app health is partial but user writes are failing.
- What would go wrong without it: random traffic shifts and stale promotion create inconsistent bookings.

### 14. Code Sample

```python
def should_failover(error_rate: float, replica_lag_seconds: int, max_rpo_seconds: int) -> bool:
    # Staff concept: failover decision must consider user impact and data loss tolerance.
    return error_rate > 0.20 and replica_lag_seconds <= max_rpo_seconds
```

### 15. Mini Program / Simulation

```python
def failover_step(primary_accepting_writes: bool, target_promoted: bool) -> str:
    if primary_accepting_writes:
        return "fence-primary-writes"
    if not target_promoted:
        return "promote-target-region"
    return "shift-global-traffic"


print(failover_step(True, False))
print(failover_step(False, False))
print(failover_step(False, True))
```

### 16. Practical Question

> You are designing regional failover for a booking write path. How would you structure the strategy?

### 17. Strong Answer

I would define clear detection signals, RPO/RTO thresholds, write fencing, target promotion, traffic shift, validation, and failback. It fits because failover needs to be repeatable under stress. The trade-off is standby cost, automation risk, and reconciliation complexity. I would test failovers regularly, monitor replica lag and target capacity, and avoid automatic promotion if signals cannot prevent split-brain.

### 18. Revision Notes

- One-line summary: failover is a practiced sequence, not only a route change.
- Three keywords: fence, promote, shift.
- One interview trap: forgetting failback.
- One memory trick: failover has a checklist, not a panic button.

---

## 7.2.6 Split-Brain Avoidance

### 1. Intuition

Split-brain is when two sides both think they are the leader. The system continues moving, but now it may create conflicting truths.

### 2. Definition

- Definition: split-brain is a failure mode where multiple partitions or nodes independently accept conflicting authoritative writes.
- Category: distributed coordination and high availability.
- Core idea: preserve a single source of authority during partitions and failover.

### 3. Why It Exists

Networks partition, health checks lie, and failover automation can promote a standby while the old primary is still alive. Without fencing or quorum, two primaries can write divergent states.

### 4. Reality

- Where used: database clusters, leader election, distributed locks, regional failover, storage systems.
- Systems/products: ZooKeeper/etcd/Consul, database failover managers, quorum-based systems.
- Teams: database, infrastructure, SRE, platform.

### 5. How It Works

1. System detects leader or region failure.
2. Coordination layer checks quorum or lease validity.
3. Old leader is fenced from accepting writes.
4. New leader is promoted only with authority.
5. Clients route writes only to the current fenced leader.

### 6. What Problem It Solves

- Primary problem solved: preventing conflicting authoritative writes during partitions.
- Secondary benefits: safer failover, clearer recovery, stronger data correctness.
- Systems impact: may reduce availability to preserve correctness.

### 7. When to Rely on It

- Use for critical write paths where divergent state is unacceptable.
- Strong fit for payments, inventory reservations, account balances, primary database promotion.
- Interview keywords: quorum, fencing token, lease, leader election, partition, dual primary.

### 8. When Not to Use It

- Avoid strong split-brain prevention only when domain can merge conflicts safely.
- Avoid availability-at-all-costs for non-mergeable data.
- Use CRDTs or domain merge rules for truly multi-writer eventually consistent data.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Protects data correctness | May reject writes during uncertainty |
| Makes failover safer | Requires coordination infrastructure |
| Reduces reconciliation pain | Can increase latency for leadership changes |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: single authoritative writer and safer recovery.
- Give up: some availability during network uncertainty.
- Latency/cost impact: quorum/coordination cost is paid to avoid corruption.

#### Common Mistakes

- Mistake: health check failure means safe to promote. Better approach: fence old primary first.
- Mistake: local locks across regions. Better approach: quorum-backed leadership.
- Mistake: no fencing token. Better approach: downstream systems reject stale leaders.

### 11. Key Numbers

- Quorum: majority of coordination nodes, commonly 2 of 3 or 3 of 5.
- Metrics: leader changes, fencing failures, lease expiry, quorum availability, rejected stale writes.
- Lease duration: must exceed timing uncertainty with safety margin.

### 12. Failure Modes

- Two primaries accept writes after network partition.
- Stale leader continues writing to storage.
- Coordination quorum lost, so writes stop.
- Recovery: choose authoritative log, reconcile or discard conflicting writes, restore quorum, audit affected records.

### 13. Scenario

- Product / system: inventory reservation database during regional partition.
- Why this concept fits: two primaries could sell the same room.
- What would go wrong without it: conflicting reservations and customer-impacting data corruption.

### 14. Code Sample

```python
def write_allowed(node_token: int, current_fencing_token: int) -> bool:
    # Staff concept: stale leaders are fenced by monotonically increasing tokens.
    return node_token == current_fencing_token
```

### 15. Mini Program / Simulation

```python
class Storage:
    def __init__(self) -> None:
        self.current_token = 2

    def write(self, node_name: str, token: int, value: str) -> str:
        if not write_allowed(token, self.current_token):
            return f"reject stale leader {node_name}"
        return f"accept {value} from {node_name}"


storage = Storage()
print(storage.write("old-primary", 1, "reserve-room"))
print(storage.write("new-primary", 2, "reserve-room"))
```

### 16. Practical Question

> You are designing database failover for a payment system. How would you avoid split-brain?

### 17. Strong Answer

I would avoid split-brain using quorum-backed leader election, write fencing, and client routing that only trusts the current leader. It fits because payment state cannot be merged casually. The trade-off is reduced availability during uncertainty and dependency on coordination infrastructure. I would use fencing tokens, reject stale leaders downstream, monitor leader changes, and test partition scenarios.

### 18. Revision Notes

- One-line summary: split-brain avoidance prevents two leaders from writing two truths.
- Three keywords: quorum, fencing, leader.
- One interview trap: promoting standby without fencing old primary.
- One memory trick: one crown, one kingdom.

---

# Topic 7.3: Data Lifecycle and Storage Economics

## 7.3.1 Hot, Warm, and Cold Storage Tiers

### 1. Intuition

Not all data deserves expensive fast storage forever. Hot data is on the desk, warm data is in a nearby cabinet, and cold data is in a warehouse.

### 2. Definition

- Definition: storage tiers classify data by access frequency, latency need, durability, and cost.
- Category: storage architecture and cost optimization.
- Core idea: place data on the cheapest tier that still meets access and recovery needs.

### 3. Why It Exists

Storage grows faster than budgets. Keeping rarely accessed data on premium SSD/database storage wastes money, while moving active data too far away hurts latency and user experience.

### 4. Reality

- Where used: logs, metrics, events, media files, backups, analytics, customer documents.
- Systems/products: SSD databases, object storage, archive tiers, data lakes, warehouses.
- Teams: data platform, infrastructure, FinOps, SRE, analytics.

### 5. How It Works

1. Classify data by access pattern and business value.
2. Keep hot data on low-latency serving storage.
3. Move warm data to cheaper but queryable storage.
4. Move cold data to archival storage with slower restore.
5. Monitor access and promote/demote data as patterns change.

### 6. What Problem It Solves

- Primary problem solved: balancing storage cost against latency and retrieval needs.
- Secondary benefits: database size control, backup cost reduction, lifecycle governance.
- Systems impact: affects query latency, restore time, retention policy, and cost.

### 7. When to Rely on It

- Use when data volume grows and access frequency changes over time.
- Strong fit for logs, historical orders, media, compliance archives, and analytics.
- Interview keywords: hot/warm/cold, object storage, archive, retrieval latency, FinOps.

### 8. When Not to Use It

- Avoid tiering tiny datasets where complexity costs more than storage.
- Avoid moving data cold if product needs frequent low-latency access.
- Use compression, indexing, or pruning first if data remains hot.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces storage cost | Adds retrieval and lifecycle complexity |
| Keeps serving stores smaller | Cold restore can be slow |
| Aligns cost with access value | Misclassification can hurt UX |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: lower storage cost and smaller hot systems.
- Give up: uniform access latency.
- Latency/cost impact: cold storage is cheaper but slower and may charge retrieval fees.

#### Common Mistakes

- Mistake: tier by age only. Better approach: use age plus actual access pattern and business value.
- Mistake: no promotion path. Better approach: allow data to move back if it becomes hot.
- Mistake: archive without restore testing. Better approach: test retrieval and query flows.

### 11. Key Numbers

- Hot access: milliseconds to low seconds depending on store.
- Cold retrieval: minutes to hours depending on archive tier.
- Metrics: storage cost per TB, retrieval count, hot set size, cache hit ratio, restore time.

### 12. Failure Modes

- Data moved cold but needed for active customer flow.
- Archive restore exceeds incident or compliance deadline.
- Lifecycle job deletes wrong tier.
- Recovery: access-based promotion, lifecycle dry run, restore drills, policy versioning.

### 13. Scenario

- Product / system: booking history for 10 years.
- Why this concept fits: recent bookings are hot; old invoices are rarely accessed but must be retained.
- What would go wrong without it: OLTP databases grow expensive and slow.

### 14. Code Sample

```python
def storage_tier(days_since_access: int) -> str:
    if days_since_access <= 30:
        return "hot"
    if days_since_access <= 365:
        return "warm"
    # Staff concept: cold tier is chosen when low access justifies slower retrieval.
    return "cold"
```

### 15. Mini Program / Simulation

```python
objects = {"invoice-1": 5, "invoice-2": 120, "invoice-3": 900}


def plan_tier_moves(access_age_by_object: dict[str, int]) -> dict[str, str]:
    return {object_id: storage_tier(days) for object_id, days in access_age_by_object.items()}


print(plan_tier_moves(objects))
```

### 16. Practical Question

> You are storing 10 years of booking data. How would hot, warm, and cold tiers help, and what trade-offs would you consider?

### 17. Strong Answer

I would keep recent and frequently accessed records hot, move older queryable data to warm storage, and archive rarely accessed compliance data to cold storage. It fits because access frequency drops over time and cost matters at scale. The trade-off is slower retrieval and lifecycle complexity. I would use access metrics, restore tests, policy versioning, and a promotion path for data that becomes active again.

### 18. Revision Notes

- One-line summary: storage tiering matches data cost to access value.
- Three keywords: access, latency, cost.
- One interview trap: tiering only by age.
- One memory trick: desk, cabinet, warehouse.

---

## 7.3.2 Tiered Storage Policies

### 1. Intuition

Tiered storage policy is the rulebook that moves data between storage tiers automatically and safely.

### 2. Definition

- Definition: tiered storage policies define when and how data transitions between hot, warm, cold, and deletion states.
- Category: lifecycle automation.
- Core idea: automate storage movement according to access, age, compliance, and cost rules.

### 3. Why It Exists

Manual data movement does not scale. Without policies, expensive stores fill up, archives become inconsistent, and teams forget retention obligations.

### 4. Reality

- Where used: object storage lifecycle rules, data lake table policies, log retention, warehouse partitions.
- Systems/products: S3 lifecycle policies, GCS lifecycle, Iceberg/Delta table retention, Elasticsearch index lifecycle management.
- Teams: data platform, infra, observability, FinOps, governance.

### 5. How It Works

1. Data is tagged with class, owner, creation time, and access pattern.
2. Policy engine evaluates lifecycle rules.
3. Objects or partitions transition to another tier.
4. Deletion or legal hold rules are enforced.
5. Reports show cost, exceptions, and policy violations.

### 6. What Problem It Solves

- Primary problem solved: scalable and repeatable storage lifecycle management.
- Secondary benefits: cost savings, compliance enforcement, reduced manual operations.
- Systems impact: changes data availability, retrieval latency, and governance evidence.

### 7. When to Rely on It

- Use when storage volumes are large and lifecycle rules are predictable.
- Strong fit for logs, backups, media, table partitions, and archives.
- Interview keywords: lifecycle policy, object tags, retention, legal hold, cost optimization.

### 8. When Not to Use It

- Avoid automatic deletion without clear owner and recoverability.
- Avoid tiering encrypted/compliance data without verifying key and audit requirements.
- Use manual review for high-risk data classes until policy confidence is high.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Automates cost control | Bad policy can move/delete wrong data |
| Enforces retention consistently | Policy exceptions need governance |
| Reduces manual toil | Harder to reason about data location manually |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: repeatable automation and lower cost.
- Give up: purely manual data placement.
- Latency/cost impact: cheaper storage with more retrieval planning.

#### Common Mistakes

- Mistake: no dry-run or report mode. Better approach: preview affected objects before enforcing.
- Mistake: no data owner. Better approach: require owner and classification tags.
- Mistake: ignore legal hold. Better approach: policy engine must respect holds and retention locks.

### 11. Key Numbers

- Policy cadence: daily evaluation is common for object lifecycle.
- Metrics: objects transitioned, storage saved, retrieval cost, policy exceptions, deletion count.
- Tags: owner, data class, retention class, created date, last access date.

### 12. Failure Modes

- Policy deletes data still under legal hold.
- Transition breaks a downstream job expecting hot path.
- Missing tags cause data to remain expensive forever.
- Recovery: dry run, tag validation, restore window, policy rollback, owner approval.

### 13. Scenario

- Product / system: observability logs for thousands of services.
- Why this concept fits: recent logs need fast search; old logs need cheap retention.
- What would go wrong without it: log storage costs grow without bound.

### 14. Code Sample

```python
def lifecycle_action(days_old: int, legal_hold: bool) -> str:
    if legal_hold:
        return "retain"
    if days_old > 365:
        # Staff concept: policy must encode retention and deletion rules explicitly.
        return "delete"
    if days_old > 90:
        return "archive"
    return "keep-hot"
```

### 15. Mini Program / Simulation

```python
objects = [
    {"id": "log-a", "days_old": 10, "legal_hold": False},
    {"id": "log-b", "days_old": 120, "legal_hold": False},
    {"id": "log-c", "days_old": 500, "legal_hold": True},
]


print({item["id"]: lifecycle_action(item["days_old"], item["legal_hold"]) for item in objects})
```

### 16. Practical Question

> You are managing petabytes of logs. How would tiered storage policies help, and what trade-offs would you consider?

### 17. Strong Answer

I would use lifecycle policies driven by age, access, data classification, and legal hold to move logs from hot search to warm object storage and then archive or delete. It fits because manual movement cannot scale. The trade-off is policy risk and retrieval latency. I would use dry-run reports, owner tags, retention locks, restore tests, and exception dashboards.

### 18. Revision Notes

- One-line summary: tiered policies automate safe movement across storage classes.
- Three keywords: tags, lifecycle, legal hold.
- One interview trap: automatic deletion without dry run or ownership.
- One memory trick: policy is the conveyor belt between tiers.

---

## 7.3.3 Archival Pipelines

### 1. Intuition

An archival pipeline is a controlled assembly line that takes old or inactive data out of serving systems and stores it cheaply, durably, and retrievably.

### 2. Definition

- Definition: an archival pipeline extracts, validates, writes, indexes, and verifies data in long-term storage.
- Category: data lifecycle and compliance infrastructure.
- Core idea: archive is not just copying files; it must preserve integrity, discoverability, and restore ability.

### 3. Why It Exists

Serving databases and search clusters should not hold every old record forever. But archived data may still be needed for audits, customer exports, disputes, legal holds, or historical analysis.

### 4. Reality

- Where used: invoices, logs, audit events, media, warehouse snapshots, customer records.
- Systems/products: object storage, data lake tables, archive indexes, manifest files, checksum validation.
- Teams: data platform, compliance, finance, SRE, database teams.

### 5. How It Works

1. Select eligible data using policy.
2. Export data in partitioned format.
3. Write to archival storage with metadata and checksum.
4. Verify counts, checksums, and manifest completeness.
5. Mark source rows archived or delete after retention-safe validation.

### 6. What Problem It Solves

- Primary problem solved: moving old data out of expensive serving systems without losing it.
- Secondary benefits: compliance retention, lower database size, cheaper long-term storage.
- Systems impact: reduces hot storage pressure but requires restore and discovery paths.

### 7. When to Rely on It

- Use for data that must be retained but rarely queried online.
- Strong fit for financial records, audit logs, historical bookings, and old media.
- Interview keywords: manifest, checksum, partition, restore, retention, archive index.

### 8. When Not to Use It

- Avoid archiving data still needed in hot user journeys.
- Avoid deleting source data before archive verification.
- Use online tiering or query federation if users still need frequent access.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Shrinks serving systems | Restore path must be built and tested |
| Lowers long-term cost | Archive jobs can fail partially |
| Supports compliance retention | Discoverability can be poor without indexes |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: cheaper durable retention and smaller serving stores.
- Give up: immediate online queryability.
- Latency/cost impact: lower storage cost with slower restore and query access.

#### Common Mistakes

- Mistake: archive equals copy. Better approach: verify count, checksum, manifest, and restore.
- Mistake: no archive index. Better approach: store searchable metadata.
- Mistake: delete source too early. Better approach: two-phase archive and cleanup.

### 11. Key Numbers

- Metrics: archived rows/bytes, checksum failures, restore success rate, source cleanup lag.
- Partitioning: often by date, tenant, region, or data class.
- Restore objective: define expected time to retrieve archived records.

### 12. Failure Modes

- Partial archive leaves missing partitions.
- Corrupt archive discovered years later.
- Restore tool cannot find needed record.
- Recovery: manifests, checksums, duplicate copies where required, periodic restore drills.

### 13. Scenario

- Product / system: archive completed bookings older than seven years.
- Why this concept fits: data is rarely used but needed for audits and disputes.
- What would go wrong without it: primary booking database becomes too large and expensive.

### 14. Code Sample

```python
import hashlib


def checksum(payload: str) -> str:
    # Staff concept: archival pipelines verify integrity, not only file movement.
    return hashlib.sha256(payload.encode()).hexdigest()
```

### 15. Mini Program / Simulation

```python
from dataclasses import dataclass


@dataclass(frozen=True)
class ArchiveManifest:
    partition: str
    row_count: int
    checksum: str


def build_manifest(partition: str, records: list[str]) -> ArchiveManifest:
    payload = "|".join(records)
    return ArchiveManifest(partition, len(records), checksum(payload))


print(build_manifest("bookings/year=2018", ["b1", "b2", "b3"]))
```

### 16. Practical Question

> You are archiving old financial records out of an OLTP database. How would you design the archival pipeline?

### 17. Strong Answer

I would select eligible records by retention policy, export them to partitioned archival storage, write manifests and checksums, verify counts, then mark or remove source rows only after validation. It fits because old data must remain durable and retrievable but should not burden OLTP. The trade-off is restore latency and pipeline complexity. I would test restore, index archive metadata, and monitor partial failures.

### 18. Revision Notes

- One-line summary: archival pipelines move old data cheaply without losing proof or restore ability.
- Three keywords: manifest, checksum, restore.
- One interview trap: deleting source before archive verification.
- One memory trick: archive means store plus prove plus restore.

---

## 7.3.4 Data Retention Strategies

### 1. Intuition

Data retention strategy answers, "How long should we keep this, and why?" Keeping everything forever is usually expensive, risky, and unnecessary.

### 2. Definition

- Definition: data retention defines how long each data class is stored, when it is deleted, and which exceptions apply.
- Category: governance, compliance, and storage lifecycle.
- Core idea: retention should be intentional, documented, enforceable, and auditable.

### 3. Why It Exists

Data has cost and risk. More data increases storage cost, breach impact, legal discovery scope, and operational complexity. But deleting too early can break product, audit, or compliance needs.

### 4. Reality

- Where used: logs, user data, audit trails, payments, telemetry, backups, support tickets.
- Systems/products: retention catalogs, lifecycle policies, data governance platforms, deletion workflows.
- Teams: legal, compliance, security, data platform, product, SRE.

### 5. How It Works

1. Classify data by type and sensitivity.
2. Define retention period and legal basis.
3. Apply deletion, anonymization, or archival policy.
4. Handle exceptions like legal hold.
5. Audit enforcement and prove deletion/retention behavior.

### 6. What Problem It Solves

- Primary problem solved: balancing data usefulness, cost, compliance, and risk.
- Secondary benefits: smaller breach radius, lower storage cost, clearer ownership.
- Systems impact: affects schemas, backups, logs, analytics, and customer deletion workflows.

### 7. When to Rely on It

- Use for every production data class, especially PII, logs, payments, and audit data.
- Strong fit for compliance-heavy systems and large data estates.
- Interview keywords: retention period, legal hold, deletion, anonymization, audit.

### 8. When Not to Use It

- Do not apply one retention period to all data.
- Avoid deleting data required for legal, financial, or user-facing reasons.
- Use anonymization or aggregation when analytics value remains but identity is unnecessary.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces cost and risk | Requires classification and ownership |
| Supports compliance | Deletion across backups and replicas is hard |
| Clarifies data lifecycle | Incorrect deletion can break product/audit |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: lower cost, lower risk, better governance.
- Give up: unlimited historical detail.
- Latency/cost impact: deletion/anonymization jobs cost compute but reduce long-term storage and risk.

#### Common Mistakes

- Mistake: retain everything forever. Better approach: define value and obligation by data class.
- Mistake: delete primary DB but not logs/backups. Better approach: retention covers all copies.
- Mistake: no legal hold exception. Better approach: policy engine supports holds.

### 11. Key Numbers

- Metrics: data by class/age, deletion backlog, retention exceptions, legal holds, policy coverage.
- Retention: varies by business/legal need; document the rationale.
- Delete SLA: define how long enforcement can take after eligibility.

### 12. Failure Modes

- Sensitive logs retained longer than policy.
- Retention job deletes records under legal hold.
- Backups restore deleted data without reapplying policy.
- Recovery: policy audit, restore-time deletion replay, legal hold checks, data inventory improvements.

### 13. Scenario

- Product / system: customer support chat logs.
- Why this concept fits: logs help support and training but may include sensitive data.
- What would go wrong without it: long-lived sensitive data increases breach and compliance risk.

### 14. Code Sample

```python
def should_delete(days_old: int, retention_days: int, legal_hold: bool) -> bool:
    # Staff concept: retention deletion must respect legal hold exceptions.
    return days_old > retention_days and not legal_hold
```

### 15. Mini Program / Simulation

```python
records = [
    {"id": "chat-1", "days_old": 45, "retention": 30, "legal_hold": False},
    {"id": "chat-2", "days_old": 120, "retention": 30, "legal_hold": True},
]


print([record["id"] for record in records if should_delete(record["days_old"], record["retention"], record["legal_hold"])])
```

### 16. Practical Question

> You are defining retention for logs, payments, and support data. How would you approach it?

### 17. Strong Answer

I would classify data by sensitivity, business value, legal need, and product dependency, then define retention and deletion/anonymization policy per class. It fits because keeping everything forever is costly and risky. The trade-off is policy complexity and deletion correctness across replicas, logs, and backups. I would include legal holds, owner approval, audit evidence, and restore-time deletion replay.

### 18. Revision Notes

- One-line summary: retention decides how long each data class should live.
- Three keywords: class, hold, delete.
- One interview trap: forgetting backups and logs.
- One memory trick: every data type needs an expiration story.

---

## 7.3.5 GDPR-Style Deletions

### 1. Intuition

GDPR-style deletion is not just deleting one row. It is finding a person's data across systems, removing or anonymizing it safely, and proving the workflow happened.

### 2. Definition

- Definition: GDPR-style deletion is a user-data erasure workflow across primary stores, replicas, caches, logs, search indexes, and downstream systems.
- Category: privacy engineering and data governance.
- Core idea: personal data deletion must be complete, traceable, and compatible with legal retention exceptions.

### 3. Why It Exists

Users and regulations may require erasure or anonymization of personal data. Distributed systems create many copies, so naive deletion from the main database is incomplete.

### 4. Reality

- Where used: consumer apps, SaaS, marketplaces, payments-adjacent systems, analytics platforms.
- Systems/products: privacy workflows, data inventory, deletion orchestration, tombstone events, anonymization jobs.
- Teams: privacy engineering, security, data platform, legal/compliance, backend service owners.

### 5. How It Works

1. Receive verified deletion request.
2. Identify data subjects and data inventory locations.
3. Emit deletion command or tombstone event to owning services.
4. Services delete, anonymize, or retain with lawful exception.
5. Orchestrator tracks completion, retries, evidence, and audit trail.

### 6. What Problem It Solves

- Primary problem solved: consistent privacy erasure across distributed data copies.
- Secondary benefits: reduced privacy risk, compliance evidence, clearer data ownership.
- Systems impact: requires service ownership, idempotent deletion, audit logs, and downstream propagation.

### 7. When to Rely on It

- Use for personal data systems and user deletion requests.
- Strong fit when multiple services store user identifiers, profiles, events, or derived indexes.
- Interview keywords: data subject, erasure, tombstone, anonymization, data inventory, audit.

### 8. When Not to Use It

- Do not delete data that must be retained for legal, fraud, financial, or security obligations without policy review.
- Avoid hard deletion when anonymization satisfies privacy need and preserves aggregate analytics.
- Use retention exception workflows for regulated records.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces privacy and compliance risk | Hard across distributed copies |
| Gives auditable erasure workflow | Conflicts with retention/legal holds need handling |
| Clarifies service data ownership | Derived data and backups are complex |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: privacy compliance and user trust.
- Give up: easy reuse of all historical user-level data.
- Latency/cost impact: deletion orchestration adds async jobs and audit storage.

#### Common Mistakes

- Mistake: delete only user table. Better approach: inventory all systems and derived stores.
- Mistake: deletion event is not idempotent. Better approach: deletion commands can be retried safely.
- Mistake: no proof. Better approach: record service acknowledgements and exceptions.

### 11. Key Numbers

- Metrics: deletion request SLA, completion rate, failed services, retry count, exception count.
- Delete workflow: often asynchronous with deadline, retries, and manual review for exceptions.
- Coverage: percentage of services with registered personal-data inventory.

### 12. Failure Modes

- Search index keeps deleted profile.
- Analytics table stores raw user ID after account deletion.
- Backup restore reintroduces erased data.
- Recovery: tombstone replay, deletion ledger, restore-time scrub, service inventory audit.

### 13. Scenario

- Product / system: user requests account deletion from travel marketplace.
- Why this concept fits: profile, bookings, support chats, logs, recommendations, and search all may reference the user.
- What would go wrong without it: partial deletion creates privacy and trust failure.

### 14. Code Sample

```python
def anonymize_user_record(record: dict[str, str]) -> dict[str, str]:
    # Staff concept: anonymization can preserve non-personal aggregate value.
    return {**record, "email": "deleted", "name": "deleted", "user_id": f"deleted:{record['user_id']}"}
```

### 15. Mini Program / Simulation

```python
class DeletionOrchestrator:
    def __init__(self, services: list[str]) -> None:
        self.services = services
        self.completed: set[str] = set()

    def acknowledge(self, service: str) -> None:
        self.completed.add(service)

    def pending(self) -> list[str]:
        return [service for service in self.services if service not in self.completed]


orchestrator = DeletionOrchestrator(["profile", "search", "analytics"])
orchestrator.acknowledge("profile")
print(orchestrator.pending())
```

### 16. Practical Question

> You are implementing user deletion across microservices. How would you design GDPR-style deletion?

### 17. Strong Answer

I would maintain a data inventory, verify the request, then orchestrate idempotent deletion or anonymization commands to each owning service. It fits because personal data is distributed across primary stores, indexes, caches, logs, and analytics. The trade-off is async complexity and legal exceptions. I would keep a deletion ledger, service acknowledgements, retries, tombstone replay after restore, and audit evidence.

### 18. Revision Notes

- One-line summary: GDPR-style deletion is distributed erasure plus proof.
- Three keywords: inventory, tombstone, audit.
- One interview trap: deleting only the main user row.
- One memory trick: find, delete, prove.

---

## 7.3.6 Cost-Aware Storage Design

### 1. Intuition

Cost-aware storage design means every byte has a business reason to live where it lives. Staff-level engineers design for performance and the bill.

### 2. Definition

- Definition: cost-aware storage design chooses storage systems, layouts, retention, indexes, and access patterns with explicit cost-performance trade-offs.
- Category: storage architecture and FinOps.
- Core idea: optimize total cost while preserving required latency, durability, and correctness.

### 3. Why It Exists

At scale, storage costs include capacity, IOPS, replication, snapshots, indexes, network egress, retrieval, backup, and engineering operations. Naive designs overspend or underperform.

### 4. Reality

- Where used: data lakes, observability platforms, search, databases, backups, media systems.
- Systems/products: object storage, columnar formats, compression, partitioning, index lifecycle, reserved capacity.
- Teams: platform, data engineering, FinOps, SRE, product infrastructure.

### 5. How It Works

1. Understand access pattern and SLO.
2. Estimate data growth, query frequency, and retention.
3. Choose storage engine, format, partitioning, compression, and indexes.
4. Apply tiering, TTL, sampling, and archival rules.
5. Monitor cost per feature, tenant, query, and data class.

### 6. What Problem It Solves

- Primary problem solved: avoiding unsustainable storage spend without breaking product guarantees.
- Secondary benefits: better capacity planning, clearer ownership, simpler cost conversations.
- Systems impact: affects database schema, partitioning, cache strategy, retention, and analytics design.

### 7. When to Rely on It

- Use for high-volume data, long retention, expensive indexes, and multi-tenant systems.
- Strong fit when storage spend is a major product cost driver.
- Interview keywords: compression, partition pruning, TTL, egress, index cost, storage tier.

### 8. When Not to Use It

- Avoid premature cost optimization for small early-stage datasets.
- Avoid cost cuts that violate SLO, compliance, or recovery needs.
- Use simple storage first when scale is uncertain and migration is cheap.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces long-term spend | Adds design and measurement effort |
| Improves ownership by data class | Can over-optimize too early |
| Aligns architecture with business value | Cost-saving choices can hurt latency or flexibility |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: sustainable economics and better capacity planning.
- Give up: always using the fastest/easiest storage everywhere.
- Latency/cost impact: cheaper layouts often trade off retrieval speed or query flexibility.

#### Common Mistakes

- Mistake: optimize storage GB only. Better approach: include IOPS, indexes, egress, snapshots, and compute.
- Mistake: no owner for data growth. Better approach: tag cost by product, tenant, or pipeline.
- Mistake: delete before understanding value. Better approach: classify and measure usage first.

### 11. Key Numbers

- Metrics: cost per TB, cost per query, storage growth rate, index size ratio, egress cost, retrieval cost.
- Compression: can reduce storage significantly depending on data format.
- Replication: 3x replication triples raw stored bytes before compression/dedup effects.

### 12. Failure Modes

- Expensive secondary indexes grow larger than base data.
- Cross-region egress dominates storage savings.
- Cold retrieval fees surprise teams during incident.
- Recovery: cost dashboards, budgets/alerts, partition pruning, index lifecycle, egress-aware placement.

### 13. Scenario

- Product / system: observability platform storing logs and traces.
- Why this concept fits: data volume is enormous and query patterns vary by age.
- What would go wrong without it: storage and index costs can exceed product value.

### 14. Code Sample

```python
def monthly_storage_cost(terabytes: float, dollars_per_tb: float, replication_factor: int) -> float:
    # Staff concept: replication and tier choice are first-class cost drivers.
    return terabytes * dollars_per_tb * replication_factor
```

### 15. Mini Program / Simulation

```python
tiers = {"hot": 80.0, "warm": 20.0, "cold": 4.0}
data_by_tier_tb = {"hot": 20.0, "warm": 100.0, "cold": 500.0}


def total_monthly_cost(data_by_tier: dict[str, float]) -> float:
    return sum(monthly_storage_cost(tb, tiers[tier], 1) for tier, tb in data_by_tier.items())


print(total_monthly_cost(data_by_tier_tb))
```

### 16. Practical Question

> You are designing a log platform at petabyte scale. How would cost-aware storage design shape the architecture?

### 17. Strong Answer

I would model access patterns, retention, query frequency, index size, compression, replication, egress, and restore requirements. It fits because storage economics can dominate at petabyte scale. The trade-off is query flexibility and latency when moving older data to cheaper tiers. I would use hot indexes for recent data, object storage for older partitions, compression, lifecycle policies, and dashboards for cost per tenant and data class.

### 18. Revision Notes

- One-line summary: cost-aware storage chooses the cheapest tier that still meets the promise.
- Three keywords: tier, index, egress.
- One interview trap: counting only raw GB cost.
- One memory trick: every byte gets a job and a rent.

---

# Topic 7.4: Deployment, Migration, and Evolution

## 7.4.1 Infrastructure as Code Mindset

### 1. Intuition

Infrastructure as Code means infrastructure changes are reviewed, versioned, repeatable, and recoverable like application code.

### 2. Definition

- Definition: Infrastructure as Code manages infrastructure resources through declarative or scripted configuration stored in version control.
- Category: platform operations and deployment governance.
- Core idea: make infrastructure state reproducible and auditable.

### 3. Why It Exists

Manual console changes create drift, undocumented dependencies, slow recovery, and unreviewed risk. IaC gives teams repeatability and change control.

### 4. Reality

- Where used: cloud resources, Kubernetes, networking, IAM, monitoring, databases, CI/CD.
- Systems/products: Terraform, Pulumi, CloudFormation, CDK, Helm, Kustomize.
- Teams: platform, SRE, security, infrastructure, application teams.

### 5. How It Works

1. Desired infrastructure is written as code.
2. Changes are reviewed through pull requests.
3. Plan/diff shows intended changes.
4. Pipeline applies changes with controlled permissions.
5. Drift detection and state management keep reality aligned.

### 6. What Problem It Solves

- Primary problem solved: repeatable and auditable infrastructure changes.
- Secondary benefits: disaster recovery, environment consistency, reviewable security posture.
- Systems impact: improves governance but requires state, module, and ownership discipline.

### 7. When to Rely on It

- Use for any production infrastructure that must be reproducible.
- Strong fit for multi-environment, multi-region, compliance-sensitive systems.
- Interview keywords: declarative, plan, drift, state, modules, review.

### 8. When Not to Use It

- Avoid heavy IaC ceremony for quick local experiments that will not persist.
- Avoid unmanaged shared state and ad hoc module changes.
- Use break-glass manual changes only with post-incident reconciliation.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reproducible infrastructure | State management can be tricky |
| Reviewable and auditable changes | Bad IaC change can affect many resources |
| Reduces manual drift | Requires module and ownership discipline |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: repeatability, auditability, and faster recovery.
- Give up: casual manual changes.
- Latency/cost impact: slower small changes, safer large operations.

#### Common Mistakes

- Mistake: console hotfix never codified. Better approach: reconcile into IaC immediately.
- Mistake: no plan review. Better approach: require plan/diff review for risky changes.
- Mistake: one giant state file. Better approach: split state by ownership/blast radius.

### 11. Key Numbers

- Metrics: drift count, failed applies, plan size, change lead time, rollback time.
- State scope: split by environment, region, or ownership to reduce blast radius.
- Review: high-risk resources require security/platform approval.

### 12. Failure Modes

- State corruption blocks changes.
- Drift causes plan to destroy unexpected resource.
- Module update changes many environments at once.
- Recovery: state backups, drift detection, scoped applies, pinned module versions, break-glass runbooks.

### 13. Scenario

- Product / system: multi-region booking platform infrastructure.
- Why this concept fits: regions must be reproducible and reviewable.
- What would go wrong without it: failover environment differs from primary and breaks during incident.

### 14. Code Sample

```hcl
resource "aws_s3_bucket" "booking_archive" {
  bucket = "booking-archive-prod"

  tags = {
    owner = "data-platform"
    tier  = "archive"
  }
}
```

### 15. Mini Program / Simulation

```python
def detect_drift(desired: dict[str, str], actual: dict[str, str]) -> dict[str, tuple[str | None, str | None]]:
    keys = set(desired) | set(actual)
    # Staff concept: IaC mindset compares desired state with real state.
    return {key: (desired.get(key), actual.get(key)) for key in keys if desired.get(key) != actual.get(key)}


print(detect_drift({"instance_type": "m6i.large"}, {"instance_type": "m6i.xlarge"}))
```

### 16. Practical Question

> You are building infrastructure for a regulated multi-region system. How would an IaC mindset help?

### 17. Strong Answer

I would manage production infrastructure through versioned IaC with plan review, scoped state, modules, and drift detection. It fits because multi-region systems must be reproducible and auditable. The trade-off is change discipline and state management complexity. I would avoid console-only changes, split state by blast radius, back up state, and require approvals for risky resources like IAM, networking, and databases.

### 18. Revision Notes

- One-line summary: IaC makes infrastructure reviewable, repeatable, and recoverable.
- Three keywords: plan, state, drift.
- One interview trap: forgetting to reconcile manual changes.
- One memory trick: infra should have a git history.

---

## 7.4.2 Immutable Infrastructure

### 1. Intuition

Immutable infrastructure replaces servers instead of patching them in place. If you need a new version, build a new image and roll it out.

### 2. Definition

- Definition: immutable infrastructure deploys new infrastructure artifacts rather than modifying running instances manually.
- Category: deployment reliability and environment consistency.
- Core idea: reduce drift by treating instances/containers as disposable outputs of a build.

### 3. Why It Exists

Mutable servers accumulate snowflake changes. Over time, environments differ, rollbacks are unclear, and incident recovery becomes guesswork.

### 4. Reality

- Where used: container deployments, VM images, autoscaling groups, Kubernetes, golden images.
- Systems/products: Docker images, AMIs, Packer, Kubernetes Deployments, blue-green stacks.
- Teams: platform, SRE, backend, release engineering.

### 5. How It Works

1. Build versioned artifact or machine image.
2. Deploy new instances/pods from that artifact.
3. Shift traffic after health checks pass.
4. Remove old instances after drain period.
5. Roll back by redeploying previous artifact, not hand-editing servers.

### 6. What Problem It Solves

- Primary problem solved: configuration drift and unreproducible runtime environments.
- Secondary benefits: safer rollback, cleaner autoscaling, predictable recovery.
- Systems impact: improves release confidence but requires image/build pipelines.

### 7. When to Rely on It

- Use for production services, autoscaling fleets, Kubernetes workloads, and regulated systems.
- Strong fit when consistency and rollback matter.
- Interview keywords: golden image, container image, replace not patch, drift, rollout.

### 8. When Not to Use It

- Avoid for stateful data stored only on instance disk.
- Avoid manual hot patches except emergency break-glass, followed by image rebuild.
- Use configuration management carefully for legacy mutable fleets during migration.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces drift | Requires artifact build pipeline |
| Simplifies rollback | Stateful workloads need careful data separation |
| Makes scaling predictable | Image rollout can be slower than small config tweak |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: consistency and repeatable rollback.
- Give up: quick manual edits on live hosts.
- Latency/cost impact: more build/deploy work, fewer runtime surprises.

#### Common Mistakes

- Mistake: SSH patching production and leaving it. Better approach: rebuild artifact and redeploy.
- Mistake: storing state on disposable instance. Better approach: external durable state.
- Mistake: unversioned images. Better approach: immutable tags/digests.

### 11. Key Numbers

- Metrics: image age, deployment success rate, rollback time, drift incidents, instance replacement time.
- Rollout: canary or rolling update percentages should fit service capacity.
- Artifact: use immutable version/digest for traceability.

### 12. Failure Modes

- Bad image deployed to entire fleet.
- Instances cannot drain connections before termination.
- Stateful data lost on replacement.
- Recovery: canary rollout, health gates, connection draining, previous image rollback, external storage.

### 13. Scenario

- Product / system: checkout service running on Kubernetes.
- Why this concept fits: each pod should be replaceable and identical.
- What would go wrong without it: emergency host edits create different behavior across replicas.

### 14. Code Sample

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: checkout
spec:
  template:
    spec:
      containers:
        - name: checkout
          image: checkout-service:2026.06.17-1
```

### 15. Mini Program / Simulation

```python
def rollout_action(current_version: str, desired_version: str) -> str:
    if current_version == desired_version:
        return "no-op"
    # Staff concept: immutable infra replaces versioned artifacts, not live patching.
    return f"replace instances with {desired_version}"


print(rollout_action("v1", "v2"))
```

### 16. Practical Question

> You are migrating a manually patched fleet to safer deployments. How would immutable infrastructure help?

### 17. Strong Answer

I would build versioned images and replace instances through controlled rollouts rather than modifying live servers. It fits because immutable artifacts reduce drift and make rollback clear. The trade-off is build pipeline discipline and state separation. I would externalize state, canary images, use immutable tags or digests, drain old instances, and keep break-glass changes temporary and reconciled.

### 18. Revision Notes

- One-line summary: immutable infrastructure replaces instances instead of patching them.
- Three keywords: image, replace, drift.
- One interview trap: keeping state on disposable servers.
- One memory trick: do not repair the snowflake; melt and recreate it.

---

## 7.4.3 Feature Flags

### 1. Intuition

Feature flags separate deployment from release. Code can be deployed safely while behavior is enabled for specific users, percentages, tenants, or environments.

### 2. Definition

- Definition: a feature flag is a runtime-controlled switch that changes behavior without redeploying code.
- Category: release management and progressive delivery.
- Core idea: reduce release risk by controlling exposure dynamically.

### 3. Why It Exists

Deploying code to all users at once makes rollback blunt and risky. Feature flags let teams test, canary, disable, or personalize behavior after deployment.

### 4. Reality

- Where used: product launches, experiments, migrations, kill switches, entitlement gates.
- Systems/products: LaunchDarkly, Unleash, internal config services, dynamic config platforms.
- Teams: product engineering, platform, experimentation, SRE.

### 5. How It Works

1. Code checks flag state at runtime.
2. Flag service evaluates targeting rules.
3. Feature is enabled for selected users/tenants/percentages.
4. Metrics compare behavior and errors.
5. Flag is ramped, disabled, or removed after completion.

### 6. What Problem It Solves

- Primary problem solved: reducing release blast radius and enabling dynamic control.
- Secondary benefits: experiments, kill switches, migration switches, tenant rollout.
- Systems impact: improves release safety but creates configuration and cleanup burden.

### 7. When to Rely on It

- Use for risky launches, gradual rollout, migrations, and operational kill switches.
- Strong fit when rollback by redeploy is too slow or risky.
- Interview keywords: progressive delivery, kill switch, ramp, targeting, flag debt.

### 8. When Not to Use It

- Avoid flags for every tiny code path if cleanup discipline is weak.
- Avoid long-lived flags that create permanent complexity.
- Use normal config or separate deployments when runtime switching is unnecessary.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Separates deploy from release | Creates flag debt if not removed |
| Enables quick disable | Adds testing combinations |
| Supports targeted rollout | Flag service dependency matters |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: controlled exposure and fast mitigation.
- Give up: simpler code paths.
- Latency/cost impact: flag evaluation adds small overhead and operational dependency.

#### Common Mistakes

- Mistake: no owner or expiry. Better approach: every flag has owner and cleanup date.
- Mistake: flag defaults unsafe when config unavailable. Better approach: define safe fallback per flag.
- Mistake: no metrics by flag state. Better approach: compare errors/latency/conversion for on vs off.

### 11. Key Numbers

- Ramp pattern: 1%, 5%, 10%, 25%, 50%, 100% when risk is high.
- Metrics: exposure count, error rate by variant, latency by variant, rollback time, stale flag count.
- Flag TTL: short-lived release flags should be removed after rollout stabilizes.

### 12. Failure Modes

- Bad default enables risky feature for everyone.
- Flag service outage changes behavior unexpectedly.
- Old flags interact and create untested combinations.
- Recovery: safe defaults, local cache, kill switch, ownership/expiry, flag cleanup reviews.

### 13. Scenario

- Product / system: new checkout pricing engine.
- Why this concept fits: pricing can be enabled for a small percentage and disabled quickly.
- What would go wrong without it: bad pricing logic reaches all users and rollback needs redeploy.

### 14. Code Sample

```python
def price_engine(flag_enabled: bool) -> str:
    # Staff concept: flag separates code deployment from feature exposure.
    return "new-pricing" if flag_enabled else "legacy-pricing"
```

### 15. Mini Program / Simulation

```python
def percentage_flag(user_id: int, rollout_percent: int) -> bool:
    return user_id % 100 < rollout_percent


users = range(10)
print([price_engine(percentage_flag(user_id, 20)) for user_id in users])
```

### 16. Practical Question

> You are launching a new checkout flow. How would feature flags help, and what trade-offs would you consider?

### 17. Strong Answer

I would deploy the code behind a feature flag, start with internal users or 1%, watch metrics, then ramp gradually. It fits because checkout risk needs controlled exposure and quick disable. The trade-off is extra code paths and flag lifecycle management. I would define safe defaults, owner, expiry, metrics by flag state, and a cleanup plan after rollout.

### 18. Revision Notes

- One-line summary: feature flags decouple deploy from release.
- Three keywords: target, ramp, cleanup.
- One interview trap: leaving flags forever.
- One memory trick: deploy the wire, flip the switch later.

---

## 7.4.4 Dark Launches

### 1. Intuition

A dark launch sends real production-like traffic to new code without showing its response to users. The new path runs in the shadows so you can measure it before trusting it.

### 2. Definition

- Definition: a dark launch exercises new functionality in production without exposing its output as user-visible behavior.
- Category: progressive delivery and production validation.
- Core idea: validate performance and correctness under real traffic before release.

### 3. Why It Exists

Staging rarely matches production traffic. Dark launches reveal load, latency, data-shape, and dependency issues before users depend on the new path.

### 4. Reality

- Where used: search ranking, recommendations, pricing, ML models, migrations, new services.
- Systems/products: shadow writes, tee traffic, compare pipelines, feature flags, observability dashboards.
- Teams: platform, product engineering, ML, SRE, migrations.

### 5. How It Works

1. Production request enters normal path.
2. A copy or derived request is sent to new path.
3. New path response is recorded but not returned to user.
4. Metrics compare latency, errors, and output differences.
5. Team fixes issues before progressive release.

### 6. What Problem It Solves

- Primary problem solved: validating new behavior under real production conditions without user impact.
- Secondary benefits: load testing, correctness comparison, migration confidence.
- Systems impact: increases confidence but adds duplicate work and isolation needs.

### 7. When to Rely on It

- Use for risky backends, expensive query paths, ranking/pricing changes, and migrations.
- Strong fit when real traffic distribution matters.
- Interview keywords: shadow traffic, non-user-visible, compare, side effects, production validation.

### 8. When Not to Use It

- Avoid dark launching paths with unsafe side effects unless writes are isolated.
- Avoid duplicating traffic if downstream cannot handle extra load.
- Use synthetic load or canary if shadowing is not safe or useful.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Validates with real traffic | Doubles or increases backend load |
| Reduces user-visible risk | Side effects must be blocked or isolated |
| Supports output comparison | Requires metrics and diff tooling |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: realistic validation before exposure.
- Give up: lower resource usage during launch.
- Latency/cost impact: user path should not wait on shadow path, but infrastructure cost rises.

#### Common Mistakes

- Mistake: shadow path writes to production state. Better approach: no side effects or isolated shadow stores.
- Mistake: user request waits for dark path. Better approach: async tee where possible.
- Mistake: no comparison metric. Better approach: define expected diff bounds.

### 11. Key Numbers

- Metrics: shadow error rate, shadow latency, diff rate, added QPS, downstream saturation.
- Ramp: shadow 1%, 5%, 25%, 100% depending on capacity.
- Rule: shadow traffic must fit spare capacity or be throttled.

### 12. Failure Modes

- Shadow traffic overloads a dependency.
- Dark path mutates production state.
- Diff tooling misses important semantic differences.
- Recovery: throttle shadowing, isolate writes, kill switch, compare business metrics and raw outputs.

### 13. Scenario

- Product / system: new hotel ranking service.
- Why this concept fits: ranking quality and latency need real traffic validation.
- What would go wrong without it: release may fail under real query mix despite staging success.

### 14. Code Sample

```python
def handle_search(query: str, dark_launch: bool) -> str:
    visible_result = "legacy-ranking"
    if dark_launch:
        # Staff concept: dark path runs but does not control user response.
        shadow_result = "new-ranking"
        print(f"compare {visible_result} vs {shadow_result}")
    return visible_result
```

### 15. Mini Program / Simulation

```python
def diff_rate(legacy_results: list[str], shadow_results: list[str]) -> float:
    differences = sum(1 for legacy, shadow in zip(legacy_results, shadow_results) if legacy != shadow)
    return differences / len(legacy_results)


print(diff_rate(["a", "b", "c"], ["a", "x", "c"]))
```

### 16. Practical Question

> You are launching a new recommendation service. How would a dark launch help?

### 17. Strong Answer

I would shadow a controlled percentage of real traffic to the new recommendation service while still returning legacy results to users. It fits because production traffic distribution matters and user-visible risk should be low. The trade-off is extra load and the need to avoid side effects. I would throttle shadow traffic, isolate writes, compare outputs and latency, and keep a kill switch.

### 18. Revision Notes

- One-line summary: dark launch runs new code under real traffic without user-visible output.
- Three keywords: shadow, compare, no side effects.
- One interview trap: letting dark traffic mutate production.
- One memory trick: rehearse on stage before opening the curtain.

---

## 7.4.5 Schema Versioning

### 1. Intuition

Schema versioning lets producers and consumers evolve without breaking each other. It is a compatibility contract for data shape.

### 2. Definition

- Definition: schema versioning tracks changes to API, event, database, or file schemas and defines compatibility rules.
- Category: evolution and contract management.
- Core idea: make data changes explicit, compatible, and governable across services.

### 3. Why It Exists

Distributed systems have old producers, new producers, old consumers, backfills, replay, and stored historical data. If schema changes are unmanaged, consumers break.

### 4. Reality

- Where used: events, APIs, databases, warehouses, CDC, protobuf/Avro/JSON schemas.
- Systems/products: schema registries, API versioning, protobuf field rules, database migration tools.
- Teams: platform, data engineering, backend service owners, integration teams.

### 5. How It Works

1. Define schema and version.
2. Validate changes against compatibility mode.
3. Producers publish with version or compatible encoding.
4. Consumers tolerate old/new fields as required.
5. Deprecated fields are removed only after consumers migrate.

### 6. What Problem It Solves

- Primary problem solved: safe evolution of data contracts across independent deployments.
- Secondary benefits: replay safety, integration stability, clearer ownership.
- Systems impact: affects APIs, event streams, storage formats, and migrations.

### 7. When to Rely on It

- Use whenever producers and consumers deploy independently.
- Strong fit for events, CDC, public APIs, data lake tables, and shared contracts.
- Interview keywords: backward compatibility, forward compatibility, schema registry, optional field, deprecation.

### 8. When Not to Use It

- Avoid heavyweight schema governance for private in-process objects.
- Avoid breaking changes unless versioned explicitly or all consumers migrate first.
- Use expand-contract for database schemas and additive changes for events when possible.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Prevents consumer breakage | Adds governance and validation steps |
| Supports independent deploys | Compatibility rules require discipline |
| Improves replay/backfill safety | Old versions may linger |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: safe evolution and independent deployment.
- Give up: casual breaking changes.
- Latency/cost impact: validation overhead is small; coordination cost saves outages.

#### Common Mistakes

- Mistake: deleting required field immediately. Better approach: deprecate, migrate consumers, then remove.
- Mistake: using one version label with no compatibility rules. Better approach: enforce backward/forward policy.
- Mistake: no replay test. Better approach: test consumers against historical messages.

### 11. Key Numbers

- Metrics: schema compatibility failures, consumers per version, deprecated field age, replay failures.
- Compatibility modes: backward, forward, full, none.
- Versioning: major versions for breaking changes, additive fields for compatible evolution.

### 12. Failure Modes

- New producer emits field type change that breaks old consumer.
- Replay of old events fails against new schema.
- CDC schema change breaks downstream parser.
- Recovery: schema rollback, compatibility gate, consumer pinning, dual-write/versioned topic if needed.

### 13. Scenario

- Product / system: booking events consumed by analytics, search, and notifications.
- Why this concept fits: many consumers deploy independently and replay old events.
- What would go wrong without it: one event shape change breaks downstream services.

### 14. Code Sample

```json
{
  "schema": "booking_created",
  "version": 2,
  "required": ["booking_id", "user_id"],
  "optional": ["coupon_code"]
}
```

### 15. Mini Program / Simulation

```python
def compatible_change(old_required: set[str], new_required: set[str]) -> bool:
    # Staff concept: adding a new required field can break old producers/consumers.
    return new_required.issubset(old_required)


print(compatible_change({"booking_id", "user_id"}, {"booking_id", "user_id"}))
print(compatible_change({"booking_id", "user_id"}, {"booking_id", "user_id", "coupon_code"}))
```

### 16. Practical Question

> You are evolving event schemas used by many consumers. How would schema versioning help?

### 17. Strong Answer

I would use a schema registry or compatibility gate so producers cannot publish breaking changes without explicit versioning. It fits because consumers deploy independently and may replay historical events. The trade-off is governance and cleanup work. I would prefer additive optional fields, deprecate before removal, test replay, track consumers by version, and use major versions or new topics for breaking changes.

### 18. Revision Notes

- One-line summary: schema versioning makes data evolution explicit and compatible.
- Three keywords: compatibility, deprecation, replay.
- One interview trap: adding required fields casually.
- One memory trick: schema is a contract with history.

---

## 7.4.6 Online Migrations

### 1. Intuition

Online migration is changing the engine while the car is moving. Users continue using the system while data, code, or infrastructure transitions in controlled phases.

### 2. Definition

- Definition: online migration changes data, schema, service, or infrastructure while keeping the system available.
- Category: production evolution and migration safety.
- Core idea: migrate incrementally with compatibility, validation, and rollback/roll-forward paths.

### 3. Why It Exists

Large production systems cannot stop for a big-bang migration. One-shot changes risk downtime, data loss, and rollback impossibility.

### 4. Reality

- Where used: database migrations, service extraction, cloud migration, storage engine changes, queue migrations.
- Systems/products: expand-contract, backfills, dual reads/writes, shadow traffic, CDC, feature flags.
- Teams: backend, platform, database, SRE, migration task forces.

### 5. How It Works

1. Prepare compatible destination and code paths.
2. Start dual write, CDC, or backfill depending on migration type.
3. Validate source and target consistency.
4. Shift reads gradually.
5. Stop old path and clean up only after stability.

### 6. What Problem It Solves

- Primary problem solved: changing critical production systems without downtime.
- Secondary benefits: reduced blast radius, measurable progress, safer rollback/roll-forward.
- Systems impact: affects code, data, traffic, observability, and incident response.

### 7. When to Rely on It

- Use for large datasets, critical services, multi-tenant systems, and rolling deployments.
- Strong fit when downtime or big-bang rollback is unacceptable.
- Interview keywords: expand-contract, backfill, dual write, validation, cutover, rollback.

### 8. When Not to Use It

- Avoid complex online migration for tiny low-risk data where maintenance window is acceptable.
- Avoid dual writes without reconciliation and idempotency.
- Use offline migration only when business downtime is acceptable and safer.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Avoids downtime | Adds temporary dual paths |
| Supports gradual validation | Migration can run for days or weeks |
| Reduces big-bang risk | Cleanup discipline is required |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: availability and incremental safety.
- Give up: short-term simplicity.
- Latency/cost impact: temporary dual writes/backfills add load and operational cost.

#### Common Mistakes

- Mistake: cutover before validation. Better approach: compare counts, checksums, and sampled reads.
- Mistake: no idempotency in backfill. Better approach: resumable chunks and safe retries.
- Mistake: cleanup immediately after cutover. Better approach: observe, then contract.

### 11. Key Numbers

- Metrics: backfill progress, mismatch count, dual-write error rate, lag, cutover error rate.
- Chunk size: tune to avoid lock, CPU, and replica lag problems.
- Cutover: ramp by tenant, region, percentage, or endpoint where possible.

### 12. Failure Modes

- Backfill overloads source database.
- Dual writes diverge.
- Read cutover exposes missing data.
- Recovery: pause migration, replay from source, rollback reads, fix mismatches, continue roll-forward if safer.

### 13. Scenario

- Product / system: migrate booking search from old index to new search cluster.
- Why this concept fits: search must stay available and data must match.
- What would go wrong without it: full cutover can expose missing or incorrect results.

### 14. Code Sample

```python
def choose_read_path(user_id: int, rollout_percent: int) -> str:
    # Staff concept: online migration shifts reads gradually after validation.
    return "new-store" if user_id % 100 < rollout_percent else "old-store"
```

### 15. Mini Program / Simulation

```python
def compare_records(source: dict[str, str], target: dict[str, str]) -> list[str]:
    mismatched = []
    for key, source_value in source.items():
        if target.get(key) != source_value:
            mismatched.append(key)
    return mismatched


print(compare_records({"b1": "ok", "b2": "paid"}, {"b1": "ok", "b2": "pending"}))
```

### 16. Practical Question

> You are migrating from one search backend to another with no downtime. How would you structure the online migration?

### 17. Strong Answer

I would build the new backend, backfill data in chunks, keep it updated through CDC or dual writes, validate counts and sampled records, shadow reads, then gradually shift read traffic with a flag. The trade-off is temporary dual-system complexity and extra load. I would monitor lag, mismatches, cutover errors, and have rollback for reads while keeping source as authority until confidence is high.

### 18. Revision Notes

- One-line summary: online migration changes production gradually while users keep working.
- Three keywords: backfill, validate, cutover.
- One interview trap: cutover before consistency checks.
- One memory trick: migrate with two rails before removing the old rail.

---

## 7.4.7 Roll-Forward vs Rollback

### 1. Intuition

Rollback goes back to the previous version. Roll-forward fixes the problem with a new version. Staff-level judgment is knowing which path is safer for the current failure.

### 2. Definition

- Definition: rollback reverts to a prior known version; roll-forward deploys a new fix or migration step to move the system to a safe state.
- Category: release recovery and incident response.
- Core idea: choose the recovery path that minimizes user impact, data risk, and operational uncertainty.

### 3. Why It Exists

Not every bad deployment can be safely reverted. Database migrations, data writes, external side effects, and client compatibility can make rollback dangerous. Sometimes the safest recovery is a forward fix.

### 4. Reality

- Where used: deploy pipelines, database migrations, mobile releases, event schemas, infrastructure changes.
- Systems/products: CI/CD rollback buttons, feature flags, migration runbooks, hotfix pipelines.
- Teams: release engineering, SRE, backend, database, incident commanders.

### 5. How It Works

1. Detect regression and assess blast radius.
2. Determine whether previous version is compatible with current data/state.
3. If safe, rollback artifact or disable flag.
4. If rollback is unsafe, roll forward with hotfix, config, or migration repair.
5. Validate recovery and record prevention actions.

### 6. What Problem It Solves

- Primary problem solved: selecting safe recovery action after bad release or migration.
- Secondary benefits: faster incident response, reduced data corruption risk, clearer release planning.
- Systems impact: affects deployment design, schema strategy, feature flags, and runbooks.

### 7. When to Rely on It

- Use rollback for stateless app regressions with backward-compatible state.
- Use roll-forward when data/schema/external side effects make rollback unsafe.
- Interview keywords: backward compatibility, irreversible migration, hotfix, kill switch, expand-contract.

### 8. When Not to Use It

- Do not rollback across incompatible schema or data changes.
- Do not roll forward blindly if a flag disable or rollback is safer.
- Use feature flags and expand-contract to keep both options available.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Rollback can be fast and simple | Rollback can break with changed schema/data |
| Roll-forward handles irreversible state | Roll-forward requires new fix under pressure |
| Both support recovery planning | Wrong choice can worsen incident |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: explicit recovery decision instead of panic.
- Give up: assuming one universal recovery button.
- Latency/cost impact: rollback may restore quickly; roll-forward may take longer but avoid data damage.

#### Common Mistakes

- Mistake: every deploy is rollback-safe. Better approach: label rollback safety before deploy.
- Mistake: irreversible DB migration in same deploy as app change. Better approach: expand-contract and staged cleanup.
- Mistake: no kill switch. Better approach: feature flags for risky behavior.

### 11. Key Numbers

- Metrics: time to mitigation, rollback success rate, hotfix lead time, failed rollback count, flag disable time.
- Release gates: require migration compatibility check before deploy.
- Recovery objective: align mitigation time with service SLO and incident severity.

### 12. Failure Modes

- Rollback app cannot read new schema.
- Roll-forward hotfix is rushed and creates second incident.
- External side effects cannot be undone.
- Recovery: disable feature, compatibility layers, compensating actions, data repair jobs, staged migrations.

### 13. Scenario

- Product / system: checkout deploy introduces wrong tax calculation after schema migration.
- Why this concept fits: reverting code may not be safe if schema/data changed.
- What would go wrong without it: blind rollback can break checkout harder than the original bug.

### 14. Code Sample

```python
def recovery_strategy(schema_backward_compatible: bool, feature_flag_available: bool) -> str:
    if feature_flag_available:
        return "disable-flag"
    if schema_backward_compatible:
        return "rollback"
    # Staff concept: incompatible state often forces roll-forward or repair.
    return "roll-forward-hotfix"
```

### 15. Mini Program / Simulation

```python
deployments = [
    {"name": "ui-copy", "schema_backward_compatible": True, "flag": False},
    {"name": "pricing-engine", "schema_backward_compatible": False, "flag": True},
    {"name": "tax-migration", "schema_backward_compatible": False, "flag": False},
]


print({deploy["name"]: recovery_strategy(deploy["schema_backward_compatible"], deploy["flag"]) for deploy in deployments})
```

### 16. Practical Question

> A production deploy has failed after a database migration. How do you decide roll-forward vs rollback?

### 17. Strong Answer

I would first assess user impact, data correctness, and whether the previous app version is compatible with current schema and data. If a feature flag can disable the behavior, I would use that first. If rollback is compatible and fast, I would rollback. If schema or external side effects are irreversible, I would roll forward with a hotfix or repair migration. The trade-off is speed vs data safety. I would design future migrations with expand-contract so rollback remains possible longer.

### 18. Revision Notes

- One-line summary: rollback goes back; roll-forward fixes forward when state cannot go back safely.
- Three keywords: compatibility, hotfix, kill switch.
- One interview trap: rolling back across incompatible schema.
- One memory trick: you can go back only if the bridge still exists.

---

## Part 1 Comparison Sheet

| Topic | Core idea | Staff-level trade-off |
|---|---|---|
| API gateways | controlled public entry point | consistency of policy vs gateway blast radius |
| Global load balancing | steer traffic across regions | lower latency/HA vs data and routing complexity |
| Request shaping | make demand safer before forwarding | stability vs pass-through simplicity |
| Throttling | enforce speed limits | fairness/protection vs client-visible rejection |
| WAF concepts | filter risky HTTP traffic | defense-in-depth vs false positives |
| Edge authentication | verify identity before backend fanout | early rejection vs edge dependency |
| Multi-layer rate limiting | protect different bottlenecks at different layers | layered defense vs debugging complexity |
| Active-active/passive | choose live-live or standby DR | availability/latency vs conflict/cost |
| RPO/RTO | measure data loss and downtime targets | business alignment vs cost of meeting targets |
| Replication lag | freshness gap between regions | low local latency vs stale remote reads |
| Geo-fencing/residency | geography as policy boundary | compliance vs failover flexibility |
| Region failover | planned regional recovery sequence | faster recovery vs automation and capacity risk |
| Split-brain avoidance | prevent two authorities | correctness vs availability during uncertainty |
| Storage tiers | match storage cost to access value | lower cost vs slower retrieval |
| Tiered policies | automate lifecycle movement | automation vs policy-risk |
| Archival pipelines | retain old data cheaply and verifiably | lower hot cost vs restore complexity |
| Retention strategies | define how long data lives | lower risk/cost vs loss of history |
| GDPR-style deletion | distributed erasure plus proof | privacy compliance vs orchestration complexity |
| Cost-aware storage | design with the bill in mind | sustainable economics vs premature optimization |
| IaC mindset | infra as reviewed desired state | repeatability vs state/module discipline |
| Immutable infrastructure | replace artifacts, do not patch live | consistency vs build pipeline overhead |
| Feature flags | deploy and release separately | control vs flag debt |
| Dark launches | shadow test with real traffic | confidence vs extra load/side-effect risk |
| Schema versioning | data contracts evolve safely | compatibility vs governance |
| Online migrations | change production gradually | availability vs temporary dual paths |
| Roll-forward/rollback | choose safest recovery direction | speed vs state compatibility |

---

## Part 1 Interview Playbook

Use this answer shape for staff/principal topics:

```text
The staff-level issue here is <blast radius / compliance / recovery / cost / evolution>.
I would choose <mechanism> because <business promise and technical constraint>.
The backend pieces affected are <gateway / service / DB / cache / queue / storage / region>.
The main trade-off is <latency / availability / cost / complexity / consistency>.
I would mitigate it with <policy / automation / validation / testing / observability>.
I would prove it using <specific metric or drill>.
I would reject <simpler or stronger alternative> because <reason>.
```

---

## Part 1 Fast Recall Rules

- Gateways centralize public policy, not business logic.
- Global load balancing must consider health, capacity, latency, and policy.
- Request shaping protects systems by priority and cost.
- Throttling is fairness and capacity protection.
- WAF is defense-in-depth, not a secure-code substitute.
- Edge authentication verifies identity early; services still authorize.
- Multi-layer rate limiting works because each layer sees different risk.
- Active-active needs conflict strategy; active-passive needs tested promotion.
- RPO is data loss; RTO is downtime.
- Async cross-region replicas can be stale.
- Data residency applies to logs, backups, analytics, and failover too.
- Failover needs fencing, promotion, traffic shift, validation, and failback.
- Split-brain prevention may sacrifice availability to preserve correctness.
- Hot, warm, and cold tiers trade retrieval speed for cost.
- Lifecycle policies need owner tags, dry run, and legal-hold awareness.
- Archive means write, verify, index, and restore.
- Retention should be per data class, not forever by default.
- GDPR-style deletion is find, delete or anonymize, and prove.
- Cost-aware storage includes indexes, egress, replication, backups, and retrieval.
- IaC makes infra reviewable and reproducible.
- Immutable infrastructure replaces live instances rather than patching them.
- Feature flags reduce release blast radius but must be cleaned up.
- Dark launches validate new paths with real traffic and no user-visible output.
- Schema versioning protects independent producers and consumers.
- Online migrations use compatibility, backfill, validation, and gradual cutover.
- Rollback is only safe when state is backward-compatible; otherwise roll forward.

---

# Part 2: Remaining Staff / Principal-Level Additions

Part 2 finishes the Staff/Principal appendix with deeper rate-limiting algorithms, security/compliance operating models, migration patterns, and the communication muscle expected in senior design reviews.

---

# Topic 7.5: Rate Limiting Algorithms Deep Dive

## 7.5.1 Token Bucket

### 1. Intuition

Token bucket is a jar that refills steadily. A request may pass only if it can take a token from the jar, so the system allows normal bursts while still enforcing a long-term average rate.

### 2. Definition

- Definition: token bucket is a rate limiting algorithm that refills tokens at a fixed rate up to a maximum capacity.
- Category: traffic control algorithm.
- Core idea: allow controlled bursts while preserving a steady average rate.

### 3. Why It Exists

Real clients send short bursts even when their average usage is reasonable. A strict per-second limiter rejects too much useful traffic, while no limiter lets bursts overload shared capacity.

### 4. Reality

- Where used: API gateways, service clients, network shapers, cloud quotas, distributed limiters.
- Systems/products: Envoy local rate limits, Redis-backed API limits, cloud API quotas, payment provider APIs.
- Teams: API platform, SRE, edge, backend service owners.

### 5. How It Works

1. Bucket has capacity `C`.
2. Tokens refill at rate `R` per second.
3. Each request costs one or more tokens.
4. If enough tokens exist, request is allowed and tokens are removed.
5. If not enough tokens exist, request is rejected, delayed, or given retry guidance.

### 6. What Problem It Solves

- Primary problem solved: enforcing average rate while allowing safe bursts.
- Secondary benefits: better user experience, quota fairness, dependency protection.
- Systems impact: protects capacity without being overly rigid.

### 7. When to Rely on It

- Use for public APIs, tenant quotas, write APIs, and endpoints with natural burst behavior.
- Strong fit when clients should be allowed to use saved capacity briefly.
- Interview keywords: burst capacity, refill rate, average rate, 429, Retry-After.

### 8. When Not to Use It

- Avoid when traffic must be smoothed into a constant output rate.
- Avoid if exact global limits across many regions are mandatory without coordination.
- Use leaky bucket for smoother egress or distributed rate limiting for multi-node enforcement.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Allows controlled bursts | Needs token state per key |
| Simple and widely understood | Distributed accuracy is harder |
| Good client experience | Bad burst size can still overload downstream |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: burst tolerance plus average-rate control.
- Give up: perfectly smooth traffic output.
- Latency/cost impact: cheap local decision, possible state-store cost when distributed.

#### Common Mistakes

- Mistake: bucket capacity equals steady rate. Better approach: choose burst separately from refill rate.
- Mistake: every request costs one token. Better approach: charge expensive routes more tokens.
- Mistake: no retry guidance. Better approach: return Retry-After based on refill time.

### 11. Key Numbers

- Refill rate: allowed average requests per second or minute.
- Capacity: maximum burst tokens.
- Metrics: allowed count, rejected count, token starvation, retry-after, per-key usage.

### 12. Failure Modes

- Too-large bucket allows damaging bursts.
- Too-small bucket rejects normal client behavior.
- Shared state outage causes fail-open or fail-closed risk.
- Recovery: tune burst independently, fallback to local limits, add per-route costs, monitor false positives.

### 13. Scenario

- Product / system: partner booking API with paid tiers.
- Why this concept fits: partners may burst during sale events but must respect average quota.
- What would go wrong without it: strict limits reject useful bursts or no limits overload booking services.

### 14. Code Sample

```python
def refill_tokens(current: float, capacity: int, elapsed_seconds: float, refill_per_second: float) -> float:
    # Staff concept: token bucket separates burst capacity from steady refill rate.
    return min(capacity, current + elapsed_seconds * refill_per_second)
```

### 15. Mini Program / Simulation

```python
import time


class TokenBucketLimiter:
    def __init__(self, capacity: int, refill_per_second: float) -> None:
        self.capacity = capacity
        self.refill_per_second = refill_per_second
        self.tokens = float(capacity)
        self.last_seen = time.monotonic()

    def allow(self, cost: int = 1) -> bool:
        now = time.monotonic()
        self.tokens = refill_tokens(self.tokens, self.capacity, now - self.last_seen, self.refill_per_second)
        self.last_seen = now
        if self.tokens >= cost:
            self.tokens -= cost
            return True
        return False


limiter = TokenBucketLimiter(capacity=3, refill_per_second=1)
print([limiter.allow() for _ in range(5)])
```

### 16. Practical Question

> You are designing partner API quotas where short bursts are acceptable. How would token bucket help, and what trade-offs would you consider?

### 17. Strong Answer

I would use token bucket because it enforces an average rate while allowing controlled bursts. The refill rate represents the long-term quota, and bucket capacity represents burst tolerance. The trade-off is choosing burst size carefully and storing per-key state, especially in distributed systems. I would charge expensive routes more tokens, return Retry-After, monitor false positives, and add local fallback limits if shared state fails.

### 18. Revision Notes

- One-line summary: token bucket allows bursts while enforcing average rate.
- Three keywords: refill, capacity, burst.
- One interview trap: confusing burst size with steady rate.
- One memory trick: saved tokens let a client burst briefly.

---

## 7.5.2 Leaky Bucket

### 1. Intuition

Leaky bucket is a queue with a fixed drain rate. Requests may arrive unevenly, but the system releases them downstream at a smoother pace.

### 2. Definition

- Definition: leaky bucket limits traffic by accepting requests into a bounded queue and processing them at a fixed rate.
- Category: rate limiting and traffic smoothing algorithm.
- Core idea: smooth bursty input into steady output.

### 3. Why It Exists

Some downstream systems cannot tolerate bursts even if average rate is acceptable. Leaky bucket protects them by pacing output and dropping or rejecting when the queue is full.

### 4. Reality

- Where used: network traffic shaping, job dispatchers, webhook delivery, notification sending, dependency clients.
- Systems/products: API dispatch workers, queue consumers, outbound email/SMS senders, network shapers.
- Teams: SRE, platform, messaging, network, integrations.

### 5. How It Works

1. Incoming requests enter a bounded bucket or queue.
2. Queue accepts until capacity is reached.
3. Worker drains requests at fixed rate.
4. Overflow is rejected or dropped according to policy.
5. Queue delay and drop rate are monitored.

### 6. What Problem It Solves

- Primary problem solved: smoothing bursts before they hit downstream capacity.
- Secondary benefits: bounded queueing, predictable egress, dependency protection.
- Systems impact: improves downstream stability but adds queueing latency.

### 7. When to Rely on It

- Use when downstream requires steady processing instead of bursts.
- Strong fit for outbound integrations, notifications, and expensive dependency clients.
- Interview keywords: smoothing, drain rate, bounded queue, fixed output rate.

### 8. When Not to Use It

- Avoid for interactive user requests when queue delay violates latency SLO.
- Avoid if bursts should be allowed and downstream can absorb them.
- Use token bucket for burst-tolerant quotas or priority queues for mixed importance work.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Smooths traffic output | Adds waiting latency |
| Protects burst-sensitive dependencies | Queue overflow needs policy |
| Easy to reason about drain rate | Can hide overload until queue fills |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: stable downstream load.
- Give up: immediate request handling.
- Latency/cost impact: lower dependency spikes, higher queue wait for bursts.

#### Common Mistakes

- Mistake: unbounded queue. Better approach: fixed capacity with explicit reject/drop behavior.
- Mistake: using it on all user-facing endpoints. Better approach: use only when latency budget allows.
- Mistake: no queue age metric. Better approach: monitor oldest item age and drain lag.

### 11. Key Numbers

- Drain rate: requests or jobs per second.
- Queue capacity: maximum waiting items.
- Metrics: queue depth, oldest age, drain rate, overflow count, downstream error rate.

### 12. Failure Modes

- Queue fills and all new requests are rejected.
- Queue delay exceeds business usefulness.
- Drainer fails and backlog grows silently.
- Recovery: bounded queue alerts, dead-letter policy, autoscale drainers, shed low-priority work.

### 13. Scenario

- Product / system: webhook delivery to partner systems.
- Why this concept fits: partners may allow only steady delivery rates.
- What would go wrong without it: bursts cause partner throttling or webhook failures.

### 14. Code Sample

```python
def queue_accepts(current_depth: int, capacity: int) -> bool:
    # Staff concept: leaky bucket is safe only when the waiting bucket is bounded.
    return current_depth < capacity
```

### 15. Mini Program / Simulation

```python
from collections import deque


class LeakyBucketLimiter:
    def __init__(self, capacity: int) -> None:
        self.queue: deque[str] = deque(maxlen=capacity)

    def offer(self, request_id: str) -> bool:
        if len(self.queue) == self.queue.maxlen:
            return False
        self.queue.append(request_id)
        return True

    def drain_one(self) -> str | None:
        return self.queue.popleft() if self.queue else None


bucket = LeakyBucketLimiter(capacity=2)
print(bucket.offer("r1"), bucket.offer("r2"), bucket.offer("r3"))
print(bucket.drain_one())
```

### 16. Practical Question

> You are sending webhooks to partner systems that reject bursts. How would leaky bucket help, and what trade-offs would you consider?

### 17. Strong Answer

I would use leaky bucket to queue webhook jobs and drain them at a partner-specific fixed rate. It fits because the partner needs smooth traffic, not just average quota control. The trade-off is queueing latency and overflow behavior. I would bound the queue, track oldest job age, provide retry and DLQ handling, and use token bucket instead if controlled bursts are acceptable.

### 18. Revision Notes

- One-line summary: leaky bucket smooths bursts into a fixed output rate.
- Three keywords: queue, drain, smooth.
- One interview trap: using an unbounded queue.
- One memory trick: water enters fast but leaks out steadily.

---

## 7.5.3 Fixed Window

### 1. Intuition

Fixed window divides time into boxes and counts requests in each box. If the box is full, new requests wait for the next box.

### 2. Definition

- Definition: fixed window rate limiting counts requests in discrete time windows and rejects once the limit is reached.
- Category: counter-based rate limiting algorithm.
- Core idea: simple per-window quota enforcement.

### 3. Why It Exists

Many systems need a cheap and easy limiter. Fixed window is simple to implement with counters and expiration, especially when approximate boundary behavior is acceptable.

### 4. Reality

- Where used: simple API quotas, admin tools, internal APIs, low-risk endpoints.
- Systems/products: Redis counters with TTL, in-memory counters, gateway plugins.
- Teams: API platform, backend, internal tooling.

### 5. How It Works

1. Pick a window length such as one minute.
2. Build a key from client identity and window start.
3. Increment request counter for that key.
4. Allow if count is within limit.
5. Expire the key after the window ends.

### 6. What Problem It Solves

- Primary problem solved: low-cost request quota counting.
- Secondary benefits: simple implementation, easy debugging, low memory.
- Systems impact: protects simple endpoints but has boundary burst weakness.

### 7. When to Rely on It

- Use for simple quotas where edge-window bursts are tolerable.
- Strong fit for internal APIs or low-risk public limits.
- Interview keywords: counter, TTL, window boundary, simple limiter.

### 8. When Not to Use It

- Avoid for high-risk public APIs where users can exploit window edges.
- Avoid where smooth enforcement is required.
- Use sliding window or token bucket for better burst control.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Very simple | Boundary burst can double effective rate briefly |
| Low memory and CPU | Less fair near window edges |
| Easy to implement with Redis TTL | Coarse time resolution |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: simplicity and speed.
- Give up: precise smoothing and fair boundary behavior.
- Latency/cost impact: very cheap decision, possible backend burst at boundaries.

#### Common Mistakes

- Mistake: ignoring boundary burst. Better approach: call out worst-case double burst.
- Mistake: one global counter for all users. Better approach: key by identity and route.
- Mistake: missing TTL. Better approach: expire counters automatically.

### 11. Key Numbers

- Worst-case burst: nearly 2x limit around boundary.
- Window length: seconds, minutes, or hours depending on quota.
- Metrics: counter increments, rejections, keys created, boundary burst incidents.

### 12. Failure Modes

- Attackers time requests at window edges.
- Hot key overloads counter store.
- Clock skew creates inconsistent windows across nodes.
- Recovery: use server-side time, shard keys, move to sliding window or token bucket.

### 13. Scenario

- Product / system: internal admin API with low QPS.
- Why this concept fits: simple quota is enough and boundary bursts are low risk.
- What would go wrong without it: accidental loops can spam internal dependencies.

### 14. Code Sample

```python
def fixed_window_key(client_id: str, now_seconds: int, window_seconds: int) -> str:
    # Staff concept: fixed window state is scoped by client and discrete time bucket.
    return f"{client_id}:{now_seconds // window_seconds}"
```

### 15. Mini Program / Simulation

```python
class FixedWindowLimiter:
    def __init__(self, limit: int, window_seconds: int) -> None:
        self.limit = limit
        self.window_seconds = window_seconds
        self.counts: dict[str, int] = {}

    def allow(self, client_id: str, now_seconds: int) -> bool:
        key = fixed_window_key(client_id, now_seconds, self.window_seconds)
        count = self.counts.get(key, 0) + 1
        self.counts[key] = count
        return count <= self.limit


limiter = FixedWindowLimiter(limit=2, window_seconds=60)
print([limiter.allow("u1", 59), limiter.allow("u1", 59), limiter.allow("u1", 60)])
```

### 16. Practical Question

> You need a simple low-cost rate limiter for an internal API. How would fixed window work, and what trade-offs would you mention?

### 17. Strong Answer

I would use fixed window if simplicity matters and boundary bursts are acceptable. Each client and route gets a counter for the current time window, usually with TTL. The trade-off is that clients can burst near the boundary and briefly exceed the intended rate. For high-risk public APIs, I would prefer token bucket or sliding window. I would monitor hot keys and rejection rates.

### 18. Revision Notes

- One-line summary: fixed window counts requests inside discrete time buckets.
- Three keywords: counter, TTL, boundary.
- One interview trap: forgetting the 2x boundary burst.
- One memory trick: a quota box resets when the clock crosses the line.

---

## 7.5.4 Sliding Window Log

### 1. Intuition

Sliding window log keeps the exact timestamps of recent requests. To decide, it looks back over the last N seconds, not just the current clock box.

### 2. Definition

- Definition: sliding window log stores request timestamps and counts only those inside the rolling time window.
- Category: precise rate limiting algorithm.
- Core idea: enforce exact rolling-window limits using per-request history.

### 3. Why It Exists

Fixed windows allow boundary bursts. Sliding window log fixes that by measuring the true last-window usage, but it pays more memory and cleanup cost.

### 4. Reality

- Where used: sensitive APIs, login attempts, fraud-sensitive operations, account-level protection.
- Systems/products: Redis sorted sets, in-memory timestamp queues, gateway plugins.
- Teams: security, API platform, fraud, SRE.

### 5. How It Works

1. Store timestamp for each request per key.
2. Remove timestamps older than window.
3. Count remaining timestamps.
4. Allow if count is below limit.
5. Add current timestamp if allowed or record rejection.

### 6. What Problem It Solves

- Primary problem solved: precise rolling-window rate enforcement.
- Secondary benefits: fairer limits, reduced boundary exploitation, useful audit trail.
- Systems impact: improves correctness but uses more memory and write operations.

### 7. When to Rely on It

- Use for high-risk flows where exactness matters more than memory cost.
- Strong fit for login attempts, password resets, payment attempts, and abuse controls.
- Interview keywords: rolling window, timestamp log, Redis sorted set, exactness.

### 8. When Not to Use It

- Avoid for very high-cardinality high-QPS traffic when memory cost is too high.
- Avoid if approximate enforcement is enough.
- Use sliding window counter or token bucket for lower-memory alternatives.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Precise rolling-window limit | Stores timestamp per request |
| Avoids fixed-window boundary burst | Cleanup cost per decision |
| Useful for security audit | Can be expensive at high QPS |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: accuracy and fairness.
- Give up: low memory and low write amplification.
- Latency/cost impact: more state operations, better abuse control.

#### Common Mistakes

- Mistake: never pruning old timestamps. Better approach: prune on write or background cleanup.
- Mistake: storing logs forever. Better approach: TTL equal to window plus safety margin.
- Mistake: using it for every endpoint. Better approach: reserve for high-risk keys.

### 11. Key Numbers

- Memory: O(number of requests in window per key).
- Operation cost: prune plus count plus insert.
- Metrics: timestamp count, prune count, limiter latency, memory per key, rejection rate.

### 12. Failure Modes

- Hot users create large timestamp sets.
- Cleanup lag increases memory.
- Clock skew across nodes breaks fairness.
- Recovery: server-side time, TTLs, cap history length, approximate fallback under overload.

### 13. Scenario

- Product / system: login attempt limiting per account.
- Why this concept fits: exact rolling limits reduce credential-stuffing windows.
- What would go wrong without it: attackers exploit fixed-window boundaries to double attempts.

### 14. Code Sample

```python
def prune_window(timestamps: list[int], now: int, window_seconds: int) -> list[int]:
    # Staff concept: sliding log counts only real requests inside the rolling window.
    return [timestamp for timestamp in timestamps if timestamp > now - window_seconds]
```

### 15. Mini Program / Simulation

```python
class SlidingWindowLogLimiter:
    def __init__(self, limit: int, window_seconds: int) -> None:
        self.limit = limit
        self.window_seconds = window_seconds
        self.logs: dict[str, list[int]] = {}

    def allow(self, key: str, now: int) -> bool:
        recent = prune_window(self.logs.get(key, []), now, self.window_seconds)
        if len(recent) >= self.limit:
            self.logs[key] = recent
            return False
        recent.append(now)
        self.logs[key] = recent
        return True


limiter = SlidingWindowLogLimiter(limit=2, window_seconds=60)
print([limiter.allow("acct", t) for t in [1, 2, 59, 62]])
```

### 16. Practical Question

> You are protecting login attempts from abuse. How would sliding window log help, and what trade-offs would you consider?

### 17. Strong Answer

I would use sliding window log when exact rolling-window enforcement matters, such as login attempts. It stores recent timestamps per account or IP and rejects if the count within the last window exceeds the limit. The trade-off is memory and cleanup overhead. For very high-QPS APIs I would use token bucket or sliding window counter. I would monitor limiter latency and per-key memory.

### 18. Revision Notes

- One-line summary: sliding window log is exact but memory-heavy.
- Three keywords: timestamp, rolling, prune.
- One interview trap: using it everywhere at high QPS.
- One memory trick: keep the receipt timestamps for the last minute.

---

## 7.5.5 Sliding Window Counter

### 1. Intuition

Sliding window counter approximates a rolling window by blending the previous fixed window with the current one. It is smoother than fixed window and cheaper than storing every timestamp.

### 2. Definition

- Definition: sliding window counter estimates rolling usage using weighted counts from current and previous windows.
- Category: approximate rate limiting algorithm.
- Core idea: reduce fixed-window boundary burst with low memory.

### 3. Why It Exists

Sliding window log is accurate but memory-heavy. Fixed window is cheap but unfair near boundaries. Sliding window counter sits between them.

### 4. Reality

- Where used: API gateways, high-QPS public APIs, tenant limits, edge rate limiting.
- Systems/products: Redis counters, gateway plugins, custom in-memory limiters.
- Teams: API platform, SRE, edge, backend.

### 5. How It Works

1. Track count for current fixed window.
2. Track count for previous fixed window.
3. Compute how far into the current window we are.
4. Weight previous count by remaining overlap.
5. Reject if estimated rolling count exceeds limit.

### 6. What Problem It Solves

- Primary problem solved: reducing boundary burst without per-request timestamp storage.
- Secondary benefits: lower memory, smoother fairness, good high-QPS behavior.
- Systems impact: better than fixed window for public APIs, cheaper than exact logs.

### 7. When to Rely on It

- Use when fixed window is too bursty and sliding log is too expensive.
- Strong fit for high-scale API quotas with acceptable approximation.
- Interview keywords: weighted previous window, approximation, lower memory, rolling estimate.

### 8. When Not to Use It

- Avoid when exact audit-grade limits are required.
- Avoid if approximation errors are unacceptable for security decisions.
- Use sliding window log for exactness or token bucket for burst-based quota.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Lower memory than timestamp log | Approximate, not exact |
| Smoother than fixed window | More complex than fixed counter |
| Good high-QPS fit | Boundary math must be correct |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: memory efficiency and smoother enforcement.
- Give up: exact rolling-window precision.
- Latency/cost impact: cheap counter operations with better fairness than fixed window.

#### Common Mistakes

- Mistake: forgetting previous-window weighting. Better approach: weight by remaining overlap.
- Mistake: using client clocks. Better approach: use server-side time.
- Mistake: claiming exactness. Better approach: call it an approximation.

### 11. Key Numbers

- Memory: O(2 counters per key per window).
- Metrics: estimated usage, current count, previous count, rejection rate, approximation errors.
- Best fit: high-cardinality keys where timestamp logs would be expensive.

### 12. Failure Modes

- Bad time math over-allows or over-blocks.
- Counter reset loses previous window.
- Hot key overloads counter store.
- Recovery: server-side clock, atomic increments, key sharding, fallback limits.

### 13. Scenario

- Product / system: public search API with millions of users.
- Why this concept fits: high QPS needs low memory but fixed-window bursts are too rough.
- What would go wrong without it: fixed window permits edge bursts or sliding log becomes too expensive.

### 14. Code Sample

```python
def estimated_sliding_count(previous_count: int, current_count: int, elapsed_in_window: int, window_seconds: int) -> float:
    # Staff concept: previous window contributes only for the overlapping fraction.
    previous_weight = (window_seconds - elapsed_in_window) / window_seconds
    return previous_count * previous_weight + current_count
```

### 15. Mini Program / Simulation

```python
def sliding_counter_allows(previous_count: int, current_count: int, elapsed: int, window: int, limit: int) -> bool:
    return estimated_sliding_count(previous_count, current_count, elapsed, window) < limit


print(estimated_sliding_count(previous_count=80, current_count=10, elapsed_in_window=15, window_seconds=60))
print(sliding_counter_allows(80, 10, 15, 60, 100))
```

### 16. Practical Question

> You need a high-QPS public API limiter that is fairer than fixed window but cheaper than timestamp logs. How would sliding window counter help?

### 17. Strong Answer

I would use sliding window counter to approximate rolling usage from current and previous window counters. It reduces fixed-window boundary bursts while avoiding per-request timestamp storage. The trade-off is approximation, so I would not use it for audit-grade enforcement. I would use server-side time, atomic counters, and monitor hot keys and rejection accuracy.

### 18. Revision Notes

- One-line summary: sliding window counter approximates rolling usage with two weighted counters.
- Three keywords: current, previous, weighted.
- One interview trap: calling it exact.
- One memory trick: borrow part of the last box based on overlap.

---

## 7.5.6 Distributed Rate Limiting

### 1. Intuition

Distributed rate limiting is enforcing one quota across many gateways, regions, or service instances. The hard part is that every node sees only part of the traffic.

### 2. Definition

- Definition: distributed rate limiting coordinates limiter decisions across multiple nodes using shared state, partitioned ownership, or approximate local budgets.
- Category: distributed traffic control.
- Core idea: enforce shared limits while balancing accuracy, latency, availability, and cost.

### 3. Why It Exists

A single-node limiter breaks when traffic is served by many instances. Without coordination, each instance can allow the full quota and the system over-admits traffic.

### 4. Reality

- Where used: global API gateways, multi-region platforms, cloud services, partner APIs, login protection.
- Systems/products: Redis/Valkey counters, Envoy rate limit service, sharded counters, CRDT/approximate counters.
- Teams: API platform, SRE, edge, security, infrastructure.

### 5. How It Works

1. Request arrives at one of many nodes.
2. Node computes limiter key and cost.
3. Decision is made by shared counter, owning shard, local budget, or hybrid service.
4. If allowed, usage state is updated.
5. System reconciles drift or falls back during dependency failures.

### 6. What Problem It Solves

- Primary problem solved: enforcing quotas across horizontally scaled or multi-region systems.
- Secondary benefits: tenant fairness, global abuse protection, dependency safety.
- Systems impact: adds coordination to the hot path or accepts approximation.

### 7. When to Rely on It

- Use when traffic for one key can hit multiple nodes or regions.
- Strong fit for public APIs, global auth, and multi-tenant quotas.
- Interview keywords: shared counter, sharding, local budget, consistency, hot key.

### 8. When Not to Use It

- Avoid strict global counters for ultra-low-latency paths if approximation is acceptable.
- Avoid one central limiter if it becomes a global dependency and bottleneck.
- Use local rate limits for best-effort protection or regional quotas when global exactness is unnecessary.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Enforces shared quotas | Adds latency or coordination cost |
| Protects global resources | Shared limiter can become bottleneck |
| Supports multi-node fairness | Exactness vs availability is a hard trade-off |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: consistent quota across nodes.
- Give up: purely local low-latency decisions or exact availability during partitions.
- Latency/cost impact: shared state adds network calls; local budgets reduce latency but over-admit risk.

#### Common Mistakes

- Mistake: each node applies full quota independently. Better approach: split quota or use shared coordination.
- Mistake: central limiter without fallback. Better approach: define fail-open/fail-closed by route risk.
- Mistake: ignore hot keys. Better approach: shard, cache, or pre-allocate local budgets.

### 11. Key Numbers

- Metrics: limiter decision latency, shared-store QPS, over-limit drift, hot-key count, fallback decisions.
- Common patterns: central counter, sharded owner, local token leasing, approximate regional budget.
- Failure policy: fail-open for low-risk availability paths, fail-closed for high-risk abuse paths.

### 12. Failure Modes

- Shared limiter outage blocks or allows too much traffic.
- Network partition creates inconsistent counters.
- Hot tenant key overloads one shard.
- Recovery: local fallback budgets, shard hot keys, circuit breaker around limiter, reconciliation and audit.

### 13. Scenario

- Product / system: global partner API served by gateways in US, EU, and APAC.
- Why this concept fits: one partner quota must apply across all gateways.
- What would go wrong without it: each region permits full quota and overloads backend services.

### 14. Code Sample

```python
def regional_budget(global_limit: int, active_regions: int) -> int:
    # Staff concept: local budget leasing trades perfect accuracy for lower-latency decisions.
    return max(1, global_limit // active_regions)
```

### 15. Mini Program / Simulation

```python
def distributed_allow(region_usage: dict[str, int], region_limit: int) -> dict[str, bool]:
    return {region: usage < region_limit for region, usage in region_usage.items()}


limit = regional_budget(global_limit=900, active_regions=3)
print(limit)
print(distributed_allow({"us": 299, "eu": 310, "apac": 100}, limit))
```

### 16. Practical Question

> You are enforcing a single partner quota across many gateway instances and regions. How would distributed rate limiting work, and what trade-offs would you consider?

### 17. Strong Answer

I would choose between a central/shared limiter, sharded ownership, or local budget leasing based on required accuracy and latency. Strict shared counters are more accurate but add network latency and failure dependency. Local budgets are faster and more available but can over-admit during imbalance. I would define fail-open/fail-closed by route risk, monitor limiter latency and drift, and handle hot keys through sharding or pre-allocation.

### 18. Revision Notes

- One-line summary: distributed rate limiting enforces one quota across many nodes.
- Three keywords: shared state, local budget, drift.
- One interview trap: giving every node the full global quota.
- One memory trick: one quota, many doors, shared accounting.

---

# Topic 7.6: Advanced Security and Compliance

## 7.6.1 PII Segregation

### 1. Intuition

PII segregation means personal data does not casually live everywhere. It is placed in stricter rooms, with fewer doors, clearer ownership, and stronger audit trails.

### 2. Definition

- Definition: PII segregation separates personally identifiable information from non-sensitive operational data using storage, access, encryption, and service boundaries.
- Category: privacy and security architecture.
- Core idea: reduce the blast radius of personal data exposure.

### 3. Why It Exists

When PII is copied into every table, log, cache, event, and analytics store, every system becomes a privacy risk. Segregation limits who can access PII and where it can leak.

### 4. Reality

- Where used: user profiles, payments-adjacent systems, healthcare, enterprise SaaS, analytics platforms.
- Systems/products: user profile vaults, tokenization services, privacy proxies, data catalogs, restricted warehouses.
- Teams: security, privacy engineering, data governance, backend platform, compliance.

### 5. How It Works

1. Classify fields as PII, sensitive PII, or non-PII.
2. Store PII in dedicated tables, services, or vaults.
3. Use stable surrogate IDs in most business systems.
4. Restrict access through policy, encryption, and audit.
5. Prevent PII from leaking into logs, analytics, events, and support exports.

### 6. What Problem It Solves

- Primary problem solved: limiting privacy blast radius and access scope.
- Secondary benefits: simpler compliance, safer analytics, clearer data ownership.
- Systems impact: changes schemas, event contracts, logging, access control, and deletion workflows.

### 7. When to Rely on It

- Use when systems store names, emails, phone numbers, addresses, IDs, or other personal fields.
- Strong fit for regulated data or large organizations with many data consumers.
- Interview keywords: tokenization, data minimization, privacy vault, PII boundary, data catalog.

### 8. When Not to Use It

- Avoid excessive separation for low-risk non-personal metadata.
- Avoid designs that make every user request synchronously call a fragile PII vault if latency cannot tolerate it.
- Use masking, hashing, or anonymization when full PII is not needed.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces breach blast radius | Adds service/schema complexity |
| Makes access easier to audit | Joins and support workflows need careful design |
| Supports privacy deletion | Can add latency for PII lookups |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: privacy control and blast-radius reduction.
- Give up: simple everywhere-available user fields.
- Latency/cost impact: extra lookup or service call for true PII, lower compliance risk.

#### Common Mistakes

- Mistake: segregating database but leaking PII in logs. Better approach: cover logs, events, caches, and exports.
- Mistake: using email as primary key. Better approach: use surrogate user IDs.
- Mistake: giving analysts raw PII by default. Better approach: masked or tokenized datasets.

### 11. Key Numbers

- Metrics: PII field inventory coverage, raw PII access count, masking violations, log leak detections.
- Access review: periodic review by role, purpose, and data class.
- SLO: PII vault latency must fit user-facing paths that require it.

### 12. Failure Modes

- PII appears in application logs or events.
- Support export includes fields beyond purpose.
- Analytics copy bypasses deletion workflow.
- Recovery: data scan, purge leaked copies, rotate access, update schema/log filters, audit consumers.

### 13. Scenario

- Product / system: travel marketplace user profile and booking services.
- Why this concept fits: booking needs user ID often, but email and phone are needed only in specific flows.
- What would go wrong without it: every downstream consumer gains unnecessary personal data exposure.

### 14. Code Sample

```python
def public_booking_event(booking: dict[str, str]) -> dict[str, str]:
    # Staff concept: events use surrogate identifiers instead of raw PII.
    return {"booking_id": booking["booking_id"], "user_id": booking["user_id"], "status": booking["status"]}
```

### 15. Mini Program / Simulation

```python
PII_FIELDS = {"email", "phone", "address", "passport_number"}


def scrub_for_logs(payload: dict[str, str]) -> dict[str, str]:
    return {key: ("<redacted>" if key in PII_FIELDS else value) for key, value in payload.items()}


print(scrub_for_logs({"user_id": "u1", "email": "a@example.com", "status": "active"}))
```

### 16. Practical Question

> You are designing user profile storage for a large booking platform. How would PII segregation help, and what trade-offs would you consider?

### 17. Strong Answer

I would segregate PII into a dedicated profile or privacy service and let most systems use surrogate IDs. It fits because most workflows do not need raw email, phone, or address. The trade-off is lookup complexity and possible latency when PII is truly needed. I would enforce masking in logs/events, audit access, use purpose-based permissions, and include segregated stores in deletion workflows.

### 18. Revision Notes

- One-line summary: PII segregation keeps personal data out of places that do not need it.
- Three keywords: surrogate ID, masking, blast radius.
- One interview trap: forgetting logs and analytics.
- One memory trick: keep PII in the vault, not in every drawer.

---

## 7.6.2 Encryption Key Rotation

### 1. Intuition

Key rotation is changing the locks before old keys become too risky. Good systems can rotate keys without losing data or taking downtime.

### 2. Definition

- Definition: encryption key rotation replaces or rewraps cryptographic keys on a planned or emergency schedule.
- Category: cryptographic operations and compliance.
- Core idea: limit the useful lifetime of a compromised or aging key.

### 3. Why It Exists

Keys can leak, age, be overused, or violate policy. Without rotation, a single old key may protect years of data and create a large breach window.

### 4. Reality

- Where used: databases, object storage, secrets, tokens, message encryption, backups.
- Systems/products: KMS, HSM, envelope encryption, certificate managers, secret stores.
- Teams: security platform, infrastructure, compliance, backend service owners.

### 5. How It Works

1. Data is encrypted with a data encryption key.
2. Data key is wrapped by a key encryption key in KMS/HSM.
3. New key version is created.
4. New writes use the latest key version.
5. Old data is rewrapped or reencrypted gradually, while old keys remain available for reads until migration completes.

### 6. What Problem It Solves

- Primary problem solved: reducing exposure from long-lived encryption keys.
- Secondary benefits: compliance evidence, incident response readiness, key-scoped blast radius.
- Systems impact: affects read/write paths, backups, audit logs, and migration jobs.

### 7. When to Rely on It

- Use for any sensitive encrypted data, secrets, signing keys, or compliance-controlled systems.
- Strong fit for customer-managed keys and regulated environments.
- Interview keywords: KMS, envelope encryption, key version, rewrap, compromise response.

### 8. When Not to Use It

- Avoid ad hoc application-managed crypto if a managed KMS can meet the need.
- Avoid deleting old key material before all old ciphertext is rewrapped.
- Use certificate rotation processes for TLS certs, which are related but operationally different.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Limits key compromise window | Rotation jobs add operational risk |
| Supports compliance | Old data may require rewrap/backfill |
| Improves incident readiness | Key deletion mistakes can make data unreadable |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: smaller cryptographic blast radius.
- Give up: simple one-key-forever operation.
- Latency/cost impact: KMS calls and rewrap jobs cost money and may affect latency if not cached carefully.

#### Common Mistakes

- Mistake: rotate new writes only and forget old data. Better approach: define rewrap/reencrypt strategy.
- Mistake: delete old keys early. Better approach: disable, observe, then destroy after proof.
- Mistake: no key version in metadata. Better approach: store key version with ciphertext.

### 11. Key Numbers

- Rotation cadence: often 30, 90, 180, or 365 days depending on policy and data class.
- Metrics: key age, ciphertext by key version, rewrap progress, decrypt failures, KMS latency.
- Emergency target: compromise rotation should be rehearsed and much faster than scheduled rotation.

### 12. Failure Modes

- Old key disabled before old data is migrated.
- KMS outage blocks decrypt path.
- Rotation job overloads database or object store.
- Recovery: restore key version, pause job, use cached data keys where safe, retry rewrap chunks.

### 13. Scenario

- Product / system: encrypted passport document storage.
- Why this concept fits: sensitive documents need limited key lifetime and auditable key use.
- What would go wrong without it: one compromised key exposes all historical documents.

### 14. Code Sample

```python
def ciphertext_metadata(record_id: str, key_version: str) -> dict[str, str]:
    # Staff concept: ciphertext must record which key version can decrypt it.
    return {"record_id": record_id, "key_version": key_version}
```

### 15. Mini Program / Simulation

```python
records = [
    {"id": "doc-1", "key_version": "v1"},
    {"id": "doc-2", "key_version": "v2"},
]


def needs_rewrap(record: dict[str, str], latest_key_version: str) -> bool:
    return record["key_version"] != latest_key_version


print([record["id"] for record in records if needs_rewrap(record, "v2")])
```

### 16. Practical Question

> You are storing sensitive customer documents. How would you design encryption key rotation?

### 17. Strong Answer

I would use envelope encryption with a managed KMS or HSM. New writes would use the latest key version, and old ciphertext would be rewrapped gradually with metadata tracking key version. The trade-off is operational complexity and KMS dependency. I would never delete old keys until rewrap is proven, monitor decrypt failures, keep audit logs, and rehearse emergency rotation.

### 18. Revision Notes

- One-line summary: key rotation changes cryptographic keys safely over time.
- Three keywords: KMS, version, rewrap.
- One interview trap: deleting old keys too soon.
- One memory trick: change the lock, but keep old doors open until moved.

---

## 7.6.3 Audit Logging

### 1. Intuition

Audit logging is the tamper-resistant record of who did what, when, where, and why. It is less about debugging and more about accountability.

### 2. Definition

- Definition: audit logging records security, access, administrative, and compliance-relevant actions in a durable and reviewable trail.
- Category: security observability and governance.
- Core idea: create reliable evidence for sensitive actions.

### 3. Why It Exists

Systems need to prove access, investigate incidents, detect abuse, and satisfy compliance. Normal application logs are often too noisy, mutable, or incomplete.

### 4. Reality

- Where used: admin actions, data exports, permission changes, support access, payment operations, key use.
- Systems/products: SIEM, immutable log storage, CloudTrail-like systems, security data lakes.
- Teams: security, compliance, SRE, platform, internal tools.

### 5. How It Works

1. Identify auditable events and required fields.
2. Emit structured audit events from trusted services.
3. Include actor, action, resource, timestamp, result, reason, and request context.
4. Store logs durably with restricted write/read access.
5. Alert, review, retain, and export evidence as required.

### 6. What Problem It Solves

- Primary problem solved: accountability for sensitive system actions.
- Secondary benefits: incident investigation, compliance evidence, abuse detection.
- Systems impact: affects admin tools, access workflows, storage retention, and security monitoring.

### 7. When to Rely on It

- Use for privileged actions, PII access, configuration changes, data exports, auth decisions, and key operations.
- Strong fit for regulated systems and enterprise SaaS.
- Interview keywords: immutable logs, actor, resource, action, SIEM, retention.

### 8. When Not to Use It

- Do not treat general debug logs as audit logs.
- Avoid logging sensitive payloads into audit records unless strictly required and protected.
- Use metrics/traces for performance debugging; audit logs are evidence trails.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Supports accountability and compliance | Adds storage and privacy burden |
| Helps incident investigation | Bad logging can expose sensitive data |
| Enables abuse detection | Must be tamper-resistant and complete |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: evidence and accountability.
- Give up: some storage cost and event design simplicity.
- Latency/cost impact: audit events should be reliable but not make core paths fragile.

#### Common Mistakes

- Mistake: audit log is mutable by admins being audited. Better approach: write-once or tightly controlled storage.
- Mistake: missing failed attempts. Better approach: log allowed and denied sensitive actions.
- Mistake: no correlation ID. Better approach: link audit event to request and incident context.

### 11. Key Numbers

- Retention: often months to years depending on regulation and data class.
- Metrics: audit event drop rate, ingestion lag, query latency, alert count, storage growth.
- Required fields: actor, action, resource, timestamp, result, source, reason.

### 12. Failure Modes

- Audit pipeline outage drops events.
- Admin can delete or modify audit evidence.
- Logs include too much PII and create new risk.
- Recovery: durable buffer, append-only storage, access separation, redaction, completeness checks.

### 13. Scenario

- Product / system: support staff viewing customer booking details.
- Why this concept fits: sensitive access must be attributable and reviewable.
- What would go wrong without it: insider misuse or mistaken access cannot be investigated.

### 14. Code Sample

```python
from datetime import datetime, timezone


def audit_event(actor: str, action: str, resource: str, result: str) -> dict[str, str]:
    # Staff concept: audit logs are structured evidence, not free-form debug strings.
    return {"actor": actor, "action": action, "resource": resource, "result": result, "time": datetime.now(timezone.utc).isoformat()}
```

### 15. Mini Program / Simulation

```python
def sensitive_access(actor: str, resource: str, allowed: bool) -> dict[str, str]:
    result = "allowed" if allowed else "denied"
    return audit_event(actor, "view_customer_profile", resource, result)


print(sensitive_access("support-user-7", "customer-123", True))
print(sensitive_access("support-user-8", "customer-456", False))
```

### 16. Practical Question

> You are designing support access to customer PII. How would audit logging fit, and what trade-offs would you consider?

### 17. Strong Answer

I would emit structured audit events for every allowed and denied sensitive access, including actor, action, resource, result, reason, source, and correlation ID. It fits because support access must be accountable and reviewable. The trade-off is storage cost and privacy risk in the logs themselves. I would use append-only storage, redaction, ingestion durability, separation of duties, and alerts for suspicious patterns.

### 18. Revision Notes

- One-line summary: audit logs are durable evidence for sensitive actions.
- Three keywords: actor, action, evidence.
- One interview trap: confusing debug logs with audit logs.
- One memory trick: audit asks who did what to which thing.

---

## 7.6.4 RBAC vs ABAC

### 1. Intuition

RBAC says, "What role do you have?" ABAC says, "What attributes are true about you, the resource, the action, and the context?"

### 2. Definition

- Definition: Role-Based Access Control grants permissions through roles; Attribute-Based Access Control evaluates policies using attributes.
- Category: authorization model.
- Core idea: choose between simpler role mapping and more expressive policy evaluation.

### 3. Why It Exists

Authorization needs to be understandable and enforceable. RBAC works well for stable job roles, while ABAC handles context-sensitive policies such as region, tenant, ownership, risk, and time.

### 4. Reality

- Where used: admin tools, enterprise SaaS, cloud IAM, data platforms, support systems.
- Systems/products: IAM policies, OPA/Rego, Cedar-style policy engines, permission services.
- Teams: security, platform, enterprise product, internal tools.

### 5. How It Works

1. RBAC maps users or groups to roles.
2. Roles contain permissions.
3. ABAC collects subject, resource, action, and environment attributes.
4. Policy engine evaluates whether action is allowed.
5. Decision and reason are logged for audit and debugging.

### 6. What Problem It Solves

- Primary problem solved: controlling who can do what.
- Secondary benefits: tenant isolation, compliance, explainable access, policy reuse.
- Systems impact: shapes service APIs, data access, admin tools, and audit logs.

### 7. When to Rely on It

- Use RBAC when roles are stable and permission sets are understandable.
- Use ABAC when authorization depends on resource ownership, tenant, location, time, risk, or data class.
- Interview keywords: role explosion, policy engine, subject/resource/action, context.

### 8. When Not to Use It

- Avoid pure RBAC when roles multiply into hundreds of special cases.
- Avoid ABAC if the organization cannot explain, test, and audit policies.
- Use simple ownership checks for small systems before introducing a full policy engine.

### 9. Pros and Cons

| RBAC Pros | ABAC Cons |
|---|---|
| Easy to understand and audit | ABAC policies can become hard to reason about |
| Good for stable job roles | Attribute quality and freshness matter |
| Simple enforcement path | Policy engines add latency and operational ownership |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: RBAC simplicity or ABAC expressiveness.
- Give up: RBAC flexibility or ABAC simplicity.
- Latency/cost impact: ABAC may require policy engine calls and attribute fetching.

#### Common Mistakes

- Mistake: RBAC role explosion. Better approach: combine coarse roles with attributes.
- Mistake: ABAC policies with no test suite. Better approach: version policies and test decisions.
- Mistake: no denial reason. Better approach: log explainable authorization outcomes.

### 11. Key Numbers

- Metrics: denied decisions, policy evaluation latency, role count, unused permissions, policy test coverage.
- RBAC smell: too many exception roles.
- ABAC requirement: trusted attributes and versioned policies.

### 12. Failure Modes

- Wrong role grants broad access.
- Stale attribute allows access after tenant change.
- Policy update denies critical operations globally.
- Recovery: staged policy rollout, policy tests, break-glass access, audit diff, attribute freshness checks.

### 13. Scenario

- Product / system: enterprise SaaS admin console.
- Why this concept fits: admins have roles, but access may also depend on tenant, region, and data sensitivity.
- What would go wrong without it: either too many roles or overbroad permissions.

### 14. Code Sample

```python
def rbac_allows(role: str, action: str) -> bool:
    permissions = {"support": {"booking:read"}, "admin": {"booking:read", "booking:refund"}}
    # Staff concept: RBAC maps role to allowed actions.
    return action in permissions.get(role, set())
```

### 15. Mini Program / Simulation

```python
def abac_allows(subject: dict[str, str], resource: dict[str, str], action: str) -> bool:
    if action == "booking:read" and subject.get("tenant") == resource.get("tenant"):
        # Staff concept: ABAC includes subject and resource attributes.
        return subject.get("region") == resource.get("region")
    return False


print(rbac_allows("support", "booking:read"))
print(abac_allows({"tenant": "t1", "region": "eu"}, {"tenant": "t1", "region": "eu"}, "booking:read"))
```

### 16. Practical Question

> You are designing authorization for enterprise admin tools. How would you choose RBAC vs ABAC?

### 17. Strong Answer

I would start with RBAC for stable job roles and add ABAC where access depends on tenant, ownership, region, sensitivity, or time. RBAC is simpler to explain and audit, but it can create role explosion. ABAC is more expressive but needs trusted attributes, policy tests, and good observability. I would log decisions, stage policy changes, and use least privilege reviews to reduce overbroad roles.

### 18. Revision Notes

- One-line summary: RBAC uses roles; ABAC uses attributes and context.
- Three keywords: role, attribute, policy.
- One interview trap: solving every exception by adding another role.
- One memory trick: RBAC is job title, ABAC is full situation.

---

## 7.6.5 Least Privilege Enforcement

### 1. Intuition

Least privilege means every identity gets only the permissions it needs, for only as long as it needs them. It is permission dieting for systems and people.

### 2. Definition

- Definition: least privilege enforcement grants minimal required access to users, services, jobs, and infrastructure.
- Category: access control and security governance.
- Core idea: reduce damage from mistakes, compromise, and misuse.

### 3. Why It Exists

Overbroad permissions are convenient until an account is compromised or a bug uses permissions it never should have had. Least privilege reduces blast radius.

### 4. Reality

- Where used: IAM, Kubernetes service accounts, database users, CI/CD tokens, admin tools, cloud roles.
- Systems/products: IAM Access Analyzer, permission boundaries, just-in-time access, secret managers.
- Teams: security, platform, SRE, developer productivity, compliance.

### 5. How It Works

1. Identify actor and required actions.
2. Grant narrowly scoped permissions by resource and action.
3. Use temporary credentials where possible.
4. Monitor actual usage and remove unused access.
5. Review privileged access and exceptions regularly.

### 6. What Problem It Solves

- Primary problem solved: limiting damage from compromised or misused identities.
- Secondary benefits: auditability, compliance, safer automation, clearer ownership.
- Systems impact: affects IAM, service accounts, pipelines, operational access, and incident response.

### 7. When to Rely on It

- Use everywhere, especially production data, secrets, cloud resources, and admin actions.
- Strong fit for multi-team environments and regulated systems.
- Interview keywords: scoped role, JIT access, permission review, break-glass, privilege creep.

### 8. When Not to Use It

- Do not make access so strict that incidents cannot be mitigated.
- Avoid permanent broad admin roles for convenience.
- Use break-glass flows with audit for emergencies.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces blast radius | Requires ongoing review and tooling |
| Improves compliance | Can slow work if workflows are poor |
| Clarifies ownership | Fine-grained policies can be complex |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: smaller security blast radius.
- Give up: casual broad access.
- Latency/cost impact: more approval and automation work, fewer severe access incidents.

#### Common Mistakes

- Mistake: wildcard permissions in production roles. Better approach: scope actions and resources.
- Mistake: permanent emergency access. Better approach: time-bound break-glass with audit.
- Mistake: never pruning unused permissions. Better approach: usage-based access reviews.

### 11. Key Numbers

- Metrics: unused permissions, privileged identities, break-glass events, access review completion, policy violations.
- Review cadence: high-risk roles often reviewed quarterly or more often.
- Credential lifetime: prefer short-lived tokens for automation and humans.

### 12. Failure Modes

- CI token can delete production resources.
- Service account has access to all tenant data.
- Emergency access has no audit trail.
- Recovery: revoke/rotate credentials, reduce policy scope, audit use, add approval and time bounds.

### 13. Scenario

- Product / system: booking service accessing payment and profile data.
- Why this concept fits: the service should read only fields and resources needed for checkout.
- What would go wrong without it: a booking-service bug or compromise can read all customer PII.

### 14. Code Sample

```python
def scope_allows(scopes: set[str], required_scope: str) -> bool:
    # Staff concept: service calls should require explicit minimal scopes.
    return required_scope in scopes
```

### 15. Mini Program / Simulation

```python
service_scopes = {"booking:write", "profile:read-minimal"}


def call_profile_service(scopes: set[str], full_profile: bool) -> str:
    required = "profile:read-full" if full_profile else "profile:read-minimal"
    return "allowed" if scope_allows(scopes, required) else "denied"


print(call_profile_service(service_scopes, full_profile=False))
print(call_profile_service(service_scopes, full_profile=True))
```

### 16. Practical Question

> You are designing production access for services and engineers. How would least privilege enforcement work?

### 17. Strong Answer

I would define permissions by actor, action, resource, and duration. Services get narrow scopes and humans use just-in-time access for privileged operations. It fits because overbroad access increases breach and mistake blast radius. The trade-off is workflow complexity. I would add break-glass access with audit, monitor unused permissions, review privileged roles, and rotate credentials after incidents.

### 18. Revision Notes

- One-line summary: least privilege gives only the access needed, only when needed.
- Three keywords: scope, JIT, review.
- One interview trap: permanent broad admin access.
- One memory trick: give keys to one room, not the whole building.

---

## 7.6.6 Zero-Trust Principles

### 1. Intuition

Zero trust means the network location does not automatically make a request trustworthy. Every request must prove identity, context, and authorization.

### 2. Definition

- Definition: zero trust is a security model that continuously verifies identity, device, context, and policy for access decisions.
- Category: security architecture.
- Core idea: never trust by default, even inside the network perimeter.

### 3. Why It Exists

Traditional perimeter security assumes internal network traffic is mostly trusted. Cloud, remote work, microservices, compromised credentials, and lateral movement make that assumption unsafe.

### 4. Reality

- Where used: enterprise access, service-to-service communication, cloud platforms, admin tools, data access.
- Systems/products: identity-aware proxies, mTLS, device posture checks, service mesh, policy engines.
- Teams: security, platform, networking, identity, SRE.

### 5. How It Works

1. Authenticate every user, service, and device.
2. Authorize each request using identity, context, and policy.
3. Use least privilege and short-lived credentials.
4. Encrypt traffic and verify service identity, often with mTLS.
5. Continuously log, monitor, and adapt policy based on risk.

### 6. What Problem It Solves

- Primary problem solved: reducing lateral movement and implicit trust inside systems.
- Secondary benefits: stronger service identity, better access governance, safer remote access.
- Systems impact: affects networking, identity, service mesh, device posture, and authorization.

### 7. When to Rely on It

- Use for modern cloud and microservice environments, sensitive admin access, and regulated systems.
- Strong fit when internal network compromise is a realistic threat.
- Interview keywords: verify explicitly, least privilege, assume breach, mTLS, identity-aware proxy.

### 8. When Not to Use It

- Avoid buying tools without defining identity, policy, and ownership.
- Avoid trying to migrate everything at once without service inventory.
- Use risk-based rollout, starting with admin access and sensitive services.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces implicit trust | Requires identity and policy maturity |
| Limits lateral movement | Can add latency and operational complexity |
| Improves access visibility | Migration can be long and disruptive |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: stronger security posture and reduced breach blast radius.
- Give up: simple network-based trust.
- Latency/cost impact: more auth/policy checks and certificate operations, lower security risk.

#### Common Mistakes

- Mistake: zero trust means one product. Better approach: it is architecture plus process.
- Mistake: trusting internal IP ranges. Better approach: verify identity and policy per request.
- Mistake: no service inventory. Better approach: know identities and dependencies first.

### 11. Key Numbers

- Metrics: percent of traffic with mTLS, policy decision latency, unmanaged service count, denied access, certificate rotation failures.
- Rollout: start with highest-risk admin/data access before broad service mesh enforcement.
- Credential lifetime: short-lived credentials reduce replay risk.

### 12. Failure Modes

- Policy outage blocks critical services.
- Certificate expiry breaks service-to-service calls.
- Legacy service bypasses identity checks.
- Recovery: staged rollout, emergency policy bypass with audit, certificate automation, inventory enforcement.

### 13. Scenario

- Product / system: internal admin tools and microservices in a booking platform.
- Why this concept fits: compromise of one service should not allow free movement to customer data.
- What would go wrong without it: attackers use internal network trust to move laterally.

### 14. Code Sample

```python
def zero_trust_decision(identity_verified: bool, policy_allows: bool, device_healthy: bool) -> bool:
    # Staff concept: zero trust requires explicit verification, not network location trust.
    return identity_verified and policy_allows and device_healthy
```

### 15. Mini Program / Simulation

```python
requests = [
    {"identity_verified": True, "policy_allows": True, "device_healthy": True},
    {"identity_verified": True, "policy_allows": False, "device_healthy": True},
]


print([zero_trust_decision(**request) for request in requests])
```

### 16. Practical Question

> You are modernizing internal service and admin access. How would zero-trust principles shape the design?

### 17. Strong Answer

I would remove implicit network trust and require explicit identity, policy, and context checks for users and services. It fits because internal compromise and lateral movement are realistic risks. The trade-off is operational complexity, certificate/policy management, and migration effort. I would start with admin access and sensitive data paths, use mTLS and identity-aware proxies, automate certificate rotation, and monitor policy decision latency and denials.

### 18. Revision Notes

- One-line summary: zero trust verifies every access instead of trusting the network.
- Three keywords: identity, policy, context.
- One interview trap: calling zero trust a product purchase.
- One memory trick: inside the building still needs a badge check.

---

# Topic 7.7: Advanced Scaling and Migration Patterns

## 7.7.1 Shadow Traffic

### 1. Intuition

Shadow traffic is letting a new system watch real production requests without letting it affect users. It is rehearsal with real inputs and no visible output.

### 2. Definition

- Definition: shadow traffic duplicates production requests to a new path for observation while the original path remains authoritative.
- Category: migration validation and production testing.
- Core idea: test behavior, scale, and performance under real traffic before cutover.

### 3. Why It Exists

Synthetic tests rarely capture real request shapes, tenant behavior, headers, payload weirdness, and peak traffic. Shadowing gives confidence before a risky migration.

### 4. Reality

- Where used: search migrations, payment risk engines, recommendation services, pricing systems, API rewrites.
- Systems/products: API gateways, service mesh traffic mirroring, Kafka fanout, load-test replay tools.
- Teams: platform, SRE, backend, data, migration teams.

### 5. How It Works

1. Production request goes to old authoritative service.
2. A copy is sent asynchronously or out-of-band to the new service.
3. New service response is captured but not returned to the user.
4. Outputs, latency, errors, and resource usage are compared.
5. Traffic percentage increases as confidence grows.

### 6. What Problem It Solves

- Primary problem solved: validating a new path with real production traffic before user impact.
- Secondary benefits: catches payload edge cases, measures capacity, verifies parity.
- Systems impact: increases confidence but adds mirrored load and observability requirements.

### 7. When to Rely on It

- Use before replacing critical read paths or deterministic business logic.
- Strong fit when requests can be safely replayed without side effects.
- Interview keywords: mirror, dark read, parity, no side effects, production validation.

### 8. When Not to Use It

- Avoid mirroring side-effecting writes unless the shadow path is sandboxed.
- Avoid sending PII to unapproved environments.
- Use canary release when user-visible behavior needs real gradual rollout.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Tests with real traffic | Adds extra load |
| No user-visible risk if isolated | Side effects must be blocked |
| Finds data and payload edge cases | Requires parity comparison tooling |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: production realism without serving users from new path.
- Give up: simplicity and some extra capacity.
- Latency/cost impact: mirroring should not block the main path, but the shadow service needs capacity.

#### Common Mistakes

- Mistake: shadow writes create real side effects. Better approach: disable writes or use sandbox dependencies.
- Mistake: compare only status codes. Better approach: compare important fields, latency, and error classes.
- Mistake: mirror all traffic immediately. Better approach: ramp by tenant, route, or percentage.

### 11. Key Numbers

- Metrics: parity mismatch rate, shadow latency, shadow error rate, mirrored QPS, dropped mirror events.
- Safety: shadowing should add near-zero latency to authoritative path.
- Ramp: start tiny, then increase after mismatch and resource checks.

### 12. Failure Modes

- Shadow path accidentally mutates production state.
- Mirroring overloads new service or shared dependencies.
- PII is copied to an unapproved environment.
- Recovery: kill switch, sandbox dependencies, traffic cap, redaction, dependency isolation.

### 13. Scenario

- Product / system: migrating hotel search ranking to a new service.
- Why this concept fits: search is read-heavy and responses can be compared safely.
- What would go wrong without it: cutover exposes ranking bugs and latency regressions directly to users.

### 14. Code Sample

```python
def should_shadow(route: str, percentage: int, request_hash: int) -> bool:
    # Staff concept: shadow traffic ramps by deterministic sampling, not random chaos per retry.
    return route == "/search" and request_hash % 100 < percentage
```

### 15. Mini Program / Simulation

```python
def compare_search(old_result: list[str], new_result: list[str]) -> dict[str, int]:
    overlap = len(set(old_result[:5]) & set(new_result[:5]))
    return {"top5_overlap": overlap, "old_count": len(old_result), "new_count": len(new_result)}


print(should_shadow("/search", 10, 7))
print(compare_search(["h1", "h2", "h3"], ["h2", "h1", "h4"]))
```

### 16. Practical Question

> You are replacing a critical search service. How would shadow traffic reduce migration risk?

### 17. Strong Answer

I would mirror a small percentage of production search requests to the new service while continuing to serve users from the old service. The new path would be read-only or isolated from side effects. I would compare response parity, latency, and errors, then ramp by route or tenant. The trade-off is extra load and comparison complexity, so I would add a kill switch, dependency isolation, and PII controls.

### 18. Revision Notes

- One-line summary: shadow traffic tests a new path with real requests without serving users from it.
- Three keywords: mirror, parity, side effects.
- One interview trap: shadowing writes into real dependencies.
- One memory trick: the new system watches the game before joining it.

---

## 7.7.2 Dual Writes

### 1. Intuition

Dual writes means writing the same business change to two places during migration. It sounds simple, but it is one of the easiest ways to create inconsistent state.

### 2. Definition

- Definition: dual writes update two systems from one logical operation, usually during a migration or data model transition.
- Category: migration and consistency pattern.
- Core idea: keep old and new stores populated while moving traffic gradually.

### 3. Why It Exists

During migrations, the old system may still serve production while the new system needs fresh data. Dual writes bridge the transition, but failures between writes create divergence.

### 4. Reality

- Where used: database migrations, monolith extraction, search index updates, cache/index side writes, event-driven migrations.
- Systems/products: outbox pattern, CDC pipelines, transactional logs, migration jobs.
- Teams: backend, data platform, SRE, platform migration teams.

### 5. How It Works

1. Request updates authoritative source.
2. Second write updates new store, event stream, or extracted service.
3. Failures are retried or repaired by reconciliation.
4. Reads gradually shift after validation.
5. Old write path is removed after cutover and backout window.

### 6. What Problem It Solves

- Primary problem solved: keeping old and new systems in sync during migration.
- Secondary benefits: gradual rollout, validation, fallback capability.
- Systems impact: introduces temporary consistency, retry, and reconciliation complexity.

### 7. When to Rely on It

- Use when both old and new systems must receive updates during transition.
- Strong fit when paired with outbox, idempotency, and reconciliation.
- Interview keywords: divergence, outbox, idempotency, reconciliation, cutover.

### 8. When Not to Use It

- Avoid naive two independent writes in request path without failure handling.
- Avoid if the second system can be built from CDC or backfill more safely.
- Use transactional outbox when atomicity with event publication is needed.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Keeps new system fresh | Creates divergence risk |
| Enables gradual cutover | Adds temporary code paths |
| Supports fallback | Requires reconciliation and idempotency |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: migration flexibility and warm new system.
- Give up: single-source simplicity during migration.
- Latency/cost impact: extra write cost and possible request latency if synchronous.

#### Common Mistakes

- Mistake: write DB then remote service with no retry. Better approach: transactional outbox or CDC.
- Mistake: no idempotency key. Better approach: make retries safe.
- Mistake: no reconciliation. Better approach: compare and repair until cutover completes.

### 11. Key Numbers

- Metrics: dual-write success rate, divergence count, retry backlog, reconciliation lag, repair count.
- Cutover gate: divergence should be below defined threshold for a sustained period.
- Backout: define whether old or new system is authoritative during rollback.

### 12. Failure Modes

- First write succeeds and second fails.
- Retry duplicates side effects.
- New schema cannot represent old state correctly.
- Recovery: idempotency, outbox, reconciliation job, authoritative-source decision, rollback plan.

### 13. Scenario

- Product / system: extracting booking preferences from a monolith into a profile service.
- Why this concept fits: old monolith still serves reads while new service is warmed.
- What would go wrong without it: new service starts stale or cutover loses recent updates.

### 14. Code Sample

```python
def outbox_event(aggregate_id: str, version: int, payload: dict[str, str]) -> dict[str, object]:
    # Staff concept: outbox makes the second write retryable and idempotent.
    return {"idempotency_key": f"{aggregate_id}:{version}", "payload": payload}
```

### 15. Mini Program / Simulation

```python
published: set[str] = set()


def publish_once(event: dict[str, object]) -> str:
    key = str(event["idempotency_key"])
    if key in published:
        return "duplicate_ignored"
    published.add(key)
    return "published"


event = outbox_event("booking-1", 2, {"seat": "aisle"})
print(publish_once(event))
print(publish_once(event))
```

### 16. Practical Question

> You are migrating writes from a monolith table to a new service. How would you handle dual writes safely?

### 17. Strong Answer

I would avoid naive independent writes. I would keep one authoritative write, record an outbox event transactionally, and have a worker update the new system with idempotency. During migration I would run reconciliation to find divergence, then shift reads only after error rates and lag are acceptable. The trade-off is temporary complexity and eventual consistency, but it gives safer retries and rollback.

### 18. Revision Notes

- One-line summary: dual writes keep two systems updated during migration but risk divergence.
- Three keywords: outbox, idempotency, reconciliation.
- One interview trap: two writes in the request path with no recovery story.
- One memory trick: two notebooks need one reliable copying process.

---

## 7.7.3 Read/Write Splitting

### 1. Intuition

Read/write splitting sends writes to the primary and many reads to replicas. It scales reads, but users may read stale data right after writing.

### 2. Definition

- Definition: read/write splitting routes mutations to a primary data source and eligible reads to replicas or read-optimized stores.
- Category: database scaling and consistency pattern.
- Core idea: scale read load while preserving a clear write authority.

### 3. Why It Exists

Many workloads are read-heavy. A single primary may handle writes but not all reads. Replicas increase read capacity, but replication lag changes consistency behavior.

### 4. Reality

- Where used: relational DB replicas, search indexes, reporting stores, CQRS read models, cache-aside patterns.
- Systems/products: MySQL/PostgreSQL replicas, Aurora readers, Elasticsearch, materialized views.
- Teams: backend, database, SRE, data platform.

### 5. How It Works

1. Writes go to primary or authoritative service.
2. Replication sends changes to read replicas.
3. Router chooses primary for writes and read-after-write-sensitive reads.
4. Router chooses replicas for safe stale-tolerant reads.
5. Lag is monitored and traffic shifts if replicas fall behind.

### 6. What Problem It Solves

- Primary problem solved: scaling read throughput without scaling writes equally.
- Secondary benefits: analytical isolation, lower primary load, regional read latency reduction.
- Systems impact: introduces consistency and routing decisions into application design.

### 7. When to Rely on It

- Use for read-heavy workloads where many reads can tolerate slight staleness.
- Strong fit for browse/search/reporting and dashboard reads.
- Interview keywords: primary, replica, replication lag, read-after-write, stale reads.

### 8. When Not to Use It

- Avoid replica reads for payment confirmation, inventory reservation, or immediate post-write reads.
- Avoid if application cannot tolerate stale data and primary can handle load.
- Use sticky primary reads or session consistency after user writes.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Scales read traffic | Replica lag causes stale reads |
| Reduces primary load | Query routing becomes complex |
| Supports regional reads | Failover and consistency need care |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: read scalability and primary protection.
- Give up: simple single-source read consistency.
- Latency/cost impact: lower read latency/capacity pressure, higher infrastructure cost.

#### Common Mistakes

- Mistake: all reads go to replicas. Better approach: route consistency-sensitive reads to primary.
- Mistake: ignore lag. Better approach: monitor lag and remove unhealthy replicas.
- Mistake: user writes then sees stale profile. Better approach: sticky primary or session read token.

### 11. Key Numbers

- Metrics: replica lag, primary CPU, replica CPU, stale-read incidents, read/write QPS split.
- SLO: maximum acceptable lag depends on business flow.
- Capacity: read replicas multiply read capacity but do not remove write bottlenecks.

### 12. Failure Modes

- Replica lag grows during write spike.
- Read router sends critical reads to stale replica.
- Failover promotes replica with missing writes.
- Recovery: lag-aware routing, primary fallback, session consistency, failover validation.

### 13. Scenario

- Product / system: hotel browsing with many catalog reads and fewer booking writes.
- Why this concept fits: browsing can tolerate small lag, but booking confirmation cannot.
- What would go wrong without it: primary DB becomes overloaded by read traffic.

### 14. Code Sample

```python
def choose_read_source(needs_fresh_data: bool, replica_lag_ms: int, max_lag_ms: int) -> str:
    # Staff concept: read routing must include consistency needs and replica health.
    if needs_fresh_data or replica_lag_ms > max_lag_ms:
        return "primary"
    return "replica"
```

### 15. Mini Program / Simulation

```python
reads = [
    {"name": "browse_hotels", "fresh": False, "lag": 80},
    {"name": "booking_confirmation", "fresh": True, "lag": 20},
    {"name": "dashboard", "fresh": False, "lag": 2000},
]


print({read["name"]: choose_read_source(read["fresh"], read["lag"], 500) for read in reads})
```

### 16. Practical Question

> You are scaling a read-heavy booking platform. How would read/write splitting help, and what risks would you handle?

### 17. Strong Answer

I would route writes and consistency-sensitive reads to the primary, while sending stale-tolerant reads like browsing or reports to replicas. This reduces primary load and scales read throughput. The main risk is stale reads from replica lag, especially after writes. I would monitor lag, use primary fallback, add sticky primary reads after user writes, and define which endpoints can tolerate staleness.

### 18. Revision Notes

- One-line summary: read/write splitting scales reads by using replicas while writes stay authoritative.
- Three keywords: primary, replica, lag.
- One interview trap: sending all reads to replicas.
- One memory trick: write to the captain, read from copies when freshness allows.

---

## 7.7.4 Strangler Fig Pattern

### 1. Intuition

Strangler Fig migration wraps an old system and gradually routes pieces to new services. The new system grows around the old one until the old part can be retired.

### 2. Definition

- Definition: Strangler Fig pattern incrementally replaces a legacy system by routing selected capabilities to new implementations while the old system continues running.
- Category: modernization and migration pattern.
- Core idea: avoid big-bang rewrite by migrating capability by capability.

### 3. Why It Exists

Big rewrites are risky, slow, and often fail before delivering value. Strangler migration lets teams ship improvements while preserving business continuity.

### 4. Reality

- Where used: monolith decomposition, legacy API replacement, frontend/backend modernization, database migrations.
- Systems/products: API gateway routing, facade services, anti-corruption layers, feature flags.
- Teams: platform, backend, product engineering, SRE, architecture.

### 5. How It Works

1. Put a routing layer or facade in front of legacy system.
2. Choose one capability to extract.
3. Build new implementation and data integration.
4. Route a small percentage or tenant subset to new path.
5. Validate, expand, and retire old capability after confidence.

### 6. What Problem It Solves

- Primary problem solved: reducing risk of large legacy replacement.
- Secondary benefits: incremental delivery, safer rollback, clearer ownership.
- Systems impact: adds routing, compatibility, data sync, and temporary duplicate paths.

### 7. When to Rely on It

- Use when a legacy system is too large or critical for a big-bang rewrite.
- Strong fit when capabilities can be isolated behind stable contracts.
- Interview keywords: facade, routing, anti-corruption layer, incremental migration, retire legacy.

### 8. When Not to Use It

- Avoid if the legacy system is small and direct replacement is cheaper.
- Avoid extracting capabilities without clear domain boundaries.
- Use modularization first if the monolith boundaries are too tangled.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Avoids big-bang rewrite | Temporary routing/data complexity |
| Delivers value incrementally | Old and new systems coexist longer |
| Enables safer rollback | Boundary selection is hard |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: incremental modernization and lower cutover risk.
- Give up: short-term architectural simplicity.
- Latency/cost impact: facade/routing adds hops, but reduces rewrite risk.

#### Common Mistakes

- Mistake: extract by technical layer instead of business capability. Better approach: migrate bounded capabilities.
- Mistake: no retirement plan. Better approach: define deletion criteria for old code.
- Mistake: facade becomes permanent junk drawer. Better approach: keep routing thin and owned.

### 11. Key Numbers

- Metrics: percentage of traffic on new path, legacy endpoint count, parity mismatches, rollback count, retired code count.
- Migration unit: capability, route, tenant, or workflow.
- Exit gate: old capability has zero traffic and no required data ownership.

### 12. Failure Modes

- Routing sends requests to wrong implementation.
- New service depends on legacy in circular ways.
- Old code never gets retired.
- Recovery: feature flags, route ownership, dependency mapping, deletion milestones, observability per path.

### 13. Scenario

- Product / system: replacing a legacy booking monolith capability by capability.
- Why this concept fits: booking is critical and cannot be rewritten all at once.
- What would go wrong without it: big-bang rewrite delays value and creates high cutover risk.

### 14. Code Sample

```python
def route_capability(capability: str, migrated: set[str]) -> str:
    # Staff concept: strangler routing moves one capability at a time.
    return "new_service" if capability in migrated else "legacy_monolith"
```

### 15. Mini Program / Simulation

```python
migrated_capabilities = {"search", "reviews"}
for capability in ["search", "booking", "reviews", "refunds"]:
    print(capability, route_capability(capability, migrated_capabilities))
```

### 16. Practical Question

> You are modernizing a large legacy monolith. How would the Strangler Fig pattern reduce risk?

### 17. Strong Answer

I would put a facade or gateway in front of the monolith and migrate one business capability at a time. Each extracted capability would have clear routing, data ownership, parity checks, and rollback. The trade-off is temporary coexistence complexity and extra routing. I would avoid a big-bang rewrite, track traffic migration and legacy deletion, and prevent the facade from becoming a permanent dumping ground.

### 18. Revision Notes

- One-line summary: Strangler Fig replaces legacy systems gradually by routing capabilities to new implementations.
- Three keywords: facade, capability, retire.
- One interview trap: migrating by technical layer instead of business capability.
- One memory trick: new growth surrounds old system one branch at a time.

---

## 7.7.5 Monolith to Microservices Evolution

### 1. Intuition

Moving from monolith to microservices is not splitting files into network calls. It is gradually creating independently owned, observable, deployable business capabilities.

### 2. Definition

- Definition: monolith to microservices evolution incrementally decomposes a shared application into separately owned services around business boundaries.
- Category: architecture modernization.
- Core idea: gain independent change and scaling by accepting distributed-systems complexity.

### 3. Why It Exists

Large monoliths can slow teams, couple deployments, and make scaling uneven. Microservices help when organizational and technical boundaries need independent ownership.

### 4. Reality

- Where used: large SaaS platforms, marketplaces, banking, travel, ecommerce, logistics.
- Systems/products: service platforms, API gateways, event buses, service meshes, observability stacks.
- Teams: product engineering, platform, SRE, architecture, data platform.

### 5. How It Works

1. Stabilize the monolith with tests and observability.
2. Identify bounded contexts and ownership seams.
3. Extract one low-risk/high-value capability.
4. Define APIs, data ownership, and operational SLOs.
5. Repeat with migration patterns such as Strangler, events, and outbox.

### 6. What Problem It Solves

- Primary problem solved: reducing team and deployment coupling in large systems.
- Secondary benefits: independent scaling, clearer ownership, technology flexibility.
- Systems impact: increases network, consistency, observability, and platform requirements.

### 7. When to Rely on It

- Use when team autonomy, deployment independence, or scaling boundaries are real constraints.
- Strong fit when domains are clear and platform maturity exists.
- Interview keywords: bounded context, data ownership, operational maturity, distributed complexity.

### 8. When Not to Use It

- Avoid microservices for small teams or unclear domains.
- Avoid splitting before the monolith has tests and observability.
- Use modular monolith if organizational scale does not require distributed services.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Independent deployment and ownership | Distributed complexity |
| Service-specific scaling | Network latency and failure handling |
| Clearer domain boundaries | Data consistency becomes harder |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: autonomy and targeted scaling.
- Give up: in-process simplicity and single transaction boundaries.
- Latency/cost impact: network calls, more infrastructure, more observability needs.

#### Common Mistakes

- Mistake: split database tables first with no domain boundary. Better approach: start with business capabilities.
- Mistake: distributed monolith. Better approach: independent deployability and ownership.
- Mistake: no platform support. Better approach: logging, tracing, CI/CD, service discovery, and SLOs first.

### 11. Key Numbers

- Metrics: deploy frequency, lead time, service SLOs, cross-service call count, incident ownership clarity.
- Service smell: too many synchronous calls for one user action.
- Extraction gate: clear owner, API, data model, tests, dashboards, rollback.

### 12. Failure Modes

- Service extraction creates chatty distributed monolith.
- Data ownership remains shared and ambiguous.
- Teams cannot operate services they own.
- Recovery: domain review, platform investment, API contracts, async boundaries, service ownership model.

### 13. Scenario

- Product / system: travel platform monolith with search, booking, payment, loyalty, and support modules.
- Why this concept fits: different teams need independent release and scaling cycles.
- What would go wrong without it: every change requires monolith-wide coordination and risky releases.

### 14. Code Sample

```python
def extraction_score(team_owner: bool, clear_data_owner: bool, high_change_rate: bool) -> int:
    # Staff concept: service extraction should be driven by ownership and change pressure.
    return sum([team_owner, clear_data_owner, high_change_rate])
```

### 15. Mini Program / Simulation

```python
candidates = {
    "loyalty": (True, True, True),
    "shared_utils": (False, False, True),
    "payments": (True, True, False),
}


print({name: extraction_score(*signals) for name, signals in candidates.items()})
```

### 16. Practical Question

> A team wants to split a monolith into microservices. What would you ask before approving the design?

### 17. Strong Answer

I would ask whether the driver is team autonomy, deployment independence, scaling, or domain ownership. I would not split just because microservices sound modern. I would first ensure tests, observability, CI/CD, service ownership, and data boundaries exist. The trade-off is gaining autonomy while accepting network failures, eventual consistency, and operational cost. I would start with one clear business capability and migrate incrementally.

### 18. Revision Notes

- One-line summary: monolith to microservices is an organizational and domain evolution, not just code splitting.
- Three keywords: ownership, boundary, operations.
- One interview trap: creating a distributed monolith.
- One memory trick: split when teams and domains need independent life.

---

## 7.7.6 Contract Testing

### 1. Intuition

Contract testing checks that service producers and consumers agree on the shape and meaning of their API. It prevents one team from changing a service in a way that silently breaks another.

### 2. Definition

- Definition: contract testing verifies compatibility between service providers and consumers using explicit API or message contracts.
- Category: integration testing and service governance.
- Core idea: catch breaking changes before deployment by testing expectations at the boundary.

### 3. Why It Exists

In distributed systems, end-to-end tests are expensive and flaky, while unit tests do not prove compatibility. Contract tests give focused confidence at service boundaries.

### 4. Reality

- Where used: microservices, event-driven systems, public APIs, SDK-backed APIs, schema registries.
- Systems/products: Pact, OpenAPI checks, protobuf compatibility tests, schema registry compatibility rules.
- Teams: platform, API teams, microservice owners, QA, developer productivity.

### 5. How It Works

1. Consumer defines expected request/response or message shape.
2. Contract is stored and versioned.
3. Provider runs tests to prove it satisfies consumer expectations.
4. CI blocks breaking changes.
5. Contracts evolve using backward-compatible rules.

### 6. What Problem It Solves

- Primary problem solved: preventing integration breakage between independently deployed services.
- Secondary benefits: faster CI, clearer API ownership, safer schema evolution.
- Systems impact: improves release confidence without relying only on full end-to-end tests.

### 7. When to Rely on It

- Use when services deploy independently and consumers rely on stable APIs/events.
- Strong fit for microservices, event schemas, and external APIs.
- Interview keywords: consumer-driven contract, backward compatibility, schema evolution, CI gate.

### 8. When Not to Use It

- Avoid replacing all integration tests with contract tests.
- Avoid contracts that test provider implementation details instead of boundary behavior.
- Use end-to-end tests for critical workflows that require multiple services together.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Catches breaking API changes early | Requires contract ownership and versioning |
| Faster than broad end-to-end tests | False confidence if contracts are incomplete |
| Supports independent deployments | Tooling and CI discipline are needed |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: focused boundary confidence.
- Give up: relying only on live integrated environments.
- Latency/cost impact: faster feedback in CI, some maintenance cost for contracts.

#### Common Mistakes

- Mistake: provider changes response fields without consumer tests. Better approach: run consumer contracts in provider CI.
- Mistake: contract tests include database behavior. Better approach: test API/message boundary.
- Mistake: remove end-to-end smoke tests. Better approach: combine contract tests with small critical E2E suite.

### 11. Key Numbers

- Metrics: contract test failures, breaking changes blocked, contract coverage by consumer, schema compatibility violations.
- Compatibility: adding optional fields is usually safer than removing or renaming fields.
- CI gate: provider deploy should verify active consumer contracts.

### 12. Failure Modes

- Consumer expectation not captured, so provider breaks it.
- Contract registry is stale.
- Provider supports contract but behavior semantics changed.
- Recovery: consumer-driven updates, version contracts, semantic examples, production canaries, E2E smoke tests.

### 13. Scenario

- Product / system: booking service consumes pricing API.
- Why this concept fits: pricing team deploys independently and must not break booking checkout.
- What would go wrong without it: a response rename breaks checkout only after deployment.

### 14. Code Sample

```python
def validates_price_contract(response: dict[str, object]) -> bool:
    # Staff concept: contract tests protect the consumer-visible boundary.
    return isinstance(response.get("total"), (int, float)) and response.get("currency") in {"USD", "EUR", "INR"}
```

### 15. Mini Program / Simulation

```python
responses = [
    {"total": 129.99, "currency": "USD"},
    {"amount": 129.99, "currency": "USD"},
]


print([validates_price_contract(response) for response in responses])
```

### 16. Practical Question

> You have independently deployed booking and pricing services. How would contract testing help?

### 17. Strong Answer

I would define consumer-driven contracts for the pricing responses that booking depends on, then run those contracts in the pricing service CI before deployment. This catches breaking changes like field removal or type changes early. The trade-off is maintaining versioned contracts and not confusing them with full end-to-end tests. I would still keep a small checkout smoke test and use schema compatibility rules for events.

### 18. Revision Notes

- One-line summary: contract testing protects service boundaries from breaking changes.
- Three keywords: consumer, provider, compatibility.
- One interview trap: replacing all integration testing with contracts.
- One memory trick: contracts are promises services test before shipping.

---

# Topic 7.8: Decision Narration and Staff-Level Communication

## 7.8.1 Assumption Declaration

### 1. Intuition

Assumption declaration is saying out loud what you are taking as true before designing. It prevents hidden guesses from becoming accidental architecture.

### 2. Definition

- Definition: assumption declaration explicitly states workload, user, business, regulatory, and operational assumptions before making design decisions.
- Category: staff-level communication and design framing.
- Core idea: make the design's starting conditions visible and testable.

### 3. Why It Exists

System design discussions fail when people optimize for different imagined realities. Clear assumptions align the room and make later decisions understandable.

### 4. Reality

- Where used: architecture reviews, incident reviews, design docs, interviews, roadmap planning.
- Systems/products: design RFCs, ADRs, architecture review boards, planning docs.
- Teams: engineering leadership, product, SRE, security, data, architecture.

### 5. How It Works

1. Identify unknowns that affect design.
2. State assumptions clearly and early.
3. Label which assumptions are risky or need validation.
4. Connect decisions back to those assumptions.
5. Update design if assumptions change.

### 6. What Problem It Solves

- Primary problem solved: avoiding mismatched mental models.
- Secondary benefits: better review quality, faster alignment, clearer trade-offs.
- Systems impact: improves decision quality before code or infrastructure is built.

### 7. When to Rely on It

- Use at the start of design docs, interviews, migration plans, and incident remediations.
- Strong fit when requirements are ambiguous or high-stakes.
- Interview keywords: assumptions, constraints, unknowns, validate, revisit.

### 8. When Not to Use It

- Avoid long lists of obvious assumptions that do not affect decisions.
- Avoid treating assumptions as facts forever.
- Use discovery or measurement when an assumption is too risky to leave unvalidated.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Aligns reviewers quickly | Can become boilerplate if not decision-relevant |
| Makes trade-offs explainable | Wrong assumptions can mislead design |
| Helps identify validation work | Requires discipline to update |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: clarity and shared context.
- Give up: rushing directly into diagrams and components.
- Latency/cost impact: small upfront time reduces costly redesign later.

#### Common Mistakes

- Mistake: assumptions hidden in the solution. Better approach: state them before the design.
- Mistake: no confidence level. Better approach: mark assumption confidence and validation plan.
- Mistake: assumptions never revisited. Better approach: update ADR/RFC when reality changes.

### 11. Key Numbers

- Useful assumption categories: traffic, data size, latency, availability, compliance, team ownership, cost.
- Metrics: assumptions validated, assumptions changed, design decisions affected.
- Interview target: state 3-6 important assumptions, not 30 generic ones.

### 12. Failure Modes

- Design optimizes for wrong traffic shape.
- Compliance constraint discovered too late.
- Teams disagree because hidden priorities differ.
- Recovery: pause design, restate assumptions, gather data, revise decisions, document changes.

### 13. Scenario

- Product / system: designing global booking checkout.
- Why this concept fits: latency, inventory consistency, payment risk, and regional compliance assumptions drive architecture.
- What would go wrong without it: reviewers argue about solutions while assuming different business constraints.

### 14. Code Sample

```python
def risky_assumptions(assumptions: dict[str, str]) -> list[str]:
    # Staff concept: assumptions with low confidence need validation before architecture hardens.
    return [name for name, confidence in assumptions.items() if confidence == "low"]
```

### 15. Mini Program / Simulation

```python
assumptions = {
    "peak_qps_known": "high",
    "payment_provider_latency": "medium",
    "data_residency_rules": "low",
}


print(risky_assumptions(assumptions))
```

### 16. Practical Question

> In a system design interview or review, how would you begin when requirements are incomplete?

### 17. Strong Answer

I would state the key assumptions that drive architecture, such as traffic, data size, latency target, consistency needs, compliance constraints, and team ownership. I would mark uncertain assumptions and explain how I would validate them. This matters because the right design changes when assumptions change. It also helps reviewers understand why I choose one trade-off over another.

### 18. Revision Notes

- One-line summary: assumption declaration makes the design's starting point explicit.
- Three keywords: unknowns, constraints, validate.
- One interview trap: jumping to components before framing assumptions.
- One memory trick: name the ground before building on it.

---

## 7.8.2 Multiple-Option Framing

### 1. Intuition

Multiple-option framing means you do not present one design as destiny. You show credible alternatives and explain why one best fits the current constraints.

### 2. Definition

- Definition: multiple-option framing compares several viable approaches before recommending one.
- Category: staff-level design communication.
- Core idea: demonstrate judgment by considering alternatives, not just proposing a favorite.

### 3. Why It Exists

Senior decisions are rarely about one obvious answer. Leaders need to see that you considered cost, risk, reversibility, speed, and business goals.

### 4. Reality

- Where used: design docs, ADRs, architecture reviews, vendor selections, migration plans, interview answers.
- Systems/products: RFC templates, ADR repositories, planning docs, review boards.
- Teams: staff engineers, architects, product leadership, SRE, security.

### 5. How It Works

1. Identify two to four realistic options.
2. Describe each option neutrally.
3. Compare using decision criteria.
4. Recommend one option for current constraints.
5. Explain what would make another option better later.

### 6. What Problem It Solves

- Primary problem solved: avoiding one-track design decisions.
- Secondary benefits: alignment, transparency, better challenge from reviewers.
- Systems impact: improves architecture review and reduces surprise objections.

### 7. When to Rely on It

- Use for significant architecture, migration, vendor, reliability, or security choices.
- Strong fit when trade-offs are real and stakeholders differ.
- Interview keywords: options, criteria, recommendation, constraints, reversibility.

### 8. When Not to Use It

- Avoid overdoing it for tiny implementation details.
- Avoid fake options that no one would choose.
- Use direct decision-making when the choice is standard and low-risk.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Shows judgment and breadth | Takes more preparation |
| Reduces review churn | Too many options can confuse |
| Makes recommendation defensible | Weak criteria produce weak conclusions |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: transparent decision process.
- Give up: speed of presenting only one answer.
- Latency/cost impact: upfront thinking reduces expensive reversals.

#### Common Mistakes

- Mistake: strawman alternatives. Better approach: include credible options.
- Mistake: compare without criteria. Better approach: define criteria first.
- Mistake: no recommendation. Better approach: choose and explain why.

### 11. Key Numbers

- Good option count: usually 2-4.
- Criteria examples: latency, consistency, cost, reliability, compliance, delivery time, operational ownership.
- Output: one recommendation plus revisit triggers.

### 12. Failure Modes

- Team debates preferences without criteria.
- Reviewer rejects design because alternatives were not considered.
- Decision becomes analysis paralysis.
- Recovery: constrain options, choose criteria, timebox decision, document recommendation.

### 13. Scenario

- Product / system: choosing between synchronous checkout inventory validation, reservation service, or event-driven hold.
- Why this concept fits: each option trades latency, correctness, and complexity differently.
- What would go wrong without it: the team may overbuild or pick a low-latency option that oversells inventory.

### 14. Code Sample

```python
def rank_option(scores: dict[str, int], weights: dict[str, int]) -> int:
    # Staff concept: options should be compared against explicit criteria.
    return sum(scores[criterion] * weights[criterion] for criterion in weights)
```

### 15. Mini Program / Simulation

```python
weights = {"reliability": 3, "cost": 1, "speed": 2}
options = {
    "simple_replica": {"reliability": 2, "cost": 3, "speed": 3},
    "event_driven": {"reliability": 3, "cost": 2, "speed": 2},
}


print({name: rank_option(scores, weights) for name, scores in options.items()})
```

### 16. Practical Question

> How would you present architecture options in a staff-level design review?

### 17. Strong Answer

I would present two to four credible options, define decision criteria, compare trade-offs, and make a clear recommendation. I would also say what would make another option better in the future. This shows I am not attached to one design blindly. The trade-off is taking more upfront time, but it helps reviewers align and challenge the decision productively.

### 18. Revision Notes

- One-line summary: multiple-option framing compares credible alternatives before recommending one.
- Three keywords: options, criteria, recommendation.
- One interview trap: listing options but never choosing.
- One memory trick: show the menu, then order with reasons.

---

## 7.8.3 Explicit Trade-Off Comparison

### 1. Intuition

Explicit trade-off comparison means saying what improves and what gets worse. Staff engineers do not hide costs; they make them visible.

### 2. Definition

- Definition: explicit trade-off comparison evaluates design choices across concrete dimensions such as latency, availability, consistency, cost, complexity, and risk.
- Category: architecture communication and decision quality.
- Core idea: every meaningful design decision buys something by spending something else.

### 3. Why It Exists

Architecture decisions fail when teams hear only benefits. Clear trade-offs help stakeholders make informed business and technical decisions.

### 4. Reality

- Where used: RFCs, ADRs, roadmap decisions, reliability investments, cloud cost reviews, incident remediations.
- Systems/products: design review templates, decision records, risk registers.
- Teams: engineering, product, finance, security, SRE, leadership.

### 5. How It Works

1. Define the decision being made.
2. Select relevant comparison dimensions.
3. Compare options honestly across those dimensions.
4. Highlight non-negotiable constraints.
5. State accepted risks and mitigations.

### 6. What Problem It Solves

- Primary problem solved: making hidden costs and risks visible.
- Secondary benefits: better stakeholder alignment, fewer surprises, clearer mitigation work.
- Systems impact: improves long-term maintainability and operational readiness.

### 7. When to Rely on It

- Use whenever architecture has meaningful cost, reliability, complexity, or delivery implications.
- Strong fit for CAP-style choices, scaling approaches, migration strategies, and security controls.
- Interview keywords: latency vs consistency, cost vs reliability, complexity vs speed, accepted risk.

### 8. When Not to Use It

- Avoid overanalyzing tiny changes.
- Avoid generic trade-off words without concrete consequences.
- Use direct implementation notes for simple local changes.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Makes decisions honest | Can slow low-risk work if overused |
| Improves stakeholder alignment | Requires measurable criteria |
| Clarifies mitigations | May surface uncomfortable risks |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: informed decision-making.
- Give up: comforting simplicity of one-sided proposals.
- Latency/cost impact: small review overhead, lower risk of expensive surprises.

#### Common Mistakes

- Mistake: only listing pros. Better approach: list what gets worse too.
- Mistake: vague trade-offs. Better approach: name concrete impact and mitigation.
- Mistake: no owner for accepted risk. Better approach: assign owner and review date.

### 11. Key Numbers

- Common dimensions: latency, availability, consistency, cost, complexity, operability, security, delivery time.
- Output: accepted risks, mitigations, owners, review trigger.
- Interview target: compare 3-5 dimensions clearly.

### 12. Failure Modes

- Team chooses cheap option without reliability implications understood.
- Security risk accepted implicitly and discovered later.
- Cost doubles because operational load was ignored.
- Recovery: revisit trade-off matrix, document accepted risk, define mitigations, adjust roadmap.

### 13. Scenario

- Product / system: choosing active-active multi-region checkout.
- Why this concept fits: active-active improves availability and latency but increases conflict handling and cost.
- What would go wrong without it: team underestimates data consistency and operational complexity.

### 14. Code Sample

```python
def tradeoff_summary(gain: str, cost: str, mitigation: str) -> str:
    # Staff concept: good trade-off language names benefit, cost, and mitigation together.
    return f"Gain: {gain}; Cost: {cost}; Mitigation: {mitigation}"
```

### 15. Mini Program / Simulation

```python
print(tradeoff_summary(
    "lower regional latency",
    "conflict resolution complexity",
    "single-writer inventory plus active-active reads",
))
```

### 16. Practical Question

> How would you explain the trade-off of using asynchronous replication across regions?

### 17. Strong Answer

I would say asynchronous replication improves write latency and regional availability because the primary does not wait for every remote region. The cost is possible data loss within the replication lag window and stale reads in replicas. I would mitigate with RPO targets, lag monitoring, idempotent operations, and routing critical reads to authoritative sources. That makes the accepted risk explicit.

### 18. Revision Notes

- One-line summary: explicit trade-off comparison names what you gain and what you pay.
- Three keywords: gain, cost, mitigation.
- One interview trap: saying only the benefits.
- One memory trick: every design receipt has a price line.

---

## 7.8.4 Justified Decision Making

### 1. Intuition

Justified decision making is choosing a path and tying it to constraints, evidence, and goals. It turns preference into accountable reasoning.

### 2. Definition

- Definition: justified decision making records why a design was chosen based on requirements, evidence, trade-offs, and constraints.
- Category: architecture governance and staff communication.
- Core idea: decisions should be explainable, reviewable, and revisitable.

### 3. Why It Exists

Large systems outlive the meeting where decisions were made. Future teams need to understand why the design exists and when it should change.

### 4. Reality

- Where used: ADRs, RFCs, technical strategy, architecture review, compliance evidence, roadmap choices.
- Systems/products: ADR repositories, design docs, architecture boards, RFC systems.
- Teams: staff engineers, architects, engineering managers, SRE, security, product.

### 5. How It Works

1. State the decision clearly.
2. Tie it to goals, constraints, and assumptions.
3. Summarize alternatives considered.
4. Explain trade-offs and accepted risks.
5. Define revisit triggers and owners.

### 6. What Problem It Solves

- Primary problem solved: preventing architecture from becoming unexplained folklore.
- Secondary benefits: smoother onboarding, better reviews, easier future changes.
- Systems impact: improves continuity and reduces repeated debates.

### 7. When to Rely on It

- Use for decisions that affect architecture, cost, reliability, security, data, or team ownership.
- Strong fit for migrations, major dependencies, and irreversible or expensive choices.
- Interview keywords: ADR, rationale, constraints, evidence, revisit trigger.

### 8. When Not to Use It

- Avoid heavy ADRs for obvious small implementation choices.
- Avoid writing decisions after the fact with no real rationale.
- Use lightweight notes for reversible local decisions.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Creates durable rationale | Takes time to write well |
| Helps future teams revisit decisions | Can become ceremony if every tiny choice is recorded |
| Reduces repeated debates | Bad evidence leads to bad confidence |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: durable alignment and accountability.
- Give up: undocumented speed.
- Latency/cost impact: writing cost upfront, less confusion later.

#### Common Mistakes

- Mistake: decision says what but not why. Better approach: include rationale and constraints.
- Mistake: no rejected alternatives. Better approach: record credible alternatives and why rejected.
- Mistake: no revisit trigger. Better approach: say what would change the decision.

### 11. Key Numbers

- ADR core fields: context, decision, alternatives, consequences, owners, date, status.
- Revisit triggers: traffic growth, compliance change, cost threshold, incident pattern, team ownership change.
- Good decision length: enough to explain reasoning, not a novel.

### 12. Failure Modes

- Team repeats the same debate every quarter.
- New engineer removes an important constraint unknowingly.
- Decision remains after assumptions are false.
- Recovery: ADR cleanup, decision review cadence, explicit status, architecture ownership.

### 13. Scenario

- Product / system: choosing Kafka over direct synchronous calls for booking event propagation.
- Why this concept fits: future teams need to know why async was chosen and what guarantees it provides.
- What would go wrong without it: consumers assume stronger ordering or delivery semantics than intended.

### 14. Code Sample

```python
def adr_status(decision_age_days: int, revisit_after_days: int) -> str:
    # Staff concept: decisions need revisit triggers, not permanent unquestioned status.
    return "review" if decision_age_days >= revisit_after_days else "current"
```

### 15. Mini Program / Simulation

```python
decisions = {"use_kafka_for_booking_events": 420, "primary_region_failover": 45}
print({name: adr_status(age, 365) for name, age in decisions.items()})
```

### 16. Practical Question

> How would you document a major architecture decision so future teams understand it?

### 17. Strong Answer

I would write an ADR or RFC that states the context, decision, constraints, alternatives, trade-offs, accepted risks, and revisit triggers. The key is not just what we chose, but why it fits the current business and technical reality. The trade-off is documentation time, but it prevents repeated debates and helps future teams know when to change course.

### 18. Revision Notes

- One-line summary: justified decisions explain why a design was chosen and when to revisit it.
- Three keywords: rationale, evidence, revisit.
- One interview trap: documenting only the final choice.
- One memory trick: leave future engineers the reason, not just the result.

---

## 7.8.5 Rejected-Alternative Explanation

### 1. Intuition

Rejected-alternative explanation is respectfully saying why you did not choose other credible designs. It shows you understand the space, not just your selected answer.

### 2. Definition

- Definition: rejected-alternative explanation records credible options that were considered and why they were not selected.
- Category: design communication and decision governance.
- Core idea: make non-decisions visible so they do not resurface without new evidence.

### 3. Why It Exists

Teams often revisit the same alternatives because no one documented why they were rejected. Clear rejection reasons save time and improve trust.

### 4. Reality

- Where used: ADRs, design docs, vendor evaluations, migration plans, interview answers.
- Systems/products: architecture decision records, RFC templates, planning docs.
- Teams: engineering, architecture, product, finance, security, SRE.

### 5. How It Works

1. List credible alternatives.
2. Explain why each was rejected using decision criteria.
3. Avoid insulting or strawman wording.
4. Note conditions where rejected option might become viable.
5. Keep explanation concise and evidence-based.

### 6. What Problem It Solves

- Primary problem solved: preventing repeated unresolved debates.
- Secondary benefits: improves reviewer trust, shows diligence, supports future reassessment.
- Systems impact: keeps architecture history understandable.

### 7. When to Rely on It

- Use for major choices with real alternatives.
- Strong fit when stakeholders favored different approaches.
- Interview keywords: rejected because, not chosen, current constraints, revisit if.

### 8. When Not to Use It

- Avoid documenting every trivial non-choice.
- Avoid fake rejected alternatives.
- Avoid language that attacks teams or prior decisions.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Reduces repeated debates | Can become verbose if every minor option is listed |
| Builds trust in recommendation | Needs careful neutral wording |
| Helps future reassessment | Bad criteria can age poorly |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: clarity on why alternatives lost.
- Give up: ultra-short decision docs.
- Latency/cost impact: small writing cost, less future churn.

#### Common Mistakes

- Mistake: strawman rejection. Better approach: describe alternative fairly.
- Mistake: rejected forever. Better approach: specify conditions that could change decision.
- Mistake: personal blame. Better approach: tie rejection to constraints and evidence.

### 11. Key Numbers

- Good count: 1-3 rejected alternatives for most major decisions.
- Useful fields: alternative, reason rejected, revisit trigger.
- Quality marker: someone who preferred the rejected option agrees it is represented fairly.

### 12. Failure Modes

- Rejected option resurfaces every planning cycle.
- Stakeholder feels dismissed.
- Future team cannot tell whether rejection still applies.
- Recovery: rewrite neutrally, add criteria, add revisit trigger, invite stakeholder review.

### 13. Scenario

- Product / system: choosing managed database over self-hosted database for a new service.
- Why this concept fits: self-hosted may be cheaper at scale but managed service speeds delivery and reduces operational burden now.
- What would go wrong without it: the team repeatedly relitigates database ownership.

### 14. Code Sample

```python
def rejected_alternative(name: str, reason: str, revisit_if: str) -> dict[str, str]:
    # Staff concept: rejected alternatives should include revisit conditions.
    return {"alternative": name, "reason": reason, "revisit_if": revisit_if}
```

### 15. Mini Program / Simulation

```python
print(rejected_alternative(
    "self_hosted_database",
    "team lacks 24x7 database operations capacity",
    "traffic or cost exceeds managed-service threshold",
))
```

### 16. Practical Question

> Why should a design doc explain rejected alternatives, and how would you write them?

### 17. Strong Answer

I would include credible rejected alternatives so reviewers know I considered the space and future teams know why those paths were not chosen. I would describe each option fairly, reject it using decision criteria, and add revisit conditions. The trade-off is a little more documentation, but it prevents repeated debates and makes the recommendation more trustworthy.

### 18. Revision Notes

- One-line summary: rejected-alternative explanation records why credible options were not chosen.
- Three keywords: fair, reason, revisit.
- One interview trap: using strawman alternatives.
- One memory trick: close the doors politely and leave labels on them.

---

## 7.8.6 Business Impact Alignment

### 1. Intuition

Business impact alignment connects architecture choices to customer experience, revenue, risk, cost, and delivery goals. It answers, "Why should the business care?"

### 2. Definition

- Definition: business impact alignment ties technical decisions to measurable product, customer, financial, compliance, or operational outcomes.
- Category: staff/principal engineering communication.
- Core idea: architecture is valuable when it improves business outcomes under constraints.

### 3. Why It Exists

Staff engineers influence beyond code. To get alignment, they must explain why technical investments matter to users, the company, and risk posture.

### 4. Reality

- Where used: roadmap planning, architecture reviews, reliability investment proposals, cost optimization, security initiatives.
- Systems/products: OKRs, business cases, SLO reviews, cost reports, incident impact reports.
- Teams: engineering, product, finance, support, security, executives.

### 5. How It Works

1. Identify the business goal or risk.
2. Map technical decision to user/business outcome.
3. Quantify impact where possible.
4. Explain trade-offs in business language.
5. Define success metrics and review cadence.

### 6. What Problem It Solves

- Primary problem solved: preventing architecture from being detached from business value.
- Secondary benefits: better prioritization, leadership alignment, clearer success measurement.
- Systems impact: influences what gets funded, built, delayed, or simplified.

### 7. When to Rely on It

- Use for major platform investments, migrations, reliability work, security controls, and cost changes.
- Strong fit when work competes with product features.
- Interview keywords: customer impact, revenue risk, cost, compliance, operational efficiency, OKR.

### 8. When Not to Use It

- Avoid forcing business framing for tiny code cleanup.
- Avoid vague claims like "better scalability" without connecting to demand or risk.
- Use engineering hygiene framing for local improvements with small scope.

### 9. Pros and Cons

| Pros | Cons |
|---|---|
| Improves prioritization | Requires business context |
| Helps secure investment | Impact may be hard to quantify |
| Aligns teams around outcomes | Over-quantification can create false precision |

### 10. Trade-offs and Common Mistakes

#### Trade-offs

- Gain: clearer prioritization and executive alignment.
- Give up: purely technical justification.
- Latency/cost impact: more discovery work, better funding and sequencing decisions.

#### Common Mistakes

- Mistake: describe architecture only in component terms. Better approach: connect to customer/business impact.
- Mistake: no success metric. Better approach: define measurable outcome.
- Mistake: ignore cost of delay. Better approach: explain risk of not doing the work.

### 11. Key Numbers

- Metrics: conversion, latency, availability, incident minutes, support tickets, infrastructure cost, compliance findings.
- Useful formula: impact = affected users x severity x duration or frequency.
- Review: success metrics should be checked after rollout.

### 12. Failure Modes

- Platform work cannot get prioritized because value is unclear.
- Team overbuilds reliability beyond product need.
- Cost optimization hurts customer experience.
- Recovery: connect work to OKRs, quantify customer/risk impact, define guardrail metrics.

### 13. Scenario

- Product / system: investing in checkout reliability for peak travel season.
- Why this concept fits: checkout downtime directly affects booking revenue and customer trust.
- What would go wrong without it: reliability work is seen as optional infrastructure polish.

### 14. Code Sample

```python
def incident_impact(affected_users: int, minutes: int, severity_score: int) -> int:
    # Staff concept: technical incidents should be translated into business/user impact.
    return affected_users * minutes * severity_score
```

### 15. Mini Program / Simulation

```python
checkout_incident = incident_impact(affected_users=12000, minutes=18, severity_score=5)
search_incident = incident_impact(affected_users=50000, minutes=5, severity_score=2)
print({"checkout": checkout_incident, "search": search_incident})
```

### 16. Practical Question

> How would you justify a reliability investment to product and leadership?

### 17. Strong Answer

I would connect the technical investment to business impact: affected users, revenue risk, support load, brand trust, compliance, or operational cost. For checkout reliability, I would show incident frequency, conversion impact, peak-season risk, and expected reduction in downtime. The trade-off is engineering capacity not spent on features, so I would define success metrics and guardrails to ensure the investment is proportional.

### 18. Revision Notes

- One-line summary: business impact alignment translates technical choices into customer and company outcomes.
- Three keywords: outcome, metric, risk.
- One interview trap: saying "scalability" without explaining why it matters.
- One memory trick: architecture earns trust when it changes a business metric.

---

## Final Comparison Sheet

| Area | Staff-level purpose | Main trade-off |
|---|---|---|
| API gateways | centralize edge policy | control vs gateway bottleneck |
| Global load balancing | route across regions safely | latency vs correctness/policy |
| Request shaping | protect capacity by priority/cost | fairness vs rejection complexity |
| Throttling and rate limiting | enforce fairness and protect dependencies | availability vs strictness |
| DR and recovery | survive regional/system failure | cost vs recovery target |
| Storage economics | match data value to storage cost | cost vs retrieval latency |
| Deployment strategy | reduce release and migration blast radius | speed vs temporary complexity |
| Rate limiting algorithms | shape traffic with algorithm-specific behavior | precision vs cost/latency |
| Security and compliance | reduce breach, privacy, and audit risk | control vs operational complexity |
| Scaling migrations | evolve systems without big-bang risk | safety vs temporary dual paths |
| Staff communication | make decisions understandable and durable | speed vs alignment |

---

## Final Interview Playbook

Use this answer shape for Staff/Principal system design topics:

```text
I will first state the assumptions: <traffic / data / latency / compliance / ownership>.
The staff-level problem is <blast radius / migration risk / compliance / recovery / cost / alignment>.
I see <2-3 credible options>: <option A>, <option B>, <option C>.
Given <constraint>, I would choose <decision> because <technical and business reason>.
The trade-off is <what gets better> versus <what gets worse>.
I would mitigate the risk with <rate limit / audit / fallback / reconciliation / rollout / validation>.
I would prove it with <metric, drill, contract test, parity check, or audit evidence>.
I would reject <alternative> for now because <reason>, and revisit if <trigger> changes.
```

---

## Fast Recall Rules

- Staff-level design starts with assumptions and constraints.
- Gateways centralize policy, but business logic belongs behind them.
- Global load balancing must consider health, capacity, latency, residency, and failover.
- Request shaping and throttling protect shared systems by priority and cost.
- Active-active needs conflict strategy; active-passive needs promotion discipline.
- RPO is data loss; RTO is downtime.
- Data residency includes logs, backups, analytics, and failover.
- Storage tiers trade retrieval speed for cost.
- Lifecycle policies need ownership, dry run, and legal-hold awareness.
- IaC makes infrastructure reviewable and reproducible.
- Feature flags separate deploy from release and need cleanup.
- Dark launches and shadow traffic test new paths without visible user impact.
- Token bucket allows bursts while enforcing average rate.
- Leaky bucket smooths output with queueing latency.
- Fixed window is simple but has boundary bursts.
- Sliding log is precise but memory-heavy.
- Sliding counter is approximate but scalable.
- Distributed rate limiting is about shared quota scope, drift, and fallback.
- PII segregation reduces privacy blast radius across services, logs, events, and analytics.
- Key rotation needs key versions, rewrap strategy, and restore proof.
- Audit logs are structured evidence, not debug logs.
- RBAC is simple roles; ABAC is contextual policy.
- Least privilege applies to humans, services, pipelines, and break-glass.
- Zero trust verifies identity and policy per request instead of trusting the network.
- Dual writes need idempotency, outbox/CDC, and reconciliation.
- Read/write splitting needs lag-aware routing and read-after-write handling.
- Strangler Fig replaces legacy capability by capability.
- Microservices are justified by ownership, domain boundaries, and operational maturity.
- Contract tests protect service boundaries from breaking changes.
- Staff communication compares options, names trade-offs, justifies decisions, and explains rejected alternatives.
- Business impact alignment connects architecture to customers, revenue, risk, cost, and delivery.

