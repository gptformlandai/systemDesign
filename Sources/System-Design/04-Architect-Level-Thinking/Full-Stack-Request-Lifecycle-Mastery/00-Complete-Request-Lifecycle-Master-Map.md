# 00 - Complete Request Lifecycle Master Map

> Goal: trace one request from the moment a user action forms it to the moment the response is rendered, logged, traced, cached, retried, or rejected.

---

## 1. Beginner Mental Model

A web request feels simple:

```text
User clicks Buy -> browser sends request -> server processes -> database saves -> response returns
```

At production scale, the request passes through many decision points:

```text
User intent
  -> client runtime
  -> network resolution
  -> edge protection
  -> traffic routing
  -> API boundary
  -> service logic
  -> state systems
  -> async side effects
  -> observability
  -> response/render
```

The mastery move is to ask at every point:

- Who owns this step?
- What can fail here?
- What latency is added?
- What security check happens?
- What consistency model is used?
- What gets logged/traced/metriced?
- What is the fallback?

---

## 2. End-To-End Request Flow

```text
1. User performs an action
2. Client validates and forms request
3. Client attaches headers, cookies, auth, trace context
4. Browser/mobile stack resolves DNS
5. Client connects through TCP/TLS or QUIC
6. CDN/edge receives request
7. WAF/bot layer inspects request
8. Edge cache serves or forwards
9. Global load balancer chooses region
10. Regional load balancer chooses ingress
11. API gateway authenticates, authorizes, routes, throttles
12. Service mesh/proxy applies mTLS, retries, circuit breaking
13. Backend service validates business request
14. Backend checks cache or writes through state path
15. Database/search/object store/queue participates
16. Downstream services or events run side effects
17. Observability emits logs, metrics, traces, audit records
18. Response is serialized, compressed, and returned
19. Client receives, caches, renders, retries, or shows error
```

Short interview line:

```text
I split the lifecycle into client, edge, gateway, service, data, async, observability, and response
paths. Each layer has its own security, latency, consistency, and failure behavior.
```

---

## 3. Step 1 - Request Formation On The Client

Client sources:

| Client | Example |
|---|---|
| Browser SPA | React/Next checkout page calls `/api/checkout` |
| Server-rendered page | HTML form posts to backend |
| Mobile app | iOS/Android calls API gateway |
| Partner API client | Server-to-server signed request |
| Internal service | gRPC/HTTP request inside VPC |

Client does:

- validates obvious input
- reads session/cookie/token
- attaches CSRF token if needed
- attaches idempotency key for retries on writes
- attaches trace context
- chooses timeout and retry policy
- serializes body as JSON, protobuf, form data, or multipart

Common headers:

| Header | Purpose |
|---|---|
| `Authorization` | bearer token, signed token, or credential reference |
| `Cookie` | browser session, CSRF cookie, preference cookie |
| `Content-Type` | request body format |
| `Accept` | response format |
| `Idempotency-Key` | duplicate write protection |
| `X-Request-ID` | request correlation |
| `traceparent` | W3C trace context |
| `baggage` | trace metadata, use carefully |
| `User-Agent` | client/browser identity |
| `Origin` | browser CORS/security origin |
| `Referer` | previous page, privacy-sensitive |
| `Accept-Encoding` | compression support |

Wrong option:

```text
Generate a new idempotency key on every retry.
```

What fails:

```text
The server sees each retry as a new operation and may create duplicate orders, duplicate transfers,
or duplicate payments.
```

Better:

```text
Generate one idempotency key for one logical user action and reuse it across retries.
```

---

## 4. Step 2 - DNS, Connection, TLS, And Protocol

DNS:

- resolves `www.example.com` to an edge/global endpoint
- may use geo-DNS or latency-based routing
- TTL controls how quickly clients move after failover

Connection options:

| Protocol | Common Use | Notes |
|---|---|---|
| HTTP/1.1 | Simple APIs, legacy clients | one request per connection at a time unless multiple connections |
| HTTP/2 | Modern web/API | multiplexing, header compression |
| HTTP/3 | QUIC over UDP | lower connection setup cost and better behavior on some networks |
| gRPC | service-to-service APIs | HTTP/2, protobuf, streaming |
| WebSocket | bidirectional realtime | long-lived connection |
| SSE | server-to-client events | simpler one-way streaming |

TLS:

- authenticates server
- encrypts traffic
- can support mTLS for service-to-service or partner APIs
- termination can happen at CDN, load balancer, ingress, or service proxy

Wrong option:

```text
Terminate TLS at the edge and send plain HTTP across internal networks with no compensating controls.
```

What fails:

```text
Internal traffic can be observed or modified if the private network is compromised. Compliance and
zero-trust requirements may fail.
```

Better:

```text
Use TLS externally and mTLS/service identity internally for sensitive environments.
```

---

## 5. Step 3 - CDN, Edge Cache, WAF, And Bot Defense

Edge layer responsibilities:

| Layer | Role |
|---|---|
| CDN | cache static content and some public dynamic content close to users |
| Edge compute | lightweight redirects, header normalization, A/B routing |
| WAF | block known attack signatures and bad request patterns |
| Bot defense | detect scraping, credential stuffing, fake checkout attacks |
| DDoS protection | absorb volumetric attacks |

CDN cache fit:

| Good Fit | Bad Fit |
|---|---|
| static assets | personalized checkout page |
| product images | account balance |
| public product pages with short TTL | sensitive PII |
| catalog browse pages | payment result response |

Important headers:

| Header | Meaning |
|---|---|
| `Cache-Control` | browser/CDN caching policy |
| `ETag` | entity version for revalidation |
| `Last-Modified` | timestamp-based revalidation |
| `Vary` | which request headers affect cache key |
| `Content-Security-Policy` | XSS blast-radius reduction |
| `Strict-Transport-Security` | force HTTPS |
| `X-Content-Type-Options` | prevent MIME sniffing |

Wrong option:

```text
Cache all GET responses at CDN for high performance.
```

What fails:

```text
Personalized or sensitive responses can leak between users. Stale account/order/payment states can
be shown incorrectly.
```

Better:

```text
Cache public immutable assets aggressively, cache public catalog with TTL/revalidation, and mark
private user/account responses as no-store or private.
```

---

## 6. Step 4 - Global And Regional Load Balancing

Global load balancing decides:

- nearest healthy region
- failover region
- weighted traffic split for migrations
- blue/green or canary deployment target

Regional load balancing decides:

- healthy availability zone
- healthy ingress/load balancer target
- backend pool
- connection draining during deploys

Algorithms:

| Algorithm | Use |
|---|---|
| round robin | simple equal distribution |
| weighted round robin | shift traffic gradually |
| least connections | long-running or uneven requests |
| latency-based | route to fastest endpoint |
| consistent hashing | sticky routing/cache affinity |
| random two choices | scalable load spreading |

Wrong option:

```text
Use sticky sessions to one app server for all user state.
```

What fails:

```text
Server failure drops user state, scaling becomes uneven, deployments become fragile, and hot users
can overload specific instances.
```

Better:

```text
Keep app servers stateless where possible. Put shared session state in a durable/session store or
use signed tokens with revocation strategy.
```

---

## 7. Step 5 - API Gateway And Request Boundary

Gateway responsibilities:

- route requests to services
- authenticate user/client
- authorize coarse access
- validate request shape
- enforce rate limits and quotas
- terminate external protocol
- transform protocol or headers
- emit access logs
- inject correlation/trace IDs
- apply request/response size limits

Gateway is good for:

- cross-cutting policies
- simple auth enforcement
- routing and versioning
- edge throttling
- request normalization

Gateway is bad for:

- complex business logic
- long-running workflows
- deep domain transactions
- hidden coupling between services

Wrong option:

```text
Put checkout orchestration business logic inside the API gateway.
```

What fails:

```text
Gateway becomes a domain monolith, hard to test, hard to version, hard to observe, and risky to
change for unrelated API traffic.
```

Better:

```text
Use gateway for boundary concerns and route to a checkout/order orchestration service for domain workflow.
```

---

## 8. Step 6 - Rate Limiting, Quotas, And Overload Control

Rate limiting protects:

- service capacity
- database capacity
- external provider quotas
- user fairness
- fraud/abuse boundaries

Algorithms:

| Algorithm | Best For | Trade-off |
|---|---|---|
| fixed window | simple quotas | boundary bursts |
| sliding window log | precise limits | memory cost |
| sliding window counter | balanced API limits | approximate |
| token bucket | allows bursts | needs token storage |
| leaky bucket | smooth output | can add queueing delay |
| concurrency limit | protect scarce resources | rejects when saturated |

Where to apply:

| Layer | Limit Type |
|---|---|
| CDN/WAF | IP/bot/geo/path limits |
| API gateway | user/client/API key quota |
| service | business-specific limits |
| DB/client pool | concurrency and queue depth |
| queue consumer | processing rate/backpressure |

Wrong option:

```text
Only rate-limit by IP address.
```

What fails:

```text
NATed users share IPs, attackers rotate IPs, and authenticated abuse from one account can bypass
useful limits.
```

Better:

```text
Combine IP, user ID, device/client ID, API key, route, and risk score, with separate limits for
anonymous and authenticated traffic.
```

---

## 9. Step 7 - Backend Service Execution

Typical service flow:

```text
1. Accept request with correlation IDs
2. Deserialize and validate schema
3. Authenticate context from gateway/service identity
4. Authorize domain action
5. Check idempotency for write operations
6. Read cache or source-of-truth
7. Execute business logic
8. Write transaction or enqueue event
9. Emit audit/event/log/metrics
10. Return response
```

Backend patterns:

| Pattern | Use |
|---|---|
| layered monolith | early product, low operational overhead |
| modular monolith | strong boundaries without distributed complexity |
| microservices | independent scale/ownership/failure boundaries |
| BFF | frontend-specific aggregation |
| CQRS | separate read and write models |
| event-driven | async propagation and decoupling |
| orchestration | central workflow coordination |
| choreography | services react to events |

Wrong option:

```text
Start with dozens of microservices before data ownership and team boundaries are clear.
```

What fails:

```text
You create distributed transactions, network latency, versioning problems, tracing complexity, and
operational cost before the product needs them.
```

Better:

```text
Start modular, split services around clear ownership, scale, data boundaries, and failure isolation.
```

---

## 10. Step 8 - Cache Path

Cache types:

| Cache | Example | Use |
|---|---|---|
| browser cache | static CSS/JS/image | reduce repeat downloads |
| CDN cache | product images, public pages | global latency reduction |
| gateway cache | safe GET aggregation | protect backend |
| app local cache | config, small hot data | micro-latency |
| distributed cache | Redis/Memcached | shared hot data |
| database buffer cache | DB internal | query/storage acceleration |
| search index | Elasticsearch/OpenSearch | derived query model |

Cache patterns:

| Pattern | Meaning |
|---|---|
| cache-aside | app reads/writes cache around DB |
| read-through | cache layer loads from DB |
| write-through | write cache and DB together |
| write-behind | write cache first, DB later |
| refresh-ahead | refresh before expiry |
| stale-while-revalidate | serve stale while refreshing |

Wrong option:

```text
Use cache as the source of truth for checkout inventory or account balance.
```

What fails:

```text
Eviction, stale values, race conditions, and cache loss can create oversell or incorrect financial state.
```

Better:

```text
Use cache for read acceleration. Commit correctness-critical writes to a transactional source of truth.
```

---

## 11. Step 9 - Database And State Path

Data systems:

| System | Fit |
|---|---|
| PostgreSQL/MySQL | relational transactions, orders, payments, ledgers |
| DynamoDB/Cassandra | high-scale key-value/wide-column, high availability |
| MongoDB | document aggregates, flexible schema |
| Redis | cache, counters, rate limits, ephemeral state |
| Elasticsearch/OpenSearch | search and filtering |
| Kafka/Pulsar | event log, async propagation |
| S3/object storage | images, receipts, exports, logs |
| data warehouse/lake | analytics and reporting |
| graph DB | relationships, fraud rings, recommendations |
| time-series DB | metrics, telemetry |

State categories:

| State | Consistency Need |
|---|---|
| product catalog display | eventual usually fine |
| cart | eventual or read-your-writes |
| inventory reservation | strong per SKU or partition |
| order state | strong state machine |
| payment state | strict, idempotent, auditable |
| ledger | strict ACID/append-only |
| search index | derived/eventual |
| recommendation | derived/eventual |

Wrong option:

```text
Put every data access pattern into one relational database forever.
```

What fails:

```text
Search, analytics, caching, event replay, object storage, and high-scale counters overload the
transactional database and force poor query patterns.
```

Better:

```text
Keep a clear source of truth and build derived stores for search, cache, analytics, and read models.
```

---

## 12. Step 10 - Async Side Effects

Async is used for:

- email/SMS/push notifications
- search index updates
- analytics events
- recommendation signals
- inventory projections
- payment webhook processing
- fraud scoring
- fulfillment tasks
- cache invalidation

Message systems:

| Tool | Fit |
|---|---|
| Kafka | high-throughput event log, replay, stream processing |
| SQS | managed queues, simple async jobs |
| SNS/PubSub | fanout notifications |
| RabbitMQ | routing patterns, work queues |
| Pulsar | multi-tenant messaging, geo-replication |
| Kinesis | AWS streaming ingestion |

Wrong option:

```text
Send the database commit and Kafka event as two independent operations with no recovery plan.
```

What fails:

```text
The DB write can succeed but the event publish can fail, leaving downstream systems permanently stale.
```

Better:

```text
Use transactional outbox, CDC, or a durable workflow engine so committed state and emitted events
can be reconciled.
```

---

## 13. Step 11 - Observability During The Request

Signals:

| Signal | Question Answered |
|---|---|
| logs | what happened for this request? |
| metrics | how often, how slow, how many errors? |
| traces | where was time spent across services? |
| profiles | what code consumed CPU/memory? |
| RUM | what did real users experience? |
| audits | who did what to sensitive state? |

Trace path:

```text
browser traceparent
  -> CDN/gateway access log
  -> API span
  -> service spans
  -> cache span
  -> DB span
  -> queue publish span
  -> worker consume span
```

Important IDs:

| ID | Purpose |
|---|---|
| request ID | one inbound request correlation |
| trace ID | distributed trace across services |
| span ID | one operation inside trace |
| user ID | actor, hashed/pseudonymized where needed |
| session ID | user session correlation |
| order ID | business entity |
| payment ID | payment entity |
| idempotency key | duplicate-write guard |

Wrong option:

```text
Log only plain strings like "checkout failed".
```

What fails:

```text
You cannot filter by order ID, trace ID, user segment, provider, shard, region, or failure class.
```

Better:

```text
Emit structured logs with stable fields and correlate them with traces and metrics.
```

---

## 14. Step 12 - Response Path

Backend response work:

- choose status code
- serialize body
- set cache headers
- compress response
- include request ID
- include retry-after when throttled
- set cookies if needed
- avoid leaking sensitive internal details

Common status codes:

| Code | Meaning |
|---:|---|
| 200 | success |
| 201 | resource created |
| 202 | accepted for async processing |
| 204 | success with no body |
| 400 | bad request |
| 401 | unauthenticated |
| 403 | unauthorized |
| 404 | not found |
| 409 | conflict |
| 422 | semantic validation failure |
| 429 | rate limited |
| 500 | internal error |
| 502 | bad gateway |
| 503 | unavailable |
| 504 | gateway timeout |

Wrong option:

```text
Return 200 OK for every business failure and encode errors only in body.
```

What fails:

```text
Clients, gateways, monitors, retry libraries, and dashboards cannot classify failures correctly.
```

Better:

```text
Use HTTP semantics for transport/API failure and domain error codes for business-specific detail.
```

---

## 15. Complete Example - E-Commerce Checkout Request

```text
1. User clicks Place Order
2. Browser sends POST /checkout with session cookie, CSRF token, idempotency key, traceparent
3. DNS resolves to CDN/global edge
4. WAF checks bot/fraud signatures
5. Global load balancer routes to nearest healthy region
6. API gateway validates auth, request size, and quota
7. Checkout service validates cart and price quote
8. Idempotency table checks whether this checkout already completed
9. Inventory service reserves stock with conditional write
10. Payment service creates/uses payment intent
11. Order service creates order state
12. Transactional outbox emits OrderCreated/PaymentPending
13. Notification/search/fulfillment update asynchronously
14. Response returns order ID and status
15. Client renders confirmation or pending payment state
16. Trace shows gateway -> checkout -> inventory -> payment -> order -> DB
```

Consistency:

```text
Product browsing can be eventual. Checkout reservation and payment cannot be casual eventual state.
```

---

## 16. Complete Example - Finance Transfer Request

```text
1. User submits transfer
2. Client sends POST /transfers with auth token, idempotency key, device risk context
3. Edge allows but does not cache
4. Gateway authenticates and rate-limits
5. Transfer service validates account ownership and risk
6. Ledger service opens strict transaction
7. Idempotency record is inserted or reused
8. Debit and credit ledger entries are written atomically
9. Balance projection updates in same transaction or from committed ledger
10. Audit record is written
11. Outbox event is recorded for notifications/reporting
12. Response returns committed/pending/rejected
13. Reconciliation later verifies ledger and external settlement
```

Consistency:

```text
If the system cannot confirm correctness, it should return pending or reject rather than fake success.
```

---

## 17. Master Checklist

Use this on every design:

| Layer | Questions |
|---|---|
| Client | What headers, timeout, retry, idempotency, and cache behavior? |
| DNS/TLS | How is traffic routed and encrypted? |
| CDN/WAF | What is cached, blocked, or challenged? |
| Load balancer | What region/zone/service receives traffic? |
| Gateway | What auth, quota, validation, and routing happen? |
| Service | What business invariant is protected? |
| Cache | Is this acceleration or source of truth? |
| DB | What consistency, isolation, replication, and shard strategy? |
| Queue | What side effects are async? How are duplicates handled? |
| Workflow | Saga, 2PC, orchestration, choreography, or simple transaction? |
| Observability | What trace/log/metric/audit proves behavior? |
| Response | What status, cache headers, retry guidance, and UX state? |

---

## 18. Revision Notes

- One-line summary: A production request is a chain of security, routing, consistency, state, and observability decisions.
- Three keywords: boundary, state, trace.
- One interview trap: jumping directly from API to database and skipping edge, gateway, cache, async, and failure behavior.
- Memory trick: follow the request like a detective: who touched it, what changed, what can prove it?

