# JavaScript Interview Track Index

This folder is the JavaScript language and runtime track for frontend, backend Node.js, full-stack, and MAANG-style interviews.

Goal:
- Build JavaScript from beginner fundamentals to senior production judgment.
- Keep each topic modular so revision is easy.
- Make the answer pattern repeatable: mental model, definition, internals, code, traps, production judgment, scenario answer, revision.
- Cover both browser JavaScript and Node.js because modern interviews often test language behavior, runtime behavior, and production failure modes together.

Use this index as the reading order.

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
- You can explain JavaScript types, scope, closures, `this`, prototypes, and basic async behavior without guessing.
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
| 9 | `02-Intermediate-Frontend-FullStack/JavaScript-React-Integration-Hooks-Concurrent-Rendering-Master-Sheet.md` | React hooks (useState/useEffect/useCallback/useMemo/useRef), concurrent rendering, useTransition, Suspense, streaming SSR, hydration, RSC |
| 10 | `02-Intermediate-Frontend-FullStack/JavaScript-Frontend-Interview-Scenarios.md` | UI race conditions, forms, state bugs, debounce/throttle, rendering and performance traps |

Intermediate target:
- You can explain how browser JavaScript responds to user events, network calls, timers, and rendering work.
- You can reason about async ordering, race conditions, stale state, and UI responsiveness.
- You can use TypeScript to prevent common production bugs without overcomplicating code.
- You can connect JavaScript language behavior to actual frontend user experience.

---

## 3. Backend Node.js Path

Use this path if you are preparing for Node.js backend, full-stack, platform, or production engineering interviews.

| Order | File | What It Builds |
|---:|---|---|
| 10 | `03-Backend-NodeJS/JavaScript-NodeJS-Backend-Production-Master-Sheet.md` | Node runtime, CommonJS/ESM, APIs, streams, buffers, event-loop blocking, worker threads |
| 11 | `03-Backend-NodeJS/JavaScript-NodeJS-Interview-Scenarios.md` | API latency, blocked event loop, retries, queues, streams, backpressure, memory leaks |
| 12 | `03-Backend-NodeJS/JavaScript-Security-Best-Practices-Master-Sheet.md` | XSS, CSRF, prototype pollution, JWT/storage, npm supply chain, injection risks |
| 13 | `03-Backend-NodeJS/JavaScript-Testing-Patterns-Master-Sheet.md` | Jest/Vitest, mocks, fake timers, integration tests, Playwright, contract tests |

Backend target:
- You can explain why Node.js is efficient for IO-heavy workloads and weak for CPU-heavy work unless designed carefully.
- You can identify event-loop blocking, unbounded concurrency, missing backpressure, memory leaks, and retry storms.
- You can build safe backend JavaScript with validation, timeouts, idempotency, logging, dependency hygiene, and tests.
- You can explain when to use streams, worker threads, queues, caching, and external systems.

---

## 4. Senior / MAANG Path

These are the pro sheets.

| Order | File | What It Builds |
|---:|---|---|
| 14 | `04-Senior-MAANG/JavaScript-Performance-Memory-Debugging-Master-Sheet.md` | Memory leaks, profiling, DevTools, heap snapshots, event-loop lag, bundle/runtime cost |
| 15 | `04-Senior-MAANG/JavaScript-Production-Debugging-Case-Studies.md` | High CPU, memory leak, blocked event loop, API storm, bad bundle, async failures |
| 16 | `04-Senior-MAANG/JavaScript-System-Design-For-Frontend-And-Node.md` | Client/server JS architecture, SSR/CSR, caching, API gateway patterns, scalability |

Senior target:
- You can debug JavaScript as a runtime, not just write syntax.
- You can reason about latency, responsiveness, memory growth, CPU saturation, network waterfalls, and bundle cost.
- You can explain frontend and Node.js production incidents using evidence: metrics, logs, traces, profiles, heap snapshots, and code paths.
- You can discuss modern architecture trade-offs across browser, server, edge, and API layers.

---

## 5. Scenario Practice Path

Use these after the concept sheets. They train fast spoken answers and hands-on interview solutions.

| Order | File | What It Builds |
|---:|---|---|
| 17 | `05-Scenario-Practice/JavaScript-Tricky-Output-Questions.md` | Coercion, hoisting, closure loops, `this`, async order, equality, prototype traps |
| 18 | `05-Scenario-Practice/JavaScript-Machine-Coding-Patterns.md` | Debounce, throttle, promise pool, event emitter, retry, memoize, LRU cache, pub-sub |
| 19 | `05-Scenario-Practice/JavaScript-Quick-Revision-And-Answer-Templates.md` | 30-60 second answers, scenario templates, final revision tables, confidence checklist |
| 20 | `05-Scenario-Practice/JavaScript-MAANG-Mock-Interview-Question-Bank.md` | Full mock rounds, follow-up ladders, scoring rubrics, production depth checks, final drills |

Scenario target:
- You can solve output questions by execution rules.
- You can implement common JavaScript machine-coding utilities from scratch.
- You can answer quickly under pressure without losing production judgment.
- You can move from beginner wording to senior wording for the same topic.
- You can handle realistic mock rounds with follow-ups across language, browser, Node.js, security, testing, performance, and system design.

---

## 6. Interview Answer Pattern

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

## 7. What A MAANG-Level JavaScript Learner Should Master

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
- Web workers.
- Accessibility and user-perceived performance awareness.

### Node.js Backend

- Node.js event loop phases.
- Non-blocking IO.
- CommonJS vs ES modules.
- Streams and backpressure.
- Buffers.
- File system and network APIs.
- `process`, environment variables, signals.
- Worker threads and child processes.
- Clustering and horizontal scaling.
- Timeouts, retries, idempotency.
- Structured logging and correlation IDs.
- Dependency and package management.

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
- Testcontainers or real dependency tests for backend.
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

### Production Debugging

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

---

## 8. One-Day Revision Plan

### Hours 1-2

- JavaScript core master sheet.
- Execution context, scope, and closures.
- `this`, prototypes, and classes.

### Hours 3-4

- Async JavaScript and event loop.
- Arrays, objects, and functional patterns.
- Modern ES features.

### Hours 5-6

- Browser DOM and Web APIs.
- TypeScript for production safety.
- Node.js backend production sheet.

### Hours 7-8

- Performance, memory, and debugging.
- Security best practices.
- Testing patterns.
- Tricky output questions.
- Machine-coding patterns.
- Quick revision and answer templates.
- MAANG mock interview question bank.
- Practice strong answers aloud.

---

## 9. Final Confidence Checklist

You are ready when you can answer these without notes:

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
- How does Node.js handle many concurrent requests?
- What blocks the Node.js event loop?
- What are streams and backpressure?
- When would you use worker threads?
- CommonJS vs ES modules?
- How do you debug high CPU in Node.js?
- How do you debug a JavaScript memory leak?
- How do you profile frontend performance?
- What is prototype pollution?
- How do XSS and CSRF differ?
- How do TypeScript types improve safety?
- Why is runtime validation still needed with TypeScript?
- How do you test async JavaScript?
- When do you use fake timers?
- How do you implement debounce from scratch?
- How do you implement throttle from scratch?
- How do you implement a promise pool?
- How do you implement an event emitter?
- Can you solve JavaScript output traps without guessing?

---

## 10. Mastery Coverage Map

| Level | What This Track Covers | Status |
|---|---|---|
| Beginner | Syntax-adjacent fundamentals, values, variables, functions, objects, arrays | Complete path planned |
| Intermediate | Closures, prototypes, async, DOM, TypeScript, browser APIs | Complete path planned |
| Senior | Node.js runtime, performance, memory, security, testing, debugging | Complete path planned |
| MAANG | runtime trade-offs, production incidents, async ordering, machine coding, system scenarios | Complete path planned |

What makes this one-stop:

- Every major JavaScript interview area has a dedicated sheet.
- The index gives a learning order instead of random notes.
- Each advanced topic will include traps, trade-offs, and production judgment.
- Browser and Node.js are both covered because JavaScript interviews often cross runtime boundaries.
- TypeScript is included because modern production JavaScript is rarely purely untyped.
- Scenario sheets train fast answers, output reasoning, and hands-on coding utilities.
- Production sheets cover debugging, security, performance, and incident thinking.

---

## 11. Official Source Notes

Use these sources when refreshing modern JavaScript, TypeScript, browser, and Node.js details:

- ECMAScript specification: `https://tc39.es/ecma262/`
- TC39 proposals: `https://github.com/tc39/proposals`
- MDN JavaScript reference: `https://developer.mozilla.org/en-US/docs/Web/JavaScript`
- MDN Web APIs: `https://developer.mozilla.org/en-US/docs/Web/API`
- HTML event loop specification: `https://html.spec.whatwg.org/`
- Node.js documentation: `https://nodejs.org/api/`
- TypeScript handbook: `https://www.typescriptlang.org/docs/`
- Web.dev performance guidance: `https://web.dev/learn/performance/`
- OWASP Cheat Sheet Series: `https://cheatsheetseries.owasp.org/`
- Jest documentation: `https://jestjs.io/docs/getting-started`
- Vitest documentation: `https://vitest.dev/guide/`
- Playwright documentation: `https://playwright.dev/docs/intro`

Interview safety line:

```text
I separate JavaScript language rules from browser runtime behavior, Node.js runtime behavior,
and TypeScript compile-time checks. Before recommending a feature for production, I check
runtime support, build tooling, framework behavior, security impact, and operational risk.
```
