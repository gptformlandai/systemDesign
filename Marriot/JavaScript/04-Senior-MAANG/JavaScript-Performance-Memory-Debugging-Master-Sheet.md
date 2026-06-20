# JavaScript Performance Memory Debugging Master Sheet

> Goal: become the engineer who can diagnose slow pages, leaking Node services, blocked event loops, oversized bundles, high memory, long tasks, bad Core Web Vitals, and production-only performance regressions using evidence instead of guesses.

---

## 1. How To Use This Sheet

Use this file after the core JavaScript, async, browser, Node, security, and testing sheets.

This is the senior layer. It teaches how JavaScript behaves under load, how browsers and Node spend time, how memory grows, how to prove root cause, and how to communicate fixes in interviews and production incidents.

Read in this order:

1. Mental model.
2. Metrics.
3. Browser performance debugging.
4. Node performance debugging.
5. Memory leaks.
6. Profiling tools.
7. Playbooks and case studies.
8. Strong interview answers.
9. Rapid revision.

---

## 2. The Performance Mental Model

Performance is not one thing.

It is the relationship between:

- CPU time.
- Memory pressure.
- Network latency.
- Disk or database wait time.
- Rendering work.
- Main-thread availability.
- Event-loop delay.
- Garbage collection.
- Bundle size.
- Cache behavior.
- Contention on shared resources.

The senior answer is never only "optimize the code".

The senior answer is:

1. Define the user-visible symptom.
2. Pick the right metric.
3. Capture evidence.
4. Isolate the hot path.
5. Fix the highest-impact bottleneck.
6. Verify before and after.
7. Add guardrails so it does not regress.

---

## 3. One-Line Definitions

| Term | Meaning |
|---|---|
| Latency | Time taken for one operation to complete. |
| Throughput | Number of operations completed per unit time. |
| Event-loop lag | Delay between when a callback should run and when it actually runs. |
| Long task | Browser main-thread task taking more than about 50 ms. |
| Memory leak | Memory that remains reachable even though the app no longer needs it. |
| Heap | Memory used for JavaScript objects. |
| RSS | Total memory held by a Node process from the OS perspective. |
| External memory | Native memory referenced by JS objects, often Buffers or native addons. |
| Flamegraph | Visualization of where CPU time is spent. |
| Heap snapshot | Point-in-time graph of allocated objects and retainers. |
| Retainer | Object path preventing another object from being garbage collected. |
| Layout thrashing | Repeated read/write layout operations causing forced reflow. |
| Hydration cost | Time spent attaching JS behavior to server-rendered HTML. |
| Bundle cost | Network, parse, compile, and execute cost of shipped JavaScript. |

---

## 4. Beginner To Senior Progression

### Beginner Level

A beginner says:

> The app is slow. We should use memoization.

This is incomplete because it jumps to a fix.

### Intermediate Level

An intermediate engineer says:

> The page is slow after clicking search. I will debounce the API call and avoid unnecessary re-renders.

Better, but still not enough if the root cause is layout, bundle, backend latency, or memory pressure.

### Senior Level

A senior engineer says:

> I will first define whether the issue is load performance, interaction latency, memory growth, or backend wait. Then I will collect traces, compare before and after, isolate the bottleneck, and only then optimize. For browser issues I would use Web Vitals, Network, Performance, Memory, and Coverage. For Node I would check event-loop delay, CPU profile, heap, RSS, GC, dependency latency, pool saturation, and logs/traces.

That is the MAANG-level pattern.

---

## 5. Golden Debugging Loop

Use this loop in interviews and production.

1. Reproduce or locate the symptom.
2. Identify affected users, endpoints, devices, browsers, versions, and traffic shape.
3. Measure the right metric.
4. Compare healthy vs unhealthy traces.
5. Locate whether the bottleneck is CPU, memory, network, rendering, I/O, database, cache, dependency, or bundle.
6. Make one focused change.
7. Verify with the same metric.
8. Add tests, budgets, alerts, dashboards, or automated profiling guardrails.

Never debug only by reading code unless the issue is obvious and low risk.

---

## 6. Browser Performance Metrics

### Core Web Vitals

| Metric | Measures | Good Target | Common Root Causes |
|---|---|---:|---|
| LCP | Largest visible content load | <= 2.5 s | slow server, render-blocking assets, huge image, slow font, client rendering delay |
| INP | Interaction responsiveness | <= 200 ms | long JS tasks, heavy re-render, sync work, layout thrashing |
| CLS | Visual stability | <= 0.1 | missing dimensions, ads, late fonts, injected banners |

### Other Important Metrics

| Metric | Meaning |
|---|---|
| TTFB | Time to first byte from server. |
| FCP | First Contentful Paint. |
| TTI | Time to Interactive, less emphasized now but still useful. |
| TBT | Total Blocking Time, lab proxy for interaction blocking. |
| Long tasks | Tasks over about 50 ms on main thread. |
| JS parse/compile time | Browser cost before code even runs. |
| Resource load time | Network cost of assets. |

---

## 7. Node Performance Metrics

| Metric | Meaning | Why It Matters |
|---|---|---|
| p50/p95/p99 latency | Response time distribution | Tail latency reveals production pain. |
| Event-loop delay | Main thread scheduling delay | Detects CPU blocking and sync APIs. |
| CPU usage | Process CPU consumption | Reveals hot loops, JSON work, crypto, compression. |
| Heap used | JS object memory | Detects object growth and leaks. |
| RSS | Total process memory | Reveals Buffers, native memory, fragmentation. |
| External memory | Native memory linked to JS | Important for Buffer-heavy services. |
| GC pause time | Time spent collecting memory | High pause time hurts latency. |
| Active handles | Open timers, sockets, servers | Reveals leaks and shutdown issues. |
| DB pool usage | Connection pressure | Detects waiting and saturation. |
| Queue depth | Backlog of async jobs | Detects overload or consumer failure. |
| Error rate | Failed requests | Performance and correctness often regress together. |

---

## 8. Browser Rendering Pipeline

A browser roughly does:

1. Parse HTML.
2. Build DOM.
3. Parse CSS.
4. Build CSSOM.
5. Combine into render tree.
6. Layout.
7. Paint.
8. Composite.
9. Run JavaScript on the main thread when scheduled.

JavaScript can hurt performance by:

- Blocking parsing.
- Blocking rendering.
- Creating long tasks.
- Forcing layout repeatedly.
- Creating too many DOM nodes.
- Triggering expensive style recalculation.
- Shipping too much code.
- Hydrating too much UI at once.

---

## 9. Node Runtime Pipeline

Node performance is shaped by:

- V8 executing JavaScript.
- One main event loop thread.
- libuv worker pool for selected operations.
- OS networking.
- Native addons.
- Garbage collection.
- External systems like DB, Redis, queues, APIs.

Node is strong for I/O concurrency.

Node is vulnerable when CPU-heavy work blocks the main thread.

Common blockers:

- Large JSON parse/stringify.
- Synchronous file APIs.
- CPU-heavy loops.
- Regex catastrophes.
- Compression on main thread.
- Crypto on main thread.
- Huge object transformations.
- Logging too much synchronously.

---

## 10. Performance Is A Product Behavior

A performance issue should be described as user impact.

Weak:

> The bundle is big.

Strong:

> Checkout users on mid-range Android devices have p75 INP around 450 ms after the payment step. The trace shows a 270 ms main-thread task caused by validation and state normalization. We can split validation, defer non-critical analytics, and move heavy work off the critical interaction path.

Production debugging starts with impact.

---

## 11. Browser DevTools Performance Panel

Use the Performance panel when:

- A click feels slow.
- Scrolling janks.
- Input is delayed.
- Animation stutters.
- Page load spends too much time in JS.
- Hydration is expensive.

### What To Capture

1. Start recording.
2. Reproduce the interaction.
3. Stop recording.
4. Inspect Main thread.
5. Locate long tasks.
6. Expand call stacks.
7. Look at scripting, rendering, painting, idle time.
8. Check screenshots/filmstrip for user-visible delay.

### What To Say In Interview

> I would take a Performance trace around the slow interaction, identify whether time is spent in scripting, rendering, painting, or network, then inspect long tasks and call stacks. I would optimize the largest verified cost first and re-run the same trace.

---

## 12. Browser Memory Panel

Use the Memory panel when:

- SPA gets slower over time.
- Navigation between routes increases memory.
- Detached DOM nodes grow.
- Event listeners remain after unmount.
- Heap grows after forced GC.
- Long sessions crash mobile browsers.

### Heap Snapshot Workflow

1. Open Memory panel.
2. Take baseline heap snapshot.
3. Perform suspected action multiple times.
4. Force GC if available.
5. Take another snapshot.
6. Compare snapshots.
7. Sort by retained size.
8. Inspect retainers.
9. Find the path keeping objects alive.
10. Fix cleanup or ownership.

---

## 13. Node Profiling Toolbox

Use these tools depending on symptom.

| Symptom | Tool |
|---|---|
| High CPU | CPU profile, flamegraph, `--prof`, clinic flame, inspector profiler |
| Event-loop lag | `perf_hooks.monitorEventLoopDelay`, APM, custom lag metric |
| Heap growth | heap snapshots, allocation sampling, `--inspect`, heapdump in controlled environments |
| RSS growth | process memory metrics, Buffer tracking, native addon review |
| Slow dependency | tracing, logs, metrics, DB slow query logs |
| Queue backlog | queue depth, consumer lag, worker health |
| GC pressure | GC logs, heap profile, allocation rate |

---

## 14. Quick Triage Matrix

| Observation | Likely Area | First Evidence |
|---|---|---|
| p99 latency high, CPU high | CPU-bound JS | CPU profile, event-loop delay |
| p99 latency high, CPU low | I/O wait | traces, dependency latency, pool metrics |
| memory grows per request | leak | heap diff, retained objects |
| RSS grows but heap stable | Buffer/native memory | external memory, Buffer usage |
| page input delayed | main-thread blocking | Performance trace, INP attribution |
| page load slow on 4G | network/bundle/server | Lighthouse, Network, RUM |
| CLS high | layout instability | Web Vitals attribution, screenshots |
| CPU spikes during scroll | rendering/layout | DevTools frames, layout events |
| app slows after route changes | browser leak | heap snapshots, detached nodes |

---

## 15. Memory Fundamentals

JavaScript memory has two key ideas:

- Allocation: creating objects, closures, arrays, strings, maps, DOM nodes, buffers.
- Reachability: whether something can still be reached from roots.

Garbage collection frees unreachable objects.

A leak happens when an object is still reachable by mistake.

Common roots:

- Global variables.
- Module-level caches.
- Active timers.
- Event listeners.
- Closures.
- DOM references.
- Pending promises.
- Sockets.
- Request maps.
- In-memory queues.

---

## 16. Heap vs RSS vs External Memory

### Heap

The V8 heap stores JavaScript objects.

Examples:

- Plain objects.
- Arrays.
- Closures.
- Maps and Sets.
- Strings.

### RSS

RSS is total resident memory held by the process.

It includes:

- V8 heap.
- C++ objects.
- Buffers.
- Stacks.
- Native library allocations.
- Memory fragmentation.

### External Memory

External memory is memory associated with JS objects but allocated outside the V8 heap.

Examples:

- `Buffer` data.
- Native addon memory.
- Some binary payloads.

Senior trap:

> Heap can look stable while RSS grows. That often points to Buffers, native memory, fragmentation, or external allocations.

---

## 17. Garbage Collection Mental Model

V8 uses generational garbage collection.

Simplified:

- New objects are allocated in young generation.
- Short-lived objects are collected quickly.
- Objects that survive may move to old generation.
- Old generation collection is more expensive.
- High allocation rates create GC pressure.

Performance problem examples:

- Creating many temporary arrays per request.
- Rebuilding large objects on every render.
- Parsing huge JSON repeatedly.
- Keeping large caches without limits.
- Creating closures inside tight loops unnecessarily.

---

## 18. Memory Leak Pattern: Global Cache Without Bound

Bad:

```js
const cache = new Map();

export function getUserProfile(userId, loader) {
  if (cache.has(userId)) return cache.get(userId);

  const profile = loader(userId);
  cache.set(userId, profile);
  return profile;
}
```

Problem:

- Cache grows forever.
- User IDs are unbounded.
- Old data remains reachable from module scope.

Better:

```js
class LruCache {
  constructor(limit) {
    this.limit = limit;
    this.values = new Map();
  }

  get(key) {
    if (!this.values.has(key)) return undefined;

    const value = this.values.get(key);
    this.values.delete(key);
    this.values.set(key, value);
    return value;
  }

  set(key, value) {
    if (this.values.has(key)) {
      this.values.delete(key);
    }

    this.values.set(key, value);

    if (this.values.size > this.limit) {
      const oldestKey = this.values.keys().next().value;
      this.values.delete(oldestKey);
    }
  }
}

const userCache = new LruCache(10_000);
```

Senior answer:

> Caches need capacity, TTL, invalidation, and observability. An unbounded cache is a memory leak with better marketing.

---

## 19. Memory Leak Pattern: Event Listener Cleanup

Bad browser code:

```js
function mountSearchBox(input, onSearch) {
  input.addEventListener("input", event => {
    onSearch(event.target.value);
  });
}
```

Problem:

- Anonymous listener cannot be removed later.
- If `input` or callback captures component state, old state may stay alive.

Better:

```js
function mountSearchBox(input, onSearch) {
  function handleInput(event) {
    onSearch(event.target.value);
  }

  input.addEventListener("input", handleInput);

  return function cleanup() {
    input.removeEventListener("input", handleInput);
  };
}
```

React shape:

```jsx
useEffect(() => {
  function handleResize() {
    setWidth(window.innerWidth);
  }

  window.addEventListener("resize", handleResize);
  return () => window.removeEventListener("resize", handleResize);
}, []);
```

---

## 20. Memory Leak Pattern: Timer Cleanup

Bad:

```js
function startPolling(fetchStatus) {
  setInterval(fetchStatus, 5_000);
}
```

Better:

```js
function startPolling(fetchStatus) {
  const intervalId = setInterval(fetchStatus, 5_000);

  return function stopPolling() {
    clearInterval(intervalId);
  };
}
```

Production caution:

- Timers keep callbacks reachable.
- Callbacks may keep large objects reachable.
- Intervals can continue after route changes, tab state changes, or request cancellation.

---

## 21. Memory Leak Pattern: Pending Promise Registry

Bad:

```js
const pendingRequests = new Map();

function trackRequest(requestId, promise) {
  pendingRequests.set(requestId, promise);
  return promise;
}
```

Better:

```js
const pendingRequests = new Map();

function trackRequest(requestId, promise) {
  pendingRequests.set(requestId, promise);

  return promise.finally(() => {
    pendingRequests.delete(requestId);
  });
}
```

Production caution:

- Always delete on success, failure, and cancellation.
- Add timeouts for requests that may never settle.

---

## 22. Memory Leak Pattern: Detached DOM Nodes

Detached DOM nodes happen when a node is removed from the document but still referenced by JavaScript.

Example:

```js
const removedRows = [];

function removeRow(rowElement) {
  rowElement.remove();
  removedRows.push(rowElement);
}
```

Problem:

- The DOM node is gone visually.
- JavaScript still holds it.
- Child nodes, listeners, and data remain retained.

Better:

```js
function removeRow(rowElement) {
  rowElement.replaceChildren();
  rowElement.remove();
}
```

In real apps, prefer storing IDs or serializable data instead of DOM node references.

---

## 23. Memory Leak Pattern: Closures Capturing Large Objects

Bad:

```js
function createHandler(bigReport) {
  return function handleClick() {
    console.log(bigReport.summary.id);
  };
}
```

If the handler lives long, the whole `bigReport` may be retained.

Better:

```js
function createHandler(bigReport) {
  const reportId = bigReport.summary.id;

  return function handleClick() {
    console.log(reportId);
  };
}
```

Senior answer:

> Closures are not leaks by themselves. They become leaks when a long-lived function retains more state than it actually needs.

---

## 24. Memory Leak Pattern: Request-Scoped Data In Module Scope

Bad Node code:

```js
const recentRequests = [];

export function requestLogger(req, res, next) {
  recentRequests.push({
    headers: req.headers,
    body: req.body,
    time: Date.now()
  });

  next();
}
```

Better:

```js
const recentRequests = [];
const MAX_RECENT_REQUESTS = 100;

export function requestLogger(req, res, next) {
  recentRequests.push({
    method: req.method,
    path: req.path,
    time: Date.now()
  });

  while (recentRequests.length > MAX_RECENT_REQUESTS) {
    recentRequests.shift();
  }

  next();
}
```

Even better:

- Send structured logs to a log system.
- Do not retain request bodies in memory.
- Redact PII.

---

## 25. Event-Loop Delay

Event-loop delay measures how late Node is in executing scheduled callbacks.

If the event loop is blocked, Node cannot:

- Accept new callbacks promptly.
- Respond to completed I/O.
- Process timers on time.
- Send responses quickly.
- Handle health checks reliably.

Example monitor:

```js
import { monitorEventLoopDelay } from "node:perf_hooks";

const histogram = monitorEventLoopDelay({ resolution: 20 });
histogram.enable();

setInterval(() => {
  const p50 = histogram.percentile(50) / 1_000_000;
  const p95 = histogram.percentile(95) / 1_000_000;
  const p99 = histogram.percentile(99) / 1_000_000;

  console.log({
    eventLoopDelayMs: {
      p50: Number(p50.toFixed(2)),
      p95: Number(p95.toFixed(2)),
      p99: Number(p99.toFixed(2))
    }
  });

  histogram.reset();
}, 10_000);
```

Interview answer:

> Event-loop delay tells me whether JavaScript execution is preventing Node from scheduling callbacks. If delay rises with CPU, I suspect CPU-bound code. If delay is low but latency is high, I look at dependencies, pools, queues, or network waits.

---

## 26. Event-Loop Utilization

Node exposes event-loop utilization through `performance.eventLoopUtilization()`.

```js
import { performance } from "node:perf_hooks";

let previous = performance.eventLoopUtilization();

setInterval(() => {
  const current = performance.eventLoopUtilization(previous);
  previous = performance.eventLoopUtilization();

  console.log({
    eventLoopUtilization: Number(current.utilization.toFixed(3)),
    activeMs: Number(current.active.toFixed(2)),
    idleMs: Number(current.idle.toFixed(2))
  });
}, 10_000);
```

Use it with:

- CPU usage.
- Latency percentiles.
- request rate.
- dependency latency.

No single metric is enough.

---

## 27. CPU Profiling Node

When CPU is high:

1. Capture CPU profile during the spike.
2. Keep the window short and representative.
3. Inspect top functions by self time and total time.
4. Look for application code, JSON, regex, serialization, validation, templating, compression, crypto, logging.
5. Verify fix with another profile.

Example local command:

```bash
node --inspect server.js
```

Then open Chrome DevTools for Node and record CPU profile.

Alternative:

```bash
node --cpu-prof server.js
```

This writes a CPU profile file that can be inspected with supported tooling.

---

## 28. CPU Hot Path Example

Bad:

```js
function countByStatus(bookings) {
  return bookings.reduce((result, booking) => {
    const existing = result.find(item => item.status === booking.status);

    if (existing) {
      existing.count += 1;
    } else {
      result.push({ status: booking.status, count: 1 });
    }

    return result;
  }, []);
}
```

Problem:

- `find` inside `reduce` can become O(n * k).
- With many statuses or many items, this becomes expensive.

Better:

```js
function countByStatus(bookings) {
  const counts = new Map();

  for (const booking of bookings) {
    counts.set(booking.status, (counts.get(booking.status) ?? 0) + 1);
  }

  return Array.from(counts, ([status, count]) => ({ status, count }));
}
```

Performance lesson:

- Algorithmic improvements beat micro-optimizations.
- Measure on realistic input sizes.

---

## 29. Browser Long Tasks

A long task blocks the browser main thread long enough to delay interaction.

Common causes:

- Large JS bundle executing on load.
- Heavy client-side rendering.
- Complex state updates.
- Expensive sorting/filtering.
- Massive DOM operations.
- Synchronous validation.
- JSON parse/stringify.
- Third-party scripts.

Long task observer:

```js
const observer = new PerformanceObserver(list => {
  for (const entry of list.getEntries()) {
    console.log({
      name: entry.name,
      startTime: Math.round(entry.startTime),
      duration: Math.round(entry.duration)
    });
  }
});

observer.observe({ type: "longtask", buffered: true });
```

Production caution:

- Long task attribution may be limited.
- Use it as a signal, then inspect traces.

---

## 30. Measuring Browser Interactions

Use User Timing marks around important flows.

```js
async function submitCheckout(order) {
  performance.mark("checkout-submit-start");

  const response = await fetch("/api/checkout", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(order)
  });

  performance.mark("checkout-submit-response");

  const result = await response.json();

  performance.mark("checkout-submit-render-start");
  renderConfirmation(result);
  performance.mark("checkout-submit-end");

  performance.measure(
    "checkout-submit-network",
    "checkout-submit-start",
    "checkout-submit-response"
  );

  performance.measure(
    "checkout-submit-total",
    "checkout-submit-start",
    "checkout-submit-end"
  );
}
```

Use custom measurements with RUM to connect traces to real users.

---

## 31. Measuring Web Vitals

Production Web Vitals should be measured from real users when possible.

Important dimensions:

- Route.
- Device class.
- Browser.
- Network type.
- Geography.
- App version.
- Experiment flag.
- Authenticated vs guest.

Averages hide pain. Use percentiles.

Strong answer:

> I would track p75 Core Web Vitals by route and device class, because a desktop lab score can hide poor mobile production performance.

---

## 32. Network Waterfall Debugging

Use the Network panel for:

- Slow TTFB.
- Render-blocking assets.
- Too many requests.
- Large JS/CSS/image payloads.
- Missing compression.
- Missing caching.
- Duplicate API calls.
- Bad priority.
- Late preloads.

Checklist:

- Is HTML delayed by server time?
- Are critical resources discovered late?
- Are scripts blocking rendering?
- Are images properly sized?
- Is compression enabled?
- Are cache headers correct?
- Are API calls duplicated?
- Is third-party code delaying main content?

---

## 33. Bundle Cost Mental Model

JavaScript bundle cost includes:

1. Download.
2. Decompression.
3. Parse.
4. Compile.
5. Execute.
6. Hydrate or render.
7. Memory retained by modules.

A 500 KB JavaScript file is not just network cost.

It also consumes CPU and memory.

Especially on:

- Low-end phones.
- Battery-saving mode.
- Older browsers.
- Slow networks.
- Pages with many third-party scripts.

---

## 34. Bundle Debugging Checklist

Look for:

- Large dependencies.
- Duplicate dependency versions.
- Poor tree shaking.
- Barrel exports pulling too much code.
- Moment-like large date libraries where smaller alternatives fit.
- Shipping admin-only code to normal users.
- Shipping all locales.
- Eager loading rare routes.
- Heavy chart/editor/map libraries in initial path.
- Transpilation output too broad for target browsers.

Typical fixes:

- Route-level code splitting.
- Component-level dynamic import for rare heavy UI.
- Dependency replacement.
- Tree-shaking-friendly imports.
- Bundle analyzer budgets.
- Modern build targets.
- Server rendering or partial hydration where appropriate.

---

## 35. Dynamic Import Example

Before:

```js
import HeavyChart from "./HeavyChart.js";

export function Dashboard({ report }) {
  return HeavyChart.render(report);
}
```

After:

```js
export async function renderDashboard(report) {
  const { HeavyChart } = await import("./HeavyChart.js");
  return HeavyChart.render(report);
}
```

Production caution:

- Dynamic imports improve initial load only when the loaded code is not needed immediately.
- Do not split so aggressively that users wait on many small chunks.

---

## 36. Tree Shaking Traps

Tree shaking works best with:

- ES modules.
- Static imports.
- Side-effect-free modules.
- Bundler configuration that understands package metadata.

Trap:

```js
import * as dateUtils from "large-date-library";
```

Better when supported:

```js
import { formatDate } from "large-date-library/format";
```

Another trap:

```js
export * from "./admin-tools.js";
export * from "./public-tools.js";
```

Barrel files can accidentally pull more code if side effects or bundler limitations exist.

---

## 37. Runtime Cost Beats Bundle Size Sometimes

A smaller bundle can still be slower if it does expensive work.

Example:

```js
const normalized = hugeItems
  .map(expensiveNormalize)
  .filter(isVisible)
  .sort(compareByScore);
```

If this runs on every keystroke, bundle size is not the primary issue.

Senior framing:

> I separate load cost from runtime interaction cost. Bundle analysis helps load performance, while traces and profiles reveal runtime CPU and rendering cost.

---

## 38. Layout Thrashing

Layout thrashing happens when code repeatedly writes to the DOM and then reads layout.

Bad:

```js
for (const item of items) {
  item.element.style.width = `${container.offsetWidth / 3}px`;
  item.element.style.height = `${item.element.offsetWidth}px`;
}
```

Better:

```js
const containerWidth = container.offsetWidth;
const itemWidth = containerWidth / 3;

for (const item of items) {
  item.element.style.width = `${itemWidth}px`;
  item.element.style.height = `${itemWidth}px`;
}
```

Best pattern:

- Batch layout reads.
- Batch DOM writes.
- Avoid measuring after every write.
- Use CSS layout when possible.

---

## 39. DOM Size And Rendering Cost

Large DOM trees hurt:

- Style calculation.
- Layout.
- Paint.
- Memory.
- Event handling.
- Accessibility tree generation.

Solutions:

- Pagination.
- Virtualization.
- Progressive rendering.
- Avoid hidden huge trees.
- Remove unused nodes.
- Prefer CSS over JS-driven layout.

Virtual list shape:

```js
function getVisibleRange({ scrollTop, rowHeight, viewportHeight, total }) {
  const start = Math.max(0, Math.floor(scrollTop / rowHeight) - 5);
  const visibleCount = Math.ceil(viewportHeight / rowHeight) + 10;
  const end = Math.min(total, start + visibleCount);

  return { start, end };
}
```

---

## 40. React Rendering Performance

Even if this sheet is JavaScript-wide, React is common in interviews.

Common issues:

- Parent state causes large subtree re-render.
- Unstable callback props.
- Unstable object/array props.
- Derived data recalculated every render.
- Context updates too broad.
- List keys unstable.
- Expensive components not split.

Use:

- React DevTools Profiler.
- Browser Performance trace.
- Memoization only around verified hot paths.
- State colocation.
- Context splitting.
- List virtualization.

---

## 41. React Memoization Trap

Bad:

```jsx
const filters = { status, region };

return <BookingList filters={filters} />;
```

Even if `BookingList` is memoized, a new object is passed every render.

Better:

```jsx
const filters = useMemo(() => ({ status, region }), [status, region]);

return <BookingList filters={filters} />;
```

But do not memoize everything.

Strong answer:

> I use memoization after profiling shows wasted render cost. I also check whether state is placed too high, because memoization may hide a design issue.

---

## 42. Debounce And Throttle

Debounce: wait until activity stops.

Good for:

- Search input.
- Autosave after typing.
- Resize after user stops resizing.

Throttle: run at most once per interval.

Good for:

- Scroll events.
- Drag events.
- Continuous telemetry.

Debounce example:

```js
function debounce(fn, delayMs) {
  let timeoutId;

  return function debounced(...args) {
    clearTimeout(timeoutId);

    timeoutId = setTimeout(() => {
      fn.apply(this, args);
    }, delayMs);
  };
}
```

Throttle example:

```js
function throttle(fn, intervalMs) {
  let lastRun = 0;
  let trailingTimeoutId;

  return function throttled(...args) {
    const now = Date.now();
    const remaining = intervalMs - (now - lastRun);

    if (remaining <= 0) {
      clearTimeout(trailingTimeoutId);
      trailingTimeoutId = undefined;
      lastRun = now;
      fn.apply(this, args);
      return;
    }

    if (!trailingTimeoutId) {
      trailingTimeoutId = setTimeout(() => {
        lastRun = Date.now();
        trailingTimeoutId = undefined;
        fn.apply(this, args);
      }, remaining);
    }
  };
}
```

---

## 43. Scheduling Browser Work

Not all work belongs on the critical path.

Options:

- `requestAnimationFrame` for visual updates before paint.
- `requestIdleCallback` for non-critical background work where supported.
- `setTimeout` to break up tasks.
- Web Workers for CPU-heavy work.
- Streaming APIs for incremental processing.

Chunking example:

```js
function processInChunks(items, processItem, chunkSize = 100) {
  let index = 0;

  return new Promise(resolve => {
    function runChunk() {
      const end = Math.min(index + chunkSize, items.length);

      while (index < end) {
        processItem(items[index]);
        index += 1;
      }

      if (index < items.length) {
        setTimeout(runChunk, 0);
      } else {
        resolve();
      }
    }

    runChunk();
  });
}
```

Caution:

- Chunking improves responsiveness but may increase total completion time.
- Use progress states for user-visible long work.

---

## 44. Web Workers

Use Web Workers for CPU-heavy browser work that does not need direct DOM access.

Good candidates:

- Large data transforms.
- CSV parsing.
- Image processing.
- Search indexing.
- Compression.
- Expensive validation.

Worker example:

```js
const worker = new Worker(new URL("./report-worker.js", import.meta.url), {
  type: "module"
});

worker.postMessage({ type: "BUILD_REPORT", rows });

worker.addEventListener("message", event => {
  if (event.data.type === "REPORT_READY") {
    renderReport(event.data.report);
  }
});
```

Worker file:

```js
self.addEventListener("message", event => {
  if (event.data.type !== "BUILD_REPORT") return;

  const report = buildReport(event.data.rows);
  self.postMessage({ type: "REPORT_READY", report });
});
```

Production caution:

- Data copied to/from workers has cost.
- Transferables help for large binary data.
- Workers do not fix slow algorithms automatically.

---

## 45. Node Worker Threads

Use worker threads for CPU-heavy work in Node.

Main thread:

```js
import { Worker } from "node:worker_threads";

export function runCpuJob(payload) {
  return new Promise((resolve, reject) => {
    const worker = new Worker(new URL("./cpu-worker.js", import.meta.url), {
      workerData: payload
    });

    worker.once("message", resolve);
    worker.once("error", reject);
    worker.once("exit", code => {
      if (code !== 0) {
        reject(new Error(`Worker stopped with exit code ${code}`));
      }
    });
  });
}
```

Worker:

```js
import { parentPort, workerData } from "node:worker_threads";

const result = buildExpensiveReport(workerData);
parentPort.postMessage(result);
```

Production caution:

- Use a worker pool for frequent jobs.
- Creating a worker per request can be expensive.
- Add timeouts and cancellation strategy.

---

## 46. Streams And Backpressure

Streams prevent loading entire payloads into memory.

Bad:

```js
const file = await fs.promises.readFile("large-export.csv");
res.send(file);
```

Better:

```js
import { createReadStream } from "node:fs";
import { pipeline } from "node:stream/promises";

export async function downloadExport(req, res) {
  res.setHeader("content-type", "text/csv");
  await pipeline(createReadStream("large-export.csv"), res);
}
```

Backpressure means the writer respects the reader's speed.

Without backpressure:

- Memory grows.
- GC pressure rises.
- Latency increases.
- Process can crash.

---

## 47. JSON Performance

Large JSON can hurt both browser and Node.

Costs:

- Network transfer.
- Parse time.
- Stringify time.
- Memory overhead.
- Blocking main thread/event loop.

Bad server shape:

```js
app.get("/api/bookings", async (req, res) => {
  const bookings = await db.booking.findMany();
  res.json(bookings);
});
```

Better:

```js
app.get("/api/bookings", async (req, res) => {
  const pageSize = Math.min(Number(req.query.pageSize ?? 50), 100);
  const cursor = req.query.cursor;

  const page = await bookingRepository.findPage({ pageSize, cursor });
  res.json(page);
});
```

Senior answer:

> I would avoid returning unbounded JSON. Pagination, field selection, streaming, compression, and caching are usually better than trying to stringify a huge object faster.

---

## 48. Regex Performance

Some regular expressions can cause catastrophic backtracking.

Risky:

```js
const pattern = /^(a+)+$/;
pattern.test("aaaaaaaaaaaaaaaaaaaaaaaaaaaa!");
```

Better:

- Simplify regex.
- Avoid nested quantifiers.
- Use length limits.
- Use safe-regex tooling in CI.
- Prefer parsers for complex grammars.

Production symptom:

- CPU spikes.
- Event-loop delay rises.
- One crafted input can degrade service.

This is both performance and security.

---

## 49. Synchronous API Traps In Node

Avoid sync APIs on request paths.

Bad:

```js
import { readFileSync } from "node:fs";

app.get("/config", (req, res) => {
  const config = JSON.parse(readFileSync("./config.json", "utf8"));
  res.json(config);
});
```

Better:

```js
import { readFile } from "node:fs/promises";

let cachedConfig;

async function getConfig() {
  if (!cachedConfig) {
    cachedConfig = JSON.parse(await readFile("./config.json", "utf8"));
  }

  return cachedConfig;
}

app.get("/config", async (req, res, next) => {
  try {
    res.json(await getConfig());
  } catch (error) {
    next(error);
  }
});
```

---

## 50. Logging Performance

Logging can become a bottleneck.

Problems:

- Logging entire request/response bodies.
- Synchronous transports.
- High cardinality labels.
- Logging inside tight loops.
- Excessive error stack creation.
- PII leakage.

Better pattern:

```js
logger.info({
  requestId: req.id,
  method: req.method,
  path: req.path,
  statusCode: res.statusCode,
  durationMs
}, "request completed");
```

Strong answer:

> I log enough structured data to debug production, but I avoid logging large payloads and high-cardinality fields that hurt cost, performance, and privacy.

---

## 51. Database Wait vs JavaScript CPU

High endpoint latency is often not JavaScript CPU.

Differentiate:

| Signal | CPU-Bound JS | DB/Dependency Wait |
|---|---|---|
| CPU usage | high | low/medium |
| event-loop delay | high | low |
| dependency span | normal | high |
| DB pool wait | normal | high |
| profile hot path | app functions | mostly idle/waiting |

Use distributed tracing to see where time goes.

---

## 52. Connection Pool Saturation

Symptoms:

- Latency rises under load.
- CPU may be normal.
- DB queries wait before execution.
- Timeouts increase.
- Adding app instances may worsen DB pressure.

Fixes:

- Measure pool wait time.
- Tune pool size based on DB capacity.
- Reduce query count.
- Add indexes.
- Cache safe reads.
- Avoid long transactions.
- Add backpressure and rate limits.

Senior answer:

> A bigger pool is not automatically better. It can increase database contention. I would measure pool wait and DB saturation before tuning.

---

## 53. N Plus One Performance Bug

Bad:

```js
app.get("/api/hotels", async (req, res) => {
  const hotels = await db.hotel.findMany();

  const result = [];
  for (const hotel of hotels) {
    const rooms = await db.room.findMany({ where: { hotelId: hotel.id } });
    result.push({ ...hotel, rooms });
  }

  res.json(result);
});
```

Better:

```js
app.get("/api/hotels", async (req, res) => {
  const hotels = await db.hotel.findManyWithRooms();
  res.json(hotels);
});
```

If manual batching is needed:

```js
const hotels = await db.hotel.findMany();
const hotelIds = hotels.map(hotel => hotel.id);
const rooms = await db.room.findManyByHotelIds(hotelIds);

const roomsByHotelId = new Map();
for (const room of rooms) {
  const group = roomsByHotelId.get(room.hotelId) ?? [];
  group.push(room);
  roomsByHotelId.set(room.hotelId, group);
}

const result = hotels.map(hotel => ({
  ...hotel,
  rooms: roomsByHotelId.get(hotel.id) ?? []
}));
```

---

## 54. API Payload Performance

Optimize payloads by:

- Returning only required fields.
- Paginating lists.
- Compressing responses.
- Using ETags or cache validators.
- Avoiding repeated nested data.
- Moving expensive exports to async jobs.
- Streaming large downloads.
- Using GraphQL/DataLoader carefully if appropriate.

Avoid:

- Returning everything because frontend might need it.
- Deeply nested unbounded responses.
- Client-side filtering of huge datasets that the server can filter.

---

## 55. Caching Performance

Caching improves latency but adds correctness risks.

Cache what is:

- Read-heavy.
- Expensive to compute.
- Safe to reuse.
- Has clear invalidation.
- Has acceptable staleness.

Cache dangers:

- Stale data.
- Memory leaks.
- Cache stampede.
- Thundering herd.
- Leaking user-specific data.
- Unbounded cardinality.

Request coalescing example:

```js
const inFlight = new Map();

async function getHotelRate(hotelId, loadRate) {
  if (inFlight.has(hotelId)) {
    return inFlight.get(hotelId);
  }

  const promise = loadRate(hotelId).finally(() => {
    inFlight.delete(hotelId);
  });

  inFlight.set(hotelId, promise);
  return promise;
}
```

---

## 56. Cache Stampede Protection

When a hot cache key expires, many requests may hit the backend at once.

Protection patterns:

- Request coalescing.
- Stale-while-revalidate.
- Soft TTL plus hard TTL.
- Randomized TTL jitter.
- Background refresh.
- Rate limiting expensive reloads.

Interview answer:

> I would avoid synchronized expiry for hot keys. I would use TTL jitter and request coalescing so one request refreshes while others reuse stale-but-safe data when allowed.

---

## 57. Web Vitals Root-Cause Playbooks

### High LCP

Check:

- Slow TTFB.
- Largest image too large.
- Image discovered late.
- CSS or JS render-blocking.
- Client rendering waits for API.
- Font blocks text.
- CDN/cache misses.

Fixes:

- Improve server response.
- Preload critical image/font carefully.
- Use responsive images.
- Reduce render-blocking CSS/JS.
- SSR critical content where appropriate.
- Cache HTML/data safely.

### High INP

Check:

- Long tasks near interaction.
- Heavy event handlers.
- React re-render cost.
- Layout thrashing.
- Large JSON parsing.
- Third-party scripts.

Fixes:

- Reduce synchronous work.
- Split tasks.
- Defer non-critical work.
- Use workers.
- Reduce render scope.
- Virtualize large lists.

### High CLS

Check:

- Images without dimensions.
- Ads/embeds without reserved space.
- Late banners.
- Font swaps.
- Dynamic content injected above existing content.

Fixes:

- Reserve dimensions.
- Use placeholders.
- Avoid inserting content above current viewport.
- Tune font loading.

---

## 58. Image Performance

Images often dominate LCP.

Good practices:

- Use correct dimensions.
- Use responsive `srcset`.
- Use modern formats where supported.
- Compress appropriately.
- Lazy-load non-critical images.
- Preload only the actual critical LCP image.
- Avoid CSS background images for critical content when they delay discovery.

Example:

```html
<img
  src="/images/hotel-room-800.webp"
  srcset="/images/hotel-room-400.webp 400w, /images/hotel-room-800.webp 800w, /images/hotel-room-1200.webp 1200w"
  sizes="(max-width: 600px) 100vw, 600px"
  width="800"
  height="500"
  alt="Hotel room with balcony"
/>
```

---

## 59. Font Performance

Fonts can hurt FCP, LCP, and CLS.

Watch for:

- Too many font weights.
- Late font discovery.
- Blocking font display.
- Layout shift when font swaps.

Common options:

```css
@font-face {
  font-family: "BrandSans";
  src: url("/fonts/brand-sans.woff2") format("woff2");
  font-display: swap;
}
```

Caution:

- `swap` can improve text visibility but may cause visual shift.
- Use size-adjust and fallback tuning where possible.

---

## 60. Third-Party Script Performance

Third-party scripts can hurt:

- Main-thread time.
- Network priority.
- Privacy/security.
- Reliability.
- INP.
- LCP.

Governance checklist:

- Is the script necessary?
- Can it load after consent or interaction?
- Can it run server-side instead?
- Is it async/defer?
- Is it measured in RUM?
- Does it have an owner?
- Is there a budget?

Senior answer:

> Third-party scripts need ownership and budgets. I would not let unowned analytics silently consume the main thread on critical flows.

---

## 61. Hydration Performance

Hydration can be expensive because the browser downloads JS, parses it, runs it, and attaches behavior to server HTML.

Symptoms:

- Content appears but cannot be interacted with quickly.
- Main thread busy after initial render.
- High INP after load.
- Large framework/runtime cost.

Mitigations:

- Reduce JS shipped for static parts.
- Split routes/components.
- Defer non-critical components.
- Partial hydration/islands where framework supports it.
- Server components where appropriate.
- Avoid heavy work during initial render.

---

## 62. Service Worker Performance

Service workers can improve repeat visits but can also cause bugs.

Benefits:

- Offline support.
- Faster cached assets.
- Background sync.
- Request interception.

Risks:

- Serving stale broken assets.
- Cache growth.
- Hard-to-debug update behavior.
- Extra latency in fetch handler.

Good practices:

- Version caches.
- Delete old caches.
- Keep fetch handler fast.
- Do not cache user-specific sensitive data casually.
- Add observability for service worker version.

---

## 63. Performance Budgets

Budgets prevent regressions.

Examples:

- Initial JS <= specific KB per route.
- LCP p75 <= 2.5 s by route.
- INP p75 <= 200 ms.
- CLS p75 <= 0.1.
- Node p95 latency <= target.
- Event-loop delay p99 <= target.
- Heap after smoke flow <= target.
- API payload size <= target.

Budgets belong in:

- CI.
- Dashboards.
- Release gates.
- PR review.
- Ownership docs.

---

## 64. Browser RUM Event Shape

Example real-user metric payload:

```js
function sendMetric(metric) {
  const body = JSON.stringify({
    name: metric.name,
    value: metric.value,
    rating: metric.rating,
    route: location.pathname,
    connection: navigator.connection?.effectiveType,
    userAgent: navigator.userAgent,
    appVersion: window.__APP_VERSION__,
    timestamp: Date.now()
  });

  navigator.sendBeacon("/rum", body);
}
```

Production caution:

- Avoid collecting sensitive data.
- Sample high-volume events.
- Use route templates, not raw paths with IDs.
- Respect privacy and consent rules.

---

## 65. Node Memory Logger

Small production-safe shape:

```js
function readMemoryUsage() {
  const memory = process.memoryUsage();

  return {
    rssMb: Math.round(memory.rss / 1024 / 1024),
    heapTotalMb: Math.round(memory.heapTotal / 1024 / 1024),
    heapUsedMb: Math.round(memory.heapUsed / 1024 / 1024),
    externalMb: Math.round(memory.external / 1024 / 1024),
    arrayBuffersMb: Math.round((memory.arrayBuffers ?? 0) / 1024 / 1024)
  };
}

setInterval(() => {
  logger.info({ memory: readMemoryUsage() }, "process memory sample");
}, 30_000);
```

Do not rely only on logs for serious memory debugging. Use profiles and snapshots.

---

## 66. Node Active Handles

Active handles can reveal resources keeping a process alive.

Examples:

- Timers.
- Sockets.
- Servers.
- File watchers.

Development-only debugging shape:

```js
setInterval(() => {
  const activeHandles = process._getActiveHandles();
  console.log(activeHandles.map(handle => handle.constructor.name));
}, 10_000);
```

Caution:

- Underscore APIs are not stable public APIs.
- Use carefully in debugging, not as normal production logic.

---

## 67. Heap Snapshot Safety In Production

Heap snapshots can be large and sensitive.

Risks:

- Pause the process.
- Increase memory pressure.
- Include secrets or PII.
- Create huge files.

Safer approach:

- Reproduce in staging if possible.
- Use canary instance if production capture is required.
- Restrict access.
- Redact and protect artifacts.
- Capture during controlled window.
- Prefer allocation sampling for lower overhead when possible.

---

## 68. Heap Snapshot Analysis

In a heap snapshot, inspect:

- Retained size, not only shallow size.
- Dominator tree.
- Object counts over time.
- Retainer paths.
- Detached DOM nodes in browser.
- Large arrays/maps/strings.
- Closures retaining unexpected state.
- Buffers and external memory clues.

Question to ask:

> What path from a root keeps this object alive?

That is the core of leak debugging.

---

## 69. Allocation Sampling

Allocation sampling helps identify where objects are allocated over time.

Use it when:

- Heap grows gradually.
- You need lower overhead than full snapshots.
- You want call stacks for allocations.

Interpretation:

- High allocation rate is not always a leak.
- A leak means retained memory grows.
- High allocation rate can still cause GC pressure and latency.

---

## 70. Flamegraph Interpretation

Flamegraph basics:

- Wider blocks mean more CPU time.
- Stack height shows call depth.
- Hot paths are wide stacks.
- Self time means time inside the function itself.
- Total time includes callees.

Common findings:

- `JSON.stringify` dominates.
- Validation library dominates.
- Regex dominates.
- Template rendering dominates.
- Logging serialization dominates.
- Sorting/filtering dominates.
- Compression dominates.

Senior answer:

> I do not optimize by intuition. I use a flamegraph to find the hot path, then decide whether the fix is algorithmic, architectural, caching, offloading, batching, or reducing work.

---

## 71. Micro-Optimization vs Algorithmic Optimization

Micro-optimization:

- Replacing one array method with another.
- Avoiding tiny allocations.
- Changing loop syntax.

Algorithmic optimization:

- O(n squared) to O(n).
- Avoiding repeated queries.
- Avoiding repeated JSON parsing.
- Indexing data once.
- Moving work out of hot path.

Interview rule:

> Start with algorithm, architecture, and measured hot paths. Discuss micro-optimizations only when profiling proves they matter.

---

## 72. Common JavaScript Performance Traps

- `Array.find` inside a loop over large data.
- Re-sorting on every render.
- Re-parsing same JSON repeatedly.
- Creating unbounded Maps.
- Sync Node APIs on request path.
- Large JSON responses.
- Regex catastrophic backtracking.
- Too many DOM nodes.
- Layout thrashing.
- Event listeners not cleaned up.
- Third-party scripts on critical path.
- Bundle includes rare admin features.
- Logging huge payloads.
- No pagination.
- DB N plus one queries.

---

## 73. Performance Code Review Checklist

Ask:

- Is data bounded?
- Is cache bounded?
- Is response payload bounded?
- Is this work on a hot path?
- Is this work repeated unnecessarily?
- Does this create many allocations?
- Does this block the browser main thread or Node event loop?
- Does this add to initial bundle?
- Does it clean timers/listeners/subscriptions?
- Does it respect backpressure?
- Does it have metrics?
- Does it have tests or budgets for regression?

---

## 74. Production Debugging: Slow Browser Interaction

Scenario:

> Users say the search page freezes while typing.

Senior investigation:

1. Check RUM INP by route and device.
2. Reproduce on lower-end device or throttled CPU.
3. Capture Performance trace while typing.
4. Look for long tasks around input events.
5. Inspect call stack.
6. Check if filtering, rendering, layout, or API calls dominate.
7. Apply fix based on evidence.
8. Verify INP and trace improvement.

Possible fixes:

- Debounce network call.
- Move filtering to worker.
- Virtualize results.
- Memoize derived data.
- Reduce re-render scope.
- Split long task into chunks.

---

## 75. Production Debugging: Node Latency Spike

Scenario:

> p99 API latency jumps from 300 ms to 5 s during peak traffic.

Senior investigation:

1. Check CPU, memory, event-loop delay, error rate.
2. Compare traces for slow and normal requests.
3. Check DB pool wait and dependency latency.
4. Capture CPU profile if CPU/event-loop delay are high.
5. Check recent deploy diff.
6. Check payload sizes and traffic mix.
7. Roll back or mitigate if user impact is active.
8. Patch root cause and add guardrails.

Answer pattern:

> If event-loop delay is high, I suspect CPU-bound work. If event-loop delay is low but request spans show DB wait, I investigate pool saturation or slow queries.

---

## 76. Production Debugging: Memory Leak In SPA

Scenario:

> A dashboard crashes after being open for one hour.

Senior investigation:

1. Track heap over time.
2. Reproduce route/navigation flow.
3. Take heap snapshot baseline.
4. Repeat suspected actions.
5. Force GC.
6. Take second snapshot.
7. Compare retained objects.
8. Look for detached DOM nodes, listeners, timers, large arrays.
9. Fix cleanup.
10. Add regression test or runtime guard if possible.

Common root causes:

- Charts not destroyed.
- WebSocket listeners not removed.
- Intervals not cleared.
- Detached DOM nodes retained.
- Global stores keeping old route data.

---

## 77. Production Debugging: Node Memory Leak

Scenario:

> Node service memory grows until pods restart.

Senior investigation:

1. Compare heap used, RSS, external memory.
2. Check if growth correlates with requests, jobs, uploads, or messages.
3. Capture heap snapshots safely or reproduce in staging.
4. Compare snapshots over repeated flows.
5. Inspect retained Maps, arrays, closures, request objects.
6. Check Buffers and native memory if RSS grows faster than heap.
7. Review caches, registries, queues, and event emitters.
8. Add limits, cleanup, TTL, or streaming.
9. Add alerts and memory trend dashboards.

---

## 78. Production Debugging: Bundle Regression

Scenario:

> Checkout initial load became slower after a release.

Senior investigation:

1. Compare bundle analyzer before and after.
2. Check route chunks.
3. Inspect dependency changes.
4. Check if a heavy library moved into initial path.
5. Look for duplicate versions.
6. Check coverage for unused JS.
7. Measure parse/compile/execution time.
8. Split or remove the cost.
9. Add bundle budget in CI.

Strong answer:

> I would not only compare compressed KB. I would also look at parse and execution time because JavaScript CPU cost often dominates on mobile.

---

## 79. Production Debugging: Event-Loop Blocked By JSON

Scenario:

> Export endpoint blocks the entire Node service.

Bad pattern:

```js
app.get("/export", async (req, res) => {
  const rows = await loadMillionsOfRows();
  res.json(rows);
});
```

Better architecture:

- Start async export job.
- Store result in object storage.
- Return job ID.
- Notify or poll for completion.
- Stream file download.

For smaller but still large responses:

- Paginate.
- Stream.
- Select fewer fields.
- Compress carefully.
- Move serialization off hot path if needed.

---

## 80. Mini Program: Event-Loop Delay Demo

```js
import { monitorEventLoopDelay } from "node:perf_hooks";

const histogram = monitorEventLoopDelay({ resolution: 10 });
histogram.enable();

setInterval(() => {
  console.log({
    p99Ms: Math.round(histogram.percentile(99) / 1_000_000),
    maxMs: Math.round(histogram.max / 1_000_000)
  });
  histogram.reset();
}, 1_000);

setInterval(() => {
  const start = Date.now();
  while (Date.now() - start < 250) {
    Math.sqrt(Math.random());
  }
}, 5_000);
```

Expected learning:

- CPU blocking makes callbacks late.
- Timers do not run exactly on schedule.
- Latency can spike even when I/O is async.

---

## 81. Mini Program: Memory Growth Demo

```js
const retained = [];

function allocateBatch() {
  const batch = Array.from({ length: 10_000 }, (_, index) => ({
    id: `${Date.now()}-${index}`,
    payload: "x".repeat(1_000)
  }));

  retained.push(batch);
}

setInterval(() => {
  allocateBatch();

  const memory = process.memoryUsage();
  console.log({
    heapUsedMb: Math.round(memory.heapUsed / 1024 / 1024),
    rssMb: Math.round(memory.rss / 1024 / 1024)
  });
}, 1_000);
```

Lesson:

- Memory grows because `retained` remains reachable.
- GC cannot collect reachable objects.

---

## 82. Mini Program: Bounded Cache

```js
class TtlCache {
  constructor({ maxEntries, ttlMs }) {
    this.maxEntries = maxEntries;
    this.ttlMs = ttlMs;
    this.values = new Map();
  }

  get(key) {
    const entry = this.values.get(key);
    if (!entry) return undefined;

    if (Date.now() > entry.expiresAt) {
      this.values.delete(key);
      return undefined;
    }

    this.values.delete(key);
    this.values.set(key, entry);
    return entry.value;
  }

  set(key, value) {
    this.values.set(key, {
      value,
      expiresAt: Date.now() + this.ttlMs
    });

    while (this.values.size > this.maxEntries) {
      const oldestKey = this.values.keys().next().value;
      this.values.delete(oldestKey);
    }
  }
}
```

Senior discussion:

- This controls size and age.
- Real systems also need metrics and invalidation rules.

---

## 83. Mini Program: Request Duration Middleware

```js
export function durationMiddleware(req, res, next) {
  const start = process.hrtime.bigint();

  res.on("finish", () => {
    const durationNs = process.hrtime.bigint() - start;
    const durationMs = Number(durationNs) / 1_000_000;

    logger.info({
      requestId: req.id,
      method: req.method,
      path: req.route?.path ?? req.path,
      statusCode: res.statusCode,
      durationMs: Number(durationMs.toFixed(2))
    }, "request completed");
  });

  next();
}
```

Production note:

- Use route templates instead of raw dynamic URLs to avoid high cardinality.

---

## 84. Mini Program: Timeout And Abort

```js
async function fetchWithTimeout(url, options = {}) {
  const timeoutMs = options.timeoutMs ?? 5_000;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal
    });
  } finally {
    clearTimeout(timeoutId);
  }
}
```

Performance connection:

- Hanging requests consume resources.
- Timeouts protect latency and memory.
- Cancellation prevents stale work.

---

## 85. Mini Program: Chunked Browser Processing

```js
async function normalizeRowsWithoutFreezing(rows) {
  const output = [];
  const chunkSize = 500;

  for (let index = 0; index < rows.length; index += chunkSize) {
    const chunk = rows.slice(index, index + chunkSize);

    for (const row of chunk) {
      output.push(normalizeRow(row));
    }

    await new Promise(resolve => setTimeout(resolve, 0));
  }

  return output;
}
```

Interview nuance:

- This improves responsiveness.
- A worker may be better if CPU work remains large.
- Avoid `slice` allocation if profiling shows it matters.

---

## 86. Observability For Performance

You need signals before incidents.

Browser signals:

- Web Vitals by route/device.
- JS errors.
- Long tasks.
- Resource timing.
- API timings from client perspective.
- Bundle version.

Node signals:

- p50/p95/p99 latency.
- throughput.
- CPU.
- memory.
- event-loop delay.
- GC.
- dependency spans.
- DB pool wait.
- queue depth.
- error rate.

Dashboards should separate:

- deploy version.
- endpoint/route.
- region.
- dependency.
- device class.

---

## 87. Profiling In CI

Useful CI checks:

- Bundle size budgets.
- Lighthouse CI for representative pages.
- Playwright trace for key flows.
- API latency smoke tests.
- Memory smoke tests for leaks after repeated actions.
- Dependency duplicate checks.
- Safe regex checks.

CI should catch obvious regressions, not replace production RUM.

---

## 88. Load Testing JavaScript Services

Load testing goals:

- Find saturation point.
- Observe latency percentiles.
- Validate timeouts/retries/backpressure.
- Detect memory growth.
- Detect event-loop lag.
- Validate autoscaling behavior.

Avoid bad load tests:

- Only testing localhost.
- Only testing p50.
- Ignoring dependency limits.
- No warm-up.
- Unrealistic payloads.
- No think time.
- No production-like data volume.

---

## 89. Performance Testing Frontend Flows

Test realistic flows:

- Initial landing route.
- Login.
- Search.
- Dashboard with large data.
- Checkout/payment.
- Route transitions.
- Long session navigation.

Measure:

- LCP.
- INP proxy in lab where possible.
- Long tasks.
- JS heap after repeated flows.
- Network requests.
- Bundle size.

---

## 90. Debugging With Source Maps

Source maps help map minified production code back to original source.

Best practices:

- Upload source maps to observability provider.
- Do not publicly expose sensitive source maps unless acceptable.
- Match source maps to exact release version.
- Include release ID in frontend and backend telemetry.

Without versioned source maps, production traces become much harder to act on.

---

## 91. Release Regression Strategy

When performance regresses after deploy:

1. Confirm regression with metrics.
2. Compare release versions.
3. Check feature flags and experiments.
4. Inspect changed dependencies.
5. Compare bundle and profile data.
6. Roll back or disable feature if impact is severe.
7. Patch root cause.
8. Add regression guardrail.

Senior behavior:

- Mitigate user pain first.
- Root-cause after stabilization.
- Preserve evidence.

---

## 92. Performance And Security Intersection

Performance bugs can become security risks.

Examples:

- Catastrophic regex enables ReDoS.
- Expensive endpoint enables DoS.
- Unbounded upload consumes memory.
- Unbounded JSON body blocks event loop.
- No rate limit allows resource exhaustion.
- Logging huge payloads leaks PII and increases cost.

Secure performance posture:

- Body size limits.
- Rate limits.
- Timeouts.
- Safe regex.
- Streaming uploads.
- Backpressure.
- Circuit breakers.

---

## 93. Body Size Limits

Node service example:

```js
app.use(express.json({ limit: "1mb" }));
```

Why it matters:

- Large bodies consume memory.
- JSON parsing blocks the event loop.
- Attackers can create cheap server-side cost.

For large legitimate uploads:

- Stream files.
- Store directly to object storage where possible.
- Validate metadata first.
- Scan asynchronously if needed.

---

## 94. Backpressure In APIs

Backpressure means the system says "slow down" before it collapses.

Patterns:

- Rate limits.
- Concurrency limits.
- Queue limits.
- Circuit breakers.
- Load shedding.
- 429/503 responses with retry guidance.
- Bounded worker pools.

Without backpressure:

- Queues grow unbounded.
- Memory rises.
- Latency becomes useless.
- Retries amplify load.

---

## 95. Concurrency Limiter

```js
function createLimiter(maxConcurrent) {
  let active = 0;
  const queue = [];

  function runNext() {
    if (active >= maxConcurrent || queue.length === 0) return;

    const { task, resolve, reject } = queue.shift();
    active += 1;

    Promise.resolve()
      .then(task)
      .then(resolve, reject)
      .finally(() => {
        active -= 1;
        runNext();
      });
  }

  return function limit(task) {
    return new Promise((resolve, reject) => {
      queue.push({ task, resolve, reject });
      runNext();
    });
  };
}
```

Production addition:

- Bound the queue.
- Add timeout.
- Emit metrics.
- Reject fast during overload.

---

## 96. Browser Storage Performance

Storage APIs can affect responsiveness.

Watch for:

- Large `localStorage` reads/writes blocking main thread.
- Storing huge JSON strings.
- Parsing on startup.
- Unbounded IndexedDB data.
- Cache cleanup missing.

`localStorage` is synchronous.

Bad:

```js
const state = JSON.parse(localStorage.getItem("huge-state"));
```

Better:

- Store less.
- Use async storage like IndexedDB for larger data.
- Load lazily.
- Version and clean old state.

---

## 97. Performance-Friendly Data Shapes

Prefer data structures that match access patterns.

Bad repeated lookup:

```js
function attachRooms(bookings, rooms) {
  return bookings.map(booking => ({
    ...booking,
    room: rooms.find(room => room.id === booking.roomId)
  }));
}
```

Better:

```js
function attachRooms(bookings, rooms) {
  const roomsById = new Map(rooms.map(room => [room.id, room]));

  return bookings.map(booking => ({
    ...booking,
    room: roomsById.get(booking.roomId) ?? null
  }));
}
```

Rule:

- If you repeatedly look up by ID, build an index.

---

## 98. Sorting Performance

Sorting is O(n log n), and comparators matter.

Bad:

```js
items.sort((left, right) => {
  return new Date(left.createdAt) - new Date(right.createdAt);
});
```

Better:

```js
items.sort((left, right) => {
  return left.createdAtTimestamp - right.createdAtTimestamp;
});
```

Reason:

- Comparator runs many times.
- Avoid expensive parsing inside comparator.

---

## 99. Avoiding Unnecessary Allocation

Allocations are normal, but hot paths matter.

Potentially costly:

```js
function normalize(items) {
  return items
    .map(item => ({ ...item, name: item.name.trim() }))
    .filter(item => item.name.length > 0)
    .map(item => ({ id: item.id, label: item.name.toUpperCase() }));
}
```

More direct:

```js
function normalize(items) {
  const result = [];

  for (const item of items) {
    const name = item.name.trim();
    if (name.length === 0) continue;

    result.push({
      id: item.id,
      label: name.toUpperCase()
    });
  }

  return result;
}
```

Caution:

- Prefer readability until profiling shows this path matters.

---

## 100. Error Stack Cost

Creating errors can be expensive if done repeatedly on hot paths.

Bad:

```js
function validateItem(item) {
  if (!item.id) {
    return new Error("Missing id");
  }

  return null;
}
```

Better for high-volume validation:

```js
function validateItem(item) {
  if (!item.id) {
    return { code: "MISSING_ID", message: "Missing id" };
  }

  return null;
}
```

Use actual `Error` objects for exceptional paths, not routine control flow at massive volume.

---

## 101. Date Performance

Date parsing can be expensive and inconsistent if repeated heavily.

Better patterns:

- Store timestamps when possible.
- Parse once at boundaries.
- Avoid parsing inside sort comparators.
- Avoid locale formatting in huge loops.
- Cache expensive formatters.

Formatter example:

```js
const dateFormatter = new Intl.DateTimeFormat("en-US", {
  dateStyle: "medium",
  timeStyle: "short"
});

function formatBookingTime(timestamp) {
  return dateFormatter.format(new Date(timestamp));
}
```

---

## 102. Intl Formatter Trap

Bad:

```js
function formatPrices(prices) {
  return prices.map(price => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD"
    }).format(price);
  });
}
```

Better:

```js
const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD"
});

function formatPrices(prices) {
  return prices.map(price => currencyFormatter.format(price));
}
```

---

## 103. Browser Resource Timing

Resource Timing helps understand network cost.

```js
const resources = performance.getEntriesByType("resource");

const slowResources = resources
  .filter(resource => resource.duration > 500)
  .map(resource => ({
    name: resource.name,
    duration: Math.round(resource.duration),
    transferSize: resource.transferSize
  }));

console.table(slowResources);
```

Use it for:

- Slow scripts.
- Large images.
- CDN issues.
- Cache misses.
- Third-party resources.

---

## 104. Navigation Timing

```js
const navigation = performance.getEntriesByType("navigation")[0];

console.log({
  ttfb: Math.round(navigation.responseStart - navigation.requestStart),
  domContentLoaded: Math.round(navigation.domContentLoadedEventEnd),
  loadEventEnd: Math.round(navigation.loadEventEnd)
});
```

Use Navigation Timing to separate:

- server time.
- redirect time.
- DNS/TLS/connect time.
- response download.
- document parsing.
- load event.

---

## 105. PerformanceObserver For Measures

```js
const observer = new PerformanceObserver(list => {
  for (const entry of list.getEntries()) {
    console.log({
      name: entry.name,
      duration: Math.round(entry.duration)
    });
  }
});

observer.observe({ type: "measure", buffered: true });
```

Use custom measures for business flows:

- checkout submission.
- search result render.
- dashboard hydration.
- report export preparation.

---

## 106. Node Async Hooks Caution

`async_hooks` can trace async resources, but it has complexity and overhead.

Use cases:

- Context propagation.
- Advanced diagnostics.
- Request correlation.

Cautions:

- Can affect performance.
- Can be hard to interpret.
- Use built-in `AsyncLocalStorage` for request context where appropriate.

---

## 107. AsyncLocalStorage Performance Caution

```js
import { AsyncLocalStorage } from "node:async_hooks";

const requestContext = new AsyncLocalStorage();

app.use((req, res, next) => {
  requestContext.run({ requestId: req.id }, next);
});
```

Useful for logging correlation.

But:

- Measure overhead in high-throughput services.
- Avoid storing huge request objects in context.
- Store IDs and small metadata only.

---

## 108. Memory-Safe EventEmitter Usage

Bad:

```js
eventBus.on("booking.updated", booking => {
  sendNotification(booking);
});
```

If called repeatedly during setup, listeners accumulate.

Better:

```js
function subscribeToBookingUpdates(eventBus) {
  function handleBookingUpdated(booking) {
    sendNotification(booking);
  }

  eventBus.on("booking.updated", handleBookingUpdated);

  return () => {
    eventBus.off("booking.updated", handleBookingUpdated);
  };
}
```

Warning sign:

- `MaxListenersExceededWarning` means investigate listener ownership.

---

## 109. WebSocket Performance And Memory

Risks:

- Connection maps grow forever.
- Heartbeat timers leak.
- Per-client queues grow unbounded.
- Large messages block processing.
- Slow clients cause memory pressure.

Good practices:

- Heartbeat with cleanup.
- Bound per-client queue.
- Drop or close slow clients.
- Limit message size.
- Authenticate connections.
- Track connection count and send queue size.

---

## 110. Queue Worker Performance

Worker bottlenecks:

- CPU-heavy job processing.
- Slow dependency calls.
- No concurrency limit.
- Too much concurrency causing dependency saturation.
- Job payload too large.
- Retry storm.
- Poison messages.
- Memory not released after job.

Metrics:

- queue depth.
- oldest job age.
- processing time.
- success/failure count.
- retry count.
- worker memory.
- dependency latency.

---

## 111. Retry Storms

Retries improve reliability only with control.

Bad:

```js
async function callDependency() {
  try {
    return await fetch("https://dependency.example/api");
  } catch {
    return callDependency();
  }
}
```

Better:

```js
async function retryWithBackoff(task, { maxAttempts, baseDelayMs }) {
  let lastError;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await task();
    } catch (error) {
      lastError = error;

      if (attempt === maxAttempts) break;

      const jitter = Math.floor(Math.random() * baseDelayMs);
      const delayMs = baseDelayMs * 2 ** (attempt - 1) + jitter;
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }

  throw lastError;
}
```

Performance link:

- Bad retries amplify latency and load.
- Use backoff, jitter, caps, timeouts, and circuit breakers.

---

## 112. Profiling Does Not Replace Architecture

Sometimes the fix is not a local code optimization.

Examples:

- Use async export instead of synchronous export.
- Use CDN instead of app server for static assets.
- Use pagination instead of huge payloads.
- Use database index instead of filtering in JS.
- Use queue instead of doing work in request.
- Use cache instead of recomputing hot data.
- Use worker pool instead of blocking event loop.

Senior answer:

> Profiling tells me where time goes. Architecture decides whether that work should happen there at all.

---

## 113. Interview Pattern: Diagnose Slow Page Load

Strong answer:

> I would first separate server, network, resource, and main-thread cost. I would check TTFB, LCP, render-blocking assets, bundle size, image size, and JS execution. Then I would use a Network waterfall and Performance trace. If LCP is delayed by server response, I optimize backend/cache/CDN. If the resource is discovered late, I preload or change markup. If JS blocks rendering, I reduce or defer script work. Finally, I verify with Lighthouse and production RUM by route/device.

---

## 114. Interview Pattern: Diagnose Slow Node API

Strong answer:

> I would start with latency percentiles and traces, not averages. Then I would check CPU, event-loop delay, memory, dependency spans, DB pool wait, and recent deploys. If CPU and event-loop delay are high, I profile the process. If CPU is low and dependency spans are high, I investigate DB/API latency, pool saturation, or retries. I would mitigate first if production is impacted, then fix and add metrics or tests to prevent recurrence.

---

## 115. Interview Pattern: Diagnose Memory Leak

Strong answer:

> I would confirm whether heap, RSS, or external memory is growing. Then I would reproduce the flow, capture snapshots or allocation profiles, compare retained objects, and inspect retainer paths. In browser apps I look for detached DOM nodes, listeners, timers, and stale closures. In Node I look for unbounded Maps, request registries, caches, Buffers, event listeners, and queues. The fix is to correct ownership with cleanup, TTL, capacity, streaming, or cancellation.

---

## 116. Interview Pattern: Improve INP

Strong answer:

> INP is about interaction responsiveness. I would collect real-user data by route and device, capture a trace around the poor interaction, and locate long tasks after the input. Common fixes include reducing event handler work, splitting long tasks, moving CPU work to a worker, reducing re-render scope, virtualizing large lists, and deferring analytics or non-critical updates. I would validate p75 INP and trace improvements after the fix.

---

## 117. Interview Pattern: Reduce Bundle Size

Strong answer:

> I would use a bundle analyzer to find large dependencies, duplicate versions, and code in the initial path that is not needed for first render. Then I would apply route-level splitting, dynamic imports for rare heavy components, dependency replacement, tree-shaking-friendly imports, and modern build targets. I would also measure parse and execution time, because compressed KB alone does not represent the full JavaScript cost.

---

## 118. Interview Pattern: Event Loop Lag

Strong answer:

> Event-loop lag means callbacks are running later than expected because the main thread is busy. I would measure event-loop delay and CPU alongside request latency. If high lag correlates with high CPU, I would capture a CPU profile and look for JSON work, regex, validation, compression, crypto, logging, or large data transforms. Fixes include better algorithms, streaming, pagination, worker threads, caching, or moving work out of request path.

---

## 119. Common Wrong Answers

| Wrong Answer | Why It Is Weak | Better Answer |
|---|---|---|
| Use `useMemo` everywhere. | Adds complexity without evidence. | Profile renders and memoize hot paths only. |
| Node is async so it cannot block. | JS CPU blocks the event loop. | Async I/O helps, CPU still blocks. |
| Memory leak means GC is broken. | Usually references remain reachable. | Find retainer path and fix ownership. |
| Bundle size is only network. | Parse/compile/execute also matter. | Measure total JS cost. |
| Increase DB pool size. | Can worsen contention. | Measure pool wait and DB capacity. |
| Cache everything. | Staleness and memory risk. | Cache bounded, safe, observable data. |
| Average latency is fine. | Tail users suffer. | Use p95/p99 and segment by route/device. |

---

## 120. MAANG Scenario: Search UI Freezes

Question:

> Search suggestions freeze on low-end Android devices after 6 characters. How do you debug and fix?

Strong answer:

1. Measure INP and long tasks for search route by device.
2. Reproduce with CPU throttling.
3. Capture Performance trace while typing.
4. Check whether cost is input handler, filtering, rendering, layout, network, or third-party work.
5. Debounce API call if duplicate network requests exist.
6. Move heavy local filtering to worker if CPU-bound.
7. Virtualize results if DOM is large.
8. Reduce re-render scope if framework rendering dominates.
9. Verify trace and RUM improvement.

---

## 121. MAANG Scenario: Node Service CPU At 100 Percent

Question:

> A Node API runs at 100 percent CPU and health checks fail. What do you do?

Strong answer:

1. Mitigate first: scale, shed load, disable feature, or roll back if needed.
2. Check event-loop delay and request latency.
3. Capture CPU profile during spike.
4. Identify hot function.
5. Check recent deploy and traffic shape.
6. Common suspects: JSON, regex, validation, compression, crypto, logging, sorting.
7. Fix by reducing work, changing algorithm, streaming, caching, worker threads, or async job.
8. Add regression test, load test, or alert.

---

## 122. MAANG Scenario: Memory Grows Only In Production

Question:

> Memory is stable in staging but grows in production.

Strong answer:

1. Compare traffic, payload sizes, feature flags, and long-session behavior.
2. Segment memory by heap/RSS/external.
3. Check if growth correlates with specific route/job/customer.
4. Capture safe snapshots or allocation profiles from canary.
5. Inspect retained objects and retainer paths.
6. Look for production-only caches, queues, websockets, uploads, or analytics.
7. Add bounds and cleanup.
8. Keep dashboards by version and instance.

---

## 123. MAANG Scenario: Checkout LCP Regression

Question:

> LCP p75 regressed from 2.1 s to 4.0 s after release.

Strong answer:

1. Confirm by route, device, browser, and release.
2. Check TTFB and backend changes.
3. Inspect LCP element.
4. Compare waterfall before/after.
5. Check image, font, CSS, JS, API dependency.
6. Compare bundle analyzer.
7. Roll back or disable feature if active impact is high.
8. Fix discovered root cause and add budget.

---

## 124. MAANG Scenario: Large Export Endpoint

Question:

> A Node endpoint exporting reports causes latency for unrelated APIs.

Strong answer:

> The export is likely blocking CPU, memory, or event loop. I would check event-loop delay, CPU, heap/RSS, and traces. A large export should usually be an async job or streaming pipeline, not a synchronous request that loads all rows and stringifies JSON. I would move it to a queue, stream results to storage, expose job status, and enforce concurrency limits.

---

## 125. MAANG Scenario: Dashboard Memory Leak

Question:

> Dashboard memory grows after each tab switch.

Strong answer:

1. Reproduce tab switching.
2. Take heap snapshot baseline.
3. Switch tabs repeatedly.
4. Force GC and take second snapshot.
5. Compare retained objects.
6. Check chart instances, timers, subscriptions, WebSocket handlers, detached DOM nodes.
7. Ensure cleanup on unmount/tab removal.
8. Add automated long-session test if possible.

---

## 126. Decision Table: What Fix Should I Reach For?

| Root Cause | Good Fix |
|---|---|
| O(n squared) transform | Use index/map or better algorithm. |
| Huge DOM list | Virtualize, paginate, or progressively render. |
| Large initial JS | Code split, remove dependency, tree shake. |
| Heavy interaction CPU | Worker, chunking, memoization, reduce render scope. |
| Large response | Pagination, field selection, streaming. |
| DB wait | indexes, query optimization, batching, pool tuning. |
| Event-loop blocking | profile, reduce sync work, worker threads. |
| Heap leak | cleanup, bounds, TTL, remove references. |
| RSS leak | inspect Buffers/native memory/streaming. |
| Retry storm | backoff, jitter, circuit breaker, rate limit. |
| Cache stampede | coalescing, TTL jitter, stale-while-revalidate. |

---

## 127. Production Checklist Before Shipping Heavy JS

- Does it affect initial route bundle?
- Is it loaded only when needed?
- Is the work bounded?
- Does it run on every render or interaction?
- Does it allocate large objects?
- Does it touch layout repeatedly?
- Does it clean listeners/timers/subscriptions?
- Is it measured with RUM or logs?
- Is there a fallback for slow devices?
- Is there a budget or alert?

---

## 128. Production Checklist Before Shipping Node Hot Path

- Is request body size limited?
- Is response size bounded?
- Are DB queries bounded and indexed?
- Are external calls timed out?
- Are retries capped with backoff and jitter?
- Is concurrency limited?
- Are caches bounded?
- Are streams used for large payloads?
- Is CPU-heavy work offloaded?
- Are latency, event-loop delay, CPU, memory, and errors monitored?

---

## 129. Fast Debugging Commands And Tools

Browser:

- Chrome DevTools Performance panel.
- Chrome DevTools Memory panel.
- Lighthouse.
- Coverage panel.
- Network panel.
- React DevTools Profiler.
- WebPageTest.
- RUM provider.

Node:

- `node --inspect`.
- `node --cpu-prof`.
- `node --heap-prof`.
- `perf_hooks`.
- APM tracing.
- Clinic.js tooling where available.
- Heap snapshots.
- Load testing tools.

---

## 130. What To Put In A Performance Incident Report

Include:

- User impact.
- Start/end time.
- Affected route/endpoint/version.
- Metrics before/during/after.
- Root cause.
- Why detection happened when it did.
- Mitigation.
- Permanent fix.
- Preventive guardrail.
- Owner and follow-up date.

Avoid:

- Blame language.
- Vague cause like "JavaScript was slow".
- Missing evidence.
- No action item.

---

## 131. Performance Review Template

Use this for PRs or design docs.

```md
## Performance Review

- User flow:
- Expected traffic/data size:
- Critical path work:
- Main-thread/event-loop impact:
- Network payload impact:
- Memory ownership:
- Cache bounds:
- Timeout/concurrency behavior:
- Metrics added:
- Regression guardrail:
```

---

## 132. Rapid Revision

- Performance starts with user-visible symptom and metric.
- Browser bottlenecks often involve network, main thread, rendering, bundle, or third-party scripts.
- Node bottlenecks often involve event-loop blocking, dependency wait, DB pools, memory, or retries.
- Heap growth means retained JS objects; RSS growth can mean Buffers/native memory too.
- A leak is about reachability, not GC failure.
- Long tasks hurt INP.
- LCP is usually server, image, render-blocking asset, or client rendering delay.
- CLS is layout instability.
- Bundle cost includes download plus parse, compile, execute, and memory.
- Algorithmic fixes usually beat micro-optimizations.
- Unbounded caches, queues, arrays, and maps are production risks.
- Streams and backpressure protect memory.
- Worker threads and Web Workers help CPU-heavy work, but transfer and orchestration have cost.
- Use percentiles, not averages.
- Profile before optimizing.
- Verify after fixing.

---

## 133. 60-Second Interview Answer

> I debug JavaScript performance by first classifying the symptom: page load, interaction latency, memory growth, Node API latency, or bundle regression. For browser issues I look at Core Web Vitals, Network, Performance, Memory, Coverage, and real-user data by route/device. For Node I check p95/p99 latency, CPU, event-loop delay, heap/RSS/external memory, GC, traces, DB pool wait, and dependency latency. Then I capture the right profile, isolate the hot path or retainer path, apply the smallest high-impact fix, and verify with the same metric. I also add budgets, alerts, tests, or dashboards so the regression does not come back.

---

## 134. Official And High-Value Sources

Use these for deeper reference:

- MDN Web Docs: Performance APIs, Web Workers, Resource Timing, Navigation Timing.
- web.dev: Core Web Vitals, LCP, INP, CLS, performance guidance.
- Chrome DevTools documentation: Performance panel, Memory panel, Coverage, Network.
- Node.js documentation: `perf_hooks`, diagnostics, inspector, worker threads, streams.
- V8 blog and documentation: garbage collection, profiling, JavaScript engine behavior.
- React documentation: rendering, memoization, profiler guidance.
- OpenTelemetry documentation: tracing and metrics concepts.
- OWASP guidance: ReDoS, resource exhaustion, input limits.

---

## 135. Final Mental Model

Performance engineering is not about clever tricks.

It is about disciplined diagnosis:

1. Measure the symptom users feel.
2. Find where time or memory is actually going.
3. Remove unnecessary work.
4. Bound unavoidable work.
5. Move heavy work off critical paths.
6. Protect systems with backpressure.
7. Keep observability and budgets in place.

That is the difference between a developer who can make code faster and an engineer who can keep production healthy.
