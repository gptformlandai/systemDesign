# React + Next.js Active Recall Question Bank — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: cover the answer, recall it, then check | ✅ ⚠️ ❌

---

## How to Use

Read the question. Form your answer. Then reveal the answer below. Mark ✅ ⚠️ ❌.
Re-test ❌ and ⚠️ every 2 days. Target: all ✅ before your interview.

Difficulty tiers: 🟢 Foundation | 🟡 Intermediate | 🔴 MAANG

---

## Part 1: Browser and Rendering Foundations

**Q1 🟢** What are the 6 steps in the browser rendering pipeline?

> Parse HTML (DOM) → Parse CSS (CSSOM) → Build render tree → Layout → Paint → Composite

**Q2 🟢** What is hydration?

> React attaches event handlers and client-side behavior to server-rendered HTML. The HTML was already sent by the server; hydration makes it interactive.

**Q3 🟡** What causes a hydration mismatch and what are two ways to fix it?

> Server renders different HTML than what React would render on the client. Fix 1: move time/browser-dependent values to `useEffect`. Fix 2: `suppressHydrationWarning` for intentionally differing values (e.g., timestamps).

**Q4 🟡** What is the difference between LCP, INP, and CLS?

> LCP (Largest Contentful Paint) = loading speed. INP (Interaction to Next Paint) = responsiveness. CLS (Cumulative Layout Shift) = visual stability. Good: LCP < 2.5s, INP < 200ms, CLS < 0.1.

**Q5 🔴** When does the browser block rendering, and how do you prevent it?

> Render-blocking: synchronous `<script>` tags in `<head>`, large synchronous CSS. Prevention: `defer`/`async` on scripts, split critical CSS, preload key assets with `<link rel="preload">`.

---

## Part 2: React Core

**Q6 🟢** What are the three phases of React rendering?

> Trigger → Render → Commit. Trigger (state/prop change), Render (React calls components to compute new tree), Commit (React applies DOM changes and runs effects).

**Q7 🟢** Why should you never use array index as a key in a dynamic list?

> Index keys tie React's identity system to position. When items are inserted, deleted, or reordered, indices shift — React reuses DOM nodes for the wrong items, causing state, focus, and input value bugs.

**Q8 🟡** What is the difference between controlled and uncontrolled components?

> Controlled: React state owns the input value (value + onChange). Uncontrolled: DOM owns the value, read via ref on submit. Controlled = easier validation and conditional logic; uncontrolled = less re-renders for large forms.

**Q9 🟡** What happens when you call `setState` inside `useEffect` with no dependency array?

> Infinite loop. No dependency array = effect runs after every render. `setState` triggers a render. Render triggers the effect again. Use `[]` to run once, or add specific deps.

**Q10 🟡** What does `React.memo` do and when does it fail?

> `React.memo` skips re-rendering if props are shallowly equal. It fails when props include inline functions, inline objects, or inline arrays — these create new references every render, making shallow equality always false. Fix: `useCallback` for functions, `useMemo` for objects.

**Q11 🔴** Explain React's reconciliation algorithm and what "keys" tell the reconciler.

> React compares the previous and next render tree (diffing). It assumes same position = same component type. Keys give stable identity to list items. Without keys, React maps items to positions — inserting at the start causes all items to update. With stable keys, React knows "this item moved" vs "this is a new item".

---

## Part 3: Hooks

**Q12 🟢** What are the two rules of hooks?

> 1. Only call hooks at the top level (not in conditions, loops, or nested functions). 2. Only call hooks in React functions or custom hooks. Reason: React uses call order to track hook state across renders.

**Q13 🟢** What is the difference between `useState` and `useRef`?

> `useState` triggers a re-render when the value changes. `useRef` does not. Use `useRef` for mutable values that should not affect the UI (timer IDs, DOM refs, previous values).

**Q14 🟡** What is a stale closure and how do you fix it in `useEffect`?

> A stale closure captures an old value from a previous render. In `useEffect`, if the dependency array is missing a dependency, the effect function reads the old value. Fix: add the dependency, or use a `useRef` to always hold the latest value.

**Q15 🟡** When should you use `useReducer` over `useState`?

> When state transitions are complex (multiple sub-values that change together), when next state depends on previous state in non-trivial ways, or when you want to model explicit state machines (idle/loading/success/error). `useReducer` transitions are testable as pure functions.

**Q16 🟡** What is the difference between `useTransition` and `useDeferredValue`?

> `useTransition` wraps a setter call — you control the source. `useDeferredValue` defers a received value — you do not control the setter (it comes from a parent). Both mark work as non-urgent for concurrent rendering.

**Q17 🔴** What does `useLayoutEffect` do differently from `useEffect`?

> `useLayoutEffect` fires synchronously after DOM mutations but before the browser paints. Use for measuring DOM and updating state/position before the user sees a flash. `useEffect` fires after paint — safe for most side effects. Overusing `useLayoutEffect` blocks painting.

---

## Part 4: Next.js App Router

**Q18 🟢** What is the difference between a Server Component and a Client Component?

> Server Components render on the server only — no client JS shipped. They can fetch data, use server secrets, and access databases directly. Client Components are marked `'use client'` and run in the browser — needed for state, effects, event handlers, and browser APIs.

**Q19 🟢** What file conventions does the App Router use?

> `page.tsx` = route content, `layout.tsx` = persistent shell, `loading.tsx` = Suspense fallback, `error.tsx` = Error Boundary (must be `'use client'`), `not-found.tsx` = 404, `template.tsx` = layout that remounts on navigation.

**Q20 🟡** What is the difference between `revalidatePath` and `revalidateTag`?

> `revalidatePath('/posts')` invalidates the cached page at that URL. `revalidateTag('posts')` invalidates all fetches that were tagged with `'posts'`. Tags are more targeted — a mutation to one post can revalidate just that post's tag.

**Q21 🟡** When would you use parallel routes vs intercepting routes?

> Parallel routes: render multiple independent page components simultaneously in one layout (e.g., dashboard with sidebar slots). Intercepting routes: same URL shows different UI depending on how you arrived (e.g., clicking a photo from a feed shows a modal; sharing that URL shows the full page).

**Q22 🟡** What are the four fetch caching options in Next.js?

> `force-cache` (default when static) — cache indefinitely. `no-store` — never cache, always fresh. `{ next: { revalidate: N } }` — ISR, stale after N seconds. `{ next: { tags: ['tag'] } }` — cache until `revalidateTag('tag')` is called.

**Q23 🔴** Explain Request Memoization in Next.js.

> Within a single server render pass, identical `fetch()` calls (same URL + options) are deduplicated — only one HTTP request is made. This applies to the render phase only, not across requests. Use `React.cache()` to get the same deduplication for non-fetch functions (DB calls, etc.).

---

## Part 5: Performance

**Q24 🟡** Name three causes of a large JavaScript bundle in Next.js.

> 1. Large libraries not tree-shaken (moment.js, full lodash). 2. Heavy components not lazy-loaded with `next/dynamic`. 3. Server-only code accidentally imported in Client Components. 4. Duplicate dependencies at different versions.

**Q25 🟡** What does `next/dynamic` do and when do you use it?

> `next/dynamic` is dynamic import with SSR control. It splits the component into a separate chunk loaded only when first rendered. Use for: heavy components not needed above the fold, client-only components that would cause hydration issues if SSR'd, admin/editor widgets.

**Q26 🟡** What is the `priority` prop on `next/image` and why does it matter?

> `priority` tells Next.js to add a `<link rel="preload">` for the image in the page `<head>`. Without it, the LCP image loads after the JS evaluates — slow LCP. Use `priority` on the hero image or any image visible above the fold on initial load.

**Q27 🔴** What is ISR and what is its cache invalidation risk?

> ISR (Incremental Static Regeneration) generates static pages at build time and regenerates them after a time interval or on-demand trigger. Risk: between revalidations, users see stale data. For frequently changing data (prices, availability), use on-demand revalidation via webhook rather than a time-based TTL.

---

## Part 6: Security

**Q28 🟡** What is the primary security rule for Server Actions?

> Never trust client-sent data or user IDs. Always authenticate inside the action (get user from server-side session), always validate input with Zod, always authorize that the authenticated user owns the resource being modified.

**Q29 🟡** What is the difference between `httpOnly` and `sameSite` cookies?

> `httpOnly`: cookie cannot be read by JavaScript — prevents XSS cookie theft. `sameSite`: controls cross-site request behavior — `strict` prevents CSRF by blocking cross-origin requests, `lax` allows navigation links, `none` allows all cross-site (requires `secure`).

**Q30 🔴** How does Next.js protect Server Actions from CSRF?

> Next.js automatically validates the `Origin` header for Server Action requests. It rejects requests from origins other than the app's own origin. This is automatic — developers do not need to add CSRF tokens for Server Actions (unlike traditional API routes where manual CSRF protection may be needed).

---

## Quick Recall — Fill in the Blank

| Statement | Answer |
|---|---|
| `useEffect` with empty `[]` runs ___ | Once on mount |
| `useEffect` with no deps array runs ___ | After every render |
| `useLayoutEffect` fires ___ the browser paints | Before |
| `React.memo` uses ___ equality for props | Shallow |
| `useCallback` memoizes a ___ | Function reference |
| `useMemo` memoizes a ___ | Computed value |
| Next.js `error.tsx` must be a ___ Component | Client |
| `'use server'` marks a file/function as a ___ | Server Action |
| `revalidateTag` invalidates ___ | All fetches tagged with that string |
| ISR `revalidate: 60` means stale after ___ | 60 seconds |
| `suppressHydrationWarning` suppresses ___ | One-level hydration mismatch warnings |
| `use client` pushes the client bundle boundary ___ | Down the tree |
| `next/dynamic` with `ssr: false` ___ server render | Skips |
