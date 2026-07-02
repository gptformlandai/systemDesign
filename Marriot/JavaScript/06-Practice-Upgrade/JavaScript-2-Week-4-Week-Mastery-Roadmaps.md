# JavaScript 2-Week And 4-Week Mastery Roadmaps

> Track: JavaScript Interview Track - Practice Upgrade  
> Goal: convert the JavaScript sheets into a concrete beginner-to-pro study path.

---

## 1. Daily Study Loop

Every study day:

1. Read one sheet section.
2. Answer 20 active recall questions.
3. Build or sketch one mini-lab.
4. Explain one concept aloud in 90 seconds.
5. Mark Red/Yellow/Green.

Time split:

| Activity | Sprint | Deep Plan |
|---|---:|---:|
| Reading | 30% | 35% |
| Coding/labs | 30% | 30% |
| Active recall | 25% | 20% |
| Mock/debugging | 15% | 15% |

---

## 2. Two-Week Interview Sprint

Best for someone who already knows basic JavaScript syntax.

| Day | Focus | Practice |
|---:|---|---|
| 0 | Setup, Node LTS, package manager, first project | setup lab + install/lockfile explanation |
| 1 | values, variables, equality, coercion | output questions |
| 2 | execution context, scope, closures | closure memory traps |
| 3 | `this`, prototypes, classes | prototype and binding drills |
| 4 | arrays, objects, functional patterns | object transform labs |
| 5 | modern ES and feature adoption | modules/runtime support drill |
| 6 | async, event loop, promises | async output + promise pool |
| 7 | DOM, events, fetch, CORS | browser scenario drills |
| 8 | storage, workers, PWA, performance | Web Vitals and worker lab |
| 9 | TypeScript and runtime validation | typed API boundary lab |
| 10 | Node.js backend production | timeout/retry/idempotency lab |
| 11 | security and testing | XSS/CSRF/JWT/testing mock |
| 12 | performance, memory, debugging, observability | heap/profile/runbook lab |
| 13 | runtimes, a11y/i18n/Web Components, system design | protocol/runtime/a11y mock |
| 14 | capstone | full JavaScript platform loop |

Pass gate:

- solve 50 output questions without guessing
- implement 12+ machine-coding utilities
- finish 8+ mini-labs
- explain browser vs Node vs edge runtime differences
- explain one full capstone request flow

---

## 3. Four-Week Mastery Plan

### Week 1: Language Core

| Day | Focus | Deliverable |
|---:|---|---|
| 1 | setup and first project | reproducible project skeleton |
| 2 | primitive/reference values | memory/value notes |
| 3 | equality/coercion/truthiness | output question set |
| 4 | execution context/call stack | stack and scope diagram |
| 5 | hoisting/TDZ/closures | closure examples |
| 6 | `this`/prototypes/classes | binding/prototype cheatsheet |
| 7 | mock day | 60-minute core JS mock |

### Week 2: Browser, Async, TypeScript

| Day | Focus | Deliverable |
|---:|---|---|
| 8 | event loop/promises | async output simulator |
| 9 | cancellation/retry/concurrency | reliable fetch helper |
| 10 | DOM/events/forms/fetch/CORS | event delegation lab |
| 11 | storage/workers/PWA | offline/cache decision |
| 12 | frontend performance | Web Vitals debug memo |
| 13 | TypeScript | typed API and runtime validation |
| 14 | a11y/i18n/Web Components | accessible autocomplete design |

### Week 3: Node, Security, Testing

| Day | Focus | Deliverable |
|---:|---|---|
| 15 | Node runtime/modules/package hygiene | ESM/CJS and lockfile notes |
| 16 | APIs, streams, backpressure | streaming upload/download sketch |
| 17 | timeouts/retries/idempotency | resilient client lab |
| 18 | worker threads/queues/scaling | CPU offload decision |
| 19 | security | XSS/CSRF/prototype pollution review |
| 20 | testing | unit/integration/E2E/contract test plan |
| 21 | mock day | Node/security/testing mock |

### Week 4: Senior Production And Capstone

| Day | Focus | Deliverable |
|---:|---|---|
| 22 | performance/memory/debugging | heap/profile runbook |
| 23 | observability/SRE/OpenTelemetry | dashboard and alert plan |
| 24 | modern runtimes | Node/browser/edge/Deno/Bun decision memo |
| 25 | current ECMAScript/platform update | feature adoption checklist |
| 26 | system design | checkout/search/realtime design |
| 27 | capstone build/design | full-stack booking platform |
| 28 | final mock + rubric | readiness scorecard |

---

## 4. Red Gap Repair Plan

| Red Gap | Repair Drill |
|---|---|
| setup unclear | create project and explain Node/package/lockfile/scripts |
| coercion weak | solve 30 output questions aloud |
| closures weak | draw retained lexical environments |
| async weak | trace sync/microtask/task order |
| DOM weak | build event delegation todo |
| TypeScript weak | model discriminated union state |
| Node weak | explain event-loop delay and backpressure |
| security weak | walk XSS/CSRF/prototype pollution defenses |
| testing weak | map feature to unit/integration/E2E/contract tests |
| performance weak | debug LCP/INP/event-loop/memory cases |
| runtime weak | compare browser, Node, edge, Deno, Bun |
| a11y/i18n weak | design accessible localized autocomplete |
| observability weak | design logs/metrics/traces/RUM/source maps |
| capstone weak | explain request from UI to API to observability |

---

## 5. Final Capstone Checklist

Before calling the track complete, explain this system:

```text
JavaScript hotel booking platform:
- setup with Node LTS, package manager, lockfile, scripts, CI
- browser UI with accessible forms, i18n, fetch cancellation, storage decisions
- TypeScript API boundaries and runtime validation
- Node BFF/API with validation, auth, idempotency, timeouts, retries
- streams/backpressure for file export/upload
- worker/queue path for CPU-heavy or background jobs
- XSS, CSRF, JWT/session, prototype pollution, supply-chain defenses
- unit, integration, E2E, contract, accessibility, performance tests
- Web Vitals, logs, metrics, traces, source maps, release metadata
- runtime decision: browser vs Node vs edge/serverless
- build/tooling cross-links: bundlers, package managers, monorepos
```

