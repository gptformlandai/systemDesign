# JavaScript — React Integration, Hooks, Concurrent Rendering — Master Sheet

## What This Covers

- Core hooks: useState, useEffect, useCallback, useMemo, useRef
- Custom hooks pattern
- Concurrent rendering and React 18 features (Suspense, transitions, streaming SSR)
- Component lifecycle through hooks
- Hydration mismatch and streaming SSR
- State management design patterns
- Common React interview traps

---

## 1. Mental Model

```text
React = a function that maps state to UI

Hooks = let function components subscribe to React's state/effects system
Concurrent Mode = React can pause, resume, and abort renders
Suspense = declarative loading/error state — component "suspends" while waiting for data

Browser:
  useState → state change → re-render (synchronous render in legacy, batched in React 18)
  useEffect → after paint → DOM side effects

Server:
  RSC (React Server Components) → run on server, no state, no effects
  Client components → hydrated on the client
```

---

## 2. Interview Priority Meter

| Topic | Priority | Why |
|---|---|---|
| useState and state batching (React 18) | Very high | Foundation |
| useEffect dependency array traps | Very high | Most common interview trap |
| useCallback vs useMemo | High | Performance optimization |
| Custom hooks | High | Abstraction pattern |
| Suspense and error boundaries | High | Modern async rendering |
| Concurrent rendering (useTransition, useDeferredValue) | High | React 18 MAANG question |
| Hydration mismatch | Medium-high | SSR debugging |
| Streaming SSR | Medium-high | Modern architecture |
| React Server Components | Medium-high | 2025 MAANG expectation |

---

## 3. useState

```jsx
const [count, setCount] = useState(0);
```

### Functional Updates

```jsx
// Wrong when update depends on current state
setCount(count + 1);
setCount(count + 1); // Both read same stale count → only +1 total

// Correct: functional update
setCount(prev => prev + 1);
setCount(prev => prev + 1); // +2 total
```

### Lazy Initialization (Avoid Expensive Computation on Every Render)

```jsx
// Bad: expensive() runs on every render
const [value, setValue] = useState(expensive());

// Good: expensive() runs only once
const [value, setValue] = useState(() => expensive());
```

### Object State Traps

```jsx
// Wrong: React won't detect the mutation
const [user, setUser] = useState({ name: "Ava", age: 25 });
user.name = "Bob"; // Mutating state directly
setUser(user); // Same reference → React skips re-render

// Correct: new object reference
setUser({ ...user, name: "Bob" });
```

### State Batching (React 18)

React 18 batches state updates automatically, even in async callbacks and event handlers:

```jsx
// React 18: both setCount and setLoading batched into one re-render
setTimeout(() => {
    setCount(prev => prev + 1);
    setLoading(false);
}, 0);

// React 17: two separate re-renders (both inside async callback)
// React 18: one batched re-render
```

To opt out:

```jsx
import { flushSync } from 'react-dom';

flushSync(() => {
    setCount(prev => prev + 1);
});
// DOM updated here
flushSync(() => {
    setLoading(false);
});
```

---

## 4. useEffect — Deep Dive

```jsx
useEffect(() => {
    // Side effect: runs after render/DOM paint

    return () => {
        // Cleanup: runs before next effect, or on unmount
    };
}, [dependency1, dependency2]); // Re-run when deps change
```

### Dependency Array Rules

```jsx
// [] = run once on mount, cleanup on unmount
useEffect(() => {
    const subscription = subscribe();
    return () => subscription.unsubscribe();
}, []);

// [count] = run when count changes
useEffect(() => {
    document.title = `Count: ${count}`;
}, [count]);

// No array = run after every render (usually wrong)
useEffect(() => {
    console.log("rendered"); // Runs after every render
});
```

### Stale Closure Trap

```jsx
// Trap: effect captures stale value
function Counter() {
    const [count, setCount] = useState(0);

    useEffect(() => {
        const interval = setInterval(() => {
            console.log(count); // Always 0 — stale closure
            setCount(count + 1); // Always sets to 1
        }, 1000);
        return () => clearInterval(interval);
    }, []); // Missing count in deps

    return <div>{count}</div>;
}
```

Fix with functional update:

```jsx
useEffect(() => {
    const interval = setInterval(() => {
        setCount(prev => prev + 1); // Always uses current state
    }, 1000);
    return () => clearInterval(interval);
}, []); // Safe — no dependency on count
```

### Missing Dependency Trap

```jsx
function Search({ query }) {
    const [results, setResults] = useState([]);

    useEffect(() => {
        fetchResults(query).then(setResults); // query used but not in deps!
    }, []); // Bug: won't re-fetch when query changes
    // Fix: }, [query]);
}
```

### Cleanup Race Condition

```jsx
useEffect(() => {
    let cancelled = false;

    fetch(`/api/users/${userId}`)
        .then(r => r.json())
        .then(data => {
            if (!cancelled) { // Don't update state if component unmounted or userId changed
                setUser(data);
            }
        });

    return () => { cancelled = true; };
}, [userId]);
```

---

## 5. useCallback and useMemo

### useCallback — Memoize Functions

```jsx
// Problem: new function reference on every render → child re-renders
function Parent() {
    const [count, setCount] = useState(0);

    const handleClick = () => setCount(prev => prev + 1); // New ref every render

    return <ExpensiveChild onClick={handleClick} />;
}

// Fix: stable reference
function Parent() {
    const [count, setCount] = useState(0);

    const handleClick = useCallback(() => {
        setCount(prev => prev + 1);
    }, []); // No deps: always same function

    return <ExpensiveChild onClick={handleClick} />;
}
```

**When to use**:
- Props passed to `React.memo()` wrapped children
- Event handlers in dependency arrays of other hooks
- Callbacks that are dependencies of useEffect

### useMemo — Memoize Values

```jsx
// Expensive computation cached between renders
const sortedList = useMemo(() => {
    return items.sort((a, b) => a.price - b.price);
}, [items]); // Only recompute when items changes
```

```jsx
// Stable object reference for expensive context values
const contextValue = useMemo(() => ({
    user,
    permissions,
    login,
    logout
}), [user, permissions, login, logout]); // Only new object when deps change

return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
```

**useCallback vs useMemo**:

```jsx
// useCallback(fn, deps) ≡ useMemo(() => fn, deps)
const memoizedFn = useCallback(() => doSomething(a, b), [a, b]);
const memoizedFn2 = useMemo(() => () => doSomething(a, b), [a, b]);
// Both equivalent — useCallback is just cleaner for functions
```

---

## 6. useRef

```jsx
// Persist value across renders without causing re-render
const countRef = useRef(0);

// Access DOM element imperatively
const inputRef = useRef(null);

useEffect(() => {
    inputRef.current.focus(); // Imperative DOM access
}, []);

return <input ref={inputRef} />;
```

### Ref for Stable Callback Pattern

```jsx
// Capture latest value without re-running effects
function useLatestCallback(fn) {
    const ref = useRef(fn);

    useLayoutEffect(() => {
        ref.current = fn; // Always latest version
    });

    return useCallback((...args) => {
        return ref.current(...args);
    }, []); // Stable reference, always calls latest fn
}
```

### Tracking Previous Value

```jsx
function usePrevious(value) {
    const ref = useRef();

    useEffect(() => {
        ref.current = value;
    }); // No deps: after every render, store current value

    return ref.current; // Returns previous value (from before last render)
}
```

---

## 7. Custom Hooks

Custom hooks extract reusable stateful logic.

```jsx
// Data fetching hook
function useBookings(guestId) {
    const [bookings, setBookings] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!guestId) return;

        let cancelled = false;
        setLoading(true);

        fetch(`/api/guests/${guestId}/bookings`)
            .then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            })
            .then(data => {
                if (!cancelled) {
                    setBookings(data);
                    setLoading(false);
                }
            })
            .catch(err => {
                if (!cancelled) {
                    setError(err.message);
                    setLoading(false);
                }
            });

        return () => { cancelled = true; };
    }, [guestId]);

    return { bookings, loading, error };
}

// Usage
function BookingList({ guestId }) {
    const { bookings, loading, error } = useBookings(guestId);

    if (loading) return <Spinner />;
    if (error) return <ErrorMessage message={error} />;
    return <ul>{bookings.map(b => <BookingItem key={b.id} booking={b} />)}</ul>;
}
```

**Rules of hooks** (enforced by `eslint-plugin-react-hooks`):
1. Only call hooks at the top level (not inside loops, conditions, or nested functions)
2. Only call hooks from React functions or other custom hooks

---

## 8. React.memo

```jsx
// Prevents re-render if props haven't changed (shallow comparison)
const BookingCard = React.memo(function BookingCard({ booking, onCancel }) {
    console.log("rendering BookingCard", booking.id);
    return (
        <div>
            <h3>{booking.hotel}</h3>
            <button onClick={() => onCancel(booking.id)}>Cancel</button>
        </div>
    );
});

// onCancel must be stable (useCallback) for memo to help
function Parent() {
    const handleCancel = useCallback((id) => {
        cancelBooking(id);
    }, []);

    return <BookingCard booking={booking} onCancel={handleCancel} />;
}
```

**When memo helps**: many children, complex render, props change infrequently.
**When memo hurts**: simple components, props change every render → overhead without benefit.

---

## 9. Suspense and Error Boundaries

### Suspense

Suspense lets components "suspend" while waiting for async operations.

```jsx
// Data fetching with Suspense (React 18 + data library like TanStack Query)
function BookingDetail({ bookingId }) {
    const booking = useSuspenseQuery({
        queryKey: ['booking', bookingId],
        queryFn: () => fetchBooking(bookingId)
    });

    return <div>{booking.hotel}</div>;
}

// Parent wraps with Suspense + ErrorBoundary
function BookingPage({ bookingId }) {
    return (
        <ErrorBoundary fallback={<ErrorView />}>
            <Suspense fallback={<Spinner />}>
                <BookingDetail bookingId={bookingId} />
            </Suspense>
        </ErrorBoundary>
    );
}
```

### Error Boundaries

Class components only (no hook equivalent yet):

```jsx
class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, info) {
        // Log to error tracking service
        errorTracker.captureException(error, { extra: info });
    }

    render() {
        if (this.state.hasError) {
            return this.props.fallback || <div>Something went wrong</div>;
        }
        return this.props.children;
    }
}
```

---

## 10. Concurrent Rendering — useTransition and useDeferredValue

React 18 can pause low-priority renders when higher-priority updates arrive.

### useTransition — Mark Non-Urgent Updates

```jsx
function SearchPage() {
    const [query, setQuery] = useState('');
    const [deferredQuery, setDeferredQuery] = useState('');
    const [isPending, startTransition] = useTransition();

    function handleChange(event) {
        setQuery(event.target.value); // Urgent: update input immediately

        startTransition(() => {
            setDeferredQuery(event.target.value); // Non-urgent: can defer
        });
    }

    return (
        <>
            <input value={query} onChange={handleChange} />
            {isPending && <Spinner />}
            <SearchResults query={deferredQuery} /> {/* May render stale value during transition */}
        </>
    );
}
```

**Key**: React renders the urgent update (input value) immediately. The transition update (`deferredQuery`) can be interrupted if the user keeps typing — preventing the app from feeling sluggish on every keystroke.

### useDeferredValue — Defer a Value

```jsx
function SearchResults({ query }) {
    const deferredQuery = useDeferredValue(query);
    // Renders with potentially stale deferredQuery during transitions

    const results = useMemo(
        () => expensiveFilter(allItems, deferredQuery),
        [deferredQuery]
    );

    return <ul>{results.map(r => <ResultItem key={r.id} result={r} />)}</ul>;
}
```

**useTransition vs useDeferredValue**:

| | useTransition | useDeferredValue |
|---|---|---|
| Controls | Transitions you initiate | A derived value |
| Use when | You trigger the state update | You receive a prop or state to defer |
| Shows isPending | Yes | No (must compare current vs deferred value) |

---

## 11. Streaming SSR and Hydration

### React 18 Streaming SSR

```jsx
// server.js (Node.js)
import { renderToPipeableStream } from 'react-dom/server';

app.get('/*', (req, res) => {
    const { pipe } = renderToPipeableStream(<App />, {
        bootstrapScripts: ['/main.js'],
        onShellReady() {
            res.setHeader('Content-Type', 'text/html');
            pipe(res); // Stream HTML shell immediately
        },
        onError(error) {
            console.error(error);
        }
    });
});
```

```jsx
// Client: use Suspense to mark what can be streamed later
function App() {
    return (
        <html>
            <body>
                <Header /> {/* Sent immediately */}
                <Suspense fallback={<Spinner />}>
                    <BookingsList /> {/* Streamed after data is ready */}
                </Suspense>
            </body>
        </html>
    );
}
```

**Streaming SSR flow**:
1. Server sends HTML shell (header, nav) immediately → browser renders
2. Server streams remaining HTML for each Suspense boundary as data resolves
3. Client hydrates each part as it arrives (selective hydration)
4. User can interact with early content before full hydration

### Hydration Mismatch

```text
Hydration mismatch: HTML from SSR doesn't match what React renders on client
→ React throws warning, may discard server HTML and re-render from scratch
→ Performance hit + flash of content
```

**Common causes**:

```jsx
// Random/time-based values differ between server and client
function Component() {
    return <div>{Math.random()}</div>; // Different on server vs client → mismatch

    return <div>{new Date().toString()}</div>; // Different time → mismatch

    return <div>{typeof window !== 'undefined' ? 'client' : 'server'}</div>; // Mismatch
}
```

**Fix for client-only values**:

```jsx
function Component() {
    const [isClient, setIsClient] = useState(false);

    useEffect(() => {
        setIsClient(true); // Only runs on client after hydration
    }, []);

    if (!isClient) return null; // Match server output during hydration

    return <div>{window.location.pathname}</div>;
}
```

---

## 12. React Server Components (RSC)

React Server Components run on the server and never ship their JavaScript to the client.

```jsx
// app/page.tsx (Next.js App Router) — Server Component (default)
// No 'use client' directive = runs on server
export default async function BookingsPage() {
    // Direct data access — no useEffect, no API call from client
    const bookings = await fetchBookingsFromDB();

    return (
        <div>
            <h1>Bookings</h1>
            {bookings.map(b => (
                <div key={b.id}>{b.hotel} — {b.status}</div>
            ))}
            <CancelButton bookingId={booking.id} /> {/* Client component */}
        </div>
    );
}
```

```jsx
// Client component for interactivity
'use client';

export function CancelButton({ bookingId }) {
    const [cancelling, setCancelling] = useState(false);

    return (
        <button onClick={() => {
            setCancelling(true);
            cancelBooking(bookingId);
        }}>
            {cancelling ? 'Cancelling...' : 'Cancel'}
        </button>
    );
}
```

**RSC vs Client Component**:

| | Server Component | Client Component |
|---|---|---|
| Renders on | Server | Client (and server for SSR) |
| Can use | `async/await`, server APIs, DB | useState, useEffect, event handlers |
| Sends to browser | HTML (no JS bundle) | JavaScript |
| Re-renders | On server request | On state/prop change |

---

## 13. State Management Design

### When to Use Each

| Solution | Use When |
|---|---|
| useState | Local, isolated state |
| useReducer | Complex state transitions, multiple sub-values |
| Context | Cross-component shared state (theme, auth, locale) |
| Zustand/Jotai | Global client state without boilerplate |
| TanStack Query | Server state (async data fetching, caching, invalidation) |
| Redux Toolkit | Large apps with complex global state + devtools |

### useReducer Pattern

```jsx
const initialState = { count: 0, loading: false, error: null };

function reducer(state, action) {
    switch (action.type) {
        case 'INCREMENT':
            return { ...state, count: state.count + 1 };
        case 'LOADING':
            return { ...state, loading: true };
        case 'SUCCESS':
            return { ...state, loading: false, data: action.payload };
        case 'ERROR':
            return { ...state, loading: false, error: action.error };
        default:
            return state;
    }
}

function Counter() {
    const [state, dispatch] = useReducer(reducer, initialState);

    return (
        <button onClick={() => dispatch({ type: 'INCREMENT' })}>
            {state.count}
        </button>
    );
}
```

### Context Performance Trap

```jsx
// BAD: Any context value change re-renders ALL consumers
const AppContext = React.createContext();

function AppProvider({ children }) {
    const [user, setUser] = useState(null);
    const [theme, setTheme] = useState('light');

    return (
        <AppContext.Provider value={{ user, theme, setUser, setTheme }}>
            {children}
        </AppContext.Provider>
    );
}
// Changing theme re-renders all components using AppContext

// GOOD: Split contexts by update frequency
const UserContext = React.createContext();
const ThemeContext = React.createContext();
```

---

## 14. Common React Interview Traps

| Trap | Why It Fails | Fix |
|---|---|---|
| Missing dep in useEffect | Stale closure, stale prop | Add all deps; use functional updates |
| Mutating state directly | React doesn't detect mutation | Always return new reference |
| useCallback without React.memo | useCallback alone doesn't prevent child re-render | Combine with React.memo on child |
| Infinite useEffect loop | Effect updates a dep → triggers effect again | Check deps; use functional updates |
| Fetch in useEffect without cleanup | Stale response updates state after unmount | Cancelled flag or AbortController |
| Context causing unnecessary re-renders | Single context with mixed update frequency | Split contexts, or use selector libraries |
| `key` on wrong element | React incorrectly matches elements | Key should be on the outermost element in the list |
| `key` using array index | Stable keys by index → stale state if list reorders | Use unique entity IDs |

---

## 15. Strong Interview Answers

### useEffect Dependency Array

```text
The dependency array tells React when to re-run the effect. An empty array runs the effect once
on mount. A missing dependency means the effect uses a stale version of that value, which causes
bugs when it should react to changes.

React's eslint plugin catches most missing dependencies. I follow the rule: list everything the
effect uses. For functions, I use useCallback to stabilize them. For values that change frequently
but shouldn't trigger the effect, I use refs.
```

### Concurrent Rendering

```text
React 18 introduced concurrent rendering, which lets React pause low-priority renders when urgent
updates arrive. Without it, a slow render blocks everything.

I use useTransition to mark non-urgent state updates — like filtering a large list while the user
types. The input updates immediately; the list render can be interrupted. This keeps the UI
responsive without adding debounce complexity.
```

### Hydration

```text
Hydration mismatches happen when the HTML rendered on the server doesn't match what React would
render on the client. Common causes are random values, dates, or window/document access that differ
between environments.

I fix this by using useEffect to run client-only logic after hydration, or by deferring rendering
of client-specific content until after mount. React 18's selective hydration also makes this less
catastrophic — only the affected Suspense boundary needs to recover.
```

---

## 16. Final Revision Checklist

```text
□ useState functional updates: setCount(prev => prev + 1) for state-dependent updates
□ Lazy useState: useState(() => expensive()) for one-time computation
□ React 18 automatic batching: async callbacks now batched too
□ useEffect deps: list everything; missing dep = stale closure bug
□ useEffect cleanup: cancel flag or AbortController to prevent stale updates
□ useCallback: stabilize function refs for React.memo children or useEffect deps
□ useMemo: memoize expensive computation or stable object references for context
□ React.memo: requires stable props (useCallback for functions)
□ Custom hooks: extract stateful logic; rules of hooks apply
□ Suspense: declarative loading; suspending component triggers nearest Suspense boundary
□ ErrorBoundary: class component; catches render errors
□ useTransition: urgent vs non-urgent state updates; renders non-urgently in background
□ useDeferredValue: defer a derived value from a prop
□ Streaming SSR: React.renderToPipeableStream; streams Suspense boundaries
□ Hydration mismatch: random/time/window causes; fix with useEffect/deferred rendering
□ RSC: server-only; no hooks; reduces JS bundle; pair with Client Components for interactivity
□ Context split: separate contexts by update frequency to avoid unnecessary re-renders
```
