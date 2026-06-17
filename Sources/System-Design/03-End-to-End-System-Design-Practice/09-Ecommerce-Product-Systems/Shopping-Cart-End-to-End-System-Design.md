# Shopping Cart - End-to-End System Design

> Goal: practice one complete shopping cart problem from requirements to HLD, LLD, machine coding, concurrency, and scale.

---

## How To Use This File

- Use this when the interview asks for cart, checkout pre-processing, wishlist/cart state, guest cart merge, or e-commerce session state.
- Start with basic add/update/remove, then discuss cart versioning, price recalculation, coupons, inventory validation, TTL, merge, and checkout handoff.
- Keep one idea sharp: a cart is a mutable draft, not an order. It can be eventually consistent for convenience, but checkout must revalidate everything.
- In interviews, explain why cart state and order state should be separate.

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

| Layer | Interview signal | Shopping cart focus |
|---|---|---|
| Problem understanding | Can define cart lifecycle | guest cart, user cart, add/remove/update, merge, checkout |
| HLD | Can design stateful service | cart service, pricing, promotions, inventory preview, cache/store |
| LLD | Can model clean entities | `Cart`, `CartItem`, `CartVersion`, `PriceQuote`, `CartMergePolicy` |
| Machine coding | Can implement mutation safely | add item, update quantity, optimistic version, thread-safe map |
| Traffic spikes | Can protect checkout | cart write bursts, price recalculation storms, abandoned carts |
| Scale | Can reason storage and TTL | sharding by user/cart ID, cache, persistence, expiration |

---

# 1. Problem Understanding

## 1.1 Functional Requirements

Core requirements:

- Create cart for guest or logged-in user.
- Add item to cart.
- Update item quantity.
- Remove item from cart.
- View cart with item details and estimated total.
- Apply/remove coupon codes.
- Save cart across sessions for logged-in users.
- Expire abandoned guest carts.
- Merge guest cart into user cart after login.
- Handoff cart to checkout.

Optional requirements to clarify:

- Does cart support multiple sellers or warehouses?
- Does cart support digital goods and subscriptions?
- Should users save items for later?
- Should cart display live inventory availability?
- Should cart reserve inventory before checkout?
- Are coupons applied in cart or checkout only?

Out of scope unless interviewer asks:

- Full product catalog system.
- Full payment flow.
- Full recommendation engine.
- Full warehouse allocation.

## 1.2 Non-Functional Requirements

Correctness:

- Cart mutations should not lose items under concurrent updates.
- Checkout must use a consistent cart snapshot.
- Cart total shown to user is an estimate until checkout validation.

Availability:

- Cart should be highly available.
- Temporary cart read staleness is usually acceptable.
- Checkout handoff must be reliable.

Performance:

- Add/update/remove should be low latency.
- Cart view should load quickly.
- Price recalculation should not block simple mutations unnecessarily.

Scalability:

- Cart writes can be high during sales.
- Most carts are abandoned.
- Storage should use TTL/cleanup.

Security:

- Users can only mutate their own carts.
- Coupon and price fields must not be trusted from clients.
- Cart APIs need abuse protection.

## 1.3 Constraints

- Cart is mutable and frequently updated.
- Product price and availability can change after item is added.
- Guest users may not have stable identity.
- Multiple devices may edit the same user cart.
- Cart may contain stale/discontinued SKUs.
- Inventory reservation usually happens at checkout, not cart add.

## 1.4 Scale Assumptions

Example interview-scale assumptions:

| Metric | Assumption |
|---|---|
| Daily active users | 100M |
| Active carts/day | 80M |
| Cart mutations/day | 1B |
| Peak cart writes/sec | 100K+ |
| Average cart size | 5 to 20 items |
| Guest cart TTL | 7 to 30 days |
| Logged-in cart retention | 90 to 365 days |

Back-of-the-envelope:

- `1B cart mutations/day` is about `11.5K/sec` average.
- Peak may be 10x during sales.
- Cart state is small, but high write volume and high abandonment matter.
- Cart storage should be sharded and TTL-aware.

## 1.5 Clarifying Questions To Ask

- Do we support guest carts?
- What happens when a user logs in with an existing cart?
- Can users edit cart from multiple devices?
- Is inventory reserved on add-to-cart or only checkout?
- Should cart pricing be exact or estimated?
- Should coupons be validated immediately or at checkout?
- What is the abandoned-cart retention policy?

Strong interview framing:

> I will model the cart as a mutable draft with versioning. It can store selected SKUs and quantities, but checkout will revalidate price, coupons, inventory, address, and payment. This keeps cart fast while preserving order correctness.

---

# 2. High-Level Design

## 2.1 Architecture

```text
Clients
  |
  v
API Gateway
  |
  +--> Cart Service
  |      |
  |      +--> Cart Store
  |      +--> Cart Cache
  |
  +--> Catalog Service
  +--> Pricing Service
  +--> Promotion Service
  +--> Inventory Service
  +--> Checkout Service

Event Bus:
  CartCreated
  CartUpdated
  CouponApplied
  CartMerged
  CartCheckedOut
  CartExpired
```

Cart view path:

```text
Client -> Cart Service -> cart store/cache -> Catalog/Pricing preview -> response
```

Mutation path:

```text
Client -> Cart Service -> validate user -> load cart -> apply mutation -> increment version -> persist -> publish event
```

Checkout handoff:

```text
Client -> Checkout Service -> Cart Service snapshot(cartId, version) -> validate -> checkout workflow
```

## 2.2 APIs

Create/get cart:

```http
POST /v1/carts
GET /v1/carts/{cartId}
```

Add item:

```http
POST /v1/carts/{cartId}/items
If-Match: 17

{
  "skuId": "sku_123",
  "quantity": 2
}
```

Update quantity:

```http
PATCH /v1/carts/{cartId}/items/{skuId}
If-Match: 18

{
  "quantity": 3
}
```

Apply coupon:

```http
POST /v1/carts/{cartId}/coupons

{
  "code": "SAVE10"
}
```

Merge guest cart:

```http
POST /v1/carts/merge

{
  "guestCartId": "guest_1",
  "userCartId": "cart_2"
}
```

Checkout snapshot:

```http
POST /v1/carts/{cartId}/snapshot

{
  "expectedVersion": 19
}
```

## 2.3 Core Components

### Component Responsibility Map

| Component | Responsibility |
|---|---|
| Cart API | request validation, auth, idempotency |
| Cart Service | cart mutation, merge, snapshot |
| Cart Store | durable cart state |
| Cart Cache | fast reads for active carts |
| Catalog Client | SKU existence and display data |
| Pricing Client | estimated price |
| Promotion Client | coupon preview |
| Inventory Client | availability preview |
| Event Publisher | cart events for analytics/recovery |

### Cart State

Cart should store:

- `cartId`
- `userId` or guest session ID
- items with `skuId`, `quantity`, and added timestamp
- coupon codes
- selected address/shipping preference if needed
- cart version
- status: active, checked_out, expired, merged
- timestamps

Cart should not trust/store as final truth:

- final item price
- final tax
- final shipping fee
- final discount
- final inventory availability

It can store preview values for display, but checkout must recompute.

### Versioning

Why versioning matters:

- User opens cart on phone and laptop.
- Both update quantity.
- Without version checks, one write can silently overwrite another.

Pattern:

```text
read cart version 7
client sends If-Match: 7
server updates cart and writes version 8
another stale request with If-Match: 7 is rejected or merged
```

### Guest Cart Merge

Merge policies:

| Policy | Behavior |
|---|---|
| sum quantities | same SKU quantities are added |
| max quantity | keep larger quantity |
| prefer user cart | ignore duplicate guest item |
| prefer latest | keep latest updated item |

Strong default:

- Merge by SKU.
- Sum quantities up to SKU purchase limit.
- Preserve coupon only if eligible.
- Recalculate cart preview after merge.

## 2.4 Data Layer

Storage choices:

| Data | Storage |
|---|---|
| active cart | Redis/DynamoDB/Cassandra/document DB |
| durable user cart | document DB or key-value store |
| guest cart | key-value store with TTL |
| cart events | Kafka/PubSub event log |
| abandoned-cart campaign | analytics/event pipeline |

Example document:

```json
{
  "cartId": "cart_1",
  "userId": "user_1",
  "status": "ACTIVE",
  "version": 19,
  "items": [
    {
      "skuId": "sku_123",
      "quantity": 2,
      "addedAt": "2026-06-17T10:00:00Z"
    }
  ],
  "couponCodes": ["SAVE10"],
  "updatedAt": "2026-06-17T10:02:00Z"
}
```

Indexes:

- `cartId -> cart`
- `userId -> active cart`
- `guestSessionId -> guest cart`
- `updatedAt` for expiration jobs

## 2.5 Scalability

Partitioning:

- Shard by `cartId` or `userId`.
- Keep one user's active cart on one shard when possible.
- Guest carts can be partitioned by session ID.

Caching:

- Cache active cart reads.
- Write-through or write-around depending storage choice.
- Use TTL for inactive carts.

High write volume:

- Batch non-critical cart events.
- Avoid synchronous recommendation calls on cart mutation.
- Recalculate totals asynchronously or lazily if acceptable.

## 2.6 Performance

Latency budget:

| Step | Target |
|---|---|
| auth/session | 5 to 20 ms |
| cart store read/write | 5 to 30 ms |
| catalog preview | 10 to 50 ms |
| price preview | 10 to 50 ms |
| total API | 50 to 150 ms typical |

Optimization rules:

- Keep mutation path minimal.
- Use cached product display data for cart view.
- Recompute exact price only when needed.
- Do not call too many downstream services synchronously for every add-to-cart.

## 2.7 Async Systems

Useful events:

- `CartCreated`
- `CartItemAdded`
- `CartItemRemoved`
- `CartQuantityChanged`
- `CouponApplied`
- `CartMerged`
- `CartCheckedOut`
- `CartExpired`

Consumers:

- analytics
- recommendations
- abandoned-cart reminders
- fraud/risk
- inventory demand forecasting

Important rule:

- Cart events are useful, but the current cart document remains the source of truth.

## 2.8 Safety And Failure Handling

| Failure | Mitigation |
|---|---|
| stale cart update | optimistic version check |
| duplicate add request | idempotency key for mutation |
| product discontinued | mark item unavailable on cart view |
| price changed | show updated price and require confirmation |
| coupon invalid | remove/flag coupon during recalculation |
| cart store unavailable | degrade reads from cache, block checkout |
| checkout started with stale cart | snapshot with expected version |
| guest cart lost | acceptable if retention SLA says so |

## 2.9 Observability

Metrics:

- cart create rate
- add/update/remove rate
- cart read latency
- cart mutation latency
- version conflict rate
- cart merge success/failure
- abandoned cart count
- cart-to-checkout conversion
- cart store errors

Logs:

- `requestId`
- `userId`
- `cartId`
- `cartVersion`
- `mutationType`
- `idempotencyKey`

Alerts:

- cart mutation error spike
- storage latency spike
- version conflict spike
- checkout snapshot failures

## 2.10 Tradeoffs

| Choice | Pros | Cons |
|---|---|---|
| store cart in cache only | very fast | data loss risk |
| durable document store | reliable | slightly higher latency |
| reserve inventory on add | improves stock confidence | locks inventory too early |
| reserve inventory on checkout | simpler and fairer | item may sell out from cart |
| optimistic locking | high concurrency | clients must handle conflicts |
| pessimistic locking | simpler mutation correctness | lower throughput |

---

# 3. Low-Level Design

## 3.1 Object Modelling

```text
Cart
CartItem
CartStatus
CartMutation
CartSnapshot
CartMergePolicy
CartRepository
CartService
PricePreview
CouponPreview
```

## 3.2 OOP Fundamentals

Encapsulation:

- `Cart` owns item quantity rules.
- `CartService` owns auth, versioning, persistence, and events.

Polymorphism:

- `CartMergePolicy` can vary by business rule.
- `CartExpirationPolicy` can vary for guest/user carts.

Composition:

- `CartViewBuilder` combines cart state with catalog, pricing, and inventory preview.

## 3.3 Design Patterns

| Pattern | Use |
|---|---|
| Strategy | merge policy, expiration policy |
| Repository | cart persistence |
| Optimistic Locking | cart version conflict prevention |
| Builder | cart view response |
| Command | cart mutation requests |

## 3.4 Sequence Diagram

Add item:

```text
Client
  -> CartService: addItem(cartId, skuId, qty, expectedVersion)
  -> CartRepository: load(cartId)
  -> Cart: add item and increment quantity
  -> CartRepository: compareAndSet(cartId, oldVersion, newCart)
  -> EventBus: CartItemAdded
  -> Client: updated cart
```

Checkout snapshot:

```text
CheckoutService
  -> CartService: snapshot(cartId, expectedVersion)
  -> CartRepository: load(cartId)
  -> CartService: verify version/status
  -> CheckoutService: immutable cart snapshot
```

## 3.5 Edge Cases

- Add item with quantity 0.
- Quantity exceeds per-user purchase limit.
- Same SKU added twice concurrently.
- User logs in after building guest cart.
- User has cart open on multiple devices.
- Coupon conflicts with existing coupon.
- Cart item belongs to seller no longer active.
- Cart checked out while another update arrives.
- Cart expires during checkout.

---

# 4. Machine Coding Layer

## 4.1 Code Structure

```text
models/
  cart.py
services/
  cart_service.py
repositories/
  cart_repository.py
policies/
  merge_policy.py
```

## 4.2 Core Logic Implementation

Thread-safe in-memory cart service with optimistic versioning:

```python
from dataclasses import dataclass, field
from threading import Lock


@dataclass
class CartItem:
    sku_id: str
    quantity: int


@dataclass
class Cart:
    cart_id: str
    user_id: str | None
    version: int = 0
    items: dict[str, CartItem] = field(default_factory=dict)
    status: str = "ACTIVE"


class VersionConflict(Exception):
    pass


class CartService:
    def __init__(self) -> None:
        self.carts: dict[str, Cart] = {}
        self.lock = Lock()

    def create_cart(self, cart_id: str, user_id: str | None) -> Cart:
        with self.lock:
            cart = Cart(cart_id=cart_id, user_id=user_id)
            self.carts[cart_id] = cart
            return self._copy(cart)

    def add_item(self, cart_id: str, sku_id: str, quantity: int, expected_version: int) -> Cart:
        if quantity <= 0:
            raise ValueError("quantity must be positive")

        with self.lock:
            cart = self.carts[cart_id]
            self._check_version(cart, expected_version)
            self._check_active(cart)

            existing = cart.items.get(sku_id)
            if existing:
                existing.quantity += quantity
            else:
                cart.items[sku_id] = CartItem(sku_id=sku_id, quantity=quantity)

            cart.version += 1
            return self._copy(cart)

    def update_quantity(self, cart_id: str, sku_id: str, quantity: int, expected_version: int) -> Cart:
        with self.lock:
            cart = self.carts[cart_id]
            self._check_version(cart, expected_version)
            self._check_active(cart)

            if quantity <= 0:
                cart.items.pop(sku_id, None)
            else:
                cart.items[sku_id] = CartItem(sku_id=sku_id, quantity=quantity)

            cart.version += 1
            return self._copy(cart)

    def snapshot_for_checkout(self, cart_id: str, expected_version: int) -> Cart:
        with self.lock:
            cart = self.carts[cart_id]
            self._check_version(cart, expected_version)
            self._check_active(cart)
            return self._copy(cart)

    def _check_version(self, cart: Cart, expected_version: int) -> None:
        if cart.version != expected_version:
            raise VersionConflict(f"expected {expected_version}, found {cart.version}")

    def _check_active(self, cart: Cart) -> None:
        if cart.status != "ACTIVE":
            raise ValueError("cart is not active")

    def _copy(self, cart: Cart) -> Cart:
        return Cart(
            cart_id=cart.cart_id,
            user_id=cart.user_id,
            version=cart.version,
            items={sku: CartItem(item.sku_id, item.quantity) for sku, item in cart.items.items()},
            status=cart.status,
        )
```

What this demonstrates:

- Mutations are protected by a lock.
- Each update checks expected version.
- Checkout receives a stable cart snapshot.
- Quantity updates and removals are centralized.

## 4.3 Testing Thinking

Test cases:

- Add item to empty cart.
- Add same item twice increments quantity.
- Update quantity to zero removes item.
- Stale expected version raises conflict.
- Snapshot with current version succeeds.
- Mutation after checkout status fails.
- Concurrent updates do not lose quantity.

---

# 5. Handling Abnormal Traffic Spikes

## 5.1 Spike Types

- Sale traffic causing add-to-cart burst.
- Bot traffic adding hot SKU repeatedly.
- Price recalculation storm.
- Downstream catalog/pricing service slowdown.
- Abandoned-cart notification batch.

## 5.2 Immediate Response

- Rate limit abusive users/IPs.
- Keep cart mutation path minimal.
- Cache product display data.
- Defer non-critical events.
- Return partial cart view if preview services fail.
- Do not allow checkout without full validation.

## 5.3 Degradation Policy

| Situation | Degradation |
|---|---|
| pricing preview down | show cart items, hide final total |
| inventory preview down | show "availability checked at checkout" |
| recommendation down | omit recommendations |
| cart store slow | serve read-only stale cart if safe |
| checkout snapshot fails | ask user to retry |

## 5.4 Spike Interview Answer

> I would keep add/update/remove fast and protect them with sharding, TTL, and optimistic versioning. I would treat cart pricing as a preview and make checkout perform authoritative validation. During traffic spikes, I would degrade cart enrichments but never skip checkout validation.

---

# 6. Scaling Beyond One Service

## 6.1 Distributed Design

```text
Cart API
  -> Cart Service shards by cartId/userId
  -> Cart Store partitions
  -> Cart Event Stream
  -> Analytics/Recommendation/Abandoned Cart consumers
```

## 6.2 Scaling Rules

- Keep one active cart per user if business allows.
- Use TTL for guest carts and abandoned carts.
- Shard by stable ID.
- Use optimistic locking in persistent store.
- Separate cart write path from cart view enrichment.
- Do not make cart service depend synchronously on too many systems for mutation.

## 6.3 Interview Answer

> At scale, I would shard cart state by cart ID or user ID and store it in a durable key-value/document store with TTL. Mutations use optimistic versioning to avoid lost updates. Cart view can enrich with cached catalog and estimated pricing, but checkout must request an immutable cart snapshot and revalidate all business rules.

---

# 7. Final Interview Playbook

Start with:

> A cart is a mutable draft. It should be fast and available, but it should not be treated as a confirmed order.

Then cover:

1. Guest and logged-in cart lifecycle.
2. Add/update/remove APIs.
3. Cart versioning for multi-device concurrency.
4. Cart storage and TTL.
5. Cart view enrichment with catalog/pricing/inventory previews.
6. Checkout snapshot and validation.
7. Merge behavior after login.
8. Failure handling and traffic spikes.

Common traps:

- Reserving inventory forever when item is added to cart.
- Treating cart total as final payment amount.
- Ignoring stale multi-device updates.
- Forgetting guest cart expiration.
- Blocking cart mutations on slow recommendation/pricing calls.

---

# 8. Fast Recall Rules

- Cart is a draft, order is a commitment.
- Cart can be available and eventually enriched.
- Checkout must revalidate everything.
- Use optimistic versioning for concurrent edits.
- Use TTL for guest and abandoned carts.
- Merge guest cart carefully after login.
- Store SKU and quantity, not trusted final price.
- Do not reserve inventory too early unless business requires it.
- Degrade preview, not checkout validation.
- Shard by cart ID or user ID.

