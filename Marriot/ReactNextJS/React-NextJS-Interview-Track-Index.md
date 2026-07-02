# React + Next.js Interview Track Index

This folder is the React and Next.js mastery track for frontend, full-stack, product engineering, and MAANG-style interviews.

Audience:
- You know JavaScript or TypeScript basics.
- You want React and Next.js from first local setup to production architecture.
- You want beginner clarity, senior-level trade-offs, code intuition, and interview-ready answers.
- You want modern App Router, React Server Components, Server Actions, caching, security, testing, observability, and frontend system design.

Current baseline checked on July 2, 2026:
- React docs show React `19.2`.
- Next.js docs show latest `16.2.10`.
- Next.js App Router is the primary modern architecture path.
- Next.js 16 teaches Cache Components and `use cache` as the modern caching path.
- Next.js 16 renames/deprecates Middleware in favor of `proxy.ts`.
- React Compiler is important for modern render optimization and automatic memoization.

Track size: **50 learning modules + index/checklist = 52 markdown files, 11 groups**, beginner to PRO-ready.

---

## How To Read These Notes

If React/Next.js is new, read from Group 0 to Group 10.

If you already build React apps:
- Start at Group 5 for modern Next.js App Router, Cache Components, Proxy, and Server Actions.
- Use Group 6 for security, testing, accessibility, and UI quality.
- Use Group 7 for production architecture, observability, migration, and system design.
- Finish with Group 9 and Group 10 for interview practice and capstone validation.

Every sheet follows this rhythm:

```text
Intuition
Definition
How it works
Code example
Trade-offs
Failure modes
Common mistakes
Interview answer
Revision notes
```

---

## 0. Setup And Tooling

| Order | File | What It Builds |
|---:|---|---|
| 1 | `00-Setup-And-Tooling/React-NextJS-Developer-Environment-Gold-Sheet.md` | Node/package manager setup, create-next-app choices, TypeScript, scripts, env validation, debugging, project structure |

Target:
- You can start and explain a modern Next.js project from zero.
- You understand local workflow, CI gates, env boundaries, and first-mile debugging.

---

## 1. Web And Rendering Foundations

| Order | File | What It Builds |
|---:|---|---|
| 2 | `01-Web-Rendering-Foundations/React-Next-Web-Rendering-Fundamentals-Gold-Sheet.md` | DOM, CSSOM, render tree, critical rendering path, event loop, hydration, SPA/MPA/hybrid, CSR/SSR/SSG/ISR, Core Web Vitals, resource hints, Chrome DevTools, PPR |

Target:
- You can explain what the browser does before React enters the picture.
- You can compare virtual DOM, real DOM, hydration, and rendering strategies clearly.

---

## 2. React Core And Hooks

| Order | File | What It Builds |
|---:|---|---|
| 3 | `02-React-Core-And-Hooks/React-Core-Fundamentals-Gold-Sheet.md` | JSX, components, props, state, lifecycle, reconciliation, keys, controlled/uncontrolled, batching, refs, Strict Mode, React 19 changes |
| 4 | `02-React-Core-And-Hooks/React-Hooks-Complete-Gold-Sheet.md` | `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, `useReducer`, custom hooks, hook rules, stale closures, TypeScript |
| 5 | `02-React-Core-And-Hooks/React-Advanced-Internals-Fiber-Concurrency-Suspense-Gold-Sheet.md` | Fiber, work loop, lanes, scheduler, concurrent rendering, Suspense internals, render purity |
| 6 | `02-React-Core-And-Hooks/React-TypeScript-Deep-Dive-Gold-Sheet.md` | Props typing, events, refs, reducers, discriminated unions, generic components, Zod integration |
| 7 | `02-React-Core-And-Hooks/React-18-19-New-Features-Concurrent-Gold-Sheet.md` | Automatic batching, `flushSync`, `useTransition`, `useDeferredValue`, `useId`, `use()`, `useActionState`, `useOptimistic`, ref as prop |
| 8 | `02-React-Core-And-Hooks/React-Compiler-Auto-Memoization-Gold-Sheet.md` | React Compiler, auto memoization, Next.js `reactCompiler`, annotation mode, `use memo`, `use no memo`, adoption and debugging |
| 9 | `02-React-Core-And-Hooks/React-Custom-Hooks-Pattern-Library-Gold-Sheet.md` | Production custom hooks: async, fetch, pagination, storage, debounce, throttle, intersection observer, window size, and more |

Target:
- You understand React rendering as trigger, render, commit.
- You can explain hooks without stale-closure confusion.
- You know when manual memoization still matters in a compiler world.

---

## 3. React Architecture And Routing

| Order | File | What It Builds |
|---:|---|---|
| 10 | `03-React-Architecture-And-Routing/React-Routing-SPA-Next-Router-Gold-Sheet.md` | React Router data APIs, nested routes, lazy route loading, Next file routing, dynamic routes, route groups |
| 11 | `03-React-Architecture-And-Routing/React-Advanced-Frontend-Patterns-Gold-Sheet.md` | Compound components, render props, HOCs, controlled/uncontrolled APIs, composition trade-offs |
| 12 | `03-React-Architecture-And-Routing/React-Error-Handling-Error-Boundaries-Gold-Sheet.md` | Error zones, ErrorBoundary class component, async error state, Next.js `error.tsx`, `not-found.tsx`, `global-error.tsx`, retry |

Target:
- You can design navigation for SPA and Next.js apps.
- You can choose component patterns based on API ergonomics and maintainability.
- You can explain which errors React catches and which ones need route/server handling.

---

## 4. Forms, Client State, And Server State

| Order | File | What It Builds |
|---:|---|---|
| 13 | `04-Forms-State-Server-State/React-Forms-Complete-Gold-Sheet.md` | Controlled/uncontrolled forms, validation, React Hook Form, Formik, large-form performance |
| 14 | `04-Forms-State-Server-State/React-State-Management-Full-Spectrum-Gold-Sheet.md` | Local/global decision, Context limits, Redux Toolkit, Zustand, Jotai, RTK Query |
| 15 | `04-Forms-State-Server-State/React-Server-State-TanStack-SWR-Gold-Sheet.md` | Server state vs client state, TanStack Query, cache, background refetch, mutations, SWR, consistency |

Target:
- You classify state before choosing a library.
- You separate form state, UI state, global client state, and server cache.

---

## 5. Next.js App Router And Data

| Order | File | What It Builds |
|---:|---|---|
| 16 | `05-NextJS-App-Router-And-Data/NextJS-App-Router-Core-Gold-Sheet.md` | App Router, Server Components, Client Components, `use client`, `use server`, layouts, templates, parallel routes, intercepting routes, streaming |
| 17 | `05-NextJS-App-Router-And-Data/NextJS-Rendering-Strategies-Deep-Dive-Gold-Sheet.md` | SSR, SSG, ISR, CSR, streaming, partial pre-rendering, when to use each |
| 18 | `05-NextJS-App-Router-And-Data/NextJS-Data-Fetching-Architecture-Gold-Sheet.md` | Async Server Components, Server Actions, route handlers, BFF, `React.cache`, waterfall prevention |
| 19 | `05-NextJS-App-Router-And-Data/NextJS-Caching-Revalidation-Performance-Gold-Sheet.md` | Previous App Router cache model: Router Cache, Full Route Cache, Data Cache, Request Memoization, revalidation |
| 20 | `05-NextJS-App-Router-And-Data/NextJS-16-Cache-Components-Use-Cache-Gold-Sheet.md` | Modern Next.js 16 Cache Components, `use cache`, `cacheLife`, `cacheTag`, `updateTag`, remote cache, migration framing |
| 21 | `05-NextJS-App-Router-And-Data/NextJS-Server-Actions-Mutations-Forms-Gold-Sheet.md` | Server Actions, `useActionState`, `useFormStatus`, `useOptimistic`, uploads, auth/validate/authz inside actions |
| 22 | `05-NextJS-App-Router-And-Data/NextJS-Middleware-Edge-Runtime-Gold-Sheet.md` | Legacy Middleware patterns, Edge vs Node, auth redirects, i18n, A/B, rate limiting |
| 23 | `05-NextJS-App-Router-And-Data/NextJS-Proxy-Runtime-Migration-Gold-Sheet.md` | Modern `proxy.ts`, Middleware migration, matchers, rewrites, redirects, CORS, runtime choice |
| 24 | `05-NextJS-App-Router-And-Data/NextJS-Metadata-SEO-Structured-Data-Gold-Sheet.md` | Static/dynamic metadata, title templates, Open Graph, Twitter cards, JSON-LD, sitemap, robots, OG images |
| 25 | `05-NextJS-App-Router-And-Data/NextJS-Advanced-Routing-Parallel-Intercepting-Gold-Sheet.md` | Route groups, catch-all segments, parallel routes, intercepting routes, modal pattern, route handlers, streams |

Target:
- You can design modern App Router systems and still explain legacy Pages Router and previous cache models.
- You can choose rendering and caching strategies with business constraints in mind.
- You know Next.js 16 cache/proxy vocabulary accurately.

---

## 6. Performance, Security, Styling, And Testing

| Order | File | What It Builds |
|---:|---|---|
| 26 | `06-Performance-Security-Styling-Testing/React-Next-Authentication-Security-Gold-Sheet.md` | Cookies vs tokens, sessions, NextAuth basics, route protection, XSS, CSRF |
| 27 | `06-Performance-Security-Styling-Testing/React-Next-RSC-Server-Actions-Security-Hardening-Gold-Sheet.md` | RSC data leaks, Server Action hardening, DAL, DTOs, tainting, env boundaries, cache privacy, incident response |
| 28 | `06-Performance-Security-Styling-Testing/React-Next-Styling-UI-A11y-Gold-Sheet.md` | CSS, Flexbox, Grid, Tailwind, CSS Modules, CSS-in-JS, design systems, accessibility |
| 29 | `06-Performance-Security-Styling-Testing/React-Next-Design-System-A11y-I18n-Visual-Testing-Gold-Sheet.md` | Design tokens, headless UI, keyboard/focus, ARIA, i18n, RTL, Storybook, visual regression |
| 30 | `06-Performance-Security-Styling-Testing/React-Next-Performance-Optimization-Gold-Sheet.md` | Code splitting, lazy loading, tree shaking, bundle analysis, images, Web Vitals |
| 31 | `06-Performance-Security-Styling-Testing/React-Next-Testing-Gold-Sheet.md` | Jest, React Testing Library, integration tests, Cypress, Playwright |

Target:
- You can ship secure, styled, accessible, tested, measurable apps.
- You can discuss RSC/Server Action security at a modern production level.

---

## 7. Production Architecture And System Design

| Order | File | What It Builds |
|---:|---|---|
| 32 | `07-Production-Architecture-And-System-Design/React-Next-Realtime-Interactive-Systems-Gold-Sheet.md` | WebSockets, polling, SSE, optimistic UI, reconnect patterns |
| 33 | `07-Production-Architecture-And-System-Design/React-Next-Deployment-Infrastructure-Gold-Sheet.md` | Vercel, CI/CD, environment variables, Edge functions, Docker/Kubernetes, deployment strategy |
| 34 | `07-Production-Architecture-And-System-Design/React-Next-Production-Architecture-Gold-Sheet.md` | Feature-based structure, dependency direction, monorepo, Turborepo, architecture governance |
| 35 | `07-Production-Architecture-And-System-Design/React-Next-Error-Handling-Observability-Gold-Sheet.md` | Error routes, Sentry integration, retry backoff, Web Vitals monitoring |
| 36 | `07-Production-Architecture-And-System-Design/React-Next-Production-Observability-RUM-SLO-Gold-Sheet.md` | RUM, Web Vitals, traces, OpenTelemetry, logs, release health, SLOs, privacy |
| 37 | `07-Production-Architecture-And-System-Design/React-Next-Frontend-System-Design-Gold-Sheet.md` | Frontend system design template, collaborative editor, ecommerce search, architecture communication |
| 38 | `07-Production-Architecture-And-System-Design/React-Next-Migration-Upgrade-Playbooks-Gold-Sheet.md` | CRA/Vite to Next.js, Pages to App Router, React upgrade, Next 15/16, Middleware to Proxy, Cache Components |
| 39 | `07-Production-Architecture-And-System-Design/React-Next-PWA-Offline-Resilience-Gold-Sheet.md` | PWA, service workers, offline UX, IndexedDB, sync queues, conflict handling, resilient fetch |

Target:
- You can speak like a senior frontend architect.
- You can connect UI decisions to reliability, observability, rollout risk, and distributed system behavior.

---

## 8. Real Projects And MAANG Interview Prep

| Order | File | What It Builds |
|---:|---|---|
| 40 | `08-Projects-And-Interview-Prep/React-Next-Real-World-Projects-Gold-Sheet.md` | SaaS dashboard, ecommerce, GenAI chat UI, design decisions |
| 41 | `08-Projects-And-Interview-Prep/React-Next-MAANG-Interview-Preparation-Gold-Sheet.md` | Frontend system design questions, architecture explanation, trade-offs, debugging strategies |

Target:
- You can use the theory to design realistic applications.
- You can answer interview questions with crisp structure and production maturity.

---

## 9. Practice Upgrade

| Order | File | What It Builds |
|---:|---|---|
| 42 | `09-Practice-Upgrade/React-NextJS-Tricky-Behavior-Questions-Gold-Sheet.md` | Code-snippet gotchas: batching, stale closures, mutation, keys, hydration, fetch dedupe |
| 43 | `09-Practice-Upgrade/React-NextJS-LLD-Machine-Coding-Design-Gold-Sheet.md` | LLD designs: infinite scroll, multi-step form, real-time search, feature flag dashboard |
| 44 | `09-Practice-Upgrade/React-NextJS-Production-Debugging-Case-Studies-Gold-Sheet.md` | Hydration mismatch, memory leak, ISR stale data, bundle bloat, async params error |
| 45 | `09-Practice-Upgrade/React-NextJS-Active-Recall-Question-Bank.md` | Foundation/intermediate/MAANG active recall questions |
| 46 | `09-Practice-Upgrade/React-NextJS-Runnable-Mini-Labs.md` | Timed labs with reference solutions |
| 47 | `09-Practice-Upgrade/React-NextJS-Mock-Interview-Scripts.md` | Timed mock rounds for foundations, hooks/state, Next architecture, performance, system design |
| 48 | `09-Practice-Upgrade/React-NextJS-Interview-Scoring-Rubrics.md` | Rubrics and readiness gates |
| 49 | `09-Practice-Upgrade/React-NextJS-2-Week-4-Week-Mastery-Roadmaps.md` | Day-by-day study plans |

Target:
- You practice recognizing bugs before the interview exposes them.
- You build LLD components under time pressure.
- You can narrate real incident stories with root cause, fix, and prevention.

---

## 10. Capstone

| Order | File | What It Builds |
|---:|---|---|
| 50 | `10-Capstone/React-NextJS-Production-Capstone-App.md` | End-to-end Team Operations Dashboard capstone: routes, auth, DAL/DTO, Server Actions, Cache Components, UI system, tests, observability, production review |

Target:
- You can prove the full track by building and explaining one realistic production-style application.

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

## Modern Next.js Interview Corrections

Use these corrections in 2026+ interviews:

- Say `proxy.ts` first for modern request-boundary logic; mention Middleware for older projects.
- Say Cache Components / `use cache` first for modern Next.js 16 caching; mention the previous cache model for existing codebases.
- Treat Server Actions as externally callable mutation endpoints.
- Treat Server Components as server-side rendering units, not automatic data-leak protection.
- Mention React Compiler when discussing memoization strategy, but do not claim it replaces architecture or profiling.

---

## Official Docs Checked

- React Docs: https://react.dev/
- React Compiler: https://react.dev/learn/react-compiler
- React Hooks Reference: https://react.dev/reference/react
- React Suspense: https://react.dev/reference/react/Suspense
- Next.js Docs: https://nextjs.org/docs
- Next.js Installation: https://nextjs.org/docs/app/getting-started/installation
- Next.js Cache Components: https://nextjs.org/docs/app/getting-started/caching
- Next.js Proxy: https://nextjs.org/docs/app/api-reference/file-conventions/proxy
- Next.js Data Security: https://nextjs.org/docs/app/guides/data-security
- Next.js React Compiler Config: https://nextjs.org/docs/app/api-reference/config/next-config-js/reactCompiler
- Next.js Internationalization: https://nextjs.org/docs/app/guides/internationalization
- Next.js OpenTelemetry: https://nextjs.org/docs/app/guides/open-telemetry
- Next.js PWAs: https://nextjs.org/docs/app/guides/pwas
- React Router Routing: https://reactrouter.com/start/data/routing
- Redux Toolkit: https://redux-toolkit.js.org/introduction/getting-started
- TanStack Query: https://tanstack.com/query/latest/docs/framework/react/overview
- SWR: https://swr.vercel.app/docs/getting-started

