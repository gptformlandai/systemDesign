# Spring Boot Hotel Booking Production Reference Project

> Track: Spring Boot Interview Track - Capstone  
> Goal: unify the entire Spring Boot curriculum into one end-to-end production-style system.

---

## 1. Intuition

The capstone is the rehearsal room. Every separate Spring Boot concept becomes one coherent
system: API, validation, transactions, security, tests, events, batch, observability,
runtime, supply chain, and modernization.

---

## 2. Definition

- Definition: The hotel booking reference project is a production-style Spring Boot system
  design and implementation blueprint for interview practice.
- Category: capstone, portfolio architecture, system design plus implementation.
- Core idea: prove mastery by connecting topics into one realistic service.

---

## 3. Why It Exists

Learners often know individual annotations but struggle to explain the full lifecycle:

```text
HTTP request -> security -> controller -> validation -> service -> transaction -> DB
-> outbox -> Kafka -> consumer -> observability -> deployment -> incident response
```

This capstone forces that full chain to become muscle memory.

---

## 4. Reality

Reference project:

```text
Hotel Booking Platform
```

Core capabilities:

- customer searches hotels
- customer creates booking
- system prevents double booking
- payment authorization is called
- confirmation event is published
- loyalty consumer processes event idempotently
- nightly settlement batch reconciles payments
- admin portal uses secure BFF/session flow
- observability and runtime controls are production-ready

---

## 5. How It Works

Request lifecycle:

1. Client sends `POST /bookings` with idempotency key.
2. Security filter validates session/JWT.
3. Controller validates DTO.
4. Service starts transaction.
5. Availability row is locked or constrained.
6. Booking row is inserted.
7. Outbox row is inserted in the same transaction.
8. Transaction commits.
9. Relay publishes booking event to Kafka/Pulsar.
10. Consumer processes event idempotently.
11. Metrics, logs, traces, and audit events record the flow.
12. Kubernetes readiness/liveness and graceful shutdown protect rollout.

Failure path:

- payment timeout -> booking remains pending or compensation starts
- duplicate request -> idempotency table returns previous response
- Kafka publish fails -> outbox relay retries
- consumer crashes -> event replay and dedupe table prevent duplicate effect
- DB pool exhaustion -> readiness may remain up but p99 alerts fire

Recovery path:

- retry with backoff where safe
- compensate where business effect cannot complete
- reconcile with batch
- use runbooks for Hikari/JFR/thread/heap/debugging

---

## 6. What Problem It Solves

- Primary problem solved: disconnected Spring Boot knowledge.
- Secondary benefits: interview storytelling, practical code intuition, architecture depth.
- Systems impact: learner can reason from code to production operations.

---

## 7. When To Rely On It

Use the capstone when:

- finishing the track
- preparing for MAANG system design plus coding hybrid rounds
- testing whether knowledge is connected
- building a portfolio project
- running mock interviews

---

## 8. When Not To Use It

Do not try to build every feature on day one.

Build in phases:

1. REST + validation + tests.
2. JPA + transactions + migrations.
3. Security + tenant/domain checks.
4. Payment client + resilience.
5. Outbox + messaging.
6. Batch + reconciliation.
7. Observability + runtime.
8. Supply chain + modernization.

---

## 9. Pros And Cons

| Pros | Cons |
|---|---|
| Connects every topic | Larger than a small tutorial |
| Excellent interview anchor | Requires disciplined scope |
| Shows production thinking | Needs multiple testing styles |
| Can become portfolio project | Messaging/batch/runtime add complexity |
| Exposes weak areas quickly | Not all features must be coded fully |

---

## 10. Trade-offs And Common Mistakes

### Trade-offs

- Gain: end-to-end mastery and interview fluency.
- Give up: quick single-topic comfort.
- Latency: payment, DB, cache, and event decisions affect p99.
- Throughput: database constraints and pool size limit booking writes.
- Consistency: booking invariants must be strong; notifications can be eventual.
- Complexity: each reliability pattern should protect a real failure mode.

### Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Start with Kafka before REST/JPA is correct | Adds complexity too early | Build core booking invariant first |
| Payment call inside long DB transaction | Holds locks/connections | Use pending state or short transaction |
| No idempotency | Retries create duplicates | Idempotency key and request hash |
| No DB constraint | Race condition survives app checks | Unique/constraint plus transaction |
| Expose entity as API | API coupled to schema | DTOs and mapping |
| No observability | Cannot debug production | Metrics, logs, traces, audit |

---

## 11. Key Numbers

Reasoning targets:

- Booking create p95: usually target low hundreds of milliseconds excluding slow payment.
- Payment timeout: often hundreds of milliseconds to a few seconds, with strict upper bound.
- DB pool: usually small per pod; do not size it equal to request concurrency.
- Idempotency key TTL: often 24 hours to 7 days depending on client retry window.
- Outbox retry: seconds to minutes with backoff.
- Batch settlement: nightly or hourly depending on business.
- SLO example: 99.9% successful booking API availability.

---

## 12. Failure Modes

| Failure | User Observes | Mitigation |
|---|---|---|
| Duplicate booking attempt | One succeeds, one conflict | DB constraint/locking |
| Payment timeout | Pending or failed booking | Resilience and reconciliation |
| Duplicate POST retry | Same response returned | Idempotency table |
| Event publish failure | Delayed side effect | Outbox relay retry |
| Consumer duplicate | No duplicate email/loyalty | Processed event table |
| Bad deployment | Errors spike | Canary and rollback |
| Hikari exhaustion | p99 grows/timeouts | Query/pool/concurrency runbook |
| CVE in dependency | Release blocked | SBOM scan and patch |

---

## 13. Scenario

- Product/system: hotel booking platform.
- Why this concept fits: it touches nearly every high-frequency Spring Boot interview topic.
- What would go wrong without it: knowledge remains fragmented and hard to communicate
  under interview pressure.

---

## 14. Project Blueprint

Suggested package layout:

```text
com.example.hotel
  HotelApplication
  booking
    api
    application
    domain
    persistence
  inventory
  payment
  notification
  settlement
  security
  observability
  shared
```

Suggested endpoints:

```text
POST   /bookings
GET    /bookings/{id}
GET    /bookings?customerId=&cursor=
DELETE /bookings/{id}
GET    /hotels/search
POST   /admin/hotels/{id}/inventory
GET    /actuator/health/readiness
```

Suggested tables:

```text
bookings(id, customer_id, hotel_id, room_type_id, check_in, check_out, status, version)
room_inventory(hotel_id, room_type_id, date, available_count, version)
idempotency_keys(user_id, key, request_hash, response_json, expires_at)
payments(id, booking_id, status, provider_reference)
outbox_events(id, aggregate_id, event_type, payload, status, created_at)
processed_events(consumer_name, event_id, processed_at)
```

---

## 15. Mini Program / Simulation

```python
class Inventory:
    def __init__(self, rooms):
        self.rooms = rooms

    def book(self, idempotency_key, seen):
        if idempotency_key in seen:
            return seen[idempotency_key]
        if self.rooms <= 0:
            return "sold-out"
        self.rooms -= 1
        seen[idempotency_key] = "confirmed"
        return "confirmed"


def main():
    inventory = Inventory(rooms=1)
    seen = {}
    print(inventory.book("abc", seen))
    print(inventory.book("abc", seen))
    print(inventory.book("xyz", seen))


if __name__ == "__main__":
    main()
```

---

## 16. Practical Question

> Design and implement a Spring Boot hotel booking platform that supports booking,
> payment authorization, inventory protection, notification, settlement, security,
> observability, and production-safe deployment.

---

## 17. Strong Answer

I would start with a modular Spring Boot service: booking, inventory, payment, notification,
and settlement modules. The booking API uses DTOs, validation, ProblemDetail, pagination,
and idempotency keys. The service transaction protects the booking invariant with database
constraints and optimistic or pessimistic locking. Payment calls have timeouts, retries only
where safe, circuit breaker, and clear pending/failed states. A booking commit writes an
outbox row in the same transaction; a relay publishes to Kafka or Pulsar; consumers are
idempotent. Security combines JWT/resource-server or BFF/session flow with method and
domain authorization. Tests include unit, MVC slice, JPA/Testcontainers, WireMock, contract,
migration, and architecture boundary tests. Production includes Actuator, Micrometer,
OpenTelemetry, structured logs, Docker/Kubernetes probes, graceful shutdown, SBOM/image
scans, and canary rollback.

---

## 18. Revision Notes

- One-line summary: the capstone proves Spring Boot mastery by connecting request lifecycle,
  data correctness, security, messaging, batch, observability, runtime, and supply chain.
- Three keywords: invariant, idempotency, outbox.
- One interview trap: reliability patterns should protect specific failure modes, not be
  listed as buzzwords.
- One memory trick: request -> transaction -> event -> observe -> operate.

