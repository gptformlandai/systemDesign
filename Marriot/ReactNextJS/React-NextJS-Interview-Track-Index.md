# React + Next.js Interview Track Index

This folder is the React and Next.js mastery track for frontend, full-stack, product engineering, and MAANG-style interviews.

Audience:
- You know JavaScript or TypeScript basics.
- You want React and Next.js from browser fundamentals to production architecture.
- You want modular notes like the Python and React Native tracks.
- You want beginner clarity, senior-level trade-offs, code intuition, and interview-ready answers.

Current baseline checked on June 27, 2026:
- React docs show React `19.2`.
- Next.js docs show latest `16.2.9`.
- Next.js App Router is the primary modern architecture path.
- React Router docs show latest `8.0.1`, while v6+ data-router concepts remain important for interviews and existing apps.

Goal:
- Build deep React intuition from browser rendering and reconciliation to hooks, Fiber, Suspense, and production patterns.
- Master modern Next.js App Router: Server Components, Client Components, Server Actions, layouts, rendering strategies, caching, revalidation, and BFF design.
- Prepare for MAANG frontend system design, debugging rounds, real-world projects, and production architecture decisions.

Track size: **40 files, 9 groups**, beginner to MAANG-ready.

---

## How To Read These Notes

Read in order if React/Next.js is new. If you already build React apps, jump to Group 5 onward for modern Next.js, caching, performance, production, and system design.

Every sheet follows this rhythm:

```text
Intuition
Definition
Implementation model
Code example
Trade-offs
Real-world use cases
Mistakes to avoid
Interview answer
Revision notes
```

---

## 1. Web And Rendering Foundations

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Web-Rendering-Foundations/React-Next-Web-Rendering-Fundamentals-Gold-Sheet.md` | DOM, CSSOM, render tree, critical rendering path, event loop, hydration, SPA/MPA/hybrid, CSR/SSR/SSG/ISR, Core Web Vitals, resource hints, Chrome DevTools Performance panel, PPR |

Target:
- You can explain what the browser does before React enters the picture.
- You can compare virtual DOM, real DOM, hydration, and rendering strategies clearly.
- You know LCP/INP/CLS targets and how to hit them.

---

## 2. React Core And Hooks

| Order | File | What It Builds |
|---:|---|---|
| 2 | `02-React-Core-And-Hooks/React-Core-Fundamentals-Gold-Sheet.md` | JSX, components, props, state, class vs function, lifecycle, reconciliation, keys, controlled/uncontrolled, batching, flushSync, forwardRef, useImperativeHandle, Strict Mode, React 19 changes |
| 3 | `02-React-Core-And-Hooks/React-Hooks-Complete-Gold-Sheet.md` | `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, `useReducer`, custom hooks, hook rules, React 18/19 hooks, stale closure deep dive, TypeScript for hooks |
| 4 | `02-React-Core-And-Hooks/React-Advanced-Internals-Fiber-Concurrency-Suspense-Gold-Sheet.md` | Fiber node structure, work loop, concurrent lanes, scheduler, Suspense internals, render purity requirement |
| 25 | `02-React-Core-And-Hooks/React-TypeScript-Deep-Dive-Gold-Sheet.md` | Props typing, interface vs type, useState/useRef/useReducer typing, event handlers, discriminated unions, generic components, forwardRef typing, Zod validation |
| 26 | `02-React-Core-And-Hooks/React-18-19-New-Features-Concurrent-Gold-Sheet.md` | Automatic batching, flushSync, useTransition, useDeferredValue, useId, Suspense streaming SSR, React 19: use(), useActionState, useOptimistic, ref as prop, createRoot |
| 27 | `02-React-Core-And-Hooks/React-Custom-Hooks-Pattern-Library-Gold-Sheet.md` | 18 production custom hooks: useAsync, useFetch, usePagination, useLocalStorage, useDebounce, useThrottle, useIntersectionObserver, useWindowSize, and more |
| 28 | `02-React-Core-And-Hooks/React-Error-Handling-Error-Boundaries-Gold-Sheet.md` | 3 error zones, ErrorBoundary class component, what EB catches vs misses, async error state machine, Next.js error.tsx/not-found.tsx/global-error.tsx, retry with backoff |

Target:
- You understand React rendering as trigger, render, commit.
- You can explain hooks without stale-closure confusion.
- You can discuss React internals at a high level without overclaiming implementation details.
- You write TypeScript React components without reaching for `any`.

---

## 3. React Architecture And Routing

| Order | File | What It Builds |
|---:|---|---|
| 5 | `03-React-Architecture-And-Routing/React-Routing-SPA-Next-Router-Gold-Sheet.md` | React Router data APIs, nested routes, lazy route loading, Next file routing, dynamic routes, route groups |
| 21 | `03-React-Architecture-And-Routing/React-Advanced-Frontend-Patterns-Gold-Sheet.md` | Compound components, render props, HOCs, controlled/uncontrolled component APIs |

Target:
- You can design navigation for SPA and Next.js apps.
- You can choose component patterns based on API ergonomics and long-term maintainability.

---

## 4. Forms, Client State, And Server State

| Order | File | What It Builds |
|---:|---|---|
| 6 | `04-Forms-State-Server-State/React-Forms-Complete-Gold-Sheet.md` | Controlled/uncontrolled forms, validation, React Hook Form, Formik, large-form performance |
| 7 | `04-Forms-State-Server-State/React-State-Management-Full-Spectrum-Gold-Sheet.md` | Local/global decision, Context limitations, Redux, Redux Toolkit, Zustand internals + selector optimization, Jotai atoms, RTK Query |
| 8 | `04-Forms-State-Server-State/React-Server-State-TanStack-SWR-Gold-Sheet.md` | Server state vs client state, TanStack Query, cache, background refetch, mutations, SWR, consistency |

Target:
- You classify state before choosing a library.
- You separate form state, UI state, global client state, and server cache.

---

## 5. Next.js App Router And Data

| Order | File | What It Builds |
|---:|---|---|
| 9 | `05-NextJS-App-Router-And-Data/NextJS-App-Router-Core-Gold-Sheet.md` | App Router, Server Components, Client Components, `use client`, `use server`, layouts, templates, parallel routes, intercepting routes, Next.js 15 async params, streaming |
| 10 | `05-NextJS-App-Router-And-Data/NextJS-Rendering-Strategies-Deep-Dive-Gold-Sheet.md` | SSR, SSG, ISR, CSR, streaming, partial pre-rendering, when to use each |
| 11 | `05-NextJS-App-Router-And-Data/NextJS-Data-Fetching-Architecture-Gold-Sheet.md` | Async Server Components, Server Actions, API routes, BFF, React.cache(), waterfall prevention, Promise.all patterns |
| 12 | `05-NextJS-App-Router-And-Data/NextJS-Caching-Revalidation-Performance-Gold-Sheet.md` | 4-layer cache model: Router Cache → Full Route Cache → Data Cache → Request Memoization; on-demand revalidation; opt-out cheat sheet |
| 29 | `05-NextJS-App-Router-And-Data/NextJS-Server-Actions-Mutations-Forms-Gold-Sheet.md` | 'use server' rules, useActionState, useFormStatus, useOptimistic, file uploads, security (auth+validate+authz inside action) |
| 30 | `05-NextJS-App-Router-And-Data/NextJS-Middleware-Edge-Runtime-Gold-Sheet.md` | middleware.ts, NextResponse methods, auth middleware with jose JWT, i18n routing, A/B testing, rate limiting, Edge vs Node.js runtime |
| 31 | `05-NextJS-App-Router-And-Data/NextJS-Metadata-SEO-Structured-Data-Gold-Sheet.md` | Static + dynamic metadata, title templates, Open Graph, Twitter Card, JSON-LD schemas, sitemap.ts, robots.ts, dynamic OG images |
| 32 | `05-NextJS-App-Router-And-Data/NextJS-Advanced-Routing-Parallel-Intercepting-Gold-Sheet.md` | Route groups, catch-all segments, parallel routes (@slot), intercepting routes, combined modal pattern, route handlers, streaming with ReadableStream |

Target:
- You can design modern App Router systems and still explain legacy Pages Router APIs.
- You can choose rendering and caching strategies with business constraints in mind.
- You know the complete caching architecture and can reason about each layer.

---

## 6. Performance, Security, Styling, And Testing

| Order | File | What It Builds |
|---:|---|---|
| 13 | `06-Performance-Security-Styling-Testing/React-Next-Authentication-Security-Gold-Sheet.md` | Cookies vs tokens, session management, NextAuth basics, route protection, XSS, CSRF |
| 14 | `06-Performance-Security-Styling-Testing/React-Next-Styling-UI-A11y-Gold-Sheet.md` | CSS fundamentals, Flexbox, Grid, Tailwind, CSS Modules, styled-components, design systems, accessibility |
| 15 | `06-Performance-Security-Styling-Testing/React-Next-Performance-Optimization-Gold-Sheet.md` | Code splitting, lazy loading, tree shaking, bundle analysis, images, Web Vitals |
| 16 | `06-Performance-Security-Styling-Testing/React-Next-Testing-Gold-Sheet.md` | Jest, React Testing Library, integration tests, Cypress, Playwright |

Target:
- You can ship secure, styled, accessible, tested, measurable apps.
- You can debug performance using Web Vitals and bundle/runtime evidence.

---

## 7. Production Architecture And System Design

| Order | File | What It Builds |
|---:|---|---|
| 17 | `07-Production-Architecture-And-System-Design/React-Next-Realtime-Interactive-Systems-Gold-Sheet.md` | WebSockets, polling, SSE, optimistic UI, reconnect patterns |
| 18 | `07-Production-Architecture-And-System-Design/React-Next-Deployment-Infrastructure-Gold-Sheet.md` | Vercel, CI/CD, environment variables, Edge functions |
| 19 | `07-Production-Architecture-And-System-Design/React-Next-Production-Architecture-Gold-Sheet.md` | Feature-based folder structure, dependency direction, monorepo, Turborepo |
| 20 | `07-Production-Architecture-And-System-Design/React-Next-Error-Handling-Observability-Gold-Sheet.md` | error.tsx/global-error.tsx/not-found.tsx, Sentry integration, retry backoff, Web Vitals monitoring |
| 22 | `07-Production-Architecture-And-System-Design/React-Next-Frontend-System-Design-Gold-Sheet.md` | System design template (10 dimensions), real-time collaborative editor design, large-scale e-commerce search design |

Target:
- You can speak like a senior frontend architect.
- You can connect UI patterns to distributed systems, caching, release risk, and observability.

---

## 8. Real Projects And MAANG Interview Prep

| Order | File | What It Builds |
|---:|---|---|
| 23 | `08-Projects-And-Interview-Prep/React-Next-Real-World-Projects-Gold-Sheet.md` | SaaS dashboard, ecommerce, GenAI chat UI, design decisions |
| 24 | `08-Projects-And-Interview-Prep/React-Next-MAANG-Interview-Preparation-Gold-Sheet.md` | Frontend system design questions, architecture explanation, trade-offs, debugging strategies |

Target:
- You can use the theory to design realistic applications.
- You can answer interview questions with crisp structure and production maturity.

---

## 9. Practice Upgrade

| Order | File | What It Builds |
|---:|---|---|
| 33 | `09-Practice-Upgrade/React-NextJS-Tricky-Behavior-Questions-Gold-Sheet.md` | 15 code-snippet gotchas: batching, stale closures, mutation, index keys, hydration, fetch deduplication |
| 34 | `09-Practice-Upgrade/React-NextJS-LLD-Machine-Coding-Design-Gold-Sheet.md` | 4 complete LLD designs with code: infinite scroll, multi-step form, real-time search + URL sync, feature flag dashboard |
| 35 | `09-Practice-Upgrade/React-NextJS-Production-Debugging-Case-Studies-Gold-Sheet.md` | 5 narrative case studies: hydration mismatch, memory leak, ISR stale data, bundle bloat, Next.js 15 async params error |
| 36 | `09-Practice-Upgrade/React-NextJS-Active-Recall-Question-Bank.md` | 30 cover-and-answer questions: 🟢 Foundation / 🟡 Intermediate / 🔴 MAANG tiers |
| 37 | `09-Practice-Upgrade/React-NextJS-Runnable-Mini-Labs.md` | 7 timed labs: F1/F2/F3 (5 min each), I1/I2 (15 min each), S1/S2 (30 min each) with reference solutions |
| 38 | `09-Practice-Upgrade/React-NextJS-Mock-Interview-Scripts.md` | 5 timed mock rounds: Foundations (25 min), Hooks+State (30 min), Next.js Architecture (35 min), Performance+Debugging (30 min), System Design (40 min) |
| 39 | `09-Practice-Upgrade/React-NextJS-Interview-Scoring-Rubrics.md` | 1-5 rubrics for 10 topics; readiness gates: Starter Ready / Mid-Level Ready / Senior Ready / MAANG Ready |
| 40 | `09-Practice-Upgrade/React-NextJS-2-Week-4-Week-Mastery-Roadmaps.md` | Day-by-day 2-week sprint (2–2.5 hrs/day) and 4-week deep mastery (45–60 min/day) plans |

Target:
- You practice recognizing bugs in code snippets before the interview does.
- You build LLD components under time pressure.
- You can narrate real incident stories with root cause, fix, and prevention.
- You know exactly what day to study what — no guessing.

---

## Interview Answer Pattern

Use this for most React/Next.js answers:

1. Define the concept.
2. Explain the mental model.
3. Show how it works in code or request flow.
4. State why it exists.
5. Explain trade-offs.
6. Mention common mistakes.
7. Close with production judgment.

Example:

```text
Hydration is the process where React attaches event handlers and client-side
behavior to HTML that was already rendered by the server. It improves first paint
and SEO compared with pure CSR, but it can fail if the server-rendered HTML does
not match the first client render. In production, I avoid browser-only values
during server render, isolate client-only logic behind effects or client
components, and monitor hydration warnings because they can cause broken UI.
```

---

## Official Docs Checked

- React Docs: https://react.dev/
- React Render and Commit: https://react.dev/learn/render-and-commit
- React Hooks Reference: https://react.dev/reference/react
- React Suspense: https://react.dev/reference/react/Suspense
- React Portals: https://react.dev/reference/react-dom/createPortal
- Next.js App Router: https://nextjs.org/docs/app
- Next.js Server and Client Components: https://nextjs.org/docs/app/getting-started/server-and-client-components
- Next.js Caching: https://nextjs.org/docs/app/guides/caching-without-cache-components
- Next.js Data Fetching: https://nextjs.org/docs/app/getting-started/fetching-data
- Next.js Server Actions: https://nextjs.org/docs/app/guides/forms
- React Router Routing: https://reactrouter.com/start/data/routing
- Redux Toolkit: https://redux-toolkit.js.org/introduction/getting-started
- TanStack Query: https://tanstack.com/query/latest/docs/framework/react/overview
- SWR: https://swr.vercel.app/docs/getting-started


Audience:
- You know JavaScript or TypeScript basics.
- You want React and Next.js from browser fundamentals to production architecture.
- You want modular notes like the Python and React Native tracks.
- You want beginner clarity, senior-level trade-offs, code intuition, and interview-ready answers.

Current baseline checked on June 27, 2026:
- React docs show React `19.2`.
- Next.js docs show latest `16.2.9`.
- Next.js App Router is the primary modern architecture path.
- React Router docs show latest `8.0.1`, while v6+ data-router concepts remain important for interviews and existing apps.

Goal:
- Build deep React intuition from browser rendering and reconciliation to hooks, Fiber, Suspense, and production patterns.
- Master modern Next.js App Router: Server Components, Client Components, Server Actions, layouts, rendering strategies, caching, revalidation, and BFF design.
- Prepare for MAANG frontend system design, debugging rounds, real-world projects, and production architecture decisions.

---

## How To Read These Notes

Read in order if React/Next.js is new. If you already build React apps, jump to Group 5 onward for modern Next.js, caching, performance, production, and system design.

Every sheet follows this rhythm:

```text
Intuition
Definition
Implementation model
Code example
Trade-offs
Real-world use cases
Mistakes to avoid
Interview answer
Revision notes
```

---

## 1. Web And Rendering Foundations

| Order | File | What It Builds |
|---:|---|---|
| 1 | `01-Web-Rendering-Foundations/React-Next-Web-Rendering-Fundamentals-Gold-Sheet.md` | DOM, CSSOM, render tree, critical rendering path, event loop, hydration, SPA/MPA/hybrid, CSR/SSR/SSG/ISR |

Target:
- You can explain what the browser does before React enters the picture.
- You can compare virtual DOM, real DOM, hydration, and rendering strategies clearly.

---

## 2. React Core And Hooks

| Order | File | What It Builds |
|---:|---|---|
| 2 | `02-React-Core-And-Hooks/React-Core-Fundamentals-Gold-Sheet.md` | JSX, components, props, state, class vs function, lifecycle, reconciliation, keys, controlled/uncontrolled |
| 3 | `02-React-Core-And-Hooks/React-Hooks-Complete-Gold-Sheet.md` | `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, `useReducer`, custom hooks, hook rules |
| 4 | `02-React-Core-And-Hooks/React-Advanced-Internals-Fiber-Concurrency-Suspense-Gold-Sheet.md` | Fiber, concurrent rendering, batching, Suspense, lazy loading, error boundaries, portals |

Target:
- You understand React rendering as trigger, render, commit.
- You can explain hooks without stale-closure confusion.
- You can discuss React internals at a high level without overclaiming implementation details.

---

## 3. React Architecture And Routing

| Order | File | What It Builds |
|---:|---|---|
| 5 | `03-React-Architecture-And-Routing/React-Routing-SPA-Next-Router-Gold-Sheet.md` | React Router data APIs, nested routes, lazy route loading, Next file routing, dynamic routes, route groups |
| 21 | `03-React-Architecture-And-Routing/React-Advanced-Frontend-Patterns-Gold-Sheet.md` | Compound components, render props, HOCs, controlled/uncontrolled component APIs |

Target:
- You can design navigation for SPA and Next.js apps.
- You can choose component patterns based on API ergonomics and long-term maintainability.

---

## 4. Forms, Client State, And Server State

| Order | File | What It Builds |
|---:|---|---|
| 6 | `04-Forms-State-Server-State/React-Forms-Complete-Gold-Sheet.md` | Controlled/uncontrolled forms, validation, React Hook Form, Formik, large-form performance |
| 7 | `04-Forms-State-Server-State/React-State-Management-Full-Spectrum-Gold-Sheet.md` | Local/global decision, Context limitations, Redux, Redux Toolkit, Zustand, Jotai, Recoil |
| 8 | `04-Forms-State-Server-State/React-Server-State-TanStack-SWR-Gold-Sheet.md` | Server state vs client state, TanStack Query, cache, background refetch, mutations, SWR, consistency |

Target:
- You classify state before choosing a library.
- You separate form state, UI state, global client state, and server cache.

---

## 5. Next.js App Router And Data

| Order | File | What It Builds |
|---:|---|---|
| 9 | `05-NextJS-App-Router-And-Data/NextJS-App-Router-Core-Gold-Sheet.md` | App Router, Server Components, Client Components, `use client`, `use server`, layouts, templates |
| 10 | `05-NextJS-App-Router-And-Data/NextJS-Rendering-Strategies-Deep-Dive-Gold-Sheet.md` | SSR, SSG, ISR, CSR, streaming, partial pre-rendering, when to use each |
| 11 | `05-NextJS-App-Router-And-Data/NextJS-Data-Fetching-Architecture-Gold-Sheet.md` | `getStaticProps`, `getServerSideProps`, App Router async components, Server Actions, API routes, BFF |
| 12 | `05-NextJS-App-Router-And-Data/NextJS-Caching-Revalidation-Performance-Gold-Sheet.md` | HTTP cache, CDN cache, Next cache layers, revalidation, memoization, request deduplication |

Target:
- You can design modern App Router systems and still explain legacy Pages Router APIs.
- You can choose rendering and caching strategies with business constraints in mind.

---

## 6. Performance, Security, Styling, And Testing

| Order | File | What It Builds |
|---:|---|---|
| 13 | `06-Performance-Security-Styling-Testing/React-Next-Authentication-Security-Gold-Sheet.md` | Cookies vs tokens, session management, NextAuth basics, route protection, XSS, CSRF |
| 14 | `06-Performance-Security-Styling-Testing/React-Next-Styling-UI-A11y-Gold-Sheet.md` | CSS fundamentals, Flexbox, Grid, Tailwind, CSS Modules, styled-components, design systems, accessibility |
| 15 | `06-Performance-Security-Styling-Testing/React-Next-Performance-Optimization-Gold-Sheet.md` | Code splitting, lazy loading, tree shaking, bundle analysis, images, Web Vitals |
| 16 | `06-Performance-Security-Styling-Testing/React-Next-Testing-Gold-Sheet.md` | Jest, React Testing Library, integration tests, Cypress, Playwright |

Target:
- You can ship secure, styled, accessible, tested, measurable apps.
- You can debug performance using Web Vitals and bundle/runtime evidence.

---

## 7. Production Architecture And System Design

| Order | File | What It Builds |
|---:|---|---|
| 17 | `07-Production-Architecture-And-System-Design/React-Next-Realtime-Interactive-Systems-Gold-Sheet.md` | WebSockets, polling, server-sent events awareness, optimistic UI |
| 18 | `07-Production-Architecture-And-System-Design/React-Next-Deployment-Infrastructure-Gold-Sheet.md` | Vercel, CI/CD, environment variables, Edge functions |
| 19 | `07-Production-Architecture-And-System-Design/React-Next-Production-Architecture-Gold-Sheet.md` | Large-app folder structure, feature architecture, component design, monorepo, Turborepo |
| 20 | `07-Production-Architecture-And-System-Design/React-Next-Error-Handling-Observability-Gold-Sheet.md` | Logging, monitoring, Sentry, error boundaries, API failure handling |
| 22 | `07-Production-Architecture-And-System-Design/React-Next-Frontend-System-Design-Gold-Sheet.md` | Scalable frontend design, SSR/CSR/ISR trade-offs, data flow, caching, performance vs scalability |

Target:
- You can speak like a senior frontend architect.
- You can connect UI patterns to distributed systems, caching, release risk, and observability.

---

## 8. Real Projects And MAANG Interview Prep

| Order | File | What It Builds |
|---:|---|---|
| 23 | `08-Projects-And-Interview-Prep/React-Next-Real-World-Projects-Gold-Sheet.md` | SaaS dashboard, ecommerce, GenAI chat UI, design decisions |
| 24 | `08-Projects-And-Interview-Prep/React-Next-MAANG-Interview-Preparation-Gold-Sheet.md` | Frontend system design questions, architecture explanation, trade-offs, debugging strategies |

Target:
- You can use the theory to design realistic applications.
- You can answer interview questions with crisp structure and production maturity.

---

## Interview Answer Pattern

Use this for most React/Next.js answers:

1. Define the concept.
2. Explain the mental model.
3. Show how it works in code or request flow.
4. State why it exists.
5. Explain trade-offs.
6. Mention common mistakes.
7. Close with production judgment.

Example:

```text
Hydration is the process where React attaches event handlers and client-side
behavior to HTML that was already rendered by the server. It improves first paint
and SEO compared with pure CSR, but it can fail if the server-rendered HTML does
not match the first client render. In production, I avoid browser-only values
during server render, isolate client-only logic behind effects or client
components, and monitor hydration warnings because they can cause broken UI.
```

---

## Official Docs Checked

- React Docs: https://react.dev/
- React Render and Commit: https://react.dev/learn/render-and-commit
- React Hooks Reference: https://react.dev/reference/react
- React Suspense: https://react.dev/reference/react/Suspense
- React Portals: https://react.dev/reference/react-dom/createPortal
- Next.js App Router: https://nextjs.org/docs/app
- Next.js Server and Client Components: https://nextjs.org/docs/app/getting-started/server-and-client-components
- Next.js Caching: https://nextjs.org/docs/app/guides/caching-without-cache-components
- Next.js Data Fetching: https://nextjs.org/docs/app/getting-started/fetching-data
- Next.js Server Actions: https://nextjs.org/docs/app/guides/forms
- React Router Routing: https://reactrouter.com/start/data/routing
- Redux Toolkit: https://redux-toolkit.js.org/introduction/getting-started
- TanStack Query: https://tanstack.com/query/latest/docs/framework/react/overview
- SWR: https://swr.vercel.app/docs/getting-started

