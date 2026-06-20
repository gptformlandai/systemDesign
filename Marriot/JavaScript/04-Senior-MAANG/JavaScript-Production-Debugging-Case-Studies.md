# JavaScript Production Debugging Case Studies

> Goal: build production debugging judgment for JavaScript systems across browser, Node.js, APIs, queues, bundles, async failures, memory leaks, event-loop stalls, and user-facing incidents.

---

## 1. How To Use This Sheet

This file is not a syntax sheet.

It is a production incident training sheet.

Use it to practice:

- How to classify JavaScript production failures.
- Which metric or trace to check first.
- How to move from symptom to evidence.
- How to find root cause without guessing.
- How to mitigate user impact while debugging.
- How to explain incidents in senior interviews.
- How to prevent the same failure from returning.

Read the case studies out loud. The goal is to sound like an engineer who has handled real incidents.

---

## 2. The Senior Production Debugging Mindset

Production debugging is different from local debugging.

Local debugging asks:

> Why does this code not work on my machine?

Production debugging asks:

> What changed, who is affected, what evidence proves the bottleneck, how do we reduce impact now, and how do we prevent recurrence?

Senior engineers do not start with random code changes.

They start with:

1. User impact.
2. Blast radius.
3. Timeline.
4. Recent changes.
5. Metrics.
6. Logs.
7. Traces.
8. Profiles.
9. Safe mitigation.
10. Permanent fix.

---

## 3. Universal Incident Loop

Use this loop for every case.

1. Confirm the symptom.
2. Define severity.
3. Identify affected route, endpoint, service, version, browser, device, region, customer, or traffic segment.
4. Check recent deploys, flags, config, dependency changes, data shape changes, and traffic spikes.
5. Inspect dashboards.
6. Compare healthy and unhealthy traces.
7. Form a hypothesis.
8. Validate with evidence.
9. Mitigate if impact is active.
10. Patch root cause.
11. Verify recovery.
12. Add guardrails.
13. Write a short post-incident note.

---

## 4. Evidence Map

| Symptom | First Signals | Deeper Tools |
|---|---|---|
| Browser page slow | Web Vitals, Network, Performance trace | DevTools, RUM, bundle analyzer |
| Input lag | INP, long tasks, interaction trace | React Profiler, Performance panel |
| Node API slow | p95/p99 latency, traces, CPU, event-loop delay | CPU profile, DB metrics, APM |
| Node memory growth | heap, RSS, external memory | heap snapshots, allocation sampling |
| High CPU | process CPU, event-loop delay | flamegraph, CPU profile |
| Async failures | error logs, unhandled rejections, retries | traces, queue metrics, dependency logs |
| API storm | request rate, retries, dependency saturation | rate-limit logs, client version, traces |
| Bad bundle | bundle size, LCP, long tasks | analyzer, Coverage, source maps |
| Queue backlog | queue depth, oldest message age | worker logs, dependency metrics |
| WebSocket failure | connection count, send queue, reconnect rate | client logs, gateway metrics |

---

## 5. Production Severity Classification

| Severity | Meaning | Example |
|---|---|---|
| SEV1 | Major business-critical outage | Checkout unavailable globally. |
| SEV2 | Large degraded experience | API p99 latency above 10 seconds for many users. |
| SEV3 | Limited user impact | One route slow for a subset of users. |
| SEV4 | Low urgency bug | Debuggable issue with workaround. |

Interview wording:

> I classify severity based on user impact, business criticality, scope, error rate, latency, and whether there is a workaround.

---

## 6. Mitigation vs Root Cause

Mitigation reduces pain now.

Root cause prevents recurrence.

Examples:

| Incident | Mitigation | Root Cause Fix |
|---|---|---|
| Bad release | rollback or disable flag | add test/budget/guardrail |
| API storm | rate limit, circuit breaker | fix retry policy |
| high CPU | scale, disable feature | profile and fix hot path |
| memory leak | restart pods temporarily | fix retained references |
| bad bundle | rollback | code split/remove dependency |
| DB saturation | shed load/cache reads | query/index/pool fix |

Senior behavior:

> During active impact, I mitigate first. After stabilization, I continue root-cause analysis and add prevention.

---

## 7. Case Study Format

Each production case uses this shape:

- Symptom.
- First questions.
- Signals to inspect.
- Likely root causes.
- Investigation path.
- Fix.
- Prevention.
- Strong interview answer.

Use this repeatable frame until it becomes natural.

---

## 8. Case 1: Node API High CPU After Release

### Symptom

A Node API service jumps from 35 percent CPU to 100 percent CPU after a deployment. Health checks begin failing. p99 latency rises from 400 ms to 8 seconds.

### First Questions

- Which version introduced the spike?
- Is the spike on all pods or a subset?
- Which endpoints changed?
- Did traffic volume or payload size change?
- Is event-loop delay high?
- Did dependency latency also rise?

### Signals To Inspect

- CPU by pod and version.
- Event-loop delay p95/p99.
- Request rate by endpoint.
- Latency by endpoint.
- Error rate.
- Recent deploy diff.
- CPU profile.

### Likely Root Causes

- Large JSON parse/stringify.
- Expensive validation.
- Regex backtracking.
- Synchronous compression or crypto.
- Sorting/filtering large arrays.
- Logging huge payloads.
- Inefficient loop added in release.

### Investigation Path

1. Confirm the regression aligns with release time.
2. Compare endpoint latency before and after release.
3. Check event-loop delay.
4. Capture CPU profile during spike.
5. Identify hottest stack.
6. Compare code diff around hot function.
7. Mitigate by rollback or feature flag if impact is severe.
8. Patch the hot path.

### Example Root Cause

```js
function buildBookingResponse(bookings, rooms) {
  return bookings.map(booking => ({
    ...booking,
    room: rooms.find(room => room.id === booking.roomId)
  }));
}
```

This is expensive when both arrays are large.

Better:

```js
function buildBookingResponse(bookings, rooms) {
  const roomsById = new Map(rooms.map(room => [room.id, room]));

  return bookings.map(booking => ({
    ...booking,
    room: roomsById.get(booking.roomId) ?? null
  }));
}
```

### Prevention

- Add load test with realistic data volume.
- Add CPU profile for the high-volume endpoint in performance testing.
- Add code review checklist for repeated lookups.
- Track event-loop delay alert.

### Strong Interview Answer

> I would first confirm the spike correlates with a release and identify affected endpoints. Since CPU and event-loop delay are high, I would capture a CPU profile and inspect the hottest stack. If the root cause is an O(n squared) transform or heavy serialization, I would mitigate with rollback or a feature flag, then fix the algorithm, verify latency and CPU, and add a load test and event-loop delay alert.

---

## 9. Case 2: Event Loop Blocked By Large JSON

### Symptom

Users report all APIs are slow whenever the export endpoint runs. CPU spikes and p99 latency rises across unrelated endpoints.

### First Questions

- Does the export endpoint load all rows into memory?
- Does it call `res.json` with a huge object?
- Does event-loop delay spike during exports?
- Are other endpoints slow only during export traffic?

### Bad Pattern

```js
app.get("/api/export", async (req, res) => {
  const rows = await reportRepository.loadAllRows(req.query.reportId);
  res.json(rows);
});
```

Problem:

- Huge DB result.
- Huge object graph.
- Expensive JSON serialization.
- Large memory pressure.
- Event loop blocked while serializing.

### Better Production Design

- Start an export job.
- Process in a worker.
- Stream rows to file/object storage.
- Return job ID.
- Let client poll or receive notification.
- Download file from storage/CDN.

### Streaming Shape

```js
import { pipeline } from "node:stream/promises";

app.get("/api/export.csv", async (req, res, next) => {
  try {
    res.setHeader("content-type", "text/csv");
    const stream = reportRepository.createCsvStream(req.query.reportId);
    await pipeline(stream, res);
  } catch (error) {
    next(error);
  }
});
```

### Prevention

- Limit synchronous export endpoints.
- Add response size budgets.
- Use async jobs for large exports.
- Track event-loop delay and heap during export.
- Add load test for export path.

### Strong Interview Answer

> Large JSON serialization can block Node even when database access is async. I would prove it with event-loop delay and CPU profiling. The fix is usually architectural: stream data or move export generation to an async job instead of building and stringifying a giant object in the request path.

---

## 10. Case 3: Catastrophic Regex Backtracking

### Symptom

One endpoint intermittently consumes a full CPU core. It happens more often with unusual user input. Error rate rises due to timeouts.

### Suspicious Code

```js
const emailLikePattern = /^([a-zA-Z0-9_.+-]+)+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/;

function isValidEmail(value) {
  return emailLikePattern.test(value);
}
```

### Investigation Path

1. Check CPU profile.
2. Look for regex functions or validation library stack.
3. Compare slow request payloads.
4. Test suspicious input locally with timing.
5. Replace unsafe regex.
6. Add length limits and validation tests.

### Safer Approach

```js
function isReasonableEmail(value) {
  if (typeof value !== "string") return false;
  if (value.length > 320) return false;
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value);
}
```

### Prevention

- Use length limits.
- Avoid nested quantifiers.
- Use safe-regex checks.
- Prefer proven validators.
- Add security performance tests for hostile input.

### Strong Interview Answer

> If CPU spikes are tied to specific input, I consider ReDoS. I would inspect CPU profiles and payloads, add input length limits, replace unsafe regex, and add tests for pathological inputs. This is both a performance and security incident.

---

## 11. Case 4: Memory Leak From Unbounded Map

### Symptom

Node pods restart every few hours due to memory limits. Heap grows steadily with traffic. Restart temporarily fixes it.

### Bad Code

```js
const requestResults = new Map();

export async function getResult(requestId, loader) {
  if (requestResults.has(requestId)) {
    return requestResults.get(requestId);
  }

  const result = await loader();
  requestResults.set(requestId, result);
  return result;
}
```

### Root Cause

- `requestId` is high-cardinality.
- Map is module-scoped.
- Entries are never removed.
- Heap grows forever.

### Investigation Path

1. Confirm heap grows, not only RSS.
2. Capture heap snapshot before and after traffic.
3. Compare retained `Map` entries.
4. Inspect retainer path to module-level map.
5. Patch with TTL/capacity or remove cache.

### Safer Cache

```js
class ExpiringMap {
  constructor(ttlMs) {
    this.ttlMs = ttlMs;
    this.values = new Map();
  }

  set(key, value) {
    this.values.set(key, {
      value,
      expiresAt: Date.now() + this.ttlMs
    });
  }

  get(key) {
    const entry = this.values.get(key);
    if (!entry) return undefined;

    if (Date.now() > entry.expiresAt) {
      this.values.delete(key);
      return undefined;
    }

    return entry.value;
  }

  cleanup() {
    const now = Date.now();
    for (const [key, entry] of this.values) {
      if (now > entry.expiresAt) {
        this.values.delete(key);
      }
    }
  }
}
```

### Prevention

- Caches must have TTL and capacity.
- Track cache size as metric.
- Avoid high-cardinality keys unless bounded.
- Add memory trend alert.

### Strong Interview Answer

> A memory leak means objects remain reachable. I would compare heap snapshots, inspect retained size and retainer paths, and look for module-level Maps, caches, queues, listeners, or pending request registries. The fix is ownership: remove references, add TTL/capacity, and monitor size.

---

## 12. Case 5: RSS Grows But Heap Looks Stable

### Symptom

Container memory grows until restart. V8 heap metrics look mostly stable. RSS and external memory grow.

### Likely Causes

- Buffers retained.
- Native addon memory.
- Large file uploads buffered in memory.
- Stream backpressure ignored.
- Memory fragmentation.

### Bad Upload Pattern

```js
app.post("/upload", async (req, res) => {
  const chunks = [];

  for await (const chunk of req) {
    chunks.push(chunk);
  }

  const file = Buffer.concat(chunks);
  await storage.save(file);
  res.sendStatus(204);
});
```

### Better Pattern

```js
import { pipeline } from "node:stream/promises";

app.post("/upload", async (req, res, next) => {
  try {
    const writeStream = storage.createWriteStream();
    await pipeline(req, writeStream);
    res.sendStatus(204);
  } catch (error) {
    next(error);
  }
});
```

### Investigation Path

1. Compare heap, RSS, external memory, arrayBuffers.
2. Check upload/download paths.
3. Inspect Buffer usage.
4. Check whether streams respect backpressure.
5. Reproduce with large payloads.
6. Add size limits and streaming.

### Strong Interview Answer

> Stable heap with growing RSS tells me to look outside ordinary JS objects: Buffers, native memory, streams, or fragmentation. I would inspect external memory, upload paths, and stream backpressure, then replace buffering with streaming and enforce size limits.

---

## 13. Case 6: Browser Memory Leak After Route Changes

### Symptom

A single-page app becomes slower after navigating between dashboard tabs. Mobile browsers crash after long sessions.

### Likely Root Causes

- Event listeners not removed.
- Intervals not cleared.
- Chart instances not destroyed.
- WebSocket subscriptions retained.
- Detached DOM nodes.
- Global store retains old route data.

### Bad Code

```js
function mountChart(container, data) {
  const chart = createChart(container, data);
  window.addEventListener("resize", () => chart.resize());
}
```

### Better Code

```js
function mountChart(container, data) {
  const chart = createChart(container, data);

  function handleResize() {
    chart.resize();
  }

  window.addEventListener("resize", handleResize);

  return function cleanup() {
    window.removeEventListener("resize", handleResize);
    chart.destroy();
  };
}
```

### Investigation Path

1. Reproduce route switching.
2. Take heap snapshot baseline.
3. Switch routes repeatedly.
4. Force GC.
5. Take second snapshot.
6. Search detached nodes and chart objects.
7. Inspect retainer paths.
8. Add cleanup.

### Prevention

- Component cleanup tests.
- Long-session E2E smoke test.
- Heap budget after repeated navigation.
- Library lifecycle checklist.

### Strong Interview Answer

> In browser memory leaks, I look for retained DOM nodes, timers, event listeners, subscriptions, and closures. I would compare heap snapshots after repeated navigation and inspect retainer paths. The fix is cleanup ownership, not forcing GC.

---

## 14. Case 7: Input Lag From Expensive Filtering

### Symptom

Search input freezes after typing. INP is poor on mid-range phones. Desktop feels acceptable.

### Bad Code

```js
function handleSearchInput(value) {
  const results = allBookings
    .filter(booking => booking.guestName.toLowerCase().includes(value.toLowerCase()))
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));

  renderResults(results);
}
```

### Problems

- Runs on every keystroke.
- Repeated lowercase conversion.
- Repeated date parsing inside sort comparator.
- Potentially large render.
- Main thread blocked.

### Investigation Path

1. Check INP and long tasks.
2. Record typing trace.
3. Inspect event handler cost.
4. Check render cost and DOM size.
5. Apply targeted fix.

### Better Shape

```js
const searchableBookings = allBookings.map(booking => ({
  ...booking,
  guestNameSearch: booking.guestName.toLowerCase(),
  createdAtTimestamp: Date.parse(booking.createdAt)
}));

const handleSearchInput = debounce(value => {
  const query = value.toLowerCase();

  const results = searchableBookings
    .filter(booking => booking.guestNameSearch.includes(query))
    .sort((left, right) => right.createdAtTimestamp - left.createdAtTimestamp);

  renderResults(results.slice(0, 100));
}, 150);
```

### Prevention

- Measure interactions on slower devices.
- Virtualize large result lists.
- Move heavy filtering to Web Worker if needed.
- Avoid expensive comparator work.

### Strong Interview Answer

> I would use a Performance trace around typing and check long tasks. If filtering and rendering dominate, I would debounce, precompute searchable fields, avoid repeated date parsing, limit visible results, virtualize the list, or move heavy work to a worker.

---

## 15. Case 8: LCP Regression From Late Image Discovery

### Symptom

LCP regresses on product pages after design changes. The hero image is the LCP element.

### Bad Pattern

```css
.hero {
  background-image: url("/images/hotel-large.webp");
}
```

The browser may discover CSS background images later than important HTML images.

### Better Pattern

```html
<img
  src="/images/hotel-800.webp"
  srcset="/images/hotel-400.webp 400w, /images/hotel-800.webp 800w, /images/hotel-1200.webp 1200w"
  sizes="100vw"
  width="1200"
  height="700"
  alt="Hotel exterior"
/>
```

### Investigation Path

1. Confirm LCP element.
2. Compare Network waterfall.
3. Check resource discovery time.
4. Check image size and format.
5. Check if JS blocks rendering.
6. Fix markup and priority.

### Prevention

- LCP budget per route.
- Image review checklist.
- RUM by route and device.
- Visual regression plus performance review for hero changes.

### Strong Interview Answer

> For LCP regressions I identify the LCP element, then check whether delay is TTFB, resource load, render-blocking assets, or client rendering. If the LCP image is discovered late through CSS, I would expose it as an image, size it correctly, and validate with waterfall and RUM.

---

## 16. Case 9: CLS From Injected Banner

### Symptom

CLS rises after adding a promotional banner. Users complain buttons move while clicking.

### Bad Pattern

```js
setTimeout(() => {
  document.body.prepend(createPromoBanner());
}, 1_000);
```

### Root Cause

- Content injected above existing content after initial render.
- No reserved space.
- User-visible layout shift.

### Better Pattern

```html
<div id="promo-slot" style="min-height: 64px"></div>
```

```js
const slot = document.querySelector("#promo-slot");
slot.replaceChildren(createPromoBanner());
```

### Investigation Path

1. Check Web Vitals attribution.
2. Use Performance trace screenshots.
3. Identify shifting element.
4. Reserve space or avoid injecting above current viewport.

### Strong Interview Answer

> CLS is a visual stability issue. I would identify which element shifted, reserve dimensions for late content, avoid inserting content above existing viewport, and verify p75 CLS in RUM.

---

## 17. Case 10: Bundle Regression From Accidental Import

### Symptom

Initial JS bundle grows by 700 KB after a release. Mobile LCP and INP worsen.

### Bad Import

```js
import { AdminReportBuilder } from "../admin/reporting";

export function CheckoutPage() {
  return null;
}
```

A shared barrel file pulled admin-only code into the checkout chunk.

### Investigation Path

1. Compare bundle analyzer before/after.
2. Check route chunks.
3. Find large dependency or module path.
4. Inspect import graph.
5. Remove accidental import or split chunk.
6. Add CI budget.

### Better Pattern

```js
async function openAdminReportBuilder() {
  const { AdminReportBuilder } = await import("../admin/reporting/AdminReportBuilder.js");
  return AdminReportBuilder.open();
}
```

### Prevention

- Bundle analyzer in CI.
- Route-level budgets.
- Avoid unsafe barrel exports for heavy modules.
- Review dependency changes.

### Strong Interview Answer

> I would compare bundle analyzer output by route and identify what moved into the initial chunk. Then I would remove the accidental import, code split rare heavy features, and add a bundle budget so the regression is caught in CI.

---

## 18. Case 11: Third-Party Script Blocks Main Thread

### Symptom

Checkout interaction latency worsens, but app code did not change much. Performance trace shows long tasks from a third-party analytics script.

### Investigation Path

1. Check Performance trace attribution.
2. Review third-party scripts added recently.
3. Measure cost by route.
4. Verify if script is needed before checkout completes.
5. Defer or remove from critical path.

### Mitigations

- Load after consent.
- Load after user interaction.
- Use `async` or `defer` where appropriate.
- Server-side event forwarding.
- Route-based loading.
- Vendor budget and ownership.

### Strong Interview Answer

> Third-party scripts are production dependencies. If they block the main thread on checkout, I would measure their cost, assign ownership, move them off the critical path, and add a budget. User interaction performance should not be silently traded away for unbounded analytics.

---

## 19. Case 12: Duplicate API Calls From React Effect

### Symptom

Backend request volume doubles after a frontend release. Users see occasional flicker. API costs rise.

### Bad Code

```jsx
function BookingPage({ bookingId }) {
  const [booking, setBooking] = useState(null);

  useEffect(() => {
    fetch(`/api/bookings/${bookingId}`)
      .then(response => response.json())
      .then(setBooking);
  });

  return <BookingDetails booking={booking} />;
}
```

Problem:

- Missing dependency array.
- Effect runs after every render.
- `setBooking` causes another render.

### Better Code

```jsx
function BookingPage({ bookingId }) {
  const [booking, setBooking] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    fetch(`/api/bookings/${bookingId}`, { signal: controller.signal })
      .then(response => response.json())
      .then(setBooking)
      .catch(error => {
        if (error.name !== "AbortError") {
          throw error;
        }
      });

    return () => controller.abort();
  }, [bookingId]);

  return <BookingDetails booking={booking} />;
}
```

### Investigation Path

1. Check API request rate by client version.
2. Inspect browser Network panel.
3. Check frontend release diff.
4. Check effects and retry logic.
5. Add cancellation and dependency correctness.

### Strong Interview Answer

> A frontend bug can create a backend incident. I would segment request rate by app version and route, inspect the Network panel and frontend diff, then fix the effect dependency/cancellation logic and add monitoring for duplicate client calls.

---

## 20. Case 13: Race Condition Shows Stale Data

### Symptom

Users type quickly in search. Sometimes older results replace newer results.

### Bad Code

```js
async function search(query) {
  const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
  const results = await response.json();
  renderResults(results);
}
```

Problem:

- Requests can resolve out of order.
- Older response can win.

### Better Code With Abort

```js
let activeController;

async function search(query) {
  activeController?.abort();
  activeController = new AbortController();

  try {
    const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`, {
      signal: activeController.signal
    });

    const results = await response.json();
    renderResults(results);
  } catch (error) {
    if (error.name !== "AbortError") {
      showSearchError();
    }
  }
}
```

### Better Code With Sequence

```js
let latestRequestId = 0;

async function search(query) {
  const requestId = latestRequestId + 1;
  latestRequestId = requestId;

  const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
  const results = await response.json();

  if (requestId === latestRequestId) {
    renderResults(results);
  }
}
```

### Strong Interview Answer

> Async race bugs happen when older work completes after newer work. I would reproduce with throttled network, inspect request order, then use cancellation or sequence guards so only the latest request updates UI.

---

## 21. Case 14: Unhandled Promise Rejections Crash Node

### Symptom

A service occasionally crashes under dependency failures. Logs show unhandled promise rejections.

### Bad Code

```js
app.post("/api/payment", async (req, res) => {
  const payment = paymentService.charge(req.body);
  res.json({ paymentId: payment.id });
});
```

Problem:

- Missing `await`.
- Rejection is not handled in request flow.
- Response may use unresolved promise incorrectly.

### Better Code

```js
app.post("/api/payment", async (req, res, next) => {
  try {
    const payment = await paymentService.charge(req.body);
    res.json({ paymentId: payment.id });
  } catch (error) {
    next(error);
  }
});
```

### Investigation Path

1. Check crash logs.
2. Inspect unhandled rejection stack.
3. Search recent async code changes.
4. Check missing `await` or promise chain return.
5. Add tests for dependency failure.

### Prevention

- Use async error handling wrapper.
- Lint floating promises where TypeScript is available.
- Add failure-path tests.
- Add process-level logging but do not rely on it as recovery.

### Strong Interview Answer

> Unhandled rejections mean async errors escaped ownership. I would find the promise that was not awaited or returned, add request-level error handling, test the failure path, and rely on process-level handlers only for logging and graceful shutdown, not normal recovery.

---

## 22. Case 15: Promise.all Causes Partial Failure Incident

### Symptom

Profile page fails completely if one optional widget API fails.

### Bad Code

```js
const [profile, recommendations, promotions] = await Promise.all([
  fetchProfile(userId),
  fetchRecommendations(userId),
  fetchPromotions(userId)
]);
```

Problem:

- `Promise.all` rejects when any promise rejects.
- Optional data breaks critical page.

### Better Code

```js
const [profile, recommendationsResult, promotionsResult] = await Promise.all([
  fetchProfile(userId),
  fetchRecommendations(userId).then(
    value => ({ status: "fulfilled", value }),
    reason => ({ status: "rejected", reason })
  ),
  fetchPromotions(userId).then(
    value => ({ status: "fulfilled", value }),
    reason => ({ status: "rejected", reason })
  )
]);

const recommendations = recommendationsResult.status === "fulfilled"
  ? recommendationsResult.value
  : [];

const promotions = promotionsResult.status === "fulfilled"
  ? promotionsResult.value
  : [];
```

Alternative:

```js
const results = await Promise.allSettled([
  fetchRecommendations(userId),
  fetchPromotions(userId)
]);
```

### Strong Interview Answer

> I use `Promise.all` for all-or-nothing dependencies. For optional sections, I use `allSettled`, fallbacks, and error boundaries so a non-critical dependency does not take down the page.

---

## 23. Case 16: Retry Storm Takes Down Dependency

### Symptom

A downstream API becomes slow. Instead of recovering, traffic multiplies because every client retries aggressively. The dependency falls over.

### Bad Code

```js
async function callCatalog() {
  try {
    return await fetch("https://catalog.example.com/items");
  } catch {
    return callCatalog();
  }
}
```

### Better Retry

```js
async function retryWithBackoff(task, maxAttempts = 3) {
  let lastError;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await task();
    } catch (error) {
      lastError = error;
      if (attempt === maxAttempts) break;

      const jitterMs = Math.floor(Math.random() * 100);
      const delayMs = 100 * 2 ** (attempt - 1) + jitterMs;
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }

  throw lastError;
}
```

### Investigation Path

1. Check request rate to dependency.
2. Segment original requests vs retries.
3. Check timeout configuration.
4. Check retry count and jitter.
5. Add circuit breaker or temporary rate limit.

### Prevention

- Max attempts.
- Exponential backoff.
- Jitter.
- Timeouts.
- Circuit breaker.
- Retry budgets.
- Idempotency keys where writes are retried.

### Strong Interview Answer

> Retries can amplify outages. I would cap retries, add backoff and jitter, use timeouts, and protect dependencies with circuit breakers and retry budgets. During active incidents I would reduce retry pressure quickly.

---

## 24. Case 17: Missing Timeout Causes Threadless Waiting

### Symptom

Node service p99 latency rises because calls to a dependency hang for 60 seconds. CPU is low, event-loop delay is low, but requests pile up.

### Bad Code

```js
async function loadPrice(hotelId) {
  const response = await fetch(`https://pricing.example.com/hotels/${hotelId}`);
  return response.json();
}
```

### Better Code

```js
async function fetchWithTimeout(url, timeoutMs) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, { signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}
```

### Investigation Path

1. Check traces for slow dependency spans.
2. Check event-loop delay to rule out CPU block.
3. Check open request count.
4. Check timeout settings.
5. Add timeout and fallback.

### Strong Interview Answer

> Low CPU with high latency usually points to waiting, not CPU. I would inspect traces and dependency spans, add timeouts and fallbacks, and ensure hung calls do not consume request capacity indefinitely.

---

## 25. Case 18: Connection Pool Saturation

### Symptom

API latency spikes under load. CPU is moderate. DB CPU is high. App logs show waiting for connections.

### Investigation Path

1. Check DB pool wait time.
2. Check active and idle connections.
3. Check slow queries.
4. Check recent code for new query loops.
5. Check transaction duration.
6. Tune only after understanding DB capacity.

### Common Root Causes

- N plus one queries.
- Missing index.
- Long transaction.
- Pool too small for normal traffic.
- Pool too large causing DB contention.
- Query added to hot endpoint.

### Strong Interview Answer

> I would not blindly increase pool size. I would inspect pool wait, DB saturation, query latency, and transaction duration. The right fix may be query optimization, indexing, batching, caching, or pool tuning based on actual DB capacity.

---

## 26. Case 19: N Plus One Query From Node API

### Symptom

Endpoint works in staging but times out in production with real data.

### Bad Code

```js
app.get("/api/hotels", async (req, res) => {
  const hotels = await hotelRepository.findAll();

  const response = [];
  for (const hotel of hotels) {
    const rooms = await roomRepository.findByHotelId(hotel.id);
    response.push({ ...hotel, rooms });
  }

  res.json(response);
});
```

### Better Code

```js
app.get("/api/hotels", async (req, res) => {
  const hotels = await hotelRepository.findAll();
  const rooms = await roomRepository.findByHotelIds(hotels.map(hotel => hotel.id));

  const roomsByHotelId = new Map();
  for (const room of rooms) {
    const list = roomsByHotelId.get(room.hotelId) ?? [];
    list.push(room);
    roomsByHotelId.set(room.hotelId, list);
  }

  res.json(hotels.map(hotel => ({
    ...hotel,
    rooms: roomsByHotelId.get(hotel.id) ?? []
  })));
});
```

### Strong Interview Answer

> Staging often hides data-size bugs. I would inspect traces and query count per request. If I see N plus one, I batch queries, use joins/includes where appropriate, add indexes, and test with production-like data volume.

---

## 27. Case 20: Queue Backlog After Worker Release

### Symptom

Queue depth grows. Oldest message age rises. API is fine, but async jobs are delayed by hours.

### First Questions

- Did worker throughput drop?
- Did job volume increase?
- Are jobs failing and retrying?
- Is dependency latency high?
- Did worker concurrency change?
- Is one poison message blocking processing?

### Investigation Path

1. Check queue depth and oldest age.
2. Check worker success/failure counts.
3. Check job duration percentiles.
4. Check retry count.
5. Check dependency spans.
6. Roll back worker if release caused regression.

### Prevention

- Dead-letter queues.
- Retry caps.
- Worker metrics.
- Job timeouts.
- Concurrency limits.
- Idempotent handlers.

### Strong Interview Answer

> For queue incidents I check depth, oldest message age, processing time, failure/retry rate, and worker health. The fix may be rollback, scaling workers, dependency mitigation, concurrency tuning, or isolating poison messages.

---

## 28. Case 21: Poison Message Infinite Retry

### Symptom

One bad job fails repeatedly and consumes worker capacity.

### Bad Handler

```js
async function handleJob(job) {
  const payload = JSON.parse(job.body);
  await processPayment(payload);
}
```

If payload is invalid, the job keeps retrying forever.

### Better Handler

```js
async function handleJob(job) {
  let payload;

  try {
    payload = JSON.parse(job.body);
  } catch (error) {
    await deadLetter(job, "INVALID_JSON");
    return;
  }

  if (!isValidPaymentPayload(payload)) {
    await deadLetter(job, "INVALID_PAYMENT_PAYLOAD");
    return;
  }

  await processPayment(payload);
}
```

### Strong Interview Answer

> Not all failures are retryable. I classify errors as retryable or permanent, use dead-letter queues for poison messages, cap retries, and make handlers idempotent.

---

## 29. Case 22: WebSocket Reconnect Storm

### Symptom

A gateway restart causes thousands of clients to reconnect immediately. Backend CPU and connection count spike.

### Root Causes

- No reconnect jitter.
- Clients reconnect instantly.
- Server restart drops all connections at once.
- Authentication endpoint receives storm.

### Better Client Reconnect

```js
function reconnect(attempt) {
  const baseDelayMs = Math.min(30_000, 500 * 2 ** attempt);
  const jitterMs = Math.floor(Math.random() * 1_000);

  setTimeout(() => {
    connectWebSocket(attempt + 1);
  }, baseDelayMs + jitterMs);
}
```

### Prevention

- Exponential backoff with jitter.
- Connection draining.
- Rate limits.
- Staggered deploys.
- Backpressure on auth/session validation.

### Strong Interview Answer

> Reconnect storms are retry storms with sockets. I would add jittered exponential backoff, drain connections during deploys, rate limit reconnect paths, and monitor connection count and authentication load.

---

## 30. Case 23: WebSocket Memory Leak From Send Queue

### Symptom

Memory grows when some clients are on poor networks. Server stores messages for slow clients.

### Bad Pattern

```js
function sendToClient(client, message) {
  client.queue.push(message);
}
```

### Better Pattern

```js
function sendToClient(client, message) {
  if (client.queue.length > 100) {
    client.close(1013, "Client too slow");
    return;
  }

  client.queue.push(message);
  flushClientQueue(client);
}
```

### Strong Interview Answer

> Slow clients can create server memory pressure. I would bound per-client queues, drop or disconnect slow clients, track queue size, and apply backpressure instead of buffering forever.

---

## 31. Case 24: CORS Misconfiguration Looks Like API Outage

### Symptom

Frontend calls fail in browser after deployment, but API works with curl.

### Investigation Path

1. Check browser console error.
2. Check preflight request.
3. Check `Access-Control-Allow-Origin`.
4. Check credentials mode and cookies.
5. Check environment-specific domain config.

### Strong Interview Answer

> If curl works but browser fails, I suspect browser policy such as CORS, cookies, mixed content, or CSP. I would inspect the preflight and response headers, then fix the allowed origin/credentials configuration for the deployed domain.

---

## 32. Case 25: Cookie Auth Fails After Browser Change

### Symptom

Users are logged out after redirect from identity provider. Works in local dev.

### Likely Causes

- Missing `SameSite=None` for cross-site auth flow.
- Missing `Secure` on cross-site cookie.
- Domain mismatch.
- HTTP instead of HTTPS.
- Third-party cookie restrictions.

### Cookie Shape

```http
Set-Cookie: session=abc; HttpOnly; Secure; SameSite=None; Path=/
```

### Strong Interview Answer

> Browser auth issues often involve cookie attributes. I would inspect Set-Cookie headers, SameSite, Secure, domain, path, HTTPS, and redirect domain. Browser behavior can differ from local dev because local flows are often same-site.

---

## 33. Case 26: CSP Blocks Production Script

### Symptom

A new frontend release loads blank page for some users. Console shows CSP violation.

### Investigation Path

1. Check browser console.
2. Inspect `Content-Security-Policy` header.
3. Check script source, nonce, hash, or CDN domain.
4. Compare staging vs production headers.
5. Fix policy or script loading path.

### Strong Interview Answer

> A blank page with console CSP violations is a policy/config issue, not necessarily JavaScript logic. I would inspect the blocked resource and update CSP safely without weakening it broadly.

---

## 34. Case 27: Source Maps Missing During Incident

### Symptom

Production error stack points to minified file line 1 column 723991. The team cannot quickly map it to source.

### Prevention

- Upload source maps during build.
- Tag errors with release version.
- Keep source maps private if required.
- Verify upload in CI.
- Keep artifacts aligned with deployed assets.

### Strong Interview Answer

> Source maps are part of production readiness. I would ensure release-tagged source maps are uploaded securely so minified production errors can be mapped to exact source code quickly.

---

## 35. Case 28: Hydration Mismatch Breaks Interactions

### Symptom

Server-rendered page appears, but buttons do not work correctly. Console shows hydration mismatch.

### Common Causes

- Rendering `Date.now()` on server and client.
- Random IDs generated differently.
- Browser-only data used during server render.
- Locale/time zone mismatch.
- Feature flag differs between server and client.

### Bad Code

```jsx
function Header() {
  return <div>{new Date().toLocaleTimeString()}</div>;
}
```

### Better Pattern

```jsx
function Header() {
  const [time, setTime] = useState(null);

  useEffect(() => {
    setTime(new Date().toLocaleTimeString());
  }, []);

  return <div>{time ?? ""}</div>;
}
```

### Strong Interview Answer

> Hydration requires server and client initial markup to match. I would look for non-deterministic rendering, browser-only APIs, feature flag mismatch, or locale differences, then move client-only values into effects or provide deterministic server data.

---

## 36. Case 29: Service Worker Serves Stale Broken Assets

### Symptom

Some users see a blank page after release, but hard refresh fixes it.

### Likely Root Cause

A service worker cached old HTML that points to deleted JS chunks, or cached old JS with new HTML.

### Investigation Path

1. Check affected users have old service worker version.
2. Inspect cache storage.
3. Check asset naming and cache strategy.
4. Verify old assets remain available during rollout.
5. Add service worker update flow.

### Prevention

- Version caches.
- Delete old caches carefully.
- Keep old hashed assets available for a safe window.
- Avoid caching HTML too aggressively.
- Add service worker version telemetry.

### Strong Interview Answer

> Service workers are a deployment surface. If hard refresh fixes a blank page, I check cache versioning, old chunk availability, and service worker update behavior. The fix is coordinated asset caching and safer rollout.

---

## 37. Case 30: CDN Cache Poisoning By Missing Vary

### Symptom

Users occasionally see content for the wrong locale or logged-out variant.

### Likely Cause

Cache key does not vary by header/cookie/query that affects response.

### Investigation Path

1. Inspect response headers.
2. Check CDN cache key rules.
3. Check `Vary` behavior.
4. Check whether personalized responses are cached publicly.
5. Purge bad cache and fix policy.

### Strong Interview Answer

> Caching bugs can look like random frontend bugs. I would inspect CDN cache keys, `Vary` headers, cookies, auth state, and whether personalized responses are cacheable. Correctness comes before cache hit rate.

---

## 38. Case 31: API Storm From Polling

### Symptom

A dashboard creates massive backend traffic every minute. Many browser tabs remain open all day.

### Bad Code

```js
setInterval(() => {
  fetch("/api/dashboard/summary");
}, 1_000);
```

### Better Pattern

```js
let stopped = false;

async function pollDashboard() {
  while (!stopped) {
    await fetch("/api/dashboard/summary");
    await new Promise(resolve => setTimeout(resolve, 30_000));
  }
}

function stopPolling() {
  stopped = true;
}
```

Better still:

- Pause when tab hidden.
- Use server push where appropriate.
- Use stale-while-revalidate.
- Back off on errors.
- Add jitter.

### Strong Interview Answer

> Polling needs ownership. I would check request rate by route and client version, pause polling on hidden tabs, add backoff and jitter, increase interval, and consider push or cache-based patterns.

---

## 39. Case 32: Hidden Tab Keeps Burning CPU

### Symptom

Laptop fans spin when app is in background. Users report battery drain.

### Bad Pattern

```js
setInterval(renderLiveChart, 500);
```

### Better Pattern

```js
document.addEventListener("visibilitychange", () => {
  if (document.hidden) {
    pauseLiveChart();
  } else {
    resumeLiveChart();
  }
});
```

### Strong Interview Answer

> Browser apps should respect page visibility. I would pause animations, polling, and non-critical timers when hidden, then resume safely when visible.

---

## 40. Case 33: Infinite Render Loop

### Symptom

Frontend CPU spikes and page freezes after opening a component.

### Bad Code

```jsx
function UserCard({ user }) {
  const [displayName, setDisplayName] = useState("");

  setDisplayName(user.name.trim());

  return <div>{displayName}</div>;
}
```

### Better Code

```jsx
function UserCard({ user }) {
  const displayName = user.name.trim();
  return <div>{displayName}</div>;
}
```

Or if state is truly needed:

```jsx
useEffect(() => {
  setDisplayName(user.name.trim());
}, [user.name]);
```

### Strong Interview Answer

> State updates during render can create render loops. I would inspect profiler/render counts and move derived values out of state or update state inside a properly scoped effect.

---

## 41. Case 34: Context Update Re-Renders Entire App

### Symptom

Typing in one field causes the whole React app to re-render.

### Root Cause

A broad context provider stores frequently changing state.

### Better Patterns

- Colocate state near usage.
- Split context by update frequency.
- Use selectors where supported.
- Memoize provider value carefully.
- Avoid global state for local input.

### Strong Interview Answer

> I would use React Profiler to identify broad re-renders. If context updates are too wide, I split context, colocate state, or use selectors so high-frequency updates do not invalidate unrelated UI.

---

## 42. Case 35: Large List Without Virtualization

### Symptom

Dashboard renders 20,000 rows. Initial render takes seconds and scrolling is janky.

### Investigation Path

1. Check DOM node count.
2. Record Performance trace.
3. Check rendering and layout time.
4. Implement pagination or virtualization.

### Visible Range Function

```js
function getVisibleRange(scrollTop, rowHeight, viewportHeight, totalRows) {
  const overscan = 8;
  const start = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
  const count = Math.ceil(viewportHeight / rowHeight) + overscan * 2;
  const end = Math.min(totalRows, start + count);

  return { start, end };
}
```

### Strong Interview Answer

> Large DOM lists hurt layout, paint, memory, and accessibility tree generation. I would paginate or virtualize, keeping DOM nodes proportional to viewport size instead of data size.

---

## 43. Case 36: Layout Thrashing During Resize

### Symptom

Window resizing or dragging panels causes jank.

### Bad Code

```js
for (const card of cards) {
  card.style.width = `${container.offsetWidth / 3}px`;
  card.style.height = `${card.offsetWidth}px`;
}
```

### Better Code

```js
const containerWidth = container.offsetWidth;
const cardWidth = containerWidth / 3;

for (const card of cards) {
  card.style.width = `${cardWidth}px`;
  card.style.height = `${cardWidth}px`;
}
```

### Strong Interview Answer

> Layout thrashing happens when DOM reads and writes are interleaved. I would batch reads, batch writes, prefer CSS layout, and verify with Performance trace layout events.

---

## 44. Case 37: LocalStorage Blocks Startup

### Symptom

App startup is slow for returning users with large saved state.

### Bad Code

```js
const savedState = JSON.parse(localStorage.getItem("app-state"));
hydrateApp(savedState);
```

### Problem

- `localStorage` is synchronous.
- Large JSON parse blocks main thread.
- Startup path is critical.

### Better Approach

- Store less data.
- Load non-critical state lazily.
- Use IndexedDB for larger data.
- Version and clean old state.
- Avoid storing huge normalized app state.

### Strong Interview Answer

> Synchronous storage on startup can block the main thread. I would measure startup trace, reduce stored data, lazily load non-critical state, and use async storage for larger data.

---

## 45. Case 38: Memory Leak From Analytics Queue

### Symptom

Frontend heap grows during offline usage. When network is blocked, analytics events accumulate forever.

### Bad Code

```js
const analyticsQueue = [];

function track(event) {
  analyticsQueue.push(event);
  flushLater();
}
```

### Better Code

```js
const analyticsQueue = [];
const MAX_ANALYTICS_QUEUE = 500;

function track(event) {
  if (analyticsQueue.length >= MAX_ANALYTICS_QUEUE) {
    analyticsQueue.shift();
  }

  analyticsQueue.push(event);
  flushLater();
}
```

### Strong Interview Answer

> Offline queues must be bounded. I would cap queue size, drop low-priority events, persist carefully if needed, and avoid letting telemetry harm the product experience.

---

## 46. Case 39: Dependency Timeout Causes Cascading Failure

### Symptom

Recommendation service slows down. Main API waits too long and eventually all request workers are busy.

### Fix Pattern

```js
async function getRecommendations(userId) {
  try {
    const response = await fetchWithTimeout(
      `https://recommendations.example.com/users/${userId}`,
      300
    );

    return response.json();
  } catch {
    return [];
  }
}
```

### Strong Interview Answer

> Optional dependencies should fail soft. I would use short timeouts, fallbacks, circuit breakers, and traces so one slow dependency does not cascade into a full API incident.

---

## 47. Case 40: Circuit Breaker Needed

### Symptom

A dependency is failing. Every request still calls it, making latency worse.

### Simple Circuit Breaker Shape

```js
function createCircuitBreaker({ failureThreshold, cooldownMs }) {
  let failures = 0;
  let openedAt = 0;

  return async function call(task) {
    if (openedAt && Date.now() - openedAt < cooldownMs) {
      throw new Error("Circuit open");
    }

    try {
      const result = await task();
      failures = 0;
      openedAt = 0;
      return result;
    } catch (error) {
      failures += 1;
      if (failures >= failureThreshold) {
        openedAt = Date.now();
      }
      throw error;
    }
  };
}
```

### Strong Interview Answer

> Circuit breakers prevent repeated calls to a known unhealthy dependency. I would combine them with timeouts, fallback behavior, metrics, and careful recovery logic.

---

## 48. Case 41: Request Body Too Large Blocks Node

### Symptom

Large POST requests cause high memory and event-loop delay.

### Fix

```js
app.use(express.json({ limit: "1mb" }));
```

For file uploads:

- Stream files.
- Validate content length.
- Upload directly to object storage if possible.
- Scan asynchronously.

### Strong Interview Answer

> Large request bodies are resource exhaustion risks. I would enforce body limits, stream legitimate large uploads, and avoid parsing unbounded JSON on the event loop.

---

## 49. Case 42: Command Injection Patch Causes CPU Regression

### Symptom

A security patch replaces shell command usage with a pure JS parser, but CPU spikes under large inputs.

### Senior Investigation

- Security fix is necessary.
- Performance regression still needs handling.
- Profile new parser.
- Add input limits.
- Move heavy parsing to worker/job if needed.
- Stream if possible.

### Strong Interview Answer

> Security and performance must both hold. I would keep the injection fix, profile the new safe path, add input bounds, and move heavy work off the request path if needed.

---

## 50. Case 43: Logging Huge Payloads Creates Latency

### Symptom

Latency increases after adding request logging for debugging.

### Bad Code

```js
logger.info({ body: req.body, headers: req.headers }, "incoming request");
```

### Better Code

```js
logger.info({
  requestId: req.id,
  method: req.method,
  path: req.route?.path ?? req.path,
  contentLength: req.headers["content-length"]
}, "incoming request");
```

### Strong Interview Answer

> Logging can become CPU, I/O, privacy, and cost overhead. I would log structured metadata, avoid bodies and secrets, sample where appropriate, and ensure logging transports are production-safe.

---

## 51. Case 44: High Cardinality Metrics Break Dashboard

### Symptom

Metrics cost spikes and dashboards become slow after adding labels.

### Bad Metric

```js
metrics.increment("api.request", {
  userId: req.user.id,
  path: req.path
});
```

### Better Metric

```js
metrics.increment("api.request", {
  route: req.route?.path ?? "unknown",
  method: req.method,
  statusClass: `${Math.floor(res.statusCode / 100)}xx`
});
```

### Strong Interview Answer

> Observability data must be bounded. I avoid high-cardinality labels like user ID or raw URL and use route templates and controlled dimensions.

---

## 52. Case 45: Feature Flag Causes Inconsistent Behavior

### Symptom

Only some users see errors. Logs show mixed code paths.

### Investigation Path

1. Segment metrics by flag state.
2. Check rollout percentage.
3. Check user targeting rules.
4. Check server and client flag consistency.
5. Disable flag if needed.

### Strong Interview Answer

> Feature flags are part of production state. I would segment metrics by flag, verify targeting, and use the flag for fast mitigation if the new path is bad.

---

## 53. Case 46: Environment Config Mismatch

### Symptom

Production fails but staging works. Same code, different behavior.

### Common Causes

- Wrong API base URL.
- Missing env var.
- Different Node version.
- Different build target.
- Different CSP/CORS config.
- Different CDN caching.
- Different feature flags.

### Strong Interview Answer

> When code is the same but behavior differs, I compare environment config, runtime versions, secrets, flags, headers, CDN behavior, and infrastructure settings.

---

## 54. Case 47: Node Version Upgrade Regression

### Symptom

After Node upgrade, memory or latency changes.

### Investigation Path

1. Compare Node/V8 release notes.
2. Check dependency compatibility.
3. Compare GC behavior.
4. Run load tests before and after.
5. Check native addons.
6. Roll back runtime if needed.

### Strong Interview Answer

> Runtime upgrades can change performance behavior. I would test with production-like traffic, compare GC and CPU profiles, and roll out gradually with version-segmented dashboards.

---

## 55. Case 48: ESM/CommonJS Import Regression

### Symptom

Cold start gets slower after switching module style or adding a package.

### Investigation Path

- Check import graph.
- Check top-level side effects.
- Check dynamic imports.
- Check bundler output.
- Check serverless cold start metrics.

### Strong Interview Answer

> Module loading can affect startup and cold starts. I would inspect import graphs and top-level side effects, then lazy-load rare heavy modules where appropriate.

---

## 56. Case 49: Serverless Cold Start Spike

### Symptom

First request after idle takes several seconds.

### Causes

- Large bundle.
- Heavy top-level initialization.
- Cold DB connection.
- Large dependency graph.
- Runtime startup.

### Fixes

- Reduce bundle and dependencies.
- Move rare work out of top level.
- Reuse clients across invocations carefully.
- Use provisioned concurrency if business-critical.
- Keep warm only when justified.

### Strong Interview Answer

> For serverless cold starts I inspect initialization time, bundle size, top-level imports, connection setup, and runtime choice. Fixes include reducing dependencies, lazy loading, connection reuse, and provisioned concurrency where cost is justified.

---

## 57. Case 50: Edge Runtime Unsupported API

### Symptom

Code works in Node but fails at the edge runtime.

### Cause

Edge runtimes may not support all Node APIs like `fs`, some crypto APIs, native modules, or long-running processes.

### Strong Interview Answer

> Browser, Node, and edge runtimes have different APIs and performance constraints. I would check runtime compatibility, remove Node-only APIs from edge code, and keep edge handlers small and stateless.

---

## 58. Case 51: AsyncLocalStorage Stores Too Much

### Symptom

Memory grows and request context objects are large.

### Bad Code

```js
requestContext.run({ req, res, user, body: req.body }, next);
```

### Better Code

```js
requestContext.run({
  requestId: req.id,
  userId: req.user?.id
}, next);
```

### Strong Interview Answer

> Request context should store small metadata, not full request objects or bodies. I would inspect retained objects and reduce context to IDs needed for correlation.

---

## 59. Case 52: EventEmitter Listener Leak

### Symptom

Node logs `MaxListenersExceededWarning`. Memory grows slowly.

### Bad Code

```js
function handleRequest(req) {
  eventBus.on("booking.updated", booking => {
    notify(req.user.id, booking);
  });
}
```

### Better Code

```js
function subscribeForRequest(req, res) {
  function handleBookingUpdated(booking) {
    notify(req.user.id, booking);
  }

  eventBus.on("booking.updated", handleBookingUpdated);

  res.on("finish", () => {
    eventBus.off("booking.updated", handleBookingUpdated);
  });
}
```

### Strong Interview Answer

> Listener leaks keep callbacks and captured state alive. I would inspect listener counts, owner lifecycle, and cleanup paths, then remove listeners when the request/component/subscription ends.

---

## 60. Case 53: Abort Missing In Component Fetch

### Symptom

User navigates away, but slow request completes and updates unmounted component or stale state.

### Better Pattern

```jsx
useEffect(() => {
  const controller = new AbortController();

  async function load() {
    const response = await fetch(url, { signal: controller.signal });
    const data = await response.json();
    setData(data);
  }

  load().catch(error => {
    if (error.name !== "AbortError") {
      setError(error);
    }
  });

  return () => controller.abort();
}, [url]);
```

### Strong Interview Answer

> Async UI work needs lifecycle ownership. I use abort or sequence guards so stale requests cannot update state after navigation or newer inputs.

---

## 61. Case 54: Payment Double Submit

### Symptom

Users are charged twice when they double-click checkout or refresh during slow payment.

### Client Mitigation

```js
let submitting = false;

async function submitPayment(payload) {
  if (submitting) return;
  submitting = true;

  try {
    await sendPayment(payload);
  } finally {
    submitting = false;
  }
}
```

### Server Requirement

Client-side prevention is not enough.

Use idempotency key.

```js
app.post("/api/payments", async (req, res) => {
  const idempotencyKey = req.header("idempotency-key");
  const result = await paymentService.chargeOnce(idempotencyKey, req.body);
  res.json(result);
});
```

### Strong Interview Answer

> For payment double-submit, UI disabling helps UX but the server must enforce idempotency. I would use idempotency keys, store request outcome, and safely return the same result for retries.

---

## 62. Case 55: Idempotency Store Memory Leak

### Symptom

Payment service adds idempotency keys in memory and memory grows forever.

### Bad Code

```js
const processedPayments = new Map();
```

### Better Production Approach

- Store idempotency keys in durable storage.
- Add TTL based on business window.
- Use unique constraint.
- Store status and result.
- Clean expired keys.

### Strong Interview Answer

> Idempotency must survive process restarts and must be bounded. I would use durable storage with TTL and unique constraints, not an unbounded in-memory map.

---

## 63. Case 56: Partial Deploy Causes API Contract Mismatch

### Symptom

Frontend expects `totalPrice`, backend returns `priceTotal` for some users during rollout.

### Prevention

- Backward-compatible API changes.
- Add fields before removing old ones.
- Contract tests.
- Versioned APIs where necessary.
- Coordinated rollout.

### Strong Interview Answer

> Distributed releases are not atomic. I design API changes to be backward compatible, support old and new clients during rollout, and use contract tests to catch mismatches.

---

## 64. Case 57: Date Time Zone Bug In Production

### Symptom

Bookings appear one day off for users in certain regions.

### Bad Code

```js
const date = new Date("2026-06-20");
```

Interpretation can surprise depending on timezone and usage.

### Better Principles

- Store instants as UTC timestamps.
- Store date-only values as date-only strings with clear semantics.
- Convert for display at boundaries.
- Test multiple time zones.

### Strong Interview Answer

> Time bugs are data modeling bugs. I separate instants from date-only values, store UTC for instants, preserve local date semantics when needed, and test across time zones.

---

## 65. Case 58: Floating Point Money Bug

### Symptom

Totals occasionally show `10.299999999` or off-by-one cent.

### Bad Code

```js
const total = items.reduce((sum, item) => sum + item.price, 0);
```

### Better Pattern

```js
const totalCents = items.reduce((sum, item) => sum + item.priceCents, 0);
```

### Strong Interview Answer

> JavaScript numbers are floating point. For money I use integer minor units or a decimal library, never binary floating point for financial totals.

---

## 66. Case 59: BigInt Serialization Crash

### Symptom

API crashes when returning database IDs stored as BigInt.

### Bad Code

```js
res.json({ id: 123n });
```

`JSON.stringify` cannot serialize BigInt by default.

### Better Code

```js
res.json({ id: String(booking.id) });
```

### Strong Interview Answer

> BigInt is not JSON-serializable by default. I convert IDs to strings at API boundaries and document the contract.

---

## 67. Case 60: Prototype Pollution Causes Strange Behavior

### Symptom

Objects unexpectedly inherit values after processing user input.

### Bad Merge

```js
function merge(target, source) {
  for (const key in source) {
    target[key] = source[key];
  }

  return target;
}
```

### Safer Shape

```js
function assignSafe(target, source) {
  for (const [key, value] of Object.entries(source)) {
    if (key === "__proto__" || key === "constructor" || key === "prototype") {
      continue;
    }

    target[key] = value;
  }

  return target;
}
```

### Strong Interview Answer

> Prototype pollution is a security and reliability issue. I avoid unsafe deep merges of user input, block dangerous keys, validate schemas, and keep dependencies patched.

---

## 68. Case 61: Supply Chain Dependency Breaks Build

### Symptom

CI fails after a transitive dependency release. App code did not change.

### Investigation Path

1. Check lockfile changes.
2. Check package registry incident.
3. Check dependency version ranges.
4. Pin or override bad version.
5. Rebuild lockfile intentionally.
6. Add dependency update policy.

### Strong Interview Answer

> Dependencies are production risk. I use lockfiles, controlled updates, vulnerability scanning, and rollback/override strategy for bad transitive releases.

---

## 69. Case 62: Build Target Ships Too Much Polyfill

### Symptom

Bundle grows after changing browser support config.

### Investigation Path

- Compare transpilation output.
- Check browserslist.
- Check polyfills.
- Check modern vs legacy bundle strategy.
- Measure real user browser distribution.

### Strong Interview Answer

> Browser targets control shipped JavaScript. I would align targets with actual supported browsers and avoid forcing all modern users to download legacy code unnecessarily.

---

## 70. Case 63: Memory Leak From In-Flight Requests Map

### Symptom

Service tracks in-flight work for deduplication. Memory grows when dependency hangs.

### Bad Code

```js
const inFlight = new Map();

function loadOnce(key, loader) {
  if (inFlight.has(key)) return inFlight.get(key);

  const promise = loader();
  inFlight.set(key, promise);
  return promise;
}
```

### Better Code

```js
const inFlight = new Map();

function loadOnce(key, loader) {
  if (inFlight.has(key)) return inFlight.get(key);

  const promise = loader().finally(() => {
    inFlight.delete(key);
  });

  inFlight.set(key, promise);
  return promise;
}
```

Add timeout to avoid never-settling promises.

### Strong Interview Answer

> In-flight deduplication maps must delete entries on success, failure, and timeout. Otherwise hung promises retain memory and block fresh work.

---

## 71. Case 64: File Watcher Leak In Dev Tooling

### Symptom

Local dev server gets slower and eventually hits file descriptor limits.

### Cause

Watchers are created repeatedly without being closed.

### Fix Pattern

```js
let watcher;

function startWatching(path) {
  watcher?.close();
  watcher = fs.watch(path, handleChange);
}

function stopWatching() {
  watcher?.close();
  watcher = undefined;
}
```

### Strong Interview Answer

> Resource leaks are not only memory leaks. Timers, sockets, file watchers, and handles also need lifecycle ownership and cleanup.

---

## 72. Case 65: Graceful Shutdown Missing

### Symptom

Deploys cause failed requests because Node exits while requests are in flight.

### Better Pattern

```js
function shutdown(server) {
  server.close(error => {
    if (error) {
      process.exitCode = 1;
    }

    database.close().finally(() => {
      process.exit();
    });
  });
}

process.on("SIGTERM", () => shutdown(server));
```

### Production Additions

- Stop accepting new traffic.
- Let in-flight requests finish within timeout.
- Close DB/queue connections.
- Stop workers.
- Force exit after grace period.

### Strong Interview Answer

> Graceful shutdown protects users during deploys. I stop accepting new requests, drain in-flight work, close resources, and enforce a maximum shutdown deadline.

---

## 73. Case 66: Health Check Lies

### Symptom

Load balancer sends traffic to unhealthy pods because `/health` returns 200 even when DB is unavailable.

### Better Split

- Liveness: process is alive.
- Readiness: process can serve traffic.

```js
app.get("/live", (req, res) => {
  res.sendStatus(200);
});

app.get("/ready", async (req, res) => {
  const databaseReady = await database.ping();
  res.sendStatus(databaseReady ? 200 : 503);
});
```

### Strong Interview Answer

> Liveness and readiness should be separate. A process can be alive but not ready to serve traffic. Readiness should reflect critical dependencies and startup state.

---

## 74. Case 67: Alert Fatigue Hides Real Incident

### Symptom

Many noisy alerts fire daily. Team misses real latency regression.

### Better Alerts

- User-impact metrics.
- Burn-rate alerts.
- p95/p99 latency by critical route.
- Error rate by service.
- Saturation signals.
- Actionable thresholds.

### Strong Interview Answer

> Alerts should be actionable and tied to user impact. I would reduce noisy alerts, add SLO-based alerting, and route alerts to owners with runbooks.

---

## 75. Case 68: Dashboard Averages Hide Tail Latency

### Symptom

Average latency looks fine, but users complain.

### Root Cause

Averages hide p95/p99 pain.

### Strong Interview Answer

> I use percentiles and segmentation. Averages can hide tail latency, device-specific issues, route-specific regressions, or regional problems.

---

## 76. Case 69: Production-Only Data Shape Breaks Code

### Symptom

Code works in tests but fails with real customer data.

### Bad Assumption

```js
const primaryGuest = booking.guests[0].name;
```

### Better Code

```js
const primaryGuestName = booking.guests[0]?.name ?? "Unknown guest";
```

### Prevention

- Schema validation.
- Contract tests.
- Production-like fixtures.
- Defensive boundaries.
- Data quality dashboards.

### Strong Interview Answer

> Production data is messy. I validate boundaries, handle missing/optional values intentionally, and test with realistic fixtures instead of ideal examples only.

---

## 77. Case 70: Timeouts Too Aggressive

### Symptom

Requests fail even though dependency usually responds in 600 ms. Timeout is 300 ms.

### Senior Reasoning

Timeouts must balance:

- User experience.
- Dependency SLO.
- Retry behavior.
- End-to-end deadline.
- Critical vs optional dependency.

### Strong Interview Answer

> Timeouts should be derived from end-to-end latency budget and dependency behavior. Too long causes resource pileup; too short causes self-inflicted failures.

---

## 78. Case 71: Cache Stampede After Deploy

### Symptom

Cache is cleared during deploy. All pods recompute hot data at once. DB overloads.

### Fixes

- Warm cache gradually.
- Request coalescing.
- TTL jitter.
- Stale-while-revalidate.
- Avoid global cache purge for hot data.

### Strong Interview Answer

> Cache invalidation can create load spikes. I would use request coalescing, TTL jitter, stale safe reads, and gradual warming to avoid stampedes.

---

## 79. Case 72: Stale Cache Serves Wrong Price

### Symptom

Hotel prices are stale after update.

### Investigation Path

1. Check cache key.
2. Check TTL.
3. Check invalidation event.
4. Check whether user-specific fields are cached globally.
5. Check stale-while-revalidate policy.

### Strong Interview Answer

> Caching is a correctness trade-off. For prices, I would define freshness requirements clearly, invalidate on source updates, and avoid caching user-specific or highly volatile data incorrectly.

---

## 80. Case 73: Browser Error Spike After Dependency Update

### Symptom

Frontend error rate increases after updating a UI dependency.

### Investigation Path

- Segment errors by release.
- Use source maps.
- Check stack traces.
- Compare dependency changelog.
- Reproduce affected browser/device.
- Roll back if severe.

### Strong Interview Answer

> I would segment by release, use source maps to locate the stack, compare dependency changes, and roll back quickly if the error affects critical flows.

---

## 81. Case 74: Mobile Safari Bug Not Seen In Chrome

### Symptom

Feature works in Chrome but fails on iOS Safari.

### Investigation Path

- Check API compatibility.
- Check CSS support.
- Test on real device or simulator.
- Check polyfills.
- Check touch/input behavior.
- Check storage and cookie restrictions.

### Strong Interview Answer

> Browser-specific incidents need real browser evidence. I would verify API support, reproduce on target device, and use progressive enhancement or fallback rather than assuming Chrome behavior is universal.

---

## 82. Case 75: Accessibility Regression Becomes Production Bug

### Symptom

Keyboard users cannot complete checkout after modal change.

### Root Cause

- Focus not trapped in modal.
- Focus not restored.
- Button not reachable by keyboard.
- ARIA role missing or wrong.

### Strong Interview Answer

> Accessibility bugs are production bugs. I would test keyboard flow, focus management, screen reader semantics, and add automated accessibility checks plus manual tests for critical flows.

---

## 83. Case 76: Error Boundary Missing

### Symptom

One widget throws and the whole page becomes blank.

### Strong Answer

> I would isolate non-critical UI with error boundaries so one widget failure does not break the entire page. I would also log the error with release metadata and show a fallback.

---

## 84. Case 77: Backend Sends HTML Error To JSON Client

### Symptom

Frontend crashes with JSON parse error when backend returns 500 HTML page.

### Bad Code

```js
const data = await response.json();
```

### Better Code

```js
async function readJsonResponse(response) {
  const contentType = response.headers.get("content-type") ?? "";

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  if (!contentType.includes("application/json")) {
    throw new Error("Expected JSON response");
  }

  return response.json();
}
```

### Strong Interview Answer

> Clients should handle non-2xx and unexpected content types. I would check response status and headers before parsing JSON and show a controlled error state.

---

## 85. Case 78: Silent Catch Hides Production Failure

### Symptom

Feature silently does nothing. Logs show no errors.

### Bad Code

```js
try {
  await submitBooking(payload);
} catch {}
```

### Better Code

```js
try {
  await submitBooking(payload);
} catch (error) {
  logger.error({ error, bookingId: payload.bookingId }, "booking submit failed");
  showSubmitError();
}
```

### Strong Interview Answer

> Silent catches destroy debuggability. I would log useful context, avoid sensitive data, show a user-appropriate fallback, and let monitoring detect the failure.

---

## 86. Case 79: Error Message Leaks Sensitive Data

### Symptom

Frontend displays raw backend error containing internal details.

### Better Pattern

```js
app.use((error, req, res, next) => {
  logger.error({ error, requestId: req.id }, "request failed");

  res.status(500).json({
    code: "INTERNAL_ERROR",
    message: "Something went wrong",
    requestId: req.id
  });
});
```

### Strong Interview Answer

> Production errors should be useful to users and safe for security. I log internal detail server-side with request ID, but return sanitized errors to clients.

---

## 87. Case 80: Rate Limiter Blocks Legit Users

### Symptom

After adding rate limiting, users behind corporate NAT are blocked.

### Investigation Path

- Check rate-limit key.
- Check IP forwarding headers.
- Check authenticated user limits vs IP limits.
- Check endpoint-specific thresholds.
- Check allowlists and abuse patterns.

### Strong Interview Answer

> Rate limits need correct identity and thresholds. I would avoid relying only on IP when many users share NAT, and combine user, token, route, and abuse signals where appropriate.

---

## 88. Production Runbook: High CPU

Checklist:

1. Confirm CPU by version/pod.
2. Check traffic and endpoint distribution.
3. Check event-loop delay.
4. Capture CPU profile.
5. Identify hot stack.
6. Mitigate with rollback/flag/scale if active impact.
7. Fix algorithm, serialization, regex, logging, compression, or architecture.
8. Add load test or profiling guardrail.

---

## 89. Production Runbook: Memory Leak

Checklist:

1. Determine heap vs RSS vs external.
2. Check correlation with traffic/routes/jobs/uploads.
3. Capture snapshots safely.
4. Compare retained objects.
5. Inspect retainer paths.
6. Check Maps, arrays, caches, queues, listeners, timers, Buffers.
7. Add cleanup, TTL, capacity, streaming, or cancellation.
8. Add memory trend alert.

---

## 90. Production Runbook: Slow Browser Page

Checklist:

1. Check RUM by route/device/version.
2. Identify whether LCP, INP, CLS, or error rate regressed.
3. Use Network waterfall.
4. Use Performance trace.
5. Check bundle analyzer and Coverage.
6. Check images/fonts/third-party scripts.
7. Patch root cause.
8. Verify with lab and production data.

---

## 91. Production Runbook: API Storm

Checklist:

1. Identify caller version and route.
2. Check request rate by endpoint.
3. Check retries, polling, reconnects, duplicate effects.
4. Rate limit or disable bad client path.
5. Add backoff, jitter, cancellation, or debounce.
6. Add server protection.
7. Monitor recovery.

---

## 92. Production Runbook: Async Failure

Checklist:

1. Check unhandled rejection logs.
2. Check missing `await` or missing `return`.
3. Check `Promise.all` all-or-nothing behavior.
4. Check timeouts and aborts.
5. Check retry policy.
6. Check queue failure and DLQ.
7. Add failure-path tests.

---

## 93. Production Runbook: Bad Bundle

Checklist:

1. Compare bundle sizes by route before/after.
2. Inspect analyzer.
3. Check large dependencies and duplicates.
4. Check accidental imports/barrels.
5. Check build target and polyfills.
6. Code split rare heavy features.
7. Add CI budget.

---

## 94. Strong 30-Second Incident Answer

> I start by defining user impact and blast radius. Then I compare healthy and unhealthy metrics by version, route, endpoint, device, and region. I use logs, traces, profiles, heap snapshots, Network waterfalls, or Performance traces depending on the symptom. If users are actively impacted, I mitigate first with rollback, feature flag, scaling, rate limiting, or circuit breaker. Then I patch root cause, verify recovery, and add a guardrail so the issue does not return.

---

## 95. Strong 60-Second JavaScript Production Answer

> For JavaScript production debugging, I separate browser issues from Node/runtime issues. In the browser I check Web Vitals, Network waterfalls, Performance traces, Memory snapshots, bundle analyzer, and source-mapped errors. In Node I check latency percentiles, CPU, event-loop delay, heap/RSS/external memory, traces, dependency latency, DB pool wait, queues, and logs. I avoid guessing: I capture evidence, isolate the hot path or retained object, mitigate active impact, fix the root cause, and add observability, tests, budgets, or runbooks to prevent recurrence.

---

## 96. Common Interview Traps

| Trap | Better Senior Position |
|---|---|
| Start by rewriting code | Start with impact and evidence. |
| Use averages only | Use percentiles and segmentation. |
| Assume Node async means non-blocking | CPU still blocks the event loop. |
| Force GC for memory leak | Fix retained references. |
| Increase DB pool blindly | Measure pool wait and DB capacity. |
| Retry without limits | Use timeout, backoff, jitter, caps. |
| Cache everything | Bound cache and define freshness. |
| Ignore rollback | Mitigate active user impact first. |
| Debug browser only on desktop | Segment by device and browser. |
| Trust staging data | Test production-like volume and shapes. |

---

## 97. Debugging Communication Template

Use this during incident updates:

```text
Impact:
- What users or systems are affected?

Current status:
- Is the issue ongoing, mitigated, or resolved?

Evidence:
- What metric/log/trace/profile supports the current hypothesis?

Action:
- What are we doing now?

Next update:
- When will we update again?
```

---

## 98. Post-Incident Report Template

```md
Incident Report: <Title>

## Summary

## User Impact

## Timeline

## Detection

## Root Cause

## Mitigation

## Resolution

## What Went Well

## What Could Improve

## Follow-Up Actions

| Action | Owner | Due Date |
|---|---|---|
| | | |
```

---

## 99. Production Debugging Checklist

Before declaring resolved:

- Is user impact gone?
- Did metrics recover?
- Did error rate normalize?
- Did latency percentiles recover?
- Did memory/CPU stabilize?
- Did queue depth drain?
- Was rollback or flag state documented?
- Is root cause known?
- Is there a prevention action?
- Is monitoring updated?
- Are tests or budgets added where useful?

---

## 100. Rapid Revision

- Production debugging starts with impact, not code.
- Segment by version, route, endpoint, region, browser, device, customer, and feature flag.
- Mitigation and root cause are different tasks.
- High CPU plus event-loop delay means profile Node.
- High latency with low CPU often means dependency wait or pool saturation.
- Heap growth means retained JS objects.
- RSS growth with stable heap points to Buffers/native memory/streams/fragmentation.
- Browser INP problems usually need Performance traces.
- LCP needs LCP element and waterfall analysis.
- CLS needs layout shift attribution.
- API storms often come from retries, polling, reconnects, or frontend effects.
- Async bugs often come from missing `await`, race conditions, missing abort, or wrong `Promise.all` semantics.
- Bad bundles need analyzer, route-level chunks, and budgets.
- Caches need TTL, capacity, invalidation, and metrics.
- Queues need DLQ, retry caps, idempotency, and oldest-age metrics.
- Logs and metrics can create incidents if unbounded or high-cardinality.
- A good incident answer includes evidence, mitigation, fix, and prevention.

---

## 101. Final Mental Model

The best JavaScript production engineers do four things well:

1. They classify the failure correctly.
2. They pick the right evidence.
3. They reduce user impact quickly.
4. They turn the incident into a guardrail.

That is what separates debugging from guessing.
