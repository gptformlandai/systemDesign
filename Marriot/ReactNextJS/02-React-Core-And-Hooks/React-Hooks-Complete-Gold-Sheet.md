# React Hooks Complete - Gold Sheet

> Track Module - Group 2: React Core And Hooks
> Covers: `useState`, `useEffect`, `useRef`, `useMemo`, `useCallback`, `useReducer`, custom hooks, rules and anti-patterns

---

## 1. Intuition

Hooks let function components remember state, synchronize with external systems, hold refs, and reuse behavior.

```text
render snapshot -> event/effect closes over that snapshot -> next state triggers next render
```

The key phrase: each render has its own values.

---

## 2. `useState`

Use for local state that affects rendering.

```tsx
const [count, setCount] = useState(0);
```

Functional update avoids stale state:

```tsx
setCount(current => current + 1);
```

Use when:
- input value
- selected tab
- modal open state
- local toggle

Avoid when:
- value can be derived from props/state
- server cache belongs in query library
- mutable value does not need render, use `useRef`

---

## 3. `useEffect`

Effects synchronize with external systems after render.

```tsx
useEffect(() => {
  const id = setInterval(refresh, 5000);
  return () => clearInterval(id);
}, [refresh]);
```

Good effect uses:
- subscriptions
- timers
- browser APIs
- analytics events
- imperative third-party widgets

Common trap:
Using effects to derive state that could be calculated in render.

Bad:

```tsx
useEffect(() => {
  setFullName(firstName + ' ' + lastName);
}, [firstName, lastName]);
```

Better:

```tsx
const fullName = `${firstName} ${lastName}`;
```

---

## 4. `useRef`

Refs hold mutable values that do not trigger renders.

```tsx
const inputRef = useRef<HTMLInputElement>(null);

function focusInput() {
  inputRef.current?.focus();
}
```

Use for:
- DOM access
- timer IDs
- previous value tracking
- mutable instance-like values

Avoid:
- storing state that should update UI
- using refs to bypass React data flow

---

## 5. `useMemo` And `useCallback`

`useMemo` memoizes a calculated value.

```tsx
const expensiveTotal = useMemo(() => {
  return items.reduce((sum, item) => sum + item.price, 0);
}, [items]);
```

`useCallback` memoizes a function identity.

```tsx
const handleSelect = useCallback((id: string) => {
  setSelectedId(id);
}, []);
```

Use when:
- expensive calculation
- stable identity matters for memoized children
- dependency to another hook needs stability

Do not use everywhere. Memoization has cost and adds cognitive load.

---

## 6. `useReducer`

Use when state transitions are complex or event-driven.

```tsx
type State = {status: 'idle' | 'loading' | 'success' | 'error'};
type Action =
  | {type: 'submit'}
  | {type: 'success'}
  | {type: 'error'};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'submit':
      return {status: 'loading'};
    case 'success':
      return {status: 'success'};
    case 'error':
      return {status: 'error'};
    default:
      return state;
  }
}
```

Benefit:
State transitions become explicit and testable.

---

## 7. Custom Hooks

Custom hooks extract reusable behavior.

```tsx
function useDebouncedValue<T>(value: T, delayMs: number) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return debounced;
}
```

Design principles:
- Name starts with `use`.
- Hide mechanics, expose intent.
- Return stable API where useful.
- Keep side effects inside the hook.
- Do not make one hook do unrelated jobs.

---

## 8. Rules And Anti-Patterns

Rules:
- Call hooks only at top level.
- Call hooks only from React functions or custom hooks.
- Do not call hooks conditionally, inside loops, or after early returns.

Why:
React relies on call order to associate hook state with a component.

Anti-patterns:
- using effect for derived state
- missing effect dependencies
- suppressing hook lint without understanding
- stale closures in timers/async callbacks
- overusing `useMemo` and `useCallback`
- custom hooks that hide too much global behavior

---

## 9. Real-World Use Cases

- `useDebouncedValue` for search.
- `useReducer` for checkout state machine.
- `useRef` for form focus.
- `useEffect` for WebSocket subscription cleanup.
- `useMemo` for expensive filtered table rows.

---

## 10. Strong Interview Answer

Question:
What are the biggest pitfalls with hooks?

Strong answer:

```text
Hooks operate per render, so closures capture values from the render where they
were created. That causes stale state bugs in timers, async callbacks, and effects.
I use functional state updates, correct dependency arrays, and refs for mutable
non-render values. I also avoid using effects for derived state and avoid
memoization unless the calculation or function identity actually matters.
```

---

## 11. Revision Notes

- One-line summary: Hooks attach stateful behavior to function component renders.
- Three keywords: snapshot, dependencies, cleanup.
- One interview trap: Empty dependency array does not mean "run after every render."
- One memory trick: Every render is a snapshot; effects and callbacks remember that snapshot.

---

## 12. React 18 + 19 Hooks Deep Dive

### useTransition — Marking Work as Non-Urgent

```tsx
function SearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<string[]>([]);
  const [isPending, startTransition] = useTransition();

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setQuery(e.target.value);  // urgent — input updates immediately

    startTransition(() => {
      // non-urgent — React can interrupt this to keep input responsive
      setResults(expensiveFilter(e.target.value));
    });
  }

  return (
    <>
      <input value={query} onChange={handleChange} />
      {isPending ? <Spinner /> : <ResultsList items={results} />}
    </>
  );
}
```

**Key rule:** `startTransition` only accepts synchronous updates. You cannot await inside it. For async work, start the fetch normally, then wrap the state update inside `startTransition`.

```tsx
async function handleSearch(query: string) {
  const data = await fetchResults(query);  // fetch is outside startTransition
  startTransition(() => {
    setResults(data);  // only the state update is marked non-urgent
  });
}
```

### useDeferredValue — Deferring a Received Value

```tsx
function ProductList({ query }: { query: string }) {
  const deferredQuery = useDeferredValue(query);
  // query: always current (typing is responsive)
  // deferredQuery: one render behind during transitions (expensive filter debounced)
  
  const filtered = useMemo(
    () => products.filter(p => p.name.includes(deferredQuery)),
    [deferredQuery]
  );

  const isStale = query !== deferredQuery;

  return (
    <div style={{ opacity: isStale ? 0.7 : 1 }}>
      {filtered.map(p => <ProductCard key={p.id} product={p} />)}
    </div>
  );
}
```

**useTransition vs useDeferredValue:**
- `useTransition`: you control the state setter (can wrap it)
- `useDeferredValue`: you receive a value from outside (parent controls the setter)

### useId — SSR-Safe Unique IDs

```tsx
function FormField({ label }: { label: string }) {
  const id = useId();
  // Stable across server and client renders — no hydration mismatch
  return (
    <>
      <label htmlFor={id}>{label}</label>
      <input id={id} />
    </>
  );
}
```

Never use `useId` as a key in lists — it is the same across renders. Use only for accessibility/DOM attributes.

### React 19: use() Hook

```tsx
import { use, Suspense } from 'react';

async function fetchUser(id: string): Promise<User> {
  const res = await fetch(`/api/users/${id}`);
  return res.json();
}

// The promise is created OUTSIDE the component (stable reference)
const userPromise = fetchUser('123');

function UserCard() {
  // use() suspends the component until the promise resolves
  const user = use(userPromise);
  return <div>{user.name}</div>;
}

// Parent wraps in Suspense
function App() {
  return (
    <Suspense fallback={<Spinner />}>
      <UserCard />
    </Suspense>
  );
}
```

`use()` can also be called inside conditional logic — unlike other hooks:
```tsx
function Message({ darkMode }: { darkMode: boolean }) {
  const theme = darkMode ? use(darkThemeContext) : use(lightThemeContext);
  return <p style={{ color: theme.color }}>Hello</p>;
}
```

### React 19: useActionState

```tsx
'use client';
import { useActionState } from 'react';

async function loginAction(prevState: unknown, formData: FormData) {
  'use server';
  const email = formData.get('email') as string;
  // validate and authenticate...
  return { error: 'Invalid credentials' };
}

function LoginForm() {
  const [state, formAction, isPending] = useActionState(loginAction, null);

  return (
    <form action={formAction}>
      <input name="email" type="email" />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Logging in...' : 'Login'}
      </button>
      {state?.error && <p style={{ color: 'red' }}>{state.error}</p>}
    </form>
  );
}
```

### React 19: useOptimistic

```tsx
'use client';
import { useOptimistic } from 'react';

function MessageThread({ messages }: { messages: Message[] }) {
  const [optimisticMessages, addOptimistic] = useOptimistic(
    messages,
    // Reducer: how to apply the optimistic update
    (state: Message[], newMessage: Message) => [...state, newMessage]
  );

  async function sendMessage(formData: FormData) {
    const text = formData.get('message') as string;
    const optimistic = { id: crypto.randomUUID(), text, sending: true };
    
    addOptimistic(optimistic);  // immediately shows in UI
    await sendMessageAction(text);  // server action — real persist
    // After action completes, React reverts optimistic and uses real server state
  }

  return (
    <form action={sendMessage}>
      {optimisticMessages.map(msg => (
        <div key={msg.id} style={{ opacity: msg.sending ? 0.6 : 1 }}>
          {msg.text}
        </div>
      ))}
      <input name="message" />
      <button type="submit">Send</button>
    </form>
  );
}
```

---

## 13. Stale Closure Deep Dive

### Problem 1 — Interval Captures Stale Count

```tsx
// BUG: count never increments past 1
function Counter() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setCount(count + 1);  // count is always 0 here — captured at mount
    }, 1000);
    return () => clearInterval(timer);
  }, []);  // missing count in deps

  return <p>{count}</p>;
}

// FIX 1: Functional updater (does not need to read count)
useEffect(() => {
  const timer = setInterval(() => {
    setCount(prev => prev + 1);  // prev is always current
  }, 1000);
  return () => clearInterval(timer);
}, []);

// FIX 2: Add count to deps (re-creates interval on every count change)
useEffect(() => {
  const timer = setInterval(() => {
    setCount(count + 1);
  }, 1000);
  return () => clearInterval(timer);
}, [count]);  // correct but less efficient
```

### Problem 2 — WebSocket Callback with Stale State

```tsx
// BUG: messages list is always empty in the callback
function Chat() {
  const [messages, setMessages] = useState<string[]>([]);

  useEffect(() => {
    const ws = new WebSocket('wss://example.com');
    ws.onmessage = (e) => {
      setMessages([...messages, e.data]);  // messages captured at [] forever
    };
    return () => ws.close();
  }, []);

  return <ul>{messages.map((m, i) => <li key={i}>{m}</li>)}</ul>;
}

// FIX: Functional updater
ws.onmessage = (e) => {
  setMessages(prev => [...prev, e.data]);  // always reads current list
};
```

### Problem 3 — Event Listener with Stale State

```tsx
// BUG: handler always sees count as 0
useEffect(() => {
  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') console.log('Count:', count);  // always 0
  }
  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, []);

// FIX: Ref to always hold latest value
const countRef = useRef(count);
useEffect(() => { countRef.current = count; });  // sync ref every render

useEffect(() => {
  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') console.log('Count:', countRef.current);  // always current
  }
  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, []);
```

---

## 14. TypeScript for Hooks

### useState — Type Inference and Unions

```tsx
// Type inferred from initial value
const [count, setCount] = useState(0);  // number
const [name, setName] = useState('');   // string

// Explicit type for nullable or complex initial
const [user, setUser] = useState<User | null>(null);
const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');

// Lazy initializer with type
const [data, setData] = useState<Map<string, number>>(() => new Map());
```

### useRef — Two Modes

```tsx
// Mode 1: Mutable container (no DOM) — MutableRefObject<T>
const intervalRef = useRef<NodeJS.Timeout | null>(null);
const countRef = useRef(0);
countRef.current++;  // mutable, no re-render

// Mode 2: DOM ref — RefObject<T> (initializer must be null)
const inputRef = useRef<HTMLInputElement>(null);
// inputRef.current is HTMLInputElement | null
inputRef.current?.focus();  // safe optional chain

// RULE: if you will assign to .current yourself → useRef(initialValue)
// RULE: if React assigns it (DOM element) → useRef(null) with explicit type
```

### useReducer — Discriminated Union Actions

```tsx
type State = { count: number; status: 'idle' | 'loading' | 'error' };
type Action =
  | { type: 'INCREMENT' }
  | { type: 'DECREMENT' }
  | { type: 'SET_STATUS'; payload: State['status'] };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'INCREMENT': return { ...state, count: state.count + 1 };
    case 'DECREMENT': return { ...state, count: state.count - 1 };
    case 'SET_STATUS': return { ...state, status: action.payload };
    default: return state;
  }
}
const [state, dispatch] = useReducer(reducer, { count: 0, status: 'idle' });
dispatch({ type: 'SET_STATUS', payload: 'loading' });  // TypeScript validates payload
```

### useContext — Typed Context with Error

```tsx
type ThemeContextValue = { theme: 'light' | 'dark'; toggle: () => void };
const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be inside ThemeProvider');
  return ctx;
}
```

---

## 15. Common Hook Mistakes Table

| Mistake | Symptom | Fix |
|---|---|---|
| Missing dep in useEffect | Stale value inside effect callback | Add dep or use functional updater |
| No cleanup in useEffect | Memory leak, multiple subscriptions | Return cleanup function |
| Missing key prop | State/input persists across items | Stable key from data ID |
| Calling hook inside condition | Runtime error: "Rendered fewer hooks" | Move hook to top level |
| `[]` deps with inline function | Function recreated every render, effect re-runs | `useCallback` for function dep |
| `useMemo` on trivial computation | Code complexity with no gain | Only useMemo for >1ms compute or stable ref passing |
| Not using functional updater in interval | Stale count, never increments | `setCount(prev => prev + 1)` |
| `async` useEffect | "Warning: Can't perform a React state update on unmounted component" | Define async function inside, call it, use cancelled flag |

