# React + Next.js Interview Scoring Rubrics — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: topic rubric (1-5) → readiness gates → action plan

---

## How to Use

For each topic, read the 1-5 descriptors. Score yourself honestly. Then check your readiness gate. Use the action plan to close gaps.

---

## Topic 1: React Component Model and Rendering

| Score | Description |
|---|---|
| 5 | Explains reconciliation, Fiber work loop, concurrent mode lanes, stale closure trap, batching internals. Can predict exact render counts. |
| 4 | Explains re-render triggers (state, props, context), reconciliation overview, React.memo and its limits, correct use of keys. |
| 3 | Knows when components re-render and why. Can use React.memo and useCallback but not sure when they're counterproductive. |
| 2 | Understands controlled inputs and props. Confused about when memoization helps. |
| 1 | Can render a component but does not understand re-render triggers. |

**Action plan if < 3:** Re-read `React-Core-Fundamentals-Gold-Sheet.md` section on rendering phases. Do F1 and F2 labs.

---

## Topic 2: Hooks — Depth and Correctness

| Score | Description |
|---|---|
| 5 | Can explain stale closures, useLayoutEffect vs useEffect timing, React 18 useTransition/useDeferredValue tradeoffs, custom hook patterns, AbortController cleanup. Knows when NOT to use useMemo. |
| 4 | Uses all core hooks correctly with proper deps arrays. Writes correct useEffect cleanup. Understands useReducer for state machines. Writes useful custom hooks. |
| 3 | Uses useState, useEffect, useRef correctly. Understands basic useCallback/useMemo. Sometimes writes incomplete cleanup or stale deps. |
| 2 | Can use useState and basic useEffect. Unclear on cleanup, deps, or memoization. |
| 1 | Can call useState. Does not understand useEffect or deps array. |

**Action plan if < 3:** Re-read `React-Hooks-Complete-Gold-Sheet.md`. Do I2 lab (useReducer form). Run tricky questions Q12-Q17.

---

## Topic 3: Custom Hooks and Reusability

| Score | Description |
|---|---|
| 5 | Extracts hooks naturally, knows how to compose hooks, handles edge cases (SSR guards, isMounted patterns, AbortController). Identifies when NOT to extract a hook. |
| 4 | Writes useDebounce, useLocalStorage, useAsync, usePagination from scratch. Handles cleanup and SSR. |
| 3 | Can extract simple custom hooks. May miss edge cases (cleanup, isMounted, SSR). |
| 2 | Understands the concept but struggles to implement from scratch. |
| 1 | Does not know custom hooks. |

**Action plan if < 4:** Re-read `React-Custom-Hooks-Pattern-Library-Gold-Sheet.md`. Write useDebounce and useLocalStorage from memory.

---

## Topic 4: TypeScript in React

| Score | Description |
|---|---|
| 5 | Types generic components, discriminated unions, forwardRef, utility types (Omit/Pick/ReturnType), event handlers, Zod integration. Knows type vs interface tradeoffs. |
| 4 | Types all hooks correctly, event handlers, component props. Extends HTML element props. Types context with default value. |
| 3 | Can type basic props and useState. Reaches for `any` when things get complex. |
| 2 | Adds TypeScript types but often wrong or too broad. |
| 1 | Does not use TypeScript in React. |

**Action plan if < 3:** Re-read `React-TypeScript-Deep-Dive-Gold-Sheet.md`. Focus on event handler types and useRef typing.

---

## Topic 5: Next.js App Router Core

| Score | Description |
|---|---|
| 5 | Explains all four caching layers, on-demand revalidation strategy, parallel and intercepting routes, Next.js 15 async params, Server Actions security model, middleware edge runtime. |
| 4 | Knows Server vs Client components, file conventions (page/layout/error/loading), ISR vs SSR vs SSG tradeoffs, Server Actions with auth/validation, basic caching. |
| 3 | Builds a multi-route App Router app. Knows when to use 'use client'. Understands ISR vs SSR at a high level. |
| 2 | Knows pages vs app router difference. Confused about Server Components and when to use Client. |
| 1 | Only familiar with pages router. |

**Action plan if < 3:** Work through `NextJS-App-Router-Core-Gold-Sheet.md` then `NextJS-Data-Fetching-Architecture-Gold-Sheet.md`.

---

## Topic 6: State Management

| Score | Description |
|---|---|
| 5 | Knows when to use useState vs useReducer vs Zustand vs React Query. Understands Zustand selector optimization, TanStack Query cache normalization, optimistic updates with useOptimistic. |
| 4 | Makes correct state management choices. Implements global state with Zustand or Context. Uses TanStack Query for server state with correct staleTime settings. |
| 3 | Uses Context for global state. Understands TanStack Query basics (useQuery, useMutation). May not know when Context causes performance problems. |
| 2 | Uses useState for everything including global state — prop drilling. |
| 1 | No concept of global state management. |

**Action plan if < 3:** Re-read `React-State-Management-Full-Spectrum-Gold-Sheet.md` and `React-Server-State-TanStack-SWR-Gold-Sheet.md`.

---

## Topic 7: Performance Optimization

| Score | Description |
|---|---|
| 5 | Runs bundle analysis, knows tree-shaking, code splitting with next/dynamic, image optimization pitfalls, Core Web Vitals targets and how to hit them, virtualizing large lists. |
| 4 | Knows useMemo/useCallback when beneficial, React.memo pitfalls, lazy loading, LCP optimization with priority, avoiding CLS with image dimensions. |
| 3 | Knows memoization exists. Can use React DevTools Profiler. Understands lazy loading concept. |
| 2 | Performance intuition but no systematic knowledge. |
| 1 | No performance knowledge. |

**Action plan if < 3:** Re-read `React-Next-Performance-Optimization-Gold-Sheet.md`. Practice the bundle analyzer on a local Next.js app.

---

## Topic 8: Error Handling

| Score | Description |
|---|---|
| 5 | Knows what Error Boundaries catch vs don't catch, error.tsx/not-found.tsx/global-error.tsx structure, async error state machines, retry with exponential backoff, Sentry integration pattern. |
| 4 | Sets up error.tsx, uses react-error-boundary, handles async errors with try/catch in effects and Server Actions. |
| 3 | Knows Error Boundaries exist. Uses try/catch for async errors. May miss that async errors bypass Error Boundaries. |
| 2 | Basic try/catch but no boundary strategy. |
| 1 | No error handling strategy. |

**Action plan if < 3:** Re-read `React-Error-Handling-Error-Boundaries-Gold-Sheet.md`.

---

## Topic 9: Testing

| Score | Description |
|---|---|
| 5 | Full RTL + Playwright testing strategy. MSW for API mocking. Tests user behavior not implementation. A11y testing. CI pipeline with coverage gates. |
| 4 | Writes RTL tests for user interactions. Uses MSW. Writes Playwright E2E for critical paths. Knows what to avoid testing (implementation details). |
| 3 | Writes basic RTL component tests. Understands mocking concept. |
| 2 | Can write tests but tests implementation details (internal state, method calls). |
| 1 | No testing knowledge. |

**Action plan if < 3:** Re-read `React-Next-Testing-Gold-Sheet.md`. Write one RTL test and one Playwright test for your S2 lab.

---

## Topic 10: System Design

| Score | Description |
|---|---|
| 5 | Full LLD designs for complex systems (real-time collab, large-scale e-commerce). Covers caching, CDN, Server/Client split, WebSocket, offline, A/B testing, performance budgets. |
| 4 | Designs multi-tier frontends. Chooses correct rendering strategy per page type. Handles real-time with WebSocket. Designs around Next.js caching architecture. |
| 3 | Can design a simple multi-page Next.js app with correct server/client split. Handles authentication and routing. |
| 2 | Knows basic component structure but struggles with architecture decisions. |
| 1 | No system design knowledge. |

**Action plan if < 3:** Re-read `React-Next-Frontend-System-Design-Gold-Sheet.md`. Practice describing your S2 lab design before coding.

---

## Readiness Gates

### Starter Ready (Junior — 1-3 YOE interviews)
- Topics 1, 2, 5: score ≥ 3
- Topics 3, 4, 6: score ≥ 2
- Can complete F1, F2, F3 labs under time

### Mid-Level Ready (4-6 YOE — most product company roles)
- Topics 1, 2, 3, 5, 6: score ≥ 4
- Topics 4, 7, 8: score ≥ 3
- Can complete I1 and I2 labs under time
- Can answer Round 1 and Round 2 mock interview at score ≥ 3

### Senior Ready (7+ YOE — senior SWE roles)
- All topics: score ≥ 4
- Topics 5, 7, 10: score ≥ 5
- Can complete S1 and S2 labs under time
- Rounds 1-4 mock interview at score ≥ 4

### MAANG Ready
- All topics: score ≥ 5
- All labs completed under time with clean code
- All 5 mock interview rounds at score ≥ 4
- Can explain production debugging cases from memory with correct prevention strategies
