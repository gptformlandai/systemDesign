# Food Delivery (Swiggy / Zomato) - End-to-End System Design

> Goal: practice one complete E2E marketplace transaction problem from problem understanding to HLD, LLD, machine coding, traffic spike handling, and global scale.

---

## How To Use This File

- Treat this as the repeatable pattern for marketplace ordering + geo dispatch systems.
- Start broad with requirements and scale, then zoom into restaurant discovery, menus, cart/checkout, order state, payment, restaurant acceptance, delivery matching, live tracking, consistency, LLD, and coding.
- In interviews, do not recite everything. Pick the parts that match the interviewer's signal.
- For Swiggy/Zomato-style systems, optimize order correctness, menu availability, payment reliability, restaurant workflow, delivery-partner matching, ETA accuracy, and spike resilience.

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

| Layer | Interview signal | Food delivery focus |
|---|---|---|
| Problem understanding | Can clarify marketplace lifecycle | browse restaurants, menu, cart, pay, restaurant accept, delivery, tracking |
| HLD | Can design transaction + geo systems | restaurant search, menu service, order service, payment, dispatch, tracking |
| LLD | Can model maintainable components | `Restaurant`, `MenuItem`, `Cart`, `Order`, `DeliveryTask`, `CourierLocation` |
| Machine coding | Can implement critical path | checkout, idempotent order, payment callback, state machine, courier assignment |
| Traffic spikes | Can protect production | lunch rush, rain surge, festival promos, restaurant overload, courier shortage |
| Global scale | Can reason across cities | city partitioning, geo dispatch, restaurant shards, delivery zones |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Users can browse nearby restaurants.
- Users can search/filter restaurants by cuisine, rating, delivery time, and availability.
- Users can view menus and item availability.
- Users can add items to cart and place an order.
- System calculates price, tax, delivery fee, discounts, and ETA.
- User pays online or selects cash if in scope.
- Restaurant accepts/rejects order.
- Delivery partner is assigned.
- User can track order and delivery partner location.
- System supports cancellation/refund according to state.

Optional requirements to clarify:

- Do restaurants have limited item inventory?
- Is cash-on-delivery in scope?
- Are scheduled/pre-orders in scope?
- Are multiple restaurants per order allowed?
- Do we support pickup/takeaway?
- Are ratings, reviews, and recommendations in scope?

Out of scope unless interviewer asks:

- Full loyalty/promotions platform.
- Full route optimization engine.
- Full fraud/risk model.
- Full restaurant partner onboarding.

## 1.2 Non-Functional Requirements

Order correctness:

- User should not be charged twice.
- Order state transitions must be valid.
- Restaurant/courier/user must see consistent enough state.
- Refund/cancellation must be auditable.

Marketplace performance:

- Fast nearby restaurant discovery.
- Menu reads are cacheable but availability can change.
- Checkout should revalidate price and availability.

Delivery:

- Efficient courier matching by geo and workload.
- Live tracking should be fresh but can be eventually consistent.
- Dispatch must handle courier rejection/timeout.

## 1.3 Constraints

- Restaurant menus change frequently.
- Item availability may be limited or manually toggled.
- Restaurant may reject or timeout after payment.
- Delivery partner availability/location is high-volume and ephemeral.
- Lunch/dinner spikes are predictable and intense.
- Discounts/payment/fees create many edge cases.
- ETA is approximate and changes over time.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---:|
| Registered users | 300 million |
| DAU | 50 million |
| Restaurants | millions |
| Delivery partners | millions |
| Orders/day | 20-50 million |
| Peak order QPS | 50K-200K globally |
| Courier location update interval | 2-5 seconds |
| Menu read to order ratio | 100:1 or higher |
| Order API availability | 99.99% |

## 1.5 Capacity Math

Back-of-the-envelope:

- `50M orders/day` is about `580 orders/sec` average, but lunch/dinner peaks can be 20x+ in cities.
- Restaurant/menu browsing QPS is much higher than order QPS.
- Courier location updates can be millions/sec globally.
- Dispatch queries are local by city/zone and can use geo cells.
- Checkout consistency scope is order/cart/restaurant/item availability, not global.

Useful interview numbers:

| Item | Rough value |
|---|---:|
| Courier location TTL | 10-30 seconds |
| Restaurant accept timeout | 30-120 seconds |
| Courier offer timeout | 10-30 seconds |
| Menu cache TTL | seconds to minutes |
| Order state update target | sub-second to few seconds |
| Payment callback retry | hours to days |

## 1.6 Clarifying Questions To Ask

- Should payment happen before or after restaurant acceptance?
- Can restaurant reject after payment?
- Is item-level inventory strict or best-effort availability?
- Do we assign courier before or after restaurant accepts?
- What cancellation/refund policies apply?
- Are promotions/coupons in scope?

Strong interview framing:

> I will design food delivery as a city-partitioned marketplace transaction system. Browse/menu data is read-heavy and cacheable; checkout revalidates price and availability; Order Service owns the state machine; Payment uses idempotency; Dispatch uses geo matching and offer timeouts for delivery partners.

---

# 2. High-Level Design

## 2.1 Architecture

Primary flows:

```text
Browse flow:
User -> Restaurant Search Service
     -> Geo restaurant index + availability + ranking
     -> nearby restaurant list

Checkout/order flow:
User -> Cart/Checkout Service
     -> Menu/Pricing/Promo revalidation
     -> Order Service creates order
     -> Payment Service
     -> Restaurant Order Service accept/reject
     -> Dispatch Service assigns courier
     -> Tracking/Notification updates

Courier dispatch flow:
Courier app -> Location Service -> Courier Geo Index
Order ready/accepted -> Dispatch Service -> nearby courier offers
```

Recommended architecture:

```text
User App                         Courier App
   |                                  |
   v                                  v
+-----------------------+      +----------------------+
| API Gateway + Auth    |      | Location Gateway     |
+-----------+-----------+      +----------+-----------+
            |                             |
            +-----------------------------+
            |
            v
+-----------------------+        +----------------------+
| Restaurant Search     |        | Courier Location     |
| geo/ranking/cache     |        | Geo Index            |
+-----------+-----------+        +----------+-----------+
            |                               |
            v                               v
+-----------------------+        +----------------------+
| Menu/Pricing/Cart     |        | Dispatch Service     |
+-----------+-----------+        | courier matching     |
            |                    +----------+-----------+
            v                               |
+-----------------------+                   v
| Order Service         |<------->+----------------------+
| state machine         |         | Delivery Task Svc    |
+-----------+-----------+         +----------------------+
            |
            +------------------+------------------+
            |                  |                  |
            v                  v                  v
+----------------+    +----------------+   +----------------+
| Payment Svc    |    | Restaurant Svc |   | Notification   |
+----------------+    +----------------+   +----------------+
```

Request flow for order:

1. User builds cart from one restaurant.
2. Checkout Service revalidates menu item availability, price, fees, taxes, and discounts.
3. Order Service creates order with idempotency key.
4. Payment Service charges or authorizes payment.
5. Restaurant receives order and accepts/rejects within timeout.
6. If accepted, Dispatch Service finds nearby delivery partners.
7. Courier accepts delivery task.
8. Order transitions through preparing, picked up, delivered.
9. Payment is captured/settled and receipt is sent.
10. Cancellation/refund workflows run based on state.

## 2.2 APIs And Events

### Nearby Restaurants

```http
GET /v1/restaurants/nearby?lat=12.9716&lng=77.5946&radius=3000
Authorization: Bearer <token>
```

### Get Menu

```http
GET /v1/restaurants/{restaurantId}/menu
```

### Checkout

```http
POST /v1/orders/checkout
Idempotency-Key: checkout-abc
```

```json
{
  "userId": "u-1",
  "restaurantId": "rest-1",
  "items": [
    {"menuItemId": "item-1", "quantity": 2}
  ],
  "deliveryAddress": {"lat": 12.9352, "lng": 77.6245},
  "paymentMethodId": "pm-1",
  "couponCode": "LUNCH"
}
```

Response:

```json
{
  "orderId": "ord-1",
  "state": "PAYMENT_PENDING",
  "amount": 450,
  "etaMinutes": 35
}
```

### Restaurant Accept

```http
POST /v1/restaurants/{restaurantId}/orders/{orderId}/accept
```

### Courier Location Update

```http
POST /v1/couriers/{courierId}/location
```

### Order Tracking

```http
GET /v1/orders/{orderId}/tracking
Authorization: Bearer <token>
```

Important API points:

- Checkout is idempotent.
- Price/menu availability is revalidated at checkout.
- Restaurant accept/reject is state-machine controlled.
- Courier assignment uses short-lived offers/locks.
- Tracking state can be eventually consistent.

## 2.3 Core Components

Think of food delivery as six connected planes:

| Plane | What it handles | Main goal |
|---|---|---|
| Discovery plane | nearby restaurants, ranking, search | fast marketplace browsing |
| Menu/pricing plane | item availability, price, fees, promos | correct checkout |
| Order plane | lifecycle and state machine | transaction truth |
| Payment plane | charge/capture/refund | money correctness |
| Restaurant plane | accept/reject/prep state | kitchen workflow |
| Dispatch plane | courier geo matching and tracking | delivery execution |

### Component Responsibility Map

| Component | Owns | Does not own | Scales by |
|---|---|---|---|
| Restaurant Search | nearby restaurant ranking | order truth | browse QPS |
| Menu Service | menu items/prices/availability | courier assignment | menu reads/writes |
| Cart/Checkout | price and availability validation | final delivery tracking | checkout QPS |
| Order Service | order lifecycle state | payment provider internals | order events |
| Payment Service | auth/capture/refund | restaurant prep state | payment volume |
| Restaurant Order Service | restaurant accept/prep events | user payment method | restaurant orders |
| Courier Location Service | courier position/availability | order pricing | location updates |
| Dispatch Service | courier matching/offers | menu availability | dispatch QPS |
| Tracking Service | order/courier live view | payment ledger | tracking reads |
| Notification Service | user/restaurant/courier updates | source of truth | events |

### Restaurant Discovery And Geo Queries

Why it exists:

- Users need nearby restaurants that can deliver to their location.
- Ranking depends on distance, prep time, rating, availability, fees, and personalization.

Geo strategies:

| Strategy | Use |
|---|---|
| Geohash/S2/H3 cells | find restaurants/couriers by nearby cells |
| Delivery polygons | validate restaurant serviceability |
| City/zone partitioning | natural scale boundary |
| Cache popular neighborhoods | reduce repeated browse load |

Failure behavior:

- Ranking can degrade to distance/popularity.
- Checkout still validates restaurant is open/serviceable.

### Menu And Availability

Core responsibilities:

- Store restaurant menu, item price, options, availability.
- Allow restaurant to mark item out of stock.
- Provide menu reads to users.
- Revalidate at checkout.

Consistency model:

- Menu reads can be cached.
- Checkout must read fresh enough availability and price.
- If item changed, return conflict and ask user to update cart.

Strict inventory options:

- For limited items, use atomic stock decrement.
- For normal menu availability, boolean availability may be enough.

### Order State Machine

States:

```text
CREATED -> PAYMENT_PENDING -> PAID -> RESTAURANT_PENDING -> ACCEPTED
        -> PREPARING -> READY_FOR_PICKUP -> PICKED_UP -> DELIVERED
        -> REJECTED
        -> CANCELLED
        -> REFUND_PENDING
        -> REFUNDED
```

Rules:

- Cannot dispatch if restaurant rejects.
- Cannot deliver before pickup.
- Cancellation/refund policy depends on state.
- Duplicate events are idempotent.

Interview signal:

> Order Service is the source of truth; restaurant, payment, dispatch, and tracking are state-machine participants.

### Payment Workflow

Common options:

| Option | Flow | Trade-off |
|---|---|---|
| Charge before restaurant accept | faster checkout | refund needed if rejected |
| Authorize then capture | cleaner reject handling | provider support/complexity |
| Pay after accept | fewer refunds | slower acceptance and risk |

Recommended interview approach:

- Create order.
- Authorize/charge payment idempotently.
- If restaurant rejects, refund/void payment.
- If delivered, capture/settle if using auth/capture.

Failure behavior:

- Payment timeout: retry with same idempotency key.
- Payment success but order update fails: payment callback/reconciliation repairs.
- Refund is idempotent by order/payment ID.

### Dispatch And Courier Matching

Candidate flow:

1. Order accepted and pickup location known.
2. Query courier geo index near restaurant.
3. Filter by availability, vehicle type, capacity, workload, stale location.
4. Rank by pickup ETA, route fit, fairness, acceptance probability.
5. Send delivery offer with timeout.
6. Courier accepts; Delivery Task Service locks courier.
7. If timeout/reject, offer next candidate.

Concurrency issue:

- One courier can receive many nearby delivery offers.
- Use short-lived courier lock/CAS to ensure one active delivery assignment.

Interview signal:

> Courier assignment looks like ride-hailing matching, but order state remains the durable transaction truth.

## 2.4 Data Layer

### Core Data Models

Restaurant:

```json
{
  "restaurantId": "rest-1",
  "cityId": "blr",
  "name": "Biryani House",
  "location": {"lat": 12.9716, "lng": 77.5946},
  "state": "OPEN"
}
```

Menu item:

```json
{
  "restaurantId": "rest-1",
  "menuItemId": "item-1",
  "name": "Paneer Roll",
  "price": 180,
  "available": true,
  "version": 12
}
```

Order:

```json
{
  "orderId": "ord-1",
  "userId": "u-1",
  "restaurantId": "rest-1",
  "state": "ACCEPTED",
  "amount": 450,
  "createdAt": "2026-06-17T12:00:00Z"
}
```

Delivery task:

```json
{
  "deliveryTaskId": "del-1",
  "orderId": "ord-1",
  "courierId": "c-1",
  "state": "ASSIGNED",
  "pickupEtaSeconds": 420
}
```

### Storage And Schema Choices

| Data type | Candidate store | Why |
|---|---|---|
| Restaurants | relational/search/geo index | discovery and serviceability |
| Menus | document/relational + cache | read-heavy structured data |
| Orders | relational DB | transaction lifecycle |
| Payments | ledger DB | money correctness |
| Courier location | TTL geo KV | high-write ephemeral |
| Delivery tasks | relational/strong KV | assignment state |
| Events | stream | async notifications/tracking/analytics |

Relational-style tables:

```sql
restaurants(restaurant_id PK, city_id, name, state, lat, lng)
menu_items(restaurant_id, menu_item_id, price, available, version)
orders(order_id PK, user_id, restaurant_id, state, amount, created_at, updated_at)
order_items(order_id, menu_item_id, quantity, unit_price)
payments(payment_id PK, order_id, state, amount, provider_ref)
delivery_tasks(task_id PK, order_id, courier_id, state, created_at)
courier_state(courier_id PK, availability, active_task_id, version)
```

Important indexes:

- Geo index on restaurant/courier location.
- `menu_items(restaurant_id, menu_item_id)` for checkout validation.
- `orders(user_id, created_at DESC)` for user history.
- `orders(restaurant_id, state)` for restaurant order queue.
- `delivery_tasks(courier_id, state)` for active tasks.

### Partitioning

- Partition restaurants and couriers by city/zone.
- Partition orders by city/time or order ID.
- Partition restaurant order queues by `restaurantId`.
- Partition courier state by `courierId`.
- Keep dispatch local to city/zone.

### Replication And Consistency

- Menu browse can be eventually consistent.
- Checkout must revalidate menu/restaurant availability.
- Order/payment state requires durable consistency.
- Courier location is eventually consistent and TTL-based.
- Dispatch assignment requires CAS/lock per courier.

## 2.5 Scalability

### Horizontal Scaling

- Restaurant Search scales by browse QPS.
- Menu Service scales by menu reads and cache hit rate.
- Order Service scales by order state events.
- Payment Service scales by payment attempts.
- Courier Location Service scales by update QPS.
- Dispatch Service scales by accepted orders and courier offer volume.

### Hot Restaurant / City Strategy

- Queue restaurant orders if kitchen capacity is full.
- Disable ordering for overloaded restaurant temporarily.
- Rate-limit promo abuse and repeated checkout.
- Scale dispatch workers by city/zone.
- Increase courier incentives in supply shortage.
- Degrade ETA precision, not order correctness.

## 2.6 Performance

### Latency Budget Example

| Stage | Target |
|---|---:|
| Nearby restaurant search | 50-300 ms |
| Menu fetch | 50-200 ms |
| Checkout validation | 100-500 ms |
| Payment | provider-dependent |
| Restaurant accept | 30-120 seconds human-dependent |
| Courier dispatch | seconds to minutes |
| Tracking update | few seconds |

### Optimization Rules

- Cache restaurant/menu reads.
- Revalidate price and availability at checkout.
- Keep order state transitions small and atomic.
- Use async notifications and tracking projections.
- Use geo cells for courier matching.
- Batch live tracking updates to clients.

## 2.7 Async Systems

Use streams for:

- order created
- payment succeeded/failed
- restaurant accepted/rejected
- dispatch requested
- courier assigned/rejected
- order picked up/delivered
- refund requested/completed
- notification requested
- analytics/fraud events

Queue notes:

- Consumers are idempotent.
- Order state changes publish through outbox.
- Dispatch retries must not assign multiple couriers.
- Payment/refund workflows use idempotency.

## 2.8 Security, Privacy, And Abuse

Security:

- Authenticated user/restaurant/courier APIs.
- Role-based access to order details.
- Payment method tokenization.
- Signed upload URLs for restaurant images if needed.

Privacy:

- User address and courier location are sensitive.
- Share live courier location only for active delivery.
- Restaurants should see only relevant order/customer details.

Abuse controls:

- Rate-limit coupon and checkout attempts.
- Detect fake courier GPS.
- Detect restaurant order manipulation.
- Fraud/risk checks for high-value/coupon-heavy orders.

## 2.9 Observability And Operations

Core SLIs:

| Area | Metrics |
|---|---|
| Discovery | search latency, serviceability errors, cache hit rate |
| Checkout | validation failures, price mismatch rate, order creation latency |
| Order | state transition failures, stuck orders, cancellation rate |
| Payment | success rate, duplicate callbacks, refund lag |
| Restaurant | accept latency, reject rate, timeout rate |
| Dispatch | assignment latency, courier acceptance rate, no-courier rate |
| Tracking | location freshness, tracking error rate |

Alerts:

- Orders stuck in paid/restaurant pending.
- Payment success but order not updated.
- Dispatch no-courier rate spikes in a zone.
- Restaurant accept timeout spikes.
- Courier location freshness drops.

## 2.10 Tradeoffs

| Decision | Option A | Option B | Trade-off |
|---|---|---|---|
| Payment timing | charge before restaurant accept | charge after accept | faster UX vs refund complexity |
| Menu consistency | cached menu | live menu read | low latency vs stale availability |
| Courier matching | greedy nearest | batch/route optimization | speed vs efficiency |
| Dispatch offer | one courier at a time | multiple couriers | less conflict vs faster accept |
| Tracking | frequent updates | batched updates | freshness vs battery/cost |
| Restaurant capacity | accept all | throttle/queue orders | revenue vs kitchen reliability |

Interview framing:

> I would make Order Service the source of truth, with idempotent payment and refund flows. Restaurant/menu reads are cacheable, checkout revalidates, and delivery matching is geo-based with short-lived courier locks.

---

# 3. Low-Level Design

LLD goal:

> Model food delivery around restaurants, menus, carts, orders, payments, restaurant acceptance, delivery tasks, courier location, and state transitions.

Simple rules:

- Cart is not order truth.
- Checkout revalidates price and availability.
- Order state machine controls lifecycle.
- Courier location is ephemeral.
- Payment and refund are idempotent.

## 3.1 Object Modelling

Core entities:

| Entity | Owns | Key invariant |
|---|---|---|
| `Restaurant` | location, serviceability, open state | closed restaurant cannot accept new orders |
| `MenuItem` | price, availability, version | checkout uses current version |
| `Cart` | user's tentative items | not durable purchase |
| `Order` | items, amount, lifecycle | valid state transitions only |
| `PaymentAttempt` | charge/refund state | idempotent per order |
| `DeliveryTask` | courier assignment and delivery state | one active courier assignment |
| `CourierLocation` | latest position and TTL | stale couriers ignored |
| `CourierOffer` | short-lived assignment offer | accepted once before expiry |

Core services:

| Service | Responsibility | Should not do |
|---|---|---|
| `RestaurantSearchService` | nearby/ranked restaurants | create orders |
| `MenuService` | menu and availability | charge payment |
| `CheckoutService` | validate cart and price | assign couriers |
| `OrderService` | order state machine | run geo index |
| `PaymentService` | charge/refund | decide restaurant prep |
| `DispatchService` | courier matching | mutate order without state machine |
| `TrackingService` | live order/courier state | source of order truth |

## 3.2 OOP Fundamentals

Encapsulation:

- `Order` owns valid state transitions.
- `MenuItem` owns availability/version validation.
- `DeliveryTask` owns courier assignment state.

Abstraction:

- `GeoIndex` hides courier/restaurant geospatial implementation.
- `PaymentGateway` hides provider APIs.
- `NotificationGateway` hides SMS/push/email providers.

Polymorphism:

- Different dispatch strategies: nearest, batch, route-aware, priority.
- Different pricing strategies: delivery fee, surge, subscription, promo.

Composition:

- `CheckoutService` composes menu, pricing, order, payment, and notification services.

## 3.3 SOLID Principles

| Principle | Food delivery application |
|---|---|
| Single Responsibility | `DispatchService` only assigns couriers |
| Open/Closed | add payment method without rewriting order state |
| Liskov Substitution | any `GeoIndex` supports nearby query contract |
| Interface Segregation | separate user, restaurant, courier APIs |
| Dependency Inversion | workflows depend on interfaces, not providers |

## 3.4 Design Patterns

| Pattern | Where to use | Why |
|---|---|---|
| State | order and delivery lifecycle | valid transitions |
| Saga | order -> payment -> restaurant -> dispatch | recover multi-step flow |
| Strategy | dispatch and pricing | choose by city/load/context |
| Command | checkout/accept/cancel events | idempotency and audit |
| Observer/Event Publisher | order events to notifications/tracking | decouple consumers |

## 3.5 UML / Diagrams

### Checkout Sequence

```text
User -> CheckoutService: checkout(cart)
CheckoutService -> MenuService: revalidate items/prices
CheckoutService -> PricingService: calculate total
CheckoutService -> OrderService: create order
OrderService -> PaymentService: charge/authorize
PaymentService -> OrderService: payment success/failure
OrderService -> RestaurantService: send order
RestaurantService -> OrderService: accept/reject
OrderService -> DispatchService: request courier
```

### Dispatch Sequence

```text
DispatchService -> CourierGeoIndex: nearby available couriers
DispatchService -> CourierOfferService: hold courier
CourierApp -> CourierOfferService: accept
CourierOfferService -> DeliveryTaskService: assign courier
DeliveryTaskService -> OrderService: courier assigned
TrackingService -> UserApp: live updates
```

## 3.6 Class Design

Interfaces:

```java
interface MenuService {
    ValidatedCart validate(String restaurantId, List<CartItem> items);
}

interface OrderStateMachine {
    Order transition(String orderId, OrderEvent event);
}

interface CourierGeoIndex {
    List<CourierCandidate> nearbyCouriers(Location pickup, int radiusMeters);
}

interface CourierLockStore {
    boolean tryAssign(String courierId, String deliveryTaskId, Duration ttl);
}

interface PaymentGateway {
    PaymentResult charge(String idempotencyKey, Money amount, String paymentMethodId);
}
```

Design notes:

- `validate()` checks item availability and current price/version.
- `transition()` rejects stale or invalid events.
- `tryAssign()` prevents double courier assignment.

## 3.7 Edge Cases

| Case | Handling |
|---|---|
| item becomes unavailable during checkout | reject and ask user to update cart |
| payment succeeds but restaurant rejects | refund/void payment |
| restaurant accepts after timeout | reject late accept or manual workflow |
| no courier available | retry dispatch, delay, cancel/refund if needed |
| courier accepts two tasks | courier CAS lock allows one |
| duplicate payment callback | idempotent order update |
| user cancels after prep starts | apply policy fee/refund |
| stale courier location | ignore by TTL |

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Suggested package layout:

```text
fooddelivery/
  domain/
    Restaurant.java
    MenuItem.java
    Cart.java
    Order.java
    DeliveryTask.java
    CourierLocation.java
  service/
    RestaurantSearchService.java
    MenuService.java
    CheckoutService.java
    OrderService.java
    DispatchService.java
  port/
    OrderRepository.java
    MenuRepository.java
    CourierGeoIndex.java
    PaymentGateway.java
  adapter/
    InMemoryOrderRepository.java
    InMemoryCourierGeoIndex.java
  app/
    FoodDeliveryDemo.java
```

## 4.2 Core Logic Implementation

Focused Python implementation sketch:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock
from typing import Dict


class OrderState(str, Enum):
    CREATED = "CREATED"
    PAID = "PAID"
    ACCEPTED = "ACCEPTED"
    PICKED_UP = "PICKED_UP"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


@dataclass
class MenuItem:
    item_id: str
    price: int
    available: bool
    version: int


@dataclass
class Order:
    order_id: str
    restaurant_id: str
    state: OrderState
    amount: int


class InMemoryFoodDelivery:
    def __init__(self) -> None:
        self.menu: Dict[str, MenuItem] = {}
        self.orders: Dict[str, Order] = {}
        self.lock = Lock()

    def add_item(self, item: MenuItem) -> None:
        self.menu[item.item_id] = item

    def checkout(self, restaurant_id: str, item_ids: list[str]) -> Order:
        with self.lock:
            total = 0
            for item_id in item_ids:
                item = self.menu[item_id]
                if not item.available:
                    raise ValueError("item unavailable")
                total += item.price
            order_id = f"ord-{len(self.orders) + 1}"
            order = Order(order_id, restaurant_id, OrderState.CREATED, total)
            self.orders[order_id] = order
            return order

    def transition(self, order_id: str, new_state: OrderState) -> Order:
        allowed = {
            OrderState.CREATED: {OrderState.PAID, OrderState.CANCELLED},
            OrderState.PAID: {OrderState.ACCEPTED, OrderState.CANCELLED},
            OrderState.ACCEPTED: {OrderState.PICKED_UP, OrderState.CANCELLED},
            OrderState.PICKED_UP: {OrderState.DELIVERED},
        }
        with self.lock:
            order = self.orders[order_id]
            if new_state not in allowed.get(order.state, set()):
                raise ValueError("invalid order transition")
            order.state = new_state
            return order


app = InMemoryFoodDelivery()
app.add_item(MenuItem("item-1", 180, True, 1))
order = app.checkout("rest-1", ["item-1"])
app.transition(order.order_id, OrderState.PAID)
print(order.amount)
```

## 4.3 Data Structures

| Structure | Use |
|---|---|
| `dict[restaurantId -> Restaurant]` | restaurant metadata |
| `dict[itemId -> MenuItem]` | menu validation |
| `dict[orderId -> Order]` | order state |
| `dict[courierId -> CourierLocation]` | courier location |
| `dict[courierId -> deliveryTaskId]` | courier assignment lock |
| `dict[idempotencyKey -> result]` | checkout/payment dedup |

## 4.4 Concurrency

High-signal concurrency issues:

- Item availability changes during checkout.
- Duplicate checkout retry.
- Payment callback races with cancellation.
- Courier receives multiple offers.
- Restaurant accept races with timeout/cancel.

Handling strategy:

- Versioned menu validation.
- Idempotency keys for checkout/payment/refund.
- Order state machine with compare-and-set.
- Courier lock/CAS with offer timeout.

## 4.5 Testing Thinking

Unit tests:

- Unavailable item cannot be ordered.
- Order state transitions are valid.
- Duplicate payment callback is idempotent.
- Courier cannot be assigned to two active tasks.
- Restaurant reject triggers refund path.

Load tests:

- Lunch rush order spike.
- Restaurant hotspot.
- Courier location flood.
- Dispatch shortage.
- Payment provider latency spike.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

| Spike type | Example | Main risk |
|---|---|---|
| Lunch/dinner rush | city-wide ordering | order/payment/dispatch pressure |
| Rain surge | demand spike + courier shortage | no courier availability |
| Promo campaign | coupon abuse and checkout spike | payment/order overload |
| Restaurant viral demand | kitchen overwhelmed | accept delays/rejections |
| Courier reconnect storm | app/network recovery | location ingestion spike |

## 5.2 Immediate Spike Response

1. Protect order state and payment correctness.
2. Throttle overloaded restaurants.
3. Scale dispatch by city/zone.
4. Use courier incentives or surge delivery fees.
5. Degrade ETA precision before order correctness.
6. Queue restaurant orders when kitchen capacity is constrained.
7. Rate-limit coupon/checkout abuse.

## 5.3 Degradation Policy

Protect in this order:

1. Active orders and payment/refund correctness.
2. Restaurant accept/reject state.
3. Courier dispatch.
4. Tracking freshness.
5. Restaurant ranking/search personalization.
6. Promotions/experiments.

Not allowed:

- Double-charge user.
- Lose paid order.
- Assign one courier to conflicting active tasks.
- Show private address/location to wrong party.

## 5.4 Spike Interview Answer

> During spikes I protect active orders and payment/refund correctness first. Browse/ranking and ETA can degrade. Restaurant and courier capacity are local bottlenecks, so I use city/zone dispatch scaling, restaurant throttling, and idempotent state-machine workflows.

---

# 6. Scaling To Global Cities

## 6.1 Global Architecture

```text
Global routing
  -> city/zone marketplace cell
  -> restaurant/menu search caches
  -> order/payment services
  -> restaurant workflow
  -> courier location/dispatch services
  -> tracking and notification pipelines
```

## 6.2 Multi-Region Strategy

- Partition marketplace by city/zone.
- Keep order lifecycle in the city/order owner region.
- Keep courier matching local.
- Replicate order history for user support.
- Keep payment ledger durable and reconciled.
- Use regional failover with degraded tracking if needed.

## 6.3 Global Capacity Plan

| Layer | Scaling plan |
|---|---|
| Restaurant search | city geo index and cache |
| Menu | restaurant-partitioned cache/read model |
| Checkout | stateless service + fresh validation |
| Order | durable state machine |
| Payment | idempotent ledger and provider retries |
| Dispatch | courier geo index by zone |
| Tracking | TTL location store and event stream |
| Notification | async multi-channel delivery |

## 6.4 Global Interview Answer

> I would scale food delivery by city/zone. Discovery and menu reads are cached, checkout revalidates state, Order Service owns the lifecycle, Payment is idempotent, and Dispatch uses geo matching with courier locks and retryable offers.

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
I will clarify restaurant browse, menu inventory, payment timing, restaurant acceptance, delivery assignment, cancellation, and refunds.
I will estimate browse QPS, menu reads, order QPS, courier location updates, dispatch QPS, and payment callbacks.
HLD includes Restaurant Search, Menu, Checkout, Order, Payment, Restaurant Workflow, Dispatch, Tracking, and Notification.
Checkout revalidates menu price/availability.
Order lifecycle is a state machine.
Payment/refund is idempotent.
Courier assignment uses geo index plus short-lived locks.
For spikes, I partition by city/zone and protect active orders before ranking/ETA precision.
```

---

# 8. Fast Recall Rules

- Food delivery is marketplace transaction plus geo dispatch.
- Browse/menu reads are cacheable; checkout revalidates.
- Cart is not order truth.
- Order Service owns lifecycle state.
- Restaurant can accept/reject/timeout.
- Payment and refund must be idempotent.
- Courier location is ephemeral TTL state.
- Courier assignment needs CAS/lock.
- ETA is approximate and can degrade.
- Active paid orders must not be lost.
