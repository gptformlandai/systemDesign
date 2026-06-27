# React + Next.js Mock Interview Scripts — Gold Sheet

> Track File #38 of 40 · Group 9: Practice Upgrade
> Format: timed round → interviewer questions → expected coverage → follow-ups

---

## How to Use

Pair with a friend or self-interview against a mirror. Set a timer. Interviewer reads questions. Candidate answers out loud (no notes). Debrief against expected coverage after each question.

---

## Round 1 — Foundations (25 minutes)

**Focus:** Browser rendering, React basics, component model

---

**Q1 (3 min):** "Walk me through what happens in the browser from the moment you type a URL to when the user sees content."

Expected coverage:
- DNS lookup → TCP connection → TLS handshake
- Server sends HTML
- Browser parses HTML → builds DOM
- Parses CSS → builds CSSOM
- Constructs render tree (DOM + CSSOM)
- Layout pass (box positions) → Paint → Composite layers

Follow-up: "Where can JavaScript block this process?"
Expected: `<script>` in `<head>` without defer/async blocks HTML parsing; render-blocking CSS blocks paint.

---

**Q2 (4 min):** "Explain how React decides when to re-render a component."

Expected coverage:
- State change (via setState)
- Props change (parent re-renders, child re-renders unless memoized)
- Context value change (all consumers re-render)
- Reconciliation: React compares virtual DOM trees, applies minimal DOM changes

Follow-up: "If a parent re-renders and passes a function as a prop to a memoized child, what happens?"
Expected: Inline functions create a new reference on every render → React.memo's shallow comparison fails → child re-renders. Fix: useCallback.

---

**Q3 (3 min):** "What is the difference between keys and IDs in React lists?"

Expected coverage:
- Key: React-internal identity for reconciler (string/number, not exposed to component)
- ID: application data, can be used as key
- Why not index: index changes on insertion/deletion → React maps wrong node to wrong item
- Rule: stable, unique across siblings, from data (not Math.random())

---

**Q4 (3 min):** "When would you use a class component in 2024?"

Expected coverage:
- Almost never — hooks cover 99% of cases
- Only remaining reason: Error Boundaries must be class components (`componentDidCatch`)
- `react-error-boundary` library wraps this for function components
- Correct senior answer: "I'd use `react-error-boundary` package instead of writing class components"

---

**Q5 (4 min):** "Explain controlled vs uncontrolled components. When do you choose each?"

Expected coverage:
- Controlled: React state is the source of truth (`value + onChange`)
- Uncontrolled: DOM is the source of truth, read via `ref` on submit
- Controlled: good for real-time validation, conditional fields, dynamic forms
- Uncontrolled: simpler for file inputs; sometimes used in very large forms for perf
- Most form libraries (RHF) use uncontrolled by default for performance

---

**Q6 (8 min, practical):** "Live code: Build a component that accepts an array of items, renders them in a list, and lets you mark items as done. Clicking a done item removes it from the list."

Expected:
```tsx
function TodoList({ initialItems }: { initialItems: string[] }) {
  const [items, setItems] = useState(initialItems);
  const remove = (idx: number) => setItems(prev => prev.filter((_, i) => i !== idx));
  return (
    <ul>
      {items.map((item, idx) => (
        <li key={item} onClick={() => remove(idx)} style={{ cursor: 'pointer' }}>
          ✓ {item}
        </li>
      ))}
    </ul>
  );
}
```

Follow-up: "What's wrong with using `idx` as the key if items have stable IDs?"
Expected: Using `idx` means React remaps identity when items are removed — can cause animation/transition bugs. Use stable IDs.

---

## Round 2 — Hooks and State (30 minutes)

**Focus:** All hooks, state patterns, custom hooks, performance

---

**Q1 (4 min):** "Explain the two rules of hooks and why they exist."

Expected:
- Top-level only (not inside conditions/loops/nested functions)
- React functions only (not plain JS functions)
- Reason: React uses the order of hook calls to store state. Conditionally skipping a hook changes the order — React can't match state to the right hook.

---

**Q2 (5 min):** "You have a search input. The user types and an API call fires. After 3 months in production, you find out the component sometimes shows stale results. Diagnose the bug."

Expected coverage:
- Race condition: user types "re", then "react". Two requests fly. "react" response arrives first, then "re" response arrives and overwrites with fewer results.
- Root cause: no request cancellation
- Fix: AbortController — cancel previous request when a new one fires

```tsx
useEffect(() => {
  const controller = new AbortController();
  fetch(`/api/search?q=${query}`, { signal: controller.signal })
    .then(r => r.json())
    .then(setResults)
    .catch(e => { if (e.name !== 'AbortError') setError(e); });
  return () => controller.abort();
}, [query]);
```

---

**Q3 (4 min):** "useReducer vs useState — when do you reach for useReducer?"

Expected:
- Complex state transitions (multiple fields change together)
- Next state depends on current state in non-trivial ways
- Want to model a state machine explicitly (idle/loading/success/error)
- Bonus: reducer is a pure function — unit testable without React

---

**Q4 (5 min):** "Explain useCallback and useMemo. What's the performance trap?"

Expected:
- `useCallback(fn, deps)` returns stable function reference; prevents child re-renders
- `useMemo(fn, deps)` returns memoized computed value; skips expensive recalculation
- Trap: memoization has a cost — comparing deps arrays every render. For trivial computations, `useMemo` is slower than just computing the value. Use for: expensive computations (>1ms), stable refs to pass to memoized children.

---

**Q5 (4 min):** "When does useEffect run and when does useLayoutEffect run? Give a practical use case for each."

Expected:
- `useEffect`: after paint. Use for: data fetching, subscriptions, analytics events
- `useLayoutEffect`: after DOM mutation, before paint. Use for: measuring DOM elements and applying styles/position to prevent flash (e.g., tooltip positioning based on measured element size)

---

**Q6 (8 min, practical):** "Write a custom `useLocalStorage` hook."

Expected:
```tsx
function useLocalStorage<T>(key: string, initialValue: T) {
  const [value, setValue] = useState<T>(() => {
    if (typeof window === 'undefined') return initialValue;
    try {
      const stored = localStorage.getItem(key);
      return stored ? JSON.parse(stored) : initialValue;
    } catch {
      return initialValue;
    }
  });

  const setStored = useCallback((newValue: T | ((prev: T) => T)) => {
    setValue(prev => {
      const next = typeof newValue === 'function' ? (newValue as (p: T) => T)(prev) : newValue;
      localStorage.setItem(key, JSON.stringify(next));
      return next;
    });
  }, [key]);

  return [value, setStored] as const;
}
```

Follow-up: "What breaks if the component SSR'd?"
Expected: `localStorage` doesn't exist on server → error. Fix: lazy init with `typeof window === 'undefined'` guard.

---

## Round 3 — Next.js Architecture (35 minutes)

**Focus:** App Router, Server/Client split, caching, Server Actions

---

**Q1 (4 min):** "Explain the difference between a Server Component and a Client Component. What can each do that the other cannot?"

Expected:
| Server Component | Client Component |
|---|---|
| Access server secrets, DB, filesystem | useState, useEffect, event handlers |
| No client JS shipped | Browser APIs |
| Cannot use hooks | Can use hooks |
| Cannot handle user interaction | Can be interactive |

---

**Q2 (5 min):** "You're building a product page for an e-commerce site. Walk me through what should be a Server Component vs a Client Component."

Expected design:
- Server: `page.tsx` fetches product data — SEO-critical, no interaction
- Server: Product title, price, description rendered in HTML
- Client: "Add to Cart" button — needs state and event handler
- Client: Image gallery with click-to-zoom — needs state
- Client: Product reviews tab — needs client-side toggle
- Key insight: Push 'use client' as far down the tree as possible

---

**Q3 (5 min):** "Describe Next.js's 4-layer caching architecture."

Expected:
1. Request Memoization: deduplicates identical `fetch()` in one render pass
2. Data Cache: persistent fetch cache across requests (controlled by `revalidate` tag)
3. Full Route Cache: static HTML/RSC payload at build time
4. Router Cache: client-side cache of RSC payloads for previously visited routes

---

**Q4 (5 min):** "When would you use ISR vs Server-Side Rendering vs Static Generation for a blog?"

Expected:
- Blog post: SSG at build time — content doesn't change often, excellent performance
- Blog post with comments: ISR `revalidate: 60` — mostly static, occasionally updated
- Personalized feed: SSR — user-specific, must be fresh
- Admin dashboard: SSR — always authenticated, real-time data
- Public pricing page: SSG — rarely changes, serve from CDN edge

---

**Q5 (5 min):** "Explain Server Actions and their security model."

Expected:
- `'use server'` marks function as server-side RPC callable from client
- Replaces `POST /api/...` for mutations
- Security rules: authenticate inside the action (don't trust client data), validate with Zod, authorize ownership
- Next.js auto-protects against CSRF via Origin header validation
- Bonus: supports progressive enhancement — works without JS (HTML form + action attribute)

---

**Q6 (4 min):** "What is `revalidatePath` and when do you use it vs `revalidateTag`?"

Expected:
- `revalidatePath('/products/[id]')` — revalidates one URL path
- `revalidateTag('products')` — revalidates all fetches tagged with 'products'
- Use path when a mutation affects exactly one URL
- Use tag when a mutation affects multiple pages (e.g., a price change could appear on product page, cart, checkout summary — all tagged 'product-prices')

---

**Q7 (7 min, practical):** "Sketch a Server Action for updating a user's profile. Include auth, validation, and error handling."

Expected:
```tsx
'use server';
import { getSession } from '@/lib/auth';
import { z } from 'zod';
import { db } from '@/lib/db';
import { revalidatePath } from 'next/cache';

const ProfileSchema = z.object({
  name: z.string().min(2).max(50),
  bio: z.string().max(200).optional(),
});

export async function updateProfile(prevState: unknown, formData: FormData) {
  const session = await getSession();
  if (!session) return { error: 'Unauthorized' };

  const result = ProfileSchema.safeParse({
    name: formData.get('name'),
    bio: formData.get('bio'),
  });
  if (!result.success) return { error: result.error.flatten() };

  await db.user.update({
    where: { id: session.userId },
    data: result.data,
  });

  revalidatePath('/profile');
  return { success: true };
}
```

---

## Round 4 — Performance and Debugging (30 minutes)

**Focus:** Optimization techniques, debugging tools, root-cause analysis

---

**Q1 (4 min):** "A React app feels laggy when a user types in a search input. How do you diagnose and fix it?"

Expected investigation:
1. React DevTools Profiler: record typing, look for components that re-render on every keystroke
2. Common cause: expensive component re-renders on every keystroke (large list being filtered synchronously)

Fix options:
- `useDeferredValue` to defer the expensive filter while keeping input responsive
- `useTransition` around the state update
- Virtualize large lists (react-virtual)
- `React.memo` on list items to skip re-renders for unchanged items

---

**Q2 (4 min):** "Walk me through using the Chrome DevTools Performance tab to identify a slow page load."

Expected:
1. Open DevTools → Performance → click Record → reload page → stop
2. Look at the main thread timeline — find long tasks (red triangles = > 50ms)
3. Find LCP event marker — what triggered it? Large image or slow text?
4. Flame chart: look for deep call stacks, JavaScript parsing/execution
5. Summary panel: % time in scripting vs rendering vs painting
6. Fix targets: large JS chunks (code-split), render-blocking scripts (defer), unoptimized images (next/image)

---

**Q3 (5 min):** "Describe three ways Next.js helps optimize images vs a plain `<img>` tag."

Expected:
- Automatic format conversion: serves WebP/AVIF when browser supports it
- Responsive sizes: generates multiple sizes, serves the right one based on `sizes` prop
- Lazy loading: `loading="lazy"` by default (defer off-screen images)
- `priority`: preloads LCP images for faster LCP
- Prevents CLS: requires `width` and `height` (or `fill`) — browser reserves space before image loads

---

**Q4 (4 min):** "You deploy a Next.js app and Sentry reports 'Hydration failed' errors in production. How do you debug this?"

Expected approach:
1. `NODE_ENV=production` build locally — hydration errors are more visible in production builds
2. Look at the error detail — React 18 tells you which element mismatched
3. Common causes: time-dependent values, localStorage, window, Math.random() in render
4. Check for conditional rendering based on browser APIs
5. Fix: move browser-dependent values to `useEffect`, or use `suppressHydrationWarning` for intentionally dynamic values

---

**Q5 (5 min):** "How do you prevent waterfall data fetching in Next.js?"

Expected:
- Problem: sequential `await` in a Server Component causes N sequential round trips
  ```tsx
  const user = await fetchUser();
  const posts = await fetchPosts(user.id);  // waits for user first — waterfall
  ```
- Fix 1: parallel `Promise.all` for independent data:
  ```tsx
  const [user, config] = await Promise.all([fetchUser(), fetchConfig()]);
  ```
- Fix 2: start fetching early (don't await immediately):
  ```tsx
  const userPromise = fetchUser();   // starts immediately
  const postsPromise = fetchPosts(); // starts immediately
  const [user, posts] = await Promise.all([userPromise, postsPromise]);
  ```
- Fix 3: `React.cache()` deduplication for repeated fetches

---

**Q6 (8 min, practical):** "Write a `useAsync` hook that tracks loading/error/data state for any async function."

Expected:
```tsx
type AsyncState<T> = 
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };

function useAsync<T>(asyncFn: () => Promise<T>, deps: unknown[]) {
  const [state, setState] = useState<AsyncState<T>>({ status: 'idle' });
  
  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });
    asyncFn()
      .then(data => { if (!cancelled) setState({ status: 'success', data }); })
      .catch(error => { if (!cancelled) setState({ status: 'error', error }); });
    return () => { cancelled = true; };
  }, deps);  // eslint-disable-line react-hooks/exhaustive-deps
  
  return state;
}
```

---

## Round 5 — System Design (40 minutes)

**Focus:** Architecture decisions, tradeoffs, scalability

---

**Q1 (15 min):** "Design the frontend architecture for a real-time collaborative document editor (like Notion or Google Docs). Cover: component architecture, state management, real-time sync, and how you'd handle offline/reconnection."

Expected coverage:
- Server Components for initial load, Client Components for editor interactivity
- State: local document state + OT/CRDT for conflict-free concurrent edits
- Real-time: WebSocket connection, broadcast changes to all connected clients
- Optimistic updates: apply change locally immediately, sync server asynchronously
- Offline: IndexedDB as local buffer, sync queue, reconnection replay
- Presence: cursor positions, user names via separate presence WebSocket channel

---

**Q2 (15 min):** "Design a large e-commerce product listing page that serves 10 million users. Cover: rendering strategy, caching, performance, and personalization."

Expected coverage:
- SSG for base product pages (fast, CDN-served)
- ISR with short TTL for prices/availability
- Client island for: add to cart, wishlist toggle, personalized "Recommended for you"
- Edge middleware for: A/B testing, geolocation-based pricing, session cookie check
- CDN caching: static HTML served from edge, ISR regeneration at origin
- Personalization: inject via streaming after static shell, not in the HTML itself (to keep CDN cacheability)

---

**Q3 (10 min):** "You're inheriting a large React codebase with no tests. What's your strategy for adding a safety net incrementally?"

Expected:
- Start with E2E tests for critical user paths (checkout, login) — highest value per test written
- Add integration tests for complex components/pages
- Unit tests for utility functions and hooks only (avoid over-testing implementation details)
- Tools: Playwright for E2E, React Testing Library + Vitest for component tests, MSW for API mocking
- Golden rule: test behavior not implementation — don't test component internals

---

## Quick Debrief Criteria After Each Round

| Score | Criteria |
|---|---|
| 5 | Complete, articulate, handled follow-ups, bonus depth |
| 4 | Correct with minor gaps, no major misses |
| 3 | Got the main idea, missed some key details |
| 2 | Partially correct, missed core concepts |
| 1 | Off track or incorrect — revisit that section |

Target: all 4+ before your MAANG interview.
