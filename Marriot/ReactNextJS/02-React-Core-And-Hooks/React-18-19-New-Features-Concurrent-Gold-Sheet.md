# React 18 & 19 — New Features, Concurrent Mode, and Modern APIs Gold Sheet

> Track Module - Group 2: React Core And Hooks
> Level: intermediate → MAANG | React 18 concurrent features, React 19 server model

---

## 1. Intuition

React 18 changed the rendering model from synchronous to concurrent. React 19 extended this into the server, turning server-side functions into first-class React concepts.

```text
React 17 and earlier:
  state update → synchronous re-render → browser blocks until done

React 18+:
  state update → concurrent render → React can pause, interrupt, resume
  → urgent updates (typing) take priority over non-urgent (filter results)

React 19:
  async data access via use()
  server functions (Server Actions)
  form actions as first-class React
  improved error and hydration handling
```

---

## 2. React 18 — Automatic Batching

Before React 18, state updates inside async functions or event handlers outside React events were NOT batched — each caused a separate re-render.

```tsx
// React 17: two re-renders (fetch callback is not a React synthetic event)
fetch('/api/data').then(() => {
  setLoading(false);   // re-render 1
  setData(result);     // re-render 2
});

// React 18: ONE re-render — automatic batching everywhere
fetch('/api/data').then(() => {
  setLoading(false);   // batched
  setData(result);     // batched → single re-render
});

// To opt out of batching when needed:
import { flushSync } from 'react-dom';

flushSync(() => setLoading(false));  // forces immediate synchronous re-render
flushSync(() => setData(result));    // forces another synchronous re-render
// Use flushSync only when third-party DOM libraries need the DOM updated immediately
```

---

## 3. React 18 — useTransition

`useTransition` marks a state update as non-urgent. React renders the urgent update first, then applies the transition when the browser has idle time.

```tsx
import { useTransition, useState } from 'react';

function SearchResults() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Product[]>([]);
  const [isPending, startTransition] = useTransition();

  function handleSearch(value: string) {
    setQuery(value);  // urgent — update input immediately (no delay)
    
    startTransition(() => {
      // non-urgent — React can defer this if higher-priority work arrives
      setResults(filterProducts(value));  // potentially expensive filter
    });
  }

  return (
    <div>
      <input value={query} onChange={e => handleSearch(e.target.value)} />
      
      {/* Show stale results with opacity while new ones are computing */}
      <div style={{ opacity: isPending ? 0.6 : 1 }}>
        <ProductList products={results} />
      </div>
    </div>
  );
}
```

**When to use `useTransition`:**
- Expensive filter/sort on large datasets
- Tab switching where new content takes time to render
- Navigation where the next page renders many components
- Any update where keeping the current UI visible while computing is better UX

**Not for:** input typing (that should always be immediate), form submission state.

---

## 4. React 18 — useDeferredValue

`useDeferredValue` defers a VALUE rather than wrapping a SET call. Use it when you do not control the source of an update.

```tsx
import { useDeferredValue, memo } from 'react';

function SearchPage() {
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  
  // deferredQuery lags behind query — input stays responsive
  // during transition, deferredQuery === previous value
  
  const isStale = query !== deferredQuery;

  return (
    <div>
      <input value={query} onChange={e => setQuery(e.target.value)} />
      
      <div style={{ opacity: isStale ? 0.6 : 1 }}>
        {/* ExpensiveResults only re-renders when deferredQuery changes */}
        <ExpensiveResults query={deferredQuery} />
      </div>
    </div>
  );
}

// Must be memoized to benefit from deferral — otherwise renders on every query change anyway
const ExpensiveResults = memo(function ExpensiveResults({ query }: { query: string }) {
  const results = heavyFilter(query);  // expensive
  return <ResultList items={results} />;
});
```

**`useTransition` vs `useDeferredValue`:**
- `useTransition`: you control the setter (wrap the `setState` call)
- `useDeferredValue`: you receive the value from outside (prop, param) — cannot wrap the setter

---

## 5. React 18 — useId

`useId` generates stable, unique IDs that are consistent between server and client renders. Solves the hydration mismatch problem with IDs.

```tsx
import { useId } from 'react';

function FormField({ label, type = 'text' }: { label: string; type?: string }) {
  // Stable across server/client renders — no hydration mismatch
  const id = useId();
  
  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} aria-describedby={`${id}-description`} />
      <p id={`${id}-description`}>Helper text for {label}</p>
    </div>
  );
}

// Do NOT use for list keys — useId is not for that
// Do NOT use Math.random() or Date.now() — causes hydration mismatch
```

---

## 6. React 18 — Suspense and Streaming SSR

React 18 upgraded Suspense from a lazy-loading tool to a first-class streaming mechanism. With Next.js, components can stream HTML to the browser progressively.

```tsx
// Suspense wraps components that "suspend" — async data loading
import { Suspense } from 'react';

function Dashboard() {
  return (
    <div>
      <Header />  {/* renders immediately */}
      
      <Suspense fallback={<Spinner />}>
        <UserProfile />  {/* suspends while loading — streams later */}
      </Suspense>
      
      <Suspense fallback={<ChartSkeleton />}>
        <AnalyticsChart />  {/* streams independently of UserProfile */}
      </Suspense>
    </div>
  );
}
```

```text
Without streaming (React 17):
  server must wait for ALL data → then sends entire HTML → browser renders

With React 18 streaming:
  server sends shell (Header) immediately
  browser renders shell
  UserProfile data arrives → server streams UserProfile HTML → browser inserts
  AnalyticsChart data arrives independently → streams separately
  
Result: user sees content faster even when parts are slow
```

---

## 7. React 19 — use() Hook

`use()` is a new hook that can read async resources (Promises, Context) directly during render. It integrates with Suspense.

```tsx
import { use, Suspense } from 'react';

// Server Component creates the Promise
async function ProductsPage() {
  const productsPromise = getProducts();  // returns Promise<Product[]> — NOT awaited yet
  
  return (
    <Suspense fallback={<ProductsSkeleton />}>
      {/* ProductList receives the promise and reads it with use() */}
      <ProductList productsPromise={productsPromise} />
    </Suspense>
  );
}

// Client or Server Component reads the promise
function ProductList({ productsPromise }: { productsPromise: Promise<Product[]> }) {
  // use() suspends the component while the promise is pending
  // Suspense boundary above shows fallback
  const products = use(productsPromise);
  
  return <ul>{products.map(p => <ProductItem key={p.id} product={p} />)}</ul>;
}

// use() also reads Context (alternative to useContext)
function ThemedComponent() {
  const theme = use(ThemeContext);  // same as useContext(ThemeContext)
  return <div className={theme}>...</div>;
}
```

**Key difference from `useEffect` + fetch:**
- `use()` suspends cleanly — the component does not render in a loading state, it simply does not render yet
- Works with Suspense boundaries for streaming
- Cannot be in conditional or loops (unlike `useContext` — though `use()` CAN be conditional)

---

## 8. React 19 — useActionState (Form Actions)

`useActionState` connects React state to server-side or async actions.

```tsx
'use client';

import { useActionState } from 'react';
import { createPost } from '@/actions/posts';  // server action

type ActionState = {
  success: boolean;
  error: string | null;
  postId: string | null;
};

const initialState: ActionState = { success: false, error: null, postId: null };

function CreatePostForm() {
  // [state, formAction, isPending]
  const [state, formAction, isPending] = useActionState(
    createPost,     // the server action
    initialState,   // initial state value
  );

  return (
    <form action={formAction}>
      <input name="title" placeholder="Post title" required />
      <textarea name="content" placeholder="Content" required />
      
      <button type="submit" disabled={isPending}>
        {isPending ? 'Creating...' : 'Create Post'}
      </button>
      
      {state.error && <p role="alert" style={{color: 'red'}}>{state.error}</p>}
      {state.success && <p style={{color: 'green'}}>Post created! ID: {state.postId}</p>}
    </form>
  );
}
```

```tsx
// Server action side
'use server';

export async function createPost(prevState: ActionState, formData: FormData): Promise<ActionState> {
  const title = String(formData.get('title'));
  const content = String(formData.get('content'));
  
  if (!title || !content) {
    return { success: false, error: 'Title and content are required', postId: null };
  }
  
  try {
    const post = await db.post.create({ data: { title, content } });
    revalidatePath('/posts');
    return { success: true, error: null, postId: post.id };
  } catch {
    return { success: false, error: 'Failed to create post', postId: null };
  }
}
```

---

## 9. React 19 — useOptimistic

`useOptimistic` shows an optimistic update immediately while an async action completes.

```tsx
'use client';

import { useOptimistic, startTransition } from 'react';

type Message = { id: string; content: string; status: 'sent' | 'pending' };

function ChatThread({ messages, roomId }: { messages: Message[]; roomId: string }) {
  const [optimisticMessages, addOptimisticMessage] = useOptimistic(
    messages,
    (currentMessages: Message[], newMessage: Message) => [...currentMessages, newMessage],
  );

  async function sendMessage(content: string) {
    const optimisticMsg: Message = {
      id: `temp-${Date.now()}`,
      content,
      status: 'pending',  // shown as sending
    };

    // Show optimistic message immediately
    startTransition(() => {
      addOptimisticMessage(optimisticMsg);
    });

    // Send to server — if it fails, optimistic update is reverted automatically
    await sendMessageToServer(roomId, content);
    // After server responds, the real messages prop updates and replaces the optimistic state
  }

  return (
    <div>
      {optimisticMessages.map(msg => (
        <MessageBubble
          key={msg.id}
          message={msg}
          faded={msg.status === 'pending'}
        />
      ))}
      <MessageInput onSend={sendMessage} />
    </div>
  );
}
```

---

## 10. React 19 — Improved Error and Hydration Handling

React 19 improved error boundaries to distinguish hydration errors from render errors, and provides better messaging.

```tsx
// React 19 — separate handlers for different error types
import { createRoot } from 'react-dom/client';

const root = createRoot(document.getElementById('root')!, {
  onUncaughtError: (error, errorInfo) => {
    // Errors not caught by any Error Boundary
    reportToSentry(error, {context: 'uncaught', componentStack: errorInfo.componentStack});
  },
  onCaughtError: (error, errorInfo) => {
    // Errors caught by an Error Boundary
    reportToSentry(error, {context: 'caught', componentStack: errorInfo.componentStack});
  },
  onRecoverableError: (error, errorInfo) => {
    // Recoverable errors (hydration mismatch that React auto-fixed)
    // Log these but do not alert — React recovered
    console.warn('Recoverable error:', error);
  },
});
```

React 19 also introduced the `ref` prop directly — `forwardRef` is no longer required in React 19:

```tsx
// React 19 — ref as a regular prop (no forwardRef needed)
function Input({ ref, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { ref?: React.Ref<HTMLInputElement> }) {
  return <input ref={ref} {...props} />;
}

// React 18 and earlier — requires forwardRef
const Input = React.forwardRef<HTMLInputElement, Props>((props, ref) => (
  <input ref={ref} {...props} />
));
```

---

## 11. React 18 Root API

```tsx
// Old React 17 root
import ReactDOM from 'react-dom';
ReactDOM.render(<App />, document.getElementById('root'));

// New React 18+ root (required for concurrent features)
import { createRoot } from 'react-dom/client';
const root = createRoot(document.getElementById('root')!);
root.render(<App />);

// For hydration (SSR apps)
import { hydrateRoot } from 'react-dom/client';
hydrateRoot(document.getElementById('root')!, <App />);
```

---

## 12. Summary Table — React 18 vs 19 Features

| Feature | Version | Purpose | When to Use |
|---|---|---|---|
| Automatic Batching | 18 | Fewer re-renders in async code | Automatic — nothing to do |
| `useTransition` | 18 | Defer non-urgent state updates | Expensive renders triggered by user input |
| `useDeferredValue` | 18 | Defer a received value | When you can't wrap the setter |
| `useId` | 18 | Stable IDs for SSR/hydration | Form field IDs, accessibility |
| Streaming Suspense | 18 | Progressive HTML streaming | Next.js server components |
| `use()` | 19 | Read promises/context in render | Suspense-integrated async data |
| `useActionState` | 19 | Form + server action state | Form submissions with server actions |
| `useOptimistic` | 19 | Instant UI update before server responds | Likes, cart, messages |
| `ref` as prop | 19 | No more `forwardRef` boilerplate | All new components in React 19 |

---

## 13. Strong Interview Answer

**Q: What are the most important changes in React 18?**

```text
React 18's most important change is the concurrent rendering model. Automatic
batching reduces unnecessary re-renders in async code. useTransition and
useDeferredValue let you mark expensive renders as non-urgent so the browser
stays responsive to user input. Streaming Suspense enables progressive HTML
delivery from the server — users see content before all data is ready. These
changes are complementary: the concurrent model is the foundation, and the
new hooks are tools to take advantage of it.
```

**Q: What is the difference between useTransition and useDeferredValue?**

```text
Both defer non-urgent work, but the API differs. useTransition wraps a setState
call — you have control of the setter. useDeferredValue defers a value you receive
as a prop or from context — you cannot wrap the setter. Use useTransition when
triggering from user interaction; use useDeferredValue when receiving a value
from a parent.
```

---

## 14. Revision Notes

- React 18: automatic batching everywhere (async too), `useTransition` for non-urgent updates, `useDeferredValue` for values you don't control, `useId` for stable SSR IDs, streaming Suspense
- React 19: `use()` reads promises/context in render (suspends cleanly), `useActionState` for form+server action state, `useOptimistic` for instant UI updates, `ref` as prop (no `forwardRef`)
- `flushSync` is the escape hatch when you need synchronous DOM update (rare — third-party integration)
- `useTransition` → you control the setter. `useDeferredValue` → you receive the value
- Concurrent mode does not require you to change your code — but the new hooks unlock its benefits
