# Microservices Communication API Contracts FAANG Master Sheet

Target: starter, intermediate, senior, and FAANG-level microservices interviews.

This sheet covers:
- REST vs gRPC vs messaging
- synchronous vs asynchronous communication
- API Gateway and BFF
- API versioning
- backward compatibility
- consumer-driven contracts
- schema evolution
- service discovery and load balancing
- request correlation
- communication anti-patterns

Goal:

```text
After reading this sheet, you should be able to choose the right communication style between
services, design APIs that evolve safely, and explain how contracts prevent independent
deployments from breaking each other.
```

---

## 0. How To Use This Guide By Level

| Level | What To Focus On |
|---|---|
| Starter | REST, API Gateway, sync vs async |
| Intermediate | gRPC, BFF, versioning, service discovery |
| Senior | backward compatibility, contract testing, schema evolution |
| FAANG-ready | dependency management, latency budgets, fan-out control, rollout safety |

Must-say line:

```text
In microservices, every service call is a network call, so communication design must include
latency, failure handling, contract compatibility, security, and observability.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| REST | Very high | Most common API style |
| gRPC | High | Low-latency typed service calls |
| Async messaging | Very high | Loose coupling and event-driven systems |
| API Gateway | Very high | Client entry point |
| BFF | High | Client-specific API shape |
| API versioning | Very high | Safe independent deployments |
| Backward compatibility | Very high | Prevent broken consumers |
| Contract testing | High | Provider/consumer safety |
| Service discovery | High | Dynamic routing |
| Load balancing | High | Scaling traffic |
| Correlation ID | High | Debugging distributed calls |
| Fan-out control | High | Tail latency and blast radius |

---

# 2. Communication Decision Framework

Ask these questions:

1. Does caller need an immediate answer?
2. Is this command, query, or event?
3. Can the work happen later?
4. Is ordering required?
5. Can duplicate processing happen safely?
6. What is the latency budget?
7. How many downstream services are involved?
8. What happens when downstream is unavailable?

Decision table:

| Requirement | Communication Style |
|---|---|
| Immediate read/write response | REST or gRPC |
| Side effect after state change | event/message |
| Fan-out to many services | event |
| Low-latency typed internal call | gRPC |
| External public API | REST/HTTP |
| Client-specific aggregation | BFF/API composition |
| Long-running workflow | async + Saga |

Strong answer:

```text
I use synchronous calls when the caller needs an immediate response and asynchronous events
when work can happen later or many services need to react independently.
```

---

# 3. REST

REST is common for request-response APIs over HTTP.

Good for:
- public APIs
- CRUD-style resources
- browser/mobile clients
- simple service calls
- human-debuggable requests

Example:

```text
GET /api/bookings/B123
POST /api/bookings
PATCH /api/bookings/B123/status
```

Pros:
- simple
- widely supported
- works with HTTP tooling
- cache-friendly for reads

Cons:
- text payload overhead
- weaker schema enforcement unless OpenAPI is used
- easy to create chatty APIs
- versioning must be managed carefully

Strong answer:

```text
REST is my default for public resource-oriented APIs because it is simple, debuggable, and
widely supported. I still define clear contracts, status codes, timeouts, and versioning.
```

---

# 4. gRPC

gRPC is a high-performance RPC framework using Protocol Buffers.

Good for:
- internal service-to-service calls
- low-latency communication
- strongly typed APIs
- streaming
- polyglot backends

Example mental model:

```text
BookingService.GetBooking(bookingId) -> BookingResponse
```

Pros:
- typed schema
- efficient binary format
- supports streaming
- strong code generation

Cons:
- less browser-friendly directly
- debugging requires tooling
- versioning still matters
- not always needed for simple CRUD APIs

Strong answer:

```text
I choose gRPC for internal low-latency typed service calls, especially when teams benefit
from code-generated clients and strict schemas. For public APIs, REST is often simpler.
```

---

# 5. Async Events And Messaging

Async communication means caller does not wait for all downstream work.

Example:

```text
Booking Service publishes BookingConfirmed
Notification Service sends email
Loyalty Service awards points
Analytics Service updates reports
```

Good for:
- fan-out
- loose coupling
- background work
- event-driven workflows
- smoothing traffic spikes

Costs:
- eventual consistency
- duplicates
- ordering issues
- harder debugging
- DLQ/replay strategy needed

Strong answer:

```text
Events are ideal when multiple services need to react to a state change without blocking
the user request. The trade-off is eventual consistency and more operational complexity.
```

---

# 6. API Gateway

API Gateway is the edge entry point.

Responsibilities:
- routing
- authentication integration
- TLS termination, often at edge/load balancer
- rate limiting
- request/response header handling
- CORS
- coarse authorization
- observability

Should not contain:
- core domain logic
- cross-service transactions
- heavy orchestration
- business rules that belong in services

Strong answer:

```text
The gateway should handle edge concerns, not become a domain monolith. Services still own
business authorization and domain rules.
```

---

# 7. BFF

BFF means Backend For Frontend.

Use when different clients need different API shapes:
- mobile
- web
- partner API
- admin portal

Example:

```text
Mobile BFF returns compact booking summary.
Web BFF returns full booking details plus recommendations.
Admin BFF returns audit and support fields.
```

Strong answer:

```text
BFF prevents one generic API from becoming bloated for every client. It lets each frontend
get a tailored API while core services remain domain-focused.
```

---

# 8. API Composition

API composition means combining data from multiple services.

Example:

```text
Booking page needs:
- booking details
- hotel summary
- payment status
- loyalty points
```

Where composition can live:
- gateway
- BFF
- dedicated composition service
- frontend, for simple cases

Risks:
- fan-out latency
- partial failure
- inconsistent snapshots
- high coupling to many services

Senior answer:

```text
For read-heavy composition, I avoid deep synchronous fan-out when possible. If the query is
frequent or latency-sensitive, I consider a materialized read model.
```

---

# 9. Fan-Out And Tail Latency

If one API calls five downstream services:

```text
overall latency is affected by the slowest dependency
```

Problems:
- one slow service hurts user request
- retries multiply traffic
- partial failure complexity
- hard SLO ownership

Controls:
- timeouts per dependency
- parallel calls with deadline
- fallback for optional data
- read models for frequent screens
- cache optional data
- limit number of dependencies per request

Strong answer:

```text
Fan-out increases tail latency and blast radius. I keep critical request paths small and
move optional or frequent aggregated data into caches or read models.
```

---

# 10. API Versioning

Versioning is needed when contracts change.

Approaches:

| Approach | Example | Notes |
|---|---|---|
| URI version | `/v1/bookings` | simple, visible |
| Header version | `Accept: application/vnd.company.v2+json` | cleaner URLs, more hidden |
| Field evolution | add optional fields | best for non-breaking changes |
| New endpoint | `/booking-summaries` | often better than breaking old one |

Rule:

```text
Prefer backward-compatible evolution over frequent version bumps.
```

---

# 11. Backward Compatibility

Backward-compatible changes:
- add optional response field
- add optional request field
- add new endpoint
- add new enum only if consumers tolerate unknowns
- relax validation carefully

Breaking changes:
- remove field
- rename field
- change field meaning
- change type
- make optional field required
- change status code semantics
- remove enum value or add enum to strict consumer

Strong answer:

```text
Independent deployment requires backward compatibility. Providers should not break existing
consumers during rolling deploys.
```

---

# 12. Consumer-Driven Contracts

Consumer-driven contract testing verifies provider and consumer expectations.

Flow:

```text
Consumer defines expected request/response.
Provider verifies it can satisfy that contract.
CI blocks breaking provider changes.
```

Tools:
- Pact
- Spring Cloud Contract

Strong answer:

```text
Contract tests reduce the risk of independent deployments by checking that providers still
satisfy consumer expectations.
```

---

# 13. Schema Evolution For Events

Events also need compatibility.

Rules:
- add optional fields
- keep old fields until consumers migrate
- avoid changing field meaning
- include event type and version
- use schema registry for Avro/Protobuf where useful
- design consumers to ignore unknown fields

Bad:

```text
Rename bookingId to reservationId in BookingConfirmed without compatibility plan.
```

Better:

```text
Add reservationId, keep bookingId, migrate consumers, then deprecate later.
```

---

# 14. Service Discovery

Service discovery solves dynamic service location.

Options:
- Kubernetes Service DNS
- cloud load balancer
- Eureka/Consul
- service mesh discovery

Strong answer:

```text
In Kubernetes, services usually call stable DNS names and Kubernetes routes traffic to
healthy pods. In non-Kubernetes platforms, a registry like Eureka or Consul may be used.
```

---

# 15. Load Balancing

Load balancing distributes traffic across instances.

Common approaches:
- server-side load balancer
- client-side load balancer
- Kubernetes Service
- service mesh proxy

Algorithms:
- round-robin
- least connections
- weighted
- locality-aware
- consistent hashing, for special cases

Interview line:

```text
Load balancing must consider health, latency, locality, and retry behavior, not only round-robin.
```

---

# 16. Correlation IDs

Every request should carry an ID.

Example:

```text
X-Correlation-Id: c-123
```

Flow:

```text
Gateway -> Booking -> Payment -> Inventory
```

Every service logs the same correlation ID.

Strong answer:

```text
Correlation IDs are essential because one user request crosses many services. They connect
logs, traces, and downstream calls.
```

---

# 17. Communication Anti-Patterns

| Anti-pattern | Why Bad | Better |
|---|---|---|
| Chatty service calls | high latency and coupling | coarser API/read model |
| Gateway has business logic | domain leakage | keep logic in services |
| Breaking API changes | independent deploy failure | backward compatibility |
| No timeout | thread exhaustion | strict deadlines |
| Blind retry | retry storm | retry only transient and safe |
| Shared DTO library everywhere | tight coupling | explicit contracts |
| Frontend calls every service | security and coupling | BFF/gateway |
| Sync chain for side effects | slow user request | async events |

---

# 18. Production Scenario: Booking Details Page

Requirement:

```text
Show booking details with hotel info, payment status, loyalty estimate, and cancellation policy.
```

Design:
1. Use BFF or composition service for client-specific response.
2. Booking details are critical and synchronous.
3. Hotel info can be cached.
4. Loyalty estimate can be optional fallback.
5. Payment status has strict timeout.
6. Use correlation ID across all calls.
7. Monitor fan-out latency.
8. For high traffic, build materialized booking view.

Strong answer:

```text
I would avoid putting this composition in every client. A BFF can call the required services,
but I would keep fan-out small, apply timeouts, cache optional data, and consider a read
model if the page is high-traffic or latency-sensitive.
```

---

# 19. Hot Interview Questions

### Q1. REST vs gRPC?

```text
REST is simple and public-API friendly. gRPC is strongly typed and efficient for internal
service-to-service calls.
```

### Q2. Sync vs async?

```text
Use sync when caller needs an immediate answer. Use async for fan-out, side effects, and
loose coupling.
```

### Q3. How do you version APIs?

```text
Prefer backward-compatible field evolution. Use URI/header versioning only for breaking
changes that cannot be avoided.
```

### Q4. What is contract testing?

```text
It verifies provider and consumer API expectations so independent deployments do not break
each other.
```

### Q5. Why is fan-out dangerous?

```text
Overall latency and availability depend on multiple downstream services. Tail latency and
partial failure become harder.
```

---

# 20. Final Rapid Revision

| Need | Choose |
|---|---|
| Public API | REST |
| Low-latency internal typed call | gRPC |
| Side effect/fan-out | async event |
| Client-specific aggregation | BFF |
| Edge routing | API Gateway |
| Frequent aggregate read | materialized view |
| Dynamic service location | service discovery |
| Safe API evolution | backward compatibility |
| Provider/consumer safety | contract testing |
| Debug request path | correlation ID + trace |

---

# 21. Strong Closing Answer

If interviewer asks:

```text
How should microservices communicate?
```

Say:

```text
I choose communication based on the business need. REST is a good default for public and
resource-oriented APIs, gRPC is useful for low-latency typed internal calls, and async events
are best for fan-out and side effects. I keep APIs backward-compatible, use contract tests
for critical consumers, control fan-out with timeouts and read models, and propagate
correlation IDs for debugging.
```

---

# 22. Official Source Notes

Useful references:

- Kubernetes Services: https://kubernetes.io/docs/concepts/services-networking/service/
- gRPC Documentation: https://grpc.io/docs/
- OpenAPI Specification: https://spec.openapis.org/oas/latest.html
- Pact Contract Testing: https://docs.pact.io/
- Spring Cloud Contract: https://docs.spring.io/spring-cloud-contract/reference/

