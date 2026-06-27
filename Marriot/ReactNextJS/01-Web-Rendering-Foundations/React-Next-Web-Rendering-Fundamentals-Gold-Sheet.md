# Web And Rendering Fundamentals - Gold Sheet

> Track File #1 of 24 - Group 1: Web And Rendering Foundations
> Covers: DOM, CSSOM, render tree, critical rendering path, virtual DOM, hydration, event loop, SPA/MPA/hybrid, CSR/SSR/SSG/ISR

---

## 1. Intuition

Before React or Next.js, the browser is a rendering engine.

```text
HTML -> DOM
CSS -> CSSOM
DOM + CSSOM -> render tree
render tree -> layout
layout -> paint
paint -> composite pixels on screen
```

React changes how we describe UI. It does not remove browser rendering cost.

---

## 2. Browser Rendering

| Step | What Happens | Performance Risk |
|---|---|---|
| Parse HTML | Browser builds DOM | Large HTML blocks parsing |
| Parse CSS | Browser builds CSSOM | CSS blocks render |
| Build render tree | Visible DOM nodes plus styles | Hidden nodes excluded |
| Layout | Compute element sizes/positions | Reflow from layout reads/writes |
| Paint | Fill pixels, text, shadows, images | Expensive shadows/large images |
| Composite | Layers combined by GPU | Too many layers can hurt memory |

Critical Rendering Path:

```text
network -> HTML -> CSS/JS discovery -> CSSOM/DOM -> render tree -> layout -> paint
```

Production insight:
The fastest React code cannot save a page that ships huge blocking CSS, huge JS, unoptimized fonts, and slow server responses.

---

## 3. Event Loop

Browser JavaScript runs on an event loop.

```text
call stack
  -> microtasks: Promise callbacks, queueMicrotask
  -> rendering opportunity
  -> macrotasks/tasks: timers, events, network callbacks
```

Example:

```js
console.log('A');

setTimeout(() => console.log('B'), 0);

Promise.resolve().then(() => console.log('C'));

console.log('D');

// A, D, C, B
```

Interview insight:
Microtasks run before timers. Heavy JS blocks rendering and user input because the browser cannot paint while the main thread is busy.

---

## 4. Real DOM vs Virtual DOM

Real DOM:
- Browser's actual document tree.
- Mutations can trigger layout/paint work.

Virtual DOM:
- A lightweight representation of desired UI.
- React compares render outputs and commits needed DOM changes.

Important:
Virtual DOM is not automatically faster than all manual DOM code. It gives a declarative programming model and predictable UI updates. Performance still depends on component structure, render cost, DOM size, and browser work.

---

## 5. Hydration

Hydration means React attaches client-side behavior to server-rendered HTML.

```text
server renders HTML
  -> browser displays HTML
  -> JS bundle loads
  -> React hydrates
  -> event handlers and client state become active
```

Why it exists:
- Better initial HTML for SEO and first paint.
- Client-side interactivity after JS loads.

Hydration problems:
- Server HTML differs from first client render.
- Client-only APIs used during server render.
- Random values, dates, locale differences.
- User-specific content cached incorrectly.
- Too much JS delays time to interactive.

Safe pattern:

```tsx
'use client';

import {useEffect, useState} from 'react';

export function ClientTime() {
  const [time, setTime] = useState<string | null>(null);

  useEffect(() => {
    setTime(new Date().toLocaleTimeString());
  }, []);

  return <span>{time ?? 'Loading...'}</span>;
}
```

---

## 6. SPA vs MPA vs Hybrid

| Model | Meaning | Strength | Weakness |
|---|---|---|---|
| SPA | One shell, client-side navigation | App-like UX | JS-heavy, SEO/first load concerns |
| MPA | Server returns new document per page | Simple, SEO-friendly | Full reload navigation |
| Hybrid | Mix of server-rendered and client-interactive routes | Best fit per route | More architectural complexity |

Modern Next.js is hybrid by design.

---

## 7. CSR vs SSR vs SSG vs ISR

| Strategy | When HTML Is Created | Best For | Trade-off |
|---|---|---|---|
| CSR | Browser after JS loads | private dashboards, highly interactive tools | poor initial SEO/perceived speed if overused |
| SSR | per request on server | personalized or fresh pages | server cost and latency |
| SSG | build time | docs, marketing, static pages | stale until rebuild |
| ISR | static page regenerated after interval or trigger | catalogs/blogs with freshness needs | cache invalidation complexity |

Strong decision rule:
Use the least dynamic rendering model that satisfies freshness, personalization, and SEO requirements.

---

## 8. Real-World Use Cases

- Ecommerce product listing: SSG/ISR for public catalog, CSR for cart drawer.
- SaaS dashboard: SSR for auth shell, CSR/server state for private widgets.
- Blog/docs: SSG, CDN cache, minimal JS.
- Stock dashboard: SSR shell plus realtime client updates.
- GenAI chat: server-rendered shell plus streaming client/server interaction.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Thinking React skips browser layout | Browser still lays out DOM | Optimize DOM/CSS/images too |
| Hydrating mismatched HTML | Causes warnings and broken UI | Make first server/client render match |
| Using SSR for everything | Server cost and complexity | Use static/client where appropriate |
| Using CSR for public SEO pages | Poor first HTML and SEO | Use SSR/SSG/ISR |
| Blocking main thread | Input/rendering jank | Split work, defer, web workers |

---

## 10. Strong Interview Answer

Question:
Compare CSR, SSR, SSG, and ISR.

Strong answer:

```text
CSR renders primarily in the browser after JavaScript loads, which works well for
private highly interactive apps but can hurt first load and SEO. SSR renders on
each request, which helps personalization and fresh data but costs server latency.
SSG renders at build time, which is fastest and CDN-friendly but can become stale.
ISR keeps static benefits while regenerating pages after time or on-demand triggers.
In Next.js, I choose per route based on SEO, freshness, personalization, cost, and
cache invalidation complexity.
```

---

## 11. Revision Notes

- One-line summary: Browser rendering cost still matters in React and Next.js.
- Three keywords: DOM, hydration, rendering strategy.
- One interview trap: Virtual DOM is not magic performance dust.
- One memory trick: CSR runs late in browser, SSR runs per request, SSG runs at build, ISR refreshes static.

---

## 12. Core Web Vitals — Targets and How to Hit Them

Google's field quality signals that directly affect search ranking.

| Metric | Good | Needs Improvement | Poor | Measures |
|---|---|---|---|---|
| LCP | < 2.5s | 2.5–4s | > 4s | Loading performance of largest visible element |
| INP | < 200ms | 200–500ms | > 500ms | Responsiveness to user input (replaces FID) |
| CLS | < 0.1 | 0.1–0.25 | > 0.25 | Visual stability — layout shifts |

### Improving LCP

The LCP element is usually a hero image or h1 text block.

```tsx
// 1. Preload the LCP image
<Image src="/hero.jpg" alt="Hero" width={1200} height={600} priority />
// next/image with priority adds <link rel="preload"> in <head>

// 2. Avoid lazy loading above-the-fold images
// WRONG:
<img src="/hero.jpg" loading="lazy" />
// RIGHT:
<img src="/hero.jpg" loading="eager" fetchpriority="high" />

// 3. Preconnect to image CDN origin
<link rel="preconnect" href="https://cdn.example.com" />

// 4. Use next/font — eliminates render-blocking Google Fonts round trip
import { Inter } from 'next/font/google';
const inter = Inter({ subsets: ['latin'] });
```

### Improving INP

INP measures the worst interaction delay. Long JavaScript tasks block the main thread.

```tsx
// 1. Break up long synchronous work with setTimeout yield
function processLargeList(items: Item[]) {
  const CHUNK = 50;
  let index = 0;
  function processChunk() {
    for (let i = 0; i < CHUNK && index < items.length; i++, index++) {
      process(items[index]);
    }
    if (index < items.length) setTimeout(processChunk, 0);  // yield to browser
  }
  processChunk();
}

// 2. Use useTransition for expensive state updates
const [isPending, startTransition] = useTransition();
startTransition(() => setExpensiveFilter(newValue));  // non-urgent

// 3. Virtualize large lists instead of rendering 10,000 DOM nodes
// react-virtual or @tanstack/react-virtual
```

### Improving CLS

CLS penalizes unexpected layout shifts — elements jumping as page loads.

```tsx
// 1. Always provide width and height on images
<Image src="/product.jpg" width={400} height={400} alt="..." />
// Without dimensions, browser doesn't reserve space → shift when image loads

// 2. Reserve space for dynamic content with min-height
<div style={{ minHeight: 200 }}>  {/* prevents collapse while loading */}
  {loading ? <Skeleton /> : <Content />}
</div>

// 3. Never insert content above existing content
// Ads, cookie banners, and dynamic inserts above the fold cause CLS
// Use bottom banners or overlay modals instead
```

---

## 13. Resource Hints

Tell the browser to start work on resources before they're discovered in parsing.

```html
<!-- preconnect: establish connection to origin early (DNS + TCP + TLS) -->
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://api.example.com" crossorigin />

<!-- prefetch: fetch resource for future navigation (low priority) -->
<link rel="prefetch" href="/checkout" />
<!-- Use for pages the user will likely navigate to next -->

<!-- preload: fetch resource for THIS page at high priority -->
<link rel="preload" href="/hero.jpg" as="image" />
<link rel="preload" href="/fonts/inter.woff2" as="font" type="font/woff2" crossorigin />
<!-- Use for LCP images, critical fonts, hero video -->

<!-- dns-prefetch: DNS lookup only (lighter than preconnect) -->
<link rel="dns-prefetch" href="https://analytics.example.com" />
```

**In Next.js:** `next/image` with `priority` auto-generates the preload link. For other resources, add to `<Head>` or app/layout metadata.

```tsx
// app/layout.tsx
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html>
      <head>
        <link rel="preconnect" href="https://cdn.example.com" />
      </head>
      <body>{children}</body>
    </html>
  );
}
```

---

## 14. Chrome DevTools Performance Panel Walkthrough

**How to profile a slow Next.js page:**

1. Open DevTools → Performance → ⚙ Gear icon → CPU throttle 4x slowdown (simulates mobile)
2. Click Record → reload page → stop recording after page is interactive
3. Examine the timeline:
   - **Blue** = Parsing HTML/CSS
   - **Yellow** = JavaScript execution
   - **Purple** = Layout/style recalculations
   - **Green** = Paint
4. Look for **red triangles on yellow tasks** — these are long tasks (> 50ms) that block the main thread
5. Click a long task → see the call stack in the bottom panel — trace to your code
6. Find **LCP marker** in the timeline — what happened just before it? Large image load? JS parse?

**Key observations to make:**
- "Time to First Byte" (TTFB) — how fast is the server?
- Scripting % vs Rendering % vs Idle — are you JS-heavy or render-heavy?
- Network waterfall — are resources loaded sequentially that could be parallel?

---

## 15. Render-Blocking Resources

A resource is render-blocking if the browser stops building the render tree until it loads.

| Resource | Blocks? | Fix |
|---|---|---|
| `<script src="..." />` in `<head>` | Yes | Add `defer` or `async` |
| `<script src="..." defer />` | No | Executes after HTML parsed |
| `<script src="..." async />` | No | Executes when loaded, HTML parsing continues |
| CSS `<link rel="stylesheet" />` | Yes | Split critical/non-critical CSS |
| Inline `<style>` | No | Parsed as HTML |
| Web font with @font-face | Can cause FOIT | Use `font-display: swap` |

```tsx
// In Next.js: Script component handles this automatically
import Script from 'next/script';

// strategy="lazyOnload" — loads after page is interactive
<Script src="https://analytics.example.com/analytics.js" strategy="lazyOnload" />

// strategy="afterInteractive" — loads after hydration (default)
<Script src="/third-party.js" strategy="afterInteractive" />

// strategy="beforeInteractive" — needed for critical scripts only (consent banners)
<Script src="/consent.js" strategy="beforeInteractive" />
```

---

## 16. Partial Prerendering (PPR) — Next.js Experimental

PPR generates a static shell at build time with Suspense boundaries marking dynamic holes. The static shell is served instantly from the CDN; dynamic content streams in per-request.

```tsx
// next.config.ts
const nextConfig = { experimental: { ppr: true } };

// app/products/[id]/page.tsx
import { Suspense } from 'react';

export default function ProductPage({ params }: { params: { id: string } }) {
  return (
    <div>
      {/* Static — part of the prerendered shell */}
      <ProductHeader />
      
      {/* Dynamic — streams in after static shell */}
      <Suspense fallback={<PriceSkeleton />}>
        <DynamicPrice productId={params.id} />
      </Suspense>
      
      <Suspense fallback={<ReviewsSkeleton />}>
        <Reviews productId={params.id} />
      </Suspense>
    </div>
  );
}
```

**Mental model:** PPR = SSG (static shell, CDN cached) + SSR (dynamic holes, streamed per-request) in one response.

