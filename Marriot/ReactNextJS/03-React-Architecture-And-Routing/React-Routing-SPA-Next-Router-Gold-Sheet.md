# React Routing: SPA And Next.js Router - Gold Sheet

> Track File #5 of 24 - Group 3: React Architecture And Routing
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

---

## 8. Real-World Use Cases

- SaaS app: `/dashboard`, `/settings`, nested account routes.
- Ecommerce: `/products/[id]`, `/cart`, `/checkout`.
- Marketing site: route groups for public pages.
- Admin tool: lazy-loaded admin routes.
- Search page: URL search params for shareable filter state.

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

