# Microservice Design Patterns Interview Master Sheet

> Goal: prepare microservice design patterns from interview, product engineering, and production debugging angles.

This sheet is built for Java/Spring Boot backend interviews where the interviewer may ask:

- How do you split services?
- How do services communicate?
- How do you handle distributed transactions?
- How do you avoid double booking?
- How do you make services resilient?
- How do you debug production issues?
- How do you deploy safely?
- How do you secure service-to-service calls?

---

# 0. Interview Command Center

## Priority Meter

| Topic | Priority | Why It Matters |
|---|---:|---|
| Microservice vs monolith | Very high | Opening interview question |
| Service decomposition | Very high | Architecture maturity |
| Database per service | Very high | Core microservice rule |
| Sync vs async communication | Very high | Real system design |
| API Gateway | Very high | Entry point pattern |
| Service discovery | High | Runtime routing |
| Saga pattern | Very high | Distributed transaction answer |
| Outbox pattern | Very high | Reliable event publishing |
| Idempotency | Very high | Retry-safe systems |
| Circuit breaker | Very high | Failure isolation |
| Retry + timeout + backoff | Very high | Production reliability |
| Bulkhead | High | Stop cascading failure |
| CQRS | High | Read/write separation |
| Event sourcing | Medium | Advanced pattern |
| Cache-aside | High | Performance |
| Distributed tracing | High | Debugging |
| Centralized logging | High | Splunk/Dynatrace mapping |
| Health checks | High | Kubernetes/deployment readiness |
| Blue-green/canary deployment | Medium-high | CI/CD maturity |
| Contract testing | Medium-high | Microservice testing |
| Strangler fig | Medium | Migration pattern |

---

## One-Line Interview Definition

Microservices split a large application into independently deployable services, each owning a focused business capability, its own data, and communicating through APIs or events.

Strong answer:

> I use microservices when independent scaling, deployment, team ownership, and domain boundaries matter. I avoid them for small systems because they introduce distributed system complexity like network failures, consistency challenges, observability needs, and deployment coordination.

---

## Golden Rule

Microservices are not mainly about splitting code.

They are about splitting:

- business ownership
- data ownership
- deployment ownership
- scaling ownership
- failure boundaries

If services share the same database and must deploy together, they are usually a distributed monolith.

---

# 1. Microservice vs Monolith

## Monolith

A monolith packages many business capabilities into one deployable application.

Examples:

- booking
- payment
- notification
- loyalty
- reporting

all inside one application.

## Microservices

Microservices split those capabilities into separate services.

Example:

```text
Booking Service
Payment Service
Inventory Service
Notification Service
Loyalty Service
Search Service
```

Each service can be built, deployed, scaled, and owned separately.

## Strong Interview Comparison

| Area | Monolith | Microservices |
|---|---|---|
| Deployment | One deployment | Independent deployments |
| Data | Often one DB | Database per service |
| Calls | In-memory method calls | Network calls/events |
| Debugging | Easier locally | Needs logs/traces/metrics |
| Scaling | Whole app scales | Service-level scaling |
| Failure | One app may fail together | Failures can be isolated |
| Complexity | Lower operational complexity | Higher distributed complexity |

## When Microservices Are Worth It

Use microservices when:

- multiple teams own different domains
- modules need independent deployment
- traffic differs by capability
- some domains need separate scalability
- technology or data boundaries are clearly different
- business domain is large enough

## When Not To Use Microservices

Avoid microservices when:

- team is small
- product is early-stage
- domain boundaries are unclear
- operations maturity is low
- observability is weak
- database boundaries cannot be separated

## Interview Trap

Mistake:

> Microservices are always better than monoliths.

Better answer:

> Microservices solve team and scaling problems, but they add network, consistency, deployment, and observability complexity. For small or early systems, a modular monolith is often better.

---

# 2. Service Decomposition Pattern

## Definition

Service decomposition is the pattern of splitting a system into services based on business capabilities or bounded contexts.

## Common Strategies

| Strategy | Meaning | Example |
|---|---|---|
| By business capability | Split by business function | Booking, Payment, Loyalty |
| By bounded context | Split by domain language and model | Inventory vs Reservation |
| By subdomain | Core/supporting/generic domain | Pricing, Search, Reporting |
| By transaction boundary | Keep strongly consistent operations together | Booking + availability lock |

## Marriott Example

Possible services:

```text
Search Service
Availability Service
Booking Service
Payment Service
Notification Service
Loyalty Service
User/Profile Service
Pricing Service
Review Service
Reporting Service
```

## How To Explain In Interview

Start with the business flow:

```text
Search hotel -> Check availability -> Reserve room -> Take payment -> Confirm booking -> Notify guest -> Award loyalty points
```

Then split services around ownership:

- Search owns read-optimized hotel search.
- Availability owns inventory and date availability.
- Booking owns reservation lifecycle.
- Payment owns payment authorization/capture.
- Notification owns email/SMS/push.
- Loyalty owns points calculation and redemption.

## Bad Decomposition

Do not split only by technical layers:

```text
Controller Service
Repository Service
Validation Service
DTO Service
```

That is not microservices. That is a distributed layered monolith.

## Strong Answer

> I decompose services by business capability and bounded context. For a hotel platform, Booking, Availability, Payment, Notification, and Loyalty are good candidates because they have different data ownership, scaling needs, and business rules. I avoid splitting by technical layers because that creates chatty services and tight coupling.

---

# 3. Database Per Service Pattern

## Definition

Each microservice owns its own database schema or database. Other services cannot directly read/write it.

## Why It Exists

It protects service independence.

If many services directly share one database:

- schema changes become dangerous
- deployments become coupled
- business logic leaks across services
- ownership becomes unclear
- one bad query can hurt everyone

## Example

```text
Booking Service      -> booking_db
Payment Service      -> payment_db
Loyalty Service      -> loyalty_db
Notification Service -> notification_db
```

## How Services Access Data

Services access another service's data through:

- REST API
- gRPC
- events
- read models
- materialized views

Not direct table joins across service databases.

## Interview Trap

Question:

> If Booking needs payment status, can it join payment table?

Strong answer:

> No. Booking should not directly query Payment Service tables. It can call Payment Service API for current status or consume payment events and maintain a local read model if it needs fast reads.

## Pros and Cons

| Pros | Cons |
|---|---|
| Independent schema evolution | Hard cross-service queries |
| Strong ownership | Data duplication |
| Independent scaling | Eventual consistency |
| Safer deployments | More integration complexity |

## Strong Answer

> Database per service is central to microservices. It gives ownership and independent deployment, but it means we cannot use cross-service joins. For cross-service reads, I use APIs, events, or read models depending on latency and consistency needs.

---

# 4. Shared Database Anti-Pattern

## Definition

Multiple microservices read and write the same database tables.

## Why It Is Bad

It creates hidden coupling.

Problems:

- service A changes schema and breaks service B
- business rules are spread across services
- no clear data owner
- migrations are risky
- performance issues affect all services

## When It May Be Temporarily Accepted

Only during migration:

- legacy system modernization
- strangler migration
- short-term bridge

Even then, define an exit plan.

## Better Alternative

Use:

- database per service
- API composition
- event-driven data replication
- change data capture
- read model

## Strong Answer

> A shared database may look simple, but it defeats microservice independence. I would avoid it for new systems. If we inherit it, I would isolate ownership gradually using the strangler pattern and move consumers to APIs or events.

---

# 5. Synchronous Communication Pattern

## Definition

A service calls another service and waits for the response.

Common protocols:

- REST
- gRPC
- GraphQL for client-facing aggregation

## Example

```text
Booking Service -> Availability Service
Booking Service waits for availability response
```

## When To Use

Use sync calls when:

- user needs immediate response
- operation requires real-time validation
- dependency is part of request decision
- latency is acceptable

Example:

- check if room is available before booking
- validate payment method before confirming

## Risks

Sync calls can cause:

- increased latency
- cascading failure
- timeout issues
- retry storms
- tight runtime coupling

## Required Protection

Always combine sync calls with:

- timeout
- retry with backoff
- circuit breaker
- fallback if possible
- monitoring

## Strong Answer

> Synchronous calls are good when the caller needs immediate data to complete the request. But I protect them with timeouts, retries with backoff, circuit breakers, and clear fallback behavior because network calls can fail or become slow.

---

# 6. Asynchronous Communication Pattern

## Definition

Services communicate through messages/events without waiting for immediate processing.

Common tools:

- Kafka
- RabbitMQ
- SNS/SQS
- Pub/Sub

## Example

```text
Booking Service publishes BookingConfirmed event
Notification Service consumes event and sends email
Loyalty Service consumes event and adds points
```

## When To Use

Use async messaging when:

- operation can happen later
- multiple services need to react
- caller should not wait
- loose coupling is important
- spike absorption is needed

Examples:

- send confirmation email
- update loyalty points
- update analytics
- trigger invoice generation

## Pros and Cons

| Pros | Cons |
|---|---|
| Loose coupling | Eventual consistency |
| Better resilience | Harder debugging |
| Handles spikes | Duplicate events possible |
| Easy fan-out | Requires idempotent consumers |

## Strong Answer

> I use async events for side effects and downstream workflows that do not need to block the user request. For example, after booking confirmation, notification and loyalty updates can be event-driven. I make consumers idempotent because events may be delivered more than once.

---

# 7. API Gateway Pattern

## Definition

API Gateway is a single entry point for client requests into backend services.

## Responsibilities

Common gateway responsibilities:

- routing
- authentication
- authorization checks
- rate limiting
- request/response transformation
- SSL termination
- API versioning
- logging/correlation ID

## Example

```text
Mobile App / Web App
        |
   API Gateway
        |
  -------------------------
  | Search | Booking | User |
  -------------------------
```

## When To Use

Use API Gateway when:

- many backend services exist
- clients should not know service topology
- common cross-cutting concerns exist
- authentication/rate limits are centralized

## What Not To Put In Gateway

Avoid putting core business logic in gateway.

Bad:

```text
Gateway calculates room price
Gateway handles booking transaction
Gateway owns business validation
```

Better:

```text
Pricing Service calculates price
Booking Service owns booking rules
Gateway routes and enforces edge policies
```

## Strong Answer

> API Gateway is the front door for clients. I use it for routing, auth, rate limiting, and request-level policies. I avoid putting business logic there because that makes the gateway a bottleneck and creates coupling.

---

# 8. BFF Pattern

## Definition

BFF means Backend For Frontend.

It creates separate backend APIs tailored for each client type.

## Example

```text
Mobile BFF
Web BFF
Partner API BFF
```

Each BFF can compose backend services differently.

## When To Use

Use BFF when:

- mobile and web need different response shapes
- frontend requirements change quickly
- you want to avoid over-fetching/under-fetching
- different clients have different workflows

## Marriott Example

Mobile app may need:

- booking summary
- loyalty points
- upcoming stays
- mobile check-in status

Web admin may need:

- booking details
- audit history
- payment info
- support notes

Different BFFs prevent one generic API from becoming messy.

## Strong Answer

> BFF is useful when different clients need different API shapes. It keeps frontend-specific aggregation out of core domain services while avoiding a bloated one-size-fits-all API.

---

# 9. Service Discovery Pattern

## Definition

Service discovery lets services find each other dynamically at runtime.

## Why It Exists

In cloud/Kubernetes environments:

- service instances come and go
- IPs change
- replicas scale up/down

Hardcoding hostnames/IPs does not work well.

## Types

| Type | Meaning | Example |
|---|---|---|
| Client-side discovery | Client picks instance | Eureka + Ribbon style |
| Server-side discovery | Load balancer picks instance | Kubernetes Service |

## Kubernetes Example

```text
booking-service calls http://payment-service
Kubernetes Service routes to one payment pod
```

## Strong Answer

> Service discovery solves dynamic service location. In Kubernetes, server-side discovery is common: services call stable DNS names, and Kubernetes routes to healthy pods.

---

# 10. Load Balancing Pattern

## Definition

Load balancing distributes traffic across multiple service instances.

## Why It Matters

It improves:

- availability
- scalability
- throughput
- fault tolerance

## Algorithms

| Algorithm | Meaning |
|---|---|
| Round robin | Rotate requests across instances |
| Least connections | Send to least busy instance |
| Weighted | More traffic to stronger instances |
| Random | Random instance selection |

## Interview Trap

Load balancing does not fix bad state management.

If service instances store important user state in memory, scaling becomes risky.

Better:

- keep services stateless
- store session state externally
- use DB/cache/message broker

## Strong Answer

> Microservice instances should usually be stateless so load balancers can route requests to any healthy instance. State should live in a database, cache, or external storage.

---

# 11. Saga Pattern

## Definition

Saga manages distributed transactions using a sequence of local transactions and compensating actions.

## Why It Exists

In microservices, one business workflow may span multiple services.

Example:

```text
Reserve room -> Take payment -> Confirm booking -> Send notification
```

There is no single database transaction across all services.

Saga coordinates the workflow.

## Types

| Type | Meaning |
|---|---|
| Choreography | Services react to each other's events |
| Orchestration | A central orchestrator commands each step |

---

## Choreography Saga

Flow:

```text
Booking Service creates pending booking
Booking Service publishes BookingCreated
Payment Service consumes event and charges payment
Payment Service publishes PaymentSucceeded
Booking Service confirms booking
Notification Service sends confirmation
```

## Pros

- no central coordinator
- loosely coupled
- event-driven

## Cons

- flow is harder to visualize
- debugging is harder
- event chains can become messy

---

## Orchestration Saga

Flow:

```text
Booking Orchestrator
  -> reserve inventory
  -> charge payment
  -> confirm booking
  -> send notification
```

## Pros

- easier to understand workflow
- centralized decision making
- easier failure handling

## Cons

- orchestrator can become complex
- tighter control coupling

---

## Compensation Example

If payment fails:

```text
1. Booking pending
2. Inventory reserved
3. Payment failed
4. Release inventory
5. Mark booking failed
```

## Strong Answer

> For distributed transactions, I avoid two-phase commit in most microservice systems. I use Saga. For a booking system, I would create a pending booking, reserve inventory, process payment, and confirm booking. If payment fails, I compensate by releasing inventory and marking booking failed.

---

# 12. Transactional Outbox Pattern

## Definition

Outbox pattern reliably publishes events by writing the business change and event record in the same local database transaction.

## Problem It Solves

Bad flow:

```text
1. Save booking in DB
2. Publish BookingCreated event to Kafka
```

What if DB save succeeds but Kafka publish fails?

Then booking exists but no event is published.

## Outbox Solution

Use one local transaction:

```text
1. Save booking
2. Save outbox_event row
3. Commit transaction
4. Background publisher reads outbox_event
5. Publisher sends event to Kafka
6. Mark event as published
```

## Table Example

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    event_type VARCHAR(100),
    payload JSONB,
    status VARCHAR(20),
    created_at TIMESTAMP,
    published_at TIMESTAMP
);
```

## Java-ish Flow

```java
@Transactional
public Booking createBooking(CreateBookingRequest request) {
    Booking booking = bookingRepository.save(new Booking(request));

    OutboxEvent event = OutboxEvent.bookingCreated(booking);
    outboxRepository.save(event);

    return booking;
}
```

## Strong Answer

> I use transactional outbox when a database update must reliably produce an event. Instead of saving to DB and publishing directly, I save both the business entity and an outbox row in the same transaction. A separate publisher then sends the event. This prevents lost events.

---

# 13. Idempotent Consumer Pattern

## Definition

An idempotent consumer processes the same message multiple times without creating duplicate side effects.

## Why It Exists

Message brokers can deliver duplicates.

Retries can also duplicate work.

Example duplicate risk:

```text
BookingConfirmed event arrives twice
Loyalty Service adds points twice
```

## Solution

Store processed event IDs.

```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP
);
```

Consumer flow:

```text
1. Receive event
2. Check processed_events
3. If already processed, ignore
4. Else process business logic
5. Insert event_id into processed_events
6. Commit
```

## Java-ish Flow

```java
@Transactional
public void handleBookingConfirmed(BookingConfirmedEvent event) {
    if (processedEventRepository.existsById(event.eventId())) {
        return;
    }

    loyaltyService.addPoints(event.customerId(), event.amount());
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
}
```

## Strong Answer

> Since event delivery can be at least once, consumers must be idempotent. I store event IDs or use natural unique business keys so processing the same event twice does not duplicate side effects.

---

# 14. Inbox Pattern

## Definition

Inbox pattern stores incoming messages before processing them.

## Why It Exists

It helps make message processing reliable and auditable.

Flow:

```text
1. Consumer receives message
2. Store message in inbox table
3. Process inbox record
4. Mark processed
```

## When To Use

Use inbox when:

- processing is complex
- auditability is important
- you need replay
- duplicate detection is needed
- downstream side effects must be controlled

## Strong Answer

> Inbox pattern is useful on the consumer side. It records received messages and processing status, which gives idempotency, retry, and auditability.

---

# 15. CQRS Pattern

## Definition

CQRS means Command Query Responsibility Segregation.

It separates write models from read models.

## Basic Idea

```text
Command side: create/update booking
Query side: search bookings, show dashboard, show booking summary
```

## Why It Exists

Write model and read model often have different needs.

Write side needs:

- validation
- consistency
- domain rules

Read side needs:

- speed
- denormalized views
- filtering
- aggregation

## Marriott Example

Booking write model:

```text
booking_id
customer_id
room_id
status
check_in
check_out
payment_status
```

Booking read model:

```text
booking_id
hotel_name
guest_name
room_type
loyalty_tier
payment_status
check_in
check_out
display_status
```

The read model may be updated through events.

## When To Use

Use CQRS when:

- reads are much heavier than writes
- read queries are complex
- you need denormalized read views
- different scale requirements exist

## When Not To Use

Avoid CQRS when:

- CRUD is simple
- data model is small
- consistency must be immediate everywhere
- team cannot operate event-driven complexity

## Strong Answer

> CQRS separates write and read concerns. I use it when read requirements are complex or high-volume, such as hotel search or booking dashboards. I avoid it for simple CRUD because it adds synchronization and eventual consistency complexity.

---

# 16. Event Sourcing Pattern

## Definition

Event sourcing stores state changes as a sequence of events instead of only storing current state.

## Example

Instead of storing only:

```text
Booking status = CANCELLED
```

Store events:

```text
BookingCreated
RoomReserved
PaymentCaptured
BookingConfirmed
BookingCancelled
RefundInitiated
```

Current state is rebuilt by replaying events.

## Benefits

- full audit history
- easy replay
- temporal debugging
- event-driven integration

## Costs

- more complex queries
- schema evolution challenges
- replay logic
- eventual consistency
- harder debugging for beginners

## When To Use

Use when:

- audit trail is critical
- every state transition matters
- domain is event-heavy
- replay is valuable

Examples:

- payments
- wallet/ledger
- booking lifecycle audit

## Strong Answer

> Event sourcing stores every state change as an event and derives current state by replaying events. It is powerful for audit-heavy domains, but I would not use it for normal CRUD because it adds significant complexity.

---

# 17. Event-Driven Architecture Pattern

## Definition

Services publish and consume events to communicate state changes.

## Event Types

| Type | Meaning | Example |
|---|---|---|
| Domain event | Something important happened | BookingConfirmed |
| Integration event | Event shared across services | PaymentSucceeded |
| Command message | Ask another service to do something | ChargePayment |

## Event Naming

Good names are past tense:

- BookingCreated
- PaymentSucceeded
- RoomReserved
- BookingCancelled

Avoid vague names:

- BookingEvent
- UpdateEvent
- ProcessData

## Strong Answer

> I model events as business facts that already happened. For example, BookingConfirmed means the booking service has confirmed the booking. Other services can react independently, like notification or loyalty.

---

# 18. Retry Pattern

## Definition

Retry pattern re-attempts a failed operation, usually for transient failures.

## When To Retry

Retry when failure is temporary:

- network timeout
- temporary 503
- rate-limited call with retry-after
- database deadlock
- broker unavailable briefly

## When Not To Retry

Do not retry:

- validation errors
- authentication failures
- 400 bad request
- insufficient balance
- business rule failures

## Retry With Backoff

Bad:

```text
retry immediately 10 times
```

Better:

```text
retry after 100ms, 300ms, 1s, 3s
```

Add jitter to avoid thundering herd.

## Strong Answer

> I retry only transient failures and always combine retry with timeout, exponential backoff, max attempts, and idempotency. Retrying non-transient business errors creates load without helping.

---

# 19. Timeout Pattern

## Definition

Timeout limits how long a service waits for a dependency.

## Why It Matters

Without timeout:

- request threads get stuck
- connection pools exhaust
- latency increases
- cascading failure begins

## Example

```text
Booking Service calls Payment Service
Payment Service is slow
Booking threads wait forever
Booking Service becomes unavailable
```

## Strong Answer

> Every network call should have a timeout. A missing timeout can turn a slow dependency into a full service outage by exhausting threads or connections.

---

# 20. Circuit Breaker Pattern

## Definition

Circuit breaker stops calls to a failing dependency temporarily.

## States

| State | Meaning |
|---|---|
| Closed | Calls go through normally |
| Open | Calls fail fast |
| Half-open | Some trial calls are allowed |

## Why It Exists

If Payment Service is down, Booking Service should not keep hammering it.

Circuit breaker:

- prevents repeated failed calls
- protects threads
- reduces cascading failure
- gives dependency time to recover

## Flow

```text
Payment calls fail repeatedly
Circuit opens
Booking fails fast or uses fallback
After wait time, circuit half-opens
If trial succeeds, circuit closes
```

## Strong Answer

> Circuit breaker protects a service from repeatedly calling a failing dependency. It fails fast after a threshold and later allows trial calls in half-open state. I use it with timeout and retry carefully because retrying behind an open circuit defeats the purpose.

---

# 21. Bulkhead Pattern

## Definition

Bulkhead isolates resources so one failing dependency does not consume all capacity.

## Example

Separate thread pools:

```text
Payment calls      -> payment thread pool
Notification calls -> notification thread pool
Search calls       -> search thread pool
```

If notification is slow, payment calls still work.

## Where To Apply

- thread pools
- connection pools
- queues
- rate limits
- service instances

## Strong Answer

> Bulkhead pattern isolates resources. If one dependency is slow, it should not consume all threads or connections and break unrelated flows.

---

# 22. Rate Limiter Pattern

## Definition

Rate limiter controls how many requests are allowed in a time window.

## Why It Exists

It protects systems from:

- abuse
- accidental traffic spikes
- expensive operations
- downstream overload

## Common Algorithms

| Algorithm | Meaning |
|---|---|
| Fixed window | Count requests per fixed time window |
| Sliding window | Smoother rolling window |
| Token bucket | Tokens refill over time, burst allowed |
| Leaky bucket | Requests processed at steady rate |

## Where To Apply

- API Gateway
- per user
- per IP
- per partner/client
- per endpoint

## Strong Answer

> I usually rate limit at the gateway for external traffic and sometimes internally for expensive operations. Token bucket is common because it allows controlled bursts while maintaining an average rate.

---

# 23. Dead Letter Queue Pattern

## Definition

DLQ stores messages that cannot be processed successfully after retries.

## Why It Exists

Without DLQ:

- bad messages can block consumers
- failures are hidden
- infinite retries overload the system

## Flow

```text
Consumer receives event
Processing fails
Retry with backoff
Still fails after max attempts
Move to DLQ
Alert team
Fix and replay if needed
```

## Strong Answer

> DLQ is used for messages that fail repeatedly. It prevents poison messages from blocking processing and gives teams a place to inspect, fix, and replay failed messages.

---

# 24. Cache-Aside Pattern

## Definition

Application checks cache first. If cache miss, load from DB and populate cache.

## Flow

```text
1. Check Redis for hotel details
2. If found, return
3. If not found, query DB
4. Store result in Redis with TTL
5. Return result
```

## Java-ish Flow

```java
public Hotel getHotel(String hotelId) {
    Hotel cached = cache.get(hotelId);
    if (cached != null) {
        return cached;
    }

    Hotel hotel = hotelRepository.findById(hotelId)
        .orElseThrow();

    cache.put(hotelId, hotel, Duration.ofMinutes(10));
    return hotel;
}
```

## When To Use

Use for:

- read-heavy data
- hotel details
- static configuration
- price rules with controlled TTL

## Risks

- stale data
- cache stampede
- invalidation complexity
- inconsistent reads

## Strong Answer

> Cache-aside is useful for read-heavy data. The application loads from cache first and falls back to DB on miss. I use TTLs and invalidation carefully because caching trades freshness for speed.

---

# 25. API Composition Pattern

## Definition

An API composer calls multiple services and combines results into one response.

## Example

Booking details page needs:

- booking info from Booking Service
- payment status from Payment Service
- loyalty info from Loyalty Service
- hotel info from Hotel Service

API composer combines them.

## Where It Lives

Can be in:

- API Gateway
- BFF
- dedicated aggregator service

## Risks

- high latency due to multiple calls
- partial failure
- many dependencies
- difficult caching

## Strong Answer

> API composition is useful when a client needs data from multiple services. I use it carefully with parallel calls, timeouts, fallback, and partial response handling. For heavy read use cases, a denormalized read model may be better.

---

# 26. Materialized View / Read Model Pattern

## Definition

A service maintains a local read-optimized copy of data from events.

## Example

Search Service stores:

```text
hotel_id
hotel_name
city
amenities
rating
available_rooms_count
min_price
```

This avoids calling multiple services for every search.

## How It Updates

```text
HotelUpdated event
PriceChanged event
AvailabilityChanged event

Search Service consumes events and updates search index/read model
```

## Strong Answer

> For high-volume reads like hotel search, I would not compose many services on every request. I would maintain a read-optimized model updated through events, accepting eventual consistency for better performance.

---

# 27. Strangler Fig Pattern

## Definition

Gradually replace a legacy system by routing pieces of functionality to new services.

## Flow

```text
Old monolith handles everything
New Booking Service is built
Gateway routes booking APIs to new service
Remaining APIs still go to monolith
Gradually move more features
```

## When To Use

Use for:

- legacy modernization
- risky large rewrites
- gradual migration

## Strong Answer

> Instead of rewriting a monolith all at once, I use the strangler pattern. We put a routing layer in front, move one capability at a time to a new service, and gradually shrink the legacy system.

---

# 28. Anti-Corruption Layer Pattern

## Definition

An anti-corruption layer protects a new domain model from legacy or external models.

## Example

Legacy system uses:

```text
RM_TYP_CD
HTL_NUM
ARR_DT
DEP_DT
```

New service uses:

```text
roomTypeCode
hotelId
checkInDate
checkOutDate
```

ACL translates between them.

## Why It Exists

It prevents messy external models from leaking into clean domain code.

## Strong Answer

> I use an anti-corruption layer when integrating with legacy systems or third-party APIs. It translates external models into our domain model so the core service stays clean and independent.

---

# 29. Sidecar Pattern

## Definition

A sidecar runs alongside the main service and provides supporting capabilities.

## Examples

- logging agent
- proxy
- service mesh sidecar
- metrics collector
- config reloader

## Kubernetes Example

```text
Pod
  - booking-service container
  - logging-agent container
```

## Strong Answer

> Sidecar pattern lets us add infrastructure behavior like logging, proxying, or telemetry without changing application code. In Kubernetes, a sidecar container runs in the same pod as the application.

---

# 30. Service Mesh Pattern

## Definition

Service mesh manages service-to-service communication at infrastructure level.

## Features

- mTLS
- traffic routing
- retries
- timeouts
- circuit breaking
- observability
- policy enforcement

## Examples

- Istio
- Linkerd
- Consul Connect

## When To Use

Use when:

- many services exist
- service-to-service policies are complex
- mTLS is needed across services
- traffic shifting is important

## When Not To Use

Avoid if:

- system is small
- team lacks operational maturity
- simpler gateway/client libraries are enough

## Strong Answer

> Service mesh moves communication concerns like mTLS, traffic routing, retries, and observability out of application code and into infrastructure. It is powerful but adds operational complexity.

---

# 31. Observability Patterns

## Three Pillars

| Pillar | Purpose |
|---|---|
| Logs | What happened |
| Metrics | How system behaves over time |
| Traces | Where request spent time |

## Centralized Logging

Logs from all services go to one system.

Examples:

- Splunk
- ELK
- CloudWatch

Must include:

- timestamp
- service name
- request ID / correlation ID
- user/customer ID when safe
- booking ID / transaction ID
- error code

## Distributed Tracing

Trace follows one request across services.

Example:

```text
API Gateway
 -> Booking Service
   -> Availability Service
   -> Payment Service
   -> Notification event publish
```

Tools:

- OpenTelemetry
- Jaeger
- Zipkin
- Dynatrace

## Metrics

Important metrics:

- request rate
- error rate
- latency p95/p99
- CPU/memory
- DB connection pool usage
- queue lag
- Kafka consumer lag
- retry count
- circuit breaker open count

## Strong Answer

> In microservices, observability is mandatory. I use centralized logs with correlation IDs, metrics for latency/error/throughput, and distributed tracing to follow requests across service boundaries. This is how we debug production issues quickly.

---

# 32. Health Check Pattern

## Definition

Health checks tell infrastructure whether a service instance is alive and ready.

## Types

| Type | Meaning |
|---|---|
| Liveness | Is process alive? |
| Readiness | Can it receive traffic? |
| Startup | Has it finished starting? |

## Kubernetes Mapping

```text
Liveness failure  -> restart container
Readiness failure -> remove from service traffic
```

## Spring Boot Actuator

Useful endpoints:

```text
/actuator/health
/actuator/metrics
/actuator/prometheus
```

## Strong Answer

> Liveness tells if the app should be restarted. Readiness tells if it should receive traffic. A service can be alive but not ready if DB or critical dependencies are unavailable.

---

# 33. Security Patterns

## Edge Authentication

Client authenticates at API Gateway or identity layer.

Common:

- OAuth2
- JWT
- OpenID Connect

## Service Authorization

Even after gateway auth, internal services should validate authorization when needed.

## Service-To-Service Security

Common approaches:

- mTLS
- signed JWT between services
- service mesh identity
- network policies

## Secrets Management

Do not store secrets in code.

Use:

- Kubernetes secrets
- AWS Secrets Manager
- Vault
- cloud secret stores

## Strong Answer

> I authenticate users at the edge using OAuth2/JWT, enforce authorization in services for business decisions, and secure service-to-service traffic using mTLS or platform identity. Secrets should come from a secret manager, not code or plain config.

---

# 34. Deployment Patterns

## Rolling Deployment

Replace instances gradually.

Pros:

- simple
- common in Kubernetes

Cons:

- old and new versions run together
- backward compatibility matters

## Blue-Green Deployment

Two environments:

```text
Blue  = current production
Green = new version
```

Switch traffic after validation.

Pros:

- fast rollback
- clean cutover

Cons:

- needs duplicate infrastructure

## Canary Deployment

Send small traffic percentage to new version.

Example:

```text
1% -> 5% -> 25% -> 50% -> 100%
```

Rollback if metrics degrade.

## Strong Answer

> For safer releases, I prefer rolling or canary deployments. Canary is stronger for risky changes because we expose the new version to a small percentage of traffic and watch metrics before full rollout.

---

# 35. Contract Testing Pattern

## Definition

Contract testing verifies that service providers and consumers agree on API behavior.

## Why It Exists

Microservices evolve independently.

Consumer expects:

```json
{
  "bookingId": "B1",
  "status": "CONFIRMED"
}
```

Provider accidentally changes:

```json
{
  "id": "B1",
  "bookingStatus": "CONFIRMED"
}
```

Consumer breaks.

## Tools

- Pact
- Spring Cloud Contract

## Strong Answer

> Contract testing catches breaking API changes between services before deployment. It is especially useful when services are owned by different teams and deploy independently.

---

# 36. Microservice Testing Pyramid

## Levels

| Test Type | Purpose |
|---|---|
| Unit test | Test class logic |
| Slice test | Test layer like controller/repository |
| Integration test | Test service with DB/broker |
| Contract test | Verify API compatibility |
| End-to-end test | Verify critical business flow |

## Interview Point

Do not rely only on end-to-end tests.

They are:

- slow
- flaky
- expensive
- harder to debug

Use fewer E2E tests and more unit/integration/contract tests.

## Strong Answer

> In microservices, I use unit tests for business logic, integration tests for DB/broker behavior, contract tests for service APIs, and a small number of E2E tests for critical flows like booking confirmation.

---

# 37. Configuration Pattern

## Definition

Externalized configuration keeps environment-specific values outside code.

## Examples

- DB URL
- topic names
- feature flags
- timeout values
- retry limits
- API endpoints

## Sources

- environment variables
- Kubernetes ConfigMap
- Spring Cloud Config
- AWS Parameter Store
- Vault

## Strong Answer

> Configuration should be externalized so the same artifact can run in different environments. Secrets should not be stored with normal config; they should come from a secret manager.

---

# 38. Feature Flag Pattern

## Definition

Feature flags enable or disable behavior without redeploying code.

## Use Cases

- gradual rollout
- A/B testing
- disable broken feature
- release code separately from feature launch

## Risk

Too many stale flags create complexity.

## Strong Answer

> Feature flags help release safely by separating deployment from feature activation. But flags need ownership and cleanup, otherwise code becomes hard to reason about.

---

# 39. Hotel Booking System: Pattern Mapping

## Requirement

Design a hotel booking backend.

Core flow:

```text
Search hotels
Check availability
Create booking
Reserve room
Process payment
Confirm booking
Send notification
Award loyalty points
```

## Service Split

```text
Search Service
Hotel Service
Availability Service
Booking Service
Payment Service
Notification Service
Loyalty Service
Pricing Service
```

## Patterns Used

| Problem | Pattern |
|---|---|
| Client entry point | API Gateway |
| Different mobile/web needs | BFF |
| Independent domains | Service decomposition |
| Data ownership | Database per service |
| Booking + payment workflow | Saga |
| Reliable event publishing | Outbox |
| Duplicate events | Idempotent consumer |
| Search performance | CQRS/read model |
| Email/loyalty after booking | Async events |
| Payment service failure | Circuit breaker |
| Slow dependencies | Timeout |
| Transient failure | Retry with backoff |
| Spike protection | Rate limiter |
| Debugging request | Distributed tracing |
| Production monitoring | Logs + metrics + traces |
| Safe releases | Canary deployment |

## Booking Flow With Saga

```text
1. Client calls POST /bookings
2. Booking Service creates PENDING booking
3. Booking Service asks Availability Service to reserve room
4. Availability Service reserves room for short TTL
5. Booking Service asks Payment Service to authorize/capture payment
6. If payment succeeds, Booking Service confirms booking
7. Booking Service publishes BookingConfirmed through outbox
8. Notification Service sends email
9. Loyalty Service adds points
```

## Failure Handling

If payment fails:

```text
1. Mark booking as FAILED
2. Release reserved room
3. Publish BookingFailed event
4. Notify user if needed
```

If notification fails:

```text
Booking remains confirmed
Notification goes to retry/DLQ
```

Why?

Email failure should not cancel a successful booking.

## Strong Interview Answer

> I would make booking a saga because it spans inventory, payment, notification, and loyalty. The booking starts as pending, inventory is reserved, payment is processed, and then booking is confirmed. If payment fails, we compensate by releasing inventory and marking the booking failed. Confirmation events are published using outbox so downstream systems like notification and loyalty receive them reliably.

---

# 40. Hot Interview Questions And Strong Answers

## Q1. What is a microservice?

A microservice is an independently deployable service that owns a focused business capability and usually owns its own data. It communicates with other services through APIs or events.

## Q2. Microservices vs monolith?

Monolith is simpler to develop and deploy early, but scales and releases as one unit. Microservices allow independent scaling and deployment, but add distributed system complexity.

## Q3. How do you split microservices?

By business capability or bounded context, not by technical layers. I look for separate data ownership, team ownership, deployment needs, and scaling needs.

## Q4. Why database per service?

It gives data ownership and independent schema evolution. Without it, services become tightly coupled through a shared database.

## Q5. How do services communicate?

Synchronous REST/gRPC for immediate request-response needs. Asynchronous messaging/events for side effects, fan-out, and loose coupling.

## Q6. What is Saga?

Saga manages distributed transactions using local transactions and compensating actions. It can be orchestration-based or choreography-based.

## Q7. Choreography vs orchestration?

Choreography uses events and no central controller. Orchestration uses a coordinator to command each step. Choreography is loosely coupled but harder to trace; orchestration is easier to reason about but can centralize workflow complexity.

## Q8. Why not use two-phase commit?

Two-phase commit can block, reduce availability, and tightly couple services/resources. In most microservice systems, Saga with compensation is preferred.

## Q9. What is outbox pattern?

Outbox writes business data and event data in the same local transaction. A separate publisher sends the event later. This prevents lost events.

## Q10. What is idempotency?

Idempotency means the same operation can be safely repeated without changing the result incorrectly. It is required for retries and duplicate message handling.

## Q11. What is circuit breaker?

Circuit breaker prevents repeated calls to a failing dependency. It opens after failures, fails fast, and later allows trial calls.

## Q12. Retry vs circuit breaker?

Retry handles temporary failures. Circuit breaker protects against persistent failures. Retry must be limited and combined with timeout/backoff.

## Q13. What is bulkhead?

Bulkhead isolates resources like thread pools or connection pools so one slow dependency does not break the whole service.

## Q14. What is API Gateway?

API Gateway is the client entry point for routing, authentication, rate limiting, and cross-cutting edge concerns.

## Q15. What is BFF?

BFF is a backend tailored to a specific frontend, such as mobile or web, to avoid forcing all clients into one generic API shape.

## Q16. How do you handle eventual consistency?

I make state transitions explicit, use events, design user-visible statuses, use retries/DLQ, and make consumers idempotent.

## Q17. How do you debug microservices?

Use correlation IDs, centralized logs, distributed tracing, metrics, health checks, dashboards, and alerts. Trace the request path service by service.

## Q18. What is CQRS?

CQRS separates write and read models. It is useful when read queries are complex or high-volume and can tolerate eventual consistency.

## Q19. What is event sourcing?

Event sourcing stores state changes as events and rebuilds current state from event history. It is useful for audit-heavy domains but complex for normal CRUD.

## Q20. How do you prevent double booking?

Use database constraints/locking in the Availability or Booking service, keep the booking flow transactional locally, use a saga for cross-service steps, and make retries idempotent.

## Q21. What is DLQ?

Dead letter queue stores messages that fail repeatedly so they do not block processing and can be inspected or replayed.

## Q22. What is service discovery?

Service discovery lets services locate healthy instances dynamically. In Kubernetes, services usually call stable DNS names and Kubernetes routes to pods.

## Q23. What is service mesh?

Service mesh handles service-to-service communication concerns like mTLS, retries, traffic routing, and observability at infrastructure level.

## Q24. How do you deploy microservices safely?

Use rolling, blue-green, or canary deployments, backward-compatible APIs, database migration discipline, health checks, metrics, and quick rollback.

## Q25. What is contract testing?

Contract testing verifies that service providers and consumers agree on API behavior, reducing breaking changes during independent deployments.

---

# 41. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Splitting by controller/service/repository layers | Creates distributed monolith | Split by business capability |
| Shared database for all services | Tight coupling | Database per service |
| No timeout on service calls | Thread exhaustion | Set strict timeout |
| Blind retries | Retry storm | Retry only transient errors with backoff |
| No idempotency | Duplicate side effects | Idempotency keys/event IDs |
| Events published outside DB transaction | Lost event risk | Outbox pattern |
| One generic mega gateway | Bottleneck and business logic leakage | Keep gateway thin |
| Too many tiny services | Operational overhead | Start with clear boundaries |
| Ignoring observability | Impossible debugging | Logs, metrics, traces |
| Using microservices for small app | Overengineering | Modular monolith first |
| Treating async as instant | Eventual consistency surprise | Design visible states |
| No DLQ | Poison messages block system | Retry then DLQ |
| Ignoring schema evolution | Consumers break | Versioning/contracts |

---

# 42. Final Rapid Revision

## If Interviewer Says X, Think Y

| Interviewer Says | Think Pattern |
|---|---|
| Split this system | Decomposition by business capability |
| Each service owns data | Database per service |
| Need one client entry point | API Gateway |
| Mobile and web need different response | BFF |
| Booking spans payment and inventory | Saga |
| DB commit and Kafka event must both happen | Outbox |
| Event may arrive twice | Idempotent consumer |
| Search is slow with many joins | CQRS/read model |
| Downstream service is failing | Circuit breaker |
| Downstream service is slow | Timeout |
| Temporary network failure | Retry with backoff |
| One dependency consumes all threads | Bulkhead |
| Bad messages keep failing | DLQ |
| Debug request across services | Distributed tracing |
| Legacy migration | Strangler fig |
| External ugly model | Anti-corruption layer |
| Safe rollout | Canary/blue-green |
| API compatibility | Contract testing |

---

# 43. One-Hour Revision Plan

## First 15 Minutes: Foundation

Revise:

- microservice vs monolith
- service decomposition
- database per service
- sync vs async
- API Gateway and BFF

## Next 15 Minutes: Data Consistency

Revise:

- Saga
- choreography vs orchestration
- outbox
- idempotent consumer
- CQRS
- double-booking handling

## Next 15 Minutes: Resilience

Revise:

- timeout
- retry with backoff
- circuit breaker
- bulkhead
- rate limiter
- DLQ

## Final 15 Minutes: Production Maturity

Revise:

- logs/metrics/traces
- health checks
- security
- canary/blue-green
- contract testing
- hotel booking pattern mapping

---

# 44. Strong Closing Answer

If asked:

> How do you design microservices for a hotel booking platform?

Say:

> I would split the system by business capability: Search, Availability, Booking, Payment, Notification, Loyalty, and Pricing. Each service should own its own data. The booking flow is a distributed workflow, so I would use a Saga: create a pending booking, reserve inventory, process payment, then confirm the booking. If payment fails, compensate by releasing inventory and marking the booking failed. For reliable event publishing, I would use the outbox pattern, and for consumers like Notification and Loyalty, I would make processing idempotent. For resilience, every service call should have timeout, retry with backoff where appropriate, circuit breaker, and monitoring. For production debugging, I would use correlation IDs, centralized logs, metrics, and distributed tracing.

---

# 45. Final Memory Trick

Remember this sequence:

```text
Split -> Communicate -> Protect data -> Handle failure -> Observe -> Deploy safely
```

Mapping:

```text
Split              = decomposition, database per service
Communicate        = REST/gRPC/events, gateway, BFF
Protect data       = saga, outbox, idempotency, CQRS
Handle failure     = timeout, retry, circuit breaker, bulkhead, DLQ
Observe            = logs, metrics, traces, health checks
Deploy safely      = rolling, blue-green, canary, contract tests
```

