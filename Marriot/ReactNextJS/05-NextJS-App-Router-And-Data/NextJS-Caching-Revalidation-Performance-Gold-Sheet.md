# Next.js Caching, Revalidation, And Performance - Gold Sheet

> Track File #12 of 24 - Group 5: Next.js App Router And Data
> Covers: HTTP cache, CDN cache, Next.js caching layers, revalidation, memoization, deduplication of requests

> Current Next.js 16 note: this sheet preserves the previous App Router caching model because it is still important for existing codebases and interviews. For the modern Cache Components model, read `NextJS-16-Cache-Components-Use-Cache-Gold-Sheet.md` after this file.

---

## 1. Intuition

Caching is controlled reuse.

```text
Can I reuse this response/render/data safely?
For whom?
For how long?
How do I invalidate it?
```

Caching improves latency and cost, but wrong caching creates stale data or privacy leaks.

---

## 2. HTTP Caching Basics

Important headers:
- `Cache-Control`
- `ETag`
- `Last-Modified`
- `Vary`
- `s-maxage`
- `stale-while-revalidate`

Example:

```http
Cache-Control: public, s-maxage=300, stale-while-revalidate=60
```

Meaning:
Shared cache can serve for 5 minutes and may serve stale briefly while revalidating.

---

## 3. CDN Caching

CDNs cache content near users.

Good for:
- static assets
- images
- public pages
- API responses safe for shared cache

Danger:
Never cache personalized data publicly. Use correct `private`, `no-store`, or user-specific cache keys.

---

## 4. Next.js Caching Layers

Think in layers:

```text
browser cache
CDN/edge cache
Next route/output cache
data cache
request memoization/deduplication during render
client server-state cache
```

Different layers solve different problems. Do not say "Next cache" as if it is one thing.

---

## 5. Revalidation Strategies

Time-based:

```ts
await fetch('https://api.example.com/products', {
  next: {revalidate: 300},
});
```

On-demand path:

```ts
import {revalidatePath} from 'next/cache';

revalidatePath('/products');
```

On-demand tag:

```ts
await fetch(url, {next: {tags: ['products']}});
revalidateTag('products');
```

Use tags when many routes depend on the same data.

---

## 6. Memoization And Deduplication

During server rendering, duplicate compatible requests can be deduped or memoized depending on framework behavior and cache mode.

Design principle:
Create shared data functions so the app has one policy for each data source.

```ts
export async function getProduct(id: string) {
  const response = await fetch(`${API_URL}/products/${id}`, {
    next: {tags: [`product:${id}`]},
  });

  if (!response.ok) {
    throw new Error('Product request failed');
  }

  return response.json() as Promise<Product>;
}
```

---

## 7. Cache Decision Matrix

| Data | Cache Policy |
|---|---|
| Marketing page | static/CDN |
| Product catalog | ISR/tag revalidation |
| User dashboard | private/no-store or per-user |
| Cart | private, often server/session backed |
| Feature flags | short TTL, user/tenant aware |
| Search results | short TTL or client cache by query |
| Payment status | no optimistic cache; verify server |

---

## 8. Real-World Use Cases

- CMS publishes article: call on-demand revalidation for article path/tag.
- Product price update: revalidate product and category tags.
- Personalized dashboard: avoid shared public cache.
- Search autocomplete: client cache plus debounce.
- Admin mutation: revalidate affected table query/path.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Publicly caching personalized page | Privacy leak | private/no-store/user-specific |
| Revalidating whole site | Expensive | path/tag targeted revalidation |
| No cache ownership | Random stale bugs | central data functions |
| Assuming one cache layer | Wrong debugging | identify layer |
| Ignoring `Vary`/cookies | wrong user content | vary or avoid shared cache |

---

## 10. Strong Interview Answer

Question:
How do you design caching in a Next.js app?

Strong answer:

```text
I separate browser, CDN, route/output, server data, request memoization, and
client query cache. Public static content can use CDN and ISR. User-specific data
must not be publicly cached. I define data access functions with explicit cache
and revalidation policy, use time-based revalidation for tolerable staleness, and
tag/path revalidation after mutations. The key trade-off is latency and cost
versus freshness and invalidation complexity.
```

---

## 11. Revision Notes

- One-line summary: Caching is safe reuse plus clear invalidation.
- Three keywords: CDN, tags, revalidation.
- One interview trap: Never public-cache user-specific content.
- One memory trick: Cache policy needs owner, scope, TTL, invalidation.

---

## 12. The 4-Layer Caching Architecture — Complete Reference

Next.js has four distinct caching layers. Understanding each is critical for senior interviews.

```
Incoming Request
       │
       ▼
[1] Router Cache           ← Client-side in-memory (per navigation, browser tab)
       │ miss
       ▼
[2] Full Route Cache       ← Server-side static HTML + RSC payload (build time)
       │ miss
       ▼
[3] Data Cache             ← Persistent fetch() cache (across requests, on server)
       │ miss
       ▼
[4] Request Memoization    ← In-memory dedup within ONE render pass (per request)
       │
       ▼
Origin (DB / API)
```

### Layer 1: Router Cache (Client)

- Lives in the browser (JavaScript memory), not on disk
- Caches the RSC payload of previously visited routes
- Duration: session-based, cleared on full page reload
- **Behavior change in Next.js 15:** Router Cache is now session-based by default (opt-in staleness time removed). Pages are fetched fresh on navigation by default.
- To opt in to staleness: `staleTimes` option in `next.config.ts`

```ts
// next.config.ts — re-enable staleness (Next.js 15)
const nextConfig = {
  experimental: {
    staleTimes: {
      dynamic: 30,   // dynamic routes cached 30 seconds
      static: 180,   // static routes cached 3 minutes
    },
  },
};
```

### Layer 2: Full Route Cache (Server)

- Stores rendered HTML + RSC payload for statically generated routes
- Set at build time (`next build`)
- Invalidated by: `revalidatePath`, `revalidateTag`, or time-based revalidation
- Only applies to routes that are **static** — routes that use cookies, headers, or `no-store` fetches are excluded

```tsx
// Force static even with dynamic segments
export const dynamic = 'force-static';
export const revalidate = 3600;  // regenerate every hour

// Force dynamic (never statically cache)
export const dynamic = 'force-dynamic';
```

### Layer 3: Data Cache

- Persistent server-side cache for `fetch()` calls
- Survives across multiple requests and deployments (on Vercel, stored in the data layer)
- Controlled by `fetch()` options and route segment config

```tsx
// Opt out entirely — always fetch fresh
fetch(url, { cache: 'no-store' });

// Cache indefinitely (until manual revalidation)
fetch(url, { cache: 'force-cache' });

// Cache with time-based expiry
fetch(url, { next: { revalidate: 3600 } });

// Cache with tag for on-demand invalidation
fetch(url, { next: { tags: ['products', `product-${id}`] } });
```

### Layer 4: Request Memoization

- In-memory deduplication within a single server render
- Automatic for identical `fetch(url)` calls (same URL + options)
- Only lasts for one request — not persistent
- Use `React.cache()` for the same behavior on non-fetch functions

```tsx
// Both components call getUser('123') — only ONE DB query executes
// app/layout.tsx
const user = await getUser(session.userId);

// app/dashboard/page.tsx  
const user = await getUser(session.userId);  // returns cached result from this render
```

---

## 13. Opt-Out Cheat Sheet

| Layer | How to opt out |
|---|---|
| Router Cache | Call `router.refresh()` or `revalidatePath()` from Server Action |
| Full Route Cache | `export const dynamic = 'force-dynamic'` or use `cookies()`/`headers()` |
| Data Cache | `fetch(url, { cache: 'no-store' })` |
| Request Memoization | Cannot opt out (per-request, always active) |

---

## 14. On-Demand Revalidation Patterns

### Pattern: Webhook from CMS

```tsx
// app/api/revalidate/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { revalidateTag } from 'next/cache';

export async function POST(req: NextRequest) {
  const { secret, tags } = await req.json();
  
  if (secret !== process.env.REVALIDATION_SECRET) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  for (const tag of tags) {
    revalidateTag(tag);
  }
  
  return NextResponse.json({ revalidated: true, tags });
}
```

### Pattern: Server Action After Mutation

```tsx
'use server';
import { revalidatePath, revalidateTag } from 'next/cache';
import { db } from '@/lib/db';

export async function updateProduct(id: string, data: ProductInput) {
  await db.product.update({ where: { id }, data });
  
  // Invalidate by tag — all pages that fetched this product's data
  revalidateTag(`product-${id}`);
  revalidateTag('products');  // also invalidate listing pages
  
  // OR invalidate by path
  revalidatePath(`/products/${id}`);
  revalidatePath('/products');
}
```

---

## 15. Caching Decision Flowchart

```
Is the response user-specific?
  YES → never public-cache. SSR with cache: 'no-store'. CDN: private.
  NO  →
    How often does data change?
      Never / rarely → SSG with force-cache. CDN: max-age=31536000.
      Occasionally (hours) → ISR with revalidate: 3600.
      Frequently (minutes) → ISR with revalidate: 60 OR on-demand revalidation.
      On every update → on-demand revalidateTag webhook from CMS/backend.
      Real-time → SSR + streaming, or client-side polling / WebSocket.
```
