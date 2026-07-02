# JavaScript Runnable Mini Labs

> Track: JavaScript Interview Track - Practice Upgrade  
> Goal: turn JavaScript concepts into small buildable labs.

Each lab should take 45-120 minutes.

---

## 1. Lab Output Rules

For every lab, produce:

1. short design notes
2. code sketch or runnable snippet
3. tests or manual verification steps
4. failure modes
5. 60-second interview explanation

---

## 2. Lab 1: Reproducible First Project

Build/sketch:

- Node LTS pin
- one package manager
- lockfile
- `dev`, `test`, `lint`, `typecheck`, `build`
- first function and test
- env validation example

Explain:

```text
Why the same install/build command should work locally and in CI.
```

---

## 3. Lab 2: Output Question Simulator

Build:

- list of code snippets
- predicted output
- actual output runner
- explanation field

Topics:

- hoisting
- closures
- `this`
- promises/timers
- coercion

---

## 4. Lab 3: Reliable Fetch Helper

Build:

- timeout with `AbortController`
- retry with backoff and jitter
- no retry for unsafe methods unless idempotency key exists
- error normalization

Tests:

- timeout aborts
- 500 retries
- 400 does not retry
- stale request can be cancelled

---

## 5. Lab 4: Accessible Autocomplete

Build/sketch:

- keyboard navigation
- focus management
- loading state
- stale response guard
- locale-aware display
- ARIA combobox/listbox pattern awareness

Verify:

- mouse works
- keyboard works
- screen reader state is considered
- slow network does not show stale results

---

## 6. Lab 5: Web Component Widget

Build:

- `booking-card` custom element
- observed attributes
- optional Shadow DOM
- slot for content
- cleanup in `disconnectedCallback`

Explain:

```text
When Web Components help and when a framework component is simpler.
```

---

## 7. Lab 6: Node API With Idempotency

Build/sketch:

- `POST /bookings`
- request validation
- idempotency key store
- stable error shape
- structured logs
- graceful shutdown

Tests:

- same key same payload returns same result
- same key different payload returns conflict
- missing validation returns 400

---

## 8. Lab 7: Stream Large File Safely

Build:

- stream file or generated CSV
- use `pipeline`
- avoid buffering whole file
- handle client abort
- add size/time limits

Explain:

```text
Streams plus backpressure protect memory when producer is faster than consumer.
```

---

## 9. Lab 8: Event Loop Lag Detector

Build:

- `monitorEventLoopDelay`
- synthetic blocking function
- p95/p99 log output
- alert threshold idea

Deliverable:

```text
A runbook for "API p99 rose because event loop is blocked."
```

---

## 10. Lab 9: Browser Performance Runbook

Given:

```text
Checkout page LCP and INP regress after a release.
```

Produce:

- waterfall inspection plan
- long task analysis
- bundle diff
- Web Vitals interpretation
- mitigation and prevention

---

## 11. Lab 10: Security Review

Review a small app for:

- reflected/stored/DOM XSS
- CSRF
- token storage
- prototype pollution
- SSRF
- ReDoS
- dependency risk
- secret logging

Deliverable:

```text
Threat table with vulnerability, exploit path, fix, and test.
```

---

## 12. Lab 11: Runtime Decision Memo

Given:

- public content route
- checkout API
- auth redirect
- image transform
- realtime dashboard
- background report export

Choose:

- browser
- Node
- edge
- serverless
- worker thread
- queue/job

Explain constraints and failure modes.

---

## 13. Lab 12: Observability Dashboard

Design panels:

- browser JS error rate
- LCP/INP/CLS by route
- Node request rate/errors/duration
- event-loop p99
- heap/RSS/external memory
- dependency latency
- queue age
- source map/release health

Add alerts and owners.

---

## 14. Lab 13: Build Tool Cross-Link Drill

Use adjacent BuildTools material to explain:

- dependency graph
- ESM/CJS/tree shaking
- code splitting
- source maps
- Vite vs Webpack vs Rollup
- monorepo build cache
- bundle budget

Deliverable:

```text
A 2-minute answer: "How does my JavaScript become production code?"
```

---

## 15. Lab 14: Capstone Walkthrough

Walk through:

```text
UI click -> validation -> accessible state -> fetch with cancellation -> Node API
-> validation -> idempotency -> dependency timeout/retry -> response -> logs/metrics/traces
-> browser RUM -> source-mapped error if failure -> canary/rollback.
```

Deliverable:

- 5-minute spoken answer
- failure table
- tests per stage
- observability per stage
- runtime/tooling decisions

---

## 16. Completion Gate

You completed the labs when you can:

1. create a reproducible JavaScript project
2. solve output questions by rules
3. build safe async helpers
4. design accessible localized UI
5. build production-style Node API boundaries
6. stream data with backpressure
7. debug event-loop lag and browser performance
8. review security threats
9. choose runtimes intentionally
10. explain the capstone lifecycle without notes

