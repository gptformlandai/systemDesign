# React Custom Hooks Production Pattern Library — Gold Sheet

> Track Module - Group 2: React Core And Hooks
> Level: intermediate → senior | 18 production-ready hooks across 6 categories

---

## 1. Intuition

Custom hooks are the primary code-sharing primitive in React. A well-designed hook hides mechanics, exposes intent, and separates what-happens from how-it-happens.

```text
Bad custom hook: leaks implementation, couples caller to internals
Good custom hook: clean API — caller only knows what they get, not how
```

Design rules:
1. Name starts with `use`
2. Single responsibility — one hook, one concern
3. Return stable references where possible (memoize functions and objects in the return value)
4. Handle cleanup — every effect inside a hook needs to return a cleanup function
5. Expose what callers need, not all internal state

---

## 2. Category 1: Async Data

### useAsync — Generic Async State Machine

```tsx
type AsyncState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };

function useAsync<T>(asyncFn: () => Promise<T>, immediate = true) {
  const [state, setState] = useState<AsyncState<T>>({ status: 'idle' });
  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => { isMountedRef.current = false; };
  }, []);

  const execute = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const data = await asyncFn();
      if (isMountedRef.current) setState({ status: 'success', data });
    } catch (error) {
      if (isMountedRef.current) setState({ status: 'error', error: error as Error });
    }
  }, [asyncFn]);

  useEffect(() => {
    if (immediate) execute();
  }, [execute, immediate]);

  return { ...state, execute };
}

// Usage
function UserProfile({ userId }: { userId: string }) {
  const fetchUser = useCallback(() => fetchUserById(userId), [userId]);
  const { status, data: user, error, execute: retry } = useAsync(fetchUser);

  if (status === 'loading') return <Spinner />;
  if (status === 'error') return <button onClick={retry}>Retry: {error.message}</button>;
  if (status === 'success') return <UserCard user={user} />;
  return null;
}
```

### useFetch — HTTP Fetch with Abort

```tsx
function useFetch<T>(url: string, options?: RequestInit) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(url, { ...options, signal: controller.signal });
        if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);
        const json: T = await res.json();
        setData(json);
      } catch (err) {
        if (err instanceof Error && err.name !== 'AbortError') {
          setError(err);
        }
      } finally {
        setLoading(false);
      }
    }
    
    load();
    return () => controller.abort();  // cancel in-flight request on cleanup
  }, [url]);  // re-fetch when URL changes

  return { data, loading, error };
}

// Usage
function ProductPage({ id }: { id: string }) {
  const { data: product, loading, error } = useFetch<Product>(`/api/products/${id}`);
  // ...
}
```

### usePagination — Paginated Data with Load More

```tsx
function usePagination<T>(fetchPage: (page: number) => Promise<{ items: T[]; hasMore: boolean }>) {
  const [items, setItems] = useState<T[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    try {
      const result = await fetchPage(page);
      setItems(prev => [...prev, ...result.items]);
      setHasMore(result.hasMore);
      setPage(p => p + 1);
    } catch (err) {
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  }, [page, loading, hasMore, fetchPage]);

  useEffect(() => { loadMore(); }, []);  // load first page on mount

  const reset = useCallback(() => {
    setItems([]);
    setPage(1);
    setHasMore(true);
  }, []);

  return { items, hasMore, loading, error, loadMore, reset };
}
```

---

## 3. Category 2: Browser APIs

### useLocalStorage — Persistent State

```tsx
function useLocalStorage<T>(key: string, initialValue: T) {
  const [storedValue, setStoredValue] = useState<T>(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? (JSON.parse(item) as T) : initialValue;
    } catch {
      return initialValue;
    }
  });

  const setValue = useCallback((value: T | ((prev: T) => T)) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (error) {
      console.warn(`useLocalStorage: Failed to set key "${key}"`, error);
    }
  }, [key, storedValue]);

  const removeValue = useCallback(() => {
    try {
      window.localStorage.removeItem(key);
      setStoredValue(initialValue);
    } catch { /* ignore */ }
  }, [key, initialValue]);

  return [storedValue, setValue, removeValue] as const;
}

// Usage
const [theme, setTheme, clearTheme] = useLocalStorage<'light' | 'dark'>('theme', 'light');
```

### useMediaQuery — Responsive Hooks

```tsx
function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const mediaQuery = window.matchMedia(query);
    const handler = (event: MediaQueryListEvent) => setMatches(event.matches);
    
    setMatches(mediaQuery.matches);
    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, [query]);

  return matches;
}

// Convenience hooks built on useMediaQuery
const useIsMobile = () => useMediaQuery('(max-width: 768px)');
const useIsDesktop = () => useMediaQuery('(min-width: 1024px)');
const usePrefersReducedMotion = () => useMediaQuery('(prefers-reduced-motion: reduce)');
const usePrefersDark = () => useMediaQuery('(prefers-color-scheme: dark)');

// Usage
function ResponsiveNav() {
  const isMobile = useIsMobile();
  return isMobile ? <MobileNav /> : <DesktopNav />;
}
```

### useEventListener — Clean Event Subscription

```tsx
function useEventListener<K extends keyof WindowEventMap>(
  eventName: K,
  handler: (event: WindowEventMap[K]) => void,
  element?: EventTarget,
  options?: AddEventListenerOptions,
) {
  const handlerRef = useRef(handler);
  
  useEffect(() => {
    handlerRef.current = handler;
  }, [handler]);

  useEffect(() => {
    const target = element ?? window;
    const listener = (event: Event) => handlerRef.current(event as WindowEventMap[K]);
    
    target.addEventListener(eventName, listener, options);
    return () => target.removeEventListener(eventName, listener, options);
  }, [eventName, element, options]);
}

// Usage
useEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

useEventListener('scroll', handleScroll, scrollContainerRef.current ?? undefined);
```

---

## 4. Category 3: UI Behavior

### useDebounce — Delay Value Updates

```tsx
function useDebounce<T>(value: T, delayMs: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const id = setTimeout(() => setDebouncedValue(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);

  return debouncedValue;
}

// Usage — search with debounce
function SearchBar({ onSearch }: { onSearch: (q: string) => void }) {
  const [input, setInput] = useState('');
  const debouncedInput = useDebounce(input, 300);

  useEffect(() => {
    onSearch(debouncedInput);
  }, [debouncedInput, onSearch]);

  return <input value={input} onChange={e => setInput(e.target.value)} />;
}
```

### useThrottle — Rate-limited Updates

```tsx
function useThrottle<T>(value: T, limitMs: number): T {
  const [throttledValue, setThrottledValue] = useState<T>(value);
  const lastRunRef = useRef(0);

  useEffect(() => {
    const now = Date.now();
    const timeSinceLastRun = now - lastRunRef.current;

    if (timeSinceLastRun >= limitMs) {
      lastRunRef.current = now;
      setThrottledValue(value);
    } else {
      const id = setTimeout(() => {
        lastRunRef.current = Date.now();
        setThrottledValue(value);
      }, limitMs - timeSinceLastRun);
      return () => clearTimeout(id);
    }
  }, [value, limitMs]);

  return throttledValue;
}
```

### useOnClickOutside — Close Dropdowns/Modals

```tsx
function useOnClickOutside<T extends HTMLElement>(
  ref: React.RefObject<T | null>,
  handler: (event: MouseEvent | TouchEvent) => void,
) {
  useEffect(() => {
    const listener = (event: MouseEvent | TouchEvent) => {
      if (!ref.current || ref.current.contains(event.target as Node)) return;
      handler(event);
    };

    document.addEventListener('mousedown', listener);
    document.addEventListener('touchstart', listener);

    return () => {
      document.removeEventListener('mousedown', listener);
      document.removeEventListener('touchstart', listener);
    };
  }, [ref, handler]);
}

// Usage
function Dropdown({ options }: { options: string[] }) {
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useOnClickOutside(dropdownRef, () => setOpen(false));

  return (
    <div ref={dropdownRef}>
      <button onClick={() => setOpen(o => !o)}>Menu</button>
      {open && <ul>{options.map(o => <li key={o}>{o}</li>)}</ul>}
    </div>
  );
}
```

### usePrevious — Track Previous Value

```tsx
function usePrevious<T>(value: T): T | undefined {
  const ref = useRef<T | undefined>(undefined);

  useEffect(() => {
    ref.current = value;
  });  // no deps array — runs after every render, capturing the previous render's value

  return ref.current;
}

// Usage
function AnimatedCounter({ count }: { count: number }) {
  const prevCount = usePrevious(count);
  const direction = count > (prevCount ?? 0) ? 'up' : 'down';
  // ...
}
```

---

## 5. Category 4: Intersection Observer

### useIntersectionObserver — Lazy Loading / Infinite Scroll Trigger

```tsx
type IntersectionOptions = {
  threshold?: number | number[];
  rootMargin?: string;
  root?: Element | null;
};

function useIntersectionObserver<T extends HTMLElement>(
  options: IntersectionOptions = {},
): [React.RefCallback<T>, boolean] {
  const [isIntersecting, setIsIntersecting] = useState(false);
  const observerRef = useRef<IntersectionObserver | null>(null);

  const ref: React.RefCallback<T> = useCallback((element) => {
    if (observerRef.current) observerRef.current.disconnect();
    if (!element) return;

    observerRef.current = new IntersectionObserver(([entry]) => {
      setIsIntersecting(entry.isIntersecting);
    }, options);

    observerRef.current.observe(element);
  }, [options.threshold, options.rootMargin]);  // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => () => observerRef.current?.disconnect(), []);

  return [ref, isIntersecting];
}

// Usage — infinite scroll sentinel
function InfiniteList() {
  const [sentinelRef, isVisible] = useIntersectionObserver<HTMLDivElement>({ threshold: 0.1 });
  const { items, hasMore, loading, loadMore } = usePagination(fetchItems);

  useEffect(() => {
    if (isVisible && hasMore && !loading) loadMore();
  }, [isVisible, hasMore, loading, loadMore]);

  return (
    <div>
      {items.map(item => <ItemCard key={item.id} item={item} />)}
      {hasMore && <div ref={sentinelRef}>{loading ? <Spinner /> : null}</div>}
    </div>
  );
}
```

---

## 6. Category 5: Window and Document

### useWindowSize — Responsive Layout Calculations

```tsx
type WindowSize = { width: number; height: number };

function useWindowSize(): WindowSize {
  const [size, setSize] = useState<WindowSize>(() => ({
    width: typeof window !== 'undefined' ? window.innerWidth : 0,
    height: typeof window !== 'undefined' ? window.innerHeight : 0,
  }));

  useEffect(() => {
    const handler = () => setSize({ width: window.innerWidth, height: window.innerHeight });
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, []);

  return size;
}
```

### useScrollPosition — Scroll-aware UI

```tsx
function useScrollPosition() {
  const [scrollY, setScrollY] = useState(0);
  const [scrollDirection, setScrollDirection] = useState<'up' | 'down' | null>(null);
  const prevScrollY = useRef(0);

  useEffect(() => {
    const handler = () => {
      const currentY = window.scrollY;
      setScrollDirection(currentY > prevScrollY.current ? 'down' : 'up');
      setScrollY(currentY);
      prevScrollY.current = currentY;
    };

    window.addEventListener('scroll', handler, { passive: true });
    return () => window.removeEventListener('scroll', handler);
  }, []);

  return { scrollY, scrollDirection, isAtTop: scrollY < 10 };
}

// Usage — hide/show navbar on scroll
function Navbar() {
  const { scrollDirection, isAtTop } = useScrollPosition();
  const visible = isAtTop || scrollDirection === 'up';

  return (
    <nav style={{ transform: visible ? 'translateY(0)' : 'translateY(-100%)' }}>
      ...
    </nav>
  );
}
```

---

## 7. Category 6: Async State and Timers

### useInterval — Declarative setInterval

```tsx
function useInterval(callback: () => void, delayMs: number | null) {
  const callbackRef = useRef(callback);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    if (delayMs === null) return;  // null = paused

    const id = setInterval(() => callbackRef.current(), delayMs);
    return () => clearInterval(id);
  }, [delayMs]);
}

// Usage — live clock, stop polling when tab is hidden
function LiveClock() {
  const [time, setTime] = useState(new Date());
  const [paused, setPaused] = useState(false);

  useInterval(() => setTime(new Date()), paused ? null : 1000);

  return (
    <div>
      <p>{time.toLocaleTimeString()}</p>
      <button onClick={() => setPaused(p => !p)}>{paused ? 'Resume' : 'Pause'}</button>
    </div>
  );
}
```

### useToggle — Boolean State

```tsx
function useToggle(initial = false): [boolean, () => void, (value: boolean) => void] {
  const [value, setValue] = useState(initial);
  const toggle = useCallback(() => setValue(v => !v), []);
  return [value, toggle, setValue];
}

// Usage
const [isOpen, toggleOpen, setOpen] = useToggle(false);
```

### useCountdown — Timed Events

```tsx
function useCountdown(seconds: number, onComplete?: () => void) {
  const [remaining, setRemaining] = useState(seconds);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => { onCompleteRef.current = onComplete; }, [onComplete]);

  useEffect(() => {
    if (remaining <= 0) {
      onCompleteRef.current?.();
      return;
    }
    const id = setTimeout(() => setRemaining(r => r - 1), 1000);
    return () => clearTimeout(id);
  }, [remaining]);

  const reset = useCallback(() => setRemaining(seconds), [seconds]);

  return { remaining, isComplete: remaining <= 0, reset };
}
```

---

## 8. Hook Composition Pattern

Good hooks compose other hooks:

```tsx
// useSearchWithDebounce — composes useFetch + useDebounce
function useSearchWithDebounce<T>(endpoint: string, debounceMs = 300) {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebounce(query, debounceMs);
  
  const url = debouncedQuery
    ? `${endpoint}?q=${encodeURIComponent(debouncedQuery)}`
    : null;
  
  // null URL → skip fetch
  const { data, loading, error } = useFetch<T[]>(url ?? '');
  
  return {
    query,
    setQuery,
    results: url ? (data ?? []) : [],
    loading: loading && !!url,
    error,
  };
}

// Usage
function ProductSearch() {
  const { query, setQuery, results, loading } = useSearchWithDebounce<Product>('/api/products');
  
  return (
    <div>
      <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search..." />
      {loading && <Spinner />}
      {results.map(p => <ProductCard key={p.id} product={p} />)}
    </div>
  );
}
```

---

## 9. Common Hook Design Mistakes

| Mistake | Symptom | Fix |
|---|---|---|
| Missing cleanup in useEffect | Memory leak, zombie state updates | Return cleanup function for every subscription/timer |
| Stale closure in callback | Callback reads initial value forever | Use `useRef` for the latest value, or correct deps |
| Unstable return value | Child re-renders every time | `useCallback` for functions, `useMemo` for objects in return |
| Over-extracting hooks | "useButtonThatDoesEverything" | One hook, one concern |
| Hook returning too much internal state | Couples caller to internals | Only return what callers need |
| Not handling SSR | `window` is undefined on server | Guard with `typeof window !== 'undefined'` |

---

## 10. Strong Interview Answer

**Q: What makes a well-designed custom hook?**

```text
A good custom hook hides implementation and exposes intent. The caller should
understand the API without knowing how it works internally. It handles its own
cleanup — every subscription, timer, or event listener gets removed when the
hook unmounts. It returns stable references using useCallback and useMemo so
downstream components don't re-render unnecessarily. It composes smaller hooks
rather than doing everything in one useEffect. And it has a single responsibility
— a useAsync hook should not also handle authentication, for example.
```

---

## 11. Revision Notes

- `useAsync` and `useFetch`: always use `AbortController` for fetch, guard `setState` with `isMounted` ref
- `useLocalStorage`: lazy state initialization from storage, handle JSON parse failure
- `useMediaQuery`: `matchMedia` with `addEventListener('change', ...)` — not `resize`
- `useOnClickOutside`: checks `ref.current.contains(event.target)` — excludes the element itself
- `usePrevious`: runs with no deps array — captures value BEFORE current render
- `useIntersectionObserver`: returns a `RefCallback` (not a `RefObject`) so element can change
- Composition: hooks that call other hooks produce clean, tested primitives
