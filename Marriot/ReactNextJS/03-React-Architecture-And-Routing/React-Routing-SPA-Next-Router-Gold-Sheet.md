# React Routing: SPA And Next.js Router - Gold Sheet

> Track Module - Group 3: React Architecture And Routing
> Covers: React Router data APIs, nested routing, lazy route loading, Next.js file-based routing, dynamic routes, route groups, navigation strategies

---

## 1. Intuition

Routing maps URLs to UI and data needs.

```text
URL -> route match -> layout -> data -> screen -> navigation state
```

In React SPAs, routing happens mostly in the browser. In Next.js, routing is tied to the filesystem, server rendering, layouts, and caching.

---

## 2. React Router Data APIs

Modern React Router supports data-aware routes.

```tsx
import {createBrowserRouter, useLoaderData} from 'react-router';

const router = createBrowserRouter([
  {
    path: '/',
    Component: RootLayout,
    children: [
      {
        path: 'teams/:teamId',
        loader: async ({params}) => {
          return fetchTeam(params.teamId!);
        },
        Component: TeamPage,
      },
    ],
  },
]);

function TeamPage() {
  const team = useLoaderData() as Team;
  return <h1>{team.name}</h1>;
}
```

Benefits:
- route-level data loading
- pending UI
- actions/forms
- error boundaries per route
- nested layout data

---

## 3. Nested Routing

Nested routes model nested UI.

```tsx
{
  path: '/dashboard',
  Component: DashboardLayout,
  children: [
    {index: true, Component: DashboardHome},
    {path: 'settings', Component: SettingsPage},
  ],
}
```

Layout component:

```tsx
import {Outlet} from 'react-router';

export function DashboardLayout() {
  return (
    <section>
      <Sidebar />
      <main>
        <Outlet />
      </main>
    </section>
  );
}
```

---

## 4. Lazy Route Loading

Lazy loading avoids shipping every route's code upfront.

```tsx
const AdminPage = lazy(() => import('./AdminPage'));

<Suspense fallback={<PageSpinner />}>
  <AdminPage />
</Suspense>
```

Use for:
- admin pages
- rarely visited settings
- heavy charts
- editor experiences

Avoid:
- splitting every tiny component into its own chunk
- creating route waterfalls with too many nested lazy imports

---

## 5. Next.js File-Based Routing

App Router conventions:

```text
app/
  layout.tsx
  page.tsx
  products/
    page.tsx
    [productId]/
      page.tsx
  (marketing)/
    pricing/
      page.tsx
  api/
    health/
      route.ts
```

Key files:
- `page.tsx`: route UI.
- `layout.tsx`: shared persistent layout.
- `template.tsx`: new instance on navigation.
- `loading.tsx`: Suspense fallback for segment.
- `error.tsx`: segment error boundary.
- `not-found.tsx`: 404 fallback.
- `route.ts`: route handler/API endpoint.

---

## 6. Dynamic Routes And Route Groups

Dynamic segment:

```text
app/products/[productId]/page.tsx
```

```tsx
export default async function ProductPage({
  params,
}: {
  params: Promise<{productId: string}>;
}) {
  const {productId} = await params;
  const product = await getProduct(productId);
  return <ProductDetails product={product} />;
}
```

Route group:

```text
app/(dashboard)/settings/page.tsx
```

Route groups organize code without adding URL segments.

---

## 7. Navigation Strategies

React Router:
- `Link` for declarative navigation.
- `useNavigate` for imperative navigation.
- loaders/actions for data mutation/navigation.

Next.js:
- `next/link` for normal navigation.
- `redirect()` on server.
- `useRouter()` in client components for imperative navigation.

Production rule:
Prefer link-based navigation when possible. Use imperative navigation for event-driven flows such as post-submit redirects.

### Next.js Client Navigation Hooks

```tsx
'use client';
import {useRouter, usePathname, useSearchParams} from 'next/navigation';

function SearchBar() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  function handleSearch(term: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (term) {
      params.set('q', term);
    } else {
      params.delete('q');
    }
    // Updates URL without full reload — shareable filter state
    router.replace(`${pathname}?${params.toString()}`);
  }

  return <input onChange={e => handleSearch(e.target.value)} />;
}
```

`router.push()` adds to browser history. `router.replace()` does not.

### Parallel Routes and Intercepting Routes

**Parallel routes** render multiple pages in the same layout simultaneously:

```text
app/
  layout.tsx       # renders both @feed and @modal slots
  @feed/
    page.tsx       # main feed
  @modal/
    (...)booking/
      [id]/
        page.tsx   # intercepted route shown as modal
  booking/
    [id]/
      page.tsx     # full page when navigated directly
```

Useful for:
- Modals that update the URL (shareable/bookmarkable)
- Side-by-side dashboards
- Split-view layouts

**Intercepting routes** catch a route in a parent layout and render it differently (e.g. as a modal), but navigating directly to the URL shows the full page:

```text
(.)booking/[id]   — intercept same-segment /booking/[id]
(..)item/[id]     — intercept one segment up
(...)item/[id]    — intercept from app root
```

---

## 8. Route Protection in Middleware

```ts
// middleware.ts
import {NextRequest, NextResponse} from 'next/server';

export function middleware(request: NextRequest) {
  const {pathname} = request.nextUrl;
  const session = request.cookies.get('session')?.value;

  const isProtected =
    pathname.startsWith('/dashboard') || pathname.startsWith('/api/bookings');

  if (isProtected && !session) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(loginUrl);
  }
}

export const config = {
  matcher: ['/dashboard/:path*', '/api/bookings/:path*'],
};
```

Trap: middleware only checks whether a cookie exists — validate the JWT in server components/actions for actual security.

---

## 9. Real-World Use Cases

- SaaS app: `/dashboard`, `/settings`, nested account routes.
- Ecommerce: `/products/[id]`, `/cart`, `/checkout`.
- Marketing site: route groups for public pages.
- Admin tool: lazy-loaded admin routes.
- Search page: `useSearchParams` + `router.replace()` for shareable filter state.
- Hotel gallery: intercepting route shows photo as modal, direct URL shows full page.

---

## 10. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| `router.push()` for back navigation | Wrong history entry | Use `router.back()` or Link |
| Storing filters in component state | Not shareable | Use URL search params |
| `useRouter` for simple links | Less accessible | Use `<Link>` |
| Route protection in UI only | Not security | Protect in middleware + server |
| Importing `useSearchParams` outside Suspense | Opt-out of static render | Wrap in Suspense |

---

## 11. Strong Interview Answer

Question:
How do you handle routing and navigation in Next.js?

Strong answer:

```text
In Next.js App Router, routing is file-system based. I use `layout.tsx` for
persistent shared UI, `loading.tsx` for Suspense fallbacks, `error.tsx` for error
boundaries, and `not-found.tsx` for 404s. For client navigation I use `next/link`
and `useRouter` for imperative flows like post-submit redirects. Filter state goes
into URL search params via `useSearchParams` + `router.replace()` so results are
shareable. I protect routes in middleware for fast redirects, then validate the
session properly in server components. For modal patterns I use parallel +
intercepting routes so gallery items have shareable URLs but open as modals within
the layout.
```

---

## 12. Revision Notes

- One-line summary: Next.js routing is file-system, data-aware, and composable with layouts and parallel routes.
- Three keywords: layouts, searchParams, intercepting.
- One interview trap: `useSearchParams` must be inside a Suspense boundary to avoid opting out of static rendering.
- One memory trick: Link for navigation, searchParams for state, middleware for broad protection, server components for actual authorization.


---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| Storing route state only in React state | Not shareable/bookmarkable | Use URL params/search params |
| One giant route tree without layouts | Duplicated shell UI | Use nested layouts |
| Passing full objects through navigation | Stale and heavy | Pass IDs or URL params |
| Using client router where server redirect fits | More JS and complexity | Redirect on server when possible |
| Ignoring 404/error/loading files in Next | Bad UX | Add route segment states |

---

## 10. Strong Interview Answer

Question:
How do React Router and Next.js routing differ?

Strong answer:

```text
React Router is a client-side routing library for React apps, and its data APIs
let routes define loaders, actions, pending UI, and error boundaries. Next.js App
Router is filesystem-based and deeply integrated with server rendering, layouts,
Server Components, caching, and route handlers. In a pure SPA, I think route match
to component and data loader. In Next.js, I also think about server/client
boundaries, route segment loading, caching, and rendering strategy per route.
```

---

## 11. Revision Notes

- One-line summary: Routing maps URL state to UI, data, layouts, and loading/error boundaries.
- Three keywords: nested routes, dynamic segments, layouts.
- One interview trap: Route groups do not add URL segments.
- One memory trick: URL state should survive refresh, share, and back button.

