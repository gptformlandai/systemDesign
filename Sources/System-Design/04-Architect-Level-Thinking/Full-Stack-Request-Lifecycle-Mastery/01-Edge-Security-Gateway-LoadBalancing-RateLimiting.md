# 01 - Edge Security Gateway Load Balancing Rate Limiting

> Goal: understand every decision before a request reaches business code: DNS, CDN, WAF, TLS, load balancing, gateway policy, authentication, authorization, rate limiting, and overload protection.

---

## 1. Intuition

The edge and gateway are the airport security and traffic control system for your backend.

They decide:

- who is allowed in
- which region receives traffic
- which requests are suspicious
- which requests are too expensive
- which backend owns the route
- which traffic should be rejected before it hurts databases

Beginner line:

```text
The edge protects and routes traffic before it reaches application services. It should reject bad
requests early, cache safe content, spread load, and preserve enough context for observability.
```

---

## 2. Layered Edge Architecture

```text
Client
  -> DNS / global traffic manager
  -> CDN / edge cache
  -> DDoS protection
  -> WAF / bot defense
  -> TLS termination
  -> global load balancer
  -> regional load balancer / ingress
  -> API gateway
  -> service mesh sidecar / proxy
  -> backend service
```

Not every system needs every box as a separate product, but every serious system needs these responsibilities somewhere.

---

## 3. DNS And Global Traffic Management

DNS choices:

| Choice | Use | Risk |
|---|---|---|
| simple DNS | small app, one region | no intelligent failover |
| weighted DNS | gradual migration/canary | DNS cache delays |
| geo DNS | route by geography | not always best latency |
| latency-based routing | route by measured performance | needs health and measurement |
| anycast | route to nearest network edge | operational complexity |

Key values:

| Setting | Meaning |
|---|---|
| TTL | how long clients/cache resolvers keep answer |
| health check | whether endpoint is eligible |
| failover policy | where traffic goes when region fails |

Wrong option:

```text
Set a very long DNS TTL for a multi-region app that needs fast failover.
```

What fails:

```text
Clients continue using dead regional endpoints after failure because resolvers cache the old answer.
```

Better:

```text
Use controlled TTLs plus global load balancing or anycast/CDN routing for fast failover.
```

---

## 4. CDN And Edge Cache

Good CDN targets:

- static JS/CSS/images/fonts
- product images
- public catalog pages
- anonymous home pages
- documentation
- public API responses with clear TTL

Bad CDN targets:

- account balances
- payment results
- private order details
- personalized checkout state
- admin responses
- anything with PII unless explicitly designed

Cache policy examples:

```http
Cache-Control: public, max-age=31536000, immutable
```

Use for hashed static assets.

```http
Cache-Control: public, max-age=60, stale-while-revalidate=300
```

Use for public catalog pages that can tolerate short staleness.

```http
Cache-Control: no-store
```

Use for sensitive finance/account/payment responses.

Wrong option:

```text
Cache product inventory count at CDN for 30 minutes during flash sale.
```

What fails:

```text
Users see stale availability, add unavailable items, overload checkout, and trust drops.
```

Better:

```text
Cache product detail but either separate inventory into short-TTL/dynamic field or show approximate
availability with checkout-time reservation.
```

---

## 5. WAF, Bot Defense, And Abuse Controls

WAF blocks:

- SQL injection signatures
- XSS payloads
- path traversal
- suspicious headers
- known bad IPs
- oversized payloads
- protocol anomalies

Bot defense detects:

- credential stuffing
- scraping
- fake account creation
- card testing
- checkout abuse
- inventory hoarding

Signals:

| Signal | Example |
|---|---|
| IP reputation | known proxy/bot networks |
| request velocity | too many login attempts |
| device fingerprint | suspicious automation |
| behavior | impossible navigation speed |
| challenge result | CAPTCHA/proof-of-work |
| account risk | many failed payments |

Wrong option:

```text
Use CAPTCHA on every request to stop bots.
```

What fails:

```text
Real users suffer, conversion drops, accessibility worsens, and advanced bots still bypass it.
```

Better:

```text
Use risk-based challenges only for suspicious flows and combine WAF, rate limits, device signals,
and account-level controls.
```

---

## 6. TLS, mTLS, And Security Headers

External TLS:

- encrypts client-to-edge traffic
- proves server identity
- protects cookies/tokens in transit

Internal mTLS:

- authenticates service identity
- encrypts service-to-service traffic
- enables zero-trust networking

Security headers:

| Header | Purpose |
|---|---|
| `Strict-Transport-Security` | force HTTPS |
| `Content-Security-Policy` | reduce XSS blast radius |
| `X-Content-Type-Options: nosniff` | prevent MIME sniffing |
| `Frame-Options` or CSP frame policy | reduce clickjacking |
| `Referrer-Policy` | reduce sensitive URL leakage |
| `Permissions-Policy` | limit browser capabilities |
| `Set-Cookie: HttpOnly; Secure; SameSite` | safer browser session cookie |

Wrong option:

```text
Put JWT access tokens in localStorage and rely on HTTPS only.
```

What fails:

```text
HTTPS protects transport, not JavaScript runtime. XSS can read localStorage and steal the token.
```

Better:

```text
For browser sessions, prefer secure HttpOnly SameSite cookies or a BFF pattern where the browser
does not directly hold long-lived sensitive tokens.
```

---

## 7. Load Balancing

Load balancer responsibilities:

- health checks
- traffic distribution
- TLS termination or passthrough
- connection draining
- autoscaling integration
- zone/region failover
- request/connection metrics

Algorithms:

| Algorithm | Strength | Weakness |
|---|---|---|
| round robin | simple | ignores uneven work |
| least connections | good for long requests | can be fooled by request cost |
| weighted | migration/canary | manual weight tuning |
| latency-based | user performance | needs accurate health data |
| consistent hashing | cache/session affinity | uneven hot keys |
| random two choices | good large-scale balance | less predictable |

Layer choices:

| Layer | Example |
|---|---|
| L4 | TCP/UDP, fast, less request awareness |
| L7 | HTTP-aware routing, headers, paths, auth |

Wrong option:

```text
Use one load balancer and one region for a global high-traffic commerce site.
```

What fails:

```text
Regional outage becomes global outage, latency is poor far from the region, and traffic spikes have
one choke point.
```

Better:

```text
Use CDN/global load balancing, multi-AZ regional load balancers, and regional isolation for critical paths.
```

---

## 8. API Gateway

Gateway can own:

- route matching
- API versioning
- authentication enforcement
- coarse authorization
- request schema validation
- quota/rate limit
- API key validation
- request/response transformation
- CORS
- access logging
- trace propagation
- canary routing

Gateway should not own:

- deep domain rules
- long-running checkout/payment workflow
- database transactions
- business state machines
- cross-service compensation logic

Right split:

```text
Gateway: "Can this caller invoke POST /checkout?"
Checkout service: "Is this cart valid, price quote current, inventory reservable, and payment safe?"
```

Wrong option:

```text
Gateway calls inventory, payment, and order databases directly for checkout.
```

What fails:

```text
The gateway becomes a distributed monolith with domain logic, weak testability, bad ownership, and
risky deployments.
```

Better:

```text
Gateway forwards to a domain service or workflow orchestrator that owns checkout semantics.
```

---

## 9. Authentication And Authorization

Authentication answers:

```text
Who are you?
```

Authorization answers:

```text
What are you allowed to do?
```

Common auth mechanisms:

| Mechanism | Fit |
|---|---|
| session cookie | browser apps |
| OAuth/OIDC | user identity federation |
| API key | partner/server API identity, not enough alone for high security |
| JWT | stateless claims, short-lived access tokens |
| opaque token | server-side introspection/revocation |
| mTLS | service/partner identity |
| signed request | webhooks and partner APIs |

Authorization models:

| Model | Fit |
|---|---|
| RBAC | role-based enterprise permissions |
| ABAC | attribute/policy-driven access |
| ReBAC | relationship-based access, docs/org graphs |
| ACL | per-resource grants |
| policy engine | centralized rules with local enforcement |

Wrong option:

```text
Trust the userId sent in the request body.
```

What fails:

```text
Attackers can act as other users by changing the body.
```

Better:

```text
Derive actor identity from verified session/token context and authorize resource access server-side.
```

---

## 10. Rate Limiting And Quotas

Common limit dimensions:

| Dimension | Example |
|---|---|
| IP | anonymous traffic |
| user ID | authenticated fairness |
| API key/client ID | partner quota |
| route | expensive endpoints |
| tenant | SaaS isolation |
| payment method/card fingerprint | card testing defense |
| SKU/product | flash-sale hotspot |
| region | regional protection |

Rate-limit storage:

| Store | Fit |
|---|---|
| local memory | fastest, approximate per instance |
| Redis | shared counters/token buckets |
| gateway-native | easy API policy |
| streaming detection | fraud/abuse analytics |
| database | usually too slow for per-request hot limits |

Token bucket sketch:

```text
capacity = max burst
refill_rate = allowed rate per second
if tokens > 0: allow and decrement
else: reject with 429 or queue if safe
```

Wrong option:

```text
Queue all excess checkout requests during overload.
```

What fails:

```text
Queues grow, latency explodes, users wait for stale inventory, and downstream systems get hit later
by a retry/queue storm.
```

Better:

```text
Use admission control: reject early with 429/503, expose retry-after, use waiting room only for
carefully designed sale events, and protect correctness-critical systems.
```

---

## 11. Timeouts, Retries, Circuit Breakers, And Bulkheads

Timeouts:

- every network call needs a timeout
- timeout should be lower than caller's remaining budget
- avoid infinite waits

Retries:

- only retry safe/idempotent operations
- use exponential backoff with jitter
- cap attempts
- do not retry permanent validation errors

Circuit breakers:

- stop calling a failing dependency temporarily
- fail fast or use fallback
- protect dependency recovery

Bulkheads:

- isolate resource pools by dependency or traffic type
- prevent one slow dependency from exhausting all workers

Wrong option:

```text
Retry every failed payment charge three times automatically.
```

What fails:

```text
You may double charge or create ambiguous provider state, especially after timeouts.
```

Better:

```text
Use idempotency keys with payment provider, treat timeout as unknown, query/reconcile status before
new attempts, and expose pending state when needed.
```

---

## 12. E-Commerce Availability Edge Choices

Chosen:

| Layer | Choice | Why |
|---|---|---|
| CDN | aggressive static/image caching | reduce origin traffic |
| Catalog pages | short TTL/stale-while-revalidate | availability and speed |
| Search | regional search clusters | read scale and isolation |
| Gateway | route-level quotas | protect checkout and search |
| Bot defense | stricter on login/checkout | reduce credential stuffing and stock hoarding |
| LB | multi-region active-active for reads | survive regional issues |

Rejected:

| Wrong Choice | What Fails |
|---|---|
| strict origin read for every product page | origin/database becomes bottleneck |
| cache personalized checkout at CDN | privacy/correctness failure |
| no bot controls during flash sale | inventory hoarding and provider overload |
| no per-SKU throttling | hot products overload inventory path |

---

## 13. Finance Strict Consistency Edge Choices

Chosen:

| Layer | Choice | Why |
|---|---|---|
| CDN | static assets only | account data cannot be public cached |
| WAF | strict request validation | reduce attack surface |
| Gateway | auth, device/risk context, quotas | protect money APIs |
| TLS/mTLS | encrypted and authenticated | compliance and zero trust |
| Rate limit | per user/account/device | stop abuse without breaking all users |
| LB | prefer healthy primary/region for writes | preserve consistency model |

Rejected:

| Wrong Choice | What Fails |
|---|---|
| cache account balances at CDN | users see wrong/private balances |
| multi-primary writes without conflict-free design | double spend or lost updates |
| retry transfer blindly after timeout | duplicate transfer risk |
| availability over ledger consistency | money correctness breach |

---

## 14. Interview Answer Template

```text
At the edge, I first separate public cacheable traffic from private state-changing traffic. Static
assets and public catalog can be cached aggressively, while checkout, payment, and account APIs go
through auth, WAF, rate limits, and no-store cache policy. Global load balancing chooses a healthy
region, and the API gateway enforces boundary policies but does not own domain workflow. For overload,
I prefer admission control, bounded retries, circuit breakers, and per-route quotas rather than
letting traffic reach the database. For finance, I reduce caching and favor consistency; for
e-commerce discovery, I use edge caching and eventual freshness to preserve availability.
```

---

## 15. Revision Notes

- One-line summary: The edge and gateway protect, route, throttle, and observe requests before business code runs.
- Three keywords: cache, authenticate, throttle.
- One interview trap: putting domain workflow inside the gateway.
- Memory trick: edge handles traffic physics; services handle business truth.

