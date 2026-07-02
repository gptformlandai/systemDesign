# React + Next.js MAANG Interview Preparation - Gold Sheet

> Track Module - Group 8: Projects And Interview Prep
> Covers: frontend system design questions, architecture decisions, trade-off techniques, debugging strategies

---

## 1. Answer Framework

Use this structure:

```text
1. Clarify requirements.
2. Identify users, traffic, devices, SEO, freshness, auth.
3. Choose rendering strategy per route.
4. Define data ownership and state management.
5. Define caching/revalidation.
6. Discuss performance and Web Vitals.
7. Discuss security.
8. Discuss error handling and observability.
9. Discuss trade-offs and alternatives.
```

---

## 2. Common Frontend System Design Questions

- Design an ecommerce frontend.
- Design a SaaS analytics dashboard.
- Design a news feed.
- Design a GenAI chat app.
- Design a design system.
- Design a real-time collaboration UI.
- Design authentication and route protection in Next.js.
- Design caching for product catalog pages.
- Design a migration from CSR React to Next.js App Router.
- Debug a slow React page.

---

## 3. Trade-off Explanation Techniques

Use this pattern:

```text
I would choose X because requirement A matters most.
The trade-off is Y.
If requirement B changes, I would switch to Z.
```

Example:

```text
I would use ISR for product pages because they need SEO and high cache hit ratio
but can tolerate short staleness. The trade-off is cache invalidation complexity.
If pricing must be strictly real-time per user, I would render price dynamically
or fetch it client/server per request instead of caching it publicly.
```

---

## 4. Debugging Strategy

Slow page:

```text
1. Check Web Vitals: LCP, CLS, INP.
2. Inspect network waterfall and TTFB.
3. Analyze bundle and hydration cost.
4. Profile React renders.
5. Check image/font loading.
6. Check server/API latency.
7. Reproduce in production build.
```

Hydration bug:

```text
1. Identify mismatch warning.
2. Compare server HTML vs first client render.
3. Look for time/random/browser-only values.
4. Move client-only logic to effect/client component.
5. Verify no user-specific data leaked through cache.
```

Stale data:

```text
1. Identify cache layer.
2. Check query key/path/tag/TTL.
3. Check mutation invalidation.
4. Check CDN/browser headers.
5. Add targeted revalidation.
```

---

## 5. High-Frequency Interview Traps

| Trap | Better Answer |
|---|---|
| "SSR is always best" | choose per route |
| "Virtual DOM makes React fast" | declarative diffing, not magic |
| "Context replaces all state libraries" | context has rerender limitations |
| "Server Components can use hooks" | server components cannot use state/effects |
| "Client validation is enough" | server validates truth |
| "Cache everything" | scope and privacy matter |
| "Hydration is same as rendering" | hydration attaches behavior to server HTML |
| "Optimistic UI for payments" | wait for server confirmation |
| "E2E tests cover everything" | expensive, use pyramid |
| "Frontend system design is just UI" | includes rendering, data, security, cache, observability |

---

## 6. Strong Mini Answers

### Hydration

```text
Hydration attaches React event handlers and client behavior to server-rendered
HTML. It can fail when server and first client render differ, often due to dates,
random values, browser-only APIs, or user-specific cached content.
```

### Server Components

```text
Server Components render on the server, can fetch data and access server-only
resources, and do not ship component JavaScript to the browser. Client Components
are needed for interactivity, state, effects, and browser APIs.
```

### State Choice

```text
Local state stays local, URL state goes to route/search params, server state goes
to TanStack Query/SWR or server rendering, and complex global workflows may use
Redux Toolkit or Zustand depending on scale and conventions.
```

### Caching

```text
I separate browser, CDN, Next output/data cache, request memoization, and client
query cache. Public static content can be cached aggressively. User-specific data
must be private or no-store. Mutations need targeted path/tag invalidation.
```

---

## 7. MAANG Readiness Checklist

You are ready when you can explain:
- browser rendering and event loop
- hydration and mismatch causes
- CSR/SSR/SSG/ISR/streaming/PPR trade-offs
- React render/commit/reconciliation
- hooks and stale closures
- Fiber/concurrent rendering at high level
- Suspense, lazy, portals, error boundaries
- React Router and Next App Router routing
- form architecture and large-form performance
- client state vs server state
- Redux Toolkit, Zustand, Jotai, Recoil concepts
- TanStack Query/SWR cache and mutations
- Server Components and Client Components
- Server Actions and route handlers
- Next caching and revalidation
- auth, XSS, CSRF, route protection
- styling, design systems, accessibility
- Web Vitals and bundle analysis
- testing pyramid
- realtime and optimistic UI
- deployment, env vars, Edge runtime
- production architecture and monorepos
- observability and error handling
- real project designs

---

## 8. Final Strong Close

```text
For React and Next.js, I think in layers: browser rendering, React render/commit,
server/client boundaries, data ownership, cache policy, and user experience.
The senior skill is not knowing one feature in isolation. It is choosing the right
rendering, state, caching, security, and performance strategy for each route and
being able to explain the trade-offs clearly.
```

