# JavaScript Interview Track Index

This folder is the JavaScript language and runtime track for frontend, backend Node.js,
full-stack, and MAANG-style interviews.

It now contains 31 topic/practice sheets plus this index.

Goal:

- Build JavaScript from beginner setup to senior production judgment.
- Keep each topic modular so revision is easy.
- Make the answer pattern repeatable: mental model, definition, internals, code, traps,
  production judgment, scenario answer, and revision.
- Cover browser JavaScript, Node.js, TypeScript, modern runtimes, security, testing,
  performance, observability, and system design together.
- Convert knowledge into interview performance using output drills, machine coding,
  active recall, mock rounds, runnable labs, and a capstone.

Use this index as the reading order.

---

## 0. Setup Layer

Read this first if you are a beginner or if your project setup feels random.

| Order | File | What It Builds |
|---:|---|---|
| 0 | `00-Setup/JavaScript-Setup-Node-Package-Managers-First-Project-Gold-Sheet.md` | Node setup, package managers, lockfiles, scripts, module formats, first project workflow |

Setup target:

- You can install and pin Node.js intentionally.
- You can explain npm, pnpm, Yarn, lockfiles, and `package.json`.
- You can create a small JavaScript project with lint, format, test, and start scripts.
- You can avoid mixed lockfiles, hidden runtime assumptions, and ESM/CommonJS confusion.

---

## 1. Starter Path

Read these first if you want JavaScript fundamentals to feel clear instead of magical.

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Starter-Path/JavaScript-Core-Interview-Master-Sheet.md` | Types, variables, equality, coercion, functions, objects, arrays, errors |
| 2 | `01-Starter-Path/JavaScript-Execution-Context-Scope-Closures-Deep-Dive.md` | Execution context, call stack, lexical environment, hoisting, closures, memory retention |
| 3 | `01-Starter-Path/JavaScript-This-Prototypes-Classes-Deep-Dive.md` | `this`, binding rules, prototype chain, `new`, classes, inheritance, descriptors |
| 4 | `01-Starter-Path/JavaScript-Arrays-Objects-Functional-Patterns.md` | `map`, `filter`, `reduce`, grouping, cloning, immutability, object transforms |
| 5 | `01-Starter-Path/JavaScript-Modern-ES-Features-Master-Sheet.md` | ES6+ features, modules, destructuring, spread/rest, optional chaining, nullish coalescing |

Starter target:

- You can explain JavaScript types, scope, closures, `this`, prototypes, and basic async
  behavior without guessing.
- You can solve common output questions by rules, not memory.
- You can write small interview snippets using arrays, objects, functions, and modern syntax.
- You can explain when JavaScript behavior is convenient and when it becomes a trap.

---

## 2. Intermediate Frontend / Full-Stack Path

After the starter path, read these.

| Order | File | What It Builds |
|---:|---|---|
| 6 | `02-Intermediate-Frontend-FullStack/JavaScript-Async-Event-Loop-Promises-Master-Sheet.md` | Event loop, microtasks, macrotasks, Promises, async/await, timers, output order |
| 7 | `02-Intermediate-Frontend-FullStack/TypeScript-For-JavaScript-Engineers-Master-Sheet.md` | Types, interfaces, generics, narrowing, utility types, production safety |
| 8 | `02-Intermediate-Frontend-FullStack/JavaScript-Browser-DOM-Web-APIs-Master-Sheet.md` | DOM, events, bubbling/capturing, storage, fetch, CORS, rendering basics |
| 9 | `02-Intermediate-Frontend-FullStack/JavaScript-Browser-Storage-Workers-PWA-Performance-Mastery-Sheet.md` | Cookies, IndexedDB, workers, service workers, PWA, hydration, lazy loading, frontend performance |
| 10 | `02-Intermediate-Frontend-FullStack/JavaScript-Accessibility-I18n-Web-Components-Gold-Sheet.md` | Accessibility, semantic HTML, ARIA judgment, keyboard support, i18n, Web Components, Shadow DOM |
| 11 | `02-Intermediate-Frontend-FullStack/JavaScript-React-Integration-Hooks-Concurrent-Rendering-Master-Sheet.md` | React hooks, concurrent rendering, Suspense, streaming SSR, hydration, RSC awareness |
| 12 | `02-Intermediate-Frontend-FullStack/JavaScript-Frontend-Interview-Scenarios.md` | UI race conditions, forms, state bugs, debounce/throttle, rendering and performance traps |

Intermediate target:

- You can explain how browser JavaScript responds to user events, network calls, timers,
  rendering work, storage, and worker execution.
- You can reason about async ordering, race conditions, stale state, UI responsiveness,
  accessibility, and localization.
- You can use TypeScript to prevent common production bugs without confusing compile-time
  checks with runtime validation.
- You can connect JavaScript language behavior to actual frontend user experience.

---

## 3. Backend Node.js Path

Use this path if you are preparing for Node.js backend, full-stack, platform, or production
engineering interviews.

| Order | File | What It Builds |
|---:|---|---|
| 13 | `03-Backend-NodeJS/JavaScript-NodeJS-Backend-Production-Master-Sheet.md` | Node runtime, CommonJS/ESM, APIs, streams, buffers, event-loop blocking, worker threads |
| 14 | `03-Backend-NodeJS/JavaScript-NodeJS-Interview-Scenarios.md` | API latency, blocked event loop, retries, queues, streams, backpressure, memory leaks |
| 15 | `03-Backend-NodeJS/JavaScript-Security-Best-Practices-Master-Sheet.md` | XSS, CSRF, prototype pollution, JWT/storage, npm supply chain, injection risks |
| 16 | `03-Backend-NodeJS/JavaScript-Testing-Patterns-Master-Sheet.md` | Jest/Vitest, mocks, fake timers, integration tests, Playwright, contract tests |

Backend target:

- You can explain why Node.js is efficient for IO-heavy workloads and weak for CPU-heavy
  work unless designed carefully.
- You can identify event-loop blocking, unbounded concurrency, missing backpressure, memory
  leaks, retry storms, and dependency risks.
- You can build safer backend JavaScript with validation, timeouts, idempotency, logging,
  dependency hygiene, and tests.
- You can explain when to use streams, worker threads, queues, caching, and external systems.

---

## 4. Senior / MAANG Path

These are the pro sheets.

| Order | File | What It Builds |
|---:|---|---|
| 17 | `04-Senior-MAANG/JavaScript-Current-ECMAScript-Platform-Update-Gold-Sheet.md` | Stable feature awareness, TC39 proposal judgment, runtime support checks, adoption safety |
| 18 | `04-Senior-MAANG/JavaScript-Modern-Runtimes-Node-Browser-Edge-Deno-Bun-Gold-Sheet.md` | Browser vs Node vs edge vs serverless vs Deno vs Bun runtime trade-offs |
| 19 | `04-Senior-MAANG/JavaScript-Performance-Memory-Debugging-Master-Sheet.md` | Memory leaks, profiling, DevTools, heap snapshots, event-loop lag, bundle/runtime cost |
| 20 | `04-Senior-MAANG/JavaScript-Observability-SRE-OpenTelemetry-Production-Gold-Sheet.md` | Logs, metrics, traces, RUM, source maps, SLOs, alerting, incident response |
| 21 | `04-Senior-MAANG/JavaScript-Production-Debugging-Case-Studies.md` | High CPU, memory leak, blocked event loop, API storm, bad bundle, async failures |
| 22 | `04-Senior-MAANG/JavaScript-System-Design-For-Frontend-And-Node.md` | Client/server JS architecture, SSR/CSR, caching, API gateway patterns, scalability |

Senior target:

- You can debug JavaScript as a runtime, not just write syntax.
- You can reason about latency, responsiveness, memory growth, CPU saturation, network
  waterfalls, bundle cost, runtime constraints, and production signals.
- You can explain frontend and Node.js incidents using evidence: metrics, logs, traces,
  profiles, heap snapshots, source maps, and code paths.
- You can discuss modern architecture trade-offs across browser, server, edge, serverless,
  and API layers.

---

## 5. Scenario Practice Path

Use these after the concept sheets. They train fast spoken answers and hands-on interview
solutions.

| Order | File | What It Builds |
|---:|---|---|
| 23 | `05-Scenario-Practice/JavaScript-Tricky-Output-Questions.md` | Coercion, hoisting, closure loops, `this`, async order, equality, prototype traps |
| 24 | `05-Scenario-Practice/JavaScript-Machine-Coding-Patterns.md` | Debounce, throttle, promise pool, event emitter, retry, memoize, LRU cache, pub-sub |
| 25 | `05-Scenario-Practice/JavaScript-Quick-Revision-And-Answer-Templates.md` | 30-60 second answers, scenario templates, final revision tables, confidence checklist |
| 26 | `05-Scenario-Practice/JavaScript-MAANG-Mock-Interview-Question-Bank.md` | Mock rounds, follow-up ladders, scoring rubrics, production depth checks, final drills |

Scenario target:

- You can solve output questions by execution rules.
- You can implement common JavaScript machine-coding utilities from scratch.
- You can answer quickly under pressure without losing production judgment.
- You can move from beginner wording to senior wording for the same topic.
- You can handle realistic mock rounds across language, browser, Node.js, security, testing,
  performance, observability, runtime trade-offs, and system design.

---

## 6. Practice Upgrade Path

Use this layer to turn reading into mastery.

| Order | File | What It Builds |
|---:|---|---|
| 27 | `06-Practice-Upgrade/JavaScript-2-Week-4-Week-Mastery-Roadmaps.md` | Beginner-to-pro study schedule, checkpoints, daily practice, interview readiness gates |
| 28 | `06-Practice-Upgrade/JavaScript-Active-Recall-Question-Bank.md` | Spaced recall questions across fundamentals, async, browser, Node, security, performance |
| 29 | `06-Practice-Upgrade/JavaScript-Runnable-Mini-Labs.md` | Runnable labs for event loop, cancellation, promise pools, event emitters, streams, leaks, caching |

Practice target:

- You can study the track in either a compressed 2-week path or a deeper 4-week path.
- You can test memory using active recall instead of rereading passively.
- You can run and modify small programs so tricky behavior becomes visible.
- You can measure readiness with checkpoints instead of vibes.

---

## 7. Capstone

Finish with one end-to-end system.

| Order | File | What It Builds |
|---:|---|---|
| 30 | `07-Capstone/JavaScript-FullStack-Production-Capstone.md` | Full-stack booking platform design connecting UI, TypeScript, Node, security, testing, performance, observability, deployment |

Capstone target:

- You can connect a user click to browser runtime, network behavior, Node runtime, external
  dependencies, logs, metrics, traces, source maps, and incident response.
- You can defend production decisions instead of only naming JavaScript features.
- You can produce a MAANG-style final answer with architecture, trade-offs, failures,
  testing, and operations.

---

## 8. Interview Answer Pattern

Use this structure for most JavaScript answers:

1. Give a crisp definition.
2. Explain why the feature exists.
3. Explain how it works internally or at runtime.
4. Give a small code example.
5. Mention the trap.
6. Mention production judgment.
7. Close with a trade-off or when-not-to-use statement.

Example:

```text
A closure is when a function remembers variables from its lexical scope even after the outer
function has finished executing. It is useful for callbacks, encapsulation, function factories,
and async code. The trap is that closures can also retain memory unintentionally if they keep
large objects reachable. In production, I use closures carefully for state, but I avoid hidden
shared mutable state when it makes behavior hard to reason about.
```

---

## 9. What A MAANG-Level JavaScript Learner Should Master

### Setup And Tooling

- Node.js release lines: Current vs Active LTS vs Maintenance LTS.
- Runtime pinning using `.nvmrc`, `.node-version`, Volta, asdf, or Docker.
- `package.json` scripts and the `packageManager` field.
- npm, pnpm, Yarn, lockfiles, and reproducible installs.
- ESM vs CommonJS project decisions.
- Environment variables and secret handling.
- Lint, format, test, build, and CI command basics.
- Browser target vs Node target vs library target.
- Cross-links to build tooling and package publishing tracks.

### Language Fundamentals

- Primitive vs reference values.
- `var`, `let`, and `const`.
- Scope and lexical environment.
- Hoisting and temporal dead zone.
- Closures and memory retention.
- Functions, arrow functions, and default parameters.
- Equality: `==`, `===`, `Object.is`.
- Type coercion rules.
- Truthy and falsy values.
- Error handling and custom errors.

### Objects And Prototypes

- Object property access.
- Prototype chain.
- `this` binding rules.
- `call`, `apply`, and `bind`.
- Constructor functions and `new`.
- Classes as syntax over prototypes.
- Inheritance trade-offs.
- Property descriptors.
- Shallow copy vs deep copy.
- Immutability and structural sharing awareness.

### Modern ECMAScript

- Stable modern features and runtime support checks.
- Modules, dynamic import, and top-level await.
- Optional chaining and nullish coalescing.
- Private fields and class features.
- Immutable array helpers such as `toSorted`.
- `Object.groupBy` and `Map.groupBy` with compatibility awareness.
- TC39 stages and why proposal stage does not equal production readiness.
- Polyfill vs transpile vs cannot-polyfill distinctions.

### Async JavaScript

- Call stack.
- Event loop.
- Microtask queue.
- Macrotask/task queue.
- Promise states.
- `then`, `catch`, `finally`.
- `async` / `await` desugaring mental model.
- Timer behavior.
- Fetch and cancellation.
- Race conditions.
- Promise combinators: `all`, `allSettled`, `race`, `any`.
- Unhandled rejections.

### Browser And Frontend Runtime

- DOM selection and mutation.
- Event propagation: capture, target, bubble.
- Event delegation.
- Browser storage: cookies, localStorage, sessionStorage, IndexedDB awareness.
- Fetch, CORS, and preflight.
- Rendering pipeline basics.
- Reflow, repaint, layout thrashing.
- Debounce and throttle.
- Network waterfalls.
- Web workers and service workers.
- Accessibility, keyboard support, focus management, and ARIA judgment.
- Internationalization using `Intl`, locale-aware formatting, and bidi awareness.
- Web Components and Shadow DOM trade-offs.

### React And Framework Integration

- Hooks and stale closure traps.
- Dependency arrays and effect cleanup.
- Memoization cost vs benefit.
- Concurrent rendering awareness.
- Suspense and streaming SSR.
- Hydration mismatch diagnosis.
- Server Components awareness.
- Relationship between JavaScript runtime rules and framework behavior.
- Cross-link to the dedicated React/NextJS track for deeper framework mastery.

### Node.js Backend

- Node.js event loop phases.
- Non-blocking IO.
- CommonJS vs ES modules.
- Streams and backpressure.
- Buffers.
- File system and network APIs.
- `process`, environment variables, and signals.
- Worker threads and child processes.
- Clustering and horizontal scaling.
- Timeouts, retries, idempotency.
- Structured logging and correlation IDs.
- Dependency and package management.

### Runtime And Platform Judgment

- Browser vs Node.js vs edge runtime vs serverless runtime.
- Deno and Bun awareness without hype.
- Cold starts, execution limits, API support, and portability.
- Filesystem, networking, crypto, and native dependency constraints.
- Runtime-specific debugging and deployment differences.
- Choosing runtime based on workload, not trend.

### TypeScript

- Type annotations.
- Interfaces vs type aliases.
- Union and intersection types.
- Type narrowing.
- Generics.
- Utility types.
- Discriminated unions.
- `unknown` vs `any`.
- `never` and exhaustive checks.
- Type-safe API boundaries.
- Runtime validation gap.

### Testing

- Unit, integration, component, E2E, and contract tests.
- Jest/Vitest basics.
- Mocking and spies.
- Fake timers.
- Testing async code.
- DOM/component testing awareness.
- Playwright for browser flows.
- Real dependency tests for backend when risk justifies it.
- Flaky test diagnosis.
- Coverage judgment.

### Security

- XSS.
- CSRF.
- Prototype pollution.
- npm supply-chain risks.
- JWT storage mistakes.
- Cookie flags: `HttpOnly`, `Secure`, `SameSite`.
- Input validation.
- Output encoding.
- Injection risks.
- Secret management.
- Dependency scanning.

### Performance And Debugging

- Browser DevTools.
- Node inspector.
- Heap snapshots.
- Performance profiles.
- Event-loop lag.
- Memory leaks through closures/listeners/caches.
- API latency debugging.
- Bundle size diagnosis.
- Source maps.
- Logging/tracing correlation.
- Incident response communication.

### Observability And SRE

- Structured logs with request IDs.
- Metrics: latency, error rate, throughput, saturation.
- Traces across browser, BFF, services, and dependencies.
- Real user monitoring.
- Error grouping and release metadata.
- Source map security and upload strategy.
- SLOs, alert thresholds, dashboards, and runbooks.
- Incident timeline and rollback communication.

### Special Interview Skills

- Tricky output reasoning.
- Async output ordering.
- Closure loop traps.
- `this` binding traps.
- Coercion and equality traps.
- Prototype chain explanation.
- Machine-coding utilities.
- Frontend scenario debugging.
- Node.js production incident reasoning.
- Runtime choice defense.
- Observability-driven debugging.
- End-to-end capstone design.

---

## 10. Adjacent Tracks To Use With JavaScript

Some JavaScript mastery lives in neighboring folders. Use these when you need deeper
coverage than a JavaScript interview sheet should contain.

| Adjacent Track | Use It For |
|---|---|
| `Marriot/BuildTools/Frontend-Build-Tools-Bundling-Interview-Track-Index.md` | Vite, Webpack, Rollup, esbuild, Babel/SWC, bundling, tree shaking, source maps |
| `Marriot/BackendBuildTools/04-NodeJS-Builds/NodeJS-Package-Managers-npm-yarn-pnpm-Gold-Sheet.md` | npm vs Yarn vs pnpm, lockfiles, installs, workspaces, package manager decisions |
| `Marriot/BackendBuildTools/04-NodeJS-Builds/NodeJS-Build-Runtime-Serve-Scale-Logs-Gold-Sheet.md` | Node build, runtime, serving, scaling, logs, deployment operations |
| `Marriot/ReactNextJS/React-NextJS-Interview-Track-Index.md` | Deep React, Next.js, app router, rendering, hydration, framework interviews |
| `Debugging/02-VSCode-Frontend-NodeJS/` | Practical VS Code browser and Node.js debugging workflows |
| `Debugging/05-Concurrency-Threading/24-NodeJS-EventLoop-Worker-Threads-Cluster-Debug.md` | Deeper Node event loop, worker threads, cluster, concurrency debugging |
| `Sources/NodeJS/NodeJS-Complete-Refresher.md` | Broader Node.js refresher material |

---

## 11. Two-Week Mastery Shortcut

Use this when an interview is close.

| Day Range | Focus |
|---|---|
| Days 1-2 | Setup, core language, execution context, closures |
| Days 3-4 | `this`, prototypes, arrays/objects, modern ES |
| Days 5-6 | Async, event loop, promises, TypeScript |
| Days 7-8 | Browser DOM, storage, workers, accessibility, i18n |
| Days 9-10 | Node.js, security, testing |
| Days 11-12 | Performance, observability, runtime/platform judgment |
| Days 13-14 | Machine coding, mock rounds, capstone explanation |

For the full plan, use `06-Practice-Upgrade/JavaScript-2-Week-4-Week-Mastery-Roadmaps.md`.

---

## 12. One-Day Revision Plan

### Hours 1-2

- Setup sheet.
- JavaScript core master sheet.
- Execution context, scope, and closures.
- `this`, prototypes, and classes.

### Hours 3-4

- Async JavaScript and event loop.
- Arrays, objects, and functional patterns.
- Modern ES features.
- Current ECMAScript and platform update.

### Hours 5-6

- Browser DOM and Web APIs.
- Accessibility, i18n, and Web Components.
- TypeScript for production safety.
- Node.js backend production sheet.

### Hours 7-8

- Performance, memory, and debugging.
- Observability and SRE.
- Security best practices.
- Testing patterns.
- Tricky output questions.
- Machine-coding patterns.
- Mock interview question bank.
- Capstone architecture answer aloud.

---

## 13. Final Confidence Checklist

You are ready when you can answer these without notes:

- How do you set up a reproducible JavaScript project?
- Why should production apps usually use Node.js LTS rather than Current?
- What are JavaScript primitive and reference types?
- Why is `typeof null` equal to `object`?
- Difference between `var`, `let`, and `const`?
- What is hoisting?
- What is temporal dead zone?
- What is a closure?
- How can closures cause memory leaks?
- How does `this` get its value?
- Arrow function `this` vs normal function `this`?
- How does the prototype chain work?
- What happens when you call a function with `new`?
- What are classes in JavaScript internally?
- Difference between `==`, `===`, and `Object.is`?
- How does type coercion work in common traps?
- Difference between shallow copy and deep copy?
- `map` vs `forEach`?
- `map` vs `flatMap`?
- How does `reduce` work and when should you avoid it?
- What is the event loop?
- Microtask vs macrotask?
- Why does Promise callback run before `setTimeout`?
- How does `async` / `await` work internally?
- What is an unhandled promise rejection?
- `Promise.all` vs `Promise.allSettled` vs `Promise.race` vs `Promise.any`?
- How do you prevent async race conditions?
- What is event delegation?
- Capturing vs bubbling?
- What causes layout thrashing?
- How do debounce and throttle differ?
- How does fetch handle HTTP errors?
- What is CORS and preflight?
- Why can localStorage be risky for tokens?
- How do you make a JavaScript UI keyboard-accessible?
- When is ARIA useful and when is it harmful?
- How does `Intl` help with localization?
- What are Web Components and Shadow DOM trade-offs?
- How does Node.js handle many concurrent requests?
- What blocks the Node.js event loop?
- What are streams and backpressure?
- When would you use worker threads?
- CommonJS vs ES modules?
- How do browser, Node.js, edge, serverless, Deno, and Bun runtime constraints differ?
- How do you debug high CPU in Node.js?
- How do you debug a JavaScript memory leak?
- How do you profile frontend performance?
- What is prototype pollution?
- How do XSS and CSRF differ?
- How do TypeScript types improve safety?
- Why is runtime validation still needed with TypeScript?
- How do you test async JavaScript?
- When do you use fake timers?
- What JavaScript signals should appear on a production dashboard?
- How do logs, metrics, traces, RUM, source maps, and SLOs fit together?
- How do you implement debounce from scratch?
- How do you implement throttle from scratch?
- How do you implement a promise pool?
- How do you implement an event emitter?
- Can you solve JavaScript output traps without guessing?
- Can you explain the capstone from browser click to backend trace and rollback?

---

## 14. Mastery Coverage Map

| Level | What This Track Covers | Status |
|---|---|---|
| Beginner | Setup, syntax-adjacent fundamentals, values, variables, functions, objects, arrays | Covered |
| Intermediate | Closures, prototypes, async, DOM, TypeScript, browser APIs, a11y, i18n, Web Components | Covered |
| Backend | Node runtime, APIs, streams, backpressure, security, testing, dependency hygiene | Covered |
| Senior | Runtime selection, performance, memory, debugging, observability, platform constraints | Covered |
| MAANG | Production incidents, async ordering, machine coding, system scenarios, capstone explanation | Covered |

What makes this one-stop:

- Setup is included, so beginners are not dropped into syntax without a working project.
- Every major JavaScript interview area has a dedicated sheet.
- The index gives a learning order instead of random notes.
- Advanced topics include traps, trade-offs, and production judgment.
- Browser and Node.js are both covered because JavaScript interviews often cross runtime boundaries.
- Accessibility, i18n, Web Components, modern runtimes, and observability are now first-class.
- TypeScript is included because modern production JavaScript is rarely purely untyped.
- Scenario sheets train fast answers, output reasoning, and hands-on coding utilities.
- Practice upgrades add roadmaps, active recall, runnable labs, and a production capstone.
- Adjacent build, React, Node, and debugging tracks are linked so deeper study has a path.

---

## 15. Official Source Notes

Use these sources when refreshing modern JavaScript, TypeScript, browser, and Node.js details:

- ECMAScript specification: `https://tc39.es/ecma262/`
- TC39 proposals: `https://github.com/tc39/proposals`
- MDN JavaScript reference: `https://developer.mozilla.org/en-US/docs/Web/JavaScript`
- MDN Web APIs: `https://developer.mozilla.org/en-US/docs/Web/API`
- HTML event loop specification: `https://html.spec.whatwg.org/`
- Node.js documentation: `https://nodejs.org/api/`
- Node.js release schedule: `https://nodejs.org/en/about/previous-releases`
- TypeScript handbook: `https://www.typescriptlang.org/docs/`
- Web.dev performance guidance: `https://web.dev/learn/performance/`
- OWASP Cheat Sheet Series: `https://cheatsheetseries.owasp.org/`
- OpenTelemetry JavaScript: `https://opentelemetry.io/docs/languages/js/`
- Jest documentation: `https://jestjs.io/docs/getting-started`
- Vitest documentation: `https://vitest.dev/guide/`
- Playwright documentation: `https://playwright.dev/docs/intro`

Interview safety line:

```text
I separate JavaScript language rules from browser runtime behavior, Node.js runtime behavior,
edge/serverless runtime constraints, and TypeScript compile-time checks. Before recommending
a feature for production, I check runtime support, build tooling, framework behavior, security
impact, operational risk, and rollback strategy.
```
