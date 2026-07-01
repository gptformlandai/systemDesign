# 05 - Availability E-Commerce Eventual Consistency Architecture

> Goal: design a high-traffic e-commerce system that remains available during spikes and partial failures, while accepting eventual consistency where the product can tolerate it.

---

## 1. Problem Statement

Design a high-traffic e-commerce platform like Amazon/Flipkart for:

- product discovery
- search/filter
- product details
- cart
- checkout
- payment
- order creation
- inventory reservation
- fulfillment events
- notifications
- analytics/recommendations

Primary product promise:

```text
Users should almost always be able to browse, search, view products, use cart, and attempt checkout,
even during spikes and partial failures.
```

Important nuance:

```text
Availability-first does not mean every operation is eventually consistent. Discovery can be stale;
checkout correctness still needs controlled write boundaries.
```

---

## 2. CAP Position

Availability-first paths:

| Path | Consistency Choice | Why |
|---|---|---|
| home page | eventual | stale banners/recommendations acceptable |
| product catalog | eventual/bounded stale | product info can lag briefly |
| search index | eventual | indexing delay acceptable |
| recommendations | eventual | derived data |
| reviews summary | eventual | minor lag acceptable |
| cart | read-your-writes preferred, eventual merge acceptable | user convenience |
| inventory display | approximate/stale | final check at reservation |

Consistency-first paths inside commerce:

| Path | Consistency Choice | Why |
|---|---|---|
| inventory reservation | strong per SKU/reservation unit | avoid uncontrolled oversell |
| order creation | transactional state machine | avoid duplicate/invalid orders |
| payment authorization/capture | idempotent strict state | avoid double charge |
| refunds | auditable strict state | money correctness |

CAP answer:

```text
For the e-commerce discovery path, I choose availability and low latency with eventual consistency.
For checkout-critical writes, I use stronger local consistency and idempotent workflows. This gives
high availability for most user traffic without corrupting money or inventory state.
```

---

## 3. High-Level Architecture

```text
Browser / Mobile App
  |
  v
CDN + WAF + Bot Defense
  |
  v
Global Load Balancer
  |
  v
API Gateway / BFF
  |
  +--> Catalog Service ------> Catalog DB ----------+
  |                         \-> Search Index <------|-- CDC/Event Stream
  |
  +--> Cart Service ---------> Cart Store / Redis / KV
  |
  +--> Checkout Orchestrator
           |
           +--> Pricing Service
           +--> Inventory Service -> Inventory DB
           +--> Payment Service --> Payment Provider
           +--> Order Service ----> Orders DB
           +--> Outbox -----------> Kafka/Event Stream
                                      |
                                      +--> Fulfillment
                                      +--> Notification
                                      +--> Analytics
                                      +--> Search/Read Models
```

Core design split:

```text
Read-heavy discovery path:
  CDN + cache + search + read replicas + eventual updates

Write-critical checkout path:
  orchestrator + idempotency + conditional inventory reservation + payment state machine + order DB
```

---

## 4. Request Lifecycle - Product Browse

```text
1. User opens product page
2. Browser sends GET /products/{id}
3. CDN checks cache
4. If hit, returns cached page/API response/image
5. If miss, request reaches gateway
6. Gateway authenticates optional user context, applies rate limit
7. Product BFF calls catalog cache/search/read model
8. Catalog data returns, maybe slightly stale
9. Inventory display uses short-TTL availability estimate
10. Recommendations/reviews load asynchronously
11. Response returns with cache headers
12. Client renders page
13. RUM/APM logs LCP, latency, cache hit/miss
```

Chosen:

- CDN for static assets and public product content
- Redis/app cache for product details
- Elasticsearch/OpenSearch for search/filter
- async CDC from catalog source to search index
- short-TTL inventory display

Rejected:

| Wrong Choice | What Fails |
|---|---|
| every product page reads primary DB | database overload during traffic spikes |
| search directly from OLTP DB with complex filters | slow queries and DB contention |
| inventory display requires strict real-time read | browse latency and availability suffer |
| no CDN for images/assets | origin bandwidth and global latency explode |

---

## 5. Request Lifecycle - Search

```text
1. User searches "running shoes"
2. Gateway validates query, rate-limits bots
3. Search service queries OpenSearch/Elasticsearch
4. Ranking uses index fields and optional personalization
5. Results include product IDs, title, image, price snapshot
6. Product details are hydrated from cache/read model if needed
7. Response returns quickly
8. Click/search events are published asynchronously for analytics/recommendations
```

Consistency:

```text
Search index may lag catalog updates. That is acceptable if product detail and checkout validate
current state before purchase.
```

Wrong option:

```text
Make search strongly consistent with every catalog update.
```

What fails:

```text
Catalog writes become coupled to search indexing latency/failure. Availability and write throughput suffer.
```

Better:

```text
Use asynchronous indexing with freshness SLO, monitoring, and rebuild capability.
```

---

## 6. Request Lifecycle - Add To Cart

```text
1. User clicks Add to Cart
2. Client sends POST /cart/items with idempotency key
3. Gateway authenticates session and rate-limits
4. Cart service validates SKU exists using catalog read model
5. Cart service writes user cart to cart store
6. Cart response returns immediately
7. CartUpdated event is emitted asynchronously
```

Cart consistency:

- user should see own cart updates
- cross-device cart can merge
- cart may contain item that later becomes unavailable
- final price/inventory validated at checkout

Storage options:

| Option | Fit |
|---|---|
| Redis | fast cart, TTL, session-like |
| DynamoDB/Cassandra | highly available cart by userId |
| relational DB | simpler MVP, transactional cart/order |
| local browser cart | anonymous fallback, not source of truth |

Chosen at scale:

```text
Key-value/document cart store partitioned by userId, with read-your-writes behavior where possible
and merge/conflict strategy for multi-device updates.
```

Wrong option:

```text
Treat cart item as inventory reservation.
```

What fails:

```text
Users can hoard stock by adding to cart and never checking out.
```

Better:

```text
Cart is intent. Inventory reservation happens during checkout with TTL.
```

---

## 7. Request Lifecycle - Checkout

```text
1. User clicks Place Order
2. Client sends POST /checkout with session cookie, CSRF token, idempotency key, traceparent
3. Gateway authenticates, authorizes, rate-limits, validates size/schema
4. Checkout orchestrator checks idempotency table
5. Cart is loaded and locked/logically snapshotted
6. Pricing service recalculates price, coupon, tax, shipping
7. Inventory service reserves stock with conditional write and reservation TTL
8. Payment service creates/authorizes payment intent with provider idempotency key
9. Order service creates order with state PAYMENT_AUTHORIZED or PAYMENT_PENDING
10. Transactional outbox records events
11. Response returns orderId and current state
12. Async workers handle notification, fulfillment, analytics, search updates
```

Important:

```text
Checkout is a workflow, not one giant distributed transaction.
```

Chosen:

- checkout orchestrator
- idempotency key
- inventory reservation TTL
- payment intent state machine
- order state machine
- transactional outbox
- async side effects

Rejected:

| Wrong Choice | What Fails |
|---|---|
| one synchronous mega-call to every downstream system | low availability and high p99 |
| 2PC across inventory, payment provider, order, shipping | provider incompatibility and blocking |
| async payment but show paid immediately | incorrect order state |
| no idempotency on checkout | duplicate order/charge |
| cache controls inventory reservation | oversell or lost reservation |

---

## 8. Inventory Design

Inventory states:

```text
available
reserved
sold
released
returned
```

Reservation flow:

```text
UPDATE inventory
SET available = available - :qty,
    reserved = reserved + :qty
WHERE sku_id = :sku
  AND available >= :qty;
```

If affected rows = 1:

```text
reservation succeeds
```

If affected rows = 0:

```text
out of stock or insufficient inventory
```

Reservation record:

```text
reservation_id
sku_id
order_attempt_id
qty
status: RESERVED | CONFIRMED | RELEASED | EXPIRED
expires_at
```

Flash-sale options:

| Option | Fit | Risk |
|---|---|---|
| DB conditional update | simple, strong per SKU | hot row under massive sale |
| per-SKU queue | serializes reservations | queue latency |
| token/preallocation buckets | high throughput | complexity and reconciliation |
| limited oversell | some retail cases | customer disappointment |
| waitlist | preserves UX | operational flow |

Chosen:

```text
Conditional reservation for normal SKUs, plus hot-SKU admission control or per-SKU reservation queue
for flash sales.
```

Wrong option:

```text
Use eventually consistent inventory counters for checkout commit.
```

What fails:

```text
Multiple regions can sell the same last unit.
```

Better:

```text
Use a single reservation authority per SKU/region or a carefully partitioned token allocation model.
```

---

## 9. Payment Design Inside E-Commerce

Payment states:

```text
CREATED
AUTHORIZING
AUTHORIZED
AUTH_UNKNOWN
CAPTURED
FAILED
CANCELLED
REFUNDED
```

Payment flow:

```text
1. Create payment intent
2. Call provider with idempotency key
3. If success, mark authorized/captured
4. If timeout, mark AUTH_UNKNOWN
5. Webhook/reconciliation confirms final state
6. Order state updates from payment state
```

Wrong option:

```text
On provider timeout, call authorize again immediately with a new provider key.
```

What fails:

```text
User may be charged twice.
```

Better:

```text
Use the same provider idempotency key, mark unknown, query provider, and reconcile with webhooks.
```

---

## 10. Order State Machine

Order states:

```text
CREATED
INVENTORY_RESERVED
PAYMENT_PENDING
PAYMENT_AUTHORIZED
CONFIRMED
FULFILLMENT_REQUESTED
SHIPPED
DELIVERED
CANCELLED
REFUND_PENDING
REFUNDED
```

Rules:

- state transitions must be legal
- order creation must be idempotent
- every payment/inventory transition recorded
- outbox emits events after committed state
- stuck orders monitored

Wrong option:

```text
Use a free-form status string updated by any service.
```

What fails:

```text
Invalid states, race conditions, and impossible debugging.
```

Better:

```text
Use an explicit state machine with transition ownership and audit history.
```

---

## 11. Data Storage Choices

| Domain | Choice | Why |
|---|---|---|
| catalog source | relational/document | seller/product management |
| product read model | cache + search index | fast reads/search |
| images/media | object storage + CDN | global static delivery |
| cart | KV/document by userId | high availability and simple access |
| inventory | relational/strong KV by SKU/location | conditional reservation |
| orders | relational/distributed SQL | transactions, state machine |
| payments | relational/distributed SQL | idempotency and audit |
| events | Kafka/Pulsar | async propagation |
| analytics | warehouse/lake | reporting |
| sessions/rate limits | Redis | low-latency ephemeral state |

Sharding:

| Domain | Shard Key |
|---|---|
| cart | userId |
| orders | userId or orderId with secondary index |
| inventory | skuId + fulfillment location |
| payments | paymentId/orderId, often region scoped |
| events | aggregateId/orderId/SKU depending topic |
| search | index partitions by product/category/routing |

Wrong option:

```text
Use one orders table without partitioning/index strategy at 50M orders/day.
```

What fails:

```text
Storage, index maintenance, backups, and query latency become painful.
```

Better:

```text
Partition by time/region/order ID and design access-specific indexes/read models.
```

---

## 12. Multi-Region Strategy

Read path:

- active-active regions
- CDN edge
- regional search clusters
- regional caches
- async catalog replication

Write path:

- order has home region
- inventory reservation authority per SKU/location
- payment routed by region/provider capability
- events replicated asynchronously

Failure handling:

| Failure | Behavior |
|---|---|
| search region down | route to another cluster or degraded search |
| recommendations down | hide/degrade recommendations |
| cart store partial outage | fallback to cached/local cart or degraded |
| payment provider down | route to alternate provider if safe |
| inventory hot SKU overloaded | waiting room/admission control |
| region down | read traffic fails over, in-flight checkout reconciled |

Wrong option:

```text
Allow active-active inventory writes for same SKU in every region with no coordination.
```

What fails:

```text
The same units can be sold in multiple regions.
```

Better:

```text
Use regional inventory allocation, SKU home region, token buckets, or strong reservation authority.
```

---

## 13. Observability View

Trace for checkout:

```text
POST /checkout
  gateway.auth
  gateway.rate_limit
  checkout.idempotency_check
  cart.load
  pricing.quote
  inventory.reserve
  payment.authorize
  order.create
  outbox.write
```

Datadog dashboards:

- browse availability
- CDN hit rate
- search p95/p99
- cache hit ratio
- checkout funnel
- inventory reservation failure
- payment provider latency
- order stuck states
- Kafka lag/DLQ
- DB connection pool/lock wait

Logs should show:

- `trace_id`
- `request_id`
- `user_id_hash`
- `cart_id`
- `order_id`
- `reservation_id`
- `payment_intent_id`
- `idempotency_key`
- `sku_id`
- `provider`
- `state_transition`

---

## 14. Chosen Final Architecture

Final choice:

```text
Use availability-first architecture for discovery and cart, with CDN, search, caches, read models,
and async propagation. Use a checkout orchestrator for write-critical workflow, with idempotency,
inventory reservation, payment state machine, order state machine, transactional outbox, and async
side effects. Do not force strict global consistency across the entire commerce platform.
```

Why this is right:

- most traffic is read-heavy
- stale discovery data is tolerable
- cache/search/CDN keep system available
- checkout still protects money/inventory correctness
- async events decouple non-critical side effects
- observability can detect stale indexes, stuck orders, and failed workflows

Why the tempting alternatives are wrong:

| Alternative | Why Rejected |
|---|---|
| global strict transaction for everything | latency and availability collapse |
| eventual consistency for all checkout writes | oversell/double charge risk |
| one database for all read/search/write workloads | bottleneck and poor fit |
| microservice for every tiny entity | distributed complexity without benefit |
| no workflow orchestrator | checkout state becomes hard to reason about |

---

## 15. Strong Interview Answer

```text
For high-traffic e-commerce, I would not use one consistency model everywhere. Product discovery,
search, recommendations, and reviews are read-heavy and can tolerate eventual consistency, so I
would use CDN, search indexes, Redis caches, read replicas, and async CDC/event streams. Checkout
changes the correctness requirement. Inventory reservation uses conditional writes or a reservation
authority, payment uses idempotent payment intents and provider reconciliation, and order creation
uses a state machine with a transactional outbox. I would reject 2PC across payment, inventory, and
shipping because it hurts availability and external providers do not participate cleanly. The system
stays highly available for browsing while preserving correctness where business damage is real.
```

---

## 16. Revision Notes

- One-line summary: E-commerce is AP/eventual for discovery, stronger consistency for checkout boundaries.
- Three keywords: CDN, reservation, outbox.
- One interview trap: saying "eventual consistency everywhere" for checkout.
- Memory trick: stale product page is okay; stale money or final stock is not.

