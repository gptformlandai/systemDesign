# JavaScript Modern Runtimes: Browser, Node, Edge, Deno, And Bun Gold Sheet

> Track: JavaScript Interview Track - Senior / MAANG  
> Goal: choose the right JavaScript runtime instead of assuming all JavaScript environments are equivalent.

---

## 1. Intuition

JavaScript is the language. The runtime is the world it lives in. Browser JavaScript has
DOM and Web APIs. Node.js has file system, processes, streams, and server APIs. Edge runtimes
are small and close to users. Deno and Bun change tooling and runtime assumptions.

---

## 2. Runtime Map

| Runtime | Strength | Common Limits |
|---|---|---|
| Browser | UI, DOM, Web APIs, user interaction | sandboxed, no direct filesystem/server APIs |
| Node.js | backend services, scripts, streams, ecosystem | event-loop blocking, dependency risk |
| Edge runtime | low-latency request handling near users | limited Node APIs, CPU/time limits |
| Serverless Node | bursty APIs, low ops overhead | cold starts, package size, connection reuse |
| Deno | secure-by-default posture, TypeScript-friendly tooling | ecosystem compatibility decisions |
| Bun | fast runtime/tooling focus | compatibility and operational maturity checks |

---

## 3. Node.js Production Baseline

As of July 2, 2026, official Node release data lists v24 as LTS and v26 as Current. Production
apps should normally use Active LTS or Maintenance LTS unless there is a tested reason to use
Current.

Runtime selection questions:

- Is the service long-running or serverless?
- Does it need filesystem access?
- Does it use native modules?
- Does it need worker threads?
- Does it require low cold start?
- Does it run close to the user?
- Does it need Web APIs or Node APIs?

---

## 4. Browser vs Node

| Concern | Browser | Node.js |
|---|---|---|
| Global object | `window` / `globalThis` | `global` / `globalThis` |
| Modules | ESM via bundler/native | ESM and CommonJS |
| Filesystem | not direct | `node:fs` |
| Network | `fetch`, WebSocket | `fetch`, `http`, `undici`, sockets |
| UI | DOM, CSSOM | none |
| Security | sandbox, CSP, CORS | server trust boundary |
| Performance | main thread, rendering, Web Vitals | event loop, CPU, memory, IO |

Trap:

```text
Code that runs in Node may fail in browser or edge because APIs differ.
```

---

## 5. Edge Runtime

Edge is useful for:

- redirects/rewrites
- authentication gate checks
- geo/header based routing
- cache key shaping
- lightweight personalization
- low-latency BFF-style reads

Avoid edge for:

- heavy CPU work
- long-running jobs
- direct database drivers that need Node APIs
- large dependencies
- native modules
- code needing unrestricted filesystem/process APIs

Interview line:

```text
Edge runtime is a latency and cache-placement tool, not a general replacement for Node.
```

---

## 6. Deno And Bun Awareness

You do not need to turn every interview into a runtime debate, but senior candidates should
know that alternative runtimes exist.

Ask:

- Does the runtime support the required Node/npm ecosystem?
- Does deployment platform support it?
- Are observability and debugging mature enough?
- Are dependencies compatible?
- Does the team know it?
- Does it solve a real problem or only look modern?

Good answer:

```text
I would not switch runtimes casually. I would compare compatibility, deployment support,
performance evidence, security model, package ecosystem, and operational tooling.
```

---

## 7. Runtime Decision Matrix

| Use Case | Strong Default |
|---|---|
| interactive web app | browser plus framework/build tool |
| backend REST API | Node.js LTS |
| large file streaming API | Node.js with streams/backpressure |
| lightweight auth redirect | edge runtime |
| cron/background job | Node worker/container/serverless job |
| CPU-heavy computation | worker threads, separate service, or WASM depending context |
| CLI tool | Node.js, Bun, or Deno after compatibility check |
| framework-neutral widget | browser/Web Component |

---

## 8. Failure Modes

| Failure | Cause | Fix |
|---|---|---|
| Edge function crashes | imported `fs` or native module | separate edge-safe code |
| Serverless cold start spike | huge bundle/top-level imports | reduce deps and lazy-load |
| Browser bundle leaks secret | env var exposed to client | server-only config boundary |
| Node service stalls | CPU-heavy work on event loop | worker/thread/process/job |
| Deno/Bun migration breaks package | ecosystem incompatibility | compatibility test suite |
| Runtime mismatch in CI | tests run on different Node | pin Node in CI and local |

---

## 9. Practical Question

> Should a booking platform use Node, edge functions, Deno, or Bun for its checkout API?

---

## 10. Strong Answer

For checkout I would default to Node.js LTS because it has mature ecosystem support,
observability, database drivers, and operational patterns. I might use edge for lightweight
auth redirects, geo routing, or cache-related decisions before checkout, but not for heavy
payment workflows or database-heavy transactions unless the platform explicitly supports it.
I would consider Deno or Bun only with compatibility, deployment, debugging, and team readiness
evidence. Runtime choice is not just speed; it is reliability, ecosystem, debugging, security,
and rollout safety.

---

## 11. Revision Notes

- One-line summary: JavaScript runtime choice is an architecture decision, not syntax trivia.
- Three keywords: APIs, limits, operations.
- One interview trap: edge runtime is not full Node.js.
- One memory trick: browser for UI, Node for services, edge for tiny nearby decisions.

