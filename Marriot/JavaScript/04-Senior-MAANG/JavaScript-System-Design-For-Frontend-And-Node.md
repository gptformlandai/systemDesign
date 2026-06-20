# JavaScript System Design For Frontend And Node

> Goal: design production-grade JavaScript systems across browser, frontend application architecture, Node.js backends, BFF layers, SSR/CSR/SSG/ISR, edge functions, APIs, caching, realtime, observability, security, scaling, and MAANG interview scenarios.

---

## 1. How To Use This Sheet

This file is the architecture bridge for the JavaScript track.

Use it after learning:

- Core JavaScript.
- Async and event loop.
- Browser APIs.
- Node.js production.
- Security.
- Testing.
- Performance and memory debugging.
- Production incident case studies.

This sheet answers the senior interview question:

> How do you design a JavaScript system, not just write JavaScript code?

You should be able to design:

- A scalable frontend app.
- A Node.js API service.
- A backend-for-frontend layer.
- A server-rendered app.
- A realtime dashboard.
- A file upload/download system.
- A secure auth flow.
- A cache-aware data loading flow.
- A production observable system.

---

## 2. The JavaScript Architecture Mental Model

JavaScript is no longer only a browser language.

Modern JavaScript systems can run in:

- Browser main thread.
- Browser workers.
- Service workers.
- Node.js servers.
- Serverless functions.
- Edge runtimes.
- Build tools.
- CLI jobs.
- Queue workers.
- Test runners.

System design requires knowing where work belongs.

Senior design question:

> Should this work happen in the browser, at the edge, in a BFF, in a Node API, in a worker, in a queue, in the database, or in a CDN/cache?

That placement decision affects:

- Latency.
- Cost.
- Security.
- Availability.
- Consistency.
- User experience.
- Developer velocity.
- Observability.
- Operational risk.

---

## 3. One-Line Architecture Definitions

| Term | Meaning |
|---|---|
| CSR | Client-side rendering; browser downloads JS and renders UI. |
| SSR | Server-side rendering; server returns HTML for a route. |
| SSG | Static site generation; HTML generated at build time. |
| ISR | Incremental static regeneration; static pages refreshed after deploy. |
| Hydration | Browser attaches JS behavior to server-rendered HTML. |
| BFF | Backend for frontend; API layer shaped for one frontend experience. |
| Edge runtime | Code executed near users on CDN/edge infrastructure. |
| API gateway | Entry point handling routing, auth, rate limits, policies. |
| CDN | Distributed cache for static and sometimes dynamic content. |
| RUM | Real user monitoring from browsers. |
| Contract test | Test verifying provider and consumer API expectations. |
| Idempotency | Repeating an operation safely without duplicate side effects. |
| Backpressure | System slows/rejects work instead of collapsing. |
| Eventual consistency | System becomes consistent later, not immediately. |
| Optimistic UI | UI updates before server confirmation, with rollback if needed. |

---

## 4. Senior Architecture Answer Pattern

Use this structure in interviews:

1. Clarify requirements.
2. Define users and core flows.
3. Choose rendering strategy.
4. Define data ownership and API boundaries.
5. Design frontend state and caching.
6. Design backend services and persistence.
7. Add security controls.
8. Add performance strategy.
9. Add reliability and failure handling.
10. Add observability.
11. Discuss trade-offs.
12. Mention phased rollout.

Strong opening:

> I will first separate product requirements from technical constraints. Then I will choose where each piece of work belongs: browser, edge, BFF, Node API, queue, cache, database, or CDN. My design will optimize user-perceived latency, correctness, security, operability, and rollout safety.

---

## 5. Requirements To Clarify

Ask these before designing.

### Product Requirements

- What is the core user flow?
- Is the experience read-heavy, write-heavy, or interactive?
- Is SEO required?
- Is offline support required?
- Is realtime required?
- Are users authenticated?
- Are there admin and customer views?
- Is mobile performance critical?

### Scale Requirements

- Daily active users.
- Requests per second.
- Peak traffic pattern.
- Geographic distribution.
- Data size per page.
- File size limits.
- Realtime connection count.
- Expected latency targets.

### Reliability Requirements

- Availability target.
- Critical flows.
- Recovery time objective.
- Data correctness requirements.
- Failure tolerance.
- Dependency fallback behavior.

### Security Requirements

- Authentication model.
- Authorization model.
- PII or payment data.
- Compliance constraints.
- Threat model.
- Audit logging.

---

## 6. Rendering Strategy Decision Table

| Strategy | Best For | Trade-Offs |
|---|---|---|
| CSR | authenticated apps, dashboards, highly interactive tools | slower first load, SEO weaker, larger JS cost |
| SSR | SEO pages, personalized first render, faster first content | server cost, hydration complexity, cache complexity |
| SSG | docs, marketing, catalog pages with infrequent changes | stale until rebuild unless paired with regeneration |
| ISR | large content sites needing freshness without full rebuild | cache invalidation and stale behavior complexity |
| Edge SSR | low-latency global personalization | runtime limits, data access constraints, cost |
| Hybrid | most real apps | more architectural complexity |

Senior answer:

> Rendering is not a framework preference. It is a product trade-off between SEO, first content, interactivity, freshness, infrastructure cost, and operational complexity.

---

## 7. CSR Architecture

Client-side rendering means the browser downloads HTML shell, JavaScript, and data, then renders UI.

Good fit:

- Authenticated dashboards.
- Internal tools.
- Complex interactive apps.
- Experiences where SEO is not important.
- Apps with rich client state.

Weaknesses:

- Blank shell if JS fails.
- More JavaScript cost on user device.
- Poor first content on slow networks.
- Requires strong loading/error states.

CSR flow:

1. Browser requests HTML shell.
2. Browser downloads JS/CSS.
3. JS executes.
4. App requests data.
5. UI renders.
6. User interactions update state and call APIs.

Design checklist:

- Route-level code splitting.
- API request deduplication.
- Loading and error boundaries.
- Client cache strategy.
- Web Vitals monitoring.
- Source maps.
- Auth token/session strategy.

---

## 8. SSR Architecture

Server-side rendering returns HTML for a route.

Good fit:

- SEO-heavy pages.
- Content pages.
- Product/catalog pages.
- Pages needing fast first meaningful content.
- Personalized pages where server can safely render initial state.

SSR flow:

1. Browser requests route.
2. Server fetches required data.
3. Server renders HTML.
4. Browser receives visible content.
5. Browser downloads JS.
6. Hydration attaches event handlers.
7. Client continues as interactive app.

Trade-offs:

- Server cost per request.
- Hydration mismatch risk.
- Caching is more nuanced.
- Slow backend data can delay HTML.
- Need server and client render consistency.

Senior answer:

> SSR improves first content and SEO, but it does not remove JavaScript cost. Hydration and data fetching still need performance design.

---

## 9. SSG And ISR Architecture

Static generation creates HTML ahead of time.

Good fit:

- Documentation.
- Blog/content pages.
- Landing pages.
- Large catalog pages where stale content is acceptable.

ISR-like regeneration adds controlled freshness.

Useful when:

- There are too many pages to rebuild constantly.
- Content updates are frequent but not instant-critical.
- CDN caching is important.

Trade-offs:

- Stale content windows.
- Regeneration race conditions.
- Cache invalidation complexity.
- Preview/draft content complexity.

Design question:

> How fresh does the user need the data to be?

That answer drives static vs dynamic rendering.

---

## 10. Edge Rendering

Edge rendering runs logic near users.

Good fit:

- Lightweight personalization.
- Header/cookie based routing.
- A/B testing.
- Geo-aware content.
- Auth redirects.
- Cache-key normalization.

Bad fit:

- Heavy CPU.
- Large dependencies.
- Long-running tasks.
- Direct private database access in many designs.
- Node-specific APIs not supported by edge runtime.

Edge rule:

> Keep edge code small, fast, stateless, and cache-aware.

Example edge decision:

```js
export default async function handleRequest(request) {
  const country = request.headers.get("x-country") ?? "US";
  const url = new URL(request.url);

  if (country === "IN" && url.pathname === "/deals") {
    url.pathname = "/in/deals";
    return Response.redirect(url, 302);
  }

  return fetch(request);
}
```

---

## 11. Hydration Design

Hydration is often the hidden cost of SSR.

Problems:

- Server HTML is visible but not interactive.
- Hydration blocks main thread.
- Mismatches cause errors.
- Too much client JS reduces SSR benefits.

Common hydration risks:

- Rendering `Date.now()` on server and client.
- Random values in render.
- Browser-only APIs in server render.
- Feature flag mismatch.
- Locale/timezone mismatch.
- Different data snapshots.

Design choices:

- Hydrate only interactive parts where framework supports it.
- Defer non-critical widgets.
- Keep render deterministic.
- Pass server data explicitly.
- Monitor hydration errors.

---

## 12. Frontend Architecture Layers

A maintainable frontend usually has layers.

| Layer | Responsibility |
|---|---|
| Route/page | route params, layout, initial data needs |
| Feature module | business workflow, state, actions |
| UI components | rendering and interaction primitives |
| API client | HTTP details, auth headers, error mapping |
| Client cache | query state, dedupe, invalidation |
| Domain utilities | pure business transformations |
| Observability | error reporting, RUM, logs |

Avoid mixing:

- API fetch logic inside deeply nested buttons.
- Business rules inside CSS components.
- Auth decisions only in UI.
- Global state for every local field.

---

## 13. Node Backend Architecture Layers

A maintainable Node service usually has layers.

| Layer | Responsibility |
|---|---|
| Route/controller | HTTP shape, status codes, request parsing |
| Validation | schema and boundary checks |
| Service/use case | business workflow |
| Repository/client | persistence or dependency access |
| Domain model | business rules and invariants |
| Observability | logs, metrics, traces |
| Error handling | safe response and internal logging |

Example shape:

```js
app.post("/bookings", asyncHandler(async (req, res) => {
  const command = createBookingSchema.parse(req.body);
  const booking = await bookingService.createBooking(command, req.user);
  res.status(201).json(serializeBooking(booking));
}));
```

Design principle:

> Routes should orchestrate HTTP. Business decisions should live below them.

---

## 14. BFF Pattern

BFF means Backend for Frontend.

It is a backend API shaped specifically for one frontend experience.

Good fit:

- Frontend needs data from many services.
- Mobile and web need different payloads.
- API composition is too expensive in browser.
- You need server-side auth/session handling.
- You want to hide internal services.

BFF responsibilities:

- Compose service calls.
- Shape payload for frontend.
- Enforce auth at server boundary.
- Reduce client round trips.
- Apply caching safely.
- Handle fallbacks for optional sections.

BFF caution:

- Do not let BFF become a giant unowned monolith.
- Keep domain ownership clear.
- Avoid duplicating business rules from core services.

---

## 15. API Gateway vs BFF

| Concern | API Gateway | BFF |
|---|---|---|
| Main role | infrastructure entry point | experience-specific composition |
| Routing | yes | sometimes |
| Auth policy | yes | yes, experience-aware |
| Rate limiting | yes | sometimes |
| Payload shaping | usually no | yes |
| Business orchestration | no or minimal | yes, but carefully |
| Frontend-specific needs | limited | primary purpose |

Senior answer:

> API gateway is a platform edge concern. BFF is a product experience concern. They can coexist but should not be confused.

---

## 16. Data Fetching Strategies

Frontend data can be fetched:

- On the server before render.
- In the browser after route load.
- In parallel with route transition.
- Lazily when component becomes visible.
- Optimistically before server confirmation.
- Through streaming or subscriptions.

Choose based on:

- SEO.
- freshness.
- latency.
- personalization.
- cacheability.
- interaction needs.
- failure tolerance.

Example:

| Data | Good Strategy |
|---|---|
| public product page | SSR/SSG with CDN cache |
| authenticated dashboard | CSR with client cache or SSR shell |
| checkout price | server validated fresh API call |
| recommendations | lazy optional fetch with fallback |
| notifications | WebSocket/SSE/polling |
| large report | async job and download |

---

## 17. Frontend State Design

Do not put all state in one global store.

State types:

| State Type | Examples | Best Home |
|---|---|---|
| Local UI | modal open, input text | component/local state |
| Server cache | bookings, profile, search results | query/cache library |
| URL state | filters, pagination, tabs | URL/search params |
| Auth session | current user, capabilities | auth/session provider |
| Domain draft | checkout form, wizard | feature module/store |
| Global UI | theme, toast | small global store |

Senior rule:

> Put state where its lifetime and ownership naturally belong.

---

## 18. Server State vs Client State

Server state:

- Lives on server.
- Can be stale.
- Needs fetching.
- Needs cache invalidation.
- Can fail.
- May be shared across users.

Client state:

- Lives only in browser session.
- Often synchronous.
- UI-specific.
- Does not need refetch.

Common mistake:

- Treating server data as normal local state and manually managing loading, stale data, dedupe, retry, and invalidation everywhere.

Better:

- Use a consistent server-state abstraction or clear local conventions.

---

## 19. API Contract Design

Good API contracts are boring and explicit.

Include:

- Resource shape.
- Required/optional fields.
- Pagination.
- Sorting/filtering semantics.
- Error format.
- Auth requirements.
- Rate limits.
- Idempotency rules.
- Versioning/compatibility policy.

Example error shape:

```json
{
  "code": "BOOKING_NOT_FOUND",
  "message": "Booking was not found",
  "requestId": "req_123"
}
```

Avoid:

- Returning raw stack traces.
- Changing field meanings silently.
- Removing fields without compatibility window.
- Using HTTP 200 for errors.

---

## 20. API Versioning And Compatibility

Web clients are not upgraded instantly.

Reasons:

- Browser tabs remain open.
- Service workers cache old assets.
- CDN caches assets.
- Mobile apps update slowly.
- Deploys are gradual.

Safe change pattern:

1. Add new field.
2. Support old and new field.
3. Deploy frontend using new field.
4. Wait for compatibility window.
5. Remove old field later.

Unsafe:

- Rename `price` to `totalPrice` and remove `price` in same release.

Senior answer:

> I design APIs for overlapping client and server versions. Backward compatibility is part of frontend system design.

---

## 21. Pagination Design

Pagination prevents huge payloads.

Options:

| Type | Best For | Trade-Offs |
|---|---|---|
| Offset pagination | simple admin lists | slow for deep pages, unstable with inserts |
| Cursor pagination | feeds, large lists | more complex, stable and scalable |
| Keyset pagination | ordered large datasets | requires stable sort key |

Cursor response shape:

```json
{
  "items": [],
  "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2...",
  "hasMore": true
}
```

Design rule:

> Never return unbounded lists from production APIs.

---

## 22. Filtering And Sorting Design

Decide where filtering happens.

Browser filtering is OK when:

- Dataset is small.
- Data is already loaded.
- No security concerns.
- User needs instant local interaction.

Server filtering is required when:

- Dataset is large.
- Data is sensitive.
- Filtering requires authorization.
- Results must be fresh.
- Pagination depends on filter.

Sorting caution:

- Sort on indexed fields in database when possible.
- Avoid sorting massive arrays in Node request path.
- Avoid client-side sorting of huge datasets.

---

## 23. Caching Layers

JavaScript systems often use multiple caches.

| Layer | Examples | Purpose |
|---|---|---|
| Browser HTTP cache | static assets | avoid re-download |
| Service worker cache | offline/repeat visits | controlled client cache |
| CDN cache | HTML/assets/API responses | reduce origin latency/load |
| BFF memory cache | small hot config | reduce dependency calls |
| Redis/cache service | shared hot data | cross-instance caching |
| Client query cache | server state in browser | avoid duplicate fetches |
| Database cache/index | query optimization | storage-level performance |

Cache design asks:

- What is the key?
- What is the TTL?
- Who can see this data?
- How stale can it be?
- How is it invalidated?
- What happens on cache miss?
- What prevents stampede?

---

## 24. HTTP Cache Headers

Static hashed assets:

```http
Cache-Control: public, max-age=31536000, immutable
```

HTML shell:

```http
Cache-Control: no-cache
```

Public API response with short freshness:

```http
Cache-Control: public, max-age=60, stale-while-revalidate=300
```

User-specific response:

```http
Cache-Control: private, no-store
```

Senior caution:

> Cache headers are security and correctness controls. User-specific data must not be cached publicly.

---

## 25. Cache Key Design

Bad cache key:

```js
const key = "hotel-search";
```

Better:

```js
function searchCacheKey({ city, checkIn, checkOut, guests, currency }) {
  return ["hotel-search", city, checkIn, checkOut, guests, currency].join(":");
}
```

For user-specific data:

```js
function accountCacheKey(userId) {
  return `account-summary:${userId}`;
}
```

Cache key must include every input that changes the response.

---

## 26. Cache Stampede Protection

A hot key expiring can overload the backend.

Use:

- TTL jitter.
- Request coalescing.
- Stale-while-revalidate.
- Background refresh.
- Locking for expensive recomputation.

Example request coalescing:

```js
const inFlight = new Map();

async function loadOnce(key, loader) {
  if (inFlight.has(key)) {
    return inFlight.get(key);
  }

  const promise = loader().finally(() => {
    inFlight.delete(key);
  });

  inFlight.set(key, promise);
  return promise;
}
```

---

## 27. CDN Design

CDNs help with:

- Static assets.
- Images.
- Public pages.
- API responses where safe.
- Edge redirects.
- DDoS absorption.
- TLS termination.

Design concerns:

- Cache key.
- Vary headers.
- Cookies.
- Query parameters.
- Purge strategy.
- Stale content behavior.
- Personalized content.

Senior answer:

> CDN is not only a speed tool. It is a correctness boundary. Cache-key design determines whether users see the right content.

---

## 28. Image And Asset Architecture

Image-heavy apps need a dedicated strategy.

Design:

- Upload original to object storage.
- Generate variants asynchronously.
- Store metadata.
- Serve through CDN.
- Use responsive images.
- Lazy-load non-critical images.
- Preload only critical LCP image.

Flow:

1. User uploads image.
2. Backend issues signed upload URL.
3. Browser uploads directly to object storage.
4. Storage event triggers processing worker.
5. Worker creates variants.
6. CDN serves optimized variant.

This avoids sending large files through Node API memory.

---

## 29. File Upload System Design

Bad design:

- Browser uploads huge file to Node.
- Node buffers whole file.
- Node uploads to storage.

Better design:

- Backend authenticates user.
- Backend creates signed upload URL.
- Browser uploads directly to object storage.
- Backend records metadata.
- Worker scans/processes file.

Example signed URL flow:

```js
app.post("/uploads", asyncHandler(async (req, res) => {
  const command = uploadRequestSchema.parse(req.body);

  const upload = await uploadService.createSignedUpload({
    userId: req.user.id,
    fileName: command.fileName,
    contentType: command.contentType,
    sizeBytes: command.sizeBytes
  });

  res.status(201).json(upload);
}));
```

Security requirements:

- File size limits.
- Content type validation.
- Malware scan where required.
- Authorization checks.
- Private bucket by default.
- Signed URL expiration.

---

## 30. File Download System Design

For private downloads:

1. User requests download.
2. Backend checks authorization.
3. Backend returns short-lived signed URL.
4. Browser downloads from object storage/CDN.

Benefits:

- Node does not stream huge file unnecessarily.
- CDN/storage handles bandwidth.
- Authorization remains server-controlled.

Caution:

- Signed URLs must expire.
- Do not leak URLs in logs.
- Use content disposition safely.

---

## 31. Authentication Architecture

Common web auth models:

| Model | Description | Trade-Offs |
|---|---|---|
| Server session cookie | server stores session, browser sends cookie | simple revocation, server storage needed |
| JWT access token | self-contained token | stateless verification, revocation harder |
| OAuth/OIDC | delegated identity | robust standard, flow complexity |
| BFF session | browser uses cookie to BFF, BFF handles tokens | safer token handling, BFF required |

Senior preference for browser apps:

> For high-security browser apps, I often prefer HttpOnly Secure SameSite cookies and a BFF/session model so tokens are not exposed to JavaScript.

---

## 32. Authorization Architecture

Authentication answers:

> Who are you?

Authorization answers:

> What are you allowed to do?

Rules:

- Enforce authorization on backend.
- Frontend route guards are UX only.
- Check tenant/customer boundaries.
- Use resource-level authorization.
- Log authorization denials safely.

Example:

```js
async function getBookingForUser(bookingId, user) {
  const booking = await bookingRepository.findById(bookingId);

  if (!booking || booking.customerId !== user.customerId) {
    throw new NotFoundError("Booking not found");
  }

  return booking;
}
```

Returning 404 can avoid leaking resource existence.

---

## 33. Session And Token Storage

Avoid storing sensitive long-lived tokens in browser JavaScript-accessible storage when possible.

Storage options:

| Storage | Pros | Risks |
|---|---|---|
| HttpOnly cookie | not readable by JS | CSRF must be handled |
| memory | reduced persistence | lost on refresh, XSS can still act |
| localStorage | simple | XSS token theft risk |
| sessionStorage | tab-scoped | XSS token theft risk |

Cookie protections:

```http
Set-Cookie: session=abc; HttpOnly; Secure; SameSite=Lax; Path=/
```

For cross-site auth flows, `SameSite=None; Secure` may be required.

---

## 34. CSRF And XSS In Architecture

If using cookies for auth:

- Add SameSite where possible.
- Use CSRF tokens for risky cross-site flows.
- Validate origin/referer for state-changing requests when appropriate.

For XSS defense:

- Escape output.
- Avoid unsafe HTML insertion.
- Use CSP.
- Sanitize untrusted HTML.
- Keep dependencies patched.
- Avoid storing tokens in localStorage when possible.

Architecture point:

> Auth design, rendering design, and security design are connected.

---

## 35. API Security Controls

Baseline controls:

- Authentication.
- Authorization.
- Input validation.
- Output encoding.
- Rate limiting.
- Body size limits.
- CORS policy.
- CSRF protection where needed.
- Security headers.
- Audit logs.
- Secret management.
- Dependency scanning.

Express baseline shape:

```js
app.use(express.json({ limit: "1mb" }));
app.use(requestIdMiddleware);
app.use(securityHeadersMiddleware);
app.use(rateLimitMiddleware);
app.use(authenticateSession);
```

Order matters.

---

## 36. CORS Design

CORS is browser access control, not backend authentication.

Good CORS design:

- Allow only known origins.
- Handle credentials carefully.
- Do not use wildcard with credentials.
- Keep methods/headers limited.
- Configure per environment.

Example:

```js
const allowedOrigins = new Set([
  "https://app.example.com",
  "https://admin.example.com"
]);

function corsOrigin(origin, callback) {
  if (!origin || allowedOrigins.has(origin)) {
    callback(null, true);
    return;
  }

  callback(new Error("Origin not allowed"));
}
```

---

## 37. Node Scalability Model

Node scales well for I/O-heavy workloads.

Node needs care for:

- CPU-heavy operations.
- Large JSON payloads.
- Blocking sync APIs.
- Memory pressure.
- Long-running tasks.
- Backpressure.

Scale options:

- Multiple Node processes/pods.
- Horizontal autoscaling.
- Worker threads for CPU tasks.
- Queues for async work.
- Caches for hot reads.
- Database query optimization.
- CDN for static/public content.

Senior rule:

> Do not solve CPU-heavy request paths by adding endless Node pods. Move or reduce the work.

---

## 38. Horizontal Scaling And Statelessness

Stateless app servers scale more easily.

Keep outside process memory:

- Sessions.
- Idempotency keys.
- Long-lived jobs.
- Shared cache.
- Uploaded files.
- Durable events.

Use external systems:

- Redis/cache for shared short-lived data.
- Database for durable state.
- Object storage for files.
- Queue for async work.
- Pub/sub for events.

In-memory state is OK for:

- Small per-process caches with safe misses.
- Feature config cached with TTL.
- Metrics buffers.

But it must be bounded.

---

## 39. Worker Threads And Queues

Use worker threads when:

- Work is CPU-heavy.
- Work must return quickly to same service.
- Data transfer cost is acceptable.

Use queues when:

- Work can be async.
- Work can be retried.
- Work may take longer than request timeout.
- Work needs durability.
- Work needs smoothing under spikes.

Example queue use cases:

- Email sending.
- Report generation.
- Image processing.
- Webhook processing.
- Audit log enrichment.
- Payment reconciliation.

---

## 40. Async Job Design

Async job flow:

1. API validates request.
2. API creates job record.
3. API enqueues job.
4. API returns `202 Accepted` with job ID.
5. Worker processes job.
6. Worker updates status.
7. Client polls or receives event.

Response:

```json
{
  "jobId": "job_123",
  "status": "QUEUED"
}
```

Job requirements:

- Idempotency.
- Retry policy.
- Dead-letter queue.
- Timeout.
- Progress state.
- Ownership.
- Metrics.

---

## 41. Realtime Architecture Options

| Option | Best For | Trade-Offs |
|---|---|---|
| Polling | simple low-frequency updates | wasteful at scale |
| Long polling | near realtime without WebSocket | more server complexity |
| SSE | server-to-client events | one-way only, simple over HTTP |
| WebSocket | bidirectional realtime | connection management complexity |
| Push notifications | offline/user notifications | platform constraints |

Choose based on:

- Update frequency.
- Direction of communication.
- Connection count.
- Delivery guarantees.
- Infrastructure support.
- Browser compatibility.

---

## 42. WebSocket System Design

Use WebSockets for:

- Chat.
- Collaboration.
- Live dashboards.
- Games.
- Trading/order updates.

Architecture:

1. Client connects with auth token/session.
2. Gateway validates auth.
3. Connection registry stores user/socket mapping.
4. Client subscribes to channels.
5. Backend publishes events.
6. Gateway sends to subscribed sockets.

Challenges:

- Horizontal scaling.
- Sticky sessions or shared pub/sub.
- Reconnect storms.
- Slow clients.
- Heartbeats.
- Backpressure.
- Authorization per channel.

---

## 43. SSE System Design

Server-Sent Events are good for one-way updates.

Good fit:

- Notifications.
- Job progress.
- Live status.
- Simple dashboards.

Example:

```js
app.get("/jobs/:id/events", asyncHandler(async (req, res) => {
  res.setHeader("content-type", "text/event-stream");
  res.setHeader("cache-control", "no-cache");

  const unsubscribe = jobEvents.subscribe(req.params.id, event => {
    res.write(`data: ${JSON.stringify(event)}\n\n`);
  });

  req.on("close", unsubscribe);
}));
```

Caution:

- Track open connections.
- Clean up on close.
- Handle proxies/timeouts.

---

## 44. Offline And Progressive Web App Design

Offline design asks:

- What can user do offline?
- What data is cached?
- How is conflict handled?
- What happens when reconnecting?
- What data is too sensitive to store?

PWA tools:

- Service worker.
- Cache Storage.
- IndexedDB.
- Background sync where available.
- Web app manifest.

Cautions:

- Service workers complicate deployment.
- Offline writes need conflict strategy.
- Sensitive data storage needs security review.

---

## 45. Optimistic UI Design

Optimistic UI updates before server confirmation.

Good fit:

- Likes.
- Saves.
- Low-risk preference toggles.
- Draft UI updates.

Risky for:

- Payments.
- Inventory reservations.
- Permission changes.
- Irreversible actions.

Pattern:

1. Apply optimistic state.
2. Send mutation with idempotency key.
3. If success, confirm state.
4. If failure, rollback or reconcile.
5. Show clear user feedback.

Senior answer:

> Optimistic UI is a latency illusion. It still needs server truth, rollback, idempotency, and conflict handling.

---

## 46. Idempotency Design

Use idempotency for:

- Payments.
- Booking creation.
- Order submission.
- Webhooks.
- Retryable writes.

Server shape:

```js
app.post("/payments", asyncHandler(async (req, res) => {
  const idempotencyKey = req.header("idempotency-key");

  const result = await paymentService.chargeOnce({
    idempotencyKey,
    userId: req.user.id,
    payload: req.body
  });

  res.status(result.created ? 201 : 200).json(result.payment);
}));
```

Storage requirements:

- Durable store.
- Unique constraint.
- TTL based on business rules.
- Store final response or operation status.

---

## 47. Webhook System Design

Webhook receiver requirements:

- Verify signature using raw body.
- Deduplicate event ID.
- Process asynchronously.
- Respond quickly.
- Retry safely.
- Store event audit trail.

Flow:

1. Receive webhook.
2. Verify signature.
3. Store event ID if new.
4. Enqueue processing job.
5. Return 2xx.
6. Worker performs side effects idempotently.

Caution:

> Webhooks are usually at-least-once delivery. Duplicate events are normal.

---

## 48. Search Architecture

Search can be designed with:

- Database query for small/simple search.
- Search engine for full text, ranking, facets.
- Client-side search for small local datasets.
- Typeahead service for suggestions.

Frontend concerns:

- Debounce.
- Cancel stale requests.
- Show loading state.
- Prevent out-of-order responses.
- Cache recent queries.

Backend concerns:

- Rate limits.
- Query length limits.
- Index design.
- Pagination.
- Relevance tuning.

---

## 49. Typeahead Design

Typeahead requirements:

- Low latency.
- High request volume.
- Stale request cancellation.
- Prefix search.
- Ranking.
- Abuse protection.

Frontend pattern:

```js
let activeController;

async function searchSuggestions(query) {
  activeController?.abort();
  activeController = new AbortController();

  const response = await fetch(`/suggest?q=${encodeURIComponent(query)}`, {
    signal: activeController.signal
  });

  return response.json();
}
```

Backend pattern:

- Cache hot prefixes.
- Limit query length.
- Return small payload.
- Track p95 latency.

---

## 50. Notification System Design

Notification channels:

- In-app notification.
- Email.
- SMS.
- Push notification.
- WebSocket/SSE live event.

Design concerns:

- User preferences.
- Delivery retries.
- Provider fallback.
- Rate limits.
- Deduplication.
- Audit trail.
- Quiet hours.

JavaScript architecture:

- API writes notification event.
- Queue processes delivery.
- Worker sends through providers.
- Frontend receives live updates through SSE/WebSocket or polling.

---

## 51. Observability Architecture

You need observability across browser and Node.

Browser signals:

- Runtime errors.
- Web Vitals.
- Route transitions.
- API timings.
- Long tasks.
- User-impact events.
- Release version.

Node signals:

- Request rate.
- Latency percentiles.
- Error rate.
- CPU.
- memory.
- event-loop delay.
- DB pool wait.
- dependency spans.
- queue depth.

Correlation:

- `x-request-id` header.
- trace context.
- release ID.
- user/session ID where privacy-safe.

---

## 52. Request ID Propagation

Gateway or app creates request ID.

```js
import crypto from "node:crypto";

function requestIdMiddleware(req, res, next) {
  req.id = req.header("x-request-id") || crypto.randomUUID();
  res.setHeader("x-request-id", req.id);
  next();
}
```

Frontend can include request ID in error reports when available.

Design rule:

> A production trace should connect browser action, API request, backend logs, dependency spans, and user-visible error.

---

## 53. Logging Architecture

Good logs are:

- structured.
- bounded.
- redacted.
- correlated.
- sampled where high volume.
- low-cardinality when indexed.

Bad:

```js
logger.info({ req, body: req.body }, "request");
```

Good:

```js
logger.info({
  requestId: req.id,
  method: req.method,
  route: req.route?.path ?? "unknown",
  statusCode: res.statusCode,
  durationMs
}, "request completed");
```

---

## 54. Metrics Architecture

Useful metrics:

- API latency p50/p95/p99.
- error rate.
- request rate.
- event-loop delay.
- heap/RSS/external memory.
- cache hit rate.
- queue depth.
- oldest job age.
- DB pool wait.
- Web Vitals p75.
- frontend runtime error rate.

Avoid high-cardinality labels:

- user ID.
- raw URL with IDs.
- request ID.
- email.

Use route templates instead.

---

## 55. Tracing Architecture

Tracing answers:

> Where did this request spend time?

Trace spans may include:

- browser interaction.
- CDN/gateway.
- BFF route.
- Node service.
- DB query.
- cache call.
- external API.
- queue publish.

Senior design:

- Propagate trace context.
- Add meaningful span names.
- Avoid sensitive data in spans.
- Sample intelligently.
- Connect frontend and backend traces where possible.

---

## 56. Error Handling Architecture

Frontend:

- route-level error boundaries.
- component fallbacks.
- API error mapping.
- retry only where safe.
- user-friendly messages.
- error reporting with release ID.

Backend:

- central error handler.
- typed/domain errors.
- safe external response.
- internal structured logs.
- request ID.
- no stack traces to users.

Backend shape:

```js
app.use((error, req, res, next) => {
  logger.error({ error, requestId: req.id }, "request failed");

  res.status(error.statusCode ?? 500).json({
    code: error.code ?? "INTERNAL_ERROR",
    message: error.publicMessage ?? "Something went wrong",
    requestId: req.id
  });
});
```

---

## 57. Performance Architecture

Frontend performance:

- route-level code splitting.
- critical CSS strategy.
- image optimization.
- CDN caching.
- lazy non-critical widgets.
- worker for CPU-heavy work.
- list virtualization.
- Web Vitals monitoring.

Backend performance:

- pagination.
- indexes.
- caching.
- streaming.
- bounded payloads.
- async jobs.
- worker threads for CPU.
- event-loop delay monitoring.

Architecture rule:

> Performance problems are often placement problems: work happens on the wrong layer or at the wrong time.

---

## 58. Resilience Architecture

Resilience patterns:

- timeouts.
- retries with backoff and jitter.
- circuit breakers.
- bulkheads.
- rate limits.
- load shedding.
- fallbacks.
- queues.
- idempotency.
- graceful degradation.

Example dependency wrapper:

```js
async function loadRecommendations(userId) {
  try {
    const response = await fetchWithTimeout(
      `https://recommendations.example/users/${userId}`,
      300
    );

    return response.json();
  } catch {
    return [];
  }
}
```

Optional dependencies should not break critical flows.

---

## 59. Rate Limiting Design

Rate-limit dimensions:

- IP.
- user ID.
- customer/tenant ID.
- API key.
- route.
- operation type.

Bad:

```js
const key = req.ip;
```

Better:

```js
function rateLimitKey(req) {
  if (req.user?.customerId) {
    return `customer:${req.user.customerId}`;
  }

  return `ip:${req.ip}`;
}
```

Design caution:

- NAT can group many legitimate users behind one IP.
- Login endpoints need stricter controls.
- Expensive endpoints need operation-specific limits.

---

## 60. Multi-Tenant JavaScript Apps

Multi-tenant concerns:

- Tenant isolation.
- Authorization checks.
- Cache keys include tenant.
- Logs include tenant ID where safe.
- Metrics segment by tenant tier, not raw tenant if too high cardinality.
- Data access always scoped.

Bad cache:

```js
cache.set("dashboard", dashboard);
```

Good cache:

```js
cache.set(`tenant:${tenantId}:dashboard`, dashboard, { ttlSeconds: 60 });
```

Senior answer:

> Tenant ID is part of the security boundary and often part of cache keys, DB queries, audit logs, and authorization checks.

---

## 61. Deployment Architecture

Frontend deployment:

- Build assets with content hashes.
- Upload assets to CDN/object storage.
- Deploy HTML/app shell.
- Keep old assets for compatibility window.
- Upload source maps securely.
- Track release ID.

Backend deployment:

- Rolling deploy.
- Health/readiness checks.
- Graceful shutdown.
- Database migration compatibility.
- Feature flags.
- Canary metrics.

Never assume frontend and backend deploy atomically.

---

## 62. Feature Flag Architecture

Feature flags help:

- Progressive rollout.
- Kill switch.
- A/B testing.
- Canary exposure.
- Operational mitigation.

Risks:

- Inconsistent frontend/backend evaluation.
- Stale flag cache.
- Forgotten flags.
- Complex test matrix.

Good practice:

- Backend compatibility first.
- Server-provided config for critical flows.
- Expiration owner/date.
- Metrics segmented by flag.
- Remove stale flags.

---

## 63. Database Migration With JS Clients

Safe migration pattern:

1. Add new column/table.
2. Deploy backend writing both old and new.
3. Backfill.
4. Deploy readers using new field.
5. Stop old writes.
6. Remove old field later.

Why:

- Rolling deploys overlap versions.
- Frontend clients may be cached.
- Queue workers may run older code.

Senior answer:

> Schema migration is part of application system design, not just database work.

---

## 64. Testing System Architecture

Test layers:

- Unit tests for pure logic.
- Component tests for UI behavior.
- API integration tests.
- Contract tests.
- E2E tests for critical flows.
- Performance smoke tests.
- Accessibility tests.
- Security tests.
- Load tests for backend.

Architecture needs tests for:

- API compatibility.
- auth/authorization.
- cache isolation.
- idempotency.
- retries/timeouts.
- rendering fallback.
- error boundaries.
- migrations.

---

## 65. Frontend Build Architecture

Build concerns:

- module bundling.
- code splitting.
- tree shaking.
- CSS extraction.
- asset hashing.
- source maps.
- environment variables.
- browser targets.
- dependency deduplication.

Build outputs should support:

- cacheable assets.
- release rollback.
- error symbolication.
- route-level performance budgets.

Senior answer:

> The build pipeline is production infrastructure. A bad bundle can be a production incident.

---

## 66. Monorepo Architecture For JS

Monorepo benefits:

- shared types.
- shared UI components.
- shared API clients.
- atomic changes.
- consistent tooling.

Risks:

- slow CI.
- unclear ownership.
- accidental coupling.
- shared package breaking many apps.
- dependency version complexity.

Good practices:

- package boundaries.
- ownership.
- affected builds/tests.
- semantic versioning or change controls.
- avoid dumping all utilities into one shared package.

---

## 67. TypeScript In System Design

TypeScript helps with:

- API contracts.
- domain models.
- refactoring safety.
- client/server shared types.
- invalid state reduction.

But TypeScript does not replace:

- runtime validation.
- authorization.
- integration tests.
- schema compatibility.

Boundary rule:

> Validate at runtime when data crosses trust boundaries: HTTP, queue, storage, environment variables, webhooks, browser storage.

---

## 68. Environment Configuration Design

Config sources:

- environment variables.
- secret manager.
- runtime config endpoint.
- build-time variables.
- feature flag service.

Rules:

- Validate required config at startup.
- Do not put secrets in frontend bundles.
- Separate build-time and runtime config.
- Keep environment parity.
- Log config presence, not secret values.

Example:

```js
function requireEnv(name) {
  const value = process.env[name];

  if (!value) {
    throw new Error(`Missing required env var: ${name}`);
  }

  return value;
}
```

---

## 69. Designing A Booking App

Prompt:

> Design a hotel booking web app with search, room details, checkout, payment, and booking management.

High-level architecture:

- CDN serves static assets.
- SSR/SSG for public hotel pages if SEO matters.
- CSR/dashboard for authenticated booking management.
- BFF composes hotel, pricing, availability, user, and booking services.
- Node APIs own booking commands.
- Payment uses idempotency keys.
- Search uses indexed backend/search service.
- Images served through CDN.
- Observability connects browser and backend.

Critical concerns:

- Price freshness.
- Inventory race conditions.
- Payment idempotency.
- Cache correctness.
- SEO for public hotel pages.
- Mobile performance.

---

## 70. Booking Checkout Flow

Checkout flow:

1. User selects room.
2. Frontend requests latest price/availability.
3. Backend creates temporary reservation hold.
4. User enters payment.
5. Frontend submits payment with idempotency key.
6. Backend confirms payment.
7. Backend finalizes booking.
8. Confirmation returned and emailed.

Design notes:

- Do not trust frontend price.
- Reservation holds need TTL.
- Payment must be idempotent.
- Confirmation should be durable.
- Email should be async.

---

## 71. Designing A Dashboard

Prompt:

> Design an internal operations dashboard with charts, filters, live updates, and exports.

Architecture:

- CSR authenticated app.
- API/BFF for dashboard summaries.
- Query cache for server state.
- URL state for filters.
- Virtualized tables.
- SSE/WebSocket for live updates if needed.
- Exports as async jobs.
- Role-based authorization.
- RUM and API metrics.

Avoid:

- Loading all data into browser.
- Rendering huge DOM tables.
- Exporting in request path.
- Polling every second for all users.

---

## 72. Designing A Realtime Collaboration App

Prompt:

> Design a collaborative document editor.

Core needs:

- Realtime updates.
- Conflict handling.
- Presence.
- Offline or reconnect behavior.
- Version history.
- Access control.

Architecture:

- Browser app with local optimistic updates.
- WebSocket gateway.
- Pub/sub across gateway instances.
- Conflict-resolution model such as OT/CRDT or server ordering.
- Durable document store.
- Snapshotting.
- Presence store with TTL.

Senior answer:

> Realtime collaboration is mainly a consistency and conflict-resolution problem, not just a WebSocket problem.

---

## 73. Designing A Notification Center

Prompt:

> Design a notification system for web users.

Architecture:

- Event producers publish notification events.
- Notification service stores notifications.
- Worker sends email/push if needed.
- Frontend fetches notification list.
- SSE/WebSocket sends unread-count updates.
- User preferences control channels.

Data model:

- notification ID.
- user ID.
- type.
- payload.
- read status.
- created time.
- delivery status.

Concerns:

- deduplication.
- preferences.
- rate limits.
- retries.
- unread count caching.

---

## 74. Designing A Feed

Prompt:

> Design a personalized feed in a JavaScript web app.

Architecture options:

- Pull model: generate feed on request.
- Push model: precompute feed fanout.
- Hybrid: precompute for heavy users, rank on read.

Frontend:

- cursor pagination.
- infinite scroll with virtualization.
- skeleton loading.
- stale request cancellation.
- impression tracking with batching.

Backend:

- ranking service.
- feed cache.
- pagination cursor.
- dedupe.
- freshness rules.

Trade-off:

> Freshness, personalization, latency, and cost pull against each other.

---

## 75. Designing Search

Prompt:

> Design hotel search with filters, sorting, and typeahead.

Frontend:

- debounce input.
- abort stale requests.
- keep filters in URL.
- show partial loading states.
- avoid stale result race.

Backend:

- search index.
- filters/facets.
- cursor pagination.
- ranking.
- cache hot queries.
- rate limit.

Data freshness:

- availability/prices may need fresh verification during checkout.
- search results can be slightly stale if final checkout verifies.

---

## 76. Designing An Admin Tool

Admin tools need:

- strong authorization.
- audit logs.
- safe bulk actions.
- confirmation flows.
- pagination.
- filters.
- exports as async jobs.
- role-based UI.

Architecture:

- CSR app often sufficient.
- BFF/API for admin-specific payloads.
- server-side authorization for every action.
- audit event for mutations.
- feature flags for risky tools.

Senior answer:

> Admin UI security is not only hiding buttons. Every backend mutation needs authorization and auditability.

---

## 77. Designing A Public Content Site

Requirements:

- SEO.
- fast LCP.
- high cacheability.
- content freshness.
- preview mode.

Architecture:

- SSG/ISR for content pages.
- CDN for HTML/assets/images.
- CMS webhook triggers regeneration.
- Preview bypasses cache with auth.
- client JS kept minimal.

Trade-off:

> Static content is fast and resilient, but freshness and preview workflows require design.

---

## 78. Designing A Payment Flow

Payment design requirements:

- server validates amount.
- idempotency key.
- no card data through your server unless compliant.
- secure provider integration.
- webhook verification.
- duplicate webhook handling.
- booking finalization after payment confirmation.

Frontend:

- disable duplicate submit.
- show progress.
- handle provider errors.
- never trust client-only success.

Backend:

- create payment intent/session.
- verify webhook.
- idempotently update order.
- audit payment state.

---

## 79. Designing Large Report Export

Bad:

- request loads all rows.
- Node stringifies huge JSON.
- browser waits on long request.

Good:

1. User requests export.
2. API creates export job.
3. Worker streams rows to file.
4. File stored in object storage.
5. User gets notification/download link.
6. Link expires.

Benefits:

- avoids request timeout.
- protects event loop.
- supports progress.
- supports retry.

---

## 80. Designing Client-Side Analytics

Analytics should not hurt UX.

Design:

- event schema.
- batching.
- sampling.
- consent handling.
- offline queue with cap.
- `sendBeacon` for page unload events.
- low-priority scheduling.
- PII redaction.

Bad:

- synchronous analytics on click before critical action.
- unbounded offline queue.
- logging full payloads.

Senior answer:

> Analytics is production code. It needs budgets, ownership, privacy review, and failure isolation.

---

## 81. Designing Error Reporting

Frontend error event should include:

- release version.
- route.
- browser.
- stack trace with source map.
- component/feature context.
- request ID if connected to API.
- user/session ID only if privacy-safe.

Backend error event should include:

- request ID.
- route template.
- status code.
- error code.
- service version.
- dependency span if relevant.

Do not include secrets or raw PII.

---

## 82. Accessibility In Architecture

Accessibility is not a final polish task.

Design for:

- keyboard navigation.
- focus management.
- semantic HTML.
- screen reader announcements.
- color contrast.
- reduced motion.
- form errors.
- modal behavior.

Testing:

- automated accessibility checks.
- keyboard walkthroughs.
- critical-flow manual tests.

Senior answer:

> Accessibility is part of production correctness and should be included in design and testing, especially for critical flows.

---

## 83. Internationalization Architecture

i18n concerns:

- translations.
- pluralization.
- date/time formatting.
- number/currency formatting.
- right-to-left layout.
- locale routing.
- translation loading.

Performance concerns:

- do not ship all locales to all users.
- lazy-load locale bundles.
- cache translations.

Example:

```js
const formatter = new Intl.NumberFormat(locale, {
  style: "currency",
  currency
});
```

Caution:

- Cache formatters for repeated formatting in hot paths.

---

## 84. Privacy And Data Minimization

Architecture should minimize data exposure.

Principles:

- Fetch only needed fields.
- Do not log PII.
- Do not send sensitive fields to frontend unnecessarily.
- Mask sensitive values in UI.
- Use short-lived signed URLs.
- Keep audit logs for sensitive actions.
- Respect consent requirements.

Senior answer:

> The safest frontend data is data never sent to the browser.

---

## 85. Cost-Aware JavaScript Design

Cost drivers:

- server rendering compute.
- edge invocations.
- CDN egress.
- logs and metrics volume.
- third-party scripts.
- API calls from polling.
- DB queries.
- queue retries.
- large bundle delivery.

Cost optimizations:

- cache safely.
- batch events.
- reduce duplicate requests.
- avoid high-cardinality metrics.
- move static work to build time.
- use CDN for assets.
- right-size serverless functions.

---

## 86. Architecture Review Checklist

Before approving a JS system design, ask:

- What rendering model is chosen and why?
- What data is fetched where?
- What is cached and with what key/TTL?
- What happens on dependency failure?
- How are auth and authorization enforced?
- How are large payloads bounded?
- How does the system handle retries?
- How are duplicate writes prevented?
- How are old frontend versions supported?
- How are metrics/logs/traces connected?
- How is performance protected?
- How is rollout controlled?

---

## 87. Common Wrong System Design Answers

| Weak Answer | Why Weak | Stronger Answer |
|---|---|---|
| Use React and Node. | Names tools, not architecture. | Explain rendering, APIs, state, cache, auth, scaling. |
| Put everything in global state. | Poor ownership and performance. | Separate local, URL, server, and domain state. |
| Use SSR for everything. | SSR has cost and hydration complexity. | Choose per route based on SEO/freshness/interactivity. |
| Cache everything. | Can leak or stale data. | Cache with key, TTL, invalidation, privacy rules. |
| Use WebSocket for realtime. | Ignores connection scaling. | Discuss auth, pub/sub, reconnect, backpressure. |
| Increase timeout. | Avoids design issue. | Move long work async or add fallback. |
| Store JWT in localStorage. | XSS token theft risk. | Prefer secure cookie/BFF where appropriate. |
| Backend trusts frontend price. | Security/correctness bug. | Server verifies price and availability. |

---

## 88. MAANG Scenario: Design Checkout

Prompt:

> Design checkout for a travel booking web app.

Strong answer outline:

1. Public search/details can be SSR/SSG for SEO.
2. Checkout is authenticated and server-validated.
3. Frontend requests fresh price/availability before payment.
4. Backend creates reservation hold with TTL.
5. Payment uses provider and idempotency key.
6. Webhook finalizes booking idempotently.
7. Confirmation email is async.
8. Observability tracks checkout conversion, errors, latency, payment failures.
9. Security includes auth, CSRF/CORS/cookies, rate limits, audit logs.
10. Rollout uses feature flags and canary.

---

## 89. MAANG Scenario: Design Realtime Dashboard

Prompt:

> Design a realtime operations dashboard for thousands of users.

Strong answer outline:

- CSR app for authenticated dashboard.
- Initial data fetched through BFF.
- Updates through SSE or WebSocket depending on bidirectional need.
- Server publishes events through pub/sub.
- Client applies updates with backpressure and batching.
- Large tables are virtualized.
- Exports are async jobs.
- Authorization checked per tenant/channel.
- Metrics include connection count, event lag, dropped updates, API latency.

---

## 90. MAANG Scenario: Design Public Hotel Pages

Prompt:

> Design public hotel pages with SEO, fast load, and frequent price changes.

Strong answer outline:

- Hotel descriptive content can be SSG/ISR and CDN cached.
- Images served through CDN with responsive variants.
- Price/availability fetched dynamically or revalidated frequently.
- LCP image optimized and discoverable.
- Structured data for SEO.
- Client JS minimized for non-interactive content.
- Checkout verifies latest price server-side.

Trade-off:

> Static hotel content can be cached aggressively, but price and availability need freshness boundaries.

---

## 91. MAANG Scenario: Design Large File Upload

Prompt:

> Design upload of large documents/images from browser.

Strong answer outline:

1. Browser asks API for signed upload URL.
2. API authenticates and validates metadata.
3. Browser uploads directly to object storage.
4. Storage event triggers scan/processing worker.
5. Worker stores variants/metadata.
6. Frontend polls or receives progress.
7. Downloads use short-lived signed URLs.
8. Limits, content type checks, malware scanning, and authorization are required.

---

## 92. MAANG Scenario: Design Notification System

Prompt:

> Design web notifications with email fallback.

Strong answer outline:

- Events published by product services.
- Notification service stores user notifications.
- Worker sends email/push based on preferences.
- In-app UI fetches notifications with pagination.
- SSE/WebSocket updates unread count.
- Deduplication prevents duplicate sends.
- Retry policy and DLQ handle provider failures.
- User preferences and quiet hours control delivery.

---

## 93. MAANG Scenario: Design Frontend Platform

Prompt:

> Design frontend platform for multiple teams building web apps.

Strong answer outline:

- shared design system.
- routing conventions.
- build pipeline.
- performance budgets.
- source map upload.
- RUM/error monitoring.
- shared auth/session package.
- API client conventions.
- accessibility standards.
- deployment templates.
- ownership model.

Senior caution:

> Shared platform should reduce repeated work without creating a bottleneck or hiding ownership.

---

## 94. 30-Second System Design Answer

> For a JavaScript system, I first choose where work belongs: browser, server, edge, BFF, API, queue, cache, database, or CDN. Then I decide rendering strategy per route: CSR for interactive authenticated apps, SSR/SSG/ISR for SEO and fast first content, and edge only for lightweight global logic. I design API contracts, auth, caching, state ownership, performance, reliability, observability, and rollout safety. The final design should handle old clients, failures, scale, and production debugging.

---

## 95. 60-Second Senior Answer

> I would start with requirements: SEO, auth, latency, freshness, interactivity, scale, and reliability. For frontend, I choose CSR, SSR, SSG, ISR, edge, or hybrid per route. I separate local UI state, server state, URL state, and domain workflow state. For backend, I design Node routes, validation, services, repositories, auth, rate limits, and observability. For expensive or long work, I use queues, workers, streaming, or object storage. For performance, I use CDN, caching, code splitting, pagination, image optimization, and Web Vitals. For reliability, I add timeouts, retries with backoff, circuit breakers, idempotency, graceful shutdown, and rollback/feature flags. I verify the design with tests, dashboards, traces, and clear ownership.

---

## 96. Rapid Revision

- JavaScript system design is about work placement.
- Browser, Node, edge, workers, queues, CDN, and database each have different strengths.
- CSR is good for interactive authenticated apps.
- SSR helps SEO and first content but adds server and hydration cost.
- SSG/ISR work well for cacheable content with controlled freshness.
- BFF composes data for one frontend experience.
- API gateway handles platform edge concerns.
- Server state and client state should be separated.
- Cache design needs key, TTL, invalidation, privacy, and stampede control.
- Web clients are not upgraded instantly, so APIs need compatibility windows.
- Large files should usually go directly to object storage with signed URLs.
- Long tasks should move to queues/workers or be streamed.
- WebSockets need auth, scaling, heartbeats, backpressure, and reconnect strategy.
- Auth belongs on the backend; frontend guards are UX only.
- Observability must connect browser and backend with request IDs/traces/releases.
- Feature flags help rollout but add distributed state complexity.
- Performance budgets and source maps are production infrastructure.
- Good design includes failure behavior, not only happy path.

---

## 97. Official And High-Value Sources

Use these for deeper study:

- MDN Web Docs: HTTP caching, Fetch, Service Workers, Web Workers, CORS, cookies, Performance APIs.
- Node.js documentation: HTTP, streams, worker threads, diagnostics, perf_hooks, cluster/process, async context.
- web.dev: Core Web Vitals, rendering performance, image optimization, caching.
- React documentation: rendering, hydration, effects, server rendering, error boundaries, profiler.
- OpenTelemetry documentation: traces, metrics, logs, context propagation.
- OWASP guidance: authentication, session management, XSS, CSRF, secure headers, logging.
- Cloud/CDN provider docs: cache keys, edge functions, signed URLs, object storage, load balancing.
- Database docs for pagination, indexing, transactions, connection pooling.

---

## 98. Final Mental Model

A strong JavaScript system design answer does not start with a framework.

It starts with boundaries:

1. What runs in the browser?
2. What runs on the server?
3. What can be cached?
4. What must be fresh?
5. What must be secure?
6. What can fail?
7. What must be observable?
8. What changes during rollout?

Once those boundaries are clear, the architecture becomes a set of deliberate trade-offs instead of a pile of libraries.
