# React Native Hooks Deep Dive — Gold Sheet

> Track Module - Group 1: Starter Path
> Level: beginner to interview-ready | Mode: master React hooks in a mobile context
> Prerequisite: React-Native-Components-Props-State-Hooks-Gold-Sheet.md

---

## 1. Interview Priority Meter

| Hook / Topic | Frequency | Why It Matters |
|---|---|---|
| `useState` batching and identity | Very high | Root of most re-render bugs |
| `useEffect` cleanup and dependency array | Very high | Most common source of memory leaks and stale bugs |
| `useCallback` and `useMemo` — when they help vs hurt | Very high | Over-optimization is a real anti-pattern |
| `useRef` for mutable values | High | Non-obvious use case beyond DOM refs |
| `useReducer` vs `useState` | High | Complex state ownership question |
| `useContext` with performance | High | Re-render explosion trap |
| Custom hooks — design rules | Very high | Senior signal: composition over duplication |
| Hook rules — why and what breaks them | High | Linter enforces them but interview tests understanding |
| `useLayoutEffect` vs `useEffect` | Medium | Timing difference matters for measurements |
| `useId`, `useDeferredValue`, `useTransition` | Medium | React 18+ modern hooks |

---

## 2. The Hook Rules — Why They Exist

React hooks require two rules. These are not arbitrary style choices — they protect React's internal call-order contract.

### Rule 1: Only call hooks at the top level

```tsx
// Wrong — conditional hook breaks call order
function UserCard({isLoggedIn}: {isLoggedIn: boolean}) {
  if (isLoggedIn) {
    const [name, setName] = useState(''); // hook inside conditional
  }
  // ...
}

// Correct — hook at top level, condition inside
function UserCard({isLoggedIn}: {isLoggedIn: boolean}) {
  const [name, setName] = useState('');
  if (!isLoggedIn) return null;
  // ...
}
```

Why this matters:
React tracks hooks by their call order on every render. If a hook is conditionally skipped on one render, all hooks after it shift position by one. React cannot map hook state to the right hook.

### Rule 2: Only call hooks from React function components or other hooks

```tsx
// Wrong — hook outside component
const [count] = useState(0); // top-level module scope — breaks

// Wrong — hook in regular function
function calculateTotal(items: Item[]) {
  const [tax] = useState(0.1); // not a component — breaks
}

// Correct — hook inside component
function CartScreen() {
  const [count, setCount] = useState(0);
  // ...
}

// Correct — hook inside custom hook
function useCartCount() {
  const [count, setCount] = useState(0);
  return count;
}
```

---

## 3. useState — Full Mental Model

### Basic State

```tsx
const [value, setValue] = useState<string>('');
const [count, setCount] = useState(0);
const [user, setUser] = useState<User | null>(null);
```

### Functional Updates — When Order Matters

When new state depends on previous state, always use the functional form:

```tsx
// Wrong — can miss rapid updates because value is captured in closure
const increment = () => setCount(count + 1);

// Correct — receives guaranteed latest value
const increment = () => setCount(prev => prev + 1);

// This matters for rapid presses (e.g., triple-tap)
onPress={() => {
  increment(); // each gets the latest prev
  increment();
  increment(); // ends at count + 3, not count + 1
}}
```

### Object State — Spread Required

React uses `Object.is` for state comparison. Mutating the existing object does not trigger re-render:

```tsx
// Wrong — mutates same reference, no re-render
const updateName = () => {
  user.name = 'New Name'; // mutation
  setUser(user);           // same reference — React skips re-render
};

// Correct — create new object
const updateName = (name: string) => {
  setUser(prev => ({...prev, name}));
};
```

### Lazy Initial State

Expensive initial computations should use the function form:

```tsx
// Wrong — loadFromStorage runs on every render
const [prefs, setPrefs] = useState(loadFromStorage());

// Correct — function runs once on mount
const [prefs, setPrefs] = useState(() => loadFromStorage());
```

### State Batching (React 18+)

React 18 batches all state updates inside event handlers AND async callbacks:

```tsx
const handlePress = async () => {
  setLoading(true);
  const data = await fetchUser();
  setUser(data);        // React 18: both setState calls are batched
  setLoading(false);    // single re-render for both
};
```

---

## 4. useEffect — The Most Misunderstood Hook

### The Four Forms

```tsx
// 1. Runs after every render — rarely what you want in mobile
useEffect(() => { doSomething(); });

// 2. Runs once on mount — equivalent to componentDidMount
useEffect(() => { fetchData(); }, []);

// 3. Runs when dependencies change
useEffect(() => { fetchUser(userId); }, [userId]);

// 4. Cleanup on unmount or before re-run
useEffect(() => {
  const sub = subscribe(userId);
  return () => sub.unsubscribe(); // cleanup function
}, [userId]);
```

### The Cleanup Function — Critical for Mobile

In React Native, not cleaning up causes memory leaks and crashes when navigating:

```tsx
// Memory leak — timer keeps running after screen unmounts
useEffect(() => {
  const id = setInterval(() => tick(), 1000);
}, []);

// Correct — cleanup stops the timer
useEffect(() => {
  const id = setInterval(() => tick(), 1000);
  return () => clearInterval(id);  // runs when component unmounts
}, []);
```

Common cleanup patterns:
```tsx
// Event subscription cleanup
useEffect(() => {
  const sub = AppState.addEventListener('change', handler);
  return () => sub.remove();
}, []);

// Network request cancellation
useEffect(() => {
  const controller = new AbortController();
  fetch(url, {signal: controller.signal}).then(setData).catch(() => {});
  return () => controller.abort();
}, [url]);

// WebSocket cleanup
useEffect(() => {
  const ws = new WebSocket(endpoint);
  ws.onmessage = handleMessage;
  return () => ws.close();
}, [endpoint]);
```

### Stale Closure — The Most Common Bug

```tsx
function Counter() {
  const [count, setCount] = useState(0);

  // Bug: count is captured as 0 when effect runs
  useEffect(() => {
    const id = setInterval(() => {
      console.log(count); // always logs 0 — stale closure
      setCount(count + 1); // always sets to 1, not increments
    }, 1000);
    return () => clearInterval(id);
  }, []); // empty dep array captures count at mount
}

// Fix 1: Include count in dependencies (re-creates interval each time)
useEffect(() => {
  const id = setInterval(() => setCount(count + 1), 1000);
  return () => clearInterval(id);
}, [count]);

// Fix 2: Use functional update (no dependency on count)
useEffect(() => {
  const id = setInterval(() => setCount(prev => prev + 1), 1000);
  return () => clearInterval(id);
}, []); // correct — functional update does not need count in deps
```

### Infinite Loop Trap

```tsx
// Infinite loop — object/array in deps is new on every render
const [data, setData] = useState([]);
const options = {limit: 10}; // new object each render

useEffect(() => {
  fetchData(options);
}, [options]); // options is always a new reference — infinite loop

// Fix — move object inside effect, or use useMemo/useRef
useEffect(() => {
  fetchData({limit: 10}); // stable literal inside effect
}, []);
```

### Missing Dependencies — lint warning exists for a reason

```tsx
// ESLint warning: 'userId' is missing from deps
useEffect(() => {
  loadUser(userId); // userId is used but not in deps
}, []); // bug: userId changes but effect does not re-run

// Correct
useEffect(() => {
  loadUser(userId);
}, [userId]); // re-runs when userId changes
```

---

## 5. useLayoutEffect vs useEffect

| | `useEffect` | `useLayoutEffect` |
|---|---|---|
| When it runs | After paint (async) | Before paint (sync, after DOM mutations) |
| Blocks paint? | No | Yes |
| Use for | Most side effects, data fetching | Measuring layout, preventing flicker |
| Mobile usage | Default choice | Measuring component dimensions |

```tsx
// useLayoutEffect for layout measurement — avoids flicker
const ref = useRef<View>(null);
const [height, setHeight] = useState(0);

useLayoutEffect(() => {
  ref.current?.measure((x, y, width, measuredHeight) => {
    setHeight(measuredHeight); // set before first paint
  });
}, []);
```

---

## 6. useRef — Beyond DOM Refs

`useRef` returns a mutable container whose `.current` value persists across renders without causing re-renders. This has two distinct uses:

### Use 1: Reference to a native view

```tsx
const inputRef = useRef<TextInput>(null);

// Focus the input programmatically
const handleSubmit = () => {
  nextInputRef.current?.focus();
};

<TextInput ref={inputRef} onSubmitEditing={handleSubmit} />
```

### Use 2: Mutable value that does not trigger re-render

```tsx
// Track if component is still mounted — prevents state update after unmount
const isMounted = useRef(true);

useEffect(() => {
  return () => { isMounted.current = false; };
}, []);

const fetchData = async () => {
  const data = await loadUser();
  if (isMounted.current) {  // safe — no crash if navigated away
    setUser(data);
  }
};
```

```tsx
// Track previous value
function usePrevious<T>(value: T): T | undefined {
  const ref = useRef<T>();
  useEffect(() => {
    ref.current = value;
  });
  return ref.current;
}

const prevCount = usePrevious(count); // previous render's count
```

```tsx
// Store a timer/subscription ID without re-rendering
const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

const startTimer = () => {
  timerRef.current = setTimeout(callback, 3000);
};

const cancelTimer = () => {
  if (timerRef.current) clearTimeout(timerRef.current);
};
```

Key rule: `ref.current` changes do NOT cause re-renders. Use `useState` when you need the UI to update; use `useRef` when you need persistence without re-rendering.

---

## 7. useCallback — When It Helps and When It Hurts

`useCallback` memoizes a function so its reference stays stable across renders.

### When it actually helps

```tsx
// Without useCallback — FlatList row re-renders on every parent render
// because onPress is a new function reference each time
<FlatList
  data={items}
  renderItem={({item}) => (
    <ItemRow item={item} onPress={() => navigate(item.id)} /> // new fn each render
  )}
/>

// With useCallback — stable reference, row only re-renders when items/navigate change
const handleItemPress = useCallback((id: string) => {
  navigate('Product', {id});
}, [navigate]);

<FlatList
  data={items}
  renderItem={({item}) => (
    <ItemRow item={item} onPress={handleItemPress} />
  )}
/>
```

### When it does NOT help — and actually wastes memory

```tsx
// Useless useCallback — non-memoized child will re-render anyway
// useCallback only helps when the receiving component is memoized with React.memo
const handlePress = useCallback(() => {
  doSomething();
}, []);

<View onPress={handlePress}> {/* View is not memoized — no benefit */}
```

Rules for useCallback:
1. Only useful when the receiving component is wrapped in `React.memo`
2. Or when the function is used as a `useEffect` dependency
3. Not useful for inline handlers in non-memoized components

### useMemo — memoize computed values

```tsx
// Expensive sort runs on every render without useMemo
const sortedItems = items.sort((a, b) => a.price - b.price); // runs always

// With useMemo — only recomputes when items changes
const sortedItems = useMemo(
  () => [...items].sort((a, b) => a.price - b.price),
  [items],
);
```

Anti-pattern — memoizing cheap computations:
```tsx
// Pointless — the memoization overhead costs more than the addition
const total = useMemo(() => a + b, [a, b]);

// Just compute it
const total = a + b;
```

Interview signal:
"I use `useCallback` when the child component is wrapped in `React.memo` and I can measure a render performance issue. I do not add it by default because premature memoization adds cognitive overhead without benefit."

---

## 8. useReducer — When State Gets Complex

Use `useReducer` instead of `useState` when:
- Multiple state values update together
- Next state depends on previous state in complex ways
- The state update logic is complex enough to deserve testing separately

```tsx
type State = {
  loading: boolean;
  data: User | null;
  error: string | null;
};

type Action =
  | {type: 'FETCH_START'}
  | {type: 'FETCH_SUCCESS'; payload: User}
  | {type: 'FETCH_ERROR'; error: string};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':
      return {loading: true, data: null, error: null};
    case 'FETCH_SUCCESS':
      return {loading: false, data: action.payload, error: null};
    case 'FETCH_ERROR':
      return {loading: false, data: null, error: action.error};
    default:
      return state;
  }
}

function UserScreen({userId}: {userId: string}) {
  const [state, dispatch] = useReducer(reducer, {
    loading: false,
    data: null,
    error: null,
  });

  useEffect(() => {
    dispatch({type: 'FETCH_START'});
    fetchUser(userId)
      .then(user => dispatch({type: 'FETCH_SUCCESS', payload: user}))
      .catch(err => dispatch({type: 'FETCH_ERROR', error: err.message}));
  }, [userId]);
}
```

`useState` vs `useReducer`:
```text
useState: 1-3 related state values, simple updates, local form state
useReducer: many state values that transition together, complex business logic,
            state machine behavior, when you want to unit test state transitions
```

---

## 9. useContext — Power and Re-render Trap

Context shares values across the component tree without prop drilling. The trap is that every context consumer re-renders when the context value changes — even if the component only uses part of the context.

```tsx
// Context definition
const ThemeContext = React.createContext<Theme | null>(null);

// Provider
function App() {
  const [theme, setTheme] = useState<Theme>(defaultTheme);
  return (
    <ThemeContext.Provider value={{theme, setTheme}}>
      <NavigationContainer>...</NavigationContainer>
    </ThemeContext.Provider>
  );
}

// Consumer
function Button({label}: {label: string}) {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('Button must be inside ThemeContext');
  return (
    <Pressable style={{backgroundColor: ctx.theme.primary}}>
      <Text>{label}</Text>
    </Pressable>
  );
}
```

### The Re-render Trap

```tsx
// Problem: ALL consumers re-render when ANY value changes
const AuthContext = React.createContext({user: null, cart: [], settings: {}});
// If cart updates, UserAvatar (which only uses user) also re-renders

// Fix 1: Split contexts by update frequency
const UserContext = React.createContext<User | null>(null);
const CartContext = React.createContext<CartItem[]>([]);

// Fix 2: Memoize the context value
function AuthProvider({children}: {children: React.ReactNode}) {
  const [user, setUser] = useState<User | null>(null);
  const value = useMemo(() => ({user, setUser}), [user]);
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}
```

Context is best for:
- Theme / design system values (change rarely)
- Auth user (changes rarely — login/logout)
- Navigation (React Navigation uses context internally)
- Locale / i18n strings

Context is NOT best for:
- High-frequency updates (animations, scroll position)
- Server data cache (use React Query / Zustand instead)
- Complex shared state (use Zustand/Redux)

---

## 10. Custom Hooks — The Senior Signal

Custom hooks let you extract stateful logic from components. They are the React equivalent of utility functions — but they can use other hooks inside them.

### Design Rules

1. Must start with `use` prefix (lint enforced)
2. Can call other hooks inside
3. Return only what the consumer needs
4. Each call to a custom hook gets independent state

### Pattern 1: Async data with loading/error

```tsx
type AsyncState<T> = {
  data: T | null;
  loading: boolean;
  error: string | null;
};

function useAsync<T>(asyncFn: () => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let cancelled = false;
    setState({data: null, loading: true, error: null});

    asyncFn()
      .then(data => {
        if (!cancelled) setState({data, loading: false, error: null});
      })
      .catch(err => {
        if (!cancelled) setState({data: null, loading: false, error: err.message});
      });

    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return state;
}

// Usage
const {data: user, loading, error} = useAsync(() => fetchUser(userId), [userId]);
```

### Pattern 2: Network status

```tsx
import NetInfo from '@react-native-community/netinfo';

function useNetworkStatus() {
  const [isOnline, setIsOnline] = useState(true);

  useEffect(() => {
    const unsubscribe = NetInfo.addEventListener(state => {
      setIsOnline(state.isConnected ?? true);
    });
    return unsubscribe;
  }, []);

  return isOnline;
}
```

### Pattern 3: Keyboard visibility

```tsx
import {Keyboard, Platform} from 'react-native';

function useKeyboardVisible() {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const showEvent = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hideEvent = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';

    const show = Keyboard.addListener(showEvent, () => setIsVisible(true));
    const hide = Keyboard.addListener(hideEvent, () => setIsVisible(false));

    return () => {
      show.remove();
      hide.remove();
    };
  }, []);

  return isVisible;
}
```

### Pattern 4: Debounced value

```tsx
function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);

  return debounced;
}

// Usage in search
const [query, setQuery] = useState('');
const debouncedQuery = useDebounce(query, 400);

useEffect(() => {
  if (debouncedQuery) search(debouncedQuery);
}, [debouncedQuery]);
```

### Pattern 5: AppState with callback

```tsx
function useAppState(onForeground?: () => void, onBackground?: () => void) {
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    const sub = AppState.addEventListener('change', nextState => {
      if (appState.current === 'background' && nextState === 'active') {
        onForeground?.();
      }
      if (appState.current === 'active' && nextState === 'background') {
        onBackground?.();
      }
      appState.current = nextState;
    });
    return () => sub.remove();
  }, [onForeground, onBackground]);
}
```

---

## 11. React 18+ Modern Hooks

### useTransition — keep UI responsive during expensive updates

```tsx
const [isPending, startTransition] = useTransition();
const [searchResults, setSearchResults] = useState([]);

const handleSearch = (query: string) => {
  startTransition(() => {
    // This update is low priority — React will not block
    setSearchResults(heavyFilter(query));
  });
};

// Show spinner while transition runs
{isPending && <ActivityIndicator />}
```

### useDeferredValue — defer a value update

```tsx
const deferredQuery = useDeferredValue(query);
// deferredQuery lags behind query — React renders with old value
// until a lower-priority update is ready
```

### useId — stable unique IDs

```tsx
const id = useId();
// Generates a unique ID stable across renders
// Useful for accessibility labels that need matching IDs
<TextInput accessibilityLabelledBy={id} />
<Text nativeID={id}>Username</Text>
```

---

## 12. Hook Composition Pattern — Building a Feature

Example: a complete search feature using composed custom hooks:

```tsx
function SearchScreen() {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, 400);
  const isOnline = useNetworkStatus();
  const isKeyboardVisible = useKeyboardVisible();
  const {data: results, loading, error} = useAsync(
    () => (debouncedQuery ? searchProducts(debouncedQuery) : Promise.resolve([])),
    [debouncedQuery],
  );

  return (
    <SafeAreaView style={{flex: 1}}>
      <TextInput
        value={query}
        onChangeText={setQuery}
        placeholder={isOnline ? 'Search...' : 'Offline — no search'}
        editable={isOnline}
      />
      {loading && <ActivityIndicator />}
      {error && <Text style={styles.error}>{error}</Text>}
      {!loading && !error && (
        <FlatList
          data={results}
          keyExtractor={item => item.id}
          renderItem={({item}) => <ProductRow product={item} />}
          style={{flex: 1, marginBottom: isKeyboardVisible ? 300 : 0}}
        />
      )}
    </SafeAreaView>
  );
}
```

The screen has zero business logic — it composes hooks. This is the senior React Native pattern.

---

## 13. Common Hook Traps Summary

| Trap | Wrong | Correct |
|---|---|---|
| Hook in conditional | `if (x) useState()` | Always call at top level |
| Missing cleanup | `useEffect(() => subscribe())` | Always return cleanup fn |
| Stale closure | `setCount(count + 1)` in interval | `setCount(prev => prev + 1)` |
| Object in deps | `useEffect(fn, [options])` where `options` is inline | Move object inside effect |
| useMemo on cheap ops | `useMemo(() => a + b, [a,b])` | Just write `a + b` |
| useCallback on non-memo child | wrapping every handler | Only wrap for memoized children |
| Mutation on state | `user.name = ''; setUser(user)` | `setUser({...user, name: ''})` |
| setState after unmount | setUser(data) without cancelled check | Use `isMounted` ref or AbortController |

---

## 14. Revision Notes

- Hook rules exist to protect React's call-order contract — never skip them
- `useEffect` cleanup is required for any subscription, timer, or request
- Stale closures in effects come from missing dependencies — trust the lint rule
- `useRef` stores mutable values without re-rendering — useful for timers, isMounted, previous values
- `useCallback` only helps when the receiving component is `React.memo` wrapped
- `useMemo` is for expensive computations — not for every value
- Custom hooks = extracted stateful logic that can be independently tested
- Context re-renders all consumers — split contexts by update frequency
- `useReducer` beats multiple `useState` when transitions are complex
- Modern hooks (`useTransition`, `useDeferredValue`) keep the UI responsive during heavy updates
