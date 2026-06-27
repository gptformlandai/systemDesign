# React + Next.js Frontend System Design - Gold Sheet

> Track File #22 of 24 - Group 7: Production Architecture And System Design
> Covers: scalable frontend apps, SSR vs CSR vs ISR trade-offs, data flow, caching decisions, performance vs scalability

---

## 1. Intuition

Frontend system design is distributed systems thinking at the user edge.

```text
browser
  -> CDN
  -> Next server/edge
  -> BFF
  -> backend APIs
  -> cache/state/UI
```

A senior frontend design balances UX, SEO, performance, security, cost, and team velocity.

---

## 2. Design Framework

Use this order:

```text
1. Requirements
2. Users/devices/network
3. Rendering strategy
4. Data ownership and flow
5. State/caching
6. Performance budget
7. Security/auth
8. Reliability/offline/realtime
9. Observability
10. Team architecture
```

---

## 3. SSR vs CSR vs ISR Trade-offs

| Need | Prefer |
|---|---|
| public SEO | SSG/ISR/SSR |
| highly personalized | SSR |
| static docs | SSG |
| changing catalog | ISR |
| private app shell | SSR + CSR |
| heavy interaction | CSR island |
| low server cost | static/CDN where possible |

Strong phrase:
Static where possible, dynamic where necessary, client-side where interaction demands it.

---

## 4. Data Flow Architecture

Example ecommerce:

```text
Product listing:
  Next server fetches catalog -> ISR cache -> CDN -> browser

Cart:
  client store for immediate UI
  server/session source of truth
  mutation invalidates cart

Checkout:
  server action/API route validates
  backend payment service confirms
```

Rule:
Do not let frontend-only state become source of truth for critical business data.

---

## 5. Caching Strategy Decisions

Ask:
- Is data public or private?
- How stale can it be?
- What invalidates it?
- Who pays for recompute?
- What happens during backend failure?

Use:
- CDN for public stable assets/pages.
- ISR for public content with tolerable staleness.
- no-store/private for user data.
- TanStack Query/SWR for client-side server state.
- tags/path revalidation after mutations.

---

## 6. Performance vs Scalability

Performance:
- user-perceived speed
- Web Vitals
- hydration cost
- bundle size

Scalability:
- server load
- CDN hit ratio
- backend fanout
- cache invalidation
- build time

Trade-off example:
SSR fresh page may improve correctness but increase server load. ISR may reduce load but introduce staleness.

---

## 7. Real-World System Design Examples

### News Homepage

- ISR for article list.
- On-demand revalidate when CMS publishes.
- CDN cache.
- Client personalization island.

### SaaS Dashboard

- SSR auth/session shell.
- Client server-state queries for widgets.
- WebSocket for live alerts if needed.
- Feature-based architecture.

### Ecommerce

- ISR catalog.
- SSR/Server Action checkout.
- client cart drawer with server reconciliation.
- image optimization/CDN.

---

## 8. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| One rendering mode for all pages | ignores requirements | choose per route |
| Frontend cart as only truth | data loss/fraud risk | server/session truth |
| No cache invalidation story | stale bugs | define TTL/tags/events |
| Ignoring old browsers/devices | bad UX | performance budgets |
| No observability | cannot debug | Web Vitals/errors/traces |

---

## 9. Strong Interview Answer

Question:
Design a scalable frontend architecture for an ecommerce app.

Strong answer:

```text
I would use Next.js with ISR for public catalog and product pages because they
need SEO and can tolerate controlled staleness. Product updates trigger tag/path
revalidation. The cart has immediate client UI but reconciles with a server/session
source of truth. Checkout uses server-side validation and payment confirmation,
not optimistic success. Images use Next/CDN optimization. Auth uses secure cookies.
Observability covers Web Vitals, API errors, checkout failures, and release
version. Code is feature-based with shared design-system primitives.
```

---

## 10. Revision Notes

- One-line summary: Frontend system design chooses rendering, data, cache, and UX trade-offs per product requirement.
- Three keywords: rendering, data flow, cache.
- One interview trap: Frontend architecture includes security and observability.
- One memory trick: Public pages love CDN; private data hates public cache.

