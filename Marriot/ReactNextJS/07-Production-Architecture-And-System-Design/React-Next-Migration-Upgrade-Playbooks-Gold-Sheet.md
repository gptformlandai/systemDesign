# React + Next.js Migration And Upgrade Playbooks - Gold Sheet

> Track File #44 - Group 7: Production Architecture And System Design
> Level: senior -> architect | CRA/Vite to Next.js, Pages to App Router, React upgrades, Next 15/16, Middleware to Proxy, Cache Components

---

## 1. Intuition

A migration is not a rewrite with nicer vocabulary. It is a risk-controlled path from old behavior to new behavior.

```text
Inventory -> compatibility plan -> thin vertical slice -> parallel run -> migrate route by route -> remove old path
```

Senior engineers migrate systems while users keep working.

---

## 2. Definition

- Definition: A migration playbook is a step-by-step strategy for moving an app between frameworks, routers, runtime models, or major versions safely.
- Category: Production architecture / delivery.
- Core idea: Reduce risk through inventory, incremental adoption, test coverage, observability, and rollback.

---

## 3. Universal Migration Checklist

```text
[ ] Inventory routes, dependencies, runtime assumptions, and APIs.
[ ] Identify business-critical flows.
[ ] Add smoke tests before changing architecture.
[ ] Create a compatibility layer if needed.
[ ] Migrate one low-risk vertical slice.
[ ] Run old and new paths side by side where possible.
[ ] Add metrics for errors, latency, Web Vitals, and conversion.
[ ] Keep rollback simple.
[ ] Remove old code only after adoption is stable.
```

---

## 4. CRA Or Vite To Next.js

Why migrate:
- SEO;
- server rendering;
- file-system routing;
- full-stack route handlers;
- Server Components;
- better data/caching architecture;
- deployment platform integration.

Plan:

```text
1. Keep existing app running.
2. Create a Next.js app shell.
3. Move shared UI and utilities first.
4. Migrate routes one at a time.
5. Replace client-only data fetches with server reads where useful.
6. Move API proxy logic into route handlers/BFF.
7. Add metadata, loading, error, not-found states.
8. Measure bundle and Web Vitals after each route.
```

Watch outs:
- browser-only libraries break in Server Components;
- environment variables change behavior;
- routing semantics differ;
- global CSS and asset paths may need changes;
- auth/session logic may need server adaptation.

---

## 5. Pages Router To App Router

Old:

```text
pages/
  index.tsx
  products/[id].tsx
  api/products.ts
```

New:

```text
app/
  page.tsx
  products/[id]/page.tsx
  api/products/route.ts
```

Migration plan:

```text
1. Keep Pages Router and App Router side by side.
2. Start with non-critical routes.
3. Move layout concerns into app/layout.tsx and nested layouts.
4. Replace getServerSideProps/getStaticProps with Server Components and fetch/data functions.
5. Replace API routes with route handlers where useful.
6. Add loading.tsx, error.tsx, not-found.tsx.
7. Re-check metadata and redirects.
8. Delete old pages after traffic is stable.
```

Mapping:

| Pages Router | App Router |
|---|---|
| `getServerSideProps` | async Server Component / dynamic route |
| `getStaticProps` | static Server Component / cache |
| `getStaticPaths` | `generateStaticParams` |
| `pages/api/*` | `app/api/*/route.ts` |
| `_app.tsx` | root `layout.tsx` |
| `_document.tsx` | root `layout.tsx` html/body |
| `next/head` | metadata API |

---

## 6. React 18/19 Upgrade

Core checks:
- `createRoot` instead of legacy render;
- Strict Mode effect behavior;
- automatic batching assumptions;
- Suspense compatibility;
- React 19 form/action hooks where adopted;
- ref as prop changes;
- library compatibility.

Playbook:

```text
1. Upgrade React and React DOM in a branch.
2. Update testing libraries and type packages.
3. Run strict typecheck and test suite.
4. Check Strict Mode warnings.
5. Smoke test forms, modals, portals, Suspense, and hydration.
6. Profile high-traffic pages.
7. Roll out with release health monitoring.
```

---

## 7. Next.js 15/16 Upgrade

Focus areas:
- async request APIs;
- App Router behavior changes;
- Turbopack defaults;
- caching model changes;
- Middleware renamed to Proxy in Next.js 16;
- Cache Components adoption;
- `next.config.ts` changes;
- third-party package compatibility.

Playbook:

```text
1. Read version upgrade guide.
2. Run official codemods where available.
3. Build with warnings treated seriously.
4. Fix async params/searchParams/cookies/headers usage.
5. Migrate middleware.ts to proxy.ts for Next.js 16.
6. Decide whether to adopt Cache Components now or later.
7. Validate critical routes with e2e tests.
8. Monitor release health by route.
```

---

## 8. Middleware To Proxy

Checklist:

```text
[ ] Rename middleware.ts to proxy.ts.
[ ] Rename exported function middleware -> proxy.
[ ] Keep matcher behavior.
[ ] Re-check runtime assumptions.
[ ] Remove DB-heavy logic if it accidentally grew there.
[ ] Verify auth redirects, CORS, locale redirects, and rewrites.
[ ] Add direct tests for protected route behavior.
```

Codemod:

```bash
npx @next/codemod@canary middleware-to-proxy .
```

---

## 9. Cache Components Migration

Previous model:

```ts
fetch(url, { next: { revalidate: 300, tags: ['products'] } });
```

Cache Components model:

```ts
import { cacheLife, cacheTag } from 'next/cache';

export async function getProducts() {
  'use cache';

  cacheLife('minutes');
  cacheTag('products');

  return fetchProducts();
}
```

Migration strategy:

```text
1. Inventory existing fetch cache and revalidation usage.
2. Enable cacheComponents in a branch.
3. Start with public deterministic data.
4. Move cache ownership into data functions.
5. Add tags and cacheLife profiles.
6. Keep request-specific data outside cached scopes.
7. Add tests for stale/fresh behavior after mutations.
```

---

## 10. Auth Migration

Auth migrations are high risk.

Checklist:

```text
[ ] Session compatibility between old and new routes.
[ ] Cookie domain, path, sameSite, secure flags.
[ ] CSRF behavior.
[ ] Route protection in Proxy and server code.
[ ] Server Actions re-authorize.
[ ] Token refresh behavior.
[ ] Logout clears all relevant cookies.
[ ] E2E tests for user A vs user B access.
```

Never migrate auth and routing architecture blindly in one huge PR.

---

## 11. Rollback Strategy

Before migration:

```text
How do we return to the previous behavior in 10 minutes?
```

Options:
- feature flag per route;
- reverse proxy route split;
- keep old app deployed;
- canary deploy;
- release rollback;
- config switch for new cache model;
- package version rollback.

---

## 12. Common Mistakes

| Mistake | Why It Hurts | Better Approach |
|---|---|---|
| Big-bang rewrite | Hard to debug and rollback | Route-by-route migration |
| No baseline tests | You do not know what broke | Add smoke tests first |
| Migrating only happy path | Edge cases regress | Test auth/error/loading states |
| Ignoring observability | Slow failures go unseen | Add release health dashboards |
| Mixing cache changes with UI rewrite | Debugging gets muddy | Separate architecture changes |
| Deleting old code too early | Rollback impossible | Remove after stable traffic |

---

## 13. Practical Question

> You need to migrate a large Pages Router ecommerce app to App Router without disrupting checkout. What is your plan?

---

## 14. Strong Answer

```text
I would avoid a big-bang rewrite. First I would inventory routes, add smoke and
e2e tests around checkout, product pages, auth, and search, then introduce App
Router side by side. I would migrate low-risk public routes first, map
getStaticProps/getServerSideProps to Server Components and data functions, and
add route-level loading/error/not-found states. Checkout and auth would move
last, with feature flags, release health metrics, and rollback ready. I would
separate routing migration from cache-model migration unless there is a strong
reason to combine them.
```

---

## 15. Revision Notes

- One-line summary: Safe migrations are incremental, observable, and reversible.
- Three keywords: inventory, slice, rollback.
- One interview trap: A rewrite is not a migration strategy.
- One memory trick: Migrate behavior, then architecture, then cleanup.

