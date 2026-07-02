# 37. Browser DevTools Debugging: Performance, Memory, Network, Storage

## Goal

Debug frontend problems beyond breakpoints: performance regressions, memory leaks, network waterfalls, storage/cache issues, service workers, React rendering, and Core Web Vitals.

---

## DevTools Panel Map

| Panel | Use |
|---|---|
| Elements | DOM, CSS, layout, computed styles |
| Console | runtime errors, logs, ad-hoc expressions |
| Sources | JS/TS breakpoints, source maps |
| Network | requests, headers, payloads, timing, cache |
| Performance | main-thread flame chart, layout, paint, long tasks |
| Memory | heap snapshots, allocation timeline, detached DOM |
| Application | localStorage, IndexedDB, cookies, service workers, cache |
| Lighthouse | high-level performance/accessibility checks |
| Coverage | unused JS/CSS |

---

## Network Waterfall Reading

Timing phases:

```text
Queueing -> DNS -> TCP -> TLS -> Request sent -> Waiting (TTFB) -> Download
```

Interpretation:

| Slow Phase | Likely Cause |
|---|---|
| DNS | resolver or domain issue |
| TCP | network path, firewall, server unreachable |
| TLS | cert/protocol/SNI/trust issue |
| Waiting/TTFB | backend slow or proxy waiting |
| Download | large payload or slow bandwidth |
| Stalled/Queueing | browser connection limit or priority |

---

## Performance Panel Workflow

```text
1. Open Performance panel.
2. Enable screenshots and web vitals.
3. Start recording.
4. Reproduce slow interaction.
5. Stop recording.
6. Find long tasks > 50ms.
7. Expand main thread flame chart.
8. Identify script, layout, style, paint, or GC cost.
9. Map bundled JS back to source via source maps.
```

Long task rule:

```text
If the main thread is blocked, input feels frozen.
```

---

## Core Web Vitals Debug Map

| Metric | Meaning | Common Cause |
|---|---|---|
| LCP | largest visible content load | slow image, slow server, render-blocking JS |
| INP | interaction responsiveness | long JS tasks, heavy event handler |
| CLS | layout shift | missing image dimensions, late ads/fonts |
| TTFB | server response start | backend/CDN/network latency |

Debug with Performance, Network, Lighthouse, and RUM data together.

---

## React Rendering Debug

Use React DevTools Profiler:

- which component rendered
- why it rendered
- render duration
- commit duration
- props/state changes

Common bugs:

```text
new object/function props every render
missing memoization
global state update re-renders entire tree
expensive list rendering
uncontrolled effect loops
hydration mismatch in SSR/Next.js
```

Fix:

- `React.memo`
- `useMemo` / `useCallback` carefully
- list virtualization
- split state
- avoid expensive work in render
- inspect hydration warnings

---

## Memory Leak Workflow

```text
1. Open Memory panel.
2. Take baseline heap snapshot.
3. Perform interaction repeatedly.
4. Force GC.
5. Take second snapshot.
6. Compare retained objects.
7. Look for detached DOM nodes, listeners, timers, caches.
8. Fix cleanup path.
```

Common leak roots:

- event listener not removed
- interval/timeout not cleared
- WebSocket not closed
- detached DOM retained by closure
- global cache grows forever
- React effect cleanup missing

---

## Application Panel Debugging

Check:

```text
cookies:
  SameSite, Secure, HttpOnly, domain/path, expiry

localStorage/sessionStorage:
  stale flags, corrupted state, environment mismatch

IndexedDB:
  old schema, failed migration, huge data

Service workers:
  old worker controlling page, stale cache, failed update

Cache storage:
  wrong asset version, stale API response
```

If "hard refresh fixes it," suspect cache or service worker.

---

## Hydration And SSR Debugging

Symptoms:

- React hydration warning
- UI flickers after load
- server HTML differs from client render
- only production build fails

Causes:

- time/random value rendered on server
- browser-only API used during SSR
- locale/timezone mismatch
- feature flag differs server/client
- data fetched differently server/client

Fix:

- move browser-only code to effect/client component
- serialize consistent initial data
- avoid non-deterministic render output
- compare server HTML and client state

---

## Practical Question

> Users report the checkout page freezes for two seconds after clicking "Place Order." Backend traces are normal. How do you debug?

---

## Strong Answer

I would open Chrome DevTools Performance panel, record the click interaction, and look for long tasks on the main thread. If backend traces are normal, the freeze is likely frontend CPU, rendering, layout, or synchronous work. I would inspect the flame chart to identify expensive JavaScript, layout recalculation, paint, or GC.

If it is React, I would use React DevTools Profiler to find which component re-rendered and why. I would also check Network to ensure the request is not blocked or queued. The fix might be splitting heavy work, using a worker, virtualizing a list, memoizing carefully, or removing a render loop.

---

## Interview Sound Bite

Frontend debugging is not only breakpoints. Network explains request timing, Performance explains main-thread blocking, Memory explains retained objects, Application explains storage/cache/service workers, and React Profiler explains component render cost.
