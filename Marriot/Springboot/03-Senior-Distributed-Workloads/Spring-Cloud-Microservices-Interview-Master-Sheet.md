# Spring Cloud Microservices Interview Master Sheet

Target: Java Backend / Spring Boot / distributed systems interviews.

This sheet covers:
- Spring Cloud big picture
- centralized configuration
- Spring Cloud Config
- API Gateway
- Spring Cloud Gateway
- OpenFeign
- service discovery
- client-side load balancing
- microservice communication patterns
- secrets/config safety
- production trade-offs

Goal:

```text
After reading this sheet, you should be able to explain how Spring Boot services are wired
in a microservice environment using Config Server, Gateway, OpenFeign, discovery, load
balancing, and production-safe configuration patterns.
```

---

## 0. How To Use This Guide By Level

| Level | Focus |
|---|---|
| Beginner | why microservices need config, gateway, clients, discovery |
| Intermediate | Config Server, Gateway routes, Feign clients, load balancing |
| Senior | failure modes, secrets, config refresh, gateway filters, discovery health |
| MAANG-ready | trade-offs, operational complexity, rollout safety, platform alternatives |

Strong line:

```text
Spring Cloud helps solve distributed-system plumbing, but it does not remove the need to
design for failure, security, observability, and operational simplicity.
```

---

## 1. Interview Priority Meter

| Topic | Priority | Why Interviewers Ask |
|---|---:|---|
| Spring Cloud purpose | High | Microservice awareness |
| Config Server | High | Centralized config |
| Config refresh | Medium-high | Runtime config changes |
| Secrets management | Very high | Production security |
| API Gateway | Very high | Entry point pattern |
| Gateway routing | High | Request flow |
| Gateway filters | High | Cross-cutting concerns |
| OpenFeign | High | Declarative clients |
| Service discovery | High | Dynamic service locations |
| Client-side load balancing | High | Instance selection |
| Circuit breaker at gateway/client | High | Resilience |
| Distributed tracing | High | Debugging |
| Microservice trade-offs | Very high | Architecture maturity |

---

# 2. Spring Cloud Big Picture

Spring Boot builds one service well.

Spring Cloud helps multiple services work together.

Common needs:
- centralized config
- service discovery
- API gateway
- load balancing
- declarative clients
- circuit breakers
- distributed tracing
- cloud-native integration

Strong answer:

```text
Spring Cloud provides patterns and integrations for distributed systems, such as config
server, gateway, service discovery, load balancing, declarative clients, and circuit breakers.
```

---

# 3. Microservice Request Flow

```text
Client
  |
  v
API Gateway
  |
  v
booking-service
  |
  +--> payment-service
  +--> inventory-service
  +--> notification-service
```

Supporting infrastructure:

```text
Config Server -> services load configuration
Service Registry -> services register/discover
Observability -> metrics/logs/traces
Broker -> async events
```

---

# 4. Spring Cloud Config

Spring Cloud Config centralizes external configuration.

Mental model:

```text
Config repository
      |
      v
Config Server
      |
      v
Spring Boot services
```

Config can come from:
- Git
- filesystem
- Vault
- JDBC
- cloud secret/config stores

Strong answer:

```text
Spring Cloud Config gives services a central place to load environment-specific properties,
often backed by Git or a secrets/config backend.
```

---

# 5. Config Server

Example server application:

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

Example config:

```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/config-repo
```

---

# 6. Config Client

Example:

```yaml
spring:
  application:
    name: booking-service
  config:
    import: optional:configserver:http://config-server:8888
```

Config repository example:

```text
booking-service-dev.yml
booking-service-prod.yml
application.yml
```

Interview line:

```text
Service name and active profile decide which config files are loaded.
```

---

# 7. Config Refresh

Runtime config refresh can update some properties without full restart.

Tools:
- Actuator refresh endpoint, depending setup
- Spring Cloud Bus
- restart rollout
- platform-level config reload

Senior caution:

```text
Not every config should change dynamically. Thread pools, datasource settings, security
settings, and complex beans may need restart or careful refresh design.
```

Strong answer:

```text
I prefer immutable config plus controlled rollout for critical settings. Dynamic refresh is
useful, but it must be tested because not every bean reacts safely.
```

---

# 8. Secrets Management

Do not store secrets in plain Git.

Secrets include:
- database passwords
- API keys
- OAuth client secrets
- signing keys
- encryption keys

Better options:
- Vault
- AWS Secrets Manager
- Azure Key Vault
- GCP Secret Manager
- Kubernetes Secrets with external secret operator
- encrypted config values with key rotation strategy

Strong answer:

```text
Configuration and secrets are different. Config can live in Git, but secrets should come
from a secrets manager with access control, rotation, and audit.
```

---

# 9. API Gateway

Gateway is the entry point for clients.

Responsibilities:
- routing
- authentication/token relay
- TLS termination, often at edge/load balancer too
- rate limiting
- request/response header handling
- path rewriting
- CORS
- centralized cross-cutting filters
- observability

Should not contain:
- core business logic
- heavy orchestration
- domain decisions that belong in services

Strong answer:

```text
An API Gateway centralizes edge concerns like routing, auth integration, rate limiting,
CORS, and request filters, while business logic should remain in domain services.
```

---

# 10. Spring Cloud Gateway

Gateway route has:
- predicate: when route matches
- filter: how request/response is modified
- URI: where to send request

Example:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: booking-service
          uri: lb://booking-service
          predicates:
            - Path=/api/bookings/**
          filters:
            - StripPrefix=1
```

Mental model:

```text
Request -> route predicate -> gateway filters -> downstream service
```

---

# 11. Gateway Filters

Common filters:
- add/remove request header
- rewrite path
- strip prefix
- retry
- circuit breaker
- rate limiter
- token relay
- secure headers
- request size limit
- response header cleanup

Example use:

```text
/api/bookings/** -> strip /api -> booking-service
```

Strong answer:

```text
Gateway filters are good for cross-cutting HTTP concerns, but I avoid putting domain logic
in the gateway.
```

---

# 12. Gateway Rate Limiting

Use gateway rate limiting to protect services.

Examples:
- per IP
- per API key
- per user
- per route
- per tenant

Caution:

```text
Rate limiting key must match business/security need. IP-only can be unfair behind NAT.
```

Strong answer:

```text
Gateway rate limiting protects downstream services before traffic reaches them, but the
keying strategy must be chosen carefully.
```

---

# 13. Gateway Security

Gateway can:
- validate token at edge
- relay token downstream
- enforce coarse route-level rules
- add security headers
- handle CORS

Services should still:
- validate authorization for sensitive APIs
- enforce ownership checks
- avoid trusting only the gateway

Strong answer:

```text
Gateway security is useful, but internal services should still enforce authorization because
gateway bypass or internal calls are possible.
```

---

# 14. OpenFeign

OpenFeign is declarative HTTP client.

Example:

```java
@FeignClient(name = "inventory-service")
interface InventoryClient {
    @GetMapping("/api/inventory/{hotelId}")
    InventoryResponse getInventory(@PathVariable Long hotelId);
}
```

Service use:

```java
@Service
class BookingService {
    private final InventoryClient inventoryClient;

    BookingService(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }
}
```

Strong answer:

```text
Feign lets me define HTTP clients as Java interfaces. It reduces boilerplate, especially
with service discovery and load balancing.
```

---

# 15. Feign Trade-Offs

Pros:
- simple declarative API
- less boilerplate
- integrates with Spring MVC annotations
- works with discovery/load balancing

Cons:
- hides network call behind interface
- easy to forget timeouts/retries
- can encourage chatty service calls
- error mapping must be configured

Senior answer:

```text
Feign is convenient, but I still treat every method call as a network call with timeout,
retry, metrics, and failure handling.
```

---

# 16. Service Discovery

Problem:

```text
booking-service needs payment-service location, but instances change dynamically.
```

Service discovery solution:

```text
service registers itself
client asks registry for healthy instances
load balancer chooses one
```

Implementations:
- Eureka
- Consul
- Zookeeper
- Kubernetes service discovery
- cloud platform service discovery

Strong answer:

```text
Service discovery lets clients find service instances dynamically instead of hardcoding
hostnames and ports.
```

---

# 17. DiscoveryClient And LoadBalancer

Spring Cloud Commons provides common abstractions:
- `DiscoveryClient`
- `ReactiveDiscoveryClient`
- `ServiceRegistry`
- Spring Cloud LoadBalancer

Modern note:

```text
With a discovery implementation on the classpath, explicit @EnableDiscoveryClient is often
not required.
```

Load-balanced call:

```text
http://inventory-service/api/inventory
```

Here `inventory-service` is a service ID, not a physical host.

---

# 18. Client-Side vs Server-Side Load Balancing

| Client-Side | Server-Side |
|---|---|
| client chooses instance | load balancer chooses instance |
| needs discovery client | simpler client |
| service-aware | centralized |
| common with Spring Cloud LoadBalancer | common with cloud load balancer/Kubernetes service |

Interview line:

```text
In Kubernetes, service discovery and load balancing may already be provided by Services,
so adding a separate registry may be unnecessary.
```

---

# 19. Config And Discovery In Kubernetes

In Kubernetes:
- ConfigMaps and Secrets can provide config
- Services provide stable DNS names
- Ingress/Gateway handles edge traffic
- readiness/liveness controls routing and restarts

Spring Cloud may still be useful for:
- Config Server with Git-based config
- Gateway features
- OpenFeign
- CircuitBreaker
- centralized cross-cloud patterns

Senior answer:

```text
I do not add Spring Cloud components automatically. I compare them with platform-native
capabilities like Kubernetes Services, ConfigMaps, Secrets, and Ingress.
```

---

# 20. Microservice Communication Patterns

| Pattern | Use |
|---|---|
| Synchronous HTTP | immediate query/command |
| Async messaging | event reaction/workflow |
| Request aggregation | API composition |
| Saga | distributed business transaction |
| Outbox | reliable event publishing |
| CQRS/read model | query optimization |

Rule:

```text
Use sync calls when caller needs immediate answer. Use events when work can be asynchronous.
```

---

# 21. Distributed Transaction Trap

Common mistake:

```text
Use one transaction across all microservices.
```

Better:

```text
Use local transactions plus saga/outbox/compensation where needed.
```

Example:

```text
booking created
inventory reserved
payment authorized
if payment fails, release inventory
```

Strong answer:

```text
In microservices, I avoid distributed transactions unless absolutely required. I prefer
local transactions, events, idempotency, and compensating actions.
```

---

# 22. Versioning And Compatibility

Microservices deploy independently.

Rules:
- keep APIs backward-compatible
- add fields before removing old ones
- support old and new clients during rollout
- version breaking APIs
- use consumer-driven contracts for critical APIs

Strong answer:

```text
Independent deployment requires backward-compatible contracts. A service should not break
existing clients during rolling deploys.
```

---

# 23. Production Scenario: Booking Platform

Requirement:

```text
Build Spring Cloud architecture for booking-service, inventory-service, payment-service,
and notification-service.
```

Design:
1. Gateway routes external traffic to booking APIs.
2. Gateway handles CORS, coarse auth, rate limiting, and token relay.
3. Services enforce method-level authorization and ownership.
4. Config comes from Config Server or platform-native config.
5. Secrets come from secrets manager, not plain Git.
6. Booking service calls inventory/payment using Feign or RestClient with timeouts.
7. Discovery/load balancing is provided by Kubernetes Services or Spring Cloud discovery.
8. BookingCreated event is sent through outbox to Kafka for notification/analytics.
9. Observability includes metrics, traces, logs, and correlation IDs.
10. Failures use retry/circuit breaker only where safe.

Strong interview answer:

```text
I would keep the gateway focused on edge concerns like routing, CORS, token relay, and
rate limiting. Each service still validates authorization. Config would be centralized,
but secrets would come from a secrets manager. For service calls, I would use Feign or
RestClient with timeouts, metrics, and resilience. Discovery/load balancing can come from
Spring Cloud or Kubernetes depending on platform. Async side effects should use events and
outbox rather than distributed transactions.
```

---

# 24. Hot Interview Questions

### Q1. What is Spring Cloud?

```text
A set of projects that help build distributed systems with patterns like config, gateway,
discovery, load balancing, circuit breakers, and declarative clients.
```

### Q2. What is Spring Cloud Config?

```text
A centralized config system where services load environment-specific properties from a
config server backed by Git or another repository.
```

### Q3. What is an API Gateway?

```text
An edge service that routes requests and handles cross-cutting concerns like auth integration,
rate limiting, CORS, headers, and path rewriting.
```

### Q4. What is OpenFeign?

```text
A declarative HTTP client where Java interfaces represent remote service APIs.
```

### Q5. What is service discovery?

```text
A mechanism for services to register and discover dynamic service instances.
```

### Q6. Is Eureka always needed in Kubernetes?

```text
No. Kubernetes Services already provide discovery and load balancing for many use cases.
```

### Q7. Should gateway contain business logic?

```text
No. Gateway should handle edge concerns. Business rules belong in domain services.
```

---

# 25. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Put business logic in gateway | gateway becomes domain monolith | keep domain logic in services |
| Store secrets in config Git repo | credential leak | secrets manager |
| Dynamic refresh everything | unsafe runtime behavior | immutable config or tested refresh |
| No service-level auth | gateway bypass risk | defense in depth |
| Feign without timeouts | hung callers | configure timeout/resilience |
| Too many sync calls | latency and coupling | async/events/read models |
| Add Eureka on Kubernetes blindly | duplicate platform feature | use platform service discovery |
| No contract compatibility | rolling deploy breakage | backward-compatible APIs |
| Config change without audit | hard incident analysis | versioned config and approvals |

---

# 26. One-Hour Revision Plan

### First 15 Minutes: Spring Cloud Basics

Revise:
- why Spring Cloud exists
- config
- gateway
- discovery
- load balancing

Must say:

```text
Spring Cloud solves distributed-system plumbing, not business architecture by itself.
```

### Next 15 Minutes: Config

Revise:
- Config Server
- config client
- profiles
- secrets
- refresh

Must say:

```text
Secrets should not live as plain text in a config repository.
```

### Next 15 Minutes: Gateway And Clients

Revise:
- routes
- predicates
- filters
- rate limiting
- OpenFeign
- timeouts

Must say:

```text
Feign methods are network calls, even if they look like local Java calls.
```

### Final 15 Minutes: Discovery And Architecture

Revise:
- discovery client
- load balancing
- Kubernetes alternative
- sync vs async
- distributed transaction trap

Must say:

```text
In microservices, I prefer local transactions, events, idempotency, and compensation over
global distributed transactions.
```

---

# 27. Final Rapid Revision Sheet

| Need | Spring Cloud Concept |
|---|---|
| Centralized config | Config Server |
| Load remote config | Config Client |
| Edge routing | Spring Cloud Gateway |
| Route match condition | Predicate |
| Modify request/response | Gateway filter |
| Declarative HTTP client | OpenFeign |
| Dynamic instance lookup | Service discovery |
| Choose service instance | LoadBalancer |
| Config but not secret | Git config repo |
| Secret values | secrets manager |
| Cross-service request path | tracing |
| Avoid global transaction | saga/outbox |
| Kubernetes service lookup | Service DNS |

---

# 28. Strong Closing Answer

If interviewer asks:

```text
How do you use Spring Cloud in microservices?
```

Say:

```text
I use Spring Cloud selectively for distributed-system concerns. Config Server can centralize
environment-specific configuration, while secrets should come from a secure secrets manager.
Spring Cloud Gateway handles edge routing, filters, CORS, token relay, and rate limiting,
but business logic stays in services. OpenFeign is useful for declarative clients, but I
still configure timeouts, metrics, and resilience. Discovery and load balancing can come
from Spring Cloud or from the platform, such as Kubernetes Services. For consistency across
services, I avoid global transactions and prefer local transactions, outbox, events, and
compensation.
```

---

# 29. Official Source Notes

Useful official references:

- Spring Cloud Config: https://docs.spring.io/spring-cloud-config/reference/index.html
- Spring Cloud Gateway: https://docs.spring.io/spring-cloud-gateway/reference/index.html
- Spring Cloud OpenFeign: https://docs.spring.io/spring-cloud-openfeign/reference/index.html
- Spring Cloud Commons: https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/common-abstractions.html
- Spring Cloud Circuit Breaker: https://docs.spring.io/spring-cloud-circuitbreaker/reference/index.html

