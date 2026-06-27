# Next.js Rendering Strategies Deep Dive - Gold Sheet

> Track File #10 of 24 - Group 5: Next.js App Router And Data
> Covers: SSR, SSG, ISR, CSR, partial pre-rendering, streaming, when to use each

---

## 1. Intuition

Rendering strategy decides when and where HTML is produced.

```text
Build time? Request time? Browser time? Static shell plus streamed dynamic parts?
```

A senior answer chooses per route, not one strategy for the whole application.

---

## 2. Strategy Comparison

| Strategy | HTML Created | Best For | Main Cost |
|---|---|---|---|
| SSG | build time | docs, marketing, stable pages | stale until rebuild/revalidate |
| ISR | build plus regeneration | catalogs, blogs, CMS pages | invalidation complexity |
| SSR | request time | personalized/fresh data | server latency/cost |
| CSR | browser | private interactive apps | slower first content/SEO |
| Streaming | server sends chunks | mixed slow/fast sections | boundary design |
| PPR | static shell plus dynamic holes | hybrid pages | platform/support complexity |

---

## 3. SSR

Use SSR when each request needs fresh or personalized HTML.

Examples:
- authenticated dashboard shell
- request-specific recommendations
- geo/personalized landing page

Trade-offs:
- higher server cost
- slower than CDN static
- depends on backend latency
- must avoid leaking user data into shared cache

---

## 4. SSG

Use SSG when content is known at build time.

Examples:
- documentation
- marketing pages
- pricing page
- static blog article

Trade-offs:
- rebuild needed for changes
- many pages can slow build
- not good for highly personalized content

---

## 5. ISR

ISR regenerates static content after a time interval or on demand.

Example mental model:

```text
serve cached static page
  -> page becomes stale
  -> regeneration happens
  -> next users receive fresh static page
```

Use for:
- product catalog
- CMS article pages
- marketplace listings

Trade-off:
Users may briefly see stale content.

---

## 6. CSR

Use CSR for interactions that only matter after login or where SEO is not important.

Examples:
- chart filters
- admin table controls
- private settings page widgets
- collaborative editor client shell

Trade-off:
Initial meaningful content waits for JS and data fetch unless you combine with server-rendered shell.

---

## 7. Streaming And Partial Pre-Rendering

Streaming sends ready UI chunks as they become available.

```tsx
export default function Dashboard() {
  return (
    <>
      <FastSummary />
      <Suspense fallback={<ChartSkeleton />}>
        <SlowChart />
      </Suspense>
    </>
  );
}
```

Partial pre-rendering mental model:
- static shell is pre-rendered
- dynamic sections are filled later
- users see useful UI earlier

Use when:
- page has a stable shell
- some data is slow or request-specific
- you want static-like speed plus dynamic content

---

## 8. Decision Table

| Requirement | Strong Fit |
|---|---|
| SEO public page, rarely changes | SSG |
| SEO public page, changes periodically | ISR |
| Personalized HTML | SSR |
| Private interactive dashboard | SSR shell + CSR widgets |
| Slow widgets mixed with fast shell | Streaming |
| Mostly static with dynamic islands | PPR/streaming-style architecture |
| Highly interactive editor | CSR after initial shell |

---

## 9. Real-World Use Cases

- Ecommerce category: ISR with on-demand revalidation after catalog update.
- Product detail: ISR for public content, client cart button.
- Account page: SSR session check plus client interactivity.
- Blog: SSG or ISR from CMS.
- GenAI chat: server-rendered shell plus streaming response.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| SSR for every route | Expensive and slow | Use static where possible |
| SSG personalized data | Privacy risk/stale user data | Use SSR/auth boundary |
| CSR public landing page | SEO/perceived speed hit | SSG/SSR |
| One giant Suspense boundary | Whole page skeleton | Use route/widget boundaries |
| Ignoring cache invalidation | Stale business data | Define revalidation policy |

---

## 11. Strong Interview Answer

Question:
When do you use SSR, SSG, ISR, CSR, streaming, and PPR?

Strong answer:

```text
I choose per route. SSG is best for stable public pages. ISR works when public
content changes periodically but can tolerate short staleness. SSR is for
personalized or request-fresh HTML, with higher server cost. CSR is best for
private interactive experiences where SEO is not critical. Streaming helps when
some parts are slow and we want users to see the shell early. Partial
pre-rendering follows the same idea: static shell plus dynamic holes, if the
platform supports it.
```

---

## 12. Revision Notes

- One-line summary: Rendering strategy is a freshness, SEO, cost, and interactivity decision.
- Three keywords: static, request, stream.
- One interview trap: SSR is not automatically better.
- One memory trick: Static when possible, server when necessary, client when interactive.

