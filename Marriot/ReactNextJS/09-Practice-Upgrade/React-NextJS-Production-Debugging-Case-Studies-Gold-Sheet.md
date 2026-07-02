# React + Next.js Production Debugging Case Studies — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: Incident → Investigation → Root Cause → Fix → Prevention → 60-second answer

---

## Case Study 1 — Hydration Mismatch Crashing the App

### Incident

Production Next.js e-commerce site. Home page shows a "cart" icon with item count from localStorage. After deployment, ~15% of users see a brief flash of wrong content and React DevTools shows hydration errors. Sentry reports hundreds of "Hydration failed because initial UI does not match" errors.

### Investigation

1. Reproduced in production build (not dev — hydration errors are sometimes suppressed in dev)
2. Checked the component rendering the cart badge:
   ```tsx
   // CartBadge.tsx (originally)
   function CartBadge() {
     const itemCount = JSON.parse(localStorage.getItem('cart') ?? '[]').length;
     return <span className="badge">{itemCount}</span>;
   }
   ```
3. Found that `localStorage` is a browser-only API — during Server-Side Rendering, `localStorage` does not exist. Next.js generates HTML with whatever the server produces (error/crash handled silently, renders 0), then client hydrates with the real localStorage value.
4. Server renders: `<span class="badge">0</span>`
5. Client first render: `<span class="badge">3</span>` (user had 3 items)
6. React detects mismatch → hydration error

### Root Cause

Reading browser-only APIs (`localStorage`, `window`, `document`) during the initial render of a component that is server-rendered. The server has no access to these APIs — it renders one thing, the client renders another.

### Fix

```tsx
// Fix 1: useEffect for browser-only data (recommended)
function CartBadge() {
  const [count, setCount] = useState(0);  // start with 0 on server

  useEffect(() => {
    // This only runs on client — after hydration — no mismatch
    const items = JSON.parse(localStorage.getItem('cart') ?? '[]');
    setCount(items.length);
  }, []);

  if (count === 0) return null;
  return <span className="badge">{count}</span>;
}

// Fix 2: Suppress with suppressHydrationWarning (for intentionally different server/client values)
// ONLY for cases where difference is acceptable (timestamps, random IDs)
<span suppressHydrationWarning>{Date.now()}</span>

// Fix 3: Zustand with SSR — use a flag to prevent SSR rendering
const useCartStore = create(persist(...));
const useIsHydrated = () => {
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => setHydrated(true), []);
  return hydrated;
};

function CartBadge() {
  const count = useCartStore(s => s.items.length);
  const isHydrated = useIsHydrated();
  if (!isHydrated) return null;
  return count > 0 ? <span className="badge">{count}</span> : null;
}
```

### Prevention

- Never read browser APIs (`localStorage`, `window`, `document`, `navigator`) at render time in components that are SSR'd
- Use `useEffect` for all browser-only state initialization
- Add a Playwright test that checks no hydration errors in console on page load
- Enable React's strict mode in development to surface these earlier

### 60-Second Interview Answer

```text
We had hydration errors because a Cart Badge component read localStorage during
initial render. The server has no localStorage — it rendered 0. The client had
3 items — it rendered 3. React detected the mismatch. The fix was to initialize
count at 0, then update it in a useEffect after hydration. The root principle is:
never read browser APIs during the render phase of server-rendered components.
```

---

## Case Study 2 — Memory Leak in Dashboard Causing 2GB RAM Usage

### Incident

Company internal dashboard built in React. After keeping the tab open for 2 hours, the browser tab uses 2GB of RAM and becomes unresponsive. Closing and reopening the tab resets it. Only happens on the "Live Metrics" page.

### Investigation

1. Opened Chrome DevTools → Memory → took heap snapshot with live metrics open for 5 min
2. Took another snapshot 30 min later
3. Compared snapshots: found thousands of `EventEmitter`, `WebSocket`, and closure objects accumulating
4. Traced to this component:
   ```tsx
   function LiveMetrics() {
     const [metrics, setMetrics] = useState<Metric[]>([]);

     useEffect(() => {
       const ws = new WebSocket('wss://metrics.internal/stream');
       ws.onmessage = (event) => {
         const metric = JSON.parse(event.data);
         setMetrics(prev => [...prev, metric]);  // accumulates forever!
       };
       // NO ws.close() — WebSocket never closed
     }, []);

     return <MetricsChart data={metrics} />;
   }
   ```
5. Two memory leaks found:
   - WebSocket never closed (accumulates in the browser's connection pool)
   - Metrics array grows without bound — every message appended forever

### Root Cause

1. Missing `ws.close()` in useEffect cleanup — WebSocket connection and its memory never released
2. Unbounded state growth — keeping all 2 hours of metric data in a single array

### Fix

```tsx
function LiveMetrics() {
  const [metrics, setMetrics] = useState<Metric[]>([]);

  useEffect(() => {
    const ws = new WebSocket('wss://metrics.internal/stream');
    
    ws.onmessage = (event) => {
      const metric = JSON.parse(event.data);
      // Keep only last 100 data points — sliding window
      setMetrics(prev => {
        const next = [...prev, metric];
        return next.length > 100 ? next.slice(-100) : next;
      });
    };

    ws.onerror = () => ws.close();

    // CLEANUP — close WebSocket when component unmounts
    return () => {
      ws.close();
    };
  }, []);

  return <MetricsChart data={metrics} />;
}
```

### Prevention

- Code review checklist: every `useEffect` with subscriptions/connections must have a cleanup return
- Automated: React Strict Mode double-invokes effects — a missing cleanup breaks the component in dev, surfacing the bug early
- Monitoring: track browser memory usage in production via `performance.memory` API and alert if tab exceeds 1GB
- Use `useRef` to hold mutable values (like the sliding window size) that should not trigger re-renders

### 60-Second Interview Answer

```text
Users' browser tabs grew to 2GB after 2 hours on a metrics dashboard. Heap
snapshots showed accumulating WebSocket and closure objects. The component had
two problems: the WebSocket was never closed in the useEffect cleanup, and the
metrics array grew without bound — one entry per message for 2 hours. The fix
was to close the WebSocket in the cleanup return and implement a sliding window
keeping only the last 100 data points. Prevention: React Strict Mode exposes
missing cleanups early, and code review checklists should require every
subscription to have explicit cleanup.
```

---

## Case Study 3 — ISR Cache Showing Stale Prices for 24 Hours

### Incident

Product catalog uses ISR with `revalidate: 86400` (1 day). Sales team ran a flash sale with 50% discounts. Customers complained prices shown were the original prices, not the discount prices. Revenue was lost for several hours before the issue was noticed.

### Investigation

1. ISR `revalidate: 86400` = cached pages regenerate every 24 hours
2. Flash sale prices updated in the database at 10 AM
3. Next.js continues serving stale HTML cached from the previous day
4. First user to hit the page after 24 hours triggers regeneration — everyone before that sees old prices
5. The price shown on the product page was generated at midnight — fully stale

### Root Cause

Using time-based ISR revalidation for data that can change urgently at any time. A 24-hour cache TTL is correct for product descriptions but wrong for prices, availability, and promotional content.

### Fix

```tsx
// Fix 1: On-demand revalidation — revalidate immediately when price changes

// app/api/products/[id]/page.tsx
export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const product = await fetch(`https://api.internal/products/${id}`, {
    next: { tags: [`product-${id}`, 'product-prices'] },  // cache tags
  }).then(r => r.json());

  return <ProductDetail product={product} />;
}

// When a price changes, call revalidation API
// app/api/revalidate/route.ts
import { revalidateTag } from 'next/cache';

export async function POST(req: Request) {
  const { secret, tag } = await req.json();
  
  // Validate secret to prevent unauthorized revalidation
  if (secret !== process.env.REVALIDATION_SECRET) {
    return Response.json({ error: 'Unauthorized' }, { status: 401 });
  }
  
  revalidateTag(tag);  // revalidates all pages fetching with this tag
  return Response.json({ revalidated: true });
}

// In your CMS webhook or price update service:
// POST /api/revalidate { secret: ..., tag: 'product-prices' }
// This immediately marks all product pages for regeneration

// Fix 2: Short TTL for volatile data
export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const product = await fetch(`https://api.internal/products/${id}`, {
    next: { revalidate: 60 },  // max 1 minute stale for prices
  }).then(r => r.json());
  // ...
}
```

### Prevention

- Never use a single `revalidate` value for the whole page — different data has different freshness requirements
- Separate volatile data (prices, availability) from stable data (descriptions, images) in data fetching
- Configure CMS/backend to call `revalidatePath` or `revalidateTag` webhooks on content publish
- Add monitoring: compare DB prices to live page prices on a schedule — alert when they diverge

### 60-Second Interview Answer

```text
Our product pages used ISR with 24-hour revalidation. When we ran a flash sale,
the updated prices in the database weren't reflected on the site for up to 24 hours.
The fix was to switch from time-based revalidation to tag-based on-demand revalidation.
Product pages are fetched with a cache tag like 'product-prices'. When prices update,
our backend hits a Next.js revalidation endpoint that calls revalidateTag('product-prices').
Pages are immediately marked stale and regenerated on the next request. The lesson
is that cache TTL must match the actual freshness requirements of the data.
```

---

## Case Study 4 — Login Page Shows 500 After Switching to App Router

### Incident

Team migrated a Next.js pages-router login page to the App Router. The new page uses `cookies()` to check for an existing session and redirect. Works in development but throws a 500 error in production on Vercel.

### Investigation

1. Checked Vercel function logs: `Error: cookies() was called outside a request scope`
2. Traced to the layout:
   ```tsx
   // app/layout.tsx
   import { cookies } from 'next/headers';
   
   const session = await cookies().get('auth');  // called at module level!
   
   export default function RootLayout({ children }: { children: React.ReactNode }) {
     return <html><body>{children}</body></html>;
   }
   ```
3. `cookies()` was called outside of any async function — at module initialization time
4. Next.js `headers()` and `cookies()` are request-scoped dynamic functions — they can only be called within async Server Component render functions, not at module level

### Root Cause

`cookies()`, `headers()`, `searchParams`, and other dynamic functions must be called within the render function of a Server Component (inside an `async function` that React calls per-request). Calling them at module level causes them to run once when the module loads — outside of any request context.

### Fix

```tsx
// WRONG — called at module level
const session = await cookies().get('auth');

// CORRECT — called inside async render function
export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const cookieStore = await cookies();  // Next.js 15: cookies() returns a Promise
  const session = cookieStore.get('auth');
  
  if (!session) redirect('/login');
  
  return <html><body>{children}</body></html>;
}

// Note: In Next.js 15, cookies() and headers() are now async — must be awaited
const cookieStore = await cookies();
const headersList = await headers();
```

### Prevention

- Never call dynamic functions (`cookies()`, `headers()`, `params`, `searchParams`) at module level
- Enable the ESLint plugin for Next.js — it catches some of these patterns
- Always put session checks inside the component function body, not at the top of the file

---

## Case Study 5 — Client Bundle 1.8MB Causing 8-Second First Load

### Incident

New Next.js marketing site takes 8 seconds to load on mobile. Lighthouse scores 12/100 for Performance. Users on slow connections abandon before seeing any content.

### Investigation

1. Ran `npx @next/bundle-analyzer` — visualized the bundle composition
2. Found three massive modules:
   - `moment.js` with all locales: 329KB
   - `@fullcalendar/react` imported on every page: 680KB
   - `lodash` (full build, not tree-shaken): 71KB
3. Also found: an entire admin data table imported in a marketing page by accident

### Root Cause

Uncritical large dependencies imported without regard for bundle size, and no code splitting between pages.

### Fix

```tsx
// Fix 1: Replace moment.js
// Before: import moment from 'moment'; — 329KB
// After: use Intl or date-fns
const formatted = new Intl.DateTimeFormat('en-US', { dateStyle: 'long' }).format(date);
// Or: import { format } from 'date-fns'; — 2KB for just what you need

// Fix 2: Lazy load heavy components
// Before: import FullCalendar from '@fullcalendar/react'; at top — loads on every page
// After: dynamic import only when the calendar page loads
import dynamic from 'next/dynamic';
const FullCalendar = dynamic(() => import('@fullcalendar/react'), {
  loading: () => <CalendarSkeleton />,
  ssr: false,  // skip SSR for this heavy component
});

// Fix 3: Tree-shake lodash
// Before: import _ from 'lodash';  — 71KB
// After: import specific functions
import debounce from 'lodash/debounce';  // 2KB
// Better: write it yourself or use the native Web API

// Fix 4: Route-based code splitting (automatic in Next.js)
// Each page.tsx and its imports are code-split automatically
// Do NOT import admin pages in marketing pages

// Fix 5: Analyze the bundle regularly
// package.json script:
// "analyze": "ANALYZE=true next build"
// next.config.ts:
const withBundleAnalyzer = require('@next/bundle-analyzer')({
  enabled: process.env.ANALYZE === 'true',
});
module.exports = withBundleAnalyzer({});
```

After fixes: Bundle reduced from 1.8MB to 340KB. First Load JS reduced from 1.2MB to 180KB. Lighthouse performance: 87/100.

### Prevention

- Set a bundle size budget in CI — fail the build if bundle exceeds threshold (`bundlesize` npm package)
- Run bundle analyzer on every major dependency addition
- Never import from `lodash` without checking if the native alternative exists
- `next/dynamic` for any component > 50KB that is not needed above the fold

### 60-Second Interview Answer

```text
Our marketing site had an 8-second first load because we shipped 1.8MB of JavaScript.
Bundle analysis showed three causes: moment.js loaded with all locales, a calendar
library imported on every page, and lodash imported as a full bundle. We replaced
moment with the native Intl API, lazy-loaded the calendar with next/dynamic only
on the calendar page, and replaced lodash with specific function imports or native
methods. Bundle dropped from 1.8MB to 340KB and Lighthouse performance went from
12 to 87. Prevention is a CI bundle size budget that fails the build if you add
an unreviewed large dependency.
```
