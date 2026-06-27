# Next.js Data Fetching Architecture - Gold Sheet

> Track File #11 of 24 - Group 5: Next.js App Router And Data
> Covers: `getStaticProps`, `getServerSideProps`, App Router async components, Server Actions, API routes, BFF pattern

---

## 1. Intuition

Data fetching architecture decides where data is loaded and who is allowed to know secrets.

```text
browser fetch
server component fetch
server action mutation
route handler API
BFF layer
backend service
```

Modern Next.js encourages server-side data fetching by default for route UI, with client fetching for interactive or frequently changing client state.

---

## 2. Legacy Pages Router APIs

Know these for interviews and existing codebases.

`getStaticProps`:
- build-time static props
- can be paired with ISR

```tsx
export async function getStaticProps() {
  return {
    props: {posts: await getPosts()},
    revalidate: 60,
  };
}
```

`getServerSideProps`:
- request-time data
- good for personalized/fresh pages

```tsx
export async function getServerSideProps(context) {
  const user = await getUserFromCookie(context.req);
  return {props: {user}};
}
```

App Router uses different patterns, but legacy knowledge still matters.

---

## 3. App Router Async Components

Server Components can be async.

```tsx
export default async function OrdersPage() {
  const orders = await getOrders();
  return <OrdersTable orders={orders} />;
}
```

Benefits:
- data stays server-side
- no client loading waterfall for initial HTML
- server-only secrets remain private
- less client JS

---

## 4. Server Actions

Server Actions run on the server and are useful for mutations.

```tsx
// app/actions.ts
'use server';

import {revalidatePath} from 'next/cache';

export async function createProduct(formData: FormData) {
  const name = String(formData.get('name'));
  await db.product.create({data: {name}});
  revalidatePath('/products');
}
```

Form usage:

```tsx
<form action={createProduct}>
  <input name="name" />
  <button type="submit">Create</button>
</form>
```

Production concerns:
- validate all input server-side
- check authorization
- handle errors
- revalidate affected cache
- avoid exposing secrets in client code

---

## 5. API Routes And Route Handlers

Route handler:

```ts
// app/api/health/route.ts
export async function GET() {
  return Response.json({ok: true});
}
```

Use route handlers for:
- webhooks
- third-party callbacks
- browser API endpoints
- BFF endpoints
- file upload signatures

Do not create route handlers for every server component fetch if direct server-side function calls are cleaner.

---

## 6. Backend For Frontend Pattern

BFF means the frontend has a server-side layer tailored to UI needs.

```text
Client UI -> Next.js BFF -> backend services
```

Benefits:
- hides backend complexity
- protects secrets
- aggregates multiple services
- shapes DTOs for UI
- enforces auth/session

Trade-offs:
- another layer to operate
- can become a dumping ground
- must avoid duplicating core business logic

---

## 7. Client Fetching Still Matters

Use client fetching for:
- highly interactive widgets
- realtime updates
- background refresh
- user-triggered filters
- client-only state

Example:

```tsx
'use client';

function NotificationsBell() {
  const {data} = useQuery({
    queryKey: ['notifications'],
    queryFn: fetchNotifications,
    refetchInterval: 30_000,
  });

  return <Bell count={data?.unreadCount ?? 0} />;
}
```

---

## 8. Real-World Use Cases

- Product page: server fetch product and recommendations.
- Checkout mutation: Server Action with auth and idempotency.
- Webhook: route handler.
- Dashboard widget: client query with refetch.
- Multi-service aggregation: BFF endpoint.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Fetching secrets from client | Exposes credentials | Fetch on server |
| Route handler for every internal call | Extra HTTP overhead | Call server function directly |
| No server validation in Server Action | Client can be bypassed | Validate and authorize |
| Forgetting revalidation | UI stays stale | Revalidate path/tag |
| Moving all logic to BFF | Backend duplication | Keep business truth in services |

---

## 10. Strong Interview Answer

Question:
How do you fetch and mutate data in modern Next.js?

Strong answer:

```text
In App Router, I fetch initial route data in async Server Components whenever
possible so secrets stay server-side and the page avoids client waterfalls. For
mutations, I use Server Actions or route handlers depending on whether the action
is form/UI-owned or an API/webhook-style endpoint. I still use client fetching
for interactive widgets and background refresh. The BFF pattern is useful when
the UI needs aggregated, shaped data across services, but it must not become a
duplicate business-logic layer.
```

---

## 11. Revision Notes

- One-line summary: Fetch on server by default, mutate with Server Actions or route handlers, use client fetching for interactivity.
- Three keywords: async component, Server Action, BFF.
- One interview trap: Server Actions still require validation and authorization.
- One memory trick: Server fetch protects secrets; client fetch powers interaction.

---

## 12. React.cache() — Deduplication for Non-Fetch Functions

`fetch()` is automatically deduplicated within a render pass (Request Memoization). For DB calls or any other async function, use `React.cache()` to get the same behavior.

```tsx
// lib/data.ts
import { cache } from 'react';
import { db } from './db';

// Calling getUser('123') multiple times in the same render returns the same promise
export const getUser = cache(async (id: string) => {
  console.log('DB query for user', id);  // only logs ONCE even if called 3 times
  return db.user.findUnique({ where: { id } });
});

// layout.tsx calls getUser('123')
// page.tsx also calls getUser('123')
// → Only ONE DB query executes
```

**When to use:** Any expensive server-side function (DB queries, 3rd party API calls) that might be called from multiple Server Components in the same render tree.

---

## 13. Preventing Waterfall Data Fetching

**Problem:** Sequential awaits create a waterfall — each fetch waits for the previous.

```tsx
// BAD: Sequential — total time = t1 + t2 + t3
export default async function Dashboard() {
  const user = await getUser();          // 100ms
  const projects = await getProjects();  // 150ms
  const metrics = await getMetrics();    // 200ms
  // Total: 450ms
}
```

**Fix 1: Promise.all for independent data**

```tsx
// GOOD: Parallel — total time = max(t1, t2, t3)
export default async function Dashboard() {
  const [user, projects, metrics] = await Promise.all([
    getUser(),
    getProjects(),
    getMetrics(),
  ]);
  // Total: ~200ms
}
```

**Fix 2: Start promises early, await later**

```tsx
export default async function Dashboard() {
  // Start all fetches immediately
  const userPromise = getUser();
  const projectsPromise = getProjects();
  const metricsPromise = getMetrics();
  
  // Await only when you need the data
  const user = await userPromise;
  // projects and metrics are still resolving in background
  
  return <DashboardLayout user={user} projectsPromise={projectsPromise} />;
}
```

**Fix 3: Suspense boundaries for independent parallel rendering**

```tsx
// Each Suspense boundary renders independently — no waiting between them
export default function Dashboard() {
  return (
    <>
      <Suspense fallback={<UserSkeleton />}>
        <UserSection />       {/* fetches independently */}
      </Suspense>
      <Suspense fallback={<ProjectsSkeleton />}>
        <ProjectsSection />   {/* fetches independently */}
      </Suspense>
    </>
  );
}
```

---

## 14. BFF Pattern — Backend for Frontend

The BFF is a server-side aggregation layer that shapes data specifically for the UI.

```
Client → Next.js Server (BFF) → Microservice A
                               → Microservice B  
                               → Microservice C
```

```tsx
// app/api/dashboard/route.ts — BFF aggregates 3 services
export async function GET(req: NextRequest) {
  const session = await getSession(req);
  
  const [userData, ordersData, recommendationsData] = await Promise.all([
    fetch(`https://user-service/users/${session.userId}`),
    fetch(`https://orders-service/users/${session.userId}/orders?limit=5`),
    fetch(`https://ml-service/recommendations/${session.userId}`),
  ]);

  const [user, orders, recommendations] = await Promise.all([
    userData.json(),
    ordersData.json(),
    recommendationsData.json(),
  ]);

  // Shape the response for exactly what the UI needs
  return NextResponse.json({
    name: user.name,
    recentOrders: orders.slice(0, 3),
    topRecommendations: recommendations.slice(0, 5),
  });
}
```

**BFF rules:**
- Shape data — don't duplicate business logic
- Handle auth at the BFF layer
- Use Request Memoization and caching aggressively
- Keep it thin — aggregation only, not business rules

---

## 15. Sequential vs Parallel Fetch Decision Tree

```
Are the fetches independent of each other?
  YES → Promise.all or Suspense boundaries (parallel)
  NO  → await sequentially (order matters)
         Example: fetchUser() then fetchPostsForUser(user.id)

Does the data change frequently?
  YES → cache: 'no-store' or short revalidate
  NO  → static or long-TTL ISR

Is the data user-specific?
  YES → SSR (no-store, per-request)
  NO  → SSG or ISR (cache-friendly)
```

