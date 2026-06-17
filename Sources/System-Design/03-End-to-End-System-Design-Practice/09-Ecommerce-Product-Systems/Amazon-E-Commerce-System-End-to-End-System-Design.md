# Amazon E-Commerce System - End-to-End System Design

> Goal: practice one complete commerce platform problem from requirements to HLD, LLD, machine coding, traffic spikes, and global scale.

---

## How To Use This File

- Use this when the interview asks for Amazon, Flipkart, marketplace, e-commerce platform, checkout, order management, or inventory correctness.
- Start broad with browsing and checkout, then zoom into catalog, search, cart, pricing, inventory reservation, payment, order workflow, shipment, and notifications.
- Keep one idea sharp: e-commerce is not one database transaction. It is a business workflow coordinated across many services, with careful idempotency and compensation.
- In interviews, separate read-heavy product discovery from write-critical checkout and order correctness.

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

| Layer | Interview signal | Amazon focus |
|---|---|---|
| Problem understanding | Can clarify commerce scope | browse, search, cart, checkout, payment, order, shipment, returns |
| HLD | Can decompose platform | catalog, search, cart, pricing, inventory, order, payment, fulfillment |
| LLD | Can model business entities | `Product`, `SKU`, `Cart`, `Order`, `Payment`, `Shipment`, `Return` |
| Machine coding | Can implement core workflow | idempotent checkout, inventory reservation, order state transitions |
| Traffic spikes | Can protect hot paths | flash sale, search surge, payment retries, stock conflicts |
| Global scale | Can reason across regions | regional inventory, multi-region reads, order home region, async replication |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core customer requirements:

- Users can browse product categories.
- Users can search and filter products.
- Users can view product details, images, price, stock, reviews, and delivery options.
- Users can add, update, and remove cart items.
- Users can apply coupons, gift cards, wallet balance, or loyalty points.
- Users can place an order.
- System reserves inventory during checkout.
- System processes payment.
- System creates order and shipment tasks.
- Users can track orders.
- Users can cancel, return, or request refund based on policy.

Seller/admin requirements:

- Sellers can create and update product listings.
- Sellers can update inventory.
- Sellers can manage pricing and promotions.
- Operations teams can manage fulfillment and returns.

Optional requirements to clarify:

- Is this first-party inventory, marketplace inventory, or both?
- Do we support auctions, subscriptions, or digital goods?
- Are recommendations in scope?
- Are reviews and ratings in scope?
- Are returns/refunds in scope?
- Do we need multi-currency and cross-border shipping?

Out of scope unless interviewer asks:

- Full warehouse robotics.
- Full tax engine implementation.
- Full fraud ML model.
- Full ad bidding platform.

## 1.2 Non-Functional Requirements

Availability:

- Product browsing and search should be highly available.
- Checkout should degrade carefully but not corrupt order/payment state.

Correctness:

- Do not oversell inventory beyond allowed policy.
- Do not double charge.
- Do not create duplicate orders for the same checkout request.
- Preserve immutable audit history for order and payment changes.

Performance:

- Product pages should load quickly.
- Search should have low latency.
- Checkout should be predictable under spike load.

Scalability:

- Read traffic is much larger than write traffic.
- Hot products and flash sales must not overload inventory or payment systems.

Reliability:

- External payment and shipping providers can timeout.
- Async events may be duplicated or delayed.
- Workflows must be idempotent and retry-safe.

Security:

- Protect user PII.
- Tokenize payment information.
- Enforce seller/admin authorization.
- Keep audit logs for financial and inventory operations.

## 1.3 Constraints

- Product catalog can be eventually consistent for reads.
- Checkout/order/payment state needs stronger consistency.
- Inventory can be modeled as available, reserved, sold, and returned.
- Promotions can change quickly but must be reproducible for an order.
- Payment providers can return ambiguous timeout states.
- Shipment providers can be slow or partially unavailable.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Users | 500M+ |
| Daily active users | 100M |
| Products/SKUs | 500M+ |
| Product page views/day | 5B |
| Searches/day | 1B |
| Orders/day | 50M |
| Peak checkout requests/sec | 100K+ |
| Peak product page reads/sec | millions globally |
| Availability target | 99.99% for browsing, very high correctness for checkout |

Back-of-the-envelope:

- `5B product views/day` is about `58K/sec` average, often 10x peak.
- `50M orders/day` is about `580/sec` average, but campaign peaks can be huge.
- Images and static product content should be served through CDN.
- Search/index reads should be separated from order writes.

## 1.5 Clarifying Questions To Ask

- Is the focus discovery, checkout, or end-to-end platform?
- Are sellers updating inventory in real time?
- Is overselling absolutely forbidden or is limited oversell acceptable?
- Should payment be captured immediately or after shipment?
- Are coupons, taxes, and shipping fee calculation in scope?
- Do users need guest checkout?
- What is the expected behavior when payment succeeds but order creation times out?

Strong interview framing:

> I will split the system into read-heavy discovery and correctness-heavy commerce workflow. Product browsing can use denormalized indexes and caches. Checkout will use idempotency, inventory reservation, order state machines, payment orchestration, and async fulfillment events.

---

# 2. High-Level Design

## 2.1 Architecture

High-level components:

```text
Clients
  |
  v
API Gateway
  |
  +--> Identity Service
  +--> Catalog Service
  +--> Search Service
  +--> Recommendation Service
  +--> Cart Service
  +--> Pricing Service
  +--> Promotion Service
  +--> Inventory Service
  +--> Checkout Service
  +--> Order Service
  +--> Payment Service
  +--> Fulfillment Service
  +--> Notification Service
  +--> Returns/Refund Service

Event Bus:
  ProductUpdated
  InventoryChanged
  CartCheckedOut
  OrderCreated
  PaymentAuthorized
  PaymentFailed
  ShipmentCreated
  OrderDelivered
  RefundIssued
```

Read path:

```text
User -> API Gateway -> Search/Catalog -> cache/search index -> product detail -> CDN images
```

Checkout path:

```text
User -> Checkout Service
     -> Cart Service validates cart
     -> Pricing/Promotion computes final price
     -> Inventory reserves stock
     -> Order creates pending order
     -> Payment authorizes/captures
     -> Order confirms
     -> Fulfillment creates shipment
     -> Notification sends confirmation
```

## 2.2 APIs

Catalog/search:

```http
GET /v1/products/{productId}
GET /v1/search?q=phone&category=electronics&sort=price
GET /v1/products/{productId}/availability?zip=500081
```

Cart:

```http
POST /v1/carts/{cartId}/items
PATCH /v1/carts/{cartId}/items/{skuId}
DELETE /v1/carts/{cartId}/items/{skuId}
GET /v1/carts/{cartId}
```

Checkout:

```http
POST /v1/checkout
Idempotency-Key: checkout_123

{
  "cartId": "cart_1",
  "addressId": "addr_1",
  "paymentMethodId": "pm_1",
  "couponCodes": ["SAVE10"]
}
```

Response:

```json
{
  "checkoutId": "chk_1",
  "orderId": "ord_1",
  "status": "PAYMENT_PENDING",
  "amount": 129900
}
```

Order:

```http
GET /v1/orders/{orderId}
POST /v1/orders/{orderId}/cancel
POST /v1/orders/{orderId}/return
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| API Gateway | auth, routing, rate limits, request IDs |
| Catalog Service | product metadata, SKU mapping, seller listing data |
| Search Service | full-text search, filters, sort, ranking |
| Cart Service | user cart state, guest cart merge, cart version |
| Pricing Service | base price, taxes, shipping, dynamic price |
| Promotion Service | coupons, discounts, eligibility, usage limits |
| Inventory Service | available/reserved/sold stock |
| Checkout Service | orchestrates checkout workflow |
| Order Service | source of truth for order lifecycle |
| Payment Service | payment intent, auth/capture/refund, gateway adapter |
| Fulfillment Service | warehouse allocation, packing, shipment |
| Notification Service | email, SMS, push events |

### Product Discovery

Product discovery is read-heavy.

Use:

- CDN for images and static assets.
- Search index for query/filter/sort.
- Product cache for hot product details.
- Denormalized product view documents.
- Async index updates from catalog changes.

Tradeoff:

- Catalog update may take seconds to appear in search.
- Checkout must revalidate price and inventory using source-of-truth services.

### Checkout Workflow

Checkout should not be a giant distributed transaction.

Use a saga:

1. Validate cart version.
2. Compute price and promotions.
3. Reserve inventory.
4. Create pending order.
5. Authorize or capture payment.
6. Confirm order.
7. Publish fulfillment event.
8. If any step fails, compensate earlier steps.

Compensation examples:

- Payment fails: release inventory reservation.
- Order creation fails after inventory reservation: release reservation.
- Fulfillment fails: keep order confirmed but move to fulfillment exception state.
- Payment timeout: mark payment as pending and reconcile with provider.

### Inventory Model

Basic states:

| State | Meaning |
|---|---|
| available | can be reserved |
| reserved | held for a checkout/order |
| sold | committed after order confirmation |
| returned | added back after return policy completes |
| damaged/lost | excluded from sellable stock |

Simple inventory formula:

```text
sellable = on_hand - reserved - safety_stock
```

For hot items:

- Use per-SKU atomic counters.
- Partition very hot inventory by seller/warehouse.
- Use a reservation TTL.
- Use queueing for flash sales.

## 2.4 Data Layer

Possible storage choices:

| Data | Storage |
|---|---|
| product master | relational or document DB |
| product search documents | Elasticsearch/OpenSearch/Solr-like index |
| product images | object storage + CDN |
| cart | key-value/document DB with TTL |
| price and promotion rules | relational/document DB + cache |
| inventory counters | strongly consistent DB or atomic KV |
| orders | relational/sharded DB |
| payments | relational DB + immutable ledger |
| events | Kafka/PubSub-like event log |

Core entities:

```text
Product(productId, title, brand, categoryId, attributes)
SKU(skuId, productId, sellerId, variant, listPrice)
Inventory(skuId, warehouseId, available, reserved, version)
Cart(cartId, userId, items, couponCodes, version, updatedAt)
Order(orderId, userId, status, amount, address, createdAt)
OrderItem(orderId, skuId, quantity, unitPrice, discount, status)
Payment(paymentId, orderId, status, amount, providerRef)
Shipment(shipmentId, orderId, warehouseId, carrier, status)
```

## 2.5 Scalability

Read scaling:

- Cache product details.
- Serve images via CDN.
- Use search replicas.
- Precompute category pages and top deals.

Write scaling:

- Shard cart by user ID.
- Shard orders by order ID or user ID.
- Shard inventory by SKU/warehouse.
- Use event-driven workflows for non-critical side effects.

Hot product mitigation:

- Queue checkout attempts.
- Use reservation tokens.
- Use stock buckets per warehouse.
- Rate limit bots and repeated attempts.
- Show approximate availability on product page, revalidate at checkout.

## 2.6 Caching

Good cache candidates:

- Product detail pages.
- Product images.
- Category navigation.
- Search suggestions.
- Promotion rule metadata.
- Delivery promise templates.

Bad cache candidates:

- Final checkout total without revalidation.
- Payment state.
- Order state transitions.
- Inventory for hot SKUs without source-of-truth validation.

Cache invalidation strategy:

- Product metadata: event-based invalidation.
- Price changes: short TTL + versioned price book.
- Inventory: short TTL for display only, source check during reservation.

## 2.7 Async Systems

Events:

| Event | Consumers |
|---|---|
| ProductUpdated | search index, cache invalidator |
| InventoryChanged | availability cache, product page |
| CartCheckedOut | analytics, personalization |
| OrderCreated | payment, fraud, notification |
| PaymentSucceeded | order, fulfillment, ledger |
| ShipmentCreated | notification, tracking |
| OrderDelivered | review prompt, return window |

Rules:

- Events must be idempotent.
- Consumers should store processed event IDs.
- Use outbox pattern for order/payment events.
- Do not rely only on in-memory queues for critical commerce events.

## 2.8 Safety And Failure Handling

Important failure cases:

| Failure | Mitigation |
|---|---|
| duplicate checkout click | idempotency key |
| payment timeout | pending state + reconciliation |
| inventory reserve succeeds, payment fails | release reservation |
| payment succeeds, order confirm fails | retry order confirm with idempotent transaction |
| event duplicate | idempotent consumer |
| search index stale | revalidate SKU at checkout |
| warehouse unavailable | fallback warehouse allocation |
| promotion changed during checkout | cart version and price snapshot |

## 2.9 Observability

Metrics:

- product page latency
- search latency
- cart update latency
- checkout conversion rate
- inventory reservation success rate
- payment success/failure/timeout rate
- order confirmation delay
- fulfillment backlog
- duplicate idempotency hits

Logs/traces:

- `requestId`
- `userId`
- `cartId`
- `checkoutId`
- `orderId`
- `paymentId`
- `inventoryReservationId`

Alerts:

- payment provider error spike
- checkout latency spike
- inventory reservation conflict spike
- order stuck in pending state
- outbox event lag
- fulfillment queue backlog

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| monolith commerce service | simple early development | hard to scale independently |
| microservices | ownership and scaling isolation | distributed workflow complexity |
| strong inventory consistency | prevents oversell | higher latency and lower availability |
| eventual product search | fast and scalable | stale search results |
| sync checkout workflow | simple response model | sensitive to downstream latency |
| async saga | resilient and scalable | more states and compensation logic |

---

# 3. Low-Level Design

## 3.1 Object Modelling

Important classes:

```text
Product
SKU
Seller
Cart
CartItem
PriceQuote
Discount
InventoryReservation
CheckoutSession
Order
OrderItem
PaymentAttempt
Shipment
ReturnRequest
```

Important enums:

```text
OrderStatus:
  CREATED
  PAYMENT_PENDING
  PAID
  CONFIRMED
  FULFILLMENT_PENDING
  SHIPPED
  DELIVERED
  CANCELLED
  RETURNED

PaymentStatus:
  INITIATED
  AUTHORIZED
  CAPTURED
  FAILED
  REFUNDED
  RECONCILING
```

## 3.2 OOP Fundamentals

Encapsulation:

- `Cart` owns item mutation and versioning.
- `Order` owns legal state transitions.
- `InventoryService` owns reservation correctness.

Polymorphism:

- `PaymentProvider` implementations for Stripe, Adyen, Razorpay, internal wallet.
- `DiscountRule` implementations for percentage, fixed amount, free shipping, BOGO.

Composition:

- `CheckoutService` composes cart, pricing, inventory, order, and payment services.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Saga | checkout workflow with compensation |
| State | order/payment lifecycle |
| Strategy | payment provider, promotion rule, shipping calculation |
| Adapter | external payment/shipping providers |
| Outbox | reliable event publishing |
| Repository | persistence isolation |
| Factory | create order/payment objects from checkout request |

## 3.4 Sequence Diagram

```text
User
  -> CheckoutService: checkout(cartId, idempotencyKey)
CheckoutService
  -> CartService: getCart(cartId)
  -> PricingService: quote(cart)
  -> InventoryService: reserve(items)
  -> OrderService: createPendingOrder()
  -> PaymentService: charge()
  -> OrderService: confirm()
  -> EventBus: publish(OrderConfirmed)
  -> User: order confirmation
```

Failure sequence:

```text
PaymentService returns FAILED
  -> CheckoutService releases inventory reservation
  -> OrderService marks payment_failed
  -> User sees payment failure and can retry
```

## 3.5 Class Design

```text
CheckoutService
  - cartService
  - pricingService
  - inventoryService
  - orderService
  - paymentService
  + checkout(request)

OrderService
  + createPendingOrder(...)
  + markPaid(...)
  + confirm(...)
  + cancel(...)

InventoryService
  + reserve(items, ttl)
  + commit(reservationId)
  + release(reservationId)

PaymentService
  + createPaymentIntent(...)
  + charge(...)
  + refund(...)
```

## 3.6 Edge Cases

- Item price changed after it was added to cart.
- Item goes out of stock during checkout.
- Coupon expires during checkout.
- User double-clicks place order.
- Payment succeeds but client times out.
- Payment webhook arrives before synchronous response.
- Order confirmation event is duplicated.
- Seller cancels listing after order creation.
- Partial shipment fails.
- Return request arrives after return window.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

Good machine-coding decomposition:

```text
models/
  cart.py
  order.py
  inventory.py
services/
  checkout_service.py
  inventory_service.py
  payment_service.py
  order_service.py
repositories/
  idempotency_store.py
```

## 4.2 Core Logic Implementation

Small checkout saga simulation:

```python
from dataclasses import dataclass
from enum import Enum
from threading import Lock


class OrderStatus(Enum):
    PENDING_PAYMENT = "PENDING_PAYMENT"
    CONFIRMED = "CONFIRMED"
    PAYMENT_FAILED = "PAYMENT_FAILED"


@dataclass(frozen=True)
class CartItem:
    sku_id: str
    quantity: int
    unit_price: int


@dataclass
class Order:
    order_id: str
    items: list[CartItem]
    amount: int
    status: OrderStatus


class InventoryService:
    def __init__(self, stock: dict[str, int]) -> None:
        self.stock = stock
        self.reserved: dict[str, int] = {}
        self.lock = Lock()

    def reserve(self, items: list[CartItem]) -> str:
        with self.lock:
            for item in items:
                if self.stock.get(item.sku_id, 0) < item.quantity:
                    raise ValueError(f"out of stock: {item.sku_id}")

            for item in items:
                self.stock[item.sku_id] -= item.quantity
                self.reserved[item.sku_id] = self.reserved.get(item.sku_id, 0) + item.quantity

            return "res_1"

    def release(self, items: list[CartItem]) -> None:
        with self.lock:
            for item in items:
                self.stock[item.sku_id] += item.quantity
                self.reserved[item.sku_id] -= item.quantity

    def commit(self, items: list[CartItem]) -> None:
        with self.lock:
            for item in items:
                self.reserved[item.sku_id] -= item.quantity


class PaymentService:
    def __init__(self, should_succeed: bool = True) -> None:
        self.should_succeed = should_succeed

    def charge(self, order_id: str, amount: int) -> bool:
        return self.should_succeed


class CheckoutService:
    def __init__(self, inventory: InventoryService, payments: PaymentService) -> None:
        self.inventory = inventory
        self.payments = payments
        self.orders: dict[str, Order] = {}
        self.idempotency: dict[str, str] = {}
        self.lock = Lock()

    def checkout(self, idempotency_key: str, items: list[CartItem]) -> Order:
        with self.lock:
            if idempotency_key in self.idempotency:
                return self.orders[self.idempotency[idempotency_key]]

            amount = sum(item.quantity * item.unit_price for item in items)
            order_id = f"ord_{len(self.orders) + 1}"
            order = Order(order_id, items, amount, OrderStatus.PENDING_PAYMENT)
            self.orders[order_id] = order
            self.idempotency[idempotency_key] = order_id

        self.inventory.reserve(items)

        if not self.payments.charge(order.order_id, order.amount):
            self.inventory.release(items)
            order.status = OrderStatus.PAYMENT_FAILED
            return order

        self.inventory.commit(items)
        order.status = OrderStatus.CONFIRMED
        return order
```

What this demonstrates:

- Idempotency prevents duplicate order creation.
- Inventory reservation protects against oversell.
- Payment failure compensates by releasing reserved stock.
- Real production code would persist state transitions and publish events with outbox.

## 4.3 Testing Thinking

Test cases:

- Same idempotency key returns same order.
- Out-of-stock checkout fails.
- Payment failure releases inventory.
- Successful payment commits inventory.
- Concurrent checkouts for limited SKU allow only valid quantity.
- Checkout retry after timeout does not double charge.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Festival sale.
- Flash sale on one product.
- Bot scraping product pages.
- Payment provider outage causing retries.
- Search traffic surge from campaign.
- Inventory update storm from sellers.

## 5.2 Immediate Response

- Protect APIs with rate limiting.
- Queue checkout attempts for hot SKUs.
- Serve cached product pages.
- Disable expensive filters temporarily.
- Use circuit breakers for bad payment providers.
- Use async retries with backoff, not tight loops.

## 5.3 Degradation Policy

| Area | Degradation |
|---|---|
| search | fewer filters, cached results |
| product page | approximate stock |
| cart | allow edits, delay recalculation |
| checkout | queue hot SKU attempts |
| payment | fail over provider or mark pending |
| notification | delay non-critical messages |

## 5.4 Spike Interview Answer

> During a sale, I would protect product discovery with CDN/search replicas and protect checkout with queueing, idempotency, and inventory reservations. I would not trust cached stock for final purchase. For payment outages, I would avoid retry storms, move ambiguous payments to reconciliation, and make every callback idempotent.

---

# 6. Global Scale

## 6.1 Regional Architecture

Principles:

- Keep user reads close to users.
- Keep order writes in an order home region.
- Keep inventory close to warehouses/sellers.
- Avoid cross-region synchronous checkout dependencies where possible.

Pattern:

```text
Global DNS / Edge
  -> regional API gateways
  -> regional catalog/search caches
  -> order home region
  -> inventory region by warehouse
  -> payment provider routing by geography
```

## 6.2 Multi-Region Challenges

- Inventory cannot be blindly active-active for the same unit of stock.
- Product catalog can replicate asynchronously.
- Payment and ledger data may have residency constraints.
- Returns/refunds must preserve order history.
- Search ranking can vary by region.

## 6.3 Interview Answer

> I would make product discovery multi-region and eventually consistent. For checkout, I would assign each order to a home region and route inventory reservations to the owning warehouse region. I would avoid global distributed transactions and instead use saga steps with idempotency, compensation, outbox events, and reconciliation.

---

# 7. Final Interview Playbook

Start with:

> Amazon-like e-commerce has two different shapes: read-heavy product discovery and correctness-heavy checkout. I will design them separately and connect them through validated checkout.

Then cover:

1. Functional and non-functional requirements.
2. Read path for product discovery.
3. Checkout saga.
4. Inventory reservation and payment idempotency.
5. Order state machine.
6. Async fulfillment and notifications.
7. Failure handling and reconciliation.
8. Scale, spikes, and global regions.

Common traps:

- Treating checkout as one distributed SQL transaction.
- Trusting cached price/inventory at checkout.
- Forgetting duplicate payment prevention.
- Forgetting reservation expiry.
- Ignoring event duplication.

---

# 8. Fast Recall Rules

- Product discovery is read-heavy; checkout is correctness-heavy.
- Cache product pages, not final checkout truth.
- Revalidate price, promotion, address, stock, and payment before order confirmation.
- Use idempotency for checkout and payment.
- Use inventory reservation with TTL.
- Use saga and compensation instead of distributed transactions.
- Payment timeout means unknown, not failed.
- Publish critical events with outbox.
- Hot products need queueing, rate limits, and atomic stock counters.
- Global checkout needs a home region and clear ownership.

