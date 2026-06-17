# API Gateway - End-to-End System Design

> Goal: design a production-grade API gateway that provides a controlled entry point for clients, routes traffic to backend services, applies shared edge policies, and protects the platform without becoming a business-logic monolith.

---

## How To Use This File

- Use this when the interview problem says API gateway, edge gateway, public API platform, BFF gateway, routing layer, or microservice entry point.
- Keep the gateway's boundary clear: it owns cross-cutting edge concerns, not domain business workflows.
- Focus on routing, auth, TLS, rate limiting, request validation, observability, retries/timeouts, versioning, canary routing, and failure isolation.
- In interviews, explain how the gateway helps and how it can become a bottleneck or blast radius if designed poorly.

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

| Layer | Interview signal | API gateway focus |
|---|---|---|
| Problem understanding | Can define gateway scope | routing, auth, TLS, rate limiting, validation, observability |
| HLD | Can design safe edge architecture | gateway fleet, config plane, service discovery, policy engine, telemetry |
| LLD | Can model request pipeline | `Route`, `Policy`, `Filter`, `Upstream`, `GatewayContext`, `RetryPolicy` |
| Machine coding | Can implement routing/filter chain | match route, run filters, call upstream, handle timeout |
| Traffic spikes | Can protect backends | throttling, load shedding, circuit breakers, canary rollback |
| Billion users | Can reason globally | regional gateways, edge routing, config distribution, multi-tenant isolation |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Accept client HTTP/gRPC requests at a public or internal edge.
- Route requests to correct backend services based on host, path, method, headers, or API version.
- Terminate TLS or pass through TLS depending on deployment model.
- Authenticate clients and propagate identity to backends.
- Authorize coarse-grained access at the edge when possible.
- Apply rate limits, quotas, request size limits, and request validation.
- Support route-level timeouts, retries, circuit breakers, and load balancing.
- Support API versioning, canary routing, blue-green routing, and traffic splitting.
- Emit logs, metrics, and traces for every request.
- Return consistent error responses.

Optional requirements to clarify:

- Is this public internet-facing or internal service-to-service gateway?
- Does it support REST, gRPC, GraphQL, WebSockets, or all?
- Is request/response transformation required?
- Should the gateway aggregate calls to multiple services?
- Is multi-region active-active required?
- Are tenants, API keys, and developer portals in scope?

Out of scope unless asked:

- Full service mesh.
- Full WAF/bot management platform.
- Full identity provider implementation.
- Full API marketplace/developer portal.
- Deep domain orchestration inside the gateway.

## 1.2 Non-Functional Requirements

Performance:

- Low added latency, often p99 under 10 to 20 ms for gateway overhead.
- High throughput and horizontal scalability.
- Efficient connection pooling to upstream services.
- Non-blocking I/O or event-driven runtime for high concurrency.

Availability:

- Gateway must be highly available because it is in front of many services.
- Bad config should not take down the fleet.
- Gateway should degrade gracefully when policy/config services are unavailable.
- It should limit blast radius by route, tenant, region, or cell.

Security and operations:

- Enforce TLS and auth consistently.
- Avoid leaking internal service details in errors.
- Provide strong observability and auditability.
- Support safe config rollout and fast rollback.

## 1.3 Constraints

- Gateway is on the hot path of almost every API request.
- It can become a bottleneck if overloaded with business logic.
- It can become a large blast radius if config or deployment is wrong.
- Policy checks must be fast and cacheable.
- Upstream services have different latency budgets and failure modes.
- Public clients can send malformed, abusive, or expensive requests.

## 1.4 Scale Assumptions

| Metric | Assumption |
|---|---|
| Global traffic | 10M requests/sec peak |
| Gateway nodes | 20,000 globally |
| Backend services | 1,000+ |
| Routes | 100K configured routes |
| Tenants/API clients | 1M+ |
| Added latency budget | p99 under 20 ms |
| Config propagation target | under 60 seconds globally |
| Availability target | 99.99%+ for gateway layer |

Back-of-the-envelope:

- Every gateway hop adds latency, so expensive policy lookups should be cached locally.
- If all gateway nodes reload config simultaneously, config rollout can become an outage.
- Route matching must be efficient because it runs for every request.
- High-cardinality logs must be sampled or controlled to avoid observability overload.

## 1.5 Clarifying Questions To Ask

- Which protocols must the gateway support?
- Should the gateway terminate TLS?
- Should it authenticate JWTs/API keys or call an auth service?
- Is rate limiting local, distributed, or delegated to a limiter service?
- Does the gateway transform payloads or only route requests?
- Is request aggregation allowed or discouraged?
- What is the deployment model: global edge, regional, per-cell, or per-cluster?

Strong interview framing:

> I will design the API gateway as a stateless, horizontally scalable edge fleet with a separate control plane for routes and policies. The data plane handles TLS, auth, routing, validation, rate limiting, timeouts, retries, and telemetry with local cached config, while domain business logic remains in backend services.

---

# 2. High-Level Design

## 2.1 Architecture

Primary request flow:

```text
Client
  -> DNS / Global Load Balancer
  -> CDN / WAF optional
  -> API Gateway Data Plane
  -> Filter Chain: TLS, auth, rate limit, validation, routing, telemetry
  -> Upstream Load Balancer / Service Discovery
  -> Backend Service
  -> Gateway response filters
  -> Client
```

Recommended architecture:

```text
                         +----------------------+
                         | Admin / Config API   |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Gateway Control Plane|
                         +----------+-----------+
                                    |
                    config snapshots / xDS / polling
                                    |
                                    v
+-------------+          +----------------------+
| DNS / GLB   |--------->| Gateway Data Plane   |
+------+------+          +----------+-----------+
       |                            |
       v                            v
+-------------+          +----------------------+
| CDN / WAF   |          | Route / Policy Cache |
+-------------+          +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Service Discovery    |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | Backend Services     |
                         +----------------------+

Gateway -> Metrics / Logs / Traces -> Observability Platform
```

Separate the gateway into two planes:

| Plane | Responsibility | Examples |
|---|---|---|
| data plane | handles live request traffic | Envoy, NGINX, Kong proxy, Spring Cloud Gateway runtime |
| control plane | manages config and policy | route API, policy store, config validation, rollout controller |

## 2.2 APIs

### Configure Route

```http
PUT /v1/gateway/routes/{routeId}
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "host": "api.example.com",
  "pathPrefix": "/v1/orders",
  "methods": ["GET", "POST"],
  "upstreamService": "orders-service",
  "timeoutMs": 800,
  "retryPolicy": {"maxRetries": 1, "retryOn": ["5xx", "connect-failure"]},
  "authPolicy": "jwt-required",
  "rateLimitPolicy": "tenant-orders-default"
}
```

### Gateway Request

```http
GET /v1/orders/ord_123 HTTP/1.1
Host: api.example.com
Authorization: Bearer <jwt>
X-Request-Id: req_123
```

Gateway forwards to upstream with normalized headers:

```http
GET /orders/ord_123 HTTP/1.1
Host: orders-service.internal
X-Request-Id: req_123
X-User-Id: u_42
X-Tenant-Id: t_99
X-Forwarded-For: 203.0.113.10
```

### Gateway Error Response

```http
HTTP/1.1 504 Gateway Timeout
Content-Type: application/json
X-Request-Id: req_123

{
  "error": "UPSTREAM_TIMEOUT",
  "message": "The upstream service did not respond in time."
}
```

## 2.3 Core Components

Think of API Gateway as a configurable request pipeline.

| Stage | What happens | Why it matters |
|---|---|---|
| connection handling | TLS, HTTP parsing, keep-alive | secure and efficient edge |
| request normalization | headers, request ID, body size checks | consistent downstream behavior |
| authentication | JWT/API key/mTLS validation | identify caller |
| authorization | coarse scope/role checks | reject obvious forbidden requests early |
| rate limiting | IP/user/tenant/route limits | protect platform and fairness |
| route matching | host/path/method/version lookup | choose upstream |
| request validation | schema, size, required headers | block malformed traffic |
| upstream call | load balancing, retries, timeouts | reliable backend access |
| response filtering | headers, error normalization | stable client contract |
| telemetry | logs, metrics, traces | operations and debugging |

### Component Responsibility Map

| Component | Owns | Does not own | Scaling concern |
|---|---|---|---|
| DNS / Global LB | route users to region/edge | API policy | global availability |
| CDN / WAF | static cache, bot/WAF rules | service routing internals | attack traffic |
| Gateway Data Plane | request pipeline and forwarding | config authoring | request QPS |
| Gateway Control Plane | route/policy lifecycle | live request handling | config safety |
| Route Matcher | host/path/method/version selection | auth logic | route count |
| Auth Filter | JWT/API key/mTLS validation | identity provider truth | key cache and token volume |
| Rate Limit Filter | quota enforcement | billing truth | limiter QPS |
| Policy Engine | validation and filter config | backend business logic | policy complexity |
| Service Discovery Client | upstream endpoints | service health truth alone | endpoint churn |
| Telemetry Exporter | logs, metrics, traces | blocking requests | event volume |

### What Gateway Should Own

- TLS termination and client connection handling.
- Coarse authentication and authorization.
- Routing and API version selection.
- Request validation and size limits.
- Rate limiting and request shaping.
- Timeout, retry, and circuit breaker policy.
- Load balancing to upstream instances.
- Standard error mapping.
- Observability and correlation IDs.

### What Gateway Should Avoid

- Deep domain business workflows.
- Multi-step transaction orchestration.
- Large response aggregation that creates backend coupling.
- Service-specific business rules that change frequently.
- Heavy synchronous calls to many policy systems.
- Storing durable domain state.

Interview rule:

> The gateway should centralize cross-cutting edge policy, not become the product's business brain.

### Route Matching

Route matching inputs:

- host,
- path prefix or path template,
- HTTP method,
- headers,
- API version,
- tenant/client tier.

Efficient matching options:

| Approach | Fit |
|---|---|
| prefix tree/trie | many path prefixes |
| compiled route table | fast deterministic matching |
| regex routes | flexible but slower; use carefully |
| host map plus path trie | common practical design |

### Policy And Config Distribution

Control plane responsibilities:

- validate route config,
- detect conflicts,
- compile route tables,
- rollout by version,
- canary to a subset of gateways,
- rollback on errors,
- audit admin changes.

Data plane behavior:

- keep last-known-good config,
- reject invalid config snapshots,
- apply config atomically,
- expose current config version in metrics,
- continue serving if control plane is down.

### Reliability Policies

| Policy | Purpose | Caution |
|---|---|---|
| timeout | cap upstream wait time | must fit service latency budget |
| retry | recover transient failures | can amplify load if too aggressive |
| circuit breaker | stop calling unhealthy upstream | needs per-route thresholds |
| bulkhead | isolate pools by route/tenant | more resource management |
| load shedding | reject early under overload | must return clear errors |

### Observability

Every request should have:

- request ID,
- route ID,
- upstream service,
- status code,
- gateway latency,
- upstream latency,
- rate-limit decision,
- auth decision,
- retry count,
- circuit breaker state,
- trace context propagation.

One-stop interview answer:

> I would split the gateway into a stateless data plane and a safe control plane. The data plane runs a configurable filter chain for TLS, auth, rate limits, validation, route matching, upstream load balancing, timeouts, retries, and telemetry. The control plane validates and rolls out route/policy config safely. The gateway keeps last-known-good config and avoids deep business orchestration to reduce latency and blast radius.

---

# 3. Low-Level Design

LLD goal:

> Model the gateway as a filter chain plus route table, with policies and upstream selection kept modular.

Simple rule:

- `Route` maps request shape to upstream.
- `Filter` performs one cross-cutting step.
- `Policy` configures filter behavior.
- `GatewayContext` carries request state.
- `UpstreamClient` calls backend services.

Starter map:

| LLD question | API gateway answer |
|---|---|
| Request state object | `GatewayContext` |
| Route object | `RouteDefinition` |
| Shared behavior | `GatewayFilter` |
| Policy object | `AuthPolicy`, `RateLimitPolicy`, `RetryPolicy`, `TimeoutPolicy` |
| Upstream object | `UpstreamCluster` |
| Routing structure | host map plus path trie |
| Config snapshot | `GatewayConfigVersion` |
| Output | proxied response or gateway error |

Beginner-friendly design order:

1. Model `GatewayRequest` and `GatewayContext`.
2. Model `RouteDefinition` and `UpstreamCluster`.
3. Build route matcher by host, path, and method.
4. Add filter chain: auth, rate limit, validation, telemetry.
5. Add upstream client with timeout, retry, and circuit breaker.
6. Add config snapshot so route/policy changes are versioned.
7. Add last-known-good config fallback.

Interview sentence:

> In LLD, I will model the gateway as a versioned route table plus a filter chain. Each filter owns one concern, such as auth or rate limiting, and the final upstream handler applies timeout/retry policies before returning a normalized response.

## 3.1 Object Modelling

| Entity | Responsibility | Key invariant |
|---|---|---|
| `GatewayRequest` | normalized client request | body size and method are validated |
| `GatewayContext` | request ID, identity, route, decisions | request-scoped only |
| `RouteDefinition` | host/path/method to upstream mapping | route conflicts rejected by control plane |
| `GatewayFilter` | one pipeline concern | should be stateless or safely cached |
| `Policy` | filter configuration | versioned and auditable |
| `UpstreamCluster` | backend service endpoints | health and load balancing apply |
| `GatewayConfigSnapshot` | routes and policies | applied atomically |
| `GatewayResponse` | client response | hides internal error details |

Core services:

| Service | Responsibility |
|---|---|
| `RouteMatcher` | find matching route quickly |
| `FilterChain` | run configured filters in order |
| `AuthFilter` | validate identity and scopes |
| `RateLimitFilter` | enforce quotas |
| `ValidationFilter` | validate headers/body/schema |
| `UpstreamProxy` | call backend with timeout/retry/load balancing |
| `ConfigManager` | apply route/policy snapshots |
| `TelemetryFilter` | emit logs, metrics, traces |

## 3.2 Class Sketch

```java
interface GatewayFilter {
    GatewayResponse apply(GatewayContext context, FilterChain chain);
}

interface RouteMatcher {
    Optional<RouteDefinition> match(GatewayRequest request);
}

interface UpstreamClient {
    GatewayResponse execute(GatewayContext context, RouteDefinition route);
}

final class GatewayConfigSnapshot {
    private final String version;
    private final List<RouteDefinition> routes;
    private final Map<String, Policy> policies;
}
```

## 3.3 Sequence Diagram

```text
Client -> Gateway: HTTP request
Gateway -> ConfigManager: get active snapshot
Gateway -> RouteMatcher: match(host, path, method)
Gateway -> FilterChain: run filters
AuthFilter -> JwtVerifier: verify token
RateLimitFilter -> LimiterClient: check quota
ValidationFilter -> SchemaCache: validate request
UpstreamProxy -> ServiceDiscovery: get endpoints
UpstreamProxy -> BackendService: forward request
TelemetryFilter -> Metrics/Trace: emit
Gateway --> Client: response
```

## 3.4 Design Patterns

| Pattern | Usage |
|---|---|
| Chain of Responsibility | gateway filters |
| Strategy | load balancing, retry, auth, rate-limit strategies |
| Factory | build filters from route policy config |
| Adapter | service discovery, auth provider, limiter, telemetry clients |
| Circuit Breaker | upstream protection |
| Decorator | metrics/tracing around upstream calls |

## 3.5 Edge Cases

| Case | Handling |
|---|---|
| no matching route | return 404/route-not-found |
| auth provider unavailable | use cached keys for JWT; fail closed for protected APIs |
| config snapshot invalid | reject and keep last-known-good config |
| upstream timeout | return 504 or fallback if configured |
| upstream overloaded | circuit breaker and load shedding |
| retry storm | use retry budgets and avoid retrying non-idempotent requests |
| route conflict | control plane validation prevents publish |
| gateway overload | shed low-priority traffic before queues explode |
| huge request body | reject early before upstream call |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
apigateway/
  domain/
    GatewayRequest.java
    GatewayResponse.java
    GatewayContext.java
    RouteDefinition.java
    UpstreamCluster.java
  filter/
    GatewayFilter.java
    AuthFilter.java
    RateLimitFilter.java
    ValidationFilter.java
    TelemetryFilter.java
  routing/
    RouteMatcher.java
    TrieRouteMatcher.java
  proxy/
    UpstreamClient.java
    LoadBalancer.java
    RetryPolicy.java
  config/
    GatewayConfigSnapshot.java
    ConfigManager.java
```

## 4.2 Core Logic Implementation

Python sketch:

```python
from dataclasses import dataclass
from typing import Callable


@dataclass(frozen=True)
class Request:
    method: str
    path: str
    headers: dict[str, str]


@dataclass(frozen=True)
class Route:
    method: str
    path_prefix: str
    upstream: str
    requires_auth: bool


class SimpleGateway:
    def __init__(self, routes: list[Route]) -> None:
        self.routes = routes

    def match_route(self, request: Request) -> Route | None:
        candidates = [
            route for route in self.routes
            if route.method == request.method and request.path.startswith(route.path_prefix)
        ]
        return max(candidates, key=lambda route: len(route.path_prefix), default=None)

    def handle(self, request: Request, upstream_call: Callable[[str, Request], str]) -> tuple[int, str]:
        route = self.match_route(request)
        if route is None:
            return 404, "route not found"
        if route.requires_auth and "Authorization" not in request.headers:
            return 401, "missing authorization"
        if int(request.headers.get("Content-Length", "0")) > 1_000_000:
            return 413, "request too large"
        return 200, upstream_call(route.upstream, request)


gateway = SimpleGateway([
    Route("GET", "/v1/orders", "orders-service", True),
    Route("GET", "/v1/catalog", "catalog-service", False),
])

request = Request("GET", "/v1/orders/123", {"Authorization": "Bearer token"})
print(gateway.handle(request, lambda upstream, req: f"proxied to {upstream}"))
```

## 4.3 Data Structures

| Need | Data structure |
|---|---|
| host lookup | `Map<Host, RouteTree>` |
| path routing | trie or prefix tree |
| exact routes | hash map by method/path |
| policy cache | `Map<PolicyId, Policy>` with version |
| upstream endpoints | weighted list or ring |
| circuit states | `Map<RouteId, CircuitState>` |
| metrics | async event queue |

## 4.4 Concurrency

- Route config snapshots should be immutable once active.
- Config updates should swap atomically.
- Connection pools must be thread-safe or event-loop-owned.
- Circuit breaker state must be concurrency-safe per route/upstream.
- Metrics emission should be async and non-blocking.
- Large request queues should be bounded to avoid memory blowups.

## 4.5 Performance Optimization

- Compile route tables before publishing config.
- Cache JWT public keys and policy decisions where safe.
- Reuse upstream connections.
- Use event-driven I/O.
- Avoid blocking calls in filters.
- Keep request/response transformations small.
- Sample logs for high-QPS routes while preserving error logs.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Public API Traffic Spike

Problem:

- Many clients hit gateway at once, threatening both gateway and backend services.

Handling:

- Apply CDN/WAF coarse filtering.
- Enforce gateway rate limits by tenant/user/route.
- Shed low-priority traffic.
- Reduce max body size or page size if product allows.
- Protect upstreams with circuit breakers and concurrency limits.

## 5.2 Bad Config Rollout

Problem:

- A route or policy change breaks traffic globally.

Handling:

- Validate config in control plane.
- Canary config to small gateway subset.
- Compare route-level error rates.
- Roll back to previous snapshot.
- Gate risky policy changes with approvals.

## 5.3 Upstream Outage

Handling:

- Per-route timeout.
- Circuit breaker opens for failing upstream.
- Retry only idempotent requests with retry budget.
- Return normalized `503` or `504`.
- Avoid retry amplification.

## 5.4 Gateway Overload

Handling:

- Bounded queues.
- Load shedding.
- Autoscale gateway fleet.
- Disable expensive optional filters temporarily.
- Keep health checks lightweight.
- Use cell-based isolation to limit blast radius.

---

# 6. Scaling To A Billion Users

## 6.1 Partitioning And Deployment

| Concern | Strategy |
|---|---|
| global traffic | DNS/global load balancer routes to nearest region |
| regional capacity | horizontally scaled stateless gateway fleet |
| tenant isolation | per-tenant limits and optional dedicated cells |
| config rollout | versioned snapshots and staged deployment |
| route volume | compiled route tables and prefix tries |
| observability | route-level aggregation and sampling |

## 6.2 Multi-Region Strategy

- Deploy gateways in every major region.
- Route users to nearest healthy region.
- Keep data plane stateless and region-local.
- Replicate route/policy config globally.
- Use last-known-good config if control plane is unreachable.
- Keep global rate limits approximate unless endpoint is sensitive.

## 6.3 Control Plane Reliability

- Store route/policy config durably.
- Validate config before publishing.
- Compile config into efficient snapshots.
- Publish snapshots with version numbers.
- Support rollback.
- Data plane continues serving existing config during control-plane outage.

## 6.4 Observability

Track:

- gateway p50/p95/p99 latency,
- upstream latency by route,
- status codes by route and tenant,
- auth failures,
- rate-limit blocks,
- config version by node,
- circuit breaker state,
- retry counts,
- queue depth and load shedding,
- top erroring routes,
- gateway CPU/memory/connection count.

---

# 7. Final Interview Playbook

## 7.1 Recommended Walkthrough

1. Clarify protocol, public/internal scope, and gateway responsibilities.
2. Draw data plane and control plane.
3. Explain request pipeline filters.
4. Explain route matching and service discovery.
5. Add auth, rate limits, validation, timeouts, retries, circuit breakers.
6. Discuss config rollout and last-known-good behavior.
7. Discuss traffic spikes, bad config, upstream failures.
8. Scale globally with regional stateless gateway fleets.

## 7.2 Key Trade-Offs

| Decision | Option A | Option B | Recommendation |
|---|---|---|---|
| gateway logic | thin routing | heavy orchestration | thin plus cross-cutting policies |
| config | dynamic runtime | static deploy | dynamic versioned config with validation |
| auth | call auth service every request | validate cached JWT keys | cached validation for latency |
| retries | aggressive | retry budget | limited retries for idempotent requests |
| deployment | central gateway | regional gateway | regional stateless fleets |

## 7.3 Common Mistakes

- Putting deep business logic in the gateway.
- Making gateway call many services to build one response.
- Forgetting bad config as a major failure mode.
- Retrying non-idempotent requests blindly.
- Using one global gateway cluster for all traffic.
- Blocking every request on control-plane availability.
- Ignoring per-route observability.

## 7.4 Strong Closing

> An API gateway should be a controlled, observable, stateless edge data plane with a safe control plane. It centralizes cross-cutting concerns like routing, auth, rate limits, validation, timeouts, retries, and telemetry while keeping domain logic in backend services and protecting itself from bad config or upstream failures.

---

# 8. Fast Recall Rules

- Gateway = controlled entry point.
- Split data plane and control plane.
- Own cross-cutting concerns, not deep business logic.
- Use filter chain for auth, rate limit, validation, telemetry.
- Use route table/trie for matching.
- Keep last-known-good config.
- Canary and rollback config changes.
- Use timeouts, retry budgets, and circuit breakers.
- Regional stateless fleets scale best.
- Observe by route, tenant, upstream, and config version.
