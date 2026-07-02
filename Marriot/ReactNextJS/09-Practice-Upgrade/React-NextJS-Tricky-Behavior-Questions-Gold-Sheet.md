# React + Next.js Tricky Behavior Questions — Gold Sheet

> Track Module - Group 9: Practice Upgrade
> Format: code snippet → question → detailed answer | cover answer, then check

---

## How to Use This File

1. Read the code snippet and the question.
2. Form your answer before reading the explanation.
3. Check your answer.
4. Mark: ✅ correct on first try | ⚠️ partial | ❌ wrong or guessed

Re-test ❌ and ⚠️ every 2 days until all are ✅.

---

## Part 1: State and Re-renders

### Q1 — How many times does this component re-render on button click?

```tsx
function Counter() {
  const [count, setCount] = useState(0);
  const [text, setText] = useState('');

  console.log('render');

  return (
    <button onClick={() => {
      setCount(1);
      setCount(1);
      setText('clicked');
    }}>
      Click me
    </button>
  );
}
```

**Question:** How many console.log('render') outputs appear after one click?

**Answer:**
ONE render. React 18 automatically batches all state updates inside event handlers into a single re-render. Even though `setCount` is called twice and `setText` is called once, React processes all three updates together and triggers a single re-render. In React 17 and earlier, this batching only applied to synthetic event handlers. React 18 extended batching to all contexts including `setTimeout`, `Promise.then`, and `fetch` callbacks.

---

### Q2 — What does this component display after three rapid button clicks?

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button onClick={() => {
      setCount(count + 1);  // stale closure!
      setCount(count + 1);
      setCount(count + 1);
    }}>
      Count: {count}
    </button>
  );
}
```

**Question:** After one click, what is the displayed count?

**Answer:**
`Count: 1`. All three `setCount(count + 1)` calls read `count` from the same render closure — `count` is `0` each time. So all three calls are effectively `setCount(0 + 1)` = `setCount(1)`. React merges them into a single update to `1`.

To increment three times per click, use functional updates:
```tsx
setCount(c => c + 1);
setCount(c => c + 1);
setCount(c => c + 1);
// Count becomes 3 after one click
```
Functional updates receive the LATEST state — not the captured closure value.

---

### Q3 — What is the output after clicking the button?

```tsx
function App() {
  const [items, setItems] = useState([1, 2, 3]);

  function addItem() {
    items.push(4);  // direct mutation
    setItems(items);
  }

  return <button onClick={addItem}>Add: {items.length}</button>;
}
```

**Question:** Does the displayed number change after clicking? Why?

**Answer:**
The display does NOT update. `items.push(4)` mutates the existing array in place. When `setItems(items)` is called, React compares the new value to the previous value using `Object.is`. Since it is the same array reference (mutation does not create a new reference), React considers state unchanged and skips the re-render.

Fix:
```tsx
function addItem() {
  setItems(prev => [...prev, 4]);  // new array reference → triggers re-render
}
```

---

### Q4 — What does the alert show?

```tsx
function Timer() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const id = setInterval(() => {
      setCount(count + 1);
    }, 1000);
    return () => clearInterval(id);
  }, []);  // empty deps

  function handleAlert() {
    setTimeout(() => alert(count), 3000);
  }

  return <button onClick={handleAlert}>Alert in 3s (count: {count})</button>;
}
```

**Question:** User clicks the button immediately. Count is 0. 5 seconds pass. What does the alert show?

**Answer:**
The alert shows `0`. The `handleAlert` function captures `count` from the render where the button was clicked (count was 0 at that moment). Despite 5 seconds passing and the counter incrementing (the `setInterval` will cause re-renders), the setTimeout callback has a stale closure over the original `count = 0`.

Additionally, the `setInterval` callback also has a stale closure — `count` is always 0 in the interval, so `setCount(count + 1)` = `setCount(0 + 1)` = always sets to 1, not incrementing further.

Fix with functional update:
```tsx
setCount(prev => prev + 1);  // reads latest value, not stale closure
```

---

### Q5 — What happens here?

```tsx
function Parent() {
  const [count, setCount] = useState(0);

  return (
    <>
      <button onClick={() => setCount(c => c + 1)}>Increment</button>
      <Child />
    </>
  );
}

const Child = () => {
  console.log('Child render');
  return <p>I am a child</p>;
};
```

**Question:** After clicking the button 3 times, how many times did Child render?

**Answer:**
4 times (initial render + 3 from parent re-renders). `Child` is NOT memoized. Every time `Parent` re-renders (on each count increment), `Child` re-renders too, even though it has no props that changed.

Fix with `React.memo`:
```tsx
const Child = React.memo(() => {
  console.log('Child render');
  return <p>I am a child</p>;
});
// Now Child only re-renders when its props change — zero re-renders from parent count changes
```

---

## Part 2: useEffect Behavior

### Q6 — How many API calls are made?

```tsx
function SearchBox() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);

  useEffect(() => {
    if (!query) return;
    fetch(`/api/search?q=${query}`)
      .then(r => r.json())
      .then(setResults);
  }, [query]);

  return (
    <input
      value={query}
      onChange={e => setQuery(e.target.value)}
      placeholder="Search..."
    />
  );
}
```

**Question:** User types "react" (5 keystrokes). How many API calls are made?

**Answer:**
5 API calls — one per character: "r", "re", "rea", "reac", "react". Each keystroke updates `query`, which triggers the effect. This is the debouncing problem. Without debouncing, you hammer the API with partial queries.

Fix: debounce the query before using it in the effect:
```tsx
const debouncedQuery = useDebounce(query, 300);
useEffect(() => {
  if (!debouncedQuery) return;
  fetchSearchResults(debouncedQuery).then(setResults);
}, [debouncedQuery]);
```

Also: the current code does NOT cancel in-flight requests. If fast responses arrive out of order, an earlier "r" search might overwrite the results of a later "react" search (race condition).

---

### Q7 — Is this useEffect cleanup correct?

```tsx
function EventLog() {
  const [log, setLog] = useState<string[]>([]);

  useEffect(() => {
    const handler = (event: MouseEvent) => {
      setLog(prev => [...prev, `click at ${event.x},${event.y}`]);
    };
    window.addEventListener('click', handler);
  }, []);

  return <ul>{log.map((entry, i) => <li key={i}>{entry}</li>)}</ul>;
}
```

**Question:** What is wrong with this effect?

**Answer:**
Missing cleanup — the event listener is never removed. When the component unmounts (navigates away, etc.), the handler stays attached to `window`. When `setLog` is called after unmount, React warns about updating state on an unmounted component. In a development environment with React Strict Mode, this causes the listener to be registered TWICE (Strict Mode double-invokes effects) without the second registration ever being cleaned up.

Fix:
```tsx
useEffect(() => {
  const handler = (event: MouseEvent) => {
    setLog(prev => [...prev, `click at ${event.x},${event.y}`]);
  };
  window.addEventListener('click', handler);
  return () => window.removeEventListener('click', handler);  // cleanup!
}, []);
```

---

### Q8 — What does this render on mount?

```tsx
function App() {
  const [data, setData] = useState(null);

  useEffect(() => {
    setData('loaded');
  });  // no dependency array!

  console.log('render:', data);
  return <p>{data}</p>;
}
```

**Question:** Describe the sequence of renders.

**Answer:**
Infinite render loop. Without a dependency array, `useEffect` runs after EVERY render. The sequence:
1. Initial render: `data = null` → console: "render: null"
2. Effect runs → `setData('loaded')` triggers re-render
3. Re-render: `data = 'loaded'` → console: "render: loaded"  
4. Effect runs AGAIN (no deps means every render) → `setData('loaded')` — same value, but if React detects it's the same, it bails out. With strings, React uses `Object.is` and does bail out.

In this specific case with a string, React actually bails out after the second render because `'loaded' === 'loaded'`. But if the effect created a new object or array, the loop would be infinite.

The intention was probably `useEffect(() => { setData('loaded'); }, [])` — run once on mount.

---

## Part 3: Keys and Lists

### Q9 — What is wrong here and what could go wrong?

```tsx
function TodoList() {
  const [todos, setTodos] = useState([
    { text: 'Buy milk' },
    { text: 'Walk dog' },
  ]);

  function addTodoAtTop(text: string) {
    setTodos([{ text }, ...todos]);
  }

  return (
    <ul>
      {todos.map((todo, index) => (
        <li key={index}>
          <input defaultValue={todo.text} />  {/* uncontrolled input */}
        </li>
      ))}
    </ul>
  );
}
```

**Question:** User types into the first input. Then `addTodoAtTop('New task')` is called. What does the user see?

**Answer:**
The typed text appears in the WRONG input after the new task is added. `key={index}` means React identifies items by position, not by identity. When a new item is prepended, all indices shift: old index 0 → 1, old index 1 → 2. React reuses the DOM elements (because keys still exist at 0, 1) but the uncontrolled inputs keep their existing DOM value. So:
- Input at index 0 (now containing "New task" as `defaultValue`) shows whatever was previously typed by the user in the old first input
- The typed content appears misaligned

Fix: use stable, unique IDs as keys:
```tsx
setTodos(todos.map(t => ({...t, id: crypto.randomUUID()})));
{todos.map(todo => <li key={todo.id}><input defaultValue={todo.text} /></li>)}
```

---

## Part 4: Next.js Specific

### Q10 — What is wrong with this Server Component?

```tsx
// app/profile/page.tsx
export default async function ProfilePage() {
  const session = await getSession();
  const userData = await getUserData(session.userId);

  return (
    <div>
      <h1>Welcome, {userData.name}</h1>
      <p>Member since: {userData.createdAt.toLocaleDateString()}</p>
    </div>
  );
}
```

**Question:** This page is SSR. User logs in on Monday, returns on Friday. What date does `createdAt.toLocaleDateString()` use for the locale?

**Answer:**
The server's locale, not the user's browser locale. `toLocaleDateString()` without arguments uses the Node.js/Deno locale, which may differ from the user's browser locale. A user in France may see dates formatted for en-US if that is the server's locale.

Fix — pass explicit locale:
```tsx
<p>Member since: {userData.createdAt.toLocaleDateString('en-US', { dateStyle: 'long' })}</p>

// Or use Intl.DateTimeFormat with the locale from the request headers
import { headers } from 'next/headers';
const acceptLanguage = (await headers()).get('accept-language') ?? 'en-US';
const locale = new Intl.Locale(acceptLanguage.split(',')[0]).language;
```

---

### Q11 — What is the hydration error risk here?

```tsx
// app/greeting/page.tsx
export default function GreetingPage() {
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : 'Good afternoon';

  return <h1>{greeting}</h1>;
}
```

**Question:** This is a Server Component with `export default` (no `'use client'`). Is there a hydration risk?

**Answer:**
Yes, there is a hydration risk IF this component is ever used in a hybrid context or if the page is cached. The server renders at one time (e.g., 11:59 AM — "Good morning"). The client receives the cached HTML and hydrates. If the user's browser evaluates the component at 12:01 PM, the client would produce "Good afternoon", which does not match the server-rendered "Good morning" → hydration mismatch.

More commonly: if the response is statically cached (ISR, SSG, or CDN caching), the cached "Good morning" HTML is served at 3 PM, and on hydration React produces "Good afternoon" — mismatch.

Fix: move time-dependent UI to a Client Component with `useEffect`:
```tsx
'use client';

export function DynamicGreeting() {
  const [greeting, setGreeting] = useState<string | null>(null);
  useEffect(() => {
    const hour = new Date().getHours();
    setGreeting(hour < 12 ? 'Good morning' : 'Good afternoon');
  }, []);
  return <h1>{greeting ?? 'Welcome'}</h1>;
}
```

---

### Q12 — What does this fetch do in Next.js?

```tsx
// app/products/page.tsx
export default async function ProductsPage() {
  const res1 = await fetch('https://api.example.com/products');
  const res2 = await fetch('https://api.example.com/products');  // same URL
  
  const [products1, products2] = await Promise.all([res1.json(), res2.json()]);
  return <ProductList products={products1} />;
}
```

**Question:** Does Next.js make two HTTP requests to the API?

**Answer:**
No — Next.js automatically deduplicates fetch requests with the same URL and options within the same render pass. Both calls return the same cached response — only ONE HTTP request is made. This is called "Request Memoization" and applies within a single render cycle.

Note: this deduplication is for the render pass only. It does NOT persist across renders or requests. Use Next.js cache tags for cross-request caching.

If you use `React.cache()` for non-fetch functions (e.g., direct DB calls), you get the same deduplication behavior:
```tsx
import { cache } from 'react';
const getProducts = cache(async () => db.product.findMany());
// Called multiple times in the same render → single DB query
```

---

### Q13 — What renders after this navigation?

```tsx
'use client';

function Cart() {
  const [items, setItems] = useState<CartItem[]>([]);
  const router = useRouter();

  return (
    <div>
      {items.map(item => <CartRow key={item.id} item={item} />)}
      <button onClick={() => router.push('/checkout')}>Checkout</button>
    </div>
  );
}
```

**Question:** User adds 3 items to cart, clicks Checkout, then clicks browser back. How many items are in the cart?

**Answer:**
0 items — the cart is empty. `useState` is local component state. When the user navigates to `/checkout` via `router.push`, React unmounts the `Cart` component. Its state is destroyed. When the user navigates back, `Cart` mounts fresh with `items = []`.

Solutions:
- Store cart state in Zustand or Redux (survives component unmount, does not survive page refresh)
- Persist cart to `localStorage` or a cookie (survives page refresh)
- Store cart on the server and fetch on mount (survives device change)

Most production apps use a combination: Zustand for immediate UI + server-side cart as source of truth.

---

## Part 5: Performance Traps

### Q14 — Why does React.memo not work here?

```tsx
const ExpensiveComponent = React.memo(({ onUpdate }: { onUpdate: () => void }) => {
  console.log('render');
  return <button onClick={onUpdate}>Update</button>;
});

function Parent() {
  const [count, setCount] = useState(0);

  return (
    <div>
      <button onClick={() => setCount(c => c + 1)}>Parent Count: {count}</button>
      <ExpensiveComponent onUpdate={() => console.log('updated')} />
    </div>
  );
}
```

**Question:** Does `React.memo` prevent `ExpensiveComponent` from re-rendering on each parent button click?

**Answer:**
No. `React.memo` compares props using shallow equality. The `onUpdate` prop is an inline arrow function `() => console.log('updated')` defined inside `Parent`. Every time `Parent` re-renders (every count increment), a NEW function reference is created. `React.memo` sees `previousOnUpdate !== newOnUpdate` and re-renders `ExpensiveComponent`.

Fix: wrap the callback in `useCallback`:
```tsx
const handleUpdate = useCallback(() => console.log('updated'), []);
// Now handleUpdate is the same reference across re-renders
// React.memo comparison passes → ExpensiveComponent does NOT re-render
<ExpensiveComponent onUpdate={handleUpdate} />
```

---

### Q15 — What is wrong with this useMemo?

```tsx
function ProductList({ products }: { products: Product[] }) {
  const total = useMemo(() => products.length, [products]);
  
  return <p>Total: {total}</p>;
}
```

**Question:** Is this useMemo worthwhile?

**Answer:**
No. `useMemo` has overhead: React must store the memoized value and compare dependencies on every render. The calculation `products.length` is a property access — it takes less than a microsecond and has no meaningful cost. Wrapping it in `useMemo` adds complexity and actually makes it SLOWER for this trivial operation.

`useMemo` is worthwhile when:
- The calculation is genuinely expensive (sorting 10,000 items, complex filtering)
- The result is a new object/array reference that downstream memoized components depend on for reference stability

Rule of thumb: profile first, then memoize. Do not memoize proactively.

---

## Quick Reference — 30-Second Answers

| Trap | One-line Answer |
|---|---|
| `setCount(count + 1)` vs `setCount(c => c + 1)` | Functional update reads latest state, not stale closure |
| `array.push()` then `setState(array)` | Same reference — React bails out. Create new array |
| Empty `useEffect` dep array | Runs once on mount, not "on every render" |
| No dep array in `useEffect` | Runs after EVERY render |
| `key={index}` in mutable lists | Index shifts on insert/delete — wrong element gets state |
| Inline function prop + `React.memo` | New function reference every render — memo fails |
| `useMemo` on trivial operations | Overhead exceeds benefit — remove it |
| Server time-dependent rendering | Use `useEffect` to set time-based UI on client |
| Next.js fetch deduplication | Same URL same render = one HTTP request (Request Memoization) |
| Hydration mismatch | Server HTML ≠ first client render → fix with `useEffect` for dynamic values |
