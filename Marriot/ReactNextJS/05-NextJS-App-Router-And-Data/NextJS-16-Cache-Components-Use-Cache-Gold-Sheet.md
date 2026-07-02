# Next.js 16 Cache Components And `use cache` - Gold Sheet

> Track File #23 - Group 5: Next.js App Router And Data
> Level: intermediate -> architect | Cache Components, `use cache`, cache profiles, tags, remote cache, and migration from the previous model

---

## 1. Intuition

Modern Next.js caching is moving from "the framework guesses what can be cached" toward "the developer marks reusable work explicitly."

Old mental model:

```text
fetch cache + route cache + router cache + revalidate flags
```

New Cache Components mental model:

```text
Enable cacheComponents -> mark reusable async work with "use cache"
-> define lifetime with cacheLife -> invalidate with tags/paths
```

The question is no longer only "is this route static or dynamic?" It is:

```text
Which data or UI fragments are deterministic enough to reuse?
For how long?
Who can see them?
How are they invalidated?
```

---

## 2. Definition

- Definition: Cache Components is the modern Next.js caching model where async functions, components, pages, or layouts can cache their returned output using the `use cache` directive.
- Category: App Router caching / rendering architecture.
- Core idea: Cache reusable server work intentionally at data level or UI level.

---

## 3. Enable Cache Components

```ts
// next.config.ts
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  cacheComponents: true,
};

export default nextConfig;
```

Once enabled:
- routes can contain cached and uncached parts;
- Suspense boundaries become important for streaming uncached work;
- `GET` Route Handlers follow the Cache Components prerendering model;
- you should be explicit about dynamic request APIs such as `cookies()` and `headers()`.

---

## 4. Data-Level Caching

Use when multiple components need the same data or computation.

```ts
import { cacheLife, cacheTag } from 'next/cache';

export async function getProducts() {
  'use cache';

  cacheLife('minutes');
  cacheTag('products');

  const response = await fetch('https://api.example.com/products');

  if (!response.ok) {
    throw new Error('Failed to load products');
  }

  return response.json() as Promise<Product[]>;
}
```

Properties:
- caches the function return value;
- works best for deterministic inputs;
- should not read per-user secrets unless using a private/user-aware strategy;
- should own its cache tags near the data source.

---

## 5. UI-Level Caching

Use when a component or route fragment is expensive but reusable.

```tsx
import { cacheLife, cacheTag } from 'next/cache';
import { getProducts } from '@/data/products';

export async function ProductRail() {
  'use cache';

  cacheLife('hours');
  cacheTag('products');

  const products = await getProducts();

  return (
    <section>
      {products.map(product => (
        <article key={product.id}>{product.name}</article>
      ))}
    </section>
  );
}
```

Good for:
- public product rails;
- CMS sections;
- navigation menus;
- expensive markdown rendering;
- mostly static dashboards by tenant if cache key includes tenant.

Bad for:
- user-specific account data;
- cart contents;
- live payment status;
- content that depends on request cookies but has no private cache boundary.

---

## 6. Cache Lifetimes

Use `cacheLife` to name how long cached work can be reused.

```ts
import { cacheLife } from 'next/cache';

export async function getCatalog() {
  'use cache';
  cacheLife('hours');

  return fetchCatalog();
}
```

Custom cache profiles:

```ts
// next.config.ts
import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  cacheComponents: true,
  cacheLife: {
    product: {
      stale: 300,
      revalidate: 900,
      expire: 3600,
    },
  },
};

export default nextConfig;
```

Then:

```ts
cacheLife('product');
```

Architecture rule:
Name profiles after business freshness, not implementation details.

Good:
- `product`
- `cms`
- `pricingPreview`
- `tenantConfig`

Weak:
- `short`
- `medium`
- `cache1`

---

## 7. Tags And Invalidation

Assign tags near the cached data:

```ts
import { cacheTag } from 'next/cache';

export async function getProduct(productId: string) {
  'use cache';

  cacheTag('products');
  cacheTag(`product:${productId}`);

  return fetchProduct(productId);
}
```

After mutation:

```ts
'use server';

import { updateTag, revalidatePath } from 'next/cache';

export async function updateProduct(input: UpdateProductInput) {
  await saveProduct(input);

  updateTag(`product:${input.id}`);
  updateTag('products');
  revalidatePath(`/products/${input.id}`);
}
```

Tag strategy:

| Tag | Use |
|---|---|
| `products` | Product list pages |
| `product:123` | Product detail |
| `tenant:acme` | Tenant configuration |
| `cms:home` | Home page CMS block |
| `user:123` | Only if private/user-aware caching is correct |

---

## 8. Remote Cache

Default `use cache` may use memory depending on runtime and deployment behavior. For durable shared caching across instances, use a remote cache strategy when appropriate.

Use remote cache when:
- the app runs on many server instances;
- recomputation is expensive;
- the cache must survive cold starts;
- a self-hosted environment needs shared cache behavior.

Avoid remote cache when:
- data is cheap to recompute;
- data is highly personalized;
- invalidation is not understood;
- cache keys could leak tenant/user data.

---

## 9. Dynamic Runtime APIs

Request-specific APIs affect cacheability:
- `cookies()`
- `headers()`
- `searchParams`
- auth/session helpers
- geolocation or user-agent decisions

Principle:
Do not accidentally cache request-specific output as public output.

Safer pattern:

```tsx
import { Suspense } from 'react';
import { PublicCatalog } from './public-catalog';
import { UserGreeting } from './user-greeting';

export default function Page() {
  return (
    <>
      <PublicCatalog />
      <Suspense fallback={null}>
        <UserGreeting />
      </Suspense>
    </>
  );
}
```

Separate public reusable fragments from request-specific fragments.

---

## 10. Previous Model Compatibility

You will still see:

```ts
fetch(url, { next: { revalidate: 300, tags: ['products'] } });
revalidateTag('products');
revalidatePath('/products');
```

These matter in existing codebases and interviews. But for new Next.js 16+ material, explain Cache Components first, then mention the previous model.

Interview framing:

```text
In older App Router projects I reason about Router Cache, Full Route Cache,
Data Cache, and request memoization. In newer Cache Components projects I start
with explicit "use cache" boundaries, cacheLife, cacheTag, updateTag, and
Suspense around uncached dynamic work.
```

---

## 11. Decision Matrix

| Data/UI | Cache Boundary | Lifetime | Invalidation |
|---|---|---|---|
| Marketing homepage CMS | UI-level `use cache` | Hours | CMS webhook tag |
| Product catalog | Data-level `use cache` | Minutes | `products`, `product:id` |
| User dashboard | Usually uncached/private | Request/session | Mutation refresh |
| Tenant config | Data-level `use cache` | Minutes/hours | `tenant:id` |
| Cart | Do not public-cache | Request/session | Server mutation |
| Search results | Short cache or client cache | Seconds/minutes | Query key |
| Pricing | Depends on business rules | Very short or none | Price update event |

---

## 12. Failure Modes

| Failure | User Symptom | Cause | Fix |
|---|---|---|---|
| Stale catalog | Old product remains visible | Missing tag invalidation | Add `cacheTag` and update after mutation |
| Privacy leak | User sees another user's data | Public cache around personalized content | Split private data out |
| Stampede | Many requests recompute at once | Expired hot cache | Longer stale window, remote cache, queue |
| Dynamic API error | Build/runtime warning | Cached scope reads request data incorrectly | Move request data outside cached scope |
| Over-invalidation | Site slows after every mutation | Revalidating broad paths | Use targeted tags |

---

## 13. Code Pattern: Cached DAL

```ts
// data/products.ts
import 'server-only';
import { cacheLife, cacheTag } from 'next/cache';
import { db } from '@/lib/db';

export async function getProductDTO(productId: string) {
  'use cache';

  cacheLife('product');
  cacheTag(`product:${productId}`);

  const product = await db.product.findUniqueOrThrow({
    where: { id: productId },
    select: {
      id: true,
      name: true,
      price: true,
      imageUrl: true,
    },
  });

  return product;
}
```

```ts
// app/admin/products/actions.ts
'use server';

import { updateTag } from 'next/cache';
import { z } from 'zod';
import { requireAdmin } from '@/lib/auth';
import { db } from '@/lib/db';

const schema = z.object({
  id: z.string().uuid(),
  price: z.number().positive(),
});

export async function updatePrice(rawInput: unknown) {
  const user = await requireAdmin();
  const input = schema.parse(rawInput);

  await db.audit.create({
    data: { actorId: user.id, action: 'PRODUCT_PRICE_UPDATE' },
  });

  await db.product.update({
    where: { id: input.id },
    data: { price: input.price },
  });

  updateTag(`product:${input.id}`);
  updateTag('products');
}
```

---

## 14. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Caching entire user pages | Privacy risk | Cache public fragments only |
| No tag ownership | Invalidation becomes random | Define tags in DAL |
| Revalidating broad paths | Expensive and imprecise | Use specific tags |
| Mixing old/new mental models | Confusing interviews and bugs | State which model the app uses |
| Ignoring Suspense | Uncached work blocks cached shell | Stream dynamic islands |

---

## 15. Practical Question

> You are building an ecommerce product listing in Next.js 16. How would you cache it and keep prices fresh?

---

## 16. Strong Answer

```text
I would enable Cache Components and put catalog reads behind server-only data
functions marked with "use cache". Product list and product detail functions
would use cacheLife profiles and tags like "products" and "product:id". After an
admin price update or inventory event, the mutation path would update the
specific product tag and any list tag. I would not cache cart, checkout, or
personalized recommendations as public UI. For dynamic user fragments I would
split them behind Suspense so the public catalog can stream quickly while
request-specific data remains isolated.
```

---

## 17. Revision Notes

- One-line summary: Next.js 16 Cache Components make cache boundaries explicit with `use cache`, `cacheLife`, and tags.
- Three keywords: `use cache`, `cacheLife`, `cacheTag`.
- One interview trap: Do not explain only the older four-cache-layer model for a modern Next.js 16 app.
- One memory trick: Cache answer = scope, lifetime, owner, invalidation, privacy.

