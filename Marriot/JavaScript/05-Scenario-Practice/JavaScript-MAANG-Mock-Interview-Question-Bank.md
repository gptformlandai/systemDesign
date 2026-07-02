# JavaScript MAANG Mock Interview Question Bank

> Goal: practice complete JavaScript interview rounds with realistic questions, follow-ups, scoring rubrics, strong answer checkpoints, production depth, and final drills.

---

## 1. How To Use This Question Bank

This file is the mock interview layer after the concept sheets, scenario sheets, machine-coding workbook, and quick revision sheet.

Use it in four passes:

| Pass | Goal | How To Practice |
|---|---|---|
| Pass 1 | identify gaps | answer each question in 60 seconds without notes |
| Pass 2 | deepen | add internals, code example, trap, and production line |
| Pass 3 | pressure | run timed mock rounds with no pausing |
| Pass 4 | polish | compare against strong-answer checkpoints and scoring rubric |

Interview rule:

```text
Do not memorize paragraphs. Memorize mental models, then speak naturally with examples and trade-offs.
```

---

## 2. Universal Scoring Rubric

Score each answer from 1 to 5.

| Score | Meaning | Signal |
|---:|---|---|
| 1 | incorrect | confuses concept or gives harmful advice |
| 2 | shallow | definition only, no mechanism or edge case |
| 3 | solid | correct definition plus basic example |
| 4 | strong | includes mechanism, trap, and trade-off |
| 5 | senior | connects internals to production failure mode and design judgment |

A MAANG-ready answer usually has:

- crisp definition,
- why it exists,
- runtime/internal mechanism,
- code or concrete example,
- common trap,
- production impact,
- trade-off or when-not-to-use.

---

## 3. Mock Round Formats

### 30-Minute Screening Round

| Time | Section |
|---:|---|
| 5 min | core language quick questions |
| 10 min | async/event loop output question |
| 10 min | machine-coding utility |
| 5 min | production follow-ups |

### 60-Minute Frontend Round

| Time | Section |
|---:|---|
| 10 min | JS fundamentals |
| 10 min | browser/DOM/events |
| 15 min | async race or UI scenario |
| 15 min | performance/security scenario |
| 10 min | trade-offs and testing |

### 60-Minute Node.js Round

| Time | Section |
|---:|---|
| 10 min | Node runtime and event loop |
| 15 min | API/backend production scenario |
| 15 min | streams/queues/retries |
| 10 min | debugging incident |
| 10 min | security/testing/observability |

### 90-Minute MAANG Loop Round

| Time | Section |
|---:|---|
| 15 min | core JS deep dive |
| 20 min | machine coding |
| 20 min | production debugging |
| 20 min | frontend or Node system design |
| 15 min | follow-up ladder and trade-offs |

---

## 4. Answer Quality Ladder

When practicing, climb the ladder.

### Level 1: Definition

```text
A closure is a function that remembers variables from outside.
```

### Level 2: Mechanism

```text
A closure retains references to lexical bindings from the environment where the function was created.
```

### Level 3: Example

```text
A counter factory returns an inner function that keeps access to count after the outer function returns.
```

### Level 4: Trap

```text
Closures can accidentally retain large objects or create loop bugs with var.
```

### Level 5: Production Judgment

```text
In production, I clean up long-lived callbacks/listeners and avoid retaining heavy state through closures.
```

---

## 5. Core Language Round

### Q1. Explain JavaScript primitive vs reference values.

Strong answer must include:

- primitives are immutable values,
- objects are reference values,
- assignment of object variable copies reference,
- equality differs for primitives vs objects,
- shallow copies preserve nested references.

Follow-ups:

- Why does `{}` === `{}` return false?
- What happens when spreading an object with nested fields?
- How would you safely update nested state?

Weak answer:

```text
Primitive values are simple and reference values are objects.
```

Strong answer:

```text
Primitive values like string, number, boolean, bigint, symbol, null, and undefined are compared by value. Objects, arrays, and functions are reference values, so variables hold references to heap objects. Assigning an object variable copies the reference, not the object. That is why two object literals are not equal by strict equality, and why shallow copies still share nested objects. In production state updates, I avoid mutating shared references accidentally.
```

Scoring:

- 3: lists primitive and object types.
- 4: explains reference assignment and equality.
- 5: connects shallow copy to real state mutation bugs.

---

### Q2. Why is `typeof null` equal to `object`?

Strong answer must include:

- historical JavaScript bug,
- preserved for backward compatibility,
- null is still a primitive,
- use `value === null` to check null.

Strong answer:

```text
typeof null returns object because of a historical implementation bug in early JavaScript. It was preserved for backward compatibility. Conceptually, null is a primitive value representing intentional absence, not an object. In production I check null explicitly with value === null, or use value == null intentionally only when I want to match both null and undefined.
```

Follow-ups:

- Difference between null and undefined?
- When is `value == null` acceptable?

---

### Q3. Explain `var`, `let`, and `const`.

Strong answer must include:

- `var` function scope,
- `let` and `const` block scope,
- hoisting behavior,
- TDZ,
- const binding vs object mutation,
- modern default: const, then let, avoid var.

Strong answer:

```text
var is function-scoped and hoisted with undefined. let and const are block-scoped and hoisted too, but they are unavailable until initialized because of temporal dead zone. const prevents reassignment of the binding, not mutation of the referenced object. In modern code I default to const, use let for reassignment, and avoid var because function scope causes confusing bugs in loops and closures.
```

Follow-ups:

- Why does `let` throw before declaration?
- Can a const object be mutated?
- Why does var in a loop often cause async callback bugs?

---

### Q4. Explain hoisting and TDZ.

Strong answer must include:

- declarations processed before execution,
- var initialized as undefined,
- function declarations initialized,
- let/const/class hoisted but unavailable,
- TDZ prevents access before initialization.

Practice code:

```js
console.log(a);
var a = 1;

console.log(b);
let b = 2;
```

Expected:

```text
undefined
ReferenceError
```

Strong answer:

```text
Hoisting means JavaScript creates declaration bindings before executing statements in a scope. var bindings are initialized to undefined, so accessing them before assignment gives undefined. Function declarations are initialized with the function. let, const, and class declarations also get bindings, but they stay in the temporal dead zone until execution reaches the declaration, so early access throws ReferenceError.
```

---

### Q5. Explain closures.

Strong answer must include:

- lexical environment retention,
- function outlives outer call,
- not a copy of values,
- use cases,
- memory retention risk.

Code prompt:

```js
function createCounter() {
  let count = 0;
  return function increment() {
    count += 1;
    return count;
  };
}
```

Strong answer:

```text
A closure is when a function retains access to variables from its lexical environment after the outer function has returned. The inner function does not copy count; it keeps a reference to the binding. Closures power callbacks, factories, memoization, private state, and event handlers. The production trap is memory retention: a long-lived closure can keep large objects, DOM nodes, or stale state reachable.
```

Follow-ups:

- How can closures cause memory leaks?
- How do closures behave in loops with var vs let?
- How do closures support memoization?

---

### Q6. Explain `this` binding.

Strong answer must include:

- call-site rule,
- method call,
- plain call,
- `call/apply/bind`,
- `new`,
- arrow lexical this.

Strong answer:

```text
For normal functions, this is determined by how the function is called. In obj.fn(), this is obj. In a plain function call, strict mode gives undefined. call, apply, and bind explicitly set this. new creates a new object and binds this to it. Arrow functions do not create their own this; they capture lexical this from the surrounding scope.
```

Follow-ups:

- Why does a detached method lose this?
- Why should arrow functions not be used as prototype methods?
- How does bind differ from call?

---

### Q7. Explain prototypes.

Strong answer must include:

- property lookup delegation,
- prototype chain,
- constructor function prototype,
- class syntax over prototypes,
- shadowing.

Strong answer:

```text
Every ordinary JavaScript object has an internal prototype link. When a property is not found directly on the object, JavaScript looks up the prototype chain. Constructor functions use their prototype object for methods shared by instances. Class syntax is a cleaner way to create constructor and prototype relationships, but JavaScript inheritance is still delegation-based.
```

Follow-ups:

- Difference between `prototype` and `__proto__`?
- What happens with property shadowing?
- How does `Object.create` work?

---

### Q8. Explain classes in JavaScript.

Strong answer must include:

- syntax over prototypes,
- constructor initializes instance,
- methods on prototype,
- class body strict mode,
- class declarations not usable before declaration,
- methods not auto-bound.

Strong answer:

```text
JavaScript classes are syntax over prototype-based behavior. The constructor initializes instance fields, while methods are placed on the prototype and shared by instances. Class bodies run in strict mode. Classes make inheritance and object creation cleaner, but they do not turn JavaScript into classical class-copy inheritance. Class methods are also not auto-bound, so passing a method as a callback can lose this.
```

---

### Q9. Explain equality in JavaScript.

Strong answer must include:

- `==` coercion,
- `===` strict comparison,
- `Object.is`,
- `NaN`,
- `-0`,
- object references.

Strong answer:

```text
Double equals compares after coercion, which can produce surprising results. Triple equals avoids general type coercion and is the default choice in production. Object.is is similar to strict equality but treats NaN as equal to itself and distinguishes 0 from -0. Objects are compared by reference, not structure.
```

Follow-ups:

- Why is `NaN === NaN` false?
- When might `Object.is` matter?
- Is `value == null` always bad?

---

### Q10. Explain type coercion.

Strong answer must include:

- implicit conversion,
- ToPrimitive/ToNumber/ToString/ToBoolean idea,
- `+` special behavior,
- explicit conversion preferred.

Strong answer:

```text
Coercion is JavaScript converting values between types implicitly or explicitly. Operators trigger rules like ToPrimitive, ToNumber, ToString, and ToBoolean. The plus operator is tricky because it can perform string concatenation or numeric addition, while subtraction usually converts to numbers. In production, I avoid clever coercion and convert explicitly at boundaries.
```

Practice outputs:

```js
console.log("5" + 1);
console.log("5" - 1);
console.log([] == false);
console.log(Boolean([]));
```

---

## 6. Async And Event Loop Round

### Q11. Explain the event loop.

Strong answer must include:

- call stack,
- runtime APIs,
- task queue,
- microtask queue,
- promise callbacks before timers,
- rendering/browser or phases/Node awareness,
- blocking sync code.

Strong answer:

```text
JavaScript executes synchronous code on the call stack. Async work is handled by the runtime, such as browser Web APIs or Node/libuv. When async work is ready, callbacks are queued. Promise reactions go to the microtask queue, while timers and events go to task queues. After the current stack is empty, microtasks are drained before the next task, which is why Promise.then usually runs before setTimeout. Long synchronous work blocks the event loop and delays UI, timers, and IO callbacks.
```

Follow-ups:

- What is a microtask?
- Can microtasks starve rendering?
- How is Node event loop different from browser event loop?

---

### Q12. Predict the output.

```js
console.log("A");
setTimeout(() => console.log("B"), 0);
Promise.resolve().then(() => console.log("C"));
queueMicrotask(() => console.log("D"));
console.log("E");
```

Expected:

```text
A
E
C
D
B
```

Reason:

- sync first: A, E,
- promise and queueMicrotask are microtasks in scheduling order,
- timer runs after microtasks.

Strong spoken answer:

```text
I first run synchronous code, so A and E print. Then the microtask queue drains in the order callbacks were queued: C then D. The setTimeout callback is a task, so B runs after microtasks.
```

---

### Q13. Explain promises.

Strong answer must include:

- pending/fulfilled/rejected,
- settles once,
- executor runs synchronously,
- callbacks as microtasks,
- promises do not cancel work.

Strong answer:

```text
A Promise represents eventual completion or failure. It starts pending and settles once as fulfilled or rejected. The executor function runs synchronously when the promise is created, but then/catch/finally callbacks run asynchronously as microtasks. Promises compose async work, but they do not cancel underlying work by themselves.
```

Follow-ups:

- Why does Promise executor run before next log?
- What is an unhandled rejection?
- How do you cancel fetch?

---

### Q14. Explain async/await.

Strong answer must include:

- syntax over promises,
- async function always returns promise,
- await pauses async function only,
- resume as microtask,
- try/catch for awaited failures.

Strong answer:

```text
async/await is syntax over promises. An async function always returns a promise. await pauses that async function until the awaited promise settles, then resumes execution later through the microtask queue. It does not block the entire JavaScript thread. Errors from awaited promises can be handled with try/catch.
```

Trap question:

```js
async function getValue() {
  return 10;
}
console.log(getValue());
```

Expected:

```text
Promise
```

---

### Q15. Explain Promise combinators.

Strong answer must include:

- all,
- allSettled,
- race,
- any,
- preserve order,
- reject-fast behavior,
- concurrency caveat.

Strong answer:

```text
Promise.all waits for all promises to fulfill and returns results in input order, but rejects fast on first failure. allSettled waits for every promise and returns status objects, useful for partial success. race settles with the first fulfilled or rejected promise, often used for timeout wrappers. any resolves with the first fulfilled promise and rejects only if all fail. Promise.all starts all passed work, so it is not a concurrency limiter.
```

Follow-ups:

- Which would you use for optional widgets?
- Which would you use for fallback providers?
- How do you limit concurrency?

---

### Q16. What is wrong with async `forEach`?

Prompt:

```js
items.forEach(async item => {
  await save(item);
});
console.log("done");
```

Strong answer:

```text
forEach does not await async callbacks. It starts callbacks and immediately continues, so done can print before saves finish. For sequential work, use for...of with await. For parallel work, use Promise.all(items.map(save)). For limited concurrency, use a promise pool.
```

Correct sequential code:

```js
for (const item of items) {
  await save(item);
}
```

Correct parallel code:

```js
await Promise.all(items.map(item => save(item)));
```

---

### Q17. Explain stale async response bug.

Strong answer must include:

- request A starts,
- request B starts later,
- A resolves after B,
- stale A overwrites UI,
- fix with abort and request ID/latest-only guard.

Strong answer:

```text
A stale response bug happens when an older request resolves after a newer request and overwrites current UI state. Search autocomplete is a classic example. I fix it by debouncing input, aborting previous fetch where possible, and also using a latest request ID guard because cancellation is not guaranteed. Only the latest request can update state.
```

---

### Q18. Explain unhandled promise rejection.

Strong answer must include:

- rejection without handler,
- runtime warning/error behavior,
- await/return/catch,
- fire-and-forget catch,
- production logging.

Strong answer:

```text
An unhandled promise rejection happens when a promise rejects and no rejection handler is attached in time. In production this can hide failed async work or crash depending runtime policy. I return or await promises, use try/catch around awaited calls, attach catch to fire-and-forget work, and log failures with enough context.
```

---

## 7. Browser And Frontend Round

### Q19. Explain DOM and rendering cost.

Strong answer must include:

- DOM tree,
- JS mutation,
- style/layout/paint/composite,
- layout thrashing,
- batching,
- event delegation.

Strong answer:

```text
The DOM is the browser's object representation of the page. JavaScript can mutate it, but DOM changes can trigger style recalculation, layout, paint, and compositing. Repeated reads and writes can cause layout thrashing. In production I batch DOM updates, avoid unnecessary layout reads, use event delegation for large lists, and measure with DevTools performance traces.
```

---

### Q20. Explain event bubbling, capturing, and delegation.

Strong answer must include:

- capture phase,
- target phase,
- bubble phase,
- addEventListener capture option,
- delegation with closest,
- cleanup.

Strong answer:

```text
DOM events generally travel from root down during capture, run at the target, then bubble back up. Event delegation attaches one listener to a parent and handles events from matching children using bubbling. It reduces listener count and works for dynamic lists. In production I check target with closest and contains, and clean listeners on unmount.
```

---

### Q21. Explain fetch error handling.

Strong answer must include:

- fetch returns promise,
- network errors reject,
- HTTP 4xx/5xx do not reject,
- check `response.ok`,
- abort support,
- timeout wrapper.

Strong answer:

```text
fetch returns a promise for a Response. It rejects for network-level failures, but HTTP error statuses like 404 or 500 still resolve, so I check response.ok and throw if needed. For cancellation I use AbortController. For production calls I add timeout, retry only safe errors, and handle JSON parsing errors separately.
```

Code:

```js
async function getJson(url, signal) {
  const response = await fetch(url, { signal });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}
```

---

### Q22. Explain CORS.

Strong answer must include:

- browser security mechanism,
- origin = scheme/host/port,
- server opt-in headers,
- preflight,
- credentials + wildcard issue,
- not auth.

Strong answer:

```text
CORS is a browser-enforced mechanism controlling whether a page can read a response from another origin. The server opts in with headers like Access-Control-Allow-Origin. Non-simple requests may trigger a preflight OPTIONS request. If credentials are used, wildcard origins are not allowed. CORS is not authentication and does not protect the server from non-browser clients.
```

---

### Q23. Explain browser storage options.

Strong answer must include:

- localStorage synchronous/string-only,
- sessionStorage tab session,
- IndexedDB async structured storage,
- cookies request-bound,
- security trade-offs.

Strong answer:

```text
localStorage is persistent, synchronous, and string-only, so it is simple but can block and is exposed to XSS. sessionStorage is per tab session. IndexedDB is asynchronous and better for larger structured data. Cookies are sent automatically with requests and can be HttpOnly/Secure/SameSite, making them better for high-risk auth when CSRF is handled.
```

---

### Q24. How do you debug slow page load?

Strong answer must include:

- Core Web Vitals,
- waterfall,
- server time,
- JS bundle,
- render blocking,
- images/fonts,
- hydration/main thread,
- measure before/after.

Strong answer:

```text
I split slow page load into server response, network transfer, render-blocking resources, JavaScript bundle cost, image/font loading, and main-thread rendering or hydration. I check Core Web Vitals, DevTools waterfall, performance trace, bundle analyzer, and real-user monitoring. Then I fix the measured bottleneck and verify with before/after metrics.
```

---

### Q25. How do you debug input lag?

Strong answer must include:

- INP,
- long tasks,
- heavy event handlers,
- expensive render,
- layout thrash,
- debounce/throttle,
- worker,
- reduce render scope.

Strong answer:

```text
I inspect INP and performance traces for long tasks around the interaction. Common causes are heavy synchronous handlers, large rerenders, expensive validation, layout thrashing, or too much JavaScript on the main thread. I would split work, debounce noncritical tasks, memoize carefully, move CPU-heavy work to a worker, and reduce render scope.
```

---

### Q26. Explain hydration.

Strong answer must include:

- server HTML,
- client JS attaches behavior,
- hydration cost,
- mismatch,
- partial/progressive hydration mention if senior.

Strong answer:

```text
Hydration is when client JavaScript attaches event handlers and state to server-rendered HTML. It improves initial content visibility, but hydration can block the main thread and delay interactivity. Hydration mismatches happen when server and client render different markup. For large apps, partial hydration, islands, streaming, or server components can reduce client work depending framework.
```

---

## 8. Node.js Backend Round

### Q27. Explain Node.js runtime.

Strong answer must include:

- V8,
- libuv,
- event loop,
- non-blocking IO,
- good for IO-heavy,
- CPU-heavy blocks event loop.

Strong answer:

```text
Node.js is a JavaScript runtime built on V8 with Node APIs and libuv for event loop and async IO. It is strong for IO-heavy services because it can handle many concurrent operations without a thread per request. CPU-heavy JavaScript blocks the event loop, so for heavy computation I use worker threads, child processes, separate services, or move work outside the request path.
```

---

### Q28. What blocks the Node event loop?

Strong answer must include:

- sync CPU work,
- sync fs/crypto/compression,
- large JSON parse/stringify,
- regex backtracking,
- huge loops,
- excessive logging,
- profiling.

Strong answer:

```text
The event loop is blocked by synchronous JavaScript or sync native calls: huge loops, large JSON parse/stringify, regex backtracking, sync fs, crypto, compression, or expensive logging. When blocked, Node cannot process other callbacks promptly. I diagnose with event loop delay metrics and CPU profiles, then move heavy work to workers, stream data, reduce algorithmic complexity, or remove sync APIs from request paths.
```

---

### Q29. Explain streams and backpressure.

Strong answer must include:

- chunk processing,
- avoids buffering entire data,
- readable/writable/transform,
- backpressure signal,
- memory protection.

Strong answer:

```text
Streams process data chunk by chunk instead of loading everything into memory. They are useful for files, uploads, downloads, compression, and proxies. Backpressure means the consumer tells the producer to slow down when it cannot keep up. In Node writable streams, write can return false and drain signals when to continue. Ignoring backpressure causes memory growth.
```

---

### Q30. How do you design a production Express API?

Strong answer must include:

- thin routes,
- validation,
- auth/authz,
- centralized errors,
- async handler,
- logging/metrics/tracing,
- timeouts,
- rate limits,
- graceful shutdown,
- dependency boundaries.

Strong answer:

```text
I keep route handlers thin and validate input at the boundary. Business logic goes into services, database access into repositories/adapters. I add authentication and authorization explicitly, centralized error handling, structured logs with request IDs, metrics, tracing, outbound timeouts, rate limits, and graceful shutdown. I avoid blocking the event loop and make retries idempotent where writes are involved.
```

---

### Q31. Explain graceful shutdown in Node.

Strong answer must include:

- SIGTERM/SIGINT,
- stop accepting new requests,
- finish in-flight with timeout,
- close DB/queues,
- readiness/liveness,
- avoid abrupt termination.

Strong answer:

```text
Graceful shutdown means handling signals like SIGTERM, marking the process not ready, stopping new requests, allowing in-flight work to finish within a timeout, closing database pools, message consumers, and servers, then exiting. It prevents dropped requests, partial writes, and corrupted processing during deployments or autoscaling.
```

---

### Q32. How do you debug API latency in Node?

Strong answer must include:

- break down latency,
- service vs dependency vs DB,
- event loop lag,
- connection pool,
- logs/traces,
- recent deploys,
- mitigation.

Strong answer:

```text
I break latency into gateway, service handler, dependency calls, database time, queue time, and event loop delay. I check traces, structured logs, RED metrics, connection pool saturation, slow queries, dependency health, and recent deployments. Mitigation might be rollback, caching, load shedding, increasing pool capacity carefully, fixing slow queries, or adding timeouts.
```

---

## 9. Security Round

### Q33. Explain XSS and prevention.

Strong answer must include:

- attacker JS executes in trusted origin,
- source/sink/context,
- output encoding,
- avoid unsafe innerHTML,
- sanitization for rich HTML,
- CSP defense in depth.

Strong answer:

```text
XSS happens when attacker-controlled data is executed as JavaScript in a trusted page. I identify the source, sink, and output context. Prevention is context-aware output encoding, framework-safe rendering, avoiding unsafe innerHTML, sanitizing rich HTML with a proven sanitizer, and CSP as defense in depth. Input validation alone is not enough because output context determines exploitability.
```

---

### Q34. Explain CSRF.

Strong answer must include:

- unwanted authenticated browser request,
- cookies sent automatically,
- SameSite,
- CSRF token,
- origin/referer checks,
- no state-changing GET.

Strong answer:

```text
CSRF tricks a logged-in browser into sending an unwanted state-changing request using existing cookies. Defenses include SameSite cookies, CSRF tokens, origin or referer checks, and avoiding state-changing GET endpoints. XSS and CSRF are different: XSS runs attacker code in the site, while CSRF abuses browser credential attachment.
```

---

### Q35. Explain JWT risks.

Strong answer must include:

- signed not encrypted,
- validate signature,
- algorithm,
- issuer/audience/expiry,
- storage risk,
- revocation challenge.

Strong answer:

```text
A JWT is a signed token containing claims. Standard JWT payload is encoded, not encrypted, so sensitive data should not be placed inside unless using encryption. Servers must validate signature, algorithm, issuer, audience, expiry, and key rotation. Storage is a major risk in browsers. JWTs are convenient for stateless verification but revocation and rotation need careful design.
```

---

### Q36. Explain prototype pollution.

Strong answer must include:

- attacker controls object keys,
- `__proto__`, constructor, prototype,
- unsafe deep merge,
- changes prototype chain,
- block keys and validate schemas.

Strong answer:

```text
Prototype pollution happens when attacker-controlled keys are merged into objects in a way that modifies Object.prototype or another prototype. Dangerous keys include __proto__, constructor, and prototype. It often comes from unsafe deep merge or path setters. Defenses include schema validation, blocking dangerous keys, using safe merge libraries, and avoiding arbitrary object merging from user input.
```

---

### Q37. How do you store auth tokens in browser?

Strong answer must include:

- localStorage exposed to XSS,
- HttpOnly cookie unreadable by JS,
- Secure/SameSite,
- CSRF trade-off,
- threat model.

Strong answer:

```text
For high-risk browser auth, I prefer HttpOnly, Secure, SameSite cookies because JavaScript cannot read them, reducing token theft from XSS. Cookies introduce CSRF considerations depending SameSite and flow, so I add CSRF protections where needed. localStorage is easy but readable by injected JavaScript, so it is risky if XSS occurs. The final choice depends on threat model and architecture.
```

---

## 10. Testing Round

### Q38. How do you test async JavaScript?

Strong answer must include:

- await/return promise,
- success and failure,
- fake timers,
- microtasks,
- avoid assertions after test ends.

Strong answer:

```text
I test async JavaScript by awaiting the promise or returning it from the test. I cover success, rejection, timeout, and cancellation paths. For timers, I use fake timers carefully and ensure promise microtasks are flushed where needed. The main trap is a test passing before async assertions actually run.
```

---

### Q39. Unit vs integration vs e2e?

Strong answer must include:

- unit fast isolated logic,
- integration boundaries,
- e2e critical flows,
- contract tests,
- balanced pyramid.

Strong answer:

```text
Unit tests verify small logic quickly. Integration tests verify modules and real boundaries like database adapters or API clients. Contract tests protect API compatibility. E2E tests verify critical user flows through the full stack but are slower and more brittle. I use a balanced pyramid: many unit/integration tests and fewer high-value E2E tests.
```

---

### Q40. What makes a test brittle?

Strong answer must include:

- implementation details,
- over-mocking,
- snapshots without behavior,
- timing assumptions,
- selectors tied to layout,
- nondeterminism.

Strong answer:

```text
Tests become brittle when they assert implementation details instead of behavior, overuse snapshots, depend on timing, mock too much, or use selectors tied to layout instead of accessible behavior. I prefer testing user-visible behavior, stable contracts, and meaningful outcomes.
```

---

### Q41. How do you test debounce?

Strong answer must include:

- fake timers,
- rapid calls,
- only latest call,
- cancel/flush if supported,
- preserve args/this if relevant.

Test sketch:

```js
const fn = vi.fn();
const debounced = debounce(fn, 100);

debounced("a");
debounced("ab");
vi.advanceTimersByTime(99);
expect(fn).not.toHaveBeenCalled();
vi.advanceTimersByTime(1);
expect(fn).toHaveBeenCalledWith("ab");
```

Strong answer:

```text
I use fake timers so the test is deterministic. I call the debounced function multiple times, advance time before the delay to verify it has not run, then advance to the delay and verify only the latest arguments were used. If the implementation supports cancel or flush, I test those explicitly.
```

---

## 11. Performance And Debugging Round

### Q42. How do you debug a memory leak?

Strong answer must include:

- reproduce repeatedly,
- heap snapshots,
- allocation timeline,
- retaining paths,
- common sources,
- verify fix.

Strong answer:

```text
I reproduce the leak with a repeated flow, take heap snapshots before and after, compare retained objects, and inspect retaining paths. Common JavaScript leaks are event listeners, intervals, subscriptions, closures, detached DOM nodes, global stores, unbounded caches, and queues. I fix the owner reference, add cleanup or bounds, and verify with another snapshot and memory trend.
```

---

### Q43. How do you debug high CPU in Node?

Strong answer must include:

- CPU profile,
- event loop delay,
- hot endpoint,
- sync work,
- regex/JSON/crypto/compression,
- mitigation.

Strong answer:

```text
I check CPU profiles, event loop delay, request rate, hot endpoints, and recent deployments. Causes include tight loops, large JSON processing, regex backtracking, sync crypto/compression/fs, or excessive logging. Mitigation may be rollback, rate limiting, scaling, moving CPU work to workers, streaming, or fixing algorithmic complexity.
```

---

### Q44. How do you improve bundle performance?

Strong answer must include:

- bundle analyzer,
- code splitting,
- tree shaking,
- lazy loading,
- dependency replacement,
- budgets,
- parse/execute cost.

Strong answer:

```text
I inspect bundle analyzer and real performance data. I remove unused dependencies, ensure tree shaking works, split by routes/features, lazy-load noncritical code, replace heavy libraries where justified, and set bundle budgets in CI. I remember that JavaScript cost includes download, parse, compile, and execution, not only transfer size.
```

---

### Q45. Explain Core Web Vitals.

Strong answer must include:

- LCP,
- INP,
- CLS,
- user experience,
- field data,
- fixes.

Strong answer:

```text
Core Web Vitals measure user experience. LCP measures loading of the largest visible content. INP measures interaction responsiveness. CLS measures unexpected layout shifts. I optimize them by improving server and resource loading, reducing main-thread work, reserving layout dimensions, and verifying with field data and lab traces.
```

---

## 12. Machine Coding Round

### Q46. Implement debounce.

Expected essentials:

- closure timer,
- clear previous timer,
- latest args,
- preserve this,
- optional cancel.

Reference implementation:

```js
function debounce(fn, delayMs) {
  let timeoutId;

  function debounced(...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
      fn.apply(this, args);
    }, delayMs);
  }

  debounced.cancel = function cancel() {
    clearTimeout(timeoutId);
    timeoutId = undefined;
  };

  return debounced;
}
```

Follow-ups:

- Add leading option.
- Add trailing option.
- Add flush.
- Test with fake timers.

---

### Q47. Implement throttle.

Expected essentials:

- rate limit,
- timestamp or timer,
- leading/trailing requirement,
- preserve this/args,
- final action behavior.

Strong explanation:

```text
Throttle limits execution frequency. I clarify whether leading and trailing calls are needed because dropping the final event can be wrong for drag or resize workflows. I store timing state in closure and preserve latest arguments for trailing execution.
```

---

### Q48. Implement EventEmitter.

Expected essentials:

- Map event name to Set listeners,
- on/off/emit/once,
- unsubscribe function,
- copy listeners during emit,
- cleanup empty sets.

Reference implementation:

```js
class EventEmitter {
  constructor() {
    this.listeners = new Map();
  }

  on(eventName, listener) {
    if (!this.listeners.has(eventName)) {
      this.listeners.set(eventName, new Set());
    }

    this.listeners.get(eventName).add(listener);
    return () => this.off(eventName, listener);
  }

  off(eventName, listener) {
    const listeners = this.listeners.get(eventName);
    if (!listeners) return;

    listeners.delete(listener);
    if (listeners.size === 0) {
      this.listeners.delete(eventName);
    }
  }

  once(eventName, listener) {
    const unsubscribe = this.on(eventName, (...args) => {
      unsubscribe();
      listener(...args);
    });

    return unsubscribe;
  }

  emit(eventName, ...args) {
    const listeners = this.listeners.get(eventName);
    if (!listeners) return false;

    for (const listener of [...listeners]) {
      listener(...args);
    }

    return true;
  }
}
```

---

### Q49. Implement promise pool.

Expected essentials:

- concurrency limit,
- preserve order,
- handle errors,
- avoid starting all work at once.

Reference implementation:

```js
async function promisePool(items, limit, task) {
  const results = new Array(items.length);
  let nextIndex = 0;

  async function worker() {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      results[index] = await task(items[index], index);
    }
  }

  await Promise.all(
    Array.from({ length: Math.min(limit, items.length) }, () => worker())
  );

  return results;
}
```

Strong explanation:

```text
Promise.all starts everything immediately, so it does not limit concurrency. A pool starts N workers that pull from a shared index and store results by original index.
```

---

### Q50. Implement LRU cache.

Expected essentials:

- capacity,
- get marks recent,
- set marks recent,
- evict least recent,
- O(1) Map solution or hash map plus linked list.

Reference implementation:

```js
class LruCache {
  constructor(capacity) {
    this.capacity = capacity;
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

    if (this.values.size > this.capacity) {
      this.values.delete(this.values.keys().next().value);
    }
  }
}
```

---

### Q51. Implement retry with backoff.

Expected essentials:

- max attempts,
- delay,
- backoff,
- jitter,
- retry predicate,
- throw last error.

Reference implementation:

```js
async function retry(task, options = {}) {
  const maxAttempts = options.maxAttempts ?? 3;
  const baseDelayMs = options.baseDelayMs ?? 100;
  const shouldRetry = options.shouldRetry ?? (() => true);

  let lastError;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      return await task(attempt);
    } catch (error) {
      lastError = error;

      if (attempt === maxAttempts || !shouldRetry(error)) {
        break;
      }

      const jitter = Math.floor(Math.random() * baseDelayMs);
      const delayMs = baseDelayMs * 2 ** (attempt - 1) + jitter;
      await new Promise(resolve => setTimeout(resolve, delayMs));
    }
  }

  throw lastError;
}
```

Production follow-up:

```text
Retries amplify load, so I use caps, backoff, jitter, idempotency, and retryable error classification.
```

---

## 13. System Design Round

### Q52. Design autocomplete.

Strong answer must include:

- clarify scale/latency/ranking,
- debounce,
- abort/latest guard,
- cache recent queries,
- loading/error states,
- backend prefix/search index,
- ranking/personalization,
- rate limit,
- observability.

Strong answer outline:

```text
On frontend I debounce input, cancel previous requests, use latest-only guards, cache recent query results, and handle loading/error/empty states accessibly. Backend can use a prefix index, search engine, or trie-like structure depending scale and ranking needs. I would add ranking, typo tolerance if required, rate limiting, caching, metrics for latency and zero-result rate, and safeguards against abusive queries.
```

Follow-ups:

- How do you prevent stale results?
- How do you support typo tolerance?
- How do you cache without showing stale data?

---

### Q53. Design file upload.

Strong answer must include:

- file size/type,
- signed URL,
- direct object storage,
- multipart/resumable,
- metadata API,
- progress,
- virus scan,
- async processing,
- auth and limits.

Strong answer outline:

```text
I would use an API to authorize upload and issue signed URLs, then upload directly to object storage to avoid routing large files through app servers. For large files I would support multipart/resumable upload. Metadata is stored separately. After upload, background workers can scan, process, and generate thumbnails. Frontend shows progress, retry, cancel, and validation. Production controls include auth, size/type limits, malware scanning, lifecycle cleanup, and audit logs.
```

---

### Q54. Design notification system.

Strong answer must include:

- channels,
- preferences,
- queue,
- workers,
- templates,
- idempotency,
- retry/DLQ,
- rate limits,
- observability.

Strong answer outline:

```text
I would separate notification creation from delivery. APIs write notification events and user preferences. A queue decouples producers from channel workers for email, SMS, push, or in-app. Workers render templates, enforce preferences, use idempotency keys, retry transient failures with backoff, and send permanent failures to a dead-letter queue. Observability tracks delivery latency, failure rate, provider errors, and user opt-outs.
```

---

### Q55. Design real-time chat.

Strong answer must include:

- WebSocket/SSE choice,
- auth,
- connection management,
- message persistence,
- ordering,
- fanout,
- presence,
- retries/acks,
- scaling.

Strong answer outline:

```text
I would use WebSockets for bidirectional chat. Clients authenticate connection, send messages to a service that persists them and publishes to recipients. Multiple gateway nodes need shared pub-sub or message broker for fanout. I would handle ordering with server timestamps or sequence IDs, acknowledgments for delivery, reconnect/resync, rate limits, presence, and observability for connection count, message latency, and failure rates.
```

---

### Q56. Design frontend state management for dashboard.

Strong answer must include:

- local UI state,
- server state,
- URL state,
- global client state,
- cache invalidation,
- optimistic updates,
- error/loading states.

Strong answer outline:

```text
I separate state by ownership. Local component state handles UI-only details. URL state handles shareable filters and pagination. Server state belongs in a query/cache layer with loading, error, freshness, invalidation, and retry behavior. Global client state is only for cross-cutting UI/session state. This avoids putting everything into one global store and makes data freshness easier to reason about.
```

---

## 14. Behavioral Production Follow-Ups

### Q57. Tell me about a time you debugged a production issue.

Strong answer shape:

```text
Situation -> symptom -> impact -> investigation -> mitigation -> root cause -> fix -> prevention -> learning
```

JavaScript-specific signals to include when relevant:

- event loop lag,
- heap snapshot,
- CPU profile,
- browser performance trace,
- network waterfall,
- source maps,
- logs/traces/request IDs,
- regression test,
- alert improvement.

---

### Q58. How do you decide between quick fix and root-cause fix?

Strong answer:

```text
During an incident, mitigation comes first if users are impacted. That can be rollback, feature flag disable, rate limit, cache bypass, or traffic shift. After stability, I root cause the issue and add a durable fix with tests and alerts. I avoid making risky architectural changes during active incidents unless required for recovery.
```

---

### Q59. How do you communicate trade-offs?

Strong answer:

```text
I frame trade-offs around user impact, latency, consistency, operational complexity, cost, security, and failure modes. I avoid saying one option is always best. I state the condition where each option wins and what I would measure after shipping.
```

---

## 15. Rapid Fire Drill

Answer each in 20 seconds.

1. Why is `typeof null` object?
2. Why does `NaN !== NaN`?
3. What does Object.is fix?
4. Is object spread deep?
5. Does const freeze objects?
6. What is TDZ?
7. What is lexical scope?
8. What does closure retain?
9. How is this determined?
10. Why do arrows not work as prototype methods?
11. What is prototype lookup?
12. Does class remove prototypes?
13. Does async function return value or promise?
14. Does await block the whole thread?
15. Do promises run before timers?
16. Does Promise.all limit concurrency?
17. Does fetch reject on 500?
18. Does Promise.race cancel losers?
19. What causes stale search result?
20. How to cancel fetch?
21. What is event delegation?
22. What is layout thrashing?
23. What is LCP?
24. What is INP?
25. What is CLS?
26. What blocks Node event loop?
27. What is backpressure?
28. Why use streams?
29. What is XSS?
30. What is CSRF?
31. What is CORS?
32. Is JWT encrypted?
33. Why avoid localStorage for sensitive tokens?
34. What is prototype pollution?
35. How do fake timers help?
36. Why are snapshots brittle?
37. How to test async rejection?
38. What is LRU?
39. Why retry with jitter?
40. Why idempotency keys?
41. Why pin Node version?
42. What does a lockfile protect?
43. Why prefer Node LTS for production?
44. When is edge runtime useful?
45. What breaks Node libraries in edge runtime?
46. Why does TC39 stage not equal production support?
47. When can ARIA make accessibility worse?
48. How does Intl help with dates and currency?
49. What is a trace ID?
50. What should source maps include for production debugging?

---

## 16. Final Mock Interview Set A

Use this as a full 60-minute round.

### Section 1: Core JS

1. Explain closure with memory implications.
2. Explain this binding with a detached method example.
3. Explain prototype lookup and class syntax.
4. Predict output involving var/let in async loop.

### Section 2: Async

1. Predict promise/timer output.
2. Explain Promise.all vs promise pool.
3. Design latest-only search request handling.

### Section 3: Machine Coding

1. Implement EventEmitter.
2. Add once and unsubscribe.
3. Discuss listener error strategy.

### Section 4: Production

1. Debug frontend memory leak.
2. Debug CORS auth issue.
3. Explain test strategy for the fix.

Passing bar:

- no major conceptual mistakes,
- code compiles mentally,
- production follow-ups include cleanup and observability.

---

## 17. Final Mock Interview Set B

Use this as a Node/full-stack round.

### Section 1: Runtime

1. Explain Node runtime and libuv.
2. What blocks the event loop?
3. Explain streams and backpressure.

### Section 2: Backend API

1. Design safe Express endpoint for booking creation.
2. Add validation, idempotency, timeout, retry rules.
3. Add observability and graceful shutdown.

### Section 3: Incident

1. API p95 latency jumps from 120 ms to 4 seconds.
2. CPU is normal, DB pool is saturated.
3. What do you check and how do you mitigate?

### Section 4: Security

1. JWT storage decision in browser.
2. XSS prevention.
3. CSRF controls for cookie auth.

Passing bar:

- separates symptom from root cause,
- mentions connection pools and slow query traces,
- avoids retry storm,
- includes idempotency for writes.

---

## 18. Final Mock Interview Set C

Use this as a frontend/system design round.

### Section 1: Browser

1. Explain rendering pipeline.
2. Debug poor INP.
3. Debug layout shift.

### Section 2: Architecture

1. Choose CSR vs SSR vs SSG for product catalog.
2. Design caching strategy.
3. Explain stale data and invalidation.

### Section 3: Scenario

1. Autocomplete shows older results after newer query.
2. Implement fix at frontend level.
3. Extend design to backend ranking and rate limiting.

### Section 4: Testing

1. Test debounce.
2. Test stale response guard.
3. Test accessibility of search results.

Passing bar:

- connects user experience metrics to implementation,
- includes cancellation and latest-only guard,
- uses accessible loading/error states.

---

## 19. Interviewer Follow-Up Ladders

### Closure Follow-Up Ladder

1. What is closure?
2. Is the captured value copied?
3. What happens with var in loop?
4. How can closure cause memory leak?
5. How would you detect closure-based leak?
6. How would you fix it in a React component or DOM listener?

### Event Loop Follow-Up Ladder

1. What runs first: promise or timeout?
2. What is microtask queue?
3. Can microtasks starve rendering?
4. How does async/await resume?
5. What is different in Node?
6. How do you measure event loop lag?

### Security Follow-Up Ladder

1. What is XSS?
2. Is input validation enough?
3. What is output context?
4. What does CSP add?
5. How do cookies change CSRF risk?
6. How would you test the fix?

### Performance Follow-Up Ladder

1. Page is slow. What first?
2. Which metric is bad?
3. Is bottleneck network, JS, render, or server?
4. How do you prove it?
5. What fix has smallest blast radius?
6. How do you prevent regression?

---

## 20. Platform, Observability, And Capstone Round

Use this round after finishing the enriched setup, runtime, observability, and capstone sheets.

### Q60. How do you set up a JavaScript project so it is reproducible?

Strong answer must include:

- Node version pinning,
- one package manager,
- committed lockfile,
- `packageManager` field or equivalent convention,
- scripts for lint/test/build/start,
- environment variable documentation,
- CI using the same commands.

Strong answer:

```text
I pin the Node version, choose one package manager, commit the lockfile, and make the common workflows explicit in package scripts. I also document environment variables, avoid mixed lockfiles, and run the same install, lint, test, and build commands in CI. That makes local development, CI, and deployment use the same dependency graph and runtime assumptions.
```

Follow-ups:

- Why is mixing npm, pnpm, and Yarn lockfiles risky?
- What does the `packageManager` field protect?
- Why should production usually prefer Node LTS over Current?

---

### Q61. How do you decide whether to use browser JavaScript, Node.js, edge runtime, serverless, Deno, or Bun?

Strong answer must include:

- workload type,
- latency and geography,
- runtime APIs,
- cold start or long-running process needs,
- filesystem/native dependency constraints,
- ecosystem maturity,
- observability and deployment support.

Strong answer:

```text
I choose runtime based on workload and constraints. Browser JavaScript is for user interaction. Node.js is strong for IO-heavy backend services with mature package support. Edge runtimes help with low-latency request shaping near users, but have stricter APIs. Serverless is useful for bursty workloads but needs cold-start and connection-pool planning. Deno and Bun can be good fits in specific teams, but I would validate ecosystem, deployment, debugging, and operational support before production adoption.
```

Follow-ups:

- Why can edge runtime break Node libraries?
- When is serverless a poor fit?
- What would you measure before migrating runtime?

---

### Q62. How do you handle a modern ECMAScript feature in production?

Strong answer must include:

- stable spec vs proposal distinction,
- runtime support,
- transpilation/polyfill decision,
- browser/Node target,
- bundle/runtime cost,
- test coverage,
- fallback or rollback.

Strong answer:

```text
I first separate stable ECMAScript from TC39 proposals. Then I check support in the runtimes we target: browsers, Node, edge, or build output. If syntax can be transpiled safely, I verify bundle impact. If behavior needs a polyfill, I check cost and correctness. Some features cannot be fully polyfilled. I would ship only with tests, compatibility checks, and a rollback path.
```

Follow-ups:

- Why does TC39 stage not automatically mean production-ready?
- What is the difference between transpiling syntax and polyfilling APIs?
- Why can feature support differ between browser and Node?

---

### Q63. How do you make a JavaScript UI accessible and localized?

Strong answer must include:

- semantic HTML first,
- keyboard navigation,
- focus management,
- labels and error messages,
- cautious ARIA,
- `Intl` formatting,
- locale and time zone awareness,
- text expansion and RTL/bidi awareness.

Strong answer:

```text
I start with semantic HTML and native controls because they already carry accessibility behavior. Then I verify labels, keyboard navigation, focus order, visible focus, error messages, and screen-reader announcements for dynamic updates. I use ARIA only when semantics are missing, not as decoration. For localization, I avoid hard-coded date, number, currency, and plural formatting and use Intl with locale and time zone awareness.
```

Follow-ups:

- When can ARIA make accessibility worse?
- How do you test keyboard accessibility?
- Why is string concatenation dangerous for i18n?

---

### Q64. What observability would you add to a production JavaScript system?

Strong answer must include:

- frontend RUM,
- browser error reporting,
- Node structured logs,
- metrics,
- distributed traces,
- request/correlation IDs,
- source maps and release metadata,
- SLOs and alerting.

Strong answer:

```text
On the frontend I want RUM for user-perceived latency, browser errors, release metadata, and source-map-backed stack traces. On the backend I want structured logs, RED-style metrics, distributed traces, dependency latency, event-loop lag, memory, and saturation signals. A request or trace ID should connect browser actions to Node services. Alerts should map to SLO impact, not just noisy raw metrics.
```

Follow-ups:

- What should be on a JavaScript production dashboard?
- How do source maps help and what is their security risk?
- How do you avoid alert fatigue?

---

### Q65. Explain the full JavaScript capstone from click to production incident handling.

Strong answer must include:

- accessible client interaction,
- validation and cancellation,
- TypeScript boundary awareness,
- Node API validation,
- idempotency for writes,
- timeouts/retries,
- logs/metrics/traces,
- tests,
- deployment and rollback.

Strong answer outline:

```text
For a booking flow, the browser validates accessible form state, cancels stale search requests, and sends a request with an idempotency key and trace ID. The Node API validates runtime input, checks auth, applies idempotency, calls inventory/payment with timeouts and retry rules, and returns stable error shapes. Tests cover utilities, API behavior, browser flow, and failure paths. Observability connects RUM, logs, metrics, traces, and source maps. If a release causes latency or errors, I mitigate with rollback or feature flag, then root cause with traces, profiles, and regression tests.
```

Follow-ups:

- Where can TypeScript help and where can it not help?
- What makes retry safe for booking creation?
- What would you monitor during rollout?

---

### Platform Follow-Up Ladder

1. What runtime are you targeting?
2. Which APIs does that runtime support?
3. How do dependencies behave there?
4. What are the deployment limits?
5. How do you observe it?
6. How do you roll back if the runtime choice fails?

### Observability Follow-Up Ladder

1. What symptom did the user see?
2. Which metric proves impact?
3. Which trace/log points to the failing dependency?
4. Which profile or source map points to code?
5. What is the mitigation?
6. What test or alert prevents recurrence?

---

## 21. Final Evaluation Checklist

You are ready for a JavaScript MAANG-style interview when you can:

- answer core language questions without guessing,
- solve tricky output by rules,
- implement common utilities from scratch,
- explain browser and Node runtime differences,
- explain runtime and platform trade-offs,
- debug async races,
- debug memory leaks,
- debug performance incidents with tools,
- explain security controls precisely,
- explain accessibility and i18n production decisions,
- design observability for frontend and Node systems,
- design frontend and Node systems with trade-offs,
- write tests for async and timer behavior,
- explain a full-stack capstone end to end,
- connect every answer to production failure modes.

Final spoken line:

```text
For JavaScript interviews, I try to connect language semantics, runtime behavior, and production impact. Knowing what the feature does is useful, but knowing how it fails is what makes the answer senior.
```
