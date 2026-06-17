# Uber / Ola - End-to-End System Design

> Goal: practice one complete E2E ride-hailing problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for geo + matching + transaction systems.
- Start broad with requirements and scale, then zoom into location ingestion, geo queries, driver matching, trip state machines, pricing, payments, consistency, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Uber/Ola-style systems, optimize low-latency matching, high-frequency location updates, correct trip state transitions, fair driver assignment, and payment/order reliability.

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

| Layer | Interview signal | Ride-hailing focus |
|---|---|---|
| Problem understanding | Can clarify scope and lifecycle | rider request, driver location, matching, trip, cancellation, payment |
| HLD | Can design geo and transactional systems | location service, geo index, matching service, trip service, pricing, payment, notification |
| LLD | Can model maintainable components | `Driver`, `Rider`, `Trip`, `GeoCell`, `MatchOffer`, `TripStateMachine` |
| Machine coding | Can implement critical path | nearby driver query, offer lock, accept, trip state transition |
| Traffic spikes | Can protect production | airport rush, concert exit, rain surge, driver reconnect storm |
| Global scale | Can reason across regions | city partitioning, geo sharding, regional matching, dispatch fairness |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Riders can request a ride from pickup to destination.
- Drivers publish live location and availability.
- System finds nearby eligible drivers.
- System sends offers to drivers and handles accept/decline/timeout.
- Once accepted, a trip is created and tracked through lifecycle states.
- Riders and drivers receive real-time trip updates.
- Fare estimate and final fare are calculated.
- Payment is charged after trip completion.
- Support cancellation and refunds/fees.

Optional requirements to clarify:

- Are pooling/shared rides in scope?
- Do we support scheduled rides?
- Do we support multiple vehicle types?
- Is driver ETA/routing accuracy in scope?
- Are cash payments in scope?
- Are surge pricing and incentives in scope?
- Do we need fraud/risk checks?

Out of scope unless interviewer asks:

- Full maps/routing engine internals.
- Full payment processor integration details.
- Full driver onboarding/compliance system.
- Full ML demand prediction system.

## 1.2 Non-Functional Requirements

Matching:

- Low-latency driver discovery and offer flow.
- Avoid assigning one driver to multiple trips.
- Handle high concurrency around popular pickup areas.
- Fair matching and bounded offer timeouts.

Location:

- High-throughput location ingestion.
- Fresh but not perfectly consistent driver locations.
- Efficient nearby driver search.

Trip correctness:

- Trip state transitions must be valid.
- Payment should not double-charge.
- Cancellation and timeout behavior must be deterministic.

## 1.3 Constraints

- Driver locations are high-volume and ephemeral.
- Geo queries need locality-based partitioning.
- Matching has race conditions: multiple riders may target the same driver.
- Drivers can accept, decline, disconnect, or become stale.
- Exactly-once payment is hard; use idempotency.
- ETA/pricing can be approximate before trip completion.
- City-level regulations and pricing rules may vary.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 500 million |
| Active drivers/day | 5 million |
| Active riders/day | 50 million |
| Trips/day | 100 million |
| Driver location updates | every 2-5 seconds while online |
| Peak ride requests | 100K+/sec globally |
| Matching latency target | p95 under 1-2 seconds |
| Location freshness target | under 10 seconds |
| Trip API availability | 99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- `5M active drivers / 3 sec update interval` can produce `1.6M location updates/sec` globally.
- `100M trips/day` is about `1.1K trips/sec` average, with large city/event peaks.
- Matching may query multiple rings/cells per request and send several driver offers.
- Location writes are high-volume ephemeral writes; trip/payment writes are lower-volume but require stronger correctness.
- City or region partitioning is natural because pickup and driver matching are local.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Location update interval | 2-5 seconds |
| Driver location TTL | 10-30 seconds |
| Offer timeout | 5-15 seconds |
| Nearby search radius | 1-5 km, expanding rings |
| Trip state update latency | sub-second to few seconds |
| Payment idempotency TTL | days |

## 1.6 Clarifying Questions To Ask

- Are we designing rider booking only or full driver/rider lifecycle?
- What vehicle types and matching constraints matter?
- Do we need strict nearest-driver matching or approximate matching?
- How do drivers accept ride offers?
- What happens when two riders target the same driver?
- Are surge pricing and payment in scope?

Strong interview framing:

> I will design ride-hailing as a geo-partitioned realtime matching system with ephemeral driver location state and durable trip/payment state. Matching uses nearby driver queries plus short-lived offer locks; Trip Service owns the state machine; Payment uses idempotent transaction workflows.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Driver location flow:
Driver app -> Location Gateway -> Location Service
          -> Geo Index / TTL location store
          -> availability updates

Ride request flow:
Rider app -> Ride Request API
          -> Pricing/ETA estimate
          -> Matching Service
          -> nearby eligible drivers from Geo Index
          -> Offer Service locks candidate driver
          -> Driver accepts
          -> Trip Service creates trip
          -> Notification/Realtime updates

Trip completion flow:
Trip Service -> Fare Service -> Payment Service
             -> Receipt/Notification -> Analytics
```

Recommended architecture:

```text
Rider App                      Driver App
   |                              |
   v                              v
+-----------------------+   +----------------------+
| API Gateway + Auth    |   | Location Gateway     |
+-----------+-----------+   +----------+-----------+
            |                          |
            v                          v
+-----------------------+   +----------------------+
| Ride Request Service  |   | Location Service     |
+-----------+-----------+   +----------+-----------+
            |                          |
            v                          v
+-----------------------+   +----------------------+
| Matching Service      |<--| Geo Index / TTL KV   |
+-----------+-----------+   +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Offer/Lock Service    |------->| Driver Notification  |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Trip Service          |------->| Pricing/Fare Service |
+-----------+-----------+        +----------------------+
            |
            v
+-----------------------+        +----------------------+
| Payment Service       |<------>| Payment Provider     |
+-----------------------+        +----------------------+
```

Request flow for matching:

1. Rider submits pickup, destination, vehicle type, and payment method.
2. Ride Request Service validates rider and creates request with idempotency key.
3. Pricing Service returns fare estimate and surge multiplier.
4. Matching Service queries nearby available drivers from Geo Index.
5. Candidates are ranked by ETA, availability, vehicle type, driver score, and fairness.
6. Offer Service places short-lived lock/hold on one or more candidate drivers.
7. Driver receives offer and accepts before timeout.
8. Trip Service atomically creates trip and marks driver busy.
9. Rider and driver receive trip assignment events.

## 2.2 APIs And Events

### Driver Location Update

```http
POST /v1/drivers/{driverId}/location
Authorization: Bearer <token>
```

```json
{
  "lat": 12.9716,
  "lng": 77.5946,
  "heading": 120,
  "speedMps": 8.5,
  "availability": "AVAILABLE",
  "updatedAt": "2026-06-17T12:00:00Z"
}
```

### Request Ride

```http
POST /v1/rides/requests
Idempotency-Key: req-abc
```

```json
{
  "riderId": "r-1",
  "pickup": {"lat": 12.9716, "lng": 77.5946},
  "dropoff": {"lat": 12.9352, "lng": 77.6245},
  "vehicleType": "SEDAN",
  "paymentMethodId": "pm-1"
}
```

### Driver Offer Event

```json
{
  "type": "RIDE_OFFER",
  "offerId": "off-1",
  "requestId": "req-1",
  "pickupEtaSeconds": 240,
  "expiresAt": "2026-06-17T12:00:10Z"
}
```

### Accept Offer

```http
POST /v1/drivers/{driverId}/offers/{offerId}/accept
Idempotency-Key: accept-xyz
```

### Trip State Update

```http
POST /v1/trips/{tripId}/events
```

```json
{
  "eventType": "DRIVER_ARRIVED",
  "occurredAt": "2026-06-17T12:05:00Z"
}
```

Important API points:

- Ride request and offer accept need idempotency.
- Driver location writes are high-volume and ephemeral.
- Trip events should follow a strict state machine.
- Payment charge must be idempotent by trip ID.

## 2.3 Core Components

Think of ride-hailing as five connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Location plane | driver coordinates, availability, geo index | fresh nearby search |
| Matching plane | candidate selection and offer locks | assign one driver fairly and quickly |
| Trip plane | lifecycle, state machine, rider/driver updates | transaction correctness |
| Pricing/payment plane | estimates, final fare, charges/refunds | money correctness |
| Realtime plane | notifications, app updates, ETA changes | user experience |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Location Service | driver location and TTL availability | trip truth | location update QPS |
| Geo Index | cell -> available drivers | payment state | active drivers/cells |
| Matching Service | candidate ranking and search expansion | driver lock ownership | ride request QPS |
| Offer/Lock Service | short-lived driver offer holds | fare calculation | offer QPS |
| Trip Service | trip lifecycle and state transitions | live geo index | trip events |
| Pricing/Fare Service | estimate and final fare | driver availability | pricing QPS |
| Payment Service | charge/refund workflow | matching decisions | payment events |
| Notification Service | rider/driver messages | trip source of truth | notification volume |

### Location Service And Geo Index

Why it exists:

- Matching depends on fresh driver location.
- Exact geospatial scans are too expensive.

Core responsibilities:

- Ingest driver location updates.
- Store latest driver location with TTL.
- Maintain mapping from geo cell to available drivers.
- Remove stale/unavailable drivers.
- Support nearby search by cell expansion.

Geo strategies:

| Strategy | Idea | Notes |
|---|---|---|
| Geohash/S2/H3 cells | map lat/lng to cells | easy radius/ring queries |
| City partition | shard by city/region | natural locality |
| TTL KV store | latest driver state | stale drivers expire |
| In-memory index | hot city matching | fast but needs recovery path |

Failure behavior:

- Stale drivers should not be matched.
- Location store outage can degrade matching but should not corrupt trips.
- Driver app can resend latest location.

Interview signal:

> Location data is ephemeral; trip assignment is durable.

### Matching Service

Why it exists:

- A rider request must choose an eligible driver quickly.
- The nearest driver is not always the best driver.

Candidate flow:

1. Compute pickup cell.
2. Query current and neighboring cells.
3. Filter by availability, vehicle type, distance, stale location, driver state.
4. Rank by pickup ETA, fairness, acceptance probability, cancellation risk.
5. Send offer to best candidate or small batch.
6. Expand radius if no driver accepts.

Matching algorithms:

| Algorithm | Use | Trade-off |
|---|---|---|
| Greedy nearest | simple low latency | may be unfair/suboptimal globally |
| Ranked candidate list | practical production baseline | needs scoring and tuning |
| Batch matching | high-demand areas | better global efficiency but more latency |
| Auction/offer fanout | send to multiple drivers | faster accept but risk conflicts/noise |

Interview signal:

> Matching is usually approximate and local; correctness is enforced by driver locks and trip state, not by perfect geo ranking.

### Offer Locking And Concurrency

Problem:

- Many riders can match the same nearby driver.
- One driver must not accept two active trips.

Lock strategy:

- Use short-lived driver hold: `driverId -> offerId/requestId`.
- Lock TTL matches offer timeout.
- Accept operation uses compare-and-set on offer/driver state.
- Trip creation and driver busy transition are atomic in Trip Service or through a transactional outbox.

Concurrency options:

| Option | Fit | Risk |
|---|---|---|
| Pessimistic driver lock | simple correctness | lock contention/timeouts |
| Optimistic CAS on driver state | high throughput | retry logic needed |
| Queue per driver | serializes offers | more operational complexity |

Interview signal:

> The lock is short-lived and protects assignment only. The durable trip state is the final source of truth.

### Trip State Machine

States:

```text
REQUESTED -> MATCHING -> DRIVER_ASSIGNED -> DRIVER_ARRIVED -> IN_PROGRESS -> COMPLETED
          -> CANCELLED_BY_RIDER
          -> CANCELLED_BY_DRIVER
          -> EXPIRED_NO_DRIVER
```

Rules:

- Cannot start trip before driver assigned.
- Cannot complete trip before in progress.
- Cancellation fee depends on state and timing.
- Completed trip triggers fare finalization and payment.

Failure behavior:

- Duplicate state event is idempotent.
- Invalid transition is rejected.
- If payment fails after completion, trip remains completed and payment recovery continues asynchronously.

### Pricing And Payment

Pricing:

- Estimate uses distance/time, demand/supply, vehicle type, city rules.
- Final fare uses actual trip distance/time plus fees/taxes/promotions.

Payment:

- Pre-authorize if required.
- Charge on completion with idempotency key `tripId`.
- Refund/cancel fee as separate idempotent transactions.

Failure behavior:

- Payment provider timeout: retry with same idempotency key.
- Charge succeeds but ack lost: provider idempotency returns same result.
- Payment failure does not undo completed trip; mark payment pending/failed.

## 2.4 Data Layer

### Core Data Models

Driver location:

```json
{
  "driverId": "d-1",
  "cityId": "blr",
  "cellId": "cell-123",
  "lat": 12.9716,
  "lng": 77.5946,
  "availability": "AVAILABLE",
  "updatedAt": "2026-06-17T12:00:00Z",
  "ttlSeconds": 20
}
```

Ride request:

```json
{
  "requestId": "req-1",
  "riderId": "r-1",
  "pickup": {"lat": 12.9716, "lng": 77.5946},
  "dropoff": {"lat": 12.9352, "lng": 77.6245},
  "vehicleType": "SEDAN",
  "state": "MATCHING"
}
```

Trip:

```json
{
  "tripId": "trip-1",
  "requestId": "req-1",
  "riderId": "r-1",
  "driverId": "d-1",
  "state": "DRIVER_ASSIGNED",
  "createdAt": "2026-06-17T12:00:10Z"
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Driver location | in-memory geo/KV with TTL | high-write ephemeral |
| Driver availability | KV/CAS store | lock/assignment correctness |
| Ride requests | relational/document DB | request lifecycle |
| Trips | relational DB or strongly consistent store | state machine correctness |
| Offers | KV with TTL + durable event log | timeout and accept flow |
| Payments | relational ledger/payment DB | money correctness |
| Events | stream | async notifications/analytics |

Relational-style tables:

```sql
ride_requests(request_id PK, rider_id, pickup_cell, vehicle_type, state, created_at)
driver_state(driver_id PK, city_id, availability, active_trip_id, version, updated_at)
ride_offers(offer_id PK, request_id, driver_id, state, expires_at)
trips(trip_id PK, request_id, rider_id, driver_id, state, created_at, updated_at)
trip_events(trip_id, event_sequence, event_type, occurred_at)
payments(payment_id PK, trip_id, amount, state, provider_ref, idempotency_key)
```

Important indexes:

- `ride_requests(rider_id, created_at DESC)` for rider history.
- `trips(driver_id, state)` for active driver trip.
- `ride_offers(driver_id, state, expires_at)` for active locks.
- `payments(trip_id)` for charge lookup.

### Partitioning

- Partition location data by city and geo cell.
- Partition matching workers by city/zone.
- Partition trips by `tripId` or city/time bucket.
- Partition driver state by `driverId`.
- Keep payment ledger partitioned by `tripId` or payment account.

### Replication And Consistency

- Location can be eventually consistent and TTL-based.
- Driver assignment and trip state require strong consistency or CAS.
- Payment ledger requires idempotency and auditability.
- Cross-region matching is usually avoided; city is the natural boundary.

## 2.5 Scalability

### Horizontal Scaling

- Location ingestion scales by city/cell partitions.
- Matching Service scales by ride request QPS.
- Offer Service scales by active offer count and timeout scans.
- Trip Service scales by trip event QPS.
- Notification Service scales by rider/driver updates.

### Hot Zone Strategy

- Split dense geo cells dynamically.
- Use ring expansion with caps.
- Use batch matching in extreme demand areas.
- Rate-limit repeated rider requests.
- Use driver supply/demand forecasting for pre-positioning.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Ride request validation | 20-80 ms |
| Geo nearby query | 20-100 ms |
| Candidate ranking | 20-100 ms |
| Offer send + accept | 5-15 seconds user-dependent |
| Trip assignment commit | 50-200 ms |
| Realtime notification | 50-500 ms |

### Optimization Rules

- Keep location updates lightweight.
- Avoid expensive routing calls for every candidate; approximate first.
- Cache city/vehicle pricing rules.
- Use short-lived locks and fast offer expiry.
- Keep payment off the matching critical path except pre-checks.

## 2.7 Async Systems

Use streams for:

- driver location sampled events
- ride requested
- offer created/accepted/expired
- trip state changed
- fare finalized
- payment charged/failed
- notifications
- analytics and fraud signals

Queue notes:

- Consumers must be idempotent.
- Trip state changes should publish through outbox.
- Payment retries should use idempotency keys.
- Offer timeout workers must handle late accepts deterministically.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated rider/driver APIs.
- Driver can only update own location.
- Rider/driver can only access assigned trip details.
- Payment tokens are stored securely through payment provider/token vault.

Privacy:

- Driver/rider exact location is sensitive.
- Share location only during active trip/request.
- Retain location history according to policy.

Abuse controls:

- Rate-limit ride request spam.
- Detect fake driver GPS or emulator patterns.
- Detect cancellation abuse.
- Risk checks before matching or payment capture.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Location | update QPS, stale driver rate, geo query latency |
| Matching | match success rate, time to match, offer acceptance rate |
| Locks | lock contention, expired offers, double-assignment attempts |
| Trip | invalid transition count, assignment latency, cancellation rate |
| Payment | charge success, retry count, idempotency conflicts |
| Realtime | notification latency, driver/rider update failures |

Alerts:

- Match success rate drops in a city.
- Stale driver rate spikes.
- Double-assignment guard triggers.
- Payment failures spike.
- Offer expiry backlog grows.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Geo index | in-memory TTL index | durable geo DB | speed vs recovery complexity |
| Matching | greedy nearest | batch optimization | low latency vs global optimality |
| Driver lock | pessimistic TTL lock | optimistic CAS | simple safety vs retries/contention |
| Location consistency | eventual | strongly consistent | scalability vs freshness precision |
| Payment | sync charge in trip completion | async recovery workflow | immediate certainty vs availability |
| City partition | local matching | global matching | locality and scale vs cross-city complexity |

Interview framing:

> I would keep location ephemeral and approximate, but make driver assignment, trip state, and payment idempotent and strongly controlled. Matching can be probabilistic; trip creation cannot be.

---

# 3. Low-Level Design

LLD goal:

> Model ride-hailing around driver location, geo cells, ride requests, match offers, driver locks, trip state transitions, fare calculation, and payment idempotency.

Simple rules:

- Location is not trip truth.
- Driver assignment must be atomic.
- Trip state machine rejects invalid transitions.
- Payment uses idempotent charge/refund operations.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Driver` | driver identity and status | one active trip max |
| `Rider` | rider identity and payment profile | blocked riders cannot request |
| `DriverLocation` | latest lat/lng and TTL | stale locations are ignored |
| `RideRequest` | pickup/dropoff/request state | one terminal state |
| `MatchOffer` | driver hold and timeout | accepted once before expiry |
| `Trip` | assigned rider/driver and state | valid state transitions only |
| `Fare` | estimate/final amount | final fare tied to trip |
| `PaymentAttempt` | charge/refund state | idempotent by trip/payment key |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `LocationService` | location updates and geo index | own trip assignment |
| `MatchingService` | candidate search/ranking | charge payment |
| `OfferService` | short-lived driver holds | calculate final fare |
| `TripService` | trip state machine | query raw geo cells directly |
| `PricingService` | estimate/fare rules | lock drivers |
| `PaymentService` | charge/refund workflow | choose drivers |

## 3.2 OOP Fundamentals

Encapsulation:

- `Trip` owns valid state transitions.
- `MatchOffer` owns expiry/accept rules.
- `DriverState` owns availability transitions.

Abstraction:

- `GeoIndex` hides geohash/S2/H3 implementation.
- `LockStore` hides CAS/distributed lock details.
- `PaymentGateway` hides provider API.

Polymorphism:

- Different matching strategies: nearest, batch, premium, pool.
- Different pricing strategies by city/vehicle/time.

Composition:

- `RideRequestService` composes pricing, matching, offer, trip, and notification services.

## 3.3 SOLID Principles

| Principle | Ride-hailing application |
|---|---|
| Single Responsibility | `TripService` owns trip lifecycle only |
| Open/Closed | add vehicle type without rewriting matching core |
| Liskov Substitution | any `GeoIndex` supports update/query contract |
| Interface Segregation | separate location, matching, trip, payment APIs |
| Dependency Inversion | services depend on `LockStore`, not concrete Redis/DB |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| Strategy | matching and pricing | swap algorithm by city/context |
| State | trip lifecycle | enforce valid transitions |
| Command | ride request and accept offer | idempotent operations |
| Observer/Event Publisher | trip/payment events | decouple notifications/analytics |
| Saga | trip completion -> fare -> payment | multi-step transaction recovery |

## 3.5 UML / Diagrams

### Matching Sequence

```text
Rider -> RideRequestService: requestRide
RideRequestService -> PricingService: estimate
RideRequestService -> MatchingService: findCandidates
MatchingService -> GeoIndex: nearby drivers
MatchingService -> OfferService: createOffer/lock driver
OfferService -> NotificationService: send offer
Driver -> OfferService: accept
OfferService -> TripService: createTripIfDriverHeld
TripService -> EventStream: trip.assigned
```

### Trip Completion Sequence

```text
Driver -> TripService: completeTrip
TripService -> TripStateMachine: validate transition
TripService -> FareService: calculate final fare
TripService -> PaymentService: charge(tripId)
PaymentService -> Provider: idempotent charge
PaymentService -> EventStream: payment.succeeded/failed
```

## 3.6 Class Design

Interfaces:

```java
interface GeoIndex {
    void updateLocation(DriverLocation location);
    List<DriverCandidate> nearbyDrivers(Location pickup, VehicleType vehicleType, int radiusMeters);
}

interface DriverLockStore {
    boolean tryHoldDriver(String driverId, String offerId, Duration ttl);
    boolean confirmHold(String driverId, String offerId, String tripId);
    void release(String driverId, String offerId);
}

interface TripStateMachine {
    Trip transition(String tripId, TripEvent event);
}

interface PaymentGateway {
    PaymentResult charge(String idempotencyKey, Money amount, String paymentMethodId);
}
```

Design notes:

- `tryHoldDriver()` must be atomic.
- `confirmHold()` should fail if hold expired or changed.
- `transition()` rejects invalid state movement.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| two riders target same driver | driver lock/CAS allows one winner |
| driver accepts after offer expiry | reject and release/ignore |
| trip assigned but notification lost | app sync fetches active trip |
| driver location stale | exclude by TTL |
| rider retries request | idempotency returns same request |
| payment timeout | retry same idempotency key |
| driver app disconnects during trip | trip remains active; location updates degrade |
| invalid trip transition | reject and log |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
ridehailing/
  domain/
    Driver.java
    DriverLocation.java
    RideRequest.java
    MatchOffer.java
    Trip.java
    Fare.java
  service/
    LocationService.java
    MatchingService.java
    OfferService.java
    TripService.java
    PaymentService.java
  port/
    GeoIndex.java
    DriverLockStore.java
    TripRepository.java
    PaymentGateway.java
  adapter/
    InMemoryGeoIndex.java
    InMemoryDriverLockStore.java
  app/
    RideHailingDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from enum import Enum
from math import hypot
from threading import Lock
from typing import Dict, Optional


class TripState(str, Enum):
    ASSIGNED = "ASSIGNED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


@dataclass
class DriverLocation:
    driver_id: str
    lat: float
    lng: float
    available: bool = True


@dataclass
class Trip:
    trip_id: str
    rider_id: str
    driver_id: str
    state: TripState


class InMemoryRideHailing:
    def __init__(self) -> None:
        self.locations: Dict[str, DriverLocation] = {}
        self.driver_hold: Dict[str, str] = {}
        self.trips: Dict[str, Trip] = {}
        self.lock = Lock()

    def update_driver(self, location: DriverLocation) -> None:
        with self.lock:
            self.locations[location.driver_id] = location

    def find_nearest(self, pickup_lat: float, pickup_lng: float) -> Optional[str]:
        best_driver = None
        best_distance = float("inf")
        for loc in self.locations.values():
            if not loc.available or loc.driver_id in self.driver_hold:
                continue
            dist = hypot(loc.lat - pickup_lat, loc.lng - pickup_lng)
            if dist < best_distance:
                best_distance = dist
                best_driver = loc.driver_id
        return best_driver

    def request_ride(self, rider_id: str, pickup_lat: float, pickup_lng: float) -> Trip:
        with self.lock:
            driver_id = self.find_nearest(pickup_lat, pickup_lng)
            if driver_id is None:
                raise ValueError("no driver available")
            offer_id = f"offer-{rider_id}-{driver_id}"
            self.driver_hold[driver_id] = offer_id
            trip_id = f"trip-{len(self.trips) + 1}"
            self.locations[driver_id].available = False
            trip = Trip(trip_id, rider_id, driver_id, TripState.ASSIGNED)
            self.trips[trip_id] = trip
            return trip

    def transition(self, trip_id: str, new_state: TripState) -> Trip:
        with self.lock:
            trip = self.trips[trip_id]
            allowed = {
                TripState.ASSIGNED: {TripState.IN_PROGRESS, TripState.CANCELLED},
                TripState.IN_PROGRESS: {TripState.COMPLETED, TripState.CANCELLED},
            }
            if new_state not in allowed.get(trip.state, set()):
                raise ValueError("invalid trip transition")
            trip.state = new_state
            return trip


app = InMemoryRideHailing()
app.update_driver(DriverLocation("d1", 12.97, 77.59))
trip = app.request_ride("r1", 12.971, 77.594)
print(trip.driver_id)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[driverId -> DriverLocation]` | latest driver location |
| `dict[cellId -> set[driverId]]` | geo index simulation |
| `dict[driverId -> offerId]` | driver hold/lock |
| `dict[tripId -> Trip]` | trip state |
| `dict[idempotencyKey -> result]` | retry-safe requests/payments |

## 4.4 Concurrency

High-signal concurrency issues:

- Two riders matching one driver.
- Driver accepts expired offer.
- Duplicate rider request.
- Duplicate payment charge.
- Trip state event retries.

Handling strategy:

- Atomic driver hold/CAS.
- Offer TTL and accept compare-and-set.
- Idempotency for request/accept/payment.
- State machine with valid transitions.

## 4.5 Testing Thinking

Unit tests:

- Nearest available driver selected.
- Held driver is not matched again.
- Expired offer cannot be accepted.
- Invalid trip transition rejected.
- Duplicate payment idempotency returns same result.

Load tests:

- Dense pickup hotspot.
- Driver location update flood.
- Offer timeout storm.
- Payment provider latency spike.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Event exit | concert/stadium | hot pickup cells and no driver supply |
| Weather surge | rain | pricing/matching pressure |
| Airport rush | flights land | local matching hotspot |
| Driver reconnect storm | app/network recovery | location ingestion spike |
| Payment provider degradation | provider outage | completion/payment backlog |

## 5.2 Immediate Spike Response

1. Protect trip state and driver assignment correctness.
2. Scale matching/location by city and hot cell.
3. Use search radius expansion with caps.
4. Apply request throttling and queueing in overloaded zones.
5. Use surge/incentives to balance supply.
6. Degrade ETA precision before matching correctness.
7. Queue payment retries asynchronously.

## 5.3 Degradation Policy

Protect in this order:

1. Existing active trips.
2. Driver assignment correctness.
3. Ride request/matching.
4. Payment recovery.
5. ETA precision.
6. Promotions/experiments.

Not allowed:

- Assign one driver to two trips.
- Lose trip state.
- Double-charge riders.
- Expose rider/driver location outside valid trip context.

## 5.4 Spike Interview Answer

> During spikes I protect existing trips and assignment correctness first. Location and matching scale by city/cell; driver locks prevent double assignment; payments use idempotent async recovery. ETA precision and experiments can degrade before trip correctness.

---

# 6. Scaling To Global Cities

## 6.1 Global Architecture

```text
Global routing
  -> city/region ride-hailing cell
  -> local location ingestion and geo index
  -> local matching and offer services
  -> durable trip/payment services
  -> global analytics/fraud/pricing pipelines
```

## 6.2 Multi-Region Strategy

- Partition matching by city/metro area.
- Keep driver/rider matching local.
- Store trip/payment data durably with regional replication.
- Fail over city services with degraded location freshness if needed.
- Keep pricing rules city-specific.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Location | city/cell sharded TTL stores |
| Geo index | in-memory hot partitions |
| Matching | city-zone workers and batch mode |
| Offer locks | driver-partitioned CAS store |
| Trip | durable state machine service |
| Payment | idempotent ledger and provider retries |
| Notifications | async rider/driver event delivery |
| Analytics | event streams and data lake |

## 6.4 Global Interview Answer

> I would scale ride-hailing by city because matching is local. Driver locations live in ephemeral geo indexes; matching uses approximate nearby candidates; short-lived locks protect driver assignment; Trip Service owns durable state; Payment Service owns idempotent money movement.

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
I will clarify ride request, driver availability, vehicle types, matching, cancellation, pricing, payment, and pooling scope.
I will estimate location update QPS, ride request QPS, active drivers, geo query load, and offer concurrency.
HLD includes Location Service, Geo Index, Matching Service, Offer/Lock Service, Trip Service, Pricing/Fare Service, Payment, and Notifications.
I keep location eventually consistent but driver assignment and trip state strongly controlled.
I use short-lived locks or CAS to prevent double assignment.
Payments use idempotency and async recovery.
For spikes, I partition by city/cell and degrade ETA precision before correctness.
```

---

# 8. Fast Recall Rules

- Ride-hailing is local by city/geo cell.
- Location is ephemeral; trip state is durable.
- Geo queries use cells/geohash/S2/H3 style indexing.
- Matching is approximate; assignment must be correct.
- Use short-lived driver locks or CAS.
- Trip lifecycle is a state machine.
- Payment must be idempotent.
- One driver cannot have two active trips.
- Realtime notifications are not source of truth.
- Hot zones need cell splitting, queueing, surge, and backpressure.
