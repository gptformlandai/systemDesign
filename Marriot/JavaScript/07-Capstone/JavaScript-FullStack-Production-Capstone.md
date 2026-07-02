# JavaScript Full-Stack Production Capstone

> Track: JavaScript Interview Track - Capstone  
> Goal: connect language, browser, TypeScript, Node, security, testing, performance, observability, build tooling, and runtime decisions into one system.

---

## 1. Intuition

The capstone is where JavaScript stops being a set of interview facts and becomes a system.
It proves you can connect a user click to browser behavior, network calls, Node runtime,
security, tests, performance, observability, deployment, and incident response.

---

## 2. Capstone System

```text
Hotel Booking JavaScript Platform
```

Capabilities:

- accessible hotel search UI
- localized price/date display
- booking form with validation
- cancellable search requests
- Node BFF/API for booking
- idempotent create booking
- payment dependency timeout/retry policy
- file export using streams
- worker/queue path for heavy work
- browser RUM and Node observability
- source maps and release metadata
- safe build/package/runtime setup

---

## 3. Architecture

```text
Browser UI
  -> TypeScript components
  -> accessible forms and i18n
  -> fetch with AbortController
  -> Node BFF/API
      -> validation
      -> auth/session/JWT checks
      -> idempotency
      -> payment/inventory clients
      -> logs/metrics/traces
      -> queues/workers/streams where needed
  -> database/cache/external APIs
  -> observability and release systems
```

---

## 4. Request Lifecycle

1. User fills booking form.
2. UI validates visible fields and preserves accessibility.
3. Client sends request with idempotency key and trace/request ID.
4. Node API validates runtime data; TypeScript alone is not trusted.
5. Auth and authorization run server-side.
6. API checks idempotency store.
7. API calls inventory/payment with timeouts and safe retry policy.
8. API returns stable success/error shape.
9. Browser updates UI and announces relevant state.
10. Logs, metrics, traces, and RUM connect the flow.
11. Source maps map production errors to source.
12. Canary/rollback protects deployment.

---

## 5. Required Decisions

| Area | Decision To Defend |
|---|---|
| Setup | Node LTS, package manager, lockfile, scripts, CI |
| Module format | ESM/CJS/dual package boundaries |
| Frontend | CSR/SSR/SSG/islands depending product |
| Accessibility | semantic controls, keyboard, focus, announcements |
| i18n | Intl formatting, message templates, text expansion |
| TypeScript | strict boundaries plus runtime validation |
| API | REST/BFF, validation, stable error shape |
| Security | XSS, CSRF, token storage, prototype pollution, dependency risk |
| Resilience | timeouts, retries, backoff, idempotency, circuit breaker awareness |
| Performance | Web Vitals, bundle budget, long tasks, worker offload |
| Node runtime | event-loop lag, streams/backpressure, worker threads |
| Observability | RUM, logs, metrics, traces, source maps, release metadata |
| Deployment | Node vs edge/serverless, build artifact, rollback |

---

## 6. Failure Modes

| Failure | User Observes | Fix |
|---|---|---|
| stale search response | old results overwrite new | AbortController/request token |
| duplicate booking | two confirmations | idempotency key and server invariant |
| checkout hangs | spinner forever | timeout, error state, retry policy |
| inaccessible modal | keyboard trap | focus management and semantic dialog |
| wrong currency/date | user confusion | `Intl` APIs and locale tests |
| bad bundle | slow first load | bundle analyzer and budget |
| event loop blocked | API p99 spike | profile, worker/job offload |
| memory leak | tab/service grows | heap snapshot and cleanup ownership |
| token stolen | account risk | HttpOnly/session/BFF or careful token model |
| source maps missing | unreadable stack | private source-map upload in CI |

---

## 7. Testing Strategy

| Test Type | What It Proves |
|---|---|
| Unit | pure functions, mappers, validators |
| Component | visible UI behavior, accessible queries |
| Integration | API plus database/cache boundary |
| Contract | frontend/BFF/API compatibility |
| E2E | critical booking path |
| Security | XSS/CSRF/authz/prototype pollution checks |
| Accessibility | keyboard/focus/ARIA checks |
| Performance | bundle budget, Web Vitals, API latency |
| Observability | logs/traces/source maps/release metadata present |

---

## 8. Practical Question

> Design and implement a JavaScript hotel booking platform from browser UI to Node API,
> with production-grade security, testing, performance, observability, and runtime decisions.

---

## 9. Strong Answer

I would start with reproducible setup: Node LTS, one package manager, lockfile, scripts,
TypeScript, lint, test, and CI. The browser UI would use semantic HTML, accessible form
controls, focus management, and `Intl` for localized dates/currency. Search requests use
`AbortController` or stale response guards. The Node BFF/API validates all runtime input,
enforces auth and authorization, handles idempotency for booking creation, and calls payment
or inventory with timeouts, bounded retries, and stable error shapes. Large exports use
streams and backpressure; CPU-heavy work moves to workers or queues. Security covers XSS,
CSRF, token storage, prototype pollution, SSRF, and npm supply chain. Tests cover unit,
component, integration, contract, E2E, accessibility, and performance budgets. Observability
connects browser RUM, source-mapped errors, Node logs, metrics, traces, event-loop delay,
memory, and release metadata. Runtime choice is intentional: browser for UI, Node LTS for
checkout API, edge only for small latency-sensitive routing/auth/cache decisions.

---

## 10. Revision Notes

- One-line summary: JavaScript mastery is the full path from UI event to runtime behavior,
  production safety, and incident evidence.
- Three keywords: lifecycle, runtime, evidence.
- One interview trap: TypeScript types do not replace runtime validation at trust boundaries.
- One memory trick: click -> fetch -> API -> observe -> recover.

