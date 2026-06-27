# Next.js App Router Core - Gold Sheet

> Track File #9 of 24 - Group 5: Next.js App Router And Data
> Covers: App Router architecture, Server Components, Client Components, `use client`, `use server`, layouts, templates, file-based routing

---

## 1. Intuition

Next.js App Router lets you build a route tree where server-rendered UI, client interactivity, layouts, loading states, errors, and data fetching live together.

```text
app route segment
  -> layout
  -> page
  -> loading/error/not-found
  -> server/client component boundary
```

Default mindset:
Server Components first. Client Components only where interactivity or browser APIs are needed.

---

## 2. App Router Architecture

```text
app/
  layout.tsx
  page.tsx
  dashboard/
    layout.tsx
    loading.tsx
    error.tsx
    page.tsx
  users/
    [userId]/
      page.tsx
```

Key conventions:
- `layout.tsx`: shared UI that persists across navigation.
- `page.tsx`: route content.
- `template.tsx`: like layout but remounts on navigation.
- `loading.tsx`: route segment Suspense fallback.
- `error.tsx`: route segment error boundary, must be client component.
- `not-found.tsx`: 404 UI.

---

## 3. Server Components

Server Components render on the server and do not ship their component JavaScript to the browser.

Good for:
- data fetching
- reading server-only secrets
- rendering static/non-interactive UI
- reducing client bundle
- direct database/internal API access through server code

```tsx
export default async function ProductsPage() {
  const products = await getProducts();

  return (
    <main>
      <h1>Products</h1>
      <ProductGrid products={products} />
    </main>
  );
}
```

Server Components cannot use:
- `useState`
- `useEffect`
- event handlers
- browser APIs

---

## 4. Client Components

Client Components run in the browser after hydration.

Use when:
- state
- effects
- event handlers
- browser APIs
- interactive widgets
- client-side stores

```tsx
'use client';

import {useState} from 'react';

export function AddToCartButton({productId}: {productId: string}) {
  const [pending, setPending] = useState(false);

  async function add() {
    setPending(true);
    await addToCart(productId);
    setPending(false);
  }

  return <button onClick={add} disabled={pending}>Add to cart</button>;
}
```

Important:
`'use client'` marks a boundary. Child components imported by that file are part of the client bundle unless carefully split.

---

## 5. `use client` And `use server`

`use client`:
- file-level directive
- defines client component entry point
- needed for hooks, browser APIs, event handlers

`use server`:
- marks server functions/actions
- ensures code runs on server

Example Server Action:

```tsx
'use server';

export async function createPost(formData: FormData) {
  const title = String(formData.get('title'));
  await db.post.create({data: {title}});
}
```

---

## 6. Layouts vs Templates

Layout:
- persists between child navigations
- preserves state
- good for nav/sidebar/shell

Template:
- creates new instance per navigation
- useful when you need remount behavior

Use layout by default. Use template when reset/remount is intentional.

---

## 7. Passing Data Across Boundaries

Server to client props must be serializable.

Good:

```tsx
<ProductCard product={{id: product.id, name: product.name}} />
```

Avoid:
- functions as props from server to client
- class instances
- database connections
- non-serializable values

---

## 8. Real-World Use Cases

- Product page: Server Component fetches product, Client Component handles add-to-cart.
- Dashboard: server layout checks session, client chart handles interaction.
- Blog: server renders article, client comment widget hydrates.
- Admin: server fetches initial data, client table controls filters if highly interactive.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Marking whole app `use client` | Ships too much JS | Push client boundary down |
| Using browser API in Server Component | Server has no `window` | Move to Client Component/effect |
| Passing non-serializable props | Boundary failure | Pass plain data |
| Fetching everything in Client Components | Loses server benefits | Fetch on server where possible |
| Using template accidentally | State resets unexpectedly | Use layout by default |

---

## 10. Strong Interview Answer

Question:
Explain Server Components vs Client Components in Next.js.

Strong answer:

```text
In the App Router, components are Server Components by default. Server Components
render on the server, can fetch data and use server-only resources, and do not
ship their component JS to the browser. Client Components are marked with
`use client` and are needed for state, effects, event handlers, browser APIs, and
interactive widgets. A senior design pushes client boundaries as low as possible
to reduce bundle size while keeping interactive islands where users need them.
```

---

## 11. Revision Notes

- One-line summary: App Router is a route tree with server-first rendering and explicit client islands.
- Three keywords: Server Component, Client Component, layout.
- One interview trap: `use client` does not mean client-only rendering for all ancestors; it marks a client boundary.
- One memory trick: Fetch and render static UI on server; interact on client.

---

## 12. Parallel Routes — Multiple Pages in One Layout

Parallel routes let you render multiple page components simultaneously in a single layout using **named slots** (`@slotName` folders).

```
app/
  layout.tsx          ← receives @team and @analytics as props
  page.tsx
  @team/
    page.tsx
    default.tsx       ← required fallback
  @analytics/
    page.tsx
    default.tsx
```

```tsx
// app/layout.tsx
export default function DashboardLayout({
  children,
  team,
  analytics,
}: {
  children: React.ReactNode;
  team: React.ReactNode;
  analytics: React.ReactNode;
}) {
  return (
    <div className="dashboard">
      <main>{children}</main>
      <aside>{team}</aside>
      <section>{analytics}</section>
    </div>
  );
}
```

**Important:** each slot needs a `default.tsx` for when the slot has no active route (e.g., on initial navigation).

---

## 13. Intercepting Routes

Intercepting routes show different UI for the same URL depending on how you arrived:
- Click a photo in a feed → modal (intercepted)
- Navigate directly to `/photos/123` → full photo page (not intercepted)

```
app/
  feed/
    page.tsx
    (.)photos/         ← intercepts /photos at same level
      [id]/
        page.tsx       ← shows modal
  photos/
    [id]/
      page.tsx         ← shows full page when navigated directly
```

**Convention:**
- `(.)path` — same level
- `(..)path` — one level up
- `(...)path` — root level

---

## 14. Next.js 15 — Async Params and SearchParams

**Breaking change in Next.js 15:** `params` and `searchParams` are now Promises and must be awaited.

```tsx
// Next.js 14 (synchronous)
export default function Page({ params }: { params: { id: string } }) {
  const { id } = params;
}

// Next.js 15 (asynchronous — must await)
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
}

// searchParams also async in Next.js 15
export default async function SearchPage({
  searchParams,
}: {
  searchParams: Promise<{ q?: string }>;
}) {
  const { q } = await searchParams;
}
```

---

## 15. template.tsx vs layout.tsx

| `layout.tsx` | `template.tsx` |
|---|---|
| Persists across navigations | Re-mounts on every navigation |
| Shared state preserved | Fresh instance every navigation |
| Good for: sidebars, persistent nav, shared state | Good for: enter/exit animations, resetting forms, `useEffect` on route change |

```tsx
// template.tsx re-mounts — runs enter animation every navigation
'use client';
export default function Template({ children }: { children: React.ReactNode }) {
  return <div className="animate-fade-in">{children}</div>;
}
```

---

## 16. Route Handlers

```tsx
// app/api/products/route.ts
import { NextRequest, NextResponse } from 'next/server';

// GET /api/products
export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const q = searchParams.get('q') ?? '';
  const products = await db.product.findMany({ where: { name: { contains: q } } });
  return NextResponse.json(products);
}

// POST /api/products
export async function POST(request: NextRequest) {
  const body = await request.json();
  const product = await db.product.create({ data: body });
  return NextResponse.json(product, { status: 201 });
}

// Dynamic route: app/api/products/[id]/route.ts
export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ id: string }> }  // async in Next.js 15
) {
  const { id } = await params;
  const product = await db.product.findUniqueOrThrow({ where: { id } });
  return NextResponse.json(product);
}
```

---

## 17. Streaming with Suspense

```tsx
// app/dashboard/page.tsx
import { Suspense } from 'react';

export default function Dashboard() {
  return (
    <div>
      {/* Rendered immediately with static HTML */}
      <h1>Dashboard</h1>
      
      {/* Streamed in as data resolves */}
      <Suspense fallback={<MetricsSkeleton />}>
        <SlowMetrics />  {/* fetches inside, suspends */}
      </Suspense>
      
      <Suspense fallback={<ChartSkeleton />}>
        <RevenueChart />
      </Suspense>
    </div>
  );
}

// SlowMetrics.tsx — Server Component that fetches
async function SlowMetrics() {
  const metrics = await fetch('https://slow-api.example.com/metrics', { cache: 'no-store' });
  const data = await metrics.json();
  return <MetricsGrid data={data} />;
}
```

**Streaming advantage:** Browser receives and renders the `<h1>` immediately. `SlowMetrics` and `RevenueChart` render independently as their data arrives. Users see content progressively — not all-or-nothing.

