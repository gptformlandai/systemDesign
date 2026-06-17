# BookMyShow - End-to-End System Design

> Goal: practice one complete E2E seat-booking problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for high-concurrency inventory reservation systems.
- Start broad with requirements and scale, then zoom into events, shows, seat maps, reservations, locking, payment, expiry, consistency, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For BookMyShow-style systems, optimize seat inventory correctness, fast seat-map reads, high-concurrency reservation safety, payment reliability, and graceful handling of flash-sale spikes.

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

| Layer | Interview signal | BookMyShow focus |
|---|---|---|
| Problem understanding | Can clarify booking lifecycle | browse events, showtimes, seat map, hold seats, pay, confirm/cancel |
| HLD | Can design inventory systems | catalog, search, seat inventory, reservation service, payment, expiry workers |
| LLD | Can model maintainable components | `Event`, `Venue`, `Show`, `Seat`, `SeatHold`, `Booking`, `PaymentAttempt` |
| Machine coding | Can implement critical path | lock seats, expire holds, confirm booking, prevent double booking |
| Traffic spikes | Can protect production | blockbuster release, presale launch, payment gateway delay, bot traffic |
| Global scale | Can reason across regions | city partitioning, show-level hot partitions, read caches, queueing |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can search/browse movies/events by city, date, venue, and language.
- Users can view available shows and seat maps.
- Users can select seats.
- System temporarily holds selected seats for a short time.
- User can pay within hold window.
- Booking is confirmed after successful payment.
- Holds expire automatically if payment is not completed.
- Users receive tickets/booking confirmation.
- Admins/partners can create venues, screens, seat layouts, and showtimes.

Optional requirements to clarify:

- Are events/movies/concerts all in scope or only cinema?
- Is dynamic pricing in scope?
- Are cancellation/refunds in scope?
- Do we support coupons/wallet/loyalty?
- Are waitlists in scope?
- Do we need fraud/bot prevention?

Out of scope unless interviewer asks:

- Full payment provider internals.
- Full recommendation system.
- Full partner/admin operations portal.
- Full ticket scanning/check-in system.

## 1.2 Non-Functional Requirements

Inventory correctness:

- No double booking of a seat.
- Seat hold/booking state must be consistent.
- Payment success must map to exactly one confirmed booking.

Performance:

- Fast city/show browse.
- Fast seat-map reads.
- Low-latency seat hold attempt, especially during spikes.

Reliability:

- Holds expire reliably.
- Payment callbacks are idempotent.
- Booking confirmation survives retries and provider timeouts.

## 1.3 Constraints

- Seat inventory has high contention during popular releases.
- Seat maps are read-heavy and cacheable, but availability changes frequently.
- Payment can be slow or fail after seats are held.
- Users may abandon checkout.
- Bots can hammer seat holds.
- Strong consistency is needed per show/seat, not globally.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Cities | thousands |
| Venues | 100K+ |
| Shows/day | millions |
| Peak browse QPS | millions/sec during releases |
| Peak seat hold QPS | 100K+/sec globally |
| Hot show concurrency | 10K-100K users for one show |
| Seats per screen/show | 100-500 typical |
| Hold TTL | 5-10 minutes |
| Booking API availability | 99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- Seat-map reads can be 100x-1000x higher than actual booking writes.
- One hot show with 300 seats and 50K users is mostly contention; the system must reject quickly and safely.
- Holds create temporary inventory writes and expiry events.
- Payment callbacks may arrive late, duplicated, or out of order.
- Strong consistency scope is small: `(showId, seatId)`.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Hold TTL | 5-10 minutes |
| Seat map cache TTL | seconds, with invalidation/events |
| Booking confirmation latency | seconds, payment-dependent |
| Seat hold target | p95 under 100-300 ms |
| Payment callback retry window | hours to days |
| Hot show queue wait | seconds to minutes if throttled |

## 1.6 Clarifying Questions To Ask

- Is seat selection required, or can the system auto-assign seats?
- What is the hold duration?
- Can a user hold seats across multiple shows?
- What happens if payment succeeds after hold expiry?
- Do we need cancellation/refund?
- Are prices fixed per show or dynamic per demand/seat?

Strong interview framing:

> I will design BookMyShow around a per-show inventory service. Browse/search is read-heavy and cacheable, but seat hold and booking confirmation require strict per-seat concurrency control. Payment uses idempotent callbacks and the hold expiry workflow releases abandoned seats.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Browse flow:
Client -> Catalog/Search Service -> city/date/event/show listings
       -> Cache/Search Index -> Show metadata

Seat map flow:
Client -> Seat Map Service
       -> Layout Store + Seat Inventory Store
       -> availability view

Booking flow:
Client -> Reservation Service: hold seats
       -> Seat Inventory lock/CAS
       -> SeatHold created with TTL
       -> Payment Service
       -> Payment callback
       -> Booking Service confirms seats
       -> Ticket/Notification
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
            +--------------------+---------------------+
            |                    |                     |
            v                    v                     v
+----------------+      +----------------+      +----------------+
| Catalog/Search |      | Seat Map Svc   |      | Reservation Svc|
| events/shows   |      | layout+state   |      | hold seats     |
+-------+--------+      +-------+--------+      +-------+--------+
        |                       |                       |
        v                       v                       v
+----------------+      +----------------+      +----------------+
| Search/Cache   |      | Seat Inventory |      | Hold Store     |
+----------------+      | Store          |      | TTL/expiry     |
                        +-------+--------+      +-------+--------+
                                |                       |
                                v                       v
                        +----------------+      +----------------+
                        | Booking Svc    |<---->| Payment Svc    |
                        +-------+--------+      +-------+--------+
                                |                       |
                                v                       v
                        +----------------+      +----------------+
                        | Ticket/Notify  |      | Payment Gateway|
                        +----------------+      +----------------+
```

Request flow for booking:

1. User selects seats for a show.
2. Reservation Service validates user, show, and seat IDs.
3. Seat Inventory atomically holds each requested seat if available.
4. Hold record is created with expiry time.
5. Client starts payment using `bookingIntentId/holdId`.
6. Payment callback arrives with success/failure.
7. Booking Service validates hold is active and confirms seats.
8. Seat state changes from `HELD` to `BOOKED`.
9. Ticket is generated and notification is sent.
10. Expiry worker releases holds that were not paid.

## 2.2 APIs

### Search Shows

```http
GET /v1/cities/{cityId}/shows?movieId=m-1&date=2026-06-17
```

### Get Seat Map

```http
GET /v1/shows/{showId}/seat-map
Authorization: Bearer <token>
```

Response:

```json
{
  "showId": "show-1",
  "seats": [
    {"seatId": "A1", "row": "A", "number": 1, "status": "AVAILABLE", "price": 250},
    {"seatId": "A2", "row": "A", "number": 2, "status": "HELD", "price": 250}
  ]
}
```

### Hold Seats

```http
POST /v1/shows/{showId}/holds
Idempotency-Key: hold-abc
```

```json
{
  "userId": "u-1",
  "seatIds": ["A1", "A2"]
}
```

Response:

```json
{
  "holdId": "hold-1",
  "showId": "show-1",
  "seatIds": ["A1", "A2"],
  "amount": 500,
  "expiresAt": "2026-06-17T12:10:00Z"
}
```

### Confirm Payment Callback

```http
POST /v1/payments/callback
```

```json
{
  "paymentId": "pay-1",
  "holdId": "hold-1",
  "status": "SUCCESS",
  "providerReference": "pg-123"
}
```

Important API points:

- Hold seats is idempotent.
- Payment callbacks are idempotent.
- Seat map status can be slightly stale, but hold attempt must be strongly checked.
- Confirm booking must verify hold ownership and expiry.

## 2.3 Core Components

Think of BookMyShow as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Catalog plane | movies/events, venues, shows | fast discovery |
| Inventory plane | seats, availability, holds, bookings | no double booking |
| Transaction plane | hold, pay, confirm, expire | correct booking lifecycle |
| Payment plane | provider interactions, callbacks, refunds | money correctness |
| Notification plane | tickets, email/SMS/push | user confirmation |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Catalog Service | events, venues, shows | live seat locks | browse QPS |
| Seat Map Service | layout + availability view | payment confirmation | seat-map reads |
| Reservation Service | hold request workflow | payment provider calls | hold QPS |
| Seat Inventory Store | per-seat state | search ranking | seat contention |
| Booking Service | confirmed booking and ticket | seat-map rendering | confirmations |
| Payment Service | payment attempts/callbacks | seat lock algorithm | payment events |
| Expiry Worker | expired hold release | user search | hold volume |
| Notification Service | tickets/confirmations | source of truth | notification volume |

### Seat Inventory And Locking

Core invariant:

> A seat for a show can be `AVAILABLE`, `HELD`, or `BOOKED`. At most one non-expired hold or booking can own it at a time.

Locking options:

| Option | How | Trade-off |
|---|---|---|
| DB row lock | `SELECT FOR UPDATE` seats | strong but can bottleneck hot shows |
| Optimistic version | update if `version` and state match | scalable but needs retries |
| Redis/distributed lock | lock seat key with TTL | fast but must reconcile with DB truth |
| Single-writer actor per show | serialize seat commands | great for hot shows but operationally complex |

Recommended interview approach:

- Use atomic conditional update on seat rows or per-show inventory partition.
- For small seat sets, transactionally update all requested seats.
- If any seat fails, release acquired holds and return conflict.
- Store hold with TTL and expiry event.

### Reservation And Hold Flow

Steps:

1. Validate show is active and seats exist.
2. Attempt atomic hold for all requested seats.
3. Create `SeatHold` record with expiry.
4. Publish `hold.created`.
5. Client proceeds to payment.
6. Expiry worker releases unpaid hold.

Failure behavior:

- Client retry with same idempotency key returns same hold if still valid.
- If partial hold fails, release partial changes.
- If hold creation succeeds but response lost, retry fetches existing hold.

### Booking Confirmation

Rules:

- Payment success does not automatically mean booking if hold expired.
- Booking confirm must validate hold state, user, amount, and seat states.
- Confirm operation must be idempotent by `holdId/paymentId`.
- Once booked, seats never return to available unless cancellation/refund policy allows.

Failure behavior:

- Payment success but confirm fails due to expired hold: refund or manual reconciliation.
- Booking confirmed but notification lost: user can fetch booking from account.
- Duplicate callback returns same booking.

### Expiry Worker

Why it exists:

- Users abandon checkout.
- Held seats must return to available.

Implementation patterns:

- Delay queue with expiry timestamp.
- Periodic scanner by `expiresAt`.
- TTL event stream if infrastructure supports it.

Important details:

- Expiry is idempotent.
- Only release seats still owned by that hold.
- Do not release seats already booked.

## 2.4 Data Layer

### Core Data Models

Show:

```json
{
  "showId": "show-1",
  "eventId": "movie-1",
  "venueId": "venue-1",
  "screenId": "screen-1",
  "startsAt": "2026-06-17T18:00:00Z",
  "state": "ACTIVE"
}
```

Seat inventory:

```json
{
  "showId": "show-1",
  "seatId": "A1",
  "status": "HELD",
  "holdId": "hold-1",
  "bookingId": null,
  "version": 7,
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

Seat hold:

```json
{
  "holdId": "hold-1",
  "showId": "show-1",
  "userId": "u-1",
  "seatIds": ["A1", "A2"],
  "state": "ACTIVE",
  "expiresAt": "2026-06-17T12:10:00Z"
}
```

Booking:

```json
{
  "bookingId": "book-1",
  "holdId": "hold-1",
  "userId": "u-1",
  "showId": "show-1",
  "seatIds": ["A1", "A2"],
  "state": "CONFIRMED",
  "amount": 500
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Catalog/shows | relational/document DB + cache | structured browse |
| Seat layout | relational/document DB | stable per screen/show |
| Seat inventory | relational/strong KV | per-seat consistency |
| Holds | relational/KV with expiry index | active reservation lifecycle |
| Bookings | relational DB | durable user purchase |
| Payments | relational ledger | money auditability |
| Events | stream | expiry, notifications, analytics |

Relational-style tables:

```sql
shows(show_id PK, event_id, venue_id, screen_id, starts_at, state)
seat_inventory(show_id, seat_id, status, hold_id, booking_id, version, updated_at)
seat_holds(hold_id PK, show_id, user_id, state, expires_at, amount)
hold_seats(hold_id, show_id, seat_id)
bookings(booking_id PK, hold_id, user_id, show_id, state, amount, created_at)
payments(payment_id PK, hold_id, booking_id, state, provider_ref, amount)
```

Important indexes:

- `seat_inventory(show_id, seat_id)` for atomic hold.
- `seat_holds(expires_at, state)` for expiry scanner.
- `bookings(user_id, created_at DESC)` for user history.
- `payments(provider_ref)` for callback idempotency.

### Partitioning

- Partition catalog by city/date.
- Partition seat inventory by `showId`.
- Keep hot shows isolated or single-writer where needed.
- Partition bookings by user/time or show/time.
- Payment ledger partitioned by payment ID/booking ID.

### Replication And Consistency

- Seat inventory requires strong consistency per show/seat.
- Catalog/search can be eventually consistent.
- Seat-map reads can be slightly stale.
- Payment and booking require idempotent durable state.

## 2.5 Scalability

### Horizontal Scaling

- Catalog Service scales by browse QPS.
- Seat Map Service scales by read QPS and cache.
- Reservation Service scales by hold QPS.
- Inventory partitions scale by show/city.
- Expiry workers scale by active holds.

### Hot Show Strategy

- Virtual waiting room or queue.
- Rate-limit seat-map refreshes.
- Use per-show single-writer or partitioned inventory actor.
- Cache seat map layout separately from availability.
- Reject conflicts quickly.
- Bot detection and CAPTCHA/risk checks.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Search/browse | 50-200 ms |
| Seat map read | 50-300 ms |
| Hold seats | 100-300 ms |
| Payment redirect/callback | payment-dependent |
| Confirm booking | 100-500 ms |
| Ticket notification | seconds |

### Optimization Rules

- Cache show listings and static layouts.
- Do not cache final hold decision.
- Use atomic conditional updates for seats.
- Keep hold TTL short.
- Batch availability updates to clients.
- Use waiting room for extreme hot shows.

## 2.7 Async Systems

Use streams for:

- hold created
- hold expired
- booking confirmed
- payment succeeded/failed
- ticket generated
- notification requested
- inventory changed
- fraud/risk events

Queue notes:

- Consumers are idempotent.
- Expiry workers check ownership before release.
- Payment callback and booking confirmation use outbox/ledger.
- Notifications are not source of truth.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated booking APIs.
- Payment tokens handled by payment provider/token vault.
- Signed ticket/barcode generation.
- Authorization for user booking history.

Privacy:

- User booking history is sensitive.
- Do not expose other users holding seats.
- Seat hold status can be public, but hold owner is private.

Abuse controls:

- Rate-limit seat holds and refreshes.
- Bot detection for hot shows.
- Limit active holds per user/payment method.
- Risk checks for coupons and high-demand events.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Inventory | double-book prevention, hold conflict rate, hold latency |
| Booking | confirmation success, booking failure reason |
| Expiry | expired hold release lag, stuck held seats |
| Payment | callback latency, duplicate callbacks, refund queue |
| Browse | search latency, seat-map latency, cache hit rate |
| Abuse | hold rate-limits, bot score distribution |

Alerts:

- Seat double-book guard triggers.
- Held seats do not expire on time.
- Payment success but booking confirmation failures rise.
- Hot show inventory partition saturates.
- Seat-map error rate spikes.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Seat locking | DB row locks | optimistic CAS | simple correctness vs higher throughput |
| Hot show | waiting room | open flood | fairness/protection vs friction |
| Seat map | cache availability | live read every time | low latency vs staleness |
| Hold TTL | short | long | inventory turnover vs checkout comfort |
| Payment | hold then pay | pay then allocate | lower payment waste vs inventory lock contention |
| Expiry | delay queue | scanner | timely release vs operational simplicity |

Interview framing:

> I would cache browse and seat-map reads, but never trust cache for booking. The hold operation uses atomic per-seat updates, payment confirmation is idempotent, and expiry workers release abandoned holds safely.

---

# 3. Low-Level Design

LLD goal:

> Model BookMyShow around shows, seats, seat inventory, temporary holds, confirmed bookings, payment attempts, and expiry.

Simple rules:

- Seat map cache is not inventory truth.
- A seat can be booked once.
- Holds expire and must be idempotently released.
- Payment callback must not double-confirm.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Event` | movie/concert metadata | active event can have shows |
| `Venue` | address/screens/layouts | screen layout is stable |
| `Show` | event+venue+time | owns seat inventory scope |
| `Seat` | row/number/category | layout identity |
| `SeatInventory` | status/version/owner | no double active owner |
| `SeatHold` | user seats and expiry | active until confirmed/expired |
| `Booking` | confirmed purchase | created once per hold |
| `PaymentAttempt` | provider transaction | idempotent callback processing |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `CatalogService` | browse/search shows | lock seats |
| `SeatMapService` | render availability | confirm payment |
| `ReservationService` | create/release holds | generate ticket alone |
| `BookingService` | confirm booking | own provider payment API |
| `PaymentService` | payment attempts/callbacks | mutate seat state without booking rules |
| `ExpiryService` | release expired holds | book seats |

## 3.2 OOP Fundamentals

Encapsulation:

- `SeatInventory` owns allowed status transitions.
- `SeatHold` owns expiry and ownership checks.
- `Booking` owns confirmation state.

Abstraction:

- `InventoryRepository` hides DB/CAS details.
- `PaymentGateway` hides provider APIs.
- `TicketGenerator` hides ticket/barcode format.

Polymorphism:

- Different pricing strategies by event/seat category.
- Different inventory strategies: row lock, optimistic CAS, single-writer.

Composition:

- `BookingService` composes hold repository, inventory repository, payment service, ticket service, and event publisher.

## 3.3 SOLID Principles

| Principle | BookMyShow application |
|---|---|
| Single Responsibility | `ExpiryService` only expires holds |
| Open/Closed | add pricing category without rewriting hold logic |
| Liskov Substitution | any `InventoryLockStrategy` preserves no-double-booking |
| Interface Segregation | separate catalog, inventory, booking, payment APIs |
| Dependency Inversion | services depend on repositories/gateways interfaces |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| State | seat/hold/booking lifecycle | explicit valid transitions |
| Command | hold seats and confirm booking | idempotent operations |
| Strategy | inventory locking and pricing | swap based on hotness/event |
| Saga | hold -> payment -> booking -> ticket | recover multi-step flow |
| Observer/Event Publisher | inventory/payment events | notifications and cache invalidation |

## 3.5 UML / Diagrams

### Hold Sequence

```text
Client -> ReservationService: holdSeats(showId, seats)
ReservationService -> InventoryRepository: atomic hold if AVAILABLE
ReservationService -> HoldRepository: create hold with expiry
ReservationService -> EventStream: hold.created
ReservationService -> Client: holdId + expiry
```

### Payment Confirmation Sequence

```text
PaymentProvider -> PaymentService: callback
PaymentService -> PaymentRepository: record idempotently
PaymentService -> BookingService: confirm(holdId)
BookingService -> HoldRepository: validate active hold
BookingService -> InventoryRepository: HELD -> BOOKED if owner holdId
BookingService -> TicketService: generate ticket
BookingService -> EventStream: booking.confirmed
```

## 3.6 Class Design

Interfaces:

```java
interface InventoryRepository {
    boolean holdSeats(String showId, List<String> seatIds, String holdId, Instant expiresAt);
    boolean confirmHeldSeats(String showId, List<String> seatIds, String holdId, String bookingId);
    void releaseHold(String showId, List<String> seatIds, String holdId);
}

interface HoldRepository {
    SeatHold create(String userId, String showId, List<String> seatIds, Money amount);
    Optional<SeatHold> get(String holdId);
    void markExpired(String holdId);
}

interface PaymentService {
    PaymentResult processCallback(PaymentCallback callback);
}
```

Design notes:

- `confirmHeldSeats()` must check seat owner is still `holdId`.
- `releaseHold()` must not release already booked seats.
- Hold and payment callbacks need idempotency keys.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| two users hold same seat | one atomic update wins |
| user retries hold request | same hold returned if active |
| payment succeeds after hold expiry | refund or manual reconciliation |
| expiry worker races with payment callback | compare hold state and seat owner transactionally |
| partial seat hold failure | release partial and return conflict |
| notification lost | user can fetch booking |
| seat map stale | hold attempt performs fresh consistency check |
| duplicate payment callback | idempotent booking confirmation |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
bookmyshow/
  domain/
    Event.java
    Show.java
    SeatInventory.java
    SeatHold.java
    Booking.java
    PaymentAttempt.java
  service/
    CatalogService.java
    ReservationService.java
    BookingService.java
    ExpiryService.java
  port/
    InventoryRepository.java
    HoldRepository.java
    PaymentGateway.java
    TicketGenerator.java
  adapter/
    InMemoryInventoryRepository.java
  app/
    BookMyShowDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock
from time import time
from typing import Dict, List


class SeatStatus(str, Enum):
    AVAILABLE = "AVAILABLE"
    HELD = "HELD"
    BOOKED = "BOOKED"


@dataclass
class SeatState:
    status: SeatStatus
    owner_id: str | None = None
    expires_at: float | None = None


class InMemoryBookMyShow:
    def __init__(self, seats: List[str]) -> None:
        self.seats: Dict[str, SeatState] = {s: SeatState(SeatStatus.AVAILABLE) for s in seats}
        self.bookings: Dict[str, List[str]] = {}
        self.lock = Lock()

    def hold(self, user_id: str, seat_ids: List[str], hold_id: str, ttl_seconds: int = 300) -> None:
        with self.lock:
            now = time()
            self._expire_locked(now)
            if any(self.seats[s].status != SeatStatus.AVAILABLE for s in seat_ids):
                raise ValueError("one or more seats unavailable")
            expires_at = now + ttl_seconds
            for seat_id in seat_ids:
                self.seats[seat_id] = SeatState(SeatStatus.HELD, hold_id, expires_at)

    def confirm(self, hold_id: str, booking_id: str) -> None:
        with self.lock:
            held = [sid for sid, state in self.seats.items() if state.owner_id == hold_id]
            if not held:
                raise ValueError("hold not found or expired")
            now = time()
            for seat_id in held:
                state = self.seats[seat_id]
                if state.status != SeatStatus.HELD or state.expires_at < now:
                    raise ValueError("hold expired")
            for seat_id in held:
                self.seats[seat_id] = SeatState(SeatStatus.BOOKED, booking_id, None)
            self.bookings[booking_id] = held

    def _expire_locked(self, now: float) -> None:
        for seat_id, state in self.seats.items():
            if state.status == SeatStatus.HELD and state.expires_at is not None and state.expires_at < now:
                self.seats[seat_id] = SeatState(SeatStatus.AVAILABLE)


app = InMemoryBookMyShow(["A1", "A2", "A3"])
app.hold("u1", ["A1", "A2"], "hold-1")
app.confirm("hold-1", "book-1")
print(app.seats["A1"].status)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[seatId -> SeatState]` | inventory state |
| `dict[holdId -> SeatHold]` | active holds |
| `dict[bookingId -> Booking]` | confirmed bookings |
| `priorityQueue(expiresAt, holdId)` | hold expiry |
| `dict[idempotencyKey -> result]` | retry dedup |

## 4.4 Concurrency

High-signal concurrency issues:

- Two users try same seat.
- Expiry races with payment success.
- Duplicate payment callbacks.
- Partial failure while holding multiple seats.

Handling strategy:

- Atomic conditional seat updates.
- Transaction around hold/confirm.
- Idempotency for hold and payment.
- Owner check when releasing/confirming seats.

## 4.5 Testing Thinking

Unit tests:

- Same seat cannot be held twice.
- Hold expires and seat becomes available.
- Confirm converts held seats to booked.
- Expiry does not release booked seats.
- Duplicate payment callback returns same booking.

Load tests:

- Hot show with many users selecting same seats.
- Payment callback storm.
- Expiry worker backlog.
- Seat-map refresh flood.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Blockbuster release | tickets open at midnight | hot show inventory contention |
| Bot traffic | scripts hold seats | unfair inventory capture |
| Payment gateway slowness | checkout delays | holds expire while users pay |
| Seat-map refresh storm | users repeatedly refresh | cache/backend pressure |
| Expiry backlog | many abandoned holds | seats stuck as held |

## 5.2 Immediate Spike Response

1. Protect inventory correctness.
2. Use waiting room/rate limits for hot shows.
3. Cache static layout and throttle seat-map refresh.
4. Reject unavailable seats quickly.
5. Scale per-show inventory workers.
6. Prioritize expiry of hot-show holds.
7. Queue payment reconciliation safely.

## 5.3 Degradation Policy

Protect in this order:

1. No double booking.
2. Booking confirmation correctness.
3. Hold/release workflow.
4. Seat-map freshness.
5. Search/browse personalization.
6. Notifications.

Not allowed:

- Sell the same seat twice.
- Confirm booking without successful payment.
- Release a booked seat due to stale expiry event.
- Leak payment or user booking details.

## 5.4 Spike Interview Answer

> During spikes I protect the seat inventory service first. Seat maps can be stale and users can wait in a queue, but hold/confirm must use atomic conditional updates. Payment callbacks and expiry workers are idempotent and reconcile safely.

---

# 6. Scaling To Many Cities And Events

## 6.1 Global Architecture

```text
Global/city routing
  -> catalog/search per city
  -> show-level inventory partitions
  -> reservation and payment workflows
  -> expiry workers and notification pipelines
```

## 6.2 Multi-Region Strategy

- Partition catalog by city.
- Keep a show inventory owner region/shard.
- Avoid active-active writes for the same show seats.
- Replicate booking data for reads and disaster recovery.
- Use waiting room for globally hot events.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Catalog | search/cache by city/date |
| Seat map | cached layout + live availability |
| Inventory | show-partitioned strong writes |
| Reservation | stateless API plus inventory CAS |
| Expiry | delay queue/scanner by expiry time |
| Payment | idempotent ledger and callbacks |
| Notification | async ticket delivery |
| Abuse | rate limits and bot defenses |

## 6.4 Global Interview Answer

> I would scale BookMyShow by making browse/search read-heavy and cacheable, while isolating each show inventory into a strongly controlled write path. The core correctness boundary is the atomic seat hold/confirm operation.

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
I will clarify event type, seat selection, hold TTL, payment, cancellation, refunds, and dynamic pricing.
I will estimate browse QPS, seat-map QPS, hold QPS, hot-show concurrency, and payment callback volume.
HLD includes Catalog/Search, Seat Map, Reservation, Inventory, Payment, Booking, Expiry, Ticket, and Notification services.
I cache browse and layout, but not the booking decision.
Seat hold uses atomic conditional update or a per-show single-writer.
Payment and booking confirmation are idempotent.
Expiry releases only seats still owned by the hold.
```

---

# 8. Fast Recall Rules

- Seat inventory correctness is the heart of the system.
- Seat map can be stale; hold cannot.
- A seat is available, held, or booked.
- Hold has TTL and owner.
- Confirm only if hold is active and payment succeeded.
- Expiry must check ownership.
- Payment callbacks are duplicated and delayed in real life.
- Hot shows need queueing/rate limits.
- Strong consistency scope is `(showId, seatId)`.
- Never sell the same seat twice.
