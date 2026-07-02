# JavaScript Active Recall Question Bank

> Track: JavaScript Interview Track - Practice Upgrade  
> Mode: answer aloud before checking notes.

---

## 1. Setup And Tooling

1. How do you pin Node.js for a project?
2. Why should production apps usually use Node LTS?
3. npm install vs npm ci?
4. Why commit a lockfile?
5. What does the `packageManager` field do?
6. How does Corepack help?
7. ESM vs CommonJS?
8. Why can frontend env vars leak secrets?
9. What scripts should a serious JS project have?
10. What belongs in a first JavaScript project README?

---

## 2. Core JavaScript

1. Primitive vs reference values?
2. Why is `typeof null` `"object"`?
3. `var` vs `let` vs `const`?
4. What is TDZ?
5. What is lexical scope?
6. What is a closure?
7. How can a closure leak memory?
8. How does `this` get its value?
9. Arrow `this` vs normal function `this`?
10. How does prototype lookup work?
11. What does `new` do?
12. What are property descriptors?
13. `==` vs `===` vs `Object.is`?
14. Shallow copy vs deep copy?
15. When should you avoid clever `reduce`?

---

## 3. Modern ECMAScript

1. Optional chaining trap?
2. `??` vs `||`?
3. Spread is shallow: what does that mean?
4. Top-level await trade-offs?
5. Dynamic import use cases?
6. `Map` vs object?
7. `WeakMap` use cases?
8. `structuredClone` limits?
9. Non-mutating array copy methods?
10. How do you decide whether to adopt a new TC39 feature?

---

## 4. Async And Browser Runtime

1. Call stack vs task queue vs microtask queue?
2. Why do promise callbacks run before timers?
3. Promise executor sync or async?
4. `Promise.all` vs `allSettled`?
5. `Promise.race` vs `any`?
6. Why is async `forEach` dangerous?
7. How do you cancel fetch?
8. How do you prevent stale search responses?
9. Debounce vs throttle?
10. How do long tasks hurt INP?

---

## 5. DOM, Web APIs, A11y, I18n

1. DOM vs BOM?
2. event target vs currentTarget?
3. capture vs bubble?
4. event delegation benefits?
5. Why does fetch not reject on HTTP 404?
6. What is CORS preflight?
7. localStorage vs sessionStorage vs IndexedDB?
8. Why prefer semantic HTML?
9. What does focus management mean?
10. ARIA trap?
11. How do you format currency for a locale?
12. What are Custom Elements and Shadow DOM?

---

## 6. TypeScript

1. `any` vs `unknown`?
2. type alias vs interface?
3. union vs intersection?
4. narrowing?
5. discriminated union?
6. `never` and exhaustive checks?
7. generics?
8. conditional and mapped types?
9. runtime validation gap?
10. tsconfig settings that matter in production?

---

## 7. Node.js

1. Why is Node good for IO-heavy systems?
2. What blocks the event loop?
3. What are event-loop phases?
4. What is libuv?
5. What are streams?
6. What is backpressure?
7. When use worker threads?
8. child process vs worker thread?
9. cluster vs replicas?
10. graceful shutdown?
11. health check vs readiness?
12. AsyncLocalStorage?
13. diagnostics_channel?
14. How do you make retries safe?
15. How do you design idempotency?

---

## 8. Security And Supply Chain

1. XSS types?
2. CSP and Trusted Types?
3. CSRF and SameSite?
4. localStorage token risk?
5. JWT verification mistakes?
6. prototype pollution?
7. SQL vs NoSQL injection?
8. SSRF?
9. dependency confusion?
10. npm audit nuance?
11. secret logging risk?
12. ReDoS?

---

## 9. Testing

1. Unit vs integration vs E2E?
2. Jest vs Vitest?
3. node:test use case?
4. fake timers?
5. testing async code?
6. mocking vs dependency injection?
7. Playwright use cases?
8. contract tests?
9. accessibility testing?
10. performance budgets in CI?

---

## 10. Performance Debugging Observability

1. How debug a memory leak?
2. heap snapshot vs CPU profile?
3. event-loop delay?
4. RSS vs heap?
5. Core Web Vitals?
6. LCP root causes?
7. INP root causes?
8. bundle size vs runtime cost?
9. source maps in production?
10. logs vs metrics vs traces?
11. RUM?
12. high-cardinality metric labels?

---

## 11. Runtime And Architecture

1. Browser vs Node vs edge?
2. When use edge runtime?
3. Why can edge fail with Node APIs?
4. Deno/Bun adoption checklist?
5. SSR vs CSR vs SSG?
6. BFF pattern?
7. WebSocket vs SSE?
8. offline-first trade-offs?
9. optimistic UI trade-offs?
10. frontend system design answer pattern?

---

## 12. Final Readiness Gate

You are ready when you can answer without notes:

1. Explain JavaScript execution, scope, closures, `this`, prototypes, and async ordering.
2. Build a small project with pinned Node, package manager, lockfile, scripts, tests.
3. Design browser UI with fetch cancellation, accessibility, i18n, and performance awareness.
4. Build a Node API with validation, auth, idempotency, timeouts, retries, and observability.
5. Debug p99 latency, event-loop lag, memory leak, bad bundle, XSS, and stale service worker.
6. Choose browser vs Node vs edge vs serverless intentionally.
7. Explain the capstone request lifecycle end to end.

