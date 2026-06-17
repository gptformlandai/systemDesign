# Airline Booking System - End-to-End System Design

> Goal: practice one complete E2E travel booking problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for distributed booking and reservation systems.
- Start broad with requirements and scale, then zoom into flight search, fare inventory, seat holds, PNRs, payment, ticketing, concurrency, consistency, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For airline booking systems, optimize inventory correctness, fare consistency, reservation expiry, payment/ticketing reliability, external-system integration, and graceful conflict handling.

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

| Layer | Interview signal | Airline booking focus |
|---|---|---|
| Problem understanding | Can clarify booking lifecycle | search flights, select fare, passenger details, hold, pay, ticket |
| HLD | Can design transactional systems | search, pricing, inventory, reservation, payment, ticketing, notification |
| LLD | Can model maintainable components | `Flight`, `FareClass`, `InventoryHold`, `PNR`, `Ticket`, `PaymentAttempt` |
| Machine coding | Can implement critical path | hold inventory, confirm reservation, ticket after payment, expire hold |
| Traffic spikes | Can protect production | sale launch, holiday search bursts, payment/GDS delays |
| Global scale | Can reason across partners | airline/GDS integration, multi-leg itinerary, region/data partitioning |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can search flights by origin, destination, date, passengers, and cabin.
- Users can view fares and availability.
- Users can select itinerary and fare class.
- System temporarily holds fare inventory/seats.
- User enters passenger details.
- User pays for reservation.
- System issues ticket(s) after successful payment.
- Holds expire if payment/ticketing is not completed.
- User can view booking/PNR and receive confirmation.

Optional requirements to clarify:

- Are we an airline-owned system or OTA aggregating multiple airlines/GDS?
- Is seat selection required before payment?
- Are multi-leg and connecting flights in scope?
- Do we support cancellation/refund/change?
- Are loyalty points and coupons in scope?
- Is overbooking allowed by airline policy?

Out of scope unless interviewer asks:

- Full GDS protocol internals.
- Full airline revenue management system.
- Full fraud/risk engine.
- Full refund/exchange repricing engine.

## 1.2 Non-Functional Requirements

Inventory correctness:

- Do not sell more inventory than allowed by airline policy.
- Fare class availability must be revalidated before booking.
- Holds expire deterministically.

Transaction reliability:

- Payment and ticketing must be idempotent.
- If payment succeeds but ticketing fails, recovery/refund workflow is required.
- Booking state should be auditable.

Search performance:

- Flight search is read-heavy and cacheable, but prices/availability can change.
- Search results can be stale; booking confirmation cannot.

## 1.3 Constraints

- Flight availability changes frequently.
- Fare prices can change between search and booking.
- External airline/GDS systems can be slow or unavailable.
- Multi-passenger and multi-leg itineraries require all-or-nothing reservation semantics.
- Payment providers send duplicate/late callbacks.
- Seat selection and fare inventory are related but not identical.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Search QPS | millions/sec globally during peaks |
| Booking attempts/day | tens of millions |
| Confirmed bookings/day | millions |
| Flights in active schedule | millions |
| Peak sale traffic | 10x-100x normal |
| Reservation hold TTL | 10-20 minutes |
| Search latency target | p95 under 1-2 sec |
| Booking confirmation target | seconds, external-dependent |
| Booking availability target | 99.9%-99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- Search QPS is much higher than booking QPS.
- A single flight/fare can become hot during sales or holidays.
- Multi-leg itinerary search can multiply availability/pricing checks.
- Booking requires strong consistency per `(flightId, fareClass)` or external airline inventory record.
- Ticketing can lag payment and needs recovery state.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Reservation hold TTL | 10-20 minutes |
| Search cache TTL | seconds to minutes |
| Fare quote TTL | minutes |
| Payment callback retry | hours to days |
| Ticketing retry window | minutes to hours |
| Flight inventory partition | flight/date/fare class |

## 1.6 Clarifying Questions To Ask

- Are we the airline source of truth or a booking layer over airlines/GDS?
- Do we hold exact seats or fare-class inventory?
- Are connecting/multi-airline itineraries in scope?
- What happens if fare changes after search?
- What happens if payment succeeds but ticketing fails?
- Are cancellations/refunds/changes in scope?

Strong interview framing:

> I will design airline booking with a read-heavy search path and a strongly controlled reservation path. Search results are quotes; booking revalidates fare inventory, creates a short-lived PNR/hold, takes payment idempotently, then issues tickets through an idempotent ticketing workflow.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Search flow:
Client -> Flight Search Service
       -> Schedule Cache + Availability Cache + Pricing/Fare Cache
       -> ranked itineraries and fare quotes

Booking flow:
Client -> Reservation Service
       -> reprice/revalidate fare
       -> Inventory Service hold seats/fare class
       -> create PNR/reservation
       -> Payment Service
       -> Ticketing Service
       -> Confirmation/Notification

Expiry/recovery flow:
Hold expires -> Inventory release
Payment success but ticket failure -> retry ticketing or refund workflow
```

Recommended architecture:

```text
Client Apps
  |
  v
+-----------------------+
| API Gateway + Auth    |
+-----------+-----------+
            |
            +-------------------+-------------------+
            |                   |                   |
            v                   v                   v
+----------------+     +----------------+    +----------------+
| Flight Search  |     | Reservation    |    | Booking/PNR    |
| Service        |     | Service        |    | Service        |
+-------+--------+     +-------+--------+    +-------+--------+
        |                      |                     |
        v                      v                     v
+----------------+     +----------------+    +----------------+
| Search/Fare    |     | Inventory      |    | Booking DB     |
| Cache/Index    |     | Service        |    +----------------+
+----------------+     +-------+--------+
                               |
                               v
                      +----------------+
                      | Payment Svc    |
                      +-------+--------+
                              |
                              v
                      +----------------+
                      | Ticketing Svc  |
                      | Airline/GDS    |
                      +----------------+
```

Request flow for booking:

1. User selects itinerary/fare from search result.
2. Reservation Service revalidates itinerary, fare, passenger count, and availability.
3. Inventory Service atomically holds inventory for all flight legs/fare classes.
4. Booking/PNR Service creates reservation with expiry.
5. Payment Service creates payment attempt with idempotency key.
6. Payment success triggers ticketing.
7. Ticketing Service issues ticket(s) through airline/GDS or internal ticket ledger.
8. Booking changes to `TICKETED`.
9. Confirmation is sent.
10. Expiry worker releases unpaid holds.

## 2.2 APIs

### Search Flights

```http
GET /v1/flights/search?from=SFO&to=JFK&date=2026-07-01&passengers=2&cabin=ECONOMY
```

### Create Fare Quote

```http
POST /v1/fare-quotes
```

```json
{
  "itineraryId": "itin-1",
  "passengers": 2,
  "fareClass": "Y"
}
```

### Hold Reservation

```http
POST /v1/reservations
Idempotency-Key: res-abc
```

```json
{
  "fareQuoteId": "quote-1",
  "passengers": [
    {"firstName": "Asha", "lastName": "Rao"},
    {"firstName": "Dev", "lastName": "Rao"}
  ]
}
```

Response:

```json
{
  "pnr": "AB12CD",
  "reservationId": "res-1",
  "state": "HELD",
  "amount": 62000,
  "expiresAt": "2026-06-17T12:20:00Z"
}
```

### Payment Callback

```http
POST /v1/payments/callback
```

### Get Booking

```http
GET /v1/bookings/{reservationId}
Authorization: Bearer <token>
```

Important API points:

- Search is not a guarantee; fare quote/hold is closer to commitment.
- Reservation and payment are idempotent.
- Ticketing may be async after payment.
- Booking state must be visible to user during recovery.

## 2.3 Core Components

Think of airline booking as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Search plane | schedules, fares, availability snapshots | fast discovery |
| Pricing plane | fare quotes, taxes, rules | transparent price validation |
| Inventory plane | fare class/seat holds | no unintended oversell |
| Reservation plane | PNR, passengers, hold expiry | auditable booking lifecycle |
| Payment plane | charge/refund/capture | money correctness |
| Ticketing plane | issue tickets with airline/GDS | final travel entitlement |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Flight Search Service | itinerary search | final inventory commitment | search QPS |
| Fare/Pricing Service | fare quote, taxes, rules | seat locks | quote QPS |
| Inventory Service | fare class availability/holds | payment callbacks | hot flight/fare conflicts |
| Reservation Service | hold orchestration | provider charge internals | booking attempts |
| Booking/PNR Service | passenger reservation state | search index | reservation reads/writes |
| Payment Service | payment attempt and callbacks | fare inventory locks | payment volume |
| Ticketing Service | ticket issue/retry | search ranking | ticketing events |
| Expiry Worker | release unpaid holds | user search | active holds |

### Search And Fare Quotes

Why it exists:

- Users need fast search over many flights.
- Live inventory checks for every search can overload airline systems.

Strategy:

- Cache schedules and approximate availability.
- Return fare quote with TTL.
- Revalidate before reservation hold.
- Inform user if fare changed or sold out.

Failure behavior:

- Search cache stale: booking revalidation catches it.
- Fare quote expired: user must refresh quote.

Interview signal:

> Search result is an offer to check, not a booking guarantee.

### Inventory And Locking

Inventory can mean:

- Fare class seats: `flightId + fareClass -> available count`.
- Specific seat map assignment: `flightId + seatId`.
- Overbooking-adjusted availability if airline policy allows.

Locking options:

| Option | Fit | Trade-off |
|---|---|---|
| Atomic decrement with hold record | fare-class inventory | simple and scalable |
| DB row lock per flight/fare | hot conflict safety | can bottleneck |
| Optimistic version/CAS | high throughput | retry on conflicts |
| External GDS hold | OTA model | slower, external dependency |

Recommended approach:

- For fare inventory, use atomic conditional decrement if enough seats exist.
- For multi-leg itinerary, reserve all legs in a saga with compensation.
- Holds have TTL and owner reservation ID.
- Confirm/ticket only if hold remains active.

### Reservation / PNR State Machine

States:

```text
CREATED -> HELD -> PAYMENT_PENDING -> PAID -> TICKETING -> TICKETED
       -> EXPIRED
       -> CANCELLED
       -> PAYMENT_FAILED
       -> TICKETING_FAILED
       -> REFUND_PENDING
```

Rules:

- Cannot ticket before payment success.
- Cannot pay after hold expiry without revalidation.
- Duplicate callbacks should not create duplicate tickets.
- Failed ticketing after payment triggers retry or refund/manual ops.

Interview signal:

> Reservation state is the system's audit trail. Do not hide failures behind a boolean booking status.

### Payment And Ticketing Saga

Flow:

1. Hold inventory.
2. Create PNR/reservation.
3. Take payment.
4. Issue ticket.
5. Confirm booking.

Saga failure handling:

| Failure | Recovery |
|---|---|
| hold fails | ask user to choose another fare |
| payment fails | keep hold until expiry or release |
| payment succeeds but ticket fails | retry ticketing, then refund/manual review |
| callback duplicated | idempotent processing |
| hold expires before payment success | reject ticketing and refund |

### Seat Selection

Important distinction:

- Fare inventory reserves a right to travel.
- Seat selection reserves a physical seat assignment.

Options:

- Seat selection after ticketing.
- Seat selection during reservation with separate seat hold.
- Paid seats require separate payment/refund logic.

Interview signal:

> Airline inventory is usually fare-class capacity first; exact seat assignment can be a separate reservation.

## 2.4 Data Layer

### Core Data Models

Flight inventory:

```json
{
  "flightId": "fl-100",
  "departureDate": "2026-07-01",
  "fareClass": "Y",
  "available": 12,
  "held": 4,
  "sold": 120,
  "version": 88
}
```

Fare quote:

```json
{
  "fareQuoteId": "quote-1",
  "itineraryId": "itin-1",
  "fareClass": "Y",
  "passengers": 2,
  "amount": 62000,
  "expiresAt": "2026-06-17T12:10:00Z"
}
```

Reservation:

```json
{
  "reservationId": "res-1",
  "pnr": "AB12CD",
  "state": "HELD",
  "fareQuoteId": "quote-1",
  "passengers": 2,
  "expiresAt": "2026-06-17T12:20:00Z"
}
```

Ticket:

```json
{
  "ticketId": "tkt-1",
  "reservationId": "res-1",
  "passengerId": "pax-1",
  "flightId": "fl-100",
  "state": "ISSUED"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Schedules | search index/cache | fast flight search |
| Fare quotes | KV/relational TTL | quote lifecycle |
| Inventory | strongly consistent DB/KV | atomic availability |
| Reservations/PNR | relational DB | audit and lifecycle |
| Payments | ledger DB | money correctness |
| Tickets | relational/ledger | final entitlement |
| Events | stream | saga/recovery/notifications |

Relational-style tables:

```sql
flights(flight_id PK, airline, origin, destination, departure_time, arrival_time)
flight_inventory(flight_id, fare_class, available, held, sold, version)
fare_quotes(fare_quote_id PK, itinerary_id, amount, expires_at)
reservations(reservation_id PK, pnr, user_id, state, expires_at, amount)
reservation_segments(reservation_id, flight_id, fare_class, passengers)
payments(payment_id PK, reservation_id, state, provider_ref, amount)
tickets(ticket_id PK, reservation_id, passenger_id, flight_id, state)
```

Important indexes:

- `flight_inventory(flight_id, fare_class)` for atomic hold.
- `reservations(pnr)` for lookup.
- `reservations(expires_at, state)` for expiry.
- `payments(provider_ref)` for callback idempotency.
- `tickets(reservation_id)` for booking retrieval.

### Partitioning

- Partition search by route/date.
- Partition inventory by `flightId`.
- Partition reservations by reservation ID/time.
- Hot sale flights can get dedicated inventory partitions.
- External airline/GDS integration queues partition by airline/provider.

### Replication And Consistency

- Search and fare cache can be eventually consistent.
- Inventory hold/confirm requires strong consistency per flight/fare.
- Reservation/payment/ticket state needs durable audit log.
- External provider state may be eventually reconciled.

## 2.5 Scalability

### Horizontal Scaling

- Search Service scales by query QPS and cache.
- Fare Service scales by quote QPS.
- Inventory Service scales by flight/fare partitions.
- Reservation Service scales by booking attempts.
- Ticketing workers scale by provider throughput.
- Expiry workers scale by active holds.

### Hot Flight/Sale Strategy

- Queue booking attempts for hot flights.
- Cache search pages aggressively.
- Use atomic inventory counters with versioning.
- Apply per-user/session rate limits.
- Protect airline/GDS providers with backpressure.
- Communicate fare changes clearly.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Flight search | 500 ms-2 sec |
| Fare quote | 100-500 ms |
| Inventory hold | 100-500 ms |
| Payment | provider-dependent |
| Ticketing | seconds to minutes |
| Booking retrieval | 50-200 ms |

### Optimization Rules

- Cache search and schedule data.
- Revalidate inventory at reservation time.
- Keep inventory hold transaction small.
- Use async ticketing with visible booking state.
- Use idempotency everywhere user/provider can retry.

## 2.7 Async Systems

Use streams for:

- fare quote created/expired
- inventory held/released
- reservation state changed
- payment succeeded/failed
- ticketing requested/succeeded/failed
- refund requested
- notification requested
- provider reconciliation

Queue notes:

- Saga steps must be idempotent.
- Ticketing requests use reservation ID as idempotency key.
- Expiry must check reservation state before release.
- Reconciliation jobs compare internal and provider states.

## 2.8 Security, Privacy, And Compliance

Security:

- Authenticated booking APIs.
- Payment tokens through secure provider/token vault.
- PII encryption for passenger data.
- Least-privilege access to booking records.

Privacy/compliance:

- Passenger names/passport details are sensitive.
- Audit access to PNR data.
- Retention policies for travel records.

Abuse controls:

- Rate-limit search scraping.
- Bot defenses for fare sales.
- Fraud/risk checks before ticketing.
- Payment anomaly detection.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Search | latency, cache hit rate, stale quote rate |
| Inventory | hold success, conflict rate, oversell guard |
| Reservation | state transition failures, expiry lag |
| Payment | success rate, callback duplicate rate, pending payments |
| Ticketing | issue latency, provider failure rate, retry backlog |
| Saga | stuck reservation count, refund/manual queue |

Alerts:

- Inventory oversell guard triggers.
- Payment succeeded but ticketing failure spikes.
- Expired holds are not released.
- Provider/GDS latency exceeds threshold.
- Search returns many stale fares.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Search availability | cached snapshot | live inventory every query | speed vs freshness |
| Inventory | atomic counters | row locks | throughput vs simplicity |
| Multi-leg hold | saga with compensation | distributed transaction | availability vs complexity |
| Ticketing | async | synchronous before response | resilience vs immediate certainty |
| Overbooking | allow by policy | strict no oversell | revenue optimization vs user risk |
| External systems | direct calls | queued adapters | freshness vs provider protection |

Interview framing:

> I would separate search from booking. Search is cached and approximate; reservation revalidates, atomically holds inventory, then runs an idempotent payment and ticketing saga with expiry and reconciliation.

---

# 3. Low-Level Design

LLD goal:

> Model airline booking around flights, fare inventory, fare quotes, inventory holds, PNR reservations, tickets, payments, and saga recovery.

Simple rules:

- Search result is not a guarantee.
- Hold inventory before payment.
- Ticket only after payment.
- Every external callback/request is idempotent.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Flight` | schedule and route | identified by flight/date |
| `FareClassInventory` | available/held/sold count | cannot exceed policy capacity |
| `FareQuote` | price with TTL | expires before hold |
| `InventoryHold` | reserved units and expiry | released or ticketed once |
| `Reservation/PNR` | passenger itinerary state | auditable lifecycle |
| `PaymentAttempt` | payment provider state | idempotent by reservation |
| `Ticket` | passenger flight entitlement | issued once per passenger/segment |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `SearchService` | discover flights | commit inventory |
| `PricingService` | quote fare | issue tickets |
| `InventoryService` | hold/release/confirm inventory | charge cards |
| `ReservationService` | PNR lifecycle | search ranking |
| `PaymentService` | payment attempts/callbacks | mutate inventory alone |
| `TicketingService` | issue tickets | run search |

## 3.2 OOP Fundamentals

Encapsulation:

- `Reservation` owns valid state transitions.
- `FareClassInventory` owns available/held/sold math.
- `Ticket` owns issued/void state.

Abstraction:

- `InventoryRepository` hides lock/CAS implementation.
- `AirlineProviderClient` hides airline/GDS protocol.
- `PaymentGateway` hides payment provider.

Polymorphism:

- Different inventory strategies for airline-owned vs OTA/GDS.
- Different refund/ticketing policies by airline.

Composition:

- `ReservationSaga` composes inventory, payment, ticketing, notification, and recovery services.

## 3.3 SOLID Principles

| Principle | Airline booking application |
|---|---|
| Single Responsibility | `TicketingService` only issues/voids tickets |
| Open/Closed | add provider adapter without rewriting reservation saga |
| Liskov Substitution | any inventory provider preserves hold/release/confirm contract |
| Interface Segregation | separate search, pricing, inventory, payment, ticketing APIs |
| Dependency Inversion | core workflow depends on provider interfaces |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Saga | hold -> pay -> ticket -> notify | recover multi-step workflow |
| State | reservation lifecycle | valid transitions |
| Adapter | airline/GDS/payment providers | isolate external APIs |
| Command | reservation/payment/ticketing requests | idempotency and retry |
| Observer/Event Publisher | state changes | notifications/reconciliation |

## 3.5 UML / Diagrams

### Booking Sequence

```text
Client -> ReservationService: createReservation(quote)
ReservationService -> PricingService: revalidate quote
ReservationService -> InventoryService: hold(flight,fare,count)
ReservationService -> PnrRepository: create HELD
Client -> PaymentService: pay(reservationId)
PaymentService -> ReservationSaga: paymentSucceeded
ReservationSaga -> TicketingService: issue tickets
TicketingService -> ReservationService: mark TICKETED
ReservationService -> NotificationService: send confirmation
```

### Expiry Sequence

```text
ExpiryWorker -> ReservationRepository: find expired HELD/PAYMENT_PENDING
ExpiryWorker -> InventoryService: release hold
ExpiryWorker -> ReservationService: mark EXPIRED
ExpiryWorker -> EventStream: reservation.expired
```

## 3.6 Class Design

Interfaces:

```java
interface InventoryService {
    InventoryHold hold(Itinerary itinerary, int passengerCount, Duration ttl);
    void release(String holdId);
    void confirmSold(String holdId, String reservationId);
}

interface ReservationStateMachine {
    Reservation transition(String reservationId, ReservationEvent event);
}

interface TicketingProvider {
    List<Ticket> issueTickets(String reservationId, List<Passenger> passengers);
}

interface PaymentGateway {
    PaymentResult charge(String idempotencyKey, Money amount, String paymentMethodId);
}
```

Design notes:

- `hold()` should be all-or-nothing for itinerary legs, or compensate safely.
- `issueTickets()` must be idempotent by reservation ID.
- `transition()` should reject stale/out-of-order events.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| fare changes after search | reprice and ask user to accept |
| inventory sold out during checkout | hold fails and user selects another fare |
| payment succeeds after hold expiry | refund or revalidate before ticket |
| ticketing provider timeout | retry idempotently and reconcile |
| duplicate payment callback | return existing reservation state |
| multi-leg partial hold failure | release successful leg holds |
| duplicate ticket issue request | provider/client idempotency returns same ticket |
| user closes browser after payment | booking state visible by reservation lookup |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
airline/
  domain/
    Flight.java
    FareClassInventory.java
    FareQuote.java
    Reservation.java
    Ticket.java
    PaymentAttempt.java
  service/
    SearchService.java
    PricingService.java
    InventoryService.java
    ReservationService.java
    TicketingService.java
  port/
    InventoryRepository.java
    PaymentGateway.java
    AirlineProviderClient.java
    EventPublisher.java
  adapter/
    InMemoryInventoryRepository.java
  app/
    AirlineBookingDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock
from typing import Dict


class ReservationState(str, Enum):
    HELD = "HELD"
    PAID = "PAID"
    TICKETED = "TICKETED"
    EXPIRED = "EXPIRED"


@dataclass
class Inventory:
    available: int
    held: int = 0
    sold: int = 0


@dataclass
class Reservation:
    reservation_id: str
    flight_id: str
    count: int
    state: ReservationState


class InMemoryAirlineBooking:
    def __init__(self) -> None:
        self.inventory: Dict[str, Inventory] = {}
        self.reservations: Dict[str, Reservation] = {}
        self.lock = Lock()

    def add_inventory(self, flight_id: str, seats: int) -> None:
        self.inventory[flight_id] = Inventory(available=seats)

    def hold(self, flight_id: str, passenger_count: int) -> Reservation:
        with self.lock:
            inv = self.inventory[flight_id]
            if inv.available < passenger_count:
                raise ValueError("not enough inventory")
            inv.available -= passenger_count
            inv.held += passenger_count
            reservation_id = f"res-{len(self.reservations) + 1}"
            res = Reservation(reservation_id, flight_id, passenger_count, ReservationState.HELD)
            self.reservations[reservation_id] = res
            return res

    def mark_paid(self, reservation_id: str) -> Reservation:
        with self.lock:
            res = self.reservations[reservation_id]
            if res.state != ReservationState.HELD:
                raise ValueError("reservation is not payable")
            res.state = ReservationState.PAID
            return res

    def ticket(self, reservation_id: str) -> Reservation:
        with self.lock:
            res = self.reservations[reservation_id]
            if res.state != ReservationState.PAID:
                raise ValueError("reservation is not paid")
            inv = self.inventory[res.flight_id]
            inv.held -= res.count
            inv.sold += res.count
            res.state = ReservationState.TICKETED
            return res


app = InMemoryAirlineBooking()
app.add_inventory("fl-1", 2)
reservation = app.hold("fl-1", 2)
app.mark_paid(reservation.reservation_id)
ticketed = app.ticket(reservation.reservation_id)
print(ticketed.state)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[flightId -> Inventory]` | fare/seat inventory |
| `dict[reservationId -> Reservation]` | PNR lifecycle |
| `dict[idempotencyKey -> result]` | retries |
| `priorityQueue(expiresAt, reservationId)` | hold expiry |
| `list[ReservationEvent]` | audit/event stream |

## 4.4 Concurrency

High-signal concurrency issues:

- Many users hold same fare inventory.
- Multi-leg reservation partial failure.
- Payment callback races with expiry.
- Ticketing retry duplicates.

Handling strategy:

- Atomic conditional decrement/versioning.
- Saga with compensation for multi-leg holds.
- State machine transition checks.
- Idempotency keys for payment/ticketing.

## 4.5 Testing Thinking

Unit tests:

- Cannot hold more seats than available.
- Paid reservation can be ticketed.
- Unpaid reservation cannot be ticketed.
- Duplicate ticketing request is idempotent.
- Expired reservation releases inventory.

Load tests:

- Hot sale inventory contention.
- Search burst with stale cache.
- Payment callback storm.
- Provider ticketing timeout backlog.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Fare sale launch | airline promo | hot inventory conflicts |
| Holiday search burst | peak travel | search/cache overload |
| Payment provider delay | gateway slowness | holds expire while paid |
| GDS/airline outage | provider unavailable | ticketing backlog |
| Bot scraping | fare comparison abuse | search infrastructure pressure |

## 5.2 Immediate Spike Response

1. Protect inventory correctness.
2. Cache search/schedule aggressively.
3. Queue booking attempts for hot flights.
4. Rate-limit bots and repeated fare checks.
5. Apply backpressure to external airline/GDS calls.
6. Keep payment/ticketing recovery visible.
7. Reconcile stuck reservations.

## 5.3 Degradation Policy

Protect in this order:

1. Inventory correctness.
2. Paid reservation/ticketing recovery.
3. Booking hold creation.
4. Booking retrieval.
5. Search freshness.
6. Recommendations/promotions.

Not allowed:

- Sell beyond allowed inventory policy.
- Double-charge without recovery.
- Issue duplicate tickets.
- Hide stuck paid bookings from user/support.

## 5.4 Spike Interview Answer

> During spikes I protect inventory and paid booking recovery first. Search can be cached and stale, but reservation creation must revalidate inventory. Ticketing/provider failures are handled through an idempotent saga with retry, refund, and reconciliation.

---

# 6. Scaling To Global Travel

## 6.1 Global Architecture

```text
Global routing
  -> search caches by route/date
  -> inventory owners by airline/flight/date
  -> reservation/payment/ticketing workflow
  -> provider adapters for airline/GDS
  -> reconciliation and notification pipelines
```

## 6.2 Multi-Region Strategy

- Partition search by geography/route/date.
- Keep inventory writes for a flight in one owner shard/region.
- Replicate reservation reads globally.
- Use provider-specific regional adapters.
- Queue external calls and apply provider backpressure.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Search | route/date indexes and caches |
| Pricing | fare quote caches with TTL |
| Inventory | flight/fare-class strong partitions |
| Reservation | idempotent state machine |
| Payment | ledger and retry workflow |
| Ticketing | provider queues and adapters |
| Expiry | hold scanner/delay queue |
| Reconciliation | compare internal/provider states |

## 6.4 Global Interview Answer

> I would scale airline booking by separating cached search from strongly consistent reservation. Inventory is owned per flight/fare class; reservations use short-lived holds; payment and ticketing run as an idempotent saga with reconciliation for external provider failures.

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
I will clarify airline vs OTA, fare inventory vs seat selection, multi-leg, payment, ticketing, cancellation, and refunds.
I will estimate search QPS, booking QPS, hot flight conflicts, hold TTL, payment callback volume, and provider throughput.
HLD includes Search, Pricing, Inventory, Reservation/PNR, Payment, Ticketing, Expiry, Notification, and Reconciliation.
Search is cached and approximate; booking revalidates.
Inventory hold is atomic per flight/fare class.
Payment and ticketing are idempotent saga steps.
Provider failures require retry, refund, and reconciliation.
```

---

# 8. Fast Recall Rules

- Search is not booking truth.
- Fare quote has TTL.
- Inventory hold comes before payment/ticket.
- Fare inventory and seat assignment are different.
- Use atomic decrement/CAS for fare class inventory.
- Multi-leg booking needs saga and compensation.
- Payment success does not guarantee ticket until ticketing succeeds.
- Duplicate callbacks are normal.
- Ticketing must be idempotent.
- Stuck paid bookings need reconciliation/refund workflow.
